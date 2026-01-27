package uk.selfemploy.persistence.exception;

import uk.selfemploy.common.enums.SubmissionType;

import java.util.UUID;

/**
 * Exception thrown when attempting to create a duplicate submission.
 *
 * <p>A submission is considered a duplicate if there's already a submission
 * with the same (business_id, tax_year_start, type) combination.</p>
 */
public class DuplicateSubmissionException extends RuntimeException {

    private final UUID businessId;
    private final int taxYearStart;
    private final SubmissionType type;

    public DuplicateSubmissionException(UUID businessId, int taxYearStart, SubmissionType type) {
        super(String.format(
            "A submission already exists for business %s, tax year %d/%d, type %s",
            businessId, taxYearStart, taxYearStart + 1, type
        ));
        this.businessId = businessId;
        this.taxYearStart = taxYearStart;
        this.type = type;
    }

    public DuplicateSubmissionException(UUID businessId, int taxYearStart, SubmissionType type, Throwable cause) {
        super(String.format(
            "A submission already exists for business %s, tax year %d/%d, type %s",
            businessId, taxYearStart, taxYearStart + 1, type
        ), cause);
        this.businessId = businessId;
        this.taxYearStart = taxYearStart;
        this.type = type;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public int getTaxYearStart() {
        return taxYearStart;
    }

    public SubmissionType getType() {
        return type;
    }
}
