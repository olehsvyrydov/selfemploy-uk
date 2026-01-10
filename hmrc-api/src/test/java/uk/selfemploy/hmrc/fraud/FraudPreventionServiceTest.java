package uk.selfemploy.hmrc.fraud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.fraud.collectors.DeviceIdCollector;
import uk.selfemploy.hmrc.fraud.collectors.LocalIpsCollector;
import uk.selfemploy.hmrc.fraud.collectors.TimezoneCollector;
import uk.selfemploy.hmrc.fraud.collectors.UserIdsCollector;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FraudPreventionService.
 */
@DisplayName("FraudPreventionService")
class FraudPreventionServiceTest {

    private FraudPreventionService service;

    @BeforeEach
    void setup() {
        // Use real collectors
        service = new FraudPreventionService(
            new DeviceIdCollector(),
            new TimezoneCollector(),
            new LocalIpsCollector(),
            new UserIdsCollector(),
            "1.0.0-TEST"
        );
    }

    @Nested
    @DisplayName("Generate Headers")
    class GenerateHeaders {

        @Test
        @DisplayName("should include connection method header")
        void shouldIncludeConnectionMethodHeader() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(headers)
                .containsKey(FraudPreventionHeaders.Headers.CONNECTION_METHOD);
            assertThat(headers.get(FraudPreventionHeaders.Headers.CONNECTION_METHOD))
                .isEqualTo(FraudPreventionHeaders.CONNECTION_METHOD_DESKTOP_APP_DIRECT);
        }

        @Test
        @DisplayName("should include device ID header")
        void shouldIncludeDeviceIdHeader() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(headers)
                .containsKey(FraudPreventionHeaders.Headers.DEVICE_ID);
            assertThat(headers.get(FraudPreventionHeaders.Headers.DEVICE_ID))
                .isNotBlank();
        }

        @Test
        @DisplayName("should include user IDs header")
        void shouldIncludeUserIdsHeader() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(headers)
                .containsKey(FraudPreventionHeaders.Headers.USER_IDS);
            assertThat(headers.get(FraudPreventionHeaders.Headers.USER_IDS))
                .startsWith("os=");
        }

        @Test
        @DisplayName("should include timezone header")
        void shouldIncludeTimezoneHeader() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(headers)
                .containsKey(FraudPreventionHeaders.Headers.TIMEZONE);
            assertThat(headers.get(FraudPreventionHeaders.Headers.TIMEZONE))
                .matches("UTC[+-]\\d{2}:\\d{2}");
        }

        @Test
        @DisplayName("should include local IPs header")
        void shouldIncludeLocalIpsHeader() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(headers)
                .containsKey(FraudPreventionHeaders.Headers.LOCAL_IPS);
            // May be empty if no network interface is up
        }

        @Test
        @DisplayName("should include vendor version header")
        void shouldIncludeVendorVersionHeader() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(headers)
                .containsKey(FraudPreventionHeaders.Headers.VENDOR_VERSION);
            assertThat(headers.get(FraudPreventionHeaders.Headers.VENDOR_VERSION))
                .contains("SelfEmployment")
                .contains("1.0.0-TEST");
        }

        @Test
        @DisplayName("should include vendor product name header")
        void shouldIncludeVendorProductNameHeader() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(headers)
                .containsKey(FraudPreventionHeaders.Headers.VENDOR_PRODUCT_NAME);
            assertThat(headers.get(FraudPreventionHeaders.Headers.VENDOR_PRODUCT_NAME))
                .contains("SelfEmployment");
        }

        @Test
        @DisplayName("should include screens header")
        void shouldIncludeScreensHeader() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(headers)
                .containsKey(FraudPreventionHeaders.Headers.SCREENS);
            assertThat(headers.get(FraudPreventionHeaders.Headers.SCREENS))
                .contains("width=")
                .contains("height=");
        }

        @Test
        @DisplayName("should include window size header")
        void shouldIncludeWindowSizeHeader() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(headers)
                .containsKey(FraudPreventionHeaders.Headers.WINDOW_SIZE);
            assertThat(headers.get(FraudPreventionHeaders.Headers.WINDOW_SIZE))
                .contains("width=")
                .contains("height=");
        }

        @Test
        @DisplayName("should generate consistent device ID")
        void shouldGenerateConsistentDeviceId() {
            Map<String, String> headers1 = service.generateHeaders();
            Map<String, String> headers2 = service.generateHeaders();

            assertThat(headers1.get(FraudPreventionHeaders.Headers.DEVICE_ID))
                .isEqualTo(headers2.get(FraudPreventionHeaders.Headers.DEVICE_ID));
        }
    }

    @Nested
    @DisplayName("Validate Headers")
    class ValidateHeaders {

        @Test
        @DisplayName("should validate complete headers")
        void shouldValidateCompleteHeaders() {
            Map<String, String> headers = service.generateHeaders();

            assertThat(service.validateHeaders(headers)).isTrue();
        }

        @Test
        @DisplayName("should fail validation for missing mandatory header")
        void shouldFailValidationForMissingMandatoryHeader() {
            Map<String, String> headers = service.generateHeaders();
            headers.remove(FraudPreventionHeaders.Headers.DEVICE_ID);

            assertThat(service.validateHeaders(headers)).isFalse();
        }

        @Test
        @DisplayName("should fail validation for empty mandatory header")
        void shouldFailValidationForEmptyMandatoryHeader() {
            Map<String, String> headers = service.generateHeaders();
            headers.put(FraudPreventionHeaders.Headers.DEVICE_ID, "");

            assertThat(service.validateHeaders(headers)).isFalse();
        }
    }
}
