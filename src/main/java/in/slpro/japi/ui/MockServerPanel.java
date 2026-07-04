package in.slpro.japi.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MockServerPanel extends JPanel {
    private final MainFrame mainFrame;
    private HttpServer server;
    private boolean isRunning = false;

    private final JTextField portField;
    private final JButton startStopBtn;
    private final JLabel statusLabel;
    private final JTable rulesTable;
    private final DefaultTableModel rulesModel;
    private final JTextArea mockBodyArea;
    private final JComboBox<String> methodCombo;
    private final JTextField pathField;
    private final JTextField statusField;
    private final JComboBox<String> contentTypeCombo;
    private final JTextArea serverLogArea;

    private static class MockRule {
        String method;
        String path;
        int responseStatus;
        String contentType;
        String body;

        MockRule(String method, String path, int responseStatus, String contentType, String body) {
            this.method = method;
            this.path = path.startsWith("/") ? path : "/" + path;
            this.responseStatus = responseStatus;
            this.contentType = contentType;
            this.body = body;
        }
    }

    private final List<MockRule> rulesList = new ArrayList<>();

    public MockServerPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // --- TOP BAR: Server Controls ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        topPanel.setBackground(Color.WHITE);

        topPanel.add(new JLabel("Mock Server Port:"));
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

        add(topPanel, BorderLayout.NORTH);

        // --- CENTER: Rules Management & Log ---
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.65);

        // Left Component: Rules Editor & Table
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBackground(Color.WHITE);

        rulesModel = new DefaultTableModel(new String[]{"Method", "Path", "Status Code", "Content-Type"}, 0) {
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

        // Rule Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createTitledBorder("Rule Editor"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Method:"), gbc);
        gbc.gridx = 1;
        methodCombo = new JComboBox<>(new String[]{"GET", "POST", "PUT", "DELETE", "PATCH"});
        formPanel.add(methodCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Path:"), gbc);
        gbc.gridx = 1;
        pathField = new JTextField("/api/users", 20);
        formPanel.add(pathField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Status Code:"), gbc);
        gbc.gridx = 1;
        statusField = new JTextField("200", 6);
        formPanel.add(statusField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Content-Type:"), gbc);
        gbc.gridx = 1;
        contentTypeCombo = new JComboBox<>(new String[]{"application/json", "application/xml", "text/plain", "text/html"});
        formPanel.add(contentTypeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Mock Response Body:"), gbc);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.weightx = 1.0;
        mockBodyArea = new JTextArea(8, 30);
        mockBodyArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        mockBodyArea.setText("{\n  \"status\": \"success\",\n  \"data\": {}\n}");
        formPanel.add(new JScrollPane(mockBodyArea), gbc);

        gbc.gridy = 6; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0.0;
        JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        formButtons.setBackground(Color.WHITE);
        JButton addRuleBtn = new JButton("Add/Save Rule");
        addRuleBtn.addActionListener(e -> addRule());
        JButton deleteRuleBtn = new JButton("Delete Selected");
        deleteRuleBtn.addActionListener(e -> deleteRule());
        formButtons.add(addRuleBtn);
        formButtons.add(deleteRuleBtn);
        formPanel.add(formButtons, gbc);

        leftPanel.add(formPanel, BorderLayout.SOUTH);
        mainSplit.setLeftComponent(leftPanel);

        // Right Component: Live logs
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(Color.WHITE);

        serverLogArea = new JTextArea();
        serverLogArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        serverLogArea.setEditable(false);
        serverLogArea.setBackground(new Color(248, 249, 250));
        JScrollPane logScroll = new JScrollPane(serverLogArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Mock Server Live Traffic Log"));
        rightPanel.add(logScroll, BorderLayout.CENTER);

        JButton clearLogBtn = new JButton("Clear Traffic Log");
        clearLogBtn.addActionListener(e -> serverLogArea.setText(""));
        rightPanel.add(clearLogBtn, BorderLayout.SOUTH);

        mainSplit.setRightComponent(rightPanel);

        add(mainSplit, BorderLayout.CENTER);

        // Add some default rules
        rulesList.add(new MockRule("GET", "/api/users", 200, "application/json", "[\n  {\"id\": 1, \"name\": \"Alice\"},\n  {\"id\": 2, \"name\": \"Bob\"}\n]"));
        rulesList.add(new MockRule("POST", "/api/users", 201, "application/json", "{\n  \"status\": \"created\",\n  \"id\": 3\n}"));
        refreshRulesTable();
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
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new MockHandler());
            server.setExecutor(null); // default executor
            server.start();

            isRunning = true;
            startStopBtn.setText("Stop Server");
            startStopBtn.setBackground(new Color(231, 76, 60));
            statusLabel.setText("Status: Running on port " + port);
            statusLabel.setForeground(new Color(39, 174, 96));
            logTraffic("Server started on port " + port);
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

    private void logTraffic(String msg) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        SwingUtilities.invokeLater(() -> {
            serverLogArea.append(String.format("[%s] %s%n", timestamp, msg));
            serverLogArea.setCaretPosition(serverLogArea.getDocument().getLength());
        });
    }

    private void addRule() {
        String method = (String) methodCombo.getSelectedItem();
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Path cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int status;
        try {
            status = Integer.parseInt(statusField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid status code", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String contentType = (String) contentTypeCombo.getSelectedItem();
        String body = mockBodyArea.getText();

        // Check if exists
        rulesList.removeIf(r -> r.method.equalsIgnoreCase(method) && r.path.equals(path));

        rulesList.add(new MockRule(method, path, status, contentType, body));
        refreshRulesTable();
        logTraffic("Saved rule: " + method + " " + path);
    }

    private void deleteRule() {
        int selected = rulesTable.getSelectedRow();
        if (selected >= 0) {
            String method = (String) rulesModel.getValueAt(selected, 0);
            String path = (String) rulesModel.getValueAt(selected, 1);
            rulesList.removeIf(r -> r.method.equalsIgnoreCase(method) && r.path.equals(path));
            refreshRulesTable();
            logTraffic("Deleted rule: " + method + " " + path);
        }
    }

    private void refreshRulesTable() {
        rulesModel.setRowCount(0);
        for (MockRule r : rulesList) {
            rulesModel.addRow(new Object[]{r.method, r.path, r.responseStatus, r.contentType});
        }
    }

    private class MockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();
            String requestPath = exchange.getRequestURI().getPath();

            logTraffic("Incoming request: " + requestMethod + " " + requestPath);

            // Find matching rule
            MockRule matched = null;
            for (MockRule rule : rulesList) {
                if (rule.method.equalsIgnoreCase(requestMethod) && rule.path.equals(requestPath)) {
                    matched = rule;
                    break;
                }
            }

            // If no exact match, try matching just by path (wildcard or method fallback)
            if (matched == null) {
                for (MockRule rule : rulesList) {
                    if (rule.path.equals(requestPath)) {
                        matched = rule;
                        break;
                    }
                }
            }

            if (matched != null) {
                byte[] responseBytes = matched.body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", matched.contentType + "; charset=utf-8");
                exchange.sendResponseHeaders(matched.responseStatus, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
                logTraffic(String.format("Matched rule: %s %s -> Sending %d (%s)",
                        matched.method, matched.path, matched.responseStatus, matched.contentType));
            } else {
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
}
