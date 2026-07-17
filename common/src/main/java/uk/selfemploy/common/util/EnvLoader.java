package uk.selfemploy.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to load environment variables from .env files.
 *
 * <p>Only an allowlisted set of keys is applied as System properties. A {@code .env} file lives in
 * the working directory — a low bar for an attacker to plant — and setting arbitrary system
 * properties from it is dangerous: keys like {@code javax.net.ssl.trustStore} or {@code https.proxyHost}
 * would let a planted file intercept the TLS connection that carries the HMRC client secret, and the
 * {@code HMRC_*_URL} keys would let it redirect that secret outright. Environment URLs are chosen by
 * the app (Settings ⇄ hardcoded HMRC constants), not by {@code .env}, so only the credential and
 * connection keys the app reads from a {@code .env} are honoured; everything else is logged and
 * ignored. Command-line {@code -D} properties are unaffected.
 */
public final class EnvLoader {

    private static final Logger LOG = Logger.getLogger(EnvLoader.class.getName());

    /** The only keys a .env file may set as system properties. */
    private static final Set<String> ALLOWED_KEYS = Set.of(
        "HMRC_CLIENT_ID",
        "HMRC_CLIENT_SECRET",
        "HMRC_SCOPES",
        "HMRC_CALLBACK_PORT");

    private EnvLoader() {
        // Utility class
    }

    /**
     * Loads environment variables from a .env file.
     * Variables are set as System properties with their environment variable names.
     *
     * @param envFilePath Path to the .env file
     * @return Map of loaded variables
     */
    public static Map<String, String> load(Path envFilePath) {
        Map<String, String> env = new HashMap<>();

        if (!Files.exists(envFilePath)) {
            LOG.warning("No .env file found at: " + envFilePath);
            return env;
        }

        try {
            Files.readAllLines(envFilePath).forEach(line -> {
                // Skip comments and empty lines
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return;
                }

                // Parse KEY=VALUE
                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex > 0) {
                    String key = trimmed.substring(0, equalsIndex).trim();
                    String value = trimmed.substring(equalsIndex + 1).trim();

                    // Remove surrounding quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    if (!ALLOWED_KEYS.contains(key)) {
                        LOG.warning("Ignoring .env key '" + key + "': not one this app reads from a "
                            + ".env file. Set HMRC URLs via the Settings environment selector.");
                        return;
                    }

                    env.put(key, value);
                    System.setProperty(key, value);
                    LOG.fine("Loaded env: " + key + "=***");
                }
            });

            LOG.info("Loaded " + env.size() + " environment variables from " + envFilePath);

        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load .env file: " + envFilePath, e);
        }

        return env;
    }

    /**
     * Loads .env file from the current working directory.
     *
     * @return Map of loaded variables
     */
    public static Map<String, String> loadFromCurrentDir() {
        return load(Path.of(".env"));
    }

    /**
     * Loads .env file from multiple possible locations.
     * Tries: current directory, user home, project root.
     *
     * @return Map of loaded variables
     */
    public static Map<String, String> loadFromDefaultLocations() {
        // Try current directory first
        Path envPath = Path.of(".env");
        if (Files.exists(envPath)) {
            return load(envPath);
        }

        // Try user home
        envPath = Path.of(System.getProperty("user.home"), ".selfemploy", ".env");
        if (Files.exists(envPath)) {
            return load(envPath);
        }

        // Try project root (parent of current if in subdirectory)
        envPath = Path.of("../.env");
        if (Files.exists(envPath)) {
            return load(envPath);
        }

        LOG.warning("No .env file found in default locations");
        return new HashMap<>();
    }
}
