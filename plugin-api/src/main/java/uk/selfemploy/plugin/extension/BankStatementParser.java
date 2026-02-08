package uk.selfemploy.plugin.extension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Extension point for parsing bank statements into structured transactions.
 *
 * <p>Extends {@link DataImporter} for backward compatibility with the generic
 * import dialog, and adds bank-statement-specific capabilities: format detection,
 * preview parsing, and structured transaction output.</p>
 *
 * <h2>Priority Ranges</h2>
 * <ul>
 *   <li><b>0-99:</b> Built-in parsers (e.g., CSV parser at priority 10)</li>
 *   <li><b>100+:</b> Plugin parsers (default priority from {@link Prioritizable})</li>
 * </ul>
 *
 * <h2>Column Mapping</h2>
 * <p>Self-describing formats (OFX, QIF) return {@code false} from
 * {@link #requiresColumnMapping()}. CSV parsers return {@code true}
 * because they need user-configured column mapping.</p>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class OfxParser implements BankStatementParser {
 *     public String getFormatId() { return "ofx"; }
 *     public boolean requiresColumnMapping() { return false; }
 *     // ... implement remaining methods
 * }
 * }</pre>
 *
 * @see DataImporter
 * @see Prioritizable
 * @see ParsedTransaction
 * @see StatementParseRequest
 * @see StatementParseResult
 */
public interface BankStatementParser extends DataImporter, Prioritizable {

    /**
     * Returns the unique format identifier for this parser (e.g., "csv", "ofx", "qif").
     *
     * @return the format ID, never null or blank
     */
    String getFormatId();

    /**
     * Returns the set of bank-specific format identifiers this parser supports.
     *
     * <p>Format IDs use lowercase kebab-case (e.g., "csv-barclays", "csv-monzo",
     * "ofx-generic"). These IDs are used for parser lookup and UI display.</p>
     *
     * @return unmodifiable set of supported bank format IDs, never null
     */
    Set<String> getSupportedBankFormats();

    /**
     * Parses all transactions from a bank statement using the provided configuration.
     *
     * @param request parsing configuration with column hints and options
     * @return result containing parsed transactions, warnings, and errors
     * @throws DataImportException if a fatal parsing error occurs
     */
    StatementParseResult parseStatement(StatementParseRequest request);

    /**
     * Attempts to detect the format from file content.
     *
     * <p>Implementations should inspect file headers and structure,
     * not just file extensions, when possible.</p>
     *
     * @param file the file to inspect
     * @return the format ID if recognized, or empty if unrecognized
     */
    Optional<String> detectFormat(Path file);

    /**
     * Parses a preview of the first N transactions for wizard display.
     *
     * <p>Implementations may return fewer transactions than maxRows if
     * the file contains fewer rows.</p>
     *
     * @param file    the statement file
     * @param maxRows maximum number of transactions to return
     * @return list of preview transactions
     */
    List<ParsedTransaction> parsePreview(Path file, int maxRows);

    /**
     * Whether this parser requires user-configured column mapping.
     *
     * <p>CSV parsers return {@code true} because CSV column names vary by bank.
     * Self-describing formats like OFX and QIF return {@code false}.</p>
     *
     * <p>Default implementation returns {@code false} since most structured
     * formats (OFX, QIF) are self-describing.</p>
     *
     * @return true if column mapping is required, false otherwise
     */
    default boolean requiresColumnMapping() {
        return false;
    }

    /**
     * Default bridge from {@link DataImporter#importData(Path, ImportContext)} to
     * {@link #parseStatement(StatementParseRequest)}.
     *
     * <p>This provides backward compatibility with the generic import dialog.
     * The method detects the format, parses the file using auto-detect settings,
     * and converts the result to an {@link ImportResult}.</p>
     *
     * @param file    the file to import data from
     * @param context the import context with options
     * @return the import result with counts and any errors
     */
    @Override
    default ImportResult importData(Path file, ImportContext context) {
        StatementParseResult result = parseStatement(StatementParseRequest.autoDetect());

        if (result.hasErrors()) {
            return ImportResult.failure(String.join("; ", result.errors()));
        }

        return new ImportResult(
            result.transactions().size(),
            0,
            0,
            result.errors(),
            result.warnings()
        );
    }
}
