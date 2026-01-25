package uk.selfemploy.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.service.DeadlineNotification;
import uk.selfemploy.ui.service.NotificationPriority;
import uk.selfemploy.ui.util.DialogStyler;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;

/**
 * Custom notification dialog matching /aura's design specification.
 * Uses a Stage instead of Dialog for full control over styling.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Slate gradient header with bell icon</li>
 *   <li>Empty state with large bell icon and helpful message</li>
 *   <li>Deadline reminder card showing next Self Assessment deadline</li>
 *   <li>Teal primary button</li>
 *   <li>Professional muted color palette</li>
 * </ul>
 *
 * @see <a href="docs/designs/notifications-dialog-redesign.md">Design Specification</a>
 */
public class NotificationDialog {

    private static final Logger LOG = Logger.getLogger(NotificationDialog.class.getName());

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final List<DeadlineNotification> notifications;
    private final TaxYear taxYear;
    private final Runnable markAllReadHandler;
    private final Stage stage;

    /**
     * Creates a new notification dialog.
     *
     * @param notifications      list of notifications to display
     * @param taxYear            current tax year for deadline calculation
     * @param markAllReadHandler callback when "Mark All Read" is clicked
     */
    public NotificationDialog(List<DeadlineNotification> notifications,
                              TaxYear taxYear,
                              Runnable markAllReadHandler) {
        this.notifications = notifications;
        this.taxYear = taxYear;
        this.markAllReadHandler = markAllReadHandler;

        this.stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);  // Must be first, before other init methods
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Notifications");

        // Find owner window
        Window owner = Window.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(null);
        if (owner != null) {
            stage.initOwner(owner);
        }

        // Build content
        VBox container = new VBox(0);
        container.getStyleClass().add("notification-dialog-container");

        // Header
        container.getChildren().add(createHeader());

        // Body (empty state or notification list)
        if (notifications.isEmpty()) {
            container.getChildren().add(createEmptyState());
        } else {
            container.getChildren().add(createNotificationList());
        }

        // Footer with buttons
        container.getChildren().add(createFooter());

        // Apply styling using DialogStyler utility (SE-830)
        DialogStyler.applyRoundedClip(container, DialogStyler.CORNER_RADIUS);
        StackPane shadowWrapper = DialogStyler.createShadowWrapper(container);
        DialogStyler.setupStyledDialog(stage, shadowWrapper, "/css/notifications.css");
        DialogStyler.centerOnOwner(stage);

        LOG.fine("NotificationDialog created with " + notifications.size() + " notifications");
    }

    /**
     * Creates the slate gradient header with bell icon and close button.
     */
    private HBox createHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("notification-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));

        // Bell icon
        FontIcon bellIcon = FontIcon.of(FontAwesomeSolid.BELL, 20);
        bellIcon.getStyleClass().add("notification-dialog-icon");

        // Title
        Label title = new Label("Notifications");
        title.getStyleClass().add("notification-dialog-title");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Unread count badge (if any unread)
        long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
        HBox rightSide = new HBox(12);
        rightSide.setAlignment(Pos.CENTER_RIGHT);

        if (unreadCount > 0) {
            Label badge = new Label(String.valueOf(unreadCount));
            badge.getStyleClass().add("notification-badge");
            rightSide.getChildren().add(badge);
        }

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("notification-dialog-close");
        closeBtn.setOnAction(e -> stage.close());
        rightSide.getChildren().add(closeBtn);

        header.getChildren().addAll(bellIcon, title, spacer, rightSide);
        return header;
    }

    /**
     * Creates the empty state with large bell icon, title, and deadline reminder.
     */
    private VBox createEmptyState() {
        VBox emptyState = new VBox(12);
        emptyState.getStyleClass().add("notification-empty-state");
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(24, 20, 16, 20));
        emptyState.setFillWidth(true);
        emptyState.setMinWidth(340);

        // Large bell icon
        FontIcon bellIcon = FontIcon.of(FontAwesomeSolid.BELL, 48);
        bellIcon.getStyleClass().add("notification-empty-icon");

        // Title
        Label title = new Label("All caught up!");
        title.getStyleClass().add("notification-empty-title");
        title.setMinHeight(Region.USE_PREF_SIZE);

        // Subtitle - set max width to prevent truncation
        Label subtitle = new Label("You'll be notified when important\ndeadlines are approaching.");
        subtitle.getStyleClass().add("notification-empty-text");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(300);
        subtitle.setMinHeight(Region.USE_PREF_SIZE);

        emptyState.getChildren().addAll(bellIcon, title, subtitle);

        // Add deadline reminder card if we have a tax year
        if (taxYear != null) {
            VBox deadlineCard = createDeadlineCard();
            VBox.setMargin(deadlineCard, new Insets(16, 0, 0, 0));
            emptyState.getChildren().add(deadlineCard);
        }

        return emptyState;
    }

    /**
     * Creates the deadline reminder card showing next Self Assessment deadline.
     */
    private VBox createDeadlineCard() {
        VBox card = new VBox(4);
        card.getStyleClass().add("notification-deadline-card");
        card.setPadding(new Insets(12, 16, 12, 16));

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Calendar icon
        FontIcon icon = FontIcon.of(FontAwesomeSolid.CALENDAR_ALT, 16);
        icon.getStyleClass().add("notification-deadline-icon");

        // Text content
        VBox textContent = new VBox(2);

        LocalDate deadline = taxYear.onlineFilingDeadline();
        String dateStr = deadline.format(DATE_FORMAT);
        Label dateLabel = new Label(dateStr + " - Self Assessment deadline");
        dateLabel.getStyleClass().add("notification-deadline-text");

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
        String daysText = daysRemaining == 1 ? "1 day remaining" : daysRemaining + " days remaining";
        Label daysLabel = new Label(daysText);
        daysLabel.getStyleClass().add("notification-deadline-subtext");

        textContent.getChildren().addAll(dateLabel, daysLabel);
        row.getChildren().addAll(icon, textContent);
        card.getChildren().add(row);

        return card;
    }

    /**
     * Creates the scrollable notification list.
     */
    private ScrollPane createNotificationList() {
        VBox list = new VBox(0);
        list.getStyleClass().add("notification-list");

        for (DeadlineNotification notification : notifications) {
            list.getChildren().add(createNotificationItem(notification));
        }

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.getStyleClass().add("notification-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setMaxHeight(280);

        return scrollPane;
    }

    /**
     * Creates a single notification item.
     */
    private HBox createNotificationItem(DeadlineNotification notification) {
        HBox item = new HBox(12);
        item.getStyleClass().add("notification-item");
        if (!notification.isRead()) {
            item.getStyleClass().add("notification-item-unread");
        }
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setAlignment(Pos.TOP_LEFT);

        // Unread indicator dot
        if (!notification.isRead()) {
            StackPane dotWrapper = new StackPane();
            dotWrapper.setMinWidth(16);
            dotWrapper.setAlignment(Pos.TOP_CENTER);
            dotWrapper.setPadding(new Insets(6, 0, 0, 0));

            Region dot = new Region();
            dot.getStyleClass().add("notification-unread-dot");
            dotWrapper.getChildren().add(dot);
            item.getChildren().add(dotWrapper);
        }

        // Type icon
        StackPane iconWrapper = new StackPane();
        iconWrapper.getStyleClass().addAll("notification-type-icon", getTypeIconClass(notification.priority()));
        iconWrapper.setMinSize(32, 32);
        iconWrapper.setMaxSize(32, 32);

        FontIcon iconText = FontIcon.of(getTypeIcon(notification.priority()), 14);
        iconText.getStyleClass().add("notification-type-icon-text");
        iconWrapper.getChildren().add(iconText);

        // Content
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label title = new Label(notification.title());
        title.getStyleClass().add("notification-item-title");

        Label body = new Label(notification.message());
        body.getStyleClass().add("notification-item-body");
        body.setWrapText(true);

        Label time = new Label(formatTimeAgo(notification.triggeredAt()));
        time.getStyleClass().add("notification-item-time");

        content.getChildren().addAll(title, body, time);

        item.getChildren().addAll(iconWrapper, content);
        return item;
    }

    /**
     * Creates the footer with buttons.
     */
    private HBox createFooter() {
        HBox footer = new HBox(12);
        footer.getStyleClass().add("notification-dialog-buttons");
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 20, 20, 20));

        // Mark All Read button (only if there are unread notifications)
        boolean hasUnread = notifications.stream().anyMatch(n -> !n.isRead());
        if (hasUnread && markAllReadHandler != null) {
            Button markReadBtn = new Button("Mark All Read");
            markReadBtn.getStyleClass().add("notification-btn-secondary");
            markReadBtn.setOnAction(e -> {
                markAllReadHandler.run();
                stage.close();
            });
            footer.getChildren().add(markReadBtn);
        }

        // OK button
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("notification-btn-primary");
        okBtn.setOnAction(e -> stage.close());
        okBtn.setDefaultButton(true);
        footer.getChildren().add(okBtn);

        return footer;
    }

    /**
     * Returns the icon for a notification priority.
     */
    private Ikon getTypeIcon(NotificationPriority priority) {
        return switch (priority) {
            case LOW -> FontAwesomeSolid.INFO_CIRCLE;
            case MEDIUM -> FontAwesomeSolid.CLIPBOARD;
            case HIGH -> FontAwesomeSolid.EXCLAMATION_TRIANGLE;
            case CRITICAL -> FontAwesomeSolid.EXCLAMATION_CIRCLE;
        };
    }

    /**
     * Returns the CSS class for a notification type icon.
     */
    private String getTypeIconClass(NotificationPriority priority) {
        return switch (priority) {
            case LOW -> "notification-type-info";
            case MEDIUM -> "notification-type-calendar";
            case HIGH -> "notification-type-warning";
            case CRITICAL -> "notification-type-warning";
        };
    }

    /**
     * Formats a timestamp as relative time (e.g., "2h ago", "1d ago").
     */
    private String formatTimeAgo(java.time.LocalDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }

        java.time.Duration duration = java.time.Duration.between(timestamp, java.time.LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else {
            return days + "d ago";
        }
    }

    /**
     * Shows the dialog and waits for it to close.
     */
    public void showDialog() {
        stage.showAndWait();
    }
}
