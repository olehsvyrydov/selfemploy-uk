package uk.selfemploy.ui.service;

import uk.selfemploy.core.service.PrivacyAcknowledgmentService;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * SQLite-backed implementation of PrivacyAcknowledgmentService for the UI layer.
 *
 * <p>This extends the core PrivacyAcknowledgmentService to use the SQLite repository
 * instead of the Panache-based repository that requires Quarkus CDI.</p>
 *
 * <p>SE-507: Privacy Notice UI</p>
 */
public class SqlitePrivacyAcknowledgmentService extends PrivacyAcknowledgmentService {

    private static final Logger LOG = Logger.getLogger(SqlitePrivacyAcknowledgmentService.class.getName());

    private final SqlitePrivacyAcknowledgmentRepository sqliteRepository;

    public SqlitePrivacyAcknowledgmentService() {
        super(null); // Parent won't be used - we override all methods
        this.sqliteRepository = new SqlitePrivacyAcknowledgmentRepository();
    }

    /**
     * Constructor for testing with custom repository.
     */
    SqlitePrivacyAcknowledgmentService(SqlitePrivacyAcknowledgmentRepository repository) {
        super(null);
        this.sqliteRepository = repository;
    }

    @Override
    public boolean saveAcknowledgment(String privacyVersion, Instant acknowledgedAt, String applicationVersion) {
        if (privacyVersion == null || privacyVersion.isBlank()) {
            return false;
        }
        if (acknowledgedAt == null) {
            return false;
        }
        if (applicationVersion == null || applicationVersion.isBlank()) {
            return false;
        }

        return sqliteRepository.save(privacyVersion, acknowledgedAt, applicationVersion);
    }

    @Override
    public Optional<String> getAcknowledgedVersion() {
        return sqliteRepository.getLatestAcknowledgedVersion();
    }

    @Override
    public boolean hasAcknowledgedVersion(String version) {
        return getAcknowledgedVersion()
                .map(v -> v.equals(version))
                .orElse(false);
    }

    @Override
    public Optional<Instant> getAcknowledgmentTimestamp() {
        return sqliteRepository.getLatestAcknowledgmentTimestamp();
    }
}
