package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.core.exception.SubmissionException;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD tests for AutoOAuthSubmissionService.
 *
 * <p>Tests the automatic OAuth flow during HMRC submission:</p>
 * <ul>
 *   <li>Pre-flight NINO validation before OAuth</li>
 *   <li>Auto-trigger OAuth when tokens are missing/expired</li>
 *   <li>Progress callback during OAuth flow</li>
 *   <li>Handle 401 errors with automatic OAuth redirect and retry</li>
 *   <li>Cancel submission during OAuth flow</li>
 * </ul>
 */
@DisplayName("AutoOAuthSubmissionService")
class AutoOAuthSubmissionServiceTest {

    private static final String TEST_NINO = "QQ123456C";
    private static final String TEST_BUSINESS_ID = "XAIS12345678901";
    private static final UUID BUSINESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UiQuarterlySubmissionService mockSubmissionService;
    private HmrcOAuthService mockOAuthService;
    private SqliteDataStore mockDataStore;
    private HmrcConnectionService mockConnectionService;
    private AutoOAuthSubmissionService service;

    @BeforeEach
    void setUp() {
        mockSubmissionService = mock(UiQuarterlySubmissionService.class);
        mockOAuthService = mock(HmrcOAuthService.class);
        mockDataStore = mock(SqliteDataStore.class);
        mockConnectionService = mock(HmrcConnectionService.class);

        service = new AutoOAuthSubmissionService(
                mockSubmissionService,
                mockOAuthService,
                mockDataStore,
                mockConnectionService
        );
    }

    @Nested
    @DisplayName("Pre-flight NINO Validation")
    class PreFlightNinoValidationTests {

        @Test
        @DisplayName("TC-1: should throw NINO_REQUIRED before triggering OAuth when NINO not set")
        void shouldThrowNinoRequiredBeforeOAuth() {
            // Given: NINO not configured
            when(mockDataStore.loadNino()).thenReturn(null);
            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When/Then: Should fail with NINO_REQUIRED before OAuth is triggered
            assertThatThrownBy(() -> service.submit(reviewData, Instant.now(), "hash"))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("NINO_REQUIRED");

            // OAuth should NOT be triggered
            verify(mockOAuthService, never()).authenticate();
        }

        @Test
        @DisplayName("TC-2: should throw NINO_REQUIRED when NINO is blank")
        void shouldThrowNinoRequiredWhenBlank() {
            // Given: NINO is blank
            when(mockDataStore.loadNino()).thenReturn("   ");
            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When/Then
            assertThatThrownBy(() -> service.submit(reviewData, Instant.now(), "hash"))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("NINO_REQUIRED");

            verify(mockOAuthService, never()).authenticate();
        }

        @Test
        @DisplayName("TC-3: should proceed to OAuth check when NINO is valid")
        void shouldProceedWhenNinoValid() {
            // Given: NINO configured, but not connected
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockConnectionService.isConnected()).thenReturn(false);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.NOT_CONNECTED);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When: Submit is called
            // The OAuth flow should be triggered (but will fail since we haven't set it up)
            assertThatThrownBy(() -> service.submit(reviewData, Instant.now(), "hash"))
                    .isInstanceOf(SubmissionException.class)
                    .satisfies(ex -> {
                        // Should have progressed past NINO check
                        assertThat(ex.getMessage()).doesNotContain("NINO_REQUIRED");
                    });
        }
    }

    @Nested
    @DisplayName("Auto-trigger OAuth")
    class AutoTriggerOAuthTests {

        @Test
        @DisplayName("TC-4: should trigger OAuth when not connected")
        void shouldTriggerOAuthWhenNotConnected() throws Exception {
            // Given: NINO valid, not connected
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected()).thenReturn(false);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.NOT_CONNECTED);

            // OAuth will complete successfully
            OAuthTokens tokens = createTestTokens();
            when(mockOAuthService.authenticate()).thenReturn(CompletableFuture.completedFuture(tokens));

            // After OAuth, connection service reports connected
            when(mockConnectionService.isConnected())
                    .thenReturn(false)  // First call
                    .thenReturn(true);  // After OAuth

            // Submission succeeds
            Submission mockSubmission = createMockSubmission();
            when(mockSubmissionService.submit(any(), any(), any())).thenReturn(mockSubmission);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When
            Submission result = service.submit(reviewData, Instant.now(), "hash");

            // Then
            verify(mockOAuthService).authenticate();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("TC-5: should skip OAuth when already connected")
        void shouldSkipOAuthWhenConnected() throws Exception {
            // Given: Already connected
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected()).thenReturn(true);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.READY_TO_SUBMIT);

            // Submission succeeds
            Submission mockSubmission = createMockSubmission();
            when(mockSubmissionService.submit(any(), any(), any())).thenReturn(mockSubmission);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When
            Submission result = service.submit(reviewData, Instant.now(), "hash");

            // Then
            verify(mockOAuthService, never()).authenticate();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("TC-6: should trigger OAuth when session expired")
        void shouldTriggerOAuthWhenSessionExpired() throws Exception {
            // Given: Session expired
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected()).thenReturn(false);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.SESSION_EXPIRED);

            // OAuth will complete successfully
            OAuthTokens tokens = createTestTokens();
            when(mockOAuthService.authenticate()).thenReturn(CompletableFuture.completedFuture(tokens));

            // After OAuth, connection service reports connected
            when(mockConnectionService.isConnected())
                    .thenReturn(false)  // First call
                    .thenReturn(true);  // After OAuth

            // Submission succeeds
            Submission mockSubmission = createMockSubmission();
            when(mockSubmissionService.submit(any(), any(), any())).thenReturn(mockSubmission);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When
            Submission result = service.submit(reviewData, Instant.now(), "hash");

            // Then
            verify(mockOAuthService).authenticate();
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("TC-7: should trigger OAuth when needs verification")
        void shouldTriggerOAuthWhenNeedsVerification() throws Exception {
            // Given: Needs verification (tokens exist but not verified this session)
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected()).thenReturn(false);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.NEEDS_VERIFICATION);

            // OAuth will complete successfully
            OAuthTokens tokens = createTestTokens();
            when(mockOAuthService.authenticate()).thenReturn(CompletableFuture.completedFuture(tokens));

            // After OAuth, connection service reports connected
            when(mockConnectionService.isConnected())
                    .thenReturn(false)  // First call
                    .thenReturn(true);  // After OAuth

            // Submission succeeds
            Submission mockSubmission = createMockSubmission();
            when(mockSubmissionService.submit(any(), any(), any())).thenReturn(mockSubmission);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When
            Submission result = service.submit(reviewData, Instant.now(), "hash");

            // Then: OAuth should be triggered (not just verification)
            verify(mockOAuthService).authenticate();
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Progress Callback")
    class ProgressCallbackTests {

        @Test
        @DisplayName("TC-8: should report AUTHENTICATING status during OAuth flow")
        void shouldReportAuthenticatingDuringOAuth() throws Exception {
            // Given
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected()).thenReturn(false);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.NOT_CONNECTED);

            // Track progress updates
            AtomicReference<AutoOAuthSubmissionService.SubmissionProgress> lastProgress = new AtomicReference<>();
            service.setProgressCallback(lastProgress::set);

            // OAuth completes after we check progress
            CompletableFuture<OAuthTokens> oauthFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(oauthFuture);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When: Start submission on background thread
            Thread submissionThread = new Thread(() -> {
                try {
                    service.submit(reviewData, Instant.now(), "hash");
                } catch (Exception e) {
                    // Expected - OAuth won't complete
                }
            });
            submissionThread.start();

            // Wait for OAuth to start
            Thread.sleep(100);

            // Then: Progress should show AUTHENTICATING
            assertThat(lastProgress.get()).isNotNull();
            assertThat(lastProgress.get().stage())
                    .isEqualTo(AutoOAuthSubmissionService.SubmissionStage.AUTHENTICATING);

            // Cleanup
            oauthFuture.completeExceptionally(new RuntimeException("Test timeout"));
            submissionThread.join(1000);
        }

        @Test
        @DisplayName("TC-9: should report SUBMITTING status after OAuth completes")
        void shouldReportSubmittingAfterOAuth() throws Exception {
            // Given
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected())
                    .thenReturn(false)  // Initially not connected
                    .thenReturn(true);  // After OAuth
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.NOT_CONNECTED);

            // Track all progress updates
            java.util.List<AutoOAuthSubmissionService.SubmissionStage> stages =
                    java.util.Collections.synchronizedList(new java.util.ArrayList<>());
            service.setProgressCallback(progress -> stages.add(progress.stage()));

            // OAuth completes successfully
            OAuthTokens tokens = createTestTokens();
            when(mockOAuthService.authenticate()).thenReturn(CompletableFuture.completedFuture(tokens));

            // Submission succeeds
            Submission mockSubmission = createMockSubmission();
            when(mockSubmissionService.submit(any(), any(), any())).thenReturn(mockSubmission);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When
            service.submit(reviewData, Instant.now(), "hash");

            // Then: Should have reported AUTHENTICATING then SUBMITTING
            assertThat(stages).contains(
                    AutoOAuthSubmissionService.SubmissionStage.AUTHENTICATING,
                    AutoOAuthSubmissionService.SubmissionStage.SUBMITTING
            );
        }

        @Test
        @DisplayName("TC-10: should report COMPLETE status after successful submission")
        void shouldReportCompleteAfterSubmission() throws Exception {
            // Given
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected()).thenReturn(true);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.READY_TO_SUBMIT);

            AtomicReference<AutoOAuthSubmissionService.SubmissionProgress> lastProgress = new AtomicReference<>();
            service.setProgressCallback(lastProgress::set);

            Submission mockSubmission = createMockSubmission();
            when(mockSubmissionService.submit(any(), any(), any())).thenReturn(mockSubmission);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When
            service.submit(reviewData, Instant.now(), "hash");

            // Then
            assertThat(lastProgress.get()).isNotNull();
            assertThat(lastProgress.get().stage())
                    .isEqualTo(AutoOAuthSubmissionService.SubmissionStage.COMPLETE);
        }
    }

    @Nested
    @DisplayName("Handle 401 with Retry")
    class Handle401WithRetryTests {

        @Test
        @DisplayName("TC-11: should trigger OAuth and retry on 401 error")
        void shouldTriggerOAuthAndRetryOn401() throws Exception {
            // Given: Connected but submission returns 401
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected()).thenReturn(true);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.READY_TO_SUBMIT);

            // First submission fails with 401
            SubmissionException sessionExpired = new SubmissionException("SESSION_EXPIRED: Token invalid");
            Submission successSubmission = createMockSubmission();
            when(mockSubmissionService.submit(any(), any(), any()))
                    .thenThrow(sessionExpired)  // First call fails
                    .thenReturn(successSubmission);  // Retry succeeds

            // OAuth re-authentication succeeds
            OAuthTokens tokens = createTestTokens();
            when(mockOAuthService.authenticate()).thenReturn(CompletableFuture.completedFuture(tokens));

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When
            Submission result = service.submit(reviewData, Instant.now(), "hash");

            // Then: Should have triggered OAuth and retried
            verify(mockOAuthService).authenticate();
            verify(mockSubmissionService, times(2)).submit(any(), any(), any());
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        }

        @Test
        @DisplayName("TC-12: should only retry once on 401")
        void shouldOnlyRetryOnce() throws Exception {
            // Given: Connected but all submissions return 401
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected()).thenReturn(true);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.READY_TO_SUBMIT);

            // All submissions fail with 401
            SubmissionException sessionExpired = new SubmissionException("SESSION_EXPIRED: Token invalid");
            when(mockSubmissionService.submit(any(), any(), any())).thenThrow(sessionExpired);

            // OAuth succeeds but token still doesn't work
            OAuthTokens tokens = createTestTokens();
            when(mockOAuthService.authenticate()).thenReturn(CompletableFuture.completedFuture(tokens));

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When/Then: Should fail after one retry
            assertThatThrownBy(() -> service.submit(reviewData, Instant.now(), "hash"))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("SESSION_EXPIRED");

            // Should have called OAuth once and submission twice (original + 1 retry)
            verify(mockOAuthService, times(1)).authenticate();
            verify(mockSubmissionService, times(2)).submit(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Cancellation")
    class CancellationTests {

        @Test
        @DisplayName("TC-13: should cancel OAuth flow and throw cancelled exception")
        void shouldCancelOAuthFlow() throws Exception {
            // Given
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockConnectionService.isConnected()).thenReturn(false);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.NOT_CONNECTED);

            // OAuth will hang until cancelled
            CompletableFuture<OAuthTokens> oauthFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(oauthFuture);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // Start submission in background
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread submissionThread = new Thread(() -> {
                try {
                    service.submit(reviewData, Instant.now(), "hash");
                } catch (Exception e) {
                    error.set(e);
                }
            });
            submissionThread.start();

            // Wait for OAuth to start
            Thread.sleep(100);

            // When: Cancel the submission
            service.cancel();

            // Complete the OAuth future with cancellation (simulating real behavior)
            oauthFuture.completeExceptionally(new RuntimeException("Cancelled"));

            submissionThread.join(1000);

            // Then: Should have thrown cancellation exception
            assertThat(error.get()).isNotNull();
            verify(mockOAuthService).cancelAuthentication();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("TC-14: should propagate OAuth errors")
        void shouldPropagateOAuthErrors() {
            // Given
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockConnectionService.isConnected()).thenReturn(false);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.NOT_CONNECTED);

            // OAuth fails
            when(mockOAuthService.authenticate())
                    .thenReturn(CompletableFuture.failedFuture(
                            new RuntimeException("OAuth failed: access_denied")));

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When/Then
            assertThatThrownBy(() -> service.submit(reviewData, Instant.now(), "hash"))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("OAuth");
        }

        @Test
        @DisplayName("TC-15: should propagate non-401 submission errors without retry")
        void shouldPropagateNon401ErrorsWithoutRetry() {
            // Given: Connected
            when(mockDataStore.loadNino()).thenReturn(TEST_NINO);
            when(mockDataStore.loadHmrcBusinessId()).thenReturn(TEST_BUSINESS_ID);
            when(mockConnectionService.isConnected()).thenReturn(true);
            when(mockConnectionService.getConnectionState())
                    .thenReturn(HmrcConnectionService.ConnectionState.READY_TO_SUBMIT);

            // Submission fails with non-401 error
            SubmissionException validationError = new SubmissionException("FORMAT_VALUE: Invalid income");
            when(mockSubmissionService.submit(any(), any(), any())).thenThrow(validationError);

            QuarterlyReviewData reviewData = createReviewData(Quarter.Q1);

            // When/Then: Should propagate error without retry
            assertThatThrownBy(() -> service.submit(reviewData, Instant.now(), "hash"))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("FORMAT_VALUE");

            // No OAuth triggered, only one submission attempt
            verify(mockOAuthService, never()).authenticate();
            verify(mockSubmissionService, times(1)).submit(any(), any(), any());
        }
    }

    // ==================== Helper Methods ====================

    private QuarterlyReviewData createReviewData(Quarter quarter) {
        TaxYear taxYear = TaxYear.of(2025);
        return QuarterlyReviewData.builder()
                .quarter(quarter)
                .taxYear(taxYear)
                .periodStart(quarter.getStartDate(taxYear))
                .periodEnd(quarter.getEndDate(taxYear))
                .totalIncome(new BigDecimal("5000.00"))
                .incomeTransactionCount(5)
                .totalExpenses(new BigDecimal("800.00"))
                .expenseTransactionCount(3)
                .expensesByCategory(new EnumMap<>(ExpenseCategory.class))
                .build();
    }

    private OAuthTokens createTestTokens() {
        return OAuthTokens.create(
                "test-access-token",
                "test-refresh-token",
                14400,
                "Bearer",
                "read:vat write:vat"
        );
    }

    private Submission createMockSubmission() {
        return new Submission(
                UUID.randomUUID(),
                BUSINESS_ID,
                uk.selfemploy.common.enums.SubmissionType.QUARTERLY_Q1,
                TaxYear.of(2025),
                LocalDate.of(2025, 4, 6),
                LocalDate.of(2025, 7, 5),
                new BigDecimal("5000.00"),
                new BigDecimal("800.00"),
                new BigDecimal("4200.00"),
                SubmissionStatus.ACCEPTED,
                "HMRC-REF-123456",
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                "hash",
                null,
                TEST_NINO
        );
    }
}
