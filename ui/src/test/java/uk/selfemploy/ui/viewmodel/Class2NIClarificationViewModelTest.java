package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for SE-810: Class 2 NI Credit Clarification UI.
 *
 * <p>Tests the ViewModel that provides educational content about Class 2
 * National Insurance and its importance for State Pension credits.</p>
 *
 * <p>Three scenarios (2025/26 rates from TaxRateConfiguration):</p>
 * <ol>
 *   <li>Above Small Profits Threshold (£6,845) - must pay Class 2</li>
 *   <li>Below SPT but positive - voluntary payment option</li>
 *   <li>Zero/loss - no payment required</li>
 * </ol>
 */
@DisplayName("SE-810: Class 2 NI Clarification ViewModel")
class Class2NIClarificationViewModelTest {

    private Class2NIClarificationViewModel viewModel;

    // Constants for testing
    private static final BigDecimal ABOVE_SPT = new BigDecimal("10000.00");
    private static final BigDecimal BELOW_SPT = new BigDecimal("5000.00");
    private static final BigDecimal ZERO_PROFIT = BigDecimal.ZERO;
    private static final BigDecimal LOSS = new BigDecimal("-1000.00");

    @BeforeEach
    void setUp() {
        viewModel = new Class2NIClarificationViewModel();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should not be visible initially")
        void shouldNotBeVisibleInitially() {
            assertThat(viewModel.visibleProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should not show voluntary badge initially")
        void shouldNotShowVoluntaryBadgeInitially() {
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should have default title")
        void shouldHaveDefaultTitle() {
            assertThat(viewModel.titleTextProperty().get())
                    .isEqualTo("Class 2 NI Credits");
        }
    }

    @Nested
    @DisplayName("Above Small Profits Threshold")
    class AboveSmallProfitsThreshold {

        @BeforeEach
        void setUp() {
            viewModel.updateForProfit(ABOVE_SPT);
        }

        @Test
        @DisplayName("should be visible when profit above SPT")
        void shouldBeVisibleWhenProfitAboveSPT() {
            assertThat(viewModel.visibleProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should not show voluntary badge")
        void shouldNotShowVoluntaryBadge() {
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should show standard title")
        void shouldShowStandardTitle() {
            assertThat(viewModel.titleTextProperty().get())
                    .isEqualTo("Class 2 NI Credits");
        }

        @Test
        @DisplayName("should show entitled message in body")
        void shouldShowEntitledMessageInBody() {
            assertThat(viewModel.bodyTextProperty().get())
                    .contains("Class 2 National Insurance helps you qualify for State Pension")
                    .contains("profits exceed £6,845") // 2025/26 SPT from TaxRateConfiguration
                    .contains("entitled to pay");
        }

        @Test
        @DisplayName("should show pension insight")
        void shouldShowPensionInsight() {
            assertThat(viewModel.pensionInsightTextProperty().get())
                    .contains("qualifying year")
                    .contains("State Pension")
                    .contains("35 qualifying years");
        }
    }

    @Nested
    @DisplayName("Below Small Profits Threshold (Voluntary)")
    class BelowSmallProfitsThreshold {

        @BeforeEach
        void setUp() {
            viewModel.updateForProfit(BELOW_SPT);
        }

        @Test
        @DisplayName("should be visible when profit below SPT")
        void shouldBeVisibleWhenProfitBelowSPT() {
            assertThat(viewModel.visibleProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should show voluntary badge")
        void shouldShowVoluntaryBadge() {
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should show voluntary title")
        void shouldShowVoluntaryTitle() {
            assertThat(viewModel.titleTextProperty().get())
                    .isEqualTo("Class 2 NI Credits (Voluntary)");
        }

        @Test
        @DisplayName("should show voluntary message in body")
        void shouldShowVoluntaryMessageInBody() {
            assertThat(viewModel.bodyTextProperty().get())
                    .contains("below the Small Profits Threshold")
                    .contains("Class 2 NI is voluntary")
                    .contains("protect your State Pension entitlement");
        }

        @Test
        @DisplayName("should show enhanced pension insight for voluntary")
        void shouldShowEnhancedPensionInsightForVoluntary() {
            assertThat(viewModel.pensionInsightTextProperty().get())
                    .contains("Paying voluntarily")
                    .contains("35 qualifying years");
        }
    }

    @Nested
    @DisplayName("Zero or Loss Profit")
    class ZeroOrLossProfit {

        @Test
        @DisplayName("should be visible for zero profit")
        void shouldBeVisibleForZeroProfit() {
            viewModel.updateForProfit(ZERO_PROFIT);
            assertThat(viewModel.visibleProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should be visible for loss")
        void shouldBeVisibleForLoss() {
            viewModel.updateForProfit(LOSS);
            assertThat(viewModel.visibleProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should not show voluntary badge for zero profit")
        void shouldNotShowVoluntaryBadgeForZeroProfit() {
            viewModel.updateForProfit(ZERO_PROFIT);
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should show standard title for zero profit")
        void shouldShowStandardTitleForZeroProfit() {
            viewModel.updateForProfit(ZERO_PROFIT);
            assertThat(viewModel.titleTextProperty().get())
                    .isEqualTo("Class 2 NI Credits");
        }

        @Test
        @DisplayName("should show no profits message for zero")
        void shouldShowNoProfitsMessageForZero() {
            viewModel.updateForProfit(ZERO_PROFIT);
            assertThat(viewModel.bodyTextProperty().get())
                    .contains("no profits this year")
                    .contains("don't need to pay")
                    .contains("gaps in your National Insurance record");
        }

        @Test
        @DisplayName("should show no profits message for loss")
        void shouldShowNoProfitsMessageForLoss() {
            viewModel.updateForProfit(LOSS);
            assertThat(viewModel.bodyTextProperty().get())
                    .contains("no profits this year");
        }

        @Test
        @DisplayName("should show gap warning in pension insight for zero/loss")
        void shouldShowGapWarningInPensionInsight() {
            viewModel.updateForProfit(ZERO_PROFIT);
            assertThat(viewModel.pensionInsightTextProperty().get())
                    .contains("qualifying year")
                    .contains("State Pension");
        }
    }

    @Nested
    @DisplayName("Rate Information")
    class RateInformation {

        // 2025/26 rates from YAML configuration
        // Source: core/src/main/resources/tax-rates/2025-26.yaml

        @Test
        @DisplayName("should provide weekly rate from TaxRateConfiguration")
        void shouldProvideWeeklyRate() {
            // 2025/26: £3.50 per week
            assertThat(viewModel.getWeeklyRate().compareTo(new BigDecimal("3.50")))
                    .isEqualTo(0);
        }

        @Test
        @DisplayName("should provide annual amount calculated from weekly rate")
        void shouldProvideAnnualAmount() {
            // 2025/26: £3.50 * 52 weeks = £182.00
            assertThat(viewModel.getAnnualAmount())
                    .isEqualTo(new BigDecimal("182.00"));
        }

        @Test
        @DisplayName("should provide formatted weekly rate")
        void shouldProvideFormattedWeeklyRate() {
            assertThat(viewModel.getFormattedWeeklyRate())
                    .isEqualTo("£3.50");
        }

        @Test
        @DisplayName("should provide formatted annual amount")
        void shouldProvideFormattedAnnualAmount() {
            assertThat(viewModel.getFormattedAnnualAmount())
                    .isEqualTo("£182.00");
        }

        @Test
        @DisplayName("should provide small profits threshold from TaxRateConfiguration")
        void shouldProvideSmallProfitsThreshold() {
            // 2025/26: £6,845 threshold
            assertThat(viewModel.getSmallProfitsThreshold())
                    .isEqualTo(new BigDecimal("6845"));
        }
    }

    @Nested
    @DisplayName("External Links")
    class ExternalLinks {

        @Test
        @DisplayName("should provide state pension forecast URL")
        void shouldProvideStatePensionForecastUrl() {
            assertThat(viewModel.getStatePensionForecastUrl())
                    .isEqualTo("https://www.gov.uk/check-state-pension");
        }

        @Test
        @DisplayName("should provide NI record URL")
        void shouldProvideNiRecordUrl() {
            assertThat(viewModel.getNiRecordUrl())
                    .isEqualTo("https://www.gov.uk/check-national-insurance-record");
        }

        @Test
        @DisplayName("should provide voluntary NI guidance URL")
        void shouldProvideVoluntaryNiGuidanceUrl() {
            assertThat(viewModel.getVoluntaryNiGuidanceUrl())
                    .isEqualTo("https://www.gov.uk/voluntary-national-insurance-contributions");
        }
    }

    @Nested
    @DisplayName("Tax Year Information")
    class TaxYearInformation {

        @Test
        @DisplayName("should provide current tax year")
        void shouldProvideCurrentTaxYear() {
            assertThat(viewModel.getTaxYear())
                    .isEqualTo("2025/26");
        }

        @Test
        @DisplayName("should include tax year in rate label")
        void shouldIncludeTaxYearInRateLabel() {
            assertThat(viewModel.getWeeklyRateLabel())
                    .contains("2025/26");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle exactly SPT threshold as above")
        void shouldHandleExactlyAtSPTAsAbove() {
            // 2025/26 SPT is £6,845
            viewModel.updateForProfit(new BigDecimal("6845.00"));
            // At exactly SPT, treat as above (entitled)
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isFalse();
            assertThat(viewModel.bodyTextProperty().get())
                    .contains("entitled to pay");
        }

        @Test
        @DisplayName("should handle just below SPT as voluntary")
        void shouldHandleJustBelowSPTAsVoluntary() {
            // 2025/26 SPT is £6,845
            viewModel.updateForProfit(new BigDecimal("6844.99"));
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should handle one penny profit as below SPT")
        void shouldHandleOnePennyProfitAsBelowSPT() {
            viewModel.updateForProfit(new BigDecimal("0.01"));
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should handle null profit as zero")
        void shouldHandleNullProfitAsZero() {
            viewModel.updateForProfit(null);
            assertThat(viewModel.visibleProperty().get()).isTrue();
            assertThat(viewModel.bodyTextProperty().get())
                    .contains("no profits this year");
        }
    }

    @Nested
    @DisplayName("Scenario Property")
    class ScenarioProperty {

        @Test
        @DisplayName("should set scenario to ABOVE_SPT for high profits")
        void shouldSetScenarioToAboveSPT() {
            viewModel.updateForProfit(ABOVE_SPT);
            assertThat(viewModel.scenarioProperty().get())
                    .isEqualTo(Class2NIClarificationViewModel.Scenario.ABOVE_SPT);
        }

        @Test
        @DisplayName("should set scenario to BELOW_SPT for low profits")
        void shouldSetScenarioToBelowSPT() {
            viewModel.updateForProfit(BELOW_SPT);
            assertThat(viewModel.scenarioProperty().get())
                    .isEqualTo(Class2NIClarificationViewModel.Scenario.BELOW_SPT);
        }

        @Test
        @DisplayName("should set scenario to ZERO_LOSS for no profits")
        void shouldSetScenarioToZeroLoss() {
            viewModel.updateForProfit(ZERO_PROFIT);
            assertThat(viewModel.scenarioProperty().get())
                    .isEqualTo(Class2NIClarificationViewModel.Scenario.ZERO_LOSS);
        }
    }
}
