package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
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

/**
 * In-memory implementation of ExpenseService for UI standalone mode.
 * Wraps the InMemoryExpenseRepository to provide the same interface as ExpenseService.
 */
public class InMemoryExpenseService extends ExpenseService {

    private static final int MAX_DESCRIPTION_LENGTH = 100;
    private final InMemoryExpenseRepository repository;

    public InMemoryExpenseService() {
        super(null); // No Panache repository in standalone mode
        this.repository = new InMemoryExpenseRepository();
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
        return repository.findByIdAsDomain(id);
    }

    @Override
    public Expense update(UUID id, LocalDate date, BigDecimal amount,
                          String description, ExpenseCategory category,
                          String receiptPath, String notes) {
        if (id == null) {
            throw new ValidationException("id", "Expense id cannot be null");
        }

        Expense existingExpense = repository.findByIdAsDomain(id)
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
            notes
        );

        return repository.update(updatedExpense);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Expense id cannot be null");
        }
        return repository.deleteByIdAndReturn(id);
    }

    @Override
    public List<Expense> findByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        return repository.findByDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    @Override
    public List<Expense> findByCategory(UUID businessId, ExpenseCategory category) {
        validateBusinessId(businessId);
        validateCategory(category);
        return repository.findByCategory(businessId, category);
    }

    @Override
    public BigDecimal getTotalByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        return repository.calculateTotalForDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    @Override
    public BigDecimal getDeductibleTotal(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        return repository.calculateAllowableTotalForDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Gets totals grouped by category for SA103 form.
     */
    public Map<ExpenseCategory, BigDecimal> getTotalsByCategory(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        return repository.calculateTotalsByCategoryForDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Clears all data (useful for testing).
     */
    public void clearAll() {
        repository.clear();
    }

    /**
     * Returns the count of all expenses.
     */
    public long count() {
        return repository.count();
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
        // Allow dates in any tax year for flexibility
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
