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
 * @version 2.0.1
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

        try {
            java.net.URL iconUrl = getClass().getResource("/icon.png");
            if (iconUrl != null) {
                java.awt.Image appIcon = javax.imageio.ImageIO.read(iconUrl);
                setIconImage(appIcon);

                // For modern macOS/Linux taskbars (optional but good practice)
                if (java.awt.Taskbar.isTaskbarSupported()
                        && java.awt.Taskbar.getTaskbar().isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                    java.awt.Taskbar.getTaskbar().setIconImage(appIcon);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }

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
        setupConsoleHotkey();
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
        } catch (Exception e) {
        }
    }

    public void undoCollectionTree() {
        if (!collectionUndoStack.isEmpty()) {
            if (in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().isStricterEditing()) {
                int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to undo the last action?",
                        "Confirm Undo", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION)
                    return;
            }
            String state = collectionUndoStack.pop();
            try {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<CollectionModel>>() {
                }.getType();
                List<CollectionModel> restored = gson.fromJson(state, listType);
                if (restored != null) {
                    this.collections.clear();
                    this.collections.addAll(restored);
                    saveCollections();
                    sidebarPanel.refreshCollections(this.collections);
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("UNDO_COLLECTION_ACTION",
                            "User", "Restored previous collection state.");
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
        consoleItem.addActionListener(e -> ConsoleDialog.showConsole(this));
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
        userGuideItem.addActionListener(e -> openDocumentationPanel(null));
        helpMenu.add(userGuideItem);
        JMenuItem featuresItem = new JMenuItem("Features");
        featuresItem.addActionListener(e -> openFeaturesPanel());
        helpMenu.add(featuresItem);
        JMenuItem licenseItem = new JMenuItem("License & Copyright");
        licenseItem.addActionListener(e -> openLicensePanel());
        helpMenu.add(licenseItem);
        helpMenu.addSeparator();
        JMenuItem aboutItem = new JMenuItem("About Us");
        aboutItem.addActionListener(e -> openAboutUsPanel());
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
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete collection '" + col.getName() + "'?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return;
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
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete request '" + req.getName() + "'?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return;
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
        if (items == null || items.isEmpty())
            return;
        if (in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().isStricterEditing()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete " + items.size() + " item(s)?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION)
                return;
        } else {
            showToast(this, "Deleted " + items.size() + " item(s)");
        }
        pushCollectionStateForUndo();
        boolean changed = false;
        for (Object item : items) {
            if (item instanceof CollectionModel col) {
                if (OTHERS_COLLECTION_ID.equals(col.getId()))
                    continue;
                closeTabsForCollectionRecursive(col);
                if (deleteCollectionRecursive(collections, col))
                    changed = true;
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
        String markdown = loadAboutUsMarkdown();
        String html = renderAboutUsHtml(markdown);

        javax.swing.JEditorPane pane = new javax.swing.JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(UIManager.getColor("Panel.background"));
        pane.setText(html);
        pane.setCaretPosition(0);

        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                String desc = e.getDescription();
                if ("paypal-contribute".equals(desc)) {
                    showPayPalContributionDialog();
                } else if (e.getURL() != null) {
                    try {
                        java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        try {
                            if (desc != null && (desc.startsWith("http://") || desc.startsWith("https://"))) {
                                java.awt.Desktop.getDesktop().browse(new java.net.URI(desc));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(pane);
        scroll.setBorder(null);
        JPanel welcome = new JPanel(new BorderLayout());
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
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_COLLECTION", "User",
                            "Format: Postman, Status: Success, Collection: [" + col.getName() + " / " + col.getId()
                                    + "], Requests: " + totalRequests + ", Imported From: " + file.getAbsolutePath());
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
                        in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_ENVIRONMENT",
                                "User", "Format: Postman, Status: Success, Environment: [" + env.getName() + " / "
                                        + env.getId() + "], Imported From: " + file.getAbsolutePath());
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
                            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_ENVIRONMENT",
                                    "User",
                                    "Format: Postman Globals, Status: Success, Environment: [" + globals.getName()
                                            + " / " + globals.getId() + "], Imported From: " + file.getAbsolutePath());
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
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_ENVIRONMENT", "User",
                            "Format: Postman, Status: Success, Environment: [" + env.getName() + " / " + env.getId()
                                    + "], Imported From: " + file.getAbsolutePath());
                } else {
                    failedFiles.add(file.getName() + " (unrecognized Postman format)");
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_FAILED", "User",
                            "Format: Postman, Status: Failed (unrecognized format), Imported From: "
                                    + file.getAbsolutePath());
                }
            } catch (Exception e) {
                failedFiles.add(file.getName() + " (" + e.getMessage() + ")");
                in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_FAILED", "User",
                        "Format: Postman, Status: Error (" + e.getMessage() + "), Imported From: "
                                + file.getAbsolutePath());
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
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_COLLECTION", "User",
                            "Format: ApiBanker, Status: Success, Collection: [" + col.getName() + " / " + col.getId()
                                    + "], Imported From: " + file.getAbsolutePath());
                } else if (root.has("variables")) {
                    EnvironmentModel env = gson.fromJson(root, EnvironmentModel.class);
                    env.setId(UUID.randomUUID().toString());
                    environments.add(env);
                    importedEnvironments.add(env.getName());
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_ENVIRONMENT", "User",
                            "Format: ApiBanker, Status: Success, Environment: [" + env.getName() + " / " + env.getId()
                                    + "], Imported From: " + file.getAbsolutePath());
                } else {
                    failedFiles.add(file.getName() + " (Unknown format)");
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_FAILED", "User",
                            "Format: ApiBanker, Status: Failed (Unknown format), Imported From: "
                                    + file.getAbsolutePath());
                }
            } catch (Exception e) {
                failedFiles.add(file.getName() + " (" + e.getMessage() + ")");
                in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("IMPORT_FAILED", "User",
                        "Format: ApiBanker, Status: Error (" + e.getMessage() + "), Imported From: "
                                + file.getAbsolutePath());
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
            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("EXPORT_COLLECTION", "User",
                    "Format: Postman, Source: [" + col.getName() + " / " + col.getId() + "] -> Exported to: "
                            + chooser.getSelectedFile().getAbsolutePath());
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
            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("EXPORT_COLLECTION", "User",
                    "Format: ApiBanker, Source: [" + col.getName() + " / " + col.getId() + "] -> Exported to: "
                            + chooser.getSelectedFile().getAbsolutePath());
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

    private static final String PAYPAL_CONTRIBUTE_URL = "https://paypal.me/ncrk";

    private void openAboutUsPanel() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if ("About Us".equals(workspaceTabs.getTitleAt(i))) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }

        String markdown = loadAboutUsMarkdown();
        String html = renderAboutUsHtml(markdown);

        javax.swing.JEditorPane pane = new javax.swing.JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(javax.swing.UIManager.getColor("Panel.background"));
        pane.setText(html);
        pane.setCaretPosition(0);

        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                String desc = e.getDescription();
                if ("paypal-contribute".equals(desc)) {
                    showPayPalContributionDialog();
                } else if (e.getURL() != null) {
                    try {
                        java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        try {
                            if (desc != null && (desc.startsWith("http://") || desc.startsWith("https://"))) {
                                java.awt.Desktop.getDesktop().browse(new java.net.URI(desc));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(pane);
        scroll.setBorder(null);
        javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
        wrapper.add(scroll, java.awt.BorderLayout.CENTER);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("About Us", wrapper);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("About Us", idx, wrapper));
        workspaceTabs.setSelectedIndex(idx);
    }

    /**
     * Displays a confirmation dialog before redirecting the user to PayPal for
     * a voluntary contribution. This ensures the user has explicitly opted in
     * before being taken to an external payment page.
     */
    private void showPayPalContributionDialog() {
        boolean dk = com.formdev.flatlaf.FlatLaf.isLafDark();
        Color accentColor = UIManager.getColor("AccentColor");
        if (accentColor == null)
            accentColor = new Color(26, 115, 232);
        String acHex = String.format("#%02x%02x%02x", accentColor.getRed(), accentColor.getGreen(),
                accentColor.getBlue());
        String bgHex = dk ? "#2b2b2b" : "#ffffff";
        String fgHex = dk ? "#cccccc" : "#333333";
        String mutedHex = dk ? "#999999" : "#666666";

        String htmlMessage = "<html><body style='font-family: Segoe UI, Arial, sans-serif; width: 360px; padding: 5px;'>"
                + "<div style='text-align: center; margin-bottom: 12px;'>"
                + "<span style='font-size: 36px;'>💙</span>"
                + "</div>"
                + "<p style='font-size: 14px; font-weight: bold; color: " + acHex
                + "; text-align: center; margin: 0 0 8px 0;'>"
                + "Support ApiBanker Development</p>"
                + "<p style='font-size: 12px; color: " + fgHex
                + "; text-align: center; line-height: 1.6; margin: 0 0 12px 0;'>"
                + "ApiBanker is <b>free to use</b> software. "
                + "Your voluntary contribution helps fund ongoing development, bug fixes, and new features.</p>"
                + "<p style='font-size: 12px; color: " + mutedHex
                + "; text-align: center; line-height: 1.5; margin: 0 0 8px 0;'>"
                + "You will be redirected to <b>PayPal</b> in your default browser to complete the contribution.<br>"
                + "No amount is required — any support is appreciated!</p>"
                + "<hr style='border: none; border-top: 1px solid " + (dk ? "#444" : "#e0e0e0") + "; margin: 12px 0;'>"
                + "<p style='font-size: 11px; color: " + mutedHex + "; text-align: center; margin: 0;'>"
                + "© 2024–2026 NCRK</p>"
                + "</body></html>";

        JLabel messageLabel = new JLabel(htmlMessage);

        Object[] options = { "Open PayPal", "Cancel" };
        int result = JOptionPane.showOptionDialog(
                this,
                messageLabel,
                "Contribute via PayPal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        if (result == JOptionPane.YES_OPTION) {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(PAYPAL_CONTRIBUTE_URL));
                in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction(
                        "PAYPAL_CONTRIBUTE", "User", "User opened PayPal contribution page.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not open PayPal in your default browser.\n"
                                + "Please visit: " + PAYPAL_CONTRIBUTE_URL,
                        "Browser Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String loadAboutUsMarkdown() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/docs/AboutUs.md")) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        try {
            java.io.File f = new java.io.File("docs/AboutUs.md");
            if (f.exists()) {
                return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        try {
            java.io.File f = new java.io.File("../docs/AboutUs.md");
            if (f.exists()) {
                return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return "# About ApiBanker\n\nAbout Us document could not be loaded from docs/AboutUs.md.";
    }

    private String renderAboutUsHtml(String markdown) {
        Color ac = javax.swing.UIManager.getColor("AccentColor");
        if (ac == null)
            ac = new java.awt.Color(26, 115, 232);
        String a = String.format("#%02x%02x%02x", ac.getRed(), ac.getGreen(), ac.getBlue());
        boolean dk = com.formdev.flatlaf.FlatLaf.isLafDark();
        String bg = dk ? "#1e1e1e" : "#f8f9fa";
        String fg = dk ? "#cccccc" : "#333333";
        String cardBg = dk ? "#2b2b2b" : "#ffffff";
        String cardBdr = dk ? "#3a3a3a" : "#e2e8f0";
        String cardFg = dk ? "#bbbbbb" : "#4a5568";
        String cbg = dk ? "#1a1a1a" : "#f0f4f8";
        String linkColor = dk ? "#66b2ff" : "#0066cc";

        org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder().build();
        org.commonmark.node.Node document = parser.parse(markdown);
        org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder()
                .build();
        String bodyHtml = renderer.render(document);

        if (dk) {
            bodyHtml = bodyHtml.replace("background-color: #ffffff;", "background-color: " + cardBg + ";")
                    .replace("border: 1px solid #e2e8f0;", "border: 1px solid " + cardBdr + ";")
                    .replace("color: #4a5568;", "color: " + cardFg + ";");
        }

        StringBuilder css = new StringBuilder();
        css.append("body { font-family: 'Segoe UI', Arial, sans-serif; color: ").append(fg)
                .append("; background-color: ").append(bg).append("; padding: 20px 30px; line-height: 1.6; }\n");
        css.append("h1 { color: ").append(a)
                .append("; font-size: 32px; font-weight: bold; text-align: center; margin-bottom: 4px; }\n");
        css.append("h3 { color: ").append(a)
                .append("; font-size: 15px; margin-top: 0; margin-bottom: 8px; font-weight: bold; }\n");
        css.append("p, li { font-size: 13px; line-height: 1.6; }\n");
        css.append("a { color: ").append(linkColor).append("; text-decoration: underline; font-weight: bold; }\n");
        css.append("code { background-color: ").append(cbg).append("; border: 1px solid ").append(cardBdr)
                .append("; border-radius: 4px; padding: 1px 5px; font-family: monospace; font-size: 12px; }\n");

        return "<html><head><style>" + css.toString() + "</style></head><body>" + bodyHtml + "</body></html>";
    }

    /**
     * Opens a Features panel that renders README.md with Markdown-to-HTML
     * conversion.
     */
    /**
     * Opens the License & Copyright panel that renders LICENSE.md with
     * Markdown-to-HTML conversion, similar to the User Guide and Features panels.
     */
    private void openLicensePanel() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if ("License & Copyright".equals(workspaceTabs.getTitleAt(i))) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }

        String markdown = loadLicenseMarkdown();
        String html = renderLicenseHtml(markdown);

        javax.swing.JEditorPane pane = new javax.swing.JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(javax.swing.UIManager.getColor("Panel.background"));
        pane.setText(html);
        pane.setCaretPosition(0);

        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                String desc = e.getDescription();
                if (desc != null && desc.startsWith("#")) {
                    String ref = desc.substring(1);
                    pane.scrollToReference(ref);
                } else if (e.getURL() != null) {
                    try {
                        java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        try {
                            if (desc != null && (desc.startsWith("http://") || desc.startsWith("https://"))) {
                                java.awt.Desktop.getDesktop().browse(new java.net.URI(desc));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(pane);
        scroll.setBorder(null);
        javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
        wrapper.add(scroll, java.awt.BorderLayout.CENTER);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("License & Copyright", wrapper);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("License & Copyright", idx, wrapper));
        workspaceTabs.setSelectedIndex(idx);
    }

    private String loadLicenseMarkdown() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/docs/LICENSE.md")) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        try {
            java.io.File f = new java.io.File("docs/LICENSE.md");
            if (f.exists()) {
                return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        try {
            java.io.File f = new java.io.File("../docs/LICENSE.md");
            if (f.exists()) {
                return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return "# License & Copyright\n\nLicense document could not be loaded from docs/LICENSE.md.";
    }

    private String renderLicenseHtml(String markdown) {
        Color ac = javax.swing.UIManager.getColor("AccentColor");
        if (ac == null)
            ac = new java.awt.Color(26, 115, 232);
        String a = String.format("#%02x%02x%02x", ac.getRed(), ac.getGreen(), ac.getBlue());
        boolean dk = com.formdev.flatlaf.FlatLaf.isLafDark();
        String bg = dk ? "#1e1e1e" : "#ffffff";
        String fg = dk ? "#cccccc" : "#333333";
        String card = dk ? "#2b2b2b" : "#f8f9fa";
        String bdr = dk ? "#3a3a3a" : "#e0e0e0";
        String cbg = dk ? "#1a1a1a" : "#f0f4f8";
        String linkColor = dk ? "#66b2ff" : "#0066cc";
        String cardBg = dk ? "#2b2b2b" : "#ffffff";
        String cardBdr = dk ? "#3a3a3a" : "#e2e8f0";
        String cardFg = dk ? "#bbbbbb" : "#4a5568";

        org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder().build();
        org.commonmark.node.Node document = parser.parse(markdown);
        org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder()
                .build();
        String bodyHtml = renderer.render(document);

        if (dk) {
            bodyHtml = bodyHtml.replace("background-color: #ffffff;", "background-color: " + cardBg + ";")
                    .replace("border: 1px solid #e2e8f0;", "border: 1px solid " + cardBdr + ";")
                    .replace("color: #4a5568;", "color: " + cardFg + ";");
        }

        StringBuilder css = new StringBuilder();
        css.append("body { font-family: 'Segoe UI', Arial, sans-serif; color: ").append(fg)
                .append("; background-color: ").append(bg).append("; padding: 25px 35px; line-height: 1.6; }\n");
        css.append("h1 { color: ").append(a).append("; font-size: 26px; border-bottom: 2px solid ").append(a)
                .append("; padding-bottom: 8px; margin-bottom: 20px; }\n");
        css.append("h2 { color: ").append(a).append("; font-size: 18px; border-bottom: 1px solid ").append(bdr)
                .append("; padding-bottom: 6px; margin-top: 30px; margin-bottom: 12px; }\n");
        css.append("h3 { color: ").append(a).append("; font-size: 14px; margin-top: 18px; margin-bottom: 6px; }\n");
        css.append("p, li { font-size: 13px; line-height: 1.6; }\n");
        css.append("a { color: ").append(linkColor).append("; text-decoration: underline; font-weight: bold; }\n");
        css.append("code { background-color: ").append(cbg).append("; border: 1px solid ").append(bdr)
                .append("; border-radius: 4px; padding: 1px 5px; font-family: monospace; font-size: 12px; }\n");
        css.append("pre { background-color: ").append(cbg).append("; border: 1px solid ").append(bdr).append(
                "; border-radius: 6px; padding: 12px 16px; font-family: monospace; font-size: 12px; margin: 10px 0; }\n");
        css.append("blockquote { background-color: ").append(card).append("; border-left: 4px solid ").append(a)
                .append("; padding: 10px 16px; margin: 12px 0; font-size: 13px; }\n");
        css.append("table { border-collapse: collapse; width: 100%; margin: 12px 0; }\n");
        css.append("th { background-color: ").append(cbg).append("; color: ").append(a)
                .append("; font-weight: bold; border: 1px solid ").append(bdr)
                .append("; padding: 8px 12px; font-size: 13px; text-align: left; }\n");
        css.append("td { border: 1px solid ").append(bdr).append("; padding: 8px 12px; font-size: 13px; }\n");
        css.append("hr { border: none; border-top: 1px solid ").append(bdr).append("; margin: 25px 0; }\n");

        return "<html><head><style>" + css.toString() + "</style></head><body>" + bodyHtml + "</body></html>";
    }

    private void openFeaturesPanel() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if ("Features".equals(workspaceTabs.getTitleAt(i))) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }

        String markdown = loadFeaturesMarkdown();
        String html = renderFeaturesHtml(markdown);

        javax.swing.JEditorPane pane = new javax.swing.JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setBackground(javax.swing.UIManager.getColor("Panel.background"));
        pane.setText(html);
        pane.setCaretPosition(0);

        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                String desc = e.getDescription();
                if (desc != null && desc.startsWith("#")) {
                    String ref = desc.substring(1);
                    pane.scrollToReference(ref);
                } else if (e.getURL() != null) {
                    try {
                        java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        try {
                            if (desc != null && (desc.startsWith("http://") || desc.startsWith("https://"))) {
                                java.awt.Desktop.getDesktop().browse(new java.net.URI(desc));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(pane);
        scroll.setBorder(null);
        javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());
        wrapper.add(scroll, java.awt.BorderLayout.CENTER);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Features", wrapper);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Features", idx, wrapper));
        workspaceTabs.setSelectedIndex(idx);
    }

    private String loadFeaturesMarkdown() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/docs/Features.md")) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        try {
            java.io.File f = new java.io.File("docs/Features.md");
            if (f.exists()) {
                return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        try {
            java.io.File f = new java.io.File("../docs/Features.md");
            if (f.exists()) {
                return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return "# ApiBanker Features\n\nFeatures document could not be loaded from docs/Features.md.";
    }

    private String renderFeaturesHtml(String markdown) {
        Color ac = javax.swing.UIManager.getColor("AccentColor");
        if (ac == null)
            ac = new java.awt.Color(26, 115, 232);
        String a = String.format("#%02x%02x%02x", ac.getRed(), ac.getGreen(), ac.getBlue());
        boolean dk = com.formdev.flatlaf.FlatLaf.isLafDark();
        String bg = dk ? "#1e1e1e" : "#ffffff";
        String fg = dk ? "#cccccc" : "#333333";
        String card = dk ? "#2b2b2b" : "#f8f9fa";
        String bdr = dk ? "#3a3a3a" : "#e0e0e0";
        String cbg = dk ? "#1a1a1a" : "#f0f4f8";
        String linkColor = dk ? "#66b2ff" : "#0066cc";

        java.util.List<org.commonmark.Extension> extensions = java.util.Arrays
                .asList(org.commonmark.ext.gfm.tables.TablesExtension.create());
        org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder().extensions(extensions).build();
        org.commonmark.node.Node document = parser.parse(markdown);
        org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder()
                .extensions(extensions).build();
        String bodyHtml = renderer.render(document);

        StringBuilder css = new StringBuilder();
        css.append("body { font-family: 'Segoe UI', Arial, sans-serif; color: ").append(fg)
                .append("; background-color: ").append(bg).append("; padding: 25px 35px; line-height: 1.6; }\n");
        css.append("h1 { color: ").append(a).append("; font-size: 26px; border-bottom: 2px solid ").append(a)
                .append("; padding-bottom: 8px; margin-bottom: 20px; }\n");
        css.append("h2 { color: ").append(a).append("; font-size: 18px; border-bottom: 1px solid ").append(bdr)
                .append("; padding-bottom: 6px; margin-top: 30px; margin-bottom: 12px; }\n");
        css.append("h3 { color: ").append(a).append("; font-size: 14px; margin-top: 18px; margin-bottom: 6px; }\n");
        css.append("p, li { font-size: 13px; line-height: 1.6; }\n");
        css.append("a { color: ").append(linkColor).append("; text-decoration: underline; font-weight: bold; }\n");
        css.append("code { background-color: ").append(cbg).append("; border: 1px solid ").append(bdr)
                .append("; border-radius: 4px; padding: 1px 5px; font-family: monospace; font-size: 12px; }\n");
        css.append("pre { background-color: ").append(cbg).append("; border: 1px solid ").append(bdr).append(
                "; border-radius: 6px; padding: 12px 16px; font-family: monospace; font-size: 12px; margin: 10px 0; }\n");
        css.append("blockquote { background-color: ").append(card).append("; border-left: 4px solid ").append(a)
                .append("; padding: 10px 16px; margin: 12px 0; font-size: 13px; }\n");
        css.append("table { border-collapse: collapse; width: 100%; margin: 12px 0; }\n");
        css.append("th { background-color: ").append(cbg).append("; color: ").append(a)
                .append("; font-weight: bold; border: 1px solid ").append(bdr)
                .append("; padding: 8px 12px; font-size: 13px; text-align: left; }\n");
        css.append("td { border: 1px solid ").append(bdr).append("; padding: 8px 12px; font-size: 13px; }\n");
        css.append("hr { border: none; border-top: 1px solid ").append(bdr).append("; margin: 25px 0; }\n");

        return "<html><head><style>" + css.toString() + "</style></head><body>" + bodyHtml + "</body></html>";
    }

    /**
     * Opens the User Guide tab in the workspace.
     */
    public void openDocumentationPanel(String anchorId) {
        javax.swing.JEditorPane targetPane = null;
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if ("User Guide".equals(workspaceTabs.getTitleAt(i))) {
                workspaceTabs.setSelectedIndex(i);
                Component comp = workspaceTabs.getComponentAt(i);
                if (comp instanceof javax.swing.JPanel wrapper) {
                    if (wrapper.getComponentCount() > 0
                            && wrapper.getComponent(0) instanceof javax.swing.JScrollPane scroll) {
                        if (scroll.getViewport().getView() instanceof javax.swing.JEditorPane editor) {
                            targetPane = editor;
                        }
                    }
                }
                break;
            }
        }

        if (targetPane == null) {
            String markdown = loadUserGuideMarkdown();
            String html = renderUserGuideHtml(markdown);

            targetPane = new javax.swing.JEditorPane();
            targetPane.setContentType("text/html");
            targetPane.setEditable(false);
            targetPane.setBackground(javax.swing.UIManager.getColor("Panel.background"));
            targetPane.setText(html);
            targetPane.setCaretPosition(0);

            javax.swing.JEditorPane finalPane = targetPane;
            targetPane.addHyperlinkListener(e -> {
                if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                    String desc = e.getDescription();
                    if (desc != null && desc.startsWith("#")) {
                        String ref = desc.substring(1);
                        finalPane.scrollToReference(ref);
                    } else if (e.getURL() != null) {
                        try {
                            java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                        } catch (Exception ex) {
                            try {
                                if (desc != null && (desc.startsWith("http://") || desc.startsWith("https://"))) {
                                    java.awt.Desktop.getDesktop().browse(new java.net.URI(desc));
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            });

            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(targetPane);
            scroll.setBorder(null);
            javax.swing.JPanel wrapper = new javax.swing.JPanel(new java.awt.BorderLayout());

            javax.swing.JPanel searchBar = buildDocSearchBar(targetPane);
            wrapper.add(searchBar, java.awt.BorderLayout.NORTH);
            wrapper.add(scroll, java.awt.BorderLayout.CENTER);

            targetPane.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F,
                            java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "openSearch");
            targetPane.getActionMap().put("openSearch", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    searchBar.setVisible(true);
                    for (java.awt.Component c : searchBar.getComponents()) {
                        if (c instanceof javax.swing.JTextField) {
                            c.requestFocusInWindow();
                            ((javax.swing.JTextField) c).selectAll();
                            break;
                        }
                    }
                }
            });

            int idx = workspaceTabs.getTabCount();
            workspaceTabs.addTab("User Guide", wrapper);
            workspaceTabs.setTabComponentAt(idx, buildTabHeader("User Guide", idx, wrapper));
            workspaceTabs.setSelectedIndex(idx);
        }

        if (anchorId != null) {
            javax.swing.JEditorPane finalPane = targetPane;
            SwingUtilities.invokeLater(() -> finalPane.scrollToReference(anchorId));
        }
    }

    private JPanel buildDocSearchBar(javax.swing.JEditorPane editorPane) {
        JPanel outerPanel = new JPanel(new java.awt.BorderLayout());
        outerPanel.setBackground(UIManager.getColor("Panel.background"));
        outerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Workspace.borderColor")));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftPanel.setOpaque(false);

        JTextField searchField = new JTextField(20);
        JButton prevBtn = new JButton("Prev");
        JButton nextBtn = new JButton("Next");
        JButton closeBtn = new JButton("X");
        closeBtn.setMargin(new java.awt.Insets(2, 5, 2, 5));
        closeBtn.setToolTipText("Close Search (Esc)");

        JCheckBox exactCheck = new JCheckBox("Exact Match");
        JCheckBox fuzzyCheck = new JCheckBox("Fuzzy Search");
        JLabel countLabel = new JLabel("0/0");

        exactCheck.setOpaque(false);
        fuzzyCheck.setOpaque(false);

        leftPanel.add(new JLabel("Search:"));
        leftPanel.add(searchField);
        leftPanel.add(prevBtn);
        leftPanel.add(nextBtn);
        leftPanel.add(exactCheck);
        leftPanel.add(fuzzyCheck);
        leftPanel.add(countLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightPanel.setOpaque(false);
        rightPanel.add(closeBtn);

        outerPanel.add(leftPanel, java.awt.BorderLayout.CENTER);
        outerPanel.add(rightPanel, java.awt.BorderLayout.EAST);

        List<int[]> matchRanges = new ArrayList<>();
        int[] currentIndex = { -1 };

        boolean isDark = com.formdev.flatlaf.FlatLaf.isLafDark();
        Color hlColor = isDark ? new Color(100, 100, 0, 150) : new Color(255, 255, 0, 150);
        Color currentHlColor = isDark ? new Color(150, 100, 0, 200) : new Color(255, 150, 0, 200);

        javax.swing.text.Highlighter.HighlightPainter painter = new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(
                hlColor);
        javax.swing.text.Highlighter.HighlightPainter currentPainter = new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(
                currentHlColor);
        Object[] currentHighlightTag = { null };

        Runnable updateHighlight = () -> {
            try {
                if (currentHighlightTag[0] != null) {
                    editorPane.getHighlighter().removeHighlight(currentHighlightTag[0]);
                    currentHighlightTag[0] = null;
                }
                if (currentIndex[0] >= 0 && currentIndex[0] < matchRanges.size()) {
                    int[] range = matchRanges.get(currentIndex[0]);
                    currentHighlightTag[0] = editorPane.getHighlighter().addHighlight(range[0], range[1],
                            currentPainter);

                    editorPane.setCaretPosition(range[0]);
                    countLabel.setText((currentIndex[0] + 1) + " / " + matchRanges.size());
                } else {
                    countLabel.setText("0 / " + matchRanges.size());
                }
            } catch (Exception ex) {
            }
        };

        Runnable doSearch = () -> {
            editorPane.getHighlighter().removeAllHighlights();
            matchRanges.clear();
            currentIndex[0] = -1;
            currentHighlightTag[0] = null;

            String q = searchField.getText();
            if (q.isEmpty()) {
                countLabel.setText("0/0");
                return;
            }

            boolean exact = exactCheck.isSelected();
            boolean fuzzy = fuzzyCheck.isSelected();

            try {
                javax.swing.text.Document doc = editorPane.getDocument();
                String text = doc.getText(0, doc.getLength());

                if (fuzzy) {
                    StringBuilder regex = new StringBuilder();
                    for (char c : q.toCharArray()) {
                        if (Character.isWhitespace(c)) {
                            regex.append("\\s+");
                        } else {
                            regex.append(java.util.regex.Pattern.quote(String.valueOf(c))).append(".*?");
                        }
                    }
                    int flags = exact ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile(regex.toString(), flags);
                    java.util.regex.Matcher m = p.matcher(text);
                    while (m.find()) {
                        if (m.end() - m.start() <= q.length() * 3 + 15) {
                            matchRanges.add(new int[] { m.start(), m.end() });
                            editorPane.getHighlighter().addHighlight(m.start(), m.end(), painter);
                        }
                    }
                } else {
                    String targetText = exact ? text : text.toLowerCase();
                    String queryText = exact ? q : q.toLowerCase();
                    int idx = 0;
                    while ((idx = targetText.indexOf(queryText, idx)) >= 0) {
                        matchRanges.add(new int[] { idx, idx + q.length() });
                        editorPane.getHighlighter().addHighlight(idx, idx + q.length(), painter);
                        idx += q.length();
                    }
                }

                if (!matchRanges.isEmpty()) {
                    currentIndex[0] = 0;
                }
                updateHighlight.run();
            } catch (Exception ex) {
            }
        };

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                doSearch.run();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                doSearch.run();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                doSearch.run();
            }
        });

        searchField.addActionListener(e -> {
            if (matchRanges.isEmpty())
                return;
            currentIndex[0] = (currentIndex[0] + 1) % matchRanges.size();
            updateHighlight.run();
        });

        exactCheck.addActionListener(e -> doSearch.run());
        fuzzyCheck.addActionListener(e -> doSearch.run());

        nextBtn.addActionListener(e -> {
            if (matchRanges.isEmpty())
                return;
            currentIndex[0] = (currentIndex[0] + 1) % matchRanges.size();
            updateHighlight.run();
        });

        prevBtn.addActionListener(e -> {
            if (matchRanges.isEmpty())
                return;
            currentIndex[0] = (currentIndex[0] - 1 + matchRanges.size()) % matchRanges.size();
            updateHighlight.run();
        });

        closeBtn.addActionListener(e -> {
            outerPanel.setVisible(false);
            searchField.setText("");
            editorPane.getHighlighter().removeAllHighlights();
            editorPane.requestFocusInWindow();
        });

        searchField.getInputMap().put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                "closeSearch");
        searchField.getActionMap().put("closeSearch", new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                closeBtn.doClick();
            }
        });

        outerPanel.setVisible(false);
        return outerPanel;
    }

    public JButton createInfoBadge(String anchorId, String tooltip) {
        JButton badge = new JButton("?");
        badge.putClientProperty("JButton.buttonType", "help");
        badge.setToolTipText("Help: " + tooltip);
        badge.setFocusPainted(false);
        badge.setCursor(new Cursor(Cursor.HAND_CURSOR));
        badge.addActionListener(e -> openDocumentationPanel(anchorId));
        return badge;
    }

    private String loadUserGuideMarkdown() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/docs/UserGuide.md")) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        try {
            java.io.File f = new java.io.File("docs/UserGuide.md");
            if (f.exists()) {
                return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        try {
            java.io.File f = new java.io.File("../docs/UserGuide.md");
            if (f.exists()) {
                return java.nio.file.Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return "# ApiBanker User Guide\n\nUser guide document could not be loaded from docs/UserGuide.md.";
    }

    private String renderUserGuideHtml(String markdown) {
        Color ac = javax.swing.UIManager.getColor("AccentColor");
        if (ac == null)
            ac = new java.awt.Color(26, 115, 232);
        String a = String.format("#%02x%02x%02x", ac.getRed(), ac.getGreen(), ac.getBlue());
        boolean dk = com.formdev.flatlaf.FlatLaf.isLafDark();
        String bg = dk ? "#1e1e1e" : "#ffffff";
        String fg = dk ? "#cccccc" : "#333333";
        String card = dk ? "#2b2b2b" : "#f8f9fa";
        String bdr = dk ? "#3a3a3a" : "#e0e0e0";
        String cbg = dk ? "#1a1a1a" : "#f0f4f8";
        String linkColor = dk ? "#66b2ff" : "#0066cc";

        java.util.List<org.commonmark.Extension> extensions = java.util.Arrays
                .asList(org.commonmark.ext.gfm.tables.TablesExtension.create());
        org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder().extensions(extensions).build();
        org.commonmark.node.Node document = parser.parse(markdown);
        org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder()
                .extensions(extensions).build();
        String bodyHtml = renderer.render(document);

        StringBuilder css = new StringBuilder();
        css.append("body { font-family: 'Segoe UI', Arial, sans-serif; color: ").append(fg)
                .append("; background-color: ").append(bg).append("; padding: 25px 35px; line-height: 1.6; }\n");
        css.append("h1 { color: ").append(a).append("; font-size: 26px; border-bottom: 2px solid ").append(a)
                .append("; padding-bottom: 8px; margin-bottom: 20px; }\n");
        css.append("h2 { color: ").append(a).append("; font-size: 18px; border-bottom: 1px solid ").append(bdr)
                .append("; padding-bottom: 6px; margin-top: 30px; margin-bottom: 12px; }\n");
        css.append("h3 { color: ").append(a).append("; font-size: 14px; margin-top: 18px; margin-bottom: 6px; }\n");
        css.append("p, li { font-size: 13px; line-height: 1.6; }\n");
        css.append("a { color: ").append(linkColor).append("; text-decoration: underline; font-weight: bold; }\n");
        css.append("code { background-color: ").append(cbg).append("; border: 1px solid ").append(bdr)
                .append("; border-radius: 4px; padding: 1px 5px; font-family: monospace; font-size: 12px; }\n");
        css.append("pre { background-color: ").append(cbg).append("; border: 1px solid ").append(bdr).append(
                "; border-radius: 6px; padding: 12px 16px; font-family: monospace; font-size: 12px; margin: 10px 0; }\n");
        css.append("blockquote { background-color: ").append(card).append("; border-left: 4px solid ").append(a)
                .append("; padding: 10px 16px; margin: 12px 0; font-size: 13px; }\n");
        css.append("table { border-collapse: collapse; width: 100%; margin: 12px 0; }\n");
        css.append("th { background-color: ").append(cbg).append("; color: ").append(a)
                .append("; font-weight: bold; border: 1px solid ").append(bdr)
                .append("; padding: 8px 12px; font-size: 13px; text-align: left; }\n");
        css.append("td { border: 1px solid ").append(bdr).append("; padding: 8px 12px; font-size: 13px; }\n");
        css.append("hr { border: none; border-top: 1px solid ").append(bdr).append("; margin: 25px 0; }\n");

        return "<html><head><style>" + css.toString() + "</style></head><body>" + bodyHtml + "</body></html>";
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

    private void setupConsoleHotkey() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK),
                "openConsole");
        root.getActionMap().put("openConsole", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ConsoleDialog.showConsole(MainFrame.this);
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

    public void zoom(int increment) {
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
        if (ConsoleDialog.getInstance() != null) {
            ConsoleDialog.getInstance().updateFontSize(size);
        }
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
                } else if ("License & Copyright".equals(title)) {
                    type = "license";
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
        in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("APP_CLOSE", "System",
                "ApiBanker Version " + App.getVersion() + " closed.");
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
        } else if ("license".equals(ts.getType())) {
            openLicensePanel();
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
            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("DELETE_COLLECTION", "User",
                    "Deleted: [" + target.getName() + " / " + target.getId() + "]");
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
