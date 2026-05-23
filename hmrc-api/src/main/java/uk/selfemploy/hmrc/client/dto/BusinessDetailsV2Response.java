package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Response shape for HMRC Business Details API v2.
 *
 * <p>Endpoint: {@code GET /individuals/business/details/{nino}}.
 *
 * <p>Returns the customer's MTD-registered income sources: self-employment businesses
 * in {@code businessData} and property businesses (UK / foreign) in {@code propertyData}.
 * Either array may be empty if the customer has no registered sources of that type.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/business-details-api">
 *     HMRC Business Details API</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessDetailsV2Response(
    @JsonProperty("nino") String nino,
    @JsonProperty("mtdbsa") String mtdbsa,
    @JsonProperty("businessData") List<IncomeSource> businessData,
    @JsonProperty("propertyData") List<IncomeSource> propertyData
) {

    /**
     * One MTD-registered income source — either a self-employment business or a
     * property business. {@code incomeSourceId} is what other MTD ITSA APIs call
     * {@code businessId}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IncomeSource(
        @JsonProperty("incomeSourceId") String incomeSourceId,
        @JsonProperty("incomeSourceType") String incomeSourceType,
        @JsonProperty("tradingName") String tradingName,
        @JsonProperty("tradingStartDate") LocalDate tradingStartDate,
        @JsonProperty("cessationDate") LocalDate cessationDate,
        @JsonProperty("accountingPeriodStartDate") LocalDate accountingPeriodStartDate,
        @JsonProperty("accountingPeriodEndDate") LocalDate accountingPeriodEndDate,
        @JsonProperty("firstAccountingPeriodStartDate") LocalDate firstAccountingPeriodStartDate,
        @JsonProperty("firstAccountingPeriodEndDate") LocalDate firstAccountingPeriodEndDate,
        @JsonProperty("businessAddressLineOne") String businessAddressLineOne,
        @JsonProperty("businessAddressLineTwo") String businessAddressLineTwo,
        @JsonProperty("businessAddressLineThree") String businessAddressLineThree,
        @JsonProperty("businessAddressLineFour") String businessAddressLineFour,
        @JsonProperty("businessAddressPostcode") String businessAddressPostcode,
        @JsonProperty("businessAddressCountryCode") String businessAddressCountryCode
    ) {
        /**
         * @return {@code true} if this income source has not been ceased.
         */
        public boolean isActive() {
            return cessationDate == null;
        }
    }

    /**
     * Convenience accessor returning an empty list if {@code businessData} is {@code null}.
     */
    public List<IncomeSource> businessesOrEmpty() {
        return businessData != null ? businessData : List.of();
    }

    /**
     * Convenience accessor returning an empty list if {@code propertyData} is {@code null}.
     */
    public List<IncomeSource> propertiesOrEmpty() {
        return propertyData != null ? propertyData : List.of();
    }
}
