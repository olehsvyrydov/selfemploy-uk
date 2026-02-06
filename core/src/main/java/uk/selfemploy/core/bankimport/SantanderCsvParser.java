package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * CSV parser for Santander UK bank statements.
 *
 * <p>Santander CSV format:
 * Date,Description,Amount,Balance
 *
 * <p>Uses a single Amount column (positive for income, negative for expenses).</p>
 */
@ApplicationScoped
public class SantanderCsvParser extends AbstractBankCsvParser {

    private static final String BANK_NAME = "Santander";
    private static final String[] EXPECTED_HEADERS = {"Date", "Description", "Amount", "Balance"};

    private static final int COL_DATE = 0;
    private static final int COL_DESCRIPTION = 1;
    private static final int COL_AMOUNT = 2;
    private static final int COL_BALANCE = 3;

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

        if (fields.length < 4) {
            throw new CsvParseException("Invalid number of columns", fileName, lineNumber);
        }

        LocalDate date = parseDate(fields[COL_DATE].trim(), fileName, lineNumber,
            DATE_FORMAT_SLASH, DATE_FORMAT_DASH_SHORT, DATE_FORMAT_ISO);

        String description = fields[COL_DESCRIPTION].trim();
        validateDescription(description, fileName, lineNumber);

        BigDecimal amount = cleanAndParseAmount(fields[COL_AMOUNT].trim());
        if (amount == null) {
            throw new CsvParseException("Amount cannot be empty", fileName, lineNumber);
        }

        BigDecimal balance = cleanAndParseAmount(fields[COL_BALANCE].trim());

        return new ImportedTransaction(date, amount, description, balance, null);
    }
}
