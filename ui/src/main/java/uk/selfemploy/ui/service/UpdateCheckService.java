package uk.selfemploy.ui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.selfemploy.common.util.VersionInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks whether a newer application release is available on GitHub and, if so, describes how to
 * install it for the detected {@link InstallType}.
 *
 * <p>The check is opt-out (a user preference), runs off the UI thread, and fails silently on any
 * network or parse error so an offline user is never nagged and startup is never blocked. The HTTP
 * layer is injected via {@link ReleaseFetcher} so tests exercise the logic without a network call.</p>
 */
public class UpdateCheckService {

    private static final Logger LOG = Logger.getLogger(UpdateCheckService.class.getName());

    /** The GitHub API endpoint returning the latest published release for the project repository. */
    static final String LATEST_RELEASE_URL =
            "https://api.github.com/repos/olehsvyrydov/selfemploy-uk/releases/latest";

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(15);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Shared so repeated checks reuse one connection pool. */
    private static final HttpClient SHARED_CLIENT =
            HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();

    /** Off the UI thread; daemon so a pending check never keeps the app alive. */
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "update-check");
        t.setDaemon(true);
        return t;
    });

    /** Supplies the raw JSON body of the latest-release endpoint, or empty on any failure. */
    @FunctionalInterface
    interface ReleaseFetcher {
        /** Fetches the latest-release JSON, returning empty rather than throwing on failure. */
        Optional<String> fetchLatestReleaseJson();
    }

    /**
     * The outcome of an update check.
     *
     * @param currentVersion  the running application version (leading {@code v} stripped)
     * @param latestVersion   the latest published release version (leading {@code v} stripped)
     * @param updateAvailable whether {@code latestVersion} is newer than {@code currentVersion}
     */
    public record UpdateCheckResult(String currentVersion, String latestVersion, boolean updateAvailable) {
    }

    private final ReleaseFetcher fetcher;
    private final BooleanSupplier enabledSupplier;
    private final Supplier<String> currentVersionSupplier;

    /**
     * Creates a service wired to GitHub, the persisted opt-out preference, and the running version.
     */
    public UpdateCheckService() {
        this(() -> httpFetch(SHARED_CLIENT),
                () -> SqliteDataStore.getInstance().isUpdateCheckEnabled(),
                VersionInfo::getVersion);
    }

    /**
     * Creates a service with an injected HTTP layer, enable flag, and version source, for testing.
     *
     * @param fetcher                supplies the latest-release JSON
     * @param enabledSupplier        whether update checks are enabled
     * @param currentVersionSupplier the running application version
     */
    UpdateCheckService(ReleaseFetcher fetcher, BooleanSupplier enabledSupplier,
                       Supplier<String> currentVersionSupplier) {
        this.fetcher = fetcher;
        this.enabledSupplier = enabledSupplier;
        this.currentVersionSupplier = currentVersionSupplier;
    }

    /**
     * Runs {@link #checkForUpdate()} off the calling thread. When checks are disabled the fetcher is
     * never invoked and the future completes with an empty result.
     *
     * @return a future of the result, empty when disabled or when no update information is available
     */
    public CompletableFuture<Optional<UpdateCheckResult>> checkForUpdateAsync() {
        if (!enabledSupplier.getAsBoolean()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.supplyAsync(this::checkForUpdate, EXECUTOR);
    }

    /**
     * Fetches the latest release and compares it with the running version. Blocking; call off the UI
     * thread. Returns empty when checks are disabled or when no update information could be obtained
     * (network failure, missing/blank tag, or unparseable versions) — this method never throws.
     *
     * @return the result, or empty when no update information is available
     */
    Optional<UpdateCheckResult> checkForUpdate() {
        if (!enabledSupplier.getAsBoolean()) {
            return Optional.empty();
        }
        Optional<String> body = fetcher.fetchLatestReleaseJson();
        if (body.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> tag = parseTagName(body.get());
        if (tag.isEmpty()) {
            return Optional.empty();
        }
        String current = currentVersionSupplier.get();
        String latest = tag.get();
        boolean available = isUpdateAvailable(current, latest);
        return Optional.of(new UpdateCheckResult(stripLeadingV(current), stripLeadingV(latest), available));
    }

    /**
     * The localized update instruction for the detected install type.
     */
    public String updateGuidanceKey() {
        return InstallType.detect().guidanceKey();
    }

    private Optional<String> parseTagName(String json) {
        try {
            JsonNode tag = MAPPER.readTree(json).get("tag_name");
            if (tag == null || tag.isNull()) {
                return Optional.empty();
            }
            String value = tag.asText().trim();
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        } catch (Exception e) {
            LOG.fine("Could not parse release JSON: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> httpFetch(HttpClient client) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LATEST_RELEASE_URL))
                    .timeout(HTTP_TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.body());
        } catch (Exception e) {
            LOG.log(Level.FINE, "Update check request failed", e);
            return Optional.empty();
        }
    }

    // === Semantic version comparison ===

    /**
     * Returns whether {@code latestVersion} is a newer release than {@code currentVersion}. A leading
     * {@code v} is ignored. Malformed or null versions yield {@code false} so a garbled release tag
     * never triggers a false "update available" notice.
     *
     * @param currentVersion the running version
     * @param latestVersion  the candidate newer version
     * @return true only if {@code latestVersion} is unambiguously newer
     */
    public static boolean isUpdateAvailable(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) {
            return false;
        }
        try {
            return compareVersions(latestVersion, currentVersion) > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Compares two semantic versions, ignoring a leading {@code v} and any {@code +build} metadata.
     * A pre-release (e.g. {@code 1.0.0-beta.1}) sorts below its release ({@code 1.0.0}); pre-release
     * identifiers are compared per the SemVer specification (numeric identifiers rank below
     * alphanumeric ones and compare numerically).
     *
     * @return a negative number, zero, or a positive number as {@code left} is less than, equal to,
     *         or greater than {@code right}
     * @throws IllegalArgumentException if either version has an empty or non-numeric release segment
     */
    static int compareVersions(String left, String right) {
        Parsed l = parse(left);
        Parsed r = parse(right);
        int core = compareRelease(l.release(), r.release());
        if (core != 0) {
            return core;
        }
        return comparePreRelease(l.preRelease(), r.preRelease());
    }

    private record Parsed(int[] release, String[] preRelease) {
    }

    private static Parsed parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("version is null");
        }
        String version = stripLeadingV(raw.trim());
        int plus = version.indexOf('+');
        if (plus >= 0) {
            version = version.substring(0, plus);
        }
        int dash = version.indexOf('-');
        String corePart = dash >= 0 ? version.substring(0, dash) : version;
        String prePart = dash >= 0 ? version.substring(dash + 1) : null;

        String[] coreTokens = corePart.split("\\.", -1);
        int[] release = new int[coreTokens.length];
        for (int i = 0; i < coreTokens.length; i++) {
            String token = coreTokens[i].trim();
            int value = Integer.parseInt(token);
            if (value < 0) {
                throw new IllegalArgumentException("negative version segment: " + token);
            }
            release[i] = value;
        }
        String[] preRelease = (prePart == null || prePart.isEmpty()) ? null : prePart.split("\\.", -1);
        return new Parsed(release, preRelease);
    }

    private static int compareRelease(int[] left, int[] right) {
        int length = Math.max(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int a = i < left.length ? left[i] : 0;
            int b = i < right.length ? right[i] : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    private static int comparePreRelease(String[] left, String[] right) {
        if (left == null && right == null) {
            return 0;
        }
        // A release ranks above a pre-release of the same core version.
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        int length = Math.min(left.length, right.length);
        for (int i = 0; i < length; i++) {
            int cmp = comparePreReleaseIdentifier(left[i], right[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(left.length, right.length);
    }

    private static int comparePreReleaseIdentifier(String a, String b) {
        boolean aNumeric = isNumeric(a);
        boolean bNumeric = isNumeric(b);
        if (aNumeric && bNumeric) {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        }
        if (aNumeric) {
            return -1;
        }
        if (bNumeric) {
            return 1;
        }
        return a.compareTo(b);
    }

    private static boolean isNumeric(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String stripLeadingV(String version) {
        if (!version.isEmpty() && (version.charAt(0) == 'v' || version.charAt(0) == 'V')) {
            return version.substring(1);
        }
        return version;
    }
}
