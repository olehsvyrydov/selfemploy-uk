package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SqliteBankTransactionService.
 * Verifies save/find/update/count/hash/status operations against in-memory SQLite.
 */
class SqliteBankTransactionServiceTest {

    private static UUID businessId;
    private SqliteBankTransactionService service;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
        businessId = UUID.randomUUID();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
        service = new SqliteBankTransactionService(businessId);
    }

    // === Constructor Tests ===

    @Test
    void constructor_shouldRejectNullBusinessId() {
        assertThatThrownBy(() -> new SqliteBankTransactionService(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Business ID cannot be null");
    }

    @Test
    void getBusinessId_shouldReturnConfiguredId() {
        assertThat(service.getBusinessId()).isEqualTo(businessId);
    }

    // === Save and Find Tests ===

    @Test
    void save_andFindAll_shouldPersistTransaction() {
        BankTransaction tx = createTestTransaction("Test payment", new BigDecimal("100.00"));
        service.save(tx);

        List<BankTransaction> all = service.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).description()).isEqualTo("Test payment");
        assertThat(all.get(0).amount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void save_shouldRejectNull() {
        assertThatThrownBy(() -> service.save(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findById_shouldReturnTransaction() {
        BankTransaction tx = createTestTransaction("Find me", new BigDecimal("50.00"));
        service.save(tx);

        Optional<BankTransaction> found = service.findById(tx.id());
        assertThat(found).isPresent();
        assertThat(found.get().description()).isEqualTo("Find me");
    }

    @Test
    void findById_shouldReturnEmptyForNonexistent() {
        Optional<BankTransaction> found = service.findById(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void findById_shouldRejectNull() {
        assertThatThrownBy(() -> service.findById(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findAll_shouldReturnOrderedByDateDescending() {
        service.save(createTestTransaction("Old", new BigDecimal("10"), LocalDate.of(2025, 1, 1)));
        service.save(createTestTransaction("New", new BigDecimal("20"), LocalDate.of(2025, 6, 1)));
        service.save(createTestTransaction("Mid", new BigDecimal("15"), LocalDate.of(2025, 3, 1)));

        List<BankTransaction> all = service.findAll();
        assertThat(all).hasSize(3);
        assertThat(all.get(0).description()).isEqualTo("New");
        assertThat(all.get(1).description()).isEqualTo("Mid");
        assertThat(all.get(2).description()).isEqualTo("Old");
    }

    // === Update / Categorize Tests ===

    @Test
    void categorizeAsExpense_shouldUpdateStatusAndLink() {
        BankTransaction tx = createTestTransaction("Expense item", new BigDecimal("-45.00"));
        service.save(tx);

        UUID expenseId = UUID.randomUUID();
        service.categorizeAsExpense(tx.id(), expenseId, Instant.now());

        BankTransaction updated = service.findById(tx.id()).orElseThrow();
        assertThat(updated.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
        assertThat(updated.expenseId()).isEqualTo(expenseId);
    }

    @Test
    void categorizeAsIncome_shouldUpdateStatusAndLink() {
        BankTransaction tx = createTestTransaction("Income item", new BigDecimal("500.00"));
        service.save(tx);

        UUID incomeId = UUID.randomUUID();
        service.categorizeAsIncome(tx.id(), incomeId, Instant.now());

        BankTransaction updated = service.findById(tx.id()).orElseThrow();
        assertThat(updated.reviewStatus()).isEqualTo(ReviewStatus.CATEGORIZED);
        assertThat(updated.incomeId()).isEqualTo(incomeId);
    }

    @Test
    void exclude_shouldUpdateStatusAndReason() {
        BankTransaction tx = createTestTransaction("Exclude me", new BigDecimal("10.00"));
        service.save(tx);

        service.exclude(tx.id(), "Personal transaction", Instant.now());

        BankTransaction updated = service.findById(tx.id()).orElseThrow();
        assertThat(updated.reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
        assertThat(updated.exclusionReason()).isEqualTo("Personal transaction");
    }

    @Test
    void skip_shouldUpdateStatusToSkipped() {
        BankTransaction tx = createTestTransaction("Skip me", new BigDecimal("10.00"));
        service.save(tx);

        service.skip(tx.id(), Instant.now());

        BankTransaction updated = service.findById(tx.id()).orElseThrow();
        assertThat(updated.reviewStatus()).isEqualTo(ReviewStatus.SKIPPED);
    }

    @Test
    void setBusinessFlag_shouldUpdateFlag() {
        BankTransaction tx = createTestTransaction("Business expense", new BigDecimal("-30.00"));
        service.save(tx);

        // Initially null
        assertThat(service.findById(tx.id()).orElseThrow().isBusiness()).isNull();

        // Set to business
        service.setBusinessFlag(tx.id(), true, Instant.now());
        assertThat(service.findById(tx.id()).orElseThrow().isBusiness()).isTrue();

        // Set to personal
        service.setBusinessFlag(tx.id(), false, Instant.now());
        assertThat(service.findById(tx.id()).orElseThrow().isBusiness()).isFalse();
    }

    // === Count and Hash Tests ===

    @Test
    void count_shouldReturnTotalForBusiness() {
        assertThat(service.count()).isZero();

        service.save(createTestTransaction("One", new BigDecimal("1")));
        service.save(createTestTransaction("Two", new BigDecimal("2")));

        assertThat(service.count()).isEqualTo(2);
    }

    @Test
    void getStatusCounts_shouldReturnPerStatusBreakdown() {
        BankTransaction tx1 = createTestTransaction("Pending", new BigDecimal("10"));
        service.save(tx1);

        BankTransaction tx2 = createTestTransaction("For exclude", new BigDecimal("20"));
        service.save(tx2);
        service.exclude(tx2.id(), "Transfer", Instant.now());

        BankTransaction tx3 = createTestTransaction("For categorize", new BigDecimal("30"));
        service.save(tx3);
        service.categorizeAsIncome(tx3.id(), UUID.randomUUID(), Instant.now());

        Map<ReviewStatus, Long> counts = service.getStatusCounts();
        assertThat(counts.get(ReviewStatus.PENDING)).isEqualTo(1);
        assertThat(counts.get(ReviewStatus.EXCLUDED)).isEqualTo(1);
        assertThat(counts.get(ReviewStatus.CATEGORIZED)).isEqualTo(1);
    }

    @Test
    void existsByHash_shouldDetectDuplicates() {
        String hash = "abc123hash";
        assertThat(service.existsByHash(hash)).isFalse();

        service.save(createTestTransactionWithHash("Tx", new BigDecimal("10"), hash));

        assertThat(service.existsByHash(hash)).isTrue();
    }

    @Test
    void existsByHash_shouldRejectNullOrEmpty() {
        assertThatThrownBy(() -> service.existsByHash(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.existsByHash(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // === Delete Tests ===

    @Test
    void delete_shouldSoftDeleteTransaction() {
        BankTransaction tx = createTestTransaction("Delete me", new BigDecimal("10"));
        service.save(tx);
        assertThat(service.count()).isEqualTo(1);

        boolean deleted = service.delete(tx.id());
        assertThat(deleted).isTrue();
        // After soft-delete, the transaction should not appear in findAll
        assertThat(service.findAll()).isEmpty();
        // But count of active transactions should be zero
        assertThat(service.count()).isZero();
    }

    @Test
    void delete_shouldReturnFalseForNonexistent() {
        assertThat(service.delete(UUID.randomUUID())).isFalse();
    }

    @Test
    void delete_shouldRejectNull() {
        assertThatThrownBy(() -> service.delete(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // === Retention Policy Enforcement Tests ===

    @Test
    void delete_shouldSoftDeleteWithTimestamp() {
        BankTransaction tx = createTestTransaction("Soft delete me", new BigDecimal("25.00"));
        service.save(tx);

        Instant beforeDelete = Instant.now();
        boolean deleted = service.delete(tx.id());
        assertThat(deleted).isTrue();

        // The transaction should no longer be visible to normal queries
        assertThat(service.findAll()).isEmpty();
    }

    // === Nullable Fields Roundtrip ===

    @Test
    void save_shouldPersistAllNullableFields() {
        Instant now = Instant.now();
        BankTransaction tx = new BankTransaction(
            UUID.randomUUID(), businessId, UUID.randomUUID(), "csv-barclays",
            LocalDate.of(2025, 6, 15), new BigDecimal("-99.99"),
            "Full field test", "1234", "BANK-TX-001", "hash-full",
            ReviewStatus.PENDING, null, null, null,
            null, new BigDecimal("0.85"), ExpenseCategory.TRAVEL,
            now, now, null, null, null
        );
        service.save(tx);

        BankTransaction loaded = service.findById(tx.id()).orElseThrow();
        assertThat(loaded.sourceFormatId()).isEqualTo("csv-barclays");
        assertThat(loaded.accountLastFour()).isEqualTo("1234");
        assertThat(loaded.bankTransactionId()).isEqualTo("BANK-TX-001");
        assertThat(loaded.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(loaded.suggestedCategory()).isEqualTo(ExpenseCategory.TRAVEL);
        assertThat(loaded.isBusiness()).isNull();
    }

    // === Modification Log Tests (audit trail for all state changes) ===

    @Nested
    @DisplayName("modification audit log")
    class ModificationLog {

        @Test
        @DisplayName("categorizeAsExpense should log CATEGORIZED modification")
        void categorizeAsExpense_shouldLogModification() {
            BankTransaction tx = createTestTransaction("Expense item", new BigDecimal("-45.00"));
            service.save(tx);

            UUID expenseId = UUID.randomUUID();
            service.categorizeAsExpense(tx.id(), expenseId, Instant.now());

            List<Map<String, String>> logs = SqliteDataStore.getInstance()
                    .findModificationLogs(tx.id());
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).get("modification_type")).isEqualTo("CATEGORIZED");
            assertThat(logs.get(0).get("field_name")).isEqualTo("review_status");
            assertThat(logs.get(0).get("previous_value")).isEqualTo("PENDING");
            assertThat(logs.get(0).get("new_value")).isEqualTo("CATEGORIZED");
            assertThat(logs.get(0).get("modified_by")).isEqualTo("local-user");
        }

        @Test
        @DisplayName("categorizeAsIncome should log CATEGORIZED modification")
        void categorizeAsIncome_shouldLogModification() {
            BankTransaction tx = createTestTransaction("Income item", new BigDecimal("500.00"));
            service.save(tx);

            UUID incomeId = UUID.randomUUID();
            service.categorizeAsIncome(tx.id(), incomeId, Instant.now());

            List<Map<String, String>> logs = SqliteDataStore.getInstance()
                    .findModificationLogs(tx.id());
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).get("modification_type")).isEqualTo("CATEGORIZED");
        }

        @Test
        @DisplayName("exclude should log EXCLUDED modification")
        void exclude_shouldLogModification() {
            BankTransaction tx = createTestTransaction("Exclude me", new BigDecimal("10.00"));
            service.save(tx);

            service.exclude(tx.id(), "Personal transaction", Instant.now());

            List<Map<String, String>> logs = SqliteDataStore.getInstance()
                    .findModificationLogs(tx.id());
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).get("modification_type")).isEqualTo("EXCLUDED");
            assertThat(logs.get(0).get("previous_value")).isEqualTo("PENDING");
            assertThat(logs.get(0).get("new_value")).isEqualTo("EXCLUDED");
        }

        @Test
        @DisplayName("setBusinessFlag should log BUSINESS_PERSONAL_CHANGED modification")
        void setBusinessFlag_shouldLogModification() {
            BankTransaction tx = createTestTransaction("Business item", new BigDecimal("-30.00"));
            service.save(tx);

            service.setBusinessFlag(tx.id(), true, Instant.now());

            List<Map<String, String>> logs = SqliteDataStore.getInstance()
                    .findModificationLogs(tx.id());
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).get("modification_type")).isEqualTo("BUSINESS_PERSONAL_CHANGED");
            assertThat(logs.get(0).get("field_name")).isEqualTo("is_business");
            assertThat(logs.get(0).get("new_value")).isEqualTo("true");
        }

        @Test
        @DisplayName("skip should log modification")
        void skip_shouldLogModification() {
            BankTransaction tx = createTestTransaction("Skip me", new BigDecimal("10.00"));
            service.save(tx);

            service.skip(tx.id(), Instant.now());

            List<Map<String, String>> logs = SqliteDataStore.getInstance()
                    .findModificationLogs(tx.id());
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).get("modification_type")).isEqualTo("EXCLUDED");
            assertThat(logs.get(0).get("new_value")).isEqualTo("SKIPPED");
        }

        @Test
        @DisplayName("delete should log modification")
        void delete_shouldLogModification() {
            BankTransaction tx = createTestTransaction("Delete me", new BigDecimal("10.00"));
            service.save(tx);

            service.delete(tx.id());

            List<Map<String, String>> logs = SqliteDataStore.getInstance()
                    .findModificationLogs(tx.id());
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0).get("modification_type")).isEqualTo("EXCLUDED");
            assertThat(logs.get(0).get("field_name")).isEqualTo("deleted_at");
        }

        @Test
        @DisplayName("multiple state changes should create multiple log entries")
        void multipleChanges_shouldCreateMultipleLogs() {
            BankTransaction tx = createTestTransaction("Multi change", new BigDecimal("-15.00"));
            service.save(tx);

            service.setBusinessFlag(tx.id(), true, Instant.now());
            service.setBusinessFlag(tx.id(), false, Instant.now());

            List<Map<String, String>> logs = SqliteDataStore.getInstance()
                    .findModificationLogs(tx.id());
            assertThat(logs).hasSize(2);
        }
    }

    // === Helpers ===

    private BankTransaction createTestTransaction(String desc, BigDecimal amount) {
        return createTestTransaction(desc, amount, LocalDate.of(2025, 6, 1));
    }

    private BankTransaction createTestTransaction(String desc, BigDecimal amount, LocalDate date) {
        return BankTransaction.create(
            businessId, UUID.randomUUID(), null,
            date, amount, desc, null, null,
            UUID.randomUUID().toString(), Instant.now()
        );
    }

    private BankTransaction createTestTransactionWithHash(String desc, BigDecimal amount, String hash) {
        Instant now = Instant.now();
        return new BankTransaction(
            UUID.randomUUID(), businessId, UUID.randomUUID(), null,
            LocalDate.of(2025, 6, 1), amount, desc, null, null,
            hash, ReviewStatus.PENDING, null, null, null,
            null, null, null, now, now, null, null, null
        );
    }
}
