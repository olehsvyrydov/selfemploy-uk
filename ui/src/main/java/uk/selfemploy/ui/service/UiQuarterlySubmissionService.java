package uk.selfemploy.ui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.dto.PeriodicUpdate;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.service.submission.SubmissionStrategy;
import uk.selfemploy.ui.service.submission.SubmissionStrategyFactory;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.core.exception.SubmissionException;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UI-layer service for submitting quarterly updates to HMRC.
 *
 * <p>This service bridges the standalone JavaFX UI to the HMRC MTD API.
 * Since the UI doesn't use Quarkus CDI, it directly uses {@link java.net.http.HttpClient}
 * for API calls and {@link OAuthServiceFactory} for OAuth tokens.</p>
 *
 * <p>Implements the same PeriodicUpdate building logic as the core
 * {@code QuarterlySubmissionService}, mapping SA103 expense categories
 * to HMRC's expected deduction fields.</p>
 */
public class UiQuarterlySubmissionService {

    private static final Logger LOG = Logger.getLogger(UiQuarterlySubmissionService.class.getName());


    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Returns the HMRC Accept header for the Self-Employment Business API.
     *
     * <p><strong>API Version Selection:</strong></p>
     * <ul>
     *   <li>Tax year 2024-25 and earlier: v5.0 (uses POST /period endpoint)</li>
     *   <li>Tax year 2025-26 onwards: v7.0 (uses PUT /cumulative endpoint)</li>
     * </ul>
     *
     * <p>The API version aligns with the endpoint change from periodic to cumulative
     * reporting introduced for MTD ITSA from tax year 2025-26.</p>
     *
     * @param taxYear the tax year for the submission
     * @return the Accept header value (v5.0 for 2024-25 and earlier, v7.0 for 2025-26 onwards)
     */
    String getAcceptHeader(TaxYear taxYear) {
        // Tax year 2025-26 onwards uses v7.0 with cumulative endpoint
        // Tax year 2024-25 and earlier uses v5.0 with period endpoint
        if (taxYear != null && taxYear.startYear() >= 2025) {
            return "application/vnd.hmrc.7.0+json";
        }
        return "application/vnd.hmrc.5.0+json";
    }

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final SqliteDataStore dataStore;
    private final SubmissionStrategyFactory strategyFactory;

    private String nino;
    private String hmrcBusinessId;

    /**
     * Creates a new UiQuarterlySubmissionService with default HTTP client.
     * Uses the singleton SqliteDataStore for NINO reload.
     */
    public UiQuarterlySubmissionService() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build(),
                SqliteDataStore.getInstance());
    }

    /**
     * Creates a new UiQuarterlySubmissionService with a custom HTTP client.
     * Uses the singleton SqliteDataStore for NINO reload.
     * Visible for testing.
     *
     * @param httpClient the HTTP client to use for HMRC API calls
     */
    UiQuarterlySubmissionService(HttpClient httpClient) {
        this(httpClient, SqliteDataStore.getInstance());
    }

    /**
     * Creates a new UiQuarterlySubmissionService with a custom HTTP client and data store.
     * Visible for testing.
     *
     * @param httpClient the HTTP client to use for HMRC API calls
     * @param dataStore  the SQLite data store for reloading NINO and business ID at submission time
     */
    UiQuarterlySubmissionService(HttpClient httpClient, SqliteDataStore dataStore) {
        this(httpClient, dataStore, new SubmissionStrategyFactory());
    }

    /**
     * Creates a new UiQuarterlySubmissionService with all dependencies injected.
     * Visible for testing.
     *
     * @param httpClient the HTTP client to use for HMRC API calls
     * @param dataStore  the SQLite data store for reloading NINO and business ID at submission time
     * @param strategyFactory the factory for selecting submission strategies based on tax year
     */
    UiQuarterlySubmissionService(HttpClient httpClient, SqliteDataStore dataStore, SubmissionStrategyFactory strategyFactory) {
        this.httpClient = httpClient;
        this.dataStore = dataStore;
        this.strategyFactory = strategyFactory;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ==================== NINO/Business ID Management ====================

    /**
     * Sets the National Insurance Number for submissions.
     * The NINO is normalised to uppercase.
     *
     * @param nino the NINO (e.g., "QQ123456C")
     */
    public void setNino(String nino) {
        this.nino = nino != null ? nino.toUpperCase() : null;
    }

    /**
     * Gets the configured NINO.
     *
     * @return the NINO, or null if not set
     */
    public String getNino() {
        return nino;
    }

    /**
     * Sets the HMRC-assigned business ID for API submissions.
     *
     * @param hmrcBusinessId the HMRC business ID (e.g., "XAIS12345678901")
     */
    public void setHmrcBusinessId(String hmrcBusinessId) {
        this.hmrcBusinessId = hmrcBusinessId;
    }

    /**
     * Gets the configured HMRC business ID.
     *
     * @return the HMRC business ID, or null if not set
     */
    public String getHmrcBusinessId() {
        return hmrcBusinessId;
    }

    // ==================== Submission ====================

    /**
     * Submits quarterly data to HMRC.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Validates NINO, business ID, and declaration info</li>
     *   <li>Gets a valid OAuth bearer token</li>
     *   <li>Builds a PeriodicUpdate DTO from the review data</li>
     *   <li>Sends HTTP POST to HMRC Self-Employment API</li>
     *   <li>Parses response and returns a Submission record</li>
     * </ol>
     *
     * @param reviewData            the reviewed quarterly data
     * @param declarationAcceptedAt when the user accepted the declaration (UTC)
     * @param declarationTextHash   SHA-256 hash of the confirmation texts
     * @return the Submission record with HMRC reference on success
     * @throws SubmissionException if submission fails
     */
    public Submission submit(QuarterlyReviewData reviewData, Instant declarationAcceptedAt, String declarationTextHash) {
        // Validate prerequisites
        validateSubmissionPrerequisites(reviewData, declarationAcceptedAt, declarationTextHash);

        // Select the appropriate strategy based on tax year
        SubmissionStrategy strategy = strategyFactory.getStrategy(reviewData.getTaxYear());
        LOG.info("Selected submission strategy: " + strategy.getDescription());

        // Serialize to JSON using the strategy
        String jsonBody;
        try {
            jsonBody = strategy.serializeRequest(reviewData);
            // Debug logging at FINE level to avoid exposing financial data in production logs
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("HMRC Submission JSON Body: " + jsonBody);
            }
        } catch (Exception e) {
            throw new SubmissionException("Failed to serialize submission data", e);
        }

        // Build API URL using the strategy
        String apiBaseUrl = System.getProperty("HMRC_API_BASE_URL", "https://test-api.service.hmrc.gov.uk");
        boolean isSandbox = apiBaseUrl.contains("test-api");
        String url = strategy.buildEndpointUrl(apiBaseUrl, nino, hmrcBusinessId, reviewData.getTaxYear());

        LOG.info("Submitting quarterly update to HMRC: " + url);

        // Try submission with automatic token refresh on 401
        return submitWithRetry(url, jsonBody, strategy.getHttpMethod(), isSandbox,
                reviewData, declarationAcceptedAt, declarationTextHash);
    }

    /**
     * Submits the request with automatic retry on 401 (Invalid Credentials).
     * If the first attempt returns 401, refreshes the OAuth token and retries once.
     */
    private Submission submitWithRetry(String url, String jsonBody, String httpMethod, boolean isSandbox,
                                        QuarterlyReviewData reviewData, Instant declarationAcceptedAt,
                                        String declarationTextHash) {
        // First attempt
        String bearerToken = getBearerToken();
        HttpResponse<String> response = executeRequest(url, jsonBody, httpMethod, isSandbox, bearerToken, reviewData);

        // Check for 401 - token might be invalid even though it appeared valid
        if (response.statusCode() == 401) {
            LOG.info("Received 401 Unauthorized - attempting token refresh and retry");
            try {
                // Force token refresh
                bearerToken = getBearerToken(true);
                // Retry the request
                response = executeRequest(url, jsonBody, httpMethod, isSandbox, bearerToken, reviewData);

                if (response.statusCode() == 401) {
                    // Still failing after refresh - session is truly expired
                    LOG.warning("Still receiving 401 after token refresh - session expired");
                    SqliteDataStore.getInstance().clearOAuthTokens();
                    throw new SubmissionException(
                            "SESSION_EXPIRED: Your HMRC session has expired. Please reconnect via the HMRC Submission page.");
                }
            } catch (SubmissionException e) {
                throw e;
            } catch (Exception e) {
                LOG.warning("Token refresh failed: " + e.getMessage());
                SqliteDataStore.getInstance().clearOAuthTokens();
                throw new SubmissionException(
                        "SESSION_EXPIRED: Your HMRC session has expired. Please reconnect via the HMRC Submission page.", e);
            }
        }

        return handleResponse(response, reviewData, declarationAcceptedAt, declarationTextHash);
    }

    /**
     * Executes the HTTP request to HMRC.
     */
    private HttpResponse<String> executeRequest(String url, String jsonBody, String httpMethod,
                                                 boolean isSandbox, String bearerToken,
                                                 QuarterlyReviewData reviewData) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Content-Type", "application/json")
                    .header("Accept", getAcceptHeader(reviewData.getTaxYear()));

            // Add fraud prevention headers (required by HMRC for ALL MTD API calls)
            addFraudPreventionHeaders(requestBuilder);

            // Sandbox mode: Don't add Gov-Test-Scenario header to get default success response.
            if (isSandbox) {
                LOG.info("Sandbox mode: Using default success scenario (no Gov-Test-Scenario header)");
            }

            // Use HTTP method from strategy (PUT for cumulative, POST for period)
            if ("PUT".equals(httpMethod)) {
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
                LOG.info("Using PUT method for cumulative endpoint (tax year 2025-26+)");
            } else {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
                LOG.info("Using POST method for period endpoint (tax year 2024-25 or earlier)");
            }

            HttpRequest request = requestBuilder.build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (java.net.http.HttpTimeoutException e) {
            LOG.log(Level.WARNING, "HMRC API request timed out", e);
            throw new SubmissionException("HMRC API request timed out. Please try again.", e, true);
        } catch (java.io.IOException e) {
            LOG.log(Level.WARNING, "Network error during HMRC submission", e);
            throw new SubmissionException("Network error: " + e.getMessage() + ". Please check your connection.", e, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SubmissionException("Submission was interrupted", e, true);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error during HMRC submission", e);
            throw new SubmissionException("Unexpected error: " + e.getMessage(), e);
        }
    }

    // ==================== PeriodicUpdate Building ====================

    /**
     * Builds a PeriodicUpdate DTO from the quarterly review data.
     *
     * <p>Maps the UI expense categories to the HMRC SA103 deduction fields.
     * This mirrors the mapping in the core QuarterlySubmissionService.</p>
     *
     * @param reviewData the review data containing income and expense totals
     * @return the PeriodicUpdate DTO ready for HMRC API submission
     * @throws IllegalArgumentException if reviewData is null
     */
    public PeriodicUpdate buildPeriodicUpdate(QuarterlyReviewData reviewData) {
        if (reviewData == null) {
            throw new IllegalArgumentException("reviewData must not be null");
        }

        // Income as turnover
        PeriodicUpdate.PeriodIncome periodIncome =
                PeriodicUpdate.PeriodIncome.ofTurnover(reviewData.getTotalIncome());

        // Expenses mapped to SA103 categories
        Map<ExpenseCategory, CategorySummary> expenses = reviewData.getExpensesByCategory();

        PeriodicUpdate.PeriodExpenses periodExpenses = PeriodicUpdate.PeriodExpenses.builder()
                .costOfGoodsBought(getCategoryAmount(expenses, ExpenseCategory.COST_OF_GOODS))
                .cisPaymentsToSubcontractors(getCategoryAmount(expenses, ExpenseCategory.SUBCONTRACTOR_COSTS))
                .staffCosts(getCategoryAmount(expenses, ExpenseCategory.STAFF_COSTS))
                .travelCosts(sumCategoryAmounts(expenses, ExpenseCategory.TRAVEL, ExpenseCategory.TRAVEL_MILEAGE))
                .premisesRunningCosts(getCategoryAmount(expenses, ExpenseCategory.PREMISES))
                .maintenanceCosts(getCategoryAmount(expenses, ExpenseCategory.REPAIRS))
                .adminCosts(getCategoryAmount(expenses, ExpenseCategory.OFFICE_COSTS))
                .advertisingCosts(getCategoryAmount(expenses, ExpenseCategory.ADVERTISING))
                .businessEntertainmentCosts(getCategoryAmount(expenses, ExpenseCategory.BUSINESS_ENTERTAINMENT))
                .interest(getCategoryAmount(expenses, ExpenseCategory.INTEREST))
                .financialCharges(getCategoryAmount(expenses, ExpenseCategory.FINANCIAL_CHARGES))
                .badDebt(getCategoryAmount(expenses, ExpenseCategory.BAD_DEBTS))
                .professionalFees(getCategoryAmount(expenses, ExpenseCategory.PROFESSIONAL_FEES))
                .depreciation(getCategoryAmount(expenses, ExpenseCategory.DEPRECIATION))
                .other(sumCategoryAmounts(expenses, ExpenseCategory.OTHER_EXPENSES, ExpenseCategory.HOME_OFFICE_SIMPLIFIED))
                .build();

        return new PeriodicUpdate(
                reviewData.getPeriodStart(),
                reviewData.getPeriodEnd(),
                periodIncome,
                periodExpenses
        );
    }

    /**
     * Serializes a PeriodicUpdate to JSON.
     *
     * @param periodicUpdate the PeriodicUpdate to serialize
     * @return JSON string
     * @throws Exception if serialization fails
     */
    public String serializePeriodicUpdate(PeriodicUpdate periodicUpdate) throws Exception {
        return objectMapper.writeValueAsString(periodicUpdate);
    }

    // ==================== Response Handling ====================

    /**
     * Handles the HTTP response from HMRC.
     * BUG-10H-001: Saves submission to SQLite for history persistence.
     */
    private Submission handleResponse(HttpResponse<String> response,
                                       QuarterlyReviewData reviewData,
                                       Instant declarationAcceptedAt,
                                       String declarationTextHash) {
        int statusCode = response.statusCode();
        String body = response.body();

        LOG.info("HMRC response: status=" + statusCode + ", bodyLength=" + (body != null ? body.length() : 0));

        if (statusCode >= 200 && statusCode < 300) {
            // Success - parse HMRC reference from response
            String hmrcReference = parseHmrcReference(body);
            LOG.info("Submission accepted by HMRC: reference=" + hmrcReference);

            Submission submission = buildSubmissionRecord(
                    CoreServiceFactory.getDefaultBusinessId(), reviewData,
                    SubmissionStatus.ACCEPTED, hmrcReference,
                    null,
                    declarationAcceptedAt, declarationTextHash
            );

            saveSubmissionToSqlite(submission);

            return submission;
        }

        // Error response
        String errorMessage = parseErrorMessage(body, statusCode);
        LOG.warning("HMRC rejected submission: " + errorMessage);

        boolean isRetryable = statusCode >= 500 || statusCode == 408 || statusCode == 429;

        if (isRetryable) {
            throw new SubmissionException(errorMessage, null, true);
        } else {
            throw new SubmissionException(errorMessage);
        }
    }

    /**
     * Parses the HMRC reference from the response body.
     */
    private String parseHmrcReference(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            var node = objectMapper.readTree(body);
            if (node.has("id")) {
                return node.get("id").asText();
            }
            if (node.has("transactionReference")) {
                return node.get("transactionReference").asText();
            }
            // Some HMRC responses use the "periodId" field
            if (node.has("periodId")) {
                return node.get("periodId").asText();
            }
        } catch (Exception e) {
            LOG.warning("Could not parse HMRC reference from response: " + e.getMessage());
        }
        return null;
    }

    /**
     * Parses the error message from an HMRC error response.
     */
    private String parseErrorMessage(String body, int statusCode) {
        if (body != null && !body.isBlank()) {
            try {
                var node = objectMapper.readTree(body);
                if (node.has("code") && node.has("message")) {
                    return node.get("code").asText() + ": " + node.get("message").asText();
                }
                if (node.has("message")) {
                    return node.get("message").asText();
                }
                // HMRC error array format
                if (node.has("errors") && node.get("errors").isArray()) {
                    var errors = node.get("errors");
                    if (!errors.isEmpty()) {
                        var firstError = errors.get(0);
                        if (firstError.has("code") && firstError.has("message")) {
                            return firstError.get("code").asText() + ": " + firstError.get("message").asText();
                        }
                    }
                }
            } catch (Exception e) {
                LOG.fine("Could not parse error response: " + e.getMessage());
            }
        }
        return "HMRC_ERROR_" + statusCode + ": Submission failed with HTTP status " + statusCode;
    }

    // ==================== Submission Persistence ====================

    /**
     * Saves a submission to SQLite for history persistence.
     *
     * @param submission The domain submission to persist
     */
    private void saveSubmissionToSqlite(Submission submission) {
        try {
            UUID businessId = submission.businessId();
            if (businessId == null) {
                LOG.warning("Cannot save submission: businessId is null");
                return;
            }

            SqliteSubmissionRepository repository = new SqliteSubmissionRepository(businessId);
            SubmissionRecord record = SubmissionRecord.fromDomainSubmission(submission);

            repository.save(record);
            LOG.info("Submission saved to SQLite: id=" + record.id() + ", reference=" + record.hmrcReference());

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to save submission to SQLite (non-fatal): " + e.getMessage(), e);
        }
    }

    // ==================== Submission Record Building ====================

    /**
     * Builds a Submission domain record from the response data.
     * Visible for testing.
     *
     * @param businessId            the business ID
     * @param reviewData            the quarterly review data
     * @param status                the submission status (ACCEPTED/REJECTED)
     * @param hmrcReference         the HMRC reference (null if rejected)
     * @param errorMessage          the error message (null if accepted)
     * @param declarationAcceptedAt when the user accepted the declaration
     * @param declarationTextHash   SHA-256 hash of the declaration text
     * @return the Submission domain record
     */
    Submission buildSubmissionRecord(UUID businessId,
                                      QuarterlyReviewData reviewData,
                                      SubmissionStatus status,
                                      String hmrcReference,
                                      String errorMessage,
                                      Instant declarationAcceptedAt,
                                      String declarationTextHash) {
        Quarter quarter = reviewData.getQuarter();
        SubmissionType type = switch (quarter) {
            case Q1 -> SubmissionType.QUARTERLY_Q1;
            case Q2 -> SubmissionType.QUARTERLY_Q2;
            case Q3 -> SubmissionType.QUARTERLY_Q3;
            case Q4 -> SubmissionType.QUARTERLY_Q4;
        };

        return new Submission(
                UUID.randomUUID(),
                businessId,
                type,
                reviewData.getTaxYear(),
                reviewData.getPeriodStart(),
                reviewData.getPeriodEnd(),
                reviewData.getTotalIncome(),
                reviewData.getTotalExpenses(),
                reviewData.getNetProfit(),
                status,
                hmrcReference,
                errorMessage,
                Instant.now(),
                Instant.now(),
                declarationAcceptedAt,
                declarationTextHash,
                null,  // UTR (not yet stored in UI layer)
                nino
        );
    }

    // ==================== URL Building ====================

    /**
     * Builds the HMRC Self-Employment periodic update API URL.
     *
     * <p>HMRC MTD API endpoints differ based on tax year:</p>
     * <ul>
     *   <li>Tax year 2024-25 and earlier (v5.0): POST to /period (no taxYear query param, dates in request body)</li>
     *   <li>Tax year 2025-26 onwards (v7.0): PUT to /cumulative?taxYear=YYYY-YY</li>
     * </ul>
     *
     * <p>Visible for testing.</p>
     *
     * @param apiBaseUrl the HMRC API base URL
     * @param nino       the National Insurance Number
     * @param businessId the business ID
     * @param taxYear    the tax year for the submission
     * @return the full API URL
     */
    String getApiUrl(String apiBaseUrl, String nino, String businessId, TaxYear taxYear) {
        String basePath = apiBaseUrl + "/individuals/business/self-employment/" + nino + "/" + businessId;

        // Use cumulative endpoint for 2025-26+, period endpoint for earlier years
        // For v5.0 (2024-25 and earlier), tax year is determined from periodDates in request body
        // For v7.0 (2025-26+), tax year is a query parameter
        if (taxYear != null && taxYear.startYear() >= 2025) {
            String taxYearStr = taxYear.hmrcFormat();
            return basePath + "/cumulative?taxYear=" + taxYearStr;
        } else {
            // v5.0 API - no taxYear query parameter, dates in periodDates object in body
            return basePath + "/period";
        }
    }

    // ==================== OAuth Token ====================

    /**
     * Gets a valid OAuth bearer token from the OAuth service.
     * If tokens appear expired or near-expiry, attempts refresh first.
     *
     * @return the access token string
     * @throws SubmissionException if not connected or tokens are expired
     */
    private String getBearerToken() {
        return getBearerToken(false);
    }

    /**
     * Gets a valid OAuth bearer token, optionally forcing a refresh.
     *
     * @param forceRefresh if true, attempts to refresh even if tokens appear valid
     * @return the access token string
     * @throws SubmissionException if not connected or tokens are expired
     */
    private String getBearerToken(boolean forceRefresh) {
        try {
            HmrcOAuthService oauthService = OAuthServiceFactory.getOAuthService();

            if (!oauthService.isConnected()) {
                throw new SubmissionException(
                        "NOT_CONNECTED: Not connected to HMRC. Please connect first via the HMRC Submission page.");
            }

            OAuthTokens tokens = oauthService.getCurrentTokens();
            if (tokens == null) {
                throw new SubmissionException(
                        "TOKEN_EXPIRED: Your HMRC session has expired. Please reconnect via the HMRC Submission page.");
            }

            // Refresh if expired, near-expiry (< 5 min), or forced
            if (forceRefresh || tokens.isExpired() || tokens.getSecondsUntilExpiry() < 300) {
                LOG.info("Attempting to refresh OAuth tokens (forceRefresh=" + forceRefresh +
                        ", expired=" + tokens.isExpired() +
                        ", secondsRemaining=" + tokens.getSecondsUntilExpiry() + ")");
                tokens = refreshAndPersistTokens(oauthService);
            }

            return tokens.accessToken();
        } catch (SubmissionException e) {
            throw e;
        } catch (Exception e) {
            throw new SubmissionException("Failed to get HMRC authentication token: " + e.getMessage(), e);
        }
    }

    /**
     * Refreshes OAuth tokens and persists them to storage.
     *
     * @param oauthService the OAuth service
     * @return the new tokens
     * @throws SubmissionException if refresh fails
     */
    private OAuthTokens refreshAndPersistTokens(HmrcOAuthService oauthService) {
        try {
            OAuthTokens newTokens = oauthService.refreshAccessToken()
                    .get(30, java.util.concurrent.TimeUnit.SECONDS);

            // Persist refreshed tokens
            SqliteDataStore.getInstance().saveOAuthTokens(
                    newTokens.accessToken(),
                    newTokens.refreshToken(),
                    newTokens.expiresIn(),
                    newTokens.tokenType(),
                    newTokens.scope(),
                    newTokens.issuedAt()
            );
            LOG.info("OAuth tokens refreshed and persisted");
            return newTokens;

        } catch (Exception e) {
            LOG.warning("Token refresh failed: " + e.getMessage());
            // Clear invalid tokens
            SqliteDataStore.getInstance().clearOAuthTokens();
            throw new SubmissionException(
                    "SESSION_EXPIRED: Your HMRC session has expired and could not be refreshed. " +
                    "Please reconnect via the HMRC Submission page.", e);
        }
    }

    // ==================== Validation ====================

    /**
     * Validates all prerequisites before submission.
     */
    private void validateSubmissionPrerequisites(QuarterlyReviewData reviewData,
                                                  Instant declarationAcceptedAt,
                                                  String declarationTextHash) {
        if (reviewData == null) {
            throw new SubmissionException("Review data is required");
        }

        // ALWAYS reload NINO and HMRC business ID from SQLite to get the latest values.
        // This handles the case where the singleton was created before the user configured
        // their HMRC connection in Settings. Sprint 13: Fix for stale singleton cache.
        if (dataStore != null) {
            String reloadedNino = dataStore.loadNino();
            if (reloadedNino != null && !reloadedNino.isBlank()) {
                if (nino == null || !nino.equals(reloadedNino)) {
                    LOG.info("Loaded NINO from SQLite: " + (nino == null ? "was null" : "was different"));
                    setNino(reloadedNino);
                }
            }

            String reloadedHmrcBusinessId = dataStore.loadHmrcBusinessId();
            if (reloadedHmrcBusinessId != null && !reloadedHmrcBusinessId.isBlank()) {
                if (hmrcBusinessId == null || !hmrcBusinessId.equals(reloadedHmrcBusinessId)) {
                    LOG.info("Loaded HMRC business ID from SQLite: " + reloadedHmrcBusinessId +
                             " (was " + (hmrcBusinessId == null ? "null" : hmrcBusinessId) + ")");
                    setHmrcBusinessId(reloadedHmrcBusinessId);
                }
            } else {
                LOG.warning("No HMRC business ID found in SQLite");
            }
        }

        if (nino == null || nino.isBlank()) {
            throw new SubmissionException(
                    "NINO_REQUIRED: National Insurance Number (NINO) is required. " +
                    "Please set your NINO in Settings before submitting.");
        }
        if (hmrcBusinessId == null || hmrcBusinessId.isBlank()) {
            throw new SubmissionException(
                    "HMRC_PROFILE_NOT_SYNCED: Your HMRC business profile needs to be synced. " +
                    "Go to Settings and reconnect to HMRC to fetch your business details.");
        }
        // Validate HMRC business ID format (e.g. "XAIS12345678901")
        if (!hmrcBusinessId.matches("^X[A-Z0-9]{1}IS[0-9]{11}$")) {
            throw new SubmissionException(
                    "HMRC_PROFILE_NOT_SYNCED: Your HMRC business ID appears invalid. " +
                    "Go to Settings and reconnect to HMRC to refresh your business details.");
        }
        if (declarationAcceptedAt == null) {
            throw new SubmissionException(
                    "DECLARATION_REQUIRED: You must accept the declaration before submitting.");
        }
        if (declarationTextHash == null || declarationTextHash.isBlank()) {
            throw new SubmissionException(
                    "DECLARATION_REQUIRED: Declaration text hash is required for audit trail.");
        }
    }

    // ==================== Category Helpers ====================

    /**
     * Gets the amount for a single expense category, defaulting to zero.
     */
    private BigDecimal getCategoryAmount(Map<ExpenseCategory, CategorySummary> expenses, ExpenseCategory category) {
        CategorySummary summary = expenses.get(category);
        return summary != null ? summary.amount() : BigDecimal.ZERO;
    }

    /**
     * Sums amounts for multiple expense categories (e.g., Travel + Travel Mileage).
     */
    private BigDecimal sumCategoryAmounts(Map<ExpenseCategory, CategorySummary> expenses, ExpenseCategory... categories) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ExpenseCategory category : categories) {
            sum = sum.add(getCategoryAmount(expenses, category));
        }
        return sum;
    }

    // ==================== Fraud Prevention Headers ====================

    /**
     * Adds HMRC fraud prevention headers to the request.
     * These headers are legally required for all MTD API calls.
     *
     * @param requestBuilder the HTTP request builder to add headers to
     */
    private void addFraudPreventionHeaders(HttpRequest.Builder requestBuilder) {
        requestBuilder
                .header("Gov-Client-Connection-Method", "DESKTOP_APP_DIRECT")
                .header("Gov-Client-User-IDs", "")
                .header("Gov-Vendor-Version", "SelfEmployment=1.0")
                .header("Gov-Vendor-Product-Name", "UK Self-Employment Manager");

        // Add timezone
        try {
            TimeZone tz = TimeZone.getDefault();
            int offsetMs = tz.getRawOffset();
            int hours = Math.abs(offsetMs) / 3600000;
            int minutes = (Math.abs(offsetMs) % 3600000) / 60000;
            String sign = offsetMs >= 0 ? "+" : "-";
            requestBuilder.header("Gov-Client-Timezone",
                    String.format("UTC%s%02d:%02d", sign, hours, minutes));
        } catch (Exception e) {
            requestBuilder.header("Gov-Client-Timezone", "UTC+00:00");
        }

        // Add local IP
        try {
            String localIp = InetAddress.getLocalHost().getHostAddress();
            requestBuilder.header("Gov-Client-Local-IPs", localIp);
        } catch (Exception e) {
            LOG.fine("Could not determine local IP for fraud prevention header");
        }
    }
}
