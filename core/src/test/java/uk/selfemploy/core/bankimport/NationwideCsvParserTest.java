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

/**
 * Unit tests for NationwideCsvParser.
 *
 * Nationwide CSV format:
 * Date,Transaction type,Description,Paid out,Paid in,Balance
 */
@DisplayName("NationwideCsvParser Tests")
class NationwideCsvParserTest {

    @TempDir
    Path tempDir;

    private NationwideCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new NationwideCsvParser();
    }

    @Nested
    @DisplayName("Bank Name Tests")
    class BankNameTests {

        @Test
        @DisplayName("should return Nationwide as bank name")
        void shouldReturnNationwideAsBankName() {
            assertThat(parser.getBankName()).isEqualTo("Nationwide");
        }
    }

    @Nested
    @DisplayName("Header Detection Tests")
    class HeaderDetectionTests {

        @Test
        @DisplayName("should detect Nationwide headers")
        void shouldDetectNationwideHeaders() {
            String[] headers = {"Date", "Transaction type", "Description", "Paid out", "Paid in", "Balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should not detect other bank headers")
        void shouldNotDetectOtherBankHeaders() {
            String[] barclaysHeaders = {"Date", "Description", "Money Out", "Money In", "Balance"};
            assertThat(parser.canParse(barclaysHeaders)).isFalse();
        }

        @Test
        @DisplayName("should handle case-insensitive headers")
        void shouldHandleCaseInsensitiveHeaders() {
            String[] headers = {"date", "transaction type", "description", "paid out", "paid in", "balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should return expected headers")
        void shouldReturnExpectedHeaders() {
            String[] expected = parser.getExpectedHeaders();
            assertThat(expected).containsExactly("Date", "Transaction type", "Description", "Paid out", "Paid in", "Balance");
        }

        @Test
        @DisplayName("should reject wrong column count")
        void shouldRejectWrongColumnCount() {
            String[] tooFew = {"Date", "Description", "Balance"};
            assertThat(parser.canParse(tooFew)).isFalse();
        }
    }

    @Nested
    @DisplayName("Parsing Tests")
    class ParsingTests {

        @Test
        @DisplayName("should parse expense transaction (Paid out)")
        void shouldParseExpenseTransaction() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,Visa purchase,TESCO STORES,45.67,,1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("Visa purchase - TESCO STORES");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-45.67"));
            assertThat(tx.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should parse income transaction (Paid in)")
        void shouldParseIncomeTransaction() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,Transfer,SALARY PAYMENT,,2500.00,3734.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(tx.isIncome()).isTrue();
        }

        @Test
        @DisplayName("should combine type and description")
        void shouldCombineTypeAndDescription() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,Direct debit,BT GROUP PLC,55.00,,945.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("Direct debit - BT GROUP PLC");
        }

        @Test
        @DisplayName("should use description only when type is empty")
        void shouldUseDescriptionOnlyWhenTypeEmpty() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,,INTEREST PAYMENT,,0.15,1000.15
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("INTEREST PAYMENT");
        }

        @Test
        @DisplayName("should parse multiple transactions")
        void shouldParseMultipleTransactions() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,Visa,EXPENSE 1,10.00,,990.00
                16/06/2025,Transfer,INCOME 1,,50.00,1040.00
                17/06/2025,DD,EXPENSE 2,20.00,,1020.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(3);
            assertThat(transactions.get(0).isExpense()).isTrue();
            assertThat(transactions.get(1).isIncome()).isTrue();
            assertThat(transactions.get(2).isExpense()).isTrue();
        }

        @Test
        @DisplayName("should skip empty lines")
        void shouldSkipEmptyLines() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,Visa,EXPENSE,10.00,,990.00

                16/06/2025,Transfer,INCOME,,50.00,1040.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(2);
        }

        @Test
        @DisplayName("should handle amounts with currency symbols")
        void shouldHandleAmountsWithCurrencySymbols() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,Visa,PURCHASE,£49.99,,£1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("-49.99"));
        }

        @Test
        @DisplayName("should set reference to null")
        void shouldSetReferenceToNull() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,Visa,PURCHASE,10.00,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).reference()).isNull();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should throw when date is invalid")
        void shouldThrowWhenDateInvalid() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                INVALID,Visa,EXPENSE,10.00,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("date");
        }

        @Test
        @DisplayName("should throw when both amounts are empty")
        void shouldThrowWhenBothAmountsEmpty() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,Visa,EXPENSE,,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw when description is empty and type is empty")
        void shouldThrowWhenDescriptionEmpty() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,,,10.00,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should throw when too few columns")
        void shouldThrowWhenTooFewColumns() throws IOException {
            String csv = """
                Date,Transaction type,Description,Paid out,Paid in,Balance
                15/06/2025,Visa,EXPENSE
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("columns");
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("nationwide.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
