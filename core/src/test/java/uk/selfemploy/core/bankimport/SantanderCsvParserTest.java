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

@DisplayName("SantanderCsvParser")
class SantanderCsvParserTest {

    @TempDir
    Path tempDir;

    private SantanderCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new SantanderCsvParser();
    }

    @Nested
    @DisplayName("Bank Name")
    class BankName {

        @Test
        @DisplayName("returns Santander")
        void returnsSantander() {
            assertThat(parser.getBankName()).isEqualTo("Santander");
        }
    }

    @Nested
    @DisplayName("Header Detection")
    class HeaderDetection {

        @Test
        @DisplayName("detects Santander headers")
        void detectsSantanderHeaders() {
            String[] headers = {"Date", "Description", "Amount", "Balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("rejects non-Santander headers")
        void rejectsNonSantanderHeaders() {
            String[] headers = {"Date", "Description", "Money Out", "Money In", "Balance"};
            assertThat(parser.canParse(headers)).isFalse();
        }

        @Test
        @DisplayName("handles case-insensitive headers")
        void handlesCaseInsensitive() {
            String[] headers = {"date", "description", "amount", "balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("returns expected headers")
        void returnsExpectedHeaders() {
            assertThat(parser.getExpectedHeaders()).containsExactly(
                "Date", "Description", "Amount", "Balance");
        }
    }

    @Nested
    @DisplayName("Parsing")
    class Parsing {

        @Test
        @DisplayName("parses expense transaction (negative amount)")
        void parsesExpense() throws IOException {
            String csv = """
                Date,Description,Amount,Balance
                15/06/2025,CARD PAYMENT TO AMAZON,-49.99,1234.56
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            ImportedTransaction tx = txs.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("CARD PAYMENT TO AMAZON");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-49.99"));
            assertThat(tx.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("parses income transaction (positive amount)")
        void parsesIncome() throws IOException {
            String csv = """
                Date,Description,Amount,Balance
                15/06/2025,FASTER PAYMENTS RECEIPT FROM CLIENT,500.00,1734.56
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
                Date,Description,Amount,Balance
                15/06/2025,EXPENSE 1,-10.00,990.00
                16/06/2025,INCOME 1,50.00,1040.00
                17/06/2025,EXPENSE 2,-20.00,1020.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(3);
            assertThat(txs.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(txs.get(1).date()).isEqualTo(LocalDate.of(2025, 6, 16));
            assertThat(txs.get(2).date()).isEqualTo(LocalDate.of(2025, 6, 17));
        }

        @Test
        @DisplayName("handles quoted descriptions with commas")
        void handlesQuotedDescriptions() throws IOException {
            String csv = """
                Date,Description,Amount,Balance
                15/06/2025,"CARD PAYMENT, AMAZON MARKETPLACE",-49.99,1234.56
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).description()).isEqualTo("CARD PAYMENT, AMAZON MARKETPLACE");
        }

        @Test
        @DisplayName("handles currency symbols in amounts")
        void handlesCurrencySymbols() throws IOException {
            String csv = """
                Date,Description,Amount,Balance
                15/06/2025,PURCHASE,£-49.99,£1234.56
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).amount()).isEqualByComparingTo(new BigDecimal("-49.99"));
        }

        @Test
        @DisplayName("handles different date formats")
        void handlesDifferentDateFormats() throws IOException {
            String csv = """
                Date,Description,Amount,Balance
                15-Jun-2025,EXPENSE,-10.00,990.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
        }

        @Test
        @DisplayName("skips empty lines")
        void skipsEmptyLines() throws IOException {
            String csv = """
                Date,Description,Amount,Balance
                15/06/2025,EXPENSE 1,-10.00,990.00

                16/06/2025,EXPENSE 2,-20.00,970.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(2);
        }

        @Test
        @DisplayName("reference is always null")
        void referenceIsNull() throws IOException {
            String csv = """
                Date,Description,Amount,Balance
                15/06/2025,PURCHASE,-10.00,990.00
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
                Date,Description,Amount,Balance
                INVALID_DATE,EXPENSE,-10.00,990.00
                """;
            Path file = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(file, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("date");
        }

        @Test
        @DisplayName("throws when description is empty")
        void throwsWhenDescriptionEmpty() throws IOException {
            String csv = """
                Date,Description,Amount,Balance
                15/06/2025,,-10.00,990.00
                """;
            Path file = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(file, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("description");
        }

        @Test
        @DisplayName("throws when amount is empty")
        void throwsWhenAmountEmpty() throws IOException {
            String csv = """
                Date,Description,Amount,Balance
                15/06/2025,PURCHASE,,990.00
                """;
            Path file = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(file, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class);
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("santander.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
