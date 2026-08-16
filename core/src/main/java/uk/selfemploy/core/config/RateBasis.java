package uk.selfemploy.core.config;

/**
 * Which published tax year a figure's rates actually came from.
 *
 * <p>These are equal for any year the app ships a rate file for. They differ once the calendar
 * moves past the last rate file: the figures are still computed, but on an earlier year's rates,
 * and are therefore an estimate rather than that year's tax. Carrying the two together means a
 * caller holding a liability also holds the answer to "is this year's tax, or last year's rates?"
 *
 * @param taxYear   the tax year the figure was asked for (e.g. 2027 for 2027/28)
 * @param ratesYear the tax year whose published rates were used
 */
public record RateBasis(int taxYear, int ratesYear) {

    /**
     * Whether the figures were computed on a different year's rates than the one asked for.
     */
    public boolean isEstimated() {
        return taxYear != ratesYear;
    }

    /**
     * The rates year as a UK tax-year label, e.g. {@code "2026/27"}.
     */
    public String ratesYearLabel() {
        return String.format("%d/%02d", ratesYear, (ratesYear + 1) % 100);
    }

    /**
     * The requested year as a UK tax-year label, e.g. {@code "2027/28"}.
     */
    public String taxYearLabel() {
        return String.format("%d/%02d", taxYear, (taxYear + 1) % 100);
    }
}
