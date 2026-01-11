package uk.selfemploy.core.bankimport;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * Strategy interface for parsing bank-specific CSV formats.
 *
 * <p>Each UK bank exports transaction data in a different CSV format.
 * Implementations of this interface handle the specific parsing logic
 * for each bank format.</p>
 *
 * <p>Supported banks:
 * <ul>
 *   <li>Barclays</li>
 *   <li>HSBC</li>
 *   <li>Lloyds</li>
 *   <li>Nationwide</li>
 *   <li>Starling</li>
 *   <li>Monzo</li>
 * </ul>
 */
public interface BankCsvParser {

    /**
     * Returns the display name of the bank this parser handles.
     *
     * @return the bank name (e.g., "Barclays", "HSBC")
     */
    String getBankName();

    /**
     * Checks if this parser can handle a CSV file with the given headers.
     *
     * <p>This method is used by the bank format detector to auto-detect
     * which parser should be used for a given CSV file.</p>
     *
     * @param headers the header row from the CSV file
     * @return true if this parser can parse the CSV format
     */
    boolean canParse(String[] headers);

    /**
     * Parses a CSV file and returns a list of imported transactions.
     *
     * <p>The parser should:
     * <ul>
     *   <li>Parse dates according to the bank's format</li>
     *   <li>Handle income/expense amounts (positive/negative)</li>
     *   <li>Extract description text</li>
     *   <li>Optionally extract balance and reference</li>
     * </ul>
     *
     * @param csvFile the path to the CSV file
     * @param charset the character encoding of the file
     * @return list of parsed transactions
     * @throws CsvParseException if the file cannot be parsed
     */
    List<ImportedTransaction> parse(Path csvFile, Charset charset) throws CsvParseException;

    /**
     * Returns the expected column headers for this bank format.
     *
     * <p>Used for format detection and validation.</p>
     *
     * @return array of expected header names
     */
    String[] getExpectedHeaders();
}
