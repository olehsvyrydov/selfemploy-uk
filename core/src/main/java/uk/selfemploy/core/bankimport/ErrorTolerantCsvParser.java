package uk.selfemploy.core.bankimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps any BankCsvParser to provide error-tolerant parsing.
 *
 * <p>Instead of throwing an exception on the first malformed row,
 * this parser collects errors and continues parsing the remaining rows.
 * This is useful for the import wizard where the user should see all
 * valid transactions and be warned about problematic rows.</p>
 *
 * <p>Usage:
 * <pre>{@code
 * CsvParseResult result = ErrorTolerantCsvParser.parse(barclaysParser, csvFile, charset);
 * List<ImportedTransaction> valid = result.transactions();
 * List<CsvParseError> errors = result.errors();
 * }</pre>
 */
public final class ErrorTolerantCsvParser {

    private ErrorTolerantCsvParser() {
        // Utility class
    }

    /**
     * Parses a CSV file, collecting errors instead of throwing on malformed rows.
     *
     * <p>The parser delegates to the provided BankCsvParser's line-level parsing
     * logic but catches CsvParseException for individual rows. IO errors and
     * structural issues (like missing headers) still throw exceptions.</p>
     *
     * @param parser the bank-specific parser to use
     * @param csvFile path to the CSV file
     * @param charset character encoding of the file
     * @return result containing parsed transactions and any errors
     * @throws CsvParseException if the file cannot be read at all
     */
    public static CsvParseResult parse(BankCsvParser parser, Path csvFile, Charset charset) {
        if (parser instanceof AbstractBankCsvParser abstractParser) {
            return parseWithAbstractParser(abstractParser, csvFile, charset);
        }
        // For parsers that don't extend AbstractBankCsvParser (e.g., BarclaysCsvParser),
        // use reflection-free line-by-line approach
        return parseLineByLine(parser, csvFile, charset);
    }

    private static CsvParseResult parseWithAbstractParser(
            AbstractBankCsvParser parser, Path csvFile, Charset charset) {

        List<ImportedTransaction> transactions = new ArrayList<>();
        List<CsvParseError> errors = new ArrayList<>();
        String fileName = csvFile.getFileName().toString();
        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvFile, charset)) {
            // Skip header line
            reader.readLine();
            lineNumber++;

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {
                    ImportedTransaction tx = parser.parseLine(line, fileName, lineNumber);
                    if (tx != null) {
                        transactions.add(tx);
                    }
                } catch (CsvParseException e) {
                    errors.add(new CsvParseError(lineNumber, line, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new CsvParseException("Failed to read CSV file", fileName, lineNumber, e);
        }

        return new CsvParseResult(transactions, errors);
    }

    private static CsvParseResult parseLineByLine(
            BankCsvParser parser, Path csvFile, Charset charset) {

        List<ImportedTransaction> transactions = new ArrayList<>();
        List<CsvParseError> errors = new ArrayList<>();
        String fileName = csvFile.getFileName().toString();
        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvFile, charset)) {
            // Skip header line
            reader.readLine();
            lineNumber++;

            // Read all remaining lines
            List<String> dataLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!line.isBlank()) {
                    dataLines.add(line);
                }
            }

            // Try to parse each line individually by creating single-line temp files
            for (int i = 0; i < dataLines.size(); i++) {
                String dataLine = dataLines.get(i);
                int originalLineNumber = i + 2; // +2 for 1-based + header
                try {
                    // Create a temp file with header + single data line
                    Path tempFile = Files.createTempFile("csv_parse_", ".csv");
                    try {
                        String headerLine = Files.readAllLines(csvFile, charset).get(0);
                        Files.writeString(tempFile, headerLine + "\n" + dataLine + "\n", charset);
                        List<ImportedTransaction> parsed = parser.parse(tempFile, charset);
                        transactions.addAll(parsed);
                    } finally {
                        Files.deleteIfExists(tempFile);
                    }
                } catch (CsvParseException e) {
                    errors.add(new CsvParseError(originalLineNumber, dataLine, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new CsvParseException("Failed to read CSV file", fileName, lineNumber, e);
        }

        return new CsvParseResult(transactions, errors);
    }
}
