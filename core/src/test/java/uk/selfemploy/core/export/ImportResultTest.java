package uk.selfemploy.core.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ImportResult record.
 * Tests factory methods and computed properties.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("ImportResult")
class ImportResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("success() should create successful result with counts")
        void successShouldCreateSuccessfulResult() {
            // When
            ImportResult result = ImportResult.success(10, 2, 1);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.importedCount()).isEqualTo(10);
            assertThat(result.skippedCount()).isEqualTo(2);
            assertThat(result.duplicateCount()).isEqualTo(1);
            assertThat(result.errorCount()).isZero();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("partial() with imported records should be successful")
        void partialWithImportedShouldBeSuccessful() {
            // Given
            List<String> errors = List.of("Row 5: Invalid date");

            // When
            ImportResult result = ImportResult.partial(8, 2, 0, 1, errors);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.importedCount()).isEqualTo(8);
            assertThat(result.skippedCount()).isEqualTo(2);
            assertThat(result.duplicateCount()).isZero();
            assertThat(result.errorCount()).isEqualTo(1);
            assertThat(result.errors()).containsExactly("Row 5: Invalid date");
        }

        @Test
        @DisplayName("partial() with no imports should be unsuccessful")
        void partialWithNoImportsShouldBeUnsuccessful() {
            // Given
            List<String> errors = List.of("All rows invalid");

            // When
            ImportResult result = ImportResult.partial(0, 5, 0, 5, errors);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.importedCount()).isZero();
        }

        @Test
        @DisplayName("failure() should create failed result with error message")
        void failureShouldCreateFailedResult() {
            // Given
            String errorMessage = "File not found";

            // When
            ImportResult result = ImportResult.failure(errorMessage);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.importedCount()).isZero();
            assertThat(result.skippedCount()).isZero();
            assertThat(result.duplicateCount()).isZero();
            assertThat(result.errorCount()).isEqualTo(1);
            assertThat(result.errors()).containsExactly(errorMessage);
        }

        @Test
        @DisplayName("success() with all zeros should be valid")
        void successWithAllZerosShouldBeValid() {
            // When
            ImportResult result = ImportResult.success(0, 0, 0);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.totalProcessed()).isZero();
        }
    }

    @Nested
    @DisplayName("Total Processed Calculation")
    class TotalProcessedCalculation {

        @ParameterizedTest
        @CsvSource({
            "0, 0, 0, 0, 0",
            "10, 0, 0, 0, 10",
            "10, 5, 0, 0, 15",
            "10, 5, 3, 0, 18",
            "10, 5, 3, 2, 20",
            "100, 50, 25, 5, 180"
        })
        @DisplayName("totalProcessed() should return sum of all counts")
        void totalProcessedShouldReturnSum(int imported, int skipped, int duplicate, int error, int expected) {
            // When
            ImportResult result = ImportResult.partial(imported, skipped, duplicate, error, List.of());

            // Then
            assertThat(result.totalProcessed()).isEqualTo(expected);
        }

        @Test
        @DisplayName("totalProcessed() on success result should match counts")
        void totalProcessedOnSuccessShouldMatchCounts() {
            // When
            ImportResult result = ImportResult.success(10, 2, 1);

            // Then
            assertThat(result.totalProcessed()).isEqualTo(13);
        }

        @Test
        @DisplayName("totalProcessed() on failure result should return 1 (error count)")
        void totalProcessedOnFailureShouldReturnErrorCount() {
            // When
            ImportResult result = ImportResult.failure("error");

            // Then
            assertThat(result.totalProcessed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should preserve multiple errors")
        void shouldPreserveMultipleErrors() {
            // Given
            List<String> errors = List.of(
                "Row 1: Invalid date",
                "Row 3: Negative amount",
                "Row 5: Missing category"
            );

            // When
            ImportResult result = ImportResult.partial(7, 3, 0, 3, errors);

            // Then
            assertThat(result.errors()).hasSize(3);
            assertThat(result.errors()).containsExactlyElementsOf(errors);
        }

        @Test
        @DisplayName("success() should have empty errors list")
        void successShouldHaveEmptyErrors() {
            // When
            ImportResult result = ImportResult.success(10, 0, 0);

            // Then
            assertThat(result.errors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("equal results should be equal")
        void equalResultsShouldBeEqual() {
            // Given
            ImportResult result1 = ImportResult.success(10, 2, 1);
            ImportResult result2 = ImportResult.success(10, 2, 1);

            // Then
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("different results should not be equal")
        void differentResultsShouldNotBeEqual() {
            // Given
            ImportResult result1 = ImportResult.success(10, 2, 1);
            ImportResult result2 = ImportResult.success(10, 3, 1);

            // Then
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("results with different error lists should not be equal")
        void resultsWithDifferentErrorsShouldNotBeEqual() {
            // Given
            ImportResult result1 = ImportResult.partial(5, 5, 0, 5, List.of("error1"));
            ImportResult result2 = ImportResult.partial(5, 5, 0, 5, List.of("error2"));

            // Then
            assertThat(result1).isNotEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle large counts")
        void shouldHandleLargeCounts() {
            // When
            ImportResult result = ImportResult.success(1000000, 500000, 100000);

            // Then
            assertThat(result.totalProcessed()).isEqualTo(1600000);
        }

        @Test
        @DisplayName("should handle empty error list in partial")
        void shouldHandleEmptyErrorListInPartial() {
            // When
            ImportResult result = ImportResult.partial(10, 0, 0, 0, List.of());

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("should handle many duplicate records")
        void shouldHandleManyDuplicates() {
            // When
            ImportResult result = ImportResult.success(10, 0, 990);

            // Then
            assertThat(result.duplicateCount()).isEqualTo(990);
            assertThat(result.totalProcessed()).isEqualTo(1000);
        }
    }
}
