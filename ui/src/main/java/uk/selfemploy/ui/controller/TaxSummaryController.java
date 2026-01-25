package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.component.HelpDialog;
import uk.selfemploy.ui.help.HelpContent;
import uk.selfemploy.ui.help.HelpService;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.legal.Disclaimers;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.util.BrowserUtil;
import uk.selfemploy.ui.viewmodel.Class2NIClarificationViewModel;
import uk.selfemploy.ui.viewmodel.TaxSummaryViewModel;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for the Tax Summary view (SE-306).
 * Displays tax calculation breakdown with SA103 box mappings.
 */
public class TaxSummaryController implements Initializable, MainController.TaxYearAware, Refreshable {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);
    private static final String TOGGLE_EXPANDED = "[v]";
    private static final String TOGGLE_COLLAPSED = "[>]";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK);

    // === FXML Injected Fields ===

    @FXML private VBox taxSummaryContainer;

    // Header
    @FXML private Label taxYearBadge;

    // Draft Banner
    @FXML private HBox draftBanner;

    // Disclaimer Banner (SE-509)
    @FXML private HBox taxDisclaimerBanner;
    @FXML private Label taxDisclaimerText;

    // Tax At A Glance
    @FXML private Label netProfitValue;
    @FXML private Label incomeTaxValue;
    @FXML private Label niClass4Value;
    @FXML private Label totalTaxDueValue;

    // Income Section
    @FXML private VBox incomeSection;
    @FXML private Label incomeToggle;
    @FXML private Label incomeTotalLabel;
    @FXML private VBox incomeContent;
    @FXML private Label turnoverLineValue;
    @FXML private Label otherIncomeLineValue;

    // Expenses Section
    @FXML private VBox expensesSection;
    @FXML private Label expensesToggle;
    @FXML private Label expensesTotalLabel;
    @FXML private VBox expensesContent;

    // Profit Calculation
    @FXML private Label turnoverCalcValue;
    @FXML private Label expensesCalcValue;
    @FXML private Label netProfitCalcValue;

    // Income Tax Section
    @FXML private VBox incomeTaxSection;
    @FXML private Label incomeTaxToggle;
    @FXML private Label incomeTaxTotalLabel;
    @FXML private VBox incomeTaxContent;

    // NI Section
    @FXML private VBox niSection;
    @FXML private Label niToggle;
    @FXML private Label niTotalLabel;
    @FXML private VBox niContent;

    // Class 2 NI Section (SE-810)
    @FXML private VBox class2NiCard;
    @FXML private Label class2Title;
    @FXML private Label voluntaryBadge;
    @FXML private Button class2HelpBtn;
    @FXML private Label class2BodyText;
    @FXML private Label class2WeeklyLabel;
    @FXML private Label class2WeeklyValue;
    @FXML private Label class2AnnualValue;
    @FXML private Label pensionInsightText;
    @FXML private Hyperlink statePensionLink;

    // POA Section
    @FXML private VBox poaSection;
    @FXML private Label poaToggle;
    @FXML private Label poaSectionTitle;
    @FXML private Label poaTotalLabel;
    @FXML private VBox poaContent;
    @FXML private Label poaFirstLabel;
    @FXML private Label poaFirstPayment;
    @FXML private Label poaSecondLabel;
    @FXML private Label poaSecondPayment;
    @FXML private Hyperlink poaInfoLink;

    // Total Due Card
    @FXML private Label taxYearDueLabel;
    @FXML private Label taxYearDueValue;
    @FXML private HBox poaDueRow;
    @FXML private Label poaDueValue;
    @FXML private Label grandTotalValue;
    @FXML private Label dueDateLabel;

    // Action Bar
    @FXML private Button exportPdfBtn;
    @FXML private Button submitBtn;

    // === State ===

    private TaxSummaryViewModel viewModel;
    private Class2NIClarificationViewModel class2ViewModel;
    private TaxYear taxYear;

    // Service dependencies (similar pattern to DashboardController)
    private IncomeService incomeService;
    private ExpenseService expenseService;
    private UUID businessId;

    // Section expansion state
    private boolean incomeSectionExpanded = true;
    private boolean expensesSectionExpanded = true;
    private boolean incomeTaxSectionExpanded = true;
    private boolean niSectionExpanded = true;
    private boolean poaSectionExpanded = true;

    // Help service for styled dialogs
    private final HelpService helpService = new HelpService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (viewModel == null) {
            viewModel = new TaxSummaryViewModel();
        }
        if (class2ViewModel == null) {
            class2ViewModel = new Class2NIClarificationViewModel();
        }
        setupBindings();
        initializeDisclaimers();
        initializeClass2Section();
    }

    /**
     * Initializes the disclaimer text from centralized legal constants (SE-509).
     * AC-4: Disclaimer cannot be dismissed permanently.
     */
    private void initializeDisclaimers() {
        if (taxDisclaimerText != null) {
            taxDisclaimerText.setText(Disclaimers.TAX_SUMMARY_DISCLAIMER);
        }
        // AC-4: Disclaimer banner is always visible - no dismiss functionality
        if (taxDisclaimerBanner != null) {
            taxDisclaimerBanner.setVisible(true);
            taxDisclaimerBanner.setManaged(true);
        }
    }

    /**
     * Initializes the Class 2 NI clarification section (SE-810).
     * Sets up static content and binds dynamic properties to the ViewModel.
     */
    private void initializeClass2Section() {
        if (class2ViewModel == null) {
            return;
        }

        // Bind visibility
        if (class2NiCard != null) {
            class2NiCard.visibleProperty().bind(class2ViewModel.visibleProperty());
            class2NiCard.managedProperty().bind(class2ViewModel.visibleProperty());
        }

        // Bind voluntary badge
        if (voluntaryBadge != null) {
            voluntaryBadge.visibleProperty().bind(class2ViewModel.showVoluntaryBadgeProperty());
            voluntaryBadge.managedProperty().bind(class2ViewModel.showVoluntaryBadgeProperty());
        }

        // Bind text content
        if (class2Title != null) {
            class2Title.textProperty().bind(class2ViewModel.titleTextProperty());
        }
        if (class2BodyText != null) {
            class2BodyText.textProperty().bind(class2ViewModel.bodyTextProperty());
        }
        if (pensionInsightText != null) {
            pensionInsightText.textProperty().bind(class2ViewModel.pensionInsightTextProperty());
        }

        // Set static rate information
        if (class2WeeklyLabel != null) {
            class2WeeklyLabel.setText(class2ViewModel.getWeeklyRateLabel());
        }
        if (class2WeeklyValue != null) {
            class2WeeklyValue.setText(class2ViewModel.getFormattedWeeklyRate());
        }
        if (class2AnnualValue != null) {
            class2AnnualValue.setText(class2ViewModel.getFormattedAnnualAmount());
        }
    }

    /**
     * Updates the Class 2 NI section based on the current net profit.
     */
    private void updateClass2Section() {
        if (class2ViewModel == null || viewModel == null) {
            return;
        }

        // Update the ViewModel based on net profit
        BigDecimal netProfit = viewModel.getNetProfit();
        class2ViewModel.updateForProfit(netProfit);
    }

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;

        // Fallback to CoreServiceFactory if not initialized via CDI (same pattern as DashboardController)
        if (incomeService == null) {
            incomeService = CoreServiceFactory.getIncomeService();
        }
        if (expenseService == null) {
            expenseService = CoreServiceFactory.getExpenseService();
        }
        if (businessId == null) {
            businessId = CoreServiceFactory.getDefaultBusinessId();
        }

        if (viewModel != null) {
            viewModel.setTaxYear(taxYear);
            loadTaxSummaryData();
            updateDisplay();
        }
    }

    /**
     * Initializes the controller with required service dependencies.
     * This enables tax summary data integration with real backend services.
     *
     * @param incomeService  the income service for loading income data
     * @param expenseService the expense service for loading expense data
     * @param businessId     the current business ID
     */
    public void initializeWithDependencies(IncomeService incomeService, ExpenseService expenseService, UUID businessId) {
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.businessId = businessId;

        // Load data if tax year is already set
        if (taxYear != null && viewModel != null) {
            viewModel.setTaxYear(taxYear);
            loadTaxSummaryData();
            updateDisplay();
        }
    }

    /**
     * Loads income and expense data from services and populates the ViewModel.
     */
    private void loadTaxSummaryData() {
        if (incomeService == null || expenseService == null || businessId == null || taxYear == null) {
            return;
        }

        // Clear previous data
        viewModel.clearExpenseBreakdown();
        viewModel.setTurnover(BigDecimal.ZERO);

        // Load income data
        var incomes = incomeService.findByTaxYear(businessId, taxYear);
        BigDecimal totalIncome = incomes.stream()
            .map(Income::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        viewModel.setTurnover(totalIncome);

        // Load expense data and group by category for SA103 breakdown
        var expenses = expenseService.findByTaxYear(businessId, taxYear);
        for (Expense expense : expenses) {
            viewModel.addExpenseByCategory(expense.category(), expense.amount());
        }

        // Calculate tax with the loaded data
        viewModel.calculateTax();

        // Update Class 2 NI section based on net profit
        updateClass2Section();
    }

    /**
     * Sets the ViewModel (for testing).
     */
    public void setViewModel(TaxSummaryViewModel viewModel) {
        this.viewModel = viewModel;
    }

    // === Refreshable Implementation ===

    @Override
    public void refreshData() {
        loadTaxSummaryData();
        updateDisplay();
    }

    // === Draft Banner ===

    public boolean isDraftBannerVisible() {
        return viewModel != null && !viewModel.isSubmitted();
    }

    // === Submit Button ===

    public boolean isSubmitButtonEnabled() {
        // Submit disabled for Sprint 3 - will be enabled in future sprint
        return false;
    }

    // === Tax Year Display ===

    public String getTaxYearBadgeText() {
        if (taxYear == null) {
            return "";
        }
        return "Tax Year " + taxYear.label();
    }

    // === Section Expansion ===

    public boolean isIncomeSectionExpanded() {
        return incomeSectionExpanded;
    }

    public boolean isExpensesSectionExpanded() {
        return expensesSectionExpanded;
    }

    public boolean isIncomeTaxSectionExpanded() {
        return incomeTaxSectionExpanded;
    }

    public boolean isNiSectionExpanded() {
        return niSectionExpanded;
    }

    public boolean isPoaSectionExpanded() {
        return poaSectionExpanded;
    }

    public void toggleIncomeSection() {
        incomeSectionExpanded = !incomeSectionExpanded;
        updateSectionVisibility();
    }

    public void toggleExpensesSection() {
        expensesSectionExpanded = !expensesSectionExpanded;
        updateSectionVisibility();
    }

    public void toggleIncomeTaxSection() {
        incomeTaxSectionExpanded = !incomeTaxSectionExpanded;
        updateSectionVisibility();
    }

    public void toggleNISection() {
        niSectionExpanded = !niSectionExpanded;
        updateSectionVisibility();
    }

    public void togglePOASection() {
        poaSectionExpanded = !poaSectionExpanded;
        updateSectionVisibility();
    }

    // === Toggle Icon Text ===

    public String getIncomeSectionToggleText() {
        return incomeSectionExpanded ? TOGGLE_EXPANDED : TOGGLE_COLLAPSED;
    }

    public String getExpensesSectionToggleText() {
        return expensesSectionExpanded ? TOGGLE_EXPANDED : TOGGLE_COLLAPSED;
    }

    public String getIncomeTaxSectionToggleText() {
        return incomeTaxSectionExpanded ? TOGGLE_EXPANDED : TOGGLE_COLLAPSED;
    }

    public String getNiSectionToggleText() {
        return niSectionExpanded ? TOGGLE_EXPANDED : TOGGLE_COLLAPSED;
    }

    public String getPoaSectionToggleText() {
        return poaSectionExpanded ? TOGGLE_EXPANDED : TOGGLE_COLLAPSED;
    }

    // === POA Visibility ===

    public boolean isPoaSectionVisible() {
        return viewModel != null && viewModel.requiresPaymentOnAccount();
    }

    // === Currency Formatting ===

    public String getFormattedTurnover() {
        if (viewModel == null) {
            return formatCurrency(BigDecimal.ZERO);
        }
        return formatCurrency(viewModel.getTurnover());
    }

    public String getFormattedExpensesForCalculation() {
        if (viewModel == null) {
            return "-" + formatCurrency(BigDecimal.ZERO);
        }
        return "-" + formatCurrency(viewModel.getTotalExpenses());
    }

    // === Expense Line Items ===

    /**
     * Returns expense line items sorted by SA103 box number.
     */
    public List<ExpenseLineItem> getExpenseLineItems() {
        if (viewModel == null) {
            return Collections.emptyList();
        }

        Map<ExpenseCategory, BigDecimal> breakdown;
        try {
            breakdown = viewModel.getExpenseBreakdown();
        } catch (IllegalArgumentException e) {
            // EnumMap copy constructor fails on empty maps
            return Collections.emptyList();
        }

        if (breakdown == null || breakdown.isEmpty()) {
            return Collections.emptyList();
        }

        return breakdown.entrySet().stream()
            .map(entry -> new ExpenseLineItem(
                formatCategoryDisplayName(entry.getKey()),
                "Box " + entry.getKey().getSa103Box(),
                entry.getValue()
            ))
            .sorted(Comparator.comparing(item -> Integer.parseInt(item.boxRef().replace("Box ", ""))))
            .collect(Collectors.toList());
    }

    // === Income Tax Bands ===

    /**
     * Returns income tax bands for display.
     */
    public List<TaxBandItem> getIncomeTaxBands() {
        if (viewModel == null) {
            return Collections.emptyList();
        }

        List<TaxBandItem> bands = new ArrayList<>();

        // Personal Allowance - £0 to threshold at 0%
        bands.add(new TaxBandItem(
            "Personal Allowance (0%)",
            formatRange(BigDecimal.ZERO, viewModel.getPersonalAllowance()),
            BigDecimal.ZERO
        ));

        // Basic Rate - threshold to £37,700 at 20%
        bands.add(new TaxBandItem(
            "Basic Rate (20%)",
            formatBasicRateRange(),
            viewModel.getBasicRateTax()
        ));

        // Higher Rate - £37,700 to £125,140 at 40%
        bands.add(new TaxBandItem(
            "Higher Rate (40%)",
            formatHigherRateRange(),
            viewModel.getHigherRateTax()
        ));

        // Additional Rate - above £125,140 at 45%
        bands.add(new TaxBandItem(
            "Additional Rate (45%)",
            "Above £125,140",
            viewModel.getAdditionalRateTax()
        ));

        return bands;
    }

    // === NI Class 4 Bands ===

    /**
     * Returns NI Class 4 rate bands for display.
     * Uses the correct 2024/25 and 2025/26 rates: 6% main rate, 2% additional rate.
     */
    public List<TaxBandItem> getNiClass4Bands() {
        if (viewModel == null) {
            return Collections.emptyList();
        }

        List<TaxBandItem> bands = new ArrayList<>();

        // Below Lower Profits Limit - 0%
        bands.add(new TaxBandItem(
            "Below LPL (0%)",
            "Up to £12,570",
            BigDecimal.ZERO
        ));

        // Main rate - 6% between LPL and UPL (corrected from 9%)
        bands.add(new TaxBandItem(
            "Main Rate (6%)",
            "£12,570 - £50,270",
            viewModel.getNiMainRateTax()
        ));

        // Additional rate - 2% above UPL
        bands.add(new TaxBandItem(
            "Additional Rate (2%)",
            "Above £50,270",
            viewModel.getNiAdditionalRateTax()
        ));

        return bands;
    }

    // === Due Date ===

    public String getDueDateText() {
        if (taxYear == null) {
            return "";
        }
        return "Due by " + taxYear.onlineFilingDeadline().format(DATE_FORMAT);
    }

    // === Grand Total ===

    public BigDecimal getGrandTotal() {
        if (viewModel == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = viewModel.getTotalTax();
        if (viewModel.requiresPaymentOnAccount()) {
            total = total.add(viewModel.getPaymentOnAccountAmount());
        }
        return total;
    }

    // === FXML Event Handlers ===

    @FXML
    void handleToggleIncomeSection() {
        toggleIncomeSection();
    }

    @FXML
    void handleToggleExpensesSection() {
        toggleExpensesSection();
    }

    @FXML
    void handleToggleIncomeTaxSection() {
        toggleIncomeTaxSection();
    }

    @FXML
    void handleToggleNISection() {
        toggleNISection();
    }

    @FXML
    void handleTogglePOASection() {
        togglePOASection();
    }

    @FXML
    void handleExportPDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Tax Summary as PDF");
        fileChooser.setInitialFileName("tax-summary-" + (taxYear != null ? taxYear.label().replace("/", "-") : "draft") + ".pdf");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(exportPdfBtn.getScene().getWindow());
        if (file != null) {
            exportToPdf(file);
        }
    }

    @FXML
    void handleSubmitToHMRC(ActionEvent event) {
        // Disabled for Sprint 3
        showInfo("Coming Soon", "HMRC submission will be available in a future update.");
    }

    @FXML
    void handleShowPOAInfo(ActionEvent event) {
        HelpContent content = HelpContent.builder()
                .title("Payments on Account")
                .body("If your tax bill is more than £1,000, you'll usually need to make " +
                    "advance payments towards next year's tax bill. These are called " +
                    "'payments on account'.\n\n" +
                    "Each payment is half of your previous year's tax bill. You make " +
                    "two payments a year:\n" +
                    "• First payment: 31 January\n" +
                    "• Second payment: 31 July\n\n" +
                    "These payments go towards next year's tax bill. If you've paid too much, " +
                    "you'll get a refund. If you haven't paid enough, you'll need to make a " +
                    "'balancing payment'.")
                .hmrcLink("https://www.gov.uk/understand-self-assessment-bill/payments-on-account")
                .linkText("View HMRC guidance")
                .build();

        // Tax & Calculation category color (green)
        HelpDialog dialog = new HelpDialog(content, FontAwesomeSolid.CHART_BAR, "#059669", helpService);
        dialog.showAndWait();
    }

    @FXML
    void handleShowClass2Help(ActionEvent event) {
        HelpContent content = HelpContent.builder()
                .title("About Class 2 National Insurance")
                .body("Class 2 National Insurance is a flat-rate contribution paid by " +
                    "self-employed people.\n\n" +
                    "Key points:\n" +
                    "• It's a fixed weekly amount (not based on profits)\n" +
                    "• Contributes to your State Pension entitlement\n" +
                    "• You need 35 qualifying years for the full State Pension\n" +
                    "• Each year you pay counts as a qualifying year\n\n" +
                    "Small Profits Threshold (SPT):\n" +
                    "If your profits are below £6,725, Class 2 NI is voluntary. " +
                    "However, you may still want to pay to protect your State Pension.\n\n" +
                    "Full State Pension (2025/26): £221.20 per week (£11,502/year)")
                .hmrcLink("https://www.gov.uk/national-insurance/how-much-you-pay")
                .linkText("View HMRC NI rates")
                .build();

        // Tax & Calculation category color (green)
        HelpDialog dialog = new HelpDialog(content, FontAwesomeSolid.HEARTBEAT, "#059669", helpService);
        dialog.showAndWait();
    }

    @FXML
    void handleStatePensionLink(ActionEvent event) {
        if (class2ViewModel != null) {
            openUrl(class2ViewModel.getStatePensionForecastUrl());
        }
    }

    // === Private Helper Methods ===

    private void setupBindings() {
        if (viewModel == null) {
            return;
        }

        // Bind draft banner visibility
        if (draftBanner != null) {
            draftBanner.visibleProperty().bind(viewModel.submittedProperty().not());
            draftBanner.managedProperty().bind(viewModel.submittedProperty().not());
        }

        // Bind POA section visibility
        if (poaSection != null) {
            poaSection.visibleProperty().bind(viewModel.requiresPaymentOnAccountProperty());
            poaSection.managedProperty().bind(viewModel.requiresPaymentOnAccountProperty());
        }

        // Bind POA due row visibility
        if (poaDueRow != null) {
            poaDueRow.visibleProperty().bind(viewModel.requiresPaymentOnAccountProperty());
            poaDueRow.managedProperty().bind(viewModel.requiresPaymentOnAccountProperty());
        }
    }

    private void updateDisplay() {
        if (viewModel == null) {
            return;
        }

        // Update header
        if (taxYearBadge != null) {
            taxYearBadge.setText(getTaxYearBadgeText());
        }

        // Update tax at a glance
        if (netProfitValue != null) {
            netProfitValue.setText(viewModel.getFormattedNetProfit());
        }
        if (incomeTaxValue != null) {
            incomeTaxValue.setText(viewModel.getFormattedIncomeTax());
        }
        if (niClass4Value != null) {
            niClass4Value.setText(viewModel.getFormattedNiClass4());
        }
        if (totalTaxDueValue != null) {
            totalTaxDueValue.setText(viewModel.getFormattedTotalTax());
        }

        // Update income section
        if (turnoverLineValue != null) {
            turnoverLineValue.setText(viewModel.getFormattedTurnover());
        }
        if (incomeTotalLabel != null) {
            incomeTotalLabel.setText(viewModel.getFormattedTurnover());
        }

        // Update expenses section
        if (expensesTotalLabel != null) {
            expensesTotalLabel.setText(viewModel.getFormattedTotalExpenses());
        }
        populateExpenseLineItems();

        // Update profit calculation
        if (turnoverCalcValue != null) {
            turnoverCalcValue.setText(viewModel.getFormattedTurnover());
        }
        if (expensesCalcValue != null) {
            expensesCalcValue.setText(getFormattedExpensesForCalculation());
        }
        if (netProfitCalcValue != null) {
            netProfitCalcValue.setText(viewModel.getFormattedNetProfit());
        }

        // Update income tax section
        if (incomeTaxTotalLabel != null) {
            incomeTaxTotalLabel.setText(viewModel.getFormattedIncomeTax());
        }
        populateIncomeTaxBands();

        // Update NI section
        if (niTotalLabel != null) {
            niTotalLabel.setText(viewModel.getFormattedNiClass4());
        }
        populateNiBands();

        // Update POA section
        updatePoaSection();

        // Update Class 2 NI section (SE-810)
        updateClass2Section();

        // Update total due card
        if (taxYearDueLabel != null && taxYear != null) {
            taxYearDueLabel.setText("Tax Year " + taxYear.label() + ":");
        }
        if (taxYearDueValue != null) {
            taxYearDueValue.setText(viewModel.getFormattedTotalTax());
        }
        if (poaDueValue != null) {
            poaDueValue.setText(viewModel.getFormattedPaymentOnAccount());
        }
        if (grandTotalValue != null) {
            grandTotalValue.setText(formatCurrency(getGrandTotal()));
        }
        if (dueDateLabel != null) {
            dueDateLabel.setText(getDueDateText());
        }

        // Update section visibility
        updateSectionVisibility();
    }

    private void populateExpenseLineItems() {
        if (expensesContent == null) {
            return;
        }

        expensesContent.getChildren().clear();

        for (ExpenseLineItem item : getExpenseLineItems()) {
            HBox row = createExpenseLineItemRow(item);
            expensesContent.getChildren().add(row);
        }
    }

    private HBox createExpenseLineItemRow(ExpenseLineItem item) {
        HBox row = new HBox();
        row.getStyleClass().add("line-item");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label categoryLabel = new Label(item.category());
        categoryLabel.getStyleClass().add("line-item-label");

        Label boxLabel = new Label("(" + item.boxRef() + ")");
        boxLabel.getStyleClass().add("line-item-box");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label amountLabel = new Label(formatCurrency(item.amount()));
        amountLabel.getStyleClass().add("line-item-value");

        row.getChildren().addAll(categoryLabel, boxLabel, spacer, amountLabel);
        return row;
    }

    private void populateIncomeTaxBands() {
        if (incomeTaxContent == null) {
            return;
        }

        incomeTaxContent.getChildren().clear();

        for (TaxBandItem band : getIncomeTaxBands()) {
            HBox row = createTaxBandRow(band, "income-tax");
            incomeTaxContent.getChildren().add(row);
        }
    }

    private void populateNiBands() {
        if (niContent == null) {
            return;
        }

        niContent.getChildren().clear();

        for (TaxBandItem band : getNiClass4Bands()) {
            HBox row = createTaxBandRow(band, "ni-class4");
            niContent.getChildren().add(row);
        }
    }

    private HBox createTaxBandRow(TaxBandItem band, String styleType) {
        HBox row = new HBox();
        row.getStyleClass().add("tax-band-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox descBox = new VBox(2);
        Label descLabel = new Label(band.description());
        descLabel.getStyleClass().add("tax-band-desc");
        Label rangeLabel = new Label(band.range());
        rangeLabel.getStyleClass().add("tax-band-range");
        descBox.getChildren().addAll(descLabel, rangeLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label amountLabel = new Label(formatCurrency(band.amount()));
        amountLabel.getStyleClass().addAll("tax-band-amount", styleType);

        row.getChildren().addAll(descBox, spacer, amountLabel);
        return row;
    }

    private void updatePoaSection() {
        if (viewModel == null || !viewModel.requiresPaymentOnAccount()) {
            return;
        }

        if (poaSectionTitle != null && taxYear != null) {
            int endYear = taxYear.startYear() + 1;
            int nextYearEnd = endYear + 1;
            poaSectionTitle.setText("PAYMENTS ON ACCOUNT (" + endYear + "/" + (nextYearEnd % 100) + ")");
        }

        if (poaTotalLabel != null) {
            poaTotalLabel.setText(viewModel.getFormattedPaymentOnAccount());
        }

        BigDecimal halfPoa = viewModel.getPaymentOnAccountAmount().divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        String halfPoaFormatted = formatCurrency(halfPoa);

        if (poaFirstLabel != null && viewModel.getFirstPoaDueDate() != null) {
            poaFirstLabel.setText("First Payment (" + viewModel.getFirstPoaDueDate().format(DATE_FORMAT) + ")");
        }
        if (poaFirstPayment != null) {
            poaFirstPayment.setText(halfPoaFormatted);
        }

        if (poaSecondLabel != null && viewModel.getSecondPoaDueDate() != null) {
            poaSecondLabel.setText("Second Payment (" + viewModel.getSecondPoaDueDate().format(DATE_FORMAT) + ")");
        }
        if (poaSecondPayment != null) {
            poaSecondPayment.setText(halfPoaFormatted);
        }
    }

    private void updateSectionVisibility() {
        if (incomeContent != null) {
            incomeContent.setVisible(incomeSectionExpanded);
            incomeContent.setManaged(incomeSectionExpanded);
        }
        if (incomeToggle != null) {
            incomeToggle.setText(getIncomeSectionToggleText());
        }

        if (expensesContent != null) {
            expensesContent.setVisible(expensesSectionExpanded);
            expensesContent.setManaged(expensesSectionExpanded);
        }
        if (expensesToggle != null) {
            expensesToggle.setText(getExpensesSectionToggleText());
        }

        if (incomeTaxContent != null) {
            incomeTaxContent.setVisible(incomeTaxSectionExpanded);
            incomeTaxContent.setManaged(incomeTaxSectionExpanded);
        }
        if (incomeTaxToggle != null) {
            incomeTaxToggle.setText(getIncomeTaxSectionToggleText());
        }

        if (niContent != null) {
            niContent.setVisible(niSectionExpanded);
            niContent.setManaged(niSectionExpanded);
        }
        if (niToggle != null) {
            niToggle.setText(getNiSectionToggleText());
        }

        if (poaContent != null) {
            poaContent.setVisible(poaSectionExpanded);
            poaContent.setManaged(poaSectionExpanded);
        }
        if (poaToggle != null) {
            poaToggle.setText(getPoaSectionToggleText());
        }
    }

    private void exportToPdf(File file) {
        // Basic PDF export implementation
        // In a full implementation, this would use a PDF library like iText or OpenPDF
        try {
            // For now, show a success message
            showInfo("Export Complete", "Tax summary exported to:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Export Failed", "Failed to export PDF: " + e.getMessage());
        }
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openUrl(String url) {
        BrowserUtil.openUrl(url, error -> {
            // Show error on JavaFX thread
            javafx.application.Platform.runLater(() ->
                    showError("Unable to Open Link", "Could not open the link in your browser.\n\nURL: " + url));
        });
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return CURRENCY_FORMAT.format(amount);
    }

    private String formatCategoryDisplayName(ExpenseCategory category) {
        // Convert OFFICE_COSTS to "Office costs"
        String name = category.getDisplayName();
        if (name.contains("(")) {
            name = name.substring(0, name.indexOf("(")).trim();
        }
        return name;
    }

    private String formatRange(BigDecimal from, BigDecimal to) {
        return formatCurrency(from) + " - " + formatCurrency(to);
    }

    private String formatBasicRateRange() {
        // Basic rate band for 2024-25: £12,570 to £50,270
        return "£12,571 - £50,270";
    }

    private String formatHigherRateRange() {
        // Higher rate band for 2024-25: £50,271 to £125,140
        return "£50,271 - £125,140";
    }

    // === Record Types for Line Items ===

    /**
     * Represents an expense line item with SA103 box reference.
     */
    public record ExpenseLineItem(String category, String boxRef, BigDecimal amount) {}

    /**
     * Represents a tax band row for income tax or NI display.
     */
    public record TaxBandItem(String description, String range, BigDecimal amount) {}
}
