package uk.selfemploy.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("BankTransactionRepository")
class BankTransactionRepositoryTest {

    @Inject
    BankTransactionRepository bankTransactionRepository;

    @Inject
    ImportAuditRepository importAuditRepository;

    @Inject
    BusinessRepository businessRepository;

    @Inject
    IncomeRepository incomeRepository;

    private UUID businessId;
    private UUID importAuditId;
    private UUID incomeId;

    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    @BeforeEach
    @Transactional
    void setUp() {
        bankTransactionRepository.deleteAll();
        incomeRepository.deleteAll();
        importAuditRepository.deleteAll();
        businessRepository.deleteAll();

        Business business = businessRepository.save(Business.create(
            "Test Business", "1234567890",
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5),
            BusinessType.SELF_EMPLOYED, null
        ));
        businessId = business.id();

        ImportAudit audit = importAuditRepository.save(ImportAudit.create(
            businessId, NOW, "barclays-2025.csv", "abc123hash",
            ImportAuditType.BANK_CSV, 10, 8, 2, List.of()
        ));
        importAuditId = audit.id();

        // Create a real income record for FK references in categorization tests
        Income income = incomeRepository.save(Income.create(
            businessId, LocalDate.of(2025, 6, 15),
            new BigDecimal("150.00"), "Test income", IncomeCategory.SALES, null
        ));
        incomeId = income.id();
    }

    private BankTransaction createTransaction(String desc, BigDecimal amount, String hash) {
        return BankTransaction.create(
            businessId, importAuditId, "csv-barclays",
            LocalDate.of(2025, 6, 15), amount, desc,
            "1234", "TXN-001", hash, NOW
        );
    }

    // --- Save ---

    @Test
    @Transactional
    @DisplayName("save: persists and retrieves a bank transaction")
    void savePersistsAndRetrieves() {
        BankTransaction tx = createTransaction("ACME PAYMENT", new BigDecimal("150.00"), "hash1");

        BankTransaction saved = bankTransactionRepository.save(tx);

        assertThat(saved.id()).isEqualTo(tx.id());
        assertThat(saved.businessId()).isEqualTo(businessId);
        assertThat(saved.importAuditId()).isEqualTo(importAuditId);
        assertThat(saved.sourceFormatId()).isEqualTo("csv-barclays");
        assertThat(saved.date()).isEqualTo(LocalDate.of(2025, 6, 15));
        assertThat(saved.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(saved.description()).isEqualTo("ACME PAYMENT");
        assertThat(saved.transactionHash()).isEqualTo("hash1");
        assertThat(saved.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    @Transactional
    @DisplayName("saveAll: saves batch of transactions")
    void saveAllSavesBatch() {
        List<BankTransaction> batch = List.of(
            createTransaction("TX1", new BigDecimal("10.00"), "hash-a"),
            createTransaction("TX2", new BigDecimal("20.00"), "hash-b"),
            createTransaction("TX3", new BigDecimal("30.00"), "hash-c")
        );

        List<BankTransaction> saved = bankTransactionRepository.saveAll(batch);

        assertThat(saved).hasSize(3);
    }

    // --- findByIdActive ---

    @Test
    @Transactional
    @DisplayName("findByIdActive: finds active transaction by ID")
    void findByIdActiveFindsActive() {
        BankTransaction tx = bankTransactionRepository.save(
            createTransaction("PAYMENT", new BigDecimal("50.00"), "hash-find")
        );

        Optional<BankTransaction> found = bankTransactionRepository.findByIdActive(tx.id());

        assertThat(found).isPresent();
        assertThat(found.get().description()).isEqualTo("PAYMENT");
    }

    @Test
    @Transactional
    @DisplayName("findByIdActive: excludes soft-deleted transactions")
    void findByIdActiveExcludesSoftDeleted() {
        BankTransaction tx = createTransaction("DELETED", new BigDecimal("50.00"), "hash-del");
        BankTransaction deleted = tx.withSoftDelete(NOW, "system", "test");
        bankTransactionRepository.save(deleted);

        Optional<BankTransaction> found = bankTransactionRepository.findByIdActive(deleted.id());

        assertThat(found).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("findByIdActive: returns empty for non-existent ID")
    void findByIdActiveReturnsEmptyForMissing() {
        Optional<BankTransaction> found = bankTransactionRepository.findByIdActive(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // --- findByBusinessId ---

    @Test
    @Transactional
    @DisplayName("findByBusinessId: finds all active transactions")
    void findByBusinessIdFindsAllActive() {
        bankTransactionRepository.save(createTransaction("TX1", new BigDecimal("10.00"), "h1"));
        bankTransactionRepository.save(createTransaction("TX2", new BigDecimal("20.00"), "h2"));

        List<BankTransaction> found = bankTransactionRepository.findByBusinessId(businessId);

        assertThat(found).hasSize(2);
    }

    @Test
    @Transactional
    @DisplayName("findByBusinessId: excludes soft-deleted transactions")
    void findByBusinessIdExcludesDeleted() {
        bankTransactionRepository.save(createTransaction("ACTIVE", new BigDecimal("10.00"), "h1"));
        BankTransaction deleted = createTransaction("DELETED", new BigDecimal("20.00"), "h2")
            .withSoftDelete(NOW, "system", "test");
        bankTransactionRepository.save(deleted);

        List<BankTransaction> found = bankTransactionRepository.findByBusinessId(businessId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).description()).isEqualTo("ACTIVE");
    }

    @Test
    @Transactional
    @DisplayName("findByBusinessId: returns empty for business with no transactions")
    void findByBusinessIdReturnsEmptyForNone() {
        List<BankTransaction> found = bankTransactionRepository.findByBusinessId(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // --- findByImportAuditId ---

    @Test
    @Transactional
    @DisplayName("findByImportAuditId: finds transactions for a specific import")
    void findByImportAuditIdFindsByAudit() {
        bankTransactionRepository.save(createTransaction("TX1", new BigDecimal("10.00"), "h1"));
        bankTransactionRepository.save(createTransaction("TX2", new BigDecimal("20.00"), "h2"));

        List<BankTransaction> found = bankTransactionRepository.findByImportAuditId(importAuditId);

        assertThat(found).hasSize(2);
    }

    // --- findByReviewStatus ---

    @Test
    @Transactional
    @DisplayName("findPending: finds only pending transactions")
    void findPendingFindsOnlyPending() {
        bankTransactionRepository.save(createTransaction("PENDING", new BigDecimal("10.00"), "h1"));

        BankTransaction categorized = createTransaction("DONE", new BigDecimal("20.00"), "h2")
            .withCategorizedAsIncome(incomeId, NOW);
        bankTransactionRepository.save(categorized);

        List<BankTransaction> pending = bankTransactionRepository.findPending(businessId);

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).description()).isEqualTo("PENDING");
    }

    @Test
    @Transactional
    @DisplayName("findByReviewStatus: finds categorized transactions")
    void findByReviewStatusFindsCategorized() {
        bankTransactionRepository.save(createTransaction("PENDING", new BigDecimal("10.00"), "h1"));

        BankTransaction categorized = createTransaction("DONE", new BigDecimal("20.00"), "h2")
            .withCategorizedAsIncome(incomeId, NOW);
        bankTransactionRepository.save(categorized);

        List<BankTransaction> result = bankTransactionRepository
            .findByReviewStatus(businessId, ReviewStatus.CATEGORIZED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).description()).isEqualTo("DONE");
    }

    // --- existsByHash ---

    @Test
    @Transactional
    @DisplayName("existsByHash: returns true for existing hash")
    void existsByHashTrueForExisting() {
        bankTransactionRepository.save(createTransaction("TX", new BigDecimal("10.00"), "unique-hash"));

        assertThat(bankTransactionRepository.existsByHash(businessId, "unique-hash")).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("existsByHash: returns false for non-existing hash")
    void existsByHashFalseForMissing() {
        assertThat(bankTransactionRepository.existsByHash(businessId, "no-such-hash")).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("existsByHash: ignores soft-deleted transactions")
    void existsByHashIgnoresDeleted() {
        BankTransaction deleted = createTransaction("TX", new BigDecimal("10.00"), "del-hash")
            .withSoftDelete(NOW, "system", "test");
        bankTransactionRepository.save(deleted);

        assertThat(bankTransactionRepository.existsByHash(businessId, "del-hash")).isFalse();
    }

    // --- Counting ---

    @Test
    @Transactional
    @DisplayName("countActive: counts only non-deleted transactions")
    void countActiveCountsNonDeleted() {
        bankTransactionRepository.save(createTransaction("TX1", new BigDecimal("10.00"), "h1"));
        bankTransactionRepository.save(createTransaction("TX2", new BigDecimal("20.00"), "h2"));
        BankTransaction deleted = createTransaction("DEL", new BigDecimal("30.00"), "h3")
            .withSoftDelete(NOW, "system", "test");
        bankTransactionRepository.save(deleted);

        assertThat(bankTransactionRepository.countActive(businessId)).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("countByStatus: counts transactions per review status")
    void countByStatusCountsPerStatus() {
        bankTransactionRepository.save(createTransaction("TX1", new BigDecimal("10.00"), "h1"));
        BankTransaction categorized = createTransaction("TX2", new BigDecimal("20.00"), "h2")
            .withCategorizedAsIncome(incomeId, NOW);
        bankTransactionRepository.save(categorized);

        assertThat(bankTransactionRepository.countByStatus(businessId, ReviewStatus.PENDING)).isEqualTo(1);
        assertThat(bankTransactionRepository.countByStatus(businessId, ReviewStatus.CATEGORIZED)).isEqualTo(1);
    }

    // --- Update ---

    @Test
    @Transactional
    @DisplayName("update: updates review status")
    void updateUpdatesStatus() {
        BankTransaction tx = bankTransactionRepository.save(
            createTransaction("TX", new BigDecimal("10.00"), "h-upd")
        );

        BankTransaction excluded = tx.withExcluded("TRANSFER", NOW.plusSeconds(60));
        BankTransaction updated = bankTransactionRepository.update(excluded);

        assertThat(updated.reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
        assertThat(updated.exclusionReason()).isEqualTo("TRANSFER");
    }

    @Test
    @Transactional
    @DisplayName("update: updates business flag")
    void updateUpdatesBusinessFlag() {
        BankTransaction tx = bankTransactionRepository.save(
            createTransaction("TX", new BigDecimal("10.00"), "h-biz")
        );

        BankTransaction flagged = tx.withBusinessFlag(true, NOW.plusSeconds(60));
        BankTransaction updated = bankTransactionRepository.update(flagged);

        assertThat(updated.isBusiness()).isTrue();
    }

    // --- Soft delete by import ---

    @Test
    @Transactional
    @DisplayName("softDeleteByImportAuditId: soft-deletes all transactions for an import")
    void softDeleteByImportAuditIdDeletesAll() {
        bankTransactionRepository.save(createTransaction("TX1", new BigDecimal("10.00"), "h1"));
        bankTransactionRepository.save(createTransaction("TX2", new BigDecimal("20.00"), "h2"));

        int deleted = bankTransactionRepository.softDeleteByImportAuditId(
            importAuditId, NOW.plusSeconds(60), "system", "undo import"
        );

        assertThat(deleted).isEqualTo(2);
        assertThat(bankTransactionRepository.findByImportAuditId(importAuditId)).isEmpty();
    }
}
