package in.slpro.apibanker.ui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * KeyValueTableModel
 *
 * <p>
 * Core functionality and implementation logic for KeyValueTableModel.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.0
 * @since 1.0.0
 */
public class KeyValueTableModel extends AbstractTableModel {
    private final String[] columns;
    private final List<String[]> rows = new ArrayList<>();
    private final boolean hasDescription;

    public KeyValueTableModel(boolean hasDescription) {
        this.hasDescription = hasDescription;
        this.columns = hasDescription ? new String[] { "", "Key", "Value", "Description" }
                : new String[] { "", "Key", "Value" };
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int col) {
        return columns[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        String[] r = rows.get(row);
        if (col == 0)
            return Boolean.parseBoolean(r[0]);
        return r[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        if (col == 0)
            return Boolean.class;
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return true;
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        String[] r = rows.get(row);
        r[col] = value != null ? value.toString() : "";
        fireTableCellUpdated(row, col);
    }

    public void addRow(boolean enabled, String key, String value, String description) {
        int cols = hasDescription ? 4 : 3;
        String[] r = new String[cols];
        r[0] = String.valueOf(enabled);
        r[1] = key != null ? key : "";
        r[2] = value != null ? value : "";
        if (hasDescription && cols > 3)
            r[3] = description != null ? description : "";
        rows.add(r);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void removeRow(int row) {
        if (row >= 0 && row < rows.size()) {
            rows.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    public void clear() {
        int size = rows.size();
        rows.clear();
        if (size > 0)
            fireTableRowsDeleted(0, size - 1);
    }

    public List<String[]> getRows() {
        return rows;
    }
}

