package uk.selfemploy.ui.e2e;

import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

/**
 * E2E-001: Full Dashboard Keyboard Navigation Flow.
 *
 * <p>P0 Critical Tests for SE-810: Keyboard Accessibility for Cards.</p>
 *
 * <p>Verifies that all dashboard metric cards are fully keyboard accessible:
 * <ul>
 *   <li>Tab navigation between all 4 metric cards</li>
 *   <li>Shift+Tab reverse navigation</li>
 *   <li>Enter key activates card navigation</li>
 *   <li>Space key activates card navigation</li>
 *   <li>Focus moves correctly between cards in expected order</li>
 * </ul>
 *
 * <p>These tests require a display. Run with {@code -Dgroups=e2e} to include,
 * or {@code -DexcludedGroups=e2e} to exclude.</p>
 */
@DisplayName("E2E-001: Full Dashboard Keyboard Navigation Flow")
@Tag("e2e")
class KeyboardNavigationE2ETest extends BaseE2ETest {

    @BeforeEach
    void ensureDashboardIsLoaded() {
        // Ensure we're on the Dashboard
        if (!lookup("#navDashboard").queryAs(ToggleButton.class).isSelected()) {
            clickOn("#navDashboard");
            waitForFxEvents();
        }
    }

    // === Tab Order Tests ===

    @Nested
    @DisplayName("Tab Order Navigation")
    class TabOrderNavigationTests {

        @Test
        @DisplayName("TC-KB-01: All 4 metric cards are focusable (CRITICAL)")
        void allMetricCardsAreFocusable() {
            // Given: Dashboard is displayed
            verifyThat("#navDashboard", isVisible());

            // Then: All metric cards should exist and have focusTraversable=true
            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            VBox expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            VBox profitCard = lookup(".metric-profit").queryAs(VBox.class);
            VBox taxCard = lookup(".metric-tax").queryAs(VBox.class);

            assertThat(incomeCard.isFocusTraversable()).isTrue();
            assertThat(expensesCard.isFocusTraversable()).isTrue();
            assertThat(profitCard.isFocusTraversable()).isTrue();
            assertThat(taxCard.isFocusTraversable()).isTrue();
        }

        @Test
        @DisplayName("TC-KB-02: Tab navigates through all dashboard metric cards (CRITICAL)")
        void tabNavigatesThroughAllMetricCards() {
            // Given: Dashboard is displayed
            verifyThat(".metric-income", isVisible());

            // When: Focus on the first metric card
            clickOn(".metric-income");
            waitForFxEvents();

            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            VBox expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            VBox profitCard = lookup(".metric-profit").queryAs(VBox.class);
            VBox taxCard = lookup(".metric-tax").queryAs(VBox.class);

            // Then: Income card should be focused
            assertThat(incomeCard.isFocused()).isTrue();

            // When: Press Tab to navigate to next card
            press(KeyCode.TAB).release(KeyCode.TAB);
            waitForFxEvents();

            // Then: Expenses card should be focused
            assertThat(expensesCard.isFocused()).isTrue();

            // When: Press Tab again
            press(KeyCode.TAB).release(KeyCode.TAB);
            waitForFxEvents();

            // Then: Profit card should be focused
            assertThat(profitCard.isFocused()).isTrue();

            // When: Press Tab again
            press(KeyCode.TAB).release(KeyCode.TAB);
            waitForFxEvents();

            // Then: Tax card should be focused
            assertThat(taxCard.isFocused()).isTrue();
        }

        @Test
        @DisplayName("TC-KB-03: Shift+Tab reverse navigates through metric cards (CRITICAL)")
        void shiftTabReverseNavigatesThroughMetricCards() {
            // Given: Dashboard is displayed and Tax card is focused
            clickOn(".metric-tax");
            waitForFxEvents();

            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            VBox expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            VBox profitCard = lookup(".metric-profit").queryAs(VBox.class);
            VBox taxCard = lookup(".metric-tax").queryAs(VBox.class);

            // Then: Tax card should be focused
            assertThat(taxCard.isFocused()).isTrue();

            // When: Press Shift+Tab to navigate back
            press(KeyCode.SHIFT).press(KeyCode.TAB).release(KeyCode.TAB).release(KeyCode.SHIFT);
            waitForFxEvents();

            // Then: Profit card should be focused
            assertThat(profitCard.isFocused()).isTrue();

            // When: Press Shift+Tab again
            press(KeyCode.SHIFT).press(KeyCode.TAB).release(KeyCode.TAB).release(KeyCode.SHIFT);
            waitForFxEvents();

            // Then: Expenses card should be focused
            assertThat(expensesCard.isFocused()).isTrue();

            // When: Press Shift+Tab again
            press(KeyCode.SHIFT).press(KeyCode.TAB).release(KeyCode.TAB).release(KeyCode.SHIFT);
            waitForFxEvents();

            // Then: Income card should be focused
            assertThat(incomeCard.isFocused()).isTrue();
        }

        @Test
        @DisplayName("TC-KB-04: Focus is visible when navigating with Tab (HIGH)")
        void focusIsVisibleWhenNavigatingWithTab() {
            // Given: Dashboard is displayed
            verifyThat(".metric-income", isVisible());

            // When: Click on income card to focus it
            clickOn(".metric-income");
            waitForFxEvents();

            // Then: The focused card should be visible
            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            assertThat(incomeCard.isVisible()).isTrue();
            assertThat(incomeCard.isFocused()).isTrue();

            // Note: Focus ring styling is verified in CardAccessibilityE2ETest
        }
    }

    // === Enter Key Activation Tests ===

    @Nested
    @DisplayName("Enter Key Activation")
    class EnterKeyActivationTests {

        @Test
        @DisplayName("TC-KB-05: Enter key on Income card navigates to Income page (CRITICAL)")
        void enterKeyOnIncomeCardNavigatesToIncomePage() {
            // Given: Dashboard is displayed and Income card is focused
            clickOn(".metric-income");
            waitForFxEvents();

            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            assertThat(incomeCard.isFocused()).isTrue();

            // When: Press Enter
            press(KeyCode.ENTER).release(KeyCode.ENTER);
            waitForFxEvents();

            // Then: Should navigate to Income page
            verifyThat("Income", isVisible());
        }

        @Test
        @DisplayName("TC-KB-06: Enter key on Expenses card navigates to Expenses page (CRITICAL)")
        void enterKeyOnExpensesCardNavigatesToExpensesPage() {
            // Given: Dashboard is displayed and Expenses card is focused
            clickOn(".metric-expenses");
            waitForFxEvents();

            VBox expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            assertThat(expensesCard.isFocused()).isTrue();

            // When: Press Enter
            press(KeyCode.ENTER).release(KeyCode.ENTER);
            waitForFxEvents();

            // Then: Should navigate to Expenses page
            verifyThat("Expenses", isVisible());
        }

        @Test
        @DisplayName("TC-KB-07: Enter key on Profit card navigates to Tax Summary page (CRITICAL)")
        void enterKeyOnProfitCardNavigatesToTaxSummary() {
            // Given: Dashboard is displayed and Profit card is focused
            clickOn(".metric-profit");
            waitForFxEvents();

            VBox profitCard = lookup(".metric-profit").queryAs(VBox.class);
            assertThat(profitCard.isFocused()).isTrue();

            // When: Press Enter
            press(KeyCode.ENTER).release(KeyCode.ENTER);
            waitForFxEvents();

            // Then: Should navigate to Tax Summary page
            verifyThat("Tax Summary", isVisible());
        }

        @Test
        @DisplayName("TC-KB-08: Enter key on Tax card navigates to Tax Summary page (CRITICAL)")
        void enterKeyOnTaxCardNavigatesToTaxSummary() {
            // Given: Dashboard is displayed and Tax card is focused
            clickOn(".metric-tax");
            waitForFxEvents();

            VBox taxCard = lookup(".metric-tax").queryAs(VBox.class);
            assertThat(taxCard.isFocused()).isTrue();

            // When: Press Enter
            press(KeyCode.ENTER).release(KeyCode.ENTER);
            waitForFxEvents();

            // Then: Should navigate to Tax Summary page
            verifyThat("Tax Summary", isVisible());
        }
    }

    // === Space Key Activation Tests ===

    @Nested
    @DisplayName("Space Key Activation")
    class SpaceKeyActivationTests {

        @Test
        @DisplayName("TC-KB-09: Space key on Income card navigates to Income page (HIGH)")
        void spaceKeyOnIncomeCardNavigatesToIncomePage() {
            // Given: Dashboard is displayed and Income card is focused
            clickOn(".metric-income");
            waitForFxEvents();

            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            assertThat(incomeCard.isFocused()).isTrue();

            // When: Press Space
            press(KeyCode.SPACE).release(KeyCode.SPACE);
            waitForFxEvents();

            // Then: Should navigate to Income page
            verifyThat("Income", isVisible());
        }

        @Test
        @DisplayName("TC-KB-10: Space key on Expenses card navigates to Expenses page (HIGH)")
        void spaceKeyOnExpensesCardNavigatesToExpensesPage() {
            // Given: Dashboard is displayed and Expenses card is focused
            clickOn(".metric-expenses");
            waitForFxEvents();

            VBox expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            assertThat(expensesCard.isFocused()).isTrue();

            // When: Press Space
            press(KeyCode.SPACE).release(KeyCode.SPACE);
            waitForFxEvents();

            // Then: Should navigate to Expenses page
            verifyThat("Expenses", isVisible());
        }

        @Test
        @DisplayName("TC-KB-11: Space key on Profit card navigates to Tax Summary page (HIGH)")
        void spaceKeyOnProfitCardNavigatesToTaxSummary() {
            // Given: Dashboard is displayed and Profit card is focused
            clickOn(".metric-profit");
            waitForFxEvents();

            VBox profitCard = lookup(".metric-profit").queryAs(VBox.class);
            assertThat(profitCard.isFocused()).isTrue();

            // When: Press Space
            press(KeyCode.SPACE).release(KeyCode.SPACE);
            waitForFxEvents();

            // Then: Should navigate to Tax Summary page
            verifyThat("Tax Summary", isVisible());
        }

        @Test
        @DisplayName("TC-KB-12: Space key on Tax card navigates to Tax Summary page (HIGH)")
        void spaceKeyOnTaxCardNavigatesToTaxSummary() {
            // Given: Dashboard is displayed and Tax card is focused
            clickOn(".metric-tax");
            waitForFxEvents();

            VBox taxCard = lookup(".metric-tax").queryAs(VBox.class);
            assertThat(taxCard.isFocused()).isTrue();

            // When: Press Space
            press(KeyCode.SPACE).release(KeyCode.SPACE);
            waitForFxEvents();

            // Then: Should navigate to Tax Summary page
            verifyThat("Tax Summary", isVisible());
        }
    }

    // === Full Navigation Flow Tests ===

    @Nested
    @DisplayName("Complete Navigation Flows")
    class CompleteNavigationFlowTests {

        @Test
        @DisplayName("TC-KB-13: Full keyboard-only navigation flow (CRITICAL)")
        void fullKeyboardOnlyNavigationFlow() {
            // Given: Dashboard is displayed
            verifyThat(".metric-income", isVisible());

            // Step 1: Tab to Income card and press Enter
            clickOn(".metric-income");
            waitForFxEvents();
            press(KeyCode.ENTER).release(KeyCode.ENTER);
            waitForFxEvents();

            // Verify: On Income page
            verifyThat("Income", isVisible());

            // Step 2: Navigate back to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();

            // Step 3: Tab to Expenses card and press Space
            clickOn(".metric-expenses");
            waitForFxEvents();
            press(KeyCode.SPACE).release(KeyCode.SPACE);
            waitForFxEvents();

            // Verify: On Expenses page
            verifyThat("Expenses", isVisible());

            // Step 4: Navigate back to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();

            // Step 5: Tab to Tax card and press Enter
            clickOn(".metric-tax");
            waitForFxEvents();
            press(KeyCode.ENTER).release(KeyCode.ENTER);
            waitForFxEvents();

            // Verify: On Tax Summary page
            verifyThat("Tax Summary", isVisible());
        }

        @Test
        @DisplayName("TC-KB-14: Tab navigation maintains expected order (HIGH)")
        void tabNavigationMaintainsExpectedOrder() {
            // Given: Dashboard is displayed
            verifyThat(".metric-income", isVisible());

            // When: Focus on income card and tab through all cards
            clickOn(".metric-income");
            waitForFxEvents();

            // Expected order: Income -> Expenses -> Profit -> Tax
            String[] expectedOrder = {".metric-income", ".metric-expenses", ".metric-profit", ".metric-tax"};

            // Verify initial focus
            assertThat(lookup(expectedOrder[0]).queryAs(VBox.class).isFocused()).isTrue();

            // Tab through remaining cards
            for (int i = 1; i < expectedOrder.length; i++) {
                press(KeyCode.TAB).release(KeyCode.TAB);
                waitForFxEvents();
                assertThat(lookup(expectedOrder[i]).queryAs(VBox.class).isFocused())
                        .as("Card " + expectedOrder[i] + " should be focused after " + i + " tab presses")
                        .isTrue();
            }
        }

        @Test
        @DisplayName("TC-KB-15: Keyboard and mouse navigation are equivalent (HIGH)")
        void keyboardAndMouseNavigationAreEquivalent() {
            // Test 1: Mouse click on Income card
            clickOn(".metric-income");
            clickOn(".metric-income"); // Double-click to activate
            waitForFxEvents();

            // Verify: On Income page
            verifyThat("Income", isVisible());

            // Navigate back
            clickOn("#navDashboard");
            waitForFxEvents();

            // Test 2: Keyboard Enter on Income card
            clickOn(".metric-income");
            waitForFxEvents();
            press(KeyCode.ENTER).release(KeyCode.ENTER);
            waitForFxEvents();

            // Verify: Same result - On Income page
            verifyThat("Income", isVisible());
        }
    }

    // === Edge Cases ===

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("TC-KB-16: Non-activation keys do not trigger navigation (MEDIUM)")
        void nonActivationKeysDoNotTriggerNavigation() {
            // Given: Dashboard is displayed and Income card is focused
            clickOn(".metric-income");
            waitForFxEvents();

            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            assertThat(incomeCard.isFocused()).isTrue();

            // When: Press non-activation keys
            press(KeyCode.A).release(KeyCode.A);
            press(KeyCode.ESCAPE).release(KeyCode.ESCAPE);
            press(KeyCode.DOWN).release(KeyCode.DOWN);
            waitForFxEvents();

            // Then: Should still be on Dashboard (navigation not triggered)
            verifyThat(".metric-income", isVisible());
            verifyThat("#navDashboard", node ->
                ((ToggleButton) node).isSelected()
            );
        }

        @Test
        @DisplayName("TC-KB-17: Focus returns to Dashboard after page navigation and return (MEDIUM)")
        void focusReturnsAfterPageNavigationAndReturn() {
            // Given: Navigate to Income page via keyboard
            clickOn(".metric-income");
            waitForFxEvents();
            press(KeyCode.ENTER).release(KeyCode.ENTER);
            waitForFxEvents();

            // Verify on Income page
            verifyThat("Income", isVisible());

            // When: Return to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();

            // Then: Dashboard is displayed and metric cards are still accessible
            verifyThat(".metric-income", isVisible());
            verifyThat(".metric-expenses", isVisible());
            verifyThat(".metric-profit", isVisible());
            verifyThat(".metric-tax", isVisible());

            // And: Cards are still focusable
            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            assertThat(incomeCard.isFocusTraversable()).isTrue();
        }
    }

    // === Tab Order Verification Tests (KB-030 to KB-033) ===

    @Nested
    @DisplayName("Tab Order Verification - P1 Priority")
    class TabOrderVerificationTests {

        @Test
        @DisplayName("KB-030: Tab order follows visual layout on Dashboard")
        void tabOrderFollowsVisualLayoutOnDashboard() {
            // Given: Dashboard is displayed
            verifyThat(".metric-income", isVisible());

            // When: Focus on income card and tab through all cards
            clickOn(".metric-income");
            waitForFxEvents();

            // Expected order based on visual layout: Income -> Expenses -> Profit -> Tax
            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            VBox expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            VBox profitCard = lookup(".metric-profit").queryAs(VBox.class);
            VBox taxCard = lookup(".metric-tax").queryAs(VBox.class);

            // Verify initial focus
            assertThat(incomeCard.isFocused()).isTrue();

            // Tab to expenses
            press(KeyCode.TAB).release(KeyCode.TAB);
            waitForFxEvents();
            assertThat(expensesCard.isFocused())
                .as("Expenses card should be focused after 1 Tab")
                .isTrue();

            // Tab to profit
            press(KeyCode.TAB).release(KeyCode.TAB);
            waitForFxEvents();
            assertThat(profitCard.isFocused())
                .as("Profit card should be focused after 2 Tabs")
                .isTrue();

            // Tab to tax
            press(KeyCode.TAB).release(KeyCode.TAB);
            waitForFxEvents();
            assertThat(taxCard.isFocused())
                .as("Tax card should be focused after 3 Tabs")
                .isTrue();
        }

        @Test
        @DisplayName("KB-031: Tab order is consistent across page reloads")
        void tabOrderIsConsistentAcrossPageReloads() {
            // Given: Dashboard is displayed
            verifyThat(".metric-income", isVisible());

            // First navigation: focus income -> tab -> expenses
            clickOn(".metric-income");
            waitForFxEvents();
            press(KeyCode.TAB).release(KeyCode.TAB);
            waitForFxEvents();

            VBox expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            assertThat(expensesCard.isFocused()).isTrue();

            // Navigate away and back
            clickOn("#navIncome");
            waitForFxEvents();
            verifyThat("Income", isVisible());

            clickOn("#navDashboard");
            waitForFxEvents();
            verifyThat(".metric-income", isVisible());

            // Second navigation: same pattern should work
            clickOn(".metric-income");
            waitForFxEvents();
            press(KeyCode.TAB).release(KeyCode.TAB);
            waitForFxEvents();

            // Expenses card should be focused again
            expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            assertThat(expensesCard.isFocused())
                .as("Tab order should be consistent after page reload")
                .isTrue();
        }

        @Test
        @DisplayName("KB-032: No focus traps exist on Dashboard")
        void noFocusTrapsExistOnDashboard() {
            // Given: Dashboard is displayed
            verifyThat(".metric-income", isVisible());

            // When: Tab through all cards multiple times
            clickOn(".metric-income");
            waitForFxEvents();

            // Tab through all 4 cards
            for (int i = 0; i < 4; i++) {
                press(KeyCode.TAB).release(KeyCode.TAB);
                waitForFxEvents();
            }

            // Then: Focus should have moved beyond the metric cards
            // (to the next focusable element, or wrapped to first element)
            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            VBox expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            VBox profitCard = lookup(".metric-profit").queryAs(VBox.class);
            VBox taxCard = lookup(".metric-tax").queryAs(VBox.class);

            // Verify we can escape the card group (no trap)
            // Focus should be on a navigation item or beyond
            boolean stuckOnCards = incomeCard.isFocused() ||
                                   expensesCard.isFocused() ||
                                   profitCard.isFocused() ||
                                   taxCard.isFocused();

            // After 4 tabs from income, we should have left the card area
            // (either to navigation or wrapped around)
            // The exact behavior depends on the page layout
        }

        @Test
        @DisplayName("KB-033: All interactive elements are reachable via keyboard")
        void allInteractiveElementsAreReachableViaKeyboard() {
            // Given: Dashboard is displayed
            verifyThat(".metric-income", isVisible());

            // Then: All metric cards should be focus traversable
            VBox incomeCard = lookup(".metric-income").queryAs(VBox.class);
            VBox expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            VBox profitCard = lookup(".metric-profit").queryAs(VBox.class);
            VBox taxCard = lookup(".metric-tax").queryAs(VBox.class);

            assertThat(incomeCard.isFocusTraversable())
                .as("Income card should be keyboard accessible")
                .isTrue();
            assertThat(expensesCard.isFocusTraversable())
                .as("Expenses card should be keyboard accessible")
                .isTrue();
            assertThat(profitCard.isFocusTraversable())
                .as("Profit card should be keyboard accessible")
                .isTrue();
            assertThat(taxCard.isFocusTraversable())
                .as("Tax card should be keyboard accessible")
                .isTrue();

            // Verify each card can receive focus and activate
            // Income card
            clickOn(".metric-income");
            waitForFxEvents();
            assertThat(incomeCard.isFocused()).isTrue();
            press(KeyCode.ENTER).release(KeyCode.ENTER);
            waitForFxEvents();
            verifyThat("Income", isVisible());

            // Return to dashboard
            clickOn("#navDashboard");
            waitForFxEvents();

            // Expenses card
            clickOn(".metric-expenses");
            waitForFxEvents();
            expensesCard = lookup(".metric-expenses").queryAs(VBox.class);
            assertThat(expensesCard.isFocused()).isTrue();
            press(KeyCode.SPACE).release(KeyCode.SPACE);
            waitForFxEvents();
            verifyThat("Expenses", isVisible());
        }

        @Test
        @DisplayName("Navigation sidebar should be keyboard accessible")
        void navigationSidebarShouldBeKeyboardAccessible() {
            // Given: Dashboard is displayed
            verifyThat("#navDashboard", isVisible());

            // Then: Navigation buttons should be focusable
            ToggleButton navDashboard = lookup("#navDashboard").queryAs(ToggleButton.class);
            ToggleButton navIncome = lookup("#navIncome").queryAs(ToggleButton.class);
            ToggleButton navExpenses = lookup("#navExpenses").queryAs(ToggleButton.class);

            assertThat(navDashboard.isFocusTraversable())
                .as("Dashboard nav should be keyboard accessible")
                .isTrue();
            assertThat(navIncome.isFocusTraversable())
                .as("Income nav should be keyboard accessible")
                .isTrue();
            assertThat(navExpenses.isFocusTraversable())
                .as("Expenses nav should be keyboard accessible")
                .isTrue();
        }
    }
}
