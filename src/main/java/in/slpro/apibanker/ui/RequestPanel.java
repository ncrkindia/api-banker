package in.slpro.apibanker.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import in.slpro.apibanker.http.HttpClientWrapper;
import in.slpro.apibanker.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * RequestPanel
 *
 * <p>
 * This panel provides the main interface for constructing, editing, and
 * dispatching
 * individual API requests. It behaves similarly to Postman's request builder,
 * featuring
 * a URL bar, HTTP method selector, and a comprehensive tabbed view for
 * configuring
 * Params, Headers, Body, Auth, and Scripts. It also manages the split-pane
 * layout
 * linking the request builder to the {@link ResponsePanel}.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
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
    private DefaultTableModel urlencodedModel;
    private HighlightRSyntaxTextArea bodyArea;
    private HighlightRSyntaxTextArea graphqlQueryArea;
    private HighlightRSyntaxTextArea graphqlVarsArea;
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
    private JComboBox<String> sslVerifyCombo;
    private JComboBox<String> redirectVerifyCombo;

    // Auth fields
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

    // Response
    private ResponsePanel responsePanel;

    // Split
    private JSplitPane splitPane;

    private static final String[] METHODS = { "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS" };

    /**
     * Constructs the primary Request Builder interface.
     * <p>
     * Initializes the Top Bar (Method, URL, Send/Save buttons), the Tabbed
     * Configuration pane
     * (Params, Headers, Body with dynamic sub-panels, Auth, Scripts), and the
     * bottom
     * Response Panel. Binds UI state directly to the provided {@link RequestModel}.
     * </p>
     * 
     * @param mainFrame    The root application window (for reading active
     *                     environments).
     * @param requestModel The persistent state model containing the request's
     *                     details.
     */
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
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                onUrlChanged();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                onUrlChanged();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                onUrlChanged();
            }

            private void onUrlChanged() {
                if (isSyncing)
                    return;
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
            if (isSyncing)
                return;
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
        bodyTypeCombo = new JComboBox<>(
                new String[] { "none", "raw", "form-data", "x-www-form-urlencoded", "graphql" });
        bodyTypeCombo.addActionListener(e -> updateBodyCard());
        bodyTypeBar.add(new JLabel("Body:"));
        bodyTypeBar.add(bodyTypeCombo);
        rawTypeCombo = new JComboBox<>(new String[] { "JSON", "Text", "HTML", "XML", "JavaScript" });
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

        formDataModel = buildFormDataModel();
        formDataModel.addTableModelListener(e -> triggerVariableRepaint());
        JPanel formDataPanel = buildKVPanel(buildFormDataTable(formDataModel), formDataModel);
        bodyCards.add(formDataPanel, "form-data");

        urlencodedModel = buildKVModel();
        urlencodedModel.addTableModelListener(e -> triggerVariableRepaint());
        JPanel urlencodedPanel = buildKVPanel(buildKVTable(urlencodedModel), urlencodedModel);
        bodyCards.add(urlencodedPanel, "x-www-form-urlencoded");

        // GraphQL Panel
        JPanel graphqlPanel = new JPanel(new BorderLayout(0, 4));

        JPanel gqlToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        JButton fetchIntrospectionBtn = new JButton("Fetch Schema");
        fetchIntrospectionBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        fetchIntrospectionBtn.addActionListener(e -> fetchIntrospectionSchema());
        gqlToolbar.add(fetchIntrospectionBtn);
        graphqlPanel.add(gqlToolbar, BorderLayout.NORTH);

        JSplitPane gqlSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        gqlSplit.setResizeWeight(0.65);
        gqlSplit.setBorder(null);

        JPanel queryWrap = new JPanel(new BorderLayout());
        queryWrap.add(new JLabel("Query:"), BorderLayout.NORTH);
        graphqlQueryArea = new HighlightRSyntaxTextArea();
        graphqlQueryArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        graphqlQueryArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        graphqlQueryArea.setAntiAliasingEnabled(true);
        graphqlQueryArea.setHighlightCurrentLine(false);
        queryWrap.add(new RTextScrollPane(graphqlQueryArea), BorderLayout.CENTER);
        gqlSplit.setTopComponent(queryWrap);

        JPanel varsWrap = new JPanel(new BorderLayout());
        varsWrap.add(new JLabel("Variables (JSON):"), BorderLayout.NORTH);
        graphqlVarsArea = new HighlightRSyntaxTextArea();
        graphqlVarsArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        graphqlVarsArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        graphqlVarsArea.setAntiAliasingEnabled(true);
        graphqlVarsArea.setHighlightCurrentLine(false);
        varsWrap.add(new RTextScrollPane(graphqlVarsArea), BorderLayout.CENTER);
        gqlSplit.setBottomComponent(varsWrap);

        graphqlPanel.add(gqlSplit, BorderLayout.CENTER);
        bodyCards.add(graphqlPanel, "graphql");

        bodyPanel.add(bodyCards, BorderLayout.CENTER);
        bodyTypeCombo.addActionListener(e -> {
            String sel = (String) bodyTypeCombo.getSelectedItem();
            rawTypeCombo.setVisible("raw".equals(sel));
            bodyCardLayout.show(bodyCards, sel != null ? sel : "none");
            if ("raw".equals(sel)) {
                String rawType = (String) rawTypeCombo.getSelectedItem();
                bodyArea.setSyntaxEditingStyle("JSON".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_JSON
                        : "HTML".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_HTML
                                : "XML".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_XML
                                        : SyntaxConstants.SYNTAX_STYLE_NONE);
            }
        });
        rawTypeCombo.addActionListener(e -> {
            String rawType = (String) rawTypeCombo.getSelectedItem();
            bodyArea.setSyntaxEditingStyle("JSON".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_JSON
                    : "HTML".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_HTML
                            : "XML".equals(rawType) ? SyntaxConstants.SYNTAX_STYLE_XML
                                    : SyntaxConstants.SYNTAX_STYLE_NONE);
        });
        requestTabs.addTab("Body", bodyPanel);

        // Auth tab
        authPanel = new JPanel(new BorderLayout(0, 8));
        authPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        authPanel.setBackground(UIManager.getColor("Panel.background"));
        JPanel authTypeBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        authTypeBar.setBackground(UIManager.getColor("Panel.background"));
        authTypeCombo = new JComboBox<>(new String[] { "inherit", "none", "bearer", "basic", "apiKey", "oauth2" });
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
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        basicPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        basicUsernameField = new JTextField();
        basicPanel.add(basicUsernameField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        basicPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        basicPasswordField = new JPasswordField();
        basicPanel.add(basicPasswordField, gbc);
        authCardPanel.add(basicPanel, "basic");

        JPanel apiKeyPanel = new JPanel(new GridBagLayout());
        apiKeyPanel.setBackground(UIManager.getColor("Panel.background"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        apiKeyPanel.add(new JLabel("Key Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        apiKeyNameField = new HighlightTextField();
        apiKeyPanel.add(apiKeyNameField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        apiKeyPanel.add(new JLabel("Key Value:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        apiKeyValueField = new HighlightTextField();
        apiKeyPanel.add(apiKeyValueField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        apiKeyPanel.add(new JLabel("Add to:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        apiKeyInCombo = new JComboBox<>(new String[] { "header", "query" });
        apiKeyPanel.add(apiKeyInCombo, gbc);
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

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Grant Type:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2GrantTypeCombo = new JComboBox<>(
                new String[] { "authorization_code", "implicit", "password", "client_credentials" });
        oauth2Form.add(oauth2GrantTypeCombo, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Callback URL:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2CallbackUrlField = new HighlightTextField();
        oauth2Form.add(oauth2CallbackUrlField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Auth URL:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2AuthUrlField = new HighlightTextField();
        oauth2Form.add(oauth2AuthUrlField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Access Token URL:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2AccessTokenUrlField = new HighlightTextField();
        oauth2Form.add(oauth2AccessTokenUrlField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Client ID:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2ClientIdField = new HighlightTextField();
        oauth2Form.add(oauth2ClientIdField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Client Secret:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2ClientSecretField = new HighlightTextField();
        oauth2Form.add(oauth2ClientSecretField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Scope:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2ScopeField = new HighlightTextField();
        oauth2Form.add(oauth2ScopeField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("State:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2StateField = new HighlightTextField();
        oauth2Form.add(oauth2StateField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Username (Password Grant):"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2UsernameField = new HighlightTextField();
        oauth2Form.add(oauth2UsernameField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Password (Password Grant):"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2PasswordField = new JPasswordField();
        oauth2Form.add(oauth2PasswordField, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Client Auth:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
        oauth2ClientAuthCombo = new JComboBox<>(new String[] { "header", "body" });
        oauth2Form.add(oauth2ClientAuthCombo, gbcOauth);
        rowOauth++;

        gbcOauth.gridx = 0;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 0;
        oauth2Form.add(new JLabel("Access Token:"), gbcOauth);
        gbcOauth.gridx = 1;
        gbcOauth.gridy = rowOauth;
        gbcOauth.weightx = 1;
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
            collectModel(); // save fields to model first
            in.slpro.apibanker.http.OAuth2Manager.getNewAccessToken(requestModel, this);
        });
        atkPanel.add(oauth2GetTokenBtn, BorderLayout.EAST);
        oauth2Form.add(atkPanel, gbcOauth);

        oauth2Panel.add(new JScrollPane(oauth2Form), BorderLayout.CENTER);
        authCardPanel.add(oauth2Panel, "oauth2");

        authTypeCombo
                .addActionListener(e -> authCardLayout.show(authCardPanel, (String) authTypeCombo.getSelectedItem()));
        authPanel.add(authCardPanel, BorderLayout.CENTER);
        requestTabs.addTab("Auth", authPanel);

        // Scripts
        preScriptArea = buildScriptArea();
        postScriptArea = buildScriptArea();
        requestTabs.addTab("Pre-request Script", buildScriptTab(preScriptArea, false));
        requestTabs.addTab("Tests", buildScriptTab(postScriptArea, true));

        // Settings tab
        JPanel settingsTabPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        settingsTabPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        settingsTabPanel.setBackground(UIManager.getColor("Panel.background"));

        settingsTabPanel.add(new JLabel("SSL Verification:"));
        sslVerifyCombo = new JComboBox<>(new String[] { "Inherit", "Do not verify", "Verify" });
        sslVerifyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sslVerifyCombo.setToolTipText(
                "Select SSL verification behavior. 'Inherit' will resolve to Collection/Folder setting recursively.");
        settingsTabPanel.add(sslVerifyCombo);

        settingsTabPanel.add(new JLabel("Auto Redirect (302):"));
        redirectVerifyCombo = new JComboBox<>(new String[] { "Inherit", "No (Don't follow)", "Yes (Follow)" });
        redirectVerifyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        redirectVerifyCombo.setToolTipText(
                "Select redirect behavior. 'Inherit' will resolve to Collection/Folder setting recursively.");
        settingsTabPanel.add(redirectVerifyCombo);

        JPanel settingsOuter = new JPanel(new BorderLayout());
        settingsOuter.setBackground(UIManager.getColor("Panel.background"));
        settingsOuter.add(settingsTabPanel, BorderLayout.NORTH);

        requestTabs.addTab("Settings", settingsOuter);

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
        VariableHelper.attachToTextComponent(graphqlQueryArea, requestModel, mainFrame);
        VariableHelper.attachToTextComponent(graphqlVarsArea, requestModel, mainFrame);

        // Keyboard Shortcuts
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                        "sendRequest");
        this.getActionMap().put("sendRequest", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (sendBtn != null && sendBtn.isEnabled()) {
                    sendBtn.doClick();
                }
            }
        });
    }

    private JPanel buildLabeledField(String label, JTextField field) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIManager.getColor("Panel.background"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
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
                "let val = apibanker.environment.get(\"variable_name\");\nconsole.log(val);\n");
        addSnippetButton(snippetPanel, scriptArea, "Set env variable",
                "apibanker.environment.set(\"variable_name\", \"value\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Clear env variable",
                "apibanker.environment.unset(\"variable_name\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Get global variable",
                "let val = apibanker.globals.get(\"variable_name\");\nconsole.log(val);\n");
        addSnippetButton(snippetPanel, scriptArea, "Set global variable",
                "apibanker.globals.set(\"variable_name\", \"value\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Clear global variable",
                "apibanker.globals.unset(\"variable_name\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Get collection variable",
                "let val = apibanker.collectionVariables.get(\"variable_name\");\nconsole.log(val);\n");
        addSnippetButton(snippetPanel, scriptArea, "Set collection variable",
                "apibanker.collectionVariables.set(\"variable_name\", \"value\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Clear collection variable",
                "apibanker.collectionVariables.unset(\"variable_name\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Get local variable",
                "let val = apibanker.variables.get(\"variable_name\");\n");
        addSnippetButton(snippetPanel, scriptArea, "Set local variable",
                "apibanker.variables.set(\"variable_name\", \"value\");\n");
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
                    "apibanker.test(\"Status code is 200\", function() {\n    apibanker.expect(apibanker.response.code).to.equal(200);\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "Response time < 500ms",
                    "apibanker.test(\"Response time is acceptable\", function() {\n    apibanker.expect(apibanker.response.responseTime).to.be.below(500);\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "Body contains string",
                    "apibanker.test(\"Body contains expected text\", function() {\n    apibanker.expect(apibanker.response.text()).to.include(\"expected\");\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "JSON value check",
                    "apibanker.test(\"JSON value check\", function() {\n    var data = apibanker.response.json();\n    apibanker.expect(data.key).to.equal(\"expected_value\");\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "JSON has property",
                    "apibanker.test(\"Has expected property\", function() {\n    var data = apibanker.response.json();\n    apibanker.expect(data).to.have.property(\"key\");\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "Response header check",
                    "apibanker.test(\"Content-Type is JSON\", function() {\n    var ct = apibanker.response.headers.get(\"content-type\");\n    apibanker.expect(ct).to.include(\"application/json\");\n});\n");
            addSnippetButton(snippetPanel, scriptArea, "Save response to env",
                    "apibanker.test(\"Save token to env\", function() {\n    var data = apibanker.response.json();\n    apibanker.environment.set(\"auth_token\", data.token);\n});\n");
        } else {
            snippetPanel.add(Box.createVerticalStrut(8));
            addSnippetButton(snippetPanel, scriptArea, "Set request header",
                    "// Headers are set in the Headers tab.\n// Use pre-request to compute dynamic values:\nvar timestamp = new Date().getTime();\napibanker.environment.set(\"timestamp\", \"\" + timestamp);\n");
            addSnippetButton(snippetPanel, scriptArea, "Generate random data",
                    "var rand = Math.floor(Math.random() * 10000);\napibanker.environment.set(\"random_id\", \"\" + rand);\nconsole.log(\"Generated ID: \" + rand);\n");
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
        btn.setForeground(UIManager.getColor("AccentColor") != null ? UIManager.getColor("AccentColor")
                : new Color(52, 152, 219));
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
            } catch (Exception ignored) {
            }
        });
        parent.add(btn);
        parent.add(Box.createVerticalStrut(2));
    }

    private DefaultTableModel buildFormDataModel() {
        return new DefaultTableModel(new Object[] { "", "Key", "Type", "Value", "Description" }, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return true;
            }
        };
    }

    private JTable buildFormDataTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    Object val = getValueAt(row, col);
                    if (val instanceof String s) {
                        java.util.regex.Matcher matcher = VariableHelper.VAR_PATTERN.matcher(s);
                        StringBuilder sb = new StringBuilder(
                                "<html><body style='font-family: sans-serif; padding: 2px;'>");
                        boolean found = false;
                        while (matcher.find()) {
                            String varName = matcher.group(1).trim();
                            VariableHelper.VariableResolution res = VariableHelper.resolveVariable(varName,
                                    requestModel, mainFrame);
                            if (res.resolved) {
                                sb.append(String.format(
                                        "<b>Variable:</b> %s<br/><b>Source:</b> %s<br/><b>Current Value:</b> <font color='green'>%s</font><br/><br/>",
                                        res.name, res.source, res.value));
                            } else {
                                sb.append(String.format(
                                        "<b>Variable:</b> %s<br/><b>Source:</b> <font color='red'>Unresolved</font><br/><br/>",
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
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                
                if (t.getModel().getColumnCount() > 3) {
                    Object desc = t.getModel().getValueAt(t.convertRowIndexToModel(row), t.getModel().getColumnCount() - 1);
                    if (desc instanceof String && ((String) desc).contains("<calculated>")) {
                        if (!isSelected) {
                            c.setBackground(UIManager.getColor("Panel.background").darker());
                            c.setForeground(Color.GRAY);
                        }
                    } else {
                        if (!isSelected) {
                            c.setBackground(t.getBackground());
                            c.setForeground(t.getForeground());
                        }
                    }
                }
                
                if (c instanceof JLabel label && value instanceof String s) {
                    if (s.contains("{{") && s.contains("}}")) {
                        java.util.regex.Matcher matcher = VariableHelper.VAR_PATTERN.matcher(s);
                        StringBuffer sb = new StringBuffer("<html>");
                        while (matcher.find()) {
                            String varName = matcher.group(1).trim();
                            VariableHelper.VariableResolution res = VariableHelper.resolveVariable(varName,
                                    requestModel, mainFrame);
                            String colorStr;
                            if (res.resolved) {
                                Color colVal = res.getColor();
                                colorStr = String.format("#%02x%02x%02x", colVal.getRed(), colVal.getGreen(),
                                        colVal.getBlue());
                            } else {
                                Color colVal = res.getColor();
                                colorStr = String.format("#%02x%02x%02x", colVal.getRed(), colVal.getGreen(),
                                        colVal.getBlue());
                            }
                            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                                    "<span style='color: " + colorStr + "; font-weight: bold;'>{{" + varName
                                            + "}}</span>"));
                        }
                        matcher.appendTail(sb);
                        sb.append("</html>");
                        label.setText(sb.toString());
                    }
                }
                return c;
            }
        };

        JComboBox<String> typeCombo = new JComboBox<>(new String[] { "text", "file" });
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(typeCombo));
        table.getColumnModel().getColumn(3).setCellEditor(new FileCellEditor());

        table.getColumnModel().getColumn(0).setMaxWidth(30);
        table.getColumnModel().getColumn(0).setMinWidth(30);
        table.getColumnModel().getColumn(1).setCellRenderer(defaultRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(defaultRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(defaultRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(defaultRenderer);
        table.setRowHeight(24);
        return table;
    }

    private class FileCellEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JTextField text;
        private JButton btn;
        private String currentVal;

        public FileCellEditor() {
            panel = new JPanel(new BorderLayout(2, 0));
            panel.setOpaque(false);
            text = new JTextField();
            btn = new JButton("...");
            btn.setPreferredSize(new Dimension(24, 18));
            btn.setFocusable(false);
            btn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                    text.setText(chooser.getSelectedFile().getAbsolutePath());
                    fireEditingStopped();
                }
            });
            panel.add(text, BorderLayout.CENTER);
            panel.add(btn, BorderLayout.EAST);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            String type = (String) table.getValueAt(row, 2);
            currentVal = value != null ? value.toString() : "";
            text.setText(currentVal);
            if ("file".equalsIgnoreCase(type)) {
                return panel;
            } else {
                return text;
            }
        }

        @Override
        public Object getCellEditorValue() {
            return text.getText();
        }
    }

    private DefaultTableModel buildKVModel() {
        return new DefaultTableModel(new Object[] { "", "Key", "Value", "Description" }, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                if (getColumnCount() > 3) {
                    Object desc = getValueAt(r, 3);
                    if (desc instanceof String && ((String) desc).contains("<calculated>")) {
                        return false;
                    }
                }
                return true;
            }
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
                        StringBuilder sb = new StringBuilder(
                                "<html><body style='font-family: sans-serif; padding: 2px;'>");
                        boolean found = false;
                        while (matcher.find()) {
                            String varName = matcher.group(1).trim();
                            VariableHelper.VariableResolution res = VariableHelper.resolveVariable(varName,
                                    requestModel, mainFrame);
                            if (res.resolved) {
                                sb.append(String.format(
                                        "<b>Variable:</b> %s<br/><b>Source:</b> %s<br/><b>Current Value:</b> <font color='green'>%s</font><br/><br/>",
                                        res.name, res.source, res.value));
                            } else {
                                sb.append(String.format(
                                        "<b>Variable:</b> %s<br/><b>Source:</b> <font color='red'>Unresolved</font><br/><br/>",
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
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus,
                    int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                
                if (t.getModel().getColumnCount() > 3) {
                    Object desc = t.getModel().getValueAt(t.convertRowIndexToModel(row), 3);
                    if (desc instanceof String && ((String) desc).contains("<calculated>")) {
                        if (!isSelected) {
                            c.setBackground(UIManager.getColor("Panel.background").darker());
                            c.setForeground(Color.GRAY);
                        }
                    } else {
                        if (!isSelected) {
                            c.setBackground(t.getBackground());
                            c.setForeground(t.getForeground());
                        }
                    }
                }
                
                if (c instanceof JLabel label && value instanceof String s) {
                    if (s.contains("{{") && s.contains("}}")) {
                        java.util.regex.Matcher matcher = VariableHelper.VAR_PATTERN.matcher(s);
                        StringBuffer sb = new StringBuffer("<html>");
                        while (matcher.find()) {
                            String varName = matcher.group(1).trim();
                            VariableHelper.VariableResolution res = VariableHelper.resolveVariable(varName,
                                    requestModel, mainFrame);
                            String colorStr;
                            if (res.resolved) {
                                Color colVal = res.getColor();
                                colorStr = String.format("#%02x%02x%02x", colVal.getRed(), colVal.getGreen(),
                                        colVal.getBlue());
                            } else {
                                Color colVal = res.getColor();
                                colorStr = String.format("#%02x%02x%02x", colVal.getRed(), colVal.getGreen(),
                                        colVal.getBlue());
                            }
                            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                                    "<span style='color: " + colorStr + "; font-weight: bold;'>{{" + varName
                                            + "}}</span>"));
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
        in.slpro.apibanker.ui.GlobalVariablesPanel.setupTableCopyPaste(table, model);
        return table;
    }

    private JPanel buildKVPanel(JTable table, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIManager.getColor("Panel.background"));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        btns.setBackground(UIManager.getColor("Panel.background"));
        JButton addBtn = new JButton("+ Add Row");
        JButton delBtn = new JButton("Delete");
        addBtn.addActionListener(e -> model.addRow(new Object[] { true, "", "", "" }));
        delBtn.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0)
                model.removeRow(r);
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
                    paramsModel
                            .addRow(new Object[] { kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription() });
                }
            }

            headersModel.setRowCount(0);
            
            // 1. Add Default Computed Headers at the top
            headersModel.addRow(new Object[] { true, "Host", "<calculated at runtime>", "<calculated>" });
            
            String bt = requestModel.getBodyType() != null ? requestModel.getBodyType() : "none";
            if (!"none".equalsIgnoreCase(bt)) {
                String ct = "";
                if ("raw".equalsIgnoreCase(bt)) {
                    String rt = requestModel.getBodyRawType();
                    if ("JSON".equalsIgnoreCase(rt)) ct = "application/json";
                    else if ("XML".equalsIgnoreCase(rt)) ct = "application/xml";
                    else if ("HTML".equalsIgnoreCase(rt)) ct = "text/html";
                    else ct = "text/plain";
                } else if ("form-data".equalsIgnoreCase(bt)) {
                    ct = "multipart/form-data; boundary=<calculated>";
                } else if ("x-www-form-urlencoded".equalsIgnoreCase(bt) || "form".equalsIgnoreCase(bt)) {
                    ct = "application/x-www-form-urlencoded";
                } else if ("graphql".equalsIgnoreCase(bt)) {
                    ct = "application/json";
                }
                
                // Only show computed Content-Type if user hasn't overridden it
                boolean userHasCt = false;
                if (requestModel.getHeaders() != null) {
                    userHasCt = requestModel.getHeaders().stream().anyMatch(h -> "Content-Type".equalsIgnoreCase(h.getKey()));
                }
                if (!ct.isEmpty() && !userHasCt) {
                    headersModel.addRow(new Object[] { true, "Content-Type", ct, "<calculated>" });
                }
                headersModel.addRow(new Object[] { true, "Content-Length", "<calculated at runtime>", "<calculated>" });
            } else {
                headersModel.addRow(new Object[] { true, "Content-Length", "<calculated at runtime>", "<calculated>" });
            }
            
            // 2. Add user headers
            if (requestModel.getHeaders() != null) {
                for (KeyValueItem kv : requestModel.getHeaders()) {
                    headersModel
                            .addRow(new Object[] { kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription() });
                }
            }

            if ("form".equals(bt)) {
                bt = "x-www-form-urlencoded";
            }
            bodyTypeCombo.setSelectedItem(bt);
            rawTypeCombo
                    .setSelectedItem(requestModel.getBodyRawType() != null ? requestModel.getBodyRawType() : "JSON");

            if ("graphql".equals(bt)) {
                String rawContent = requestModel.getBodyRawContent();
                String query = "";
                String vars = "";
                if (rawContent != null && rawContent.trim().startsWith("{")) {
                    try {
                        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(rawContent)
                                .getAsJsonObject();
                        if (json.has("query")) {
                            query = json.get("query").getAsString();
                        }
                        if (json.has("variables")) {
                            vars = json.get("variables").getAsString();
                        }
                    } catch (Exception e) {
                        query = rawContent;
                    }
                } else {
                    query = rawContent != null ? rawContent : "";
                }
                graphqlQueryArea.setText(query);
                graphqlVarsArea.setText(vars != null ? vars : "");
                bodyArea.setText("");
            } else {
                bodyArea.setText(requestModel.getBodyRawContent() != null ? requestModel.getBodyRawContent() : "");
                graphqlQueryArea.setText("");
                graphqlVarsArea.setText("");
            }

            formDataModel.setRowCount(0);
            if (requestModel.getFormData() != null) {
                for (KeyValueItem kv : requestModel.getFormData()) {
                    formDataModel.addRow(new Object[] { kv.isEnabled(), kv.getKey(),
                            kv.getType() != null ? kv.getType() : "text", kv.getValue(), kv.getDescription() });
                }
            }

            urlencodedModel.setRowCount(0);
            if (requestModel.getUrlencodedData() != null && !requestModel.getUrlencodedData().isEmpty()) {
                for (KeyValueItem kv : requestModel.getUrlencodedData()) {
                    urlencodedModel
                            .addRow(new Object[] { kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription() });
                }
            } else if (requestModel.getFormData() != null && ("form".equals(requestModel.getBodyType())
                    || "x-www-form-urlencoded".equals(requestModel.getBodyType()))) {
                for (KeyValueItem kv : requestModel.getFormData()) {
                    urlencodedModel
                            .addRow(new Object[] { kv.isEnabled(), kv.getKey(), kv.getValue(), kv.getDescription() });
                }
            }

            String at = requestModel.getAuthType() != null ? requestModel.getAuthType() : "inherit";
            authTypeCombo.setSelectedItem(at);
            authCardLayout.show(authCardPanel, at);
            bearerTokenField.setText(requestModel.getAuthToken() != null ? requestModel.getAuthToken() : "");
            basicUsernameField.setText(requestModel.getAuthUsername() != null ? requestModel.getAuthUsername() : "");
            basicPasswordField.setText(requestModel.getAuthPassword() != null ? requestModel.getAuthPassword() : "");
            apiKeyNameField.setText(requestModel.getAuthApiKeyName() != null ? requestModel.getAuthApiKeyName() : "");
            apiKeyValueField
                    .setText(requestModel.getAuthApiKeyValue() != null ? requestModel.getAuthApiKeyValue() : "");
            apiKeyInCombo.setSelectedItem(
                    requestModel.getAuthApiKeyIn() != null ? requestModel.getAuthApiKeyIn() : "header");
            oauth2GrantTypeCombo
                    .setSelectedItem(requestModel.getOauth2GrantType() != null ? requestModel.getOauth2GrantType()
                            : "client_credentials");
            oauth2CallbackUrlField
                    .setText(requestModel.getOauth2CallbackUrl() != null ? requestModel.getOauth2CallbackUrl() : "");
            oauth2AuthUrlField.setText(requestModel.getOauth2AuthUrl() != null ? requestModel.getOauth2AuthUrl() : "");
            oauth2AccessTokenUrlField.setText(
                    requestModel.getOauth2AccessTokenUrl() != null ? requestModel.getOauth2AccessTokenUrl() : "");
            oauth2ClientIdField
                    .setText(requestModel.getOauth2ClientId() != null ? requestModel.getOauth2ClientId() : "");
            oauth2ClientSecretField
                    .setText(requestModel.getOauth2ClientSecret() != null ? requestModel.getOauth2ClientSecret() : "");
            oauth2ScopeField.setText(requestModel.getOauth2Scope() != null ? requestModel.getOauth2Scope() : "");
            oauth2StateField.setText(requestModel.getOauth2State() != null ? requestModel.getOauth2State() : "");
            oauth2UsernameField
                    .setText(requestModel.getOauth2Username() != null ? requestModel.getOauth2Username() : "");
            oauth2PasswordField
                    .setText(requestModel.getOauth2Password() != null ? requestModel.getOauth2Password() : "");
            oauth2ClientAuthCombo.setSelectedItem(
                    requestModel.getOauth2ClientAuth() != null ? requestModel.getOauth2ClientAuth() : "header");
            oauth2AccessTokenField
                    .setText(requestModel.getOauth2AccessToken() != null ? requestModel.getOauth2AccessToken() : "");

            preScriptArea.setText(requestModel.getPreRequestScript() != null ? requestModel.getPreRequestScript() : "");
            postScriptArea
                    .setText(requestModel.getPostRequestScript() != null ? requestModel.getPostRequestScript() : "");
            String sslSetting = requestModel.getSslSetting();
            if ("INHERIT".equalsIgnoreCase(sslSetting)) {
                sslVerifyCombo.setSelectedIndex(0);
            } else if ("NO_VERIFY".equalsIgnoreCase(sslSetting)) {
                sslVerifyCombo.setSelectedIndex(1);
            } else {
                sslVerifyCombo.setSelectedIndex(2);
            }

            String redirectSetting = requestModel.getRedirectSetting();
            if ("INHERIT".equalsIgnoreCase(redirectSetting)) {
                redirectVerifyCombo.setSelectedIndex(0);
            } else if ("NO".equalsIgnoreCase(redirectSetting)) {
                redirectVerifyCombo.setSelectedIndex(1);
            } else {
                redirectVerifyCombo.setSelectedIndex(2);
            }
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
        if ("graphql".equals(bodyTypeCombo.getSelectedItem())) {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("query", graphqlQueryArea.getText());
            json.addProperty("variables", graphqlVarsArea.getText());
            requestModel.setBodyRawContent(new com.google.gson.Gson().toJson(json));
        } else {
            requestModel.setBodyRawContent(bodyArea.getText());
        }
        requestModel.setAuthType((String) authTypeCombo.getSelectedItem());
        requestModel.setAuthToken(bearerTokenField.getText());
        requestModel.setAuthUsername(basicUsernameField.getText());
        requestModel.setAuthPassword(new String(basicPasswordField.getPassword()));
        requestModel.setAuthApiKeyName(apiKeyNameField.getText());
        requestModel.setAuthApiKeyValue(apiKeyValueField.getText());
        requestModel.setAuthApiKeyIn((String) apiKeyInCombo.getSelectedItem());
        requestModel.setOauth2GrantType((String) oauth2GrantTypeCombo.getSelectedItem());
        requestModel.setOauth2CallbackUrl(oauth2CallbackUrlField.getText());
        requestModel.setOauth2AuthUrl(oauth2AuthUrlField.getText());
        requestModel.setOauth2AccessTokenUrl(oauth2AccessTokenUrlField.getText());
        requestModel.setOauth2ClientId(oauth2ClientIdField.getText());
        requestModel.setOauth2ClientSecret(oauth2ClientSecretField.getText());
        requestModel.setOauth2Scope(oauth2ScopeField.getText());
        requestModel.setOauth2State(oauth2StateField.getText());
        requestModel.setOauth2Username(oauth2UsernameField.getText());
        requestModel.setOauth2Password(new String(oauth2PasswordField.getPassword()));
        requestModel.setOauth2ClientAuth((String) oauth2ClientAuthCombo.getSelectedItem());
        requestModel.setOauth2AccessToken(oauth2AccessTokenField.getText());
        requestModel.setPreRequestScript(preScriptArea.getText());
        requestModel.setPostRequestScript(postScriptArea.getText());
        int sslIndex = sslVerifyCombo.getSelectedIndex();
        if (sslIndex == 0) {
            requestModel.setSslSetting("INHERIT");
        } else if (sslIndex == 1) {
            requestModel.setSslSetting("NO_VERIFY");
        } else {
            requestModel.setSslSetting("VERIFY");
        }
        requestModel.setSslVerification(sslIndex != 1);

        int redirectIndex = redirectVerifyCombo.getSelectedIndex();
        if (redirectIndex == 0) {
            requestModel.setRedirectSetting("INHERIT");
        } else if (redirectIndex == 1) {
            requestModel.setRedirectSetting("NO");
        } else {
            requestModel.setRedirectSetting("YES");
        }

        requestModel.setParams(extractKV(paramsModel, false));
        requestModel.setHeaders(extractKV(headersModel, true));
        requestModel.setFormData(extractFormData(formDataModel));
        requestModel.setUrlencodedData(extractKV(urlencodedModel, false));
    }

    private List<KeyValueItem> extractFormData(DefaultTableModel model) {
        List<KeyValueItem> list = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            boolean enabled = model.getValueAt(i, 0) instanceof Boolean b && b;
            String key = (String) model.getValueAt(i, 1);
            String type = (String) model.getValueAt(i, 2);
            String value = (String) model.getValueAt(i, 3);
            String desc = model.getColumnCount() > 4 ? (String) model.getValueAt(i, 4) : "";
            KeyValueItem kv = new KeyValueItem(key != null ? key : "", value != null ? value : "", enabled);
            kv.setType(type != null ? type : "text");
            kv.setDescription(desc != null ? desc : "");
            list.add(kv);
        }
        return list;
    }

    private List<KeyValueItem> extractKV(DefaultTableModel model, boolean isHeader) {
        List<KeyValueItem> list = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String desc = model.getColumnCount() > 3 ? (String) model.getValueAt(i, 3) : "";
            if (desc != null && desc.contains("<calculated>")) {
                continue;
            }
            String key = (String) model.getValueAt(i, 1);
            if (isHeader && key != null) {
                String k = key.trim();
                if (k.equalsIgnoreCase("Host") || k.equalsIgnoreCase("Content-Length")) {
                    continue;
                }
            }
            boolean enabled = model.getValueAt(i, 0) instanceof Boolean b && b;
            String value = (String) model.getValueAt(i, 2);
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
        if ("graphql".equals(bodyTypeCombo.getSelectedItem())) {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("query", graphqlQueryArea.getText());
            json.addProperty("variables", graphqlVarsArea.getText());
            m.setBodyRawContent(new com.google.gson.Gson().toJson(json));
        } else {
            m.setBodyRawContent(bodyArea.getText());
        }
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
        m.setSslVerification(sslIndex2 != 1);

        int redirectIndex2 = redirectVerifyCombo.getSelectedIndex();
        if (redirectIndex2 == 0) {
            m.setRedirectSetting("INHERIT");
        } else if (redirectIndex2 == 1) {
            m.setRedirectSetting("NO");
        } else {
            m.setRedirectSetting("YES");
        }

        m.setParams(extractKV(paramsModel, false));
        m.setHeaders(extractKV(headersModel, true));
        m.setFormData(extractFormData(formDataModel));
        m.setUrlencodedData(extractKV(urlencodedModel, false));
        return m;
    }

    public boolean hasUnsavedChanges() {
        if (originalModelJson == null)
            return false;
        RequestModel current = collectToNewModel();

        com.google.gson.Gson gson = new com.google.gson.Gson();
        RequestModel original = gson.fromJson(originalModelJson, RequestModel.class);

        normalizeRequestModel(original);
        normalizeRequestModel(current);

        String cleanOriginalJson = gson.toJson(original);
        String cleanCurrentJson = gson.toJson(current);
        return !cleanOriginalJson.equals(cleanCurrentJson);
    }

    private void normalizeRequestModel(RequestModel m) {
        if (m == null)
            return;
        m.setName(m.getName() == null ? "" : m.getName().trim());
        m.setMethod(m.getMethod() == null ? "GET" : m.getMethod().trim());
        m.setUrl(m.getUrl() == null ? "" : m.getUrl().trim());
        m.setBodyType(m.getBodyType() == null ? "none" : m.getBodyType().trim());
        if ("form".equals(m.getBodyType()))
            m.setBodyType("x-www-form-urlencoded");
        m.setBodyRawType(m.getBodyRawType() == null ? "JSON" : m.getBodyRawType().trim());
        m.setBodyRawContent(m.getBodyRawContent() == null ? "" : m.getBodyRawContent().trim());
        m.setAuthType(m.getAuthType() == null ? "none" : m.getAuthType().trim());
        m.setAuthToken(m.getAuthToken() == null ? "" : m.getAuthToken().trim());
        m.setAuthUsername(m.getAuthUsername() == null ? "" : m.getAuthUsername().trim());
        m.setAuthPassword(m.getAuthPassword() == null ? "" : m.getAuthPassword().trim());
        m.setAuthApiKeyName(m.getAuthApiKeyName() == null ? "" : m.getAuthApiKeyName().trim());
        m.setAuthApiKeyValue(m.getAuthApiKeyValue() == null ? "" : m.getAuthApiKeyValue().trim());
        m.setAuthApiKeyIn(m.getAuthApiKeyIn() == null ? "header" : m.getAuthApiKeyIn().trim());
        m.setPreRequestScript(m.getPreRequestScript() == null ? "" : m.getPreRequestScript().trim());
        m.setPostRequestScript(m.getPostRequestScript() == null ? "" : m.getPostRequestScript().trim());
        m.setType(m.getType() == null ? "request" : m.getType().trim());

        m.setHeaders(normalizeKV(m.getHeaders()));
        m.setParams(normalizeKV(m.getParams()));
        m.setFormData(normalizeKV(m.getFormData()));
        m.setUrlencodedData(normalizeKV(m.getUrlencodedData()));

        m.setTimestamp(null);
        m.setResponseStatus(null);
        m.setActualUrl(null);
        m.setComparatorTextA(null);
        m.setComparatorTextB(null);
        m.setComparatorMode(0);
    }

    private List<KeyValueItem> normalizeKV(List<KeyValueItem> list) {
        List<KeyValueItem> res = new ArrayList<>();
        if (list == null)
            return res;
        for (KeyValueItem item : list) {
            if (item == null)
                continue;
            KeyValueItem normalized = new KeyValueItem(
                    item.getKey() == null ? "" : item.getKey().trim(),
                    item.getValue() == null ? "" : item.getValue().trim(),
                    item.isEnabled());
            normalized.setDescription(item.getDescription() == null ? "" : item.getDescription().trim());
            normalized.setType(item.getType() == null ? "text" : item.getType().trim());
            res.add(normalized);
        }
        return res;
    }

    public RequestModel getRequestModel() {
        collectModel();
        return requestModel;
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
        int height = Math.max(32, size + 16);
        if (methodCombo != null) {
            methodCombo.setPreferredSize(null);
            int comboWidth = Math.max(90, methodCombo.getPreferredSize().width);
            methodCombo.setPreferredSize(new Dimension(comboWidth, height));
        }
        if (urlField != null) {
            urlField.setPreferredSize(new Dimension(0, height));
        }
        if (sendBtn != null) {
            sendBtn.setPreferredSize(null);
            sendBtn.setPreferredSize(new Dimension(sendBtn.getPreferredSize().width + 12, height));
        }
        if (saveBtn != null) {
            saveBtn.setPreferredSize(null);
            saveBtn.setPreferredSize(new Dimension(saveBtn.getPreferredSize().width + 12, height));
        }
        if (codeBtn != null) {
            codeBtn.setPreferredSize(null);
            codeBtn.setPreferredSize(new Dimension(codeBtn.getPreferredSize().width + 12, height));
        }
        if (graphqlQueryArea != null) {
            graphqlQueryArea.setFont(new Font("JetBrains Mono", Font.PLAIN, size - 2));
        }
        if (graphqlVarsArea != null) {
            graphqlVarsArea.setFont(new Font("JetBrains Mono", Font.PLAIN, size - 2));
        }
        revalidate();
        repaint();
    }

    private void fetchIntrospectionSchema() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid URL first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String introspectionQuery = "query IntrospectionQuery {\n" +
                "  __schema {\n" +
                "    queryType { name }\n" +
                "    mutationType { name }\n" +
                "    subscriptionType { name }\n" +
                "    types {\n" +
                "      kind\n" +
                "      name\n" +
                "      description\n" +
                "    }\n" +
                "  }\n" +
                "}";

        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("query", introspectionQuery);
        payload.add("variables", new com.google.gson.JsonObject());

        RequestModel snapshot = new RequestModel();
        snapshot.setId(java.util.UUID.randomUUID().toString());
        snapshot.setName("GraphQL Introspection");
        snapshot.setUrl(url);
        snapshot.setMethod("POST");
        snapshot.setBodyType("raw");
        snapshot.setBodyRawType("JSON");
        snapshot.setBodyRawContent(new com.google.gson.Gson().toJson(payload));

        snapshot.setAuthType((String) authTypeCombo.getSelectedItem());
        snapshot.setAuthToken(bearerTokenField.getText());
        snapshot.setAuthUsername(basicUsernameField.getText());
        snapshot.setAuthPassword(new String(basicPasswordField.getPassword()));
        snapshot.setAuthApiKeyName(apiKeyNameField.getText());
        snapshot.setAuthApiKeyValue(apiKeyValueField.getText());
        snapshot.setAuthApiKeyIn((String) apiKeyInCombo.getSelectedItem());

        snapshot.setOauth2GrantType((String) oauth2GrantTypeCombo.getSelectedItem());
        snapshot.setOauth2CallbackUrl(oauth2CallbackUrlField.getText());
        snapshot.setOauth2AuthUrl(oauth2AuthUrlField.getText());
        snapshot.setOauth2AccessTokenUrl(oauth2AccessTokenUrlField.getText());
        snapshot.setOauth2ClientId(oauth2ClientIdField.getText());
        snapshot.setOauth2ClientSecret(oauth2ClientSecretField.getText());
        snapshot.setOauth2Scope(oauth2ScopeField.getText());
        snapshot.setOauth2State(oauth2StateField.getText());
        snapshot.setOauth2Username(oauth2UsernameField.getText());
        snapshot.setOauth2Password(new String(oauth2PasswordField.getPassword()));
        snapshot.setOauth2ClientAuth((String) oauth2ClientAuthCombo.getSelectedItem());
        snapshot.setOauth2AccessToken(oauth2AccessTokenField.getText());

        snapshot.setHeaders(extractKV(headersModel, true));

        sendBtn.setEnabled(false);
        sendBtn.setText("Introspecting...");
        responsePanel.reset();

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
                    MainFrame.showToast(RequestPanel.this, "GraphQL Introspection Schema loaded.");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(RequestPanel.this,
                            "Introspection failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    sendBtn.setEnabled(true);
                    sendBtn.setText("Send");
                }
            }
        };
        worker.execute();
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
        if (url == null || url.trim().isEmpty())
            return list;
        int qIdx = url.indexOf('?');
        if (qIdx < 0)
            return list;
        String queryStr = url.substring(qIdx + 1);
        if (queryStr.isEmpty())
            return list;
        String[] pairs = queryStr.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty())
                continue;
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
        if (s == null)
            return "";
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
            paramsModel
                    .addRow(new Object[] { item.isEnabled(), item.getKey(), item.getValue(), item.getDescription() });
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
                    if (sb.length() > 0)
                        sb.append("&");
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


