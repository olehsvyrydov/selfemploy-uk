package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;
import uk.selfemploy.common.domain.TransactionModificationLog;
import uk.selfemploy.common.enums.ModificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the transaction modification log.
 *
 * <p>Maps the transaction_modification_log table created in V18 migration.
 * Each entry records a single modification to a bank transaction, forming
 * an immutable audit trail for MTD compliance.</p>
 */
@Entity
@Table(name = "transaction_modification_log")
public class TransactionModificationLogEntity {

    @Id
    private UUID id;

    @Column(name = "bank_transaction_id", nullable = false)
    private UUID bankTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "modification_type", nullable = false, length = 50)
    private ModificationType modificationType;

    @Column(name = "field_name", length = 50)
    private String fieldName;

    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "modified_by", nullable = false)
    private String modifiedBy;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    public TransactionModificationLogEntity() {}

    /**
     * Creates a JPA entity from a domain TransactionModificationLog.
     */
    public static TransactionModificationLogEntity fromDomain(TransactionModificationLog log) {
        TransactionModificationLogEntity entity = new TransactionModificationLogEntity();
        entity.id = log.id();
        entity.bankTransactionId = log.bankTransactionId();
        entity.modificationType = log.modificationType();
        entity.fieldName = log.fieldName();
        entity.previousValue = log.previousValue();
        entity.newValue = log.newValue();
        entity.modifiedBy = log.modifiedBy();
        entity.modifiedAt = log.modifiedAt();
        return entity;
    }

    /**
     * Converts this entity to a domain TransactionModificationLog.
     */
    public TransactionModificationLog toDomain() {
        return new TransactionModificationLog(
            id, bankTransactionId, modificationType,
            fieldName, previousValue, newValue,
            modifiedBy, modifiedAt
        );
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBankTransactionId() { return bankTransactionId; }
    public void setBankTransactionId(UUID bankTransactionId) { this.bankTransactionId = bankTransactionId; }

    public ModificationType getModificationType() { return modificationType; }
    public void setModificationType(ModificationType modificationType) { this.modificationType = modificationType; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }
}
