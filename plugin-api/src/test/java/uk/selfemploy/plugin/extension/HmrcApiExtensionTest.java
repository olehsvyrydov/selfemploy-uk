package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.extension.HmrcApiExtension.HmrcApiType;
import uk.selfemploy.plugin.extension.HmrcApiExtension.HmrcSubmissionContext;
import uk.selfemploy.plugin.extension.HmrcApiExtension.HmrcSubmissionResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HmrcApiExtension} interface.
 */
@DisplayName("HmrcApiExtension")
class HmrcApiExtensionTest {

    @Nested
    @DisplayName("HmrcApiType enum")
    class HmrcApiTypeEnum {

        @Test
        @DisplayName("should have expected types")
        void shouldHaveExpectedTypes() {
            assertThat(HmrcApiType.values()).containsExactlyInAnyOrder(
                HmrcApiType.SELF_ASSESSMENT,
                HmrcApiType.VAT,
                HmrcApiType.PARTNERSHIP,
                HmrcApiType.CORPORATION_TAX,
                HmrcApiType.OTHER
            );
        }
    }

    @Nested
    @DisplayName("HmrcSubmissionContext record")
    class SubmissionContextRecord {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            Map<String, Object> data = Map.of("key", "value");
            HmrcSubmissionContext context = new HmrcSubmissionContext(
                2024, "token123", true, data
            );

            assertThat(context.taxYear()).isEqualTo(2024);
            assertThat(context.accessToken()).isEqualTo("token123");
            assertThat(context.sandbox()).isTrue();
            assertThat(context.submissionData()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("getData should return value")
        void getDataShouldReturnValue() {
            HmrcSubmissionContext context = new HmrcSubmissionContext(
                2024, "token", false, Map.of("amount", 1000)
            );

            assertThat(context.<Integer>getData("amount", 0)).isEqualTo(1000);
        }

        @Test
        @DisplayName("getData should return default for missing key")
        void getDataShouldReturnDefaultForMissingKey() {
            HmrcSubmissionContext context = new HmrcSubmissionContext(
                2024, "token", false, Collections.emptyMap()
            );

            assertThat(context.<Integer>getData("missing", 42)).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("HmrcSubmissionResult record")
    class SubmissionResultRecord {

        @Test
        @DisplayName("success should create successful result")
        void successShouldCreateSuccessfulResult() {
            HmrcSubmissionResult result = HmrcSubmissionResult.success(
                "corr-123", "receipt-456"
            );

            assertThat(result.success()).isTrue();
            assertThat(result.correlationId()).isEqualTo("corr-123");
            assertThat(result.receiptId()).isEqualTo("receipt-456");
            assertThat(result.errorCode()).isNull();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("failure should create failed result")
        void failureShouldCreateFailedResult() {
            HmrcSubmissionResult result = HmrcSubmissionResult.failure(
                "corr-123", "INVALID_REQUEST", "Missing required field"
            );

            assertThat(result.success()).isFalse();
            assertThat(result.correlationId()).isEqualTo("corr-123");
            assertThat(result.receiptId()).isNull();
            assertThat(result.errorCode()).isEqualTo("INVALID_REQUEST");
            assertThat(result.errorMessage()).isEqualTo("Missing required field");
        }
    }

    @Nested
    @DisplayName("when implementing extension")
    class Implementation {

        @Test
        @DisplayName("should be implementable")
        void shouldBeImplementable() {
            HmrcApiExtension extension = new TestHmrcApiExtension();

            assertThat(extension.getExtensionId()).isEqualTo("test-hmrc");
            assertThat(extension.getExtensionName()).isEqualTo("Test HMRC Extension");
            assertThat(extension.getApiType()).isEqualTo(HmrcApiType.SELF_ASSESSMENT);
        }

        @Test
        @DisplayName("should have default empty required scopes")
        void shouldHaveDefaultEmptyRequiredScopes() {
            HmrcApiExtension extension = new TestHmrcApiExtension();

            assertThat(extension.getRequiredScopes()).isEmpty();
        }

        @Test
        @DisplayName("should extend ExtensionPoint")
        void shouldExtendExtensionPoint() {
            HmrcApiExtension extension = new TestHmrcApiExtension();

            assertThat(extension).isInstanceOf(ExtensionPoint.class);
        }
    }

    /**
     * Test implementation of HmrcApiExtension.
     */
    private static class TestHmrcApiExtension implements HmrcApiExtension {
        @Override
        public String getExtensionId() {
            return "test-hmrc";
        }

        @Override
        public String getExtensionName() {
            return "Test HMRC Extension";
        }

        @Override
        public HmrcApiType getApiType() {
            return HmrcApiType.SELF_ASSESSMENT;
        }

        @Override
        public boolean canSubmit(HmrcSubmissionContext context) {
            return true;
        }

        @Override
        public HmrcSubmissionResult submit(HmrcSubmissionContext context) {
            return HmrcSubmissionResult.success("corr-123", "receipt-456");
        }
    }
}
