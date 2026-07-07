package in.slpro.japi.ui;

import in.slpro.japi.http.CookieJar;
import in.slpro.japi.model.CookieModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CookieJarDialog extends JDialog {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JTextField filterField;

    public CookieJarDialog(JFrame parent) {
        super(parent, "Cookie Jar Manager", true);
        setSize(700, 450);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        headerPanel.add(new JLabel("<html><b>Manage captured or manual cookies</b><br/>Cookies will be attached to outgoing requests matching domain/path.</html>"), BorderLayout.WEST);
        
        // Filter field
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        filterPanel.add(new JLabel("Search Domain:"));
        filterField = new JTextField(15);
        filterField.addCaretListener(e -> refreshTable());
        filterPanel.add(filterField);
        headerPanel.add(filterPanel, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new Object[]{"Domain", "Name", "Value", "Path", "Secure", "HttpOnly"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UIManager.getColor("Separator.foreground")));
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton addBtn = new JButton("Add Cookie");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton clearAllBtn = new JButton("Clear All");
        JButton closeBtn = new JButton("Close");

        addBtn.addActionListener(e -> showAddEditDialog(null));
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String domain = (String) table.getValueAt(row, 0);
                String name = (String) table.getValueAt(row, 1);
                CookieModel selected = findCookie(domain, name);
                if (selected != null) {
                    showAddEditDialog(selected);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a cookie to edit.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                String domain = (String) table.getValueAt(row, 0);
                String name = (String) table.getValueAt(row, 1);
                CookieModel selected = findCookie(domain, name);
                if (selected != null) {
                    CookieJar.getInstance().removeCookie(selected);
                    refreshTable();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a cookie to delete.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        clearAllBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear all cookies?", "Confirm Clear", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                CookieJar.getInstance().clear();
                refreshTable();
            }
        });
        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(clearAllBtn);
        
        // Push close to the right
        JPanel actionContainer = new JPanel(new BorderLayout());
        actionContainer.add(buttonPanel, BorderLayout.WEST);
        
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        closePanel.add(closeBtn);
        actionContainer.add(closePanel, BorderLayout.EAST);

        add(actionContainer, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String query = filterField.getText().trim().toLowerCase();
        List<CookieModel> list = CookieJar.getInstance().getCookies();
        for (CookieModel c : list) {
            if (query.isEmpty() || c.getDomain().toLowerCase().contains(query) || c.getName().toLowerCase().contains(query)) {
                tableModel.addRow(new Object[]{
                        c.getDomain(),
                        c.getName(),
                        c.getValue(),
                        c.getPath(),
                        c.isSecure() ? "Yes" : "No",
                        c.isHttpOnly() ? "Yes" : "No"
                });
            }
        }
    }

    private CookieModel findCookie(String domain, String name) {
        for (CookieModel c : CookieJar.getInstance().getCookies()) {
            if (c.getDomain().equalsIgnoreCase(domain) && c.getName().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    private void showAddEditDialog(CookieModel existing) {
        JDialog dialog = new JDialog(this, existing == null ? "Add Cookie" : "Edit Cookie", true);
        dialog.setSize(400, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField domainField = new JTextField(existing != null ? existing.getDomain() : "example.com");
        JTextField nameField = new JTextField(existing != null ? existing.getName() : "");
        JTextField valueField = new JTextField(existing != null ? existing.getValue() : "");
        JTextField pathField = new JTextField(existing != null ? existing.getPath() : "/");
        JCheckBox secureCheck = new JCheckBox("Secure", existing != null && existing.isSecure());
        JCheckBox httpOnlyCheck = new JCheckBox("HttpOnly", existing != null && existing.isHttpOnly());

        if (existing != null) {
            domainField.setEditable(false);
            nameField.setEditable(false);
        }

        gbc.gridx = 0; gbc.gridy = 0; dialog.add(new JLabel("Domain:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; dialog.add(domainField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0; dialog.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; dialog.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; dialog.add(new JLabel("Value:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; dialog.add(valueField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0; dialog.add(new JLabel("Path:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; dialog.add(pathField, gbc);

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkPanel.add(secureCheck);
        checkPanel.add(Box.createHorizontalStrut(10));
        checkPanel.add(httpOnlyCheck);
        gbc.gridx = 1; gbc.gridy = 4; dialog.add(checkPanel, gbc);

        // Buttons
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton saveBtn = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");

        saveBtn.addActionListener(e -> {
            String domain = domainField.getText().trim();
            String name = nameField.getText().trim();
            String value = valueField.getText().trim();
            String path = pathField.getText().trim();

            if (domain.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Domain and Name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            CookieModel cookie = new CookieModel(name, value, domain, path);
            cookie.setSecure(secureCheck.isSelected());
            cookie.setHttpOnly(httpOnlyCheck.isSelected());

            CookieJar.getInstance().addCookie(cookie);
            dialog.dispose();
            refreshTable();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dbPanel.add(saveBtn);
        dbPanel.add(cancelBtn);

        gbc.gridx = 1; gbc.gridy = 5; gbc.weighty = 1.0; gbc.anchor = GridBagConstraints.SOUTH;
        dialog.add(dbPanel, gbc);

        dialog.setVisible(true);
    }
}
