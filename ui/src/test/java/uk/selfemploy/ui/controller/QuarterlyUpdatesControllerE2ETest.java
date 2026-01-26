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
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.QuarterStatus;
import uk.selfemploy.ui.viewmodel.QuarterViewModel;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * E2E Tests for QuarterlyUpdatesController.
 * Sprint 10D: Quarterly Updates UI (SE-10D-001, SE-10D-002, SE-10D-003)
 *
 * <p>Test cases based on /rob's QA Test Design Document:</p>
 * <ul>
 *   <li>TC-QU-001: All 4 quarters displayed</li>
 *   <li>TC-QU-002: Quarter date ranges correct</li>
 *   <li>TC-QU-003: Quarter deadlines correct</li>
 *   <li>TC-QU-004: Current quarter highlighted</li>
 *   <li>TC-QU-005: FUTURE status for future quarters</li>
 *   <li>TC-QU-006: DRAFT status for current quarter</li>
 *   <li>TC-QU-007: OVERDUE status for past deadline</li>
 *   <li>TC-QU-008: Income totals displayed correctly</li>
 *   <li>TC-QU-009: Expense totals displayed correctly</li>
 *   <li>TC-QU-010: Net profit/loss calculated correctly</li>
 *   <li>TC-QU-011: Future quarters show "--" for financials</li>
 *   <li>TC-QU-012: Keyboard navigation works</li>
 *   <li>TC-QU-015: Tax year boundary handling</li>
 *   <li>TC-QU-016: No financial data scenario</li>
 *   <li>TC-QU-017: Service error handling</li>
 *   <li>TC-QU-018: Accessible text for screen readers</li>
 * </ul>
 *
 * @see QuarterlyUpdatesController
 * @see QuarterViewModel
 * @see QuarterStatus
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("QuarterlyUpdatesController E2E Tests")
class QuarterlyUpdatesControllerE2ETest {

    @Mock
    private IncomeService incomeService;

    @Mock
    private ExpenseService expenseService;

    private QuarterlyUpdatesController controller;
    private UUID businessId;
    private TaxYear taxYear202526;

    // Test data constants from /rob's test design
    private static final BigDecimal Q1_INCOME = new BigDecimal("5000.00");
    private static final BigDecimal Q1_EXPENSES = new BigDecimal("800.00");
    private static final BigDecimal Q2_INCOME = new BigDecimal("4200.00");
    private static final BigDecimal Q2_EXPENSES = new BigDecimal("650.00");
    private static final BigDecimal Q3_INCOME = new BigDecimal("3100.00");
    private static final BigDecimal Q3_EXPENSES = new BigDecimal("400.00");

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        taxYear202526 = TaxYear.of(2025);
        controller = new QuarterlyUpdatesController();
    }

    /**
     * Creates a fixed clock for the given date.
     */
    private Clock fixedClockAt(int year, int month, int day) {
        return Clock.fixed(
            LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
    }

    /**
     * Sets up mock services with standard test data.
     */
    private void setupStandardTestData() {
        // Q1 data
        lenient().when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
            .thenReturn(Q1_INCOME);
        lenient().when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
            .thenReturn(Q1_EXPENSES);

        // Q2 data
        lenient().when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q2)))
            .thenReturn(Q2_INCOME);
        lenient().when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q2)))
            .thenReturn(Q2_EXPENSES);

        // Q3 data
        lenient().when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q3)))
            .thenReturn(Q3_INCOME);
        lenient().when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q3)))
            .thenReturn(Q3_EXPENSES);

        // Q4 - no data (null returns)
        lenient().when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q4)))
            .thenReturn(null);
        lenient().when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q4)))
            .thenReturn(null);
    }

    // =========================================================================
    // TC-QU-001: All 4 Quarters Displayed
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-001: All 4 Quarters Displayed")
    class AllQuartersDisplayedTests {

        @Test
        @DisplayName("Should display exactly 4 quarters (Q1-Q4)")
        void shouldDisplayAllFourQuarters() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters)
                .hasSize(4)
                .extracting(QuarterViewModel::getQuarter)
                .containsExactly(Quarter.Q1, Quarter.Q2, Quarter.Q3, Quarter.Q4);
        }

        @Test
        @DisplayName("Should display quarters in correct order (Q1, Q2, Q3, Q4)")
        void shouldDisplayQuartersInCorrectOrder() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getQuarter()).isEqualTo(Quarter.Q1);
            assertThat(quarters.get(1).getQuarter()).isEqualTo(Quarter.Q2);
            assertThat(quarters.get(2).getQuarter()).isEqualTo(Quarter.Q3);
            assertThat(quarters.get(3).getQuarter()).isEqualTo(Quarter.Q4);
        }
    }

    // =========================================================================
    // TC-QU-002: Quarter Date Ranges Correct
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-002: Quarter Date Ranges Correct")
    class QuarterDateRangesTests {

        @Test
        @DisplayName("Q1 should have date range 6 Apr - 5 Jul")
        void q1ShouldHaveCorrectDateRange() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();
            QuarterViewModel q1 = quarters.get(0);

            // Then
            assertThat(q1.getStartDate()).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(q1.getEndDate()).isEqualTo(LocalDate.of(2025, 7, 5));
            assertThat(q1.getDateRangeText()).isEqualTo("6 Apr - 5 Jul");
        }

        @Test
        @DisplayName("Q2 should have date range 6 Jul - 5 Oct")
        void q2ShouldHaveCorrectDateRange() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();
            QuarterViewModel q2 = quarters.get(1);

            // Then
            assertThat(q2.getStartDate()).isEqualTo(LocalDate.of(2025, 7, 6));
            assertThat(q2.getEndDate()).isEqualTo(LocalDate.of(2025, 10, 5));
            assertThat(q2.getDateRangeText()).isEqualTo("6 Jul - 5 Oct");
        }

        @Test
        @DisplayName("Q3 should have date range 6 Oct - 5 Jan (crossing calendar year)")
        void q3ShouldHaveCorrectDateRange() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();
            QuarterViewModel q3 = quarters.get(2);

            // Then
            assertThat(q3.getStartDate()).isEqualTo(LocalDate.of(2025, 10, 6));
            assertThat(q3.getEndDate()).isEqualTo(LocalDate.of(2026, 1, 5));
            assertThat(q3.getDateRangeText()).isEqualTo("6 Oct - 5 Jan");
        }

        @Test
        @DisplayName("Q4 should have date range 6 Jan - 5 Apr")
        void q4ShouldHaveCorrectDateRange() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();
            QuarterViewModel q4 = quarters.get(3);

            // Then
            assertThat(q4.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 6));
            assertThat(q4.getEndDate()).isEqualTo(LocalDate.of(2026, 4, 5));
            assertThat(q4.getDateRangeText()).isEqualTo("6 Jan - 5 Apr");
        }
    }

    // =========================================================================
    // TC-QU-003: Quarter Deadlines Correct
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-003: Quarter Deadlines Correct")
    class QuarterDeadlinesTests {

        @Test
        @DisplayName("Q1 deadline should be 7 Aug 2025")
        void q1ShouldHaveCorrectDeadline() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getDeadline()).isEqualTo(LocalDate.of(2025, 8, 7));
            assertThat(quarters.get(0).getDeadlineText()).isEqualTo("Deadline: 7 Aug 2025");
        }

        @Test
        @DisplayName("Q2 deadline should be 7 Nov 2025")
        void q2ShouldHaveCorrectDeadline() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(1).getDeadline()).isEqualTo(LocalDate.of(2025, 11, 7));
            assertThat(quarters.get(1).getDeadlineText()).isEqualTo("Deadline: 7 Nov 2025");
        }

        @Test
        @DisplayName("Q3 deadline should be 7 Feb 2026")
        void q3ShouldHaveCorrectDeadline() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(2).getDeadline()).isEqualTo(LocalDate.of(2026, 2, 7));
            assertThat(quarters.get(2).getDeadlineText()).isEqualTo("Deadline: 7 Feb 2026");
        }

        @Test
        @DisplayName("Q4 deadline should be 7 May 2026")
        void q4ShouldHaveCorrectDeadline() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(3).getDeadline()).isEqualTo(LocalDate.of(2026, 5, 7));
            assertThat(quarters.get(3).getDeadlineText()).isEqualTo("Deadline: 7 May 2026");
        }
    }

    // =========================================================================
    // TC-QU-004: Current Quarter Highlighted
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-004: Current Quarter Highlighted")
    class CurrentQuarterHighlightedTests {

        @Test
        @DisplayName("Should mark Q1 as current when date is in Q1")
        void shouldMarkQ1AsCurrentWhenInQ1() {
            // Given - Date is 15 April 2025 (Q1)
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).isCurrent()).isTrue();  // Q1
            assertThat(quarters.get(1).isCurrent()).isFalse(); // Q2
            assertThat(quarters.get(2).isCurrent()).isFalse(); // Q3
            assertThat(quarters.get(3).isCurrent()).isFalse(); // Q4
        }

        @Test
        @DisplayName("Should mark Q2 as current when date is in Q2")
        void shouldMarkQ2AsCurrentWhenInQ2() {
            // Given - Date is 15 August 2025 (Q2)
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 8, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).isCurrent()).isFalse(); // Q1
            assertThat(quarters.get(1).isCurrent()).isTrue();  // Q2
            assertThat(quarters.get(2).isCurrent()).isFalse(); // Q3
            assertThat(quarters.get(3).isCurrent()).isFalse(); // Q4
        }

        @Test
        @DisplayName("Should mark Q3 as current when date is in Q3")
        void shouldMarkQ3AsCurrentWhenInQ3() {
            // Given - Date is 15 October 2025 (Q3)
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).isCurrent()).isFalse(); // Q1
            assertThat(quarters.get(1).isCurrent()).isFalse(); // Q2
            assertThat(quarters.get(2).isCurrent()).isTrue();  // Q3
            assertThat(quarters.get(3).isCurrent()).isFalse(); // Q4
        }

        @Test
        @DisplayName("Should mark Q4 as current when date is in Q4")
        void shouldMarkQ4AsCurrentWhenInQ4() {
            // Given - Date is 15 February 2026 (Q4)
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2026, 2, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).isCurrent()).isFalse(); // Q1
            assertThat(quarters.get(1).isCurrent()).isFalse(); // Q2
            assertThat(quarters.get(2).isCurrent()).isFalse(); // Q3
            assertThat(quarters.get(3).isCurrent()).isTrue();  // Q4
        }

        @Test
        @DisplayName("Only one quarter should be marked as current at any time")
        void onlyOneQuarterShouldBeCurrentAtAnyTime() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            long currentCount = quarters.stream().filter(QuarterViewModel::isCurrent).count();
            assertThat(currentCount).isEqualTo(1);
        }
    }

    // =========================================================================
    // TC-QU-005: FUTURE Status for Future Quarters
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-005: FUTURE Status for Future Quarters")
    class FutureStatusTests {

        @Test
        @DisplayName("Should show FUTURE status for quarters that haven't started")
        void shouldShowFutureStatusForFutureQuarters() {
            // Given - Date is in Q1 (April 2025)
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q2, Q3, Q4 should be FUTURE
            assertThat(quarters.get(1).getStatus()).isEqualTo(QuarterStatus.FUTURE);
            assertThat(quarters.get(2).getStatus()).isEqualTo(QuarterStatus.FUTURE);
            assertThat(quarters.get(3).getStatus()).isEqualTo(QuarterStatus.FUTURE);
        }

        @Test
        @DisplayName("FUTURE status should have correct display text")
        void futureStatusShouldHaveCorrectDisplayText() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(3).getStatus().getDisplayText()).isEqualTo("Future");
        }

        @Test
        @DisplayName("FUTURE status should have correct style class")
        void futureStatusShouldHaveCorrectStyleClass() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(3).getStatus().getStyleClass()).isEqualTo("status-badge-future");
            assertThat(quarters.get(3).getStatus().getCardStyleClass()).isEqualTo("quarter-card-future");
        }
    }

    // =========================================================================
    // TC-QU-006: DRAFT Status for Current Quarter
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-006: DRAFT Status for Current Quarter")
    class DraftStatusTests {

        @Test
        @DisplayName("Should show DRAFT status for current quarter")
        void shouldShowDraftStatusForCurrentQuarter() {
            // Given - Date is in Q3 (October 2025)
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q3 (current) should be DRAFT
            assertThat(quarters.get(2).getStatus()).isEqualTo(QuarterStatus.DRAFT);
        }

        @Test
        @DisplayName("DRAFT status should have correct display text")
        void draftStatusShouldHaveCorrectDisplayText() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(2).getStatus().getDisplayText()).isEqualTo("Draft");
        }

        @Test
        @DisplayName("DRAFT status should have correct style class")
        void draftStatusShouldHaveCorrectStyleClass() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(2).getStatus().getStyleClass()).isEqualTo("status-badge-draft");
            assertThat(quarters.get(2).getStatus().getCardStyleClass()).isEqualTo("quarter-card-draft");
        }

        @Test
        @DisplayName("DRAFT status colors should match design spec")
        void draftStatusColorsShouldMatchDesignSpec() {
            // Given - From test design document
            // Background: #fff3cd (light yellow)
            // Text: #856404 (dark yellow/brown)

            // When
            QuarterStatus draftStatus = QuarterStatus.DRAFT;

            // Then
            assertThat(draftStatus.getBackgroundColor()).isEqualTo("#fff3cd");
            assertThat(draftStatus.getTextColor()).isEqualTo("#856404");
        }
    }

    // =========================================================================
    // TC-QU-007: OVERDUE Status for Past Deadline
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-007: OVERDUE Status for Past Deadline")
    class OverdueStatusTests {

        @Test
        @DisplayName("Should show OVERDUE status for Q1 when after Q1 deadline")
        void shouldShowOverdueStatusForQ1WhenAfterDeadline() {
            // Given - Date is 15 February 2026, Q1 deadline was 7 Aug 2025
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2026, 2, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q1 should be OVERDUE
            assertThat(quarters.get(0).getStatus()).isEqualTo(QuarterStatus.OVERDUE);
        }

        @Test
        @DisplayName("Should show OVERDUE status for Q2 when after Q2 deadline")
        void shouldShowOverdueStatusForQ2WhenAfterDeadline() {
            // Given - Date is 15 February 2026, Q2 deadline was 7 Nov 2025
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2026, 2, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q2 should also be OVERDUE
            assertThat(quarters.get(1).getStatus()).isEqualTo(QuarterStatus.OVERDUE);
        }

        @Test
        @DisplayName("OVERDUE status should have correct display text")
        void overdueStatusShouldHaveCorrectDisplayText() {
            // When
            QuarterStatus overdueStatus = QuarterStatus.OVERDUE;

            // Then
            assertThat(overdueStatus.getDisplayText()).isEqualTo("Overdue");
        }

        @Test
        @DisplayName("OVERDUE status should have warning styling")
        void overdueStatusShouldHaveWarningStyle() {
            // When
            QuarterStatus overdueStatus = QuarterStatus.OVERDUE;

            // Then - From test design document:
            // Background: #f8d7da (light red)
            // Text: #721c24 (dark red)
            assertThat(overdueStatus.getBackgroundColor()).isEqualTo("#f8d7da");
            assertThat(overdueStatus.getTextColor()).isEqualTo("#721c24");
            assertThat(overdueStatus.getStyleClass()).isEqualTo("status-badge-overdue");
        }

        @Test
        @DisplayName("Should not show OVERDUE for quarters before deadline")
        void shouldNotShowOverdueForQuartersBeforeDeadline() {
            // Given - Date is 1 August 2025, Q1 deadline is 7 Aug 2025
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 8, 1));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q1 should NOT be OVERDUE (deadline not yet passed)
            assertThat(quarters.get(0).getStatus()).isNotEqualTo(QuarterStatus.OVERDUE);
        }
    }

    // =========================================================================
    // TC-QU-008: Income Totals Displayed Correctly
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-008: Income Totals Displayed Correctly")
    class IncomeTotalsTests {

        @Test
        @DisplayName("Should display correct income total for Q1")
        void shouldDisplayCorrectIncomeForQ1() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15)); // Date in Q3 so Q1 data loads
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getTotalIncome()).isEqualByComparingTo(Q1_INCOME);
        }

        @Test
        @DisplayName("Should display correct income total for Q2")
        void shouldDisplayCorrectIncomeForQ2() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15)); // Date in Q3 so Q2 data loads
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(1).getTotalIncome()).isEqualByComparingTo(Q2_INCOME);
        }

        @Test
        @DisplayName("Should format income with pound sign and 2 decimal places")
        void shouldFormatIncomeCorrectly() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Format: £X,XXX.XX
            assertThat(quarters.get(0).getFormattedIncome()).isEqualTo("\u00A35,000.00");
        }

        @Test
        @DisplayName("Should format income with thousands separator")
        void shouldFormatIncomeWithThousandsSeparator() {
            // Given - Large income amount
            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("12345.67"));
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Includes comma for thousands
            assertThat(quarters.get(0).getFormattedIncome()).isEqualTo("\u00A312,345.67");
        }
    }

    // =========================================================================
    // TC-QU-009: Expense Totals Displayed Correctly
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-009: Expense Totals Displayed Correctly")
    class ExpenseTotalsTests {

        @Test
        @DisplayName("Should display correct expense total for Q1")
        void shouldDisplayCorrectExpensesForQ1() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getTotalExpenses()).isEqualByComparingTo(Q1_EXPENSES);
        }

        @Test
        @DisplayName("Should display correct expense total for Q2")
        void shouldDisplayCorrectExpensesForQ2() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(1).getTotalExpenses()).isEqualByComparingTo(Q2_EXPENSES);
        }

        @Test
        @DisplayName("Should format expenses with pound sign and 2 decimal places")
        void shouldFormatExpensesCorrectly() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getFormattedExpenses()).isEqualTo("\u00A3800.00");
        }
    }

    // =========================================================================
    // TC-QU-010: Net Profit/Loss Calculated Correctly
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-010: Net Profit/Loss Calculated Correctly")
    class NetProfitLossTests {

        @Test
        @DisplayName("Should calculate net profit correctly (income - expenses)")
        void shouldCalculateNetProfitCorrectly() {
            // Given - Q1: Income £5000, Expenses £800 = Net £4200
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getNetProfitLoss())
                .isEqualByComparingTo(Q1_INCOME.subtract(Q1_EXPENSES));
            assertThat(quarters.get(0).getNetProfitLoss())
                .isEqualByComparingTo(new BigDecimal("4200.00"));
        }

        @Test
        @DisplayName("Should handle loss scenario (expenses > income)")
        void shouldHandleLossScenario() {
            // Given - Income < Expenses
            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("1000.00"));
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("1500.00"));
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Net = 1000 - 1500 = -500 (loss)
            assertThat(quarters.get(0).getNetProfitLoss())
                .isEqualByComparingTo(new BigDecimal("-500.00"));
        }

        @Test
        @DisplayName("Should format net profit/loss correctly")
        void shouldFormatNetProfitLossCorrectly() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getFormattedNetProfitLoss()).isEqualTo("\u00A34,200.00");
        }

        @Test
        @DisplayName("Should handle zero income with expenses (pure loss)")
        void shouldHandleZeroIncomeWithExpenses() {
            // Given
            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("500.00"));
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getNetProfitLoss())
                .isEqualByComparingTo(new BigDecimal("-500.00"));
        }

        @Test
        @DisplayName("Should handle income with zero expenses (pure profit)")
        void shouldHandleIncomeWithZeroExpenses() {
            // Given
            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(new BigDecimal("5000.00"));
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(BigDecimal.ZERO);
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getNetProfitLoss())
                .isEqualByComparingTo(new BigDecimal("5000.00"));
        }
    }

    // =========================================================================
    // TC-QU-011: Future Quarters Show "--" for Financials
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-011: Future Quarters Show '--' for Financials")
    class FutureQuartersFinancialsTests {

        @Test
        @DisplayName("Future quarters should have null income")
        void futureQuartersShouldHaveNullIncome() {
            // Given - Date is in Q1
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q4 (future) should have null income
            assertThat(quarters.get(3).getTotalIncome()).isNull();
        }

        @Test
        @DisplayName("Future quarters should have null expenses")
        void futureQuartersShouldHaveNullExpenses() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q4 (future) should have null expenses
            assertThat(quarters.get(3).getTotalExpenses()).isNull();
        }

        @Test
        @DisplayName("Future quarters should have null net profit/loss")
        void futureQuartersShouldHaveNullNetProfitLoss() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(3).getNetProfitLoss()).isNull();
        }

        @Test
        @DisplayName("Future quarters should display '--' for formatted income")
        void futureQuartersShouldDisplayDashForIncome() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(3).getFormattedIncome()).isEqualTo("--");
        }

        @Test
        @DisplayName("Future quarters should display '--' for formatted expenses")
        void futureQuartersShouldDisplayDashForExpenses() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(3).getFormattedExpenses()).isEqualTo("--");
        }

        @Test
        @DisplayName("Future quarters should display '--' for formatted net")
        void futureQuartersShouldDisplayDashForNet() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(3).getFormattedNetProfitLoss()).isEqualTo("--");
        }

        @Test
        @DisplayName("Future quarters should not have data")
        void futureQuartersShouldNotHaveData() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(3).hasData()).isFalse();
        }
    }

    // =========================================================================
    // TC-QU-012: Keyboard Navigation Works
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-012: Keyboard Navigation / Accessibility")
    class KeyboardNavigationTests {

        @Test
        @DisplayName("Quarter view model should provide accessible text")
        void quarterViewModelShouldProvideAccessibleText() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15)); // Q3 is current
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();
            String accessibleText = quarters.get(2).getAccessibleText();

            // Then - Should include key information
            assertThat(accessibleText)
                .contains("Q3 2025/26")
                .contains("current quarter")
                .contains("Draft")
                .contains("income")
                .contains("expenses")
                .contains("Deadline");
        }

        @Test
        @DisplayName("Accessible text should include financial amounts when data exists")
        void accessibleTextShouldIncludeFinancialAmountsWhenDataExists() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 12, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();
            String accessibleText = quarters.get(0).getAccessibleText(); // Q1

            // Then
            assertThat(accessibleText)
                .contains("\u00A35,000.00")
                .contains("\u00A3800.00")
                .contains("\u00A34,200.00");
        }

        @Test
        @DisplayName("Accessible text for future quarter should not include financial amounts")
        void accessibleTextForFutureQuarterShouldNotIncludeFinancialAmounts() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 4, 15)); // Q1 is current, Q4 is future
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();
            String accessibleText = quarters.get(3).getAccessibleText(); // Q4 (future)

            // Then - Should not include income/expenses/net
            assertThat(accessibleText)
                .contains("Q4 2025/26")
                .contains("Future")
                .contains("Deadline")
                .doesNotContain("income")
                .doesNotContain("expenses");
        }
    }

    // =========================================================================
    // TC-QU-015: Tax Year Boundary - Q3 End Date in Next Calendar Year
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-015: Tax Year Boundary Handling")
    class TaxYearBoundaryTests {

        @Test
        @DisplayName("Q3 should be current when date is 1 January (crosses calendar year)")
        void q3ShouldBeCurrentOnJan1() {
            // Given - Date is 1 January 2026, still within Q3 (ends 5 Jan)
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2026, 1, 1));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q3 should be current (ends 5 Jan)
            assertThat(quarters.get(2).isCurrent()).isTrue(); // Q3
            assertThat(quarters.get(3).isCurrent()).isFalse(); // Q4
        }

        @Test
        @DisplayName("Q3 end date should be in following calendar year")
        void q3EndDateShouldBeInFollowingCalendarYear() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q3 of 2025/26 ends in January 2026
            assertThat(quarters.get(2).getEndDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        }

        @Test
        @DisplayName("Q4 should be current when date is 6 January")
        void q4ShouldBeCurrentOnJan6() {
            // Given - Date is 6 January 2026, within Q4
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2026, 1, 6));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q4 should be current
            assertThat(quarters.get(3).isCurrent()).isTrue();
        }

        @Test
        @DisplayName("Q4 should still be FUTURE on 5 January")
        void q4ShouldBeFutureOnJan5() {
            // Given - Date is 5 January 2026, last day of Q3
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2026, 1, 5));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q4 should be FUTURE (hasn't started yet)
            assertThat(quarters.get(3).getStatus()).isEqualTo(QuarterStatus.FUTURE);
        }
    }

    // =========================================================================
    // TC-QU-016: No Financial Data Available
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-016: No Financial Data Available")
    class NoFinancialDataTests {

        @Test
        @DisplayName("Should handle quarter with no income or expense data")
        void shouldHandleQuarterWithNoData() {
            // Given - Service returns null for all data
            when(incomeService.getTotalByQuarter(any(), any(), any())).thenReturn(null);
            when(expenseService.getDeductibleTotalByQuarter(any(), any(), any())).thenReturn(null);
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 8, 15)); // Q2 is current
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q1 (past) should have null data but not throw
            assertThat(quarters.get(0).getTotalIncome()).isNull();
            assertThat(quarters.get(0).getTotalExpenses()).isNull();
            assertThat(quarters.get(0).getFormattedIncome()).isEqualTo("--");
            assertThat(quarters.get(0).getFormattedExpenses()).isEqualTo("--");
        }

        @Test
        @DisplayName("Should handle quarter with zero income and expenses")
        void shouldHandleQuarterWithZeroAmounts() {
            // Given - Service returns zero
            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenReturn(BigDecimal.ZERO);
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 8, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(quarters.get(0).getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(quarters.get(0).getNetProfitLoss()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(quarters.get(0).getFormattedIncome()).isEqualTo("\u00A30.00");
        }
    }

    // =========================================================================
    // TC-QU-017: Service Error Handling
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-017: Service Error Handling")
    class ServiceErrorHandlingTests {

        @Test
        @DisplayName("Should handle IncomeService throwing exception")
        void shouldHandleIncomeServiceException() {
            // Given - Income service throws exception for Q1
            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenThrow(new RuntimeException("Database connection failed"));
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 8, 15)); // Q2 is current
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Should not crash, Q1 should show "--"
            assertThat(quarters).hasSize(4);
            assertThat(quarters.get(0).getTotalIncome()).isNull();
            assertThat(quarters.get(0).getFormattedIncome()).isEqualTo("--");
        }

        @Test
        @DisplayName("Should handle ExpenseService throwing exception")
        void shouldHandleExpenseServiceException() {
            // Given - Expense service throws exception for Q1
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenThrow(new RuntimeException("Database connection failed"));
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 8, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Should not crash
            assertThat(quarters).hasSize(4);
        }

        @Test
        @DisplayName("Other quarters should load even if one quarter fails")
        void otherQuartersShouldLoadEvenIfOneQuarterFails() {
            // Given - Q1 fails but Q2 succeeds
            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenThrow(new RuntimeException("Database error"));
            when(incomeService.getTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q2)))
                .thenReturn(Q2_INCOME);
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q1)))
                .thenThrow(new RuntimeException("Database error"));
            when(expenseService.getDeductibleTotalByQuarter(eq(businessId), eq(taxYear202526), eq(Quarter.Q2)))
                .thenReturn(Q2_EXPENSES);
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15)); // Q3 is current
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q1 should have null data but Q2 should have valid data
            assertThat(quarters.get(0).getTotalIncome()).isNull();
            assertThat(quarters.get(1).getTotalIncome()).isEqualByComparingTo(Q2_INCOME);
        }
    }

    // =========================================================================
    // TC-QU-018: Accessible Text for Screen Readers
    // =========================================================================
    @Nested
    @DisplayName("TC-QU-018: Accessible Text for Screen Readers")
    class AccessibleTextTests {

        @Test
        @DisplayName("Accessible text should include quarter label")
        void accessibleTextShouldIncludeQuarterLabel() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getAccessibleText()).contains("Q1 2025/26");
            assertThat(quarters.get(1).getAccessibleText()).contains("Q2 2025/26");
            assertThat(quarters.get(2).getAccessibleText()).contains("Q3 2025/26");
            assertThat(quarters.get(3).getAccessibleText()).contains("Q4 2025/26");
        }

        @Test
        @DisplayName("Accessible text should include status")
        void accessibleTextShouldIncludeStatus() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15)); // Q3 current
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(2).getAccessibleText()).contains("Draft");
            assertThat(quarters.get(3).getAccessibleText()).contains("Future");
        }

        @Test
        @DisplayName("Accessible text should include deadline")
        void accessibleTextShouldIncludeDeadline() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(0).getAccessibleText()).contains("Deadline: 7 Aug 2025");
        }

        @Test
        @DisplayName("Accessible text should indicate current quarter")
        void accessibleTextShouldIndicateCurrentQuarter() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15)); // Q3 current
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then
            assertThat(quarters.get(2).getAccessibleText()).contains("current quarter");
            assertThat(quarters.get(0).getAccessibleText()).doesNotContain("current quarter");
        }

        @Test
        @DisplayName("Accessible text format should be screen-reader friendly")
        void accessibleTextFormatShouldBeScreenReaderFriendly() {
            // Given
            setupStandardTestData();
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 10, 15));
            controller.setTaxYear(taxYear202526);

            // When
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();
            String accessibleText = quarters.get(2).getAccessibleText();

            // Then - Format: "Q3 2025/26, current quarter, Draft, income £X, expenses £Y, net £Z, Deadline: D Mon YYYY"
            assertThat(accessibleText)
                .startsWith("Q3 2025/26")
                .contains("current quarter")
                .contains("Draft")
                .contains("income")
                .contains("expenses")
                .contains("net")
                .contains("Deadline");
        }
    }

    // =========================================================================
    // Additional Integration Tests
    // =========================================================================
    @Nested
    @DisplayName("Integration: Quarter Label Formatting")
    class QuarterLabelFormattingTests {

        @Test
        @DisplayName("All quarter labels should be correctly formatted")
        void allQuarterLabelsShouldBeCorrectlyFormatted() {
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
    }

    @Nested
    @DisplayName("Integration: Tax Year Change")
    class TaxYearChangeTests {

        @Test
        @DisplayName("Should refresh data when tax year changes")
        void shouldRefreshDataWhenTaxYearChanges() {
            // Given - Start with 2025/26
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);
            List<QuarterViewModel> firstQuarters = controller.getQuarterViewModels();
            assertThat(firstQuarters.get(0).getLabel()).isEqualTo("Q1 2025/26");

            // When - Change to 2024/25
            TaxYear taxYear202425 = TaxYear.of(2024);
            controller.setTaxYear(taxYear202425);
            List<QuarterViewModel> secondQuarters = controller.getQuarterViewModels();

            // Then - Labels should reflect new tax year
            assertThat(secondQuarters.get(0).getLabel()).isEqualTo("Q1 2024/25");
        }

        @Test
        @DisplayName("Quarter dates should update when tax year changes")
        void quarterDatesShouldUpdateWhenTaxYearChanges() {
            // Given
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear202526);

            // When
            TaxYear taxYear202425 = TaxYear.of(2024);
            controller.setTaxYear(taxYear202425);
            List<QuarterViewModel> quarters = controller.getQuarterViewModels();

            // Then - Q1 of 2024/25 starts 6 April 2024
            assertThat(quarters.get(0).getStartDate()).isEqualTo(LocalDate.of(2024, 4, 6));
            assertThat(quarters.get(0).getDeadline()).isEqualTo(LocalDate.of(2024, 8, 7));
        }
    }

    @Nested
    @DisplayName("Integration: Status Transitions")
    class StatusTransitionTests {

        @Test
        @DisplayName("Status should transition from FUTURE to DRAFT when quarter starts")
        void statusShouldTransitionFromFutureToDraftWhenQuarterStarts() {
            // Given - Q2 is future on 15 June
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 6, 15));
            controller.setTaxYear(taxYear202526);
            List<QuarterViewModel> quartersBefore = controller.getQuarterViewModels();
            assertThat(quartersBefore.get(1).getStatus()).isEqualTo(QuarterStatus.FUTURE); // Q2 future

            // When - Q2 starts on 6 July
            controller.setClock(fixedClockAt(2025, 7, 10));
            controller.setTaxYear(taxYear202526); // Refresh

            // Then - Q2 should now be DRAFT
            List<QuarterViewModel> quartersAfter = controller.getQuarterViewModels();
            assertThat(quartersAfter.get(1).getStatus()).isEqualTo(QuarterStatus.DRAFT);
        }

        @Test
        @DisplayName("Status should transition from DRAFT to OVERDUE when deadline passes")
        void statusShouldTransitionFromDraftToOverdueWhenDeadlinePasses() {
            // Given - Before Q1 deadline (7 Aug)
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setClock(fixedClockAt(2025, 8, 1));
            controller.setTaxYear(taxYear202526);
            List<QuarterViewModel> quartersBefore = controller.getQuarterViewModels();
            // Q1 ended 5 July, deadline 7 Aug - on 1 Aug it should be DRAFT
            assertThat(quartersBefore.get(0).getStatus()).isEqualTo(QuarterStatus.DRAFT);

            // When - After Q1 deadline
            controller.setClock(fixedClockAt(2025, 8, 10));
            controller.setTaxYear(taxYear202526); // Refresh

            // Then - Q1 should now be OVERDUE
            List<QuarterViewModel> quartersAfter = controller.getQuarterViewModels();
            assertThat(quartersAfter.get(0).getStatus()).isEqualTo(QuarterStatus.OVERDUE);
        }
    }
}
