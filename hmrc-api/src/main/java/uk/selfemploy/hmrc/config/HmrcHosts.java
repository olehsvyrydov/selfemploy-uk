package uk.selfemploy.hmrc.config;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The single allowlist of destinations the app may send HMRC credentials or tokens to.
 *
 * <p>Base URLs are read from {@code HMRC_*} system properties, which {@code EnvLoader} can populate
 * from a {@code .env} file. Enforcing this allowlist immediately before any request that carries a
 * secret means a redirected URL cannot exfiltrate it to another host. Only HTTPS on an official
 * HMRC domain is permitted.
 *
 * <p>Loopback is allowed only when {@link #ALLOW_LOOPBACK_PROPERTY} is set — a test-only opt-in for
 * WireMock, which {@code EnvLoader} must refuse to set from a {@code .env} file. It never widens the
 * allowlist to any other host.
 */
public final class HmrcHosts {

    /** Test-only system property enabling loopback destinations. Never settable from {@code .env}. */
    public static final String ALLOW_LOOPBACK_PROPERTY = "selfemploy.hmrc.allow-loopback";

    /**
     * HMRC's official domain suffixes. Defined in code on purpose and not read from any external
     * configuration: this list is the security boundary that keeps the client secret from being sent
     * elsewhere, so a runtime source an attacker could reach (a {@code .env} file, a system property)
     * must never be able to widen it. HMRC's domains are stable; a genuine change is a code change,
     * reviewed like any other.
     */
    private static final List<String> ALLOWED_HOST_SUFFIXES =
        List.of(".service.hmrc.gov.uk", ".tax.service.gov.uk");

    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

    private HmrcHosts() {
    }

    /**
     * Whether the app may send a request carrying HMRC credentials or tokens to this URL.
     *
     * @param uri the request target
     * @return true only for HTTPS on an official HMRC host, or loopback under the test opt-in
     */
    public static boolean isAllowed(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return false;
        }
        String scheme = uri.getScheme();
        String host = uri.getHost().toLowerCase(Locale.ROOT);

        if ("https".equalsIgnoreCase(scheme)
            && ALLOWED_HOST_SUFFIXES.stream().anyMatch(host::endsWith)) {
            return true;
        }
        return loopbackAllowed() && LOOPBACK_HOSTS.contains(host);
    }

    private static boolean loopbackAllowed() {
        return Boolean.getBoolean(ALLOW_LOOPBACK_PROPERTY);
    }
}
