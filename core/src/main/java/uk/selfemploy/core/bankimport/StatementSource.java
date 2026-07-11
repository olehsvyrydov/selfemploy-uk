package uk.selfemploy.core.bankimport;

/**
 * A source-agnostic port that yields normalized bank transactions.
 *
 * <p>Every way of getting transactions into the app — a CSV file today, an Open
 * Banking feed tomorrow — is an implementation of this interface. Downstream
 * processing (duplicate detection, categorization, reconciliation) consumes the
 * {@link ImportedTransaction}s a source produces and never needs to know where they
 * came from. Adding a new source therefore means implementing only this interface,
 * with no change to the pipeline behind it.</p>
 *
 * @see CsvStatementSource
 * @see StatementBatch
 */
public interface StatementSource {

    /**
     * A stable identifier for the kind of source, used as provenance on the batch —
     * e.g. {@code "csv"} or, later, {@code "open-banking:truelayer"}.
     *
     * @return the source-type identifier, never null or blank
     */
    String sourceType();

    /**
     * Fetches a batch of normalized transactions from this source.
     *
     * @return the fetched batch, never null (may be empty)
     * @throws StatementSourceException if the source cannot be read or parsed
     */
    StatementBatch fetch() throws StatementSourceException;
}
