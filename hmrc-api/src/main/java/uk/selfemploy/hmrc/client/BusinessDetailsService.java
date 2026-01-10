package uk.selfemploy.hmrc.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.client.dto.BusinessDetails;
import uk.selfemploy.hmrc.exception.HmrcApiException;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;

/**
 * Service for retrieving business details from HMRC Self-Employment API.
 * Handles authentication, NINO validation, and API calls.
 */
@ApplicationScoped
public class BusinessDetailsService {

    private static final Logger log = LoggerFactory.getLogger(BusinessDetailsService.class);

    /**
     * NINO format: 2 letters, 6 digits, 1 letter (A-D).
     * First letter cannot be D, F, I, Q, U, V.
     * Second letter cannot be D, F, I, O, Q, U, V.
     * Prefix cannot be BG, GB, KN, NK, NT, TN, ZZ.
     */
    private static final Pattern NINO_PATTERN = Pattern.compile(
        "^(?!BG|GB|KN|NK|NT|TN|ZZ)[A-CEGHJ-PR-TW-Z][A-CEGHJ-NPR-TW-Z][0-9]{6}[A-D]$",
        Pattern.CASE_INSENSITIVE
    );

    private final BusinessDetailsClient businessDetailsClient;
    private final TokenStorageService tokenStorageService;

    @Inject
    public BusinessDetailsService(
            @RestClient BusinessDetailsClient businessDetailsClient,
            TokenStorageService tokenStorageService) {
        this.businessDetailsClient = businessDetailsClient;
        this.tokenStorageService = tokenStorageService;
    }

    /**
     * Lists all self-employment businesses for a National Insurance Number.
     *
     * @param nino National Insurance Number (with or without spaces)
     * @return CompletionStage with list of business details
     * @throws IllegalArgumentException if NINO is invalid
     * @throws HmrcApiException if not authenticated
     */
    public CompletionStage<List<BusinessDetails>> listBusinesses(String nino) {
        return CompletableFuture.supplyAsync(() -> {
            String normalizedNino = validateAndNormalizeNino(nino);
            String authorization = getAuthorizationHeader();
            return new Object[] { normalizedNino, authorization };
        }).thenCompose(args -> {
            String normalizedNino = (String) args[0];
            String authorization = (String) args[1];

            log.info("Listing businesses for NINO: {}****{}",
                normalizedNino.substring(0, 2), normalizedNino.substring(7));

            return businessDetailsClient.listBusinesses(normalizedNino, authorization)
                .thenApply(response -> {
                    List<BusinessDetails> businesses = response.selfEmployments();
                    log.info("Found {} self-employment businesses",
                        businesses != null ? businesses.size() : 0);
                    return businesses != null ? businesses : List.of();
                });
        });
    }

    /**
     * Gets details for a specific self-employment business.
     *
     * @param nino National Insurance Number
     * @param businessId Business ID (e.g., XAIS12345678901)
     * @return CompletionStage with business details
     * @throws IllegalArgumentException if NINO is invalid
     * @throws HmrcApiException if not authenticated
     */
    public CompletionStage<BusinessDetails> getBusinessDetails(String nino, String businessId) {
        return CompletableFuture.supplyAsync(() -> {
            String normalizedNino = validateAndNormalizeNino(nino);
            String authorization = getAuthorizationHeader();
            return new Object[] { normalizedNino, authorization };
        }).thenCompose(args -> {
            String normalizedNino = (String) args[0];
            String authorization = (String) args[1];

            log.info("Getting business details for business ID: {}", businessId);

            return businessDetailsClient.getBusinessDetails(normalizedNino, businessId, authorization);
        });
    }

    /**
     * Checks if there is a valid (non-expired) connection to HMRC.
     *
     * @return true if authenticated with valid tokens
     */
    public boolean isConnected() {
        Optional<OAuthTokens> tokens = tokenStorageService.loadTokens();

        if (tokens.isEmpty()) {
            return false;
        }

        OAuthTokens oauthTokens = tokens.get();
        return !oauthTokens.isExpired();
    }

    /**
     * Gets the current access token for manual API calls.
     *
     * @return Optional containing access token if available
     */
    public Optional<String> getAccessToken() {
        return tokenStorageService.loadTokens()
            .filter(tokens -> !tokens.isExpired())
            .map(OAuthTokens::accessToken);
    }

    private String validateAndNormalizeNino(String nino) {
        if (nino == null || nino.isBlank()) {
            throw new IllegalArgumentException("NINO cannot be null or empty");
        }

        // Remove spaces and convert to uppercase
        String normalized = nino.replaceAll("\\s+", "").toUpperCase();

        if (!NINO_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid NINO format: " + maskNino(normalized));
        }

        return normalized;
    }

    private String getAuthorizationHeader() {
        Optional<OAuthTokens> tokens = tokenStorageService.loadTokens();

        if (tokens.isEmpty()) {
            throw new HmrcApiException("Not authenticated - please connect to HMRC first");
        }

        OAuthTokens oauthTokens = tokens.get();
        return "Bearer " + oauthTokens.accessToken();
    }

    private String maskNino(String nino) {
        if (nino == null || nino.length() < 4) {
            return "****";
        }
        return nino.substring(0, 2) + "****" + nino.substring(nino.length() - 1);
    }
}
