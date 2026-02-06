package uk.selfemploy.common.domain;

import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents an imported bank transaction in the staging/review workflow.
 *
 * <p>BankTransaction is a staging entity for the bank import review workflow.
 * Transactions are imported here first, then promoted to Income or Expense
 * records after user review.</p>
 *
 * <p>State machine: PENDING → CATEGORIZED | EXCLUDED | SKIPPED</p>
 *
 * <p>Digital link chain for MTD audit trail:
 * Original File → ImportAudit → BankTransaction → Income/Expense → AnnualSubmission</p>
 */
public record BankTransaction(
    UUID id,
    UUID businessId,
    UUID importAuditId,
    String sourceFormatId,
    LocalDate date,
    BigDecimal amount,
    String description,
    String accountLastFour,
    String bankTransactionId,
    String transactionHash,
    ReviewStatus reviewStatus,
    UUID incomeId,
    UUID expenseId,
    String exclusionReason,
    Boolean isBusiness,
    BigDecimal confidenceScore,
    ExpenseCategory suggestedCategory,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt,
    String deletedBy,
    String deletionReason
) {

    /**
     * Compact constructor for validation.
     */
    public BankTransaction {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null");
        }
        if (importAuditId == null) {
            throw new IllegalArgumentException("importAuditId cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("date cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be null or empty");
        }
        if (transactionHash == null || transactionHash.isBlank()) {
            throw new IllegalArgumentException("transactionHash cannot be null or empty");
        }
        if (reviewStatus == null) {
            throw new IllegalArgumentException("reviewStatus cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
    }

    /**
     * Creates a new pending bank transaction from an import.
     *
     * @param businessId the business this transaction belongs to
     * @param importAuditId the import audit record that created this
     * @param sourceFormatId nullable format identifier (e.g., "csv-barclays")
     * @param date transaction date
     * @param amount transaction amount (positive=income, negative=expense)
     * @param description transaction description
     * @param accountLastFour last 4 digits of account (GDPR data minimization)
     * @param bankTransactionId bank's own transaction identifier
     * @param transactionHash SHA-256 hash for duplicate detection
     * @param now current timestamp
     */
    public static BankTransaction create(
            UUID businessId,
            UUID importAuditId,
            String sourceFormatId,
            LocalDate date,
            BigDecimal amount,
            String description,
            String accountLastFour,
            String bankTransactionId,
            String transactionHash,
            Instant now) {
        return new BankTransaction(
            UUID.randomUUID(),
            businessId,
            importAuditId,
            sourceFormatId,
            date,
            amount,
            description,
            accountLastFour,
            bankTransactionId,
            transactionHash,
            ReviewStatus.PENDING,
            null,
            null,
            null,
            null,
            null,
            null,
            now,
            now,
            null,
            null,
            null
        );
    }

    /**
     * Returns true if this transaction represents income (positive amount).
     * Direction-based classification: positive = income, negative = expense.
     */
    public boolean isIncome() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns true if this transaction represents an expense (negative amount).
     * Direction-based classification: positive = income, negative = expense.
     */
    public boolean isExpense() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Returns the absolute amount for display purposes.
     */
    public BigDecimal absoluteAmount() {
        return amount.abs();
    }

    /**
     * Returns true if this transaction has been soft deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Returns true if this transaction has been reviewed.
     */
    public boolean isReviewed() {
        return reviewStatus.isReviewed();
    }

    /**
     * Creates a copy with the transaction categorized as income.
     */
    public BankTransaction withCategorizedAsIncome(UUID incomeId, Instant now) {
        return new BankTransaction(
            id, businessId, importAuditId, sourceFormatId, date, amount,
            description, accountLastFour, bankTransactionId, transactionHash,
            ReviewStatus.CATEGORIZED, incomeId, null, null,
            isBusiness, confidenceScore, suggestedCategory,
            createdAt, now, deletedAt, deletedBy, deletionReason
        );
    }

    /**
     * Creates a copy with the transaction categorized as expense.
     */
    public BankTransaction withCategorizedAsExpense(UUID expenseId, Instant now) {
        return new BankTransaction(
            id, businessId, importAuditId, sourceFormatId, date, amount,
            description, accountLastFour, bankTransactionId, transactionHash,
            ReviewStatus.CATEGORIZED, null, expenseId, null,
            isBusiness, confidenceScore, suggestedCategory,
            createdAt, now, deletedAt, deletedBy, deletionReason
        );
    }

    /**
     * Creates a copy with the transaction excluded.
     */
    public BankTransaction withExcluded(String reason, Instant now) {
        return new BankTransaction(
            id, businessId, importAuditId, sourceFormatId, date, amount,
            description, accountLastFour, bankTransactionId, transactionHash,
            ReviewStatus.EXCLUDED, null, null, reason,
            isBusiness, confidenceScore, suggestedCategory,
            createdAt, now, deletedAt, deletedBy, deletionReason
        );
    }

    /**
     * Creates a copy with business/personal flag set.
     * Nullable isBusiness: null = uncategorized, true = business, false = personal.
     */
    public BankTransaction withBusinessFlag(Boolean isBusiness, Instant now) {
        return new BankTransaction(
            id, businessId, importAuditId, sourceFormatId, date, amount,
            description, accountLastFour, bankTransactionId, transactionHash,
            reviewStatus, incomeId, expenseId, exclusionReason,
            isBusiness, confidenceScore, suggestedCategory,
            createdAt, now, deletedAt, deletedBy, deletionReason
        );
    }

    /**
     * Creates a copy with classification suggestion.
     * Adds an auto-categorization suggestion with confidence score.
     */
    public BankTransaction withSuggestion(ExpenseCategory category, BigDecimal confidence, Instant now) {
        return new BankTransaction(
            id, businessId, importAuditId, sourceFormatId, date, amount,
            description, accountLastFour, bankTransactionId, transactionHash,
            reviewStatus, incomeId, expenseId, exclusionReason,
            isBusiness, confidence, category,
            createdAt, now, deletedAt, deletedBy, deletionReason
        );
    }

    /**
     * Creates a soft-deleted copy.
     */
    public BankTransaction withSoftDelete(Instant timestamp, String deletedBy, String reason) {
        return new BankTransaction(
            id, businessId, importAuditId, sourceFormatId, date, amount,
            description, accountLastFour, bankTransactionId, transactionHash,
            reviewStatus, incomeId, expenseId, exclusionReason,
            isBusiness, confidenceScore, suggestedCategory,
            createdAt, timestamp, timestamp, deletedBy, reason
        );
    }
}
