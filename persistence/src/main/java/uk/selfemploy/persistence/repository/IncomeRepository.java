package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.persistence.entity.IncomeEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for Income entities.
 *
 * <p>All queries filter out soft-deleted records by default (deleted_at IS NULL).
 * Use the *IncludingDeleted variants for admin/audit operations.</p>
 */
@ApplicationScoped
public class IncomeRepository implements PanacheRepositoryBase<IncomeEntity, UUID> {

    // Base filter for active (non-deleted) records
    private static final String ACTIVE_FILTER = "deletedAt IS NULL";

    /**
     * Saves an income to the database.
     */
    public Income save(Income income) {
        IncomeEntity entity = IncomeEntity.fromDomain(income);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds an active income by ID.
     */
    public Optional<Income> findByIdAsDomain(UUID id) {
        return findByIdOptional(id)
            .filter(e -> !e.isDeleted())
            .map(IncomeEntity::toDomain);
    }

    /**
     * Finds an income by ID including soft-deleted records.
     */
    public Optional<Income> findByIdAsDomainIncludingDeleted(UUID id) {
        return findByIdOptional(id)
            .map(IncomeEntity::toDomain);
    }

    /**
     * Finds all active incomes for a business.
     */
    public List<Income> findByBusinessId(UUID businessId) {
        return find("businessId = ?1 and " + ACTIVE_FILTER, businessId)
            .stream()
            .map(IncomeEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all incomes for a business including soft-deleted.
     */
    public List<Income> findByBusinessIdIncludingDeleted(UUID businessId) {
        return find("businessId", businessId)
            .stream()
            .map(IncomeEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all active incomes within a date range.
     */
    public List<Income> findByDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and date >= ?2 and date <= ?3 and " + ACTIVE_FILTER,
                businessId, startDate, endDate)
            .stream()
            .map(IncomeEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all incomes within a date range including soft-deleted.
     */
    public List<Income> findByDateRangeIncludingDeleted(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and date >= ?2 and date <= ?3",
                businessId, startDate, endDate)
            .stream()
            .map(IncomeEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all active incomes by category.
     */
    public List<Income> findByCategory(UUID businessId, IncomeCategory category) {
        return find("businessId = ?1 and category = ?2 and " + ACTIVE_FILTER, businessId, category)
            .stream()
            .map(IncomeEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Calculates total income for a business within a date range (active records only).
     */
    public BigDecimal calculateTotalForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        List<Income> incomes = findByDateRange(businessId, startDate, endDate);
        return incomes.stream()
            .map(Income::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total income by category for a business within a date range (active records only).
     */
    public BigDecimal calculateTotalByCategoryForDateRange(UUID businessId, IncomeCategory category, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and category = ?2 and date >= ?3 and date <= ?4 and " + ACTIVE_FILTER,
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
     * Deletes an income by ID (hard delete).
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return deleteById(id);
    }

    /**
     * Soft deletes income records by their IDs.
     *
     * @param ids the IDs of records to soft delete
     * @param deletedAt the deletion timestamp
     * @param deletedBy who performed the deletion
     * @param reason the reason for deletion
     * @return the number of records soft deleted
     */
    public int softDeleteByIds(List<UUID> ids, Instant deletedAt, String deletedBy, String reason) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (UUID id : ids) {
            IncomeEntity entity = findById(id);
            if (entity != null && !entity.isDeleted()) {
                entity.softDelete(deletedAt, deletedBy, reason);
                persist(entity);
                count++;
            }
        }
        return count;
    }

    /**
     * Restores soft-deleted income records by their IDs.
     *
     * @param ids the IDs of records to restore
     * @return the number of records restored
     */
    public int restoreByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (UUID id : ids) {
            IncomeEntity entity = findById(id);
            if (entity != null && entity.isDeleted()) {
                entity.restore();
                persist(entity);
                count++;
            }
        }
        return count;
    }

    /**
     * Finds active income entities (not domain objects) for duplicate detection.
     * Returns entities to allow access to all fields for comparison.
     */
    public List<IncomeEntity> findEntitiesByDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and date >= ?2 and date <= ?3 and " + ACTIVE_FILTER,
                businessId, startDate, endDate)
            .list();
    }
}
