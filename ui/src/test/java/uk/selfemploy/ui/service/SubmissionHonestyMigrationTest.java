package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the submission-history honesty migration relabels the fabricated
 * "SA-..." / ACCEPTED rows left by the old simulated annual-submission flow as
 * NOT_SUBMITTED, so the history screen can no longer present them as HMRC filings.
 */
@DisplayName("Submission honesty migration")
class SubmissionHonestyMigrationTest {

    private SqliteDataStore dataStore;
    private UUID businessId;

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
        dataStore = SqliteDataStore.getInstance();
        businessId = UUID.randomUUID();
        dataStore.ensureBusinessExists(businessId);
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.resetTestData();
    }

    private SubmissionRecord record(String id, String status, String reference) {
        return new SubmissionRecord(
            id,
            businessId.toString(),
            "ANNUAL",
            2025,
            LocalDate.of(2025, 4, 6),
            LocalDate.of(2026, 4, 5),
            new BigDecimal("50000.00"),
            new BigDecimal("10000.00"),
            new BigDecimal("40000.00"),
            status,
            reference,
            null,
            Instant.parse("2026-01-24T14:35:00Z")
        );
    }

    @Test
    @DisplayName("relabels a fabricated SA- ACCEPTED row as NOT_SUBMITTED and clears the fake reference")
    void relabelsFabricatedRow() {
        String id = UUID.randomUUID().toString();
        dataStore.saveSubmission(record(id, "ACCEPTED", "SA-12345678"));

        dataStore.migrateSubmissionHonesty();

        Optional<SubmissionRecord> after = dataStore.findSubmissionById(id);
        assertThat(after).isPresent();
        assertThat(after.get().status()).isEqualTo("NOT_SUBMITTED");
        assertThat(after.get().hmrcReference()).isNull();
    }

    @Test
    @DisplayName("leaves a genuine HMRC reference untouched")
    void leavesGenuineRowUntouched() {
        String id = UUID.randomUUID().toString();
        dataStore.saveSubmission(record(id, "ACCEPTED", "X9ABCD1234567"));

        dataStore.migrateSubmissionHonesty();

        Optional<SubmissionRecord> after = dataStore.findSubmissionById(id);
        assertThat(after).isPresent();
        assertThat(after.get().status()).isEqualTo("ACCEPTED");
        assertThat(after.get().hmrcReference()).isEqualTo("X9ABCD1234567");
    }

    @Test
    @DisplayName("is idempotent - a second run changes nothing")
    void isIdempotent() {
        String id = UUID.randomUUID().toString();
        dataStore.saveSubmission(record(id, "ACCEPTED", "SA-PERIOD-TEST"));

        dataStore.migrateSubmissionHonesty();
        dataStore.migrateSubmissionHonesty();

        Optional<SubmissionRecord> after = dataStore.findSubmissionById(id);
        assertThat(after).isPresent();
        assertThat(after.get().status()).isEqualTo("NOT_SUBMITTED");
        assertThat(after.get().hmrcReference()).isNull();
    }

    @Test
    @DisplayName("rebuilds a legacy table whose CHECK constraint predates NOT_SUBMITTED")
    void rebuildsLegacyTableConstraint() throws Exception {
        // Recreate the submissions table with the old four-value CHECK constraint
        // and seed it with a fabricated row, exactly as older builds left it.
        dataStore.executeRawForTest("DROP TABLE submissions");
        dataStore.executeRawForTest("""
            CREATE TABLE submissions (
                id TEXT PRIMARY KEY,
                business_id TEXT NOT NULL,
                type TEXT NOT NULL,
                tax_year_start INTEGER NOT NULL,
                period_start TEXT NOT NULL,
                period_end TEXT NOT NULL,
                total_income TEXT NOT NULL,
                total_expenses TEXT NOT NULL,
                net_profit TEXT NOT NULL,
                status TEXT NOT NULL,
                hmrc_reference TEXT,
                error_message TEXT,
                submitted_at TEXT NOT NULL,
                created_at TEXT NOT NULL DEFAULT (datetime('now')),
                CHECK (type IN ('QUARTERLY_Q1', 'QUARTERLY_Q2', 'QUARTERLY_Q3', 'QUARTERLY_Q4', 'ANNUAL')),
                CHECK (status IN ('PENDING', 'SUBMITTED', 'ACCEPTED', 'REJECTED'))
            )
        """);
        dataStore.executeRawForTest(
            "INSERT INTO submissions (id, business_id, type, tax_year_start, period_start, "
            + "period_end, total_income, total_expenses, net_profit, status, hmrc_reference, "
            + "submitted_at) VALUES ('legacy-1', '" + businessId + "', 'ANNUAL', 2024, "
            + "'2024-04-06', '2025-04-05', '10.00', '2.00', '8.00', 'ACCEPTED', 'SA-FIRST', "
            + "'2025-01-10T09:00:00Z')");

        dataStore.migrateSubmissionHonesty();

        Optional<SubmissionRecord> after = dataStore.findSubmissionById("legacy-1");
        assertThat(after).isPresent();
        assertThat(after.get().status()).isEqualTo("NOT_SUBMITTED");
        assertThat(after.get().hmrcReference()).isNull();

        // The widened constraint must now accept a NOT_SUBMITTED insert; if the
        // CHECK still rejected it the row would never persist.
        String freshId = UUID.randomUUID().toString();
        dataStore.saveSubmission(record(freshId, "NOT_SUBMITTED", null));
        Optional<SubmissionRecord> fresh = dataStore.findSubmissionById(freshId);
        assertThat(fresh).isPresent();
        assertThat(fresh.get().status()).isEqualTo("NOT_SUBMITTED");
    }
}
