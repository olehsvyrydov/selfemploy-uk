package uk.selfemploy.core.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import uk.selfemploy.core.config.NIClass4Rates;
import uk.selfemploy.core.config.TaxRateConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boundary and parametric tests for National Insurance Class 4 for tax year 2026/27.
 *
 * <p>These tests pin the behaviour of the rate-configuration layer and the
 * calculator at the boundaries that matter for HMRC compliance: the Lower
 * Profits Limit, the Upper Profits Limit, the first pound either side of each,
 * and a mid-band sample. Any change to the published 2026/27 rates that breaks
 * the math will break a named assertion here.</p>
 *
 * <h3>Authority</h3>
 * <ul>
 *   <li><a href="https://www.gov.uk/self-employed-national-insurance-rates">HMRC: Self-employed National Insurance rates</a></li>
 *   <li><a href="https://www.gov.uk/income-tax-rates">HMRC: Income Tax rates and Personal Allowances</a></li>
 * </ul>
 *
 * <p>Tax year 2026/27 rates (verified against HMRC 2026-05-24):</p>
 * <ul>
 *   <li>Lower Profits Limit: £12,570 (frozen)</li>
 *   <li>Upper Profits Limit: £50,270 (frozen)</li>
 *   <li>Main rate: 6% (held since April 2024)</li>
 *   <li>Additional rate: 2%</li>
 * </ul>
 */
@DisplayName("National Insurance Class 4 Calculator Tests (2026/27)")
class NationalInsuranceCalculator2026_27Test {

    private static final int TAX_YEAR_2026 = 2026;
    private static final int TAX_YEAR_2025 = 2025;

    private static final BigDecimal LPL_2026 = new BigDecimal("12570");
    private static final BigDecimal UPL_2026 = new BigDecimal("50270");
    private static final BigDecimal MAIN_RATE_2026 = new BigDecimal("0.06");
    private static final BigDecimal ADDITIONAL_RATE_2026 = new BigDecimal("0.02");

    private NationalInsuranceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new NationalInsuranceCalculator(TAX_YEAR_2026);
    }

    // ---------------------------------------------------------------------
    // Boundary tests (Acceptance Criteria 1 and 2)
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Lower Profits Limit boundary")
    class LowerProfitsLimitBoundary {

        @Test
        @DisplayName("profit at LPL minus £1 yields zero NI")
        void profitJustBelowLplYieldsZeroNi() {
            BigDecimal profit = LPL_2026.subtract(BigDecimal.ONE);

            NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.mainRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.additionalRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("profit exactly at LPL yields zero NI")
        void profitAtLplYieldsZeroNi() {
            NICalculationResult result = calculator.calculate(LPL_2026);

            assertThat(result.totalNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.mainRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.additionalRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("profit at LPL plus £1 yields exactly the main rate on £1")
        void profitJustAboveLplYieldsMainRateOnOnePound() {
            BigDecimal profit = LPL_2026.add(BigDecimal.ONE);
            BigDecimal expected = MAIN_RATE_2026.setScale(2, RoundingMode.HALF_UP); // £0.06

            NICalculationResult result = calculator.calculate(profit);

            assertThat(result.totalNI()).isEqualByComparingTo(expected);
            assertThat(result.mainRateNI()).isEqualByComparingTo(expected);
            assertThat(result.additionalRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Upper Profits Limit boundary")
    class UpperProfitsLimitBoundary {

        @Test
        @DisplayName("profit exactly at UPL uses the full main-rate band only")
        void profitAtUplUsesFullMainRateBandOnly() {
            BigDecimal mainBand = UPL_2026.subtract(LPL_2026);
            BigDecimal expectedMain = mainBand.multiply(MAIN_RATE_2026).setScale(2, RoundingMode.HALF_UP);

            NICalculationResult result = calculator.calculate(UPL_2026);

            assertThat(result.mainRateNI()).isEqualByComparingTo(expectedMain);
            assertThat(result.additionalRateNI()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalNI()).isEqualByComparingTo(expectedMain);
        }

        @Test
        @DisplayName("profit at UPL plus £1 has both main-rate and additional-rate components non-zero")
        void profitJustAboveUplHasBothComponentsNonZero() {
            BigDecimal profit = UPL_2026.add(BigDecimal.ONE);

            NICalculationResult result = calculator.calculate(profit);

            // Acceptance Criterion 2: both components must be non-zero at UPL + £1
            assertThat(result.mainRateNI()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.additionalRateNI()).isGreaterThan(BigDecimal.ZERO);

            BigDecimal expectedMain = UPL_2026.subtract(LPL_2026)
                .multiply(MAIN_RATE_2026).setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedAdditional = ADDITIONAL_RATE_2026.setScale(2, RoundingMode.HALF_UP);

            assertThat(result.mainRateNI()).isEqualByComparingTo(expectedMain);
            assertThat(result.additionalRateNI()).isEqualByComparingTo(expectedAdditional);
        }
    }

    // ---------------------------------------------------------------------
    // Parametric coverage of every meaningful profit point
    // ---------------------------------------------------------------------

    /**
     * Provides (profit, expectedMainRateNI, expectedAdditionalRateNI) tuples
     * covering boundaries, mid-band, full main-rate band, and additional-rate band.
     * Expected values are computed from the published 2026/27 rates so any drift
     * between configuration and calculator surfaces as a named test failure.
     */
    static Stream<Arguments> profitScenarios() {
        BigDecimal lpl = new BigDecimal("12570");
        BigDecimal upl = new BigDecimal("50270");
        BigDecimal mainRate = new BigDecimal("0.06");
        BigDecimal additionalRate = new BigDecimal("0.02");
        BigDecimal fullMainBand = upl.subtract(lpl)
            .multiply(mainRate).setScale(2, RoundingMode.HALF_UP); // £2,262.00

        return Stream.of(
            // profit, expected main-rate NI, expected additional-rate NI, label
            Arguments.of(new BigDecimal("0"), bd("0.00"), bd("0.00"), "zero profit"),
            Arguments.of(lpl.subtract(BigDecimal.ONE), bd("0.00"), bd("0.00"), "LPL minus £1"),
            Arguments.of(lpl, bd("0.00"), bd("0.00"), "LPL exactly"),
            Arguments.of(lpl.add(BigDecimal.ONE), bd("0.06"), bd("0.00"), "LPL plus £1"),
            Arguments.of(new BigDecimal("30000"),
                new BigDecimal("30000").subtract(lpl).multiply(mainRate).setScale(2, RoundingMode.HALF_UP),
                bd("0.00"),
                "mid main-rate band (£30,000)"),
            Arguments.of(upl.subtract(BigDecimal.ONE),
                upl.subtract(BigDecimal.ONE).subtract(lpl).multiply(mainRate).setScale(2, RoundingMode.HALF_UP),
                bd("0.00"),
                "UPL minus £1"),
            Arguments.of(upl, fullMainBand, bd("0.00"), "UPL exactly"),
            Arguments.of(upl.add(BigDecimal.ONE),
                fullMainBand,
                additionalRate.setScale(2, RoundingMode.HALF_UP),
                "UPL plus £1"),
            Arguments.of(upl.add(new BigDecimal("10000")),
                fullMainBand,
                new BigDecimal("10000").multiply(additionalRate).setScale(2, RoundingMode.HALF_UP),
                "UPL plus £10,000")
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @ParameterizedTest(name = "[{index}] {3}: profit={0}")
    @MethodSource("profitScenarios")
    @DisplayName("calculator matches expected NI across the profit spectrum")
    void calculatorMatchesExpectedNiAcrossProfitSpectrum(
            BigDecimal profit,
            BigDecimal expectedMainRateNI,
            BigDecimal expectedAdditionalRateNI,
            String label) {

        NICalculationResult result = calculator.calculate(profit);

        BigDecimal expectedTotal = expectedMainRateNI.add(expectedAdditionalRateNI);

        assertThat(result.mainRateNI())
            .as("main-rate component for %s", label)
            .isEqualByComparingTo(expectedMainRateNI);
        assertThat(result.additionalRateNI())
            .as("additional-rate component for %s", label)
            .isEqualByComparingTo(expectedAdditionalRateNI);
        assertThat(result.totalNI())
            .as("total NI for %s", label)
            .isEqualByComparingTo(expectedTotal);
    }

    // ---------------------------------------------------------------------
    // Tax-year switch verification (Acceptance Criterion 3)
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("Tax year switch from 2025/26 to 2026/27")
    class TaxYearSwitch {

        @Test
        @DisplayName("rate configuration exposes distinct rate records per tax year")
        void rateConfigurationExposesDistinctRecordsPerTaxYear() {
            NIClass4Rates rates2025 =
                TaxRateConfiguration.getInstance().getNIClass4Rates(TAX_YEAR_2025);
            NIClass4Rates rates2026 =
                TaxRateConfiguration.getInstance().getNIClass4Rates(TAX_YEAR_2026);

            // Distinct record instances per year (independent cache entries)
            assertThat(rates2025).isNotSameAs(rates2026);

            // Both years are fully populated (no null/zero defaults).
            assertThat(rates2025.lowerProfitsLimit()).isGreaterThan(BigDecimal.ZERO);
            assertThat(rates2025.upperProfitsLimit()).isGreaterThan(BigDecimal.ZERO);
            assertThat(rates2026.lowerProfitsLimit()).isGreaterThan(BigDecimal.ZERO);
            assertThat(rates2026.upperProfitsLimit()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("2026/27 NI Class 4 values match the published HMRC figures")
        void rates2026MatchPublishedHmrcFigures() {
            NIClass4Rates rates =
                TaxRateConfiguration.getInstance().getNIClass4Rates(TAX_YEAR_2026);

            assertThat(rates.lowerProfitsLimit()).isEqualByComparingTo(LPL_2026);
            assertThat(rates.upperProfitsLimit()).isEqualByComparingTo(UPL_2026);
            assertThat(rates.mainRate()).isEqualByComparingTo(MAIN_RATE_2026);
            assertThat(rates.additionalRate()).isEqualByComparingTo(ADDITIONAL_RATE_2026);
        }

        @Test
        @DisplayName("switching active tax year is reflected in calculator's loaded rates")
        void switchingActiveTaxYearChangesLoadedRates() {
            NationalInsuranceCalculator calc2025 = new NationalInsuranceCalculator(TAX_YEAR_2025);
            NationalInsuranceCalculator calc2026 = new NationalInsuranceCalculator(TAX_YEAR_2026);

            assertThat(calc2025.getTaxYear()).isEqualTo(TAX_YEAR_2025);
            assertThat(calc2026.getTaxYear()).isEqualTo(TAX_YEAR_2026);

            // The rate record observable from the calculator differs by year, even if
            // for 2025/26 vs 2026/27 the published numbers happen to coincide
            // (frozen thresholds, held rates). Asserting record identity here proves
            // that the calculator is reading from the per-year configuration cache
            // rather than a shared singleton.
            assertThat(calc2025.getRates()).isNotSameAs(calc2026.getRates());
        }

        @Test
        @DisplayName("Class 2 weekly rate uprates from 2025/26 (£3.50) to 2026/27 (£3.65) — value-diff proof of year switch")
        void class2WeeklyRateUpratesYearOnYear() {
            // Class 4 thresholds and rates are frozen between 2025/26 and 2026/27,
            // so the only published numerical delta between the two YAML files is the
            // Class 2 voluntary weekly contribution rate. Pinning both ends of the
            // delta is the strongest value-based evidence that the per-year loader
            // genuinely reads the right file for each year.
            uk.selfemploy.core.config.NIClass2Rates class2_2025 =
                TaxRateConfiguration.getInstance().getNIClass2Rates(TAX_YEAR_2025);
            uk.selfemploy.core.config.NIClass2Rates class2_2026 =
                TaxRateConfiguration.getInstance().getNIClass2Rates(TAX_YEAR_2026);

            assertThat(class2_2025.weeklyRate())
                .as("2025/26 Class 2 weekly rate per HMRC")
                .isEqualByComparingTo(new BigDecimal("3.50"));
            assertThat(class2_2026.weeklyRate())
                .as("2026/27 Class 2 weekly rate per HMRC")
                .isEqualByComparingTo(new BigDecimal("3.65"));
            assertThat(class2_2026.weeklyRate())
                .as("year switch must change the weekly rate")
                .isGreaterThan(class2_2025.weeklyRate());
        }

        @Test
        @DisplayName("2025/26 NI Class 4 values match the published HMRC figures (baseline pin)")
        void rates2025MatchPublishedHmrcFigures() {
            // Without this pin, a silent corruption of 2025-26.yaml could let the
            // year-switch assertions above pass while still serving wrong numbers.
            NIClass4Rates rates =
                TaxRateConfiguration.getInstance().getNIClass4Rates(TAX_YEAR_2025);

            assertThat(rates.lowerProfitsLimit()).isEqualByComparingTo(new BigDecimal("12570"));
            assertThat(rates.upperProfitsLimit()).isEqualByComparingTo(new BigDecimal("50270"));
            assertThat(rates.mainRate()).isEqualByComparingTo(new BigDecimal("0.06"));
            assertThat(rates.additionalRate()).isEqualByComparingTo(new BigDecimal("0.02"));
        }
    }
}
