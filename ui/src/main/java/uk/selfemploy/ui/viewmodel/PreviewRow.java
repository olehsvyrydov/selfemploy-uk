package uk.selfemploy.ui.viewmodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a raw row from CSV preview data.
 * Contains the column values as strings before any parsing or interpretation.
 *
 * SE-802: Bank Import Column Mapping Wizard
 */
public class PreviewRow {

    private final List<String> values;

    /**
     * Creates a preview row with the given column values.
     *
     * @param values list of column values in order
     */
    public PreviewRow(List<String> values) {
        this.values = new ArrayList<>(values);
    }

    /**
     * Gets the value at the specified column index.
     *
     * @param index column index (0-based)
     * @return the value at that column, or empty string if index out of bounds
     */
    public String getValue(int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index);
    }

    /**
     * Gets all column values.
     *
     * @return list of column values
     */
    public List<String> getValues() {
        return new ArrayList<>(values);
    }

    /**
     * Gets the number of columns in this row.
     *
     * @return column count
     */
    public int getColumnCount() {
        return values.size();
    }

    @Override
    public String toString() {
        return String.join(", ", values);
    }
}
