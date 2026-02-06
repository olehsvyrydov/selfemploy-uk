package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * CSV parser for Revolut bank statements.
 *
 * <p>Revolut CSV format:
 * Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
 *
 * <p>Uses a single Amount column (positive for income, negative for expenses).
 * Only transactions with State "COMPLETED" are imported.</p>
 */
@ApplicationScoped
public class RevolutCsvParser extends AbstractBankCsvParser {

    private static final String BANK_NAME = "Revolut";
    private static final String[] EXPECTED_HEADERS = {
        "Type", "Product", "Started Date", "Completed Date", "Description",
        "Amount", "Fee", "Currency", "State", "Balance"
    };

    private static final int COL_TYPE = 0;
    private static final int COL_COMPLETED_DATE = 3;
    private static final int COL_DESCRIPTION = 4;
    private static final int COL_AMOUNT = 5;
    private static final int COL_CURRENCY = 7;
    private static final int COL_STATE = 8;
    private static final int COL_BALANCE = 9;

    private static final DateTimeFormatter REVOLUT_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter REVOLUT_DATE_ONLY =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String getBankName() {
        return BANK_NAME;
    }

    @Override
    public String[] getExpectedHeaders() {
        return Arrays.copyOf(EXPECTED_HEADERS, EXPECTED_HEADERS.length);
    }

    @Override
    protected ImportedTransaction parseLine(String line, String fileName, int lineNumber) {
        String[] fields = parseCsvLine(line);

        if (fields.length < 10) {
            throw new CsvParseException("Invalid number of columns", fileName, lineNumber);
        }

        // Skip non-completed transactions
        String state = fields[COL_STATE].trim();
        if (!"COMPLETED".equalsIgnoreCase(state)) {
            return null;
        }

        // Skip non-GBP transactions
        String currency = fields[COL_CURRENCY].trim();
        if (!currency.isEmpty() && !"GBP".equalsIgnoreCase(currency)) {
            return null;
        }

        // Parse completed date (may include time component)
        String dateStr = fields[COL_COMPLETED_DATE].trim();
        if (dateStr.isEmpty()) {
            dateStr = fields[2].trim(); // Fall back to Started Date
        }
        LocalDate date = parseRevolutDate(dateStr, fileName, lineNumber);

        String description = fields[COL_DESCRIPTION].trim();
        String type = fields[COL_TYPE].trim();
        if (description.isEmpty()) {
            description = type;
        }
        validateDescription(description, fileName, lineNumber);

        BigDecimal amount = cleanAndParseAmount(fields[COL_AMOUNT].trim());
        if (amount == null) {
            throw new CsvParseException("Amount cannot be empty", fileName, lineNumber);
        }

        BigDecimal balance = cleanAndParseAmount(fields[COL_BALANCE].trim());

        return new ImportedTransaction(date, amount, description, balance, null);
    }

    private LocalDate parseRevolutDate(String dateStr, String fileName, int lineNumber) {
        if (dateStr.isBlank()) {
            throw new CsvParseException("Empty date not allowed", fileName, lineNumber);
        }

        // Strip time component if present (e.g., "2025-06-15 10:30:00")
        String dateOnly = dateStr.contains(" ") ? dateStr.split(" ")[0] : dateStr;
        return parseDate(dateOnly, fileName, lineNumber,
            REVOLUT_DATE_ONLY, DATE_FORMAT_ISO, DATE_FORMAT_SLASH);
    }
}
