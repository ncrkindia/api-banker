package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import in.slpro.apibanker.logger.ConsoleLogger;
import in.slpro.apibanker.logger.LogEntry;

import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * LogConsolePanel
 *
 * <p>
 * Core functionality and implementation logic for LogConsolePanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.1
 * @since 1.0.0
 */
public class LogConsolePanel extends JPanel implements ConsoleLogger.LogListener {
    private final List<LogEntry> displayedEntries = new ArrayList<>();
    private final DefaultTableModel tableModel;
    private final JTable logTable;
    private final JTextArea detailViewer;

    private final JTextField searchField;
    private final JComboBox<String> methodCombo;
    private final JTextField statusFilterField;
    private final JTextField latencyFilterField;
    private final JTextField sizeFilterField;

    public LogConsolePanel(MainFrame mainFrame) {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        // --- TOP PANEL: Filtering & Exporter ---
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(UIManager.getColor("Panel.background"));
        topPanel.setBorder(BorderFactory.createTitledBorder("Log Filters & Exporter"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("Search Text:"), gbc);
        gbc.gridx = 1;
        searchField = new JTextField(15);
        topPanel.add(searchField, gbc);

        gbc.gridx = 2;
        topPanel.add(new JLabel("Method:"), gbc);
        gbc.gridx = 3;
        methodCombo = new JComboBox<>(new String[] { "ALL", "GET", "POST", "PUT", "DELETE", "PATCH" });
        topPanel.add(methodCombo, gbc);

        gbc.gridx = 4;
        topPanel.add(new JLabel("Status Code:"), gbc);
        gbc.gridx = 5;
        statusFilterField = new JTextField(4);
        topPanel.add(statusFilterField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        topPanel.add(new JLabel("Latency (>= ms):"), gbc);
        gbc.gridx = 1;
        latencyFilterField = new JTextField(6);
        topPanel.add(latencyFilterField, gbc);

        gbc.gridx = 2;
        topPanel.add(new JLabel("Size (>= bytes):"), gbc);
        gbc.gridx = 3;
        sizeFilterField = new JTextField(6);
        topPanel.add(sizeFilterField, gbc);

        gbc.gridx = 4;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnPanel.setBackground(UIManager.getColor("Panel.background"));
        JButton filterBtn = new JButton("Apply Filters");
        Color accent = UIManager.getColor("AccentColor");
        filterBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        filterBtn.setForeground(Color.WHITE);
        filterBtn.addActionListener(e -> applyFilters());
        JButton clearFiltersBtn = new JButton("Reset");
        clearFiltersBtn.addActionListener(e -> {
            searchField.setText("");
            methodCombo.setSelectedIndex(0);
            statusFilterField.setText("");
            latencyFilterField.setText("");
            sizeFilterField.setText("");
            applyFilters();
        });
        btnPanel.add(filterBtn);
        btnPanel.add(clearFiltersBtn);
        btnPanel.add(mainFrame.createInfoBadge("sec-console-logs", "View Logs Guide"));
        gbc.gridwidth = 2;
        topPanel.add(btnPanel, gbc);

        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.gridwidth = 1;
        JPanel exportPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        exportPanel.setBackground(UIManager.getColor("Panel.background"));
        JButton exportCsvBtn = new JButton("Export to CSV");
        JButton exportHtmlBtn = new JButton("Export to HTML");
        JButton exportTxtBtn = new JButton("Export to TXT");
        exportCsvBtn.addActionListener(e -> exportLogs("csv"));
        exportHtmlBtn.addActionListener(e -> exportLogs("html"));
        exportTxtBtn.addActionListener(e -> exportLogs("txt"));
        exportPanel.add(exportCsvBtn);
        exportPanel.add(exportHtmlBtn);
        exportPanel.add(exportTxtBtn);
        topPanel.add(exportPanel, gbc);

        add(topPanel, BorderLayout.NORTH);

        // --- CENTER: Log Table & Detail split ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        detailViewer = new JTextArea();
        detailViewer.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        detailViewer.setEditable(false);
        detailViewer.setBackground(UIManager.getColor("Workspace.panelBackground"));

        tableModel = new DefaultTableModel(
                new String[] { "Timestamp", "Level", "Method", "URL", "Status", "Latency (ms)", "Size (bytes)" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(tableModel);
        logTable.setRowHeight(24);
        logTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logTable.getSelectionModel().addListSelectionListener(e -> {
            int selected = logTable.getSelectedRow();
            if (selected >= 0 && selected < displayedEntries.size()) {
                detailViewer.setText(displayedEntries.get(selected).getMessage());
                detailViewer.setCaretPosition(0);
            }
        });

        JScrollPane tableScroll = new JScrollPane(logTable);
        splitPane.setTopComponent(tableScroll);

        JScrollPane detailScroll = new JScrollPane(detailViewer);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Log Entry Trace Details"));
        splitPane.setBottomComponent(detailScroll);

        add(splitPane, BorderLayout.CENTER);

        // Register listener and load initial
        ConsoleLogger.getInstance().addListener(this);
        loadAllLogs();
    }

    private void loadAllLogs() {
        displayedEntries.clear();
        tableModel.setRowCount(0);
        for (LogEntry entry : ConsoleLogger.getInstance().getEntries()) {
            addEntryToTable(entry);
        }
    }

    private void addEntryToTable(LogEntry entry) {
        displayedEntries.add(0, entry);
        String time = LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.getTimestamp()), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        tableModel.insertRow(0, new Object[] {
                time,
                entry.getLevel(),
                entry.getMethod() != null ? entry.getMethod() : "N/A",
                entry.getUrl() != null ? entry.getUrl() : "N/A",
                entry.getStatusCode(),
                entry.getDurationMs(),
                entry.getResponseSize()
        });
    }

    private void applyFilters() {
        displayedEntries.clear();
        tableModel.setRowCount(0);

        String search = searchField.getText().toLowerCase().trim();
        String method = (String) methodCombo.getSelectedItem();
        String status = statusFilterField.getText().trim();
        int minLatency = -1;
        long minSize = -1;

        try {
            if (!latencyFilterField.getText().trim().isEmpty()) {
                minLatency = Integer.parseInt(latencyFilterField.getText().trim());
            }
        } catch (NumberFormatException ignored) {
        }

        try {
            if (!sizeFilterField.getText().trim().isEmpty()) {
                minSize = Long.parseLong(sizeFilterField.getText().trim());
            }
        } catch (NumberFormatException ignored) {
        }

        for (LogEntry entry : ConsoleLogger.getInstance().getEntries()) {
            if (!search.isEmpty()) {
                boolean matchMsg = entry.getMessage() != null && entry.getMessage().toLowerCase().contains(search);
                boolean matchUrl = entry.getUrl() != null && entry.getUrl().toLowerCase().contains(search);
                if (!matchMsg && !matchUrl)
                    continue;
            }

            if (!"ALL".equals(method)) {
                if (entry.getMethod() == null || !entry.getMethod().equalsIgnoreCase(method))
                    continue;
            }

            if (!status.isEmpty()) {
                String entryStatus = String.valueOf(entry.getStatusCode());
                if (status.endsWith("xx")) {
                    String prefix = status.substring(0, 1);
                    if (!entryStatus.startsWith(prefix))
                        continue;
                } else {
                    if (!entryStatus.equals(status))
                        continue;
                }
            }

            if (minLatency != -1 && entry.getDurationMs() < minLatency)
                continue;
            if (minSize != -1 && entry.getResponseSize() < minSize)
                continue;

            addEntryToTable(entry);
        }
    }

    @Override
    public void onLogEntry(LogEntry entry) {
        SwingUtilities.invokeLater(() -> {
            // Check if matches currently set filters
            boolean matches = true;
            String search = searchField.getText().toLowerCase().trim();
            if (!search.isEmpty()) {
                boolean matchMsg = entry.getMessage() != null && entry.getMessage().toLowerCase().contains(search);
                boolean matchUrl = entry.getUrl() != null && entry.getUrl().toLowerCase().contains(search);
                if (!matchMsg && !matchUrl)
                    matches = false;
            }
            String method = (String) methodCombo.getSelectedItem();
            if (!"ALL".equals(method)) {
                if (entry.getMethod() == null || !entry.getMethod().equalsIgnoreCase(method))
                    matches = false;
            }
            String status = statusFilterField.getText().trim();
            if (!status.isEmpty()) {
                String entryStatus = String.valueOf(entry.getStatusCode());
                if (status.endsWith("xx")) {
                    String prefix = status.substring(0, 1);
                    if (!entryStatus.startsWith(prefix))
                        matches = false;
                } else {
                    if (!entryStatus.equals(status))
                        matches = false;
                }
            }
            try {
                if (!latencyFilterField.getText().trim().isEmpty()) {
                    int minLatency = Integer.parseInt(latencyFilterField.getText().trim());
                    if (entry.getDurationMs() < minLatency)
                        matches = false;
                }
            } catch (NumberFormatException ignored) {
            }
            try {
                if (!sizeFilterField.getText().trim().isEmpty()) {
                    long minSize = Long.parseLong(sizeFilterField.getText().trim());
                    if (entry.getResponseSize() < minSize)
                        matches = false;
                }
            } catch (NumberFormatException ignored) {
            }

            if (matches) {
                addEntryToTable(entry);
            }
        });
    }

    private String formatHeaders(java.util.Map<String, java.util.List<String>> headers) {
        if (headers == null || headers.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, java.util.List<String>> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(String.join(", ", entry.getValue())).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String str) {
        if (str == null)
            return "";
        return str.replace("\"", "\"\"");
    }

    private String escapeHtml(String str) {
        if (str == null)
            return "";
        return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    private void exportLogs(String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
        String ext = "txt".equals(format) ? "log" : format;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("apibanker-live-log-" + timestamp + "." + ext));
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(dest, StandardCharsets.UTF_8)) {
                if ("csv".equals(format)) {
                    pw.println(
                            "Timestamp,Level,Method,URL,Status,Latency(ms),Size(bytes),Request Headers,Request Body,Response Headers,Response Body");
                    for (LogEntry e : displayedEntries) {
                        String time = LocalDateTime
                                .ofInstant(Instant.ofEpochMilli(e.getTimestamp()), ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%d,\"%s\",\"%s\",\"%s\",\"%s\"%n",
                                time, e.getLevel(), e.getMethod(), e.getUrl(), e.getStatusCode(), e.getDurationMs(),
                                e.getResponseSize(),
                                escapeCsv(formatHeaders(e.getRequestHeaders())), escapeCsv(e.getRequestBody()),
                                escapeCsv(formatHeaders(e.getResponseHeaders())), escapeCsv(e.getResponseBody()));
                    }
                } else if ("html".equals(format)) {
                    pw.println("<html><head><title>ApiBanker Logs Export</title>");
                    pw.println(
                            "<style>table { border-collapse: collapse; width: 100%; } th, td { border: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; } tr:nth-child(even){background-color: #f2f2f2} th { background-color: #2c3e50; color: white; } pre { white-space: pre-wrap; word-wrap: break-word; }</style>");
                    pw.println("</head><body><h2>ApiBanker Exported Traffic Logs</h2>");
                    pw.println(
                            "<table><tr><th>Timestamp</th><th>Level</th><th>Method</th><th>URL</th><th>Status</th><th>Latency (ms)</th><th>Size (bytes)</th><th>Request Headers</th><th>Request Body</th><th>Response Headers</th><th>Response Body</th></tr>");
                    for (LogEntry e : displayedEntries) {
                        String time = LocalDateTime
                                .ofInstant(Instant.ofEpochMilli(e.getTimestamp()), ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        pw.printf(
                                "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%d</td><td>%d</td><td>%d</td><td>%s</td><td><pre>%s</pre></td><td>%s</td><td><pre>%s</pre></td></tr>%n",
                                time, e.getLevel(), e.getMethod(), e.getUrl(), e.getStatusCode(), e.getDurationMs(),
                                e.getResponseSize(),
                                escapeHtml(formatHeaders(e.getRequestHeaders())), escapeHtml(e.getRequestBody()),
                                escapeHtml(formatHeaders(e.getResponseHeaders())), escapeHtml(e.getResponseBody()));
                    }
                    pw.println("</table></body></html>");
                } else {
                    for (LogEntry e : displayedEntries) {
                        String time = LocalDateTime
                                .ofInstant(Instant.ofEpochMilli(e.getTimestamp()), ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        pw.println("=================================================");
                        pw.printf("Time: %s | Level: %s | Method: %s | Status: %d | Latency: %d ms | Size: %d bytes%n",
                                time, e.getLevel(), e.getMethod(), e.getStatusCode(), e.getDurationMs(),
                                e.getResponseSize());
                        pw.println("URL: " + e.getUrl());
                        pw.println("Request Headers:\n" + formatHeaders(e.getRequestHeaders()));
                        pw.println("Request Body:\n" + (e.getRequestBody() != null ? e.getRequestBody() : ""));
                        pw.println("Response Headers:\n" + formatHeaders(e.getResponseHeaders()));
                        pw.println("Response Body:\n" + (e.getResponseBody() != null ? e.getResponseBody() : ""));
                        pw.println("Trace:");
                        pw.println(e.getMessage());
                        pw.println("=================================================\n");
                    }
                }
                MainFrame.showToast(this, "Logs successfully exported!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exporting: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void removeListener() {
        ConsoleLogger.getInstance().removeListener(this);
    }
}


