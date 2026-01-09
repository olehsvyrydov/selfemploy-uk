package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.persistence.entity.ExpenseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for Expense entities.
 */
@ApplicationScoped
public class ExpenseRepository implements PanacheRepositoryBase<ExpenseEntity, UUID> {

    /**
     * Saves an expense to the database.
     */
    public Expense save(Expense expense) {
        ExpenseEntity entity = ExpenseEntity.fromDomain(expense);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds an expense by ID.
     */
    public Optional<Expense> findByIdAsDomain(UUID id) {
        return findByIdOptional(id)
            .map(ExpenseEntity::toDomain);
    }

    /**
     * Finds all expenses for a business.
     */
    public List<Expense> findByBusinessId(UUID businessId) {
        return find("businessId", businessId)
            .stream()
            .map(ExpenseEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all expenses within a date range.
     */
    public List<Expense> findByDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and date >= ?2 and date <= ?3", businessId, startDate, endDate)
            .stream()
            .map(ExpenseEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all expenses by category.
     */
    public List<Expense> findByCategory(UUID businessId, ExpenseCategory category) {
        return find("businessId = ?1 and category = ?2", businessId, category)
            .stream()
            .map(ExpenseEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Calculates total expenses for a business within a date range.
     */
    public BigDecimal calculateTotalForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = findByDateRange(businessId, startDate, endDate);
        return expenses.stream()
            .map(Expense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total allowable expenses for a business within a date range.
     */
    public BigDecimal calculateAllowableTotalForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = findByDateRange(businessId, startDate, endDate);
        return expenses.stream()
            .filter(Expense::isAllowable)
            .map(Expense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total expenses by category for a business within a date range.
     */
    public BigDecimal calculateTotalByCategoryForDateRange(UUID businessId, ExpenseCategory category, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and category = ?2 and date >= ?3 and date <= ?4",
                businessId, category, startDate, endDate)
            .stream()
            .map(ExpenseEntity::toDomain)
            .map(Expense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates totals by category (for SA103 form).
     */
    public Map<ExpenseCategory, BigDecimal> calculateTotalsByCategoryForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = findByDateRange(businessId, startDate, endDate);
        return expenses.stream()
            .collect(Collectors.groupingBy(
                Expense::category,
                Collectors.reducing(BigDecimal.ZERO, Expense::amount, BigDecimal::add)
            ));
    }

    /**
     * Updates an expense.
     */
    public Expense update(Expense expense) {
        ExpenseEntity entity = findById(expense.id());
        if (entity == null) {
            throw new IllegalArgumentException("Expense not found: " + expense.id());
        }
        entity.setBusinessId(expense.businessId());
        entity.setDate(expense.date());
        entity.setAmount(expense.amount());
        entity.setDescription(expense.description());
        entity.setCategory(expense.category());
        entity.setReceiptPath(expense.receiptPath());
        entity.setNotes(expense.notes());
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Deletes an expense by ID.
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return deleteById(id);
    }
}
