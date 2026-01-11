package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Display model for imported transaction table rows in the CSV Bank Import Wizard.
 * Represents a single parsed transaction from the CSV file.
 *
 * SE-601: CSV Bank Import Wizard
 *
 * @param id Unique identifier for this transaction row
 * @param date Transaction date
 * @param description Transaction description from the bank
 * @param amount Transaction amount (always positive, type indicates direction)
 * @param type Whether this is income or expense
 * @param category Assigned expense category (null if uncategorized)
 * @param isDuplicate True if this transaction already exists in the database
 * @param confidence Auto-categorization confidence (0-100)
 * @param status Row status for visual indication
 */
public record ImportedTransactionRow(
    UUID id,
    LocalDate date,
    String description,
    BigDecimal amount,
    TransactionType type,
    ExpenseCategory category,
    boolean isDuplicate,
    int confidence,
    TransactionStatus status
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM ''yy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    /**
     * Creates a new transaction row with a generated UUID.
     */
    public static ImportedTransactionRow create(
            LocalDate date,
            String description,
            BigDecimal amount,
            TransactionType type,
            ExpenseCategory category,
            boolean isDuplicate,
            int confidence
    ) {
        TransactionStatus status = determineStatus(category, isDuplicate, confidence);
        return new ImportedTransactionRow(
                UUID.randomUUID(),
                date,
                description,
                amount.abs(),
                type,
                category,
                isDuplicate,
                confidence,
                status
        );
    }

    /**
     * Creates a copy with an updated category.
     */
    public ImportedTransactionRow withCategory(ExpenseCategory newCategory) {
        TransactionStatus newStatus = determineStatus(newCategory, isDuplicate, 100);
        return new ImportedTransactionRow(
                id, date, description, amount, type, newCategory, isDuplicate, 100, newStatus
        );
    }

    /**
     * Creates a copy with updated duplicate status.
     */
    public ImportedTransactionRow withDuplicateStatus(boolean newIsDuplicate) {
        TransactionStatus newStatus = determineStatus(category, newIsDuplicate, confidence);
        return new ImportedTransactionRow(
                id, date, description, amount, type, category, newIsDuplicate, confidence, newStatus
        );
    }

    /**
     * Returns the date formatted for display (e.g., "5 Jan '26").
     */
    public String getFormattedDate() {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Returns the amount formatted as GBP currency (e.g., "£1,500.00").
     */
    public String getFormattedAmount() {
        return CURRENCY_FORMAT.format(amount);
    }

    /**
     * Returns the amount with sign prefix for display.
     * Income: +£1,500.00, Expense: -£45.50
     */
    public String getFormattedAmountWithSign() {
        String formatted = CURRENCY_FORMAT.format(amount);
        return type == TransactionType.INCOME ? "+" + formatted : "-" + formatted;
    }

    /**
     * Returns the category display name, or "Uncategorized" if null.
     */
    public String getCategoryDisplay() {
        return category != null ? category.getDisplayName() : "Uncategorized";
    }

    /**
     * Returns the confidence display string (e.g., "85%").
     */
    public String getConfidenceDisplay() {
        if (category == null) {
            return "-";
        }
        return confidence + "%";
    }

    /**
     * Returns the CSS class for confidence level styling.
     */
    public String getConfidenceCssClass() {
        if (category == null || confidence == 0) {
            return "confidence-low";
        }
        if (confidence >= 80) {
            return "confidence-high";
        } else if (confidence >= 50) {
            return "confidence-medium";
        } else {
            return "confidence-low";
        }
    }

    /**
     * Returns true if this transaction needs manual categorization.
     */
    public boolean needsCategorization() {
        return category == null && type == TransactionType.EXPENSE;
    }

    /**
     * Checks if this transaction matches a search query.
     * Searches in description (case-insensitive).
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return description != null && description.toLowerCase().contains(query.toLowerCase());
    }

    /**
     * Checks if this transaction matches the given filter.
     */
    public boolean matchesFilter(TransactionFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case INCOME_ONLY -> type == TransactionType.INCOME;
            case EXPENSES_ONLY -> type == TransactionType.EXPENSE;
            case UNCATEGORIZED -> category == null;
            case DUPLICATES -> isDuplicate;
        };
    }

    /**
     * Determines the status based on category, duplicate flag, and confidence.
     */
    private static TransactionStatus determineStatus(ExpenseCategory category, boolean isDuplicate, int confidence) {
        if (isDuplicate) {
            return TransactionStatus.DUPLICATE;
        }
        if (category == null || confidence < 50) {
            return TransactionStatus.WARNING;
        }
        return TransactionStatus.OK;
    }
}
