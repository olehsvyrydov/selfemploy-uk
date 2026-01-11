package uk.selfemploy.core.calculator;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Result of a Payments on Account (POA) calculation.
 *
 * <p>Payments on Account are advance payments towards your Self Assessment tax bill.
 * They apply when your tax liability exceeds £1,000 unless:
 * <ul>
 *   <li>It's your first year of self-employment</li>
 *   <li>More than 80% of your income is already taxed at source (PAYE)</li>
 * </ul>
 *
 * <p>POA amounts are based on the previous year's tax liability, with each of the two
 * payments being 50% of that amount.
 *
 * @param previousYearLiability The tax liability from the previous tax year
 * @param requiresPoa Whether POA is required for the next tax year
 * @param exemptionReason The reason POA is not required (null if POA is required)
 * @param firstPayment Amount due for first POA (31 January)
 * @param secondPayment Amount due for second POA (31 July)
 * @param firstPaymentDeadline Deadline for first POA (31 January following tax year end)
 * @param secondPaymentDeadline Deadline for second POA (31 July following tax year end)
 * @param taxYear The tax year these payments relate to
 */
public record PaymentsOnAccountResult(
    BigDecimal previousYearLiability,
    boolean requiresPoa,
    ExemptionReason exemptionReason,
    BigDecimal firstPayment,
    BigDecimal secondPayment,
    LocalDate firstPaymentDeadline,
    LocalDate secondPaymentDeadline,
    int taxYear
) {

    /**
     * Reasons why Payments on Account may not be required.
     */
    public enum ExemptionReason {
        /**
         * First year of self-employment - no previous year liability to base POA on.
         */
        FIRST_YEAR("First year of self-employment"),

        /**
         * More than 80% of income is already taxed at source (PAYE).
         */
        PAYE_EXCEEDS_80_PERCENT("More than 80% of income taxed at source"),

        /**
         * Previous year tax liability is £1,000 or less.
         */
        BELOW_THRESHOLD("Tax liability £1,000 or less");

        private final String description;

        ExemptionReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Returns the total of both POA payments.
     *
     * @return Sum of first and second payments
     */
    public BigDecimal totalPoaPayments() {
        return firstPayment.add(secondPayment);
    }

    /**
     * Returns a human-readable description of why POA is not required.
     *
     * @return Exemption reason description, or null if POA is required
     */
    public String exemptionDescription() {
        return exemptionReason != null ? exemptionReason.getDescription() : null;
    }

    /**
     * Creates a result indicating POA is not required.
     *
     * @param previousYearLiability The previous year's tax liability
     * @param reason Why POA is not required
     * @param taxYear The tax year
     * @return A new PaymentsOnAccountResult with zero payments
     */
    public static PaymentsOnAccountResult notRequired(
            BigDecimal previousYearLiability,
            ExemptionReason reason,
            int taxYear) {
        return new PaymentsOnAccountResult(
            previousYearLiability,
            false,
            reason,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            null,
            taxYear
        );
    }

    /**
     * Creates a result indicating POA is required.
     *
     * @param previousYearLiability The previous year's tax liability
     * @param firstPayment Amount for first POA
     * @param secondPayment Amount for second POA
     * @param firstDeadline Deadline for first payment
     * @param secondDeadline Deadline for second payment
     * @param taxYear The tax year
     * @return A new PaymentsOnAccountResult with calculated payments
     */
    public static PaymentsOnAccountResult required(
            BigDecimal previousYearLiability,
            BigDecimal firstPayment,
            BigDecimal secondPayment,
            LocalDate firstDeadline,
            LocalDate secondDeadline,
            int taxYear) {
        return new PaymentsOnAccountResult(
            previousYearLiability,
            true,
            null,
            firstPayment,
            secondPayment,
            firstDeadline,
            secondDeadline,
            taxYear
        );
    }
}
