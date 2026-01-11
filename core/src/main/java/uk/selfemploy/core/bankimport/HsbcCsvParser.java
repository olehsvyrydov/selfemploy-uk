package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * CSV parser for HSBC bank statements.
 *
 * <p>HSBC CSV format:
 * Date,Type,Description,Paid Out,Paid In,Balance
 */
@ApplicationScoped
public class HsbcCsvParser extends AbstractBankCsvParser {

    private static final String BANK_NAME = "HSBC";
    private static final String[] EXPECTED_HEADERS = {"Date", "Type", "Description", "Paid Out", "Paid In", "Balance"};

    private static final int COL_DATE = 0;
    private static final int COL_TYPE = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_PAID_OUT = 3;
    private static final int COL_PAID_IN = 4;
    private static final int COL_BALANCE = 5;

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

        if (fields.length < 6) {
            throw new CsvParseException("Invalid number of columns", fileName, lineNumber);
        }

        LocalDate date = parseDate(fields[COL_DATE].trim(), fileName, lineNumber,
            DATE_FORMAT_SLASH, DATE_FORMAT_DASH_SHORT, DATE_FORMAT_ISO);

        // Combine type and description for more context
        String type = fields[COL_TYPE].trim();
        String description = fields[COL_DESCRIPTION].trim();
        String fullDescription = type.isEmpty() ? description : type + " - " + description;

        validateDescription(fullDescription, fileName, lineNumber);

        BigDecimal amount = parseAmount(
            fields[COL_PAID_OUT].trim(),
            fields[COL_PAID_IN].trim(),
            fileName,
            lineNumber
        );

        BigDecimal balance = cleanAndParseAmount(fields[COL_BALANCE].trim());

        return new ImportedTransaction(date, amount, fullDescription, balance, null);
    }

    private BigDecimal parseAmount(String paidOut, String paidIn, String fileName, int lineNumber) {
        BigDecimal out = cleanAndParseAmount(paidOut);
        BigDecimal in = cleanAndParseAmount(paidIn);

        if (out == null && in == null) {
            throw new CsvParseException("No amount specified (both Paid Out and Paid In are empty)", fileName, lineNumber);
        }

        if (out != null) {
            return out.negate(); // Expense is negative
        }
        return in; // Income is positive
    }
}
