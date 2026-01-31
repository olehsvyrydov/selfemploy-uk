package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.core.service.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SQLite-backed implementation of ExpenseService.
 * All operations go directly to the SQLite database - no in-memory caching.
 * This ensures data is never lost.
 */
public class SqliteExpenseService extends ExpenseService {

    private static final int MAX_DESCRIPTION_LENGTH = 100;
    private final SqliteExpenseRepository repository;
    private final UUID businessId;

    public SqliteExpenseService(UUID businessId) {
        super(null); // No Panache repository in standalone mode
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.repository = new SqliteExpenseRepository(businessId);
    }

    @Override
    public Expense create(UUID businessId, LocalDate date, BigDecimal amount,
                          String description, ExpenseCategory category,
                          String receiptPath, String notes) {
        validateBusinessId(businessId);
        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);

        Expense expense = Expense.create(businessId, date, amount, description, category, receiptPath, notes);
        return repository.save(expense);
    }

    @Override
    public Optional<Expense> findById(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Expense id cannot be null");
        }
        return repository.findById(id);
    }

    @Override
    public Expense update(UUID id, LocalDate date, BigDecimal amount,
                          String description, ExpenseCategory category,
                          String receiptPath, String notes) {
        if (id == null) {
            throw new ValidationException("id", "Expense id cannot be null");
        }

        Expense existingExpense = repository.findById(id)
                .orElseThrow(() -> new ValidationException("id", "Expense not found: " + id));

        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);

        Expense updatedExpense = new Expense(
                existingExpense.id(),
                existingExpense.businessId(),
                date,
                amount,
                description,
                category,
                receiptPath,
                notes,
                existingExpense.bankTransactionRef(),
                existingExpense.supplierRef(),
                existingExpense.invoiceNumber()
        );

        return repository.save(updatedExpense);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Expense id cannot be null");
        }
        return repository.delete(id);
    }

    @Override
    public List<Expense> findByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        return repository.findByTaxYear(taxYear);
    }

    @Override
    public List<Expense> findByCategory(UUID businessId, ExpenseCategory category) {
        validateBusinessId(businessId);
        validateCategory(category);
        return repository.findByCategory(category);
    }

    @Override
    public BigDecimal getTotalByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        return repository.getTotalByTaxYear(taxYear);
    }

    @Override
    public BigDecimal getDeductibleTotal(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        return repository.getAllowableTotalByTaxYear(taxYear);
    }

    /**
     * Gets the total deductible expenses for a specific quarter within a tax year.
     * Sprint 10D: SE-10D-003 - Cumulative Totals Display
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @param quarter    The quarter
     * @return Total deductible expense amount for the quarter
     */
    @Override
    public BigDecimal getDeductibleTotalByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        if (quarter == null) {
            throw new ValidationException("quarter", "Quarter cannot be null");
        }
        return repository.getAllowableTotalForDateRange(quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
    }

    /**
     * Finds all expenses for a business within a specific quarter.
     * SE-10G-003: Override to use SQLite repository instead of null parent repository.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @param quarter    The quarter
     * @return List of expenses within the quarter
     */
    @Override
    public List<Expense> findByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        if (quarter == null) {
            throw new ValidationException("quarter", "Quarter cannot be null");
        }
        return repository.findByDateRange(quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
    }

    /**
     * Gets expense totals grouped by category for a specific quarter.
     * SE-10G-003: Override to use SQLite repository instead of null parent repository.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @param quarter    The quarter
     * @return Map of expense categories to totals
     */
    @Override
    public Map<ExpenseCategory, BigDecimal> getTotalsByCategoryByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        if (quarter == null) {
            throw new ValidationException("quarter", "Quarter cannot be null");
        }
        List<Expense> expenses = repository.findByDateRange(quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::category,
                        Collectors.reducing(BigDecimal.ZERO, Expense::amount, BigDecimal::add)
                ));
    }

    /**
     * Gets totals grouped by category for SA103 form.
     */
    public Map<ExpenseCategory, BigDecimal> getTotalsByCategory(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        return repository.getTotalsByCategoryForTaxYear(taxYear);
    }

    /**
     * Returns the count of all expenses.
     */
    public long count() {
        return repository.count();
    }

    /**
     * Returns the business ID for this service.
     */
    public UUID getBusinessId() {
        return businessId;
    }

    // === Validation Methods ===

    private void validateBusinessId(UUID businessId) {
        if (businessId == null) {
            throw new ValidationException("businessId", "Business ID cannot be null");
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException("date", "Expense date cannot be null");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("amount", "Expense amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount", "Expense amount must be positive");
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new ValidationException("description", "Expense description cannot be null or empty");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("description",
                    String.format("Expense description cannot exceed %d characters", MAX_DESCRIPTION_LENGTH));
        }
    }

    private void validateCategory(ExpenseCategory category) {
        if (category == null) {
            throw new ValidationException("category", "Expense category cannot be null");
        }
    }
}
