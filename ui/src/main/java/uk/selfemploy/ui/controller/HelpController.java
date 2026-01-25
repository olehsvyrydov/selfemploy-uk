package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
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

    private static final List<String> HELP_CATEGORIES = Arrays.asList(
        "User Guide",
        "Tax & Calculation",
        "Expenses",
        "HMRC Submission",
        "General"
    );

    private static final Map<String, List<HelpTopic>> CATEGORY_TOPICS;
    private static final Map<HelpTopic, String> TOPIC_DISPLAY_NAMES;
    private static final Map<HmrcLinkTopic, String> LINK_DISPLAY_NAMES;
    private static final Map<HelpTopic, String> TOPIC_DESCRIPTIONS;
    private static final Map<HelpTopic, Ikon> TOPIC_ICONS;
    private static final Map<String, String> CATEGORY_COLORS;

    static {
        CATEGORY_TOPICS = new LinkedHashMap<>();
        CATEGORY_TOPICS.put("User Guide", Arrays.asList(
            HelpTopic.GETTING_STARTED,
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

        TOPIC_DISPLAY_NAMES = new EnumMap<>(HelpTopic.class);
        TOPIC_DISPLAY_NAMES.put(HelpTopic.NET_PROFIT, "Net Profit");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.INCOME_TAX, "Income Tax");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.PERSONAL_ALLOWANCE, "Personal Allowance");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.NI_CLASS_4, "NI Class 4");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.NI_CLASS_2, "NI Class 2");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.PAYMENTS_ON_ACCOUNT, "Payments on Account");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.EXPENSE_CATEGORY, "Expense Categories");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.ALLOWABLE_EXPENSES, "Allowable Expenses");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.DECLARATION, "Declaration");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.HMRC_SUBMISSION, "HMRC Submission");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.TAX_YEAR, "Tax Year");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.SA103_FORM, "SA103 Form");
        // User Guide topics
        TOPIC_DISPLAY_NAMES.put(HelpTopic.GETTING_STARTED, "Getting Started");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.HMRC_CONNECTION, "HMRC Connection");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.SECURITY_PRIVACY, "Security & Privacy");
        TOPIC_DISPLAY_NAMES.put(HelpTopic.FAQ, "FAQ");

        LINK_DISPLAY_NAMES = new EnumMap<>(HmrcLinkTopic.class);
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.TAX_RATES, "Income Tax Rates");
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.NI_RATES, "NI Rates for Self-Employed");
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.ALLOWABLE_EXPENSES, "Allowable Expenses Guide");
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.FILING_DEADLINES, "Filing Deadlines");
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.PAYMENTS_ON_ACCOUNT, "Payments on Account");
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.PERSONAL_ALLOWANCE, "Personal Allowance");
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.SA103_FORM, "SA103 Form & Notes");
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.MTD_FOR_ITSA, "Making Tax Digital");
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.STATE_PENSION, "State Pension Forecast");
        LINK_DISPLAY_NAMES.put(HmrcLinkTopic.NI_RECORD, "Check NI Record");

        // Topic descriptions for rich help topic cards
        TOPIC_DESCRIPTIONS = new EnumMap<>(HelpTopic.class);
        TOPIC_DESCRIPTIONS.put(HelpTopic.NET_PROFIT, "Understanding your taxable profit calculation");
        TOPIC_DESCRIPTIONS.put(HelpTopic.INCOME_TAX, "Tax bands and rates for the current tax year");
        TOPIC_DESCRIPTIONS.put(HelpTopic.PERSONAL_ALLOWANCE, "Tax-free income allowance explained");
        TOPIC_DESCRIPTIONS.put(HelpTopic.NI_CLASS_4, "National Insurance for self-employed profits");
        TOPIC_DESCRIPTIONS.put(HelpTopic.NI_CLASS_2, "Weekly National Insurance contributions");
        TOPIC_DESCRIPTIONS.put(HelpTopic.PAYMENTS_ON_ACCOUNT, "Advance tax payments to HMRC");
        TOPIC_DESCRIPTIONS.put(HelpTopic.EXPENSE_CATEGORY, "SA103 expense categories explained");
        TOPIC_DESCRIPTIONS.put(HelpTopic.ALLOWABLE_EXPENSES, "What expenses can you claim?");
        TOPIC_DESCRIPTIONS.put(HelpTopic.DECLARATION, "Legal declaration requirements");
        TOPIC_DESCRIPTIONS.put(HelpTopic.HMRC_SUBMISSION, "Submitting your Self Assessment");
        TOPIC_DESCRIPTIONS.put(HelpTopic.TAX_YEAR, "UK tax year dates and deadlines");
        TOPIC_DESCRIPTIONS.put(HelpTopic.SA103_FORM, "Self-employment supplementary pages");
        // User Guide topics
        TOPIC_DESCRIPTIONS.put(HelpTopic.GETTING_STARTED, "Quick overview of app features and first steps");
        TOPIC_DESCRIPTIONS.put(HelpTopic.HMRC_CONNECTION, "How OAuth2 works and password safety");
        TOPIC_DESCRIPTIONS.put(HelpTopic.SECURITY_PRIVACY, "Local storage, encryption and data handling");
        TOPIC_DESCRIPTIONS.put(HelpTopic.FAQ, "Common questions and answers");

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

    private static final String APPLICATION_VERSION = "0.1.0-SNAPSHOT";

    // === FXML Injected Fields ===

    @FXML private ListView<String> categoryList;
    @FXML private ListView<String> topicList;
    @FXML private VBox contentPane;
    @FXML private Label helpTitle;
    @FXML private TextArea helpBody;
    @FXML private Label hmrcLinkLabel;
    @FXML private Label versionLabel;

    // === State ===

    private TaxYear taxYear;
    private final HelpService helpService;
    private HelpTopic selectedTopic;

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
     * Returns the list of help categories.
     */
    public List<String> getHelpCategories() {
        return HELP_CATEGORIES;
    }

    /**
     * Returns topics for a given category.
     */
    public List<HelpTopic> getTopicsForCategory(String category) {
        return CATEGORY_TOPICS.getOrDefault(category, Collections.emptyList());
    }

    /**
     * Returns quick links to HMRC resources.
     */
    public List<HmrcLinkTopic> getQuickLinks() {
        return Arrays.asList(
            HmrcLinkTopic.TAX_RATES,
            HmrcLinkTopic.NI_RATES,
            HmrcLinkTopic.ALLOWABLE_EXPENSES,
            HmrcLinkTopic.FILING_DEADLINES,
            HmrcLinkTopic.PAYMENTS_ON_ACCOUNT,
            HmrcLinkTopic.SA103_FORM
        );
    }

    /**
     * Returns a display-friendly name for a help topic.
     */
    public String getTopicDisplayName(HelpTopic topic) {
        return TOPIC_DISPLAY_NAMES.getOrDefault(topic, topic.name());
    }

    /**
     * Returns a display-friendly name for an HMRC link topic.
     */
    public String getLinkDisplayName(HmrcLinkTopic topic) {
        return LINK_DISPLAY_NAMES.getOrDefault(topic, topic.name());
    }

    /**
     * Returns the description for a help topic.
     */
    public String getTopicDescription(HelpTopic topic) {
        return TOPIC_DESCRIPTIONS.getOrDefault(topic, "");
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

    /**
     * Returns the comprehensive User Guide content for the User Guide dialog.
     * This content is adapted from the full USER_GUIDE.md for in-app display.
     *
     * @return the user guide content as a formatted string
     */
    public String getUserGuideContent() {
        return USER_GUIDE_CONTENT;
    }

    /**
     * Comprehensive User Guide content for in-app display.
     * Adapted from docs/USER_GUIDE.md.
     */
    private static final String USER_GUIDE_CONTENT = """
            What This App Does
            ━━━━━━━━━━━━━━━━━━
            UK Self-Employment Manager is a free, open-source desktop application \
            for UK self-employed individuals to:

            • Track Income - Record all business income with dates and descriptions
            • Track Expenses - Log expenses categorised to match HMRC's SA103 form
            • Calculate Tax - Real-time estimates of Income Tax and NI Class 4
            • Submit to HMRC - One-click annual Self Assessment via MTD APIs

            What makes it different: completely free, privacy-first (data stays on \
            your computer), open source, and MTD compliant.

            Getting Started
            ━━━━━━━━━━━━━━━
            1. Complete the setup wizard with your name and UTR
            2. Select your tax year (runs 6 April to 5 April)
            3. Choose your business type (Freelancer, Sole Trader, etc.)

            After setup, you can:
            • Add income from the Dashboard or Income page
            • Record expenses from the Expenses page
            • View tax estimates on the Tax Summary page

            Daily Usage
            ━━━━━━━━━━━
            Adding Income:
            Navigate to Income > Add Income > Enter date, amount, description, source

            Adding Expenses:
            Navigate to Expenses > Add Expense > Select SA103 category > Enter details

            Expense categories match HMRC's SA103F form:
            • Cost of Sales (Box 17)
            • Staff Costs (Box 19)
            • Travel (Box 20)
            • Premises (Box 21)
            • Office Costs (Box 23)
            • Professional Fees (Box 26)
            • Other Expenses (Box 28)

            Understanding HMRC Connection
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            This app uses OAuth2 - a secure industry-standard protocol:

            1. You click "Connect to HMRC" in the app
            2. Your browser opens HMRC's official Government Gateway
            3. You log in with YOUR credentials on HMRC's website
            4. HMRC asks if you want to grant this app access
            5. You click "Allow" and return to the app

            Your password is NEVER shared with us:
            • You enter your password only on HMRC's website
            • We receive only a limited-access token
            • HMRC controls what we can do on your behalf

            Security and Privacy
            ━━━━━━━━━━━━━━━━━━━━
            Local-Only Storage:
            • All data stays on YOUR computer
            • We have NO access to your data
            • No cloud servers store your information

            Encryption:
            • Database encrypted with AES-256-GCM
            • HMRC tokens stored in your OS keychain
            • All data at rest is encrypted

            What's sent to HMRC when you submit:
            • Your tax figures (income, expenses, profit)
            • Fraud prevention headers (required by HMRC)

            Key Deadlines
            ━━━━━━━━━━━━━
            • Tax Year Ends: 5 April
            • Online Return Deadline: 31 January
            • Tax Payment Deadline: 31 January
            • Payment on Account 2: 31 July

            Late filing penalties:
            • 1 day late: £100
            • 3 months late: £10/day (up to £900)
            • 6 months late: £300 or 5% of tax owed
            • 12 months late: £300 or up to 100% of tax owed

            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            ⚠ This software is a record-keeping tool, not a substitute \
            for professional advice. Tax calculations are estimates only. \
            Always verify with a qualified accountant.
            """;

    // === FXML Event Handlers ===

    @FXML
    void handleCategorySelect() {
        // Handle category selection from ListView
        if (categoryList != null && categoryList.getSelectionModel().getSelectedItem() != null) {
            String category = categoryList.getSelectionModel().getSelectedItem();
            updateTopicList(category);
        }
    }

    @FXML
    void handleTopicSelect() {
        // Handle topic selection from ListView
        if (topicList != null && topicList.getSelectionModel().getSelectedItem() != null) {
            String topicName = topicList.getSelectionModel().getSelectedItem();
            HelpTopic topic = findTopicByDisplayName(topicName);
            if (topic != null) {
                showHelpContent(topic);
            }
        }
    }

    @FXML
    void handleOpenHmrcLink(ActionEvent event) {
        if (selectedTopic != null) {
            getHelpForTopic(selectedTopic).ifPresent(content -> {
                if (content.hmrcLink() != null) {
                    // Use in-app browser for HMRC/GOV.UK links
                    helpService.openHmrcGuidance(content.hmrcLink(), content.title());
                }
            });
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
        LOG.info("Help topic clicked: National Insurance");
        showHelpDialog(HelpTopic.NI_CLASS_4);
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
        var contentOpt = getHelpForTopic(topic);
        contentOpt.ifPresent(content -> {
            Ikon icon = getTopicIcon(topic);
            String category = getCategoryForTopic(topic);
            String color = getCategoryColor(category);

            HelpDialog dialog = new HelpDialog(content, icon, color, helpService, size);
            dialog.showAndWaitDialog();
        });
    }

    /**
     * Shows the comprehensive User Guide dialog with all key information.
     * This provides a single-page overview of the application.
     */
    private void showUserGuideDialog() {
        HelpContent userGuideContent = HelpContent.builder()
                .title("User Guide")
                .body(getUserGuideContent())
                .build();

        String color = getCategoryColor("User Guide");
        // Use large mode (1200x900) for User Guide with styled section titles
        HelpDialog dialog = new HelpDialog(userGuideContent, FontAwesomeSolid.BOOK, color, helpService, true);
        dialog.showAndWaitDialog();
    }

    private void updateTopicList(String category) {
        if (topicList != null) {
            topicList.getItems().clear();
            List<HelpTopic> topics = getTopicsForCategory(category);
            for (HelpTopic topic : topics) {
                topicList.getItems().add(getTopicDisplayName(topic));
            }
        }
    }

    private void showHelpContent(HelpTopic topic) {
        selectedTopic = topic;
        getHelpForTopic(topic).ifPresent(content -> {
            if (helpTitle != null) {
                helpTitle.setText(content.title());
            }
            if (helpBody != null) {
                helpBody.setText(content.body());
            }
            if (hmrcLinkLabel != null && content.linkText() != null) {
                hmrcLinkLabel.setText(content.linkText());
                hmrcLinkLabel.setVisible(true);
            }
        });
    }

    /**
     * Finds a HelpTopic by its display name.
     *
     * @param displayName the display name to search for
     * @return the matching HelpTopic, or null if not found
     */
    public HelpTopic findTopicByDisplayName(String displayName) {
        for (Map.Entry<HelpTopic, String> entry : TOPIC_DISPLAY_NAMES.entrySet()) {
            if (entry.getValue().equals(displayName)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
