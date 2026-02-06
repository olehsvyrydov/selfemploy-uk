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

@DisplayName("RevolutCsvParser")
class RevolutCsvParserTest {

    @TempDir
    Path tempDir;

    private RevolutCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new RevolutCsvParser();
    }

    @Nested
    @DisplayName("Bank Name")
    class BankName {

        @Test
        @DisplayName("returns Revolut")
        void returnsRevolut() {
            assertThat(parser.getBankName()).isEqualTo("Revolut");
        }
    }

    @Nested
    @DisplayName("Header Detection")
    class HeaderDetection {

        @Test
        @DisplayName("detects Revolut headers")
        void detectsRevolutHeaders() {
            String[] headers = {"Type", "Product", "Started Date", "Completed Date",
                "Description", "Amount", "Fee", "Currency", "State", "Balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("rejects non-Revolut headers")
        void rejectsNonRevolutHeaders() {
            String[] headers = {"Date", "Description", "Amount", "Balance"};
            assertThat(parser.canParse(headers)).isFalse();
        }

        @Test
        @DisplayName("handles case-insensitive headers")
        void handlesCaseInsensitive() {
            String[] headers = {"type", "product", "started date", "completed date",
                "description", "amount", "fee", "currency", "state", "balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("returns expected headers")
        void returnsExpectedHeaders() {
            assertThat(parser.getExpectedHeaders()).containsExactly(
                "Type", "Product", "Started Date", "Completed Date",
                "Description", "Amount", "Fee", "Currency", "State", "Balance");
        }
    }

    @Nested
    @DisplayName("Parsing")
    class Parsing {

        @Test
        @DisplayName("parses expense transaction")
        void parsesExpense() throws IOException {
            String csv = """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                CARD_PAYMENT,Current,2025-06-14 09:30:00,2025-06-15 10:00:00,Amazon Marketplace,-49.99,0.00,GBP,COMPLETED,1234.56
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            ImportedTransaction tx = txs.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("Amazon Marketplace");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-49.99"));
            assertThat(tx.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("parses income transaction")
        void parsesIncome() throws IOException {
            String csv = """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                TRANSFER,Current,2025-06-15 08:00:00,2025-06-15 08:05:00,Client Payment,5000.00,0.00,GBP,COMPLETED,6234.56
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).amount()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(txs.get(0).isIncome()).isTrue();
        }

        @Test
        @DisplayName("skips non-completed transactions")
        void skipsNonCompleted() throws IOException {
            String csv = """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                CARD_PAYMENT,Current,2025-06-15 10:00:00,2025-06-15 10:00:00,Pending Purchase,-20.00,0.00,GBP,PENDING,1000.00
                CARD_PAYMENT,Current,2025-06-15 11:00:00,2025-06-15 11:00:00,Completed Purchase,-30.00,0.00,GBP,COMPLETED,970.00
                CARD_PAYMENT,Current,2025-06-15 12:00:00,2025-06-15 12:00:00,Reverted Purchase,-10.00,0.00,GBP,REVERTED,980.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).description()).isEqualTo("Completed Purchase");
        }

        @Test
        @DisplayName("skips non-GBP transactions")
        void skipsNonGbp() throws IOException {
            String csv = """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                CARD_PAYMENT,Current,2025-06-15 10:00:00,2025-06-15 10:00:00,Euro Purchase,-25.00,0.50,EUR,COMPLETED,1000.00
                CARD_PAYMENT,Current,2025-06-15 11:00:00,2025-06-15 11:00:00,UK Purchase,-30.00,0.00,GBP,COMPLETED,970.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).description()).isEqualTo("UK Purchase");
        }

        @Test
        @DisplayName("parses multiple transactions")
        void parsesMultiple() throws IOException {
            String csv = """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                CARD_PAYMENT,Current,2025-06-14 09:00:00,2025-06-15 10:00:00,Expense 1,-10.00,0.00,GBP,COMPLETED,990.00
                TRANSFER,Current,2025-06-15 08:00:00,2025-06-16 08:00:00,Income 1,50.00,0.00,GBP,COMPLETED,1040.00
                CARD_PAYMENT,Current,2025-06-16 12:00:00,2025-06-17 12:00:00,Expense 2,-20.00,0.00,GBP,COMPLETED,1020.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(3);
            assertThat(txs.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(txs.get(1).date()).isEqualTo(LocalDate.of(2025, 6, 16));
            assertThat(txs.get(2).date()).isEqualTo(LocalDate.of(2025, 6, 17));
        }

        @Test
        @DisplayName("uses type as description when description is empty")
        void usesTypeAsDescriptionFallback() throws IOException {
            String csv = """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                TOPUP,Current,2025-06-15 10:00:00,2025-06-15 10:00:00,,100.00,0.00,GBP,COMPLETED,1100.00
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).description()).isEqualTo("TOPUP");
        }

        @Test
        @DisplayName("handles quoted descriptions with commas")
        void handlesQuotedDescriptions() throws IOException {
            String csv = """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                CARD_PAYMENT,Current,2025-06-15 10:00:00,2025-06-15 10:00:00,"Amazon, Marketplace",-49.99,0.00,GBP,COMPLETED,950.01
                """;
            Path file = createCsvFile(csv);

            List<ImportedTransaction> txs = parser.parse(file, StandardCharsets.UTF_8);

            assertThat(txs).hasSize(1);
            assertThat(txs.get(0).description()).isEqualTo("Amazon, Marketplace");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("throws when amount is empty")
        void throwsWhenAmountEmpty() throws IOException {
            String csv = """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                CARD_PAYMENT,Current,2025-06-15 10:00:00,2025-06-15 10:00:00,Purchase,,0.00,GBP,COMPLETED,1000.00
                """;
            Path file = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(file, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class);
        }

        @Test
        @DisplayName("throws when date is invalid")
        void throwsWhenDateInvalid() throws IOException {
            String csv = """
                Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
                CARD_PAYMENT,Current,INVALID,INVALID,Purchase,-10.00,0.00,GBP,COMPLETED,990.00
                """;
            Path file = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(file, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class);
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("revolut.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
