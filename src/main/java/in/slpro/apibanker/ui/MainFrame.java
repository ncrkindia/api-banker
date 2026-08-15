package in.slpro.apibanker.ui;

import javax.swing.*;

import in.slpro.apibanker.App;
import in.slpro.apibanker.model.*;
import in.slpro.apibanker.storage.StorageManager;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * MainFrame
 *
 * <p>
 * This is the central UI Controller and Root Window for the ApiBanker
 * application.
 * It manages the primary layout (Sidebar vs Workspace), handles global
 * application state,
 * routes actions from the Menu Bar, and orchestrates the lifecycle of all
 * Workspace Tabs
 * (Requests, Environments, Mock Server, etc.). It acts as the central event bus
 * for
 * saving, restoring, and persisting UI state across sessions.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class MainFrame extends JFrame {
    private final StorageManager storage;
    private List<CollectionModel> collections;
    private List<EnvironmentModel> environments;
    private List<RequestModel> history;

    private SidebarPanel sidebarPanel;
    private JTabbedPane workspaceTabs;
    private JPanel workspacePanel;
    private CardLayout workspaceCardLayout;
    private JComboBox<String> envCombo;
    private JButton envManageBtn;
    private JButton globalVarBtn;
    private JButton settingsBtn;
    private JPanel envSelectorPanel;

    private int currentFontSize = 16;

    private static final int MAX_HISTORY = 500;
    public static final String OTHERS_COLLECTION_ID = "__others__";

    private static MainFrame instance;
    public static File lastFileChooserDirectory = null;

    /**
     * @return The singleton instance of the MainFrame.
     */
    public static MainFrame getInstance() {
        return instance;
    }

    /**
     * Locates the parent {@link CollectionModel} (folder/collection) that contains
     * the given request.
     * 
     * @param req The request to search for.
     * @return The parent CollectionModel, or null if not found.
     */
    public CollectionModel getParentCollection(RequestModel req) {
        return findRequestParent(req);
    }

    /**
     * @return The global list of previously executed requests (History).
     */
    public List<RequestModel> getHistoryList() {
        return history;
    }

    /**
     * A static helper to locate the parent collection of a request without needing
     * a direct reference
     * to the MainFrame instance.
     * 
     * @param req The RequestModel.
     * @return The parent CollectionModel.
     */
    public static CollectionModel findParentCollection(RequestModel req) {
        MainFrame frame = getInstance();
        return frame != null ? frame.getParentCollection(req) : null;
    }

    /**
     * Constructs the primary application window.
     * <p>
     * This constructor handles the bootstrap phase of the UI:
     * 1. Loads persisted state (Collections, Environments, History) via
     * {@link StorageManager}.
     * 2. Restores window dimensions, maximized state, and font scaling.
     * 3. Initializes the internal UI components (Sidebar, Workspace).
     * 4. Ensures the default "__others__" collection exists for orphan requests.
     * 5. Binds global hotkeys and applies the selected FlatLaf theme.
     * </p>
     */
    public MainFrame() {
        instance = this;
        this.storage = StorageManager.getInstance();
        this.collections = storage.loadCollections();
        this.environments = storage.loadEnvironments();
        this.history = storage.loadHistory();

        setTitle("ApiBanker - Offline API Client");

        // Read saved font size
        int savedFontSize = storage.getSettings().getFontSize();
        if (savedFontSize >= 10 && savedFontSize <= 30) {
            currentFontSize = savedFontSize;
        }

        // Read saved window settings
        int width = storage.getSettings().getWindowWidth();
        int height = storage.getSettings().getWindowHeight();
        if (width > 200 && height > 200) {
            setSize(width, height);
        } else {
            setSize(1300, 800);
        }

        if (storage.getSettings().isWindowMaximized()) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        initUI();
        ensureOthersCollection();
        sidebarPanel.refreshCollections(collections);
        setupZoom();
        setupSaveHotkey();
        applyTheme();
        updateFontSize(currentFontSize);
    }

    private final java.util.Stack<String> collectionUndoStack = new java.util.Stack<>();

    public void pushCollectionStateForUndo() {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String state = gson.toJson(collections);
            collectionUndoStack.push(state);
            if (collectionUndoStack.size() > 50) {
                collectionUndoStack.remove(0);
            }
        } catch (Exception e) {}
    }

    public void undoCollectionTree() {
        if (!collectionUndoStack.isEmpty()) {
            if (in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().isStricterEditing()) {
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to undo the last action?", "Confirm Undo", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
            }
            String state = collectionUndoStack.pop();
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<CollectionModel>>(){}.getType();
                List<CollectionModel> restored = gson.fromJson(state, listType);
                if (restored != null) {
                    this.collections.clear();
                    this.collections.addAll(restored);
                    saveCollections();
                    sidebarPanel.refreshCollections(this.collections);
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("UNDO_COLLECTION_ACTION", "User", "Restored previous collection state.");
                    showToast(this, "Undo successful");
                }
            } catch (Exception e) {
                showToast(this, "Undo failed");
            }
        } else {
            showToast(this, "Nothing to undo");
        }
    }

    /**
     * Initializes the core UI hierarchy.
     * <p>
     * Sets up the global JMenuBar, the structural JSplitPane separating the Sidebar
     * from the Workspace,
     * and initializes the CardLayout responsible for toggling between the Welcome
     * screen and the Tabbed Pane.
     * Finally, it restores any previously open tabs from the user's last session
     * via {@link AppSettings}.
     * </p>
     */
    private void initUI() {
        // Menu bar
        setJMenuBar(buildMenuBar());

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Sidebar
        sidebarPanel = new SidebarPanel(this);

        // Workspace
        workspaceTabs = new JTabbedPane();
        workspaceTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        workspaceTabs.addChangeListener(e -> {
            Component selected = workspaceTabs.getSelectedComponent();
            if (selected instanceof GlobalVariablesPanel) {
                ((GlobalVariablesPanel) selected).loadModel();
            } else if (selected instanceof EnvironmentManagerPanel) {
                ((EnvironmentManagerPanel) selected).refreshEnvironments(environments);
            }
        });

        workspaceCardLayout = new CardLayout();
        workspacePanel = new JPanel(workspaceCardLayout);

        JPanel welcomePanel = buildWelcomePanel();
        workspacePanel.add(welcomePanel, "welcome");
        workspacePanel.add(workspaceTabs, "tabs");

        workspaceCardLayout.show(workspacePanel, "welcome");

        workspaceTabs.addContainerListener(new java.awt.event.ContainerListener() {
            @Override
            public void componentAdded(java.awt.event.ContainerEvent e) {
                SwingUtilities.invokeLater(() -> updateWorkspaceVisibility());
            }

            @Override
            public void componentRemoved(java.awt.event.ContainerEvent e) {
                SwingUtilities.invokeLater(() -> updateWorkspaceVisibility());
            }
        });

        // JSplitPane to make sidebar adjustable in size
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, workspacePanel);
        splitPane.setDividerLocation(300);
        splitPane.setBorder(null);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // Load collections into sidebar
        sidebarPanel.refreshCollections(collections);
        sidebarPanel.refreshHistory(history);

        // Restore opened tabs
        List<AppSettings.OpenTabState> openTabs = storage.getSettings().getOpenTabs();
        if (openTabs != null && !openTabs.isEmpty()) {
            for (AppSettings.OpenTabState ts : openTabs) {
                restoreTab(ts);
            }
            int savedIndex = storage.getSettings().getSelectedTabIndex();
            if (savedIndex >= 0 && savedIndex < workspaceTabs.getTabCount()) {
                workspaceTabs.setSelectedIndex(savedIndex);
            }
            // If nothing was successfully restored, open the welcome tab
            if (workspaceTabs.getTabCount() == 0) {
                openWelcomeTab();
            }
        } else {
            openWelcomeTab();
        }
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newReqItem = new JMenuItem("New Request");
        newReqItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newReqItem.addActionListener(e -> openNewRequest());
        JMenu importMenu = new JMenu("Import");
        JMenuItem importApiBankerItem = new JMenuItem("ApiBanker Files");
        importApiBankerItem.addActionListener(e -> importApiBankerFiles());
        JMenuItem importPostmanItem = new JMenuItem("Postman Files");
        importPostmanItem.addActionListener(e -> importPostmanFiles());
        JMenuItem importOpenApiItem = new JMenuItem("OpenAPI / Swagger Spec");
        importOpenApiItem.addActionListener(e -> importOpenApiSpec());
        importMenu.add(importApiBankerItem);
        importMenu.add(importPostmanItem);
        importMenu.add(importOpenApiItem);

        JMenu exportMenu = new JMenu("Export");
        JMenuItem exportApiBankerItem = new JMenuItem("ApiBanker Files");
        exportApiBankerItem.addActionListener(e -> openExportTab("apibanker"));
        JMenuItem exportPostmanItem = new JMenuItem("Postman Files");
        exportPostmanItem.addActionListener(e -> openExportTab("postman"));
        exportMenu.add(exportApiBankerItem);
        exportMenu.add(exportPostmanItem);

        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(e -> openSettings());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> onClose());

        fileMenu.add(newReqItem);
        fileMenu.addSeparator();
        fileMenu.add(importMenu);
        fileMenu.add(exportMenu);
        fileMenu.addSeparator();
        fileMenu.add(settingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem consoleItem = new JMenuItem("Console");
        consoleItem.addActionListener(e -> new ConsoleDialog(this).setVisible(true));
        JMenuItem logConsoleItem = new JMenuItem("Log Console Tab");
        logConsoleItem.addActionListener(e -> openLogConsole());
        JMenuItem runnerLogsItem = new JMenuItem("Collection Runner Logs");
        runnerLogsItem.addActionListener(e -> openCollectionRunnerLogs());
        JMenuItem mockLogsItem = new JMenuItem("Mock Server Logs");
        mockLogsItem.addActionListener(e -> openMockServerLogs());
        viewMenu.add(consoleItem);
        viewMenu.add(logConsoleItem);
        viewMenu.add(runnerLogsItem);
        viewMenu.add(mockLogsItem);
        viewMenu.addSeparator();
        JMenuItem envMgrItem = new JMenuItem("Environment Manager...");
        envMgrItem.addActionListener(e -> openEnvManager());
        JMenuItem globalVarsItem = new JMenuItem("Global Variables...");
        globalVarsItem.addActionListener(e -> openGlobalVariables());
        viewMenu.add(envMgrItem);
        viewMenu.add(globalVarsItem);

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem mockServerItem = new JMenuItem("Mock Server");
        mockServerItem.addActionListener(e -> openMockServer());
        JMenuItem compareItem = new JMenuItem("Data Comparator");
        compareItem.addActionListener(e -> openDataComparator());
        
        JMenuItem jwtItem = new JMenuItem("JWT Decoder");
        jwtItem.addActionListener(e -> openJwtDecoder());
        JMenuItem dataToolsItem = new JMenuItem("Data Tools");
        dataToolsItem.addActionListener(e -> openDataTools());
        JMenuItem cookieJarItem = new JMenuItem("Cookie Jar Manager...");
        cookieJarItem.addActionListener(e -> openCookieJarManager());
        
        toolsMenu.add(mockServerItem);
        toolsMenu.add(compareItem);
        toolsMenu.addSeparator();
        toolsMenu.add(jwtItem);
        toolsMenu.add(dataToolsItem);
        toolsMenu.add(cookieJarItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem userGuideItem = new JMenuItem("User Guide");
        userGuideItem.addActionListener(e -> openDocumentationPanel());
        helpMenu.add(userGuideItem);
        JMenuItem featuresItem = new JMenuItem("Features");
        featuresItem.addActionListener(e -> openFeaturesPanel());
        helpMenu.add(featuresItem);
        helpMenu.addSeparator();
        JMenuItem aboutItem = new JMenuItem("About ApiBanker");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        bar.add(fileMenu);
        bar.add(viewMenu);
        bar.add(toolsMenu);
        bar.add(helpMenu);
        
        bar.add(Box.createHorizontalGlue());
        bar.add(buildEnvSelector());
        
        return bar;
    }

    private JPanel buildEnvSelector() {
        envSelectorPanel = new JPanel();
        envSelectorPanel.setLayout(new BoxLayout(envSelectorPanel, BoxLayout.X_AXIS));
        envSelectorPanel.setOpaque(false);
        envSelectorPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        envSelectorPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        envCombo = new JComboBox<>();
        envCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshEnvCombo();
        envCombo.setAlignmentY(Component.CENTER_ALIGNMENT);

        envCombo.addActionListener(e -> {
            int idx = envCombo.getSelectedIndex();
            if (idx > 0 && idx - 1 < environments.size()) {
                storage.getSettings().setActiveEnvironmentId(environments.get(idx - 1).getId());
                storage.saveSettings();
            } else {
                storage.getSettings().setActiveEnvironmentId(null);
                storage.saveSettings();
            }
            triggerVariableRepaintAll();
        });

        envSelectorPanel.add(envCombo);
        envSelectorPanel.add(Box.createHorizontalStrut(6));

        Dimension fixedSize = new Dimension(34, 34);

        envManageBtn = new JButton("☁");
        envManageBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        envManageBtn.setForeground(new Color(52, 152, 219));
        envManageBtn.putClientProperty("fixedMargin", true);
        envManageBtn.putClientProperty("fixedFont", true);
        envManageBtn.setToolTipText("Environment Manager");
        envManageBtn.setFocusPainted(false);
        envManageBtn.setMargin(new Insets(0, 0, 0, 0));
        envManageBtn.setAlignmentY(Component.CENTER_ALIGNMENT);
        envManageBtn.setPreferredSize(fixedSize);
        envManageBtn.setMinimumSize(fixedSize);
        envManageBtn.setMaximumSize(fixedSize);
        envManageBtn.addActionListener(e -> openEnvManager());
        envSelectorPanel.add(envManageBtn);
        envSelectorPanel.add(Box.createHorizontalStrut(6));

        globalVarBtn = new JButton("🌐");
        globalVarBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        globalVarBtn.setForeground(new Color(46, 204, 113));
        globalVarBtn.putClientProperty("fixedMargin", true);
        globalVarBtn.putClientProperty("fixedFont", true);
        globalVarBtn.setToolTipText("Global Variable Manager");
        globalVarBtn.setFocusPainted(false);
        globalVarBtn.setMargin(new Insets(0, 0, 0, 0));
        globalVarBtn.setAlignmentY(Component.CENTER_ALIGNMENT);
        globalVarBtn.setPreferredSize(fixedSize);
        globalVarBtn.setMinimumSize(fixedSize);
        globalVarBtn.setMaximumSize(fixedSize);
        globalVarBtn.addActionListener(e -> openGlobalVariables());
        envSelectorPanel.add(globalVarBtn);
        envSelectorPanel.add(Box.createHorizontalStrut(6));

        settingsBtn = new JButton("⚙");
        settingsBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        settingsBtn.setForeground(new Color(230, 126, 34));
        settingsBtn.putClientProperty("fixedMargin", true);
        settingsBtn.putClientProperty("fixedFont", true);
        settingsBtn.setToolTipText("Settings");
        settingsBtn.setFocusPainted(false);
        settingsBtn.setMargin(new Insets(0, 0, 0, 0));
        settingsBtn.setAlignmentY(Component.CENTER_ALIGNMENT);
        settingsBtn.setPreferredSize(fixedSize);
        settingsBtn.setMinimumSize(fixedSize);
        settingsBtn.setMaximumSize(fixedSize);
        settingsBtn.addActionListener(e -> openSettings());
        envSelectorPanel.add(settingsBtn);

        // Adjust initial dimensions based on currentFontSize
        int height = Math.max(26, currentFontSize + 10);
        int width = Math.max(140, currentFontSize * 9);
        envCombo.setPreferredSize(new Dimension(width, height));
        envCombo.setMaximumSize(new Dimension(width, height));

        return envSelectorPanel;
    }

    public void refreshEnvCombo() {
        String activeId = storage.getSettings().getActiveEnvironmentId();
        envCombo.removeAllItems();
        envCombo.addItem("No Environment");
        int selIdx = 0;
        for (int i = 0; i < environments.size(); i++) {
            envCombo.addItem(environments.get(i).getName());
            if (environments.get(i).getId().equals(activeId))
                selIdx = i + 1;
        }
        envCombo.setSelectedIndex(selIdx);
    }

    public EnvironmentModel getActiveEnvironment() {
        int idx = envCombo.getSelectedIndex();
        if (idx > 0 && idx - 1 < environments.size())
            return environments.get(idx - 1);
        return null;
    }

    // ─── Collections ─────────────────────────────────────────────────────────

    public List<CollectionModel> getCollections() {
        return collections;
    }

    private void ensureOthersCollection() {
        for (CollectionModel col : collections) {
            if (OTHERS_COLLECTION_ID.equals(col.getId()))
                return;
        }
        CollectionModel others = new CollectionModel(OTHERS_COLLECTION_ID, "Others");
        others.setRequests(new ArrayList<>());
        collections.add(0, others);
        saveCollections();
    }

    public CollectionModel getOrCreateOthersCollection() {
        for (CollectionModel col : collections) {
            if (OTHERS_COLLECTION_ID.equals(col.getId()))
                return col;
        }
        ensureOthersCollection();
        return collections.get(0);
    }

    public CollectionModel askTargetCollection(String prompt) {
        List<CollectionModel> cols = getCollections();
        if (cols.isEmpty()) {
            ensureOthersCollection();
            cols = getCollections();
        }
        CollectionModel others = getOrCreateOthersCollection();
        return (CollectionModel) JOptionPane.showInputDialog(this,
                prompt, "Select Collection", JOptionPane.PLAIN_MESSAGE,
                null, cols.toArray(), others);
    }

    public void moveRequestToCollection(RequestModel req) {
        CollectionModel sourceCol = findRequestParent(req);
        List<CollectionPathWrapper> wrappers = getAllCollectionsAndFoldersWithPaths();
        CollectionPathWrapper defaultSel = null;
        if (sourceCol != null) {
            for (CollectionPathWrapper w : wrappers) {
                if (w.model == sourceCol) {
                    defaultSel = w;
                    break;
                }
            }
        }
        CollectionPathWrapper targetWrapper = (CollectionPathWrapper) JOptionPane.showInputDialog(this,
                "Move '" + req.getName() + "' to:", "Move to Collection/Folder", JOptionPane.PLAIN_MESSAGE,
                null, wrappers.toArray(),
                defaultSel != null ? defaultSel : (wrappers.isEmpty() ? null : wrappers.get(0)));
        if (targetWrapper == null || targetWrapper.model == sourceCol)
            return;
        if (sourceCol != null)
            sourceCol.getRequests().remove(req);
        pushCollectionStateForUndo();
        targetWrapper.model.getRequests().add(req);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
    }

    public void refreshCollections(List<CollectionModel> collections) {
        sidebarPanel.refreshCollections(collections);
    }

    public CollectionModel createCollection(String name) {
        pushCollectionStateForUndo();
        CollectionModel col = new CollectionModel(UUID.randomUUID().toString(), name);
        col.setRequests(new ArrayList<>());
        collections.add(col);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        return col;
    }

    public void deleteCollection(CollectionModel col) {
        if (OTHERS_COLLECTION_ID.equals(col.getId())) {
            JOptionPane.showMessageDialog(this, "Cannot delete the 'Others' collection.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().isStricterEditing()) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete collection '" + col.getName() + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        } else {
            showToast(this, "Deleted collection '" + col.getName() + "'");
        }
        pushCollectionStateForUndo();
        closeTabsForCollectionRecursive(col);
        deleteCollectionRecursive(collections, col);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
    }

    private void closeTabsForCollectionRecursive(CollectionModel col) {
        if (col.getRequests() != null) {
            for (RequestModel req : col.getRequests()) {
                closeTabForRequest(req);
            }
        }
        if (col.getFolders() != null) {
            for (CollectionModel folder : col.getFolders()) {
                closeTabsForCollectionRecursive(folder);
            }
        }
    }

    private void closeTabForRequest(RequestModel req) {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof RequestPanel rp && rp.getRequestModel().getId().equals(req.getId())) {
                workspaceTabs.removeTabAt(i);
                break;
            }
        }
    }

    public RequestModel addRequestToCollection(CollectionModel col, String name) {
        pushCollectionStateForUndo();
        RequestModel req = new RequestModel();
        req.setName(name);
        col.getRequests().add(req);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRequest(req);
        return req;
    }

    public RequestModel addRunnerToCollection(CollectionModel col) {
        pushCollectionStateForUndo();
        RequestModel runner = new RequestModel();
        runner.setName(col.getName() + " Runner");
        runner.setType("runner");
        runner.setMethod("RUNNER");
        col.getRequests().add(runner);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRunner(col, runner);
        return runner;
    }

    public void deleteRequest(RequestModel req) {
        if (in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().isStricterEditing()) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete request '" + req.getName() + "'?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        } else {
            showToast(this, "Deleted request '" + req.getName() + "'");
        }
        pushCollectionStateForUndo();
        for (CollectionModel col : collections) {
            if (deleteRequestRecursive(col, req)) {
                break;
            }
        }
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        // Close tab if open
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof RequestPanel rp && rp.getRequestModel().getId().equals(req.getId())) {
                workspaceTabs.removeTabAt(i);
                break;
            }
        }
    }

    public void deleteMultiple(List<Object> items) {
        if (items == null || items.isEmpty()) return;
        if (in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().isStricterEditing()) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + items.size() + " item(s)?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        } else {
            showToast(this, "Deleted " + items.size() + " item(s)");
        }
        pushCollectionStateForUndo();
        boolean changed = false;
        for (Object item : items) {
            if (item instanceof CollectionModel col) {
                if (OTHERS_COLLECTION_ID.equals(col.getId())) continue;
                closeTabsForCollectionRecursive(col);
                if (deleteCollectionRecursive(collections, col)) changed = true;
            } else if (item instanceof RequestModel req) {
                closeTabForRequest(req);
                for (CollectionModel c : collections) {
                    if (deleteRequestRecursive(c, req)) {
                        changed = true;
                        break;
                    }
                }
            }
        }
        if (changed) {
            saveCollections();
            sidebarPanel.refreshCollections(collections);
        }
    }

    public RequestModel addComparatorToCollection(CollectionModel col) {
        pushCollectionStateForUndo();
        RequestModel comp = new RequestModel();
        comp.setName(col.getName() + " Comparator");
        comp.setType("comparator");
        comp.setMethod("COMPARE");
        col.getRequests().add(comp);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRequest(comp);
        return comp;
    }

    public RequestModel addMockServerToCollection(CollectionModel col) {
        pushCollectionStateForUndo();
        RequestModel mock = new RequestModel();
        mock.setName(col.getName() + " Mock Server");
        mock.setType("mockserver");
        mock.setMethod("MOCK");
        col.getRequests().add(mock);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openMockServer(mock);
        return mock;
    }

    public RequestModel addWebSocketToCollection(CollectionModel col) {
        RequestModel ws = new RequestModel();
        ws.setName(col.getName() + " WS Client");
        ws.setType("websocket");
        ws.setMethod("WS");
        col.getRequests().add(ws);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRequest(ws);
        return ws;
    }

    public RequestModel duplicateRequestModel(RequestModel req) {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(req);
        RequestModel dup = gson.fromJson(json, RequestModel.class);
        dup.setId(UUID.randomUUID().toString());
        return dup;
    }

    public void duplicateRequest(RequestModel req) {
        for (CollectionModel col : collections) {
            if (duplicateRequestRecursive(col, req)) {
                break;
            }
        }
    }

    public void saveRequestAs(RequestModel req) {
        String newName = JOptionPane.showInputDialog(this, "Save As name:", req.getName());
        if (newName == null || newName.isBlank())
            return;
        List<CollectionPathWrapper> wrappers = getAllCollectionsAndFoldersWithPaths();
        if (wrappers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No collections available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        CollectionPathWrapper targetWrapper = (CollectionPathWrapper) JOptionPane.showInputDialog(this,
                "Select destination collection/folder:", "Save As", JOptionPane.PLAIN_MESSAGE,
                null, wrappers.toArray(), wrappers.get(0));
        if (targetWrapper == null)
            return;

        RequestModel dup = duplicateRequestModel(req);
        dup.setName(newName.trim());
        targetWrapper.model.getRequests().add(dup);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRequest(dup);
    }

    public void saveCollections() {
        storage.saveCollections(collections);
    }

    public void saveCurrentRequest(RequestModel req) {
        saveCollections();
    }

    public void updateTabTitle(RequestModel req) {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            RequestModel model = null;
            if (c instanceof RequestPanel rp) {
                model = rp.getRequestModel();
            } else if (c instanceof CollectionRunnerPanel crp) {
                model = crp.getRequestModel();
            } else if (c instanceof DataComparatorPanel dcp) {
                model = dcp.getRequestModel();
            } else if (c instanceof MockServerPanel msp) {
                model = msp.getRequestModel();
            }

            if (model != null && model.getId().equals(req.getId())) {
                workspaceTabs.setTitleAt(i, req.getName());
                Component tabComp = workspaceTabs.getTabComponentAt(i);
                if (tabComp instanceof TabHeaderPanel header) {
                    header.title = req.getName();
                    for (Component child : header.getComponents()) {
                        if (child instanceof JLabel titleLabel) {
                            String displayTitle = req.getName();
                            if (displayTitle.length() > 16) {
                                displayTitle = displayTitle.substring(0, 13) + "...";
                            }
                            titleLabel.setText(displayTitle);
                            titleLabel.setToolTipText(req.getName());
                        }
                        if (child instanceof JTextField editField) {
                            editField.setText(req.getName());
                        }
                    }
                }
                break;
            }
        }
    }

    // ─── Tabs ─────────────────────────────────────────────────────────────────

    public void openRequest(RequestModel req) {
        if ("runner".equals(req.getType())) {
            CollectionModel parentCol = findRequestParent(req);
            if (parentCol != null) {
                openRunner(parentCol, req);
                return;
            }
        }

        if ("comparator".equals(req.getType())) {
            for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
                Component c = workspaceTabs.getComponentAt(i);
                if (c instanceof DataComparatorPanel dcp && dcp.getRequestModel().getId().equals(req.getId())) {
                    workspaceTabs.setSelectedIndex(i);
                    return;
                }
            }
            DataComparatorPanel panel = new DataComparatorPanel(this, req);
            int idx = workspaceTabs.getTabCount();
            workspaceTabs.addTab(req.getName(), panel);
            workspaceTabs.setTabComponentAt(idx, buildTabHeader(req.getName(), idx, panel));
            workspaceTabs.setSelectedIndex(idx);
            return;
        }

        if ("mockserver".equals(req.getType())) {
            openMockServer(req);
            return;
        }

        if ("websocket".equals(req.getType())) {
            for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
                Component c = workspaceTabs.getComponentAt(i);
                if (c instanceof WebSocketPanel wsp && wsp.getRequestModel().getId().equals(req.getId())) {
                    workspaceTabs.setSelectedIndex(i);
                    return;
                }
            }
            WebSocketPanel panel = new WebSocketPanel(this, req);
            int idx = workspaceTabs.getTabCount();
            workspaceTabs.addTab(req.getName(), panel);
            workspaceTabs.setTabComponentAt(idx, buildTabHeader(req.getName(), idx, panel));
            workspaceTabs.setSelectedIndex(idx);
            return;
        }

        // Check if already open
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof RequestPanel rp && rp.getRequestModel().getId().equals(req.getId())) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        RequestPanel panel = new RequestPanel(this, req);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab(req.getName(), panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader(req.getName(), idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openRunner(CollectionModel col, RequestModel runner) {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof CollectionRunnerPanel crp && crp.getRequestModel().getId().equals(runner.getId())) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        CollectionRunnerPanel panel = new CollectionRunnerPanel(this, col, runner);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab(runner.getName(), panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader(runner.getName(), idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openCollection(CollectionModel col) {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof CollectionPanel cp && cp.getCollectionModel().getId().equals(col.getId())) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        CollectionPanel panel = new CollectionPanel(this, col);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab(col.getName(), panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader(col.getName(), idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openHistoryRequest(RequestModel req) {
        // Create a temporary copy for viewing
        RequestModel copy = new RequestModel();
        copy.setId(UUID.randomUUID().toString());
        copy.setName(req.getName() + " (History)");
        copy.setMethod(req.getMethod());
        copy.setUrl(req.getUrl());
        copy.setHeaders(req.getHeaders());
        copy.setParams(req.getParams());
        copy.setBodyType(req.getBodyType());
        copy.setBodyRawContent(req.getBodyRawContent());
        copy.setBodyRawType(req.getBodyRawType());
        copy.setAuthType(req.getAuthType());
        copy.setAuthToken(req.getAuthToken());
        openRequest(copy);
    }

    private void openWelcomeTab() {
        workspaceTabs.removeAll();
        updateWorkspaceVisibility();
    }

    private void updateWorkspaceVisibility() {
        if (workspaceTabs.getTabCount() == 0) {
            workspaceCardLayout.show(workspacePanel, "welcome");
        } else {
            workspaceCardLayout.show(workspacePanel, "tabs");
        }
    }

    private JPanel buildWelcomePanel() {
        JPanel welcome = new JPanel(new BorderLayout());
        welcome.setBackground(UIManager.getColor("Panel.background"));

        JTextPane welcomePane = new JTextPane();
        welcomePane.setContentType("text/html");
        welcomePane.setEditable(false);
        welcomePane.setBackground(UIManager.getColor("Panel.background"));

        Color accentColor = UIManager.getColor("AccentColor");
        if (accentColor == null)
            accentColor = new Color(26, 115, 232);
        String accentHex = String.format("#%02x%02x%02x", accentColor.getRed(), accentColor.getGreen(),
                accentColor.getBlue());
        String textHex = "#555555";
        String cardBgHex = "#FFFFFF";

        boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
        String borderColorHex = "#DDDDDD";
        if (isDark) {
            textHex = "#CCCCCC";
            cardBgHex = "#2B2B2B";
            borderColorHex = "#444444";
        }

        String html = "<html><body style='font-family:\"Segoe UI\",sans-serif; margin:30px; color:" + textHex + ";'>"
                + "<div style='text-align:center; margin-bottom:30px;'>"
                + "  <h1 style='color:" + accentHex + "; font-size:36px; margin:0;'>ApiBanker</h1>"
                + "  <h2 style='font-weight:normal; font-size:18px; margin:5px 0 15px 0;'>The Ultimate Offline API Client & Collection Runner</h2>"
                + "<table width='100%' cellpadding='10' cellspacing='10'>"
                + "  <tr>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>&#128640; Collection Runner</h3>"
                + "      <p style='font-size:13px; line-height:1.5;'>Execute whole API suites concurrently with configurable virtual users and delay. Monitor real-time logs, view live analytics charts (response codes and latency percentiles), and export polished PDF or Excel summary reports.</p>"
                + "    </td>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>&#128272; Advanced Authentication</h3>"
                + "      <p style='font-size:13px; line-height:1.5;'>Native support for OAuth 2.0, Bearer tokens, and Basic Auth. Authenticate at the root Collection or Folder level and recursively inherit security credentials down to all nested requests.</p>"
                + "    </td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>&#128196; OpenAPI / Swagger Import</h3>"
                + "      <p style='font-size:13px; line-height:1.5;'>Import any <b>OpenAPI 3.x</b> or <b>Swagger 2.x</b> spec (JSON or YAML) as a fully structured Collection. Selectively pick endpoints, configure the Base URL strategy (inline or <code>{{baseUrl}}</code> collection variable for multi-server specs), auto-detect security schemes, and auto-generate professional API documentation into the Collection README.</p>"
                + "    </td>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>&#9875; Rhino Scripting Engine</h3>"
                + "      <p style='font-size:13px; line-height:1.5;'>Write custom JavaScript in Pre-request and Post-request tabs. Manage dynamic state with <code>apibanker.globals</code>, <code>apibanker.environment</code>, and <code>apibanker.collectionVariables</code> scopes, plus built-in code snippets.</p>"
                + "    </td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>&#9889; Integrated Tool Suite</h3>"
                + "      <p style='font-size:13px; line-height:1.5;'>Built-in JWT Decoder, Data Comparator, JSON Schema validation, Mock Data Generator, Postman v2.1 &amp; JMeter import-export, OpenAPI Swagger importer, Global Variables tab, and an offline local Mock Server. Distribute seamlessly using built-in Native Packaging (Windows .msi, GraalVM .exe, Linux .deb).</p>"
                + "    </td>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>&#128274; Privacy &amp; Local Security</h3>"
                + "      <ul style='font-size:13px; line-height:1.6; margin:0; padding-left:20px;'>"
                + "        <li><b>Local-First:</b> No telemetry, tracking, or user registrations.</li>"
                + "        <li><b>Auto Migration:</b> Zero-loss migration to <code>.apibanker</code> workspace.</li>"
                + "        <li><b>Git-Friendly:</b> Human-readable JSON workspace files.</li>"
                + "      </ul>"
                + "    </td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td colspan='2' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>&#9000; Keyboard Shortcuts</h3>"
                + "      <table style='font-size:13px; border-collapse:collapse;'>"
                + "        <tr><td style='padding:3px 16px 3px 0;'><b>Ctrl + S</b></td><td style='padding:3px 30px 3px 0;'>Save Active Tab</td>"
                + "            <td style='padding:3px 16px 3px 0;'><b>Ctrl + R</b></td><td style='padding:3px 30px 3px 0;'>Send / Run Request</td></tr>"
                + "        <tr><td style='padding:3px 16px 3px 0;'><b>F2 / Del</b></td><td style='padding:3px 30px 3px 0;'>Rename / Delete Node</td>"
                + "            <td style='padding:3px 16px 3px 0;'><b>Ctrl + C/V/D</b></td><td style='padding:3px 30px 3px 0;'>Copy / Paste / Duplicate</td></tr>"
                + "        <tr><td style='padding:3px 16px 3px 0;'><b>Ctrl + = / -</b></td><td style='padding:3px 30px 3px 0;'>Zoom UI In / Out</td>"
                + "            <td style='padding:3px 16px 3px 0;'><b>Ctrl + O</b></td><td style='padding:3px 30px 3px 0;'>Open Collection</td></tr>"
                + "      </table>"
                + "    </td>"
                + "  </tr>"
                + "</table>"
                + "<div style='margin-top:30px; text-align:center; font-size:12px; color:#888;'>"
                + "  © 2026 ApiBanker by SLPRO. All Rights Reserved."
                + "</div>"
                + "</body></html>";

        welcomePane.setText(html);
        welcomePane.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(welcomePane);
        scroll.setBorder(null);
        welcome.add(scroll, BorderLayout.CENTER);
        return welcome;
    }

    private void openNewRequest() {
        RequestModel req = new RequestModel();
        req.setName("Untitled Request");
        CollectionModel others = getOrCreateOthersCollection();
        others.getRequests().add(req);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRequest(req);
    }

    private void openExportTab(String type) {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof ExportPanel) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        ExportPanel panel = new ExportPanel(this, type);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Export", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Export", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openJwtDecoder(String initialToken) {
        RequestModel req = new RequestModel();
        req.setName("JWT Decoder");
        req.setType("jwt");
        if (initialToken != null)
            req.setBodyRawContent(initialToken);
        JwtDecoderPanel panel = new JwtDecoderPanel(this, req);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("JWT Decoder", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("JWT Decoder", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openJwtDecoder() {
        openJwtDecoder(null);
    }

    public void openJsonTool() {
        openDataTools();
    }

    public void openDataComparator() {
        CollectionModel col = askTargetCollection("Save Data Comparator to:");
        if (col == null)
            return;
        addComparatorToCollection(col);
    }

    public void openMockServer() {
        CollectionModel col = askTargetCollection("Save Mock Server to:");
        if (col == null)
            return;
        addMockServerToCollection(col);
    }

    public void openMockServer(RequestModel req) {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof MockServerPanel msp && msp.getRequestModel().getId().equals(req.getId())) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        MockServerPanel panel = new MockServerPanel(this, req);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab(req.getName(), panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader(req.getName(), idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openDataTools() {
        DataToolsPanel panel = new DataToolsPanel(this);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Data Tools", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Data Tools", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openLogConsole() {
        LogConsolePanel panel = new LogConsolePanel(this);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Log Console", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Log Console", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openCollectionRunnerLogs() {
        CollectionRunnerLogsPanel panel = new CollectionRunnerLogsPanel(this);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Collection Runner Logs", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Collection Runner Logs", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openMockServerLogs() {
        MockServerLogsPanel panel = new MockServerLogsPanel(this);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Mock Server Logs", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Mock Server Logs", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public boolean closeTab(Component tabContent) {
        int idx = workspaceTabs.indexOfComponent(tabContent);
        if (idx >= 0) {
            if (tabContent instanceof RequestPanel rp) {
                if (rp.hasUnsavedChanges()) {
                    int option = JOptionPane.showConfirmDialog(
                            this,
                            "Request \"" + rp.getRequestModel().getName() + "\" has unsaved changes. Save them?",
                            "Save Changes?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        rp.save();
                    } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                        return false;
                    }
                } else {
                    rp.getRequestModel(); // triggers collect
                }
                saveCollections();
            } else if (tabContent instanceof CollectionPanel cp) {
                if (cp.hasUnsavedChanges()) {
                    int option = JOptionPane.showConfirmDialog(
                            this,
                            "Collection \"" + cp.getCollectionModel().getName() + "\" has unsaved changes. Save them?",
                            "Save Changes?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        cp.save();
                    } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                        return false;
                    }
                }
                saveCollections();
            } else if (tabContent instanceof SettingsPanel sp) {
                if (sp.hasUnsavedChanges()) {
                    int option = JOptionPane.showConfirmDialog(
                            this,
                            "Settings have unsaved changes. Save them?",
                            "Save Changes?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        sp.saveSettings();
                    } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                        return false;
                    }
                }
            } else if (tabContent instanceof CollectionRunnerPanel crp) {
                crp.saveConfig();
                saveCollections();
            } else if (tabContent instanceof LogConsolePanel lcp) {
                lcp.removeListener();
            } else if (tabContent instanceof MockServerPanel msp) {
                if (msp.hasUnsavedChanges()) {
                    int option = JOptionPane.showConfirmDialog(
                            this,
                            "Mock Server has unsaved changes. Save them?",
                            "Save Changes?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        msp.save();
                    } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                        return false;
                    }
                }
                msp.stopServerIfRunning();
            }
            workspaceTabs.removeTabAt(idx);
            return true;
        }
        return false;
    }

    private void togglePinTab(Component tabContent) {
        int idx = workspaceTabs.indexOfComponent(tabContent);
        if (idx < 0)
            return;

        JComponent comp = (JComponent) tabContent;
        boolean isPinned = Boolean.TRUE.equals(comp.getClientProperty("pinned"));
        comp.putClientProperty("pinned", !isPinned);

        String currentTitle = workspaceTabs.getTitleAt(idx);

        if (!isPinned) {
            // Pinning: move to index among pinned tabs
            int firstNonPinnedIdx = 0;
            for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
                Component c = workspaceTabs.getComponentAt(i);
                if (c != tabContent && Boolean.TRUE.equals(((JComponent) c).getClientProperty("pinned"))) {
                    firstNonPinnedIdx++;
                }
            }
            if (idx != firstNonPinnedIdx) {
                workspaceTabs.removeTabAt(idx);
                workspaceTabs.insertTab(currentTitle, null, tabContent, null, firstNonPinnedIdx);
                idx = firstNonPinnedIdx;
            }
        }

        // Rebuild all tab headers to ensure index bounds and pin labels are correct
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component tc = workspaceTabs.getComponentAt(i);
            workspaceTabs.setTabComponentAt(i, buildTabHeader(workspaceTabs.getTitleAt(i), i, tc));
        }
        workspaceTabs.setSelectedComponent(tabContent);
    }

    private void closeOthers(Component tabContent) {
        List<Component> tabsToRemove = new ArrayList<>();
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c != tabContent && !Boolean.TRUE.equals(((JComponent) c).getClientProperty("pinned"))) {
                tabsToRemove.add(c);
            }
        }
        for (Component c : tabsToRemove) {
            if (!closeTab(c)) {
                break;
            }
        }
    }

    private void closeToLeft(Component tabContent) {
        int targetIdx = workspaceTabs.indexOfComponent(tabContent);
        if (targetIdx < 0)
            return;
        List<Component> tabsToRemove = new ArrayList<>();
        for (int i = 0; i < targetIdx; i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (!Boolean.TRUE.equals(((JComponent) c).getClientProperty("pinned"))) {
                tabsToRemove.add(c);
            }
        }
        for (Component c : tabsToRemove) {
            if (!closeTab(c)) {
                break;
            }
        }
    }

    private void closeToRight(Component tabContent) {
        int targetIdx = workspaceTabs.indexOfComponent(tabContent);
        if (targetIdx < 0)
            return;
        List<Component> tabsToRemove = new ArrayList<>();
        for (int i = targetIdx + 1; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (!Boolean.TRUE.equals(((JComponent) c).getClientProperty("pinned"))) {
                tabsToRemove.add(c);
            }
        }
        for (Component c : tabsToRemove) {
            if (!closeTab(c)) {
                break;
            }
        }
    }

    private void closeAllTabs() {
        List<Component> tabsToRemove = new ArrayList<>();
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (!Boolean.TRUE.equals(((JComponent) c).getClientProperty("pinned"))) {
                tabsToRemove.add(c);
            }
        }
        for (Component c : tabsToRemove) {
            if (!closeTab(c)) {
                break;
            }
        }
    }

    private JPanel buildTabHeader(String title, int tabIndex, Component tabContent) {
        TabHeaderPanel header = new TabHeaderPanel(title);

        String displayTitle = title;
        if (displayTitle.length() > 16) {
            displayTitle = displayTitle.substring(0, 13) + "...";
        }
        JLabel titleLabel = new JLabel(displayTitle);
        titleLabel.setToolTipText(title);
        
        if ("☁".equals(title) || "🌐".equals(title) || "⚙".equals(title)) {
            titleLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, currentFontSize + 2));
            if ("☁".equals(title)) {
                titleLabel.setForeground(new Color(52, 152, 219));
                titleLabel.setToolTipText("Environment Manager");
            } else if ("🌐".equals(title)) {
                titleLabel.setForeground(new Color(46, 204, 113));
                titleLabel.setToolTipText("Global Variables Manager");
            } else if ("⚙".equals(title)) {
                titleLabel.setForeground(new Color(230, 126, 34));
                titleLabel.setToolTipText("Settings");
            }
        } else {
            titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, currentFontSize));
        }

        boolean renameable = (tabContent instanceof RequestPanel) ||
                (tabContent instanceof CollectionRunnerPanel) ||
                (tabContent instanceof DataComparatorPanel) ||
                (tabContent instanceof MockServerPanel) ||
                (tabContent instanceof CollectionPanel) ||
                (tabContent instanceof WebSocketPanel);

        final JTextField editField;
        final Runnable startEdit;

        if (renameable) {
            final JTextField tf = new JTextField(title);
            tf.setFont(titleLabel.getFont());
            tf.setPreferredSize(new Dimension(140, currentFontSize + 8));
            tf.setVisible(false);
            editField = tf;

            startEdit = () -> {
                tf.setText(header.title);
                titleLabel.setVisible(false);
                tf.setVisible(true);
                header.revalidate();
                header.repaint();
                tf.requestFocusInWindow();
                tf.selectAll();
            };

            Runnable saveRename = new Runnable() {
                private boolean processing = false;

                @Override
                public void run() {
                    if (processing)
                        return;
                    processing = true;
                    try {
                        String newName = tf.getText().trim();
                        if (!newName.isEmpty() && !newName.equals(header.title)) {
                            header.title = newName;
                            if (tabContent instanceof RequestPanel rp) {
                                rp.getRequestModel().setName(newName);
                                sidebarPanel.refreshCollections(collections);
                                sidebarPanel.refreshHistory(history);
                            } else if (tabContent instanceof CollectionRunnerPanel crp) {
                                crp.getRequestModel().setName(newName);
                                sidebarPanel.refreshCollections(collections);
                            } else if (tabContent instanceof DataComparatorPanel cp) {
                                cp.getRequestModel().setName(newName);
                            } else if (tabContent instanceof CollectionPanel cp) {
                                cp.getCollectionModel().setName(newName);
                                sidebarPanel.refreshCollections(collections);
                            }
                            saveCollections();

                            // Update the label and tooltip
                            String newDisplay = newName;
                            if (newDisplay.length() > 16) {
                                newDisplay = newDisplay.substring(0, 13) + "...";
                            }
                            titleLabel.setText(newDisplay);
                            titleLabel.setToolTipText(newName);
                        }
                    } finally {
                        tf.setVisible(false);
                        titleLabel.setVisible(true);
                        header.revalidate();
                        header.repaint();
                        processing = false;
                    }
                }
            };

            tf.addActionListener(ae -> saveRename.run());
            tf.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent fe) {
                    saveRename.run();
                }
            });
        } else {
            editField = null;
            startEdit = null;
        }

        final Runnable startEditAction = startEdit;
        MouseAdapter tabMouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = new JPopupMenu();
                    if (renameable && startEditAction != null) {
                        JMenuItem renameItem = new JMenuItem("Rename");
                        renameItem.addActionListener(ae -> startEditAction.run());
                        menu.add(renameItem);
                    }

                    boolean isPinned = Boolean.TRUE.equals(((JComponent) tabContent).getClientProperty("pinned"));
                    JMenuItem pinItem = new JMenuItem(isPinned ? "Unpin Tab" : "Pin Tab");
                    pinItem.addActionListener(ae -> togglePinTab(tabContent));
                    menu.add(pinItem);

                    menu.addSeparator();

                    JMenuItem closeItem = new JMenuItem("Close");
                    closeItem.addActionListener(ae -> closeTab(tabContent));
                    menu.add(closeItem);

                    JMenuItem closeOthersItem = new JMenuItem("Close Others");
                    closeOthersItem.addActionListener(ae -> closeOthers(tabContent));
                    menu.add(closeOthersItem);

                    JMenuItem closeLeftItem = new JMenuItem("Close to Left");
                    closeLeftItem.addActionListener(ae -> closeToLeft(tabContent));
                    menu.add(closeLeftItem);

                    JMenuItem closeRightItem = new JMenuItem("Close to Right");
                    closeRightItem.addActionListener(ae -> closeToRight(tabContent));
                    menu.add(closeRightItem);

                    JMenuItem closeAllItem = new JMenuItem("Close All");
                    closeAllItem.addActionListener(ae -> closeAllTabs());
                    menu.add(closeAllItem);

                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };
        header.addMouseListener(tabMouseListener);
        titleLabel.addMouseListener(tabMouseListener);

        JButton closeBtn = new JButton("×");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, currentFontSize));
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(null);
        closeBtn.setMargin(new Insets(0, 0, 0, 0));
        closeBtn.setPreferredSize(new Dimension(currentFontSize + 6, currentFontSize + 6));
        closeBtn.setToolTipText("Close tab");
        closeBtn.setForeground(Color.GRAY);
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(Color.RED);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(Color.GRAY);
            }
        });
        closeBtn.addActionListener(e -> closeTab(tabContent));

        header.add(titleLabel);
        if (renameable && editField != null) {
            header.add(editField);
        }

        boolean isPinned = Boolean.TRUE.equals(((JComponent) tabContent).getClientProperty("pinned"));
        if (isPinned) {
            JLabel pinLabel = new JLabel("📌");
            pinLabel.setFont(new Font("Segoe UI", Font.PLAIN, currentFontSize - 2));
            pinLabel.addMouseListener(tabMouseListener);
            header.add(pinLabel);
        } else {
            header.add(closeBtn);
        }
        return header;
    }

    // ─── Environments ─────────────────────────────────────────────────────────

    public List<EnvironmentModel> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<EnvironmentModel> envs) {
        this.environments = envs;
        storage.saveEnvironments(envs);
        refreshEnvCombo();
    }

    public void openEnvManager() {
        if (workspaceTabs.getTabCount() == 0) {
            workspaceCardLayout.show(workspacePanel, "tabs");
        }
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if (workspaceTabs.getComponentAt(i) instanceof EnvironmentManagerPanel) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        EnvironmentManagerPanel panel = new EnvironmentManagerPanel(this);
        panel.updateFontSize(currentFontSize);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("☁", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("☁", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openCookieJarManager() {
        if (workspaceTabs.getTabCount() == 0) {
            workspaceCardLayout.show(workspacePanel, "tabs");
        }
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if (workspaceTabs.getComponentAt(i) instanceof CookieJarPanel) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        CookieJarPanel panel = new CookieJarPanel(this);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Cookie Jar", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Cookie Jar", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openGlobalVariables() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if (workspaceTabs.getComponentAt(i) instanceof GlobalVariablesPanel) {
                workspaceTabs.setSelectedIndex(i);
                ((GlobalVariablesPanel) workspaceTabs.getComponentAt(i)).loadModel();
                return;
            }
        }
        GlobalVariablesPanel panel = new GlobalVariablesPanel(this);
        panel.updateFontSize(currentFontSize);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("🌐", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("🌐", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void triggerVariableRepaintAll() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof RequestPanel rp) {
                rp.triggerVariableRepaint();
            } else if (c instanceof CollectionPanel cp) {
                cp.triggerVariableRepaint();
            }
        }
    }

    // ─── History ─────────────────────────────────────────────────────────────

    public void addRequestToHistory(RequestModel req) {
        RequestModel historyItem = new RequestModel();
        historyItem.setId(UUID.randomUUID().toString());
        historyItem.setName(req.getName());
        historyItem.setMethod(req.getMethod());
        historyItem.setUrl(req.getUrl());
        historyItem.setHeaders(req.getHeaders() != null ? new ArrayList<>(req.getHeaders()) : new ArrayList<>());
        historyItem.setParams(req.getParams() != null ? new ArrayList<>(req.getParams()) : new ArrayList<>());
        historyItem.setBodyType(req.getBodyType());
        historyItem.setBodyRawContent(req.getBodyRawContent());
        historyItem.setBodyRawType(req.getBodyRawType());
        historyItem.setFormData(req.getFormData() != null ? new ArrayList<>(req.getFormData()) : new ArrayList<>());
        historyItem.setAuthType(req.getAuthType());
        historyItem.setAuthToken(req.getAuthToken());
        historyItem.setAuthUsername(req.getAuthUsername());
        historyItem.setAuthPassword(req.getAuthPassword());
        historyItem.setAuthApiKeyIn(req.getAuthApiKeyIn());
        historyItem.setAuthApiKeyName(req.getAuthApiKeyName());
        historyItem.setAuthApiKeyValue(req.getAuthApiKeyValue());
        historyItem.setPreRequestScript(req.getPreRequestScript());
        historyItem.setPostRequestScript(req.getPostRequestScript());
        historyItem.setType(req.getType());
        historyItem.setTimestamp(req.getTimestamp());
        historyItem.setResponseStatus(req.getResponseStatus());
        historyItem.setActualUrl(req.getActualUrl());

        history.add(0, historyItem);
        if (history.size() > MAX_HISTORY)
            history = new ArrayList<>(history.subList(0, MAX_HISTORY));
        storage.saveHistory(history);
        sidebarPanel.refreshHistory(history);
    }

    public void clearHistory() {
        history.clear();
        storage.saveHistory(history);
    }

    // ─── Import / Export ─────────────────────────────────────────────────────

    public SidebarPanel getSidebarPanel() {
        return sidebarPanel;
    }

    public void saveWorkspace() {
        storage.saveCollections(collections);
    }

    public void addCollection(CollectionModel collection) {
        collections.add(collection);
        if (sidebarPanel != null) {
            sidebarPanel.refreshCollections(collections);
        }
    }

    public File getLastFileChooserDirectory() {
        return lastFileChooserDirectory;
    }

    public void setLastFileChooserDirectory(File dir) {
        lastFileChooserDirectory = dir;
    }

    public void importOpenApiSpec() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if (workspaceTabs.getComponentAt(i) instanceof OpenApiImportPanel) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        OpenApiImportPanel panel = new OpenApiImportPanel(this);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("OpenAPI Import", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("OpenAPI Import", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void refreshEnvironmentPanels() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component comp = workspaceTabs.getComponentAt(i);
            if (comp instanceof EnvironmentManagerPanel emp) {
                emp.refreshEnvironments(environments);
            }
        }
    }

    public void importPostmanFiles() {
        JFileChooser chooser = new JFileChooser(lastFileChooserDirectory);
        chooser.setDialogTitle("Import Postman Files (Collections/Environments)");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter("Postman JSON files (*.json)", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File[] files = chooser.getSelectedFiles();
        if (files == null || files.length == 0) {
            return;
        }

        lastFileChooserDirectory = files[0].getParentFile();

        List<String> importedCollections = new ArrayList<>();
        List<String> importedEnvironments = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (File file : files) {
            try {
                String json = java.nio.file.Files.readString(file.toPath());
                // TODO: Remove legacy ApiBanker support in future release
                json = json.replace("ApiBanker.", "apibanker.");
                com.google.gson.JsonElement parsedElement = com.google.gson.JsonParser.parseString(json);
                if (!parsedElement.isJsonObject()) {
                    failedFiles.add(file.getName() + " (not a JSON Object)");
                    continue;
                }
                com.google.gson.JsonObject root = parsedElement.getAsJsonObject();

                if (root.has("item") || root.has("info")) {
                    // Import as Collection
                    CollectionModel col = new CollectionModel();
                    col.setId(UUID.randomUUID().toString());

                    com.google.gson.JsonObject info = root.has("info") ? root.getAsJsonObject("info") : null;
                    col.setName(info != null && info.has("name") ? info.get("name").getAsString()
                            : file.getName());

                    if (root.has("item") && root.get("item").isJsonArray()) {
                        parsePostmanItemsRecursive(root.getAsJsonArray("item"), col);
                    }
                    if (root.has("event") && root.get("event").isJsonArray()) {
                        for (com.google.gson.JsonElement evEl : root.getAsJsonArray("event")) {
                            com.google.gson.JsonObject ev = evEl.getAsJsonObject();
                            String listen = ev.has("listen") ? ev.get("listen").getAsString() : "";
                            if (ev.has("script") && ev.getAsJsonObject("script").has("exec")) {
                                StringBuilder sb = new StringBuilder();
                                for (com.google.gson.JsonElement line : ev.getAsJsonObject("script")
                                        .getAsJsonArray("exec")) {
                                    sb.append(line.getAsString()).append("\n");
                                }
                                String scriptText = sb.toString().replace("pm.", "apibanker.");
                                if ("prerequest".equals(listen))
                                    col.setPreRequestScript(scriptText);
                                else if ("test".equals(listen))
                                    col.setPostRequestScript(scriptText);
                            }
                        }
                    }
                    if (root.has("variable") && root.get("variable").isJsonArray()) {
                        List<KeyValueItem> vars = new ArrayList<>();
                        for (com.google.gson.JsonElement varEl : root.getAsJsonArray("variable")) {
                            com.google.gson.JsonObject vObj = varEl.getAsJsonObject();
                            String key = vObj.has("key") ? vObj.get("key").getAsString() : "";
                            String value = vObj.has("value") ? vObj.get("value").getAsString() : "";
                            vars.add(new KeyValueItem(key, value, true));
                        }
                        col.setVariables(vars);
                    }
                    collections.add(col);
                    int totalRequests = countRequestsRecursive(col);
                    importedCollections.add(col.getName() + " (" + totalRequests + " requests)");
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_COLLECTION", "User", "Format: Postman, Status: Success, Collection: [" + col.getName() + " / " + col.getId() + "], Requests: " + totalRequests + ", Imported From: " + file.getAbsolutePath());
                } else if (root.has("environments") && root.get("environments").isJsonArray()) {
                    // Postman Data Export containing multiple environments
                    for (com.google.gson.JsonElement envEl : root.getAsJsonArray("environments")) {
                        com.google.gson.JsonObject envObj = envEl.getAsJsonObject();
                        EnvironmentModel env = new EnvironmentModel();
                        env.setId(UUID.randomUUID().toString());
                        env.setName(envObj.has("name") ? envObj.get("name").getAsString() : "Imported");

                        List<KeyValueItem> vars = new ArrayList<>();
                        if (envObj.has("values") && envObj.get("values").isJsonArray()) {
                            for (com.google.gson.JsonElement el : envObj.getAsJsonArray("values")) {
                                com.google.gson.JsonObject v = el.getAsJsonObject();
                                String key = v.has("key") ? v.get("key").getAsString() : "";
                                String value = v.has("value") ? v.get("value").getAsString() : "";
                                boolean enabled = !v.has("enabled") || v.get("enabled").getAsBoolean();
                                vars.add(new KeyValueItem(key, value, enabled));
                            }
                        }
                        env.setVariables(vars);
                        environments.add(env);
                        importedEnvironments.add(env.getName());
                        in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_ENVIRONMENT", "User", "Format: Postman, Status: Success, Environment: [" + env.getName() + " / " + env.getId() + "], Imported From: " + file.getAbsolutePath());
                    }
                    
                    // Also import global variables as a separate environment if they exist
                    if (root.has("values") && root.get("values").isJsonArray()) {
                        EnvironmentModel globals = new EnvironmentModel();
                        globals.setId(UUID.randomUUID().toString());
                        globals.setName("Postman Globals");
                        List<KeyValueItem> globalVars = new ArrayList<>();
                        for (com.google.gson.JsonElement el : root.getAsJsonArray("values")) {
                            com.google.gson.JsonObject v = el.getAsJsonObject();
                            String key = v.has("key") ? v.get("key").getAsString() : "";
                            String value = v.has("value") ? v.get("value").getAsString() : "";
                            boolean enabled = !v.has("enabled") || v.get("enabled").getAsBoolean();
                            globalVars.add(new KeyValueItem(key, value, enabled));
                        }
                        if (!globalVars.isEmpty()) {
                            globals.setVariables(globalVars);
                            environments.add(globals);
                            importedEnvironments.add(globals.getName());
                            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_ENVIRONMENT", "User", "Format: Postman Globals, Status: Success, Environment: [" + globals.getName() + " / " + globals.getId() + "], Imported From: " + file.getAbsolutePath());
                        }
                    }
                } else if (root.has("values")) {
                    // Import as single Environment
                    EnvironmentModel env = new EnvironmentModel();
                    env.setId(UUID.randomUUID().toString());
                    env.setName(
                            root.has("name") ? root.get("name").getAsString() : file.getName().replace(".json", ""));

                    List<KeyValueItem> vars = new ArrayList<>();
                    if (root.has("values") && root.get("values").isJsonArray()) {
                        for (com.google.gson.JsonElement el : root.getAsJsonArray("values")) {
                            com.google.gson.JsonObject v = el.getAsJsonObject();
                            String key = v.has("key") ? v.get("key").getAsString() : "";
                            String value = v.has("value") ? v.get("value").getAsString() : "";
                            boolean enabled = !v.has("enabled") || v.get("enabled").getAsBoolean();
                            vars.add(new KeyValueItem(key, value, enabled));
                        }
                    }
                    env.setVariables(vars);
                    environments.add(env);
                    importedEnvironments.add(env.getName());
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_ENVIRONMENT", "User", "Format: Postman, Status: Success, Environment: [" + env.getName() + " / " + env.getId() + "], Imported From: " + file.getAbsolutePath());
                } else {
                    failedFiles.add(file.getName() + " (unrecognized Postman format)");
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_FAILED", "User", "Format: Postman, Status: Failed (unrecognized format), Imported From: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                failedFiles.add(file.getName() + " (" + e.getMessage() + ")");
                in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_FAILED", "User", "Format: Postman, Status: Error (" + e.getMessage() + "), Imported From: " + file.getAbsolutePath());
            }
        }

        if (!importedCollections.isEmpty()) {
            saveCollections();
            sidebarPanel.refreshCollections(collections);
        }

        if (!importedEnvironments.isEmpty()) {
            setEnvironments(environments);
            refreshEnvironmentPanels();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Import Results:\n\n");
        if (!importedCollections.isEmpty()) {
            sb.append("Imported Collections:\n");
            for (String colName : importedCollections) {
                sb.append(" - ").append(colName).append("\n");
            }
            sb.append("\n");
        }
        if (!importedEnvironments.isEmpty()) {
            sb.append("Imported Environments:\n");
            for (String envName : importedEnvironments) {
                sb.append(" - ").append(envName).append("\n");
            }
            sb.append("\n");
        }
        if (!failedFiles.isEmpty()) {
            sb.append("Failed/Skipped Files:\n");
            for (String failDetail : failedFiles) {
                sb.append(" - ").append(failDetail).append("\n");
            }
        }

        JOptionPane.showMessageDialog(this, sb.toString(), "Import Summary", JOptionPane.INFORMATION_MESSAGE);
    }

    public void importApiBankerFiles() {
        JFileChooser chooser = new JFileChooser(lastFileChooserDirectory);
        chooser.setDialogTitle("Import ApiBanker Files (Collections/Environments)");
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter("ApiBanker JSON files (*.json)", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        File[] files = chooser.getSelectedFiles();
        if (files == null || files.length == 0)
            return;
        lastFileChooserDirectory = files[0].getParentFile();

        List<String> importedCollections = new ArrayList<>();
        List<String> importedEnvironments = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        com.google.gson.Gson gson = new com.google.gson.Gson();

        for (File file : files) {
            try {
                String jsonContent = java.nio.file.Files.readString(file.toPath(),
                        java.nio.charset.StandardCharsets.UTF_8);
                // TODO: Remove legacy ApiBanker support in future release
                jsonContent = jsonContent.replace("ApiBanker.", "apibanker.");
                com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(jsonContent).getAsJsonObject();
                if (root.has("folders") && root.has("requests")) {
                    CollectionModel col = gson.fromJson(root, CollectionModel.class);
                    col.setId(UUID.randomUUID().toString()); // new ID to avoid clash
                    collections.add(col);
                    importedCollections.add(col.getName());
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_COLLECTION", "User", "Format: ApiBanker, Status: Success, Collection: [" + col.getName() + " / " + col.getId() + "], Imported From: " + file.getAbsolutePath());
                } else if (root.has("variables")) {
                    EnvironmentModel env = gson.fromJson(root, EnvironmentModel.class);
                    env.setId(UUID.randomUUID().toString());
                    environments.add(env);
                    importedEnvironments.add(env.getName());
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_ENVIRONMENT", "User", "Format: ApiBanker, Status: Success, Environment: [" + env.getName() + " / " + env.getId() + "], Imported From: " + file.getAbsolutePath());
                } else {
                    failedFiles.add(file.getName() + " (Unknown format)");
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_FAILED", "User", "Format: ApiBanker, Status: Failed (Unknown format), Imported From: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                failedFiles.add(file.getName() + " (" + e.getMessage() + ")");
                in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_FAILED", "User", "Format: ApiBanker, Status: Error (" + e.getMessage() + "), Imported From: " + file.getAbsolutePath());
            }
        }

        if (!importedCollections.isEmpty()) {
            saveCollections();
            sidebarPanel.refreshCollections(collections);
        }

        if (!importedEnvironments.isEmpty()) {
            setEnvironments(environments);
            for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
                Component comp = workspaceTabs.getComponentAt(i);
                if (comp instanceof EnvironmentManagerPanel emp) {
                    emp.refreshEnvironments(environments);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Import Results:\n\n");
        if (!importedCollections.isEmpty()) {
            sb.append("Imported Collections:\n");
            for (String colName : importedCollections)
                sb.append(" - ").append(colName).append("\n");
            sb.append("\n");
        }
        if (!importedEnvironments.isEmpty()) {
            sb.append("Imported Environments:\n");
            for (String envName : importedEnvironments)
                sb.append(" - ").append(envName).append("\n");
            sb.append("\n");
        }
        if (!failedFiles.isEmpty()) {
            sb.append("Failed/Skipped Files:\n");
            for (String failDetail : failedFiles)
                sb.append(" - ").append(failDetail).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Import Summary", JOptionPane.INFORMATION_MESSAGE);
    }

    private int countRequestsRecursive(CollectionModel col) {
        int count = col.getRequests().size();
        for (CollectionModel folder : col.getFolders()) {
            count += countRequestsRecursive(folder);
        }
        return count;
    }

    private void parsePostmanItemsRecursive(com.google.gson.JsonArray items, CollectionModel parent) {
        for (com.google.gson.JsonElement el : items) {
            com.google.gson.JsonObject item = el.getAsJsonObject();
            if (item.has("item")) {
                // Folder
                CollectionModel subFolder = new CollectionModel();
                subFolder.setId(UUID.randomUUID().toString());
                subFolder.setName(item.has("name") ? item.get("name").getAsString() : "Folder");
                if (item.has("event") && item.get("event").isJsonArray()) {
                    for (com.google.gson.JsonElement evEl : item.getAsJsonArray("event")) {
                        com.google.gson.JsonObject ev = evEl.getAsJsonObject();
                        String listen = ev.has("listen") ? ev.get("listen").getAsString() : "";
                        if (ev.has("script") && ev.getAsJsonObject("script").has("exec")) {
                            StringBuilder sb = new StringBuilder();
                            for (com.google.gson.JsonElement line : ev.getAsJsonObject("script")
                                    .getAsJsonArray("exec")) {
                                sb.append(line.getAsString()).append("\n");
                            }
                            String scriptText = sb.toString().replace("pm.", "apibanker.");
                            if ("prerequest".equals(listen))
                                subFolder.setPreRequestScript(scriptText);
                            else if ("test".equals(listen))
                                subFolder.setPostRequestScript(scriptText);
                        }
                    }
                }
                parsePostmanItemsRecursive(item.getAsJsonArray("item"), subFolder);
                parent.getFolders().add(subFolder);
            } else if (item.has("request")) {
                RequestModel req = new RequestModel();
                req.setId(UUID.randomUUID().toString());
                req.setName(item.has("name") ? item.get("name").getAsString() : "Request");
                if (item.has("event") && item.get("event").isJsonArray()) {
                    for (com.google.gson.JsonElement evEl : item.getAsJsonArray("event")) {
                        com.google.gson.JsonObject ev = evEl.getAsJsonObject();
                        String listen = ev.has("listen") ? ev.get("listen").getAsString() : "";
                        if (ev.has("script") && ev.getAsJsonObject("script").has("exec")) {
                            StringBuilder sb = new StringBuilder();
                            for (com.google.gson.JsonElement line : ev.getAsJsonObject("script")
                                    .getAsJsonArray("exec")) {
                                sb.append(line.getAsString()).append("\n");
                            }
                            String scriptText = sb.toString().replace("pm.", "apibanker.");
                            if ("prerequest".equals(listen))
                                req.setPreRequestScript(scriptText);
                            else if ("test".equals(listen))
                                req.setPostRequestScript(scriptText);
                        }
                    }
                }
                com.google.gson.JsonObject reqObj = item.getAsJsonObject("request");
                req.setMethod(reqObj.has("method") ? reqObj.get("method").getAsString() : "GET");

                if (reqObj.has("url")) {
                    com.google.gson.JsonElement urlEl = reqObj.get("url");
                    if (urlEl.isJsonObject()) {
                        req.setUrl(urlEl.getAsJsonObject().has("raw") ? urlEl.getAsJsonObject().get("raw").getAsString()
                                : "");
                    } else {
                        req.setUrl(urlEl.getAsString());
                    }
                }

                if (reqObj.has("header") && reqObj.get("header").isJsonArray()) {
                    List<KeyValueItem> headers = new ArrayList<>();
                    for (com.google.gson.JsonElement h : reqObj.getAsJsonArray("header")) {
                        com.google.gson.JsonObject hObj = h.getAsJsonObject();
                        headers.add(new KeyValueItem(
                                hObj.has("key") ? hObj.get("key").getAsString() : "",
                                hObj.has("value") ? hObj.get("value").getAsString() : "",
                                !hObj.has("disabled") || !hObj.get("disabled").getAsBoolean()));
                    }
                    req.setHeaders(headers);
                }

                if (reqObj.has("body")) {
                    com.google.gson.JsonObject body = reqObj.getAsJsonObject("body");
                    String mode = body.has("mode") ? body.get("mode").getAsString() : "none";
                    if ("raw".equals(mode)) {
                        req.setBodyType("raw");
                        req.setBodyRawContent(body.has("raw") ? body.get("raw").getAsString() : "");
                        if (body.has("options")) {
                            com.google.gson.JsonObject opts = body.getAsJsonObject("options");
                            if (opts.has("raw")) {
                                com.google.gson.JsonObject rawOpts = opts.getAsJsonObject("raw");
                                String language = rawOpts.has("language") ? rawOpts.get("language").getAsString()
                                        : "json";
                                if ("json".equalsIgnoreCase(language)) {
                                    req.setBodyRawType("JSON");
                                } else if ("html".equalsIgnoreCase(language)) {
                                    req.setBodyRawType("HTML");
                                } else if ("xml".equalsIgnoreCase(language)) {
                                    req.setBodyRawType("XML");
                                } else if ("javascript".equalsIgnoreCase(language)) {
                                    req.setBodyRawType("JavaScript");
                                } else {
                                    req.setBodyRawType("Text");
                                }
                            }
                        }
                    } else if ("formdata".equals(mode)) {
                        req.setBodyType("form-data");
                        List<KeyValueItem> fd = new ArrayList<>();
                        if (body.has("formdata") && body.get("formdata").isJsonArray()) {
                            for (com.google.gson.JsonElement fEl : body.getAsJsonArray("formdata")) {
                                com.google.gson.JsonObject fObj = fEl.getAsJsonObject();
                                KeyValueItem kv = new KeyValueItem(
                                        fObj.has("key") ? fObj.get("key").getAsString() : "",
                                        fObj.has("value") ? fObj.get("value").getAsString() : "",
                                        !fObj.has("disabled") || !fObj.get("disabled").getAsBoolean());
                                kv.setType(fObj.has("type") ? fObj.get("type").getAsString() : "text");
                                fd.add(kv);
                            }
                        }
                        req.setFormData(fd);
                    } else if ("urlencoded".equals(mode)) {
                        req.setBodyType("x-www-form-urlencoded");
                        List<KeyValueItem> ue = new ArrayList<>();
                        if (body.has("urlencoded") && body.get("urlencoded").isJsonArray()) {
                            for (com.google.gson.JsonElement uEl : body.getAsJsonArray("urlencoded")) {
                                com.google.gson.JsonObject uObj = uEl.getAsJsonObject();
                                ue.add(new KeyValueItem(
                                        uObj.has("key") ? uObj.get("key").getAsString() : "",
                                        uObj.has("value") ? uObj.get("value").getAsString() : "",
                                        !uObj.has("disabled") || !uObj.get("disabled").getAsBoolean()));
                            }
                        }
                        req.setUrlencodedData(ue);
                    } else if ("graphql".equals(mode)) {
                        req.setBodyType("graphql");
                        if (body.has("graphql")) {
                            com.google.gson.JsonObject gqlObj = body.getAsJsonObject("graphql");
                            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                            json.addProperty("query", gqlObj.has("query") ? gqlObj.get("query").getAsString() : "");
                            json.addProperty("variables",
                                    gqlObj.has("variables") ? gqlObj.get("variables").getAsString() : "");
                            req.setBodyRawContent(json.toString());
                        }
                    }
                }
                parent.getRequests().add(req);
            }
        }
    }

    public void exportCollection(CollectionModel col) {
        JFileChooser chooser = new JFileChooser(lastFileChooserDirectory);
        chooser.setSelectedFile(new File(col.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + "_postman_collection.json"));
        chooser.setDialogTitle("Export Collection");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        lastFileChooserDirectory = chooser.getSelectedFile().getParentFile();
        try {
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            com.google.gson.JsonObject info = new com.google.gson.JsonObject();
            info.addProperty("name", col.getName());
            info.addProperty("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
            info.addProperty("_exporter_id", "ApiBanker-" + in.slpro.apibanker.App.getVersion());
            info.addProperty("_exported_by",
                    "ApiBanker v" + in.slpro.apibanker.App.getVersion() + " (Offline API Client)");
            info.addProperty("_exported_at",
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date()));
            root.add("info", info);
            root.addProperty("_apibanker_version", in.slpro.apibanker.App.getVersion());
            com.google.gson.JsonArray items = new com.google.gson.JsonArray();
            exportCollectionRecursive(col, items);
            root.add("item", items);

            if (col.getVariables() != null && !col.getVariables().isEmpty()) {
                com.google.gson.JsonArray varsArr = new com.google.gson.JsonArray();
                for (KeyValueItem kv : col.getVariables()) {
                    if (kv.isEnabled()) {
                        com.google.gson.JsonObject vObj = new com.google.gson.JsonObject();
                        vObj.addProperty("key", kv.getKey());
                        vObj.addProperty("value", kv.getValue());
                        vObj.addProperty("type", "string");
                        varsArr.add(vObj);
                    }
                }
                if (varsArr.size() > 0)
                    root.add("variable", varsArr);
            }

            com.google.gson.JsonArray events = new com.google.gson.JsonArray();
            if (col.getPreRequestScript() != null && !col.getPreRequestScript().isEmpty()) {
                com.google.gson.JsonObject ev = new com.google.gson.JsonObject();
                ev.addProperty("listen", "prerequest");
                com.google.gson.JsonObject script = new com.google.gson.JsonObject();
                script.addProperty("type", "text/javascript");
                com.google.gson.JsonArray exec = new com.google.gson.JsonArray();
                for (String line : col.getPreRequestScript().replace("apibanker.", "pm.").split("\n"))
                    exec.add(line);
                script.add("exec", exec);
                ev.add("script", script);
                events.add(ev);
            }
            if (col.getPostRequestScript() != null && !col.getPostRequestScript().isEmpty()) {
                com.google.gson.JsonObject ev = new com.google.gson.JsonObject();
                ev.addProperty("listen", "test");
                com.google.gson.JsonObject script = new com.google.gson.JsonObject();
                script.addProperty("type", "text/javascript");
                com.google.gson.JsonArray exec = new com.google.gson.JsonArray();
                for (String line : col.getPostRequestScript().replace("apibanker.", "pm.").split("\n"))
                    exec.add(line);
                script.add("exec", exec);
                ev.add("script", script);
                events.add(ev);
            }
            if (events.size() > 0)
                root.add("event", events);

            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), gson.toJson(root));
            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("EXPORT_COLLECTION", "User", "Format: Postman, Source: [" + col.getName() + " / " + col.getId() + "] -> Exported to: " + chooser.getSelectedFile().getAbsolutePath());
            showToast(this, "Collection exported successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportCollectionRecursive(CollectionModel col, com.google.gson.JsonArray items) {
        if (col.getFolders() != null) {
            for (CollectionModel folder : col.getFolders()) {
                com.google.gson.JsonObject folderObj = new com.google.gson.JsonObject();
                folderObj.addProperty("name", folder.getName());

                com.google.gson.JsonArray events = new com.google.gson.JsonArray();
                if (folder.getPreRequestScript() != null && !folder.getPreRequestScript().isEmpty()) {
                    com.google.gson.JsonObject ev = new com.google.gson.JsonObject();
                    ev.addProperty("listen", "prerequest");
                    com.google.gson.JsonObject script = new com.google.gson.JsonObject();
                    script.addProperty("type", "text/javascript");
                    com.google.gson.JsonArray exec = new com.google.gson.JsonArray();
                    for (String line : folder.getPreRequestScript().replace("apibanker.", "pm.").split("\n"))
                        exec.add(line);
                    script.add("exec", exec);
                    ev.add("script", script);
                    events.add(ev);
                }
                if (folder.getPostRequestScript() != null && !folder.getPostRequestScript().isEmpty()) {
                    com.google.gson.JsonObject ev = new com.google.gson.JsonObject();
                    ev.addProperty("listen", "test");
                    com.google.gson.JsonObject script = new com.google.gson.JsonObject();
                    script.addProperty("type", "text/javascript");
                    com.google.gson.JsonArray exec = new com.google.gson.JsonArray();
                    for (String line : folder.getPostRequestScript().replace("apibanker.", "pm.").split("\n"))
                        exec.add(line);
                    script.add("exec", exec);
                    ev.add("script", script);
                    events.add(ev);
                }
                if (events.size() > 0)
                    folderObj.add("event", events);

                if (folder.getVariables() != null && !folder.getVariables().isEmpty()) {
                    com.google.gson.JsonArray varsArr = new com.google.gson.JsonArray();
                    for (KeyValueItem kv : folder.getVariables()) {
                        if (kv.isEnabled()) {
                            com.google.gson.JsonObject vObj = new com.google.gson.JsonObject();
                            vObj.addProperty("key", kv.getKey());
                            vObj.addProperty("value", kv.getValue());
                            vObj.addProperty("type", "string");
                            varsArr.add(vObj);
                        }
                    }
                    if (varsArr.size() > 0)
                        folderObj.add("variable", varsArr);
                }

                com.google.gson.JsonArray folderItems = new com.google.gson.JsonArray();
                exportCollectionRecursive(folder, folderItems);
                folderObj.add("item", folderItems);
                items.add(folderObj);
            }
        }
        if (col.getRequests() != null) {
            for (RequestModel req : col.getRequests()) {
                if ("runner".equals(req.getType()))
                    continue;
                com.google.gson.JsonObject item = new com.google.gson.JsonObject();
                item.addProperty("name", req.getName());
                com.google.gson.JsonObject reqObj = new com.google.gson.JsonObject();
                reqObj.addProperty("method", req.getMethod());
                com.google.gson.JsonObject url = new com.google.gson.JsonObject();
                url.addProperty("raw", req.getUrl());
                try {
                    String urlStr = req.getUrl();
                    if (urlStr != null && !urlStr.isEmpty()) {
                        String withoutProtocol = urlStr;
                        if (urlStr.contains("://")) {
                            String[] protoParts = urlStr.split("://", 2);
                            url.addProperty("protocol", protoParts[0]);
                            withoutProtocol = protoParts[1];
                        }
                        String[] parts = withoutProtocol.split("/", 2);
                        String hostStr = parts[0];
                        String pathStr = parts.length > 1 ? parts[1] : "";

                        if (hostStr.contains(":")) {
                            String[] hostParts = hostStr.split(":", 2);
                            com.google.gson.JsonArray hostArr = new com.google.gson.JsonArray();
                            for (String h : hostParts[0].split("\\."))
                                hostArr.add(h);
                            url.add("host", hostArr);
                            url.addProperty("port", hostParts[1]);
                        } else {
                            com.google.gson.JsonArray hostArr = new com.google.gson.JsonArray();
                            for (String h : hostStr.split("\\."))
                                hostArr.add(h);
                            url.add("host", hostArr);
                        }
                        com.google.gson.JsonArray pathArr = new com.google.gson.JsonArray();
                        for (String p : pathStr.split("/", -1))
                            pathArr.add(p);
                        url.add("path", pathArr);
                    }
                } catch (Exception ignored) {
                }
                reqObj.add("url", url);
                item.add("request", reqObj);

                com.google.gson.JsonArray events = new com.google.gson.JsonArray();
                if (req.getPreRequestScript() != null && !req.getPreRequestScript().isEmpty()) {
                    com.google.gson.JsonObject ev = new com.google.gson.JsonObject();
                    ev.addProperty("listen", "prerequest");
                    com.google.gson.JsonObject script = new com.google.gson.JsonObject();
                    script.addProperty("type", "text/javascript");
                    com.google.gson.JsonArray exec = new com.google.gson.JsonArray();
                    for (String line : req.getPreRequestScript().replace("apibanker.", "pm.").split("\n"))
                        exec.add(line);
                    script.add("exec", exec);
                    ev.add("script", script);
                    events.add(ev);
                }
                if (req.getPostRequestScript() != null && !req.getPostRequestScript().isEmpty()) {
                    com.google.gson.JsonObject ev = new com.google.gson.JsonObject();
                    ev.addProperty("listen", "test");
                    com.google.gson.JsonObject script = new com.google.gson.JsonObject();
                    script.addProperty("type", "text/javascript");
                    com.google.gson.JsonArray exec = new com.google.gson.JsonArray();
                    for (String line : req.getPostRequestScript().replace("apibanker.", "pm.").split("\n"))
                        exec.add(line);
                    script.add("exec", exec);
                    ev.add("script", script);
                    events.add(ev);
                }
                if (events.size() > 0)
                    item.add("event", events);

                items.add(item);
            }
        }
    }

    public void exportApiBankerCollection(CollectionModel col) {
        JFileChooser chooser = new JFileChooser(lastFileChooserDirectory);
        chooser.setSelectedFile(
                new File(col.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + "_apibanker_collection.json"));
        chooser.setDialogTitle("Export ApiBanker Collection");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        lastFileChooserDirectory = chooser.getSelectedFile().getParentFile();
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            com.google.gson.JsonObject root = gson.toJsonTree(col).getAsJsonObject();
            com.google.gson.JsonObject metadata = new com.google.gson.JsonObject();
            metadata.addProperty("exported_by", "ApiBanker v" + in.slpro.apibanker.App.getVersion());
            metadata.addProperty("exported_at",
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date()));
            root.add("_metadata", metadata);
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), gson.toJson(root));
            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("EXPORT_COLLECTION", "User", "Format: ApiBanker, Source: [" + col.getName() + " / " + col.getId() + "] -> Exported to: " + chooser.getSelectedFile().getAbsolutePath());
            showToast(this, "ApiBanker Collection exported successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    public void openSettings() {
        if (workspaceTabs.getTabCount() == 0) {
            workspaceCardLayout.show(workspacePanel, "tabs");
        }

        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if (workspaceTabs.getComponentAt(i) instanceof SettingsPanel) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }

        SettingsPanel panel = new SettingsPanel(this);
        panel.updateFontSize(currentFontSize);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("⚙", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("⚙", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openWelcomeTabAsTab() {
        if (workspaceTabs.getTabCount() == 0) {
            return; // Already showing welcome page
        }

        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if ("Welcome".equals(workspaceTabs.getTitleAt(i))) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }

        JPanel welcomePanel = buildWelcomePanel();
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Welcome", welcomePanel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Welcome", idx, welcomePanel));
        workspaceTabs.setSelectedIndex(idx);
    }

    private void showAbout() {
        openWelcomeTabAsTab();
    }

    /**
     * Opens a Features panel that renders README.md with Markdown-to-HTML
     * conversion.
     */
    private void openFeaturesPanel() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if ("Features".equals(workspaceTabs.getTitleAt(i))) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        Color ac = javax.swing.UIManager.getColor("AccentColor");
        if (ac == null)
            ac = new java.awt.Color(26, 115, 232);
        String aHex = String.format("#%02x%02x%02x", ac.getRed(), ac.getGreen(), ac.getBlue());
        boolean dk = com.formdev.flatlaf.FlatLaf.isLafDark();
        String fg = dk ? "#cccccc" : "#333333";
        String bg = dk ? "#1e1e1e" : "#fafafa";
        String cbg = dk ? "#2b2b2b" : "#f0f4f8";
        String bdr = dk ? "#3a3a3a" : "#e0e0e0";

        // Read README.md
        String md = "";
        try (java.io.InputStream is = MainFrame.class.getResourceAsStream("/README.md") != null 
                ? MainFrame.class.getResourceAsStream("/README.md") 
                : MainFrame.class.getClassLoader().getResourceAsStream("README.md")) {
            if (is != null) {
                md = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } else {
                md = "# Features\n\nNo README.md found.";
            }
        } catch (Exception ignored) {
            md = "# Features\n\nNo README.md found.";
        }
        if (md.isBlank())
            md = "# Features\n\nNo README.md found.";

        // Markdown -> HTML
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:\"Segoe UI\",Arial,sans-serif;font-size:14px;margin:30px 40px;color:")
                .append(fg).append(";background:").append(bg).append(";'>");
        boolean inList = false;
        for (String raw : md.split("\n")) {
            String t = raw.trim();
            boolean isBullet = t.startsWith("- ") || t.startsWith("* ");
            if (!isBullet && inList) {
                sb.append("</ul>");
                inList = false;
            }
            if (t.startsWith("### ")) {
                sb.append("<h3 style='color:").append(aHex).append(";margin:16px 0 4px;'>")
                        .append(mdInline(t.substring(4), aHex, cbg)).append("</h3>");
            } else if (t.startsWith("## ")) {
                sb.append("<h2 style='color:").append(aHex).append(";border-bottom:2px solid ").append(aHex)
                        .append(";padding-bottom:5px;margin-top:28px;'>").append(mdInline(t.substring(3), aHex, cbg))
                        .append("</h2>");
            } else if (t.startsWith("# ")) {
                sb.append("<h1 style='color:").append(aHex).append(";font-size:24px;margin-bottom:4px;'>")
                        .append(mdInline(t.substring(2), aHex, cbg)).append("</h1>");
            } else if (t.startsWith("---")) {
                sb.append("<hr style='border:none;border-top:1px solid ").append(bdr).append(";margin:14px 0;'>");
            } else if (isBullet) {
                if (!inList) {
                    sb.append("<ul style='font-size:13px;line-height:1.8;margin:4px 0;padding-left:20px;'>");
                    inList = true;
                }
                sb.append("<li>").append(mdInline(t.substring(2), aHex, cbg)).append("</li>");
            } else if (t.isEmpty()) {
                sb.append("<br>");
            } else {
                sb.append("<p style='font-size:13px;line-height:1.7;margin:4px 0;'>").append(mdInline(t, aHex, cbg))
                        .append("</p>");
            }
        }
        if (inList)
            sb.append("</ul>");
        sb.append("</body></html>");

        javax.swing.JTextPane pane = new javax.swing.JTextPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(javax.swing.UIManager.getColor("Panel.background"));
        pane.setText(sb.toString());
        pane.setCaretPosition(0);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(pane);
        scroll.setBorder(null);
        javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
        wrapper.add(scroll, java.awt.BorderLayout.CENTER);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Features", wrapper);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Features", idx, wrapper));
        workspaceTabs.setSelectedIndex(idx);
    }

    private String mdInline(String text, String aHex, String cbg) {
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        text = text.replaceAll("\\*(.+?)\\*", "<i>$1</i>");
        text = text.replaceAll("`([^`]+)`",
                "<code style='background:" + cbg + ";padding:1px 5px;border-radius:3px;font-size:12px;'>$1</code>");
        text = text.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "<a href='$2' style='color:" + aHex + ";'>$1</a>");
        return text;
    }

    /**
     * Opens the User Guide tab in the workspace.
     */
    private void openDocumentationPanel() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if ("User Guide".equals(workspaceTabs.getTitleAt(i))) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        Color ac = javax.swing.UIManager.getColor("AccentColor");
        if (ac == null)
            ac = new java.awt.Color(26, 115, 232);
        String a = String.format("#%02x%02x%02x", ac.getRed(), ac.getGreen(), ac.getBlue());
        boolean dk = com.formdev.flatlaf.FlatLaf.isLafDark();
        String bg = dk ? "#1e1e1e" : "#fafafa";
        String fg = dk ? "#cccccc" : "#333333";
        String card = dk ? "#2b2b2b" : "#ffffff";
        String bdr = dk ? "#3a3a3a" : "#e0e0e0";
        String cbg = dk ? "#1a1a1a" : "#f0f4f8";
        String ver = in.slpro.apibanker.App.getVersion();
        String html = buildDocHtml(a, bg, fg, card, bdr, cbg, ver);
        javax.swing.JTextPane pane = new javax.swing.JTextPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(javax.swing.UIManager.getColor("Panel.background"));
        pane.setText(html);
        pane.setCaretPosition(0);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(pane);
        scroll.setBorder(null);
        javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
        wrapper.add(scroll, java.awt.BorderLayout.CENTER);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("User Guide", wrapper);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("User Guide", idx, wrapper));
        workspaceTabs.setSelectedIndex(idx);
    }

    private String buildDocHtml(String a, String bg, String fg, String card, String bdr, String cbg, String ver) {
        String sec = "font-family:'Segoe UI',Arial,sans-serif;color:" + fg + ";background:" + bg
                + ";padding:30px 40px;";
        String h1s = "color:" + a + ";font-size:28px;margin:0 0 4px 0;";
        String h2s = "color:" + a + ";font-size:18px;border-bottom:2px solid " + a
                + ";padding-bottom:6px;margin-top:36px;margin-bottom:12px;";
        String h3s = "color:" + a + ";font-size:14px;margin:16px 0 4px 0;";
        String ps = "font-size:13px;line-height:1.7;margin:4px 0 10px 0;";
        String lis = "font-size:13px;line-height:1.8;margin:0;padding-left:20px;";
        String cds = "background:" + cbg + ";border:1px solid " + bdr
                + ";border-radius:4px;padding:1px 5px;font-family:monospace;font-size:12px;";
        String pre = "background:" + cbg + ";border:1px solid " + bdr
                + ";border-radius:6px;padding:12px 16px;font-family:monospace;font-size:12px;margin:8px 0;";
        String cs = "background:" + card + ";border:1px solid " + bdr
                + ";border-radius:8px;padding:16px 20px;margin:10px 0;";
        String tds = "border:1px solid " + bdr + ";padding:7px 12px;font-size:13px;";
        String ths = tds + "background:" + cbg + ";font-weight:bold;color:" + a + ";";

        return "<html><body style='" + sec + "'>"

        // Header
                + "<div style='text-align:center;margin-bottom:24px;'>"
                + "<h1 style='" + h1s + "'>&#128196; ApiBanker User Guide</h1>"
                + "<div style='font-size:12px;color:#888;'>Version " + ver
                + " &nbsp;|&nbsp; Offline-First API Client &nbsp;|&nbsp; From SL Pro</div>"
                + "</div>"
                + "<hr style='border:none;border-top:1px solid " + bdr + ";margin-bottom:28px;'>"

                // TOC
                + "<div style='" + cs + "'>"
                + "<b style='color:" + a + ";'>Contents</b><br><br>"
                + "<span style='font-size:13px;line-height:2;'>"
                + "1. Making API Requests &nbsp;&nbsp; 2. Collections &amp; Folders &nbsp;&nbsp; 3. Environments &amp; Variables<br>"
                + "4. Authentication &nbsp;&nbsp; 5. Scripting Engine &nbsp;&nbsp; 6. OpenAPI / Swagger Import<br>"
                + "7. Collection Runner &nbsp;&nbsp; 8. Built-in Tools &nbsp;&nbsp; 9. Keyboard Shortcuts &nbsp;&nbsp; 10. Common Use Cases &nbsp;&nbsp; 11. UI &amp; Accessibility"
                + "</span></div>"

                // 1. Making API Requests
                + "<h2 style='" + h2s + "'>1. &#128640; Making API Requests</h2>"
                + "<p style='" + ps
                + "'>The <b>Request Panel</b> is the core workspace. Open a new request from the sidebar or via <b>File &rarr; New Request</b>.</p>"
                + "<div style='" + cs + "'>"
                + "<h3 style='" + h3s + "'>Steps</h3>"
                + "<ol style='" + lis + "'>"
                + "<li>Select the HTTP method (GET, POST, PUT, PATCH, DELETE) from the dropdown.</li>"
                + "<li>Enter the request URL. Use <code style='" + cds
                + "'>{{variableName}}</code> for dynamic values.</li>"
                + "<li>Add <b>Query Params</b>, <b>Headers</b>, or a <b>Body</b> via the tabs below the URL bar.</li>"
                + "<li>Press <b>Send</b> or use <code style='" + cds + "'>Ctrl+R</code> to execute.</li>"
                + "<li>View the response status, latency, size, headers, and formatted body in the Response panel.</li>"
                + "</ol></div>"
                + "<h3 style='" + h3s + "'>Body Types Supported</h3>"
                + "<ul style='" + lis + "'>"
                + "<li><b>raw JSON / XML / HTML / Text</b> &mdash; with syntax highlighting</li>"
                + "<li><b>form-data</b> &mdash; key-value pairs and file uploads</li>"
                + "<li><b>x-www-form-urlencoded</b> &mdash; URL-encoded key-value pairs</li>"
                + "<li><b>GraphQL</b> &mdash; query + variables editor with schema introspection</li>"
                + "</ul>"

                // 2. Collections
                + "<h2 style='" + h2s + "'>2. &#128193; Collections &amp; Folders</h2>"
                + "<p style='" + ps
                + "'>Collections group related requests. Folders allow nested organisation within a Collection.</p>"
                + "<div style='" + cs + "'>"
                + "<h3 style='" + h3s + "'>How to Organise</h3>"
                + "<ul style='" + lis + "'>"
                + "<li><b>Right-click</b> in the sidebar tree to add a Collection, Folder, or Request.</li>"
                + "<li>Drag requests into folders, or use <b>Ctrl+D</b> to duplicate.</li>"
                + "<li><b>F2</b> to rename; <b>Delete</b> to remove any selected node.</li>"
                + "<li>Open the <b>Collection Details</b> panel to add a <b>README</b> (Markdown), set Auth, or manage Variables.</li>"
                + "<li>Use <b>Export</b> to save the Collection as a Postman v2.1 JSON or JMeter .jmx file.</li>"
                + "</ul></div>"

                // 3. Environments
                + "<h2 style='" + h2s + "'>3. &#127758; Environments &amp; Variables</h2>"
                + "<p style='" + ps
                + "'>Variables let you swap base URLs, API keys, and tokens without editing each request manually.</p>"
                + "<h3 style='" + h3s + "'>Variable Scopes (in priority order)</h3>"
                + "<table width='100%' cellspacing='0' style='border-collapse:collapse;margin:8px 0;'>"
                + "<tr><th style='" + ths + "'>Scope</th><th style='" + ths + "'>Where to Manage</th><th style='" + ths
                + "'>Syntax</th></tr>"
                + "<tr><td style='" + tds + "'>Global</td><td style='" + tds + "'>Global Variables tab</td><td style='"
                + tds + "'><code style='" + cds + "'>{{var}}</code></td></tr>"
                + "<tr><td style='" + tds + "'>Environment</td><td style='" + tds
                + "'>Environment Manager (top-right dropdown)</td><td style='" + tds + "'><code style='" + cds
                + "'>{{var}}</code></td></tr>"
                + "<tr><td style='" + tds + "'>Collection</td><td style='" + tds
                + "'>Collection Details &rarr; Variables tab</td><td style='" + tds + "'><code style='" + cds
                + "'>{{var}}</code></td></tr>"
                + "</table>"
                + "<div style='" + cs
                + "'><b>Tip:</b> Variables now resolve recursively! A collection variable can safely reference an environment or global variable for ultimate dynamic flexibility. Active environment variables override collection variables. Use <b>Ctrl+E</b> or the top-right dropdown to switch environments quickly.</div>"

                // 4. Auth
                + "<h2 style='" + h2s + "'>4. &#128272; Authentication</h2>"
                + "<p style='" + ps
                + "'>Set auth at the <b>Collection level</b> and all child requests inherit it automatically.</p>"
                + "<table width='100%' cellspacing='0' style='border-collapse:collapse;margin:8px 0;'>"
                + "<tr><th style='" + ths + "'>Type</th><th style='" + ths + "'>How to Configure</th></tr>"
                + "<tr><td style='" + tds + "'><b>Bearer Token</b></td><td style='" + tds
                + "'>Paste the token in the Auth tab &rarr; Bearer field.</td></tr>"
                + "<tr><td style='" + tds + "'><b>Basic Auth</b></td><td style='" + tds
                + "'>Enter Username and Password; sent as Base64 header.</td></tr>"
                + "<tr><td style='" + tds + "'><b>API Key</b></td><td style='" + tds
                + "'>Specify Key name, value, and location (header/query).</td></tr>"
                + "<tr><td style='" + tds + "'><b>OAuth 2.0</b></td><td style='" + tds
                + "'>Configure grant type, token URL, scopes. Click <b>Get Token</b>.</td></tr>"
                + "<tr><td style='" + tds + "'><b>Inherit</b></td><td style='" + tds
                + "'>Request uses parent Folder or Collection auth.</td></tr>"
                + "</table>"

                // 5. Scripting
                + "<h2 style='" + h2s + "'>5. &#128295; Scripting Engine (Rhino JS)</h2>"
                + "<p style='" + ps
                + "'>Write JavaScript in the <b>Pre-Request</b> and <b>Tests</b> tabs to automate and chain requests.</p>"
                + "<h3 style='" + h3s + "'>Common Snippets</h3>"
                + "<pre style='" + pre + "'>"
                + "// Set an environment variable from a response field\n"
                + "var token = JSON.parse(apibanker.response.body).access_token;\n"
                + "apibanker.environment.set(\"token\", token);\n\n"
                + "// Assert response status\n"
                + "apibanker.test(\"Status is 200\", () => apibanker.response.status === 200);\n\n"
                + "// Read a collection variable\n"
                + "var base = apibanker.collectionVariables.get(\"baseUrl\");"
                + "</pre>"
                + "<div style='" + cs
                + "'><b>Tip:</b> Click <b>Snippets</b> in the sidebar to insert pre-built script templates instantly.</div>"

                // 6. Swagger Import
                + "<h2 style='" + h2s + "'>6. &#128196; OpenAPI / Swagger Import</h2>"
                + "<p style='" + ps
                + "'>Import any OpenAPI 3.x or Swagger 2.x specification (JSON or YAML) and instantly create a fully structured Collection.</p>"
                + "<div style='" + cs + "'>"
                + "<h3 style='" + h3s + "'>How to Import</h3>"
                + "<ol style='" + lis + "'>"
                + "<li>Go to <b>File &rarr; Import OpenAPI / Swagger Spec</b>.</li>"
                + "<li>Click <b>Browse</b> and select your <code style='" + cds + "'>.json</code> or <code style='"
                + cds + "'>.yaml</code> file.</li>"
                + "<li>Click <b>Analyze Spec</b> &mdash; endpoints populate the table with colour-coded HTTP methods.</li>"
                + "<li>Check/uncheck individual endpoints to import selectively. Use <b>Select All</b> / <b>Deselect All</b> for bulk actions.</li>"
                + "<li>Choose <b>Base URL strategy</b>:<br>"
                + "&nbsp;&nbsp;&bull; <b>Directly in Request URL</b> &mdash; full URL baked into each request.<br>"
                + "&nbsp;&nbsp;&bull; <b>Collection Variable</b> &mdash; stores server(s) as <code style='" + cds
                + "'>{{baseUrl}}</code>, <code style='" + cds + "'>{{baseUrl_1}}</code>, etc.</li>"
                + "<li>Click <b>Import Selected</b>.</li>"
                + "</ol></div>"
                + "<h3 style='" + h3s + "'>What Gets Imported</h3>"
                + "<ul style='" + lis + "'>"
                + "<li>All selected endpoints as individual Requests with correct method and URL</li>"
                + "<li>Security schemes mapped to Collection auth (Bearer, Basic, API Key, OAuth2)</li>"
                + "<li>Server URLs as Collection Variables (falls back to <code style='" + cds
                + "'>http://localhost</code> if none defined)</li>"
                + "<li>Professional API documentation auto-generated in the Collection README covering servers, auth, per-endpoint parameters, and request bodies</li>"
                + "</ul>"

                // 7. Collection Runner
                + "<h2 style='" + h2s + "'>7. &#128202; Collection Runner</h2>"
                + "<p style='" + ps + "'>Run an entire Collection as a batch test suite with JMeter-compatible load simulation.</p>"
                + "<div style='" + cs + "'>"
                + "<ol style='" + lis + "'>"
                + "<li>Right-click a Collection &rarr; <b>Open Runner</b>, or click the Runner node in the sidebar.</li>"
                + "<li>Choose <b>Fixed Iterations</b> or <b>Fixed Duration</b> (Sec/Min/Hours/Days).</li>"
                + "<li>Set <b>Virtual Users (VUsers)</b>, <b>Ramp-up (s)</b>, and <b>Delay</b> between requests.</li>"
                + "<li>Select an <b>Environment</b> to resolve variables and click <b>Run</b>.</li>"
                + "<li>Export native <b>.jmx</b> files via <b>Export JMeter</b>. Mappings: <i>VUsers &rarr; ThreadGroup.num_threads</i>, <i>Ramp-up &rarr; ThreadGroup.ramp_time</i>, and <i>Duration &rarr; ThreadGroup.scheduler</i>.</li>"
                + "</ol></div>"

                // 8. Tools
                + "<h2 style='" + h2s + "'>8. &#9889; Built-in Tools</h2>"
                + "<table width='100%' cellspacing='0' style='border-collapse:collapse;margin:8px 0;'>"
                + "<tr><th style='" + ths + "'>Tool</th><th style='" + ths + "'>Location</th><th style='" + ths
                + "'>Purpose</th></tr>"
                + "<tr><td style='" + tds + "'><b>JWT Decoder</b></td><td style='" + tds
                + "'>Sidebar or Tools menu</td><td style='" + tds
                + "'>Paste a JWT to decode header, payload, and signature.</td></tr>"
                + "<tr><td style='" + tds + "'><b>Data Comparator</b></td><td style='" + tds
                + "'>Sidebar</td><td style='" + tds + "'>Diff two JSON or XML payloads side-by-side.</td></tr>"
                + "<tr><td style='" + tds + "'><b>JSON Formatter</b></td><td style='" + tds
                + "'>Data Tools tab</td><td style='" + tds + "'>Prettify or minify JSON / validate schema.</td></tr>"
                + "<tr><td style='" + tds + "'><b>Mock Data Generator</b></td><td style='" + tds
                + "'>Data Tools tab</td><td style='" + tds + "'>Generate realistic test data in bulk.</td></tr>"
                + "<tr><td style='" + tds + "'><b>Mock Server</b></td><td style='" + tds
                + "'>Tools menu</td><td style='" + tds
                + "'>Serve mock responses locally for frontend development.</td></tr>"
                + "<tr><td style='" + tds + "'><b>Cookie Jar</b></td><td style='" + tds + "'>Tools menu</td><td style='"
                + tds + "'>Manage and send cookies per domain.</td></tr>"
                + "<tr><td style='" + tds + "'><b>Log Console</b></td><td style='" + tds + "'>View menu</td><td style='"
                + tds + "'>Full request/response log with filtering.</td></tr>"
                + "</table>"

                // 9. Shortcuts
                + "<h2 style='" + h2s + "'>9. &#9000; Keyboard Shortcuts</h2>"
                + "<table width='80%' cellspacing='0' style='border-collapse:collapse;margin:8px 0;'>"
                + "<tr><th style='" + ths + "'>Shortcut</th><th style='" + ths + "'>Action</th></tr>"
                + "<tr><td style='" + tds + "'><b>Ctrl + R</b></td><td style='" + tds
                + "'>Send / Run active request</td></tr>"
                + "<tr><td style='" + tds + "'><b>Ctrl + S</b></td><td style='" + tds + "'>Save active tab</td></tr>"
                + "<tr><td style='" + tds + "'><b>Ctrl + O</b></td><td style='" + tds
                + "'>Open a Collection from file</td></tr>"
                + "<tr><td style='" + tds + "'><b>Ctrl + D</b></td><td style='" + tds
                + "'>Duplicate selected node</td></tr>"
                + "<tr><td style='" + tds + "'><b>Ctrl + C / V</b></td><td style='" + tds
                + "'>Copy / Paste request node</td></tr>"
                + "<tr><td style='" + tds + "'><b>F2</b></td><td style='" + tds + "'>Rename selected node</td></tr>"
                + "<tr><td style='" + tds + "'><b>Delete</b></td><td style='" + tds + "'>Delete selected node</td></tr>"
                + "<tr><td style='" + tds + "'><b>Ctrl + = / &ndash;</b></td><td style='" + tds
                + "'>Zoom UI in / out</td></tr>"
                + "<tr><td style='" + tds + "'><b>Ctrl + Scroll</b></td><td style='" + tds
                + "'>Zoom UI with mouse wheel</td></tr>"
                + "</table>"

                // 10. Use Cases
                + "<h2 style='" + h2s + "'>10. &#128161; Common Use Cases</h2>"
                + "<div style='" + cs + "'>"
                + "<h3 style='" + h3s + "'>&#10003; Test a REST API from Swagger Docs</h3>"
                + "<p style='" + ps
                + "'>Import the Swagger JSON &rarr; endpoints become requests &rarr; set an Environment with your API key &rarr; run the Collection Runner for regression testing.</p>"
                + "<h3 style='" + h3s + "'>&#10003; Chain Requests (Login &rarr; Use Token)</h3>"
                + "<p style='" + ps + "'>In the <b>Tests</b> tab of your login request, extract the token:<br>"
                + "<code style='" + cds
                + "'>apibanker.environment.set(\"token\", JSON.parse(apibanker.response.body).token)</code><br>"
                + "Then set Auth to <b>Bearer</b> with value <code style='" + cds
                + "'>{{token}}</code> in subsequent requests or at the Collection level.</p>"
                + "<h3 style='" + h3s + "'>&#10003; Load Test an Endpoint</h3>"
                + "<p style='" + ps
                + "'>Open the Collection Runner &rarr; set 100 iterations and 10 VUsers &rarr; monitor the live scatter plot &rarr; export a PDF report for sharing.</p>"
                + "<h3 style='" + h3s + "'>&#10003; Mock a Backend for Frontend Dev</h3>"
                + "<p style='" + ps
                + "'>Open <b>Tools &rarr; Mock Server</b> &rarr; define routes and responses &rarr; point your frontend to <code style='"
                + cds + "'>http://localhost:&lt;port&gt;</code>.</p>"
                + "</div>"

                // 11. UI & Accessibility
                + "<h2 style='" + h2s + "'>11. &#127912; UI, Safety &amp; Accessibility</h2>"
                + "<div style='" + cs + "'>"
                + "<p style='" + ps + "'><b>Responsive Scaling:</b> ApiBanker features fully responsive, real-time UI scaling via <b>Ctrl+Scroll</b> or <b>Ctrl+=/-</b>. Action buttons dynamically recalculate their layout so labels like <i>Sending...</i> and <i>Cancel</i> never clip.</p>"
                + "<p style='" + ps + "'><b>Stricter Editing Mode:</b> Enable this mode in Settings (<b>⚙</b>) to safeguard your workspace. Destructive operations (deletions, undo, paste) will force explicit confirmations to prevent accidental data loss.</p>"
                + "<p style='" + ps + "'><b>Rich Diagnostics:</b> SSL validation and connection timeouts are deeply configurable. Hover over the SSL status in responses to view a richly formatted, color-coded certificate breakdown.</p>"
                + "</div>"

                // Footer
                + "<div style='margin-top:40px;text-align:center;font-size:11px;color:#888;'>"
                + "ApiBanker v" + ver + " &mdash; &copy; 2026 SLPRO. All Rights Reserved."
                + "</div>"
                + "</body></html>";
    }

    private void applyTheme() {
        String theme = storage.getSettings().getTheme();
        try {
            App.setupTheme(theme, currentFontSize);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {
        }
    }

    private void setupZoom() {
        JComponent root = getRootPane();

        // Zoom In (Ctrl + EQUALS / Ctrl + ADD)
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "zoomIn");

        // Zoom Out (Ctrl + MINUS / Ctrl + SUBTRACT)
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut");

        root.getActionMap().put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoom(1);
            }
        });
        root.getActionMap().put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                zoom(-1);
            }
        });
    }

    private void setupSaveHotkey() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "saveActiveTab");
        root.getActionMap().put("saveActiveTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveActiveTab();
            }
        });
    }

    private void saveActiveTab() {
        Component c = workspaceTabs.getSelectedComponent();
        if (c instanceof RequestPanel rp) {
            if (rp.hasUnsavedChanges()) {
                rp.save();
            }
        } else if (c instanceof CollectionPanel cp) {
            if (cp.hasUnsavedChanges()) {
                cp.save();
            }
        } else if (c instanceof MockServerPanel msp) {
            msp.save();
        }
    }

    private void zoom(int increment) {
        currentFontSize += increment;
        if (currentFontSize < 10)
            currentFontSize = 10;
        if (currentFontSize > 24)
            currentFontSize = 24;

        Font currentDefaultFont = UIManager.getFont("defaultFont");
        if (currentDefaultFont == null) {
            currentDefaultFont = new Font("Segoe UI", Font.PLAIN, 12);
        }
        Font newDefaultFont = currentDefaultFont.deriveFont((float) currentFontSize);
        UIManager.put("defaultFont", newDefaultFont);

        com.formdev.flatlaf.FlatLaf.updateUI();

        updateFontSize(currentFontSize);
    }

    private void updateFontSize(int size) {
        if (sidebarPanel != null) {
            sidebarPanel.updateFontSize(size);
        }
        if (workspaceTabs != null) {
            for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
                Component tab = workspaceTabs.getComponentAt(i);
                if (tab instanceof RequestPanel rp) {
                    rp.updateFontSize(size);
                } else if (tab instanceof JwtDecoderPanel jp) {
                    jp.updateFontSize(size);
                } else if (tab instanceof JsonToolPanel jtp) {
                    jtp.updateFontSize(size);
                } else if (tab instanceof DataComparatorPanel cp) {
                    cp.updateFontSize(size);
                } else if (tab instanceof CollectionRunnerPanel crp) {
                    crp.updateFontSize(size);
                } else if (tab instanceof MockServerPanel msp) {
                    msp.updateFontSize(size);
                } else if (tab instanceof SettingsPanel sp) {
                    sp.updateFontSize(size);
                } else if (tab instanceof CookieJarPanel cjp) {
                    cjp.updateFontSize(size);
                } else if (tab instanceof EnvironmentManagerPanel emp) {
                    emp.updateFontSize(size);
                } else if (tab instanceof CollectionPanel cpnl) {
                    cpnl.updateFontSize(size);
                } else if (tab instanceof OpenApiImportPanel op) {
                    op.updateFontSize(size);
                }

                Component tabComp = workspaceTabs.getTabComponentAt(i);
                if (tabComp != null) {
                    FontScaleHelper.scaleFonts(tabComp, size);
                    if (tabComp instanceof JPanel header) {
                        for (Component child : header.getComponents()) {
                            if (child instanceof JButton closeBtn) {
                                closeBtn.setPreferredSize(new Dimension(size + 6, size + 6));
                            }
                        }
                    }
                }
            }
        }
        if (envCombo != null && envManageBtn != null) {
            int height = Math.max(26, size + 10);
            int width = Math.max(140, size * 9);
            envCombo.setPreferredSize(new Dimension(width, height));
            envCombo.setMaximumSize(new Dimension(width, height));
            
            if (envSelectorPanel != null) {
                envSelectorPanel.revalidate();
                envSelectorPanel.repaint();
            }
        }
        FontScaleHelper.scaleFonts(this, size);
        revalidate();
        repaint();
    }

    private void onClose() {
        // First check for unsaved changes on all tabs
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof RequestPanel rp) {
                if (rp.hasUnsavedChanges()) {
                    workspaceTabs.setSelectedIndex(i);
                    int option = JOptionPane.showConfirmDialog(
                            this,
                            "Request \"" + rp.getRequestModel().getName() + "\" has unsaved changes. Save them?",
                            "Save Changes?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        rp.save();
                    } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                        return; // Cancel closing the app
                    }
                }
            } else if (c instanceof CollectionPanel cp) {
                if (cp.hasUnsavedChanges()) {
                    workspaceTabs.setSelectedIndex(i);
                    int option = JOptionPane.showConfirmDialog(
                            this,
                            "Collection \"" + cp.getCollectionModel().getName() + "\" has unsaved changes. Save them?",
                            "Save Changes?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        cp.save();
                    } else if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                        return; // Cancel closing the app
                    }
                }
            }
        }

        // Save open requests and stop mock servers
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof RequestPanel rp) {
                rp.getRequestModel(); // triggers collectModel
            } else if (c instanceof CollectionRunnerPanel crp) {
                crp.saveConfig();
            } else if (c instanceof DataComparatorPanel cp) {
                cp.getRequestModel(); // triggers collect
            } else if (c instanceof MockServerPanel msp) {
                msp.stopServerIfRunning();
                msp.updateModel(); // triggers collect
            } else if (c instanceof WebSocketPanel wsp) {
                wsp.getRequestModel(); // triggers collect
            }
        }
        saveCollections();
        storage.saveHistory(history);

        // Save sequence and state of open tabs
        List<AppSettings.OpenTabState> tabStates = new ArrayList<>();
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            String type = null;
            String reqId = null;

            if (c instanceof RequestPanel rp) {
                type = "request";
                reqId = rp.getRequestModel().getId();
            } else if (c instanceof CollectionRunnerPanel crp) {
                type = "runner";
                reqId = crp.getRequestModel().getId();
            } else if (c instanceof DataComparatorPanel cp) {
                type = "comparator";
                reqId = cp.getRequestModel().getId();
            } else if (c instanceof JwtDecoderPanel) {
                type = "jwt";
            } else if (c instanceof JsonToolPanel) {
                type = "json";
            } else if (c instanceof MockServerPanel msp) {
                type = "mockserver";
                reqId = msp.getRequestModel().getId();
            } else if (c instanceof WebSocketPanel wsp) {
                type = "websocket";
                reqId = wsp.getRequestModel().getId();
            } else if (c instanceof DataToolsPanel) {
                type = "datatools";
            } else if (c instanceof LogConsolePanel) {
                type = "logconsole";
            } else if (c instanceof SettingsPanel) {
                type = "settings";
            } else if (c instanceof CookieJarPanel) {
                type = "cookiejar";
            } else if (c instanceof EnvironmentManagerPanel) {
                type = "envmanager";
            } else {
                String title = workspaceTabs.getTitleAt(i);
                if ("Welcome".equals(title)) {
                    type = "welcome";
                }
            }

            if (type != null) {
                tabStates.add(new AppSettings.OpenTabState(type, reqId));
            }
        }
        storage.getSettings().setOpenTabs(tabStates);
        storage.getSettings().setSelectedTabIndex(workspaceTabs.getSelectedIndex());

        // Save zoom and window settings
        storage.getSettings().setFontSize(currentFontSize);
        boolean maximized = (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
        storage.getSettings().setWindowMaximized(maximized);
        if (!maximized) {
            storage.getSettings().setWindowWidth(getWidth());
            storage.getSettings().setWindowHeight(getHeight());
            storage.getSettings().setWindowX(getX());
            storage.getSettings().setWindowY(getY());
        }
        storage.saveSettings();
        in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("APP_CLOSE", "System", "ApiBanker Version " + App.getVersion() + " closed.");
        System.exit(0);
    }

    private RequestModel findRequestModel(String id) {
        if (id == null)
            return null;
        for (CollectionModel col : collections) {
            RequestModel found = findRequestModelRecursive(col, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private RequestModel findRequestModelRecursive(CollectionModel col, String id) {
        for (RequestModel req : col.getRequests()) {
            if (id.equals(req.getId())) {
                return req;
            }
        }
        for (CollectionModel sub : col.getFolders()) {
            RequestModel found = findRequestModelRecursive(sub, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void restoreTab(AppSettings.OpenTabState ts) {
        if ("request".equals(ts.getType())) {
            RequestModel req = findRequestModel(ts.getRequestModelId());
            if (req != null)
                openRequest(req);
        } else if ("runner".equals(ts.getType())) {
            RequestModel runner = findRequestModel(ts.getRequestModelId());
            if (runner != null) {
                CollectionModel parentCol = findRequestParent(runner);
                if (parentCol != null) {
                    openRunner(parentCol, runner);
                }
            }
        } else if ("comparator".equals(ts.getType())) {
            RequestModel req = findRequestModel(ts.getRequestModelId());
            if (req != null) {
                openRequest(req);
            } else {
                openDataComparator();
            }
        } else if ("jwt".equals(ts.getType())) {
            openJwtDecoder();
        } else if ("json".equals(ts.getType())) {
            openJsonTool();
        } else if ("mockserver".equals(ts.getType())) {
            RequestModel req = findRequestModel(ts.getRequestModelId());
            if (req != null) {
                openMockServer(req);
            } else {
                openMockServer();
            }
        } else if ("websocket".equals(ts.getType())) {
            RequestModel req = findRequestModel(ts.getRequestModelId());
            if (req != null) {
                openRequest(req);
            }
        } else if ("datatools".equals(ts.getType())) {
            openDataTools();
        } else if ("logconsole".equals(ts.getType())) {
            openLogConsole();
        } else if ("welcome".equals(ts.getType())) {
            openWelcomeTab();
        } else if ("settings".equals(ts.getType())) {
            openSettings();
        } else if ("cookiejar".equals(ts.getType())) {
            openCookieJarManager();
        } else if ("envmanager".equals(ts.getType())) {
            openEnvManager();
        } else if ("globalvars".equals(ts.getType())) {
            openGlobalVariables();
        }
    }

    public static void showToast(Component parent, String message) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        if (window == null) {
            Frame[] frames = Frame.getFrames();
            for (Frame f : frames) {
                if (f.isVisible()) {
                    window = f;
                    break;
                }
            }
        }
        if (window == null)
            return;

        JWindow toast = new JWindow(window);
        toast.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(33, 33, 33, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        panel.add(label, BorderLayout.CENTER);
        toast.add(panel);
        toast.pack();

        Point parentPos = window.getLocationOnScreen();
        int x = parentPos.x + (window.getWidth() - toast.getWidth()) / 2;
        int y = parentPos.y + window.getHeight() - toast.getHeight() - 80;
        toast.setLocation(x, y);

        toast.setVisible(true);

        new Thread(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
            }
            SwingUtilities.invokeLater(() -> {
                toast.dispose();
            });
        }).start();
    }

    private static class TabHeaderPanel extends JPanel {
        String title;
        Runnable renameAction;

        TabHeaderPanel(String title) {
            super(new FlowLayout(FlowLayout.LEFT, 4, 0));
            this.title = title;
            setOpaque(false);
        }
    }

    public static class CollectionPathWrapper {
        public final CollectionModel model;
        public final String path;

        public CollectionPathWrapper(CollectionModel model, String path) {
            this.model = model;
            this.path = path;
        }

        @Override
        public String toString() {
            return path;
        }
    }

    public List<CollectionPathWrapper> getAllCollectionsAndFoldersWithPaths() {
        List<CollectionPathWrapper> list = new ArrayList<>();
        for (CollectionModel col : collections) {
            collectCollectionsAndFoldersWithPathsRecursive(col, col.getName(), list);
        }
        return list;
    }

    private void collectCollectionsAndFoldersWithPathsRecursive(CollectionModel col, String currentPath,
            List<CollectionPathWrapper> list) {
        list.add(new CollectionPathWrapper(col, currentPath));
        for (CollectionModel sub : col.getFolders()) {
            collectCollectionsAndFoldersWithPathsRecursive(sub, currentPath + " > " + sub.getName(), list);
        }
    }

    public CollectionModel findRequestParent(RequestModel req) {
        for (CollectionModel col : collections) {
            CollectionModel parent = findRequestParentRecursive(col, req);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    private CollectionModel findRequestParentRecursive(CollectionModel col, RequestModel req) {
        if (col.getRequests().contains(req)) {
            return col;
        }
        for (CollectionModel sub : col.getFolders()) {
            CollectionModel parent = findRequestParentRecursive(sub, req);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    public CollectionModel findFolderParent(CollectionModel target) {
        for (CollectionModel col : collections) {
            if (col == target)
                return null; // Root collection has no parent
            CollectionModel parent = findFolderParentRecursive(col, target);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    private CollectionModel findFolderParentRecursive(CollectionModel col, CollectionModel target) {
        if (col.getFolders().contains(target)) {
            return col;
        }
        for (CollectionModel sub : col.getFolders()) {
            CollectionModel parent = findFolderParentRecursive(sub, target);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    private boolean deleteCollectionRecursive(List<CollectionModel> list, CollectionModel target) {
        if (list.remove(target)) {
            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("DELETE_COLLECTION", "User", "Deleted: [" + target.getName() + " / " + target.getId() + "]");
            return true;
        }
        for (CollectionModel col : list) {
            if (deleteCollectionRecursive(col.getFolders(), target)) {
                return true;
            }
        }
        return false;
    }

    private boolean deleteRequestRecursive(CollectionModel col, RequestModel req) {
        if (col.getRequests().remove(req)) {
            return true;
        }
        for (CollectionModel sub : col.getFolders()) {
            if (deleteRequestRecursive(sub, req)) {
                return true;
            }
        }
        return false;
    }

    private boolean duplicateRequestRecursive(CollectionModel col, RequestModel req) {
        int index = col.getRequests().indexOf(req);
        if (index >= 0) {
            RequestModel dup = duplicateRequestModel(req);
            dup.setName(req.getName() + " Copy");
            col.getRequests().add(index + 1, dup);
            saveCollections();
            sidebarPanel.refreshCollections(collections);
            openRequest(dup);
            return true;
        }
        for (CollectionModel sub : col.getFolders()) {
            if (duplicateRequestRecursive(sub, req)) {
                return true;
            }
        }
        return false;
    }

    public CollectionModel findCollectionParent(CollectionModel child) {
        for (CollectionModel col : collections) {
            if (col == child)
                return null;
            CollectionModel parent = findCollectionParentRecursive(col, child);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    private CollectionModel findCollectionParentRecursive(CollectionModel current, CollectionModel target) {
        for (CollectionModel folder : current.getFolders()) {
            if (folder == target) {
                return current;
            }
            CollectionModel parent = findCollectionParentRecursive(folder, target);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    public boolean resolveSslVerification(RequestModel req) {
        if (req == null) {
            return true;
        }
        // 1. Check Global forced options first
        String globalSetting = storage.getSettings().getGlobalSslSetting();
        if ("VERIFY_FORCED".equalsIgnoreCase(globalSetting)) {
            return true;
        }
        if ("NO_VERIFY_FORCED".equalsIgnoreCase(globalSetting)) {
            return false;
        }

        // 2. Check request setting
        String reqSetting = req.getSslSetting();
        if ("VERIFY".equalsIgnoreCase(reqSetting)) {
            return true;
        }
        if ("NO_VERIFY".equalsIgnoreCase(reqSetting)) {
            return false;
        }

        // 3. Traversal up parent folders/collections recursively
        CollectionModel parent = getParentCollection(req);
        while (parent != null) {
            String parentSetting = parent.getSslSetting();
            if ("VERIFY".equalsIgnoreCase(parentSetting)) {
                return true;
            }
            if ("NO_VERIFY".equalsIgnoreCase(parentSetting)) {
                return false;
            }
            parent = findCollectionParent(parent);
        }

        // 4. Default to Global Setting (VERIFY or NO_VERIFY)
        return !"NO_VERIFY".equalsIgnoreCase(globalSetting);
    }

    public static boolean resolveSslVerificationStatic(RequestModel req) {
        MainFrame frame = getInstance();
        if (frame != null) {
            return frame.resolveSslVerification(req);
        }
        String globalSetting = StorageManager.getInstance().getSettings().getGlobalSslSetting();
        if ("VERIFY_FORCED".equalsIgnoreCase(globalSetting)) {
            return true;
        }
        if ("NO_VERIFY_FORCED".equalsIgnoreCase(globalSetting)) {
            return false;
        }
        String reqSetting = req.getSslSetting();
        if ("VERIFY".equalsIgnoreCase(reqSetting)) {
            return true;
        }
        if ("NO_VERIFY".equalsIgnoreCase(reqSetting)) {
            return false;
        }
        return !"NO_VERIFY".equalsIgnoreCase(globalSetting);
    }

    public boolean resolveRedirectSetting(RequestModel req) {
        if (req == null) {
            return true;
        }
        // 1. Check Global forced options first
        String globalSetting = storage.getSettings().getGlobalRedirectSetting();
        if ("YES_FORCED".equalsIgnoreCase(globalSetting)) {
            return true;
        }
        if ("NO_FORCED".equalsIgnoreCase(globalSetting)) {
            return false;
        }

        // 2. Check request setting
        String reqSetting = req.getRedirectSetting();
        if ("YES".equalsIgnoreCase(reqSetting)) {
            return true;
        }
        if ("NO".equalsIgnoreCase(reqSetting)) {
            return false;
        }

        // 3. Traversal up parent folders/collections recursively
        CollectionModel parent = getParentCollection(req);
        while (parent != null) {
            String parentSetting = parent.getRedirectSetting();
            if ("YES".equalsIgnoreCase(parentSetting)) {
                return true;
            }
            if ("NO".equalsIgnoreCase(parentSetting)) {
                return false;
            }
            parent = findCollectionParent(parent);
        }

        // 4. Default to Global Setting (YES or NO)
        return !"NO".equalsIgnoreCase(globalSetting);
    }

    public static boolean resolveRedirectSettingStatic(RequestModel req) {
        MainFrame frame = getInstance();
        if (frame != null) {
            return frame.resolveRedirectSetting(req);
        }
        String globalSetting = StorageManager.getInstance().getSettings().getGlobalRedirectSetting();
        if ("YES_FORCED".equalsIgnoreCase(globalSetting)) {
            return true;
        }
        if ("NO_FORCED".equalsIgnoreCase(globalSetting)) {
            return false;
        }
        String reqSetting = req.getRedirectSetting();
        if ("YES".equalsIgnoreCase(reqSetting)) {
            return true;
        }
        if ("NO".equalsIgnoreCase(reqSetting)) {
            return false;
        }
        return !"NO".equalsIgnoreCase(globalSetting);
    }

    public int resolveTimeout(RequestModel req) {
        if (req == null) {
            return 120;
        }
        String globalSetting = storage.getSettings().getGlobalTimeoutSetting();
        if ("CUSTOM_FORCED".equalsIgnoreCase(globalSetting)) {
            return storage.getSettings().getGlobalTimeoutValue();
        }

        String reqSetting = req.getTimeoutSetting();
        if ("CUSTOM".equalsIgnoreCase(reqSetting)) {
            return req.getTimeoutValue();
        }

        CollectionModel parent = getParentCollection(req);
        while (parent != null) {
            String parentSetting = parent.getTimeoutSetting();
            if ("CUSTOM".equalsIgnoreCase(parentSetting)) {
                return parent.getTimeoutValue();
            }
            parent = findCollectionParent(parent);
        }

        if ("CUSTOM_OPTIONAL".equalsIgnoreCase(globalSetting)) {
            return storage.getSettings().getGlobalTimeoutValue();
        }
        return 120;
    }

    public static int resolveTimeoutStatic(RequestModel req) {
        MainFrame frame = getInstance();
        if (frame != null) {
            return frame.resolveTimeout(req);
        }
        String globalSetting = StorageManager.getInstance().getSettings().getGlobalTimeoutSetting();
        if ("CUSTOM_FORCED".equalsIgnoreCase(globalSetting)) {
            return StorageManager.getInstance().getSettings().getGlobalTimeoutValue();
        }
        String reqSetting = req.getTimeoutSetting();
        if ("CUSTOM".equalsIgnoreCase(reqSetting)) {
            return req.getTimeoutValue();
        }
        if ("CUSTOM_OPTIONAL".equalsIgnoreCase(globalSetting)) {
            return StorageManager.getInstance().getSettings().getGlobalTimeoutValue();
        }
        return 120;
    }
}
