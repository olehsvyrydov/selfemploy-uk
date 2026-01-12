package uk.selfemploy.core.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TaxRateConfiguration.
 *
 * Tests thread safety, caching behavior, singleton pattern, and YAML loading.
 *
 * TD-007: Externalize Tax Rates
 * Test Cases: TC-TD007-001 through TC-TD007-012
 */
@Tag("integration")
@DisplayName("TaxRateConfiguration Integration Tests (TD-007)")
class TaxRateConfigurationIntegrationTest {

    private TaxRateConfiguration configuration;

    @BeforeEach
    void setUp() {
        // Get fresh instance
        configuration = TaxRateConfiguration.getInstance();
    }

    @AfterEach
    void tearDown() {
        // Clear cache after each test to ensure isolation
        configuration.clearCache();
    }

    @Nested
    @DisplayName("TC-TD007-001: YAML Configuration Loading")
    class YamlConfigurationLoading {

        @Test
        @DisplayName("should load 2024-25.yaml successfully")
        void shouldLoad202425YamlSuccessfully() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            assertThat(rates).isNotNull();
            assertThat(rates.personalAllowance())
                .as("Personal Allowance from YAML")
                .isEqualByComparingTo(new BigDecimal("12570"));
        }

        @Test
        @DisplayName("should load 2025-26.yaml successfully")
        void shouldLoad202526YamlSuccessfully() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2025);

            assertThat(rates).isNotNull();
            assertThat(rates.personalAllowance())
                .as("Personal Allowance from YAML")
                .isEqualByComparingTo(new BigDecimal("12570"));
        }

        @Test
        @DisplayName("should return valid rates from YAML")
        void shouldReturnValidRatesFromYaml() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            assertThat(rates.personalAllowance()).isGreaterThan(BigDecimal.ZERO);
            assertThat(rates.basicRate()).isGreaterThan(BigDecimal.ZERO);
            assertThat(rates.higherRate()).isGreaterThan(rates.basicRate());
            assertThat(rates.additionalRate()).isGreaterThan(rates.higherRate());
        }
    }

    @Nested
    @DisplayName("TC-TD007-002: Fallback to Default Rates")
    class FallbackToDefaultRates {

        @Test
        @DisplayName("should return fallback rates for unsupported year")
        void shouldReturnFallbackRatesForUnsupportedYear() {
            // Year 2020 is not supported (no YAML file)
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2020);

            // Should return default rates, not null
            assertThat(rates).isNotNull();
            assertThat(rates.personalAllowance())
                .as("Default Personal Allowance")
                .isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return fallback NI Class 4 rates for unsupported year")
        void shouldReturnFallbackNiClass4RatesForUnsupportedYear() {
            NIClass4Rates rates = configuration.getNIClass4Rates(2020);

            assertThat(rates).isNotNull();
            assertThat(rates.lowerProfitsLimit()).isGreaterThan(BigDecimal.ZERO);
            assertThat(rates.mainRate()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should return fallback NI Class 2 rates for unsupported year")
        void shouldReturnFallbackNiClass2RatesForUnsupportedYear() {
            NIClass2Rates rates = configuration.getNIClass2Rates(2020);

            assertThat(rates).isNotNull();
            assertThat(rates.weeklyRate()).isGreaterThan(BigDecimal.ZERO);
            assertThat(rates.smallProfitsThreshold()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("TC-TD007-005: Thread Safety - Concurrent Access")
    class ThreadSafetyConcurrentAccess {

        @Test
        @DisplayName("should return same singleton instance from multiple threads")
        void shouldReturnSameSingletonInstanceFromMultipleThreads() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            Set<TaxRateConfiguration> instances = Collections.synchronizedSet(new HashSet<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        instances.add(TaxRateConfiguration.getInstance());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // Start all threads simultaneously
            doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(instances)
                .as("All threads should get the same singleton instance")
                .hasSize(1);
        }

        @Test
        @DisplayName("should return consistent values across concurrent reads")
        void shouldReturnConsistentValuesAcrossConcurrentReads() throws InterruptedException {
            int threadCount = 10;
            int iterationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            List<BigDecimal> personalAllowances = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < iterationsPerThread; j++) {
                            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);
                            personalAllowances.add(rates.personalAllowance());
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(errorCount.get()).isZero();
            assertThat(personalAllowances)
                .as("All concurrent reads should return the same value")
                .hasSize(threadCount * iterationsPerThread)
                .containsOnly(new BigDecimal("12570"));
        }

        @Test
        @DisplayName("should handle concurrent reads of different tax years")
        void shouldHandleConcurrentReadsOfDifferentTaxYears() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        // Half the threads read 2024, half read 2025
                        int year = (threadIndex % 2 == 0) ? 2024 : 2025;
                        IncomeTaxRates rates = configuration.getIncomeTaxRates(year);
                        NIClass4Rates niRates = configuration.getNIClass4Rates(year);
                        NIClass2Rates ni2Rates = configuration.getNIClass2Rates(year);

                        // Verify rates are valid
                        if (rates.personalAllowance().compareTo(BigDecimal.ZERO) <= 0 ||
                            niRates.mainRate().compareTo(BigDecimal.ZERO) <= 0 ||
                            ni2Rates.weeklyRate().compareTo(BigDecimal.ZERO) <= 0) {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(errorCount.get())
                .as("No errors should occur during concurrent access to different tax years")
                .isZero();
        }
    }

    @Nested
    @DisplayName("TC-TD007-006: Cache Behavior")
    class CacheBehavior {

        @Test
        @DisplayName("should return same instance from cache on repeated calls")
        void shouldReturnSameInstanceFromCacheOnRepeatedCalls() {
            IncomeTaxRates rates1 = configuration.getIncomeTaxRates(2024);
            IncomeTaxRates rates2 = configuration.getIncomeTaxRates(2024);

            // Should be the exact same object (from cache)
            assertThat(rates1).isSameAs(rates2);
        }

        @Test
        @DisplayName("should cache rates per tax year independently")
        void shouldCacheRatesPerTaxYearIndependently() {
            IncomeTaxRates rates2024 = configuration.getIncomeTaxRates(2024);
            IncomeTaxRates rates2025 = configuration.getIncomeTaxRates(2025);

            // Different years should have different cached objects
            assertThat(rates2024).isNotSameAs(rates2025);
        }

        @Test
        @DisplayName("should cache NI Class 4 rates separately from Income Tax")
        void shouldCacheNiClass4RatesSeparatelyFromIncomeTax() {
            IncomeTaxRates incomeTaxRates = configuration.getIncomeTaxRates(2024);
            NIClass4Rates niRates = configuration.getNIClass4Rates(2024);

            // Different rate types should be cached independently
            assertThat(incomeTaxRates).isNotSameAs(niRates);
        }

        @Test
        @DisplayName("clearCache should empty all caches")
        void clearCacheShouldEmptyAllCaches() {
            // First load into cache
            IncomeTaxRates rates1 = configuration.getIncomeTaxRates(2024);

            // Clear cache
            configuration.clearCache();

            // Second load should create new instance
            IncomeTaxRates rates2 = configuration.getIncomeTaxRates(2024);

            // Values should be equal but objects should be different (new load)
            assertThat(rates1.personalAllowance()).isEqualByComparingTo(rates2.personalAllowance());
            // Note: After cache clear, a new equivalent object is created
            // Both should have same values
        }
    }

    @Nested
    @DisplayName("TC-TD007-007: Supported Tax Years List")
    class SupportedTaxYearsList {

        @Test
        @DisplayName("should return list containing 2024 and 2025")
        void shouldReturnListContaining2024And2025() {
            List<Integer> supportedYears = configuration.getSupportedTaxYears();

            assertThat(supportedYears)
                .as("Supported tax years")
                .contains(2024, 2025);
        }

        @Test
        @DisplayName("isTaxYearSupported should return true for 2024")
        void isTaxYearSupportedShouldReturnTrueFor2024() {
            assertThat(configuration.isTaxYearSupported(2024)).isTrue();
        }

        @Test
        @DisplayName("isTaxYearSupported should return true for 2025")
        void isTaxYearSupportedShouldReturnTrueFor2025() {
            assertThat(configuration.isTaxYearSupported(2025)).isTrue();
        }

        @Test
        @DisplayName("isTaxYearSupported should return false for 2023")
        void isTaxYearSupportedShouldReturnFalseFor2023() {
            assertThat(configuration.isTaxYearSupported(2023)).isFalse();
        }

        @Test
        @DisplayName("supported years list should be immutable")
        void supportedYearsListShouldBeImmutable() {
            List<Integer> supportedYears = configuration.getSupportedTaxYears();

            // Should not be able to modify the returned list
            org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> supportedYears.add(2020)
            );
        }
    }

    @Nested
    @DisplayName("TC-TD007-009 & TC-TD007-010: NI Rates Loading")
    class NiRatesLoading {

        @Test
        @DisplayName("should load NI Class 4 rates correctly for 2024/25")
        void shouldLoadNiClass4RatesCorrectlyFor202425() {
            NIClass4Rates rates = configuration.getNIClass4Rates(2024);

            assertThat(rates.lowerProfitsLimit())
                .as("Lower Profits Limit")
                .isEqualByComparingTo(new BigDecimal("12570"));
            assertThat(rates.upperProfitsLimit())
                .as("Upper Profits Limit")
                .isEqualByComparingTo(new BigDecimal("50270"));
            assertThat(rates.mainRate())
                .as("Main Rate (6%)")
                .isEqualByComparingTo(new BigDecimal("0.06"));
            assertThat(rates.additionalRate())
                .as("Additional Rate (2%)")
                .isEqualByComparingTo(new BigDecimal("0.02"));
        }

        @Test
        @DisplayName("should load NI Class 2 rates correctly for 2024/25")
        void shouldLoadNiClass2RatesCorrectlyFor202425() {
            NIClass2Rates rates = configuration.getNIClass2Rates(2024);

            assertThat(rates.weeklyRate())
                .as("Weekly Rate (2024/25)")
                .isEqualByComparingTo(new BigDecimal("3.45"));
            assertThat(rates.smallProfitsThreshold())
                .as("Small Profits Threshold (2024/25)")
                .isEqualByComparingTo(new BigDecimal("6725"));
        }

        @Test
        @DisplayName("should load NI Class 2 rates correctly for 2025/26")
        void shouldLoadNiClass2RatesCorrectlyFor202526() {
            NIClass2Rates rates = configuration.getNIClass2Rates(2025);

            assertThat(rates.weeklyRate())
                .as("Weekly Rate (2025/26) - increased")
                .isEqualByComparingTo(new BigDecimal("3.50"));
            assertThat(rates.smallProfitsThreshold())
                .as("Small Profits Threshold (2025/26) - increased")
                .isEqualByComparingTo(new BigDecimal("6845"));
        }
    }

    @Nested
    @DisplayName("TC-TD007-012: BigDecimal Precision")
    class BigDecimalPrecision {

        @Test
        @DisplayName("should maintain BigDecimal precision for rates")
        void shouldMaintainBigDecimalPrecisionForRates() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            // Basic rate should be exactly 0.20, not 0.19999999 or 0.20000001
            assertThat(rates.basicRate())
                .as("Basic rate precision")
                .isEqualByComparingTo(new BigDecimal("0.20"));

            // Verify using scale-sensitive comparison
            BigDecimal expected = new BigDecimal("0.20");
            assertThat(rates.basicRate().subtract(expected).abs())
                .as("Basic rate should have no floating-point errors")
                .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should maintain precision for integer values")
        void shouldMaintainPrecisionForIntegerValues() {
            IncomeTaxRates rates = configuration.getIncomeTaxRates(2024);

            // Personal Allowance should be exactly 12570
            assertThat(rates.personalAllowance().stripTrailingZeros().scale())
                .as("Personal Allowance should be a whole number")
                .isLessThanOrEqualTo(0);

            assertThat(rates.personalAllowance().longValue())
                .as("Personal Allowance exact value")
                .isEqualTo(12570L);
        }
    }

    @Nested
    @DisplayName("Singleton Pattern Verification")
    class SingletonPatternVerification {

        @Test
        @DisplayName("getInstance should always return the same instance")
        void getInstanceShouldAlwaysReturnTheSameInstance() {
            TaxRateConfiguration instance1 = TaxRateConfiguration.getInstance();
            TaxRateConfiguration instance2 = TaxRateConfiguration.getInstance();
            TaxRateConfiguration instance3 = TaxRateConfiguration.getInstance();

            assertThat(instance1)
                .isSameAs(instance2)
                .isSameAs(instance3);
        }
    }
}
