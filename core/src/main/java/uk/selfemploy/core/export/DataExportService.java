package uk.selfemploy.core.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for exporting income and expense data to various formats.
 *
 * <p>Supports:
 * <ul>
 *   <li>JSON export with full metadata for re-import</li>
 *   <li>CSV export for income records</li>
 *   <li>CSV export for expense records</li>
 *   <li>Combined report with summary</li>
 * </ul>
 */
@ApplicationScoped
public class DataExportService {

    private static final String APP_VERSION = "0.1.0";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final ObjectMapper objectMapper;

    @Inject
    public DataExportService(IncomeService incomeService, ExpenseService expenseService) {
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.objectMapper = createObjectMapper();
    }

    /**
     * Exports all data to JSON format.
     *
     * @param businessId The business ID
     * @param taxYears   The tax years to export
     * @param outputFile The output file path
     * @return Export result
     */
    public ExportResult exportToJson(UUID businessId, TaxYear[] taxYears, Path outputFile) {
        return exportToJson(businessId, taxYears, outputFile, ExportOptions.noFilter());
    }

    /**
     * Exports data to JSON format with filtering options.
     *
     * @param businessId The business ID
     * @param taxYears   The tax years to export
     * @param outputFile The output file path
     * @param options    Export options for filtering
     * @return Export result
     */
    public ExportResult exportToJson(UUID businessId, TaxYear[] taxYears, Path outputFile, ExportOptions options) {
        validateInputs(businessId, taxYears);

        try {
            List<Income> allIncomes = new ArrayList<>();
            List<Expense> allExpenses = new ArrayList<>();

            for (TaxYear taxYear : taxYears) {
                List<Income> yearIncomes = incomeService.findByTaxYear(businessId, taxYear);
                List<Expense> yearExpenses = expenseService.findByTaxYear(businessId, taxYear);

                if (options.hasDateFilter()) {
                    yearIncomes = yearIncomes.stream()
                        .filter(i -> options.isWithinRange(i.date()))
                        .collect(Collectors.toList());
                    yearExpenses = yearExpenses.stream()
                        .filter(e -> options.isWithinRange(e.date()))
                        .collect(Collectors.toList());
                }

                allIncomes.addAll(yearIncomes);
                allExpenses.addAll(yearExpenses);
            }

            // Build export structure
            Map<String, Object> exportData = new LinkedHashMap<>();

            // Metadata
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("appVersion", APP_VERSION);
            metadata.put("exportDate", LocalDateTime.now().toString());
            metadata.put("taxYears", Arrays.stream(taxYears)
                .map(TaxYear::label)
                .collect(Collectors.toList()));
            if (options.hasDateFilter()) {
                metadata.put("filterStartDate", options.startDate() != null ? options.startDate().toString() : null);
                metadata.put("filterEndDate", options.endDate() != null ? options.endDate().toString() : null);
            }
            exportData.put("metadata", metadata);

            // Income data
            exportData.put("incomes", allIncomes.stream()
                .map(this::incomeToMap)
                .collect(Collectors.toList()));

            // Expense data
            exportData.put("expenses", allExpenses.stream()
                .map(this::expenseToMap)
                .collect(Collectors.toList()));

            // Write to file
            objectMapper.writeValue(outputFile.toFile(), exportData);

            return ExportResult.success(outputFile, allIncomes.size(), allExpenses.size());

        } catch (IOException e) {
            return ExportResult.failure("Failed to export data: " + e.getMessage());
        }
    }

    /**
     * Exports income records to CSV format.
     *
     * @param businessId The business ID
     * @param taxYears   The tax years to export
     * @param outputFile The output file path
     * @return Export result
     */
    public ExportResult exportIncomeToCsv(UUID businessId, TaxYear[] taxYears, Path outputFile) {
        validateInputs(businessId, taxYears);

        try {
            List<Income> allIncomes = new ArrayList<>();
            for (TaxYear taxYear : taxYears) {
                allIncomes.addAll(incomeService.findByTaxYear(businessId, taxYear));
            }

            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                // Write header
                writer.write("Date,Amount,Description,Category,Reference");
                writer.newLine();

                // Write data rows
                for (Income income : allIncomes) {
                    writer.write(formatCsvRow(
                        income.date().format(DATE_FORMAT),
                        income.amount().toPlainString(),
                        income.description(),
                        income.category().getDisplayName(),
                        income.reference() != null ? income.reference() : ""
                    ));
                    writer.newLine();
                }
            }

            return ExportResult.success(outputFile, allIncomes.size(), 0);

        } catch (IOException e) {
            return ExportResult.failure("Failed to export income: " + e.getMessage());
        }
    }

    /**
     * Exports expense records to CSV format.
     *
     * @param businessId The business ID
     * @param taxYears   The tax years to export
     * @param outputFile The output file path
     * @return Export result
     */
    public ExportResult exportExpensesToCsv(UUID businessId, TaxYear[] taxYears, Path outputFile) {
        validateInputs(businessId, taxYears);

        try {
            List<Expense> allExpenses = new ArrayList<>();
            for (TaxYear taxYear : taxYears) {
                allExpenses.addAll(expenseService.findByTaxYear(businessId, taxYear));
            }

            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                // Write header
                writer.write("Date,Amount,Description,Category,SA103 Box,Notes");
                writer.newLine();

                // Write data rows
                for (Expense expense : allExpenses) {
                    writer.write(formatCsvRow(
                        expense.date().format(DATE_FORMAT),
                        expense.amount().toPlainString(),
                        expense.description(),
                        expense.category().getDisplayName(),
                        expense.category().getSa103Box(),
                        expense.notes() != null ? expense.notes() : ""
                    ));
                    writer.newLine();
                }
            }

            return ExportResult.success(outputFile, 0, allExpenses.size());

        } catch (IOException e) {
            return ExportResult.failure("Failed to export expenses: " + e.getMessage());
        }
    }

    /**
     * Exports a combined report with income, expenses, and summary files.
     *
     * @param businessId The business ID
     * @param taxYears   The tax years to export
     * @param outputDir  The output directory
     * @return Combined export result
     */
    public CombinedExportResult exportCombinedReport(UUID businessId, TaxYear[] taxYears, Path outputDir) {
        validateInputs(businessId, taxYears);

        try {
            // Create output directory if needed
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            // Export income
            Path incomeFile = outputDir.resolve("income_" + timestamp + ".csv");
            ExportResult incomeResult = exportIncomeToCsv(businessId, taxYears, incomeFile);
            if (!incomeResult.success()) {
                return CombinedExportResult.failure(incomeResult.errorMessage());
            }

            // Export expenses
            Path expenseFile = outputDir.resolve("expenses_" + timestamp + ".csv");
            ExportResult expenseResult = exportExpensesToCsv(businessId, taxYears, expenseFile);
            if (!expenseResult.success()) {
                return CombinedExportResult.failure(expenseResult.errorMessage());
            }

            // Calculate totals
            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpenses = BigDecimal.ZERO;

            for (TaxYear taxYear : taxYears) {
                totalIncome = totalIncome.add(incomeService.getTotalByTaxYear(businessId, taxYear));
                totalExpenses = totalExpenses.add(expenseService.getTotalByTaxYear(businessId, taxYear));
            }

            BigDecimal netProfit = totalIncome.subtract(totalExpenses);

            // Write summary
            Path summaryFile = outputDir.resolve("summary_" + timestamp + ".txt");
            writeSummary(summaryFile, taxYears, totalIncome, totalExpenses, netProfit,
                incomeResult.incomeCount(), expenseResult.expenseCount());

            return CombinedExportResult.success(
                incomeFile, expenseFile, summaryFile,
                incomeResult.incomeCount(), expenseResult.expenseCount()
            );

        } catch (IOException e) {
            return CombinedExportResult.failure("Failed to export combined report: " + e.getMessage());
        }
    }

    private void writeSummary(Path file, TaxYear[] taxYears, BigDecimal totalIncome,
            BigDecimal totalExpenses, BigDecimal netProfit, int incomeCount, int expenseCount)
            throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("===== UK Self-Employment Financial Summary =====");
            writer.newLine();
            writer.newLine();
            writer.write("Export Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            writer.newLine();
            writer.write("Tax Years: " + Arrays.stream(taxYears)
                .map(TaxYear::label)
                .collect(Collectors.joining(", ")));
            writer.newLine();
            writer.newLine();
            writer.write("----- Totals -----");
            writer.newLine();
            writer.write(String.format("Total Income: GBP %.2f (%d records)", totalIncome, incomeCount));
            writer.newLine();
            writer.write(String.format("Total Expenses: GBP %.2f (%d records)", totalExpenses, expenseCount));
            writer.newLine();
            writer.write(String.format("Net Profit: GBP %.2f", netProfit));
            writer.newLine();
            writer.newLine();
            writer.write("----- Files Generated -----");
            writer.newLine();
            writer.write("- income_*.csv: All income records");
            writer.newLine();
            writer.write("- expenses_*.csv: All expense records");
            writer.newLine();
            writer.write("- summary_*.txt: This summary file");
            writer.newLine();
        }
    }

    private void validateInputs(UUID businessId, TaxYear[] taxYears) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        if (taxYears == null || taxYears.length == 0) {
            throw new IllegalArgumentException("At least one tax year must be specified");
        }
    }

    private Map<String, Object> incomeToMap(Income income) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", income.id().toString());
        map.put("date", income.date().toString());
        map.put("amount", income.amount().toPlainString());
        map.put("description", income.description());
        map.put("category", income.category().name());
        map.put("reference", income.reference());
        return map;
    }

    private Map<String, Object> expenseToMap(Expense expense) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", expense.id().toString());
        map.put("date", expense.date().toString());
        map.put("amount", expense.amount().toPlainString());
        map.put("description", expense.description());
        map.put("category", expense.category().name());
        map.put("sa103Box", expense.category().getSa103Box());
        map.put("allowable", expense.category().isAllowable());
        map.put("receiptPath", expense.receiptPath());
        map.put("notes", expense.notes());
        return map;
    }

    private String formatCsvRow(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escapeCsv(values[i]));
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
