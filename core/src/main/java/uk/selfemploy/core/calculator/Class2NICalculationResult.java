package uk.selfemploy.core.calculator;

import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Result of a National Insurance Class 2 calculation.
 *
 * <p>Class 2 NI is a flat-rate contribution paid by self-employed individuals.
 * For 2025/26:
 * <ul>
 *   <li>Weekly rate: £3.50</li>
 *   <li>Annual amount: £182.00 (52 weeks)</li>
 *   <li>Small Profits Threshold: £6,845 (mandatory above, optional below)</li>
 * </ul>
 *
 * <p><strong>Null handling:</strong> The {@code grossProfit} field may be {@code null}
 * when the calculator receives a null input. In such cases, it is normalized to
 * {@link BigDecimal#ZERO}. Methods like {@link #effectiveRate()} handle null gracefully.
 *
 * @param grossProfit            The gross profit amount (may be null, treated as zero)
 * @param smallProfitsThreshold  The Small Profits Threshold for the tax year (never null)
 * @param weeklyRate             The weekly Class 2 NI rate (never null)
 * @param weeksLiable            Number of weeks liable for Class 2 NI
 * @param totalNI                Total Class 2 NI due (never null)
 * @param isMandatory            Whether Class 2 NI is mandatory (profits above threshold)
 * @param isVoluntary            Whether Class 2 NI is being paid voluntarily
 */
public record Class2NICalculationResult(
    @Nullable BigDecimal grossProfit,
    BigDecimal smallProfitsThreshold,
    BigDecimal weeklyRate,
    int weeksLiable,
    BigDecimal totalNI,
    boolean isMandatory,
    boolean isVoluntary
) {
    /**
     * Returns true if Class 2 NI is applicable (either mandatory or voluntary).
     *
     * @return {@code true} if total NI is greater than zero
     */
    public boolean isApplicable() {
        return totalNI.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calculates the effective NI rate as a percentage of gross profit.
     *
     * <p>Returns {@link BigDecimal#ZERO} if gross profit is null, zero, or negative,
     * as percentage calculation is not meaningful in these cases.
     *
     * @return the effective rate as a percentage (e.g., 1.82 for 1.82%), or zero
     */
    public BigDecimal effectiveRate() {
        if (grossProfit == null || grossProfit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalNI
            .multiply(new BigDecimal("100"))
            .divide(grossProfit, 2, RoundingMode.HALF_UP);
    }
}
