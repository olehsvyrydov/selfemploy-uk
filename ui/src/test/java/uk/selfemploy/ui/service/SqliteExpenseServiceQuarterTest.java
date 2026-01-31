package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.exception.ValidationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for quarter-based methods in SqliteExpenseService.
 * SE-10G-003: Verifies that findByQuarter() and getTotalsByCategoryByQuarter()
 * are properly overridden and don't fall through to parent's null repository.
 */
@DisplayName("SqliteExpenseService Quarter Methods")
class SqliteExpenseServiceQuarterTest {

    private SqliteExpenseService service;
    private UUID businessId;
    private TaxYear taxYear;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        service = new SqliteExpenseService(businessId);
        taxYear = TaxYear.of(2025); // 2025/26 tax year
    }

    @Nested
    @DisplayName("findByQuarter")
    class FindByQuarterTests {

        @Test
        @DisplayName("should return empty list when no expenses exist for quarter")
        void shouldReturnEmptyListWhenNoExpenses() {
            List<Expense> result = service.findByQuarter(businessId, taxYear, Quarter.Q1);
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should return expenses within quarter date range")
        void shouldReturnExpensesWithinQuarter() {
            // Create an expense in Q1 (Apr 6 - Jul 5)
            service.create(businessId,
                    Quarter.Q1.getStartDate(taxYear).plusDays(1),
                    new BigDecimal("100.00"),
                    "Q1 expense",
                    ExpenseCategory.OFFICE_COSTS,
                    null, null);

            List<Expense> result = service.findByQuarter(businessId, taxYear, Quarter.Q1);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).description()).isEqualTo("Q1 expense");
        }

        @Test
        @DisplayName("should not return expenses from different quarter")
        void shouldNotReturnExpensesFromDifferentQuarter() {
            // Create expense in Q1
            service.create(businessId,
                    Quarter.Q1.getStartDate(taxYear).plusDays(1),
                    new BigDecimal("100.00"),
                    "Q1 expense",
                    ExpenseCategory.OFFICE_COSTS,
                    null, null);

            // Query Q2 - should be empty
            List<Expense> result = service.findByQuarter(businessId, taxYear, Quarter.Q2);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdNull() {
            assertThatThrownBy(() -> service.findByQuarter(null, taxYear, Quarter.Q1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("should throw ValidationException when taxYear is null")
        void shouldThrowWhenTaxYearNull() {
            assertThatThrownBy(() -> service.findByQuarter(businessId, null, Quarter.Q1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("should throw ValidationException when quarter is null")
        void shouldThrowWhenQuarterNull() {
            assertThatThrownBy(() -> service.findByQuarter(businessId, taxYear, null))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("getTotalsByCategoryByQuarter")
    class GetTotalsByCategoryByQuarterTests {

        @Test
        @DisplayName("should return empty map when no expenses exist")
        void shouldReturnEmptyMapWhenNoExpenses() {
            Map<ExpenseCategory, BigDecimal> result =
                    service.getTotalsByCategoryByQuarter(businessId, taxYear, Quarter.Q1);
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should return category totals for expenses in quarter")
        void shouldReturnCategoryTotalsForQuarter() {
            // Create expenses in Q1
            service.create(businessId,
                    Quarter.Q1.getStartDate(taxYear).plusDays(1),
                    new BigDecimal("50.00"),
                    "Office supplies",
                    ExpenseCategory.OFFICE_COSTS,
                    null, null);

            service.create(businessId,
                    Quarter.Q1.getStartDate(taxYear).plusDays(2),
                    new BigDecimal("30.00"),
                    "More supplies",
                    ExpenseCategory.OFFICE_COSTS,
                    null, null);

            Map<ExpenseCategory, BigDecimal> result =
                    service.getTotalsByCategoryByQuarter(businessId, taxYear, Quarter.Q1);
            assertThat(result).containsKey(ExpenseCategory.OFFICE_COSTS);
            assertThat(result.get(ExpenseCategory.OFFICE_COSTS))
                    .isEqualByComparingTo(new BigDecimal("80.00"));
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdNull() {
            assertThatThrownBy(() ->
                    service.getTotalsByCategoryByQuarter(null, taxYear, Quarter.Q1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("should throw ValidationException when taxYear is null")
        void shouldThrowWhenTaxYearNull() {
            assertThatThrownBy(() ->
                    service.getTotalsByCategoryByQuarter(businessId, null, Quarter.Q1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("should throw ValidationException when quarter is null")
        void shouldThrowWhenQuarterNull() {
            assertThatThrownBy(() ->
                    service.getTotalsByCategoryByQuarter(businessId, taxYear, null))
                    .isInstanceOf(ValidationException.class);
        }
    }
}
