package uk.selfemploy.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to load environment variables from .env files.
 * Sets them as System properties so they're available to SmallRye Config.
 */
public final class EnvLoader {

    private static final Logger LOG = Logger.getLogger(EnvLoader.class.getName());

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

                    env.put(key, value);

                    // Set as System property (for SmallRye Config)
                    // SmallRye maps HMRC_CLIENT_ID -> hmrc.client-id
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
