package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TaxYear Entity Tests")
class TaxYearTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("should create valid tax year 2025/26")
        void shouldCreateValidTaxYear2025_26() {
            TaxYear taxYear = TaxYear.of(2025);

            assertThat(taxYear).isNotNull();
            assertThat(taxYear.id()).isNotNull();
            assertThat(taxYear.startYear()).isEqualTo(2025);
            assertThat(taxYear.startDate()).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(taxYear.endDate()).isEqualTo(LocalDate.of(2026, 4, 5));
            assertThat(taxYear.label()).isEqualTo("2025/26");
        }

        @Test
        @DisplayName("should create valid tax year 2024/25")
        void shouldCreateValidTaxYear2024_25() {
            TaxYear taxYear = TaxYear.of(2024);

            assertThat(taxYear.startDate()).isEqualTo(LocalDate.of(2024, 4, 6));
            assertThat(taxYear.endDate()).isEqualTo(LocalDate.of(2025, 4, 5));
            assertThat(taxYear.label()).isEqualTo("2024/25");
        }

        @Test
        @DisplayName("should generate unique ID for each tax year")
        void shouldGenerateUniqueId() {
            TaxYear taxYear1 = TaxYear.of(2025);
            TaxYear taxYear2 = TaxYear.of(2025);

            // Different instances should have different IDs
            assertThat(taxYear1.id()).isNotEqualTo(taxYear2.id());
        }
    }

    @Nested
    @DisplayName("Date Range Tests")
    class DateRangeTests {

        @ParameterizedTest
        @CsvSource({
            "2020, 2020-04-06, 2021-04-05",
            "2021, 2021-04-06, 2022-04-05",
            "2022, 2022-04-06, 2023-04-05",
            "2023, 2023-04-06, 2024-04-05",
            "2024, 2024-04-06, 2025-04-05",
            "2025, 2025-04-06, 2026-04-05",
            "2026, 2026-04-06, 2027-04-05"
        })
        @DisplayName("should calculate correct date ranges for tax years")
        void shouldCalculateCorrectDateRanges(int startYear, LocalDate expectedStart, LocalDate expectedEnd) {
            TaxYear taxYear = TaxYear.of(startYear);

            assertThat(taxYear.startDate()).isEqualTo(expectedStart);
            assertThat(taxYear.endDate()).isEqualTo(expectedEnd);
        }
    }

    @Nested
    @DisplayName("Label Tests")
    class LabelTests {

        @ParameterizedTest
        @CsvSource({
            "2020, 2020/21",
            "2021, 2021/22",
            "2022, 2022/23",
            "2023, 2023/24",
            "2024, 2024/25",
            "2025, 2025/26",
            "2026, 2026/27"
        })
        @DisplayName("should generate correct labels for tax years")
        void shouldGenerateCorrectLabels(int startYear, String expectedLabel) {
            TaxYear taxYear = TaxYear.of(startYear);

            assertThat(taxYear.label()).isEqualTo(expectedLabel);
        }
    }

    @Nested
    @DisplayName("Contains Date Tests")
    class ContainsDateTests {

        @Test
        @DisplayName("should return true for date within tax year")
        void shouldReturnTrueForDateWithinTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);

            assertThat(taxYear.contains(LocalDate.of(2025, 4, 6))).isTrue();   // Start date
            assertThat(taxYear.contains(LocalDate.of(2025, 7, 15))).isTrue();  // Middle
            assertThat(taxYear.contains(LocalDate.of(2026, 1, 1))).isTrue();   // January next year
            assertThat(taxYear.contains(LocalDate.of(2026, 4, 5))).isTrue();   // End date
        }

        @Test
        @DisplayName("should return false for date outside tax year")
        void shouldReturnFalseForDateOutsideTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);

            assertThat(taxYear.contains(LocalDate.of(2025, 4, 5))).isFalse();  // Day before start
            assertThat(taxYear.contains(LocalDate.of(2026, 4, 6))).isFalse();  // Day after end
            assertThat(taxYear.contains(LocalDate.of(2024, 12, 31))).isFalse();
        }

        @Test
        @DisplayName("should handle null date")
        void shouldHandleNullDate() {
            TaxYear taxYear = TaxYear.of(2025);

            assertThat(taxYear.contains(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Current Tax Year Tests")
    class CurrentTaxYearTests {

        @Test
        @DisplayName("should return current tax year based on today's date")
        void shouldReturnCurrentTaxYear() {
            TaxYear current = TaxYear.current();
            LocalDate today = LocalDate.now();

            assertThat(current.contains(today)).isTrue();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should reject year before 2000")
        void shouldRejectYearBefore2000() {
            assertThatThrownBy(() -> TaxYear.of(1999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("year");
        }

        @Test
        @DisplayName("should reject year after 2100")
        void shouldRejectYearAfter2100() {
            assertThatThrownBy(() -> TaxYear.of(2101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("year");
        }

        @Test
        @DisplayName("should accept valid year range")
        void shouldAcceptValidYearRange() {
            // Should not throw
            TaxYear.of(2000);
            TaxYear.of(2050);
            TaxYear.of(2100);
        }
    }

    @Nested
    @DisplayName("Navigation Tests")
    class NavigationTests {

        @Test
        @DisplayName("should return previous tax year")
        void shouldReturnPreviousTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);
            TaxYear previous = taxYear.previous();

            assertThat(previous.startYear()).isEqualTo(2024);
            assertThat(previous.label()).isEqualTo("2024/25");
        }

        @Test
        @DisplayName("should return next tax year")
        void shouldReturnNextTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);
            TaxYear next = taxYear.next();

            assertThat(next.startYear()).isEqualTo(2026);
            assertThat(next.label()).isEqualTo("2026/27");
        }
    }

    @Nested
    @DisplayName("Deadline Tests")
    class DeadlineTests {

        @Test
        @DisplayName("should return correct online filing deadline")
        void shouldReturnCorrectOnlineFilingDeadline() {
            TaxYear taxYear = TaxYear.of(2025);

            // Online filing deadline is 31 January following the tax year
            assertThat(taxYear.onlineFilingDeadline()).isEqualTo(LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("should return correct paper filing deadline")
        void shouldReturnCorrectPaperFilingDeadline() {
            TaxYear taxYear = TaxYear.of(2025);

            // Paper filing deadline is 31 October following the tax year
            assertThat(taxYear.paperFilingDeadline()).isEqualTo(LocalDate.of(2026, 10, 31));
        }

        @Test
        @DisplayName("should return correct payment deadline")
        void shouldReturnCorrectPaymentDeadline() {
            TaxYear taxYear = TaxYear.of(2025);

            // Payment deadline is 31 January following the tax year
            assertThat(taxYear.paymentDeadline()).isEqualTo(LocalDate.of(2027, 1, 31));
        }
    }

    @Nested
    @DisplayName("HMRC Format Tests")
    class HmrcFormatTests {

        @ParameterizedTest
        @CsvSource({
            "2020, 2020-21",
            "2021, 2021-22",
            "2022, 2022-23",
            "2023, 2023-24",
            "2024, 2024-25",
            "2025, 2025-26",
            "2026, 2026-27",
            "2099, 2099-00"
        })
        @DisplayName("should format tax year for HMRC API (hyphen-separated)")
        void shouldFormatForHmrcApi(int startYear, String expectedFormat) {
            TaxYear taxYear = TaxYear.of(startYear);

            assertThat(taxYear.hmrcFormat()).isEqualTo(expectedFormat);
        }

        @Test
        @DisplayName("should use hyphen separator for HMRC format")
        void shouldUseHyphenSeparator() {
            TaxYear taxYear = TaxYear.of(2025);

            assertThat(taxYear.hmrcFormat()).contains("-");
            assertThat(taxYear.hmrcFormat()).doesNotContain("/");
        }

        @Test
        @DisplayName("should pad single-digit end year with zero")
        void shouldPadSingleDigitEndYear() {
            TaxYear taxYear2099 = TaxYear.of(2099);

            // 2100 mod 100 = 0, so should be formatted as "2099-00"
            assertThat(taxYear2099.hmrcFormat()).isEqualTo("2099-00");
        }
    }
}
