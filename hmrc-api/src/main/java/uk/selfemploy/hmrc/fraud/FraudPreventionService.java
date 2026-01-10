package uk.selfemploy.hmrc.fraud;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.fraud.collectors.DeviceIdCollector;
import uk.selfemploy.hmrc.fraud.collectors.LocalIpsCollector;
import uk.selfemploy.hmrc.fraud.collectors.TimezoneCollector;
import uk.selfemploy.hmrc.fraud.collectors.UserIdsCollector;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for generating HMRC fraud prevention headers.
 * Collects device and environment information required by HMRC for all API calls.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/guides/fraud-prevention/">HMRC Fraud Prevention</a>
 */
@ApplicationScoped
public class FraudPreventionService {

    private static final Logger log = LoggerFactory.getLogger(FraudPreventionService.class);

    private static final String APP_NAME = "SelfEmployment";

    private final DeviceIdCollector deviceIdCollector;
    private final TimezoneCollector timezoneCollector;
    private final LocalIpsCollector localIpsCollector;
    private final UserIdsCollector userIdsCollector;
    private final String appVersion;

    @Inject
    public FraudPreventionService(
            DeviceIdCollector deviceIdCollector,
            TimezoneCollector timezoneCollector,
            LocalIpsCollector localIpsCollector,
            UserIdsCollector userIdsCollector,
            @ConfigProperty(name = "quarkus.application.version", defaultValue = "0.1.0") String appVersion) {
        this.deviceIdCollector = deviceIdCollector;
        this.timezoneCollector = timezoneCollector;
        this.localIpsCollector = localIpsCollector;
        this.userIdsCollector = userIdsCollector;
        this.appVersion = appVersion;
    }

    /**
     * Generates all required fraud prevention headers.
     *
     * @return Map of header names to values
     */
    public Map<String, String> generateHeaders() {
        log.debug("Generating fraud prevention headers");

        Map<String, String> headers = new LinkedHashMap<>();

        // Connection method - always DESKTOP_APP_DIRECT for this application
        headers.put(FraudPreventionHeaders.Headers.CONNECTION_METHOD,
            FraudPreventionHeaders.CONNECTION_METHOD_DESKTOP_APP_DIRECT);

        // Device ID
        addHeader(headers, deviceIdCollector);

        // User IDs
        addHeader(headers, userIdsCollector);

        // Timezone
        addHeader(headers, timezoneCollector);

        // Local IPs
        addHeader(headers, localIpsCollector);

        // Vendor version
        headers.put(FraudPreventionHeaders.Headers.VENDOR_VERSION,
            encodeHeaderValue(APP_NAME + "=" + appVersion));

        // Vendor product name
        headers.put(FraudPreventionHeaders.Headers.VENDOR_PRODUCT_NAME,
            encodeHeaderValue(APP_NAME));

        // Screens info (simplified for desktop)
        headers.put(FraudPreventionHeaders.Headers.SCREENS, getScreensInfo());

        // Window size (not applicable for headless, but include anyway)
        headers.put(FraudPreventionHeaders.Headers.WINDOW_SIZE,
            getWindowSize());

        log.debug("Generated {} fraud prevention headers", headers.size());

        return headers;
    }

    /**
     * Validates that all mandatory headers are present.
     *
     * @return true if all mandatory headers are present
     */
    public boolean validateHeaders(Map<String, String> headers) {
        String[] mandatoryHeaders = {
            FraudPreventionHeaders.Headers.CONNECTION_METHOD,
            FraudPreventionHeaders.Headers.DEVICE_ID,
            FraudPreventionHeaders.Headers.USER_IDS,
            FraudPreventionHeaders.Headers.TIMEZONE
        };

        for (String header : mandatoryHeaders) {
            if (!headers.containsKey(header) || headers.get(header) == null
                    || headers.get(header).isBlank()) {
                log.warn("Missing mandatory fraud prevention header: {}", header);
                return false;
            }
        }

        return true;
    }

    private void addHeader(Map<String, String> headers, HeaderCollector collector) {
        try {
            String value = collector.collect();
            if (value != null && !value.isBlank()) {
                headers.put(collector.getHeaderName(), value);
            } else if (collector.isMandatory()) {
                log.warn("Mandatory header {} returned empty value", collector.getHeaderName());
            }
        } catch (Exception e) {
            log.error("Failed to collect header {}", collector.getHeaderName(), e);
            if (collector.isMandatory()) {
                throw new RuntimeException("Failed to collect mandatory header: "
                    + collector.getHeaderName(), e);
            }
        }
    }

    private String getScreensInfo() {
        try {
            java.awt.GraphicsEnvironment ge =
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
            java.awt.GraphicsDevice[] screens = ge.getScreenDevices();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < screens.length; i++) {
                if (i > 0) sb.append(",");

                java.awt.Rectangle bounds = screens[i].getDefaultConfiguration().getBounds();
                int width = (int) bounds.getWidth();
                int height = (int) bounds.getHeight();

                sb.append("width=").append(width);
                sb.append("&height=").append(height);
                sb.append("&scaling-factor=1");
                sb.append("&colour-depth=24");
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("Failed to get screen info", e);
            return "width=1920&height=1080&scaling-factor=1&colour-depth=24";
        }
    }

    private String getWindowSize() {
        // For desktop app, use a reasonable default
        return "width=1200&height=800";
    }

    private String encodeHeaderValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
