package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the shared fraud-prevention applier emits the full mandatory {@code DESKTOP_APP_DIRECT}
 * header set. Every HMRC submission path routes through {@link HmrcFraudHeaders#apply}, so this locks
 * in that none can regress to the hand-rolled reduced subset (empty user IDs, a hard-coded vendor
 * version, and no device/MAC/user-agent/timestamp headers) that HMRC would reject.
 */
@DisplayName("HmrcFraudHeaders - mandatory header set")
class HmrcFraudHeadersTest {

    @Test
    @DisplayName("applies the full mandatory Gov-Client/Gov-Vendor fraud-prevention set")
    void appliesFullMandatorySet() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("https://test-api.service.hmrc.gov.uk/x"))
                .GET();

        HmrcFraudHeaders.apply(builder);
        HttpRequest request = builder.build();

        // Includes the headers the previous hand-rolled quarterly subset omitted or emptied (device id,
        // user IDs, user agent, local-IP timestamp). Gov-Client-MAC-Addresses, Screens and Window-Size
        // are environment-dependent (a runner with no readable NIC hardware address / no display omits
        // them) and are intentionally not asserted so the check is stable in a headless CI.
        List<String> mandatory = List.of(
                "Gov-Client-Connection-Method",
                "Gov-Client-Device-ID",
                "Gov-Client-User-IDs",
                "Gov-Client-Timezone",
                "Gov-Client-Local-IPs",
                "Gov-Client-Local-IPs-Timestamp",
                "Gov-Client-User-Agent",
                "Gov-Vendor-Version",
                "Gov-Vendor-Product-Name");
        for (String header : mandatory) {
            assertThat(request.headers().firstValue(header))
                    .as("fraud-prevention header %s", header)
                    .isPresent();
        }

        // Vendor-Version tracks the real build metadata, not the old hard-coded "SelfEmployment=1.0",
        // and its value is percent-encoded per HMRC's fraud-header spec ("=" -> "%3D") — which the old
        // hand-rolled subset sent raw.
        assertThat(request.headers().firstValue("Gov-Vendor-Version"))
                .get().asString().startsWith("SelfEmployment%3D");
        assertThat(request.headers().firstValue("Gov-Client-Connection-Method"))
                .contains("DESKTOP_APP_DIRECT");
    }
}
