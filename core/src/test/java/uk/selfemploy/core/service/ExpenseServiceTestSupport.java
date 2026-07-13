package uk.selfemploy.core.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Test support for {@link ExpenseService}, which is an abstract contract with no storage
 * dependency of its own.
 *
 * <p>{@link ExpenseStore} mirrors the subset of repository operations the service delegates to,
 * and {@link StoreBackedExpenseService} is a concrete implementation that applies the service's
 * shared validation rules and then delegates to a store. Tests mock the store and drive the
 * service exactly as a production subclass would, keeping validation and delegation coverage in
 * {@code core} without any persistence dependency.</p>
 */
final class ExpenseServiceTestSupport {

    private ExpenseServiceTestSupport() {
    }

    /** The storage operations {@link StoreBackedExpenseService} delegates to. */
    interface ExpenseStore {
        Expense save(Expense expense);

        Optional<Expense> findByIdAsDomain(UUID id);

        Expense update(Expense expense);

        boolean deleteByIdAndReturn(UUID id);

        List<Expense> findByDateRange(UUID businessId, LocalDate start, LocalDate end);

        List<Expense> findByCategory(UUID businessId, ExpenseCategory category);

        BigDecimal calculateTotalForDateRange(UUID businessId, LocalDate start, LocalDate end);

        BigDecimal calculateAllowableTotalForDateRange(UUID businessId, LocalDate start, LocalDate end);

        Map<ExpenseCategory, BigDecimal> calculateTotalsByCategoryForDateRange(UUID businessId, LocalDate start, LocalDate end);
    }

    /** Concrete {@link ExpenseService} that validates via the shared rules, then delegates to a store. */
    static final class StoreBackedExpenseService extends ExpenseService {

        private final ExpenseStore store;

        StoreBackedExpenseService(ExpenseStore store) {
            super();
            this.store = store;
        }

        @Override
        public Expense create(UUID businessId, LocalDate date, BigDecimal amount,
                              String description, ExpenseCategory category,
                              String receiptPath, String notes) {
            validateBusinessId(businessId);
            validateDate(date);
            validateAmount(amount);
            validateDescription(description);
            validateCategory(category);

            Expense expense = Expense.create(businessId, date, amount, description, category, receiptPath, notes);
            return store.save(expense);
        }

        @Override
        public Optional<Expense> findById(UUID id) {
            if (id == null) {
                throw new ValidationException("id", "Expense id cannot be null");
            }
            return store.findByIdAsDomain(id);
        }

        @Override
        public Expense update(UUID id, LocalDate date, BigDecimal amount,
                              String description, ExpenseCategory category,
                              String receiptPath, String notes) {
            if (id == null) {
                throw new ValidationException("id", "Expense id cannot be null");
            }

            Expense existingExpense = store.findByIdAsDomain(id)
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
                    existingExpense.invoiceNumber(),
                    existingExpense.bankTransactionId()
            );

            return store.update(updatedExpense);
        }

        @Override
        public boolean delete(UUID id) {
            if (id == null) {
                throw new ValidationException("id", "Expense id cannot be null");
            }

            Optional<Expense> existingExpense = store.findByIdAsDomain(id);
            if (existingExpense.isEmpty()) {
                return false;
            }

            return store.deleteByIdAndReturn(id);
        }

        @Override
        public List<Expense> findByTaxYear(UUID businessId, uk.selfemploy.common.domain.TaxYear taxYear) {
            validateBusinessId(businessId);
            if (taxYear == null) {
                throw new ValidationException("taxYear", "Tax year cannot be null");
            }
            return store.findByDateRange(businessId, taxYear.startDate(), taxYear.endDate());
        }

        @Override
        public List<Expense> findByCategory(UUID businessId, ExpenseCategory category) {
            validateBusinessId(businessId);
            validateCategory(category);
            return store.findByCategory(businessId, category);
        }

        @Override
        public BigDecimal getTotalByTaxYear(UUID businessId, uk.selfemploy.common.domain.TaxYear taxYear) {
            validateBusinessId(businessId);
            if (taxYear == null) {
                throw new ValidationException("taxYear", "Tax year cannot be null");
            }
            return store.calculateTotalForDateRange(businessId, taxYear.startDate(), taxYear.endDate());
        }

        @Override
        public BigDecimal getDeductibleTotal(UUID businessId, uk.selfemploy.common.domain.TaxYear taxYear) {
            validateBusinessId(businessId);
            if (taxYear == null) {
                throw new ValidationException("taxYear", "Tax year cannot be null");
            }
            return store.calculateAllowableTotalForDateRange(businessId, taxYear.startDate(), taxYear.endDate());
        }

        @Override
        public BigDecimal getDeductibleTotalByQuarter(UUID businessId, uk.selfemploy.common.domain.TaxYear taxYear,
                                                      uk.selfemploy.common.domain.Quarter quarter) {
            validateBusinessId(businessId);
            if (taxYear == null) {
                throw new ValidationException("taxYear", "Tax year cannot be null");
            }
            if (quarter == null) {
                throw new ValidationException("quarter", "Quarter cannot be null");
            }
            return store.calculateAllowableTotalForDateRange(
                    businessId, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
        }

        @Override
        public List<Expense> findByQuarter(UUID businessId, uk.selfemploy.common.domain.TaxYear taxYear,
                                           uk.selfemploy.common.domain.Quarter quarter) {
            validateBusinessId(businessId);
            if (taxYear == null) {
                throw new ValidationException("taxYear", "Tax year cannot be null");
            }
            if (quarter == null) {
                throw new ValidationException("quarter", "Quarter cannot be null");
            }
            return store.findByDateRange(
                    businessId, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
        }

        @Override
        public Map<ExpenseCategory, BigDecimal> getTotalsByCategoryByQuarter(UUID businessId,
                                                      uk.selfemploy.common.domain.TaxYear taxYear,
                                                      uk.selfemploy.common.domain.Quarter quarter) {
            validateBusinessId(businessId);
            if (taxYear == null) {
                throw new ValidationException("taxYear", "Tax year cannot be null");
            }
            if (quarter == null) {
                throw new ValidationException("quarter", "Quarter cannot be null");
            }
            return store.calculateTotalsByCategoryForDateRange(
                    businessId, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
        }
    }
}
