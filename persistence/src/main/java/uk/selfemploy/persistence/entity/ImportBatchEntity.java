package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;
import uk.selfemploy.common.domain.ImportBatch;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for ImportBatch.
 */
@Entity
@Table(name = "import_batches")
public class ImportBatchEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "total_transactions", nullable = false)
    private int totalTransactions;

    @Column(name = "income_count", nullable = false)
    private int incomeCount;

    @Column(name = "expense_count", nullable = false)
    private int expenseCount;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    // Default constructor for JPA
    public ImportBatchEntity() {}

    /**
     * Creates a JPA entity from a domain ImportBatch.
     */
    public static ImportBatchEntity fromDomain(ImportBatch batch) {
        ImportBatchEntity entity = new ImportBatchEntity();
        entity.id = batch.id();
        entity.businessId = batch.businessId();
        entity.bankName = batch.bankName();
        entity.fileName = batch.fileName();
        entity.fileSizeBytes = batch.fileSizeBytes();
        entity.totalTransactions = batch.totalTransactions();
        entity.incomeCount = batch.incomeCount();
        entity.expenseCount = batch.expenseCount();
        entity.duplicateCount = batch.duplicateCount();
        entity.importedAt = batch.importedAt();
        return entity;
    }

    /**
     * Converts this entity to a domain ImportBatch.
     */
    public ImportBatch toDomain() {
        return new ImportBatch(
            id,
            businessId,
            bankName,
            fileName,
            fileSizeBytes,
            totalTransactions,
            incomeCount,
            expenseCount,
            duplicateCount,
            importedAt
        );
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID businessId) { this.businessId = businessId; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public int getTotalTransactions() { return totalTransactions; }
    public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
    public int getIncomeCount() { return incomeCount; }
    public void setIncomeCount(int incomeCount) { this.incomeCount = incomeCount; }
    public int getExpenseCount() { return expenseCount; }
    public void setExpenseCount(int expenseCount) { this.expenseCount = expenseCount; }
    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
    public Instant getImportedAt() { return importedAt; }
    public void setImportedAt(Instant importedAt) { this.importedAt = importedAt; }
}
