package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.BusinessDetailsV2Response;
import uk.selfemploy.hmrc.client.dto.BusinessDetailsV2Response.IncomeSource;

import jakarta.ws.rs.Path;

import java.io.InputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract + DTO tests for the new BusinessDetailsClient (Business Details API v2).
 *
 * <p>Two test groups:
 * <ul>
 *   <li>Interface annotation contract — asserts the per-endpoint Accept header and
 *       path stay locked to HMRC's published v2 spec. If a future change downgrades
 *       to a retired version, the build fails immediately.</li>
 *   <li>DTO deserialization — loads the sandbox v2 response fixture and asserts the
 *       shape that downstream services rely on.</li>
 * </ul>
 *
 * <p>Full HTTP round-trip is covered by the WireMock integration tests.
 */
@DisplayName("BusinessDetailsClient (Business Details API v2)")
class BusinessDetailsClientTest {

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /individuals/business/details")
        void pathIsCorrect() {
            Path path = BusinessDetailsClient.class.getAnnotation(Path.class);
            assertThat(path)
                .as("@Path missing on BusinessDetailsClient")
                .isNotNull();
            assertThat(path.value()).isEqualTo("/individuals/business/details");
        }

        @Test
        @DisplayName("Accept header declares HMRC v2")
        void acceptHeaderIsV2() {
            ClientHeaderParam header = BusinessDetailsClient.class.getAnnotation(ClientHeaderParam.class);
            assertThat(header)
                .as("@ClientHeaderParam missing on BusinessDetailsClient — Accept header would default and HMRC would reject v2+ calls")
                .isNotNull();
            assertThat(header.name()).isEqualTo("Accept");
            assertThat(header.value())
                .as("BusinessDetailsClient must declare Business Details API v2 — v1 was retired in production 2026-03-24")
                .containsExactly("application/vnd.hmrc.2.0+json");
        }
    }

    @Nested
    @DisplayName("BusinessDetailsV2Response DTO")
    class DtoDeserialization {

        private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

        @Test
        @DisplayName("deserializes sandbox v2 response with one business + one property")
        void deserializesSandboxResponse() throws Exception {
            BusinessDetailsV2Response response = loadFixture();

            assertThat(response.nino()).isEqualTo("AA000001A");
            assertThat(response.mtdbsa()).isEqualTo("XQIT00000000001");
            assertThat(response.businessesOrEmpty()).hasSize(1);
            assertThat(response.propertiesOrEmpty()).hasSize(1);
        }

        @Test
        @DisplayName("self-employment income source has v2 field shape including incomeSourceId")
        void selfEmploymentFields() throws Exception {
            IncomeSource business = loadFixture().businessesOrEmpty().get(0);

            assertThat(business.incomeSourceId()).isEqualTo("XAIS12345678901");
            assertThat(business.incomeSourceType()).isEqualTo("self-employment");
            assertThat(business.tradingName()).isEqualTo("Test Business Ltd");
            assertThat(business.accountingPeriodStartDate()).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(business.accountingPeriodEndDate()).isEqualTo(LocalDate.of(2026, 4, 5));
            assertThat(business.businessAddressPostcode()).isEqualTo("AB12 3CD");
            assertThat(business.isActive()).isTrue();
        }

        @Test
        @DisplayName("property income source has type uk-property and active = true when cessationDate is null")
        void propertyFields() throws Exception {
            IncomeSource property = loadFixture().propertiesOrEmpty().get(0);

            assertThat(property.incomeSourceId()).isEqualTo("XPIS98765432101");
            assertThat(property.incomeSourceType()).isEqualTo("uk-property");
            assertThat(property.tradingName()).isNull();
            assertThat(property.isActive()).isTrue();
        }

        @Test
        @DisplayName("unknown fields in the JSON are ignored (forward-compat with HMRC schema additions)")
        void unknownFieldsAreIgnored() throws Exception {
            String jsonWithUnknownField = """
                {
                  "nino": "AA000001A",
                  "mtdbsa": "XQIT00000000001",
                  "businessData": [],
                  "propertyData": [],
                  "someFutureFieldAddedByHmrc": "this should not break deserialisation"
                }
                """;

            BusinessDetailsV2Response response = mapper.readValue(jsonWithUnknownField, BusinessDetailsV2Response.class);

            assertThat(response.nino()).isEqualTo("AA000001A");
            assertThat(response.businessesOrEmpty()).isEmpty();
        }

        private BusinessDetailsV2Response loadFixture() throws Exception {
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("hmrc-sandbox/business-details-v2-response.json")) {
                assertThat(is).as("fixture business-details-v2-response.json missing on classpath").isNotNull();
                return mapper.readValue(is, BusinessDetailsV2Response.class);
            }
        }
    }
}
