package uk.selfemploy.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.persistence.entity.ColumnMappingPreferenceEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ColumnMappingPreference entities.
 * Provides data access for saved column mapping preferences.
 *
 * SE-802: Bank Import Column Mapping Wizard
 */
@ApplicationScoped
public class ColumnMappingPreferenceRepository
        implements PanacheRepositoryBase<ColumnMappingPreferenceEntity, UUID> {

    /**
     * Saves a column mapping preference to the database.
     *
     * @param preference the preference to save
     * @return the saved preference
     */
    public ColumnMappingPreferenceEntity save(ColumnMappingPreferenceEntity preference) {
        persist(preference);
        return preference;
    }

    /**
     * Finds a preference by ID.
     *
     * @param id the preference ID
     * @return Optional containing the preference if found
     */
    public Optional<ColumnMappingPreferenceEntity> findByIdOptional(UUID id) {
        return find("id", id).firstResultOptional();
    }

    /**
     * Finds all preferences for a business.
     *
     * @param businessId the business ID
     * @return list of preferences for the business
     */
    public List<ColumnMappingPreferenceEntity> findByBusinessId(UUID businessId) {
        return find("businessId", businessId).list();
    }

    /**
     * Finds all preferences for a business, ordered by last used date descending.
     *
     * @param businessId the business ID
     * @return list of preferences, most recently used first
     */
    public List<ColumnMappingPreferenceEntity> findByBusinessIdOrderByLastUsed(UUID businessId) {
        return find("businessId = ?1 ORDER BY lastUsedAt DESC", businessId).list();
    }

    /**
     * Finds a preference by business ID and bank identifier.
     *
     * @param businessId the business ID
     * @param bankIdentifier the bank identifier
     * @return Optional containing the preference if found
     */
    public Optional<ColumnMappingPreferenceEntity> findByBusinessIdAndBankIdentifier(
            UUID businessId, String bankIdentifier) {
        return find("businessId = ?1 AND bankIdentifier = ?2", businessId, bankIdentifier)
                .firstResultOptional();
    }

    /**
     * Finds preferences matching any of the given bank identifiers for a business.
     * Useful for detecting saved mappings when a CSV is loaded.
     *
     * @param businessId the business ID
     * @param bankIdentifiers list of possible bank identifiers
     * @return list of matching preferences
     */
    public List<ColumnMappingPreferenceEntity> findByBusinessIdAndBankIdentifiers(
            UUID businessId, List<String> bankIdentifiers) {
        return find("businessId = ?1 AND bankIdentifier IN ?2", businessId, bankIdentifiers)
                .list();
    }

    /**
     * Finds the most recently used preference for a business.
     *
     * @param businessId the business ID
     * @return Optional containing the most recent preference if any exist
     */
    public Optional<ColumnMappingPreferenceEntity> findMostRecentByBusinessId(UUID businessId) {
        return find("businessId = ?1 ORDER BY lastUsedAt DESC", businessId)
                .firstResultOptional();
    }

    /**
     * Deletes a preference by ID.
     *
     * @param id the preference ID
     * @return true if deleted, false if not found
     */
    public boolean deleteByIdAndReturn(UUID id) {
        return deleteById(id);
    }

    /**
     * Deletes all preferences for a business.
     *
     * @param businessId the business ID
     * @return number of preferences deleted
     */
    public long deleteByBusinessId(UUID businessId) {
        return delete("businessId", businessId);
    }

    /**
     * Counts preferences for a business.
     *
     * @param businessId the business ID
     * @return number of preferences
     */
    public long countByBusinessId(UUID businessId) {
        return count("businessId", businessId);
    }

    /**
     * Updates the last used timestamp and increments use count.
     *
     * @param id the preference ID
     */
    public void markAsUsed(UUID id) {
        findByIdOptional(id).ifPresent(ColumnMappingPreferenceEntity::markUsed);
    }
}
