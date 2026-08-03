package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a quarter can be built from.
 *
 * <p>This carries the figures filed to HMRC for a quarterly update, so refusing to build is not a
 * safe failure — it is a quarter that cannot be reported.
 */
@DisplayName("Quarterly review data")
class QuarterlyReviewDataTest {

    private static QuarterlyReviewData.Builder aQuarter() {
        return QuarterlyReviewData.builder()
                .quarter(Quarter.Q1)
                .taxYear(TaxYear.of(2025))
                .periodStart(LocalDate.of(2025, 4, 6))
                .periodEnd(LocalDate.of(2025, 7, 5))
                .totalIncome(new BigDecimal("1000.00"))
                .totalExpenses(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("a quarter with no expenses builds, whatever empty map it is given")
    void shouldBuildANilQuarterFromAnyEmptyMap() {
        // Map.of() is not an EnumMap, and EnumMap's copy constructor rejects an empty map that is
        // not one — it cannot infer the key type. A nil quarter is an ordinary thing to report.
        QuarterlyReviewData fromImmutable = aQuarter().expensesByCategory(Map.of()).build();

        assertThat(fromImmutable.getExpensesByCategory()).isEmpty();
        assertThat(fromImmutable.getTotalExpenses()).isZero();
    }

    @Test
    @DisplayName("a quarter with no expense map at all builds")
    void shouldBuildWithoutAnExpenseMap() {
        assertThat(aQuarter().build().getExpensesByCategory()).isEmpty();
    }

    @Test
    @DisplayName("the categories given are the categories reported")
    void shouldKeepTheCategoriesItWasGiven() {
        QuarterlyReviewData data = aQuarter()
                .expensesByCategory(Map.of(
                        ExpenseCategory.OFFICE_COSTS, new CategorySummary(new BigDecimal("120.00"), 2),
                        ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("30.00"), 1)))
                .totalExpenses(new BigDecimal("150.00"))
                .build();

        assertThat(data.getExpensesByCategory())
                .containsOnlyKeys(ExpenseCategory.OFFICE_COSTS, ExpenseCategory.TRAVEL);
        assertThat(data.getExpensesByCategory().get(ExpenseCategory.OFFICE_COSTS).amount())
                .isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(data.getExpensesByCategory().get(ExpenseCategory.OFFICE_COSTS).transactionCount())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("the map handed to the builder cannot be changed underneath the quarter")
    void shouldNotShareTheCallersMap() {
        Map<ExpenseCategory, CategorySummary> callers = new java.util.HashMap<>();
        callers.put(ExpenseCategory.OFFICE_COSTS, new CategorySummary(new BigDecimal("10.00"), 1));

        QuarterlyReviewData data = aQuarter().expensesByCategory(callers).build();
        callers.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("999.00"), 9));

        assertThat(data.getExpensesByCategory())
                .as("a quarter whose figures change after it was built is a submission that no "
                    + "longer matches what was reviewed")
                .containsOnlyKeys(ExpenseCategory.OFFICE_COSTS);
    }
}
