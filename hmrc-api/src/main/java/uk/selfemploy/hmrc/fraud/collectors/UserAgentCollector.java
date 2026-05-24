package uk.selfemploy.hmrc.fraud.collectors;

import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.hmrc.fraud.FraudPreventionHeaders;
import uk.selfemploy.hmrc.fraud.HeaderCollector;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Collects the device OS / hardware descriptor required by HMRC under
 * {@code Gov-Client-User-Agent}. Distinct from a browser User-Agent — HMRC
 * expects an ampersand-delimited key/value pair set:
 *
 * <pre>os-family=Linux&amp;os-version=6.5.0-15-generic&amp;device-manufacturer=unknown&amp;device-model=unknown</pre>
 *
 * <p>For a self-installed desktop app we cannot reliably discover the OEM
 * manufacturer or model without platform-specific calls, so {@code unknown}
 * is sent — HMRC accepts the literal value.
 */
@ApplicationScoped
public class UserAgentCollector implements HeaderCollector {

    @Override
    public String getHeaderName() {
        return FraudPreventionHeaders.Headers.USER_AGENT;
    }

    @Override
    public String collect() {
        String osName = sanitise(System.getProperty("os.name", "unknown"));
        String osVersion = sanitise(System.getProperty("os.version", "unknown"));
        String family = osFamily(osName);

        // device-model is the hardware model per HMRC's spec (e.g. "iPhone12,1",
        // "MacBookPro18,4"). The JVM cannot reliably read hardware model on commodity
        // desktops; CPU architecture (os.arch) is a category mismatch and triggers
        // HMRC's live validator. Emit "unknown" — HMRC explicitly accepts this for
        // platforms where the value cannot be determined.
        return "os-family=" + encode(family)
            + "&os-version=" + encode(osVersion)
            + "&device-manufacturer=unknown"
            + "&device-model=unknown";
    }

    private static String osFamily(String osName) {
        String lower = osName.toLowerCase();
        if (lower.contains("win")) return "Windows";
        if (lower.contains("mac") || lower.contains("darwin")) return "macOS";
        if (lower.contains("nix") || lower.contains("nux") || lower.contains("aix")) return "Linux";
        return osName;
    }

    private static String sanitise(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
