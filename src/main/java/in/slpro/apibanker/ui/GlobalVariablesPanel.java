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

public class GlobalVariablesPanel extends JPanel {
    private final MainFrame mainFrame;
    private final DefaultTableModel globalsTableModel;

    public GlobalVariablesPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        JLabel titleLabel = new JLabel("Global Variables");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

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

        JPanel varBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        varBtns.setOpaque(false);
        JButton addVarBtn = new JButton("+ Add Variable");
        JButton delVarBtn = new JButton("Delete Row");
        addVarBtn.addActionListener(e -> globalsTableModel.addRow(new Object[] { true, "", "" }));
        delVarBtn.addActionListener(e -> {
            Action delAction = varTable.getActionMap().get("deleteRow");
            if (delAction != null) {
                delAction.actionPerformed(new ActionEvent(varTable, ActionEvent.ACTION_PERFORMED, null));
            }
        });
        varBtns.add(addVarBtn);
        varBtns.add(delVarBtn);

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(varTable);
        centerContainer.add(scrollPane, BorderLayout.CENTER);
        centerContainer.add(varBtns, BorderLayout.SOUTH);
        add(centerContainer, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomBar.setOpaque(false);
        bottomBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> mainFrame.closeTab(this));
        JButton saveBtn = new JButton("Save All");
        saveBtn.addActionListener(e -> {
            if (varTable.getCellEditor() != null)
                varTable.getCellEditor().stopCellEditing();
            saveGlobals();
        });
        bottomBar.add(cancelBtn);
        bottomBar.add(saveBtn);
        add(bottomBar, BorderLayout.SOUTH);

        // Load existing
        loadModel();
    }

    public void loadModel() {
        if (globalsTableModel == null)
            return;
        globalsTableModel.setRowCount(0);
        List<KeyValueItem> globals = StorageManager.getInstance().getSettings().getGlobalVariables();
        if (globals != null) {
            for (KeyValueItem kv : globals) {
                globalsTableModel.addRow(new Object[] { kv.isEnabled(), kv.getKey(), kv.getValue() });
            }
        }
    }

    private void saveGlobals() {
        List<KeyValueItem> newGlobals = new ArrayList<>();
        for (int i = 0; i < globalsTableModel.getRowCount(); i++) {
            boolean enabled = (Boolean) globalsTableModel.getValueAt(i, 0);
            String key = (String) globalsTableModel.getValueAt(i, 1);
            String value = (String) globalsTableModel.getValueAt(i, 2);
            if (key != null && !key.isBlank()) {
                newGlobals.add(new KeyValueItem(key, value, enabled));
            }
        }
        StorageManager.getInstance().getSettings().setGlobalVariables(newGlobals);
        StorageManager.getInstance().saveSettings();
        mainFrame.closeTab(this);
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

                Object[][] deletedData = new Object[rows.length][model.getColumnCount()];
                for (int i = 0; i < rows.length; i++) {
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        deletedData[i][c] = model.getValueAt(rows[i], c);
                    }
                }

                int[] sortedRows = java.util.Arrays.copyOf(rows, rows.length);
                java.util.Arrays.sort(sortedRows);

                if (table.getCellEditor() != null)
                    table.getCellEditor().stopCellEditing();

                for (int i = sortedRows.length - 1; i >= 0; i--) {
                    model.removeRow(sortedRows[i]);
                }

                undoManager.addEdit(new javax.swing.undo.AbstractUndoableEdit() {
                    @Override
                    public void undo() {
                        super.undo();
                        for (int i = 0; i < sortedRows.length; i++) {
                            model.insertRow(sortedRows[i], deletedData[i]);
                        }
                    }

                    @Override
                    public void redo() {
                        super.redo();
                        for (int i = sortedRows.length - 1; i >= 0; i--) {
                            model.removeRow(sortedRows[i]);
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
                    Boolean enabled = (Boolean) model.getValueAt(r, 0);
                    String key = (String) model.getValueAt(r, 1);
                    String value = (String) model.getValueAt(r, 2);
                    sb.append(enabled != null ? enabled : true).append("\t")
                            .append(key != null ? key : "").append("\t")
                            .append(value != null ? value : "").append("\n");
                }
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
                            boolean enabled = true;
                            String key = "";
                            String value = "";
                            if (parts.length == 3) {
                                enabled = Boolean.parseBoolean(parts[0]);
                                key = parts[1];
                                value = parts[2];
                            } else if (parts.length >= 2) {
                                key = parts[0];
                                value = parts[1];
                            }
                            model.addRow(new Object[] { enabled, key, value });
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
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }
}
