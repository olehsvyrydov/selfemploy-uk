package uk.selfemploy.ui.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.plugin.extension.BankStatementParser;
import uk.selfemploy.plugin.extension.ImportContext;
import uk.selfemploy.plugin.extension.ImportResult;
import uk.selfemploy.plugin.extension.ParsedTransaction;
import uk.selfemploy.plugin.extension.StatementParseRequest;
import uk.selfemploy.plugin.extension.StatementParseResult;
import uk.selfemploy.ui.viewmodel.BankFormat;
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
import java.util.stream.Collectors;

/**
 * Parses CSV bank statements using a user-configured column mapping.
 *
 * <p>Implements {@link BankStatementParser} SPI to participate in the plugin
 * architecture. The built-in CSV parser has priority 10 (core range) and
 * requires column mapping since CSV formats vary by bank.</p>
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
public class CsvTransactionParser implements BankStatementParser {

    private static final Logger LOG = LoggerFactory.getLogger(CsvTransactionParser.class);

    /** Priority in the built-in range for the core CSV parser. */
    private static final int CSV_PARSER_PRIORITY = 10;

    /** The file path currently being parsed via SPI methods. Thread-local to support concurrent parsing. */
    private final ThreadLocal<Path> currentFile = new ThreadLocal<>();

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

    // ========================================================================
    // BankStatementParser SPI implementation
    // ========================================================================

    @Override
    public String getFormatId() {
        return "csv";
    }

    @Override
    public Set<String> getSupportedBankFormats() {
        return Arrays.stream(BankFormat.values())
            .filter(f -> f != BankFormat.UNKNOWN)
            .map(BankFormat::toFormatId)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public StatementParseResult parseStatement(StatementParseRequest request) {
        Path file = currentFile.get();
        if (file == null) {
            file = request.getOption(StatementParseRequest.OPT_FILE_PATH, null);
        }
        if (file == null) {
            return StatementParseResult.failure("No file path provided for CSV parsing");
        }

        try {
            ColumnMapping mapping = buildColumnMappingFromRequest(request);
            ParseResult result = parse(file, mapping);

            List<ParsedTransaction> parsedTransactions = result.transactions().stream()
                .map(this::toParsedTransaction)
                .collect(Collectors.toList());

            return new StatementParseResult(parsedTransactions, result.warnings(), null, "csv");
        } catch (Exception e) {
            LOG.error("Failed to parse CSV via SPI: {}", e.getMessage(), e);
            return StatementParseResult.failure("CSV parse error: " + e.getMessage());
        }
    }

    @Override
    public Optional<String> detectFormat(Path file) {
        if (file == null) {
            return Optional.empty();
        }
        String fileName = file.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".csv")) {
            return Optional.of("csv");
        }
        return Optional.empty();
    }

    @Override
    public List<ParsedTransaction> parsePreview(Path file, int maxRows) {
        try {
            currentFile.set(file);
            StatementParseResult result = parseStatement(StatementParseRequest.autoDetect());
            List<ParsedTransaction> transactions = result.transactions();
            if (transactions.size() > maxRows) {
                return List.copyOf(transactions.subList(0, maxRows));
            }
            return transactions;
        } finally {
            currentFile.remove();
        }
    }

    @Override
    public boolean requiresColumnMapping() {
        return true;
    }

    @Override
    public int getPriority() {
        return CSV_PARSER_PRIORITY;
    }

    @Override
    public String getImporterId() {
        return "csv-generic";
    }

    @Override
    public String getImporterName() {
        return "CSV Bank Statement";
    }

    @Override
    public List<String> getSupportedFileTypes() {
        return List.of(".csv");
    }

    @Override
    public ImportResult importData(Path file, ImportContext context) {
        try {
            currentFile.set(file);
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
        } finally {
            currentFile.remove();
        }
    }

    // ========================================================================
    // Original parse methods (unchanged API)
    // ========================================================================

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

    // ========================================================================
    // SPI bridge helpers
    // ========================================================================

    /**
     * Converts an ImportedTransactionRow (UI view model) to a ParsedTransaction (SPI type).
     * Amount follows the SPI convention: positive=income, negative=expense.
     */
    private ParsedTransaction toParsedTransaction(ImportedTransactionRow row) {
        BigDecimal signedAmount = row.type() == TransactionType.EXPENSE
            ? row.amount().negate()
            : row.amount();

        return new ParsedTransaction(
            row.date(),
            row.description(),
            signedAmount,
            null,  // reference - not available from CSV
            null,  // category - not set at parse time
            null   // accountInfo - not available from CSV
        );
    }

    /**
     * Builds a ColumnMapping from a StatementParseRequest.
     * This bridges SPI requests to the existing parse() method.
     */
    private ColumnMapping buildColumnMappingFromRequest(StatementParseRequest request) {
        ColumnMapping mapping = new ColumnMapping();

        if (request.dateColumn() != null) {
            mapping.setDateColumn(request.dateColumn());
        }
        if (request.descriptionColumn() != null) {
            mapping.setDescriptionColumn(request.descriptionColumn());
        }
        if (request.dateFormat() != null) {
            mapping.setDateFormat(request.dateFormat());
        }

        boolean separateColumns = request.getOption(
            StatementParseRequest.OPT_SEPARATE_COLUMNS, false
        );
        mapping.setSeparateAmountColumns(separateColumns);

        if (separateColumns) {
            String incomeCol = request.getOption(StatementParseRequest.OPT_INCOME_COLUMN, null);
            String expenseCol = request.getOption(StatementParseRequest.OPT_EXPENSE_COLUMN, null);
            if (incomeCol != null) mapping.setIncomeColumn(incomeCol);
            if (expenseCol != null) mapping.setExpenseColumn(expenseCol);
        } else {
            if (request.amountColumn() != null) {
                mapping.setAmountColumn(request.amountColumn());
            }
        }

        return mapping;
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
