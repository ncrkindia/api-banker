package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import in.slpro.apibanker.storage.StorageManager;

import java.awt.*;

/**
 * SettingsPanel
 *
 * <p>
 * Core functionality and implementation logic for SettingsPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.6.0-beta
 * @since 1.0.0
 */
public class SettingsPanel extends JPanel {
    private final MainFrame mainFrame;
    private final StorageManager storage;

    private JTextField dirField;
    private JTextField logsDirField;
    private JComboBox<String> themeCombo;
    private JComboBox<String> uiModeCombo;
    private JCheckBox loggingCheck;
    private JCheckBox actionAuditCheck;
    private JCheckBox stricterEditingCheck;
    private JComboBox<String> sslPolicyCombo;
    private JComboBox<String> redirectPolicyCombo;
    private JComboBox<String> timeoutPolicyCombo;
    private JTextField timeoutValueField;

    public SettingsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.storage = StorageManager.getInstance();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(UIManager.getColor("Panel.background"));

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("Settings ");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(mainFrame.createInfoBadge("sec-settings", "View Settings Guide"));
        add(headerPanel, BorderLayout.NORTH);

        // Content Panel (GridBagLayout)
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Data directory row
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel dirLabel = new JLabel("Data Directory:");
        dirLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(dirLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        dirField = new JTextField(storage.getSettings().getDataDirectory());
        dirField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(dirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseBtn = new JButton("Browse");
        browseBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(storage.getSettings().getDataDirectory());
            fc.setDialogTitle("Select Storage Folder for ApiBanker");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                dirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        contentPanel.add(browseBtn, gbc);

        // Logs directory row
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel logsDirLabel = new JLabel("Logs Directory:");
        logsDirLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(logsDirLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        logsDirField = new JTextField(storage.getSettings().getLogsDirectory());
        logsDirField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(logsDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browseLogsBtn = new JButton("Browse");
        browseLogsBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        browseLogsBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(storage.getSettings().getLogsDirectory());
            fc.setDialogTitle("Select Logs Folder for ApiBanker");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                logsDirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        contentPanel.add(browseLogsBtn, gbc);

        // Theme row
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JLabel themeLabel = new JLabel("Theme:");
        themeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(themeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        themeCombo = new JComboBox<>(new String[] { "light", "dark" });
        themeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        themeCombo.setSelectedItem(storage.getSettings().getTheme());
        contentPanel.add(themeCombo, gbc);

        // Logging row
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        JLabel loggingLabel = new JLabel("Enable Request Logging:");
        loggingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(loggingLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        loggingCheck = new JCheckBox();
        loggingCheck.setSelected(storage.getSettings().isEnableLogging());
        contentPanel.add(loggingCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        JLabel actionAuditLabel = new JLabel("Enable Action Audit Log:");
        actionAuditLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(actionAuditLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        actionAuditCheck = new JCheckBox();
        actionAuditCheck.setSelected(storage.getSettings().isEnableActionAuditLog());
        contentPanel.add(actionAuditCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        JLabel stricterEditingLabel = new JLabel("Mode Stricter Editing:");
        stricterEditingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(stricterEditingLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        stricterEditingCheck = new JCheckBox();
        stricterEditingCheck.setSelected(storage.getSettings().isStricterEditing());
        contentPanel.add(stricterEditingCheck, gbc);

        // Global SSL row
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0;
        JLabel sslLabel = new JLabel("SSL Verification Policy:");
        sslLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(sslLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        sslPolicyCombo = new JComboBox<>(new String[] {
                "Verify",
                "Don't Verify",
                "Verify (FORCED)",
                "Don't Verify (FORCED)"
        });
        sslPolicyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        String currentSsl = storage.getSettings().getGlobalSslSetting();
        if ("NO_VERIFY".equalsIgnoreCase(currentSsl)) {
            sslPolicyCombo.setSelectedIndex(1);
        } else if ("VERIFY_FORCED".equalsIgnoreCase(currentSsl)) {
            sslPolicyCombo.setSelectedIndex(2);
        } else if ("NO_VERIFY_FORCED".equalsIgnoreCase(currentSsl)) {
            sslPolicyCombo.setSelectedIndex(3);
        } else {
            sslPolicyCombo.setSelectedIndex(0);
        }
        contentPanel.add(sslPolicyCombo, gbc);

        // Global Redirect row
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 0;
        JLabel redirectLabel = new JLabel("Auto Redirect Policy (302):");
        redirectLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(redirectLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        redirectPolicyCombo = new JComboBox<>(new String[] {
                "Yes (Follow)",
                "No (Don't Follow)",
                "Yes (FORCED)",
                "No (FORCED)"
        });
        redirectPolicyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        String currentRedirect = storage.getSettings().getGlobalRedirectSetting();
        if ("NO".equalsIgnoreCase(currentRedirect)) {
            redirectPolicyCombo.setSelectedIndex(1);
        } else if ("YES_FORCED".equalsIgnoreCase(currentRedirect)) {
            redirectPolicyCombo.setSelectedIndex(2);
        } else if ("NO_FORCED".equalsIgnoreCase(currentRedirect)) {
            redirectPolicyCombo.setSelectedIndex(3);
        } else {
            redirectPolicyCombo.setSelectedIndex(0);
        }
        contentPanel.add(redirectPolicyCombo, gbc);

        // Global Timeout row
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.weightx = 0;
        JLabel timeoutLabel = new JLabel("Connection Timeout Policy:");
        timeoutLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(timeoutLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JPanel timeoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        timeoutPanel.setOpaque(false);
        timeoutPolicyCombo = new JComboBox<>(new String[] {
                "Default (120s)",
                "Custom (Optional)",
                "Custom (FORCED)"
        });
        timeoutPolicyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        String currentTimeout = storage.getSettings().getGlobalTimeoutSetting();
        if ("CUSTOM_OPTIONAL".equalsIgnoreCase(currentTimeout)) {
            timeoutPolicyCombo.setSelectedIndex(1);
        } else if ("CUSTOM_FORCED".equalsIgnoreCase(currentTimeout)) {
            timeoutPolicyCombo.setSelectedIndex(2);
        } else {
            timeoutPolicyCombo.setSelectedIndex(0);
        }
        timeoutPanel.add(timeoutPolicyCombo);

        JLabel secondsLabel = new JLabel("Seconds:");
        timeoutPanel.add(secondsLabel);
        timeoutValueField = new JTextField(String.valueOf(storage.getSettings().getGlobalTimeoutValue()), 5);
        timeoutValueField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        timeoutValueField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar()))
                    e.consume();
            }
        });
        timeoutValueField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) {
                try {
                    int val = Integer.parseInt(timeoutValueField.getText().trim());
                    if (val < 1)
                        timeoutValueField.setText("1");
                    if (val > 1200)
                        timeoutValueField.setText("1200");
                } catch (NumberFormatException ex) {
                    timeoutValueField.setText("120");
                }
            }
        });
        timeoutPanel.add(timeoutValueField);

        timeoutPolicyCombo.addActionListener(e -> {
            boolean isCustom = timeoutPolicyCombo.getSelectedIndex() > 0;
            timeoutValueField.setVisible(isCustom);
            secondsLabel.setVisible(isCustom);
        });
        boolean initialCustom = timeoutPolicyCombo.getSelectedIndex() > 0;
        timeoutValueField.setVisible(initialCustom);
        secondsLabel.setVisible(initialCustom);

        contentPanel.add(timeoutPanel, gbc);

        // UI Mode row
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.weightx = 0;
        JLabel uiModeLabel = new JLabel("UI Rendering Mode:");
        uiModeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        contentPanel.add(uiModeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        uiModeCombo = new JComboBox<>(new String[] { "Classic", "Modern" });
        uiModeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        uiModeCombo.setSelectedItem(storage.getSettings().getUiMode());
        contentPanel.add(uiModeCombo, gbc);

        // Empty space filler
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        contentPanel.add(Box.createGlue(), gbc);

        add(contentPanel, BorderLayout.CENTER);

        // Buttons Panel (FlowLayout Right)
        JPanel btnsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnsPanel.setOpaque(false);

        JButton saveBtn = new JButton("Save Settings");
        Color accent = UIManager.getColor("AccentColor");
        saveBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.addActionListener(e -> saveSettings());

        btnsPanel.add(saveBtn);
        add(btnsPanel, BorderLayout.SOUTH);

        // Add Ctrl+S shortcut
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                "saveSettings");
        am.put("saveSettings", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveSettings();
            }
        });
    }

    public void saveSettings() {
        String newDir = dirField.getText().trim();
        boolean dirChanged = !newDir.equals(storage.getSettings().getDataDirectory());

        storage.updateDataDirectory(newDir);

        if (dirChanged) {
            storage.saveCollections(mainFrame.getCollections());
            storage.saveEnvironments(mainFrame.getEnvironments());
            storage.saveHistory(mainFrame.getHistoryList());
        }

        storage.updateLogsDirectory(logsDirField.getText().trim());
        storage.getSettings().setTheme((String) themeCombo.getSelectedItem());
        storage.getSettings().setUiMode((String) uiModeCombo.getSelectedItem());
        storage.getSettings().setEnableLogging(loggingCheck.isSelected());
        storage.getSettings().setEnableActionAuditLog(actionAuditCheck.isSelected());
        storage.getSettings().setStricterEditing(stricterEditingCheck.isSelected());
        in.slpro.apibanker.logger.ConsoleLogger.getInstance().setEnableLogging(loggingCheck.isSelected());

        int sslIndex = sslPolicyCombo.getSelectedIndex();
        String sslVal = "VERIFY";
        if (sslIndex == 1) {
            sslVal = "NO_VERIFY";
        } else if (sslIndex == 2) {
            sslVal = "VERIFY_FORCED";
        } else if (sslIndex == 3) {
            sslVal = "NO_VERIFY_FORCED";
        }
        storage.getSettings().setGlobalSslSetting(sslVal);

        int redirectIndex = redirectPolicyCombo.getSelectedIndex();
        String redirectVal = "YES";
        if (redirectIndex == 1) {
            redirectVal = "NO";
        } else if (redirectIndex == 2) {
            redirectVal = "YES_FORCED";
        } else if (redirectIndex == 3) {
            redirectVal = "NO_FORCED";
        }
        storage.getSettings().setGlobalRedirectSetting(redirectVal);

        int timeoutIndex = timeoutPolicyCombo.getSelectedIndex();
        String timeoutVal = "DEFAULT";
        if (timeoutIndex == 1) {
            timeoutVal = "CUSTOM_OPTIONAL";
        } else if (timeoutIndex == 2) {
            timeoutVal = "CUSTOM_FORCED";
        }
        storage.getSettings().setGlobalTimeoutSetting(timeoutVal);

        try {
            int tVal = Integer.parseInt(timeoutValueField.getText().trim());
            storage.getSettings().setGlobalTimeoutValue(tVal);
        } catch (NumberFormatException e) {
            storage.getSettings().setGlobalTimeoutValue(120);
        }

        storage.saveSettings();
        MainFrame.showToast(this, "Settings saved. Restart ApiBanker to apply theme changes.");
    }

    public boolean hasUnsavedChanges() {
        var s = storage.getSettings();
        if (!dirField.getText().trim().equals(s.getDataDirectory()))
            return true;
        if (!logsDirField.getText().trim().equals(s.getLogsDirectory()))
            return true;
        if (!themeCombo.getSelectedItem().toString().equals(s.getTheme()))
            return true;
        if (!uiModeCombo.getSelectedItem().toString().equals(s.getUiMode()))
            return true;
        if (loggingCheck.isSelected() != s.isEnableLogging())
            return true;
        if (actionAuditCheck.isSelected() != s.isEnableActionAuditLog())
            return true;
        if (stricterEditingCheck.isSelected() != s.isStricterEditing())
            return true;

        int sslIndex = sslPolicyCombo.getSelectedIndex();
        String sslVal = "VERIFY";
        if (sslIndex == 1) {
            sslVal = "NO_VERIFY";
        } else if (sslIndex == 2) {
            sslVal = "VERIFY_FORCED";
        } else if (sslIndex == 3) {
            sslVal = "NO_VERIFY_FORCED";
        }
        if (!sslVal.equals(s.getGlobalSslSetting()))
            return true;

        int redirectIndex = redirectPolicyCombo.getSelectedIndex();
        String redirectVal = "YES";
        if (redirectIndex == 1) {
            redirectVal = "NO";
        } else if (redirectIndex == 2) {
            redirectVal = "YES_FORCED";
        } else if (redirectIndex == 3) {
            redirectVal = "NO_FORCED";
        }
        if (!redirectVal.equals(s.getGlobalRedirectSetting()))
            return true;

        int timeoutIndex = timeoutPolicyCombo.getSelectedIndex();
        String timeoutVal = "DEFAULT";
        if (timeoutIndex == 1) {
            timeoutVal = "CUSTOM_OPTIONAL";
        } else if (timeoutIndex == 2) {
            timeoutVal = "CUSTOM_FORCED";
        }
        if (!timeoutVal.equals(s.getGlobalTimeoutSetting()))
            return true;

        try {
            int tVal = Integer.parseInt(timeoutValueField.getText().trim());
            if (tVal != s.getGlobalTimeoutValue())
                return true;
        } catch (NumberFormatException e) {
            if (s.getGlobalTimeoutValue() != 120)
                return true;
        }

        return false;
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
    }
}
