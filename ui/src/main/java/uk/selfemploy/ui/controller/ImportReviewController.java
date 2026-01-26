package uk.selfemploy.ui.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.ui.component.ComparisonDialog;
import uk.selfemploy.ui.viewmodel.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the Import Review page.
 * Handles duplicate detection review during bank import.
 *
 * SE-10B-005: Import Review UI
 */
public class ImportReviewController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ImportReviewController.class);

    // Header
    @FXML private Label headerSubtitle;
    @FXML private Button cancelImportBtn;

    // Loading state
    @FXML private VBox loadingState;
    @FXML private Label loadingLabel;

    // All clear state
    @FXML private VBox allClearState;
    @FXML private Button continueImportBtn;

    // Review content
    @FXML private VBox reviewContent;

    // Summary bar
    @FXML private HBox summaryBar;
    @FXML private Label totalCountLabel;
    @FXML private Label newCountLabel;
    @FXML private Label exactCountLabel;
    @FXML private Label likelyCountLabel;

    // Bulk actions
    @FXML private HBox bulkActionBar;
    @FXML private Button importAllNewBtn;
    @FXML private Button skipAllDuplicatesBtn;
    @FXML private Label selectionLabel;

    // Review table
    @FXML private TableView<ImportCandidateViewModel> reviewTable;
    @FXML private TableColumn<ImportCandidateViewModel, Boolean> selectColumn;
    @FXML private TableColumn<ImportCandidateViewModel, String> dateColumn;
    @FXML private TableColumn<ImportCandidateViewModel, String> descriptionColumn;
    @FXML private TableColumn<ImportCandidateViewModel, String> amountColumn;
    @FXML private TableColumn<ImportCandidateViewModel, String> matchColumn;
    @FXML private TableColumn<ImportCandidateViewModel, ImportAction> actionColumn;

    // Footer
    @FXML private Button backBtn;
    @FXML private Label importCountLabel;
    @FXML private Button importBtn;

    // ViewModel
    private ImportReviewViewModel viewModel;

    // Callbacks
    private Consumer<List<ImportCandidateViewModel>> onImportComplete;
    private Runnable onCancel;
    private Runnable onBack;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOG.info("ImportReviewController.initialize() called");
        viewModel = new ImportReviewViewModel();

        setupTableColumns();
        setupBindings();
        setupKeyboardNavigation();
        setupBulkActionButtons();

        LOG.info("Buttons initialized - importAllNewBtn: {}, skipAllDuplicatesBtn: {}",
                 importAllNewBtn != null ? "OK" : "NULL",
                 skipAllDuplicatesBtn != null ? "OK" : "NULL");
    }

    /**
     * Sets up bulk action buttons with explicit event handlers and bindings.
     * SE-10C-001: Fix bulk action button click handling.
     */
    private void setupBulkActionButtons() {
        if (importAllNewBtn != null) {
            // Ensure button receives mouse events (SE-10C-001 fix)
            importAllNewBtn.setPickOnBounds(true);

            // Set explicit action handler as backup to FXML onAction
            importAllNewBtn.setOnAction(this::handleImportAllNew);

            // AC5: Disable when no NEW items exist
            importAllNewBtn.disableProperty().bind(
                viewModel.newCountProperty().isEqualTo(0)
            );

            LOG.debug("Import All New button configured - pickOnBounds: true, action handler set");
        }

        if (skipAllDuplicatesBtn != null) {
            // Ensure button receives mouse events (SE-10C-001 fix)
            skipAllDuplicatesBtn.setPickOnBounds(true);

            // Set explicit action handler as backup to FXML onAction
            skipAllDuplicatesBtn.setOnAction(this::handleSkipAllDuplicates);

            // AC5: Disable when no EXACT items exist
            skipAllDuplicatesBtn.disableProperty().bind(
                viewModel.exactCountProperty().isEqualTo(0)
            );

            LOG.debug("Skip All Duplicates button configured - pickOnBounds: true, action handler set");
        }
    }

    /**
     * Sets the candidates to review.
     *
     * @param candidates The list of import candidates
     */
    public void setCandidates(List<ImportCandidateViewModel> candidates) {
        LOG.info("setCandidates() called with {} candidates", candidates.size());
        viewModel.setCandidates(candidates);
        reviewTable.setItems(viewModel.getCandidates());
        updateState();
        LOG.info("After updateState() - reviewContent visible: {}, managed: {}",
                 reviewContent.isVisible(), reviewContent.isManaged());
        LOG.info("Bulk action bar visible: {}, buttons disabled - importAllNew: {}, skipAllDuplicates: {}",
                 bulkActionBar != null ? bulkActionBar.isVisible() : "NULL",
                 importAllNewBtn.isDisable(),
                 skipAllDuplicatesBtn.isDisable());
    }

    /**
     * Sets the callback for when import is complete.
     */
    public void setOnImportComplete(Consumer<List<ImportCandidateViewModel>> callback) {
        this.onImportComplete = callback;
    }

    /**
     * Sets the callback for cancel action.
     */
    public void setOnCancel(Runnable callback) {
        this.onCancel = callback;
    }

    /**
     * Sets the callback for back action.
     */
    public void setOnBack(Runnable callback) {
        this.onBack = callback;
    }

    /**
     * Hides the back button. Use this when there's no previous step to return to
     * (e.g., when opened directly from Settings > Import Data).
     */
    public void hideBackButton() {
        if (backBtn != null) {
            backBtn.setVisible(false);
            backBtn.setManaged(false);
        }
    }

    private void setupTableColumns() {
        // Select column (checkbox)
        selectColumn.setCellValueFactory(cellData ->
            new SimpleBooleanProperty(cellData.getValue().isSelected()));
        selectColumn.setCellFactory(col -> new CheckBoxTableCell<>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    ImportCandidateViewModel candidate = getTableView().getItems().get(getIndex());
                    setSelectedStateCallback(index -> candidate.selectedProperty());
                }
            }
        });

        // Date column
        dateColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedDate()));

        // Description column
        descriptionColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDescription()));
        descriptionColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    if (item.length() > 50) {
                        setTooltip(new Tooltip(item));
                    }
                }
            }
        });

        // Amount column
        amountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFormattedAmount()));
        amountColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().removeAll("amount-positive", "amount-negative");
                } else {
                    setText(item);
                    getStyleClass().removeAll("amount-positive", "amount-negative");
                    if (item.startsWith("+")) {
                        getStyleClass().add("amount-positive");
                    } else {
                        getStyleClass().add("amount-negative");
                    }
                }
            }
        });
        amountColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Match column (badge)
        matchColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getMatchType().getDisplayText()));
        matchColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    ImportCandidateViewModel candidate = getTableView().getItems().get(getIndex());
                    MatchType matchType = candidate.getMatchType();

                    HBox badge = new HBox(4);
                    badge.setAlignment(javafx.geometry.Pos.CENTER);
                    badge.getStyleClass().addAll("match-badge", matchType.getStyleClass());

                    Label icon = new Label(matchType.getIcon());
                    icon.getStyleClass().add("match-badge-icon");

                    Label text = new Label(matchType.getDisplayText());
                    text.getStyleClass().add("match-badge-text");

                    badge.getChildren().addAll(icon, text);

                    // Accessible text
                    badge.setAccessibleText(matchType.getDisplayText() + " match");

                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Action column (dropdown)
        actionColumn.setCellValueFactory(cellData ->
            cellData.getValue().actionProperty());
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<ImportAction> comboBox = new ComboBox<>();

            {
                comboBox.setItems(FXCollections.observableArrayList(ImportAction.values()));
                comboBox.getStyleClass().add("action-combo");
                comboBox.setOnAction(e -> {
                    ImportCandidateViewModel candidate = getTableView().getItems().get(getIndex());
                    candidate.setAction(comboBox.getValue());
                    updateImportCount();
                });
            }

            @Override
            protected void updateItem(ImportAction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    comboBox.setValue(item);
                    setGraphic(comboBox);
                }
            }
        });

        // Row styling based on match type
        reviewTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ImportCandidateViewModel item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-new", "row-exact", "row-likely");
                if (!empty && item != null) {
                    getStyleClass().add(item.getMatchType().getRowStyleClass());
                    setAccessibleText(item.getAccessibleText());
                }
            }
        });
    }

    private void setupBindings() {
        // Bind summary counts
        viewModel.totalCountProperty().addListener((obs, oldVal, newVal) -> {
            totalCountLabel.setText(String.valueOf(newVal));
            headerSubtitle.setText("Review " + newVal + " transactions before importing");
        });

        viewModel.newCountProperty().addListener((obs, oldVal, newVal) ->
            newCountLabel.setText(String.valueOf(newVal)));

        viewModel.exactCountProperty().addListener((obs, oldVal, newVal) ->
            exactCountLabel.setText(String.valueOf(newVal)));

        viewModel.likelyCountProperty().addListener((obs, oldVal, newVal) ->
            likelyCountLabel.setText(String.valueOf(newVal)));

        viewModel.selectedCountProperty().addListener((obs, oldVal, newVal) ->
            selectionLabel.setText(newVal + " selected"));

        viewModel.importCountProperty().addListener((obs, oldVal, newVal) -> {
            importCountLabel.setText(newVal + " items will be imported");
            importBtn.setDisable(newVal.intValue() == 0);
        });

        // Loading state bindings
        viewModel.loadingProperty().addListener((obs, oldVal, newVal) -> updateState());
        viewModel.loadingMessageProperty().addListener((obs, oldVal, newVal) ->
            loadingLabel.setText(newVal));
    }

    private void setupKeyboardNavigation() {
        // Enter/Space on table rows for comparison dialog
        reviewTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                ImportCandidateViewModel selected = reviewTable.getSelectionModel().getSelectedItem();
                if (selected != null && selected.hasMatch()) {
                    showComparisonDialog(selected);
                    event.consume();
                }
            }
        });

        // Double-click for comparison
        reviewTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ImportCandidateViewModel selected = reviewTable.getSelectionModel().getSelectedItem();
                if (selected != null && selected.hasMatch()) {
                    showComparisonDialog(selected);
                }
            }
        });
    }

    private void updateState() {
        boolean isLoading = viewModel.isLoading();
        boolean isAllNew = viewModel.isAllNew() && !isLoading;
        boolean hasItemsToReview = viewModel.hasCandidates() && !isAllNew && !isLoading;

        loadingState.setVisible(isLoading);
        loadingState.setManaged(isLoading);

        allClearState.setVisible(isAllNew);
        allClearState.setManaged(isAllNew);

        reviewContent.setVisible(hasItemsToReview);
        reviewContent.setManaged(hasItemsToReview);
    }

    private void updateImportCount() {
        int count = 0;
        for (ImportCandidateViewModel candidate : viewModel.getCandidates()) {
            if (candidate.willBeImported()) {
                count++;
            }
        }
        // Force property update by manually setting through binding
        viewModel.importCountProperty().set(count);
    }

    private void showComparisonDialog(ImportCandidateViewModel candidate) {
        LOG.info("Opening comparison dialog for: {}", candidate);

        Window owner = reviewTable.getScene().getWindow();
        ComparisonDialog dialog = new ComparisonDialog(owner, candidate);

        dialog.setOnActionSelected(action -> {
            LOG.info("User selected action: {} for candidate: {}", action, candidate.getDescription());
            candidate.setAction(action);
            updateImportCount();
            reviewTable.refresh();
        });

        dialog.showAndWait().ifPresent(action -> {
            LOG.debug("Comparison dialog closed with action: {}", action);
        });
    }

    // === Action Handlers ===

    @FXML
    void handleImportAllNew(ActionEvent event) {
        LOG.info("handleImportAllNew() called - button was clicked!");
        viewModel.importAllNew();
        updateImportCount();
        if (reviewTable != null) {
            reviewTable.refresh();
        }
        LOG.info("Set all NEW items to IMPORT - count: {}", viewModel.getNewCount());
    }

    @FXML
    void handleSkipAllDuplicates(ActionEvent event) {
        LOG.info("handleSkipAllDuplicates() called - button was clicked!");
        viewModel.skipAllDuplicates();
        updateImportCount();
        if (reviewTable != null) {
            reviewTable.refresh();
        }
        LOG.info("Set all EXACT matches to SKIP - count: {}", viewModel.getExactCount());
    }

    @FXML
    void handleContinueImport(ActionEvent event) {
        // All clear - import all items
        viewModel.importAllNew();
        handleImport(event);
    }

    @FXML
    void handleImport(ActionEvent event) {
        List<ImportCandidateViewModel> toImport = viewModel.getCandidatesToImport();
        LOG.info("Importing {} transactions", toImport.size());

        if (onImportComplete != null) {
            onImportComplete.accept(toImport);
        }
    }

    @FXML
    void handleCancelImport(ActionEvent event) {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    @FXML
    void handleBack(ActionEvent event) {
        if (onBack != null) {
            onBack.run();
        }
    }

    // === For Testing ===

    /**
     * Returns the ViewModel for testing.
     */
    public ImportReviewViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Sets the ViewModel directly (for testing).
     */
    public void setViewModel(ImportReviewViewModel viewModel) {
        this.viewModel = viewModel;
        if (reviewTable != null) {
            reviewTable.setItems(viewModel.getCandidates());
        }
        // Re-setup button bindings with new viewModel
        setupBulkActionButtons();
    }
}
