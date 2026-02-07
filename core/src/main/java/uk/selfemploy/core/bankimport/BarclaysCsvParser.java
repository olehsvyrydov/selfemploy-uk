package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;

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
 * CSV parser for Barclays bank statements.
 *
 * <p>Barclays CSV format:
 * Date,Description,Money Out,Money In,Balance
 *
 * <p>Date formats supported:
 * <ul>
 *   <li>dd/MM/yyyy (e.g., 15/06/2025)</li>
 *   <li>dd-MMM-yyyy (e.g., 15-Jun-2025)</li>
 * </ul>
 */
@ApplicationScoped
public class BarclaysCsvParser implements BankCsvParser {

    private static final String BANK_NAME = "Barclays";
    private static final String[] EXPECTED_HEADERS = {"Date", "Description", "Money Out", "Money In", "Balance"};

    private static final DateTimeFormatter DATE_FORMAT_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMAT_DASH = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.UK);

    private static final int COL_DATE = 0;
    private static final int COL_DESCRIPTION = 1;
    private static final int COL_MONEY_OUT = 2;
    private static final int COL_MONEY_IN = 3;
    private static final int COL_BALANCE = 4;

    @Override
    public String getBankName() {
        return BANK_NAME;
    }

    @Override
    public boolean canParse(String[] headers) {
        if (headers.length != EXPECTED_HEADERS.length) {
            return false;
        }

        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            if (!EXPECTED_HEADERS[i].equalsIgnoreCase(headers[i].trim())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String[] getExpectedHeaders() {
        return Arrays.copyOf(EXPECTED_HEADERS, EXPECTED_HEADERS.length);
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
                transactions.add(transaction);
            }
        } catch (IOException e) {
            throw new CsvParseException("Failed to read CSV file", fileName, lineNumber, e);
        }

        return transactions;
    }

    private ImportedTransaction parseLine(String line, String fileName, int lineNumber) {
        String[] fields = parseCsvLine(line);

        if (fields.length < 5) {
            throw new CsvParseException("Invalid number of columns", fileName, lineNumber);
        }

        LocalDate date = parseDate(fields[COL_DATE].trim(), fileName, lineNumber);
        String description = fields[COL_DESCRIPTION].trim();

        if (description.isBlank()) {
            throw new CsvParseException("Empty description not allowed", fileName, lineNumber);
        }

        BigDecimal amount = parseAmount(
            fields[COL_MONEY_OUT].trim(),
            fields[COL_MONEY_IN].trim(),
            fileName,
            lineNumber
        );

        BigDecimal balance = parseBalance(fields[COL_BALANCE].trim(), fileName, lineNumber);

        return new ImportedTransaction(date, amount, description, balance, null);
    }

    private String[] parseCsvLine(String line) {
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

    private LocalDate parseDate(String dateStr, String fileName, int lineNumber) {
        if (dateStr.isBlank()) {
            throw new CsvParseException("Empty date not allowed", fileName, lineNumber);
        }

        try {
            // Try dd/MM/yyyy format first
            return LocalDate.parse(dateStr, DATE_FORMAT_SLASH);
        } catch (DateTimeParseException e1) {
            try {
                // Try dd-MMM-yyyy format
                return LocalDate.parse(dateStr, DATE_FORMAT_DASH);
            } catch (DateTimeParseException e2) {
                throw new CsvParseException("Invalid date format: " + dateStr, fileName, lineNumber);
            }
        }
    }

    private BigDecimal parseAmount(String moneyOut, String moneyIn, String fileName, int lineNumber) {
        // Money Out = expense (negative), Money In = income (positive)
        boolean hasMoneyOut = !moneyOut.isBlank();
        boolean hasMoneyIn = !moneyIn.isBlank();

        if (!hasMoneyOut && !hasMoneyIn) {
            throw new CsvParseException("No amount specified (both Money Out and Money In are empty)", fileName, lineNumber);
        }

        try {
            if (hasMoneyOut) {
                BigDecimal value = cleanAndParseAmount(moneyOut);
                return value.negate(); // Expense is negative
            } else {
                return cleanAndParseAmount(moneyIn); // Income is positive
            }
        } catch (NumberFormatException e) {
            throw new CsvParseException("Invalid amount format", fileName, lineNumber, e);
        }
    }

    private BigDecimal parseBalance(String balanceStr, String fileName, int lineNumber) {
        if (balanceStr.isBlank()) {
            return null;
        }

        try {
            return cleanAndParseAmount(balanceStr);
        } catch (NumberFormatException e) {
            throw new CsvParseException("Invalid balance format: " + balanceStr, fileName, lineNumber, e);
        }
    }

    /**
     * Cleans and parses an amount string.
     *
     * <p>Handles:
     * <ul>
     *   <li>Currency symbols (GBP, GBP)</li>
     *   <li>Thousand separators (1,234.56)</li>
     *   <li>Leading/trailing whitespace</li>
     * </ul>
     */
    private BigDecimal cleanAndParseAmount(String amountStr) {
        String cleaned = amountStr
            .replace("GBP", "")
            .replace("£", "")
            .replace(",", "")
            .trim();

        return new BigDecimal(cleaned);
    }
}
