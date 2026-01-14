package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import uk.selfemploy.core.config.NIClass2Rates;
import uk.selfemploy.core.config.TaxRateConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * ViewModel for the Class 2 NI Credit Clarification UI.
 *
 * <p>SE-810: Provides educational content about Class 2 National Insurance
 * and its importance for State Pension credits. Content varies based on
 * the user's profit level:</p>
 *
 * <ul>
 *   <li><b>Above SPT (£6,725):</b> Must pay Class 2, entitled message</li>
 *   <li><b>Below SPT:</b> Voluntary payment option</li>
 *   <li><b>Zero/Loss:</b> No payment required, gap warning</li>
 * </ul>
 *
 * <h3>2025/26 Rates:</h3>
 * <ul>
 *   <li>Weekly rate: £3.45</li>
 *   <li>Annual amount (52 weeks): £179.40</li>
 *   <li>Small Profits Threshold: £6,725</li>
 * </ul>
 *
 * @see <a href="https://www.gov.uk/self-employed-national-insurance-rates">HMRC Class 2 NI rates</a>
 */
public class Class2NIClarificationViewModel {

    // Default tax year (2025/26)
    private static final int DEFAULT_TAX_YEAR = 2025;
    private static final int WEEKS_PER_YEAR = 52;

    // Tax year for this instance
    private final int taxYear;

    // Rates loaded from TaxRateConfiguration
    private final NIClass2Rates rates;

    // HMRC URLs
    private static final String STATE_PENSION_FORECAST_URL = "https://www.gov.uk/check-state-pension";
    private static final String NI_RECORD_URL = "https://www.gov.uk/check-national-insurance-record";
    private static final String VOLUNTARY_NI_URL = "https://www.gov.uk/voluntary-national-insurance-contributions";

    // Content for different scenarios
    private static final String TITLE_STANDARD = "Class 2 NI Credits";
    private static final String TITLE_VOLUNTARY = "Class 2 NI Credits (Voluntary)";

    // Note: Body text references SPT dynamically in applyAboveSPTState() and applyBelowSPTState()
    private static final String BODY_ABOVE_SPT_TEMPLATE =
            "Class 2 National Insurance helps you qualify for State Pension. " +
                    "As your profits exceed %s, you're entitled to pay Class 2 contributions.";

    private static final String BODY_BELOW_SPT_TEMPLATE =
            "Your profits are below the Small Profits Threshold (%s), so " +
                    "Class 2 NI is voluntary. However, you may still want to pay to " +
                    "protect your State Pension entitlement.";

    private static final String BODY_ZERO_LOSS =
            "You have no profits this year, so you don't need to pay Class 2 NI. " +
                    "However, gaps in your National Insurance record can affect your State Pension.";

    private static final String PENSION_INSIGHT_STANDARD =
            "Each year you pay Class 2 NI counts as a 'qualifying year' for State Pension. " +
                    "You need 35 qualifying years for the full State Pension.";

    private static final String PENSION_INSIGHT_VOLUNTARY_TEMPLATE =
            "Paying voluntarily ensures this year counts toward your 35 qualifying years " +
                    "for full State Pension. At just %s per year, this can be valuable protection.";

    private static final String PENSION_INSIGHT_ZERO_LOSS =
            "Each year without Class 2 contributions may create a gap in your NI record. " +
                    "Consider checking your qualifying year count for State Pension.";

    // Currency formatter
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    // === State Properties ===
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final BooleanProperty showVoluntaryBadge = new SimpleBooleanProperty(false);
    private final StringProperty titleText = new SimpleStringProperty(TITLE_STANDARD);
    private final StringProperty bodyText = new SimpleStringProperty("");
    private final StringProperty pensionInsightText = new SimpleStringProperty(PENSION_INSIGHT_STANDARD);
    private final ObjectProperty<Scenario> scenario = new SimpleObjectProperty<>(null);

    /**
     * Profit scenarios for Class 2 NI.
     */
    public enum Scenario {
        /** Profits >= £6,725 - must pay Class 2 */
        ABOVE_SPT,
        /** Profits > £0 but < £6,725 - voluntary */
        BELOW_SPT,
        /** Profits <= £0 - no payment required */
        ZERO_LOSS
    }

    /**
     * Creates a new Class2NIClarificationViewModel with default tax year (2025/26).
     */
    public Class2NIClarificationViewModel() {
        this(DEFAULT_TAX_YEAR);
    }

    /**
     * Creates a new Class2NIClarificationViewModel for a specific tax year.
     *
     * @param taxYear the tax year (e.g., 2025 for 2025/26)
     */
    public Class2NIClarificationViewModel(int taxYear) {
        this.taxYear = taxYear;
        this.rates = TaxRateConfiguration.getInstance().getNIClass2Rates(taxYear);
    }

    // === State Properties ===

    /**
     * Property indicating whether the Class 2 NI card should be visible.
     */
    public BooleanProperty visibleProperty() {
        return visible;
    }

    /**
     * Property indicating whether the "VOLUNTARY" badge should be shown.
     */
    public BooleanProperty showVoluntaryBadgeProperty() {
        return showVoluntaryBadge;
    }

    /**
     * Property containing the title text.
     */
    public StringProperty titleTextProperty() {
        return titleText;
    }

    /**
     * Property containing the body text explaining the user's situation.
     */
    public StringProperty bodyTextProperty() {
        return bodyText;
    }

    /**
     * Property containing the pension insight text.
     */
    public StringProperty pensionInsightTextProperty() {
        return pensionInsightText;
    }

    /**
     * Property containing the current scenario.
     */
    public ObjectProperty<Scenario> scenarioProperty() {
        return scenario;
    }

    // === Actions ===

    /**
     * Updates the ViewModel based on the user's profit.
     *
     * @param profit the net profit amount (can be null for zero)
     */
    public void updateForProfit(BigDecimal profit) {
        BigDecimal effectiveProfit = profit != null ? profit : BigDecimal.ZERO;

        visible.set(true);

        if (effectiveProfit.compareTo(rates.smallProfitsThreshold()) >= 0) {
            // Above or at SPT - must pay
            applyAboveSPTState();
        } else if (effectiveProfit.compareTo(BigDecimal.ZERO) > 0) {
            // Below SPT but positive - voluntary
            applyBelowSPTState();
        } else {
            // Zero or loss - no payment required
            applyZeroLossState();
        }
    }

    // === Rate Information ===

    /**
     * Returns the Class 2 NI weekly rate from TaxRateConfiguration.
     */
    public BigDecimal getWeeklyRate() {
        return rates.weeklyRate();
    }

    /**
     * Returns the Class 2 NI annual amount (52 weeks).
     * Calculated from weekly rate.
     */
    public BigDecimal getAnnualAmount() {
        return rates.weeklyRate()
                .multiply(BigDecimal.valueOf(WEEKS_PER_YEAR))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the formatted weekly rate (e.g., "£3.45").
     */
    public String getFormattedWeeklyRate() {
        return CURRENCY_FORMAT.format(rates.weeklyRate());
    }

    /**
     * Returns the formatted annual amount (e.g., "£179.40").
     */
    public String getFormattedAnnualAmount() {
        return CURRENCY_FORMAT.format(getAnnualAmount());
    }

    /**
     * Returns the Small Profits Threshold from TaxRateConfiguration.
     */
    public BigDecimal getSmallProfitsThreshold() {
        return rates.smallProfitsThreshold();
    }

    /**
     * Returns the current tax year formatted as "YYYY/YY".
     */
    public String getTaxYear() {
        return String.format("%d/%02d", taxYear, (taxYear + 1) % 100);
    }

    /**
     * Returns the weekly rate label including tax year.
     */
    public String getWeeklyRateLabel() {
        return "Weekly rate (" + getTaxYear() + "):";
    }

    // === External Links ===

    /**
     * Returns the URL for checking State Pension forecast.
     */
    public String getStatePensionForecastUrl() {
        return STATE_PENSION_FORECAST_URL;
    }

    /**
     * Returns the URL for checking National Insurance record.
     */
    public String getNiRecordUrl() {
        return NI_RECORD_URL;
    }

    /**
     * Returns the URL for voluntary NI contributions guidance.
     */
    public String getVoluntaryNiGuidanceUrl() {
        return VOLUNTARY_NI_URL;
    }

    // === Private Methods ===

    private void applyAboveSPTState() {
        scenario.set(Scenario.ABOVE_SPT);
        showVoluntaryBadge.set(false);
        titleText.set(TITLE_STANDARD);
        bodyText.set(String.format(BODY_ABOVE_SPT_TEMPLATE, getFormattedSmallProfitsThreshold()));
        pensionInsightText.set(PENSION_INSIGHT_STANDARD);
    }

    private void applyBelowSPTState() {
        scenario.set(Scenario.BELOW_SPT);
        showVoluntaryBadge.set(true);
        titleText.set(TITLE_VOLUNTARY);
        bodyText.set(String.format(BODY_BELOW_SPT_TEMPLATE, getFormattedSmallProfitsThreshold()));
        pensionInsightText.set(String.format(PENSION_INSIGHT_VOLUNTARY_TEMPLATE, getFormattedAnnualAmount()));
    }

    /**
     * Returns the formatted Small Profits Threshold (e.g., "£6,845").
     */
    private String getFormattedSmallProfitsThreshold() {
        return CURRENCY_FORMAT.format(rates.smallProfitsThreshold());
    }

    private void applyZeroLossState() {
        scenario.set(Scenario.ZERO_LOSS);
        showVoluntaryBadge.set(false);
        titleText.set(TITLE_STANDARD);
        bodyText.set(BODY_ZERO_LOSS);
        pensionInsightText.set(PENSION_INSIGHT_ZERO_LOSS);
    }
}
