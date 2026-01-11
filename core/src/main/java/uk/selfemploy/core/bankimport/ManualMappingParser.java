package uk.selfemploy.core.bankimport;

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
 * CSV parser that uses manual column mapping provided by the user.
 *
 * <p>This parser is used when the bank format is not auto-detected.
 * The user provides column indices for date, description, and amount fields.</p>
 */
public class ManualMappingParser implements BankCsvParser {

    private static final String BANK_NAME = "Manual Mapping";

    private static final DateTimeFormatter[] DATE_FORMATS = {
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.UK),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.UK),
        DateTimeFormatter.ISO_LOCAL_DATE
    };

    private final ColumnMapping mapping;

    /**
     * Creates a manual mapping parser with the specified column configuration.
     *
     * @param mapping the column mapping configuration
     */
    public ManualMappingParser(ColumnMapping mapping) {
        this.mapping = mapping;
    }

    @Override
    public String getBankName() {
        return mapping.bankName() != null ? mapping.bankName() : BANK_NAME;
    }

    @Override
    public boolean canParse(String[] headers) {
        // Manual mapping doesn't auto-detect; it must be explicitly configured
        return false;
    }

    @Override
    public String[] getExpectedHeaders() {
        return new String[0]; // No expected headers for manual mapping
    }

    @Override
    public List<ImportedTransaction> parse(Path csvFile, Charset charset) throws CsvParseException {
        List<ImportedTransaction> transactions = new ArrayList<>();
        String fileName = csvFile.getFileName().toString();
        int lineNumber = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvFile, charset)) {
            // Skip header line if configured
            if (mapping.hasHeaderRow()) {
                reader.readLine();
                lineNumber++;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                ImportedTransaction transaction = parseLine(line, fileName, lineNumber);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }
        } catch (IOException e) {
            throw new CsvParseException("Failed to read CSV file", fileName, lineNumber, e);
        }

        return transactions;
    }

    private ImportedTransaction parseLine(String line, String fileName, int lineNumber) {
        String[] fields = parseCsvLine(line);

        // Validate column indices
        int maxIndex = Math.max(mapping.dateColumn(),
            Math.max(mapping.descriptionColumn(),
                Math.max(mapping.amountColumn(),
                    Math.max(
                        mapping.creditColumn() >= 0 ? mapping.creditColumn() : -1,
                        Math.max(
                            mapping.debitColumn() >= 0 ? mapping.debitColumn() : -1,
                            mapping.balanceColumn() >= 0 ? mapping.balanceColumn() : -1
                        )
                    )
                )
            )
        );

        if (fields.length <= maxIndex) {
            throw new CsvParseException("Invalid number of columns (expected at least " + (maxIndex + 1) + ")", fileName, lineNumber);
        }

        LocalDate date = parseDate(fields[mapping.dateColumn()].trim(), fileName, lineNumber);

        String description = fields[mapping.descriptionColumn()].trim();
        if (description.isBlank()) {
            throw new CsvParseException("Empty description not allowed", fileName, lineNumber);
        }

        BigDecimal amount = parseAmount(fields, fileName, lineNumber);

        BigDecimal balance = null;
        if (mapping.balanceColumn() >= 0 && mapping.balanceColumn() < fields.length) {
            balance = cleanAndParseAmount(fields[mapping.balanceColumn()].trim());
        }

        String reference = null;
        if (mapping.referenceColumn() >= 0 && mapping.referenceColumn() < fields.length) {
            reference = fields[mapping.referenceColumn()].trim();
            if (reference.isBlank()) {
                reference = null;
            }
        }

        return new ImportedTransaction(date, amount, description, balance, reference);
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
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

    private LocalDate parseDate(String dateStr, String fileName, int lineNumber) {
        if (dateStr.isBlank()) {
            throw new CsvParseException("Empty date not allowed", fileName, lineNumber);
        }

        // Try custom format first if specified
        if (mapping.dateFormat() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mapping.dateFormat(), Locale.UK);
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Fall through to try other formats
            }
        }

        // Try standard formats
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, format);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        throw new CsvParseException("Invalid date format: " + dateStr, fileName, lineNumber);
    }

    private BigDecimal parseAmount(String[] fields, String fileName, int lineNumber) {
        if (mapping.usesSeparateColumns()) {
            // Separate debit/credit columns
            BigDecimal debit = null;
            BigDecimal credit = null;

            if (mapping.debitColumn() >= 0 && mapping.debitColumn() < fields.length) {
                debit = cleanAndParseAmount(fields[mapping.debitColumn()].trim());
            }
            if (mapping.creditColumn() >= 0 && mapping.creditColumn() < fields.length) {
                credit = cleanAndParseAmount(fields[mapping.creditColumn()].trim());
            }

            if (debit == null && credit == null) {
                throw new CsvParseException("No amount specified (both debit and credit are empty)", fileName, lineNumber);
            }

            if (debit != null) {
                return mapping.debitIsNegative() ? debit.negate() : debit;
            }
            return mapping.creditIsPositive() ? credit : credit.negate();
        } else {
            // Single amount column
            BigDecimal amount = cleanAndParseAmount(fields[mapping.amountColumn()].trim());
            if (amount == null) {
                throw new CsvParseException("Amount cannot be empty", fileName, lineNumber);
            }
            return amount;
        }
    }

    private BigDecimal cleanAndParseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            return null;
        }

        String cleaned = amountStr
            .replace("GBP", "")
            .replace("£", "")
            .replace(",", "")
            .replace(" ", "")
            .trim();

        if (cleaned.isEmpty()) {
            return null;
        }

        return new BigDecimal(cleaned);
    }

    /**
     * Configuration for manual column mapping.
     */
    public record ColumnMapping(
        String bankName,
        boolean hasHeaderRow,
        int dateColumn,
        String dateFormat,
        int descriptionColumn,
        int amountColumn,
        int debitColumn,
        int creditColumn,
        boolean debitIsNegative,
        boolean creditIsPositive,
        int balanceColumn,
        int referenceColumn
    ) {
        public boolean usesSeparateColumns() {
            return debitColumn >= 0 || creditColumn >= 0;
        }

        /**
         * Builder for creating column mapping configurations.
         */
        public static class Builder {
            private String bankName = "Manual Import";
            private boolean hasHeaderRow = true;
            private int dateColumn = 0;
            private String dateFormat = null;
            private int descriptionColumn = 1;
            private int amountColumn = 2;
            private int debitColumn = -1;
            private int creditColumn = -1;
            private boolean debitIsNegative = true;
            private boolean creditIsPositive = true;
            private int balanceColumn = -1;
            private int referenceColumn = -1;

            public Builder bankName(String bankName) {
                this.bankName = bankName;
                return this;
            }

            public Builder hasHeaderRow(boolean hasHeaderRow) {
                this.hasHeaderRow = hasHeaderRow;
                return this;
            }

            public Builder dateColumn(int dateColumn) {
                this.dateColumn = dateColumn;
                return this;
            }

            public Builder dateFormat(String dateFormat) {
                this.dateFormat = dateFormat;
                return this;
            }

            public Builder descriptionColumn(int descriptionColumn) {
                this.descriptionColumn = descriptionColumn;
                return this;
            }

            public Builder amountColumn(int amountColumn) {
                this.amountColumn = amountColumn;
                return this;
            }

            public Builder debitColumn(int debitColumn) {
                this.debitColumn = debitColumn;
                return this;
            }

            public Builder creditColumn(int creditColumn) {
                this.creditColumn = creditColumn;
                return this;
            }

            public Builder debitIsNegative(boolean debitIsNegative) {
                this.debitIsNegative = debitIsNegative;
                return this;
            }

            public Builder creditIsPositive(boolean creditIsPositive) {
                this.creditIsPositive = creditIsPositive;
                return this;
            }

            public Builder balanceColumn(int balanceColumn) {
                this.balanceColumn = balanceColumn;
                return this;
            }

            public Builder referenceColumn(int referenceColumn) {
                this.referenceColumn = referenceColumn;
                return this;
            }

            public ColumnMapping build() {
                return new ColumnMapping(
                    bankName, hasHeaderRow, dateColumn, dateFormat, descriptionColumn,
                    amountColumn, debitColumn, creditColumn, debitIsNegative, creditIsPositive,
                    balanceColumn, referenceColumn
                );
            }
        }
    }
}
