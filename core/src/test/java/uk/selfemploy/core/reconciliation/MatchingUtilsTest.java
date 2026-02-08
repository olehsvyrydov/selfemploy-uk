package uk.selfemploy.core.reconciliation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for MatchingUtils - shared matching utilities for reconciliation.
 */
@DisplayName("MatchingUtils Tests")
class MatchingUtilsTest {

    @Nested
    @DisplayName("normalizeDescription")
    class NormalizeDescription {

        @Test
        void shouldReturnEmptyForNull() {
            assertThat(MatchingUtils.normalizeDescription(null)).isEmpty();
        }

        @Test
        void shouldLowercase() {
            assertThat(MatchingUtils.normalizeDescription("ACME LTD")).isEqualTo("acme ltd");
        }

        @Test
        void shouldTrim() {
            assertThat(MatchingUtils.normalizeDescription("  payment  ")).isEqualTo("payment");
        }

        @Test
        void shouldCollapseWhitespace() {
            assertThat(MatchingUtils.normalizeDescription("payment   for   goods"))
                .isEqualTo("payment for goods");
        }

        @Test
        void shouldHandleTabsAndNewlines() {
            assertThat(MatchingUtils.normalizeDescription("payment\t\nfor\r\ngoods"))
                .isEqualTo("payment for goods");
        }

        @Test
        void shouldHandleEmpty() {
            assertThat(MatchingUtils.normalizeDescription("")).isEmpty();
        }

        @Test
        void shouldHandleAlreadyNormalized() {
            assertThat(MatchingUtils.normalizeDescription("simple payment"))
                .isEqualTo("simple payment");
        }
    }

    @Nested
    @DisplayName("calculateSimilarity")
    class CalculateSimilarity {

        @Test
        void identicalStringsReturnOne() {
            assertThat(MatchingUtils.calculateSimilarity("payment", "payment")).isEqualTo(1.0);
        }

        @Test
        void completelyDifferentStringsReturnLow() {
            double similarity = MatchingUtils.calculateSimilarity("abcdef", "xyz");
            assertThat(similarity).isLessThan(0.5);
        }

        @Test
        void similarStringsReturnHighScore() {
            // "acme ltd" vs "acme limited" - similar but not identical
            double similarity = MatchingUtils.calculateSimilarity("acme ltd", "acme limited");
            assertThat(similarity).isGreaterThan(0.5);
        }

        @Test
        void bothEmptyReturnsOne() {
            assertThat(MatchingUtils.calculateSimilarity("", "")).isEqualTo(1.0);
        }

        @Test
        void nullTreatedAsEmpty() {
            assertThat(MatchingUtils.calculateSimilarity(null, null)).isEqualTo(1.0);
        }

        @Test
        void oneEmptyOneNotReturnsZero() {
            assertThat(MatchingUtils.calculateSimilarity("", "something")).isEqualTo(0.0);
        }

        @Test
        void singleCharDifferenceIsClose() {
            // "payment" vs "payments" - 1 char difference, 8 max length
            double similarity = MatchingUtils.calculateSimilarity("payment", "payments");
            assertThat(similarity).isCloseTo(0.875, within(0.01));
        }
    }

    @Nested
    @DisplayName("createExactKey")
    class CreateExactKey {

        @Test
        void shouldCreatePipeDelimitedKey() {
            String key = MatchingUtils.createExactKey(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "Payment for goods");
            assertThat(key).isEqualTo("2025-06-15|100|payment for goods");
        }

        @Test
        void shouldNormalizeDescription() {
            String key = MatchingUtils.createExactKey(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("50.00"),
                "  ACME  LTD  ");
            assertThat(key).contains("acme ltd");
        }

        @Test
        void shouldStripTrailingZeros() {
            String key1 = MatchingUtils.createExactKey(
                LocalDate.of(2025, 1, 1), new BigDecimal("100.00"), "test");
            String key2 = MatchingUtils.createExactKey(
                LocalDate.of(2025, 1, 1), new BigDecimal("100"), "test");
            assertThat(key1).isEqualTo(key2);
        }
    }

    @Nested
    @DisplayName("isWithinTolerance")
    class IsWithinTolerance {

        @Test
        void exactAmountsAreWithinTolerance() {
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("100.00"), new BigDecimal("100.00"))).isTrue();
        }

        @Test
        void nullAmountsAreNotWithinTolerance() {
            assertThat(MatchingUtils.isWithinTolerance(null, new BigDecimal("100.00"))).isFalse();
            assertThat(MatchingUtils.isWithinTolerance(new BigDecimal("100.00"), null)).isFalse();
        }

        @Test
        void withinOnePercentForLargeAmounts() {
            // GBP 1000.00 +/- 1% = GBP 990 to GBP 1010
            // 1% of 1000 = 10, which is > 1.00, so 1% applies
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("1000.00"), new BigDecimal("1009.99"))).isTrue();
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("1000.00"), new BigDecimal("1010.01"))).isFalse();
        }

        @Test
        void withinOneGbpForSmallAmounts() {
            // GBP 50.00 - 1% = GBP 0.50, absolute = GBP 1.00
            // GBP 1.00 > GBP 0.50, so GBP 1.00 tolerance applies
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("50.00"), new BigDecimal("51.00"))).isTrue();
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("50.00"), new BigDecimal("51.01"))).isFalse();
        }

        @Test
        void toleranceSwitchesFromAbsoluteToRelativeAt100() {
            // At GBP 100: 1% = GBP 1.00, absolute = GBP 1.00 -> equal, both apply
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("100.00"), new BigDecimal("101.00"))).isTrue();
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("100.00"), new BigDecimal("101.01"))).isFalse();
        }

        @Test
        void verySmallAmountUsesAbsoluteTolerance() {
            // GBP 5.00 - 1% = GBP 0.05, absolute = GBP 1.00
            // GBP 1.00 > GBP 0.05, so GBP 1.00 applies
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("5.00"), new BigDecimal("5.99"))).isTrue();
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("5.00"), new BigDecimal("6.01"))).isFalse();
        }

        @Test
        void symmetricComparison() {
            // Order should not matter
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("100.00"), new BigDecimal("100.50"))).isTrue();
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("100.50"), new BigDecimal("100.00"))).isTrue();
        }

        @Test
        void beyondToleranceReturnsFalse() {
            // GBP 500.00 - 1% = GBP 5.00 (> 1.00, so 1% applies)
            // GBP 500 + 5 = 505
            assertThat(MatchingUtils.isWithinTolerance(
                new BigDecimal("500.00"), new BigDecimal("505.01"))).isFalse();
        }
    }

    @Nested
    @DisplayName("isExactAmount")
    class IsExactAmount {

        @Test
        void sameAmountsAreExact() {
            assertThat(MatchingUtils.isExactAmount(
                new BigDecimal("100.00"), new BigDecimal("100.00"))).isTrue();
        }

        @Test
        void differentScaleSameValueAreExact() {
            assertThat(MatchingUtils.isExactAmount(
                new BigDecimal("100.0"), new BigDecimal("100.00"))).isTrue();
        }

        @Test
        void differentAmountsAreNotExact() {
            assertThat(MatchingUtils.isExactAmount(
                new BigDecimal("100.00"), new BigDecimal("100.01"))).isFalse();
        }

        @Test
        void nullAmountsAreNotExact() {
            assertThat(MatchingUtils.isExactAmount(null, new BigDecimal("100.00"))).isFalse();
            assertThat(MatchingUtils.isExactAmount(new BigDecimal("100.00"), null)).isFalse();
        }

        @Test
        void pennyDifferenceIsNotExact() {
            assertThat(MatchingUtils.isExactAmount(
                new BigDecimal("99.99"), new BigDecimal("100.00"))).isFalse();
        }
    }
}
