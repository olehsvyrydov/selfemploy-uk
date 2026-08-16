package uk.selfemploy.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.calculator.TaxLiabilityCalculator;
import uk.selfemploy.core.calculator.TaxLiabilityResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rate files run out at some point, and the day they do the app must stop claiming its
 * figures are that year's tax.
 *
 * <p>{@link #currentTaxYearIsConfigured()} is the tripwire: it fails on the first run after the
 * last shipped rate file expires, so the gap announces itself in the build rather than in a
 * user's estimate. It is meant to go red — the fix is to add the next year's rate file, not to
 * relax the assertion.
 */
@DisplayName("Tax year rate cliff")
class TaxYearRateCliffTest {

    private final TaxRateConfiguration configuration = TaxRateConfiguration.getInstance();

    @Test
    @DisplayName("the tax year we are actually in has its own rate file")
    void currentTaxYearIsConfigured() {
        int wallClockTaxYear = TaxYear.current().startYear();

        assertThat(configuration.isTaxYearSupported(wallClockTaxYear))
            .as("No tax-rates/%d-%02d.yaml. Figures for the current year are being estimated on "
                    + "an earlier year's rates — add the rate file.",
                wallClockTaxYear, (wallClockTaxYear + 1) % 100)
            .isTrue();
    }

    @Test
    @DisplayName("a configured year is its own rate basis and is not an estimate")
    void configuredYearIsNotEstimated() {
        for (int year : configuration.getSupportedTaxYears()) {
            RateBasis basis = configuration.rateBasisFor(year);

            assertThat(basis.ratesYear()).isEqualTo(year);
            assertThat(basis.isEstimated()).isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {2027, 2028, 2031, 2040})
    @DisplayName("a year past the last rate file is served that file's rates, and says so")
    void unconfiguredFutureYearIsEstimatedOnTheLatestConfiguredYear(int taxYear) {
        int latestConfigured = configuration.getSupportedTaxYears().stream()
            .mapToInt(Integer::intValue).max().orElseThrow();

        RateBasis basis = configuration.rateBasisFor(taxYear);

        assertThat(basis.isEstimated()).isTrue();
        assertThat(basis.ratesYear()).isEqualTo(latestConfigured);
        assertThat(configuration.getNIClass2Rates(taxYear))
            .isEqualTo(configuration.getNIClass2Rates(latestConfigured));
        assertThat(configuration.getIncomeTaxRates(taxYear))
            .isEqualTo(configuration.getIncomeTaxRates(latestConfigured));
        assertThat(configuration.getNIClass4Rates(taxYear))
            .isEqualTo(configuration.getNIClass4Rates(latestConfigured));
    }

    @Test
    @DisplayName("a year before the first rate file is served that file's rates, and says so")
    void unconfiguredPastYearIsEstimatedOnTheEarliestConfiguredYear() {
        int earliestConfigured = configuration.getSupportedTaxYears().stream()
            .mapToInt(Integer::intValue).min().orElseThrow();

        RateBasis basis = configuration.rateBasisFor(earliestConfigured - 3);

        assertThat(basis.isEstimated()).isTrue();
        assertThat(basis.ratesYear()).isEqualTo(earliestConfigured);
    }

    @Test
    @DisplayName("no year is served the hardcoded defaults instead of a published rate file")
    void hardcodedDefaultsAreNeverServed() {
        for (int year = 2015; year <= 2045; year++) {
            RateBasis basis = configuration.rateBasisFor(year);

            assertThat(configuration.isTaxYearSupported(basis.ratesYear()))
                .as("rates for %d came from unpublished year %d", year, basis.ratesYear())
                .isTrue();
            assertThat(configuration.getNIClass2Rates(year))
                .as("rates for %d", year)
                .isEqualTo(configuration.getNIClass2Rates(basis.ratesYear()));
        }
    }

    @Test
    @DisplayName("a liability carries the rates year it was computed on")
    void liabilityCarriesItsRateBasis() {
        int latestConfigured = configuration.getSupportedTaxYears().stream()
            .mapToInt(Integer::intValue).max().orElseThrow();
        BigDecimal profit = new BigDecimal("40000");

        TaxLiabilityResult configured =
            new TaxLiabilityCalculator(latestConfigured).calculate(profit);
        TaxLiabilityResult beyondTheCliff =
            new TaxLiabilityCalculator(latestConfigured + 1).calculate(profit);

        assertThat(configured.rateBasis().isEstimated()).isFalse();
        assertThat(configured.rateBasis().ratesYear()).isEqualTo(latestConfigured);

        assertThat(beyondTheCliff.rateBasis().isEstimated()).isTrue();
        assertThat(beyondTheCliff.rateBasis().ratesYear()).isEqualTo(latestConfigured);
        assertThat(beyondTheCliff.rateBasis().taxYear()).isEqualTo(latestConfigured + 1);
        assertThat(beyondTheCliff.rateBasis().ratesYearLabel())
            .isEqualTo(latestConfigured + "/" + String.format("%02d", (latestConfigured + 1) % 100));
    }
}
