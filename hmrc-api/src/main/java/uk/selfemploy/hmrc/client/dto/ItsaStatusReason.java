package uk.selfemploy.hmrc.client.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The reason a customer is in their current ITSA (Income Tax Self Assessment) status,
 * as returned by HMRC SA Individual Details API v2.
 *
 * <p>HMRC's 2026-05-15 production deployment added seven new reason codes; the existing
 * "Non-Digital" status was renamed to "Digitally Exempt" at the same time. Unknown
 * codes are surfaced via {@link #UNKNOWN} so a future HMRC schema addition does not
 * fail deserialisation.
 *
 * <p><strong>Privacy note (UK GDPR Art. 9):</strong> two of the new reasons relate to
 * special-category data — {@link #BLIND_PERSONS_ALLOWANCE} (health) and
 * {@link #MINISTERS_OF_RELIGION} (religious belief). Lawful basis must be UK GDPR
 * Art. 9(2)(b) (social security / legal obligation), NOT consent. See SLFEMPUK-39
 * (S17-15) for the lawful-basis register entry that must accompany any UI surface
 * showing these reasons.
 *
 * @see <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/self-assessment-individual-details-api">
 *     HMRC Self Assessment Individual Details API</a>
 */
public enum ItsaStatusReason {

    /** Customer-initiated registration. */
    SIGNUP_RETURN_AVAILABLE("Sign up - return available"),

    /** Customer-initiated registration; no return available yet. */
    SIGNUP_NO_RETURN_AVAILABLE("Sign up - no return available"),

    /** Migrated from agent services. */
    SIGNUP_AGENT("Sign up - agent"),

    /** Newly added 2026-05-15: customer holds capacitor status (acting on behalf of another). */
    CAPACITOR("Capacitor"),

    /** Newly added 2026-05-15: residence and remittance basis (post-non-dom abolition, FA 2025). */
    RESIDENCE_AND_REMITTANCE("Residence and remittance"),

    /**
     * Newly added 2026-05-15: Ministers of religion treatment (ITEPA 2003 s.290+).
     * <strong>UK GDPR Art. 9 Special Category data (religious belief).</strong>
     */
    MINISTERS_OF_RELIGION("Ministers of religion"),

    /** Newly added 2026-05-15: Lloyd's underwriters treatment (FA 1993 s.171+). */
    LLOYDS_UNDERWRITERS("Lloyds underwriters"),

    /**
     * Newly added 2026-05-15: customer claims Blind Person's Allowance (ITA 2007 s.38).
     * <strong>UK GDPR Art. 9 Special Category data (health / disability).</strong>
     */
    BLIND_PERSONS_ALLOWANCE("Blind persons allowance"),

    /** Newly added 2026-05-15: customer claims Married Couple's Allowance (ITA 2007 s.45). */
    MARRIED_COUPLES_ALLOWANCE("Married couples allowance"),

    /** Newly added 2026-05-15: HMRC did not consider the return for MTD assessment. */
    RETURN_NOT_CONSIDERED("Return not considered"),

    /**
     * Sentinel — HMRC returned a reason code the app does not yet model.
     * Callers should display the raw string from HMRC alongside this and treat the
     * record as data-only (no business logic decisions branch on UNKNOWN).
     */
    UNKNOWN("Unknown");

    private final String hmrcLabel;

    ItsaStatusReason(String hmrcLabel) {
        this.hmrcLabel = hmrcLabel;
    }

    /** @return the exact string HMRC sends on the wire for this reason. */
    @JsonValue
    public String hmrcLabel() {
        return hmrcLabel;
    }

    /**
     * @return {@code true} when this reason carries UK GDPR Art. 9 Special Category data
     *         (health or religious-belief markers) and must be processed under
     *         Art. 9(2)(b) lawful basis only.
     */
    public boolean isSpecialCategoryData() {
        return this == BLIND_PERSONS_ALLOWANCE || this == MINISTERS_OF_RELIGION;
    }

    /**
     * Maps an HMRC-supplied label string back to the enum, returning {@link #UNKNOWN}
     * for any value not modelled (forward-compat with future HMRC additions).
     */
    @JsonCreator
    public static ItsaStatusReason fromHmrcLabel(String label) {
        if (label == null) {
            return UNKNOWN;
        }
        for (ItsaStatusReason reason : values()) {
            if (reason.hmrcLabel.equalsIgnoreCase(label)) {
                return reason;
            }
        }
        return UNKNOWN;
    }
}
