package in.slpro.japi.ui;

import in.slpro.japi.model.CollectionModel;
import in.slpro.japi.model.KeyValueItem;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CollectionPanel extends JPanel {
    private final MainFrame mainFrame;
    private final CollectionModel collectionModel;
    private String originalModelJson;

    // UI elements
    private JLabel titleLabel;
    private JTabbedPane tabbedPane;
    private JButton saveBtn;

    // Overview tab
    private RSyntaxTextArea readmeArea;

    // Auth tab
    private JComboBox<String> authTypeCombo;
    private CardLayout authCardLayout;
    private JPanel authCardPanel;
    private HighlightTextField bearerTokenField;
    private JTextField basicUsernameField;
    private JPasswordField basicPasswordField;
    private HighlightTextField apiKeyNameField;
    private HighlightTextField apiKeyValueField;
    private JComboBox<String> apiKeyInCombo;

    // Variables tab
    private DefaultTableModel variablesModel;

    // Pre-request Script tab
    private RSyntaxTextArea preScriptArea;

    // Tests (Post-request) Script tab
    private RSyntaxTextArea postScriptArea;

    public CollectionPanel(MainFrame mainFrame, CollectionModel collectionModel) {
        this.mainFrame = mainFrame;
        this.collectionModel = collectionModel;
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        buildUI();
        loadModel();

        Font defaultFont = UIManager.getFont("defaultFont");
        if (defaultFont != null) {
            updateFontSize(defaultFont.getSize());
        }
    }

    private void buildUI() {
        // Top: Title bar
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        headerPanel.setBackground(UIManager.getColor("Panel.background"));

        titleLabel = new JLabel("📁 " + collectionModel.getName());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        saveBtn = new JButton("Save");
        Color accent = UIManager.getColor("AccentColor");
        saveBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.setPreferredSize(new Dimension(80, 28));
        saveBtn.addActionListener(e -> save());
        headerPanel.add(saveBtn, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Nested Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // 1. Overview Tab
        JPanel overviewPanel = new JPanel(new BorderLayout());
        overviewPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        overviewPanel.setBackground(UIManager.getColor("Panel.background"));
        JLabel descLabel = new JLabel("Description / README (Markdown supported):");
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        descLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        overviewPanel.add(descLabel, BorderLayout.NORTH);

        readmeArea = new RSyntaxTextArea();
        readmeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
        readmeArea.setCodeFoldingEnabled(true);
        readmeArea.setAntiAliasingEnabled(true);
        readmeArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        overviewPanel.add(new RTextScrollPane(readmeArea), BorderLayout.CENTER);
        tabbedPane.addTab("Overview", overviewPanel);

        // 2. Authorization Tab
        JPanel authPanel = new JPanel(new BorderLayout(0, 8));
        authPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        authPanel.setBackground(UIManager.getColor("Panel.background"));
        JPanel authTypeBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authTypeBar.setBackground(UIManager.getColor("Panel.background"));
        authTypeCombo = new JComboBox<>(new String[]{"none", "bearer", "basic", "apiKey"});
        authTypeBar.add(new JLabel("Auth Type:"));
        authTypeBar.add(authTypeCombo);
        authPanel.add(authTypeBar, BorderLayout.NORTH);

        authCardLayout = new CardLayout();
        authCardPanel = new JPanel(authCardLayout);
        authCardPanel.setBackground(UIManager.getColor("Panel.background"));
        authCardPanel.add(new JPanel(), "none");

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

        authPanel.add(authCardPanel, BorderLayout.CENTER);
        authTypeCombo.addActionListener(e -> authCardLayout.show(authCardPanel, (String) authTypeCombo.getSelectedItem()));
        tabbedPane.addTab("Authorization", authPanel);

        // 3. Variables Tab
        variablesModel = buildKVModel();
        variablesModel.addTableModelListener(e -> {
            mainFrame.triggerVariableRepaintAll();
        });
        JTable variablesTable = buildKVTable(variablesModel);
        JPanel variablesPanel = buildKVPanel(variablesTable, variablesModel);
        tabbedPane.addTab("Variables", variablesPanel);

        // 4. Pre-request Script Tab
        preScriptArea = buildScriptArea();
        tabbedPane.addTab("Pre-request Script", buildScriptTab(preScriptArea, false));

        // 5. Tests Tab
        postScriptArea = buildScriptArea();
        tabbedPane.addTab("Tests", buildScriptTab(postScriptArea, true));

        add(tabbedPane, BorderLayout.CENTER);

        // Attach variable highlights and tooltips
        VariableHelper.attachToTextComponent(bearerTokenField, collectionModel, mainFrame);
        VariableHelper.attachToTextComponent(apiKeyNameField, collectionModel, mainFrame);
        VariableHelper.attachToTextComponent(apiKeyValueField, collectionModel, mainFrame);
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

    private DefaultTableModel buildKVModel() {
        return new DefaultTableModel(new Object[]{"", "Variable", "Value", "Description"}, 0) {
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
                            VariableHelper.VariableResolution res = VariableHelper.resolveVariable(varName, collectionModel, mainFrame);
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
                            VariableHelper.VariableResolution res = VariableHelper.resolveVariable(varName, collectionModel, mainFrame);
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

    private void loadModel() {
        readmeArea.setText(collectionModel.getReadme());
        authTypeCombo.setSelectedItem(collectionModel.getAuthType());
        authCardLayout.show(authCardPanel, collectionModel.getAuthType());
        bearerTokenField.setText(collectionModel.getAuthToken());
        basicUsernameField.setText(collectionModel.getAuthUsername());
        basicPasswordField.setText(collectionModel.getAuthPassword());
        apiKeyNameField.setText(collectionModel.getAuthApiKeyName());
        apiKeyValueField.setText(collectionModel.getAuthApiKeyValue());
        apiKeyInCombo.setSelectedItem(collectionModel.getAuthApiKeyIn());

        variablesModel.setRowCount(0);
        if (collectionModel.getVariables() != null) {
            for (KeyValueItem kv : collectionModel.getVariables()) {
                variablesModel.addRow(new Object[]{kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription()});
            }
        }

        preScriptArea.setText(collectionModel.getPreRequestScript());
        postScriptArea.setText(collectionModel.getPostRequestScript());
        this.originalModelJson = new com.google.gson.Gson().toJson(collectionModel);
    }

    public void save() {
        collectionModel.setReadme(readmeArea.getText());
        collectionModel.setAuthType((String) authTypeCombo.getSelectedItem());
        collectionModel.setAuthToken(bearerTokenField.getText());
        collectionModel.setAuthUsername(basicUsernameField.getText());
        collectionModel.setAuthPassword(new String(basicPasswordField.getPassword()));
        collectionModel.setAuthApiKeyName(apiKeyNameField.getText());
        collectionModel.setAuthApiKeyValue(apiKeyValueField.getText());
        collectionModel.setAuthApiKeyIn((String) apiKeyInCombo.getSelectedItem());

        List<KeyValueItem> vars = new ArrayList<>();
        for (int i = 0; i < variablesModel.getRowCount(); i++) {
            boolean enabled = variablesModel.getValueAt(i, 0) instanceof Boolean b && b;
            String key = (String) variablesModel.getValueAt(i, 1);
            String value = (String) variablesModel.getValueAt(i, 2);
            String desc = variablesModel.getColumnCount() > 3 ? (String) variablesModel.getValueAt(i, 3) : "";
            KeyValueItem kv = new KeyValueItem(key != null ? key : "", value != null ? value : "", enabled);
            kv.setDescription(desc);
            vars.add(kv);
        }
        collectionModel.setVariables(vars);

        collectionModel.setPreRequestScript(preScriptArea.getText());
        collectionModel.setPostRequestScript(postScriptArea.getText());

        mainFrame.saveCollections();
        MainFrame.showToast(this, "Collection \"" + collectionModel.getName() + "\" saved.");
        this.originalModelJson = new com.google.gson.Gson().toJson(collectionModel);
    }

    public CollectionModel collectToNewModel() {
        CollectionModel m = new CollectionModel();
        m.setId(collectionModel.getId());
        m.setName(collectionModel.getName());
        m.setRequests(collectionModel.getRequests());

        m.setReadme(readmeArea.getText());
        m.setAuthType((String) authTypeCombo.getSelectedItem());
        m.setAuthToken(bearerTokenField.getText());
        m.setAuthUsername(basicUsernameField.getText());
        m.setAuthPassword(new String(basicPasswordField.getPassword()));
        m.setAuthApiKeyName(apiKeyNameField.getText());
        m.setAuthApiKeyValue(apiKeyValueField.getText());
        m.setAuthApiKeyIn((String) apiKeyInCombo.getSelectedItem());

        List<KeyValueItem> vars = new ArrayList<>();
        for (int i = 0; i < variablesModel.getRowCount(); i++) {
            boolean enabled = variablesModel.getValueAt(i, 0) instanceof Boolean b && b;
            String key = (String) variablesModel.getValueAt(i, 1);
            String value = (String) variablesModel.getValueAt(i, 2);
            String desc = variablesModel.getColumnCount() > 3 ? (String) variablesModel.getValueAt(i, 3) : "";
            KeyValueItem kv = new KeyValueItem(key != null ? key : "", value != null ? value : "", enabled);
            kv.setDescription(desc);
            vars.add(kv);
        }
        m.setVariables(vars);

        m.setPreRequestScript(preScriptArea.getText());
        m.setPostRequestScript(postScriptArea.getText());
        return m;
    }

    public boolean hasUnsavedChanges() {
        if (originalModelJson == null) return false;
        CollectionModel current = collectToNewModel();
        String currentJson = new com.google.gson.Gson().toJson(current);
        return !originalModelJson.equals(currentJson);
    }

    public void updateFontSize(int size) {
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, size + 2));
        if (saveBtn != null) {
            int height = Math.max(28, size + 12);
            int width = Math.max(80, size * 6);
            saveBtn.setPreferredSize(new Dimension(width, height));
        }
        FontScaleHelper.scaleFonts(this, size);
        revalidate();
        repaint();
    }

    public CollectionModel getCollectionModel() {
        return collectionModel;
    }

    public void triggerVariableRepaint() {
        repaint();
    }
}
