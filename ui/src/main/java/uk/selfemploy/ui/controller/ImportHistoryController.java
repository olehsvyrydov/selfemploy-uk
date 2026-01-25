package uk.selfemploy.ui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.ui.viewmodel.*;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * Controller for the Import History page.
 * Displays past imports with undo functionality.
 *
 * SE-10B-006: Import History View
 */
public class ImportHistoryController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ImportHistoryController.class);
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    // Header
    @FXML private Label headerSubtitle;
    @FXML private Button newImportBtn;

    // Filter bar
    @FXML private ToggleButton allTimeFilter;
    @FXML private ToggleButton last7DaysFilter;
    @FXML private ToggleButton last30DaysFilter;
    @FXML private ToggleButton thisTaxYearFilter;
    @FXML private Label resultCountLabel;

    // States
    @FXML private VBox loadingState;
    @FXML private VBox emptyState;
    @FXML private VBox noResultsState;

    // History list
    @FXML private VBox historyList;

    // ViewModel
    private ImportHistoryViewModel viewModel;

    // Filter toggle group
    private ToggleGroup filterGroup;

    // Callbacks
    private Runnable onNewImport;
    private Consumer<ImportHistoryItemViewModel> onUndoImport;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viewModel = new ImportHistoryViewModel();

        setupFilterGroup();
        setupBindings();
    }

    /**
     * Sets the import history data.
     */
    public void setImports(List<ImportHistoryItemViewModel> imports) {
        viewModel.setImports(imports);
        refreshHistoryList();
    }

    /**
     * Sets the callback for starting a new import.
     */
    public void setOnNewImport(Runnable callback) {
        this.onNewImport = callback;
    }

    /**
     * Sets the callback for undo action.
     */
    public void setOnUndoImport(Consumer<ImportHistoryItemViewModel> callback) {
        this.onUndoImport = callback;
    }

    private void setupFilterGroup() {
        filterGroup = new ToggleGroup();
        allTimeFilter.setToggleGroup(filterGroup);
        last7DaysFilter.setToggleGroup(filterGroup);
        last30DaysFilter.setToggleGroup(filterGroup);
        thisTaxYearFilter.setToggleGroup(filterGroup);

        // Default selection
        allTimeFilter.setSelected(true);

        // Map toggle buttons to filters
        allTimeFilter.setUserData(ImportHistoryFilter.ALL_TIME);
        last7DaysFilter.setUserData(ImportHistoryFilter.LAST_7_DAYS);
        last30DaysFilter.setUserData(ImportHistoryFilter.LAST_30_DAYS);
        thisTaxYearFilter.setUserData(ImportHistoryFilter.THIS_TAX_YEAR);
    }

    private void setupBindings() {
        // Update result count label
        viewModel.totalCountProperty().addListener((obs, oldVal, newVal) ->
            updateResultCount());

        // Update visibility based on state
        viewModel.loadingProperty().addListener((obs, oldVal, newVal) -> updateState());
    }

    private void updateState() {
        boolean isLoading = viewModel.isLoading();
        boolean isEmpty = viewModel.isEmpty() && !isLoading;
        boolean isFilteredEmpty = viewModel.isFilteredEmpty() && !isLoading;
        boolean hasItems = !isEmpty && !isFilteredEmpty && !isLoading;

        loadingState.setVisible(isLoading);
        loadingState.setManaged(isLoading);

        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);

        noResultsState.setVisible(isFilteredEmpty);
        noResultsState.setManaged(isFilteredEmpty);

        historyList.setVisible(hasItems);
        historyList.setManaged(hasItems);
    }

    private void updateResultCount() {
        int count = viewModel.getFilteredImports().size();
        resultCountLabel.setText(count == 1 ? "1 import" : count + " imports");
    }

    private void refreshHistoryList() {
        historyList.getChildren().clear();

        for (ImportHistoryItemViewModel item : viewModel.getFilteredImports()) {
            historyList.getChildren().add(createHistoryCard(item));
        }

        updateState();
        updateResultCount();
    }

    /**
     * Creates a history card for an import item.
     * Follows /aura design specification with expandable details.
     */
    private VBox createHistoryCard(ImportHistoryItemViewModel item) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("import-history-card", item.getStatus().getStyleClass());
        card.setPadding(new Insets(16));
        card.setFocusTraversable(true);
        card.setAccessibleText(item.getAccessibleText());

        // Header row
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        // Status icon
        Label statusIcon = new Label(getStatusIcon(item.getStatus()));
        statusIcon.getStyleClass().addAll("status-icon", "status-icon-" + item.getStatus().name().toLowerCase());

        // Info section
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label fileName = new Label(item.getFileName());
        fileName.getStyleClass().add("import-file-name");

        HBox meta = new HBox(8);
        Label dateLabel = new Label(item.getFormattedDateTime());
        dateLabel.getStyleClass().add("import-date");

        Label separator = new Label("*");
        separator.getStyleClass().add("meta-separator");

        Label recordsLabel = new Label(item.getRecordCountText());
        recordsLabel.getStyleClass().add("import-records");

        meta.getChildren().addAll(dateLabel, separator, recordsLabel);
        info.getChildren().addAll(fileName, meta);

        // Status badge
        Label statusBadge = new Label(item.getStatus().getDisplayText());
        statusBadge.getStyleClass().addAll("import-status-badge", item.getStatus().getStyleClass());

        // Expand/collapse button
        Button expandBtn = new Button(item.isExpanded() ? "[-]" : "[+]");
        expandBtn.getStyleClass().add("expand-btn");
        expandBtn.setAccessibleText(item.isExpanded() ? "Collapse details" : "Expand details");

        header.getChildren().addAll(statusIcon, info, statusBadge, expandBtn);

        // Details section (initially hidden)
        VBox details = createDetailsSection(item);
        details.setVisible(item.isExpanded());
        details.setManaged(item.isExpanded());

        // Toggle expand/collapse
        expandBtn.setOnAction(e -> {
            boolean expanded = !item.isExpanded();
            item.setExpanded(expanded);
            details.setVisible(expanded);
            details.setManaged(expanded);
            expandBtn.setText(expanded ? "[-]" : "[+]");
            expandBtn.setAccessibleText(expanded ? "Collapse details" : "Expand details");
        });

        // Keyboard support
        card.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                boolean expanded = !item.isExpanded();
                item.setExpanded(expanded);
                details.setVisible(expanded);
                details.setManaged(expanded);
                expandBtn.setText(expanded ? "[-]" : "[+]");
                e.consume();
            }
        });

        card.getChildren().addAll(header, details);
        return card;
    }

    private VBox createDetailsSection(ImportHistoryItemViewModel item) {
        VBox details = new VBox(12);
        details.getStyleClass().add("import-details");
        details.setPadding(new Insets(12, 0, 0, 44)); // Offset for icon

        // Bank format
        HBox formatRow = createDetailRow("Bank Format", item.getBankFormatDisplay());

        // Amounts
        HBox amountsRow = new HBox(24);
        amountsRow.getChildren().addAll(
            createAmountBox("Income", item.getIncomeRecords() + " records",
                item.getFormattedIncomeTotal(), "amount-income"),
            createAmountBox("Expenses", item.getExpenseRecords() + " records",
                item.getFormattedExpenseTotal(), "amount-expense")
        );

        // Undo button
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button undoBtn = new Button("Undo Import");
        undoBtn.getStyleClass().addAll("button-danger", "undo-btn");
        undoBtn.setDisable(!item.canUndo());

        // Tooltip with reason (per /inga requirement)
        Tooltip undoTooltip = new Tooltip(item.getUndoTooltipText());
        undoBtn.setTooltip(undoTooltip);

        undoBtn.setOnAction(e -> handleUndoClick(item));

        // Undo status message if disabled
        if (!item.canUndo()) {
            Label reasonLabel = new Label(item.getUndoDisabledReason());
            reasonLabel.getStyleClass().add("undo-disabled-reason");
            actionRow.getChildren().addAll(undoBtn, reasonLabel);
        } else {
            actionRow.getChildren().add(undoBtn);
        }

        details.getChildren().addAll(formatRow, amountsRow, actionRow);
        return details;
    }

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelText = new Label(label + ":");
        labelText.getStyleClass().add("detail-label");

        Label valueText = new Label(value);
        valueText.getStyleClass().add("detail-value");

        row.getChildren().addAll(labelText, valueText);
        return row;
    }

    private VBox createAmountBox(String label, String count, String amount, String styleClass) {
        VBox box = new VBox(4);
        box.getStyleClass().add("amount-box");

        Label labelText = new Label(label);
        labelText.getStyleClass().add("amount-label");

        Label countText = new Label(count);
        countText.getStyleClass().add("amount-count");

        Label amountText = new Label(amount);
        amountText.getStyleClass().addAll("amount-value", styleClass);

        box.getChildren().addAll(labelText, countText, amountText);
        return box;
    }

    private String getStatusIcon(ImportStatus status) {
        return switch (status) {
            case ACTIVE -> "[check]";
            case UNDONE -> "[undo]";
            case LOCKED -> "[lock]";
        };
    }

    private void handleUndoClick(ImportHistoryItemViewModel item) {
        // Show confirmation dialog (per /inga requirement)
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Undo Import?");
        confirmation.setHeaderText("Are you sure you want to undo this import?");
        confirmation.setContentText(String.format(
            "This will remove:\n" +
            "- %d income records (%s)\n" +
            "- %d expense records (%s)\n\n" +
            "Your tax calculations will be updated automatically.\n\n" +
            "This action cannot be undone.",
            item.getIncomeRecords(), item.getFormattedIncomeTotal(),
            item.getExpenseRecords(), item.getFormattedExpenseTotal()
        ));

        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (onUndoImport != null) {
                    onUndoImport.accept(item);
                }
                LOG.info("Undo requested for import: {}", item.getId());
            }
        });
    }

    // === Action Handlers ===

    @FXML
    void handleNewImport(ActionEvent event) {
        if (onNewImport != null) {
            onNewImport.run();
        }
    }

    @FXML
    void handleFilterChange(ActionEvent event) {
        Toggle selected = filterGroup.getSelectedToggle();
        if (selected != null && selected.getUserData() instanceof ImportHistoryFilter filter) {
            viewModel.setFilter(filter);
            refreshHistoryList();
            LOG.info("Filter changed to: {}", filter);
        }
    }

    // === For Testing ===

    /**
     * Returns the ViewModel for testing.
     */
    public ImportHistoryViewModel getViewModel() {
        return viewModel;
    }

    /**
     * Sets the ViewModel directly (for testing).
     */
    public void setViewModel(ImportHistoryViewModel viewModel) {
        this.viewModel = viewModel;
        setupBindings();
        refreshHistoryList();
    }

    /**
     * Creates sample data for testing.
     */
    public static List<ImportHistoryItemViewModel> createSampleData() {
        List<ImportHistoryItemViewModel> samples = new ArrayList<>();

        // Recent active import
        samples.add(new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            LocalDateTime.now().minusDays(2),
            "barclays-january-2026.csv",
            "/path/to/file.csv",
            "Barclays",
            15, 42,
            new BigDecimal("3450.00"),
            new BigDecimal("1230.50"),
            ImportStatus.ACTIVE,
            null, null
        ));

        // Old import (cannot undo - older than 7 days)
        samples.add(new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            LocalDateTime.now().minusDays(15),
            "hsbc-december-2025.csv",
            "/path/to/file.csv",
            "HSBC",
            8, 23,
            new BigDecimal("2100.00"),
            new BigDecimal("890.25"),
            ImportStatus.ACTIVE,
            null, null
        ));

        // Locked import (used in tax submission)
        samples.add(new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            LocalDateTime.now().minusDays(30),
            "lloyds-november-2025.csv",
            "/path/to/file.csv",
            "Lloyds",
            12, 35,
            new BigDecimal("5200.00"),
            new BigDecimal("2150.75"),
            ImportStatus.LOCKED,
            null,
            LocalDateTime.now().minusDays(5)
        ));

        // Undone import
        samples.add(new ImportHistoryItemViewModel(
            UUID.randomUUID(),
            LocalDateTime.now().minusDays(3),
            "monzo-january-2026.csv",
            "/path/to/file.csv",
            "Monzo",
            5, 18,
            new BigDecimal("980.00"),
            new BigDecimal("450.30"),
            ImportStatus.UNDONE,
            LocalDateTime.now().minusDays(1),
            null
        ));

        return samples;
    }
}
