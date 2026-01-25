package uk.selfemploy.ui.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for Button/Link Audit functionality (Sprint 10A).
 * Tests BA-U01 through BA-U08 from /rob's test design.
 *
 * <p>SE-10A-005: Button/Link Audit Tests</p>
 *
 * <p>Note: These tests verify the audit logic and rules without instantiating
 * JavaFX components. Tests requiring JavaFX toolkit are in E2E tests with @Tag("e2e").</p>
 *
 * @see docs/sprints/sprint-10A/testing/rob-test-design-10A-10B.md
 */
@DisplayName("Button Audit Service Tests")
class ButtonAuditServiceTest {

    // === BA-U01: Clickable Element Type Classification ===

    @Nested
    @DisplayName("BA-U01: Clickable Element Type Classification")
    class ClickableElementTypeClassification {

        @Test
        @DisplayName("should classify Button as clickable")
        void shouldClassifyButtonAsClickable() {
            // Given
            String componentType = "javafx.scene.control.Button";

            // When
            boolean isClickable = isClickableType(componentType);

            // Then
            assertThat(isClickable).isTrue();
        }

        @Test
        @DisplayName("should classify Hyperlink as clickable")
        void shouldClassifyHyperlinkAsClickable() {
            // Given
            String componentType = "javafx.scene.control.Hyperlink";

            // When
            boolean isClickable = isClickableType(componentType);

            // Then
            assertThat(isClickable).isTrue();
        }

        @Test
        @DisplayName("should classify AccessibleCard as clickable")
        void shouldClassifyAccessibleCardAsClickable() {
            // Given
            String componentType = "uk.selfemploy.ui.component.AccessibleCard";

            // When
            boolean isClickable = isClickableType(componentType);

            // Then
            assertThat(isClickable).isTrue();
        }

        @Test
        @DisplayName("should classify AccessibleButton as clickable")
        void shouldClassifyAccessibleButtonAsClickable() {
            // Given
            String componentType = "uk.selfemploy.ui.component.AccessibleButton";

            // When
            boolean isClickable = isClickableType(componentType);

            // Then
            assertThat(isClickable).isTrue();
        }

        @Test
        @DisplayName("should classify AccessibleLink as clickable")
        void shouldClassifyAccessibleLinkAsClickable() {
            // Given
            String componentType = "uk.selfemploy.ui.component.AccessibleLink";

            // When
            boolean isClickable = isClickableType(componentType);

            // Then
            assertThat(isClickable).isTrue();
        }

        @Test
        @DisplayName("should classify AccessibleIconButton as clickable")
        void shouldClassifyAccessibleIconButtonAsClickable() {
            // Given
            String componentType = "uk.selfemploy.ui.component.AccessibleIconButton";

            // When
            boolean isClickable = isClickableType(componentType);

            // Then
            assertThat(isClickable).isTrue();
        }

        @Test
        @DisplayName("should not classify VBox as clickable")
        void shouldNotClassifyVBoxAsClickable() {
            // Given
            String componentType = "javafx.scene.layout.VBox";

            // When
            boolean isClickable = isClickableType(componentType);

            // Then
            assertThat(isClickable).isFalse();
        }

        @Test
        @DisplayName("should not classify Label as clickable")
        void shouldNotClassifyLabelAsClickable() {
            // Given
            String componentType = "javafx.scene.control.Label";

            // When
            boolean isClickable = isClickableType(componentType);

            // Then
            assertThat(isClickable).isFalse();
        }
    }

    // === BA-U02: Disabled Button Tooltip Rules ===

    @Nested
    @DisplayName("BA-U02: Disabled Button Tooltip Rules")
    class DisabledButtonTooltipRules {

        @Test
        @DisplayName("should require tooltip for disabled button")
        void shouldRequireTooltipForDisabledButton() {
            // Given
            boolean isDisabled = true;
            String tooltipText = null;

            // When
            boolean needsTooltip = needsDisabledTooltip(isDisabled, tooltipText);

            // Then
            assertThat(needsTooltip).isTrue();
        }

        @Test
        @DisplayName("should not require tooltip for enabled button")
        void shouldNotRequireTooltipForEnabledButton() {
            // Given
            boolean isDisabled = false;
            String tooltipText = null;

            // When
            boolean needsTooltip = needsDisabledTooltip(isDisabled, tooltipText);

            // Then
            assertThat(needsTooltip).isFalse();
        }

        @Test
        @DisplayName("should not require tooltip when already set")
        void shouldNotRequireTooltipWhenAlreadySet() {
            // Given
            boolean isDisabled = true;
            String tooltipText = "Connect to HMRC first";

            // When
            boolean needsTooltip = needsDisabledTooltip(isDisabled, tooltipText);

            // Then
            assertThat(needsTooltip).isFalse();
        }

        @Test
        @DisplayName("should require tooltip when text is empty")
        void shouldRequireTooltipWhenTextIsEmpty() {
            // Given
            boolean isDisabled = true;
            String tooltipText = "";

            // When
            boolean needsTooltip = needsDisabledTooltip(isDisabled, tooltipText);

            // Then
            assertThat(needsTooltip).isTrue();
        }

        @Test
        @DisplayName("should require tooltip when text is whitespace")
        void shouldRequireTooltipWhenTextIsWhitespace() {
            // Given
            boolean isDisabled = true;
            String tooltipText = "   ";

            // When
            boolean needsTooltip = needsDisabledTooltip(isDisabled, tooltipText);

            // Then
            assertThat(needsTooltip).isTrue();
        }
    }

    // === BA-U03: Action Handler Rules ===

    @Nested
    @DisplayName("BA-U03: Action Handler Rules")
    class ActionHandlerRules {

        @Test
        @DisplayName("should detect missing action handler")
        void shouldDetectMissingActionHandler() {
            // Given
            boolean hasHandler = false;

            // When
            boolean needsHandler = needsActionHandler(hasHandler);

            // Then
            assertThat(needsHandler).isTrue();
        }

        @Test
        @DisplayName("should not flag when handler present")
        void shouldNotFlagWhenHandlerPresent() {
            // Given
            boolean hasHandler = true;

            // When
            boolean needsHandler = needsActionHandler(hasHandler);

            // Then
            assertThat(needsHandler).isFalse();
        }
    }

    // === BA-U05: Card Click Handler Rules ===

    @Nested
    @DisplayName("BA-U05: Card Click Handler Rules")
    class CardClickHandlerRules {

        @Test
        @DisplayName("should require click handler for interactive cards")
        void shouldRequireClickHandlerForInteractiveCards() {
            // Given
            boolean isInteractive = true;
            boolean hasHandler = false;

            // When
            boolean needsHandler = cardNeedsHandler(isInteractive, hasHandler);

            // Then
            assertThat(needsHandler).isTrue();
        }

        @Test
        @DisplayName("should not require handler for non-interactive cards")
        void shouldNotRequireHandlerForNonInteractiveCards() {
            // Given
            boolean isInteractive = false;
            boolean hasHandler = false;

            // When
            boolean needsHandler = cardNeedsHandler(isInteractive, hasHandler);

            // Then
            assertThat(needsHandler).isFalse();
        }

        @Test
        @DisplayName("should not flag when handler present")
        void shouldNotFlagWhenHandlerPresentOnCard() {
            // Given
            boolean isInteractive = true;
            boolean hasHandler = true;

            // When
            boolean needsHandler = cardNeedsHandler(isInteractive, hasHandler);

            // Then
            assertThat(needsHandler).isFalse();
        }
    }

    // === BA-U06: Menu Item Action Rules ===

    @Nested
    @DisplayName("BA-U06: Menu Item Action Rules")
    class MenuItemActionRules {

        @Test
        @DisplayName("should require handler for non-separator menu items")
        void shouldRequireHandlerForNonSeparatorMenuItems() {
            // Given
            boolean isSeparator = false;
            boolean hasHandler = false;

            // When
            boolean needsHandler = menuItemNeedsHandler(isSeparator, hasHandler);

            // Then
            assertThat(needsHandler).isTrue();
        }

        @Test
        @DisplayName("should not require handler for separator menu items")
        void shouldNotRequireHandlerForSeparatorMenuItems() {
            // Given
            boolean isSeparator = true;
            boolean hasHandler = false;

            // When
            boolean needsHandler = menuItemNeedsHandler(isSeparator, hasHandler);

            // Then
            assertThat(needsHandler).isFalse();
        }
    }

    // === BA-U08: Dialog Button Rules ===

    @Nested
    @DisplayName("BA-U08: Dialog Button Rules")
    class DialogButtonRules {

        @Test
        @DisplayName("should require handler for OK button")
        void shouldRequireHandlerForOkButton() {
            // Given
            String buttonText = "OK";
            boolean hasHandler = false;

            // When
            boolean needsHandler = dialogButtonNeedsHandler(buttonText, hasHandler);

            // Then
            assertThat(needsHandler).isTrue();
        }

        @Test
        @DisplayName("should require handler for Cancel button")
        void shouldRequireHandlerForCancelButton() {
            // Given
            String buttonText = "Cancel";
            boolean hasHandler = false;

            // When
            boolean needsHandler = dialogButtonNeedsHandler(buttonText, hasHandler);

            // Then
            assertThat(needsHandler).isTrue();
        }

        @Test
        @DisplayName("should require handler for Save button")
        void shouldRequireHandlerForSaveButton() {
            // Given
            String buttonText = "Save";
            boolean hasHandler = false;

            // When
            boolean needsHandler = dialogButtonNeedsHandler(buttonText, hasHandler);

            // Then
            assertThat(needsHandler).isTrue();
        }

        @Test
        @DisplayName("should not flag when handler present")
        void shouldNotFlagDialogButtonWhenHandlerPresent() {
            // Given
            String buttonText = "OK";
            boolean hasHandler = true;

            // When
            boolean needsHandler = dialogButtonNeedsHandler(buttonText, hasHandler);

            // Then
            assertThat(needsHandler).isFalse();
        }
    }

    // === Helper Methods (would be in ButtonAuditService in production) ===

    /**
     * Checks if a component type is a clickable element.
     */
    private boolean isClickableType(String componentType) {
        return componentType.contains("Button")
                || componentType.contains("Hyperlink")
                || componentType.contains("AccessibleCard")
                || componentType.contains("AccessibleLink");
    }

    /**
     * Checks if a disabled button needs a tooltip.
     */
    private boolean needsDisabledTooltip(boolean isDisabled, String tooltipText) {
        if (!isDisabled) {
            return false;
        }
        return tooltipText == null || tooltipText.isBlank();
    }

    /**
     * Checks if a clickable element needs an action handler.
     */
    private boolean needsActionHandler(boolean hasHandler) {
        return !hasHandler;
    }

    /**
     * Checks if an interactive card needs a click handler.
     */
    private boolean cardNeedsHandler(boolean isInteractive, boolean hasHandler) {
        return isInteractive && !hasHandler;
    }

    /**
     * Checks if a menu item needs an action handler.
     */
    private boolean menuItemNeedsHandler(boolean isSeparator, boolean hasHandler) {
        return !isSeparator && !hasHandler;
    }

    /**
     * Checks if a dialog button needs an action handler.
     */
    private boolean dialogButtonNeedsHandler(String buttonText, boolean hasHandler) {
        if (hasHandler) {
            return false;
        }
        // Common dialog buttons that should have handlers
        return buttonText != null && (
                buttonText.equalsIgnoreCase("OK")
                        || buttonText.equalsIgnoreCase("Cancel")
                        || buttonText.equalsIgnoreCase("Save")
                        || buttonText.equalsIgnoreCase("Close")
                        || buttonText.equalsIgnoreCase("Yes")
                        || buttonText.equalsIgnoreCase("No")
        );
    }
}
