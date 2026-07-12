package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory expense store used as a test double for {@link InMemoryExpenseService} in the UI's
 * standalone mode. Holds no external state: data lives only in {@link #storage} for the lifetime
 * of the instance. The shipping app persists expenses through {@link SqliteExpenseRepository}.
 */
public class InMemoryExpenseRepository {

    private final Map<UUID, Expense> storage = new ConcurrentHashMap<>();

    public InMemoryExpenseRepository() {
    }

    /**
     * Saves an expense to the in-memory store.
     */
    public Expense save(Expense expense) {
        storage.put(expense.id(), expense);
        return expense;
    }

    /**
     * Finds an expense by ID.
     */
    public Optional<Expense> findByIdAsDomain(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    /**
     * Finds all expenses for a business.
     */
    public List<Expense> findByBusinessId(UUID businessId) {
        return storage.values().stream()
            .filter(e -> e.businessId().equals(businessId))
            .sorted(Comparator.comparing(Expense::date).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Finds all expenses within a date range.
     */
    public List<Expense> findByDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return storage.values().stream()
            .filter(e -> e.businessId().equals(businessId))
            .filter(e -> !e.date().isBefore(startDate) && !e.date().isAfter(endDate))
            .sorted(Comparator.comparing(Expense::date).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Finds all expenses by category.
     */
    public List<Expense> findByCategory(UUID businessId, ExpenseCategory category) {
        return storage.values().stream()
            .filter(e -> e.businessId().equals(businessId))
            .filter(e -> e.category() == category)
            .sorted(Comparator.comparing(Expense::date).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Calculates total expenses for a business within a date range.
     */
    public BigDecimal calculateTotalForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return findByDateRange(businessId, startDate, endDate).stream()
            .map(Expense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total allowable expenses for a business within a date range.
     */
    public BigDecimal calculateAllowableTotalForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return findByDateRange(businessId, startDate, endDate).stream()
            .filter(Expense::isAllowable)
            .map(Expense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates totals by category (for SA103 form).
     */
    public Map<ExpenseCategory, BigDecimal> calculateTotalsByCategoryForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return findByDateRange(businessId, startDate, endDate).stream()
            .collect(Collectors.groupingBy(
                Expense::category,
                Collectors.reducing(BigDecimal.ZERO, Expense::amount, BigDecimal::add)
            ));
    }

    /**
     * Updates an expense.
     */
    public Expense update(Expense expense) {
        if (!storage.containsKey(expense.id())) {
            throw new IllegalArgumentException("Expense not found: " + expense.id());
        }
        storage.put(expense.id(), expense);
        return expense;
    }

    /**
     * Deletes an expense by ID.
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return storage.remove(id) != null;
    }

    /**
     * Returns the count of all expenses.
     */
    public long count() {
        return storage.size();
    }

    /**
     * Clears all data (useful for testing).
     */
    public void clear() {
        storage.clear();
    }
}
