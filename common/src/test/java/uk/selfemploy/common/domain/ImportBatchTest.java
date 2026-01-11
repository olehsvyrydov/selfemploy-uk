package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ImportBatch domain object.
 */
@DisplayName("ImportBatch Tests")
class ImportBatchTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final String BANK_NAME = "Barclays";
    private static final String FILE_NAME = "statement_2025.csv";
    private static final long FILE_SIZE = 10240L;
    private static final int TOTAL_TRANSACTIONS = 50;
    private static final int INCOME_COUNT = 20;
    private static final int EXPENSE_COUNT = 25;
    private static final int DUPLICATE_COUNT = 5;

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("should create import batch with valid data")
        void shouldCreateWithValidData() {
            ImportBatch batch = ImportBatch.create(
                BUSINESS_ID, BANK_NAME, FILE_NAME, FILE_SIZE,
                TOTAL_TRANSACTIONS, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT
            );

            assertThat(batch.id()).isNotNull();
            assertThat(batch.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(batch.bankName()).isEqualTo(BANK_NAME);
            assertThat(batch.fileName()).isEqualTo(FILE_NAME);
            assertThat(batch.fileSizeBytes()).isEqualTo(FILE_SIZE);
            assertThat(batch.totalTransactions()).isEqualTo(TOTAL_TRANSACTIONS);
            assertThat(batch.incomeCount()).isEqualTo(INCOME_COUNT);
            assertThat(batch.expenseCount()).isEqualTo(EXPENSE_COUNT);
            assertThat(batch.duplicateCount()).isEqualTo(DUPLICATE_COUNT);
            assertThat(batch.importedAt()).isNotNull();
        }

        @Test
        @DisplayName("should generate unique IDs")
        void shouldGenerateUniqueIds() {
            ImportBatch batch1 = ImportBatch.create(
                BUSINESS_ID, BANK_NAME, FILE_NAME, FILE_SIZE,
                TOTAL_TRANSACTIONS, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT
            );
            ImportBatch batch2 = ImportBatch.create(
                BUSINESS_ID, BANK_NAME, FILE_NAME, FILE_SIZE,
                TOTAL_TRANSACTIONS, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT
            );

            assertThat(batch1.id()).isNotEqualTo(batch2.id());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should throw when businessId is null")
        void shouldThrowWhenBusinessIdNull() {
            assertThatThrownBy(() -> ImportBatch.create(
                null, BANK_NAME, FILE_NAME, FILE_SIZE,
                TOTAL_TRANSACTIONS, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("businessId");
        }

        @Test
        @DisplayName("should throw when bankName is null or blank")
        void shouldThrowWhenBankNameNullOrBlank() {
            assertThatThrownBy(() -> ImportBatch.create(
                BUSINESS_ID, null, FILE_NAME, FILE_SIZE,
                TOTAL_TRANSACTIONS, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bankName");

            assertThatThrownBy(() -> ImportBatch.create(
                BUSINESS_ID, "  ", FILE_NAME, FILE_SIZE,
                TOTAL_TRANSACTIONS, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bankName");
        }

        @Test
        @DisplayName("should throw when fileName is null or blank")
        void shouldThrowWhenFileNameNullOrBlank() {
            assertThatThrownBy(() -> ImportBatch.create(
                BUSINESS_ID, BANK_NAME, null, FILE_SIZE,
                TOTAL_TRANSACTIONS, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileName");
        }

        @Test
        @DisplayName("should throw when fileSizeBytes is negative")
        void shouldThrowWhenFileSizeNegative() {
            assertThatThrownBy(() -> ImportBatch.create(
                BUSINESS_ID, BANK_NAME, FILE_NAME, -1L,
                TOTAL_TRANSACTIONS, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileSizeBytes");
        }

        @Test
        @DisplayName("should throw when counts are negative")
        void shouldThrowWhenCountsNegative() {
            assertThatThrownBy(() -> ImportBatch.create(
                BUSINESS_ID, BANK_NAME, FILE_NAME, FILE_SIZE,
                -1, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalTransactions");

            assertThatThrownBy(() -> ImportBatch.create(
                BUSINESS_ID, BANK_NAME, FILE_NAME, FILE_SIZE,
                TOTAL_TRANSACTIONS, -1, EXPENSE_COUNT, DUPLICATE_COUNT
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incomeCount");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("should create with existing ID and timestamp")
        void shouldCreateWithExistingIdAndTimestamp() {
            UUID id = UUID.randomUUID();
            Instant importedAt = Instant.now().minusSeconds(3600);

            ImportBatch batch = new ImportBatch(
                id, BUSINESS_ID, BANK_NAME, FILE_NAME, FILE_SIZE,
                TOTAL_TRANSACTIONS, INCOME_COUNT, EXPENSE_COUNT, DUPLICATE_COUNT, importedAt
            );

            assertThat(batch.id()).isEqualTo(id);
            assertThat(batch.importedAt()).isEqualTo(importedAt);
        }
    }
}
