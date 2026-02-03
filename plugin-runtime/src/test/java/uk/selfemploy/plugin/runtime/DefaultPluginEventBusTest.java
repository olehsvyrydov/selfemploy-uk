package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.api.EventHandler;
import uk.selfemploy.plugin.api.PluginEvent;
import uk.selfemploy.plugin.api.Subscription;
import uk.selfemploy.plugin.api.ThreadAffinity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultPluginEventBus}.
 */
@DisplayName("DefaultPluginEventBus")
class DefaultPluginEventBusTest {

    private DefaultPluginEventBus eventBus;
    private ExecutorService testExecutor;

    // Test event classes
    static class TestEvent extends PluginEvent {
        private final String message;

        TestEvent(String sourcePluginId, String message) {
            super(sourcePluginId);
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }

    static class AnotherEvent extends PluginEvent {
        private final int value;

        AnotherEvent(String sourcePluginId, int value) {
            super(sourcePluginId);
            this.value = value;
        }

        int getValue() {
            return value;
        }
    }

    @BeforeEach
    void setUp() {
        // Use a single-threaded executor for predictable testing
        testExecutor = Executors.newSingleThreadExecutor();
        eventBus = new DefaultPluginEventBus(testExecutor);
    }

    @AfterEach
    void tearDown() {
        eventBus.shutdown();
        testExecutor.shutdownNow();
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create event bus with default executor")
        void shouldCreateWithDefaultExecutor() {
            try (var bus = new DefaultPluginEventBus()) {
                assertThat(bus.getTotalSubscriptionCount()).isZero();
            }
        }

        @Test
        @DisplayName("should create event bus with custom executor")
        void shouldCreateWithCustomExecutor() {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                var bus = new DefaultPluginEventBus(executor);
                assertThat(bus.getTotalSubscriptionCount()).isZero();
                bus.shutdown();
            } finally {
                executor.shutdownNow();
            }
        }

        @Test
        @DisplayName("should reject null executor")
        void shouldRejectNullExecutor() {
            assertThatThrownBy(() -> new DefaultPluginEventBus(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("backgroundExecutor");
        }
    }

    @Nested
    @DisplayName("subscribe()")
    class SubscribeTests {

        @Test
        @DisplayName("should subscribe handler to event type")
        void shouldSubscribeHandlerToEventType() {
            Subscription sub = eventBus.subscribe(
                TestEvent.class,
                event -> {},
                ThreadAffinity.BACKGROUND
            );

            assertThat(sub).isNotNull();
            assertThat(sub.isActive()).isTrue();
            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("should allow multiple handlers for same event type")
        void shouldAllowMultipleHandlersForSameEventType() {
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND);
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND);
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND);

            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(3);
        }

        @Test
        @DisplayName("should use default BACKGROUND affinity")
        void shouldUseDefaultBackgroundAffinity() {
            Subscription sub = eventBus.subscribe(TestEvent.class, e -> {});

            assertThat(sub.isActive()).isTrue();
            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("should reject null event type")
        void shouldRejectNullEventType() {
            assertThatThrownBy(() -> eventBus.subscribe(null, e -> {}, ThreadAffinity.BACKGROUND))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventType");
        }

        @Test
        @DisplayName("should reject null handler")
        void shouldRejectNullHandler() {
            assertThatThrownBy(() -> eventBus.subscribe(TestEvent.class, null, ThreadAffinity.BACKGROUND))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("handler");
        }

        @Test
        @DisplayName("should reject null affinity")
        void shouldRejectNullAffinity() {
            assertThatThrownBy(() -> eventBus.subscribe(TestEvent.class, e -> {}, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("affinity");
        }

        @Test
        @DisplayName("should track subscription by plugin ID")
        void shouldTrackSubscriptionByPluginId() {
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND, "plugin.a");
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND, "plugin.a");
            eventBus.subscribe(AnotherEvent.class, e -> {}, ThreadAffinity.BACKGROUND, "plugin.a");

            assertThat(eventBus.getTotalSubscriptionCount()).isEqualTo(3);

            eventBus.unsubscribeAll("plugin.a");

            assertThat(eventBus.getTotalSubscriptionCount()).isZero();
        }

        @Test
        @DisplayName("should throw when subscribing after shutdown")
        void shouldThrowWhenSubscribingAfterShutdown() {
            eventBus.shutdown();

            assertThatThrownBy(() -> eventBus.subscribe(TestEvent.class, e -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shut down");
        }
    }

    @Nested
    @DisplayName("publish()")
    class PublishTests {

        @Test
        @DisplayName("should deliver event to subscriber")
        void shouldDeliverEventToSubscriber() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> received = new AtomicReference<>();

            eventBus.subscribe(TestEvent.class, event -> {
                received.set(event.getMessage());
                latch.countDown();
            });

            eventBus.publish(new TestEvent("source", "hello"));

            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(received.get()).isEqualTo("hello");
        }

        @Test
        @DisplayName("should deliver event to multiple subscribers")
        void shouldDeliverEventToMultipleSubscribers() throws InterruptedException {
            int subscriberCount = 5;
            CountDownLatch latch = new CountDownLatch(subscriberCount);
            AtomicInteger deliveryCount = new AtomicInteger(0);

            for (int i = 0; i < subscriberCount; i++) {
                eventBus.subscribe(TestEvent.class, event -> {
                    deliveryCount.incrementAndGet();
                    latch.countDown();
                });
            }

            eventBus.publish(new TestEvent("source", "test"));

            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(deliveryCount.get()).isEqualTo(subscriberCount);
        }

        @Test
        @DisplayName("should not deliver event to unsubscribed handlers")
        void shouldNotDeliverToUnsubscribedHandlers() throws InterruptedException {
            AtomicBoolean handler1Called = new AtomicBoolean(false);
            AtomicBoolean handler2Called = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            Subscription sub1 = eventBus.subscribe(TestEvent.class, e -> handler1Called.set(true));
            eventBus.subscribe(TestEvent.class, e -> {
                handler2Called.set(true);
                latch.countDown();
            });

            sub1.unsubscribe();
            eventBus.publish(new TestEvent("source", "test"));

            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(handler1Called.get()).isFalse();
            assertThat(handler2Called.get()).isTrue();
        }

        @Test
        @DisplayName("should not deliver events of different types")
        void shouldNotDeliverEventsOfDifferentTypes() throws InterruptedException {
            AtomicBoolean testEventHandled = new AtomicBoolean(false);
            AtomicBoolean anotherEventHandled = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            eventBus.subscribe(TestEvent.class, e -> testEventHandled.set(true));
            eventBus.subscribe(AnotherEvent.class, e -> {
                anotherEventHandled.set(true);
                latch.countDown();
            });

            eventBus.publish(new AnotherEvent("source", 42));

            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(testEventHandled.get()).isFalse();
            assertThat(anotherEventHandled.get()).isTrue();
        }

        @Test
        @DisplayName("should reject null event")
        void shouldRejectNullEvent() {
            assertThatThrownBy(() -> eventBus.publish(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event");
        }

        @Test
        @DisplayName("should handle publish with no subscribers")
        void shouldHandlePublishWithNoSubscribers() {
            // Should not throw
            eventBus.publish(new TestEvent("source", "orphan"));
        }

        @Test
        @DisplayName("should not publish after shutdown")
        void shouldNotPublishAfterShutdown() {
            AtomicBoolean handled = new AtomicBoolean(false);
            eventBus.subscribe(TestEvent.class, e -> handled.set(true));

            eventBus.shutdown();
            eventBus.publish(new TestEvent("source", "test"));

            // Give some time for potential async delivery
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            assertThat(handled.get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Exception isolation (COND-1101-C)")
    class ExceptionIsolationTests {

        @Test
        @DisplayName("should isolate exceptions between handlers")
        void shouldIsolateExceptionsBetweenHandlers() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(2);
            AtomicBoolean handler1Called = new AtomicBoolean(false);
            AtomicBoolean handler2Called = new AtomicBoolean(false);

            // Handler that throws
            eventBus.subscribe(TestEvent.class, event -> {
                handler1Called.set(true);
                latch.countDown();
                throw new RuntimeException("Handler 1 error");
            });

            // Handler that should still receive event
            eventBus.subscribe(TestEvent.class, event -> {
                handler2Called.set(true);
                latch.countDown();
            });

            eventBus.publish(new TestEvent("source", "test"));

            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(handler1Called.get()).isTrue();
            assertThat(handler2Called.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Subscription")
    class SubscriptionTests {

        @Test
        @DisplayName("should unsubscribe via subscription object")
        void shouldUnsubscribeViaSubscriptionObject() {
            Subscription sub = eventBus.subscribe(TestEvent.class, e -> {});
            assertThat(sub.isActive()).isTrue();
            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(1);

            sub.unsubscribe();

            assertThat(sub.isActive()).isFalse();
            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isZero();
        }

        @Test
        @DisplayName("should be idempotent on multiple unsubscribe calls")
        void shouldBeIdempotentOnMultipleUnsubscribeCalls() {
            Subscription sub = eventBus.subscribe(TestEvent.class, e -> {});

            sub.unsubscribe();
            sub.unsubscribe();
            sub.unsubscribe();

            assertThat(sub.isActive()).isFalse();
        }

        @Test
        @DisplayName("should support try-with-resources")
        void shouldSupportTryWithResources() {
            try (Subscription sub = eventBus.subscribe(TestEvent.class, e -> {})) {
                assertThat(sub.isActive()).isTrue();
            }

            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isZero();
        }
    }

    @Nested
    @DisplayName("unsubscribeAll()")
    class UnsubscribeAllTests {

        @Test
        @DisplayName("should unsubscribe all handlers for plugin")
        void shouldUnsubscribeAllHandlersForPlugin() {
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND, "plugin.a");
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND, "plugin.a");
            eventBus.subscribe(AnotherEvent.class, e -> {}, ThreadAffinity.BACKGROUND, "plugin.a");
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND, "plugin.b");

            eventBus.unsubscribeAll("plugin.a");

            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(1);
            assertThat(eventBus.getSubscriptionCount(AnotherEvent.class)).isZero();
        }

        @Test
        @DisplayName("should handle null plugin ID")
        void shouldHandleNullPluginId() {
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND, "plugin.a");

            eventBus.unsubscribeAll(null);

            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle unknown plugin ID")
        void shouldHandleUnknownPluginId() {
            eventBus.subscribe(TestEvent.class, e -> {}, ThreadAffinity.BACKGROUND, "plugin.a");

            eventBus.unsubscribeAll("plugin.unknown");

            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Thread safety (COND-1101-E)")
    class ThreadSafetyTests {

        @Test
        @DisplayName("should handle concurrent subscriptions")
        void shouldHandleConcurrentSubscriptions() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await();
                        eventBus.subscribe(TestEvent.class, e -> {});
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }

            startLatch.countDown();
            assertThat(doneLatch.await(1, TimeUnit.SECONDS)).isTrue();

            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("should handle concurrent publish and subscribe")
        void shouldHandleConcurrentPublishAndSubscribe() throws InterruptedException {
            int iterations = 100;
            CountDownLatch latch = new CountDownLatch(iterations);
            List<String> received = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch subscriberReady = new CountDownLatch(1);

            // Add initial subscriber to ensure at least one is ready before publishing
            eventBus.subscribe(TestEvent.class, event -> {
                received.add(event.getMessage());
                latch.countDown();
            });

            // Subscriber thread adds more subscribers concurrently
            Thread subscriber = new Thread(() -> {
                subscriberReady.countDown();
                for (int i = 0; i < iterations / 2; i++) {
                    eventBus.subscribe(TestEvent.class, event -> {
                        received.add(event.getMessage());
                        latch.countDown();
                    });
                }
            });

            // Publisher thread
            Thread publisher = new Thread(() -> {
                try {
                    // Wait for subscriber thread to start
                    subscriberReady.await(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 0; i < iterations; i++) {
                    eventBus.publish(new TestEvent("source", "msg-" + i));
                }
            });

            subscriber.start();
            publisher.start();

            subscriber.join(1000);
            publisher.join(1000);

            // Wait for some deliveries
            latch.await(1, TimeUnit.SECONDS);

            // Some messages should have been received (at least by the initial subscriber)
            assertThat(received).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("getSubscriptionCount()")
    class GetSubscriptionCountTests {

        @Test
        @DisplayName("should return zero for no subscriptions")
        void shouldReturnZeroForNoSubscriptions() {
            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isZero();
        }

        @Test
        @DisplayName("should return correct count")
        void shouldReturnCorrectCount() {
            eventBus.subscribe(TestEvent.class, e -> {});
            eventBus.subscribe(TestEvent.class, e -> {});

            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(2);
        }

        @Test
        @DisplayName("should not count unsubscribed handlers")
        void shouldNotCountUnsubscribedHandlers() {
            Subscription sub1 = eventBus.subscribe(TestEvent.class, e -> {});
            eventBus.subscribe(TestEvent.class, e -> {});

            sub1.unsubscribe();

            assertThat(eventBus.getSubscriptionCount(TestEvent.class)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getTotalSubscriptionCount()")
    class GetTotalSubscriptionCountTests {

        @Test
        @DisplayName("should return total across all event types")
        void shouldReturnTotalAcrossAllEventTypes() {
            eventBus.subscribe(TestEvent.class, e -> {});
            eventBus.subscribe(TestEvent.class, e -> {});
            eventBus.subscribe(AnotherEvent.class, e -> {});

            assertThat(eventBus.getTotalSubscriptionCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("shutdown()")
    class ShutdownTests {

        @Test
        @DisplayName("should clear all subscriptions")
        void shouldClearAllSubscriptions() {
            eventBus.subscribe(TestEvent.class, e -> {});
            eventBus.subscribe(AnotherEvent.class, e -> {});

            eventBus.shutdown();

            assertThat(eventBus.getTotalSubscriptionCount()).isZero();
        }

        @Test
        @DisplayName("should be idempotent")
        void shouldBeIdempotent() {
            eventBus.shutdown();
            eventBus.shutdown();
            eventBus.shutdown();

            // Should not throw
            assertThat(eventBus.getTotalSubscriptionCount()).isZero();
        }
    }
}
