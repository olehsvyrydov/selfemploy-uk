package uk.selfemploy.core.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ExportResult record.
 * Tests factory methods and computed properties.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("ExportResult")
class ExportResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("success() should create successful result with path and counts")
        void successShouldCreateSuccessfulResult() {
            // Given
            Path filePath = Path.of("/tmp/export.json");
            int incomeCount = 10;
            int expenseCount = 5;

            // When
            ExportResult result = ExportResult.success(filePath, incomeCount, expenseCount);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.filePath()).isEqualTo(filePath);
            assertThat(result.incomeCount()).isEqualTo(incomeCount);
            assertThat(result.expenseCount()).isEqualTo(expenseCount);
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("success() with zero counts should be valid")
        void successWithZeroCountsShouldBeValid() {
            // When
            ExportResult result = ExportResult.success(Path.of("/tmp/empty.json"), 0, 0);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.incomeCount()).isZero();
            assertThat(result.expenseCount()).isZero();
            assertThat(result.totalCount()).isZero();
        }

        @Test
        @DisplayName("failure() should create failed result with error message")
        void failureShouldCreateFailedResult() {
            // Given
            String errorMessage = "Export failed: disk full";

            // When
            ExportResult result = ExportResult.failure(errorMessage);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.filePath()).isNull();
            assertThat(result.incomeCount()).isZero();
            assertThat(result.expenseCount()).isZero();
            assertThat(result.errorMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("failure() with null message should be allowed")
        void failureWithNullMessageShouldBeAllowed() {
            // When
            ExportResult result = ExportResult.failure(null);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("Total Count Calculation")
    class TotalCountCalculation {

        @ParameterizedTest
        @CsvSource({
            "0, 0, 0",
            "10, 0, 10",
            "0, 5, 5",
            "10, 5, 15",
            "100, 200, 300"
        })
        @DisplayName("totalCount() should return sum of income and expense counts")
        void totalCountShouldReturnSum(int income, int expense, int expected) {
            // When
            ExportResult result = ExportResult.success(Path.of("/tmp/test.json"), income, expense);

            // Then
            assertThat(result.totalCount()).isEqualTo(expected);
        }

        @Test
        @DisplayName("totalCount() on failure result should return zero")
        void totalCountOnFailureShouldReturnZero() {
            // When
            ExportResult result = ExportResult.failure("error");

            // Then
            assertThat(result.totalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("equal results should be equal")
        void equalResultsShouldBeEqual() {
            // Given
            Path path = Path.of("/tmp/test.json");
            ExportResult result1 = ExportResult.success(path, 10, 5);
            ExportResult result2 = ExportResult.success(path, 10, 5);

            // Then
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("different results should not be equal")
        void differentResultsShouldNotBeEqual() {
            // Given
            ExportResult result1 = ExportResult.success(Path.of("/tmp/a.json"), 10, 5);
            ExportResult result2 = ExportResult.success(Path.of("/tmp/b.json"), 10, 5);

            // Then
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("failure results with same error should be equal")
        void failureResultsWithSameErrorShouldBeEqual() {
            // Given
            ExportResult result1 = ExportResult.failure("error");
            ExportResult result2 = ExportResult.failure("error");

            // Then
            assertThat(result1).isEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle large counts")
        void shouldHandleLargeCounts() {
            // When
            ExportResult result = ExportResult.success(Path.of("/tmp/large.json"),
                Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2);

            // Then
            assertThat(result.incomeCount()).isEqualTo(Integer.MAX_VALUE / 2);
            assertThat(result.expenseCount()).isEqualTo(Integer.MAX_VALUE / 2);
        }

        @Test
        @DisplayName("should handle very long error message")
        void shouldHandleVeryLongErrorMessage() {
            // Given
            String longError = "x".repeat(10000);

            // When
            ExportResult result = ExportResult.failure(longError);

            // Then
            assertThat(result.errorMessage()).hasSize(10000);
        }

        @Test
        @DisplayName("should handle path with special characters")
        void shouldHandlePathWithSpecialCharacters() {
            // Given
            Path path = Path.of("/tmp/export with spaces & symbols.json");

            // When
            ExportResult result = ExportResult.success(path, 1, 1);

            // Then
            assertThat(result.filePath()).isEqualTo(path);
        }
    }
}
