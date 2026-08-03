package in.slpro.japi.ui;

import in.slpro.japi.model.ResponseModel;
import in.slpro.japi.model.ScriptResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * ResponsePanel
 *
 * <p>
 * Core functionality and implementation logic for ResponsePanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class ResponsePanel extends JPanel {
    private final JLabel statusLabel;
    private final JLabel timeLabel;
    private final JLabel sizeLabel;
    private final JLabel sslLabel;
    private final RSyntaxTextArea bodyArea;
    private final DefaultTableModel headersModel;
    private final JTabbedPane tabs;
    private final JPanel contentCard;

    private ResponseModel currentResponse;
    private JTextField searchField;
    private JLabel matchCountLabel;
    private JButton prevSearchBtn;
    private JButton nextSearchBtn;

    // Test Results tab components
    private final JLabel testSummaryLabel;
    private final DefaultTableModel testResultsModel;
    private final JTextArea consoleLogArea;
    private final JPanel testResultsPanel;

    public ResponsePanel() {
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        statusBar.setBackground(UIManager.getColor("Workspace.panelBackground"));
        statusBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Workspace.borderColor")));

        statusLabel = new JLabel("—");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        timeLabel = new JLabel("—");
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(new Color(100, 100, 100));

        sizeLabel = new JLabel("—");
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sizeLabel.setForeground(new Color(100, 100, 100));

        sslLabel = new JLabel("SSL");
        sslLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sslLabel.setOpaque(true);
        sslLabel.setVisible(false);
        sslLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        statusBar.add(new JLabel("Status:"));
        statusBar.add(statusLabel);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(new JLabel("Time:"));
        statusBar.add(timeLabel);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(new JLabel("Size:"));
        statusBar.add(sizeLabel);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(sslLabel);

        add(statusBar, BorderLayout.NORTH);

        // Tabs
        tabs = new JTabbedPane();

        // Body tab
        bodyArea = new RSyntaxTextArea();
        bodyArea.setEditable(false);
        bodyArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        bodyArea.setCodeFoldingEnabled(true);
        bodyArea.setAntiAliasingEnabled(true);
        bodyArea.setHighlightCurrentLine(false);
        RTextScrollPane bodyScroll = new RTextScrollPane(bodyArea);
        bodyScroll.setBorder(null);

        JPanel bodyPanel = new JPanel(new BorderLayout());

        // Search Panel (West)
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        searchPanel.setOpaque(false);

        JLabel searchIconLabel = new JLabel("Find:");
        searchIconLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchPanel.add(searchIconLabel);

        searchField = new JTextField(15);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                performSearch(true, false);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                performSearch(true, false);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                performSearch(true, false);
            }
        });
        searchField.addActionListener(e -> performSearch(true, true));
        searchPanel.add(searchField);

        prevSearchBtn = new JButton("◀");
        prevSearchBtn.setToolTipText("Previous Match");
        prevSearchBtn.addActionListener(e -> performSearch(false, true));
        searchPanel.add(prevSearchBtn);

        nextSearchBtn = new JButton("▶");
        nextSearchBtn.setToolTipText("Next Match");
        nextSearchBtn.addActionListener(e -> performSearch(true, true));
        searchPanel.add(nextSearchBtn);

        matchCountLabel = new JLabel("");
        matchCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        matchCountLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        searchPanel.add(matchCountLabel);

        // Actions Panel (East)
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        actionsPanel.setOpaque(false);

        JToggleButton wrapBtn = new JToggleButton("Wrap");
        wrapBtn.addActionListener(e -> bodyArea.setLineWrap(((JToggleButton) e.getSource()).isSelected()));
        actionsPanel.add(wrapBtn);

        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> {
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(bodyArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
        });
        actionsPanel.add(copyBtn);

        JButton exportBtn = new JButton("Save to File");
        exportBtn.addActionListener(e -> exportResponse());
        actionsPanel.add(exportBtn);

        JPanel bodyToolbar = new JPanel(new BorderLayout());
        bodyToolbar.setBackground(UIManager.getColor("Panel.background"));
        bodyToolbar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        bodyToolbar.add(searchPanel, BorderLayout.WEST);
        bodyToolbar.add(actionsPanel, BorderLayout.EAST);

        bodyPanel.add(bodyToolbar, BorderLayout.NORTH);
        bodyPanel.add(bodyScroll, BorderLayout.CENTER);
        tabs.addTab("Body", bodyPanel);

        // Headers tab
        headersModel = new DefaultTableModel(new String[] { "Header", "Value" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable headersTable = new JTable(headersModel);
        headersTable.setRowHeight(24);
        tabs.addTab("Headers", new JScrollPane(headersTable));

        // Test Results tab
        testResultsPanel = new JPanel(new BorderLayout(0, 0));
        testResultsPanel.setBackground(UIManager.getColor("Panel.background"));

        // Summary bar at the top of test results
        testSummaryLabel = new JLabel("No tests run");
        testSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        testSummaryLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        testSummaryLabel.setOpaque(true);
        testSummaryLabel.setBackground(UIManager.getColor("Workspace.panelBackground"));
        testResultsPanel.add(testSummaryLabel, BorderLayout.NORTH);

        // Assertion results table
        testResultsModel = new DefaultTableModel(new String[] { "Status", "Test Name", "Details" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable testTable = new JTable(testResultsModel);
        testTable.setRowHeight(28);
        testTable.getColumnModel().getColumn(0).setMaxWidth(80);
        testTable.getColumnModel().getColumn(0).setMinWidth(60);

        // Custom renderer for status column (PASS/FAIL badges)
        testTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 11));
                if ("PASS".equals(value)) {
                    label.setForeground(new Color(39, 174, 96));
                    if (!isSelected)
                        label.setBackground(new Color(39, 174, 96, 30));
                } else if ("FAIL".equals(value)) {
                    label.setForeground(new Color(192, 57, 43));
                    if (!isSelected)
                        label.setBackground(new Color(192, 57, 43, 30));
                }
                label.setOpaque(true);
                return label;
            }
        });

        // Split: top = test table, bottom = console logs
        consoleLogArea = new JTextArea();
        consoleLogArea.setEditable(false);
        consoleLogArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        consoleLogArea.setBackground(UIManager.getColor("Panel.background"));
        consoleLogArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBackground(UIManager.getColor("Panel.background"));
        JLabel consoleTitle = new JLabel("  Console Output");
        consoleTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        consoleTitle.setForeground(new Color(120, 120, 120));
        consoleTitle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Workspace.borderColor")),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        consolePanel.add(consoleTitle, BorderLayout.NORTH);
        consolePanel.add(new JScrollPane(consoleLogArea), BorderLayout.CENTER);

        JSplitPane testSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(testTable), consolePanel);
        testSplit.setResizeWeight(0.65);
        testSplit.setDividerSize(4);
        testResultsPanel.add(testSplit, BorderLayout.CENTER);

        tabs.addTab("Test Results", testResultsPanel);

        // Empty state
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setBackground(UIManager.getColor("Panel.background"));
        JLabel emptyLabel = new JLabel("Send a request to see the response", SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emptyLabel.setForeground(new Color(150, 150, 150));
        emptyPanel.add(emptyLabel, BorderLayout.CENTER);

        // Card layout to switch between empty and response
        contentCard = new JPanel(new CardLayout());
        contentCard.add(emptyPanel, "empty");
        contentCard.add(tabs, "response");
        add(contentCard, BorderLayout.CENTER);
    }

    public void showResponse(ResponseModel response) {
        if (response == null)
            return;
        this.currentResponse = response;
        if (searchField != null) {
            searchField.setText("");
            matchCountLabel.setText("");
        }

        int code = response.getStatusCode();
        String statusText = code + " " + response.getStatusText();
        statusLabel.setText(statusText);

        Color statusColor;
        if (code >= 200 && code < 300)
            statusColor = new Color(39, 174, 96);
        else if (code >= 400 && code < 500)
            statusColor = new Color(230, 126, 34);
        else if (code >= 500)
            statusColor = new Color(192, 57, 43);
        else
            statusColor = new Color(100, 100, 100);
        statusLabel.setForeground(statusColor);

        long totalMs = response.getExecutionTimeMs();
        long networkMs = response.getNetworkTimeMs();
        timeLabel.setText(networkMs + " ms");
        timeLabel.setForeground(networkMs < 500 ? new Color(39, 174, 96)
                : networkMs < 2000 ? new Color(230, 126, 34) : new Color(192, 57, 43));

        StringBuilder timeBreakdown = new StringBuilder("<html><b style='font-size:11px'>Time Breakdown:</b><br><br>");
        timeBreakdown.append("<b>Pre-request Scripts:</b> ").append(response.getPreRequestTimeMs()).append(" ms<br>");
        timeBreakdown.append("<b>Network Request:</b> ").append(networkMs).append(" ms<br>");
        timeBreakdown.append("<b>Test Scripts:</b> ").append(response.getTestScriptTimeMs()).append(" ms<br><br>");
        timeBreakdown.append("<b>Total Execution Time:</b> ").append(totalMs).append(" ms<br><br>");
        timeLabel.setToolTipText(timeBreakdown.toString());

        long bytes = response.getSizeBytes();
        sizeLabel.setText(bytes < 1024 ? bytes + " B" : (bytes / 1024) + " KB");

        // SSL Label
        if (response.getSslDetails() != null && !response.getSslDetails().isBlank()) {
            sslLabel.setVisible(true);
            sslLabel.setToolTipText(response.getSslDetails());
            if (response.isSslValid()) {
                sslLabel.setForeground(new Color(39, 174, 96));
                sslLabel.setBackground(new Color(39, 174, 96, 30));
            } else {
                sslLabel.setForeground(new Color(192, 57, 43));
                sslLabel.setBackground(new Color(192, 57, 43, 30));
            }
        } else {
            sslLabel.setVisible(false);
        }

        // Set body
        String body = response.getBody() != null ? response.getBody() : "";
        String contentType = "";
        if (response.getHeaders() != null) {
            List<String> ct = response.getHeaders().get("content-type");
            if (ct == null)
                ct = response.getHeaders().get("Content-Type");
            if (ct != null && !ct.isEmpty())
                contentType = ct.get(0).toLowerCase();
        }

        if (contentType.contains("json")) {
            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            try {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(body);
                bodyArea.setText(gson.toJson(el));
            } catch (Exception e) {
                bodyArea.setText(body);
            }
        } else if (contentType.contains("xml") || contentType.contains("html")) {
            bodyArea.setSyntaxEditingStyle(
                    contentType.contains("xml") ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_HTML);
            bodyArea.setText(body);
        } else {
            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            bodyArea.setText(body);
        }
        bodyArea.setCaretPosition(0);

        // Headers
        headersModel.setRowCount(0);
        if (response.getHeaders() != null) {
            for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
                headersModel.addRow(new Object[] { entry.getKey(), String.join(", ", entry.getValue()) });
            }
        }

        // Show response card
        CardLayout cl = (CardLayout) contentCard.getLayout();
        cl.show(contentCard, "response");
    }

    /**
     * Shows test results from script execution in the "Test Results" tab.
     * Displays pre-request script results and test script results with
     * pass/fail badges, assertion details, and console log output.
     */
    public void showTestResults(ScriptResult preRequestResult, ScriptResult testResult) {
        testResultsModel.setRowCount(0);
        consoleLogArea.setText("");

        int totalPassed = 0;
        int totalFailed = 0;
        StringBuilder consoleText = new StringBuilder();

        // Pre-request script results
        if (preRequestResult != null) {
            if (preRequestResult.hasError()) {
                testResultsModel.addRow(new Object[] { "FAIL", "Pre-request Script", preRequestResult.getError() });
                totalFailed++;
            }
            for (String log : preRequestResult.getConsoleLogs()) {
                consoleText.append("[Pre-request] ").append(log).append("\n");
            }
        }

        // Test script results
        if (testResult != null) {
            if (testResult.hasError()) {
                testResultsModel.addRow(new Object[] { "FAIL", "Test Script Error", testResult.getError() });
                totalFailed++;
            }

            for (ScriptResult.TestAssertion assertion : testResult.getAssertions()) {
                String status = assertion.isPassed() ? "PASS" : "FAIL";
                String details = assertion.isPassed() ? ""
                        : (assertion.getFailureMessage() != null ? assertion.getFailureMessage() : "");
                testResultsModel.addRow(new Object[] { status, assertion.getName(), details });
                if (assertion.isPassed())
                    totalPassed++;
                else
                    totalFailed++;
            }

            for (String log : testResult.getConsoleLogs()) {
                consoleText.append("[Test] ").append(log).append("\n");
            }
        }

        // Update summary label
        int total = totalPassed + totalFailed;
        if (total == 0) {
            testSummaryLabel.setText("  No tests defined");
            testSummaryLabel.setForeground(new Color(120, 120, 120));
            testSummaryLabel.setBackground(UIManager.getColor("Workspace.panelBackground"));
        } else if (totalFailed == 0) {
            testSummaryLabel.setText("  ✓ All " + totalPassed + " test" + (totalPassed != 1 ? "s" : "") + " passed");
            testSummaryLabel.setForeground(new Color(39, 174, 96));
            testSummaryLabel.setBackground(new Color(39, 174, 96, 25));
        } else {
            testSummaryLabel
                    .setText("  ✗ " + totalFailed + " of " + total + " test" + (total != 1 ? "s" : "") + " failed  |  "
                            + totalPassed + " passed");
            testSummaryLabel.setForeground(new Color(192, 57, 43));
            testSummaryLabel.setBackground(new Color(192, 57, 43, 25));
        }

        // Update console log area
        if (consoleText.length() > 0) {
            consoleLogArea.setText(consoleText.toString());
        } else {
            consoleLogArea.setText("  (no console output)");
        }

        // Update the Test Results tab title with pass/fail indicator
        int testTabIdx = tabs.indexOfComponent(testResultsPanel);
        if (testTabIdx >= 0) {
            if (total > 0) {
                String badge = totalFailed == 0 ? " (" + totalPassed + "/" + total + " ✓)"
                        : " (" + totalPassed + "/" + total + " ✗)";
                tabs.setTitleAt(testTabIdx, "Test Results" + badge);
                tabs.setForegroundAt(testTabIdx, totalFailed == 0 ? new Color(39, 174, 96) : new Color(192, 57, 43));
            } else {
                tabs.setTitleAt(testTabIdx, "Test Results");
                tabs.setForegroundAt(testTabIdx, null);
            }
        }

        // Auto-switch to Test Results tab if tests were run and there are failures
        if (totalFailed > 0) {
            if (testTabIdx >= 0)
                tabs.setSelectedIndex(testTabIdx);
        }
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
        revalidate();
        repaint();
    }

    public void reset() {
        statusLabel.setText("—");
        statusLabel.setForeground(new Color(100, 100, 100));
        timeLabel.setText("—");
        sizeLabel.setText("—");
        sslLabel.setVisible(false);
        bodyArea.setText("");
        headersModel.setRowCount(0);

        // Reset test results
        testResultsModel.setRowCount(0);
        consoleLogArea.setText("");
        testSummaryLabel.setText("  No tests run");
        testSummaryLabel.setForeground(new Color(120, 120, 120));
        testSummaryLabel.setBackground(UIManager.getColor("Workspace.panelBackground"));
        int testTabIdx = tabs.indexOfComponent(testResultsPanel);
        if (testTabIdx >= 0) {
            tabs.setTitleAt(testTabIdx, "Test Results");
            tabs.setForegroundAt(testTabIdx, null);
        }

        CardLayout cl = (CardLayout) contentCard.getLayout();
        cl.show(contentCard, "empty");
    }

    private void performSearch(boolean forward, boolean findNext) {
        if (searchField == null || bodyArea == null)
            return;
        String text = searchField.getText();
        if (text == null || text.isEmpty()) {
            bodyArea.setMarkAllHighlightColor(null);
            SearchContext context = new SearchContext();
            SearchEngine.markAll(bodyArea, context);
            matchCountLabel.setText("");
            return;
        }

        SearchContext context = new SearchContext();
        context.setSearchFor(text);
        context.setMatchCase(false);
        context.setRegularExpression(false);
        context.setSearchForward(forward);

        // Mark all matches
        SearchResult markAllResult = SearchEngine.markAll(bodyArea, context);
        int count = markAllResult.getMarkedCount();
        if (count > 0) {
            matchCountLabel.setText(count + " match" + (count > 1 ? "es" : ""));
        } else {
            matchCountLabel.setText("No matches");
        }

        if (findNext) {
            boolean found = SearchEngine.find(bodyArea, context).wasFound();
            if (!found) {
                // Wrap around
                if (forward) {
                    bodyArea.setCaretPosition(0);
                } else {
                    bodyArea.setCaretPosition(bodyArea.getDocument().getLength());
                }
                SearchEngine.find(bodyArea, context);
            }
        }
    }

    private void exportResponse() {
        if (currentResponse == null || currentResponse.getBody() == null) {
            JOptionPane.showMessageDialog(this, "No response content to save.", "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Response to File");

        // Suggest filename based on content type
        String defaultName = "response.txt";
        if (currentResponse.getHeaders() != null) {
            List<String> ct = currentResponse.getHeaders().get("content-type");
            if (ct == null)
                ct = currentResponse.getHeaders().get("Content-Type");
            if (ct != null && !ct.isEmpty()) {
                String type = ct.get(0).toLowerCase();
                if (type.contains("json")) {
                    defaultName = "response.json";
                } else if (type.contains("xml")) {
                    defaultName = "response.xml";
                } else if (type.contains("html")) {
                    defaultName = "response.html";
                }
            }
        }
        fileChooser.setSelectedFile(new File(defaultName));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                Files.writeString(fileToSave.toPath(), currentResponse.getBody(), StandardCharsets.UTF_8);
                MainFrame.showToast(this, "Response saved to " + fileToSave.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving response:\n" + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
