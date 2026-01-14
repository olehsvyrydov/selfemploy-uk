package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for SE-512: SubmissionDeclarationViewModel.
 *
 * <p>Tests the ViewModel that provides JavaFX property bindings for the
 * 6-checkbox submission declaration UI component.</p>
 */
@DisplayName("SE-512: SubmissionDeclarationViewModel")
class SubmissionDeclarationViewModelTest {

    private Clock fixedClock;
    private Instant fixedInstant;
    private SubmissionDeclarationViewModel viewModel;

    @BeforeEach
    void setUp() {
        fixedInstant = Instant.parse("2026-01-12T14:30:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        viewModel = new SubmissionDeclarationViewModel(fixedClock, "2025-26");
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should have all checkboxes unchecked initially")
        void shouldHaveAllCheckboxesUncheckedInitially() {
            assertThat(viewModel.accuracyStatementProperty().get()).isFalse();
            assertThat(viewModel.penaltiesWarningProperty().get()).isFalse();
            assertThat(viewModel.recordKeepingProperty().get()).isFalse();
            assertThat(viewModel.calculationVerificationProperty().get()).isFalse();
            assertThat(viewModel.legalEffectProperty().get()).isFalse();
            assertThat(viewModel.identityConfirmationProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should not be complete initially")
        void shouldNotBeCompleteInitially() {
            assertThat(viewModel.isCompleteProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should have zero confirmed count initially")
        void shouldHaveZeroConfirmedCountInitially() {
            assertThat(viewModel.confirmedCountProperty().get()).isZero();
        }

        @Test
        @DisplayName("should show 0 of 6 progress initially")
        void shouldShowProgressInitially() {
            assertThat(viewModel.progressTextProperty().get()).isEqualTo("0 of 6 confirmations completed");
        }

        @Test
        @DisplayName("should not show timestamp section initially")
        void shouldNotShowTimestampSectionInitially() {
            assertThat(viewModel.showTimestampSectionProperty().get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Checkbox Properties")
    class CheckboxProperties {

        @Test
        @DisplayName("should update confirmed count when checkbox is checked")
        void shouldUpdateConfirmedCountWhenCheckboxChecked() {
            viewModel.accuracyStatementProperty().set(true);

            assertThat(viewModel.confirmedCountProperty().get()).isEqualTo(1);
            assertThat(viewModel.progressTextProperty().get()).isEqualTo("1 of 6 confirmations completed");
        }

        @Test
        @DisplayName("should track multiple checkboxes independently")
        void shouldTrackMultipleCheckboxesIndependently() {
            viewModel.accuracyStatementProperty().set(true);
            viewModel.penaltiesWarningProperty().set(true);
            viewModel.recordKeepingProperty().set(true);

            assertThat(viewModel.confirmedCountProperty().get()).isEqualTo(3);
            assertThat(viewModel.progressTextProperty().get()).isEqualTo("3 of 6 confirmations completed");
        }

        @Test
        @DisplayName("should decrease count when checkbox is unchecked")
        void shouldDecreaseCountWhenCheckboxUnchecked() {
            viewModel.accuracyStatementProperty().set(true);
            viewModel.penaltiesWarningProperty().set(true);
            viewModel.accuracyStatementProperty().set(false);

            assertThat(viewModel.confirmedCountProperty().get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should be complete when all 6 checkboxes are checked")
        void shouldBeCompleteWhenAllCheckboxesChecked() {
            checkAllBoxes();

            assertThat(viewModel.isCompleteProperty().get()).isTrue();
            assertThat(viewModel.confirmedCountProperty().get()).isEqualTo(6);
        }

        @Test
        @DisplayName("should not be complete with only 5 checkboxes checked")
        void shouldNotBeCompleteWithOnly5Checkboxes() {
            viewModel.accuracyStatementProperty().set(true);
            viewModel.penaltiesWarningProperty().set(true);
            viewModel.recordKeepingProperty().set(true);
            viewModel.calculationVerificationProperty().set(true);
            viewModel.legalEffectProperty().set(true);
            // Missing: identityConfirmation

            assertThat(viewModel.isCompleteProperty().get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Timestamp Section")
    class TimestampSection {

        @Test
        @DisplayName("should show timestamp section when all checkboxes checked")
        void shouldShowTimestampSectionWhenComplete() {
            checkAllBoxes();

            assertThat(viewModel.showTimestampSectionProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should hide timestamp section when unchecked")
        void shouldHideTimestampSectionWhenUnchecked() {
            checkAllBoxes();
            viewModel.accuracyStatementProperty().set(false);

            assertThat(viewModel.showTimestampSectionProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should provide formatted timestamp")
        void shouldProvideFormattedTimestamp() {
            checkAllBoxes();

            assertThat(viewModel.getTimestampDisplay()).contains("12 January 2026");
            assertThat(viewModel.getTimestampDisplay()).contains("14:30:00");
        }

        @Test
        @DisplayName("should return empty timestamp when not complete")
        void shouldReturnEmptyTimestampWhenNotComplete() {
            assertThat(viewModel.getTimestampDisplay()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Declaration ID")
    class DeclarationId {

        @Test
        @DisplayName("should generate declaration ID when complete")
        void shouldGenerateDeclarationIdWhenComplete() {
            checkAllBoxes();

            String declarationId = viewModel.getDeclarationIdDisplay();
            assertThat(declarationId).startsWith("DECL-20260112-143000-");
            assertThat(declarationId).hasSize(26); // DECL-YYYYMMDD-HHMMSS-XXXXX
        }

        @Test
        @DisplayName("should return empty ID when not complete")
        void shouldReturnEmptyIdWhenNotComplete() {
            assertThat(viewModel.getDeclarationIdDisplay()).isEmpty();
        }

        @Test
        @DisplayName("should preserve same declaration ID once generated")
        void shouldPreserveSameDeclarationIdOnceGenerated() {
            checkAllBoxes();
            String id1 = viewModel.getDeclarationIdDisplay();
            String id2 = viewModel.getDeclarationIdDisplay();

            assertThat(id1).isEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("Declaration Texts")
    class DeclarationTexts {

        @Test
        @DisplayName("should provide accuracy statement text")
        void shouldProvideAccuracyStatementText() {
            String text = viewModel.getAccuracyStatementText();

            assertThat(text).contains("correct and complete");
            assertThat(text).contains("best of my knowledge and belief");
        }

        @Test
        @DisplayName("should provide penalties warning text")
        void shouldProvidePenaltiesWarningText() {
            String text = viewModel.getPenaltiesWarningText();

            assertThat(text).contains("financial penalties");
            assertThat(text).contains("prosecution");
        }

        @Test
        @DisplayName("should provide record keeping text")
        void shouldProvideRecordKeepingText() {
            String text = viewModel.getRecordKeepingText();

            assertThat(text).contains("kept records");
            assertThat(text).contains("5 years");
        }

        @Test
        @DisplayName("should provide calculation verification text")
        void shouldProvideCalculationVerificationText() {
            String text = viewModel.getCalculationVerificationText();

            assertThat(text).contains("reviewed the tax calculation");
            assertThat(text).contains("accurate");
        }

        @Test
        @DisplayName("should provide legal effect text")
        void shouldProvideLegalEffectText() {
            String text = viewModel.getLegalEffectText();

            assertThat(text).contains("legal act");
            assertThat(text).contains("signing a paper return");
        }

        @Test
        @DisplayName("should provide identity confirmation text")
        void shouldProvideIdentityConfirmationText() {
            String text = viewModel.getIdentityConfirmationText();

            assertThat(text).contains("I am the person");
            assertThat(text).contains("authorised");
        }
    }

    @Nested
    @DisplayName("Build Declaration")
    class BuildDeclaration {

        @Test
        @DisplayName("should build declaration when complete")
        void shouldBuildDeclarationWhenComplete() {
            checkAllBoxes();

            var declaration = viewModel.buildDeclaration();

            assertThat(declaration).isNotNull();
            assertThat(declaration.isPresent()).isTrue();
            assertThat(declaration.get().items()).hasSize(6);
            assertThat(declaration.get().taxYear()).isEqualTo("2025-26");
        }

        @Test
        @DisplayName("should return empty when not complete")
        void shouldReturnEmptyWhenNotComplete() {
            viewModel.accuracyStatementProperty().set(true);

            var declaration = viewModel.buildDeclaration();

            assertThat(declaration).isEmpty();
        }
    }

    @Nested
    @DisplayName("Reset")
    class Reset {

        @Test
        @DisplayName("should reset all checkboxes")
        void shouldResetAllCheckboxes() {
            checkAllBoxes();

            viewModel.reset();

            assertThat(viewModel.accuracyStatementProperty().get()).isFalse();
            assertThat(viewModel.penaltiesWarningProperty().get()).isFalse();
            assertThat(viewModel.recordKeepingProperty().get()).isFalse();
            assertThat(viewModel.calculationVerificationProperty().get()).isFalse();
            assertThat(viewModel.legalEffectProperty().get()).isFalse();
            assertThat(viewModel.identityConfirmationProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should reset progress after reset")
        void shouldResetProgressAfterReset() {
            checkAllBoxes();

            viewModel.reset();

            assertThat(viewModel.confirmedCountProperty().get()).isZero();
            assertThat(viewModel.isCompleteProperty().get()).isFalse();
            assertThat(viewModel.showTimestampSectionProperty().get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Disabled State")
    class DisabledState {

        @Test
        @DisplayName("should allow disabling all checkboxes")
        void shouldAllowDisablingAllCheckboxes() {
            viewModel.disabledProperty().set(true);

            assertThat(viewModel.disabledProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should be enabled by default")
        void shouldBeEnabledByDefault() {
            assertThat(viewModel.disabledProperty().get()).isFalse();
        }
    }

    // Helper method
    private void checkAllBoxes() {
        viewModel.accuracyStatementProperty().set(true);
        viewModel.penaltiesWarningProperty().set(true);
        viewModel.recordKeepingProperty().set(true);
        viewModel.calculationVerificationProperty().set(true);
        viewModel.legalEffectProperty().set(true);
        viewModel.identityConfirmationProperty().set(true);
    }
}
