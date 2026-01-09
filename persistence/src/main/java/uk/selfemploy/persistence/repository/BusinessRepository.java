package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.persistence.entity.BusinessEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository for Business entities.
 */
@ApplicationScoped
public class BusinessRepository implements PanacheRepositoryBase<BusinessEntity, UUID> {

    /**
     * Saves a business to the database.
     */
    public Business save(Business business) {
        BusinessEntity entity = BusinessEntity.fromDomain(business);
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Finds a business by ID.
     */
    public Optional<Business> findByIdAsDomain(UUID id) {
        return findByIdOptional(id)
            .map(BusinessEntity::toDomain);
    }

    /**
     * Finds all active businesses.
     */
    public List<Business> findAllActive() {
        return find("active", true)
            .stream()
            .map(BusinessEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds all businesses as domain objects.
     */
    public List<Business> findAllAsDomain() {
        return findAll()
            .stream()
            .map(BusinessEntity::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Finds a business by UTR.
     */
    public Optional<Business> findByUtr(String utr) {
        return find("utr", utr)
            .firstResultOptional()
            .map(BusinessEntity::toDomain);
    }

    /**
     * Updates a business.
     */
    public Business update(Business business) {
        BusinessEntity entity = findById(business.id());
        if (entity == null) {
            throw new IllegalArgumentException("Business not found: " + business.id());
        }
        entity.setName(business.name());
        entity.setUtr(business.utr());
        entity.setAccountingPeriodStart(business.accountingPeriodStart());
        entity.setAccountingPeriodEnd(business.accountingPeriodEnd());
        entity.setType(business.type());
        entity.setDescription(business.description());
        entity.setActive(business.active());
        persist(entity);
        return entity.toDomain();
    }

    /**
     * Deletes a business by ID.
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return deleteById(id);
    }
}
