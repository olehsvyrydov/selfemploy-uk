package uk.selfemploy.ui.service.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AbstractSubmissionStrategy.
 *
 * <p>Verifies the shared expense mapping logic extracted per Rev's code review
 * suggestion to reduce duplication between PeriodSubmissionStrategy and
 * CumulativeSubmissionStrategy.</p>
 */
@DisplayName("AbstractSubmissionStrategy Tests")
class AbstractSubmissionStrategyTest {

    private PeriodSubmissionStrategy strategy;
    private TaxYear taxYear;

    @BeforeEach
    void setUp() {
        strategy = new PeriodSubmissionStrategy();
        taxYear = TaxYear.of(2024);
    }

    @Nested
    @DisplayName("mapExpenses() - Expense Category Mapping")
    class MapExpensesTests {

        @Test
        @DisplayName("should map all SA103 expense categories correctly")
        void shouldMapAllSa103Categories() {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.COST_OF_GOODS, new CategorySummary(new BigDecimal("100.00"), 1));
            expenses.put(ExpenseCategory.SUBCONTRACTOR_COSTS, new CategorySummary(new BigDecimal("200.00"), 2));
            expenses.put(ExpenseCategory.STAFF_COSTS, new CategorySummary(new BigDecimal("300.00"), 3));
            expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("50.00"), 1));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySummary(new BigDecimal("30.00"), 1));
            expenses.put(ExpenseCategory.PREMISES, new CategorySummary(new BigDecimal("400.00"), 4));
            expenses.put(ExpenseCategory.REPAIRS, new CategorySummary(new BigDecimal("150.00"), 2));
            expenses.put(ExpenseCategory.OFFICE_COSTS, new CategorySummary(new BigDecimal("75.00"), 3));
            expenses.put(ExpenseCategory.ADVERTISING, new CategorySummary(new BigDecimal("120.00"), 2));
            expenses.put(ExpenseCategory.BUSINESS_ENTERTAINMENT, new CategorySummary(new BigDecimal("60.00"), 1));
            expenses.put(ExpenseCategory.INTEREST, new CategorySummary(new BigDecimal("90.00"), 1));
            expenses.put(ExpenseCategory.FINANCIAL_CHARGES, new CategorySummary(new BigDecimal("25.00"), 1));
            expenses.put(ExpenseCategory.BAD_DEBTS, new CategorySummary(new BigDecimal("500.00"), 1));
            expenses.put(ExpenseCategory.PROFESSIONAL_FEES, new CategorySummary(new BigDecimal("350.00"), 2));
            expenses.put(ExpenseCategory.DEPRECIATION, new CategorySummary(new BigDecimal("200.00"), 1));
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySummary(new BigDecimal("45.00"), 1));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySummary(new BigDecimal("55.00"), 1));

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.costOfGoodsBought()).isEqualByComparingTo("100.00");
            assertThat(mapped.cisPaymentsToSubcontractors()).isEqualByComparingTo("200.00");
            assertThat(mapped.staffCosts()).isEqualByComparingTo("300.00");
            // Travel + Travel Mileage combined
            assertThat(mapped.travelCosts()).isEqualByComparingTo("80.00");
            assertThat(mapped.premisesRunningCosts()).isEqualByComparingTo("400.00");
            assertThat(mapped.maintenanceCosts()).isEqualByComparingTo("150.00");
            assertThat(mapped.adminCosts()).isEqualByComparingTo("75.00");
            assertThat(mapped.advertisingCosts()).isEqualByComparingTo("120.00");
            assertThat(mapped.businessEntertainmentCosts()).isEqualByComparingTo("60.00");
            assertThat(mapped.interest()).isEqualByComparingTo("90.00");
            assertThat(mapped.financialCharges()).isEqualByComparingTo("25.00");
            assertThat(mapped.badDebt()).isEqualByComparingTo("500.00");
            assertThat(mapped.professionalFees()).isEqualByComparingTo("350.00");
            assertThat(mapped.depreciation()).isEqualByComparingTo("200.00");
            // Other + Home Office Simplified combined
            assertThat(mapped.other()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("should return zeros for empty expense map")
        void shouldReturnZerosForEmptyExpenseMap() {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.costOfGoodsBought()).isEqualByComparingTo("0");
            assertThat(mapped.staffCosts()).isEqualByComparingTo("0");
            assertThat(mapped.travelCosts()).isEqualByComparingTo("0");
            assertThat(mapped.other()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should return zeros for null expense map")
        void shouldReturnZerosForNullExpenseMap() {
            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(null);

            assertThat(mapped.costOfGoodsBought()).isEqualByComparingTo("0");
            assertThat(mapped.staffCosts()).isEqualByComparingTo("0");
            assertThat(mapped.travelCosts()).isEqualByComparingTo("0");
            assertThat(mapped.other()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should combine Travel and Travel Mileage into travelCosts")
        void shouldCombineTravelAndTravelMileage() {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("100.00"), 5));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySummary(new BigDecimal("75.00"), 10));

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.travelCosts()).isEqualByComparingTo("175.00");
        }

        @Test
        @DisplayName("should combine Other Expenses and Home Office Simplified into other")
        void shouldCombineOtherAndHomeOfficeSimpified() {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySummary(new BigDecimal("250.00"), 3));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySummary(new BigDecimal("150.00"), 1));

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.other()).isEqualByComparingTo("400.00");
        }

        @Test
        @DisplayName("should handle partial expense categories")
        void shouldHandlePartialExpenseCategories() {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.PROFESSIONAL_FEES, new CategorySummary(new BigDecimal("500.00"), 2));
            expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("200.00"), 5));
            // Other categories not set

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.professionalFees()).isEqualByComparingTo("500.00");
            assertThat(mapped.travelCosts()).isEqualByComparingTo("200.00");
            assertThat(mapped.costOfGoodsBought()).isEqualByComparingTo("0");
            assertThat(mapped.staffCosts()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("Consistency Between Strategies")
    class StrategyConsistencyTests {

        @Test
        @DisplayName("PeriodSubmissionStrategy and CumulativeSubmissionStrategy should produce same expense mapping")
        void shouldProduceSameExpenseMapping() {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.COST_OF_GOODS, new CategorySummary(new BigDecimal("100.00"), 1));
            expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("50.00"), 1));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySummary(new BigDecimal("30.00"), 1));
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySummary(new BigDecimal("45.00"), 1));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySummary(new BigDecimal("55.00"), 1));

            PeriodSubmissionStrategy periodStrategy = new PeriodSubmissionStrategy();
            CumulativeSubmissionStrategy cumulativeStrategy = new CumulativeSubmissionStrategy();

            AbstractSubmissionStrategy.MappedExpenses periodMapped = periodStrategy.mapExpenses(expenses);
            AbstractSubmissionStrategy.MappedExpenses cumulativeMapped = cumulativeStrategy.mapExpenses(expenses);

            // Both strategies should produce identical mappings
            assertThat(periodMapped.costOfGoodsBought()).isEqualByComparingTo(cumulativeMapped.costOfGoodsBought());
            assertThat(periodMapped.travelCosts()).isEqualByComparingTo(cumulativeMapped.travelCosts());
            assertThat(periodMapped.other()).isEqualByComparingTo(cumulativeMapped.other());
        }
    }
}
