package uk.selfemploy.plugin.runtime;

import uk.selfemploy.plugin.api.PluginContext;
import uk.selfemploy.plugin.api.PluginPermission;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of {@link PluginContext} that provides runtime context to plugins.
 *
 * <p>This implementation provides:</p>
 * <ul>
 *   <li>Application version information</li>
 *   <li>Isolated data directory for each plugin</li>
 * </ul>
 *
 * <h2>Data Directory</h2>
 * <p>Each plugin receives a unique data directory under the application's
 * plugin-data folder. The directory is created lazily when first accessed.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and therefore thread-safe.</p>
 *
 * @see PluginContext
 * @see PluginManager
 */
public class PluginContextImpl implements PluginContext {

    private final String appVersion;
    private final Path pluginDataDirectory;
    private final Set<PluginPermission> grantedPermissions;

    /**
     * Creates a new PluginContextImpl.
     *
     * @param appVersion          the application version string
     * @param pluginDataDirectory the plugin's isolated data directory
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if appVersion is blank
     */
    public PluginContextImpl(String appVersion, Path pluginDataDirectory) {
        this(appVersion, pluginDataDirectory, Collections.emptySet());
    }

    /**
     * Creates a new PluginContextImpl with granted permissions.
     *
     * @param appVersion          the application version string
     * @param pluginDataDirectory the plugin's isolated data directory
     * @param grantedPermissions  the permissions granted to this plugin
     * @throws NullPointerException     if appVersion or pluginDataDirectory is null
     * @throws IllegalArgumentException if appVersion is blank
     */
    public PluginContextImpl(String appVersion, Path pluginDataDirectory,
                             Set<PluginPermission> grantedPermissions) {
        if (appVersion == null || appVersion.isBlank()) {
            throw new IllegalArgumentException("appVersion must not be null or blank");
        }
        this.appVersion = appVersion;
        this.pluginDataDirectory = Objects.requireNonNull(
            pluginDataDirectory,
            "pluginDataDirectory must not be null"
        );
        this.grantedPermissions = grantedPermissions != null && !grantedPermissions.isEmpty() ?
            Collections.unmodifiableSet(EnumSet.copyOf(grantedPermissions)) :
            Collections.emptySet();
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    @Override
    public Path getPluginDataDirectory() {
        ensureDirectoryExists();
        return pluginDataDirectory;
    }

    @Override
    public Set<PluginPermission> getGrantedPermissions() {
        return grantedPermissions;
    }

    /**
     * Ensures the plugin data directory exists, creating it if necessary.
     *
     * @throws UncheckedIOException if the directory cannot be created
     */
    private void ensureDirectoryExists() {
        if (!Files.exists(pluginDataDirectory)) {
            try {
                Files.createDirectories(pluginDataDirectory);
            } catch (IOException e) {
                throw new UncheckedIOException(
                    "Failed to create plugin data directory: " + pluginDataDirectory,
                    e
                );
            }
        }
    }

    @Override
    public String toString() {
        return String.format(
            "PluginContextImpl[appVersion=%s, dataDir=%s, permissions=%s]",
            appVersion,
            pluginDataDirectory,
            grantedPermissions
        );
    }

    /**
     * Builder for creating PluginContextImpl instances.
     *
     * <p>This builder simplifies the creation of context instances
     * and provides sensible defaults.</p>
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * PluginContext context = PluginContextImpl.builder()
     *     .appVersion("0.1.0")
     *     .baseDataDirectory(Paths.get("~/.selfemploy/plugin-data"))
     *     .pluginId("my-plugin")
     *     .grantedPermissions(Set.of(PluginPermission.DATA_READ))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private String appVersion;
        private Path baseDataDirectory;
        private String pluginId;
        private Set<PluginPermission> grantedPermissions = Collections.emptySet();

        /**
         * Sets the application version.
         *
         * @param appVersion the application version string
         * @return this builder
         */
        public Builder appVersion(String appVersion) {
            this.appVersion = appVersion;
            return this;
        }

        /**
         * Sets the base directory for all plugin data.
         *
         * <p>The plugin's specific directory will be created as a
         * subdirectory using the plugin ID.</p>
         *
         * @param baseDataDirectory the base plugin data directory
         * @return this builder
         */
        public Builder baseDataDirectory(Path baseDataDirectory) {
            this.baseDataDirectory = baseDataDirectory;
            return this;
        }

        /**
         * Sets the plugin ID.
         *
         * <p>This is used to create the plugin-specific subdirectory.</p>
         *
         * @param pluginId the plugin ID
         * @return this builder
         */
        public Builder pluginId(String pluginId) {
            this.pluginId = pluginId;
            return this;
        }

        /**
         * Sets the permissions granted to this plugin.
         *
         * @param permissions the granted permissions
         * @return this builder
         */
        public Builder grantedPermissions(Set<PluginPermission> permissions) {
            this.grantedPermissions = permissions != null ? permissions : Collections.emptySet();
            return this;
        }

        /**
         * Builds the PluginContextImpl.
         *
         * @return the new PluginContextImpl instance
         * @throws NullPointerException     if required fields are not set
         * @throws IllegalArgumentException if appVersion or pluginId is blank
         */
        public PluginContextImpl build() {
            Objects.requireNonNull(baseDataDirectory, "baseDataDirectory must be set");
            if (pluginId == null || pluginId.isBlank()) {
                throw new IllegalArgumentException("pluginId must not be null or blank");
            }

            // Create plugin-specific directory path
            // Sanitize plugin ID for filesystem safety
            String safeDirName = sanitizeForFilesystem(pluginId);
            Path pluginDataDir = baseDataDirectory.resolve(safeDirName);

            return new PluginContextImpl(appVersion, pluginDataDir, grantedPermissions);
        }

        /**
         * Sanitizes a string for use as a directory name.
         *
         * @param name the name to sanitize
         * @return a filesystem-safe name
         */
        private String sanitizeForFilesystem(String name) {
            // Replace characters that are invalid in filenames
            return name.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
    }

    /**
     * Creates a new builder for PluginContextImpl.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
