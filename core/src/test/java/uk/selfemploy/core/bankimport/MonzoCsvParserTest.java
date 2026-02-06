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
 * Unit tests for MonzoCsvParser.
 *
 * Monzo CSV format (core 8 columns, additional columns may vary):
 * Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,...
 *
 * Uses single pre-signed Amount column (positive = income, negative = expense).
 * Flexible header validation - only first 8 columns are checked.
 */
@DisplayName("MonzoCsvParser Tests")
class MonzoCsvParserTest {

    @TempDir
    Path tempDir;

    private MonzoCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new MonzoCsvParser();
    }

    @Nested
    @DisplayName("Bank Name Tests")
    class BankNameTests {

        @Test
        @DisplayName("should return Monzo as bank name")
        void shouldReturnMonzoAsBankName() {
            assertThat(parser.getBankName()).isEqualTo("Monzo");
        }
    }

    @Nested
    @DisplayName("Header Detection Tests")
    class HeaderDetectionTests {

        @Test
        @DisplayName("should detect simplified Monzo headers")
        void shouldDetectSimplifiedHeaders() {
            String[] headers = {"Transaction ID", "Date", "Time", "Type", "Name", "Emoji",
                "Category", "Amount", "Currency", "Notes", "Address", "Description"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should detect full Monzo headers")
        void shouldDetectFullHeaders() {
            String[] headers = {"Transaction ID", "Date", "Time", "Type", "Name", "Emoji",
                "Category", "Amount", "Currency", "Local amount", "Local currency",
                "Notes and #tags", "Address", "Receipt", "Description", "Category split"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should detect with only core 8 headers")
        void shouldDetectWithOnlyCoreHeaders() {
            String[] headers = {"Transaction ID", "Date", "Time", "Type", "Name", "Emoji",
                "Category", "Amount"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should not detect with fewer than 8 headers")
        void shouldNotDetectWithFewerHeaders() {
            String[] tooFew = {"Transaction ID", "Date", "Time", "Type", "Name"};
            assertThat(parser.canParse(tooFew)).isFalse();
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
            String[] headers = {"transaction id", "date", "time", "type", "name", "emoji",
                "category", "amount"};
            assertThat(parser.canParse(headers)).isTrue();
        }
    }

    @Nested
    @DisplayName("Parsing Tests")
    class ParsingTests {

        @Test
        @DisplayName("should parse expense transaction (negative amount)")
        void shouldParseExpenseTransaction() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_001,15/06/2025,14:30:00,Payment,TESCO,,,−25.50,GBP,,,TESCO STORES
                """;
            // Monzo uses minus sign, but let's use standard hyphen
            String csvFixed = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_001,15/06/2025,14:30:00,Payment,TESCO,,,-25.50,GBP,,,TESCO STORES
                """;
            Path csvFile = createCsvFile(csvFixed);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("TESCO");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-25.50"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should parse income transaction (positive amount)")
        void shouldParseIncomeTransaction() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_002,15/06/2025,09:00:00,Faster Payment,ACME LTD,,,1500.00,GBP,,,INVOICE 123
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(tx.isIncome()).isTrue();
        }

        @Test
        @DisplayName("should use Name as description")
        void shouldUseNameAsDescription() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_003,15/06/2025,12:00:00,Payment,STARBUCKS COFFEE,,Eating out,-4.50,GBP,,,
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("STARBUCKS COFFEE");
        }

        @Test
        @DisplayName("should fall back to Type when Name is empty")
        void shouldFallBackToTypeWhenNameEmpty() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_004,15/06/2025,12:00:00,Interest,,,Income,0.15,GBP,,,
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("Interest");
        }

        @Test
        @DisplayName("should store transaction ID as reference")
        void shouldStoreTransactionIdAsReference() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_abc123,15/06/2025,14:30:00,Payment,TESCO,,,-25.50,GBP,,,
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).reference()).isEqualTo("tx_abc123");
        }

        @Test
        @DisplayName("should not store balance (Monzo doesn't provide running balance)")
        void shouldNotStoreBalance() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_001,15/06/2025,14:30:00,Payment,TESCO,,,-25.50,GBP,,,
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).balance()).isNull();
        }

        @Test
        @DisplayName("should parse multiple transactions")
        void shouldParseMultipleTransactions() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_001,15/06/2025,14:30:00,Payment,TESCO,,,-25.50,GBP,,,
                tx_002,16/06/2025,09:00:00,FPS,ACME LTD,,,1500.00,GBP,,,
                tx_003,17/06/2025,11:15:00,Payment,AMAZON,,,-49.99,GBP,,,
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
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_001,15/06/2025,14:30:00,Payment,TESCO,,,-25.50,GBP,,,

                tx_002,16/06/2025,09:00:00,FPS,ACME,,,500.00,GBP,,,
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(2);
        }

        @Test
        @DisplayName("should handle ISO date format")
        void shouldHandleIsoDateFormat() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_001,2025-06-15,14:30:00,Payment,TESCO,,,-25.50,GBP,,,
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
        }

        @Test
        @DisplayName("should handle extra trailing columns gracefully")
        void shouldHandleExtraTrailingColumns() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Local amount,Local currency,Notes and #tags,Address,Receipt,Description,Category split
                tx_001,15/06/2025,14:30:00,Payment,TESCO,,,−25.50,GBP,-25.50,GBP,,London,,TESCO STORES,
                """;
            // Use standard minus sign
            String csvFixed = csv.replace("−", "-");
            Path csvFile = createCsvFile(csvFixed);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).description()).isEqualTo("TESCO");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should throw when date is invalid")
        void shouldThrowWhenDateInvalid() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_001,INVALID,14:30:00,Payment,TESCO,,,-25.50,GBP,,,
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("date");
        }

        @Test
        @DisplayName("should throw when amount is empty")
        void shouldThrowWhenAmountEmpty() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_001,15/06/2025,14:30:00,Payment,TESCO,,,,GBP,,,
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("Amount");
        }

        @Test
        @DisplayName("should throw when too few columns")
        void shouldThrowWhenTooFewColumns() throws IOException {
            String csv = """
                Transaction ID,Date,Time,Type,Name,Emoji,Category,Amount,Currency,Notes,Address,Description
                tx_001,15/06/2025,14:30:00,Payment
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("columns");
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("monzo.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
