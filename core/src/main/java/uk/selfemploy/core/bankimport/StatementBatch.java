package uk.selfemploy.core.bankimport;

import java.util.List;

/**
 * A batch of normalized transactions fetched from a {@link StatementSource}, together
 * with provenance describing where they came from.
 *
 * <p>The provenance fields let downstream code and audit trails record the origin of
 * imported data without coupling to any particular source implementation.</p>
 *
 * @param sourceType      the {@link StatementSource#sourceType()} that produced this
 *                        batch (e.g. {@code "csv"}); required
 * @param sourceReference a human-meaningful reference for the specific source, such as
 *                        a file name or account id; may be null
 * @param detectedFormat  the format/provider recognised for the source, such as the
 *                        bank name for a CSV; may be null
 * @param transactions    the normalized transactions; a null value is treated as an
 *                        empty list, and the list is copied defensively
 */
public record StatementBatch(
    String sourceType,
    String sourceReference,
    String detectedFormat,
    List<ImportedTransaction> transactions
) {
    public StatementBatch {
        if (sourceType == null || sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType cannot be null or blank");
        }
        transactions = transactions == null ? List.of() : List.copyOf(transactions);
    }

    /**
     * Returns the number of transactions in this batch.
     */
    public int size() {
        return transactions.size();
    }

    /**
     * Returns true if this batch contains no transactions.
     */
    public boolean isEmpty() {
        return transactions.isEmpty();
    }
}
