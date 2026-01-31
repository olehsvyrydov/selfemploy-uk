package uk.selfemploy.ui.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.service.submission.CumulativeSubmissionStrategy;
import uk.selfemploy.ui.service.submission.PeriodSubmissionStrategy;
import uk.selfemploy.ui.service.submission.SubmissionStrategy;
import uk.selfemploy.ui.service.submission.SubmissionStrategyFactory;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the Cumulative Endpoint Support feature (v5.0).
 *
 * <p>Tests the end-to-end submission flow for tax years 2025-26 onwards,
 * verifying correct strategy selection, endpoint URL generation, and
 * request serialization.</p>
 *
 * <h3>Test Scenarios (designed by /rob):</h3>
 * <ul>
 *   <li>E2E-CUM-001: Happy Path - Tax Year 2025-26 Submission</li>
 *   <li>E2E-CUM-002: Tax Year 2024-25 Uses Period Endpoint</li>
 *   <li>E2E-CUM-003: Error Handling - Validation Error (422)</li>
 *   <li>E2E-CUM-004: Error Handling - Missing taxYear Parameter (400)</li>
 *   <li>E2E-CUM-005: Far Future Tax Year (2030) Uses Cumulative</li>
 * </ul>
 *
 * @see CumulativeSubmissionStrategy
 * @see PeriodSubmissionStrategy
 */
@Tag("cumulative")
@DisplayName("Integration: Cumulative Endpoint Submission Strategy")
class CumulativeSubmissionE2ETest {

    private SubmissionStrategyFactory factory;

    private static final String BASE_URL = "https://test-api.service.hmrc.gov.uk";
    private static final String NINO = "QQ123456C";
    private static final String BUSINESS_ID = "XAIS12345678901";

    @BeforeEach
    void setUp() {
        factory = new SubmissionStrategyFactory();
    }

    @Nested
    @DisplayName("E2E-CUM-001: Happy Path - Tax Year 2025-26 Submission")
    class HappyPath202526Tests {

        @Test
        @DisplayName("should select CumulativeSubmissionStrategy for tax year 2025-26")
        void shouldSelectCumulativeStrategy() {
            TaxYear taxYear = TaxYear.of(2025);

            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy).isInstanceOf(CumulativeSubmissionStrategy.class);
        }

        @Test
        @DisplayName("should use PUT HTTP method for cumulative endpoint")
        void shouldUsePutMethod() {
            TaxYear taxYear = TaxYear.of(2025);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy.getHttpMethod()).isEqualTo("PUT");
        }

        @Test
        @DisplayName("should build correct endpoint URL with taxYear query parameter")
        void shouldBuildCorrectEndpointUrl() {
            TaxYear taxYear = TaxYear.of(2025);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            String url = strategy.buildEndpointUrl(BASE_URL, NINO, BUSINESS_ID, taxYear);

            assertThat(url).isEqualTo(BASE_URL + "/individuals/business/self-employment/"
                    + NINO + "/" + BUSINESS_ID + "/cumulative?taxYear=2025-26");
        }

        @Test
        @DisplayName("should serialize request WITHOUT periodDates wrapper")
        void shouldSerializeWithoutPeriodDates() throws Exception {
            TaxYear taxYear = TaxYear.of(2025);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);
            QuarterlyReviewData reviewData = createReviewData(taxYear, Quarter.Q1);

            String json = strategy.serializeRequest(reviewData);

            // Cumulative endpoint does NOT have periodDates wrapper
            assertThat(json).doesNotContain("\"periodDates\"");
            assertThat(json).doesNotContain("\"periodStartDate\"");
            assertThat(json).doesNotContain("\"periodEndDate\"");
            // But should have income and expenses at root level
            assertThat(json).contains("\"periodIncome\"");
            assertThat(json).contains("\"periodExpenses\"");
            assertThat(json).contains("\"turnover\"");
        }

        @Test
        @DisplayName("should map expense categories correctly in cumulative request")
        void shouldMapExpenseCategoriesCorrectly() throws Exception {
            TaxYear taxYear = TaxYear.of(2025);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.COST_OF_GOODS, new CategorySummary(new BigDecimal("100.00"), 1));
            expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("50.00"), 2));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySummary(new BigDecimal("30.00"), 3));

            QuarterlyReviewData reviewData = createReviewDataWithExpenses(taxYear, Quarter.Q1, expenses);

            String json = strategy.serializeRequest(reviewData);

            assertThat(json).contains("\"costOfGoodsBought\"");
            assertThat(json).contains("\"travelCosts\"");
        }
    }

    @Nested
    @DisplayName("E2E-CUM-002: Tax Year 2024-25 Uses Period Endpoint")
    class TaxYear202425Tests {

        @Test
        @DisplayName("should select PeriodSubmissionStrategy for tax year 2024-25")
        void shouldSelectPeriodStrategy() {
            TaxYear taxYear = TaxYear.of(2024);

            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy).isInstanceOf(PeriodSubmissionStrategy.class);
        }

        @Test
        @DisplayName("should use POST HTTP method for period endpoint")
        void shouldUsePostMethod() {
            TaxYear taxYear = TaxYear.of(2024);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy.getHttpMethod()).isEqualTo("POST");
        }

        @Test
        @DisplayName("should build period endpoint URL without taxYear query parameter")
        void shouldBuildPeriodEndpointUrl() {
            TaxYear taxYear = TaxYear.of(2024);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            String url = strategy.buildEndpointUrl(BASE_URL, NINO, BUSINESS_ID, taxYear);

            assertThat(url).isEqualTo(BASE_URL + "/individuals/business/self-employment/"
                    + NINO + "/" + BUSINESS_ID + "/period");
            assertThat(url).doesNotContain("taxYear");
        }

        @Test
        @DisplayName("should serialize request WITH periodDates wrapper")
        void shouldSerializeWithPeriodDates() throws Exception {
            TaxYear taxYear = TaxYear.of(2024);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);
            QuarterlyReviewData reviewData = createReviewData(taxYear, Quarter.Q4);

            String json = strategy.serializeRequest(reviewData);

            // Period endpoint HAS periodDates wrapper
            assertThat(json).contains("\"periodDates\"");
            assertThat(json).contains("\"periodStartDate\"");
            assertThat(json).contains("\"periodEndDate\"");
            assertThat(json).contains("\"periodIncome\"");
            assertThat(json).contains("\"periodExpenses\"");
        }
    }

    @Nested
    @DisplayName("E2E-CUM-003: Error Handling - Validation Error (422)")
    class ValidationError422Tests {

        @Test
        @DisplayName("should handle null reviewData gracefully")
        void shouldHandleNullReviewData() {
            TaxYear taxYear = TaxYear.of(2025);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            try {
                strategy.serializeRequest(null);
                assertThat(false).as("Expected IllegalArgumentException").isTrue();
            } catch (Exception e) {
                assertThat(e).isInstanceOf(IllegalArgumentException.class);
                assertThat(e.getMessage()).contains("reviewData");
            }
        }

        @Test
        @DisplayName("should serialize zero amounts correctly for nil return")
        void shouldSerializeZeroAmountsForNilReturn() throws Exception {
            TaxYear taxYear = TaxYear.of(2025);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);
            QuarterlyReviewData nilData = createReviewData(taxYear, Quarter.Q1,
                    BigDecimal.ZERO, BigDecimal.ZERO);

            String json = strategy.serializeRequest(nilData);

            assertThat(json).contains("\"periodIncome\"");
            assertThat(json).contains("\"turnover\":0");
        }
    }

    @Nested
    @DisplayName("E2E-CUM-004: Error Handling - Missing taxYear Parameter (400)")
    class MissingTaxYearTests {

        @Test
        @DisplayName("should default to period strategy when tax year is null")
        void shouldDefaultToPeriodStrategyWhenTaxYearNull() {
            SubmissionStrategy strategy = factory.getStrategy(null);

            assertThat(strategy).isInstanceOf(PeriodSubmissionStrategy.class);
            assertThat(strategy.getHttpMethod()).isEqualTo("POST");
        }

        @Test
        @DisplayName("should build period endpoint URL when tax year is null")
        void shouldBuildPeriodEndpointWhenTaxYearNull() {
            SubmissionStrategy strategy = factory.getStrategy(null);

            String url = strategy.buildEndpointUrl(BASE_URL, NINO, BUSINESS_ID, null);

            // Should default to period endpoint
            assertThat(url).endsWith("/period");
        }
    }

    @Nested
    @DisplayName("E2E-CUM-005: Far Future Tax Year (2030) Uses Cumulative")
    class FarFutureTaxYearTests {

        @Test
        @DisplayName("should select CumulativeSubmissionStrategy for tax year 2030")
        void shouldSelectCumulativeStrategyForFarFuture() {
            TaxYear taxYear = TaxYear.of(2030);

            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy).isInstanceOf(CumulativeSubmissionStrategy.class);
        }

        @Test
        @DisplayName("should build correct endpoint URL for far future tax year")
        void shouldBuildCorrectEndpointUrlForFarFuture() {
            TaxYear taxYear = TaxYear.of(2030);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            String url = strategy.buildEndpointUrl(BASE_URL, NINO, BUSINESS_ID, taxYear);

            assertThat(url).isEqualTo(BASE_URL + "/individuals/business/self-employment/"
                    + NINO + "/" + BUSINESS_ID + "/cumulative?taxYear=2030-31");
        }

        @Test
        @DisplayName("should use PUT method for far future tax years")
        void shouldUsePutForFarFuture() {
            TaxYear taxYear = TaxYear.of(2030);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy.getHttpMethod()).isEqualTo("PUT");
        }

        @Test
        @DisplayName("should serialize request without periodDates for far future tax year")
        void shouldSerializeWithoutPeriodDatesForFarFuture() throws Exception {
            TaxYear taxYear = TaxYear.of(2030);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);
            QuarterlyReviewData reviewData = createReviewData(taxYear, Quarter.Q2);

            String json = strategy.serializeRequest(reviewData);

            assertThat(json).doesNotContain("\"periodDates\"");
            assertThat(json).contains("\"periodIncome\"");
        }
    }

    @Nested
    @DisplayName("Tax Year Boundary Tests")
    class TaxYearBoundaryTests {

        @Test
        @DisplayName("tax year 2024 should use period endpoint (last year)")
        void shouldUsePeriodFor2024() {
            TaxYear taxYear = TaxYear.of(2024);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy).isInstanceOf(PeriodSubmissionStrategy.class);
        }

        @Test
        @DisplayName("tax year 2025 should use cumulative endpoint (first year)")
        void shouldUseCumulativeFor2025() {
            TaxYear taxYear = TaxYear.of(2025);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy).isInstanceOf(CumulativeSubmissionStrategy.class);
        }

        @Test
        @DisplayName("tax year 2017 should use period endpoint (earliest supported)")
        void shouldUsePeriodFor2017() {
            TaxYear taxYear = TaxYear.of(2017);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy).isInstanceOf(PeriodSubmissionStrategy.class);
        }
    }

    // ==================== Helper Methods ====================

    private QuarterlyReviewData createReviewData(TaxYear taxYear, Quarter quarter) {
        return createReviewData(taxYear, quarter, new BigDecimal("5000.00"), new BigDecimal("800.00"));
    }

    private QuarterlyReviewData createReviewData(TaxYear taxYear, Quarter quarter,
                                                  BigDecimal income, BigDecimal expenses) {
        return QuarterlyReviewData.builder()
                .quarter(quarter)
                .taxYear(taxYear)
                .periodStart(quarter.getStartDate(taxYear))
                .periodEnd(quarter.getEndDate(taxYear))
                .totalIncome(income)
                .incomeTransactionCount(5)
                .totalExpenses(expenses)
                .expenseTransactionCount(3)
                .expensesByCategory(new EnumMap<>(ExpenseCategory.class))
                .build();
    }

    private QuarterlyReviewData createReviewDataWithExpenses(TaxYear taxYear, Quarter quarter,
                                                              Map<ExpenseCategory, CategorySummary> expenses) {
        BigDecimal totalExpenses = expenses.values().stream()
                .map(CategorySummary::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return QuarterlyReviewData.builder()
                .quarter(quarter)
                .taxYear(taxYear)
                .periodStart(quarter.getStartDate(taxYear))
                .periodEnd(quarter.getEndDate(taxYear))
                .totalIncome(new BigDecimal("10000.00"))
                .incomeTransactionCount(10)
                .totalExpenses(totalExpenses)
                .expenseTransactionCount(expenses.size())
                .expensesByCategory(expenses)
                .build();
    }
}
