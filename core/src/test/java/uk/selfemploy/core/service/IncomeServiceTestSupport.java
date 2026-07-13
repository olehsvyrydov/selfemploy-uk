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
 * Test support for {@link IncomeService}, which is an abstract contract with no storage
 * dependency of its own.
 *
 * <p>{@link IncomeStore} mirrors the subset of repository operations the service delegates to,
 * and {@link StoreBackedIncomeService} is a concrete implementation that applies the service's
 * shared validation rules and then delegates to a store. Tests mock the store and drive the
 * service exactly as a production subclass would, keeping validation and delegation coverage in
 * {@code core} without any persistence dependency.</p>
 */
final class IncomeServiceTestSupport {

    private IncomeServiceTestSupport() {
    }

    /** The storage operations {@link StoreBackedIncomeService} delegates to. */
    interface IncomeStore {
        Income save(Income income);

        Optional<Income> findByIdAsDomain(UUID id);

        Income update(Income income);

        boolean deleteByIdAndReturn(UUID id);

        List<Income> findByDateRange(UUID businessId, LocalDate start, LocalDate end);

        List<Income> findByCategory(UUID businessId, IncomeCategory category);

        BigDecimal calculateTotalForDateRange(UUID businessId, LocalDate start, LocalDate end);
    }

    /** Concrete {@link IncomeService} that validates via the shared rules, then delegates to a store. */
    static final class StoreBackedIncomeService extends IncomeService {

        private final IncomeStore store;

        StoreBackedIncomeService(IncomeStore store) {
            super();
            this.store = store;
        }

        @Override
        public Income create(UUID businessId, LocalDate date, BigDecimal amount,
                             String description, IncomeCategory category, String reference,
                             String clientName, IncomeStatus status) {
            validateBusinessId(businessId);
            validateDate(date);
            validateAmount(amount);
            validateDescription(description);
            validateCategory(category);

            Income income = Income.create(businessId, date, amount, description, category, reference,
                    clientName, status);
            return store.save(income);
        }

        @Override
        public Optional<Income> findById(UUID id) {
            if (id == null) {
                throw new ValidationException("id", "Income id cannot be null");
            }
            return store.findByIdAsDomain(id);
        }

        @Override
        public Income update(UUID id, LocalDate date, BigDecimal amount,
                             String description, IncomeCategory category, String reference,
                             String clientName, IncomeStatus status) {
            if (id == null) {
                throw new ValidationException("id", "Income id cannot be null");
            }

            Income existingIncome = store.findByIdAsDomain(id)
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
                    existingIncome.receiptPath(),
                    existingIncome.bankTransactionId(),
                    clientName != null ? clientName : existingIncome.clientName(),
                    status != null ? status : existingIncome.status()
            );

            return store.update(updatedIncome);
        }

        @Override
        public boolean delete(UUID id) {
            if (id == null) {
                throw new ValidationException("id", "Income id cannot be null");
            }

            Optional<Income> existingIncome = store.findByIdAsDomain(id);
            if (existingIncome.isEmpty()) {
                return false;
            }

            return store.deleteByIdAndReturn(id);
        }

        @Override
        public List<Income> findByTaxYear(UUID businessId, TaxYear taxYear) {
            validateBusinessId(businessId);
            if (taxYear == null) {
                throw new ValidationException("taxYear", "Tax year cannot be null");
            }
            return store.findByDateRange(businessId, taxYear.startDate(), taxYear.endDate());
        }

        @Override
        public List<Income> findByCategory(UUID businessId, IncomeCategory category) {
            validateBusinessId(businessId);
            validateCategory(category);
            return store.findByCategory(businessId, category);
        }

        @Override
        public BigDecimal getTotalByTaxYear(UUID businessId, TaxYear taxYear) {
            validateBusinessId(businessId);
            if (taxYear == null) {
                throw new ValidationException("taxYear", "Tax year cannot be null");
            }
            return store.calculateTotalForDateRange(businessId, taxYear.startDate(), taxYear.endDate());
        }

        @Override
        public BigDecimal getTotalByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
            validateBusinessId(businessId);
            if (taxYear == null) {
                throw new ValidationException("taxYear", "Tax year cannot be null");
            }
            if (quarter == null) {
                throw new ValidationException("quarter", "Quarter cannot be null");
            }
            return store.calculateTotalForDateRange(
                    businessId, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear));
        }

        @Override
        public List<Income> findByQuarter(UUID businessId, TaxYear taxYear, Quarter quarter) {
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
    }
}
