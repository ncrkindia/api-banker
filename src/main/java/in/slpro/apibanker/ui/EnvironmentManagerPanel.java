package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import in.slpro.apibanker.model.EnvironmentModel;
import in.slpro.apibanker.model.KeyValueItem;
import in.slpro.apibanker.storage.StorageManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * EnvironmentManagerPanel
 */
/**
 * EnvironmentManagerPanel
 *
 * <p>
 * Core functionality and implementation logic for EnvironmentManagerPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.6.0-beta
 * @since 1.0.0
 */
public class EnvironmentManagerPanel extends JPanel {
    private final MainFrame mainFrame;
    private final List<EnvironmentModel> environments;
    private final DefaultListModel<String> envListModel;
    private final JList<String> envList;
    private final DefaultTableModel varTableModel;
    private int selectedEnvIndex = -1;
    private java.util.Stack<List<EnvironmentModel>> undoStack = new java.util.Stack<>();
    private static List<EnvironmentModel> clipboardEnvironments = new ArrayList<>();
    private boolean isLoadingEnv = false;

    public EnvironmentManagerPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.environments = new ArrayList<>(mainFrame.getEnvironments());
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        // Left panel - environment list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(240, 0));
        leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
                UIManager.getColor("Separator.foreground") != null ? UIManager.getColor("Separator.foreground")
                        : new Color(220, 220, 220)));
        leftPanel.setBackground(UIManager.getColor("Panel.background"));

        JPanel leftHeader = new JPanel();
        leftHeader.setLayout(new BoxLayout(leftHeader, BoxLayout.Y_AXIS));
        leftHeader.setBackground(UIManager.getColor("Panel.background"));
        leftHeader.setBorder(new EmptyBorder(8, 10, 8, 10));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(UIManager.getColor("Panel.background"));
        titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel titleLabel = new JLabel("Environments");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createHorizontalStrut(5));
        titlePanel.add(mainFrame.createInfoBadge("sec-env-manager", "View Environment Manager Guide"));
        leftHeader.add(titlePanel);
        leftHeader.add(Box.createVerticalStrut(4));

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        leftBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton addEnvBtn = new JButton("+");
        addEnvBtn.setToolTipText("Add Environment");
        addEnvBtn.addActionListener(e -> addEnvironment());
        JButton delEnvBtn = new JButton("×");
        delEnvBtn.setToolTipText("Delete Environment");
        delEnvBtn.addActionListener(e -> deleteEnvironment());
        leftBtns.add(addEnvBtn);
        leftBtns.add(delEnvBtn);
        leftHeader.add(leftBtns);
        leftPanel.add(leftHeader, BorderLayout.NORTH);

        envListModel = new DefaultListModel<>();
        envList = new JList<>(envListModel);
        envList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        envList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                loadSelectedEnv();
        });
        leftPanel.add(new JScrollPane(envList), BorderLayout.CENTER);

        // Right panel - variables
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        rightPanel.setBackground(UIManager.getColor("Panel.background"));

        varTableModel = new DefaultTableModel(new String[] { "", "Variable", "Value" }, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return true;
            }
        };
        JTable varTable = new JTable(varTableModel);
        varTable.getColumnModel().getColumn(0).setMaxWidth(30);
        varTable.setRowHeight(24);

        GlobalVariablesPanel.setupTableCopyPaste(varTable, varTableModel);

        varTableModel.addTableModelListener(e -> {
            if (isLoadingEnv) return;
            SwingUtilities.invokeLater(() -> {
                boolean changed = false;
                int rowCount = varTableModel.getRowCount();
                if (rowCount == 0) {
                    varTableModel.addRow(new Object[] { true, "", "" });
                    changed = true;
                } else {
                    String key = (String) varTableModel.getValueAt(rowCount - 1, 1);
                    String val = (String) varTableModel.getValueAt(rowCount - 1, 2);
                    if ((key != null && !key.isBlank()) || (val != null && !val.isBlank())) {
                        varTableModel.addRow(new Object[] { true, "", "" });
                        changed = true;
                    }
                }
                
                int editingRow = varTable.getEditingRow();
                for (int i = varTableModel.getRowCount() - 2; i >= 0; i--) {
                    String k = (String) varTableModel.getValueAt(i, 1);
                    String v = (String) varTableModel.getValueAt(i, 2);
                    if ((k == null || k.isBlank()) && (v == null || v.isBlank())) {
                        if (i != editingRow) {
                            varTableModel.removeRow(i);
                            changed = true;
                        }
                    }
                }
                if (!changed) {
                    autoSave();
                }
            });
        });

        JPanel rightContainer = new JPanel(new BorderLayout());
        rightContainer.setOpaque(false);
        JScrollPane varTableScroll = new JScrollPane(varTable);
        varTableScroll.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        rightContainer.add(varTableScroll, BorderLayout.CENTER);
        rightPanel.add(rightContainer, BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {}
            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {
                if (varTable.getCellEditor() != null) varTable.getCellEditor().stopCellEditing();
                autoSave();
            }
            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });

        envList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                boolean ctrl = e.isControlDown();
                int code = e.getKeyCode();
                if (code == java.awt.event.KeyEvent.VK_F2) {
                    renameSelected();
                } else if (code == java.awt.event.KeyEvent.VK_DELETE) {
                    deleteSelected();
                } else if (ctrl && code == java.awt.event.KeyEvent.VK_C) {
                    copySelected();
                } else if (ctrl && code == java.awt.event.KeyEvent.VK_V) {
                    pasteEnvironments();
                } else if (ctrl && code == java.awt.event.KeyEvent.VK_Z) {
                    undo();
                }
            }
        });

        envList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = envList.locationToIndex(e.getPoint());
                    if (row != -1 && !envList.isSelectedIndex(row)) {
                        envList.setSelectedIndex(row);
                    }
                    showContextMenu(e.getX(), e.getY());
                }
            }
        });

        // Load environments
        loadModel();
    }

    private void pushUndoState() {
        List<EnvironmentModel> copy = new ArrayList<>();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        for (EnvironmentModel e : environments) {
            copy.add(gson.fromJson(gson.toJson(e), EnvironmentModel.class));
        }
        undoStack.push(copy);
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        
        if (StorageManager.getInstance().getSettings().isStricterEditing()) {
            int confirm = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), 
                    "Are you sure you want to undo the last environment change?", 
                    "Confirm Undo", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        
        List<EnvironmentModel> prev = undoStack.pop();
        environments.clear();
        environments.addAll(prev);
        envListModel.clear();
        for (EnvironmentModel env : environments) {
            envListModel.addElement(env.getName());
        }
        selectedEnvIndex = -1;
        varTableModel.setRowCount(0);
        if (!envListModel.isEmpty()) {
            envList.setSelectedIndex(0);
        }
        in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("UNDO_ENVIRONMENT_ACTION", "User", "Restored previous environment state.");
        MainFrame.showToast(this, "Undo successful");
    }

    private void autoSave() {
        if (selectedEnvIndex >= 0) {
            saveCurrentToModel(selectedEnvIndex);
        }
        mainFrame.setEnvironments(environments);
        StorageManager.getInstance().saveEnvironments(environments);
    }

    private void renameSelected() {
        int idx = envList.getSelectedIndex();
        if (idx < 0) return;
        Rectangle bounds = envList.getCellBounds(idx, idx);
        if (bounds == null) return;
        
        JTextField editor = new JTextField(environments.get(idx).getName());
        editor.setBounds(bounds);
        envList.add(editor);
        editor.requestFocus();
        editor.selectAll();
        
        Action finishEdit = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                String text = editor.getText();
                if (!text.isBlank()) {
                    pushUndoState();
                    environments.get(idx).setName(text);
                    envListModel.set(idx, text);
                    autoSave();
                }
                envList.remove(editor);
                envList.repaint();
                envList.requestFocus();
            }
        };
        editor.addActionListener(finishEdit);
        editor.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent e) { finishEdit.actionPerformed(null); }
        });
        envList.repaint();
    }

    private void deleteSelected() {
        int[] indices = envList.getSelectedIndices();
        if (indices.length == 0) return;
        
        if (StorageManager.getInstance().getSettings().isStricterEditing()) {
            int confirm = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), 
                    "Are you sure you want to delete the selected " + indices.length + " environment(s)?", 
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        
        pushUndoState();
        for (int i = indices.length - 1; i >= 0; i--) {
            int idx = indices[i];
            EnvironmentModel deletedEnv = environments.get(idx);
            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("DELETE_ENVIRONMENT", "User", "Deleted: [" + deletedEnv.getName() + " / " + deletedEnv.getId() + "]");
            environments.remove(idx);
            envListModel.remove(idx);
        }
        selectedEnvIndex = -1;
        isLoadingEnv = true;
        varTableModel.setRowCount(0);
        isLoadingEnv = false;
        if (!envListModel.isEmpty()) {
            envList.setSelectedIndex(Math.min(indices[0], envListModel.size() - 1));
        }
        autoSave();
    }

    private void copySelected() {
        int[] indices = envList.getSelectedIndices();
        if (indices.length == 0) return;
        clipboardEnvironments.clear();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        for (int idx : indices) {
            EnvironmentModel copy = gson.fromJson(gson.toJson(environments.get(idx)), EnvironmentModel.class);
            clipboardEnvironments.add(copy);
        }
        MainFrame.showToast(this, "Copied " + indices.length + " environment(s)");
    }

    private void pasteEnvironments() {
        if (clipboardEnvironments.isEmpty()) return;
        
        if (StorageManager.getInstance().getSettings().isStricterEditing()) {
            int confirm = JOptionPane.showConfirmDialog(SwingUtilities.getWindowAncestor(this), 
                    "Are you sure you want to paste/duplicate " + clipboardEnvironments.size() + " environment(s)?", 
                    "Confirm Paste", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }
        
        pushUndoState();
        com.google.gson.Gson gson = new com.google.gson.Gson();
        for (EnvironmentModel env : clipboardEnvironments) {
            EnvironmentModel copy = gson.fromJson(gson.toJson(env), EnvironmentModel.class);
            copy.setId(UUID.randomUUID().toString());
            copy.setName(copy.getName() + " (Copy)");
            environments.add(copy);
            envListModel.addElement(copy.getName());
            in.slpro.apibanker.logger.ActionAuditLogger.getInstance().logAction("COPY_ENVIRONMENT", "User", "Source: [" + env.getName() + " / " + env.getId() + "] -> Copied to: [" + copy.getName() + " / " + copy.getId() + "]");
        }
        envList.setSelectedIndex(environments.size() - 1);
    }

    private void showContextMenu(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem rename = new JMenuItem("Rename");
        rename.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        rename.addActionListener(e -> renameSelected());
        
        JMenuItem copy = new JMenuItem("Copy");
        copy.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        copy.addActionListener(e -> copySelected());
        
        JMenuItem paste = new JMenuItem("Paste");
        paste.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        paste.addActionListener(e -> pasteEnvironments());
        
        JMenuItem delete = new JMenuItem("Delete");
        delete.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        delete.addActionListener(e -> deleteSelected());
        
        JMenuItem undo = new JMenuItem("Undo");
        undo.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        undo.addActionListener(e -> undo());
        
        menu.add(rename);
        menu.add(copy);
        menu.add(paste);
        menu.add(delete);
        menu.addSeparator();
        menu.add(undo);
        
        menu.show(envList, x, y);
    }

    public void loadModel() {
        if (envListModel == null)
            return;
        int currentIdx = envList.getSelectedIndex();
        String currentName = null;
        if (currentIdx >= 0 && currentIdx < environments.size()) {
            currentName = environments.get(currentIdx).getName();
            // Force save current to model before we reload from external changes
            saveCurrentToModel(currentIdx);
        }

        // Reset selected index so that the ListSelectionListener doesn't save to the
        // wrong index
        selectedEnvIndex = -1;

        environments.clear();
        if (mainFrame.getEnvironments() != null) {
            // deeply copy to avoid same reference issues but we want real time updates so
            // just clear and copy the list. Wait, mainFrame.getEnvironments() elements are
            // mutable.
            // If we want real time updates, we just copy the list.
            for (EnvironmentModel m : mainFrame.getEnvironments()) {
                // If we don't clone, the UI edits will mutate the main frame's objects
                // immediately, which contradicts "Save All" button.
                // Wait, if it contradicts, we should clone them? Yes, in original code it's
                // `new ArrayList<>(mainFrame.getEnvironments());` which copies the list, not
                // the objects.
                // So editing the table modifies the EnvironmentModel objects directly!
                // Ah, the original code doesn't deep copy either. So "Save All" just saves to
                // StorageManager.
                environments.add(m);
            }
        }

        envListModel.clear();
        for (EnvironmentModel env : environments) {
            envListModel.addElement(env.getName());
        }

        if (currentName != null) {
            int newIdx = -1;
            for (int i = 0; i < environments.size(); i++) {
                if (environments.get(i).getName().equals(currentName)) {
                    newIdx = i;
                    break;
                }
            }
            if (newIdx >= 0) {
                envList.setSelectedIndex(newIdx);
            } else if (!envListModel.isEmpty()) {
                envList.setSelectedIndex(0);
            }
        } else if (!envListModel.isEmpty()) {
            envList.setSelectedIndex(0);
        }
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
        revalidate();
        repaint();
    }

    private void addEnvironment() {
        pushUndoState();
        String name = "New Environment";
        EnvironmentModel env = new EnvironmentModel();
        env.setId(UUID.randomUUID().toString());
        env.setName(name);
        env.setVariables(new ArrayList<>());
        environments.add(env);
        envListModel.addElement(name);
        int idx = environments.size() - 1;
        envList.setSelectedIndex(idx);
        envList.ensureIndexIsVisible(idx);
        autoSave();
        SwingUtilities.invokeLater(this::renameSelected);
    }

    private void deleteEnvironment() {
        deleteSelected();
    }

    private void loadSelectedEnv() {
        if (selectedEnvIndex >= 0 && selectedEnvIndex < environments.size()) {
            saveCurrentToModel(selectedEnvIndex);
        }
        int idx = envList.getSelectedIndex();
        selectedEnvIndex = idx;
        if (idx < 0 || idx >= environments.size())
            return;
        EnvironmentModel env = environments.get(idx);
        isLoadingEnv = true;
        varTableModel.setRowCount(0);
        if (env.getVariables() != null) {
            for (KeyValueItem kv : env.getVariables()) {
                varTableModel.addRow(new Object[] { kv.isEnabled(), kv.getKey(), kv.getValue() });
            }
        }
        varTableModel.addRow(new Object[] { true, "", "" });
        isLoadingEnv = false;
        autoSave();
    }

    private void saveCurrentToModel(int idx) {
        if (idx < 0 || idx >= environments.size())
            return;
        EnvironmentModel env = environments.get(idx);
        List<KeyValueItem> vars = new ArrayList<>();
        for (int i = 0; i < varTableModel.getRowCount(); i++) {
            boolean enabled = (Boolean) varTableModel.getValueAt(i, 0);
            String key = (String) varTableModel.getValueAt(i, 1);
            String value = (String) varTableModel.getValueAt(i, 2);
            if ((key != null && !key.isBlank()) || (value != null && !value.isBlank())) {
                vars.add(new KeyValueItem(key != null ? key : "", value != null ? value : "", enabled));
            }
        }
        env.setVariables(vars);
    }

    public void refreshEnvironments(List<EnvironmentModel> newEnvs) {
        List<EnvironmentModel> copy = new ArrayList<>(newEnvs);
        this.environments.clear();
        this.environments.addAll(copy);
        envListModel.clear();
        for (EnvironmentModel env : environments) {
            envListModel.addElement(env.getName());
        }
        selectedEnvIndex = -1;
        isLoadingEnv = true;
        varTableModel.setRowCount(0);
        isLoadingEnv = false;
        if (!environments.isEmpty()) {
            envList.setSelectedIndex(0);
        }
    }
}


