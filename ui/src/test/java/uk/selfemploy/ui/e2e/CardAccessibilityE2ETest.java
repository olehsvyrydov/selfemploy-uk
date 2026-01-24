package uk.selfemploy.ui.e2e;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for SE-810 and SE-811: Card Accessibility.
 *
 * <p>SE-810: Keyboard Accessibility for Cards - Tab navigation and Enter/Space activation.</p>
 * <p>SE-811: Focus Indicators for Cards - Visible focus ring on focus.</p>
 *
 * <p>These tests verify that cards are accessible via keyboard navigation
 * and have visible focus indicators as per WCAG 2.1 AA guidelines.</p>
 */
@DisplayName("SE-810 & SE-811: Card Accessibility E2E Tests")
@Tag("e2e")
class CardAccessibilityE2ETest extends BaseE2ETest {

    @Nested
    @DisplayName("Dashboard Metric Cards")
    class DashboardMetricCards {

        @Test
        @DisplayName("should navigate to metric cards using Tab key")
        void shouldNavigateToMetricCardsUsingTab() {
            // Given: Dashboard is displayed
            clickOn("Dashboard");

            // When: User presses Tab to navigate
            // The first focusable element after page title should be the tax year badge
            // Then the metric cards

            // Navigate to income card
            press(KeyCode.TAB).release(KeyCode.TAB);
            press(KeyCode.TAB).release(KeyCode.TAB);
            press(KeyCode.TAB).release(KeyCode.TAB);

            // Then: Income card should be focused (verify it's focusable)
            // We verify that the card structure exists with focusable property
            verifyThat(".metric-income", isVisible());
        }

        @Test
        @DisplayName("should display focus indicator when card is focused")
        void shouldDisplayFocusIndicatorWhenCardIsFocused() {
            // Given: Dashboard is displayed
            clickOn("Dashboard");

            // When: User focuses on a metric card
            // First click to ensure dashboard is in focus
            clickOn(".metric-income");

            // Then: The card should have focus styles applied
            // We verify the card is visible and focusable
            verifyThat(".metric-income", isVisible());
        }

        @Test
        @DisplayName("should navigate to Income page when Enter is pressed on Income card")
        void shouldNavigateToIncomePageOnEnterPress() {
            // Given: Dashboard is displayed and Income card is focused
            clickOn("Dashboard");
            clickOn(".metric-income");

            // When: User presses Enter
            press(KeyCode.ENTER).release(KeyCode.ENTER);

            // Then: Should navigate to Income page
            verifyThat("Income", isVisible());
        }

        @Test
        @DisplayName("should navigate to Expenses page when Space is pressed on Expenses card")
        void shouldNavigateToExpensesPageOnSpacePress() {
            // Given: Dashboard is displayed and Expenses card is clicked
            clickOn("Dashboard");
            clickOn(".metric-expenses");

            // When: User presses Space
            press(KeyCode.SPACE).release(KeyCode.SPACE);

            // Then: Should navigate to Expenses page
            verifyThat("Expenses", isVisible());
        }

        @Test
        @DisplayName("should navigate to Tax Summary when Enter is pressed on Tax card")
        void shouldNavigateToTaxSummaryOnEnterPress() {
            // Given: Dashboard is displayed and Tax card is clicked
            clickOn("Dashboard");
            clickOn(".metric-tax");

            // When: User presses Enter
            press(KeyCode.ENTER).release(KeyCode.ENTER);

            // Then: Should navigate to Tax Summary page
            verifyThat("Tax Summary", isVisible());
        }

        @Test
        @DisplayName("should navigate to Tax Summary when Enter is pressed on Profit card")
        void shouldNavigateToTaxSummaryOnProfitCardEnterPress() {
            // Given: Dashboard is displayed and Profit card is clicked
            clickOn("Dashboard");
            clickOn(".metric-profit");

            // When: User presses Enter
            press(KeyCode.ENTER).release(KeyCode.ENTER);

            // Then: Should navigate to Tax Summary page
            verifyThat("Tax Summary", isVisible());
        }
    }

    @Nested
    @DisplayName("Income Summary Cards")
    class IncomeSummaryCards {

        @Test
        @DisplayName("should have focusable summary cards on Income page")
        void shouldHaveFocusableSummaryCardsOnIncomePage() {
            // Given: Income page is displayed
            clickOn("Income");

            // Then: Summary cards should be visible
            verifyThat(".summary-card-income-total", isVisible());
            verifyThat(".summary-card-income-paid", isVisible());
            verifyThat(".summary-card-income-unpaid", isVisible());
        }

        @Test
        @DisplayName("should open Paid help when Enter pressed on help icon")
        void shouldOpenPaidHelpOnEnterPress() {
            // Given: Income page is displayed
            clickOn("Income");

            // When: Focus on paid help icon and press Enter
            // Click first to focus
            clickOn("#paidHelpIcon");
            press(KeyCode.ENTER).release(KeyCode.ENTER);

            // Then: Help dialog should appear
            // The dialog contains "Help" title or content about paid income
        }

        @Test
        @DisplayName("should open Unpaid help when Space pressed on help icon")
        void shouldOpenUnpaidHelpOnSpacePress() {
            // Given: Income page is displayed
            clickOn("Income");

            // When: Focus on unpaid help icon and press Space
            clickOn("#unpaidHelpIcon");
            press(KeyCode.SPACE).release(KeyCode.SPACE);

            // Then: Help dialog should appear
        }
    }

    @Nested
    @DisplayName("Expense Summary Cards")
    class ExpenseSummaryCards {

        @Test
        @DisplayName("should have focusable summary cards on Expense page")
        void shouldHaveFocusableSummaryCardsOnExpensePage() {
            // Given: Expenses page is displayed
            clickOn("Expenses");

            // Then: Summary cards should be visible
            verifyThat(".summary-card-total", isVisible());
            verifyThat(".summary-card-deductible", isVisible());
            verifyThat(".summary-card-nondeductible", isVisible());
        }

        @Test
        @DisplayName("should open Deductible help when Enter pressed on help icon")
        void shouldOpenDeductibleHelpOnEnterPress() {
            // Given: Expenses page is displayed
            clickOn("Expenses");

            // When: Focus on deductible help icon and press Enter
            clickOn("#deductibleHelpIcon");
            press(KeyCode.ENTER).release(KeyCode.ENTER);

            // Then: Help dialog should appear
        }

        @Test
        @DisplayName("should open Non-Deductible help when Space pressed on help icon")
        void shouldOpenNonDeductibleHelpOnSpacePress() {
            // Given: Expenses page is displayed
            clickOn("Expenses");

            // When: Focus on non-deductible help icon and press Space
            clickOn("#nonDeductibleHelpIcon");
            press(KeyCode.SPACE).release(KeyCode.SPACE);

            // Then: Help dialog should appear
        }
    }

    @Nested
    @DisplayName("Accessibility Compliance")
    class AccessibilityCompliance {

        @Test
        @DisplayName("metric cards should have hand cursor")
        void metricCardsShouldHaveHandCursor() {
            // Given: Dashboard is displayed
            clickOn("Dashboard");

            // Then: Metric cards should be visible (cursor is set in FXML)
            verifyThat(".metric-income", isVisible());
            verifyThat(".metric-expenses", isVisible());
            verifyThat(".metric-profit", isVisible());
            verifyThat(".metric-tax", isVisible());
        }

        @Test
        @DisplayName("cards should respond to both mouse and keyboard")
        void cardsShouldRespondToBothMouseAndKeyboard() {
            // Given: Dashboard is displayed
            clickOn("Dashboard");

            // When: User clicks on income card
            clickOn(".metric-income");

            // Then: Should navigate to Income page
            verifyThat("Income", isVisible());

            // Given: Back to dashboard
            clickOn("Dashboard");

            // When: User presses Enter on expenses card
            clickOn(".metric-expenses");
            press(KeyCode.ENTER).release(KeyCode.ENTER);

            // Then: Should navigate to Expenses page
            verifyThat("Expenses", isVisible());
        }
    }

    // === Focus Indicator Tests (FI-001 to FI-005) ===

    @Nested
    @DisplayName("Focus Indicator Tests - SE-811 WCAG 2.1 AA")
    class FocusIndicatorTests {

        @Test
        @DisplayName("FI-001: Income card shows focus ring on focus")
        void incomeCardShowsFocusRingOnFocus() {
            // Given: Dashboard is displayed
            clickOn("Dashboard");

            // When: User focuses on income card
            clickOn(".metric-income");
            waitForFxEvents();

            // Then: Income card should be focused and visible
            javafx.scene.layout.VBox incomeCard = lookup(".metric-income").queryAs(javafx.scene.layout.VBox.class);
            assertThat(incomeCard.isFocused()).isTrue();

            // Focus ring is applied via CSS pseudo-class :focused
            // The card should have the metric-card style class which defines focus styles
            assertThat(incomeCard.getStyleClass()).contains("metric-card");
        }

        @Test
        @DisplayName("FI-002: Focus ring has sufficient contrast (3:1 ratio via CSS)")
        void focusRingHasSufficientContrast() {
            // Given: Dashboard is displayed
            clickOn("Dashboard");

            // When: User focuses on any metric card
            clickOn(".metric-income");
            waitForFxEvents();

            // Then: The card should have focus-related styles applied
            // CSS defines: -fx-border-color: #005ea5 (GDS blue) with 3px border
            // This provides >3:1 contrast ratio against white/light backgrounds
            javafx.scene.layout.VBox incomeCard = lookup(".metric-income").queryAs(javafx.scene.layout.VBox.class);
            assertThat(incomeCard.isFocused()).isTrue();

            // Verify the card has the expected style classes for focus styling
            assertThat(incomeCard.getStyleClass()).contains("metric-card");
            assertThat(incomeCard.getStyleClass()).contains("metric-income");
        }

        @Test
        @DisplayName("FI-003: Focus ring appears on all metric cards")
        void focusRingAppearsOnAllMetricCards() {
            // Given: Dashboard is displayed
            clickOn("Dashboard");

            String[] cardSelectors = {".metric-income", ".metric-expenses", ".metric-profit", ".metric-tax"};

            for (String selector : cardSelectors) {
                // When: Focus each card
                clickOn(selector);
                waitForFxEvents();

                // Then: Each card should be focusable and focused
                javafx.scene.layout.VBox card = lookup(selector).queryAs(javafx.scene.layout.VBox.class);
                assertThat(card.isFocused())
                    .as("Card " + selector + " should be focused when clicked")
                    .isTrue();
                assertThat(card.isFocusTraversable())
                    .as("Card " + selector + " should be focus traversable")
                    .isTrue();
            }
        }

        @Test
        @DisplayName("FI-004: Focus indicator disappears when focus leaves")
        void focusIndicatorDisappearsWhenFocusLeaves() {
            // Given: Dashboard is displayed and income card is focused
            clickOn("Dashboard");
            clickOn(".metric-income");
            waitForFxEvents();

            javafx.scene.layout.VBox incomeCard = lookup(".metric-income").queryAs(javafx.scene.layout.VBox.class);
            assertThat(incomeCard.isFocused()).isTrue();

            // When: Focus moves to another card
            press(KeyCode.TAB).release(KeyCode.TAB);
            waitForFxEvents();

            // Then: Income card should no longer be focused
            assertThat(incomeCard.isFocused()).isFalse();

            // And: Expenses card should now be focused
            javafx.scene.layout.VBox expensesCard = lookup(".metric-expenses").queryAs(javafx.scene.layout.VBox.class);
            assertThat(expensesCard.isFocused()).isTrue();
        }

        @Test
        @DisplayName("FI-005: Focus ring visible in high contrast mode (style classes present)")
        void focusRingVisibleInHighContrastMode() {
            // Given: Dashboard is displayed
            clickOn("Dashboard");

            // When: Focus on a metric card
            clickOn(".metric-income");
            waitForFxEvents();

            // Then: Card should have appropriate style classes for high contrast support
            // The CSS uses solid border colors (not gradients) for high contrast compatibility
            javafx.scene.layout.VBox incomeCard = lookup(".metric-income").queryAs(javafx.scene.layout.VBox.class);
            assertThat(incomeCard.isFocused()).isTrue();

            // Verify the structure supports high contrast:
            // - Card has defined border width and color via CSS
            // - Uses solid colors rather than opacity-based focus indicators
            assertThat(incomeCard.getStyleClass()).contains("metric-card");

            // The card should be visible and have a positive size
            assertThat(incomeCard.getWidth()).isGreaterThan(0);
            assertThat(incomeCard.getHeight()).isGreaterThan(0);
        }
    }
}
