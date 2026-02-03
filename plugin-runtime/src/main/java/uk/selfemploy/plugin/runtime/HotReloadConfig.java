package uk.selfemploy.plugin.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration for the plugin hot-reload system.
 *
 * <p>This class encapsulates all configuration options for the
 * {@link PluginHotReloader}, including the watch directory and
 * debounce settings.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * HotReloadConfig config = HotReloadConfig.builder()
 *     .watchDirectory(Paths.get("/plugins"))
 *     .debounceMillis(1000)  // 1 second debounce
 *     .build();
 * }</pre>
 *
 * @see PluginHotReloader
 */
public final class HotReloadConfig {

    /**
     * Default debounce delay in milliseconds (500ms).
     *
     * <p>This delay prevents multiple reloads when a file is being
     * written in chunks during compilation.</p>
     */
    public static final long DEFAULT_DEBOUNCE_MILLIS = 500;

    /**
     * Minimum allowed debounce delay.
     */
    public static final long MIN_DEBOUNCE_MILLIS = 1;

    private final Path watchDirectory;
    private final long debounceMillis;

    /**
     * Private constructor - use {@link #builder()} instead.
     */
    private HotReloadConfig(Builder builder) {
        this.watchDirectory = Objects.requireNonNull(
            builder.watchDirectory,
            "watchDirectory must not be null"
        );

        // Validate watch directory exists and is a directory
        if (!Files.exists(this.watchDirectory)) {
            throw new IllegalArgumentException(
                "Watch directory does not exist: " + this.watchDirectory
            );
        }
        if (!Files.isDirectory(this.watchDirectory)) {
            throw new IllegalArgumentException(
                "Watch directory is not a directory: " + this.watchDirectory
            );
        }

        // Validate debounce
        if (builder.debounceMillis < MIN_DEBOUNCE_MILLIS) {
            throw new IllegalArgumentException(
                "debounce must be at least " + MIN_DEBOUNCE_MILLIS + "ms, was: " + builder.debounceMillis
            );
        }
        this.debounceMillis = builder.debounceMillis;
    }

    /**
     * Returns the directory to watch for plugin JAR changes.
     *
     * @return the watch directory, never null
     */
    public Path getWatchDirectory() {
        return watchDirectory;
    }

    /**
     * Returns the debounce delay in milliseconds.
     *
     * <p>When a file change is detected, the reloader waits this long before
     * triggering a reload. This prevents multiple reloads when a file is
     * being written in chunks (e.g., during compilation).</p>
     *
     * @return the debounce delay in milliseconds
     */
    public long getDebounceMillis() {
        return debounceMillis;
    }

    /**
     * Creates a new builder for HotReloadConfig.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format(
            "HotReloadConfig[watchDirectory=%s, debounceMillis=%d]",
            watchDirectory, debounceMillis
        );
    }

    /**
     * Builder for creating HotReloadConfig instances.
     */
    public static final class Builder {
        private Path watchDirectory;
        private long debounceMillis = DEFAULT_DEBOUNCE_MILLIS;

        /**
         * Private constructor - use {@link HotReloadConfig#builder()}.
         */
        private Builder() {
        }

        /**
         * Sets the directory to watch for plugin JAR changes.
         *
         * <p>The directory must exist and be a valid directory.</p>
         *
         * @param watchDirectory the directory to watch
         * @return this builder
         */
        public Builder watchDirectory(Path watchDirectory) {
            this.watchDirectory = watchDirectory;
            return this;
        }

        /**
         * Sets the debounce delay in milliseconds.
         *
         * <p>When multiple file change events occur rapidly (e.g., during
         * compilation), only one reload will be triggered after the debounce
         * delay has passed since the last change.</p>
         *
         * @param debounceMillis the debounce delay (must be positive, default 500ms)
         * @return this builder
         */
        public Builder debounceMillis(long debounceMillis) {
            this.debounceMillis = debounceMillis;
            return this;
        }

        /**
         * Builds the HotReloadConfig.
         *
         * @return the config instance
         * @throws NullPointerException     if watchDirectory is null
         * @throws IllegalArgumentException if watchDirectory doesn't exist,
         *                                  is not a directory, or debounce is invalid
         */
        public HotReloadConfig build() {
            return new HotReloadConfig(this);
        }
    }
}
