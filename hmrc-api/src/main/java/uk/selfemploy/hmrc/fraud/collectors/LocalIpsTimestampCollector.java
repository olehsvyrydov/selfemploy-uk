package uk.selfemploy.hmrc.fraud.collectors;

import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.hmrc.fraud.FraudPreventionHeaders;
import uk.selfemploy.hmrc.fraud.HeaderCollector;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Collects the timestamp at which {@link LocalIpsCollector} sampled the
 * device IPs. HMRC requires this companion header for DESKTOP_APP_DIRECT.
 *
 * <p>Format: ISO-8601 UTC with milliseconds, e.g. {@code 2024-01-15T13:45:01.234Z}.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/guides/fraud-prevention/">HMRC Fraud Prevention</a>
 */
@ApplicationScoped
public class LocalIpsTimestampCollector implements HeaderCollector {

    private static final DateTimeFormatter ISO_INSTANT_MILLIS =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(java.time.ZoneOffset.UTC);

    @Override
    public String getHeaderName() {
        return FraudPreventionHeaders.Headers.LOCAL_IPS_TIMESTAMP;
    }

    @Override
    public String collect() {
        return ISO_INSTANT_MILLIS.format(Instant.now());
    }
}
