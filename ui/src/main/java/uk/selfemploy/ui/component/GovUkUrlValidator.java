package uk.selfemploy.ui.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Validates URLs against a whitelist of allowed domains for the in-app browser.
 *
 * <p>SE-7XX: In-App Browser for HMRC Guidance</p>
 *
 * <p>Security consideration: This validator ensures that the in-app browser
 * only loads content from trusted domains to protect users
 * from malicious redirects or phishing attempts.</p>
 *
 * <p>Allowed domains include:</p>
 * <ul>
 *   <li>gov.uk and all subdomains (www.gov.uk, hmrc.gov.uk, etc.)</li>
 *   <li>github.com and all subdomains (for project documentation)</li>
 * </ul>
 *
 * <p>Architecture Reference: docs/sprints/sprint-7/approvals/jorge-architecture-inapp-browser.md</p>
 */
public class GovUkUrlValidator {

    private static final Logger LOG = LoggerFactory.getLogger(GovUkUrlValidator.class);

    /**
     * Set of allowed base domains.
     */
    private static final Set<String> ALLOWED_BASE_DOMAINS = Set.of(
            "gov.uk",
            "github.com"
    );

    /**
     * Set of allowed domain suffixes for display purposes.
     */
    private static final Set<String> ALLOWED_DOMAIN_PATTERNS = Set.of(
            "gov.uk",
            "www.gov.uk",
            "github.com",
            "www.github.com"
    );

    /**
     * Only HTTP and HTTPS protocols are allowed.
     */
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    /**
     * Returns the set of allowed domain patterns.
     *
     * @return unmodifiable set of allowed domain patterns
     */
    public static Set<String> getAllowedDomainPatterns() {
        return ALLOWED_DOMAIN_PATTERNS;
    }

    /**
     * Validates whether the given URL is allowed in the in-app browser.
     *
     * <p>A URL is allowed if:</p>
     * <ul>
     *   <li>It uses HTTP or HTTPS protocol</li>
     *   <li>The host is exactly a GOV.UK domain or a subdomain of gov.uk</li>
     *   <li>The URL does not contain authentication information (user:pass@)</li>
     * </ul>
     *
     * @param url the URL to validate (may be null)
     * @return true if the URL is allowed, false otherwise
     */
    public boolean isAllowedUrl(String url) {
        if (url == null || url.isBlank()) {
            LOG.debug("Rejecting null or blank URL");
            return false;
        }

        try {
            URI uri = new URI(url.trim());

            // Check scheme (protocol)
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                LOG.debug("Rejecting URL with disallowed scheme: {}", scheme);
                return false;
            }

            // Reject URLs with authentication info (user:pass@)
            if (uri.getUserInfo() != null) {
                LOG.debug("Rejecting URL with authentication info");
                return false;
            }

            // Check host
            String host = uri.getHost();
            if (host == null) {
                LOG.debug("Rejecting URL with null host");
                return false;
            }

            // Normalize to lowercase for comparison
            host = host.toLowerCase();

            // Check if host matches allowed patterns
            return isAllowedHost(host);

        } catch (URISyntaxException e) {
            LOG.debug("Rejecting malformed URL: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the host matches an allowed domain pattern.
     *
     * <p>The host must be exactly an allowed base domain or a subdomain of it.</p>
     *
     * @param host the lowercase hostname to check
     * @return true if the host is allowed
     */
    private boolean isAllowedHost(String host) {
        // Check each allowed base domain
        for (String baseDomain : ALLOWED_BASE_DOMAINS) {
            // Exact match for the base domain
            if (host.equals(baseDomain)) {
                return true;
            }

            // Check if it's a subdomain (e.g., www.gov.uk, hmrc.gov.uk, www.github.com)
            String suffix = "." + baseDomain;
            if (host.endsWith(suffix)) {
                // Additional check to prevent spoofing like "evil.com.gov.uk"
                // by ensuring it's a valid subdomain structure
                String prefix = host.substring(0, host.length() - suffix.length());

                // Prefix should not be empty and should not contain suspicious patterns
                if (!prefix.isEmpty() && isValidSubdomainPrefix(prefix)) {
                    return true;
                }
            }
        }

        LOG.debug("Rejecting host not matching allowed patterns: {}", host);
        return false;
    }

    /**
     * Validates that a subdomain prefix is legitimate and not suspicious.
     *
     * @param prefix the subdomain prefix (e.g., "www" for www.gov.uk)
     * @return true if the prefix appears to be a valid subdomain
     */
    private boolean isValidSubdomainPrefix(String prefix) {
        // Check for suspicious patterns that might indicate spoofing
        // A valid subdomain should only contain alphanumeric characters, hyphens, and dots
        // It should not start or end with a hyphen

        if (prefix.startsWith("-") || prefix.endsWith("-")) {
            return false;
        }

        // Check each character is valid for a subdomain
        for (char c : prefix.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '.') {
                return false;
            }
        }

        // Prevent double dots which could indicate suspicious structure
        if (prefix.contains("..")) {
            return false;
        }

        return true;
    }
}
