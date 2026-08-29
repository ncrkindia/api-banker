package in.slpro.apibanker.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.RequestModel;

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

/**
 * MockServerPanel
 *
 * <p>
 * Core functionality and implementation logic for MockServerPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.1
 * @since 1.0.0
 */
public class MockServerPanel extends JPanel {
    private final MainFrame mainFrame;
    private final RequestModel model;

    private HttpServer server;
    private boolean isRunning = false;
    private java.time.LocalDateTime startTime;

    private final JTextField portField;
    private final JButton startStopBtn;
    private final JLabel statusLabel;
    private final JButton saveConfigBtn;

    private final JTable rulesTable;
    private final DefaultTableModel rulesModel;

    // Rule Editor Components
    private final JComboBox<String> methodCombo;
    private final JTextField pathField;
    private final JComboBox<String> responseModeCombo;
    private final JTextField delayField;

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
    private final DefaultTableModel headersModel;
    private final JTable headersTable;
    private boolean isUpdatingHeaders = false;

    private final JTextArea serverLogArea;

    public static class MockResponse {
        public int responseStatus = 200;
        public String contentType = "application/json";
        public String body = "{\n  \"status\": \"success\"\n}";
        public String matchType = "none"; // none, query, header, body
        public String matchKey = "";
        public String matchValue = "";
        public List<String[]> headers = new ArrayList<>(); // e.g. ["true", "X-Custom", "value"]
    }

    public static class MockRule {
        public String method = "GET";
        public String path = "/";
        public String responseMode = "fixed"; // fixed, sequence, random
        public int delayMs = 0; // Delay in milliseconds
        public List<MockResponse> responses = new ArrayList<>();
        public transient int sequenceIndex = 0;

        public MockRule() {
        }

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
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(UIManager.getColor("Panel.background"));

        JPanel leftTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftTopPanel.setBackground(UIManager.getColor("Panel.background"));

        leftTopPanel.add(new JLabel("Port:"));
        portField = new JTextField("8085", 6);
        leftTopPanel.add(portField);

        startStopBtn = new AnimatedGradientButton("Start Server");
        startStopBtn.setBackground(new Color(46, 204, 113));
        startStopBtn.setForeground(Color.WHITE);
        startStopBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        startStopBtn.addActionListener(e -> toggleServer());
        leftTopPanel.add(startStopBtn);
        
        leftTopPanel.add(mainFrame.createInfoBadge("sec-mock-server", "View Mock Server Guide"));

        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        leftTopPanel.add(statusLabel);

        // Add Import Actions
        JButton importApisBtn = new JButton("Import APIs");
        importApisBtn.addActionListener(e -> importApisFromCollection());
        leftTopPanel.add(importApisBtn);

        JButton importOpenApiBtn = new JButton("Import OpenAPI Spec");
        importOpenApiBtn.addActionListener(e -> importOpenApiSpec());
        leftTopPanel.add(importOpenApiBtn);

        topPanel.add(leftTopPanel, BorderLayout.WEST);

        // Add Save Mock Config
        JPanel rightTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightTopPanel.setBackground(UIManager.getColor("Panel.background"));
        saveConfigBtn = new JButton("Save");
        Color accent = UIManager.getColor("AccentColor");
        saveConfigBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        saveConfigBtn.setForeground(Color.WHITE);
        saveConfigBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveConfigBtn.setPreferredSize(new Dimension(110, 28));
        saveConfigBtn.addActionListener(e -> {
            updateModel();
            mainFrame.saveCollections();
            MainFrame.showToast(this, "Mock Server configuration saved!");
        });
        rightTopPanel.add(saveConfigBtn);

        topPanel.add(rightTopPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- CENTER: Rules Management & Log ---
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.65);

        // Left Component: Rules Editor & Table
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBackground(UIManager.getColor("Panel.background"));

        rulesModel = new DefaultTableModel(new String[] { "Method", "Path", "Mode", "ResponsesCount" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rulesTable = new JTable(rulesModel);
        rulesTable.setRowHeight(24);
        rulesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rulesTable.getColumnModel().getColumn(0).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof String method && !isSelected) {
                    c.setForeground(getMethodColor(method));
                    c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                }
                return c;
            }
        });
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

        gbc.gridx = 0;
        gbc.gridy = 0;
        generalSettings.add(new JLabel("Method:"), gbc);
        gbc.gridx = 1;
        methodCombo = new JComboBox<>(new String[] { "GET", "POST", "PUT", "DELETE", "PATCH" });
        methodCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String method) {
                    if (!isSelected || index == -1) {
                        c.setForeground(getMethodColor(method));
                    }
                    c.setFont(new Font("Segoe UI", Font.BOLD, 12));
                }
                return c;
            }
        });
        methodCombo.addActionListener(e -> {
            String method = (String) methodCombo.getSelectedItem();
            methodCombo.setForeground(getMethodColor(method));
        });
        methodCombo.setForeground(getMethodColor((String) methodCombo.getSelectedItem()));
        generalSettings.add(methodCombo, gbc);

        gbc.gridx = 2;
        generalSettings.add(new JLabel("Path:"), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        pathField = new JTextField("/api/users", 15);
        generalSettings.add(pathField, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.0;
        generalSettings.add(new JLabel("Response Mode:"), gbc);
        gbc.gridx = 5;
        responseModeCombo = new JComboBox<>(new String[] { "fixed", "sequence", "random" });
        generalSettings.add(responseModeCombo, gbc);

        gbc.gridx = 6;
        gbc.weightx = 0.0;
        generalSettings.add(new JLabel("Delay (ms):"), gbc);
        gbc.gridx = 7;
        gbc.weightx = 0.5;
        delayField = new JTextField("0", 6);
        generalSettings.add(delayField, gbc);

        editorPanel.add(generalSettings, BorderLayout.NORTH);

        // Split responses and details
        JSplitPane responseSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        responseSplit.setResizeWeight(0.4);

        // Response List Panel
        JPanel responseListPanel = new JPanel(new BorderLayout(5, 5));
        responsesModel = new DefaultTableModel(new String[] { "#", "Status", "Content-Type", "Criteria" }, 0) {
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

        gbcDet.gridx = 0;
        gbcDet.gridy = 0;
        responseDetailsPanel.add(new JLabel("Status:"), gbcDet);
        gbcDet.gridx = 1;
        statusField = new JTextField("200", 6);
        responseDetailsPanel.add(statusField, gbcDet);

        gbcDet.gridx = 2;
        responseDetailsPanel.add(new JLabel("Type:"), gbcDet);
        gbcDet.gridx = 3;
        gbcDet.weightx = 1.0;
        contentTypeCombo = new JComboBox<>(
                new String[] { "application/json", "application/xml", "text/plain", "text/html" });
        responseDetailsPanel.add(contentTypeCombo, gbcDet);

        // Criteria Matching
        gbcDet.gridx = 0;
        gbcDet.gridy = 1;
        gbcDet.weightx = 0.0;
        responseDetailsPanel.add(new JLabel("Match:"), gbcDet);
        gbcDet.gridx = 1;
        matchTypeCombo = new JComboBox<>(new String[] { "none", "query", "header", "body" });
        responseDetailsPanel.add(matchTypeCombo, gbcDet);

        gbcDet.gridx = 2;
        responseDetailsPanel.add(new JLabel("Key:"), gbcDet);
        gbcDet.gridx = 3;
        gbcDet.weightx = 1.0;
        matchKeyField = new JTextField();
        responseDetailsPanel.add(matchKeyField, gbcDet);

        gbcDet.gridx = 0;
        gbcDet.gridy = 2;
        gbcDet.weightx = 0.0;
        responseDetailsPanel.add(new JLabel("Value:"), gbcDet);
        gbcDet.gridx = 1;
        gbcDet.gridwidth = 3;
        gbcDet.weightx = 1.0;
        matchValueField = new JTextField();
        responseDetailsPanel.add(matchValueField, gbcDet);

        gbcDet.gridx = 0;
        gbcDet.gridy = 3;
        gbcDet.gridwidth = 4;
        gbcDet.weighty = 1.0;
        gbcDet.fill = GridBagConstraints.BOTH;

        JTabbedPane respTabs = new JTabbedPane();

        mockBodyArea = new JTextArea(5, 20);
        mockBodyArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        respTabs.addTab("Body", new JScrollPane(mockBodyArea));

        headersModel = new DefaultTableModel(new Object[] { "Active", "Key", "Value" }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                String key = (String) getValueAt(row, 1);
                if (key != null) {
                    if (key.equalsIgnoreCase("Content-Type") || key.equalsIgnoreCase("Date")
                            || key.equalsIgnoreCase("Connection")) {
                        return column == 0;
                    }
                    if (key.equalsIgnoreCase("Server")) {
                        return column != 1;
                    }
                }
                return true;
            }
        };
        headersModel.addTableModelListener(e -> {
            if (isUpdatingHeaders)
                return;
            boolean hasEmpty = false;
            for (int i = 0; i < headersModel.getRowCount(); i++) {
                String k = (String) headersModel.getValueAt(i, 1);
                String v = (String) headersModel.getValueAt(i, 2);
                if ((k == null || k.trim().isEmpty()) && (v == null || v.trim().isEmpty())) {
                    hasEmpty = true;
                    break;
                }
            }
            if (!hasEmpty) {
                SwingUtilities.invokeLater(() -> {
                    if (isUpdatingHeaders)
                        return;
                    boolean stillHasEmpty = false;
                    for (int i = 0; i < headersModel.getRowCount(); i++) {
                        String k = (String) headersModel.getValueAt(i, 1);
                        String v = (String) headersModel.getValueAt(i, 2);
                        if ((k == null || k.trim().isEmpty()) && (v == null || v.trim().isEmpty())) {
                            stillHasEmpty = true;
                            break;
                        }
                    }
                    if (!stillHasEmpty) {
                        isUpdatingHeaders = true;
                        headersModel.addRow(new Object[] { true, "", "" });
                        isUpdatingHeaders = false;
                    }
                });
            }
        });

        isUpdatingHeaders = true;
        headersModel.addRow(new Object[] { true, "", "" });
        isUpdatingHeaders = false;

        headersTable = new JTable(headersModel);
        headersTable.setRowHeight(24);

        headersTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String key = (String) table.getModel().getValueAt(table.convertRowIndexToModel(row), 1);
                if (key != null && (key.equalsIgnoreCase("Content-Type") || key.equalsIgnoreCase("Date")
                        || key.equalsIgnoreCase("Connection") || key.equalsIgnoreCase("Server"))) {
                    if (!isSelected) {
                        c.setBackground(new Color(245, 245, 250));
                        c.setForeground(Color.GRAY);
                    }
                } else {
                    if (!isSelected) {
                        c.setBackground(table.getBackground());
                        c.setForeground(table.getForeground());
                    }
                }
                return c;
            }
        });
        headersTable.setDefaultRenderer(Boolean.class, headersTable.getDefaultRenderer(Boolean.class));
        headersTable.putClientProperty("terminateEditOnFocusLost", true);
        GlobalVariablesPanel.setupTableCopyPaste(headersTable, headersModel);

        contentTypeCombo.addActionListener(e -> {
            String newType = (String) contentTypeCombo.getSelectedItem();
            for (int i = 0; i < headersModel.getRowCount(); i++) {
                String k = (String) headersModel.getValueAt(i, 1);
                if ("Content-Type".equalsIgnoreCase(k)) {
                    headersModel.setValueAt(newType, i, 2);
                    break;
                }
            }
        });

        respTabs.addTab("Headers", new JScrollPane(headersTable));

        responseDetailsPanel.add(respTabs, gbcDet);

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

        JButton downloadLogBtn = new JButton("Download Logs");
        downloadLogBtn.addActionListener(e -> downloadLogs());

        JPanel bottomBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        bottomBtnPanel.setBackground(UIManager.getColor("Panel.background"));
        bottomBtnPanel.add(clearLogBtn);
        bottomBtnPanel.add(downloadLogBtn);

        rightPanel.add(bottomBtnPanel, BorderLayout.SOUTH);

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
                    delayField.setText(String.valueOf(activeRule.delayMs));

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
            rulesList.add(new MockRule("GET", "/api/users", 200, "application/json",
                    "[\n  {\"id\": 1, \"name\": \"Alice\"},\n  {\"id\": 2, \"name\": \"Bob\"}\n]"));
            rulesList.add(new MockRule("POST", "/api/users", 201, "application/json",
                    "{\n  \"status\": \"created\",\n  \"id\": 3\n}"));
            refreshRulesTable();
        }

        setupTableActions(rulesTable, "rule");
        setupTableActions(responsesTable, "response");
    }

    private Color getMethodColor(String method) {
        if (method == null)
            return UIManager.getColor("Label.foreground");
        return switch (method.toUpperCase()) {
            case "GET" -> new Color(39, 174, 96);
            case "POST" -> new Color(52, 152, 219);
            case "PUT" -> new Color(230, 126, 34);
            case "PATCH" -> new Color(155, 89, 182);
            case "DELETE" -> new Color(192, 57, 43);
            default -> UIManager.getColor("Label.foreground");
        };
    }

    private void setupTableActions(JTable table, String type) {
        Action copyAction = new AbstractAction("Copy") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int[] selected = table.getSelectedRows();
                if (selected.length == 0)
                    return;
                com.google.gson.Gson gson = new com.google.gson.Gson();
                String data = "";
                if ("rule".equals(type)) {
                    List<MockRule> copyRules = new ArrayList<>();
                    for (int row : selected) {
                        copyRules.add(rulesList.get(row));
                    }
                    data = gson.toJson(copyRules);
                } else {
                    if (activeRule == null)
                        return;
                    List<MockResponse> copyResp = new ArrayList<>();
                    for (int row : selected) {
                        copyResp.add(activeRule.responses.get(row));
                    }
                    data = gson.toJson(copyResp);
                }
                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(data);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            }
        };

        Action pasteAction = new AbstractAction("Paste") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                            .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                    if (data == null || data.isBlank())
                        return;
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    if ("rule".equals(type)) {
                        MockRule[] pastedRules = gson.fromJson(data, MockRule[].class);
                        if (pastedRules != null && pastedRules.length > 0) {
                            rulesList.addAll(Arrays.asList(pastedRules));
                            refreshRulesTable();
                            updateModel();
                        }
                    } else {
                        if (activeRule == null)
                            return;
                        MockResponse[] pastedResp = gson.fromJson(data, MockResponse[].class);
                        if (pastedResp != null && pastedResp.length > 0) {
                            activeRule.responses.addAll(Arrays.asList(pastedResp));
                            refreshResponsesTable();
                        }
                    }
                } catch (Exception ex) {
                    // ignore invalid clipboard
                }
            }
        };

        Action deleteAction = new AbstractAction("Delete") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if ("rule".equals(type))
                    deleteRule();
                else
                    deleteResponse();
            }
        };

        table.getActionMap().put("copy", copyAction);
        table.getActionMap().put("paste", pasteAction);

        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0),
                "delete");
        table.getActionMap().put("delete", deleteAction);

        JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(copyAction));
        popup.add(new JMenuItem(pasteAction));
        popup.add(new JMenuItem(deleteAction));

        if ("rule".equals(type)) {
            popup.addSeparator();
            JMenu codeMenu = new JMenu("Code");

            JMenuItem curlItem = new JMenuItem("Curl");
            curlItem.addActionListener(e -> copyCodeForRules(table, "Curl"));
            codeMenu.add(curlItem);

            JMenuItem pythonItem = new JMenuItem("Python");
            pythonItem.addActionListener(e -> copyCodeForRules(table, "Python"));
            codeMenu.add(pythonItem);

            JMenuItem jsItem = new JMenuItem("JavaScript");
            jsItem.addActionListener(e -> copyCodeForRules(table, "JavaScript"));
            codeMenu.add(jsItem);

            JMenuItem javaItem = new JMenuItem("Java");
            javaItem.addActionListener(e -> copyCodeForRules(table, "Java"));
            codeMenu.add(javaItem);

            popup.add(codeMenu);
        }

        table.setComponentPopupMenu(popup);
    }

    private void copyCodeForRules(JTable table, String language) {
        int[] selected = table.getSelectedRows();
        if (selected.length == 0)
            return;

        String port = portField.getText().trim();
        if (port.isEmpty())
            port = "8085";
        String baseUrl = "http://localhost:" + port;

        StringBuilder sb = new StringBuilder();
        for (int row : selected) {
            MockRule rule = rulesList.get(row);
            String url = baseUrl + rule.path;
            String method = rule.method.toUpperCase();

            if (sb.length() > 0)
                sb.append("\n\n");

            if ("Curl".equals(language)) {
                sb.append(String.format("curl -X %s \"%s\"", method, url));
            } else if ("Python".equals(language)) {
                sb.append("import requests\n");
                sb.append(String.format("response = requests.request(\"%s\", \"%s\")\n", method, url));
                sb.append("print(response.text)");
            } else if ("JavaScript".equals(language)) {
                sb.append(String.format("fetch(\"%s\", {\n  method: \"%s\"\n})\n", url, method));
                sb.append(".then(response => response.text())\n");
                sb.append(".then(result => console.log(result))\n");
                sb.append(".catch(error => console.log('error', error));");
            } else if ("Java".equals(language)) {
                sb.append("java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();\n");
                sb.append("java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()\n");
                sb.append(String.format("  .uri(java.net.URI.create(\"%s\"))\n", url));
                if (method.equals("GET") || method.equals("DELETE")) {
                    sb.append(String.format("  .%s()\n", method));
                } else {
                    sb.append(String.format("  .method(\"%s\", java.net.http.HttpRequest.BodyPublishers.noBody())\n",
                            method));
                }
                sb.append("  .build();\n");
                sb.append(
                        "java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());\n");
                sb.append("System.out.println(response.body());");
            }
        }

        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        MainFrame.showToast(this, language + " code copied to clipboard!");
    }

    private void toggleServer() {
        if (isRunning) {
            stopServer();
        } else {
            startServer();
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null)
            return "mockserver";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    private File getLogFile() {
        String logsDir = in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().getLogsDirectory();
        File logsFolder = new File(logsDir);
        if (!logsFolder.exists())
            logsFolder.mkdirs();

        CollectionModel collection = mainFrame.getParentCollection(model);
        String cleanColl = sanitizeFilename(collection != null ? collection.getName() : "others");
        String cleanMock = sanitizeFilename(model.getName());
        String dirName = cleanColl + "-" + cleanMock;
        File runDir = new File(logsFolder, dirName);
        if (!runDir.exists()) {
            runDir.mkdirs();
        }

        LocalDateTime start = startTime != null ? startTime : LocalDateTime.now();
        String fileStr = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS"));
        return new File(runDir, fileStr + ".log");
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
            startTime = LocalDateTime.now();

            // Write start header to file if logging enabled
            if (in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().isEnableLogging()) {
                try {
                    File logFile = getLogFile();
                    CollectionModel parentCol = MainFrame.findParentCollection(model);
                    String collectionName = parentCol != null ? parentCol.getName() : "Unknown";
                    String collectionId = parentCol != null ? parentCol.getId() : "Unknown";
                    try (java.io.PrintWriter pw = new java.io.PrintWriter(
                            new java.io.FileWriter(logFile, StandardCharsets.UTF_8, true))) {
                        pw.println("=================================================");
                        pw.println("Collection Name: " + collectionName);
                        pw.println("Collection ID: " + collectionId);
                        pw.println("Mock Server Name: " + model.getName());
                        pw.println("Mock Server ID: " + model.getId());
                        pw.println("Port: " + port);
                        pw.println("Started: " + startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        pw.println("=================================================");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

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
            JOptionPane.showMessageDialog(this, "Could not start server: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (server != null) {
            try {
                server.stop(0);
            } catch (Exception ignored) {
            }
        }
        isRunning = false;
        startStopBtn.setText("Start Server");
        startStopBtn.setBackground(new Color(46, 204, 113));
        statusLabel.setText("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        logTraffic("Server stopped");

        // Write stop footer to file if logging enabled
        if (in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().isEnableLogging()) {
            try {
                File logFile = getLogFile();
                try (java.io.PrintWriter pw = new java.io.PrintWriter(
                        new java.io.FileWriter(logFile, StandardCharsets.UTF_8, true))) {
                    pw.println("=================================================");
                    pw.println("Mock Server Stopped: "
                            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    pw.println("=================================================");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stopServerIfRunning() {
        if (isRunning) {
            stopServer();
        }
    }

    private void logTraffic(String msg) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String formatted = String.format("[%s] %s", timestamp, msg);
        SwingUtilities.invokeLater(() -> {
            serverLogArea.append(formatted + "\n");
            serverLogArea.setCaretPosition(serverLogArea.getDocument().getLength());
        });

        // Write to log file if logging is enabled
        if (in.slpro.apibanker.storage.StorageManager.getInstance().getSettings().isEnableLogging()) {
            try {
                File logFile = getLogFile();
                try (java.io.PrintWriter pw = new java.io.PrintWriter(
                        new java.io.FileWriter(logFile, StandardCharsets.UTF_8, true))) {
                    pw.println(formatted);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadLogs() {
        if (serverLogArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No logs to download.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("mockserver-" + sanitizeFilename(model.getName()) + ".log"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.FileWriter(chooser.getSelectedFile(), StandardCharsets.UTF_8))) {
                pw.write(serverLogArea.getText());
                MainFrame.showToast(this, "Logs downloaded successfully.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to download logs: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearRuleFormForNew() {
        activeRule = null;
        activeResponse = null;
        methodCombo.setSelectedIndex(0);
        pathField.setText("/new-endpoint");
        responseModeCombo.setSelectedIndex(0);
        delayField.setText("0");
        responsesModel.setRowCount(0);
        loadActiveResponseToUI();
        pathField.requestFocus();
    }

    private void saveActiveResponseFromUI() {
        if (activeResponse != null) {
            try {
                activeResponse.responseStatus = Integer.parseInt(statusField.getText().trim());
            } catch (Exception ignored) {
            }
            activeResponse.contentType = (String) contentTypeCombo.getSelectedItem();
            activeResponse.body = mockBodyArea.getText();
            activeResponse.matchType = (String) matchTypeCombo.getSelectedItem();
            activeResponse.matchKey = matchKeyField.getText().trim();
            activeResponse.matchValue = matchValueField.getText().trim();

            if (headersTable.getCellEditor() != null) {
                headersTable.getCellEditor().stopCellEditing();
            }
            if (activeResponse.headers == null) {
                activeResponse.headers = new ArrayList<>();
            }
            activeResponse.headers.clear();
            for (int i = 0; i < headersModel.getRowCount(); i++) {
                Boolean active = (Boolean) headersModel.getValueAt(i, 0);
                String key = (String) headersModel.getValueAt(i, 1);
                String val = (String) headersModel.getValueAt(i, 2);
                if (key != null && !key.trim().isEmpty()) {
                    key = key.trim();
                    boolean isActive = (active != null && active);
                    if (key.equalsIgnoreCase("Content-Type") && isActive && val != null
                            && val.equals(activeResponse.contentType))
                        continue;
                    if (key.equalsIgnoreCase("Date") && isActive && val != null && val.equals("<auto-generated>"))
                        continue;
                    if (key.equalsIgnoreCase("Connection") && isActive && val != null && val.equals("keep-alive"))
                        continue;
                    if (key.equalsIgnoreCase("Server") && isActive && val != null
                            && val.equals("ApiBanker Mock Server"))
                        continue;

                    activeResponse.headers.add(new String[] { String.valueOf(isActive), key, val == null ? "" : val });
                }
            }
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

            isUpdatingHeaders = true;
            headersModel.setRowCount(0);

            // 1. Insert Defaults at the Top
            headersModel.addRow(new Object[] { true, "Content-Type", activeResponse.contentType });
            headersModel.addRow(new Object[] { true, "Date", "<auto-generated>" });
            headersModel.addRow(new Object[] { true, "Server", "ApiBanker Mock Server" });
            headersModel.addRow(new Object[] { true, "Connection", "keep-alive" });

            // 2. Override with custom or saved headers
            if (activeResponse.headers != null) {
                for (String[] h : activeResponse.headers) {
                    boolean active = h.length > 0 && "true".equalsIgnoreCase(h[0]);
                    String key = h.length > 1 ? h[1] : "";
                    String val = h.length > 2 ? h[2] : "";

                    if (key.equalsIgnoreCase("Content-Type")) {
                        headersModel.setValueAt(active, 0, 0);
                        headersModel.setValueAt(val, 0, 2);
                    } else if (key.equalsIgnoreCase("Date")) {
                        headersModel.setValueAt(active, 1, 0);
                        headersModel.setValueAt(val, 1, 2);
                    } else if (key.equalsIgnoreCase("Server")) {
                        headersModel.setValueAt(active, 2, 0);
                        headersModel.setValueAt(val, 2, 2);
                    } else if (key.equalsIgnoreCase("Connection")) {
                        headersModel.setValueAt(active, 3, 0);
                        headersModel.setValueAt(val, 3, 2);
                    } else {
                        headersModel.addRow(new Object[] { active, key, val });
                    }
                }
            }

            headersModel.addRow(new Object[] { true, "", "" });
            isUpdatingHeaders = false;

            statusField.setEnabled(true);
            contentTypeCombo.setEnabled(true);
            matchTypeCombo.setEnabled(true);
            matchKeyField.setEnabled(true);
            matchValueField.setEnabled(true);
            mockBodyArea.setEnabled(true);
            headersTable.setEnabled(true);
        } else {
            statusField.setText("");
            contentTypeCombo.setSelectedIndex(0);
            matchTypeCombo.setSelectedIndex(0);
            matchKeyField.setText("");
            matchValueField.setText("");
            mockBodyArea.setText("");

            isUpdatingHeaders = true;
            headersModel.setRowCount(0);
            headersModel.addRow(new Object[] { true, "", "" });
            isUpdatingHeaders = false;

            statusField.setEnabled(false);
            contentTypeCombo.setEnabled(false);
            matchTypeCombo.setEnabled(false);
            matchKeyField.setEnabled(false);
            matchValueField.setEnabled(false);
            mockBodyArea.setEnabled(false);
            headersTable.setEnabled(false);
        }
    }

    private void addResponse() {
        if (activeRule == null) {
            JOptionPane.showMessageDialog(this, "Please select or create a rule first.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        saveActiveResponseFromUI();
        MockResponse resp = new MockResponse();
        activeRule.responses.add(resp);
        refreshResponsesTable();
        responsesTable.setRowSelectionInterval(activeRule.responses.size() - 1, activeRule.responses.size() - 1);
    }

    private void deleteResponse() {
        if (activeRule == null)
            return;
        int[] selected = responsesTable.getSelectedRows();
        if (selected.length > 0) {
            Arrays.sort(selected);
            for (int i = selected.length - 1; i >= 0; i--) {
                activeRule.responses.remove(selected[i]);
            }
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
            try {
                activeRule.delayMs = Integer.parseInt(delayField.getText().trim());
            } catch (Exception ignored) {
                activeRule.delayMs = 0;
            }
            if (activeRule.responses.isEmpty()) {
                MockResponse resp = new MockResponse();
                activeRule.responses.add(resp);
            }
            rulesList.add(activeRule);
        } else {
            activeRule.method = method;
            activeRule.path = path.startsWith("/") ? path : "/" + path;
            activeRule.responseMode = (String) responseModeCombo.getSelectedItem();
            try {
                activeRule.delayMs = Integer.parseInt(delayField.getText().trim());
            } catch (Exception ignored) {
                activeRule.delayMs = 0;
            }
        }

        refreshRulesTable();
        updateModel();
        logTraffic("Saved rule: " + method + " " + path);
    }

    private void deleteRule() {
        int[] selected = rulesTable.getSelectedRows();
        if (selected.length > 0) {
            Arrays.sort(selected);
            for (int i = selected.length - 1; i >= 0; i--) {
                rulesList.remove(selected[i]);
            }
            activeRule = null;
            activeResponse = null;
            refreshRulesTable();
            refreshResponsesTable();
            loadActiveResponseToUI();
            updateModel();
            logTraffic("Deleted " + selected.length + " rule(s).");
        }
    }

    private void refreshRulesTable() {
        rulesModel.setRowCount(0);
        for (MockRule r : rulesList) {
            rulesModel.addRow(new Object[] { r.method, r.path, r.responseMode, r.responses.size() });
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
                responsesModel.addRow(new Object[] { idx++, r.responseStatus, r.contentType, crit });
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
        } catch (Exception ignored) {
        }
        config.rules = this.rulesList;
        String json = new com.google.gson.Gson().toJson(config);
        model.setBodyRawContent(json);
    }

    public void loadFromModel() {
        if (model.getBodyRawContent() != null && !model.getBodyRawContent().isBlank()) {
            try {
                MockServerConfig config = new com.google.gson.Gson().fromJson(model.getBodyRawContent(),
                        MockServerConfig.class);
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

    public void save() {
        saveActiveResponseFromUI();
        updateModel();
        mainFrame.saveCollections();
        MainFrame.showToast(this, "Mock Server configuration saved!");
    }

    public boolean hasUnsavedChanges() {
        MockServerConfig config = new MockServerConfig();
        try {
            config.port = Integer.parseInt(portField.getText().trim());
        } catch (Exception ignored) {
            config.port = 8085;
        }
        
        // Deep copy rules to not mutate original UI state before actual save
        config.rules = new ArrayList<>();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = gson.toJson(this.rulesList);
        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<MockRule>>(){}.getType();
        config.rules = gson.fromJson(json, listType);

        if (activeRule != null) {
            int idx = rulesList.indexOf(activeRule);
            if (idx >= 0) {
                MockRule cloneRule = config.rules.get(idx);
                cloneRule.method = (String) methodCombo.getSelectedItem();
                cloneRule.path = pathField.getText().trim();
                cloneRule.responseMode = (String) responseModeCombo.getSelectedItem();
                try {
                    cloneRule.delayMs = Integer.parseInt(delayField.getText().trim());
                } catch (Exception ignored) {
                    cloneRule.delayMs = 0;
                }
                
                if (activeResponse != null) {
                    int respIdx = activeRule.responses.indexOf(activeResponse);
                    if (respIdx >= 0) {
                        MockResponse cloneResp = cloneRule.responses.get(respIdx);
                        try {
                            cloneResp.responseStatus = Integer.parseInt(statusField.getText().trim());
                        } catch (Exception ignored) {}
                        cloneResp.contentType = (String) contentTypeCombo.getSelectedItem();
                        cloneResp.matchType = (String) matchTypeCombo.getSelectedItem();
                        cloneResp.matchKey = matchKeyField.getText().trim();
                        cloneResp.matchValue = matchValueField.getText().trim();
                        cloneResp.body = mockBodyArea.getText();
                        
                        if (headersTable.getCellEditor() != null) {
                            headersTable.getCellEditor().stopCellEditing();
                        }
                        cloneResp.headers.clear();
                        for (int i = 0; i < headersModel.getRowCount(); i++) {
                            Boolean active = (Boolean) headersModel.getValueAt(i, 0);
                            String key = (String) headersModel.getValueAt(i, 1);
                            String val = (String) headersModel.getValueAt(i, 2);
                            if (key != null && !key.trim().isEmpty()) {
                                key = key.trim();
                                boolean isActive = (active != null && active);
                                if (key.equalsIgnoreCase("Content-Type") && isActive && val != null && val.equals(cloneResp.contentType))
                                    continue;
                                if (key.equalsIgnoreCase("Date") && isActive && val != null && val.equals("<auto-generated>"))
                                    continue;
                                if (key.equalsIgnoreCase("Connection") && isActive && val != null && val.equals("keep-alive"))
                                    continue;
                                if (key.equalsIgnoreCase("Server") && isActive && val != null && val.equals("ApiBanker Mock Server"))
                                    continue;

                                cloneResp.headers.add(new String[] { String.valueOf(isActive), key, val == null ? "" : val });
                            }
                        }
                    }
                }
            }
        }
        
        String currentJson = gson.toJson(config);
        String savedJson = model.getBodyRawContent();
        if (savedJson == null || savedJson.isBlank()) {
            return !currentJson.equals("{\"port\":8085,\"rules\":[]}");
        }
        try {
            MockServerConfig savedConfig = gson.fromJson(savedJson, MockServerConfig.class);
            String normalizedSavedJson = gson.toJson(savedConfig);
            return !currentJson.equals(normalizedSavedJson);
        } catch (Exception e) {
            return !currentJson.equals(savedJson);
        }
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
        int height = Math.max(28, size + 12);
        
        if (startStopBtn != null) {
            startStopBtn.setPreferredSize(null);
            int startWidth = Math.max(100, startStopBtn.getPreferredSize().width + 12);
            startStopBtn.setPreferredSize(new Dimension(startWidth, height));
        }
        
        if (saveConfigBtn != null) {
            saveConfigBtn.setPreferredSize(null);
            int saveWidth = Math.max(80, saveConfigBtn.getPreferredSize().width + 16);
            saveConfigBtn.setPreferredSize(new Dimension(saveWidth, height));
        }
    }

    private String extractPath(String url) {
        if (url == null || url.isBlank())
            return "/";
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
            JOptionPane.showMessageDialog(this, "No collections available.", "Import APIs",
                    JOptionPane.WARNING_MESSAGE);
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

        DefaultTableModel model = new DefaultTableModel(new Object[] { "Import", "Name", "Method", "Path" }, 0) {
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
                        model.addRow(new Object[] { true, req.getName(), req.getMethod(), extractPath(req.getUrl()) });
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
                        resp.body = (sourceBody != null && !sourceBody.isEmpty()) ? sourceBody
                                : "{\n  \"message\": \"Imported mock endpoint\"\n}";
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
            JOptionPane.showMessageDialog(this, "Imported " + imported + " APIs to Mock Server!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "Successfully imported " + count + " endpoints from OpenAPI spec!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error parsing OpenAPI Spec: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
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
                        if (method.equals("GET") || method.equals("POST") || method.equals("PUT")
                                || method.equals("DELETE") || method.equals("PATCH")) {
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
                                    for (Map.Entry<String, com.google.gson.JsonElement> respEntry : responsesObj
                                            .entrySet()) {
                                        String statusStr = respEntry.getKey();
                                        int status = 200;
                                        try {
                                            status = Integer.parseInt(statusStr);
                                        } catch (Exception ignored) {
                                        }

                                        MockResponse openApiResp = new MockResponse();
                                        openApiResp.responseStatus = status;
                                        openApiResp.contentType = "application/json";
                                        openApiResp.body = "{\n  \"status\": " + status + "\n}";
                                        rule.responses.add(openApiResp);
                                    }
                                }
                            } catch (Exception ignored) {
                            }

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
            if (trimmed.startsWith("#") || trimmed.isEmpty())
                continue;

            if (trimmed.startsWith("/") && trimmed.endsWith(":")) {
                currentPath = trimmed.substring(0, trimmed.length() - 1).trim();
            } else if (currentPath != null && (trimmed.startsWith("get:") || trimmed.startsWith("post:")
                    || trimmed.startsWith("put:") || trimmed.startsWith("delete:") || trimmed.startsWith("patch:"))) {
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
            String query = exchange.getRequestURI().getQuery();

            String requestBody = "";
            try (java.io.InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }

            StringBuilder reqLog = new StringBuilder();
            reqLog.append("=== INCOMING REQUEST ===\n");
            reqLog.append(requestMethod).append(" ").append(requestPath).append(query != null ? "?" + query : "")
                    .append("\n");
            reqLog.append("Headers:\n");
            exchange.getRequestHeaders().forEach(
                    (k, v) -> reqLog.append("  ").append(k).append(": ").append(String.join(", ", v)).append("\n"));
            if (!requestBody.isEmpty()) {
                reqLog.append("Body:\n").append(requestBody).append("\n");
            }
            logTraffic(reqLog.toString());

            Map<String, String> queryParams = new HashMap<>();
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
                    if (matchedRule.delayMs > 0) {
                        try {
                            Thread.sleep(matchedRule.delayMs);
                        } catch (InterruptedException ignored) {
                        }
                    }

                    byte[] responseBytes = matchedResponse.body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", matchedResponse.contentType + "; charset=utf-8");
                    boolean hasServer = false;
                    if (matchedResponse.headers != null) {
                        for (String[] h : matchedResponse.headers) {
                            if (h.length >= 3 && "true".equalsIgnoreCase(h[0])) {
                                exchange.getResponseHeaders().add(h[1], h[2]);
                                if (h[1].equalsIgnoreCase("Server"))
                                    hasServer = true;
                            }
                        }
                    }
                    if (!hasServer) {
                        exchange.getResponseHeaders().add("Server", "ApiBanker Mock Server");
                    }
                    exchange.sendResponseHeaders(matchedResponse.responseStatus, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }

                    StringBuilder resLog = new StringBuilder();
                    resLog.append("=== OUTGOING RESPONSE ===\n");
                    resLog.append("Matched Rule: ").append(matchedRule.method).append(" ").append(matchedRule.path)
                            .append("\n");
                    resLog.append("Status: ").append(matchedResponse.responseStatus).append("\n");
                    if (matchedRule.delayMs > 0) {
                        resLog.append("Simulated Delay: ").append(matchedRule.delayMs).append("ms\n");
                    }
                    resLog.append("Headers:\n");
                    exchange.getResponseHeaders().forEach((k, v) -> resLog.append("  ").append(k).append(": ")
                            .append(String.join(", ", v)).append("\n"));
                    resLog.append("Body:\n").append(matchedResponse.body).append("\n");
                    resLog.append("=========================\n");
                    logTraffic(resLog.toString());
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

            StringBuilder resLog = new StringBuilder();
            resLog.append("=== OUTGOING RESPONSE (404 Not Found) ===\n");
            resLog.append("No matching rule found.\n");
            resLog.append("Body:\n").append(errorBody).append("\n");
            resLog.append("=========================================\n");
            logTraffic(resLog.toString());
        }
    }
}
