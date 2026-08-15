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

public class MockServerLogsPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable logTable;
    private final JTextArea summaryViewer;
    private final JTextField serverFilterField;
    private final JTextField dateFilterField;
    private final TableRowSorter<DefaultTableModel> sorter;

    private List<MockLogEntry> allLogs = new ArrayList<>();
    private File selectedLogFile = null;

    public MockServerLogsPanel(MainFrame mainFrame) {
        this.summaryViewer = new JTextArea();
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        // TOP: Filters and Export
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBackground(UIManager.getColor("Panel.background"));
        topPanel.setBorder(BorderFactory.createTitledBorder("Mock Server Logs Filters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(new JLabel("Server Group:"), gbc);
        gbc.gridx = 1;
        serverFilterField = new JTextField(15);
        topPanel.add(serverFilterField, gbc);

        gbc.gridx = 2;
        topPanel.add(new JLabel("Date/Time:"), gbc);
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
            serverFilterField.setText("");
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

        tableModel = new DefaultTableModel(new String[] { "Date / Time", "Mock Server Group", "Log File" }, 0) {
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
                    selectedLogFile = new File(path);
                    displaySummary(selectedLogFile);
                } else {
                    selectedLogFile = null;
                    summaryViewer.setText("");
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(logTable);
        splitPane.setTopComponent(tableScroll);

        // Bottom Details
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBackground(UIManager.getColor("Panel.background"));
        detailPanel.setBorder(BorderFactory.createTitledBorder("Log Content & Exports"));

        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        exportPanel.setBackground(UIManager.getColor("Panel.background"));
        JButton expLogBtn = new JButton("Export Log File");

        expLogBtn.addActionListener(e -> exportFile());

        exportPanel.add(expLogBtn);

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
        allLogs.clear();
        tableModel.setRowCount(0);
        String logsDir = StorageManager.getInstance().getSettings().getLogsDirectory();
        File logsFolder = new File(logsDir);
        if (logsFolder.exists() && logsFolder.isDirectory()) {
            File[] groups = logsFolder.listFiles();
            if (groups != null) {
                for (File group : groups) {
                    if (group.isDirectory()) {
                        String groupName = group.getName();
                        File[] logFiles = group.listFiles((dir, name) -> name.endsWith(".log"));
                        if (logFiles != null) {
                            for (File logFile : logFiles) {
                                String dateStr = logFile.getName().replace(".log", "");
                                allLogs.add(new MockLogEntry(dateStr, groupName, logFile.getAbsolutePath()));
                            }
                        }
                    }
                }
            }
        }
        for (MockLogEntry e : allLogs) {
            tableModel.addRow(new Object[] { e.date, e.group, e.path });
        }
    }

    private void applyFilters() {
        String serverFilter = serverFilterField.getText().toLowerCase().trim();
        String dateFilter = dateFilterField.getText().toLowerCase().trim();
        tableModel.setRowCount(0);
        for (MockLogEntry e : allLogs) {
            boolean matchServer = serverFilter.isEmpty() || e.group.toLowerCase().contains(serverFilter);
            boolean matchDate = dateFilter.isEmpty() || e.date.toLowerCase().contains(dateFilter);
            if (matchServer && matchDate) {
                tableModel.addRow(new Object[] { e.date, e.group, e.path });
            }
        }
    }

    private void displaySummary(File logFile) {
        summaryViewer.setText("");
        if (logFile != null && logFile.exists()) {
            try {
                String content = Files.readString(logFile.toPath());
                summaryViewer.setText(content);
                summaryViewer.setCaretPosition(0);
            } catch (Exception ex) {
                summaryViewer.setText("Error reading log file: " + ex.getMessage());
            }
        } else {
            summaryViewer.setText("No log file found.");
        }
    }

    private void exportFile() {
        if (selectedLogFile == null) {
            JOptionPane.showMessageDialog(this, "Please select a log file first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!selectedLogFile.exists()) {
            JOptionPane.showMessageDialog(this, "Log file does not exist. Perhaps it was not saved.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        String safeName = selectedLogFile.getParentFile().getName() + "-" + selectedLogFile.getName();
        chooser.setSelectedFile(new File(safeName));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.copy(selectedLogFile.toPath(), chooser.getSelectedFile().toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                MainFrame.showToast(this, "Log exported successfully.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static class MockLogEntry {
        String date;
        String group;
        String path;

        MockLogEntry(String date, String group, String path) {
            this.date = date;
            this.group = group;
            this.path = path;
        }
    }
}
