package uk.selfemploy.plugin.extension;

import java.util.List;

/**
 * Extension point for exporting data to various formats.
 *
 * <p>Plugins implement this interface to support exporting data to different
 * file formats or external systems. Exporters can handle income, expenses,
 * tax summaries, or any combination of data types.</p>
 *
 * <h2>Export Formats</h2>
 * <p>Common export formats include:</p>
 * <ul>
 *   <li>{@code CSV} - Comma-separated values for spreadsheets</li>
 *   <li>{@code XLSX} - Microsoft Excel format</li>
 *   <li>{@code QIF} - Quicken Interchange Format</li>
 *   <li>{@code JSON} - JavaScript Object Notation</li>
 *   <li>{@code SAGE} - Sage accounting software format</li>
 * </ul>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class SageExporter implements DataExporter {
 *     @Override
 *     public String getExporterId() {
 *         return "sage-export";
 *     }
 *
 *     @Override
 *     public String getExporterName() {
 *         return "Sage Accounting";
 *     }
 *
 *     @Override
 *     public List<String> getSupportedFormats() {
 *         return List.of("CSV", "SAGE");
 *     }
 *
 *     @Override
 *     public byte[] exportData(ExportContext context, String format) {
 *         // Generate export data
 *         return exportBytes;
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Export operations may be called from background threads. Implementations
 * must be thread-safe.</p>
 *
 * @see ExportContext
 * @see ExtensionPoint
 */
public interface DataExporter extends ExtensionPoint {

    /**
     * Returns the unique identifier for this exporter.
     *
     * <p>The ID must be unique across all data exporters. It is recommended
     * to use a namespaced format like "plugin-name.exporter-id".</p>
     *
     * @return the exporter ID, never null or blank
     */
    String getExporterId();

    /**
     * Returns the display name for this exporter.
     *
     * <p>This name is shown in the export dialog and format selection menus.
     * Include the target system name for clarity.</p>
     *
     * @return the exporter name, never null or blank
     */
    String getExporterName();

    /**
     * Returns a description of what this exporter produces.
     *
     * <p>This description helps users understand what format or system
     * this exporter targets.</p>
     *
     * @return the exporter description, never null (may be empty)
     */
    default String getExporterDescription() {
        return "";
    }

    /**
     * Returns the list of output formats this exporter supports.
     *
     * <p>Format identifiers should be uppercase (e.g., "CSV", "XLSX").
     * At least one format must be supported.</p>
     *
     * @return list of supported format identifiers, never null or empty
     */
    List<String> getSupportedFormats();

    /**
     * Returns the default file extension for the given format.
     *
     * <p>This extension is used when suggesting a filename for the export.
     * Should include the dot prefix (e.g., ".csv", ".xlsx").</p>
     *
     * <p>Default implementation returns a lowercase version of the format
     * with a dot prefix.</p>
     *
     * @param format the output format
     * @return the file extension for this format
     */
    default String getFileExtension(String format) {
        return "." + format.toLowerCase();
    }

    /**
     * Exports data in the specified format.
     *
     * <p>This method performs the actual export operation, generating the
     * output bytes in the requested format. The export context specifies
     * what data to include and any formatting options.</p>
     *
     * <p>This method may be called from a background thread. Implementations
     * must be thread-safe and should report progress for large exports.</p>
     *
     * @param context the export context specifying scope and options
     * @param format  the desired output format (must be in {@link #getSupportedFormats()})
     * @return the exported data as bytes, never null
     * @throws IllegalArgumentException if the format is not supported
     * @throws DataExportException if export fails
     */
    byte[] exportData(ExportContext context, String format);
}
