package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.IndividualLossesResponse;
import uk.selfemploy.hmrc.client.dto.IndividualLossesResponse.BroughtForwardLoss;

import jakarta.ws.rs.Path;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract + DTO tests for IndividualLossesClient (HMRC Individual Losses
 * API v6).
 *
 * <p>Locks v6 as the production-targeted Accept header (v7 is sandbox-only as
 * of 2026-03) and exercises a representative list-brought-forward-losses
 * payload covering self-employment, UK-property, and foreign-property loss
 * types.
 */
@DisplayName("IndividualLossesClient (HMRC Individual Losses API v6)")
class IndividualLossesClientTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /individuals/losses")
        void pathIsCorrect() {
            Path path = IndividualLossesClient.class.getAnnotation(Path.class);
            assertThat(path).isNotNull();
            assertThat(path.value()).isEqualTo("/individuals/losses");
        }

        @Test
        @DisplayName("Accept header declares HMRC v6 (v7 is sandbox-only)")
        void acceptHeaderIsV6() {
            ClientHeaderParam header = IndividualLossesClient.class
                .getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.name()).isEqualTo("Accept");
            assertThat(header.value())
                .as("Individual Losses API v6 is the latest production version; "
                    + "v7 has not yet been promoted out of HMRC sandbox.")
                .containsExactly("application/vnd.hmrc.6.0+json");
        }
    }

    @Nested
    @DisplayName("v6 fixture deserialization")
    class V6Fixture {

        @Test
        @DisplayName("loads all three loss entries across loss types")
        void loadsAllLossEntries() throws Exception {
            IndividualLossesResponse response = loadFixture();

            assertThat(response.lossesOrEmpty()).hasSize(3);
            assertThat(response.lossesOrEmpty())
                .extracting(BroughtForwardLoss::lossType)
                .containsExactly("self-employment", "uk-property-non-fhl", "foreign-property");
        }

        @Test
        @DisplayName("loads loss amounts and tax-year-claimed-for accurately")
        void loadsLossAmounts() throws Exception {
            BroughtForwardLoss first = loadFixture().lossesOrEmpty().get(0);

            assertThat(first.lossId()).isEqualTo("AAZZ1234567890a");
            assertThat(first.incomeSourceId()).isEqualTo("XAIS12345678901");
            assertThat(first.taxYearClaimedFor()).isEqualTo("2026-27");
            assertThat(first.lossAmount()).isEqualByComparingTo("2750.55");
        }

        @Test
        @DisplayName("unknown HMRC fields tolerated at top level (forward-compat)")
        void unknownFieldsTolerated() throws Exception {
            // Fixture contains "newSchemaFieldFromHmrc" at top level — successful load
            // proves @JsonIgnoreProperties is wired.
            IndividualLossesResponse response = loadFixture();
            assertThat(response).isNotNull();
            assertThat(response.lossesOrEmpty()).isNotEmpty();
        }

        private IndividualLossesResponse loadFixture() throws Exception {
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("hmrc-sandbox/individual-losses-v6-response.json")) {
                assertThat(is)
                    .as("fixture individual-losses-v6-response.json missing on classpath")
                    .isNotNull();
                return mapper.readValue(is, IndividualLossesResponse.class);
            }
        }
    }
}
