package uk.selfemploy.ui.service;

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
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.ui.viewmodel.SubmissionTableRow;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for generating PDF confirmation documents from submission history.
 *
 * <p>SE-SH-005: PDF Download functionality</p>
 *
 * <p>Generates PDF documents containing:</p>
 * <ul>
 *   <li>Submission type and tax year</li>
 *   <li>HMRC reference number</li>
 *   <li>Submission date/time</li>
 *   <li>Status (Accepted/Rejected/Pending)</li>
 *   <li>Financial summary (income, expenses, net profit, tax due)</li>
 *   <li>Error message (if rejected)</li>
 * </ul>
 *
 * <p>Uses OpenPDF library (LGPL license) for PDF generation.</p>
 */
public class SubmissionPdfDownloadService {

    private static final Logger LOG = Logger.getLogger(SubmissionPdfDownloadService.class.getName());

    // Fonts
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font SUBHEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font ERROR_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(139, 0, 0));

    // Colors
    private static final Color TABLE_HEADER_BG = new Color(236, 240, 241);
    private static final Color STATUS_ACCEPTED_BG = new Color(212, 237, 218);
    private static final Color STATUS_REJECTED_BG = new Color(248, 215, 218);
    private static final Color STATUS_PENDING_BG = new Color(255, 243, 205);

    // Formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm");

    /**
     * Generates a filename for the PDF based on submission details.
     *
     * @param submission the submission to generate filename for
     * @return a safe filename ending in .pdf
     * @throws IllegalArgumentException if submission is null
     */
    public String generateFilename(SubmissionTableRow submission) {
        if (submission == null) {
            throw new IllegalArgumentException("Submission is required to generate filename");
        }

        String type = submission.type() != null
            ? submission.type().getShortName().toLowerCase().replace(" ", "-")
            : "submission";

        String taxYear = submission.taxYear() != null
            ? submission.taxYear().replace("/", "-")
            : "unknown";

        String reference = submission.hmrcReference() != null && !submission.hmrcReference().isBlank()
            ? sanitizeForFilename(submission.hmrcReference())
            : "id-" + submission.id();

        return String.format("submission-%s-%s-%s.pdf", type, taxYear, reference);
    }

    /**
     * Generates a PDF file and saves it to the specified path.
     *
     * @param submission the submission to generate PDF for
     * @param outputPath the path to save the PDF to
     * @throws IOException if PDF generation or file writing fails
     */
    public void generatePdf(SubmissionTableRow submission, Path outputPath) throws IOException {
        if (submission == null) {
            throw new IllegalArgumentException("Submission is required");
        }
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path is required");
        }

        // Create parent directories if needed
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        try (OutputStream fos = Files.newOutputStream(outputPath)) {
            generatePdfToStream(submission, fos);
        }
    }

    /**
     * Generates a PDF as a byte array.
     *
     * @param submission the submission to generate PDF for
     * @return the PDF document as a byte array
     * @throws IOException if PDF generation fails
     */
    public byte[] generatePdfBytes(SubmissionTableRow submission) throws IOException {
        if (submission == null) {
            throw new IllegalArgumentException("Submission is required");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        generatePdfToStream(submission, baos);
        return baos.toByteArray();
    }

    /**
     * Returns the user's Downloads directory.
     *
     * @return the path to the Downloads directory
     */
    public Path getDownloadsDirectory() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        // Try standard Downloads folder first
        Path downloads = Paths.get(userHome, "Downloads");
        if (Files.isDirectory(downloads)) {
            return downloads;
        }

        // Linux XDG
        if (os.contains("nux")) {
            String xdgDownloads = System.getenv("XDG_DOWNLOAD_DIR");
            if (xdgDownloads != null) {
                Path xdgPath = Paths.get(xdgDownloads);
                if (Files.isDirectory(xdgPath)) {
                    return xdgPath;
                }
            }
        }

        // Fall back to home directory
        return Paths.get(userHome);
    }

    private void generatePdfToStream(SubmissionTableRow submission, OutputStream outputStream) throws IOException {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add content sections
            addHeader(document, submission);
            addStatusSection(document, submission);
            addReferenceSection(document, submission);
            addFinancialSummary(document, submission);

            if (submission.isRejected() && submission.hasError()) {
                addErrorSection(document, submission);
            }

            addFooter(document);

        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF document", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void addHeader(Document document, SubmissionTableRow submission) throws DocumentException {
        // Logo placeholder
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 3});

        // Logo placeholder cell
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.BOX);
        logoCell.setBorderColor(Color.LIGHT_GRAY);
        logoCell.setFixedHeight(50f);
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
        titlePara.add(new Chunk("Submission Confirmation", TITLE_FONT));
        titlePara.add(Chunk.NEWLINE);

        String subtitle = submission.type() != null
            ? submission.type().getDisplayName()
            : "HMRC Submission";
        titlePara.add(new Chunk(subtitle, HEADER_FONT));
        titleCell.addElement(titlePara);
        headerTable.addCell(titleCell);

        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    private void addStatusSection(Document document, SubmissionTableRow submission) throws DocumentException {
        PdfPTable statusTable = new PdfPTable(1);
        statusTable.setWidthPercentage(100);
        statusTable.setSpacingBefore(10);
        statusTable.setSpacingAfter(15);

        Color statusBg = getStatusBackgroundColor(submission.status());
        PdfPCell statusCell = new PdfPCell();
        statusCell.setBackgroundColor(statusBg);
        statusCell.setBorder(Rectangle.NO_BORDER);
        statusCell.setPadding(15);

        Paragraph statusPara = new Paragraph();
        String statusIcon = getStatusIcon(submission.status());
        String statusText = submission.getStatusDisplay();
        statusPara.add(new Chunk(statusIcon + " " + statusText, HEADER_FONT));
        statusPara.add(Chunk.NEWLINE);

        if (submission.submittedAt() != null) {
            String dateText = "Submitted on " + submission.submittedAt().format(DATETIME_FORMATTER);
            statusPara.add(new Chunk(dateText, NORMAL_FONT));
        }

        statusCell.addElement(statusPara);
        statusTable.addCell(statusCell);
        document.add(statusTable);
    }

    private void addReferenceSection(Document document, SubmissionTableRow submission) throws DocumentException {
        Paragraph header = new Paragraph("Submission Details", SUBHEADER_FONT);
        header.setSpacingBefore(10);
        header.setSpacingAfter(5);
        document.add(header);

        PdfPTable refTable = new PdfPTable(2);
        refTable.setWidthPercentage(100);
        refTable.setWidths(new float[]{1, 2});

        // HMRC Reference
        addLabelValueRow(refTable, "HMRC Reference:", submission.getReferenceDisplay());

        // Tax Year
        addLabelValueRow(refTable, "Tax Year:", submission.taxYear() != null ? submission.taxYear() : "-");

        // Submission Type
        addLabelValueRow(refTable, "Type:", submission.getTypeDisplayName());

        // Submission Date
        String dateStr = submission.submittedAt() != null
            ? submission.submittedAt().format(DATETIME_FORMATTER)
            : "-";
        addLabelValueRow(refTable, "Submitted:", dateStr);

        document.add(refTable);
    }

    private void addFinancialSummary(Document document, SubmissionTableRow submission) throws DocumentException {
        Paragraph header = new Paragraph("Financial Summary", SUBHEADER_FONT);
        header.setSpacingBefore(15);
        header.setSpacingAfter(5);
        document.add(header);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1});

        addTableHeader(table, "Description", "Amount");

        addTableRow(table, "Total Income", submission.getFormattedIncome());
        addTableRow(table, "Total Expenses", submission.getFormattedExpenses());

        // Net Profit row (highlighted)
        addBoldRow(table, "Net Profit", submission.getFormattedProfit());

        // Tax Due (if available)
        if (submission.taxDue() != null) {
            addBoldRow(table, "Tax Due", submission.getFormattedTaxDue());
        }

        document.add(table);
    }

    private void addErrorSection(Document document, SubmissionTableRow submission) throws DocumentException {
        Paragraph header = new Paragraph("Error Details", SUBHEADER_FONT);
        header.setSpacingBefore(15);
        header.setSpacingAfter(5);
        document.add(header);

        PdfPTable errorTable = new PdfPTable(1);
        errorTable.setWidthPercentage(100);

        PdfPCell errorCell = new PdfPCell();
        errorCell.setBackgroundColor(STATUS_REJECTED_BG);
        errorCell.setBorder(Rectangle.BOX);
        errorCell.setBorderColor(new Color(198, 40, 40));
        errorCell.setBorderWidth(1);
        errorCell.setPadding(12);

        Paragraph errorPara = new Paragraph(submission.errorMessage(), ERROR_FONT);
        errorCell.addElement(errorPara);
        errorTable.addCell(errorCell);

        document.add(errorTable);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(30);

        footer.add(new Chunk("This document is a record of your HMRC submission. ", NORMAL_FONT));
        footer.add(new Chunk("Keep this confirmation for your records.", NORMAL_FONT));
        footer.add(Chunk.NEWLINE);
        footer.add(Chunk.NEWLINE);

        String generatedAt = LocalDateTime.now().format(DATETIME_FORMATTER);
        footer.add(new Chunk("Generated: " + generatedAt, SMALL_FONT));
        footer.add(Chunk.NEWLINE);
        footer.add(new Chunk("UK Self-Employment Manager", SMALL_FONT));

        document.add(footer);
    }

    // Helper methods

    private void addLabelValueRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "-", NORMAL_FONT));
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

    private void addBoldRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, BOLD_FONT));
        labelCell.setBorder(Rectangle.TOP);
        labelCell.setBorderColorTop(Color.BLACK);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BOLD_FONT));
        valueCell.setBorder(Rectangle.TOP);
        valueCell.setBorderColorTop(Color.BLACK);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private Color getStatusBackgroundColor(SubmissionStatus status) {
        if (status == null) {
            return TABLE_HEADER_BG;
        }
        return switch (status) {
            case ACCEPTED -> STATUS_ACCEPTED_BG;
            case REJECTED -> STATUS_REJECTED_BG;
            case PENDING, SUBMITTED -> STATUS_PENDING_BG;
        };
    }

    private String getStatusIcon(SubmissionStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case ACCEPTED -> "[OK]";
            case REJECTED -> "[X]";
            case PENDING -> "[...]";
            case SUBMITTED -> "[>]";
        };
    }

    private String sanitizeForFilename(String input) {
        if (input == null) {
            return "";
        }
        // Replace invalid filename characters with underscores
        return input.replaceAll("[/\\\\:*?\"<>|]", "_");
    }
}
