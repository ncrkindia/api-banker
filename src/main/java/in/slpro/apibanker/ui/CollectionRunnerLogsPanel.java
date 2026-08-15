package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import in.slpro.apibanker.storage.StorageManager;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * CollectionRunnerLogsPanel
 *
 * <p>
 * Core functionality and implementation logic for CollectionRunnerLogsPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.6.0-beta
 * @since 1.0.0
 */
public class CollectionRunnerLogsPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable logTable;
    private final JTextArea summaryViewer;
    private final JTextField collFilterField;
    private final JTextField dateFilterField;
    private final TableRowSorter<DefaultTableModel> sorter;

    private List<RunLogEntry> allRuns = new ArrayList<>();
    private File selectedRunDir = null;

    public CollectionRunnerLogsPanel(MainFrame mainFrame) {
        this.summaryViewer = new JTextArea();
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        // TOP: Filters and Export
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(UIManager.getColor("Panel.background"));
        topPanel.setBorder(BorderFactory.createTitledBorder("Runner Logs Filters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("Collection:"), gbc);
        gbc.gridx = 1;
        collFilterField = new JTextField(15);
        topPanel.add(collFilterField, gbc);

        gbc.gridx = 2;
        topPanel.add(new JLabel("Date (yyyy-MM-dd):"), gbc);
        gbc.gridx = 3;
        dateFilterField = new JTextField(10);
        topPanel.add(dateFilterField, gbc);

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
            collFilterField.setText("");
            dateFilterField.setText("");
            applyFilters();
        });
        btnPanel.add(filterBtn);
        btnPanel.add(clearFiltersBtn);
        btnPanel.add(mainFrame.createInfoBadge("sec-console-logs", "View Logs Guide"));
        topPanel.add(btnPanel, gbc);

        gbc.gridx = 5;
        gbc.gridy = 0;
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadLogs());
        topPanel.add(refreshBtn, gbc);

        add(topPanel, BorderLayout.NORTH);

        // CENTER: Split Pane (Table + Details)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.4);

        tableModel = new DefaultTableModel(new String[] { "Date / Time", "Collection - Runner", "Folder Path" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(tableModel);
        logTable.setRowHeight(26);
        logTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        sorter = new TableRowSorter<>(tableModel);
        logTable.setRowSorter(sorter);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));

        logTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int viewRow = logTable.getSelectedRow();
                if (viewRow >= 0) {
                    int modelRow = logTable.convertRowIndexToModel(viewRow);
                    String path = (String) tableModel.getValueAt(modelRow, 2);
                    selectedRunDir = new File(path);
                    displaySummary(selectedRunDir);
                } else {
                    selectedRunDir = null;
                    summaryViewer.setText("");
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(logTable);
        splitPane.setTopComponent(tableScroll);

        // Bottom Details
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBackground(UIManager.getColor("Panel.background"));
        detailPanel.setBorder(BorderFactory.createTitledBorder("Run Details & Exports"));

        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        exportPanel.setBackground(UIManager.getColor("Panel.background"));
        JButton expSummaryBtn = new JButton("Export Summary Log");
        JButton expDumpBtn = new JButton("Export Dump Log");
        JButton expMetricsBtn = new JButton("Export Metrics");

        expSummaryBtn.addActionListener(e -> exportFile("summary.log"));
        expDumpBtn.addActionListener(e -> exportFile("dump.log"));

        JPopupMenu metricsMenu = new JPopupMenu();
        JMenuItem expHtml = new JMenuItem("Export HTML");
        JMenuItem expPdf = new JMenuItem("Export PDF");
        JMenuItem expXlsx = new JMenuItem("Export Excel");
        JMenuItem expCsv = new JMenuItem("Export CSV");

        expHtml.addActionListener(e -> exportGeneratedMetrics("html"));
        expPdf.addActionListener(e -> exportGeneratedMetrics("pdf"));
        expXlsx.addActionListener(e -> exportGeneratedMetrics("xlsx"));
        expCsv.addActionListener(e -> exportGeneratedMetrics("csv"));

        metricsMenu.add(expHtml);
        metricsMenu.add(expCsv);
        // Only HTML and CSV are easily generated from logs. PDF/Excel requires
        // charts/POI which need UI context or complex libs.

        expMetricsBtn.addActionListener(e -> metricsMenu.show(expMetricsBtn, 0, expMetricsBtn.getHeight()));

        exportPanel.add(expSummaryBtn);
        exportPanel.add(expDumpBtn);
        exportPanel.add(expMetricsBtn);

        summaryViewer.setEditable(false);
        summaryViewer.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        summaryViewer.setBackground(UIManager.getColor("Workspace.panelBackground"));

        detailPanel.add(exportPanel, BorderLayout.NORTH);
        detailPanel.add(new JScrollPane(summaryViewer), BorderLayout.CENTER);

        splitPane.setBottomComponent(detailPanel);
        add(splitPane, BorderLayout.CENTER);

        loadLogs();
    }

    private void loadLogs() {
        allRuns.clear();
        tableModel.setRowCount(0);
        String logsDir = StorageManager.getInstance().getSettings().getLogsDirectory();
        File logsFolder = new File(logsDir);
        if (logsFolder.exists() && logsFolder.isDirectory()) {
            File[] runGroups = logsFolder.listFiles();
            if (runGroups != null) {
                for (File group : runGroups) {
                    if (group.isDirectory()) {
                        String collName = group.getName();
                        File[] runs = group.listFiles();
                        if (runs != null) {
                            for (File run : runs) {
                                if (run.isDirectory()) {
                                    String dateStr = run.getName();
                                    allRuns.add(new RunLogEntry(dateStr, collName, run.getAbsolutePath()));
                                }
                            }
                        }
                    }
                }
            }
        }
        for (RunLogEntry e : allRuns) {
            tableModel.addRow(new Object[] { e.date, e.collection, e.path });
        }
    }

    private void applyFilters() {
        String collFilter = collFilterField.getText().toLowerCase().trim();
        String dateFilter = dateFilterField.getText().toLowerCase().trim();
        tableModel.setRowCount(0);
        for (RunLogEntry e : allRuns) {
            boolean matchColl = collFilter.isEmpty() || e.collection.toLowerCase().contains(collFilter);
            boolean matchDate = dateFilter.isEmpty() || e.date.toLowerCase().contains(dateFilter);
            if (matchColl && matchDate) {
                tableModel.addRow(new Object[] { e.date, e.collection, e.path });
            }
        }
    }

    private void displaySummary(File runDir) {
        summaryViewer.setText("");
        File sumFile = new File(runDir, "summary.log");
        if (sumFile.exists()) {
            try {
                String content = Files.readString(sumFile.toPath());
                summaryViewer.setText(content);
                summaryViewer.setCaretPosition(0);
            } catch (Exception ex) {
                summaryViewer.setText("Error reading summary.log: " + ex.getMessage());
            }
        } else {
            summaryViewer.setText("No summary.log found in " + runDir.getName());
        }
    }

    private void exportFile(String fileName) {
        if (selectedRunDir == null) {
            JOptionPane.showMessageDialog(this, "Please select a run first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File source = new File(selectedRunDir, fileName);
        if (!source.exists()) {
            JOptionPane.showMessageDialog(this, fileName + " does not exist for this run. Perhaps it was not saved.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        String safeName = selectedRunDir.getParentFile().getName() + "-" + selectedRunDir.getName() + "-" + fileName;
        chooser.setSelectedFile(new File(safeName));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.copy(source.toPath(), chooser.getSelectedFile().toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                MainFrame.showToast(this, fileName + " exported successfully.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportGeneratedMetrics(String format) {
        if (selectedRunDir == null) {
            JOptionPane.showMessageDialog(this, "Please select a run first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File sumFile = new File(selectedRunDir, "summary.log");
        if (!sumFile.exists()) {
            JOptionPane.showMessageDialog(this, "No summary.log found to generate metrics.", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        String safeName = selectedRunDir.getParentFile().getName() + "-" + selectedRunDir.getName() + "-metrics."
                + format;
        chooser.setSelectedFile(new File(safeName));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                List<LogItem> items = parseSummaryLog(sumFile);
                if ("csv".equalsIgnoreCase(format)) {
                    generateCSV(items, chooser.getSelectedFile());
                } else if ("html".equalsIgnoreCase(format)) {
                    generateHTML(items, chooser.getSelectedFile());
                } else {
                    JOptionPane.showMessageDialog(this,
                            format.toUpperCase() + " generation from logs is not supported yet.", "Info",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                MainFrame.showToast(this,
                        "Metrics (" + format.toUpperCase() + ") generated and exported successfully.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Generation failed: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class LogItem {
        String time, method, name;
        int status;
        long latency;
        String tests = "-";
    }

    private List<LogItem> parseSummaryLog(File sumFile) throws Exception {
        List<LogItem> items = new ArrayList<>();
        List<String> lines = Files.readAllLines(sumFile.toPath());
        LogItem lastItem = null;
        for (String line : lines) {
            // 2026-08-04 17:21:00 | GET MyReq | Status: 200 | Latency: 120ms
            if (line.contains(" | Status: ") && line.contains(" | Latency: ")) {
                try {
                    String[] parts = line.split(" \\| ");
                    if (parts.length >= 4) {
                        LogItem item = new LogItem();
                        item.time = parts[0].trim();
                        String[] reqParts = parts[1].trim().split(" ", 2);
                        item.method = reqParts[0];
                        item.name = reqParts.length > 1 ? reqParts[1] : "";
                        item.status = Integer.parseInt(parts[2].replace("Status:", "").trim());
                        item.latency = Long.parseLong(parts[3].replace("Latency:", "").replace("ms", "").trim());
                        items.add(item);
                        lastItem = item;
                    }
                } catch (Exception ignored) {
                }
            } else if (lastItem != null && line.contains("       Tests: ")) {
                lastItem.tests = line.trim().replace("Tests: ", "");
                lastItem = null;
            }
        }
        return items;
    }

    private void generateCSV(List<LogItem> items, File dest) throws Exception {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.FileWriter(dest, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("\"ApiBanker Generated Metrics Report\"");
            pw.println("\"Generated At:\",\"" + java.time.LocalDateTime.now().toString() + "\"");
            pw.println();
            pw.println("Time,Method,API Name,Status,Latency (ms),Tests");
            for (LogItem item : items) {
                pw.println(String.format("\"%s\",\"%s\",\"%s\",%d,%d,\"%s\"", item.time, item.method, item.name, item.status,
                        item.latency, item.tests));
            }
        }
    }

    private void generateHTML(List<LogItem> items, File dest) throws Exception {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.FileWriter(dest, java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("<html><head><title>ApiBanker Metrics</title>");
            pw.println(
                    "<style>body{font-family:sans-serif;margin:20px;} table{width:100%;border-collapse:collapse;} th,td{border:1px solid #ddd;padding:8px;text-align:left;} th{background-color:#f4f4f4;} .pass{color:green;} .fail{color:red;}</style>");
            pw.println("</head><body>");
            pw.println("<h2>ApiBanker Generated Metrics Report</h2>");
            pw.println(
                    "<table><tr><th>Time</th><th>Method</th><th>API Name</th><th>Status</th><th>Latency (ms)</th><th>Tests</th></tr>");
            for (LogItem item : items) {
                String statusClass = (item.status >= 200 && item.status < 400) ? "pass" : "fail";
                pw.println(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td class='%s'>%d</td><td>%d</td><td>%s</td></tr>",
                        item.time, item.method, item.name, statusClass, item.status, item.latency, item.tests));
            }
            pw.println("</table></body></html>");
        }
    }

    private static class RunLogEntry {
        String date;
        String collection;
        String path;

        RunLogEntry(String date, String collection, String path) {
            this.date = date;
            this.collection = collection;
            this.path = path;
        }
    }
}


