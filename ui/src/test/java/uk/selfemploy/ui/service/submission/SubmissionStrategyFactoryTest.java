package uk.selfemploy.ui.service.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.selfemploy.common.domain.TaxYear;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SubmissionStrategyFactory.
 *
 * <p>Verifies that the correct submission strategy is selected based on tax year,
 * following HMRC API versioning requirements:</p>
 * <ul>
 *   <li>Tax years 2017-18 to 2024-25: PeriodSubmissionStrategy (POST /period)</li>
 *   <li>Tax years 2025-26 onwards: CumulativeSubmissionStrategy (PUT /cumulative)</li>
 * </ul>
 */
@DisplayName("SubmissionStrategyFactory Tests")
class SubmissionStrategyFactoryTest {

    private SubmissionStrategyFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SubmissionStrategyFactory();
    }

    @Nested
    @DisplayName("Strategy Selection by Tax Year")
    class StrategySelectionTests {

        @ParameterizedTest(name = "Tax year {0} should use PeriodSubmissionStrategy")
        @ValueSource(ints = {2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024})
        @DisplayName("should return PeriodSubmissionStrategy for tax years 2017-2024")
        void shouldReturnPeriodStrategyForOlderTaxYears(int startYear) {
            TaxYear taxYear = TaxYear.of(startYear);

            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy).isInstanceOf(PeriodSubmissionStrategy.class);
            assertThat(strategy.getHttpMethod()).isEqualTo("POST");
        }

        @ParameterizedTest(name = "Tax year {0} should use CumulativeSubmissionStrategy")
        @ValueSource(ints = {2025, 2026, 2027, 2028, 2029, 2030})
        @DisplayName("should return CumulativeSubmissionStrategy for tax years 2025+")
        void shouldReturnCumulativeStrategyForNewerTaxYears(int startYear) {
            TaxYear taxYear = TaxYear.of(startYear);

            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy).isInstanceOf(CumulativeSubmissionStrategy.class);
            assertThat(strategy.getHttpMethod()).isEqualTo("PUT");
        }

        @Test
        @DisplayName("should return default strategy when tax year is null")
        void shouldReturnDefaultStrategyWhenTaxYearIsNull() {
            SubmissionStrategy strategy = factory.getStrategy(null);

            // Default is PeriodSubmissionStrategy for safety with unknown tax years
            assertThat(strategy).isInstanceOf(PeriodSubmissionStrategy.class);
        }
    }

    @Nested
    @DisplayName("Endpoint URL Generation")
    class EndpointUrlTests {

        private static final String BASE_URL = "https://test-api.service.hmrc.gov.uk";
        private static final String NINO = "QQ123456C";
        private static final String BUSINESS_ID = "XAIS12345678901";

        @ParameterizedTest(name = "Tax year {0} should generate /period endpoint")
        @CsvSource({
            "2024, /individuals/business/self-employment/QQ123456C/XAIS12345678901/period"
        })
        @DisplayName("should generate period endpoint URL for older tax years")
        void shouldGeneratePeriodUrl(int startYear, String expectedPath) {
            TaxYear taxYear = TaxYear.of(startYear);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            String url = strategy.buildEndpointUrl(BASE_URL, NINO, BUSINESS_ID, taxYear);

            assertThat(url).isEqualTo(BASE_URL + expectedPath);
        }

        @ParameterizedTest(name = "Tax year {0} should generate /cumulative?taxYear={1} endpoint")
        @CsvSource({
            "2025, 2025-26",
            "2026, 2026-27",
            "2027, 2027-28"
        })
        @DisplayName("should generate cumulative endpoint URL with taxYear query param for newer tax years")
        void shouldGenerateCumulativeUrl(int startYear, String expectedTaxYearParam) {
            TaxYear taxYear = TaxYear.of(startYear);
            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            String url = strategy.buildEndpointUrl(BASE_URL, NINO, BUSINESS_ID, taxYear);

            assertThat(url).isEqualTo(BASE_URL + "/individuals/business/self-employment/"
                    + NINO + "/" + BUSINESS_ID + "/cumulative?taxYear=" + expectedTaxYearParam);
        }
    }

    @Nested
    @DisplayName("HTTP Method Selection")
    class HttpMethodTests {

        @Test
        @DisplayName("PeriodSubmissionStrategy should use POST")
        void periodStrategyShouldUsePost() {
            SubmissionStrategy strategy = factory.getStrategy(TaxYear.of(2024));

            assertThat(strategy.getHttpMethod()).isEqualTo("POST");
        }

        @Test
        @DisplayName("CumulativeSubmissionStrategy should use PUT")
        void cumulativeStrategyShouldUsePut() {
            SubmissionStrategy strategy = factory.getStrategy(TaxYear.of(2025));

            assertThat(strategy.getHttpMethod()).isEqualTo("PUT");
        }
    }

    @Nested
    @DisplayName("Strategy Registration")
    class StrategyRegistrationTests {

        @Test
        @DisplayName("should return all registered strategies")
        void shouldReturnAllRegisteredStrategies() {
            List<SubmissionStrategy> strategies = factory.getStrategies();

            assertThat(strategies).hasSize(2);
            assertThat(strategies).extracting(s -> s.getClass().getSimpleName())
                    .containsExactly("CumulativeSubmissionStrategy", "PeriodSubmissionStrategy");
        }

        @Test
        @DisplayName("should return default strategy")
        void shouldReturnDefaultStrategy() {
            SubmissionStrategy defaultStrategy = factory.getDefaultStrategy();

            assertThat(defaultStrategy).isInstanceOf(PeriodSubmissionStrategy.class);
        }

        @Test
        @DisplayName("should allow registering new strategies with priority")
        void shouldAllowRegisteringNewStrategies() {
            // Create a custom strategy for testing
            SubmissionStrategy customStrategy = new PeriodSubmissionStrategy() {
                @Override
                public String getDescription() {
                    return "Custom test strategy";
                }

                @Override
                public boolean supports(TaxYear taxYear) {
                    return taxYear != null && taxYear.startYear() == 2024;
                }
            };

            factory.registerStrategy(customStrategy);

            // New strategy should take priority for 2024
            SubmissionStrategy selected = factory.getStrategy(TaxYear.of(2024));
            assertThat(selected.getDescription()).isEqualTo("Custom test strategy");
        }
    }

    @Nested
    @DisplayName("Strategy Description")
    class StrategyDescriptionTests {

        @Test
        @DisplayName("PeriodSubmissionStrategy should have descriptive description")
        void periodStrategyDescription() {
            SubmissionStrategy strategy = factory.getStrategy(TaxYear.of(2024));

            assertThat(strategy.getDescription())
                    .contains("Period")
                    .contains("2024-25");
        }

        @Test
        @DisplayName("CumulativeSubmissionStrategy should have descriptive description")
        void cumulativeStrategyDescription() {
            SubmissionStrategy strategy = factory.getStrategy(TaxYear.of(2025));

            assertThat(strategy.getDescription())
                    .contains("Cumulative")
                    .contains("2025-26");
        }
    }

    @Nested
    @DisplayName("Far Future Tax Years")
    class FarFutureTaxYearTests {

        @ParameterizedTest(name = "Tax year {0} should still use CumulativeSubmissionStrategy")
        @ValueSource(ints = {2040, 2050, 2100})
        @DisplayName("should use CumulativeSubmissionStrategy for far future tax years")
        void shouldUseCumulativeForFarFuture(int startYear) {
            TaxYear taxYear = TaxYear.of(startYear);

            SubmissionStrategy strategy = factory.getStrategy(taxYear);

            assertThat(strategy).isInstanceOf(CumulativeSubmissionStrategy.class);
        }
    }
}
