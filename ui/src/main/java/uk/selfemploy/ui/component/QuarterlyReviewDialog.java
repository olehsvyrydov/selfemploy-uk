package uk.selfemploy.ui.component;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.exception.SubmissionException;
import uk.selfemploy.ui.util.DialogStyler;
import uk.selfemploy.ui.util.HmrcErrorGuidance;
import uk.selfemploy.ui.util.SubmissionErrorDisplay;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for reviewing quarterly data before submission to HMRC.
 *
 * <p>This dialog displays:</p>
 * <ul>
 *   <li>Header with quarter info and date range</li>
 *   <li>Income summary with transaction count</li>
 *   <li>Expenses grouped by SA103 category</li>
 *   <li>Net profit/loss calculation</li>
 *   <li>Confirmation checkboxes (all must be checked to enable submit)</li>
 *   <li>Submit to HMRC button</li>
 * </ul>
 *
 * <p>Design Guidelines:</p>
 * <ul>
 *   <li>Width: 500px (wider than info dialog for expense tables)</li>
 *   <li>Green header for normal review, red header for overdue</li>
 *   <li>Uses DialogStyler for consistent styling</li>
 * </ul>
 *
 * <p>Implementation: /james</p>
 *
 * @see QuarterlyReviewData
 * @see DialogStyler
 */
public class QuarterlyReviewDialog {

    private static final Logger LOG = Logger.getLogger(QuarterlyReviewDialog.class.getName());
    private static final String STYLESHEET = "/css/help.css";
    private static final HmrcErrorGuidance ERROR_GUIDANCE = new HmrcErrorGuidance();

    // ==================== Functional Interface ====================

    /**
     * Functional interface for handling HMRC submission.
     *
     * <p>This allows the dialog to remain decoupled from the backend service.
     * The controller wires up the actual QuarterlySubmissionService via this interface.</p>
     */
    @FunctionalInterface
    public interface SubmissionHandler {

        /**
         * Submits the quarterly data to HMRC.
         *
         * @param reviewData            the reviewed quarterly data
         * @param declarationAcceptedAt when the user accepted the declaration (UTC)
         * @param declarationTextHash   SHA-256 hash of the declaration confirmation texts
         * @return the Submission record with HMRC reference on success
         * @throws SubmissionException if submission fails
         */
        Submission submit(QuarterlyReviewData reviewData, Instant declarationAcceptedAt, String declarationTextHash);
    }

    // ==================== Design Constants ====================

    /**
     * Dialog width in pixels - slightly wider than info dialog for tables.
     */
    public static final int DIALOG_WIDTH = 500;

    /**
     * Corner radius for the dialog container.
     */
    public static final double CORNER_RADIUS = 12.0;

    // ==================== Error Dialog Constants ====================

    /**
     * Hint text shown in error dialogs when the failure is retryable (e.g., server error, timeout).
     * Condition 7 (/jorge): retryable flag must produce explicit text hint in the dialog body.
     */
    public static final String RETRYABLE_HINT_TEXT =
            "This is a temporary issue. You can try again.";

    /**
     * Solid colour for the "Open Settings" button in error dialogs.
     * D-07 (/aura): use solid #0066cc instead of Bootstrap gradient.
     */
    public static final String SETTINGS_BUTTON_COLOR = "#0066cc";

    // ==================== Header Gradient Colors ====================

    private static final String[] REVIEW_GRADIENT = {"#28a745", "#48c664"};  // Green for review
    private static final String[] OVERDUE_GRADIENT = {"#dc3545", "#e4606d"}; // Red for overdue

    // ==================== Confirmation Texts ====================

    private static final String[] NORMAL_CONFIRMATIONS = {
            "I confirm I have included all income received during this period",
            "I confirm all expenses are allowable business costs",
            "I understand this will be submitted to HMRC"
    };

    private static final String[] NIL_RETURN_CONFIRMATIONS = {
            "I confirm I had no business income this quarter",
            "I confirm I had no business expenses this quarter",
            "I understand this nil return will be submitted to HMRC"
    };

    // ==================== Instance Fields ====================

    private final QuarterlyReviewData reviewData;
    private final boolean isOverdue;
    private final Stage stage;
    private final List<CheckBox> confirmationCheckboxes = new ArrayList<>();
    private Button submitButton;
    private Button cancelButton;
    private StackPane progressOverlay;
    private Runnable onSubmitAction;
    private SubmissionHandler submissionHandler;
    private Consumer<Submission> onSubmitComplete;
    private Runnable onOpenSettings;

    // ==================== Constructor ====================

    /**
     * Creates a QuarterlyReviewDialog.
     *
     * @param reviewData the aggregated review data (must not be null)
     * @param isOverdue whether this quarter is overdue
     */
    public QuarterlyReviewDialog(QuarterlyReviewData reviewData, boolean isOverdue) {
        this.reviewData = Objects.requireNonNull(reviewData, "reviewData must not be null");
        this.isOverdue = isOverdue;

        this.stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Review " + reviewData.getPeriodHeaderText());

        // Find owner window
        Window owner = Window.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(null);
        if (owner != null) {
            stage.initOwner(owner);
        }

        initializeDialog();
    }

    // ==================== Public API ====================

    /**
     * Sets the callback for the submit button.
     *
     * @param action the callback to execute when submit is clicked
     */
    public void setOnSubmitAction(Runnable action) {
        this.onSubmitAction = action;
    }

    /**
     * Sets the submission handler for HMRC submission.
     *
     * <p>When set, the submit button will call this handler instead of showing
     * the "Coming Soon" placeholder. The handler runs on a background thread.</p>
     *
     * @param handler the submission handler
     */
    public void setSubmissionHandler(SubmissionHandler handler) {
        this.submissionHandler = handler;
    }

    /**
     * Sets the callback invoked when submission completes successfully.
     *
     * @param callback the callback receiving the accepted Submission
     */
    public void setOnSubmitComplete(Consumer<Submission> callback) {
        this.onSubmitComplete = callback;
    }

    /**
     * Sets a callback to navigate to the Settings page.
     *
     * <p>SE-10E-003: When a settings error occurs (e.g., NINO not set, OAuth expired),
     * the error dialog shows an "Open Settings" button. When clicked, both the error dialog
     * and the review dialog are closed, and this callback is invoked to navigate to Settings.</p>
     *
     * @param onOpenSettings callback to navigate to Settings, or null to disable
     */
    public void setOnOpenSettings(Runnable onOpenSettings) {
        this.onOpenSettings = onOpenSettings;
    }

    /**
     * Shows the dialog and waits for the user to close it.
     */
    public void showAndWait() {
        stage.showAndWait();
    }

    /**
     * Shows the dialog (non-blocking).
     */
    public void show() {
        stage.show();
    }

    /**
     * Closes the dialog.
     */
    public void close() {
        stage.close();
    }

    // ==================== Static Helper Methods (for testing) ====================

    /**
     * Returns the confirmation texts for the checkboxes.
     *
     * @param isNilReturn whether this is a nil return
     * @return array of confirmation texts
     */
    public static String[] getConfirmationTexts(boolean isNilReturn) {
        return isNilReturn ? NIL_RETURN_CONFIRMATIONS : NORMAL_CONFIRMATIONS;
    }

    /**
     * Returns the submit button text.
     *
     * @param isNilReturn whether this is a nil return
     * @return the button text
     */
    public static String getSubmitButtonText(boolean isNilReturn) {
        return isNilReturn ? "Submit Nil Return to HMRC" : "Submit to HMRC";
    }

    /**
     * Returns the header gradient colors.
     *
     * @param isOverdue whether this quarter is overdue
     * @return array of [startColor, endColor] hex values
     */
    public static String[] getHeaderGradientColors(boolean isOverdue) {
        return isOverdue ? OVERDUE_GRADIENT : REVIEW_GRADIENT;
    }

    /**
     * Computes a SHA-256 hash of the declaration confirmation texts.
     *
     * <p>This hash is stored with the submission as an audit trail to prove
     * which declaration text the user agreed to.</p>
     *
     * @param isNilReturn whether this is a nil return
     * @return lowercase hex-encoded SHA-256 hash (64 characters)
     */
    public static String computeDeclarationHash(boolean isNilReturn) {
        String[] texts = getConfirmationTexts(isNilReturn);
        String allText = String.join("|", texts);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(allText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by the Java specification, should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Formats a currency amount for display.
     *
     * @param amount the amount to format
     * @return formatted string with pound sign and comma separators
     */
    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "\u00A30.00";
        }
        return String.format("\u00A3%,.2f", amount);
    }

    // ==================== Private Methods ====================

    private void initializeDialog() {
        VBox container = new VBox(0);
        container.getStyleClass().add("help-dialog-container");
        container.setMinWidth(DIALOG_WIDTH);
        container.setMaxWidth(DIALOG_WIDTH);

        // Header
        container.getChildren().add(createHeader());

        // Scrollable body content
        ScrollPane scrollPane = new ScrollPane(createBody());
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(450); // Limit height to prevent overly tall dialog
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        container.getChildren().add(scrollPane);

        // Confirmations section
        container.getChildren().add(createConfirmationsSection());

        // Footer with buttons
        container.getChildren().add(createFooter());

        // Wrap in StackPane for progress overlay support
        progressOverlay = createProgressOverlay();
        progressOverlay.setVisible(false);

        StackPane dialogStack = new StackPane(container, progressOverlay);
        dialogStack.getStyleClass().add("help-dialog-container");
        dialogStack.setMinWidth(DIALOG_WIDTH);
        dialogStack.setMaxWidth(DIALOG_WIDTH);

        // Apply styling using DialogStyler utility
        DialogStyler.applyRoundedClip(dialogStack, CORNER_RADIUS);
        StackPane shadowWrapper = DialogStyler.createShadowWrapper(dialogStack);
        DialogStyler.setupStyledDialog(stage, shadowWrapper, STYLESHEET);
        DialogStyler.centerOnOwner(stage);

        LOG.fine("QuarterlyReviewDialog created for " + reviewData.getPeriodHeaderText());
    }

    /**
     * Creates the gradient header with icon, title, and close button.
     */
    private HBox createHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("help-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));

        // Apply status-specific gradient color
        String[] colors = getHeaderGradientColors(isOverdue);
        header.setStyle("-fx-background-color: linear-gradient(to right, " + colors[0] + ", " + colors[1] + ");" +
                        "-fx-background-radius: 11 11 0 0;");

        // Icon circle
        StackPane iconWrapper = new StackPane();
        iconWrapper.setMinSize(40, 40);
        iconWrapper.setMaxSize(40, 40);
        iconWrapper.setAlignment(Pos.CENTER);
        iconWrapper.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20;");

        FontIcon icon = FontIcon.of(isOverdue ? FontAwesomeSolid.EXCLAMATION_TRIANGLE : FontAwesomeSolid.CLIPBOARD_CHECK, 18);
        icon.setIconColor(Color.WHITE);
        iconWrapper.getChildren().add(icon);

        // Title
        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(reviewData.getPeriodHeaderText());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 600;");

        Label subtitleLabel = new Label(reviewData.getDateRangeText());
        subtitleLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;");

        titleBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Close button
        Button closeBtn = new Button("\u2715");
        closeBtn.getStyleClass().add("help-dialog-close");
        closeBtn.setOnAction(e -> stage.close());

        header.getChildren().addAll(iconWrapper, titleBox, spacer, closeBtn);

        return header;
    }

    /**
     * Creates the body content with income, expenses, and net profit.
     */
    private VBox createBody() {
        VBox body = new VBox(16);
        body.getStyleClass().add("help-dialog-body");
        body.setPadding(new Insets(20));
        body.setStyle("-fx-background-color: white;");

        // Income Summary Card
        body.getChildren().add(createIncomeCard());

        // Expenses by Category Card
        body.getChildren().add(createExpensesCard());

        // Net Profit/Loss Card
        body.getChildren().add(createNetProfitCard());

        return body;
    }

    /**
     * Creates the income summary card.
     */
    private VBox createIncomeCard() {
        VBox card = createCard("Income Summary");

        HBox row = createDataRow(
                "Total Income",
                reviewData.getFormattedIncome(),
                reviewData.getIncomeTransactionCount() + " transactions"
        );
        card.getChildren().add(row);

        return card;
    }

    /**
     * Creates the expenses by category card.
     */
    private VBox createExpensesCard() {
        VBox card = createCard("Expenses by SA103 Category");

        Map<ExpenseCategory, CategorySummary> expenses = reviewData.getExpensesByCategory();

        if (expenses.isEmpty()) {
            Label noExpenses = new Label("No expenses recorded");
            noExpenses.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
            card.getChildren().add(noExpenses);
        } else {
            // Display categories in a logical order matching SA103 form
            for (ExpenseCategory category : getDisplayOrderCategories()) {
                CategorySummary summary = expenses.get(category);
                if (summary != null && summary.hasTransactions()) {
                    HBox row = createCategoryRow(category, summary);
                    card.getChildren().add(row);
                }
            }

            // Total expenses row
            card.getChildren().add(new Separator());
            HBox totalRow = createDataRow(
                    "Total Expenses",
                    reviewData.getFormattedExpenses(),
                    reviewData.getExpenseTransactionCount() + " transactions"
            );
            totalRow.setStyle("-fx-font-weight: 600;");
            card.getChildren().add(totalRow);
        }

        return card;
    }

    /**
     * Returns expense categories in display order (matching SA103 form boxes).
     */
    private List<ExpenseCategory> getDisplayOrderCategories() {
        return List.of(
                ExpenseCategory.COST_OF_GOODS,       // Box 17
                ExpenseCategory.SUBCONTRACTOR_COSTS, // Box 18
                ExpenseCategory.STAFF_COSTS,         // Box 19
                ExpenseCategory.TRAVEL,              // Box 20
                ExpenseCategory.TRAVEL_MILEAGE,      // Box 20
                ExpenseCategory.PREMISES,            // Box 21
                ExpenseCategory.REPAIRS,             // Box 22
                ExpenseCategory.OFFICE_COSTS,        // Box 23
                ExpenseCategory.ADVERTISING,         // Box 24
                ExpenseCategory.INTEREST,            // Box 25
                ExpenseCategory.FINANCIAL_CHARGES,   // Box 26
                ExpenseCategory.BAD_DEBTS,           // Box 27
                ExpenseCategory.PROFESSIONAL_FEES,   // Box 28
                ExpenseCategory.OTHER_EXPENSES,      // Box 30
                ExpenseCategory.HOME_OFFICE_SIMPLIFIED // Box 30
        );
    }

    /**
     * Creates a row for an expense category.
     */
    private HBox createCategoryRow(ExpenseCategory category, CategorySummary summary) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        // Category name with box number
        String displayName = category.getShortDisplayName() + " (Box " + category.getSa103Box() + ")";
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Amount
        Label amountLabel = new Label(formatCurrency(summary.amount()));
        amountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #212529; -fx-font-weight: 500;");
        amountLabel.setMinWidth(80);
        amountLabel.setAlignment(Pos.CENTER_RIGHT);

        // Transaction count
        Label countLabel = new Label("(" + summary.transactionCount() + ")");
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
        countLabel.setMinWidth(40);
        countLabel.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(nameLabel, spacer, amountLabel, countLabel);
        return row;
    }

    /**
     * Creates the net profit/loss card.
     */
    private VBox createNetProfitCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));

        BigDecimal netProfit = reviewData.getNetProfit();
        boolean isLoss = netProfit.compareTo(BigDecimal.ZERO) < 0;

        // Background color based on profit/loss
        String bgColor = isLoss ? "#fff5f5" : "#f0fdf4";
        String borderColor = isLoss ? "#feb2b2" : "#86efac";
        card.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;",
                bgColor, borderColor
        ));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Icon
        FontIcon icon = FontIcon.of(isLoss ? FontAwesomeSolid.ARROW_DOWN : FontAwesomeSolid.ARROW_UP, 16);
        icon.setIconColor(isLoss ? Color.web("#dc3545") : Color.web("#28a745"));

        // Label
        Label label = new Label(isLoss ? "Net Loss" : "Net Profit");
        label.setStyle(String.format("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: %s;",
                isLoss ? "#dc3545" : "#28a745"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Amount
        Label amountLabel = new Label(formatCurrency(netProfit.abs()));
        amountLabel.setStyle(String.format("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: %s;",
                isLoss ? "#dc3545" : "#28a745"));

        row.getChildren().addAll(icon, label, spacer, amountLabel);
        card.getChildren().add(row);

        return card;
    }

    /**
     * Creates the confirmations section with checkboxes.
     */
    private VBox createConfirmationsSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16, 20, 8, 20));
        section.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-width: 1 0 0 0;");

        Label heading = new Label("Confirmation");
        heading.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #212529;");
        section.getChildren().add(heading);

        String[] confirmations = getConfirmationTexts(reviewData.isNilReturn());
        for (String text : confirmations) {
            CheckBox checkbox = new CheckBox(text);
            checkbox.setWrapText(true);
            checkbox.setStyle("-fx-font-size: 12px;");
            checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> updateSubmitButtonState());
            confirmationCheckboxes.add(checkbox);
            section.getChildren().add(checkbox);
        }

        return section;
    }

    /**
     * Creates the footer with Cancel and Submit buttons.
     */
    private HBox createFooter() {
        HBox footer = new HBox(12);
        footer.getStyleClass().add("help-dialog-buttons");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 20, 20, 20));

        // Cancel button
        cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: #e9ecef; " +
                "-fx-text-fill: #495057; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 6; " +
                "-fx-cursor: hand;"
        );
        cancelButton.setOnAction(e -> stage.close());

        // Submit button
        submitButton = new Button(getSubmitButtonText(reviewData.isNilReturn()));
        submitButton.setStyle(
                "-fx-background-color: " + (isOverdue ? OVERDUE_GRADIENT[0] : REVIEW_GRADIENT[0]) + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 500; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 6; " +
                "-fx-cursor: hand;"
        );
        submitButton.setDisable(true); // Initially disabled until all checkboxes checked
        submitButton.setOnAction(e -> handleSubmit());

        footer.getChildren().addAll(cancelButton, submitButton);

        return footer;
    }

    /**
     * Creates a styled card container.
     */
    private VBox createCard(String title) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: #f8f9fa; " +
                "-fx-border-color: #e9ecef; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 12;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        card.getChildren().add(titleLabel);

        return card;
    }

    /**
     * Creates a data row with label, value, and optional suffix.
     */
    private HBox createDataRow(String label, String value, String suffix) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 13px; -fx-text-fill: #212529; -fx-font-weight: 500;");

        row.getChildren().addAll(labelNode, spacer, valueNode);

        if (suffix != null && !suffix.isEmpty()) {
            Label suffixNode = new Label(" (" + suffix + ")");
            suffixNode.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
            row.getChildren().add(suffixNode);
        }

        return row;
    }

    /**
     * Updates the submit button enabled state based on checkbox states.
     */
    private void updateSubmitButtonState() {
        boolean allChecked = confirmationCheckboxes.stream().allMatch(CheckBox::isSelected);
        submitButton.setDisable(!allChecked);
    }

    /**
     * Handles the submit button click.
     *
     * <p>If a {@link SubmissionHandler} is configured, runs the submission on a
     * background thread with progress indicator. Otherwise falls back to executing
     * the legacy onSubmitAction callback.</p>
     */
    private void handleSubmit() {
        LOG.info("Submit clicked for " + reviewData.getPeriodHeaderText());

        if (submissionHandler != null) {
            performSubmission();
        } else {
            // Legacy: execute callback if provided (no HMRC submission)
            if (onSubmitAction != null) {
                onSubmitAction.run();
            }
            stage.close();
        }
    }

    /**
     * Performs the actual HMRC submission on a background thread.
     */
    private void performSubmission() {
        // 1. Disable buttons immediately (prevent double-click)
        submitButton.setDisable(true);
        cancelButton.setDisable(true);

        // 2. Show progress overlay
        progressOverlay.setVisible(true);

        // 3. Capture declaration timestamp and hash
        Instant declarationAcceptedAt = Instant.now();
        String declarationTextHash = computeDeclarationHash(reviewData.isNilReturn());

        // 4. Run submission on background thread
        javafx.concurrent.Task<Submission> task = new javafx.concurrent.Task<>() {
            @Override
            protected Submission call() throws Exception {
                return submissionHandler.submit(reviewData, declarationAcceptedAt, declarationTextHash);
            }
        };

        task.setOnSucceeded(event -> {
            progressOverlay.setVisible(false);
            Submission result = task.getValue();
            LOG.info("Submission accepted: " + result.hmrcReference());

            // Execute legacy callback if provided
            if (onSubmitAction != null) {
                onSubmitAction.run();
            }

            showSuccessDialog(result);

            if (onSubmitComplete != null) {
                onSubmitComplete.accept(result);
            }

            stage.close();
        });

        task.setOnFailed(event -> {
            progressOverlay.setVisible(false);
            Throwable exception = task.getException();
            LOG.log(Level.WARNING, "Submission failed for " + reviewData.getPeriodHeaderText(), exception);

            showErrorDialog(exception);

            // Re-enable buttons so user can try again
            submitButton.setDisable(false);
            cancelButton.setDisable(false);
            updateSubmitButtonState(); // Re-check checkboxes state
        });

        Thread submissionThread = new Thread(task);
        submissionThread.setDaemon(true);
        submissionThread.setName("hmrc-submission");
        submissionThread.start();
    }

    // ==================== Progress Overlay ====================

    /**
     * Creates the progress overlay shown during submission.
     *
     * @return a StackPane overlay with spinner and message
     */
    private StackPane createProgressOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(255,255,255,0.85);");

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);

        Label message = new Label("Submitting to HMRC...");
        message.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #495057;");

        Label subMessage = new Label("Please do not close this window");
        subMessage.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

        content.getChildren().addAll(spinner, message, subMessage);
        overlay.getChildren().add(content);

        return overlay;
    }

    // ==================== Success Dialog ====================

    /**
     * Shows a success dialog after HMRC has accepted the submission.
     *
     * @param submission the accepted submission with HMRC reference
     */
    private void showSuccessDialog(Submission submission) {
        Stage successStage = new Stage();
        successStage.initStyle(StageStyle.TRANSPARENT);
        successStage.initModality(Modality.APPLICATION_MODAL);
        successStage.initOwner(stage);
        successStage.setTitle("Submission Accepted");

        VBox container = new VBox(0);
        container.setMinWidth(420);
        container.setMaxWidth(420);

        // Green header with checkmark
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #28a745, #48c664); " +
                         "-fx-background-radius: 11 11 0 0;");

        StackPane iconWrapper = new StackPane();
        iconWrapper.setMinSize(40, 40);
        iconWrapper.setMaxSize(40, 40);
        iconWrapper.setAlignment(Pos.CENTER);
        iconWrapper.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20;");

        FontIcon checkIcon = FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 20);
        checkIcon.setIconColor(Color.WHITE);
        iconWrapper.getChildren().add(checkIcon);

        Label titleLabel = new Label("Submission Accepted");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 600;");

        header.getChildren().addAll(iconWrapper, titleLabel);
        container.getChildren().add(header);

        // Body with submission details
        VBox body = new VBox(12);
        body.setPadding(new Insets(20));
        body.setStyle("-fx-background-color: white;");

        if (submission.hmrcReference() != null) {
            Label refLabel = new Label("HMRC Reference");
            refLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
            Label refValue = new Label(submission.hmrcReference());
            refValue.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #212529;");
            body.getChildren().addAll(refLabel, refValue, new Separator());
        }

        // Period details
        body.getChildren().add(createSuccessRow("Period",
                reviewData.getFullDateRangeText()));
        body.getChildren().add(createSuccessRow("Income",
                reviewData.getFormattedIncome()));
        body.getChildren().add(createSuccessRow("Expenses",
                reviewData.getFormattedExpenses()));
        body.getChildren().add(createSuccessRow("Net Profit",
                reviewData.getFormattedNetProfit()));

        body.getChildren().add(new Separator());

        Label confirmMsg = new Label("Your quarterly update has been submitted to HMRC.");
        confirmMsg.setWrapText(true);
        confirmMsg.setStyle("-fx-font-size: 13px; -fx-text-fill: #28a745; -fx-font-weight: 500;");
        body.getChildren().add(confirmMsg);

        container.getChildren().add(body);

        // Footer with OK button
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 16, 20));

        Button okBtn = new Button("OK");
        okBtn.setStyle(
                "-fx-background-color: #28a745; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 500; " +
                "-fx-padding: 10 30; " +
                "-fx-background-radius: 6; " +
                "-fx-cursor: hand;"
        );
        okBtn.setOnAction(e -> successStage.close());
        footer.getChildren().add(okBtn);
        container.getChildren().add(footer);

        DialogStyler.applyRoundedClip(container, CORNER_RADIUS);
        StackPane shadowWrapper = DialogStyler.createShadowWrapper(container);
        DialogStyler.setupStyledDialog(successStage, shadowWrapper, STYLESHEET);
        DialogStyler.centerOnOwner(successStage);

        successStage.showAndWait();
    }

    /**
     * Creates a label-value row for the success dialog.
     */
    private HBox createSuccessRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
        labelNode.setMinWidth(80);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 13px; -fx-text-fill: #212529; -fx-font-weight: 500;");

        row.getChildren().addAll(labelNode, spacer, valueNode);
        return row;
    }

    // ==================== Error Dialog ====================

    /**
     * Shows an actionable error dialog when submission fails.
     *
     * <p>SE-10E-003: Redesigned to use {@link SubmissionErrorDisplay} for rendering
     * categorised, actionable error information instead of a generic error message.</p>
     *
     * <p>Features:</p>
     * <ul>
     *   <li>Specific title per error type (e.g., "National Insurance Number Not Set")</li>
     *   <li>Compliance banner: "Your return has NOT been submitted to HMRC" (always visible)</li>
     *   <li>User-friendly guidance text</li>
     *   <li>Error code reference for support</li>
     *   <li>Context-sensitive "Open Settings" button for configuration errors</li>
     * </ul>
     *
     * @param exception the exception from the failed submission
     */
    private void showErrorDialog(Throwable exception) {
        SubmissionErrorDisplay display = ERROR_GUIDANCE.buildErrorDisplay(exception);

        Stage errorStage = new Stage();
        errorStage.initStyle(StageStyle.TRANSPARENT);
        errorStage.initModality(Modality.APPLICATION_MODAL);
        errorStage.initOwner(stage);
        errorStage.setTitle(display.title());

        VBox container = new VBox(0);
        container.setMinWidth(420);
        container.setMaxWidth(420);

        // ---- Header with specific title ----
        String headerColor = display.retryable() ? "#e67e22" : "#dc3545";
        String headerEndColor = display.retryable() ? "#f39c12" : "#e4606d";
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, " +
                         headerColor + ", " + headerEndColor + "); " +
                         "-fx-background-radius: 11 11 0 0;");

        StackPane iconWrapper = new StackPane();
        iconWrapper.setMinSize(40, 40);
        iconWrapper.setMaxSize(40, 40);
        iconWrapper.setAlignment(Pos.CENTER);
        iconWrapper.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20;");

        FontIcon warningIcon = FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 20);
        warningIcon.setIconColor(Color.WHITE);
        iconWrapper.getChildren().add(warningIcon);

        Label titleLabel = new Label(display.title());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 600;");
        titleLabel.setWrapText(true);

        header.getChildren().addAll(iconWrapper, titleLabel);
        container.getChildren().add(header);

        // ---- Compliance banner (MANDATORY - always visible) ----
        HBox complianceBanner = new HBox(8);
        complianceBanner.setAlignment(Pos.CENTER_LEFT);
        complianceBanner.setPadding(new Insets(12, 16, 12, 16));
        complianceBanner.setStyle(
                "-fx-background-color: #fff3cd; " +
                "-fx-border-color: #ffc107; " +
                "-fx-border-width: 0 0 0 4;");

        FontIcon bannerIcon = FontIcon.of(FontAwesomeSolid.EXCLAMATION_CIRCLE, 16);
        bannerIcon.setIconColor(Color.web("#856404"));

        Label bannerLabel = new Label("Your return has NOT been submitted to HMRC.");
        bannerLabel.setStyle("-fx-text-fill: #856404; -fx-font-size: 13px; -fx-font-weight: 700;");
        bannerLabel.setWrapText(true);

        complianceBanner.getChildren().addAll(bannerIcon, bannerLabel);
        container.getChildren().add(complianceBanner);

        // ---- Body with error guidance ----
        VBox body = new VBox(12);
        body.setPadding(new Insets(20));
        body.setStyle("-fx-background-color: white;");

        Label messageLabel = new Label(display.message());
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057;");
        body.getChildren().add(messageLabel);

        // Only show guidance label if it's different from the message (avoid duplication)
        if (display.guidance() != null && !display.guidance().equals(display.message())) {
            Label guidanceLabel = new Label(display.guidance());
            guidanceLabel.setWrapText(true);
            guidanceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057;");
            body.getChildren().add(guidanceLabel);
        }

        // Retryable hint (Condition 7 - /jorge): show explicit text when error is temporary
        if (display.retryable()) {
            Label retryHintLabel = new Label(RETRYABLE_HINT_TEXT);
            retryHintLabel.setWrapText(true);
            retryHintLabel.setStyle(
                    "-fx-font-size: 13px; -fx-text-fill: #e67e22; -fx-font-weight: 600;");
            body.getChildren().add(retryHintLabel);
        }

        container.getChildren().add(body);

        // Error code for support reference (standalone section)
        if (display.errorCode() != null) {
            HBox errorCodeRow = new HBox(8);
            errorCodeRow.setAlignment(Pos.CENTER_LEFT);
            errorCodeRow.setPadding(new Insets(10, 20, 10, 20));
            errorCodeRow.setStyle(
                    "-fx-background-color: #f8f9fa; " +
                    "-fx-border-color: #dee2e6; " +
                    "-fx-border-width: 1 0 0 0;");
            Label codeLabel = new Label("Error: " + display.errorCode());
            codeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-family: monospace;");
            errorCodeRow.getChildren().add(codeLabel);
            container.getChildren().add(errorCodeRow);
        }

        // ---- Footer with context-sensitive buttons ----
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 20, 16, 20));

        // "Open Settings" button for settings errors
        if (display.settingsError() && onOpenSettings != null) {
            Button settingsBtn = new Button("Open Settings");
            FontIcon settingsIcon = FontIcon.of(FontAwesomeSolid.COG, 13);
            settingsIcon.setIconColor(Color.WHITE);
            settingsBtn.setGraphic(settingsIcon);
            settingsBtn.setStyle(
                    "-fx-background-color: " + SETTINGS_BUTTON_COLOR + "; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 13px; " +
                    "-fx-font-weight: 600; " +
                    "-fx-padding: 10 20; " +
                    "-fx-background-radius: 6; " +
                    "-fx-cursor: hand;"
            );
            settingsBtn.setOnAction(e -> {
                errorStage.close();
                stage.close();
                onOpenSettings.run();
            });
            footer.getChildren().add(settingsBtn);
        }

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
                "-fx-background-color: #e9ecef; " +
                "-fx-text-fill: #495057; " +
                "-fx-font-size: 13px; " +
                "-fx-padding: 10 20; " +
                "-fx-background-radius: 6; " +
                "-fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> errorStage.close());
        footer.getChildren().add(closeBtn);

        container.getChildren().add(footer);

        DialogStyler.applyRoundedClip(container, CORNER_RADIUS);
        StackPane shadowWrapper = DialogStyler.createShadowWrapper(container);
        DialogStyler.setupStyledDialog(errorStage, shadowWrapper, STYLESHEET);
        DialogStyler.centerOnOwner(errorStage);

        errorStage.showAndWait();
    }
}
