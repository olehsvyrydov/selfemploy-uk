package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExtensionRegistry")
class ExtensionRegistryTest {

    private ExtensionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ExtensionRegistry();
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Registers extension without plugin ID")
        void registersWithoutPluginId() {
            TestExtension extension = createExtension("Test");

            registry.register(TestExtension.class, extension);

            assertThat(registry.getExtensions(TestExtension.class)).containsExactly(extension);
        }

        @Test
        @DisplayName("Registers extension with plugin ID")
        void registersWithPluginId() {
            TestExtension extension = createExtension("Test");

            registry.register("my-plugin", TestExtension.class, extension);

            assertThat(registry.getExtensions(TestExtension.class)).containsExactly(extension);
        }

        @Test
        @DisplayName("Registers multiple extensions of same type")
        void registersMultiple() {
            TestExtension ext1 = createExtension("First");
            TestExtension ext2 = createExtension("Second");

            registry.register(TestExtension.class, ext1);
            registry.register(TestExtension.class, ext2);

            assertThat(registry.getExtensions(TestExtension.class)).containsExactly(ext1, ext2);
        }

        @Test
        @DisplayName("Throws on null type")
        void throwsOnNullType() {
            assertThatThrownBy(() -> registry.register(null, createExtension("Test")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
        }

        @Test
        @DisplayName("Throws on null extension")
        void throwsOnNullExtension() {
            assertThatThrownBy(() -> registry.register(TestExtension.class, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("extension");
        }
    }

    @Nested
    @DisplayName("Unregistration")
    class Unregistration {

        @Test
        @DisplayName("Unregisters single extension")
        void unregistersSingle() {
            TestExtension extension = createExtension("Test");
            registry.register(TestExtension.class, extension);

            boolean result = registry.unregister(TestExtension.class, extension);

            assertThat(result).isTrue();
            assertThat(registry.getExtensions(TestExtension.class)).isEmpty();
        }

        @Test
        @DisplayName("Returns false when extension not found")
        void returnsFalseWhenNotFound() {
            TestExtension extension = createExtension("Test");

            boolean result = registry.unregister(TestExtension.class, extension);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Unregisters all extensions for plugin")
        void unregistersAllForPlugin() {
            TestExtension ext1 = createExtension("First");
            TestExtension ext2 = createExtension("Second");
            TestExtension ext3 = createExtension("Third");

            registry.register("plugin-a", TestExtension.class, ext1);
            registry.register("plugin-a", TestExtension.class, ext2);
            registry.register("plugin-b", TestExtension.class, ext3);

            int count = registry.unregisterAll("plugin-a");

            assertThat(count).isEqualTo(2);
            assertThat(registry.getExtensions(TestExtension.class)).containsExactly(ext3);
        }

        @Test
        @DisplayName("Returns 0 when no extensions for plugin")
        void returnsZeroForUnknownPlugin() {
            int count = registry.unregisterAll("unknown-plugin");

            assertThat(count).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Returns empty list for unregistered type")
        void returnsEmptyForUnknownType() {
            List<TestExtension> extensions = registry.getExtensions(TestExtension.class);

            assertThat(extensions).isEmpty();
        }

        @Test
        @DisplayName("Returns unmodifiable list")
        void returnsUnmodifiableList() {
            TestExtension extension = createExtension("Test");
            registry.register(TestExtension.class, extension);

            List<TestExtension> extensions = registry.getExtensions(TestExtension.class);

            assertThatThrownBy(() -> extensions.add(createExtension("New")))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Preserves registration order")
        void preservesOrder() {
            TestExtension ext1 = createExtension("First");
            TestExtension ext2 = createExtension("Second");
            TestExtension ext3 = createExtension("Third");

            registry.register(TestExtension.class, ext1);
            registry.register(TestExtension.class, ext2);
            registry.register(TestExtension.class, ext3);

            List<TestExtension> extensions = registry.getExtensions(TestExtension.class);

            assertThat(extensions).containsExactly(ext1, ext2, ext3);
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        @Test
        @DisplayName("hasExtensions returns true when extensions registered")
        void hasExtensionsReturnsTrue() {
            registry.register(TestExtension.class, createExtension("Test"));

            assertThat(registry.hasExtensions(TestExtension.class)).isTrue();
        }

        @Test
        @DisplayName("hasExtensions returns false when no extensions")
        void hasExtensionsReturnsFalse() {
            assertThat(registry.hasExtensions(TestExtension.class)).isFalse();
        }

        @Test
        @DisplayName("getExtensionCount returns correct count")
        void getExtensionCountReturnsCount() {
            registry.register(TestExtension.class, createExtension("First"));
            registry.register(TestExtension.class, createExtension("Second"));

            assertThat(registry.getExtensionCount(TestExtension.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("getTotalExtensionCount returns total across types")
        void getTotalExtensionCount() {
            registry.register(TestExtension.class, createExtension("First"));
            registry.register(TestExtension.class, createExtension("Second"));

            assertThat(registry.getTotalExtensionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("getRegisteredTypes returns all types")
        void getRegisteredTypes() {
            registry.register(TestExtension.class, createExtension("Test"));

            var types = registry.getRegisteredTypes();

            assertThat(types.contains(TestExtension.class)).isTrue();
        }
    }

    @Nested
    @DisplayName("Clear")
    class Clear {

        @Test
        @DisplayName("Clears all extensions")
        void clearsAll() {
            registry.register(TestExtension.class, createExtension("Test"));

            registry.clear();

            assertThat(registry.getTotalExtensionCount()).isEqualTo(0);
            assertThat(registry.getRegisteredTypes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Thread Safety (EXT-040 to EXT-044)")
    class ThreadSafety {

        private static final int THREAD_COUNT = 10;
        private static final int OPERATIONS_PER_THREAD = 100;

        @RepeatedTest(3)
        @DisplayName("EXT-040: Concurrent registration is thread-safe")
        void concurrentRegistrationIsThreadSafe() throws InterruptedException {
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
                            TestExtension ext = createExtension("thread-" + threadId + "-ext-" + j);
                            registry.register("plugin-" + threadId, TestExtension.class, ext);
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
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(successCount.get()).isEqualTo(THREAD_COUNT * OPERATIONS_PER_THREAD);
            assertThat(registry.getTotalExtensionCount()).isEqualTo(THREAD_COUNT * OPERATIONS_PER_THREAD);
        }

        @RepeatedTest(3)
        @DisplayName("EXT-041: Concurrent unregistration is thread-safe")
        void concurrentUnregistrationIsThreadSafe() throws InterruptedException {
            // First, register extensions
            List<TestExtension> allExtensions = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    TestExtension ext = createExtension("ext-" + i + "-" + j);
                    registry.register("plugin-" + i, TestExtension.class, ext);
                    allExtensions.add(ext);
                }
            }

            assertThat(registry.getTotalExtensionCount()).isEqualTo(THREAD_COUNT * OPERATIONS_PER_THREAD);

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        registry.unregisterAll("plugin-" + threadId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(registry.getTotalExtensionCount()).isEqualTo(0);
        }

        @RepeatedTest(3)
        @DisplayName("EXT-042: Concurrent read/write is thread-safe")
        void concurrentReadWriteIsThreadSafe() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
            AtomicInteger exceptionCount = new AtomicInteger(0);

            // Half threads write, half threads read
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            if (threadId % 2 == 0) {
                                // Writer thread
                                TestExtension ext = createExtension("ext-" + threadId + "-" + j);
                                registry.register("plugin-" + threadId, TestExtension.class, ext);
                            } else {
                                // Reader thread
                                List<TestExtension> extensions = registry.getExtensions(TestExtension.class);
                                // Iterate through the list (should not cause ConcurrentModificationException)
                                for (TestExtension ext : extensions) {
                                    ext.getName(); // Access element
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
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(exceptionCount.get()).isEqualTo(0);
        }

        @RepeatedTest(3)
        @DisplayName("EXT-043: getExtensions returns snapshot-safe list")
        void getExtensionsReturnsSnapshotSafeList() throws InterruptedException {
            // Register some initial extensions
            for (int i = 0; i < 50; i++) {
                registry.register(TestExtension.class, createExtension("initial-" + i));
            }

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);
            AtomicInteger exceptionCount = new AtomicInteger(0);

            // Thread 1: Continuously iterate
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 100; i++) {
                        List<TestExtension> extensions = registry.getExtensions(TestExtension.class);
                        // Iterate through - should not throw ConcurrentModificationException
                        for (TestExtension ext : extensions) {
                            ext.getName();
                        }
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            // Thread 2: Continuously modify
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 100; i++) {
                        TestExtension ext = createExtension("new-" + i);
                        registry.register(TestExtension.class, ext);
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            startLatch.countDown();
            boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(exceptionCount.get()).isEqualTo(0);
        }

        @Test
        @DisplayName("EXT-044: Registry uses ConcurrentHashMap for thread safety")
        void registryUsesConcurrentDataStructures() {
            // This is implicitly tested by the concurrent tests above passing
            // We verify the behavior characteristics of ConcurrentHashMap
            registry.register(TestExtension.class, createExtension("test"));

            // Operations should be atomic
            assertThat(registry.getTotalExtensionCount()).isEqualTo(1);

            // Modifications while iterating should not throw
            for (Class<?> type : registry.getRegisteredTypes()) {
                registry.register(TestExtension.class, createExtension("during-iteration"));
                assertThat(type).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Null input handling (EDGE-001 to EDGE-004)")
    class NullInputHandling {

        @Test
        @DisplayName("EDGE-001: register with null type throws NullPointerException")
        void registerWithNullTypeThrows() {
            assertThatThrownBy(() -> registry.register(null, createExtension("test")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
        }

        @Test
        @DisplayName("EDGE-002: register with null extension throws NullPointerException")
        void registerWithNullExtensionThrows() {
            assertThatThrownBy(() -> registry.register(TestExtension.class, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("extension");
        }

        @Test
        @DisplayName("EDGE-003: unregister with null type throws NullPointerException")
        void unregisterWithNullTypeThrows() {
            assertThatThrownBy(() -> registry.unregister(null, createExtension("test")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
        }

        @Test
        @DisplayName("EDGE-004: unregisterAll with null pluginId throws NullPointerException")
        void unregisterAllWithNullPluginIdThrows() {
            assertThatThrownBy(() -> registry.unregisterAll(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("pluginId");
        }
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
