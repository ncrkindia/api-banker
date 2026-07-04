package in.slpro.japi.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import in.slpro.japi.model.CollectionModel;
import in.slpro.japi.model.RequestModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class MockServerPanel extends JPanel {
    private final MainFrame mainFrame;
    private final RequestModel model;

    private HttpServer server;
    private boolean isRunning = false;

    private final JTextField portField;
    private final JButton startStopBtn;
    private final JLabel statusLabel;

    private final JTable rulesTable;
    private final DefaultTableModel rulesModel;

    // Rule Editor Components
    private final JComboBox<String> methodCombo;
    private final JTextField pathField;
    private final JComboBox<String> responseModeCombo;

    // Responses List Components
    private final JTable responsesTable;
    private final DefaultTableModel responsesModel;

    // Selected Response Details Components
    private final JTextField statusField;
    private final JComboBox<String> contentTypeCombo;
    private final JComboBox<String> matchTypeCombo;
    private final JTextField matchKeyField;
    private final JTextField matchValueField;
    private final JTextArea mockBodyArea;

    private final JTextArea serverLogArea;

    public static class MockResponse {
        public int responseStatus = 200;
        public String contentType = "application/json";
        public String body = "{\n  \"status\": \"success\"\n}";
        public String matchType = "none"; // none, query, header, body
        public String matchKey = "";
        public String matchValue = "";
    }

    public static class MockRule {
        public String method = "GET";
        public String path = "/";
        public String responseMode = "fixed"; // fixed, sequence, random
        public List<MockResponse> responses = new ArrayList<>();
        public transient int sequenceIndex = 0;

        public MockRule() {}

        public MockRule(String method, String path, int responseStatus, String contentType, String body) {
            this.method = method;
            this.path = path.startsWith("/") ? path : "/" + path;
            this.responseMode = "fixed";
            MockResponse resp = new MockResponse();
            resp.responseStatus = responseStatus;
            resp.contentType = contentType;
            resp.body = body;
            this.responses.add(resp);
        }
    }

    private static class MockServerConfig {
        int port = 8085;
        List<MockRule> rules = new ArrayList<>();
    }

    private final List<MockRule> rulesList = new ArrayList<>();
    private MockRule activeRule;
    private MockResponse activeResponse;

    public MockServerPanel(MainFrame mainFrame, RequestModel model) {
        this.mainFrame = mainFrame;
        this.model = model;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        // --- TOP BAR: Server Controls ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBackground(UIManager.getColor("Panel.background"));

        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("8085", 6);
        topPanel.add(portField);

        startStopBtn = new JButton("Start Server");
        startStopBtn.setBackground(new Color(46, 204, 113));
        startStopBtn.setForeground(Color.WHITE);
        startStopBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        startStopBtn.addActionListener(e -> toggleServer());
        topPanel.add(startStopBtn);

        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topPanel.add(statusLabel);

        // Add Save Mock Config
        JButton saveConfigBtn = new JButton("Save Config");
        saveConfigBtn.addActionListener(e -> {
            updateModel();
            mainFrame.saveCollections();
            MainFrame.showToast(this, "Mock Server configuration saved!");
        });
        topPanel.add(saveConfigBtn);

        // Add Import Actions
        JButton importApisBtn = new JButton("Import APIs");
        importApisBtn.addActionListener(e -> importApisFromCollection());
        topPanel.add(importApisBtn);

        JButton importOpenApiBtn = new JButton("Import OpenAPI Spec");
        importOpenApiBtn.addActionListener(e -> importOpenApiSpec());
        topPanel.add(importOpenApiBtn);

        add(topPanel, BorderLayout.NORTH);

        // --- CENTER: Rules Management & Log ---
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.65);

        // Left Component: Rules Editor & Table
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBackground(UIManager.getColor("Panel.background"));

        rulesModel = new DefaultTableModel(new String[]{"Method", "Path", "Mode", "ResponsesCount"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rulesTable = new JTable(rulesModel);
        rulesTable.setRowHeight(24);
        rulesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane tableScroll = new JScrollPane(rulesTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Configured Mock Rules"));
        leftPanel.add(tableScroll, BorderLayout.CENTER);

        // Rule Editor Panel (JSplitPane vertical)
        JPanel editorPanel = new JPanel(new BorderLayout(10, 10));
        editorPanel.setBorder(BorderFactory.createTitledBorder("Rule Editor"));

        // General settings
        JPanel generalSettings = new JPanel(new GridBagLayout());
        generalSettings.setBackground(UIManager.getColor("Panel.background"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        generalSettings.add(new JLabel("Method:"), gbc);
        gbc.gridx = 1;
        methodCombo = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE", "PATCH"});
        generalSettings.add(methodCombo, gbc);

        gbc.gridx = 2;
        generalSettings.add(new JLabel("Path:"), gbc);
        gbc.gridx = 3; gbc.weightx = 1.0;
        pathField = new JTextField("/api/users", 15);
        generalSettings.add(pathField, gbc);

        gbc.gridx = 4; gbc.weightx = 0.0;
        generalSettings.add(new JLabel("Response Mode:"), gbc);
        gbc.gridx = 5;
        responseModeCombo = new JComboBox<>(new String[]{"fixed", "sequence", "random"});
        generalSettings.add(responseModeCombo, gbc);

        editorPanel.add(generalSettings, BorderLayout.NORTH);

        // Split responses and details
        JSplitPane responseSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        responseSplit.setResizeWeight(0.4);

        // Response List Panel
        JPanel responseListPanel = new JPanel(new BorderLayout(5, 5));
        responsesModel = new DefaultTableModel(new String[]{"#", "Status", "Content-Type", "Criteria"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        responsesTable = new JTable(responsesModel);
        responsesTable.setRowHeight(22);
        JScrollPane responseTableScroll = new JScrollPane(responsesTable);
        responseTableScroll.setPreferredSize(new Dimension(150, 120));
        responseListPanel.add(responseTableScroll, BorderLayout.CENTER);

        JPanel responseButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton addResponseBtn = new JButton("Add Resp");
        addResponseBtn.addActionListener(e -> addResponse());
        JButton deleteResponseBtn = new JButton("Delete Resp");
        deleteResponseBtn.addActionListener(e -> deleteResponse());
        responseButtons.add(addResponseBtn);
        responseButtons.add(deleteResponseBtn);
        responseListPanel.add(responseButtons, BorderLayout.SOUTH);

        responseSplit.setLeftComponent(responseListPanel);

        // Response Details Panel
        JPanel responseDetailsPanel = new JPanel(new GridBagLayout());
        responseDetailsPanel.setBackground(UIManager.getColor("Panel.background"));
        GridBagConstraints gbcDet = new GridBagConstraints();
        gbcDet.insets = new Insets(3, 3, 3, 3);
        gbcDet.fill = GridBagConstraints.HORIZONTAL;

        gbcDet.gridx = 0; gbcDet.gridy = 0;
        responseDetailsPanel.add(new JLabel("Status:"), gbcDet);
        gbcDet.gridx = 1;
        statusField = new JTextField("200", 6);
        responseDetailsPanel.add(statusField, gbcDet);

        gbcDet.gridx = 2;
        responseDetailsPanel.add(new JLabel("Type:"), gbcDet);
        gbcDet.gridx = 3; gbcDet.weightx = 1.0;
        contentTypeCombo = new JComboBox<>(new String[]{"application/json", "application/xml", "text/plain", "text/html"});
        responseDetailsPanel.add(contentTypeCombo, gbcDet);

        // Criteria Matching
        gbcDet.gridx = 0; gbcDet.gridy = 1; gbcDet.weightx = 0.0;
        responseDetailsPanel.add(new JLabel("Match:"), gbcDet);
        gbcDet.gridx = 1;
        matchTypeCombo = new JComboBox<>(new String[]{"none", "query", "header", "body"});
        responseDetailsPanel.add(matchTypeCombo, gbcDet);

        gbcDet.gridx = 2;
        responseDetailsPanel.add(new JLabel("Key:"), gbcDet);
        gbcDet.gridx = 3; gbcDet.weightx = 1.0;
        matchKeyField = new JTextField();
        responseDetailsPanel.add(matchKeyField, gbcDet);

        gbcDet.gridx = 0; gbcDet.gridy = 2; gbcDet.weightx = 0.0;
        responseDetailsPanel.add(new JLabel("Value:"), gbcDet);
        gbcDet.gridx = 1; gbcDet.gridwidth = 3; gbcDet.weightx = 1.0;
        matchValueField = new JTextField();
        responseDetailsPanel.add(matchValueField, gbcDet);

        gbcDet.gridx = 0; gbcDet.gridy = 3; gbcDet.gridwidth = 4; gbcDet.weightx = 0.0;
        responseDetailsPanel.add(new JLabel("Response Body:"), gbcDet);

        gbcDet.gridx = 0; gbcDet.gridy = 4; gbcDet.gridwidth = 4; gbcDet.weighty = 1.0; gbcDet.fill = GridBagConstraints.BOTH;
        mockBodyArea = new JTextArea(5, 20);
        mockBodyArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        responseDetailsPanel.add(new JScrollPane(mockBodyArea), gbcDet);

        responseSplit.setRightComponent(responseDetailsPanel);

        editorPanel.add(responseSplit, BorderLayout.CENTER);

        // Rule Save/Delete/New buttons
        JPanel ruleButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton newRuleBtn = new JButton("New Rule");
        newRuleBtn.addActionListener(e -> clearRuleFormForNew());
        JButton saveRuleBtn = new JButton("Save/Update Rule");
        saveRuleBtn.addActionListener(e -> saveRule());
        JButton deleteRuleBtn = new JButton("Delete Rule");
        deleteRuleBtn.addActionListener(e -> deleteRule());
        ruleButtons.add(newRuleBtn);
        ruleButtons.add(saveRuleBtn);
        ruleButtons.add(deleteRuleBtn);
        editorPanel.add(ruleButtons, BorderLayout.SOUTH);

        leftPanel.add(editorPanel, BorderLayout.SOUTH);
        mainSplit.setLeftComponent(leftPanel);

        // Right Component: Live logs
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(UIManager.getColor("Panel.background"));

        serverLogArea = new JTextArea();
        serverLogArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        serverLogArea.setEditable(false);
        serverLogArea.setBackground(UIManager.getColor("Workspace.panelBackground"));
        JScrollPane logScroll = new JScrollPane(serverLogArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Mock Server Live Traffic Log"));
        rightPanel.add(logScroll, BorderLayout.CENTER);

        JButton clearLogBtn = new JButton("Clear Traffic Log");
        clearLogBtn.addActionListener(e -> serverLogArea.setText(""));
        rightPanel.add(clearLogBtn, BorderLayout.SOUTH);

        mainSplit.setRightComponent(rightPanel);

        add(mainSplit, BorderLayout.CENTER);

        // Set up Listeners
        rulesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selected = rulesTable.getSelectedRow();
                if (selected >= 0 && selected < rulesList.size()) {
                    saveActiveResponseFromUI();
                    activeRule = rulesList.get(selected);

                    methodCombo.setSelectedItem(activeRule.method);
                    pathField.setText(activeRule.path);
                    responseModeCombo.setSelectedItem(activeRule.responseMode);

                    refreshResponsesTable();

                    if (!activeRule.responses.isEmpty()) {
                        responsesTable.setRowSelectionInterval(0, 0);
                    } else {
                        activeResponse = null;
                        loadActiveResponseToUI();
                    }
                }
            }
        });

        responsesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selected = responsesTable.getSelectedRow();
                if (selected >= 0 && activeRule != null && selected < activeRule.responses.size()) {
                    saveActiveResponseFromUI();
                    activeResponse = activeRule.responses.get(selected);
                    loadActiveResponseToUI();
                }
            }
        });

        loadFromModel();

        if (rulesList.isEmpty()) {
            rulesList.add(new MockRule("GET", "/api/users", 200, "application/json", "[\n  {\"id\": 1, \"name\": \"Alice\"},\n  {\"id\": 2, \"name\": \"Bob\"}\n]"));
            rulesList.add(new MockRule("POST", "/api/users", 201, "application/json", "{\n  \"status\": \"created\",\n  \"id\": 3\n}"));
            refreshRulesTable();
        }
    }

    private void toggleServer() {
        if (isRunning) {
            stopServer();
        } else {
            startServer();
        }
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid port number", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            server.createContext("/", new MockHandler());
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();

            isRunning = true;
            startStopBtn.setText("Stop Server");
            startStopBtn.setBackground(new Color(231, 76, 60));
            statusLabel.setText("Status: Running on http://localhost:" + port);
            statusLabel.setForeground(new Color(39, 174, 96));
            logTraffic("Server started on http://localhost:" + port);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not start server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (server != null) {
            try {
                server.stop(0);
            } catch (Exception ignored) {}
        }
        isRunning = false;
        startStopBtn.setText("Start Server");
        startStopBtn.setBackground(new Color(46, 204, 113));
        statusLabel.setText("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        logTraffic("Server stopped");
    }

    public void stopServerIfRunning() {
        if (isRunning) {
            stopServer();
        }
    }

    private void logTraffic(String msg) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        SwingUtilities.invokeLater(() -> {
            serverLogArea.append(String.format("[%s] %s%n", timestamp, msg));
            serverLogArea.setCaretPosition(serverLogArea.getDocument().getLength());
        });
    }

    private void clearRuleFormForNew() {
        activeRule = null;
        activeResponse = null;
        methodCombo.setSelectedIndex(0);
        pathField.setText("/new-endpoint");
        responseModeCombo.setSelectedIndex(0);
        responsesModel.setRowCount(0);
        loadActiveResponseToUI();
        pathField.requestFocus();
    }

    private void saveActiveResponseFromUI() {
        if (activeResponse != null) {
            try {
                activeResponse.responseStatus = Integer.parseInt(statusField.getText().trim());
            } catch (Exception ignored) {}
            activeResponse.contentType = (String) contentTypeCombo.getSelectedItem();
            activeResponse.body = mockBodyArea.getText();
            activeResponse.matchType = (String) matchTypeCombo.getSelectedItem();
            activeResponse.matchKey = matchKeyField.getText().trim();
            activeResponse.matchValue = matchValueField.getText().trim();
        }
    }

    private void loadActiveResponseToUI() {
        if (activeResponse != null) {
            statusField.setText(String.valueOf(activeResponse.responseStatus));
            contentTypeCombo.setSelectedItem(activeResponse.contentType);
            matchTypeCombo.setSelectedItem(activeResponse.matchType);
            matchKeyField.setText(activeResponse.matchKey);
            matchValueField.setText(activeResponse.matchValue);
            mockBodyArea.setText(activeResponse.body);

            statusField.setEnabled(true);
            contentTypeCombo.setEnabled(true);
            matchTypeCombo.setEnabled(true);
            matchKeyField.setEnabled(true);
            matchValueField.setEnabled(true);
            mockBodyArea.setEnabled(true);
        } else {
            statusField.setText("");
            contentTypeCombo.setSelectedIndex(0);
            matchTypeCombo.setSelectedIndex(0);
            matchKeyField.setText("");
            matchValueField.setText("");
            mockBodyArea.setText("");

            statusField.setEnabled(false);
            contentTypeCombo.setEnabled(false);
            matchTypeCombo.setEnabled(false);
            matchKeyField.setEnabled(false);
            matchValueField.setEnabled(false);
            mockBodyArea.setEnabled(false);
        }
    }

    private void addResponse() {
        if (activeRule == null) {
            JOptionPane.showMessageDialog(this, "Please select or create a rule first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        saveActiveResponseFromUI();
        MockResponse resp = new MockResponse();
        activeRule.responses.add(resp);
        refreshResponsesTable();
        responsesTable.setRowSelectionInterval(activeRule.responses.size() - 1, activeRule.responses.size() - 1);
    }

    private void deleteResponse() {
        if (activeRule == null || activeResponse == null) return;
        int row = responsesTable.getSelectedRow();
        if (row >= 0 && row < activeRule.responses.size()) {
            activeRule.responses.remove(row);
            activeResponse = null;
            refreshResponsesTable();
            if (!activeRule.responses.isEmpty()) {
                responsesTable.setRowSelectionInterval(0, 0);
            } else {
                loadActiveResponseToUI();
            }
        }
    }

    private void saveRule() {
        String method = (String) methodCombo.getSelectedItem();
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Path cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        saveActiveResponseFromUI();

        if (activeRule == null) {
            activeRule = new MockRule();
            activeRule.method = method;
            activeRule.path = path.startsWith("/") ? path : "/" + path;
            activeRule.responseMode = (String) responseModeCombo.getSelectedItem();
            if (activeRule.responses.isEmpty()) {
                MockResponse resp = new MockResponse();
                activeRule.responses.add(resp);
            }
            rulesList.add(activeRule);
        } else {
            activeRule.method = method;
            activeRule.path = path.startsWith("/") ? path : "/" + path;
            activeRule.responseMode = (String) responseModeCombo.getSelectedItem();
        }

        refreshRulesTable();
        updateModel();
        logTraffic("Saved rule: " + method + " " + path);
    }

    private void deleteRule() {
        int selected = rulesTable.getSelectedRow();
        if (selected >= 0) {
            rulesList.remove(selected);
            activeRule = null;
            activeResponse = null;
            refreshRulesTable();
            refreshResponsesTable();
            loadActiveResponseToUI();
            updateModel();
            logTraffic("Deleted selected rule.");
        }
    }

    private void refreshRulesTable() {
        rulesModel.setRowCount(0);
        for (MockRule r : rulesList) {
            rulesModel.addRow(new Object[]{r.method, r.path, r.responseMode, r.responses.size()});
        }
    }

    private void refreshResponsesTable() {
        responsesModel.setRowCount(0);
        if (activeRule != null) {
            int idx = 1;
            for (MockResponse r : activeRule.responses) {
                String crit = "None";
                if (!"none".equals(r.matchType)) {
                    crit = r.matchType + " (" + r.matchKey + "=" + r.matchValue + ")";
                }
                responsesModel.addRow(new Object[]{idx++, r.responseStatus, r.contentType, crit});
            }
        }
    }

    public RequestModel getRequestModel() {
        updateModel();
        return model;
    }

    public void updateModel() {
        MockServerConfig config = new MockServerConfig();
        try {
            config.port = Integer.parseInt(portField.getText().trim());
        } catch (Exception ignored) {}
        config.rules = this.rulesList;
        String json = new com.google.gson.Gson().toJson(config);
        model.setBodyRawContent(json);
    }

    public void loadFromModel() {
        if (model.getBodyRawContent() != null && !model.getBodyRawContent().isBlank()) {
            try {
                MockServerConfig config = new com.google.gson.Gson().fromJson(model.getBodyRawContent(), MockServerConfig.class);
                if (config != null) {
                    this.portField.setText(String.valueOf(config.port));
                    this.rulesList.clear();
                    if (config.rules != null) {
                        this.rulesList.addAll(config.rules);
                    }
                    refreshRulesTable();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
    }

    private String extractPath(String url) {
        if (url == null || url.isBlank()) return "/";
        url = url.replaceAll("\\{\\{[^}]+\\}\\}", "");
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                java.net.URL u = java.net.URI.create(url).toURL();
                return u.getPath().isEmpty() ? "/" : u.getPath();
            } catch (Exception e) {
                url = url.replaceFirst("^https?://[^/]+", "");
            }
        }
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        int queryIdx = url.indexOf('?');
        if (queryIdx != -1) {
            url = url.substring(0, queryIdx);
        }
        return url;
    }

    private void importApisFromCollection() {
        List<CollectionModel> cols = mainFrame.getCollections();
        if (cols.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No collections available.", "Import APIs", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(mainFrame, "Import APIs from Collection", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Select Collection:"));
        JComboBox<CollectionModel> colCombo = new JComboBox<>(cols.toArray(new CollectionModel[0]));
        top.add(colCombo);
        dialog.add(top, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Import", "Name", "Method", "Path"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        dialog.add(scroll, BorderLayout.CENTER);

        Runnable fillTable = () -> {
            model.setRowCount(0);
            CollectionModel selectedCol = (CollectionModel) colCombo.getSelectedItem();
            if (selectedCol != null) {
                for (RequestModel req : selectedCol.getRequests()) {
                    if ("request".equals(req.getType())) {
                        model.addRow(new Object[]{true, req.getName(), req.getMethod(), extractPath(req.getUrl())});
                    }
                }
            }
        };

        colCombo.addActionListener(e -> fillTable.run());
        fillTable.run();

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectAll = new JButton("Select All");
        selectAll.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(true, i, 0);
            }
        });
        JButton selectNone = new JButton("Deselect All");
        selectNone.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++) {
                model.setValueAt(false, i, 0);
            }
        });

        JButton importBtn = new JButton("Import Selected");
        importBtn.addActionListener(e -> {
            int imported = 0;
            CollectionModel selectedCol = (CollectionModel) colCombo.getSelectedItem();
            if (selectedCol != null) {
                for (int i = 0; i < model.getRowCount(); i++) {
                    Boolean check = (Boolean) model.getValueAt(i, 0);
                    if (check != null && check) {
                        String name = (String) model.getValueAt(i, 1);
                        String method = (String) model.getValueAt(i, 2);
                        String path = (String) model.getValueAt(i, 3);

                        // Find source body if any
                        String sourceBody = "";
                        for (RequestModel req : selectedCol.getRequests()) {
                            if (req.getName().equals(name) && req.getMethod().equals(method)) {
                                sourceBody = req.getBodyRawContent();
                                break;
                            }
                        }

                        MockRule rule = new MockRule();
                        rule.method = method;
                        rule.path = path;
                        rule.responseMode = "fixed";

                        MockResponse resp = new MockResponse();
                        resp.responseStatus = 200;
                        resp.contentType = "application/json";
                        resp.body = (sourceBody != null && !sourceBody.isEmpty()) ? sourceBody : "{\n  \"message\": \"Imported mock endpoint\"\n}";
                        rule.responses.add(resp);

                        rulesList.removeIf(r -> r.method.equalsIgnoreCase(rule.method) && r.path.equals(rule.path));
                        rulesList.add(rule);
                        imported++;
                    }
                }
            }
            refreshRulesTable();
            updateModel();
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "Imported " + imported + " APIs to Mock Server!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        bottom.add(selectAll);
        bottom.add(selectNone);
        bottom.add(importBtn);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void importOpenApiSpec() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select OpenAPI Spec File (JSON or YAML)");
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String content = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
                int count = 0;
                if (file.getName().endsWith(".json")) {
                    count = parseOpenApiJson(content);
                } else {
                    count = parseOpenApiYaml(content);
                }
                refreshRulesTable();
                updateModel();
                JOptionPane.showMessageDialog(this, "Successfully imported " + count + " endpoints from OpenAPI spec!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error parsing OpenAPI Spec: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private int parseOpenApiJson(String content) {
        int count = 0;
        try {
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
            com.google.gson.JsonObject paths = root.getAsJsonObject("paths");
            if (paths != null) {
                for (Map.Entry<String, com.google.gson.JsonElement> pathEntry : paths.entrySet()) {
                    String path = pathEntry.getKey();
                    com.google.gson.JsonObject pathItem = pathEntry.getValue().getAsJsonObject();
                    for (Map.Entry<String, com.google.gson.JsonElement> methodEntry : pathItem.entrySet()) {
                        String method = methodEntry.getKey().toUpperCase();
                        if (method.equals("GET") || method.equals("POST") || method.equals("PUT") || method.equals("DELETE") || method.equals("PATCH")) {
                            MockRule rule = new MockRule();
                            rule.method = method;
                            rule.path = path.startsWith("/") ? path : "/" + path;
                            rule.responseMode = "fixed";

                            MockResponse resp = new MockResponse();
                            resp.responseStatus = 200;
                            resp.contentType = "application/json";
                            resp.body = "{\n  \"message\": \"Imported from OpenAPI JSON\"\n}";

                            try {
                                com.google.gson.JsonObject methodObj = methodEntry.getValue().getAsJsonObject();
                                com.google.gson.JsonObject responsesObj = methodObj.getAsJsonObject("responses");
                                if (responsesObj != null) {
                                    rule.responses.clear();
                                    for (Map.Entry<String, com.google.gson.JsonElement> respEntry : responsesObj.entrySet()) {
                                        String statusStr = respEntry.getKey();
                                        int status = 200;
                                        try { status = Integer.parseInt(statusStr); } catch (Exception ignored) {}

                                        MockResponse openApiResp = new MockResponse();
                                        openApiResp.responseStatus = status;
                                        openApiResp.contentType = "application/json";
                                        openApiResp.body = "{\n  \"status\": " + status + "\n}";
                                        rule.responses.add(openApiResp);
                                    }
                                }
                            } catch (Exception ignored) {}

                            if (rule.responses.isEmpty()) {
                                rule.responses.add(resp);
                            }

                            rulesList.removeIf(r -> r.method.equalsIgnoreCase(rule.method) && r.path.equals(rule.path));
                            rulesList.add(rule);
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private int parseOpenApiYaml(String content) {
        int count = 0;
        String currentPath = null;
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;

            if (trimmed.startsWith("/") && trimmed.endsWith(":")) {
                currentPath = trimmed.substring(0, trimmed.length() - 1).trim();
            } else if (currentPath != null && (trimmed.startsWith("get:") || trimmed.startsWith("post:") || trimmed.startsWith("put:") || trimmed.startsWith("delete:") || trimmed.startsWith("patch:"))) {
                String method = trimmed.substring(0, trimmed.indexOf(":")).toUpperCase().trim();
                MockRule rule = new MockRule();
                rule.method = method;
                rule.path = currentPath;
                rule.responseMode = "fixed";

                MockResponse resp = new MockResponse();
                resp.responseStatus = 200;
                resp.contentType = "application/json";
                resp.body = "{\n  \"message\": \"Imported from OpenAPI YAML\"\n}";
                rule.responses.add(resp);

                rulesList.removeIf(r -> r.method.equalsIgnoreCase(rule.method) && r.path.equals(rule.path));
                rulesList.add(rule);
                count++;
            }
        }
        return count;
    }

    private class MockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            String requestPath = exchange.getRequestURI().getPath();

            logTraffic("Incoming request: " + requestMethod + " " + requestPath);

            Map<String, String> queryParams = new HashMap<>();
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length > 0) {
                        String key = pair[0];
                        String val = pair.length > 1 ? pair[1] : "";
                        queryParams.put(key, val);
                    }
                }
            }

            Map<String, String> headers = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    headers.put(entry.getKey().toLowerCase(), entry.getValue().get(0));
                }
            }

            String requestBody = "";
            try (java.io.InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {}

            MockRule matchedRule = null;
            for (MockRule rule : rulesList) {
                if (rule.method.equalsIgnoreCase(requestMethod) && rule.path.equals(requestPath)) {
                    matchedRule = rule;
                    break;
                }
            }

            if (matchedRule == null) {
                for (MockRule rule : rulesList) {
                    if (rule.path.equals(requestPath)) {
                        matchedRule = rule;
                        break;
                    }
                }
            }

            if (matchedRule != null) {
                MockResponse matchedResponse = null;

                // 1. Try to match by input criteria
                for (MockResponse resp : matchedRule.responses) {
                    if ("query".equalsIgnoreCase(resp.matchType) && !resp.matchKey.isEmpty()) {
                        String val = queryParams.get(resp.matchKey);
                        if (val != null && val.contains(resp.matchValue)) {
                            matchedResponse = resp;
                            break;
                        }
                    } else if ("header".equalsIgnoreCase(resp.matchType) && !resp.matchKey.isEmpty()) {
                        String val = headers.get(resp.matchKey.toLowerCase());
                        if (val != null && val.contains(resp.matchValue)) {
                            matchedResponse = resp;
                            break;
                        }
                    } else if ("body".equalsIgnoreCase(resp.matchType) && !resp.matchValue.isEmpty()) {
                        if (requestBody.contains(resp.matchValue)) {
                            matchedResponse = resp;
                            break;
                        }
                    }
                }

                // 2. Fallback to responseMode if no criteria matched
                if (matchedResponse == null && !matchedRule.responses.isEmpty()) {
                    if ("sequence".equalsIgnoreCase(matchedRule.responseMode)) {
                        int idx = matchedRule.sequenceIndex % matchedRule.responses.size();
                        matchedResponse = matchedRule.responses.get(idx);
                        matchedRule.sequenceIndex++;
                    } else if ("random".equalsIgnoreCase(matchedRule.responseMode)) {
                        int idx = new Random().nextInt(matchedRule.responses.size());
                        matchedResponse = matchedRule.responses.get(idx);
                    } else {
                        matchedResponse = matchedRule.responses.get(0);
                    }
                }

                if (matchedResponse != null) {
                    byte[] responseBytes = matchedResponse.body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", matchedResponse.contentType + "; charset=utf-8");
                    exchange.sendResponseHeaders(matchedResponse.responseStatus, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                    logTraffic(String.format("Matched Rule: %s %s -> Sending %d (%s)",
                            matchedRule.method, matchedRule.path, matchedResponse.responseStatus, matchedResponse.contentType));
                } else {
                    send404(exchange, requestPath);
                }
            } else {
                send404(exchange, requestPath);
            }
        }

        private void send404(HttpExchange exchange, String requestPath) throws IOException {
            String errorBody = "{\"error\": \"Mock rule not found for path: " + requestPath + "\"}";
            byte[] responseBytes = errorBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
            logTraffic("No matching rule found. Sent 404 Not Found");
        }
    }
}
