package uk.selfemploy.common.domain;

import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
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
    UUID bankTransactionId
) {
    /**
     * Compact constructor for validation.
     */
    public Expense {
        validateBusinessId(businessId);
        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);
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
