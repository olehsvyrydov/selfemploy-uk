package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the transaction pipeline is source-agnostic: the same processing runs over a
 * batch regardless of which {@link StatementSource} produced it. A future Open Banking
 * source would slot in exactly where the in-memory fake does here.
 */
@DisplayName("StatementSource contract")
class StatementSourceContractTest {

    /** An entirely in-memory source — no CSV, no file — standing in for any non-CSV feed. */
    private record InMemorySource(List<ImportedTransaction> transactions) implements StatementSource {
        @Override public String sourceType() {
            return "in-memory";
        }

        @Override public StatementBatch fetch() {
            return new StatementBatch("in-memory", "unit-test", "fake", transactions);
        }
    }

    /** A tiny source-agnostic "pipeline": split a fetched batch into income/expense counts.
     * It only knows about {@link StatementSource}/{@link ImportedTransaction}. */
    private static Map<String, Long> summarise(StatementSource source) throws StatementSourceException {
        return source.fetch().transactions().stream()
            .collect(Collectors.groupingBy(
                t -> t.isIncome() ? "income" : "expense",
                Collectors.counting()));
    }

    private ImportedTransaction txn(String amount, String description) {
        return new ImportedTransaction(
            LocalDate.of(2025, 5, 1), new BigDecimal(amount), description, null, null);
    }

    @Test
    @DisplayName("a non-CSV source satisfies the same contract and feeds the same pipeline")
    void inMemorySourceFeedsSamePipeline() throws Exception {
        StatementSource source = new InMemorySource(List.of(
            txn("100.00", "Invoice 1"),
            txn("250.00", "Invoice 2"),
            txn("-30.00", "Stationery")));

        Map<String, Long> summary = summarise(source);

        assertThat(source.sourceType()).isEqualTo("in-memory");
        assertThat(summary).containsEntry("income", 2L).containsEntry("expense", 1L);
    }

    @Test
    @DisplayName("the pipeline treats two different sources with identical shape identically")
    void differentSourcesSameResult() throws Exception {
        List<ImportedTransaction> txns = List.of(txn("100.00", "A"), txn("-40.00", "B"));

        StatementSource a = new InMemorySource(txns);
        StatementSource b = new StatementSource() {
            @Override public String sourceType() {
                return "another-feed";
            }

            @Override public StatementBatch fetch() {
                return new StatementBatch("another-feed", "ref", "fmt", txns);
            }
        };

        assertThat(summarise(a)).isEqualTo(summarise(b));
    }
}
