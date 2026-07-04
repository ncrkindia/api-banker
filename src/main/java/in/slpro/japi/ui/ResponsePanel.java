package in.slpro.japi.ui;

import in.slpro.japi.model.ResponseModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ResponsePanel extends JPanel {
    private final JLabel statusLabel;
    private final JLabel timeLabel;
    private final JLabel sizeLabel;
    private final RSyntaxTextArea bodyArea;
    private final DefaultTableModel headersModel;
    private final JTabbedPane tabs;
    private final JPanel contentCard;

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

        statusBar.add(new JLabel("Status:"));
        statusBar.add(statusLabel);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(new JLabel("Time:"));
        statusBar.add(timeLabel);
        statusBar.add(new JSeparator(JSeparator.VERTICAL));
        statusBar.add(new JLabel("Size:"));
        statusBar.add(sizeLabel);

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
        JPanel bodyToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        bodyToolbar.setBackground(UIManager.getColor("Panel.background"));
        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> {
            java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(bodyArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
        });
        JToggleButton wrapBtn = new JToggleButton("Wrap");
        wrapBtn.addActionListener(e -> bodyArea.setLineWrap(((JToggleButton) e.getSource()).isSelected()));
        bodyToolbar.add(wrapBtn);
        bodyToolbar.add(copyBtn);
        bodyPanel.add(bodyToolbar, BorderLayout.NORTH);
        bodyPanel.add(bodyScroll, BorderLayout.CENTER);
        tabs.addTab("Body", bodyPanel);

        // Headers tab
        headersModel = new DefaultTableModel(new String[]{"Header", "Value"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable headersTable = new JTable(headersModel);
        headersTable.setRowHeight(24);
        tabs.addTab("Headers", new JScrollPane(headersTable));

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
        if (response == null) return;

        int code = response.getStatusCode();
        String statusText = code + " " + response.getStatusText();
        statusLabel.setText(statusText);

        Color statusColor;
        if (code >= 200 && code < 300) statusColor = new Color(39, 174, 96);
        else if (code >= 400 && code < 500) statusColor = new Color(230, 126, 34);
        else if (code >= 500) statusColor = new Color(192, 57, 43);
        else statusColor = new Color(100, 100, 100);
        statusLabel.setForeground(statusColor);

        long ms = response.getExecutionTimeMs();
        timeLabel.setText(ms + " ms");
        timeLabel.setForeground(ms < 500 ? new Color(39, 174, 96) : ms < 2000 ? new Color(230, 126, 34) : new Color(192, 57, 43));

        long bytes = response.getSizeBytes();
        sizeLabel.setText(bytes < 1024 ? bytes + " B" : (bytes / 1024) + " KB");

        // Set body
        String body = response.getBody() != null ? response.getBody() : "";
        String contentType = "";
        if (response.getHeaders() != null) {
            List<String> ct = response.getHeaders().get("content-type");
            if (ct == null) ct = response.getHeaders().get("Content-Type");
            if (ct != null && !ct.isEmpty()) contentType = ct.get(0).toLowerCase();
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
            bodyArea.setSyntaxEditingStyle(contentType.contains("xml") ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_HTML);
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
                headersModel.addRow(new Object[]{entry.getKey(), String.join(", ", entry.getValue())});
            }
        }

        // Show response card
        CardLayout cl = (CardLayout) contentCard.getLayout();
        cl.show(contentCard, "response");
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
    }

    public void reset() {
        statusLabel.setText("—");
        statusLabel.setForeground(new Color(100, 100, 100));
        timeLabel.setText("—");
        sizeLabel.setText("—");
        bodyArea.setText("");
        headersModel.setRowCount(0);

        CardLayout cl = (CardLayout) contentCard.getLayout();
        cl.show(contentCard, "empty");
    }
}
