package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Response shape for HMRC Obligations API v3 — income and expenditure.
 *
 * <p>Endpoint: {@code GET /obligations/details/{nino}/income-and-expenditure}.
 *
 * <p>Returns the MTD-quarterly obligations the customer has been issued by HMRC,
 * each with a period (from/to) and a due date. Under the v3 cadence (deployed
 * 2026-03-24), due dates fall on the 7th of the month following quarter end.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/obligations-api">
 *     HMRC Obligations API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ObligationsResponse(
    @JsonProperty("obligations") List<ObligationGroup> obligations
) {

    /**
     * Obligations grouped by income source (e.g. one self-employment business or
     * one property business).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObligationGroup(
        @JsonProperty("incomeSourceType") String incomeSourceType,
        @JsonProperty("incomeSourceId") String incomeSourceId,
        @JsonProperty("obligationDetails") List<ObligationDetail> obligationDetails
    ) {
    }

    /**
     * A single obligation — one quarterly update (or annual EOPS-style obligation).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObligationDetail(
        @JsonProperty("periodStartDate") LocalDate periodStartDate,
        @JsonProperty("periodEndDate") LocalDate periodEndDate,
        @JsonProperty("dueDate") LocalDate dueDate,
        @JsonProperty("receivedDate") LocalDate receivedDate,
        @JsonProperty("status") String status,
        @JsonProperty("periodKey") String periodKey
    ) {
        /** @return {@code true} when the obligation has been fulfilled (received by HMRC). */
        public boolean isFulfilled() {
            return "Fulfilled".equalsIgnoreCase(status) || receivedDate != null;
        }

        /** @return {@code true} when the obligation is still open (not yet received). */
        public boolean isOpen() {
            return "Open".equalsIgnoreCase(status) && receivedDate == null;
        }
    }

    /** Convenience accessor returning an empty list if {@code obligations} is {@code null}. */
    public List<ObligationGroup> obligationsOrEmpty() {
        return obligations != null ? obligations : List.of();
    }
}
