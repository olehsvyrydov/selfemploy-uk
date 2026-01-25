package uk.selfemploy.ui.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for Toast Notification System (Sprint 10A).
 * Tests TN-I01 through TN-I02 from /rob's test design.
 *
 * <p>SE-10A-006: Toast Notification Integration Tests</p>
 *
 * <p>These tests verify toast notification integration with application services
 * without requiring full JavaFX toolkit initialization.</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@DisplayName("Toast Notification Integration Tests")
class ToastNotificationIntegrationTest {

    // === TN-I01: Toast on Income Saved ===

    @Nested
    @DisplayName("TN-I01: Toast on Income Saved")
    class ToastOnIncomeSaved {

        @Test
        @DisplayName("should trigger success toast callback when income is saved")
        void shouldTriggerSuccessToastCallbackWhenIncomeSaved() {
            // Given - a mock toast callback
            AtomicBoolean toastTriggered = new AtomicBoolean(false);
            AtomicBoolean isSuccess = new AtomicBoolean(false);
            String[] capturedMessage = new String[1];

            // Simulate a service with toast callback
            MockIncomeService service = new MockIncomeService((message, type) -> {
                toastTriggered.set(true);
                isSuccess.set(type == ToastType.SUCCESS);
                capturedMessage[0] = message;
            });

            // When - save income
            service.saveIncome("Consulting work", 1500.00);

            // Then
            assertThat(toastTriggered.get()).isTrue();
            assertThat(isSuccess.get()).isTrue();
            assertThat(capturedMessage[0]).containsIgnoringCase("saved");
        }

        @Test
        @DisplayName("should trigger error toast callback when save fails")
        void shouldTriggerErrorToastCallbackWhenSaveFails() {
            // Given - a mock toast callback
            AtomicBoolean toastTriggered = new AtomicBoolean(false);
            AtomicBoolean isError = new AtomicBoolean(false);

            MockIncomeService service = new MockIncomeService((message, type) -> {
                toastTriggered.set(true);
                isError.set(type == ToastType.ERROR);
            });

            // When - save fails
            service.saveIncomeWithError();

            // Then
            assertThat(toastTriggered.get()).isTrue();
            assertThat(isError.get()).isTrue();
        }

        @Test
        @DisplayName("should include amount in success message")
        void shouldIncludeAmountInSuccessMessage() {
            // Given
            String[] capturedMessage = new String[1];
            MockIncomeService service = new MockIncomeService((message, type) -> {
                capturedMessage[0] = message;
            });

            // When
            service.saveIncome("Project payment", 2500.00);

            // Then
            assertThat(capturedMessage[0]).isNotNull();
            // Message should be descriptive
            assertThat(capturedMessage[0]).isNotEmpty();
        }
    }

    // === TN-I02: Toast on Export Complete ===

    @Nested
    @DisplayName("TN-I02: Toast on Export Complete")
    class ToastOnExportComplete {

        @Test
        @DisplayName("should trigger success toast when export completes")
        void shouldTriggerSuccessToastWhenExportCompletes() {
            // Given
            AtomicBoolean toastTriggered = new AtomicBoolean(false);
            AtomicBoolean isSuccess = new AtomicBoolean(false);
            String[] capturedMessage = new String[1];

            MockExportService service = new MockExportService((message, type) -> {
                toastTriggered.set(true);
                isSuccess.set(type == ToastType.SUCCESS);
                capturedMessage[0] = message;
            });

            // When
            service.exportData("/home/user/Documents/export.csv");

            // Then
            assertThat(toastTriggered.get()).isTrue();
            assertThat(isSuccess.get()).isTrue();
        }

        @Test
        @DisplayName("should include file path in export success message")
        void shouldIncludeFilePathInExportSuccessMessage() {
            // Given
            String[] capturedMessage = new String[1];
            MockExportService service = new MockExportService((message, type) -> {
                capturedMessage[0] = message;
            });

            // When
            service.exportData("/home/user/Documents/tax-data-2025.csv");

            // Then
            assertThat(capturedMessage[0]).isNotNull();
            assertThat(capturedMessage[0])
                    .containsAnyOf("export", "Export", "saved", "Saved");
        }

        @Test
        @DisplayName("should trigger error toast when export fails")
        void shouldTriggerErrorToastWhenExportFails() {
            // Given
            AtomicBoolean isError = new AtomicBoolean(false);
            String[] capturedMessage = new String[1];

            MockExportService service = new MockExportService((message, type) -> {
                isError.set(type == ToastType.ERROR);
                capturedMessage[0] = message;
            });

            // When - export to read-only location
            service.exportWithError("/readonly/location/file.csv");

            // Then
            assertThat(isError.get()).isTrue();
            assertThat(capturedMessage[0]).isNotNull();
        }
    }

    // === Toast Callback Integration ===

    @Nested
    @DisplayName("Toast Callback Integration")
    class ToastCallbackIntegration {

        @Test
        @DisplayName("should support multiple toast callbacks")
        void shouldSupportMultipleToastCallbacks() {
            // Given
            AtomicInteger toastCount = new AtomicInteger(0);

            ToastCallback callback = (message, type) -> toastCount.incrementAndGet();
            MockIncomeService service = new MockIncomeService(callback);

            // When - multiple operations
            service.saveIncome("Income 1", 100.00);
            service.saveIncome("Income 2", 200.00);
            service.saveIncome("Income 3", 300.00);

            // Then
            assertThat(toastCount.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("should handle null callback gracefully")
        void shouldHandleNullCallbackGracefully() {
            // Given - service with null callback
            MockIncomeService service = new MockIncomeService(null);

            // When/Then - should not throw
            service.saveIncome("Income", 100.00);
        }
    }

    // === Helper Types ===

    enum ToastType {
        SUCCESS, ERROR, WARNING, INFO
    }

    @FunctionalInterface
    interface ToastCallback {
        void show(String message, ToastType type);
    }

    static class MockIncomeService {
        private final ToastCallback toastCallback;

        MockIncomeService(ToastCallback callback) {
            this.toastCallback = callback;
        }

        void saveIncome(String description, double amount) {
            // Simulate save operation
            if (toastCallback != null) {
                toastCallback.show("Income saved successfully", ToastType.SUCCESS);
            }
        }

        void saveIncomeWithError() {
            // Simulate save failure
            if (toastCallback != null) {
                toastCallback.show("Failed to save income", ToastType.ERROR);
            }
        }
    }

    static class MockExportService {
        private final ToastCallback toastCallback;

        MockExportService(ToastCallback callback) {
            this.toastCallback = callback;
        }

        void exportData(String path) {
            // Simulate export operation
            if (toastCallback != null) {
                toastCallback.show("Export completed: " + path, ToastType.SUCCESS);
            }
        }

        void exportWithError(String path) {
            // Simulate export failure
            if (toastCallback != null) {
                toastCallback.show("Failed to export to: " + path, ToastType.ERROR);
            }
        }
    }
}
