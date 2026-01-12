package uk.selfemploy.core.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration loader for externalized tax rates.
 *
 * Loads tax rates from YAML configuration files in the tax-rates/ resource directory.
 * Falls back to hardcoded rates if YAML files are not found (graceful degradation).
 *
 * File naming convention: {year}-{year+1 mod 100}.yaml (e.g., 2024-25.yaml)
 *
 * Thread-safe singleton with caching of loaded rates.
 */
public class TaxRateConfiguration {

    private static final Logger LOGGER = Logger.getLogger(TaxRateConfiguration.class.getName());
    private static final String TAX_RATES_PATH = "tax-rates/";

    private static volatile TaxRateConfiguration instance;

    // Caches for loaded rates
    private final Map<Integer, IncomeTaxRates> incomeTaxRatesCache = new ConcurrentHashMap<>();
    private final Map<Integer, NIClass4Rates> niClass4RatesCache = new ConcurrentHashMap<>();
    private final Map<Integer, NIClass2Rates> niClass2RatesCache = new ConcurrentHashMap<>();

    // List of supported tax years (loaded from available YAML files)
    private final List<Integer> supportedTaxYears = new ArrayList<>();

    private TaxRateConfiguration() {
        // Discover available tax years on initialization
        discoverSupportedTaxYears();
    }

    /**
     * Returns the singleton instance of TaxRateConfiguration.
     * Thread-safe using double-checked locking.
     */
    public static TaxRateConfiguration getInstance() {
        if (instance == null) {
            synchronized (TaxRateConfiguration.class) {
                if (instance == null) {
                    instance = new TaxRateConfiguration();
                }
            }
        }
        return instance;
    }

    /**
     * Gets Income Tax rates for the specified tax year.
     * Returns cached rates if available, otherwise loads from YAML.
     * Falls back to default rates if YAML is not found.
     *
     * @param taxYear The tax year (e.g., 2024 for 2024/25)
     * @return IncomeTaxRates for the specified year
     */
    public IncomeTaxRates getIncomeTaxRates(int taxYear) {
        return incomeTaxRatesCache.computeIfAbsent(taxYear, this::loadIncomeTaxRates);
    }

    /**
     * Gets NI Class 4 rates for the specified tax year.
     * Returns cached rates if available, otherwise loads from YAML.
     * Falls back to default rates if YAML is not found.
     *
     * @param taxYear The tax year (e.g., 2024 for 2024/25)
     * @return NIClass4Rates for the specified year
     */
    public NIClass4Rates getNIClass4Rates(int taxYear) {
        return niClass4RatesCache.computeIfAbsent(taxYear, this::loadNIClass4Rates);
    }

    /**
     * Gets NI Class 2 rates for the specified tax year.
     * Returns cached rates if available, otherwise loads from YAML.
     * Falls back to default rates if YAML is not found.
     *
     * @param taxYear The tax year (e.g., 2024 for 2024/25)
     * @return NIClass2Rates for the specified year
     */
    public NIClass2Rates getNIClass2Rates(int taxYear) {
        return niClass2RatesCache.computeIfAbsent(taxYear, this::loadNIClass2Rates);
    }

    /**
     * Returns the list of supported tax years (those with YAML configuration files).
     *
     * @return List of supported tax years
     */
    public List<Integer> getSupportedTaxYears() {
        return List.copyOf(supportedTaxYears);
    }

    /**
     * Checks if a specific tax year is supported (has a YAML configuration file).
     *
     * @param taxYear The tax year to check
     * @return true if the tax year is supported, false otherwise
     */
    public boolean isTaxYearSupported(int taxYear) {
        return supportedTaxYears.contains(taxYear);
    }

    /**
     * Clears the cache - useful for testing.
     */
    void clearCache() {
        incomeTaxRatesCache.clear();
        niClass4RatesCache.clear();
        niClass2RatesCache.clear();
    }

    /**
     * Discovers available tax years by checking for YAML files.
     */
    private void discoverSupportedTaxYears() {
        // Check for known tax years (2024, 2025, etc.)
        for (int year = 2024; year <= 2030; year++) {
            String filename = getYamlFilename(year);
            if (getClass().getClassLoader().getResource(TAX_RATES_PATH + filename) != null) {
                supportedTaxYears.add(year);
                final int foundYear = year;
                LOGGER.fine(() -> "Found tax rates for year: " + foundYear);
            }
        }
    }

    /**
     * Loads Income Tax rates from YAML for the specified tax year.
     * Falls back to default rates if YAML is not found.
     */
    private IncomeTaxRates loadIncomeTaxRates(int taxYear) {
        Map<String, Object> taxData = loadYamlForYear(taxYear);

        if (taxData == null || !taxData.containsKey("incomeTax")) {
            LOGGER.warning(() -> "No income tax rates found for year " + taxYear + ", using fallback");
            return IncomeTaxRates.defaultRates();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> incomeTax = (Map<String, Object>) taxData.get("incomeTax");

        return new IncomeTaxRates(
            toBigDecimal(incomeTax.get("personalAllowance")),
            toBigDecimal(incomeTax.get("basicRateUpperLimit")),
            toBigDecimal(incomeTax.get("higherRateUpperLimit")),
            toBigDecimal(incomeTax.get("taperThreshold")),
            toBigDecimal(incomeTax.get("basicRate")),
            toBigDecimal(incomeTax.get("higherRate")),
            toBigDecimal(incomeTax.get("additionalRate"))
        );
    }

    /**
     * Loads NI Class 4 rates from YAML for the specified tax year.
     * Falls back to default rates if YAML is not found.
     */
    private NIClass4Rates loadNIClass4Rates(int taxYear) {
        Map<String, Object> taxData = loadYamlForYear(taxYear);

        if (taxData == null || !taxData.containsKey("niClass4")) {
            LOGGER.warning(() -> "No NI Class 4 rates found for year " + taxYear + ", using fallback");
            return NIClass4Rates.defaultRates();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> niClass4 = (Map<String, Object>) taxData.get("niClass4");

        return new NIClass4Rates(
            toBigDecimal(niClass4.get("lowerProfitsLimit")),
            toBigDecimal(niClass4.get("upperProfitsLimit")),
            toBigDecimal(niClass4.get("mainRate")),
            toBigDecimal(niClass4.get("additionalRate"))
        );
    }

    /**
     * Loads NI Class 2 rates from YAML for the specified tax year.
     * Falls back to default rates if YAML is not found.
     */
    private NIClass2Rates loadNIClass2Rates(int taxYear) {
        Map<String, Object> taxData = loadYamlForYear(taxYear);

        if (taxData == null || !taxData.containsKey("niClass2")) {
            LOGGER.warning(() -> "No NI Class 2 rates found for year " + taxYear + ", using fallback");
            return NIClass2Rates.defaultRates();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> niClass2 = (Map<String, Object>) taxData.get("niClass2");

        return new NIClass2Rates(
            toBigDecimal(niClass2.get("weeklyRate")),
            toBigDecimal(niClass2.get("smallProfitsThreshold"))
        );
    }

    /**
     * Loads YAML data for the specified tax year.
     *
     * @param taxYear The tax year
     * @return Map of YAML data, or null if file not found
     */
    private Map<String, Object> loadYamlForYear(int taxYear) {
        String filename = getYamlFilename(taxYear);
        String resourcePath = TAX_RATES_PATH + filename;

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                LOGGER.fine(() -> "YAML file not found: " + resourcePath);
                return null;
            }

            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = yaml.load(inputStream);
            LOGGER.fine(() -> "Loaded tax rates from: " + resourcePath);
            return data;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading YAML file: " + resourcePath, e);
            return null;
        }
    }

    /**
     * Generates the YAML filename for a given tax year.
     * Format: {year}-{(year+1) mod 100}.yaml (e.g., 2024-25.yaml)
     *
     * @param taxYear The tax year
     * @return The YAML filename
     */
    private String getYamlFilename(int taxYear) {
        int nextYearShort = (taxYear + 1) % 100;
        return String.format("%d-%02d.yaml", taxYear, nextYearShort);
    }

    /**
     * Converts various number types to BigDecimal.
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Integer) {
            return BigDecimal.valueOf((Integer) value);
        }
        if (value instanceof Long) {
            return BigDecimal.valueOf((Long) value);
        }
        if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value);
        }
        return new BigDecimal(value.toString());
    }

    /**
     * Resets the singleton instance (for testing only).
     */
    static void resetInstance() {
        instance = null;
    }
}
