package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.extension.TaxContext.Region;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TaxContext}.
 */
@DisplayName("TaxContext")
class TaxContextTest {

    @Nested
    @DisplayName("when creating context")
    class Creation {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            BigDecimal income = new BigDecimal("50000");
            BigDecimal expenses = new BigDecimal("10000");

            TaxContext context = new TaxContext(2024, income, expenses, Region.ENGLAND, null);

            assertThat(context.taxYear()).isEqualTo(2024);
            assertThat(context.grossIncome()).isEqualByComparingTo(income);
            assertThat(context.totalExpenses()).isEqualByComparingTo(expenses);
            assertThat(context.region()).isEqualTo(Region.ENGLAND);
            assertThat(context.additionalData()).isEmpty();
        }

        @Test
        @DisplayName("should reject null gross income")
        void shouldRejectNullGrossIncome() {
            assertThatThrownBy(() -> new TaxContext(
                2024, null, BigDecimal.ZERO, Region.ENGLAND, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Gross income");
        }

        @Test
        @DisplayName("should reject null total expenses")
        void shouldRejectNullTotalExpenses() {
            assertThatThrownBy(() -> new TaxContext(
                2024, BigDecimal.ZERO, null, Region.ENGLAND, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Total expenses");
        }

        @Test
        @DisplayName("should reject null region")
        void shouldRejectNullRegion() {
            assertThatThrownBy(() -> new TaxContext(
                2024, BigDecimal.ZERO, BigDecimal.ZERO, null, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Region");
        }

        @Test
        @DisplayName("should reject negative gross income")
        void shouldRejectNegativeGrossIncome() {
            assertThatThrownBy(() -> new TaxContext(
                2024, new BigDecimal("-1"), BigDecimal.ZERO, Region.ENGLAND, null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gross income");
        }

        @Test
        @DisplayName("should reject negative total expenses")
        void shouldRejectNegativeTotalExpenses() {
            assertThatThrownBy(() -> new TaxContext(
                2024, BigDecimal.ZERO, new BigDecimal("-1"), Region.ENGLAND, null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total expenses");
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("forEngland should create England context")
        void forEnglandShouldCreateEnglandContext() {
            TaxContext context = TaxContext.forEngland(
                2024, new BigDecimal("50000"), new BigDecimal("10000")
            );

            assertThat(context.region()).isEqualTo(Region.ENGLAND);
            assertThat(context.region().usesScottishRates()).isFalse();
        }

        @Test
        @DisplayName("forScotland should create Scotland context")
        void forScotlandShouldCreateScotlandContext() {
            TaxContext context = TaxContext.forScotland(
                2024, new BigDecimal("50000"), new BigDecimal("10000")
            );

            assertThat(context.region()).isEqualTo(Region.SCOTLAND);
            assertThat(context.region().usesScottishRates()).isTrue();
        }
    }

    @Nested
    @DisplayName("getTaxableProfit method")
    class GetTaxableProfit {

        @Test
        @DisplayName("should calculate profit correctly")
        void shouldCalculateProfitCorrectly() {
            TaxContext context = TaxContext.forEngland(
                2024, new BigDecimal("50000"), new BigDecimal("10000")
            );

            assertThat(context.getTaxableProfit()).isEqualByComparingTo("40000");
        }

        @Test
        @DisplayName("should return zero when expenses exceed income")
        void shouldReturnZeroWhenExpensesExceedIncome() {
            TaxContext context = TaxContext.forEngland(
                2024, new BigDecimal("10000"), new BigDecimal("50000")
            );

            assertThat(context.getTaxableProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Region enum")
    class RegionEnum {

        @Test
        @DisplayName("Scotland should use Scottish rates")
        void scotlandShouldUseScottishRates() {
            assertThat(Region.SCOTLAND.usesScottishRates()).isTrue();
        }

        @Test
        @DisplayName("England should not use Scottish rates")
        void englandShouldNotUseScottishRates() {
            assertThat(Region.ENGLAND.usesScottishRates()).isFalse();
        }

        @Test
        @DisplayName("Wales should not use Scottish rates")
        void walesShouldNotUseScottishRates() {
            assertThat(Region.WALES.usesScottishRates()).isFalse();
        }

        @Test
        @DisplayName("Northern Ireland should not use Scottish rates")
        void northernIrelandShouldNotUseScottishRates() {
            assertThat(Region.NORTHERN_IRELAND.usesScottishRates()).isFalse();
        }

        @Test
        @DisplayName("should have display names")
        void shouldHaveDisplayNames() {
            assertThat(Region.ENGLAND.getDisplayName()).isEqualTo("England");
            assertThat(Region.SCOTLAND.getDisplayName()).isEqualTo("Scotland");
        }
    }
}
