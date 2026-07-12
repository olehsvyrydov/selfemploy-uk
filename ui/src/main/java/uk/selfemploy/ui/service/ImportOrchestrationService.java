package uk.selfemploy.ui.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.bankimport.BankFormatDetector;
import uk.selfemploy.core.bankimport.CsvStatementSource;
import uk.selfemploy.core.bankimport.ImportedTransaction;
import uk.selfemploy.core.bankimport.StandardBankParsers;
import uk.selfemploy.core.bankimport.StatementBatch;
import uk.selfemploy.core.bankimport.StatementSourceException;
import uk.selfemploy.core.reconciliation.MatchingUtils;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.ColumnMapping;
import uk.selfemploy.ui.viewmodel.ImportedTransactionRow;
import uk.selfemploy.ui.viewmodel.TransactionType;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Orchestrates CSV file loading, parsing, and transaction import.
 *
 * <p>Extracted from BankImportWizardController to follow the
 * single-responsibility principle. The controller handles UI concerns
 * (FXML, threading, dialogs); this service handles business logic
 * (file I/O, parsing, persistence).</p>
 */
public class ImportOrchestrationService {

    private static final Logger LOG = LoggerFactory.getLogger(ImportOrchestrationService.class);

    private final CsvTransactionParser csvParser;
    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final SqliteBankTransactionService bankTransactionService;
    private final UUID businessId;

    public ImportOrchestrationService(
            CsvTransactionParser csvParser,
            IncomeService incomeService,
            ExpenseService expenseService,
            SqliteBankTransactionService bankTransactionService,
            UUID businessId) {
        this.csvParser = csvParser;
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.bankTransactionService = bankTransactionService;
        this.businessId = businessId;
    }

    /**
     * Result of loading a CSV file: headers and row count.
     */
    public record FileLoadResult(List<String> headers, int rowCount) {
        public FileLoadResult {
            headers = List.copyOf(headers);
        }
    }

    /**
     * Result of an import operation. {@code importedCount} is the number of transactions staged for
     * review; {@code skippedCount} is the number skipped as already-staged duplicates; {@code batchId}
     * tags the staged rows so the review screen can scope to this import.
     */
    public record ImportResult(int importedCount, int errorCount, int skippedCount, UUID batchId) {
        public boolean hasErrors() {
            return errorCount > 0;
        }
    }

    /**
     * Loads a CSV file and returns its headers and row count.
     *
     * @param csvFile path to the CSV file
     * @return file load result with headers and row count
     * @throws IOException if the file cannot be read
     */
    public FileLoadResult loadFile(Path csvFile) throws IOException {
        List<String> headers = new ArrayList<>();
        int rowCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine != null && !headerLine.isBlank()) {
                String[] headerFields = CsvTransactionParser.parseCsvLine(headerLine);
                for (String h : headerFields) {
                    headers.add(h.trim());
                }
            }

            while (reader.readLine() != null) {
                rowCount++;
            }
        }

        return new FileLoadResult(headers, rowCount);
    }

    /**
     * Parses CSV transactions using the given column mapping.
     *
     * @param csvFile path to the CSV file
     * @param mapping column mapping configuration
     * @return parse result with transactions and warnings
     */
    public CsvTransactionParser.ParseResult parseTransactions(Path csvFile, ColumnMapping mapping) {
        return csvParser.parse(csvFile, mapping);
    }

    /**
     * Parses a CSV using the built-in per-bank parsers when its format is recognised, so a known
     * bank statement is read by its dedicated parser rather than generic column mapping.
     *
     * @param csvFile the CSV file
     * @return the parsed rows if a bank format was recognised and read, or empty to signal the
     *         caller should fall back to manual column mapping
     */
    public Optional<List<ImportedTransactionRow>> autoDetectTransactions(Path csvFile) {
        return autoDetectTransactions(csvFile, new BankFormatDetector(StandardBankParsers.all()));
    }

    Optional<List<ImportedTransactionRow>> autoDetectTransactions(Path csvFile, BankFormatDetector detector) {
        CsvStatementSource source = new CsvStatementSource(csvFile, StandardCharsets.UTF_8, detector);
        try {
            StatementBatch batch = source.fetch();
            List<ImportedTransactionRow> rows = new ArrayList<>(batch.size());
            for (ImportedTransaction t : batch.transactions()) {
                TransactionType type = t.isIncome() ? TransactionType.INCOME : TransactionType.EXPENSE;
                rows.add(ImportedTransactionRow.create(
                    t.date(), t.description(), t.amount(), type, null, false, 0));
            }
            return Optional.of(rows);
        } catch (StatementSourceException e) {
            LOG.info("No bank auto-detect for {} ({}); using manual mapping",
                csvFile.getFileName(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Flags parsed transactions that already exist in the database.
     *
     * <p>The CSV parser cannot know about stored data, so it leaves every row as
     * not-a-duplicate. Without this pass, re-importing the same statement created
     * duplicate records and the wizard reported "Duplicates: 0". Here each row is
     * compared against the income (for income rows) or expense (for expense rows)
     * records already stored for the tax year the row falls in, using the same
     * exact-match key as the Settings reconciliation flow (date + absolute amount +
     * normalised description). Matching rows are returned flagged as duplicates,
     * which the wizard then excludes from import by default.</p>
     *
     * @param rows the parsed transactions
     * @return a new list with matching rows marked as duplicates; order is preserved
     */
    public List<ImportedTransactionRow> markDuplicates(List<ImportedTransactionRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows == null ? List.of() : rows;
        }

        Set<TaxYear> taxYears = new HashSet<>();
        for (ImportedTransactionRow row : rows) {
            // Undated rows cannot be placed in a tax year; they are handled as
            // non-duplicates below rather than crashing the whole import.
            if (row.date() != null) {
                taxYears.add(taxYearFor(row.date()));
            }
        }

        Set<String> incomeKeys = new HashSet<>();
        Set<String> expenseKeys = new HashSet<>();
        for (TaxYear taxYear : taxYears) {
            if (incomeService != null) {
                for (Income income : incomeService.findByTaxYear(businessId, taxYear)) {
                    incomeKeys.add(MatchingUtils.createExactKey(
                        income.date(), income.amount(), income.description()));
                }
            }
            if (expenseService != null) {
                for (Expense expense : expenseService.findByTaxYear(businessId, taxYear)) {
                    expenseKeys.add(MatchingUtils.createExactKey(
                        expense.date(), expense.amount(), expense.description()));
                }
            }
        }

        List<ImportedTransactionRow> result = new ArrayList<>(rows.size());
        int duplicates = 0;
        for (ImportedTransactionRow row : rows) {
            // An incomplete row cannot form a match key, so it is left unchanged
            // (treated as a non-duplicate) rather than breaking the import.
            if (row.date() == null || row.amount() == null
                || row.description() == null || row.type() == null) {
                result.add(row);
                continue;
            }
            String key = MatchingUtils.createExactKey(row.date(), row.amount(), row.description());
            boolean isDuplicate = row.type() == TransactionType.INCOME
                ? incomeKeys.contains(key)
                : expenseKeys.contains(key);
            if (isDuplicate) {
                duplicates++;
                result.add(row.withDuplicateStatus(true));
            } else {
                result.add(row);
            }
        }

        LOG.info("Duplicate scan: {} of {} imported rows already exist", duplicates, rows.size());
        return result;
    }

    /**
     * Returns the UK tax year (6 April - 5 April) that the given date falls in.
     */
    private static TaxYear taxYearFor(LocalDate date) {
        TaxYear candidate = TaxYear.of(date.getYear());
        return candidate.contains(date) ? candidate : TaxYear.of(date.getYear() - 1);
    }

    /**
     * Stages parsed transactions into the bank-transaction review queue.
     *
     * <p>Rows are written to {@code bank_transactions} with review status PENDING under a single
     * import batch id, rather than committed straight to income/expense. The Bank Review screen is
     * where a reviewer turns them into income/expense records. A row whose deterministic hash already
     * exists in {@code bank_transactions} is skipped, so re-importing the same statement stages
     * nothing new.</p>
     *
     * @param transactions the transactions to stage
     * @param progressCallback callback for progress updates (0.0 to 1.0)
     * @return import result with counts and the batch id for the staged rows
     */
    public ImportResult importTransactions(
            List<ImportedTransactionRow> transactions,
            Consumer<Double> progressCallback) {

        UUID batchId = UUID.randomUUID();
        Instant now = Instant.now();
        int staged = 0;
        int skipped = 0;
        int errors = 0;
        int total = transactions.size();

        for (int i = 0; i < total; i++) {
            ImportedTransactionRow row = transactions.get(i);

            try {
                // Include direction so a credit and a debit with the same date/amount/description
                // do not collide and wrongly skip one another as a duplicate.
                String hash = MatchingUtils.createExactKey(row.date(), row.amount(), row.description())
                    + "|" + row.type();
                if (bankTransactionService.existsByHash(hash)) {
                    skipped++;
                } else {
                    // Direction-based sign: income positive, expense negative.
                    BigDecimal signedAmount = row.type() == TransactionType.INCOME
                        ? row.amount()
                        : row.amount().negate();
                    BankTransaction tx = BankTransaction.create(
                        businessId, batchId, null, row.date(), signedAmount, row.description(),
                        null, null, hash, now);
                    if (row.type() == TransactionType.EXPENSE && row.category() != null) {
                        tx = tx.withSuggestion(row.category(), null, now);
                    }
                    bankTransactionService.save(tx);
                    staged++;
                }
            } catch (Exception e) {
                LOG.warn("Failed to stage transaction: {}", row.description(), e);
                errors++;
            }

            if (progressCallback != null) {
                double progress = (double) (i + 1) / total;
                progressCallback.accept(progress);
            }
        }

        LOG.info("Import staged: {} new, {} duplicate(s) skipped, {} error(s) out of {} total",
                staged, skipped, errors, total);
        return new ImportResult(staged, errors, skipped, batchId);
    }
}
