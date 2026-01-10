package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Business details from HMRC Self-Employment API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusinessDetails(
    @JsonProperty("businessId") String businessId,
    @JsonProperty("typeOfBusiness") String typeOfBusiness,
    @JsonProperty("tradingName") String tradingName,
    @JsonProperty("tradingStartDate") LocalDate tradingStartDate,
    @JsonProperty("accountingPeriods") List<AccountingPeriod> accountingPeriods,
    @JsonProperty("cessationDate") LocalDate cessationDate,
    @JsonProperty("addressLineOne") String addressLineOne,
    @JsonProperty("addressLineTwo") String addressLineTwo,
    @JsonProperty("addressLineThree") String addressLineThree,
    @JsonProperty("addressLineFour") String addressLineFour,
    @JsonProperty("postalCode") String postalCode,
    @JsonProperty("countryCode") String countryCode
) {
    /**
     * Accounting period for the business.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AccountingPeriod(
        @JsonProperty("start") LocalDate start,
        @JsonProperty("end") LocalDate end
    ) {}

    /**
     * Checks if the business is active (not ceased).
     */
    public boolean isActive() {
        return cessationDate == null;
    }

    /**
     * Gets a formatted address string.
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLineOne != null) sb.append(addressLineOne);
        if (addressLineTwo != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(addressLineTwo);
        }
        if (addressLineThree != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(addressLineThree);
        }
        if (postalCode != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(postalCode);
        }
        return sb.toString();
    }
}
