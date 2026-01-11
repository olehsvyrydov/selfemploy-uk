package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

/**
 * CSV parser for Monzo bank statements.
 *
 * <p>Monzo CSV format:
 * Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Local amount,Local currency,Notes and #tags,Address,Receipt,Description,Category split
 *
 * <p>Simplified format (common export):
 * Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
 *
 * <p>Note: Monzo uses a single Amount column with positive for income
 * and negative for expenses.</p>
 */
@ApplicationScoped
public class MonzoCsvParser extends AbstractBankCsvParser {

    private static final String BANK_NAME = "Monzo";

    // Multiple possible header formats
    private static final String[] EXPECTED_HEADERS_SIMPLE = {
        "Transaction ID", "Date", "Time", "Type", "Name", "Emoji", "Category", "Amount", "Currency", "Notes", "Address", "Description"
    };
    private static final String[] EXPECTED_HEADERS_FULL = {
        "Transaction ID", "Date", "Time", "Type", "Name", "Emoji", "Category", "Amount", "Currency", "Local amount", "Local currency", "Notes and #tags", "Address", "Receipt", "Description", "Category split"
    };

    private static final int COL_TRANSACTION_ID = 0;
    private static final int COL_DATE = 1;
    private static final int COL_TIME = 2;
    private static final int COL_TYPE = 3;
    private static final int COL_NAME = 4;
    private static final int COL_EMOJI = 5;
    private static final int COL_CATEGORY = 6;
    private static final int COL_AMOUNT = 7;

    @Override
    public String getBankName() {
        return BANK_NAME;
    }

    @Override
    public String[] getExpectedHeaders() {
        return Arrays.copyOf(EXPECTED_HEADERS_SIMPLE, EXPECTED_HEADERS_SIMPLE.length);
    }

    @Override
    public boolean canParse(String[] headers) {
        // Check if first 8 headers match (core fields)
        if (headers.length < 8) {
            return false;
        }

        // Check core headers
        String[] coreExpected = {"Transaction ID", "Date", "Time", "Type", "Name", "Emoji", "Category", "Amount"};
        for (int i = 0; i < coreExpected.length; i++) {
            if (!coreExpected[i].equalsIgnoreCase(headers[i].trim())) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected ImportedTransaction parseLine(String line, String fileName, int lineNumber) {
        String[] fields = parseCsvLine(line);

        if (fields.length < 8) {
            throw new CsvParseException("Invalid number of columns", fileName, lineNumber);
        }

        String transactionId = fields[COL_TRANSACTION_ID].trim();
        LocalDate date = parseMonzoDate(fields[COL_DATE].trim(), fileName, lineNumber);

        String type = fields[COL_TYPE].trim();
        String name = fields[COL_NAME].trim();

        // Build description from name and type
        String description = buildDescription(name, type);
        validateDescription(description, fileName, lineNumber);

        BigDecimal amount = cleanAndParseAmount(fields[COL_AMOUNT].trim());
        if (amount == null) {
            throw new CsvParseException("Amount cannot be empty", fileName, lineNumber);
        }

        // Monzo doesn't provide running balance in standard exports
        return new ImportedTransaction(date, amount, description, null, transactionId);
    }

    private LocalDate parseMonzoDate(String dateStr, String fileName, int lineNumber) {
        if (dateStr.isBlank()) {
            throw new CsvParseException("Empty date not allowed", fileName, lineNumber);
        }

        // Monzo uses dd/MM/yyyy format
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMAT_SLASH);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(dateStr.trim(), DATE_FORMAT_ISO);
            } catch (DateTimeParseException e2) {
                throw new CsvParseException("Invalid date format: " + dateStr, fileName, lineNumber);
            }
        }
    }

    private String buildDescription(String name, String type) {
        if (!name.isEmpty()) {
            return name;
        }
        if (!type.isEmpty()) {
            return type;
        }
        return "No description";
    }
}
