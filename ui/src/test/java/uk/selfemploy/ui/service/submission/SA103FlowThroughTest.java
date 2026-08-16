package uk.selfemploy.ui.service.submission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.profit.CategorySpend;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SA103 flow-through tests verifying that expense categories, income categories,
 * and amounts correctly flow from UI aggregation through submission strategies
 * to the HMRC API request format.
 *
 * Tests cover SA103F (full) box numbers 17-30 for expenses, and boxes 9-10 for income.
 * Uses BigDecimal for all amounts to ensure financial precision.
 */
@DisplayName("SA103 Flow-Through Tests")
class SA103FlowThroughTest {

    private PeriodSubmissionStrategy periodStrategy;
    private CumulativeSubmissionStrategy cumulativeStrategy;
    private ObjectMapper objectMapper;

    // Standard test data
    private static final TaxYear TAX_YEAR_2024 = TaxYear.of(2024); // 2024-25
    private static final TaxYear TAX_YEAR_2025 = TaxYear.of(2025); // 2025-26
    private static final Quarter Q1 = Quarter.Q1;

    @BeforeEach
    void setUp() {
        periodStrategy = new PeriodSubmissionStrategy();
        cumulativeStrategy = new CumulativeSubmissionStrategy();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ==================== Helper Methods ====================

    private QuarterlyReviewData buildReviewData(
            BigDecimal totalIncome,
            Map<ExpenseCategory, CategorySpend> expenses,
            BigDecimal totalExpenses) {
        return buildReviewData(totalIncome, expenses, totalExpenses, TAX_YEAR_2024, Q1);
    }

    private QuarterlyReviewData buildReviewData(
            BigDecimal totalIncome,
            Map<ExpenseCategory, CategorySpend> expenses,
            BigDecimal totalExpenses,
            TaxYear taxYear,
            Quarter quarter) {
        return QuarterlyReviewData.builder()
                .quarter(quarter)
                .taxYear(taxYear)
                .periodStart(quarter.getStartDate(taxYear))
                .periodEnd(quarter.getEndDate(taxYear))
                .totalIncome(totalIncome)
                .incomeTransactionCount(1)
                .spendByCategory(expenses)
                .totalExpenses(totalExpenses)
                .expenseTransactionCount(expenses.size())
                .build();
    }

    private Map<ExpenseCategory, CategorySpend> singleCategoryExpense(
            ExpenseCategory category, BigDecimal amount) {
        Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
        expenses.put(category, new CategorySpend(amount, amount));
        return expenses;
    }

    // ==================== Group A: Single-Category Flow-Through ====================

    @Nested
    @DisplayName("Group A: Single-Category Flow-Through (SA103F Boxes 17-30)")
    class GroupA_SingleCategoryFlowThrough {

        @ParameterizedTest(name = "ExpenseCategory.{0} maps to correct SA103F field")
        @MethodSource("uk.selfemploy.ui.service.submission.SA103FlowThroughTest#singleCategoryArguments")
        @DisplayName("should map single expense category to correct SA103F box field")
        void shouldMapSingleExpenseCategoryToCorrectField(
                ExpenseCategory category, String expectedFieldName, String sa103Box) {

            BigDecimal amount = new BigDecimal("150.00");
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(category, amount);

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            BigDecimal actualAmount = getFieldValue(mapped, expectedFieldName);
            assertThat(actualAmount)
                    .as("ExpenseCategory.%s -> SA103F Box %s (%s)", category, sa103Box, expectedFieldName)
                    .isEqualByComparingTo(amount);
        }

        @ParameterizedTest(name = "ExpenseCategory.{0} zeros out other fields")
        @MethodSource("uk.selfemploy.ui.service.submission.SA103FlowThroughTest#singleCategoryArguments")
        @DisplayName("should zero out non-matching fields for single category")
        void shouldZeroOutNonMatchingFields(
                ExpenseCategory category, String expectedFieldName, String sa103Box) {

            BigDecimal amount = new BigDecimal("250.00");
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(category, amount);

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            // Verify all other fields are zero (except for aggregated categories)
            BigDecimal totalMapped = getAllFieldsTotal(mapped);
            BigDecimal expectedTotal = getExpectedTotal(category, amount);

            assertThat(totalMapped)
                    .as("Total of all mapped fields for single category %s", category)
                    .isEqualByComparingTo(expectedTotal);
        }

        @ParameterizedTest(name = "ExpenseCategory.{0} serializes via PeriodSubmissionStrategy")
        @MethodSource("uk.selfemploy.ui.service.submission.SA103FlowThroughTest#singleCategoryArguments")
        @DisplayName("should serialize single category correctly through PeriodSubmissionStrategy")
        void shouldSerializeSingleCategoryViaPeriodStrategy(
                ExpenseCategory category, String expectedFieldName, String sa103Box) throws Exception {

            BigDecimal amount = new BigDecimal("99.99");
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(category, amount);
            QuarterlyReviewData data = buildReviewData(BigDecimal.ZERO, expenses, amount);

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);
            JsonNode expensesNode = root.path("periodExpenses");

            assertThat(expensesNode.isMissingNode())
                    .as("periodExpenses should be present in JSON")
                    .isFalse();
        }

        @ParameterizedTest(name = "ExpenseCategory.{0} serializes via CumulativeSubmissionStrategy")
        @MethodSource("uk.selfemploy.ui.service.submission.SA103FlowThroughTest#singleCategoryArguments")
        @DisplayName("should serialize single category correctly through CumulativeSubmissionStrategy")
        void shouldSerializeSingleCategoryViaCumulativeStrategy(
                ExpenseCategory category, String expectedFieldName, String sa103Box) throws Exception {

            BigDecimal amount = new BigDecimal("77.77");
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(category, amount);
            QuarterlyReviewData data = buildReviewData(
                    BigDecimal.ZERO, expenses, amount, TAX_YEAR_2025, Q1);

            String json = cumulativeStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);
            JsonNode expensesNode = root.path("periodExpenses");

            assertThat(expensesNode.isMissingNode())
                    .as("periodExpenses should be present in JSON")
                    .isFalse();
        }
    }

    // ==================== Group B: Income Flow-Through ====================

    @Nested
    @DisplayName("Group B: Income Flow-Through (SA103F Boxes 9-10)")
    class GroupB_IncomeFlowThrough {

        @Test
        @DisplayName("SALES income should map to turnover field (Box 9)")
        void salesIncomeShouldMapToTurnover() throws Exception {
            BigDecimal income = new BigDecimal("5000.00");
            QuarterlyReviewData data = buildReviewData(
                    income, new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO);

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodIncome").path("turnover").decimalValue())
                    .isEqualByComparingTo(income);
        }

        @Test
        @DisplayName("SALES income should produce zero in other income field")
        void salesIncomeShouldProduceZeroOtherIncome() throws Exception {
            BigDecimal income = new BigDecimal("3000.00");
            QuarterlyReviewData data = buildReviewData(
                    income, new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO);

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodIncome").path("other").decimalValue())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("zero income should produce zero turnover")
        void zeroIncomeShouldProduceZeroTurnover() throws Exception {
            QuarterlyReviewData data = buildReviewData(
                    BigDecimal.ZERO, new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO);

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodIncome").path("turnover").decimalValue())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("income should flow through CumulativeSubmissionStrategy to turnover")
        void incomeShouldFlowThroughCumulativeStrategy() throws Exception {
            BigDecimal income = new BigDecimal("7500.00");
            QuarterlyReviewData data = buildReviewData(
                    income, new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO,
                    TAX_YEAR_2025, Q1);

            String json = cumulativeStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodIncome").path("turnover").decimalValue())
                    .isEqualByComparingTo(income);
        }

        @Test
        @DisplayName("large income amount should serialize with full precision")
        void largeIncomeAmountShouldSerializeWithPrecision() throws Exception {
            BigDecimal income = new BigDecimal("123456.78");
            QuarterlyReviewData data = buildReviewData(
                    income, new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO);

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodIncome").path("turnover").decimalValue())
                    .isEqualByComparingTo(income);
        }
    }

    // ==================== Group C: Multi-Category Aggregation ====================

    @Nested
    @DisplayName("Group C: Multi-Category Aggregation")
    class GroupC_MultiCategoryAggregation {

        @Test
        @DisplayName("TRAVEL + TRAVEL_MILEAGE should aggregate to travelCosts (Box 20)")
        void travelAndMileageShouldAggregate() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.TRAVEL, new CategorySpend(new BigDecimal("200.00"), new BigDecimal("200.00")));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySpend(new BigDecimal("150.00"), new BigDecimal("150.00")));

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo(new BigDecimal("350.00"));
        }

        @Test
        @DisplayName("OTHER_EXPENSES + HOME_OFFICE_SIMPLIFIED should aggregate to other (Box 30)")
        void otherAndHomeOfficeShouldAggregate() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySpend(new BigDecimal("100.00"), new BigDecimal("100.00")));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySpend(new BigDecimal("26.00"), new BigDecimal("26.00")));

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            assertThat(mapped.other().spent()).isEqualByComparingTo(new BigDecimal("126.00"));
        }

        @Test
        @DisplayName("TRAVEL only (without TRAVEL_MILEAGE) should map correctly")
        void travelOnlyShouldMapCorrectly() {
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(
                    ExpenseCategory.TRAVEL, new BigDecimal("300.00"));

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("TRAVEL_MILEAGE only (without TRAVEL) should map correctly")
        void travelMileageOnlyShouldMapCorrectly() {
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(
                    ExpenseCategory.TRAVEL_MILEAGE, new BigDecimal("175.00"));

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo(new BigDecimal("175.00"));
        }

        @Test
        @DisplayName("OTHER_EXPENSES only (without HOME_OFFICE_SIMPLIFIED) should map correctly")
        void otherExpensesOnlyShouldMapCorrectly() {
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(
                    ExpenseCategory.OTHER_EXPENSES, new BigDecimal("50.00"));

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            assertThat(mapped.other().spent()).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("HOME_OFFICE_SIMPLIFIED only (without OTHER_EXPENSES) should map correctly")
        void homeOfficeOnlyShouldMapCorrectly() {
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(
                    ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new BigDecimal("18.00"));

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            assertThat(mapped.other().spent()).isEqualByComparingTo(new BigDecimal("18.00"));
        }

        @Test
        @DisplayName("aggregation should serialize correctly through period strategy")
        void aggregationShouldSerializeThroughPeriodStrategy() throws Exception {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.TRAVEL, new CategorySpend(new BigDecimal("100.00"), new BigDecimal("100.00")));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySpend(new BigDecimal("75.00"), new BigDecimal("75.00")));
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySpend(new BigDecimal("50.00"), new BigDecimal("50.00")));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySpend(new BigDecimal("26.00"), new BigDecimal("26.00")));

            QuarterlyReviewData data = buildReviewData(
                    BigDecimal.ZERO, expenses, new BigDecimal("251.00"));

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);
            JsonNode expNode = root.path("periodExpenses");

            assertThat(expNode.path("carVanTravelExpenses").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("175.00"));
            assertThat(expNode.path("otherExpenses").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("76.00"));
        }

        @Test
        @DisplayName("aggregation should be consistent between Period and Cumulative strategies")
        void aggregationShouldBeConsistentBetweenStrategies() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.TRAVEL, new CategorySpend(new BigDecimal("500.00"), new BigDecimal("500.00")));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySpend(new BigDecimal("300.00"), new BigDecimal("300.00")));
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySpend(new BigDecimal("200.00"), new BigDecimal("200.00")));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySpend(new BigDecimal("78.00"), new BigDecimal("78.00")));

            AbstractSubmissionStrategy.MappedExpenses periodMapped = periodStrategy.mapExpenses(expenses);
            AbstractSubmissionStrategy.MappedExpenses cumulMapped = cumulativeStrategy.mapExpenses(expenses);

            // Both columns, not just the declared spend: the strategies filing the same total while
            // disagreeing about the disallowed part of it would file two different deductions.
            assertThat(periodMapped.travelCosts()).isEqualTo(cumulMapped.travelCosts());
            assertThat(periodMapped.other()).isEqualTo(cumulMapped.other());
        }
    }

    // ==================== Group D: Exclusion/Filtering ====================

    @Nested
    @DisplayName("Group D: Exclusion and Filtering")
    class GroupD_ExclusionFiltering {

        @Test
        @DisplayName("empty expense map should produce all zero fields")
        void emptyExpenseMapShouldProduceAllZeros() {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            assertThat(mapped.costOfGoodsBought().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.cisPaymentsToSubcontractors().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.staffCosts().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.premisesRunningCosts().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.maintenanceCosts().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.adminCosts().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.advertisingCosts().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.businessEntertainmentCosts().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.interest().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.financialCharges().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.badDebt().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.professionalFees().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.depreciation().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.other().spent()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("null expense map should produce all zero fields")
        void nullExpenseMapShouldProduceAllZeros() {
            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(null);

            assertThat(mapped.costOfGoodsBought().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.travelCosts().spent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(mapped.other().spent()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("only allowable categories should be mapped (non-allowable still included)")
        void nonAllowableCategoriesShouldStillBeMapped() {
            // DEPRECIATION and BUSINESS_ENTERTAINMENT are not allowable but still mapped
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.DEPRECIATION, new CategorySpend(new BigDecimal("500.00"), new BigDecimal("500.00")));
            expenses.put(ExpenseCategory.BUSINESS_ENTERTAINMENT, new CategorySpend(new BigDecimal("200.00"), new BigDecimal("200.00")));

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            // They should still appear in the mapping - HMRC needs them for the form
            assertThat(mapped.depreciation().spent()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(mapped.businessEntertainmentCosts().spent()).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("nil return should serialize with zero income and expenses")
        void nilReturnShouldSerializeCorrectly() throws Exception {
            QuarterlyReviewData data = buildReviewData(
                    BigDecimal.ZERO, new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO);

            assertThat(data.isNilReturn()).isTrue();

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodIncome").path("turnover").decimalValue())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ==================== Group E: Non-Allowable Expenses ====================

    @Nested
    @DisplayName("Group E: Non-Allowable Expenses")
    class GroupE_NonAllowableExpenses {

        @Test
        @DisplayName("DEPRECIATION should map to depreciation field (Box 29, not allowable)")
        void depreciationShouldMapCorrectly() {
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(
                    ExpenseCategory.DEPRECIATION, new BigDecimal("1000.00"));

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            assertThat(mapped.depreciation().spent()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("BUSINESS_ENTERTAINMENT should map to businessEntertainmentCosts (not allowable)")
        void businessEntertainmentShouldMapCorrectly() {
            Map<ExpenseCategory, CategorySpend> expenses = singleCategoryExpense(
                    ExpenseCategory.BUSINESS_ENTERTAINMENT, new BigDecimal("350.00"));

            AbstractSubmissionStrategy.MappedExpenses mapped = periodStrategy.mapExpenses(expenses);

            assertThat(mapped.businessEntertainmentCosts().spent()).isEqualByComparingTo(new BigDecimal("350.00"));
        }

        @Test
        @DisplayName("DEPRECIATION isAllowable should return false")
        void depreciationIsNotAllowable() {
            assertThat(ExpenseCategory.DEPRECIATION.isAllowable()).isFalse();
        }

        @Test
        @DisplayName("BUSINESS_ENTERTAINMENT isAllowable should return false")
        void businessEntertainmentIsNotAllowable() {
            assertThat(ExpenseCategory.BUSINESS_ENTERTAINMENT.isAllowable()).isFalse();
        }

        @Test
        @DisplayName("non-allowable expenses should still be serialized in JSON")
        void nonAllowableShouldBeSerializedInJson() throws Exception {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.DEPRECIATION, new CategorySpend(new BigDecimal("500.00"), new BigDecimal("500.00")));
            expenses.put(ExpenseCategory.BUSINESS_ENTERTAINMENT, new CategorySpend(new BigDecimal("100.00"), new BigDecimal("100.00")));

            QuarterlyReviewData data = buildReviewData(
                    BigDecimal.ZERO, expenses, new BigDecimal("600.00"));

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);
            JsonNode expNode = root.path("periodExpenses");

            assertThat(expNode.path("depreciation").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(expNode.path("businessEntertainmentCosts").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("mixed allowable and non-allowable expenses should serialize correctly")
        void mixedAllowableAndNonAllowableShouldSerialize() throws Exception {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.TRAVEL, new CategorySpend(new BigDecimal("200.00"), new BigDecimal("200.00")));
            expenses.put(ExpenseCategory.DEPRECIATION, new CategorySpend(new BigDecimal("500.00"), new BigDecimal("500.00")));
            expenses.put(ExpenseCategory.PROFESSIONAL_FEES, new CategorySpend(new BigDecimal("300.00"), new BigDecimal("300.00")));
            expenses.put(ExpenseCategory.BUSINESS_ENTERTAINMENT, new CategorySpend(new BigDecimal("100.00"), new BigDecimal("100.00")));

            QuarterlyReviewData data = buildReviewData(
                    new BigDecimal("5000.00"), expenses, new BigDecimal("1100.00"));

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);
            JsonNode expNode = root.path("periodExpenses");

            assertThat(expNode.path("carVanTravelExpenses").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(expNode.path("depreciation").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(expNode.path("professionalFees").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(expNode.path("businessEntertainmentCosts").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("100.00"));
        }
    }

    // ==================== Group F: Tax Year Boundary ====================

    @Nested
    @DisplayName("Group F: Tax Year Boundary (6 Apr - 5 Apr)")
    class GroupF_TaxYearBoundary {

        @Test
        @DisplayName("PeriodSubmissionStrategy should support tax years up to 2024-25")
        void periodStrategyShouldSupportUpTo2024() {
            assertThat(periodStrategy.supports(TaxYear.of(2024))).isTrue();
            assertThat(periodStrategy.supports(TaxYear.of(2017))).isTrue();
            assertThat(periodStrategy.supports(TaxYear.of(2025))).isFalse();
        }

        @Test
        @DisplayName("CumulativeSubmissionStrategy should support tax years from 2025-26")
        void cumulativeStrategyShouldSupportFrom2025() {
            assertThat(cumulativeStrategy.supports(TaxYear.of(2025))).isTrue();
            assertThat(cumulativeStrategy.supports(TaxYear.of(2026))).isTrue();
            assertThat(cumulativeStrategy.supports(TaxYear.of(2024))).isFalse();
        }

        @Test
        @DisplayName("period strategy should include periodDates in JSON")
        void periodStrategyShouldIncludePeriodDates() throws Exception {
            QuarterlyReviewData data = buildReviewData(
                    new BigDecimal("1000.00"), new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO);

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.has("periodDates")).isTrue();
            assertThat(root.path("periodDates").has("periodStartDate")).isTrue();
            assertThat(root.path("periodDates").has("periodEndDate")).isTrue();
        }

        @Test
        @DisplayName("cumulative strategy should NOT include periodDates in JSON")
        void cumulativeStrategyShouldNotIncludePeriodDates() throws Exception {
            QuarterlyReviewData data = buildReviewData(
                    new BigDecimal("1000.00"), new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO,
                    TAX_YEAR_2025, Q1);

            String json = cumulativeStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.has("periodDates")).isFalse();
        }

        @Test
        @DisplayName("period strategy should use POST method")
        void periodStrategyShouldUsePost() {
            assertThat(periodStrategy.getHttpMethod()).isEqualTo("POST");
        }

        @Test
        @DisplayName("cumulative strategy should use PUT method")
        void cumulativeStrategyShouldUsePut() {
            assertThat(cumulativeStrategy.getHttpMethod()).isEqualTo("PUT");
        }

        @Test
        @DisplayName("period strategy should build /period endpoint URL")
        void periodStrategyShouldBuildPeriodUrl() {
            String url = periodStrategy.buildEndpointUrl(
                    "https://test-api.service.hmrc.gov.uk",
                    "AA123456A", "XAIS12345678910", TAX_YEAR_2024);
            assertThat(url).endsWith("/period");
        }

        @Test
        @DisplayName("cumulative strategy should build /cumulative endpoint URL with taxYear param")
        void cumulativeStrategyShouldBuildCumulativeUrl() {
            String url = cumulativeStrategy.buildEndpointUrl(
                    "https://test-api.service.hmrc.gov.uk",
                    "AA123456A", "XAIS12345678910", TAX_YEAR_2025);
            assertThat(url).contains("/cumulative?taxYear=2025-26");
        }

        @Test
        @DisplayName("Q1 period dates should be 6 Apr to 5 Jul for 2024-25")
        void q1PeriodDatesShouldBeCorrectFor2024() throws Exception {
            QuarterlyReviewData data = buildReviewData(
                    BigDecimal.ZERO, new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO,
                    TAX_YEAR_2024, Quarter.Q1);

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodDates").path("periodStartDate").asText())
                    .isEqualTo("2024-04-06");
            assertThat(root.path("periodDates").path("periodEndDate").asText())
                    .isEqualTo("2024-07-05");
        }

        @Test
        @DisplayName("Q4 period dates should be 6 Jan to 5 Apr (next calendar year) for 2024-25")
        void q4PeriodDatesShouldCrossCalendarYear() throws Exception {
            QuarterlyReviewData data = buildReviewData(
                    BigDecimal.ZERO, new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO,
                    TAX_YEAR_2024, Quarter.Q4);

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodDates").path("periodStartDate").asText())
                    .isEqualTo("2025-01-06");
            assertThat(root.path("periodDates").path("periodEndDate").asText())
                    .isEqualTo("2025-04-05");
        }
    }

    // ==================== Group G: Quarterly Aggregation ====================

    @Nested
    @DisplayName("Group G: Quarterly Aggregation")
    class GroupG_QuarterlyAggregation {

        @Test
        @DisplayName("should serialize Q1 review data correctly")
        void shouldSerializeQ1Correctly() throws Exception {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.TRAVEL, new CategorySpend(new BigDecimal("500.00"), new BigDecimal("500.00")));
            expenses.put(ExpenseCategory.OFFICE_COSTS, new CategorySpend(new BigDecimal("200.00"), new BigDecimal("200.00")));

            QuarterlyReviewData data = buildReviewData(
                    new BigDecimal("10000.00"), expenses, new BigDecimal("700.00"),
                    TAX_YEAR_2024, Quarter.Q1);

            String json = periodStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodIncome").path("turnover").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(root.path("periodExpenses").path("carVanTravelExpenses").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(root.path("periodExpenses").path("adminCosts").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("should serialize all four quarters for a tax year")
        void shouldSerializeAllFourQuarters() throws Exception {
            for (Quarter q : Quarter.values()) {
                Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
                expenses.put(ExpenseCategory.PROFESSIONAL_FEES,
                        new CategorySpend(new BigDecimal("100.00"), new BigDecimal("100.00")));

                QuarterlyReviewData data = buildReviewData(
                        new BigDecimal("2500.00"), expenses, new BigDecimal("100.00"),
                        TAX_YEAR_2024, q);

                String json = periodStrategy.serializeRequest(data);
                JsonNode root = objectMapper.readTree(json);

                assertThat(root.path("periodIncome").path("turnover").decimalValue())
                        .as("Quarter %s income", q)
                        .isEqualByComparingTo(new BigDecimal("2500.00"));
                assertThat(root.path("periodExpenses").path("professionalFees").decimalValue())
                        .as("Quarter %s professional fees", q)
                        .isEqualByComparingTo(new BigDecimal("100.00"));
            }
        }

        @Test
        @DisplayName("should handle nil return quarter correctly")
        void shouldHandleNilReturnQuarter() throws Exception {
            QuarterlyReviewData data = buildReviewData(
                    BigDecimal.ZERO, new EnumMap<>(ExpenseCategory.class), BigDecimal.ZERO,
                    TAX_YEAR_2024, Quarter.Q2);

            assertThat(data.isNilReturn()).isTrue();

            String json = periodStrategy.serializeRequest(data);
            assertThat(json).isNotBlank();
        }

        @Test
        @DisplayName("null reviewData should throw IllegalArgumentException")
        void nullReviewDataShouldThrow() {
            assertThatThrownBy(() -> periodStrategy.serializeRequest(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("cumulative strategy should serialize Q1 correctly for 2025-26")
        void cumulativeStrategyShouldSerializeQ1() throws Exception {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.STAFF_COSTS, new CategorySpend(new BigDecimal("3000.00"), new BigDecimal("3000.00")));

            QuarterlyReviewData data = buildReviewData(
                    new BigDecimal("15000.00"), expenses, new BigDecimal("3000.00"),
                    TAX_YEAR_2025, Quarter.Q1);

            String json = cumulativeStrategy.serializeRequest(data);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.path("periodIncome").path("turnover").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("15000.00"));
            assertThat(root.path("periodExpenses").path("wagesAndStaffCosts").decimalValue())
                    .isEqualByComparingTo(new BigDecimal("3000.00"));
        }

        @Test
        @DisplayName("all expense categories should round-trip through Period strategy")
        void allCategoriesShouldRoundTripThroughPeriodStrategy() throws Exception {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            for (ExpenseCategory cat : ExpenseCategory.values()) {
                expenses.put(cat, new CategorySpend(new BigDecimal("10.00"), new BigDecimal("10.00")));
            }

            BigDecimal totalExpenses = new BigDecimal("170.00"); // 17 categories x 10
            QuarterlyReviewData data = buildReviewData(
                    new BigDecimal("5000.00"), expenses, totalExpenses);

            String json = periodStrategy.serializeRequest(data);

            assertThat(json).isNotBlank();
            JsonNode root = objectMapper.readTree(json);
            assertThat(root.has("periodExpenses")).isTrue();
        }

        @Test
        @DisplayName("all expense categories should round-trip through Cumulative strategy")
        void allCategoriesShouldRoundTripThroughCumulativeStrategy() throws Exception {
            Map<ExpenseCategory, CategorySpend> expenses = new EnumMap<>(ExpenseCategory.class);
            for (ExpenseCategory cat : ExpenseCategory.values()) {
                expenses.put(cat, new CategorySpend(new BigDecimal("10.00"), new BigDecimal("10.00")));
            }

            BigDecimal totalExpenses = new BigDecimal("170.00");
            QuarterlyReviewData data = buildReviewData(
                    new BigDecimal("5000.00"), expenses, totalExpenses, TAX_YEAR_2025, Q1);

            String json = cumulativeStrategy.serializeRequest(data);

            assertThat(json).isNotBlank();
            JsonNode root = objectMapper.readTree(json);
            assertThat(root.has("periodExpenses")).isTrue();
        }
    }

    // ==================== SA103F Box Number Verification ====================

    @Nested
    @DisplayName("SA103F Box Number Verification")
    class BoxNumberVerification {

        @ParameterizedTest(name = "{0} should have SA103F box {1}")
        @MethodSource("uk.selfemploy.ui.service.submission.SA103FlowThroughTest#expenseCategoryBoxNumbers")
        @DisplayName("should have correct SA103F box number for each category")
        void shouldHaveCorrectBoxNumber(ExpenseCategory category, String expectedBox) {
            assertThat(category.getSa103Box())
                    .as("ExpenseCategory.%s SA103F box", category.name())
                    .isEqualTo(expectedBox);
        }

        @Test
        @DisplayName("SALES income should have box 9")
        void salesIncomeShouldHaveBox9() {
            assertThat(IncomeCategory.SALES.getSa103Box()).isEqualTo("9");
        }

        @Test
        @DisplayName("OTHER_INCOME should have box 10")
        void otherIncomeShouldHaveBox10() {
            assertThat(IncomeCategory.OTHER_INCOME.getSa103Box()).isEqualTo("10");
        }
    }

    // ==================== Parameterized Test Data Sources ====================

    static Stream<Arguments> singleCategoryArguments() {
        return Stream.of(
                Arguments.of(ExpenseCategory.COST_OF_GOODS, "costOfGoods", "17"),
                Arguments.of(ExpenseCategory.SUBCONTRACTOR_COSTS, "paymentsToSubcontractors", "18"),
                Arguments.of(ExpenseCategory.STAFF_COSTS, "wagesAndStaffCosts", "19"),
                Arguments.of(ExpenseCategory.TRAVEL, "carVanTravelExpenses", "20"),
                Arguments.of(ExpenseCategory.TRAVEL_MILEAGE, "carVanTravelExpenses", "20"),
                Arguments.of(ExpenseCategory.PREMISES, "premisesRunningCosts", "21"),
                Arguments.of(ExpenseCategory.REPAIRS, "maintenanceCosts", "22"),
                Arguments.of(ExpenseCategory.OFFICE_COSTS, "adminCosts", "23"),
                Arguments.of(ExpenseCategory.ADVERTISING, "advertisingCosts", "24"),
                Arguments.of(ExpenseCategory.INTEREST, "interestOnBankOtherLoans", "25"),
                Arguments.of(ExpenseCategory.FINANCIAL_CHARGES, "financeCharges", "26"),
                Arguments.of(ExpenseCategory.BAD_DEBTS, "irrecoverableDebts", "27"),
                Arguments.of(ExpenseCategory.PROFESSIONAL_FEES, "professionalFees", "28"),
                Arguments.of(ExpenseCategory.DEPRECIATION, "depreciation", "29"),
                Arguments.of(ExpenseCategory.OTHER_EXPENSES, "otherExpenses", "30"),
                Arguments.of(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, "otherExpenses", "30"),
                Arguments.of(ExpenseCategory.BUSINESS_ENTERTAINMENT, "businessEntertainmentCosts", "24")
        );
    }

    static Stream<Arguments> expenseCategoryBoxNumbers() {
        return Stream.of(
                Arguments.of(ExpenseCategory.COST_OF_GOODS, "17"),
                Arguments.of(ExpenseCategory.SUBCONTRACTOR_COSTS, "18"),
                Arguments.of(ExpenseCategory.STAFF_COSTS, "19"),
                Arguments.of(ExpenseCategory.TRAVEL, "20"),
                Arguments.of(ExpenseCategory.TRAVEL_MILEAGE, "20"),
                Arguments.of(ExpenseCategory.PREMISES, "21"),
                Arguments.of(ExpenseCategory.REPAIRS, "22"),
                Arguments.of(ExpenseCategory.OFFICE_COSTS, "23"),
                Arguments.of(ExpenseCategory.ADVERTISING, "24"),
                Arguments.of(ExpenseCategory.INTEREST, "25"),
                Arguments.of(ExpenseCategory.FINANCIAL_CHARGES, "26"),
                Arguments.of(ExpenseCategory.BAD_DEBTS, "27"),
                Arguments.of(ExpenseCategory.PROFESSIONAL_FEES, "28"),
                Arguments.of(ExpenseCategory.DEPRECIATION, "29"),
                Arguments.of(ExpenseCategory.OTHER_EXPENSES, "30"),
                Arguments.of(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, "30"),
                Arguments.of(ExpenseCategory.BUSINESS_ENTERTAINMENT, "24")
        );
    }

    // ==================== Utility Methods ====================

    /**
     * Gets the value of a MappedExpenses field by name via reflection-free lookup.
     */
    private BigDecimal getFieldValue(AbstractSubmissionStrategy.MappedExpenses mapped, String fieldName) {
        return switch (fieldName) {
            case "costOfGoods" -> mapped.costOfGoodsBought().spent();
            case "paymentsToSubcontractors" -> mapped.cisPaymentsToSubcontractors().spent();
            case "wagesAndStaffCosts" -> mapped.staffCosts().spent();
            case "carVanTravelExpenses" -> mapped.travelCosts().spent();
            case "premisesRunningCosts" -> mapped.premisesRunningCosts().spent();
            case "maintenanceCosts" -> mapped.maintenanceCosts().spent();
            case "adminCosts" -> mapped.adminCosts().spent();
            case "advertisingCosts" -> mapped.advertisingCosts().spent();
            case "businessEntertainmentCosts" -> mapped.businessEntertainmentCosts().spent();
            case "interestOnBankOtherLoans" -> mapped.interest().spent();
            case "financeCharges" -> mapped.financialCharges().spent();
            case "irrecoverableDebts" -> mapped.badDebt().spent();
            case "professionalFees" -> mapped.professionalFees().spent();
            case "depreciation" -> mapped.depreciation().spent();
            case "otherExpenses" -> mapped.other().spent();
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };
    }

    /**
     * Sums all fields in a MappedExpenses record.
     */
    private BigDecimal getAllFieldsTotal(AbstractSubmissionStrategy.MappedExpenses mapped) {
        return mapped.costOfGoodsBought().spent()
                .add(mapped.cisPaymentsToSubcontractors().spent())
                .add(mapped.staffCosts().spent())
                .add(mapped.travelCosts().spent())
                .add(mapped.premisesRunningCosts().spent())
                .add(mapped.maintenanceCosts().spent())
                .add(mapped.adminCosts().spent())
                .add(mapped.advertisingCosts().spent())
                .add(mapped.businessEntertainmentCosts().spent())
                .add(mapped.interest().spent())
                .add(mapped.financialCharges().spent())
                .add(mapped.badDebt().spent())
                .add(mapped.professionalFees().spent())
                .add(mapped.depreciation().spent())
                .add(mapped.other().spent());
    }

    /**
     * Calculates expected total for a single category, accounting for
     * aggregated categories (TRAVEL+TRAVEL_MILEAGE, OTHER_EXPENSES+HOME_OFFICE_SIMPLIFIED).
     */
    private BigDecimal getExpectedTotal(ExpenseCategory category, BigDecimal amount) {
        // Single categories contribute only their amount to the total
        return amount;
    }
}
