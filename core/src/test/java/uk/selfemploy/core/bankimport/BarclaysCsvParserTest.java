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
 * Unit tests for BarclaysCsvParser.
 *
 * Barclays CSV format:
 * Date,Description,Money Out,Money In,Balance
 */
@DisplayName("BarclaysCsvParser Tests")
class BarclaysCsvParserTest {

    @TempDir
    Path tempDir;

    private BarclaysCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new BarclaysCsvParser();
    }

    @Nested
    @DisplayName("Bank Name Tests")
    class BankNameTests {

        @Test
        @DisplayName("should return Barclays as bank name")
        void shouldReturnBarclaysAsBankName() {
            assertThat(parser.getBankName()).isEqualTo("Barclays");
        }
    }

    @Nested
    @DisplayName("Header Detection Tests")
    class HeaderDetectionTests {

        @Test
        @DisplayName("should detect Barclays headers")
        void shouldDetectBarclaysHeaders() {
            String[] headers = {"Date", "Description", "Money Out", "Money In", "Balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should not detect other bank headers")
        void shouldNotDetectOtherBankHeaders() {
            String[] hsbcHeaders = {"Date", "Type", "Description", "Paid Out", "Paid In", "Balance"};
            assertThat(parser.canParse(hsbcHeaders)).isFalse();
        }

        @Test
        @DisplayName("should handle case-insensitive headers")
        void shouldHandleCaseInsensitiveHeaders() {
            String[] headers = {"date", "description", "money out", "money in", "balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should return expected headers")
        void shouldReturnExpectedHeaders() {
            String[] expected = parser.getExpectedHeaders();
            assertThat(expected).containsExactly("Date", "Description", "Money Out", "Money In", "Balance");
        }
    }

    @Nested
    @DisplayName("Parsing Tests")
    class ParsingTests {

        @Test
        @DisplayName("should parse expense transaction (Money Out)")
        void shouldParseExpenseTransaction() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,AMAZON PURCHASE,49.99,,1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("AMAZON PURCHASE");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-49.99"));
            assertThat(tx.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should parse income transaction (Money In)")
        void shouldParseIncomeTransaction() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,CLIENT PAYMENT,,500.00,1734.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("CLIENT PAYMENT");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(tx.isIncome()).isTrue();
        }

        @Test
        @DisplayName("should parse multiple transactions")
        void shouldParseMultipleTransactions() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,EXPENSE 1,10.00,,990.00
                16/06/2025,INCOME 1,,50.00,1040.00
                17/06/2025,EXPENSE 2,20.00,,1020.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(3);
            assertThat(transactions.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(transactions.get(1).date()).isEqualTo(LocalDate.of(2025, 6, 16));
            assertThat(transactions.get(2).date()).isEqualTo(LocalDate.of(2025, 6, 17));
        }

        @Test
        @DisplayName("should handle quoted descriptions with commas")
        void shouldHandleQuotedDescriptions() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,"AMAZON, MARKETPLACE",49.99,,1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).description()).isEqualTo("AMAZON, MARKETPLACE");
        }

        @Test
        @DisplayName("should handle amounts with currency symbols")
        void shouldHandleAmountsWithCurrencySymbols() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,PURCHASE,GBP49.99,,GBP1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("-49.99"));
        }

        @Test
        @DisplayName("should handle amounts with thousand separators")
        void shouldHandleAmountsWithThousandSeparators() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,LARGE PURCHASE,"1,234.56",,5000.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("-1234.56"));
        }

        @Test
        @DisplayName("should skip empty lines")
        void shouldSkipEmptyLines() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,EXPENSE,10.00,,990.00

                16/06/2025,INCOME,,50.00,1040.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(2);
        }

        @Test
        @DisplayName("should handle different date formats")
        void shouldHandleDifferentDateFormats() throws IOException {
            // Barclays sometimes uses different date formats
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15-Jun-2025,EXPENSE,10.00,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should throw when date is invalid")
        void shouldThrowWhenDateInvalid() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                INVALID_DATE,EXPENSE,10.00,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("date");
        }

        @Test
        @DisplayName("should throw when amount is invalid")
        void shouldThrowWhenAmountInvalid() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,EXPENSE,NOT_A_NUMBER,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw when description is empty")
        void shouldThrowWhenDescriptionEmpty() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,,10.00,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("description");
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("barclays.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
