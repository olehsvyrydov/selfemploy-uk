package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.enums.BusinessType;
import uk.selfemploy.ui.viewmodel.OnboardingViewModel.OnboardingCompletionSummary;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OnboardingSetupService")
class OnboardingSetupServiceTest {

    private SqliteDataStore dataStore;
    private OnboardingSetupService service;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetInstance();
        dataStore = SqliteDataStore.getInstance();
        service = new OnboardingSetupService(dataStore);
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.resetTestData();
    }

    @Test
    @DisplayName("onboarding is required on a fresh database")
    void requiredOnFreshDatabase() {
        assertThat(service.isRequired()).isTrue();
    }

    @Test
    @DisplayName("completing persists the identity to the shared settings and marks onboarding done")
    void completePersistsIdentityAndFlag() {
        OnboardingCompletionSummary summary = new OnboardingCompletionSummary(
            "Sarah Smith", "1234567890", "QQ123456C", "2025/26", BusinessType.SOLE_TRADER);

        service.complete(summary);

        assertThat(service.isRequired()).isFalse();
        assertThat(dataStore.loadDisplayName()).isEqualTo("Sarah Smith");
        assertThat(dataStore.loadUtr()).isEqualTo("1234567890");
        assertThat(dataStore.loadNino()).isEqualTo("QQ123456C");
        assertThat(dataStore.loadOnboardingTaxYear()).isEqualTo("2025/26");
        assertThat(dataStore.loadBusinessType()).isEqualTo("SOLE_TRADER");
    }

    @Test
    @DisplayName("a skipped wizard (blank fields) records completion without overwriting identity")
    void completeWithBlankFieldsOnlyMarksDone() {
        OnboardingCompletionSummary blank = new OnboardingCompletionSummary("", "", "", "", null);

        service.complete(blank);

        assertThat(service.isRequired()).isFalse();
        assertThat(dataStore.loadDisplayName()).isNull();
        assertThat(dataStore.loadBusinessType()).isNull();
    }

    @Test
    @DisplayName("a null summary still records completion so the app does not loop")
    void completeWithNullMarksDone() {
        service.complete(null);
        assertThat(service.isRequired()).isFalse();
    }

    @Test
    @DisplayName("reset makes onboarding required again")
    void resetMakesRequiredAgain() {
        service.complete(null);
        assertThat(service.isRequired()).isFalse();

        service.reset();

        assertThat(service.isRequired()).isTrue();
    }
}
