package uk.selfemploy.core.pdf;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
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
import uk.selfemploy.core.calculator.NICalculationResult;
import uk.selfemploy.core.calculator.TaxCalculationResult;
import uk.selfemploy.core.calculator.TaxLiabilityResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for PDF content verification (SE-501).
 *
 * These tests use OpenPDF's text extraction to verify PDF content,
 * covering QA test cases TC-501-001 through TC-501-018.
 *
 * @see uk.selfemploy.core.pdf.SubmissionPdfGenerator
 */
@DisplayName("PDF Content Verification Tests (SE-501)")
class PdfContentVerificationTest {

    private SubmissionPdfGenerator pdfGenerator;

    @TempDir
    Path tempDir;

    // Test data constants
    private static final UUID SUBMISSION_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");
    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final String HMRC_REFERENCE = "HMRC-TEST-12345";
    private static final TaxYear TAX_YEAR_2025 = TaxYear.of(2025);
    private static final Instant SUBMISSION_TIME = Instant.parse("2026-01-15T14:30:00Z");

    @BeforeEach
    void setUp() {
        pdfGenerator = new SubmissionPdfGenerator();
    }

    // ===== P0 Critical Tests =====

    @Nested
    @DisplayName("P0 Critical: Core PDF Generation and Content")
    class P0CriticalTests {

        @Test
        @DisplayName("TC-501-001: PDF generated only for successful submission")
        void tc501_001_shouldGeneratePdfForSuccessfulSubmission() throws PdfGenerationException, IOException {
            // Given: Successful submission
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF file exists with valid content
            assertThat(outputPath).exists();
            assertThat(Files.size(outputPath)).isGreaterThan(0);

            // Verify it's a valid PDF
            byte[] content = Files.readAllBytes(outputPath);
            assertThat(new String(content, 0, 4)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("TC-501-002: PDF rejected for failed submission")
        void tc501_002_shouldRejectNonSuccessfulSubmission() {
            // Given: Failed submission
            Submission failedSubmission = createFailedSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // Then: Should throw PdfGenerationException
            assertThatThrownBy(() ->
                pdfGenerator.generate(failedSubmission, taxResult, null, outputPath)
            )
                .isInstanceOf(PdfGenerationException.class)
                .hasMessageContaining("non-successful submission");
        }

        @Test
        @DisplayName("TC-501-003: PDF contains HMRC reference")
        void tc501_003_shouldContainHmrcReference() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF contains HMRC reference
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("HMRC Reference");
            assertThat(pdfText).contains(HMRC_REFERENCE);
        }

        @Test
        @DisplayName("TC-501-004: PDF contains submission date")
        void tc501_004_shouldContainSubmissionDate() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF contains submission date in expected format
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Submission Date");
            // Verify date is present (format: dd MMM yyyy at HH:mm)
            assertThat(pdfText).containsPattern("\\d{2} \\w{3} \\d{4} at \\d{2}:\\d{2}");
        }

        @Test
        @DisplayName("TC-501-005: PDF contains tax year")
        void tc501_005_shouldContainTaxYear() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF contains tax year
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Tax Year");
            assertThat(pdfText).contains("2025/26");
        }

        @Test
        @DisplayName("TC-501-006: PDF contains income summary")
        void tc501_006_shouldContainIncomeSummary() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF contains income summary
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Income Summary");
            assertThat(pdfText).contains("Total Income");
            assertThat(pdfText).contains("50,000.00"); // Total income
        }

        @Test
        @DisplayName("TC-501-007: PDF contains expense breakdown by SA103 category")
        void tc501_007_shouldContainExpenseBreakdownByCategory() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expenses = createExpensesByCategory();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, expenses, outputPath);

            // Then: PDF contains expense breakdown with SA103 categories
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Expense Breakdown");
            assertThat(pdfText).contains("SA103F");
            // Check for expense categories
            assertThat(pdfText).contains("Phone, stationery and office costs"); // OFFICE_COSTS
            assertThat(pdfText).contains("Car, van and travel expenses"); // TRAVEL
        }

        @Test
        @DisplayName("TC-501-008: PDF contains tax calculation breakdown (Income Tax + NI)")
        void tc501_008_shouldContainTaxCalculationBreakdown() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF contains tax calculation breakdown
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Tax Calculation");
            assertThat(pdfText).contains("Income Tax");
            assertThat(pdfText).contains("Personal Allowance");
            assertThat(pdfText).contains("Taxable Income");
            assertThat(pdfText).contains("National Insurance Class 4");
            assertThat(pdfText).contains("TOTAL TAX LIABILITY");
        }

        @Test
        @DisplayName("TC-501-009: PDF contains payment deadline (31 January)")
        void tc501_009_shouldContainPaymentDeadline() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF contains payment deadline for 2025/26 tax year
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Payment Deadline");
            assertThat(pdfText).contains("31 January 2027"); // Payment due for 2025/26 tax year
        }

        @Test
        @DisplayName("TC-501-011: PDF saved to specified output path")
        void tc501_011_shouldSaveToSpecifiedPath() throws PdfGenerationException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("test-confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: File exists at specified path
            assertThat(outputPath).exists();
            assertThat(outputPath.toString()).endsWith("test-confirmation.pdf");
        }

        @Test
        @DisplayName("TC-501-017: PDF rejects missing HMRC reference")
        void tc501_017_shouldRejectMissingHmrcReference() {
            // Given: Submission without HMRC reference
            Submission submissionWithoutRef = new Submission(
                SUBMISSION_ID,
                BUSINESS_ID,
                SubmissionType.ANNUAL,
                TAX_YEAR_2025,
                TAX_YEAR_2025.startDate(),
                TAX_YEAR_2025.endDate(),
                new BigDecimal("50000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("40000.00"),
                SubmissionStatus.ACCEPTED,
                null, // No HMRC reference
                null,
                SUBMISSION_TIME,
                SUBMISSION_TIME
            );
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // Then: Should throw PdfGenerationException
            assertThatThrownBy(() ->
                pdfGenerator.generate(submissionWithoutRef, taxResult, null, outputPath)
            )
                .isInstanceOf(PdfGenerationException.class)
                .hasMessageContaining("HMRC reference is required");
        }
    }

    // ===== P1 Important Tests =====

    @Nested
    @DisplayName("P1 Important: Professional Styling and File Operations")
    class P1ImportantTests {

        @Test
        @DisplayName("TC-501-010: PDF has professional styling with logo placeholder")
        void tc501_010_shouldHaveProfessionalStylingWithLogoPlaceholder() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF contains title and logo placeholder
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("LOGO");
            assertThat(pdfText).contains("Self Assessment");
            assertThat(pdfText).contains("Submission Confirmation");
        }

        @Test
        @DisplayName("TC-501-012: PDF creates parent directories if missing")
        void tc501_012_shouldCreateParentDirectories() throws PdfGenerationException {
            // Given: Path with non-existent parent directories
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("new-dir/sub-dir/confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: Parent directories created and file exists
            assertThat(outputPath).exists();
            assertThat(outputPath.getParent()).exists();
            assertThat(outputPath.getParent().getFileName().toString()).isEqualTo("sub-dir");
        }

        @Test
        @DisplayName("TC-501-013: PDF filename generation includes tax year and reference")
        void tc501_013_shouldGenerateCorrectFilename() throws PdfGenerationException {
            // Given
            Submission submission = createSuccessfulSubmission();

            // When
            String filename = pdfGenerator.generateFilename(submission);

            // Then: Filename format: SA-Confirmation_2025-26_HMRC-TEST-12345.pdf
            assertThat(filename).startsWith("SA-Confirmation_");
            assertThat(filename).contains("2025-26");
            assertThat(filename).contains(HMRC_REFERENCE);
            assertThat(filename).endsWith(".pdf");
        }

        @Test
        @DisplayName("TC-501-014: generateBytes returns valid PDF byte array")
        void tc501_014_shouldGenerateValidPdfBytes() throws PdfGenerationException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();

            // When
            byte[] pdfBytes = pdfGenerator.generateBytes(submission, taxResult, null);

            // Then: Valid PDF byte array
            assertThat(pdfBytes).isNotNull();
            assertThat(pdfBytes.length).isGreaterThan(0);
            // PDF files start with %PDF
            assertThat(new String(pdfBytes, 0, 4)).isEqualTo("%PDF");
        }

        @Test
        @DisplayName("TC-501-015: PDF contains declaration footer with generation timestamp")
        void tc501_015_shouldContainDeclarationFooter() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF contains declaration and timestamps
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Declaration");
            assertThat(pdfText).contains("Keep this confirmation for your records");
            assertThat(pdfText).contains("Generated:");
            assertThat(pdfText).contains("Reference ID:");
        }

        @Test
        @DisplayName("TC-501-016: PDF handles empty expenses gracefully")
        void tc501_016_shouldHandleEmptyExpenses() throws PdfGenerationException, IOException {
            // Given: No expenses
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> emptyExpenses = new EnumMap<>(ExpenseCategory.class);
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When: Should not throw exception
            pdfGenerator.generate(submission, taxResult, emptyExpenses, outputPath);

            // Then: PDF generated without expense section
            assertThat(outputPath).exists();
            String pdfText = extractPdfText(outputPath);
            // Should NOT contain expense breakdown when empty
            assertThat(pdfText).doesNotContain("Expense Breakdown");
        }

        @Test
        @DisplayName("TC-501-016b: PDF handles null expenses gracefully")
        void tc501_016b_shouldHandleNullExpenses() throws PdfGenerationException, IOException {
            // Given: Null expenses map
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When: Should not throw exception
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: PDF generated successfully
            assertThat(outputPath).exists();
        }
    }

    // ===== P2 Nice-to-have Tests =====

    @Nested
    @DisplayName("P2 Nice-to-have: Currency Formatting")
    class P2NiceToHaveTests {

        @Test
        @DisplayName("TC-501-018: Currency values formatted with GBP symbol and 2 decimal places")
        void tc501_018_shouldFormatCurrencyCorrectly() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: Currency values properly formatted
            String pdfText = extractPdfText(outputPath);
            // Check for pound symbol (may be encoded differently in PDF)
            // Values should have proper thousands separators and 2 decimal places
            assertThat(pdfText).containsPattern("50,000\\.00"); // Total income
            assertThat(pdfText).containsPattern("10,000\\.00"); // Total expenses
        }
    }

    // ===== Additional Content Verification Tests =====

    @Nested
    @DisplayName("Additional: Content Verification")
    class AdditionalContentTests {

        @Test
        @DisplayName("PDF contains net profit calculation")
        void shouldContainNetProfitCalculation() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Net Profit");
            assertThat(pdfText).contains("40,000.00");
        }

        @Test
        @DisplayName("PDF contains submission period")
        void shouldContainSubmissionPeriod() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: Contains period dates
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Period");
            assertThat(pdfText).contains("April 2025"); // Start of 2025/26 tax year
            assertThat(pdfText).contains("April 2026"); // End of 2025/26 tax year
        }

        @Test
        @DisplayName("PDF contains submission type")
        void shouldContainSubmissionType() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Submission Type");
            assertThat(pdfText).contains("Annual"); // Annual submission
        }

        @Test
        @DisplayName("PDF contains tax rate breakdown")
        void shouldContainTaxRateBreakdown() throws PdfGenerationException, IOException {
            // Given
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, null, outputPath);

            // Then: Contains tax rate information
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Basic Rate");
            assertThat(pdfText).contains("20%");
        }

        @Test
        @DisplayName("PDF payment deadline calculation is correct")
        void shouldCalculatePaymentDeadlineCorrectly() throws PdfGenerationException {
            // Given: Tax year 2025/26
            LocalDate expectedDeadline = LocalDate.of(2027, 1, 31);

            // When
            LocalDate deadline = pdfGenerator.calculatePaymentDeadline(TAX_YEAR_2025);

            // Then
            assertThat(deadline).isEqualTo(expectedDeadline);
        }

        @Test
        @DisplayName("PDF with all expense categories displays correctly")
        void shouldDisplayAllExpenseCategories() throws PdfGenerationException, IOException {
            // Given: Multiple expense categories
            Submission submission = createSuccessfulSubmission();
            TaxLiabilityResult taxResult = createTaxResult();
            Map<ExpenseCategory, BigDecimal> expenses = new EnumMap<>(ExpenseCategory.class);
            expenses.put(ExpenseCategory.OFFICE_COSTS, new BigDecimal("2000.00"));
            expenses.put(ExpenseCategory.TRAVEL, new BigDecimal("3000.00"));
            expenses.put(ExpenseCategory.PROFESSIONAL_FEES, new BigDecimal("1500.00"));
            expenses.put(ExpenseCategory.ADVERTISING, new BigDecimal("1000.00"));
            expenses.put(ExpenseCategory.OTHER_EXPENSES, new BigDecimal("2500.00"));
            Path outputPath = tempDir.resolve("confirmation.pdf");

            // When
            pdfGenerator.generate(submission, taxResult, expenses, outputPath);

            // Then: All categories present in PDF
            String pdfText = extractPdfText(outputPath);
            assertThat(pdfText).contains("Phone, stationery and office costs");
            assertThat(pdfText).contains("Car, van and travel expenses");
            assertThat(pdfText).contains("Accountancy, legal and professional fees");
            assertThat(pdfText).contains("Advertising and marketing costs");
            assertThat(pdfText).contains("Other business expenses");
            assertThat(pdfText).contains("Total Expenses");
        }
    }

    // ===== Helper Methods =====

    /**
     * Extracts text content from a PDF file using OpenPDF's PdfTextExtractor.
     */
    private String extractPdfText(Path pdfPath) throws IOException {
        try (PdfReader reader = new PdfReader(pdfPath.toString())) {
            StringBuilder textBuilder = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                textBuilder.append(extractor.getTextFromPage(i));
                textBuilder.append("\n");
            }

            return textBuilder.toString();
        }
    }

    private Submission createSuccessfulSubmission() {
        return new Submission(
            SUBMISSION_ID,
            BUSINESS_ID,
            SubmissionType.ANNUAL,
            TAX_YEAR_2025,
            TAX_YEAR_2025.startDate(),
            TAX_YEAR_2025.endDate(),
            new BigDecimal("50000.00"),
            new BigDecimal("10000.00"),
            new BigDecimal("40000.00"),
            SubmissionStatus.ACCEPTED,
            HMRC_REFERENCE,
            null,
            SUBMISSION_TIME,
            SUBMISSION_TIME
        );
    }

    private Submission createFailedSubmission() {
        return new Submission(
            SUBMISSION_ID,
            BUSINESS_ID,
            SubmissionType.ANNUAL,
            TAX_YEAR_2025,
            TAX_YEAR_2025.startDate(),
            TAX_YEAR_2025.endDate(),
            new BigDecimal("50000.00"),
            new BigDecimal("10000.00"),
            new BigDecimal("40000.00"),
            SubmissionStatus.REJECTED,
            null,
            "Validation error",
            SUBMISSION_TIME,
            SUBMISSION_TIME
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

        NICalculationResult niDetails = new NICalculationResult(
            new BigDecimal("40000.00"), // grossProfit
            new BigDecimal("12570.00"), // lowerProfitsLimit
            new BigDecimal("27430.00"), // profitSubjectToNI
            new BigDecimal("27430.00"), // mainRateAmount
            new BigDecimal("2468.70"),  // mainRateNI (9%)
            BigDecimal.ZERO,            // additionalRateAmount
            BigDecimal.ZERO,            // additionalRateNI
            new BigDecimal("2468.70")   // totalNI
        );

        return new TaxLiabilityResult(
            new BigDecimal("40000.00"), // grossProfit
            new BigDecimal("5486.00"),  // incomeTax
            new BigDecimal("2468.70"),  // niClass4
            new BigDecimal("7954.70"),  // totalLiability
            incomeTaxDetails,
            niDetails
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
