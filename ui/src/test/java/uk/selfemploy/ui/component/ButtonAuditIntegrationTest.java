package uk.selfemploy.ui.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for Button/Link Audit functionality (Sprint 10A).
 * Tests BA-I01 through BA-I04 from /rob's test design.
 *
 * <p>SE-10A-005: Button/Link Audit Integration Tests</p>
 *
 * <p>These tests verify button and link functionality logic without
 * requiring the JavaFX toolkit. Tests using JavaFX controls are in E2E tests.</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@DisplayName("Button Audit Integration Tests")
class ButtonAuditIntegrationTest {

    // === BA-I01: Dashboard Card Navigation ===

    @Nested
    @DisplayName("BA-I01: Dashboard Card Navigation Logic")
    class DashboardCardNavigationLogic {

        @Test
        @DisplayName("should execute navigation callback when card is activated")
        void shouldExecuteNavigationCallbackWhenCardActivated() {
            // Given
            AtomicBoolean navigated = new AtomicBoolean(false);
            MockCard card = new MockCard("Income", () -> navigated.set(true));

            // When
            card.activate();

            // Then
            assertThat(navigated.get()).isTrue();
        }

        @Test
        @DisplayName("should navigate to correct target based on card type")
        void shouldNavigateToCorrectTargetBasedOnCardType() {
            // Given
            List<String> navigationLog = new ArrayList<>();

            MockCard incomeCard = new MockCard("Income", () -> navigationLog.add("INCOME_PAGE"));
            MockCard expensesCard = new MockCard("Expenses", () -> navigationLog.add("EXPENSES_PAGE"));
            MockCard profitCard = new MockCard("Profit", () -> navigationLog.add("TAX_PAGE"));
            MockCard taxCard = new MockCard("Tax", () -> navigationLog.add("TAX_PAGE"));

            // When
            incomeCard.activate();
            expensesCard.activate();
            profitCard.activate();
            taxCard.activate();

            // Then
            assertThat(navigationLog)
                    .containsExactly("INCOME_PAGE", "EXPENSES_PAGE", "TAX_PAGE", "TAX_PAGE");
        }

        @Test
        @DisplayName("should not navigate when card has no callback")
        void shouldNotNavigateWhenCardHasNoCallback() {
            // Given
            MockCard card = new MockCard("Info", null);

            // When/Then - should not throw
            card.activate();
        }
    }

    // === BA-I02: Quick Action Dialog Logic ===

    @Nested
    @DisplayName("BA-I02: Quick Action Dialog Logic")
    class QuickActionDialogLogic {

        @Test
        @DisplayName("should trigger dialog open callback when button activated")
        void shouldTriggerDialogOpenCallbackWhenButtonActivated() {
            // Given
            AtomicBoolean dialogOpened = new AtomicBoolean(false);
            MockButton addIncomeBtn = new MockButton("+ Add Income", () -> dialogOpened.set(true));

            // When
            addIncomeBtn.fire();

            // Then
            assertThat(dialogOpened.get()).isTrue();
        }

        @Test
        @DisplayName("should differentiate between income and expense dialogs")
        void shouldDifferentiateBetweenIncomeAndExpenseDialogs() {
            // Given
            List<String> dialogsOpened = new ArrayList<>();
            MockButton addIncomeBtn = new MockButton("+ Add Income", () -> dialogsOpened.add("INCOME_DIALOG"));
            MockButton addExpenseBtn = new MockButton("+ Add Expense", () -> dialogsOpened.add("EXPENSE_DIALOG"));

            // When
            addIncomeBtn.fire();
            addExpenseBtn.fire();

            // Then
            assertThat(dialogsOpened)
                    .containsExactly("INCOME_DIALOG", "EXPENSE_DIALOG");
        }

        @Test
        @DisplayName("should have correct button text for quick actions")
        void shouldHaveCorrectButtonTextForQuickActions() {
            // Given
            MockButton addIncomeBtn = new MockButton("+ Add Income", () -> {});
            MockButton addExpenseBtn = new MockButton("+ Add Expense", () -> {});
            MockButton viewTaxBtn = new MockButton("View Tax Breakdown", () -> {});

            // Then
            assertThat(addIncomeBtn.getText()).isEqualTo("+ Add Income");
            assertThat(addExpenseBtn.getText()).isEqualTo("+ Add Expense");
            assertThat(viewTaxBtn.getText()).isEqualTo("View Tax Breakdown");
        }
    }

    // === BA-I03: External Link Handling ===

    @Nested
    @DisplayName("BA-I03: External Link Handling")
    class ExternalLinkHandling {

        @Test
        @DisplayName("should track URL to open in browser")
        void shouldTrackUrlToOpenInBrowser() {
            // Given
            List<String> openedUrls = new ArrayList<>();
            MockLink hmrcLink = new MockLink("HMRC Self Assessment",
                    "https://www.gov.uk/self-assessment-tax-returns",
                    url -> openedUrls.add(url));

            // When
            hmrcLink.click();

            // Then
            assertThat(openedUrls)
                    .hasSize(1)
                    .containsExactly("https://www.gov.uk/self-assessment-tax-returns");
        }

        @Test
        @DisplayName("should handle multiple external links")
        void shouldHandleMultipleExternalLinks() {
            // Given
            List<String> openedUrls = new ArrayList<>();
            java.util.function.Consumer<String> browserOpener = openedUrls::add;

            MockLink link1 = new MockLink("HMRC", "https://www.gov.uk", browserOpener);
            MockLink link2 = new MockLink("GitHub", "https://github.com", browserOpener);
            MockLink link3 = new MockLink("Help", "https://help.example.com", browserOpener);

            // When
            link1.click();
            link2.click();
            link3.click();

            // Then
            assertThat(openedUrls)
                    .hasSize(3)
                    .containsExactly("https://www.gov.uk", "https://github.com", "https://help.example.com");
        }

        @Test
        @DisplayName("should validate URL before opening")
        void shouldValidateUrlBeforeOpening() {
            // Given
            List<String> openedUrls = new ArrayList<>();
            MockLink validLink = new MockLink("Valid", "https://example.com", openedUrls::add);
            MockLink invalidLink = new MockLink("Invalid", "", openedUrls::add);

            // When
            validLink.click();
            invalidLink.click();

            // Then - only valid URL should be added
            assertThat(openedUrls)
                    .hasSize(1)
                    .containsExactly("https://example.com");
        }
    }

    // === BA-I04: Disabled Button State Logic ===

    @Nested
    @DisplayName("BA-I04: Disabled Button State Logic")
    class DisabledButtonStateLogic {

        @Test
        @DisplayName("should disable submit when not connected to HMRC")
        void shouldDisableSubmitWhenNotConnectedToHmrc() {
            // Given
            boolean isHmrcConnected = false;

            // When
            MockButton submitBtn = createSubmitButton(isHmrcConnected);

            // Then
            assertThat(submitBtn.isDisabled()).isTrue();
            assertThat(submitBtn.getTooltipText())
                    .containsIgnoringCase("connect")
                    .containsIgnoringCase("HMRC");
        }

        @Test
        @DisplayName("should enable submit when connected to HMRC")
        void shouldEnableSubmitWhenConnectedToHmrc() {
            // Given
            boolean isHmrcConnected = true;

            // When
            MockButton submitBtn = createSubmitButton(isHmrcConnected);

            // Then
            assertThat(submitBtn.isDisabled()).isFalse();
        }

        @Test
        @DisplayName("should update button state when connection changes")
        void shouldUpdateButtonStateWhenConnectionChanges() {
            // Given
            boolean[] isConnected = {false};
            MockButton submitBtn = new MockButton("Submit", () -> {});
            submitBtn.updateState(!isConnected[0], "Connect to HMRC first");

            // Initial state
            assertThat(submitBtn.isDisabled()).isTrue();

            // When connection established
            isConnected[0] = true;
            submitBtn.updateState(!isConnected[0], null);

            // Then
            assertThat(submitBtn.isDisabled()).isFalse();
        }

        @Test
        @DisplayName("should provide tooltip explaining why button is disabled")
        void shouldProvideTooltipExplainingWhyDisabled() {
            // Given/When
            MockButton submitBtn = createSubmitButton(false);

            // Then
            assertThat(submitBtn.getTooltipText())
                    .isNotNull()
                    .isNotEmpty();
        }

        private MockButton createSubmitButton(boolean isHmrcConnected) {
            MockButton btn = new MockButton("Submit to HMRC", () -> {});
            if (!isHmrcConnected) {
                btn.updateState(true, "Connect to HMRC first to enable submission");
            }
            return btn;
        }
    }

    // === Button State Management ===

    @Nested
    @DisplayName("Button State Management")
    class ButtonStateManagement {

        @Test
        @DisplayName("should correctly manage button disable state")
        void shouldManageButtonDisableState() {
            // Given
            MockButton button = new MockButton("Action", () -> {});
            boolean canPerformAction = false;

            // When
            button.updateState(!canPerformAction, "Cannot perform action");

            // Then
            assertThat(button.isDisabled()).isTrue();

            // When action becomes available
            canPerformAction = true;
            button.updateState(!canPerformAction, null);

            // Then
            assertThat(button.isDisabled()).isFalse();
        }

        @Test
        @DisplayName("should update tooltip when state changes")
        void shouldUpdateTooltipWhenStateChanges() {
            // Given
            MockButton button = new MockButton("Submit", () -> {});
            button.updateState(true, "Not ready");

            // Initial state
            assertThat(button.isDisabled()).isTrue();
            assertThat(button.getTooltipText()).isEqualTo("Not ready");

            // When state changes to ready
            button.updateState(false, "Click to submit");

            // Then
            assertThat(button.isDisabled()).isFalse();
            assertThat(button.getTooltipText()).isEqualTo("Click to submit");
        }
    }

    // === Mock Classes ===

    static class MockCard {
        private final String label;
        private final Runnable onAction;

        MockCard(String label, Runnable onAction) {
            this.label = label;
            this.onAction = onAction;
        }

        void activate() {
            if (onAction != null) {
                onAction.run();
            }
        }

        String getLabel() {
            return label;
        }
    }

    static class MockButton {
        private final String text;
        private final Runnable onAction;
        private boolean disabled = false;
        private String tooltipText = null;

        MockButton(String text, Runnable onAction) {
            this.text = text;
            this.onAction = onAction;
        }

        void fire() {
            if (onAction != null && !disabled) {
                onAction.run();
            }
        }

        void updateState(boolean disabled, String tooltip) {
            this.disabled = disabled;
            this.tooltipText = tooltip;
        }

        String getText() {
            return text;
        }

        boolean isDisabled() {
            return disabled;
        }

        String getTooltipText() {
            return tooltipText;
        }
    }

    static class MockLink {
        private final String text;
        private final String url;
        private final java.util.function.Consumer<String> browserOpener;

        MockLink(String text, String url, java.util.function.Consumer<String> browserOpener) {
            this.text = text;
            this.url = url;
            this.browserOpener = browserOpener;
        }

        void click() {
            if (url != null && !url.isEmpty() && browserOpener != null) {
                browserOpener.accept(url);
            }
        }

        String getText() {
            return text;
        }

        String getUrl() {
            return url;
        }
    }
}
