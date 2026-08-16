package uk.selfemploy.ui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.core.profit.ProfitTotals;
import uk.selfemploy.ui.OneProfitFixture;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteExpenseService;
import uk.selfemploy.ui.service.SqliteIncomeService;
import uk.selfemploy.ui.service.SqliteTestSupport;
import uk.selfemploy.ui.service.submission.CumulativeSubmissionStrategy;
import uk.selfemploy.ui.service.submission.PeriodSubmissionStrategy;
import uk.selfemploy.ui.service.submission.SubmissionStrategy;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What goes on the wire is what the Self Employment Business API v5 will accept.
 *
 * <p>The schemas under {@code src/test/resources/hmrc/v5/schemas} are HMRC's own request schemas,
 * downloaded unmodified from the developer hub. Every one of them sets
 * {@code additionalProperties: false}, which is the property this test exists to exploit: a key HMRC
 * does not define is a build failure here rather than a field the sandbox quietly ignores and
 * production rejects.
 *
 * <p>The payloads are produced by the strategies {@code UiQuarterlySubmissionService} itself calls
 * — the JSON asserted on is the JSON that would be sent. Both strategies are driven from the same
 * dataset because what is under test is the key names and the two expense columns, and neither
 * depends on which tax year selected the strategy.
 *
 * <p>SA103F is a two-column form: boxes 17-30 are what was spent and boxes 32-45 are the part of it
 * that cannot be claimed. HMRC derives the deduction by subtracting the second column from the
 * first, so a payload carrying only allowable totals is not merely incomplete — it is a different
 * declaration. {@link #theTwoColumnsReconcileToTheClaim} is what refuses that payload, and it
 * equally refuses the opposite error of filing gross spend with no disallowables block, which would
 * under-declare tax by the disallowed amount.
 *
 * @see uk.selfemploy.ui.service.submission.AbstractSubmissionStrategy
 */
@DisplayName("HMRC v5 payload contract")
class HmrcPayloadContractTest {

    private static final String SCHEMA_ROOT = "classpath:/hmrc/v5/schemas/";

    /**
     * The period endpoint accepts either definition depending on the tax year, and
     * {@link PeriodSubmissionStrategy} covers tax years spanning both. A payload it produces must
     * therefore satisfy each of them.
     */
    private static final List<String> PERIOD_SCHEMAS = List.of(
            SCHEMA_ROOT + "createPeriodSummary/def1/request.json",
            SCHEMA_ROOT + "createPeriodSummary/def2/request.json");

    private static final String CUMULATIVE_SCHEMA =
            SCHEMA_ROOT + "createAmendCumulativePeriodSummary/request.json";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonSchemaFactory SCHEMAS =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

    private static UUID businessId;

    private SqliteIncomeService incomeService;
    private SqliteExpenseService expenseService;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
        businessId = UUID.randomUUID();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
        incomeService = new SqliteIncomeService(businessId);
        expenseService = new SqliteExpenseService(businessId);
    }

    /** The aggregation the submission service really feeds to a strategy. */
    private QuarterlyReviewData filedQuarter() {
        QuarterlyUpdatesController controller = new QuarterlyUpdatesController();
        controller.initializeWithDependencies(incomeService, expenseService, businessId);
        controller.setTaxYear(OneProfitFixture.TAX_YEAR);
        return controller.aggregateReviewData(OneProfitFixture.QUARTER);
    }

    /** The same records the payload is built from, split the one way the app splits them. */
    private ProfitTotals quarterTotals() {
        return ProfitTotals.of(List.of(), expenseService.findByQuarter(
                businessId, OneProfitFixture.TAX_YEAR, OneProfitFixture.QUARTER));
    }

    private JsonNode payloadOf(SubmissionStrategy strategy) throws Exception {
        return JSON.readTree(strategy.serializeRequest(filedQuarter()));
    }

    private void assertConformsTo(JsonNode payload, String schemaLocation) {
        JsonSchema schema = SCHEMAS.getSchema(SchemaLocation.of(schemaLocation));
        Set<ValidationMessage> failures = schema.validate(payload);
        assertThat(failures)
                .as("%s rejected by %s%n%s", payload, schemaLocation, payload.toPrettyString())
                .isEmpty();
    }

    private static BigDecimal sumOf(JsonNode block) {
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode value : block) {
            total = total.add(value.decimalValue());
        }
        return total;
    }

    @Test
    @DisplayName("the period payload uses only keys the period schemas define")
    void thePeriodPayloadConformsToTheSchema() throws Exception {
        OneProfitFixture.seedWithPartBusinessExpense(incomeService, expenseService, businessId);

        JsonNode payload = payloadOf(new PeriodSubmissionStrategy());

        PERIOD_SCHEMAS.forEach(schema -> assertConformsTo(payload, schema));
    }

    @Test
    @DisplayName("the cumulative payload uses only keys the cumulative schema defines")
    void theCumulativePayloadConformsToTheSchema() throws Exception {
        OneProfitFixture.seedWithPartBusinessExpense(incomeService, expenseService, businessId);

        assertConformsTo(payloadOf(new CumulativeSubmissionStrategy()), CUMULATIVE_SCHEMA);
    }

    @Test
    @DisplayName("a nil quarter still conforms, so an empty payload cannot drift unnoticed")
    void aNilQuarterConformsToTheSchema() throws Exception {
        JsonNode period = payloadOf(new PeriodSubmissionStrategy());
        JsonNode cumulative = payloadOf(new CumulativeSubmissionStrategy());

        PERIOD_SCHEMAS.forEach(schema -> assertConformsTo(period, schema));
        assertConformsTo(cumulative, CUMULATIVE_SCHEMA);
    }

    @Test
    @DisplayName("both columns are filed, and their difference is the claim")
    void theTwoColumnsReconcileToTheClaim() throws Exception {
        OneProfitFixture.seedWithPartBusinessExpense(incomeService, expenseService, businessId);
        ProfitTotals totals = quarterTotals();

        JsonNode payload = payloadOf(new CumulativeSubmissionStrategy());
        JsonNode expenses = payload.get("periodExpenses");
        JsonNode disallowable = payload.get("periodDisallowableExpenses");

        assertThat(disallowable)
                .as("without this block HMRC treats the whole spend as deductible, which under-"
                    + "declares tax by the disallowed amount")
                .isNotNull();
        assertThat(sumOf(expenses))
                .as("boxes 17-30 declare what was spent, not what may be claimed")
                .isEqualByComparingTo(totals.grossSpend());
        assertThat(sumOf(disallowable))
                .as("boxes 32-45 declare the part that cannot be claimed")
                .isEqualByComparingTo(totals.grossSpend().subtract(totals.allowableSpend()));
        assertThat(sumOf(expenses).subtract(sumOf(disallowable)))
                .as("the deduction HMRC derives from the two columns is the one the app claims")
                .isEqualByComparingTo(totals.allowableSpend());
    }

    @Test
    @DisplayName("a category HMRC disallows is reported in full and disallowed in full")
    void aDisallowedCategoryIsReportedRatherThanFiledAsZero() throws Exception {
        OneProfitFixture.seedBase(incomeService, expenseService, businessId);

        JsonNode payload = payloadOf(new CumulativeSubmissionStrategy());

        assertThat(payload.get("periodExpenses").get("businessEntertainmentCosts").decimalValue())
                .as("entertainment used to be filed as zero, which reports it as never having "
                    + "happened rather than as spend that cannot be claimed")
                .isEqualByComparingTo(OneProfitFixture.ENTERTAINMENT);
        assertThat(payload.get("periodDisallowableExpenses")
                        .get("businessEntertainmentCostsDisallowable").decimalValue())
                .isEqualByComparingTo(OneProfitFixture.ENTERTAINMENT);
    }

    @Test
    @DisplayName("the private share of a part-business expense is disallowed, not omitted")
    void aPartBusinessExpenseIsDeclaredInFullAndPartlyDisallowed() throws Exception {
        OneProfitFixture.seedWithPartBusinessExpense(incomeService, expenseService, businessId);

        JsonNode payload = payloadOf(new CumulativeSubmissionStrategy());
        BigDecimal privateShare = OneProfitFixture.PHONE_BILL.subtract(OneProfitFixture.PHONE_CLAIMABLE);

        assertThat(payload.get("periodExpenses").get("adminCosts").decimalValue())
                .isEqualByComparingTo(
                        OneProfitFixture.OFFICE_COSTS.add(OneProfitFixture.PHONE_BILL));
        assertThat(payload.get("periodDisallowableExpenses").get("adminCostsDisallowable").decimalValue())
                .as("the %s of the phone bill that is not business use", privateShare)
                .isEqualByComparingTo(privateShare);
    }

    @Test
    @DisplayName("turnover is filed under the key the schema names")
    void turnoverIsFiledUnderTheSchemaKey() throws Exception {
        OneProfitFixture.seedBase(incomeService, expenseService, businessId);

        JsonNode payload = payloadOf(new CumulativeSubmissionStrategy());

        assertThat(payload.get("periodIncome").get("turnover").decimalValue())
                .isEqualByComparingTo(OneProfitFixture.TURNOVER);
    }
}
