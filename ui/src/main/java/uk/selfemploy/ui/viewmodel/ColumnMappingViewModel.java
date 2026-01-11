package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

/**
 * ViewModel for the Column Mapping Wizard.
 * Manages the 3-step wizard for mapping CSV columns to transaction fields
 * and confirming amount interpretation.
 *
 * SE-802: Bank Import Column Mapping Wizard
 *
 * <p>Step 1: Column Selection - Map Date, Description, Amount columns
 * <p>Step 2: Amount Interpretation - Choose how to interpret positive/negative values
 * <p>Step 3: Summary & Confirmation - Review and confirm before import proceeds
 */
public class ColumnMappingViewModel {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);
    private static final int TOTAL_STEPS = 3;

    private static final List<String> DATE_COLUMN_KEYWORDS = List.of(
            "date", "transaction date", "trans date", "posting date", "value date"
    );

    private static final List<String> DESCRIPTION_COLUMN_KEYWORDS = List.of(
            "description", "desc", "details", "narrative", "reference", "name",
            "counter party", "counterparty", "transaction description", "type"
    );

    private static final List<String> AMOUNT_COLUMN_KEYWORDS = List.of(
            "amount", "value", "sum", "total", "amount (gbp)", "gbp"
    );

    private static final List<String> STEP_NAMES = List.of(
            "Column Selection",
            "Amount Interpretation",
            "Summary & Confirmation"
    );

    // === Wizard State ===
    private final IntegerProperty currentStep = new SimpleIntegerProperty(1);
    private final BooleanProperty mappingConfirmed = new SimpleBooleanProperty(false);
    private final BooleanProperty savePreferenceSelected = new SimpleBooleanProperty(false);

    // === CSV Data ===
    private final ObservableList<String> csvHeaders = FXCollections.observableArrayList();
    private final ObservableList<PreviewRow> previewRows = FXCollections.observableArrayList();

    // === Step 1: Column Selection ===
    private final StringProperty selectedDateColumn = new SimpleStringProperty();
    private final StringProperty selectedDescriptionColumn = new SimpleStringProperty();
    private final StringProperty selectedAmountColumn = new SimpleStringProperty();
    private final StringProperty selectedCategoryColumn = new SimpleStringProperty();
    private final StringProperty selectedDateFormat = new SimpleStringProperty();

    // === Step 2: Amount Interpretation ===
    private final ObjectProperty<AmountInterpretation> amountInterpretation =
            new SimpleObjectProperty<>(AmountInterpretation.STANDARD);
    private final StringProperty selectedIncomeColumn = new SimpleStringProperty();
    private final StringProperty selectedExpenseColumn = new SimpleStringProperty();

    /**
     * Creates a new ColumnMappingViewModel with default state.
     */
    public ColumnMappingViewModel() {
        // Default initialization
    }

    // =====================================================
    // WIZARD NAVIGATION
    // =====================================================

    public int getCurrentStep() {
        return currentStep.get();
    }

    public IntegerProperty currentStepProperty() {
        return currentStep;
    }

    public String getCurrentStepName() {
        int step = currentStep.get();
        if (step >= 1 && step <= STEP_NAMES.size()) {
            return STEP_NAMES.get(step - 1);
        }
        return "";
    }

    public void goToNextStep() {
        if (canGoNext() && currentStep.get() < TOTAL_STEPS) {
            currentStep.set(currentStep.get() + 1);
        }
    }

    public void goToPreviousStep() {
        if (canGoPrevious()) {
            currentStep.set(currentStep.get() - 1);
        }
    }

    public boolean canGoNext() {
        return switch (currentStep.get()) {
            case 1 -> canProceedFromStep1();
            case 2 -> canProceedFromStep2();
            case 3 -> false; // Final step - user must confirm
            default -> false;
        };
    }

    public boolean canGoPrevious() {
        return currentStep.get() > 1;
    }

    public boolean canProceedFromStep1() {
        return !isBlank(selectedDateColumn.get()) &&
                !isBlank(selectedDescriptionColumn.get()) &&
                !isBlank(selectedAmountColumn.get()) &&
                !isBlank(selectedDateFormat.get());
    }

    public boolean canProceedFromStep2() {
        if (amountInterpretation.get() == AmountInterpretation.SEPARATE_COLUMNS) {
            return !isBlank(selectedIncomeColumn.get()) &&
                    !isBlank(selectedExpenseColumn.get());
        }
        return true; // STANDARD and INVERTED don't need additional columns
    }

    // =====================================================
    // CSV DATA MANAGEMENT
    // =====================================================

    public List<String> getCsvHeaders() {
        return new ArrayList<>(csvHeaders);
    }

    public void setCsvHeaders(List<String> headers) {
        csvHeaders.setAll(headers);
    }

    public ObservableList<String> csvHeadersProperty() {
        return csvHeaders;
    }

    public List<PreviewRow> getPreviewRows() {
        return new ArrayList<>(previewRows);
    }

    public void setPreviewRows(List<PreviewRow> rows) {
        // Only keep first 5 rows for preview
        if (rows.size() > 5) {
            previewRows.setAll(rows.subList(0, 5));
        } else {
            previewRows.setAll(rows);
        }
    }

    // =====================================================
    // STEP 1: COLUMN SELECTION
    // =====================================================

    public String getSelectedDateColumn() {
        return selectedDateColumn.get();
    }

    public void setSelectedDateColumn(String column) {
        selectedDateColumn.set(column);
    }

    public StringProperty selectedDateColumnProperty() {
        return selectedDateColumn;
    }

    public String getSelectedDescriptionColumn() {
        return selectedDescriptionColumn.get();
    }

    public void setSelectedDescriptionColumn(String column) {
        selectedDescriptionColumn.set(column);
    }

    public StringProperty selectedDescriptionColumnProperty() {
        return selectedDescriptionColumn;
    }

    public String getSelectedAmountColumn() {
        return selectedAmountColumn.get();
    }

    public void setSelectedAmountColumn(String column) {
        selectedAmountColumn.set(column);
    }

    public StringProperty selectedAmountColumnProperty() {
        return selectedAmountColumn;
    }

    public String getSelectedCategoryColumn() {
        return selectedCategoryColumn.get();
    }

    public void setSelectedCategoryColumn(String column) {
        selectedCategoryColumn.set(column);
    }

    public StringProperty selectedCategoryColumnProperty() {
        return selectedCategoryColumn;
    }

    public String getSelectedDateFormat() {
        return selectedDateFormat.get();
    }

    public void setSelectedDateFormat(String format) {
        selectedDateFormat.set(format);
    }

    public StringProperty selectedDateFormatProperty() {
        return selectedDateFormat;
    }

    /**
     * Auto-detects column mappings based on common header names.
     */
    public void autoDetectColumns() {
        for (String header : csvHeaders) {
            String lowerHeader = header.toLowerCase().trim();

            // Detect date column
            if (selectedDateColumn.get() == null) {
                for (String keyword : DATE_COLUMN_KEYWORDS) {
                    if (lowerHeader.equals(keyword) || lowerHeader.contains(keyword)) {
                        selectedDateColumn.set(header);
                        break;
                    }
                }
            }

            // Detect description column
            if (selectedDescriptionColumn.get() == null) {
                for (String keyword : DESCRIPTION_COLUMN_KEYWORDS) {
                    if (lowerHeader.equals(keyword) || lowerHeader.contains(keyword)) {
                        selectedDescriptionColumn.set(header);
                        break;
                    }
                }
            }

            // Detect amount column
            if (selectedAmountColumn.get() == null) {
                for (String keyword : AMOUNT_COLUMN_KEYWORDS) {
                    if (lowerHeader.equals(keyword) || lowerHeader.contains(keyword)) {
                        selectedAmountColumn.set(header);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Gets a sample value from the first preview row for a column.
     */
    public String getColumnSampleValue(String columnName) {
        int index = csvHeaders.indexOf(columnName);
        if (index >= 0 && !previewRows.isEmpty()) {
            return previewRows.get(0).getValue(index);
        }
        return "";
    }

    /**
     * Formats a column option for dropdown display.
     * Format: "Column Name (sample value...)"
     */
    public String formatColumnOption(String columnName) {
        String sample = getColumnSampleValue(columnName);
        if (sample.isEmpty()) {
            return columnName;
        }
        // Truncate long samples
        if (sample.length() > 15) {
            sample = sample.substring(0, 12) + "...";
        }
        return String.format("%s (%s)", columnName, sample);
    }

    // =====================================================
    // STEP 2: AMOUNT INTERPRETATION
    // =====================================================

    public AmountInterpretation getAmountInterpretation() {
        return amountInterpretation.get();
    }

    public void setAmountInterpretation(AmountInterpretation interpretation) {
        amountInterpretation.set(interpretation);
    }

    public ObjectProperty<AmountInterpretation> amountInterpretationProperty() {
        return amountInterpretation;
    }

    public String getSelectedIncomeColumn() {
        return selectedIncomeColumn.get();
    }

    public void setSelectedIncomeColumn(String column) {
        selectedIncomeColumn.set(column);
    }

    public StringProperty selectedIncomeColumnProperty() {
        return selectedIncomeColumn;
    }

    public String getSelectedExpenseColumn() {
        return selectedExpenseColumn.get();
    }

    public void setSelectedExpenseColumn(String column) {
        selectedExpenseColumn.set(column);
    }

    public StringProperty selectedExpenseColumnProperty() {
        return selectedExpenseColumn;
    }

    /**
     * Returns what positive values mean based on current interpretation.
     */
    public String getPositiveMeaning() {
        return switch (amountInterpretation.get()) {
            case STANDARD -> "INCOME";
            case INVERTED -> "EXPENSE";
            case SEPARATE_COLUMNS -> "N/A";
        };
    }

    /**
     * Returns what negative values mean based on current interpretation.
     */
    public String getNegativeMeaning() {
        return switch (amountInterpretation.get()) {
            case STANDARD -> "EXPENSE";
            case INVERTED -> "INCOME";
            case SEPARATE_COLUMNS -> "N/A";
        };
    }

    /**
     * Finds a positive amount example from the preview data.
     */
    public BigDecimal getPositiveAmountExample() {
        int amountIndex = csvHeaders.indexOf(selectedAmountColumn.get());
        if (amountIndex < 0) return null;

        for (PreviewRow row : previewRows) {
            BigDecimal amount = parseAmount(row.getValue(amountIndex));
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                return amount;
            }
        }
        return null;
    }

    /**
     * Finds a negative amount example from the preview data.
     */
    public BigDecimal getNegativeAmountExample() {
        int amountIndex = csvHeaders.indexOf(selectedAmountColumn.get());
        if (amountIndex < 0) return null;

        for (PreviewRow row : previewRows) {
            BigDecimal amount = parseAmount(row.getValue(amountIndex));
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                return amount;
            }
        }
        return null;
    }

    /**
     * Formats the positive amount example with interpretation.
     */
    public String getFormattedPositiveExample() {
        BigDecimal example = getPositiveAmountExample();
        if (example == null) {
            return "No positive values found";
        }
        String formatted = CURRENCY_FORMAT.format(example);
        return String.format("+%s -> %s", formatted, getPositiveMeaning());
    }

    /**
     * Formats the negative amount example with interpretation.
     */
    public String getFormattedNegativeExample() {
        BigDecimal example = getNegativeAmountExample();
        if (example == null) {
            return "No negative values found";
        }
        String formatted = CURRENCY_FORMAT.format(example.abs());
        return String.format("-%s -> %s", formatted, getNegativeMeaning());
    }

    // =====================================================
    // STEP 3: SUMMARY & CONFIRMATION
    // =====================================================

    /**
     * Gets preview rows classified based on current mapping and interpretation.
     */
    public List<ClassifiedPreviewRow> getClassifiedPreviewRows() {
        List<ClassifiedPreviewRow> classified = new ArrayList<>();

        int dateIndex = csvHeaders.indexOf(selectedDateColumn.get());
        int descIndex = csvHeaders.indexOf(selectedDescriptionColumn.get());
        int amountIndex = csvHeaders.indexOf(selectedAmountColumn.get());

        for (PreviewRow row : previewRows) {
            String date = dateIndex >= 0 ? row.getValue(dateIndex) : "";
            String description = descIndex >= 0 ? row.getValue(descIndex) : "";
            BigDecimal amount = amountIndex >= 0 ? parseAmount(row.getValue(amountIndex)) : BigDecimal.ZERO;

            TransactionType type = classifyAmount(amount);
            classified.add(new ClassifiedPreviewRow(row, date, description, amount, type));
        }

        return classified;
    }

    /**
     * Calculates the count of income transactions in preview.
     */
    public int getPreviewIncomeCount() {
        return (int) getClassifiedPreviewRows().stream()
                .filter(r -> r.getClassification() == TransactionType.INCOME)
                .count();
    }

    /**
     * Calculates the count of expense transactions in preview.
     */
    public int getPreviewExpenseCount() {
        return (int) getClassifiedPreviewRows().stream()
                .filter(r -> r.getClassification() == TransactionType.EXPENSE)
                .count();
    }

    /**
     * Calculates the total income amount in preview.
     */
    public BigDecimal getPreviewIncomeTotal() {
        return getClassifiedPreviewRows().stream()
                .filter(r -> r.getClassification() == TransactionType.INCOME)
                .map(r -> r.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total expense amount in preview.
     */
    public BigDecimal getPreviewExpenseTotal() {
        return getClassifiedPreviewRows().stream()
                .filter(r -> r.getClassification() == TransactionType.EXPENSE)
                .map(r -> r.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Gets income summary text for display.
     */
    public String getIncomeSummaryText() {
        int count = getPreviewIncomeCount();
        return count + " transaction" + (count == 1 ? "" : "s");
    }

    /**
     * Gets expense summary text for display.
     */
    public String getExpenseSummaryText() {
        int count = getPreviewExpenseCount();
        return count + " transaction" + (count == 1 ? "" : "s");
    }

    /**
     * Gets formatted income total for display.
     */
    public String getFormattedIncomeTotal() {
        return "+" + CURRENCY_FORMAT.format(getPreviewIncomeTotal());
    }

    /**
     * Gets formatted expense total for display.
     */
    public String getFormattedExpenseTotal() {
        return "-" + CURRENCY_FORMAT.format(getPreviewExpenseTotal());
    }

    public boolean isSavePreferenceSelected() {
        return savePreferenceSelected.get();
    }

    public void setSavePreferenceSelected(boolean selected) {
        savePreferenceSelected.set(selected);
    }

    public BooleanProperty savePreferenceSelectedProperty() {
        return savePreferenceSelected;
    }

    public boolean isMappingConfirmed() {
        return mappingConfirmed.get();
    }

    public void confirmMapping() {
        mappingConfirmed.set(true);
    }

    public BooleanProperty mappingConfirmedProperty() {
        return mappingConfirmed;
    }

    // =====================================================
    // BUILD RESULT
    // =====================================================

    /**
     * Builds the ColumnMapping result from the current wizard state.
     */
    public ColumnMapping buildColumnMapping() {
        ColumnMapping mapping = new ColumnMapping();

        mapping.setDateColumn(selectedDateColumn.get());
        mapping.setDescriptionColumn(selectedDescriptionColumn.get());
        mapping.setDateFormat(selectedDateFormat.get());
        mapping.setCategoryColumn(selectedCategoryColumn.get());
        mapping.setAmountInterpretation(amountInterpretation.get());

        if (amountInterpretation.get() == AmountInterpretation.SEPARATE_COLUMNS) {
            mapping.setSeparateAmountColumns(true);
            mapping.setIncomeColumn(selectedIncomeColumn.get());
            mapping.setExpenseColumn(selectedExpenseColumn.get());
        } else {
            mapping.setSeparateAmountColumns(false);
            mapping.setAmountColumn(selectedAmountColumn.get());
        }

        return mapping;
    }

    // =====================================================
    // RESET & CLEAR
    // =====================================================

    /**
     * Resets the wizard to initial state, preserving CSV data.
     */
    public void reset() {
        currentStep.set(1);
        selectedDateColumn.set(null);
        selectedDescriptionColumn.set(null);
        selectedAmountColumn.set(null);
        selectedCategoryColumn.set(null);
        selectedDateFormat.set(null);
        selectedIncomeColumn.set(null);
        selectedExpenseColumn.set(null);
        amountInterpretation.set(AmountInterpretation.STANDARD);
        mappingConfirmed.set(false);
        savePreferenceSelected.set(false);
    }

    /**
     * Clears all data including CSV headers and preview.
     */
    public void clearAll() {
        reset();
        csvHeaders.clear();
        previewRows.clear();
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    private TransactionType classifyAmount(BigDecimal amount) {
        if (amount == null) {
            return TransactionType.EXPENSE; // Default to expense for safety
        }

        boolean isPositive = amount.compareTo(BigDecimal.ZERO) > 0;

        return switch (amountInterpretation.get()) {
            case STANDARD -> isPositive ? TransactionType.INCOME : TransactionType.EXPENSE;
            case INVERTED -> isPositive ? TransactionType.EXPENSE : TransactionType.INCOME;
            case SEPARATE_COLUMNS -> TransactionType.EXPENSE; // Determined by column, not sign
        };
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            // Remove currency symbols and commas, handle parentheses for negative
            String cleaned = value.trim()
                    .replace("£", "")
                    .replace(",", "")
                    .replace(" ", "");

            // Handle (100.00) format for negative numbers
            if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
                cleaned = "-" + cleaned.substring(1, cleaned.length() - 1);
            }

            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
