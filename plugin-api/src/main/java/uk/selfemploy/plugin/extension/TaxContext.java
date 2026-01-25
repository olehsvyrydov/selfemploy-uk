package uk.selfemploy.plugin.extension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Context information for tax calculations.
 *
 * <p>This record provides all the input data needed for calculating tax
 * liability. It includes the financial figures, the tax year, and the
 * taxpayer's region (which affects income tax rates in Scotland).</p>
 *
 * <h2>Regional Variations</h2>
 * <p>The UK has different income tax rates for Scotland vs rest of UK:</p>
 * <ul>
 *   <li>{@code ENGLAND}, {@code WALES}, {@code NORTHERN_IRELAND} - UK rates</li>
 *   <li>{@code SCOTLAND} - Scottish rates with additional bands</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * TaxContext context = new TaxContext(
 *     2024,
 *     new BigDecimal("50000"),
 *     new BigDecimal("10000"),
 *     TaxContext.Region.SCOTLAND,
 *     Collections.emptyMap()
 * );
 * TaxResult result = calculator.calculateTax(context);
 * }</pre>
 *
 * @param taxYear       the tax year (e.g., 2024 for 2024/25)
 * @param grossIncome   total income before expenses
 * @param totalExpenses total allowable expenses
 * @param region        the taxpayer's region
 * @param additionalData additional data for specialized calculations
 *
 * @see TaxCalculatorExtension
 * @see TaxResult
 */
public record TaxContext(
    int taxYear,
    BigDecimal grossIncome,
    BigDecimal totalExpenses,
    Region region,
    Map<String, Object> additionalData
) {

    /**
     * UK regions for tax calculation purposes.
     */
    public enum Region {
        /** England uses UK income tax rates */
        ENGLAND("England"),

        /** Wales uses UK income tax rates */
        WALES("Wales"),

        /** Scotland has its own income tax rates */
        SCOTLAND("Scotland"),

        /** Northern Ireland uses UK income tax rates */
        NORTHERN_IRELAND("Northern Ireland");

        private final String displayName;

        Region(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Returns the display name for this region.
         *
         * @return the display name
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Returns whether this region uses Scottish income tax rates.
         *
         * @return true if Scottish rates apply
         */
        public boolean usesScottishRates() {
            return this == SCOTLAND;
        }
    }

    /**
     * Constructs a TaxContext with validation.
     *
     * @param taxYear       the tax year
     * @param grossIncome   total income, must not be null
     * @param totalExpenses total expenses, must not be null
     * @param region        the region, must not be null
     * @param additionalData additional data (may be null)
     * @throws NullPointerException if required parameters are null
     * @throws IllegalArgumentException if values are negative
     */
    public TaxContext {
        Objects.requireNonNull(grossIncome, "Gross income must not be null");
        Objects.requireNonNull(totalExpenses, "Total expenses must not be null");
        Objects.requireNonNull(region, "Region must not be null");
        if (grossIncome.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Gross income must not be negative");
        }
        if (totalExpenses.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total expenses must not be negative");
        }
        additionalData = additionalData == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(additionalData);
    }

    /**
     * Creates a basic TaxContext for England with default values.
     *
     * @param taxYear       the tax year
     * @param grossIncome   total income
     * @param totalExpenses total expenses
     * @return a context for England
     */
    public static TaxContext forEngland(int taxYear, BigDecimal grossIncome, BigDecimal totalExpenses) {
        return new TaxContext(taxYear, grossIncome, totalExpenses, Region.ENGLAND, null);
    }

    /**
     * Creates a basic TaxContext for Scotland with default values.
     *
     * @param taxYear       the tax year
     * @param grossIncome   total income
     * @param totalExpenses total expenses
     * @return a context for Scotland
     */
    public static TaxContext forScotland(int taxYear, BigDecimal grossIncome, BigDecimal totalExpenses) {
        return new TaxContext(taxYear, grossIncome, totalExpenses, Region.SCOTLAND, null);
    }

    /**
     * Calculates the taxable profit (gross income minus expenses).
     *
     * @return the taxable profit (never negative)
     */
    public BigDecimal getTaxableProfit() {
        BigDecimal profit = grossIncome.subtract(totalExpenses);
        return profit.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : profit;
    }

    /**
     * Returns an additional data value cast to the expected type.
     *
     * @param key          the data key
     * @param defaultValue the default value if not set
     * @param <T>          the value type
     * @return the value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, T defaultValue) {
        Object value = additionalData.get(key);
        return value != null ? (T) value : defaultValue;
    }
}
