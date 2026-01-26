package uk.selfemploy.core.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.persistence.repository.IncomeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for managing Income entities.
 *
 * Provides CRUD operations with validation and business rule enforcement.
 * Income dates are validated to be within a valid UK tax year range.
 */
@ApplicationScoped
public class IncomeService {

    private static final int MAX_DESCRIPTION_LENGTH = 100;

    private final IncomeRepository incomeRepository;

    @Inject
    public IncomeService(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    /**
     * Creates a new income entry.
     *
     * @param businessId   The business ID (required)
     * @param date         The income date (must be within a valid tax year)
     * @param amount       The income amount (must be positive)
     * @param description  The description (required, max 100 chars)
     * @param category     The income category (required)
     * @param reference    Optional reference number
     * @return The created income
     * @throws ValidationException if validation fails
     */
    public Income create(UUID businessId, LocalDate date, BigDecimal amount,
                         String description, IncomeCategory category, String reference) {
        validateBusinessId(businessId);
        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);

        Income income = Income.create(businessId, date, amount, description, category, reference);
        return incomeRepository.save(income);
    }

    /**
     * Finds an income by ID.
     *
     * @param id The income ID
     * @return Optional containing the income if found
     * @throws ValidationException if id is null
     */
    public Optional<Income> findById(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Income id cannot be null");
        }
        return incomeRepository.findByIdAsDomain(id);
    }

    /**
     * Updates an existing income.
     *
     * @param id          The income ID
     * @param date        The updated date
     * @param amount      The updated amount
     * @param description The updated description
     * @param category    The updated category
     * @param reference   The updated reference
     * @return The updated income
     * @throws ValidationException if income not found or validation fails
     */
    public Income update(UUID id, LocalDate date, BigDecimal amount,
                         String description, IncomeCategory category, String reference) {
        if (id == null) {
            throw new ValidationException("id", "Income id cannot be null");
        }

        Income existingIncome = incomeRepository.findByIdAsDomain(id)
                .orElseThrow(() -> new ValidationException("id", "Income not found: " + id));

        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);

        Income updatedIncome = new Income(
                existingIncome.id(),
                existingIncome.businessId(),
                date,
                amount,
                description,
                category,
                reference,
                existingIncome.bankTransactionRef(),
                existingIncome.invoiceNumber(),
                existingIncome.receiptPath()
        );

        return incomeRepository.update(updatedIncome);
    }

    /**
     * Deletes an income by ID.
     *
     * @param id The income ID
     * @return true if deleted, false if not found
     * @throws ValidationException if id is null or income is linked to HMRC submission
     */
    public boolean delete(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Income id cannot be null");
        }

        Optional<Income> existingIncome = incomeRepository.findByIdAsDomain(id);
        if (existingIncome.isEmpty()) {
            return false;
        }

        // TODO: Check if linked to HMRC submission and throw ValidationException
        // if (isLinkedToHmrcSubmission(id)) {
        //     throw new ValidationException("id", "Cannot delete income linked to HMRC submission");
        // }

        return incomeRepository.deleteByIdAndReturn(id);
    }

    /**
     * Finds all incomes for a business within a tax year.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @return List of incomes within the tax year
     * @throws ValidationException if businessId or taxYear is null
     */
    public List<Income> findByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }

        return incomeRepository.findByDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Finds all incomes for a business by category.
     *
     * @param businessId The business ID
     * @param category   The income category
     * @return List of incomes matching the category
     * @throws ValidationException if businessId or category is null
     */
    public List<Income> findByCategory(UUID businessId, IncomeCategory category) {
        validateBusinessId(businessId);
        validateCategory(category);

        return incomeRepository.findByCategory(businessId, category);
    }

    /**
     * Gets the total income for a business within a tax year.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @return Total income amount
     * @throws ValidationException if businessId or taxYear is null
     */
    public BigDecimal getTotalByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }

        return incomeRepository.calculateTotalForDateRange(businessId, taxYear.startDate(), taxYear.endDate());
    }

    /**
     * Gets the total income for a business within a specific quarter.
     * Sprint 10D: SE-10D-003 - Cumulative Totals Display
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @param quarter    The quarter
     * @return Total income amount for the quarter
     * @throws ValidationException if any parameter is null
     */
    public BigDecimal getTotalByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        if (quarter == null) {
            throw new ValidationException("quarter", "Quarter cannot be null");
        }

        return incomeRepository.calculateTotalForDateRange(
                businessId, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
    }

    private void validateBusinessId(UUID businessId) {
        if (businessId == null) {
            throw new ValidationException("businessId", "Business ID cannot be null");
        }
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException("date", "Income date cannot be null");
        }

        // Determine which tax year this date should belong to
        TaxYear taxYear = determineTaxYear(date);
        if (!taxYear.contains(date)) {
            throw new ValidationException("date",
                    String.format("Income date %s is not within a valid tax year", date));
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
            throw new ValidationException("amount", "Income amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount", "Income amount must be positive");
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new ValidationException("description", "Income description cannot be null or empty");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("description",
                    String.format("Income description cannot exceed %d characters", MAX_DESCRIPTION_LENGTH));
        }
    }

    private void validateCategory(IncomeCategory category) {
        if (category == null) {
            throw new ValidationException("category", "Income category cannot be null");
        }
    }
}
