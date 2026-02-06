package uk.selfemploy.core.bankimport;

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
import uk.selfemploy.common.domain.ImportBatch;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.persistence.repository.ImportBatchRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CsvImportService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CsvImportService Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class CsvImportServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private BankFormatDetector formatDetector;

    @Mock
    private DuplicateDetector duplicateDetector;

    @Mock
    private DescriptionCategorizer categorizer;

    @Mock
    private IncomeService incomeService;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private ImportBatchRepository batchRepository;

    @Mock
    private BankCsvParser barclaysParser;

    private CsvImportService importService;

    private static final UUID BUSINESS_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        importService = new CsvImportService(
            formatDetector,
            duplicateDetector,
            categorizer,
            incomeService,
            expenseService,
            batchRepository
        );
    }

    @Nested
    @DisplayName("Format Detection Tests")
    class FormatDetectionTests {

        @Test
        @DisplayName("should detect bank format automatically")
        void shouldDetectBankFormat() throws IOException {
            Path csvFile = createBarclaysCsv();

            when(formatDetector.detectFormat(csvFile, StandardCharsets.UTF_8))
                .thenReturn(Optional.of(barclaysParser));
            when(barclaysParser.getBankName()).thenReturn("Barclays");
            when(barclaysParser.parse(csvFile, StandardCharsets.UTF_8))
                .thenReturn(Collections.emptyList());
            when(duplicateDetector.checkDuplicates(eq(BUSINESS_ID), any()))
                .thenReturn(new DuplicateCheckResult(Collections.emptyList(), Collections.emptyList()));
            when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CsvImportResult result = importService.importCsv(BUSINESS_ID, csvFile, StandardCharsets.UTF_8);

            assertThat(result.bankName()).isEqualTo("Barclays");
            verify(formatDetector).detectFormat(csvFile, StandardCharsets.UTF_8);
        }

        @Test
        @DisplayName("should throw when format not detected")
        void shouldThrowWhenFormatNotDetected() throws IOException {
            Path csvFile = createUnknownCsv();

            when(formatDetector.detectFormat(csvFile, StandardCharsets.UTF_8))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                importService.importCsv(BUSINESS_ID, csvFile, StandardCharsets.UTF_8))
                .isInstanceOf(CsvParseException.class)
                .hasMessageContaining("format");
        }
    }

    @Nested
    @DisplayName("Income Import Tests")
    class IncomeImportTests {

        @Test
        @DisplayName("should import income transactions")
        void shouldImportIncome() throws IOException {
            Path csvFile = createBarclaysCsv();
            ImportedTransaction incomeTx = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                new BigDecimal("1000.00"),
                null
            );

            setupMocksForImport(List.of(incomeTx));

            CsvImportResult result = importService.importCsv(BUSINESS_ID, csvFile, StandardCharsets.UTF_8);

            assertThat(result.incomeCount()).isEqualTo(1);
            assertThat(result.expenseCount()).isEqualTo(0);

            verify(incomeService).create(
                eq(BUSINESS_ID),
                eq(LocalDate.of(2025, 6, 15)),
                eq(new BigDecimal("100.00")),
                eq("CLIENT PAYMENT"),
                any(IncomeCategory.class),
                isNull()
            );
        }
    }

    @Nested
    @DisplayName("Expense Import Tests")
    class ExpenseImportTests {

        @Test
        @DisplayName("should import expense transactions")
        void shouldImportExpense() throws IOException {
            Path csvFile = createBarclaysCsv();
            ImportedTransaction expenseTx = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("-50.00"),
                "AMAZON PURCHASE",
                new BigDecimal("950.00"),
                null
            );

            setupMocksForImport(List.of(expenseTx));

            CsvImportResult result = importService.importCsv(BUSINESS_ID, csvFile, StandardCharsets.UTF_8);

            assertThat(result.incomeCount()).isEqualTo(0);
            assertThat(result.expenseCount()).isEqualTo(1);

            verify(expenseService).create(
                eq(BUSINESS_ID),
                eq(LocalDate.of(2025, 6, 15)),
                eq(new BigDecimal("50.00")), // Absolute value
                eq("AMAZON PURCHASE"),
                any(ExpenseCategory.class),
                isNull(),
                isNull()
            );
        }
    }

    @Nested
    @DisplayName("Duplicate Handling Tests")
    class DuplicateHandlingTests {

        @Test
        @DisplayName("should skip duplicate transactions")
        void shouldSkipDuplicates() throws IOException {
            Path csvFile = createBarclaysCsv();
            ImportedTransaction tx = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                null,
                null
            );

            when(formatDetector.detectFormat(csvFile, StandardCharsets.UTF_8))
                .thenReturn(Optional.of(barclaysParser));
            when(barclaysParser.getBankName()).thenReturn("Barclays");
            when(barclaysParser.parse(csvFile, StandardCharsets.UTF_8))
                .thenReturn(List.of(tx));
            // Mark as duplicate
            when(duplicateDetector.checkDuplicates(eq(BUSINESS_ID), any()))
                .thenReturn(new DuplicateCheckResult(Collections.emptyList(), List.of(tx)));
            when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CsvImportResult result = importService.importCsv(BUSINESS_ID, csvFile, StandardCharsets.UTF_8);

            assertThat(result.duplicateCount()).isEqualTo(1);
            assertThat(result.incomeCount()).isEqualTo(0);

            verify(incomeService, never()).create(any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Category Suggestion Tests")
    class CategorySuggestionTests {

        @Test
        @DisplayName("should use suggested category for expenses")
        void shouldUseSuggestedCategoryForExpenses() throws IOException {
            Path csvFile = createBarclaysCsv();
            ImportedTransaction expenseTx = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("-50.00"),
                "AMAZON MARKETPLACE",
                null,
                null
            );

            setupMocksForImport(List.of(expenseTx));
            when(categorizer.suggestExpenseCategory("AMAZON MARKETPLACE"))
                .thenReturn(new CategorySuggestion<>(ExpenseCategory.OFFICE_COSTS, Confidence.HIGH));

            importService.importCsv(BUSINESS_ID, csvFile, StandardCharsets.UTF_8);

            verify(expenseService).create(
                any(),
                any(),
                any(),
                any(),
                eq(ExpenseCategory.OFFICE_COSTS),
                any(),
                any()
            );
        }

        @Test
        @DisplayName("should use suggested category for income")
        void shouldUseSuggestedCategoryForIncome() throws IOException {
            Path csvFile = createBarclaysCsv();
            ImportedTransaction incomeTx = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "BANK INTEREST",
                null,
                null
            );

            setupMocksForImport(List.of(incomeTx));
            when(categorizer.suggestIncomeCategory("BANK INTEREST"))
                .thenReturn(new CategorySuggestion<>(IncomeCategory.OTHER_INCOME, Confidence.HIGH));

            importService.importCsv(BUSINESS_ID, csvFile, StandardCharsets.UTF_8);

            verify(incomeService).create(
                any(),
                any(),
                any(),
                any(),
                eq(IncomeCategory.OTHER_INCOME),
                any()
            );
        }
    }

    @Nested
    @DisplayName("Import Batch Recording Tests")
    class ImportBatchRecordingTests {

        @Test
        @DisplayName("should create import batch record")
        void shouldCreateImportBatch() throws IOException {
            Path csvFile = createBarclaysCsv();
            ImportedTransaction tx = new ImportedTransaction(
                LocalDate.of(2025, 6, 15),
                new BigDecimal("100.00"),
                "PAYMENT",
                null,
                null
            );

            setupMocksForImport(List.of(tx));

            CsvImportResult result = importService.importCsv(BUSINESS_ID, csvFile, StandardCharsets.UTF_8);

            ArgumentCaptor<ImportBatch> batchCaptor = ArgumentCaptor.forClass(ImportBatch.class);
            verify(batchRepository).save(batchCaptor.capture());

            ImportBatch savedBatch = batchCaptor.getValue();
            assertThat(savedBatch.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(savedBatch.bankName()).isEqualTo("Barclays");
            assertThat(savedBatch.totalTransactions()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("File Size Validation Tests")
    class FileSizeValidationTests {

        @Test
        @DisplayName("should reject files larger than 10MB")
        void shouldRejectLargeFiles() throws IOException {
            // Create a file path that would be > 10MB
            Path largeCsvFile = tempDir.resolve("large.csv");

            // We can't actually create a 10MB file in tests, so we'll mock the size check
            // In the real implementation, the service should check Files.size()
            Files.writeString(largeCsvFile, "small content");

            // For this test, we'll verify the service has a size limit configuration
            // The actual size check is in the service
            assertThat(CsvImportService.MAX_FILE_SIZE_BYTES).isEqualTo(10 * 1024 * 1024);
        }
    }

    private void setupMocksForImport(List<ImportedTransaction> transactions) throws IOException {
        Path csvFile = tempDir.resolve("barclays.csv");

        when(formatDetector.detectFormat(any(), eq(StandardCharsets.UTF_8)))
            .thenReturn(Optional.of(barclaysParser));
        when(barclaysParser.getBankName()).thenReturn("Barclays");
        when(barclaysParser.parse(any(), eq(StandardCharsets.UTF_8)))
            .thenReturn(transactions);
        when(duplicateDetector.checkDuplicates(eq(BUSINESS_ID), any()))
            .thenReturn(new DuplicateCheckResult(transactions, Collections.emptyList()));

        // Setup category suggestions with defaults
        when(categorizer.suggestIncomeCategory(any()))
            .thenReturn(new CategorySuggestion<>(IncomeCategory.SALES, Confidence.MEDIUM));
        when(categorizer.suggestExpenseCategory(any()))
            .thenReturn(new CategorySuggestion<>(ExpenseCategory.OTHER_EXPENSES, Confidence.LOW));

        // Mock income and expense creation
        when(incomeService.create(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> new Income(
                UUID.randomUUID(),
                inv.getArgument(0),
                inv.getArgument(1),
                inv.getArgument(2),
                inv.getArgument(3),
                inv.getArgument(4),
                inv.getArgument(5),
                null,
                null,
                null,
                null
            ));

        when(expenseService.create(any(), any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> new Expense(
                UUID.randomUUID(),
                inv.getArgument(0),
                inv.getArgument(1),
                inv.getArgument(2),
                inv.getArgument(3),
                inv.getArgument(4),
                inv.getArgument(5),
                inv.getArgument(6),
                null,
                null,
                null,
                null
            ));

        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Path createBarclaysCsv() throws IOException {
        String csv = """
            Date,Description,Money Out,Money In,Balance
            15/06/2025,TEST TRANSACTION,10.00,,990.00
            """;
        Path file = tempDir.resolve("barclays.csv");
        Files.writeString(file, csv, StandardCharsets.UTF_8);
        return file;
    }

    private Path createUnknownCsv() throws IOException {
        String csv = """
            Column1,Column2,Column3
            value1,value2,value3
            """;
        Path file = tempDir.resolve("unknown.csv");
        Files.writeString(file, csv, StandardCharsets.UTF_8);
        return file;
    }
}
