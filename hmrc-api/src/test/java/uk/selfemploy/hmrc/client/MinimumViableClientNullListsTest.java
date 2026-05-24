package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.BsasResponse;
import uk.selfemploy.hmrc.client.dto.CapitalGainsDisposalsResponse;
import uk.selfemploy.hmrc.client.dto.IndividualLossesResponse;
import uk.selfemploy.hmrc.client.dto.PropertyBusinessResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural contract on the {@code *OrEmpty()} convenience accessors of the
 * minimum-viable response DTOs: deserialising an empty HMRC response (no list
 * fields present) MUST yield empty collections, never throw NPE in caller code
 * that walks the list.
 *
 * <p>HMRC frequently returns a 200 with an empty body for "valid request, no data
 * for this customer this year". Callers that iterate the response without the
 * accessor would NPE; the accessors are the defensive surface.
 */
@DisplayName("Minimum-viable client DTOs — null-list collapse to empty")
class MinimumViableClientNullListsTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("IndividualLossesResponse with no list returns an empty list, not null")
    void individualLossesNullListCollapsesToEmpty() throws Exception {
        IndividualLossesResponse parsed = mapper.readValue("{}", IndividualLossesResponse.class);
        assertThat(parsed.lossesOrEmpty()).isEmpty();
    }

    @Test
    @DisplayName("PropertyBusinessResponse with no list returns an empty list, not null")
    void propertyBusinessNullListCollapsesToEmpty() throws Exception {
        PropertyBusinessResponse parsed = mapper.readValue("{}", PropertyBusinessResponse.class);
        assertThat(parsed.incomeSourcesOrEmpty()).isEmpty();
    }

    @Test
    @DisplayName("CapitalGainsDisposalsResponse with no list returns an empty list, not null")
    void capitalGainsNullListCollapsesToEmpty() throws Exception {
        CapitalGainsDisposalsResponse parsed = mapper.readValue("{}", CapitalGainsDisposalsResponse.class);
        assertThat(parsed.disposalsOrEmpty()).isEmpty();
    }

    @Test
    @DisplayName("BsasResponse deserialises an empty payload without throwing")
    void bsasResponseEmptyPayloadDoesNotThrow() throws Exception {
        // BSAS has no list-bearing top-level field, so the relevant contract here
        // is that an empty payload deserialises cleanly. Nested records may be null,
        // which is the documented "no data this period" shape.
        BsasResponse parsed = mapper.readValue("{}", BsasResponse.class);
        assertThat(parsed).isNotNull();
        assertThat(parsed.metadata()).isNull();
        assertThat(parsed.accountingPeriod()).isNull();
        assertThat(parsed.adjustments()).isNull();
    }
}
