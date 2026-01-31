package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteTestSupport;
import uk.selfemploy.ui.viewmodel.HmrcConnectionWizardViewModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for NINO synchronization between Settings and HMRC Connection Wizard.
 *
 * <p>Tests bidirectional sync:</p>
 * <ul>
 *   <li>Settings → Wizard: Pre-populate NINO in Step 2 if exists in Settings</li>
 *   <li>Wizard → Settings: Auto-save NINO to Settings after OAuth success (Step 5)</li>
 * </ul>
 *
 * <p>Per /inga and /anna recommendations:</p>
 * <ul>
 *   <li>Settings is the single source of truth for profile data</li>
 *   <li>No prompts needed - auto-sync in both directions</li>
 *   <li>Trust the user's input</li>
 * </ul>
 */
@DisplayName("NINO Sync Integration Tests")
class NinoSyncIntegrationTest {

    private static final String TEST_NINO = "AB123456C";
    private static final String DIFFERENT_NINO = "CD987654E";

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
        SqliteTestSupport.resetTestData();
    }

    @Nested
    @DisplayName("Settings → Wizard Pre-population")
    class SettingsToWizardTests {

        @Test
        @DisplayName("should pre-populate NINO from Settings when exists")
        void shouldPrePopulateNinoFromSettings() {
            // Given: NINO exists in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // When: Loading NINO from Settings
            String savedNino = SqliteDataStore.getInstance().loadNino();

            // Then: NINO should be available for pre-population
            assertThat(savedNino).isEqualTo(TEST_NINO);
        }

        @Test
        @DisplayName("should return null when no NINO in Settings")
        void shouldReturnNullWhenNoNinoInSettings() {
            // Given: No NINO in Settings (cleared by setUp)

            // When: Loading NINO from Settings
            String savedNino = SqliteDataStore.getInstance().loadNino();

            // Then: Should be null
            assertThat(savedNino).isNull();
        }

        @Test
        @DisplayName("should handle empty string NINO gracefully")
        void shouldHandleEmptyStringNinoGracefully() {
            // Given: Empty NINO saved (edge case)
            SqliteDataStore.getInstance().saveNino("");

            // When: Loading NINO from Settings
            String savedNino = SqliteDataStore.getInstance().loadNino();

            // Then: Should be empty string
            assertThat(savedNino).isEmpty();
        }

        @Test
        @DisplayName("ViewModel should use NINO from Settings for pre-population")
        void viewModelShouldUseNinoFromSettingsForPrePopulation() {
            // Given: NINO exists in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // When: Creating ViewModel and simulating Step 2 load
            HmrcConnectionWizardViewModel viewModel = new HmrcConnectionWizardViewModel();

            // Simulate what loadStep2Content does:
            String existingNino = viewModel.getNino();
            if (existingNino == null || existingNino.isEmpty()) {
                String savedNino = SqliteDataStore.getInstance().loadNino();
                if (savedNino != null && !savedNino.isEmpty()) {
                    viewModel.setNino(savedNino);
                }
            }

            // Then: ViewModel should have the NINO from Settings
            assertThat(viewModel.getNino()).isEqualTo(TEST_NINO);
        }

        @Test
        @DisplayName("ViewModel NINO should take precedence over Settings when already set")
        void viewModelNinoShouldTakePrecedenceOverSettings() {
            // Given: NINO exists in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // And: ViewModel already has a different NINO (user returning to Step 2)
            HmrcConnectionWizardViewModel viewModel = new HmrcConnectionWizardViewModel();
            viewModel.setNino(DIFFERENT_NINO);

            // When: Simulating Step 2 load logic
            String existingNino = viewModel.getNino();
            if (existingNino == null || existingNino.isEmpty()) {
                String savedNino = SqliteDataStore.getInstance().loadNino();
                if (savedNino != null && !savedNino.isEmpty()) {
                    viewModel.setNino(savedNino);
                }
            }

            // Then: ViewModel should keep its existing NINO
            assertThat(viewModel.getNino()).isEqualTo(DIFFERENT_NINO);
        }
    }

    @Nested
    @DisplayName("Wizard → Settings Auto-save")
    class WizardToSettingsTests {

        @Test
        @DisplayName("should save NINO to Settings on Step 5")
        void shouldSaveNinoToSettingsOnStep5() {
            // Given: No NINO in Settings initially
            assertThat(SqliteDataStore.getInstance().loadNino()).isNull();

            // And: ViewModel has NINO from wizard Step 2
            HmrcConnectionWizardViewModel viewModel = new HmrcConnectionWizardViewModel();
            viewModel.setNino(TEST_NINO);

            // When: Simulating Step 5 load (auto-save logic)
            String wizardNino = viewModel.getNino();
            if (wizardNino != null && !wizardNino.isEmpty()) {
                SqliteDataStore.getInstance().saveNino(wizardNino);
            }

            // Then: NINO should be saved to Settings
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(TEST_NINO);
        }

        @Test
        @DisplayName("should update Settings NINO if different")
        void shouldUpdateSettingsNinoIfDifferent() {
            // Given: Old NINO in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // And: User entered different NINO in wizard (fixing a typo)
            HmrcConnectionWizardViewModel viewModel = new HmrcConnectionWizardViewModel();
            viewModel.setNino(DIFFERENT_NINO);

            // When: Simulating Step 5 load (auto-save logic)
            String wizardNino = viewModel.getNino();
            if (wizardNino != null && !wizardNino.isEmpty()) {
                SqliteDataStore.getInstance().saveNino(wizardNino);
            }

            // Then: Settings should be updated with new NINO
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(DIFFERENT_NINO);
        }

        @Test
        @DisplayName("should not save empty NINO to Settings")
        void shouldNotSaveEmptyNinoToSettings() {
            // Given: Existing NINO in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // And: ViewModel has empty NINO (edge case)
            HmrcConnectionWizardViewModel viewModel = new HmrcConnectionWizardViewModel();
            viewModel.setNino("");

            // When: Simulating Step 5 load (auto-save logic)
            String wizardNino = viewModel.getNino();
            if (wizardNino != null && !wizardNino.isEmpty()) {
                SqliteDataStore.getInstance().saveNino(wizardNino);
            }

            // Then: Settings should still have original NINO
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(TEST_NINO);
        }

        @Test
        @DisplayName("should not save null NINO to Settings")
        void shouldNotSaveNullNinoToSettings() {
            // Given: Existing NINO in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // And: ViewModel has null NINO (edge case)
            HmrcConnectionWizardViewModel viewModel = new HmrcConnectionWizardViewModel();
            // getNino() returns "" for null, but let's test the logic

            // When: Simulating Step 5 load (auto-save logic)
            String wizardNino = viewModel.getNino();
            if (wizardNino != null && !wizardNino.isEmpty()) {
                SqliteDataStore.getInstance().saveNino(wizardNino);
            }

            // Then: Settings should still have original NINO
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(TEST_NINO);
        }
    }

    @Nested
    @DisplayName("Full Round-trip Sync")
    class FullRoundTripTests {

        @Test
        @DisplayName("should complete full sync cycle: Settings → Wizard → Settings")
        void shouldCompleteFullSyncCycle() {
            // Given: Initial NINO in Settings
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // Step 1: Load into ViewModel (Settings → Wizard)
            HmrcConnectionWizardViewModel viewModel = new HmrcConnectionWizardViewModel();
            String savedNino = SqliteDataStore.getInstance().loadNino();
            if (savedNino != null && !savedNino.isEmpty()) {
                viewModel.setNino(savedNino);
            }

            // Verify pre-population
            assertThat(viewModel.getNino()).isEqualTo(TEST_NINO);

            // Step 2: User completes wizard without changing NINO

            // Step 3: Save back to Settings (Wizard → Settings)
            String wizardNino = viewModel.getNino();
            if (wizardNino != null && !wizardNino.isEmpty()) {
                SqliteDataStore.getInstance().saveNino(wizardNino);
            }

            // Then: Settings should still have same NINO
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(TEST_NINO);
        }

        @Test
        @DisplayName("should sync corrected NINO back to Settings")
        void shouldSyncCorrectedNinoBackToSettings() {
            // Given: Initial NINO in Settings (with typo)
            SqliteDataStore.getInstance().saveNino(TEST_NINO);

            // Step 1: Load into ViewModel (Settings → Wizard)
            HmrcConnectionWizardViewModel viewModel = new HmrcConnectionWizardViewModel();
            String savedNino = SqliteDataStore.getInstance().loadNino();
            if (savedNino != null && !savedNino.isEmpty()) {
                viewModel.setNino(savedNino);
            }

            // Step 2: User corrects the NINO in wizard
            viewModel.setNino(DIFFERENT_NINO);

            // Step 3: Save back to Settings (Wizard → Settings)
            String wizardNino = viewModel.getNino();
            if (wizardNino != null && !wizardNino.isEmpty()) {
                SqliteDataStore.getInstance().saveNino(wizardNino);
            }

            // Then: Settings should have corrected NINO
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(DIFFERENT_NINO);
        }

        @Test
        @DisplayName("should handle new user with no existing NINO")
        void shouldHandleNewUserWithNoExistingNino() {
            // Given: No NINO in Settings (new user)
            assertThat(SqliteDataStore.getInstance().loadNino()).isNull();

            // Step 1: Load into ViewModel - nothing to pre-populate
            HmrcConnectionWizardViewModel viewModel = new HmrcConnectionWizardViewModel();
            String savedNino = SqliteDataStore.getInstance().loadNino();
            if (savedNino != null && !savedNino.isEmpty()) {
                viewModel.setNino(savedNino);
            }

            // Verify no pre-population
            assertThat(viewModel.getNino()).isEmpty();

            // Step 2: User enters NINO in wizard
            viewModel.setNino(TEST_NINO);

            // Step 3: Save to Settings (Wizard → Settings)
            String wizardNino = viewModel.getNino();
            if (wizardNino != null && !wizardNino.isEmpty()) {
                SqliteDataStore.getInstance().saveNino(wizardNino);
            }

            // Then: Settings should have the new NINO
            assertThat(SqliteDataStore.getInstance().loadNino()).isEqualTo(TEST_NINO);
        }
    }

    @Nested
    @DisplayName("NINO Format Preservation")
    class NinoFormatTests {

        @Test
        @DisplayName("should preserve NINO format through sync")
        void shouldPreserveNinoFormatThroughSync() {
            // Given: Formatted NINO
            String formattedNino = "AB 12 34 56 C";
            SqliteDataStore.getInstance().saveNino(formattedNino);

            // When: Round-trip through sync
            String loaded = SqliteDataStore.getInstance().loadNino();

            // Then: Format should be preserved
            assertThat(loaded).isEqualTo(formattedNino);
        }

        @Test
        @DisplayName("should handle uppercase conversion")
        void shouldHandleUppercaseConversion() {
            // Given: Lowercase NINO
            String lowercaseNino = "ab123456c";
            SqliteDataStore.getInstance().saveNino(lowercaseNino);

            // When: Loading from Settings
            String loaded = SqliteDataStore.getInstance().loadNino();

            // Then: Should be converted to uppercase (by saveNino)
            assertThat(loaded).isEqualTo("AB123456C");
        }
    }
}
