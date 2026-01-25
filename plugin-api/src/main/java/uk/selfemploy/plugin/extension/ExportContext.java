package uk.selfemploy.plugin.extension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Context information for data export operations.
 *
 * <p>This record provides the parameters and scope for exporting data to
 * various formats. It specifies what data should be included and any
 * formatting options.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ExportContext context = new ExportContext(
 *     2024,
 *     LocalDate.of(2024, 4, 6),
 *     LocalDate.of(2025, 4, 5),
 *     EnumSet.of(ExportContext.DataType.INCOME, ExportContext.DataType.EXPENSES),
 *     Map.of("includeHeaders", true)
 * );
 * byte[] csv = exporter.exportData(context, "CSV");
 * }</pre>
 *
 * @param taxYear   the tax year for the export
 * @param startDate the start date of the data range
 * @param endDate   the end date of the data range
 * @param dataTypes the types of data to include in the export
 * @param options   additional export options
 *
 * @see DataExporter
 */
public record ExportContext(
    int taxYear,
    LocalDate startDate,
    LocalDate endDate,
    Set<DataType> dataTypes,
    Map<String, Object> options
) {

    /**
     * Option key for including column headers.
     * <p>Type: Boolean, Default: true</p>
     */
    public static final String OPTION_INCLUDE_HEADERS = "includeHeaders";

    /**
     * Option key for date format pattern.
     * <p>Type: String, Default: "yyyy-MM-dd"</p>
     */
    public static final String OPTION_DATE_FORMAT = "dateFormat";

    /**
     * Option key for currency format.
     * <p>Type: String, Default: "GBP"</p>
     */
    public static final String OPTION_CURRENCY_FORMAT = "currencyFormat";

    /**
     * Types of data that can be exported.
     */
    public enum DataType {
        /** Income/revenue transactions */
        INCOME,
        /** Expense transactions */
        EXPENSES,
        /** Tax calculations and summaries */
        TAX_SUMMARY,
        /** Business mileage records */
        MILEAGE,
        /** All data types */
        ALL
    }

    /**
     * Constructs an ExportContext with validation.
     *
     * @param taxYear   the tax year for the export
     * @param startDate the start date, must not be null
     * @param endDate   the end date, must not be null
     * @param dataTypes the types of data to include
     * @param options   additional export options (may be null)
     * @throws IllegalArgumentException if dates are invalid
     */
    public ExportContext {
        Objects.requireNonNull(startDate, "Start date must not be null");
        Objects.requireNonNull(endDate, "End date must not be null");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        dataTypes = dataTypes == null || dataTypes.isEmpty()
            ? EnumSet.of(DataType.ALL)
            : EnumSet.copyOf(dataTypes);
        options = options == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(options);
    }

    /**
     * Creates an ExportContext for a full tax year with all data types.
     *
     * @param taxYear the tax year (e.g., 2024 for 2024/25)
     * @return a context covering the full tax year
     */
    public static ExportContext forTaxYear(int taxYear) {
        return new ExportContext(
            taxYear,
            LocalDate.of(taxYear, 4, 6),
            LocalDate.of(taxYear + 1, 4, 5),
            EnumSet.of(DataType.ALL),
            Collections.emptyMap()
        );
    }

    /**
     * Returns an option value cast to the expected type.
     *
     * @param key          the option key
     * @param defaultValue the default value if option is not set
     * @param <T>          the option type
     * @return the option value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getOption(String key, T defaultValue) {
        Object value = options.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Returns whether income data should be included.
     *
     * @return true if income should be exported
     */
    public boolean includeIncome() {
        return dataTypes.contains(DataType.ALL) || dataTypes.contains(DataType.INCOME);
    }

    /**
     * Returns whether expense data should be included.
     *
     * @return true if expenses should be exported
     */
    public boolean includeExpenses() {
        return dataTypes.contains(DataType.ALL) || dataTypes.contains(DataType.EXPENSES);
    }

    /**
     * Returns whether tax summary data should be included.
     *
     * @return true if tax summary should be exported
     */
    public boolean includeTaxSummary() {
        return dataTypes.contains(DataType.ALL) || dataTypes.contains(DataType.TAX_SUMMARY);
    }
}
