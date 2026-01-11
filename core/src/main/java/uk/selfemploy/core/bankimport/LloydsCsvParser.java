package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * CSV parser for Lloyds bank statements.
 *
 * <p>Lloyds CSV format:
 * Transaction Date,Transaction Type,Sort Code,Account Number,Transaction Description,Debit Amount,Credit Amount,Balance
 *
 * <p>Simplified format (also supported):
 * Transaction Date,Transaction Type,Description,Debit,Credit,Balance
 */
@ApplicationScoped
public class LloydsCsvParser extends AbstractBankCsvParser {

    private static final String BANK_NAME = "Lloyds";
    private static final String[] EXPECTED_HEADERS = {"Transaction Date", "Transaction Type", "Description", "Debit", "Credit", "Balance"};
    private static final String[] EXPECTED_HEADERS_FULL = {"Transaction Date", "Transaction Type", "Sort Code", "Account Number", "Transaction Description", "Debit Amount", "Credit Amount", "Balance"};

    @Override
    public String getBankName() {
        return BANK_NAME;
    }

    @Override
    public String[] getExpectedHeaders() {
        return Arrays.copyOf(EXPECTED_HEADERS, EXPECTED_HEADERS.length);
    }

    @Override
    public boolean canParse(String[] headers) {
        // Check simplified format
        if (matchesHeaders(headers, EXPECTED_HEADERS)) {
            return true;
        }
        // Check full format
        return matchesHeaders(headers, EXPECTED_HEADERS_FULL);
    }

    private boolean matchesHeaders(String[] actual, String[] expected) {
        if (actual.length != expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equalsIgnoreCase(actual[i].trim())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected ImportedTransaction parseLine(String line, String fileName, int lineNumber) {
        String[] fields = parseCsvLine(line);

        // Determine which format we're parsing
        boolean isFullFormat = fields.length >= 8;

        int dateCol = 0;
        int typeCol = 1;
        int descCol = isFullFormat ? 4 : 2;
        int debitCol = isFullFormat ? 5 : 3;
        int creditCol = isFullFormat ? 6 : 4;
        int balanceCol = isFullFormat ? 7 : 5;

        if (fields.length < (isFullFormat ? 8 : 6)) {
            throw new CsvParseException("Invalid number of columns", fileName, lineNumber);
        }

        LocalDate date = parseDate(fields[dateCol].trim(), fileName, lineNumber,
            DATE_FORMAT_SLASH, DATE_FORMAT_DASH_SHORT, DATE_FORMAT_ISO);

        String type = fields[typeCol].trim();
        String description = fields[descCol].trim();
        String fullDescription = type.isEmpty() ? description : type + " - " + description;

        validateDescription(fullDescription, fileName, lineNumber);

        BigDecimal amount = parseAmount(
            fields[debitCol].trim(),
            fields[creditCol].trim(),
            fileName,
            lineNumber
        );

        BigDecimal balance = cleanAndParseAmount(fields[balanceCol].trim());

        return new ImportedTransaction(date, amount, fullDescription, balance, null);
    }

    private BigDecimal parseAmount(String debit, String credit, String fileName, int lineNumber) {
        BigDecimal debitAmount = cleanAndParseAmount(debit);
        BigDecimal creditAmount = cleanAndParseAmount(credit);

        if (debitAmount == null && creditAmount == null) {
            throw new CsvParseException("No amount specified (both Debit and Credit are empty)", fileName, lineNumber);
        }

        if (debitAmount != null) {
            return debitAmount.negate(); // Expense is negative
        }
        return creditAmount; // Income is positive
    }
}
