package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CsvStatementSource")
class CsvStatementSourceTest {

    @TempDir
    Path tempDir;

    /** A minimal parser whose match and output are fixed, so the test exercises the
     * source wrapper (detect -> parse -> batch) rather than any real bank format. */
    private static final class StubParser implements BankCsvParser {
        private final boolean matches;
        private final List<ImportedTransaction> output;
        private final CsvParseException failure;

        StubParser(boolean matches, List<ImportedTransaction> output, CsvParseException failure) {
            this.matches = matches;
            this.output = output;
            this.failure = failure;
        }

        @Override public String getBankName() {
            return "StubBank";
        }

        @Override public boolean canParse(String[] headers) {
            return matches;
        }

        @Override public List<ImportedTransaction> parse(Path csvFile, Charset charset)
                throws CsvParseException {
            if (failure != null) {
                throw failure;
            }
            return output;
        }

        @Override public String[] getExpectedHeaders() {
            return new String[] {"Date", "Amount", "Description"};
        }
    }

    private Path csvWithHeader() throws IOException {
        Path file = tempDir.resolve("statement.csv");
        Files.writeString(file, "Date,Amount,Description\n2025-05-01,100.00,Payment\n",
            StandardCharsets.UTF_8);
        return file;
    }

    private ImportedTransaction txn(String amount, String description) {
        return new ImportedTransaction(
            LocalDate.of(2025, 5, 1), new BigDecimal(amount), description, null, null);
    }

    @Test
    @DisplayName("fetches the detected parser's transactions and stamps CSV provenance")
    void fetchesAndStampsProvenance() throws Exception {
        List<ImportedTransaction> parsed = List.of(txn("100.00", "Client payment"), txn("-20.00", "Fuel"));
        BankFormatDetector detector = new BankFormatDetector(List.of(new StubParser(true, parsed, null)));
        CsvStatementSource source = new CsvStatementSource(csvWithHeader(), StandardCharsets.UTF_8, detector);

        StatementBatch batch = source.fetch();

        assertThat(source.sourceType()).isEqualTo("csv");
        assertThat(batch.sourceType()).isEqualTo("csv");
        assertThat(batch.sourceReference()).isEqualTo("statement.csv");
        assertThat(batch.detectedFormat()).isEqualTo("StubBank");
        assertThat(batch.transactions()).hasSize(2);
        assertThat(batch.transactions().get(0).description()).isEqualTo("Client payment");
    }

    @Test
    @DisplayName("throws StatementSourceException when no parser matches the file")
    void throwsWhenNoParserMatches() throws Exception {
        BankFormatDetector detector = new BankFormatDetector(List.of(new StubParser(false, List.of(), null)));
        CsvStatementSource source = new CsvStatementSource(csvWithHeader(), StandardCharsets.UTF_8, detector);

        assertThatThrownBy(source::fetch)
            .isInstanceOf(StatementSourceException.class)
            .hasMessageContaining("No bank CSV format matched");
    }

    @Test
    @DisplayName("wraps a parser failure as a StatementSourceException")
    void wrapsParserFailure() throws Exception {
        CsvParseException parseError = new CsvParseException("bad row", "statement.csv", 2, null);
        BankFormatDetector detector = new BankFormatDetector(List.of(new StubParser(true, null, parseError)));
        CsvStatementSource source = new CsvStatementSource(csvWithHeader(), StandardCharsets.UTF_8, detector);

        assertThatThrownBy(source::fetch)
            .isInstanceOf(StatementSourceException.class)
            .hasMessageContaining("Failed to parse")
            .hasCauseInstanceOf(CsvParseException.class);
    }

    @Test
    @DisplayName("wraps an unchecked parser exception (e.g. a bad amount) as a StatementSourceException")
    void wrapsUncheckedParserException() throws Exception {
        BankCsvParser throwingParser = new BankCsvParser() {
            @Override public String getBankName() {
                return "ThrowingBank";
            }

            @Override public boolean canParse(String[] headers) {
                return true;
            }

            @Override public List<ImportedTransaction> parse(Path csvFile, Charset charset) {
                throw new NumberFormatException("Character N is neither a decimal digit");
            }

            @Override public String[] getExpectedHeaders() {
                return new String[] {"Date", "Amount", "Description"};
            }
        };
        BankFormatDetector detector = new BankFormatDetector(List.of(throwingParser));
        CsvStatementSource source = new CsvStatementSource(csvWithHeader(), StandardCharsets.UTF_8, detector);

        assertThatThrownBy(source::fetch)
            .isInstanceOf(StatementSourceException.class)
            .hasMessageContaining("Unexpected error reading")
            .hasCauseInstanceOf(NumberFormatException.class);
    }
}
