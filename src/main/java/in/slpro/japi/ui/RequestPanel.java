package in.slpro.japi.ui;

import in.slpro.japi.http.HttpClientWrapper;
import in.slpro.japi.model.*;
import in.slpro.japi.storage.StorageManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RequestPanel extends JPanel {
    private final MainFrame mainFrame;
    private RequestModel requestModel;

    // URL bar
    private JComboBox<String> methodCombo;
    private JTextField urlField;
    private JButton sendBtn;
    private JButton saveBtn;

    // Tab editing
    private JTextField tabNameField;

    // Request tabs
    private JTabbedPane requestTabs;
    private DefaultTableModel paramsModel;
    private DefaultTableModel headersModel;
    private DefaultTableModel formDataModel;
    private RSyntaxTextArea bodyArea;
    private JComboBox<String> bodyTypeCombo;
    private JComboBox<String> rawTypeCombo;
    private JPanel bodyPanel;
    private CardLayout bodyCardLayout;
    private JPanel authPanel;
    private JComboBox<String> authTypeCombo;
    private CardLayout authCardLayout;
    private JPanel authCardPanel;
    private RSyntaxTextArea preScriptArea;
    private RSyntaxTextArea postScriptArea;

    // Auth fields
    private JTextField bearerTokenField;
    private JTextField basicUsernameField;
    private JPasswordField basicPasswordField;
    private JTextField apiKeyNameField;
    private JTextField apiKeyValueField;
    private JComboBox<String> apiKeyInCombo;

    // Response
    private ResponsePanel responsePanel;

    // Split
    private JSplitPane splitPane;

    private static final String[] METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"};

    public RequestPanel(MainFrame mainFrame, RequestModel requestModel) {
        this.mainFrame = mainFrame;
        this.requestModel = requestModel;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        buildUI();
        loadModel();
    }

    private void buildUI() {
        // Top: URL bar
        JPanel urlBar = new JPanel(new BorderLayout(6, 0));
        urlBar.setBorder(new EmptyBorder(8, 10, 8, 10));
        urlBar.setBackground(Color.WHITE);

        methodCombo = new JComboBox<>(METHODS);
        methodCombo.setPreferredSize(new Dimension(90, 32));
        methodCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        urlField = new JTextField();
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        urlField.setPreferredSize(new Dimension(0, 32));

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightBtns.setOpaque(false);
        sendBtn = new JButton("Send");
        sendBtn.setBackground(new Color(52, 152, 219));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendBtn.setPreferredSize(new Dimension(80, 32));
        sendBtn.addActionListener(e -> sendRequest());

        saveBtn = new JButton("Save");
        saveBtn.setPreferredSize(new Dimension(70, 32));
        saveBtn.addActionListener(e -> save());
        rightBtns.add(saveBtn);
        rightBtns.add(sendBtn);

        urlBar.add(methodCombo, BorderLayout.WEST);
        urlBar.add(urlField, BorderLayout.CENTER);
        urlBar.add(rightBtns, BorderLayout.EAST);

        add(urlBar, BorderLayout.NORTH);

        // Request tabs
        requestTabs = new JTabbedPane();
        requestTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // Params tab
        paramsModel = buildKVModel();
        JTable paramsTable = buildKVTable(paramsModel);
        JPanel paramsPanel = buildKVPanel(paramsTable, paramsModel);
        requestTabs.addTab("Params", paramsPanel);

        // Headers tab
        headersModel = buildKVModel();
        JTable headersTable = buildKVTable(headersModel);
        JPanel headersPanel = buildKVPanel(headersTable, headersModel);
        requestTabs.addTab("Headers", headersPanel);

        // Body tab
        bodyPanel = new JPanel(new BorderLayout());
        bodyCardLayout = new CardLayout();
        JPanel bodyTypeBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bodyTypeCombo = new JComboBox<>(new String[]{"none", "raw", "form-data", "x-www-form-urlencoded"});
        bodyTypeCombo.addActionListener(e -> updateBodyCard());
        bodyTypeBar.add(new JLabel("Body:"));
        bodyTypeBar.add(bodyTypeCombo);
        rawTypeCombo = new JComboBox<>(new String[]{"JSON", "Text", "HTML", "XML", "JavaScript"});
        rawTypeCombo.setVisible(false);
        bodyTypeBar.add(rawTypeCombo);
        bodyPanel.add(bodyTypeBar, BorderLayout.NORTH);

        JPanel bodyCards = new JPanel(bodyCardLayout);
        bodyArea = new RSyntaxTextArea();
        bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        bodyArea.setCodeFoldingEnabled(true);
        bodyArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        bodyArea.setAntiAliasingEnabled(true);
        bodyCards.add(new JPanel(), "none");
        bodyCards.add(new RTextScrollPane(bodyArea), "raw");
        formDataModel = buildKVModel();
        bodyCards.add(buildKVPanel(buildKVTable(formDataModel), formDataModel), "form");
        bodyPanel.add(bodyCards, BorderLayout.CENTER);
        bodyTypeCombo.addActionListener(e -> {
            String sel = (String) bodyTypeCombo.getSelectedItem();
            rawTypeCombo.setVisible("raw".equals(sel));
            bodyCardLayout.show(bodyCards, "none".equals(sel) ? "none" : "raw".equals(sel) ? "raw" : "form");
            if ("raw".equals(sel)) {
                String rawType = (String) rawTypeCombo.getSelectedItem();
                bodyArea.setSyntaxEditingStyle("JSON".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_JSON :
                        "HTML".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_HTML :
                        "XML".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
            }
        });
        rawTypeCombo.addActionListener(e -> {
            String rawType = (String) rawTypeCombo.getSelectedItem();
            bodyArea.setSyntaxEditingStyle("JSON".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_JSON :
                    "HTML".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_HTML :
                    "XML".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
        });
        requestTabs.addTab("Body", bodyPanel);

        // Auth tab
        authPanel = new JPanel(new BorderLayout(0, 8));
        authPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        authPanel.setBackground(Color.WHITE);
        JPanel authTypeBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authTypeBar.setBackground(Color.WHITE);
        authTypeCombo = new JComboBox<>(new String[]{"none", "bearer", "basic", "apiKey"});
        authTypeBar.add(new JLabel("Auth Type:"));
        authTypeBar.add(authTypeCombo);
        authPanel.add(authTypeBar, BorderLayout.NORTH);

        authCardLayout = new CardLayout();
        authCardPanel = new JPanel(authCardLayout);
        authCardPanel.setBackground(Color.WHITE);
        authCardPanel.add(new JPanel(), "none");

        JPanel bearerPanel = buildLabeledField("Token:", bearerTokenField = new JTextField());
        authCardPanel.add(bearerPanel, "bearer");

        JPanel basicPanel = new JPanel(new GridBagLayout());
        basicPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; basicPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; basicUsernameField = new JTextField(); basicPanel.add(basicUsernameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; basicPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; basicPasswordField = new JPasswordField(); basicPanel.add(basicPasswordField, gbc);
        authCardPanel.add(basicPanel, "basic");

        JPanel apiKeyPanel = new JPanel(new GridBagLayout());
        apiKeyPanel.setBackground(Color.WHITE);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; apiKeyPanel.add(new JLabel("Key Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; apiKeyNameField = new JTextField(); apiKeyPanel.add(apiKeyNameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; apiKeyPanel.add(new JLabel("Key Value:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; apiKeyValueField = new JTextField(); apiKeyPanel.add(apiKeyValueField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; apiKeyPanel.add(new JLabel("Add to:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0; apiKeyInCombo = new JComboBox<>(new String[]{"header", "query"}); apiKeyPanel.add(apiKeyInCombo, gbc);
        authCardPanel.add(apiKeyPanel, "apiKey");

        authTypeCombo.addActionListener(e -> authCardLayout.show(authCardPanel, (String) authTypeCombo.getSelectedItem()));
        authPanel.add(authCardPanel, BorderLayout.CENTER);
        requestTabs.addTab("Auth", authPanel);

        // Scripts
        preScriptArea = buildScriptArea();
        postScriptArea = buildScriptArea();
        requestTabs.addTab("Pre-request Script", new RTextScrollPane(preScriptArea));
        requestTabs.addTab("Tests", new RTextScrollPane(postScriptArea));

        // Response
        responsePanel = new ResponsePanel();

        // Split
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, requestTabs, responsePanel);
        splitPane.setResizeWeight(0.45);
        splitPane.setDividerSize(6);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel buildLabeledField(String label, JTextField field) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.weightx = 0; panel.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1; panel.add(field, gbc);
        return panel;
    }

    private RSyntaxTextArea buildScriptArea() {
        RSyntaxTextArea area = new RSyntaxTextArea();
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        return area;
    }

    private DefaultTableModel buildKVModel() {
        return new DefaultTableModel(new Object[]{"", "Key", "Value", "Description"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
    }

    private JTable buildKVTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.getColumnModel().getColumn(0).setMaxWidth(30);
        table.getColumnModel().getColumn(0).setMinWidth(30);
        table.setRowHeight(24);
        return table;
    }

    private JPanel buildKVPanel(JTable table, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        btns.setBackground(Color.WHITE);
        JButton addBtn = new JButton("+ Add Row");
        JButton delBtn = new JButton("Delete");
        addBtn.addActionListener(e -> model.addRow(new Object[]{true, "", "", ""}));
        delBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) model.removeRow(r);
        });
        btns.add(addBtn);
        btns.add(delBtn);
        panel.add(btns, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void updateBodyCard() {
        // handled inline in action listeners
    }

    private void loadModel() {
        methodCombo.setSelectedItem(requestModel.getMethod());
        urlField.setText(requestModel.getUrl());

        paramsModel.setRowCount(0);
        if (requestModel.getParams() != null) {
            for (KeyValueItem kv : requestModel.getParams()) {
                paramsModel.addRow(new Object[]{kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription()});
            }
        }

        headersModel.setRowCount(0);
        if (requestModel.getHeaders() != null) {
            for (KeyValueItem kv : requestModel.getHeaders()) {
                headersModel.addRow(new Object[]{kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription()});
            }
        }

        String bt = requestModel.getBodyType() != null ? requestModel.getBodyType() : "none";
        bodyTypeCombo.setSelectedItem(bt);
        rawTypeCombo.setSelectedItem(requestModel.getBodyRawType() != null ? requestModel.getBodyRawType() : "JSON");
        bodyArea.setText(requestModel.getBodyRawContent() != null ? requestModel.getBodyRawContent() : "");

        formDataModel.setRowCount(0);
        if (requestModel.getFormData() != null) {
            for (KeyValueItem kv : requestModel.getFormData()) {
                formDataModel.addRow(new Object[]{kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription()});
            }
        }

        String at = requestModel.getAuthType() != null ? requestModel.getAuthType() : "none";
        authTypeCombo.setSelectedItem(at);
        authCardLayout.show(authCardPanel, at);
        bearerTokenField.setText(requestModel.getAuthToken() != null ? requestModel.getAuthToken() : "");
        basicUsernameField.setText(requestModel.getAuthUsername() != null ? requestModel.getAuthUsername() : "");
        basicPasswordField.setText(requestModel.getAuthPassword() != null ? requestModel.getAuthPassword() : "");
        apiKeyNameField.setText(requestModel.getAuthApiKeyName() != null ? requestModel.getAuthApiKeyName() : "");
        apiKeyValueField.setText(requestModel.getAuthApiKeyValue() != null ? requestModel.getAuthApiKeyValue() : "");
        apiKeyInCombo.setSelectedItem(requestModel.getAuthApiKeyIn() != null ? requestModel.getAuthApiKeyIn() : "header");

        preScriptArea.setText(requestModel.getPreRequestScript() != null ? requestModel.getPreRequestScript() : "");
        postScriptArea.setText(requestModel.getPostRequestScript() != null ? requestModel.getPostRequestScript() : "");
    }

    private void collectModel() {
        requestModel.setMethod((String) methodCombo.getSelectedItem());
        requestModel.setUrl(urlField.getText().trim());
        requestModel.setBodyType((String) bodyTypeCombo.getSelectedItem());
        requestModel.setBodyRawType((String) rawTypeCombo.getSelectedItem());
        requestModel.setBodyRawContent(bodyArea.getText());
        requestModel.setAuthType((String) authTypeCombo.getSelectedItem());
        requestModel.setAuthToken(bearerTokenField.getText());
        requestModel.setAuthUsername(basicUsernameField.getText());
        requestModel.setAuthPassword(new String(basicPasswordField.getPassword()));
        requestModel.setAuthApiKeyName(apiKeyNameField.getText());
        requestModel.setAuthApiKeyValue(apiKeyValueField.getText());
        requestModel.setAuthApiKeyIn((String) apiKeyInCombo.getSelectedItem());
        requestModel.setPreRequestScript(preScriptArea.getText());
        requestModel.setPostRequestScript(postScriptArea.getText());

        requestModel.setParams(extractKV(paramsModel));
        requestModel.setHeaders(extractKV(headersModel));
        requestModel.setFormData(extractKV(formDataModel));
    }

    private List<KeyValueItem> extractKV(DefaultTableModel model) {
        List<KeyValueItem> list = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            boolean enabled = model.getValueAt(i, 0) instanceof Boolean b && b;
            String key = (String) model.getValueAt(i, 1);
            String value = (String) model.getValueAt(i, 2);
            String desc = model.getColumnCount() > 3 ? (String) model.getValueAt(i, 3) : "";
            KeyValueItem kv = new KeyValueItem(key != null ? key : "", value != null ? value : "", enabled);
            kv.setDescription(desc);
            list.add(kv);
        }
        return list;
    }

    private void sendRequest() {
        collectModel();
        sendBtn.setEnabled(false);
        sendBtn.setText("Sending...");
        responsePanel.reset();

        RequestModel snapshot = requestModel;
        EnvironmentModel env = mainFrame.getActiveEnvironment();

        SwingWorker<ResponseModel, Void> worker = new SwingWorker<>() {
            @Override
            protected ResponseModel doInBackground() {
                HttpClientWrapper client = new HttpClientWrapper();
                return client.execute(snapshot, env);
            }

            @Override
            protected void done() {
                try {
                    ResponseModel response = get();
                    responsePanel.showResponse(response);
                    // Update history metadata
                    snapshot.setTimestamp(System.currentTimeMillis());
                    snapshot.setResponseStatus(response.getStatusCode());
                    snapshot.setActualUrl(response.getActualUrl());
                    mainFrame.addRequestToHistory(snapshot);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(RequestPanel.this,
                            "Request failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    sendBtn.setEnabled(true);
                    sendBtn.setText("Send");
                }
            }
        };
        worker.execute();
    }

    private void save() {
        collectModel();
        mainFrame.saveCurrentRequest(requestModel);
    }

    public RequestModel getRequestModel() {
        collectModel();
        return requestModel;
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
    }

    public void setRequestModel(RequestModel model) {
        this.requestModel = model;
        loadModel();
    }
}
