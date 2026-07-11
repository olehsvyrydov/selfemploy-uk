package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.CalculationResponse;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HmrcCalculationComparison")
class HmrcCalculationComparisonTest {

    private CalculationResponse hmrc(BigDecimal incomeTax, BigDecimal class2,
                                     BigDecimal class4, BigDecimal totalDue) {
        return new CalculationResponse(
            "calc-1", null, null,
            totalDue, null, null, null,
            new CalculationResponse.IncomeTaxBreakdown(incomeTax, incomeTax),
            new CalculationResponse.NationalInsuranceBreakdown(
                new CalculationResponse.NationalInsuranceBreakdown.Class2Nics(class2),
                new CalculationResponse.NationalInsuranceBreakdown.Class4Nics(null, class4)),
            null, null, null, null);
    }

    @Test
    @DisplayName("reports no mismatch when the app and HMRC agree within tolerance")
    void agrees() {
        HmrcCalculationComparison comparison = new HmrcCalculationComparison(
            new BigDecimal("3500.00"), new BigDecimal("179.40"), new BigDecimal("1752.70"),
            hmrc(new BigDecimal("3500.00"), new BigDecimal("179.40"), new BigDecimal("1752.70"),
                 new BigDecimal("5432.10")));

        assertThat(comparison.hasMismatch()).isFalse();
        assertThat(comparison.lines()).hasSize(4).allMatch(HmrcCalculationComparison.Line::matches);
    }

    @Test
    @DisplayName("a difference within a pound still agrees (rounding)")
    void withinTolerance() {
        HmrcCalculationComparison comparison = new HmrcCalculationComparison(
            new BigDecimal("3500.40"), new BigDecimal("179.40"), new BigDecimal("1752.70"),
            hmrc(new BigDecimal("3500.00"), new BigDecimal("179.40"), new BigDecimal("1752.70"),
                 new BigDecimal("5432.50")));

        assertThat(comparison.hasMismatch()).isFalse();
    }

    @Test
    @DisplayName("flags a real divergence on income tax")
    void mismatchOnIncomeTax() {
        HmrcCalculationComparison comparison = new HmrcCalculationComparison(
            new BigDecimal("3500.00"), new BigDecimal("179.40"), new BigDecimal("1752.70"),
            hmrc(new BigDecimal("3750.00"), new BigDecimal("179.40"), new BigDecimal("1752.70"),
                 new BigDecimal("5682.10")));

        assertThat(comparison.hasMismatch()).isTrue();
        HmrcCalculationComparison.Line incomeTaxLine = comparison.lines().get(0);
        assertThat(incomeTaxLine.label()).isEqualTo("Income Tax");
        assertThat(incomeTaxLine.matches()).isFalse();
        assertThat(incomeTaxLine.appValue()).isEqualByComparingTo("3500.00");
        assertThat(incomeTaxLine.hmrcValue()).isEqualByComparingTo("3750.00");
    }

    @Test
    @DisplayName("treats a missing HMRC figure as a mismatch (cannot confirm)")
    void missingHmrcFigure() {
        HmrcCalculationComparison comparison = new HmrcCalculationComparison(
            new BigDecimal("3500.00"), new BigDecimal("179.40"), new BigDecimal("1752.70"),
            null);

        assertThat(comparison.hasMismatch()).isTrue();
        assertThat(comparison.lines()).allMatch(line -> !line.matches());
    }

    @Test
    @DisplayName("computes the app total from its three components")
    void computesAppTotal() {
        HmrcCalculationComparison comparison = new HmrcCalculationComparison(
            new BigDecimal("3500.00"), new BigDecimal("179.40"), new BigDecimal("1752.70"),
            hmrc(new BigDecimal("3500.00"), new BigDecimal("179.40"), new BigDecimal("1752.70"),
                 new BigDecimal("5432.10")));

        HmrcCalculationComparison.Line total = comparison.lines().get(3);
        assertThat(total.label()).isEqualTo("Total due");
        assertThat(total.appValue()).isEqualByComparingTo("5432.10");
    }
}
