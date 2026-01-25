package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency integration tests for the plugin system.
 * Verifies thread safety under high contention.
 */
@DisplayName("Concurrency Integration Tests")
class ConcurrencyIT {

    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 50;

    @TempDir
    Path tempDir;

    private PluginRegistry pluginRegistry;
    private ExtensionRegistry extensionRegistry;

    @BeforeEach
    void setUp() {
        pluginRegistry = new PluginRegistry();
        extensionRegistry = new ExtensionRegistry();
    }

    @RepeatedTest(3)
    @DisplayName("Concurrent plugin registration to registry")
    void concurrentPluginRegistration() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String pluginId = "plugin-" + threadId + "-" + j;
                        TestPlugin plugin = new TestPlugin(pluginId, "Plugin " + pluginId, "1.0.0", "0.1.0");
                        PluginContainer container = new PluginContainer(plugin);
                        pluginRegistry.add(container);
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(THREAD_COUNT * OPERATIONS_PER_THREAD);
        assertThat(pluginRegistry.size()).isEqualTo(THREAD_COUNT * OPERATIONS_PER_THREAD);
    }

    @RepeatedTest(3)
    @DisplayName("Concurrent extension registration and retrieval")
    void concurrentExtensionOperations() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        // Half threads register, half threads read
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        if (threadId % 2 == 0) {
                            // Writer thread
                            String pluginId = "plugin-" + threadId;
                            TestExtension ext = createExtension("ext-" + threadId + "-" + j);
                            extensionRegistry.register(pluginId, TestExtension.class, ext);
                        } else {
                            // Reader thread
                            List<TestExtension> extensions = extensionRegistry.getExtensions(TestExtension.class);
                            // Iterate - should not throw ConcurrentModificationException
                            for (TestExtension ext : extensions) {
                                ext.getName();
                            }
                        }
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(exceptionCount.get()).isEqualTo(0);
    }

    @RepeatedTest(3)
    @DisplayName("Concurrent plugin state queries")
    void concurrentPluginStateQueries() throws InterruptedException {
        // Pre-populate registry
        List<PluginContainer> containers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TestPlugin plugin = new TestPlugin("plugin-" + i, "Plugin " + i, "1.0.0", "0.1.0");
            PluginContainer container = new PluginContainer(plugin);
            containers.add(container);
            pluginRegistry.add(container);
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        // Various query operations
                        switch (threadId % 4) {
                            case 0 -> pluginRegistry.getAll();
                            case 1 -> pluginRegistry.get("plugin-" + (j % 100));
                            case 2 -> pluginRegistry.contains("plugin-" + (j % 100));
                            case 3 -> pluginRegistry.size();
                        }
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(exceptionCount.get()).isEqualTo(0);
    }

    @RepeatedTest(3)
    @DisplayName("Mixed concurrent operations on extension registry")
    void mixedConcurrentExtensionOperations() throws InterruptedException {
        // Pre-populate with some extensions
        for (int i = 0; i < 50; i++) {
            String pluginId = "initial-plugin-" + i;
            extensionRegistry.register(pluginId, TestExtension.class, createExtension("initial-" + i));
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        int operation = (threadId + j) % 5;
                        switch (operation) {
                            case 0 -> {
                                // Register
                                String pluginId = "plugin-" + threadId + "-" + j;
                                extensionRegistry.register(pluginId, TestExtension.class,
                                    createExtension("ext-" + threadId + "-" + j));
                            }
                            case 1 -> {
                                // Get extensions
                                extensionRegistry.getExtensions(TestExtension.class);
                            }
                            case 2 -> {
                                // Check count
                                extensionRegistry.getTotalExtensionCount();
                            }
                            case 3 -> {
                                // Has extensions
                                extensionRegistry.hasExtensions(TestExtension.class);
                            }
                            case 4 -> {
                                // Unregister all for a plugin
                                extensionRegistry.unregisterAll("plugin-" + ((threadId + j) % 10) + "-0");
                            }
                        }
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(exceptionCount.get()).isEqualTo(0);
    }

    private TestExtension createExtension(String name) {
        return new TestExtension() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public void execute() {
                // no-op
            }
        };
    }
}
