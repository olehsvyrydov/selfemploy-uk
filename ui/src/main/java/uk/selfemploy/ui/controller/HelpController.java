package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import uk.selfemploy.common.domain.TaxYear;
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
    public static final String GITHUB_REPO_URL = "https://github.com/selfemploy-uk/self-employment";

    /**
     * The GitHub Issues URL for this application.
     * Used for reporting bugs and feature requests.
     */
    public static final String GITHUB_ISSUES_URL = "https://github.com/selfemploy-uk/self-employment/issues";

    private static final List<String> HELP_CATEGORIES = Arrays.asList(
        "Tax & Calculation",
        "Expenses",
        "HMRC Submission",
        "General"
    );

    private static final Map<String, List<HelpTopic>> CATEGORY_TOPICS;
    private static final Map<HelpTopic, String> TOPIC_DISPLAY_NAMES;
    private static final Map<HmrcLinkTopic, String> LINK_DISPLAY_NAMES;

    static {
        CATEGORY_TOPICS = new LinkedHashMap<>();
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
    }

    // === FXML Injected Fields ===

    @FXML private ListView<String> categoryList;
    @FXML private ListView<String> topicList;
    @FXML private VBox contentPane;
    @FXML private Label helpTitle;
    @FXML private TextArea helpBody;
    @FXML private Label hmrcLinkLabel;

    // === State ===

    private TaxYear taxYear;
    private final HelpService helpService;
    private HelpTopic selectedTopic;

    public HelpController() {
        this.helpService = new HelpService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize would set up list views if they exist
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
        LOG.info("Opening GitHub Issues in in-app browser");
        helpService.openHmrcGuidance(GITHUB_ISSUES_URL, "GitHub Issues");
    }

    @FXML
    void handleDocumentationLink(ActionEvent event) {
        LOG.info("Opening Documentation in in-app browser");
        helpService.openHmrcGuidance(GITHUB_REPO_URL, "Documentation");
    }

    // === Help Topic Click Handlers ===

    @FXML
    void handleNetProfitClick(ActionEvent event) {
        LOG.info("Help topic clicked: Net Profit");
        showHelpDialog(HelpTopic.NET_PROFIT);
    }

    @FXML
    void handleIncomeTaxClick(ActionEvent event) {
        LOG.info("Help topic clicked: Income Tax");
        showHelpDialog(HelpTopic.INCOME_TAX);
    }

    @FXML
    void handlePersonalAllowanceClick(ActionEvent event) {
        LOG.info("Help topic clicked: Personal Allowance");
        showHelpDialog(HelpTopic.PERSONAL_ALLOWANCE);
    }

    @FXML
    void handleNationalInsuranceClick(ActionEvent event) {
        LOG.info("Help topic clicked: National Insurance");
        showHelpDialog(HelpTopic.NI_CLASS_4);
    }

    @FXML
    void handlePaymentsOnAccountClick(ActionEvent event) {
        LOG.info("Help topic clicked: Payments on Account");
        showHelpDialog(HelpTopic.PAYMENTS_ON_ACCOUNT);
    }

    @FXML
    void handleExpenseCategoriesClick(ActionEvent event) {
        LOG.info("Help topic clicked: Expense Categories");
        showHelpDialog(HelpTopic.EXPENSE_CATEGORY);
    }

    @FXML
    void handleAllowableExpensesClick(ActionEvent event) {
        LOG.info("Help topic clicked: Allowable Expenses");
        showHelpDialog(HelpTopic.ALLOWABLE_EXPENSES);
    }

    @FXML
    void handleDeclarationClick(ActionEvent event) {
        LOG.info("Help topic clicked: Declaration");
        showHelpDialog(HelpTopic.DECLARATION);
    }

    @FXML
    void handleHmrcSubmissionClick(ActionEvent event) {
        LOG.info("Help topic clicked: HMRC Submission");
        showHelpDialog(HelpTopic.HMRC_SUBMISSION);
    }

    @FXML
    void handleTaxYearClick(ActionEvent event) {
        LOG.info("Help topic clicked: Tax Year");
        showHelpDialog(HelpTopic.TAX_YEAR);
    }

    @FXML
    void handleSa103FormClick(ActionEvent event) {
        LOG.info("Help topic clicked: SA103 Form");
        showHelpDialog(HelpTopic.SA103_FORM);
    }

    // === Private Helper Methods ===

    /**
     * Shows a help dialog for the specified topic.
     * The dialog displays the help title, body text, and optionally an HMRC guidance button.
     *
     * @param topic the help topic to display
     */
    private void showHelpDialog(HelpTopic topic) {
        var contentOpt = getHelpForTopic(topic);
        contentOpt.ifPresent(content -> {
            // Create a custom dialog
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("Help - " + content.title());
            dialog.setHeaderText(content.title());

            // Create the content layout
            VBox contentBox = new VBox(12);
            contentBox.setPadding(new Insets(10, 0, 10, 0));

            Label bodyLabel = new Label(content.body());
            bodyLabel.setWrapText(true);
            bodyLabel.setMaxWidth(450);
            contentBox.getChildren().add(bodyLabel);

            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setContent(contentBox);
            dialogPane.setMinWidth(500);

            // Add HMRC link button if available
            if (content.hmrcLink() != null && !content.hmrcLink().isBlank()) {
                ButtonType hmrcButton = new ButtonType(
                    content.linkText() != null ? content.linkText() : "View HMRC Guidance",
                    ButtonBar.ButtonData.LEFT
                );
                dialog.getButtonTypes().add(0, hmrcButton);

                dialog.setResultConverter(buttonType -> {
                    if (buttonType == hmrcButton) {
                        // Open HMRC guidance in in-app browser
                        helpService.openHmrcGuidance(content.hmrcLink(), content.title());
                    }
                    return buttonType;
                });
            }

            dialog.showAndWait();
        });
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
