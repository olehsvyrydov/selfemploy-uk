package uk.selfemploy.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.persistence.repository.ExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for managing Expense entities.
 *
 * Provides CRUD operations with validation and business rule enforcement.
 * Expense categories are aligned with HMRC SA103 form categories.
 * Expense dates are validated to be within a valid UK tax year range.
 */
@ApplicationScoped
public class ExpenseService {

    private static final int MAX_DESCRIPTION_LENGTH = 100;

    private final ExpenseRepository expenseRepository;

    @Inject
    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    /**
     * Creates a new expense entry.
     *
     * @param businessId   The business ID (required)
     * @param date         The expense date (must be within a valid tax year)
     * @param amount       The expense amount (must be positive)
     * @param description  The description (required, max 100 chars)
     * @param category     The expense category (required, must be valid SA103 category)
     * @param receiptPath  Optional path to receipt file
     * @param notes        Optional notes about the expense
     * @return The created expense
     * @throws ValidationException if validation fails
     */
    public Expense create(UUID businessId, LocalDate date, BigDecimal amount,
                          String description, ExpenseCategory category,
                          String receiptPath, String notes) {
        validateBusinessId(businessId);
        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);

        Expense expense = Expense.create(businessId, date, amount, description, category, receiptPath, notes);
        return expenseRepository.save(expense);
    }

    /**
     * Finds an expense by ID.
     *
     * @param id The expense ID
     * @return Optional containing the expense if found
     * @throws ValidationException if id is null
     */
    public Optional<Expense> findById(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Expense id cannot be null");
        }
        return expenseRepository.findByIdAsDomain(id);
    }

    /**
     * Updates an existing expense.
     *
     * @param id          The expense ID
     * @param date        The updated date
     * @param amount      The updated amount
     * @param description The updated description
     * @param category    The updated category
     * @param receiptPath The updated receipt path
     * @param notes       The updated notes
     * @return The updated expense
     * @throws ValidationException if expense not found or validation fails
     */
    public Expense update(UUID id, LocalDate date, BigDecimal amount,
                          String description, ExpenseCategory category,
                          String receiptPath, String notes) {
        if (id == null) {
            throw new ValidationException("id", "Expense id cannot be null");
        }

        Expense existingExpense = expenseRepository.findByIdAsDomain(id)
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

        return expenseRepository.update(updatedExpense);
    }

    /**
     * Deletes an expense by ID.
     *
     * @param id The expense ID
     * @return true if deleted, false if not found
     * @throws ValidationException if id is null or expense is linked to HMRC submission
     */
    public boolean delete(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Expense id cannot be null");
        }

        Optional<Expense> existingExpense = expenseRepository.findByIdAsDomain(id);
        if (existingExpense.isEmpty()) {
            return false;
        }

        // TODO: Check if linked to HMRC submission and throw ValidationException
        // if (isLinkedToHmrcSubmission(id)) {
        //     throw new ValidationException("id", "Cannot delete expense linked to HMRC submission");
        // }

        return expenseRepository.deleteByIdAndReturn(id);
    }

    /**
     * Finds all expenses for a business within a tax year.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @return List of expenses within the tax year
     * @throws ValidationException if businessId or taxYear is null
     */
    public List<Expense> findByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }

        return expenseRepository.findByDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Finds all expenses for a business by category.
     *
     * @param businessId The business ID
     * @param category   The expense category
     * @return List of expenses matching the category
     * @throws ValidationException if businessId or category is null
     */
    public List<Expense> findByCategory(UUID businessId, ExpenseCategory category) {
        validateBusinessId(businessId);
        validateCategory(category);

        return expenseRepository.findByCategory(businessId, category);
    }

    /**
     * Gets the total expenses for a business within a tax year.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @return Total expense amount
     * @throws ValidationException if businessId or taxYear is null
     */
    public BigDecimal getTotalByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }

        return expenseRepository.calculateTotalForDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Gets the total deductible (allowable) expenses for a business within a tax year.
     * Only includes expenses that are allowable for tax deduction according to HMRC rules.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @return Total allowable expense amount
     * @throws ValidationException if businessId or taxYear is null
     */
    public BigDecimal getDeductibleTotal(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }

        return expenseRepository.calculateAllowableTotalForDateRange(
                businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Gets the total deductible (allowable) expenses for a business within a specific quarter.
     * Sprint 10D: SE-10D-003 - Cumulative Totals Display
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @param quarter    The quarter
     * @return Total allowable expense amount for the quarter
     * @throws ValidationException if any parameter is null
     */
    public BigDecimal getDeductibleTotalByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        if (quarter == null) {
            throw new ValidationException("quarter", "Quarter cannot be null");
        }

        return expenseRepository.calculateAllowableTotalForDateRange(
                businessId, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
    }

    /**
     * Finds all expenses for a business within a specific quarter.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @param quarter    The quarter
     * @return List of expenses within the quarter
     * @throws ValidationException if any parameter is null
     */
    public List<Expense> findByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        if (quarter == null) {
            throw new ValidationException("quarter", "Quarter cannot be null");
        }

        return expenseRepository.findByDateRange(
                businessId, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
    }

    /**
     * Gets expense totals grouped by category for a business within a specific quarter.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @param quarter    The quarter
     * @return Map of expense categories to totals
     * @throws ValidationException if any parameter is null
     */
    public Map<ExpenseCategory, BigDecimal> getTotalsByCategoryByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        if (quarter == null) {
            throw new ValidationException("quarter", "Quarter cannot be null");
        }

        return expenseRepository.calculateTotalsByCategoryForDateRange(
                businessId, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
    }

    private void validateBusinessId(UUID businessId) {
        if (businessId == null) {
            throw new ValidationException("businessId", "Business ID cannot be null");
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException("date", "Expense date cannot be null");
        }

        // Determine which tax year this date should belong to
        TaxYear taxYear = determineTaxYear(date);
        if (!taxYear.contains(date)) {
            throw new ValidationException("date",
                    String.format("Expense date %s is not within a valid tax year", date));
        }
    }

    /**
     * Determines the tax year that a date should belong to.
     * UK tax years run from 6 April to 5 April.
     */
    private TaxYear determineTaxYear(LocalDate date) {
        int year = date.getYear();
        // If date is before April 6th, it belongs to the previous tax year
        if (date.getMonthValue() < 4 || (date.getMonthValue() == 4 && date.getDayOfMonth() < 6)) {
            year = year - 1;
        }
        return TaxYear.of(year);
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
