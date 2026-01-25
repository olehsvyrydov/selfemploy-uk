package uk.selfemploy.ui.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import uk.selfemploy.ui.util.DialogStyler;
import uk.selfemploy.ui.viewmodel.ImportAction;
import uk.selfemploy.ui.viewmodel.ImportCandidateViewModel;
import uk.selfemploy.ui.viewmodel.MatchedRecordViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Comparison dialog for reviewing duplicate transactions during import.
 * Shows a side-by-side comparison of the import candidate and the existing record.
 *
 * <p>SE-10B-005: Import Review UI - Comparison Dialog</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Side-by-side layout (importing vs existing)</li>
 *   <li>Difference highlighting (yellow background on differing fields)</li>
 *   <li>Action buttons: Import Both, Skip Import, Update Existing</li>
 *   <li>Full keyboard accessibility (Tab, Enter, Escape)</li>
 * </ul>
 */
public class ComparisonDialog {

    private static final double DIALOG_WIDTH = 700;
    private static final double PANEL_MIN_WIDTH = 280;
    private static final String CSS_PATH = "/css/import-review.css";

    private final Stage stage;
    private final ImportCandidateViewModel candidate;
    private ImportAction selectedAction;
    private Consumer<ImportAction> onActionSelected;

    // UI Components for testing
    private Button importBothBtn;
    private Button skipImportBtn;
    private Button updateExistingBtn;
    private Button closeBtn;

    /**
     * Creates a new comparison dialog.
     *
     * @param owner Owner window
     * @param candidate The import candidate to compare
     */
    public ComparisonDialog(Window owner, ImportCandidateViewModel candidate) {
        this.candidate = Objects.requireNonNull(candidate, "candidate cannot be null");
        this.selectedAction = candidate.getAction();

        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Compare Transactions");

        VBox container = buildContent();
        DialogStyler.setupFullyStyledDialog(stage, container, CSS_PATH, DialogStyler.CORNER_RADIUS);

        setupKeyboardNavigation(container);
    }

    /**
     * Sets the callback for when an action is selected.
     *
     * @param callback Callback receiving the selected action
     */
    public void setOnActionSelected(Consumer<ImportAction> callback) {
        this.onActionSelected = callback;
    }

    /**
     * Shows the dialog and waits for user action.
     *
     * @return The selected action, or empty if dialog was cancelled
     */
    public Optional<ImportAction> showAndWait() {
        stage.showAndWait();
        return Optional.ofNullable(selectedAction);
    }

    /**
     * Shows the dialog without blocking.
     */
    public void show() {
        stage.show();
    }

    /**
     * Returns the selected action.
     */
    public ImportAction getSelectedAction() {
        return selectedAction;
    }

    // === UI Building ===

    private VBox buildContent() {
        VBox container = new VBox(20);
        container.getStyleClass().add("comparison-dialog");
        container.setPadding(new Insets(24));
        container.setMinWidth(DIALOG_WIDTH);
        container.setMaxWidth(DIALOG_WIDTH);

        // Header
        HBox header = buildHeader();

        // Comparison panels
        HBox comparisonPanels = buildComparisonPanels();

        // Difference note
        HBox differenceNote = buildDifferenceNote();

        // Action buttons
        HBox actionButtons = buildActionButtons();

        container.getChildren().addAll(header, comparisonPanels, differenceNote, actionButtons);

        return container;
    }

    private HBox buildHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Compare Transactions");
        title.getStyleClass().add("dialog-title");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        closeBtn = new Button("X");
        closeBtn.getStyleClass().addAll("button-icon", "close-btn");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            selectedAction = null;
            stage.close();
        });
        closeBtn.setAccessibleText("Close dialog");

        header.getChildren().addAll(title, spacer, closeBtn);
        return header;
    }

    private HBox buildComparisonPanels() {
        HBox panels = new HBox(24);
        panels.setAlignment(Pos.TOP_CENTER);

        // Import candidate panel (left)
        VBox importingPanel = buildImportingPanel();

        // Existing record panel (right)
        VBox existingPanel = buildExistingPanel();

        panels.getChildren().addAll(importingPanel, existingPanel);
        HBox.setHgrow(importingPanel, Priority.ALWAYS);
        HBox.setHgrow(existingPanel, Priority.ALWAYS);

        return panels;
    }

    private VBox buildImportingPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("comparison-panel");
        panel.setMinWidth(PANEL_MIN_WIDTH);

        Label header = new Label("IMPORTING");
        header.getStyleClass().add("comparison-panel-header");

        MatchedRecordViewModel matchedRecord = candidate.getMatchedRecord();
        List<String> differences = calculateDifferences(candidate, matchedRecord);

        VBox fields = new VBox(0);
        fields.getChildren().addAll(
            buildComparisonField("Date", candidate.getFormattedDate(),
                matchedRecord != null && !Objects.equals(candidate.getFormattedDate(), matchedRecord.getFormattedDate())),
            buildComparisonField("Description", candidate.getDescription(),
                matchedRecord != null && !Objects.equals(candidate.getDescription(), matchedRecord.getDescription())),
            buildComparisonField("Amount", candidate.getFormattedAmount(),
                matchedRecord != null && !Objects.equals(candidate.getFormattedAmount(), matchedRecord.getFormattedAmount())),
            buildComparisonField("Category", candidate.getDisplayCategory(),
                matchedRecord != null && !Objects.equals(candidate.getDisplayCategory(), matchedRecord.getDisplayCategory()))
        );

        panel.getChildren().addAll(header, fields);
        return panel;
    }

    private VBox buildExistingPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("comparison-panel");
        panel.setMinWidth(PANEL_MIN_WIDTH);

        Label header = new Label("EXISTING");
        header.getStyleClass().add("comparison-panel-header");

        MatchedRecordViewModel matchedRecord = candidate.getMatchedRecord();

        VBox fields = new VBox(0);
        if (matchedRecord != null) {
            fields.getChildren().addAll(
                buildComparisonField("Date", matchedRecord.getFormattedDate(),
                    !Objects.equals(candidate.getFormattedDate(), matchedRecord.getFormattedDate())),
                buildComparisonField("Description", matchedRecord.getDescription(),
                    !Objects.equals(candidate.getDescription(), matchedRecord.getDescription())),
                buildComparisonField("Amount", matchedRecord.getFormattedAmount(),
                    !Objects.equals(candidate.getFormattedAmount(), matchedRecord.getFormattedAmount())),
                buildComparisonField("Category", matchedRecord.getDisplayCategory(),
                    !Objects.equals(candidate.getDisplayCategory(), matchedRecord.getDisplayCategory()))
            );
        } else {
            Label noRecord = new Label("No matching record details available");
            noRecord.setStyle("-fx-text-fill: -fx-text-muted; -fx-font-style: italic;");
            fields.getChildren().add(noRecord);
        }

        panel.getChildren().addAll(header, fields);
        return panel;
    }

    private VBox buildComparisonField(String label, String value, boolean isDifferent) {
        VBox field = new VBox(4);
        field.getStyleClass().add("comparison-field");
        if (isDifferent) {
            field.getStyleClass().add("comparison-field-different");
        }
        field.setPadding(new Insets(8, isDifferent ? 8 : 0, 8, isDifferent ? 8 : 0));

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("comparison-field-label");

        Label valueNode = new Label(value != null ? value : "-");
        valueNode.getStyleClass().add("comparison-field-value");
        valueNode.setWrapText(true);

        field.getChildren().addAll(labelNode, valueNode);
        return field;
    }

    private HBox buildDifferenceNote() {
        HBox note = new HBox(8);
        note.getStyleClass().add("comparison-difference-note");
        note.setAlignment(Pos.CENTER_LEFT);
        note.setPadding(new Insets(12));

        Label icon = new Label("!");
        icon.setStyle("-fx-font-weight: bold;");

        MatchedRecordViewModel matchedRecord = candidate.getMatchedRecord();
        List<String> differences = calculateDifferences(candidate, matchedRecord);

        String message;
        if (differences.isEmpty()) {
            message = "Records appear identical";
        } else if (differences.size() == 1) {
            message = "Difference: " + differences.get(0) + " varies";
        } else {
            message = "Differences: " + String.join(", ", differences) + " vary";
        }

        Label text = new Label(message);
        text.setStyle("-fx-font-size: 13px;");

        note.getChildren().addAll(icon, text);
        return note;
    }

    private List<String> calculateDifferences(ImportCandidateViewModel candidate, MatchedRecordViewModel matchedRecord) {
        List<String> differences = new ArrayList<>();

        if (matchedRecord == null) {
            return differences;
        }

        if (!Objects.equals(candidate.getFormattedDate(), matchedRecord.getFormattedDate())) {
            differences.add("Date");
        }
        if (!Objects.equals(candidate.getDescription(), matchedRecord.getDescription())) {
            differences.add("Description");
        }
        if (!Objects.equals(candidate.getFormattedAmount(), matchedRecord.getFormattedAmount())) {
            differences.add("Amount");
        }
        if (!Objects.equals(candidate.getDisplayCategory(), matchedRecord.getDisplayCategory())) {
            differences.add("Category");
        }

        return differences;
    }

    private HBox buildActionButtons() {
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(12, 0, 0, 0));

        importBothBtn = new Button("Import Both");
        importBothBtn.getStyleClass().addAll("button-primary-outline");
        importBothBtn.setOnAction(e -> selectAction(ImportAction.IMPORT));
        importBothBtn.setAccessibleText("Import both the new and existing records");

        skipImportBtn = new Button("Skip Import");
        skipImportBtn.getStyleClass().addAll("button-secondary");
        skipImportBtn.setOnAction(e -> selectAction(ImportAction.SKIP));
        skipImportBtn.setAccessibleText("Skip importing this record, keep existing");

        updateExistingBtn = new Button("Update Existing");
        updateExistingBtn.getStyleClass().addAll("button-primary");
        updateExistingBtn.setOnAction(e -> selectAction(ImportAction.UPDATE));
        updateExistingBtn.setAccessibleText("Update the existing record with the new data");

        buttons.getChildren().addAll(importBothBtn, skipImportBtn, updateExistingBtn);
        return buttons;
    }

    private void selectAction(ImportAction action) {
        this.selectedAction = action;
        if (onActionSelected != null) {
            onActionSelected.accept(action);
        }
        stage.close();
    }

    // === Keyboard Navigation ===

    private void setupKeyboardNavigation(VBox container) {
        container.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                selectedAction = null;
                stage.close();
                event.consume();
            }
        });

        // Focus first button when dialog opens
        stage.setOnShown(e -> importBothBtn.requestFocus());
    }

    // === Testing Support ===

    /**
     * Returns the stage for testing.
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Returns the Import Both button for testing.
     */
    public Button getImportBothButton() {
        return importBothBtn;
    }

    /**
     * Returns the Skip Import button for testing.
     */
    public Button getSkipImportButton() {
        return skipImportBtn;
    }

    /**
     * Returns the Update Existing button for testing.
     */
    public Button getUpdateExistingButton() {
        return updateExistingBtn;
    }

    /**
     * Returns the close button for testing.
     */
    public Button getCloseButton() {
        return closeBtn;
    }
}
