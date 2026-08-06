package in.slpro.apibanker.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import in.slpro.apibanker.model.KeyValueItem;
import in.slpro.apibanker.model.RequestModel;
import in.slpro.apibanker.model.VariableHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * WebSocketPanel
 *
 * <p>
 * Core functionality and implementation logic for WebSocketPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class WebSocketPanel extends JPanel {
    private final MainFrame mainFrame;
    private final RequestModel requestModel;

    private final JTextField urlField;
    private final JButton connectBtn;
    private final RSyntaxTextArea messageArea;
    private final JButton sendBtn;

    private final DefaultTableModel headersModel;
    private final JTable headersTable;

    private final DefaultListModel<MessageLogItem> logModel = new DefaultListModel<>();
    private final JList<MessageLogItem> logList;
    private final JTextField searchField;
    private final List<MessageLogItem> allMessages = new ArrayList<>();

    private WebSocket webSocket;
    private boolean isConnected = false;

    public WebSocketPanel(MainFrame mainFrame, RequestModel requestModel) {
        this.mainFrame = mainFrame;
        this.requestModel = requestModel;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));

        // 1. Connection Bar (URL + Connect Button)
        JPanel connPanel = new JPanel(new BorderLayout(8, 0));
        connPanel.setBorder(new EmptyBorder(0, 0, 8, 0));

        urlField = new JTextField(requestModel.getUrl() != null ? requestModel.getUrl() : "ws://echo.websocket.events");
        urlField.setFont(new Font("Consolas", Font.PLAIN, 13));

        connectBtn = new JButton("Connect");
        connectBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        connectBtn.addActionListener(e -> toggleConnection());

        connPanel.add(new JLabel("WebSocket URL: "), BorderLayout.WEST);
        connPanel.add(urlField, BorderLayout.CENTER);
        connPanel.add(connectBtn, BorderLayout.EAST);
        add(connPanel, BorderLayout.NORTH);

        // 2. Main Content Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        splitPane.setBorder(null);

        // Left Pane: Request Composer (Payload + Headers)
        JTabbedPane leftTabbedPane = new JTabbedPane();

        // Message Payload Tab
        JPanel payloadTab = new JPanel(new BorderLayout(0, 6));
        payloadTab.setBorder(new EmptyBorder(6, 6, 6, 6));
        messageArea = new RSyntaxTextArea();
        messageArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        messageArea.setText("{\n  \"message\": \"Hello ApiBanker WebSocket!\"\n}");
        messageArea.setFont(new Font("Consolas", Font.PLAIN, 12));

        RTextScrollPane scrollMessage = new RTextScrollPane(messageArea);
        payloadTab.add(scrollMessage, BorderLayout.CENTER);

        sendBtn = new JButton("Send Message");
        sendBtn.setEnabled(false);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendBtn.addActionListener(e -> sendMessage());
        payloadTab.add(sendBtn, BorderLayout.SOUTH);

        leftTabbedPane.addTab("Message", payloadTab);

        // Handshake Headers Tab
        JPanel headersTab = new JPanel(new BorderLayout());
        headersTab.setBorder(new EmptyBorder(6, 6, 6, 6));

        headersModel = new DefaultTableModel(new Object[] { "", "Header Key", "Header Value", "Description" }, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return true;
            }
        };
        headersTable = new JTable(headersModel);
        headersTable.getColumnModel().getColumn(0).setMaxWidth(30);
        headersTable.getColumnModel().getColumn(0).setMinWidth(30);
        headersTable.setRowHeight(24);

        // Load initial headers
        if (requestModel.getHeaders() != null) {
            for (KeyValueItem kv : requestModel.getHeaders()) {
                headersModel.addRow(new Object[] { kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription() });
            }
        }

        JPanel headersTablePanel = new JPanel(new BorderLayout());
        headersTablePanel.add(new JScrollPane(headersTable), BorderLayout.CENTER);

        JPanel headersActionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        JButton addHeaderBtn = new JButton("+ Header");
        addHeaderBtn.addActionListener(e -> headersModel.addRow(new Object[] { true, "", "", "" }));
        JButton deleteHeaderBtn = new JButton("Remove");
        deleteHeaderBtn.addActionListener(e -> {
            int row = headersTable.getSelectedRow();
            if (row >= 0)
                headersModel.removeRow(row);
        });
        headersActionPanel.add(addHeaderBtn);
        headersActionPanel.add(deleteHeaderBtn);
        headersTablePanel.add(headersActionPanel, BorderLayout.SOUTH);

        headersTab.add(headersTablePanel, BorderLayout.CENTER);
        leftTabbedPane.addTab("Handshake Headers", headersTab);

        splitPane.setLeftComponent(leftTabbedPane);

        // Right Pane: Event Stream
        JPanel streamPanel = new JPanel(new BorderLayout());
        streamPanel.setBorder(new EmptyBorder(0, 8, 0, 0));

        // Stream Header (Filter and Clear)
        JPanel streamHeader = new JPanel(new BorderLayout(8, 0));
        streamHeader.setBorder(new EmptyBorder(4, 4, 6, 4));

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search frame stream...");
        searchField.addCaretListener(e -> filterLogs());

        JButton clearLogBtn = new JButton("Clear Logs");
        clearLogBtn.addActionListener(e -> {
            allMessages.clear();
            logModel.clear();
        });

        streamHeader.add(new JLabel("Message Log: "), BorderLayout.WEST);
        streamHeader.add(searchField, BorderLayout.CENTER);
        streamHeader.add(clearLogBtn, BorderLayout.EAST);

        streamPanel.add(streamHeader, BorderLayout.NORTH);

        // Log List
        logList = new JList<>(logModel);
        logList.setCellRenderer(new MessageLogRenderer());
        logList.setFont(new Font("Consolas", Font.PLAIN, 12));

        JScrollPane logScroll = new JScrollPane(logList);
        streamPanel.add(logScroll, BorderLayout.CENTER);

        splitPane.setRightComponent(streamPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    public RequestModel getRequestModel() {
        // Collect URL and headers
        requestModel.setUrl(urlField.getText().trim());
        requestModel.setHeaders(extractHeaders());
        return requestModel;
    }

    private List<KeyValueItem> extractHeaders() {
        List<KeyValueItem> list = new ArrayList<>();
        for (int i = 0; i < headersModel.getRowCount(); i++) {
            boolean enabled = headersModel.getValueAt(i, 0) instanceof Boolean b && b;
            String key = (String) headersModel.getValueAt(i, 1);
            String value = (String) headersModel.getValueAt(i, 2);
            String desc = (String) headersModel.getValueAt(i, 3);
            KeyValueItem item = new KeyValueItem(key != null ? key : "", value != null ? value : "", enabled);
            item.setDescription(desc != null ? desc : "");
            list.add(item);
        }
        return list;
    }

    public void save() {
        getRequestModel();
        mainFrame.saveCurrentRequest(requestModel);
        MainFrame.showToast(this, "WebSocket Client \"" + requestModel.getName() + "\" saved.");
    }

    private synchronized void toggleConnection() {
        if (isConnected) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        String rawUrl = urlField.getText().trim();
        String resolvedUrl = resolveVariables(rawUrl);
        if (resolvedUrl.isEmpty()) {
            addLog(MessageLogItem.Type.ERROR, "URL is empty");
            return;
        }

        addLog(MessageLogItem.Type.CONNECT, "Connecting to " + resolvedUrl + "...");
        connectBtn.setEnabled(false);
        urlField.setEditable(false);

        try {
            HttpClient client = HttpClient.newHttpClient();
            WebSocket.Builder builder = client.newWebSocketBuilder();

            // Set custom headers
            List<KeyValueItem> headers = extractHeaders();
            for (KeyValueItem kv : headers) {
                if (kv.isEnabled() && !kv.getKey().isBlank()) {
                    builder.header(resolveVariables(kv.getKey()), resolveVariables(kv.getValue()));
                }
            }

            builder.buildAsync(URI.create(resolvedUrl), new WebSocketListener())
                    .thenAccept(ws -> {
                        SwingUtilities.invokeLater(() -> {
                            webSocket = ws;
                            isConnected = true;
                            connectBtn.setText("Disconnect");
                            connectBtn.setEnabled(true);
                            sendBtn.setEnabled(true);
                            addLog(MessageLogItem.Type.CONNECT, "Connected successfully.");
                        });
                    })
                    .exceptionally(ex -> {
                        SwingUtilities.invokeLater(() -> {
                            addLog(MessageLogItem.Type.ERROR, "Connection failed: " + ex.getCause().getMessage());
                            disconnect();
                        });
                        return null;
                    });
        } catch (Exception ex) {
            addLog(MessageLogItem.Type.ERROR, "Setup error: " + ex.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Goodbye");
            } catch (Exception ignored) {
            }
            webSocket = null;
        }
        isConnected = false;
        connectBtn.setText("Connect");
        connectBtn.setEnabled(true);
        sendBtn.setEnabled(false);
        urlField.setEditable(true);
        addLog(MessageLogItem.Type.DISCONNECT, "Disconnected from server.");
    }

    private void sendMessage() {
        if (webSocket == null || !isConnected)
            return;
        String rawPayload = messageArea.getText();
        String resolvedPayload = resolveVariables(rawPayload);

        webSocket.sendText(resolvedPayload, true)
                .thenRun(() -> SwingUtilities.invokeLater(() -> {
                    addLog(MessageLogItem.Type.SENT, resolvedPayload);
                }))
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        addLog(MessageLogItem.Type.ERROR, "Send failed: " + ex.getMessage());
                    });
                    return null;
                });
    }

    private void addLog(MessageLogItem.Type type, String text) {
        MessageLogItem item = new MessageLogItem(type, text);
        allMessages.add(item);

        String filter = searchField.getText().trim().toLowerCase();
        if (filter.isEmpty() || text.toLowerCase().contains(filter)) {
            logModel.addElement(item);
            logList.ensureIndexIsVisible(logModel.size() - 1);
        }
    }

    private void filterLogs() {
        logModel.clear();
        String filter = searchField.getText().trim().toLowerCase();
        for (MessageLogItem item : allMessages) {
            if (filter.isEmpty() || item.text.toLowerCase().contains(filter)) {
                logModel.addElement(item);
            }
        }
    }

    private String resolveVariables(String val) {
        if (val == null)
            return "";
        return VariableHelper.resolveVariablesInString(val, requestModel, mainFrame);
    }

    private class WebSocketListener implements WebSocket.Listener {
        private final StringBuilder textAccumulator = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textAccumulator.append(data);
            if (last) {
                String completeMessage = textAccumulator.toString();
                textAccumulator.setLength(0);
                SwingUtilities.invokeLater(() -> {
                    addLog(MessageLogItem.Type.RECEIVED, completeMessage);
                });
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            SwingUtilities.invokeLater(() -> {
                addLog(MessageLogItem.Type.ERROR, "Socket error: " + error.getMessage());
            });
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            SwingUtilities.invokeLater(() -> {
                addLog(MessageLogItem.Type.DISCONNECT,
                        "Closed by remote peer. Status: " + statusCode + ", Reason: " + reason);
                disconnect();
            });
            return null;
        }
    }

    private static class MessageLogItem {
        enum Type {
            CONNECT, DISCONNECT, SENT, RECEIVED, ERROR
        }

        final Type type;
        final String text;
        final String timestamp;

        MessageLogItem(Type type, String text) {
            this.type = type;
            this.text = text;
            this.timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }

    private static class MessageLogRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MessageLogItem item) {
                String prefix = "";
                Color color = Color.GRAY;

                switch (item.type) {
                    case CONNECT -> {
                        prefix = "ℹ CONNECT: ";
                        color = new Color(41, 128, 185); // Blue
                    }
                    case DISCONNECT -> {
                        prefix = "ℹ DISCONNECT: ";
                        color = new Color(127, 140, 141); // Gray
                    }
                    case SENT -> {
                        prefix = "⬆ SENT: ";
                        color = new Color(52, 152, 219); // Light Blue
                    }
                    case RECEIVED -> {
                        prefix = "⬇ RECEIVED: ";
                        color = new Color(46, 189, 89); // Green
                    }
                    case ERROR -> {
                        prefix = "⚠ ERROR: ";
                        color = new Color(231, 76, 60); // Red
                    }
                }

                String content = item.text.replace("\n", " ").replace("\r", "");
                if (content.length() > 200) {
                    content = content.substring(0, 197) + "...";
                }

                setText(String.format("[%s] %s%s", item.timestamp, prefix, content));
                setForeground(isSelected ? list.getSelectionForeground() : color);
                setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());

                setToolTipText("<html><pre>" + item.text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        + "</pre></html>");
            }
            return this;
        }
    }
}

