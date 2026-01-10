package uk.selfemploy.core.service;

/**
 * Exception thrown when receipt storage operations fail.
 */
public class ReceiptStorageException extends RuntimeException {

    public enum ErrorType {
        FILE_TOO_LARGE("File exceeds maximum size limit"),
        UNSUPPORTED_FORMAT("Unsupported file format"),
        MAX_RECEIPTS_EXCEEDED("Maximum receipts per expense exceeded"),
        STORAGE_ERROR("Failed to store receipt file"),
        FILE_NOT_FOUND("Receipt file not found"),
        DELETE_ERROR("Failed to delete receipt file");

        private final String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final ErrorType errorType;

    public ReceiptStorageException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    public ReceiptStorageException(ErrorType errorType, String details) {
        super(errorType.getMessage() + ": " + details);
        this.errorType = errorType;
    }

    public ReceiptStorageException(ErrorType errorType, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
    }

    public ReceiptStorageException(ErrorType errorType, String details, Throwable cause) {
        super(errorType.getMessage() + ": " + details, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
