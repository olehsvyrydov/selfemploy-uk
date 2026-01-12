package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.enums.BusinessType;
import uk.selfemploy.ui.viewmodel.OnboardingViewModel;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the User Onboarding Wizard.
 * Handles the 4-step wizard flow and UI interactions.
 *
 * SE-702: User Onboarding Wizard
 */
public class OnboardingController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(OnboardingController.class);

    // === Progress Indicator ===
    @FXML private HBox progressContainer;
    @FXML private VBox step1Circle;
    @FXML private VBox step2Circle;
    @FXML private VBox step3Circle;
    @FXML private VBox step4Circle;

    // === Step Contents ===
    @FXML private VBox step1Content;
    @FXML private VBox step2Content;
    @FXML private VBox step3Content;
    @FXML private VBox step4Content;
    @FXML private VBox completionContent;

    // === Step 1: Welcome ===
    @FXML private Button getStartedBtn;

    // === Step 2: Your Details ===
    @FXML private TextField nameField;
    @FXML private TextField utrSegment1;
    @FXML private TextField utrSegment2;
    @FXML private TextField utrSegment3;
    @FXML private TextField niNumberField;
    @FXML private Hyperlink skipUtrLink;

    // === Step 3: Tax Year ===
    @FXML private VBox taxYearCardsContainer;

    // === Step 4: Business Type ===
    @FXML private VBox businessTypeCardsContainer;

    // === Completion Screen ===
    @FXML private Label completionTitle;
    @FXML private Hyperlink addIncomeLink;
    @FXML private Hyperlink addExpenseLink;
    @FXML private Hyperlink importCsvLink;
    @FXML private Button goToDashboardBtn;

    // === Footer ===
    @FXML private HBox footerContainer;
    @FXML private Hyperlink skipSetupLink;
    @FXML private Button backBtn;
    @FXML private Button continueBtn;

    private OnboardingViewModel viewModel;
    private Stage dialogStage;
    private Consumer<OnboardingViewModel.OnboardingCompletionSummary> onCompleteCallback;
    private ToggleGroup taxYearToggleGroup;
    private ToggleGroup businessTypeToggleGroup;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        viewModel = new OnboardingViewModel();

        setupStepBindings();
        setupStep2Bindings();
        setupStep3();
        setupStep4();
        setupNavigationBindings();

        // Initial state
        updateStepVisibility(1);
    }

    /**
     * Sets the dialog stage for closing.
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Sets the callback to be invoked when onboarding completes.
     */
    public void setOnCompleteCallback(Consumer<OnboardingViewModel.OnboardingCompletionSummary> callback) {
        this.onCompleteCallback = callback;
    }

    /**
     * Returns the ViewModel for testing.
     */
    public OnboardingViewModel getViewModel() {
        return viewModel;
    }

    // =====================================================
    // SETUP BINDINGS
    // =====================================================

    private void setupStepBindings() {
        viewModel.currentStepProperty().addListener((obs, oldVal, newVal) -> {
            updateStepIndicators(newVal.intValue());
            updateStepVisibility(newVal.intValue());
            updateNavigationButtons();
        });

        viewModel.completedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                showCompletionScreen();
            }
        });
    }

    private void updateStepIndicators(int currentStep) {
        VBox[] steps = {step1Circle, step2Circle, step3Circle, step4Circle};

        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == null) continue;

            int stepNumber = i + 1;
            steps[i].getStyleClass().removeAll("step-active", "step-completed");

            if (stepNumber < currentStep) {
                steps[i].getStyleClass().add("step-completed");
            } else if (stepNumber == currentStep) {
                steps[i].getStyleClass().add("step-active");
            }
        }
    }

    private void updateStepVisibility(int currentStep) {
        // Hide all step contents
        if (step1Content != null) {
            step1Content.setVisible(currentStep == 1);
            step1Content.setManaged(currentStep == 1);
        }
        if (step2Content != null) {
            step2Content.setVisible(currentStep == 2);
            step2Content.setManaged(currentStep == 2);
        }
        if (step3Content != null) {
            step3Content.setVisible(currentStep == 3);
            step3Content.setManaged(currentStep == 3);
        }
        if (step4Content != null) {
            step4Content.setVisible(currentStep == 4);
            step4Content.setManaged(currentStep == 4);
        }
        if (completionContent != null) {
            completionContent.setVisible(false);
            completionContent.setManaged(false);
        }

        // Show/hide progress indicator (visible from step 2)
        if (progressContainer != null) {
            progressContainer.setVisible(currentStep > 1);
            progressContainer.setManaged(currentStep > 1);
        }

        // Show/hide footer (visible from step 2)
        if (footerContainer != null) {
            footerContainer.setVisible(currentStep > 1);
            footerContainer.setManaged(currentStep > 1);
        }
    }

    private void setupStep2Bindings() {
        if (nameField != null) {
            nameField.textProperty().bindBidirectional(viewModel.userNameProperty());
            nameField.textProperty().addListener((obs, oldVal, newVal) -> updateNavigationButtons());
        }

        // UTR segmented input bindings
        if (utrSegment1 != null) {
            setupUtrSegmentField(utrSegment1, utrSegment2, 4);
        }
        if (utrSegment2 != null) {
            setupUtrSegmentField(utrSegment2, utrSegment3, 3);
        }
        if (utrSegment3 != null) {
            setupUtrSegmentField(utrSegment3, null, 3);
        }

        // Update UTR in viewModel when any segment changes
        setupUtrListeners();

        if (niNumberField != null) {
            niNumberField.textProperty().bindBidirectional(viewModel.niNumberProperty());
        }
    }

    private void setupUtrSegmentField(TextField field, TextField nextField, int maxLength) {
        // Restrict to digits only
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
            }
            // Auto-advance to next field
            if (nextField != null && newVal != null && newVal.length() >= maxLength) {
                nextField.requestFocus();
            }
        });
    }

    private void setupUtrListeners() {
        if (utrSegment1 != null && utrSegment2 != null && utrSegment3 != null) {
            Runnable updateUtr = () -> {
                viewModel.setUtrFromSegments(
                        utrSegment1.getText(),
                        utrSegment2.getText(),
                        utrSegment3.getText()
                );
                updateNavigationButtons();
            };

            utrSegment1.textProperty().addListener((obs, oldVal, newVal) -> updateUtr.run());
            utrSegment2.textProperty().addListener((obs, oldVal, newVal) -> updateUtr.run());
            utrSegment3.textProperty().addListener((obs, oldVal, newVal) -> updateUtr.run());
        }
    }

    private void setupStep3() {
        if (taxYearCardsContainer == null) return;

        taxYearToggleGroup = new ToggleGroup();

        for (String taxYear : viewModel.getAvailableTaxYears()) {
            VBox card = createTaxYearCard(taxYear);
            taxYearCardsContainer.getChildren().add(card);
        }

        taxYearToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String selectedYear = (String) newVal.getUserData();
                viewModel.setSelectedTaxYear(selectedYear);
                updateTaxYearCardStyles();
                updateNavigationButtons();
            }
        });
    }

    private VBox createTaxYearCard(String taxYear) {
        VBox card = new VBox(4);
        card.getStyleClass().add("selection-card");
        card.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        RadioButton radio = new RadioButton();
        radio.setToggleGroup(taxYearToggleGroup);
        radio.setUserData(taxYear);

        Label yearLabel = new Label(taxYear);
        yearLabel.getStyleClass().add("card-title");

        Label dateRange = new Label("(" + viewModel.getTaxYearDateRange(taxYear) + ")");
        dateRange.getStyleClass().add("card-subtitle");

        header.getChildren().addAll(radio, yearLabel, dateRange);

        // Recommended badge
        if (viewModel.isTaxYearRecommended(taxYear)) {
            Label badge = new Label("RECOMMENDED");
            badge.getStyleClass().add("recommended-badge");
            header.getChildren().add(badge);

            // Pre-select recommended
            radio.setSelected(true);
        }

        Label description = new Label(viewModel.isTaxYearRecommended(taxYear)
                ? "Current tax year"
                : "Past tax year - for late filings");
        description.getStyleClass().add("card-description");

        card.getChildren().addAll(header, description);

        // Make entire card clickable
        card.setOnMouseClicked(e -> radio.setSelected(true));

        return card;
    }

    private void updateTaxYearCardStyles() {
        for (int i = 0; i < taxYearCardsContainer.getChildren().size(); i++) {
            VBox card = (VBox) taxYearCardsContainer.getChildren().get(i);
            String taxYear = viewModel.getAvailableTaxYears().get(i);

            card.getStyleClass().remove("selected");
            if (taxYear.equals(viewModel.getSelectedTaxYear())) {
                card.getStyleClass().add("selected");
            }
        }
    }

    private void setupStep4() {
        if (businessTypeCardsContainer == null) return;

        businessTypeToggleGroup = new ToggleGroup();

        for (BusinessType type : viewModel.getAvailableBusinessTypes()) {
            VBox card = createBusinessTypeCard(type);
            businessTypeCardsContainer.getChildren().add(card);
        }

        businessTypeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                BusinessType selectedType = (BusinessType) newVal.getUserData();
                viewModel.setSelectedBusinessType(selectedType);
                updateBusinessTypeCardStyles();
            }
        });
    }

    private VBox createBusinessTypeCard(BusinessType type) {
        VBox card = new VBox(8);
        card.getStyleClass().add("selection-card");
        card.setAlignment(Pos.CENTER_LEFT);

        boolean enabled = viewModel.isBusinessTypeEnabled(type);
        if (!enabled) {
            card.getStyleClass().add("disabled");
        }

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icon
        Label icon = new Label(getBusinessTypeIcon(type));
        icon.getStyleClass().add("card-icon");

        VBox textContainer = new VBox(2);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(viewModel.getBusinessTypeDisplayName(type));
        title.getStyleClass().add("card-title");
        titleRow.getChildren().add(title);

        if (!enabled) {
            Label badge = new Label("Coming Soon");
            badge.getStyleClass().add("coming-soon-badge");
            titleRow.getChildren().add(badge);
        }

        Label description = new Label(viewModel.getBusinessTypeDescription(type));
        description.getStyleClass().add("card-description");

        textContainer.getChildren().addAll(titleRow, description);

        RadioButton radio = new RadioButton();
        radio.setToggleGroup(businessTypeToggleGroup);
        radio.setUserData(type);
        radio.setDisable(!enabled);

        header.getChildren().addAll(icon, textContainer, radio);

        card.getChildren().add(header);

        // Make entire card clickable if enabled
        if (enabled) {
            card.setOnMouseClicked(e -> radio.setSelected(true));
        }

        return card;
    }

    private String getBusinessTypeIcon(BusinessType type) {
        return switch (type) {
            case SOLE_TRADER, FREELANCER -> "[PERSON]";
            case CONTRACTOR -> "[BRIEFCASE]";
            case PARTNERSHIP -> "[PEOPLE]";
            default -> "[?]";
        };
    }

    private void updateBusinessTypeCardStyles() {
        var types = viewModel.getAvailableBusinessTypes();
        for (int i = 0; i < businessTypeCardsContainer.getChildren().size(); i++) {
            VBox card = (VBox) businessTypeCardsContainer.getChildren().get(i);
            BusinessType type = types.get(i);

            card.getStyleClass().remove("selected");
            if (type.equals(viewModel.getSelectedBusinessType())) {
                card.getStyleClass().add("selected");
            }
        }
    }

    private void setupNavigationBindings() {
        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        if (backBtn != null) {
            backBtn.setDisable(!viewModel.canGoPrevious());
        }

        if (continueBtn != null) {
            boolean canProceed = viewModel.canGoNext() || viewModel.canComplete();
            continueBtn.setDisable(!canProceed);

            String buttonText = switch (viewModel.getCurrentStep()) {
                case 2 -> "Continue";
                case 3 -> "Continue";
                case 4 -> "Finish Setup";
                default -> "Continue";
            };
            continueBtn.setText(buttonText);

            // Change style for finish button
            continueBtn.getStyleClass().remove("button-success");
            if (viewModel.getCurrentStep() == 4) {
                continueBtn.getStyleClass().add("button-success");
            }
        }

        if (skipSetupLink != null) {
            skipSetupLink.setVisible(viewModel.canSkip());
            skipSetupLink.setManaged(viewModel.canSkip());
        }
    }

    private void showCompletionScreen() {
        // Hide all step content
        if (step1Content != null) {
            step1Content.setVisible(false);
            step1Content.setManaged(false);
        }
        if (step2Content != null) {
            step2Content.setVisible(false);
            step2Content.setManaged(false);
        }
        if (step3Content != null) {
            step3Content.setVisible(false);
            step3Content.setManaged(false);
        }
        if (step4Content != null) {
            step4Content.setVisible(false);
            step4Content.setManaged(false);
        }

        // Hide progress and footer
        if (progressContainer != null) {
            progressContainer.setVisible(false);
            progressContainer.setManaged(false);
        }
        if (footerContainer != null) {
            footerContainer.setVisible(false);
            footerContainer.setManaged(false);
        }

        // Show completion screen
        if (completionContent != null) {
            completionContent.setVisible(true);
            completionContent.setManaged(true);
        }

        // Update completion title
        if (completionTitle != null) {
            completionTitle.setText(viewModel.getPersonalizedWelcome());
        }

        LOG.info("Onboarding completed for user: {}", viewModel.getUserName());
    }

    // =====================================================
    // EVENT HANDLERS
    // =====================================================

    @FXML
    void handleGetStarted(ActionEvent event) {
        viewModel.goToNextStep();
    }

    @FXML
    void handleBack(ActionEvent event) {
        viewModel.goToPreviousStep();
    }

    @FXML
    void handleContinue(ActionEvent event) {
        if (viewModel.getCurrentStep() == 4) {
            viewModel.complete();
            notifyCompletion();
        } else {
            viewModel.goToNextStep();
        }
    }

    @FXML
    void handleSkipSetup(ActionEvent event) {
        // Show confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Skip Setup");
        confirm.setHeaderText("Are you sure you want to skip?");
        confirm.setContentText("You can add your details later in Settings, " +
                "but some features may require them.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                viewModel.skipSetup();
                notifyCompletion();
            }
        });
    }

    @FXML
    void handleAddIncome(ActionEvent event) {
        LOG.info("Navigate to Add Income");
        closeAndNavigate("income");
    }

    @FXML
    void handleAddExpense(ActionEvent event) {
        LOG.info("Navigate to Add Expense");
        closeAndNavigate("expense");
    }

    @FXML
    void handleImportCsv(ActionEvent event) {
        LOG.info("Navigate to Import CSV");
        closeAndNavigate("import");
    }

    @FXML
    void handleGoToDashboard(ActionEvent event) {
        LOG.info("Navigate to Dashboard");
        closeAndNavigate("dashboard");
    }

    private void notifyCompletion() {
        if (onCompleteCallback != null) {
            onCompleteCallback.accept(viewModel.getCompletionSummary());
        }
    }

    private void closeAndNavigate(String destination) {
        notifyCompletion();
        if (dialogStage != null) {
            dialogStage.close();
        }
        // Navigation to destination would be handled by the main application controller
        LOG.info("Onboarding wizard closed, navigate to: {}", destination);
    }
}
