package in.slpro.japi.ui;

import in.slpro.japi.model.EnvironmentModel;
import in.slpro.japi.model.KeyValueItem;
import in.slpro.japi.storage.StorageManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * EnvironmentManagerDialog
 *
 * <p>
 * Core functionality and implementation logic for EnvironmentManagerDialog.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.1.0-beta
 * @since 1.0.0
 */
public class EnvironmentManagerDialog extends JDialog {
    private final MainFrame mainFrame;
    private final List<EnvironmentModel> environments;
    private final DefaultListModel<String> envListModel;
    private final JList<String> envList;
    private final DefaultTableModel varTableModel;
    private JTextField envNameField;
    private int selectedEnvIndex = -1;

    public EnvironmentManagerDialog(MainFrame mainFrame) {
        super(mainFrame, "Environment Manager", true);
        this.mainFrame = mainFrame;
        this.environments = new ArrayList<>(mainFrame.getEnvironments());
        setSize(750, 500);
        setLocationRelativeTo(mainFrame);
        setLayout(new BorderLayout());

        // Left panel - environment list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));

        JPanel leftHeader = new JPanel(new BorderLayout());
        leftHeader.setBackground(new Color(248, 249, 250));
        leftHeader.setBorder(new EmptyBorder(8, 10, 8, 10));
        leftHeader.add(new JLabel("Environments"), BorderLayout.WEST);

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        leftBtns.setOpaque(false);
        JButton addEnvBtn = new JButton("+");
        addEnvBtn.setToolTipText("Add Environment");
        addEnvBtn.addActionListener(e -> addEnvironment());
        JButton delEnvBtn = new JButton("×");
        delEnvBtn.setToolTipText("Delete Environment");
        delEnvBtn.addActionListener(e -> deleteEnvironment());
        leftBtns.add(addEnvBtn);
        leftBtns.add(delEnvBtn);
        leftHeader.add(leftBtns, BorderLayout.EAST);
        leftPanel.add(leftHeader, BorderLayout.NORTH);

        envListModel = new DefaultListModel<>();
        envList = new JList<>(envListModel);
        envList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        envList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedEnv();
        });
        leftPanel.add(new JScrollPane(envList), BorderLayout.CENTER);

        // Right panel - variables
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel namePanel = new JPanel(new BorderLayout(8, 0));
        namePanel.add(new JLabel("Name:"), BorderLayout.WEST);
        envNameField = new JTextField();
        namePanel.add(envNameField, BorderLayout.CENTER);
        JButton renameBtn = new JButton("Rename");
        renameBtn.addActionListener(e -> renameEnvironment());
        namePanel.add(renameBtn, BorderLayout.EAST);
        rightPanel.add(namePanel, BorderLayout.NORTH);

        varTableModel = new DefaultTableModel(new String[]{"", "Variable", "Value"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
        JTable varTable = new JTable(varTableModel);
        varTable.getColumnModel().getColumn(0).setMaxWidth(30);
        varTable.setRowHeight(24);

        JPanel varBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton addVarBtn = new JButton("+ Add Variable");
        JButton delVarBtn = new JButton("Delete Row");
        addVarBtn.addActionListener(e -> varTableModel.addRow(new Object[]{true, "", ""}));
        delVarBtn.addActionListener(e -> {
            int row = varTable.getSelectedRow();
            if (row >= 0) varTableModel.removeRow(row);
        });
        varBtns.add(addVarBtn);
        varBtns.add(delVarBtn);

        rightPanel.add(varBtns, BorderLayout.SOUTH);
        rightPanel.add(new JScrollPane(varTable), BorderLayout.CENTER);

        // Bottom buttons
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save All");
        saveBtn.setBackground(new Color(46, 204, 113));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> saveAll(varTable));
        JButton importBtn = new JButton("Import Postman Env");
        importBtn.addActionListener(e -> importPostman());
        JButton exportBtn = new JButton("Export");
        exportBtn.addActionListener(e -> exportCurrentEnv(varTable));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        bottomBar.add(importBtn);
        bottomBar.add(exportBtn);
        bottomBar.add(cancelBtn);
        bottomBar.add(saveBtn);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);

        // Load environments
        for (EnvironmentModel env : environments) {
            envListModel.addElement(env.getName());
        }
        if (!envListModel.isEmpty()) {
            envList.setSelectedIndex(0);
        }
    }

    private void addEnvironment() {
        String name = JOptionPane.showInputDialog(this, "Environment name:");
        if (name == null || name.isBlank()) return;
        EnvironmentModel env = new EnvironmentModel();
        env.setId(UUID.randomUUID().toString());
        env.setName(name);
        env.setVariables(new ArrayList<>());
        environments.add(env);
        envListModel.addElement(name);
        envList.setSelectedIndex(environments.size() - 1);
    }

    private void deleteEnvironment() {
        int idx = envList.getSelectedIndex();
        if (idx < 0) return;
        environments.remove(idx);
        envListModel.remove(idx);
        selectedEnvIndex = -1;
        varTableModel.setRowCount(0);
        envNameField.setText("");
        if (!envListModel.isEmpty()) envList.setSelectedIndex(Math.min(idx, envListModel.size() - 1));
    }

    private void loadSelectedEnv() {
        // Save current before switching
        if (selectedEnvIndex >= 0 && selectedEnvIndex < environments.size()) {
            saveCurrentToModel(selectedEnvIndex);
        }
        int idx = envList.getSelectedIndex();
        selectedEnvIndex = idx;
        if (idx < 0 || idx >= environments.size()) return;
        EnvironmentModel env = environments.get(idx);
        envNameField.setText(env.getName());
        varTableModel.setRowCount(0);
        if (env.getVariables() != null) {
            for (KeyValueItem kv : env.getVariables()) {
                varTableModel.addRow(new Object[]{kv.isEnabled(), kv.getKey(), kv.getValue()});
            }
        }
    }

    private void saveCurrentToModel(int idx) {
        if (idx < 0 || idx >= environments.size()) return;
        EnvironmentModel env = environments.get(idx);
        String name = envNameField.getText().trim();
        if (!name.isBlank()) {
            env.setName(name);
            envListModel.set(idx, name);
        }
        List<KeyValueItem> vars = new ArrayList<>();
        for (int i = 0; i < varTableModel.getRowCount(); i++) {
            boolean enabled = (Boolean) varTableModel.getValueAt(i, 0);
            String key = (String) varTableModel.getValueAt(i, 1);
            String value = (String) varTableModel.getValueAt(i, 2);
            if (key != null && !key.isBlank()) {
                vars.add(new KeyValueItem(key, value, enabled));
            }
        }
        env.setVariables(vars);
    }

    private void renameEnvironment() {
        int idx = envList.getSelectedIndex();
        if (idx < 0) return;
        String name = envNameField.getText().trim();
        if (name.isBlank()) return;
        environments.get(idx).setName(name);
        envListModel.set(idx, name);
    }

    private void saveAll(JTable varTable) {
        if (varTable.getCellEditor() != null) {
            varTable.getCellEditor().stopCellEditing();
        }
        if (selectedEnvIndex >= 0) saveCurrentToModel(selectedEnvIndex);
        mainFrame.setEnvironments(environments);
        StorageManager.getInstance().saveEnvironments(environments);
        dispose();
    }

    private void importPostman() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Postman Environment (.json)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            String json = java.nio.file.Files.readString(chooser.getSelectedFile().toPath());
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            EnvironmentModel env = new EnvironmentModel();
            env.setId(UUID.randomUUID().toString());
            env.setName(obj.has("name") ? obj.get("name").getAsString() : "Imported");
            List<KeyValueItem> vars = new ArrayList<>();
            if (obj.has("values") && obj.get("values").isJsonArray()) {
                for (com.google.gson.JsonElement el : obj.getAsJsonArray("values")) {
                    com.google.gson.JsonObject v = el.getAsJsonObject();
                    String key = v.has("key") ? v.get("key").getAsString() : "";
                    String value = v.has("value") ? v.get("value").getAsString() : "";
                    boolean enabled = !v.has("enabled") || v.get("enabled").getAsBoolean();
                    vars.add(new KeyValueItem(key, value, enabled));
                }
            }
            env.setVariables(vars);
            environments.add(env);
            envListModel.addElement(env.getName());
            envList.setSelectedIndex(environments.size() - 1);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Import failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCurrentEnv(JTable varTable) {
        int idx = envList.getSelectedIndex();
        if (idx < 0) return;
        if (varTable.getCellEditor() != null) {
            varTable.getCellEditor().stopCellEditing();
        }
        saveCurrentToModel(idx);
        EnvironmentModel env = environments.get(idx);
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(env.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            root.addProperty("id", env.getId());
            root.addProperty("name", env.getName());
            root.addProperty("_postman_exported_using", "JAPI/" + in.slpro.japi.App.getVersion());
            com.google.gson.JsonArray values = new com.google.gson.JsonArray();
            if (env.getVariables() != null) {
                for (KeyValueItem kv : env.getVariables()) {
                    com.google.gson.JsonObject v = new com.google.gson.JsonObject();
                    v.addProperty("key", kv.getKey());
                    v.addProperty("value", kv.getValue());
                    v.addProperty("enabled", kv.isEnabled());
                    v.addProperty("type", "default");
                    values.add(v);
                }
            }
            root.add("values", values);
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), gson.toJson(root));
            MainFrame.showToast(this, "Exported successfully.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
