package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.ui.service.OAuthConnectionHandler.ConnectionStatus;
import uk.selfemploy.ui.service.OAuthConnectionHandler.OAuthResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TDD tests for OAuthConnectionHandler.
 * Sprint 12 - SE-12-004: OAuth Connection with Progress
 *
 * <p>Tests cover the OAuth connection flow with progress reporting,
 * success/error/timeout handling, cancellation, and repository persistence.</p>
 *
 * <h2>Test Cases (10+):</h2>
 * <ol>
 *   <li>startConnection_callsOAuthService</li>
 *   <li>startConnection_reportsOpeningBrowser</li>
 *   <li>startConnection_reportsWaitingForAuth</li>
 *   <li>startConnection_reportsSuccess_onComplete</li>
 *   <li>startConnection_reportsError_onFailure</li>
 *   <li>startConnection_reportsTimeout_after60Seconds</li>
 *   <li>cancel_stopsOAuthFlow</li>
 *   <li>cancel_reportsCancel_status</li>
 *   <li>success_persistsToRepository</li>
 *   <li>error_doesNotPersist</li>
 * </ol>
 */
@DisplayName("OAuthConnectionHandler")
class OAuthConnectionHandlerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-01-29T10:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private HmrcOAuthService mockOAuthService;
    private WizardProgressRepository mockRepository;
    private List<ConnectionStatus> statusUpdates;
    private AtomicReference<OAuthResult> resultRef;
    private Consumer<ConnectionStatus> statusCallback;
    private Consumer<OAuthResult> resultCallback;
    private OAuthConnectionHandler handler;

    @BeforeEach
    void setUp() {
        mockOAuthService = mock(HmrcOAuthService.class);
        mockRepository = mock(WizardProgressRepository.class);
        statusUpdates = new ArrayList<>();
        resultRef = new AtomicReference<>();

        statusCallback = statusUpdates::add;
        resultCallback = resultRef::set;

        handler = new OAuthConnectionHandler(
            mockOAuthService,
            mockRepository,
            statusCallback,
            resultCallback,
            FIXED_CLOCK,
            DEFAULT_TIMEOUT
        );
    }

    @Nested
    @DisplayName("startConnection()")
    class StartConnectionTests {

        @Test
        @DisplayName("should call OAuth service authenticate")
        void startConnection_callsOAuthService() {
            // Given
            CompletableFuture<OAuthTokens> pendingFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(pendingFuture);

            // When
            handler.startConnection();

            // Then
            verify(mockOAuthService).authenticate();
        }

        @Test
        @DisplayName("should report OPENING_BROWSER status immediately")
        void startConnection_reportsOpeningBrowser() throws Exception {
            // Given
            CompletableFuture<OAuthTokens> pendingFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(pendingFuture);

            // When
            handler.startConnection();

            // Allow async processing
            Thread.sleep(50);

            // Then
            assertThat(statusUpdates).contains(ConnectionStatus.OPENING_BROWSER);
        }

        @Test
        @DisplayName("should report WAITING_FOR_AUTH status after browser opens")
        void startConnection_reportsWaitingForAuth() throws Exception {
            // Given
            CompletableFuture<OAuthTokens> pendingFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(pendingFuture);

            // When
            handler.startConnection();

            // Allow async processing for status transitions
            Thread.sleep(150);

            // Then
            assertThat(statusUpdates).contains(ConnectionStatus.WAITING_FOR_AUTH);
        }

        @Test
        @DisplayName("should report SUCCESS status on successful completion")
        void startConnection_reportsSuccess_onComplete() throws Exception {
            // Given
            OAuthTokens tokens = createTestTokens();
            CompletableFuture<OAuthTokens> successFuture = CompletableFuture.completedFuture(tokens);
            when(mockOAuthService.authenticate()).thenReturn(successFuture);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<OAuthResult> asyncResult = new AtomicReference<>();
            OAuthConnectionHandler asyncHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> {
                    asyncResult.set(result);
                    latch.countDown();
                },
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            // When
            asyncHandler.startConnection();
            boolean completed = latch.await(2, TimeUnit.SECONDS);

            // Then
            assertThat(completed).isTrue();
            assertThat(statusUpdates).contains(ConnectionStatus.SUCCESS);
            assertThat(asyncResult.get()).isNotNull();
            assertThat(asyncResult.get().success()).isTrue();
        }

        @Test
        @DisplayName("should report ERROR status on OAuth failure")
        void startConnection_reportsError_onFailure() throws Exception {
            // Given
            HmrcOAuthException error = new HmrcOAuthException(OAuthError.ACCESS_DENIED);
            CompletableFuture<OAuthTokens> failedFuture = CompletableFuture.failedFuture(error);
            when(mockOAuthService.authenticate()).thenReturn(failedFuture);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<OAuthResult> asyncResult = new AtomicReference<>();
            OAuthConnectionHandler asyncHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> {
                    asyncResult.set(result);
                    latch.countDown();
                },
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            // When
            asyncHandler.startConnection();
            boolean completed = latch.await(2, TimeUnit.SECONDS);

            // Then
            assertThat(completed).isTrue();
            assertThat(statusUpdates).contains(ConnectionStatus.ERROR);
            assertThat(asyncResult.get()).isNotNull();
            assertThat(asyncResult.get().success()).isFalse();
            assertThat(asyncResult.get().errorCode()).isEqualTo("ACCESS_DENIED");
        }

        @Test
        @DisplayName("should report TIMEOUT status after 60 seconds")
        void startConnection_reportsTimeout_after60Seconds() throws Exception {
            // Given - use a very short timeout for testing
            Duration shortTimeout = Duration.ofMillis(100);
            CompletableFuture<OAuthTokens> neverCompletingFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(neverCompletingFuture);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<OAuthResult> asyncResult = new AtomicReference<>();
            OAuthConnectionHandler timeoutHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> {
                    asyncResult.set(result);
                    latch.countDown();
                },
                FIXED_CLOCK,
                shortTimeout
            );

            // When
            timeoutHandler.startConnection();
            boolean completed = latch.await(2, TimeUnit.SECONDS);

            // Then
            assertThat(completed).isTrue();
            assertThat(statusUpdates).contains(ConnectionStatus.TIMEOUT);
            assertThat(asyncResult.get()).isNotNull();
            assertThat(asyncResult.get().success()).isFalse();
            assertThat(asyncResult.get().errorCode()).isEqualTo("TIMEOUT");
        }

        @Test
        @DisplayName("should report COMPLETING status before success")
        void startConnection_reportsCompleting_beforeSuccess() throws Exception {
            // Given
            OAuthTokens tokens = createTestTokens();
            CompletableFuture<OAuthTokens> successFuture = CompletableFuture.completedFuture(tokens);
            when(mockOAuthService.authenticate()).thenReturn(successFuture);

            CountDownLatch latch = new CountDownLatch(1);
            OAuthConnectionHandler asyncHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> latch.countDown(),
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            // When
            asyncHandler.startConnection();
            latch.await(2, TimeUnit.SECONDS);

            // Then - COMPLETING should appear before SUCCESS
            int completingIndex = statusUpdates.indexOf(ConnectionStatus.COMPLETING);
            int successIndex = statusUpdates.indexOf(ConnectionStatus.SUCCESS);
            assertThat(completingIndex).isGreaterThanOrEqualTo(0);
            assertThat(successIndex).isGreaterThan(completingIndex);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class CancelTests {

        @Test
        @DisplayName("should stop OAuth flow when cancelled")
        void cancel_stopsOAuthFlow() {
            // Given
            CompletableFuture<OAuthTokens> pendingFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(pendingFuture);
            handler.startConnection();

            // When
            handler.cancel();

            // Then
            verify(mockOAuthService).cancelAuthentication();
        }

        @Test
        @DisplayName("should report CANCELLED status when cancelled")
        void cancel_reportsCancel_status() throws Exception {
            // Given
            CompletableFuture<OAuthTokens> pendingFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(pendingFuture);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<OAuthResult> asyncResult = new AtomicReference<>();
            OAuthConnectionHandler asyncHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> {
                    asyncResult.set(result);
                    latch.countDown();
                },
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            asyncHandler.startConnection();
            Thread.sleep(50); // Let it start

            // When
            asyncHandler.cancel();
            // Complete the future with cancellation exception to trigger the handler
            pendingFuture.completeExceptionally(
                new HmrcOAuthException(OAuthError.USER_CANCELLED)
            );
            boolean completed = latch.await(2, TimeUnit.SECONDS);

            // Then
            assertThat(completed).isTrue();
            assertThat(statusUpdates).contains(ConnectionStatus.CANCELLED);
        }

        @Test
        @DisplayName("should be safe to call cancel when not started")
        void cancel_safeWhenNotStarted() {
            // When - calling cancel without starting should not throw
            handler.cancel();

            // Then - no exception and service cancel called
            verify(mockOAuthService).cancelAuthentication();
        }
    }

    @Nested
    @DisplayName("Repository Persistence")
    class RepositoryPersistenceTests {

        @Test
        @DisplayName("should persist progress on successful connection")
        void success_persistsToRepository() throws Exception {
            // Given
            OAuthTokens tokens = createTestTokens();
            CompletableFuture<OAuthTokens> successFuture = CompletableFuture.completedFuture(tokens);
            when(mockOAuthService.authenticate()).thenReturn(successFuture);

            CountDownLatch latch = new CountDownLatch(1);
            OAuthConnectionHandler asyncHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> latch.countDown(),
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            // When
            asyncHandler.startConnection();
            latch.await(2, TimeUnit.SECONDS);

            // Then
            ArgumentCaptor<WizardProgress> captor = ArgumentCaptor.forClass(WizardProgress.class);
            verify(mockRepository).save(captor.capture());
            WizardProgress saved = captor.getValue();
            assertThat(saved.wizardType()).isEqualTo(WizardProgress.HMRC_CONNECTION);
            assertThat(saved.currentStep()).isEqualTo(4); // Step 4 is OAuth
        }

        @Test
        @DisplayName("should not persist progress on error")
        void error_doesNotPersist() throws Exception {
            // Given
            HmrcOAuthException error = new HmrcOAuthException(OAuthError.ACCESS_DENIED);
            CompletableFuture<OAuthTokens> failedFuture = CompletableFuture.failedFuture(error);
            when(mockOAuthService.authenticate()).thenReturn(failedFuture);

            CountDownLatch latch = new CountDownLatch(1);
            OAuthConnectionHandler asyncHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> latch.countDown(),
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            // When
            asyncHandler.startConnection();
            latch.await(2, TimeUnit.SECONDS);

            // Then
            verify(mockRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not persist progress on timeout")
        void timeout_doesNotPersist() throws Exception {
            // Given
            Duration shortTimeout = Duration.ofMillis(100);
            CompletableFuture<OAuthTokens> neverCompletingFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(neverCompletingFuture);

            CountDownLatch latch = new CountDownLatch(1);
            OAuthConnectionHandler timeoutHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> latch.countDown(),
                FIXED_CLOCK,
                shortTimeout
            );

            // When
            timeoutHandler.startConnection();
            latch.await(2, TimeUnit.SECONDS);

            // Then
            verify(mockRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not persist progress on cancellation")
        void cancel_doesNotPersist() throws Exception {
            // Given
            CompletableFuture<OAuthTokens> pendingFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(pendingFuture);

            CountDownLatch latch = new CountDownLatch(1);
            OAuthConnectionHandler asyncHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> latch.countDown(),
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            asyncHandler.startConnection();
            Thread.sleep(50);

            // When
            asyncHandler.cancel();
            pendingFuture.completeExceptionally(new HmrcOAuthException(OAuthError.USER_CANCELLED));
            latch.await(2, TimeUnit.SECONDS);

            // Then
            verify(mockRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("OAuthResult")
    class OAuthResultTests {

        @Test
        @DisplayName("should create success result correctly")
        void successResult_hasCorrectFields() {
            // When
            OAuthResult result = OAuthResult.ofSuccess();

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.errorCode()).isNull();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("should create error result correctly")
        void errorResult_hasCorrectFields() {
            // When
            OAuthResult result = OAuthResult.ofError("ACCESS_DENIED", "User denied access");

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo("ACCESS_DENIED");
            assertThat(result.errorMessage()).isEqualTo("User denied access");
        }

        @Test
        @DisplayName("should create timeout result correctly")
        void timeoutResult_hasCorrectFields() {
            // When
            OAuthResult result = OAuthResult.ofTimeout();

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo("TIMEOUT");
            assertThat(result.errorMessage()).contains("timed out");
        }

        @Test
        @DisplayName("should create cancelled result correctly")
        void cancelledResult_hasCorrectFields() {
            // When
            OAuthResult result = OAuthResult.ofCancelled();

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo("USER_CANCELLED");
            assertThat(result.errorMessage()).contains("cancelled");
        }
    }

    @Nested
    @DisplayName("ConnectionStatus")
    class ConnectionStatusTests {

        @Test
        @DisplayName("should have all required status values")
        void shouldHaveAllStatusValues() {
            assertThat(ConnectionStatus.values()).containsExactlyInAnyOrder(
                ConnectionStatus.OPENING_BROWSER,
                ConnectionStatus.WAITING_FOR_AUTH,
                ConnectionStatus.COMPLETING,
                ConnectionStatus.SUCCESS,
                ConnectionStatus.ERROR,
                ConnectionStatus.TIMEOUT,
                ConnectionStatus.CANCELLED
            );
        }

        @Test
        @DisplayName("each status should have a display message")
        void eachStatus_hasDisplayMessage() {
            for (ConnectionStatus status : ConnectionStatus.values()) {
                assertThat(status.getDisplayMessage())
                    .as("Status %s should have a display message", status)
                    .isNotNull()
                    .isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should extract error code from HmrcOAuthException")
        void shouldExtractErrorCodeFromHmrcOAuthException() throws Exception {
            // Given
            HmrcOAuthException error = new HmrcOAuthException(OAuthError.INVALID_CLIENT, "Bad credentials");
            CompletableFuture<OAuthTokens> failedFuture = CompletableFuture.failedFuture(error);
            when(mockOAuthService.authenticate()).thenReturn(failedFuture);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<OAuthResult> asyncResult = new AtomicReference<>();
            OAuthConnectionHandler asyncHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> {
                    asyncResult.set(result);
                    latch.countDown();
                },
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            // When
            asyncHandler.startConnection();
            latch.await(2, TimeUnit.SECONDS);

            // Then
            assertThat(asyncResult.get().errorCode()).isEqualTo("INVALID_CLIENT");
        }

        @Test
        @DisplayName("should handle unknown exception with generic error")
        void shouldHandleUnknownException() throws Exception {
            // Given
            RuntimeException error = new RuntimeException("Unexpected error");
            CompletableFuture<OAuthTokens> failedFuture = CompletableFuture.failedFuture(error);
            when(mockOAuthService.authenticate()).thenReturn(failedFuture);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<OAuthResult> asyncResult = new AtomicReference<>();
            OAuthConnectionHandler asyncHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                statusUpdates::add,
                result -> {
                    asyncResult.set(result);
                    latch.countDown();
                },
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            // When
            asyncHandler.startConnection();
            latch.await(2, TimeUnit.SECONDS);

            // Then
            assertThat(asyncResult.get().errorCode()).isEqualTo("UNKNOWN_ERROR");
            assertThat(asyncResult.get().errorMessage()).contains("Unexpected error");
        }
    }

    @Nested
    @DisplayName("Concurrent Connection Attempts")
    class ConcurrentConnectionTests {

        @Test
        @DisplayName("should prevent multiple simultaneous connections")
        void shouldPreventMultipleConnections() {
            // Given
            CompletableFuture<OAuthTokens> pendingFuture = new CompletableFuture<>();
            when(mockOAuthService.authenticate()).thenReturn(pendingFuture);

            // When - start first connection
            handler.startConnection();
            // Try to start second connection
            handler.startConnection();

            // Then - authenticate should only be called once
            verify(mockOAuthService, times(1)).authenticate();
        }

        @Test
        @DisplayName("should allow new connection after previous completes")
        void shouldAllowNewConnectionAfterCompletion() throws Exception {
            // Given
            OAuthTokens tokens = createTestTokens();
            CompletableFuture<OAuthTokens> firstFuture = CompletableFuture.completedFuture(tokens);
            CompletableFuture<OAuthTokens> secondFuture = CompletableFuture.completedFuture(tokens);
            when(mockOAuthService.authenticate())
                .thenReturn(firstFuture)
                .thenReturn(secondFuture);

            CountDownLatch firstLatch = new CountDownLatch(1);
            CountDownLatch secondLatch = new CountDownLatch(1);

            List<ConnectionStatus> firstStatuses = new ArrayList<>();
            List<ConnectionStatus> secondStatuses = new ArrayList<>();

            OAuthConnectionHandler firstHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                firstStatuses::add,
                result -> firstLatch.countDown(),
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );

            // When - complete first connection
            firstHandler.startConnection();
            firstLatch.await(2, TimeUnit.SECONDS);

            // Create new handler for second connection
            OAuthConnectionHandler secondHandler = new OAuthConnectionHandler(
                mockOAuthService,
                mockRepository,
                secondStatuses::add,
                result -> secondLatch.countDown(),
                FIXED_CLOCK,
                DEFAULT_TIMEOUT
            );
            secondHandler.startConnection();
            secondLatch.await(2, TimeUnit.SECONDS);

            // Then - both connections should have completed
            assertThat(firstStatuses).contains(ConnectionStatus.SUCCESS);
            assertThat(secondStatuses).contains(ConnectionStatus.SUCCESS);
            verify(mockOAuthService, times(2)).authenticate();
        }
    }

    // === Helper Methods ===

    private OAuthTokens createTestTokens() {
        return OAuthTokens.create(
            "test-access-token",
            "test-refresh-token",
            14400, // 4 hours
            "Bearer",
            "read:vat write:vat"
        );
    }
}
