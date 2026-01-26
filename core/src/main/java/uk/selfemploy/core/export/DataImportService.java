package uk.selfemploy.core.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for importing income and expense data from CSV and JSON files.
 *
 * <p>Features:
 * <ul>
 *   <li>CSV import for income and expense records</li>
 *   <li>JSON import from previous exports</li>
 *   <li>Preview before import with validation</li>
 *   <li>Duplicate detection and handling</li>
 *   <li>Detailed error reporting</li>
 * </ul>
 */
@ApplicationScoped
public class DataImportService {

    private static final String[] INCOME_HEADERS = {"Date", "Amount", "Description", "Category", "Reference"};
    private static final String[] EXPENSE_HEADERS = {"Date", "Amount", "Description", "Category", "Notes"};

    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final ObjectMapper objectMapper;

    @Inject
    public DataImportService(IncomeService incomeService, ExpenseService expenseService) {
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Previews a CSV file import without actually importing.
     *
     * @param filePath   Path to the CSV file
     * @param importType Type of data (INCOME or EXPENSE)
     * @return Import preview with validation results
     */
    public ImportPreview previewCsvImport(Path filePath, ImportType importType) {
        validateFilePath(filePath);

        try {
            List<String[]> rows = parseCsvFileInternal(filePath);
            if (rows.isEmpty()) {
                return ImportPreview.invalid(List.of("File is empty or contains only headers"));
            }

            String[] expectedHeaders = importType == ImportType.INCOME ? INCOME_HEADERS : EXPENSE_HEADERS;
            String[] actualHeaders = rows.get(0);

            // Validate headers
            List<String> headerErrors = validateHeaders(actualHeaders, expectedHeaders);
            if (!headerErrors.isEmpty()) {
                return ImportPreview.invalid(headerErrors);
            }

            // Validate data rows
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            int validCount = 0;
            int invalidCount = 0;

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                List<String> rowErrors = validateRow(row, importType, i);

                if (rowErrors.isEmpty()) {
                    validCount++;
                } else {
                    invalidCount++;
                    errors.addAll(rowErrors);
                }
            }

            if (validCount == 0 && invalidCount == 0) {
                warnings.add("No records to import");
                return ImportPreview.valid(0, warnings);
            }

            if (invalidCount > 0) {
                return ImportPreview.partial(validCount, invalidCount, warnings, errors);
            }

            return ImportPreview.valid(validCount, warnings);

        } catch (IOException e) {
            throw new ImportException("Failed to read file: " + e.getMessage(), e);
        }
    }

    /**
     * Previews a JSON file import without actually importing.
     *
     * @param filePath Path to the JSON file
     * @return Import preview with validation results
     */
    public ImportPreview previewJsonImport(Path filePath) {
        validateFilePath(filePath);

        try {
            JsonNode root = objectMapper.readTree(filePath.toFile());

            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Validate structure
            if (!root.has("metadata")) {
                errors.add("Missing 'metadata' section - invalid export file structure");
            }
            if (!root.has("incomes") && !root.has("expenses")) {
                errors.add("Missing 'incomes' and 'expenses' sections - invalid export file structure");
            }

            if (!errors.isEmpty()) {
                return ImportPreview.invalid(errors);
            }

            int incomeCount = root.has("incomes") ? root.get("incomes").size() : 0;
            int expenseCount = root.has("expenses") ? root.get("expenses").size() : 0;
            int totalCount = incomeCount + expenseCount;

            // Validate each record
            int validCount = 0;
            int invalidCount = 0;

            if (root.has("incomes")) {
                for (int i = 0; i < root.get("incomes").size(); i++) {
                    JsonNode income = root.get("incomes").get(i);
                    List<String> recordErrors = validateJsonIncome(income, i);
                    if (recordErrors.isEmpty()) {
                        validCount++;
                    } else {
                        invalidCount++;
                        errors.addAll(recordErrors);
                    }
                }
            }

            if (root.has("expenses")) {
                for (int i = 0; i < root.get("expenses").size(); i++) {
                    JsonNode expense = root.get("expenses").get(i);
                    List<String> recordErrors = validateJsonExpense(expense, i);
                    if (recordErrors.isEmpty()) {
                        validCount++;
                    } else {
                        invalidCount++;
                        errors.addAll(recordErrors);
                    }
                }
            }

            if (invalidCount > 0) {
                return ImportPreview.partial(validCount, invalidCount, warnings, errors);
            }

            return ImportPreview.valid(validCount, warnings);

        } catch (IOException e) {
            throw new ImportException("Failed to read JSON file: " + e.getMessage(), e);
        }
    }

    /**
     * Imports data from a CSV file.
     *
     * @param businessId Business ID to import data for
     * @param filePath   Path to the CSV file
     * @param importType Type of data (INCOME or EXPENSE)
     * @param options    Import options
     * @return Import result
     */
    public ImportResult importCsv(UUID businessId, Path filePath, ImportType importType, ImportOptions options) {
        validateFilePath(filePath);
        validateBusinessId(businessId);

        try {
            List<String[]> rows = parseCsvFileInternal(filePath);
            if (rows.size() <= 1) {
                return ImportResult.success(0, 0, 0);
            }

            int importedCount = 0;
            int skippedCount = 0;
            int duplicateCount = 0;
            List<String> errors = new ArrayList<>();

            // Track seen records for duplicate detection
            Set<String> seenRecords = new HashSet<>();

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                List<String> rowErrors = validateRow(row, importType, i);

                if (!rowErrors.isEmpty()) {
                    skippedCount++;
                    errors.addAll(rowErrors);
                    continue;
                }

                // Check for duplicates within file
                String recordKey = generateRecordKey(row);
                if (seenRecords.contains(recordKey)) {
                    if (options.skipDuplicatesEnabled()) {
                        duplicateCount++;
                        continue;
                    }
                }
                seenRecords.add(recordKey);

                try {
                    if (importType == ImportType.INCOME) {
                        importIncomeRow(businessId, row);
                    } else {
                        importExpenseRow(businessId, row);
                    }
                    importedCount++;
                } catch (Exception e) {
                    skippedCount++;
                    errors.add("Row " + i + ": " + e.getMessage());
                }
            }

            return ImportResult.partial(importedCount, skippedCount, duplicateCount, errors.size(), errors);

        } catch (IOException e) {
            return ImportResult.failure("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Imports data from a JSON export file.
     *
     * @param businessId Business ID to import data for
     * @param filePath   Path to the JSON file
     * @param options    Import options
     * @return Import result
     */
    public ImportResult importJson(UUID businessId, Path filePath, ImportOptions options) {
        validateFilePath(filePath);
        validateBusinessId(businessId);

        try {
            JsonNode root = objectMapper.readTree(filePath.toFile());

            int importedCount = 0;
            int skippedCount = 0;
            List<String> errors = new ArrayList<>();

            // Import incomes
            if (root.has("incomes")) {
                for (JsonNode income : root.get("incomes")) {
                    try {
                        importJsonIncome(businessId, income);
                        importedCount++;
                    } catch (Exception e) {
                        skippedCount++;
                        errors.add("Income: " + e.getMessage());
                    }
                }
            }

            // Import expenses
            if (root.has("expenses")) {
                for (JsonNode expense : root.get("expenses")) {
                    try {
                        importJsonExpense(businessId, expense);
                        importedCount++;
                    } catch (Exception e) {
                        skippedCount++;
                        errors.add("Expense: " + e.getMessage());
                    }
                }
            }

            return ImportResult.partial(importedCount, skippedCount, 0, errors.size(), errors);

        } catch (IOException e) {
            return ImportResult.failure("Failed to read JSON file: " + e.getMessage());
        }
    }

    /**
     * Parses a JSON export file and returns the parsed incomes and expenses.
     * Does not import the records - just parses them for review.
     *
     * @param filePath Path to the JSON file
     * @return Parsed data containing income and expense lists
     */
    public ParsedJsonData parseJsonFile(Path filePath) {
        validateFilePath(filePath);

        try {
            JsonNode root = objectMapper.readTree(filePath.toFile());
            List<Income> incomes = new ArrayList<>();
            List<Expense> expenses = new ArrayList<>();

            // Parse incomes
            if (root.has("incomes")) {
                for (JsonNode incomeNode : root.get("incomes")) {
                    try {
                        Income income = parseJsonIncomeNode(incomeNode);
                        incomes.add(income);
                    } catch (Exception e) {
                        // Skip invalid records
                    }
                }
            }

            // Parse expenses
            if (root.has("expenses")) {
                for (JsonNode expenseNode : root.get("expenses")) {
                    try {
                        Expense expense = parseJsonExpenseNode(expenseNode);
                        expenses.add(expense);
                    } catch (Exception e) {
                        // Skip invalid records
                    }
                }
            }

            return new ParsedJsonData(incomes, expenses);

        } catch (IOException e) {
            throw new ImportException("Failed to read JSON file: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a CSV file and returns a list of Income domain objects.
     * Does not import the records - just parses them for review.
     *
     * @param filePath Path to the CSV file
     * @param importType Type of data (INCOME or EXPENSE)
     * @return List of parsed Income objects
     */
    public List<Income> parseCsvFile(Path filePath, ImportType importType) {
        validateFilePath(filePath);

        if (importType != ImportType.INCOME) {
            throw new IllegalArgumentException("Only INCOME type is supported for CSV parsing");
        }

        try {
            List<String[]> rows = parseCsvFileInternal(filePath);
            List<Income> incomes = new ArrayList<>();

            // Skip header row
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                List<String> rowErrors = validateRow(row, importType, i);

                if (rowErrors.isEmpty()) {
                    try {
                        LocalDate date = LocalDate.parse(row[0].trim());
                        BigDecimal amount = new BigDecimal(row[1].trim());
                        String description = row[2].trim();
                        IncomeCategory category = IncomeCategory.valueOf(row[3].trim().toUpperCase());
                        String reference = row.length > 4 ? row[4].trim() : null;
                        // Use placeholder business ID - will be replaced during actual import
                        UUID placeholderBusinessId = UUID.fromString("00000000-0000-0000-0000-000000000000");
                        Income income = new Income(
                            UUID.randomUUID(),
                            placeholderBusinessId,
                            date,
                            amount,
                            description,
                            category,
                            reference,
                            null, // bankTransactionRef
                            null, // invoiceNumber
                            null  // receiptPath
                        );
                        incomes.add(income);
                    } catch (Exception e) {
                        // Skip invalid rows
                    }
                }
            }

            return incomes;

        } catch (IOException e) {
            throw new ImportException("Failed to read CSV file: " + e.getMessage(), e);
        }
    }

    private Income parseJsonIncomeNode(JsonNode income) {
        LocalDate date = LocalDate.parse(income.get("date").asText());
        BigDecimal amount = new BigDecimal(income.get("amount").asText());
        String description = income.get("description").asText();
        IncomeCategory category = IncomeCategory.valueOf(income.get("category").asText().toUpperCase());
        String reference = income.has("reference") && !income.get("reference").isNull()
            ? income.get("reference").asText() : null;
        // Use placeholder business ID - will be replaced during actual import
        UUID placeholderBusinessId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String bankTransactionRef = income.has("bankTransactionRef") && !income.get("bankTransactionRef").isNull()
            ? income.get("bankTransactionRef").asText() : null;
        String invoiceNumber = income.has("invoiceNumber") && !income.get("invoiceNumber").isNull()
            ? income.get("invoiceNumber").asText() : null;
        String receiptPath = income.has("receiptPath") && !income.get("receiptPath").isNull()
            ? income.get("receiptPath").asText() : null;
        return new Income(UUID.randomUUID(), placeholderBusinessId, date, amount, description, category, reference,
            bankTransactionRef, invoiceNumber, receiptPath);
    }

    private Expense parseJsonExpenseNode(JsonNode expense) {
        LocalDate date = LocalDate.parse(expense.get("date").asText());
        BigDecimal amount = new BigDecimal(expense.get("amount").asText());
        String description = expense.get("description").asText();
        ExpenseCategory category = ExpenseCategory.valueOf(expense.get("category").asText().toUpperCase());
        String notes = expense.has("notes") && !expense.get("notes").isNull()
            ? expense.get("notes").asText() : null;
        // Use placeholder business ID - will be replaced during actual import
        UUID placeholderBusinessId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String bankTransactionRef = expense.has("bankTransactionRef") && !expense.get("bankTransactionRef").isNull()
            ? expense.get("bankTransactionRef").asText() : null;
        String supplierRef = expense.has("supplierRef") && !expense.get("supplierRef").isNull()
            ? expense.get("supplierRef").asText() : null;
        String invoiceNumber = expense.has("invoiceNumber") && !expense.get("invoiceNumber").isNull()
            ? expense.get("invoiceNumber").asText() : null;
        return new Expense(UUID.randomUUID(), placeholderBusinessId, date, amount, description, category, null, notes,
            bankTransactionRef, supplierRef, invoiceNumber);
    }

    /**
     * Result record for parsed JSON data.
     */
    public record ParsedJsonData(List<Income> incomes, List<Expense> expenses) {}

    // Validation methods

    private void validateFilePath(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }
        if (!Files.exists(filePath)) {
            throw new ImportException("File not found: " + filePath);
        }
    }

    private void validateBusinessId(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
    }

    private List<String> validateHeaders(String[] actual, String[] expected) {
        List<String> errors = new ArrayList<>();

        if (actual.length < expected.length) {
            errors.add("Invalid header: Expected " + expected.length + " columns, found " + actual.length);
            return errors;
        }

        for (int i = 0; i < expected.length; i++) {
            if (!actual[i].trim().equalsIgnoreCase(expected[i])) {
                errors.add("Invalid header at column " + (i + 1) + ": Expected '" + expected[i] + "', found '" + actual[i].trim() + "'");
            }
        }

        return errors;
    }

    private List<String> validateRow(String[] row, ImportType importType, int rowNumber) {
        List<String> errors = new ArrayList<>();
        int expectedColumns = importType == ImportType.INCOME ? 5 : 5;

        if (row.length < expectedColumns - 1) { // Allow missing optional last column
            errors.add("Row " + rowNumber + ": Insufficient columns");
            return errors;
        }

        // Validate date (column 0)
        try {
            LocalDate.parse(row[0].trim());
        } catch (DateTimeParseException e) {
            errors.add("Row " + rowNumber + ": Invalid date format '" + row[0] + "'. Expected YYYY-MM-DD");
        }

        // Validate amount (column 1)
        try {
            BigDecimal amount = new BigDecimal(row[1].trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Row " + rowNumber + ": Amount must be positive");
            }
        } catch (NumberFormatException e) {
            errors.add("Row " + rowNumber + ": Invalid amount '" + row[1] + "'");
        }

        // Validate description (column 2)
        if (row[2] == null || row[2].trim().isEmpty()) {
            errors.add("Row " + rowNumber + ": Description cannot be empty");
        }

        // Validate category (column 3)
        String categoryStr = row[3].trim().toUpperCase();
        if (importType == ImportType.INCOME) {
            try {
                IncomeCategory.valueOf(categoryStr);
            } catch (IllegalArgumentException e) {
                errors.add("Row " + rowNumber + ": Invalid income category '" + row[3] + "'. Valid: " +
                    Arrays.toString(IncomeCategory.values()));
            }
        } else {
            try {
                ExpenseCategory.valueOf(categoryStr);
            } catch (IllegalArgumentException e) {
                errors.add("Row " + rowNumber + ": Invalid expense category '" + row[3] + "'. Valid categories: OFFICE_COSTS, TRAVEL, etc.");
            }
        }

        return errors;
    }

    private List<String> validateJsonIncome(JsonNode income, int index) {
        List<String> errors = new ArrayList<>();

        if (!income.has("date")) {
            errors.add("Income " + index + ": Missing 'date' field");
        }
        if (!income.has("amount")) {
            errors.add("Income " + index + ": Missing 'amount' field");
        }
        if (!income.has("description")) {
            errors.add("Income " + index + ": Missing 'description' field");
        }
        if (!income.has("category")) {
            errors.add("Income " + index + ": Missing 'category' field");
        }

        return errors;
    }

    private List<String> validateJsonExpense(JsonNode expense, int index) {
        List<String> errors = new ArrayList<>();

        if (!expense.has("date")) {
            errors.add("Expense " + index + ": Missing 'date' field");
        }
        if (!expense.has("amount")) {
            errors.add("Expense " + index + ": Missing 'amount' field");
        }
        if (!expense.has("description")) {
            errors.add("Expense " + index + ": Missing 'description' field");
        }
        if (!expense.has("category")) {
            errors.add("Expense " + index + ": Missing 'category' field");
        }

        return errors;
    }

    // Import methods

    private void importIncomeRow(UUID businessId, String[] row) {
        LocalDate date = LocalDate.parse(row[0].trim());
        BigDecimal amount = new BigDecimal(row[1].trim());
        String description = row[2].trim();
        IncomeCategory category = IncomeCategory.valueOf(row[3].trim().toUpperCase());
        String reference = row.length > 4 ? row[4].trim() : null;

        incomeService.create(businessId, date, amount, description, category, reference);
    }

    private void importExpenseRow(UUID businessId, String[] row) {
        LocalDate date = LocalDate.parse(row[0].trim());
        BigDecimal amount = new BigDecimal(row[1].trim());
        String description = row[2].trim();
        ExpenseCategory category = ExpenseCategory.valueOf(row[3].trim().toUpperCase());
        String notes = row.length > 4 ? row[4].trim() : null;

        expenseService.create(businessId, date, amount, description, category, null, notes);
    }

    private void importJsonIncome(UUID businessId, JsonNode income) {
        LocalDate date = LocalDate.parse(income.get("date").asText());
        BigDecimal amount = new BigDecimal(income.get("amount").asText());
        String description = income.get("description").asText();
        IncomeCategory category = IncomeCategory.valueOf(income.get("category").asText().toUpperCase());
        String reference = income.has("reference") && !income.get("reference").isNull()
            ? income.get("reference").asText() : null;

        incomeService.create(businessId, date, amount, description, category, reference);
    }

    private void importJsonExpense(UUID businessId, JsonNode expense) {
        LocalDate date = LocalDate.parse(expense.get("date").asText());
        BigDecimal amount = new BigDecimal(expense.get("amount").asText());
        String description = expense.get("description").asText();
        ExpenseCategory category = ExpenseCategory.valueOf(expense.get("category").asText().toUpperCase());
        String notes = expense.has("notes") && !expense.get("notes").isNull()
            ? expense.get("notes").asText() : null;

        expenseService.create(businessId, date, amount, description, category, null, notes);
    }

    // Helper methods

    private List<String[]> parseCsvFileInternal(Path filePath) throws IOException {
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    rows.add(parseCsvLine(line));
                }
            }
        }

        return rows;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // Skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());

        return values.toArray(new String[0]);
    }

    private String generateRecordKey(String[] row) {
        // Key based on date, amount, description
        return row[0] + "|" + row[1] + "|" + row[2];
    }
}
