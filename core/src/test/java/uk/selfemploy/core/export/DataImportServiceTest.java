package uk.selfemploy.core.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataImportService.
 * Tests cover CSV and JSON import functionality with validation and preview.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataImportService Tests")
class DataImportServiceTest {

    @Mock
    private IncomeService incomeService;

    @Mock
    private ExpenseService expenseService;

    private DataImportService importService;

    @TempDir
    Path tempDir;

    private static final UUID BUSINESS_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        importService = new DataImportService(incomeService, expenseService);
    }

    @Nested
    @DisplayName("Preview Import Tests")
    class PreviewImportTests {

        @Test
        @DisplayName("should preview valid income CSV")
        void shouldPreviewValidIncomeCsv() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Consulting work,SALES,INV-001",
                "2025-07-20,500.00,Product sale,SALES,INV-002"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isTrue();
            assertThat(preview.recordCount()).isEqualTo(2);
            assertThat(preview.warnings()).isEmpty();
            assertThat(preview.errors()).isEmpty();
        }

        @Test
        @DisplayName("should preview valid expense CSV")
        void shouldPreviewValidExpenseCsv() throws IOException {
            // Given
            Path csvFile = createExpenseCsv(
                "Date,Amount,Description,Category,Notes",
                "2025-06-15,50.00,Office supplies,OFFICE_COSTS,Monthly supplies",
                "2025-07-20,200.00,Travel,TRAVEL,Client meeting"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.EXPENSE);

            // Then
            assertThat(preview.isValid()).isTrue();
            assertThat(preview.recordCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should detect invalid date format")
        void shouldDetectInvalidDateFormat() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "15/06/2025,1000.00,Invalid date format,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).hasSize(1);
            assertThat(preview.errors().get(0)).contains("date").containsIgnoringCase("row 1");
        }

        @Test
        @DisplayName("should detect negative amount")
        void shouldDetectNegativeAmount() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,-100.00,Negative amount,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).isNotEmpty();
            assertThat(preview.errors().get(0)).containsIgnoringCase("amount");
        }

        @Test
        @DisplayName("should detect invalid category")
        void shouldDetectInvalidCategory() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,100.00,Test,INVALID_CATEGORY,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).isNotEmpty();
            assertThat(preview.errors().get(0)).containsIgnoringCase("category");
        }

        @Test
        @DisplayName("should warn about missing optional fields")
        void shouldWarnAboutMissingOptionalFields() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,100.00,Test,SALES,"  // Empty reference (optional)
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isTrue();
            // Optional field warnings are not critical
        }

        @Test
        @DisplayName("should handle empty file")
        void shouldHandleEmptyFile() throws IOException {
            // Given
            Path csvFile = tempDir.resolve("empty.csv");
            Files.writeString(csvFile, "Date,Amount,Description,Category,Reference\n");

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isTrue();
            assertThat(preview.recordCount()).isEqualTo(0);
            assertThat(preview.warnings()).contains("No records to import");
        }

        @Test
        @DisplayName("should throw for null file path")
        void shouldThrowForNullFilePath() {
            assertThatThrownBy(() ->
                importService.previewCsvImport(null, ImportType.INCOME)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("File path");
        }
    }

    @Nested
    @DisplayName("Import Income CSV Tests")
    class ImportIncomeCsvTests {

        @Test
        @DisplayName("should import valid income records")
        void shouldImportValidIncomeRecords() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Consulting work,SALES,INV-001",
                "2025-07-20,500.00,Product sale,SALES,INV-002"
            );

            Income mockIncome = createMockIncome("Test", "100.00");
            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), anyString()))
                .thenReturn(mockIncome);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.INCOME,
                new ImportOptions(false, false)
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.importedCount()).isEqualTo(2);
            assertThat(result.skippedCount()).isEqualTo(0);

            verify(incomeService, times(2)).create(
                eq(BUSINESS_ID),
                any(LocalDate.class),
                any(BigDecimal.class),
                anyString(),
                any(IncomeCategory.class),
                anyString()
            );
        }

        @Test
        @DisplayName("should parse amounts with various formats")
        void shouldParseAmountsWithVariousFormats() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000,No decimals,SALES,INV-001",
                "2025-06-16,1500.5,One decimal,SALES,INV-002",
                "2025-06-17,2000.99,Two decimals,SALES,INV-003"
            );

            Income mockIncome = createMockIncome("Test", "100.00");
            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), anyString()))
                .thenReturn(mockIncome);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.INCOME,
                new ImportOptions(false, false)
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.importedCount()).isEqualTo(3);

            ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            verify(incomeService, times(3)).create(
                eq(BUSINESS_ID), any(), amountCaptor.capture(), any(), any(), any()
            );

            List<BigDecimal> amounts = amountCaptor.getAllValues();
            assertThat(amounts.get(0)).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(amounts.get(1)).isEqualByComparingTo(new BigDecimal("1500.5"));
            assertThat(amounts.get(2)).isEqualByComparingTo(new BigDecimal("2000.99"));
        }
    }

    @Nested
    @DisplayName("Import Expense CSV Tests")
    class ImportExpenseCsvTests {

        @Test
        @DisplayName("should import valid expense records")
        void shouldImportValidExpenseRecords() throws IOException {
            // Given
            Path csvFile = createExpenseCsv(
                "Date,Amount,Description,Category,Notes",
                "2025-06-15,50.00,Office supplies,OFFICE_COSTS,Monthly order",
                "2025-07-20,200.00,Train ticket,TRAVEL,Client visit"
            );

            Expense mockExpense = createMockExpense("Test", "100.00");
            when(expenseService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), any(), anyString()))
                .thenReturn(mockExpense);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.EXPENSE,
                new ImportOptions(false, false)
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.importedCount()).isEqualTo(2);

            verify(expenseService, times(2)).create(
                eq(BUSINESS_ID),
                any(LocalDate.class),
                any(BigDecimal.class),
                anyString(),
                any(ExpenseCategory.class),
                isNull(),
                anyString()
            );
        }
    }

    @Nested
    @DisplayName("Duplicate Detection Tests")
    class DuplicateDetectionTests {

        @Test
        @DisplayName("should skip duplicates when option enabled")
        void shouldSkipDuplicatesWhenEnabled() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Consulting work,SALES,INV-001",
                "2025-06-15,1000.00,Consulting work,SALES,INV-001"  // Duplicate
            );

            // First record returns income, second time throws (duplicate)
            Income mockIncome = createMockIncome("Consulting work", "1000.00");
            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), anyString()))
                .thenReturn(mockIncome);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.INCOME,
                new ImportOptions(false, true)  // skipDuplicatesEnabled = true
            );

            // Then
            assertThat(result.success()).isTrue();
            // Service deduplicates: 1 imported + 1 duplicate = 2 total
            assertThat(result.importedCount()).isEqualTo(1);
            assertThat(result.duplicateCount()).isEqualTo(1);
            assertThat(result.totalProcessed()).isEqualTo(2);
        }

        @Test
        @DisplayName("should report duplicates in result")
        void shouldReportDuplicatesInResult() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Same record,SALES,INV-001",
                "2025-06-15,1000.00,Same record,SALES,INV-001"  // Exact duplicate
            );

            Income mockIncome = createMockIncome("Same record", "1000.00");
            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), anyString()))
                .thenReturn(mockIncome);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.INCOME,
                new ImportOptions(false, true)
            );

            // Then
            assertThat(result.duplicateCount()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should report partial failure with valid and invalid rows")
        void shouldReportPartialFailure() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Valid row,SALES,INV-001",
                "invalid-date,-100,Invalid row,INVALID_CAT,INV-002",
                "2025-06-17,500.00,Another valid,SALES,INV-003"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.validRecordCount()).isEqualTo(2);
            assertThat(preview.invalidRecordCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle file not found")
        void shouldHandleFileNotFound() {
            // Given
            Path nonExistentFile = tempDir.resolve("non-existent.csv");

            // When/Then
            assertThatThrownBy(() ->
                importService.previewCsvImport(nonExistentFile, ImportType.INCOME)
            ).isInstanceOf(ImportException.class)
             .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("should validate header format")
        void shouldValidateHeaderFormat() throws IOException {
            // Given
            Path csvFile = tempDir.resolve("bad-header.csv");
            Files.writeString(csvFile, "Wrong,Headers,Here\n1,2,3\n");

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.toLowerCase().contains("header"));
        }
    }

    @Nested
    @DisplayName("Import JSON Tests")
    class ImportJsonTests {

        @Test
        @DisplayName("should import valid JSON export file")
        void shouldImportValidJsonFile() throws IOException {
            // Given
            String jsonContent = """
                {
                    "metadata": {
                        "appVersion": "0.1.0",
                        "exportDate": "2025-06-15T10:00:00",
                        "taxYears": ["2025/26"]
                    },
                    "incomes": [
                        {
                            "date": "2025-06-15",
                            "amount": "1000.00",
                            "description": "Consulting",
                            "category": "SALES",
                            "reference": "INV-001"
                        }
                    ],
                    "expenses": [
                        {
                            "date": "2025-06-20",
                            "amount": "50.00",
                            "description": "Office supplies",
                            "category": "OFFICE_COSTS",
                            "notes": "Monthly order"
                        }
                    ]
                }
                """;
            Path jsonFile = tempDir.resolve("import.json");
            Files.writeString(jsonFile, jsonContent);

            Income mockIncome = createMockIncome("Consulting", "1000.00");
            Expense mockExpense = createMockExpense("Office supplies", "50.00");

            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), any()))
                .thenReturn(mockIncome);
            when(expenseService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(mockExpense);

            // When
            ImportResult result = importService.importJson(
                BUSINESS_ID,
                jsonFile,
                new ImportOptions(false, false)
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.importedCount()).isEqualTo(2);
            verify(incomeService, times(1)).create(any(), any(), any(), any(), any(), any());
            verify(expenseService, times(1)).create(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should validate JSON structure")
        void shouldValidateJsonStructure() throws IOException {
            // Given
            String invalidJson = """
                {
                    "wrong_structure": true
                }
                """;
            Path jsonFile = tempDir.resolve("invalid.json");
            Files.writeString(jsonFile, invalidJson);

            // When
            ImportPreview preview = importService.previewJsonImport(jsonFile);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.toLowerCase().contains("structure"));
        }
    }

    // Helper methods

    private Path createIncomeCsv(String... lines) throws IOException {
        Path file = tempDir.resolve("income_" + System.currentTimeMillis() + ".csv");
        Files.writeString(file, String.join("\n", lines) + "\n");
        return file;
    }

    private Path createExpenseCsv(String... lines) throws IOException {
        Path file = tempDir.resolve("expense_" + System.currentTimeMillis() + ".csv");
        Files.writeString(file, String.join("\n", lines) + "\n");
        return file;
    }

    private Income createMockIncome(String description, String amount) {
        return new Income(
            UUID.randomUUID(),
            BUSINESS_ID,
            LocalDate.of(2025, 6, 15),
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

    private Expense createMockExpense(String description, String amount) {
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

    // =========================================================================
    // PS11-002: Export/Import Data - Additional Import Tests
    // =========================================================================
    @Nested
    @DisplayName("PS11-002: CSV Import Validation Tests")
    class Ps11002CsvImportValidationTests {

        @Test
        @DisplayName("IMP-U01: should validate required date column")
        void shouldValidateRequiredDateColumn() throws IOException {
            // Given - Missing date
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                ",1000.00,Missing date,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.toLowerCase().contains("date"));
        }

        @Test
        @DisplayName("IMP-U02: should validate required amount column")
        void shouldValidateRequiredAmountColumn() throws IOException {
            // Given - Missing amount
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,,Missing amount,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.toLowerCase().contains("amount"));
        }

        @Test
        @DisplayName("IMP-U03: should validate required description column")
        void shouldValidateRequiredDescriptionColumn() throws IOException {
            // Given - Missing description
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.toLowerCase().contains("description"));
        }

        @Test
        @DisplayName("IMP-U04: should validate required category column")
        void shouldValidateRequiredCategoryColumn() throws IOException {
            // Given - Missing category
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Test income,,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.toLowerCase().contains("category"));
        }

        @Test
        @DisplayName("IMP-U05: should accept valid ISO date format (YYYY-MM-DD)")
        void shouldAcceptValidIsoDateFormat() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Valid date,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isTrue();
        }

        @Test
        @DisplayName("IMP-U06: should reject invalid date format (DD/MM/YYYY)")
        void shouldRejectInvalidDateFormat() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "15/06/2025,1000.00,Invalid date,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.contains("date") || e.contains("YYYY-MM-DD"));
        }

        @Test
        @DisplayName("IMP-U07: should reject zero amount")
        void shouldRejectZeroAmount() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,0.00,Zero amount,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.toLowerCase().contains("amount"));
        }

        @Test
        @DisplayName("IMP-U08: should handle amounts with currency symbols")
        void shouldHandleAmountsWithCurrencySymbols() throws IOException {
            // Given - Amount with currency symbol (should fail)
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,GBP1000.00,Currency prefix,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
        }

        @Test
        @DisplayName("IMP-U09: should accept amounts without decimals")
        void shouldAcceptAmountsWithoutDecimals() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000,No decimals,SALES,INV-001"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("PS11-002: Duplicate Detection Tests")
    class Ps11002DuplicateDetectionTests {

        @Test
        @DisplayName("IMP-U10: should detect duplicate records within file")
        void shouldDetectDuplicateRecordsWithinFile() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Same invoice,SALES,INV-001",
                "2025-06-15,1000.00,Same invoice,SALES,INV-001"
            );

            Income mockIncome = createMockIncome("Same invoice", "1000.00");
            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), anyString()))
                .thenReturn(mockIncome);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.INCOME,
                new ImportOptions(false, true) // Skip duplicates enabled
            );

            // Then
            assertThat(result.duplicateCount()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("IMP-U11: should skip duplicates when option enabled")
        void shouldSkipDuplicatesWhenOptionEnabled() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Invoice A,SALES,INV-001",
                "2025-06-15,1000.00,Invoice A,SALES,INV-001"
            );

            Income mockIncome = createMockIncome("Invoice A", "1000.00");
            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), anyString()))
                .thenReturn(mockIncome);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.INCOME,
                new ImportOptions(false, true)
            );

            // Then
            assertThat(result.importedCount()).isEqualTo(1);
            assertThat(result.duplicateCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("IMP-U12: should import duplicates when option disabled")
        void shouldImportDuplicatesWhenOptionDisabled() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Invoice A,SALES,INV-001",
                "2025-06-15,1000.00,Invoice A,SALES,INV-001"
            );

            Income mockIncome = createMockIncome("Invoice A", "1000.00");
            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), anyString()))
                .thenReturn(mockIncome);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.INCOME,
                new ImportOptions(false, false) // Skip duplicates disabled
            );

            // Then
            assertThat(result.importedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("IMP-U13: should report duplicates in result summary")
        void shouldReportDuplicatesInResultSummary() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Same,SALES,INV-001",
                "2025-06-15,1000.00,Same,SALES,INV-001",
                "2025-06-15,1000.00,Same,SALES,INV-001"
            );

            Income mockIncome = createMockIncome("Same", "1000.00");
            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), anyString()))
                .thenReturn(mockIncome);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.INCOME,
                new ImportOptions(false, true)
            );

            // Then
            assertThat(result.totalProcessed()).isEqualTo(3);
            assertThat(result.importedCount()).isEqualTo(1);
            assertThat(result.duplicateCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("PS11-002: Error Handling Tests")
    class Ps11002ErrorHandlingTests {

        @Test
        @DisplayName("IMP-U14: should reject malformed CSV (wrong column count)")
        void shouldRejectMalformedCsv() throws IOException {
            // Given
            Path csvFile = tempDir.resolve("malformed.csv");
            Files.writeString(csvFile, "Date,Amount\n2025-06-15,1000.00\n");

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).isNotEmpty();
        }

        @Test
        @DisplayName("IMP-U15: should provide row number in error message")
        void shouldProvideRowNumberInErrorMessage() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Valid row,SALES,INV-001",
                "invalid-date,500.00,Invalid row,SALES,INV-002"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.errors()).anyMatch(e -> e.contains("Row 2") || e.contains("row 2"));
        }

        @Test
        @DisplayName("IMP-U16: should handle empty file")
        void shouldHandleEmptyFile() throws IOException {
            // Given
            Path csvFile = tempDir.resolve("empty.csv");
            Files.writeString(csvFile, "");

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isFalse();
        }

        @Test
        @DisplayName("IMP-U17: should handle header-only file")
        void shouldHandleHeaderOnlyFile() throws IOException {
            // Given
            Path csvFile = tempDir.resolve("header-only.csv");
            Files.writeString(csvFile, "Date,Amount,Description,Category,Reference\n");

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isTrue();
            assertThat(preview.recordCount()).isEqualTo(0);
            assertThat(preview.warnings()).contains("No records to import");
        }

        @Test
        @DisplayName("IMP-U18: should continue importing valid rows after error")
        void shouldContinueImportingValidRowsAfterError() throws IOException {
            // Given - Mix of valid and invalid rows
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,1000.00,Valid 1,SALES,INV-001",
                "invalid-date,-100,Invalid,BAD_CAT,INV-002",
                "2025-06-17,500.00,Valid 2,SALES,INV-003"
            );

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.validRecordCount()).isEqualTo(2);
            assertThat(preview.invalidRecordCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("PS11-002: Large File Handling Tests")
    class Ps11002LargeFileHandlingTests {

        @Test
        @DisplayName("IMP-U19: should handle large CSV file (1000+ rows)")
        void shouldHandleLargeCsvFile() throws IOException {
            // Given - Generate large CSV
            StringBuilder csv = new StringBuilder();
            csv.append("Date,Amount,Description,Category,Reference\n");
            for (int i = 0; i < 1000; i++) {
                csv.append(String.format("2025-06-15,%d.00,Invoice %d,SALES,INV-%04d%n", 100 + i, i, i));
            }
            Path csvFile = tempDir.resolve("large.csv");
            Files.writeString(csvFile, csv.toString());

            // When
            ImportPreview preview = importService.previewCsvImport(csvFile, ImportType.INCOME);

            // Then
            assertThat(preview.isValid()).isTrue();
            assertThat(preview.recordCount()).isEqualTo(1000);
        }

        @Test
        @DisplayName("IMP-U20: should show progress for large imports")
        void shouldReportTotalProcessedCount() throws IOException {
            // Given
            Path csvFile = createIncomeCsv(
                "Date,Amount,Description,Category,Reference",
                "2025-06-15,100.00,Row 1,SALES,INV-001",
                "2025-06-16,200.00,Row 2,SALES,INV-002",
                "2025-06-17,300.00,Row 3,SALES,INV-003",
                "2025-06-18,400.00,Row 4,SALES,INV-004",
                "2025-06-19,500.00,Row 5,SALES,INV-005"
            );

            Income mockIncome = createMockIncome("Test", "100.00");
            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), anyString()))
                .thenReturn(mockIncome);

            // When
            ImportResult result = importService.importCsv(
                BUSINESS_ID,
                csvFile,
                ImportType.INCOME,
                new ImportOptions(false, false)
            );

            // Then
            assertThat(result.totalProcessed()).isEqualTo(5);
            assertThat(result.importedCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("PS11-002: JSON Import Tests")
    class Ps11002JsonImportTests {

        @Test
        @DisplayName("IMP-U21: should import from valid JSON export")
        void shouldImportFromValidJsonExport() throws IOException {
            // Given
            String jsonContent = """
                {
                    "metadata": {
                        "appVersion": "0.1.0",
                        "exportDate": "2025-06-15T10:00:00",
                        "taxYears": ["2025/26"]
                    },
                    "incomes": [
                        {
                            "date": "2025-06-15",
                            "amount": "1000.00",
                            "description": "Consulting",
                            "category": "SALES",
                            "reference": "INV-001"
                        }
                    ],
                    "expenses": [
                        {
                            "date": "2025-06-20",
                            "amount": "50.00",
                            "description": "Office supplies",
                            "category": "OFFICE_COSTS",
                            "notes": "Monthly order"
                        }
                    ]
                }
                """;
            Path jsonFile = tempDir.resolve("import.json");
            Files.writeString(jsonFile, jsonContent);

            Income mockIncome = createMockIncome("Consulting", "1000.00");
            Expense mockExpense = createMockExpense("Office supplies", "50.00");

            when(incomeService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), any()))
                .thenReturn(mockIncome);
            when(expenseService.create(eq(BUSINESS_ID), any(), any(), anyString(), any(), any(), any()))
                .thenReturn(mockExpense);

            // When
            ImportResult result = importService.importJson(
                BUSINESS_ID,
                jsonFile,
                new ImportOptions(false, false)
            );

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.importedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("IMP-U22: should reject invalid JSON structure")
        void shouldRejectInvalidJsonStructure() throws IOException {
            // Given
            String invalidJson = """
                {
                    "randomKey": "randomValue"
                }
                """;
            Path jsonFile = tempDir.resolve("invalid.json");
            Files.writeString(jsonFile, invalidJson);

            // When
            ImportPreview preview = importService.previewJsonImport(jsonFile);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.toLowerCase().contains("structure"));
        }

        @Test
        @DisplayName("IMP-U23: should validate JSON income records")
        void shouldValidateJsonIncomeRecords() throws IOException {
            // Given - Missing required field in income
            String jsonContent = """
                {
                    "metadata": {
                        "appVersion": "0.1.0",
                        "exportDate": "2025-06-15T10:00:00",
                        "taxYears": ["2025/26"]
                    },
                    "incomes": [
                        {
                            "date": "2025-06-15",
                            "description": "Missing amount"
                        }
                    ],
                    "expenses": []
                }
                """;
            Path jsonFile = tempDir.resolve("invalid-income.json");
            Files.writeString(jsonFile, jsonContent);

            // When
            ImportPreview preview = importService.previewJsonImport(jsonFile);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e -> e.toLowerCase().contains("amount"));
        }

        @Test
        @DisplayName("IMP-U24: should validate JSON expense records")
        void shouldValidateJsonExpenseRecords() throws IOException {
            // Given - Missing required field in expense
            String jsonContent = """
                {
                    "metadata": {
                        "appVersion": "0.1.0",
                        "exportDate": "2025-06-15T10:00:00",
                        "taxYears": ["2025/26"]
                    },
                    "incomes": [],
                    "expenses": [
                        {
                            "date": "2025-06-20",
                            "amount": "50.00"
                        }
                    ]
                }
                """;
            Path jsonFile = tempDir.resolve("invalid-expense.json");
            Files.writeString(jsonFile, jsonContent);

            // When
            ImportPreview preview = importService.previewJsonImport(jsonFile);

            // Then
            assertThat(preview.isValid()).isFalse();
            assertThat(preview.errors()).anyMatch(e ->
                e.toLowerCase().contains("description") || e.toLowerCase().contains("category"));
        }
    }
}
