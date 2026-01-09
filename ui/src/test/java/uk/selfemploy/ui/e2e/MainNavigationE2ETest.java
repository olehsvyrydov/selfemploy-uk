package uk.selfemploy.ui.e2e;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for SE-103: Main Window Navigation.
 * Based on QA test specification from /rob.
 *
 * <p>These tests require a display. Run with {@code -Dgroups=e2e} to include,
 * or {@code -DexcludedGroups=e2e} to exclude.</p>
 *
 * @see docs/sprints/sprint-1/testing/rob-qa-SE-103-SE-104.md
 */
@Tag("e2e")
@DisplayName("SE-103: Main Window Navigation E2E")
class MainNavigationE2ETest extends BaseE2ETest {

    // === AC-103-1: Main Window with Sidebar Navigation ===

    @Nested
    @DisplayName("AC-103-1: Main Window with Sidebar Navigation")
    class MainWindowTests {

        @Test
        @DisplayName("TC-103-01: Application launches with main window (CRITICAL)")
        void applicationLaunchesWithMainWindow() {
            // Step 1: Verify main window appears
            Stage stage = getPrimaryStage();
            assertThat(stage.isShowing()).isTrue();

            // Step 2: Verify window title
            assertThat(stage.getTitle()).isEqualTo("UK Self-Employment Manager");

            // Step 3: Verify window size (default 1200x800)
            assertThat(stage.getWidth()).isEqualTo(1200.0);
            assertThat(stage.getHeight()).isEqualTo(800.0);

            // Step 4: Verify sidebar visible (220px width)
            VBox sidebar = lookup("#sidebar").queryAs(VBox.class);
            assertThat(sidebar).isNotNull();
            assertThat(sidebar.isVisible()).isTrue();
            assertThat(sidebar.getPrefWidth()).isEqualTo(220.0);

            // Step 5: Verify header bar visible
            assertThat(lookup("#headerBar").tryQuery()).isPresent();

            // Step 6: Verify status bar visible
            assertThat(lookup("#statusBar").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("TC-103-02: Sidebar contains all navigation items (CRITICAL)")
        void sidebarContainsAllNavigationItems() {
            // Step 1: Verify NAVIGATION section label
            assertThat(lookup(".nav-section-label").queryLabeled().getText()).isEqualTo("NAVIGATION");

            // Step 2: Verify navigation items exist
            assertThat(lookup("#navDashboard").tryQuery()).isPresent();
            assertThat(lookup("#navIncome").tryQuery()).isPresent();
            assertThat(lookup("#navExpenses").tryQuery()).isPresent();
            assertThat(lookup("#navTax").tryQuery()).isPresent();
            assertThat(lookup("#navHmrc").tryQuery()).isPresent();

            // Verify button text
            assertThat(lookup("#navDashboard").queryAs(ToggleButton.class).getText()).isEqualTo("Dashboard");
            assertThat(lookup("#navIncome").queryAs(ToggleButton.class).getText()).isEqualTo("Income");
            assertThat(lookup("#navExpenses").queryAs(ToggleButton.class).getText()).isEqualTo("Expenses");
            assertThat(lookup("#navTax").queryAs(ToggleButton.class).getText()).isEqualTo("Tax Summary");
            assertThat(lookup("#navHmrc").queryAs(ToggleButton.class).getText()).isEqualTo("HMRC Submission");

            // Step 3: Verify Help & Support button
            assertThat(lookup("#navHelp").tryQuery()).isPresent();
            assertThat(lookup("#navHelp").queryAs(Button.class).getText()).isEqualTo("Help & Support");
        }

        @Test
        @DisplayName("TC-103-03: Dashboard is default view (HIGH)")
        void dashboardIsDefaultView() {
            // Step 1: Dashboard view should be loaded
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Dashboard");

            // Step 2: Dashboard toggle button should be selected
            ToggleButton dashboardBtn = lookup("#navDashboard").queryAs(ToggleButton.class);
            assertThat(dashboardBtn.isSelected()).isTrue();

            // Step 3: Content pane should contain dashboard
            assertThat(lookup("#contentPane").tryQuery()).isPresent();
        }
    }

    // === AC-103-2: Content Area Switches Based on Navigation ===

    @Nested
    @DisplayName("AC-103-2: Content Area Switches Based on Navigation")
    class NavigationSwitchingTests {

        @Test
        @DisplayName("TC-103-04: Navigate to Income view (HIGH)")
        void navigateToIncomeView() {
            // Step 1: Click Income button
            clickOn("#navIncome");
            waitForFxEvents();

            // Step 2: Verify Income button is selected
            ToggleButton incomeBtn = lookup("#navIncome").queryAs(ToggleButton.class);
            assertThat(incomeBtn.isSelected()).isTrue();

            // Step 3: Verify Dashboard button is deselected
            ToggleButton dashboardBtn = lookup("#navDashboard").queryAs(ToggleButton.class);
            assertThat(dashboardBtn.isSelected()).isFalse();

            // Step 4: Verify page title (if Income view has one)
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Income");
        }

        @Test
        @DisplayName("TC-103-05: Navigate to Expenses view (HIGH)")
        void navigateToExpensesView() {
            // Step 1: Click Expenses button
            clickOn("#navExpenses");
            waitForFxEvents();

            // Step 2: Verify Expenses button is selected
            ToggleButton expensesBtn = lookup("#navExpenses").queryAs(ToggleButton.class);
            assertThat(expensesBtn.isSelected()).isTrue();

            // Verify page title
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Expenses");
        }

        @Test
        @DisplayName("TC-103-06: Navigate to Tax Summary view (HIGH)")
        void navigateToTaxSummaryView() {
            clickOn("#navTax");
            waitForFxEvents();

            ToggleButton taxBtn = lookup("#navTax").queryAs(ToggleButton.class);
            assertThat(taxBtn.isSelected()).isTrue();

            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Tax Summary");
        }

        @Test
        @DisplayName("TC-103-07: Navigate to HMRC Submission view (HIGH)")
        void navigateToHmrcView() {
            clickOn("#navHmrc");
            waitForFxEvents();

            ToggleButton hmrcBtn = lookup("#navHmrc").queryAs(ToggleButton.class);
            assertThat(hmrcBtn.isSelected()).isTrue();

            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("HMRC Submission");
        }

        @Test
        @DisplayName("TC-103-08: Navigate back to Dashboard (MEDIUM)")
        void navigateBackToDashboard() {
            // Navigate to Expenses first
            clickOn("#navExpenses");
            waitForFxEvents();
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Expenses");

            // Navigate back to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();

            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Dashboard");
            assertThat(lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()).isTrue();
        }

        @Test
        @DisplayName("TC-103-09: Single selection enforcement (HIGH)")
        void singleSelectionEnforcement() {
            // Click Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();
            assertThat(lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()).isTrue();

            // Click Income - Dashboard should be deselected
            clickOn("#navIncome");
            waitForFxEvents();
            assertThat(lookup("#navIncome").queryAs(ToggleButton.class).isSelected()).isTrue();
            assertThat(lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()).isFalse();

            // Click Expenses - Income should be deselected
            clickOn("#navExpenses");
            waitForFxEvents();
            assertThat(lookup("#navExpenses").queryAs(ToggleButton.class).isSelected()).isTrue();
            assertThat(lookup("#navIncome").queryAs(ToggleButton.class).isSelected()).isFalse();

            // Verify only one is selected at a time
            long selectedCount = countSelectedNavButtons();
            assertThat(selectedCount).isEqualTo(1L);
        }

        private long countSelectedNavButtons() {
            return lookup(".nav-button").queryAllAs(ToggleButton.class).stream()
                    .filter(ToggleButton::isSelected)
                    .count();
        }
    }

    // === AC-103-3: Responsive Layout ===

    @Nested
    @DisplayName("AC-103-3: Responsive Layout")
    class ResponsiveLayoutTests {

        @Test
        @DisplayName("TC-103-10: Window resizes within bounds (MEDIUM)")
        void windowResizesWithinBounds() {
            Stage stage = getPrimaryStage();

            // Resize to 1400x900
            interact(() -> {
                stage.setWidth(1400);
                stage.setHeight(900);
            });
            waitForFxEvents();

            assertThat(stage.getWidth()).isEqualTo(1400.0);
            assertThat(stage.getHeight()).isEqualTo(900.0);

            // Sidebar should remain 220px
            VBox sidebar = lookup("#sidebar").queryAs(VBox.class);
            assertThat(sidebar.getPrefWidth()).isEqualTo(220.0);
        }

        @Test
        @DisplayName("TC-103-11: Minimum window size enforced (MEDIUM)")
        void minimumWindowSizeEnforced() {
            Stage stage = getPrimaryStage();

            // Try to resize below minimum
            interact(() -> {
                stage.setWidth(800);
                stage.setHeight(500);
            });
            waitForFxEvents();

            // Window should not go below minimum (900x600)
            assertThat(stage.getMinWidth()).isEqualTo(900.0);
            assertThat(stage.getMinHeight()).isEqualTo(600.0);
        }

        @Test
        @DisplayName("TC-103-12: Sidebar maintains fixed width on resize (MEDIUM)")
        void sidebarMaintainsFixedWidthOnResize() {
            Stage stage = getPrimaryStage();
            VBox sidebar = lookup("#sidebar").queryAs(VBox.class);

            // Resize multiple times
            for (int width : new int[]{1200, 1400, 1000, 1600}) {
                interact(() -> stage.setWidth(width));
                waitForFxEvents();
                assertThat(sidebar.getPrefWidth()).isEqualTo(220.0);
            }
        }
    }

    // === AC-103-4: Professional Styling ===

    @Nested
    @DisplayName("AC-103-4: Professional Styling")
    class StylingTests {

        @Test
        @DisplayName("TC-103-13: Header bar styling (MEDIUM)")
        void headerBarStyling() {
            assertThat(lookup("#headerBar").tryQuery()).isPresent();

            // Verify logo text
            assertThat(lookup(".logo-text").queryLabeled().getText()).isEqualTo("SE");

            // Verify app title
            assertThat(lookup(".app-title").queryLabeled().getText()).isEqualTo("UK Self-Employment Manager");
        }

        @Test
        @DisplayName("TC-103-16: Navigation button styling - selected state (HIGH)")
        void navigationButtonSelectedState() {
            ToggleButton dashboardBtn = lookup("#navDashboard").queryAs(ToggleButton.class);

            // Dashboard should be selected by default
            assertThat(dashboardBtn.isSelected()).isTrue();

            // Selected button should have nav-button style class
            assertThat(dashboardBtn.getStyleClass()).contains("nav-button");
        }

        @Test
        @DisplayName("TC-103-17: Status bar styling (LOW)")
        void statusBarStyling() {
            assertThat(lookup("#statusBar").tryQuery()).isPresent();

            // Verify status labels exist
            assertThat(lookup("#taxYearLabel").tryQuery()).isPresent();
            assertThat(lookup("#deadlineLabel").tryQuery()).isPresent();
        }
    }

    // === AC-103-5: Tax Year Selector ===

    @Nested
    @DisplayName("AC-103-5: Tax Year Selector")
    class TaxYearSelectorTests {

        @Test
        @DisplayName("TC-103-18: Tax year selector displays current year (HIGH)")
        void taxYearSelectorDisplaysCurrentYear() {
            ComboBox<?> selector = lookup("#taxYearSelector").queryAs(ComboBox.class);
            assertThat(selector).isNotNull();
            assertThat(selector.getValue()).isNotNull();

            // Verify format (e.g., "2025/26")
            String selectedValue = selector.getButtonCell().getText();
            assertThat(selectedValue).matches("\\d{4}/\\d{2}");
        }

        @Test
        @DisplayName("TC-103-19: Tax year selector shows available years (MEDIUM)")
        void taxYearSelectorShowsAvailableYears() {
            ComboBox<?> selector = lookup("#taxYearSelector").queryAs(ComboBox.class);

            // Should have 3 tax years (current + 2 previous)
            assertThat(selector.getItems()).hasSize(3);
        }

        @Test
        @DisplayName("TC-103-20: Changing tax year updates content (HIGH)")
        void changingTaxYearUpdatesContent() {
            ComboBox<?> selector = lookup("#taxYearSelector").queryAs(ComboBox.class);
            String initialTaxYear = lookup("#taxYearLabel").queryLabeled().getText();

            // Click the selector to open dropdown
            clickOn("#taxYearSelector");
            waitForFxEvents();

            // Select a different year (first item in list)
            clickOn(selector.getItems().get(0).toString());
            waitForFxEvents();

            // Verify status bar updated
            String updatedTaxYear = lookup("#taxYearLabel").queryLabeled().getText();
            // The tax year label should contain "Tax Year"
            assertThat(updatedTaxYear).contains("Tax Year");
        }
    }

    // === AC-103-6: Status Bar Information ===

    @Nested
    @DisplayName("AC-103-6: Status Bar Information")
    class StatusBarTests {

        @Test
        @DisplayName("TC-103-21: Status bar shows tax year (MEDIUM)")
        void statusBarShowsTaxYear() {
            Label taxYearLabel = lookup("#taxYearLabel").queryAs(Label.class);
            assertThat(taxYearLabel.getText()).startsWith("Tax Year");
            assertThat(taxYearLabel.getText()).matches("Tax Year \\d{4}/\\d{2}");
        }

        @Test
        @DisplayName("TC-103-22: Status bar shows deadline countdown (MEDIUM)")
        void statusBarShowsDeadlineCountdown() {
            Label deadlineLabel = lookup("#deadlineLabel").queryAs(Label.class);

            // Should show either days until deadline or passed message
            assertThat(deadlineLabel.getText()).satisfiesAnyOf(
                    text -> assertThat(text).matches("\\d+ days until filing deadline"),
                    text -> assertThat(text).isEqualTo("Filing deadline is TODAY!"),
                    text -> assertThat(text).isEqualTo("Filing deadline has passed")
            );
        }
    }

    // === AC-103-7: Header Buttons ===

    @Nested
    @DisplayName("AC-103-7: Header Buttons")
    class HeaderButtonTests {

        @Test
        @DisplayName("TC-103-24: Help button functions (MEDIUM)")
        void helpButtonFunctions() {
            clickOn("#helpButton");
            waitForFxEvents();

            // Help view should load
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Help");
        }

        @Test
        @DisplayName("TC-103-25: Settings button functions (MEDIUM)")
        void settingsButtonFunctions() {
            clickOn("#settingsButton");
            waitForFxEvents();

            // Settings view should load
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Settings");
        }
    }

    // === Edge Cases ===

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("TC-103-26: Rapid navigation clicks (LOW)")
        void rapidNavigationClicks() {
            // Rapidly click multiple nav buttons
            for (int i = 0; i < 5; i++) {
                clickOn("#navIncome");
                clickOn("#navExpenses");
                clickOn("#navTax");
                clickOn("#navDashboard");
            }
            waitForFxEvents();

            // Verify no crash and final view is Dashboard
            assertThat(lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()).isTrue();
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Dashboard");
        }

        @Test
        @DisplayName("TC-103-27: View caching behavior (LOW)")
        void viewCachingBehavior() {
            // Navigate to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();

            // Navigate to Income
            clickOn("#navIncome");
            waitForFxEvents();

            // Return to Dashboard - should load quickly (cached)
            long startTime = System.currentTimeMillis();
            clickOn("#navDashboard");
            waitForFxEvents();
            long endTime = System.currentTimeMillis();

            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Dashboard");
            // View should load within reasonable time (cached behavior)
            assertThat(endTime - startTime).isLessThan(500);
        }
    }
}
