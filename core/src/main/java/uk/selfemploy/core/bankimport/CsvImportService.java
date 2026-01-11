package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import uk.selfemploy.common.domain.ImportBatch;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.persistence.repository.ImportBatchRepository;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Service for importing transactions from bank CSV files.
 *
 * <p>Features:
 * <ul>
 *   <li>Auto-detects UK bank CSV formats</li>
 *   <li>Duplicate detection based on date/amount/description</li>
 *   <li>Auto-categorization based on description keywords</li>
 *   <li>Atomic import (all or nothing)</li>
 * </ul>
 *
 * <p>Constraints:
 * <ul>
 *   <li>Maximum file size: 10MB</li>
 *   <li>Performance target: 1000 rows in less than 5 seconds</li>
 * </ul>
 */
@ApplicationScoped
public class CsvImportService {

    /**
     * Maximum file size in bytes (10MB).
     */
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

    private final BankFormatDetector formatDetector;
    private final DuplicateDetector duplicateDetector;
    private final DescriptionCategorizer categorizer;
    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final ImportBatchRepository batchRepository;

    @Inject
    public CsvImportService(
            BankFormatDetector formatDetector,
            DuplicateDetector duplicateDetector,
            DescriptionCategorizer categorizer,
            IncomeService incomeService,
            ExpenseService expenseService,
            ImportBatchRepository batchRepository) {
        this.formatDetector = formatDetector;
        this.duplicateDetector = duplicateDetector;
        this.categorizer = categorizer;
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.batchRepository = batchRepository;
    }

    /**
     * Imports transactions from a CSV file.
     *
     * <p>The import is atomic - if any transaction fails to import,
     * the entire import is rolled back.</p>
     *
     * @param businessId the business ID to import transactions for
     * @param csvFile path to the CSV file
     * @param charset character encoding of the file
     * @return result containing import statistics
     * @throws CsvParseException if the file cannot be parsed
     */
    @Transactional
    public CsvImportResult importCsv(UUID businessId, Path csvFile, Charset charset) {
        // Validate file size
        validateFileSize(csvFile);

        // Detect bank format
        BankCsvParser parser = formatDetector.detectFormat(csvFile, charset)
            .orElseThrow(() -> new CsvParseException(
                "Unknown CSV format. Please check the file format or use manual column mapping."));

        String bankName = parser.getBankName();

        // Parse transactions
        List<ImportedTransaction> allTransactions = parser.parse(csvFile, charset);

        // Check for duplicates
        DuplicateCheckResult duplicateResult = duplicateDetector.checkDuplicates(businessId, allTransactions);

        // Import unique transactions
        int incomeCount = 0;
        int expenseCount = 0;

        for (ImportedTransaction tx : duplicateResult.uniqueTransactions()) {
            if (tx.isIncome()) {
                importIncome(businessId, tx);
                incomeCount++;
            } else {
                importExpense(businessId, tx);
                expenseCount++;
            }
        }

        // Create import batch record
        long fileSize = getFileSize(csvFile);
        ImportBatch batch = ImportBatch.create(
            businessId,
            bankName,
            csvFile.getFileName().toString(),
            fileSize,
            allTransactions.size(),
            incomeCount,
            expenseCount,
            duplicateResult.duplicateCount()
        );

        batchRepository.save(batch);

        return CsvImportResult.fromBatch(batch);
    }

    /**
     * Imports a CSV file using manual column mapping.
     *
     * @param businessId the business ID
     * @param csvFile path to the CSV file
     * @param charset character encoding
     * @param mapping column mapping configuration
     * @return import result
     */
    @Transactional
    public CsvImportResult importCsvWithMapping(
            UUID businessId,
            Path csvFile,
            Charset charset,
            ManualMappingParser.ColumnMapping mapping) {

        validateFileSize(csvFile);

        ManualMappingParser parser = new ManualMappingParser(mapping);
        String bankName = parser.getBankName();

        List<ImportedTransaction> allTransactions = parser.parse(csvFile, charset);
        DuplicateCheckResult duplicateResult = duplicateDetector.checkDuplicates(businessId, allTransactions);

        int incomeCount = 0;
        int expenseCount = 0;

        for (ImportedTransaction tx : duplicateResult.uniqueTransactions()) {
            if (tx.isIncome()) {
                importIncome(businessId, tx);
                incomeCount++;
            } else {
                importExpense(businessId, tx);
                expenseCount++;
            }
        }

        long fileSize = getFileSize(csvFile);
        ImportBatch batch = ImportBatch.create(
            businessId,
            bankName,
            csvFile.getFileName().toString(),
            fileSize,
            allTransactions.size(),
            incomeCount,
            expenseCount,
            duplicateResult.duplicateCount()
        );

        batchRepository.save(batch);

        return CsvImportResult.fromBatch(batch);
    }

    /**
     * Preview transactions without importing.
     *
     * @param businessId the business ID
     * @param csvFile path to the CSV file
     * @param charset character encoding
     * @return list of parsed transactions with category suggestions
     */
    public ImportPreview previewImport(UUID businessId, Path csvFile, Charset charset) {
        validateFileSize(csvFile);

        BankCsvParser parser = formatDetector.detectFormat(csvFile, charset)
            .orElseThrow(() -> new CsvParseException(
                "Unknown CSV format. Please check the file format or use manual column mapping."));

        List<ImportedTransaction> transactions = parser.parse(csvFile, charset);
        DuplicateCheckResult duplicateResult = duplicateDetector.checkDuplicates(businessId, transactions);

        return new ImportPreview(
            parser.getBankName(),
            duplicateResult.uniqueTransactions(),
            duplicateResult.duplicateTransactions()
        );
    }

    private void importIncome(UUID businessId, ImportedTransaction tx) {
        CategorySuggestion<IncomeCategory> suggestion = categorizer.suggestIncomeCategory(tx.description());

        incomeService.create(
            businessId,
            tx.date(),
            tx.amount(),
            tx.description(),
            suggestion.category(),
            tx.reference()
        );
    }

    private void importExpense(UUID businessId, ImportedTransaction tx) {
        CategorySuggestion<ExpenseCategory> suggestion = categorizer.suggestExpenseCategory(tx.description());

        expenseService.create(
            businessId,
            tx.date(),
            tx.absoluteAmount(), // Expenses stored as positive
            tx.description(),
            suggestion.category(),
            null, // receiptPath
            null  // notes
        );
    }

    private void validateFileSize(Path csvFile) {
        long fileSize = getFileSize(csvFile);
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new CsvParseException(String.format(
                "File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                fileSize, MAX_FILE_SIZE_BYTES));
        }
    }

    private long getFileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Preview of an import showing parsed transactions before saving.
     */
    public record ImportPreview(
        String bankName,
        List<ImportedTransaction> uniqueTransactions,
        List<ImportedTransaction> duplicateTransactions
    ) {
        public int totalCount() {
            return uniqueTransactions.size() + duplicateTransactions.size();
        }

        public int uniqueCount() {
            return uniqueTransactions.size();
        }

        public int duplicateCount() {
            return duplicateTransactions.size();
        }
    }
}
