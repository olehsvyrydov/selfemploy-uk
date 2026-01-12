package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TaxSummaryViewModel.
 */
@DisplayName("TaxSummaryViewModel")
class TaxSummaryViewModelTest {

    private TaxSummaryViewModel viewModel;

    @BeforeEach
    void setup() {
        viewModel = new TaxSummaryViewModel();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should initialize with zero values")
        void shouldInitializeWithZeroValues() {
            assertThat(viewModel.getTurnover()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getNetProfit()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getIncomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getNiClass4()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(viewModel.getTotalTax()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should be draft status initially")
        void shouldBeDraftStatusInitially() {
            assertThat(viewModel.isDraft()).isTrue();
        }

        @Test
        @DisplayName("should not require payment on account initially")
        void shouldNotRequirePaymentOnAccountInitially() {
            assertThat(viewModel.requiresPaymentOnAccount()).isFalse();
        }
    }

    @Nested
    @DisplayName("SA103 Box Mappings")
    class Sa103BoxMappings {

        @Test
        @DisplayName("should show Box 15 for turnover")
        void shouldShowBox15ForTurnover() {
            viewModel.setTurnover(new BigDecimal("50000"));

            assertThat(viewModel.getTurnoverBoxNumber()).isEqualTo("15");
        }

        @Test
        @DisplayName("should show Box 31 for net profit")
        void shouldShowBox31ForNetProfit() {
            assertThat(viewModel.getNetProfitBoxNumber()).isEqualTo("31");
        }
    }

    @Nested
    @DisplayName("Expense Breakdown by Category")
    class ExpenseBreakdownByCategory {

        @Test
        @DisplayName("should track expenses by SA103 category")
        void shouldTrackExpensesBySa103Category() {
            // Given
            viewModel.addExpenseByCategory(ExpenseCategory.OFFICE_COSTS, new BigDecimal("500"));
            viewModel.addExpenseByCategory(ExpenseCategory.TRAVEL, new BigDecimal("1200"));
            viewModel.addExpenseByCategory(ExpenseCategory.PROFESSIONAL_FEES, new BigDecimal("800"));

            // When
            Map<ExpenseCategory, BigDecimal> breakdown = viewModel.getExpenseBreakdown();

            // Then
            assertThat(breakdown.get(ExpenseCategory.OFFICE_COSTS))
                .isEqualByComparingTo(new BigDecimal("500"));
            assertThat(breakdown.get(ExpenseCategory.TRAVEL))
                .isEqualByComparingTo(new BigDecimal("1200"));
            assertThat(breakdown.get(ExpenseCategory.PROFESSIONAL_FEES))
                .isEqualByComparingTo(new BigDecimal("800"));
        }

        @Test
        @DisplayName("should accumulate expenses in same category")
        void shouldAccumulateExpensesInSameCategory() {
            // Given
            viewModel.addExpenseByCategory(ExpenseCategory.OFFICE_COSTS, new BigDecimal("100"));
            viewModel.addExpenseByCategory(ExpenseCategory.OFFICE_COSTS, new BigDecimal("200"));
            viewModel.addExpenseByCategory(ExpenseCategory.OFFICE_COSTS, new BigDecimal("150"));

            // When
            Map<ExpenseCategory, BigDecimal> breakdown = viewModel.getExpenseBreakdown();

            // Then
            assertThat(breakdown.get(ExpenseCategory.OFFICE_COSTS))
                .isEqualByComparingTo(new BigDecimal("450"));
        }

        @Test
        @DisplayName("should update total expenses when adding by category")
        void shouldUpdateTotalExpensesWhenAddingByCategory() {
            // Given
            viewModel.addExpenseByCategory(ExpenseCategory.TRAVEL, new BigDecimal("1000"));
            viewModel.addExpenseByCategory(ExpenseCategory.OFFICE_COSTS, new BigDecimal("500"));

            // Then
            assertThat(viewModel.getTotalExpenses())
                .isEqualByComparingTo(new BigDecimal("1500"));
        }

        @Test
        @DisplayName("should exclude non-allowable expenses from total")
        void shouldExcludeNonAllowableExpensesFromTotal() {
            // Given
            viewModel.addExpenseByCategory(ExpenseCategory.OFFICE_COSTS, new BigDecimal("500"));
            viewModel.addExpenseByCategory(ExpenseCategory.DEPRECIATION, new BigDecimal("1000")); // Not allowable
            viewModel.addExpenseByCategory(ExpenseCategory.BUSINESS_ENTERTAINMENT, new BigDecimal("200")); // Not allowable

            // Then
            assertThat(viewModel.getAllowableExpenses())
                .isEqualByComparingTo(new BigDecimal("500"));
        }
    }

    @Nested
    @DisplayName("Tax Calculation")
    class TaxCalculation {

        @Test
        @DisplayName("should calculate tax when calculate method called")
        void shouldCalculateTaxWhenCalculateMethodCalled() {
            // Given
            viewModel.setTurnover(new BigDecimal("50000"));
            viewModel.setTotalExpenses(new BigDecimal("10000"));
            viewModel.setTaxYear(TaxYear.of(2025)); // 2025/26 tax year

            // When
            viewModel.calculateTax();

            // Then
            assertThat(viewModel.getNetProfit())
                .isEqualByComparingTo(new BigDecimal("40000"));
            assertThat(viewModel.getIncomeTax())
                .isGreaterThan(BigDecimal.ZERO);
            assertThat(viewModel.getNiClass4())
                .isGreaterThan(BigDecimal.ZERO);
            assertThat(viewModel.getTotalTax())
                .isEqualByComparingTo(viewModel.getIncomeTax()
                    .add(viewModel.getNiClass4())
                    .add(viewModel.getNiClass2()));
        }

        @Test
        @DisplayName("should calculate zero tax for income below personal allowance")
        void shouldCalculateZeroTaxForIncomeBelowPersonalAllowance() {
            // Given
            viewModel.setTurnover(new BigDecimal("10000"));
            viewModel.setTotalExpenses(new BigDecimal("0"));
            viewModel.setTaxYear(TaxYear.of(2025));

            // When
            viewModel.calculateTax();

            // Then - £10,000 is below personal allowance
            assertThat(viewModel.getIncomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Income Tax Breakdown")
    class IncomeTaxBreakdown {

        @Test
        @DisplayName("should provide personal allowance amount")
        void shouldProvidePersonalAllowanceAmount() {
            // Given
            viewModel.setTurnover(new BigDecimal("50000"));
            viewModel.setTaxYear(TaxYear.of(2025));
            viewModel.calculateTax();

            // Then
            assertThat(viewModel.getPersonalAllowance())
                .isEqualByComparingTo(new BigDecimal("12570")); // 2025/26 PA
        }

        @Test
        @DisplayName("should provide basic rate tax breakdown")
        void shouldProvideBasicRateTaxBreakdown() {
            // Given
            viewModel.setTurnover(new BigDecimal("30000"));
            viewModel.setTaxYear(TaxYear.of(2025));
            viewModel.calculateTax();

            // Then
            assertThat(viewModel.getBasicRateTax()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should provide higher rate tax for high income")
        void shouldProvideHigherRateTaxForHighIncome() {
            // Given
            viewModel.setTurnover(new BigDecimal("60000"));
            viewModel.setTaxYear(TaxYear.of(2025));
            viewModel.calculateTax();

            // Then
            assertThat(viewModel.getHigherRateTax()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("NI Class 4 Breakdown")
    class NiClass4Breakdown {

        @Test
        @DisplayName("should calculate NI above lower profits limit")
        void shouldCalculateNiAboveLowerProfitsLimit() {
            // Given
            viewModel.setTurnover(new BigDecimal("30000"));
            viewModel.setTaxYear(TaxYear.of(2025));
            viewModel.calculateTax();

            // Then
            assertThat(viewModel.getNiClass4()).isGreaterThan(BigDecimal.ZERO);
            assertThat(viewModel.getNiMainRateAmount()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should calculate zero NI below lower profits limit")
        void shouldCalculateZeroNiBelowLowerProfitsLimit() {
            // Given - £10,000 is below LPL (~£12,570)
            viewModel.setTurnover(new BigDecimal("10000"));
            viewModel.setTaxYear(TaxYear.of(2025));
            viewModel.calculateTax();

            // Then
            assertThat(viewModel.getNiClass4()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Payment on Account")
    class PaymentOnAccount {

        @Test
        @DisplayName("should not require POA when total tax under threshold")
        void shouldNotRequirePoaWhenTotalTaxUnderThreshold() {
            // Given
            viewModel.setTurnover(new BigDecimal("15000"));
            viewModel.setTaxYear(TaxYear.of(2025));
            viewModel.calculateTax();

            // Then - Tax below £1,000 threshold
            assertThat(viewModel.requiresPaymentOnAccount()).isFalse();
        }

        @Test
        @DisplayName("should require POA when total tax exceeds threshold")
        void shouldRequirePoaWhenTotalTaxExceedsThreshold() {
            // Given - High income means tax > £1,000
            viewModel.setTurnover(new BigDecimal("50000"));
            viewModel.setTaxYear(TaxYear.of(2025));
            viewModel.calculateTax();

            // Then
            assertThat(viewModel.requiresPaymentOnAccount()).isTrue();
        }

        @Test
        @DisplayName("should calculate 50% payment on account amounts")
        void shouldCalculate50PercentPaymentOnAccountAmounts() {
            // Given
            viewModel.setTurnover(new BigDecimal("50000"));
            viewModel.setTaxYear(TaxYear.of(2025));
            viewModel.calculateTax();

            // Then - POA is 50% of total tax
            BigDecimal expectedPoa = viewModel.getTotalTax()
                .divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
            assertThat(viewModel.getPaymentOnAccountAmount())
                .isEqualByComparingTo(expectedPoa);
        }

        @Test
        @DisplayName("should provide POA due dates")
        void shouldProvidePoaDueDates() {
            // Given
            viewModel.setTaxYear(TaxYear.of(2025));

            // Then - For 2025/26 tax year
            assertThat(viewModel.getFirstPoaDueDate())
                .isEqualTo(LocalDate.of(2027, 1, 31));
            assertThat(viewModel.getSecondPoaDueDate())
                .isEqualTo(LocalDate.of(2027, 7, 31));
        }
    }

    @Nested
    @DisplayName("Draft Status")
    class DraftStatus {

        @Test
        @DisplayName("should be draft until marked as submitted")
        void shouldBeDraftUntilMarkedAsSubmitted() {
            assertThat(viewModel.isDraft()).isTrue();

            viewModel.setSubmitted(true);

            assertThat(viewModel.isDraft()).isFalse();
        }
    }

    @Nested
    @DisplayName("Formatted Values")
    class FormattedValues {

        @Test
        @DisplayName("should format currency values correctly")
        void shouldFormatCurrencyValuesCorrectly() {
            viewModel.setTurnover(new BigDecimal("50000.50"));

            assertThat(viewModel.getFormattedTurnover()).contains("50,000.50");
        }

        @Test
        @DisplayName("should format tax year correctly")
        void shouldFormatTaxYearCorrectly() {
            viewModel.setTaxYear(TaxYear.of(2025));

            assertThat(viewModel.getTaxYearLabel()).isEqualTo("2025/26");
        }
    }
}
