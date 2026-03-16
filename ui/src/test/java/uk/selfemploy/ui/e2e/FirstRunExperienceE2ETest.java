package uk.selfemploy.ui.e2e;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test for the first-run experience.
 *
 * <p>Verifies that a fresh application launch (empty database) displays
 * correct defaults across the dashboard and settings pages, ensuring
 * new users see the expected initial state.</p>
 */
@Tag("e2e")
@DisplayName("First-Run Experience E2E")
class FirstRunExperienceE2ETest extends BaseE2ETest {

    // === AC-1: Dashboard Fresh State ===

    @Nested
    @DisplayName("AC-1: Dashboard displays correctly on first run")
    class DashboardFreshState {

        @BeforeEach
        void ensureDashboardIsLoaded() {
            if (!lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()) {
                clickOn("#navDashboard");
                waitForFxEvents();
            }
        }

        @Test
        @DisplayName("TC-01: Dashboard loads with zero values")
        void dashboardLoadsWithZeroValues() {
            assertThat(lookup("#incomeValue").tryQuery()).isPresent();
            Label incomeValue = lookup("#incomeValue").queryAs(Label.class);
            assertThat(incomeValue.getText()).isEqualTo("£0.00");

            assertThat(lookup("#expensesValue").tryQuery()).isPresent();
            Label expensesValue = lookup("#expensesValue").queryAs(Label.class);
            assertThat(expensesValue.getText()).isEqualTo("£0.00");

            assertThat(lookup("#profitValue").tryQuery()).isPresent();
            Label profitValue = lookup("#profitValue").queryAs(Label.class);
            assertThat(profitValue.getText()).isEqualTo("£0.00");
        }

        @Test
        @DisplayName("TC-02: All navigation buttons are visible")
        void allNavigationButtonsVisible() {
            assertThat(nodeExists("#navDashboard")).isTrue();
            assertThat(nodeExists("#navIncome")).isTrue();
            assertThat(nodeExists("#navExpenses")).isTrue();
            assertThat(nodeExists("#navTax")).isTrue();
            assertThat(nodeExists("#navHmrc")).isTrue();
        }

        @Test
        @DisplayName("TC-03: Settings button is accessible")
        void settingsButtonAccessible() {
            assertThat(nodeExists("#settingsButton")).isTrue();
        }
    }

    // === AC-2: Settings Page Default State ===

    @Nested
    @DisplayName("AC-2: Settings page shows correct defaults on first run")
    class SettingsDefaultState {

        @BeforeEach
        void navigateToSettings() {
            clickOn("#settingsButton");
            waitForFxEvents();
            shortSleep();
        }

        @Test
        @DisplayName("TC-04: Settings page title is visible")
        void settingsPageTitleVisible() {
            assertThat(lookup(".page-title").tryQuery()).isPresent();
            Label title = lookup(".page-title").queryAs(Label.class);
            assertThat(title.getText()).isEqualTo("Settings");
        }

        @Test
        @DisplayName("TC-05: Display name shows 'Not set' by default")
        void displayNameDefaultNotSet() {
            assertThat(lookup("#displayNameLabel").tryQuery()).isPresent();
            Label displayNameLabel = lookup("#displayNameLabel").queryAs(Label.class);
            assertThat(displayNameLabel.getText()).isEqualTo("Not set");
        }

        @Test
        @DisplayName("TC-06: UTR shows 'Not set' by default")
        void utrDefaultNotSet() {
            assertThat(lookup("#utrLabel").tryQuery()).isPresent();
            Label utrLabel = lookup("#utrLabel").queryAs(Label.class);
            assertThat(utrLabel.getText()).isEqualTo("Not set");
        }

        @Test
        @DisplayName("TC-07: NINO shows 'Not set' by default")
        void ninoDefaultNotSet() {
            assertThat(lookup("#ninoLabel").tryQuery()).isPresent();
            Label ninoLabel = lookup("#ninoLabel").queryAs(Label.class);
            assertThat(ninoLabel.getText()).isEqualTo("Not set");
        }

        @Test
        @DisplayName("TC-08: HMRC credentials show 'Not configured' by default")
        void hmrcCredentialsDefaultNotConfigured() {
            assertThat(lookup("#hmrcCredentialsStatusLabel").tryQuery()).isPresent();
            Label statusLabel = lookup("#hmrcCredentialsStatusLabel").queryAs(Label.class);
            assertThat(statusLabel.getText()).isEqualTo("Not configured");
        }

        @Test
        @DisplayName("TC-09: Client ID field is empty by default")
        void clientIdFieldEmpty() {
            assertThat(lookup("#hmrcClientIdField").tryQuery()).isPresent();
            TextField clientIdField = lookup("#hmrcClientIdField").queryAs(TextField.class);
            assertThat(clientIdField.getText()).isEmpty();
        }

        @Test
        @DisplayName("TC-10: Client Secret field is empty by default")
        void clientSecretFieldEmpty() {
            assertThat(lookup("#hmrcClientSecretField").tryQuery()).isPresent();
            PasswordField secretField = lookup("#hmrcClientSecretField").queryAs(PasswordField.class);
            assertThat(secretField.getText()).isEmpty();
        }

        @Test
        @DisplayName("TC-11: HMRC connection shows 'Not set up' by default")
        void hmrcConnectionDefaultNotSetUp() {
            assertThat(lookup("#hmrcConnectionStatusLabel").tryQuery()).isPresent();
            Label connectionLabel = lookup("#hmrcConnectionStatusLabel").queryAs(Label.class);
            assertThat(connectionLabel.getText()).isEqualTo("Not set up");
        }

        @Test
        @DisplayName("TC-12: Environment defaults to Sandbox")
        void environmentDefaultsSandbox() {
            assertThat(lookup("#hmrcEnvironmentCombo").tryQuery()).isPresent();
            ComboBox<?> envCombo = lookup("#hmrcEnvironmentCombo").queryAs(ComboBox.class);
            assertThat(envCombo.getValue()).isNotNull();
            assertThat(envCombo.getValue().toString()).containsIgnoringCase("sandbox");
        }

        @Test
        @DisplayName("TC-13: Redirect URI field shows default localhost callback")
        void redirectUriShowsDefault() {
            assertThat(lookup("#hmrcRedirectUriField").tryQuery()).isPresent();
            TextField redirectField = lookup("#hmrcRedirectUriField").queryAs(TextField.class);
            assertThat(redirectField.getText()).isEqualTo("http://localhost:8088/oauth/callback");
            assertThat(redirectField.isEditable()).isFalse();
        }

        @Test
        @DisplayName("TC-14: Connect & Verify button is present")
        void connectButtonPresent() {
            assertThat(lookup("#hmrcSetupButton").tryQuery()).isPresent();
            Button setupButton = lookup("#hmrcSetupButton").queryAs(Button.class);
            assertThat(setupButton.getText()).isEqualTo("Connect & Verify");
        }

        @Test
        @DisplayName("TC-15: Disconnect button is hidden by default")
        void disconnectButtonHidden() {
            assertThat(lookup("#hmrcDisconnectButton").tryQuery()).isPresent();
            Button disconnectButton = lookup("#hmrcDisconnectButton").queryAs(Button.class);
            assertThat(disconnectButton.isVisible()).isFalse();
        }
    }

    // === AC-3: Settings Page Sections Visible ===

    @Nested
    @DisplayName("AC-3: All settings sections are present")
    class SettingsSectionsPresent {

        @BeforeEach
        void navigateToSettings() {
            clickOn("#settingsButton");
            waitForFxEvents();
            shortSleep();
        }

        @Test
        @DisplayName("TC-16: Profile section is visible")
        void profileSectionVisible() {
            assertThat(nodeExists(".settings-section-profile")).isTrue();
        }

        @Test
        @DisplayName("TC-17: HMRC API Credentials section is visible")
        void hmrcCredentialsSectionVisible() {
            assertThat(nodeExists(".settings-section-hmrc")).isTrue();
        }

        @Test
        @DisplayName("TC-18: Legal section is visible")
        void legalSectionVisible() {
            assertThat(nodeExists(".settings-section-legal")).isTrue();
        }

        @Test
        @DisplayName("TC-19: Data Management section is visible")
        void dataManagementSectionVisible() {
            assertThat(nodeExists(".settings-section-data")).isTrue();
        }

        @Test
        @DisplayName("TC-20: About section with version info is present")
        void aboutSectionPresent() {
            assertThat(lookup("#versionLabel").tryQuery()).isPresent();
            Label versionLabel = lookup("#versionLabel").queryAs(Label.class);
            assertThat(versionLabel.getText()).isNotEmpty();
        }

        @Test
        @DisplayName("TC-21: Legal buttons (Terms, Privacy, Disclaimer) are present")
        void legalButtonsPresent() {
            assertThat(nodeExists("#termsButton")).isTrue();
            assertThat(nodeExists("#privacyButton")).isTrue();
            assertThat(nodeExists("#disclaimerButton")).isTrue();
        }

        @Test
        @DisplayName("TC-22: Export and Import buttons are present")
        void dataManagementButtonsPresent() {
            assertThat(nodeExists("#exportButton")).isTrue();
            assertThat(nodeExists("#importButton")).isTrue();
        }
    }
}
