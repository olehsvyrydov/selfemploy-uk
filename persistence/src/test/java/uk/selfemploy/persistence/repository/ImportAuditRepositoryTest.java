package uk.selfemploy.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.BusinessType;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.common.enums.ImportAuditType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ImportAuditRepository.
 */
@QuarkusTest
@DisplayName("ImportAuditRepository Integration Tests")
class ImportAuditRepositoryTest {

    @Inject
    ImportAuditRepository importAuditRepository;

    @Inject
    BusinessRepository businessRepository;

    private UUID businessId;

    @BeforeEach
    @Transactional
    void setUp() {
        importAuditRepository.deleteAll();
        businessRepository.deleteAll();

        Business business = businessRepository.save(Business.create(
            "Test Business", "1234567890",
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5),
            BusinessType.SELF_EMPLOYED, null
        ));
        businessId = business.id();
    }

    @Test
    @Transactional
    @DisplayName("should save and retrieve import audit")
    void shouldSaveAndRetrieveImportAudit() {
        // Given
        UUID recordId1 = UUID.randomUUID();
        UUID recordId2 = UUID.randomUUID();
        List<UUID> recordIds = List.of(recordId1, recordId2);

        ImportAudit audit = ImportAudit.create(
            businessId,
            Instant.now(),
            "bank_statement.csv",
            "abc123def456",
            ImportAuditType.CSV_INCOME,
            50, 45, 5,
            recordIds
        );

        // When
        ImportAudit saved = importAuditRepository.save(audit);

        // Then
        assertThat(saved.id()).isEqualTo(audit.id());
        assertThat(saved.businessId()).isEqualTo(businessId);
        assertThat(saved.fileName()).isEqualTo("bank_statement.csv");
        assertThat(saved.fileHash()).isEqualTo("abc123def456");
        assertThat(saved.importType()).isEqualTo(ImportAuditType.CSV_INCOME);
        assertThat(saved.totalRecords()).isEqualTo(50);
        assertThat(saved.importedCount()).isEqualTo(45);
        assertThat(saved.skippedCount()).isEqualTo(5);
        assertThat(saved.recordIds()).containsExactly(recordId1, recordId2);
        assertThat(saved.status()).isEqualTo(ImportAuditStatus.ACTIVE);
    }

    @Test
    @Transactional
    @DisplayName("should find audit by ID")
    void shouldFindAuditById() {
        // Given
        ImportAudit audit = ImportAudit.create(
            businessId, Instant.now(), "test.csv", "hash",
            ImportAuditType.CSV_INCOME, 10, 10, 0, List.of()
        );
        importAuditRepository.save(audit);

        // When
        Optional<ImportAudit> found = importAuditRepository.findByIdAsDomain(audit.id());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().fileName()).isEqualTo("test.csv");
    }

    @Test
    @Transactional
    @DisplayName("should find all audits by business ID")
    void shouldFindAllAuditsByBusinessId() {
        // Given
        importAuditRepository.save(ImportAudit.create(
            businessId, Instant.now(), "file1.csv", "hash1",
            ImportAuditType.CSV_INCOME, 10, 10, 0, List.of()
        ));
        importAuditRepository.save(ImportAudit.create(
            businessId, Instant.now(), "file2.csv", "hash2",
            ImportAuditType.CSV_EXPENSE, 20, 18, 2, List.of()
        ));

        // When
        List<ImportAudit> audits = importAuditRepository.findByBusinessId(businessId);

        // Then
        assertThat(audits).hasSize(2);
    }

    @Test
    @Transactional
    @DisplayName("should update status to UNDONE")
    void shouldUpdateStatusToUndone() {
        // Given
        ImportAudit audit = ImportAudit.create(
            businessId, Instant.now(), "test.csv", "hash",
            ImportAuditType.CSV_INCOME, 10, 10, 0, List.of()
        );
        importAuditRepository.save(audit);

        // When
        ImportAudit undoneAudit = audit.withUndone(Instant.now(), "user");
        ImportAudit updated = importAuditRepository.updateStatus(undoneAudit);

        // Then
        assertThat(updated.status()).isEqualTo(ImportAuditStatus.UNDONE);
        assertThat(updated.undoneAt()).isNotNull();
        assertThat(updated.undoneBy()).isEqualTo("user");
    }

    @Test
    @Transactional
    @DisplayName("should find undoable imports within time window")
    void shouldFindUndoableImportsWithinTimeWindow() {
        // Given
        Instant now = Instant.now();
        Instant recentTime = now.minus(3, ChronoUnit.DAYS);
        Instant oldTime = now.minus(10, ChronoUnit.DAYS);

        // Recent import (should be undoable)
        importAuditRepository.save(ImportAudit.create(
            businessId, recentTime, "recent.csv", "hash1",
            ImportAuditType.CSV_INCOME, 10, 10, 0, List.of()
        ));

        // Old import (should not be undoable)
        importAuditRepository.save(ImportAudit.create(
            businessId, oldTime, "old.csv", "hash2",
            ImportAuditType.CSV_INCOME, 10, 10, 0, List.of()
        ));

        Instant cutoff = now.minus(7, ChronoUnit.DAYS);

        // When
        List<ImportAudit> undoable = importAuditRepository.findUndoableImports(businessId, cutoff);

        // Then
        assertThat(undoable).hasSize(1);
        assertThat(undoable.get(0).fileName()).isEqualTo("recent.csv");
    }

    @Test
    @Transactional
    @DisplayName("should not include already undone imports in undoable list")
    void shouldNotIncludeAlreadyUndoneImports() {
        // Given
        Instant now = Instant.now();
        ImportAudit activeAudit = ImportAudit.create(
            businessId, now, "active.csv", "hash1",
            ImportAuditType.CSV_INCOME, 10, 10, 0, List.of()
        );
        importAuditRepository.save(activeAudit);

        ImportAudit undoneAudit = ImportAudit.create(
            businessId, now, "undone.csv", "hash2",
            ImportAuditType.CSV_INCOME, 10, 10, 0, List.of()
        ).withUndone(now, "user");
        importAuditRepository.save(undoneAudit);

        Instant cutoff = now.minus(7, ChronoUnit.DAYS);

        // When
        List<ImportAudit> undoable = importAuditRepository.findUndoableImports(businessId, cutoff);

        // Then
        assertThat(undoable).hasSize(1);
        assertThat(undoable.get(0).fileName()).isEqualTo("active.csv");
    }

    @Test
    @Transactional
    @DisplayName("should detect existing file by hash")
    void shouldDetectExistingFileByHash() {
        // Given
        String fileHash = "abc123def456";
        importAuditRepository.save(ImportAudit.create(
            businessId, Instant.now(), "original.csv", fileHash,
            ImportAuditType.CSV_INCOME, 10, 10, 0, List.of()
        ));

        // When/Then
        assertThat(importAuditRepository.existsByFileHash(businessId, fileHash)).isTrue();
        assertThat(importAuditRepository.existsByFileHash(businessId, "different")).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should not detect undone file as existing")
    void shouldNotDetectUndoneFileAsExisting() {
        // Given
        String fileHash = "abc123def456";
        ImportAudit audit = ImportAudit.create(
            businessId, Instant.now(), "original.csv", fileHash,
            ImportAuditType.CSV_INCOME, 10, 10, 0, List.of()
        ).withUndone(Instant.now(), "user");
        importAuditRepository.save(audit);

        // When/Then
        assertThat(importAuditRepository.existsByFileHash(businessId, fileHash)).isFalse();
    }
}
