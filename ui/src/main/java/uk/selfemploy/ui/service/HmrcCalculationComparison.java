package uk.selfemploy.ui.service;

import uk.selfemploy.hmrc.client.dto.CalculationResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares the app's own tax estimate against the figures HMRC returned for the
 * same year, line by line, and flags any divergence. Showing both numbers side by
 * side — and highlighting where they differ — is a deliberate check on the app's
 * own calculator before a taxpayer declares HMRC's figures as final.
 */
public final class HmrcCalculationComparison {

    /** Amounts within this many pounds of each other are treated as agreeing. */
    private static final BigDecimal TOLERANCE = new BigDecimal("1.00");

    /** One compared figure: the app's value, HMRC's value, and whether they agree. */
    public record Line(String label, BigDecimal appValue, BigDecimal hmrcValue, boolean matches) {
    }

    private final List<Line> lines;

    /**
     * @param appIncomeTax the app's estimated income tax
     * @param appClass2Nic the app's estimated Class 2 NIC
     * @param appClass4Nic the app's estimated Class 4 NIC
     * @param hmrc         HMRC's returned calculation (may have null nested fields)
     */
    public HmrcCalculationComparison(BigDecimal appIncomeTax, BigDecimal appClass2Nic,
                                     BigDecimal appClass4Nic, CalculationResponse hmrc) {
        BigDecimal appTotal = nz(appIncomeTax).add(nz(appClass2Nic)).add(nz(appClass4Nic));

        this.lines = List.of(
            line("Income Tax", appIncomeTax, hmrcIncomeTax(hmrc)),
            line("Class 2 NIC", appClass2Nic, hmrcClass2(hmrc)),
            line("Class 4 NIC", appClass4Nic, hmrcClass4(hmrc)),
            line("Total due", appTotal, hmrc == null ? null : hmrc.totalIncomeTaxAndNicsDue())
        );
    }

    public List<Line> lines() {
        return new ArrayList<>(lines);
    }

    /** True if any line's app value and HMRC value disagree beyond the tolerance. */
    public boolean hasMismatch() {
        return lines.stream().anyMatch(line -> !line.matches());
    }

    private static Line line(String label, BigDecimal appValue, BigDecimal hmrcValue) {
        return new Line(label, appValue, hmrcValue, agrees(appValue, hmrcValue));
    }

    private static boolean agrees(BigDecimal appValue, BigDecimal hmrcValue) {
        if (hmrcValue == null) {
            // HMRC did not return this figure, so agreement cannot be confirmed.
            return false;
        }
        return nz(appValue).subtract(hmrcValue).abs().compareTo(TOLERANCE) <= 0;
    }

    private static BigDecimal hmrcIncomeTax(CalculationResponse hmrc) {
        if (hmrc == null || hmrc.incomeTax() == null) {
            return null;
        }
        return hmrc.incomeTax().totalIncomeTax();
    }

    private static BigDecimal hmrcClass2(CalculationResponse hmrc) {
        if (hmrc == null || hmrc.nics() == null || hmrc.nics().class2Nics() == null) {
            return null;
        }
        return hmrc.nics().class2Nics().amount();
    }

    private static BigDecimal hmrcClass4(CalculationResponse hmrc) {
        if (hmrc == null || hmrc.nics() == null || hmrc.nics().class4Nics() == null) {
            return null;
        }
        return hmrc.nics().class4Nics().totalClass4Nics();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
