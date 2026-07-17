package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.Reliefs;

import jakarta.ws.rs.Path;

import java.io.InputStream;
import java.lang.reflect.RecordComponent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract + DTO tests for ReliefsClient (HMRC Individuals Reliefs API v3).
 *
 * <p>Locks the v3 Accept header and the "no non-UK charitable giving fields"
 * removal: the {@link Reliefs} record has no component for any of the removed
 * legacy field names, so a future contributor cannot accidentally re-introduce
 * non-UK relief on the wire.
 */
@DisplayName("ReliefsClient (HMRC Individuals Reliefs API v3)")
class ReliefsClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /individuals/reliefs")
        void pathIsCorrect() {
            Path path = ReliefsClient.class.getAnnotation(Path.class);
            assertThat(path).isNotNull();
            assertThat(path.value()).isEqualTo("/individuals/reliefs");
        }

        @Test
        @DisplayName("Accept header declares HMRC v3")
        void acceptHeaderIsV3() {
            ClientHeaderParam header = ReliefsClient.class.getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.value())
                .as("Reliefs API v3 — v2 was retired in production 2026-03-24")
                .containsExactly("application/vnd.hmrc.3.0+json");
        }
    }

    @Nested
    @DisplayName("Reliefs DTO — UK-only field surface")
    class UkOnlyFields {

        @Test
        @DisplayName("Reliefs record has NO component for any removed non-UK field name")
        void noNonUkFields() {
            String[] removedNames = {
                "nonUkCharities",
                "nonUkCharitiesCharityNames",
                "giftsToOverseasCharities",
                "sharesOrSecuritiesGiftedToNonUkCharities",
                "landAndBuildingsGiftedToNonUkCharities",
                "investmentsNonUkCharities",
                "overseasCharitiesNamed"
            };

            for (RecordComponent c : Reliefs.class.getRecordComponents()) {
                assertThat(removedNames)
                    .as("Reliefs.%s must not exist — non-UK charitable giving fields "
                        + "were removed in HMRC Reliefs v3 (2026-03-24).", c.getName())
                    .doesNotContain(c.getName());
            }
            for (RecordComponent c : Reliefs.Gifts.class.getRecordComponents()) {
                assertThat(removedNames)
                    .as("Reliefs.Gifts.%s must not exist (non-UK removed in v3)", c.getName())
                    .doesNotContain(c.getName());
            }
        }

        @Test
        @DisplayName("Reliefs.Gifts components are exactly the three UK-permitted fields")
        void giftsHasExactlyThreeUkFields() {
            String[] componentNames = java.util.Arrays.stream(Reliefs.Gifts.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toArray(String[]::new);

            assertThat(componentNames).containsExactlyInAnyOrder(
                "investmentsAmount",
                "landAndBuildings",
                "sharesOrSecurities");
        }
    }

    @Nested
    @DisplayName("v3 fixture deserialization")
    class V3Fixture {

        @Test
        @DisplayName("loads UK Gift Aid figures from sandbox v3 fixture")
        void loadsGiftAid() throws Exception {
            Reliefs reliefs = loadFixture();
            assertThat(reliefs.giftAidPayments()).isNotNull();
            assertThat(reliefs.giftAidPayments().currentYearAmount()).isEqualByComparingTo("1200.00");
            assertThat(reliefs.giftAidPayments().oneOffCurrentYearAmount()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("loads UK Gifts (investments) from sandbox v3 fixture")
        void loadsGifts() throws Exception {
            Reliefs.Gifts gifts = loadFixture().gifts();
            assertThat(gifts).isNotNull();
            assertThat(gifts.investmentsAmount()).isEqualByComparingTo("500.00");
            assertThat(gifts.landAndBuildings()).isEqualByComparingTo("0.00");
            assertThat(gifts.sharesOrSecurities()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("legacy non-UK fields in the fixture are silently dropped (forward-compat + "
            + "back-compat with stored legacy data)")
        void legacyNonUkFieldsAreIgnored() throws Exception {
            // The fixture intentionally contains nonUkCharitiesCharityNames and
            // giftsToOverseasCharities. Successful deserialization (no exception) proves
            // @JsonIgnoreProperties is wired AND the Reliefs record has no constructor
            // parameter accepting them.
            Reliefs reliefs = loadFixture();
            assertThat(reliefs).isNotNull();
            // Reliefs DTO exposes no API surface for non-UK fields — there is no getter
            // to assert against, which is precisely the security property we want.
        }

        private Reliefs loadFixture() throws Exception {
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("hmrc-sandbox/reliefs-v3-response.json")) {
                assertThat(is)
                    .as("fixture reliefs-v3-response.json missing on classpath")
                    .isNotNull();
                return mapper.readValue(is, Reliefs.class);
            }
        }
    }
}
