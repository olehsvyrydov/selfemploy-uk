package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of IncomeService for UI standalone mode.
 * Used when the application runs without a full Quarkus CDI container.
 */
public class InMemoryIncomeService extends IncomeService {

    private static final int MAX_DESCRIPTION_LENGTH = 100;
    private final Map<UUID, Income> storage = new ConcurrentHashMap<>();

    public InMemoryIncomeService() {
        super(null); // No Panache repository in standalone mode
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
        storage.put(income.id(), income);
        return income;
    }

    @Override
    public Optional<Income> findById(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Income id cannot be null");
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Income update(UUID id, LocalDate date, BigDecimal amount,
                         String description, IncomeCategory category, String reference) {
        if (id == null) {
            throw new ValidationException("id", "Income id cannot be null");
        }

        Income existingIncome = findById(id)
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

        storage.put(id, updatedIncome);
        return updatedIncome;
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null) {
            throw new ValidationException("id", "Income id cannot be null");
        }
        return storage.remove(id) != null;
    }

    @Override
    public List<Income> findByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }

        return storage.values().stream()
            .filter(i -> i.businessId().equals(businessId))
            .filter(i -> !i.date().isBefore(taxYear.startDate()) && !i.date().isAfter(taxYear.endDate()))
            .sorted(Comparator.comparing(Income::date).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public List<Income> findByCategory(UUID businessId, IncomeCategory category) {
        validateBusinessId(businessId);
        validateCategory(category);

        return storage.values().stream()
            .filter(i -> i.businessId().equals(businessId))
            .filter(i -> i.category() == category)
            .sorted(Comparator.comparing(Income::date).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getTotalByTaxYear(UUID businessId, TaxYear taxYear) {
        validateBusinessId(businessId);
        if (taxYear == null) {
            throw new ValidationException("taxYear", "Tax year cannot be null");
        }

        return findByTaxYear(businessId, taxYear).stream()
            .map(Income::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Clears all data (useful for testing).
     */
    public void clearAll() {
        storage.clear();
    }

    /**
     * Returns the count of all income entries.
     */
    public long count() {
        return storage.size();
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
