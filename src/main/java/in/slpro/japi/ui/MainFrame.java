package in.slpro.japi.ui;

import in.slpro.japi.App;
import in.slpro.japi.model.*;
import in.slpro.japi.storage.StorageManager;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * MainFrame
 *
 * <p>
 * This is the central UI Controller and Root Window for the JAPI application.
 * It manages the primary layout (Sidebar vs Workspace), handles global application state,
 * routes actions from the Menu Bar, and orchestrates the lifecycle of all Workspace Tabs
 * (Requests, Environments, Mock Server, etc.). It acts as the central event bus for 
 * saving, restoring, and persisting UI state across sessions.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
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
     * Locates the parent {@link CollectionModel} (folder/collection) that contains the given request.
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
    public List<RequestModel> getHistoryList() { return history; }

    /**
     * A static helper to locate the parent collection of a request without needing a direct reference
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
     * 1. Loads persisted state (Collections, Environments, History) via {@link StorageManager}.
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

        setTitle("JAPI - Offline API Client");

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

    /**
     * Initializes the core UI hierarchy.
     * <p>
     * Sets up the global JMenuBar, the structural JSplitPane separating the Sidebar from the Workspace,
     * and initializes the CardLayout responsible for toggling between the Welcome screen and the Tabbed Pane.
     * Finally, it restores any previously open tabs from the user's last session via {@link AppSettings}.
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
        workspaceTabs.putClientProperty("JTabbedPane.trailingComponent", buildEnvSelector());

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
        JMenuItem importItem = new JMenuItem("Import ");
        importItem.addActionListener(e -> importPostmanFiles());
        JMenuItem exportItem = new JMenuItem("Export...");
        exportItem.addActionListener(e -> openExportTab());
        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(e -> openSettings());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> onClose());
        fileMenu.add(newReqItem);
        fileMenu.addSeparator();
        fileMenu.add(importItem);
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(settingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem consoleItem = new JMenuItem("Console");
        consoleItem.addActionListener(e -> new ConsoleDialog(this).setVisible(true));
        JMenuItem logConsoleItem = new JMenuItem("Log Console Tab");
        logConsoleItem.addActionListener(e -> openLogConsole());
        JMenuItem envMgrItem = new JMenuItem("Environment Manager...");
        envMgrItem.addActionListener(e -> openEnvManager());
        viewMenu.add(consoleItem);
        viewMenu.add(logConsoleItem);
        viewMenu.add(envMgrItem);

        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem jwtItem = new JMenuItem("JWT Decoder");
        jwtItem.addActionListener(e -> openJwtDecoder());
        JMenuItem jsonItem = new JMenuItem("JSON Tool");
        jsonItem.addActionListener(e -> openJsonTool());
        JMenuItem compareItem = new JMenuItem("Data Comparator");
        compareItem.addActionListener(e -> openDataComparator());
        JMenuItem mockServerItem = new JMenuItem("Mock Server");
        mockServerItem.addActionListener(e -> openMockServer());
        JMenuItem dataToolsItem = new JMenuItem("Data Tools");
        dataToolsItem.addActionListener(e -> openDataTools());
        JMenuItem cookieJarItem = new JMenuItem("Cookie Jar Manager...");
        cookieJarItem.addActionListener(e -> openCookieJarManager());
        toolsMenu.add(jwtItem);
        toolsMenu.add(jsonItem);
        toolsMenu.add(compareItem);
        toolsMenu.addSeparator();
        toolsMenu.add(mockServerItem);
        toolsMenu.add(dataToolsItem);
        toolsMenu.add(cookieJarItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About JAPI...");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        bar.add(fileMenu);
        bar.add(viewMenu);
        bar.add(toolsMenu);
        bar.add(helpMenu);
        return bar;
    }

    private JPanel buildEnvSelector() {
        envSelectorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        envSelectorPanel.setOpaque(false);
        envSelectorPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 10));

        envCombo = new JComboBox<>();
        envCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshEnvCombo();

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

        envManageBtn = new JButton("⚙");
        envManageBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        envManageBtn.setToolTipText("Manage Environments");
        envManageBtn.setFocusPainted(false);
        envManageBtn.addActionListener(e -> openEnvManager());
        envSelectorPanel.add(envManageBtn);

        // Adjust initial dimensions based on currentFontSize
        int height = Math.max(26, currentFontSize + 10);
        int width = Math.max(140, currentFontSize * 9);
        envCombo.setPreferredSize(new Dimension(width, height));
        envManageBtn.setPreferredSize(new Dimension(height, height));

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
        targetWrapper.model.getRequests().add(req);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
    }

    public void refreshCollections(List<CollectionModel> collections) {
        sidebarPanel.refreshCollections(collections);
    }

    public void createCollection(String name) {
        CollectionModel col = new CollectionModel(UUID.randomUUID().toString(), name);
        col.setRequests(new ArrayList<>());
        collections.add(col);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
    }

    public void deleteCollection(CollectionModel col) {
        if (OTHERS_COLLECTION_ID.equals(col.getId())) {
            JOptionPane.showMessageDialog(this, "Cannot delete the 'Others' collection.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
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

    public void addRequestToCollection(CollectionModel col, String name) {
        RequestModel req = new RequestModel();
        req.setName(name);
        col.getRequests().add(req);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRequest(req);
    }

    public void addRunnerToCollection(CollectionModel col) {
        RequestModel runner = new RequestModel();
        runner.setName(col.getName() + " Runner");
        runner.setType("runner");
        runner.setMethod("RUNNER");
        col.getRequests().add(runner);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRunner(col, runner);
    }

    public void deleteRequest(RequestModel req) {
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

    public void addComparatorToCollection(CollectionModel col) {
        RequestModel comp = new RequestModel();
        comp.setName(col.getName() + " Comparator");
        comp.setType("comparator");
        comp.setMethod("COMPARE");
        col.getRequests().add(comp);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRequest(comp);
    }

    public void addMockServerToCollection(CollectionModel col) {
        RequestModel mock = new RequestModel();
        mock.setName(col.getName() + " Mock Server");
        mock.setType("mockserver");
        mock.setMethod("MOCK");
        col.getRequests().add(mock);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openMockServer(mock);
    }

    public void addWebSocketToCollection(CollectionModel col) {
        RequestModel ws = new RequestModel();
        ws.setName(col.getName() + " WS Client");
        ws.setType("websocket");
        ws.setMethod("WS");
        col.getRequests().add(ws);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
        openRequest(ws);
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
        if (accentColor == null) accentColor = new Color(26, 115, 232);
        String accentHex = String.format("#%02x%02x%02x", accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue());
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
                + "  <h1 style='color:" + accentHex + "; font-size:36px; margin:0;'>Japi</h1>"
                + "  <h2 style='font-weight:normal; font-size:18px; margin:5px 0 15px 0;'>The Ultimate Offline API Client & Collection Runner</h2>"
                + "  <div style='font-size:12px; color:#888;'>Version " + in.slpro.japi.App.getVersion()
                + " | Secured Offline-First Architecture | From SL Pro</div>"
                + "</div>"
                + "<hr style='margin-bottom:30px;'>"
                + "<table width='100%' cellpadding='10' cellspacing='10'>"
                + "  <tr>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>🚀 Collection Runner</h3>"
                + "      <p style='font-size:13px; line-height:1.5;'>Execute whole API suites concurrently with configurable virtual users and delay. Monitor real-time logs, view live multiline analytics charts (for response codes and latency percentiles), and export polished PDF or Excel summary reports.</p>"
                + "    </td>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>⚙️ Environment Management</h3>"
                + "      <p style='font-size:13px; line-height:1.5;'>Create, import, export, and switch environments instantly. Dynamically substitute double-brace variables (e.g. <code>{{url}}</code>) across headers, parameters, and bodies.</p>"
                + "    </td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>🛠️ Rhino Scripting sandbox</h3>"
                + "      <p style='font-size:13px; line-height:1.5;'>Write custom JavaScript code inside Pre-request and Post-request tabs to build dynamic workflows, manipulate variables, and chain requests.</p>"
                + "    </td>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>⚡ Integrated Tool Suite</h3>"
                + "      <p style='font-size:13px; line-height:1.5;'>Includes a built-in JWT Decoder, Data Comparator, mock JSON editor, native JMeter (.jmx) imports/exports, and offline local Mock Server.</p>"
                + "    </td>"
                + "  </tr>"
                + "  <tr>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>🔒 Privacy & Local Security</h3>"
                + "      <ul style='font-size:13px; line-height:1.6; margin:0; padding-left:20px;'>"
                + "        <li><b>Local-First:</b> No external telemetry, tracking, or user registrations.</li>"
                + "        <li><b>Local SSL:</b> Trusts self-signed certificates for localhost tests.</li>"
                + "        <li><b>Git-Friendly:</b> Save files directly to local, human-readable JSON workspaces.</li>"
                + "      </ul>"
                + "    </td>"
                + "    <td width='50%' valign='top' style='background:" + cardBgHex
                + "; border: 1px solid " + borderColorHex + "; border-radius:6px; padding:15px;'>"
                + "      <h3 style='color:" + accentHex + "; margin-top:0;'>⌨️ Keyboard Shortcuts</h3>"
                + "      <table style='font-size:13px; width:100%; border-collapse:collapse;'>"
                + "        <tr><td style='padding:3px 0;'><b>Ctrl + S</b></td><td>Save Active Tab</td></tr>"
                + "        <tr><td style='padding:3px 0;'><b>Ctrl + = / +</b></td><td>Zoom In UI</td></tr>"
                + "        <tr><td style='padding:3px 0;'><b>Ctrl + -</b></td><td>Zoom Out UI</td></tr>"
                + "        <tr><td style='padding:3px 0;'><b>Right-Click Tabs</b></td><td>Pin, Rename, Close Options</td></tr>"
                + "      </table>"
                + "    </td>"
                + "  </tr>"
                + "</table>"
                + "<div style='margin-top:30px; text-align:center; font-size:12px; color:#888;'>"
                + "  © 2026 JAPI by SLPRO. All Rights Reserved."
                + "</div>"
                + "</body></html>";

        welcomePane.setText(html);
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

    private void openExportTab() {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof ExportPanel) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
        ExportPanel panel = new ExportPanel(this);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Export", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Export", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openJwtDecoder() {
        RequestModel req = new RequestModel();
        req.setName("JWT Decoder");
        req.setType("jwt");
        JwtDecoderPanel panel = new JwtDecoderPanel(this, req);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("JWT Decoder", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("JWT Decoder", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openJsonTool() {
        RequestModel req = new RequestModel();
        req.setName("JSON Tool");
        JsonToolPanel panel = new JsonToolPanel(this, req);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("JSON Tool", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("JSON Tool", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
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
            } else if (tabContent instanceof CollectionRunnerPanel crp) {
                crp.saveConfig();
                saveCollections();
            } else if (tabContent instanceof LogConsolePanel lcp) {
                lcp.removeListener();
            } else if (tabContent instanceof MockServerPanel msp) {
                msp.stopServerIfRunning();
                msp.updateModel();
                saveCollections();
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
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, currentFontSize));

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
                } else if (e.getClickCount() == 2 && renameable && startEditAction != null) {
                    startEditAction.run();
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
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Environment Manager", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Environment Manager", idx, panel));
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
                    collections.add(col);
                    int totalRequests = countRequestsRecursive(col);
                    importedCollections.add(col.getName() + " (" + totalRequests + " requests)");
                } else if (root.has("values")) {
                    // Import as Environment
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
                } else {
                    failedFiles.add(file.getName() + " (unrecognized Postman format)");
                }
            } catch (Exception e) {
                failedFiles.add(file.getName() + " (" + e.getMessage() + ")");
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
                parsePostmanItemsRecursive(item.getAsJsonArray("item"), subFolder);
                parent.getFolders().add(subFolder);
            } else if (item.has("request")) {
                RequestModel req = new RequestModel();
                req.setId(UUID.randomUUID().toString());
                req.setName(item.has("name") ? item.get("name").getAsString() : "Request");
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
        chooser.setSelectedFile(new File(col.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json"));
        chooser.setDialogTitle("Export Collection");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        lastFileChooserDirectory = chooser.getSelectedFile().getParentFile();
        try {
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            com.google.gson.JsonObject info = new com.google.gson.JsonObject();
            info.addProperty("name", col.getName());
            info.addProperty("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
            info.addProperty("_exporter_id", "JAPI-" + in.slpro.japi.App.getVersion());
            info.addProperty("_exported_by", "JAPI v" + in.slpro.japi.App.getVersion() + " (Offline API Client)");
            info.addProperty("_exported_at",
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date()));
            root.add("info", info);
            root.addProperty("_japi_version", in.slpro.japi.App.getVersion());
            com.google.gson.JsonArray items = new com.google.gson.JsonArray();
            exportCollectionRecursive(col, items);
            root.add("item", items);
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), gson.toJson(root));
            showToast(this, "Collection exported successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCollectionRecursive(CollectionModel col, com.google.gson.JsonArray items) {
        if (col.getFolders() != null) {
            for (CollectionModel folder : col.getFolders()) {
                com.google.gson.JsonObject folderObj = new com.google.gson.JsonObject();
                folderObj.addProperty("name", folder.getName());
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
                reqObj.add("url", url);
                item.add("request", reqObj);
                items.add(item);
            }
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
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Settings", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Settings", idx, panel));
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
            envManageBtn.setPreferredSize(new Dimension(height, height));
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
        }
        storage.saveSettings();

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

    private boolean deleteCollectionRecursive(List<CollectionModel> list, CollectionModel target) {
        if (list.remove(target)) {
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
}
