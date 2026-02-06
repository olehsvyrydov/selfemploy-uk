package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MetroBankCsvParser")
class MetroBankCsvParserTest {

    @TempDir
    Path tempDir;

    private MetroBankCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new MetroBankCsvParser();
    }

    @Nested
    @DisplayName("Bank Name")
    class BankName {

        @Test
        @DisplayName("returns Metro Bank")
        void returnsMetroBank() {
            assertThat(parser.getBankName()).isEqualTo("Metro Bank");
        }
    }

    @Nested
    @DisplayName("Header Detection")
    class HeaderDetection {

        @Test
        @DisplayName("detects Metro Bank headers")
        void detectsMetroBankHeaders() {
            String[] headers = {"Date", "Transaction type", "Description", "Money out", "Money in", "Balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("rejects non-Metro Bank headers")
        void rejectsNonMetroBankHeaders() {
            String[] headers = {"Date", "Description", "Money Out", "Money In", "Balance"};
            assertThat(parser.canParse(headers)).isFalse();
        }

        @Test
        @DisplayName("handles case-insensitive headers")
        void handlesCaseInsensitive() {
            String[] headers = {"date", "transaction type", "description", "money out", "money in", "balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("returns expected headers")
        void returnsExpectedHeaders() {
            assertThat(parser.getExpectedHeaders()).containsExactly(
                "Date", "Transaction type", "Description", "Money out", "Money in", "Balance");
        }
    }

    @Nested
    @DisplayName("Parsing")
    class Parsing {

        @Test
        @DisplayName("parses expense transaction (Money out)")
        void parsesExpense() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Card Payment,AMAZON PURCHASE,49.99,,1234.56
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            ImportedTransaction tx = txs.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("Card Payment - AMAZON PURCHASE");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-49.99"));
            assertThat(tx.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("parses income transaction (Money in)")
        void parsesIncome() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Transfer,CLIENT PAYMENT LTD,,500.00,1734.56
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).amount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(txs.get(0).isIncome()).isTrue();
        }

        @Test
        @DisplayName("parses multiple transactions")
        void parsesMultiple() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Card Payment,EXPENSE 1,10.00,,990.00
                16/06/2025,Transfer,INCOME 1,,50.00,1040.00
                17/06/2025,Direct Debit,EXPENSE 2,20.00,,1020.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(3);
            assertThat(txs.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(txs.get(1).date()).isEqualTo(LocalDate.of(2025, 6, 16));
            assertThat(txs.get(2).date()).isEqualTo(LocalDate.of(2025, 6, 17));
        }

        @Test
        @DisplayName("combines type and description")
        void combinesTypeAndDescription() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Card Payment,TESCO STORES,25.00,,975.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs.get(0).description()).isEqualTo("Card Payment - TESCO STORES");
        }

        @Test
        @DisplayName("uses description only when type is empty")
        void usesDescriptionOnlyWhenTypeEmpty() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,,PURCHASE,25.00,,975.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs.get(0).description()).isEqualTo("PURCHASE");
        }

        @Test
        @DisplayName("uses type only when description is empty")
        void usesTypeOnlyWhenDescriptionEmpty() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Standing Order,,100.00,,900.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs.get(0).description()).isEqualTo("Standing Order");
        }

        @Test
        @DisplayName("handles quoted descriptions with commas")
        void handlesQuotedDescriptions() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Card Payment,"AMAZON, MARKETPLACE",49.99,,1234.56
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs.get(0).description()).isEqualTo("Card Payment - AMAZON, MARKETPLACE");
        }

        @Test
        @DisplayName("handles amounts with currency symbols")
        void handlesCurrencySymbols() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Card Payment,PURCHASE,£49.99,,£1234.56
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs.get(0).amount()).isEqualByComparingTo(new BigDecimal("-49.99"));
        }

        @Test
        @DisplayName("skips empty lines")
        void skipsEmptyLines() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Card Payment,EXPENSE 1,10.00,,990.00

                16/06/2025,Transfer,INCOME 1,,50.00,1040.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(2);
        }

        @Test
        @DisplayName("reference is always null")
        void referenceIsNull() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Card Payment,PURCHASE,10.00,,990.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs.get(0).reference()).isNull();
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("throws when date is invalid")
        void throwsWhenDateInvalid() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                INVALID,Card Payment,PURCHASE,10.00,,990.00
                """;
            Path file = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(file, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("date");
        }

        @Test
        @DisplayName("throws when both money columns are empty")
        void throwsWhenBothMoneyColumnsEmpty() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,Card Payment,PURCHASE,,,1000.00
                """;
            Path file = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(file, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("Money out");
        }

        @Test
        @DisplayName("throws when both type and description are empty")
        void throwsWhenBothTypeAndDescriptionEmpty() throws IOException {
            String csv = """
                Date,Transaction type,Description,Money out,Money in,Balance
                15/06/2025,,,10.00,,990.00
                """;
            Path file = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(file, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("description");
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("metrobank.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
