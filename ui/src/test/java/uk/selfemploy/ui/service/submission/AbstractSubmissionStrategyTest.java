package uk.selfemploy.ui.service.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.dto.DisallowableExpenses;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.profit.CategorySpend;
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
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.COST_OF_GOODS, new CategorySpend(new BigDecimal("100.00"), new BigDecimal("100.00")));
            expenses.put(ExpenseCategory.SUBCONTRACTOR_COSTS, new CategorySpend(new BigDecimal("200.00"), new BigDecimal("200.00")));
            expenses.put(ExpenseCategory.STAFF_COSTS, new CategorySpend(new BigDecimal("300.00"), new BigDecimal("300.00")));
            expenses.put(ExpenseCategory.TRAVEL, new CategorySpend(new BigDecimal("50.00"), new BigDecimal("50.00")));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySpend(new BigDecimal("30.00"), new BigDecimal("30.00")));
            expenses.put(ExpenseCategory.PREMISES, new CategorySpend(new BigDecimal("400.00"), new BigDecimal("400.00")));
            expenses.put(ExpenseCategory.REPAIRS, new CategorySpend(new BigDecimal("150.00"), new BigDecimal("150.00")));
            expenses.put(ExpenseCategory.OFFICE_COSTS, new CategorySpend(new BigDecimal("75.00"), new BigDecimal("75.00")));
            expenses.put(ExpenseCategory.ADVERTISING, new CategorySpend(new BigDecimal("120.00"), new BigDecimal("120.00")));
            expenses.put(ExpenseCategory.BUSINESS_ENTERTAINMENT, new CategorySpend(new BigDecimal("60.00"), new BigDecimal("60.00")));
            expenses.put(ExpenseCategory.INTEREST, new CategorySpend(new BigDecimal("90.00"), new BigDecimal("90.00")));
            expenses.put(ExpenseCategory.FINANCIAL_CHARGES, new CategorySpend(new BigDecimal("25.00"), new BigDecimal("25.00")));
            expenses.put(ExpenseCategory.BAD_DEBTS, new CategorySpend(new BigDecimal("500.00"), new BigDecimal("500.00")));
            expenses.put(ExpenseCategory.PROFESSIONAL_FEES, new CategorySpend(new BigDecimal("350.00"), new BigDecimal("350.00")));
            expenses.put(ExpenseCategory.DEPRECIATION, new CategorySpend(new BigDecimal("200.00"), new BigDecimal("200.00")));
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySpend(new BigDecimal("45.00"), new BigDecimal("45.00")));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySpend(new BigDecimal("55.00"), new BigDecimal("55.00")));

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.costOfGoodsBought().spent()).isEqualByComparingTo("100.00");
            assertThat(mapped.cisPaymentsToSubcontractors().spent()).isEqualByComparingTo("200.00");
            assertThat(mapped.staffCosts().spent()).isEqualByComparingTo("300.00");
            // Travel + Travel Mileage combined
            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo("80.00");
            assertThat(mapped.premisesRunningCosts().spent()).isEqualByComparingTo("400.00");
            assertThat(mapped.maintenanceCosts().spent()).isEqualByComparingTo("150.00");
            assertThat(mapped.adminCosts().spent()).isEqualByComparingTo("75.00");
            assertThat(mapped.advertisingCosts().spent()).isEqualByComparingTo("120.00");
            assertThat(mapped.businessEntertainmentCosts().spent()).isEqualByComparingTo("60.00");
            assertThat(mapped.interest().spent()).isEqualByComparingTo("90.00");
            assertThat(mapped.financialCharges().spent()).isEqualByComparingTo("25.00");
            assertThat(mapped.badDebt().spent()).isEqualByComparingTo("500.00");
            assertThat(mapped.professionalFees().spent()).isEqualByComparingTo("350.00");
            assertThat(mapped.depreciation().spent()).isEqualByComparingTo("200.00");
            // Other + Home Office Simplified combined
            assertThat(mapped.other().spent()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("should return zeros for empty expense map")
        void shouldReturnZerosForEmptyExpenseMap() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.costOfGoodsBought().spent()).isEqualByComparingTo("0");
            assertThat(mapped.staffCosts().spent()).isEqualByComparingTo("0");
            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo("0");
            assertThat(mapped.other().spent()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should return zeros for null expense map")
        void shouldReturnZerosForNullExpenseMap() {
            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(null);

            assertThat(mapped.costOfGoodsBought().spent()).isEqualByComparingTo("0");
            assertThat(mapped.staffCosts().spent()).isEqualByComparingTo("0");
            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo("0");
            assertThat(mapped.other().spent()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should combine Travel and Travel Mileage into travelCosts")
        void shouldCombineTravelAndTravelMileage() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.TRAVEL, new CategorySpend(new BigDecimal("100.00"), new BigDecimal("100.00")));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySpend(new BigDecimal("75.00"), new BigDecimal("75.00")));

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo("175.00");
        }

        @Test
        @DisplayName("should combine Other Expenses and Home Office Simplified into other")
        void shouldCombineOtherAndHomeOfficeSimpified() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySpend(new BigDecimal("250.00"), new BigDecimal("250.00")));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySpend(new BigDecimal("150.00"), new BigDecimal("150.00")));

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.other().spent()).isEqualByComparingTo("400.00");
        }

        @Test
        @DisplayName("should handle partial expense categories")
        void shouldHandlePartialExpenseCategories() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.PROFESSIONAL_FEES, new CategorySpend(new BigDecimal("500.00"), new BigDecimal("500.00")));
            expenses.put(ExpenseCategory.TRAVEL, new CategorySpend(new BigDecimal("200.00"), new BigDecimal("200.00")));
            // Other categories not set

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.professionalFees().spent()).isEqualByComparingTo("500.00");
            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo("200.00");
            assertThat(mapped.costOfGoodsBought().spent()).isEqualByComparingTo("0");
            assertThat(mapped.staffCosts().spent()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("disallowableExpenses() - the second SA103F column")
    class DisallowableExpensesTests {

        @Test
        @DisplayName("should carry the part of each category that cannot be claimed")
        void shouldCarryTheUnclaimablePart() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            // Spent in full, claimable for nothing.
            expenses.put(ExpenseCategory.BUSINESS_ENTERTAINMENT,
                    new CategorySpend(new BigDecimal("79.11"), BigDecimal.ZERO));
            // A phone bill marked 60% business use.
            expenses.put(ExpenseCategory.OFFICE_COSTS,
                    new CategorySpend(new BigDecimal("60.00"), new BigDecimal("36.00")));

            DisallowableExpenses disallowable = strategy.disallowableExpenses(strategy.mapExpenses(expenses));

            assertThat(disallowable.businessEntertainmentCosts()).isEqualByComparingTo("79.11");
            assertThat(disallowable.adminCosts()).isEqualByComparingTo("24.00");
        }

        @Test
        @DisplayName("should disallow nothing when every category is claimable in full")
        void shouldDisallowNothingWhenAllClaimable() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.PROFESSIONAL_FEES,
                    new CategorySpend(new BigDecimal("350.00"), new BigDecimal("350.00")));

            DisallowableExpenses disallowable = strategy.disallowableExpenses(strategy.mapExpenses(expenses));

            assertThat(disallowable.calculateTotal()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should aggregate the disallowed part of categories that share one box")
        void shouldAggregateAcrossCategoriesSharingABox() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.TRAVEL,
                    new CategorySpend(new BigDecimal("100.00"), new BigDecimal("70.00")));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE,
                    new CategorySpend(new BigDecimal("50.00"), new BigDecimal("45.00")));

            AbstractSubmissionStrategy.MappedExpenses mapped = strategy.mapExpenses(expenses);

            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo("150.00");
            assertThat(strategy.disallowableExpenses(mapped).travelCosts())
                    .as("both categories land in the same box, so their disallowed parts add up")
                    .isEqualByComparingTo("35.00");
        }
    }

    @Nested
    @DisplayName("Consistency Between Strategies")
    class StrategyConsistencyTests {

        @Test
        @DisplayName("PeriodSubmissionStrategy and CumulativeSubmissionStrategy should produce same expense mapping")
        void shouldProduceSameExpenseMapping() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.COST_OF_GOODS, new CategorySpend(new BigDecimal("100.00"), new BigDecimal("100.00")));
            expenses.put(ExpenseCategory.TRAVEL, new CategorySpend(new BigDecimal("50.00"), new BigDecimal("50.00")));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySpend(new BigDecimal("30.00"), new BigDecimal("30.00")));
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySpend(new BigDecimal("45.00"), new BigDecimal("45.00")));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySpend(new BigDecimal("55.00"), new BigDecimal("55.00")));

            PeriodSubmissionStrategy periodStrategy = new PeriodSubmissionStrategy();
            CumulativeSubmissionStrategy cumulativeStrategy = new CumulativeSubmissionStrategy();

            AbstractSubmissionStrategy.MappedExpenses periodMapped = periodStrategy.mapExpenses(expenses);
            AbstractSubmissionStrategy.MappedExpenses cumulativeMapped = cumulativeStrategy.mapExpenses(expenses);

            // Both strategies should produce identical mappings
            assertThat(periodMapped.costOfGoodsBought().spent()).isEqualByComparingTo(cumulativeMapped.costOfGoodsBought().spent());
            assertThat(periodMapped.travelCosts().spent()).isEqualByComparingTo(cumulativeMapped.travelCosts().spent());
            assertThat(periodMapped.other().spent()).isEqualByComparingTo(cumulativeMapped.other().spent());
        }
    }
}
