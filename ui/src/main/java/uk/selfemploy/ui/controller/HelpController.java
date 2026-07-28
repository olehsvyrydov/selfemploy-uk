package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.component.HelpDialog;
import uk.selfemploy.ui.component.HelpDialog.DialogSize;
import uk.selfemploy.ui.help.HelpContent;
import uk.selfemploy.ui.help.HelpService;
import uk.selfemploy.ui.help.HelpTopic;
import uk.selfemploy.ui.help.HmrcLinkTopic;

import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * Controller for the Help page.
 * Provides access to help content and HMRC links via HelpService.
 */
public class HelpController implements Initializable, MainController.TaxYearAware {

    private static final Logger LOG = Logger.getLogger(HelpController.class.getName());

    /**
     * The GitHub repository URL for this application.
     * Used for Documentation links in the Help page.
     */
    public static final String GITHUB_REPO_URL = "https://github.com/olehsvyrydov/selfemploy-uk";

    /**
     * The GitHub Issues URL for this application.
     * Used for reporting bugs and feature requests.
     */
    public static final String GITHUB_ISSUES_URL = "https://github.com/olehsvyrydov/selfemploy-uk/issues";

    private static final Map<String, List<HelpTopic>> CATEGORY_TOPICS;
    private static final Map<HelpTopic, Ikon> TOPIC_ICONS;
    private static final Map<String, String> CATEGORY_COLORS;

    static {
        CATEGORY_TOPICS = new LinkedHashMap<>();
        CATEGORY_TOPICS.put("User Guide", Arrays.asList(
            HelpTopic.GETTING_STARTED,
            HelpTopic.BANK_IMPORT,
            HelpTopic.HMRC_CONNECTION,
            HelpTopic.SECURITY_PRIVACY,
            HelpTopic.FAQ
        ));
        CATEGORY_TOPICS.put("Tax & Calculation", Arrays.asList(
            HelpTopic.NET_PROFIT,
            HelpTopic.INCOME_TAX,
            HelpTopic.PERSONAL_ALLOWANCE,
            HelpTopic.NI_CLASS_4,
            HelpTopic.NI_CLASS_2,
            HelpTopic.PAYMENTS_ON_ACCOUNT
        ));
        CATEGORY_TOPICS.put("Expenses", Arrays.asList(
            HelpTopic.EXPENSE_CATEGORY,
            HelpTopic.ALLOWABLE_EXPENSES
        ));
        CATEGORY_TOPICS.put("HMRC Submission", Arrays.asList(
            HelpTopic.DECLARATION,
            HelpTopic.HMRC_SUBMISSION
        ));
        CATEGORY_TOPICS.put("General", Arrays.asList(
            HelpTopic.TAX_YEAR,
            HelpTopic.SA103_FORM
        ));

        // User Guide topics

        // User Guide topics

        // Topic icons (FontAwesome)
        TOPIC_ICONS = new EnumMap<>(HelpTopic.class);
        TOPIC_ICONS.put(HelpTopic.NET_PROFIT, FontAwesomeSolid.CHART_LINE);
        TOPIC_ICONS.put(HelpTopic.INCOME_TAX, FontAwesomeSolid.POUND_SIGN);
        TOPIC_ICONS.put(HelpTopic.PERSONAL_ALLOWANCE, FontAwesomeSolid.SHIELD_ALT);
        TOPIC_ICONS.put(HelpTopic.NI_CLASS_4, FontAwesomeSolid.HEARTBEAT);
        TOPIC_ICONS.put(HelpTopic.NI_CLASS_2, FontAwesomeSolid.HEARTBEAT);
        TOPIC_ICONS.put(HelpTopic.PAYMENTS_ON_ACCOUNT, FontAwesomeSolid.CHART_BAR);
        TOPIC_ICONS.put(HelpTopic.EXPENSE_CATEGORY, FontAwesomeSolid.CLIPBOARD);
        TOPIC_ICONS.put(HelpTopic.ALLOWABLE_EXPENSES, FontAwesomeSolid.CHECK_CIRCLE);
        TOPIC_ICONS.put(HelpTopic.DECLARATION, FontAwesomeSolid.SIGNATURE);
        TOPIC_ICONS.put(HelpTopic.HMRC_SUBMISSION, FontAwesomeSolid.UPLOAD);
        TOPIC_ICONS.put(HelpTopic.TAX_YEAR, FontAwesomeSolid.CALENDAR_ALT);
        TOPIC_ICONS.put(HelpTopic.SA103_FORM, FontAwesomeSolid.FILE_ALT);
        // User Guide topics
        TOPIC_ICONS.put(HelpTopic.GETTING_STARTED, FontAwesomeSolid.ROCKET);
        TOPIC_ICONS.put(HelpTopic.BANK_IMPORT, FontAwesomeSolid.FILE_UPLOAD);
        TOPIC_ICONS.put(HelpTopic.HMRC_CONNECTION, FontAwesomeSolid.LINK);
        TOPIC_ICONS.put(HelpTopic.SECURITY_PRIVACY, FontAwesomeSolid.LOCK);
        TOPIC_ICONS.put(HelpTopic.FAQ, FontAwesomeSolid.QUESTION_CIRCLE);

        // Category colors (matching /aura's design)
        CATEGORY_COLORS = new HashMap<>();
        CATEGORY_COLORS.put("User Guide", "#7c3aed");         // Purple (indigo/violet)
        CATEGORY_COLORS.put("Tax & Calculation", "#059669");  // Green
        CATEGORY_COLORS.put("Expenses", "#d97706");           // Orange
        CATEGORY_COLORS.put("HMRC Submission", "#0066cc");    // Blue
        CATEGORY_COLORS.put("General", "#6b7280");            // Gray
    }

    // === Constants ===

    private static final String APPLICATION_VERSION = uk.selfemploy.common.util.VersionInfo.getVersion();

    // === FXML Injected Fields ===

    @FXML private Label versionLabel;

    // === State ===

    private TaxYear taxYear;
    // Rebuilt per selected tax year so year-aware topics (NI Class 2, deadlines) resolve their
    // figures for the year the user is viewing, not just the machine's current year.
    private HelpService helpService;

    // Wired by MainController; runs the guided tour when the user replays it from here.
    private Runnable onReplayTour;

    public HelpController() {
        this.helpService = new HelpService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize version label if present
        if (versionLabel != null) {
            versionLabel.setText("Version " + APPLICATION_VERSION);
        }
    }

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;
        if (taxYear != null) {
            this.helpService = new HelpService(taxYear.startYear());
        }
    }

    /**
     * Returns the current tax year.
     */
    public TaxYear getTaxYear() {
        return taxYear;
    }

    /**
     * Returns the HelpService instance.
     */
    public HelpService getHelpService() {
        return helpService;
    }

    /**
     * Gets help content for a specific topic.
     */
    public Optional<HelpContent> getHelpForTopic(HelpTopic topic) {
        return helpService.getHelp(topic);
    }

    /**
     * Gets an HMRC link URL.
     */
    public String getHmrcLink(HmrcLinkTopic topic) {
        return helpService.getHmrcLink(topic);
    }

    /**
     * Returns the icon for a help topic.
     */
    public Ikon getTopicIcon(HelpTopic topic) {
        return TOPIC_ICONS.getOrDefault(topic, FontAwesomeSolid.INFO_CIRCLE);
    }

    /**
     * Returns the color for a category.
     */
    public String getCategoryColor(String category) {
        return CATEGORY_COLORS.getOrDefault(category, "#0066cc");
    }

    /**
     * Returns the category for a given topic.
     */
    public String getCategoryForTopic(HelpTopic topic) {
        for (Map.Entry<String, List<HelpTopic>> entry : CATEGORY_TOPICS.entrySet()) {
            if (entry.getValue().contains(topic)) {
                return entry.getKey();
            }
        }
        return "General";
    }

    // === FXML Event Handlers ===

    /** Sets the callback that replays the guided tour. Wired by {@code MainController}. */
    public void setOnReplayTour(Runnable onReplayTour) {
        this.onReplayTour = onReplayTour;
    }

    @FXML
    void handleReplayTour(ActionEvent event) {
        if (onReplayTour != null) {
            onReplayTour.run();
        }
    }

    // === Quick Link Button Handlers ===

    @FXML
    void handleTaxRatesLink(ActionEvent event) {
        LOG.info("Opening HMRC Tax Rates in in-app browser");
        helpService.openHmrcGuidance(HmrcLinkTopic.TAX_RATES);
    }

    @FXML
    void handleSa103Link(ActionEvent event) {
        LOG.info("Opening HMRC SA103 Form in in-app browser");
        helpService.openHmrcGuidance(HmrcLinkTopic.SA103_FORM);
    }

    @FXML
    void handleFilingDeadlinesLink(ActionEvent event) {
        LOG.info("Opening HMRC Filing Deadlines in in-app browser");
        helpService.openHmrcGuidance(HmrcLinkTopic.FILING_DEADLINES);
    }

    @FXML
    void handleAllowableExpensesLink(ActionEvent event) {
        LOG.info("Opening HMRC Allowable Expenses in in-app browser");
        helpService.openHmrcGuidance(HmrcLinkTopic.ALLOWABLE_EXPENSES);
    }

    @FXML
    void handleNiRatesLink(ActionEvent event) {
        LOG.info("Opening HMRC NI Rates in in-app browser");
        helpService.openHmrcGuidance(HmrcLinkTopic.NI_RATES);
    }

    @FXML
    void handleStatePensionLink(ActionEvent event) {
        LOG.info("Opening HMRC State Pension in in-app browser");
        helpService.openHmrcGuidance(HmrcLinkTopic.STATE_PENSION);
    }

    @FXML
    void handleGitHubIssuesLink(ActionEvent event) {
        LOG.info("Opening GitHub Issues in external browser");
        // GitHub doesn't render well in JavaFX WebView, open in external browser
        uk.selfemploy.ui.util.BrowserUtil.openUrl(GITHUB_ISSUES_URL);
    }

    @FXML
    void handleDocumentationLink(ActionEvent event) {
        LOG.info("Opening Documentation in external browser");
        // GitHub doesn't render well in JavaFX WebView, open in external browser
        uk.selfemploy.ui.util.BrowserUtil.openUrl(GITHUB_REPO_URL);
    }

    @FXML
    void handleViewGitHubLink(ActionEvent event) {
        LOG.info("Opening GitHub Repository in external browser");
        uk.selfemploy.ui.util.BrowserUtil.openUrl(GITHUB_REPO_URL);
    }

    // === Help Topic Click Handlers (MouseEvent for onMouseClicked in FXML) ===

    @FXML
    void handleNetProfitClick(MouseEvent event) {
        LOG.info("Help topic clicked: Net Profit");
        showHelpDialog(HelpTopic.NET_PROFIT);
    }

    @FXML
    void handleIncomeTaxClick(MouseEvent event) {
        LOG.info("Help topic clicked: Income Tax");
        showHelpDialog(HelpTopic.INCOME_TAX);
    }

    @FXML
    void handlePersonalAllowanceClick(MouseEvent event) {
        LOG.info("Help topic clicked: Personal Allowance");
        showHelpDialog(HelpTopic.PERSONAL_ALLOWANCE);
    }

    @FXML
    void handleNationalInsuranceClick(MouseEvent event) {
        LOG.info("Help topic clicked: NI Class 4");
        showHelpDialog(HelpTopic.NI_CLASS_4);
    }

    @FXML
    void handleNationalInsuranceClass2Click(MouseEvent event) {
        LOG.info("Help topic clicked: NI Class 2");
        showHelpDialog(HelpTopic.NI_CLASS_2);
    }

    @FXML
    void handlePaymentsOnAccountClick(MouseEvent event) {
        LOG.info("Help topic clicked: Payments on Account");
        showHelpDialog(HelpTopic.PAYMENTS_ON_ACCOUNT);
    }

    @FXML
    void handleExpenseCategoriesClick(MouseEvent event) {
        LOG.info("Help topic clicked: Expense Categories");
        showHelpDialog(HelpTopic.EXPENSE_CATEGORY);
    }

    @FXML
    void handleAllowableExpensesClick(MouseEvent event) {
        LOG.info("Help topic clicked: Allowable Expenses");
        showHelpDialog(HelpTopic.ALLOWABLE_EXPENSES);
    }

    @FXML
    void handleDeclarationClick(MouseEvent event) {
        LOG.info("Help topic clicked: Declaration");
        showHelpDialog(HelpTopic.DECLARATION);
    }

    @FXML
    void handleHmrcSubmissionClick(MouseEvent event) {
        LOG.info("Help topic clicked: HMRC Submission");
        showHelpDialog(HelpTopic.HMRC_SUBMISSION);
    }

    @FXML
    void handleTaxYearClick(MouseEvent event) {
        LOG.info("Help topic clicked: Tax Year");
        showHelpDialog(HelpTopic.TAX_YEAR);
    }

    @FXML
    void handleSa103FormClick(MouseEvent event) {
        LOG.info("Help topic clicked: SA103 Form");
        showHelpDialog(HelpTopic.SA103_FORM);
    }

    // === User Guide Topic Click Handlers ===
    // These topics use MEDIUM size dialogs (800x600) for more comprehensive content

    @FXML
    void handleGettingStartedClick(MouseEvent event) {
        LOG.info("Help topic clicked: Getting Started");
        showHelpDialog(HelpTopic.GETTING_STARTED, DialogSize.MEDIUM);
    }

    @FXML
    void handleBankImportClick(MouseEvent event) {
        LOG.info("Help topic clicked: Bank Statement Import");
        showHelpDialog(HelpTopic.BANK_IMPORT, DialogSize.MEDIUM);
    }

    @FXML
    void handleHmrcConnectionClick(MouseEvent event) {
        LOG.info("Help topic clicked: HMRC Connection");
        showHelpDialog(HelpTopic.HMRC_CONNECTION, DialogSize.MEDIUM);
    }

    @FXML
    void handleSecurityPrivacyClick(MouseEvent event) {
        LOG.info("Help topic clicked: Security & Privacy");
        showHelpDialog(HelpTopic.SECURITY_PRIVACY, DialogSize.MEDIUM);
    }

    @FXML
    void handleFaqClick(MouseEvent event) {
        LOG.info("Help topic clicked: FAQ");
        showHelpDialog(HelpTopic.FAQ, DialogSize.MEDIUM);
    }

    @FXML
    void handleUserGuideClick(ActionEvent event) {
        LOG.info("Opening full User Guide dialog");
        showUserGuideDialog();
    }

    // === Private Helper Methods ===

    /**
     * Shows a help dialog for the specified topic with standard size.
     * The dialog displays the help title, body text, and optionally an HMRC guidance button.
     * Uses a custom styled dialog matching the application design.
     *
     * @param topic the help topic to display
     */
    private void showHelpDialog(HelpTopic topic) {
        showHelpDialog(topic, DialogSize.STANDARD);
    }

    /**
     * Shows a help dialog for the specified topic with the given size.
     * The dialog displays the help title, body text, and optionally an HMRC guidance button.
     * Uses a custom styled dialog matching the application design.
     *
     * @param topic the help topic to display
     * @param size the dialog size (STANDARD, MEDIUM, or LARGE)
     */
    private void showHelpDialog(HelpTopic topic, DialogSize size) {
        getHelpForTopic(topic).ifPresentOrElse(content -> {
            Ikon icon = getTopicIcon(topic);
            String category = getCategoryForTopic(topic);
            String color = getCategoryColor(category);

            HelpDialog dialog = new HelpDialog(content, icon, color, helpService, size);
            dialog.showAndWaitDialog();
        }, () -> LOG.warning("No help content available for topic: " + topic));
    }

    /**
     * Shows the comprehensive User Guide dialog with all key information.
     * This provides a single-page overview of the application.
     */
    private void showUserGuideDialog() {
        getHelpForTopic(HelpTopic.USER_GUIDE).ifPresentOrElse(userGuideContent -> {
            String color = getCategoryColor("User Guide");
            HelpDialog dialog = new HelpDialog(
                userGuideContent, FontAwesomeSolid.BOOK, color, helpService, HelpDialog.DialogSize.LARGE);
            dialog.showAndWaitDialog();
        }, () -> LOG.warning("User Guide help content is unavailable"));
    }

}
