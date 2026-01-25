package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ImportResult}.
 */
@DisplayName("ImportResult")
class ImportResultTest {

    @Nested
    @DisplayName("when creating result")
    class Creation {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            List<String> errors = List.of("Error 1");
            List<String> warnings = List.of("Warning 1");

            ImportResult result = new ImportResult(10, 2, 3, errors, warnings);

            assertThat(result.importedCount()).isEqualTo(10);
            assertThat(result.skippedCount()).isEqualTo(2);
            assertThat(result.duplicateCount()).isEqualTo(3);
            assertThat(result.errors()).containsExactly("Error 1");
            assertThat(result.warnings()).containsExactly("Warning 1");
        }

        @Test
        @DisplayName("should convert null errors to empty list")
        void shouldConvertNullErrorsToEmptyList() {
            ImportResult result = new ImportResult(10, 0, 0, null, null);

            assertThat(result.errors()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("should reject negative imported count")
        void shouldRejectNegativeImportedCount() {
            assertThatThrownBy(() -> new ImportResult(-1, 0, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Imported count");
        }

        @Test
        @DisplayName("should reject negative skipped count")
        void shouldRejectNegativeSkippedCount() {
            assertThatThrownBy(() -> new ImportResult(0, -1, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skipped count");
        }

        @Test
        @DisplayName("should reject negative duplicate count")
        void shouldRejectNegativeDuplicateCount() {
            assertThatThrownBy(() -> new ImportResult(0, 0, -1, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate count");
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("success should create successful result")
        void successShouldCreateSuccessfulResult() {
            ImportResult result = ImportResult.success(50, 3);

            assertThat(result.importedCount()).isEqualTo(50);
            assertThat(result.duplicateCount()).isEqualTo(3);
            assertThat(result.skippedCount()).isZero();
            assertThat(result.errors()).isEmpty();
            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("failure should create failed result")
        void failureShouldCreateFailedResult() {
            ImportResult result = ImportResult.failure("File format not supported");

            assertThat(result.importedCount()).isZero();
            assertThat(result.errors()).containsExactly("File format not supported");
            assertThat(result.hasErrors()).isTrue();
        }
    }

    @Nested
    @DisplayName("helper methods")
    class HelperMethods {

        @Test
        @DisplayName("totalProcessed should sum all counts")
        void totalProcessedShouldSumAllCounts() {
            ImportResult result = new ImportResult(10, 5, 3, null, null);

            assertThat(result.totalProcessed()).isEqualTo(18);
        }

        @Test
        @DisplayName("hasErrors should return false when no errors")
        void hasErrorsShouldReturnFalseWhenNoErrors() {
            ImportResult result = ImportResult.success(10, 0);

            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("hasWarnings should return true when warnings exist")
        void hasWarningsShouldReturnTrueWhenWarningsExist() {
            ImportResult result = new ImportResult(10, 0, 0, null, List.of("Warning"));

            assertThat(result.hasWarnings()).isTrue();
        }
    }
}
