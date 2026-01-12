package uk.selfemploy.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TaxRateConfiguration - externalized tax rates from YAML.
 *
 * Following TDD: These tests are written first, before implementation.
 */
@DisplayName("Tax Rate Configuration Tests")
class TaxRateConfigurationTest {

    private TaxRateConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = TaxRateConfiguration.getInstance();
    }

    @Nested
    @DisplayName("Income Tax Rates Loading")
    class IncomeTaxRatesLoading {

        @Test
        @DisplayName("should load personal allowance for 2024/25")
        void shouldLoadPersonalAllowanceFor2024() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            assertThat(rates.personalAllowance())
                .isEqualByComparingTo(new BigDecimal("12570"));
        }

        @Test
        @DisplayName("should load basic rate upper limit for 2024/25")
        void shouldLoadBasicRateUpperLimitFor2024() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            assertThat(rates.basicRateUpperLimit())
                .isEqualByComparingTo(new BigDecimal("50270"));
        }

        @Test
        @DisplayName("should load higher rate upper limit for 2024/25")
        void shouldLoadHigherRateUpperLimitFor2024() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            assertThat(rates.higherRateUpperLimit())
                .isEqualByComparingTo(new BigDecimal("125140"));
        }

        @Test
        @DisplayName("should load taper threshold for 2024/25")
        void shouldLoadTaperThresholdFor2024() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            assertThat(rates.taperThreshold())
                .isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("should load basic rate (20%) for 2024/25")
        void shouldLoadBasicRateFor2024() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            assertThat(rates.basicRate())
                .isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("should load higher rate (40%) for 2024/25")
        void shouldLoadHigherRateFor2024() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            assertThat(rates.higherRate())
                .isEqualByComparingTo(new BigDecimal("0.40"));
        }

        @Test
        @DisplayName("should load additional rate (45%) for 2024/25")
        void shouldLoadAdditionalRateFor2024() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            assertThat(rates.additionalRate())
                .isEqualByComparingTo(new BigDecimal("0.45"));
        }
    }

    @Nested
    @DisplayName("NI Class 4 Rates Loading")
    class NIClass4RatesLoading {

        @Test
        @DisplayName("should load lower profits limit for 2024/25")
        void shouldLoadLowerProfitsLimitFor2024() {
            NIClass4Rates rates = configuration.getNIClass4Rates(2024);

            assertThat(rates.lowerProfitsLimit())
                .isEqualByComparingTo(new BigDecimal("12570"));
        }

        @Test
        @DisplayName("should load upper profits limit for 2024/25")
        void shouldLoadUpperProfitsLimitFor2024() {
            NIClass4Rates rates = configuration.getNIClass4Rates(2024);

            assertThat(rates.upperProfitsLimit())
                .isEqualByComparingTo(new BigDecimal("50270"));
        }

        @Test
        @DisplayName("should load main rate (6%) for 2024/25")
        void shouldLoadMainRateFor2024() {
            NIClass4Rates rates = configuration.getNIClass4Rates(2024);

            assertThat(rates.mainRate())
                .isEqualByComparingTo(new BigDecimal("0.06"));
        }

        @Test
        @DisplayName("should load additional rate (2%) for 2024/25")
        void shouldLoadAdditionalRateFor2024() {
            NIClass4Rates rates = configuration.getNIClass4Rates(2024);

            assertThat(rates.additionalRate())
                .isEqualByComparingTo(new BigDecimal("0.02"));
        }
    }

    @Nested
    @DisplayName("NI Class 2 Rates Loading")
    class NIClass2RatesLoading {

        @Test
        @DisplayName("should load weekly rate for 2024/25")
        void shouldLoadWeeklyRateFor2024() {
            NIClass2Rates rates = configuration.getNIClass2Rates(2024);

            assertThat(rates.weeklyRate())
                .isEqualByComparingTo(new BigDecimal("3.45"));
        }

        @Test
        @DisplayName("should load small profits threshold for 2024/25")
        void shouldLoadSmallProfitsThresholdFor2024() {
            NIClass2Rates rates = configuration.getNIClass2Rates(2024);

            assertThat(rates.smallProfitsThreshold())
                .isEqualByComparingTo(new BigDecimal("6725"));
        }
    }

    @Nested
    @DisplayName("2025/26 Tax Year Rates")
    class TaxYear2025Rates {

        @Test
        @DisplayName("should load income tax rates for 2025/26")
        void shouldLoadIncomeTaxRatesFor2025() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2025);

            assertThat(rates.personalAllowance())
                .isEqualByComparingTo(new BigDecimal("12570"));
            assertThat(rates.basicRate())
                .isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("should load NI Class 4 rates for 2025/26")
        void shouldLoadNIClass4RatesFor2025() {
            NIClass4Rates rates = configuration.getNIClass4Rates(2025);

            assertThat(rates.mainRate())
                .isEqualByComparingTo(new BigDecimal("0.06"));
        }

        @Test
        @DisplayName("should load NI Class 2 rates for 2025/26")
        void shouldLoadNIClass2RatesFor2025() {
            NIClass2Rates rates = configuration.getNIClass2Rates(2025);

            assertThat(rates.weeklyRate())
                .isEqualByComparingTo(new BigDecimal("3.50"));
            assertThat(rates.smallProfitsThreshold())
                .isEqualByComparingTo(new BigDecimal("6845"));
        }
    }

    @Nested
    @DisplayName("Caching Behavior")
    class CachingBehavior {

        @Test
        @DisplayName("should return same instance for same tax year")
        void shouldReturnSameInstanceForSameTaxYear() {
            IncomeTaxRates rates1 = configuration.getIncomeTaxRates(2024);
            IncomeTaxRates rates2 = configuration.getIncomeTaxRates(2024);

            assertThat(rates1).isSameAs(rates2);
        }

        @Test
        @DisplayName("should cache NI Class 4 rates")
        void shouldCacheNIClass4Rates() {
            NIClass4Rates rates1 = configuration.getNIClass4Rates(2024);
            NIClass4Rates rates2 = configuration.getNIClass4Rates(2024);

            assertThat(rates1).isSameAs(rates2);
        }

        @Test
        @DisplayName("should cache NI Class 2 rates")
        void shouldCacheNIClass2Rates() {
            NIClass2Rates rates1 = configuration.getNIClass2Rates(2024);
            NIClass2Rates rates2 = configuration.getNIClass2Rates(2024);

            assertThat(rates1).isSameAs(rates2);
        }
    }

    @Nested
    @DisplayName("Fallback Behavior")
    class FallbackBehavior {

        @Test
        @DisplayName("should fallback to hardcoded rates for unknown tax year")
        void shouldFallbackToHardcodedRatesForUnknownYear() {
            // Year 2030 - no YAML file exists
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2030);

            // Should return fallback rates (same as latest known year)
            assertThat(rates.personalAllowance())
                .isEqualByComparingTo(new BigDecimal("12570"));
            assertThat(rates.basicRate())
                .isEqualByComparingTo(new BigDecimal("0.20"));
        }

        @Test
        @DisplayName("should fallback NI Class 4 rates for unknown tax year")
        void shouldFallbackNIClass4RatesForUnknownYear() {
            NIClass4Rates rates = configuration.getNIClass4Rates(2030);

            assertThat(rates.lowerProfitsLimit())
                .isEqualByComparingTo(new BigDecimal("12570"));
        }

        @Test
        @DisplayName("should fallback NI Class 2 rates for unknown tax year")
        void shouldFallbackNIClass2RatesForUnknownYear() {
            NIClass2Rates rates = configuration.getNIClass2Rates(2030);

            assertThat(rates.weeklyRate()).isNotNull();
            assertThat(rates.smallProfitsThreshold()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Singleton Pattern")
    class SingletonPattern {

        @Test
        @DisplayName("should return same instance from getInstance()")
        void shouldReturnSameInstance() {
            TaxRateConfiguration instance1 = TaxRateConfiguration.getInstance();
            TaxRateConfiguration instance2 = TaxRateConfiguration.getInstance();

            assertThat(instance1).isSameAs(instance2);
        }
    }

    @Nested
    @DisplayName("Tax Year Support")
    class TaxYearSupport {

        @Test
        @DisplayName("should return list of supported tax years")
        void shouldReturnListOfSupportedTaxYears() {
            var supportedYears = configuration.getSupportedTaxYears();

            assertThat(supportedYears).contains(2024, 2025);
        }

        @Test
        @DisplayName("should check if tax year is supported")
        void shouldCheckIfTaxYearIsSupported() {
            assertThat(configuration.isTaxYearSupported(2024)).isTrue();
            assertThat(configuration.isTaxYearSupported(2025)).isTrue();
            assertThat(configuration.isTaxYearSupported(2030)).isFalse();
        }
    }
}
