package uk.selfemploy.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.domain.TransactionModificationLog;
import uk.selfemploy.common.enums.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("TransactionModificationLogRepository")
class TransactionModificationLogRepositoryTest {

    @Inject
    TransactionModificationLogRepository modLogRepository;

    @Inject
    BankTransactionRepository bankTransactionRepository;

    @Inject
    ImportAuditRepository importAuditRepository;

    @Inject
    BusinessRepository businessRepository;

    private UUID bankTransactionId;

    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    @BeforeEach
    @Transactional
    void setUp() {
        modLogRepository.deleteAll();
        bankTransactionRepository.deleteAll();
        importAuditRepository.deleteAll();
        businessRepository.deleteAll();

        Business business = businessRepository.save(Business.create(
            "Test Business", "1234567890",
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5),
            BusinessType.SELF_EMPLOYED, null
        ));

        ImportAudit audit = importAuditRepository.save(ImportAudit.create(
            business.id(), NOW, "test.csv", "hash123",
            ImportAuditType.BANK_CSV, 5, 5, 0, List.of()
        ));

        BankTransaction tx = bankTransactionRepository.save(BankTransaction.create(
            business.id(), audit.id(), "csv-barclays",
            LocalDate.of(2025, 6, 15), new BigDecimal("100.00"), "TEST PAYMENT",
            "1234", "TXN-001", "txhash123", NOW
        ));
        bankTransactionId = tx.id();
    }

    // --- Save ---

    @Test
    @Transactional
    @DisplayName("save: persists and retrieves a modification log entry")
    void savePersistsAndRetrieves() {
        TransactionModificationLog log = TransactionModificationLog.create(
            bankTransactionId, ModificationType.CATEGORIZED,
            "reviewStatus", "PENDING", "CATEGORIZED", "user@test.com", NOW
        );

        TransactionModificationLog saved = modLogRepository.save(log);

        assertThat(saved.id()).isEqualTo(log.id());
        assertThat(saved.bankTransactionId()).isEqualTo(bankTransactionId);
        assertThat(saved.modificationType()).isEqualTo(ModificationType.CATEGORIZED);
        assertThat(saved.fieldName()).isEqualTo("reviewStatus");
        assertThat(saved.previousValue()).isEqualTo("PENDING");
        assertThat(saved.newValue()).isEqualTo("CATEGORIZED");
        assertThat(saved.modifiedBy()).isEqualTo("user@test.com");
        assertThat(saved.modifiedAt()).isEqualTo(NOW);
    }

    @Test
    @Transactional
    @DisplayName("save: persists entry with null optional fields")
    void savePersistsWithNullOptionalFields() {
        TransactionModificationLog log = TransactionModificationLog.create(
            bankTransactionId, ModificationType.EXCLUDED,
            null, null, null, "system", NOW
        );

        TransactionModificationLog saved = modLogRepository.save(log);

        assertThat(saved.fieldName()).isNull();
        assertThat(saved.previousValue()).isNull();
        assertThat(saved.newValue()).isNull();
    }

    // --- findByBankTransactionId ---

    @Test
    @Transactional
    @DisplayName("findByBankTransactionId: finds all entries in chronological order")
    void findByBankTransactionIdFindsAllChronological() {
        modLogRepository.save(TransactionModificationLog.create(
            bankTransactionId, ModificationType.CATEGORIZED,
            "reviewStatus", "PENDING", "CATEGORIZED", "user", NOW
        ));
        modLogRepository.save(TransactionModificationLog.create(
            bankTransactionId, ModificationType.RECATEGORIZED,
            "expenseCategory", "TRAVEL", "OFFICE", "user", NOW.plusSeconds(60)
        ));
        modLogRepository.save(TransactionModificationLog.create(
            bankTransactionId, ModificationType.BUSINESS_PERSONAL_CHANGED,
            "isBusiness", "false", "true", "user", NOW.plusSeconds(120)
        ));

        List<TransactionModificationLog> logs = modLogRepository.findByBankTransactionId(bankTransactionId);

        assertThat(logs).hasSize(3);
        assertThat(logs.get(0).modificationType()).isEqualTo(ModificationType.CATEGORIZED);
        assertThat(logs.get(1).modificationType()).isEqualTo(ModificationType.RECATEGORIZED);
        assertThat(logs.get(2).modificationType()).isEqualTo(ModificationType.BUSINESS_PERSONAL_CHANGED);
    }

    @Test
    @Transactional
    @DisplayName("findByBankTransactionId: returns empty for transaction with no modifications")
    void findByBankTransactionIdReturnsEmptyForNone() {
        List<TransactionModificationLog> logs = modLogRepository.findByBankTransactionId(UUID.randomUUID());

        assertThat(logs).isEmpty();
    }

    // --- findByBankTransactionIdAndType ---

    @Test
    @Transactional
    @DisplayName("findByBankTransactionIdAndType: filters by modification type")
    void findByBankTransactionIdAndTypeFilters() {
        modLogRepository.save(TransactionModificationLog.create(
            bankTransactionId, ModificationType.CATEGORIZED,
            "reviewStatus", "PENDING", "CATEGORIZED", "user", NOW
        ));
        modLogRepository.save(TransactionModificationLog.create(
            bankTransactionId, ModificationType.BUSINESS_PERSONAL_CHANGED,
            "isBusiness", null, "true", "user", NOW.plusSeconds(60)
        ));
        modLogRepository.save(TransactionModificationLog.create(
            bankTransactionId, ModificationType.CATEGORIZED,
            "incomeId", null, UUID.randomUUID().toString(), "user", NOW.plusSeconds(120)
        ));

        List<TransactionModificationLog> categorized = modLogRepository
            .findByBankTransactionIdAndType(bankTransactionId, ModificationType.CATEGORIZED);

        assertThat(categorized).hasSize(2);
        assertThat(categorized).allMatch(l -> l.modificationType() == ModificationType.CATEGORIZED);
    }

    // --- countByBankTransactionId ---

    @Test
    @Transactional
    @DisplayName("countByBankTransactionId: counts all entries for a transaction")
    void countByBankTransactionIdCounts() {
        modLogRepository.save(TransactionModificationLog.create(
            bankTransactionId, ModificationType.CATEGORIZED,
            null, null, null, "user", NOW
        ));
        modLogRepository.save(TransactionModificationLog.create(
            bankTransactionId, ModificationType.EXCLUDED,
            null, null, null, "user", NOW.plusSeconds(60)
        ));

        assertThat(modLogRepository.countByBankTransactionId(bankTransactionId)).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("countByBankTransactionId: returns zero for no entries")
    void countByBankTransactionIdReturnsZeroForNone() {
        assertThat(modLogRepository.countByBankTransactionId(UUID.randomUUID())).isZero();
    }
}
