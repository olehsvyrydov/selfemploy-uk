package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * CSV parser for Metro Bank UK statements.
 *
 * <p>Metro Bank CSV format:
 * Date,Transaction type,Description,Money out,Money in,Balance
 *
 * <p>Uses separate Money out/Money in columns like Barclays and HSBC.</p>
 */
@ApplicationScoped
public class MetroBankCsvParser extends AbstractBankCsvParser {

    private static final String BANK_NAME = "Metro Bank";
    private static final String[] EXPECTED_HEADERS = {
        "Date", "Transaction type", "Description", "Money out", "Money in", "Balance"
    };

    private static final int COL_DATE = 0;
    private static final int COL_TYPE = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_MONEY_OUT = 3;
    private static final int COL_MONEY_IN = 4;
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

        String type = fields[COL_TYPE].trim();
        String description = fields[COL_DESCRIPTION].trim();
        String fullDescription = buildDescription(type, description);
        validateDescription(fullDescription, fileName, lineNumber);

        BigDecimal moneyOut = cleanAndParseAmount(fields[COL_MONEY_OUT].trim());
        BigDecimal moneyIn = cleanAndParseAmount(fields[COL_MONEY_IN].trim());

        BigDecimal amount;
        if (moneyOut != null && moneyOut.compareTo(BigDecimal.ZERO) > 0) {
            amount = moneyOut.negate();
        } else if (moneyIn != null && moneyIn.compareTo(BigDecimal.ZERO) > 0) {
            amount = moneyIn;
        } else {
            throw new CsvParseException("Both Money out and Money in are empty or zero", fileName, lineNumber);
        }

        BigDecimal balance = cleanAndParseAmount(fields[COL_BALANCE].trim());

        return new ImportedTransaction(date, amount, fullDescription, balance, null);
    }

    private String buildDescription(String type, String description) {
        if (!type.isEmpty() && !description.isEmpty()) {
            return type + " - " + description;
        }
        return description.isEmpty() ? type : description;
    }
}
