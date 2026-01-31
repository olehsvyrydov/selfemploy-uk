package uk.selfemploy.ui.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.QuarterStatus;
import uk.selfemploy.ui.viewmodel.QuarterViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QuarterInfoDialog.
 * Tests the dialog creation logic without requiring JavaFX runtime.
 *
 * <p>Design: /aura (Senior UI/UX Design Architect)</p>
 * <p>Implementation: /james</p>
 */
@DisplayName("QuarterInfoDialog")
class QuarterInfoDialogTest {

    private static final TaxYear TAX_YEAR_2025 = TaxYear.of(2025);

    @Nested
    @DisplayName("Header Gradient Colors")
    class HeaderGradientTests {

        @Test
        @DisplayName("should return blue gradient for DRAFT status")
        void shouldReturnBlueGradientForDraft() {
            String[] colors = QuarterInfoDialog.getHeaderGradientColors(QuarterStatus.DRAFT);
            assertEquals("#0066cc", colors[0], "Start color for DRAFT should be blue");
            assertEquals("#3385d6", colors[1], "End color for DRAFT should be lighter blue");
        }

        @Test
        @DisplayName("should return red gradient for OVERDUE status")
        void shouldReturnRedGradientForOverdue() {
            String[] colors = QuarterInfoDialog.getHeaderGradientColors(QuarterStatus.OVERDUE);
            assertEquals("#dc3545", colors[0], "Start color for OVERDUE should be red");
            assertEquals("#e4606d", colors[1], "End color for OVERDUE should be lighter red");
        }

        @Test
        @DisplayName("should return green gradient for SUBMITTED status")
        void shouldReturnGreenGradientForSubmitted() {
            String[] colors = QuarterInfoDialog.getHeaderGradientColors(QuarterStatus.SUBMITTED);
            assertEquals("#28a745", colors[0], "Start color for SUBMITTED should be green");
            assertEquals("#48c664", colors[1], "End color for SUBMITTED should be lighter green");
        }

        @Test
        @DisplayName("should return gray gradient for FUTURE status")
        void shouldReturnGrayGradientForFuture() {
            String[] colors = QuarterInfoDialog.getHeaderGradientColors(QuarterStatus.FUTURE);
            assertEquals("#6c757d", colors[0], "Start color for FUTURE should be gray");
            assertEquals("#868e96", colors[1], "End color for FUTURE should be lighter gray");
        }
    }

    @Nested
    @DisplayName("Status Icons")
    class StatusIconTests {

        @Test
        @DisplayName("should return EDIT icon for DRAFT status")
        void shouldReturnEditIconForDraft() {
            String iconName = QuarterInfoDialog.getIconNameForStatus(QuarterStatus.DRAFT);
            assertEquals("EDIT", iconName, "DRAFT should use EDIT icon");
        }

        @Test
        @DisplayName("should return EXCLAMATION_TRIANGLE icon for OVERDUE status")
        void shouldReturnExclamationTriangleIconForOverdue() {
            String iconName = QuarterInfoDialog.getIconNameForStatus(QuarterStatus.OVERDUE);
            assertEquals("EXCLAMATION_TRIANGLE", iconName, "OVERDUE should use EXCLAMATION_TRIANGLE icon");
        }

        @Test
        @DisplayName("should return CHECK_CIRCLE icon for SUBMITTED status")
        void shouldReturnCheckCircleIconForSubmitted() {
            String iconName = QuarterInfoDialog.getIconNameForStatus(QuarterStatus.SUBMITTED);
            assertEquals("CHECK_CIRCLE", iconName, "SUBMITTED should use CHECK_CIRCLE icon");
        }

        @Test
        @DisplayName("should return CLOCK icon for FUTURE status")
        void shouldReturnClockIconForFuture() {
            String iconName = QuarterInfoDialog.getIconNameForStatus(QuarterStatus.FUTURE);
            assertEquals("CLOCK", iconName, "FUTURE should use CLOCK icon");
        }
    }

    @Nested
    @DisplayName("Financial Display")
    class FinancialDisplayTests {

        @Test
        @DisplayName("should calculate net profit correctly")
        void shouldCalculateNetProfitCorrectly() {
            BigDecimal income = new BigDecimal("5000.00");
            BigDecimal expenses = new BigDecimal("1500.00");
            BigDecimal netProfit = income.subtract(expenses);

            assertEquals(new BigDecimal("3500.00"), netProfit);
            assertTrue(netProfit.compareTo(BigDecimal.ZERO) > 0, "Positive net should be profit");
        }

        @Test
        @DisplayName("should calculate net loss correctly")
        void shouldCalculateNetLossCorrectly() {
            BigDecimal income = new BigDecimal("1000.00");
            BigDecimal expenses = new BigDecimal("2500.00");
            BigDecimal netLoss = income.subtract(expenses);

            assertEquals(new BigDecimal("-1500.00"), netLoss);
            assertTrue(netLoss.compareTo(BigDecimal.ZERO) < 0, "Negative net should be loss");
        }

        @Test
        @DisplayName("should determine positive net styling")
        void shouldDeterminePositiveNetStyling() {
            BigDecimal positiveNet = new BigDecimal("1000.00");
            String style = QuarterInfoDialog.getNetProfitStyle(positiveNet);
            assertTrue(style.contains("#28a745") || style.contains("green"),
                    "Positive net should use green color");
        }

        @Test
        @DisplayName("should determine negative net styling")
        void shouldDetermineNegativeNetStyling() {
            BigDecimal negativeNet = new BigDecimal("-500.00");
            String style = QuarterInfoDialog.getNetProfitStyle(negativeNet);
            assertTrue(style.contains("#dc3545") || style.contains("red"),
                    "Negative net should use red color");
        }

        @Test
        @DisplayName("should format currency correctly")
        void shouldFormatCurrencyCorrectly() {
            BigDecimal amount = new BigDecimal("12345.67");
            String formatted = QuarterInfoDialog.formatCurrency(amount);
            assertTrue(formatted.contains("12,345.67"), "Should format with comma separator");
            assertTrue(formatted.contains("\u00A3") || formatted.startsWith("£"),
                    "Should include pound sign");
        }

        @Test
        @DisplayName("should handle null amounts")
        void shouldHandleNullAmounts() {
            String formatted = QuarterInfoDialog.formatCurrency(null);
            assertEquals("--", formatted, "Null amounts should display as dashes");
        }
    }

    @Nested
    @DisplayName("Deadline Countdown")
    class DeadlineCountdownTests {

        @Test
        @DisplayName("should show days remaining for future deadline")
        void shouldShowDaysRemainingForFutureDeadline() {
            LocalDate futureDeadline = LocalDate.now().plusDays(10);
            String countdownText = QuarterInfoDialog.getDeadlineCountdownText(futureDeadline);
            assertTrue(countdownText.contains("days remaining") || countdownText.contains("day remaining"),
                    "Future deadline should show days remaining");
        }

        @Test
        @DisplayName("should show singular day for 1 day remaining")
        void shouldShowSingularDayForOneDay() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            String countdownText = QuarterInfoDialog.getDeadlineCountdownText(tomorrow);
            assertTrue(countdownText.contains("1 day remaining"),
                    "Should use singular 'day' for 1 day remaining");
        }

        @Test
        @DisplayName("should show 'today' for deadline today")
        void shouldShowTodayForDeadlineToday() {
            LocalDate today = LocalDate.now();
            String countdownText = QuarterInfoDialog.getDeadlineCountdownText(today);
            assertTrue(countdownText.toLowerCase().contains("today") ||
                       countdownText.contains("0 days"),
                    "Deadline today should show 'today' or '0 days'");
        }

        @Test
        @DisplayName("should show days overdue for past deadline")
        void shouldShowDaysOverdueForPastDeadline() {
            LocalDate pastDeadline = LocalDate.now().minusDays(5);
            String countdownText = QuarterInfoDialog.getDeadlineCountdownText(pastDeadline);
            assertTrue(countdownText.contains("overdue") || countdownText.contains("ago"),
                    "Past deadline should show overdue status");
        }

        @Test
        @DisplayName("should show singular day for 1 day overdue")
        void shouldShowSingularDayForOneDayOverdue() {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String countdownText = QuarterInfoDialog.getDeadlineCountdownText(yesterday);
            assertTrue(countdownText.contains("1 day overdue") || countdownText.contains("1 day ago"),
                    "Should use singular 'day' for 1 day overdue");
        }
    }

    @Nested
    @DisplayName("Status Description")
    class StatusDescriptionTests {

        @Test
        @DisplayName("should return draft description")
        void shouldReturnDraftDescription() {
            String description = QuarterInfoDialog.getStatusDescription(QuarterStatus.DRAFT);
            assertNotNull(description);
            assertFalse(description.isEmpty());
            // Description should mention review or preparation
            assertTrue(description.toLowerCase().contains("review") ||
                       description.toLowerCase().contains("prepar") ||
                       description.toLowerCase().contains("ready"),
                    "DRAFT description should mention review/preparation");
        }

        @Test
        @DisplayName("should return overdue description")
        void shouldReturnOverdueDescription() {
            String description = QuarterInfoDialog.getStatusDescription(QuarterStatus.OVERDUE);
            assertNotNull(description);
            assertFalse(description.isEmpty());
            // Description should indicate urgency
            assertTrue(description.toLowerCase().contains("overdue") ||
                       description.toLowerCase().contains("past") ||
                       description.toLowerCase().contains("submit"),
                    "OVERDUE description should indicate urgency");
        }

        @Test
        @DisplayName("should return submitted description")
        void shouldReturnSubmittedDescription() {
            String description = QuarterInfoDialog.getStatusDescription(QuarterStatus.SUBMITTED);
            assertNotNull(description);
            assertFalse(description.isEmpty());
            // Description should confirm submission
            assertTrue(description.toLowerCase().contains("submitted") ||
                       description.toLowerCase().contains("complete") ||
                       description.toLowerCase().contains("hmrc"),
                    "SUBMITTED description should confirm submission");
        }

        @Test
        @DisplayName("should return future description")
        void shouldReturnFutureDescription() {
            String description = QuarterInfoDialog.getStatusDescription(QuarterStatus.FUTURE);
            assertNotNull(description);
            assertFalse(description.isEmpty());
            // Description should mention future/upcoming
            assertTrue(description.toLowerCase().contains("future") ||
                       description.toLowerCase().contains("upcoming") ||
                       description.toLowerCase().contains("not yet"),
                    "FUTURE description should mention future status");
        }
    }

    @Nested
    @DisplayName("Action Button")
    class ActionButtonTests {

        @Test
        @DisplayName("should show Start Review button for DRAFT status")
        void shouldShowStartReviewButtonForDraft() {
            String buttonText = QuarterInfoDialog.getActionButtonText(QuarterStatus.DRAFT);
            assertEquals("Start Review", buttonText, "DRAFT should show 'Start Review' button");
        }

        @Test
        @DisplayName("should show Submit Now button for OVERDUE status")
        void shouldShowSubmitNowButtonForOverdue() {
            String buttonText = QuarterInfoDialog.getActionButtonText(QuarterStatus.OVERDUE);
            assertEquals("Submit Now", buttonText, "OVERDUE should show 'Submit Now' button");
        }

        @Test
        @DisplayName("should have no action button for SUBMITTED status")
        void shouldHaveNoActionButtonForSubmitted() {
            String buttonText = QuarterInfoDialog.getActionButtonText(QuarterStatus.SUBMITTED);
            assertNull(buttonText, "SUBMITTED should have no action button");
        }

        @Test
        @DisplayName("should have no action button for FUTURE status")
        void shouldHaveNoActionButtonForFuture() {
            String buttonText = QuarterInfoDialog.getActionButtonText(QuarterStatus.FUTURE);
            assertNull(buttonText, "FUTURE should have no action button");
        }

        @Test
        @DisplayName("should indicate if action button should be shown")
        void shouldIndicateActionButtonVisibility() {
            assertTrue(QuarterInfoDialog.shouldShowActionButton(QuarterStatus.DRAFT),
                    "DRAFT should show action button");
            assertTrue(QuarterInfoDialog.shouldShowActionButton(QuarterStatus.OVERDUE),
                    "OVERDUE should show action button");
            assertFalse(QuarterInfoDialog.shouldShowActionButton(QuarterStatus.SUBMITTED),
                    "SUBMITTED should not show action button");
            assertFalse(QuarterInfoDialog.shouldShowActionButton(QuarterStatus.FUTURE),
                    "FUTURE should not show action button");
        }
    }

    @Nested
    @DisplayName("Dialog Title")
    class DialogTitleTests {

        @Test
        @DisplayName("should create correct title for Q1")
        void shouldCreateCorrectTitleForQ1() {
            QuarterViewModel viewModel = createTestViewModel(Quarter.Q1, QuarterStatus.DRAFT);
            String title = QuarterInfoDialog.getDialogTitle(viewModel);
            assertTrue(title.contains("Q1") || title.contains("Quarter 1"),
                    "Title should include quarter identifier");
        }

        @Test
        @DisplayName("should include date range in subtitle")
        void shouldIncludeDateRangeInSubtitle() {
            QuarterViewModel viewModel = createTestViewModel(Quarter.Q1, QuarterStatus.DRAFT);
            String subtitle = viewModel.getDateRangeText();
            assertNotNull(subtitle);
            assertTrue(subtitle.contains("-"), "Date range should include separator");
        }
    }

    @Nested
    @DisplayName("YTD Calculations")
    class YtdCalculationsTests {

        @Test
        @DisplayName("should calculate YTD totals for Q1")
        void shouldCalculateYtdForQ1() {
            BigDecimal q1Income = new BigDecimal("5000.00");
            BigDecimal ytdIncome = QuarterInfoDialog.calculateYtdIncome(Quarter.Q1, q1Income, null, null, null);
            assertEquals(q1Income, ytdIncome, "Q1 YTD should equal Q1 value");
        }

        @Test
        @DisplayName("should calculate YTD totals for Q2")
        void shouldCalculateYtdForQ2() {
            BigDecimal q1Income = new BigDecimal("5000.00");
            BigDecimal q2Income = new BigDecimal("6000.00");
            BigDecimal ytdIncome = QuarterInfoDialog.calculateYtdIncome(Quarter.Q2, q1Income, q2Income, null, null);
            assertEquals(new BigDecimal("11000.00"), ytdIncome, "Q2 YTD should be Q1 + Q2");
        }

        @Test
        @DisplayName("should calculate YTD totals for Q3")
        void shouldCalculateYtdForQ3() {
            BigDecimal q1Income = new BigDecimal("5000.00");
            BigDecimal q2Income = new BigDecimal("6000.00");
            BigDecimal q3Income = new BigDecimal("7000.00");
            BigDecimal ytdIncome = QuarterInfoDialog.calculateYtdIncome(Quarter.Q3, q1Income, q2Income, q3Income, null);
            assertEquals(new BigDecimal("18000.00"), ytdIncome, "Q3 YTD should be Q1 + Q2 + Q3");
        }

        @Test
        @DisplayName("should calculate YTD totals for Q4")
        void shouldCalculateYtdForQ4() {
            BigDecimal q1Income = new BigDecimal("5000.00");
            BigDecimal q2Income = new BigDecimal("6000.00");
            BigDecimal q3Income = new BigDecimal("7000.00");
            BigDecimal q4Income = new BigDecimal("8000.00");
            BigDecimal ytdIncome = QuarterInfoDialog.calculateYtdIncome(Quarter.Q4, q1Income, q2Income, q3Income, q4Income);
            assertEquals(new BigDecimal("26000.00"), ytdIncome, "Q4 YTD should be Q1 + Q2 + Q3 + Q4");
        }

        @Test
        @DisplayName("should handle null quarter values in YTD calculation")
        void shouldHandleNullQuarterValuesInYtd() {
            BigDecimal q1Income = new BigDecimal("5000.00");
            BigDecimal ytdIncome = QuarterInfoDialog.calculateYtdIncome(Quarter.Q2, q1Income, null, null, null);
            assertEquals(q1Income, ytdIncome, "Should handle null values gracefully");
        }
    }

    @Nested
    @DisplayName("Design Compliance")
    class DesignComplianceTests {

        @Test
        @DisplayName("should use correct dialog width")
        void shouldUseCorrectDialogWidth() {
            int expectedWidth = 420;
            assertEquals(expectedWidth, QuarterInfoDialog.DIALOG_WIDTH,
                    "Dialog width should match design spec (420px)");
        }

        @Test
        @DisplayName("should use correct corner radius")
        void shouldUseCorrectCornerRadius() {
            double expectedRadius = 12.0;
            assertEquals(expectedRadius, QuarterInfoDialog.CORNER_RADIUS,
                    "Corner radius should match design spec (12px)");
        }

        @Test
        @DisplayName("status colors should be valid hex")
        void statusColorsShouldBeValidHex() {
            for (QuarterStatus status : QuarterStatus.values()) {
                String[] colors = QuarterInfoDialog.getHeaderGradientColors(status);
                assertTrue(colors[0].matches("#[0-9a-fA-F]{6}"),
                        "Start color should be valid hex for " + status);
                assertTrue(colors[1].matches("#[0-9a-fA-F]{6}"),
                        "End color should be valid hex for " + status);
            }
        }
    }

    // ========== Helper Methods ==========

    private QuarterViewModel createTestViewModel(Quarter quarter, QuarterStatus status) {
        return new QuarterViewModel(
                quarter,
                TAX_YEAR_2025,
                status,
                quarter == Quarter.Q2, // Q2 is "current" for testing
                new BigDecimal("5000.00"),
                new BigDecimal("1500.00")
        );
    }
}
