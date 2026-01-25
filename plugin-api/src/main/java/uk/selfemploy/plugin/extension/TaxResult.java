package uk.selfemploy.plugin.extension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of a tax calculation.
 *
 * <p>This record contains the calculated tax amounts broken down by type
 * (income tax, National Insurance) and band. It provides both summary
 * totals and detailed breakdowns for display purposes.</p>
 *
 * <h2>Tax Components</h2>
 * <ul>
 *   <li><b>Income Tax</b> - Tax on profits after personal allowance</li>
 *   <li><b>Class 4 NI</b> - National Insurance on self-employment profits</li>
 *   <li><b>Class 2 NI</b> - Flat-rate National Insurance (if applicable)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * TaxResult result = calculator.calculateTax(context);
 * System.out.println("Income Tax: " + result.incomeTax());
 * System.out.println("NI Class 4: " + result.niClass4());
 * System.out.println("Total: " + result.totalTaxDue());
 * }</pre>
 *
 * @param incomeTax         total income tax due
 * @param niClass4          Class 4 National Insurance due
 * @param niClass2          Class 2 National Insurance due (may be zero)
 * @param taxBandBreakdown  breakdown of income tax by band
 * @param metadata          additional calculation details
 *
 * @see TaxCalculatorExtension
 * @see TaxContext
 */
public record TaxResult(
    BigDecimal incomeTax,
    BigDecimal niClass4,
    BigDecimal niClass2,
    List<TaxBand> taxBandBreakdown,
    Map<String, Object> metadata
) {

    /**
     * Represents a tax band with its rate and calculated tax.
     *
     * @param bandName    the name of the tax band
     * @param rate        the tax rate as a percentage (e.g., 20 for 20%)
     * @param taxableAmount the amount taxed at this rate
     * @param taxAmount   the tax calculated for this band
     */
    public record TaxBand(
        String bandName,
        BigDecimal rate,
        BigDecimal taxableAmount,
        BigDecimal taxAmount
    ) {
        /**
         * Constructs a TaxBand with validation.
         */
        public TaxBand {
            Objects.requireNonNull(bandName, "Band name must not be null");
            Objects.requireNonNull(rate, "Rate must not be null");
            Objects.requireNonNull(taxableAmount, "Taxable amount must not be null");
            Objects.requireNonNull(taxAmount, "Tax amount must not be null");
        }
    }

    /**
     * Constructs a TaxResult with validation.
     *
     * @param incomeTax        total income tax, must not be null
     * @param niClass4         Class 4 NI, must not be null
     * @param niClass2         Class 2 NI, must not be null
     * @param taxBandBreakdown band breakdown (may be null)
     * @param metadata         additional data (may be null)
     */
    public TaxResult {
        Objects.requireNonNull(incomeTax, "Income tax must not be null");
        Objects.requireNonNull(niClass4, "NI Class 4 must not be null");
        Objects.requireNonNull(niClass2, "NI Class 2 must not be null");
        taxBandBreakdown = taxBandBreakdown == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(taxBandBreakdown);
        metadata = metadata == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(metadata);
    }

    /**
     * Creates a simple TaxResult without band breakdown.
     *
     * @param incomeTax total income tax
     * @param niClass4  Class 4 NI
     * @param niClass2  Class 2 NI
     * @return a simple tax result
     */
    public static TaxResult of(BigDecimal incomeTax, BigDecimal niClass4, BigDecimal niClass2) {
        return new TaxResult(incomeTax, niClass4, niClass2, null, null);
    }

    /**
     * Creates a TaxResult with zero tax.
     *
     * @return a zero tax result
     */
    public static TaxResult zero() {
        return new TaxResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }

    /**
     * Returns the total tax due (income tax + NI Class 4 + NI Class 2).
     *
     * @return total tax liability
     */
    public BigDecimal totalTaxDue() {
        return incomeTax.add(niClass4).add(niClass2);
    }

    /**
     * Returns the total National Insurance due (Class 4 + Class 2).
     *
     * @return total NI liability
     */
    public BigDecimal totalNationalInsurance() {
        return niClass4.add(niClass2);
    }

    /**
     * Returns a metadata value cast to the expected type.
     *
     * @param key          the metadata key
     * @param defaultValue the default value if not set
     * @param <T>          the value type
     * @return the value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }
}
