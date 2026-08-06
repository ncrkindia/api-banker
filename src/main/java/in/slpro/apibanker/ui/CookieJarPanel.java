package in.slpro.apibanker.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import in.slpro.apibanker.http.CookieJar;
import in.slpro.apibanker.model.CookieModel;

import java.awt.*;
import java.util.List;

/**
 * CookieJarPanel
 *
 * <p>
 * This panel provides a UI for managing the persistent {@link CookieJar}.
 * It allows users to view, search, manually add, edit, and delete cookies
 * that are automatically injected into requests. The table visually groups
 * cookies by Domain and Path.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class CookieJarPanel extends JPanel {
    private final MainFrame mainFrame;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JTextField filterField;
    private int currentFontSize = 14;

    /**
     * Constructs the Cookie Jar Manager interface.
     * <p>
     * Initializes the filterable cookie table, binding its rows to the underlying
     * {@link CookieJar} singleton. Also initializes the CRUD (Create, Read, Update,
     * Delete)
     * action buttons and dialogs for manual cookie override scenarios.
     * </p>
     * 
     * @param mainFrame The root application window.
     */
    public CookieJarPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        headerPanel.setBackground(UIManager.getColor("Panel.background"));

        JLabel titleLabel = new JLabel(
                "<html><b>Cookie Jar Manager</b><br/><font color='#7f8c8d'>Manage captured or manual cookies. These cookies will be attached to outgoing requests matching their domain and path.</font></html>");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterPanel.setOpaque(false);
        JLabel searchLabel = new JLabel("Search Domain:");
        searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterPanel.add(searchLabel);

        filterField = new JTextField(15);
        filterField.addCaretListener(e -> refreshTable());
        filterPanel.add(filterField);
        headerPanel.add(filterPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Table Model
        tableModel = new DefaultTableModel(new Object[] { "Domain", "Name", "Value", "Path", "Secure", "HttpOnly" },
                0) {
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
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                UIManager.getColor("Separator.foreground") != null ? UIManager.getColor("Separator.foreground")
                        : new Color(220, 220, 220)));
        add(scrollPane, BorderLayout.CENTER);

        // Action Buttons
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        leftButtonPanel.setOpaque(false);

        JButton addBtn = new JButton("Add Cookie");
        JButton editBtn = new JButton("Edit");
        JButton deleteBtn = new JButton("Delete");
        JButton clearAllBtn = new JButton("Clear All");

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
                JOptionPane.showMessageDialog(this, "Please select a cookie to edit.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "Please select a cookie to delete.", "Info",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        clearAllBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear all cookies?",
                    "Confirm Clear", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                CookieJar.getInstance().clear();
                refreshTable();
            }
        });

        leftButtonPanel.add(addBtn);
        leftButtonPanel.add(editBtn);
        leftButtonPanel.add(deleteBtn);
        leftButtonPanel.add(clearAllBtn);

        // Close button aligned on right
        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        rightButtonPanel.setOpaque(false);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> mainFrame.closeTab(this));
        rightButtonPanel.add(closeBtn);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIManager.getColor("Panel.background"));
        bottomPanel.add(leftButtonPanel, BorderLayout.WEST);
        bottomPanel.add(rightButtonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        refreshTable();
    }

    public void updateFontSize(int size) {
        this.currentFontSize = size;
        FontScaleHelper.scaleFonts(this, size);
        revalidate();
        repaint();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String query = filterField.getText().trim().toLowerCase();
        List<CookieModel> list = CookieJar.getInstance().getCookies();
        for (CookieModel c : list) {
            if (query.isEmpty() || c.getDomain().toLowerCase().contains(query)
                    || c.getName().toLowerCase().contains(query)) {
                tableModel.addRow(new Object[] {
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
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, existing == null ? "Add Cookie" : "Edit Cookie",
                Dialog.ModalityType.APPLICATION_MODAL);
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

        gbc.gridx = 0;
        gbc.gridy = 0;
        dialog.add(new JLabel("Domain:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(domainField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Value:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(valueField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        dialog.add(new JLabel("Path:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        dialog.add(pathField, gbc);

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkPanel.add(secureCheck);
        checkPanel.add(Box.createHorizontalStrut(10));
        checkPanel.add(httpOnlyCheck);
        gbc.gridx = 1;
        gbc.gridy = 4;
        dialog.add(checkPanel, gbc);

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
                JOptionPane.showMessageDialog(dialog, "Domain and Name cannot be empty.", "Error",
                        JOptionPane.ERROR_MESSAGE);
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

        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTH;
        dialog.add(dbPanel, gbc);

        FontScaleHelper.scaleFonts(dialog, currentFontSize);

        dialog.setVisible(true);
    }
}

