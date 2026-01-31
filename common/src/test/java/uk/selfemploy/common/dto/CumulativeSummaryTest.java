package uk.selfemploy.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CumulativeSummary DTO used with HMRC MTD v5.0 cumulative endpoint.
 *
 * <p>The cumulative endpoint (PUT /cumulative?taxYear=YYYY-YY) is used for tax years
 * 2025-26 onwards. Unlike the period endpoint, it does NOT use a periodDates wrapper -
 * income and expenses are at the top level.</p>
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-employment-business-api/5.0">
 *     HMRC Self-Employment Business API v5.0</a>
 */
@DisplayName("CumulativeSummary DTO")
class CumulativeSummaryTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerializationTests {

        @Test
        @DisplayName("should serialize without periodDates wrapper")
        void shouldSerializeWithoutPeriodDatesWrapper() throws Exception {
            // Given
            var income = new CumulativeSummary.CumulativeIncome(
                    new BigDecimal("10000.00"),
                    new BigDecimal("500.00")
            );
            var expenses = CumulativeSummary.CumulativeExpenses.builder()
                    .costOfGoodsBought(new BigDecimal("1000.00"))
                    .travelCosts(new BigDecimal("250.00"))
                    .build();
            var summary = new CumulativeSummary(income, expenses);

            // When
            String json = objectMapper.writeValueAsString(summary);

            // Then
            assertThat(json)
                    .doesNotContain("periodDates")
                    .doesNotContain("periodStartDate")
                    .doesNotContain("periodEndDate")
                    .contains("\"periodIncome\"")
                    .contains("\"periodExpenses\"")
                    .contains("\"turnover\":10000.00")
                    .contains("\"other\":500.00");
        }

        @Test
        @DisplayName("should serialize expenses with all HMRC fields")
        void shouldSerializeExpensesWithAllHmrcFields() throws Exception {
            // Given
            var income = CumulativeSummary.CumulativeIncome.ofTurnover(new BigDecimal("5000.00"));
            var expenses = CumulativeSummary.CumulativeExpenses.builder()
                    .costOfGoodsBought(new BigDecimal("100.00"))
                    .cisPaymentsToSubcontractors(new BigDecimal("200.00"))
                    .staffCosts(new BigDecimal("300.00"))
                    .travelCosts(new BigDecimal("400.00"))
                    .premisesRunningCosts(new BigDecimal("500.00"))
                    .maintenanceCosts(new BigDecimal("600.00"))
                    .adminCosts(new BigDecimal("700.00"))
                    .advertisingCosts(new BigDecimal("800.00"))
                    .businessEntertainmentCosts(new BigDecimal("900.00"))
                    .interest(new BigDecimal("1000.00"))
                    .financialCharges(new BigDecimal("1100.00"))
                    .badDebt(new BigDecimal("1200.00"))
                    .professionalFees(new BigDecimal("1300.00"))
                    .depreciation(new BigDecimal("1400.00"))
                    .other(new BigDecimal("1500.00"))
                    .build();
            var summary = new CumulativeSummary(income, expenses);

            // When
            String json = objectMapper.writeValueAsString(summary);

            // Then
            assertThat(json)
                    .contains("\"costOfGoodsBought\"")
                    .contains("\"cisPaymentsToSubcontractors\"")
                    .contains("\"staffCosts\"")
                    .contains("\"travelCosts\"")
                    .contains("\"premisesRunningCosts\"")
                    .contains("\"maintenanceCosts\"")
                    .contains("\"adminCosts\"")
                    .contains("\"advertisingCosts\"")
                    .contains("\"businessEntertainmentCosts\"")
                    .contains("\"interest\"")
                    .contains("\"financialCharges\"")
                    .contains("\"badDebt\"")
                    .contains("\"professionalFees\"")
                    .contains("\"depreciation\"")
                    .contains("\"other\"");
        }

        @Test
        @DisplayName("should deserialize from JSON")
        void shouldDeserializeFromJson() throws Exception {
            // Given
            String json = """
                {
                    "periodIncome": {
                        "turnover": 15000.00,
                        "other": 750.00
                    },
                    "periodExpenses": {
                        "costOfGoodsBought": 2000.00,
                        "staffCosts": 3000.00,
                        "travelCosts": 500.00
                    }
                }
                """;

            // When
            CumulativeSummary summary = objectMapper.readValue(json, CumulativeSummary.class);

            // Then
            assertThat(summary.periodIncome().turnover()).isEqualByComparingTo("15000.00");
            assertThat(summary.periodIncome().other()).isEqualByComparingTo("750.00");
            assertThat(summary.periodExpenses().costOfGoodsBought()).isEqualByComparingTo("2000.00");
            assertThat(summary.periodExpenses().staffCosts()).isEqualByComparingTo("3000.00");
            assertThat(summary.periodExpenses().travelCosts()).isEqualByComparingTo("500.00");
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("should create from PeriodicUpdate converting to flat structure")
        void shouldCreateFromPeriodicUpdate() {
            // Given
            TaxYear taxYear = TaxYear.of(2025);
            Quarter quarter = Quarter.Q1;
            var periodIncome = PeriodicUpdate.PeriodIncome.ofTurnover(new BigDecimal("10000.00"));
            var periodExpenses = PeriodicUpdate.PeriodExpenses.builder()
                    .travelCosts(new BigDecimal("500.00"))
                    .professionalFees(new BigDecimal("1000.00"))
                    .build();
            var periodicUpdate = PeriodicUpdate.forQuarter(taxYear, quarter, periodIncome, periodExpenses);

            // When
            CumulativeSummary summary = CumulativeSummary.fromPeriodicUpdate(periodicUpdate);

            // Then
            assertThat(summary.periodIncome().turnover()).isEqualByComparingTo("10000.00");
            assertThat(summary.periodIncome().other()).isEqualByComparingTo("0");
            assertThat(summary.periodExpenses().travelCosts()).isEqualByComparingTo("500.00");
            assertThat(summary.periodExpenses().professionalFees()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("should create income with turnover only")
        void shouldCreateIncomeWithTurnoverOnly() {
            // When
            var income = CumulativeSummary.CumulativeIncome.ofTurnover(new BigDecimal("25000.00"));

            // Then
            assertThat(income.turnover()).isEqualByComparingTo("25000.00");
            assertThat(income.other()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should create empty expenses")
        void shouldCreateEmptyExpenses() {
            // When
            var expenses = CumulativeSummary.CumulativeExpenses.empty();

            // Then
            assertThat(expenses.costOfGoodsBought()).isEqualByComparingTo("0");
            assertThat(expenses.staffCosts()).isEqualByComparingTo("0");
            assertThat(expenses.travelCosts()).isEqualByComparingTo("0");
            assertThat(expenses.professionalFees()).isEqualByComparingTo("0");
            assertThat(expenses.other()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("Calculations")
    class CalculationTests {

        @Test
        @DisplayName("should calculate total income")
        void shouldCalculateTotalIncome() {
            // Given
            var income = new CumulativeSummary.CumulativeIncome(
                    new BigDecimal("10000.00"),
                    new BigDecimal("2500.00")
            );

            // When
            BigDecimal total = income.calculateTotal();

            // Then
            assertThat(total).isEqualByComparingTo("12500.00");
        }

        @Test
        @DisplayName("should calculate total expenses")
        void shouldCalculateTotalExpenses() {
            // Given
            var expenses = CumulativeSummary.CumulativeExpenses.builder()
                    .costOfGoodsBought(new BigDecimal("1000.00"))
                    .staffCosts(new BigDecimal("2000.00"))
                    .travelCosts(new BigDecimal("500.00"))
                    .depreciation(new BigDecimal("300.00"))
                    .build();

            // When
            BigDecimal total = expenses.calculateTotal();

            // Then
            assertThat(total).isEqualByComparingTo("3800.00");
        }

        @Test
        @DisplayName("should calculate allowable expenses (excluding depreciation and business entertainment)")
        void shouldCalculateAllowableExpenses() {
            // Given
            var expenses = CumulativeSummary.CumulativeExpenses.builder()
                    .costOfGoodsBought(new BigDecimal("1000.00"))
                    .staffCosts(new BigDecimal("2000.00"))
                    .depreciation(new BigDecimal("500.00"))
                    .businessEntertainmentCosts(new BigDecimal("200.00"))
                    .build();

            // When
            BigDecimal allowable = expenses.calculateAllowableTotal();

            // Then
            // Total is 3700, minus depreciation (500) and entertainment (200) = 3000
            assertThat(allowable).isEqualByComparingTo("3000.00");
        }

        @Test
        @DisplayName("should calculate net profit")
        void shouldCalculateNetProfit() {
            // Given
            var income = new CumulativeSummary.CumulativeIncome(
                    new BigDecimal("50000.00"),
                    new BigDecimal("5000.00")
            );
            var expenses = CumulativeSummary.CumulativeExpenses.builder()
                    .costOfGoodsBought(new BigDecimal("10000.00"))
                    .staffCosts(new BigDecimal("15000.00"))
                    .build();
            var summary = new CumulativeSummary(income, expenses);

            // When
            BigDecimal netProfit = summary.calculateNetProfit();

            // Then
            // Income: 55000, Expenses: 25000 = 30000 profit
            assertThat(netProfit).isEqualByComparingTo("30000.00");
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandlingTests {

        @Test
        @DisplayName("should default null income values to zero")
        void shouldDefaultNullIncomeValuesToZero() {
            // When
            var income = new CumulativeSummary.CumulativeIncome(null, null);

            // Then
            assertThat(income.turnover()).isEqualByComparingTo("0");
            assertThat(income.other()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should default null expense values to zero")
        void shouldDefaultNullExpenseValuesToZero() {
            // When - using builder with nulls
            var expenses = new CumulativeSummary.CumulativeExpenses(
                    null, null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null, null
            );

            // Then
            assertThat(expenses.costOfGoodsBought()).isEqualByComparingTo("0");
            assertThat(expenses.staffCosts()).isEqualByComparingTo("0");
            assertThat(expenses.travelCosts()).isEqualByComparingTo("0");
            assertThat(expenses.calculateTotal()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should handle null income in net profit calculation")
        void shouldHandleNullIncomeInNetProfitCalculation() {
            // Given
            var expenses = CumulativeSummary.CumulativeExpenses.builder()
                    .staffCosts(new BigDecimal("1000.00"))
                    .build();
            var summary = new CumulativeSummary(null, expenses);

            // When
            BigDecimal netProfit = summary.calculateNetProfit();

            // Then - 0 income minus 1000 expenses = -1000 loss
            assertThat(netProfit).isEqualByComparingTo("-1000.00");
        }

        @Test
        @DisplayName("should handle null expenses in net profit calculation")
        void shouldHandleNullExpensesInNetProfitCalculation() {
            // Given
            var income = CumulativeSummary.CumulativeIncome.ofTurnover(new BigDecimal("5000.00"));
            var summary = new CumulativeSummary(income, null);

            // When
            BigDecimal netProfit = summary.calculateNetProfit();

            // Then - 5000 income minus 0 expenses = 5000 profit
            assertThat(netProfit).isEqualByComparingTo("5000.00");
        }
    }

    @Nested
    @DisplayName("Comparison with PeriodicUpdate")
    class ComparisonWithPeriodicUpdateTests {

        @Test
        @DisplayName("CumulativeSummary JSON should not have periodDates while PeriodicUpdate should")
        void shouldDifferInPeriodDatesPresence() throws Exception {
            // Given - same financial data
            var periodIncome = PeriodicUpdate.PeriodIncome.ofTurnover(new BigDecimal("10000.00"));
            var periodExpenses = PeriodicUpdate.PeriodExpenses.builder()
                    .travelCosts(new BigDecimal("500.00"))
                    .build();

            TaxYear taxYear = TaxYear.of(2025);
            Quarter quarter = Quarter.Q1;

            // PeriodicUpdate with periodDates
            var periodicUpdate = PeriodicUpdate.forQuarter(taxYear, quarter, periodIncome, periodExpenses);

            // CumulativeSummary without periodDates
            var cumulativeIncome = CumulativeSummary.CumulativeIncome.ofTurnover(new BigDecimal("10000.00"));
            var cumulativeExpenses = CumulativeSummary.CumulativeExpenses.builder()
                    .travelCosts(new BigDecimal("500.00"))
                    .build();
            var cumulativeSummary = new CumulativeSummary(cumulativeIncome, cumulativeExpenses);

            // When
            String periodicJson = objectMapper.writeValueAsString(periodicUpdate);
            String cumulativeJson = objectMapper.writeValueAsString(cumulativeSummary);

            // Then
            assertThat(periodicJson).contains("periodDates");
            assertThat(periodicJson).contains("periodStartDate");
            assertThat(periodicJson).contains("periodEndDate");

            assertThat(cumulativeJson).doesNotContain("periodDates");
            assertThat(cumulativeJson).doesNotContain("periodStartDate");
            assertThat(cumulativeJson).doesNotContain("periodEndDate");
        }
    }
}
