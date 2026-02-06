package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.enums.ModificationType;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransactionModificationLog")
class TransactionModificationLogTest {

    private static final UUID TX_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    @Nested
    @DisplayName("Factory method create()")
    class Create {

        @Test
        @DisplayName("creates log entry with generated ID")
        void createsLogEntry() {
            TransactionModificationLog log = TransactionModificationLog.create(
                TX_ID, ModificationType.CATEGORIZED, "reviewStatus",
                "PENDING", "CATEGORIZED", "user@example.com", NOW
            );

            assertThat(log.id()).isNotNull();
            assertThat(log.bankTransactionId()).isEqualTo(TX_ID);
            assertThat(log.modificationType()).isEqualTo(ModificationType.CATEGORIZED);
            assertThat(log.fieldName()).isEqualTo("reviewStatus");
            assertThat(log.previousValue()).isEqualTo("PENDING");
            assertThat(log.newValue()).isEqualTo("CATEGORIZED");
            assertThat(log.modifiedBy()).isEqualTo("user@example.com");
            assertThat(log.modifiedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("allows null fieldName for status-only changes")
        void allowsNullFieldName() {
            TransactionModificationLog log = TransactionModificationLog.create(
                TX_ID, ModificationType.EXCLUDED, null,
                null, null, "system", NOW
            );

            assertThat(log.fieldName()).isNull();
        }

        @Test
        @DisplayName("allows null previousValue for initial categorization")
        void allowsNullPreviousValue() {
            TransactionModificationLog log = TransactionModificationLog.create(
                TX_ID, ModificationType.CATEGORIZED, "incomeId",
                null, UUID.randomUUID().toString(), "user", NOW
            );

            assertThat(log.previousValue()).isNull();
        }

        @Test
        @DisplayName("allows null newValue for removals")
        void allowsNullNewValue() {
            TransactionModificationLog log = TransactionModificationLog.create(
                TX_ID, ModificationType.RESTORED, "incomeId",
                UUID.randomUUID().toString(), null, "user", NOW
            );

            assertThat(log.newValue()).isNull();
        }

        @Test
        @DisplayName("generates unique IDs for each entry")
        void generatesUniqueIds() {
            TransactionModificationLog log1 = TransactionModificationLog.create(
                TX_ID, ModificationType.CATEGORIZED, null, null, null, "user", NOW
            );
            TransactionModificationLog log2 = TransactionModificationLog.create(
                TX_ID, ModificationType.EXCLUDED, null, null, null, "user", NOW
            );

            assertThat(log1.id()).isNotEqualTo(log2.id());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("rejects null bankTransactionId")
        void rejectsNullBankTransactionId() {
            assertThatThrownBy(() -> TransactionModificationLog.create(
                null, ModificationType.CATEGORIZED, null, null, null, "user", NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("bankTransactionId");
        }

        @Test
        @DisplayName("rejects null modificationType")
        void rejectsNullModificationType() {
            assertThatThrownBy(() -> TransactionModificationLog.create(
                TX_ID, null, null, null, null, "user", NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("modificationType");
        }

        @Test
        @DisplayName("rejects null modifiedBy")
        void rejectsNullModifiedBy() {
            assertThatThrownBy(() -> TransactionModificationLog.create(
                TX_ID, ModificationType.CATEGORIZED, null, null, null, null, NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("modifiedBy");
        }

        @Test
        @DisplayName("rejects blank modifiedBy")
        void rejectsBlankModifiedBy() {
            assertThatThrownBy(() -> TransactionModificationLog.create(
                TX_ID, ModificationType.CATEGORIZED, null, null, null, "  ", NOW
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("modifiedBy");
        }

        @Test
        @DisplayName("rejects null modifiedAt")
        void rejectsNullModifiedAt() {
            assertThatThrownBy(() -> TransactionModificationLog.create(
                TX_ID, ModificationType.CATEGORIZED, null, null, null, "user", null
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("modifiedAt");
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("record is a value type")
        void recordIsValueType() {
            TransactionModificationLog log1 = new TransactionModificationLog(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                TX_ID, ModificationType.CATEGORIZED, "field", "old", "new", "user", NOW
            );
            TransactionModificationLog log2 = new TransactionModificationLog(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                TX_ID, ModificationType.CATEGORIZED, "field", "old", "new", "user", NOW
            );

            assertThat(log1).isEqualTo(log2);
        }
    }
}
