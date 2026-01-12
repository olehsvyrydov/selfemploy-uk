package uk.selfemploy.core.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation tests to verify externalized tax rates match HMRC published rates.
 *
 * These tests act as a contract verification against official HMRC rates.
 * If any test fails, it indicates a discrepancy between our configuration
 * and HMRC's published rates that must be investigated and corrected.
 *
 * HMRC Sources:
 * - Income Tax rates: https://www.gov.uk/income-tax-rates
 * - National Insurance rates: https://www.gov.uk/self-employed-national-insurance-rates
 *
 * TD-009: Rate Validation vs HMRC Published
 */
@DisplayName("Tax Rate Validation Against HMRC Published Rates")
class TaxRateValidationTest {

    private static TaxRateConfiguration configuration;

    @BeforeAll
    static void setUp() {
        configuration = TaxRateConfiguration.getInstance();
    }

    @Nested
    @DisplayName("2024/25 Tax Year (6 April 2024 - 5 April 2025)")
    class TaxYear2024_25 {

        private static final int TAX_YEAR = 2024;

        @Nested
        @DisplayName("Income Tax Rates")
        class IncomeTaxRates2024 {

            @Test
            @DisplayName("Personal Allowance should be 12,570 (HMRC 2024/25)")
            void personalAllowanceShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.personalAllowance())
                    .as("HMRC 2024/25 Personal Allowance must be exactly 12,570")
                    .isEqualByComparingTo(new BigDecimal("12570"));
            }

            @Test
            @DisplayName("Basic Rate should be 20% (HMRC 2024/25)")
            void basicRateShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.basicRate())
                    .as("HMRC 2024/25 Basic Rate must be exactly 20% (0.20)")
                    .isEqualByComparingTo(new BigDecimal("0.20"));
            }

            @Test
            @DisplayName("Basic Rate Upper Limit should be 50,270 (HMRC 2024/25)")
            void basicRateUpperLimitShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.basicRateUpperLimit())
                    .as("HMRC 2024/25 Basic Rate applies to income from 12,571 to 50,270")
                    .isEqualByComparingTo(new BigDecimal("50270"));
            }

            @Test
            @DisplayName("Higher Rate should be 40% (HMRC 2024/25)")
            void higherRateShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.higherRate())
                    .as("HMRC 2024/25 Higher Rate must be exactly 40% (0.40)")
                    .isEqualByComparingTo(new BigDecimal("0.40"));
            }

            @Test
            @DisplayName("Higher Rate Upper Limit should be 125,140 (HMRC 2024/25)")
            void higherRateUpperLimitShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.higherRateUpperLimit())
                    .as("HMRC 2024/25 Higher Rate applies to income from 50,271 to 125,140")
                    .isEqualByComparingTo(new BigDecimal("125140"));
            }

            @Test
            @DisplayName("Additional Rate should be 45% (HMRC 2024/25)")
            void additionalRateShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.additionalRate())
                    .as("HMRC 2024/25 Additional Rate must be exactly 45% (0.45)")
                    .isEqualByComparingTo(new BigDecimal("0.45"));
            }

            @Test
            @DisplayName("Personal Allowance Taper Threshold should be 100,000 (HMRC 2024/25)")
            void taperThresholdShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.taperThreshold())
                    .as("HMRC 2024/25 Personal Allowance reduces by 1 for every 2 over 100,000")
                    .isEqualByComparingTo(new BigDecimal("100000"));
            }
        }

        @Nested
        @DisplayName("National Insurance Class 4 Rates")
        class NIClass4Rates2024 {

            @Test
            @DisplayName("Lower Profits Limit should be 12,570 (HMRC 2024/25)")
            void lowerProfitsLimitShouldMatch() {
                NIClass4Rates rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(rates.lowerProfitsLimit())
                    .as("HMRC 2024/25 NI Class 4 Lower Profits Limit must be exactly 12,570")
                    .isEqualByComparingTo(new BigDecimal("12570"));
            }

            @Test
            @DisplayName("Upper Profits Limit should be 50,270 (HMRC 2024/25)")
            void upperProfitsLimitShouldMatch() {
                NIClass4Rates rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(rates.upperProfitsLimit())
                    .as("HMRC 2024/25 NI Class 4 Upper Profits Limit must be exactly 50,270")
                    .isEqualByComparingTo(new BigDecimal("50270"));
            }

            @Test
            @DisplayName("Main Rate should be 6% (HMRC 2024/25)")
            void mainRateShouldMatch() {
                NIClass4Rates rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(rates.mainRate())
                    .as("HMRC 2024/25 NI Class 4 Main Rate must be exactly 6% (0.06)")
                    .isEqualByComparingTo(new BigDecimal("0.06"));
            }

            @Test
            @DisplayName("Additional Rate should be 2% (HMRC 2024/25)")
            void additionalRateShouldMatch() {
                NIClass4Rates rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(rates.additionalRate())
                    .as("HMRC 2024/25 NI Class 4 Additional Rate must be exactly 2% (0.02)")
                    .isEqualByComparingTo(new BigDecimal("0.02"));
            }
        }

        @Nested
        @DisplayName("National Insurance Class 2 Rates")
        class NIClass2Rates2024 {

            @Test
            @DisplayName("Weekly Rate should be 3.45 (HMRC 2024/25)")
            void weeklyRateShouldMatch() {
                NIClass2Rates rates = configuration.getNIClass2Rates(TAX_YEAR);

                assertThat(rates.weeklyRate())
                    .as("HMRC 2024/25 NI Class 2 Weekly Rate must be exactly 3.45")
                    .isEqualByComparingTo(new BigDecimal("3.45"));
            }

            @Test
            @DisplayName("Small Profits Threshold should be 6,725 (HMRC 2024/25)")
            void smallProfitsThresholdShouldMatch() {
                NIClass2Rates rates = configuration.getNIClass2Rates(TAX_YEAR);

                assertThat(rates.smallProfitsThreshold())
                    .as("HMRC 2024/25 NI Class 2 Small Profits Threshold must be exactly 6,725")
                    .isEqualByComparingTo(new BigDecimal("6725"));
            }

            @Test
            @DisplayName("Annual Rate should be 179.40 (52 weeks x 3.45)")
            void annualRateShouldBeCorrect() {
                NIClass2Rates rates = configuration.getNIClass2Rates(TAX_YEAR);

                BigDecimal expectedAnnualRate = rates.weeklyRate().multiply(new BigDecimal("52"));

                assertThat(expectedAnnualRate)
                    .as("HMRC 2024/25 NI Class 2 Annual Rate (52 weeks x 3.45) should be 179.40")
                    .isEqualByComparingTo(new BigDecimal("179.40"));
            }
        }

        @Nested
        @DisplayName("Cross-Validation - Income Tax and NI Alignment")
        class CrossValidation2024 {

            @Test
            @DisplayName("Income Tax Personal Allowance should equal NI Class 4 Lower Profits Limit")
            void personalAllowanceShouldEqualLowerProfitsLimit() {
                IncomeTaxRates incomeTaxRates = configuration.getIncomeTaxRates(TAX_YEAR);
                NIClass4Rates niClass4Rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(incomeTaxRates.personalAllowance())
                    .as("For 2024/25, Income Tax Personal Allowance (12,570) should equal NI Class 4 Lower Profits Limit")
                    .isEqualByComparingTo(niClass4Rates.lowerProfitsLimit());
            }

            @Test
            @DisplayName("Income Tax Basic Rate Upper Limit should equal NI Class 4 Upper Profits Limit")
            void basicRateUpperLimitShouldEqualUpperProfitsLimit() {
                IncomeTaxRates incomeTaxRates = configuration.getIncomeTaxRates(TAX_YEAR);
                NIClass4Rates niClass4Rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(incomeTaxRates.basicRateUpperLimit())
                    .as("For 2024/25, Income Tax Basic Rate Upper Limit (50,270) should equal NI Class 4 Upper Profits Limit")
                    .isEqualByComparingTo(niClass4Rates.upperProfitsLimit());
            }
        }
    }

    @Nested
    @DisplayName("2025/26 Tax Year (6 April 2025 - 5 April 2026)")
    class TaxYear2025_26 {

        private static final int TAX_YEAR = 2025;

        @Nested
        @DisplayName("Income Tax Rates")
        class IncomeTaxRates2025 {

            @Test
            @DisplayName("Personal Allowance should be 12,570 (HMRC 2025/26 - frozen)")
            void personalAllowanceShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.personalAllowance())
                    .as("HMRC 2025/26 Personal Allowance (frozen) must be exactly 12,570")
                    .isEqualByComparingTo(new BigDecimal("12570"));
            }

            @Test
            @DisplayName("Basic Rate should be 20% (HMRC 2025/26)")
            void basicRateShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.basicRate())
                    .as("HMRC 2025/26 Basic Rate must be exactly 20% (0.20)")
                    .isEqualByComparingTo(new BigDecimal("0.20"));
            }

            @Test
            @DisplayName("Basic Rate Upper Limit should be 50,270 (HMRC 2025/26 - frozen)")
            void basicRateUpperLimitShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.basicRateUpperLimit())
                    .as("HMRC 2025/26 Basic Rate Upper Limit (frozen) must be exactly 50,270")
                    .isEqualByComparingTo(new BigDecimal("50270"));
            }

            @Test
            @DisplayName("Higher Rate should be 40% (HMRC 2025/26)")
            void higherRateShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.higherRate())
                    .as("HMRC 2025/26 Higher Rate must be exactly 40% (0.40)")
                    .isEqualByComparingTo(new BigDecimal("0.40"));
            }

            @Test
            @DisplayName("Higher Rate Upper Limit should be 125,140 (HMRC 2025/26 - frozen)")
            void higherRateUpperLimitShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.higherRateUpperLimit())
                    .as("HMRC 2025/26 Higher Rate Upper Limit (frozen) must be exactly 125,140")
                    .isEqualByComparingTo(new BigDecimal("125140"));
            }

            @Test
            @DisplayName("Additional Rate should be 45% (HMRC 2025/26)")
            void additionalRateShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.additionalRate())
                    .as("HMRC 2025/26 Additional Rate must be exactly 45% (0.45)")
                    .isEqualByComparingTo(new BigDecimal("0.45"));
            }

            @Test
            @DisplayName("Personal Allowance Taper Threshold should be 100,000 (HMRC 2025/26)")
            void taperThresholdShouldMatch() {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(TAX_YEAR);

                assertThat(rates.taperThreshold())
                    .as("HMRC 2025/26 Personal Allowance Taper Threshold must be exactly 100,000")
                    .isEqualByComparingTo(new BigDecimal("100000"));
            }
        }

        @Nested
        @DisplayName("National Insurance Class 4 Rates")
        class NIClass4Rates2025 {

            @Test
            @DisplayName("Lower Profits Limit should be 12,570 (HMRC 2025/26)")
            void lowerProfitsLimitShouldMatch() {
                NIClass4Rates rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(rates.lowerProfitsLimit())
                    .as("HMRC 2025/26 NI Class 4 Lower Profits Limit must be exactly 12,570")
                    .isEqualByComparingTo(new BigDecimal("12570"));
            }

            @Test
            @DisplayName("Upper Profits Limit should be 50,270 (HMRC 2025/26)")
            void upperProfitsLimitShouldMatch() {
                NIClass4Rates rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(rates.upperProfitsLimit())
                    .as("HMRC 2025/26 NI Class 4 Upper Profits Limit must be exactly 50,270")
                    .isEqualByComparingTo(new BigDecimal("50270"));
            }

            @Test
            @DisplayName("Main Rate should be 6% (HMRC 2025/26)")
            void mainRateShouldMatch() {
                NIClass4Rates rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(rates.mainRate())
                    .as("HMRC 2025/26 NI Class 4 Main Rate must be exactly 6% (0.06)")
                    .isEqualByComparingTo(new BigDecimal("0.06"));
            }

            @Test
            @DisplayName("Additional Rate should be 2% (HMRC 2025/26)")
            void additionalRateShouldMatch() {
                NIClass4Rates rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(rates.additionalRate())
                    .as("HMRC 2025/26 NI Class 4 Additional Rate must be exactly 2% (0.02)")
                    .isEqualByComparingTo(new BigDecimal("0.02"));
            }
        }

        @Nested
        @DisplayName("National Insurance Class 2 Rates")
        class NIClass2Rates2025 {

            @Test
            @DisplayName("Weekly Rate should be 3.50 (HMRC 2025/26)")
            void weeklyRateShouldMatch() {
                NIClass2Rates rates = configuration.getNIClass2Rates(TAX_YEAR);

                assertThat(rates.weeklyRate())
                    .as("HMRC 2025/26 NI Class 2 Weekly Rate must be exactly 3.50")
                    .isEqualByComparingTo(new BigDecimal("3.50"));
            }

            @Test
            @DisplayName("Small Profits Threshold should be 6,845 (HMRC 2025/26)")
            void smallProfitsThresholdShouldMatch() {
                NIClass2Rates rates = configuration.getNIClass2Rates(TAX_YEAR);

                assertThat(rates.smallProfitsThreshold())
                    .as("HMRC 2025/26 NI Class 2 Small Profits Threshold must be exactly 6,845")
                    .isEqualByComparingTo(new BigDecimal("6845"));
            }

            @Test
            @DisplayName("Annual Rate should be 182.00 (52 weeks x 3.50)")
            void annualRateShouldBeCorrect() {
                NIClass2Rates rates = configuration.getNIClass2Rates(TAX_YEAR);

                BigDecimal expectedAnnualRate = rates.weeklyRate().multiply(new BigDecimal("52"));

                assertThat(expectedAnnualRate)
                    .as("HMRC 2025/26 NI Class 2 Annual Rate (52 weeks x 3.50) should be 182.00")
                    .isEqualByComparingTo(new BigDecimal("182.00"));
            }
        }

        @Nested
        @DisplayName("Cross-Validation - Income Tax and NI Alignment")
        class CrossValidation2025 {

            @Test
            @DisplayName("Income Tax Personal Allowance should equal NI Class 4 Lower Profits Limit")
            void personalAllowanceShouldEqualLowerProfitsLimit() {
                IncomeTaxRates incomeTaxRates = configuration.getIncomeTaxRates(TAX_YEAR);
                NIClass4Rates niClass4Rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(incomeTaxRates.personalAllowance())
                    .as("For 2025/26, Income Tax Personal Allowance should equal NI Class 4 Lower Profits Limit")
                    .isEqualByComparingTo(niClass4Rates.lowerProfitsLimit());
            }

            @Test
            @DisplayName("Income Tax Basic Rate Upper Limit should equal NI Class 4 Upper Profits Limit")
            void basicRateUpperLimitShouldEqualUpperProfitsLimit() {
                IncomeTaxRates incomeTaxRates = configuration.getIncomeTaxRates(TAX_YEAR);
                NIClass4Rates niClass4Rates = configuration.getNIClass4Rates(TAX_YEAR);

                assertThat(incomeTaxRates.basicRateUpperLimit())
                    .as("For 2025/26, Income Tax Basic Rate Upper Limit should equal NI Class 4 Upper Profits Limit")
                    .isEqualByComparingTo(niClass4Rates.upperProfitsLimit());
            }
        }
    }

    @Nested
    @DisplayName("Rate Consistency Checks")
    class RateConsistencyChecks {

        @Test
        @DisplayName("Income Tax rates must be in ascending order (Basic < Higher < Additional)")
        void incomeTaxRatesShouldBeAscending() {
            for (int year : configuration.getSupportedTaxYears()) {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(year);

                assertThat(rates.basicRate())
                    .as("Basic rate must be less than Higher rate for tax year %d/%d", year, year + 1)
                    .isLessThan(rates.higherRate());

                assertThat(rates.higherRate())
                    .as("Higher rate must be less than Additional rate for tax year %d/%d", year, year + 1)
                    .isLessThan(rates.additionalRate());
            }
        }

        @Test
        @DisplayName("Income Tax thresholds must be in ascending order")
        void incomeTaxThresholdsShouldBeAscending() {
            for (int year : configuration.getSupportedTaxYears()) {
                IncomeTaxRates rates = configuration.getIncomeTaxRates(year);

                assertThat(rates.personalAllowance())
                    .as("Personal Allowance must be less than Basic Rate Upper Limit for tax year %d/%d", year, year + 1)
                    .isLessThan(rates.basicRateUpperLimit());

                assertThat(rates.basicRateUpperLimit())
                    .as("Basic Rate Upper Limit must be less than Higher Rate Upper Limit for tax year %d/%d", year, year + 1)
                    .isLessThan(rates.higherRateUpperLimit());

                assertThat(rates.taperThreshold())
                    .as("Taper Threshold must be less than Higher Rate Upper Limit for tax year %d/%d", year, year + 1)
                    .isLessThan(rates.higherRateUpperLimit());
            }
        }

        @Test
        @DisplayName("NI Class 4 thresholds must be in ascending order")
        void niClass4ThresholdsShouldBeAscending() {
            for (int year : configuration.getSupportedTaxYears()) {
                NIClass4Rates rates = configuration.getNIClass4Rates(year);

                assertThat(rates.lowerProfitsLimit())
                    .as("NI Class 4 Lower Profits Limit must be less than Upper Profits Limit for tax year %d/%d", year, year + 1)
                    .isLessThan(rates.upperProfitsLimit());
            }
        }

        @Test
        @DisplayName("NI Class 4 main rate must be greater than additional rate")
        void niClass4MainRateShouldBeGreaterThanAdditional() {
            for (int year : configuration.getSupportedTaxYears()) {
                NIClass4Rates rates = configuration.getNIClass4Rates(year);

                assertThat(rates.mainRate())
                    .as("NI Class 4 Main Rate (6%%) must be greater than Additional Rate (2%%) for tax year %d/%d", year, year + 1)
                    .isGreaterThan(rates.additionalRate());
            }
        }

        @Test
        @DisplayName("NI Class 2 weekly rate must be positive")
        void niClass2WeeklyRateShouldBePositive() {
            for (int year : configuration.getSupportedTaxYears()) {
                NIClass2Rates rates = configuration.getNIClass2Rates(year);

                assertThat(rates.weeklyRate())
                    .as("NI Class 2 Weekly Rate must be positive for tax year %d/%d", year, year + 1)
                    .isGreaterThan(BigDecimal.ZERO);
            }
        }

        @Test
        @DisplayName("NI Class 2 Small Profits Threshold must be positive")
        void niClass2SmallProfitsThresholdShouldBePositive() {
            for (int year : configuration.getSupportedTaxYears()) {
                NIClass2Rates rates = configuration.getNIClass2Rates(year);

                assertThat(rates.smallProfitsThreshold())
                    .as("NI Class 2 Small Profits Threshold must be positive for tax year %d/%d", year, year + 1)
                    .isGreaterThan(BigDecimal.ZERO);
            }
        }
    }

    @Nested
    @DisplayName("YAML Configuration Availability")
    class YamlConfigurationAvailability {

        @Test
        @DisplayName("Tax year 2024/25 should be supported")
        void taxYear2024ShouldBeSupported() {
            assertThat(configuration.isTaxYearSupported(2024))
                .as("Tax year 2024/25 must have a YAML configuration file")
                .isTrue();
        }

        @Test
        @DisplayName("Tax year 2025/26 should be supported")
        void taxYear2025ShouldBeSupported() {
            assertThat(configuration.isTaxYearSupported(2025))
                .as("Tax year 2025/26 must have a YAML configuration file")
                .isTrue();
        }

        @Test
        @DisplayName("Supported tax years list should include 2024 and 2025")
        void supportedYearsShouldInclude2024And2025() {
            assertThat(configuration.getSupportedTaxYears())
                .as("Supported tax years must include both 2024/25 and 2025/26")
                .contains(2024, 2025);
        }
    }
}
