package uk.selfemploy.plugin.extension;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single parsed transaction from a bank statement.
 *
 * <p>This is the SPI output type that bank statement parsers produce.
 * UI modules convert these to their own view models for display.</p>
 *
 * <h2>Amount Convention</h2>
 * <p>Positive amount = income (money received).
 * Negative amount = expense (money paid out).
 * This is consistent with the BankTransaction convention in the common module.</p>
 *
 * @param date        transaction date, never null
 * @param description transaction description or reference, never blank
 * @param amount      signed amount (positive=income, negative=expense), never null
 * @param reference   optional bank reference or transaction ID (e.g., OFX FITID)
 * @param category    optional category hint from the statement
 * @param accountInfo optional account identifier (e.g., last 4 digits)
 *
 * @see BankStatementParser
 * @see StatementParseResult
 */
public record ParsedTransaction(
    LocalDate date,
    String description,
    BigDecimal amount,
    String reference,
    String category,
    String accountInfo
) {

    /**
     * Compact constructor with validation of required fields.
     */
    public ParsedTransaction {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be null or blank");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
    }

    /**
     * Returns true if this transaction represents income (positive amount).
     *
     * @return true if amount is positive
     */
    public boolean isIncome() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns true if this transaction represents an expense (negative amount).
     *
     * @return true if amount is negative
     */
    public boolean isExpense() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Returns the absolute value of the amount.
     *
     * @return the absolute amount (always non-negative)
     */
    public BigDecimal absoluteAmount() {
        return amount.abs();
    }
}
