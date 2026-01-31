package uk.selfemploy.hmrc.oauth;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.exception.HmrcOAuthException;
import uk.selfemploy.hmrc.exception.HmrcOAuthException.OAuthError;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Temporary HTTP server that listens for OAuth2 callback from HMRC.
 * Starts on localhost, receives the authorization code, and shuts down.
 */
public class OAuthCallbackServer {

    private static final Logger log = LoggerFactory.getLogger(OAuthCallbackServer.class);

    private static final String SUCCESS_HTML = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Authentication Successful</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                       display: flex; justify-content: center; align-items: center; height: 100vh;
                       margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
                .card { background: white; padding: 40px 60px; border-radius: 12px; text-align: center;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.2); }
                .success-icon { font-size: 64px; color: #28a745; margin-bottom: 20px; }
                h1 { color: #333; margin: 0 0 10px 0; }
                p { color: #666; margin: 0; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="success-icon">✓</div>
                <h1>Successfully Authenticated</h1>
                <p>You can close this window and return to the application.</p>
            </div>
        </body>
        </html>
        """;

    private static final String ERROR_HTML = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Authentication Failed</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                       display: flex; justify-content: center; align-items: center; height: 100vh;
                       margin: 0; background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%); }
                .card { background: white; padding: 40px 60px; border-radius: 12px; text-align: center;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.2); }
                .error-icon { font-size: 64px; color: #dc3545; margin-bottom: 20px; }
                h1 { color: #333; margin: 0 0 10px 0; }
                p { color: #666; margin: 0; }
            </style>
        </head>
        <body>
            <div class="card">
                <div class="error-icon">✗</div>
                <h1>Authentication Failed</h1>
                <p>%s</p>
                <p style="margin-top: 20px;">Please close this window and try again.</p>
            </div>
        </body>
        </html>
        """;

    private final Vertx vertx;
    private final int port;
    private final String callbackPath;
    private final int timeoutSeconds;

    private HttpServer server;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> expectedState = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<String>> callbackFuture = new AtomicReference<>();
    private ScheduledExecutorService timeoutExecutor;

    /**
     * Creates callback server with default 2-minute timeout.
     */
    public OAuthCallbackServer(Vertx vertx, int port, String callbackPath) {
        this(vertx, port, callbackPath, 120);
    }

    /**
     * Creates callback server with custom timeout.
     */
    public OAuthCallbackServer(Vertx vertx, int port, String callbackPath, int timeoutSeconds) {
        this.vertx = vertx;
        this.port = port;
        this.callbackPath = callbackPath;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Starts the server and returns a future that completes with the authorization code.
     *
     * @param state The state parameter to validate against CSRF attacks
     * @return Future that completes with auth code or fails with HmrcOAuthException
     */
    public CompletableFuture<String> startAndAwaitCallback(String state) {
        if (running.getAndSet(true)) {
            return CompletableFuture.failedFuture(
                new HmrcOAuthException(OAuthError.SERVER_ERROR, "Server already running")
            );
        }

        expectedState.set(state);
        CompletableFuture<String> future = new CompletableFuture<>();
        callbackFuture.set(future);

        // Start timeout timer
        timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        timeoutExecutor.schedule(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(new HmrcOAuthException(OAuthError.TIMEOUT));
                stop();
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        // Start HTTP server
        server = vertx.createHttpServer();
        server.requestHandler(this::handleRequest);

        CompletableFuture<String> result = new CompletableFuture<>();

        server.listen(port)
            .onSuccess(s -> {
                log.info("OAuth callback server started on port {}", port);
                // Forward the callback future's result
                future.whenComplete((code, error) -> {
                    if (error != null) {
                        result.completeExceptionally(error);
                    } else {
                        result.complete(code);
                    }
                });
            })
            .onFailure(error -> {
                log.error("Failed to start OAuth callback server on port {}", port, error);
                running.set(false);
                result.completeExceptionally(
                    new HmrcOAuthException(OAuthError.PORT_IN_USE, error)
                );
            });

        return result;
    }

    /**
     * Stops the callback server and completes the pending future with USER_CANCELLED
     * if it has not already been completed (by a successful callback, error, or timeout).
     *
     * <p>This ensures the CompletableFuture chain always resolves when stop() is called
     * externally, preventing the UI from hanging indefinitely.</p>
     */
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        log.debug("Stopping OAuth callback server");

        // Complete the pending future with USER_CANCELLED if not already done.
        // This must happen BEFORE shutting down the executor/server to ensure
        // the future chain resolves for any waiting callers.
        CompletableFuture<String> future = callbackFuture.get();
        if (future != null && !future.isDone()) {
            future.completeExceptionally(new HmrcOAuthException(OAuthError.USER_CANCELLED));
            log.debug("Completed callback future with USER_CANCELLED");
        }

        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
            timeoutExecutor = null;
        }

        if (server != null) {
            server.close()
                .onSuccess(v -> log.debug("OAuth callback server stopped"))
                .onFailure(e -> log.warn("Error stopping OAuth callback server", e));
            server = null;
        }
    }

    /**
     * Checks if the server is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }

    private void handleRequest(HttpServerRequest request) {
        String path = request.path();

        if (!path.equals(callbackPath)) {
            request.response()
                .setStatusCode(404)
                .end("Not Found");
            return;
        }

        String code = request.getParam("code");
        String state = request.getParam("state");
        String error = request.getParam("error");
        String errorDescription = request.getParam("error_description");

        CompletableFuture<String> future = callbackFuture.get();

        // Handle OAuth error response
        if (error != null) {
            log.warn("OAuth error received: {} - {}", error, errorDescription);
            sendErrorResponse(request, errorDescription != null ? errorDescription : error);
            future.completeExceptionally(
                new HmrcOAuthException(mapOAuthError(error), errorDescription)
            );
            stop();
            return;
        }

        // Validate state parameter
        String expected = expectedState.get();
        if (state == null || !state.equals(expected)) {
            log.error("State mismatch: expected={}, received={}", expected, state);
            sendErrorResponse(request, "Invalid state parameter - possible security issue");
            future.completeExceptionally(new HmrcOAuthException(OAuthError.INVALID_STATE));
            stop();
            return;
        }

        // Validate code parameter
        if (code == null || code.isBlank()) {
            log.error("No authorization code in callback");
            sendErrorResponse(request, "No authorization code received");
            future.completeExceptionally(
                new HmrcOAuthException(OAuthError.CALLBACK_ERROR, "No authorization code")
            );
            stop();
            return;
        }

        // Success!
        log.info("OAuth authorization code received successfully");
        sendSuccessResponse(request);
        future.complete(code);
        stop();
    }

    private void sendSuccessResponse(HttpServerRequest request) {
        request.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "text/html")
            .end(SUCCESS_HTML);
    }

    private void sendErrorResponse(HttpServerRequest request, String message) {
        String html = ERROR_HTML.formatted(escapeHtml(message));
        request.response()
            .setStatusCode(400)
            .putHeader("Content-Type", "text/html")
            .end(html);
    }

    private OAuthError mapOAuthError(String error) {
        return switch (error) {
            case "access_denied" -> OAuthError.ACCESS_DENIED;
            case "invalid_grant" -> OAuthError.INVALID_GRANT;
            case "invalid_client" -> OAuthError.INVALID_CLIENT;
            case "server_error" -> OAuthError.SERVER_ERROR;
            default -> OAuthError.CALLBACK_ERROR;
        };
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }
}
