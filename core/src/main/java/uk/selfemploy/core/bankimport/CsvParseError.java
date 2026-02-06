package uk.selfemploy.core.bankimport;

/**
 * Represents a parse error for a single row in a CSV file.
 *
 * <p>Used by the error-tolerant parser to collect errors without
 * stopping the entire import process.</p>
 *
 * @param lineNumber the 1-based line number in the CSV file
 * @param rawLine the original line content that failed to parse
 * @param errorMessage description of what went wrong
 */
public record CsvParseError(
    int lineNumber,
    String rawLine,
    String errorMessage
) {
    public CsvParseError {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be >= 1");
        }
        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException("errorMessage cannot be null or blank");
        }
    }
}
