package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for SE-809: Employment Income Warning.
 *
 * <p>Tests the ViewModel that manages the visibility and state of the
 * employment income warning banner.</p>
 */
@DisplayName("SE-809: Employment Income Warning ViewModel")
class EmploymentIncomeWarningViewModelTest {

    private EmploymentIncomeWarningViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new EmploymentIncomeWarningViewModel();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should not show warning initially")
        void shouldNotShowWarningInitially() {
            assertThat(viewModel.showWarningProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should not be detected initially")
        void shouldNotBeDetectedInitially() {
            assertThat(viewModel.employmentIncomeDetectedProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should not be dismissed initially")
        void shouldNotBeDismissedInitially() {
            assertThat(viewModel.dismissedProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should be dismissable by default")
        void shouldBeDismissableByDefault() {
            assertThat(viewModel.dismissableProperty().get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Detection")
    class Detection {

        @Test
        @DisplayName("should show warning when employment income detected")
        void shouldShowWarningWhenEmploymentIncomeDetected() {
            viewModel.setEmploymentIncomeDetected(true, "BANK_IMPORT");

            assertThat(viewModel.employmentIncomeDetectedProperty().get()).isTrue();
            assertThat(viewModel.showWarningProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should store detection reason")
        void shouldStoreDetectionReason() {
            viewModel.setEmploymentIncomeDetected(true, "HMRC_DATA");

            assertThat(viewModel.detectionReasonProperty().get()).isEqualTo("HMRC_DATA");
        }

        @Test
        @DisplayName("should hide warning when detection cleared")
        void shouldHideWarningWhenDetectionCleared() {
            viewModel.setEmploymentIncomeDetected(true, "BANK_IMPORT");
            viewModel.setEmploymentIncomeDetected(false, null);

            assertThat(viewModel.showWarningProperty().get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Dismissal")
    class Dismissal {

        @Test
        @DisplayName("should hide warning when dismissed")
        void shouldHideWarningWhenDismissed() {
            viewModel.setEmploymentIncomeDetected(true, "BANK_IMPORT");

            viewModel.dismiss();

            assertThat(viewModel.dismissedProperty().get()).isTrue();
            assertThat(viewModel.showWarningProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should not show warning after dismiss even if still detected")
        void shouldNotShowWarningAfterDismiss() {
            viewModel.setEmploymentIncomeDetected(true, "BANK_IMPORT");
            viewModel.dismiss();

            // Detection is still true, but banner shouldn't show
            assertThat(viewModel.employmentIncomeDetectedProperty().get()).isTrue();
            assertThat(viewModel.showWarningProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should reset dismissal state on new session")
        void shouldResetDismissalOnNewSession() {
            viewModel.setEmploymentIncomeDetected(true, "BANK_IMPORT");
            viewModel.dismiss();

            viewModel.resetDismissal();

            assertThat(viewModel.dismissedProperty().get()).isFalse();
            assertThat(viewModel.showWarningProperty().get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Non-Dismissable Mode")
    class NonDismissableMode {

        @Test
        @DisplayName("should not allow dismissal when non-dismissable")
        void shouldNotAllowDismissalWhenNonDismissable() {
            viewModel.dismissableProperty().set(false);
            viewModel.setEmploymentIncomeDetected(true, "BANK_IMPORT");

            viewModel.dismiss();

            assertThat(viewModel.dismissedProperty().get()).isFalse();
            assertThat(viewModel.showWarningProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should set critical style when non-dismissable")
        void shouldSetCriticalStyleWhenNonDismissable() {
            viewModel.dismissableProperty().set(false);

            assertThat(viewModel.criticalModeProperty().get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Warning Text")
    class WarningText {

        @Test
        @DisplayName("should provide title text")
        void shouldProvideTitleText() {
            assertThat(viewModel.getTitleText())
                    .isEqualTo("Employment Income Detected");
        }

        @Test
        @DisplayName("should provide first paragraph text")
        void shouldProvideFirstParagraphText() {
            assertThat(viewModel.getFirstParagraphText())
                    .contains("SELF-EMPLOYMENT income only")
                    .contains("PAYE");
        }

        @Test
        @DisplayName("should provide second paragraph text")
        void shouldProvideSecondParagraphText() {
            assertThat(viewModel.getSecondParagraphText())
                    .contains("P60")
                    .contains("employment income");
        }

        @Test
        @DisplayName("should provide HMRC guidance URL")
        void shouldProvideHmrcGuidanceUrl() {
            assertThat(viewModel.getHmrcGuidanceUrl())
                    .isEqualTo("https://www.gov.uk/self-assessment-tax-returns/who-must-send-a-tax-return");
        }
    }

    @Nested
    @DisplayName("Detection Patterns")
    class DetectionPatterns {

        @Test
        @DisplayName("should detect salary in transaction description")
        void shouldDetectSalaryInTransactionDescription() {
            boolean detected = viewModel.checkTransactionDescription("SALARY FROM ACME LTD");

            assertThat(detected).isTrue();
        }

        @Test
        @DisplayName("should detect wages in transaction description")
        void shouldDetectWagesInTransactionDescription() {
            boolean detected = viewModel.checkTransactionDescription("WAGES PAYMENT");

            assertThat(detected).isTrue();
        }

        @Test
        @DisplayName("should detect PAYE in transaction description")
        void shouldDetectPayeInTransactionDescription() {
            boolean detected = viewModel.checkTransactionDescription("PAYE REFUND");

            assertThat(detected).isTrue();
        }

        @Test
        @DisplayName("should not detect normal business income")
        void shouldNotDetectNormalBusinessIncome() {
            boolean detected = viewModel.checkTransactionDescription("INVOICE PAYMENT FROM CLIENT");

            assertThat(detected).isFalse();
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            assertThat(viewModel.checkTransactionDescription("salary from employer")).isTrue();
            assertThat(viewModel.checkTransactionDescription("SALARY FROM EMPLOYER")).isTrue();
        }
    }
}
