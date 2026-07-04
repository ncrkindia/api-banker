package in.slpro.japi.ui;

import in.slpro.japi.App;
import in.slpro.japi.model.*;
import in.slpro.japi.storage.StorageManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class MainFrame extends JFrame {
    private final StorageManager storage;
    private List<CollectionModel> collections;
    private List<EnvironmentModel> environments;
    private List<RequestModel> history;

    private SidebarPanel sidebarPanel;
    private JTabbedPane workspaceTabs;
    private JComboBox<String> envCombo;
    private JLabel envIndicator;

    private int currentFontSize = 16;

    private static final int MAX_HISTORY = 500;

    public MainFrame() {
        this.storage = StorageManager.getInstance();
        this.collections = storage.loadCollections();
        this.environments = storage.loadEnvironments();
        this.history = storage.loadHistory();

        setTitle("JAPI - Offline API Client");
        setSize(1300, 800);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { onClose(); }
        });

        initUI();
        setupZoom();
        applyTheme();
    }

    private void initUI() {
        // Menu bar
        setJMenuBar(buildMenuBar());

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Sidebar
        sidebarPanel = new SidebarPanel(this);
        mainPanel.add(sidebarPanel, BorderLayout.WEST);

        // Workspace
        workspaceTabs = new JTabbedPane();
        workspaceTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        mainPanel.add(workspaceTabs, BorderLayout.CENTER);

        // Bottom status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        if ("dark".equals(storage.getSettings().getTheme())) {
            statusBar.setBackground(new Color(40, 44, 52));
            statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 64, 72)));
        } else {
            statusBar.setBackground(new Color(243, 243, 243));
            statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        }
        statusBar.add(buildEnvSelector());
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Load collections into sidebar
        sidebarPanel.refreshCollections(collections);
        sidebarPanel.refreshHistory(history);

        // Open welcome tab
        openWelcomeTab();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newReqItem = new JMenuItem("New Request");
        newReqItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newReqItem.addActionListener(e -> openNewRequest());
        JMenuItem importItem = new JMenuItem("Import Postman Collection...");
        importItem.addActionListener(e -> importPostmanCollection());
        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(e -> openSettings());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> onClose());
        fileMenu.add(newReqItem);
        fileMenu.addSeparator();
        fileMenu.add(importItem);
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
        JMenuItem compareItem = new JMenuItem("Text Comparator");
        compareItem.addActionListener(e -> openComparator());
        JMenuItem mockServerItem = new JMenuItem("Mock Server");
        mockServerItem.addActionListener(e -> openMockServer());
        JMenuItem dataToolsItem = new JMenuItem("Data Tools");
        dataToolsItem.addActionListener(e -> openDataTools());
        toolsMenu.add(jwtItem);
        toolsMenu.add(jsonItem);
        toolsMenu.add(compareItem);
        toolsMenu.addSeparator();
        toolsMenu.add(mockServerItem);
        toolsMenu.add(dataToolsItem);

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
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel("Environment:");
        if ("dark".equals(storage.getSettings().getTheme())) {
            label.setForeground(Color.WHITE);
        } else {
            label.setForeground(new Color(33, 33, 33));
        }
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(label);

        envCombo = new JComboBox<>();
        envCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        envCombo.setPreferredSize(new Dimension(180, 22));
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
        });

        panel.add(envCombo);

        JButton managBtn = new JButton("Manage");
        managBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        managBtn.addActionListener(e -> openEnvManager());
        panel.add(managBtn);

        return panel;
    }

    public void refreshEnvCombo() {
        String activeId = storage.getSettings().getActiveEnvironmentId();
        envCombo.removeAllItems();
        envCombo.addItem("No Environment");
        int selIdx = 0;
        for (int i = 0; i < environments.size(); i++) {
            envCombo.addItem(environments.get(i).getName());
            if (environments.get(i).getId().equals(activeId)) selIdx = i + 1;
        }
        envCombo.setSelectedIndex(selIdx);
    }

    public EnvironmentModel getActiveEnvironment() {
        int idx = envCombo.getSelectedIndex();
        if (idx > 0 && idx - 1 < environments.size()) return environments.get(idx - 1);
        return null;
    }

    // ─── Collections ─────────────────────────────────────────────────────────

    public List<CollectionModel> getCollections() { return collections; }

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
        collections.remove(col);
        saveCollections();
        sidebarPanel.refreshCollections(collections);
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
            col.getRequests().remove(req);
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

    public void duplicateRequest(RequestModel req) {
        for (CollectionModel col : collections) {
            if (col.getRequests().contains(req)) {
                RequestModel dup = new RequestModel();
                dup.setName(req.getName() + " Copy");
                dup.setMethod(req.getMethod());
                dup.setUrl(req.getUrl());
                dup.setHeaders(new ArrayList<>(req.getHeaders()));
                dup.setParams(new ArrayList<>(req.getParams()));
                dup.setBodyType(req.getBodyType());
                dup.setBodyRawContent(req.getBodyRawContent());
                dup.setBodyRawType(req.getBodyRawType());
                col.getRequests().add(col.getRequests().indexOf(req) + 1, dup);
                saveCollections();
                sidebarPanel.refreshCollections(collections);
                openRequest(dup);
                break;
            }
        }
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
            if (c instanceof RequestPanel rp && rp.getRequestModel().getId().equals(req.getId())) {
                workspaceTabs.setTitleAt(i, req.getName());
                break;
            }
        }
    }

    // ─── Tabs ─────────────────────────────────────────────────────────────────

    public void openRequest(RequestModel req) {
        if ("runner".equals(req.getType())) {
            CollectionModel parentCol = null;
            for (CollectionModel col : collections) {
                for (RequestModel r : col.getRequests()) {
                    if (r.getId() != null && r.getId().equals(req.getId())) {
                        parentCol = col;
                        break;
                    }
                }
                if (parentCol != null) break;
            }
            if (parentCol != null) {
                openRunner(parentCol, req);
                return;
            }
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
        JPanel welcome = new JPanel(new BorderLayout());
        welcome.setBackground(Color.WHITE);
        JLabel lbl = new JLabel("<html><center><h2>Welcome to JAPI</h2><p>Select a request from the sidebar or create a new one.</p></center></html>", SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(new Color(100, 100, 100));
        welcome.add(lbl, BorderLayout.CENTER);
        workspaceTabs.addTab("Welcome", welcome);
    }

    private void openNewRequest() {
        RequestModel req = new RequestModel();
        req.setName("Untitled Request");
        openRequest(req);
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

    public void openComparator() {
        RequestModel req = new RequestModel();
        req.setName("Comparator");
        ComparatorPanel panel = new ComparatorPanel(this, req);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Comparator", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Comparator", idx, panel));
        workspaceTabs.setSelectedIndex(idx);
    }

    public void openMockServer() {
        MockServerPanel panel = new MockServerPanel(this);
        int idx = workspaceTabs.getTabCount();
        workspaceTabs.addTab("Mock Server", panel);
        workspaceTabs.setTabComponentAt(idx, buildTabHeader("Mock Server", idx, panel));
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

    private JPanel buildTabHeader(String title, int tabIndex, Component tabContent) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JButton closeBtn = new JButton("×");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(18, 18));
        closeBtn.addActionListener(e -> {
            int idx = workspaceTabs.indexOfComponent(tabContent);
            if (idx >= 0) {
                // Save before closing if it's a request
                if (tabContent instanceof RequestPanel rp) {
                    rp.getRequestModel(); // triggers collect
                    saveCollections();
                } else if (tabContent instanceof CollectionRunnerPanel crp) {
                    crp.saveConfig();
                    saveCollections();
                } else if (tabContent instanceof LogConsolePanel lcp) {
                    lcp.removeListener();
                }
                workspaceTabs.removeTabAt(idx);
            }
        });
        header.add(titleLabel);
        header.add(closeBtn);
        return header;
    }

    // ─── Environments ─────────────────────────────────────────────────────────

    public List<EnvironmentModel> getEnvironments() { return environments; }

    public void setEnvironments(List<EnvironmentModel> envs) {
        this.environments = envs;
        storage.saveEnvironments(envs);
        refreshEnvCombo();
    }

    private void openEnvManager() {
        new EnvironmentManagerDialog(this).setVisible(true);
        sidebarPanel.refreshCollections(collections); // refresh in case env changed
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
        if (history.size() > MAX_HISTORY) history = new ArrayList<>(history.subList(0, MAX_HISTORY));
        storage.saveHistory(history);
        sidebarPanel.refreshHistory(history);
    }

    public void clearHistory() {
        history.clear();
        storage.saveHistory(history);
    }

    // ─── Import / Export ─────────────────────────────────────────────────────

    public void importPostmanCollection() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Postman Collection (.json)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            String json = java.nio.file.Files.readString(chooser.getSelectedFile().toPath());
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            CollectionModel col = new CollectionModel();
            col.setId(UUID.randomUUID().toString());

            com.google.gson.JsonObject info = root.has("info") ? root.getAsJsonObject("info") : null;
            col.setName(info != null && info.has("name") ? info.get("name").getAsString() : chooser.getSelectedFile().getName());

            List<RequestModel> reqs = new ArrayList<>();
            if (root.has("item") && root.get("item").isJsonArray()) {
                parsePostmanItems(root.getAsJsonArray("item"), reqs);
            }
            col.setRequests(reqs);
            collections.add(col);
            saveCollections();
            sidebarPanel.refreshCollections(collections);
            JOptionPane.showMessageDialog(this, "Imported " + reqs.size() + " requests into \"" + col.getName() + "\".");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Import failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void parsePostmanItems(com.google.gson.JsonArray items, List<RequestModel> reqs) {
        for (com.google.gson.JsonElement el : items) {
            com.google.gson.JsonObject item = el.getAsJsonObject();
            if (item.has("item")) {
                // Folder
                parsePostmanItems(item.getAsJsonArray("item"), reqs);
            } else if (item.has("request")) {
                RequestModel req = new RequestModel();
                req.setId(UUID.randomUUID().toString());
                req.setName(item.has("name") ? item.get("name").getAsString() : "Request");
                com.google.gson.JsonObject reqObj = item.getAsJsonObject("request");
                req.setMethod(reqObj.has("method") ? reqObj.get("method").getAsString() : "GET");

                if (reqObj.has("url")) {
                    com.google.gson.JsonElement urlEl = reqObj.get("url");
                    if (urlEl.isJsonObject()) {
                        req.setUrl(urlEl.getAsJsonObject().has("raw") ? urlEl.getAsJsonObject().get("raw").getAsString() : "");
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
                                String lang = opts.getAsJsonObject("raw").has("language") ? opts.getAsJsonObject("raw").get("language").getAsString() : "json";
                                req.setBodyRawType(lang.toUpperCase());
                            }
                        } else {
                            req.setBodyRawType("JSON");
                        }
                    }
                }
                reqs.add(req);
            }
        }
    }

    public void exportCollection(CollectionModel col) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(col.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json"));
        chooser.setDialogTitle("Export Collection");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            com.google.gson.JsonObject info = new com.google.gson.JsonObject();
            info.addProperty("name", col.getName());
            info.addProperty("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
            root.add("info", info);
            com.google.gson.JsonArray items = new com.google.gson.JsonArray();
            for (RequestModel req : col.getRequests()) {
                if ("runner".equals(req.getType())) continue;
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
            root.add("item", items);
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), gson.toJson(root));
            JOptionPane.showMessageDialog(this, "Collection exported successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    private void openSettings() {
        JDialog dialog = new JDialog(this, "Settings", true);
        dialog.setSize(420, 280);
        dialog.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; panel.add(new JLabel("Data Directory:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; JTextField dirField = new JTextField(storage.getSettings().getDataDirectory()); panel.add(dirField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton browseBtn = new JButton("Browse");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(storage.getSettings().getDataDirectory());
            fc.setDialogTitle("Select Storage Folder for JAPI");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                dirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        panel.add(browseBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; panel.add(new JLabel("Theme:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JComboBox<String> themeCombo = new JComboBox<>(new String[]{"light", "dark"});
        themeCombo.setSelectedItem(storage.getSettings().getTheme());
        panel.add(themeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; panel.add(new JLabel("Enable Request Logging:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JCheckBox loggingCheck = new JCheckBox();
        loggingCheck.setSelected(storage.getSettings().isEnableLogging());
        panel.add(loggingCheck, gbc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            storage.updateDataDirectory(dirField.getText().trim());
            storage.getSettings().setTheme((String) themeCombo.getSelectedItem());
            storage.getSettings().setEnableLogging(loggingCheck.isSelected());
            storage.saveSettings();
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "Settings saved. Restart for theme changes.", "Settings", JOptionPane.INFORMATION_MESSAGE);
        });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        btns.add(cancelBtn);
        btns.add(saveBtn);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "<html><center><b>JAPI - Offline API Client</b><br>" +
                "Version 1.0.0<br><br>" +
                "A fast, modern, and completely offline API testing tool.<br><br>" +
                "Built with Java + Swing</center></html>",
                "About JAPI", JOptionPane.INFORMATION_MESSAGE);
    }

    private void applyTheme() {
        String theme = storage.getSettings().getTheme();
        try {
            App.setupTheme(theme, currentFontSize);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {}
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
            @Override public void actionPerformed(ActionEvent e) { zoom(1); }
        });
        root.getActionMap().put("zoomOut", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { zoom(-1); }
        });
    }

    private void zoom(int increment) {
        currentFontSize += increment;
        if (currentFontSize < 10) currentFontSize = 10;
        if (currentFontSize > 24) currentFontSize = 24;

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
                } else if (tab instanceof ComparatorPanel cp) {
                    cp.updateFontSize(size);
                } else if (tab instanceof CollectionRunnerPanel crp) {
                    crp.updateFontSize(size);
                }
            }
        }
        FontScaleHelper.scaleFonts(this, size);
    }

    private void onClose() {
        // Save open requests
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            Component c = workspaceTabs.getComponentAt(i);
            if (c instanceof RequestPanel rp) {
                rp.getRequestModel(); // triggers collectModel
            } else if (c instanceof CollectionRunnerPanel crp) {
                crp.saveConfig();
            }
        }
        saveCollections();
        storage.saveHistory(history);
        System.exit(0);
    }
}
