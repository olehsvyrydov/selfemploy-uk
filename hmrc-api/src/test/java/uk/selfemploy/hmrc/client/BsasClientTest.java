package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.BsasResponse;
import uk.selfemploy.hmrc.client.dto.BsasResponse.Adjustments;

import jakarta.ws.rs.Path;

import java.io.InputStream;
import java.lang.reflect.RecordComponent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract + DTO tests for BsasClient (HMRC BSAS API v7).
 *
 * <p>Locks the v7 Accept header and asserts that the removed
 * {@code averagingAdjustment} field cannot be expressed through this client —
 * even when present in an incoming payload it is silently dropped because the
 * {@link Adjustments} record has no component to bind it to.
 */
@DisplayName("BsasClient (HMRC BSAS API v7)")
class BsasClientTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /individuals/self-assessment/adjustable-summary")
        void pathIsCorrect() {
            Path path = BsasClient.class.getAnnotation(Path.class);
            assertThat(path).isNotNull();
            assertThat(path.value()).isEqualTo("/individuals/self-assessment/adjustable-summary");
        }

        @Test
        @DisplayName("Accept header declares HMRC v7")
        void acceptHeaderIsV7() {
            ClientHeaderParam header = BsasClient.class.getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.name()).isEqualTo("Accept");
            assertThat(header.value())
                .as("BSAS v7 — averagingAdjustment removed in production 2026-03-24.")
                .containsExactly("application/vnd.hmrc.7.0+json");
        }
    }

    @Nested
    @DisplayName("Adjustments DTO — removed-field surface")
    class RemovedFieldSurface {

        @Test
        @DisplayName("Adjustments record has NO component for averagingAdjustment")
        void noAveragingAdjustment() {
            for (RecordComponent c : Adjustments.class.getRecordComponents()) {
                assertThat(c.getName())
                    .as("Adjustments.averagingAdjustment must not exist — averaging "
                        + "relief was removed from BSAS v7 (2026-03-24); restoring "
                        + "this field would re-introduce a server-side rejected payload.")
                    .isNotEqualTo("averagingAdjustment");
            }
        }

        @Test
        @DisplayName("BsasResponse root and Metadata records have NO component for averagingAdjustment")
        void noAveragingAdjustmentAtRootOrMetadata() {
            // A future refactor could lift the removed field to a different level of the
            // response shape — guard the top-level and Metadata records too, mirroring
            // the recursive removed-field scan in ReliefsClientTest.
            for (Class<?> level : new Class<?>[]{
                uk.selfemploy.hmrc.client.dto.BsasResponse.class,
                uk.selfemploy.hmrc.client.dto.BsasResponse.Metadata.class,
                uk.selfemploy.hmrc.client.dto.BsasResponse.AccountingPeriod.class}) {
                for (RecordComponent c : level.getRecordComponents()) {
                    assertThat(c.getName())
                        .as("%s.averagingAdjustment must not exist at any level of the response", level.getSimpleName())
                        .isNotEqualTo("averagingAdjustment");
                }
            }
        }
    }

    @Nested
    @DisplayName("v7 fixture deserialization")
    class V7Fixture {

        @Test
        @DisplayName("loads BSAS metadata and accounting period")
        void loadsMetadata() throws Exception {
            BsasResponse response = loadFixture();

            assertThat(response.metadata()).isNotNull();
            assertThat(response.metadata().bsasId())
                .isEqualTo("c75f40a6-a3df-4429-a697-471eeb46ba0f");
            assertThat(response.metadata().taxYear()).isEqualTo("2026-27");
            assertThat(response.metadata().summaryStatus()).isEqualTo("valid");
            assertThat(response.accountingPeriod().startDate().toString()).isEqualTo("2026-04-06");
        }

        @Test
        @DisplayName("loads permitted adjustments accurately")
        void loadsAdjustments() throws Exception {
            Adjustments adjustments = loadFixture().adjustments();

            assertThat(adjustments.incomeAdjustment()).isEqualByComparingTo("1500.00");
            assertThat(adjustments.expensesAdjustment()).isEqualByComparingTo("-250.00");
            assertThat(adjustments.deductionsAdjustment()).isEqualByComparingTo("75.50");
        }

        @Test
        @DisplayName("legacy averagingAdjustment in fixture is silently dropped")
        void averagingAdjustmentIsDropped() throws Exception {
            // The fixture intentionally carries a populated averagingAdjustment.
            // Successful deserialization (no exception) plus the absence of a
            // getter on the DTO proves no surface area exists to expose it.
            BsasResponse response = loadFixture();
            assertThat(response).isNotNull();
            assertThat(response.adjustments()).isNotNull();
        }

        private BsasResponse loadFixture() throws Exception {
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("hmrc-sandbox/bsas-v7-response.json")) {
                assertThat(is)
                    .as("fixture bsas-v7-response.json missing on classpath")
                    .isNotNull();
                return mapper.readValue(is, BsasResponse.class);
            }
        }
    }
}
