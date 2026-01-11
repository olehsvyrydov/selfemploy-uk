package uk.selfemploy.core.pdf;

/**
 * Exception thrown when PDF generation fails.
 *
 * <p>This exception is used to wrap underlying IO or library exceptions
 * that may occur during PDF generation, providing a consistent API for
 * the PDF generation service.</p>
 */
public class PdfGenerationException extends Exception {

    /**
     * Creates a new PdfGenerationException with the specified message.
     *
     * @param message the detail message
     */
    public PdfGenerationException(String message) {
        super(message);
    }

    /**
     * Creates a new PdfGenerationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause of the exception
     */
    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
