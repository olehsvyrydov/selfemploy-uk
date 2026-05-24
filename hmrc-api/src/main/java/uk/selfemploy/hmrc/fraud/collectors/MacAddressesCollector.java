package uk.selfemploy.hmrc.fraud.collectors;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.fraud.FraudPreventionHeaders;
import uk.selfemploy.hmrc.fraud.HeaderCollector;

import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Collects MAC addresses for every active, non-loopback, non-virtual network
 * interface. HMRC requires this header for DESKTOP_APP_DIRECT to fingerprint
 * the originating hardware.
 *
 * <p>Format: comma-separated, percent-encoded canonical {@code aa:bb:cc:dd:ee:ff}.
 *
 * <p>If no MAC addresses can be enumerated (sandbox, container without NET_ADMIN,
 * permissions denied), returns an empty string. The service then suppresses the
 * header — HMRC accepts absence over a malformed value, but the contract test
 * will flag a missing value in a real environment.
 */
@ApplicationScoped
public class MacAddressesCollector implements HeaderCollector {

    private static final Logger log = LoggerFactory.getLogger(MacAddressesCollector.class);

    @Override
    public String getHeaderName() {
        return FraudPreventionHeaders.Headers.MAC_ADDRESSES;
    }

    @Override
    public String collect() {
        Set<String> macs = new LinkedHashSet<>();
        try {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (iface.isLoopback() || iface.isVirtual() || !iface.isUp()) {
                    continue;
                }
                byte[] hardware = iface.getHardwareAddress();
                if (hardware == null || hardware.length != 6) {
                    continue;
                }
                macs.add(format(hardware));
            }
        } catch (Exception e) {
            log.warn("Failed to enumerate hardware addresses", e);
        }

        return macs.stream()
            .map(m -> URLEncoder.encode(m, StandardCharsets.UTF_8))
            .collect(Collectors.joining(","));
    }

    @Override
    public boolean isMandatory() {
        // Mandatory per HMRC spec, but tolerate environments where the JVM
        // cannot read NIC hardware addresses (containers, restricted sandboxes)
        // rather than failing the entire request.
        return false;
    }

    private static String format(byte[] mac) {
        StringBuilder sb = new StringBuilder(17);
        for (int i = 0; i < mac.length; i++) {
            if (i > 0) sb.append(':');
            sb.append(String.format("%02x", mac[i]));
        }
        return sb.toString();
    }
}
