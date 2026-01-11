package uk.selfemploy.common.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a batch of transactions imported from a bank CSV file.
 *
 * <p>Tracks metadata about the import for auditing and duplicate detection.</p>
 */
public record ImportBatch(
    UUID id,
    UUID businessId,
    String bankName,
    String fileName,
    long fileSizeBytes,
    int totalTransactions,
    int incomeCount,
    int expenseCount,
    int duplicateCount,
    Instant importedAt
) {
    /**
     * Compact constructor for validation.
     */
    public ImportBatch {
        validateBusinessId(businessId);
        validateBankName(bankName);
        validateFileName(fileName);
        validateFileSizeBytes(fileSizeBytes);
        validateCounts(totalTransactions, incomeCount, expenseCount, duplicateCount);
        validateId(id);
        validateImportedAt(importedAt);
    }

    /**
     * Creates a new import batch with a generated ID and current timestamp.
     */
    public static ImportBatch create(
            UUID businessId,
            String bankName,
            String fileName,
            long fileSizeBytes,
            int totalTransactions,
            int incomeCount,
            int expenseCount,
            int duplicateCount) {
        return new ImportBatch(
            UUID.randomUUID(),
            businessId,
            bankName,
            fileName,
            fileSizeBytes,
            totalTransactions,
            incomeCount,
            expenseCount,
            duplicateCount,
            Instant.now()
        );
    }

    private static void validateId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
    }

    private static void validateBusinessId(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null");
        }
    }

    private static void validateBankName(String bankName) {
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("bankName cannot be null or blank");
        }
    }

    private static void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName cannot be null or blank");
        }
    }

    private static void validateFileSizeBytes(long fileSizeBytes) {
        if (fileSizeBytes < 0) {
            throw new IllegalArgumentException("fileSizeBytes cannot be negative");
        }
    }

    private static void validateCounts(int totalTransactions, int incomeCount, int expenseCount, int duplicateCount) {
        if (totalTransactions < 0) {
            throw new IllegalArgumentException("totalTransactions cannot be negative");
        }
        if (incomeCount < 0) {
            throw new IllegalArgumentException("incomeCount cannot be negative");
        }
        if (expenseCount < 0) {
            throw new IllegalArgumentException("expenseCount cannot be negative");
        }
        if (duplicateCount < 0) {
            throw new IllegalArgumentException("duplicateCount cannot be negative");
        }
    }

    private static void validateImportedAt(Instant importedAt) {
        if (importedAt == null) {
            throw new IllegalArgumentException("importedAt cannot be null");
        }
    }
}
