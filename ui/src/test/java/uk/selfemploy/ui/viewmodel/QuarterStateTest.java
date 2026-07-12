package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuarterState")
class QuarterStateTest {

    private static final TaxYear TY_2025 = TaxYear.of(2025); // 6 Apr 2025 - 5 Apr 2026

    @Nested
    @DisplayName("isCurrentQuarter around tax-year quarter boundaries")
    class IsCurrentQuarter {

        @Test
        @DisplayName("5 Jul is the last day of Q1")
        void lastDayOfQ1() {
            LocalDate day = LocalDate.of(2025, 7, 5);
            assertThat(QuarterState.isCurrentQuarter(Quarter.Q1, TY_2025, day)).isTrue();
            assertThat(QuarterState.isCurrentQuarter(Quarter.Q2, TY_2025, day)).isFalse();
        }

        @Test
        @DisplayName("6 Jul is the first day of Q2")
        void firstDayOfQ2() {
            LocalDate day = LocalDate.of(2025, 7, 6);
            assertThat(QuarterState.isCurrentQuarter(Quarter.Q2, TY_2025, day)).isTrue();
            assertThat(QuarterState.isCurrentQuarter(Quarter.Q1, TY_2025, day)).isFalse();
        }

        @Test
        @DisplayName("5 Oct is the last day of Q2")
        void lastDayOfQ2() {
            LocalDate day = LocalDate.of(2025, 10, 5);
            assertThat(QuarterState.isCurrentQuarter(Quarter.Q2, TY_2025, day)).isTrue();
            assertThat(QuarterState.isCurrentQuarter(Quarter.Q3, TY_2025, day)).isFalse();
        }

        @Test
        @DisplayName("6 Jan is the first day of Q4")
        void firstDayOfQ4() {
            LocalDate day = LocalDate.of(2026, 1, 6);
            assertThat(QuarterState.isCurrentQuarter(Quarter.Q4, TY_2025, day)).isTrue();
            assertThat(QuarterState.isCurrentQuarter(Quarter.Q3, TY_2025, day)).isFalse();
        }

        @Test
        @DisplayName("no quarter is current when the displayed tax year does not contain today")
        void noCurrentQuarterForPastTaxYear() {
            // Today falls in 2025/26 but the user is viewing 2024/25.
            LocalDate day = LocalDate.of(2025, 7, 6);
            TaxYear viewed = TaxYear.of(2024);
            for (Quarter q : Quarter.values()) {
                assertThat(QuarterState.isCurrentQuarter(q, viewed, day)).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("resolveStatus")
    class ResolveStatus {

        @Test
        @DisplayName("a not-yet-started quarter is FUTURE")
        void futureQuarter() {
            LocalDate day = LocalDate.of(2025, 8, 1); // during Q2
            assertThat(QuarterState.resolveStatus(Quarter.Q3, TY_2025, day, false))
                .isEqualTo(QuarterStatus.FUTURE);
        }

        @Test
        @DisplayName("the in-progress current quarter with no data is DRAFT, not FUTURE")
        void currentQuarterIsDraft() {
            LocalDate day = LocalDate.of(2025, 8, 1); // within Q2
            assertThat(QuarterState.resolveStatus(Quarter.Q2, TY_2025, day, false))
                .isEqualTo(QuarterStatus.DRAFT);
        }

        @Test
        @DisplayName("a quarter past its deadline is OVERDUE")
        void overdueQuarter() {
            LocalDate day = LocalDate.of(2025, 12, 1); // past Q1 deadline 7 Aug 2025
            assertThat(QuarterState.resolveStatus(Quarter.Q1, TY_2025, day, false))
                .isEqualTo(QuarterStatus.OVERDUE);
        }

        @Test
        @DisplayName("every quarter of a fully past tax year is OVERDUE")
        void pastTaxYearAllOverdue() {
            LocalDate day = LocalDate.of(2026, 7, 11); // after all 2025/26 deadlines
            for (Quarter q : Quarter.values()) {
                assertThat(QuarterState.resolveStatus(q, TY_2025, day, false))
                    .isEqualTo(QuarterStatus.OVERDUE);
            }
        }

        @Test
        @DisplayName("null calendar context fails closed as FUTURE instead of throwing")
        void nullArgumentsFailClosed() {
            LocalDate day = LocalDate.of(2025, 8, 1);
            assertThat(QuarterState.resolveStatus(null, TY_2025, day, false))
                .isEqualTo(QuarterStatus.FUTURE);
            assertThat(QuarterState.resolveStatus(Quarter.Q2, null, day, false))
                .isEqualTo(QuarterStatus.FUTURE);
            assertThat(QuarterState.resolveStatus(Quarter.Q2, TY_2025, null, false))
                .isEqualTo(QuarterStatus.FUTURE);
        }
    }
}
