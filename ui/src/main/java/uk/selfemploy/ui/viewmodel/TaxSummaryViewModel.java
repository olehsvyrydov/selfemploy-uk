package uk.selfemploy.ui.viewmodel;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * ViewModel for the Tax Summary view.
 * Provides complete tax breakdown with SA103 box mappings for HMRC submission.
 */
public class TaxSummaryViewModel {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);
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

    private final ObservableMap<ExpenseCategory, BigDecimal> expenseBreakdown =
        FXCollections.observableMap(new EnumMap<>(ExpenseCategory.class));

    // === Calculation Results (cached) ===

    private TaxLiabilityResult lastCalculationResult;

    public TaxSummaryViewModel() {
        // Set up listeners to recalculate net profit when turnover or expenses change
        turnover.addListener((obs, oldVal, newVal) -> updateNetProfit());
        totalExpenses.addListener((obs, oldVal, newVal) -> updateNetProfit());
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

    public Map<ExpenseCategory, BigDecimal> getExpenseBreakdown() {
        return new EnumMap<>(expenseBreakdown);
    }

    public ObservableMap<ExpenseCategory, BigDecimal> expenseBreakdownProperty() {
        return expenseBreakdown;
    }

    /**
     * Adds an expense amount to a specific SA103 category.
     * Updates both the category breakdown and total expenses.
     *
     * @param category The expense category
     * @param amount The expense amount to add
     */
    public void addExpenseByCategory(ExpenseCategory category, BigDecimal amount) {
        if (category == null || amount == null) {
            return;
        }

        BigDecimal current = expenseBreakdown.getOrDefault(category, BigDecimal.ZERO);
        expenseBreakdown.put(category, current.add(amount));

        // Recalculate totals
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

    /**
     * Sets the expense breakdown from a map (replaces existing data).
     *
     * @param breakdown Map of expense categories to amounts
     */
    public void setExpenseBreakdown(Map<ExpenseCategory, BigDecimal> breakdown) {
        expenseBreakdown.clear();
        if (breakdown != null) {
            expenseBreakdown.putAll(breakdown);
        }
        recalculateExpenseTotals();
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
        BigDecimal expenses = getTotalExpenses() != null ? getTotalExpenses() : BigDecimal.ZERO;
        netProfit.set(income.subtract(expenses));
    }

    private void recalculateExpenseTotals() {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal allowable = BigDecimal.ZERO;

        for (Map.Entry<ExpenseCategory, BigDecimal> entry : expenseBreakdown.entrySet()) {
            BigDecimal amount = entry.getValue();
            total = total.add(amount);

            if (entry.getKey().isAllowable()) {
                allowable = allowable.add(amount);
            }
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
        return CURRENCY_FORMAT.format(amount);
    }
}
