package uk.selfemploy.core.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ImportPreview record.
 * Tests factory methods and validation state.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("ImportPreview")
class ImportPreviewTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("valid() should create valid preview with record count")
        void validShouldCreateValidPreview() {
            // Given
            List<String> warnings = List.of("Optional column missing");

            // When
            ImportPreview preview = ImportPreview.valid(50, warnings);

            // Then
            assertThat(preview.isValid()).isTrue();
            assertThat(preview.recordCount()).isEqualTo(50);
            assertThat(preview.validRecordCount()).isEqualTo(50);
            assertThat(preview.invalidRecordCount()).isZero();
            assertThat(preview.warnings()).containsExactly("Optional column missing");
            assertThat(preview.errors()).isEmpty();
        }

        @Test
        @DisplayName("valid() with empty warnings should be allowed")
        void validWithEmptyWarningsShouldBeAllowed() {
            // When
            ImportPreview preview = ImportPreview.valid(10, List.of());

            // Then
            assertThat(preview.isValid()).isTrue();
            assertThat(preview.warnings()).isEmpty();
        }

        @Test
        @DisplayName("valid() with zero records should be allowed")
        void validWithZeroRecordsShouldBeAllowed() {
            // When
            ImportPreview preview = ImportPreview.valid(0, List.of("File has headers only"));

            // Then
            assertThat(preview.isValid()).isTrue();
            assertThat(preview.recordCount()).isZero();
        }

        @Test
        @DisplayName("partial() should create preview with valid and invalid counts")
        void partialShouldCreateMixedPreview() {
            // Given
            List<String> warnings = List.of("Some records skipped");
            List<String> errors = List.of("Row 3: Invalid date", "Row 7: Missing amount");

            // When
            ImportPreview preview = ImportPreview.partial(8, 2, warnings, errors);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.recordCount()).isEqualTo(10);
            assertThat(preview.validRecordCount()).isEqualTo(8);
            assertThat(preview.invalidRecordCount()).isEqualTo(2);
            assertThat(preview.warnings()).containsExactly("Some records skipped");
            assertThat(preview.errors()).hasSize(2);
        }

        @Test
        @DisplayName("partial() with all invalid should not be valid")
        void partialWithAllInvalidShouldNotBeValid() {
            // When
            ImportPreview preview = ImportPreview.partial(0, 10, List.of(), List.of("All invalid"));

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.validRecordCount()).isZero();
            assertThat(preview.invalidRecordCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("invalid() should create invalid preview with errors only")
        void invalidShouldCreateInvalidPreview() {
            // Given
            List<String> errors = List.of("Invalid file format", "Missing required headers");

            // When
            ImportPreview preview = ImportPreview.invalid(errors);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.recordCount()).isZero();
            assertThat(preview.validRecordCount()).isZero();
            assertThat(preview.invalidRecordCount()).isZero();
            assertThat(preview.warnings()).isEmpty();
            assertThat(preview.errors()).hasSize(2);
        }

        @Test
        @DisplayName("invalid() with single error should work")
        void invalidWithSingleErrorShouldWork() {
            // When
            ImportPreview preview = ImportPreview.invalid(List.of("File not readable"));

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).containsExactly("File not readable");
        }
    }

    @Nested
    @DisplayName("Record Count Validation")
    class RecordCountValidation {

        @Test
        @DisplayName("recordCount should equal sum of valid and invalid in partial")
        void recordCountShouldEqualSumInPartial() {
            // Given
            int validCount = 45;
            int invalidCount = 5;

            // When
            ImportPreview preview = ImportPreview.partial(validCount, invalidCount, List.of(), List.of());

            // Then
            assertThat(preview.recordCount()).isEqualTo(validCount + invalidCount);
        }

        @Test
        @DisplayName("recordCount in valid() should equal validRecordCount")
        void recordCountInValidShouldEqualValidCount() {
            // When
            ImportPreview preview = ImportPreview.valid(100, List.of());

            // Then
            assertThat(preview.recordCount()).isEqualTo(preview.validRecordCount());
            assertThat(preview.invalidRecordCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("equal previews should be equal")
        void equalPreviewsShouldBeEqual() {
            // Given
            ImportPreview preview1 = ImportPreview.valid(10, List.of("warning"));
            ImportPreview preview2 = ImportPreview.valid(10, List.of("warning"));

            // Then
            assertThat(preview1).isEqualTo(preview2);
            assertThat(preview1.hashCode()).isEqualTo(preview2.hashCode());
        }

        @Test
        @DisplayName("different previews should not be equal")
        void differentPreviewsShouldNotBeEqual() {
            // Given
            ImportPreview preview1 = ImportPreview.valid(10, List.of());
            ImportPreview preview2 = ImportPreview.valid(20, List.of());

            // Then
            assertThat(preview1).isNotEqualTo(preview2);
        }

        @Test
        @DisplayName("previews with different errors should not be equal")
        void previewsWithDifferentErrorsShouldNotBeEqual() {
            // Given
            ImportPreview preview1 = ImportPreview.invalid(List.of("error1"));
            ImportPreview preview2 = ImportPreview.invalid(List.of("error2"));

            // Then
            assertThat(preview1).isNotEqualTo(preview2);
        }
    }

    @Nested
    @DisplayName("Warnings and Errors")
    class WarningsAndErrors {

        @Test
        @DisplayName("should preserve order of warnings")
        void shouldPreserveOrderOfWarnings() {
            // Given
            List<String> warnings = List.of("first", "second", "third");

            // When
            ImportPreview preview = ImportPreview.valid(10, warnings);

            // Then
            assertThat(preview.warnings()).containsExactly("first", "second", "third");
        }

        @Test
        @DisplayName("should preserve order of errors")
        void shouldPreserveOrderOfErrors() {
            // Given
            List<String> errors = List.of("Row 1", "Row 2", "Row 3");

            // When
            ImportPreview preview = ImportPreview.invalid(errors);

            // Then
            assertThat(preview.errors()).containsExactly("Row 1", "Row 2", "Row 3");
        }

        @Test
        @DisplayName("partial() can have both warnings and errors")
        void partialCanHaveBothWarningsAndErrors() {
            // Given
            List<String> warnings = List.of("Missing optional field");
            List<String> errors = List.of("Invalid row");

            // When
            ImportPreview preview = ImportPreview.partial(9, 1, warnings, errors);

            // Then
            assertThat(preview.warnings()).hasSize(1);
            assertThat(preview.errors()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle large record counts")
        void shouldHandleLargeRecordCounts() {
            // When
            ImportPreview preview = ImportPreview.valid(1000000, List.of());

            // Then
            assertThat(preview.recordCount()).isEqualTo(1000000);
        }

        @Test
        @DisplayName("should handle many warnings")
        void shouldHandleManyWarnings() {
            // Given
            List<String> warnings = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> "Warning " + i)
                .toList();

            // When
            ImportPreview preview = ImportPreview.valid(10, warnings);

            // Then
            assertThat(preview.warnings()).hasSize(100);
        }

        @Test
        @DisplayName("should handle many errors")
        void shouldHandleManyErrors() {
            // Given
            List<String> errors = java.util.stream.IntStream.range(0, 1000)
                .mapToObj(i -> "Error at row " + i)
                .toList();

            // When
            ImportPreview preview = ImportPreview.invalid(errors);

            // Then
            assertThat(preview.errors()).hasSize(1000);
        }
    }
}
