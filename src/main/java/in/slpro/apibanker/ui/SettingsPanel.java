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
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class SettingsPanel extends JPanel {
    private final MainFrame mainFrame;
    private final StorageManager storage;

    private JTextField dirField;
    private JTextField logsDirField;
    private JComboBox<String> themeCombo;
    private JCheckBox loggingCheck;
    private JComboBox<String> sslPolicyCombo;
    private JComboBox<String> redirectPolicyCombo;

    public SettingsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.storage = StorageManager.getInstance();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(UIManager.getColor("Panel.background"));

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("⚙ Settings");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel);
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

        // Global SSL row
        gbc.gridx = 0;
        gbc.gridy = 4;
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
        gbc.gridy = 5;
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

        // Empty space filler
        gbc.gridx = 0;
        gbc.gridy = 6;
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

        JButton cancelBtn = new JButton("Close Tab");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.addActionListener(e -> {
            mainFrame.closeTab(this);
        });

        btnsPanel.add(cancelBtn);
        btnsPanel.add(saveBtn);
        add(btnsPanel, BorderLayout.SOUTH);
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
        storage.getSettings().setEnableLogging(loggingCheck.isSelected());
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

        storage.saveSettings();
        MainFrame.showToast(this, "Settings saved. Restart ApiBanker to apply theme changes.");
    }
    
    public boolean hasUnsavedChanges() {
        var s = storage.getSettings();
        if (!dirField.getText().trim().equals(s.getDataDirectory())) return true;
        if (!logsDirField.getText().trim().equals(s.getLogsDirectory())) return true;
        if (!themeCombo.getSelectedItem().toString().equals(s.getTheme())) return true;
        if (loggingCheck.isSelected() != s.isEnableLogging()) return true;
        
        int sslIndex = sslPolicyCombo.getSelectedIndex();
        String sslVal = "VERIFY";
        if (sslIndex == 1) {
            sslVal = "NO_VERIFY";
        } else if (sslIndex == 2) {
            sslVal = "VERIFY_FORCED";
        } else if (sslIndex == 3) {
            sslVal = "NO_VERIFY_FORCED";
        }
        if (!sslVal.equals(s.getGlobalSslSetting())) return true;
        
        return false;
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
    }
}


