package uk.selfemploy.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.ui.util.DialogStyler;
import uk.selfemploy.ui.viewmodel.QuarterStatus;
import uk.selfemploy.ui.viewmodel.QuarterViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A custom styled dialog for displaying quarter information.
 *
 * <p>Replaces the standard JavaFX Alert in QuarterlyUpdatesController with a
 * professionally designed dialog that matches the application's visual style.</p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Status-colored gradient header with appropriate icon</li>
 *   <li>Status badge showing DRAFT/OVERDUE/SUBMITTED/FUTURE</li>
 *   <li>Financial card showing Income, Expenses, and Net Profit/Loss</li>
 *   <li>Optional Year-to-Date card with cumulative totals</li>
 *   <li>Deadline countdown card</li>
 *   <li>Contextual action button (Start Review/Submit Now)</li>
 * </ul>
 *
 * <p>Design: /aura (Senior UI/UX Design Architect)</p>
 * <p>Implementation: /james</p>
 *
 * @see QuarterViewModel
 * @see QuarterStatus
 */
public class QuarterInfoDialog {

    private static final Logger LOG = Logger.getLogger(QuarterInfoDialog.class.getName());
    private static final String STYLESHEET = "/css/help.css";

    // ==================== Design Constants ====================

    /**
     * Dialog width in pixels per /aura's design specification.
     */
    public static final int DIALOG_WIDTH = 420;

    /**
     * Corner radius for the dialog container.
     */
    public static final double CORNER_RADIUS = 12.0;

    // ==================== Header Gradient Colors ====================

    private static final String[] DRAFT_GRADIENT = {"#0066cc", "#3385d6"};
    private static final String[] OVERDUE_GRADIENT = {"#dc3545", "#e4606d"};
    private static final String[] SUBMITTED_GRADIENT = {"#28a745", "#48c664"};
    private static final String[] FUTURE_GRADIENT = {"#6c757d", "#868e96"};

    // ==================== Instance Fields ====================

    private final QuarterViewModel viewModel;
    private final BigDecimal ytdIncome;
    private final BigDecimal ytdExpenses;
    private final Stage stage;
    private Runnable onReviewAction;

    // ==================== Constructors ====================

    /**
     * Creates a QuarterInfoDialog without YTD totals.
     *
     * @param viewModel the quarter view model (must not be null)
     */
    public QuarterInfoDialog(QuarterViewModel viewModel) {
        this(viewModel, null, null);
    }

    /**
     * Creates a QuarterInfoDialog with optional YTD totals.
     *
     * @param viewModel the quarter view model (must not be null)
     * @param ytdIncome year-to-date income (may be null)
     * @param ytdExpenses year-to-date expenses (may be null)
     */
    public QuarterInfoDialog(QuarterViewModel viewModel, BigDecimal ytdIncome, BigDecimal ytdExpenses) {
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel must not be null");
        this.ytdIncome = ytdIncome;
        this.ytdExpenses = ytdExpenses;

        this.stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);  // Must be first
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(getDialogTitle(viewModel));

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
     * Sets the callback for the action button (Start Review or Submit Now).
     *
     * @param action the callback to execute when the action button is clicked
     */
    public void setOnReviewAction(Runnable action) {
        this.onReviewAction = action;
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
     * Returns the header gradient colors for a given status.
     *
     * @param status the quarter status
     * @return array of [startColor, endColor] hex values
     */
    public static String[] getHeaderGradientColors(QuarterStatus status) {
        return switch (status) {
            case DRAFT -> DRAFT_GRADIENT;
            case OVERDUE -> OVERDUE_GRADIENT;
            case SUBMITTED -> SUBMITTED_GRADIENT;
            case FUTURE -> FUTURE_GRADIENT;
        };
    }

    /**
     * Returns the icon name for a given status.
     *
     * @param status the quarter status
     * @return the FontAwesome icon name
     */
    public static String getIconNameForStatus(QuarterStatus status) {
        return switch (status) {
            case DRAFT -> "EDIT";
            case OVERDUE -> "EXCLAMATION_TRIANGLE";
            case SUBMITTED -> "CHECK_CIRCLE";
            case FUTURE -> "CLOCK";
        };
    }

    /**
     * Returns the FontAwesome icon for a given status.
     *
     * @param status the quarter status
     * @return the FontAwesome icon
     */
    private static Ikon getIconForStatus(QuarterStatus status) {
        return switch (status) {
            case DRAFT -> FontAwesomeSolid.EDIT;
            case OVERDUE -> FontAwesomeSolid.EXCLAMATION_TRIANGLE;
            case SUBMITTED -> FontAwesomeSolid.CHECK_CIRCLE;
            case FUTURE -> FontAwesomeSolid.CLOCK;
        };
    }

    /**
     * Returns the styling for net profit/loss based on the value.
     *
     * @param netValue the net profit (positive) or loss (negative)
     * @return CSS style string
     */
    public static String getNetProfitStyle(BigDecimal netValue) {
        if (netValue == null) {
            return "-fx-text-fill: #6c757d;";
        }
        if (netValue.compareTo(BigDecimal.ZERO) >= 0) {
            return "-fx-text-fill: #28a745; -fx-font-weight: 600;"; // Green for profit
        } else {
            return "-fx-text-fill: #dc3545; -fx-font-weight: 600;"; // Red for loss
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
            return "--";
        }
        return String.format("\u00A3%,.2f", amount);
    }

    /**
     * Returns the deadline countdown text.
     *
     * @param deadline the deadline date
     * @return human-readable countdown text
     */
    public static String getDeadlineCountdownText(LocalDate deadline) {
        if (deadline == null) {
            return "";
        }

        LocalDate today = LocalDate.now();
        long daysUntil = ChronoUnit.DAYS.between(today, deadline);

        if (daysUntil == 0) {
            return "Due today";
        } else if (daysUntil == 1) {
            return "1 day remaining";
        } else if (daysUntil > 1) {
            return daysUntil + " days remaining";
        } else if (daysUntil == -1) {
            return "1 day overdue";
        } else {
            return Math.abs(daysUntil) + " days overdue";
        }
    }

    /**
     * Returns the description text for a status.
     *
     * @param status the quarter status
     * @return description text
     */
    public static String getStatusDescription(QuarterStatus status) {
        return switch (status) {
            case DRAFT -> "Review your income and expenses before submitting to HMRC.";
            case OVERDUE -> "This quarter's deadline has passed. Please submit as soon as possible.";
            case SUBMITTED -> "This quarter has been successfully submitted to HMRC.";
            case FUTURE -> "This quarter has not yet started. No action needed yet.";
        };
    }

    /**
     * Returns the action button text for a status.
     *
     * @param status the quarter status
     * @return button text, or null if no action button
     */
    public static String getActionButtonText(QuarterStatus status) {
        return switch (status) {
            case DRAFT -> "Start Review";
            case OVERDUE -> "Submit Now";
            case SUBMITTED, FUTURE -> null;
        };
    }

    /**
     * Returns whether an action button should be shown for a status.
     *
     * @param status the quarter status
     * @return true if an action button should be shown
     */
    public static boolean shouldShowActionButton(QuarterStatus status) {
        return status == QuarterStatus.DRAFT || status == QuarterStatus.OVERDUE;
    }

    /**
     * Returns the dialog title for a quarter view model.
     *
     * @param viewModel the quarter view model
     * @return the dialog title
     */
    public static String getDialogTitle(QuarterViewModel viewModel) {
        return viewModel.getQuarter().name() + " - " + viewModel.getStatus().getDisplayText();
    }

    /**
     * Calculates year-to-date income up to and including a given quarter.
     *
     * @param quarter the quarter up to which to calculate
     * @param q1 Q1 income (may be null)
     * @param q2 Q2 income (may be null)
     * @param q3 Q3 income (may be null)
     * @param q4 Q4 income (may be null)
     * @return the YTD total
     */
    public static BigDecimal calculateYtdIncome(Quarter quarter, BigDecimal q1, BigDecimal q2,
                                                 BigDecimal q3, BigDecimal q4) {
        BigDecimal total = BigDecimal.ZERO;

        if (q1 != null) {
            total = total.add(q1);
        }
        if (quarter == Quarter.Q1) {
            return total;
        }

        if (q2 != null) {
            total = total.add(q2);
        }
        if (quarter == Quarter.Q2) {
            return total;
        }

        if (q3 != null) {
            total = total.add(q3);
        }
        if (quarter == Quarter.Q3) {
            return total;
        }

        if (q4 != null) {
            total = total.add(q4);
        }
        return total;
    }

    // ==================== Private Methods ====================

    private void initializeDialog() {
        VBox container = new VBox(0);
        container.getStyleClass().add("help-dialog-container");
        container.setMinWidth(DIALOG_WIDTH);
        container.setMaxWidth(DIALOG_WIDTH);

        // Header with status-colored gradient
        container.getChildren().add(createHeader());

        // Body content
        container.getChildren().add(createBody());

        // Footer with buttons
        container.getChildren().add(createFooter());

        // Apply styling using DialogStyler utility
        DialogStyler.applyRoundedClip(container, CORNER_RADIUS);
        StackPane shadowWrapper = DialogStyler.createShadowWrapper(container);
        DialogStyler.setupStyledDialog(stage, shadowWrapper, STYLESHEET);
        DialogStyler.centerOnOwner(stage);

        LOG.fine("QuarterInfoDialog created for " + viewModel.getQuarter());
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
        String[] colors = getHeaderGradientColors(viewModel.getStatus());
        header.setStyle("-fx-background-color: linear-gradient(to right, " + colors[0] + ", " + colors[1] + ");" +
                        "-fx-background-radius: 11 11 0 0;");

        // Icon circle with FontIcon
        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().add("help-dialog-icon-wrapper");
        iconWrapper.setMinSize(40, 40);
        iconWrapper.setMaxSize(40, 40);
        iconWrapper.setAlignment(Pos.CENTER);
        iconWrapper.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20;");

        FontIcon fontIcon = FontIcon.of(getIconForStatus(viewModel.getStatus()), 18);
        fontIcon.setIconColor(Color.WHITE);
        iconWrapper.getChildren().add(fontIcon);

        // Title - Quarter label and date range
        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(viewModel.getQuarter().name() + " " + viewModel.getTaxYear().label());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 600;");

        Label subtitleLabel = new Label(viewModel.getDateRangeText());
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
     * Creates the body content with status badge, description, and financial cards.
     */
    private VBox createBody() {
        VBox body = new VBox(16);
        body.getStyleClass().add("help-dialog-body");
        body.setPadding(new Insets(20));
        body.setStyle("-fx-background-color: white;");

        // Status badge
        HBox badgeRow = new HBox();
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label statusBadge = createStatusBadge();
        badgeRow.getChildren().add(statusBadge);
        body.getChildren().add(badgeRow);

        // Status description
        Label descriptionLabel = new Label(getStatusDescription(viewModel.getStatus()));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");
        body.getChildren().add(descriptionLabel);

        // Quarter Financial Card
        if (viewModel.hasData()) {
            body.getChildren().add(createFinancialCard(
                    viewModel.getQuarter().name() + " Period Totals",
                    viewModel.getTotalIncome(),
                    viewModel.getTotalExpenses()
            ));
        }

        // Year-to-Date Card (optional)
        if (ytdIncome != null || ytdExpenses != null) {
            body.getChildren().add(createFinancialCard(
                    "Year-to-Date",
                    ytdIncome,
                    ytdExpenses
            ));
        }

        // Deadline Card
        body.getChildren().add(createDeadlineCard());

        return body;
    }

    /**
     * Creates a status badge label.
     */
    private Label createStatusBadge() {
        QuarterStatus status = viewModel.getStatus();
        Label badge = new Label(status.getDisplayText().toUpperCase());
        badge.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-text-fill: %s; " +
                "-fx-padding: 4 12; " +
                "-fx-background-radius: 12; " +
                "-fx-font-size: 11px; " +
                "-fx-font-weight: 600;",
                status.getBackgroundColor(),
                status.getTextColor()
        ));
        return badge;
    }

    /**
     * Creates a financial summary card.
     */
    private VBox createFinancialCard(String title, BigDecimal income, BigDecimal expenses) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: #f8f9fa; " +
                "-fx-border-color: #e9ecef; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 12;"
        );

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #495057;");

        // Income row
        HBox incomeRow = createFinancialRow("Income", formatCurrency(income), "#212529");

        // Expenses row
        HBox expensesRow = createFinancialRow("Expenses", formatCurrency(expenses), "#212529");

        // Net row
        BigDecimal net = null;
        if (income != null && expenses != null) {
            net = income.subtract(expenses);
        }
        HBox netRow = createFinancialRow("Net Profit", formatCurrency(net), null);
        // Apply conditional styling for net value (index 2: label=0, spacer=1, value=2)
        Label netValue = (Label) netRow.getChildren().get(2);
        netValue.setStyle(getNetProfitStyle(net));

        card.getChildren().addAll(titleLabel, incomeRow, expensesRow, netRow);

        return card;
    }

    /**
     * Creates a row for financial data display.
     */
    private HBox createFinancialRow(String label, String value, String valueColor) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueNode = new Label(value);
        if (valueColor != null) {
            valueNode.setStyle("-fx-font-size: 13px; -fx-text-fill: " + valueColor + "; -fx-font-weight: 500;");
        }

        row.getChildren().addAll(labelNode, spacer, valueNode);

        return row;
    }

    /**
     * Creates the deadline countdown card.
     */
    private VBox createDeadlineCard() {
        VBox card = new VBox(4);

        QuarterStatus status = viewModel.getStatus();
        boolean isOverdue = status == QuarterStatus.OVERDUE;

        card.setStyle(String.format(
                "-fx-background-color: %s; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 12;",
                isOverdue ? "#fff5f5" : "#f0fdf4",
                isOverdue ? "#feb2b2" : "#86efac"
        ));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Calendar icon
        FontIcon icon = FontIcon.of(FontAwesomeSolid.CALENDAR_ALT, 16);
        icon.setIconColor(isOverdue ? Color.web("#dc3545") : Color.web("#22c55e"));

        // Text content
        VBox textContent = new VBox(2);

        Label dateLabel = new Label(viewModel.getDeadlineText());
        dateLabel.setStyle(String.format("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: %s;",
                isOverdue ? "#dc3545" : "#166534"));

        String countdownText = getDeadlineCountdownText(viewModel.getDeadline());
        Label countdownLabel = new Label(countdownText);
        countdownLabel.setStyle(String.format("-fx-font-size: 12px; -fx-text-fill: %s;",
                isOverdue ? "#ef4444" : "#22c55e"));

        textContent.getChildren().addAll(dateLabel, countdownLabel);
        row.getChildren().addAll(icon, textContent);
        card.getChildren().add(row);

        return card;
    }

    /**
     * Creates the footer with OK and optional action button.
     */
    private HBox createFooter() {
        HBox footer = new HBox(12);
        footer.getStyleClass().add("help-dialog-buttons");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 20, 20, 20));

        // Action button (if applicable)
        String actionText = getActionButtonText(viewModel.getStatus());
        if (actionText != null) {
            Button actionBtn = new Button(actionText);
            actionBtn.setStyle(
                    "-fx-background-color: " + getHeaderGradientColors(viewModel.getStatus())[0] + "; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 13px; " +
                    "-fx-font-weight: 500; " +
                    "-fx-padding: 10 20; " +
                    "-fx-background-radius: 6; " +
                    "-fx-cursor: hand;"
            );
            actionBtn.setOnAction(e -> {
                if (onReviewAction != null) {
                    onReviewAction.run();
                }
                stage.close();
            });
            footer.getChildren().add(actionBtn);
        }

        // OK button
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("help-btn-primary");
        okBtn.setOnAction(e -> stage.close());
        okBtn.setDefaultButton(true);
        footer.getChildren().add(okBtn);

        return footer;
    }
}
