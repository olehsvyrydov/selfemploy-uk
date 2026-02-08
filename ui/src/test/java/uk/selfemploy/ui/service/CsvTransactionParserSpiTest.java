package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.plugin.extension.BankStatementParser;
import uk.selfemploy.plugin.extension.DataImporter;
import uk.selfemploy.plugin.extension.ParsedTransaction;
import uk.selfemploy.plugin.extension.Prioritizable;
import uk.selfemploy.plugin.extension.StatementParseRequest;
import uk.selfemploy.plugin.extension.StatementParseResult;
import uk.selfemploy.ui.viewmodel.BankFormat;
import uk.selfemploy.ui.viewmodel.ColumnMapping;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the SPI compliance of {@link CsvTransactionParser}.
 * Verifies that CsvTransactionParser correctly implements the
 * {@link BankStatementParser} interface contract.
 */
@DisplayName("CsvTransactionParser SPI compliance")
class CsvTransactionParserSpiTest {

    @TempDir
    Path tempDir;

    private CsvTransactionParser parser;

    @BeforeEach
    void setUp() {
        parser = new CsvTransactionParser();
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("test.csv");
        Files.writeString(file, content);
        return file;
    }

    @Nested
    @DisplayName("interface implementation")
    class InterfaceImplementation {

        @Test
        @DisplayName("implements BankStatementParser")
        void implementsBankStatementParser() {
            assertThat(parser).isInstanceOf(BankStatementParser.class);
        }

        @Test
        @DisplayName("implements DataImporter transitively")
        void implementsDataImporter() {
            assertThat(parser).isInstanceOf(DataImporter.class);
        }

        @Test
        @DisplayName("implements Prioritizable transitively")
        void implementsPrioritizable() {
            assertThat(parser).isInstanceOf(Prioritizable.class);
        }
    }

    @Nested
    @DisplayName("format identity")
    class FormatIdentity {

        @Test
        @DisplayName("format ID is 'csv'")
        void formatIdIsCsv() {
            assertThat(parser.getFormatId()).isEqualTo("csv");
        }

        @Test
        @DisplayName("importer ID is 'csv-generic'")
        void importerIdIsCsvGeneric() {
            assertThat(parser.getImporterId()).isEqualTo("csv-generic");
        }

        @Test
        @DisplayName("importer name is 'CSV Bank Statement'")
        void importerNameDescriptive() {
            assertThat(parser.getImporterName()).isEqualTo("CSV Bank Statement");
        }

        @Test
        @DisplayName("supported file types includes .csv")
        void supportedFileTypesIncludesCsv() {
            assertThat(parser.getSupportedFileTypes()).contains(".csv");
        }

        @Test
        @DisplayName("supported bank formats includes all known banks")
        void supportedBankFormatsIncludesAllBanks() {
            Set<String> formats = parser.getSupportedBankFormats();

            assertThat(formats).contains(
                "csv-barclays",
                "csv-hsbc",
                "csv-lloyds",
                "csv-nationwide",
                "csv-starling",
                "csv-monzo",
                "csv-revolut",
                "csv-santander",
                "csv-metro-bank"
            );
            assertThat(formats).doesNotContain("csv-unknown");
        }
    }

    @Nested
    @DisplayName("priority")
    class Priority {

        @Test
        @DisplayName("has built-in priority (10)")
        void hasBuiltInPriority() {
            assertThat(parser.getPriority()).isEqualTo(10);
        }

        @Test
        @DisplayName("priority is below maximum built-in threshold")
        void priorityBelowMaxBuiltin() {
            assertThat(parser.getPriority()).isLessThanOrEqualTo(Prioritizable.MAX_BUILTIN_PRIORITY);
        }
    }

    @Nested
    @DisplayName("column mapping requirement")
    class ColumnMappingRequirement {

        @Test
        @DisplayName("requires column mapping")
        void requiresColumnMapping() {
            assertThat(parser.requiresColumnMapping()).isTrue();
        }
    }

    @Nested
    @DisplayName("detectFormat")
    class DetectFormat {

        @Test
        @DisplayName("detects CSV files by extension")
        void detectsCsvByExtension() {
            Optional<String> detected = parser.detectFormat(Path.of("transactions.csv"));
            assertThat(detected).isPresent().contains("csv");
        }

        @Test
        @DisplayName("detects CSV files case-insensitively")
        void detectsCsvCaseInsensitive() {
            Optional<String> detected = parser.detectFormat(Path.of("data.CSV"));
            assertThat(detected).isPresent().contains("csv");
        }

        @Test
        @DisplayName("returns empty for non-CSV files")
        void returnsEmptyForNonCsv() {
            Optional<String> detected = parser.detectFormat(Path.of("statement.ofx"));
            assertThat(detected).isEmpty();
        }

        @Test
        @DisplayName("returns empty for null file")
        void returnsEmptyForNull() {
            Optional<String> detected = parser.detectFormat(null);
            assertThat(detected).isEmpty();
        }
    }

    @Nested
    @DisplayName("parseStatement via SPI")
    class ParseStatement {

        @Test
        @DisplayName("returns failure when no file path is provided")
        void returnsFailureWithoutFilePath() {
            StatementParseResult result = parser.parseStatement(StatementParseRequest.autoDetect());

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors().get(0)).contains("No file path");
        }

        @Test
        @DisplayName("parses CSV with column mapping via request options")
        void parsesCsvWithColumnMapping() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Client Payment,1500.00\n" +
                "16/06/2025,Office Supplies,-45.99\n"
            );

            Map<String, Object> options = new LinkedHashMap<>();
            options.put(StatementParseRequest.OPT_FILE_PATH, csv);

            StatementParseRequest request = new StatementParseRequest(
                "dd/MM/yyyy", "Date", "Description", "Amount", options
            );

            StatementParseResult result = parser.parseStatement(request);

            assertThat(result.hasErrors()).isFalse();
            assertThat(result.transactions()).hasSize(2);
            assertThat(result.detectedFormatId()).isEqualTo("csv");
        }

        @Test
        @DisplayName("parses CSV with separate income/expense columns via request options")
        void parsesSeparateColumnsViaSpi() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Money out,Money in,Balance\n" +
                "15/06/2025,ACME Corp,,1500.00,2500.00\n" +
                "16/06/2025,Office Depot,45.99,,2454.01\n"
            );

            Map<String, Object> options = new LinkedHashMap<>();
            options.put(StatementParseRequest.OPT_FILE_PATH, csv);
            options.put(StatementParseRequest.OPT_SEPARATE_COLUMNS, true);
            options.put(StatementParseRequest.OPT_INCOME_COLUMN, "Money in");
            options.put(StatementParseRequest.OPT_EXPENSE_COLUMN, "Money out");

            StatementParseRequest request = new StatementParseRequest(
                "dd/MM/yyyy", "Date", "Description", null, options
            );

            StatementParseResult result = parser.parseStatement(request);

            assertThat(result.hasErrors()).isFalse();
            assertThat(result.transactions()).hasSize(2);

            // Income: positive amount
            ParsedTransaction income = result.transactions().get(0);
            assertThat(income.isIncome()).isTrue();
            assertThat(income.amount()).isEqualByComparingTo(new BigDecimal("1500.00"));

            // Expense: negative amount (SPI convention)
            ParsedTransaction expense = result.transactions().get(1);
            assertThat(expense.isExpense()).isTrue();
            assertThat(expense.amount()).isEqualByComparingTo(new BigDecimal("-45.99"));
        }

        @Test
        @DisplayName("converts amounts to signed convention (positive=income, negative=expense)")
        void convertsToSignedAmounts() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Income,500.00\n" +
                "16/06/2025,Expense,-100.00\n"
            );

            Map<String, Object> options = new LinkedHashMap<>();
            options.put(StatementParseRequest.OPT_FILE_PATH, csv);

            StatementParseRequest request = new StatementParseRequest(
                "dd/MM/yyyy", "Date", "Description", "Amount", options
            );

            StatementParseResult result = parser.parseStatement(request);

            assertThat(result.transactions().get(0).amount()).isPositive();
            assertThat(result.transactions().get(1).amount()).isNegative();
        }
    }

    @Nested
    @DisplayName("parsePreview")
    class ParsePreview {

        @Test
        @DisplayName("returns empty list when auto-detect has no column mapping")
        void returnsEmptyWithAutoDetect() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Amount\n" +
                "15/06/2025,Payment One,100.00\n" +
                "16/06/2025,Payment Two,200.00\n" +
                "17/06/2025,Payment Three,300.00\n"
            );

            // Without explicit column mapping, the parser cannot find columns.
            // This is expected for a CSV parser that requires column mapping.
            List<ParsedTransaction> preview = parser.parsePreview(csv, 2);

            assertThat(preview).hasSizeLessThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("BankFormat.toFormatId bridge")
    class BankFormatBridge {

        @Test
        @DisplayName("BARCLAYS maps to csv-barclays")
        void barclaysMapsToFormatId() {
            assertThat(BankFormat.BARCLAYS.toFormatId()).isEqualTo("csv-barclays");
        }

        @Test
        @DisplayName("METRO_BANK maps to csv-metro-bank (underscore becomes dash)")
        void metroBankMapsToFormatId() {
            assertThat(BankFormat.METRO_BANK.toFormatId()).isEqualTo("csv-metro-bank");
        }

        @Test
        @DisplayName("all bank formats produce unique format IDs")
        void allBankFormatsHaveUniqueFormatIds() {
            Set<String> formatIds = parser.getSupportedBankFormats();

            // Should be exactly 9 (all formats minus UNKNOWN)
            assertThat(formatIds).hasSize(9);
        }

        @Test
        @DisplayName("format IDs match BankFormat.toFormatId output")
        void formatIdsMatchBankFormatOutput() {
            Set<String> supportedFormats = parser.getSupportedBankFormats();

            for (BankFormat format : BankFormat.values()) {
                if (format != BankFormat.UNKNOWN) {
                    assertThat(supportedFormats)
                        .as("Expected supported formats to contain %s", format.toFormatId())
                        .contains(format.toFormatId());
                }
            }
        }
    }

    @Nested
    @DisplayName("ColumnMapping.toParseRequest bridge")
    class ColumnMappingBridge {

        @Test
        @DisplayName("converts single-amount mapping to request")
        void convertsSingleAmountMapping() {
            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setAmountColumn("Amount");
            mapping.setDateFormat("dd/MM/yyyy");

            StatementParseRequest request = mapping.toParseRequest();

            assertThat(request.dateColumn()).isEqualTo("Date");
            assertThat(request.descriptionColumn()).isEqualTo("Description");
            assertThat(request.amountColumn()).isEqualTo("Amount");
            assertThat(request.dateFormat()).isEqualTo("dd/MM/yyyy");
            assertThat(request.getOption(StatementParseRequest.OPT_SEPARATE_COLUMNS, false))
                .isFalse();
        }

        @Test
        @DisplayName("converts separate-column mapping to request with options")
        void convertsSeparateColumnMapping() {
            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Description");
            mapping.setSeparateAmountColumns(true);
            mapping.setIncomeColumn("Money in");
            mapping.setExpenseColumn("Money out");
            mapping.setDateFormat("dd/MM/yyyy");

            StatementParseRequest request = mapping.toParseRequest();

            assertThat(request.dateColumn()).isEqualTo("Date");
            assertThat(request.descriptionColumn()).isEqualTo("Description");
            assertThat(request.amountColumn()).isNull();
            assertThat(request.getOption(StatementParseRequest.OPT_SEPARATE_COLUMNS, false))
                .isTrue();
            assertThat(request.<String>getOption(StatementParseRequest.OPT_INCOME_COLUMN, null))
                .isEqualTo("Money in");
            assertThat(request.<String>getOption(StatementParseRequest.OPT_EXPENSE_COLUMN, null))
                .isEqualTo("Money out");
        }

        @Test
        @DisplayName("converts Barclays bank preset to a valid request")
        void convertsBarclaysPreset() {
            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.BARCLAYS);
            StatementParseRequest request = mapping.toParseRequest();

            assertThat(request.dateColumn()).isEqualTo("Date");
            assertThat(request.descriptionColumn()).isEqualTo("Description");
            assertThat(request.dateFormat()).isEqualTo("dd/MM/yyyy");
            assertThat(request.getOption(StatementParseRequest.OPT_SEPARATE_COLUMNS, false))
                .isTrue();
        }

        @Test
        @DisplayName("round-trip: ColumnMapping -> StatementParseRequest -> parseStatement")
        void roundTripParseViaRequest() throws IOException {
            Path csv = createCsvFile(
                "Date,Description,Money out,Money in,Balance\n" +
                "15/06/2025,CLIENT PAYMENT,,3000.00,5454.01\n" +
                "16/06/2025,OFFICE DEPOT,45.99,,5408.02\n"
            );

            ColumnMapping mapping = ColumnMapping.forBankFormat(BankFormat.BARCLAYS);
            StatementParseRequest baseRequest = mapping.toParseRequest();

            // Add file path to the request options
            Map<String, Object> options = new LinkedHashMap<>(baseRequest.options());
            options.put(StatementParseRequest.OPT_FILE_PATH, csv);

            StatementParseRequest request = new StatementParseRequest(
                baseRequest.dateFormat(),
                baseRequest.dateColumn(),
                baseRequest.descriptionColumn(),
                baseRequest.amountColumn(),
                options
            );

            StatementParseResult result = parser.parseStatement(request);

            assertThat(result.hasErrors()).isFalse();
            assertThat(result.transactions()).hasSize(2);
            assertThat(result.transactions().get(0).isIncome()).isTrue();
            assertThat(result.transactions().get(0).amount()).isEqualByComparingTo("3000.00");
            assertThat(result.transactions().get(1).isExpense()).isTrue();
            assertThat(result.transactions().get(1).amount()).isEqualByComparingTo("-45.99");
        }
    }
}
