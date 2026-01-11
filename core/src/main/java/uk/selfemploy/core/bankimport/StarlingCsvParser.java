package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * CSV parser for Starling bank statements.
 *
 * <p>Starling CSV format:
 * Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
 *
 * <p>Note: Starling uses a single Amount column with positive for income
 * and negative for expenses.</p>
 */
@ApplicationScoped
public class StarlingCsvParser extends AbstractBankCsvParser {

    private static final String BANK_NAME = "Starling";
    private static final String[] EXPECTED_HEADERS = {"Date", "Counter Party", "Reference", "Type", "Amount (GBP)", "Balance (GBP)"};

    private static final int COL_DATE = 0;
    private static final int COL_COUNTER_PARTY = 1;
    private static final int COL_REFERENCE = 2;
    private static final int COL_TYPE = 3;
    private static final int COL_AMOUNT = 4;
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
            DATE_FORMAT_SLASH, DATE_FORMAT_ISO, DATE_FORMAT_DASH_SHORT);

        String counterParty = fields[COL_COUNTER_PARTY].trim();
        String reference = fields[COL_REFERENCE].trim();
        String type = fields[COL_TYPE].trim();

        // Build description from available fields
        String description = buildDescription(counterParty, reference, type);
        validateDescription(description, fileName, lineNumber);

        BigDecimal amount = cleanAndParseAmount(fields[COL_AMOUNT].trim());
        if (amount == null) {
            throw new CsvParseException("Amount cannot be empty", fileName, lineNumber);
        }

        BigDecimal balance = cleanAndParseAmount(fields[COL_BALANCE].trim());

        return new ImportedTransaction(date, amount, description, balance, reference.isEmpty() ? null : reference);
    }

    private String buildDescription(String counterParty, String reference, String type) {
        StringBuilder desc = new StringBuilder();

        if (!counterParty.isEmpty()) {
            desc.append(counterParty);
        }

        if (!reference.isEmpty() && !reference.equals(counterParty)) {
            if (desc.length() > 0) {
                desc.append(" - ");
            }
            desc.append(reference);
        }

        if (!type.isEmpty() && desc.length() == 0) {
            desc.append(type);
        }

        return desc.length() > 0 ? desc.toString() : "No description";
    }
}
