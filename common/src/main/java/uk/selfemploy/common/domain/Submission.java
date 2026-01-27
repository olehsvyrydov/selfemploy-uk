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
 *
 * <p>Includes taxpayer identifiers for regulatory compliance:
 * <ul>
 *   <li>UTR (Unique Taxpayer Reference) - 10 digits</li>
 *   <li>NINO (National Insurance Number) - stored in uppercase</li>
 * </ul>
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
    Instant updatedAt,
    Instant declarationAcceptedAt,
    String declarationTextHash,
    String utr,
    String nino
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
            Instant.now(),
            null,  // declarationAcceptedAt - must be set before submission
            null,  // declarationTextHash - must be set before submission
            null,  // utr
            null   // nino
        );
    }

    /**
     * Creates a new pending quarterly submission with UTR and NINO.
     *
     * @param businessId    the business ID
     * @param taxYear       the tax year
     * @param quarter       the quarter
     * @param totalIncome   total income for the period
     * @param totalExpenses total expenses for the period
     * @param utr           Unique Taxpayer Reference (10 digits)
     * @param nino          National Insurance Number (uppercase)
     * @return a new pending submission with UTR and NINO
     */
    public static Submission createQuarterlyWithUtrAndNino(
            UUID businessId, TaxYear taxYear, Quarter quarter,
            BigDecimal totalIncome, BigDecimal totalExpenses,
            String utr, String nino) {
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
            Instant.now(),
            null,
            null,
            utr,
            nino
        );
    }

    /**
     * Creates a new pending submission for a quarterly update with declaration.
     *
     * @param businessId            the business ID
     * @param taxYear               the tax year
     * @param quarter               the quarter
     * @param totalIncome           total income for the period
     * @param totalExpenses         total expenses for the period
     * @param declarationAcceptedAt when the user accepted the declaration (UTC)
     * @param declarationTextHash   SHA-256 hash of the declaration text
     * @return a new pending submission with declaration info
     */
    public static Submission createQuarterlyWithDeclaration(
            UUID businessId, TaxYear taxYear, Quarter quarter,
            BigDecimal totalIncome, BigDecimal totalExpenses,
            Instant declarationAcceptedAt, String declarationTextHash) {
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
            Instant.now(),
            declarationAcceptedAt,
            declarationTextHash,
            null,  // utr
            null   // nino
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
            Instant.now(),
            null,  // declarationAcceptedAt - must be set before submission
            null,  // declarationTextHash - must be set before submission
            null,  // utr
            null   // nino
        );
    }

    /**
     * Creates a new pending annual submission with UTR and NINO.
     *
     * @param businessId    the business ID
     * @param taxYear       the tax year
     * @param totalIncome   total income for the year
     * @param totalExpenses total expenses for the year
     * @param utr           Unique Taxpayer Reference (10 digits)
     * @param nino          National Insurance Number (uppercase)
     * @return a new pending submission with UTR and NINO
     */
    public static Submission createAnnualWithUtrAndNino(
            UUID businessId, TaxYear taxYear,
            BigDecimal totalIncome, BigDecimal totalExpenses,
            String utr, String nino) {
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
            Instant.now(),
            null,
            null,
            utr,
            nino
        );
    }

    /**
     * Creates a new pending submission for annual self-assessment with declaration.
     *
     * @param businessId            the business ID
     * @param taxYear               the tax year
     * @param totalIncome           total income for the year
     * @param totalExpenses         total expenses for the year
     * @param declarationAcceptedAt when the user accepted the declaration (UTC)
     * @param declarationTextHash   SHA-256 hash of the declaration text
     * @return a new pending submission with declaration info
     */
    public static Submission createAnnualWithDeclaration(
            UUID businessId, TaxYear taxYear,
            BigDecimal totalIncome, BigDecimal totalExpenses,
            Instant declarationAcceptedAt, String declarationTextHash) {
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
            Instant.now(),
            declarationAcceptedAt,
            declarationTextHash,
            null,  // utr
            null   // nino
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
            Instant.now(),
            declarationAcceptedAt,
            declarationTextHash,
            utr,
            nino
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
            Instant.now(),
            declarationAcceptedAt,
            declarationTextHash,
            utr,
            nino
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
            Instant.now(),
            declarationAcceptedAt,
            declarationTextHash,
            utr,
            nino
        );
    }

    /**
     * Returns a copy with declaration information.
     *
     * @param declarationAcceptedAt when the declaration was accepted (UTC)
     * @param declarationTextHash   SHA-256 hash of the declaration text
     * @return a new Submission with declaration info set
     */
    public Submission withDeclaration(Instant declarationAcceptedAt, String declarationTextHash) {
        return new Submission(
            id, businessId, type, taxYear, periodStart, periodEnd,
            totalIncome, totalExpenses, netProfit,
            status,
            hmrcReference,
            errorMessage,
            submittedAt,
            Instant.now(),
            declarationAcceptedAt,
            declarationTextHash,
            utr,
            nino
        );
    }

    /**
     * Returns a copy with UTR and NINO set.
     *
     * @param utr  Unique Taxpayer Reference (10 digits)
     * @param nino National Insurance Number (uppercase)
     * @return a new Submission with UTR and NINO set
     */
    public Submission withUtrAndNino(String utr, String nino) {
        return new Submission(
            id, businessId, type, taxYear, periodStart, periodEnd,
            totalIncome, totalExpenses, netProfit,
            status,
            hmrcReference,
            errorMessage,
            submittedAt,
            Instant.now(),
            declarationAcceptedAt,
            declarationTextHash,
            utr,
            nino
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
     * Checks if this submission has a valid declaration.
     *
     * @return true if both declarationAcceptedAt and declarationTextHash are set
     */
    public boolean hasDeclaration() {
        return declarationAcceptedAt != null && declarationTextHash != null && !declarationTextHash.isBlank();
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
