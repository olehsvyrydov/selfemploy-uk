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
     * Adds every fraud-prevention header to the builder. If header collection
     * fails for any reason it is logged and skipped rather than blocking the
     * request, so submission never fails purely on header assembly.
     */
    static void apply(HttpRequest.Builder builder) {
        try {
            Map<String, String> headers = service().generateHeaders();
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (header.getKey() != null && header.getValue() != null) {
                    builder.header(header.getKey(), header.getValue());
                }
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Failed to collect fraud-prevention headers", e);
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
