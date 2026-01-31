package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterStatus;
import uk.selfemploy.ui.viewmodel.QuarterViewModel;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for QuarterlyUpdatesController.
 * Sprint 10D: Quarterly Updates UI (SE-10D-001, SE-10D-002, SE-10D-003)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QuarterlyUpdatesController")
class QuarterlyUpdatesControllerTest {

    @Mock
    private IncomeService incomeService;

    @Mock
    private ExpenseService expenseService;

    private QuarterlyUpdatesController controller;
    private UUID businessId;
    private TaxYear taxYear202526;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        taxYear202526 = TaxYear.of(2025);
        controller = new QuarterlyUpdatesController();
    }

    @Nested
    @DisplayName("SE-10D-001: Quarterly Dashboard View")
    class QuarterlyDashboardViewTests {

        @Test
        @DisplayName("Should display all 4 quarters for the current tax year")
        void shouldDisplayAllFourQuarters() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters).hasSize(4);
            assertThat(quarters.get(0).getQuarter()).isEqualTo(Quarter.Q1);
            assertThat(quarters.get(1).getQuarter()).isEqualTo(Quarter.Q2);
            assertThat(quarters.get(2).getQuarter()).isEqualTo(Quarter.Q3);
            assertThat(quarters.get(3).getQuarter()).isEqualTo(Quarter.Q4);
        }

        @Test
        @DisplayName("Should show correct date ranges for each quarter")
        void shouldShowCorrectDateRanges() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q1 2025/26: 6 Apr 2025 - 5 Jul 2025
            assertThat(quarters.get(0).getStartDate()).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(quarters.get(0).getEndDate()).isEqualTo(LocalDate.of(2025, 7, 5));

            // Q2 2025/26: 6 Jul 2025 - 5 Oct 2025
            assertThat(quarters.get(1).getStartDate()).isEqualTo(LocalDate.of(2025, 7, 6));
            assertThat(quarters.get(1).getEndDate()).isEqualTo(LocalDate.of(2025, 10, 5));

            // Q3 2025/26: 6 Oct 2025 - 5 Jan 2026
            assertThat(quarters.get(2).getStartDate()).isEqualTo(LocalDate.of(2025, 10, 6));
            assertThat(quarters.get(2).getEndDate()).isEqualTo(LocalDate.of(2026, 1, 5));

            // Q4 2025/26: 6 Jan 2026 - 5 Apr 2026
            assertThat(quarters.get(3).getStartDate()).isEqualTo(LocalDate.of(2026, 1, 6));
            assertThat(quarters.get(3).getEndDate()).isEqualTo(LocalDate.of(2026, 4, 5));
        }

        @Test
        @DisplayName("Should show deadline dates for each quarter")
        void shouldShowDeadlineDates() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getDeadline()).isEqualTo(LocalDate.of(2025, 8, 7));  // Q1: 7 Aug
            assertThat(quarters.get(1).getDeadline()).isEqualTo(LocalDate.of(2025, 11, 7)); // Q2: 7 Nov
            assertThat(quarters.get(2).getDeadline()).isEqualTo(LocalDate.of(2026, 2, 7));  // Q3: 7 Feb
            assertThat(quarters.get(3).getDeadline()).isEqualTo(LocalDate.of(2026, 5, 7));  // Q4: 7 May
        }
    }

    @Nested
    @DisplayName("SE-10D-002: Quarter Status")
    class QuarterStatusTests {

        @Test
        @DisplayName("Should show FUTURE status for quarters that haven't started")
        void shouldShowFutureStatusForFutureQuarters() {
            // Given - Date is in Q1 (April 2025)
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 4, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q2, Q3, Q4 should be FUTURE
            assertThat(quarters.get(1).getStatus()).isEqualTo(QuarterStatus.FUTURE);
            assertThat(quarters.get(2).getStatus()).isEqualTo(QuarterStatus.FUTURE);
            assertThat(quarters.get(3).getStatus()).isEqualTo(QuarterStatus.FUTURE);
        }

        @Test
        @DisplayName("Should show DRAFT status for current quarter")
        void shouldShowDraftStatusForCurrentQuarter() {
            // Given - Date is in Q3 (October 2025)
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q3 (current) should be DRAFT
            assertThat(quarters.get(2).getStatus()).isEqualTo(QuarterStatus.DRAFT);
        }

        @Test
        @DisplayName("Should show OVERDUE status for past quarter not submitted")
        void shouldShowOverdueStatusForPastQuarterNotSubmitted() {
            // Given - Date is in Q4 (Feb 2026), Q1 deadline has passed
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2026, 2, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q1 should be OVERDUE (deadline was 7 Aug 2025)
            assertThat(quarters.get(0).getStatus()).isEqualTo(QuarterStatus.OVERDUE);
        }

        @Test
        @DisplayName("Should identify current quarter correctly")
        void shouldIdentifyCurrentQuarterCorrectly() {
            // Given - Date is in Q2 (August 2025)
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 8, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q2 should be marked as current
            assertThat(quarters.get(0).isCurrent()).isFalse(); // Q1
            assertThat(quarters.get(1).isCurrent()).isTrue();  // Q2 - current
            assertThat(quarters.get(2).isCurrent()).isFalse(); // Q3
            assertThat(quarters.get(3).isCurrent()).isFalse(); // Q4
        }
    }

    @Nested
    @DisplayName("SE-10D-003: Cumulative Totals Display")
    class CumulativeTotalsTests {

        @Test
        @DisplayName("Should display income total for non-future quarter")
        void shouldDisplayIncomeTotalForQuarter() {
            // Given - Date is late in tax year so Q1 is not future
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 9, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);

            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("5000.00"));

            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getTotalIncome()).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("Should display expense total for non-future quarter")
        void shouldDisplayExpenseTotalForQuarter() {
            // Given - Date is late in tax year so Q1 is not future
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 9, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);

            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("800.00"));

            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getTotalExpenses()).isEqualByComparingTo(new BigDecimal("800.00"));
        }

        @Test
        @DisplayName("Should calculate net profit/loss for quarter")
        void shouldCalculateNetProfitLossForQuarter() {
            // Given - Date is late in tax year so Q1 is not future
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 9, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);

            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("5000.00"));
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("800.00"));

            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Net = 5000 - 800 = 4200
            assertThat(quarters.get(0).getNetProfitLoss()).isEqualByComparingTo(new BigDecimal("4200.00"));
        }

        @Test
        @DisplayName("Should show zero totals for future quarters")
        void shouldShowZeroTotalsForFutureQuarters() {
            // Given - Date is in Q1
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 4, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q4 (future) should have null totals
            QuarterViewModel q4 = quarters.get(3);
            assertThat(q4.getTotalIncome()).isNull();
            assertThat(q4.getTotalExpenses()).isNull();
            assertThat(q4.getNetProfitLoss()).isNull();
        }

        @Test
        @DisplayName("Should handle loss scenario (expenses > income)")
        void shouldHandleLossScenario() {
            // Given - Date is late in tax year so Q1 is not future
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 9, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);

            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("1000.00"));
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("1500.00"));

            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Net = 1000 - 1500 = -500 (loss)
            assertThat(quarters.get(0).getNetProfitLoss()).isEqualByComparingTo(new BigDecimal("-500.00"));
        }
    }

    @Nested
    @DisplayName("Quarter Labels and Formatting")
    class QuarterLabelsTests {

        @Test
        @DisplayName("Should format quarter label correctly")
        void shouldFormatQuarterLabelCorrectly() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getLabel()).isEqualTo("Q1 2025/26");
            assertThat(quarters.get(1).getLabel()).isEqualTo("Q2 2025/26");
            assertThat(quarters.get(2).getLabel()).isEqualTo("Q3 2025/26");
            assertThat(quarters.get(3).getLabel()).isEqualTo("Q4 2025/26");
        }

        @Test
        @DisplayName("Should format date range correctly")
        void shouldFormatDateRangeCorrectly() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getDateRangeText()).isEqualTo("6 Apr - 5 Jul");
            assertThat(quarters.get(1).getDateRangeText()).isEqualTo("6 Jul - 5 Oct");
            assertThat(quarters.get(2).getDateRangeText()).isEqualTo("6 Oct - 5 Jan");
            assertThat(quarters.get(3).getDateRangeText()).isEqualTo("6 Jan - 5 Apr");
        }

        @Test
        @DisplayName("Should format deadline text correctly")
        void shouldFormatDeadlineTextCorrectly() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getDeadlineText()).isEqualTo("Deadline: 7 Aug 2025");
            assertThat(quarters.get(1).getDeadlineText()).isEqualTo("Deadline: 7 Nov 2025");
            assertThat(quarters.get(2).getDeadlineText()).isEqualTo("Deadline: 7 Feb 2026");
            assertThat(quarters.get(3).getDeadlineText()).isEqualTo("Deadline: 7 May 2026");
        }
    }

    @Nested
    @DisplayName("Sprint 10D-FIX: Quarter Action Button Handlers")
    class QuarterActionButtonTests {

        @Test
        @DisplayName("Should have handler methods for each quarter button")
        void shouldHaveHandlerMethodsForEachQuarterButton() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);
            controller.setDialogSuppressed(true);  // Suppress dialogs in tests

            // Then - Verify the methods exist and are callable (no exceptions)
            assertThat(controller).isNotNull();
            // Handlers should be invokable - these test that the methods exist
            controller.handleQ1Action();
            controller.handleQ2Action();
            controller.handleQ3Action();
            controller.handleQ4Action();
        }

        @Test
        @DisplayName("Should track which quarter action was called")
        void shouldTrackWhichQuarterActionWasCalled() {
            // Given
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);
            controller.setDialogSuppressed(true);  // Suppress dialogs in tests

            // When
            controller.handleQ1Action();

            // Then - Last clicked quarter should be Q1
            assertThat(controller.getLastClickedQuarter()).isEqualTo(Quarter.Q1);

            // When
            controller.handleQ3Action();

            // Then - Last clicked quarter should be Q3
            assertThat(controller.getLastClickedQuarter()).isEqualTo(Quarter.Q3);
        }

        @Test
        @DisplayName("Should not process action for disabled FUTURE quarter")
        void shouldNotProcessActionForFutureQuarter() {
            // Given - Date is in Q1, so Q4 is FUTURE
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 4, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);
            controller.setDialogSuppressed(true);  // Suppress dialogs in tests

            // When - Attempt to click Q4 (should be disabled but method still callable)
            controller.handleQ4Action();

            // Then - Q4 is FUTURE, action should be ignored (last clicked stays null)
            assertThat(controller.getLastClickedQuarter()).isNull();
        }
    }

    @Nested
    @DisplayName("Sprint 10D-FIX: Tax Year Label")
    class TaxYearLabelTests {

        @Test
        @DisplayName("Should return correct tax year label text")
        void shouldReturnCorrectTaxYearLabelText() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            String taxYearText = controller.getTaxYearLabelText();

            // Then
            assertThat(taxYearText).isEqualTo("Tax Year: 2025/26");
        }

        @Test
        @DisplayName("Should return empty text when tax year not set")
        void shouldReturnEmptyTextWhenTaxYearNotSet() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            // No tax year set

            // When
            String taxYearText = controller.getTaxYearLabelText();

            // Then
            assertThat(taxYearText).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Sprint 10D-FIX: Button Text Based on Status")
    class ButtonTextTests {

        @Test
        @DisplayName("Should return 'View' for DRAFT status")
        void shouldReturnViewForDraftStatus() {
            // Given
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            String buttonText = controller.getButtonTextForQuarter(Quarter.Q3);

            // Then - Q3 is current quarter, should be DRAFT
            assertThat(buttonText).isEqualTo("Review");
        }

        @Test
        @DisplayName("Should return 'Submit Now' for OVERDUE status")
        void shouldReturnSubmitNowForOverdueStatus() {
            // Given - Date is in Feb 2026, Q1 deadline passed
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2026, 2, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            String buttonText = controller.getButtonTextForQuarter(Quarter.Q1);

            // Then - Q1 is OVERDUE
            assertThat(buttonText).isEqualTo("Submit Now");
        }

        @Test
        @DisplayName("Should return 'Future' for FUTURE status")
        void shouldReturnFutureForFutureStatus() {
            // Given - Date is in Q1, Q4 is FUTURE
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 4, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            String buttonText = controller.getButtonTextForQuarter(Quarter.Q4);

            // Then - Q4 is FUTURE
            assertThat(buttonText).isEqualTo("Future");
        }
    }

    @Nested
    @DisplayName("Sprint 10D-FIX-2: Quarter Action Dialogs")
    class QuarterActionDialogTests {

        @Test
        @DisplayName("Should generate correct dialog title for DRAFT quarter")
        void shouldGenerateCorrectDialogTitleForDraftQuarter() {
            // Given - Date is in Q3, so Q3 is DRAFT
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            String title = controller.getDialogTitleForQuarter(Quarter.Q3);

            // Then
            assertThat(title).isEqualTo("Review Quarter");
        }

        @Test
        @DisplayName("Should generate correct dialog title for OVERDUE quarter")
        void shouldGenerateCorrectDialogTitleForOverdueQuarter() {
            // Given - Date is in Feb 2026, Q1 deadline passed
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2026, 2, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            String title = controller.getDialogTitleForQuarter(Quarter.Q1);

            // Then
            assertThat(title).isEqualTo("Submit Overdue Quarter");
        }

        @Test
        @DisplayName("Should generate correct dialog title for SUBMITTED quarter")
        void shouldGenerateCorrectDialogTitleForSubmittedQuarter() {
            // Given - Create a controller that can return SUBMITTED status
            // We'll use a custom approach since we can't easily mock SUBMITTED
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When/Then - Test the method directly with status
            String title = controller.getDialogTitleForStatus(QuarterStatus.SUBMITTED);
            assertThat(title).isEqualTo("View Submission");
        }

        @Test
        @DisplayName("Should include deadline in dialog message for DRAFT quarter")
        void shouldIncludeDeadlineInDialogMessageForDraftQuarter() {
            // Given - Date is in Q3
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            String message = controller.getDialogMessageForQuarter(Quarter.Q3);

            // Then - Should contain deadline info
            assertThat(message).contains("7 Feb 2026"); // Q3 deadline
            assertThat(message).contains("Review");
        }

        @Test
        @DisplayName("Should include OVERDUE warning in dialog message for OVERDUE quarter")
        void shouldIncludeOverdueWarningInDialogMessageForOverdueQuarter() {
            // Given - Date is in Feb 2026, Q1 deadline passed
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2026, 2, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When
            String message = controller.getDialogMessageForQuarter(Quarter.Q1);

            // Then - Should contain overdue warning
            assertThat(message).contains("OVERDUE");
            assertThat(message).contains("Submit");
        }

        @Test
        @DisplayName("Should include financial totals in dialog message for OVERDUE quarter")
        void shouldIncludeFinancialTotalsInDialogMessageForOverdueQuarter() {
            // Given - Date is in Feb 2026, Q1 deadline passed
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2026, 2, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);

            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("5000.00"));
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("1000.00"));

            controller.setTaxYear(taxYear202526);

            // When
            String message = controller.getDialogMessageForQuarter(Quarter.Q1);

            // Then - Should contain financial totals
            assertThat(message).contains("5,000.00");
            assertThat(message).contains("1,000.00");
        }

        @Test
        @DisplayName("Should show already submitted message for SUBMITTED quarter")
        void shouldShowAlreadySubmittedMessageForSubmittedQuarter() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When - Test directly with status
            String message = controller.getDialogMessageForStatus(Quarter.Q1, QuarterStatus.SUBMITTED, null);

            // Then
            assertThat(message).contains("already been submitted");
        }

        @Test
        @DisplayName("Should record that dialog was shown for quarter action")
        void shouldRecordThatDialogWasShownForQuarterAction() {
            // Given - Date is in Q3
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When - Call action (with dialog suppressed in test mode)
            controller.setDialogSuppressed(true);
            controller.handleQ3Action();

            // Then - Dialog should have been "shown" (recorded)
            assertThat(controller.wasDialogShownForQuarter(Quarter.Q3)).isTrue();
        }

        @Test
        @DisplayName("Should not show dialog for FUTURE quarter")
        void shouldNotShowDialogForFutureQuarter() {
            // Given - Date is in Q1, Q4 is FUTURE
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 4, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            // When - Call action for FUTURE quarter (dialog suppressed)
            controller.setDialogSuppressed(true);
            controller.handleQ4Action();

            // Then - Dialog should NOT have been shown
            assertThat(controller.wasDialogShownForQuarter(Quarter.Q4)).isFalse();
        }
    }

    @Nested
    @DisplayName("Sprint 10D-FIX-2: Back Button Callback")
    class BackButtonCallbackTests {

        @Test
        @DisplayName("Should invoke onBack callback when handleBack is called")
        void shouldInvokeOnBackCallbackWhenHandleBackIsCalled() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            boolean[] callbackInvoked = {false};
            controller.setOnBack(() -> callbackInvoked[0] = true);

            // When
            controller.handleBack();

            // Then
            assertThat(callbackInvoked[0]).isTrue();
        }

        @Test
        @DisplayName("Should not throw when handleBack called without callback")
        void shouldNotThrowWhenHandleBackCalledWithoutCallback() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            // No callback set

            // When/Then - Should not throw
            controller.handleBack();
        }
    }

    @Nested
    @DisplayName("Quarterly Review Data Aggregation")
    class QuarterlyReviewDataAggregationTests {

        @Test
        @DisplayName("Should aggregate review data with income and expenses")
        void shouldAggregateReviewDataWithIncomeAndExpenses() {
            // Given
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("5000.00"));
            when(incomeService.countByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(3);

            List<Expense> expenses = List.of(
                createExpense(new BigDecimal("500.00"), ExpenseCategory.OFFICE_COSTS),
                createExpense(new BigDecimal("300.00"), ExpenseCategory.OFFICE_COSTS),
                createExpense(new BigDecimal("200.00"), ExpenseCategory.TRAVEL)
            );
            when(expenseService.findByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(expenses);

            // When
            QuarterlyReviewData reviewData = controller.aggregateReviewData(Quarter.Q1);

            // Then
            assertThat(reviewData.getQuarter()).isEqualTo(Quarter.Q1);
            assertThat(reviewData.getTaxYear()).isEqualTo(taxYear202526);
            assertThat(reviewData.getTotalIncome()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(reviewData.getIncomeTransactionCount()).isEqualTo(3);
            assertThat(reviewData.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(reviewData.getExpenseTransactionCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should group expenses by SA103 category")
        void shouldGroupExpensesBySA103Category() {
            // Given
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            lenient().when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(BigDecimal.ZERO);
            lenient().when(incomeService.countByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(0);

            List<Expense> expenses = List.of(
                createExpense(new BigDecimal("500.00"), ExpenseCategory.OFFICE_COSTS),
                createExpense(new BigDecimal("300.00"), ExpenseCategory.OFFICE_COSTS),
                createExpense(new BigDecimal("200.00"), ExpenseCategory.TRAVEL),
                createExpense(new BigDecimal("150.00"), ExpenseCategory.PROFESSIONAL_FEES)
            );
            when(expenseService.findByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(expenses);

            // When
            QuarterlyReviewData reviewData = controller.aggregateReviewData(Quarter.Q1);

            // Then
            Map<ExpenseCategory, CategorySummary> expensesByCategory = reviewData.getExpensesByCategory();
            assertThat(expensesByCategory).hasSize(3);

            CategorySummary officeCosts = expensesByCategory.get(ExpenseCategory.OFFICE_COSTS);
            assertThat(officeCosts.amount()).isEqualByComparingTo(new BigDecimal("800.00"));
            assertThat(officeCosts.transactionCount()).isEqualTo(2);

            CategorySummary travel = expensesByCategory.get(ExpenseCategory.TRAVEL);
            assertThat(travel.amount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(travel.transactionCount()).isEqualTo(1);

            CategorySummary professionalFees = expensesByCategory.get(ExpenseCategory.PROFESSIONAL_FEES);
            assertThat(professionalFees.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(professionalFees.transactionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate net profit correctly")
        void shouldCalculateNetProfitCorrectly() {
            // Given
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("5000.00"));
            lenient().when(incomeService.countByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(3);

            List<Expense> expenses = List.of(
                createExpense(new BigDecimal("1000.00"), ExpenseCategory.OFFICE_COSTS)
            );
            when(expenseService.findByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(expenses);

            // When
            QuarterlyReviewData reviewData = controller.aggregateReviewData(Quarter.Q1);

            // Then
            assertThat(reviewData.getNetProfit()).isEqualByComparingTo(new BigDecimal("4000.00"));
            assertThat(reviewData.isNilReturn()).isFalse();
        }

        @Test
        @DisplayName("Should detect nil return when no income or expenses")
        void shouldDetectNilReturnWhenNoIncomeOrExpenses() {
            // Given
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(BigDecimal.ZERO);
            lenient().when(incomeService.countByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(0);
            when(expenseService.findByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(List.of());

            // When
            QuarterlyReviewData reviewData = controller.aggregateReviewData(Quarter.Q1);

            // Then
            assertThat(reviewData.isNilReturn()).isTrue();
            assertThat(reviewData.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(reviewData.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should exclude non-allowable expenses from totals")
        void shouldExcludeNonAllowableExpensesFromTotals() {
            // Given
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            lenient().when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(BigDecimal.ZERO);
            lenient().when(incomeService.countByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(0);

            List<Expense> expenses = List.of(
                createExpense(new BigDecimal("500.00"), ExpenseCategory.OFFICE_COSTS),     // Allowable
                createExpense(new BigDecimal("300.00"), ExpenseCategory.DEPRECIATION),     // NOT allowable
                createExpense(new BigDecimal("200.00"), ExpenseCategory.BUSINESS_ENTERTAINMENT) // NOT allowable
            );
            when(expenseService.findByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(expenses);

            // When
            QuarterlyReviewData reviewData = controller.aggregateReviewData(Quarter.Q1);

            // Then - Only allowable expenses should be included
            assertThat(reviewData.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(reviewData.getExpenseTransactionCount()).isEqualTo(1);
            assertThat(reviewData.getExpensesByCategory()).containsKey(ExpenseCategory.OFFICE_COSTS);
            assertThat(reviewData.getExpensesByCategory()).doesNotContainKey(ExpenseCategory.DEPRECIATION);
        }

        @Test
        @DisplayName("Should set correct period dates for quarter")
        void shouldSetCorrectPeriodDatesForQuarter() {
            // Given
            Clock fixedClock = Clock.fixed(
                LocalDate.of(2025, 10, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
            );
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClock);
            controller.setTaxYear(taxYear202526);

            lenient().when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(BigDecimal.ZERO);
            lenient().when(incomeService.countByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(0);
            lenient().when(expenseService.findByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(List.of());

            // When
            QuarterlyReviewData reviewData = controller.aggregateReviewData(Quarter.Q1);

            // Then
            assertThat(reviewData.getPeriodStart()).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(reviewData.getPeriodEnd()).isEqualTo(LocalDate.of(2025, 7, 5));
        }

        private Expense createExpense(BigDecimal amount, ExpenseCategory category) {
            return Expense.create(
                businessId,
                LocalDate.of(2025, 5, 15),
                amount,
                "Test expense",
                category,
                null,
                null
            );
        }
    }
}
