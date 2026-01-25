package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ImportContext}.
 */
@DisplayName("ImportContext")
class ImportContextTest {

    @Nested
    @DisplayName("when creating context")
    class Creation {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            Map<String, Object> options = Map.of(
                ImportContext.OPTION_SKIP_DUPLICATES, true,
                ImportContext.OPTION_DEFAULT_CATEGORY, "general"
            );

            ImportContext context = new ImportContext(2024, options);

            assertThat(context.targetTaxYear()).isEqualTo(2024);
            assertThat(context.options()).containsEntry(ImportContext.OPTION_SKIP_DUPLICATES, true);
            assertThat(context.options()).containsEntry(ImportContext.OPTION_DEFAULT_CATEGORY, "general");
        }

        @Test
        @DisplayName("should convert null options to empty map")
        void shouldConvertNullOptionsToEmptyMap() {
            ImportContext context = new ImportContext(2024, null);

            assertThat(context.options()).isEmpty();
        }

        @Test
        @DisplayName("should make options immutable")
        void shouldMakeOptionsImmutable() {
            ImportContext context = new ImportContext(2024, Map.of("key", "value"));

            assertThatThrownBy(() -> context.options().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("forTaxYear should create context with defaults")
        void forTaxYearShouldCreateContextWithDefaults() {
            ImportContext context = ImportContext.forTaxYear(2024);

            assertThat(context.targetTaxYear()).isEqualTo(2024);
            assertThat(context.options()).isEmpty();
            assertThat(context.skipDuplicates()).isTrue(); // default
        }
    }

    @Nested
    @DisplayName("option helpers")
    class OptionHelpers {

        @Test
        @DisplayName("getOption should return value")
        void getOptionShouldReturnValue() {
            ImportContext context = new ImportContext(2024, Map.of("key", "value"));

            assertThat(context.<String>getOption("key", "default")).isEqualTo("value");
        }

        @Test
        @DisplayName("getOption should return default when missing")
        void getOptionShouldReturnDefaultWhenMissing() {
            ImportContext context = ImportContext.forTaxYear(2024);

            assertThat(context.<String>getOption("missing", "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("skipDuplicates should return option value")
        void skipDuplicatesShouldReturnOptionValue() {
            ImportContext context = new ImportContext(2024, Map.of(
                ImportContext.OPTION_SKIP_DUPLICATES, false
            ));

            assertThat(context.skipDuplicates()).isFalse();
        }

        @Test
        @DisplayName("skipDuplicates should return true by default")
        void skipDuplicatesShouldReturnTrueByDefault() {
            ImportContext context = ImportContext.forTaxYear(2024);

            assertThat(context.skipDuplicates()).isTrue();
        }
    }
}
