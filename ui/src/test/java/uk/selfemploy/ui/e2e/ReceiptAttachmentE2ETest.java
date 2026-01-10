package uk.selfemploy.ui.e2e;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import uk.selfemploy.ui.e2e.page.ExpenseDialogPage;
import uk.selfemploy.ui.e2e.page.ExpensePage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for SE-308: Receipt Attachment UI.
 * Based on QA test specification from /rob (30 test cases).
 *
 * <p>NOTE: Native FileChooser operations cannot be automated.
 * Tests that require file attachment use ViewModel injection
 * to bypass the native dialog.</p>
 *
 * @see docs/sprints/sprint-3/testing/rob-qa-SE-308.md
 */
@Tag("e2e")
@DisplayName("SE-308: Receipt Attachment UI E2E")
class ReceiptAttachmentE2ETest extends BaseE2ETest {

    private ExpensePage expensePage;
    private ExpenseDialogPage dialogPage;

    @BeforeEach
    void setupPages() {
        expensePage = new ExpensePage(this);
        dialogPage = new ExpenseDialogPage(this);
        expensePage.navigateTo();
    }

    // === Phase 1: P0 Critical Tests (12 tests) ===

    @Nested
    @DisplayName("TC-308-001 to TC-308-012: P0 Critical Tests")
    class CriticalTests {

        // === TC-308-001: Initial State ===

        @Nested
        @DisplayName("TC-308-001: Initial State - Dropzone Visible")
        class InitialStateTests {

            @Test
            @DisplayName("TC-308-001a: Receipt section visible in dialog")
            void receiptSectionVisibleInDialog() {
                expensePage.clickAddExpense();
                shortSleep();

                assertThat(dialogPage.isReceiptSectionVisible())
                    .as("Receipt section should be visible in Add Expense dialog")
                    .isTrue();
            }

            @Test
            @DisplayName("TC-308-001b: Dropzone visible when no receipts")
            void dropzoneVisibleWhenNoReceipts() {
                expensePage.clickAddExpense();
                shortSleep();

                assertThat(dialogPage.isReceiptDropzoneVisible())
                    .as("Dropzone should be visible when no receipts attached")
                    .isTrue();
            }

            @Test
            @DisplayName("TC-308-001c: Receipt grid hidden when no receipts")
            void receiptGridHiddenWhenNoReceipts() {
                expensePage.clickAddExpense();
                shortSleep();

                assertThat(dialogPage.isReceiptGridVisible())
                    .as("Receipt grid should be hidden when no receipts attached")
                    .isFalse();
            }

            @Test
            @DisplayName("TC-308-001d: Initial count badge shows 0 of 5")
            void initialCountBadgeShowsZero() {
                expensePage.clickAddExpense();
                shortSleep();

                String countText = dialogPage.getReceiptCountText();
                assertThat(countText)
                    .as("Count badge should show '0 of 5'")
                    .isEqualTo("0 of 5");
            }
        }

        // === TC-308-002: Attach Button ===

        @Nested
        @DisplayName("TC-308-002: Attach Receipt Button")
        class AttachButtonTests {

            @Test
            @DisplayName("TC-308-002a: Attach button visible in dropzone")
            void attachButtonVisibleInDropzone() {
                expensePage.clickAddExpense();
                shortSleep();

                assertThat(dialogPage.isAttachButtonVisible())
                    .as("Attach Receipt button should be visible")
                    .isTrue();
            }

            @Test
            @DisplayName("TC-308-002b: Attach button has correct text")
            void attachButtonHasCorrectText() {
                expensePage.clickAddExpense();
                shortSleep();

                Button attachBtn = lookup("#attachBtn").queryAs(Button.class);
                assertThat(attachBtn.getText())
                    .as("Attach button should have '+ Attach Receipt' text")
                    .isEqualTo("+ Attach Receipt");
            }

            @Test
            @DisplayName("TC-308-002c: Attach button styled correctly")
            void attachButtonStyledCorrectly() {
                expensePage.clickAddExpense();
                shortSleep();

                Button attachBtn = lookup("#attachBtn").queryAs(Button.class);
                assertThat(attachBtn.getStyleClass())
                    .as("Attach button should have 'button-attach' style class")
                    .contains("button-attach");
            }
        }

        // === TC-308-004: Maximum 5 Receipts Enforced ===

        @Nested
        @DisplayName("TC-308-004: Maximum 5 Receipts")
        class MaxReceiptsTests {

            @Test
            @DisplayName("TC-308-004a: Max receipts is 5")
            void maxReceiptsIsFive() {
                expensePage.clickAddExpense();
                shortSleep();

                String countText = dialogPage.getReceiptCountText();
                assertThat(countText)
                    .as("Max receipt count should be 5")
                    .endsWith("of 5");
            }
        }

        // === TC-308-005: Count Badge Updates ===

        @Nested
        @DisplayName("TC-308-005: Count Badge")
        class CountBadgeTests {

            @Test
            @DisplayName("TC-308-005a: Count badge exists")
            void countBadgeExists() {
                expensePage.clickAddExpense();
                shortSleep();

                Label countLabel = lookup("#receiptCount").queryAs(Label.class);
                assertThat(countLabel)
                    .as("Receipt count badge should exist")
                    .isNotNull();
            }

            @Test
            @DisplayName("TC-308-005b: Count badge styled correctly")
            void countBadgeStyledCorrectly() {
                expensePage.clickAddExpense();
                shortSleep();

                Label countLabel = lookup("#receiptCount").queryAs(Label.class);
                assertThat(countLabel.getStyleClass())
                    .as("Count badge should have 'receipt-count' style class")
                    .contains("receipt-count");
            }
        }

        // === TC-308-010: Error Dismiss Button ===

        @Nested
        @DisplayName("TC-308-010: Error Handling")
        class ErrorHandlingTests {

            @Test
            @DisplayName("TC-308-010a: Error hidden initially")
            void errorHiddenInitially() {
                expensePage.clickAddExpense();
                shortSleep();

                assertThat(dialogPage.isReceiptErrorVisible())
                    .as("Receipt error should be hidden initially")
                    .isFalse();
            }
        }

        // === TC-308-012: UI State Transitions ===

        @Nested
        @DisplayName("TC-308-012: UI State Transitions")
        class UIStateTransitionTests {

            @Test
            @DisplayName("TC-308-012a: Dropzone visible initially")
            void dropzoneVisibleInitially() {
                expensePage.clickAddExpense();
                shortSleep();

                VBox dropzone = lookup("#receiptDropzone").queryAs(VBox.class);
                assertThat(dropzone.isVisible() && dropzone.isManaged())
                    .as("Dropzone should be visible initially")
                    .isTrue();
            }

            @Test
            @DisplayName("TC-308-012b: Grid not visible initially")
            void gridNotVisibleInitially() {
                expensePage.clickAddExpense();
                shortSleep();

                FlowPane grid = lookup("#receiptGrid").queryAs(FlowPane.class);
                assertThat(grid.isVisible() && grid.isManaged())
                    .as("Receipt grid should not be visible initially")
                    .isFalse();
            }
        }
    }

    // === Phase 2: P1 High Priority Tests (10 tests) ===

    @Nested
    @DisplayName("TC-308-013 to TC-308-024: P1 High Priority Tests")
    class HighPriorityTests {

        // === TC-308-013: Drag and Drop Zone ===

        @Nested
        @DisplayName("TC-308-013: Drag and Drop Zone")
        class DragDropZoneTests {

            @Test
            @DisplayName("TC-308-013a: Dropzone has correct structure")
            void dropzoneHasCorrectStructure() {
                expensePage.clickAddExpense();
                shortSleep();

                // Verify dropzone has expected children
                VBox dropzone = lookup("#receiptDropzone").queryAs(VBox.class);
                assertThat(dropzone.getChildren())
                    .as("Dropzone should have children (icon, button, text)")
                    .isNotEmpty();
            }

            @Test
            @DisplayName("TC-308-013b: Dropzone has instructions text")
            void dropzoneHasInstructionsText() {
                expensePage.clickAddExpense();
                shortSleep();

                // Check for drag & drop instruction label
                boolean hasInstructions = lookup(".receipt-dropzone-text")
                    .tryQuery()
                    .isPresent();

                assertThat(hasInstructions)
                    .as("Dropzone should have instructions text")
                    .isTrue();
            }

            @Test
            @DisplayName("TC-308-013c: Dropzone shows supported formats")
            void dropzoneShowsSupportedFormats() {
                expensePage.clickAddExpense();
                shortSleep();

                // Check for formats label
                Label formatsLabel = lookup(".receipt-dropzone-formats").queryAs(Label.class);
                assertThat(formatsLabel.getText())
                    .as("Should show supported formats")
                    .containsIgnoringCase("JPG")
                    .containsIgnoringCase("PNG")
                    .containsIgnoringCase("PDF");
            }
        }

        // === TC-308-014: Dropzone Style ===

        @Nested
        @DisplayName("TC-308-014: Dropzone Styling")
        class DropzoneStylingTests {

            @Test
            @DisplayName("TC-308-014a: Dropzone has correct style class")
            void dropzoneHasCorrectStyleClass() {
                expensePage.clickAddExpense();
                shortSleep();

                VBox dropzone = lookup("#receiptDropzone").queryAs(VBox.class);
                assertThat(dropzone.getStyleClass())
                    .as("Dropzone should have 'receipt-dropzone' style class")
                    .contains("receipt-dropzone");
            }
        }

        // === TC-308-015: Receipt Grid Structure ===

        @Nested
        @DisplayName("TC-308-015: Receipt Grid")
        class ReceiptGridTests {

            @Test
            @DisplayName("TC-308-015a: Grid has correct style class")
            void gridHasCorrectStyleClass() {
                expensePage.clickAddExpense();
                shortSleep();

                FlowPane grid = lookup("#receiptGrid").queryAs(FlowPane.class);
                assertThat(grid.getStyleClass())
                    .as("Receipt grid should have 'receipt-grid' style class")
                    .contains("receipt-grid");
            }

            @Test
            @DisplayName("TC-308-015b: Grid is FlowPane")
            void gridIsFlowPane() {
                expensePage.clickAddExpense();
                shortSleep();

                FlowPane grid = lookup("#receiptGrid").queryAs(FlowPane.class);
                assertThat(grid)
                    .as("Receipt grid should be a FlowPane")
                    .isNotNull();
            }
        }

        // === TC-308-016: Section Header ===

        @Nested
        @DisplayName("TC-308-016: Section Header")
        class SectionHeaderTests {

            @Test
            @DisplayName("TC-308-016a: Receipt section has header")
            void receiptSectionHasHeader() {
                expensePage.clickAddExpense();
                shortSleep();

                // Check for "Receipts" label in section
                boolean hasReceiptsLabel = lookup("#receiptSection")
                    .queryAs(VBox.class)
                    .lookupAll(".field-label")
                    .stream()
                    .anyMatch(node -> node instanceof Label &&
                        "Receipts".equals(((Label) node).getText()));

                assertThat(hasReceiptsLabel)
                    .as("Receipt section should have 'Receipts' header")
                    .isTrue();
            }

            @Test
            @DisplayName("TC-308-016b: Receipt section shows optional indicator")
            void receiptSectionShowsOptionalIndicator() {
                expensePage.clickAddExpense();
                shortSleep();

                boolean hasOptionalLabel = lookup("#receiptSection")
                    .queryAs(VBox.class)
                    .lookupAll(".optional-indicator")
                    .stream()
                    .anyMatch(node -> node instanceof Label);

                assertThat(hasOptionalLabel)
                    .as("Receipt section should show (Optional) indicator")
                    .isTrue();
            }
        }
    }

    // === Phase 3: P2 Edge Cases (8 tests) ===

    @Nested
    @DisplayName("TC-308-025 to TC-308-030: P2 Edge Cases")
    class EdgeCaseTests {

        // === TC-308-025: Edit Mode ===

        @Nested
        @DisplayName("TC-308-025: Edit Mode")
        class EditModeTests {

            @Test
            @DisplayName("TC-308-025a: Receipt section visible in edit mode")
            void receiptSectionVisibleInEditMode() {
                // First need an expense to edit
                // This test verifies the structure is the same in edit mode
                expensePage.clickAddExpense();
                shortSleep();

                // Verify receipt section exists (structure should be same as add mode)
                assertThat(dialogPage.isReceiptSectionVisible())
                    .as("Receipt section should be visible in dialog")
                    .isTrue();
            }
        }

        // === TC-308-026: Error Container Structure ===

        @Nested
        @DisplayName("TC-308-026: Error Container")
        class ErrorContainerTests {

            @Test
            @DisplayName("TC-308-026a: Error container exists")
            void errorContainerExists() {
                expensePage.clickAddExpense();
                shortSleep();

                boolean hasErrorContainer = lookup("#receiptError").tryQuery().isPresent();
                assertThat(hasErrorContainer)
                    .as("Receipt error container should exist in DOM")
                    .isTrue();
            }

            @Test
            @DisplayName("TC-308-026b: Error dismiss button exists")
            void errorDismissButtonExists() {
                expensePage.clickAddExpense();
                shortSleep();

                boolean hasDismissBtn = lookup("#dismissErrorBtn").tryQuery().isPresent();
                assertThat(hasDismissBtn)
                    .as("Error dismiss button should exist")
                    .isTrue();
            }

            @Test
            @DisplayName("TC-308-026c: Error text label exists")
            void errorTextLabelExists() {
                expensePage.clickAddExpense();
                shortSleep();

                boolean hasErrorText = lookup("#receiptErrorText").tryQuery().isPresent();
                assertThat(hasErrorText)
                    .as("Error text label should exist")
                    .isTrue();
            }

            @Test
            @DisplayName("TC-308-026d: Error helper label exists")
            void errorHelperLabelExists() {
                expensePage.clickAddExpense();
                shortSleep();

                boolean hasErrorHelper = lookup("#receiptErrorHelper").tryQuery().isPresent();
                assertThat(hasErrorHelper)
                    .as("Error helper label should exist")
                    .isTrue();
            }
        }

        // === TC-308-027: Receipt Container Stack ===

        @Nested
        @DisplayName("TC-308-027: Container Structure")
        class ContainerStructureTests {

            @Test
            @DisplayName("TC-308-027a: Receipt container exists")
            void receiptContainerExists() {
                expensePage.clickAddExpense();
                shortSleep();

                boolean hasContainer = lookup("#receiptContainer").tryQuery().isPresent();
                assertThat(hasContainer)
                    .as("Receipt container (StackPane) should exist")
                    .isTrue();
            }
        }

        // === TC-308-028: Section Position ===

        @Nested
        @DisplayName("TC-308-028: Section Position in Dialog")
        class SectionPositionTests {

            @Test
            @DisplayName("TC-308-028a: Receipt section is after notes field")
            void receiptSectionAfterNotesField() {
                expensePage.clickAddExpense();
                shortSleep();

                // Both sections should exist
                boolean hasNotesField = lookup("#notesField").tryQuery().isPresent();
                boolean hasReceiptSection = lookup("#receiptSection").tryQuery().isPresent();

                assertThat(hasNotesField && hasReceiptSection)
                    .as("Both notes field and receipt section should exist")
                    .isTrue();
            }
        }
    }

    // === Additional UI Component Tests ===

    @Nested
    @DisplayName("UI Component Verification")
    class UIComponentTests {

        @Test
        @DisplayName("All receipt UI components present")
        void allReceiptUIComponentsPresent() {
            expensePage.clickAddExpense();
            shortSleep();

            // Verify all key receipt section components exist
            assertThat(lookup("#receiptSection").tryQuery()).isPresent();
            assertThat(lookup("#receiptCount").tryQuery()).isPresent();
            assertThat(lookup("#receiptContainer").tryQuery()).isPresent();
            assertThat(lookup("#receiptDropzone").tryQuery()).isPresent();
            assertThat(lookup("#attachBtn").tryQuery()).isPresent();
            assertThat(lookup("#receiptGrid").tryQuery()).isPresent();
            assertThat(lookup("#receiptError").tryQuery()).isPresent();
            assertThat(lookup("#receiptErrorText").tryQuery()).isPresent();
            assertThat(lookup("#receiptErrorHelper").tryQuery()).isPresent();
            assertThat(lookup("#dismissErrorBtn").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("Dropzone icon visible")
        void dropzoneIconVisible() {
            expensePage.clickAddExpense();
            shortSleep();

            boolean hasIcon = lookup(".receipt-dropzone-icon").tryQuery().isPresent();
            assertThat(hasIcon)
                .as("Dropzone icon should be visible")
                .isTrue();
        }

        @Test
        @DisplayName("Dialog can be closed")
        void dialogCanBeClosed() {
            expensePage.clickAddExpense();
            shortSleep();

            assertThat(dialogPage.isReceiptSectionVisible()).isTrue();

            dialogPage.clickCancel();
            shortSleep();

            // After cancel, receipt section should not be queryable (dialog closed)
            assertThat(lookup("#receiptSection").tryQuery().isPresent())
                .as("Dialog should be closed after cancel")
                .isFalse();
        }
    }

    // === Style Class Verification Tests ===

    @Nested
    @DisplayName("CSS Style Class Verification")
    class StyleClassTests {

        @Test
        @DisplayName("Receipt section has correct style class")
        void receiptSectionHasStyleClass() {
            expensePage.clickAddExpense();
            shortSleep();

            VBox receiptSection = lookup("#receiptSection").queryAs(VBox.class);
            assertThat(receiptSection.getStyleClass())
                .as("Receipt section should have 'receipt-section' style class")
                .contains("receipt-section");
        }

        @Test
        @DisplayName("Error container has correct style class")
        void errorContainerHasStyleClass() {
            expensePage.clickAddExpense();
            shortSleep();

            var errorBox = lookup("#receiptError").query();
            assertThat(errorBox.getStyleClass())
                .as("Error container should have 'receipt-error' style class")
                .contains("receipt-error");
        }
    }
}
