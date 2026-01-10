package uk.selfemploy.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ReceiptStorageService.
 */
@DisplayName("ReceiptStorageService")
class ReceiptStorageServiceTest {

    @TempDir
    Path tempDir;

    private ReceiptStorageService service;

    @BeforeEach
    void setup() {
        service = new ReceiptStorageService(tempDir);
    }

    @Nested
    @DisplayName("Store Receipt")
    class StoreReceipt {

        @Test
        @DisplayName("should store JPG file and return metadata")
        void shouldStoreJpgFileAndReturnMetadata() throws IOException {
            // Given
            Path sourceFile = createTestFile("receipt.jpg", createJpegBytes());
            UUID expenseId = UUID.randomUUID();

            // When
            ReceiptMetadata metadata = service.storeReceipt(expenseId, sourceFile, "receipt.jpg");

            // Then
            assertThat(metadata).isNotNull();
            assertThat(metadata.receiptId()).isNotNull();
            assertThat(metadata.originalFilename()).isEqualTo("receipt.jpg");
            assertThat(metadata.expenseId()).isEqualTo(expenseId);
            assertThat(metadata.mimeType()).isEqualTo("image/jpeg");
            assertThat(Files.exists(metadata.storagePath())).isTrue();
        }

        @Test
        @DisplayName("should store PNG file and return metadata")
        void shouldStorePngFileAndReturnMetadata() throws IOException {
            // Given
            Path sourceFile = createTestFile("receipt.png", createPngBytes());
            UUID expenseId = UUID.randomUUID();

            // When
            ReceiptMetadata metadata = service.storeReceipt(expenseId, sourceFile, "receipt.png");

            // Then
            assertThat(metadata.mimeType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("should store PDF file and return metadata")
        void shouldStorePdfFileAndReturnMetadata() throws IOException {
            // Given
            Path sourceFile = createTestFile("receipt.pdf", createPdfBytes());
            UUID expenseId = UUID.randomUUID();

            // When
            ReceiptMetadata metadata = service.storeReceipt(expenseId, sourceFile, "receipt.pdf");

            // Then
            assertThat(metadata.mimeType()).isEqualTo("application/pdf");
        }

        @Test
        @DisplayName("should rename file to UUID to avoid conflicts")
        void shouldRenameFileToUuidToAvoidConflicts() throws IOException {
            // Given
            Path sourceFile = createTestFile("receipt.jpg", createJpegBytes());
            UUID expenseId = UUID.randomUUID();

            // When
            ReceiptMetadata metadata = service.storeReceipt(expenseId, sourceFile, "receipt.jpg");

            // Then - stored file should have UUID-based name
            String storedFileName = metadata.storagePath().getFileName().toString();
            assertThat(storedFileName).doesNotContain("receipt");
            assertThat(storedFileName).endsWith(".jpg");
        }

        @Test
        @DisplayName("should reject file exceeding 10MB limit")
        void shouldRejectFileExceeding10MbLimit() throws IOException {
            // Given - create file larger than 10MB
            byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
            Path sourceFile = createTestFile("large.jpg", largeContent);
            UUID expenseId = UUID.randomUUID();

            // When/Then
            assertThatThrownBy(() -> service.storeReceipt(expenseId, sourceFile, "large.jpg"))
                .isInstanceOf(ReceiptStorageException.class)
                .hasMessageContaining("exceeds maximum size");
        }

        @Test
        @DisplayName("should reject unsupported file format")
        void shouldRejectUnsupportedFileFormat() throws IOException {
            // Given - text file is not supported
            Path sourceFile = createTestFile("notes.txt", "Some text content".getBytes());
            UUID expenseId = UUID.randomUUID();

            // When/Then
            assertThatThrownBy(() -> service.storeReceipt(expenseId, sourceFile, "notes.txt"))
                .isInstanceOf(ReceiptStorageException.class)
                .hasMessageContaining("Unsupported file format");
        }

        @Test
        @DisplayName("should validate actual content not just extension")
        void shouldValidateActualContentNotJustExtension() throws IOException {
            // Given - file with jpg extension but text content
            Path sourceFile = createTestFile("fake.jpg", "This is not an image".getBytes());
            UUID expenseId = UUID.randomUUID();

            // When/Then
            assertThatThrownBy(() -> service.storeReceipt(expenseId, sourceFile, "fake.jpg"))
                .isInstanceOf(ReceiptStorageException.class)
                .hasMessageContaining("Unsupported file format");
        }

        @Test
        @DisplayName("should support GIF format")
        void shouldSupportGifFormat() throws IOException {
            // Given
            Path sourceFile = createTestFile("receipt.gif", createGifBytes());
            UUID expenseId = UUID.randomUUID();

            // When
            ReceiptMetadata metadata = service.storeReceipt(expenseId, sourceFile, "receipt.gif");

            // Then
            assertThat(metadata.mimeType()).isEqualTo("image/gif");
        }
    }

    @Nested
    @DisplayName("List Receipts")
    class ListReceipts {

        @Test
        @DisplayName("should list all receipts for expense")
        void shouldListAllReceiptsForExpense() throws IOException {
            // Given
            UUID expenseId = UUID.randomUUID();
            service.storeReceipt(expenseId, createTestFile("r1.jpg", createJpegBytes()), "r1.jpg");
            service.storeReceipt(expenseId, createTestFile("r2.png", createPngBytes()), "r2.png");

            // When
            List<ReceiptMetadata> receipts = service.listReceipts(expenseId);

            // Then
            assertThat(receipts).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no receipts exist")
        void shouldReturnEmptyListWhenNoReceiptsExist() {
            // Given
            UUID expenseId = UUID.randomUUID();

            // When
            List<ReceiptMetadata> receipts = service.listReceipts(expenseId);

            // Then
            assertThat(receipts).isEmpty();
        }

        @Test
        @DisplayName("should only return receipts for specific expense")
        void shouldOnlyReturnReceiptsForSpecificExpense() throws IOException {
            // Given
            UUID expenseId1 = UUID.randomUUID();
            UUID expenseId2 = UUID.randomUUID();
            service.storeReceipt(expenseId1, createTestFile("r1.jpg", createJpegBytes()), "r1.jpg");
            service.storeReceipt(expenseId2, createTestFile("r2.jpg", createJpegBytes()), "r2.jpg");

            // When
            List<ReceiptMetadata> receipts = service.listReceipts(expenseId1);

            // Then
            assertThat(receipts).hasSize(1);
            assertThat(receipts.get(0).expenseId()).isEqualTo(expenseId1);
        }
    }

    @Nested
    @DisplayName("Get Receipt")
    class GetReceipt {

        @Test
        @DisplayName("should return receipt by ID")
        void shouldReturnReceiptById() throws IOException {
            // Given
            UUID expenseId = UUID.randomUUID();
            ReceiptMetadata stored = service.storeReceipt(
                expenseId, createTestFile("receipt.jpg", createJpegBytes()), "receipt.jpg");

            // When
            Optional<ReceiptMetadata> retrieved = service.getReceipt(stored.receiptId());

            // Then
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().receiptId()).isEqualTo(stored.receiptId());
        }

        @Test
        @DisplayName("should return empty when receipt not found")
        void shouldReturnEmptyWhenReceiptNotFound() {
            // When
            Optional<ReceiptMetadata> result = service.getReceipt(UUID.randomUUID());

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Delete Receipt")
    class DeleteReceipt {

        @Test
        @DisplayName("should delete receipt and file")
        void shouldDeleteReceiptAndFile() throws IOException {
            // Given
            UUID expenseId = UUID.randomUUID();
            ReceiptMetadata stored = service.storeReceipt(
                expenseId, createTestFile("receipt.jpg", createJpegBytes()), "receipt.jpg");
            Path storagePath = stored.storagePath();

            // When
            boolean deleted = service.deleteReceipt(stored.receiptId());

            // Then
            assertThat(deleted).isTrue();
            assertThat(Files.exists(storagePath)).isFalse();
            assertThat(service.getReceipt(stored.receiptId())).isEmpty();
        }

        @Test
        @DisplayName("should return false when receipt not found")
        void shouldReturnFalseWhenReceiptNotFound() {
            // When
            boolean deleted = service.deleteReceipt(UUID.randomUUID());

            // Then
            assertThat(deleted).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Receipts Limit")
    class MultipleReceiptsLimit {

        @Test
        @DisplayName("should allow up to 5 receipts per expense")
        void shouldAllowUpTo5ReceiptsPerExpense() throws IOException {
            // Given
            UUID expenseId = UUID.randomUUID();

            // When
            for (int i = 0; i < 5; i++) {
                service.storeReceipt(
                    expenseId,
                    createTestFile("receipt" + i + ".jpg", createJpegBytes()),
                    "receipt" + i + ".jpg"
                );
            }

            // Then
            assertThat(service.listReceipts(expenseId)).hasSize(5);
        }

        @Test
        @DisplayName("should reject 6th receipt for same expense")
        void shouldReject6thReceiptForSameExpense() throws IOException {
            // Given
            UUID expenseId = UUID.randomUUID();
            for (int i = 0; i < 5; i++) {
                service.storeReceipt(
                    expenseId,
                    createTestFile("receipt" + i + ".jpg", createJpegBytes()),
                    "receipt" + i + ".jpg"
                );
            }

            // When/Then
            Path sixthFile = createTestFile("receipt6.jpg", createJpegBytes());
            assertThatThrownBy(() -> service.storeReceipt(expenseId, sixthFile, "receipt6.jpg"))
                .isInstanceOf(ReceiptStorageException.class)
                .hasMessageContaining("Maximum receipts");
        }
    }

    // === Helper Methods ===

    private Path createTestFile(String name, byte[] content) throws IOException {
        Path file = tempDir.resolve("source").resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, content);
        return file;
    }

    // JPEG magic bytes
    private byte[] createJpegBytes() {
        byte[] jpeg = new byte[100];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        jpeg[3] = (byte) 0xE0;
        return jpeg;
    }

    // PNG magic bytes
    private byte[] createPngBytes() {
        byte[] png = new byte[100];
        png[0] = (byte) 0x89;
        png[1] = (byte) 0x50;
        png[2] = (byte) 0x4E;
        png[3] = (byte) 0x47;
        png[4] = (byte) 0x0D;
        png[5] = (byte) 0x0A;
        png[6] = (byte) 0x1A;
        png[7] = (byte) 0x0A;
        return png;
    }

    // PDF magic bytes
    private byte[] createPdfBytes() {
        byte[] pdf = new byte[100];
        pdf[0] = (byte) 0x25;
        pdf[1] = (byte) 0x50;
        pdf[2] = (byte) 0x44;
        pdf[3] = (byte) 0x46;
        pdf[4] = (byte) 0x2D;
        return pdf;
    }

    // GIF magic bytes
    private byte[] createGifBytes() {
        byte[] gif = new byte[100];
        gif[0] = (byte) 0x47;
        gif[1] = (byte) 0x49;
        gif[2] = (byte) 0x46;
        gif[3] = (byte) 0x38;
        gif[4] = (byte) 0x39;
        gif[5] = (byte) 0x61;
        return gif;
    }
}
