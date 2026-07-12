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
        service = new ImportOrchestrationService(
            null, // CSV parser not needed: rows are supplied directly
            new SqliteIncomeService(businessId),
            new SqliteExpenseService(businessId),
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
    @DisplayName("re-importing an identical batch flags all 10 rows as duplicates")
    void shouldFlagIdenticalReimport() {
        List<ImportedTransactionRow> batch = sampleBatch();

        // First import: nothing exists yet, so nothing is a duplicate.
        assertThat(service.markDuplicates(batch)).noneMatch(ImportedTransactionRow::isDuplicate);
        ImportOrchestrationService.ImportResult result = service.importTransactions(batch, null);
        assertThat(result.importedCount()).isEqualTo(10);
        assertThat(result.errorCount()).isZero();

        // Second import of the same file: every row now already exists.
        List<ImportedTransactionRow> marked = service.markDuplicates(sampleBatch());
        assertThat(marked.stream().filter(ImportedTransactionRow::isDuplicate).count()).isEqualTo(10);
        assertThat(marked).allMatch(ImportedTransactionRow::isDuplicate);
    }

    @Test
    @DisplayName("a near-duplicate with a different description is not auto-flagged")
    void shouldNotFlagNearDuplicate() {
        service.importTransactions(sampleBatch(), null);

        // Same date and amount as "Invoice 1" (100.00) but a different description.
        List<ImportedTransactionRow> near = List.of(
            ImportedTransactionRow.create(DATE, "Invoice 1 - deposit",
                new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0));

        assertThat(service.markDuplicates(near)).noneMatch(ImportedTransactionRow::isDuplicate);
    }
}
