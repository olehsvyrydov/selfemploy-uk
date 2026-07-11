package uk.selfemploy.ui.service;

import uk.selfemploy.hmrc.fraud.FraudPreventionService;
import uk.selfemploy.hmrc.fraud.collectors.DeviceIdCollector;
import uk.selfemploy.hmrc.fraud.collectors.LocalIpsCollector;
import uk.selfemploy.hmrc.fraud.collectors.LocalIpsTimestampCollector;
import uk.selfemploy.hmrc.fraud.collectors.MacAddressesCollector;
import uk.selfemploy.hmrc.fraud.collectors.TimezoneCollector;
import uk.selfemploy.hmrc.fraud.collectors.UserAgentCollector;
import uk.selfemploy.hmrc.fraud.collectors.UserIdsCollector;

import java.net.http.HttpRequest;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies HMRC's full DESKTOP_APP_DIRECT fraud-prevention header set to an
 * outgoing request, reusing {@link FraudPreventionService} from hmrc-api so the
 * runtime clients send the same complete, spec-correct headers (device id, MAC
 * addresses, local IPs and their timestamp, DST-aware timezone, user agent, …)
 * rather than a hand-rolled subset.
 */
final class HmrcFraudHeaders {

    private static final Logger LOG = Logger.getLogger(HmrcFraudHeaders.class.getName());
    private static final String APP_VERSION = "1.0.0";

    private static volatile FraudPreventionService service;

    private HmrcFraudHeaders() {
    }

    /**
     * Adds every fraud-prevention header to the builder. HMRC rejects any MTD request
     * that is missing its mandatory {@code Gov-Client-*}/{@code Gov-Vendor-*} headers,
     * so if a mandatory header cannot be assembled this fails fast with a clear message
     * rather than sending a request that HMRC is guaranteed to reject.
     */
    static void apply(HttpRequest.Builder builder) {
        Map<String, String> headers;
        try {
            headers = service().generateHeaders();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Could not assemble mandatory HMRC fraud-prevention headers", e);
            throw new IllegalStateException(
                "Could not assemble the fraud-prevention information HMRC requires for this "
                + "submission. Please try again; if it keeps happening, restart the app.", e);
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey() != null && header.getValue() != null) {
                builder.header(header.getKey(), header.getValue());
            }
        }
    }

    private static FraudPreventionService service() {
        FraudPreventionService local = service;
        if (local == null) {
            synchronized (HmrcFraudHeaders.class) {
                local = service;
                if (local == null) {
                    local = new FraudPreventionService(
                        new DeviceIdCollector(),
                        new TimezoneCollector(),
                        new LocalIpsCollector(),
                        new LocalIpsTimestampCollector(),
                        new MacAddressesCollector(),
                        new UserIdsCollector(),
                        new UserAgentCollector(),
                        APP_VERSION);
                    service = local;
                }
            }
        }
        return local;
    }
}
