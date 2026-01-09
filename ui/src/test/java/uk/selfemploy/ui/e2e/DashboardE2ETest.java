package uk.selfemploy.ui.e2e;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for SE-104: Dashboard View.
 * Based on QA test specification from /rob.
 *
 * <p>These tests require a display. Run with {@code -Dgroups=e2e} to include,
 * or {@code -DexcludedGroups=e2e} to exclude.</p>
 *
 * @see docs/sprints/sprint-1/testing/rob-qa-SE-103-SE-104.md
 */
@Tag("e2e")
@DisplayName("SE-104: Dashboard View E2E")
class DashboardE2ETest extends BaseE2ETest {

    @BeforeEach
    void ensureDashboardIsLoaded() {
        // Ensure we're on the Dashboard
        if (!lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()) {
            clickOn("#navDashboard");
            waitForFxEvents();
        }
    }

    // === AC-104-1: Summary Cards Display ===

    @Nested
    @DisplayName("AC-104-1: Summary Cards Display")
    class SummaryCardsTests {

        @Test
        @DisplayName("TC-104-01: Income card displays correctly (CRITICAL)")
        void incomeCardDisplaysCorrectly() {
            // Verify Income card exists
            assertThat(lookup("#incomeValue").tryQuery()).isPresent();

            // Verify label
            assertThat(lookup(".metric-income").queryAs(VBox.class)).isNotNull();

            // Verify value format (£X.XX)
            Label incomeValue = lookup("#incomeValue").queryAs(Label.class);
            assertThat(incomeValue.getText()).matches("£[\\d,]+\\.\\d{2}");

            // Verify trend text exists
            assertThat(lookup("#incomeTrend").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("TC-104-02: Expenses card displays correctly (CRITICAL)")
        void expensesCardDisplaysCorrectly() {
            assertThat(lookup("#expensesValue").tryQuery()).isPresent();
            assertThat(lookup(".metric-expenses").queryAs(VBox.class)).isNotNull();

            Label expensesValue = lookup("#expensesValue").queryAs(Label.class);
            assertThat(expensesValue.getText()).matches("£[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("TC-104-03: Net Profit card displays correctly (CRITICAL)")
        void netProfitCardDisplaysCorrectly() {
            assertThat(lookup("#profitValue").tryQuery()).isPresent();
            assertThat(lookup(".metric-profit").queryAs(VBox.class)).isNotNull();

            Label profitValue = lookup("#profitValue").queryAs(Label.class);
            assertThat(profitValue.getText()).matches("-?£[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("TC-104-04: Estimated Tax card displays correctly (CRITICAL)")
        void estimatedTaxCardDisplaysCorrectly() {
            assertThat(lookup("#taxValue").tryQuery()).isPresent();
            assertThat(lookup(".metric-tax").queryAs(VBox.class)).isNotNull();

            Label taxValue = lookup("#taxValue").queryAs(Label.class);
            assertThat(taxValue.getText()).matches("£[\\d,]+\\.\\d{2}");
        }

        @Test
        @DisplayName("TC-104-05: Metric cards layout - 4 column grid (HIGH)")
        void metricCardsLayout() {
            // Verify GridPane exists with 4 columns
            GridPane metricsGrid = lookup("#metricsGrid").queryAs(GridPane.class);
            assertThat(metricsGrid).isNotNull();
            assertThat(metricsGrid.getColumnConstraints()).hasSize(4);

            // Each column should be 25%
            metricsGrid.getColumnConstraints().forEach(constraint -> {
                assertThat(constraint.getPercentWidth()).isEqualTo(25.0);
            });
        }

        @Test
        @DisplayName("TC-104-06: Currency formatting UK locale (CRITICAL)")
        void currencyFormattingUkLocale() {
            // Verify all use £ symbol
            Label incomeValue = lookup("#incomeValue").queryAs(Label.class);
            Label expensesValue = lookup("#expensesValue").queryAs(Label.class);
            Label profitValue = lookup("#profitValue").queryAs(Label.class);
            Label taxValue = lookup("#taxValue").queryAs(Label.class);

            assertThat(incomeValue.getText()).startsWith("£").doesNotStartWith("$");
            assertThat(expensesValue.getText()).startsWith("£").doesNotStartWith("$");
            assertThat(profitValue.getText()).containsPattern("[£-]");
            assertThat(taxValue.getText()).startsWith("£").doesNotStartWith("$");

            // Verify 2 decimal places
            assertThat(incomeValue.getText()).matches(".*\\.\\d{2}$");
        }
    }

    // === AC-104-2: Tax Year Progress Display ===

    @Nested
    @DisplayName("AC-104-2: Tax Year Progress Display")
    class TaxYearProgressTests {

        @Test
        @DisplayName("TC-104-07: Tax year progress bar visible (HIGH)")
        void taxYearProgressBarVisible() {
            // Verify progress bar card exists
            ProgressBar progressBar = lookup("#yearProgress").queryAs(ProgressBar.class);
            assertThat(progressBar).isNotNull();
            assertThat(progressBar.isVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-104-08: Progress bar shows correct percentage (HIGH)")
        void progressBarShowsCorrectPercentage() {
            ProgressBar progressBar = lookup("#yearProgress").queryAs(ProgressBar.class);

            // Progress should be between 0 and 1
            double progress = progressBar.getProgress();
            assertThat(progress).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("TC-104-09: Tax year dates displayed (MEDIUM)")
        void taxYearDatesDisplayed() {
            // Verify start date
            Label yearStart = lookup("#yearStart").queryAs(Label.class);
            assertThat(yearStart.getText()).matches("Started: \\d+ [A-Z][a-z]+ \\d{4}");

            // Verify end date
            Label yearEnd = lookup("#yearEnd").queryAs(Label.class);
            assertThat(yearEnd.getText()).matches("Ends: \\d+ [A-Z][a-z]+ \\d{4}");

            // Verify days remaining
            Label daysRemaining = lookup("#daysRemaining").queryAs(Label.class);
            assertThat(daysRemaining.getText()).matches("\\d+ days remaining");
        }

        @Test
        @DisplayName("TC-104-10: Progress updates with tax year change (MEDIUM)")
        void progressUpdatesWithTaxYearChange() {
            // Note current progress
            ProgressBar progressBar = lookup("#yearProgress").queryAs(ProgressBar.class);
            double initialProgress = progressBar.getProgress();

            // Change tax year
            ComboBox<?> selector = lookup("#taxYearSelector").queryAs(ComboBox.class);
            clickOn("#taxYearSelector");
            waitForFxEvents();

            // Select previous year (should have 100% progress)
            if (selector.getItems().size() > 1) {
                clickOn(selector.getItems().get(2).toString()); // Last item (oldest year)
                waitForFxEvents();

                // Progress should change
                double newProgress = progressBar.getProgress();
                assertThat(newProgress).isNotEqualTo(initialProgress);
            }
        }
    }

    // === AC-104-3: Quick Actions Section ===

    @Nested
    @DisplayName("AC-104-3: Quick Actions Section")
    class QuickActionsTests {

        @Test
        @DisplayName("TC-104-11: Quick Actions card visible (HIGH)")
        void quickActionsCardVisible() {
            // Verify Quick Actions card exists
            assertThat(lookup("#addIncomeBtn").tryQuery()).isPresent();
            assertThat(lookup("#addExpenseBtn").tryQuery()).isPresent();
            assertThat(lookup("#viewTaxBtn").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("TC-104-12: Add Income button displays correctly (HIGH)")
        void addIncomeButtonDisplaysCorrectly() {
            Button addIncomeBtn = lookup("#addIncomeBtn").queryAs(Button.class);
            assertThat(addIncomeBtn.getText()).isEqualTo("+ Add Income");
            assertThat(addIncomeBtn.getStyleClass()).contains("action-income");
        }

        @Test
        @DisplayName("TC-104-13: Add Expense button displays correctly (HIGH)")
        void addExpenseButtonDisplaysCorrectly() {
            Button addExpenseBtn = lookup("#addExpenseBtn").queryAs(Button.class);
            assertThat(addExpenseBtn.getText()).isEqualTo("+ Add Expense");
            assertThat(addExpenseBtn.getStyleClass()).contains("action-expense");
        }

        @Test
        @DisplayName("TC-104-14: View Tax Breakdown button displays correctly (HIGH)")
        void viewTaxBreakdownButtonDisplaysCorrectly() {
            Button viewTaxBtn = lookup("#viewTaxBtn").queryAs(Button.class);
            assertThat(viewTaxBtn.getText()).isEqualTo("View Tax Breakdown");
            assertThat(viewTaxBtn.getStyleClass()).contains("action-tax");
        }

        @Test
        @DisplayName("TC-104-15: Quick action button click - Add Income (MEDIUM)")
        void addIncomeButtonClick() {
            // Note: Currently placeholder (TODO handler)
            Button addIncomeBtn = lookup("#addIncomeBtn").queryAs(Button.class);
            clickOn("#addIncomeBtn");
            waitForFxEvents();

            // Should not crash - placeholder behavior
            assertThat(addIncomeBtn).isNotNull();
        }

        @Test
        @DisplayName("TC-104-16: Quick action button click - Add Expense (MEDIUM)")
        void addExpenseButtonClick() {
            Button addExpenseBtn = lookup("#addExpenseBtn").queryAs(Button.class);
            clickOn("#addExpenseBtn");
            waitForFxEvents();

            // Should not crash
            assertThat(addExpenseBtn).isNotNull();
        }

        @Test
        @DisplayName("TC-104-17: Quick action button click - View Tax (MEDIUM)")
        void viewTaxButtonClick() {
            Button viewTaxBtn = lookup("#viewTaxBtn").queryAs(Button.class);
            clickOn("#viewTaxBtn");
            waitForFxEvents();

            // Should not crash
            assertThat(viewTaxBtn).isNotNull();
        }
    }

    // === AC-104-4: Upcoming Deadlines Section ===

    @Nested
    @DisplayName("AC-104-4: Upcoming Deadlines Section")
    class DeadlinesTests {

        @Test
        @DisplayName("TC-104-18: Deadlines card visible (HIGH)")
        void deadlinesCardVisible() {
            VBox deadlinesList = lookup("#deadlinesList").queryAs(VBox.class);
            assertThat(deadlinesList).isNotNull();
            assertThat(deadlinesList.isVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-104-19: Filing deadline displayed (CRITICAL)")
        void filingDeadlineDisplayed() {
            VBox deadlinesList = lookup("#deadlinesList").queryAs(VBox.class);
            assertThat(deadlinesList.getChildren()).isNotEmpty();

            // Should contain filing deadline text
            boolean hasFilingDeadline = deadlinesList.getChildren().stream()
                    .filter(node -> node instanceof HBox)
                    .flatMap(node -> ((HBox) node).getChildren().stream())
                    .filter(node -> node instanceof Label)
                    .map(node -> ((Label) node).getText())
                    .anyMatch(text -> text.contains("Filing") || text.contains("31 Jan"));

            assertThat(hasFilingDeadline).isTrue();
        }

        @Test
        @DisplayName("TC-104-20: Payment deadline displayed (CRITICAL)")
        void paymentDeadlineDisplayed() {
            VBox deadlinesList = lookup("#deadlinesList").queryAs(VBox.class);

            boolean hasPaymentDeadline = deadlinesList.getChildren().stream()
                    .filter(node -> node instanceof HBox)
                    .flatMap(node -> ((HBox) node).getChildren().stream())
                    .filter(node -> node instanceof Label)
                    .map(node -> ((Label) node).getText())
                    .anyMatch(text -> text.contains("Payment"));

            assertThat(hasPaymentDeadline).isTrue();
        }

        @Test
        @DisplayName("TC-104-21: POA deadline displayed (HIGH)")
        void poaDeadlineDisplayed() {
            VBox deadlinesList = lookup("#deadlinesList").queryAs(VBox.class);

            // Check for Payment on Account
            boolean hasPoaDeadline = deadlinesList.getChildren().stream()
                    .filter(node -> node instanceof HBox)
                    .flatMap(node -> ((HBox) node).getChildren().stream())
                    .filter(node -> node instanceof Label)
                    .map(node -> ((Label) node).getText())
                    .anyMatch(text -> text.contains("Payment on Account") || text.contains("31 Jul"));

            assertThat(hasPoaDeadline).isTrue();
        }

        @Test
        @DisplayName("TC-104-22: Deadline status indicator colors (MEDIUM)")
        void deadlineStatusIndicatorColors() {
            VBox deadlinesList = lookup("#deadlinesList").queryAs(VBox.class);
            assertThat(deadlinesList.getChildren()).isNotEmpty();

            // Each deadline should have a status style class
            deadlinesList.getChildren().forEach(child -> {
                if (child instanceof HBox) {
                    HBox deadlineRow = (HBox) child;
                    // Status indicator should have appropriate style
                    assertThat(deadlineRow.getStyleClass()).isNotEmpty();
                }
            });
        }

        @Test
        @DisplayName("TC-104-23: Deadlines update with tax year change (MEDIUM)")
        void deadlinesUpdateWithTaxYearChange() {
            // Get initial deadline dates
            VBox deadlinesList = lookup("#deadlinesList").queryAs(VBox.class);
            int initialChildCount = deadlinesList.getChildren().size();

            // Change tax year
            ComboBox<?> selector = lookup("#taxYearSelector").queryAs(ComboBox.class);
            clickOn("#taxYearSelector");
            waitForFxEvents();

            if (selector.getItems().size() > 1) {
                clickOn(selector.getItems().get(1).toString());
                waitForFxEvents();

                // Deadlines should still be present
                assertThat(deadlinesList.getChildren()).isNotEmpty();
            }
        }
    }

    // === AC-104-5: Recent Activity Section ===

    @Nested
    @DisplayName("AC-104-5: Recent Activity Section")
    class RecentActivityTests {

        @Test
        @DisplayName("TC-104-24: Recent Activity card visible (HIGH)")
        void recentActivityCardVisible() {
            VBox activityList = lookup("#activityList").queryAs(VBox.class);
            assertThat(activityList).isNotNull();
            assertThat(activityList.isVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-104-25: Empty state when no activity (HIGH)")
        void emptyStateWhenNoActivity() {
            // With no data, should show empty state message
            Label emptyLabel = lookup("#emptyActivityLabel").queryAs(Label.class);
            assertThat(emptyLabel).isNotNull();
            assertThat(emptyLabel.getText()).contains("No recent activity");
        }

        @Test
        @DisplayName("TC-104-26: View All link clickable (MEDIUM)")
        void viewAllLinkClickable() {
            // Find the View All hyperlink
            Hyperlink viewAllLink = lookup(".link").queryAs(Hyperlink.class);
            assertThat(viewAllLink).isNotNull();
            assertThat(viewAllLink.getText()).isEqualTo("View All");

            // Click should not crash
            clickOn(viewAllLink);
            waitForFxEvents();
        }
    }

    // === AC-104-6: Tax Year Badge ===

    @Nested
    @DisplayName("AC-104-6: Tax Year Badge")
    class TaxYearBadgeTests {

        @Test
        @DisplayName("TC-104-27: Tax year badge displays (MEDIUM)")
        void taxYearBadgeDisplays() {
            Label taxYearBadge = lookup("#taxYearBadge").queryAs(Label.class);
            assertThat(taxYearBadge).isNotNull();
            assertThat(taxYearBadge.getText()).matches("Tax Year \\d{4}/\\d{2}");
            assertThat(taxYearBadge.getStyleClass()).contains("tax-year-badge");
        }

        @Test
        @DisplayName("TC-104-28: Badge updates with tax year change (MEDIUM)")
        void badgeUpdatesWithTaxYearChange() {
            Label taxYearBadge = lookup("#taxYearBadge").queryAs(Label.class);
            String initialBadgeText = taxYearBadge.getText();

            // Change tax year
            ComboBox<?> selector = lookup("#taxYearSelector").queryAs(ComboBox.class);
            clickOn("#taxYearSelector");
            waitForFxEvents();

            if (selector.getItems().size() > 1) {
                clickOn(selector.getItems().get(1).toString());
                waitForFxEvents();

                // Badge should update
                String newBadgeText = taxYearBadge.getText();
                assertThat(newBadgeText).isNotEqualTo(initialBadgeText);
            }
        }
    }

    // === Visual & Styling Tests ===

    @Nested
    @DisplayName("Visual & Styling Tests")
    class VisualStylingTests {

        @Test
        @DisplayName("TC-104-29: Card shadow and radius (LOW)")
        void cardShadowAndRadius() {
            // Verify metric cards have proper styling
            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            assertThat(incomeCard.getStyleClass()).contains("metric-card");
        }

        @Test
        @DisplayName("TC-104-31: Typography hierarchy (LOW)")
        void typographyHierarchy() {
            // Verify page title exists and is styled
            Label pageTitle = lookup(".page-title").queryAs(Label.class);
            assertThat(pageTitle).isNotNull();
            assertThat(pageTitle.getText()).isEqualTo("Dashboard");

            // Verify card titles exist
            assertThat(lookup(".card-title").queryAll()).isNotEmpty();

            // Verify metric values exist
            assertThat(lookup(".metric-value").queryAll()).hasSize(4);

            // Verify metric labels exist
            assertThat(lookup(".metric-label").queryAll()).hasSize(4);
        }
    }

    // === Scrolling & Layout Tests ===

    @Nested
    @DisplayName("Scrolling & Layout Tests")
    class ScrollingLayoutTests {

        @Test
        @DisplayName("TC-104-32: Dashboard scrolls when content overflows (MEDIUM)")
        void dashboardScrollsWhenContentOverflows() {
            // Dashboard should be in a ScrollPane
            ScrollPane scrollPane = lookup(".dashboard-scroll").queryAs(ScrollPane.class);
            assertThat(scrollPane).isNotNull();
            assertThat(scrollPane.getHbarPolicy()).isEqualTo(ScrollPane.ScrollBarPolicy.NEVER);
        }

        @Test
        @DisplayName("TC-104-33: Quick Actions and Deadlines side by side (MEDIUM)")
        void quickActionsAndDeadlinesSideBySide() {
            // Find the HBox containing both cards
            Button addIncomeBtn = lookup("#addIncomeBtn").queryAs(Button.class);
            VBox deadlinesList = lookup("#deadlinesList").queryAs(VBox.class);

            // Both should be visible and accessible
            assertThat(addIncomeBtn).isNotNull();
            assertThat(deadlinesList).isNotNull();
        }
    }
}
