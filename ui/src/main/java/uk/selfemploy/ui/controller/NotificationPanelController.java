package uk.selfemploy.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import uk.selfemploy.ui.service.DeadlineNotificationService;
import uk.selfemploy.ui.service.NotificationPriority;
import uk.selfemploy.ui.viewmodel.NotificationFilter;
import uk.selfemploy.ui.viewmodel.NotificationItemViewModel;
import uk.selfemploy.ui.viewmodel.NotificationPanelViewModel;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Controller for the notification panel component.
 * Handles user interactions and delegates to ViewModel.
 *
 * <p>SE-502: Desktop Notifications</p>
 */
public class NotificationPanelController implements Initializable {

    private static final Logger LOG = Logger.getLogger(NotificationPanelController.class.getName());

    @FXML private VBox notificationPanel;
    @FXML private Button markAllReadBtn;
    @FXML private Button settingsBtn;
    @FXML private ToggleButton allTab;
    @FXML private ToggleButton unreadTab;
    @FXML private ToggleButton deadlinesTab;
    @FXML private ScrollPane notificationScroll;
    @FXML private VBox notificationList;
    @FXML private VBox emptyState;

    private NotificationPanelViewModel viewModel;
    private Consumer<String> navigationHandler;
    private Runnable settingsHandler;

    private ToggleGroup filterToggleGroup;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Create toggle group for filter tabs
        filterToggleGroup = new ToggleGroup();
        allTab.setToggleGroup(filterToggleGroup);
        unreadTab.setToggleGroup(filterToggleGroup);
        deadlinesTab.setToggleGroup(filterToggleGroup);

        LOG.info("NotificationPanelController initialized");
    }

    /**
     * Initializes the controller with a notification service.
     */
    public void initializeWithService(DeadlineNotificationService notificationService) {
        this.viewModel = new NotificationPanelViewModel(notificationService);

        // Bind visibility
        notificationPanel.visibleProperty().bind(viewModel.panelVisibleProperty());
        notificationPanel.managedProperty().bind(viewModel.panelVisibleProperty());
        emptyState.visibleProperty().bind(viewModel.emptyStateVisibleProperty());
        emptyState.managedProperty().bind(viewModel.emptyStateVisibleProperty());

        // Bind notification list to empty state opposite
        notificationScroll.visibleProperty().bind(viewModel.emptyStateVisibleProperty().not());
        notificationScroll.managedProperty().bind(viewModel.emptyStateVisibleProperty().not());

        // Listen for notification changes
        viewModel.getFilteredNotifications().addListener(
            (javafx.collections.ListChangeListener.Change<? extends NotificationItemViewModel> c) -> {
                refreshNotificationList();
            });

        // Set navigation handler
        viewModel.setNavigationHandler(url -> {
            if (navigationHandler != null) {
                navigationHandler.accept(url);
            }
        });

        LOG.info("NotificationPanelController initialized with service");
    }

    /**
     * Gets the ViewModel for external binding.
     */
    public NotificationPanelViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Sets the navigation handler for when notifications are clicked.
     */
    public void setNavigationHandler(Consumer<String> handler) {
        this.navigationHandler = handler;
        if (viewModel != null) {
            viewModel.setNavigationHandler(handler);
        }
    }

    /**
     * Sets the settings handler for when settings button is clicked.
     */
    public void setSettingsHandler(Runnable handler) {
        this.settingsHandler = handler;
    }

    /**
     * Shows the notification panel.
     */
    public void show() {
        if (viewModel != null) {
            viewModel.showPanel();
            refreshNotificationList();
        }
    }

    /**
     * Hides the notification panel.
     */
    public void hide() {
        if (viewModel != null) {
            viewModel.hidePanel();
        }
    }

    /**
     * Toggles the notification panel visibility.
     */
    public void toggle() {
        if (viewModel != null) {
            viewModel.togglePanel();
            if (viewModel.isPanelVisible()) {
                refreshNotificationList();
            }
        }
    }

    // === FXML Event Handlers ===

    @FXML
    private void handleMarkAllRead() {
        if (viewModel != null) {
            viewModel.markAllAsRead();
            refreshNotificationList();
        }
    }

    @FXML
    private void handleOpenSettings() {
        if (settingsHandler != null) {
            settingsHandler.run();
        }
    }

    @FXML
    private void handleFilterAll() {
        if (viewModel != null) {
            viewModel.setSelectedFilter(NotificationFilter.ALL);
            allTab.setSelected(true);
            refreshNotificationList();
        }
    }

    @FXML
    private void handleFilterUnread() {
        if (viewModel != null) {
            viewModel.setSelectedFilter(NotificationFilter.UNREAD);
            unreadTab.setSelected(true);
            refreshNotificationList();
        }
    }

    @FXML
    private void handleFilterDeadlines() {
        if (viewModel != null) {
            viewModel.setSelectedFilter(NotificationFilter.DEADLINES);
            deadlinesTab.setSelected(true);
            refreshNotificationList();
        }
    }

    @FXML
    private void handleViewHistory() {
        if (navigationHandler != null) {
            navigationHandler.accept("/settings/notifications");
        }
    }

    // === Private Methods ===

    private void refreshNotificationList() {
        if (viewModel == null) return;

        notificationList.getChildren().clear();

        for (NotificationItemViewModel item : viewModel.getFilteredNotifications()) {
            VBox notificationItem = createNotificationItem(item);
            notificationList.getChildren().add(notificationItem);
        }
    }

    private VBox createNotificationItem(NotificationItemViewModel item) {
        VBox container = new VBox();
        container.getStyleClass().addAll("notification-item", item.getPriorityStyleClass());
        if (!item.isRead()) {
            container.getStyleClass().add("unread");
        }
        container.setPadding(new Insets(16));

        // Content row
        HBox contentRow = new HBox(12);
        contentRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // Priority indicator
        VBox priorityIndicator = new VBox();
        priorityIndicator.getStyleClass().add("priority-indicator");
        priorityIndicator.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        Label priorityIcon = new Label(getPriorityIconText(item.priority()));
        priorityIcon.getStyleClass().add("priority-icon");
        priorityIndicator.getChildren().add(priorityIcon);

        // Main content
        VBox mainContent = new VBox(6);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // Title row
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(item.title());
        title.getStyleClass().add("notification-title");
        titleRow.getChildren().add(title);

        if (!item.isRead()) {
            Label unreadDot = new Label();
            unreadDot.getStyleClass().add("unread-dot");
            titleRow.getChildren().add(unreadDot);
        }

        // Message
        Label message = new Label(item.message());
        message.getStyleClass().add("notification-message");
        message.setWrapText(true);

        // Time
        Label time = new Label(NotificationPanelViewModel.formatTimeAgo(item.triggeredAt()));
        time.getStyleClass().add("notification-time");

        mainContent.getChildren().addAll(titleRow, message, time);

        // Action buttons for high priority
        if (item.hasActions()) {
            HBox actions = new HBox(8);
            actions.getStyleClass().add("notification-actions");

            Button actionBtn = new Button("View Details");
            actionBtn.getStyleClass().add("button-primary-small");
            actionBtn.setOnAction(e -> {
                viewModel.onNotificationClick(item.id());
                hide();
            });

            Button snoozeBtn = new Button("Snooze");
            snoozeBtn.getStyleClass().add("button-secondary-small");
            snoozeBtn.setOnAction(e -> showSnoozeMenu(item, snoozeBtn));

            actions.getChildren().addAll(actionBtn, snoozeBtn);
            mainContent.getChildren().add(actions);
        }

        // Dismiss button
        Button dismissBtn = new Button("✕");
        dismissBtn.getStyleClass().add("dismiss-button");
        dismissBtn.setOnAction(e -> {
            viewModel.dismissNotification(item.id());
            refreshNotificationList();
        });

        contentRow.getChildren().addAll(priorityIndicator, mainContent, dismissBtn);
        container.getChildren().add(contentRow);

        // Click handler
        container.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            viewModel.onNotificationClick(item.id());
            if (navigationHandler != null) {
                navigationHandler.accept(item.actionUrl());
                hide();
            }
        });

        return container;
    }

    private String getPriorityIconText(NotificationPriority priority) {
        return switch (priority) {
            case LOW -> "ℹ";
            case MEDIUM -> "⚠";
            case HIGH -> "!";
            case CRITICAL -> "‼";
        };
    }

    private void showSnoozeMenu(NotificationItemViewModel item, Button anchor) {
        ContextMenu snoozeMenu = new ContextMenu();
        snoozeMenu.getStyleClass().add("snooze-menu");

        MenuItem snooze1h = new MenuItem("1 hour");
        snooze1h.setOnAction(e -> {
            viewModel.snooze(item.id(), 1);
            refreshNotificationList();
        });

        MenuItem snooze3h = new MenuItem("3 hours");
        snooze3h.setOnAction(e -> {
            viewModel.snooze(item.id(), 3);
            refreshNotificationList();
        });

        MenuItem snoozeTomorrow = new MenuItem("Tomorrow");
        snoozeTomorrow.setOnAction(e -> {
            viewModel.snooze(item.id(), 24);
            refreshNotificationList();
        });

        MenuItem snoozeWeek = new MenuItem("Next week");
        snoozeWeek.setOnAction(e -> {
            viewModel.snooze(item.id(), 168); // 7 days
            refreshNotificationList();
        });

        snoozeMenu.getItems().addAll(snooze1h, snooze3h, new SeparatorMenuItem(), snoozeTomorrow, snoozeWeek);
        snoozeMenu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }
}
