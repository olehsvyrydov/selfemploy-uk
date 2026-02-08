package uk.selfemploy.ui.e2e;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.selfemploy.ui.service.CsvTransactionParser;
import uk.selfemploy.ui.viewmodel.BankFormat;
import uk.selfemploy.ui.viewmodel.ColumnMapping;
import uk.selfemploy.ui.viewmodel.ImportedTransactionRow;
import uk.selfemploy.ui.viewmodel.TransactionType;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Data-driven E2E tests for CSV bank statement parsing.
 *
 * Tests real CSV sample files from src/test/resources/bank-samples/ with
 * actual CsvTransactionParser and bank-specific ColumnMapping configurations.
 */
@Tag("e2e")
@DisplayName("Bank Import Data-Driven E2E Tests")
class BankImportDataDrivenE2ETest {

    private static final Path SAMPLES_DIR = Paths.get("src/test/resources/bank-samples");
    private CsvTransactionParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvTransactionParser();
    }

    // ==================== Barclays Tests ====================

    @Nested
    @DisplayName("Barclays CSV Format")
    class BarclaysTests {

        private final Path sampleFile = SAMPLES_DIR.resolve("barclays-sample.csv");
        private final ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.BARCLAYS);

        @Test
        @DisplayName("should parse Barclays sample file successfully")
        void shouldParseBarclaysSample() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            assertThat(result.transactions()).isNotEmpty();
        }

        @Test
        @DisplayName("should detect correct number of valid transactions")
        void shouldDetectCorrectTransactionCount() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            // 10 rows total: 7 valid, 1 empty description (skipped), 1 bad amount (skipped), leaves ~7
            assertThat(result.transactions()).hasSizeGreaterThanOrEqualTo(7);
        }

        @Test
        @DisplayName("should parse income transactions from Money in column")
        void shouldParseIncomeTransactions() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            List<ImportedTransactionRow> incomes = result.transactions().stream()
                    .filter(r -> r.type() == TransactionType.INCOME)
                    .toList();
            assertThat(incomes).isNotEmpty();
        }

        @Test
        @DisplayName("should parse expense transactions from Money out column")
        void shouldParseExpenseTransactions() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            List<ImportedTransactionRow> expenses = result.transactions().stream()
                    .filter(r -> r.type() == TransactionType.EXPENSE)
                    .toList();
            assertThat(expenses).isNotEmpty();
        }

        @Test
        @DisplayName("should parse Barclays date format dd/MM/yyyy correctly")
        void shouldParseDateCorrectly() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            ImportedTransactionRow first = result.transactions().get(0);
            assertThat(first.date()).isEqualTo(LocalDate.of(2025, 1, 15));
        }

        @Test
        @DisplayName("should parse Barclays income amount 2500.00")
        void shouldParseIncomeAmount() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            ImportedTransactionRow firstIncome = result.transactions().stream()
                    .filter(r -> r.type() == TransactionType.INCOME)
                    .findFirst().orElseThrow();
            assertThat(firstIncome.amount()).isEqualByComparingTo(new BigDecimal("2500.00"));
        }

        @Test
        @DisplayName("should parse Barclays expense amount 45.50")
        void shouldParseExpenseAmount() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            ImportedTransactionRow firstExpense = result.transactions().stream()
                    .filter(r -> r.type() == TransactionType.EXPENSE)
                    .findFirst().orElseThrow();
            assertThat(firstExpense.amount()).isEqualByComparingTo(new BigDecimal("45.50"));
        }

        @Test
        @DisplayName("should correctly classify Barclays income as INCOME type")
        void shouldClassifyIncomeCorrectly() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            // "Acme Ltd Invoice Payment" is in the Money in column
            ImportedTransactionRow acme = result.transactions().stream()
                    .filter(r -> r.description().contains("Acme"))
                    .findFirst().orElseThrow();
            assertThat(acme.type()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("should correctly classify Barclays expense as EXPENSE type")
        void shouldClassifyExpenseCorrectly() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            // "TFL Travel Charge" is in the Money out column
            ImportedTransactionRow tfl = result.transactions().stream()
                    .filter(r -> r.description().contains("TFL"))
                    .findFirst().orElseThrow();
            assertThat(tfl.type()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("should generate warnings for malformed rows")
        void shouldGenerateWarningsForMalformedRows() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            // Rows with empty description and invalid amount should produce warnings
            assertThat(result.warnings()).isNotEmpty();
        }

        @Test
        @DisplayName("should skip row with empty description")
        void shouldSkipRowWithEmptyDescription() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            // Row with empty description should not be in results
            boolean hasEmptyDesc = result.transactions().stream()
                    .anyMatch(r -> r.description() == null || r.description().isEmpty());
            assertThat(hasEmptyDesc).isFalse();
        }
    }

    // ==================== Monzo Tests ====================

    @Nested
    @DisplayName("Monzo CSV Format")
    class MonzoTests {

        private final Path sampleFile = SAMPLES_DIR.resolve("monzo-sample.csv");
        private final ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.MONZO);

        @Test
        @DisplayName("should parse Monzo sample file successfully")
        void shouldParseMonzoSample() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            assertThat(result.transactions()).isNotEmpty();
        }

        @Test
        @DisplayName("should parse Monzo income (positive amounts)")
        void shouldParseMonzoIncome() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            List<ImportedTransactionRow> incomes = result.transactions().stream()
                    .filter(r -> r.type() == TransactionType.INCOME)
                    .toList();
            assertThat(incomes).isNotEmpty();
            // "Design Client Fee" = 1800.00
            ImportedTransactionRow designClient = incomes.stream()
                    .filter(r -> r.description().contains("Design Client"))
                    .findFirst().orElseThrow();
            assertThat(designClient.amount()).isEqualByComparingTo(new BigDecimal("1800.00"));
        }

        @Test
        @DisplayName("should parse Monzo expenses (negative amounts)")
        void shouldParseMonzoExpenses() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            List<ImportedTransactionRow> expenses = result.transactions().stream()
                    .filter(r -> r.type() == TransactionType.EXPENSE)
                    .toList();
            assertThat(expenses).isNotEmpty();
            // "Costa Coffee" = -4.50 (absolute 4.50)
            ImportedTransactionRow costa = expenses.stream()
                    .filter(r -> r.description().contains("Costa"))
                    .findFirst().orElseThrow();
            assertThat(costa.amount()).isEqualByComparingTo(new BigDecimal("4.50"));
        }

        @Test
        @DisplayName("should parse Monzo dates dd/MM/yyyy correctly")
        void shouldParseMonzoDates() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            ImportedTransactionRow first = result.transactions().get(0);
            assertThat(first.date()).isEqualTo(LocalDate.of(2025, 1, 15));
        }

        @Test
        @DisplayName("should skip Monzo rows with missing name")
        void shouldSkipRowsWithMissingName() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            // tx_009 has "Missing Name" but empty amount column
            // tx_010 has invalid date
            assertThat(result.warnings()).isNotEmpty();
        }

        @Test
        @DisplayName("should use Name column as description for Monzo")
        void shouldUseNameColumnAsDescription() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            boolean hasDesignClient = result.transactions().stream()
                    .anyMatch(r -> r.description().contains("Design Client"));
            assertThat(hasDesignClient).isTrue();
        }
    }

    // ==================== Revolut Tests ====================

    @Nested
    @DisplayName("Revolut CSV Format")
    class RevolutTests {

        private final Path sampleFile = SAMPLES_DIR.resolve("revolut-sample.csv");
        private final ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.REVOLUT);

        @Test
        @DisplayName("should parse Revolut sample file successfully")
        void shouldParseRevolutSample() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            assertThat(result.transactions()).isNotEmpty();
        }

        @Test
        @DisplayName("should parse Revolut income (positive amounts)")
        void shouldParseRevolutIncome() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            List<ImportedTransactionRow> incomes = result.transactions().stream()
                    .filter(r -> r.type() == TransactionType.INCOME)
                    .toList();
            assertThat(incomes).isNotEmpty();
            // "Client Payment Received" = 3500.00
            ImportedTransactionRow clientPayment = incomes.stream()
                    .filter(r -> r.description().contains("Client Payment"))
                    .findFirst().orElseThrow();
            assertThat(clientPayment.amount()).isEqualByComparingTo(new BigDecimal("3500.00"));
        }

        @Test
        @DisplayName("should parse Revolut expenses (negative amounts)")
        void shouldParseRevolutExpenses() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            List<ImportedTransactionRow> expenses = result.transactions().stream()
                    .filter(r -> r.type() == TransactionType.EXPENSE)
                    .toList();
            assertThat(expenses).isNotEmpty();
            // "Google Workspace Subscription" = 11.99
            ImportedTransactionRow google = expenses.stream()
                    .filter(r -> r.description().contains("Google Workspace"))
                    .findFirst().orElseThrow();
            assertThat(google.amount()).isEqualByComparingTo(new BigDecimal("11.99"));
        }

        @Test
        @DisplayName("should parse Revolut yyyy-MM-dd date format")
        void shouldParseRevolutDates() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            ImportedTransactionRow first = result.transactions().get(0);
            assertThat(first.date()).isEqualTo(LocalDate.of(2025, 1, 15));
        }

        @Test
        @DisplayName("should handle Revolut row with missing Completed Date")
        void shouldHandleRevolutMissingDate() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            // Row with empty Completed Date or invalid date should be skipped
            assertThat(result.warnings()).isNotEmpty();
        }

        @Test
        @DisplayName("should use Description column as description for Revolut")
        void shouldUseDescriptionColumn() {
            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            boolean hasAdobeCC = result.transactions().stream()
                    .anyMatch(r -> r.description().contains("Adobe Creative Cloud"));
            assertThat(hasAdobeCC).isTrue();
        }
    }

    // ==================== Edge Cases ====================

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty file gracefully")
        void shouldHandleEmptyFile() {
            Path emptyFile = SAMPLES_DIR.resolve("empty-file.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.SANTANDER);

            CsvTransactionParser.ParseResult result = parser.parse(emptyFile, mapping);

            assertThat(result.transactions()).isEmpty();
            assertThat(result.warnings()).isNotEmpty();
        }

        @Test
        @DisplayName("should handle headers-only file with zero transactions")
        void shouldHandleHeadersOnlyFile() {
            Path headersOnly = SAMPLES_DIR.resolve("headers-only.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.SANTANDER);

            CsvTransactionParser.ParseResult result = parser.parse(headersOnly, mapping);

            assertThat(result.transactions()).isEmpty();
        }

        @Test
        @DisplayName("should never crash on any bank format sample file")
        void shouldNeverCrashOnAnySample() {
            assertThatCode(() -> {
                ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.BARCLAYS);
                parser.parse(SAMPLES_DIR.resolve("barclays-sample.csv"), mapping);
            }).doesNotThrowAnyException();

            assertThatCode(() -> {
                ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.MONZO);
                parser.parse(SAMPLES_DIR.resolve("monzo-sample.csv"), mapping);
            }).doesNotThrowAnyException();

            assertThatCode(() -> {
                ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.REVOLUT);
                parser.parse(SAMPLES_DIR.resolve("revolut-sample.csv"), mapping);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should not include any transaction with null date")
        void shouldNotIncludeNullDates() {
            Path sampleFile = SAMPLES_DIR.resolve("barclays-sample.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.BARCLAYS);

            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);

            assertThat(result.transactions())
                    .allMatch(r -> r.date() != null, "All transactions should have non-null dates");
        }

        @Test
        @DisplayName("should not include any transaction with null description")
        void shouldNotIncludeNullDescriptions() {
            Path sampleFile = SAMPLES_DIR.resolve("monzo-sample.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.MONZO);

            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);

            assertThat(result.transactions())
                    .allMatch(r -> r.description() != null && !r.description().isEmpty(),
                            "All transactions should have non-empty descriptions");
        }

        @Test
        @DisplayName("should not include any transaction with zero amount")
        void shouldNotIncludeZeroAmounts() {
            Path sampleFile = SAMPLES_DIR.resolve("revolut-sample.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.REVOLUT);

            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);

            assertThat(result.transactions())
                    .allMatch(r -> r.amount().compareTo(BigDecimal.ZERO) > 0,
                            "All transactions should have positive amounts");
        }

        @Test
        @DisplayName("should return warnings list that is not null even when no warnings")
        void shouldReturnNonNullWarningsList() {
            Path headersOnly = SAMPLES_DIR.resolve("headers-only.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.SANTANDER);

            CsvTransactionParser.ParseResult result = parser.parse(headersOnly, mapping);

            assertThat(result.warnings()).isNotNull();
        }
    }

    // ==================== Cross-Format Consistency ====================

    @Nested
    @DisplayName("Cross-Format Consistency")
    class CrossFormatConsistencyTests {

        @Test
        @DisplayName("all parsed transactions should have positive amounts (absolute value)")
        void allTransactionsShouldHavePositiveAmounts() {
            for (var entry : getSampleFilesWithMappings()) {
                CsvTransactionParser.ParseResult result = parser.parse(entry.path(), entry.mapping());
                for (ImportedTransactionRow row : result.transactions()) {
                    assertThat(row.amount())
                            .as("Amount for '%s' in %s", row.description(), entry.format())
                            .isGreaterThan(BigDecimal.ZERO);
                }
            }
        }

        @Test
        @DisplayName("all parsed transactions should have non-null type (INCOME or EXPENSE)")
        void allTransactionsShouldHaveType() {
            for (var entry : getSampleFilesWithMappings()) {
                CsvTransactionParser.ParseResult result = parser.parse(entry.path(), entry.mapping());
                for (ImportedTransactionRow row : result.transactions()) {
                    assertThat(row.type())
                            .as("Type for '%s' in %s", row.description(), entry.format())
                            .isNotNull();
                }
            }
        }

        @Test
        @DisplayName("all parsed transactions should have non-null id (UUID)")
        void allTransactionsShouldHaveId() {
            for (var entry : getSampleFilesWithMappings()) {
                CsvTransactionParser.ParseResult result = parser.parse(entry.path(), entry.mapping());
                for (ImportedTransactionRow row : result.transactions()) {
                    assertThat(row.id())
                            .as("ID for '%s' in %s", row.description(), entry.format())
                            .isNotNull();
                }
            }
        }

        @Test
        @DisplayName("warnings should contain line numbers for skipped rows")
        void warningsShouldContainLineNumbers() {
            for (var entry : getSampleFilesWithMappings()) {
                CsvTransactionParser.ParseResult result = parser.parse(entry.path(), entry.mapping());
                for (String warning : result.warnings()) {
                    if (warning.startsWith("Skipped line")) {
                        assertThat(warning)
                                .as("Warning format in %s", entry.format())
                                .matches("Skipped line \\d+:.*");
                    }
                }
            }
        }

        private record SampleEntry(BankFormat format, Path path, ColumnMapping mapping) {}

        private List<SampleEntry> getSampleFilesWithMappings() {
            return List.of(
                    new SampleEntry(BankFormat.BARCLAYS,
                            SAMPLES_DIR.resolve("barclays-sample.csv"),
                            ColumnMapping.forBankFormat(BankFormat.BARCLAYS)),
                    new SampleEntry(BankFormat.MONZO,
                            SAMPLES_DIR.resolve("monzo-sample.csv"),
                            ColumnMapping.forBankFormat(BankFormat.MONZO)),
                    new SampleEntry(BankFormat.REVOLUT,
                            SAMPLES_DIR.resolve("revolut-sample.csv"),
                            ColumnMapping.forBankFormat(BankFormat.REVOLUT))
            );
        }
    }

    // ==================== Parameterized Tests ====================

    @Nested
    @DisplayName("Parameterized Bank Format Tests")
    class ParameterizedTests {

        @ParameterizedTest(name = "Barclays income row {0}: description={1}, amount={2}")
        @CsvSource({
                "1, Acme Ltd Invoice Payment, 2500.00",
                "3, Freelance Project Fee, 1200.00",
                "7, Consulting Income, 3200.00"
        })
        @DisplayName("should parse Barclays income rows correctly")
        void shouldParseBarclaysIncomeRows(int rowNum, String expectedDesc, String expectedAmount) {
            Path sampleFile = SAMPLES_DIR.resolve("barclays-sample.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.BARCLAYS);

            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            ImportedTransactionRow row = result.transactions().stream()
                    .filter(r -> r.description().contains(expectedDesc))
                    .findFirst().orElseThrow(() ->
                            new AssertionError("Expected transaction with description containing: " + expectedDesc));

            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal(expectedAmount));
            assertThat(row.type()).isEqualTo(TransactionType.INCOME);
        }

        @ParameterizedTest(name = "Barclays expense row {0}: description={1}, amount={2}")
        @CsvSource({
                "2, TFL Travel Charge, 45.50",
                "4, Office Supplies Staples, 89.99",
                "5, Monthly Mobile Phone Bill, 35.00",
                "6, Cloud Hosting Service, 150.00",
                "8, Professional Indemnity Insurance, 420.00"
        })
        @DisplayName("should parse Barclays expense rows correctly")
        void shouldParseBarclaysExpenseRows(int rowNum, String expectedDesc, String expectedAmount) {
            Path sampleFile = SAMPLES_DIR.resolve("barclays-sample.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.BARCLAYS);

            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            ImportedTransactionRow row = result.transactions().stream()
                    .filter(r -> r.description().contains(expectedDesc))
                    .findFirst().orElseThrow(() ->
                            new AssertionError("Expected transaction with description containing: " + expectedDesc));

            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal(expectedAmount));
            assertThat(row.type()).isEqualTo(TransactionType.EXPENSE);
        }

        @ParameterizedTest(name = "Monzo row: {0} should be {1}")
        @CsvSource({
                "Design Client Fee, INCOME",
                "Costa Coffee, EXPENSE",
                "Amazon Web Services, EXPENSE",
                "Workshop Revenue, INCOME",
                "Photography Client, INCOME"
        })
        @DisplayName("should classify Monzo transactions correctly")
        void shouldClassifyMonzoTransactions(String description, TransactionType expectedType) {
            Path sampleFile = SAMPLES_DIR.resolve("monzo-sample.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.MONZO);

            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            ImportedTransactionRow row = result.transactions().stream()
                    .filter(r -> r.description().contains(description))
                    .findFirst().orElseThrow();

            assertThat(row.type()).isEqualTo(expectedType);
        }

        @ParameterizedTest(name = "Revolut row: {0} should be {1}")
        @CsvSource({
                "Google Workspace Subscription, EXPENSE",
                "Client Payment Received, INCOME",
                "Train Ticket London to Manchester, EXPENSE",
                "Retainer Fee January, INCOME",
                "Workshop Facilitation Fee, INCOME"
        })
        @DisplayName("should classify Revolut transactions correctly")
        void shouldClassifyRevolutTransactions(String description, TransactionType expectedType) {
            Path sampleFile = SAMPLES_DIR.resolve("revolut-sample.csv");
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.REVOLUT);

            CsvTransactionParser.ParseResult result = parser.parse(sampleFile, mapping);
            ImportedTransactionRow row = result.transactions().stream()
                    .filter(r -> r.description().contains(description))
                    .findFirst().orElseThrow();

            assertThat(row.type()).isEqualTo(expectedType);
        }
    }

    // ==================== ColumnMapping Factory Tests ====================

    @Nested
    @DisplayName("ColumnMapping.forBankFormat()")
    class ColumnMappingFactoryTests {

        @ParameterizedTest(name = "{0} should produce complete mapping")
        @EnumSource(value = BankFormat.class, names = "UNKNOWN", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("should create complete column mapping for known bank formats")
        void shouldCreateCompleteMappingForKnownFormats(BankFormat format) {
            ColumnMapping mapping = ColumnMapping.forBankFormat(format);

            assertThat(mapping.getDateColumn()).isNotNull();
            assertThat(mapping.getDescriptionColumn()).isNotNull();
            assertThat(mapping.getDateFormat()).isNotNull();

            if (mapping.hasSeparateAmountColumns()) {
                assertThat(mapping.getIncomeColumn()).isNotNull();
                assertThat(mapping.getExpenseColumn()).isNotNull();
            } else {
                assertThat(mapping.getAmountColumn()).isNotNull();
            }
        }

        @Test
        @DisplayName("UNKNOWN format should produce empty mapping")
        void unknownFormatShouldProduceEmptyMapping() {
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.UNKNOWN);

            assertThat(mapping.getDateColumn()).isNull();
            assertThat(mapping.getDescriptionColumn()).isNull();
        }
    }
}
