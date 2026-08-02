package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.core.profit.CategorySpend;
import uk.selfemploy.core.profit.ProfitTotals;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.calculator.TaxLiabilityCalculator;
import uk.selfemploy.core.calculator.TaxLiabilityResult;
import uk.selfemploy.core.calculator.TaxCalculationResult;
import uk.selfemploy.core.calculator.NICalculationResult;
import uk.selfemploy.core.calculator.Class2NICalculationResult;
import uk.selfemploy.ui.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * ViewModel for the Tax Summary view.
 * Provides complete tax breakdown with SA103 box mappings for HMRC submission.
 */
public class TaxSummaryViewModel {

    private static final String TURNOVER_BOX = "15";
    private static final String NET_PROFIT_BOX = "31";

    // === Core Financial Properties ===

    private final ObjectProperty<BigDecimal> turnover = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalExpenses = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> allowableExpenses = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> netProfit = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // === Tax Properties ===

    private final ObjectProperty<BigDecimal> incomeTax = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> niClass4 = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalTax = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // === Income Tax Breakdown ===

    private final ObjectProperty<BigDecimal> personalAllowance = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> taxableIncome = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> basicRateTax = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> higherRateTax = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> additionalRateTax = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // === NI Class 4 Breakdown ===

    private final ObjectProperty<BigDecimal> niMainRateAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> niMainRateTax = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> niAdditionalRateAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> niAdditionalRateTax = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // === NI Class 2 Properties ===

    private final ObjectProperty<BigDecimal> niClass2 = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> niClass2WeeklyRate = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty niClass2WeeksLiable = new SimpleIntegerProperty(0);
    private final BooleanProperty niClass2Mandatory = new SimpleBooleanProperty(false);
    private final BooleanProperty niClass2Voluntary = new SimpleBooleanProperty(false);
    private final ObjectProperty<BigDecimal> totalNI = new SimpleObjectProperty<>(BigDecimal.ZERO);

    // === Payment on Account ===

    private final ObjectProperty<BigDecimal> paymentOnAccountAmount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final BooleanProperty requiresPaymentOnAccount = new SimpleBooleanProperty(false);

    // === Status ===

    private final ObjectProperty<TaxYear> taxYear = new SimpleObjectProperty<>();
    private final BooleanProperty submitted = new SimpleBooleanProperty(false);

    // === Expense Breakdown ===

    /** What was spent per SA103 category and how much of it may be claimed. */
    private final ObservableMap<ExpenseCategory, CategorySpend> expenseBreakdown =
        FXCollections.observableMap(new EnumMap<>(ExpenseCategory.class));

    // === Calculation Results (cached) ===

    private TaxLiabilityResult lastCalculationResult;

    public TaxSummaryViewModel() {
        // Set up listeners to recalculate net profit when turnover or expenses change
        turnover.addListener((obs, oldVal, newVal) -> updateNetProfit());
        // Net profit is turnover minus ALLOWABLE expenses, so recompute when the
        // allowable total changes (it is set after the gross total in recalculateExpenseTotals).
        allowableExpenses.addListener((obs, oldVal, newVal) -> updateNetProfit());
    }

    // === Turnover (SA103 Box 15) ===

    public BigDecimal getTurnover() {
        return turnover.get();
    }

    public void setTurnover(BigDecimal value) {
        turnover.set(value != null ? value : BigDecimal.ZERO);
    }

    public ObjectProperty<BigDecimal> turnoverProperty() {
        return turnover;
    }

    public String getTurnoverBoxNumber() {
        return TURNOVER_BOX;
    }

    public String getFormattedTurnover() {
        return formatCurrency(getTurnover());
    }

    // === Total Expenses ===

    public BigDecimal getTotalExpenses() {
        return totalExpenses.get();
    }

    public void setTotalExpenses(BigDecimal value) {
        totalExpenses.set(value != null ? value : BigDecimal.ZERO);
    }

    public ObjectProperty<BigDecimal> totalExpensesProperty() {
        return totalExpenses;
    }

    public String getFormattedTotalExpenses() {
        return formatCurrency(getTotalExpenses());
    }

    // === Allowable Expenses ===

    public BigDecimal getAllowableExpenses() {
        return allowableExpenses.get();
    }

    public ObjectProperty<BigDecimal> allowableExpensesProperty() {
        return allowableExpenses;
    }

    // === Net Profit (SA103 Box 31) ===

    public BigDecimal getNetProfit() {
        return netProfit.get();
    }

    public void setNetProfit(BigDecimal value) {
        netProfit.set(value != null ? value : BigDecimal.ZERO);
    }

    public ObjectProperty<BigDecimal> netProfitProperty() {
        return netProfit;
    }

    public String getNetProfitBoxNumber() {
        return NET_PROFIT_BOX;
    }

    public String getFormattedNetProfit() {
        return formatCurrency(getNetProfit());
    }

    // === Income Tax ===

    public BigDecimal getIncomeTax() {
        return incomeTax.get();
    }

    public ObjectProperty<BigDecimal> incomeTaxProperty() {
        return incomeTax;
    }

    public String getFormattedIncomeTax() {
        return formatCurrency(getIncomeTax());
    }

    // === Income Tax Breakdown ===

    public BigDecimal getPersonalAllowance() {
        return personalAllowance.get();
    }

    public ObjectProperty<BigDecimal> personalAllowanceProperty() {
        return personalAllowance;
    }

    public BigDecimal getTaxableIncome() {
        return taxableIncome.get();
    }

    public ObjectProperty<BigDecimal> taxableIncomeProperty() {
        return taxableIncome;
    }

    public BigDecimal getBasicRateTax() {
        return basicRateTax.get();
    }

    public ObjectProperty<BigDecimal> basicRateTaxProperty() {
        return basicRateTax;
    }

    public BigDecimal getHigherRateTax() {
        return higherRateTax.get();
    }

    public ObjectProperty<BigDecimal> higherRateTaxProperty() {
        return higherRateTax;
    }

    public BigDecimal getAdditionalRateTax() {
        return additionalRateTax.get();
    }

    public ObjectProperty<BigDecimal> additionalRateTaxProperty() {
        return additionalRateTax;
    }

    // === NI Class 4 ===

    public BigDecimal getNiClass4() {
        return niClass4.get();
    }

    public ObjectProperty<BigDecimal> niClass4Property() {
        return niClass4;
    }

    public String getFormattedNiClass4() {
        return formatCurrency(getNiClass4());
    }

    // === NI Class 4 Breakdown ===

    public BigDecimal getNiMainRateAmount() {
        return niMainRateAmount.get();
    }

    public ObjectProperty<BigDecimal> niMainRateAmountProperty() {
        return niMainRateAmount;
    }

    public BigDecimal getNiMainRateTax() {
        return niMainRateTax.get();
    }

    public ObjectProperty<BigDecimal> niMainRateTaxProperty() {
        return niMainRateTax;
    }

    public BigDecimal getNiAdditionalRateAmount() {
        return niAdditionalRateAmount.get();
    }

    public ObjectProperty<BigDecimal> niAdditionalRateAmountProperty() {
        return niAdditionalRateAmount;
    }

    public BigDecimal getNiAdditionalRateTax() {
        return niAdditionalRateTax.get();
    }

    public ObjectProperty<BigDecimal> niAdditionalRateTaxProperty() {
        return niAdditionalRateTax;
    }

    // === NI Class 2 ===

    public BigDecimal getNiClass2() {
        return niClass2.get();
    }

    public ObjectProperty<BigDecimal> niClass2Property() {
        return niClass2;
    }

    public String getFormattedNiClass2() {
        return formatCurrency(getNiClass2());
    }

    public BigDecimal getNiClass2WeeklyRate() {
        return niClass2WeeklyRate.get();
    }

    public ObjectProperty<BigDecimal> niClass2WeeklyRateProperty() {
        return niClass2WeeklyRate;
    }

    public int getNiClass2WeeksLiable() {
        return niClass2WeeksLiable.get();
    }

    public IntegerProperty niClass2WeeksLiableProperty() {
        return niClass2WeeksLiable;
    }

    public boolean isNiClass2Mandatory() {
        return niClass2Mandatory.get();
    }

    public BooleanProperty niClass2MandatoryProperty() {
        return niClass2Mandatory;
    }

    public boolean isNiClass2Voluntary() {
        return niClass2Voluntary.get();
    }

    public BooleanProperty niClass2VoluntaryProperty() {
        return niClass2Voluntary;
    }

    // === Total NI (Class 2 + Class 4) ===

    public BigDecimal getTotalNI() {
        return totalNI.get();
    }

    public ObjectProperty<BigDecimal> totalNIProperty() {
        return totalNI;
    }

    public String getFormattedTotalNI() {
        return formatCurrency(getTotalNI());
    }

    // === Total Tax ===

    public BigDecimal getTotalTax() {
        return totalTax.get();
    }

    public ObjectProperty<BigDecimal> totalTaxProperty() {
        return totalTax;
    }

    public String getFormattedTotalTax() {
        return formatCurrency(getTotalTax());
    }

    // === Payment on Account ===

    public boolean requiresPaymentOnAccount() {
        return requiresPaymentOnAccount.get();
    }

    public BooleanProperty requiresPaymentOnAccountProperty() {
        return requiresPaymentOnAccount;
    }

    public BigDecimal getPaymentOnAccountAmount() {
        return paymentOnAccountAmount.get();
    }

    public ObjectProperty<BigDecimal> paymentOnAccountAmountProperty() {
        return paymentOnAccountAmount;
    }

    public String getFormattedPaymentOnAccount() {
        return formatCurrency(getPaymentOnAccountAmount());
    }

    public LocalDate getFirstPoaDueDate() {
        TaxYear year = getTaxYear();
        if (year == null) {
            return null;
        }
        // First POA is due 31 January following end of tax year
        return LocalDate.of(year.endDate().getYear() + 1, 1, 31);
    }

    public LocalDate getSecondPoaDueDate() {
        TaxYear year = getTaxYear();
        if (year == null) {
            return null;
        }
        // Second POA is due 31 July following end of tax year
        return LocalDate.of(year.endDate().getYear() + 1, 7, 31);
    }

    // === Tax Year ===

    public TaxYear getTaxYear() {
        return taxYear.get();
    }

    public void setTaxYear(TaxYear value) {
        taxYear.set(value);
    }

    public ObjectProperty<TaxYear> taxYearProperty() {
        return taxYear;
    }

    public String getTaxYearLabel() {
        TaxYear year = getTaxYear();
        if (year == null) {
            return "";
        }
        return year.label();
    }

    // === Draft Status ===

    public boolean isDraft() {
        return !submitted.get();
    }

    public boolean isSubmitted() {
        return submitted.get();
    }

    public void setSubmitted(boolean value) {
        submitted.set(value);
    }

    public BooleanProperty submittedProperty() {
        return submitted;
    }

    // === Expense Breakdown by Category ===

    /**
     * A copy of the breakdown, safe to hold and safe to ask for on a year with no records.
     *
     * <p>Built by {@code putAll} rather than the {@link EnumMap} copy constructor, which throws
     * {@code IllegalArgumentException} when handed an empty map that is not itself an EnumMap — as
     * this observable one is not. A year with no expenses is ordinary, not exceptional.
     */
    public Map<ExpenseCategory, CategorySpend> getExpenseBreakdown() {
        Map<ExpenseCategory, CategorySpend> copy = new EnumMap<>(ExpenseCategory.class);
        copy.putAll(expenseBreakdown);
        return copy;
    }

    public ObservableMap<ExpenseCategory, CategorySpend> expenseBreakdownProperty() {
        return expenseBreakdown;
    }

    /**
     * Adds what was spent in an SA103 category, claiming as much of it as the category allows.
     *
     * @param category The expense category
     * @param amount The expense amount to add
     */
    public void addExpenseByCategory(ExpenseCategory category, BigDecimal amount) {
        addExpenseByCategory(category, amount, category != null && category.isAllowable()
                ? amount : BigDecimal.ZERO);
    }

    /**
     * Adds what was spent in an SA103 category together with the part of it that may be claimed.
     *
     * <p>The two differ for a part-business expense, where the claim is a stated share of the amount.
     * The category alone cannot answer that, which is why the claimable figure is supplied rather than
     * derived: the breakdown reports the spend a return has to declare, and the claim is what reduces
     * profit.
     *
     * @param category the expense category
     * @param amount what was spent
     * @param claimable the part of it that may be claimed, which may be zero
     */
    public void addExpenseByCategory(ExpenseCategory category, BigDecimal amount, BigDecimal claimable) {
        if (category == null || amount == null) {
            return;
        }

        expenseBreakdown.put(category,
                expenseBreakdown.getOrDefault(category, CategorySpend.ZERO).plus(amount, claimable));

        recalculateExpenseTotals();
    }

    /**
     * Takes every figure from one derivation, rather than recomputing any of them here.
     *
     * <p>The totals are assigned from the record instead of being re-summed from its breakdown. The
     * two agree today, but only because two pieces of code happen to add the same numbers the same
     * way, and that is the arrangement four separate defects came from. Assigning them means there
     * is one answer.
     */
    public void setTotals(ProfitTotals totals) {
        ProfitTotals derived = totals == null ? ProfitTotals.EMPTY : totals;
        expenseBreakdown.clear();
        expenseBreakdown.putAll(derived.byCategory());
        totalExpenses.set(derived.grossSpend());
        turnover.set(derived.turnover());
        allowableExpenses.set(derived.allowableSpend());
        // Assigned last and explicitly. Both properties above carry a listener that recomputes the
        // profit, so during this method it is computed twice, once from a turnover that is current
        // against an allowable total that is not yet. Those intermediate values are never observed,
        // but leaving the final figure to a listener would mean Box 31 is the one number on the
        // return this method did not take from the derivation.
        netProfit.set(derived.netProfit());
    }

    /**
     * Replaces the breakdown with one already derived, category by category.
     *
     * <p>Takes the spend and the claim as a pair, so the caller has to have decided the claim before
     * calling. That is weaker than it looks: nothing here checks where the claim came from, and
     * {@link #addExpenseByCategory(ExpenseCategory, BigDecimal)} in this class still derives one from
     * the category alone. Prefer {@link #setTotals}, which takes a whole derivation.
     */
    public void setExpenseBreakdown(Map<ExpenseCategory, CategorySpend> breakdown) {
        expenseBreakdown.clear();
        if (breakdown != null) {
            expenseBreakdown.putAll(breakdown);
        }
        recalculateExpenseTotals();
    }

    /**
     * Clears all expense breakdown data.
     */
    public void clearExpenseBreakdown() {
        expenseBreakdown.clear();
        totalExpenses.set(BigDecimal.ZERO);
        allowableExpenses.set(BigDecimal.ZERO);
    }

    // === Tax Calculation ===

    /**
     * Calculates the full tax breakdown using the current turnover, expenses, and tax year.
     * Updates all tax-related properties.
     */
    public void calculateTax() {
        TaxYear year = getTaxYear();
        if (year == null) {
            resetTaxValues();
            return;
        }

        BigDecimal profit = getNetProfit();
        if (profit.compareTo(BigDecimal.ZERO) <= 0) {
            resetTaxValues();
            return;
        }

        try {
            TaxLiabilityCalculator calculator = new TaxLiabilityCalculator(year.startYear());
            TaxLiabilityResult result = calculator.calculate(profit);

            // Store result for reference
            this.lastCalculationResult = result;

            // Update Income Tax values
            TaxCalculationResult itResult = result.incomeTaxDetails();
            incomeTax.set(result.incomeTax());
            personalAllowance.set(itResult.personalAllowance());
            taxableIncome.set(itResult.taxableIncome());
            basicRateTax.set(itResult.basicRateTax());
            higherRateTax.set(itResult.higherRateTax());
            additionalRateTax.set(itResult.additionalRateTax());

            // Update NI Class 4 values
            NICalculationResult niClass4Result = result.niClass4Details();
            niClass4.set(result.niClass4());
            niMainRateAmount.set(niClass4Result.mainRateAmount());
            niMainRateTax.set(niClass4Result.mainRateNI());
            niAdditionalRateAmount.set(niClass4Result.additionalRateAmount());
            niAdditionalRateTax.set(niClass4Result.additionalRateNI());

            // Update NI Class 2 values
            Class2NICalculationResult niClass2Result = result.niClass2Details();
            niClass2.set(result.niClass2());
            niClass2WeeklyRate.set(niClass2Result.weeklyRate());
            niClass2WeeksLiable.set(niClass2Result.weeksLiable());
            niClass2Mandatory.set(niClass2Result.isMandatory());
            niClass2Voluntary.set(niClass2Result.isVoluntary());

            // Update total NI (Class 2 + Class 4)
            totalNI.set(result.totalNI());

            // Update totals
            totalTax.set(result.totalLiability());

            // Update Payment on Account
            requiresPaymentOnAccount.set(result.requiresPaymentOnAccount());
            paymentOnAccountAmount.set(result.paymentOnAccountAmount());

        } catch (Exception e) {
            // If calculation fails, reset values
            resetTaxValues();
        }
    }

    /**
     * Gets the last calculation result (for detailed reporting).
     *
     * @return The last TaxLiabilityResult or null if not calculated
     */
    public TaxLiabilityResult getLastCalculationResult() {
        return lastCalculationResult;
    }

    // === Private Helper Methods ===

    private void updateNetProfit() {
        BigDecimal income = getTurnover() != null ? getTurnover() : BigDecimal.ZERO;
        // Taxable net profit deducts only allowable expenses; disallowable ones
        // (e.g. business entertainment, depreciation) stay in the business but not the tax base.
        BigDecimal allowable = getAllowableExpenses() != null ? getAllowableExpenses() : BigDecimal.ZERO;
        netProfit.set(income.subtract(allowable));
    }

    private void recalculateExpenseTotals() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal allowable = BigDecimal.ZERO;
        for (CategorySpend spend : expenseBreakdown.values()) {
            total = total.add(spend.spent());
            allowable = allowable.add(spend.claimable());
        }

        totalExpenses.set(total);
        allowableExpenses.set(allowable);
    }

    private void resetTaxValues() {
        incomeTax.set(BigDecimal.ZERO);
        niClass4.set(BigDecimal.ZERO);
        totalTax.set(BigDecimal.ZERO);
        personalAllowance.set(BigDecimal.ZERO);
        taxableIncome.set(BigDecimal.ZERO);
        basicRateTax.set(BigDecimal.ZERO);
        higherRateTax.set(BigDecimal.ZERO);
        additionalRateTax.set(BigDecimal.ZERO);
        niMainRateAmount.set(BigDecimal.ZERO);
        niMainRateTax.set(BigDecimal.ZERO);
        niAdditionalRateAmount.set(BigDecimal.ZERO);
        niAdditionalRateTax.set(BigDecimal.ZERO);
        // Reset Class 2 NI values
        niClass2.set(BigDecimal.ZERO);
        niClass2WeeklyRate.set(BigDecimal.ZERO);
        niClass2WeeksLiable.set(0);
        niClass2Mandatory.set(false);
        niClass2Voluntary.set(false);
        totalNI.set(BigDecimal.ZERO);
        // Reset POA
        requiresPaymentOnAccount.set(false);
        paymentOnAccountAmount.set(BigDecimal.ZERO);
        lastCalculationResult = null;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return Money.format(amount);
    }
}
