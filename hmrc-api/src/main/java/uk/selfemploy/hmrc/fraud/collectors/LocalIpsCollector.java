package uk.selfemploy.hmrc.fraud.collectors;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.fraud.FraudPreventionHeaders;
import uk.selfemploy.hmrc.fraud.HeaderCollector;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects local IP addresses.
 * Format: Comma-separated list of IP addresses
 */
@ApplicationScoped
public class LocalIpsCollector implements HeaderCollector {

    private static final Logger log = LoggerFactory.getLogger(LocalIpsCollector.class);

    @Override
    public String getHeaderName() {
        return FraudPreventionHeaders.Headers.LOCAL_IPS;
    }

    @Override
    public String collect() {
        List<String> ips = new ArrayList<>();

        try {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) {
                        continue;
                    }

                    String ip = addr.getHostAddress();

                    // Handle IPv6 with scope ID
                    int scopeIndex = ip.indexOf('%');
                    if (scopeIndex > 0) {
                        ip = ip.substring(0, scopeIndex);
                    }

                    ips.add(ip);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get local IP addresses", e);
        }

        // Fallback if no addresses found
        if (ips.isEmpty()) {
            try {
                String localhost = InetAddress.getLocalHost().getHostAddress();
                if (!localhost.equals("127.0.0.1")) {
                    ips.add(localhost);
                }
            } catch (Exception e) {
                log.debug("Failed to get localhost address", e);
            }
        }

        return String.join(",", ips);
    }
}
