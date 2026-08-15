package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import in.slpro.apibanker.model.KeyValueItem;
import in.slpro.apibanker.storage.StorageManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * GlobalVariablesPanel
 *
 * <p>
 * Core functionality and implementation logic for GlobalVariablesPanel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.6.0-beta
 * @since 1.0.0
 */
public class GlobalVariablesPanel extends JPanel {
    private final DefaultTableModel globalsTableModel;
    private boolean isLoading = false;

    public GlobalVariablesPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(UIManager.getColor("Panel.background"));
        titlePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        JLabel titleLabel = new JLabel("Global Variables");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createHorizontalStrut(8));
        titlePanel.add(mainFrame.createInfoBadge("sec-global-vars", "View Global Variable Manager Guide"));
        add(titlePanel, BorderLayout.NORTH);

        globalsTableModel = new DefaultTableModel(new String[] { "", "Variable", "Value" }, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return true;
            }
        };
        JTable varTable = new JTable(globalsTableModel);
        varTable.getColumnModel().getColumn(0).setMaxWidth(30);
        varTable.setRowHeight(24);

        setupTableCopyPaste(varTable, globalsTableModel);

        globalsTableModel.addTableModelListener(e -> {
            if (isLoading) return;
            SwingUtilities.invokeLater(() -> {
                boolean changed = false;
                int rowCount = globalsTableModel.getRowCount();
                if (rowCount == 0) {
                    globalsTableModel.addRow(new Object[] { true, "", "" });
                    changed = true;
                } else {
                    String key = (String) globalsTableModel.getValueAt(rowCount - 1, 1);
                    String val = (String) globalsTableModel.getValueAt(rowCount - 1, 2);
                    if ((key != null && !key.isBlank()) || (val != null && !val.isBlank())) {
                        globalsTableModel.addRow(new Object[] { true, "", "" });
                        changed = true;
                    }
                }
                
                int editingRow = varTable.getEditingRow();
                for (int i = globalsTableModel.getRowCount() - 2; i >= 0; i--) {
                    String k = (String) globalsTableModel.getValueAt(i, 1);
                    String v = (String) globalsTableModel.getValueAt(i, 2);
                    if ((k == null || k.isBlank()) && (v == null || v.isBlank())) {
                        if (i != editingRow) {
                            globalsTableModel.removeRow(i);
                            changed = true;
                        }
                    }
                }
                if (!changed) {
                    autoSave();
                }
            });
        });

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(varTable);
        centerContainer.add(scrollPane, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);

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

        // Load existing
        loadModel();
    }

    public void loadModel() {
        if (globalsTableModel == null)
            return;
        isLoading = true;
        globalsTableModel.setRowCount(0);
        List<KeyValueItem> globals = StorageManager.getInstance().getSettings().getGlobalVariables();
        if (globals != null) {
            for (KeyValueItem kv : globals) {
                globalsTableModel.addRow(new Object[] { kv.isEnabled(), kv.getKey(), kv.getValue() });
            }
        }
        globalsTableModel.addRow(new Object[] { true, "", "" });
        isLoading = false;
    }

    private void autoSave() {
        List<KeyValueItem> newGlobals = new ArrayList<>();
        for (int i = 0; i < globalsTableModel.getRowCount(); i++) {
            boolean enabled = (Boolean) globalsTableModel.getValueAt(i, 0);
            String key = (String) globalsTableModel.getValueAt(i, 1);
            String value = (String) globalsTableModel.getValueAt(i, 2);
            if ((key != null && !key.isBlank()) || (value != null && !value.isBlank())) {
                newGlobals.add(new KeyValueItem(key != null ? key : "", value != null ? value : "", enabled));
            }
        }
        StorageManager.getInstance().getSettings().setGlobalVariables(newGlobals);
        StorageManager.getInstance().saveSettings();
    }

    public void updateFontSize(int size) {
        FontScaleHelper.scaleFonts(this, size);
        revalidate();
        repaint();
    }

    public static void setupTableCopyPaste(JTable table, DefaultTableModel model) {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        javax.swing.undo.UndoManager undoManager = new javax.swing.undo.UndoManager();
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                        "undo");
        table.getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    if (table.getCellEditor() != null)
                        table.getCellEditor().stopCellEditing();
                    undoManager.undo();
                }
            }
        });

        Action deleteAction = new AbstractAction("Delete Row(s)") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = table.getSelectedRows();
                if (rows.length == 0)
                    return;

                java.util.List<Integer> editableRowsList = new java.util.ArrayList<>();
                for (int r : rows) {
                    int modelRow = table.convertRowIndexToModel(r);
                    if (model.isCellEditable(modelRow, 1)) {
                        editableRowsList.add(modelRow);
                    }
                }
                if (editableRowsList.isEmpty()) return;
                
                int[] modelRows = editableRowsList.stream().mapToInt(i -> i).toArray();
                java.util.Arrays.sort(modelRows);

                Object[][] deletedData = new Object[modelRows.length][model.getColumnCount()];
                for (int i = 0; i < modelRows.length; i++) {
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        deletedData[i][c] = model.getValueAt(modelRows[i], c);
                    }
                }

                if (table.getCellEditor() != null)
                    table.getCellEditor().stopCellEditing();

                for (int i = modelRows.length - 1; i >= 0; i--) {
                    model.removeRow(modelRows[i]);
                }

                undoManager.addEdit(new javax.swing.undo.AbstractUndoableEdit() {
                    @Override
                    public void undo() {
                        super.undo();
                        for (int i = 0; i < modelRows.length; i++) {
                            model.insertRow(modelRows[i], deletedData[i]);
                        }
                    }

                    @Override
                    public void redo() {
                        super.redo();
                        for (int i = modelRows.length - 1; i >= 0; i--) {
                            model.removeRow(modelRows[i]);
                        }
                    }
                });
            }
        };

        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0),
                "deleteRow");
        table.getActionMap().put("deleteRow", deleteAction);

        Action copyAction = new AbstractAction("Copy") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = table.getSelectedRows();
                if (rows.length == 0)
                    return;
                StringBuilder sb = new StringBuilder();
                for (int r : rows) {
                    int modelRow = table.convertRowIndexToModel(r);
                    if (!model.isCellEditable(modelRow, 1)) continue;
                    
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        Object val = model.getValueAt(modelRow, c);
                        if (c == 0 && val instanceof Boolean) {
                            sb.append(val);
                        } else {
                            sb.append(val != null ? val.toString() : "");
                        }
                        if (c < model.getColumnCount() - 1) sb.append("\t");
                    }
                    sb.append("\n");
                }
                if (sb.length() == 0) return;
                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(
                        sb.toString());
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            }
        };

        Action pasteAction = new AbstractAction("Paste") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String data = (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                            .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                    if (data == null || data.isEmpty())
                        return;
                    String[] lines = data.split("\n");

                    if (table.getCellEditor() != null)
                        table.getCellEditor().stopCellEditing();

                    int initialRowCount = model.getRowCount();
                    int insertedCount = 0;

                    for (String line : lines) {
                        if (line.trim().isEmpty())
                            continue;
                        String[] parts = line.split("\t", -1);
                        if (parts.length >= 2) {
                            Object[] newRow = new Object[model.getColumnCount()];
                            for (int c = 0; c < model.getColumnCount(); c++) {
                                if (c < parts.length) {
                                    if (c == 0 && model.getColumnClass(0) == Boolean.class) {
                                        newRow[c] = Boolean.parseBoolean(parts[c]);
                                    } else {
                                        newRow[c] = parts[c];
                                    }
                                } else {
                                    if (c == 0 && model.getColumnClass(0) == Boolean.class) {
                                        newRow[c] = true;
                                    } else {
                                        newRow[c] = "";
                                    }
                                }
                            }
                            model.addRow(newRow);
                            insertedCount++;
                        }
                    }

                    final int added = insertedCount;
                    undoManager.addEdit(new javax.swing.undo.AbstractUndoableEdit() {
                        @Override
                        public void undo() {
                            super.undo();
                            for (int i = 0; i < added; i++) {
                                model.removeRow(initialRowCount);
                            }
                        }

                        @Override
                        public void redo() {
                            super.redo();
                            // Simplified redo just says sorry, can't easily redo paste without saving all
                            // data again
                            // But usually undo is sufficient
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        table.getActionMap().put("copy", copyAction);
        table.getActionMap().put("paste", pasteAction);

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem(copyAction);
        JMenuItem pasteItem = new JMenuItem(pasteAction);
        JMenuItem delItem = new JMenuItem(deleteAction);
        popupMenu.add(copyItem);
        popupMenu.add(pasteItem);
        popupMenu.addSeparator();
        popupMenu.add(delItem);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                showPopup(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        int modelRow = table.convertRowIndexToModel(row);
                        if (!model.isCellEditable(modelRow, 1)) {
                            return;
                        }
                    }
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
}

