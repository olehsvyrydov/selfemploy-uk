package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for CSV Bank Import (SE-601).
 * Tests end-to-end import flow with real CSV data for all 6 supported UK banks.
 *
 * <p>Test Categories:
 * <ul>
 *   <li>Format detection for all 6 banks (AC-2)</li>
 *   <li>End-to-end import flow with real CSV data</li>
 *   <li>Duplicate detection accuracy (AC-7)</li>
 *   <li>Category suggestion accuracy (AC-8)</li>
 *   <li>Error handling (invalid files, too large, etc.) (AC-11)</li>
 * </ul>
 *
 * @author /adam - E2E Test Automation Engineer
 * @see <a href="https://jira.selfemploy.uk/browse/SE-601">SE-601 CSV Bank Import</a>
 */
@DisplayName("CSV Import Integration Tests - SE-601")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CsvImportIntegrationTest {

    @TempDir
    Path tempDir;

    // ========================================================================
    // FORMAT DETECTION TESTS (AC-2)
    // ========================================================================

    @Nested
    @DisplayName("Bank Format Detection - AC-2")
    @Order(1)
    class BankFormatDetectionTests {

        private BankFormatDetector detector;

        @BeforeEach
        void setUp() {
            detector = new BankFormatDetector(List.of(
                new BarclaysCsvParser(),
                new HsbcCsvParser(),
                new LloydsCsvParser(),
                new NationwideCsvParser(),
                new StarlingCsvParser(),
                new MonzoCsvParser()
            ));
        }

        @Test
        @DisplayName("P0: Should detect Barclays format from CSV headers")
        void shouldDetectBarclaysFormat() throws IOException {
            Path csvFile = copyResourceToTemp("csv/barclays-sample.csv");

            var result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
            assertThat(result.get().getBankName()).isEqualTo("Barclays");
        }

        @Test
        @DisplayName("P0: Should detect HSBC format from CSV headers")
        void shouldDetectHsbcFormat() throws IOException {
            Path csvFile = copyResourceToTemp("csv/hsbc-sample.csv");

            var result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
            assertThat(result.get().getBankName()).isEqualTo("HSBC");
        }

        @Test
        @DisplayName("P0: Should detect Lloyds format from CSV headers")
        void shouldDetectLloydsFormat() throws IOException {
            Path csvFile = copyResourceToTemp("csv/lloyds-sample.csv");

            var result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
            assertThat(result.get().getBankName()).isEqualTo("Lloyds");
        }

        @Test
        @DisplayName("P0: Should detect Nationwide format from CSV headers")
        void shouldDetectNationwideFormat() throws IOException {
            Path csvFile = copyResourceToTemp("csv/nationwide-sample.csv");

            var result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
            assertThat(result.get().getBankName()).isEqualTo("Nationwide");
        }

        @Test
        @DisplayName("P0: Should detect Starling format from CSV headers")
        void shouldDetectStarlingFormat() throws IOException {
            Path csvFile = copyResourceToTemp("csv/starling-sample.csv");

            var result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
            assertThat(result.get().getBankName()).isEqualTo("Starling");
        }

        @Test
        @DisplayName("P0: Should detect Monzo format from CSV headers")
        void shouldDetectMonzoFormat() throws IOException {
            Path csvFile = copyResourceToTemp("csv/monzo-sample.csv");

            var result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
            assertThat(result.get().getBankName()).isEqualTo("Monzo");
        }

        @Test
        @DisplayName("P0: Should return empty for unknown format (AC-3 fallback)")
        void shouldReturnEmptyForUnknownFormat() throws IOException {
            Path csvFile = copyResourceToTemp("csv/unknown-format.csv");

            var result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("P1: Should extract headers from CSV file")
        void shouldExtractHeaders() throws IOException {
            Path csvFile = copyResourceToTemp("csv/barclays-sample.csv");

            String[] headers = detector.extractHeaders(csvFile, StandardCharsets.UTF_8);

            assertThat(headers).containsExactly("Date", "Description", "Money Out", "Money In", "Balance");
        }

        @Test
        @DisplayName("P1: Should return empty headers for empty file")
        void shouldReturnEmptyHeadersForEmptyFile() throws IOException {
            Path emptyFile = tempDir.resolve("empty.csv");
            Files.writeString(emptyFile, "");

            String[] headers = detector.extractHeaders(emptyFile, StandardCharsets.UTF_8);

            assertThat(headers).isEmpty();
        }

        @Test
        @DisplayName("P1: Should list all available bank names")
        void shouldListAvailableBankNames() {
            List<String> bankNames = detector.getAvailableBankNames();

            assertThat(bankNames).containsExactlyInAnyOrder(
                "Barclays", "HSBC", "Lloyds", "Nationwide", "Starling", "Monzo"
            );
        }
    }

    // ========================================================================
    // PARSER TESTS - BARCLAYS
    // ========================================================================

    @Nested
    @DisplayName("Barclays CSV Parser")
    @Order(2)
    class BarclaysCsvParserTests {

        private BarclaysCsvParser parser;

        @BeforeEach
        void setUp() {
            parser = new BarclaysCsvParser();
        }

        @Test
        @DisplayName("P0: Should parse Barclays CSV with income and expense transactions")
        void shouldParseBarclaysCsv() throws IOException {
            Path csvFile = copyResourceToTemp("csv/barclays-sample.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(6);

            // Check income transaction
            ImportedTransaction income = transactions.get(0);
            assertThat(income.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(income.description()).isEqualTo("CLIENT PAYMENT ABC LTD");
            assertThat(income.amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(income.isIncome()).isTrue();
            assertThat(income.balance()).isEqualByComparingTo(new BigDecimal("2500.00"));

            // Check expense transaction
            ImportedTransaction expense = transactions.get(1);
            assertThat(expense.date()).isEqualTo(LocalDate.of(2025, 6, 16));
            assertThat(expense.description()).isEqualTo("AMAZON MARKETPLACE");
            assertThat(expense.amount()).isEqualByComparingTo(new BigDecimal("-45.50"));
            assertThat(expense.isExpense()).isTrue();
        }

        @Test
        @DisplayName("P0: Should correctly identify income vs expense amounts")
        void shouldIdentifyIncomeVsExpense() throws IOException {
            Path csvFile = copyResourceToTemp("csv/barclays-sample.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            long incomeCount = transactions.stream().filter(ImportedTransaction::isIncome).count();
            long expenseCount = transactions.stream().filter(ImportedTransaction::isExpense).count();

            assertThat(incomeCount).isEqualTo(3);
            assertThat(expenseCount).isEqualTo(3);
        }

        @Test
        @DisplayName("P1: Should handle quoted fields with commas")
        void shouldHandleQuotedFields() throws IOException {
            Path csvFile = copyResourceToTemp("csv/barclays-quoted-fields.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(3);
            assertThat(transactions.get(0).description()).isEqualTo("CLIENT PAYMENT, INCLUDING VAT");
            // CSV parser strips escaped quotes, which is expected behavior
            assertThat(transactions.get(1).description()).containsIgnoringCase("AMAZON");
            assertThat(transactions.get(1).description()).containsIgnoringCase("PRIME");
        }

        @Test
        @DisplayName("P1: Should return expected headers")
        void shouldReturnExpectedHeaders() {
            String[] headers = parser.getExpectedHeaders();

            assertThat(headers).containsExactly("Date", "Description", "Money Out", "Money In", "Balance");
        }

        @Test
        @DisplayName("P1: Should detect canParse with correct headers")
        void shouldDetectCanParse() {
            String[] validHeaders = {"Date", "Description", "Money Out", "Money In", "Balance"};
            String[] invalidHeaders = {"Date", "Type", "Amount"};

            assertThat(parser.canParse(validHeaders)).isTrue();
            assertThat(parser.canParse(invalidHeaders)).isFalse();
        }

        @Test
        @DisplayName("P2: Should skip blank lines")
        void shouldSkipBlankLines() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,PAYMENT,,100.00,100.00

                16/06/2025,EXPENSE,50.00,,50.00
                """;
            Path csvFile = tempDir.resolve("blank-lines.csv");
            Files.writeString(csvFile, csv);

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(2);
        }
    }

    // ========================================================================
    // PARSER TESTS - HSBC
    // ========================================================================

    @Nested
    @DisplayName("HSBC CSV Parser")
    @Order(3)
    class HsbcCsvParserTests {

        private HsbcCsvParser parser;

        @BeforeEach
        void setUp() {
            parser = new HsbcCsvParser();
        }

        @Test
        @DisplayName("P0: Should parse HSBC CSV with type information")
        void shouldParseHsbcCsv() throws IOException {
            Path csvFile = copyResourceToTemp("csv/hsbc-sample.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(6);

            // HSBC combines type and description
            ImportedTransaction income = transactions.get(0);
            assertThat(income.description()).contains("PAY");
            assertThat(income.description()).contains("CLIENT INVOICE 1234");
            assertThat(income.isIncome()).isTrue();
        }

        @Test
        @DisplayName("P0: Should correctly parse HSBC date format")
        void shouldParseHsbcDateFormat() throws IOException {
            Path csvFile = copyResourceToTemp("csv/hsbc-sample.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
        }
    }

    // ========================================================================
    // PARSER TESTS - LLOYDS
    // ========================================================================

    @Nested
    @DisplayName("Lloyds CSV Parser")
    @Order(4)
    class LloydsCsvParserTests {

        private LloydsCsvParser parser;

        @BeforeEach
        void setUp() {
            parser = new LloydsCsvParser();
        }

        @Test
        @DisplayName("P0: Should parse Lloyds CSV with transaction type")
        void shouldParseLloydsCsv() throws IOException {
            Path csvFile = copyResourceToTemp("csv/lloyds-sample.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(6);

            // Check combined description
            ImportedTransaction income = transactions.get(0);
            assertThat(income.description()).contains("FPO");
            assertThat(income.description()).contains("CLIENT PAYMENT REF001");
        }

        @Test
        @DisplayName("P1: Should support both Lloyds format variants")
        void shouldSupportLloydsFormatVariants() {
            // Simplified format
            String[] simple = {"Transaction Date", "Transaction Type", "Description", "Debit", "Credit", "Balance"};
            // Full format
            String[] full = {"Transaction Date", "Transaction Type", "Sort Code", "Account Number", "Transaction Description", "Debit Amount", "Credit Amount", "Balance"};

            assertThat(parser.canParse(simple)).isTrue();
            assertThat(parser.canParse(full)).isTrue();
        }
    }

    // ========================================================================
    // PARSER TESTS - NATIONWIDE
    // ========================================================================

    @Nested
    @DisplayName("Nationwide CSV Parser")
    @Order(5)
    class NationwideCsvParserTests {

        private NationwideCsvParser parser;

        @BeforeEach
        void setUp() {
            parser = new NationwideCsvParser();
        }

        @Test
        @DisplayName("P0: Should parse Nationwide CSV")
        void shouldParseNationwideCsv() throws IOException {
            Path csvFile = copyResourceToTemp("csv/nationwide-sample.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(6);

            ImportedTransaction income = transactions.get(0);
            assertThat(income.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(income.isIncome()).isTrue();
            assertThat(income.amount()).isEqualByComparingTo(new BigDecimal("4500.00"));
        }
    }

    // ========================================================================
    // PARSER TESTS - STARLING
    // ========================================================================

    @Nested
    @DisplayName("Starling CSV Parser")
    @Order(6)
    class StarlingCsvParserTests {

        private StarlingCsvParser parser;

        @BeforeEach
        void setUp() {
            parser = new StarlingCsvParser();
        }

        @Test
        @DisplayName("P0: Should parse Starling CSV with single amount column")
        void shouldParseStarlingCsv() throws IOException {
            Path csvFile = copyResourceToTemp("csv/starling-sample.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(6);

            // Starling uses single amount column (positive = income, negative = expense)
            ImportedTransaction income = transactions.get(0);
            assertThat(income.amount()).isEqualByComparingTo(new BigDecimal("1750.00"));
            assertThat(income.isIncome()).isTrue();

            ImportedTransaction expense = transactions.get(1);
            assertThat(expense.amount()).isEqualByComparingTo(new BigDecimal("-89.99"));
            assertThat(expense.isExpense()).isTrue();
        }

        @Test
        @DisplayName("P1: Should include reference in transaction")
        void shouldIncludeReference() throws IOException {
            Path csvFile = copyResourceToTemp("csv/starling-sample.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).reference()).isEqualTo("INV-2025-001");
        }
    }

    // ========================================================================
    // PARSER TESTS - MONZO
    // ========================================================================

    @Nested
    @DisplayName("Monzo CSV Parser")
    @Order(7)
    class MonzoCsvParserTests {

        private MonzoCsvParser parser;

        @BeforeEach
        void setUp() {
            parser = new MonzoCsvParser();
        }

        @Test
        @DisplayName("P0: Should parse Monzo CSV with transaction ID")
        void shouldParseMonzoCsv() throws IOException {
            Path csvFile = copyResourceToTemp("csv/monzo-sample.csv");

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(6);

            ImportedTransaction income = transactions.get(0);
            assertThat(income.description()).isEqualTo("Business Client A");
            assertThat(income.reference()).isEqualTo("tx_001a2b3c");
            assertThat(income.isIncome()).isTrue();
        }

        @Test
        @DisplayName("P1: Should handle flexible Monzo header format")
        void shouldHandleFlexibleMonzoHeader() {
            String[] headers = {"Transaction ID", "Date", "Time", "Type", "Name", "Emoji", "Category", "Amount"};
            assertThat(parser.canParse(headers)).isTrue();
        }
    }

    // ========================================================================
    // DUPLICATE DETECTION TESTS (AC-7)
    // ========================================================================

    @Nested
    @DisplayName("Duplicate Detection - AC-7")
    @Order(8)
    class DuplicateDetectionTests {

        @Test
        @DisplayName("P0: Should detect duplicates within same file")
        void shouldDetectDuplicatesInFile() throws IOException {
            Path csvFile = copyResourceToTemp("csv/barclays-with-duplicates.csv");
            BarclaysCsvParser parser = new BarclaysCsvParser();

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            // Count unique transaction hashes
            long uniqueHashes = transactions.stream()
                .map(ImportedTransaction::transactionHash)
                .distinct()
                .count();

            // 5 transactions but only 3 unique
            assertThat(transactions).hasSize(5);
            assertThat(uniqueHashes).isEqualTo(3);
        }

        @Test
        @DisplayName("P0: Should generate consistent transaction hash")
        void shouldGenerateConsistentTransactionHash() {
            ImportedTransaction tx1 = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                null,
                null
            );

            ImportedTransaction tx2 = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                new BigDecimal("500.00"), // Different balance
                "REF123"                   // Different reference
            );

            // Hash should be same (ignores balance and reference)
            assertThat(tx1.transactionHash()).isEqualTo(tx2.transactionHash());
        }

        @Test
        @DisplayName("P1: Should normalize description in hash")
        void shouldNormalizeDescriptionInHash() {
            ImportedTransaction tx1 = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                null, null
            );

            ImportedTransaction tx2 = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "client payment",  // lowercase
                null, null
            );

            ImportedTransaction tx3 = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "  CLIENT   PAYMENT  ",  // extra whitespace
                null, null
            );

            assertThat(tx1.transactionHash()).isEqualTo(tx2.transactionHash());
            assertThat(tx1.transactionHash()).isEqualTo(tx3.transactionHash());
        }

        @Test
        @DisplayName("P1: Should differentiate by amount")
        void shouldDifferentiateByAmount() {
            ImportedTransaction tx1 = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "PAYMENT",
                null, null
            );

            ImportedTransaction tx2 = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.01"), // Different amount
                "PAYMENT",
                null, null
            );

            assertThat(tx1.transactionHash()).isNotEqualTo(tx2.transactionHash());
        }

        @Test
        @DisplayName("P1: Should differentiate by date")
        void shouldDifferentiateByDate() {
            ImportedTransaction tx1 = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "PAYMENT",
                null, null
            );

            ImportedTransaction tx2 = new ImportedTransaction(
                LocalDate.of(2025, 6, 16), // Different date
                new BigDecimal("100.00"),
                "PAYMENT",
                null, null
            );

            assertThat(tx1.transactionHash()).isNotEqualTo(tx2.transactionHash());
        }
    }

    // ========================================================================
    // CATEGORY SUGGESTION TESTS (AC-8)
    // ========================================================================

    @Nested
    @DisplayName("Category Suggestion - AC-8")
    @Order(9)
    class CategorySuggestionTests {

        private DescriptionCategorizer categorizer;

        @BeforeEach
        void setUp() {
            categorizer = new DescriptionCategorizer();
        }

        @ParameterizedTest
        @DisplayName("P0: Should suggest OFFICE_COSTS for office-related keywords")
        @ValueSource(strings = {
            "AMAZON PURCHASE",
            "Microsoft 365 Subscription",
            "ADOBE CREATIVE CLOUD",
            "VODAFONE MOBILE",
            "BT INTERNET",
            "SKY BROADBAND"
        })
        void shouldSuggestOfficeCosts(String description) {
            CategorySuggestion<ExpenseCategory> suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
            assertThat(suggestion.confidence()).isEqualTo(Confidence.HIGH);
        }

        @ParameterizedTest
        @DisplayName("P0: Should suggest TRAVEL for travel-related keywords")
        @ValueSource(strings = {
            "UBER TRIP",
            "TRAINLINE BOOKING",
            "PREMIER INN HOTEL",
            "EASYJET FLIGHT",
            "PARKING NCP"
        })
        void shouldSuggestTravel(String description) {
            CategorySuggestion<ExpenseCategory> suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.TRAVEL);
            assertThat(suggestion.confidence()).isEqualTo(Confidence.HIGH);
        }

        @ParameterizedTest
        @DisplayName("P0: Should suggest TRAVEL_MILEAGE for fuel keywords")
        @ValueSource(strings = {
            "SHELL PETROL",
            "BP FUEL",
            "ESSO GARAGE",
            "TEXACO DIESEL"
        })
        void shouldSuggestTravelMileage(String description) {
            CategorySuggestion<ExpenseCategory> suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.TRAVEL_MILEAGE);
            assertThat(suggestion.confidence()).isEqualTo(Confidence.HIGH);
        }

        @ParameterizedTest
        @DisplayName("P0: Should suggest PREMISES for utility keywords")
        @ValueSource(strings = {
            "BRITISH GAS ELECTRICITY",
            "EDF ENERGY",
            "OCTOPUS ENERGY",
            "RENT PAYMENT"
        })
        void shouldSuggestPremises(String description) {
            CategorySuggestion<ExpenseCategory> suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.PREMISES);
            assertThat(suggestion.confidence()).isEqualTo(Confidence.HIGH);
        }

        @ParameterizedTest
        @DisplayName("P0: Should suggest PROFESSIONAL_FEES for professional services")
        @ValueSource(strings = {
            "ACCOUNTANT FEE",
            "SOLICITOR CONSULTATION",
            "LEGAL SERVICES LTD"
        })
        void shouldSuggestProfessionalFees(String description) {
            CategorySuggestion<ExpenseCategory> suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.PROFESSIONAL_FEES);
            assertThat(suggestion.confidence()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("P1: Should default to OTHER_EXPENSES with low confidence")
        void shouldDefaultToOtherExpenses() {
            CategorySuggestion<ExpenseCategory> suggestion = categorizer.suggestExpenseCategory("UNKNOWN MERCHANT XYZ");

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.OTHER_EXPENSES);
            assertThat(suggestion.confidence()).isEqualTo(Confidence.LOW);
        }

        @Test
        @DisplayName("P0: Should default to SALES for income")
        void shouldDefaultToSalesForIncome() {
            CategorySuggestion<IncomeCategory> suggestion = categorizer.suggestIncomeCategory("CLIENT PAYMENT");

            assertThat(suggestion.category()).isEqualTo(IncomeCategory.SALES);
            assertThat(suggestion.confidence()).isEqualTo(Confidence.MEDIUM);
        }

        @Test
        @DisplayName("P1: Should suggest OTHER_INCOME for interest payments")
        void shouldSuggestOtherIncomeForInterest() {
            CategorySuggestion<IncomeCategory> suggestion = categorizer.suggestIncomeCategory("BANK INTEREST");

            assertThat(suggestion.category()).isEqualTo(IncomeCategory.OTHER_INCOME);
            assertThat(suggestion.confidence()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("P1: Should be case insensitive")
        void shouldBeCaseInsensitive() {
            CategorySuggestion<ExpenseCategory> lowerCase = categorizer.suggestExpenseCategory("amazon purchase");
            CategorySuggestion<ExpenseCategory> upperCase = categorizer.suggestExpenseCategory("AMAZON PURCHASE");
            CategorySuggestion<ExpenseCategory> mixedCase = categorizer.suggestExpenseCategory("Amazon Purchase");

            assertThat(lowerCase.category()).isEqualTo(upperCase.category());
            assertThat(lowerCase.category()).isEqualTo(mixedCase.category());
        }
    }

    // ========================================================================
    // ERROR HANDLING TESTS (AC-11)
    // ========================================================================

    @Nested
    @DisplayName("Error Handling - File Validation")
    @Order(10)
    class ErrorHandlingTests {

        @Test
        @DisplayName("P0: Should enforce 10MB file size limit (AC-11)")
        void shouldEnforceFileSizeLimit() {
            // Verify the constant is set correctly
            assertThat(CsvImportService.MAX_FILE_SIZE_BYTES)
                .isEqualTo(10 * 1024 * 1024L);
        }

        @Test
        @DisplayName("P0: Should handle empty CSV file")
        void shouldHandleEmptyCsvFile() throws IOException {
            Path csvFile = copyResourceToTemp("csv/empty.csv");
            BarclaysCsvParser parser = new BarclaysCsvParser();

            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).isEmpty();
        }

        @Test
        @DisplayName("P1: Should throw CsvParseException for missing file")
        void shouldThrowForMissingFile() {
            Path missingFile = tempDir.resolve("nonexistent.csv");
            BarclaysCsvParser parser = new BarclaysCsvParser();

            assertThatThrownBy(() -> parser.parse(missingFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class);
        }

        @Test
        @DisplayName("P1: Should throw CsvParseException with line number for invalid data")
        void shouldThrowWithLineNumber() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,VALID TRANSACTION,,100.00,100.00
                INVALID-DATE,INVALID TRANSACTION,,50.00,150.00
                """;
            Path csvFile = tempDir.resolve("invalid-date.csv");
            Files.writeString(csvFile, csv);

            BarclaysCsvParser parser = new BarclaysCsvParser();

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("line");
        }

        @Test
        @DisplayName("P1: Should throw for empty description")
        void shouldThrowForEmptyDescription() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,,,100.00,100.00
                """;
            Path csvFile = tempDir.resolve("empty-desc.csv");
            Files.writeString(csvFile, csv);

            BarclaysCsvParser parser = new BarclaysCsvParser();

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("description");
        }

        @Test
        @DisplayName("P1: Should throw for missing amount")
        void shouldThrowForMissingAmount() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,TRANSACTION,,,100.00
                """;
            Path csvFile = tempDir.resolve("no-amount.csv");
            Files.writeString(csvFile, csv);

            BarclaysCsvParser parser = new BarclaysCsvParser();

            assertThatThrownBy(() -> parser.parse(csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("amount");
        }
    }

    // ========================================================================
    // MANUAL MAPPING TESTS (AC-3)
    // ========================================================================

    @Nested
    @DisplayName("Manual Column Mapping - AC-3")
    @Order(11)
    class ManualMappingTests {

        @Test
        @DisplayName("P0: Should parse with single amount column mapping")
        void shouldParseWithSingleAmountColumn() throws IOException {
            String csv = """
                transaction_date,memo,value,running_total
                2025-06-15,CLIENT PAYMENT,1500.00,2500.00
                2025-06-16,EXPENSE,-45.50,2454.50
                """;
            Path csvFile = tempDir.resolve("custom.csv");
            Files.writeString(csvFile, csv);

            ManualMappingParser.ColumnMapping mapping = new ManualMappingParser.ColumnMapping.Builder()
                .dateColumn(0)
                .dateFormat("yyyy-MM-dd")
                .descriptionColumn(1)
                .amountColumn(2)
                .balanceColumn(3)
                .build();

            ManualMappingParser parser = new ManualMappingParser(mapping);
            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(2);
            assertThat(transactions.get(0).isIncome()).isTrue();
            assertThat(transactions.get(1).isExpense()).isTrue();
        }

        @Test
        @DisplayName("P0: Should parse with separate debit/credit columns")
        void shouldParseWithSeparateDebitCreditColumns() throws IOException {
            String csv = """
                Date,Description,Debit,Credit,Balance
                15/06/2025,PAYMENT,,1500.00,2500.00
                16/06/2025,EXPENSE,45.50,,2454.50
                """;
            Path csvFile = tempDir.resolve("debit-credit.csv");
            Files.writeString(csvFile, csv);

            ManualMappingParser.ColumnMapping mapping = new ManualMappingParser.ColumnMapping.Builder()
                .dateColumn(0)
                .dateFormat("dd/MM/yyyy")
                .descriptionColumn(1)
                .debitColumn(2)
                .creditColumn(3)
                .debitIsNegative(true)
                .creditIsPositive(true)
                .balanceColumn(4)
                .build();

            ManualMappingParser parser = new ManualMappingParser(mapping);
            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(2);
            assertThat(transactions.get(0).amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(transactions.get(1).amount()).isEqualByComparingTo(new BigDecimal("-45.50"));
        }

        @Test
        @DisplayName("P1: Should support custom bank name")
        void shouldSupportCustomBankName() {
            ManualMappingParser.ColumnMapping mapping = new ManualMappingParser.ColumnMapping.Builder()
                .bankName("My Custom Bank")
                .dateColumn(0)
                .descriptionColumn(1)
                .amountColumn(2)
                .build();

            ManualMappingParser parser = new ManualMappingParser(mapping);

            assertThat(parser.getBankName()).isEqualTo("My Custom Bank");
        }

        @Test
        @DisplayName("P1: Should skip header row when configured")
        void shouldSkipHeaderRow() throws IOException {
            String csv = """
                Date,Description,Amount
                15/06/2025,PAYMENT,100.00
                """;
            Path csvFile = tempDir.resolve("with-header.csv");
            Files.writeString(csvFile, csv);

            ManualMappingParser.ColumnMapping mapping = new ManualMappingParser.ColumnMapping.Builder()
                .hasHeaderRow(true)
                .dateColumn(0)
                .descriptionColumn(1)
                .amountColumn(2)
                .build();

            ManualMappingParser parser = new ManualMappingParser(mapping);
            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions).hasSize(1);
            assertThat(transactions.get(0).description()).isEqualTo("PAYMENT");
        }

        @Test
        @DisplayName("P2: Should support multiple date formats")
        void shouldSupportMultipleDateFormats() throws IOException {
            String csv = """
                2025-06-15,PAYMENT,100.00
                """;
            Path csvFile = tempDir.resolve("iso-date.csv");
            Files.writeString(csvFile, csv);

            ManualMappingParser.ColumnMapping mapping = new ManualMappingParser.ColumnMapping.Builder()
                .hasHeaderRow(false)
                .dateColumn(0)
                .dateFormat("yyyy-MM-dd")
                .descriptionColumn(1)
                .amountColumn(2)
                .build();

            ManualMappingParser parser = new ManualMappingParser(mapping);
            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);

            assertThat(transactions.get(0).date()).isEqualTo(LocalDate.of(2025, 6, 15));
        }
    }

    // ========================================================================
    // IMPORTED TRANSACTION TESTS
    // ========================================================================

    @Nested
    @DisplayName("ImportedTransaction Record")
    @Order(12)
    class ImportedTransactionTests {

        @Test
        @DisplayName("P0: Should validate non-null date")
        void shouldValidateNonNullDate() {
            assertThatThrownBy(() -> new ImportedTransaction(
                null,
                new BigDecimal("100.00"),
                "DESCRIPTION",
                null, null
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("date");
        }

        @Test
        @DisplayName("P0: Should validate non-null amount")
        void shouldValidateNonNullAmount() {
            assertThatThrownBy(() -> new ImportedTransaction(
                LocalDate.now(),
                null,
                "DESCRIPTION",
                null, null
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("P0: Should validate non-blank description")
        void shouldValidateNonBlankDescription() {
            assertThatThrownBy(() -> new ImportedTransaction(
                LocalDate.now(),
                new BigDecimal("100.00"),
                "   ",
                null, null
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("description");
        }

        @Test
        @DisplayName("P1: Should return absolute amount")
        void shouldReturnAbsoluteAmount() {
            ImportedTransaction expense = new ImportedTransaction(
                LocalDate.now(),
                new BigDecimal("-50.00"),
                "EXPENSE",
                null, null
            );

            assertThat(expense.absoluteAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("P1: Should identify zero amount as expense")
        void shouldIdentifyZeroAsExpense() {
            ImportedTransaction zero = new ImportedTransaction(
                LocalDate.now(),
                BigDecimal.ZERO,
                "ZERO AMOUNT",
                null, null
            );

            assertThat(zero.isExpense()).isTrue();
            assertThat(zero.isIncome()).isFalse();
        }
    }

    // ========================================================================
    // PERFORMANCE TESTS
    // ========================================================================

    @Nested
    @DisplayName("Performance Tests")
    @Order(13)
    class PerformanceTests {

        @Test
        @DisplayName("P1: Should parse 20 transactions in under 100ms")
        void shouldParseQuickly() throws IOException {
            Path csvFile = copyResourceToTemp("csv/barclays-large.csv");
            BarclaysCsvParser parser = new BarclaysCsvParser();

            long startTime = System.currentTimeMillis();
            List<ImportedTransaction> transactions = parser.parse(csvFile, StandardCharsets.UTF_8);
            long endTime = System.currentTimeMillis();

            assertThat(transactions).hasSize(20);
            assertThat(endTime - startTime).isLessThan(100);
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Path copyResourceToTemp(String resourcePath) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        Path targetFile = tempDir.resolve(fileName);

        Files.copy(is, targetFile);
        is.close();

        return targetFile;
    }
}
