package uk.selfemploy.ui.controller;
import uk.selfemploy.ui.component.AppDialog;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.util.StringConverter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.component.TourOverlay;
import uk.selfemploy.ui.viewmodel.TourViewModel;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.DeadlineNotificationService;
import uk.selfemploy.ui.service.ImportHistoryCoordinator;
import uk.selfemploy.ui.service.ReconciliationCoordinator;
import uk.selfemploy.ui.service.SqliteBankTransactionRepository;
import uk.selfemploy.ui.service.SqliteImportAuditRepository;
import uk.selfemploy.ui.service.SqliteNotificationStateRepository;
import uk.selfemploy.ui.service.SqliteReconciliationMatchRepository;
import uk.selfemploy.ui.service.SubmittedPeriodIndex;
import uk.selfemploy.ui.viewmodel.NavigationViewModel;
import uk.selfemploy.ui.viewmodel.View;
import uk.selfemploy.ui.i18n.Messages;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.UUID;

/**
 * Main controller for the application's root layout.
 * Manages navigation, tax year selection, and content view switching.
 */
public class MainController implements Initializable {

    private static final Logger LOG = Logger.getLogger(MainController.class.getName());

    @FXML private StackPane rootStack;
    @FXML private StackPane contentPane;
    @FXML private ToggleGroup navGroup;
    @FXML private ToggleButton navDashboard;
    @FXML private ToggleButton navIncome;
    @FXML private ToggleButton navExpenses;
    @FXML private ToggleButton navBank;
    @FXML private ToggleButton navTax;
    @FXML private ToggleButton navHmrc;
    @FXML private ComboBox<TaxYear> taxYearSelector;
    @FXML private Label taxYearLabel;
    @FXML private Label deadlineLabel;

    // Notification Bell (SE-309)
    @FXML private StackPane notificationBell;
    @FXML private Button notificationButton;
    @FXML private Label notificationBadge;

    private final NavigationViewModel navigationViewModel = new NavigationViewModel();
    private final Map<View, Node> viewCache = new HashMap<>();
    private final Map<View, Object> controllerCache = new HashMap<>();
    private final DeadlineNotificationService notificationService = new DeadlineNotificationService();
    private NotificationPanelController notificationPanelController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTaxYearSelector();
        setupNavigationBindings();
        setupNotifications();

        // Load dashboard by default
        loadView(View.DASHBOARD);
    }

    private void setupNotifications() {
        // Bind badge visibility to unread count
        notificationService.unreadCountProperty().addListener((obs, oldVal, newVal) -> {
            int count = newVal.intValue();
            notificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
            notificationBadge.setVisible(count > 0);
            notificationBadge.setManaged(count > 0);
        });

        // Persist read/snooze state so dismissed reminders don't re-nag after a restart.
        notificationService.setStateStore(new SqliteNotificationStateRepository());

        // Start notification scheduler for current tax year
        TaxYear currentYear = navigationViewModel.getSelectedTaxYear();
        if (currentYear != null) {
            notificationService.startScheduler(currentYear);
        }

        setupNotificationPanel();

        LOG.info("Notification service initialized");
    }

    /**
     * Loads the notification flyout panel and hosts it as a top-right overlay on the root stack,
     * so the bell opens the full panel (filters, snooze, dismiss) instead of a one-shot dialog.
     */
    private void setupNotificationPanel() {
        try {
            FXMLLoader loader = Messages.loader(getClass().getResource("/fxml/notification-panel.fxml"));
            Node panel = loader.load();
            notificationPanelController = loader.getController();
            notificationPanelController.initializeWithService(notificationService);
            notificationPanelController.setNavigationHandler(this::navigateFromNotification);
            notificationPanelController.setSettingsHandler(() -> {
                notificationPanelController.hide();
                loadView(View.SETTINGS);
            });

            StackPane.setAlignment(panel, Pos.TOP_RIGHT);
            StackPane.setMargin(panel, new Insets(64, 12, 12, 12)); // sit below the header bar
            rootStack.getChildren().add(panel);
        } catch (IOException e) {
            LOG.severe("Failed to load notification panel: " + e.getMessage());
        }
    }

    /** Routes a notification's deep-link action URL to the matching view, then closes the panel. */
    private void navigateFromNotification(String url) {
        if (notificationPanelController != null) {
            notificationPanelController.hide();
        }
        if (url == null || url.isBlank()) {
            return;
        }
        if (url.startsWith("/settings")) {
            loadView(View.SETTINGS);
        } else if (url.startsWith("/submission")) {
            loadView(View.HMRC_SUBMISSION);
        } else if (url.startsWith("/tax-summary")) {
            loadView(View.TAX_SUMMARY);
        } else {
            loadView(View.DASHBOARD);
        }
    }

    private void setupTaxYearSelector() {
        taxYearSelector.getItems().addAll(navigationViewModel.getAvailableTaxYears());
        taxYearSelector.setValue(navigationViewModel.getSelectedTaxYear());

        taxYearSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(TaxYear year) {
                return year != null ? year.label() : "";
            }

            @Override
            public TaxYear fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });

        taxYearSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                navigationViewModel.setSelectedTaxYear(newVal);
                updateStatusBar();
                rescopeAllViewsToTaxYear(newVal);
                refreshCurrentView();
                // Restart notification scheduler for new tax year
                notificationService.startScheduler(newVal);
            }
        });

        updateStatusBar();
    }

    private void setupNavigationBindings() {
        // Bind navigation state to toggle buttons
        navigationViewModel.currentViewProperty().addListener((obs, oldView, newView) -> {
            selectNavButton(newView);
        });
    }

    private void selectNavButton(View view) {
        switch (view) {
            case DASHBOARD -> navDashboard.setSelected(true);
            case INCOME -> navIncome.setSelected(true);
            case EXPENSES -> navExpenses.setSelected(true);
            case BANK, TRANSACTION_REVIEW, RECONCILIATION, IMPORT_HISTORY -> navBank.setSelected(true);
            case TAX_SUMMARY -> navTax.setSelected(true);
            case HMRC_SUBMISSION -> navHmrc.setSelected(true);
            default -> {} // Help and Settings don't have nav buttons
        }
    }

    private void updateStatusBar() {
        TaxYear year = navigationViewModel.getSelectedTaxYear();
        if (year != null) {
            taxYearLabel.setText("Tax Year " + year.label());

            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), year.onlineFilingDeadline());
            if (daysUntil > 0) {
                deadlineLabel.setText(daysUntil + " days until filing deadline");
            } else if (daysUntil == 0) {
                deadlineLabel.setText("Filing deadline is TODAY!");
            } else {
                deadlineLabel.setText("Filing deadline has passed");
            }
        }
    }

    private void loadView(View view) {
        navigationViewModel.navigateTo(view);

        Node viewNode = viewCache.get(view);
        Object controller = controllerCache.get(view);

        if (viewNode == null) {
            try {
                FXMLLoader loader = Messages.loader(getClass().getResource(view.getFxmlPath()));
                viewNode = loader.load();

                // Get and cache controller
                controller = loader.getController();
                controllerCache.put(view, controller);

                // Pass tax year to controller if it supports it
                if (controller instanceof TaxYearAware taxYearAware) {
                    taxYearAware.setTaxYear(navigationViewModel.getSelectedTaxYear());
                }

                // Set navigation callbacks for DashboardController (SE-BUG: View Tax Breakdown button)
                if (controller instanceof DashboardController dashboardController) {
                    dashboardController.setNavigationCallbacks(
                        () -> loadView(View.INCOME),
                        () -> loadView(View.EXPENSES),
                        () -> loadView(View.TAX_SUMMARY)
                    );
                }

                // Wire "Open Settings" callback for HMRC submission error dialogs
                if (controller instanceof HmrcSubmissionController hmrcController) {
                    hmrcController.setNavigateToSettings(() -> loadView(View.SETTINGS));
                }

                // Wire empty-state calls to action for the Tax Summary screen
                if (controller instanceof TaxSummaryController taxSummaryController) {
                    taxSummaryController.setNavigationCallbacks(
                        () -> loadView(View.INCOME),
                        () -> loadView(View.EXPENSES)
                    );
                }

                // Wire "Replay tour" on the Help screen
                if (controller instanceof HelpController helpController) {
                    helpController.setOnReplayTour(this::startTour);
                }

                // Wire post-import navigation callback for Income and Expense controllers
                if (controller instanceof IncomeController incomeController) {
                    incomeController.setNavigateToTransactionReview(
                        this::navigateToTransactionReviewWithMessage);
                }
                if (controller instanceof ExpenseController expenseController) {
                    expenseController.setNavigateToTransactionReview(
                        this::navigateToTransactionReviewWithMessage);
                }

                // Wire the reconciliation dashboard to real data + deep-link its quick actions.
                if (controller instanceof ReconciliationDashboardController reconController) {
                    wireReconciliationDashboard(reconController);
                }

                // Wire the import history screen to the real audit trail + undo.
                if (controller instanceof ImportHistoryController importHistoryController) {
                    wireImportHistory(importHistoryController);
                }

                // Wire the Bank section: its three embedded tabs (review, imports, records check).
                if (controller instanceof BankController bankController) {
                    wireBankSection(bankController);
                }

                viewCache.put(view, viewNode);
            } catch (IOException e) {
                showError("Failed to load view: " + view.getTitle(), e);
                return;
            }
        } else {
            // Refresh data when loading from cache
            if (controller instanceof Refreshable refreshable) {
                refreshable.refreshData();
            }
        }

        contentPane.getChildren().setAll(viewNode);
    }

    /**
     * Opens the Bank section on its Review Transactions tab, reloads it, and shows an import success
     * banner scoped to the just-imported batch. Called after a bank statement import from elsewhere.
     *
     * @param message the success message to display on the Review tab
     * @param batchId the imported batch to scope the review to
     */
    private void navigateToTransactionReviewWithMessage(String message, UUID batchId) {
        loadView(View.BANK);
        BankController bank = bankController();
        if (bank != null) {
            bank.selectTab(BankController.REVIEW_TAB);
            TransactionReviewController review = bank.getReviewController();
            if (review != null) {
                // showImportSuccessBanner scopes to the batch and reloads, so no extra refresh here.
                review.showImportSuccessBanner(message, batchId);
            }
        }
    }

    private BankController bankController() {
        return controllerCache.get(View.BANK) instanceof BankController b ? b : null;
    }

    private void refreshCurrentView() {
        View currentView = navigationViewModel.getCurrentView();

        // Preserve the selected Bank tab across the rebuild (a tax-year change discards the view).
        int bankTab = currentView == View.BANK && bankController() != null
            ? bankController().getSelectedTab() : -1;

        viewCache.remove(currentView);
        controllerCache.remove(currentView);
        loadView(currentView);

        if (bankTab >= 0 && bankController() != null) {
            bankController().selectTab(bankTab);
        }
    }

    /**
     * Re-scopes every already-loaded screen to the newly selected tax year.
     *
     * <p>Screens are cached after first load, so without this only the visible screen
     * (refreshed separately) would pick up the new year; other cached screens would keep
     * showing the previous year's data the next time they are shown. The currently visible
     * view is skipped here because {@link #refreshCurrentView()} rebuilds it fresh.</p>
     */
    private void rescopeAllViewsToTaxYear(TaxYear taxYear) {
        View currentView = navigationViewModel.getCurrentView();
        for (Map.Entry<View, Object> entry : controllerCache.entrySet()) {
            if (entry.getKey() == currentView) {
                continue;
            }
            if (entry.getValue() instanceof TaxYearAware taxYearAware) {
                taxYearAware.setTaxYear(taxYear);
            }
        }
    }

    private void showError(String message, Exception e) {
        AppDialog.error("Error", message + (e != null && e.getMessage() != null ? "\n\n" + e.getMessage() : ""));
    }

    // === Navigation Handlers ===

    @FXML
    void navigateToDashboard(ActionEvent event) {
        loadView(View.DASHBOARD);
    }

    @FXML
    void navigateToIncome(ActionEvent event) {
        loadView(View.INCOME);
    }

    @FXML
    void navigateToExpenses(ActionEvent event) {
        loadView(View.EXPENSES);
    }

    /**
     * Opens the Bank section from the sidebar, landing on the Review tab and clearing any batch
     * scope a prior import left on it.
     */
    @FXML
    void navigateToBank(ActionEvent event) {
        // On first load the Review tab is populated by setTaxYear; only a re-entry needs to clear a
        // lingering batch scope and reload.
        boolean firstLoad = !controllerCache.containsKey(View.BANK);
        loadView(View.BANK);
        BankController bank = bankController();
        if (bank != null) {
            bank.selectTab(BankController.REVIEW_TAB);
            if (!firstLoad) {
                TransactionReviewController review = bank.getReviewController();
                if (review != null) {
                    review.showAllTransactions();
                }
            }
        }
    }

    /** Selects a tab within the (already visible) Bank section — used by deep-link actions. */
    private void showBankTab(int tabIndex) {
        BankController bank = bankController();
        if (bank != null) {
            bank.selectTab(tabIndex);
        }
    }

    // A single shared worker so import-history queries run off the FX thread (keeping the UI
    // responsive) and reuse one SQLite connection rather than leaking one per spawned thread.
    private static final java.util.concurrent.ExecutorService IMPORT_HISTORY_WORKER =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "import-history-worker");
            t.setDaemon(true);
            return t;
        });

    private void wireBankSection(BankController bank) {
        wireReconciliationDashboard(bank.getRecordsCheckController());
        wireImportHistory(bank.getImportsController());
        bank.setOnImportStatement(() -> {
            Window owner = rootStack.getScene() != null ? rootStack.getScene().getWindow() : null;
            BankImportLauncher.launch(owner, (message, batchId) -> {
                bank.selectTab(BankController.REVIEW_TAB);
                TransactionReviewController review = bank.getReviewController();
                if (review != null) {
                    // showImportSuccessBanner scopes to the batch and reloads.
                    review.showImportSuccessBanner(message, batchId);
                }
            });
        });

        // Refresh a tab's data whenever it becomes selected, so an action on one tab (e.g. an undo)
        // can't leave stale rows on another. The initial Review tab is already loaded via setTaxYear,
        // and the other tabs load lazily when first selected.
        bank.setOnTabSelected(tabIndex -> refreshBankTab(bank, tabIndex));
    }

    private void refreshBankTab(BankController bank, int tabIndex) {
        switch (tabIndex) {
            case BankController.REVIEW_TAB -> {
                TransactionReviewController review = bank.getReviewController();
                if (review != null) {
                    review.refreshData();
                }
            }
            case BankController.IMPORTS_TAB -> loadImportHistoryAsync(bank.getImportsController());
            case BankController.RECORDS_CHECK_TAB -> {
                ReconciliationDashboardController records = bank.getRecordsCheckController();
                if (records != null) {
                    records.refresh();
                }
            }
            default -> { }
        }
    }

    private ImportHistoryCoordinator newImportHistoryCoordinator() {
        UUID businessId = CoreServiceFactory.getDefaultBusinessId();
        return new ImportHistoryCoordinator(
            businessId,
            new SqliteImportAuditRepository(),
            new SqliteBankTransactionRepository(businessId),
            SubmittedPeriodIndex.forBusiness(businessId));
    }

    private void loadImportHistoryAsync(ImportHistoryController controller) {
        IMPORT_HISTORY_WORKER.submit(() -> {
            try {
                var items = newImportHistoryCoordinator().loadHistory();
                Platform.runLater(() -> controller.setImports(items));
            } catch (Exception e) {
                LOG.severe("Failed to load import history: " + e.getMessage());
            }
        });
    }

    private void wireImportHistory(ImportHistoryController controller) {
        // The list is loaded lazily when the Imports tab is shown (see refreshBankTab), so no eager
        // query here.

        // Wire the "New Import" / empty-state buttons (previously inert): open the wizard, then
        // refresh the history and land on Bank Review scoped to the just-imported batch.
        controller.setOnNewImport(() -> {
            Window owner = rootStack.getScene() != null ? rootStack.getScene().getWindow() : null;
            BankImportLauncher.launch(owner, (message, batchId) -> {
                loadImportHistoryAsync(controller);
                navigateToTransactionReviewWithMessage(message, batchId);
            });
        });

        controller.setOnUndoImport(item -> IMPORT_HISTORY_WORKER.submit(() -> {
            ImportHistoryCoordinator.UndoResult result;
            try {
                result = newImportHistoryCoordinator().undo(item.getId());
            } catch (Exception e) {
                LOG.warning("Undo import failed: " + e.getMessage());
                Platform.runLater(() -> AppDialog.error("Cannot undo import",
                    "Something went wrong. Please try again."));
                loadImportHistoryAsync(controller);
                return;
            }
            Platform.runLater(() -> {
                if (result.success()) {
                    AppDialog.info("Import undone", result.message());
                } else {
                    AppDialog.error("Cannot undo import", result.message());
                }
            });
            loadImportHistoryAsync(controller);
        }));
    }

    private void wireReconciliationDashboard(ReconciliationDashboardController reconController) {
        UUID businessId = CoreServiceFactory.getDefaultBusinessId();
        ReconciliationCoordinator coordinator = new ReconciliationCoordinator(
            businessId,
            CoreServiceFactory.getIncomeService(),
            CoreServiceFactory.getExpenseService(),
            new SqliteReconciliationMatchRepository(),
            () -> new SqliteBankTransactionRepository(businessId).findAll());

        reconController.setOnViewIncome(() -> loadView(View.INCOME));
        reconController.setOnViewExpenses(() -> loadView(View.EXPENSES));
        reconController.setOnReviewDuplicates(() -> showBankTab(BankController.REVIEW_TAB));
        reconController.setOnFixCategories(() -> loadView(View.EXPENSES));
        reconController.setOnCheckGaps(() -> showBankTab(BankController.REVIEW_TAB));
        reconController.setCoordinator(coordinator); // the run happens lazily when the tab is shown
    }

    @FXML
    void navigateToTax(ActionEvent event) {
        loadView(View.TAX_SUMMARY);
    }

    @FXML
    void navigateToHmrc(ActionEvent event) {
        loadView(View.HMRC_SUBMISSION);
    }

    @FXML
    void handleSettings(ActionEvent event) {
        loadView(View.SETTINGS);
    }

    @FXML
    void handleHelp(ActionEvent event) {
        loadView(View.HELP);
    }

    /**
     * Runs the guided product tour over the sidebar navigation. Safe to call repeatedly (from
     * first-run onboarding and from the Help screen's "Replay tour"); a running tour is replaced.
     */
    public void startTour() {
        if (rootStack == null) {
            return;
        }
        // Remove any overlay already showing so a replay starts cleanly.
        rootStack.getChildren().removeIf(node -> node instanceof TourOverlay);

        TourViewModel viewModel = TourViewModel.defaultTour();
        viewModel.start();

        AtomicReference<TourOverlay> ref = new AtomicReference<>();
        TourOverlay overlay = new TourOverlay(viewModel, rootStack,
            () -> rootStack.getChildren().remove(ref.get()));
        ref.set(overlay);
        rootStack.getChildren().add(overlay);
        Platform.runLater(overlay::requestFocus);
    }

    @FXML
    void handleNotificationsAction(ActionEvent event) {
        showNotificationPanel();
    }

    @FXML
    void handleNotificationsClick(MouseEvent event) {
        showNotificationPanel();
    }

    private void showNotificationPanel() {
        if (notificationPanelController != null) {
            notificationPanelController.toggle();
        }
    }

    /**
     * Returns the currently selected tax year.
     */
    public TaxYear getSelectedTaxYear() {
        return navigationViewModel.getSelectedTaxYear();
    }

    /**
     * Returns the notification service for testing and external access.
     */
    public DeadlineNotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * Shuts down the notification scheduler.
     * Should be called when the application is closing.
     */
    public void shutdown() {
        notificationService.shutdown();
        LOG.info("MainController shutdown complete");
    }

    /**
     * Interface for controllers that need to know the current tax year.
     */
    public interface TaxYearAware {
        void setTaxYear(TaxYear taxYear);
    }
}
