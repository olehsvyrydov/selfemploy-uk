package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VersionRange}.
 */
@DisplayName("VersionRange")
class VersionRangeTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("should reject null or blank range")
        void shouldRejectNullOrBlankRange(String range) {
            assertThatThrownBy(() -> new VersionRange(range))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null or blank");
        }

        @Test
        @DisplayName("should accept valid exact version")
        void shouldAcceptValidExactVersion() {
            var range = new VersionRange("1.0.0");
            assertThat(range.getRangeString()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("should reject invalid version format")
        void shouldRejectInvalidVersionFormat() {
            assertThatThrownBy(() -> new VersionRange("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Exact version matching")
    class ExactVersionMatching {

        @Test
        @DisplayName("should match exact version")
        void shouldMatchExactVersion() {
            var range = new VersionRange("1.2.3");
            assertThat(range.matches("1.2.3")).isTrue();
        }

        @Test
        @DisplayName("should not match different version")
        void shouldNotMatchDifferentVersion() {
            var range = new VersionRange("1.2.3");
            assertThat(range.matches("1.2.4")).isFalse();
            assertThat(range.matches("1.3.3")).isFalse();
            assertThat(range.matches("2.2.3")).isFalse();
        }
    }

    @Nested
    @DisplayName("Caret range (^) matching")
    class CaretRangeMatching {

        @ParameterizedTest
        @CsvSource({
            "1.0.0, true",
            "1.2.3, true",
            "1.9.9, true",
            "0.9.9, false",
            "2.0.0, false",
            "2.0.1, false"
        })
        @DisplayName("^1.0.0 should match >=1.0.0 <2.0.0")
        void shouldMatchCaretRange(String version, boolean expected) {
            var range = new VersionRange("^1.0.0");
            assertThat(range.matches(version)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
            "0.1.0, true",
            "0.1.5, true",
            "0.0.9, false",
            "0.2.0, false",
            "1.0.0, false"
        })
        @DisplayName("^0.1.0 should match >=0.1.0 <0.2.0 (more restrictive for 0.x)")
        void shouldMatchCaretRangeForZeroMajor(String version, boolean expected) {
            var range = new VersionRange("^0.1.0");
            assertThat(range.matches(version)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Tilde range (~) matching")
    class TildeRangeMatching {

        @ParameterizedTest
        @CsvSource({
            "1.2.0, true",
            "1.2.3, true",
            "1.2.9, true",
            "1.1.9, false",
            "1.3.0, false",
            "2.0.0, false"
        })
        @DisplayName("~1.2.0 should match >=1.2.0 <1.3.0")
        void shouldMatchTildeRange(String version, boolean expected) {
            var range = new VersionRange("~1.2.0");
            assertThat(range.matches(version)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Comparator matching")
    class ComparatorMatching {

        @ParameterizedTest
        @CsvSource({
            "1.0.0, true",
            "1.5.0, true",
            "2.0.0, true",
            "0.9.9, false"
        })
        @DisplayName(">=1.0.0 should match versions greater than or equal")
        void shouldMatchGreaterThanOrEqual(String version, boolean expected) {
            var range = new VersionRange(">=1.0.0");
            assertThat(range.matches(version)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
            "1.5.0, true",
            "2.0.0, true",
            "1.0.0, false",
            "0.9.9, false"
        })
        @DisplayName(">1.0.0 should match versions strictly greater than")
        void shouldMatchGreaterThan(String version, boolean expected) {
            var range = new VersionRange(">1.0.0");
            assertThat(range.matches(version)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
            "0.9.9, true",
            "1.9.9, true",
            "2.0.0, false",
            "2.0.1, false"
        })
        @DisplayName("<2.0.0 should match versions strictly less than")
        void shouldMatchLessThan(String version, boolean expected) {
            var range = new VersionRange("<2.0.0");
            assertThat(range.matches(version)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
            "0.9.9, true",
            "2.0.0, true",
            "2.0.1, false"
        })
        @DisplayName("<=2.0.0 should match versions less than or equal")
        void shouldMatchLessThanOrEqual(String version, boolean expected) {
            var range = new VersionRange("<=2.0.0");
            assertThat(range.matches(version)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Combined range matching")
    class CombinedRangeMatching {

        @ParameterizedTest
        @CsvSource({
            "1.0.0, true",
            "1.5.0, true",
            "1.9.9, true",
            "0.9.9, false",
            "2.0.0, false"
        })
        @DisplayName(">=1.0.0 <2.0.0 should match range")
        void shouldMatchCombinedRange(String version, boolean expected) {
            var range = new VersionRange(">=1.0.0 <2.0.0");
            assertThat(range.matches(version)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle null version")
        void shouldHandleNullVersion() {
            var range = new VersionRange("^1.0.0");
            assertThat(range.matches(null)).isFalse();
        }

        @Test
        @DisplayName("should handle empty version")
        void shouldHandleEmptyVersion() {
            var range = new VersionRange("^1.0.0");
            assertThat(range.matches("")).isFalse();
        }

        @Test
        @DisplayName("should handle invalid version format in matches")
        void shouldHandleInvalidVersionFormat() {
            var range = new VersionRange("^1.0.0");
            assertThat(range.matches("invalid")).isFalse();
        }

        @Test
        @DisplayName("should handle version with prerelease")
        void shouldHandleVersionWithPrerelease() {
            var range = new VersionRange("^1.0.0");
            assertThat(range.matches("1.0.0-alpha")).isTrue();
        }
    }

    @Nested
    @DisplayName("Object methods")
    class ObjectMethods {

        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEquals() {
            var range1 = new VersionRange("^1.0.0");
            var range2 = new VersionRange("^1.0.0");
            var range3 = new VersionRange("~1.0.0");

            assertThat(range1).isEqualTo(range2);
            assertThat(range1).isNotEqualTo(range3);
        }

        @Test
        @DisplayName("should implement hashCode correctly")
        void shouldImplementHashCode() {
            var range1 = new VersionRange("^1.0.0");
            var range2 = new VersionRange("^1.0.0");

            assertThat(range1.hashCode()).isEqualTo(range2.hashCode());
        }

        @Test
        @DisplayName("should implement toString")
        void shouldImplementToString() {
            var range = new VersionRange("^1.0.0");
            assertThat(range.toString()).contains("^1.0.0");
        }
    }
}
