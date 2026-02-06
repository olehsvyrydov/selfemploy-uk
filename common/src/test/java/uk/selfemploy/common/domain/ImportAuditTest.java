package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.enums.ImportAuditStatus;
import uk.selfemploy.common.enums.ImportAuditType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImportAudit")
class ImportAuditTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    @Nested
    @DisplayName("Basic create()")
    class BasicCreate {

        @Test
        @DisplayName("creates active audit with null audit trail fields")
        void createsWithNullAuditTrailFields() {
            ImportAudit audit = ImportAudit.create(
                BUSINESS_ID, NOW, "test.csv", "hash123",
                ImportAuditType.BANK_CSV, 10, 8, 2, List.of()
            );

            assertThat(audit.id()).isNotNull();
            assertThat(audit.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(audit.status()).isEqualTo(ImportAuditStatus.ACTIVE);
            assertThat(audit.originalFilePath()).isNull();
            assertThat(audit.originalFileEncrypted()).isNull();
            assertThat(audit.retentionUntil()).isNull();
            assertThat(audit.importedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("createWithAuditTrail()")
    class CreateWithAuditTrail {

        @Test
        @DisplayName("creates audit with all audit trail fields populated")
        void createsWithAuditTrailFields() {
            LocalDate retention = LocalDate.of(2031, 6, 15);
            ImportAudit audit = ImportAudit.createWithAuditTrail(
                BUSINESS_ID, NOW, "barclays-2025.csv", "sha256hash",
                ImportAuditType.BANK_CSV, 50, 48, 2, List.of(),
                "/data/encrypted/barclays-2025.csv.enc", true,
                retention, "user@example.com"
            );

            assertThat(audit.id()).isNotNull();
            assertThat(audit.status()).isEqualTo(ImportAuditStatus.ACTIVE);
            assertThat(audit.originalFilePath()).isEqualTo("/data/encrypted/barclays-2025.csv.enc");
            assertThat(audit.originalFileEncrypted()).isTrue();
            assertThat(audit.retentionUntil()).isEqualTo(retention);
            assertThat(audit.importedBy()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("preserves basic fields alongside audit trail fields")
        void preservesBasicFields() {
            ImportAudit audit = ImportAudit.createWithAuditTrail(
                BUSINESS_ID, NOW, "test.csv", "hash",
                ImportAuditType.BANK_CSV, 10, 8, 2, List.of(UUID.randomUUID()),
                "/path/file.enc", true, LocalDate.of(2031, 1, 1), "user"
            );

            assertThat(audit.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(audit.importTimestamp()).isEqualTo(NOW);
            assertThat(audit.fileName()).isEqualTo("test.csv");
            assertThat(audit.fileHash()).isEqualTo("hash");
            assertThat(audit.importType()).isEqualTo(ImportAuditType.BANK_CSV);
            assertThat(audit.totalRecords()).isEqualTo(10);
            assertThat(audit.importedCount()).isEqualTo(8);
            assertThat(audit.skippedCount()).isEqualTo(2);
            assertThat(audit.recordIds()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("hasEncryptedFile()")
    class HasEncryptedFile {

        @Test
        @DisplayName("returns true when file path exists and encrypted flag is true")
        void trueWhenEncrypted() {
            ImportAudit audit = ImportAudit.createWithAuditTrail(
                BUSINESS_ID, NOW, "f.csv", "h",
                ImportAuditType.BANK_CSV, 1, 1, 0, List.of(),
                "/path/file.enc", true, LocalDate.of(2031, 1, 1), "user"
            );

            assertThat(audit.hasEncryptedFile()).isTrue();
        }

        @Test
        @DisplayName("returns false when file path is null")
        void falseWhenNoPath() {
            ImportAudit audit = ImportAudit.create(
                BUSINESS_ID, NOW, "f.csv", "h",
                ImportAuditType.BANK_CSV, 1, 1, 0, List.of()
            );

            assertThat(audit.hasEncryptedFile()).isFalse();
        }

        @Test
        @DisplayName("returns false when encrypted flag is false")
        void falseWhenNotEncrypted() {
            ImportAudit audit = ImportAudit.createWithAuditTrail(
                BUSINESS_ID, NOW, "f.csv", "h",
                ImportAuditType.BANK_CSV, 1, 1, 0, List.of(),
                "/path/file.csv", false, LocalDate.of(2031, 1, 1), "user"
            );

            assertThat(audit.hasEncryptedFile()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasRetentionPolicy()")
    class HasRetentionPolicy {

        @Test
        @DisplayName("returns true when retention date is set")
        void trueWhenSet() {
            ImportAudit audit = ImportAudit.createWithAuditTrail(
                BUSINESS_ID, NOW, "f.csv", "h",
                ImportAuditType.BANK_CSV, 1, 1, 0, List.of(),
                "/path", true, LocalDate.of(2031, 1, 1), "user"
            );

            assertThat(audit.hasRetentionPolicy()).isTrue();
        }

        @Test
        @DisplayName("returns false when retention date is null")
        void falseWhenNull() {
            ImportAudit audit = ImportAudit.create(
                BUSINESS_ID, NOW, "f.csv", "h",
                ImportAuditType.BANK_CSV, 1, 1, 0, List.of()
            );

            assertThat(audit.hasRetentionPolicy()).isFalse();
        }
    }

    @Nested
    @DisplayName("withUndone() preserves audit trail fields")
    class WithUndone {

        @Test
        @DisplayName("preserves audit trail fields through undo transition")
        void preservesAuditTrailFields() {
            LocalDate retention = LocalDate.of(2031, 6, 15);
            ImportAudit audit = ImportAudit.createWithAuditTrail(
                BUSINESS_ID, NOW, "f.csv", "h",
                ImportAuditType.BANK_CSV, 1, 1, 0, List.of(),
                "/path/file.enc", true, retention, "user@example.com"
            );

            Instant undoneAt = NOW.plusSeconds(300);
            ImportAudit undone = audit.withUndone(undoneAt, "admin");

            assertThat(undone.status()).isEqualTo(ImportAuditStatus.UNDONE);
            assertThat(undone.undoneAt()).isEqualTo(undoneAt);
            assertThat(undone.undoneBy()).isEqualTo("admin");
            assertThat(undone.originalFilePath()).isEqualTo("/path/file.enc");
            assertThat(undone.originalFileEncrypted()).isTrue();
            assertThat(undone.retentionUntil()).isEqualTo(retention);
            assertThat(undone.importedBy()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("rejects null businessId")
        void rejectsNullBusinessId() {
            assertThatThrownBy(() -> ImportAudit.create(
                null, NOW, "f.csv", "h",
                ImportAuditType.BANK_CSV, 1, 1, 0, List.of()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("businessId");
        }

        @Test
        @DisplayName("rejects null importTimestamp")
        void rejectsNullTimestamp() {
            assertThatThrownBy(() -> ImportAudit.create(
                BUSINESS_ID, null, "f.csv", "h",
                ImportAuditType.BANK_CSV, 1, 1, 0, List.of()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("importTimestamp");
        }

        @Test
        @DisplayName("rejects negative totalRecords")
        void rejectsNegativeTotal() {
            assertThatThrownBy(() -> ImportAudit.create(
                BUSINESS_ID, NOW, "f.csv", "h",
                ImportAuditType.BANK_CSV, -1, 0, 0, List.of()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("totalRecords");
        }
    }
}
