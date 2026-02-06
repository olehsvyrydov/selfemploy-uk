package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;

/**
 * Holds the column mapping configuration for CSV import.
 * Tracks which CSV columns map to which transaction fields.
 *
 * SE-601: CSV Bank Import Wizard
 * SE-802: Added AmountInterpretation for correct income/expense classification
 */
public class ColumnMapping {

    // Required fields
    private final StringProperty dateColumn = new SimpleStringProperty();
    private final StringProperty descriptionColumn = new SimpleStringProperty();
    private final StringProperty dateFormat = new SimpleStringProperty();

    // Amount fields - either single column or separate income/expense
    private final BooleanProperty separateAmountColumns = new SimpleBooleanProperty(false);
    private final StringProperty amountColumn = new SimpleStringProperty();
    private final StringProperty incomeColumn = new SimpleStringProperty();
    private final StringProperty expenseColumn = new SimpleStringProperty();

    // SE-802: Amount interpretation for correct income/expense classification
    private final ObjectProperty<AmountInterpretation> amountInterpretation =
            new SimpleObjectProperty<>(AmountInterpretation.STANDARD);

    // Optional fields
    private final StringProperty categoryColumn = new SimpleStringProperty();
    private final StringProperty referenceColumn = new SimpleStringProperty();

    /**
     * Creates a new empty column mapping.
     */
    public ColumnMapping() {
        // Default empty
    }

    /**
     * Creates a pre-configured column mapping for a known bank format.
     */
    public static ColumnMapping forBankFormat(BankFormat format) {
        ColumnMapping mapping = new ColumnMapping();

        switch (format) {
            case BARCLAYS -> {
                mapping.setDateColumn("Date");
                mapping.setDescriptionColumn("Description");
                mapping.setSeparateAmountColumns(true);
                mapping.setIncomeColumn("Money in");
                mapping.setExpenseColumn("Money out");
                mapping.setDateFormat("dd/MM/yyyy");
            }
            case HSBC -> {
                mapping.setDateColumn("Date");
                mapping.setDescriptionColumn("Type");
                mapping.setSeparateAmountColumns(true);
                mapping.setIncomeColumn("Paid in");
                mapping.setExpenseColumn("Paid out");
                mapping.setDateFormat("dd/MM/yyyy");
            }
            case LLOYDS -> {
                mapping.setDateColumn("Transaction Date");
                mapping.setDescriptionColumn("Transaction Description");
                mapping.setSeparateAmountColumns(true);
                mapping.setIncomeColumn("Credit Amount");
                mapping.setExpenseColumn("Debit Amount");
                mapping.setDateFormat("dd/MM/yyyy");
            }
            case NATIONWIDE -> {
                mapping.setDateColumn("Date");
                mapping.setDescriptionColumn("Description");
                mapping.setSeparateAmountColumns(true);
                mapping.setIncomeColumn("Paid in");
                mapping.setExpenseColumn("Paid out");
                mapping.setDateFormat("dd MMM yyyy");
            }
            case STARLING -> {
                mapping.setDateColumn("Date");
                mapping.setDescriptionColumn("Counter Party");
                mapping.setSeparateAmountColumns(false);
                mapping.setAmountColumn("Amount (GBP)");
                mapping.setDateFormat("dd/MM/yyyy");
            }
            case MONZO -> {
                mapping.setDateColumn("Date");
                mapping.setDescriptionColumn("Name");
                mapping.setSeparateAmountColumns(false);
                mapping.setAmountColumn("Amount");
                mapping.setCategoryColumn("Category");
                mapping.setDateFormat("dd/MM/yyyy");
            }
            case REVOLUT -> {
                mapping.setDateColumn("Completed Date");
                mapping.setDescriptionColumn("Description");
                mapping.setSeparateAmountColumns(false);
                mapping.setAmountColumn("Amount");
                mapping.setDateFormat("yyyy-MM-dd HH:mm:ss");
            }
            case SANTANDER -> {
                mapping.setDateColumn("Date");
                mapping.setDescriptionColumn("Description");
                mapping.setSeparateAmountColumns(false);
                mapping.setAmountColumn("Amount");
                mapping.setDateFormat("dd/MM/yyyy");
            }
            case METRO_BANK -> {
                mapping.setDateColumn("Date");
                mapping.setDescriptionColumn("Description");
                mapping.setSeparateAmountColumns(true);
                mapping.setIncomeColumn("Money in");
                mapping.setExpenseColumn("Money out");
                mapping.setDateFormat("dd/MM/yyyy");
            }
            case UNKNOWN -> {
                // Leave empty for manual mapping
            }
        }

        return mapping;
    }

    /**
     * Checks if the mapping has all required fields.
     */
    public boolean isComplete() {
        if (isBlank(dateColumn.get()) || isBlank(descriptionColumn.get()) || isBlank(dateFormat.get())) {
            return false;
        }

        if (separateAmountColumns.get()) {
            return !isBlank(incomeColumn.get()) && !isBlank(expenseColumn.get());
        } else {
            return !isBlank(amountColumn.get());
        }
    }

    /**
     * Resets the mapping to empty state.
     */
    public void reset() {
        dateColumn.set(null);
        descriptionColumn.set(null);
        dateFormat.set(null);
        separateAmountColumns.set(false);
        amountColumn.set(null);
        incomeColumn.set(null);
        expenseColumn.set(null);
        amountInterpretation.set(AmountInterpretation.STANDARD);
        categoryColumn.set(null);
        referenceColumn.set(null);
    }

    // === Getters and Setters ===

    public String getDateColumn() {
        return dateColumn.get();
    }

    public void setDateColumn(String value) {
        dateColumn.set(value);
    }

    public StringProperty dateColumnProperty() {
        return dateColumn;
    }

    public String getDescriptionColumn() {
        return descriptionColumn.get();
    }

    public void setDescriptionColumn(String value) {
        descriptionColumn.set(value);
    }

    public StringProperty descriptionColumnProperty() {
        return descriptionColumn;
    }

    public String getDateFormat() {
        return dateFormat.get();
    }

    public void setDateFormat(String value) {
        dateFormat.set(value);
    }

    public StringProperty dateFormatProperty() {
        return dateFormat;
    }

    public boolean hasSeparateAmountColumns() {
        return separateAmountColumns.get();
    }

    public void setSeparateAmountColumns(boolean value) {
        separateAmountColumns.set(value);
    }

    public BooleanProperty separateAmountColumnsProperty() {
        return separateAmountColumns;
    }

    public String getAmountColumn() {
        return amountColumn.get();
    }

    public void setAmountColumn(String value) {
        amountColumn.set(value);
    }

    public StringProperty amountColumnProperty() {
        return amountColumn;
    }

    public String getIncomeColumn() {
        return incomeColumn.get();
    }

    public void setIncomeColumn(String value) {
        incomeColumn.set(value);
    }

    public StringProperty incomeColumnProperty() {
        return incomeColumn;
    }

    public String getExpenseColumn() {
        return expenseColumn.get();
    }

    public void setExpenseColumn(String value) {
        expenseColumn.set(value);
    }

    public StringProperty expenseColumnProperty() {
        return expenseColumn;
    }

    public String getCategoryColumn() {
        return categoryColumn.get();
    }

    public void setCategoryColumn(String value) {
        categoryColumn.set(value);
    }

    public StringProperty categoryColumnProperty() {
        return categoryColumn;
    }

    public String getReferenceColumn() {
        return referenceColumn.get();
    }

    public void setReferenceColumn(String value) {
        referenceColumn.set(value);
    }

    public StringProperty referenceColumnProperty() {
        return referenceColumn;
    }

    // SE-802: Amount interpretation getters/setters

    public AmountInterpretation getAmountInterpretation() {
        return amountInterpretation.get();
    }

    public void setAmountInterpretation(AmountInterpretation value) {
        amountInterpretation.set(value);
        // Update separateAmountColumns based on interpretation
        if (value == AmountInterpretation.SEPARATE_COLUMNS) {
            separateAmountColumns.set(true);
        }
    }

    public ObjectProperty<AmountInterpretation> amountInterpretationProperty() {
        return amountInterpretation;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
