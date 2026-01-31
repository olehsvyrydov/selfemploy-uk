package uk.selfemploy.ui.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WizardProgressIndicator component.
 * Sprint 12 - SE-12-001: Prerequisites Checklist Screen
 *
 * <p>Tests the reusable step progress indicator that shows wizard progress
 * with active, pending, and completed states.</p>
 *
 * <p>Note: Tagged as "e2e" because this test instantiates JavaFX components
 * which require the JavaFX toolkit to be initialized.</p>
 */
@Tag("e2e")
@DisplayName("WizardProgressIndicator")
class WizardProgressIndicatorTest {

    private WizardProgressIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new WizardProgressIndicator(5, 1);
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("should create indicator with total steps")
        void shouldCreateIndicatorWithTotalSteps() {
            assertThat(indicator.getTotalSteps()).isEqualTo(5);
        }

        @Test
        @DisplayName("should create indicator with current step")
        void shouldCreateIndicatorWithCurrentStep() {
            assertThat(indicator.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should have accessible role")
        void shouldHaveAccessibleRole() {
            assertThat(indicator.getAccessibleRole())
                .isNotNull();
        }

        @Test
        @DisplayName("should be focus traversable")
        void shouldBeFocusTraversable() {
            assertThat(indicator.isFocusTraversable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Step State Tests")
    class StepStateTests {

        @Test
        @DisplayName("should mark current step as active")
        void shouldMarkCurrentStepAsActive() {
            assertThat(indicator.isStepActive(1)).isTrue();
        }

        @Test
        @DisplayName("should mark future steps as pending")
        void shouldMarkFutureStepsAsPending() {
            for (int i = 2; i <= 5; i++) {
                assertThat(indicator.isStepPending(i))
                    .as("Step %d should be pending", i)
                    .isTrue();
            }
        }

        @Test
        @DisplayName("should not mark any step as completed initially")
        void shouldNotMarkAnyStepAsCompletedInitially() {
            for (int i = 1; i <= 5; i++) {
                assertThat(indicator.isStepCompleted(i))
                    .as("Step %d should not be completed", i)
                    .isFalse();
            }
        }
    }

    @Nested
    @DisplayName("Step Update Tests")
    class StepUpdateTests {

        @Test
        @DisplayName("should update current step")
        void shouldUpdateCurrentStep() {
            // When
            indicator.setCurrentStep(3);

            // Then
            assertThat(indicator.getCurrentStep()).isEqualTo(3);
        }

        @Test
        @DisplayName("should mark previous steps as completed when advancing")
        void shouldMarkPreviousStepsAsCompletedWhenAdvancing() {
            // Given
            indicator.setCurrentStep(3);

            // Then
            assertThat(indicator.isStepCompleted(1)).isTrue();
            assertThat(indicator.isStepCompleted(2)).isTrue();
            assertThat(indicator.isStepActive(3)).isTrue();
        }

        @Test
        @DisplayName("should not exceed total steps")
        void shouldNotExceedTotalSteps() {
            // When
            indicator.setCurrentStep(10);

            // Then
            assertThat(indicator.getCurrentStep()).isLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("should not go below step 1")
        void shouldNotGoBelowStep1() {
            // When
            indicator.setCurrentStep(0);

            // Then
            assertThat(indicator.getCurrentStep()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Accessibility Tests")
    class AccessibilityTests {

        @Test
        @DisplayName("should provide accessible text")
        void shouldProvideAccessibleText() {
            assertThat(indicator.getAccessibleText())
                .contains("Step 1 of 5");
        }

        @Test
        @DisplayName("should update accessible text when step changes")
        void shouldUpdateAccessibleTextWhenStepChanges() {
            // When
            indicator.setCurrentStep(3);

            // Then
            assertThat(indicator.getAccessibleText())
                .contains("Step 3 of 5");
        }
    }

    @Nested
    @DisplayName("Style Class Tests")
    class StyleClassTests {

        @Test
        @DisplayName("should have base style class")
        void shouldHaveBaseStyleClass() {
            assertThat(indicator.getStyleClass())
                .contains("wizard-progress-indicator");
        }
    }

    @Nested
    @DisplayName("Connector Tests")
    class ConnectorTests {

        @Test
        @DisplayName("should have connectors between steps")
        void shouldHaveConnectorsBetweenSteps() {
            // 5 steps = 4 connectors
            assertThat(indicator.getConnectorCount()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle single step indicator")
        void shouldHandleSingleStepIndicator() {
            WizardProgressIndicator singleStep = new WizardProgressIndicator(1, 1);
            assertThat(singleStep.getTotalSteps()).isEqualTo(1);
            assertThat(singleStep.getCurrentStep()).isEqualTo(1);
            assertThat(singleStep.isStepActive(1)).isTrue();
        }

        @Test
        @DisplayName("should handle two step indicator")
        void shouldHandleTwoStepIndicator() {
            WizardProgressIndicator twoStep = new WizardProgressIndicator(2, 1);
            assertThat(twoStep.getConnectorCount()).isEqualTo(1);
        }
    }
}
