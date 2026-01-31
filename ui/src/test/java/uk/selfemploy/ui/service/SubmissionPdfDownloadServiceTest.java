package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.ui.viewmodel.SubmissionTableRow;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SubmissionPdfDownloadService.
 *
 * SE-SH-005: PDF Download functionality
 *
 * Tests cover:
 * - PDF generation from SubmissionTableRow
 * - Filename generation
 * - Save to Downloads folder
 * - Error handling
 */
@DisplayName("SubmissionPdfDownloadService Tests")
class SubmissionPdfDownloadServiceTest {

    private SubmissionPdfDownloadService pdfService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        pdfService = new SubmissionPdfDownloadService();
    }

    @Nested
    @DisplayName("Filename Generation")
    class FilenameGenerationTests {

        @Test
        @DisplayName("should generate filename with type, tax year, and reference")
        void shouldGenerateFilenameWithDetails() {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();

            String filename = pdfService.generateFilename(submission);

            assertThat(filename).contains("annual");
            assertThat(filename).contains("2024-25");
            assertThat(filename).contains("SA-2025-123456");
            assertThat(filename).endsWith(".pdf");
        }

        @Test
        @DisplayName("should generate filename for quarterly submission")
        void shouldGenerateFilenameForQuarterly() {
            SubmissionTableRow submission = createQuarterlySubmission();

            String filename = pdfService.generateFilename(submission);

            assertThat(filename).containsIgnoringCase("q1");
            assertThat(filename).endsWith(".pdf");
        }

        @Test
        @DisplayName("should use submission ID when no HMRC reference")
        void shouldUseIdWhenNoReference() {
            SubmissionTableRow submission = createPendingSubmission();

            String filename = pdfService.generateFilename(submission);

            assertThat(filename).endsWith(".pdf");
            assertThat(filename).doesNotContain("null");
        }

        @Test
        @DisplayName("should sanitize filename to remove special characters")
        void shouldSanitizeFilename() {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();

            String filename = pdfService.generateFilename(submission);

            assertThat(filename).doesNotContain("/");
            assertThat(filename).doesNotContain("\\");
            assertThat(filename).doesNotContain(":");
            assertThat(filename).doesNotContain("*");
            assertThat(filename).doesNotContain("?");
        }

        @Test
        @DisplayName("should throw exception for null submission")
        void shouldThrowForNullSubmission() {
            assertThatThrownBy(() -> pdfService.generateFilename(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("PDF Generation")
    class PdfGenerationTests {

        @Test
        @DisplayName("should generate PDF file")
        void shouldGeneratePdfFile() throws Exception {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();
            Path outputPath = tempDir.resolve("test-submission.pdf");

            pdfService.generatePdf(submission, outputPath);

            assertThat(outputPath).exists();
            assertThat(Files.size(outputPath)).isGreaterThan(0);
        }

        @Test
        @DisplayName("should generate valid PDF content")
        void shouldGenerateValidPdfContent() throws Exception {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();
            Path outputPath = tempDir.resolve("test-submission.pdf");

            pdfService.generatePdf(submission, outputPath);

            // Check PDF magic bytes
            byte[] content = Files.readAllBytes(outputPath);
            assertThat(new String(content, 0, 4)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("should create parent directories if needed")
        void shouldCreateParentDirectories() throws Exception {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();
            Path outputPath = tempDir.resolve("subdir/nested/test.pdf");

            pdfService.generatePdf(submission, outputPath);

            assertThat(outputPath).exists();
            assertThat(outputPath.getParent()).exists();
        }

        @Test
        @DisplayName("should generate PDF for rejected submission")
        void shouldGeneratePdfForRejectedSubmission() throws Exception {
            SubmissionTableRow submission = createRejectedSubmission();
            Path outputPath = tempDir.resolve("rejected-submission.pdf");

            pdfService.generatePdf(submission, outputPath);

            assertThat(outputPath).exists();
        }

        @Test
        @DisplayName("should generate PDF for pending submission")
        void shouldGeneratePdfForPendingSubmission() throws Exception {
            SubmissionTableRow submission = createPendingSubmission();
            Path outputPath = tempDir.resolve("pending-submission.pdf");

            pdfService.generatePdf(submission, outputPath);

            assertThat(outputPath).exists();
        }

        @Test
        @DisplayName("should throw exception for null submission")
        void shouldThrowForNullSubmission() {
            Path outputPath = tempDir.resolve("test.pdf");

            assertThatThrownBy(() -> pdfService.generatePdf(null, outputPath))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw exception for null output path")
        void shouldThrowForNullOutputPath() {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();

            assertThatThrownBy(() -> pdfService.generatePdf(submission, null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("PDF as Byte Array")
    class PdfBytesTests {

        @Test
        @DisplayName("should generate PDF bytes")
        void shouldGeneratePdfBytes() throws Exception {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();

            byte[] pdfBytes = pdfService.generatePdfBytes(submission);

            assertThat(pdfBytes).isNotNull();
            assertThat(pdfBytes.length).isGreaterThan(0);
            assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("should throw exception for null submission")
        void shouldThrowForNullSubmission() {
            assertThatThrownBy(() -> pdfService.generatePdfBytes(null))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Display Name in PDF")
    class DisplayNamePdfTests {

        @Test
        @DisplayName("should generate PDF with display name in header")
        void shouldGeneratePdfWithDisplayNameInHeader() throws Exception {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();
            Path outputPath = tempDir.resolve("test-with-name.pdf");

            pdfService.generatePdf(submission, outputPath, "Sarah Johnson");

            assertThat(outputPath).exists();
            assertThat(Files.size(outputPath)).isGreaterThan(0);
        }

        @Test
        @DisplayName("should generate PDF without display name when null")
        void shouldGeneratePdfWithoutDisplayNameWhenNull() throws Exception {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();
            Path outputPath = tempDir.resolve("test-no-name.pdf");

            pdfService.generatePdf(submission, outputPath, null);

            assertThat(outputPath).exists();
        }

        @Test
        @DisplayName("should generate PDF without display name when empty")
        void shouldGeneratePdfWithoutDisplayNameWhenEmpty() throws Exception {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();
            Path outputPath = tempDir.resolve("test-empty-name.pdf");

            pdfService.generatePdf(submission, outputPath, "");

            assertThat(outputPath).exists();
        }

        @Test
        @DisplayName("should generate PDF bytes with display name")
        void shouldGeneratePdfBytesWithDisplayName() throws Exception {
            SubmissionTableRow submission = createAcceptedAnnualSubmission();

            byte[] pdfBytes = pdfService.generatePdfBytes(submission, "John Smith");

            assertThat(pdfBytes).isNotNull();
            assertThat(pdfBytes.length).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Downloads Path Resolution")
    class DownloadsPathTests {

        @Test
        @DisplayName("should resolve downloads directory")
        void shouldResolveDownloadsDirectory() {
            Path downloadsPath = pdfService.getDownloadsDirectory();

            assertThat(downloadsPath).isNotNull();
            // Path should be absolute and either be Downloads folder or fallback to home
            assertThat(downloadsPath.isAbsolute()).isTrue();
            // Should be a valid directory path (either Downloads or home as fallback)
            String userHome = System.getProperty("user.home");
            assertThat(downloadsPath.toString()).startsWith(userHome);
        }

        @Test
        @DisplayName("should return valid path on all platforms")
        void shouldReturnValidPath() {
            Path downloadsPath = pdfService.getDownloadsDirectory();

            assertThat(downloadsPath).isNotNull();
            // Path should be absolute
            assertThat(downloadsPath.isAbsolute()).isTrue();
        }
    }

    // Helper methods to create test data

    private SubmissionTableRow createAcceptedAnnualSubmission() {
        return SubmissionTableRow.builder()
            .id(1L)
            .submittedAt(LocalDateTime.of(2026, 1, 24, 14, 32, 15))
            .type(SubmissionType.ANNUAL)
            .taxYear("2024/25")
            .status(SubmissionStatus.ACCEPTED)
            .hmrcReference("SA-2025-123456")
            .totalIncome(new BigDecimal("45000.00"))
            .totalExpenses(new BigDecimal("13000.00"))
            .netProfit(new BigDecimal("32000.00"))
            .taxDue(new BigDecimal("5051.80"))
            .build();
    }

    private SubmissionTableRow createQuarterlySubmission() {
        return SubmissionTableRow.builder()
            .id(2L)
            .submittedAt(LocalDateTime.of(2025, 8, 15, 10, 0, 0))
            .type(SubmissionType.QUARTERLY_Q1)
            .taxYear("2025/26")
            .status(SubmissionStatus.ACCEPTED)
            .hmrcReference("MTD-Q1-2025-789012")
            .totalIncome(new BigDecimal("12000.00"))
            .totalExpenses(new BigDecimal("3000.00"))
            .netProfit(new BigDecimal("9000.00"))
            .build();
    }

    private SubmissionTableRow createRejectedSubmission() {
        return SubmissionTableRow.builder()
            .id(3L)
            .submittedAt(LocalDateTime.of(2026, 1, 20, 16, 45, 0))
            .type(SubmissionType.ANNUAL)
            .taxYear("2024/25")
            .status(SubmissionStatus.REJECTED)
            .totalIncome(new BigDecimal("50000.00"))
            .totalExpenses(new BigDecimal("10000.00"))
            .netProfit(new BigDecimal("40000.00"))
            .errorMessage("INVALID_NINO: National Insurance number format is incorrect")
            .build();
    }

    private SubmissionTableRow createPendingSubmission() {
        return SubmissionTableRow.builder()
            .id(4L)
            .submittedAt(LocalDateTime.of(2026, 1, 26, 9, 0, 0))
            .type(SubmissionType.ANNUAL)
            .taxYear("2024/25")
            .status(SubmissionStatus.PENDING)
            .totalIncome(new BigDecimal("35000.00"))
            .totalExpenses(new BigDecimal("8000.00"))
            .netProfit(new BigDecimal("27000.00"))
            .build();
    }
}
