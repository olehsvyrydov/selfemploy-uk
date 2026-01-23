package uk.selfemploy.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for storing and managing receipt files attached to expenses.
 * Receipts are stored in the file system with UUID-based names to avoid conflicts.
 */
@ApplicationScoped
public class ReceiptStorageService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptStorageService.class);

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final int MAX_RECEIPTS_PER_EXPENSE = 5;

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
        "image/jpeg",
        "image/png",
        "image/gif",
        "application/pdf"
    );

    // Magic byte patterns for file type detection
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF_MAGIC = new byte[]{0x47, 0x49, 0x46, 0x38};
    private static final byte[] PDF_MAGIC = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-

    private final Path storageDirectory;

    // In-memory index of receipts (in production, this would be a database)
    private final Map<UUID, ReceiptMetadata> receiptsById = new ConcurrentHashMap<>();
    private final Map<UUID, List<UUID>> receiptsByExpenseId = new ConcurrentHashMap<>();

    @Inject
    public ReceiptStorageService(
            @ConfigProperty(name = "selfemploy.receipts.path", defaultValue = "") String storagePath) {
        this.storageDirectory = resolveStorageDirectory(storagePath);
        initializeStorageDirectory();
    }

    /**
     * Constructor with explicit storage path.
     * Useful for standalone mode or testing.
     */
    public ReceiptStorageService(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
        initializeStorageDirectory();
    }

    /**
     * Stores a receipt file for an expense.
     *
     * @param expenseId       The expense to attach the receipt to
     * @param sourceFile      Path to the source file
     * @param originalFilename The original filename
     * @return Metadata about the stored receipt
     * @throws ReceiptStorageException if storage fails
     */
    public ReceiptMetadata storeReceipt(UUID expenseId, Path sourceFile, String originalFilename) {
        log.debug("Storing receipt for expense {}: {}", expenseId, originalFilename);

        // Validate file size
        try {
            long fileSize = Files.size(sourceFile);
            if (fileSize > MAX_FILE_SIZE) {
                throw new ReceiptStorageException(
                    ReceiptStorageException.ErrorType.FILE_TOO_LARGE,
                    String.format("File size %d bytes exceeds maximum size %d bytes", fileSize, MAX_FILE_SIZE)
                );
            }
        } catch (IOException e) {
            throw new ReceiptStorageException(
                ReceiptStorageException.ErrorType.STORAGE_ERROR,
                "Failed to read file size", e
            );
        }

        // Check receipt count limit
        List<UUID> existingReceipts = receiptsByExpenseId.getOrDefault(expenseId, List.of());
        if (existingReceipts.size() >= MAX_RECEIPTS_PER_EXPENSE) {
            throw new ReceiptStorageException(
                ReceiptStorageException.ErrorType.MAX_RECEIPTS_EXCEEDED,
                String.format("Maximum receipts (%d) reached for expense", MAX_RECEIPTS_PER_EXPENSE)
            );
        }

        // Validate file content (MIME type)
        String mimeType = detectMimeType(sourceFile);
        if (mimeType == null || !SUPPORTED_MIME_TYPES.contains(mimeType)) {
            throw new ReceiptStorageException(
                ReceiptStorageException.ErrorType.UNSUPPORTED_FORMAT,
                String.format("Unsupported file format: %s", mimeType != null ? mimeType : "unknown")
            );
        }

        // Generate UUID-based storage path
        UUID receiptId = UUID.randomUUID();
        String extension = getExtension(originalFilename, mimeType);
        String storedFileName = receiptId.toString() + extension;
        Path targetPath = storageDirectory.resolve(storedFileName);

        // Copy file to storage
        try {
            Files.copy(sourceFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Receipt stored: {} -> {}", originalFilename, targetPath);
        } catch (IOException e) {
            throw new ReceiptStorageException(
                ReceiptStorageException.ErrorType.STORAGE_ERROR,
                "Failed to copy file to storage", e
            );
        }

        // Create and store metadata
        long fileSize;
        try {
            fileSize = Files.size(targetPath);
        } catch (IOException e) {
            fileSize = 0;
        }

        ReceiptMetadata metadata = new ReceiptMetadata(
            receiptId,
            expenseId,
            originalFilename,
            targetPath,
            mimeType,
            fileSize,
            java.time.Instant.now()
        );

        // Update indexes
        receiptsById.put(receiptId, metadata);
        receiptsByExpenseId.computeIfAbsent(expenseId, k -> new ArrayList<>()).add(receiptId);

        return metadata;
    }

    /**
     * Lists all receipts for an expense.
     *
     * @param expenseId The expense ID
     * @return List of receipt metadata (may be empty)
     */
    public List<ReceiptMetadata> listReceipts(UUID expenseId) {
        List<UUID> receiptIds = receiptsByExpenseId.getOrDefault(expenseId, List.of());
        return receiptIds.stream()
            .map(receiptsById::get)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Gets a receipt by its ID.
     *
     * @param receiptId The receipt ID
     * @return Optional containing the metadata if found
     */
    public Optional<ReceiptMetadata> getReceipt(UUID receiptId) {
        return Optional.ofNullable(receiptsById.get(receiptId));
    }

    /**
     * Deletes a receipt.
     *
     * @param receiptId The receipt ID to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteReceipt(UUID receiptId) {
        ReceiptMetadata metadata = receiptsById.get(receiptId);
        if (metadata == null) {
            return false;
        }

        // Delete file
        try {
            Files.deleteIfExists(metadata.storagePath());
            log.info("Receipt file deleted: {}", metadata.storagePath());
        } catch (IOException e) {
            log.error("Failed to delete receipt file: {}", metadata.storagePath(), e);
            // Continue with removing from indexes even if file deletion fails
        }

        // Update indexes
        receiptsById.remove(receiptId);
        List<UUID> expenseReceipts = receiptsByExpenseId.get(metadata.expenseId());
        if (expenseReceipts != null) {
            expenseReceipts.remove(receiptId);
            if (expenseReceipts.isEmpty()) {
                receiptsByExpenseId.remove(metadata.expenseId());
            }
        }

        return true;
    }

    /**
     * Gets the storage directory path.
     */
    public Path getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Reassociates receipts from a temporary expense ID to the actual expense ID.
     * Used when creating a new expense where receipts are attached before the expense is saved.
     *
     * @param tempExpenseId The temporary expense ID used during attachment
     * @param actualExpenseId The actual expense ID after saving
     */
    public void reassociateReceipts(UUID tempExpenseId, UUID actualExpenseId) {
        if (tempExpenseId == null || actualExpenseId == null || tempExpenseId.equals(actualExpenseId)) {
            return;
        }

        List<UUID> receiptIds = receiptsByExpenseId.remove(tempExpenseId);
        if (receiptIds == null || receiptIds.isEmpty()) {
            return;
        }

        log.debug("Reassociating {} receipts from {} to {}", receiptIds.size(), tempExpenseId, actualExpenseId);

        // Update each receipt's metadata with the new expense ID
        for (UUID receiptId : receiptIds) {
            ReceiptMetadata oldMetadata = receiptsById.get(receiptId);
            if (oldMetadata != null) {
                ReceiptMetadata newMetadata = new ReceiptMetadata(
                    oldMetadata.receiptId(),
                    actualExpenseId,
                    oldMetadata.originalFilename(),
                    oldMetadata.storagePath(),
                    oldMetadata.mimeType(),
                    oldMetadata.fileSize(),
                    oldMetadata.uploadedAt()
                );
                receiptsById.put(receiptId, newMetadata);
            }
        }

        // Add to new expense's receipt list
        receiptsByExpenseId.computeIfAbsent(actualExpenseId, k -> new ArrayList<>()).addAll(receiptIds);
    }

    // === Private Helper Methods ===

    private void initializeStorageDirectory() {
        try {
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
                log.info("Created receipt storage directory: {}", storageDirectory);
            }
        } catch (IOException e) {
            log.error("Failed to create receipt storage directory: {}", storageDirectory, e);
        }
    }

    private Path resolveStorageDirectory(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Paths.get(configuredPath);
        }

        // Default to user's data directory
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        Path basePath;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            basePath = appData != null
                ? Paths.get(appData, "SelfEmployment")
                : Paths.get(userHome, "AppData", "Roaming", "SelfEmployment");
        } else if (os.contains("mac")) {
            basePath = Paths.get(userHome, "Library", "Application Support", "SelfEmployment");
        } else {
            String xdgData = System.getenv("XDG_DATA_HOME");
            basePath = xdgData != null
                ? Paths.get(xdgData, "selfemployment")
                : Paths.get(userHome, ".local", "share", "selfemployment");
        }

        return basePath.resolve("receipts");
    }

    private String detectMimeType(Path file) {
        try {
            byte[] header = new byte[8];
            try (InputStream is = Files.newInputStream(file)) {
                int read = is.read(header);
                if (read < 4) {
                    return null;
                }
            }

            // Check magic bytes - we only accept files that match our known formats
            // This ensures we validate actual content, not just file extension
            if (startsWith(header, JPEG_MAGIC)) {
                return "image/jpeg";
            } else if (startsWith(header, PNG_MAGIC)) {
                return "image/png";
            } else if (startsWith(header, GIF_MAGIC)) {
                return "image/gif";
            } else if (startsWith(header, PDF_MAGIC)) {
                return "application/pdf";
            }

            // If magic bytes don't match any supported format, return null
            // This prevents accepting files with wrong extension masquerading as images
            return null;
        } catch (IOException e) {
            log.debug("Failed to detect MIME type for: {}", file, e);
            return null;
        }
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String getExtension(String filename, String mimeType) {
        // Try to get extension from filename first
        if (filename != null) {
            int lastDot = filename.lastIndexOf('.');
            if (lastDot >= 0) {
                return filename.substring(lastDot).toLowerCase();
            }
        }

        // Fall back to MIME type
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "application/pdf" -> ".pdf";
            default -> "";
        };
    }
}
