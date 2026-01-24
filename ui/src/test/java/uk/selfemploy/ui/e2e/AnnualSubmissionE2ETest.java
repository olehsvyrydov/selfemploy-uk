package uk.selfemploy.ui.e2e;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.controller.AnnualSubmissionController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

/**
 * E2E-002: Annual Submission Complete Flow with Dynamic Width.
 *
 * <p>P0 Critical Tests for SE-403 and SE-512: Annual Self Assessment Submission.</p>
 *
 * <p>Verifies the complete annual submission flow:
 * <ul>
 *   <li>Dialog width changes per step (700px -> 840px -> 1220px)</li>
 *   <li>Step navigation (Next/Back buttons)</li>
 *   <li>All 6 declaration checkboxes functionality</li>
 *   <li>AS-021: Submit button disabled until all 6 checkboxes checked</li>
 *   <li>AS-022: Submit button enabled when all 6 checkboxes checked</li>
 * </ul>
 *
 * <p>These tests require a display. Run with {@code -Dgroups=e2e} to include,
 * or {@code -DexcludedGroups=e2e} to exclude.</p>
 */
@DisplayName("E2E-002: Annual Submission Complete Flow")
@Tag("e2e")
@DisabledIfSystemProperty(named = "skipE2ETests", matches = "true")
class AnnualSubmissionE2ETest extends ApplicationTest {

    private static final int STEP_1_WIDTH = 700;
    private static final int STEP_2_WIDTH = 840;
    private static final int STEP_3_WIDTH = 1220;
    private static final int WIDTH_TOLERANCE = 20; // Allow some tolerance for window decorations

    private Stage dialogStage;
    private AnnualSubmissionController controller;

    @BeforeAll
    static void setupHeadlessMode() {
        // Configure TestFX for headless mode if requested
        if (Boolean.getBoolean("headless") || Boolean.getBoolean("testfx.headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.dialogStage = stage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/annual-submission.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, STEP_1_WIDTH, 700);

        // Use test CSS to avoid StackOverflow from CSS lookup chains
        scene.getStylesheets().clear();
        String testCss = getClass().getResource("/css/test-minimal.css").toExternalForm();
        scene.getStylesheets().add(testCss);

        stage.setTitle("Annual Self Assessment Submission");
        stage.setScene(scene);
        stage.setWidth(STEP_1_WIDTH);
        stage.show();

        // Set dialog stage for dynamic resizing
        controller.setDialogStage(stage);

        // Initialize with test data
        TaxYear taxYear = TaxYear.of(2025);
        controller.initializeSubmission(
                taxYear,
                new BigDecimal("45000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("40000.00")
        );

        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void tearDown() throws TimeoutException {
        // Release all keys and mouse buttons
        release(new KeyCode[]{});
        release(new MouseButton[]{});

        // Close any open dialogs/stages
        FxToolkit.hideStage();
    }

    /**
     * Waits for JavaFX events to complete.
     */
    private void waitForFxEvents() {
        WaitForAsyncUtils.waitForFxEvents();
    }

    // === Step Navigation Tests ===

    @Nested
    @DisplayName("Step Navigation")
    class StepNavigationTests {

        @Test
        @DisplayName("TC-AS-01: Initial step is Step 1 - Review Summary (CRITICAL)")
        void initialStepIsStep1() {
            // Then: Step 1 should be active
            VBox step1Container = lookup("#step1Container").queryAs(VBox.class);
            assertThat(step1Container.getStyleClass()).contains("step-active");

            // And: Calculate button should be visible
            Button calculateButton = lookup("#calculateButton").queryAs(Button.class);
            assertThat(calculateButton.isVisible()).isTrue();
            assertThat(calculateButton.isManaged()).isTrue();

            // And: Summary panel should be visible
            VBox summaryPanel = lookup("#summaryPanel").queryAs(VBox.class);
            assertThat(summaryPanel.isVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-AS-02: Calculate button advances to Step 2 (CRITICAL)")
        void calculateButtonAdvancesToStep2() {
            // Given: On Step 1
            VBox step1Container = lookup("#step1Container").queryAs(VBox.class);
            assertThat(step1Container.getStyleClass()).contains("step-active");

            // When: Click Calculate button
            clickOn("#calculateButton");
            waitForFxEvents();

            // Then: Step 2 should be active
            VBox step2Container = lookup("#step2Container").queryAs(VBox.class);
            assertThat(step2Container.getStyleClass()).contains("step-active");

            // And: Step 1 should be completed
            assertThat(step1Container.getStyleClass()).contains("step-completed");

            // And: Calculation panel should be visible
            VBox calculationPanel = lookup("#calculationPanel").queryAs(VBox.class);
            assertThat(calculationPanel.isVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-AS-03: Review button advances to Step 3 (CRITICAL)")
        void reviewButtonAdvancesToStep3() throws InterruptedException {
            // Given: Advance to Step 2
            clickOn("#calculateButton");
            waitForFxEvents();

            // Wait for simulated calculation to complete (2 seconds in controller)
            Thread.sleep(2500);
            waitForFxEvents();

            // When: Review button should be enabled
            Button reviewButton = lookup("#reviewButton").queryAs(Button.class);
            assertThat(reviewButton.isDisabled()).isFalse();

            // Click Review button
            clickOn("#reviewButton");
            waitForFxEvents();

            // Then: Step 3 should be active
            VBox step3Container = lookup("#step3Container").queryAs(VBox.class);
            assertThat(step3Container.getStyleClass()).contains("step-active");

            // And: Declaration card should be visible
            VBox declarationCard = lookup("#declarationCard").queryAs(VBox.class);
            assertThat(declarationCard.isVisible()).isTrue();
            assertThat(declarationCard.isManaged()).isTrue();
        }

        @Test
        @DisplayName("TC-AS-04: Step progress indicator updates correctly (HIGH)")
        void stepProgressIndicatorUpdatesCorrectly() throws InterruptedException {
            // Given: On Step 1 - only Step 1 is active
            VBox step1Container = lookup("#step1Container").queryAs(VBox.class);
            VBox step2Container = lookup("#step2Container").queryAs(VBox.class);
            VBox step3Container = lookup("#step3Container").queryAs(VBox.class);

            assertThat(step1Container.getStyleClass()).contains("step-active");
            assertThat(step2Container.getStyleClass()).doesNotContain("step-active", "step-completed");

            // When: Advance to Step 2
            clickOn("#calculateButton");
            waitForFxEvents();

            // Then: Step 1 completed, Step 2 active
            assertThat(step1Container.getStyleClass()).contains("step-completed");
            assertThat(step2Container.getStyleClass()).contains("step-active");

            // Wait for calculation and advance to Step 3
            Thread.sleep(2500);
            waitForFxEvents();
            clickOn("#reviewButton");
            waitForFxEvents();

            // Then: Steps 1,2 completed, Step 3 active
            assertThat(step1Container.getStyleClass()).contains("step-completed");
            assertThat(step2Container.getStyleClass()).contains("step-completed");
            assertThat(step3Container.getStyleClass()).contains("step-active");
        }
    }

    // === Dynamic Width Tests ===

    @Nested
    @DisplayName("Dynamic Width Changes")
    class DynamicWidthTests {

        @Test
        @DisplayName("TC-AS-05: Step 1 width is 700px (HIGH)")
        void step1WidthIs700px() {
            // Given: On Step 1

            // Then: Dialog width should be approximately 700px
            double width = dialogStage.getWidth();
            assertThat(width).isBetween(
                    (double) STEP_1_WIDTH - WIDTH_TOLERANCE,
                    (double) STEP_1_WIDTH + WIDTH_TOLERANCE
            );
        }

        @Test
        @DisplayName("TC-AS-06: Step 2 width changes to 840px (HIGH)")
        void step2WidthChangesTo840px() {
            // Given: On Step 1
            double initialWidth = dialogStage.getWidth();

            // When: Advance to Step 2
            clickOn("#calculateButton");
            waitForFxEvents();

            // Allow time for resize animation
            sleep(200);

            // Then: Dialog width should be approximately 840px
            double newWidth = dialogStage.getWidth();
            assertThat(newWidth).isBetween(
                    (double) STEP_2_WIDTH - WIDTH_TOLERANCE,
                    (double) STEP_2_WIDTH + WIDTH_TOLERANCE
            );

            // And: Width should have increased
            assertThat(newWidth).isGreaterThan(initialWidth);
        }

        @Test
        @DisplayName("TC-AS-07: Step 3 width changes to 1220px (HIGH)")
        void step3WidthChangesTo1220px() throws InterruptedException {
            // Given: Advance to Step 2
            clickOn("#calculateButton");
            waitForFxEvents();

            // Wait for calculation
            Thread.sleep(2500);
            waitForFxEvents();

            double step2Width = dialogStage.getWidth();

            // When: Advance to Step 3
            clickOn("#reviewButton");
            waitForFxEvents();

            // Allow time for resize animation
            sleep(200);

            // Then: Dialog width should be approximately 1220px
            double newWidth = dialogStage.getWidth();
            assertThat(newWidth).isBetween(
                    (double) STEP_3_WIDTH - WIDTH_TOLERANCE,
                    (double) STEP_3_WIDTH + WIDTH_TOLERANCE
            );

            // And: Width should have increased from Step 2
            assertThat(newWidth).isGreaterThan(step2Width);
        }
    }

    // === 6 Declaration Checkboxes Tests ===

    @Nested
    @DisplayName("Declaration Checkboxes")
    class DeclarationCheckboxesTests {

        @BeforeEach
        void advanceToStep3() throws InterruptedException {
            // Advance to Step 3 where declaration checkboxes are visible
            clickOn("#calculateButton");
            waitForFxEvents();

            // Wait for simulated calculation to complete
            Thread.sleep(2500);
            waitForFxEvents();

            clickOn("#reviewButton");
            waitForFxEvents();
        }

        @Test
        @DisplayName("TC-AS-08: All 6 declaration checkboxes are visible (CRITICAL)")
        void allSixDeclarationCheckboxesAreVisible() {
            // Then: All 6 checkboxes should be visible
            CheckBox decl1 = lookup("#decl1Checkbox").queryAs(CheckBox.class);
            CheckBox decl2 = lookup("#decl2Checkbox").queryAs(CheckBox.class);
            CheckBox decl3 = lookup("#decl3Checkbox").queryAs(CheckBox.class);
            CheckBox decl4 = lookup("#decl4Checkbox").queryAs(CheckBox.class);
            CheckBox decl5 = lookup("#decl5Checkbox").queryAs(CheckBox.class);
            CheckBox decl6 = lookup("#decl6Checkbox").queryAs(CheckBox.class);

            assertThat(decl1.isVisible()).isTrue();
            assertThat(decl2.isVisible()).isTrue();
            assertThat(decl3.isVisible()).isTrue();
            assertThat(decl4.isVisible()).isTrue();
            assertThat(decl5.isVisible()).isTrue();
            assertThat(decl6.isVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-AS-09: All 6 checkboxes are initially unchecked (CRITICAL)")
        void allSixCheckboxesAreInitiallyUnchecked() {
            // Then: All 6 checkboxes should be unchecked
            CheckBox decl1 = lookup("#decl1Checkbox").queryAs(CheckBox.class);
            CheckBox decl2 = lookup("#decl2Checkbox").queryAs(CheckBox.class);
            CheckBox decl3 = lookup("#decl3Checkbox").queryAs(CheckBox.class);
            CheckBox decl4 = lookup("#decl4Checkbox").queryAs(CheckBox.class);
            CheckBox decl5 = lookup("#decl5Checkbox").queryAs(CheckBox.class);
            CheckBox decl6 = lookup("#decl6Checkbox").queryAs(CheckBox.class);

            assertThat(decl1.isSelected()).isFalse();
            assertThat(decl2.isSelected()).isFalse();
            assertThat(decl3.isSelected()).isFalse();
            assertThat(decl4.isSelected()).isFalse();
            assertThat(decl5.isSelected()).isFalse();
            assertThat(decl6.isSelected()).isFalse();
        }

        @Test
        @DisplayName("TC-AS-10: Clicking checkbox toggles its state (HIGH)")
        void clickingCheckboxTogglesState() {
            // Given: Checkbox 1 is unchecked
            CheckBox decl1 = lookup("#decl1Checkbox").queryAs(CheckBox.class);
            assertThat(decl1.isSelected()).isFalse();

            // When: Click checkbox 1
            clickOn("#decl1Checkbox");
            waitForFxEvents();

            // Then: Checkbox 1 should be checked
            assertThat(decl1.isSelected()).isTrue();

            // When: Click checkbox 1 again
            clickOn("#decl1Checkbox");
            waitForFxEvents();

            // Then: Checkbox 1 should be unchecked
            assertThat(decl1.isSelected()).isFalse();
        }

        @Test
        @DisplayName("TC-AS-11: Progress label updates as checkboxes are checked (HIGH)")
        void progressLabelUpdatesAsCheckboxesAreChecked() {
            // Given: Progress shows 0 of 6
            Label progressLabel = lookup("#progressLabel").queryAs(Label.class);
            assertThat(progressLabel.getText()).contains("0 of 6");

            // When: Check first checkbox
            clickOn("#decl1Checkbox");
            waitForFxEvents();

            // Then: Progress shows 1 of 6
            assertThat(progressLabel.getText()).contains("1 of 6");

            // When: Check second checkbox
            clickOn("#decl2Checkbox");
            waitForFxEvents();

            // Then: Progress shows 2 of 6
            assertThat(progressLabel.getText()).contains("2 of 6");

            // When: Check remaining checkboxes
            clickOn("#decl3Checkbox");
            clickOn("#decl4Checkbox");
            clickOn("#decl5Checkbox");
            clickOn("#decl6Checkbox");
            waitForFxEvents();

            // Then: Progress shows 6 of 6
            assertThat(progressLabel.getText()).contains("6 of 6");
        }

        @Test
        @DisplayName("TC-AS-12: Timestamp section appears when all 6 are checked (HIGH)")
        void timestampSectionAppearsWhenAllSixAreChecked() {
            // Given: Timestamp section is hidden
            VBox timestampSection = lookup("#timestampSection").queryAs(VBox.class);
            assertThat(timestampSection.isVisible()).isFalse();

            // When: Check all 6 checkboxes
            clickOn("#decl1Checkbox");
            clickOn("#decl2Checkbox");
            clickOn("#decl3Checkbox");
            clickOn("#decl4Checkbox");
            clickOn("#decl5Checkbox");
            clickOn("#decl6Checkbox");
            waitForFxEvents();

            // Then: Timestamp section should be visible
            assertThat(timestampSection.isVisible()).isTrue();
            assertThat(timestampSection.isManaged()).isTrue();

            // And: Timestamp label should have content
            Label timestampLabel = lookup("#timestampLabel").queryAs(Label.class);
            assertThat(timestampLabel.getText()).isNotEmpty();
        }

        @Test
        @DisplayName("TC-AS-13: Row gets 'checked' style when checkbox is selected (MEDIUM)")
        void rowGetsCheckedStyleWhenCheckboxIsSelected() {
            // Given: Row 1 does not have 'checked' style
            HBox decl1Row = lookup("#decl1Row").queryAs(HBox.class);
            assertThat(decl1Row.getStyleClass()).doesNotContain("checked");

            // When: Check checkbox 1
            clickOn("#decl1Checkbox");
            waitForFxEvents();

            // Then: Row 1 should have 'checked' style
            assertThat(decl1Row.getStyleClass()).contains("checked");

            // When: Uncheck checkbox 1
            clickOn("#decl1Checkbox");
            waitForFxEvents();

            // Then: Row 1 should not have 'checked' style
            assertThat(decl1Row.getStyleClass()).doesNotContain("checked");
        }
    }

    // === Submit Button State Tests (AS-021 & AS-022) ===

    @Nested
    @DisplayName("Submit Button State (AS-021 & AS-022)")
    class SubmitButtonStateTests {

        @BeforeEach
        void advanceToStep3() throws InterruptedException {
            // Advance to Step 3 where submit button is visible
            clickOn("#calculateButton");
            waitForFxEvents();

            // Wait for simulated calculation to complete
            Thread.sleep(2500);
            waitForFxEvents();

            clickOn("#reviewButton");
            waitForFxEvents();
        }

        @Test
        @DisplayName("AS-021: Submit button is disabled when 0 checkboxes checked (CRITICAL)")
        void submitButtonDisabledWhenZeroCheckboxesChecked() {
            // Given: No checkboxes are checked
            CheckBox decl1 = lookup("#decl1Checkbox").queryAs(CheckBox.class);
            assertThat(decl1.isSelected()).isFalse();

            // Then: Submit button should be disabled
            Button submitButton = lookup("#submitButton").queryAs(Button.class);
            assertThat(submitButton.isDisabled()).isTrue();
        }

        @Test
        @DisplayName("AS-021: Submit button is disabled when 5 checkboxes checked (CRITICAL)")
        void submitButtonDisabledWhenFiveCheckboxesChecked() {
            // When: Check only 5 checkboxes (not all 6)
            clickOn("#decl1Checkbox");
            clickOn("#decl2Checkbox");
            clickOn("#decl3Checkbox");
            clickOn("#decl4Checkbox");
            clickOn("#decl5Checkbox");
            waitForFxEvents();

            // Then: Submit button should still be disabled
            Button submitButton = lookup("#submitButton").queryAs(Button.class);
            assertThat(submitButton.isDisabled()).isTrue();
        }

        @Test
        @DisplayName("AS-022: Submit button is enabled when all 6 checkboxes checked (CRITICAL)")
        void submitButtonEnabledWhenAllSixCheckboxesChecked() {
            // Given: Submit button is disabled
            Button submitButton = lookup("#submitButton").queryAs(Button.class);
            assertThat(submitButton.isDisabled()).isTrue();

            // When: Check all 6 checkboxes
            clickOn("#decl1Checkbox");
            clickOn("#decl2Checkbox");
            clickOn("#decl3Checkbox");
            clickOn("#decl4Checkbox");
            clickOn("#decl5Checkbox");
            clickOn("#decl6Checkbox");
            waitForFxEvents();

            // Then: Submit button should be enabled
            assertThat(submitButton.isDisabled()).isFalse();
        }

        @Test
        @DisplayName("AS-021: Submit button becomes disabled again if checkbox unchecked (CRITICAL)")
        void submitButtonBecomesDisabledIfCheckboxUnchecked() {
            // Given: All 6 checkboxes are checked and submit is enabled
            clickOn("#decl1Checkbox");
            clickOn("#decl2Checkbox");
            clickOn("#decl3Checkbox");
            clickOn("#decl4Checkbox");
            clickOn("#decl5Checkbox");
            clickOn("#decl6Checkbox");
            waitForFxEvents();

            Button submitButton = lookup("#submitButton").queryAs(Button.class);
            assertThat(submitButton.isDisabled()).isFalse();

            // When: Uncheck one checkbox
            clickOn("#decl1Checkbox");
            waitForFxEvents();

            // Then: Submit button should be disabled again
            assertThat(submitButton.isDisabled()).isTrue();
        }

        @Test
        @DisplayName("AS-022: Submit helper text changes when all checked (HIGH)")
        void submitHelperTextChangesWhenAllChecked() {
            // Given: Helper text shows "please confirm"
            Label submitHelperText = lookup("#submitHelperText").queryAs(Label.class);
            assertThat(submitHelperText.getText().toLowerCase()).contains("please confirm");

            // When: Check all 6 checkboxes
            clickOn("#decl1Checkbox");
            clickOn("#decl2Checkbox");
            clickOn("#decl3Checkbox");
            clickOn("#decl4Checkbox");
            clickOn("#decl5Checkbox");
            clickOn("#decl6Checkbox");
            waitForFxEvents();

            // Then: Helper text should indicate ready
            assertThat(submitHelperText.getText().toLowerCase()).contains("ready");
        }

        @Test
        @DisplayName("Submit button visible only in Step 3 (HIGH)")
        void submitButtonVisibleOnlyInStep3() {
            // Note: We're already in Step 3 from @BeforeEach

            // Then: Submit container should be visible
            VBox submitContainer = lookup("#submitContainer").queryAs(VBox.class);
            assertThat(submitContainer.isVisible()).isTrue();
            assertThat(submitContainer.isManaged()).isTrue();
        }
    }

    // === Disclaimer Banner Tests (SE-509) ===

    @Nested
    @DisplayName("Disclaimer Banner (SE-509)")
    class DisclaimerBannerTests {

        @BeforeEach
        void advanceToStep3() throws InterruptedException {
            // Advance to Step 3 where disclaimer is visible
            clickOn("#calculateButton");
            waitForFxEvents();

            // Wait for simulated calculation to complete
            Thread.sleep(2500);
            waitForFxEvents();

            clickOn("#reviewButton");
            waitForFxEvents();
        }

        @Test
        @DisplayName("TC-AS-14: Submission disclaimer banner visible in Step 3 (HIGH)")
        void submissionDisclaimerBannerVisibleInStep3() {
            // Then: Disclaimer banner should be visible
            HBox disclaimerBanner = lookup("#submissionDisclaimerBanner").queryAs(HBox.class);
            assertThat(disclaimerBanner.isVisible()).isTrue();
            assertThat(disclaimerBanner.isManaged()).isTrue();
        }

        @Test
        @DisplayName("TC-AS-15: Disclaimer text contains HMRC warning (HIGH)")
        void disclaimerTextContainsHmrcWarning() {
            // Then: Disclaimer text should contain HMRC-related content
            Label disclaimerText = lookup("#submissionDisclaimerText").queryAs(Label.class);
            String text = disclaimerText.getText().toLowerCase();

            assertThat(text).satisfiesAnyOf(
                    t -> assertThat(t).contains("hmrc"),
                    t -> assertThat(t).contains("submit"),
                    t -> assertThat(t).contains("accurate"),
                    t -> assertThat(t).contains("responsible")
            );
        }

        @Test
        @DisplayName("TC-AS-16: Disclaimer cannot be dismissed (has persistent class) (HIGH)")
        void disclaimerCannotBeDismissed() {
            // Then: Disclaimer banner should have persistent class
            HBox disclaimerBanner = lookup("#submissionDisclaimerBanner").queryAs(HBox.class);
            assertThat(disclaimerBanner.getStyleClass()).contains("disclaimer-persistent");

            // And: There should be no close/dismiss button in the disclaimer
            // The disclaimer should remain visible after any interaction
            clickOn("#decl1Checkbox"); // Interact with page
            waitForFxEvents();

            assertThat(disclaimerBanner.isVisible()).isTrue();
        }
    }

    // === Financial Data Display Tests ===

    @Nested
    @DisplayName("Financial Data Display")
    class FinancialDataDisplayTests {

        @Test
        @DisplayName("TC-AS-17: Turnover value displays correctly (HIGH)")
        void turnoverValueDisplaysCorrectly() {
            // Then: Turnover should show the initialized value
            Label turnoverValue = lookup("#turnoverValue").queryAs(Label.class);
            // Initialized with 45000.00
            assertThat(turnoverValue.getText()).contains("45,000");
        }

        @Test
        @DisplayName("TC-AS-18: Expenses value displays correctly (HIGH)")
        void expensesValueDisplaysCorrectly() {
            // Then: Expenses should show the initialized value
            Label expensesValue = lookup("#expensesValue").queryAs(Label.class);
            // Initialized with 5000.00
            assertThat(expensesValue.getText()).contains("5,000");
        }

        @Test
        @DisplayName("TC-AS-19: Net profit value displays correctly (HIGH)")
        void netProfitValueDisplaysCorrectly() {
            // Then: Net profit should show the initialized value
            Label netProfitValue = lookup("#netProfitValue").queryAs(Label.class);
            // Initialized with 40000.00
            assertThat(netProfitValue.getText()).contains("40,000");
        }

        @Test
        @DisplayName("TC-AS-20: Tax year label displays correctly (HIGH)")
        void taxYearLabelDisplaysCorrectly() {
            // Then: Tax year label should show correct year
            Label taxYearLabel = lookup("#taxYearLabel").queryAs(Label.class);
            // Initialized with TaxYear.of(2025) = "2025/26"
            assertThat(taxYearLabel.getText()).contains("2025");
        }
    }
}
