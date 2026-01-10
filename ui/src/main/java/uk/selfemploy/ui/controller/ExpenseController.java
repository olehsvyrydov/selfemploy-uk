package uk.selfemploy.ui.controller;

import jakarta.inject.Inject;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.ui.viewmodel.ExpenseListViewModel;
import uk.selfemploy.ui.viewmodel.ExpenseTableRow;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller for the Expense List view.
 * Manages display of expenses, filtering, pagination, and dialog opening.
 */
public class ExpenseController implements Initializable, MainController.TaxYearAware {

    // Summary labels
    @FXML private Label totalValue;
    @FXML private Label totalCount;
    @FXML private Label deductibleValue;
    @FXML private Label deductibleCount;
    @FXML private Label nonDeductibleValue;
    @FXML private Label nonDeductibleCount;

    // Filters
    @FXML private ComboBox<ExpenseCategory> categoryFilter;
    @FXML private TextField searchField;

    // Table
    @FXML private TableView<ExpenseTableRow> expenseTable;
    @FXML private TableColumn<ExpenseTableRow, LocalDate> dateColumn;
    @FXML private TableColumn<ExpenseTableRow, String> descriptionColumn;
    @FXML private TableColumn<ExpenseTableRow, ExpenseCategory> categoryColumn;
    @FXML private TableColumn<ExpenseTableRow, String> amountColumn;
    @FXML private TableColumn<ExpenseTableRow, Boolean> deductibleColumn;

    // Pagination
    @FXML private Label resultCount;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private HBox paginationBar;

    // Empty state
    @FXML private VBox emptyState;

    // Buttons
    @FXML private Button addExpenseBtn;

    // ViewModel and Service
    private ExpenseListViewModel viewModel;

    @Inject
    private ExpenseService expenseService;

    // Business context - would normally come from a session/context service
    private UUID businessId;
    private TaxYear taxYear;
    private boolean cisBusiness = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize business ID (in real app, this would come from session)
        if (businessId == null) {
            businessId = UUID.randomUUID(); // Placeholder
        }

        // Initialize ViewModel
        if (expenseService != null) {
            viewModel = new ExpenseListViewModel(expenseService);
            viewModel.setBusinessId(businessId);
            viewModel.setCisBusiness(cisBusiness);
        }

        setupCategoryFilter();
        setupTableColumns();
        setupTableContextMenu();
        setupBindings();
        setupSearch();

        // Load data if tax year is already set
        if (taxYear != null && viewModel != null) {
            viewModel.setTaxYear(taxYear);
            viewModel.loadExpenses();
        }
    }

    private void setupCategoryFilter() {
        // Add "All Categories" option (null value)
        categoryFilter.getItems().add(null);

        // Add all available categories (excluding CIS-only if not a CIS business)
        if (viewModel != null) {
            categoryFilter.getItems().addAll(viewModel.getAvailableCategories());
        } else {
            // Fallback if viewModel not yet available
            for (ExpenseCategory cat : ExpenseCategory.values()) {
                if (!cisBusiness && cat.isCisOnly()) continue;
                categoryFilter.getItems().add(cat);
            }
        }

        // Custom cell factory for display
        categoryFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExpenseCategory category) {
                if (category == null) {
                    return "All Categories";
                }
                return category.getDisplayName() + " (Box " + category.getSa103Box() + ")";
            }

            @Override
            public ExpenseCategory fromString(String string) {
                return null; // Not needed
            }
        });

        categoryFilter.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ExpenseCategory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null) {
                    setText("All Categories");
                } else {
                    setText(item.getDisplayName() + " (Box " + item.getSa103Box() + ")");
                }
            }
        });

        // Listen for selection changes
        categoryFilter.setOnAction(event -> {
            if (viewModel != null) {
                viewModel.setSelectedCategory(categoryFilter.getValue());
                updateTableData();
            }
        });
    }

    private void setupTableColumns() {
        // Date column
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    ExpenseTableRow row = getTableRow().getItem();
                    if (row != null) {
                        setText(row.getFormattedDate());
                    }
                }
            }
        });

        // Description column
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Category column with color dot
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ExpenseCategory category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    ExpenseTableRow row = getTableRow().getItem();
                    if (row != null) {
                        HBox container = new HBox(8);
                        container.setAlignment(Pos.CENTER_LEFT);

                        // Color dot
                        Label dot = new Label();
                        dot.getStyleClass().addAll("category-dot", row.getCategoryStyleClass());

                        // Category text
                        Label text = new Label(row.getCategoryShortName() + " (" + category.getSa103Box() + ")");
                        text.getStyleClass().add("category-text");

                        container.getChildren().addAll(dot, text);
                        setGraphic(container);
                        setText(null);
                    }
                }
            }
        });

        // Amount column
        amountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedAmount()));
        amountColumn.getStyleClass().add("expense-amount-cell");

        // Deductible column
        deductibleColumn.setCellValueFactory(new PropertyValueFactory<>("deductible"));
        deductibleColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean deductible, boolean empty) {
                super.updateItem(deductible, empty);
                if (empty || deductible == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(deductible ? "Y" : "N");
                    badge.getStyleClass().addAll("deductible-badge",
                        deductible ? "deductible-yes" : "deductible-no");

                    Tooltip tooltip = new Tooltip(deductible ?
                        "Tax deductible" : "Not tax deductible");
                    Tooltip.install(badge, tooltip);

                    setGraphic(badge);
                    setText(null);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Double-click to edit
        expenseTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ExpenseTableRow selected = expenseTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    handleEditExpense(selected);
                }
            }
        });
    }

    private void setupTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(event -> {
            ExpenseTableRow selected = expenseTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleEditExpense(selected);
            }
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(event -> {
            ExpenseTableRow selected = expenseTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                handleDeleteExpense(selected);
            }
        });

        contextMenu.getItems().addAll(editItem, deleteItem);
        expenseTable.setContextMenu(contextMenu);
    }

    private void setupBindings() {
        if (viewModel == null) return;

        // Summary value bindings
        viewModel.totalExpensesProperty().addListener((obs, oldVal, newVal) ->
            totalValue.setText(viewModel.getFormattedTotalExpenses()));

        viewModel.deductibleTotalProperty().addListener((obs, oldVal, newVal) ->
            deductibleValue.setText(viewModel.getFormattedDeductibleTotal()));

        viewModel.nonDeductibleTotalProperty().addListener((obs, oldVal, newVal) ->
            nonDeductibleValue.setText(viewModel.getFormattedNonDeductibleTotal()));

        // Count bindings
        viewModel.totalCountProperty().addListener((obs, oldVal, newVal) ->
            totalCount.setText(newVal + " entries"));

        viewModel.deductibleCountProperty().addListener((obs, oldVal, newVal) ->
            deductibleCount.setText(newVal + " entries"));

        viewModel.nonDeductibleCountProperty().addListener((obs, oldVal, newVal) ->
            nonDeductibleCount.setText(newVal + " entries"));

        // Empty state visibility
        viewModel.emptyStateProperty().addListener((obs, oldVal, newVal) -> {
            emptyState.setVisible(newVal);
            emptyState.setManaged(newVal);
            expenseTable.setVisible(!newVal);
            expenseTable.setManaged(!newVal);
            paginationBar.setVisible(!newVal);
            paginationBar.setManaged(!newVal);
        });

        // Pagination button states
        viewModel.currentPageProperty().addListener((obs, oldVal, newVal) -> updatePaginationControls());
        viewModel.totalPagesProperty().addListener((obs, oldVal, newVal) -> updatePaginationControls());

        // Set initial values
        totalValue.setText(viewModel.getFormattedTotalExpenses());
        deductibleValue.setText(viewModel.getFormattedDeductibleTotal());
        nonDeductibleValue.setText(viewModel.getFormattedNonDeductibleTotal());
        totalCount.setText(viewModel.getTotalCount() + " entries");
        deductibleCount.setText(viewModel.getDeductibleCount() + " entries");
        nonDeductibleCount.setText(viewModel.getNonDeductibleCount() + " entries");
    }

    private void setupSearch() {
        // Debounced search (simplified - in production would use a proper debounce)
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (viewModel != null) {
                viewModel.setSearchText(newVal != null ? newVal : "");
                updateTableData();
            }
        });
    }

    private void updateTableData() {
        if (viewModel == null) return;

        expenseTable.getItems().setAll(viewModel.getCurrentPageItems());
        resultCount.setText(viewModel.getResultCountText());
        updatePaginationControls();
    }

    private void updatePaginationControls() {
        if (viewModel == null) return;

        prevBtn.setDisable(!viewModel.hasPreviousPage());
        nextBtn.setDisable(!viewModel.hasNextPage());
        resultCount.setText(viewModel.getResultCountText());
    }

    // === Event Handlers ===

    @FXML
    void handleAddExpense(ActionEvent event) {
        openExpenseDialog(null);
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (viewModel != null) {
            viewModel.previousPage();
            updateTableData();
        }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        if (viewModel != null) {
            viewModel.nextPage();
            updateTableData();
        }
    }

    private void handleEditExpense(ExpenseTableRow row) {
        if (expenseService == null) return;

        // Load full expense from service
        expenseService.findById(row.id()).ifPresent(expense -> {
            openExpenseDialog(expense);
        });
    }

    private void handleDeleteExpense(ExpenseTableRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Expense?");
        confirm.setHeaderText("Are you sure you want to delete this expense entry?");
        confirm.setContentText(
            "Description: " + row.description() + "\n" +
            "Amount: " + row.getFormattedAmount() + "\n" +
            "Category: " + row.getCategoryDisplayName() + "\n\n" +
            "This action cannot be undone."
        );

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK && expenseService != null) {
                expenseService.delete(row.id());
                showToast("Expense deleted successfully");
                refreshData();
            }
        });
    }

    private void openExpenseDialog(Expense expense) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/expense-dialog.fxml"));
            VBox dialogContent = loader.load();

            ExpenseDialogController dialogController = loader.getController();
            dialogController.setExpenseService(expenseService);
            dialogController.setBusinessId(businessId);
            dialogController.setTaxYear(taxYear);
            dialogController.setCisBusiness(cisBusiness);

            if (expense != null) {
                dialogController.setEditMode(expense);
            } else {
                dialogController.setAddMode();
            }

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.setTitle(expense != null ? "Edit Expense" : "Add Expense");

            Scene scene = new Scene(dialogContent);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            dialogStage.setScene(scene);

            // Set callbacks
            dialogController.setOnSave(savedExpense -> {
                dialogStage.close();
                showToast(expense != null ? "Expense updated successfully" : "Expense saved successfully");
                refreshData();
            });

            dialogController.setOnDelete(() -> {
                dialogStage.close();
                showToast("Expense deleted successfully");
                refreshData();
            });

            dialogController.setOnClose(dialogStage::close);
            dialogController.setDialogStage(dialogStage);

            dialogStage.showAndWait();

        } catch (IOException e) {
            showError("Failed to open expense dialog", e);
        }
    }

    private void showToast(String message) {
        // Simple toast notification using JavaFX PauseTransition
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

    /**
     * Refreshes the expense list from the database.
     */
    public void refreshData() {
        if (viewModel != null) {
            viewModel.loadExpenses();
            updateTableData();
        }
    }

    // === TaxYearAware Implementation ===

    @Override
    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;
        if (viewModel != null) {
            viewModel.setTaxYear(taxYear);
            viewModel.loadExpenses();
            updateTableData();
        }
    }

    // === Public API ===

    /**
     * Sets the business ID for this controller.
     */
    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
        if (viewModel != null) {
            viewModel.setBusinessId(businessId);
        }
    }

    /**
     * Sets whether this is a CIS business.
     */
    public void setCisBusiness(boolean cisBusiness) {
        this.cisBusiness = cisBusiness;
        if (viewModel != null) {
            viewModel.setCisBusiness(cisBusiness);
        }
    }

    /**
     * Sets the expense service (for testing/DI).
     */
    public void setExpenseService(ExpenseService expenseService) {
        this.expenseService = expenseService;
        if (viewModel == null && expenseService != null) {
            viewModel = new ExpenseListViewModel(expenseService);
            viewModel.setBusinessId(businessId);
            viewModel.setCisBusiness(cisBusiness);
            if (taxYear != null) {
                viewModel.setTaxYear(taxYear);
            }
        }
    }

    /**
     * Returns the underlying ViewModel for testing purposes.
     */
    public ExpenseListViewModel getViewModel() {
        return viewModel;
    }
}
