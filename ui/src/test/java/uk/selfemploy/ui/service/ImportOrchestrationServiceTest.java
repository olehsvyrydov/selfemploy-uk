package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.bankimport.BankCsvParser;
import uk.selfemploy.core.bankimport.BankFormatDetector;
import uk.selfemploy.core.bankimport.CsvParseException;
import uk.selfemploy.core.bankimport.ImportedTransaction;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.ColumnMapping;
import uk.selfemploy.ui.viewmodel.ImportedTransactionRow;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ReviewStatus;
import uk.selfemploy.ui.viewmodel.TransactionType;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private SqliteBankTransactionService bankTransactionService;
    private UUID businessId;
    private ImportOrchestrationService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        csvParser = mock(CsvTransactionParser.class);
        incomeService = mock(IncomeService.class);
        expenseService = mock(ExpenseService.class);
        bankTransactionService = mock(SqliteBankTransactionService.class);
        when(bankTransactionService.existsByHash(anyString())).thenReturn(false);
        businessId = UUID.randomUUID();
        service = new ImportOrchestrationService(
            csvParser, incomeService, expenseService, bankTransactionService, businessId);
    }

    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorTests {

        @Test
        @DisplayName("should create service with valid dependencies")
        void shouldCreateServiceWithValidDependencies() {
            var svc = new ImportOrchestrationService(
                csvParser, incomeService, expenseService, bankTransactionService, businessId);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("should accept null csvParser without throwing at construction time")
        void shouldAcceptNullCsvParser() {
            // Constructor does not validate nulls - NullPointerException thrown at usage
            var svc = new ImportOrchestrationService(
                null, incomeService, expenseService, bankTransactionService, businessId);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("should accept null incomeService without throwing at construction time")
        void shouldAcceptNullIncomeService() {
            var svc = new ImportOrchestrationService(
                csvParser, null, expenseService, bankTransactionService, businessId);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("should accept null expenseService without throwing at construction time")
        void shouldAcceptNullExpenseService() {
            var svc = new ImportOrchestrationService(
                csvParser, incomeService, null, bankTransactionService, businessId);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("should accept null businessId without throwing at construction time")
        void shouldAcceptNullBusinessId() {
            var svc = new ImportOrchestrationService(
                csvParser, incomeService, expenseService, bankTransactionService, null);
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
    @DisplayName("importTransactions() staging")
    class ImportTransactionsTests {

        private BankTransaction firstSaved() {
            ArgumentCaptor<BankTransaction> captor = ArgumentCaptor.forClass(BankTransaction.class);
            verify(bankTransactionService).save(captor.capture());
            return captor.getValue();
        }

        @Test
        @DisplayName("stages an income row as a positive PENDING bank transaction")
        void stagesIncomeRow() {
            ImportedTransactionRow incomeRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Client Payment", new BigDecimal("2500.00"),
                    TransactionType.INCOME, null, false, 0);

            ImportOrchestrationService.ImportResult result = service.importTransactions(List.of(incomeRow), "test-import.csv", null);

            BankTransaction tx = firstSaved();
            assertThat(tx.businessId()).isEqualTo(businessId);
            assertThat(tx.amount()).isEqualByComparingTo("2500.00");
            assertThat(tx.isIncome()).isTrue();
            assertThat(tx.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
            assertThat(tx.importAuditId()).isEqualTo(result.batchId());
            assertThat(result.importedCount()).isEqualTo(1);
            verifyNoInteractions(incomeService, expenseService);
        }

        @Test
        @DisplayName("stages an expense row as a negative PENDING transaction carrying the suggested category")
        void stagesExpenseRow() {
            ImportedTransactionRow expenseRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 17), "Office Supplies", new BigDecimal("89.99"),
                    TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 0);

            service.importTransactions(List.of(expenseRow), "test-import.csv", null);

            BankTransaction tx = firstSaved();
            assertThat(tx.amount()).isEqualByComparingTo("-89.99");
            assertThat(tx.isExpense()).isTrue();
            assertThat(tx.suggestedCategory()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
            verifyNoInteractions(incomeService, expenseService);
        }

        @Test
        @DisplayName("auto-suggests a category and high confidence for an uncategorised expense whose description matches a keyword")
        void autoSuggestsCategoryForUncategorisedExpense() {
            ImportedTransactionRow expenseRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 17), "AMAZON MARKETPLACE", new BigDecimal("42.00"),
                    TransactionType.EXPENSE, null, false, 0);

            service.importTransactions(List.of(expenseRow), "test-import.csv", null);

            BankTransaction tx = firstSaved();
            assertThat(tx.suggestedCategory()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
            assertThat(tx.confidenceScore()).isNotNull();
            assertThat(tx.confidenceScore()).isGreaterThanOrEqualTo(new BigDecimal("0.9"));
        }

        @Test
        @DisplayName("leaves an unrecognised expense uncategorised with low confidence rather than guessing")
        void leavesUnrecognisedExpenseUncategorised() {
            ImportedTransactionRow expenseRow = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 17), "Zzq unknown payee", new BigDecimal("42.00"),
                    TransactionType.EXPENSE, null, false, 0);

            service.importTransactions(List.of(expenseRow), "test-import.csv", null);

            BankTransaction tx = firstSaved();
            assertThat(tx.suggestedCategory()).isNull();
            assertThat(tx.confidenceScore()).isNotNull();
            assertThat(tx.confidenceScore()).isLessThan(new BigDecimal("0.6"));
        }

        @Test
        @DisplayName("stages every row under a single shared batch id")
        void stagesMixedWithSharedBatch() {
            List<ImportedTransactionRow> transactions = List.of(
                    ImportedTransactionRow.create(LocalDate.of(2025, 1, 15), "I1", new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(LocalDate.of(2025, 1, 16), "E1", new BigDecimal("50.00"), TransactionType.EXPENSE, ExpenseCategory.TRAVEL, false, 0),
                    ImportedTransactionRow.create(LocalDate.of(2025, 1, 17), "I2", new BigDecimal("200.00"), TransactionType.INCOME, null, false, 0));

            ImportOrchestrationService.ImportResult result = service.importTransactions(transactions, "test-import.csv", null);

            assertThat(result.importedCount()).isEqualTo(3);
            assertThat(result.errorCount()).isZero();
            ArgumentCaptor<BankTransaction> captor = ArgumentCaptor.forClass(BankTransaction.class);
            verify(bankTransactionService, times(3)).save(captor.capture());
            assertThat(captor.getAllValues()).extracting(BankTransaction::importAuditId)
                    .containsOnly(result.batchId());
        }

        @Test
        @DisplayName("skips a row whose hash already exists, without staging it")
        void skipsExistingHash() {
            when(bankTransactionService.existsByHash(anyString())).thenReturn(true);
            ImportedTransactionRow row = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Dup", new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0);

            ImportOrchestrationService.ImportResult result = service.importTransactions(List.of(row), "test-import.csv", null);

            assertThat(result.importedCount()).isZero();
            assertThat(result.skippedCount()).isEqualTo(1);
            verify(bankTransactionService, never()).save(any());
        }

        @Test
        @DisplayName("counts an error when staging a row throws")
        void countsErrorWhenSaveThrows() {
            doThrow(new RuntimeException("save failed")).when(bankTransactionService).save(any());
            ImportedTransactionRow row = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Boom", new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0);

            ImportOrchestrationService.ImportResult result = service.importTransactions(List.of(row), "test-import.csv", null);

            assertThat(result.importedCount()).isZero();
            assertThat(result.errorCount()).isEqualTo(1);
            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("continues staging after an individual error")
        void continuesAfterError() {
            doNothing().doThrow(new RuntimeException("fail")).doNothing().when(bankTransactionService).save(any());
            List<ImportedTransactionRow> transactions = List.of(
                    ImportedTransactionRow.create(LocalDate.of(2025, 1, 15), "OK1", new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(LocalDate.of(2025, 1, 16), "FAIL", new BigDecimal("200.00"), TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(LocalDate.of(2025, 1, 17), "OK2", new BigDecimal("300.00"), TransactionType.INCOME, null, false, 0));

            ImportOrchestrationService.ImportResult result = service.importTransactions(transactions, "test-import.csv", null);

            assertThat(result.importedCount()).isEqualTo(2);
            assertThat(result.errorCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("invokes the progress callback with fractional values")
        void invokesProgressCallback() {
            List<Double> progress = new ArrayList<>();
            List<ImportedTransactionRow> transactions = List.of(
                    ImportedTransactionRow.create(LocalDate.of(2025, 1, 15), "T1", new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(LocalDate.of(2025, 1, 16), "T2", new BigDecimal("200.00"), TransactionType.INCOME, null, false, 0),
                    ImportedTransactionRow.create(LocalDate.of(2025, 1, 17), "T3", new BigDecimal("300.00"), TransactionType.INCOME, null, false, 0));

            service.importTransactions(transactions, "test-import.csv", progress::add);

            assertThat(progress).hasSize(3);
            assertThat(progress.get(2)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("handles a null progress callback")
        void handlesNullCallback() {
            ImportedTransactionRow row = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Test", new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0);
            ImportOrchestrationService.ImportResult result = service.importTransactions(List.of(row), "test-import.csv", null);
            assertThat(result.importedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns zero counts for an empty list")
        void emptyList() {
            ImportOrchestrationService.ImportResult result = service.importTransactions(List.of(), "test-import.csv", null);
            assertThat(result.importedCount()).isZero();
            assertThat(result.errorCount()).isZero();
            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("invokes the progress callback even when a row errors")
        void progressCallbackOnError() {
            doThrow(new RuntimeException("fail")).when(bankTransactionService).save(any());
            AtomicReference<Double> last = new AtomicReference<>(0.0);
            ImportedTransactionRow row = ImportedTransactionRow.create(
                    LocalDate.of(2025, 1, 15), "Fail", new BigDecimal("100.00"), TransactionType.INCOME, null, false, 0);

            service.importTransactions(List.of(row), "test-import.csv", last::set);

            assertThat(last.get()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("ImportResult record")
    class ImportResultTests {

        @Test
        @DisplayName("hasErrors should return true when errorCount > 0")
        void hasErrorsShouldReturnTrueWhenErrors() {
            var result = new ImportOrchestrationService.ImportResult(5, 2, 0, java.util.UUID.randomUUID());
            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("hasErrors should return false when errorCount is 0")
        void hasErrorsShouldReturnFalseWhenNoErrors() {
            var result = new ImportOrchestrationService.ImportResult(5, 0, 0, java.util.UUID.randomUUID());
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

    @Nested
    @DisplayName("Duplicate Detection (B4)")
    class DuplicateDetection {

        private final LocalDate date = LocalDate.of(2025, 6, 15);

        private ImportedTransactionRow incomeRow(String description, String amount) {
            return ImportedTransactionRow.create(date, description, new BigDecimal(amount),
                TransactionType.INCOME, null, false, 0);
        }

        private ImportedTransactionRow expenseRow(String description, String amount) {
            return ImportedTransactionRow.create(date, description, new BigDecimal(amount),
                TransactionType.EXPENSE, ExpenseCategory.OFFICE_COSTS, false, 0);
        }

        private uk.selfemploy.common.domain.Income existingIncome(String description, String amount) {
            return uk.selfemploy.common.domain.Income.create(businessId, date, new BigDecimal(amount),
                description, IncomeCategory.SALES, null);
        }

        private uk.selfemploy.common.domain.Expense existingExpense(String description, String amount) {
            return uk.selfemploy.common.domain.Expense.create(businessId, date, new BigDecimal(amount),
                description, ExpenseCategory.OFFICE_COSTS, null, null);
        }

        @Test
        @DisplayName("re-importing identical rows flags them all as duplicates")
        void shouldFlagIdenticalReimportAsDuplicates() {
            // Given the same 3 income + 2 expense records already exist in the store
            List<uk.selfemploy.common.domain.Income> existingIncomes = List.of(
                existingIncome("Invoice 1", "100.00"),
                existingIncome("Invoice 2", "200.00"),
                existingIncome("Invoice 3", "300.00"));
            List<uk.selfemploy.common.domain.Expense> existingExpenses = List.of(
                existingExpense("Stationery", "10.00"),
                existingExpense("Postage", "20.00"));
            when(incomeService.findByTaxYear(eq(businessId), any())).thenReturn(existingIncomes);
            when(expenseService.findByTaxYear(eq(businessId), any())).thenReturn(existingExpenses);

            List<ImportedTransactionRow> reimport = List.of(
                incomeRow("Invoice 1", "100.00"),
                incomeRow("Invoice 2", "200.00"),
                incomeRow("Invoice 3", "300.00"),
                expenseRow("Stationery", "10.00"),
                expenseRow("Postage", "20.00"));

            // When
            List<ImportedTransactionRow> marked = service.markDuplicates(reimport);

            // Then all 5 are flagged as duplicates (regression: this used to be 0)
            assertThat(marked).allMatch(ImportedTransactionRow::isDuplicate);
            assertThat(marked.stream().filter(ImportedTransactionRow::isDuplicate).count()).isEqualTo(5);
        }

        @Test
        @DisplayName("only rows matching an existing record are flagged")
        void shouldFlagOnlyMatchingRows() {
            when(incomeService.findByTaxYear(eq(businessId), any()))
                .thenReturn(List.of(existingIncome("Invoice 1", "100.00")));
            when(expenseService.findByTaxYear(eq(businessId), any()))
                .thenReturn(List.of());

            List<ImportedTransactionRow> rows = List.of(
                incomeRow("Invoice 1", "100.00"),   // duplicate
                incomeRow("Invoice 9", "999.00"));  // new

            List<ImportedTransactionRow> marked = service.markDuplicates(rows);

            assertThat(marked.get(0).isDuplicate()).isTrue();
            assertThat(marked.get(1).isDuplicate()).isFalse();
        }

        @Test
        @DisplayName("an income row is not matched against an existing expense of the same amount")
        void shouldNotCrossMatchIncomeAgainstExpense() {
            when(incomeService.findByTaxYear(eq(businessId), any())).thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(businessId), any()))
                .thenReturn(List.of(existingExpense("Invoice 1", "100.00")));

            List<ImportedTransactionRow> marked = service.markDuplicates(
                List.of(incomeRow("Invoice 1", "100.00")));

            assertThat(marked.get(0).isDuplicate()).isFalse();
        }

        @Test
        @DisplayName("no existing records means nothing is flagged")
        void shouldFlagNothingWhenStoreEmpty() {
            when(incomeService.findByTaxYear(eq(businessId), any())).thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(businessId), any())).thenReturn(List.of());

            List<ImportedTransactionRow> marked = service.markDuplicates(
                List.of(incomeRow("Invoice 1", "100.00"), expenseRow("Postage", "20.00")));

            assertThat(marked).noneMatch(ImportedTransactionRow::isDuplicate);
        }

        @Test
        @DisplayName("an undated row is passed through as a non-duplicate rather than crashing the scan")
        void shouldNotCrashOnUndatedRow() {
            when(incomeService.findByTaxYear(eq(businessId), any())).thenReturn(List.of());
            when(expenseService.findByTaxYear(eq(businessId), any())).thenReturn(List.of());

            ImportedTransactionRow undated = ImportedTransactionRow.create(
                null, "No date", new BigDecimal("42.00"), TransactionType.INCOME, null, false, 0);

            List<ImportedTransactionRow> marked = service.markDuplicates(
                List.of(undated, incomeRow("Invoice 1", "100.00")));

            assertThat(marked).hasSize(2);
            assertThat(marked.get(0).isDuplicate()).isFalse();
            assertThat(marked.get(1).isDuplicate()).isFalse();
        }
    }

    @Nested
    @DisplayName("autoDetectTransactions()")
    class AutoDetect {

        private final class StubParser implements BankCsvParser {
            private final boolean matches;
            private final List<ImportedTransaction> output;

            StubParser(boolean matches, List<ImportedTransaction> output) {
                this.matches = matches;
                this.output = output;
            }

            @Override public String getBankName() {
                return "StubBank";
            }

            @Override public boolean canParse(String[] headers) {
                return matches;
            }

            @Override public List<ImportedTransaction> parse(Path csvFile, Charset charset) {
                return output;
            }

            @Override public String[] getExpectedHeaders() {
                return new String[] {"Date", "Amount"};
            }
        }

        private Path csvWithHeader() throws IOException {
            Path file = tempDir.resolve("statement.csv");
            Files.writeString(file, "Date,Amount,Description\n2025-05-01,100.00,Payment\n",
                StandardCharsets.UTF_8);
            return file;
        }

        @Test
        @DisplayName("returns converted rows (type from sign, absolute amount) for a recognised format")
        void recognisedFormatReturnsRows() throws IOException {
            List<ImportedTransaction> parsed = List.of(
                new ImportedTransaction(LocalDate.of(2025, 5, 1), new BigDecimal("100.00"),
                    "Client payment", null, null),
                new ImportedTransaction(LocalDate.of(2025, 5, 2), new BigDecimal("-20.00"),
                    "Fuel", null, null));
            BankFormatDetector detector = new BankFormatDetector(List.of(new StubParser(true, parsed)));

            Optional<List<ImportedTransactionRow>> rows =
                service.autoDetectTransactions(csvWithHeader(), detector);

            assertThat(rows).isPresent();
            assertThat(rows.get()).hasSize(2);
            assertThat(rows.get().get(0).type()).isEqualTo(TransactionType.INCOME);
            assertThat(rows.get().get(0).amount()).isEqualByComparingTo("100.00");
            assertThat(rows.get().get(1).type()).isEqualTo(TransactionType.EXPENSE);
            assertThat(rows.get().get(1).amount()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("returns empty for an unrecognised format so the caller uses manual mapping")
        void unrecognisedFormatReturnsEmpty() throws IOException {
            BankFormatDetector detector = new BankFormatDetector(List.of(new StubParser(false, List.of())));

            Optional<List<ImportedTransactionRow>> rows =
                service.autoDetectTransactions(csvWithHeader(), detector);

            assertThat(rows).isEmpty();
        }

        @Test
        @DisplayName("returns empty when a recognised parser fails, falling back to manual mapping")
        void parseFailureReturnsEmpty() throws IOException {
            BankCsvParser throwing = new BankCsvParser() {
                @Override public String getBankName() {
                    return "Throwing";
                }

                @Override public boolean canParse(String[] headers) {
                    return true;
                }

                @Override public List<ImportedTransaction> parse(Path csvFile, Charset charset)
                        throws CsvParseException {
                    throw new CsvParseException("bad row", "statement.csv", 2, null);
                }

                @Override public String[] getExpectedHeaders() {
                    return new String[] {"Date", "Amount"};
                }
            };

            Optional<List<ImportedTransactionRow>> rows =
                service.autoDetectTransactions(csvWithHeader(), new BankFormatDetector(List.of(throwing)));

            assertThat(rows).isEmpty();
        }
    }
}
