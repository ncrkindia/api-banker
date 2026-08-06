package in.slpro.japi.ui;

import in.slpro.japi.model.CollectionModel;
import in.slpro.japi.model.KeyValueItem;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.node.Node;

/**
 * CollectionPanel
 *
 * <p>
 * Core functionality and implementation logic for CollectionPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class CollectionPanel extends JPanel {
    private final MainFrame mainFrame;
    private final CollectionModel collectionModel;
    private String originalModelJson;
    private int currentGlobalFontSize = 13;

    // UI elements
    private JLabel titleLabel;
    private JTabbedPane tabbedPane;
    private JButton saveBtn;

    // Overview tab
    private RSyntaxTextArea readmeArea;
    private CardLayout overviewCardLayout;
    private JPanel overviewCardPanel;
    private JEditorPane htmlPane;
    private JComboBox<String> mdFontCombo;
    private JComboBox<Integer> mdFontSizeCombo;
    private JComboBox<String> mdHeaderCombo;
    private JPanel formatToolsPanel;
    private JToggleButton readBtn;

    // Auth tab
    private JComboBox<String> authTypeCombo;
    private CardLayout authCardLayout;
    private JPanel authCardPanel;
    private HighlightTextField bearerTokenField;
    private JComboBox<String> oauth2GrantTypeCombo;
    private HighlightTextField oauth2CallbackUrlField;
    private HighlightTextField oauth2AuthUrlField;
    private HighlightTextField oauth2AccessTokenUrlField;
    private HighlightTextField oauth2ClientIdField;
    private HighlightTextField oauth2ClientSecretField;
    private HighlightTextField oauth2ScopeField;
    private HighlightTextField oauth2StateField;
    private HighlightTextField oauth2UsernameField;
    private JPasswordField oauth2PasswordField;
    private JComboBox<String> oauth2ClientAuthCombo;
    public HighlightTextField oauth2AccessTokenField;
    private JButton oauth2GetTokenBtn;
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
    private JComboBox<String> sslVerifyCombo;
    private JComboBox<String> redirectVerifyCombo;

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

        titleLabel = new JLabel(collectionModel.getName());
        titleLabel.setIcon(UIManager.getIcon("Tree.closedIcon"));
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

        // Header containing Mode Toggles and formatting buttons
        JPanel toolbarPanel = new JPanel(new BorderLayout(10, 0));
        toolbarPanel.setBackground(UIManager.getColor("Panel.background"));
        toolbarPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        readBtn = new JToggleButton("Read");
        readBtn.setIcon(VectorIcon.getReadIcon(14));
        JToggleButton editBtn = new JToggleButton("Edit");
        editBtn.setIcon(VectorIcon.getEditIcon(14));
        readBtn.putClientProperty("JButton.buttonType", "segmented");
        editBtn.putClientProperty("JButton.buttonType", "segmented");
        readBtn.putClientProperty("JButton.segmentPosition", "first");
        editBtn.putClientProperty("JButton.segmentPosition", "last");
        
        ButtonGroup group = new ButtonGroup();
        group.add(readBtn);
        group.add(editBtn);
        readBtn.setSelected(true);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modePanel.setBackground(UIManager.getColor("Panel.background"));
        modePanel.add(readBtn);
        modePanel.add(editBtn);
        toolbarPanel.add(modePanel, BorderLayout.WEST);

        // Formatting Tools Panel (visible only in Edit mode)
        formatToolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        formatToolsPanel.setBackground(UIManager.getColor("Panel.background"));

        String[] fontNames = { "JetBrains Mono", "Segoe UI", "Arial", "Courier New", "Consolas", "Monospaced" };
        mdFontCombo = new JComboBox<>(fontNames);
        mdFontCombo.setSelectedItem("JetBrains Mono");
        mdFontCombo.addActionListener(e -> updateEditorFont());
        
        Integer[] fontSizes = { 10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24, 28, 32 };
        mdFontSizeCombo = new JComboBox<>(fontSizes);
        mdFontSizeCombo.setSelectedItem(12);
        mdFontSizeCombo.addActionListener(e -> updateEditorFont());

        formatToolsPanel.add(new JLabel("Font:"));
        formatToolsPanel.add(mdFontCombo);
        formatToolsPanel.add(Box.createHorizontalStrut(5));
        formatToolsPanel.add(new JLabel("Size:"));
        formatToolsPanel.add(mdFontSizeCombo);
        formatToolsPanel.add(Box.createHorizontalStrut(10));

        String[] headers = { "Paragraph", "Heading 1", "Heading 2", "Heading 3", "Heading 4" };
        mdHeaderCombo = new JComboBox<>(headers);
        mdHeaderCombo.addActionListener(e -> {
            int index = mdHeaderCombo.getSelectedIndex();
            if (index > 0) {
                applyHeader(index);
                mdHeaderCombo.setSelectedIndex(0);
            }
        });
        formatToolsPanel.add(new JLabel("Format:"));
        formatToolsPanel.add(mdHeaderCombo);
        formatToolsPanel.add(Box.createHorizontalStrut(10));

        JButton boldBtn = createToolbarButton("B", "Bold (Ctrl+B)", new Font("Segoe UI", Font.BOLD, 12));
        boldBtn.addActionListener(e -> insertMarkdown("**", "**"));
        formatToolsPanel.add(boldBtn);

        JButton italicBtn = createToolbarButton("I", "Italic (Ctrl+I)", new Font("Segoe UI", Font.ITALIC, 12));
        italicBtn.addActionListener(e -> insertMarkdown("*", "*"));
        formatToolsPanel.add(italicBtn);

        JButton strikeBtn = createToolbarButton("S", "Strikethrough", new Font("Segoe UI", Font.PLAIN, 12));
        strikeBtn.addActionListener(e -> insertMarkdown("~~", "~~"));
        formatToolsPanel.add(strikeBtn);

        JButton codeBtn = createToolbarButton("</>", "Code Block", new Font("Segoe UI", Font.PLAIN, 11));
        codeBtn.addActionListener(e -> insertMarkdown("`", "`"));
        formatToolsPanel.add(codeBtn);

        JButton linkBtn = createToolbarButton("", "Insert Link", new Font("Segoe UI", Font.PLAIN, 12));
        linkBtn.setIcon(VectorIcon.getLinkIcon(14));
        linkBtn.addActionListener(e -> {
            String text = JOptionPane.showInputDialog(this, "Enter Link Text:", "Insert Link", JOptionPane.PLAIN_MESSAGE);
            if (text != null && !text.trim().isEmpty()) {
                String url = JOptionPane.showInputDialog(this, "Enter Link URL:", "Insert Link", JOptionPane.PLAIN_MESSAGE);
                if (url != null) {
                    insertMarkdown("[" + text.trim() + "](", url.trim() + ")");
                }
            }
        });
        formatToolsPanel.add(linkBtn);

        JButton bulletBtn = createToolbarButton("", "Bulleted List", new Font("Segoe UI", Font.PLAIN, 12));
        bulletBtn.setIcon(VectorIcon.getBulletIcon(14));
        bulletBtn.addActionListener(e -> insertMarkdown("\n- ", ""));
        formatToolsPanel.add(bulletBtn);

        JButton numListBtn = createToolbarButton("", "Numbered List", new Font("Segoe UI", Font.PLAIN, 12));
        numListBtn.setIcon(VectorIcon.getNumberIcon(14));
        numListBtn.addActionListener(e -> insertMarkdown("\n1. ", ""));
        formatToolsPanel.add(numListBtn);

        JButton quoteBtn = createToolbarButton("", "Blockquote", new Font("Segoe UI", Font.BOLD, 12));
        quoteBtn.setIcon(VectorIcon.getQuoteIcon(14));
        quoteBtn.addActionListener(e -> insertMarkdown("\n> ", ""));
        formatToolsPanel.add(quoteBtn);

        JButton tableBtn = createToolbarButton("", "Insert Table", new Font("Segoe UI", Font.PLAIN, 12));
        tableBtn.setIcon(VectorIcon.getTableIcon(14));
        tableBtn.addActionListener(e -> {
            String tableTemplate = "\n| Header 1 | Header 2 |\n| --- | --- |\n| Cell 1 | Cell 2 |\n";
            insertMarkdown(tableTemplate, "");
        });
        formatToolsPanel.add(tableBtn);

        JButton hrBtn = createToolbarButton("", "Horizontal Line", new Font("Segoe UI", Font.PLAIN, 12));
        hrBtn.setIcon(VectorIcon.getLineIcon(14));
        hrBtn.addActionListener(e -> insertMarkdown("\n---\n", ""));
        formatToolsPanel.add(hrBtn);

        formatToolsPanel.setVisible(false);
        toolbarPanel.add(formatToolsPanel, BorderLayout.CENTER);
        overviewPanel.add(toolbarPanel, BorderLayout.NORTH);

        // CardLayout content panel
        overviewCardLayout = new CardLayout();
        overviewCardPanel = new JPanel(overviewCardLayout);
        overviewCardPanel.setBackground(UIManager.getColor("Panel.background"));

        // Edit view
        readmeArea = new RSyntaxTextArea();
        readmeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
        readmeArea.setCodeFoldingEnabled(true);
        readmeArea.setAntiAliasingEnabled(true);
        readmeArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        
        // Key bindings for RSyntaxTextArea
        readmeArea.getInputMap().put(KeyStroke.getKeyStroke("control B"), "insertBold");
        readmeArea.getActionMap().put("insertBold", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                insertMarkdown("**", "**");
            }
        });
        readmeArea.getInputMap().put(KeyStroke.getKeyStroke("control I"), "insertItalic");
        readmeArea.getActionMap().put("insertItalic", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                insertMarkdown("*", "*");
            }
        });

        RTextScrollPane editScrollPane = new RTextScrollPane(readmeArea);
        overviewCardPanel.add(editScrollPane, "edit");

        // Read view
        htmlPane = new JEditorPane();
        htmlPane.setEditable(false);
        htmlPane.setContentType("text/html");
        htmlPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    try {
                        String desc = e.getDescription();
                        if (desc != null && (desc.startsWith("http://") || desc.startsWith("https://"))) {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(desc));
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
        JScrollPane readScrollPane = new JScrollPane(htmlPane);
        readScrollPane.setBorder(null);
        overviewCardPanel.add(readScrollPane, "read");

        overviewPanel.add(overviewCardPanel, BorderLayout.CENTER);
        tabbedPane.addTab("Overview", overviewPanel);

        // Action listeners for mode changes
        readBtn.addActionListener(e -> {
            updateHtmlPreview();
            overviewCardLayout.show(overviewCardPanel, "read");
            formatToolsPanel.setVisible(false);
        });
        
        editBtn.addActionListener(e -> {
            overviewCardLayout.show(overviewCardPanel, "edit");
            formatToolsPanel.setVisible(true);
            readmeArea.requestFocusInWindow();
        });

        // 2. Authorization Tab
        JPanel authPanel = new JPanel(new BorderLayout(0, 8));
        authPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        authPanel.setBackground(UIManager.getColor("Panel.background"));
        JPanel authTypeBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authTypeBar.setBackground(UIManager.getColor("Panel.background"));
        boolean isRoot = mainFrame.getCollections().stream().anyMatch(c -> c.getId().equals(collectionModel.getId()));
        if (isRoot) {
            authTypeCombo = new JComboBox<>(new String[]{"none", "bearer", "basic", "apiKey", "oauth2"});
        } else {
            authTypeCombo = new JComboBox<>(new String[]{"inherit", "none", "bearer", "basic", "apiKey", "oauth2"});
        }
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

        // OAuth2 Panel
        JPanel oauth2Panel = new JPanel(new BorderLayout());
        oauth2Panel.setBackground(UIManager.getColor("Panel.background"));
        JPanel oauth2Form = new JPanel(new GridBagLayout());
        oauth2Form.setBackground(UIManager.getColor("Panel.background"));
        GridBagConstraints gbcOauth = new GridBagConstraints();
        gbcOauth.insets = new Insets(4, 4, 4, 4);
        gbcOauth.anchor = GridBagConstraints.WEST;
        gbcOauth.fill = GridBagConstraints.HORIZONTAL;

        int rowOauth = 0;
        
        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Grant Type:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2GrantTypeCombo = new JComboBox<>(new String[]{"authorization_code", "implicit", "password", "client_credentials"});
        oauth2Form.add(oauth2GrantTypeCombo, gbcOauth);
        rowOauth++;
        
        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Callback URL:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2CallbackUrlField = new HighlightTextField();
        oauth2Form.add(oauth2CallbackUrlField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Auth URL:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2AuthUrlField = new HighlightTextField();
        oauth2Form.add(oauth2AuthUrlField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Access Token URL:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2AccessTokenUrlField = new HighlightTextField();
        oauth2Form.add(oauth2AccessTokenUrlField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Client ID:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2ClientIdField = new HighlightTextField();
        oauth2Form.add(oauth2ClientIdField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Client Secret:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2ClientSecretField = new HighlightTextField();
        oauth2Form.add(oauth2ClientSecretField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Scope:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2ScopeField = new HighlightTextField();
        oauth2Form.add(oauth2ScopeField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("State:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2StateField = new HighlightTextField();
        oauth2Form.add(oauth2StateField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Username (Password Grant):"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2UsernameField = new HighlightTextField();
        oauth2Form.add(oauth2UsernameField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Password (Password Grant):"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2PasswordField = new JPasswordField();
        oauth2Form.add(oauth2PasswordField, gbcOauth);
        rowOauth++;
        
        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Client Auth:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        oauth2ClientAuthCombo = new JComboBox<>(new String[]{"header", "body"});
        oauth2Form.add(oauth2ClientAuthCombo, gbcOauth);
        rowOauth++;
        
        gbcOauth.gridx = 0; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Access Token:"), gbcOauth);
        gbcOauth.gridx = 1; gbcOauth.gridy = rowOauth; gbcOauth.weightx = 1;
        JPanel atkPanel = new JPanel(new BorderLayout(4, 0));
        atkPanel.setOpaque(false);
        oauth2AccessTokenField = new HighlightTextField();
        atkPanel.add(oauth2AccessTokenField, BorderLayout.CENTER);
        JButton oauth2DecodeJwtBtn = new JButton("Decode JWT");
        oauth2DecodeJwtBtn.addActionListener(e -> {
            String token = oauth2AccessTokenField.getText();
            if (token != null && !token.isBlank()) {
                mainFrame.openJwtDecoder(token);
            }
        });
        atkPanel.add(oauth2DecodeJwtBtn, BorderLayout.WEST);
        oauth2GetTokenBtn = new JButton("Get New Access Token");
        oauth2GetTokenBtn.addActionListener(e -> {
            save(); // save fields to model first
            in.slpro.japi.http.OAuth2Manager.getNewAccessToken(collectionModel, this);
        });
        atkPanel.add(oauth2GetTokenBtn, BorderLayout.EAST);
        oauth2Form.add(atkPanel, gbcOauth);

        oauth2Panel.add(new JScrollPane(oauth2Form), BorderLayout.CENTER);
        authCardPanel.add(oauth2Panel, "oauth2");


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

        // 6. Settings Tab
        JPanel settingsTabPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        settingsTabPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        settingsTabPanel.setBackground(UIManager.getColor("Panel.background"));
        
        settingsTabPanel.add(new JLabel("SSL Verification:"));
        sslVerifyCombo = new JComboBox<>(new String[]{"Inherit", "Do not verify", "Verify"});
        sslVerifyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sslVerifyCombo.setToolTipText("Select SSL verification behavior for this collection/folder. 'Inherit' resolves to parent collection/folder setting recursively, or global setting.");
        settingsTabPanel.add(sslVerifyCombo);

        settingsTabPanel.add(new JLabel("Auto Redirect (302):"));
        redirectVerifyCombo = new JComboBox<>(new String[]{"Inherit", "No (Don't follow)", "Yes (Follow)"});
        redirectVerifyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        redirectVerifyCombo.setToolTipText("Select redirect behavior for this collection/folder. 'Inherit' resolves to parent collection/folder setting recursively, or global setting.");
        settingsTabPanel.add(redirectVerifyCombo);
        
        JPanel settingsOuter = new JPanel(new BorderLayout());
        settingsOuter.setBackground(UIManager.getColor("Panel.background"));
        settingsOuter.add(settingsTabPanel, BorderLayout.NORTH);
        
        tabbedPane.addTab("Settings", settingsOuter);

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
        addSnippetButton(snippetPanel, scriptArea, "Clear env variable",
                "japi.environment.unset(\"variable_name\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Get global variable",
                "let val = japi.globals.get(\"variable_name\");\nconsole.log(val);\n");
        addSnippetButton(snippetPanel, scriptArea, "Set global variable",
                "japi.globals.set(\"variable_name\", \"value\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Clear global variable",
                "japi.globals.unset(\"variable_name\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Get collection variable",
                "let val = japi.collectionVariables.get(\"variable_name\");\nconsole.log(val);\n");
        addSnippetButton(snippetPanel, scriptArea, "Set collection variable",
                "japi.collectionVariables.set(\"variable_name\", \"value\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Clear collection variable",
                "japi.collectionVariables.unset(\"variable_name\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Get local variable",
                "let val = japi.variables.get(\"variable_name\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Set local variable",
                "japi.variables.set(\"variable_name\", \"value\");\n");
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
        if (readBtn != null) {
            readBtn.setSelected(true);
        }
        if (overviewCardLayout != null) {
            overviewCardLayout.show(overviewCardPanel, "read");
        }
        if (formatToolsPanel != null) {
            formatToolsPanel.setVisible(false);
        }
        updateHtmlPreview();

        String aType = collectionModel.getAuthType();
        boolean isRoot = mainFrame.getCollections().stream().anyMatch(c -> c.getId().equals(collectionModel.getId()));
        if (isRoot && ("inherit".equalsIgnoreCase(aType) || aType == null)) {
            aType = "none";
            collectionModel.setAuthType(aType);
        } else if (aType == null) {
            aType = "inherit";
        }
        authTypeCombo.setSelectedItem(aType);
        authCardLayout.show(authCardPanel, aType);
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
        String sslSetting = collectionModel.getSslSetting();
        if ("INHERIT".equalsIgnoreCase(sslSetting)) {
            sslVerifyCombo.setSelectedIndex(0);
        } else if ("NO_VERIFY".equalsIgnoreCase(sslSetting)) {
            sslVerifyCombo.setSelectedIndex(1);
        } else {
            sslVerifyCombo.setSelectedIndex(2);
        }
        
        String redirectSetting = collectionModel.getRedirectSetting();
        if ("INHERIT".equalsIgnoreCase(redirectSetting)) {
            redirectVerifyCombo.setSelectedIndex(0);
        } else if ("NO".equalsIgnoreCase(redirectSetting)) {
            redirectVerifyCombo.setSelectedIndex(1);
        } else {
            redirectVerifyCombo.setSelectedIndex(2);
        }
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
        collectionModel.setOauth2GrantType((String) oauth2GrantTypeCombo.getSelectedItem());
        collectionModel.setOauth2CallbackUrl(oauth2CallbackUrlField.getText());
        collectionModel.setOauth2AuthUrl(oauth2AuthUrlField.getText());
        collectionModel.setOauth2AccessTokenUrl(oauth2AccessTokenUrlField.getText());
        collectionModel.setOauth2ClientId(oauth2ClientIdField.getText());
        collectionModel.setOauth2ClientSecret(oauth2ClientSecretField.getText());
        collectionModel.setOauth2Scope(oauth2ScopeField.getText());
        collectionModel.setOauth2State(oauth2StateField.getText());
        collectionModel.setOauth2Username(oauth2UsernameField.getText());
        collectionModel.setOauth2Password(new String(oauth2PasswordField.getPassword()));
        collectionModel.setOauth2ClientAuth((String) oauth2ClientAuthCombo.getSelectedItem());
        collectionModel.setOauth2AccessToken(oauth2AccessTokenField.getText());

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
        int sslIndex = sslVerifyCombo.getSelectedIndex();
        if (sslIndex == 0) {
            collectionModel.setSslSetting("INHERIT");
        } else if (sslIndex == 1) {
            collectionModel.setSslSetting("NO_VERIFY");
        } else {
            collectionModel.setSslSetting("VERIFY");
        }
        
        int redirectIndex = redirectVerifyCombo.getSelectedIndex();
        if (redirectIndex == 0) {
            collectionModel.setRedirectSetting("INHERIT");
        } else if (redirectIndex == 1) {
            collectionModel.setRedirectSetting("NO");
        } else {
            collectionModel.setRedirectSetting("YES");
        }

        mainFrame.saveCollections();
        MainFrame.showToast(this, "Collection \"" + collectionModel.getName() + "\" saved.");
        this.originalModelJson = new com.google.gson.Gson().toJson(collectionModel);
    }

    public CollectionModel collectToNewModel() {
        CollectionModel m = new CollectionModel();
        m.setId(collectionModel.getId());
        m.setName(collectionModel.getName());
        m.setRequests(collectionModel.getRequests());
        m.setFolders(collectionModel.getFolders());

        m.setReadme(readmeArea.getText());
        m.setAuthType((String) authTypeCombo.getSelectedItem());
        m.setAuthToken(bearerTokenField.getText());
        m.setAuthUsername(basicUsernameField.getText());
        m.setAuthPassword(new String(basicPasswordField.getPassword()));
        m.setAuthApiKeyName(apiKeyNameField.getText());
        m.setAuthApiKeyValue(apiKeyValueField.getText());
        m.setAuthApiKeyIn((String) apiKeyInCombo.getSelectedItem());

        m.setOauth2GrantType((String) oauth2GrantTypeCombo.getSelectedItem());
        m.setOauth2CallbackUrl(oauth2CallbackUrlField.getText());
        m.setOauth2AuthUrl(oauth2AuthUrlField.getText());
        m.setOauth2AccessTokenUrl(oauth2AccessTokenUrlField.getText());
        m.setOauth2ClientId(oauth2ClientIdField.getText());
        m.setOauth2ClientSecret(oauth2ClientSecretField.getText());
        m.setOauth2Scope(oauth2ScopeField.getText());
        m.setOauth2State(oauth2StateField.getText());
        m.setOauth2Username(oauth2UsernameField.getText());
        m.setOauth2Password(new String(oauth2PasswordField.getPassword()));
        m.setOauth2ClientAuth((String) oauth2ClientAuthCombo.getSelectedItem());
        m.setOauth2AccessToken(oauth2AccessTokenField.getText());

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
        int sslIndex2 = sslVerifyCombo.getSelectedIndex();
        if (sslIndex2 == 0) {
            m.setSslSetting("INHERIT");
        } else if (sslIndex2 == 1) {
            m.setSslSetting("NO_VERIFY");
        } else {
            m.setSslSetting("VERIFY");
        }
        
        int redirectIndex2 = redirectVerifyCombo.getSelectedIndex();
        if (redirectIndex2 == 0) {
            m.setRedirectSetting("INHERIT");
        } else if (redirectIndex2 == 1) {
            m.setRedirectSetting("NO");
        } else {
            m.setRedirectSetting("YES");
        }
        return m;
    }

    public boolean hasUnsavedChanges() {
        if (originalModelJson == null) return false;
        CollectionModel current = collectToNewModel();
        String currentJson = new com.google.gson.Gson().toJson(current);
        return !originalModelJson.equals(currentJson);
    }

    public void updateFontSize(int size) {
        this.currentGlobalFontSize = size;
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, size + 2));
        if (saveBtn != null) {
            int height = Math.max(28, size + 12);
            int width = Math.max(80, size * 6);
            saveBtn.setPreferredSize(new Dimension(width, height));
        }
        FontScaleHelper.scaleFonts(this, size);
        if (mdFontSizeCombo != null) {
            mdFontSizeCombo.setSelectedItem(size);
        }
        updateHtmlPreview();
        revalidate();
        repaint();
    }

    private JButton createToolbarButton(String text, String tooltip, Font font) {
        JButton btn = new JButton(text);
        btn.setFont(font);
        btn.setToolTipText(tooltip);
        btn.setFocusable(false);
        btn.setMargin(new Insets(2, 6, 2, 6));
        btn.putClientProperty("JButton.buttonType", "toolBarButton");
        return btn;
    }

    private void insertMarkdown(String prefix, String suffix) {
        String selectedText = readmeArea.getSelectedText();
        if (selectedText == null) {
            selectedText = "";
        }
        int start = readmeArea.getSelectionStart();
        int end = readmeArea.getSelectionEnd();
        String replacement = prefix + selectedText + suffix;
        readmeArea.replaceRange(replacement, start, end);
        if (selectedText.isEmpty()) {
            readmeArea.setCaretPosition(start + prefix.length());
        } else {
            readmeArea.setCaretPosition(start + replacement.length());
        }
        readmeArea.requestFocusInWindow();
    }

    private void applyHeader(int level) {
        if (level <= 0) return;
        String prefix = "#".repeat(level) + " ";
        int caretPos = readmeArea.getCaretPosition();
        try {
            int line = readmeArea.getLineOfOffset(caretPos);
            int start = readmeArea.getLineStartOffset(line);
            readmeArea.insert(prefix, start);
        } catch (Exception ignored) {}
        readmeArea.requestFocusInWindow();
    }

    private void updateEditorFont() {
        if (readmeArea == null || mdFontCombo == null || mdFontSizeCombo == null) return;
        String fontName = (String) mdFontCombo.getSelectedItem();
        Integer fontSize = (Integer) mdFontSizeCombo.getSelectedItem();
        if (fontName != null && fontSize != null) {
            readmeArea.setFont(new Font(fontName, Font.PLAIN, fontSize));
        }
    }

    private String toHex(Color color) {
        if (color == null) return "#888888";
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private boolean isDarkTheme() {
        try {
            String theme = in.slpro.japi.storage.StorageManager.getInstance().getSettings().getTheme();
            return "dark".equals(theme);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateHtmlPreview() {
        if (htmlPane == null || readmeArea == null) return;
        String markdownText = readmeArea.getText();
        if (markdownText == null || markdownText.trim().isEmpty()) {
            htmlPane.setText("<html><body><p style='color: #888888; font-style: italic; font-family: sans-serif;'>No description provided. Click 'Edit' to add one.</p></body></html>");
            return;
        }

        try {
            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdownText);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String rawHtml = renderer.render(document);

            Color bg = UIManager.getColor("Panel.background");
            Color fg = UIManager.getColor("Label.foreground");
            Color border = UIManager.getColor("Workspace.borderColor");
            Color accent = UIManager.getColor("AccentColor");
            if (accent == null) accent = new Color(52, 152, 219);
            
            if (bg == null) bg = Color.WHITE;
            if (fg == null) fg = Color.BLACK;
            if (border == null) border = new Color(228, 228, 228);

            String bgHex = toHex(bg);
            String fgHex = toHex(fg);
            String borderHex = toHex(border);
            String accentHex = toHex(accent);
            String codeBgHex = toHex(isDarkTheme() ? new Color(45, 48, 52) : new Color(240, 240, 240));
            String headerBgHex = toHex(isDarkTheme() ? new Color(38, 41, 44) : new Color(245, 245, 245));
            String mutedHex = toHex(isDarkTheme() ? new Color(160, 160, 160) : new Color(100, 100, 100));

            int baseFontSize = currentGlobalFontSize;
            if (baseFontSize <= 0) {
                baseFontSize = 13;
                try {
                    Font defaultFont = UIManager.getFont("defaultFont");
                    if (defaultFont != null) {
                        baseFontSize = defaultFont.getSize();
                    }
                } catch (Exception ignored) {}
            }

            HTMLEditorKit kit = new HTMLEditorKit();
            StyleSheet styleSheet = kit.getStyleSheet();
            styleSheet.addRule("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Helvetica, Arial, sans-serif; font-size: " + baseFontSize + "px; color: " + fgHex + "; background-color: " + bgHex + "; margin: 15px; line-height: 1.6; }");
            styleSheet.addRule("h1 { font-size: 1.5em; font-weight: bold; color: " + fgHex + "; border-bottom: 1px solid " + borderHex + "; padding-bottom: 6px; margin-top: 24px; margin-bottom: 12px; }");
            styleSheet.addRule("h2 { font-size: 1.3em; font-weight: bold; color: " + fgHex + "; border-bottom: 1px solid " + borderHex + "; padding-bottom: 4px; margin-top: 20px; margin-bottom: 10px; }");
            styleSheet.addRule("h3 { font-size: 1.15em; font-weight: bold; color: " + fgHex + "; margin-top: 16px; margin-bottom: 8px; }");
            styleSheet.addRule("p { margin-top: 0px; margin-bottom: 12px; }");
            styleSheet.addRule("a { color: " + accentHex + "; text-decoration: none; }");
            styleSheet.addRule("code { font-family: 'JetBrains Mono', 'Courier New', monospace; font-size: 0.9em; background-color: " + codeBgHex + "; padding: 2px 4px; border-radius: 3px; }");
            styleSheet.addRule("pre { font-family: 'JetBrains Mono', 'Courier New', monospace; font-size: 0.9em; background-color: " + codeBgHex + "; border: 1px solid " + borderHex + "; padding: 12px; border-radius: 6px; display: block; margin-bottom: 12px; }");
            styleSheet.addRule("blockquote { border-left: 4px solid " + accentHex + "; margin: 0 0 12px 0; padding-left: 12px; color: " + mutedHex + "; font-style: italic; }");
            styleSheet.addRule("ul, ol { margin-top: 0px; margin-bottom: 12px; padding-left: 20px; }");
            styleSheet.addRule("li { margin-bottom: 4px; }");
            styleSheet.addRule("table { border-collapse: collapse; width: 100%; margin-bottom: 16px; }");
            styleSheet.addRule("th, td { border: 1px solid " + borderHex + "; padding: 8px; text-align: left; }");
            styleSheet.addRule("th { background-color: " + headerBgHex + "; font-weight: bold; }");
            styleSheet.addRule("hr { border: 0; border-top: 1px solid " + borderHex + "; margin: 20px 0; }");
            styleSheet.addRule("strong, b { font-weight: bold; }");
            styleSheet.addRule("em, i { font-style: italic; }");

            htmlPane.setEditorKit(kit);
            htmlPane.setText("<html><body>" + rawHtml + "</body></html>");
            htmlPane.setCaretPosition(0);
        } catch (Exception e) {
            htmlPane.setText("<html><body><pre style='color: red;'>" + e.getMessage() + "</pre></body></html>");
        }
    }

    public CollectionModel getCollectionModel() {
        return collectionModel;
    }

    public void triggerVariableRepaint() {
        repaint();
    }
}
