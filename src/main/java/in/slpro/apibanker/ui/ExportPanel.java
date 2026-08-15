package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import in.slpro.apibanker.model.CollectionModel;
import in.slpro.apibanker.model.EnvironmentModel;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ExportPanel
 *
 * <p>
 * Core functionality and implementation logic for ExportPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.6.0-beta
 * @since 1.0.0
 */
public class ExportPanel extends JPanel {
    private final MainFrame mainFrame;
    private final List<JCheckBox> colCheckboxes = new ArrayList<>();
    private final List<JCheckBox> envCheckboxes = new ArrayList<>();
    private final String exportType;
    private JTextField dirField;

    /**
     * Constructs a new ExportPanel to handle exporting of collections and environments.
     *
     * @param mainFrame  The parent MainFrame reference to access application state.
     * @param exportType The format to export ("apibanker" or "postman").
     */
    public ExportPanel(MainFrame mainFrame, String exportType) {
        this.mainFrame = mainFrame;
        this.exportType = exportType;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(UIManager.getColor("Panel.background"));

        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        String titleText = "apibanker".equals(exportType) ? "📤 Export ApiBanker Files" : "📤 Export Postman Files";
        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Content Panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;

        // Collections
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        contentPanel.add(createListPanel("Collections", mainFrame.getCollections(), colCheckboxes), gbc);

        // Environments
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        contentPanel.add(createListPanel("Environments", mainFrame.getEnvironments(), envCheckboxes), gbc);

        add(contentPanel, BorderLayout.CENTER);

        // Footer / Action Panel
        JPanel footerPanel = new JPanel(new GridBagLayout());
        footerPanel.setOpaque(false);
        GridBagConstraints fGbc = new GridBagConstraints();
        fGbc.insets = new Insets(10, 10, 10, 10);
        fGbc.fill = GridBagConstraints.HORIZONTAL;

        fGbc.gridx = 0;
        fGbc.gridy = 0;
        fGbc.weightx = 0;
        JLabel dirLabel = new JLabel("Export Location:");
        dirLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        footerPanel.add(dirLabel, fGbc);

        fGbc.gridx = 1;
        fGbc.weightx = 1;
        dirField = new JTextField();
        if (MainFrame.lastFileChooserDirectory != null) {
            dirField.setText(MainFrame.lastFileChooserDirectory.getAbsolutePath());
        }
        dirField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        footerPanel.add(dirField, fGbc);

        fGbc.gridx = 2;
        fGbc.weightx = 0;
        JButton browseBtn = new JButton("Browse");
        browseBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(MainFrame.lastFileChooserDirectory);
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                MainFrame.lastFileChooserDirectory = fc.getSelectedFile();
                dirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        footerPanel.add(browseBtn, fGbc);

        JPanel actionBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtnPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Close Tab");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.setPreferredSize(new Dimension(100, 32));
        cancelBtn.addActionListener(e -> mainFrame.closeTab(this));

        JButton exportBtn = new JButton("Export");
        Color accent = UIManager.getColor("AccentColor");
        exportBtn.setBackground(accent != null ? accent : new Color(52, 152, 219));
        exportBtn.setForeground(Color.WHITE);
        exportBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        exportBtn.setPreferredSize(new Dimension(130, 32));
        exportBtn.addActionListener(e -> performExport());

        actionBtnPanel.add(cancelBtn);
        actionBtnPanel.add(exportBtn);

        fGbc.gridx = 0;
        fGbc.gridy = 1;
        fGbc.gridwidth = 3;
        footerPanel.add(actionBtnPanel, fGbc);

        add(footerPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a standardized selection panel for a list of exportable items.
     *
     * @param title      The title of the panel (e.g., "Collections").
     * @param items      The list of models to display (CollectionModel or EnvironmentModel).
     * @param checkboxes The internal list to populate with created JCheckBox instances.
     * @return A constructed JPanel containing the select-all controls and item checkboxes.
     */
    private JPanel createListPanel(String title, List<?> items, List<JCheckBox> checkboxes) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1), title));

        JPanel listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(UIManager.getColor("Panel.background"));

        for (Object item : items) {
            String name = "";
            if (item instanceof CollectionModel col) {
                name = col.getName();
            } else if (item instanceof EnvironmentModel env) {
                name = env.getName();
            }
            JCheckBox cb = new JCheckBox(name);
            cb.putClientProperty("itemModel", item);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            cb.setBackground(UIManager.getColor("Panel.background"));
            checkboxes.add(cb);
            listContainer.add(cb);
        }

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel selectAllPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectAllPanel.setOpaque(false);
        JButton selectAllBtn = new JButton("Select All");
        selectAllBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        selectAllBtn.addActionListener(e -> checkboxes.forEach(cb -> cb.setSelected(true)));

        JButton selectNoneBtn = new JButton("Select None");
        selectNoneBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        selectNoneBtn.addActionListener(e -> checkboxes.forEach(cb -> cb.setSelected(false)));

        selectAllPanel.add(selectAllBtn);
        selectAllPanel.add(selectNoneBtn);
        panel.add(selectAllPanel, BorderLayout.NORTH);

        return panel;
    }

    /**
     * Executes the export operation for all selected collections and environments,
     * writing them to the chosen directory in the specified format.
     */
    private void performExport() {
        String dirPath = dirField.getText();
        if (dirPath == null || dirPath.trim().isEmpty()) {
            MainFrame.showToast(this, "Please select an export folder.");
            return;
        }
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        boolean exportedSomething = false;
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

        try {
            for (JCheckBox cb : colCheckboxes) {
                if (cb.isSelected()) {
                    CollectionModel col = (CollectionModel) cb.getClientProperty("itemModel");
                    String suffix = "apibanker".equals(exportType) ? "_apibanker_collection" : "_postman_collection";
                    File colFile = new File(dir, col.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + suffix + ".json");
                    try (java.io.FileWriter fw = new java.io.FileWriter(colFile)) {
                        if ("apibanker".equals(exportType)) {
                            com.google.gson.JsonObject root = gson.toJsonTree(col).getAsJsonObject();
                            com.google.gson.JsonObject metadata = new com.google.gson.JsonObject();
                            metadata.addProperty("exported_by", "ApiBanker v" + in.slpro.apibanker.App.getVersion());
                            metadata.addProperty("exported_at",
                                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                            .format(new java.util.Date()));
                            root.add("_metadata", metadata);
                            gson.toJson(root, fw);
                        } else {
                            // Postman export structure
                            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
                            com.google.gson.JsonObject info = new com.google.gson.JsonObject();
                            info.addProperty("name", col.getName());
                            info.addProperty("schema",
                                    "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
                            root.add("info", info);
                            com.google.gson.JsonArray items = new com.google.gson.JsonArray();
                            mainFrame.exportCollectionRecursive(col, items);
                            root.add("item", items);
                            com.google.gson.JsonArray events = new com.google.gson.JsonArray();
                            if (col.getPreRequestScript() != null && !col.getPreRequestScript().isEmpty()) {
                                com.google.gson.JsonObject ev = new com.google.gson.JsonObject();
                                ev.addProperty("listen", "prerequest");
                                com.google.gson.JsonObject script = new com.google.gson.JsonObject();
                                script.addProperty("type", "text/javascript");
                                com.google.gson.JsonArray exec = new com.google.gson.JsonArray();
                                for (String line : col.getPreRequestScript().replace("apibanker.", "pm.").split("\n"))
                                    exec.add(line);
                                script.add("exec", exec);
                                ev.add("script", script);
                                events.add(ev);
                            }
                            if (col.getPostRequestScript() != null && !col.getPostRequestScript().isEmpty()) {
                                com.google.gson.JsonObject ev = new com.google.gson.JsonObject();
                                ev.addProperty("listen", "test");
                                com.google.gson.JsonObject script = new com.google.gson.JsonObject();
                                script.addProperty("type", "text/javascript");
                                com.google.gson.JsonArray exec = new com.google.gson.JsonArray();
                                for (String line : col.getPostRequestScript().replace("apibanker.", "pm.").split("\n"))
                                    exec.add(line);
                                script.add("exec", exec);
                                ev.add("script", script);
                                events.add(ev);
                            }
                            if (events.size() > 0)
                                root.add("event", events);
                            gson.toJson(root, fw);
                        }
                    }
                    exportedSomething = true;
                    String format = "apibanker".equals(exportType) ? "ApiBanker" : "Postman";
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("EXPORT_COLLECTION", "User", "Format: " + format + ", Source: [" + col.getName() + " / " + col.getId() + "] -> Exported to: " + colFile.getAbsolutePath());
                }
            }

            for (JCheckBox cb : envCheckboxes) {
                if (cb.isSelected()) {
                    EnvironmentModel env = (EnvironmentModel) cb.getClientProperty("itemModel");
                    String suffix = "apibanker".equals(exportType) ? "_apibanker_environment" : "_postman_environment";
                    File envFile = new File(dir, env.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + suffix + ".json");
                    try (java.io.FileWriter fw = new java.io.FileWriter(envFile)) {
                        if ("apibanker".equals(exportType)) {
                            com.google.gson.JsonObject root = gson.toJsonTree(env).getAsJsonObject();
                            com.google.gson.JsonObject metadata = new com.google.gson.JsonObject();
                            metadata.addProperty("exported_by", "ApiBanker v" + in.slpro.apibanker.App.getVersion());
                            metadata.addProperty("exported_at",
                                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                            .format(new java.util.Date()));
                            root.add("_metadata", metadata);
                            gson.toJson(root, fw);
                        } else {
                            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
                            root.addProperty("name", env.getName());
                            root.addProperty("_postman_variable_scope", "environment");
                            com.google.gson.JsonArray values = new com.google.gson.JsonArray();
                            if (env.getVariables() != null) {
                                for (in.slpro.apibanker.model.KeyValueItem kv : env.getVariables()) {
                                    com.google.gson.JsonObject val = new com.google.gson.JsonObject();
                                    val.addProperty("key", kv.getKey());
                                    val.addProperty("value", kv.getValue());
                                    val.addProperty("enabled", kv.isEnabled());
                                    values.add(val);
                                }
                            }
                            root.add("values", values);
                            gson.toJson(root, fw);
                        }
                    }
                    exportedSomething = true;
                    String format = "apibanker".equals(exportType) ? "ApiBanker" : "Postman";
                    in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("EXPORT_ENVIRONMENT", "User", "Format: " + format + ", Source: [" + env.getName() + " / " + env.getId() + "] -> Exported to: " + envFile.getAbsolutePath());
                }
            }

            if (exportedSomething) {
                MainFrame.showToast(this, "Export successful!");
            } else {
                MainFrame.showToast(this, "Please select at least one item to export.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }
}


