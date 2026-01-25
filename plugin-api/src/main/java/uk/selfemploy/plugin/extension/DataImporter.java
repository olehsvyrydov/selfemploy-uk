package uk.selfemploy.plugin.extension;

import java.nio.file.Path;
import java.util.List;

/**
 * Extension point for importing transaction data from external sources.
 *
 * <p>Plugins implement this interface to support importing data from various
 * sources such as bank statements, accounting software exports, or custom
 * file formats. Importers can handle income transactions, expenses, or both.</p>
 *
 * <h2>Supported Sources</h2>
 * <p>Common import sources include:</p>
 * <ul>
 *   <li>Bank statement exports (CSV, OFX, QIF)</li>
 *   <li>Accounting software exports</li>
 *   <li>Invoice management systems</li>
 *   <li>Payment processor reports</li>
 * </ul>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class BarclaysImporter implements DataImporter {
 *     @Override
 *     public String getImporterId() {
 *         return "barclays-csv";
 *     }
 *
 *     @Override
 *     public String getImporterName() {
 *         return "Barclays Bank Statement";
 *     }
 *
 *     @Override
 *     public List<String> getSupportedFileTypes() {
 *         return List.of(".csv");
 *     }
 *
 *     @Override
 *     public ImportResult importData(Path file, ImportContext context) {
 *         // Parse and import transactions
 *         return ImportResult.success(50, 3);
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Import operations may be called from background threads. Implementations
 * must be thread-safe.</p>
 *
 * @see ImportContext
 * @see ImportResult
 * @see ExtensionPoint
 */
public interface DataImporter extends ExtensionPoint {

    /**
     * Returns the unique identifier for this importer.
     *
     * <p>The ID must be unique across all data importers. It is recommended
     * to use a namespaced format like "plugin-name.importer-id".</p>
     *
     * @return the importer ID, never null or blank
     */
    String getImporterId();

    /**
     * Returns the display name for this importer.
     *
     * <p>This name is shown in the import dialog and source selection menus.
     * Include the source name (e.g., bank name) for clarity.</p>
     *
     * @return the importer name, never null or blank
     */
    String getImporterName();

    /**
     * Returns a description of what this importer handles.
     *
     * <p>This description helps users understand what data format or source
     * this importer supports.</p>
     *
     * @return the importer description, never null (may be empty)
     */
    default String getImporterDescription() {
        return "";
    }

    /**
     * Returns the list of file extensions this importer supports.
     *
     * <p>Extensions should include the dot prefix (e.g., ".csv", ".ofx").
     * These are used for file filtering in the import dialog.</p>
     *
     * @return list of supported file extensions, never null or empty
     */
    List<String> getSupportedFileTypes();

    /**
     * Returns whether this importer can handle the given file.
     *
     * <p>Implementations should check more than just the file extension,
     * such as file headers or content structure, to confirm compatibility.</p>
     *
     * <p>Default implementation checks if the file extension matches
     * {@link #getSupportedFileTypes()}.</p>
     *
     * @param file the file to check
     * @return true if this importer can handle the file
     */
    default boolean canHandle(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return getSupportedFileTypes().stream()
            .anyMatch(ext -> fileName.endsWith(ext.toLowerCase()));
    }

    /**
     * Imports transaction data from the specified file.
     *
     * <p>This method performs the actual import operation, parsing the file
     * and creating transactions in the system. The import context provides
     * the target tax year and any options.</p>
     *
     * <p>This method may be called from a background thread. Implementations
     * must be thread-safe and should report progress for large files.</p>
     *
     * @param file    the file to import data from
     * @param context the import context with options
     * @return the import result with counts and any errors
     * @throws IllegalArgumentException if the file cannot be read
     * @throws DataImportException if import fails due to data errors
     */
    ImportResult importData(Path file, ImportContext context);
}
