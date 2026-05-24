package uk.selfemploy.hmrc.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.fraud.FraudPreventionService;
import uk.selfemploy.hmrc.logging.HmrcPiiRedactor;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.TokenStorageService;

import java.util.Map;
import java.util.Optional;

/**
 * Client headers factory for HMRC REST API calls.
 *
 * <p>Adds: Authorization (Bearer token) and HMRC fraud-prevention headers.
 *
 * <p><strong>Does NOT add an Accept header.</strong> Each MTD ITSA API has its own
 * required version (Calculations v8, Self-Employment Business v5, Obligations v3,
 * BSAS v7, etc.); each REST client interface declares its own Accept header via
 * {@code @ClientHeaderParam(name="Accept", value="application/vnd.hmrc.{N}.0+json")}.
 *
 * <p>and ADR-017.
 */
@ApplicationScoped
public class HmrcHeaderFactory implements ClientHeadersFactory {

    private static final Logger log = LoggerFactory.getLogger(HmrcHeaderFactory.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";

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

        result.putAll(incomingHeaders);
        // Merge client-declared headers (e.g. @ClientHeaderParam Accept) so the
        // per-endpoint MTD API version survives factory processing.
        result.putAll(clientOutgoingHeaders);

        if (!result.containsKey(AUTHORIZATION_HEADER)) {
            addAuthorizationHeader(result);
        }

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
            log.error("Failed to generate fraud prevention headers: {}",
                HmrcPiiRedactor.redact(String.valueOf(e.getMessage())));
            // Continue without fraud headers - API will likely reject the request
            // but let HMRC return the specific error
        }
    }
}
