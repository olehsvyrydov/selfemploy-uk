package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.exception.ValidationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for quarter-based methods in SqliteIncomeService.
 * SE-10G-003: Verifies that findByQuarter() and countByQuarter()
 * are properly overridden and don't fall through to parent's null repository.
 */
@DisplayName("SqliteIncomeService Quarter Methods")
class SqliteIncomeServiceQuarterTest {

    private SqliteIncomeService service;
    private UUID businessId;
    private TaxYear taxYear;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        service = new SqliteIncomeService(businessId);
        taxYear = TaxYear.of(2025); // 2025/26 tax year
    }

    @Nested
    @DisplayName("findByQuarter")
    class FindByQuarterTests {

        @Test
        @DisplayName("should return empty list when no income exists for quarter")
        void shouldReturnEmptyListWhenNoIncome() {
            List<Income> result = service.findByQuarter(businessId, taxYear, Quarter.Q1);
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should return income within quarter date range")
        void shouldReturnIncomeWithinQuarter() {
            // Create income in Q1 (Apr 6 - Jul 5)
            service.create(businessId,
                    Quarter.Q1.getStartDate(taxYear).plusDays(1),
                    new BigDecimal("500.00"),
                    "Q1 invoice",
                    IncomeCategory.SALES,
                    null);

            List<Income> result = service.findByQuarter(businessId, taxYear, Quarter.Q1);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).description()).isEqualTo("Q1 invoice");
        }

        @Test
        @DisplayName("should not return income from different quarter")
        void shouldNotReturnIncomeFromDifferentQuarter() {
            // Create income in Q1
            service.create(businessId,
                    Quarter.Q1.getStartDate(taxYear).plusDays(1),
                    new BigDecimal("500.00"),
                    "Q1 invoice",
                    IncomeCategory.SALES,
                    null);

            // Query Q2 - should be empty
            List<Income> result = service.findByQuarter(businessId, taxYear, Quarter.Q2);
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
    @DisplayName("countByQuarter")
    class CountByQuarterTests {

        @Test
        @DisplayName("should return zero when no income exists")
        void shouldReturnZeroWhenNoIncome() {
            int count = service.countByQuarter(businessId, taxYear, Quarter.Q1);
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("should return correct count for quarter")
        void shouldReturnCorrectCount() {
            // Create two income entries in Q1
            service.create(businessId,
                    Quarter.Q1.getStartDate(taxYear).plusDays(1),
                    new BigDecimal("500.00"),
                    "Q1 invoice 1",
                    IncomeCategory.SALES,
                    null);
            service.create(businessId,
                    Quarter.Q1.getStartDate(taxYear).plusDays(2),
                    new BigDecimal("300.00"),
                    "Q1 invoice 2",
                    IncomeCategory.OTHER_INCOME,
                    null);

            int count = service.countByQuarter(businessId, taxYear, Quarter.Q1);
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should not count income from other quarters")
        void shouldNotCountFromOtherQuarters() {
            // Create income in Q1
            service.create(businessId,
                    Quarter.Q1.getStartDate(taxYear).plusDays(1),
                    new BigDecimal("500.00"),
                    "Q1 invoice",
                    IncomeCategory.SALES,
                    null);

            // Count Q2 - should be zero
            int count = service.countByQuarter(businessId, taxYear, Quarter.Q2);
            assertThat(count).isZero();
        }
    }
}
