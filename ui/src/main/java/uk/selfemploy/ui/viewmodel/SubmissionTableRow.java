package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Display model for submission history table rows.
 * Provides formatted values for UI display in the Submission History view.
 *
 * <p>This record serves as a view model for displaying HMRC submissions
 * in the history view, providing formatted dates, amounts, and status badges.</p>
 *
 * @param id The unique identifier of the submission
 * @param submittedAt The date and time when the submission was made
 * @param type The type of submission (quarterly or annual)
 * @param taxYear The tax year in format "2025/26"
 * @param status The current status of the submission
 * @param hmrcReference The HMRC reference number (null if pending)
 * @param totalIncome Total income included in the submission
 * @param totalExpenses Total expenses included in the submission
 * @param netProfit Net profit (income - expenses)
 * @param taxDue Tax liability (for annual submissions)
 * @param errorMessage Error message if submission was rejected
 */
public record SubmissionTableRow(
    Long id,
    LocalDateTime submittedAt,
    SubmissionType type,
    String taxYear,
    SubmissionStatus status,
    String hmrcReference,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netProfit,
    BigDecimal taxDue,
    String errorMessage
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FULL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    /**
     * Returns the submission date formatted for display (e.g., "24 Jan 2026").
     */
    public String getFormattedDate() {
        return submittedAt != null ? submittedAt.format(DATE_FORMATTER) : "";
    }

    /**
     * Returns the submission time formatted for display (e.g., "14:32").
     */
    public String getFormattedTime() {
        return submittedAt != null ? submittedAt.format(TIME_FORMATTER) : "";
    }

    /**
     * Returns the full date and time formatted for display (e.g., "24 Jan 2026 14:32:15").
     */
    public String getFormattedDateTime() {
        return submittedAt != null ? submittedAt.format(FULL_DATE_TIME_FORMATTER) : "";
    }

    /**
     * Returns the type badge text (e.g., "Q1", "Annual").
     */
    public String getTypeBadgeText() {
        return type != null ? type.getShortName() : "";
    }

    /**
     * Returns the full type display name (e.g., "Quarter 1 (Apr-Jul)").
     */
    public String getTypeDisplayName() {
        return type != null ? type.getDisplayName() : "";
    }

    /**
     * Returns the period display text (e.g., "Q1 2025/26").
     */
    public String getPeriodDisplay() {
        if (type == null || taxYear == null) {
            return "";
        }
        return type.getShortName() + " " + taxYear;
    }

    /**
     * Returns the status display name (e.g., "Accepted").
     */
    public String getStatusDisplay() {
        return status != null ? status.getDisplayName() : "";
    }

    /**
     * Returns the status icon identifier.
     */
    public String getStatusIcon() {
        return status != null ? status.getIcon() : "";
    }

    /**
     * Returns the CSS style class for the status badge.
     */
    public String getStatusStyleClass() {
        return status != null ? status.getStyleClass() : "";
    }

    /**
     * Returns the CSS style class for the type badge.
     */
    public String getTypeStyleClass() {
        if (type == null) {
            return "";
        }
        return type.isQuarterly() ? "type-" + type.getShortName().toLowerCase() : "type-annual";
    }

    /**
     * Returns the HMRC reference or "Pending" if not yet assigned.
     */
    public String getReferenceDisplay() {
        return hmrcReference != null && !hmrcReference.isBlank() ? hmrcReference : "Pending";
    }

    /**
     * Returns the total income formatted as currency (e.g., "£45,000.00").
     */
    public String getFormattedIncome() {
        return totalIncome != null ? CURRENCY_FORMAT.format(totalIncome) : "£0.00";
    }

    /**
     * Returns the total expenses formatted as currency (e.g., "£13,000.00").
     */
    public String getFormattedExpenses() {
        return totalExpenses != null ? CURRENCY_FORMAT.format(totalExpenses) : "£0.00";
    }

    /**
     * Returns the net profit formatted as currency (e.g., "£32,000.00").
     */
    public String getFormattedProfit() {
        return netProfit != null ? CURRENCY_FORMAT.format(netProfit) : "£0.00";
    }

    /**
     * Returns the tax due formatted as currency (e.g., "£5,051.80").
     */
    public String getFormattedTaxDue() {
        return taxDue != null ? CURRENCY_FORMAT.format(taxDue) : "£0.00";
    }

    /**
     * Returns the primary value to display on the card.
     * For annual submissions, shows tax due; for quarterly, shows income.
     */
    public String getPrimaryValueDisplay() {
        if (type == SubmissionType.ANNUAL) {
            return getFormattedTaxDue();
        }
        return getFormattedIncome();
    }

    /**
     * Returns the label for the primary value.
     * For annual submissions, "Tax Due"; for quarterly, "Income".
     */
    public String getPrimaryValueLabel() {
        if (type == SubmissionType.ANNUAL) {
            return "Tax Due";
        }
        return "Income";
    }

    /**
     * Checks if this submission has an error message.
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isBlank();
    }

    /**
     * Checks if this submission was rejected.
     */
    public boolean isRejected() {
        return status == SubmissionStatus.REJECTED;
    }

    /**
     * Checks if this submission is still pending.
     */
    public boolean isPending() {
        return status == SubmissionStatus.PENDING;
    }

    /**
     * Checks if this submission was successful (accepted or submitted).
     */
    public boolean isSuccessful() {
        return status != null && status.isSuccessful();
    }

    /**
     * Checks if this submission matches a search query.
     * Searches in reference, tax year, and error message.
     *
     * @param query The search query (case-insensitive)
     * @return true if matches, false otherwise
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String lowerQuery = query.toLowerCase();
        return (hmrcReference != null && hmrcReference.toLowerCase().contains(lowerQuery))
            || (taxYear != null && taxYear.toLowerCase().contains(lowerQuery))
            || (errorMessage != null && errorMessage.toLowerCase().contains(lowerQuery))
            || (type != null && type.getShortName().toLowerCase().contains(lowerQuery));
    }

    /**
     * Checks if this submission matches a tax year filter.
     *
     * @param filterTaxYear The tax year to filter by (null or "All Years" means all)
     * @return true if matches, false otherwise
     */
    public boolean matchesTaxYear(String filterTaxYear) {
        if (filterTaxYear == null || filterTaxYear.isBlank() || "All Years".equals(filterTaxYear)) {
            return true;
        }
        return filterTaxYear.equals(this.taxYear);
    }

    /**
     * Checks if this submission matches a status filter.
     *
     * @param filterStatus The status to filter by (null means all)
     * @return true if matches, false otherwise
     */
    public boolean matchesStatus(SubmissionStatus filterStatus) {
        return filterStatus == null || this.status == filterStatus;
    }

    /**
     * Creates a builder for constructing SubmissionTableRow instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating SubmissionTableRow instances.
     */
    public static class Builder {
        private Long id;
        private LocalDateTime submittedAt;
        private SubmissionType type;
        private String taxYear;
        private SubmissionStatus status;
        private String hmrcReference;
        private BigDecimal totalIncome;
        private BigDecimal totalExpenses;
        private BigDecimal netProfit;
        private BigDecimal taxDue;
        private String errorMessage;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }

        public Builder type(SubmissionType type) {
            this.type = type;
            return this;
        }

        public Builder taxYear(String taxYear) {
            this.taxYear = taxYear;
            return this;
        }

        public Builder status(SubmissionStatus status) {
            this.status = status;
            return this;
        }

        public Builder hmrcReference(String hmrcReference) {
            this.hmrcReference = hmrcReference;
            return this;
        }

        public Builder totalIncome(BigDecimal totalIncome) {
            this.totalIncome = totalIncome;
            return this;
        }

        public Builder totalExpenses(BigDecimal totalExpenses) {
            this.totalExpenses = totalExpenses;
            return this;
        }

        public Builder netProfit(BigDecimal netProfit) {
            this.netProfit = netProfit;
            return this;
        }

        public Builder taxDue(BigDecimal taxDue) {
            this.taxDue = taxDue;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public SubmissionTableRow build() {
            return new SubmissionTableRow(
                id, submittedAt, type, taxYear, status, hmrcReference,
                totalIncome, totalExpenses, netProfit, taxDue, errorMessage
            );
        }
    }
}
