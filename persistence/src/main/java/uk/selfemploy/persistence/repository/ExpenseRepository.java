package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.persistence.entity.ExpenseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for Expense entities.
 *
 * <p>All queries filter out soft-deleted records by default (deleted_at IS NULL).
 * Use the *IncludingDeleted variants for admin/audit operations.</p>
 */
@ApplicationScoped
public class ExpenseRepository implements PanacheRepositoryBase<ExpenseEntity, UUID> {

    // Base filter for active (non-deleted) records
    private static final String ACTIVE_FILTER = "deletedAt IS NULL";

    /**
     * Saves an expense to the database.
     */
    public Expense save(Expense expense) {
        ExpenseEntity entity = ExpenseEntity.fromDomain(expense);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds an active expense by ID.
     */
    public Optional<Expense> findByIdAsDomain(UUID id) {
        return findByIdOptional(id)
            .filter(e -> !e.isDeleted())
            .map(ExpenseEntity::toDomain);
    }

    /**
     * Finds an expense by ID including soft-deleted records.
     */
    public Optional<Expense> findByIdAsDomainIncludingDeleted(UUID id) {
        return findByIdOptional(id)
            .map(ExpenseEntity::toDomain);
    }

    /**
     * Finds all active expenses for a business.
     */
    public List<Expense> findByBusinessId(UUID businessId) {
        return find("businessId = ?1 and " + ACTIVE_FILTER, businessId)
            .stream()
            .map(ExpenseEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all expenses for a business including soft-deleted.
     */
    public List<Expense> findByBusinessIdIncludingDeleted(UUID businessId) {
        return find("businessId", businessId)
            .stream()
            .map(ExpenseEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all active expenses within a date range.
     */
    public List<Expense> findByDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and date >= ?2 and date <= ?3 and " + ACTIVE_FILTER,
                businessId, startDate, endDate)
            .stream()
            .map(ExpenseEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all expenses within a date range including soft-deleted.
     */
    public List<Expense> findByDateRangeIncludingDeleted(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and date >= ?2 and date <= ?3",
                businessId, startDate, endDate)
            .stream()
            .map(ExpenseEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all active expenses by category.
     */
    public List<Expense> findByCategory(UUID businessId, ExpenseCategory category) {
        return find("businessId = ?1 and category = ?2 and " + ACTIVE_FILTER, businessId, category)
            .stream()
            .map(ExpenseEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Calculates total expenses for a business within a date range (active records only).
     */
    public BigDecimal calculateTotalForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = findByDateRange(businessId, startDate, endDate);
        return expenses.stream()
            .map(Expense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total allowable expenses for a business within a date range (active records only).
     */
    public BigDecimal calculateAllowableTotalForDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = findByDateRange(businessId, startDate, endDate);
        return expenses.stream()
            .filter(Expense::isAllowable)
            .map(Expense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates total expenses by category for a business within a date range (active records only).
     */
    public BigDecimal calculateTotalByCategoryForDateRange(UUID businessId, ExpenseCategory category, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and category = ?2 and date >= ?3 and date <= ?4 and " + ACTIVE_FILTER,
                businessId, category, startDate, endDate)
            .stream()
            .map(ExpenseEntity::toDomain)
            .map(Expense::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates totals by category (for SA103 form) - active records only.
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
        entity.setBankTransactionRef(expense.bankTransactionRef());
        entity.setSupplierRef(expense.supplierRef());
        entity.setInvoiceNumber(expense.invoiceNumber());
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Deletes an expense by ID (hard delete).
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return deleteById(id);
    }

    /**
     * Soft deletes expense records by their IDs.
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
            ExpenseEntity entity = findById(id);
            if (entity != null && !entity.isDeleted()) {
                entity.softDelete(deletedAt, deletedBy, reason);
                persist(entity);
                count++;
            }
        }
        return count;
    }

    /**
     * Restores soft-deleted expense records by their IDs.
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
            ExpenseEntity entity = findById(id);
            if (entity != null && entity.isDeleted()) {
                entity.restore();
                persist(entity);
                count++;
            }
        }
        return count;
    }

    /**
     * Finds active expense entities (not domain objects) for duplicate detection.
     * Returns entities to allow access to all fields for comparison.
     */
    public List<ExpenseEntity> findEntitiesByDateRange(UUID businessId, LocalDate startDate, LocalDate endDate) {
        return find("businessId = ?1 and date >= ?2 and date <= ?3 and " + ACTIVE_FILTER,
                businessId, startDate, endDate)
            .list();
    }

    // ===== Duplicate Detection Methods (Sprint 10C - SE-10C-002) =====

    /**
     * Checks if an active expense with the given bank transaction reference exists for the business.
     *
     * <p>This method is used for application-level duplicate detection since H2 does not
     * support partial unique indexes with WHERE clauses.</p>
     *
     * @param businessId the business ID
     * @param bankTransactionRef the bank transaction reference to check
     * @return true if an active expense with this reference exists, false otherwise
     */
    public boolean existsByBusinessIdAndBankTransactionRef(UUID businessId, String bankTransactionRef) {
        if (bankTransactionRef == null || bankTransactionRef.isBlank()) {
            return false;
        }
        return count("businessId = ?1 and bankTransactionRef = ?2 and " + ACTIVE_FILTER,
                businessId, bankTransactionRef) > 0;
    }

    /**
     * Checks if an active expense with the given supplier reference exists for the business.
     *
     * @param businessId the business ID
     * @param supplierRef the supplier reference to check
     * @return true if an active expense with this reference exists, false otherwise
     */
    public boolean existsByBusinessIdAndSupplierRef(UUID businessId, String supplierRef) {
        if (supplierRef == null || supplierRef.isBlank()) {
            return false;
        }
        return count("businessId = ?1 and supplierRef = ?2 and " + ACTIVE_FILTER,
                businessId, supplierRef) > 0;
    }

    /**
     * Checks if an active expense with the given invoice number exists for the business.
     *
     * @param businessId the business ID
     * @param invoiceNumber the invoice number to check
     * @return true if an active expense with this invoice number exists, false otherwise
     */
    public boolean existsByBusinessIdAndInvoiceNumber(UUID businessId, String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            return false;
        }
        return count("businessId = ?1 and invoiceNumber = ?2 and " + ACTIVE_FILTER,
                businessId, invoiceNumber) > 0;
    }
}
