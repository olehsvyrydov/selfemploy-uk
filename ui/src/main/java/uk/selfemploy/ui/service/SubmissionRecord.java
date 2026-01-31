package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.ui.viewmodel.SubmissionTableRow;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Record representing a submission stored in SQLite.
 *
 * <p>This record is the persistence layer representation of HMRC submissions,
 * stored in the local SQLite database. It follows the SQLite conventions
 * used throughout the UI module:</p>
 * <ul>
 *   <li>UUIDs stored as TEXT</li>
 *   <li>Dates stored as ISO TEXT</li>
 *   <li>BigDecimals stored as TEXT</li>
 *   <li>Enums stored as name() TEXT</li>
 *   <li>Timestamps stored as ISO Instant TEXT</li>
 * </ul>
 *
 * <p>Implements BUG-10H-001: Submission History persistence per ADR-10H-001.</p>
 *
 * @param id            UUID as TEXT
 * @param businessId    Foreign key to business table (UUID as TEXT)
 * @param type          SubmissionType enum name (e.g., "QUARTERLY_Q1", "ANNUAL")
 * @param taxYearStart  Tax year start (e.g., 2025 for tax year 2025/26)
 * @param periodStart   Period start date (ISO date)
 * @param periodEnd     Period end date (ISO date)
 * @param totalIncome   Total income amount
 * @param totalExpenses Total expenses amount
 * @param netProfit     Net profit (income - expenses)
 * @param status        SubmissionStatus enum name (e.g., "ACCEPTED", "REJECTED")
 * @param hmrcReference HMRC reference number (nullable - null if pending/rejected)
 * @param errorMessage  Error message (nullable - null if successful)
 * @param submittedAt   Submission timestamp (ISO Instant)
 */
public record SubmissionRecord(
    String id,
    String businessId,
    String type,
    int taxYearStart,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netProfit,
    String status,
    String hmrcReference,
    String errorMessage,
    Instant submittedAt
) {

    /**
     * Returns the formatted tax year string (e.g., "2025/26").
     *
     * @return The tax year in display format
     */
    public String getFormattedTaxYear() {
        int endYear = (taxYearStart + 1) % 100;
        return taxYearStart + "/" + String.format("%02d", endYear);
    }

    /**
     * Converts this record to a SubmissionTableRow for UI display.
     *
     * <p>Handles the conversion from SQLite storage format to UI display format:</p>
     * <ul>
     *   <li>String type → SubmissionType enum</li>
     *   <li>String status → SubmissionStatus enum</li>
     *   <li>Instant → LocalDateTime (using system timezone)</li>
     *   <li>taxYearStart → formatted "YYYY/YY" string</li>
     * </ul>
     *
     * @return A SubmissionTableRow for UI display
     */
    public SubmissionTableRow toTableRow() {
        return SubmissionTableRow.builder()
            .id(null) // SQLite uses UUID string, not Long - UI will use the string ID
            .submittedAt(submittedAt != null
                ? LocalDateTime.ofInstant(submittedAt, ZoneId.systemDefault())
                : null)
            .type(getSubmissionType(type))
            .taxYear(getFormattedTaxYear())
            .status(getSubmissionStatus(status))
            .hmrcReference(hmrcReference)
            .totalIncome(totalIncome)
            .totalExpenses(totalExpenses)
            .netProfit(netProfit)
            .taxDue(null) // Tax due is calculated separately for annual submissions
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * Parses a SubmissionType from its string name.
     *
     * @param typeName The enum name (e.g., "QUARTERLY_Q1")
     * @return The corresponding SubmissionType
     * @throws IllegalArgumentException if the type name is invalid
     */
    public static SubmissionType getSubmissionType(String typeName) {
        return SubmissionType.valueOf(typeName);
    }

    /**
     * Parses a SubmissionStatus from its string name.
     *
     * @param statusName The enum name (e.g., "ACCEPTED")
     * @return The corresponding SubmissionStatus
     * @throws IllegalArgumentException if the status name is invalid
     */
    public static SubmissionStatus getSubmissionStatus(String statusName) {
        return SubmissionStatus.valueOf(statusName);
    }

    /**
     * Creates a SubmissionRecord from a domain Submission object.
     *
     * <p>This factory method converts from the domain layer representation
     * to the persistence layer representation, handling type conversions:</p>
     * <ul>
     *   <li>UUID → String (toString)</li>
     *   <li>SubmissionType enum → String (name)</li>
     *   <li>SubmissionStatus enum → String (name)</li>
     *   <li>TaxYear → int (startYear)</li>
     * </ul>
     *
     * @param submission The domain Submission to convert
     * @return A new SubmissionRecord ready for SQLite persistence
     * @throws IllegalArgumentException if submission is null
     */
    public static SubmissionRecord fromDomainSubmission(Submission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("submission cannot be null");
        }
        return new SubmissionRecord(
            submission.id().toString(),
            submission.businessId().toString(),
            submission.type().name(),
            submission.taxYear().startYear(),
            submission.periodStart(),
            submission.periodEnd(),
            submission.totalIncome(),
            submission.totalExpenses(),
            submission.netProfit(),
            submission.status().name(),
            submission.hmrcReference(),
            submission.errorMessage(),
            submission.submittedAt()
        );
    }
}
