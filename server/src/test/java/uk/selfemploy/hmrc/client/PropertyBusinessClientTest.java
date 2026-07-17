package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.PropertyBusinessResponse;
import uk.selfemploy.hmrc.client.dto.PropertyBusinessResponse.IncomeSource;

import jakarta.ws.rs.Path;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract + DTO tests for PropertyBusinessClient (HMRC Property Business
 * API v6).
 *
 * <p>Locks the v6 Accept header and the per-property foreign income-source
 * shape introduced in HMRC's 2026-03-24 production deployment (one record per
 * foreign property, each carrying its own {@code countryCode} and
 * accounting-period).
 */
@DisplayName("PropertyBusinessClient (HMRC Property Business API v6)")
class PropertyBusinessClientTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /individuals/business/property")
        void pathIsCorrect() {
            Path path = PropertyBusinessClient.class.getAnnotation(Path.class);
            assertThat(path).isNotNull();
            assertThat(path.value()).isEqualTo("/individuals/business/property");
        }

        @Test
        @DisplayName("Accept header declares HMRC v6")
        void acceptHeaderIsV6() {
            ClientHeaderParam header = PropertyBusinessClient.class
                .getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.name()).isEqualTo("Accept");
            assertThat(header.value())
                .as("Property Business API v6 — per-property foreign income-source "
                    + "shape was introduced 2026-03-24.")
                .containsExactly("application/vnd.hmrc.6.0+json");
        }
    }

    @Nested
    @DisplayName("v6 fixture deserialization")
    class V6Fixture {

        @Test
        @DisplayName("loads all three income-sources (one UK, two foreign)")
        void loadsAllIncomeSources() throws Exception {
            PropertyBusinessResponse response = loadFixture();

            assertThat(response.incomeSourcesOrEmpty()).hasSize(3);
            assertThat(response.incomeSourcesOrEmpty())
                .extracting(IncomeSource::incomeSourceType)
                .containsExactly("uk-property", "foreign-property", "foreign-property");
        }

        @Test
        @DisplayName("foreign properties are modelled per-property with distinct ids and countryCodes")
        void foreignPropertiesArePerProperty() throws Exception {
            List<IncomeSource> sources = loadFixture().incomeSourcesOrEmpty();

            List<IncomeSource> foreign = sources.stream().filter(IncomeSource::isForeign).toList();
            assertThat(foreign)
                .as("v6 foreign-property shape: one record per property — not aggregated.")
                .hasSize(2);

            assertThat(foreign).extracting(IncomeSource::incomeSourceId)
                .containsExactly("XFPI11122233344", "XFPI55566677788");
            assertThat(foreign).extracting(IncomeSource::countryCode)
                .containsExactly("ESP", "FRA");
        }

        @Test
        @DisplayName("UK property income-source has no countryCode and exposes UK postcode")
        void ukPropertyShape() throws Exception {
            IncomeSource uk = loadFixture().incomeSourcesOrEmpty().get(0);

            assertThat(uk.isForeign()).isFalse();
            assertThat(uk.countryCode()).isNull();
            assertThat(uk.address().postcode()).isEqualTo("M14 5TQ");
            assertThat(uk.accountingPeriod().startDate()).isEqualTo(LocalDate.of(2026, 4, 6));
            assertThat(uk.accountingPeriod().endDate()).isEqualTo(LocalDate.of(2027, 4, 5));
        }

        @Test
        @DisplayName("unknown HMRC fields tolerated at top level (forward-compat)")
        void unknownFieldsTolerated() throws Exception {
            PropertyBusinessResponse response = loadFixture();
            assertThat(response).isNotNull();
            assertThat(response.incomeSourcesOrEmpty()).isNotEmpty();
        }

        private PropertyBusinessResponse loadFixture() throws Exception {
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("hmrc-sandbox/property-business-v6-response.json")) {
                assertThat(is)
                    .as("fixture property-business-v6-response.json missing on classpath")
                    .isNotNull();
                return mapper.readValue(is, PropertyBusinessResponse.class);
            }
        }
    }
}
