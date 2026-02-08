package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Display model for bank transaction review table rows.
 * Provides formatted values and filter matching for the Transaction Review Dashboard.
 */
public record TransactionReviewTableRow(
    UUID id,
    LocalDate date,
    String description,
    BigDecimal amount,
    boolean isIncome,
    ReviewStatus reviewStatus,
    Boolean isBusiness,
    BigDecimal confidenceScore,
    ExpenseCategory suggestedCategory,
    String exclusionReason
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM ''yy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);
    private static final BigDecimal HIGH_CONFIDENCE = new BigDecimal("0.9");
    private static final BigDecimal MEDIUM_CONFIDENCE = new BigDecimal("0.6");

    /**
     * Creates a table row from a BankTransaction domain object.
     */
    public static TransactionReviewTableRow fromDomain(BankTransaction tx) {
        return new TransactionReviewTableRow(
            tx.id(),
            tx.date(),
            tx.description(),
            tx.amount(),
            tx.isIncome(),
            tx.reviewStatus(),
            tx.isBusiness(),
            tx.confidenceScore(),
            tx.suggestedCategory(),
            tx.exclusionReason()
        );
    }

    /**
     * Returns the date formatted for display (e.g., "10 Jan '26").
     */
    public String getFormattedDate() {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Returns the absolute amount formatted as GBP currency (e.g., "£2,500.00").
     */
    public String getFormattedAmount() {
        return CURRENCY_FORMAT.format(amount.abs());
    }

    /**
     * Returns the signed amount formatted as GBP with +/- prefix.
     */
    public String getSignedFormattedAmount() {
        String formatted = CURRENCY_FORMAT.format(amount.abs());
        return isIncome ? "+" + formatted : "-" + formatted;
    }

    /**
     * Returns a confidence label: "HIGH", "MEDIUM", or "LOW".
     * Returns "—" if no confidence score is set.
     */
    public String getConfidenceLabel() {
        if (confidenceScore == null) {
            return "\u2014"; // em dash
        }
        if (confidenceScore.compareTo(HIGH_CONFIDENCE) >= 0) {
            return "HIGH";
        }
        if (confidenceScore.compareTo(MEDIUM_CONFIDENCE) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * Returns the CSS style class for the confidence badge.
     */
    public String getConfidenceStyleClass() {
        if (confidenceScore == null) {
            return "confidence-none";
        }
        if (confidenceScore.compareTo(HIGH_CONFIDENCE) >= 0) {
            return "confidence-high";
        }
        if (confidenceScore.compareTo(MEDIUM_CONFIDENCE) >= 0) {
            return "confidence-medium";
        }
        return "confidence-low";
    }

    /**
     * Returns the CSS style class for the review status badge.
     */
    public String getStatusStyleClass() {
        return switch (reviewStatus) {
            case PENDING -> "status-pending";
            case CATEGORIZED -> "status-categorized";
            case EXCLUDED -> "status-excluded";
            case SKIPPED -> "status-skipped";
        };
    }

    /**
     * Returns a display name for the suggested category, or "—" if none.
     */
    public String getSuggestedCategoryDisplay() {
        return suggestedCategory != null ? suggestedCategory.getDisplayName() : "\u2014";
    }

    /**
     * Returns the business/personal label.
     */
    public String getBusinessLabel() {
        if (isBusiness == null) {
            return "\u2014";
        }
        return isBusiness ? "Business" : "Personal";
    }

    /**
     * Checks if this row matches the search query.
     * Searches in description (case-insensitive).
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String lowerQuery = query.toLowerCase();
        return description != null && description.toLowerCase().contains(lowerQuery);
    }

    /**
     * Checks if this row matches the status filter.
     *
     * @param filterStatus null means all statuses
     */
    public boolean matchesStatus(ReviewStatus filterStatus) {
        return filterStatus == null || this.reviewStatus == filterStatus;
    }

    /**
     * Checks if this row's date is within the given range (inclusive).
     */
    public boolean matchesDateRange(LocalDate from, LocalDate to) {
        if (from != null && date.isBefore(from)) {
            return false;
        }
        if (to != null && date.isAfter(to)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if this row's absolute amount is within the given range.
     */
    public boolean matchesAmountRange(BigDecimal min, BigDecimal max) {
        BigDecimal absAmount = amount.abs();
        if (min != null && absAmount.compareTo(min) < 0) {
            return false;
        }
        if (max != null && absAmount.compareTo(max) > 0) {
            return false;
        }
        return true;
    }
}
