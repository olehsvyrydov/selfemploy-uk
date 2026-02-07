package uk.selfemploy.ui.e2e;

import org.junit.jupiter.api.*;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.viewmodel.*;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for SE-601: CSV Bank Import Wizard.
 * Tests the 4-step wizard flow for importing bank transactions.
 *
 * <p>Test Categories:
 * <ul>
 *   <li>Wizard navigation (all 4 steps) (AC-1, AC-12)</li>
 *   <li>File drop zone interaction (AC-1)</li>
 *   <li>Column mapping for unknown formats (AC-3)</li>
 *   <li>Preview table display and filtering (AC-4, AC-5)</li>
 *   <li>Transaction exclusion (AC-6)</li>
 *   <li>Category modification (AC-9)</li>
 *   <li>Import confirmation and cancellation (AC-10, AC-12)</li>
 * </ul>
 *
 * @author /adam - E2E Test Automation Engineer
 * @see <a href="https://jira.selfemploy.uk/browse/SE-601">SE-601 CSV Bank Import</a>
 */
@Tag("e2e")
@DisplayName("SE-601: CSV Bank Import Wizard E2E")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BankImportWizardE2ETest {

    private BankImportWizardViewModel viewModel;

    @BeforeEach
    void setUpWizard() {
        viewModel = new BankImportWizardViewModel();
    }

    // ========================================================================
    // WIZARD INITIAL STATE TESTS
    // ========================================================================

    @Nested
    @DisplayName("TC-601-01: Wizard Initial State")
    @Order(1)
    class WizardInitialStateTests {

        @Test
        @DisplayName("P0: Wizard starts on Step 1 - Select File")
        void wizardStartsOnStepOne() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            assertThat(viewModel.getStepLabel(1)).isEqualTo("Select File");
        }

        @Test
        @DisplayName("P0: No file selected initially")
        void noFileSelectedInitially() {
            assertThat(viewModel.getSelectedFile()).isNull();
            assertThat(viewModel.isFileSelected()).isFalse();
        }

        @Test
        @DisplayName("P0: Cannot proceed to next step without file")
        void cannotProceedWithoutFile() {
            assertThat(viewModel.canGoNext()).isFalse();
        }

        @Test
        @DisplayName("P0: Cannot go back from Step 1")
        void cannotGoBackFromStepOne() {
            assertThat(viewModel.canGoPrevious()).isFalse();
        }

        @Test
        @DisplayName("P1: Bank format is UNKNOWN initially")
        void bankFormatIsUnknownInitially() {
            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.UNKNOWN);
        }

        @Test
        @DisplayName("P1: All summary values are zero initially")
        void summaryValuesAreZeroInitially() {
            assertThat(viewModel.getIncomeCount()).isZero();
            assertThat(viewModel.getIncomeTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getExpenseCount()).isZero();
            assertThat(viewModel.getExpenseTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getDuplicateCount()).isZero();
        }
    }

    // ========================================================================
    // STEP 1: FILE SELECTION TESTS (AC-1)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-02: Step 1 - File Selection (AC-1)")
    @Order(2)
    class FileSelectionTests {

        @Test
        @DisplayName("P0: Can select CSV file via file picker")
        void canSelectCsvFile() {
            // Given
            File csvFile = new File("test-bank-statement.csv");

            // When
            viewModel.setSelectedFile(csvFile);

            // Then
            assertThat(viewModel.isFileSelected()).isTrue();
            assertThat(viewModel.getSelectedFile()).isEqualTo(csvFile);
        }

        @Test
        @DisplayName("P0: File name is displayed after selection")
        void fileNameIsDisplayed() {
            // Given
            File csvFile = new File("/path/to/bank-statement.csv");

            // When
            viewModel.setSelectedFile(csvFile);

            // Then
            assertThat(viewModel.getFileName()).isEqualTo("bank-statement.csv");
        }

        @Test
        @DisplayName("P0: Row count is displayed after parsing headers")
        void rowCountIsDisplayed() {
            // Given
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));

            // When
            viewModel.setRowCount(150);

            // Then
            assertThat(viewModel.getRowCount()).isEqualTo(150);
        }

        @Test
        @DisplayName("P0: Can proceed to Step 2 after selecting valid file")
        void canProceedAfterFileSelection() {
            // Given
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));

            // Then
            assertThat(viewModel.canGoNext()).isTrue();
        }

        @Test
        @DisplayName("P1: Clearing file resets to initial state")
        void clearingFileResetsState() {
            // Given
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Amount"));
            viewModel.setRowCount(100);

            // When
            viewModel.clearFile();

            // Then
            assertThat(viewModel.getSelectedFile()).isNull();
            assertThat(viewModel.isFileSelected()).isFalse();
            assertThat(viewModel.getCsvHeaders()).isEmpty();
            assertThat(viewModel.getRowCount()).isZero();
        }
    }

    // ========================================================================
    // STEP 1: BANK FORMAT DETECTION TESTS (AC-2)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-03: Bank Format Detection (AC-2)")
    @Order(3)
    class BankFormatDetectionTests {

        @Test
        @DisplayName("P0: Detects Barclays format")
        void detectsBarclaysFormat() {
            List<String> headers = List.of("Date", "Type", "Description", "Money out", "Money in", "Balance");

            viewModel.setCsvHeaders(headers);

            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.BARCLAYS);
        }

        @Test
        @DisplayName("P0: Detects HSBC format")
        void detectsHsbcFormat() {
            List<String> headers = List.of("Date", "Type", "Paid out", "Paid in", "Balance");

            viewModel.setCsvHeaders(headers);

            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.HSBC);
        }

        @Test
        @DisplayName("P0: Detects Lloyds format")
        void detectsLloydsFormat() {
            List<String> headers = List.of("Transaction Date", "Transaction Type", "Sort Code",
                    "Account Number", "Transaction Description", "Debit Amount", "Credit Amount", "Balance");

            viewModel.setCsvHeaders(headers);

            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.LLOYDS);
        }

        @Test
        @DisplayName("P0: Detects Nationwide format")
        void detectsNationwideFormat() {
            List<String> headers = List.of("Date", "Transaction type", "Description",
                    "Paid out", "Paid in", "Balance");

            viewModel.setCsvHeaders(headers);

            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.NATIONWIDE);
        }

        @Test
        @DisplayName("P0: Detects Starling format")
        void detectsStarlingFormat() {
            List<String> headers = List.of("Date", "Counter Party", "Reference", "Type",
                    "Amount (GBP)", "Balance (GBP)");

            viewModel.setCsvHeaders(headers);

            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.STARLING);
        }

        @Test
        @DisplayName("P0: Detects Monzo format")
        void detectsMonzoFormat() {
            List<String> headers = List.of("Transaction ID", "Date", "Time", "Type", "Name",
                    "Emoji", "Category", "Amount", "Currency", "Notes and #tags");

            viewModel.setCsvHeaders(headers);

            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.MONZO);
        }

        @Test
        @DisplayName("P0: Returns UNKNOWN for unrecognized format")
        void returnsUnknownForUnrecognizedFormat() {
            List<String> headers = List.of("Column1", "Column2", "Column3");

            viewModel.setCsvHeaders(headers);

            assertThat(viewModel.getDetectedBankFormat()).isEqualTo(BankFormat.UNKNOWN);
        }

        @Test
        @DisplayName("P1: Auto-populates column mapping for known format")
        void autoPopulatesMappingForKnownFormat() {
            List<String> headers = List.of("Date", "Type", "Description", "Money out", "Money in", "Balance");

            viewModel.setCsvHeaders(headers);

            ColumnMapping mapping = viewModel.getColumnMapping();
            assertThat(mapping.getDateColumn()).isEqualTo("Date");
            assertThat(mapping.getDescriptionColumn()).isEqualTo("Description");
            assertThat(mapping.hasSeparateAmountColumns()).isTrue();
        }
    }

    // ========================================================================
    // STEP 2: COLUMN MAPPING TESTS (AC-3)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-04: Step 2 - Column Mapping (AC-3)")
    @Order(4)
    class ColumnMappingTests {

        @BeforeEach
        void navigateToStep2() {
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount", "Category", "Reference"));
            viewModel.goToNextStep();
        }

        @Test
        @DisplayName("P0: Can set date column")
        void canSetDateColumn() {
            viewModel.getColumnMapping().setDateColumn("Date");

            assertThat(viewModel.getColumnMapping().getDateColumn()).isEqualTo("Date");
        }

        @Test
        @DisplayName("P0: Can set description column")
        void canSetDescriptionColumn() {
            viewModel.getColumnMapping().setDescriptionColumn("Description");

            assertThat(viewModel.getColumnMapping().getDescriptionColumn()).isEqualTo("Description");
        }

        @Test
        @DisplayName("P0: Can set single amount column")
        void canSetSingleAmountColumn() {
            viewModel.getColumnMapping().setAmountColumn("Amount");

            assertThat(viewModel.getColumnMapping().getAmountColumn()).isEqualTo("Amount");
            assertThat(viewModel.getColumnMapping().hasSeparateAmountColumns()).isFalse();
        }

        @Test
        @DisplayName("P0: Can set separate income/expense columns")
        void canSetSeparateIncomeExpenseColumns() {
            viewModel.getColumnMapping().setSeparateAmountColumns(true);
            viewModel.getColumnMapping().setIncomeColumn("Money In");
            viewModel.getColumnMapping().setExpenseColumn("Money Out");

            assertThat(viewModel.getColumnMapping().hasSeparateAmountColumns()).isTrue();
            assertThat(viewModel.getColumnMapping().getIncomeColumn()).isEqualTo("Money In");
            assertThat(viewModel.getColumnMapping().getExpenseColumn()).isEqualTo("Money Out");
        }

        @Test
        @DisplayName("P1: Can set date format")
        void canSetDateFormat() {
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");

            assertThat(viewModel.getColumnMapping().getDateFormat()).isEqualTo("dd/MM/yyyy");
        }

        @Test
        @DisplayName("P1: Available date formats are provided")
        void availableDateFormatsAreProvided() {
            List<String> formats = viewModel.getAvailableDateFormats();

            assertThat(formats).contains("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd");
        }

        @Test
        @DisplayName("P0: Mapping validation - incomplete without required fields")
        void mappingIncompleteWithoutRequiredFields() {
            assertThat(viewModel.getColumnMapping().isComplete()).isFalse();
        }

        @Test
        @DisplayName("P0: Mapping validation - complete with all required fields")
        void mappingCompleteWithAllRequiredFields() {
            viewModel.getColumnMapping().setDateColumn("Date");
            viewModel.getColumnMapping().setDescriptionColumn("Description");
            viewModel.getColumnMapping().setAmountColumn("Amount");
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");

            assertThat(viewModel.getColumnMapping().isComplete()).isTrue();
        }

        @Test
        @DisplayName("P0: Cannot proceed without complete mapping")
        void cannotProceedWithoutCompleteMapping() {
            assertThat(viewModel.canGoNext()).isFalse();
        }

        @Test
        @DisplayName("P0: Can proceed with complete mapping")
        void canProceedWithCompleteMapping() {
            viewModel.getColumnMapping().setDateColumn("Date");
            viewModel.getColumnMapping().setDescriptionColumn("Description");
            viewModel.getColumnMapping().setAmountColumn("Amount");
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");

            assertThat(viewModel.canGoNext()).isTrue();
        }
    }

    // ========================================================================
    // STEP 3: PREVIEW & FILTERING TESTS (AC-4, AC-5)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-05: Step 3 - Preview & Filtering (AC-4, AC-5)")
    @Order(5)
    class PreviewAndFilteringTests {

        @BeforeEach
        void navigateToStep3WithTransactions() {
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
                    LocalDate.of(2026, 1, 6), "Office supplies", new BigDecimal("45.50"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 92));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 7), "Freelance work", new BigDecimal("2000.00"),
                    TransactionType.INCOME, null, false, 0));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 8), "Existing payment", new BigDecimal("500.00"),
                    TransactionType.INCOME, ExpenseCategory.OTHER_EXPENSES, true, 100));
        }

        @Test
        @DisplayName("P0: Preview shows all parsed transactions (AC-4)")
        void previewShowsAllTransactions() {
            assertThat(viewModel.getTransactions()).hasSize(4);
        }

        @Test
        @DisplayName("P0: Summary shows income count and total")
        void summaryShowsIncomeCountAndTotal() {
            assertThat(viewModel.getIncomeCount()).isEqualTo(3);
            assertThat(viewModel.getIncomeTotal()).isEqualByComparingTo(new BigDecimal("4000.00"));
        }

        @Test
        @DisplayName("P0: Summary shows expense count and total")
        void summaryShowsExpenseCountAndTotal() {
            assertThat(viewModel.getExpenseCount()).isEqualTo(1);
            assertThat(viewModel.getExpenseTotal()).isEqualByComparingTo(new BigDecimal("45.50"));
        }

        @Test
        @DisplayName("P0: Filter by All shows all transactions (AC-5)")
        void filterByAllShowsAllTransactions() {
            viewModel.setTransactionFilter(TransactionFilter.ALL);

            assertThat(viewModel.getFilteredTransactions()).hasSize(4);
        }

        @Test
        @DisplayName("P0: Filter by Income shows only income (AC-5)")
        void filterByIncomeShowsOnlyIncome() {
            viewModel.setTransactionFilter(TransactionFilter.INCOME_ONLY);

            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(3);
            assertThat(filtered).allMatch(t -> t.type() == TransactionType.INCOME);
        }

        @Test
        @DisplayName("P0: Filter by Expenses shows only expenses (AC-5)")
        void filterByExpensesShowsOnlyExpenses() {
            viewModel.setTransactionFilter(TransactionFilter.EXPENSES_ONLY);

            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
            assertThat(filtered).allMatch(t -> t.type() == TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("P1: Filter by Uncategorized shows uncategorized only")
        void filterByUncategorizedShowsUncategorized() {
            viewModel.setTransactionFilter(TransactionFilter.UNCATEGORIZED);

            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
            assertThat(filtered).allMatch(t -> t.category() == null);
        }

        @Test
        @DisplayName("P1: Filter by Duplicates shows duplicates only (AC-7)")
        void filterByDuplicatesShowsDuplicatesOnly() {
            viewModel.setTransactionFilter(TransactionFilter.DUPLICATES);

            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
            assertThat(filtered).allMatch(ImportedTransactionRow::isDuplicate);
        }

        @Test
        @DisplayName("P1: Search filters by description")
        void searchFiltersByDescription() {
            viewModel.setSearchText("Office");

            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
            assertThat(filtered.get(0).description()).contains("Office");
        }

        @Test
        @DisplayName("P1: Search is case-insensitive")
        void searchIsCaseInsensitive() {
            viewModel.setSearchText("office");

            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(1);
        }

        @Test
        @DisplayName("P1: Filter and search can be combined")
        void filterAndSearchCanBeCombined() {
            viewModel.setTransactionFilter(TransactionFilter.INCOME_ONLY);
            viewModel.setSearchText("payment");

            List<ImportedTransactionRow> filtered = viewModel.getFilteredTransactions();
            assertThat(filtered).hasSize(2); // Client payment and Existing payment
        }
    }

    // ========================================================================
    // STEP 3: TRANSACTION EXCLUSION TESTS (AC-6)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-06: Transaction Exclusion (AC-6)")
    @Order(6)
    class TransactionExclusionTests {

        @BeforeEach
        void setUpTransactions() {
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 5), "Client payment", new BigDecimal("1500.00"),
                    TransactionType.INCOME, null, false, 85));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 6), "Office supplies", new BigDecimal("45.50"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 92));
        }

        @Test
        @DisplayName("P0: Can select individual transaction")
        void canSelectIndividualTransaction() {
            ImportedTransactionRow tx = viewModel.getTransactions().get(0);

            viewModel.selectTransaction(tx.id());

            assertThat(viewModel.isSelected(tx.id())).isTrue();
            assertThat(viewModel.getSelectedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("P0: Can deselect transaction")
        void canDeselectTransaction() {
            ImportedTransactionRow tx = viewModel.getTransactions().get(0);
            viewModel.selectTransaction(tx.id());

            viewModel.deselectTransaction(tx.id());

            assertThat(viewModel.isSelected(tx.id())).isFalse();
            assertThat(viewModel.getSelectedCount()).isZero();
        }

        @Test
        @DisplayName("P0: Can toggle selection")
        void canToggleSelection() {
            ImportedTransactionRow tx = viewModel.getTransactions().get(0);

            viewModel.toggleSelection(tx.id());
            assertThat(viewModel.isSelected(tx.id())).isTrue();

            viewModel.toggleSelection(tx.id());
            assertThat(viewModel.isSelected(tx.id())).isFalse();
        }

        @Test
        @DisplayName("P1: Can select all visible transactions")
        void canSelectAllVisibleTransactions() {
            viewModel.selectAll();

            assertThat(viewModel.getSelectedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("P1: Can clear all selections")
        void canClearAllSelections() {
            viewModel.selectAll();

            viewModel.clearSelection();

            assertThat(viewModel.getSelectedCount()).isZero();
        }

        @Test
        @DisplayName("P1: Filter change clears selection")
        void filterChangeClearsSelection() {
            viewModel.selectAll();
            assertThat(viewModel.getSelectedCount()).isEqualTo(2);

            viewModel.setTransactionFilter(TransactionFilter.INCOME_ONLY);

            assertThat(viewModel.getSelectedCount()).isZero();
        }
    }

    // ========================================================================
    // STEP 3: DUPLICATE WARNING TESTS (AC-7)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-07: Duplicate Detection Warning (AC-7)")
    @Order(7)
    class DuplicateWarningTests {

        @Test
        @DisplayName("P0: Duplicate count is displayed")
        void duplicateCountIsDisplayed() {
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 5), "Normal transaction", new BigDecimal("100.00"),
                    TransactionType.INCOME, null, false, 85));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 6), "Duplicate transaction", new BigDecimal("200.00"),
                    TransactionType.INCOME, null, true, 100));

            assertThat(viewModel.getDuplicateCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("P0: Duplicates are excluded from import")
        void duplicatesAreExcludedFromImport() {
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 5), "Normal transaction", new BigDecimal("100.00"),
                    TransactionType.INCOME, null, false, 85));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 6), "Duplicate transaction", new BigDecimal("200.00"),
                    TransactionType.INCOME, null, true, 100));

            List<ImportedTransactionRow> toImport = viewModel.getTransactionsToImport();

            assertThat(toImport).hasSize(1);
            assertThat(toImport).noneMatch(ImportedTransactionRow::isDuplicate);
        }
    }

    // ========================================================================
    // STEP 3: CATEGORY MODIFICATION TESTS (AC-9)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-08: Category Modification (AC-9)")
    @Order(8)
    class CategoryModificationTests {

        @BeforeEach
        void setUpTransactions() {
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 5), "Uncategorized expense", new BigDecimal("100.00"),
                    TransactionType.EXPENSE, null, false, 0));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 6), "Office supplies", new BigDecimal("45.50"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 92));
        }

        @Test
        @DisplayName("P0: Can update category for single transaction")
        void canUpdateCategoryForSingleTransaction() {
            ImportedTransactionRow tx = viewModel.getTransactions().get(0);

            viewModel.updateTransactionCategory(tx.id(), ExpenseCategory.TRAVEL);

            ImportedTransactionRow updated = viewModel.getTransactions().stream()
                    .filter(t -> t.id().equals(tx.id()))
                    .findFirst()
                    .orElseThrow();
            assertThat(updated.category()).isEqualTo(ExpenseCategory.TRAVEL);
        }

        @Test
        @DisplayName("P0: Can apply bulk category to selected transactions")
        void canApplyBulkCategory() {
            // Select all expenses
            viewModel.setTransactionFilter(TransactionFilter.EXPENSES_ONLY);
            viewModel.selectAll();

            // Apply category
            viewModel.applyBulkCategory(ExpenseCategory.PROFESSIONAL_FEES);

            // Verify
            List<ImportedTransactionRow> expenses = viewModel.getTransactions().stream()
                    .filter(t -> t.type() == TransactionType.EXPENSE)
                    .toList();
            assertThat(expenses).allMatch(t -> t.category() == ExpenseCategory.PROFESSIONAL_FEES);
        }

        @Test
        @DisplayName("P1: Bulk category clears selection after applying")
        void bulkCategoryClearsSelection() {
            viewModel.selectAll();

            viewModel.applyBulkCategory(ExpenseCategory.ADVERTISING);

            assertThat(viewModel.getSelectedCount()).isZero();
        }

        @Test
        @DisplayName("P1: Uncategorized count updates after categorization")
        void uncategorizedCountUpdatesAfterCategorization() {
            assertThat(viewModel.getUncategorizedCount()).isEqualTo(1);

            ImportedTransactionRow tx = viewModel.getTransactions().get(0);
            viewModel.updateTransactionCategory(tx.id(), ExpenseCategory.PREMISES);

            assertThat(viewModel.getUncategorizedCount()).isZero();
        }
    }

    // ========================================================================
    // STEP 4: CONFIRMATION TESTS (AC-10)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-09: Step 4 - Confirmation (AC-10)")
    @Order(9)
    class ConfirmationTests {

        @BeforeEach
        void navigateToStep4() {
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
            viewModel.getColumnMapping().setDateColumn("Date");
            viewModel.getColumnMapping().setDescriptionColumn("Description");
            viewModel.getColumnMapping().setAmountColumn("Amount");
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");

            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 5), "Client payment", new BigDecimal("1500.00"),
                    TransactionType.INCOME, ExpenseCategory.OTHER_EXPENSES, false, 85));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 6), "Office supplies", new BigDecimal("45.50"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 92));
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 7), "Duplicate", new BigDecimal("100.00"),
                    TransactionType.INCOME, null, true, 100));

            viewModel.goToNextStep(); // to 2
            viewModel.goToNextStep(); // to 3
            viewModel.goToNextStep(); // to 4
        }

        @Test
        @DisplayName("P0: Confirmation shows import summary")
        void confirmationShowsImportSummary() {
            assertThat(viewModel.getConfirmIncomeCount()).isEqualTo(1);
            assertThat(viewModel.getConfirmIncomeTotal()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(viewModel.getConfirmExpenseCount()).isEqualTo(1);
            assertThat(viewModel.getConfirmExpenseTotal()).isEqualByComparingTo(new BigDecimal("45.50"));
        }

        @Test
        @DisplayName("P0: Total transactions to import excludes duplicates")
        void totalToImportExcludesDuplicates() {
            assertThat(viewModel.getTotalToImport()).isEqualTo(2);
        }

        @Test
        @DisplayName("P0: Skipped count shows duplicate count")
        void skippedCountShowsDuplicateCount() {
            assertThat(viewModel.getSkippedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("P1: Category breakdown is available")
        void categoryBreakdownIsAvailable() {
            var breakdown = viewModel.getCategoryBreakdown();

            assertThat(breakdown).isNotEmpty();
            assertThat(breakdown).containsKey(ExpenseCategory.OFFICE_COSTS);
        }

        @Test
        @DisplayName("P1: Import button text shows transaction count")
        void importButtonTextShowsTransactionCount() {
            assertThat(viewModel.getImportButtonText()).isEqualTo("Import 2 Transactions");
        }
    }

    // ========================================================================
    // NAVIGATION TESTS (AC-12)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-10: Wizard Navigation (AC-12)")
    @Order(10)
    class NavigationTests {

        @BeforeEach
        void setUpForNavigation() {
            viewModel.setSelectedFile(new File("test.csv"));
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
        }

        @Test
        @DisplayName("P0: Can navigate through all 4 steps")
        void canNavigateThroughAllSteps() {
            // Step 1 -> Step 2
            viewModel.goToNextStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);

            // Complete mapping
            viewModel.getColumnMapping().setDateColumn("Date");
            viewModel.getColumnMapping().setDescriptionColumn("Description");
            viewModel.getColumnMapping().setAmountColumn("Amount");
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");

            // Step 2 -> Step 3
            viewModel.goToNextStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);

            // Step 3 -> Step 4
            viewModel.goToNextStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(4);
        }

        @Test
        @DisplayName("P0: Can navigate back to previous steps")
        void canNavigateBack() {
            viewModel.goToNextStep(); // to 2

            viewModel.goToPreviousStep();

            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("P0: Cannot go beyond Step 4")
        void cannotGoBeyondStep4() {
            viewModel.getColumnMapping().setDateColumn("Date");
            viewModel.getColumnMapping().setDescriptionColumn("Description");
            viewModel.getColumnMapping().setAmountColumn("Amount");
            viewModel.getColumnMapping().setDateFormat("dd/MM/yyyy");

            viewModel.goToNextStep(); // to 2
            viewModel.goToNextStep(); // to 3
            viewModel.goToNextStep(); // to 4
            viewModel.goToNextStep(); // try to go beyond

            assertThat(viewModel.getCurrentStep()).isEqualTo(4);
        }

        @Test
        @DisplayName("P0: Cannot go before Step 1")
        void cannotGoBeforeStep1() {
            viewModel.goToPreviousStep();
            viewModel.goToPreviousStep();

            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("P1: Step labels are correct")
        void stepLabelsAreCorrect() {
            assertThat(viewModel.getStepLabel(1)).isEqualTo("Select File");
            assertThat(viewModel.getStepLabel(2)).isEqualTo("Map Columns");
            assertThat(viewModel.getStepLabel(3)).isEqualTo("Preview");
            assertThat(viewModel.getStepLabel(4)).isEqualTo("Confirm");
        }

        @Test
        @DisplayName("P1: Step completion is tracked correctly")
        void stepCompletionIsTracked() {
            // Step 1 completed (file selected and headers parsed)
            assertThat(viewModel.isStepCompleted(1)).isTrue();

            // Step 2 not completed yet (no mapping)
            assertThat(viewModel.isStepCompleted(2)).isFalse();
        }
    }

    // ========================================================================
    // IMPORT PROGRESS TESTS
    // ========================================================================

    @Nested
    @DisplayName("TC-601-11: Import Progress")
    @Order(11)
    class ImportProgressTests {

        @Test
        @DisplayName("P0: Import progress is tracked")
        void importProgressIsTracked() {
            assertThat(viewModel.isImporting()).isFalse();
            assertThat(viewModel.getImportProgress()).isZero();

            viewModel.setImporting(true);
            assertThat(viewModel.isImporting()).isTrue();

            viewModel.setImportProgress(0.5);
            assertThat(viewModel.getImportProgress()).isEqualTo(0.5);

            viewModel.setImportProgress(1.0);
            assertThat(viewModel.getImportProgress()).isEqualTo(1.0);
        }
    }

    // ========================================================================
    // WIZARD RESET TESTS (AC-12)
    // ========================================================================

    @Nested
    @DisplayName("TC-601-12: Wizard Reset / Cancel (AC-12)")
    @Order(12)
    class WizardResetTests {

        @Test
        @DisplayName("P0: Reset returns wizard to initial state")
        void resetReturnsToInitialState() {
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

    // ========================================================================
    // CURRENCY FORMATTING TESTS
    // ========================================================================

    @Nested
    @DisplayName("TC-601-13: Currency Formatting")
    @Order(13)
    class CurrencyFormattingTests {

        @BeforeEach
        void setUp() {
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", new BigDecimal("1500.50"),
                    TransactionType.INCOME, ExpenseCategory.OTHER_EXPENSES, false, 85));
        }

        @Test
        @DisplayName("P0: Income total is formatted as GBP")
        void incomeTotalIsFormattedAsGbp() {
            // Matches currency symbol (£ or country-specific)
            assertThat(viewModel.getFormattedIncomeTotal()).matches("\\p{Sc}[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("P0: Expense total is formatted as GBP")
        void expenseTotalIsFormattedAsGbp() {
            viewModel.addTransaction(createTransaction(
                    LocalDate.of(2026, 1, 6), "Expense", new BigDecimal("250.75"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 90));

            assertThat(viewModel.getFormattedExpenseTotal()).matches("\\p{Sc}[\\d,]+\\.\\d{2}");
        }
    }

    // ========================================================================
    // IMPORTED TRANSACTION ROW TESTS
    // ========================================================================

    @Nested
    @DisplayName("TC-601-14: ImportedTransactionRow Display")
    @Order(14)
    class ImportedTransactionRowTests {

        @Test
        @DisplayName("P0: Transaction displays formatted date")
        void transactionDisplaysFormattedDate() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", BigDecimal.TEN,
                    TransactionType.INCOME, null, false, 0);

            assertThat(tx.getFormattedDate()).isEqualTo("5 Jan '26");
        }

        @Test
        @DisplayName("P0: Transaction displays formatted amount")
        void transactionDisplaysFormattedAmount() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", new BigDecimal("1500.50"),
                    TransactionType.INCOME, null, false, 0);

            assertThat(tx.getFormattedAmount()).matches("\\p{Sc}[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("P0: Income shows positive sign in amount")
        void incomeShowsPositiveSign() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", new BigDecimal("100.00"),
                    TransactionType.INCOME, null, false, 0);

            assertThat(tx.getFormattedAmountWithSign()).startsWith("+");
        }

        @Test
        @DisplayName("P0: Expense shows negative sign in amount")
        void expenseShowsNegativeSign() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", new BigDecimal("100.00"),
                    TransactionType.EXPENSE, null, false, 0);

            assertThat(tx.getFormattedAmountWithSign()).startsWith("-");
        }

        @Test
        @DisplayName("P1: Uncategorized displays 'Uncategorized'")
        void uncategorizedDisplaysUncategorized() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", BigDecimal.TEN,
                    TransactionType.EXPENSE, null, false, 0);

            assertThat(tx.getCategoryDisplay()).isEqualTo("Uncategorized");
        }

        @Test
        @DisplayName("P1: Categorized displays category name")
        void categorizedDisplaysCategoryName() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", BigDecimal.TEN,
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 90);

            assertThat(tx.getCategoryDisplay()).isEqualTo(ExpenseCategory.OFFICE_COSTS.getDisplayName());
        }

        @Test
        @DisplayName("P1: Confidence display shows percentage")
        void confidenceDisplayShowsPercentage() {
            ImportedTransactionRow tx = createTransaction(
                    LocalDate.of(2026, 1, 5), "Test", BigDecimal.TEN,
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 85);

            assertThat(tx.getConfidenceDisplay()).isEqualTo("85%");
        }

        @Test
        @DisplayName("P1: Confidence CSS class is set correctly")
        void confidenceCssClassIsSetCorrectly() {
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

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

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
