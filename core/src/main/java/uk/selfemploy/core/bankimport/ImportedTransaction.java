package uk.selfemploy.core.bankimport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a transaction parsed from a bank CSV file.
 *
 * <p>This is a raw representation of a transaction before it is categorized
 * and saved as an Income or Expense.</p>
 *
 * <p>Amount convention:
 * <ul>
 *   <li>Positive amount = Income (money received)</li>
 *   <li>Negative amount = Expense (money paid out)</li>
 * </ul>
 */
public record ImportedTransaction(
    LocalDate date,
    BigDecimal amount,
    String description,
    BigDecimal balance,
    String reference
) {
    /**
     * Compact constructor for validation.
     */
    public ImportedTransaction {
        if (date == null) {
            throw new IllegalArgumentException("date cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount cannot be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be null or blank");
        }
    }

    /**
     * Returns true if this transaction represents income (positive amount).
     */
    public boolean isIncome() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Returns true if this transaction represents an expense (negative or zero amount).
     */
    public boolean isExpense() {
        return amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Returns the absolute value of the amount.
     *
     * @return the absolute amount (always positive)
     */
    public BigDecimal absoluteAmount() {
        return amount.abs();
    }

    /**
     * Generates a hash string for duplicate detection.
     *
     * <p>The hash is based on date, amount, and normalized description.
     * Balance and reference are not included as they may vary between imports
     * of the same transaction.</p>
     *
     * @return a hash string for duplicate detection
     */
    public String transactionHash() {
        String normalizedDescription = normalizeDescription(description);
        return String.format("%s|%s|%s",
            date.toString(),
            amount.stripTrailingZeros().toPlainString(),
            normalizedDescription
        );
    }

    /**
     * Normalizes a description for hash comparison.
     *
     * <p>Normalization includes:
     * <ul>
     *   <li>Convert to lowercase</li>
     *   <li>Trim whitespace</li>
     *   <li>Collapse multiple spaces to single space</li>
     * </ul>
     */
    private static String normalizeDescription(String desc) {
        return desc.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportedTransaction that = (ImportedTransaction) o;
        return Objects.equals(date, that.date) &&
               amount.compareTo(that.amount) == 0 &&
               Objects.equals(description, that.description) &&
               (balance == null ? that.balance == null : balance.compareTo(that.balance) == 0) &&
               Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, amount, description, balance, reference);
    }
}
