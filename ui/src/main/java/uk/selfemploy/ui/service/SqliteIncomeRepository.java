package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SQLite-backed income repository.
 * All operations directly query the database - no in-memory caching.
 * This ensures data is never lost and is always consistent.
 */
public class SqliteIncomeRepository {

    private final SqliteDataStore dataStore;
    private final UUID businessId;

    /**
     * Creates a new repository for the given business.
     *
     * @param businessId The business ID for all operations
     */
    public SqliteIncomeRepository(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.dataStore = SqliteDataStore.getInstance();
        // Ensure business exists for FK constraints
        dataStore.ensureBusinessExists(businessId);
    }

    /**
     * Saves an income entry to the database.
     *
     * @param income The income to save
     * @return The saved income
     */
    public Income save(Income income) {
        if (income == null) {
            throw new IllegalArgumentException("Income cannot be null");
        }
        dataStore.saveIncome(income);
        return income;
    }

    /**
     * Finds an income entry by ID.
     *
     * @param id The income ID
     * @return The income if found
     */
    public Optional<Income> findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Income ID cannot be null");
        }
        return dataStore.findIncomeById(id);
    }

    /**
     * Finds all income for this business.
     *
     * @return All income sorted by date descending
     */
    public List<Income> findAll() {
        return dataStore.findIncomeByBusinessId(businessId);
    }

    /**
     * Finds income by tax year.
     *
     * @param taxYear The tax year to filter by
     * @return Income within the tax year
     */
    public List<Income> findByTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return dataStore.findIncomeByDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Finds income by category.
     *
     * @param category The category to filter by
     * @return Income with the given category
     */
    public List<Income> findByCategory(IncomeCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be null");
        }
        return dataStore.findIncomeByBusinessId(businessId).stream()
                .filter(i -> i.category() == category)
                .collect(Collectors.toList());
    }

    /**
     * Gets the total income for a tax year.
     *
     * @param taxYear The tax year
     * @return Total income amount
     */
    public BigDecimal getTotalByTaxYear(TaxYear taxYear) {
        if (taxYear == null) {
            throw new IllegalArgumentException("Tax year cannot be null");
        }
        return dataStore.calculateTotalIncome(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Gets the total income for a specific date range.
     * Sprint 10D: SE-10D-003 - Cumulative Totals Display
     *
     * @param startDate The start date (inclusive)
     * @param endDate   The end date (inclusive)
     * @return Total income amount for the date range
     */
    public BigDecimal getTotalForDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        return dataStore.calculateTotalIncome(businessId, startDate, endDate);
    }

    /**
     * Deletes an income entry by ID.
     *
     * @param id The income ID
     * @return true if deleted, false if not found
     */
    public boolean delete(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Income ID cannot be null");
        }
        return dataStore.deleteIncome(id);
    }

    /**
     * Returns the count of all income for this business.
     *
     * @return The income count
     */
    public long count() {
        return dataStore.findIncomeByBusinessId(businessId).size();
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
