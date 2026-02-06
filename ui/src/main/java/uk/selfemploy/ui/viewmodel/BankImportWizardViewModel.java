package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ViewModel for the CSV Bank Import Wizard.
 * Manages wizard state, column mapping, transaction preview, and import operations.
 *
 * SE-601: CSV Bank Import Wizard
 */
public class BankImportWizardViewModel {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);
    private static final int TOTAL_STEPS = 4;

    private static final List<String> DATE_FORMATS = List.of(
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "yyyy-MM-dd",
            "d MMM yyyy",
            "dd-MM-yyyy"
    );

    // === Wizard State ===
    private final IntegerProperty currentStep = new SimpleIntegerProperty(1);

    // === Step 1: File Selection ===
    private final ObjectProperty<File> selectedFile = new SimpleObjectProperty<>();
    private final ObservableList<String> csvHeaders = FXCollections.observableArrayList();
    private final ObjectProperty<BankFormat> detectedBankFormat = new SimpleObjectProperty<>(BankFormat.UNKNOWN);
    private final IntegerProperty rowCount = new SimpleIntegerProperty(0);

    // === Step 2: Column Mapping ===
    private final ColumnMapping columnMapping = new ColumnMapping();

    // === Step 3: Preview & Categorize ===
    private final ObservableList<ImportedTransactionRow> transactions = FXCollections.observableArrayList();
    private final ObjectProperty<TransactionFilter> transactionFilter = new SimpleObjectProperty<>(TransactionFilter.ALL);
    private final StringProperty searchText = new SimpleStringProperty("");
    private final Set<UUID> selectedTransactions = new HashSet<>();

    // === Step 4: Import ===
    private final BooleanProperty importing = new SimpleBooleanProperty(false);
    private final DoubleProperty importProgress = new SimpleDoubleProperty(0.0);

    /**
     * Creates a new BankImportWizardViewModel.
     */
    public BankImportWizardViewModel() {
        // Set up listeners for filter/search changes
        transactionFilter.addListener((obs, oldVal, newVal) -> {
            // Filter changed - clear selection
            selectedTransactions.clear();
        });
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

    public boolean canGoNext() {
        return switch (currentStep.get()) {
            case 1 -> isFileSelected() && !csvHeaders.isEmpty();
            case 2 -> columnMapping.isComplete();
            case 3 -> true; // Always can proceed from preview
            case 4 -> false; // Final step
            default -> false;
        };
    }

    public boolean canGoPrevious() {
        return currentStep.get() > 1;
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

    public String getStepLabel(int step) {
        return switch (step) {
            case 1 -> "Select File";
            case 2 -> "Map Columns";
            case 3 -> "Preview";
            case 4 -> "Confirm";
            default -> "";
        };
    }

    public boolean isStepCompleted(int step) {
        return switch (step) {
            case 1 -> isFileSelected() && !csvHeaders.isEmpty();
            case 2 -> columnMapping.isComplete();
            case 3 -> !transactions.isEmpty();
            case 4 -> false; // Never pre-completed
            default -> false;
        };
    }

    // =====================================================
    // STEP 1: FILE SELECTION
    // =====================================================

    public File getSelectedFile() {
        return selectedFile.get();
    }

    public void setSelectedFile(File file) {
        selectedFile.set(file);
    }

    public ObjectProperty<File> selectedFileProperty() {
        return selectedFile;
    }

    public boolean isFileSelected() {
        return selectedFile.get() != null;
    }

    public String getFileName() {
        File file = selectedFile.get();
        return file != null ? file.getName() : "";
    }

    public List<String> getCsvHeaders() {
        return new ArrayList<>(csvHeaders);
    }

    public void setCsvHeaders(List<String> headers) {
        csvHeaders.setAll(headers);
        detectBankFormat(headers);
    }

    public BankFormat getDetectedBankFormat() {
        return detectedBankFormat.get();
    }

    public ObjectProperty<BankFormat> detectedBankFormatProperty() {
        return detectedBankFormat;
    }

    public int getRowCount() {
        return rowCount.get();
    }

    public void setRowCount(int count) {
        rowCount.set(count);
    }

    public IntegerProperty rowCountProperty() {
        return rowCount;
    }

    public void clearFile() {
        selectedFile.set(null);
        csvHeaders.clear();
        detectedBankFormat.set(BankFormat.UNKNOWN);
        rowCount.set(0);
        columnMapping.reset();
        transactions.clear();
        selectedTransactions.clear();
    }

    private void detectBankFormat(List<String> headers) {
        BankFormat format = detectFormat(headers);
        detectedBankFormat.set(format);

        // Auto-populate mapping for known formats
        if (format != BankFormat.UNKNOWN) {
            ColumnMapping presetMapping = ColumnMapping.forBankFormat(format);
            applyPresetMapping(presetMapping);
        }
    }

    private BankFormat detectFormat(List<String> headers) {
        Set<String> headerSet = new HashSet<>(headers);

        // Revolut: Type, Product, Started Date, Completed Date, Description, Amount, Fee, Currency, State, Balance
        if (headerSet.contains("Started Date") && headerSet.contains("Completed Date")) {
            return BankFormat.REVOLUT;
        }

        // Metro Bank: Date, Transaction type, Description, Money out, Money in, Balance
        // Must be checked BEFORE Barclays since both have Money out/Money in
        if (headerSet.contains("Transaction type") && headerSet.contains("Money out") && headerSet.contains("Money in")) {
            return BankFormat.METRO_BANK;
        }

        // Barclays: Date, Type, Description, Money out, Money in, Balance
        if (headerSet.contains("Money out") && headerSet.contains("Money in")) {
            return BankFormat.BARCLAYS;
        }

        // HSBC: Date, Type, Paid out, Paid in, Balance
        if (headerSet.contains("Paid out") && headerSet.contains("Paid in") && !headerSet.contains("Transaction type")) {
            return BankFormat.HSBC;
        }

        // Lloyds: Transaction Date, Transaction Description, Debit Amount, Credit Amount
        if (headerSet.contains("Transaction Date") && headerSet.contains("Transaction Description")) {
            return BankFormat.LLOYDS;
        }

        // Nationwide: Date, Transaction type, Description, Paid out, Paid in
        if (headerSet.contains("Transaction type") && headerSet.contains("Paid out")) {
            return BankFormat.NATIONWIDE;
        }

        // Starling: Date, Counter Party, Reference, Type, Amount (GBP), Balance (GBP)
        if (headerSet.contains("Counter Party") && headerSet.contains("Amount (GBP)")) {
            return BankFormat.STARLING;
        }

        // Monzo: Transaction ID, Date, Time, Type, Name, Emoji, Category, Amount
        if (headerSet.contains("Transaction ID") && headerSet.contains("Emoji")) {
            return BankFormat.MONZO;
        }

        // Santander: Date, Description, Amount, Balance (generic - check last)
        if (headerSet.contains("Date") && headerSet.contains("Description")
                && headerSet.contains("Amount") && headerSet.contains("Balance")
                && headerSet.size() == 4) {
            return BankFormat.SANTANDER;
        }

        return BankFormat.UNKNOWN;
    }

    private void applyPresetMapping(ColumnMapping preset) {
        columnMapping.setDateColumn(preset.getDateColumn());
        columnMapping.setDescriptionColumn(preset.getDescriptionColumn());
        columnMapping.setDateFormat(preset.getDateFormat());
        columnMapping.setSeparateAmountColumns(preset.hasSeparateAmountColumns());

        if (preset.hasSeparateAmountColumns()) {
            columnMapping.setIncomeColumn(preset.getIncomeColumn());
            columnMapping.setExpenseColumn(preset.getExpenseColumn());
        } else {
            columnMapping.setAmountColumn(preset.getAmountColumn());
        }

        if (preset.getCategoryColumn() != null) {
            columnMapping.setCategoryColumn(preset.getCategoryColumn());
        }
    }

    // =====================================================
    // STEP 2: COLUMN MAPPING
    // =====================================================

    public ColumnMapping getColumnMapping() {
        return columnMapping;
    }

    public List<String> getAvailableDateFormats() {
        return DATE_FORMATS;
    }

    // =====================================================
    // STEP 3: PREVIEW & CATEGORIZE
    // =====================================================

    public List<ImportedTransactionRow> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public void addTransaction(ImportedTransactionRow transaction) {
        transactions.add(transaction);
    }

    public void setTransactions(List<ImportedTransactionRow> newTransactions) {
        transactions.setAll(newTransactions);
        selectedTransactions.clear();
    }

    public void clearTransactions() {
        transactions.clear();
        selectedTransactions.clear();
    }

    // --- Summary Statistics ---

    public int getIncomeCount() {
        return (int) transactions.stream()
                .filter(t -> t.type() == TransactionType.INCOME)
                .count();
    }

    public BigDecimal getIncomeTotal() {
        return transactions.stream()
                .filter(t -> t.type() == TransactionType.INCOME)
                .map(ImportedTransactionRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getFormattedIncomeTotal() {
        return CURRENCY_FORMAT.format(getIncomeTotal());
    }

    public int getExpenseCount() {
        return (int) transactions.stream()
                .filter(t -> t.type() == TransactionType.EXPENSE)
                .count();
    }

    public BigDecimal getExpenseTotal() {
        return transactions.stream()
                .filter(t -> t.type() == TransactionType.EXPENSE)
                .map(ImportedTransactionRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getFormattedExpenseTotal() {
        return CURRENCY_FORMAT.format(getExpenseTotal());
    }

    public int getDuplicateCount() {
        return (int) transactions.stream()
                .filter(ImportedTransactionRow::isDuplicate)
                .count();
    }

    public int getUncategorizedCount() {
        return (int) transactions.stream()
                .filter(t -> t.category() == null)
                .count();
    }

    // --- Filtering ---

    public TransactionFilter getTransactionFilter() {
        return transactionFilter.get();
    }

    public void setTransactionFilter(TransactionFilter filter) {
        transactionFilter.set(filter);
    }

    public ObjectProperty<TransactionFilter> transactionFilterProperty() {
        return transactionFilter;
    }

    public String getSearchText() {
        return searchText.get();
    }

    public void setSearchText(String text) {
        searchText.set(text);
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }

    public List<ImportedTransactionRow> getFilteredTransactions() {
        return transactions.stream()
                .filter(t -> t.matchesFilter(transactionFilter.get()))
                .filter(t -> t.matchesSearch(searchText.get()))
                .toList();
    }

    // --- Selection ---

    public void selectTransaction(UUID id) {
        selectedTransactions.add(id);
    }

    public void deselectTransaction(UUID id) {
        selectedTransactions.remove(id);
    }

    public void toggleSelection(UUID id) {
        if (selectedTransactions.contains(id)) {
            selectedTransactions.remove(id);
        } else {
            selectedTransactions.add(id);
        }
    }

    public boolean isSelected(UUID id) {
        return selectedTransactions.contains(id);
    }

    public int getSelectedCount() {
        return selectedTransactions.size();
    }

    public void selectAll() {
        getFilteredTransactions().forEach(t -> selectedTransactions.add(t.id()));
    }

    public void clearSelection() {
        selectedTransactions.clear();
    }

    // --- Category Operations ---

    public void applyBulkCategory(ExpenseCategory category) {
        List<ImportedTransactionRow> updated = new ArrayList<>();

        for (ImportedTransactionRow tx : transactions) {
            if (selectedTransactions.contains(tx.id())) {
                updated.add(tx.withCategory(category));
            } else {
                updated.add(tx);
            }
        }

        transactions.setAll(updated);
        selectedTransactions.clear();
    }

    public void updateTransactionCategory(UUID id, ExpenseCategory category) {
        List<ImportedTransactionRow> updated = transactions.stream()
                .map(t -> t.id().equals(id) ? t.withCategory(category) : t)
                .toList();
        transactions.setAll(updated);
    }

    public List<ImportedTransactionRow> getTransactionsToImport() {
        return transactions.stream()
                .filter(t -> !t.isDuplicate())
                .toList();
    }

    // =====================================================
    // STEP 4: CONFIRM & IMPORT
    // =====================================================

    public int getConfirmIncomeCount() {
        return (int) getTransactionsToImport().stream()
                .filter(t -> t.type() == TransactionType.INCOME)
                .count();
    }

    public BigDecimal getConfirmIncomeTotal() {
        return getTransactionsToImport().stream()
                .filter(t -> t.type() == TransactionType.INCOME)
                .map(ImportedTransactionRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getConfirmExpenseCount() {
        return (int) getTransactionsToImport().stream()
                .filter(t -> t.type() == TransactionType.EXPENSE)
                .count();
    }

    public BigDecimal getConfirmExpenseTotal() {
        return getTransactionsToImport().stream()
                .filter(t -> t.type() == TransactionType.EXPENSE)
                .map(ImportedTransactionRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<ExpenseCategory, CategoryBreakdownItem> getCategoryBreakdown() {
        Map<ExpenseCategory, List<ImportedTransactionRow>> grouped = getTransactionsToImport().stream()
                .filter(t -> t.type() == TransactionType.EXPENSE)
                .filter(t -> t.category() != null)
                .collect(Collectors.groupingBy(ImportedTransactionRow::category));

        Map<ExpenseCategory, CategoryBreakdownItem> breakdown = new LinkedHashMap<>();
        for (Map.Entry<ExpenseCategory, List<ImportedTransactionRow>> entry : grouped.entrySet()) {
            int count = entry.getValue().size();
            BigDecimal total = entry.getValue().stream()
                    .map(ImportedTransactionRow::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            breakdown.put(entry.getKey(), new CategoryBreakdownItem(count, total));
        }

        return breakdown;
    }

    public int getTotalToImport() {
        return getTransactionsToImport().size();
    }

    public int getSkippedCount() {
        return getDuplicateCount();
    }

    public String getImportButtonText() {
        int count = getTotalToImport();
        return String.format("Import %d Transaction%s", count, count == 1 ? "" : "s");
    }

    public boolean isImporting() {
        return importing.get();
    }

    public void setImporting(boolean value) {
        importing.set(value);
    }

    public BooleanProperty importingProperty() {
        return importing;
    }

    public double getImportProgress() {
        return importProgress.get();
    }

    public void setImportProgress(double value) {
        importProgress.set(value);
    }

    public DoubleProperty importProgressProperty() {
        return importProgress;
    }

    // =====================================================
    // WIZARD RESET
    // =====================================================

    public void reset() {
        currentStep.set(1);
        clearFile();
        transactionFilter.set(TransactionFilter.ALL);
        searchText.set("");
        importing.set(false);
        importProgress.set(0.0);
    }

    // =====================================================
    // INNER CLASSES
    // =====================================================

    /**
     * Represents a category breakdown item for the summary.
     */
    public record CategoryBreakdownItem(int count, BigDecimal total) {
        public String getFormattedTotal() {
            return CURRENCY_FORMAT.format(total);
        }
    }
}
