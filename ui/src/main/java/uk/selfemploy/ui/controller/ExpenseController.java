package uk.selfemploy.ui.controller;

import jakarta.inject.Inject;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.ReceiptStorageService;
import uk.selfemploy.ui.help.HelpContent;
import uk.selfemploy.ui.help.HelpService;
import uk.selfemploy.ui.help.HelpTopic;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.viewmodel.ExpenseListViewModel;
import uk.selfemploy.ui.viewmodel.ExpenseTableRow;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Controller for the Expense List view.
 * Manages display of expenses, filtering, pagination, and dialog opening.
 */
public class ExpenseController implements Initializable, MainController.TaxYearAware, Refreshable {

    // Summary labels
    @FXML private Label totalValue;
    @FXML private Label totalCount;
    @FXML private Label deductibleValue;
    @FXML private Label deductibleCount;
    @FXML private Label nonDeductibleValue;
    @FXML private Label nonDeductibleCount;

    // Help icons
    @FXML private Label deductibleHelpIcon;
    @FXML private Label nonDeductibleHelpIcon;

    // Filters
    @FXML private ComboBox<ExpenseCategory> categoryFilter;
    @FXML private TextField searchField;

    // Table
    @FXML private TableView<ExpenseTableRow> expenseTable;
    @FXML private TableColumn<ExpenseTableRow, LocalDate> dateColumn;
    @FXML private TableColumn<ExpenseTableRow, String> descriptionColumn;
    @FXML private TableColumn<ExpenseTableRow, ExpenseCategory> categoryColumn;
    @FXML private TableColumn<ExpenseTableRow, String> amountColumn;
    @FXML private TableColumn<ExpenseTableRow, Integer> receiptColumn;
    @FXML private TableColumn<ExpenseTableRow, Boolean> deductibleColumn;

    // Pagination
    @FXML private Label resultCount;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private HBox paginationBar;

    // Empty state
    @FXML private VBox emptyState;

    // Container (for dialog owner)
    @FXML private VBox expenseContainer;

    // Buttons
    @FXML private Button addExpenseBtn;

    // ViewModel and Service
    private ExpenseListViewModel viewModel;

    @Inject
    private ExpenseService expenseService;

    private ReceiptStorageService receiptStorageService;

    // Help service
    private final HelpService helpService = new HelpService();

    // Business context - would normally come from a session/context service
    private UUID businessId;
    private TaxYear taxYear;
    private boolean cisBusiness = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize business ID (in real app, this would come from session)
        if (businessId == null) {
            businessId = CoreServiceFactory.getDefaultBusinessId();
        }

        // Get expense service from CDI or fallback to CoreServiceFactory
        if (expenseService == null) {
            expenseService = CoreServiceFactory.getExpenseService();
        }

        // Get receipt storage service
        if (receiptStorageService == null) {
            receiptStorageService = CoreServiceFactory.getReceiptStorageService();
        }

        // Initialize ViewModel
        if (expenseService != null) {
            viewModel = new ExpenseListViewModel(expenseService);
            viewModel.setBusinessId(businessId);
            viewModel.setCisBusiness(cisBusiness);
            if (receiptStorageService != null) {
                viewModel.setReceiptStorageService(receiptStorageService);
            }
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
        // Date column - use lambda for record accessors
        dateColumn.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().date()));
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

        // Description column - use lambda for record accessors
        descriptionColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().description()));

        // Category column with color dot - use lambda for record accessors
        categoryColumn.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().category()));
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

                        // Category text - show short display name
                        Label text = new Label(category.getShortDisplayName() + " (Box " + category.getSa103Box() + ")");
                        text.getStyleClass().add("category-text");

                        container.getChildren().addAll(dot, text);
                        setGraphic(container);
                        setText(null);
                    }
                }
            }
        });

        // Amount column - right-aligned for easier scanning
        amountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedAmount()));
        amountColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(amount);
                    setAlignment(Pos.CENTER_RIGHT);
                    getStyleClass().add("amount-cell");
                }
            }
        });
        amountColumn.getStyleClass().add("expense-amount-cell");

        // Receipt column - shows clickable paperclip icon with badge count
        receiptColumn.setCellValueFactory(cellData ->
            new SimpleIntegerProperty(cellData.getValue().receiptCount()).asObject());
        receiptColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer count, boolean empty) {
                super.updateItem(count, empty);
                if (empty || count == null || count == 0) {
                    setText(null);
                    setGraphic(null);
                    setOnMouseClicked(null);
                    getStyleClass().remove("receipt-cell-clickable");
                } else {
                    // Paperclip icon with badge count
                    HBox container = new HBox(2);
                    container.setAlignment(Pos.CENTER);

                    Label clipIcon = new Label("📎");
                    clipIcon.getStyleClass().add("receipt-icon");

                    Label badge = new Label(String.valueOf(count));
                    badge.getStyleClass().add("receipt-badge");

                    container.getChildren().addAll(clipIcon, badge);

                    Tooltip tooltip = new Tooltip("Click to view " + count + " receipt" + (count > 1 ? "s" : ""));
                    Tooltip.install(container, tooltip);

                    setGraphic(container);
                    setText(null);
                    setAlignment(Pos.CENTER);
                    getStyleClass().add("receipt-cell-clickable");

                    // Make clickable to view receipts
                    setOnMouseClicked(event -> {
                        ExpenseTableRow row = getTableRow().getItem();
                        if (row != null) {
                            handleViewReceipts(row);
                        }
                    });
                }
            }
        });

        // Deductible column - use checkmark/cross icons
        deductibleColumn.setCellValueFactory(cellData ->
            new SimpleBooleanProperty(cellData.getValue().deductible()).asObject());
        deductibleColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean deductible, boolean empty) {
                super.updateItem(deductible, empty);
                if (empty || deductible == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Use checkmark for yes, X for no
                    Label badge = new Label(deductible ? "✓" : "✗");
                    badge.getStyleClass().addAll("deductible-badge",
                        deductible ? "deductible-yes" : "deductible-no");

                    Tooltip tooltip = new Tooltip(deductible ?
                        "Tax deductible - reduces your tax bill" : "Not tax deductible");
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

    @FXML
    void handleDeductibleHelp(MouseEvent event) {
        showHelpDialog(HelpTopic.ALLOWABLE_EXPENSES);
    }

    @FXML
    void handleNonDeductibleHelp(MouseEvent event) {
        showHelpDialog(HelpTopic.NON_DEDUCTIBLE_EXPENSES);
    }

    private void showHelpDialog(HelpTopic topic) {
        helpService.getHelp(topic).ifPresent(content -> {
            Alert helpDialog = new Alert(Alert.AlertType.INFORMATION);
            helpDialog.setTitle("Help");
            helpDialog.setHeaderText(content.title());
            helpDialog.setContentText(content.body());

            // Add "Learn More" link button if available
            if (content.hmrcLink() != null && !content.hmrcLink().isBlank()) {
                ButtonType learnMore = new ButtonType(content.linkText() != null ?
                        content.linkText() : "Learn More");
                ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
                helpDialog.getButtonTypes().setAll(learnMore, close);

                helpDialog.showAndWait().ifPresent(result -> {
                    if (result == learnMore) {
                        helpService.openExternalLink(content.hmrcLink());
                    }
                });
            } else {
                helpDialog.showAndWait();
            }
        });
    }

    private void handleEditExpense(ExpenseTableRow row) {
        if (expenseService == null) return;

        // Load full expense from service
        expenseService.findById(row.id()).ifPresent(expense -> {
            openExpenseDialog(expense);
        });
    }

    private void handleViewReceipts(ExpenseTableRow row) {
        if (receiptStorageService == null) return;

        var receipts = receiptStorageService.listReceipts(row.id());
        if (receipts.isEmpty()) return;

        if (receipts.size() == 1) {
            // Single receipt - open directly
            openReceipt(receipts.get(0));
        } else {
            // Multiple receipts - show selection dialog
            ChoiceDialog<String> dialog = new ChoiceDialog<>();
            dialog.setTitle("View Receipt");
            dialog.setHeaderText("Select a receipt to view");
            dialog.setContentText("Receipt:");

            for (var receipt : receipts) {
                dialog.getItems().add(receipt.originalFilename());
            }
            dialog.setSelectedItem(dialog.getItems().get(0));

            dialog.showAndWait().ifPresent(selected -> {
                receipts.stream()
                    .filter(r -> r.originalFilename().equals(selected))
                    .findFirst()
                    .ifPresent(this::openReceipt);
            });
        }
    }

    private void openReceipt(uk.selfemploy.core.service.ReceiptMetadata receipt) {
        if (receipt == null || receipt.storagePath() == null) return;

        java.io.File file = receipt.storagePath().toFile();
        if (!file.exists()) return;

        // Run on background thread to avoid blocking JavaFX Application Thread
        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux")) {
                    // Use xdg-open on Linux for better window handling
                    new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", file.getAbsolutePath()).start();
                } else if (os.contains("win")) {
                    new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
                } else if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                }
            } catch (java.io.IOException e) {
                // Show error on JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Could not open receipt");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
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
        javafx.stage.Window owner = expenseContainer.getScene().getWindow();

        boolean success = uk.selfemploy.ui.util.ExpenseDialogHelper.openDialog(
                owner,
                expenseService,
                businessId,
                taxYear,
                cisBusiness,
                expense,
                savedExpense -> {
                    showToast(expense != null ? "Expense updated successfully" : "Expense saved successfully");
                    refreshData();
                },
                () -> {
                    showToast("Expense deleted successfully");
                    refreshData();
                }
        );

        if (!success) {
            showError("Failed to open expense dialog", null);
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
