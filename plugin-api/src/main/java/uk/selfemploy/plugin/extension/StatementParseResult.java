package uk.selfemploy.plugin.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of parsing a bank statement.
 *
 * <p>Contains the parsed transactions, any non-fatal warnings (e.g., skipped rows),
 * and any fatal errors. The detected format ID indicates which format was used
 * for parsing.</p>
 *
 * <p>All collection fields are defensively copied and unmodifiable after construction.</p>
 *
 * @param transactions     parsed transactions, never null (may be empty)
 * @param warnings         non-fatal warnings such as skipped rows, never null
 * @param errors           fatal errors preventing successful parse, never null
 * @param detectedFormatId the format ID used or detected during parsing, may be null
 *
 * @see BankStatementParser
 * @see ParsedTransaction
 */
public record StatementParseResult(
    List<ParsedTransaction> transactions,
    List<String> warnings,
    List<String> errors,
    String detectedFormatId
) {

    /**
     * Compact constructor that makes all collections unmodifiable and defensively copied.
     */
    public StatementParseResult {
        transactions = transactions == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(transactions));
        warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(warnings));
        errors = errors == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Returns true if there are any fatal errors.
     *
     * @return true if the errors list is non-empty
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns true if there are any non-fatal warnings.
     *
     * @return true if the warnings list is non-empty
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Returns the number of successfully parsed transactions.
     *
     * @return the transaction count
     */
    public int transactionCount() {
        return transactions.size();
    }

    /**
     * Creates a failure result with a single error message and no transactions.
     *
     * @param error the error message
     * @return a failure result
     */
    public static StatementParseResult failure(String error) {
        return new StatementParseResult(null, null, List.of(error), null);
    }

    /**
     * Creates a successful result with transactions and the detected format ID.
     *
     * @param transactions the parsed transactions
     * @param formatId     the format ID used for parsing
     * @return a successful result
     */
    public static StatementParseResult success(List<ParsedTransaction> transactions, String formatId) {
        return new StatementParseResult(transactions, null, null, formatId);
    }
}
