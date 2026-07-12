package uk.selfemploy.ui.service;

import uk.selfemploy.ui.viewmodel.OnboardingViewModel.OnboardingCompletionSummary;

import java.util.function.Consumer;

/**
 * Decides whether first-run onboarding should be shown and persists its result.
 *
 * <p>Completion (whether the wizard is finished or skipped) writes the collected identity to the
 * same settings the Settings screen uses — no parallel state — and records the
 * {@code onboarding_completed} flag so onboarding is not shown again.</p>
 */
public class OnboardingSetupService {

    private final SqliteDataStore dataStore;

    public OnboardingSetupService() {
        this(SqliteDataStore.getInstance());
    }

    OnboardingSetupService(SqliteDataStore dataStore) {
        this.dataStore = dataStore;
    }

    /** True if first-run onboarding has not yet been completed or skipped. */
    public boolean isRequired() {
        return !dataStore.isOnboardingCompleted();
    }

    /**
     * Persists the onboarding result and marks onboarding complete. Blank fields are left unset, so a
     * skipped wizard simply records completion without overwriting anything. A null summary (e.g. an
     * onboarding that failed to display) still records completion so the app does not loop on it.
     */
    public void complete(OnboardingCompletionSummary summary) {
        if (summary != null) {
            saveIfPresent(summary.userName(), dataStore::saveDisplayName);
            saveIfPresent(summary.utr(), dataStore::saveUtr);
            saveIfPresent(summary.niNumber(), dataStore::saveNino);
            saveIfPresent(summary.taxYear(), dataStore::saveOnboardingTaxYear);
            if (summary.businessType() != null) {
                dataStore.saveBusinessType(summary.businessType().name());
            }
        }
        dataStore.saveOnboardingCompleted(true);
    }

    /** Clears the completion flag so onboarding runs again (Settings "Run setup again"). */
    public void reset() {
        dataStore.saveOnboardingCompleted(false);
    }

    private void saveIfPresent(String value, Consumer<String> saver) {
        if (value != null && !value.isBlank()) {
            saver.accept(value.trim());
        }
    }
}
