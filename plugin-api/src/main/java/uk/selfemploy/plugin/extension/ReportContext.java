package uk.selfemploy.plugin.extension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Context information for report generation.
 *
 * <p>This record provides the parameters and scope for generating reports.
 * It is passed to {@link ReportGenerator#generateReport(ReportContext, String)}
 * to specify what data should be included in the report.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ReportContext context = new ReportContext(
 *     2024,
 *     LocalDate.of(2024, 4, 6),
 *     LocalDate.of(2025, 4, 5),
 *     Map.of("includeCharts", true)
 * );
 * byte[] pdf = reportGenerator.generateReport(context, "PDF");
 * }</pre>
 *
 * @param taxYear    the tax year for the report (e.g., 2024 for 2024/25)
 * @param startDate  the start date of the reporting period
 * @param endDate    the end date of the reporting period
 * @param parameters additional parameters for report customization
 *
 * @see ReportGenerator
 */
public record ReportContext(
    int taxYear,
    LocalDate startDate,
    LocalDate endDate,
    Map<String, Object> parameters
) {

    /**
     * Constructs a ReportContext with validation.
     *
     * @param taxYear    the tax year for the report
     * @param startDate  the start date of the reporting period, must not be null
     * @param endDate    the end date of the reporting period, must not be null
     * @param parameters additional parameters (may be null, will be converted to empty map)
     * @throws IllegalArgumentException if dates are null or invalid
     */
    public ReportContext {
        Objects.requireNonNull(startDate, "Start date must not be null");
        Objects.requireNonNull(endDate, "End date must not be null");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        parameters = parameters == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(parameters);
    }

    /**
     * Creates a ReportContext for a full tax year.
     *
     * <p>UK tax years run from 6 April to 5 April of the following year.</p>
     *
     * @param taxYear the tax year (e.g., 2024 for 2024/25)
     * @return a context covering the full tax year
     */
    public static ReportContext forTaxYear(int taxYear) {
        return new ReportContext(
            taxYear,
            LocalDate.of(taxYear, 4, 6),
            LocalDate.of(taxYear + 1, 4, 5),
            Collections.emptyMap()
        );
    }

    /**
     * Returns a parameter value cast to the expected type.
     *
     * @param key          the parameter key
     * @param defaultValue the default value if parameter is not set
     * @param <T>          the parameter type
     * @return the parameter value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        Object value = parameters.get(key);
        return value != null ? (T) value : defaultValue;
    }
}
