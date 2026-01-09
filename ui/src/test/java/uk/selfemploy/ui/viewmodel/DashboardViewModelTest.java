package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for DashboardViewModel.
 * Tests the ViewModel logic without requiring JavaFX initialization.
 */
@DisplayName("DashboardViewModel")
class DashboardViewModelTest {

    private DashboardViewModel viewModel;
    private TaxYear currentTaxYear;

    @BeforeEach
    void setUp() {
        viewModel = new DashboardViewModel();
        currentTaxYear = TaxYear.current();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should initialize with zero values")
        void shouldInitializeWithZeroValues() {
            assertThat(viewModel.getTotalIncome()).isEqualTo(BigDecimal.ZERO);
            assertThat(viewModel.getTotalExpenses()).isEqualTo(BigDecimal.ZERO);
            assertThat(viewModel.getNetProfit()).isEqualTo(BigDecimal.ZERO);
            assertThat(viewModel.getEstimatedTax()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should initialize with current tax year")
        void shouldInitializeWithCurrentTaxYear() {
            assertThat(viewModel.getCurrentTaxYear()).isNotNull();
            assertThat(viewModel.getCurrentTaxYear().startYear()).isEqualTo(currentTaxYear.startYear());
        }

        @Test
        @DisplayName("should have empty activity list")
        void shouldHaveEmptyActivityList() {
            assertThat(viewModel.getRecentActivity()).isEmpty();
        }

        @Test
        @DisplayName("should have deadlines populated")
        void shouldHaveDeadlinesPopulated() {
            assertThat(viewModel.getDeadlines()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Currency Formatting")
    class CurrencyFormatting {

        @Test
        @DisplayName("should format income with GBP symbol")
        void shouldFormatIncomeWithGbpSymbol() {
            viewModel.setTotalIncome(new BigDecimal("24500.00"));
            assertThat(viewModel.getFormattedIncome()).isEqualTo("£24,500.00");
        }

        @Test
        @DisplayName("should format zero as £0.00")
        void shouldFormatZeroCorrectly() {
            viewModel.setTotalIncome(BigDecimal.ZERO);
            assertThat(viewModel.getFormattedIncome()).isEqualTo("£0.00");
        }

        @Test
        @DisplayName("should format expenses with GBP symbol")
        void shouldFormatExpensesWithGbpSymbol() {
            viewModel.setTotalExpenses(new BigDecimal("8230.50"));
            assertThat(viewModel.getFormattedExpenses()).isEqualTo("£8,230.50");
        }

        @Test
        @DisplayName("should format profit with GBP symbol")
        void shouldFormatProfitWithGbpSymbol() {
            viewModel.setNetProfit(new BigDecimal("16270.00"));
            assertThat(viewModel.getFormattedProfit()).isEqualTo("£16,270.00");
        }

        @Test
        @DisplayName("should format tax with GBP symbol")
        void shouldFormatTaxWithGbpSymbol() {
            viewModel.setEstimatedTax(new BigDecimal("2456.60"));
            assertThat(viewModel.getFormattedTax()).isEqualTo("£2,456.60");
        }
    }

    @Nested
    @DisplayName("Trend Formatting")
    class TrendFormatting {

        @Test
        @DisplayName("should format positive trend with plus sign")
        void shouldFormatPositiveTrendWithPlusSign() {
            viewModel.setIncomeThisMonth(new BigDecimal("2100.00"));
            assertThat(viewModel.getFormattedIncomeTrend()).isEqualTo("+£2,100.00 this month");
        }

        @Test
        @DisplayName("should format zero trend without sign")
        void shouldFormatZeroTrendWithoutSign() {
            viewModel.setIncomeThisMonth(BigDecimal.ZERO);
            assertThat(viewModel.getFormattedIncomeTrend()).isEqualTo("£0.00 this month");
        }

        @Test
        @DisplayName("should format negative trend with minus sign")
        void shouldFormatNegativeTrendWithMinusSign() {
            viewModel.setIncomeThisMonth(new BigDecimal("-500.00"));
            assertThat(viewModel.getFormattedIncomeTrend()).isEqualTo("-£500.00 this month");
        }
    }

    @Nested
    @DisplayName("Net Profit Calculation")
    class NetProfitCalculation {

        @Test
        @DisplayName("should calculate net profit as income minus expenses")
        void shouldCalculateNetProfit() {
            viewModel.setTotalIncome(new BigDecimal("24500.00"));
            viewModel.setTotalExpenses(new BigDecimal("8230.00"));

            assertThat(viewModel.getNetProfit()).isEqualByComparingTo(new BigDecimal("16270.00"));
        }

        @Test
        @DisplayName("should handle negative profit (loss)")
        void shouldHandleNegativeProfit() {
            viewModel.setTotalIncome(new BigDecimal("5000.00"));
            viewModel.setTotalExpenses(new BigDecimal("8000.00"));

            assertThat(viewModel.getNetProfit()).isEqualByComparingTo(new BigDecimal("-3000.00"));
        }
    }

    @Nested
    @DisplayName("Tax Year Progress")
    class TaxYearProgress {

        @Test
        @DisplayName("should calculate progress as percentage of year elapsed")
        void shouldCalculateProgressPercentage() {
            // Given a tax year and current date
            TaxYear taxYear = TaxYear.of(2025);
            viewModel.setCurrentTaxYear(taxYear);

            // Progress should be between 0 and 1
            double progress = viewModel.getYearProgress();
            assertThat(progress).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("should calculate days remaining until end of tax year")
        void shouldCalculateDaysRemaining() {
            TaxYear taxYear = TaxYear.of(2025);
            viewModel.setCurrentTaxYear(taxYear);

            int daysRemaining = viewModel.getDaysRemaining();
            assertThat(daysRemaining).isGreaterThanOrEqualTo(0);
            assertThat(daysRemaining).isLessThanOrEqualTo(366);
        }

        @Test
        @DisplayName("should format year progress text correctly")
        void shouldFormatYearProgressText() {
            TaxYear taxYear = TaxYear.of(2025);
            viewModel.setCurrentTaxYear(taxYear);

            String progressText = viewModel.getYearProgressText();
            assertThat(progressText).matches("\\d+ days remaining");
        }
    }

    @Nested
    @DisplayName("Deadlines")
    class Deadlines {

        @Test
        @DisplayName("should include online filing deadline")
        void shouldIncludeOnlineFilingDeadline() {
            TaxYear taxYear = TaxYear.of(2025);
            viewModel.setCurrentTaxYear(taxYear);

            assertThat(viewModel.getDeadlines())
                .anyMatch(d -> d.label().contains("Filing") && d.date().equals(taxYear.onlineFilingDeadline()));
        }

        @Test
        @DisplayName("should include payment deadline")
        void shouldIncludePaymentDeadline() {
            TaxYear taxYear = TaxYear.of(2025);
            viewModel.setCurrentTaxYear(taxYear);

            assertThat(viewModel.getDeadlines())
                .anyMatch(d -> d.label().contains("Payment") && d.date().equals(taxYear.paymentDeadline()));
        }

        @Test
        @DisplayName("should calculate deadline status based on days remaining")
        void shouldCalculateDeadlineStatus() {
            TaxYear taxYear = TaxYear.of(2025);
            viewModel.setCurrentTaxYear(taxYear);

            viewModel.getDeadlines().forEach(deadline -> {
                assertThat(deadline.status()).isNotNull();
            });
        }
    }

    @Nested
    @DisplayName("Tax Year Change")
    class TaxYearChange {

        @Test
        @DisplayName("should update all values when tax year changes")
        void shouldUpdateValuesOnTaxYearChange() {
            TaxYear oldYear = TaxYear.of(2024);
            TaxYear newYear = TaxYear.of(2025);

            viewModel.setCurrentTaxYear(oldYear);
            viewModel.setCurrentTaxYear(newYear);

            assertThat(viewModel.getCurrentTaxYear().startYear()).isEqualTo(2025);
        }

        @Test
        @DisplayName("should update deadlines when tax year changes")
        void shouldUpdateDeadlinesOnTaxYearChange() {
            TaxYear year2024 = TaxYear.of(2024);
            TaxYear year2025 = TaxYear.of(2025);

            viewModel.setCurrentTaxYear(year2024);
            LocalDate deadline2024 = viewModel.getDeadlines().get(0).date();

            viewModel.setCurrentTaxYear(year2025);
            LocalDate deadline2025 = viewModel.getDeadlines().get(0).date();

            assertThat(deadline2025).isAfter(deadline2024);
        }
    }
}
