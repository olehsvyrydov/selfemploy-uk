package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import uk.selfemploy.core.config.NIClass2Rates;
import uk.selfemploy.core.config.RateBasis;
import uk.selfemploy.core.config.TaxRateConfiguration;
import uk.selfemploy.ui.i18n.Messages;
import uk.selfemploy.ui.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ViewModel for the Class 2 NI clarification card on the Tax Summary.
 *
 * <p>Explains what Class 2 National Insurance means for the user's State Pension record at their
 * profit level: above the Small Profits Threshold the year is treated as paid, below it Class 2 is
 * voluntary, and with no profit there is nothing to pay and a possible gap in the record.
 *
 * <p>Every figure the card shows — the threshold in the body text, the weekly rate, the annual
 * amount and the year in the rate label — is written by {@link #update(int, BigDecimal)} from the
 * rates of the tax year passed to it. There is no other writer and no default year: until
 * {@code update} is called the card is invisible and its rate fields are empty, so the card cannot
 * show one year's rates beside another year's profit.
 *
 * @see <a href="https://www.gov.uk/self-employed-national-insurance-rates">HMRC Class 2 NI rates</a>
 */
public class Class2NIClarificationViewModel {

    private static final int WEEKS_PER_YEAR = 52;

    // HMRC URLs
    private static final String STATE_PENSION_FORECAST_URL = "https://www.gov.uk/check-state-pension";
    private static final String NI_RECORD_URL = "https://www.gov.uk/check-national-insurance-record";
    private static final String VOLUNTARY_NI_URL = "https://www.gov.uk/voluntary-national-insurance-contributions";

    /** Rates of the year last passed to {@link #update(int, BigDecimal)}; null until then. */
    private NIClass2Rates rates;

    // === State Properties ===
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final BooleanProperty showVoluntaryBadge = new SimpleBooleanProperty(false);
    private final StringProperty titleText = new SimpleStringProperty(Messages.get("taxSummary.class2.title"));
    private final StringProperty bodyText = new SimpleStringProperty("");
    private final StringProperty pensionInsightText = new SimpleStringProperty("");
    private final ObjectProperty<Scenario> scenario = new SimpleObjectProperty<>(null);

    // === Rate Properties (year-specific; empty until a tax year is supplied) ===
    private final StringProperty weeklyRateLabel = new SimpleStringProperty("");
    private final StringProperty weeklyRateText = new SimpleStringProperty("");
    private final StringProperty annualAmountText = new SimpleStringProperty("");

    /**
     * Profit scenarios for Class 2 NI.
     */
    public enum Scenario {
        /** Profits at or above the Small Profits Threshold - the year is treated as paid. */
        ABOVE_SPT,
        /** Profits above zero but below the Small Profits Threshold - voluntary. */
        BELOW_SPT,
        /** Profits at or below zero - no payment required. */
        ZERO_LOSS
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

    // === Rate Properties ===

    /**
     * Property containing the weekly-rate row label, including the tax year it applies to.
     */
    public ReadOnlyStringProperty weeklyRateLabelProperty() {
        return weeklyRateLabel;
    }

    /**
     * Property containing the formatted weekly Class 2 rate for the tax year being viewed.
     */
    public ReadOnlyStringProperty weeklyRateTextProperty() {
        return weeklyRateText;
    }

    /**
     * Property containing the formatted 52-week amount for the tax year being viewed.
     */
    public ReadOnlyStringProperty annualAmountTextProperty() {
        return annualAmountText;
    }

    // === Actions ===

    /**
     * Rebuilds the card for one tax year and the profit shown for that same year.
     *
     * <p>The rates are re-read from {@link TaxRateConfiguration} for {@code taxYearStart} on every
     * call, and the scenario text and all three rate fields are written from them together. Passing
     * the year in rather than holding one is what keeps the card's figures and the profit beside it
     * describing the same year.
     *
     * <p>The rate label names the year the rates were <em>published</em> for, which past the last
     * rate file is an earlier year than the one being viewed. Labelling an older weekly rate with
     * the viewed year would present it as that year's rate — the precise thing the estimated-rates
     * banner exists to prevent.
     *
     * @param taxYearStart the tax year being viewed (e.g. 2025 for 2025/26)
     * @param profit       the net profit for that year (null is treated as zero)
     */
    public void update(int taxYearStart, BigDecimal profit) {
        TaxRateConfiguration config = TaxRateConfiguration.getInstance();
        this.rates = config.getNIClass2Rates(taxYearStart);
        RateBasis basis = config.rateBasisFor(taxYearStart);

        weeklyRateLabel.set(Messages.format("taxSummary.class2.weeklyRateLabel", basis.ratesYearLabel()));
        weeklyRateText.set(Money.format(rates.weeklyRate()));
        annualAmountText.set(Money.format(annualAmount()));

        BigDecimal effectiveProfit = profit != null ? profit : BigDecimal.ZERO;
        visible.set(true);

        if (effectiveProfit.compareTo(rates.smallProfitsThreshold()) >= 0) {
            applyAboveSPTState(taxYearStart);
        } else if (effectiveProfit.compareTo(BigDecimal.ZERO) > 0) {
            applyBelowSPTState();
        } else {
            applyZeroLossState();
        }
    }

    /**
     * Hides the card and clears every year-specific figure, for when there is no year to show.
     */
    public void clear() {
        rates = null;
        visible.set(false);
        showVoluntaryBadge.set(false);
        scenario.set(null);
        titleText.set(Messages.get("taxSummary.class2.title"));
        bodyText.set("");
        pensionInsightText.set("");
        weeklyRateLabel.set("");
        weeklyRateText.set("");
        annualAmountText.set("");
    }

    // === Rate Information ===

    /**
     * Returns the Class 2 NI weekly rate for the year last passed to
     * {@link #update(int, BigDecimal)}, or null if no year has been supplied.
     */
    public BigDecimal getWeeklyRate() {
        return rates != null ? rates.weeklyRate() : null;
    }

    /**
     * Returns the 52-week Class 2 amount for the year last passed to
     * {@link #update(int, BigDecimal)}, or null if no year has been supplied.
     */
    public BigDecimal getAnnualAmount() {
        return rates != null ? annualAmount() : null;
    }

    /**
     * Returns the Small Profits Threshold for the year last passed to
     * {@link #update(int, BigDecimal)}, or null if no year has been supplied.
     */
    public BigDecimal getSmallProfitsThreshold() {
        return rates != null ? rates.smallProfitsThreshold() : null;
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

    private BigDecimal annualAmount() {
        return rates.weeklyRate()
                .multiply(BigDecimal.valueOf(WEEKS_PER_YEAR))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static String taxYearLabel(int taxYearStart) {
        return String.format("%d/%02d", taxYearStart, (taxYearStart + 1) % 100);
    }

    private void applyAboveSPTState(int taxYearStart) {
        scenario.set(Scenario.ABOVE_SPT);
        showVoluntaryBadge.set(false);
        titleText.set(Messages.get("taxSummary.class2.title"));
        bodyText.set(Messages.format("taxSummary.class2.bodyTreatedAsPaid",
                Money.format(rates.smallProfitsThreshold()), taxYearLabel(taxYearStart)));
        pensionInsightText.set(Messages.format("taxSummary.class2.pensionTreatedAsPaid",
                taxYearLabel(taxYearStart)));
    }

    private void applyBelowSPTState() {
        scenario.set(Scenario.BELOW_SPT);
        showVoluntaryBadge.set(true);
        titleText.set(Messages.get("taxSummary.class2.titleVoluntary"));
        bodyText.set(Messages.format("taxSummary.class2.bodyBelowThreshold",
                Money.format(rates.smallProfitsThreshold())));
        pensionInsightText.set(Messages.format("taxSummary.class2.pensionVoluntary",
                Money.format(annualAmount())));
    }

    private void applyZeroLossState() {
        scenario.set(Scenario.ZERO_LOSS);
        showVoluntaryBadge.set(false);
        titleText.set(Messages.get("taxSummary.class2.title"));
        bodyText.set(Messages.get("taxSummary.class2.bodyZeroLoss"));
        pensionInsightText.set(Messages.get("taxSummary.class2.pensionZeroLoss"));
    }
}
