package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.extension.TaxResult.TaxBand;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TaxResult}.
 */
@DisplayName("TaxResult")
class TaxResultTest {

    @Nested
    @DisplayName("when creating result")
    class Creation {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            TaxBand band = new TaxBand(
                "Basic Rate",
                new BigDecimal("20"),
                new BigDecimal("30000"),
                new BigDecimal("6000")
            );

            TaxResult result = new TaxResult(
                new BigDecimal("6000"),
                new BigDecimal("2400"),
                new BigDecimal("178"),
                List.of(band),
                null
            );

            assertThat(result.incomeTax()).isEqualByComparingTo("6000");
            assertThat(result.niClass4()).isEqualByComparingTo("2400");
            assertThat(result.niClass2()).isEqualByComparingTo("178");
            assertThat(result.taxBandBreakdown()).hasSize(1);
        }

        @Test
        @DisplayName("should reject null income tax")
        void shouldRejectNullIncomeTax() {
            assertThatThrownBy(() -> new TaxResult(
                null, BigDecimal.ZERO, BigDecimal.ZERO, null, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Income tax");
        }

        @Test
        @DisplayName("should reject null NI Class 4")
        void shouldRejectNullNiClass4() {
            assertThatThrownBy(() -> new TaxResult(
                BigDecimal.ZERO, null, BigDecimal.ZERO, null, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("NI Class 4");
        }

        @Test
        @DisplayName("should reject null NI Class 2")
        void shouldRejectNullNiClass2() {
            assertThatThrownBy(() -> new TaxResult(
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("NI Class 2");
        }

        @Test
        @DisplayName("should convert null band breakdown to empty list")
        void shouldConvertNullBandBreakdownToEmptyList() {
            TaxResult result = new TaxResult(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null
            );

            assertThat(result.taxBandBreakdown()).isEmpty();
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("of should create simple result")
        void ofShouldCreateSimpleResult() {
            TaxResult result = TaxResult.of(
                new BigDecimal("6000"),
                new BigDecimal("2400"),
                new BigDecimal("178")
            );

            assertThat(result.incomeTax()).isEqualByComparingTo("6000");
            assertThat(result.niClass4()).isEqualByComparingTo("2400");
            assertThat(result.niClass2()).isEqualByComparingTo("178");
            assertThat(result.taxBandBreakdown()).isEmpty();
        }

        @Test
        @DisplayName("zero should create zero result")
        void zeroShouldCreateZeroResult() {
            TaxResult result = TaxResult.zero();

            assertThat(result.incomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.niClass4()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.niClass2()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalTaxDue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("calculation methods")
    class CalculationMethods {

        @Test
        @DisplayName("totalTaxDue should sum all components")
        void totalTaxDueShouldSumAllComponents() {
            TaxResult result = TaxResult.of(
                new BigDecimal("6000"),
                new BigDecimal("2400"),
                new BigDecimal("178")
            );

            assertThat(result.totalTaxDue()).isEqualByComparingTo("8578");
        }

        @Test
        @DisplayName("totalNationalInsurance should sum NI components")
        void totalNationalInsuranceShouldSumNiComponents() {
            TaxResult result = TaxResult.of(
                new BigDecimal("6000"),
                new BigDecimal("2400"),
                new BigDecimal("178")
            );

            assertThat(result.totalNationalInsurance()).isEqualByComparingTo("2578");
        }
    }

    @Nested
    @DisplayName("TaxBand record")
    class TaxBandRecord {

        @Test
        @DisplayName("should create valid tax band")
        void shouldCreateValidTaxBand() {
            TaxBand band = new TaxBand(
                "Basic Rate",
                new BigDecimal("20"),
                new BigDecimal("30000"),
                new BigDecimal("6000")
            );

            assertThat(band.bandName()).isEqualTo("Basic Rate");
            assertThat(band.rate()).isEqualByComparingTo("20");
            assertThat(band.taxableAmount()).isEqualByComparingTo("30000");
            assertThat(band.taxAmount()).isEqualByComparingTo("6000");
        }

        @Test
        @DisplayName("should reject null band name")
        void shouldRejectNullBandName() {
            assertThatThrownBy(() -> new TaxBand(
                null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            ))
                .isInstanceOf(NullPointerException.class);
        }
    }
}
