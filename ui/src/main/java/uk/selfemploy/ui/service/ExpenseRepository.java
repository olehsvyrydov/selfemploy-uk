package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and querying expenses for a single business.
 *
 * <p>The shipping desktop app implements this with {@link SqliteExpenseRepository} (a JDBC
 * adapter). Instances are scoped to one business, fixed at construction.</p>
 */
public interface ExpenseRepository {

    Expense save(Expense expense);

    Optional<Expense> findById(UUID id);

    List<Expense> findAll();

    List<Expense> findByTaxYear(TaxYear taxYear);

    List<Expense> findByDateRange(LocalDate startDate, LocalDate endDate);

    List<Expense> findByCategory(ExpenseCategory category);

    BigDecimal getTotalByTaxYear(TaxYear taxYear);

    BigDecimal getTotalForDateRange(LocalDate startDate, LocalDate endDate);

    BigDecimal getAllowableTotalByTaxYear(TaxYear taxYear);

    BigDecimal getAllowableTotalForDateRange(LocalDate startDate, LocalDate endDate);

    Map<ExpenseCategory, BigDecimal> getTotalsByCategoryForTaxYear(TaxYear taxYear);

    boolean delete(UUID id);

    long count();

    UUID getBusinessId();
}
