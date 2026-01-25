package uk.selfemploy.core.hmrc;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.config.HmrcConfig;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing HMRC connection lifecycle.
 *
 * <p>Provides:
 * <ul>
 *   <li>Connection status monitoring</li>
 *   <li>Prerequisites tracking for setup wizard</li>
 *   <li>OAuth flow management</li>
 *   <li>User guide content for registration</li>
 * </ul>
 */
@ApplicationScoped
public class HmrcConnectionService {

    private static final Logger LOG = LoggerFactory.getLogger(HmrcConnectionService.class);

    private final HmrcOAuthService oAuthService;
    private final TokenStorageService tokenStorageService;
    private final HmrcConfig hmrcConfig;

    // Track prerequisite completion status
    private final Map<String, Boolean> prerequisiteStatus = new ConcurrentHashMap<>();

    // Prerequisite definitions
    private static final List<HmrcPrerequisite> PREREQUISITES = List.of(
        new HmrcPrerequisite(
            "self_assessment_registration",
            "Register for Self Assessment",
            "You must be registered for Self Assessment with HMRC before you can submit tax returns.",
            "https://www.gov.uk/register-for-self-assessment",
            false
        ),
        new HmrcPrerequisite(
            "utr_number",
            "Unique Taxpayer Reference (UTR)",
            "You need your 10-digit UTR number. HMRC sends this by post within 10 days of registration.",
            "https://www.gov.uk/find-lost-utr-number",
            false
        ),
        new HmrcPrerequisite(
            "government_gateway",
            "Government Gateway Account",
            "You need a Government Gateway account to authorize this app to submit on your behalf.",
            "https://www.gov.uk/log-in-register-hmrc-online-services",
            false
        )
    );

    @Inject
    public HmrcConnectionService(
            HmrcOAuthService oAuthService,
            TokenStorageService tokenStorageService,
            HmrcConfig hmrcConfig) {
        this.oAuthService = oAuthService;
        this.tokenStorageService = tokenStorageService;
        this.hmrcConfig = hmrcConfig;
    }

    /**
     * Gets the current HMRC connection status.
     *
     * @return Current connection status
     */
    public ConnectionStatus getConnectionStatus() {
        // Check if app is configured
        if (!hmrcConfig.isConfigured()) {
            return ConnectionStatus.notConfigured();
        }

        // Check for stored tokens
        Optional<OAuthTokens> storedTokens = tokenStorageService.loadTokens();
        if (storedTokens.isEmpty()) {
            return ConnectionStatus.notConnected();
        }

        OAuthTokens tokens = storedTokens.get();

        // Check if tokens are expired
        if (tokens.isExpired()) {
            if (tokens.refreshToken() != null) {
                return ConnectionStatus.expired();
            }
            return ConnectionStatus.notConnected();
        }

        // Update OAuth service with stored tokens
        oAuthService.setTokens(tokens);

        return ConnectionStatus.connected(tokens.getExpiryTime());
    }

    /**
     * Gets the list of prerequisites with current completion status.
     *
     * @return List of prerequisites
     */
    public List<HmrcPrerequisite> getPrerequisites() {
        return PREREQUISITES.stream()
            .map(p -> p.withComplete(prerequisiteStatus.getOrDefault(p.id(), false)))
            .toList();
    }

    /**
     * Marks a prerequisite as complete or incomplete.
     *
     * @param prerequisiteId The prerequisite ID
     * @param complete       Whether it's complete
     */
    public void markPrerequisiteComplete(String prerequisiteId, boolean complete) {
        prerequisiteStatus.put(prerequisiteId, complete);
        LOG.info("Prerequisite '{}' marked as {}", prerequisiteId, complete ? "complete" : "incomplete");
    }

    /**
     * Checks if all prerequisites are met.
     *
     * @return true if all prerequisites are complete
     */
    public boolean areAllPrerequisitesMet() {
        return PREREQUISITES.stream()
            .allMatch(p -> prerequisiteStatus.getOrDefault(p.id(), false));
    }

    /**
     * Initiates the HMRC OAuth connection flow.
     *
     * @return Future that completes with the connection result
     */
    public CompletableFuture<ConnectionResult> connect() {
        LOG.info("Initiating HMRC connection");

        return oAuthService.authenticate()
            .thenApply(tokens -> {
                // Save tokens on successful authentication
                tokenStorageService.saveTokens(tokens);
                LOG.info("Successfully connected to HMRC");
                return ConnectionResult.connected();
            })
            .exceptionally(error -> {
                LOG.error("Failed to connect to HMRC", error);
                return ConnectionResult.failure(
                    "Failed to connect to HMRC: " + error.getMessage()
                );
            });
    }

    /**
     * Disconnects from HMRC by clearing stored tokens.
     */
    public void disconnect() {
        LOG.info("Disconnecting from HMRC");
        tokenStorageService.deleteTokens();
        oAuthService.disconnect();
    }

    /**
     * Refreshes the HMRC connection using the refresh token.
     *
     * @return Future that completes with the connection result
     */
    public CompletableFuture<ConnectionResult> refreshConnection() {
        LOG.info("Refreshing HMRC connection");

        return oAuthService.refreshAccessToken()
            .thenApply(tokens -> {
                tokenStorageService.saveTokens(tokens);
                LOG.info("Successfully refreshed HMRC connection");
                return ConnectionResult.refreshed();
            })
            .exceptionally(error -> {
                LOG.error("Failed to refresh HMRC connection", error);
                return ConnectionResult.failure(
                    "Failed to refresh connection: " + error.getMessage()
                );
            });
    }

    /**
     * Gets guide content for registering for Self Assessment.
     *
     * @return Guide content
     */
    public HmrcGuideContent getRegistrationGuide() {
        return new HmrcGuideContent(
            "Register for Self Assessment",
            "If you're self-employed, you need to register for Self Assessment with HMRC to submit your tax returns.",
            List.of(
                new HmrcGuideContent.GuideStep(
                    1,
                    "Check if you need to register",
                    "You must register if you earned more than GBP 1,000 from self-employment, " +
                    "or if you need to prove you're self-employed for a mortgage.",
                    "https://www.gov.uk/check-if-you-need-to-register-self-assessment"
                ),
                new HmrcGuideContent.GuideStep(
                    2,
                    "Register online",
                    "Use the HMRC online service to register as self-employed. " +
                    "You'll need your National Insurance number.",
                    "https://www.gov.uk/register-for-self-assessment/self-employed"
                ),
                new HmrcGuideContent.GuideStep(
                    3,
                    "Wait for your UTR",
                    "HMRC will send you a Unique Taxpayer Reference (UTR) by post within 10 working days. " +
                    "Keep this safe - you'll need it for all HMRC communications.",
                    null
                ),
                new HmrcGuideContent.GuideStep(
                    4,
                    "Set up your Government Gateway",
                    "Create a Government Gateway account to access HMRC online services. " +
                    "Link it to your Self Assessment registration.",
                    "https://www.gov.uk/log-in-register-hmrc-online-services"
                )
            ),
            "https://www.gov.uk/register-for-self-assessment"
        );
    }

    /**
     * Gets guide content for finding or applying for a UTR.
     *
     * @return Guide content
     */
    public HmrcGuideContent getUtrGuide() {
        return new HmrcGuideContent(
            "Your Unique Taxpayer Reference (UTR)",
            "Your UTR is a 10-digit number that identifies you for Self Assessment. " +
            "You'll receive it by post after registering with HMRC.",
            List.of(
                new HmrcGuideContent.GuideStep(
                    1,
                    "Check your records",
                    "Your UTR is on previous Self Assessment tax returns, " +
                    "HMRC letters about Self Assessment, or your Personal Tax Account.",
                    null
                ),
                new HmrcGuideContent.GuideStep(
                    2,
                    "Use your Personal Tax Account",
                    "Sign in to your HMRC Personal Tax Account to view your UTR online.",
                    "https://www.gov.uk/personal-tax-account"
                ),
                new HmrcGuideContent.GuideStep(
                    3,
                    "Request a reminder",
                    "If you can't find your UTR, you can request HMRC send you a reminder.",
                    "https://www.gov.uk/find-lost-utr-number"
                )
            ),
            "https://www.gov.uk/find-lost-utr-number"
        );
    }

    /**
     * Gets guide content for setting up Government Gateway.
     *
     * @return Guide content
     */
    public HmrcGuideContent getGovernmentGatewayGuide() {
        return new HmrcGuideContent(
            "Government Gateway Account",
            "Government Gateway is how you sign in to HMRC online services. " +
            "This app uses Government Gateway to securely connect to your HMRC account.",
            List.of(
                new HmrcGuideContent.GuideStep(
                    1,
                    "Create a Government Gateway account",
                    "If you don't have one, create a Government Gateway user ID and password.",
                    "https://www.gov.uk/log-in-register-hmrc-online-services/register"
                ),
                new HmrcGuideContent.GuideStep(
                    2,
                    "Enroll for Self Assessment",
                    "Link your Government Gateway account to your Self Assessment registration " +
                    "using your UTR number.",
                    "https://www.gov.uk/log-in-register-hmrc-online-services/enrol"
                ),
                new HmrcGuideContent.GuideStep(
                    3,
                    "Authorize this app",
                    "When you click 'Connect to HMRC' in this app, you'll be taken to Government Gateway " +
                    "to sign in and authorize the UK Self-Employment Manager to access your tax information.",
                    null
                )
            ),
            "https://www.gov.uk/log-in-register-hmrc-online-services"
        );
    }

    /**
     * Gets the authorization URL for the OAuth flow.
     * Useful for displaying to the user.
     *
     * @return The authorization URL
     */
    public String getAuthorizationUrl() {
        return hmrcConfig.authorizeUrl();
    }

    /**
     * Checks if HMRC API credentials are configured.
     *
     * @return true if configured
     */
    public boolean isConfigured() {
        return hmrcConfig.isConfigured();
    }
}
