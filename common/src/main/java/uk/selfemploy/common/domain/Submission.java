package uk.selfemploy.common.domain;

import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents an HMRC submission record.
 *
 * <p>Tracks both quarterly MTD submissions and annual self-assessments.</p>
 */
public record Submission(
    UUID id,
    UUID businessId,
    SubmissionType type,
    TaxYear taxYear,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netProfit,
    SubmissionStatus status,
    String hmrcReference,
    String errorMessage,
    Instant submittedAt,
    Instant updatedAt
) {

    /**
     * Compact constructor for validation.
     */
    public Submission {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Submission type is required");
        }
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year is required");
        }
        if (periodStart == null) {
            throw new IllegalArgumentException("Period start date is required");
        }
        if (periodEnd == null) {
            throw new IllegalArgumentException("Period end date is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }
        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO;
        }
        if (netProfit == null) {
            netProfit = totalIncome.subtract(totalExpenses);
        }
    }

    /**
     * Creates a new pending submission for a quarterly update.
     */
    public static Submission createQuarterly(UUID businessId, TaxYear taxYear, Quarter quarter,
                                             BigDecimal totalIncome, BigDecimal totalExpenses) {
        SubmissionType type = switch (quarter) {
            case Q1 -> SubmissionType.QUARTERLY_Q1;
            case Q2 -> SubmissionType.QUARTERLY_Q2;
            case Q3 -> SubmissionType.QUARTERLY_Q3;
            case Q4 -> SubmissionType.QUARTERLY_Q4;
        };

        return new Submission(
            UUID.randomUUID(),
            businessId,
            type,
            taxYear,
            quarter.getStartDate(taxYear),
            quarter.getEndDate(taxYear),
            totalIncome,
            totalExpenses,
            totalIncome.subtract(totalExpenses),
            SubmissionStatus.PENDING,
            null,
            null,
            Instant.now(),
            Instant.now()
        );
    }

    /**
     * Creates a new pending submission for annual self-assessment.
     */
    public static Submission createAnnual(UUID businessId, TaxYear taxYear,
                                          BigDecimal totalIncome, BigDecimal totalExpenses) {
        return new Submission(
            UUID.randomUUID(),
            businessId,
            SubmissionType.ANNUAL,
            taxYear,
            taxYear.startDate(),
            taxYear.endDate(),
            totalIncome,
            totalExpenses,
            totalIncome.subtract(totalExpenses),
            SubmissionStatus.PENDING,
            null,
            null,
            Instant.now(),
            Instant.now()
        );
    }

    /**
     * Returns a copy with ACCEPTED status and HMRC reference.
     */
    public Submission withAccepted(String hmrcReference) {
        return new Submission(
            id, businessId, type, taxYear, periodStart, periodEnd,
            totalIncome, totalExpenses, netProfit,
            SubmissionStatus.ACCEPTED,
            hmrcReference,
            null,
            submittedAt,
            Instant.now()
        );
    }

    /**
     * Returns a copy with SUBMITTED status.
     */
    public Submission withSubmitted() {
        return new Submission(
            id, businessId, type, taxYear, periodStart, periodEnd,
            totalIncome, totalExpenses, netProfit,
            SubmissionStatus.SUBMITTED,
            hmrcReference,
            null,
            submittedAt,
            Instant.now()
        );
    }

    /**
     * Returns a copy with REJECTED status and error message.
     */
    public Submission withRejected(String errorMessage) {
        return new Submission(
            id, businessId, type, taxYear, periodStart, periodEnd,
            totalIncome, totalExpenses, netProfit,
            SubmissionStatus.REJECTED,
            null,
            errorMessage,
            submittedAt,
            Instant.now()
        );
    }

    /**
     * Returns a human-readable label for this submission.
     */
    public String getLabel() {
        return String.format("%s %s", type.getShortName(), taxYear.label());
    }

    /**
     * Checks if this submission was successful.
     */
    public boolean isSuccessful() {
        return status.isSuccessful();
    }

    /**
     * Gets the quarter for quarterly submissions.
     *
     * @return The quarter, or null for annual submissions
     */
    public Quarter getQuarter() {
        return switch (type) {
            case QUARTERLY_Q1 -> Quarter.Q1;
            case QUARTERLY_Q2 -> Quarter.Q2;
            case QUARTERLY_Q3 -> Quarter.Q3;
            case QUARTERLY_Q4 -> Quarter.Q4;
            case ANNUAL -> null;
        };
    }
}
