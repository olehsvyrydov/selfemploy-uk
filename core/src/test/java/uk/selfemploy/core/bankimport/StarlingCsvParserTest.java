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
 * Unit tests for StarlingCsvParser.
 *
 * Starling CSV format:
 * Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
 *
 * Uses single pre-signed Amount column (positive = income, negative = expense).
 */
@DisplayName("StarlingCsvParser Tests")
class StarlingCsvParserTest {

    @TempDir
    Path tempDir;

    private StarlingCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new StarlingCsvParser();
    }

    @Nested
    @DisplayName("Bank Name Tests")
    class BankNameTests {

        @Test
        @DisplayName("should return Starling as bank name")
        void shouldReturnStarlingAsBankName() {
            assertThat(parser.getBankName()).isEqualTo("Starling");
        }
    }

    @Nested
    @DisplayName("Header Detection Tests")
    class HeaderDetectionTests {

        @Test
        @DisplayName("should detect Starling headers")
        void shouldDetectStarlingHeaders() {
            String[] headers = {"Date", "Counter Party", "Reference", "Type", "Amount (GBP)", "Balance (GBP)"};
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
            String[] headers = {"date", "counter party", "reference", "type", "amount (gbp)", "balance (gbp)"};
            assertThat(parser.canParse(headers)).isTrue();
        }

        @Test
        @DisplayName("should return expected headers")
        void shouldReturnExpectedHeaders() {
            String[] expected = parser.getExpectedHeaders();
            assertThat(expected).containsExactly("Date", "Counter Party", "Reference", "Type", "Amount (GBP)", "Balance (GBP)");
        }
    }

    @Nested
    @DisplayName("Parsing Tests")
    class ParsingTests {

        @Test
        @DisplayName("should parse expense transaction (negative amount)")
        void shouldParseExpenseTransaction() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,TESCO STORES,CARD PURCHASE,CARD,-25.50,1234.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(tx.description()).isEqualTo("TESCO STORES - CARD PURCHASE");
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("-25.50"));
            assertThat(tx.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
            assertThat(tx.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should parse income transaction (positive amount)")
        void shouldParseIncomeTransaction() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,ACME LTD,INVOICE 123,FASTER PAYMENT,1500.00,2734.56
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            ImportedTransaction tx = transactions.get(0);
            assertThat(tx.amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(tx.isIncome()).isTrue();
        }

        @Test
        @DisplayName("should build description from counter party and reference")
        void shouldBuildDescriptionFromCounterPartyAndReference() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,JOHN DOE,RENT PAYMENT,TRANSFER,500.00,2500.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("JOHN DOE - RENT PAYMENT");
        }

        @Test
        @DisplayName("should use counter party only when reference matches")
        void shouldUseCounterPartyOnlyWhenReferenceDuplicate() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,TESCO STORES,TESCO STORES,CARD,-25.50,974.50
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            // When reference equals counter party, should not duplicate
            assertThat(transactions.get(0).description()).isEqualTo("TESCO STORES");
        }

        @Test
        @DisplayName("should use type when counter party and reference are empty")
        void shouldUseTypeWhenOthersEmpty() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,,,INTEREST,0.15,1000.15
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).description()).isEqualTo("INTEREST");
        }

        @Test
        @DisplayName("should store reference in transaction")
        void shouldStoreReference() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,ACME LTD,INV-2025-001,FASTER PAYMENT,1500.00,2500.00
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).reference()).isEqualTo("INV-2025-001");
        }

        @Test
        @DisplayName("should set reference to null when empty")
        void shouldSetReferenceNullWhenEmpty() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,TESCO,,CARD,-25.50,974.50
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).reference()).isNull();
        }

        @Test
        @DisplayName("should parse multiple transactions")
        void shouldParseMultipleTransactions() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,TESCO,CARD PAYMENT,CARD,-25.50,974.50
                16/06/2025,ACME LTD,INV-001,FPS,1500.00,2474.50
                17/06/2025,AMAZON,ORDER 123,CARD,-49.99,2424.51
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(3);
        }

        @Test
        @DisplayName("should skip empty lines")
        void shouldSkipEmptyLines() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,TESCO,PURCHASE,CARD,-25.50,974.50

                16/06/2025,ACME,PAYMENT,FPS,500.00,1474.50
                """;
            Path csvFile = createCsvFile(csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should throw when date is invalid")
        void shouldThrowWhenDateInvalid() throws IOException {
            String csv = """
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                INVALID,TESCO,REF,CARD,-25.50,974.50
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
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,TESCO,REF,CARD,,974.50
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
                Date,Counter Party,Reference,Type,Amount (GBP),Balance (GBP)
                15/06/2025,TESCO,REF
                """;
            Path csvFile = createCsvFile(csv);

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("columns");
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("starling.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
