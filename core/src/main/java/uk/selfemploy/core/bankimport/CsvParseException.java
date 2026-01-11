package uk.selfemploy.core.bankimport;

/**
 * Exception thrown when CSV parsing fails.
 */
public class CsvParseException extends RuntimeException {

    private final int lineNumber;
    private final String fileName;

    /**
     * Creates a new CsvParseException.
     *
     * @param message the error message
     */
    public CsvParseException(String message) {
        super(message);
        this.lineNumber = -1;
        this.fileName = null;
    }

    /**
     * Creates a new CsvParseException with line context.
     *
     * @param message the error message
     * @param fileName the name of the file being parsed
     * @param lineNumber the line number where the error occurred
     */
    public CsvParseException(String message, String fileName, int lineNumber) {
        super(String.format("%s (file: %s, line: %d)", message, fileName, lineNumber));
        this.lineNumber = lineNumber;
        this.fileName = fileName;
    }

    /**
     * Creates a new CsvParseException with a cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CsvParseException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
        this.fileName = null;
    }

    /**
     * Creates a new CsvParseException with line context and cause.
     *
     * @param message the error message
     * @param fileName the name of the file being parsed
     * @param lineNumber the line number where the error occurred
     * @param cause the underlying cause
     */
    public CsvParseException(String message, String fileName, int lineNumber, Throwable cause) {
        super(String.format("%s (file: %s, line: %d)", message, fileName, lineNumber), cause);
        this.lineNumber = lineNumber;
        this.fileName = fileName;
    }

    /**
     * Returns the line number where the error occurred.
     *
     * @return the line number, or -1 if unknown
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the file name being parsed.
     *
     * @return the file name, or null if unknown
     */
    public String getFileName() {
        return fileName;
    }
}
