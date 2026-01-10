package uk.selfemploy.hmrc.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.fraud.FraudPreventionService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageService;

import java.util.Map;
import java.util.Optional;

/**
 * Client headers factory for HMRC REST API calls.
 * Automatically adds:
 * - Authorization header with Bearer token
 * - Fraud prevention headers (required by HMRC)
 * - Accept header for HMRC API versioning
 */
@ApplicationScoped
public class HmrcHeaderFactory implements ClientHeadersFactory {

    private static final Logger log = LoggerFactory.getLogger(HmrcHeaderFactory.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String HMRC_ACCEPT_TYPE = "application/vnd.hmrc.1.0+json";

    private final TokenStorageService tokenStorageService;
    private final FraudPreventionService fraudPreventionService;

    @Inject
    public HmrcHeaderFactory(TokenStorageService tokenStorageService,
                             FraudPreventionService fraudPreventionService) {
        this.tokenStorageService = tokenStorageService;
        this.fraudPreventionService = fraudPreventionService;
    }

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                  MultivaluedMap<String, String> clientOutgoingHeaders) {
        log.debug("Updating headers for HMRC API call");

        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();

        // Copy incoming headers first
        result.putAll(incomingHeaders);

        // Add Accept header for HMRC API versioning
        if (!result.containsKey(ACCEPT_HEADER)) {
            result.add(ACCEPT_HEADER, HMRC_ACCEPT_TYPE);
        }

        // Add Authorization header if tokens available and not already set
        if (!result.containsKey(AUTHORIZATION_HEADER)) {
            addAuthorizationHeader(result);
        }

        // Add fraud prevention headers (required by HMRC)
        addFraudPreventionHeaders(result);

        log.debug("Headers updated: {} headers total", result.size());
        return result;
    }

    private void addAuthorizationHeader(MultivaluedMap<String, String> headers) {
        Optional<OAuthTokens> tokens = tokenStorageService.loadTokens();

        if (tokens.isPresent()) {
            OAuthTokens oauthTokens = tokens.get();
            String authValue = "Bearer " + oauthTokens.accessToken();
            headers.add(AUTHORIZATION_HEADER, authValue);
            log.debug("Authorization header added");
        } else {
            log.debug("No OAuth tokens available - Authorization header not added");
        }
    }

    private void addFraudPreventionHeaders(MultivaluedMap<String, String> headers) {
        try {
            Map<String, String> fraudHeaders = fraudPreventionService.generateHeaders();

            for (Map.Entry<String, String> entry : fraudHeaders.entrySet()) {
                if (!headers.containsKey(entry.getKey())) {
                    headers.add(entry.getKey(), entry.getValue());
                }
            }

            log.debug("Added {} fraud prevention headers", fraudHeaders.size());
        } catch (Exception e) {
            log.error("Failed to generate fraud prevention headers", e);
            // Continue without fraud headers - API will likely reject the request
            // but let HMRC return the specific error
        }
    }
}
