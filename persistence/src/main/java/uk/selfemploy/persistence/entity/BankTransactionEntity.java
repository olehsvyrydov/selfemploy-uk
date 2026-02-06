package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for BankTransaction staging records.
 *
 * <p>Maps the bank_transactions table created in V17 migration.</p>
 */
@Entity
@Table(name = "bank_transactions")
public class BankTransactionEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "import_audit_id", nullable = false)
    private UUID importAuditId;

    @Column(name = "source_format_id", length = 50)
    private String sourceFormatId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "account_last_four", length = 4)
    private String accountLastFour;

    @Column(name = "bank_transaction_id", length = 100)
    private String bankTransactionId;

    @Column(name = "transaction_hash", nullable = false, length = 64)
    private String transactionHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    private ReviewStatus reviewStatus;

    @Column(name = "income_id")
    private UUID incomeId;

    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "exclusion_reason", length = 200)
    private String exclusionReason;

    @Column(name = "is_business")
    private Boolean isBusiness;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_category", length = 50)
    private ExpenseCategory suggestedCategory;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    public BankTransactionEntity() {}

    /**
     * Creates a JPA entity from a domain BankTransaction.
     */
    public static BankTransactionEntity fromDomain(BankTransaction tx) {
        BankTransactionEntity entity = new BankTransactionEntity();
        entity.id = tx.id();
        entity.businessId = tx.businessId();
        entity.importAuditId = tx.importAuditId();
        entity.sourceFormatId = tx.sourceFormatId();
        entity.date = tx.date();
        entity.amount = tx.amount();
        entity.description = tx.description();
        entity.accountLastFour = tx.accountLastFour();
        entity.bankTransactionId = tx.bankTransactionId();
        entity.transactionHash = tx.transactionHash();
        entity.reviewStatus = tx.reviewStatus();
        entity.incomeId = tx.incomeId();
        entity.expenseId = tx.expenseId();
        entity.exclusionReason = tx.exclusionReason();
        entity.isBusiness = tx.isBusiness();
        entity.confidenceScore = tx.confidenceScore();
        entity.suggestedCategory = tx.suggestedCategory();
        entity.createdAt = tx.createdAt();
        entity.updatedAt = tx.updatedAt();
        entity.deletedAt = tx.deletedAt();
        entity.deletedBy = tx.deletedBy();
        entity.deletionReason = tx.deletionReason();
        return entity;
    }

    /**
     * Converts this entity to a domain BankTransaction.
     */
    public BankTransaction toDomain() {
        return new BankTransaction(
            id, businessId, importAuditId, sourceFormatId,
            date, amount, description, accountLastFour,
            bankTransactionId, transactionHash,
            reviewStatus, incomeId, expenseId, exclusionReason,
            isBusiness, confidenceScore, suggestedCategory,
            createdAt, updatedAt, deletedAt, deletedBy, deletionReason
        );
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete(Instant timestamp, String deletedBy, String reason) {
        this.deletedAt = timestamp;
        this.deletedBy = deletedBy;
        this.deletionReason = reason;
    }

    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
        this.deletionReason = null;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID businessId) { this.businessId = businessId; }
    public UUID getImportAuditId() { return importAuditId; }
    public void setImportAuditId(UUID importAuditId) { this.importAuditId = importAuditId; }
    public String getSourceFormatId() { return sourceFormatId; }
    public void setSourceFormatId(String sourceFormatId) { this.sourceFormatId = sourceFormatId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAccountLastFour() { return accountLastFour; }
    public void setAccountLastFour(String accountLastFour) { this.accountLastFour = accountLastFour; }
    public String getBankTransactionId() { return bankTransactionId; }
    public void setBankTransactionId(String bankTransactionId) { this.bankTransactionId = bankTransactionId; }
    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public UUID getIncomeId() { return incomeId; }
    public void setIncomeId(UUID incomeId) { this.incomeId = incomeId; }
    public UUID getExpenseId() { return expenseId; }
    public void setExpenseId(UUID expenseId) { this.expenseId = expenseId; }
    public String getExclusionReason() { return exclusionReason; }
    public void setExclusionReason(String exclusionReason) { this.exclusionReason = exclusionReason; }
    public Boolean getIsBusiness() { return isBusiness; }
    public void setIsBusiness(Boolean isBusiness) { this.isBusiness = isBusiness; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public ExpenseCategory getSuggestedCategory() { return suggestedCategory; }
    public void setSuggestedCategory(ExpenseCategory suggestedCategory) { this.suggestedCategory = suggestedCategory; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
    public String getDeletionReason() { return deletionReason; }
    public void setDeletionReason(String deletionReason) { this.deletionReason = deletionReason; }
}
