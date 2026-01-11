package uk.selfemploy.core.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.calculator.NICalculationResult;
import uk.selfemploy.core.calculator.TaxCalculationResult;
import uk.selfemploy.core.calculator.TaxLiabilityResult;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/**
 * Generates PDF confirmation documents for HMRC submissions.
 *
 * <p>This generator creates professional-looking PDF documents containing:
 * <ul>
 *   <li>Header with logo placeholder and title</li>
 *   <li>HMRC reference, submission date, and tax year</li>
 *   <li>Income and expense summaries by SA103 category</li>
 *   <li>Tax calculation breakdown (Income Tax + NI Class 4)</li>
 *   <li>Payment deadline warning</li>
 *   <li>Declaration timestamp</li>
 * </ul>
 *
 * <p>Uses OpenPDF library (LGPL license) for PDF generation.</p>
 *
 * @see <a href="https://github.com/LibrePDF/OpenPDF">OpenPDF GitHub</a>
 */
@ApplicationScoped
public class SubmissionPdfGenerator {

    // Fonts
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font SUBHEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font WARNING_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(139, 0, 0));

    // Colors
    private static final Color TABLE_HEADER_BG = new Color(236, 240, 241);
    private static final Color WARNING_BG_COLOR = new Color(255, 235, 238);
    private static final Color WARNING_BORDER_COLOR = new Color(198, 40, 40);

    // Formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm");

    // Layout
    private static final float LOGO_PLACEHOLDER_HEIGHT = 50f;

    /**
     * Generates a PDF confirmation document and saves it to the specified path.
     *
     * @param submission the successful HMRC submission
     * @param taxResult the tax calculation result
     * @param expensesByCategory expenses broken down by SA103 category (can be null)
     * @param outputPath the path to save the PDF to
     * @throws PdfGenerationException if PDF generation fails
     */
    public void generate(Submission submission, TaxLiabilityResult taxResult,
                         Map<ExpenseCategory, BigDecimal> expensesByCategory,
                         Path outputPath) throws PdfGenerationException {
        validateOutputPath(outputPath);
        validateInputs(submission, taxResult, outputPath);

        Map<ExpenseCategory, BigDecimal> expenses = expensesByCategory != null
                ? expensesByCategory
                : new EnumMap<>(ExpenseCategory.class);

        try {
            // Create parent directories if needed
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }

            try (OutputStream fos = Files.newOutputStream(outputPath)) {
                generatePdf(submission, taxResult, expenses, fos);
            }
        } catch (IOException e) {
            throw new PdfGenerationException("Failed to write PDF to file: " + outputPath, e);
        }
    }

    /**
     * Generates a PDF confirmation document as a byte array.
     *
     * @param submission the successful HMRC submission
     * @param taxResult the tax calculation result
     * @param expensesByCategory expenses broken down by SA103 category (can be null)
     * @return the PDF document as a byte array
     * @throws PdfGenerationException if PDF generation fails
     */
    public byte[] generateBytes(Submission submission, TaxLiabilityResult taxResult,
                                Map<ExpenseCategory, BigDecimal> expensesByCategory) throws PdfGenerationException {
        validateInputs(submission, taxResult, null);

        Map<ExpenseCategory, BigDecimal> expenses = expensesByCategory != null
                ? expensesByCategory
                : new EnumMap<>(ExpenseCategory.class);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        generatePdf(submission, taxResult, expenses, baos);
        return baos.toByteArray();
    }

    /**
     * Generates a safe filename for the PDF based on submission details.
     *
     * @param submission the submission to generate filename for
     * @return a safe filename ending in .pdf
     * @throws PdfGenerationException if submission is null
     */
    public String generateFilename(Submission submission) throws PdfGenerationException {
        if (submission == null) {
            throw new PdfGenerationException("Submission is required to generate filename");
        }

        String taxYear = submission.taxYear().label().replace("/", "-");
        String reference = submission.hmrcReference() != null
                ? submission.hmrcReference()
                : submission.id().toString().substring(0, 8);

        return String.format("SA-Confirmation_%s_%s.pdf", taxYear, reference);
    }

    /**
     * Calculates the payment deadline for a given tax year.
     *
     * @param taxYear the tax year
     * @return the payment deadline (31 January following the tax year end)
     * @throws PdfGenerationException if tax year is null
     */
    public LocalDate calculatePaymentDeadline(TaxYear taxYear) throws PdfGenerationException {
        if (taxYear == null) {
            throw new PdfGenerationException("Tax year is required to calculate payment deadline");
        }
        return taxYear.paymentDeadline();
    }

    private void validateInputs(Submission submission, TaxLiabilityResult taxResult, Path outputPath)
            throws PdfGenerationException {
        if (submission == null) {
            throw new PdfGenerationException("Submission is required");
        }
        if (taxResult == null) {
            throw new PdfGenerationException("Tax calculation result is required");
        }
        if (!submission.isSuccessful()) {
            throw new PdfGenerationException("Cannot generate PDF for non-successful submission");
        }
        if (submission.hmrcReference() == null || submission.hmrcReference().isBlank()) {
            throw new PdfGenerationException("HMRC reference is required for PDF generation");
        }
    }

    private void validateOutputPath(Path outputPath) throws PdfGenerationException {
        if (outputPath == null) {
            throw new PdfGenerationException("Output path is required");
        }
        if (outputPath.toString().isEmpty()) {
            throw new PdfGenerationException("Output path cannot be empty");
        }
    }

    private void generatePdf(Submission submission, TaxLiabilityResult taxResult,
                             Map<ExpenseCategory, BigDecimal> expenses, OutputStream outputStream)
            throws PdfGenerationException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add content sections
            addHeader(document);
            addReferenceSection(document, submission);
            addIncomeSummary(document, submission);
            addExpenseSummary(document, expenses);
            addTaxCalculation(document, taxResult);
            addPaymentDeadlineWarning(document, submission.taxYear());
            addFooter(document, submission);

        } catch (DocumentException e) {
            throw new PdfGenerationException("Failed to generate PDF document", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void addHeader(Document document) throws DocumentException {
        // Logo placeholder
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 3});

        // Logo placeholder cell
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.BOX);
        logoCell.setBorderColor(Color.LIGHT_GRAY);
        logoCell.setFixedHeight(LOGO_PLACEHOLDER_HEIGHT);
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPhrase(new Phrase("LOGO", SMALL_FONT));
        headerTable.addCell(logoCell);

        // Title cell
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.setPaddingLeft(15);

        Paragraph titlePara = new Paragraph();
        titlePara.add(new Chunk("Self Assessment", TITLE_FONT));
        titlePara.add(Chunk.NEWLINE);
        titlePara.add(new Chunk("Submission Confirmation", HEADER_FONT));
        titleCell.addElement(titlePara);
        headerTable.addCell(titleCell);

        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    private void addReferenceSection(Document document, Submission submission) throws DocumentException {
        PdfPTable refTable = new PdfPTable(2);
        refTable.setWidthPercentage(100);
        refTable.setSpacingBefore(10);
        refTable.setSpacingAfter(15);
        refTable.setWidths(new float[]{1, 2});

        // HMRC Reference
        addLabelValueRow(refTable, "HMRC Reference:", submission.hmrcReference());

        // Submission Date
        String submissionDate = submission.submittedAt()
                .atZone(ZoneId.systemDefault())
                .format(DATETIME_FORMATTER);
        addLabelValueRow(refTable, "Submission Date:", submissionDate);

        // Tax Year
        addLabelValueRow(refTable, "Tax Year:", submission.taxYear().label());

        // Period
        String period = submission.periodStart().format(DATE_FORMATTER) + " to "
                + submission.periodEnd().format(DATE_FORMATTER);
        addLabelValueRow(refTable, "Period:", period);

        // Submission Type
        addLabelValueRow(refTable, "Submission Type:", submission.type().getShortName());

        document.add(refTable);
    }

    private void addIncomeSummary(Document document, Submission submission) throws DocumentException {
        Paragraph header = new Paragraph("Income Summary", SUBHEADER_FONT);
        header.setSpacingBefore(10);
        header.setSpacingAfter(5);
        document.add(header);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1});

        addTableHeader(table, "Description", "Amount");

        addTableRow(table, "Total Income (Turnover)", formatCurrency(submission.totalIncome()));
        addTableRow(table, "Total Expenses", formatCurrency(submission.totalExpenses()));

        // Net Profit row (highlighted)
        PdfPCell labelCell = new PdfPCell(new Phrase("Net Profit", BOLD_FONT));
        labelCell.setBorder(Rectangle.TOP);
        labelCell.setBorderColorTop(Color.BLACK);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(formatCurrency(submission.netProfit()), BOLD_FONT));
        valueCell.setBorder(Rectangle.TOP);
        valueCell.setBorderColorTop(Color.BLACK);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);

        document.add(table);
    }

    private void addExpenseSummary(Document document, Map<ExpenseCategory, BigDecimal> expenses)
            throws DocumentException {
        if (expenses.isEmpty()) {
            return;
        }

        Paragraph header = new Paragraph("Expense Breakdown by Category (SA103F)", SUBHEADER_FONT);
        header.setSpacingBefore(15);
        header.setSpacingAfter(5);
        document.add(header);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 4, 1});

        addTableHeader(table, "Box", "Category", "Amount");

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<ExpenseCategory, BigDecimal> entry : expenses.entrySet()) {
            ExpenseCategory category = entry.getKey();
            BigDecimal amount = entry.getValue();

            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                addExpenseRow(table, category.getSa103Box(), category.getDisplayName(), amount);
                total = total.add(amount);
            }
        }

        // Total row
        PdfPCell boxCell = new PdfPCell(new Phrase("", BOLD_FONT));
        boxCell.setBorder(Rectangle.TOP);
        boxCell.setBorderColorTop(Color.BLACK);
        boxCell.setPadding(5);
        table.addCell(boxCell);

        PdfPCell labelCell = new PdfPCell(new Phrase("Total Expenses", BOLD_FONT));
        labelCell.setBorder(Rectangle.TOP);
        labelCell.setBorderColorTop(Color.BLACK);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(formatCurrency(total), BOLD_FONT));
        valueCell.setBorder(Rectangle.TOP);
        valueCell.setBorderColorTop(Color.BLACK);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);

        document.add(table);
    }

    private void addTaxCalculation(Document document, TaxLiabilityResult taxResult) throws DocumentException {
        Paragraph header = new Paragraph("Tax Calculation", SUBHEADER_FONT);
        header.setSpacingBefore(15);
        header.setSpacingAfter(5);
        document.add(header);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1});

        // Income Tax section
        addSectionHeader(table, "Income Tax");
        TaxCalculationResult itDetails = taxResult.incomeTaxDetails();
        addTableRow(table, "Gross Income", formatCurrency(itDetails.grossIncome()));
        addTableRow(table, "Personal Allowance", formatCurrency(itDetails.personalAllowance().negate()));
        addTableRow(table, "Taxable Income", formatCurrency(itDetails.taxableIncome()));

        if (itDetails.basicRateTax().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Basic Rate (20%)", formatCurrency(itDetails.basicRateTax()));
        }
        if (itDetails.higherRateTax().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Higher Rate (40%)", formatCurrency(itDetails.higherRateTax()));
        }
        if (itDetails.additionalRateTax().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Additional Rate (45%)", formatCurrency(itDetails.additionalRateTax()));
        }
        addBoldRow(table, "Total Income Tax", formatCurrency(taxResult.incomeTax()));

        // NI Class 4 section
        addSectionHeader(table, "National Insurance Class 4");
        NICalculationResult niDetails = taxResult.niDetails();
        addTableRow(table, "Profit Subject to NI", formatCurrency(niDetails.profitSubjectToNI()));

        if (niDetails.mainRateNI().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Main Rate (9%)", formatCurrency(niDetails.mainRateNI()));
        }
        if (niDetails.additionalRateNI().compareTo(BigDecimal.ZERO) > 0) {
            addTableRow(table, "Additional Rate (2%)", formatCurrency(niDetails.additionalRateNI()));
        }
        addBoldRow(table, "Total NI Class 4", formatCurrency(taxResult.niClass4()));

        // Total Liability
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL TAX LIABILITY", HEADER_FONT));
        totalLabelCell.setBorder(Rectangle.TOP);
        totalLabelCell.setBorderColorTop(Color.BLACK);
        totalLabelCell.setBorderWidthTop(2);
        totalLabelCell.setPadding(8);
        totalLabelCell.setBackgroundColor(TABLE_HEADER_BG);
        table.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(taxResult.totalLiability()), HEADER_FONT));
        totalValueCell.setBorder(Rectangle.TOP);
        totalValueCell.setBorderColorTop(Color.BLACK);
        totalValueCell.setBorderWidthTop(2);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPadding(8);
        totalValueCell.setBackgroundColor(TABLE_HEADER_BG);
        table.addCell(totalValueCell);

        document.add(table);

        // Effective rate
        Paragraph effectiveRate = new Paragraph(
                String.format("Effective Tax Rate: %s%%", taxResult.effectiveRate().toPlainString()),
                SMALL_FONT
        );
        effectiveRate.setSpacingBefore(5);
        document.add(effectiveRate);
    }

    private void addPaymentDeadlineWarning(Document document, TaxYear taxYear) throws DocumentException {
        LocalDate deadline = taxYear.paymentDeadline();

        PdfPTable warningTable = new PdfPTable(1);
        warningTable.setWidthPercentage(100);
        warningTable.setSpacingBefore(20);

        PdfPCell warningCell = new PdfPCell();
        warningCell.setBorder(Rectangle.BOX);
        warningCell.setBorderColor(WARNING_BORDER_COLOR);
        warningCell.setBorderWidth(2);
        warningCell.setBackgroundColor(WARNING_BG_COLOR);
        warningCell.setPadding(15);

        Paragraph warningTitle = new Paragraph("Payment Deadline", WARNING_FONT);
        warningTitle.setSpacingAfter(5);

        Paragraph warningText = new Paragraph();
        warningText.add(new Chunk("Your payment is due by ", NORMAL_FONT));
        warningText.add(new Chunk(deadline.format(DATE_FORMATTER), BOLD_FONT));
        warningText.add(new Chunk(". Interest may be charged on late payments.", NORMAL_FONT));

        warningCell.addElement(warningTitle);
        warningCell.addElement(warningText);

        warningTable.addCell(warningCell);
        document.add(warningTable);
    }

    private void addFooter(Document document, Submission submission) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(30);

        footer.add(new Chunk("Declaration", SUBHEADER_FONT));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("This document confirms your Self Assessment submission to HMRC. ", NORMAL_FONT));
        footer.add(new Chunk("Keep this confirmation for your records.", NORMAL_FONT));
        footer.add(Chunk.NEWLINE);
        footer.add(Chunk.NEWLINE);

        String generatedAt = java.time.LocalDateTime.now().format(DATETIME_FORMATTER);
        footer.add(new Chunk("Generated: " + generatedAt, SMALL_FONT));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("Reference ID: " + submission.id().toString(), SMALL_FONT));

        document.add(footer);
    }

    // Helper methods for table construction

    private void addLabelValueRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, BOLD_FONT));
            cell.setBackgroundColor(TABLE_HEADER_BG);
            cell.setPadding(5);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColorBottom(Color.GRAY);
            if (header.equals("Amount")) {
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addExpenseRow(PdfPTable table, String box, String category, BigDecimal amount) {
        PdfPCell boxCell = new PdfPCell(new Phrase(box, NORMAL_FONT));
        boxCell.setBorder(Rectangle.NO_BORDER);
        boxCell.setPadding(5);
        boxCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(boxCell);

        PdfPCell categoryCell = new PdfPCell(new Phrase(category, NORMAL_FONT));
        categoryCell.setBorder(Rectangle.NO_BORDER);
        categoryCell.setPadding(5);
        table.addCell(categoryCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(formatCurrency(amount), NORMAL_FONT));
        amountCell.setBorder(Rectangle.NO_BORDER);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amountCell.setPadding(5);
        table.addCell(amountCell);
    }

    private void addBoldRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BOLD_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addSectionHeader(PdfPTable table, String title) {
        PdfPCell cell = new PdfPCell(new Phrase(title, SUBHEADER_FONT));
        cell.setColspan(2);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(10);
        cell.setPaddingBottom(5);
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return String.format("\u00A3%,.2f", amount);
    }
}
