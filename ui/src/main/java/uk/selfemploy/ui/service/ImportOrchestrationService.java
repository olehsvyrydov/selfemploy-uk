package uk.selfemploy.ui.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.ColumnMapping;
import uk.selfemploy.ui.viewmodel.ImportedTransactionRow;
import uk.selfemploy.ui.viewmodel.TransactionType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    private final UUID businessId;

    public ImportOrchestrationService(
            CsvTransactionParser csvParser,
            IncomeService incomeService,
            ExpenseService expenseService,
            UUID businessId) {
        this.csvParser = csvParser;
        this.incomeService = incomeService;
        this.expenseService = expenseService;
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
     * Result of an import operation.
     */
    public record ImportResult(int importedCount, int errorCount) {
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
     * Imports parsed transactions by saving them as Income or Expense records.
     *
     * @param transactions the transactions to import
     * @param progressCallback callback for progress updates (0.0 to 1.0)
     * @return import result with counts
     */
    public ImportResult importTransactions(
            List<ImportedTransactionRow> transactions,
            Consumer<Double> progressCallback) {

        int imported = 0;
        int errors = 0;
        int total = transactions.size();

        for (int i = 0; i < total; i++) {
            ImportedTransactionRow row = transactions.get(i);

            try {
                if (row.type() == TransactionType.INCOME) {
                    incomeService.create(
                        businessId,
                        row.date(),
                        row.amount(),
                        row.description(),
                        IncomeCategory.SALES,
                        null  // reference
                    );
                } else {
                    expenseService.create(
                        businessId,
                        row.date(),
                        row.amount(),
                        row.description(),
                        row.category(),
                        null,  // receiptPath
                        null   // notes
                    );
                }
                imported++;
            } catch (Exception e) {
                LOG.warn("Failed to import transaction: {}", row.description(), e);
                errors++;
            }

            if (progressCallback != null) {
                double progress = (double) (i + 1) / total;
                progressCallback.accept(progress);
            }
        }

        LOG.info("Import complete: {} imported, {} errors out of {} total",
                imported, errors, total);
        return new ImportResult(imported, errors);
    }
}
