package uk.selfemploy.core.calculator;

import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Result of a National Insurance Class 2 calculation.
 *
 * <p>Class 2 NI is a flat-rate weekly contribution for self-employed individuals. From
 * 2024/25 onwards profits at or above the Small Profits Threshold are <em>treated as</em>
 * having paid it, so nothing is due; below the threshold it may still be paid voluntarily.
 * For tax years before 2024/25 it was mandatory above the threshold.
 *
 * <p>Nothing due is therefore two different facts, which {@link #isTreatedAsPaid()}
 * separates: a qualifying year earned without payment, or no qualifying year at all.
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
 * @param isMandatory            Whether Class 2 NI must be paid (only possible before 2024/25)
 * @param isVoluntary            Whether Class 2 NI is being paid voluntarily
 * @param isTreatedAsPaid        Whether the year counts as paid without payment (above the
 *                               threshold, 2024/25 onwards)
 */
public record Class2NICalculationResult(
    @Nullable BigDecimal grossProfit,
    BigDecimal smallProfitsThreshold,
    BigDecimal weeklyRate,
    int weeksLiable,
    BigDecimal totalNI,
    boolean isMandatory,
    boolean isVoluntary,
    boolean isTreatedAsPaid
) {
    /**
     * Returns true if there is a Class 2 payment to make. False both below the threshold with no
     * voluntary payment and above it, where the year is treated as paid — see
     * {@link #isTreatedAsPaid()} to tell those apart.
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
