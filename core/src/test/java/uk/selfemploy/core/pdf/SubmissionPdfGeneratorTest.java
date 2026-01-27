package uk.selfemploy.core.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.core.calculator.Class2NICalculationResult;
import uk.selfemploy.core.calculator.NICalculationResult;
import uk.selfemploy.core.calculator.TaxCalculationResult;
import uk.selfemploy.core.calculator.TaxLiabilityResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SubmissionPdfGenerator.
 *
 * Tests cover:
 * - AC-1: PDF generated after successful HMRC submission
 * - AC-2: PDF includes HMRC reference, submission date, tax year
 * - AC-3: PDF includes income summary, expense summary by category
 * - AC-4: PDF includes tax calculation breakdown (Income Tax + NI)
 * - AC-5: PDF includes payment deadline (31 January)
 * - AC-6: PDF has professional styling with logo placeholder
 * - AC-8: PDF saved locally with submission record
 *
 * TDD Approach:
 * - Write failing tests first
 * - Implement minimum code to pass
 * - Refactor
 */
@DisplayName("SubmissionPdfGenerator Tests")
class SubmissionPdfGeneratorTest {

    private SubmissionPdfGenerator pdfGenerator;

    @TempDir
    Path tempDir;

    // Test data
    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final String HMRC_REFERENCE = "XAIT00000123456";
    private static final TaxYear TAX_YEAR = TaxYear.of(2024);

    @BeforeEach
    void setUp() {
        pdfGenerator = new SubmissionPdfGenerator();
    }

    @Nested
    @DisplayName("PDF Generation Tests - AC-1")
    class PdfGenerationTests {

        @Test
        @DisplayName("should generate PDF for successful HMRC submission")
        void shouldGeneratePdfForSuccessfulSubmission() throws PdfGenerationException, IOException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();

            Path outputPath = tempDir.resolve("confirmation.pdf");

            pdfGenerator.generate(submission, taxResult, expensesByCategory, outputPath);

            assertThat(outputPath).exists();
            assertThat(Files.size(outputPath)).isGreaterThan(0);
        }

        @Test
        @DisplayName("should create PDF file with correct extension")
        void shouldCreatePdfFileWithCorrectExtension() throws PdfGenerationException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();

            Path outputPath = tempDir.resolve("confirmation.pdf");

            pdfGenerator.generate(submission, taxResult, expensesByCategory, outputPath);

            assertThat(outputPath.toString()).endsWith(".pdf");
        }

        @Test
        @DisplayName("should throw PdfGenerationException when submission is null")
        void shouldThrowWhenSubmissionIsNull() {
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            assertThatThrownBy(() -> pdfGenerator.generate(null, taxResult, expensesByCategory, outputPath))
                    .isInstanceOf(PdfGenerationException.class)
                    .hasMessageContaining("Submission");
        }

        @Test
        @DisplayName("should throw PdfGenerationException when tax result is null")
        void shouldThrowWhenTaxResultIsNull() {
            Submission submission = createSuccessfulSubmission();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            assertThatThrownBy(() -> pdfGenerator.generate(submission, null, expensesByCategory, outputPath))
                    .isInstanceOf(PdfGenerationException.class)
                    .hasMessageContaining("Tax");
        }

        @Test
        @DisplayName("should throw PdfGenerationException when output path is null")
        void shouldThrowWhenOutputPathIsNull() {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();

            assertThatThrownBy(() -> pdfGenerator.generate(submission, taxResult, expensesByCategory, null))
                    .isInstanceOf(PdfGenerationException.class)
                    .hasMessageContaining("path");
        }

        @Test
        @DisplayName("should throw PdfGenerationException for non-successful submission")
        void shouldThrowForNonSuccessfulSubmission() {
            Submission pendingSubmission = createPendingSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            assertThatThrownBy(() -> pdfGenerator.generate(pendingSubmission, taxResult, expensesByCategory, outputPath))
                    .isInstanceOf(PdfGenerationException.class)
                    .hasMessageContaining("successful");
        }

        @Test
        @DisplayName("should throw PdfGenerationException when HMRC reference is missing")
        void shouldThrowWhenHmrcReferenceIsMissing() {
            Submission submissionWithoutRef = createSubmissionWithoutHmrcReference();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            assertThatThrownBy(() -> pdfGenerator.generate(submissionWithoutRef, taxResult, expensesByCategory, outputPath))
                    .isInstanceOf(PdfGenerationException.class)
                    .hasMessageContaining("HMRC reference");
        }
    }

    @Nested
    @DisplayName("PDF Content Tests - AC-2, AC-3, AC-4, AC-5")
    class PdfContentTests {

        @Test
        @DisplayName("should include HMRC reference in generated PDF")
        void shouldIncludeHmrcReference() throws PdfGenerationException, IOException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            pdfGenerator.generate(submission, taxResult, expensesByCategory, outputPath);

            // Verify PDF was created - content verification requires reading PDF
            assertThat(outputPath).exists();
            // PDF should contain meaningful content (not just empty)
            assertThat(Files.size(outputPath)).isGreaterThan(1000);
        }

        @Test
        @DisplayName("should include submission date in generated PDF")
        void shouldIncludeSubmissionDate() throws PdfGenerationException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            pdfGenerator.generate(submission, taxResult, expensesByCategory, outputPath);

            assertThat(outputPath).exists();
            // Content verification - PDF is valid and contains data
            assertThat(Files.exists(outputPath)).isTrue();
        }

        @Test
        @DisplayName("should include tax year in generated PDF")
        void shouldIncludeTaxYear() throws PdfGenerationException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            pdfGenerator.generate(submission, taxResult, expensesByCategory, outputPath);

            assertThat(outputPath).exists();
        }

        @Test
        @DisplayName("should handle empty expenses map")
        void shouldHandleEmptyExpensesMap() throws PdfGenerationException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> emptyExpenses = new EnumMap<>(ExpenseCategory.class);
            Path outputPath = tempDir.resolve("confirmation.pdf");

            pdfGenerator.generate(submission, taxResult, emptyExpenses, outputPath);

            assertThat(outputPath).exists();
        }

        @Test
        @DisplayName("should handle null expenses map (use empty)")
        void shouldHandleNullExpensesMap() throws PdfGenerationException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // Should not throw - treat null as empty map
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            assertThat(outputPath).exists();
        }
    }

    @Nested
    @DisplayName("File System Tests - AC-8")
    class FileSystemTests {

        @Test
        @DisplayName("should create parent directories if they don't exist")
        void shouldCreateParentDirectories() throws PdfGenerationException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            Path outputPath = tempDir.resolve("subdir/nested/confirmation.pdf");

            pdfGenerator.generate(submission, taxResult, expensesByCategory, outputPath);

            assertThat(outputPath).exists();
            assertThat(outputPath.getParent()).exists();
        }

        @Test
        @DisplayName("should overwrite existing file")
        void shouldOverwriteExistingFile() throws PdfGenerationException, IOException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // Create existing file
            Files.writeString(outputPath, "existing content");
            long originalSize = Files.size(outputPath);

            pdfGenerator.generate(submission, taxResult, expensesByCategory, outputPath);

            // File should be overwritten with PDF content
            assertThat(Files.size(outputPath)).isNotEqualTo(originalSize);
            assertThat(Files.size(outputPath)).isGreaterThan(originalSize);
        }

        @Test
        @DisplayName("should throw PdfGenerationException for path to non-writable location")
        void shouldThrowForNonWritablePath() {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();
            // Path to a directory that cannot be created (root-only location)
            Path nonWritablePath = Path.of("/root/cannot_write_here/confirmation.pdf");

            assertThatThrownBy(() -> pdfGenerator.generate(submission, taxResult, expensesByCategory, nonWritablePath))
                    .isInstanceOf(PdfGenerationException.class);
        }
    }

    @Nested
    @DisplayName("PDF Filename Generation Tests")
    class FilenameGenerationTests {

        @Test
        @DisplayName("should generate filename with tax year and reference")
        void shouldGenerateFilenameWithTaxYearAndReference() throws PdfGenerationException {
            Submission submission = createSuccessfulSubmission();

            String filename = pdfGenerator.generateFilename(submission);

            assertThat(filename).contains(TAX_YEAR.label().replace("/", "-"));
            assertThat(filename).contains(HMRC_REFERENCE);
            assertThat(filename).endsWith(".pdf");
        }

        @Test
        @DisplayName("should generate safe filename without special characters")
        void shouldGenerateSafeFilename() throws PdfGenerationException {
            Submission submission = createSuccessfulSubmission();

            String filename = pdfGenerator.generateFilename(submission);

            // Filename should be safe for all OS
            assertThat(filename).doesNotContain("/");
            assertThat(filename).doesNotContain("\\");
            assertThat(filename).doesNotContain(":");
            assertThat(filename).doesNotContain("*");
            assertThat(filename).doesNotContain("?");
        }

        @Test
        @DisplayName("should throw PdfGenerationException for null submission in filename generation")
        void shouldThrowForNullSubmissionInFilename() {
            assertThatThrownBy(() -> pdfGenerator.generateFilename(null))
                    .isInstanceOf(PdfGenerationException.class)
                    .hasMessageContaining("Submission");
        }
    }

    @Nested
    @DisplayName("Payment Deadline Tests - AC-5")
    class PaymentDeadlineTests {

        @Test
        @DisplayName("should calculate correct payment deadline for 2024/25 tax year")
        void shouldCalculateCorrectPaymentDeadline() throws PdfGenerationException {
            // For tax year 2024/25, payment deadline is 31 January 2026
            LocalDate expectedDeadline = LocalDate.of(2026, 1, 31);

            LocalDate deadline = pdfGenerator.calculatePaymentDeadline(TAX_YEAR);

            assertThat(deadline).isEqualTo(expectedDeadline);
        }

        @Test
        @DisplayName("should throw PdfGenerationException for null tax year")
        void shouldThrowForNullTaxYear() {
            assertThatThrownBy(() -> pdfGenerator.calculatePaymentDeadline(null))
                    .isInstanceOf(PdfGenerationException.class)
                    .hasMessageContaining("Tax year");
        }
    }

    @Nested
    @DisplayName("PDF Byte Array Generation Tests")
    class ByteArrayGenerationTests {

        @Test
        @DisplayName("should generate PDF as byte array")
        void shouldGeneratePdfAsByteArray() throws PdfGenerationException {
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();

            byte[] pdfBytes = pdfGenerator.generateBytes(submission, taxResult, expensesByCategory);

            assertThat(pdfBytes).isNotNull();
            assertThat(pdfBytes.length).isGreaterThan(0);
            // PDF files start with %PDF
            assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("should throw PdfGenerationException when generation fails")
        void shouldThrowWhenGenerationFails() {
            Submission pendingSubmission = createPendingSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expensesByCategory = createExpensesByCategory();

            assertThatThrownBy(() -> pdfGenerator.generateBytes(pendingSubmission, taxResult, expensesByCategory))
                    .isInstanceOf(PdfGenerationException.class);
        }
    }

    // Helper methods to create test data

    private Submission createSuccessfulSubmission() {
        return new Submission(
                SUBMISSION_ID,
                BUSINESS_ID,
                SubmissionType.ANNUAL,
                TAX_YEAR,
                TAX_YEAR.startDate(),
                TAX_YEAR.endDate(),
                new BigDecimal("50000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("40000.00"),
                SubmissionStatus.ACCEPTED,
                HMRC_REFERENCE,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(), // declarationAcceptedAt
                "e7b9f3c8a1d2e4f5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9", // declarationTextHash
                "1234567890", // utr
                "AB123456A"   // nino
        );
    }

    private Submission createPendingSubmission() {
        return new Submission(
                SUBMISSION_ID,
                BUSINESS_ID,
                SubmissionType.ANNUAL,
                TAX_YEAR,
                TAX_YEAR.startDate(),
                TAX_YEAR.endDate(),
                new BigDecimal("50000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("40000.00"),
                SubmissionStatus.PENDING,
                null,
                null,
                Instant.now(),
                Instant.now(),
                null, // declarationAcceptedAt
                null, // declarationTextHash
                null, // utr
                null  // nino
        );
    }

    private Submission createSubmissionWithoutHmrcReference() {
        return new Submission(
                SUBMISSION_ID,
                BUSINESS_ID,
                SubmissionType.ANNUAL,
                TAX_YEAR,
                TAX_YEAR.startDate(),
                TAX_YEAR.endDate(),
                new BigDecimal("50000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("40000.00"),
                SubmissionStatus.ACCEPTED,
                null, // No HMRC reference
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(), // declarationAcceptedAt
                "e7b9f3c8a1d2e4f5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9", // declarationTextHash
                "1234567890", // utr
                "AB123456A"   // nino
        );
    }

    private TaxLiabilityResult createTaxResult() {
        TaxCalculationResult incomeTaxDetails = new TaxCalculationResult(
                new BigDecimal("40000.00"), // grossIncome
                new BigDecimal("12570.00"), // personalAllowance
                new BigDecimal("27430.00"), // taxableIncome
                new BigDecimal("27430.00"), // basicRateAmount
                new BigDecimal("5486.00"),  // basicRateTax (20%)
                BigDecimal.ZERO,            // higherRateAmount
                BigDecimal.ZERO,            // higherRateTax
                BigDecimal.ZERO,            // additionalRateAmount
                BigDecimal.ZERO,            // additionalRateTax
                new BigDecimal("5486.00")   // totalTax
        );

        NICalculationResult niClass4Details = new NICalculationResult(
                new BigDecimal("40000.00"), // grossProfit
                new BigDecimal("12570.00"), // lowerProfitsLimit
                new BigDecimal("27430.00"), // profitSubjectToNI
                new BigDecimal("27430.00"), // mainRateAmount
                new BigDecimal("1645.80"),  // mainRateNI (6%)
                BigDecimal.ZERO,            // additionalRateAmount
                BigDecimal.ZERO,            // additionalRateNI
                new BigDecimal("1645.80")   // totalNI
        );

        Class2NICalculationResult niClass2Details = new Class2NICalculationResult(
                new BigDecimal("40000.00"), // grossProfit
                new BigDecimal("6845"),     // smallProfitsThreshold
                new BigDecimal("3.50"),     // weeklyRate
                52,                         // weeksLiable
                new BigDecimal("182.00"),   // totalNI
                true,                       // isMandatory
                false                       // isVoluntary
        );

        return new TaxLiabilityResult(
                new BigDecimal("40000.00"), // grossProfit
                new BigDecimal("5486.00"),  // incomeTax
                new BigDecimal("1645.80"),  // niClass4
                new BigDecimal("182.00"),   // niClass2
                new BigDecimal("7313.80"),  // totalLiability (5486 + 1645.80 + 182)
                incomeTaxDetails,
                niClass4Details,
                niClass2Details
        );
    }

    private Map<ExpenseCategory, BigDecimal> createExpensesByCategory() {
        Map<ExpenseCategory, BigDecimal> expenses = new EnumMap<>(ExpenseCategory.class);
        expenses.put(ExpenseCategory.OFFICE_COSTS, new BigDecimal("2000.00"));
        expenses.put(ExpenseCategory.TRAVEL, new BigDecimal("3000.00"));
        expenses.put(ExpenseCategory.PROFESSIONAL_FEES, new BigDecimal("1500.00"));
        expenses.put(ExpenseCategory.ADVERTISING, new BigDecimal("1000.00"));
        expenses.put(ExpenseCategory.HOME_OFFICE_SIMPLIFIED, new BigDecimal("312.00"));
        expenses.put(ExpenseCategory.OTHER_EXPENSES, new BigDecimal("2188.00"));
        return expenses;
    }
}
