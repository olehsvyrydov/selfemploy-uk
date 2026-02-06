package uk.selfemploy.core.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DataExportService.
 * Tests cover JSON and CSV export functionality with various options.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataExportService Tests")
class DataExportServiceTest {

    @Mock
    private IncomeService incomeService;

    @Mock
    private ExpenseService expenseService;

    private DataExportService exportService;

    @TempDir
    Path tempDir;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final TaxYear TAX_YEAR_2025 = TaxYear.of(2025);

    @BeforeEach
    void setUp() {
        exportService = new DataExportService(incomeService, expenseService);
    }

    @Nested
    @DisplayName("Export to JSON Tests")
    class ExportToJsonTests {

        @Test
        @DisplayName("should export income and expenses to JSON file")
        void shouldExportToJson() throws IOException {
            // Given
            List<Income> incomes = List.of(createIncome("Invoice 1", "1000.00"));
            List<Expense> expenses = List.of(createExpense("Office supplies", "50.00"));

            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(incomes);
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(expenses);

            Path outputFile = tempDir.resolve("export.json");

            // When
            ExportResult result = exportService.exportToJson(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.filePath()).isEqualTo(outputFile);
            assertThat(result.incomeCount()).isEqualTo(1);
            assertThat(result.expenseCount()).isEqualTo(1);
            assertThat(Files.exists(outputFile)).isTrue();

            String content = Files.readString(outputFile);
            assertThat(content).contains("\"incomes\"");
            assertThat(content).contains("\"expenses\"");
            assertThat(content).contains("Invoice 1");
            assertThat(content).contains("Office supplies");
        }

        @Test
        @DisplayName("should include metadata in JSON export")
        void shouldIncludeMetadata() throws IOException {
            // Given
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("export.json");

            // When
            exportService.exportToJson(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            assertThat(content).contains("\"metadata\"");
            assertThat(content).contains("\"appVersion\"");
            assertThat(content).contains("\"exportDate\"");
            assertThat(content).contains("\"taxYears\"");
        }

        @Test
        @DisplayName("should export multiple tax years")
        void shouldExportMultipleTaxYears() throws IOException {
            // Given
            TaxYear taxYear2024 = TaxYear.of(2024);
            List<Income> incomes2024 = List.of(createIncome("Invoice 2024", "500.00"));
            List<Income> incomes2025 = List.of(createIncome("Invoice 2025", "1000.00"));

            when(incomeService.findByTaxYear(BUSINESS_ID, taxYear2024))
                .thenReturn(incomes2024);
            when(incomeService.findByTaxYear(BUSINESS_ID, TAX_YEAR_2025))
                .thenReturn(incomes2025);
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("export.json");

            // When
            ExportResult result = exportService.exportToJson(
                BUSINESS_ID,
                new TaxYear[]{taxYear2024, TAX_YEAR_2025},
                outputFile
            );

            // Then
            assertThat(result.incomeCount()).isEqualTo(2);
            String content = Files.readString(outputFile);
            assertThat(content).contains("Invoice 2024");
            assertThat(content).contains("Invoice 2025");
        }

        @Test
        @DisplayName("should throw exception for null business ID")
        void shouldThrowForNullBusinessId() {
            Path outputFile = tempDir.resolve("export.json");

            assertThatThrownBy(() ->
                exportService.exportToJson(null, new TaxYear[]{TAX_YEAR_2025}, outputFile)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("Business ID");
        }

        @Test
        @DisplayName("should throw exception for empty tax years")
        void shouldThrowForEmptyTaxYears() {
            Path outputFile = tempDir.resolve("export.json");

            assertThatThrownBy(() ->
                exportService.exportToJson(BUSINESS_ID, new TaxYear[]{}, outputFile)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("tax year");
        }
    }

    @Nested
    @DisplayName("Export to CSV Tests")
    class ExportToCsvTests {

        @Test
        @DisplayName("should export income to CSV")
        void shouldExportIncomeToCsv() throws IOException {
            // Given
            List<Income> incomes = List.of(
                createIncome("Consulting work", "1500.00"),
                createIncome("Product sales", "2000.00")
            );
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(incomes);

            Path outputFile = tempDir.resolve("income.csv");

            // When
            ExportResult result = exportService.exportIncomeToCsv(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.incomeCount()).isEqualTo(2);

            String content = Files.readString(outputFile);
            assertThat(content).contains("Date,Amount,Description,Category,Reference");
            assertThat(content).contains("Consulting work");
            assertThat(content).contains("1500.00");
        }

        @Test
        @DisplayName("should export expenses to CSV")
        void shouldExportExpensesToCsv() throws IOException {
            // Given
            List<Expense> expenses = List.of(
                createExpense("Office rent", "800.00"),
                createExpense("Internet bill", "50.00")
            );
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(expenses);

            Path outputFile = tempDir.resolve("expenses.csv");

            // When
            ExportResult result = exportService.exportExpensesToCsv(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.expenseCount()).isEqualTo(2);

            String content = Files.readString(outputFile);
            assertThat(content).contains("Date,Amount,Description,Category,SA103 Box,Notes");
            assertThat(content).contains("Office rent");
            assertThat(content).contains("800.00");
        }

        @Test
        @DisplayName("should export combined report with totals")
        void shouldExportCombinedReportWithTotals() throws IOException {
            // Given
            List<Income> incomes = List.of(
                createIncome("Sales", "5000.00")
            );
            List<Expense> expenses = List.of(
                createExpense("Expenses", "1000.00")
            );

            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(incomes);
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(expenses);
            when(incomeService.getTotalByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(new BigDecimal("5000.00"));
            when(expenseService.getTotalByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(new BigDecimal("1000.00"));

            Path outputDir = tempDir;

            // When
            CombinedExportResult result = exportService.exportCombinedReport(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputDir
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.incomeFilePath()).isNotNull();
            assertThat(result.expenseFilePath()).isNotNull();
            assertThat(result.summaryFilePath()).isNotNull();
            assertThat(Files.exists(result.summaryFilePath())).isTrue();

            String summaryContent = Files.readString(result.summaryFilePath());
            assertThat(summaryContent).contains("Total Income");
            assertThat(summaryContent).contains("Total Expenses");
            assertThat(summaryContent).contains("Net Profit");
        }

        @Test
        @DisplayName("should handle special characters in CSV")
        void shouldHandleSpecialCharactersInCsv() throws IOException {
            // Given
            List<Income> incomes = List.of(
                createIncome("Invoice with \"quotes\" and, comma", "100.00")
            );
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(incomes);

            Path outputFile = tempDir.resolve("income.csv");

            // When
            exportService.exportIncomeToCsv(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            // CSV should properly escape quotes and handle commas
            assertThat(content).contains("\"Invoice with \"\"quotes\"\" and, comma\"");
        }

        @Test
        @DisplayName("should include SA103 box mapping in expense CSV")
        void shouldIncludeSa103BoxInExpenseCsv() throws IOException {
            // Given
            Expense expense = new Expense(
                UUID.randomUUID(),
                BUSINESS_ID,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("200.00"),
                "Travel expense",
                ExpenseCategory.TRAVEL,
                null,
                "Business trip",
                null,
                null,
                null,
                null
            );
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of(expense));

            Path outputFile = tempDir.resolve("expenses.csv");

            // When
            exportService.exportExpensesToCsv(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            assertThat(content).contains("20"); // SA103 Box 20 for Travel
        }
    }

    @Nested
    @DisplayName("Export Options Tests")
    class ExportOptionsTests {

        @Test
        @DisplayName("should filter by date range")
        void shouldFilterByDateRange() throws IOException {
            // Given
            LocalDate startDate = LocalDate.of(2025, 6, 1);
            LocalDate endDate = LocalDate.of(2025, 8, 31);

            Income incomeInRange = createIncomeWithDate("In range", "100.00", LocalDate.of(2025, 7, 15));
            Income incomeOutOfRange = createIncomeWithDate("Out of range", "200.00", LocalDate.of(2025, 10, 15));

            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of(incomeInRange, incomeOutOfRange));
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("export.json");

            // When
            ExportOptions options = new ExportOptions(startDate, endDate);
            ExportResult result = exportService.exportToJson(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile,
                options
            );

            // Then
            assertThat(result.incomeCount()).isEqualTo(1);
            String content = Files.readString(outputFile);
            assertThat(content).contains("In range");
            assertThat(content).doesNotContain("Out of range");
        }

        @Test
        @DisplayName("should handle empty result gracefully")
        void shouldHandleEmptyResult() throws IOException {
            // Given
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("export.json");

            // When
            ExportResult result = exportService.exportToJson(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.incomeCount()).isEqualTo(0);
            assertThat(result.expenseCount()).isEqualTo(0);
        }
    }

    // =========================================================================
    // PS11-002: Export/Import Data - Additional CSV Export Tests
    // =========================================================================
    @Nested
    @DisplayName("PS11-002: CSV Export Tests")
    class Ps11002CsvExportTests {

        @Test
        @DisplayName("EXP-U01: should export income records to CSV")
        void shouldExportIncomeRecordsToCsv() throws IOException {
            // Given
            List<Income> incomes = List.of(
                createIncome("Consulting services", "1500.00"),
                createIncome("Product sales", "2000.00")
            );
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(incomes);

            Path outputFile = tempDir.resolve("income.csv");

            // When
            ExportResult result = exportService.exportIncomeToCsv(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.incomeCount()).isEqualTo(2);

            String content = Files.readString(outputFile);
            assertThat(content).contains("Consulting services");
            assertThat(content).contains("Product sales");
            assertThat(content).contains("1500.00");
            assertThat(content).contains("2000.00");
        }

        @Test
        @DisplayName("EXP-U02: should export expense records to CSV with categories")
        void shouldExportExpenseRecordsToCsv() throws IOException {
            // Given
            List<Expense> expenses = List.of(
                createExpense("Office supplies", "50.00"),
                createExpenseWithCategory("Train ticket", "200.00", ExpenseCategory.TRAVEL)
            );
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(expenses);

            Path outputFile = tempDir.resolve("expenses.csv");

            // When
            ExportResult result = exportService.exportExpensesToCsv(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.expenseCount()).isEqualTo(2);

            String content = Files.readString(outputFile);
            assertThat(content).contains("Office supplies");
            assertThat(content).contains("Train ticket");
            // Category uses getDisplayName() which returns full description
            assertThat(content).containsIgnoringCase("stationery"); // Part of "Phone, stationery and office costs"
            assertThat(content).containsIgnoringCase("travel");
        }

        @Test
        @DisplayName("EXP-U03: should include header row in CSV")
        void shouldIncludeHeaderRowInCsv() throws IOException {
            // Given
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of(createIncome("Test", "100.00")));

            Path outputFile = tempDir.resolve("income.csv");

            // When
            exportService.exportIncomeToCsv(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            String[] lines = content.split("\n");
            assertThat(lines[0]).isEqualTo("Date,Amount,Description,Category,Reference");
        }

        @Test
        @DisplayName("EXP-U04: should handle commas in descriptions")
        void shouldHandleCommasInDescriptions() throws IOException {
            // Given
            List<Income> incomes = List.of(
                createIncome("Consulting, coaching, and training", "1000.00")
            );
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(incomes);

            Path outputFile = tempDir.resolve("income.csv");

            // When
            exportService.exportIncomeToCsv(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            // Field should be properly quoted
            assertThat(content).contains("\"Consulting, coaching, and training\"");
        }

        @Test
        @DisplayName("EXP-U05: should handle quotes in descriptions")
        void shouldHandleQuotesInDescriptions() throws IOException {
            // Given
            List<Income> incomes = List.of(
                createIncome("Invoice for \"special project\"", "500.00")
            );
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(incomes);

            Path outputFile = tempDir.resolve("income.csv");

            // When
            exportService.exportIncomeToCsv(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            // Quotes should be escaped by doubling
            assertThat(content).contains("\"\"special project\"\"");
        }

        @Test
        @DisplayName("EXP-U06: should handle newlines in descriptions")
        void shouldHandleNewlinesInDescriptions() throws IOException {
            // Given
            List<Income> incomes = List.of(
                createIncome("Line 1\nLine 2", "300.00")
            );
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(incomes);

            Path outputFile = tempDir.resolve("income.csv");

            // When
            exportService.exportIncomeToCsv(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            // Field with newline should be quoted
            assertThat(content).contains("\"Line 1\nLine 2\"");
        }

        @Test
        @DisplayName("EXP-U07: should export empty file when no records (header only)")
        void shouldExportEmptyFileWhenNoRecords() throws IOException {
            // Given
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("income.csv");

            // When
            ExportResult result = exportService.exportIncomeToCsv(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.incomeCount()).isEqualTo(0);

            String content = Files.readString(outputFile);
            String[] lines = content.trim().split("\n");
            assertThat(lines).hasSize(1); // Header only
            assertThat(lines[0]).contains("Date,Amount,Description");
        }

        @Test
        @DisplayName("EXP-U08: should filter by tax year")
        void shouldFilterByTaxYear() throws IOException {
            // Given
            List<Income> incomes2025 = List.of(createIncome("2025 income", "1000.00"));

            when(incomeService.findByTaxYear(BUSINESS_ID, TAX_YEAR_2025))
                .thenReturn(incomes2025);

            Path outputFile = tempDir.resolve("income.csv");

            // When - Export only 2025/26 tax year
            ExportResult result = exportService.exportIncomeToCsv(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile
            );

            // Then - Only 2025/26 records should be exported
            assertThat(result.incomeCount()).isEqualTo(1);
            String content = Files.readString(outputFile);
            assertThat(content).contains("2025 income");
        }

        @Test
        @DisplayName("EXP-U09: should filter by date range within tax year")
        void shouldFilterByDateRangeWithinTaxYear() throws IOException {
            // Given
            LocalDate startDate = LocalDate.of(2025, 4, 6);
            LocalDate endDate = LocalDate.of(2025, 6, 30);

            Income aprilIncome = createIncomeWithDate("April income", "100.00", LocalDate.of(2025, 4, 15));
            Income julyIncome = createIncomeWithDate("July income", "200.00", LocalDate.of(2025, 7, 15));

            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of(aprilIncome, julyIncome));
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("export.json");

            // When
            ExportOptions options = new ExportOptions(startDate, endDate);
            ExportResult result = exportService.exportToJson(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile,
                options
            );

            // Then
            assertThat(result.incomeCount()).isEqualTo(1);
            String content = Files.readString(outputFile);
            assertThat(content).contains("April income");
            assertThat(content).doesNotContain("July income");
        }
    }

    // =========================================================================
    // PS11-002: JSON Export Tests
    // =========================================================================
    @Nested
    @DisplayName("PS11-002: JSON Export Tests")
    class Ps11002JsonExportTests {

        @Test
        @DisplayName("EXP-U10: should export to valid JSON format")
        void shouldExportToValidJsonFormat() throws IOException {
            // Given
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of(createIncome("Test", "100.00")));
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("export.json");

            // When
            ExportResult result = exportService.exportToJson(
                BUSINESS_ID,
                new TaxYear[]{TAX_YEAR_2025},
                outputFile
            );

            // Then
            assertThat(result.success()).isTrue();

            // Verify valid JSON by checking structure
            String content = Files.readString(outputFile);
            assertThat(content).startsWith("{");
            assertThat(content).endsWith("}");
            assertThat(content).contains("\"metadata\"");
            assertThat(content).contains("\"incomes\"");
            assertThat(content).contains("\"expenses\"");
        }

        @Test
        @DisplayName("EXP-U11: should include metadata in JSON (version, exportDate, taxYearRange)")
        void shouldIncludeMetadataInJson() throws IOException {
            // Given
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("export.json");

            // When
            exportService.exportToJson(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            assertThat(content).contains("\"appVersion\"");
            assertThat(content).contains("\"exportDate\"");
            assertThat(content).contains("\"taxYears\"");
        }

        @Test
        @DisplayName("EXP-U12: should include app version in metadata")
        void shouldIncludeAppVersionInMetadata() throws IOException {
            // Given
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("export.json");

            // When
            exportService.exportToJson(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            // App version should be present in metadata
            assertThat(content).contains("\"appVersion\" : \"0.1.0\"");
        }

        @Test
        @DisplayName("EXP-U13: should include income array in JSON")
        void shouldIncludeIncomeArrayInJson() throws IOException {
            // Given
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of(createIncome("Test income", "500.00")));
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());

            Path outputFile = tempDir.resolve("export.json");

            // When
            exportService.exportToJson(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            assertThat(content).contains("\"incomes\" : [");
            assertThat(content).contains("Test income");
        }

        @Test
        @DisplayName("EXP-U14: should include expense array in JSON")
        void shouldIncludeExpenseArrayInJson() throws IOException {
            // Given
            when(incomeService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(BUSINESS_ID), any(TaxYear.class)))
                .thenReturn(List.of(createExpense("Test expense", "100.00")));

            Path outputFile = tempDir.resolve("export.json");

            // When
            exportService.exportToJson(BUSINESS_ID, new TaxYear[]{TAX_YEAR_2025}, outputFile);

            // Then
            String content = Files.readString(outputFile);
            assertThat(content).contains("\"expenses\" : [");
            assertThat(content).contains("Test expense");
        }
    }

    // Helper methods

    private Income createIncome(String description, String amount) {
        return createIncomeWithDate(description, amount, LocalDate.of(2025, 6, 15));
    }

    private Income createIncomeWithDate(String description, String amount, LocalDate date) {
        return new Income(
            UUID.randomUUID(),
            BUSINESS_ID,
            date,
            new BigDecimal(amount),
            description,
            IncomeCategory.SALES,
            "REF-001",
            null,
            null,
            null,
            null
        );
    }

    private Expense createExpense(String description, String amount) {
        return new Expense(
            UUID.randomUUID(),
            BUSINESS_ID,
            LocalDate.of(2025, 6, 15),
            new BigDecimal(amount),
            description,
            ExpenseCategory.OFFICE_COSTS,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private Expense createExpenseWithCategory(String description, String amount, ExpenseCategory category) {
        return new Expense(
            UUID.randomUUID(),
            BUSINESS_ID,
            LocalDate.of(2025, 6, 15),
            new BigDecimal(amount),
            description,
            category,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
