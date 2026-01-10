package uk.selfemploy.hmrc.fraud.collectors;

import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.hmrc.fraud.FraudPreventionHeaders;
import uk.selfemploy.hmrc.fraud.HeaderCollector;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Collects local user IDs.
 * Format: os=<username>
 */
@ApplicationScoped
public class UserIdsCollector implements HeaderCollector {

    @Override
    public String getHeaderName() {
        return FraudPreventionHeaders.Headers.USER_IDS;
    }

    @Override
    public String collect() {
        String username = System.getProperty("user.name", "unknown");
        // URL encode the username
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        return "os=" + encodedUsername;
    }
}
