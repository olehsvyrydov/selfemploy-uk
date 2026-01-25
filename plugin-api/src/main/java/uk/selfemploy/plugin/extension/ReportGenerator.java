package uk.selfemploy.plugin.extension;

import java.util.List;

/**
 * Extension point for generating custom reports.
 *
 * <p>Plugins implement this interface to provide custom report generation
 * capabilities. Reports can be generated in various formats such as PDF,
 * Excel, or CSV, and can include any data accessible through the plugin API.</p>
 *
 * <h2>Supported Formats</h2>
 * <p>Common report formats include:</p>
 * <ul>
 *   <li>{@code PDF} - Portable Document Format for printable reports</li>
 *   <li>{@code XLSX} - Excel spreadsheet format</li>
 *   <li>{@code CSV} - Comma-separated values for data export</li>
 *   <li>{@code HTML} - Web format for browser viewing</li>
 * </ul>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class VatSummaryReportGenerator implements ReportGenerator {
 *     @Override
 *     public String getReportId() {
 *         return "vat-summary";
 *     }
 *
 *     @Override
 *     public String getReportName() {
 *         return "VAT Summary Report";
 *     }
 *
 *     @Override
 *     public String getReportDescription() {
 *         return "Generates a summary of VAT collected and paid.";
 *     }
 *
 *     @Override
 *     public List<String> getSupportedFormats() {
 *         return List.of("PDF", "XLSX");
 *     }
 *
 *     @Override
 *     public byte[] generateReport(ReportContext context, String format) {
 *         // Generate report bytes
 *         return reportBytes;
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Report generation may be called from background threads. Implementations
 * must be thread-safe.</p>
 *
 * @see ReportContext
 * @see ExtensionPoint
 */
public interface ReportGenerator extends ExtensionPoint {

    /**
     * Returns the unique identifier for this report.
     *
     * <p>The ID must be unique across all report generators. It is recommended
     * to use a namespaced format like "plugin-name.report-id".</p>
     *
     * @return the report ID, never null or blank
     */
    String getReportId();

    /**
     * Returns the display name for this report.
     *
     * <p>This name is shown in the reports menu and report selection dialogs.</p>
     *
     * @return the report name, never null or blank
     */
    String getReportName();

    /**
     * Returns a description of what this report contains.
     *
     * <p>This description helps users understand what information the report
     * includes and when it might be useful.</p>
     *
     * @return the report description, never null (may be empty)
     */
    String getReportDescription();

    /**
     * Returns the list of output formats this report supports.
     *
     * <p>Format identifiers should be uppercase (e.g., "PDF", "XLSX", "CSV").
     * At least one format must be supported.</p>
     *
     * @return list of supported format identifiers, never null or empty
     */
    List<String> getSupportedFormats();

    /**
     * Generates the report in the specified format.
     *
     * <p>This method performs the actual report generation, returning the
     * complete report content as a byte array. The format of the bytes
     * depends on the requested output format.</p>
     *
     * <p>This method may be called from a background thread. Implementations
     * must be thread-safe and should report progress for long-running operations.</p>
     *
     * @param context the report context specifying scope and parameters
     * @param format  the desired output format (must be in {@link #getSupportedFormats()})
     * @return the generated report as bytes, never null
     * @throws IllegalArgumentException if the format is not supported
     * @throws ReportGenerationException if report generation fails
     */
    byte[] generateReport(ReportContext context, String format);
}
