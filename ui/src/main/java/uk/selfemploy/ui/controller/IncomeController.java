package uk.selfemploy.ui.controller;

import jakarta.inject.Inject;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.IncomeListViewModel;
import uk.selfemploy.ui.viewmodel.IncomeTableRow;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller for the Income List View.
 * Manages the income list display, filtering, pagination, and actions.
 */
public class IncomeController implements Initializable, MainController.TaxYearAware {

    private static final Logger LOG = LoggerFactory.getLogger(IncomeController.class);

    // Summary labels
    @FXML private Label totalValue;
    @FXML private Label totalCount;
    @FXML private Label paidValue;
    @FXML private Label paidCount;
    @FXML private Label unpaidValue;
    @FXML private Label unpaidCount;

    // Filters
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;

    // Table
    @FXML private TableView<IncomeTableRow> incomeTable;
    @FXML private TableColumn<IncomeTableRow, String> dateColumn;
    @FXML private TableColumn<IncomeTableRow, String> clientColumn;
    @FXML private TableColumn<IncomeTableRow, String> descriptionColumn;
    @FXML private TableColumn<IncomeTableRow, String> amountColumn;
    @FXML private TableColumn<IncomeTableRow, String> statusColumn;

    // Pagination
    @FXML private HBox paginationBar;
    @FXML private Label resultCount;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    // Empty state
    @FXML private VBox emptyState;

    // Add button
    @FXML private Button addIncomeBtn;

    private IncomeListViewModel viewModel;
    private TaxYear currentTaxYear;

    // For dependency injection (will be set by CDI in production)
    private IncomeService incomeService;
    private UUID businessId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStatusFilter();
        setupSearchField();
        setupTableColumns();
        setupTableInteractions();
        setupContextMenu();
        setupKeyboardShortcuts();
    }

    /**
     * Initializes the controller with required dependencies.
     * Called after FXML injection in production via CDI.
     */
    public void initializeWithDependencies(IncomeService incomeService, UUID businessId) {
        this.incomeService = incomeService;
        this.businessId = businessId;
    }

    private void setupStatusFilter() {
        statusFilter.setItems(FXCollections.observableArrayList(
            "All Status",
            "Paid",
            "Unpaid"
        ));
        statusFilter.setValue("All Status");

        statusFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(String status) {
                return status;
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        });

        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                IncomeStatus status = switch (newVal) {
                    case "Paid" -> IncomeStatus.PAID;
                    case "Unpaid" -> IncomeStatus.UNPAID;
                    default -> null;
                };
                viewModel.setStatusFilter(status);
                updateTable();
            }
        });
    }

    private void setupSearchField() {
        // Debounced search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                // Simple debounce using Platform.runLater
                Platform.runLater(() -> {
                    viewModel.setSearchText(newVal);
                    updateTable();
                });
            }
        });

        // Clear on Escape
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                searchField.clear();
            }
        });
    }

    private void setupTableColumns() {
        // Date column
        dateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedDate()));

        // Client column
        clientColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().clientName()));
        clientColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        // Description column
        descriptionColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().description()));
        descriptionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: -fx-text-secondary;");
                }
            }
        });

        // Amount column
        amountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedAmount()));
        amountColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: -fx-success; -fx-alignment: CENTER-RIGHT;");
                }
            }
        });
        amountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Status column
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatusDisplay()));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("status-badge");
                    if ("Paid".equals(item)) {
                        badge.getStyleClass().add("status-paid");
                    } else {
                        badge.getStyleClass().add("status-unpaid");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        statusColumn.setStyle("-fx-alignment: CENTER;");
    }

    private void setupTableInteractions() {
        // Double-click to edit
        incomeTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                IncomeTableRow selected = incomeTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    handleEditIncome(selected);
                }
            }
        });

        // Row selection styling
        incomeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // Selection handling if needed
        });
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit Income");
        editItem.setOnAction(e -> {
            IncomeTableRow selected = incomeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleEditIncome(selected);
            }
        });

        MenuItem deleteItem = new MenuItem("Delete Income");
        deleteItem.setOnAction(e -> {
            IncomeTableRow selected = incomeTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleDeleteIncome(selected);
            }
        });

        contextMenu.getItems().addAll(editItem, deleteItem);
        incomeTable.setContextMenu(contextMenu);
    }

    private void setupKeyboardShortcuts() {
        incomeTable.setOnKeyPressed(event -> {
            IncomeTableRow selected = incomeTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            if (event.getCode() == KeyCode.ENTER) {
                handleEditIncome(selected);
            } else if (event.getCode() == KeyCode.DELETE) {
                handleDeleteIncome(selected);
            }
        });
    }

    private void setupBindings() {
        if (viewModel == null) return;

        // Bind summary values
        viewModel.totalIncomeProperty().addListener((obs, oldVal, newVal) ->
            totalValue.setText(viewModel.getFormattedTotalIncome()));
        viewModel.paidIncomeProperty().addListener((obs, oldVal, newVal) ->
            paidValue.setText(viewModel.getFormattedPaidIncome()));
        viewModel.unpaidIncomeProperty().addListener((obs, oldVal, newVal) ->
            unpaidValue.setText(viewModel.getFormattedUnpaidIncome()));

        // Bind counts
        viewModel.totalCountProperty().addListener((obs, oldVal, newVal) ->
            totalCount.setText(viewModel.getTotalCountText()));
        viewModel.paidCountProperty().addListener((obs, oldVal, newVal) ->
            paidCount.setText(viewModel.getPaidCountText()));
        viewModel.unpaidCountProperty().addListener((obs, oldVal, newVal) ->
            unpaidCount.setText(viewModel.getUnpaidCountText()));

        // Set initial values
        totalValue.setText(viewModel.getFormattedTotalIncome());
        paidValue.setText(viewModel.getFormattedPaidIncome());
        unpaidValue.setText(viewModel.getFormattedUnpaidIncome());
        totalCount.setText(viewModel.getTotalCountText());
        paidCount.setText(viewModel.getPaidCountText());
        unpaidCount.setText(viewModel.getUnpaidCountText());
    }

    private void updateTable() {
        if (viewModel == null) return;

        List<IncomeTableRow> pageItems = viewModel.getCurrentPageItems();
        incomeTable.getItems().setAll(pageItems);

        // Update result count
        resultCount.setText(viewModel.getResultCountText());

        // Update pagination buttons
        prevBtn.setDisable(!viewModel.canGoPrevious());
        nextBtn.setDisable(!viewModel.canGoNext());

        // Show/hide empty state
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = viewModel.isEmptyState();
        boolean noResults = viewModel.isNoResults();

        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);

        incomeTable.setVisible(!isEmpty);
        incomeTable.setManaged(!isEmpty);

        paginationBar.setVisible(!isEmpty && !noResults);
        paginationBar.setManaged(!isEmpty && !noResults);
    }

    // === Action Handlers ===

    @FXML
    void handleAddIncome(ActionEvent event) {
        openIncomeDialog(null);
    }

    private void handleEditIncome(IncomeTableRow row) {
        openIncomeDialog(row);
    }

    private void handleDeleteIncome(IncomeTableRow row) {
        if (row == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Income?");
        alert.setHeaderText("Are you sure you want to delete this income entry?");
        alert.setContentText(String.format(
            "Client: %s%nAmount: %s%nDate: %s%n%nThis action cannot be undone.",
            row.clientName(),
            row.getFormattedAmount(),
            row.getFormattedDate()
        ));

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (incomeService != null) {
                    boolean deleted = incomeService.delete(row.id());
                    if (deleted) {
                        viewModel.refresh();
                        updateTable();
                        showSuccessToast("Income deleted");
                    }
                }
            }
        });
    }

    private void openIncomeDialog(IncomeTableRow editRow) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/income-dialog.fxml"));
            VBox dialogContent = loader.load();

            IncomeDialogController dialogController = loader.getController();
            dialogController.initializeWithDependencies(incomeService, businessId, currentTaxYear);

            if (editRow != null) {
                // Edit mode - need to fetch the full Income from service
                if (incomeService != null) {
                    incomeService.findById(editRow.id()).ifPresent(income ->
                        dialogController.setEditMode(income, editRow.clientName(), editRow.status())
                    );
                }
            }

            // Set up callbacks
            dialogController.setOnSaveCallback(income -> {
                viewModel.refresh();
                updateTable();
                showSuccessToast(editRow == null ? "Income saved successfully" : "Changes saved");
            });

            dialogController.setOnDeleteCallback(() -> {
                viewModel.refresh();
                updateTable();
                showSuccessToast("Income deleted");
            });

            // Create and show dialog stage
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle(editRow == null ? "Add Income" : "Edit Income");

            Scene scene = new Scene(dialogContent);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            dialogStage.setScene(scene);

            dialogController.setDialogStage(dialogStage);

            dialogStage.showAndWait();

        } catch (IOException e) {
            showError("Failed to open income dialog", e);
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

    private void showSuccessToast(String message) {
        LOG.info("Income operation: {}", message);

        // Toast notification using JavaFX PauseTransition
        Alert toast = new Alert(Alert.AlertType.INFORMATION);
        toast.setTitle(null);
        toast.setHeaderText(null);
        toast.setContentText(message);
        toast.show();

        // Auto-dismiss after 2 seconds using JavaFX animation (no raw threads)
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(event -> toast.close());
        delay.play();
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    // === TaxYearAware Implementation ===

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.currentTaxYear = taxYear;
        if (incomeService != null && businessId != null) {
            viewModel = new IncomeListViewModel(incomeService, businessId);
            setupBindings();
            viewModel.loadIncome(taxYear);
            updateTable();
        }
    }

    /**
     * Refreshes the income data.
     */
    public void refreshData() {
        if (viewModel != null && currentTaxYear != null) {
            viewModel.loadIncome(currentTaxYear);
            updateTable();
        }
    }

    /**
     * Returns the ViewModel for testing.
     */
    public IncomeListViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Sets the ViewModel directly (for testing).
     */
    public void setViewModel(IncomeListViewModel viewModel) {
        this.viewModel = viewModel;
        setupBindings();
        updateTable();
    }
}
