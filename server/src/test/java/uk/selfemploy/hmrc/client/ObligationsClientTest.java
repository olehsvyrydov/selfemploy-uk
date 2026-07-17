package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.ObligationsResponse;
import uk.selfemploy.hmrc.client.dto.ObligationsResponse.ObligationDetail;
import uk.selfemploy.hmrc.client.dto.ObligationsResponse.ObligationGroup;

import jakarta.ws.rs.Path;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract + DTO tests for ObligationsClient (HMRC Obligations API v3).
 *
 * <p>Locks the v3 Accept header and the 7th-of-month due-date cadence introduced
 * in HMRC's 2026-03-24 production deployment.
 */
@DisplayName("ObligationsClient (HMRC Obligations API v3)")
class ObligationsClientTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /obligations/details")
        void pathIsCorrect() {
            Path path = ObligationsClient.class.getAnnotation(Path.class);
            assertThat(path).isNotNull();
            assertThat(path.value()).isEqualTo("/obligations/details");
        }

        @Test
        @DisplayName("Accept header declares HMRC v3")
        void acceptHeaderIsV3() {
            ClientHeaderParam header = ObligationsClient.class.getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.name()).isEqualTo("Accept");
            assertThat(header.value())
                .as("ObligationsClient must declare Obligations API v3 — v2 was retired in production 2026-03-24")
                .containsExactly("application/vnd.hmrc.3.0+json");
        }
    }

    @Nested
    @DisplayName("v3 fixture deserialization")
    class V3Fixture {

        @Test
        @DisplayName("loads obligation groups grouped by income source")
        void groupingByIncomeSource() throws Exception {
            ObligationsResponse response = loadFixture();

            assertThat(response.obligationsOrEmpty()).hasSize(2);
            assertThat(response.obligationsOrEmpty())
                .extracting(ObligationGroup::incomeSourceType)
                .containsExactly("self-employment", "uk-property");
        }

        @Test
        @DisplayName("all four 2026/27 self-employment due dates fall on the 7th (Obligations v3 cadence)")
        void quarterlyDueDatesAreOn7th() throws Exception {
            ObligationGroup selfEmployment = loadFixture().obligationsOrEmpty().get(0);

            List<ObligationDetail> quarters = selfEmployment.obligationDetails();
            assertThat(quarters).hasSize(4);

            assertThat(quarters.get(0).dueDate()).isEqualTo(LocalDate.of(2026, 8, 7));   // Q1
            assertThat(quarters.get(1).dueDate()).isEqualTo(LocalDate.of(2026, 11, 7));  // Q2
            assertThat(quarters.get(2).dueDate()).isEqualTo(LocalDate.of(2027, 2, 7));   // Q3
            assertThat(quarters.get(3).dueDate()).isEqualTo(LocalDate.of(2027, 5, 7));   // Q4

            assertThat(quarters).allMatch(q -> q.dueDate().getDayOfMonth() == 7,
                "Obligations API v3 due dates must fall on day 7 — never day 5");
        }

        @Test
        @DisplayName("Open status with no receivedDate is correctly recognised")
        void openObligation() throws Exception {
            ObligationDetail q1 = loadFixture().obligationsOrEmpty().get(0).obligationDetails().get(0);

            assertThat(q1.status()).isEqualTo("Open");
            assertThat(q1.receivedDate()).isNull();
            assertThat(q1.isOpen()).isTrue();
            assertThat(q1.isFulfilled()).isFalse();
        }

        @Test
        @DisplayName("Fulfilled status with receivedDate is correctly recognised")
        void fulfilledObligation() throws Exception {
            ObligationDetail received = loadFixture().obligationsOrEmpty().get(1).obligationDetails().get(0);

            assertThat(received.status()).isEqualTo("Fulfilled");
            assertThat(received.receivedDate()).isEqualTo(LocalDate.of(2025, 7, 30));
            assertThat(received.isFulfilled()).isTrue();
            assertThat(received.isOpen()).isFalse();
        }

        @Test
        @DisplayName("unknown HMRC fields tolerated at every level (forward-compat)")
        void unknownFieldsTolerated() throws Exception {
            // Fixture contains "newSchemaFieldFromHmrc" at top level — successful load
            // proves @JsonIgnoreProperties is wired.
            ObligationsResponse response = loadFixture();
            assertThat(response).isNotNull();
            assertThat(response.obligationsOrEmpty()).isNotEmpty();
        }

        private ObligationsResponse loadFixture() throws Exception {
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("hmrc-sandbox/obligations-v3-response.json")) {
                assertThat(is)
                    .as("fixture obligations-v3-response.json missing on classpath")
                    .isNotNull();
                return mapper.readValue(is, ObligationsResponse.class);
            }
        }
    }
}
