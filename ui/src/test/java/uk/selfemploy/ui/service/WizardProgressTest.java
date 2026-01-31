package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the WizardProgress record.
 */
@DisplayName("WizardProgress")
class WizardProgressTest {

    private static final Instant NOW = Instant.parse("2026-01-29T10:30:00Z");

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("should throw when wizardType is null")
        void shouldThrowWhenWizardTypeNull() {
            assertThatThrownBy(() -> new WizardProgress(null, 0, null, null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wizard type cannot be null or blank");
        }

        @Test
        @DisplayName("should throw when wizardType is blank")
        void shouldThrowWhenWizardTypeBlank() {
            assertThatThrownBy(() -> new WizardProgress("  ", 0, null, null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wizard type cannot be null or blank");
        }

        @Test
        @DisplayName("should throw when currentStep is negative")
        void shouldThrowWhenCurrentStepNegative() {
            assertThatThrownBy(() -> new WizardProgress("test", -1, null, null, NOW, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current step cannot be negative");
        }

        @Test
        @DisplayName("should throw when createdAt is null")
        void shouldThrowWhenCreatedAtNull() {
            assertThatThrownBy(() -> new WizardProgress("test", 0, null, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Created timestamp cannot be null");
        }

        @Test
        @DisplayName("should throw when updatedAt is null")
        void shouldThrowWhenUpdatedAtNull() {
            assertThatThrownBy(() -> new WizardProgress("test", 0, null, null, NOW, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Updated timestamp cannot be null");
        }

        @Test
        @DisplayName("should accept valid inputs")
        void shouldAcceptValidInputs() {
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                3,
                "{\"key\":\"value\"}",
                "QQ123456C",
                NOW,
                NOW
            );

            assertThat(progress.wizardType()).isEqualTo(WizardProgress.HMRC_CONNECTION);
            assertThat(progress.currentStep()).isEqualTo(3);
            assertThat(progress.checklistState()).isEqualTo("{\"key\":\"value\"}");
            assertThat(progress.ninoEntered()).isEqualTo("QQ123456C");
            assertThat(progress.createdAt()).isEqualTo(NOW);
            assertThat(progress.updatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("should accept step 0")
        void shouldAcceptStepZero() {
            WizardProgress progress = new WizardProgress("test", 0, null, null, NOW, NOW);
            assertThat(progress.currentStep()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("start() should create progress at step 0")
        void startShouldCreateProgressAtStep0() {
            WizardProgress progress = WizardProgress.start(WizardProgress.HMRC_CONNECTION, NOW);

            assertThat(progress.wizardType()).isEqualTo(WizardProgress.HMRC_CONNECTION);
            assertThat(progress.currentStep()).isEqualTo(0);
            assertThat(progress.checklistState()).isNull();
            assertThat(progress.ninoEntered()).isNull();
            assertThat(progress.createdAt()).isEqualTo(NOW);
            assertThat(progress.updatedAt()).isEqualTo(NOW);
        }
    }

    @Nested
    @DisplayName("Builder Methods")
    class BuilderMethodTests {

        @Test
        @DisplayName("withStep() should create new progress with updated step")
        void withStepShouldUpdateStep() {
            WizardProgress original = WizardProgress.start(WizardProgress.HMRC_CONNECTION, NOW);
            Instant later = NOW.plusSeconds(60);

            WizardProgress updated = original.withStep(2, later);

            assertThat(updated.currentStep()).isEqualTo(2);
            assertThat(updated.wizardType()).isEqualTo(original.wizardType());
            assertThat(updated.checklistState()).isEqualTo(original.checklistState());
            assertThat(updated.ninoEntered()).isEqualTo(original.ninoEntered());
            assertThat(updated.createdAt()).isEqualTo(original.createdAt());
            assertThat(updated.updatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("withChecklistState() should create new progress with updated state")
        void withChecklistStateShouldUpdateState() {
            WizardProgress original = WizardProgress.start(WizardProgress.HMRC_CONNECTION, NOW);
            Instant later = NOW.plusSeconds(60);
            String newState = "{\"step1\":true}";

            WizardProgress updated = original.withChecklistState(newState, later);

            assertThat(updated.checklistState()).isEqualTo(newState);
            assertThat(updated.currentStep()).isEqualTo(original.currentStep());
            assertThat(updated.ninoEntered()).isEqualTo(original.ninoEntered());
            assertThat(updated.createdAt()).isEqualTo(original.createdAt());
            assertThat(updated.updatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("withNino() should create new progress with NINO set")
        void withNinoShouldSetNino() {
            WizardProgress original = WizardProgress.start(WizardProgress.HMRC_CONNECTION, NOW);
            Instant later = NOW.plusSeconds(60);
            String nino = "QQ123456C";

            WizardProgress updated = original.withNino(nino, later);

            assertThat(updated.ninoEntered()).isEqualTo(nino);
            assertThat(updated.currentStep()).isEqualTo(original.currentStep());
            assertThat(updated.checklistState()).isEqualTo(original.checklistState());
            assertThat(updated.createdAt()).isEqualTo(original.createdAt());
            assertThat(updated.updatedAt()).isEqualTo(later);
        }
    }

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("HMRC_CONNECTION constant should be defined")
        void hmrcConnectionConstantShouldBeDefined() {
            assertThat(WizardProgress.HMRC_CONNECTION).isEqualTo("hmrc_connection");
        }
    }
}
