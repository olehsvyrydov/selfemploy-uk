package uk.selfemploy.ui.service;

import uk.selfemploy.core.service.TermsAcceptanceService;

import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * SQLite-backed implementation of TermsAcceptanceService for the UI layer.
 *
 * <p>This extends the core TermsAcceptanceService to use the SQLite repository
 * instead of the Panache-based repository that requires Quarkus CDI.</p>
 *
 * <p>SE-508: Terms of Service UI</p>
 */
public class SqliteTermsAcceptanceService extends TermsAcceptanceService {

    private static final Logger LOG = Logger.getLogger(SqliteTermsAcceptanceService.class.getName());

    private final SqliteTermsAcceptanceRepository sqliteRepository;

    public SqliteTermsAcceptanceService() {
        super(null); // Parent won't be used - we override all methods
        this.sqliteRepository = new SqliteTermsAcceptanceRepository();
    }

    /**
     * Constructor for testing with custom repository.
     */
    SqliteTermsAcceptanceService(SqliteTermsAcceptanceRepository repository) {
        super(null);
        this.sqliteRepository = repository;
    }

    @Override
    public boolean saveAcceptance(String tosVersion, Instant acceptedAt, Instant scrollCompletedAt, String applicationVersion) {
        if (tosVersion == null || tosVersion.isBlank()) {
            return false;
        }
        if (acceptedAt == null) {
            return false;
        }
        if (scrollCompletedAt == null) {
            return false;
        }
        if (applicationVersion == null || applicationVersion.isBlank()) {
            return false;
        }

        return sqliteRepository.save(tosVersion, acceptedAt, scrollCompletedAt, applicationVersion);
    }

    @Override
    public Optional<String> getAcceptedVersion() {
        return sqliteRepository.getLatestAcceptedVersion();
    }

    @Override
    public boolean hasAcceptedVersion(String version) {
        return getAcceptedVersion()
                .map(v -> v.equals(version))
                .orElse(false);
    }

    @Override
    public Optional<Instant> getAcceptanceTimestamp() {
        return sqliteRepository.getLatestAcceptanceTimestamp();
    }

    @Override
    public Optional<Instant> getScrollCompletedTimestamp() {
        return sqliteRepository.getLatestScrollCompletedTimestamp();
    }
}
