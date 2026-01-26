package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SQLite-backed expense repository.
 * All operations directly query the database - no in-memory caching.
 * This ensures data is never lost and is always consistent.
 */
public class SqliteExpenseRepository {

    private final SqliteDataStore dataStore;
    private final UUID businessId;

    /**
     * Creates a new repository for the given business.
     *
     * @param businessId The business ID for all operations
     */
    public SqliteExpenseRepository(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.dataStore = SqliteDataStore.getInstance();
        // Ensure business exists for FK constraints
        dataStore.ensureBusinessExists(businessId);
    }

    /**
     * Saves an expense to the database.
     *
     * @param expense The expense to save
     * @return The saved expense
     */
    public Expense save(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null");
        }
        dataStore.saveExpense(expense);
        return expense;
    }

    /**
     * Finds an expense by ID.
     *
     * @param id The expense ID
     * @return The expense if found
     */
    public Optional<Expense> findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Expense ID cannot be null");
        }
        return dataStore.findExpenseById(id);
    }

    /**
     * Finds all expenses for this business.
     *
     * @return All expenses sorted by date descending
     */
    public List<Expense> findAll() {
        return dataStore.findExpensesByBusinessId(businessId);
    }

    /**
     * Finds expenses by tax year.
     *
     * @param taxYear The tax year to filter by
     * @return Expenses within the tax year
     */
    public List<Expense> findByTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return dataStore.findExpensesByDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Finds expenses by category.
     *
     * @param category The category to filter by
     * @return Expenses with the given category
     */
    public List<Expense> findByCategory(ExpenseCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        return dataStore.findExpensesByBusinessId(businessId).stream()
                .filter(e -> e.category() == category)
                .collect(Collectors.toList());
    }

    /**
     * Gets the total expenses for a tax year.
     *
     * @param taxYear The tax year
     * @return Total expenses amount
     */
    public BigDecimal getTotalByTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return dataStore.calculateTotalExpenses(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Gets the allowable (deductible) expenses for a tax year.
     *
     * @param taxYear The tax year
     * @return Total allowable expenses amount
     */
    public BigDecimal getAllowableTotalByTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return dataStore.calculateAllowableExpenses(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Gets the allowable (deductible) expenses for a specific date range.
     * Sprint 10D: SE-10D-003 - Cumulative Totals Display
     *
     * @param startDate The start date (inclusive)
     * @param endDate   The end date (inclusive)
     * @return Total allowable expenses amount for the date range
     */
    public BigDecimal getAllowableTotalForDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        return dataStore.calculateAllowableExpenses(businessId, startDate, endDate);
    }

    /**
     * Gets expense totals grouped by category for a tax year.
     *
     * @param taxYear The tax year
     * @return Map of category to total amount
     */
    public Map<ExpenseCategory, BigDecimal> getTotalsByCategoryForTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return findByTaxYear(taxYear).stream()
                .collect(Collectors.groupingBy(
                        Expense::category,
                        Collectors.reducing(BigDecimal.ZERO, Expense::amount, BigDecimal::add)
                ));
    }

    /**
     * Deletes an expense by ID.
     *
     * @param id The expense ID
     * @return true if deleted, false if not found
     */
    public boolean delete(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Expense ID cannot be null");
        }
        return dataStore.deleteExpense(id);
    }

    /**
     * Returns the count of all expenses for this business.
     *
     * @return The expense count
     */
    public long count() {
        return dataStore.findExpensesByBusinessId(businessId).size();
    }

    /**
     * Returns the business ID for this repository.
     *
     * @return The business ID
     */
    public UUID getBusinessId() {
        return businessId;
    }
}
