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
}
