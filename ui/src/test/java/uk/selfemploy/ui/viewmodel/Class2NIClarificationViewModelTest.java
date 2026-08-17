package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.core.calculator.NationalInsuranceClass2Calculator;
import uk.selfemploy.core.config.RateBasis;
import uk.selfemploy.core.config.TaxRateConfiguration;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Class 2 NI clarification card.
 *
 * <p>Three scenarios, driven by the profit and the rates of the year passed in:</p>
 * <ol>
 *   <li>At or above the Small Profits Threshold — treated as paid, nothing charged</li>
 *   <li>Below the threshold but positive — voluntary payment option</li>
 *   <li>Zero or loss — nothing to pay, and a possible gap in the NI record</li>
 * </ol>
 *
 * <p>The rates shown are the ones the card must never get wrong: they belong to the year handed
 * to {@link Class2NIClarificationViewModel#update(int, BigDecimal)} and to no other.</p>
 */
@DisplayName("Class 2 NI Clarification ViewModel")
class Class2NIClarificationViewModelTest {

    private Class2NIClarificationViewModel viewModel;

    // Constants for testing (against 2025/26 rates unless a test says otherwise)
    private static final int TAX_YEAR_2025 = 2025;
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

        @Test
        @DisplayName("should show no rates at all before a tax year is supplied")
        void shouldShowNoRatesBeforeATaxYearIsSupplied() {
            assertThat(viewModel.weeklyRateLabelProperty().get()).isEmpty();
            assertThat(viewModel.weeklyRateTextProperty().get()).isEmpty();
            assertThat(viewModel.annualAmountTextProperty().get()).isEmpty();
            assertThat(viewModel.getWeeklyRate()).isNull();
            assertThat(viewModel.getSmallProfitsThreshold()).isNull();
        }
    }

    @Nested
    @DisplayName("Above Small Profits Threshold")
    class AboveSmallProfitsThreshold {

        @BeforeEach
        void setUp() {
            viewModel.update(TAX_YEAR_2025, ABOVE_SPT);
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
        @DisplayName("should say the contributions are treated as paid, not that they are owed")
        void shouldSayContributionsAreTreatedAsPaid() {
            assertThat(viewModel.bodyTextProperty().get())
                    .contains("above the Small Profits Threshold")
                    .contains("£6,845") // 2025/26 SPT from TaxRateConfiguration
                    .contains("treated as paid")
                    .contains("nothing is charged")
                    .doesNotContain("entitled to pay");
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
            viewModel.update(TAX_YEAR_2025, BELOW_SPT);
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
            viewModel.update(TAX_YEAR_2025, ZERO_PROFIT);
            assertThat(viewModel.visibleProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should be visible for loss")
        void shouldBeVisibleForLoss() {
            viewModel.update(TAX_YEAR_2025, LOSS);
            assertThat(viewModel.visibleProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should not show voluntary badge for zero profit")
        void shouldNotShowVoluntaryBadgeForZeroProfit() {
            viewModel.update(TAX_YEAR_2025, ZERO_PROFIT);
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isFalse();
        }

        @Test
        @DisplayName("should show standard title for zero profit")
        void shouldShowStandardTitleForZeroProfit() {
            viewModel.update(TAX_YEAR_2025, ZERO_PROFIT);
            assertThat(viewModel.titleTextProperty().get())
                    .isEqualTo("Class 2 NI Credits");
        }

        @Test
        @DisplayName("should show no profits message for zero")
        void shouldShowNoProfitsMessageForZero() {
            viewModel.update(TAX_YEAR_2025, ZERO_PROFIT);
            assertThat(viewModel.bodyTextProperty().get())
                    .contains("no profits this year")
                    .contains("don't need to pay")
                    .contains("gaps in your National Insurance record");
        }

        @Test
        @DisplayName("should show no profits message for loss")
        void shouldShowNoProfitsMessageForLoss() {
            viewModel.update(TAX_YEAR_2025, LOSS);
            assertThat(viewModel.bodyTextProperty().get())
                    .contains("no profits this year");
        }

        @Test
        @DisplayName("should show gap warning in pension insight for zero/loss")
        void shouldShowGapWarningInPensionInsight() {
            viewModel.update(TAX_YEAR_2025, ZERO_PROFIT);
            assertThat(viewModel.pensionInsightTextProperty().get())
                    .contains("qualifying year")
                    .contains("State Pension");
        }
    }

    @Nested
    @DisplayName("Rates belong to the year being viewed")
    class RatesBelongToTheYearBeingViewed {

        // Rates from the YAML configuration under core/src/main/resources/tax-rates/

        @Test
        @DisplayName("2025/26 shows the 2025/26 rates")
        void shouldShow2025Rates() {
            viewModel.update(2025, ABOVE_SPT);

            assertThat(viewModel.getWeeklyRate()).isEqualByComparingTo(new BigDecimal("3.50"));
            assertThat(viewModel.getAnnualAmount()).isEqualByComparingTo(new BigDecimal("182.00"));
            assertThat(viewModel.getSmallProfitsThreshold()).isEqualByComparingTo(new BigDecimal("6845"));
            assertThat(viewModel.weeklyRateLabelProperty().get()).contains("2025/26");
            assertThat(viewModel.weeklyRateTextProperty().get()).isEqualTo("£3.50");
            assertThat(viewModel.annualAmountTextProperty().get()).isEqualTo("£182.00");
        }

        @Test
        @DisplayName("2024/25 shows the 2024/25 rates, not 2025/26's")
        void shouldShow2024Rates() {
            viewModel.update(2024, ABOVE_SPT);

            assertThat(viewModel.getWeeklyRate()).isEqualByComparingTo(new BigDecimal("3.45"));
            assertThat(viewModel.getSmallProfitsThreshold()).isEqualByComparingTo(new BigDecimal("6725"));
            assertThat(viewModel.weeklyRateLabelProperty().get()).contains("2024/25");
            assertThat(viewModel.weeklyRateTextProperty().get()).isEqualTo("£3.45");
            assertThat(viewModel.annualAmountTextProperty().get()).isEqualTo("£179.40");
            assertThat(viewModel.bodyTextProperty().get()).contains("£6,725").contains("2024/25");
        }

        @Test
        @DisplayName("2026/27 shows the 2026/27 rates, not 2025/26's")
        void shouldShow2026Rates() {
            viewModel.update(2026, ABOVE_SPT);

            assertThat(viewModel.getWeeklyRate()).isEqualByComparingTo(new BigDecimal("3.65"));
            assertThat(viewModel.getSmallProfitsThreshold()).isEqualByComparingTo(new BigDecimal("7105"));
            assertThat(viewModel.weeklyRateLabelProperty().get()).contains("2026/27");
            assertThat(viewModel.weeklyRateTextProperty().get()).isEqualTo("£3.65");
            assertThat(viewModel.annualAmountTextProperty().get()).isEqualTo("£189.80");
        }

        @Test
        @DisplayName("switching year replaces every rate field, leaving none from the old year")
        void switchingYearReplacesEveryRateField() {
            viewModel.update(2026, ABOVE_SPT);
            viewModel.update(2024, ABOVE_SPT);

            assertThat(viewModel.weeklyRateLabelProperty().get()).contains("2024/25").doesNotContain("2026");
            assertThat(viewModel.weeklyRateTextProperty().get()).isEqualTo("£3.45");
            assertThat(viewModel.annualAmountTextProperty().get()).isEqualTo("£179.40");
            assertThat(viewModel.bodyTextProperty().get()).contains("£6,725").doesNotContain("£7,105");
        }

        @Test
        @DisplayName("clearing removes the card and every rate field with it")
        void clearingRemovesEveryRateField() {
            viewModel.update(2025, ABOVE_SPT);

            viewModel.clear();

            assertThat(viewModel.visibleProperty().get()).isFalse();
            assertThat(viewModel.weeklyRateLabelProperty().get()).isEmpty();
            assertThat(viewModel.weeklyRateTextProperty().get()).isEmpty();
            assertThat(viewModel.annualAmountTextProperty().get()).isEmpty();
            assertThat(viewModel.getWeeklyRate()).isNull();
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
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle exactly SPT threshold as above")
        void shouldHandleExactlyAtSPTAsAbove() {
            // 2025/26 SPT is £6,845
            viewModel.update(TAX_YEAR_2025, new BigDecimal("6845.00"));

            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isFalse();
            assertThat(viewModel.bodyTextProperty().get()).contains("treated as paid");
        }

        @Test
        @DisplayName("should handle just below SPT as voluntary")
        void shouldHandleJustBelowSPTAsVoluntary() {
            // 2025/26 SPT is £6,845
            viewModel.update(TAX_YEAR_2025, new BigDecimal("6844.99"));
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should handle one penny profit as below SPT")
        void shouldHandleOnePennyProfitAsBelowSPT() {
            viewModel.update(TAX_YEAR_2025, new BigDecimal("0.01"));
            assertThat(viewModel.showVoluntaryBadgeProperty().get()).isTrue();
        }

        @Test
        @DisplayName("should handle null profit as zero")
        void shouldHandleNullProfitAsZero() {
            viewModel.update(TAX_YEAR_2025, null);
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
            viewModel.update(TAX_YEAR_2025, ABOVE_SPT);
            assertThat(viewModel.scenarioProperty().get())
                    .isEqualTo(Class2NIClarificationViewModel.Scenario.ABOVE_SPT);
        }

        @Test
        @DisplayName("should set scenario to BELOW_SPT for low profits")
        void shouldSetScenarioToBelowSPT() {
            viewModel.update(TAX_YEAR_2025, BELOW_SPT);
            assertThat(viewModel.scenarioProperty().get())
                    .isEqualTo(Class2NIClarificationViewModel.Scenario.BELOW_SPT);
        }

        @Test
        @DisplayName("should set scenario to ZERO_LOSS for no profits")
        void shouldSetScenarioToZeroLoss() {
            viewModel.update(TAX_YEAR_2025, ZERO_PROFIT);
            assertThat(viewModel.scenarioProperty().get())
                    .isEqualTo(Class2NIClarificationViewModel.Scenario.ZERO_LOSS);
        }
    }

    @Nested
    @DisplayName("At exactly the Small Profits Threshold")
    class AtExactlyTheThreshold {

        /**
         * The card and the calculator each decide, separately, whether the user still needs to pay
         * voluntarily to earn a qualifying year. They read the same threshold but compared against
         * it differently — the card inclusively, the calculator exclusively — so at exactly the
         * threshold the screen said the year was already covered while the calculator said it was
         * voluntary. Both are asserted together so the pair cannot drift apart again at the one
         * profit where they disagreed.
         */
        @Test
        @DisplayName("the card and the calculator agree the year is already covered")
        void cardAndCalculatorAgreeAtTheThreshold() {
            NationalInsuranceClass2Calculator calculator =
                    new NationalInsuranceClass2Calculator(TAX_YEAR_2025);
            BigDecimal exactlyThreshold = calculator.getRates().smallProfitsThreshold();

            viewModel.update(TAX_YEAR_2025, exactlyThreshold);

            assertThat(calculator.calculate(exactlyThreshold).isTreatedAsPaid())
                    .as("HMRC treats profits at or above the threshold as having paid Class 2, so "
                        + "a profit of exactly %s earns the qualifying year", exactlyThreshold)
                    .isTrue();

            assertThat(viewModel.showVoluntaryBadgeProperty().get())
                    .as("the card must not offer a voluntary payment for a year the calculator "
                        + "already treats as paid")
                    .isFalse();

            assertThat(viewModel.scenarioProperty().get())
                    .isEqualTo(Class2NIClarificationViewModel.Scenario.ABOVE_SPT);
        }

        @Test
        @DisplayName("a penny below the threshold is still the voluntary case")
        void justBelowTheThresholdRemainsVoluntary() {
            NationalInsuranceClass2Calculator calculator =
                    new NationalInsuranceClass2Calculator(TAX_YEAR_2025);
            BigDecimal justBelow = calculator.getRates().smallProfitsThreshold()
                    .subtract(new BigDecimal("0.01"));

            viewModel.update(TAX_YEAR_2025, justBelow);

            assertThat(calculator.calculate(justBelow).isTreatedAsPaid())
                    .as("below the threshold nothing is treated as paid")
                    .isFalse();

            assertThat(viewModel.showVoluntaryBadgeProperty().get())
                    .as("below the threshold the voluntary option is the whole point of the card")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Past the last published rate file")
    class PastTheLastRateFile {

        /**
         * Rate lookups for an unconfigured year fall back to the latest published year rather than
         * to a hardcoded default. That keeps the figure sane, but it means the weekly rate on the
         * card belongs to an earlier year than the one being viewed — so the label has to name the
         * year the rate was published for. Naming the viewed year would state that an old rate is
         * that year's rate, which is exactly what the estimated-rates banner exists to deny.
         */
        @Test
        @DisplayName("the rate label names the year the rate was published for, not the year viewed")
        void rateLabelNamesThePublishedYear() {
            int lastPublished = TaxRateConfiguration.getInstance().getSupportedTaxYears().stream()
                    .max(Integer::compareTo)
                    .orElseThrow();
            int unconfigured = lastPublished + 1;

            RateBasis basis = TaxRateConfiguration.getInstance().rateBasisFor(unconfigured);
            assertThat(basis.isEstimated())
                    .as("the year after the last rate file must resolve to an earlier year's rates, "
                        + "otherwise this test is not exercising the fallback at all")
                    .isTrue();

            viewModel.update(unconfigured, new BigDecimal("30000.00"));

            assertThat(viewModel.weeklyRateLabelProperty().get())
                    .as("the rate shown is %s's, so the label must say so", basis.ratesYearLabel())
                    .contains(basis.ratesYearLabel())
                    .doesNotContain(basis.taxYearLabel());
        }

        @Test
        @DisplayName("for a published year the label still names that same year")
        void rateLabelNamesTheViewedYearWhenItIsPublished() {
            viewModel.update(TAX_YEAR_2025, new BigDecimal("30000.00"));

            assertThat(viewModel.weeklyRateLabelProperty().get())
                    .as("a configured year's rates are its own, so nothing changes for the normal case")
                    .contains("2025/26");
        }
    }
}
