package uk.selfemploy.ui.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ReviewStatus;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.SqliteBankTransactionService;
import uk.selfemploy.ui.viewmodel.TransactionReviewTableRow;
import uk.selfemploy.ui.viewmodel.TransactionReviewViewModel;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Transaction Review Dashboard.
 * Manages the table display, filters, batch operations, export, and keyboard shortcuts.
 */
public class TransactionReviewController implements Initializable, MainController.TaxYearAware, Refreshable {

    private static final Logger LOG = Logger.getLogger(TransactionReviewController.class.getName());

    // Container
    @FXML private VBox reviewContainer;

    // Progress
    @FXML private ProgressBar reviewProgressBar;
    @FXML private Label progressLabel;

    // Summary cards
    @FXML private Label totalValue;
    @FXML private Label pendingValue;
    @FXML private Label categorizedValue;
    @FXML private Label excludedValue;

    // Filters
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;
    @FXML private TextField amountMinField;
    @FXML private TextField amountMaxField;
    @FXML private TextField searchField;

    // Batch bar
    @FXML private HBox batchBar;
    @FXML private Button batchBusinessBtn;
    @FXML private Button batchPersonalBtn;
    @FXML private Button batchExcludeBtn;
    @FXML private Label selectedCountLabel;

    // Undo / Export
    @FXML private Button undoBtn;
    @FXML private MenuButton exportBtn;

    // Table
    @FXML private TableView<TransactionReviewTableRow> transactionTable;
    @FXML private TableColumn<TransactionReviewTableRow, Boolean> selectCol;
    @FXML private TableColumn<TransactionReviewTableRow, String> dateCol;
    @FXML private TableColumn<TransactionReviewTableRow, String> descCol;
    @FXML private TableColumn<TransactionReviewTableRow, String> amountCol;
    @FXML private TableColumn<TransactionReviewTableRow, String> categoryCol;
    @FXML private TableColumn<TransactionReviewTableRow, String> confidenceCol;
    @FXML private TableColumn<TransactionReviewTableRow, String> businessCol;
    @FXML private TableColumn<TransactionReviewTableRow, String> statusCol;
    @FXML private TableColumn<TransactionReviewTableRow, Void> actionCol;

    // Pagination
    @FXML private HBox paginationBar;
    @FXML private Label resultCount;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    // Empty state
    @FXML private VBox emptyState;

    private TransactionReviewViewModel viewModel;
    private TaxYear currentTaxYear;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStatusFilter();
        setupSearchField();
        setupDatePickers();
        setupAmountFields();
        setupTableColumns();
        setupKeyboardShortcuts();
    }

    private void setupStatusFilter() {
        statusFilterCombo.setItems(FXCollections.observableArrayList(
            "All Status", "Pending", "Categorized", "Excluded", "Skipped"
        ));
        statusFilterCombo.setValue("All Status");

        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                ReviewStatus status = switch (newVal) {
                    case "Pending" -> ReviewStatus.PENDING;
                    case "Categorized" -> ReviewStatus.CATEGORIZED;
                    case "Excluded" -> ReviewStatus.EXCLUDED;
                    case "Skipped" -> ReviewStatus.SKIPPED;
                    default -> null;
                };
                viewModel.statusFilterProperty().set(status);
                updateTable();
            }
        });
    }

    private void setupSearchField() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                Platform.runLater(() -> {
                    viewModel.searchTextProperty().set(newVal);
                    updateTable();
                });
            }
        });

        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                searchField.clear();
            }
        });
    }

    private void setupDatePickers() {
        dateFromPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                viewModel.dateFromProperty().set(newVal);
                updateTable();
            }
        });

        dateToPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                viewModel.dateToProperty().set(newVal);
                updateTable();
            }
        });
    }

    private void setupAmountFields() {
        amountMinField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                viewModel.amountMinProperty().set(parseBigDecimal(newVal));
                updateTable();
            }
        });

        amountMaxField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                viewModel.amountMaxProperty().set(parseBigDecimal(newVal));
                updateTable();
            }
        });
    }

    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setupTableColumns() {
        // Select column - checkbox
        selectCol.setCellValueFactory(cellData -> {
            TransactionReviewTableRow row = cellData.getValue();
            SimpleBooleanProperty prop = new SimpleBooleanProperty(
                viewModel != null && viewModel.isSelected(row.id())
            );
            return prop;
        });
        selectCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(e -> {
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    if (viewModel != null) {
                        viewModel.toggleSelection(row.id());
                        updateBatchButtons();
                    }
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    checkBox.setSelected(viewModel != null && viewModel.isSelected(row.id()));
                    setGraphic(checkBox);
                }
            }
        });

        // Date column
        dateCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedDate()));

        // Description column
        descCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().description()));

        // Amount column - colored by income/expense
        amountCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getSignedFormattedAmount()));
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("amount-income", "amount-expense");
                } else {
                    setText(item);
                    getStyleClass().removeAll("amount-income", "amount-expense");
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    getStyleClass().add(row.isIncome() ? "amount-income" : "amount-expense");
                }
            }
        });
        amountCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Category column
        categoryCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getSuggestedCategoryDisplay()));

        // Confidence column - colored badge
        confidenceCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getConfidenceLabel()));
        confidenceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    badge.getStyleClass().add(row.getConfidenceStyleClass());
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        confidenceCol.setStyle("-fx-alignment: CENTER;");

        // Business/Personal column - toggle buttons
        businessCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getBusinessLabel()));
        businessCol.setCellFactory(col -> new TableCell<>() {
            private final ToggleButton busBtn = new ToggleButton("Bus");
            private final ToggleButton persBtn = new ToggleButton("Pers");
            private final HBox box = new HBox(4, busBtn, persBtn);
            {
                busBtn.getStyleClass().add("toggle-business");
                persBtn.getStyleClass().add("toggle-personal");
                busBtn.setOnAction(e -> {
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    if (viewModel != null) {
                        viewModel.toggleBusinessFlag(row.id(), true);
                    }
                });
                persBtn.setOnAction(e -> {
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    if (viewModel != null) {
                        viewModel.toggleBusinessFlag(row.id(), false);
                    }
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    busBtn.setSelected(Boolean.TRUE.equals(row.isBusiness()));
                    persBtn.setSelected(Boolean.FALSE.equals(row.isBusiness()));
                    setGraphic(box);
                }
            }
        });

        // Status column - colored badge
        statusCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().reviewStatus().name()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    badge.getStyleClass().add(row.getStatusStyleClass());
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        statusCol.setStyle("-fx-alignment: CENTER;");

        // Actions column - buttons
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button excludeBtn = new Button("Exclude");
            private final Button skipBtn = new Button("Skip");
            private final HBox box = new HBox(4, excludeBtn, skipBtn);
            {
                excludeBtn.getStyleClass().addAll("action-btn", "action-btn-exclude");
                skipBtn.getStyleClass().addAll("action-btn", "action-btn-skip");
                excludeBtn.setOnAction(e -> {
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    handleExcludeSingle(row);
                });
                skipBtn.setOnAction(e -> {
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    if (viewModel != null) {
                        viewModel.skipTransaction(row.id());
                        updateTable();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TransactionReviewTableRow row = getTableView().getItems().get(getIndex());
                    // Only show actions for pending/skipped transactions
                    if (row.reviewStatus() == ReviewStatus.PENDING || row.reviewStatus() == ReviewStatus.SKIPPED) {
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void setupKeyboardShortcuts() {
        // Keyboard shortcuts are set up when the scene is available
        Platform.runLater(() -> {
            if (reviewContainer.getScene() != null) {
                registerAccelerators();
            } else {
                reviewContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        registerAccelerators();
                    }
                });
            }
        });
    }

    private void registerAccelerators() {
        var scene = reviewContainer.getScene();
        if (scene == null) return;

        // Ctrl+A: Select all
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN),
            this::handleSelectAllAction
        );
        // Ctrl+B: Batch business
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN),
            this::handleBatchBusinessAction
        );
        // Ctrl+P: Batch personal
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN),
            this::handleBatchPersonalAction
        );
        // Ctrl+E: Batch exclude
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN),
            this::handleBatchExcludeAction
        );
        // Ctrl+Z: Undo
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
            this::handleUndoAction
        );
    }

    // === Binding ===

    private void setupBindings() {
        if (viewModel == null) return;

        // Summary card bindings
        viewModel.totalCountProperty().addListener((obs, o, n) ->
            totalValue.setText(String.valueOf(n.intValue())));
        viewModel.pendingCountProperty().addListener((obs, o, n) ->
            pendingValue.setText(String.valueOf(n.intValue())));
        viewModel.categorizedCountProperty().addListener((obs, o, n) ->
            categorizedValue.setText(String.valueOf(n.intValue())));
        viewModel.excludedCountProperty().addListener((obs, o, n) ->
            excludedValue.setText(String.valueOf(n.intValue())));

        // Progress bar
        viewModel.reviewProgressProperty().addListener((obs, o, n) ->
            reviewProgressBar.setProgress(n.doubleValue()));
        viewModel.reviewedCountProperty().addListener((obs, o, n) ->
            progressLabel.setText(n.intValue() + " of " + viewModel.getTotalCount() + " reviewed"));
        viewModel.totalCountProperty().addListener((obs, o, n) ->
            progressLabel.setText(viewModel.getReviewedCount() + " of " + n.intValue() + " reviewed"));

        // Undo button
        viewModel.canUndoProperty().addListener((obs, o, n) ->
            undoBtn.setDisable(!n));

        // Selected count
        viewModel.selectedCountProperty().addListener((obs, o, n) -> {
            selectedCountLabel.setText(n.intValue() + " selected");
            updateBatchButtons();
        });

        // Set initial values
        totalValue.setText(String.valueOf(viewModel.getTotalCount()));
        pendingValue.setText(String.valueOf(viewModel.getPendingCount()));
        categorizedValue.setText(String.valueOf(viewModel.getCategorizedCount()));
        excludedValue.setText(String.valueOf(viewModel.getExcludedCount()));
        reviewProgressBar.setProgress(viewModel.getReviewProgress());
        progressLabel.setText(viewModel.getReviewedCount() + " of " + viewModel.getTotalCount() + " reviewed");
        undoBtn.setDisable(!viewModel.getCanUndo());
    }

    private void updateTable() {
        if (viewModel == null) return;

        List<TransactionReviewTableRow> pageItems = viewModel.getCurrentPageItems();
        transactionTable.getItems().setAll(pageItems);

        resultCount.setText(viewModel.getResultCountText());
        prevBtn.setDisable(!viewModel.canGoPrevious());
        nextBtn.setDisable(!viewModel.canGoNext());

        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = viewModel.isEmptyState();

        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);

        transactionTable.setVisible(!isEmpty);
        transactionTable.setManaged(!isEmpty);

        paginationBar.setVisible(!isEmpty);
        paginationBar.setManaged(!isEmpty);

        batchBar.setVisible(!isEmpty);
        batchBar.setManaged(!isEmpty);
    }

    private void updateBatchButtons() {
        boolean hasSelection = viewModel != null && viewModel.getSelectedCount() > 0;
        batchBusinessBtn.setDisable(!hasSelection);
        batchPersonalBtn.setDisable(!hasSelection);
        batchExcludeBtn.setDisable(!hasSelection);
    }

    // === Action Handlers ===

    @FXML
    void handleSelectAll(ActionEvent event) { handleSelectAllAction(); }

    @FXML
    void handleSelectPending(ActionEvent event) {
        if (viewModel != null) {
            viewModel.selectAllPending();
            updateTable();
        }
    }

    @FXML
    void handleBatchBusiness(ActionEvent event) { handleBatchBusinessAction(); }

    @FXML
    void handleBatchPersonal(ActionEvent event) { handleBatchPersonalAction(); }

    @FXML
    void handleBatchExclude(ActionEvent event) { handleBatchExcludeAction(); }

    @FXML
    void handleUndo(ActionEvent event) { handleUndoAction(); }

    @FXML
    void handleExportCsv(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Transactions as CSV");
        fileChooser.setInitialFileName("transactions.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(reviewContainer.getScene().getWindow());
        if (file != null && viewModel != null) {
            try {
                viewModel.exportCsv(file.toPath());
                showInfo("Export complete: " + file.getName());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "CSV export failed", e);
                showError("Failed to export CSV", e);
            }
        }
    }

    @FXML
    void handleExportJson(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Transactions as JSON");
        fileChooser.setInitialFileName("transactions.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(reviewContainer.getScene().getWindow());
        if (file != null && viewModel != null) {
            try {
                viewModel.exportJson(file.toPath());
                showInfo("Export complete: " + file.getName());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "JSON export failed", e);
                showError("Failed to export JSON", e);
            }
        }
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (viewModel != null) {
            viewModel.previousPage();
            updateTable();
        }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        if (viewModel != null) {
            viewModel.nextPage();
            updateTable();
        }
    }

    // Keyboard shortcut action methods

    private void handleSelectAllAction() {
        if (viewModel != null) {
            viewModel.selectAll();
            updateTable();
        }
    }

    private void handleBatchBusinessAction() {
        if (viewModel != null && viewModel.getSelectedCount() > 0) {
            viewModel.batchMarkBusiness();
            updateTable();
        }
    }

    private void handleBatchPersonalAction() {
        if (viewModel != null && viewModel.getSelectedCount() > 0) {
            viewModel.batchMarkPersonal();
            updateTable();
        }
    }

    private void handleBatchExcludeAction() {
        if (viewModel == null || viewModel.getSelectedCount() == 0) return;

        // Show exclusion reason dialog
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
            "Personal transaction",
            "Personal transaction",
            "Inter-account transfer",
            "Loan/credit repayment",
            "Tax payment",
            "Duplicate",
            "Other"
        );
        dialog.setTitle("Exclude Transactions");
        dialog.setHeaderText("Exclude " + viewModel.getSelectedCount() + " selected transactions");
        dialog.setContentText("Reason:");

        dialog.showAndWait().ifPresent(reason -> {
            viewModel.batchExclude(reason);
            updateTable();
        });
    }

    private void handleUndoAction() {
        if (viewModel != null && viewModel.getCanUndo()) {
            viewModel.undo();
            updateTable();
        }
    }

    private void handleExcludeSingle(TransactionReviewTableRow row) {
        if (viewModel == null) return;

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
            "Personal transaction",
            "Personal transaction",
            "Inter-account transfer",
            "Loan/credit repayment",
            "Tax payment",
            "Duplicate",
            "Other"
        );
        dialog.setTitle("Exclude Transaction");
        dialog.setHeaderText("Exclude: " + row.description());
        dialog.setContentText("Reason:");

        dialog.showAndWait().ifPresent(reason -> {
            viewModel.excludeTransaction(row.id(), reason);
            updateTable();
        });
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(e != null ? e.getMessage() : "Unknown error");
        alert.showAndWait();
    }

    // === TaxYearAware ===

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.currentTaxYear = taxYear;

        SqliteBankTransactionService service = CoreServiceFactory.getBankTransactionService();
        viewModel = new TransactionReviewViewModel(service);
        setupBindings();
        viewModel.loadTransactions();
        updateTable();
    }

    @Override
    public void refreshData() {
        if (viewModel != null) {
            viewModel.loadTransactions();
            updateTable();
        }
    }

    /**
     * Returns the ViewModel for testing.
     */
    public TransactionReviewViewModel getViewModel() {
        return viewModel;
    }
}
