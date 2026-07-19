package uk.selfemploy.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides application version and build metadata.
 * Values are read from version.properties which is populated by Maven resource filtering.
 */
public final class VersionInfo {

    private static final Logger LOG = Logger.getLogger(VersionInfo.class.getName());
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = VersionInfo.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                PROPS.load(is);
            } else {
                LOG.warning("version.properties not found on classpath");
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load version.properties", e);
        }
    }

    private VersionInfo() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the application version (e.g., "0.1.0-SNAPSHOT" or "1.0.0-beta.1").
     */
    public static String getVersion() {
        return PROPS.getProperty("app.version", "unknown");
    }

    /**
     * Returns the application name.
     */
    public static String getAppName() {
        return PROPS.getProperty("app.name", "UK Self-Employment Manager");
    }

    /**
     * Returns the build timestamp (yyyy-MM-dd format).
     */
    public static String getBuildTimestamp() {
        return PROPS.getProperty("app.build.timestamp", "unknown");
    }

    /**
     * Returns the license identifier.
     */
    public static String getLicense() {
        return "Apache License 2.0";
    }

    /**
     * Returns the GitHub repository URL.
     */
    public static String getGitHubUrl() {
        return PROPS.getProperty("github.url", "https://github.com/olehsvyrydov/selfemploy-uk");
    }

    /**
     * Returns the GitHub API URL for the latest release, used by the update check.
     */
    public static String getReleasesApiUrl() {
        return PROPS.getProperty("github.releases.api.url",
                "https://api.github.com/repos/olehsvyrydov/selfemploy-uk/releases/latest");
    }
}
