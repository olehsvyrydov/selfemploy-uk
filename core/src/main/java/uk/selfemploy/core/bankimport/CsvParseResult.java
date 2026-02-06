package uk.selfemploy.core.bankimport;

import java.util.List;

/**
 * Result of an error-tolerant CSV parse operation.
 *
 * <p>Contains both successfully parsed transactions and any errors
 * encountered during parsing. This allows the UI to show parsed
 * transactions while warning the user about problematic rows.</p>
 *
 * @param transactions successfully parsed transactions
 * @param errors parse errors for malformed rows
 */
public record CsvParseResult(
    List<ImportedTransaction> transactions,
    List<CsvParseError> errors
) {
    public CsvParseResult {
        transactions = List.copyOf(transactions);
        errors = List.copyOf(errors);
    }

    /**
     * Returns the total number of data rows processed (successes + errors).
     */
    public int totalRowsProcessed() {
        return transactions.size() + errors.size();
    }

    /**
     * Returns the number of successfully parsed transactions.
     */
    public int successCount() {
        return transactions.size();
    }

    /**
     * Returns the number of rows that failed to parse.
     */
    public int errorCount() {
        return errors.size();
    }

    /**
     * Returns true if any rows failed to parse.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
