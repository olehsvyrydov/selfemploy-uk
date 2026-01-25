package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for Expense.
 */
@Entity
@Table(name = "expenses")
public class ExpenseEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @Column(name = "receipt_path")
    private String receiptPath;

    @Column(length = 1000)
    private String notes;

    // Soft delete support (Sprint 10B)
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "deletion_reason")
    private String deletionReason;

    // Default constructor for JPA
    public ExpenseEntity() {}

    /**
     * Creates a JPA entity from a domain Expense.
     */
    public static ExpenseEntity fromDomain(Expense expense) {
        ExpenseEntity entity = new ExpenseEntity();
        entity.id = expense.id();
        entity.businessId = expense.businessId();
        entity.date = expense.date();
        entity.amount = expense.amount();
        entity.description = expense.description();
        entity.category = expense.category();
        entity.receiptPath = expense.receiptPath();
        entity.notes = expense.notes();
        return entity;
    }

    /**
     * Converts this entity to a domain Expense.
     */
    public Expense toDomain() {
        return new Expense(
            id,
            businessId,
            date,
            amount,
            description,
            category,
            receiptPath,
            notes
        );
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID businessId) { this.businessId = businessId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }
    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Soft delete getters and setters
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
    public String getDeletionReason() { return deletionReason; }
    public void setDeletionReason(String deletionReason) { this.deletionReason = deletionReason; }

    /**
     * Checks if this record has been soft deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft deletes this record.
     */
    public void softDelete(Instant timestamp, String deletedBy, String reason) {
        this.deletedAt = timestamp;
        this.deletedBy = deletedBy;
        this.deletionReason = reason;
    }

    /**
     * Restores a soft-deleted record.
     */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
        this.deletionReason = null;
    }
}
