package uk.selfemploy.hmrc.fraud.collectors;

import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.hmrc.fraud.FraudPreventionHeaders;
import uk.selfemploy.hmrc.fraud.HeaderCollector;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Collects the client's timezone offset.
 * Format: UTC+HH:MM or UTC-HH:MM
 */
@ApplicationScoped
public class TimezoneCollector implements HeaderCollector {

    @Override
    public String getHeaderName() {
        return FraudPreventionHeaders.Headers.TIMEZONE;
    }

    @Override
    public String collect() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        int offsetSeconds = now.getOffset().getTotalSeconds();

        // Format as UTC+HH:MM or UTC-HH:MM
        String sign = offsetSeconds >= 0 ? "+" : "-";
        int absOffset = Math.abs(offsetSeconds);
        int hours = absOffset / 3600;
        int minutes = (absOffset % 3600) / 60;

        return String.format("UTC%s%02d:%02d", sign, hours, minutes);
    }
}
