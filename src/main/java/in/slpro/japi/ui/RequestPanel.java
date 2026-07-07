package in.slpro.japi.ui;

import in.slpro.japi.http.HttpClientWrapper;
import in.slpro.japi.model.*;
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
    private boolean isSyncing = false;
    private String originalModelJson;

    // URL bar
    private JComboBox<String> methodCombo;
    private HighlightTextField urlField;
    private JButton sendBtn;
    private JButton saveBtn;
    private JButton codeBtn;


    // Request tabs
    private JTabbedPane requestTabs;
    private DefaultTableModel paramsModel;
    private DefaultTableModel headersModel;
    private DefaultTableModel formDataModel;
    private HighlightRSyntaxTextArea bodyArea;
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
    private HighlightTextField bearerTokenField;
    private JTextField basicUsernameField;
    private JPasswordField basicPasswordField;
    private HighlightTextField apiKeyNameField;
    private HighlightTextField apiKeyValueField;
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
        setBackground(UIManager.getColor("Panel.background"));
        buildUI();
        loadModel();

        Font defaultFont = UIManager.getFont("defaultFont");
        if (defaultFont != null) {
            updateFontSize(defaultFont.getSize());
        } else {
            updateFontSize(16);
        }
    }

    private void buildUI() {
        // Top: URL bar
        JPanel urlBar = new JPanel(new BorderLayout(6, 0));
        urlBar.setBorder(new EmptyBorder(8, 10, 8, 10));
        urlBar.setBackground(UIManager.getColor("Panel.background"));

        methodCombo = new JComboBox<>(METHODS);
        methodCombo.setPreferredSize(new Dimension(90, 32));
        methodCombo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        urlField = new HighlightTextField();
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        urlField.setPreferredSize(new Dimension(0, 32));
        urlField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onUrlChanged(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onUrlChanged(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onUrlChanged(); }
            private void onUrlChanged() {
                if (isSyncing) return;
                isSyncing = true;
                try {
                    syncUrlToParams();
                } finally {
                    isSyncing = false;
                }
            }
        });

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightBtns.setOpaque(false);
        sendBtn = new JButton("Send");
        Color accent = UIManager.getColor("AccentColor");
        sendBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendBtn.setPreferredSize(new Dimension(80, 32));
        sendBtn.addActionListener(e -> sendRequest());

        codeBtn = new JButton("Code");
        codeBtn.setPreferredSize(new Dimension(70, 32));
        codeBtn.addActionListener(e -> {
            collectModel();
            new CodeSnippetDialog(mainFrame, requestModel, mainFrame.getActiveEnvironment()).setVisible(true);
        });

        saveBtn = new JButton("Save");
        saveBtn.setPreferredSize(new Dimension(70, 32));
        saveBtn.addActionListener(e -> save());
        rightBtns.add(codeBtn);
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
        paramsModel.addTableModelListener(e -> {
            if (isSyncing) return;
            isSyncing = true;
            try {
                syncParamsToUrl();
                triggerVariableRepaint();
            } finally {
                isSyncing = false;
            }
        });
        JTable paramsTable = buildKVTable(paramsModel);
        JPanel paramsPanel = buildKVPanel(paramsTable, paramsModel);
        requestTabs.addTab("Params", paramsPanel);

        // Headers tab
        headersModel = buildKVModel();
        headersModel.addTableModelListener(e -> triggerVariableRepaint());
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
        bodyArea = new HighlightRSyntaxTextArea();
        bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        bodyArea.setCodeFoldingEnabled(true);
        bodyArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        bodyArea.setAntiAliasingEnabled(true);
        bodyArea.setHighlightCurrentLine(false);
        bodyCards.add(new JPanel(), "none");
        bodyCards.add(new RTextScrollPane(bodyArea), "raw");
        formDataModel = buildKVModel();
        formDataModel.addTableModelListener(e -> triggerVariableRepaint());
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
        authPanel.setBackground(UIManager.getColor("Panel.background"));
        JPanel authTypeBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authTypeBar.setBackground(UIManager.getColor("Panel.background"));
        authTypeCombo = new JComboBox<>(new String[]{"inherit", "none", "bearer", "basic", "apiKey"});
        authTypeBar.add(new JLabel("Auth Type:"));
        authTypeBar.add(authTypeCombo);
        authPanel.add(authTypeBar, BorderLayout.NORTH);

        authCardLayout = new CardLayout();
        authCardPanel = new JPanel(authCardLayout);
        authCardPanel.setBackground(UIManager.getColor("Panel.background"));
        authCardPanel.add(new JPanel(), "none");
        
        JPanel inheritPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inheritPanel.setBackground(UIManager.getColor("Panel.background"));
        inheritPanel.add(new JLabel("<html><i>Inheriting authorization from parent collection</i></html>"));
        authCardPanel.add(inheritPanel, "inherit");

        JPanel bearerPanel = buildLabeledField("Token:", bearerTokenField = new HighlightTextField());
        authCardPanel.add(bearerPanel, "bearer");

        JPanel basicPanel = new JPanel(new GridBagLayout());
        basicPanel.setBackground(UIManager.getColor("Panel.background"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; basicPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; basicUsernameField = new JTextField(); basicPanel.add(basicUsernameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; basicPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; basicPasswordField = new JPasswordField(); basicPanel.add(basicPasswordField, gbc);
        authCardPanel.add(basicPanel, "basic");

        JPanel apiKeyPanel = new JPanel(new GridBagLayout());
        apiKeyPanel.setBackground(UIManager.getColor("Panel.background"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; apiKeyPanel.add(new JLabel("Key Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; apiKeyNameField = new HighlightTextField(); apiKeyPanel.add(apiKeyNameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; apiKeyPanel.add(new JLabel("Key Value:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; apiKeyValueField = new HighlightTextField(); apiKeyPanel.add(apiKeyValueField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; apiKeyPanel.add(new JLabel("Add to:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0; apiKeyInCombo = new JComboBox<>(new String[]{"header", "query"}); apiKeyPanel.add(apiKeyInCombo, gbc);
        authCardPanel.add(apiKeyPanel, "apiKey");

        authTypeCombo.addActionListener(e -> authCardLayout.show(authCardPanel, (String) authTypeCombo.getSelectedItem()));
        authPanel.add(authCardPanel, BorderLayout.CENTER);
        requestTabs.addTab("Auth", authPanel);

        // Scripts
        preScriptArea = buildScriptArea();
        postScriptArea = buildScriptArea();
        requestTabs.addTab("Pre-request Script", buildScriptTab(preScriptArea, false));
        requestTabs.addTab("Tests", buildScriptTab(postScriptArea, true));

        // Response
        responsePanel = new ResponsePanel();

        // Split
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, requestTabs, responsePanel);
        splitPane.setResizeWeight(0.45);
        splitPane.setDividerSize(6);
        add(splitPane, BorderLayout.CENTER);

        // Attach variable highlights and tooltips
        VariableHelper.attachToTextComponent(urlField, requestModel, mainFrame);
        VariableHelper.attachToTextComponent(bearerTokenField, requestModel, mainFrame);
        VariableHelper.attachToTextComponent(apiKeyNameField, requestModel, mainFrame);
        VariableHelper.attachToTextComponent(apiKeyValueField, requestModel, mainFrame);
        VariableHelper.attachToTextComponent(bodyArea, requestModel, mainFrame);
    }

    private JPanel buildLabeledField(String label, JTextField field) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIManager.getColor("Panel.background"));
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
        area.setHighlightCurrentLine(false);
        return area;
    }

    /**
     * Builds a script editing tab with the editor on the left and a clickable
     * snippet reference sidebar on the right (similar to Postman's snippet panel).
     */
    private JPanel buildScriptTab(RSyntaxTextArea scriptArea, boolean isTestScript) {
        JPanel panel = new JPanel(new BorderLayout());

        // Snippet sidebar
        JPanel snippetPanel = new JPanel();
        snippetPanel.setLayout(new BoxLayout(snippetPanel, BoxLayout.Y_AXIS));
        snippetPanel.setBackground(UIManager.getColor("Workspace.panelBackground"));
        snippetPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, UIManager.getColor("Workspace.borderColor")),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        JLabel snippetTitle = new JLabel("Snippets");
        snippetTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        snippetTitle.setForeground(new Color(120, 120, 120));
        snippetTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        snippetPanel.add(snippetTitle);
        snippetPanel.add(Box.createVerticalStrut(6));

        // Common snippets
        addSnippetButton(snippetPanel, scriptArea, "Get env variable",
                "let val = japi.environment.get(\"variable_name\");\nconsole.log(val);\n");
        addSnippetButton(snippetPanel, scriptArea, "Set env variable",
                "japi.environment.set(\"variable_name\", \"value\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Console log",
                "console.log(\"Hello from script!\");\n");

        if (isTestScript) {
            snippetPanel.add(Box.createVerticalStrut(8));
            JLabel testTitle = new JLabel("Test Assertions");
            testTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
            testTitle.setForeground(new Color(120, 120, 120));
            testTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            snippetPanel.add(testTitle);
            snippetPanel.add(Box.createVerticalStrut(4));

            addSnippetButton(snippetPanel, scriptArea, "Status code is 200",
                    "japi.test(\"Status code is 200\", function() {\n    japi.expect(japi.response.code).to.equal(200);\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "Response time < 500ms",
                    "japi.test(\"Response time is acceptable\", function() {\n    japi.expect(japi.response.responseTime).to.be.below(500);\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "Body contains string",
                    "japi.test(\"Body contains expected text\", function() {\n    japi.expect(japi.response.text()).to.include(\"expected\");\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "JSON value check",
                    "japi.test(\"JSON value check\", function() {\n    var data = japi.response.json();\n    japi.expect(data.key).to.equal(\"expected_value\");\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "JSON has property",
                    "japi.test(\"Has expected property\", function() {\n    var data = japi.response.json();\n    japi.expect(data).to.have.property(\"key\");\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "Response header check",
                    "japi.test(\"Content-Type is JSON\", function() {\n    var ct = japi.response.headers.get(\"content-type\");\n    japi.expect(ct).to.include(\"application/json\");\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "Save response to env",
                    "japi.test(\"Save token to env\", function() {\n    var data = japi.response.json();\n    japi.environment.set(\"auth_token\", data.token);\n});\n");
        } else {
            snippetPanel.add(Box.createVerticalStrut(8));
            addSnippetButton(snippetPanel, scriptArea, "Set request header",
                    "// Headers are set in the Headers tab.\n// Use pre-request to compute dynamic values:\nvar timestamp = new Date().getTime();\njapi.environment.set(\"timestamp\", \"\" + timestamp);\n");
            addSnippetButton(snippetPanel, scriptArea, "Generate random data",
                    "var rand = Math.floor(Math.random() * 10000);\njapi.environment.set(\"random_id\", \"\" + rand);\nconsole.log(\"Generated ID: \" + rand);\n");
        }

        snippetPanel.add(Box.createVerticalGlue());

        JScrollPane snippetScroll = new JScrollPane(snippetPanel);
        snippetScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        snippetScroll.setPreferredSize(new Dimension(180, 0));
        snippetScroll.setBorder(null);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new RTextScrollPane(scriptArea), snippetScroll);
        split.setResizeWeight(1.0);
        split.setDividerSize(4);
        panel.add(split, BorderLayout.CENTER);

        return panel;
    }

    private void addSnippetButton(JPanel parent, RSyntaxTextArea targetArea, String label, String snippet) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setForeground(UIManager.getColor("AccentColor") != null ? UIManager.getColor("AccentColor") : new Color(52, 152, 219));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(new Color(52, 152, 219, 25));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setContentAreaFilled(false);
            }
        });
        btn.addActionListener(e -> {
            int pos = targetArea.getCaretPosition();
            try {
                targetArea.getDocument().insertString(pos, snippet, null);
            } catch (Exception ignored) {}
        });
        parent.add(btn);
        parent.add(Box.createVerticalStrut(2));
    }

    private DefaultTableModel buildKVModel() {
        return new DefaultTableModel(new Object[]{"", "Key", "Value", "Description"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
    }

    private JTable buildKVTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    Object val = getValueAt(row, col);
                    if (val instanceof String s) {
                        java.util.regex.Matcher matcher = VariableHelper.VAR_PATTERN.matcher(s);
                        StringBuilder sb = new StringBuilder("<html><body style='font-family: sans-serif; padding: 2px;'>");
                        boolean found = false;
                        while (matcher.find()) {
                            String varName = matcher.group(1).trim();
                            VariableHelper.VariableResolution res = VariableHelper.resolveVariable(varName, requestModel, mainFrame);
                            if (res.resolved) {
                                sb.append(String.format("<b>Variable:</b> %s<br/><b>Source:</b> %s<br/><b>Current Value:</b> <font color='green'>%s</font><br/><br/>",
                                        res.name, res.source, res.value));
                            } else {
                                sb.append(String.format("<b>Variable:</b> %s<br/><b>Source:</b> <font color='red'>Unresolved</font><br/><br/>",
                                        res.name));
                            }
                            found = true;
                        }
                        if (found) {
                            if (sb.length() > 8) {
                                sb.setLength(sb.length() - 9);
                            }
                            sb.append("</body></html>");
                            return sb.toString();
                        }
                    }
                }
                return super.getToolTipText(e);
            }
        };

        javax.swing.table.TableCellRenderer defaultRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (c instanceof JLabel label && value instanceof String s) {
                    if (s.contains("{{") && s.contains("}}")) {
                        java.util.regex.Matcher matcher = VariableHelper.VAR_PATTERN.matcher(s);
                        StringBuffer sb = new StringBuffer("<html>");
                        while (matcher.find()) {
                            String varName = matcher.group(1).trim();
                            VariableHelper.VariableResolution res = VariableHelper.resolveVariable(varName, requestModel, mainFrame);
                            String colorStr;
                            if (res.resolved) {
                                Color colVal = res.isEnv ? VariableHelper.getEnvColor() : VariableHelper.getCollectionColor();
                                colorStr = String.format("#%02x%02x%02x", colVal.getRed(), colVal.getGreen(), colVal.getBlue());
                            } else {
                                Color colVal = VariableHelper.getUnresolvedColor();
                                colorStr = String.format("#%02x%02x%02x", colVal.getRed(), colVal.getGreen(), colVal.getBlue());
                            }
                            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                                    "<span style='color: " + colorStr + "; font-weight: bold;'>{{" + varName + "}}</span>"));
                        }
                        matcher.appendTail(sb);
                        sb.append("</html>");
                        label.setText(sb.toString());
                    }
                }
                return c;
            }
        };

        table.getColumnModel().getColumn(0).setMaxWidth(30);
        table.getColumnModel().getColumn(0).setMinWidth(30);
        table.getColumnModel().getColumn(1).setCellRenderer(defaultRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(defaultRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(defaultRenderer);
        table.setRowHeight(24);
        return table;
    }

    private JPanel buildKVPanel(JTable table, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIManager.getColor("Panel.background"));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        btns.setBackground(UIManager.getColor("Panel.background"));
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
        isSyncing = true;
        try {
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

            String at = requestModel.getAuthType() != null ? requestModel.getAuthType() : "inherit";
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
        } finally {
            isSyncing = false;
        }
        this.originalModelJson = new com.google.gson.Gson().toJson(requestModel);
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

        SwingWorker<HttpClientWrapper.ExecutionResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HttpClientWrapper.ExecutionResult doInBackground() {
                HttpClientWrapper client = new HttpClientWrapper();
                return client.executeWithScripts(snapshot, env);
            }

            @Override
            protected void done() {
                try {
                    HttpClientWrapper.ExecutionResult execResult = get();
                    ResponseModel response = execResult.getResponse();
                    responsePanel.showResponse(response);
                    responsePanel.showTestResults(execResult.getPreRequestResult(), execResult.getTestResult());
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

    public void save() {
        collectModel();
        mainFrame.saveCurrentRequest(requestModel);
        this.originalModelJson = new com.google.gson.Gson().toJson(requestModel);
        MainFrame.showToast(this, "Request \"" + requestModel.getName() + "\" saved.");
    }

    public RequestModel collectToNewModel() {
        RequestModel m = new RequestModel();
        m.setId(requestModel.getId());
        m.setName(requestModel.getName());
        m.setType(requestModel.getType());
        m.setTimestamp(requestModel.getTimestamp());
        m.setResponseStatus(requestModel.getResponseStatus());
        m.setActualUrl(requestModel.getActualUrl());
        m.setComparatorTextA(requestModel.getComparatorTextA());
        m.setComparatorTextB(requestModel.getComparatorTextB());
        m.setComparatorMode(requestModel.getComparatorMode());

        m.setMethod((String) methodCombo.getSelectedItem());
        m.setUrl(urlField.getText().trim());
        m.setBodyType((String) bodyTypeCombo.getSelectedItem());
        m.setBodyRawType((String) rawTypeCombo.getSelectedItem());
        m.setBodyRawContent(bodyArea.getText());
        m.setAuthType((String) authTypeCombo.getSelectedItem());
        m.setAuthToken(bearerTokenField.getText());
        m.setAuthUsername(basicUsernameField.getText());
        m.setAuthPassword(new String(basicPasswordField.getPassword()));
        m.setAuthApiKeyName(apiKeyNameField.getText());
        m.setAuthApiKeyValue(apiKeyValueField.getText());
        m.setAuthApiKeyIn((String) apiKeyInCombo.getSelectedItem());
        m.setPreRequestScript(preScriptArea.getText());
        m.setPostRequestScript(postScriptArea.getText());

        m.setParams(extractKV(paramsModel));
        m.setHeaders(extractKV(headersModel));
        m.setFormData(extractKV(formDataModel));
        return m;
    }

    public boolean hasUnsavedChanges() {
        if (originalModelJson == null) return false;
        RequestModel current = collectToNewModel();
        String currentJson = new com.google.gson.Gson().toJson(current);
        return !originalModelJson.equals(currentJson);
    }

    public RequestModel getRequestModel() {
        collectModel();
        return requestModel;
    }

    public void updateFontSize(int size) {
        int height = Math.max(32, size + 16);
        if (methodCombo != null) methodCombo.setPreferredSize(new Dimension(Math.max(90, size * 7), height));
        if (urlField != null) urlField.setPreferredSize(new Dimension(0, height));
        if (sendBtn != null) sendBtn.setPreferredSize(new Dimension(Math.max(80, size * 6), height));
        if (saveBtn != null) saveBtn.setPreferredSize(new Dimension(Math.max(70, size * 5), height));
        if (codeBtn != null) codeBtn.setPreferredSize(new Dimension(Math.max(70, size * 5), height));
        FontScaleHelper.scaleFonts(this, size);
        revalidate();
        repaint();
    }

    public void setRequestModel(RequestModel model) {
        this.requestModel = model;
        loadModel();
    }

    private static class Param {
        String key;
        String value;
        Param(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private List<Param> parseUrlParams(String url) {
        List<Param> list = new ArrayList<>();
        if (url == null || url.trim().isEmpty()) return list;
        int qIdx = url.indexOf('?');
        if (qIdx < 0) return list;
        String queryStr = url.substring(qIdx + 1);
        if (queryStr.isEmpty()) return list;
        String[] pairs = queryStr.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            int eqIdx = pair.indexOf('=');
            String key;
            String val = "";
            if (eqIdx >= 0) {
                key = pair.substring(0, eqIdx);
                val = pair.substring(eqIdx + 1);
            } else {
                key = pair;
            }
            list.add(new Param(decodeUrlComponent(key), decodeUrlComponent(val)));
        }
        return list;
    }

    private String decodeUrlComponent(String s) {
        try {
            return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String encodeUrlComponent(String s) {
        if (s == null) return "";
        try {
            String encoded = java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
            return encoded.replace("+", "%20")
                          .replace("%7B%7B", "{{")
                          .replace("%7D%7D", "}}");
        } catch (Exception e) {
            return s;
        }
    }

    private void syncUrlToParams() {
        String url = urlField.getText();
        List<Param> parsed = parseUrlParams(url);

        List<KeyValueItem> current = new ArrayList<>();
        for (int i = 0; i < paramsModel.getRowCount(); i++) {
            boolean enabled = paramsModel.getValueAt(i, 0) instanceof Boolean b && b;
            String key = (String) paramsModel.getValueAt(i, 1);
            String value = (String) paramsModel.getValueAt(i, 2);
            String desc = paramsModel.getColumnCount() > 3 ? (String) paramsModel.getValueAt(i, 3) : "";
            KeyValueItem itemModel = new KeyValueItem(key != null ? key : "", value != null ? value : "", enabled);
            itemModel.setDescription(desc);
            current.add(itemModel);
        }

        List<KeyValueItem> updated = new ArrayList<>();
        boolean[] parsedUsed = new boolean[parsed.size()];

        for (KeyValueItem item : current) {
            if (!item.isEnabled()) {
                updated.add(item);
            } else {
                int matchIdx = -1;
                for (int j = 0; j < parsed.size(); j++) {
                    if (!parsedUsed[j] && parsed.get(j).key.equals(item.getKey())) {
                        matchIdx = j;
                        break;
                    }
                }

                if (matchIdx >= 0) {
                    item.setValue(parsed.get(matchIdx).value);
                    updated.add(item);
                    parsedUsed[matchIdx] = true;
                }
            }
        }

        for (int j = 0; j < parsed.size(); j++) {
            if (!parsedUsed[j]) {
                KeyValueItem newItem = new KeyValueItem(parsed.get(j).key, parsed.get(j).value, true);
                newItem.setDescription("");
                updated.add(newItem);
            }
        }

        paramsModel.setRowCount(0);
        for (KeyValueItem item : updated) {
            paramsModel.addRow(new Object[]{item.isEnabled(), item.getKey(), item.getValue(), item.getDescription()});
        }
    }

    private void syncParamsToUrl() {
        String url = urlField.getText();
        String baseUrl = url;
        int qIdx = url.indexOf('?');
        if (qIdx >= 0) {
            baseUrl = url.substring(0, qIdx);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramsModel.getRowCount(); i++) {
            boolean enabled = paramsModel.getValueAt(i, 0) instanceof Boolean b && b;
            if (enabled) {
                String key = (String) paramsModel.getValueAt(i, 1);
                String val = (String) paramsModel.getValueAt(i, 2);
                if (key != null && !key.trim().isEmpty()) {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(encodeUrlComponent(key))
                      .append("=")
                      .append(encodeUrlComponent(val != null ? val : ""));
                }
            }
        }

        String newUrl = baseUrl;
        if (sb.length() > 0) {
            newUrl = baseUrl + "?" + sb.toString();
        } else if (qIdx >= 0 && url.endsWith("?")) {
            newUrl = baseUrl + "?";
        }

        urlField.setText(newUrl);
    }

    public void triggerVariableRepaint() {
        repaint();
    }
}
