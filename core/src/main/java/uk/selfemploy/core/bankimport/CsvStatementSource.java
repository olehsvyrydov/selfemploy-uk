package uk.selfemploy.core.bankimport;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A {@link StatementSource} backed by a bank CSV file.
 *
 * <p>Wraps the existing auto-detecting CSV pipeline: {@link BankFormatDetector} picks
 * the matching per-bank {@link BankCsvParser} from the file's headers, and its output is
 * returned as a {@link StatementBatch}. This is the first implementation of the source
 * port; a future Open Banking feed would be another implementation feeding the same
 * downstream processing unchanged.</p>
 */
public final class CsvStatementSource implements StatementSource {

    /** The {@link #sourceType()} value for CSV-backed sources. */
    public static final String SOURCE_TYPE = "csv";

    private final Path csvFile;
    private final Charset charset;
    private final BankFormatDetector formatDetector;

    /**
     * @param csvFile        the CSV file to read; required
     * @param charset        the character encoding of the file; required
     * @param formatDetector the detector holding the available bank parsers; required
     */
    public CsvStatementSource(Path csvFile, Charset charset, BankFormatDetector formatDetector) {
        this.csvFile = Objects.requireNonNull(csvFile, "csvFile");
        this.charset = Objects.requireNonNull(charset, "charset");
        this.formatDetector = Objects.requireNonNull(formatDetector, "formatDetector");
    }

    @Override
    public String sourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public StatementBatch fetch() throws StatementSourceException {
        String fileName = String.valueOf(csvFile.getFileName());
        try {
            BankCsvParser parser = formatDetector.detectFormat(csvFile, charset)
                .orElseThrow(() -> new StatementSourceException(
                    "No bank CSV format matched " + fileName));
            List<ImportedTransaction> transactions = parser.parse(csvFile, charset);
            return new StatementBatch(SOURCE_TYPE, fileName, parser.getBankName(), transactions);
        } catch (CsvParseException e) {
            throw new StatementSourceException("Failed to parse " + fileName, e);
        }
    }
}
