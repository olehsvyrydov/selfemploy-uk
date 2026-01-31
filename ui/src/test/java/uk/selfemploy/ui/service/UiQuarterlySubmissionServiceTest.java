package uk.selfemploy.ui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.dto.PeriodicUpdate;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.service.submission.CumulativeSubmissionStrategy;
import uk.selfemploy.ui.service.submission.PeriodSubmissionStrategy;
import uk.selfemploy.ui.service.submission.SubmissionStrategy;
import uk.selfemploy.ui.service.submission.SubmissionStrategyFactory;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.core.exception.SubmissionException;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.net.http.HttpClient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for UiQuarterlySubmissionService.
 *
 * Tests the UI-layer bridge to the HMRC quarterly submission API.
 */
@DisplayName("UiQuarterlySubmissionService Tests")
class UiQuarterlySubmissionServiceTest {

    private UiQuarterlySubmissionService service;
    private static final UUID BUSINESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String TEST_NINO = "QQ123456C";

    @BeforeEach
    void setUp() {
        // Use constructor without SqliteDataStore to avoid file-system SQLite access
        // and to isolate unit tests from any persisted NINO values
        service = new UiQuarterlySubmissionService(
                HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build(),
                null);
    }

    @Nested
    @DisplayName("buildPeriodicUpdate()")
    class BuildPeriodicUpdateTests {

        @Test
        @DisplayName("should build PeriodicUpdate with income as turnover")
        void shouldBuildWithIncome() {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );

            PeriodicUpdate update = service.buildPeriodicUpdate(data);

            assertThat(update).isNotNull();
            assertThat(update.periodIncome().turnover()).isEqualByComparingTo("5000.00");
            assertThat(update.periodIncome().other()).isEqualByComparingTo("0");
            assertThat(update.periodFromDate()).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(update.periodToDate()).isEqualTo(LocalDate.of(2025, 7, 5));
        }

        @Test
        @DisplayName("should map expense categories to SA103 fields")
        void shouldMapExpenseCategories() {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.COST_OF_GOODS, new CategorySummary(new BigDecimal("100.00"), 2));
            expenses.put(ExpenseCategory.STAFF_COSTS, new CategorySummary(new BigDecimal("200.00"), 3));
            expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("50.00"), 1));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySummary(new BigDecimal("30.00"), 1));
            expenses.put(ExpenseCategory.PREMISES, new CategorySummary(new BigDecimal("150.00"), 2));
            expenses.put(ExpenseCategory.OFFICE_COSTS, new CategorySummary(new BigDecimal("75.00"), 3));
            expenses.put(ExpenseCategory.PROFESSIONAL_FEES, new CategorySummary(new BigDecimal("250.00"), 1));
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new CategorySummary(new BigDecimal("40.00"), 2));
            expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new CategorySummary(new BigDecimal("60.00"), 1));

            BigDecimal totalExpenses = expenses.values().stream()
                    .map(CategorySummary::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            QuarterlyReviewData data = createReviewData(
                    Quarter.Q2,
                    new BigDecimal("10000.00"),
                    totalExpenses,
                    expenses
            );

            PeriodicUpdate update = service.buildPeriodicUpdate(data);

            assertThat(update.periodExpenses().costOfGoodsBought()).isEqualByComparingTo("100.00");
            assertThat(update.periodExpenses().staffCosts()).isEqualByComparingTo("200.00");
            // Travel + Travel Mileage combined
            assertThat(update.periodExpenses().travelCosts()).isEqualByComparingTo("80.00");
            assertThat(update.periodExpenses().premisesRunningCosts()).isEqualByComparingTo("150.00");
            assertThat(update.periodExpenses().adminCosts()).isEqualByComparingTo("75.00");
            assertThat(update.periodExpenses().professionalFees()).isEqualByComparingTo("250.00");
            // Other + Home Office Simplified combined
            assertThat(update.periodExpenses().other()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("should handle nil return with zero amounts")
        void shouldHandleNilReturn() {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q3,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );

            PeriodicUpdate update = service.buildPeriodicUpdate(data);

            assertThat(update.periodIncome().turnover()).isEqualByComparingTo("0");
            assertThat(update.periodExpenses().calculateTotal()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should map subcontractor costs to CIS field")
        void shouldMapSubcontractorCosts() {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.SUBCONTRACTOR_COSTS, new CategorySummary(new BigDecimal("500.00"), 1));

            QuarterlyReviewData data = createReviewData(
                    Quarter.Q1,
                    BigDecimal.ZERO,
                    new BigDecimal("500.00"),
                    expenses
            );

            PeriodicUpdate update = service.buildPeriodicUpdate(data);
            assertThat(update.periodExpenses().cisPaymentsToSubcontractors()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("should map all SA103 categories correctly")
        void shouldMapAllSa103Categories() {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.REPAIRS, new CategorySummary(new BigDecimal("120.00"), 1));
            expenses.put(ExpenseCategory.ADVERTISING, new CategorySummary(new BigDecimal("90.00"), 1));
            expenses.put(ExpenseCategory.INTEREST, new CategorySummary(new BigDecimal("30.00"), 1));
            expenses.put(ExpenseCategory.FINANCIAL_CHARGES, new CategorySummary(new BigDecimal("15.00"), 1));
            expenses.put(ExpenseCategory.BAD_DEBTS, new CategorySummary(new BigDecimal("200.00"), 1));

            BigDecimal total = expenses.values().stream()
                    .map(CategorySummary::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            QuarterlyReviewData data = createReviewData(Quarter.Q1, BigDecimal.ZERO, total, expenses);
            PeriodicUpdate update = service.buildPeriodicUpdate(data);

            assertThat(update.periodExpenses().maintenanceCosts()).isEqualByComparingTo("120.00");
            assertThat(update.periodExpenses().advertisingCosts()).isEqualByComparingTo("90.00");
            assertThat(update.periodExpenses().interest()).isEqualByComparingTo("30.00");
            assertThat(update.periodExpenses().financialCharges()).isEqualByComparingTo("15.00");
            assertThat(update.periodExpenses().badDebt()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("should set correct period dates for each quarter")
        void shouldSetCorrectPeriodDates() {
            TaxYear taxYear = TaxYear.of(2025);

            for (Quarter quarter : Quarter.values()) {
                QuarterlyReviewData data = QuarterlyReviewData.builder()
                        .quarter(quarter)
                        .taxYear(taxYear)
                        .periodStart(quarter.getStartDate(taxYear))
                        .periodEnd(quarter.getEndDate(taxYear))
                        .totalIncome(BigDecimal.ZERO)
                        .totalExpenses(BigDecimal.ZERO)
                        .build();

                PeriodicUpdate update = service.buildPeriodicUpdate(data);

                assertThat(update.periodFromDate()).isEqualTo(quarter.getStartDate(taxYear));
                assertThat(update.periodToDate()).isEqualTo(quarter.getEndDate(taxYear));
            }
        }

        @Test
        @DisplayName("should throw for null reviewData")
        void shouldThrowForNullData() {
            assertThatThrownBy(() -> service.buildPeriodicUpdate(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reviewData");
        }
    }

    @Nested
    @DisplayName("serializePeriodicUpdate()")
    class SerializeTests {

        @Test
        @DisplayName("should serialize PeriodicUpdate to valid JSON")
        void shouldSerializeToJson() throws Exception {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    new BigDecimal("800.00"),
                    createSampleExpenses()
            );

            PeriodicUpdate update = service.buildPeriodicUpdate(data);
            String json = service.serializePeriodicUpdate(update);

            assertThat(json).isNotNull();
            // HMRC API v5.0 expects dates wrapped in periodDates object
            assertThat(json).contains("\"periodDates\"");
            assertThat(json).contains("\"periodStartDate\"");
            assertThat(json).contains("\"periodEndDate\"");
            assertThat(json).contains("\"periodIncome\"");
            assertThat(json).contains("\"periodExpenses\"");
            assertThat(json).contains("\"turnover\"");
            // Computed fields should NOT be serialized (HMRC rejects unknown fields)
            assertThat(json).doesNotContain("\"total\"");
            assertThat(json).doesNotContain("\"netProfit\"");
            assertThat(json).doesNotContain("\"allowableTotal\"");
            // Dates should NOT be at root level
            assertThat(json).doesNotMatch("^\\{\"periodStartDate\".*");
        }

        @Test
        @DisplayName("should produce JSON parseable by Jackson")
        void shouldProduceParseableJson() throws Exception {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );

            PeriodicUpdate update = service.buildPeriodicUpdate(data);
            String json = service.serializePeriodicUpdate(update);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            PeriodicUpdate parsed = mapper.readValue(json, PeriodicUpdate.class);

            assertThat(parsed.periodIncome().turnover()).isEqualByComparingTo("5000.00");
        }
    }

    @Nested
    @DisplayName("NINO Management")
    class NinoTests {

        @Test
        @DisplayName("should set and get NINO")
        void shouldSetAndGetNino() {
            service.setNino(TEST_NINO);
            assertThat(service.getNino()).isEqualTo(TEST_NINO);
        }

        @Test
        @DisplayName("should uppercase NINO on set")
        void shouldUppercaseNino() {
            service.setNino("qq123456c");
            assertThat(service.getNino()).isEqualTo("QQ123456C");
        }

        @Test
        @DisplayName("should return null for unset NINO")
        void shouldReturnNullForUnset() {
            assertThat(service.getNino()).isNull();
        }
    }

    @Nested
    @DisplayName("HMRC Business ID Management")
    class HmrcBusinessIdTests {

        @Test
        @DisplayName("should set and get HMRC business ID")
        void shouldSetAndGetHmrcBusinessId() {
            service.setHmrcBusinessId("XAIS12345678901");
            assertThat(service.getHmrcBusinessId()).isEqualTo("XAIS12345678901");
        }

        @Test
        @DisplayName("should return null for unset HMRC business ID")
        void shouldReturnNullForUnset() {
            assertThat(service.getHmrcBusinessId()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("should validate NINO is set before submission")
        void shouldValidateNinoIsSet() {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );
            service.setHmrcBusinessId("XAIS12345678901");
            // NINO not set

            assertThatThrownBy(() -> service.submit(data, Instant.now(), "a".repeat(64)))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("NINO");
        }

        @Test
        @DisplayName("should validate HMRC business ID is set before submission")
        void shouldValidateHmrcBusinessIdIsSet() {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );
            service.setNino(TEST_NINO);
            // HMRC business ID not set

            assertThatThrownBy(() -> service.submit(data, Instant.now(), "a".repeat(64)))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("HMRC_PROFILE_NOT_SYNCED");
        }

        @Test
        @DisplayName("should validate declaration timestamp is not null")
        void shouldValidateDeclarationTimestamp() {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );
            service.setNino(TEST_NINO);
            service.setHmrcBusinessId("XAIS12345678901");

            assertThatThrownBy(() -> service.submit(data, null, "a".repeat(64)))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("DECLARATION_REQUIRED");
        }

        @Test
        @DisplayName("should validate declaration hash is not null")
        void shouldValidateDeclarationHash() {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );
            service.setNino(TEST_NINO);
            service.setHmrcBusinessId("XAIS12345678901");

            assertThatThrownBy(() -> service.submit(data, Instant.now(), null))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("DECLARATION_REQUIRED");
        }
    }

    @Nested
    @DisplayName("buildSubmissionRecord()")
    class BuildSubmissionRecordTests {

        @Test
        @DisplayName("should build accepted submission with HMRC reference")
        void shouldBuildAcceptedSubmission() {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    new BigDecimal("800.00"),
                    createSampleExpenses()
            );

            Submission submission = service.buildSubmissionRecord(
                    BUSINESS_ID, data,
                    SubmissionStatus.ACCEPTED, "MTD-Q1-2025-123456",
                    null,
                    Instant.now(), "a".repeat(64)
            );

            assertThat(submission).isNotNull();
            assertThat(submission.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(submission.status()).isEqualTo(SubmissionStatus.ACCEPTED);
            assertThat(submission.hmrcReference()).isEqualTo("MTD-Q1-2025-123456");
            assertThat(submission.totalIncome()).isEqualByComparingTo("5000.00");
            assertThat(submission.totalExpenses()).isEqualByComparingTo("800.00");
            assertThat(submission.netProfit()).isEqualByComparingTo("4200.00");
            assertThat(submission.declarationAcceptedAt()).isNotNull();
            assertThat(submission.declarationTextHash()).hasSize(64);
        }

        @Test
        @DisplayName("should build rejected submission with error message")
        void shouldBuildRejectedSubmission() {
            QuarterlyReviewData data = createReviewData(
                    Quarter.Q2,
                    new BigDecimal("3000.00"),
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );

            Submission submission = service.buildSubmissionRecord(
                    BUSINESS_ID, data,
                    SubmissionStatus.REJECTED, null,
                    "INVALID_NINO: Format incorrect",
                    Instant.now(), "b".repeat(64)
            );

            assertThat(submission.status()).isEqualTo(SubmissionStatus.REJECTED);
            assertThat(submission.hmrcReference()).isNull();
            assertThat(submission.errorMessage()).isEqualTo("INVALID_NINO: Format incorrect");
        }

        @Test
        @DisplayName("should set correct submission type based on quarter")
        void shouldSetCorrectSubmissionType() {
            QuarterlyReviewData q3Data = createReviewData(
                    Quarter.Q3,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );

            Submission submission = service.buildSubmissionRecord(
                    BUSINESS_ID, q3Data,
                    SubmissionStatus.ACCEPTED, "REF-123",
                    null, Instant.now(), "c".repeat(64)
            );

            assertThat(submission.type().name()).isEqualTo("QUARTERLY_Q3");
        }
    }

    @Nested
    @DisplayName("getApiUrl()")
    class ApiUrlTests {

        @Test
        @DisplayName("should build period endpoint URL for tax year 2024 with query parameter")
        void shouldBuildPeriodUrlForTaxYear2024() {
            TaxYear taxYear2024 = TaxYear.of(2024);
            String url = service.getApiUrl(
                    "https://test-api.service.hmrc.gov.uk",
                    TEST_NINO,
                    BUSINESS_ID.toString(),
                    taxYear2024
            );

            // v5.0 API for 2024-25: no taxYear query param, dates in periodDates body
            assertThat(url).isEqualTo(
                    "https://test-api.service.hmrc.gov.uk/individuals/business/self-employment/"
                    + TEST_NINO + "/" + BUSINESS_ID + "/period"
            );
        }

        @Test
        @DisplayName("should build cumulative endpoint URL for tax year 2025 with query parameter")
        void shouldBuildCumulativeUrlForTaxYear2025() {
            TaxYear taxYear2025 = TaxYear.of(2025);
            String url = service.getApiUrl(
                    "https://test-api.service.hmrc.gov.uk",
                    TEST_NINO,
                    BUSINESS_ID.toString(),
                    taxYear2025
            );

            assertThat(url).isEqualTo(
                    "https://test-api.service.hmrc.gov.uk/individuals/business/self-employment/"
                    + TEST_NINO + "/" + BUSINESS_ID + "/cumulative?taxYear=2025-26"
            );
        }

        @Test
        @DisplayName("should build cumulative endpoint URL for tax year 2026 with query parameter")
        void shouldBuildCumulativeUrlForTaxYear2026() {
            TaxYear taxYear2026 = TaxYear.of(2026);
            String url = service.getApiUrl(
                    "https://test-api.service.hmrc.gov.uk",
                    TEST_NINO,
                    BUSINESS_ID.toString(),
                    taxYear2026
            );

            assertThat(url).isEqualTo(
                    "https://test-api.service.hmrc.gov.uk/individuals/business/self-employment/"
                    + TEST_NINO + "/" + BUSINESS_ID + "/cumulative?taxYear=2026-27"
            );
        }

        @Test
        @DisplayName("should use period endpoint when null (defaults to period for safety)")
        void shouldUseCurrentTaxYearWhenNull() {
            String url = service.getApiUrl(
                    "https://test-api.service.hmrc.gov.uk",
                    TEST_NINO,
                    BUSINESS_ID.toString(),
                    null
            );

            // When tax year is null, we default to period endpoint (safer for older tax years)
            // The actual tax year will be determined from periodDates in the request body
            assertThat(url).endsWith("/period");
        }
    }

    @Nested
    @DisplayName("NINO Reload from SQLite (BUG-NINO-REQUIRED)")
    class NinoReloadTests {

        @Test
        @DisplayName("should reload NINO from SqliteDataStore when in-memory NINO is null")
        void shouldReloadNinoFromSqliteWhenNull() {
            // Setup: enable test mode with in-memory SQLite
            SqliteTestSupport.enableTestMode();
            try {
                SqliteDataStore dataStore = SqliteDataStore.getInstance();

                // Create service with NO NINO set (simulates singleton created before user saves NINO)
                UiQuarterlySubmissionService serviceWithDataStore =
                        new UiQuarterlySubmissionService(HttpClient.newHttpClient(), dataStore);
                serviceWithDataStore.setHmrcBusinessId("XAIS12345678901");
                // Deliberately NOT calling serviceWithDataStore.setNino(...)

                // User saves NINO in Settings (goes directly to SQLite)
                dataStore.saveNino(TEST_NINO);

                // Create review data and attempt submission
                QuarterlyReviewData data = createReviewData(
                        Quarter.Q1,
                        new BigDecimal("5000.00"),
                        BigDecimal.ZERO,
                        new EnumMap<>(ExpenseCategory.class)
                );

                // Submit should NOT throw NINO_REQUIRED - it should get past validation
                // and fail on OAuth instead (which is expected since we're not connected)
                assertThatThrownBy(() -> serviceWithDataStore.submit(data, Instant.now(), "a".repeat(64)))
                        .isInstanceOf(SubmissionException.class)
                        .satisfies(ex -> {
                            String message = ex.getMessage();
                            assertThat(message).doesNotContain("NINO_REQUIRED");
                        });
            } finally {
                SqliteTestSupport.tearDownTestEnvironment();
            }
        }

        @Test
        @DisplayName("should still throw NINO_REQUIRED when NINO is not in SQLite either")
        void shouldStillThrowWhenNinoNotInSqliteEither() {
            SqliteTestSupport.enableTestMode();
            try {
                SqliteDataStore dataStore = SqliteDataStore.getInstance();

                // Create service with no NINO and no NINO in SQLite
                UiQuarterlySubmissionService serviceWithDataStore =
                        new UiQuarterlySubmissionService(HttpClient.newHttpClient(), dataStore);
                serviceWithDataStore.setHmrcBusinessId("XAIS12345678901");

                QuarterlyReviewData data = createReviewData(
                        Quarter.Q1,
                        new BigDecimal("5000.00"),
                        BigDecimal.ZERO,
                        new EnumMap<>(ExpenseCategory.class)
                );

                assertThatThrownBy(() -> serviceWithDataStore.submit(data, Instant.now(), "a".repeat(64)))
                        .isInstanceOf(SubmissionException.class)
                        .hasMessageContaining("NINO_REQUIRED");
            } finally {
                SqliteTestSupport.tearDownTestEnvironment();
            }
        }

        @Test
        @DisplayName("should update in-memory NINO after reloading from SQLite")
        void shouldUpdateInMemoryNinoAfterReload() {
            SqliteTestSupport.enableTestMode();
            try {
                SqliteDataStore dataStore = SqliteDataStore.getInstance();

                UiQuarterlySubmissionService serviceWithDataStore =
                        new UiQuarterlySubmissionService(HttpClient.newHttpClient(), dataStore);

                // Initially no NINO
                assertThat(serviceWithDataStore.getNino()).isNull();

                // Save NINO to SQLite
                dataStore.saveNino(TEST_NINO);

                // Trigger validation (will fail on OAuth, but NINO should be reloaded)
                serviceWithDataStore.setHmrcBusinessId("XAIS12345678901");
                QuarterlyReviewData data = createReviewData(
                        Quarter.Q1,
                        new BigDecimal("5000.00"),
                        BigDecimal.ZERO,
                        new EnumMap<>(ExpenseCategory.class)
                );

                try {
                    serviceWithDataStore.submit(data, Instant.now(), "a".repeat(64));
                } catch (SubmissionException e) {
                    // Expected - will fail on OAuth, not NINO
                    assertThat(e.getMessage()).doesNotContain("NINO_REQUIRED");
                }

                // After validation ran, in-memory NINO should be updated
                assertThat(serviceWithDataStore.getNino()).isEqualTo(TEST_NINO);
            } finally {
                SqliteTestSupport.tearDownTestEnvironment();
            }
        }
    }

    @Nested
    @DisplayName("HMRC Business ID Reload from SQLite")
    class BusinessIdReloadTests {

        @Test
        @DisplayName("should reload HMRC business ID from SqliteDataStore when in-memory hmrcBusinessId is null")
        void shouldReloadHmrcBusinessIdFromSqliteWhenNull() {
            SqliteTestSupport.enableTestMode();
            try {
                SqliteDataStore dataStore = SqliteDataStore.getInstance();

                // Create service with NO hmrcBusinessId set (simulates singleton created before user syncs)
                UiQuarterlySubmissionService serviceWithDataStore =
                        new UiQuarterlySubmissionService(HttpClient.newHttpClient(), dataStore);
                serviceWithDataStore.setNino(TEST_NINO);
                // Deliberately NOT calling serviceWithDataStore.setHmrcBusinessId(...)

                // User syncs HMRC profile - business ID saved to SQLite
                dataStore.saveHmrcBusinessId("XAIS12345678901");

                // Create review data and attempt submission
                QuarterlyReviewData data = createReviewData(
                        Quarter.Q1,
                        new BigDecimal("5000.00"),
                        BigDecimal.ZERO,
                        new EnumMap<>(ExpenseCategory.class)
                );

                // Submit should NOT throw HMRC_PROFILE_NOT_SYNCED - it should get past validation
                // and fail on OAuth instead (which is expected since we're not connected)
                assertThatThrownBy(() -> serviceWithDataStore.submit(data, Instant.now(), "a".repeat(64)))
                        .isInstanceOf(SubmissionException.class)
                        .satisfies(ex -> {
                            String message = ex.getMessage();
                            assertThat(message).doesNotContain("HMRC_PROFILE_NOT_SYNCED");
                        });
            } finally {
                SqliteTestSupport.tearDownTestEnvironment();
            }
        }

        @Test
        @DisplayName("should still throw HMRC_PROFILE_NOT_SYNCED when HMRC business ID is not in SQLite either")
        void shouldStillThrowWhenHmrcBusinessIdNotInSqliteEither() {
            SqliteTestSupport.enableTestMode();
            try {
                SqliteDataStore dataStore = SqliteDataStore.getInstance();

                // Create service with no hmrcBusinessId and no hmrcBusinessId in SQLite
                UiQuarterlySubmissionService serviceWithDataStore =
                        new UiQuarterlySubmissionService(HttpClient.newHttpClient(), dataStore);
                serviceWithDataStore.setNino(TEST_NINO);

                QuarterlyReviewData data = createReviewData(
                        Quarter.Q1,
                        new BigDecimal("5000.00"),
                        BigDecimal.ZERO,
                        new EnumMap<>(ExpenseCategory.class)
                );

                assertThatThrownBy(() -> serviceWithDataStore.submit(data, Instant.now(), "a".repeat(64)))
                        .isInstanceOf(SubmissionException.class)
                        .hasMessageContaining("HMRC_PROFILE_NOT_SYNCED");
            } finally {
                SqliteTestSupport.tearDownTestEnvironment();
            }
        }

        @Test
        @DisplayName("should update in-memory hmrcBusinessId after reloading from SQLite")
        void shouldUpdateInMemoryHmrcBusinessIdAfterReload() {
            SqliteTestSupport.enableTestMode();
            try {
                SqliteDataStore dataStore = SqliteDataStore.getInstance();

                UiQuarterlySubmissionService serviceWithDataStore =
                        new UiQuarterlySubmissionService(HttpClient.newHttpClient(), dataStore);

                // Initially no hmrcBusinessId
                assertThat(serviceWithDataStore.getHmrcBusinessId()).isNull();

                // Save hmrcBusinessId to SQLite
                dataStore.saveHmrcBusinessId("XAIS12345678901");

                // Trigger validation (will fail on OAuth, but hmrcBusinessId should be reloaded)
                serviceWithDataStore.setNino(TEST_NINO);
                QuarterlyReviewData data = createReviewData(
                        Quarter.Q1,
                        new BigDecimal("5000.00"),
                        BigDecimal.ZERO,
                        new EnumMap<>(ExpenseCategory.class)
                );

                try {
                    serviceWithDataStore.submit(data, Instant.now(), "a".repeat(64));
                } catch (SubmissionException e) {
                    // Expected - will fail on OAuth, not HMRC_PROFILE_NOT_SYNCED
                    assertThat(e.getMessage()).doesNotContain("HMRC_PROFILE_NOT_SYNCED");
                }

                // After validation ran, in-memory hmrcBusinessId should be updated
                assertThat(serviceWithDataStore.getHmrcBusinessId()).isEqualTo("XAIS12345678901");
            } finally {
                SqliteTestSupport.tearDownTestEnvironment();
            }
        }
    }

    @Nested
    @DisplayName("Pre-Submission HMRC Business ID Validation (SE-10F-003)")
    class PreSubmissionBusinessIdValidationTests {

        private QuarterlyReviewData validData;
        private final Instant declarationTime = Instant.now();
        private final String declarationHash = "a".repeat(64);

        @BeforeEach
        void setUpValidData() {
            validData = createReviewData(
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    BigDecimal.ZERO,
                    new EnumMap<>(ExpenseCategory.class)
            );
        }

        @Test
        @DisplayName("TC-1: submit() with null hmrcBusinessId throws SubmissionException with HMRC_PROFILE_NOT_SYNCED")
        void shouldThrowWhenHmrcBusinessIdIsNull() {
            service.setNino(TEST_NINO);
            // hmrcBusinessId left as null (default)

            assertThatThrownBy(() -> service.submit(validData, declarationTime, declarationHash))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("HMRC_PROFILE_NOT_SYNCED");
        }

        @Test
        @DisplayName("TC-2: submit() with empty/blank hmrcBusinessId throws SubmissionException with HMRC_PROFILE_NOT_SYNCED")
        void shouldThrowWhenHmrcBusinessIdIsBlank() {
            service.setNino(TEST_NINO);
            service.setHmrcBusinessId("   ");

            assertThatThrownBy(() -> service.submit(validData, declarationTime, declarationHash))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("HMRC_PROFILE_NOT_SYNCED");
        }

        @Test
        @DisplayName("TC-3: submit() with UUID-format businessId throws SubmissionException with HMRC_PROFILE_NOT_SYNCED")
        void shouldThrowWhenHmrcBusinessIdIsUuidFormat() {
            service.setNino(TEST_NINO);
            service.setHmrcBusinessId("550e8400-e29b-41d4-a716-446655440000");

            assertThatThrownBy(() -> service.submit(validData, declarationTime, declarationHash))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("HMRC_PROFILE_NOT_SYNCED")
                    .hasMessageContaining("invalid");
        }

        @Test
        @DisplayName("TC-4: submit() with valid XAIS format passes HMRC business ID validation")
        void shouldPassValidationWithValidHmrcBusinessId() {
            service.setNino(TEST_NINO);
            service.setHmrcBusinessId("XAIS12345678901");

            // Should pass HMRC business ID validation and fail later at OAuth step
            assertThatThrownBy(() -> service.submit(validData, declarationTime, declarationHash))
                    .isInstanceOf(SubmissionException.class)
                    .satisfies(ex -> {
                        String message = ex.getMessage();
                        assertThat(message).doesNotContain("HMRC_PROFILE_NOT_SYNCED");
                    });
        }

        @Test
        @DisplayName("TC-5: submit() with hmrcBusinessId null but available in SQLite reloads and passes validation")
        void shouldReloadFromSqliteAndPassValidation() {
            SqliteTestSupport.enableTestMode();
            try {
                SqliteDataStore testDataStore = SqliteDataStore.getInstance();

                UiQuarterlySubmissionService serviceWithDataStore =
                        new UiQuarterlySubmissionService(HttpClient.newHttpClient(), testDataStore);
                serviceWithDataStore.setNino(TEST_NINO);
                // Deliberately NOT setting hmrcBusinessId in memory

                // Save valid HMRC business ID to SQLite
                testDataStore.saveHmrcBusinessId("XAIS12345678901");

                // Should reload from SQLite, pass validation, and fail later at OAuth
                assertThatThrownBy(() -> serviceWithDataStore.submit(validData, declarationTime, declarationHash))
                        .isInstanceOf(SubmissionException.class)
                        .satisfies(ex -> {
                            String message = ex.getMessage();
                            assertThat(message).doesNotContain("HMRC_PROFILE_NOT_SYNCED");
                        });

                // Verify in-memory value was updated
                assertThat(serviceWithDataStore.getHmrcBusinessId()).isEqualTo("XAIS12345678901");
            } finally {
                SqliteTestSupport.tearDownTestEnvironment();
            }
        }

        @Test
        @DisplayName("TC-3b: submit() with lowercase hmrc business id throws SubmissionException")
        void shouldThrowWhenHmrcBusinessIdIsLowercase() {
            service.setNino(TEST_NINO);
            service.setHmrcBusinessId("xais12345678901");

            assertThatThrownBy(() -> service.submit(validData, declarationTime, declarationHash))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("HMRC_PROFILE_NOT_SYNCED");
        }

        @Test
        @DisplayName("TC-3c: submit() with too-short hmrc business id throws SubmissionException")
        void shouldThrowWhenHmrcBusinessIdIsTooShort() {
            service.setNino(TEST_NINO);
            service.setHmrcBusinessId("XAIS1234");

            assertThatThrownBy(() -> service.submit(validData, declarationTime, declarationHash))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("HMRC_PROFILE_NOT_SYNCED");
        }
    }

    @Nested
    @DisplayName("Accept Header Versioning")
    class AcceptHeaderTests {

        @Test
        @DisplayName("should use version 5.0 for tax year 2024 (period endpoint)")
        void shouldUseVersion5ForTaxYear2024() {
            // Tax year 2024-25 and earlier: v5.0
            TaxYear taxYear2024 = TaxYear.of(2024);
            String acceptHeader = service.getAcceptHeader(taxYear2024);
            assertThat(acceptHeader).isEqualTo("application/vnd.hmrc.5.0+json");
        }

        @Test
        @DisplayName("should use version 5.0 for tax year 2023 (period endpoint)")
        void shouldUseVersion5ForTaxYear2023() {
            // Tax year 2023-24 and earlier: v5.0
            TaxYear taxYear2023 = TaxYear.of(2023);
            String acceptHeader = service.getAcceptHeader(taxYear2023);
            assertThat(acceptHeader).isEqualTo("application/vnd.hmrc.5.0+json");
        }

        @Test
        @DisplayName("should use version 7.0 for tax year 2025 (cumulative endpoint)")
        void shouldUseVersion7ForTaxYear2025() {
            // Tax year 2025-26 onwards: v7.0 for cumulative endpoint
            TaxYear taxYear2025 = TaxYear.of(2025);
            String acceptHeader = service.getAcceptHeader(taxYear2025);
            assertThat(acceptHeader).isEqualTo("application/vnd.hmrc.7.0+json");
        }

        @Test
        @DisplayName("should use version 7.0 for tax year 2026 (cumulative endpoint)")
        void shouldUseVersion7ForTaxYear2026() {
            // Tax year 2026-27 onwards: v7.0 for cumulative endpoint
            TaxYear taxYear2026 = TaxYear.of(2026);
            String acceptHeader = service.getAcceptHeader(taxYear2026);
            assertThat(acceptHeader).isEqualTo("application/vnd.hmrc.7.0+json");
        }

        @Test
        @DisplayName("should use version 7.0 for far future tax year")
        void shouldUseVersion7ForFutureTaxYear() {
            // Far future tax years: v7.0
            TaxYear taxYear2030 = TaxYear.of(2030);
            String acceptHeader = service.getAcceptHeader(taxYear2030);
            assertThat(acceptHeader).isEqualTo("application/vnd.hmrc.7.0+json");
        }

        @Test
        @DisplayName("should default to version 5.0 when tax year is null")
        void shouldDefaultToVersion5WhenTaxYearIsNull() {
            // Null tax year defaults to v5.0 for safety with older tax years
            String acceptHeader = service.getAcceptHeader(null);
            assertThat(acceptHeader).isEqualTo("application/vnd.hmrc.5.0+json");
        }
    }

    @Nested
    @DisplayName("SubmissionStrategyFactory")
    class SubmissionStrategyFactoryTests {

        private SubmissionStrategyFactory factory;

        @BeforeEach
        void setUpFactory() {
            factory = new SubmissionStrategyFactory();
        }

        @Test
        @DisplayName("should return PeriodSubmissionStrategy for tax year 2024")
        void shouldReturnPeriodStrategyForTaxYear2024() {
            SubmissionStrategy strategy = factory.getStrategy(TaxYear.of(2024));

            assertThat(strategy).isInstanceOf(PeriodSubmissionStrategy.class);
            assertThat(strategy.getHttpMethod()).isEqualTo("POST");
        }

        @Test
        @DisplayName("should return CumulativeSubmissionStrategy for tax year 2025")
        void shouldReturnCumulativeStrategyForTaxYear2025() {
            SubmissionStrategy strategy = factory.getStrategy(TaxYear.of(2025));

            assertThat(strategy).isInstanceOf(CumulativeSubmissionStrategy.class);
            assertThat(strategy.getHttpMethod()).isEqualTo("PUT");
        }

        @Test
        @DisplayName("should return CumulativeSubmissionStrategy for tax year 2026")
        void shouldReturnCumulativeStrategyForTaxYear2026() {
            SubmissionStrategy strategy = factory.getStrategy(TaxYear.of(2026));

            assertThat(strategy).isInstanceOf(CumulativeSubmissionStrategy.class);
        }

        @Test
        @DisplayName("should return CumulativeSubmissionStrategy for far future tax year")
        void shouldReturnCumulativeStrategyForFutureTaxYear() {
            SubmissionStrategy strategy = factory.getStrategy(TaxYear.of(2030));

            assertThat(strategy).isInstanceOf(CumulativeSubmissionStrategy.class);
        }

        @Test
        @DisplayName("should return default strategy when tax year is null")
        void shouldReturnDefaultStrategyWhenTaxYearIsNull() {
            SubmissionStrategy strategy = factory.getStrategy(null);

            // Default is PeriodSubmissionStrategy for safety with older tax years
            assertThat(strategy).isInstanceOf(PeriodSubmissionStrategy.class);
        }
    }

    @Nested
    @DisplayName("PeriodSubmissionStrategy")
    class PeriodSubmissionStrategyTests {

        private PeriodSubmissionStrategy strategy;

        @BeforeEach
        void setUpStrategy() {
            strategy = new PeriodSubmissionStrategy();
        }

        @Test
        @DisplayName("should serialize with periodDates wrapper")
        void shouldSerializeWithPeriodDates() throws Exception {
            QuarterlyReviewData data = createReviewDataForTaxYear(
                    TaxYear.of(2024),
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    new BigDecimal("800.00"),
                    createSampleExpenses()
            );

            String json = strategy.serializeRequest(data);

            // Should have periodDates wrapper
            assertThat(json).contains("\"periodDates\"");
            assertThat(json).contains("\"periodStartDate\"");
            assertThat(json).contains("\"periodEndDate\"");
            assertThat(json).contains("\"periodIncome\"");
            assertThat(json).contains("\"periodExpenses\"");
        }

        @Test
        @DisplayName("should build correct endpoint URL")
        void shouldBuildCorrectEndpointUrl() {
            String url = strategy.buildEndpointUrl(
                    "https://test-api.service.hmrc.gov.uk",
                    TEST_NINO,
                    BUSINESS_ID.toString(),
                    TaxYear.of(2024)
            );

            assertThat(url).isEqualTo(
                    "https://test-api.service.hmrc.gov.uk/individuals/business/self-employment/"
                    + TEST_NINO + "/" + BUSINESS_ID + "/period"
            );
        }

        @Test
        @DisplayName("should throw for null reviewData")
        void shouldThrowForNullData() {
            assertThatThrownBy(() -> strategy.serializeRequest(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reviewData");
        }
    }

    @Nested
    @DisplayName("CumulativeSubmissionStrategy")
    class CumulativeSubmissionStrategyTests {

        private CumulativeSubmissionStrategy strategy;

        @BeforeEach
        void setUpStrategy() {
            strategy = new CumulativeSubmissionStrategy();
        }

        @Test
        @DisplayName("should serialize WITHOUT periodDates wrapper")
        void shouldSerializeWithoutPeriodDates() throws Exception {
            QuarterlyReviewData data = createReviewDataForTaxYear(
                    TaxYear.of(2025),
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    new BigDecimal("800.00"),
                    createSampleExpenses()
            );

            String json = strategy.serializeRequest(data);

            // Should NOT have periodDates wrapper
            assertThat(json).doesNotContain("\"periodDates\"");
            assertThat(json).doesNotContain("\"periodStartDate\"");
            assertThat(json).doesNotContain("\"periodEndDate\"");
            // But should have income and expenses
            assertThat(json).contains("\"periodIncome\"");
            assertThat(json).contains("\"periodExpenses\"");
            assertThat(json).contains("\"turnover\"");
        }

        @Test
        @DisplayName("should build correct endpoint URL with taxYear query param")
        void shouldBuildCorrectEndpointUrl() {
            String url = strategy.buildEndpointUrl(
                    "https://test-api.service.hmrc.gov.uk",
                    TEST_NINO,
                    BUSINESS_ID.toString(),
                    TaxYear.of(2025)
            );

            assertThat(url).isEqualTo(
                    "https://test-api.service.hmrc.gov.uk/individuals/business/self-employment/"
                    + TEST_NINO + "/" + BUSINESS_ID + "/cumulative?taxYear=2025-26"
            );
        }

        @Test
        @DisplayName("should map expense categories correctly")
        void shouldMapExpenseCategories() throws Exception {
            Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.COST_OF_GOODS, new CategorySummary(new BigDecimal("100.00"), 2));
            expenses.put(ExpenseCategory.STAFF_COSTS, new CategorySummary(new BigDecimal("200.00"), 3));
            expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("50.00"), 1));
            expenses.put(ExpenseCategory.TRAVEL_MILEAGE, new CategorySummary(new BigDecimal("30.00"), 1));

            BigDecimal totalExpenses = expenses.values().stream()
                    .map(CategorySummary::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            QuarterlyReviewData data = createReviewDataForTaxYear(
                    TaxYear.of(2025),
                    Quarter.Q2,
                    new BigDecimal("10000.00"),
                    totalExpenses,
                    expenses
            );

            String json = strategy.serializeRequest(data);

            // Verify key expense mappings in JSON
            assertThat(json).contains("\"costOfGoodsBought\"");
            assertThat(json).contains("\"staffCosts\"");
            assertThat(json).contains("\"travelCosts\"");
        }

        @Test
        @DisplayName("should throw for null reviewData")
        void shouldThrowForNullData() {
            assertThatThrownBy(() -> strategy.serializeRequest(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reviewData");
        }
    }

    @Nested
    @DisplayName("Submission Persistence (BUG-10H-001)")
    class SubmissionPersistenceTests {

        @Test
        @DisplayName("should save submission to SQLite after successful HMRC response")
        void shouldSaveSubmissionToSqliteAfterSuccess() {
            // Given: Test environment with SQLite
            SqliteTestSupport.enableTestMode();
            try {
                SqliteDataStore dataStore = SqliteDataStore.getInstance();
                UUID businessId = UUID.randomUUID();
                dataStore.ensureBusinessExists(businessId);
                dataStore.saveBusinessId(businessId);
                dataStore.saveNino(TEST_NINO);
                dataStore.saveHmrcBusinessId("XAIS12345678901");

                // Create repository to verify persistence
                SqliteSubmissionRepository repository = new SqliteSubmissionRepository(businessId);

                // Verify no submissions exist initially
                assertThat(repository.count()).isEqualTo(0);

                // When: Save a submission record (simulating post-HMRC success)
                SubmissionRecord record = new SubmissionRecord(
                    UUID.randomUUID().toString(),
                    businessId.toString(),
                    "QUARTERLY_Q1",
                    2025,
                    LocalDate.of(2025, 4, 6),
                    LocalDate.of(2025, 7, 5),
                    new BigDecimal("5000.00"),
                    new BigDecimal("800.00"),
                    new BigDecimal("4200.00"),
                    "ACCEPTED",
                    "HMRC-REF-123456",
                    null,
                    Instant.now()
                );
                repository.save(record);

                // Then: Submission should be persisted
                assertThat(repository.count()).isEqualTo(1);
                var saved = repository.findById(record.id());
                assertThat(saved).isPresent();
                assertThat(saved.get().hmrcReference()).isEqualTo("HMRC-REF-123456");
                assertThat(saved.get().status()).isEqualTo("ACCEPTED");
            } finally {
                SqliteTestSupport.tearDownTestEnvironment();
            }
        }

        @Test
        @DisplayName("should save submission with error for failed HMRC response")
        void shouldSaveSubmissionWithErrorForFailure() {
            SqliteTestSupport.enableTestMode();
            try {
                SqliteDataStore dataStore = SqliteDataStore.getInstance();
                UUID businessId = UUID.randomUUID();
                dataStore.ensureBusinessExists(businessId);
                dataStore.saveBusinessId(businessId);

                SqliteSubmissionRepository repository = new SqliteSubmissionRepository(businessId);

                // Save a rejected submission
                SubmissionRecord record = new SubmissionRecord(
                    UUID.randomUUID().toString(),
                    businessId.toString(),
                    "QUARTERLY_Q2",
                    2025,
                    LocalDate.of(2025, 7, 6),
                    LocalDate.of(2025, 10, 5),
                    new BigDecimal("3000.00"),
                    new BigDecimal("500.00"),
                    new BigDecimal("2500.00"),
                    "REJECTED",
                    null,
                    "FORMAT_VALUE: Invalid income format",
                    Instant.now()
                );
                repository.save(record);

                // Verify persistence
                var saved = repository.findById(record.id());
                assertThat(saved).isPresent();
                assertThat(saved.get().status()).isEqualTo("REJECTED");
                assertThat(saved.get().errorMessage()).isEqualTo("FORMAT_VALUE: Invalid income format");
                assertThat(saved.get().hmrcReference()).isNull();
            } finally {
                SqliteTestSupport.tearDownTestEnvironment();
            }
        }

        @Test
        @DisplayName("should create SubmissionRecord from Submission domain object using factory method")
        void shouldCreateSubmissionRecordFromDomainSubmission() {
            QuarterlyReviewData data = createReviewData(
                Quarter.Q1,
                new BigDecimal("5000.00"),
                new BigDecimal("800.00"),
                createSampleExpenses()
            );

            uk.selfemploy.common.domain.Submission domainSubmission = service.buildSubmissionRecord(
                BUSINESS_ID, data,
                SubmissionStatus.ACCEPTED, "HMRC-REF-789",
                null,
                Instant.now(), "a".repeat(64)
            );

            // Use factory method to convert domain submission to persistence record
            SubmissionRecord record = SubmissionRecord.fromDomainSubmission(domainSubmission);

            assertThat(record.type()).isEqualTo("QUARTERLY_Q1");
            assertThat(record.taxYearStart()).isEqualTo(2025);
            assertThat(record.status()).isEqualTo("ACCEPTED");
            assertThat(record.hmrcReference()).isEqualTo("HMRC-REF-789");
        }
    }

    @Nested
    @DisplayName("Tax Year Based DTO Selection via Strategy")
    class TaxYearBasedDtoSelectionTests {

        private SubmissionStrategyFactory factory;

        @BeforeEach
        void setUpFactory() {
            factory = new SubmissionStrategyFactory();
        }

        @Test
        @DisplayName("should use PeriodicUpdate format for tax year 2024")
        void shouldUsePeriodicUpdateForTaxYear2024() throws Exception {
            QuarterlyReviewData data = createReviewDataForTaxYear(
                    TaxYear.of(2024),
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    new BigDecimal("800.00"),
                    createSampleExpenses()
            );

            SubmissionStrategy strategy = factory.getStrategy(data.getTaxYear());
            String json = strategy.serializeRequest(data);

            // Tax year 2024-25: should use PeriodicUpdate with periodDates
            assertThat(json).contains("\"periodDates\"");
            assertThat(json).contains("\"periodStartDate\"");
            assertThat(json).contains("\"periodEndDate\"");
        }

        @Test
        @DisplayName("should use CumulativeSummary format for tax year 2025")
        void shouldUseCumulativeSummaryForTaxYear2025() throws Exception {
            QuarterlyReviewData data = createReviewDataForTaxYear(
                    TaxYear.of(2025),
                    Quarter.Q1,
                    new BigDecimal("5000.00"),
                    new BigDecimal("800.00"),
                    createSampleExpenses()
            );

            SubmissionStrategy strategy = factory.getStrategy(data.getTaxYear());
            String json = strategy.serializeRequest(data);

            // Tax year 2025-26+: should use CumulativeSummary WITHOUT periodDates
            assertThat(json).doesNotContain("\"periodDates\"");
            assertThat(json).doesNotContain("\"periodStartDate\"");
            assertThat(json).doesNotContain("\"periodEndDate\"");
            assertThat(json).contains("\"periodIncome\"");
            assertThat(json).contains("\"periodExpenses\"");
        }

        @Test
        @DisplayName("should use CumulativeSummary format for tax year 2026")
        void shouldUseCumulativeSummaryForTaxYear2026() throws Exception {
            QuarterlyReviewData data = createReviewDataForTaxYear(
                    TaxYear.of(2026),
                    Quarter.Q2,
                    new BigDecimal("10000.00"),
                    new BigDecimal("2000.00"),
                    createSampleExpenses()
            );

            SubmissionStrategy strategy = factory.getStrategy(data.getTaxYear());
            String json = strategy.serializeRequest(data);

            // Tax year 2026-27+: should use CumulativeSummary WITHOUT periodDates
            assertThat(json).doesNotContain("\"periodDates\"");
            assertThat(json).contains("\"periodIncome\"");
        }
    }

        // ==================== Helper Methods ====================

    private QuarterlyReviewData createReviewData(
            Quarter quarter,
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            Map<ExpenseCategory, CategorySummary> expenses) {

        TaxYear taxYear = TaxYear.of(2025);
        return createReviewDataForTaxYear(taxYear, quarter, totalIncome, totalExpenses, expenses);
    }

    private QuarterlyReviewData createReviewDataForTaxYear(
            TaxYear taxYear,
            Quarter quarter,
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            Map<ExpenseCategory, CategorySummary> expenses) {

        return QuarterlyReviewData.builder()
                .quarter(quarter)
                .taxYear(taxYear)
                .periodStart(quarter.getStartDate(taxYear))
                .periodEnd(quarter.getEndDate(taxYear))
                .totalIncome(totalIncome)
                .incomeTransactionCount(5)
                .totalExpenses(totalExpenses)
                .expenseTransactionCount(3)
                .expensesByCategory(expenses)
                .build();
    }

    private Map<ExpenseCategory, CategorySummary> createSampleExpenses() {
        Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
        expenses.put(ExpenseCategory.OFFICE_COSTS, new CategorySummary(new BigDecimal("500.00"), 5));
        expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("300.00"), 3));
        return expenses;
    }
}
