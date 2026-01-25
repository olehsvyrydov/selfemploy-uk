package uk.selfemploy.plugin.extension;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Context information for data import operations.
 *
 * <p>This record provides the parameters and options for importing transaction
 * data from external sources such as bank statements or CSV files.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ImportContext context = new ImportContext(
 *     2024,
 *     Map.of(
 *         "skipDuplicates", true,
 *         "defaultCategory", "general"
 *     )
 * );
 * ImportResult result = importer.importData(file, context);
 * }</pre>
 *
 * @param targetTaxYear the tax year to import transactions into
 * @param options       additional import options
 *
 * @see DataImporter
 */
public record ImportContext(
    int targetTaxYear,
    Map<String, Object> options
) {

    /**
     * Option key for skipping duplicate transactions.
     * <p>Type: Boolean, Default: true</p>
     */
    public static final String OPTION_SKIP_DUPLICATES = "skipDuplicates";

    /**
     * Option key for the default expense category.
     * <p>Type: String</p>
     */
    public static final String OPTION_DEFAULT_CATEGORY = "defaultCategory";

    /**
     * Option key for marking transactions as reviewed.
     * <p>Type: Boolean, Default: false</p>
     */
    public static final String OPTION_MARK_REVIEWED = "markReviewed";

    /**
     * Constructs an ImportContext with validation.
     *
     * @param targetTaxYear the tax year to import transactions into
     * @param options       additional import options (may be null)
     */
    public ImportContext {
        options = options == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(options);
    }

    /**
     * Creates an ImportContext with default options for a tax year.
     *
     * @param taxYear the target tax year
     * @return a context with default options
     */
    public static ImportContext forTaxYear(int taxYear) {
        return new ImportContext(taxYear, Collections.emptyMap());
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
     * Returns whether duplicate transactions should be skipped.
     *
     * @return true if duplicates should be skipped (default)
     */
    public boolean skipDuplicates() {
        return getOption(OPTION_SKIP_DUPLICATES, true);
    }
}
