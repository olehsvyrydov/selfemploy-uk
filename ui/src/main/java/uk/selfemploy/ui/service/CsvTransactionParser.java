package uk.selfemploy.ui.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.ui.viewmodel.ColumnMapping;
import uk.selfemploy.ui.viewmodel.ImportedTransactionRow;
import uk.selfemploy.ui.viewmodel.TransactionType;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Parses CSV bank statements using a user-configured column mapping.
 *
 * <p>Unlike the core bank-specific parsers (which auto-detect format),
 * this parser uses the column mapping configured in the wizard UI to
 * extract transaction data from arbitrary CSV formats.</p>
 *
 * <p>Features:
 * <ul>
 *   <li>Column resolution by header name</li>
 *   <li>Configurable date format</li>
 *   <li>Single or separate income/expense amount columns</li>
 *   <li>Error-tolerant: skips malformed rows and collects warnings</li>
 *   <li>Handles quoted CSV fields with embedded commas and escaped quotes</li>
 * </ul>
 */
public class CsvTransactionParser {

    private static final Logger LOG = LoggerFactory.getLogger(CsvTransactionParser.class);

    /**
     * Result of a CSV parse operation containing parsed transactions and any warnings.
     *
     * @param transactions successfully parsed transaction rows
     * @param warnings human-readable warning messages for skipped rows
     */
    public record ParseResult(
        List<ImportedTransactionRow> transactions,
        List<String> warnings
    ) {
        public ParseResult {
            transactions = List.copyOf(transactions);
            warnings = List.copyOf(warnings);
        }
    }

    /**
     * Parses a CSV file using the provided column mapping.
     *
     * @param csvFile path to the CSV file
     * @param mapping column mapping configuration
     * @return parse result with transactions and warnings
     */
    public ParseResult parse(Path csvFile, ColumnMapping mapping) {
        return parse(csvFile, mapping, StandardCharsets.UTF_8);
    }

    /**
     * Parses a CSV file using the provided column mapping and charset.
     *
     * @param csvFile path to the CSV file
     * @param mapping column mapping configuration
     * @param charset character encoding of the file
     * @return parse result with transactions and warnings
     */
    public ParseResult parse(Path csvFile, ColumnMapping mapping, Charset charset) {
        List<ImportedTransactionRow> transactions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvFile, charset)) {
            // Read and parse header line
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                warnings.add("CSV file is empty or has no header row");
                return new ParseResult(transactions, warnings);
            }

            String[] headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);

            // Resolve column positions from header names
            ColumnIndices indices = resolveColumnIndices(mapping, headerIndex, warnings);
            if (indices == null) {
                return new ParseResult(transactions, warnings);
            }

            // Build date formatter from mapping
            DateTimeFormatter dateFormatter = buildDateFormatter(mapping.getDateFormat());

            // Parse data rows
            String line;
            int lineNumber = 1; // header was line 1
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {
                    ImportedTransactionRow row = parseRow(line, indices, dateFormatter, mapping);
                    if (row != null) {
                        transactions.add(row);
                    }
                } catch (Exception e) {
                    warnings.add(String.format("Skipped line %d: %s", lineNumber, e.getMessage()));
                    LOG.debug("Failed to parse line {}: {}", lineNumber, e.getMessage());
                }
            }

        } catch (IOException e) {
            warnings.add("Failed to read CSV file: " + e.getMessage());
            LOG.error("Failed to read CSV file: {}", csvFile, e);
        }

        return new ParseResult(transactions, warnings);
    }

    private Map<String, Integer> buildHeaderIndex(String[] headers) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].trim(), i);
        }
        return index;
    }

    private ColumnIndices resolveColumnIndices(
            ColumnMapping mapping, Map<String, Integer> headerIndex, List<String> warnings) {

        Integer dateIdx = findColumnIndex(mapping.getDateColumn(), headerIndex);
        Integer descIdx = findColumnIndex(mapping.getDescriptionColumn(), headerIndex);

        if (dateIdx == null) {
            warnings.add("Column not found in CSV headers: " + mapping.getDateColumn());
            return null;
        }
        if (descIdx == null) {
            warnings.add("Column not found in CSV headers: " + mapping.getDescriptionColumn());
            return null;
        }

        if (mapping.hasSeparateAmountColumns()) {
            Integer incomeIdx = findColumnIndex(mapping.getIncomeColumn(), headerIndex);
            Integer expenseIdx = findColumnIndex(mapping.getExpenseColumn(), headerIndex);

            if (incomeIdx == null) {
                warnings.add("Column not found in CSV headers: " + mapping.getIncomeColumn());
                return null;
            }
            if (expenseIdx == null) {
                warnings.add("Column not found in CSV headers: " + mapping.getExpenseColumn());
                return null;
            }

            return new ColumnIndices(dateIdx, descIdx, null, incomeIdx, expenseIdx, true);
        } else {
            Integer amountIdx = findColumnIndex(mapping.getAmountColumn(), headerIndex);

            if (amountIdx == null) {
                warnings.add("Column not found in CSV headers: " + mapping.getAmountColumn());
                return null;
            }

            return new ColumnIndices(dateIdx, descIdx, amountIdx, null, null, false);
        }
    }

    private Integer findColumnIndex(String columnName, Map<String, Integer> headerIndex) {
        if (columnName == null) {
            return null;
        }

        // Exact match first
        Integer idx = headerIndex.get(columnName.trim());
        if (idx != null) {
            return idx;
        }

        // Case-insensitive fallback
        for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(columnName.trim())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private DateTimeFormatter buildDateFormatter(String dateFormat) {
        if (dateFormat == null || dateFormat.isBlank()) {
            return DateTimeFormatter.ofPattern("dd/MM/yyyy");
        }

        // Handle datetime formats by extracting just the date portion
        String cleanFormat = dateFormat.trim();
        if (cleanFormat.contains("HH") || cleanFormat.contains("hh")) {
            cleanFormat = cleanFormat.replaceAll("\\s+HH.*", "").replaceAll("\\s+hh.*", "");
        }

        return DateTimeFormatter.ofPattern(cleanFormat, Locale.UK);
    }

    private ImportedTransactionRow parseRow(
            String line, ColumnIndices indices, DateTimeFormatter dateFormatter, ColumnMapping mapping) {

        String[] fields = parseCsvLine(line);

        // Extract and parse date
        String dateStr = getField(fields, indices.dateIdx).trim();
        LocalDate date = parseDate(dateStr, dateFormatter);

        // Extract description
        String description = getField(fields, indices.descIdx).trim();
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Empty description");
        }

        // Extract and parse amount, determine type
        BigDecimal amount;
        TransactionType type;

        if (indices.separateColumns) {
            String incomeStr = getField(fields, indices.incomeIdx).trim();
            String expenseStr = getField(fields, indices.expenseIdx).trim();

            BigDecimal incomeAmount = cleanAndParseAmount(incomeStr);
            BigDecimal expenseAmount = cleanAndParseAmount(expenseStr);

            if (incomeAmount != null && incomeAmount.compareTo(BigDecimal.ZERO) > 0) {
                amount = incomeAmount;
                type = TransactionType.INCOME;
            } else if (expenseAmount != null && expenseAmount.compareTo(BigDecimal.ZERO) > 0) {
                amount = expenseAmount;
                type = TransactionType.EXPENSE;
            } else {
                throw new IllegalArgumentException("No amount in income or expense column");
            }
        } else {
            String amountStr = getField(fields, indices.amountIdx).trim();
            BigDecimal rawAmount = cleanAndParseAmount(amountStr);

            if (rawAmount == null) {
                throw new IllegalArgumentException("Empty amount");
            }

            if (rawAmount.compareTo(BigDecimal.ZERO) > 0) {
                amount = rawAmount;
                type = TransactionType.INCOME;
            } else {
                amount = rawAmount.abs();
                type = TransactionType.EXPENSE;
            }
        }

        return ImportedTransactionRow.create(
            date,
            description,
            amount,
            type,
            null,   // category - uncategorized by default
            false,  // isDuplicate - not checked here
            0       // confidence - no auto-categorization yet
        );
    }

    private String getField(String[] fields, int index) {
        if (index < 0 || index >= fields.length) {
            return "";
        }
        return fields[index];
    }

    private LocalDate parseDate(String dateStr, DateTimeFormatter formatter) {
        if (dateStr.isEmpty()) {
            throw new IllegalArgumentException("Empty date");
        }

        try {
            return LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + dateStr, e);
        }
    }

    /**
     * Cleans and parses an amount string.
     * Handles currency symbols, thousand separators, and whitespace.
     *
     * @return parsed amount or null if the string is empty
     */
    private BigDecimal cleanAndParseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            return null;
        }

        String cleaned = amountStr
            .replace("GBP", "")
            .replace("\u00a3", "")
            .replace(",", "")
            .replace(" ", "")
            .trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount: " + amountStr, e);
        }
    }

    /**
     * Parses a CSV line into individual fields.
     * Handles quoted fields, embedded commas, and escaped quotes (doubled quotes).
     */
    public static String[] parseCsvLine(String line) {
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

        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    /**
     * Holds resolved column indices for efficient row parsing.
     */
    private record ColumnIndices(
        int dateIdx,
        int descIdx,
        Integer amountIdx,
        Integer incomeIdx,
        Integer expenseIdx,
        boolean separateColumns
    ) {}
}
