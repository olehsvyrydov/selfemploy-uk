package uk.selfemploy.ui.service;

import org.junit.jupiter.api.*;
import uk.selfemploy.core.reconciliation.MatchTier;
import uk.selfemploy.core.reconciliation.ReconciliationMatch;
import uk.selfemploy.core.reconciliation.ReconciliationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence tests for {@link SqliteReconciliationMatchRepository}.
 * Uses in-memory SQLite via SqliteTestSupport.
 */
@DisplayName("SqliteReconciliationMatchRepository Persistence")
class SqliteReconciliationTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID BANK_TX_ID = UUID.randomUUID();
    private static final UUID MANUAL_TX_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    private SqliteReconciliationMatchRepository repository;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
        SqliteDataStore.getInstance().ensureBusinessExists(BUSINESS_ID);
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        SqliteDataStore.getInstance().ensureBusinessExists(BUSINESS_ID);
        repository = new SqliteReconciliationMatchRepository();
    }

    // === Save and Find by ID ===

    @Test
    void saveAndFindById() {
        ReconciliationMatch match = ReconciliationMatch.create(
            BANK_TX_ID, MANUAL_TX_ID, "INCOME",
            1.0, MatchTier.EXACT, BUSINESS_ID, NOW);

        repository.save(match);

        Optional<ReconciliationMatch> found = repository.findById(match.id());
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(match.id());
        assertThat(found.get().bankTransactionId()).isEqualTo(BANK_TX_ID);
        assertThat(found.get().manualTransactionId()).isEqualTo(MANUAL_TX_ID);
        assertThat(found.get().manualTransactionType()).isEqualTo("INCOME");
        assertThat(found.get().confidence()).isEqualTo(1.0);
        assertThat(found.get().matchTier()).isEqualTo(MatchTier.EXACT);
        assertThat(found.get().status()).isEqualTo(ReconciliationStatus.UNRESOLVED);
        assertThat(found.get().businessId()).isEqualTo(BUSINESS_ID);
        assertThat(found.get().createdAt()).isEqualTo(NOW);
        assertThat(found.get().resolvedAt()).isNull();
        assertThat(found.get().resolvedBy()).isNull();
    }

    @Test
    void findByIdReturnsEmptyForNonexistent() {
        Optional<ReconciliationMatch> found = repository.findById(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    // === Save Multiple ===

    @Test
    void saveMultipleMatches() {
        ReconciliationMatch match1 = ReconciliationMatch.create(
            BANK_TX_ID, UUID.randomUUID(), "INCOME",
            1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
        ReconciliationMatch match2 = ReconciliationMatch.create(
            BANK_TX_ID, UUID.randomUUID(), "INCOME",
            0.85, MatchTier.LIKELY, BUSINESS_ID, NOW);

        repository.saveAll(List.of(match1, match2));

        List<ReconciliationMatch> found = repository.findByBankTransactionId(BANK_TX_ID);
        assertThat(found).hasSize(2);
    }

    @Test
    void saveEmptyListDoesNothing() {
        repository.saveAll(List.of());
        List<ReconciliationMatch> found = repository.findByBusinessId(BUSINESS_ID);
        assertThat(found).isEmpty();
    }

    @Test
    void saveNullListDoesNothing() {
        repository.saveAll(null);
        List<ReconciliationMatch> found = repository.findByBusinessId(BUSINESS_ID);
        assertThat(found).isEmpty();
    }

    // === Find by Bank Transaction ID ===

    @Test
    void findByBankTransactionId() {
        UUID bankTx1 = UUID.randomUUID();
        UUID bankTx2 = UUID.randomUUID();

        ReconciliationMatch match1 = ReconciliationMatch.create(
            bankTx1, UUID.randomUUID(), "INCOME",
            1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
        ReconciliationMatch match2 = ReconciliationMatch.create(
            bankTx1, UUID.randomUUID(), "INCOME",
            0.85, MatchTier.LIKELY, BUSINESS_ID, NOW);
        ReconciliationMatch match3 = ReconciliationMatch.create(
            bankTx2, UUID.randomUUID(), "EXPENSE",
            0.30, MatchTier.POSSIBLE, BUSINESS_ID, NOW);

        repository.saveAll(List.of(match1, match2, match3));

        List<ReconciliationMatch> forBankTx1 = repository.findByBankTransactionId(bankTx1);
        assertThat(forBankTx1).hasSize(2);
        // Ordered by confidence DESC
        assertThat(forBankTx1.get(0).confidence()).isGreaterThanOrEqualTo(forBankTx1.get(1).confidence());
    }

    @Test
    void findByBankTransactionIdReturnsEmptyWhenNone() {
        List<ReconciliationMatch> found = repository.findByBankTransactionId(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    // === Find by Business ID ===

    @Test
    void findByBusinessId() {
        UUID otherBusiness = UUID.randomUUID();
        SqliteDataStore.getInstance().ensureBusinessExists(otherBusiness);

        ReconciliationMatch match1 = ReconciliationMatch.create(
            UUID.randomUUID(), UUID.randomUUID(), "INCOME",
            1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
        ReconciliationMatch match2 = ReconciliationMatch.create(
            UUID.randomUUID(), UUID.randomUUID(), "EXPENSE",
            0.30, MatchTier.POSSIBLE, otherBusiness, NOW);

        repository.saveAll(List.of(match1, match2));

        List<ReconciliationMatch> forBusiness = repository.findByBusinessId(BUSINESS_ID);
        assertThat(forBusiness).hasSize(1);
        assertThat(forBusiness.get(0).businessId()).isEqualTo(BUSINESS_ID);
    }

    // === Find Unresolved ===

    @Test
    void findUnresolvedOnlyReturnsUnresolved() {
        ReconciliationMatch unresolved = ReconciliationMatch.create(
            UUID.randomUUID(), UUID.randomUUID(), "INCOME",
            1.0, MatchTier.EXACT, BUSINESS_ID, NOW);

        ReconciliationMatch confirmed = ReconciliationMatch.create(
            UUID.randomUUID(), UUID.randomUUID(), "EXPENSE",
            0.85, MatchTier.LIKELY, BUSINESS_ID, NOW)
            .withConfirmed(Instant.parse("2025-06-16T10:00:00Z"), "local-user");

        ReconciliationMatch dismissed = ReconciliationMatch.create(
            UUID.randomUUID(), UUID.randomUUID(), "INCOME",
            0.30, MatchTier.POSSIBLE, BUSINESS_ID, NOW)
            .withDismissed(Instant.parse("2025-06-16T11:00:00Z"), "local-user");

        repository.save(unresolved);
        repository.save(confirmed);
        repository.save(dismissed);

        List<ReconciliationMatch> unresolvedList = repository.findUnresolvedByBusinessId(BUSINESS_ID);
        assertThat(unresolvedList).hasSize(1);
        assertThat(unresolvedList.get(0).id()).isEqualTo(unresolved.id());
    }

    // === Count Unresolved ===

    @Test
    void countUnresolved() {
        ReconciliationMatch m1 = ReconciliationMatch.create(
            UUID.randomUUID(), UUID.randomUUID(), "INCOME",
            1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
        ReconciliationMatch m2 = ReconciliationMatch.create(
            UUID.randomUUID(), UUID.randomUUID(), "INCOME",
            0.85, MatchTier.LIKELY, BUSINESS_ID, NOW);
        ReconciliationMatch m3 = ReconciliationMatch.create(
            UUID.randomUUID(), UUID.randomUUID(), "EXPENSE",
            0.30, MatchTier.POSSIBLE, BUSINESS_ID, NOW)
            .withConfirmed(Instant.parse("2025-06-16T10:00:00Z"), "local-user");

        repository.saveAll(List.of(m1, m2, m3));

        long count = repository.countUnresolvedByBusinessId(BUSINESS_ID);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void countUnresolvedReturnsZeroWhenNone() {
        long count = repository.countUnresolvedByBusinessId(BUSINESS_ID);
        assertThat(count).isEqualTo(0);
    }

    // === Update Status ===

    @Test
    void updateStatusToConfirmed() {
        ReconciliationMatch match = ReconciliationMatch.create(
            BANK_TX_ID, MANUAL_TX_ID, "INCOME",
            1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
        repository.save(match);

        Instant resolvedAt = Instant.parse("2025-06-16T10:00:00Z");
        boolean updated = repository.updateStatus(
            match.id(), ReconciliationStatus.CONFIRMED, resolvedAt, "local-user");

        assertThat(updated).isTrue();

        Optional<ReconciliationMatch> found = repository.findById(match.id());
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(ReconciliationStatus.CONFIRMED);
        assertThat(found.get().resolvedAt()).isEqualTo(resolvedAt);
        assertThat(found.get().resolvedBy()).isEqualTo("local-user");
    }

    @Test
    void updateStatusToDismissed() {
        ReconciliationMatch match = ReconciliationMatch.create(
            BANK_TX_ID, MANUAL_TX_ID, "EXPENSE",
            0.85, MatchTier.LIKELY, BUSINESS_ID, NOW);
        repository.save(match);

        Instant resolvedAt = Instant.parse("2025-06-16T11:00:00Z");
        boolean updated = repository.updateStatus(
            match.id(), ReconciliationStatus.DISMISSED, resolvedAt, "local-user");

        assertThat(updated).isTrue();

        Optional<ReconciliationMatch> found = repository.findById(match.id());
        assertThat(found).isPresent();
        assertThat(found.get().status()).isEqualTo(ReconciliationStatus.DISMISSED);
    }

    @Test
    void updateStatusReturnsFalseForNonexistent() {
        boolean updated = repository.updateStatus(
            UUID.randomUUID(), ReconciliationStatus.CONFIRMED, NOW, "local-user");
        assertThat(updated).isFalse();
    }

    // === Unique Constraint ===

    @Test
    void duplicateMatchIsReplacedOnSave() {
        ReconciliationMatch original = ReconciliationMatch.create(
            BANK_TX_ID, MANUAL_TX_ID, "INCOME",
            0.85, MatchTier.LIKELY, BUSINESS_ID, NOW);
        repository.save(original);

        // Save again with same bank_tx + manual_tx + type but different ID
        // INSERT OR REPLACE should replace based on unique constraint
        ReconciliationMatch replacement = new ReconciliationMatch(
            original.id(), BANK_TX_ID, MANUAL_TX_ID, "INCOME",
            1.0, MatchTier.EXACT, ReconciliationStatus.UNRESOLVED,
            BUSINESS_ID, NOW, null, null);
        repository.save(replacement);

        Optional<ReconciliationMatch> found = repository.findById(original.id());
        assertThat(found).isPresent();
        assertThat(found.get().confidence()).isEqualTo(1.0);
        assertThat(found.get().matchTier()).isEqualTo(MatchTier.EXACT);
    }

    // === All Match Tiers Persist ===

    @Test
    void allMatchTiersArePersisted() {
        for (MatchTier tier : MatchTier.values()) {
            ReconciliationMatch match = ReconciliationMatch.create(
                UUID.randomUUID(), UUID.randomUUID(), "INCOME",
                tier.getMinimumConfidence(), tier, BUSINESS_ID, NOW);
            repository.save(match);

            Optional<ReconciliationMatch> found = repository.findById(match.id());
            assertThat(found).isPresent()
                .describedAs("Match tier %s should be persistable", tier);
            assertThat(found.get().matchTier()).isEqualTo(tier);
        }
    }

    // === Expense Match Persistence ===

    @Test
    void expenseMatchIsPersisted() {
        ReconciliationMatch match = ReconciliationMatch.create(
            BANK_TX_ID, MANUAL_TX_ID, "EXPENSE",
            0.30, MatchTier.POSSIBLE, BUSINESS_ID, NOW);
        repository.save(match);

        Optional<ReconciliationMatch> found = repository.findById(match.id());
        assertThat(found).isPresent();
        assertThat(found.get().manualTransactionType()).isEqualTo("EXPENSE");
    }

    // === Ordering ===

    @Test
    void findByBankTxOrderedByConfidenceDesc() {
        UUID bankTxId = UUID.randomUUID();

        ReconciliationMatch low = ReconciliationMatch.create(
            bankTxId, UUID.randomUUID(), "INCOME",
            0.30, MatchTier.POSSIBLE, BUSINESS_ID, NOW);
        ReconciliationMatch high = ReconciliationMatch.create(
            bankTxId, UUID.randomUUID(), "INCOME",
            1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
        ReconciliationMatch mid = ReconciliationMatch.create(
            bankTxId, UUID.randomUUID(), "INCOME",
            0.85, MatchTier.LIKELY, BUSINESS_ID, NOW);

        // Save in random order
        repository.saveAll(List.of(low, high, mid));

        List<ReconciliationMatch> found = repository.findByBankTransactionId(bankTxId);
        assertThat(found).hasSize(3);
        assertThat(found.get(0).confidence()).isEqualTo(1.0);
        assertThat(found.get(1).confidence()).isEqualTo(0.85);
        assertThat(found.get(2).confidence()).isEqualTo(0.30);
    }
}
