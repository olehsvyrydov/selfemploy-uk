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
 * Unit tests for HsbcCsvParser.
 *
 * HSBC CSV format:
 * Date,Type,Description,Paid Out,Paid In,Balance
 */
@DisplayName("HsbcCsvParser Tests")
class HsbcCsvParserTest {

    @TempDir
    Path tempDir;

    private HsbcCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new HsbcCsvParser();
    }

    @Nested
    @DisplayName("Bank Name Tests")
    class BankNameTests {

        @Test
        @DisplayName("should return HSBC as bank name")
        void shouldReturnHsbcAsBankName() {
            assertThat(parser.getBankName()).isEqualTo("HSBC");
        }
    }

    @Nested
    @DisplayName("Header Detection Tests")
    class HeaderDetectionTests {

        @Test
        @DisplayName("should detect HSBC headers")
        void shouldDetectHsbcHeaders() {
            String[] headers = {"Date", "Type", "Description", "Paid Out", "Paid In", "Balance"};
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
            String[] headers = {"date", "type", "description", "paid out", "paid in", "balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should return expected headers")
        void shouldReturnExpectedHeaders() {
            String[] expected = parser.getExpectedHeaders();
            assertThat(expected).containsExactly("Date", "Type", "Description", "Paid Out", "Paid In", "Balance");
        }

        @Test
        @DisplayName("should reject headers with wrong column count")
        void shouldRejectHeadersWithWrongColumnCount() {
            String[] tooFew = {"Date", "Type", "Description"};
            assertThat(parser.canParse(tooFew)).isFalse();
        }
    }

    @Nested
    @DisplayName("Parsing Tests")
    class ParsingTests {

        @Test
        @DisplayName("should parse expense transaction (Paid Out)")
        void shouldParseExpenseTransaction() throws IOException {
            String csv = """
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,DEB,TESCO STORES,29.50,,1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("DEB - TESCO STORES");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-29.50"));
            assertThat(tx.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should parse income transaction (Paid In)")
        void shouldParseIncomeTransaction() throws IOException {
            String csv = """
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,CR,SALARY PAYMENT,,2500.00,3734.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("CR - SALARY PAYMENT");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(tx.isIncome()).isTrue();
        }

        @Test
        @DisplayName("should combine type and description")
        void shouldCombineTypeAndDescription() throws IOException {
            String csv = """
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,DD,VODAFONE LTD,35.00,,1000.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("DD - VODAFONE LTD");
        }

        @Test
        @DisplayName("should use description only when type is empty")
        void shouldUseDescriptionOnlyWhenTypeEmpty() throws IOException {
            String csv = """
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,,PAYMENT RECEIVED,,100.00,1100.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("PAYMENT RECEIVED");
        }

        @Test
        @DisplayName("should parse multiple transactions")
        void shouldParseMultipleTransactions() throws IOException {
            String csv = """
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,DEB,EXPENSE 1,10.00,,990.00
                16/06/2025,CR,INCOME 1,,50.00,1040.00
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
        @DisplayName("should handle quoted descriptions with commas")
        void shouldHandleQuotedDescriptions() throws IOException {
            String csv = """
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,DEB,"AMAZON, MARKETPLACE",49.99,,1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).description()).isEqualTo("DEB - AMAZON, MARKETPLACE");
        }

        @Test
        @DisplayName("should handle amounts with currency symbols")
        void shouldHandleAmountsWithCurrencySymbols() throws IOException {
            String csv = """
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,DEB,PURCHASE,GBP49.99,,GBP1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("-49.99"));
        }

        @Test
        @DisplayName("should skip empty lines")
        void shouldSkipEmptyLines() throws IOException {
            String csv = """
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,DEB,EXPENSE,10.00,,990.00

                16/06/2025,CR,INCOME,,50.00,1040.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(2);
        }

        @Test
        @DisplayName("should set reference to null")
        void shouldSetReferenceToNull() throws IOException {
            String csv = """
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,DEB,TESCO,10.00,,990.00
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
                Date,Type,Description,Paid Out,Paid In,Balance
                INVALID_DATE,DEB,EXPENSE,10.00,,990.00
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
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,DEB,EXPENSE,,,990.00
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
                Date,Type,Description,Paid Out,Paid In,Balance
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
                Date,Type,Description,Paid Out,Paid In,Balance
                15/06/2025,DEB,EXPENSE
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("columns");
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("hsbc.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
