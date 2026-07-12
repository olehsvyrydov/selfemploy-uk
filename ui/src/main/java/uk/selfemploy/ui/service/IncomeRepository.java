package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and querying income for a single business.
 *
 * <p>The shipping desktop app implements this with {@link SqliteIncomeRepository} (a JDBC
 * adapter). Instances are scoped to one business, fixed at construction.</p>
 */
public interface IncomeRepository {

    Income save(Income income);

    Optional<Income> findById(UUID id);

    List<Income> findAll();

    List<Income> findByTaxYear(TaxYear taxYear);

    List<Income> findByDateRange(LocalDate startDate, LocalDate endDate);

    List<Income> findByCategory(IncomeCategory category);

    BigDecimal getTotalByTaxYear(TaxYear taxYear);

    BigDecimal getTotalForDateRange(LocalDate startDate, LocalDate endDate);

    boolean delete(UUID id);

    long count();

    UUID getBusinessId();
}
