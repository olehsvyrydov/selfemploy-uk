package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.persistence.entity.IncomeEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for Income entities.
 */
@ApplicationScoped
public class IncomeRepository implements PanacheRepositoryBase<IncomeEntity, UUID> {

    /**
     * Saves an income to the database.
     */
    public Income save(Income income) {
        IncomeEntity entity = IncomeEntity.fromDomain(income);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds an income by ID.
     */
    public Optional<Income> findByIdAsDomain(UUID id) {
        return findByIdOptional(id)
            .map(IncomeEntity::toDomain);
    }

    /**
     * Finds all incomes for a business.
     */
    public List<Income> findByBusinessId(UUID businessId) {
        return find("businessId", businessId)
            .stream()
            .map(IncomeEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all incomes within a date range.
     */
    public List<Income> findByDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and date >= ?2 and date <= ?3", businessId, startDate, endDate)
            .stream()
            .map(IncomeEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all incomes by category.
     */
    public List<Income> findByCategory(UUID businessId, IncomeCategory category) {
        return find("businessId = ?1 and category = ?2", businessId, category)
            .stream()
            .map(IncomeEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Calculates total income for a business within a date range.
     */
    public BigDecimal calculateTotalForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        List<Income> incomes = findByDateRange(businessId, startDate, endDate);
        return incomes.stream()
            .map(Income::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total income by category for a business within a date range.
     */
    public BigDecimal calculateTotalByCategoryForDateRange(UUID businessId, IncomeCategory category, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and category = ?2 and date >= ?3 and date <= ?4",
                businessId, category, startDate, endDate)
            .stream()
            .map(IncomeEntity::toDomain)
            .map(Income::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Updates an income.
     */
    public Income update(Income income) {
        IncomeEntity entity = findById(income.id());
        if (entity == null) {
            throw new IllegalArgumentException("Income not found: " + income.id());
        }
        entity.setBusinessId(income.businessId());
        entity.setDate(income.date());
        entity.setAmount(income.amount());
        entity.setDescription(income.description());
        entity.setCategory(income.category());
        entity.setReference(income.reference());
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Deletes an income by ID.
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return deleteById(id);
    }
}
