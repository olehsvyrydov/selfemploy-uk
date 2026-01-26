package uk.selfemploy.common.domain;

import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents an income transaction for a self-employed business.
 *
 * Income is mapped to SA103 form boxes 9 (Turnover) and 10 (Other Income).
 *
 * <p>Unique identifier fields (Sprint 10C - SE-10C-002) enable accurate duplicate detection:
 * <ul>
 *   <li>bankTransactionRef - Bank statement transaction reference (e.g., "FPS-2025-001234")</li>
 *   <li>invoiceNumber - Invoice number for the income (e.g., "INV-2025-001")</li>
 *   <li>receiptPath - Path to the receipt/proof document</li>
 * </ul>
 */
public record Income(
    UUID id,
    UUID businessId,
    LocalDate date,
    BigDecimal amount,
    String description,
    IncomeCategory category,
    String reference,
    String bankTransactionRef,
    String invoiceNumber,
    String receiptPath
) {
    /**
     * Compact constructor for validation.
     */
    public Income {
        validateBusinessId(businessId);
        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);
    }

    /**
     * Creates a new income with a generated ID (backward compatible - no unique identifiers).
     */
    public static Income create(
            UUID businessId,
            LocalDate date,
            BigDecimal amount,
            String description,
            IncomeCategory category,
            String reference) {
        return new Income(
            UUID.randomUUID(),
            businessId,
            date,
            amount,
            description,
            category,
            reference,
            null,
            null,
            null
        );
    }

    /**
     * Creates a new income with a generated ID and unique identifier fields.
     *
     * @param businessId the business this income belongs to
     * @param date the date of the income
     * @param amount the income amount
     * @param description description of the income
     * @param category the income category (SALES, OTHER_INCOME)
     * @param reference optional general reference
     * @param bankTransactionRef optional bank statement transaction reference
     * @param invoiceNumber optional invoice number
     * @param receiptPath optional path to receipt/proof document
     */
    public static Income create(
            UUID businessId,
            LocalDate date,
            BigDecimal amount,
            String description,
            IncomeCategory category,
            String reference,
            String bankTransactionRef,
            String invoiceNumber,
            String receiptPath) {
        return new Income(
            UUID.randomUUID(),
            businessId,
            date,
            amount,
            description,
            category,
            reference,
            bankTransactionRef,
            invoiceNumber,
            receiptPath
        );
    }

    private static void validateBusinessId(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null");
        }
    }

    private static void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Income date cannot be null");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Income amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Income amount must be positive");
        }
    }

    private static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Income description cannot be null or empty");
        }
    }

    private static void validateCategory(IncomeCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Income category cannot be null");
        }
    }
}
