package uk.selfemploy.core.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/**
 * Metadata for a stored receipt file.
 *
 * @param receiptId       Unique identifier for the receipt
 * @param expenseId       ID of the expense this receipt belongs to
 * @param originalFilename Original filename provided by user
 * @param storagePath     Full path where the file is stored
 * @param mimeType        MIME type of the file (e.g., "image/jpeg")
 * @param fileSize        Size of the file in bytes
 * @param uploadedAt      Timestamp when the receipt was uploaded
 */
public record ReceiptMetadata(
    UUID receiptId,
    UUID expenseId,
    String originalFilename,
    Path storagePath,
    String mimeType,
    long fileSize,
    Instant uploadedAt
) {
    /**
     * Creates metadata for a new receipt.
     */
    public static ReceiptMetadata create(UUID expenseId, String originalFilename,
                                          Path storagePath, String mimeType, long fileSize) {
        return new ReceiptMetadata(
            UUID.randomUUID(),
            expenseId,
            originalFilename,
            storagePath,
            mimeType,
            fileSize,
            Instant.now()
        );
    }

    /**
     * Gets the file extension from the original filename.
     */
    public String getExtension() {
        if (originalFilename == null) {
            return "";
        }
        int lastDot = originalFilename.lastIndexOf('.');
        return lastDot >= 0 ? originalFilename.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * Checks if this receipt is an image file.
     */
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Checks if this receipt is a PDF file.
     */
    public boolean isPdf() {
        return "application/pdf".equals(mimeType);
    }

    /**
     * Gets a human-readable file size string.
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
}
