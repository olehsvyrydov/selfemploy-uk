package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.ColumnMapping;
import uk.selfemploy.ui.viewmodel.ImportedTransactionRow;
import uk.selfemploy.ui.viewmodel.TransactionType;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ImportOrchestrationService.
 *
 * Validates CSV file loading, parsing delegation, and transaction import
 * with correct routing to IncomeService and ExpenseService.
 */
@DisplayName("ImportOrchestrationService Tests")
class ImportOrchestrationServiceTest {

    private CsvTransactionParser csvParser;
    private IncomeService incomeService;
    private ExpenseService expenseService;
    private UUID businessId;
    private ImportOrchestrationService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        csvParser = mock(CsvTransactionParser.class);
        incomeService = mock(IncomeService.class);
        expenseService = mock(ExpenseService.class);
        businessId = UUID.randomUUID();
        service = new ImportOrchestrationService(csvParser, incomeService, expenseService, businessId);
    }

    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorTests {

        @Test
        @DisplayName("should create service with valid dependencies")
        void shouldCreateServiceWithValidDependencies() {
            var svc = new ImportOrchestrationService(csvParser, incomeService, expenseService, businessId);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("should accept null csvParser without throwing at construction time")
        void shouldAcceptNullCsvParser() {
            // Constructor does not validate nulls - NullPointerException thrown at usage
            var svc = new ImportOrchestrationService(null, incomeService, expenseService, businessId);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("should accept null incomeService without throwing at construction time")
        void shouldAcceptNullIncomeService() {
            var svc = new ImportOrchestrationService(csvParser, null, expenseService, businessId);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("should accept null expenseService without throwing at construction time")
        void shouldAcceptNullExpenseService() {
            var svc = new ImportOrchestrationService(csvParser, incomeService, null, businessId);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("should accept null businessId without throwing at construction time")
        void shouldAcceptNullBusinessId() {
            var svc = new ImportOrchestrationService(csvParser, incomeService, expenseService, null);
            assertThat(svc).isNotNull();
        }
    }

    @Nested
    @DisplayName("loadFile()")
    class LoadFileTests {

        @Test
        @DisplayName("should load CSV file and return headers and row count")
        void shouldLoadCsvFileAndReturnHeadersAndRowCount() throws IOException {
            Path csvFile = tempDir.resolve("test.csv");
            Files.writeString(csvFile, "Date,Description,Amount\n15/01/2025,Payment,100.00\n20/01/2025,Fee,200.00\n");

            ImportOrchestrationService.FileLoadResult result = service.loadFile(csvFile);

            assertThat(result.headers()).containsExactly("Date", "Description", "Amount");
            assertThat(result.rowCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty headers and zero rows for empty file")
        void shouldReturnEmptyForEmptyFile() throws IOException {
            Path csvFile = tempDir.resolve("empty.csv");
            Files.writeString(csvFile, "");

            ImportOrchestrationService.FileLoadResult result = service.loadFile(csvFile);

            assertThat(result.headers()).isEmpty();
            assertThat(result.rowCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return headers with zero rows for headers-only file")
        void shouldReturnHeadersWithZeroRowsForHeadersOnlyFile() throws IOException {
            Path csvFile = tempDir.resolve("headers.csv");
            Files.writeString(csvFile, "Date,Description,Amount\n");

            ImportOrchestrationService.FileLoadResult result = service.loadFile(csvFile);

            assertThat(result.headers()).containsExactly("Date", "Description", "Amount");
            assertThat(result.rowCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw IOException for non-existent file")
        void shouldThrowForNonExistentFile() {
            Path noFile = tempDir.resolve("nonexistent.csv");
            assertThatThrownBy(() -> service.loadFile(noFile))
                    .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("should trim header whitespace")
        void shouldTrimHeaderWhitespace() throws IOException {
            Path csvFile = tempDir.resolve("whitespace.csv");
            Files.writeString(csvFile, "  Date , Description , Amount \n15/01/2025,Payment,100.00\n");

            ImportOrchestrationService.FileLoadResult result = service.loadFile(csvFile);

            assertThat(result.headers()).containsExactly("Date", "Description", "Amount");
        }

        @Test
        @DisplayName("should return immutable headers list")
        void shouldReturnImmutableHeadersList() throws IOException {
            Path csvFile = tempDir.resolve("immutable.csv");
            Files.writeString(csvFile, "Date,Amount\n15/01/2025,100.00\n");

            ImportOrchestrationService.FileLoadResult result = service.loadFile(csvFile);

            assertThatThrownBy(() -> result.headers().add("extra"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("parseTransactions()")
    class ParseTransactionsTests {

        @Test
        @DisplayName("should delegate to CsvTransactionParser")
        void shouldDelegateToCsvTransactionParser() {
            Path csvFile = tempDir.resolve("test.csv");
            ColumnMapping mapping = new ColumnMapping();
            CsvTransactionParser.ParseResult expectedResult =
                    new CsvTransactionParser.ParseResult(List.of(), List.of());
            when(csvParser.parse(csvFile, mapping)).thenReturn(expectedResult);

            CsvTransactionParser.ParseResult result = service.parseTransactions(csvFile, mapping);

            assertThat(result).isSameAs(expectedResult);
            verify(csvParser).parse(csvFile, mapping);
        }

        @Test
        @DisplayName("should pass column mapping to parser")
        void shouldPassColumnMappingToParser() {
            Path csvFile = tempDir.resolve("test.csv");
            ColumnMapping mapping = new ColumnMapping();
            mapping.setDateColumn("Date");
            mapping.setDescriptionColumn("Desc");
            mapping.setAmountColumn("Amt");
            mapping.setDateFormat("dd/MM/yyyy");
            when(csvParser.parse(any(), any())).thenReturn(
                    new CsvTransactionParser.ParseResult(List.of(), List.of()));

            service.parseTransactions(csvFile, mapping);

            ArgumentCaptor<ColumnMapping> captor = ArgumentCaptor.forClass(ColumnMapping.class);
            verify(csvParser).parse(eq(csvFile), captor.capture());
            assertThat(captor.getValue().getDateColumn()).isEqualTo("Date");
        }
    }

    @Nested
    @DisplayName("importTransactions()")
    class ImportTransactionsTests {

        @Test
        @DisplayName("should save income transactions via IncomeService")
        void shouldSaveIncomeViaIncomeService() {
            ImportedTransactionRow incomeRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Client Payment", new BigDecimal("2500.00"),
                    TransactionType.INCOME, null, false, 0);

            service.importTransactions(List.of(incomeRow), null);

            verify(incomeService).create(
                    eq(businessId),
                    eq(LocalDate.of(2025, 1, 15)),
                    any(BigDecimal.class),
                    eq("Client Payment"),
                    eq(IncomeCategory.SALES),
                    isNull()
            );
            verifyNoInteractions(expenseService);
        }

        @Test
        @DisplayName("should save expense transactions via ExpenseService")
        void shouldSaveExpenseViaExpenseService() {
            ImportedTransactionRow expenseRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 17), "Office Supplies", new BigDecimal("89.99"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 0);

            service.importTransactions(List.of(expenseRow), null);

            verify(expenseService).create(
                    eq(businessId),
                    eq(LocalDate.of(2025, 1, 17)),
                    any(BigDecimal.class),
                    eq("Office Supplies"),
                    eq(ExpenseCategory.OFFICE_COSTS),
                    isNull(),
                    isNull()
            );
            verifyNoInteractions(incomeService);
        }

        @Test
        @DisplayName("should return correct imported count for mixed transactions")
        void shouldReturnCorrectImportedCount() {
            List<ImportedTransactionRow> transactions = List.of(
                    ImportedTransactionRow.create(
                            LocalDate.of(2025, 1, 15), "Income 1", new BigDecimal("100.00"),
                            TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(
                            LocalDate.of(2025, 1, 16), "Expense 1", new BigDecimal("50.00"),
                            TransactionType.EXPENSE, ExpenseCategory.TRAVEL, false, 0),
                    ImportedTransactionRow.create(
                            LocalDate.of(2025, 1, 17), "Income 2", new BigDecimal("200.00"),
                            TransactionType.INCOME, null, false, 0)
            );

            ImportOrchestrationService.ImportResult result = service.importTransactions(transactions, null);

            assertThat(result.importedCount()).isEqualTo(3);
            assertThat(result.errorCount()).isEqualTo(0);
            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("should count errors when IncomeService throws")
        void shouldCountErrorsWhenIncomeServiceThrows() {
            when(incomeService.create(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Save failed"));

            ImportedTransactionRow incomeRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Failed Income", new BigDecimal("100.00"),
                    TransactionType.INCOME, null, false, 0);

            ImportOrchestrationService.ImportResult result = service.importTransactions(List.of(incomeRow), null);

            assertThat(result.importedCount()).isEqualTo(0);
            assertThat(result.errorCount()).isEqualTo(1);
            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("should count errors when ExpenseService throws")
        void shouldCountErrorsWhenExpenseServiceThrows() {
            when(expenseService.create(any(), any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Save failed"));

            ImportedTransactionRow expenseRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 17), "Failed Expense", new BigDecimal("50.00"),
                    TransactionType.EXPENSE, ExpenseCategory.TRAVEL, false, 0);

            ImportOrchestrationService.ImportResult result = service.importTransactions(List.of(expenseRow), null);

            assertThat(result.importedCount()).isEqualTo(0);
            assertThat(result.errorCount()).isEqualTo(1);
            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("should continue importing after individual error")
        void shouldContinueImportingAfterError() {
            // First call succeeds, second fails, third succeeds
            when(incomeService.create(any(), any(), any(), any(), any(), any()))
                    .thenReturn(null)
                    .thenThrow(new RuntimeException("Save failed"))
                    .thenReturn(null);

            List<ImportedTransactionRow> transactions = List.of(
                    ImportedTransactionRow.create(
                            LocalDate.of(2025, 1, 15), "OK 1", new BigDecimal("100.00"),
                            TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(
                            LocalDate.of(2025, 1, 16), "FAIL", new BigDecimal("200.00"),
                            TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(
                            LocalDate.of(2025, 1, 17), "OK 2", new BigDecimal("300.00"),
                            TransactionType.INCOME, null, false, 0)
            );

            ImportOrchestrationService.ImportResult result = service.importTransactions(transactions, null);

            assertThat(result.importedCount()).isEqualTo(2);
            assertThat(result.errorCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should invoke progress callback with correct values")
        void shouldInvokeProgressCallback() {
            List<Double> progressValues = new ArrayList<>();
            Consumer<Double> callback = progressValues::add;

            List<ImportedTransactionRow> transactions = List.of(
                    ImportedTransactionRow.create(
                            LocalDate.of(2025, 1, 15), "T1", new BigDecimal("100.00"),
                            TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(
                            LocalDate.of(2025, 1, 16), "T2", new BigDecimal("200.00"),
                            TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(
                            LocalDate.of(2025, 1, 17), "T3", new BigDecimal("300.00"),
                            TransactionType.INCOME, null, false, 0)
            );

            service.importTransactions(transactions, callback);

            assertThat(progressValues).hasSize(3);
            assertThat(progressValues.get(0)).isCloseTo(1.0 / 3.0, org.assertj.core.data.Offset.offset(0.001));
            assertThat(progressValues.get(1)).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(0.001));
            assertThat(progressValues.get(2)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("should handle null progress callback without error")
        void shouldHandleNullProgressCallback() {
            ImportedTransactionRow row = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Test", new BigDecimal("100.00"),
                    TransactionType.INCOME, null, false, 0);

            ImportOrchestrationService.ImportResult result = service.importTransactions(List.of(row), null);

            assertThat(result.importedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return zero counts for empty transaction list")
        void shouldReturnZeroCountsForEmptyList() {
            ImportOrchestrationService.ImportResult result = service.importTransactions(List.of(), null);

            assertThat(result.importedCount()).isEqualTo(0);
            assertThat(result.errorCount()).isEqualTo(0);
            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("should use SALES category for income transactions")
        void shouldUseSalesCategoryForIncome() {
            ImportedTransactionRow incomeRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Income", new BigDecimal("500.00"),
                    TransactionType.INCOME, null, false, 0);

            service.importTransactions(List.of(incomeRow), null);

            verify(incomeService).create(
                    any(), any(), any(), any(),
                    eq(IncomeCategory.SALES),
                    isNull()
            );
        }

        @Test
        @DisplayName("should use row category for expense transactions")
        void shouldUseRowCategoryForExpense() {
            ImportedTransactionRow expenseRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 17), "Travel", new BigDecimal("45.00"),
                    TransactionType.EXPENSE, ExpenseCategory.TRAVEL, false, 0);

            service.importTransactions(List.of(expenseRow), null);

            verify(expenseService).create(
                    any(), any(), any(), any(),
                    eq(ExpenseCategory.TRAVEL),
                    isNull(), isNull()
            );
        }

        @Test
        @DisplayName("should pass null category for uncategorized expense")
        void shouldPassNullCategoryForUncategorizedExpense() {
            ImportedTransactionRow expenseRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 17), "Unknown", new BigDecimal("10.00"),
                    TransactionType.EXPENSE, null, false, 0);

            service.importTransactions(List.of(expenseRow), null);

            verify(expenseService).create(
                    any(), any(), any(), any(),
                    isNull(),
                    isNull(), isNull()
            );
        }

        @Test
        @DisplayName("should invoke progress callback even on error")
        void shouldInvokeProgressCallbackEvenOnError() {
            when(incomeService.create(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Save failed"));

            AtomicReference<Double> lastProgress = new AtomicReference<>(0.0);

            ImportedTransactionRow row = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Fail", new BigDecimal("100.00"),
                    TransactionType.INCOME, null, false, 0);

            service.importTransactions(List.of(row), lastProgress::set);

            assertThat(lastProgress.get()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("ImportResult record")
    class ImportResultTests {

        @Test
        @DisplayName("hasErrors should return true when errorCount > 0")
        void hasErrorsShouldReturnTrueWhenErrors() {
            var result = new ImportOrchestrationService.ImportResult(5, 2);
            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("hasErrors should return false when errorCount is 0")
        void hasErrorsShouldReturnFalseWhenNoErrors() {
            var result = new ImportOrchestrationService.ImportResult(5, 0);
            assertThat(result.hasErrors()).isFalse();
        }
    }

    @Nested
    @DisplayName("FileLoadResult record")
    class FileLoadResultTests {

        @Test
        @DisplayName("should create defensive copy of headers")
        void shouldCreateDefensiveCopyOfHeaders() {
            List<String> mutableHeaders = new ArrayList<>(List.of("Date", "Amount"));
            var result = new ImportOrchestrationService.FileLoadResult(mutableHeaders, 5);

            mutableHeaders.add("Extra");
            assertThat(result.headers()).containsExactly("Date", "Amount");
        }
    }
}
