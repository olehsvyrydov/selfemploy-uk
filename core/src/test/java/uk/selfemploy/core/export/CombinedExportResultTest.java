package uk.selfemploy.core.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CombinedExportResult record.
 * Tests factory methods and computed properties for multi-file exports.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("CombinedExportResult")
class CombinedExportResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("success() should create successful result with all file paths")
        void successShouldCreateSuccessfulResult() {
            // Given
            Path incomePath = Path.of("/tmp/income.csv");
            Path expensePath = Path.of("/tmp/expenses.csv");
            Path summaryPath = Path.of("/tmp/summary.txt");

            // When
            CombinedExportResult result = CombinedExportResult.success(
                incomePath, expensePath, summaryPath, 10, 5
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.incomeFilePath()).isEqualTo(incomePath);
            assertThat(result.expenseFilePath()).isEqualTo(expensePath);
            assertThat(result.summaryFilePath()).isEqualTo(summaryPath);
            assertThat(result.incomeCount()).isEqualTo(10);
            assertThat(result.expenseCount()).isEqualTo(5);
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("success() with zero counts should be valid")
        void successWithZeroCountsShouldBeValid() {
            // When
            CombinedExportResult result = CombinedExportResult.success(
                Path.of("/tmp/income.csv"),
                Path.of("/tmp/expenses.csv"),
                Path.of("/tmp/summary.txt"),
                0, 0
            );

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
            String errorMessage = "Failed to create output directory";

            // When
            CombinedExportResult result = CombinedExportResult.failure(errorMessage);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.incomeFilePath()).isNull();
            assertThat(result.expenseFilePath()).isNull();
            assertThat(result.summaryFilePath()).isNull();
            assertThat(result.incomeCount()).isZero();
            assertThat(result.expenseCount()).isZero();
            assertThat(result.errorMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("failure() with null message should be allowed")
        void failureWithNullMessageShouldBeAllowed() {
            // When
            CombinedExportResult result = CombinedExportResult.failure(null);

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
            "100, 200, 300",
            "1000, 500, 1500"
        })
        @DisplayName("totalCount() should return sum of income and expense counts")
        void totalCountShouldReturnSum(int income, int expense, int expected) {
            // When
            CombinedExportResult result = CombinedExportResult.success(
                Path.of("/tmp/i.csv"), Path.of("/tmp/e.csv"), Path.of("/tmp/s.txt"),
                income, expense
            );

            // Then
            assertThat(result.totalCount()).isEqualTo(expected);
        }

        @Test
        @DisplayName("totalCount() on failure result should return zero")
        void totalCountOnFailureShouldReturnZero() {
            // When
            CombinedExportResult result = CombinedExportResult.failure("error");

            // Then
            assertThat(result.totalCount()).isZero();
        }
    }

    @Nested
    @DisplayName("File Paths")
    class FilePaths {

        @Test
        @DisplayName("should preserve distinct file paths")
        void shouldPreserveDistinctPaths() {
            // Given
            Path incomePath = Path.of("/exports/2024/income.csv");
            Path expensePath = Path.of("/exports/2024/expenses.csv");
            Path summaryPath = Path.of("/exports/2024/summary.txt");

            // When
            CombinedExportResult result = CombinedExportResult.success(
                incomePath, expensePath, summaryPath, 1, 1
            );

            // Then
            assertThat(result.incomeFilePath()).isNotEqualTo(result.expenseFilePath());
            assertThat(result.incomeFilePath()).isNotEqualTo(result.summaryFilePath());
            assertThat(result.expenseFilePath()).isNotEqualTo(result.summaryFilePath());
        }

        @Test
        @DisplayName("should handle paths with timestamps")
        void shouldHandlePathsWithTimestamps() {
            // Given
            Path incomePath = Path.of("/tmp/income_20240115_143022.csv");
            Path expensePath = Path.of("/tmp/expenses_20240115_143022.csv");
            Path summaryPath = Path.of("/tmp/summary_20240115_143022.txt");

            // When
            CombinedExportResult result = CombinedExportResult.success(
                incomePath, expensePath, summaryPath, 10, 5
            );

            // Then
            assertThat(result.incomeFilePath().toString()).contains("20240115_143022");
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("equal results should be equal")
        void equalResultsShouldBeEqual() {
            // Given
            Path i = Path.of("/tmp/i.csv");
            Path e = Path.of("/tmp/e.csv");
            Path s = Path.of("/tmp/s.txt");

            CombinedExportResult result1 = CombinedExportResult.success(i, e, s, 10, 5);
            CombinedExportResult result2 = CombinedExportResult.success(i, e, s, 10, 5);

            // Then
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("different counts should not be equal")
        void differentCountsShouldNotBeEqual() {
            // Given
            Path i = Path.of("/tmp/i.csv");
            Path e = Path.of("/tmp/e.csv");
            Path s = Path.of("/tmp/s.txt");

            CombinedExportResult result1 = CombinedExportResult.success(i, e, s, 10, 5);
            CombinedExportResult result2 = CombinedExportResult.success(i, e, s, 10, 6);

            // Then
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("different paths should not be equal")
        void differentPathsShouldNotBeEqual() {
            // Given
            CombinedExportResult result1 = CombinedExportResult.success(
                Path.of("/a/i.csv"), Path.of("/a/e.csv"), Path.of("/a/s.txt"), 10, 5
            );
            CombinedExportResult result2 = CombinedExportResult.success(
                Path.of("/b/i.csv"), Path.of("/b/e.csv"), Path.of("/b/s.txt"), 10, 5
            );

            // Then
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("failure results with same error should be equal")
        void failureResultsWithSameErrorShouldBeEqual() {
            // Given
            CombinedExportResult result1 = CombinedExportResult.failure("error");
            CombinedExportResult result2 = CombinedExportResult.failure("error");

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
            CombinedExportResult result = CombinedExportResult.success(
                Path.of("/tmp/i.csv"), Path.of("/tmp/e.csv"), Path.of("/tmp/s.txt"),
                Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2
            );

            // Then
            assertThat(result.incomeCount()).isEqualTo(Integer.MAX_VALUE / 2);
            assertThat(result.expenseCount()).isEqualTo(Integer.MAX_VALUE / 2);
        }

        @Test
        @DisplayName("should handle paths with special characters")
        void shouldHandlePathsWithSpecialCharacters() {
            // Given
            Path incomePath = Path.of("/tmp/export report (2024).csv");
            Path expensePath = Path.of("/tmp/expenses & costs.csv");
            Path summaryPath = Path.of("/tmp/summary [final].txt");

            // When
            CombinedExportResult result = CombinedExportResult.success(
                incomePath, expensePath, summaryPath, 1, 1
            );

            // Then
            assertThat(result.incomeFilePath()).isEqualTo(incomePath);
            assertThat(result.expenseFilePath()).isEqualTo(expensePath);
            assertThat(result.summaryFilePath()).isEqualTo(summaryPath);
        }

        @Test
        @DisplayName("should handle very long error message")
        void shouldHandleVeryLongErrorMessage() {
            // Given
            String longError = "Error: " + "x".repeat(10000);

            // When
            CombinedExportResult result = CombinedExportResult.failure(longError);

            // Then
            assertThat(result.errorMessage()).startsWith("Error: ");
            assertThat(result.errorMessage()).hasSize(10007);
        }
    }
}
