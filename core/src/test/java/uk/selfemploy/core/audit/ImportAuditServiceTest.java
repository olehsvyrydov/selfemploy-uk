package uk.selfemploy.core.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.ImportAudit;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.common.enums.ImportAuditType;
import uk.selfemploy.persistence.repository.ImportAuditRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for Import Audit Trail (Sprint 10B).
 * Tests AUD-U01 through AUD-U10 from /rob's test design.
 *
 * <p>SE-10B-002: Import Audit Trail Tests</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Import Audit Service Tests (Sprint 10B)")
class ImportAuditServiceTest {

    @Mock
    private ImportAuditRepository auditRepository;

    private Clock fixedClock;
    private ImportAuditService service;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final Instant FIXED_TIME = Instant.parse("2025-06-15T10:30:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));
        service = new ImportAuditService(auditRepository, fixedClock);
    }

    // === AUD-U01 to AUD-U04: Audit Record Creation ===

    @Nested
    @DisplayName("Audit Record Creation")
    class AuditRecordCreation {

        @Test
        @DisplayName("AUD-U01: should create audit record on import start")
        void shouldCreateAuditRecordOnImportStart() {
            // Given
            String fileName = "bank_statement.csv";
            String fileHash = "abc123def456";
            ImportAuditType type = ImportAuditType.CSV_INCOME;
            int totalRecords = 50;
            int importedCount = 45;
            int skippedCount = 5;
            List<UUID> recordIds = List.of(UUID.randomUUID(), UUID.randomUUID());

            when(auditRepository.save(any(ImportAudit.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ImportAudit audit = service.createAuditRecord(
                BUSINESS_ID, fileName, fileHash, type,
                totalRecords, importedCount, skippedCount, recordIds
            );

            // Then
            assertThat(audit).isNotNull();
            assertThat(audit.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(audit.importTimestamp()).isEqualTo(FIXED_TIME);
            assertThat(audit.status()).isEqualTo(ImportAuditStatus.ACTIVE);

            verify(auditRepository).save(any(ImportAudit.class));
        }

        @Test
        @DisplayName("AUD-U02: should store file hash (SHA-256) for verification")
        void shouldStoreFileHashForVerification() {
            // Given
            String fileHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            when(auditRepository.save(any(ImportAudit.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ImportAudit audit = service.createAuditRecord(
                BUSINESS_ID, "test.csv", fileHash, ImportAuditType.CSV_INCOME,
                10, 10, 0, List.of()
            );

            // Then
            assertThat(audit.fileHash()).isEqualTo(fileHash);
        }

        @Test
        @DisplayName("AUD-U03: should store original file name")
        void shouldStoreOriginalFileName() {
            // Given
            String fileName = "my_bank_statement_2025.csv";
            when(auditRepository.save(any(ImportAudit.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ImportAudit audit = service.createAuditRecord(
                BUSINESS_ID, fileName, "hash123", ImportAuditType.CSV_INCOME,
                10, 10, 0, List.of()
            );

            // Then
            assertThat(audit.fileName()).isEqualTo(fileName);
        }

        @Test
        @DisplayName("AUD-U04: should store import type (INCOME/EXPENSE)")
        void shouldStoreImportType() {
            // Given
            when(auditRepository.save(any(ImportAudit.class))).thenAnswer(inv -> inv.getArgument(0));

            // When - Income import
            ImportAudit incomeAudit = service.createAuditRecord(
                BUSINESS_ID, "income.csv", "hash1", ImportAuditType.CSV_INCOME,
                10, 10, 0, List.of()
            );

            // Then
            assertThat(incomeAudit.importType()).isEqualTo(ImportAuditType.CSV_INCOME);

            // When - Expense import
            ImportAudit expenseAudit = service.createAuditRecord(
                BUSINESS_ID, "expense.csv", "hash2", ImportAuditType.CSV_EXPENSE,
                10, 10, 0, List.of()
            );

            // Then
            assertThat(expenseAudit.importType()).isEqualTo(ImportAuditType.CSV_EXPENSE);
        }
    }

    // === AUD-U05 to AUD-U07: Record Counts and Data ===

    @Nested
    @DisplayName("Record Counts and Data")
    class RecordCountsAndData {

        @Test
        @DisplayName("AUD-U05: should store record counts (imported/skipped/updated)")
        void shouldStoreRecordCounts() {
            // Given
            int totalRecords = 100;
            int importedCount = 85;
            int skippedCount = 15;

            when(auditRepository.save(any(ImportAudit.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ImportAudit audit = service.createAuditRecord(
                BUSINESS_ID, "test.csv", "hash", ImportAuditType.CSV_INCOME,
                totalRecords, importedCount, skippedCount, List.of()
            );

            // Then
            assertThat(audit.totalRecords()).isEqualTo(totalRecords);
            assertThat(audit.importedCount()).isEqualTo(importedCount);
            assertThat(audit.skippedCount()).isEqualTo(skippedCount);
        }

        @Test
        @DisplayName("AUD-U06: should store imported records as JSON")
        void shouldStoreImportedRecordsAsJson() {
            // Given
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            List<UUID> recordIds = List.of(id1, id2);

            when(auditRepository.save(any(ImportAudit.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ImportAudit audit = service.createAuditRecord(
                BUSINESS_ID, "test.csv", "hash", ImportAuditType.CSV_INCOME,
                2, 2, 0, recordIds
            );

            // Then
            assertThat(audit.recordIds()).containsExactly(id1, id2);
        }

        @Test
        @DisplayName("AUD-U07: should set status to ACTIVE on success")
        void shouldSetStatusToActiveOnSuccess() {
            // Given
            when(auditRepository.save(any(ImportAudit.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ImportAudit audit = service.createAuditRecord(
                BUSINESS_ID, "test.csv", "hash", ImportAuditType.CSV_INCOME,
                10, 10, 0, List.of()
            );

            // Then
            assertThat(audit.status()).isEqualTo(ImportAuditStatus.ACTIVE);
        }
    }

    // === AUD-U08 to AUD-U10: Retrieval and Validation ===

    @Nested
    @DisplayName("Retrieval and Validation")
    class RetrievalAndValidation {

        @Test
        @DisplayName("AUD-U08: should retrieve audit history by business ID")
        void shouldRetrieveAuditHistoryByBusinessId() {
            // Given
            ImportAudit audit1 = ImportAudit.create(BUSINESS_ID, FIXED_TIME,
                "file1.csv", "hash1", ImportAuditType.CSV_INCOME, 10, 10, 0, List.of());
            ImportAudit audit2 = ImportAudit.create(BUSINESS_ID, FIXED_TIME.plusSeconds(3600),
                "file2.csv", "hash2", ImportAuditType.CSV_EXPENSE, 20, 18, 2, List.of());

            when(auditRepository.findByBusinessId(BUSINESS_ID))
                .thenReturn(List.of(audit1, audit2));

            // When
            List<ImportAudit> history = service.getAuditHistory(BUSINESS_ID);

            // Then
            assertThat(history).hasSize(2);
            verify(auditRepository).findByBusinessId(BUSINESS_ID);
        }

        @Test
        @DisplayName("AUD-U09: should retrieve audit history by date range")
        void shouldRetrieveAuditHistoryByDateRange() {
            // Given
            LocalDate from = LocalDate.of(2025, 6, 1);
            LocalDate to = LocalDate.of(2025, 6, 30);

            ImportAudit audit = ImportAudit.create(BUSINESS_ID, FIXED_TIME,
                "file.csv", "hash", ImportAuditType.CSV_INCOME, 10, 10, 0, List.of());

            when(auditRepository.findByBusinessIdAndDateRange(BUSINESS_ID, from, to))
                .thenReturn(List.of(audit));

            // When
            List<ImportAudit> history = service.getAuditHistory(BUSINESS_ID, from, to);

            // Then
            assertThat(history).hasSize(1);
            verify(auditRepository).findByBusinessIdAndDateRange(BUSINESS_ID, from, to);
        }

        @Test
        @DisplayName("AUD-U10: should generate unique audit ID (UUID)")
        void shouldGenerateUniqueAuditId() {
            // Given
            when(auditRepository.save(any(ImportAudit.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            ImportAudit audit1 = service.createAuditRecord(
                BUSINESS_ID, "file1.csv", "hash1", ImportAuditType.CSV_INCOME,
                10, 10, 0, List.of()
            );
            ImportAudit audit2 = service.createAuditRecord(
                BUSINESS_ID, "file2.csv", "hash2", ImportAuditType.CSV_INCOME,
                10, 10, 0, List.of()
            );

            // Then
            assertThat(audit1.id()).isNotNull();
            assertThat(audit2.id()).isNotNull();
            assertThat(audit1.id()).isNotEqualTo(audit2.id());
        }
    }

    // === Immutability Tests (COND-F9) ===

    @Nested
    @DisplayName("Audit Immutability (COND-F9)")
    class AuditImmutability {

        @Test
        @DisplayName("should not allow delete operations on audit records")
        void shouldNotAllowDeleteOperationsOnAuditRecords() {
            // The ImportAuditService should NOT expose any delete methods
            // This is verified by the absence of delete methods in the service API
            // Audit records are immutable once created (COND-F9 requirement)

            // Verify the repository save is called but no delete methods exist
            when(auditRepository.save(any(ImportAudit.class))).thenAnswer(inv -> inv.getArgument(0));

            service.createAuditRecord(
                BUSINESS_ID, "file.csv", "hash", ImportAuditType.CSV_INCOME,
                10, 10, 0, List.of()
            );

            // Verify only save was called, no other repository methods
            verify(auditRepository, only()).save(any(ImportAudit.class));
        }

        @Test
        @DisplayName("should retrieve audit by ID")
        void shouldRetrieveAuditById() {
            // Given
            UUID auditId = UUID.randomUUID();
            ImportAudit audit = ImportAudit.create(BUSINESS_ID, FIXED_TIME,
                "file.csv", "hash", ImportAuditType.CSV_INCOME, 10, 10, 0, List.of());

            when(auditRepository.findByIdAsDomain(auditId))
                .thenReturn(Optional.of(audit));

            // When
            Optional<ImportAudit> result = service.getAuditById(auditId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().fileName()).isEqualTo("file.csv");
        }
    }
}
