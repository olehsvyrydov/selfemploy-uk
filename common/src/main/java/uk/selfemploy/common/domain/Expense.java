package uk.selfemploy.common.domain;

import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents an expense transaction for a self-employed business.
 *
 * Expenses are mapped to SA103 form boxes based on their category.
 *
 * <p>Unique identifier fields (Sprint 10C - SE-10C-002) enable accurate duplicate detection:
 * <ul>
 *   <li>bankTransactionRef - Bank statement transaction reference (e.g., "FPS-2025-001234")</li>
 *   <li>supplierRef - Supplier/vendor reference number</li>
 *   <li>invoiceNumber - Invoice number from supplier</li>
 * </ul>
 */
public record Expense(
    UUID id,
    UUID businessId,
    LocalDate date,
    BigDecimal amount,
    String description,
    ExpenseCategory category,
    String receiptPath,
    String notes,
    String bankTransactionRef,
    String supplierRef,
    String invoiceNumber,
    UUID bankTransactionId,
    int businessUsePercentage
) {
    /** An expense with no split stated is wholly business, which is the ordinary case. */
    public static final int FULLY_BUSINESS = 100;

    /**
     * Compact constructor for validation.
     */
    public Expense {
        validateBusinessId(businessId);
        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);
        validateBusinessUsePercentage(businessUsePercentage);
    }

    /**
     * An expense that is wholly business use.
     *
     * <p>Kept so that every caller written before expenses could be apportioned still compiles and
     * still means what it did: no stated split is the same as all of it.
     */
    public Expense(
            UUID id,
            UUID businessId,
            LocalDate date,
            BigDecimal amount,
            String description,
            ExpenseCategory category,
            String receiptPath,
            String notes,
            String bankTransactionRef,
            String supplierRef,
            String invoiceNumber,
            UUID bankTransactionId) {
        this(id, businessId, date, amount, description, category, receiptPath, notes,
             bankTransactionRef, supplierRef, invoiceNumber, bankTransactionId, FULLY_BUSINESS);
    }

    /**
     * The part of this expense that may be claimed: the stated business-use share of the amount,
     * to the penny.
     *
     * <p>Zero for a category that is not an allowable expense — depreciation and capital purchases
     * are claimed as capital allowances instead, which this app does not yet calculate, so counting
     * them here would overstate the deduction.
     *
     * @return the claimable amount, rounded half up to two decimal places
     */
    public BigDecimal allowableAmount() {
        if (!category.isAllowable()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (businessUsePercentage == FULLY_BUSINESS) {
            // Returned untouched rather than multiplied by 100 and divided again. Rounding here
            // would change the totals of every expense already recorded, including for a year
            // already filed, purely because the column was added.
            return amount;
        }
        return amount
            .multiply(BigDecimal.valueOf(businessUsePercentage))
            .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /** A copy of this expense with a different business-use share. */
    public Expense withBusinessUsePercentage(int percentage) {
        return new Expense(id, businessId, date, amount, description, category, receiptPath, notes,
                bankTransactionRef, supplierRef, invoiceNumber, bankTransactionId, percentage);
    }

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private static void validateBusinessUsePercentage(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException(
                "Business use percentage must be between 0 and 100, but was " + percentage);
        }
    }

    /**
     * Creates a new expense with a generated ID (backward compatible - no unique identifiers).
     */
    public static Expense create(
            UUID businessId,
            LocalDate date,
            BigDecimal amount,
            String description,
            ExpenseCategory category,
            String receiptPath,
            String notes) {
        return new Expense(
            UUID.randomUUID(),
            businessId,
            date,
            amount,
            description,
            category,
            receiptPath,
            notes,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Creates a new expense with a generated ID and unique identifier fields.
     *
     * @param businessId the business this expense belongs to
     * @param date the date of the expense
     * @param amount the expense amount
     * @param description description of the expense
     * @param category the expense category (SA103 box mapping)
     * @param receiptPath optional path to receipt/proof document
     * @param notes optional notes about the expense
     * @param bankTransactionRef optional bank statement transaction reference
     * @param supplierRef optional supplier/vendor reference
     * @param invoiceNumber optional invoice number from supplier
     */
    public static Expense create(
            UUID businessId,
            LocalDate date,
            BigDecimal amount,
            String description,
            ExpenseCategory category,
            String receiptPath,
            String notes,
            String bankTransactionRef,
            String supplierRef,
            String invoiceNumber) {
        return new Expense(
            UUID.randomUUID(),
            businessId,
            date,
            amount,
            description,
            category,
            receiptPath,
            notes,
            bankTransactionRef,
            supplierRef,
            invoiceNumber,
            null
        );
    }

    /**
     * Returns whether this expense is allowable for tax deduction.
     * Delegates to the category's allowable status.
     */
    public boolean isAllowable() {
        return category != null && category.isAllowable();
    }

    private static void validateBusinessId(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null");
        }
    }

    private static void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Expense date cannot be null");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expense date cannot be in the future");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Expense amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expense amount must be positive");
        }
    }

    private static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Expense description cannot be null or empty");
        }
    }

    private static void validateCategory(ExpenseCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Expense category cannot be null");
        }
    }
}
