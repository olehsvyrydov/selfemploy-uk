package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.CapitalGainsDisposalsResponse;
import uk.selfemploy.hmrc.client.dto.CapitalGainsDisposalsResponse.Disposal;

import jakarta.ws.rs.Path;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract + DTO tests for CapitalGainsClient (HMRC Individuals Disposals
 * API v3).
 *
 * <p>Locks the v3 Accept header and exercises a representative payload
 * containing both UK residential property and crypto-asset disposals — the
 * crypto category being newly supported by v3 (HMRC Cryptoassets Manual
 * CRYPTO20250).
 */
@DisplayName("CapitalGainsClient (HMRC Individuals Disposals API v3)")
class CapitalGainsClientTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /individuals/disposals/residential-property")
        void pathIsCorrect() {
            Path path = CapitalGainsClient.class.getAnnotation(Path.class);
            assertThat(path).isNotNull();
            assertThat(path.value()).isEqualTo("/individuals/disposals/residential-property");
        }

        @Test
        @DisplayName("Accept header declares HMRC v3")
        void acceptHeaderIsV3() {
            ClientHeaderParam header = CapitalGainsClient.class
                .getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.name()).isEqualTo("Accept");
            assertThat(header.value())
                .as("Individuals Disposals API v3 — crypto-asset disposals "
                    + "supported from this version.")
                .containsExactly("application/vnd.hmrc.3.0+json");
        }
    }

    @Nested
    @DisplayName("v3 fixture deserialization")
    class V3Fixture {

        @Test
        @DisplayName("loads all three disposals across asset types")
        void loadsAllDisposals() throws Exception {
            CapitalGainsDisposalsResponse response = loadFixture();

            assertThat(response.taxYear()).isEqualTo("2026-27");
            assertThat(response.disposalsOrEmpty()).hasSize(3);
            assertThat(response.disposalsOrEmpty())
                .extracting(Disposal::assetType)
                .containsExactly("residential-property", "crypto-asset", "crypto-asset");
        }

        @Test
        @DisplayName("crypto disposals are correctly recognised")
        void cryptoDisposalsAreRecognised() throws Exception {
            List<Disposal> crypto = loadFixture().disposalsOrEmpty().stream()
                .filter(Disposal::isCryptoAsset)
                .toList();

            assertThat(crypto).hasSize(2);
            assertThat(crypto).extracting(Disposal::assetDescription)
                .containsExactly("BTC", "ETH");
        }

        @Test
        @DisplayName("residential disposal carries gain; loss-making crypto carries loss")
        void gainAndLossFigures() throws Exception {
            List<Disposal> all = loadFixture().disposalsOrEmpty();

            Disposal residential = all.get(0);
            assertThat(residential.isCryptoAsset()).isFalse();
            assertThat(residential.gain()).isEqualByComparingTo("45000.00");
            assertThat(residential.loss()).isEqualByComparingTo("0.00");

            Disposal lossMaking = all.get(2);
            assertThat(lossMaking.gain()).isEqualByComparingTo("0.00");
            assertThat(lossMaking.loss()).isEqualByComparingTo("700.00");
        }

        @Test
        @DisplayName("unknown HMRC fields tolerated at top level (forward-compat)")
        void unknownFieldsTolerated() throws Exception {
            CapitalGainsDisposalsResponse response = loadFixture();
            assertThat(response).isNotNull();
            assertThat(response.disposalsOrEmpty()).isNotEmpty();
        }

        private CapitalGainsDisposalsResponse loadFixture() throws Exception {
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("hmrc-sandbox/capital-gains-v3-response.json")) {
                assertThat(is)
                    .as("fixture capital-gains-v3-response.json missing on classpath")
                    .isNotNull();
                return mapper.readValue(is, CapitalGainsDisposalsResponse.class);
            }
        }
    }
}
