package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PluginHotReloader}.
 *
 * <p>Tests verify hot-reload functionality including:</p>
 * <ul>
 *   <li>NIO WatchService-based file monitoring</li>
 *   <li>Configurable debounce (default 500ms)</li>
 *   <li>Disabled by default; enabled via system property</li>
 *   <li>Optional state preservation across reloads</li>
 *   <li>Correct reload sequence: disable → unload → load → enable</li>
 *   <li>UI notifications via HotReloadListener</li>
 * </ul>
 */
@DisplayName("PluginHotReloader")
class PluginHotReloaderTest {

    @TempDir
    Path tempDir;

    private PluginHotReloader reloader;
    private MockPluginOperations mockOperations;
    private MockHotReloadListener mockListener;

    @BeforeEach
    void setUp() {
        mockOperations = new MockPluginOperations();
        mockListener = new MockHotReloadListener();
    }

    @AfterEach
    void tearDown() {
        if (reloader != null) {
            reloader.stop();
        }
    }

    @Nested
    @DisplayName("Disabled by default behavior")
    class DisabledByDefaultTests {

        @Test
        @DisplayName("should be disabled when system property not set")
        void shouldBeDisabledWhenPropertyNotSet() {
            // Given: No system property set
            System.clearProperty("plugin.hotreload");

            // When/Then
            assertThat(PluginHotReloader.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should be enabled when system property set to true")
        void shouldBeEnabledWhenPropertySetTrue() {
            // Given
            System.setProperty("plugin.hotreload", "true");
            try {
                // When/Then
                assertThat(PluginHotReloader.isEnabled()).isTrue();
            } finally {
                System.clearProperty("plugin.hotreload");
            }
        }

        @Test
        @DisplayName("should be disabled when system property set to false")
        void shouldBeDisabledWhenPropertySetFalse() {
            // Given
            System.setProperty("plugin.hotreload", "false");
            try {
                // When/Then
                assertThat(PluginHotReloader.isEnabled()).isFalse();
            } finally {
                System.clearProperty("plugin.hotreload");
            }
        }

        @Test
        @DisplayName("should be disabled when system property has invalid value")
        void shouldBeDisabledWhenPropertyInvalid() {
            // Given
            System.setProperty("plugin.hotreload", "invalid");
            try {
                // When/Then
                assertThat(PluginHotReloader.isEnabled()).isFalse();
            } finally {
                System.clearProperty("plugin.hotreload");
            }
        }
    }

    @Nested
    @DisplayName("Configurable debounce")
    class DebounceConfigTests {

        @Test
        @DisplayName("should use default debounce of 500ms")
        void shouldUseDefaultDebounce() {
            // Given
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();

            // Then
            assertThat(config.getDebounceMillis()).isEqualTo(500);
        }

        @Test
        @DisplayName("should allow custom debounce value")
        void shouldAllowCustomDebounce() {
            // Given
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .debounceMillis(1000)
                .build();

            // Then
            assertThat(config.getDebounceMillis()).isEqualTo(1000);
        }

        @Test
        @DisplayName("should reject negative debounce value")
        void shouldRejectNegativeDebounce() {
            assertThatThrownBy(() -> HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .debounceMillis(-1)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debounce");
        }

        @Test
        @DisplayName("should reject zero debounce value")
        void shouldRejectZeroDebounce() {
            assertThatThrownBy(() -> HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .debounceMillis(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debounce");
        }
    }

    @Nested
    @DisplayName("Configuration validation")
    class ConfigValidationTests {

        @Test
        @DisplayName("should require watch directory")
        void shouldRequireWatchDirectory() {
            assertThatThrownBy(() -> HotReloadConfig.builder().build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("watchDirectory");
        }

        @Test
        @DisplayName("should require existing watch directory")
        void shouldRequireExistingWatchDirectory() {
            Path nonExistent = tempDir.resolve("nonexistent");

            assertThatThrownBy(() -> HotReloadConfig.builder()
                .watchDirectory(nonExistent)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");
        }

        @Test
        @DisplayName("should require directory (not file)")
        void shouldRequireDirectory() throws IOException {
            Path file = tempDir.resolve("file.txt");
            Files.createFile(file);

            assertThatThrownBy(() -> HotReloadConfig.builder()
                .watchDirectory(file)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a directory");
        }
    }

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidationTests {

        @Test
        @DisplayName("should reject null config")
        void shouldRejectNullConfig() {
            assertThatThrownBy(() -> new PluginHotReloader(null, mockOperations, mockListener))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("config");
        }

        @Test
        @DisplayName("should reject null plugin manager")
        void shouldRejectNullPluginManager() {
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();

            assertThatThrownBy(() -> new PluginHotReloader(config, (PluginManager) null, mockListener))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("pluginManager");
        }

        @Test
        @DisplayName("should allow null listener")
        void shouldAllowNullListener() {
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();

            // Should not throw
            reloader = new PluginHotReloader(config, mockOperations, null);
            assertThat(reloader).isNotNull();
        }
    }

    @Nested
    @DisplayName("Start/Stop lifecycle")
    class LifecycleTests {

        @Test
        @DisplayName("should start and stop cleanly")
        void shouldStartAndStopCleanly() {
            // Given
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);

            // When
            reloader.start();

            // Then
            assertThat(reloader.isRunning()).isTrue();

            // When
            reloader.stop();

            // Then
            assertThat(reloader.isRunning()).isFalse();
        }

        @Test
        @DisplayName("should be idempotent for multiple starts")
        void shouldBeIdempotentForMultipleStarts() {
            // Given
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);

            // When
            reloader.start();
            reloader.start(); // Second start should be no-op

            // Then
            assertThat(reloader.isRunning()).isTrue();
        }

        @Test
        @DisplayName("should be idempotent for multiple stops")
        void shouldBeIdempotentForMultipleStops() {
            // Given
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.start();

            // When
            reloader.stop();
            reloader.stop(); // Second stop should be no-op

            // Then
            assertThat(reloader.isRunning()).isFalse();
        }

        @Test
        @DisplayName("stop should not throw when never started")
        void stopShouldNotThrowWhenNeverStarted() {
            // Given
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);

            // When/Then - should not throw
            reloader.stop();
            assertThat(reloader.isRunning()).isFalse();
        }
    }

    @Nested
    @DisplayName("Plugin registration")
    class PluginRegistrationTests {

        @Test
        @DisplayName("should register plugin JAR for watching")
        void shouldRegisterPluginJarForWatching() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);

            // When
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // Then
            assertThat(reloader.getRegisteredPlugins()).containsKey("test-plugin-id");
            assertThat(reloader.getRegisteredPlugins().get("test-plugin-id")).isEqualTo(pluginJar);
        }

        @Test
        @DisplayName("should unregister plugin JAR")
        void shouldUnregisterPluginJar() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // When
            reloader.unregisterPlugin("test-plugin-id");

            // Then
            assertThat(reloader.getRegisteredPlugins()).doesNotContainKey("test-plugin-id");
        }

        @Test
        @DisplayName("should reject null plugin ID")
        void shouldRejectNullPluginId() throws IOException {
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);

            assertThatThrownBy(() -> reloader.registerPlugin(null, pluginJar))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("pluginId");
        }

        @Test
        @DisplayName("should reject null JAR path")
        void shouldRejectNullJarPath() {
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);

            assertThatThrownBy(() -> reloader.registerPlugin("test-plugin-id", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("jarPath");
        }
    }

    @Nested
    @DisplayName("Reload sequence")
    class ReloadSequenceTests {

        @Test
        @DisplayName("should execute reload sequence: disable -> unload -> load -> enable")
        void shouldExecuteReloadSequence() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .debounceMillis(50) // Short debounce for testing
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // When
            reloader.reloadPlugin("test-plugin-id");

            // Then - verify correct sequence
            assertThat(mockOperations.operationSequence)
                .containsExactly("disable:test-plugin-id", "unload:test-plugin-id", "load:test-plugin-id", "enable:test-plugin-id");
        }

        @Test
        @DisplayName("should handle reload for unregistered plugin gracefully")
        void shouldHandleUnregisteredPluginReload() {
            // Given
            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);

            // When/Then - should not throw
            reloader.reloadPlugin("nonexistent-plugin");

            // Verify no operations were called
            assertThat(mockOperations.operationSequence).isEmpty();
        }
    }

    @Nested
    @DisplayName("UI notification")
    class NotificationTests {

        @Test
        @DisplayName("should notify listener when reload starts")
        void shouldNotifyWhenReloadStarts() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // When
            reloader.reloadPlugin("test-plugin-id");

            // Then
            assertThat(mockListener.reloadStartedPlugins).contains("test-plugin-id");
        }

        @Test
        @DisplayName("should notify listener when reload completes")
        void shouldNotifyWhenReloadCompletes() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // When
            reloader.reloadPlugin("test-plugin-id");

            // Then
            assertThat(mockListener.reloadCompletedPlugins).contains("test-plugin-id");
            assertThat(mockListener.reloadCompletedSuccess.get("test-plugin-id")).isTrue();
        }

        @Test
        @DisplayName("should notify listener when reload fails")
        void shouldNotifyWhenReloadFails() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            mockOperations.failOnLoad = true;

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // When
            reloader.reloadPlugin("test-plugin-id");

            // Then
            assertThat(mockListener.reloadCompletedPlugins).contains("test-plugin-id");
            assertThat(mockListener.reloadCompletedSuccess.get("test-plugin-id")).isFalse();
        }
    }

    @Nested
    @DisplayName("NIO WatchService file monitoring")
    class FileWatchTests {

        @Test
        @DisplayName("should detect file modification and trigger reload")
        void shouldDetectFileModificationAndReload() throws Exception {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.write(pluginJar, "initial content".getBytes());

            CountDownLatch reloadLatch = new CountDownLatch(1);
            mockListener.onReloadCompleted = (id, success) -> reloadLatch.countDown();

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .debounceMillis(100) // Short debounce for testing
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);
            reloader.start();

            // Small delay to ensure watcher is ready
            Thread.sleep(200);

            // When - modify the file
            Files.write(pluginJar, "modified content".getBytes());

            // Then - wait for reload
            boolean reloaded = reloadLatch.await(5, TimeUnit.SECONDS);
            assertThat(reloaded).isTrue();
            assertThat(mockListener.reloadCompletedPlugins).contains("test-plugin-id");
        }

        @Test
        @DisplayName("should debounce multiple rapid file changes")
        void shouldDebounceMultipleRapidChanges() throws Exception {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.write(pluginJar, "initial".getBytes());

            AtomicInteger reloadCount = new AtomicInteger(0);
            CountDownLatch firstReloadLatch = new CountDownLatch(1);
            mockListener.onReloadCompleted = (id, success) -> {
                reloadCount.incrementAndGet();
                firstReloadLatch.countDown();
            };

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .debounceMillis(300) // Longer debounce
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);
            reloader.start();

            Thread.sleep(200);

            // When - make multiple rapid changes
            Files.write(pluginJar, "change 1".getBytes());
            Thread.sleep(50);
            Files.write(pluginJar, "change 2".getBytes());
            Thread.sleep(50);
            Files.write(pluginJar, "change 3".getBytes());

            // Then - wait for debounced reload
            boolean reloaded = firstReloadLatch.await(5, TimeUnit.SECONDS);
            assertThat(reloaded).isTrue();

            // Give time for any additional reloads
            Thread.sleep(500);

            // Should have only triggered one reload due to debouncing
            assertThat(reloadCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should ignore changes to non-registered files")
        void shouldIgnoreNonRegisteredFileChanges() throws Exception {
            // Given
            Path registeredJar = tempDir.resolve("registered-plugin.jar");
            Path unregisteredJar = tempDir.resolve("unregistered-plugin.jar");
            Files.write(registeredJar, "registered".getBytes());
            Files.write(unregisteredJar, "unregistered".getBytes());

            AtomicBoolean reloadCalled = new AtomicBoolean(false);
            mockListener.onReloadCompleted = (id, success) -> reloadCalled.set(true);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .debounceMillis(100)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("registered-plugin-id", registeredJar);
            // Note: unregisteredJar is NOT registered
            reloader.start();

            Thread.sleep(200);

            // When - modify unregistered file
            Files.write(unregisteredJar, "modified unregistered".getBytes());

            // Then - wait to ensure no reload happens
            Thread.sleep(500);
            assertThat(reloadCalled.get()).isFalse();
        }
    }

    @Nested
    @DisplayName("State preservation")
    class StatePreservationTests {

        @Test
        @DisplayName("should retrieve plugin state before unload when preservation enabled")
        void shouldRetrieveStateWhenPreservationEnabled() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            mockOperations.preserveState.put("test-plugin-id", true);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // When
            reloader.reloadPlugin("test-plugin-id");

            // Then
            assertThat(mockOperations.stateRetrievedFor).contains("test-plugin-id");
        }

        @Test
        @DisplayName("should NOT retrieve state when preservation disabled")
        void shouldNotRetrieveStateWhenPreservationDisabled() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            // preservation is disabled by default

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // When
            reloader.reloadPlugin("test-plugin-id");

            // Then
            assertThat(mockOperations.stateRetrievedFor).doesNotContain("test-plugin-id");
        }

        @Test
        @DisplayName("should restore plugin state after reload when preservation enabled")
        void shouldRestoreStateAfterReload() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            mockOperations.preserveState.put("test-plugin-id", true);

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // When
            reloader.reloadPlugin("test-plugin-id");

            // Then
            assertThat(mockOperations.stateRestoredFor).contains("test-plugin-id");
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should continue watching after reload error")
        void shouldContinueWatchingAfterReloadError() throws Exception {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.write(pluginJar, "initial".getBytes());

            AtomicInteger reloadAttempts = new AtomicInteger(0);
            mockOperations.failOnLoad = true;
            mockListener.onReloadCompleted = (id, success) -> reloadAttempts.incrementAndGet();

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .debounceMillis(100)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);
            reloader.start();

            Thread.sleep(200);

            // When - trigger first reload (fails)
            Files.write(pluginJar, "change 1".getBytes());
            Thread.sleep(500);

            // Second reload attempt
            mockOperations.failOnLoad = false; // This time it should succeed
            Files.write(pluginJar, "change 2".getBytes());
            Thread.sleep(500);

            // Then - both reloads should have been attempted
            assertThat(reloadAttempts.get()).isGreaterThanOrEqualTo(2);
            assertThat(reloader.isRunning()).isTrue();
        }

        @Test
        @DisplayName("should handle IOException during state preservation gracefully")
        void shouldHandleStatePreservationError() throws IOException {
            // Given
            Path pluginJar = tempDir.resolve("test-plugin.jar");
            Files.createFile(pluginJar);

            mockOperations.preserveState.put("test-plugin-id", true);
            mockOperations.throwOnStateRetrieval = true;

            HotReloadConfig config = HotReloadConfig.builder()
                .watchDirectory(tempDir)
                .build();
            reloader = new PluginHotReloader(config, mockOperations, mockListener);
            reloader.registerPlugin("test-plugin-id", pluginJar);

            // When/Then - should not throw, reload should continue
            reloader.reloadPlugin("test-plugin-id");

            // Reload should still complete (without state preservation)
            assertThat(mockListener.reloadCompletedPlugins).contains("test-plugin-id");
        }
    }

    // ========== Mock implementations ==========

    /**
     * Mock implementation of PluginReloadOperations for testing.
     */
    static class MockPluginOperations implements PluginHotReloader.PluginReloadOperations {
        final List<String> operationSequence = new ArrayList<>();
        final java.util.Map<String, Boolean> preserveState = new java.util.HashMap<>();
        final List<String> stateRetrievedFor = new ArrayList<>();
        final List<String> stateRestoredFor = new ArrayList<>();
        boolean failOnLoad = false;
        boolean throwOnStateRetrieval = false;

        @Override
        public void disablePlugin(String pluginId) {
            operationSequence.add("disable:" + pluginId);
        }

        @Override
        public void unloadPlugin(String pluginId) {
            operationSequence.add("unload:" + pluginId);
        }

        @Override
        public void loadPlugin(String pluginId) {
            if (failOnLoad) {
                throw new RuntimeException("Simulated load failure");
            }
            operationSequence.add("load:" + pluginId);
        }

        @Override
        public void enablePlugin(String pluginId) {
            operationSequence.add("enable:" + pluginId);
        }

        @Override
        public boolean isStatePreservationEnabled(String pluginId) {
            return preserveState.getOrDefault(pluginId, false);
        }

        @Override
        public Object retrievePluginState(String pluginId) {
            if (throwOnStateRetrieval) {
                throw new RuntimeException("Simulated state retrieval failure");
            }
            stateRetrievedFor.add(pluginId);
            return "mock-state";
        }

        @Override
        public void restorePluginState(String pluginId, Object state) {
            stateRestoredFor.add(pluginId);
        }
    }

    /**
     * Mock HotReloadListener for testing.
     */
    static class MockHotReloadListener implements HotReloadListener {
        final List<String> reloadStartedPlugins = new ArrayList<>();
        final List<String> reloadCompletedPlugins = new ArrayList<>();
        final java.util.Map<String, Boolean> reloadCompletedSuccess = new java.util.HashMap<>();
        java.util.function.BiConsumer<String, Boolean> onReloadCompleted;

        @Override
        public void onReloadStarted(String pluginId) {
            reloadStartedPlugins.add(pluginId);
        }

        @Override
        public void onReloadCompleted(String pluginId, boolean success) {
            reloadCompletedPlugins.add(pluginId);
            reloadCompletedSuccess.put(pluginId, success);
            if (onReloadCompleted != null) {
                onReloadCompleted.accept(pluginId, success);
            }
        }
    }
}
