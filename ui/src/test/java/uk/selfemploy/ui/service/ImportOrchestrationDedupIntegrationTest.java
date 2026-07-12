package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.viewmodel.ImportedTransactionRow;
import uk.selfemploy.ui.viewmodel.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Store-backed integration test for CSV re-import duplicate detection (B4).
 *
 * <p>Drives {@link ImportOrchestrationService} against the real SQLite-backed
 * income and expense services: import a batch, then re-run the same batch and
 * confirm every row is flagged as an existing duplicate.</p>
 */
@DisplayName("ImportOrchestrationService duplicate detection over SQLite")
class ImportOrchestrationDedupIntegrationTest {

    private static final LocalDate DATE = LocalDate.of(2025, 6, 15);

    private UUID businessId;
    private SqliteIncomeService incomeService;
    private SqliteExpenseService expenseService;
    private SqliteBankTransactionService bankTransactionService;
    private ImportOrchestrationService service;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetInstance();
        businessId = UUID.randomUUID();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
        incomeService = new SqliteIncomeService(businessId);
        expenseService = new SqliteExpenseService(businessId);
        bankTransactionService = new SqliteBankTransactionService(businessId);
        service = new ImportOrchestrationService(
            null, // CSV parser not needed: rows are supplied directly
            incomeService,
            expenseService,
            bankTransactionService,
            businessId);
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.resetTestData();
    }

    private List<ImportedTransactionRow> sampleBatch() {
        List<ImportedTransactionRow> rows = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            rows.add(ImportedTransactionRow.create(DATE, "Invoice " + i,
                new BigDecimal(100 * i + ".00"), TransactionType.INCOME, null, false, 0));
        }
        rows.add(ImportedTransactionRow.create(DATE, "Stationery",
            new BigDecimal("12.50"), TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 0));
        rows.add(ImportedTransactionRow.create(DATE, "Postage",
            new BigDecimal("6.20"), TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 0));
        return rows; // 10 rows total
    }

    @Test
    @DisplayName("re-importing an identical batch stages nothing new (already-staged rows are skipped)")
    void shouldSkipStagedRowsOnReimport() {
        List<ImportedTransactionRow> batch = sampleBatch();

        ImportOrchestrationService.ImportResult first = service.importTransactions(batch, null);
        assertThat(first.importedCount()).isEqualTo(10);
        assertThat(first.errorCount()).isZero();
        assertThat(bankTransactionService.count()).isEqualTo(10);

        // Second import of the same file: every row's hash already exists in bank_transactions.
        ImportOrchestrationService.ImportResult second = service.importTransactions(sampleBatch(), null);
        assertThat(second.importedCount()).isZero();
        assertThat(second.skippedCount()).isEqualTo(10);
        assertThat(bankTransactionService.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("an income and an expense with the same date/amount/description both stage")
    void incomeAndExpenseDoNotCollide() {
        List<ImportedTransactionRow> rows = List.of(
            ImportedTransactionRow.create(DATE, "Widget", new BigDecimal("100.00"),
                TransactionType.INCOME, null, false, 0),
            ImportedTransactionRow.create(DATE, "Widget", new BigDecimal("100.00"),
                TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 0));

        ImportOrchestrationService.ImportResult result = service.importTransactions(rows, null);

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isZero();
        assertThat(bankTransactionService.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("markDuplicates flags a row matching an already-committed income record")
    void shouldFlagRowMatchingCommittedIncome() {
        // A committed income record (as if a prior review committed it).
        incomeService.create(businessId, DATE, new BigDecimal("100.00"), "Invoice 1",
            uk.selfemploy.common.enums.IncomeCategory.SALES, null);

        List<ImportedTransactionRow> match = List.of(
            ImportedTransactionRow.create(DATE, "Invoice 1",
                new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0));

        assertThat(service.markDuplicates(match)).allMatch(ImportedTransactionRow::isDuplicate);
    }

    @Test
    @DisplayName("markDuplicates does not flag a near-duplicate with a different description")
    void shouldNotFlagNearDuplicate() {
        incomeService.create(businessId, DATE, new BigDecimal("100.00"), "Invoice 1",
            uk.selfemploy.common.enums.IncomeCategory.SALES, null);

        // Same date and amount but a different description.
        List<ImportedTransactionRow> near = List.of(
            ImportedTransactionRow.create(DATE, "Invoice 1 - deposit",
                new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0));

        assertThat(service.markDuplicates(near)).noneMatch(ImportedTransactionRow::isDuplicate);
    }
}
