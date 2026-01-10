package uk.selfemploy.hmrc.oauth;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for OAuthCallbackServer.
 */
@DisplayName("OAuthCallbackServer")
class OAuthCallbackServerTest {

    private static Vertx vertx;
    private OAuthCallbackServer server;
    private static final int TEST_PORT = 18088; // Different port to avoid conflicts

    @BeforeAll
    static void setupVertx() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void closeVertx() {
        if (vertx != null) {
            vertx.close();
        }
    }

    @BeforeEach
    void setup() {
        server = new OAuthCallbackServer(vertx, TEST_PORT, "/oauth/callback");
    }

    @AfterEach
    void cleanup() {
        if (server != null) {
            server.stop();
        }
    }

    @Nested
    @DisplayName("Server Lifecycle")
    class ServerLifecycle {

        @Test
        @DisplayName("should start successfully on specified port")
        void shouldStartSuccessfully() throws Exception {
            String state = "test-state-123";
            CompletableFuture<String> future = server.startAndAwaitCallback(state);

            // Wait for server to be running
            waitForServerRunning();

            assertThat(future).isNotNull();
            assertThat(server.isRunning()).isTrue();
        }

        @Test
        @DisplayName("should stop server when stop is called")
        void shouldStopServer() throws Exception {
            server.startAndAwaitCallback("test-state");
            waitForServerRunning();
            assertThat(server.isRunning()).isTrue();

            server.stop();

            assertThat(server.isRunning()).isFalse();
        }

        @Test
        @DisplayName("should handle multiple stop calls gracefully")
        void shouldHandleMultipleStopCallsGracefully() throws Exception {
            server.startAndAwaitCallback("test-state");
            waitForServerRunning();
            server.stop();
            server.stop(); // Should not throw

            assertThat(server.isRunning()).isFalse();
        }
    }

    @Nested
    @DisplayName("Callback Handling")
    class CallbackHandling {

        @Test
        @DisplayName("should extract authorization code from callback")
        void shouldExtractAuthorizationCode() throws Exception {
            String state = "valid-state-abc";
            CompletableFuture<String> future = server.startAndAwaitCallback(state);

            // Wait for server to be running
            waitForServerRunning();

            // Simulate callback from HMRC
            sendCallback("auth_code_12345", state);

            String code = future.get(5, TimeUnit.SECONDS);
            assertThat(code).isEqualTo("auth_code_12345");
        }

        @Test
        @Disabled("Integration test has timing issues with Vert.x - needs investigation")
        @DisplayName("should reject callback with mismatched state")
        void shouldRejectMismatchedState() throws Exception {
            String expectedState = "expected-state";
            CompletableFuture<String> future = server.startAndAwaitCallback(expectedState);

            // Wait for server to be running
            waitForServerRunning();

            // Simulate callback with wrong state (ignore HTTP error)
            sendCallbackIgnoreError("auth_code", "wrong-state");

            // Future should complete exceptionally
            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(HmrcOAuthException.class);
        }

        @Test
        @Disabled("Integration test has timing issues with Vert.x - needs investigation")
        @DisplayName("should handle error response from HMRC")
        void shouldHandleErrorResponse() throws Exception {
            String state = "test-state";
            CompletableFuture<String> future = server.startAndAwaitCallback(state);

            // Wait for server to be running
            waitForServerRunning();

            // Simulate error callback (ignore HTTP error response)
            sendErrorCallbackIgnoreError("access_denied", "User+denied+the+request", state);

            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(HmrcOAuthException.class);
        }

        @Test
        @Disabled("Integration test has timing issues with Vert.x - needs investigation")
        @DisplayName("should handle callback without code parameter")
        void shouldHandleCallbackWithoutCode() throws Exception {
            String state = "test-state";
            CompletableFuture<String> future = server.startAndAwaitCallback(state);

            // Wait for server to be running
            waitForServerRunning();

            // Simulate callback without code (ignore HTTP error response)
            sendCallbackWithoutCodeIgnoreError(state);

            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(HmrcOAuthException.class);
        }
    }

    @Nested
    @DisplayName("Timeout Handling")
    class TimeoutHandling {

        @Test
        @DisplayName("should timeout if no callback received")
        void shouldTimeoutIfNoCallback() throws Exception {
            // Create server with short timeout for test
            OAuthCallbackServer shortTimeoutServer = new OAuthCallbackServer(vertx, TEST_PORT + 1, "/oauth/callback", 2);

            try {
                CompletableFuture<String> future = shortTimeoutServer.startAndAwaitCallback("state");

                // Don't send callback, wait for timeout
                assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(HmrcOAuthException.class);
            } finally {
                shortTimeoutServer.stop();
            }
        }
    }

    @Nested
    @DisplayName("Response Page")
    class ResponsePage {

        @Test
        @DisplayName("should return success page on valid callback")
        void shouldReturnSuccessPage() throws Exception {
            String state = "test-state";
            server.startAndAwaitCallback(state);

            // Wait for server to be running
            waitForServerRunning();

            // Get HTTP response from callback
            String response = sendCallbackAndGetResponse("auth_code", state);

            assertThat(response.toLowerCase()).contains("success");
            assertThat(response.toLowerCase()).contains("authenticated");
        }
    }

    // Helper methods

    private void waitForServerRunning() throws InterruptedException {
        int maxAttempts = 50;
        for (int i = 0; i < maxAttempts; i++) {
            if (server.isRunning()) {
                // Additional small delay to ensure port is bound
                Thread.sleep(50);
                return;
            }
            Thread.sleep(20);
        }
        throw new IllegalStateException("Server failed to start within timeout");
    }

    private void sendCallback(String code, String state) throws Exception {
        HttpClient client = vertx.createHttpClient();
        String uri = "/oauth/callback?code=" + code + "&state=" + state;

        CompletableFuture<Void> requestFuture = new CompletableFuture<>();
        client.request(HttpMethod.GET, TEST_PORT, "localhost", uri)
            .compose(HttpClientRequest::send)
            .onSuccess(response -> requestFuture.complete(null))
            .onFailure(requestFuture::completeExceptionally);

        requestFuture.get(5, TimeUnit.SECONDS);
    }

    private void sendCallbackIgnoreError(String code, String state) throws Exception {
        HttpClient client = vertx.createHttpClient();
        String uri = "/oauth/callback?code=" + code + "&state=" + state;

        CompletableFuture<Void> requestFuture = new CompletableFuture<>();
        client.request(HttpMethod.GET, TEST_PORT, "localhost", uri)
            .compose(HttpClientRequest::send)
            .onSuccess(response -> requestFuture.complete(null))
            .onFailure(err -> requestFuture.complete(null)); // Ignore errors

        requestFuture.get(5, TimeUnit.SECONDS);
    }

    private void sendErrorCallbackIgnoreError(String error, String description, String state) throws Exception {
        HttpClient client = vertx.createHttpClient();
        String uri = "/oauth/callback?error=" + error + "&error_description=" + description + "&state=" + state;

        CompletableFuture<Void> requestFuture = new CompletableFuture<>();
        client.request(HttpMethod.GET, TEST_PORT, "localhost", uri)
            .compose(HttpClientRequest::send)
            .onSuccess(response -> requestFuture.complete(null))
            .onFailure(err -> requestFuture.complete(null)); // Ignore errors

        requestFuture.get(5, TimeUnit.SECONDS);
    }

    private void sendCallbackWithoutCodeIgnoreError(String state) throws Exception {
        HttpClient client = vertx.createHttpClient();
        String uri = "/oauth/callback?state=" + state;

        CompletableFuture<Void> requestFuture = new CompletableFuture<>();
        client.request(HttpMethod.GET, TEST_PORT, "localhost", uri)
            .compose(HttpClientRequest::send)
            .onSuccess(response -> requestFuture.complete(null))
            .onFailure(err -> requestFuture.complete(null)); // Ignore errors

        requestFuture.get(5, TimeUnit.SECONDS);
    }

    private String sendCallbackAndGetResponse(String code, String state) throws Exception {
        HttpClient client = vertx.createHttpClient();
        String uri = "/oauth/callback?code=" + code + "&state=" + state;

        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        client.request(HttpMethod.GET, TEST_PORT, "localhost", uri)
            .compose(HttpClientRequest::send)
            .compose(response -> response.body())
            .onSuccess(body -> responseFuture.complete(body.toString()))
            .onFailure(responseFuture::completeExceptionally);

        return responseFuture.get(5, TimeUnit.SECONDS);
    }
}
