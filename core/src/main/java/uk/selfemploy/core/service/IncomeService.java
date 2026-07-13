package uk.selfemploy.core.service;

import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.core.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for managing Income entities.
 *
 * <p>Defines the CRUD and reporting operations and owns the reusable validation rules
 * (dates within a valid UK tax year, positive amounts). Persistence is supplied by concrete
 * subclasses (the desktop app's SQLite-backed implementation), so this type carries no
 * storage dependency.</p>
 */
public abstract class IncomeService {

    protected static final int MAX_DESCRIPTION_LENGTH = 100;

    protected IncomeService() {
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
        return create(businessId, date, amount, description, category, reference, null, null);
    }

    /**
     * Creates a new income entry, including client name and payment status.
     *
     * @param businessId   The business ID (required)
     * @param date         The income date (must be within a valid tax year)
     * @param amount       The income amount (must be positive)
     * @param description  The description (required, max 100 chars)
     * @param category     The income category (required)
     * @param reference    Optional reference number
     * @param clientName   Optional client the income was received from
     * @param status       Payment status; defaults to PAID when null
     * @return The created income
     * @throws ValidationException if validation fails
     */
    public abstract Income create(UUID businessId, LocalDate date, BigDecimal amount,
                         String description, IncomeCategory category, String reference,
                         String clientName, IncomeStatus status);

    /**
     * Finds an income by ID.
     *
     * @param id The income ID
     * @return Optional containing the income if found
     * @throws ValidationException if id is null
     */
    public abstract Optional<Income> findById(UUID id);

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
        return update(id, date, amount, description, category, reference, null, null);
    }

    /**
     * Updates an existing income, including client name and payment status.
     *
     * <p>A null {@code clientName} or {@code status} preserves the existing stored value,
     * so callers that do not manage those fields (e.g. bank-import reconciliation) leave
     * them untouched.
     *
     * @param id          The income ID
     * @param date        The updated date
     * @param amount      The updated amount
     * @param description The updated description
     * @param category    The updated category
     * @param reference   The updated reference
     * @param clientName  The updated client name, or null to keep the existing value
     * @param status      The updated payment status, or null to keep the existing value
     * @return The updated income
     * @throws ValidationException if income not found or validation fails
     */
    public abstract Income update(UUID id, LocalDate date, BigDecimal amount,
                         String description, IncomeCategory category, String reference,
                         String clientName, IncomeStatus status);

    /**
     * Deletes an income by ID.
     *
     * @param id The income ID
     * @return true if deleted, false if not found
     * @throws ValidationException if id is null or income is linked to HMRC submission
     */
    public abstract boolean delete(UUID id);

    /**
     * Finds all incomes for a business within a tax year.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @return List of incomes within the tax year
     * @throws ValidationException if businessId or taxYear is null
     */
    public abstract List<Income> findByTaxYear(UUID businessId, TaxYear taxYear);

    /**
     * Finds all incomes for a business by category.
     *
     * @param businessId The business ID
     * @param category   The income category
     * @return List of incomes matching the category
     * @throws ValidationException if businessId or category is null
     */
    public abstract List<Income> findByCategory(UUID businessId, IncomeCategory category);

    /**
     * Gets the total income for a business within a tax year.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @return Total income amount
     * @throws ValidationException if businessId or taxYear is null
     */
    public abstract BigDecimal getTotalByTaxYear(UUID businessId, TaxYear taxYear);

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
    public abstract BigDecimal getTotalByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter);

    /**
     * Finds all incomes for a business within a specific quarter.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @param quarter    The quarter
     * @return List of incomes within the quarter
     * @throws ValidationException if any parameter is null
     */
    public abstract List<Income> findByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter);

    /**
     * Counts all incomes for a business within a specific quarter.
     *
     * @param businessId The business ID
     * @param taxYear    The tax year
     * @param quarter    The quarter
     * @return Count of income transactions within the quarter
     * @throws ValidationException if any parameter is null
     */
    public int countByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
        return findByQuarter(businessId, taxYear, quarter).size();
    }

    protected void validateBusinessId(UUID businessId) {
        if (businessId == null) {
            throw new ValidationException("businessId", "Business ID cannot be null");
        }
    }

    protected void validateDate(LocalDate date) {
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
    protected TaxYear determineTaxYear(LocalDate date) {
        int year = date.getYear();
        // If date is before April 6th, it belongs to the previous tax year
        if (date.getMonthValue() < 4 || (date.getMonthValue() == 4 && date.getDayOfMonth() < 6)) {
            year = year - 1;
        }
        return TaxYear.of(year);
    }

    protected void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("amount", "Income amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount", "Income amount must be positive");
        }
    }

    protected void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new ValidationException("description", "Income description cannot be null or empty");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("description",
                    String.format("Income description cannot exceed %d characters", MAX_DESCRIPTION_LENGTH));
        }
    }

    protected void validateCategory(IncomeCategory category) {
        if (category == null) {
            throw new ValidationException("category", "Income category cannot be null");
        }
    }
}
