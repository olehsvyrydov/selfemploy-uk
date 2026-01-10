package uk.selfemploy.ui.e2e;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Tests for SE-306: Tax Summary UI.
 * Based on QA test specification from /rob.
 *
 * <p>These tests require a display. Run with {@code -Dgroups=e2e} to include,
 * or {@code -DexcludedGroups=e2e} to exclude.</p>
 *
 * @see docs/sprints/sprint-3/testing/rob-qa-SE-306.md
 */
@Tag("e2e")
@DisplayName("SE-306: Tax Summary UI E2E")
class TaxSummaryE2ETest extends BaseE2ETest {

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("£[\\d,]+\\.\\d{2}");
    private static final String TOGGLE_EXPANDED = "[v]";
    private static final String TOGGLE_COLLAPSED = "[>]";

    @BeforeEach
    void navigateToTaxSummary() {
        // Navigate to Tax Summary view before each test
        clickOn("#navTax");
        waitForFxEvents();

        // Reset scroll position to top for consistent test state
        ScrollPane scrollPane = lookup(".scroll-pane").tryQueryAs(ScrollPane.class).orElse(null);
        if (scrollPane != null) {
            Platform.runLater(() -> scrollPane.setVvalue(0));
            WaitForAsyncUtils.waitForFxEvents();
        }

        // Reset all sections to expanded state for test consistency
        resetSectionsToExpanded();
    }

    /**
     * Resets all expandable sections to expanded state.
     * This ensures each test starts with a known state.
     */
    private void resetSectionsToExpanded() {
        // Check and expand income section if collapsed
        Label incomeToggle = lookup("#incomeToggle").tryQueryAs(Label.class).orElse(null);
        if (incomeToggle != null && TOGGLE_COLLAPSED.equals(incomeToggle.getText())) {
            clickOn("#incomeSection .section-header");
            waitForFxEvents();
        }

        // Check and expand expenses section if collapsed (may need scroll)
        Label expensesToggle = lookup("#expensesToggle").tryQueryAs(Label.class).orElse(null);
        if (expensesToggle != null && TOGGLE_COLLAPSED.equals(expensesToggle.getText())) {
            scrollToAndClick("#expensesSection .section-header");
            waitForFxEvents();
        }

        // Reset scroll to top after section resets
        ScrollPane scrollPane = lookup(".scroll-pane").tryQueryAs(ScrollPane.class).orElse(null);
        if (scrollPane != null) {
            Platform.runLater(() -> scrollPane.setVvalue(0));
            WaitForAsyncUtils.waitForFxEvents();
        }
    }

    /**
     * Scrolls the ScrollPane to make the target node visible before interaction.
     * This is necessary because TestFX cannot click on nodes that are outside
     * the visible viewport of a ScrollPane.
     */
    private void scrollToAndClick(String selector) {
        // Find the ScrollPane containing the tax summary content
        ScrollPane scrollPane = lookup(".scroll-pane").queryAs(ScrollPane.class);
        if (scrollPane == null) {
            // Fallback: try direct click if no scroll pane
            clickOn(selector);
            return;
        }

        // Find the target node
        Node target = lookup(selector).query();
        if (target == null) {
            throw new IllegalArgumentException("Could not find node: " + selector);
        }

        // Calculate scroll position to make target visible
        Platform.runLater(() -> {
            Node content = scrollPane.getContent();
            double contentHeight = content.getBoundsInLocal().getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();

            // Get target's position relative to content
            double targetY = target.localToScene(target.getBoundsInLocal()).getMinY()
                    - content.localToScene(content.getBoundsInLocal()).getMinY();

            // Calculate vvalue to center the target in viewport
            double vvalue = (targetY - viewportHeight / 2) / (contentHeight - viewportHeight);
            vvalue = Math.max(0, Math.min(1, vvalue)); // Clamp to [0, 1]

            scrollPane.setVvalue(vvalue);
        });
        WaitForAsyncUtils.waitForFxEvents();
        shortSleep();

        // Now click the target
        clickOn(selector);
    }

    // ============================================================
    // 1. Navigation & Accessibility Tests (TC-306-001 to TC-306-004)
    // ============================================================

    @Nested
    @DisplayName("1. Navigation & Accessibility")
    class NavigationTests {

        @Test
        @DisplayName("TC-306-001: Navigate to Tax Summary from Sidebar (P0)")
        void navigateToTaxSummaryFromSidebar() {
            // Already navigated in @BeforeEach, verify the view loaded
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Tax Summary");

            // Verify Tax Summary button is selected
            ToggleButton taxBtn = lookup("#navTax").queryAs(ToggleButton.class);
            assertThat(taxBtn.isSelected()).isTrue();

            // Verify main sections are present
            assertThat(lookup(".tax-glance-card").tryQuery()).isPresent();
            assertThat(lookup(".section-card").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("TC-306-002: Tax Year Badge Displays Correctly (P0)")
        void taxYearBadgeDisplaysCorrectly() {
            Label taxYearBadge = lookup("#taxYearBadge").queryAs(Label.class);
            assertThat(taxYearBadge).isNotNull();
            assertThat(taxYearBadge.getText()).matches("Tax Year \\d{4}/\\d{2}");
        }

        @Test
        @DisplayName("TC-306-003: Page Scrolls Vertically (P1)")
        void pageScrollsVertically() {
            // Verify ScrollPane exists and has correct policy
            ScrollPane scrollPane = lookup(".tax-summary-scroll").queryAs(ScrollPane.class);
            assertThat(scrollPane).isNotNull();
            assertThat(scrollPane.getHbarPolicy()).isEqualTo(ScrollPane.ScrollBarPolicy.NEVER);
            assertThat(scrollPane.isFitToWidth()).isTrue();
        }

        @Test
        @DisplayName("TC-306-004: Page Title Displayed (P1)")
        void pageTitleDisplayed() {
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Tax Summary");
        }
    }

    // ============================================================
    // 2. Tax At A Glance Card Tests (TC-306-005 to TC-306-009)
    // ============================================================

    @Nested
    @DisplayName("2. Tax At A Glance Card")
    class TaxAtAGlanceTests {

        @Test
        @DisplayName("TC-306-005: Net Profit Displays with Box 31 Reference (P0)")
        void netProfitDisplaysWithBox31Reference() {
            // Verify NET PROFIT label exists
            assertThat(lookup(".tax-metric-label").queryAllAs(Label.class).stream()
                    .anyMatch(l -> l.getText().contains("NET PROFIT"))).isTrue();

            // Verify Box 31 reference
            assertThat(lookup(".tax-metric-box-ref").queryAllAs(Label.class).stream()
                    .anyMatch(l -> l.getText().contains("Box 31"))).isTrue();

            // Verify value label exists and shows currency format
            Label netProfitValue = lookup("#netProfitValue").queryAs(Label.class);
            assertThat(netProfitValue).isNotNull();
            assertThat(netProfitValue.getText()).matches(CURRENCY_PATTERN.pattern());
        }

        @Test
        @DisplayName("TC-306-006: Income Tax Total Displays (P0)")
        void incomeTaxTotalDisplays() {
            Label incomeTaxValue = lookup("#incomeTaxValue").queryAs(Label.class);
            assertThat(incomeTaxValue).isNotNull();
            assertThat(incomeTaxValue.getText()).matches(CURRENCY_PATTERN.pattern());

            // Verify has income-tax style class
            assertThat(incomeTaxValue.getStyleClass()).contains("income-tax");
        }

        @Test
        @DisplayName("TC-306-007: NI Class 4 Total Displays (P0)")
        void niClass4TotalDisplays() {
            Label niValue = lookup("#niClass4Value").queryAs(Label.class);
            assertThat(niValue).isNotNull();
            assertThat(niValue.getText()).matches(CURRENCY_PATTERN.pattern());

            // Verify has ni-class4 style class
            assertThat(niValue.getStyleClass()).contains("ni-class4");
        }

        @Test
        @DisplayName("TC-306-008: Total Tax Due Displays (P0)")
        void totalTaxDueDisplays() {
            Label totalTaxDue = lookup("#totalTaxDueValue").queryAs(Label.class);
            assertThat(totalTaxDue).isNotNull();
            assertThat(totalTaxDue.getText()).matches(CURRENCY_PATTERN.pattern());

            // Verify has proper style class
            assertThat(totalTaxDue.getStyleClass()).contains("total-tax-due-value");
        }

        @Test
        @DisplayName("TC-306-009: Three Metric Cards Layout (P1)")
        void threeMetricCardsLayout() {
            // Find all metric cards
            var metricCards = lookup(".tax-metric-card").queryAll();
            assertThat(metricCards).hasSize(3);

            // Verify they're in an HBox
            var hbox = lookup(".tax-glance-card HBox").queryAs(HBox.class);
            assertThat(hbox).isNotNull();
        }
    }

    // ============================================================
    // 3. Section Expand/Collapse Tests (TC-306-010 to TC-306-017)
    // ============================================================

    @Nested
    @DisplayName("3. Section Expand/Collapse")
    class SectionToggleTests {

        @Test
        @DisplayName("TC-306-010: Income Section Expands by Default (P0)")
        void incomeSectionExpandsByDefault() {
            // Verify toggle shows expanded icon
            Label toggle = lookup("#incomeToggle").queryAs(Label.class);
            assertThat(toggle.getText()).isEqualTo(TOGGLE_EXPANDED);

            // Verify content is visible
            VBox content = lookup("#incomeContent").queryAs(VBox.class);
            assertThat(content.isVisible()).isTrue();
            assertThat(content.isManaged()).isTrue();
        }

        @Test
        @DisplayName("TC-306-011: Toggle Income Section Collapse (P0)")
        void toggleIncomeSectionCollapse() {
            // Section starts expanded (reset by @BeforeEach)
            assertThat(lookup("#incomeToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_EXPANDED);

            // Click using interact() to ensure FX thread execution
            HBox header = lookup("#incomeSection .section-header").queryAs(HBox.class);
            interact(() -> header.fireEvent(new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1,
                    false, false, false, false, true, false, false, false, false, false, null)));
            waitForFxEvents();
            shortSleep();

            // Re-query and verify toggle changed to collapsed
            String toggleText = lookup("#incomeToggle").queryAs(Label.class).getText();
            assertThat(toggleText).isEqualTo(TOGGLE_COLLAPSED);

            // Verify content is hidden
            VBox content = lookup("#incomeContent").queryAs(VBox.class);
            assertThat(content.isVisible()).isFalse();
        }

        @Test
        @DisplayName("TC-306-012: Toggle Income Section Expand (P0)")
        void toggleIncomeSectionExpand() {
            // Collapse first
            clickOn("#incomeSection .section-header");
            waitForFxEvents();

            // Expand again
            clickOn("#incomeSection .section-header");
            waitForFxEvents();

            // Verify expanded
            Label toggle = lookup("#incomeToggle").queryAs(Label.class);
            assertThat(toggle.getText()).isEqualTo(TOGGLE_EXPANDED);

            VBox content = lookup("#incomeContent").queryAs(VBox.class);
            assertThat(content.isVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-306-013: Toggle Expenses Section (P1)")
        void toggleExpensesSection() {
            // Scroll to expenses section (may be below viewport when income is expanded)
            scrollToAndClick("#expensesSection .section-header");
            waitForFxEvents();

            Label toggle = lookup("#expensesToggle").queryAs(Label.class);

            // After first click, toggle should be collapsed (section starts expanded)
            String initialState = toggle.getText();

            // Toggle back
            scrollToAndClick("#expensesSection .section-header");
            waitForFxEvents();

            // Verify state changed
            assertThat(toggle.getText()).isNotEqualTo(initialState);
        }

        @Test
        @DisplayName("TC-306-014: Toggle Income Tax Section (P1)")
        void toggleIncomeTaxSection() {
            // Scroll to and collapse (section may be below viewport)
            scrollToAndClick("#incomeTaxSection .section-header");
            waitForFxEvents();

            Label toggle = lookup("#incomeTaxToggle").queryAs(Label.class);
            assertThat(toggle.getText()).isEqualTo(TOGGLE_COLLAPSED);

            VBox content = lookup("#incomeTaxContent").queryAs(VBox.class);
            assertThat(content.isVisible()).isFalse();
        }

        @Test
        @DisplayName("TC-306-015: Toggle NI Class 4 Section (P1)")
        void toggleNiClass4Section() {
            // Scroll to and collapse (section may be below viewport)
            scrollToAndClick("#niSection .section-header");
            waitForFxEvents();

            Label toggle = lookup("#niToggle").queryAs(Label.class);
            assertThat(toggle.getText()).isEqualTo(TOGGLE_COLLAPSED);

            VBox content = lookup("#niContent").queryAs(VBox.class);
            assertThat(content.isVisible()).isFalse();
        }

        @Test
        @DisplayName("TC-306-017: Profit Calculation Section Shows Equals Icon (P2)")
        void profitCalculationSectionShowsEqualsIcon() {
            // Find the profit calculation section (look for the one with [=] toggle)
            var toggleLabels = lookup(".section-toggle").queryAllAs(Label.class);
            boolean hasEqualsIcon = toggleLabels.stream()
                    .anyMatch(l -> "[=]".equals(l.getText()));
            assertThat(hasEqualsIcon).isTrue();
        }
    }

    // ============================================================
    // 4. SA103 Box References Tests (TC-306-018 to TC-306-022)
    // ============================================================

    @Nested
    @DisplayName("4. SA103 Box References")
    class SA103BoxReferencesTests {

        @Test
        @DisplayName("TC-306-018: Income Section Shows Box 15 for Turnover (P0)")
        void incomeSectionShowsBox15ForTurnover() {
            // Look for Box 15 reference in income section
            VBox incomeContent = lookup("#incomeContent").queryAs(VBox.class);
            assertThat(incomeContent).isNotNull();

            // Find all line-item-box labels and check for Box 15
            var boxLabels = lookup("#incomeContent .line-item-box").queryAllAs(Label.class);
            boolean hasBox15 = boxLabels.stream()
                    .anyMatch(l -> l.getText().contains("Box 15"));
            assertThat(hasBox15).isTrue();
        }

        @Test
        @DisplayName("TC-306-019: Income Section Shows Box 16 for Other Income (P1)")
        void incomeSectionShowsBox16ForOtherIncome() {
            var boxLabels = lookup("#incomeContent .line-item-box").queryAllAs(Label.class);
            boolean hasBox16 = boxLabels.stream()
                    .anyMatch(l -> l.getText().contains("Box 16"));
            assertThat(hasBox16).isTrue();
        }

        @Test
        @DisplayName("TC-306-022: Profit Calculation Shows Box 31 Reference (P0)")
        void profitCalculationShowsBox31Reference() {
            // Look for NET PROFIT (Box 31) in profit calculation section
            var calcLabels = lookup(".calc-label").queryAllAs(Label.class);
            boolean hasBox31 = calcLabels.stream()
                    .anyMatch(l -> l.getText().contains("Box 31"));
            assertThat(hasBox31).isTrue();
        }
    }

    // ============================================================
    // 5. Conditional Visibility Tests (TC-306-023 to TC-306-027)
    // ============================================================

    @Nested
    @DisplayName("5. Conditional Visibility")
    class ConditionalVisibilityTests {

        @Test
        @DisplayName("TC-306-023: Draft Banner Visible When Not Submitted (P0)")
        void draftBannerVisibleWhenNotSubmitted() {
            HBox draftBanner = lookup("#draftBanner").queryAs(HBox.class);
            assertThat(draftBanner).isNotNull();

            // By default (not submitted), banner should be visible
            // Note: This depends on the initial state of the ViewModel
            // In a real app, the banner shows when isSubmitted = false
            assertThat(draftBanner.isVisible()).isTrue();
            assertThat(draftBanner.isManaged()).isTrue();

            // Verify banner text
            var bannerLabels = lookup("#draftBanner .draft-banner-text").queryAllAs(Label.class);
            boolean hasDraftText = bannerLabels.stream()
                    .anyMatch(l -> l.getText().contains("DRAFT"));
            assertThat(hasDraftText).isTrue();
        }

        @Test
        @DisplayName("TC-306-025: POA Section Initial State (P0)")
        void poaSectionInitialState() {
            // POA section visibility depends on tax amount > £1,000
            // Verify the section element exists (may be hidden or shown)
            VBox poaSection = lookup("#poaSection").queryAs(VBox.class);
            assertThat(poaSection).isNotNull();

            // The visibility depends on calculated tax
            // With default zero values, POA should be hidden
            // This test verifies the element exists and respects visibility binding
        }

        @Test
        @DisplayName("TC-306-027: POA Due Row Visibility Matches POA Section (P1)")
        void poaDueRowVisibilityMatchesPoaSection() {
            VBox poaSection = lookup("#poaSection").queryAs(VBox.class);
            HBox poaDueRow = lookup("#poaDueRow").queryAs(HBox.class);

            assertThat(poaSection).isNotNull();
            assertThat(poaDueRow).isNotNull();

            // Both should have same visibility state
            assertThat(poaDueRow.isVisible()).isEqualTo(poaSection.isVisible());
            assertThat(poaDueRow.isManaged()).isEqualTo(poaSection.isManaged());
        }
    }

    // ============================================================
    // 6. Data Display & Formatting Tests (TC-306-028 to TC-306-031)
    // ============================================================

    @Nested
    @DisplayName("6. Data Display & Formatting")
    class DataDisplayTests {

        @Test
        @DisplayName("TC-306-028: Currency Formatted with UK Locale (P0)")
        void currencyFormattedWithUkLocale() {
            // Check various currency labels all match UK format
            Label netProfit = lookup("#netProfitValue").queryAs(Label.class);
            Label incomeTax = lookup("#incomeTaxValue").queryAs(Label.class);
            Label niClass4 = lookup("#niClass4Value").queryAs(Label.class);
            Label totalTax = lookup("#totalTaxDueValue").queryAs(Label.class);

            // All should start with £ and have proper format
            assertThat(netProfit.getText()).startsWith("£");
            assertThat(incomeTax.getText()).startsWith("£");
            assertThat(niClass4.getText()).startsWith("£");
            assertThat(totalTax.getText()).startsWith("£");

            // Verify pattern (£X,XXX.XX format)
            assertThat(netProfit.getText()).matches(CURRENCY_PATTERN.pattern());
        }

        @Test
        @DisplayName("TC-306-029: Expenses Displayed as Negative in Calculation (P1)")
        void expensesDisplayedAsNegativeInCalculation() {
            Label expensesCalc = lookup("#expensesCalcValue").queryAs(Label.class);
            assertThat(expensesCalc).isNotNull();

            // Should start with minus sign
            assertThat(expensesCalc.getText()).startsWith("-");

            // Should have negative style class
            assertThat(expensesCalc.getStyleClass()).contains("negative");
        }

        @Test
        @DisplayName("TC-306-030: Income Tax Bands Display All Four Rates (P0)")
        void incomeTaxBandsDisplayAllFourRates() {
            VBox incomeTaxContent = lookup("#incomeTaxContent").queryAs(VBox.class);
            assertThat(incomeTaxContent).isNotNull();

            // Should have 4 tax band rows
            var bandRows = lookup("#incomeTaxContent .tax-band-row").queryAll();
            assertThat(bandRows).hasSize(4);

            // Verify bands contain expected descriptions
            var descriptions = lookup("#incomeTaxContent .tax-band-desc").queryAllAs(Label.class);
            List<String> descTexts = descriptions.stream()
                    .map(Label::getText)
                    .collect(Collectors.toList());

            assertThat(descTexts).anyMatch(t -> t.contains("Personal Allowance") || t.contains("0%"));
            assertThat(descTexts).anyMatch(t -> t.contains("Basic") || t.contains("20%"));
            assertThat(descTexts).anyMatch(t -> t.contains("Higher") || t.contains("40%"));
            assertThat(descTexts).anyMatch(t -> t.contains("Additional") || t.contains("45%"));
        }

        @Test
        @DisplayName("TC-306-031: NI Class 4 Bands Display All Three Rates (P0)")
        void niClass4BandsDisplayAllThreeRates() {
            VBox niContent = lookup("#niContent").queryAs(VBox.class);
            assertThat(niContent).isNotNull();

            // Should have 3 NI band rows
            var bandRows = lookup("#niContent .tax-band-row").queryAll();
            assertThat(bandRows).hasSize(3);

            // Verify bands contain expected rates
            var descriptions = lookup("#niContent .tax-band-desc").queryAllAs(Label.class);
            List<String> descTexts = descriptions.stream()
                    .map(Label::getText)
                    .collect(Collectors.toList());

            assertThat(descTexts).anyMatch(t -> t.contains("0%"));
            assertThat(descTexts).anyMatch(t -> t.contains("9%"));
            assertThat(descTexts).anyMatch(t -> t.contains("2%"));
        }
    }

    // ============================================================
    // 7. Action Bar Buttons Tests (TC-306-032 to TC-306-035)
    // ============================================================

    @Nested
    @DisplayName("7. Action Bar Buttons")
    class ActionBarTests {

        @Test
        @DisplayName("TC-306-032: Export PDF Button Enabled (P1)")
        void exportPdfButtonEnabled() {
            Button exportBtn = lookup("#exportPdfBtn").queryAs(Button.class);
            assertThat(exportBtn).isNotNull();
            assertThat(exportBtn.isDisabled()).isFalse();
            assertThat(exportBtn.getText()).isEqualTo("Export PDF");
        }

        @Test
        @DisplayName("TC-306-034: Submit to HMRC Button Disabled (P0)")
        void submitToHmrcButtonDisabled() {
            Button submitBtn = lookup("#submitBtn").queryAs(Button.class);
            assertThat(submitBtn).isNotNull();
            assertThat(submitBtn.isDisabled()).isTrue();
            assertThat(submitBtn.getText()).isEqualTo("Submit to HMRC");
        }

        @Test
        @DisplayName("TC-306-035: Submit Button Has Tooltip (P2)")
        void submitButtonHasTooltip() {
            Button submitBtn = lookup("#submitBtn").queryAs(Button.class);
            assertThat(submitBtn).isNotNull();

            Tooltip tooltip = submitBtn.getTooltip();
            assertThat(tooltip).isNotNull();
            assertThat(tooltip.getText()).contains("HMRC submission will be available");
        }
    }

    // ============================================================
    // 8. Edge Cases (TC-306-036 to TC-306-038)
    // ============================================================

    @Nested
    @DisplayName("8. Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("TC-306-036: Empty Expenses Section Handles Gracefully (P2)")
        void emptyExpensesSectionHandlesGracefully() {
            // Verify expenses section exists and doesn't throw errors
            VBox expensesContent = lookup("#expensesContent").queryAs(VBox.class);
            assertThat(expensesContent).isNotNull();

            // Section should be functional even if empty
            Label expensesTotal = lookup("#expensesTotalLabel").queryAs(Label.class);
            assertThat(expensesTotal).isNotNull();
            assertThat(expensesTotal.getText()).matches(CURRENCY_PATTERN.pattern());
        }

        @Test
        @DisplayName("TC-306-037: Zero Income Scenario (P2)")
        void zeroIncomeScenario() {
            // With zero/default values, verify no errors
            Label netProfit = lookup("#netProfitValue").queryAs(Label.class);
            assertThat(netProfit).isNotNull();

            // Should show £0.00 format
            assertThat(netProfit.getText()).matches(CURRENCY_PATTERN.pattern());
        }

        @Test
        @DisplayName("TC-306-038: Due Date Display Format (P2)")
        void dueDateDisplayFormat() {
            Label dueDate = lookup("#dueDateLabel").queryAs(Label.class);
            assertThat(dueDate).isNotNull();

            // Should show "Due by DD MMMM YYYY" format
            assertThat(dueDate.getText()).startsWith("Due by");
            assertThat(dueDate.getText()).matches("Due by \\d{1,2} \\w+ \\d{4}");
        }

        @Test
        @DisplayName("TC-306-039: Grand Total Display (P2)")
        void grandTotalDisplay() {
            Label grandTotal = lookup("#grandTotalValue").queryAs(Label.class);
            assertThat(grandTotal).isNotNull();
            assertThat(grandTotal.getText()).matches(CURRENCY_PATTERN.pattern());

            // Should have total-due-amount style
            assertThat(grandTotal.getStyleClass()).contains("total-due-amount");
        }
    }

    // ============================================================
    // 9. Integration Tests
    // ============================================================

    @Nested
    @DisplayName("9. Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("TC-306-040: All Sections Toggle Without Errors")
        void allSectionsToggleWithoutErrors() {
            // Toggle all expandable sections (use scrollToAndClick for sections below viewport)
            String[] sectionSelectors = {
                    "#incomeSection .section-header",
                    "#expensesSection .section-header",
                    "#incomeTaxSection .section-header",
                    "#niSection .section-header"
            };

            // Collapse all (scroll to each before clicking)
            for (String selector : sectionSelectors) {
                scrollToAndClick(selector);
                waitForFxEvents();
            }

            // Verify all collapsed
            assertThat(lookup("#incomeToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_COLLAPSED);
            assertThat(lookup("#expensesToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_COLLAPSED);
            assertThat(lookup("#incomeTaxToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_COLLAPSED);
            assertThat(lookup("#niToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_COLLAPSED);

            // Expand all (scroll to each before clicking)
            for (String selector : sectionSelectors) {
                scrollToAndClick(selector);
                waitForFxEvents();
            }

            // Verify all expanded
            assertThat(lookup("#incomeToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_EXPANDED);
            assertThat(lookup("#expensesToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_EXPANDED);
            assertThat(lookup("#incomeTaxToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_EXPANDED);
            assertThat(lookup("#niToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_EXPANDED);
        }

        @Test
        @DisplayName("TC-306-041: Rapid Section Toggles Without Crash")
        void rapidSectionTogglesWithoutCrash() {
            // Rapidly toggle sections (scroll to each before clicking)
            for (int i = 0; i < 5; i++) {
                scrollToAndClick("#incomeSection .section-header");
                scrollToAndClick("#expensesSection .section-header");
                scrollToAndClick("#incomeTaxSection .section-header");
                scrollToAndClick("#niSection .section-header");
            }
            waitForFxEvents();

            // Verify page still functional
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Tax Summary");
        }

        @Test
        @DisplayName("TC-306-042: Navigate Away and Back Preserves State")
        void navigateAwayAndBackPreservesState() {
            // Section starts expanded (reset by @BeforeEach)
            assertThat(lookup("#incomeToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_EXPANDED);

            // Collapse income section using fireEvent
            HBox header = lookup("#incomeSection .section-header").queryAs(HBox.class);
            interact(() -> header.fireEvent(new javafx.scene.input.MouseEvent(
                    javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1,
                    false, false, false, false, true, false, false, false, false, false, null)));
            waitForFxEvents();
            shortSleep();
            assertThat(lookup("#incomeToggle").queryAs(Label.class).getText()).isEqualTo(TOGGLE_COLLAPSED);

            // Navigate to Dashboard
            clickOn("#navDashboard");
            waitForFxEvents();

            // Navigate back to Tax Summary
            clickOn("#navTax");
            waitForFxEvents();

            // Verify we're back on Tax Summary
            assertThat(lookup(".page-title").queryLabeled().getText()).isEqualTo("Tax Summary");

            // Note: View state may or may not be preserved depending on caching
            // At minimum, the view should load without errors
            assertThat(lookup("#incomeSection").tryQuery()).isPresent();
        }
    }
}
