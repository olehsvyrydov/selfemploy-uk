package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for BankImportWizardViewModel.
 * Covers wizard state management, column mapping validation,
 * transaction filtering, bulk operations, and ImportedTransactionRow display formatting.
 */
@DisplayName("BankImportWizardViewModel")
class BankImportWizardViewModelTest {

    private BankImportWizardViewModel viewModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new BankImportWizardViewModel();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should initialize on step 1")
        void shouldInitializeOnStepOne() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should initialize with no file selected")
        void shouldInitializeWithNoFileSelected() {
            assertThat(viewModel.getSelectedFile()).isNull();
            assertThat(viewModel.isFileSelected()).isFalse();
        }

        @Test
        @DisplayName("should initialize with empty column mapping")
        void shouldInitializeWithEmptyColumnMapping() {
            assertThat(viewModel.getColumnMapping()).isNotNull();
            assertThat(viewModel.getColumnMapping().isComplete()).isFalse();
        }

        @Test
        @DisplayName("should initialize with empty transactions list")
        void shouldInitializeWithEmptyTransactionsList() {
            assertThat(viewModel.getTransactions()).isEmpty();
        }

        @Test
        @DisplayName("should initialize with zero summary values")
        void shouldInitializeWithZeroSummaryValues() {
            assertThat(viewModel.getIncomeCount()).isZero();
            assertThat(viewModel.getIncomeTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getExpenseCount()).isZero();
            assertThat(viewModel.getExpenseTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getDuplicateCount()).isZero();
            assertThat(viewModel.getUncategorizedCount()).isZero();
        }

        @Test
        @DisplayName("should not allow next on step 1 without file")
        void shouldNotAllowNextOnStepOneWithoutFile() {
            assertThat(viewModel.canGoNext()).isFalse();
        }

        @Test
        @DisplayName("should not allow previous on step 1")
        void shouldNotAllowPreviousOnStepOne() {
            assertThat(viewModel.canGoPrevious()).isFalse();
        }

        @Test
        @DisplayName("should initialize with unknown bank format")
        void shouldInitializeWithUnknownBankFormat() {
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("Step 1: File Selection")
    class Step1FileSelection {

        @Test
        @DisplayName("should accept CSV file")
        void shouldAcceptCsvFile() {
            // Given
            File csvFile = new File("test-bank-statement.csv");

            // When
            viewModel.setSelectedFile(csvFile);

            // Then
            assertThat(viewModel.getSelectedFile()).isEqualTo(csvFile);
            assertThat(viewModel.isFileSelected()).isTrue();
        }

        @Test
        @DisplayName("should extract file name")
        void shouldExtractFileName() {
            // Given
            File csvFile = new File("/path/to/bank-statement.csv");

            // When
            viewModel.setSelectedFile(csvFile);

            // Then
            assertThat(viewModel.getFileName()).isEqualTo("bank-statement.csv");
        }

        @Test
        @DisplayName("should allow next when file is selected")
        void shouldAllowNextWhenFileIsSelected() {
            // Given
            File csvFile = new File("test.csv");
            viewModel.setSelectedFile(csvFile);
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));

            // Then
            assertThat(viewModel.canGoNext()).isTrue();
        }

        @Test
        @DisplayName("should detect Barclays format")
        void shouldDetectBarclaysFormat() {
            // Given - Barclays headers
            List<String> headers = List.of("Date", "Type", "Description", "Money out", "Money in", "Balance");

            // When
            viewModel.setCsvHeaders(headers);

            // Then
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.BARCLAYS);
        }

        @Test
        @DisplayName("should detect HSBC format")
        void shouldDetectHsbcFormat() {
            // Given - HSBC headers
            List<String> headers = List.of("Date", "Type", "Paid out", "Paid in", "Balance");

            // When
            viewModel.setCsvHeaders(headers);

            // Then
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.HSBC);
        }

        @Test
        @DisplayName("should detect Lloyds format")
        void shouldDetectLloydsFormat() {
            // Given - Lloyds headers
            List<String> headers = List.of("Transaction Date", "Transaction Type", "Sort Code",
                    "Account Number", "Transaction Description", "Debit Amount", "Credit Amount", "Balance");

            // When
            viewModel.setCsvHeaders(headers);

            // Then
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.LLOYDS);
        }

        @Test
        @DisplayName("should detect Nationwide format")
        void shouldDetectNationwideFormat() {
            // Given - Nationwide headers
            List<String> headers = List.of("Date", "Transaction type", "Description",
                    "Paid out", "Paid in", "Balance");

            // When
            viewModel.setCsvHeaders(headers);

            // Then
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.NATIONWIDE);
        }

        @Test
        @DisplayName("should detect Starling format")
        void shouldDetectStarlingFormat() {
            // Given - Starling headers
            List<String> headers = List.of("Date", "Counter Party", "Reference", "Type",
                    "Amount (GBP)", "Balance (GBP)");

            // When
            viewModel.setCsvHeaders(headers);

            // Then
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.STARLING);
        }

        @Test
        @DisplayName("should detect Monzo format")
        void shouldDetectMonzoFormat() {
            // Given - Monzo headers
            List<String> headers = List.of("Transaction ID", "Date", "Time", "Type", "Name",
                    "Emoji", "Category", "Amount", "Currency", "Notes and #tags");

            // When
            viewModel.setCsvHeaders(headers);

            // Then
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.MONZO);
        }

        @Test
        @DisplayName("should return UNKNOWN for unrecognized format")
        void shouldReturnUnknownForUnrecognizedFormat() {
            // Given - Unknown headers
            List<String> headers = List.of("Col1", "Col2", "Col3");

            // When
            viewModel.setCsvHeaders(headers);

            // Then
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.UNKNOWN);
        }

        @Test
        @DisplayName("should auto-populate mapping for known bank format")
        void shouldAutoPopulateMappingForKnownFormat() {
            // Given - Barclays headers
            List<String> headers = List.of("Date", "Type", "Description", "Money out", "Money in", "Balance");

            // When
            viewModel.setCsvHeaders(headers);

            // Then
            ColumnMapping mapping = viewModel.getColumnMapping();
            assertThat(mapping.getDateColumn()).isEqualTo("Date");
            assertThat(mapping.getDescriptionColumn()).isEqualTo("Description");
            assertThat(mapping.hasSeparateAmountColumns()).isTrue();
            assertThat(mapping.getIncomeColumn()).isEqualTo("Money in");
            assertThat(mapping.getExpenseColumn()).isEqualTo("Money out");
        }

        @Test
        @DisplayName("should display row count after parsing headers")
        void shouldDisplayRowCount() {
            // Given
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));

            // When
            viewModel.setRowCount(150);

            // Then
            assertThat(viewModel.getRowCount()).isEqualTo(150);
        }

        @Test
        @DisplayName("should clear file and reset state on change file")
        void shouldClearFileOnChangeFile() {
            // Given
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Amount"));

            // When
            viewModel.clearFile();

            // Then
            assertThat(viewModel.getSelectedFile()).isNull();
            assertThat(viewModel.isFileSelected()).isFalse();
            assertThat(viewModel.getCsvHeaders()).isEmpty();
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("Step Navigation")
    class StepNavigation {

        @BeforeEach
        void setUpFile() {
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
        }

        @Test
        @DisplayName("should navigate to step 2")
        void shouldNavigateToStepTwo() {
            // When
            viewModel.goToNextStep();

            // Then
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("should navigate back to step 1 from step 2")
        void shouldNavigateBackToStepOne() {
            // Given
            viewModel.goToNextStep();

            // When
            viewModel.goToPreviousStep();

            // Then
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not go beyond step 4")
        void shouldNotGoBeyondStepFour() {
            // Given - navigate to step 4
            setupCompleteMappingForNavigation();
            viewModel.goToNextStep(); // to 2
            viewModel.goToNextStep(); // to 3
            viewModel.goToNextStep(); // to 4

            // When
            viewModel.goToNextStep(); // try to go beyond

            // Then
            assertThat(viewModel.getCurrentStep()).isEqualTo(4);
        }

        @Test
        @DisplayName("should not go before step 1")
        void shouldNotGoBeforeStepOne() {
            // When
            viewModel.goToPreviousStep();
            viewModel.goToPreviousStep();

            // Then
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should update step labels correctly")
        void shouldUpdateStepLabelsCorrectly() {
            assertThat(viewModel.getStepLabel(1)).isEqualTo("Select File");
            assertThat(viewModel.getStepLabel(2)).isEqualTo("Map Columns");
            assertThat(viewModel.getStepLabel(3)).isEqualTo("Preview");
            assertThat(viewModel.getStepLabel(4)).isEqualTo("Confirm");
        }

        @Test
        @DisplayName("should check if step is completed")
        void shouldCheckIfStepIsCompleted() {
            // Step 1 is completed when file is selected
            assertThat(viewModel.isStepCompleted(1)).isTrue();

            // Step 2 is not completed yet
            assertThat(viewModel.isStepCompleted(2)).isFalse();

            // Navigate to step 2 and complete mapping
            viewModel.goToNextStep();
            setupCompleteMappingForNavigation();
            assertThat(viewModel.isStepCompleted(2)).isTrue();
        }

        @Test
        @DisplayName("should require valid mapping to go from step 2 to 3")
        void shouldRequireValidMappingToGoFromStepTwoToThree() {
            // Given - on step 2 without complete mapping
            viewModel.goToNextStep();

            // Then
            assertThat(viewModel.canGoNext()).isFalse();

            // When - complete the mapping
            setupCompleteMappingForNavigation();

            // Then
            assertThat(viewModel.canGoNext()).isTrue();
        }

        private void setupCompleteMappingForNavigation() {
            ColumnMapping mapping = viewModel.getColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");
        }
    }

    @Nested
    @DisplayName("Step 2: Column Mapping")
    class Step2ColumnMapping {

        @BeforeEach
        void setUpStep2() {
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount", "Category", "Reference"));
            viewModel.goToNextStep();
        }

        @Test
        @DisplayName("should provide available columns for mapping")
        void shouldProvideAvailableColumnsForMapping() {
            assertThat(viewModel.getCsvHeaders()).containsExactly(
                    "Date", "Description", "Amount", "Category", "Reference");
        }

        @Test
        @DisplayName("should set date column")
        void shouldSetDateColumn() {
            // When
            viewModel.getColumnMapping().setDateColumn("Date");

            // Then
            assertThat(viewModel.getColumnMapping().getDateColumn()).isEqualTo("Date");
        }

        @Test
        @DisplayName("should set description column")
        void shouldSetDescriptionColumn() {
            // When
            viewModel.getColumnMapping().setDescriptionColumn("Description");

            // Then
            assertThat(viewModel.getColumnMapping().getDescriptionColumn()).isEqualTo("Description");
        }

        @Test
        @DisplayName("should set single amount column")
        void shouldSetSingleAmountColumn() {
            // When
            viewModel.getColumnMapping().setAmountColumn("Amount");

            // Then
            assertThat(viewModel.getColumnMapping().getAmountColumn()).isEqualTo("Amount");
            assertThat(viewModel.getColumnMapping().hasSeparateAmountColumns()).isFalse();
        }

        @Test
        @DisplayName("should set separate income/expense columns")
        void shouldSetSeparateIncomeExpenseColumns() {
            // Given - headers with separate columns
            viewModel.setCsvHeaders(List.of("Date", "Description", "Money In", "Money Out"));

            // When
            viewModel.getColumnMapping().setSeparateAmountColumns(true);
            viewModel.getColumnMapping().setIncomeColumn("Money In");
            viewModel.getColumnMapping().setExpenseColumn("Money Out");

            // Then
            assertThat(viewModel.getColumnMapping().hasSeparateAmountColumns()).isTrue();
            assertThat(viewModel.getColumnMapping().getIncomeColumn()).isEqualTo("Money In");
            assertThat(viewModel.getColumnMapping().getExpenseColumn()).isEqualTo("Money Out");
        }

        @Test
        @DisplayName("should set optional category column")
        void shouldSetOptionalCategoryColumn() {
            // When
            viewModel.getColumnMapping().setCategoryColumn("Category");

            // Then
            assertThat(viewModel.getColumnMapping().getCategoryColumn()).isEqualTo("Category");
        }

        @Test
        @DisplayName("should validate mapping completeness")
        void shouldValidateMappingCompleteness() {
            // Initially incomplete
            assertThat(viewModel.getColumnMapping().isComplete()).isFalse();

            // Add required fields
            viewModel.getColumnMapping().setDateColumn("Date");
            assertThat(viewModel.getColumnMapping().isComplete()).isFalse();

            viewModel.getColumnMapping().setDescriptionColumn("Description");
            assertThat(viewModel.getColumnMapping().isComplete()).isFalse();

            viewModel.getColumnMapping().setAmountColumn("Amount");
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");
            assertThat(viewModel.getColumnMapping().isComplete()).isTrue();
        }

        @Test
        @DisplayName("should set date format")
        void shouldSetDateFormat() {
            // When
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");

            // Then
            assertThat(viewModel.getColumnMapping().getDateFormat()).isEqualTo("dd/MM/yyyy");
        }

        @Test
        @DisplayName("should provide common date formats")
        void shouldProvideCommonDateFormats() {
            List<String> formats = viewModel.getAvailableDateFormats();

            assertThat(formats).containsExactly(
                    "dd/MM/yyyy",
                    "MM/dd/yyyy",
                    "yyyy-MM-dd",
                    "d MMM yyyy",
                    "dd-MM-yyyy"
            );
        }
    }

    @Nested
    @DisplayName("Step 3: Preview & Categorize")
    class Step3PreviewAndCategorize {

        @BeforeEach
        void setUpStep3() {
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
            viewModel.getColumnMapping().setDateColumn("Date");
            viewModel.getColumnMapping().setDescriptionColumn("Description");
            viewModel.getColumnMapping().setAmountColumn("Amount");
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");
            viewModel.goToNextStep(); // to 2
            viewModel.goToNextStep(); // to 3

            // Add sample transactions
            addSampleTransactions();
        }

        private void addSampleTransactions() {
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 5), "Client payment", new BigDecimal("1500.00"),
                    TransactionType.INCOME, ExpenseCategory.OTHER_EXPENSES, false, 85));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 6), "Office supplies", new BigDecimal("-45.50"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 92));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 7), "Freelance work", new BigDecimal("2000.00"),
                    TransactionType.INCOME, null, false, 0));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 8), "Existing payment", new BigDecimal("500.00"),
                    TransactionType.INCOME, ExpenseCategory.OTHER_EXPENSES, true, 100));
        }

        @Test
        @DisplayName("should calculate income count and total")
        void shouldCalculateIncomeCountAndTotal() {
            assertThat(viewModel.getIncomeCount()).isEqualTo(3);
            assertThat(viewModel.getIncomeTotal()).isEqualByComparingTo(new BigDecimal("4000.00"));
        }

        @Test
        @DisplayName("should calculate expense count and total")
        void shouldCalculateExpenseCountAndTotal() {
            assertThat(viewModel.getExpenseCount()).isEqualTo(1);
            assertThat(viewModel.getExpenseTotal()).isEqualByComparingTo(new BigDecimal("45.50"));
        }

        @Test
        @DisplayName("should count duplicates")
        void shouldCountDuplicates() {
            assertThat(viewModel.getDuplicateCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should count uncategorized transactions")
        void shouldCountUncategorizedTransactions() {
            assertThat(viewModel.getUncategorizedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should filter by transaction type - All")
        void shouldFilterByTransactionTypeAll() {
            viewModel.setTransactionFilter(TransactionFilter.ALL);
            assertThat(viewModel.getFilteredTransactions()).hasSize(4);
        }

        @Test
        @DisplayName("should filter by transaction type - Income only")
        void shouldFilterByTransactionTypeIncomeOnly() {
            viewModel.setTransactionFilter(TransactionFilter.INCOME_ONLY);
            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(3);
            assertThat(filtered).allMatch(t -> t.type() == TransactionType.INCOME);
        }

        @Test
        @DisplayName("should filter by transaction type - Expenses only")
        void shouldFilterByTransactionTypeExpensesOnly() {
            viewModel.setTransactionFilter(TransactionFilter.EXPENSES_ONLY);
            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
            assertThat(filtered).allMatch(t -> t.type() == TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("should filter by uncategorized")
        void shouldFilterByUncategorized() {
            viewModel.setTransactionFilter(TransactionFilter.UNCATEGORIZED);
            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
            assertThat(filtered.get(0).category()).isNull();
        }

        @Test
        @DisplayName("should filter by duplicates")
        void shouldFilterByDuplicates() {
            viewModel.setTransactionFilter(TransactionFilter.DUPLICATES);
            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
            assertThat(filtered).allMatch(ImportedTransactionRow::isDuplicate);
        }

        @Test
        @DisplayName("should search transactions by description")
        void shouldSearchTransactionsByDescription() {
            viewModel.setSearchText("Office");
            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
            assertThat(filtered.get(0).description()).contains("Office");
        }

        @Test
        @DisplayName("should search case-insensitively")
        void shouldSearchCaseInsensitively() {
            viewModel.setSearchText("office");

            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
        }

        @Test
        @DisplayName("should combine filter and search")
        void shouldCombineFilterAndSearch() {
            viewModel.setTransactionFilter(TransactionFilter.INCOME_ONLY);
            viewModel.setSearchText("payment");

            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(2); // Client payment and Existing payment
        }

        @Test
        @DisplayName("should clear selection when filter changes")
        void shouldClearSelectionWhenFilterChanges() {
            viewModel.selectAll();
            assertThat(viewModel.getSelectedCount()).isEqualTo(4);

            viewModel.setTransactionFilter(TransactionFilter.INCOME_ONLY);

            assertThat(viewModel.getSelectedCount()).isZero();
        }

        @Test
        @DisplayName("should select transaction")
        void shouldSelectTransaction() {
            ImportedTransactionRow tx = viewModel.getTransactions().get(0);

            viewModel.selectTransaction(tx.id());

            assertThat(viewModel.isSelected(tx.id())).isTrue();
            assertThat(viewModel.getSelectedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should deselect transaction")
        void shouldDeselectTransaction() {
            ImportedTransactionRow tx = viewModel.getTransactions().get(0);
            viewModel.selectTransaction(tx.id());

            viewModel.deselectTransaction(tx.id());

            assertThat(viewModel.isSelected(tx.id())).isFalse();
            assertThat(viewModel.getSelectedCount()).isZero();
        }

        @Test
        @DisplayName("should select all visible transactions")
        void shouldSelectAllVisibleTransactions() {
            viewModel.selectAll();

            assertThat(viewModel.getSelectedCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("should clear all selections")
        void shouldClearAllSelections() {
            viewModel.selectAll();
            viewModel.clearSelection();

            assertThat(viewModel.getSelectedCount()).isZero();
        }

        @Test
        @DisplayName("should apply bulk category to selected transactions")
        void shouldApplyBulkCategoryToSelectedTransactions() {
            // Select expense transactions
            viewModel.setTransactionFilter(TransactionFilter.EXPENSES_ONLY);
            viewModel.selectAll();

            // Apply category
            viewModel.applyBulkCategory(ExpenseCategory.OFFICE_COSTS);

            // Verify
            List<ImportedTransactionRow> expenses = viewModel.getFilteredTransactions();
            assertThat(expenses).allMatch(t -> t.category() == ExpenseCategory.OFFICE_COSTS);
        }

        @Test
        @DisplayName("should update category for single transaction")
        void shouldUpdateCategoryForSingleTransaction() {
            ImportedTransactionRow tx = viewModel.getTransactions().get(2); // Uncategorized one

            viewModel.updateTransactionCategory(tx.id(), ExpenseCategory.PROFESSIONAL_FEES);

            ImportedTransactionRow updated = viewModel.getTransactions().stream()
                    .filter(t -> t.id().equals(tx.id()))
                    .findFirst()
                    .orElseThrow();
            assertThat(updated.category()).isEqualTo(ExpenseCategory.PROFESSIONAL_FEES);
        }

        @Test
        @DisplayName("should clear selection after applying bulk category")
        void shouldClearSelectionAfterApplyingBulkCategory() {
            viewModel.selectAll();

            viewModel.applyBulkCategory(ExpenseCategory.ADVERTISING);

            assertThat(viewModel.getSelectedCount()).isZero();
        }

        @Test
        @DisplayName("should update uncategorized count after categorization")
        void shouldUpdateUncategorizedCountAfterCategorization() {
            assertThat(viewModel.getUncategorizedCount()).isEqualTo(1);

            ImportedTransactionRow tx = viewModel.getTransactions().stream()
                    .filter(t -> t.category() == null)
                    .findFirst()
                    .orElseThrow();
            viewModel.updateTransactionCategory(tx.id(), ExpenseCategory.PREMISES);

            assertThat(viewModel.getUncategorizedCount()).isZero();
        }

        @Test
        @DisplayName("should toggle transaction selection")
        void shouldToggleTransactionSelection() {
            ImportedTransactionRow tx = viewModel.getTransactions().get(0);

            viewModel.toggleSelection(tx.id());
            assertThat(viewModel.isSelected(tx.id())).isTrue();

            viewModel.toggleSelection(tx.id());
            assertThat(viewModel.isSelected(tx.id())).isFalse();
        }

        @Test
        @DisplayName("should get transactions to import (exclude duplicates)")
        void shouldGetTransactionsToImportExcludingDuplicates() {
            List<ImportedTransactionRow> toImport = viewModel.getTransactionsToImport();

            assertThat(toImport).hasSize(3);
            assertThat(toImport).noneMatch(ImportedTransactionRow::isDuplicate);
        }
    }

    @Nested
    @DisplayName("Step 4: Confirm & Import")
    class Step4ConfirmAndImport {

        @BeforeEach
        void setUpStep4() {
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
            viewModel.getColumnMapping().setDateColumn("Date");
            viewModel.getColumnMapping().setDescriptionColumn("Description");
            viewModel.getColumnMapping().setAmountColumn("Amount");
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");

            // Add transactions
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 5), "Client payment", new BigDecimal("1500.00"),
                    TransactionType.INCOME, ExpenseCategory.OTHER_EXPENSES, false, 85));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 6), "Office supplies", new BigDecimal("-45.50"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 92));

            viewModel.goToNextStep(); // to 2
            viewModel.goToNextStep(); // to 3
            viewModel.goToNextStep(); // to 4
        }

        @Test
        @DisplayName("should show import summary")
        void shouldShowImportSummary() {
            assertThat(viewModel.getConfirmIncomeCount()).isEqualTo(1);
            assertThat(viewModel.getConfirmIncomeTotal()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(viewModel.getConfirmExpenseCount()).isEqualTo(1);
            assertThat(viewModel.getConfirmExpenseTotal()).isEqualByComparingTo(new BigDecimal("45.50"));
        }

        @Test
        @DisplayName("should get category breakdown")
        void shouldGetCategoryBreakdown() {
            var breakdown = viewModel.getCategoryBreakdown();

            assertThat(breakdown).isNotEmpty();
            assertThat(breakdown).containsKey(ExpenseCategory.OFFICE_COSTS);
        }

        @Test
        @DisplayName("should get total transactions to import")
        void shouldGetTotalTransactionsToImport() {
            assertThat(viewModel.getTotalToImport()).isEqualTo(2);
        }

        @Test
        @DisplayName("should track import progress")
        void shouldTrackImportProgress() {
            assertThat(viewModel.isImporting()).isFalse();
            assertThat(viewModel.getImportProgress()).isZero();

            viewModel.setImporting(true);
            assertThat(viewModel.isImporting()).isTrue();

            viewModel.setImportProgress(0.5);
            assertThat(viewModel.getImportProgress()).isEqualTo(0.5);

            viewModel.setImportProgress(1.0);
            assertThat(viewModel.getImportProgress()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should show skipped items count")
        void shouldShowSkippedItemsCount() {
            // Add a duplicate
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 7), "Duplicate", new BigDecimal("100.00"),
                    TransactionType.INCOME, ExpenseCategory.OTHER_EXPENSES, true, 100));

            assertThat(viewModel.getSkippedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should get import button text with count")
        void shouldGetImportButtonTextWithCount() {
            assertThat(viewModel.getImportButtonText()).isEqualTo("Import 2 Transactions");
        }
    }

    @Nested
    @DisplayName("Currency Formatting")
    class CurrencyFormatting {

        @BeforeEach
        void setUp() {
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", new BigDecimal("1500.50"),
                    TransactionType.INCOME, ExpenseCategory.OTHER_EXPENSES, false, 85));
        }

        @Test
        @DisplayName("should format income total as GBP")
        void shouldFormatIncomeTotalAsGbp() {
            assertThat(viewModel.getFormattedIncomeTotal()).matches("\\p{Sc}1,500\\.50");
        }

        @Test
        @DisplayName("should format expense total as GBP")
        void shouldFormatExpenseTotalAsGbp() {
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 6), "Expense", new BigDecimal("-250.75"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 90));

            assertThat(viewModel.getFormattedExpenseTotal()).matches("\\p{Sc}250\\.75");
        }
    }

    @Nested
    @DisplayName("Wizard Reset")
    class WizardReset {

        @Test
        @DisplayName("should reset wizard to initial state")
        void shouldResetWizardToInitialState() {
            // Given - wizard in mid-state
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Amount"));
            viewModel.addTransaction(createTransaction(
                    LocalDate.now(), "Test", BigDecimal.TEN,
                    TransactionType.INCOME, null, false, 0));
            viewModel.goToNextStep();

            // When
            viewModel.reset();

            // Then
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            assertThat(viewModel.getSelectedFile()).isNull();
            assertThat(viewModel.getCsvHeaders()).isEmpty();
            assertThat(viewModel.getTransactions()).isEmpty();
            assertThat(viewModel.getColumnMapping().isComplete()).isFalse();
            assertThat(viewModel.getTransactionFilter()).isEqualTo(TransactionFilter.ALL);
            assertThat(viewModel.isImporting()).isFalse();
        }
    }

    @Nested
    @DisplayName("ImportedTransactionRow Display Formatting")
    class ImportedTransactionRowDisplay {

        @Test
        @DisplayName("should display formatted date")
        void shouldDisplayFormattedDate() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", BigDecimal.TEN,
                    TransactionType.INCOME, null, false, 0);

            assertThat(tx.getFormattedDate()).isEqualTo("5 Jan '26");
        }

        @Test
        @DisplayName("should display formatted amount")
        void shouldDisplayFormattedAmount() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", new BigDecimal("1500.50"),
                    TransactionType.INCOME, null, false, 0);

            assertThat(tx.getFormattedAmount()).matches("\\p{Sc}[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("should show positive sign for income")
        void shouldShowPositiveSignForIncome() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", new BigDecimal("100.00"),
                    TransactionType.INCOME, null, false, 0);

            assertThat(tx.getFormattedAmountWithSign()).startsWith("+");
        }

        @Test
        @DisplayName("should show negative sign for expense")
        void shouldShowNegativeSignForExpense() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", new BigDecimal("100.00"),
                    TransactionType.EXPENSE, null, false, 0);

            assertThat(tx.getFormattedAmountWithSign()).startsWith("-");
        }

        @Test
        @DisplayName("should display 'Uncategorized' for null category")
        void shouldDisplayUncategorizedForNullCategory() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", BigDecimal.TEN,
                    TransactionType.EXPENSE, null, false, 0);

            assertThat(tx.getCategoryDisplay()).isEqualTo("Uncategorized");
        }

        @Test
        @DisplayName("should display category name when categorized")
        void shouldDisplayCategoryNameWhenCategorized() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", BigDecimal.TEN,
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 90);

            assertThat(tx.getCategoryDisplay()).isEqualTo(ExpenseCategory.OFFICE_COSTS.getDisplayName());
        }

        @Test
        @DisplayName("should display confidence as percentage")
        void shouldDisplayConfidenceAsPercentage() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", BigDecimal.TEN,
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 85);

            assertThat(tx.getConfidenceDisplay()).isEqualTo("85%");
        }

        @Test
        @DisplayName("should assign correct CSS class based on confidence level")
        void shouldAssignCorrectCssClassBasedOnConfidenceLevel() {
            ImportedTransactionRow highConfidence = createTransaction(
                    LocalDate.now(), "Test", BigDecimal.TEN,
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 90);

            ImportedTransactionRow mediumConfidence = createTransaction(
                    LocalDate.now(), "Test", BigDecimal.TEN,
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 60);

            ImportedTransactionRow lowConfidence = createTransaction(
                    LocalDate.now(), "Test", BigDecimal.TEN,
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 30);

            assertThat(highConfidence.getConfidenceCssClass()).isEqualTo("confidence-high");
            assertThat(mediumConfidence.getConfidenceCssClass()).isEqualTo("confidence-medium");
            assertThat(lowConfidence.getConfidenceCssClass()).isEqualTo("confidence-low");
        }
    }

    // === Helper Methods ===

    private ImportedTransactionRow createTransaction(LocalDate date, String description,
                                                      BigDecimal amount, TransactionType type,
                                                      ExpenseCategory category, boolean isDuplicate,
                                                      int confidence) {
        return new ImportedTransactionRow(
                UUID.randomUUID(),
                date,
                description,
                amount.abs(),
                type,
                category,
                type == TransactionType.INCOME ? uk.selfemploy.common.enums.IncomeCategory.SALES : null,
                isDuplicate,
                confidence,
                TransactionStatus.OK
        );
    }
}
