package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.persistence.entity.TaxYearEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for TaxYear entities.
 */
@ApplicationScoped
public class TaxYearRepository implements PanacheRepositoryBase<TaxYearEntity, UUID> {

    /**
     * Saves a tax year to the database.
     */
    public TaxYear save(TaxYear taxYear) {
        TaxYearEntity entity = TaxYearEntity.fromDomain(taxYear);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds or creates a tax year for the given start year.
     */
    public TaxYear findOrCreate(int startYear) {
        return findByStartYear(startYear)
            .orElseGet(() -> save(TaxYear.of(startYear)));
    }

    /**
     * Finds a tax year by ID.
     */
    public Optional<TaxYear> findByIdAsDomain(UUID id) {
        return findByIdOptional(id)
            .map(TaxYearEntity::toDomain);
    }

    /**
     * Finds a tax year by start year.
     */
    public Optional<TaxYear> findByStartYear(int startYear) {
        return find("startYear", startYear)
            .firstResultOptional()
            .map(TaxYearEntity::toDomain);
    }

    /**
     * Finds the tax year containing a given date.
     */
    public Optional<TaxYear> findByDate(LocalDate date) {
        return find("startDate <= ?1 and endDate >= ?1", date)
            .firstResultOptional()
            .map(TaxYearEntity::toDomain);
    }

    /**
     * Finds all tax years as domain objects.
     */
    public List<TaxYear> findAllAsDomain() {
        return findAll()
            .stream()
            .map(TaxYearEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all tax years ordered by start year descending (most recent first).
     */
    public List<TaxYear> findAllOrderedByStartYearDesc() {
        return find("order by startYear desc")
            .stream()
            .map(TaxYearEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Deletes a tax year by ID.
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return deleteById(id);
    }
}
