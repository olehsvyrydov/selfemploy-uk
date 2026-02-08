package uk.selfemploy.plugin.extension;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for parsing a bank statement.
 *
 * <p>This is a plain-data alternative to the UI-layer ColumnMapping class,
 * suitable for use across the SPI boundary. All fields are nullable;
 * null means "auto-detect" for that configuration element.</p>
 *
 * <h2>Options Map</h2>
 * <p>The options map carries parser-specific configuration such as
 * separate income/expense column names for CSV parsers. Use the
 * {@code OPT_*} constants for well-known option keys.</p>
 *
 * @param dateFormat        date format pattern (e.g., "dd/MM/yyyy"), null for auto-detect
 * @param dateColumn        column name for date field, null for auto-detect
 * @param descriptionColumn column name for description field, null for auto-detect
 * @param amountColumn      column name for amount field, null for auto-detect
 * @param options           additional parser-specific options (never null after construction)
 *
 * @see BankStatementParser
 */
public record StatementParseRequest(
    String dateFormat,
    String dateColumn,
    String descriptionColumn,
    String amountColumn,
    Map<String, Object> options
) {

    /** Option key for whether income and expense are in separate columns. Type: Boolean. */
    public static final String OPT_SEPARATE_COLUMNS = "separateColumns";

    /** Option key for the income column name. Type: String. */
    public static final String OPT_INCOME_COLUMN = "incomeColumn";

    /** Option key for the expense column name. Type: String. */
    public static final String OPT_EXPENSE_COLUMN = "expenseColumn";

    /** Option key for the file path being parsed. Type: Path. */
    public static final String OPT_FILE_PATH = "filePath";

    /**
     * Compact constructor that makes the options map unmodifiable and defensively copied.
     */
    public StatementParseRequest {
        if (options == null) {
            options = Collections.emptyMap();
        } else {
            options = Collections.unmodifiableMap(new LinkedHashMap<>(options));
        }
    }

    /**
     * Creates a request with all auto-detect settings (all null).
     *
     * @return an auto-detect request
     */
    public static StatementParseRequest autoDetect() {
        return new StatementParseRequest(null, null, null, null, null);
    }

    /**
     * Returns an option value cast to the expected type.
     *
     * @param key          the option key
     * @param defaultValue the default value if option is not set
     * @param <T>          the option type
     * @return the option value or the default
     */
    @SuppressWarnings("unchecked")
    public <T> T getOption(String key, T defaultValue) {
        Object value = options.get(key);
        return value != null ? (T) value : defaultValue;
    }
}
