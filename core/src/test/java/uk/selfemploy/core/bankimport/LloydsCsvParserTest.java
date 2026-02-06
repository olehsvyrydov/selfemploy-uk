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
 * Unit tests for LloydsCsvParser.
 *
 * Lloyds supports two CSV formats:
 * - Simplified: Transaction Date,Transaction Type,Description,Debit,Credit,Balance
 * - Full: Transaction Date,Transaction Type,Sort Code,Account Number,Transaction Description,Debit Amount,Credit Amount,Balance
 */
@DisplayName("LloydsCsvParser Tests")
class LloydsCsvParserTest {

    @TempDir
    Path tempDir;

    private LloydsCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new LloydsCsvParser();
    }

    @Nested
    @DisplayName("Bank Name Tests")
    class BankNameTests {

        @Test
        @DisplayName("should return Lloyds as bank name")
        void shouldReturnLloydsAsBankName() {
            assertThat(parser.getBankName()).isEqualTo("Lloyds");
        }
    }

    @Nested
    @DisplayName("Header Detection Tests")
    class HeaderDetectionTests {

        @Test
        @DisplayName("should detect simplified format headers")
        void shouldDetectSimplifiedHeaders() {
            String[] headers = {"Transaction Date", "Transaction Type", "Description", "Debit", "Credit", "Balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should detect full format headers")
        void shouldDetectFullFormatHeaders() {
            String[] headers = {"Transaction Date", "Transaction Type", "Sort Code", "Account Number",
                "Transaction Description", "Debit Amount", "Credit Amount", "Balance"};
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
            String[] headers = {"transaction date", "transaction type", "description", "debit", "credit", "balance"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should return simplified expected headers")
        void shouldReturnSimplifiedExpectedHeaders() {
            String[] expected = parser.getExpectedHeaders();
            assertThat(expected).containsExactly("Transaction Date", "Transaction Type", "Description", "Debit", "Credit", "Balance");
        }
    }

    @Nested
    @DisplayName("Simplified Format Parsing Tests")
    class SimplifiedFormatParsingTests {

        @Test
        @DisplayName("should parse expense transaction (Debit)")
        void shouldParseExpenseTransaction() throws IOException {
            String csv = """
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                15/06/2025,DD,VODAFONE LTD,35.00,,1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("DD - VODAFONE LTD");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-35.00"));
            assertThat(tx.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should parse income transaction (Credit)")
        void shouldParseIncomeTransaction() throws IOException {
            String csv = """
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                15/06/2025,FPO,FREELANCE PAYMENT,,1500.00,2734.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(tx.isIncome()).isTrue();
        }

        @Test
        @DisplayName("should combine type and description")
        void shouldCombineTypeAndDescription() throws IOException {
            String csv = """
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                15/06/2025,DEB,TESCO STORES,25.00,,975.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("DEB - TESCO STORES");
        }

        @Test
        @DisplayName("should parse multiple transactions")
        void shouldParseMultipleTransactions() throws IOException {
            String csv = """
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                15/06/2025,DD,EXPENSE 1,10.00,,990.00
                16/06/2025,FPO,INCOME 1,,50.00,1040.00
                17/06/2025,DEB,EXPENSE 2,20.00,,1020.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Full Format Parsing Tests")
    class FullFormatParsingTests {

        @Test
        @DisplayName("should parse full format expense transaction")
        void shouldParseFullFormatExpenseTransaction() throws IOException {
            String csv = """
                Transaction Date,Transaction Type,Sort Code,Account Number,Transaction Description,Debit Amount,Credit Amount,Balance
                15/06/2025,DD,12-34-56,12345678,VODAFONE LTD,35.00,,1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("DD - VODAFONE LTD");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-35.00"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should parse full format income transaction")
        void shouldParseFullFormatIncomeTransaction() throws IOException {
            String csv = """
                Transaction Date,Transaction Type,Sort Code,Account Number,Transaction Description,Debit Amount,Credit Amount,Balance
                15/06/2025,FPO,12-34-56,12345678,CLIENT PAYMENT,,2500.00,3734.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(transactions.get(0).isIncome()).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should skip empty lines")
        void shouldSkipEmptyLines() throws IOException {
            String csv = """
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                15/06/2025,DD,EXPENSE,10.00,,990.00

                16/06/2025,FPO,INCOME,,50.00,1040.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(2);
        }

        @Test
        @DisplayName("should handle quoted descriptions with commas")
        void shouldHandleQuotedDescriptions() throws IOException {
            String csv = """
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                15/06/2025,DEB,"AMAZON, MARKETPLACE",49.99,,1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("DEB - AMAZON, MARKETPLACE");
        }

        @Test
        @DisplayName("should set reference to null")
        void shouldSetReferenceToNull() throws IOException {
            String csv = """
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                15/06/2025,DEB,PURCHASE,10.00,,990.00
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
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                INVALID,DD,EXPENSE,10.00,,990.00
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
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                15/06/2025,DD,EXPENSE,,,990.00
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
                Transaction Date,Transaction Type,Description,Debit,Credit,Balance
                15/06/2025,,,10.00,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("description");
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("lloyds.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
