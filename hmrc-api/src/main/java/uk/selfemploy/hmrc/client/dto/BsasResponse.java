package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Response shape for HMRC BSAS API v7 — retrieve self-employment adjustable
 * summary.
 *
 * <p>Endpoint:
 * {@code GET /individuals/self-assessment/adjustable-summary/{nino}/self-employment/{bsasId}}.
 *
 * <p>Models <strong>only fields BSAS itself contributes</strong>: the BSAS
 * identifier, accounting period, summary calculation timestamp, and the
 * adjustments overlay. The underlying income / expenses figures that feed into
 * BSAS belong to the Self-Employment Business API and are not duplicated here.
 *
 * <p>v7 (production 2026-03-24) <strong>removed {@code averagingAdjustment}
 * from the adjustments payload</strong>. The {@link Adjustments} record has no
 * component for it; future contributors cannot accidentally restore it without
 * adding it back to the type and breaking the contract test that asserts its
 * absence.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-assessment-bsas-api">
 *     HMRC BSAS API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BsasResponse(
    @JsonProperty("metadata") Metadata metadata,
    @JsonProperty("accountingPeriod") AccountingPeriod accountingPeriod,
    @JsonProperty("adjustments") Adjustments adjustments
) {

    /** Identifying and provenance fields for the BSAS calculation. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
        @JsonProperty("bsasId") String bsasId,
        @JsonProperty("incomeSourceId") String incomeSourceId,
        @JsonProperty("taxYear") String taxYear,
        @JsonProperty("requestedDateTime") OffsetDateTime requestedDateTime,
        @JsonProperty("summaryStatus") String summaryStatus
    ) {
    }

    /** Accounting period the BSAS covers. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountingPeriod(
        @JsonProperty("startDate") LocalDate startDate,
        @JsonProperty("endDate") LocalDate endDate
    ) {
    }

    /**
     * Customer overlay adjustments on top of the auto-aggregated quarterly
     * totals. Each field is optional; omitted fields mean "no adjustment".
     *
     * <p>NOTE: {@code averagingAdjustment} is intentionally absent — it was
     * removed in BSAS v7 (production 2026-03-24).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Adjustments(
        @JsonProperty("incomeAdjustment") BigDecimal incomeAdjustment,
        @JsonProperty("expensesAdjustment") BigDecimal expensesAdjustment,
        @JsonProperty("additionsAdjustment") BigDecimal additionsAdjustment,
        @JsonProperty("deductionsAdjustment") BigDecimal deductionsAdjustment
    ) {
    }
}
