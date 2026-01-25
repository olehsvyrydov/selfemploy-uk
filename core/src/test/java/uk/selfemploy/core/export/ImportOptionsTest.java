package uk.selfemploy.core.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ImportOptions record.
 * Tests factory methods and option flags.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("ImportOptions")
class ImportOptionsTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("defaults() should create options with all flags false")
        void defaultsShouldCreateOptionsWithAllFlagsFalse() {
            // When
            ImportOptions options = ImportOptions.defaults();

            // Then
            assertThat(options.mergeExisting()).isFalse();
            assertThat(options.skipDuplicatesEnabled()).isFalse();
        }

        @Test
        @DisplayName("withSkipDuplicates() should enable skip duplicates flag")
        void withSkipDuplicatesShouldEnableFlag() {
            // When
            ImportOptions options = ImportOptions.withSkipDuplicates();

            // Then
            assertThat(options.mergeExisting()).isFalse();
            assertThat(options.skipDuplicatesEnabled()).isTrue();
        }

        @Test
        @DisplayName("constructor should accept any combination of flags")
        void constructorShouldAcceptAnyCombination() {
            // When
            ImportOptions options1 = new ImportOptions(true, false);
            ImportOptions options2 = new ImportOptions(false, true);
            ImportOptions options3 = new ImportOptions(true, true);
            ImportOptions options4 = new ImportOptions(false, false);

            // Then
            assertThat(options1.mergeExisting()).isTrue();
            assertThat(options1.skipDuplicatesEnabled()).isFalse();

            assertThat(options2.mergeExisting()).isFalse();
            assertThat(options2.skipDuplicatesEnabled()).isTrue();

            assertThat(options3.mergeExisting()).isTrue();
            assertThat(options3.skipDuplicatesEnabled()).isTrue();

            assertThat(options4.mergeExisting()).isFalse();
            assertThat(options4.skipDuplicatesEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("equal options should be equal")
        void equalOptionsShouldBeEqual() {
            // Given
            ImportOptions options1 = ImportOptions.defaults();
            ImportOptions options2 = ImportOptions.defaults();

            // Then
            assertThat(options1).isEqualTo(options2);
            assertThat(options1.hashCode()).isEqualTo(options2.hashCode());
        }

        @Test
        @DisplayName("withSkipDuplicates() options should be equal")
        void withSkipDuplicatesOptionsShouldBeEqual() {
            // Given
            ImportOptions options1 = ImportOptions.withSkipDuplicates();
            ImportOptions options2 = ImportOptions.withSkipDuplicates();

            // Then
            assertThat(options1).isEqualTo(options2);
            assertThat(options1.hashCode()).isEqualTo(options2.hashCode());
        }

        @Test
        @DisplayName("different options should not be equal")
        void differentOptionsShouldNotBeEqual() {
            // Given
            ImportOptions defaults = ImportOptions.defaults();
            ImportOptions skipDuplicates = ImportOptions.withSkipDuplicates();

            // Then
            assertThat(defaults).isNotEqualTo(skipDuplicates);
        }

        @Test
        @DisplayName("options with different merge flag should not be equal")
        void optionsWithDifferentMergeFlagShouldNotBeEqual() {
            // Given
            ImportOptions options1 = new ImportOptions(true, false);
            ImportOptions options2 = new ImportOptions(false, false);

            // Then
            assertThat(options1).isNotEqualTo(options2);
        }
    }

    @Nested
    @DisplayName("Flag Combinations")
    class FlagCombinations {

        @Test
        @DisplayName("merge only should have mergeExisting true")
        void mergeOnlyShouldHaveMergeExistingTrue() {
            // When
            ImportOptions options = new ImportOptions(true, false);

            // Then
            assertThat(options.mergeExisting()).isTrue();
            assertThat(options.skipDuplicatesEnabled()).isFalse();
        }

        @Test
        @DisplayName("skip duplicates only should have skipDuplicatesEnabled true")
        void skipDuplicatesOnlyShouldHaveSkipDuplicatesEnabledTrue() {
            // When
            ImportOptions options = new ImportOptions(false, true);

            // Then
            assertThat(options.mergeExisting()).isFalse();
            assertThat(options.skipDuplicatesEnabled()).isTrue();
        }

        @Test
        @DisplayName("both flags can be true")
        void bothFlagsCanBeTrue() {
            // When
            ImportOptions options = new ImportOptions(true, true);

            // Then
            assertThat(options.mergeExisting()).isTrue();
            assertThat(options.skipDuplicatesEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Usage Patterns")
    class UsagePatterns {

        @Test
        @DisplayName("defaults() is equivalent to new ImportOptions(false, false)")
        void defaultsIsEquivalentToConstructorWithFalse() {
            // Given
            ImportOptions defaults = ImportOptions.defaults();
            ImportOptions explicit = new ImportOptions(false, false);

            // Then
            assertThat(defaults).isEqualTo(explicit);
        }

        @Test
        @DisplayName("withSkipDuplicates() is equivalent to new ImportOptions(false, true)")
        void withSkipDuplicatesIsEquivalentToConstructor() {
            // Given
            ImportOptions withSkip = ImportOptions.withSkipDuplicates();
            ImportOptions explicit = new ImportOptions(false, true);

            // Then
            assertThat(withSkip).isEqualTo(explicit);
        }
    }
}
