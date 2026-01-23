package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed implementation of IncomeService.
 * All operations go directly to the SQLite database - no in-memory caching.
 * This ensures data is never lost.
 */
public class SqliteIncomeService extends IncomeService {

    private static final int MAX_DESCRIPTION_LENGTH = 100;
    private final SqliteIncomeRepository repository;
    private final UUID businessId;

    public SqliteIncomeService(UUID businessId) {
        super(null); // No Panache repository in standalone mode
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        this.businessId = businessId;
        this.repository = new SqliteIncomeRepository(businessId);
    }

    @Override
    public Income create(UUID businessId, LocalDate date, BigDecimal amount,
                         String description, IncomeCategory category, String reference) {
        validateBusinessId(businessId);
        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);

        Income income = Income.create(businessId, date, amount, description, category, reference);
        return repository.save(income);
    }

    @Override
    public Optional<Income> findById(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Income id cannot be null");
        }
        return repository.findById(id);
    }

    @Override
    public Income update(UUID id, LocalDate date, BigDecimal amount,
                         String description, IncomeCategory category, String reference) {
        if (id == null) {
            throw new ValidationException("id", "Income id cannot be null");
        }

        Income existingIncome = repository.findById(id)
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
                reference
        );

        return repository.save(updatedIncome);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Income id cannot be null");
        }
        return repository.delete(id);
    }

    @Override
    public List<Income> findByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }
        return repository.findByTaxYear(taxYear);
    }

    @Override
    public List<Income> findByCategory(UUID businessId, IncomeCategory category) {
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

    /**
     * Returns the count of all income entries.
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
            throw new ValidationException("date", "Income date cannot be null");
        }
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
