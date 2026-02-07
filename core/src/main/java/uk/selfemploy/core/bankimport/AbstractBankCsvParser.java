package uk.selfemploy.core.bankimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Abstract base class for bank CSV parsers.
 *
 * <p>Provides common functionality for parsing CSV files from UK banks.</p>
 */
public abstract class AbstractBankCsvParser implements BankCsvParser {

    protected static final DateTimeFormatter DATE_FORMAT_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    protected static final DateTimeFormatter DATE_FORMAT_DASH_SHORT = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.UK);
    protected static final DateTimeFormatter DATE_FORMAT_ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public boolean canParse(String[] headers) {
        String[] expected = getExpectedHeaders();

        if (headers.length != expected.length) {
            return false;
        }

        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equalsIgnoreCase(headers[i].trim())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<ImportedTransaction> parse(Path csvFile, Charset charset) throws CsvParseException {
        List<ImportedTransaction> transactions = new ArrayList<>();
        String fileName = csvFile.getFileName().toString();
        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvFile, charset)) {
            // Skip header line
            String line = reader.readLine();
            lineNumber++;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                ImportedTransaction transaction = parseLine(line, fileName, lineNumber);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }
        } catch (IOException e) {
            throw new CsvParseException("Failed to read CSV file", fileName, lineNumber, e);
        }

        return transactions;
    }

    /**
     * Parses a single CSV line into an ImportedTransaction.
     *
     * @param line the CSV line to parse
     * @param fileName the file name (for error messages)
     * @param lineNumber the line number (for error messages)
     * @return the parsed transaction, or null to skip the line
     */
    protected abstract ImportedTransaction parseLine(String line, String fileName, int lineNumber);

    /**
     * Parses a CSV line into individual fields.
     *
     * <p>Handles quoted fields properly.</p>
     */
    protected String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Add the last field
        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }

    /**
     * Parses a date string using multiple format attempts.
     */
    protected LocalDate parseDate(String dateStr, String fileName, int lineNumber, DateTimeFormatter... formats) {
        if (dateStr.isBlank()) {
            throw new CsvParseException("Empty date not allowed", fileName, lineNumber);
        }

        for (DateTimeFormatter format : formats) {
            try {
                return LocalDate.parse(dateStr.trim(), format);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        throw new CsvParseException("Invalid date format: " + dateStr, fileName, lineNumber);
    }

    /**
     * Cleans and parses an amount string.
     *
     * <p>Handles currency symbols, thousand separators, and whitespace.</p>
     */
    protected BigDecimal cleanAndParseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            return null;
        }

        String cleaned = amountStr
            .replace("GBP", "")
            .replace("£", "")
            .replace(",", "")
            .replace(" ", "")
            .trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        return new BigDecimal(cleaned);
    }

    /**
     * Validates that a description is not empty.
     */
    protected void validateDescription(String description, String fileName, int lineNumber) {
        if (description == null || description.isBlank()) {
            throw new CsvParseException("Empty description not allowed", fileName, lineNumber);
        }
    }
}
