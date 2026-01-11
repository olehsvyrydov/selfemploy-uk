package uk.selfemploy.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.dto.PeriodicUpdate;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.core.auth.TokenException;
import uk.selfemploy.core.auth.TokenProvider;
import uk.selfemploy.core.exception.SubmissionException;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.hmrc.client.MtdPeriodicUpdateClient;
import uk.selfemploy.hmrc.client.dto.HmrcSubmissionResponse;
import uk.selfemploy.hmrc.exception.HmrcApiException;
import uk.selfemploy.hmrc.exception.HmrcValidationException;
import uk.selfemploy.persistence.repository.ExpenseRepository;
import uk.selfemploy.persistence.repository.IncomeRepository;
import uk.selfemploy.persistence.repository.SubmissionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * TDD tests for QuarterlySubmissionService.
 * Tests cover all acceptance criteria for SE-402.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuarterlySubmissionService")
class QuarterlySubmissionServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private MtdPeriodicUpdateClient mtdClient;

    @Mock
    private TokenProvider tokenProvider;

    private QuarterlySubmissionService service;

    private UUID businessId;
    private TaxYear taxYear2025;
    private String nino;

    private static final String TEST_BEARER_TOKEN = "Bearer test-access-token";

    @BeforeEach
    void setUp() {
        service = new QuarterlySubmissionService(
                incomeRepository,
                expenseRepository,
                submissionRepository,
                mtdClient,
                tokenProvider
        );

        businessId = UUID.randomUUID();
        taxYear2025 = TaxYear.of(2025); // 2025/26 tax year
        nino = "AB123456C";

        // Default behavior: token provider returns valid token (lenient for tests that don't use it)
        lenient().when(tokenProvider.getValidToken()).thenReturn(TEST_BEARER_TOKEN);
    }

    @Nested
    @DisplayName("AC-1: Quarter Selection")
    class QuarterSelectionTests {

        @Test
        @DisplayName("Should calculate Q1 period dates correctly (6 Apr - 5 Jul)")
        void shouldCalculateQ1DatesCorrectly() {
            // Given
            Quarter quarter = Quarter.Q1;

            // When
            LocalDate startDate = quarter.getStartDate(taxYear2025);
            LocalDate endDate = quarter.getEndDate(taxYear2025);

            // Then
            assertThat(startDate).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(endDate).isEqualTo(LocalDate.of(2025, 7, 5));
        }

        @Test
        @DisplayName("Should calculate Q2 period dates correctly (6 Jul - 5 Oct)")
        void shouldCalculateQ2DatesCorrectly() {
            // Given
            Quarter quarter = Quarter.Q2;

            // When
            LocalDate startDate = quarter.getStartDate(taxYear2025);
            LocalDate endDate = quarter.getEndDate(taxYear2025);

            // Then
            assertThat(startDate).isEqualTo(LocalDate.of(2025, 7, 6));
            assertThat(endDate).isEqualTo(LocalDate.of(2025, 10, 5));
        }

        @Test
        @DisplayName("Should calculate Q3 period dates correctly (6 Oct - 5 Jan)")
        void shouldCalculateQ3DatesCorrectly() {
            // Given
            Quarter quarter = Quarter.Q3;

            // When
            LocalDate startDate = quarter.getStartDate(taxYear2025);
            LocalDate endDate = quarter.getEndDate(taxYear2025);

            // Then
            assertThat(startDate).isEqualTo(LocalDate.of(2025, 10, 6));
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 1, 5));
        }

        @Test
        @DisplayName("Should calculate Q4 period dates correctly (6 Jan - 5 Apr)")
        void shouldCalculateQ4DatesCorrectly() {
            // Given
            Quarter quarter = Quarter.Q4;

            // When
            LocalDate startDate = quarter.getStartDate(taxYear2025);
            LocalDate endDate = quarter.getEndDate(taxYear2025);

            // Then
            assertThat(startDate).isEqualTo(LocalDate.of(2026, 1, 6));
            assertThat(endDate).isEqualTo(LocalDate.of(2026, 4, 5));
        }
    }

    @Nested
    @DisplayName("AC-2: Cumulative Totals Calculation")
    class CumulativeTotalsTests {

        @Test
        @DisplayName("Should calculate cumulative income from tax year start to Q1 end")
        void shouldCalculateCumulativeIncomeForQ1() {
            // Given
            List<Income> incomes = List.of(
                    createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 4, 10)),
                    createIncome(new BigDecimal("2000.00"), LocalDate.of(2025, 5, 15)),
                    createIncome(new BigDecimal("1500.00"), LocalDate.of(2025, 6, 20))
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(incomes);
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, Quarter.Q1))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF123", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission submission = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            assertThat(submission.totalIncome()).isEqualByComparingTo(new BigDecimal("4500.00"));
        }

        @Test
        @DisplayName("Should calculate cumulative totals from tax year start to Q2 end")
        void shouldCalculateCumulativeTotalsForQ2() {
            // Given - Q1 + Q2 data
            List<Income> incomes = List.of(
                    createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 4, 10)), // Q1
                    createIncome(new BigDecimal("2000.00"), LocalDate.of(2025, 7, 15)), // Q2
                    createIncome(new BigDecimal("1500.00"), LocalDate.of(2025, 9, 20))  // Q2
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(incomes);
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, Quarter.Q2))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF456", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission submission = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q2);

            // Then - cumulative from tax year start
            assertThat(submission.totalIncome()).isEqualByComparingTo(new BigDecimal("4500.00"));
        }

        @Test
        @DisplayName("Should calculate expenses by category for MTD submission")
        void shouldCalculateExpensesByCategory() {
            // Given
            List<Expense> expenses = List.of(
                    createExpense(new BigDecimal("100.00"), ExpenseCategory.TRAVEL),
                    createExpense(new BigDecimal("200.00"), ExpenseCategory.TRAVEL),
                    createExpense(new BigDecimal("50.00"), ExpenseCategory.OFFICE_COSTS),
                    createExpense(new BigDecimal("75.00"), ExpenseCategory.PROFESSIONAL_FEES)
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(expenses);
            when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, Quarter.Q1))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF789", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then - capture and verify PeriodicUpdate
            ArgumentCaptor<PeriodicUpdate> updateCaptor = ArgumentCaptor.forClass(PeriodicUpdate.class);
            verify(mtdClient).submitPeriodicUpdate(any(), any(), any(), updateCaptor.capture());

            PeriodicUpdate update = updateCaptor.getValue();
            assertThat(update.periodExpenses().travelCosts()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(update.periodExpenses().adminCosts()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(update.periodExpenses().professionalFees()).isEqualByComparingTo(new BigDecimal("75.00"));
        }
    }

    @Nested
    @DisplayName("AC-3: Submission Content")
    class SubmissionContentTests {

        @Test
        @DisplayName("Should include total income in submission")
        void shouldIncludeTotalIncome() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("5000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createExpense(new BigDecimal("1000.00"), ExpenseCategory.OFFICE_COSTS)));
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            ArgumentCaptor<PeriodicUpdate> captor = ArgumentCaptor.forClass(PeriodicUpdate.class);
            verify(mtdClient).submitPeriodicUpdate(any(), any(), any(), captor.capture());

            assertThat(captor.getValue().periodIncome().getTotal()).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("Should include expenses by SA103 categories in submission")
        void shouldIncludeExpensesByCategory() {
            // Given
            List<Expense> expenses = List.of(
                    createExpense(new BigDecimal("100.00"), ExpenseCategory.COST_OF_GOODS),
                    createExpense(new BigDecimal("200.00"), ExpenseCategory.STAFF_COSTS),
                    createExpense(new BigDecimal("150.00"), ExpenseCategory.PREMISES)
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(expenses);
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            ArgumentCaptor<PeriodicUpdate> captor = ArgumentCaptor.forClass(PeriodicUpdate.class);
            verify(mtdClient).submitPeriodicUpdate(any(), any(), any(), captor.capture());

            PeriodicUpdate update = captor.getValue();
            assertThat(update.periodExpenses().costOfGoodsBought()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(update.periodExpenses().staffCosts()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(update.periodExpenses().premisesRunningCosts()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should calculate net profit correctly")
        void shouldCalculateNetProfit() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("10000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createExpense(new BigDecimal("3500.00"), ExpenseCategory.OFFICE_COSTS)));
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission submission = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            assertThat(submission.netProfit()).isEqualByComparingTo(new BigDecimal("6500.00"));
        }
    }

    @Nested
    @DisplayName("AC-6: Submission Response Handling")
    class ResponseHandlingTests {

        @Test
        @DisplayName("Should return submission with ACCEPTED status on success")
        void shouldReturnAcceptedSubmission() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("HMRC-REF-123", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission submission = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            assertThat(submission.status()).isEqualTo(SubmissionStatus.ACCEPTED);
            assertThat(submission.hmrcReference()).isEqualTo("HMRC-REF-123");
        }

        @Test
        @DisplayName("Should save submission with REJECTED status on HMRC validation error")
        void shouldSaveRejectedSubmissionOnValidationError() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenThrow(new HmrcValidationException("Date is invalid", "INVALID_DATE", 400));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("HMRC validation failed");

            // Verify rejected submission was saved
            ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
            verify(submissionRepository).save(captor.capture());
            assertThat(captor.getValue().status()).isEqualTo(SubmissionStatus.REJECTED);
            assertThat(captor.getValue().errorMessage()).contains("INVALID_DATE");
        }
    }

    @Nested
    @DisplayName("AC-7: Submission History")
    class SubmissionHistoryTests {

        @Test
        @DisplayName("Should save successful submission to history with HMRC reference")
        void shouldSaveSuccessfulSubmissionWithReference() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("5000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createExpense(new BigDecimal("1000.00"), ExpenseCategory.OFFICE_COSTS)));
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("HMRC-2025-Q1-ABC", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission submission = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            verify(submissionRepository).save(any(Submission.class));

            assertThat(submission.id()).isNotNull();
            assertThat(submission.businessId()).isEqualTo(businessId);
            assertThat(submission.type()).isEqualTo(SubmissionType.QUARTERLY_Q1);
            assertThat(submission.taxYear()).isEqualTo(taxYear2025);
            assertThat(submission.hmrcReference()).isEqualTo("HMRC-2025-Q1-ABC");
            assertThat(submission.totalIncome()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(submission.totalExpenses()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(submission.netProfit()).isEqualByComparingTo(new BigDecimal("4000.00"));
        }

        @Test
        @DisplayName("Should include correct period dates in saved submission")
        void shouldIncludePeriodDatesInSubmission() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission submission = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q2);

            // Then
            assertThat(submission.periodStart()).isEqualTo(LocalDate.of(2025, 7, 6));
            assertThat(submission.periodEnd()).isEqualTo(LocalDate.of(2025, 10, 5));
        }
    }

    @Nested
    @DisplayName("AC-8: Duplicate Submission Prevention")
    class DuplicatePreventionTests {

        @Test
        @DisplayName("Should reject submission if quarter already submitted")
        void shouldRejectDuplicateSubmission() {
            // Given
            when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, Quarter.Q1))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("already been submitted");

            verify(mtdClient, never()).submitPeriodicUpdate(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should allow submission for different quarter in same tax year")
        void shouldAllowDifferentQuarterSubmission() {
            // Given - Q2 submission (Q1 already submitted)
            when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, Quarter.Q2))
                    .thenReturn(false);
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 8, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF-Q2", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission submission = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q2);

            // Then
            assertThat(submission.type()).isEqualTo(SubmissionType.QUARTERLY_Q2);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject null business ID")
        void shouldRejectNullBusinessId() {
            assertThatThrownBy(() -> service.submitQuarter(null, nino, taxYear2025, Quarter.Q1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business ID");
        }

        @Test
        @DisplayName("Should reject null NINO")
        void shouldRejectNullNino() {
            assertThatThrownBy(() -> service.submitQuarter(businessId, null, taxYear2025, Quarter.Q1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("NINO");
        }

        @Test
        @DisplayName("Should reject null tax year")
        void shouldRejectNullTaxYear() {
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, null, Quarter.Q1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Tax year");
        }

        @Test
        @DisplayName("Should reject null quarter")
        void shouldRejectNullQuarter() {
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, taxYear2025, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Quarter");
        }
    }

    @Nested
    @DisplayName("PeriodicUpdate Building Tests")
    class PeriodicUpdateBuildingTests {

        @Test
        @DisplayName("Should build PeriodicUpdate with correct period dates")
        void shouldBuildPeriodicUpdateWithCorrectDates() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            ArgumentCaptor<PeriodicUpdate> captor = ArgumentCaptor.forClass(PeriodicUpdate.class);
            verify(mtdClient).submitPeriodicUpdate(any(), any(), any(), captor.capture());

            PeriodicUpdate update = captor.getValue();
            assertThat(update.periodFromDate()).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(update.periodToDate()).isEqualTo(LocalDate.of(2025, 7, 5));
        }

        @Test
        @DisplayName("Should map all expense categories to MTD fields")
        void shouldMapAllExpenseCategories() {
            // Given - all expense categories
            List<Expense> expenses = List.of(
                    createExpense(new BigDecimal("100.00"), ExpenseCategory.COST_OF_GOODS),
                    createExpense(new BigDecimal("200.00"), ExpenseCategory.STAFF_COSTS),
                    createExpense(new BigDecimal("300.00"), ExpenseCategory.TRAVEL),
                    createExpense(new BigDecimal("400.00"), ExpenseCategory.PREMISES),
                    createExpense(new BigDecimal("500.00"), ExpenseCategory.REPAIRS),
                    createExpense(new BigDecimal("600.00"), ExpenseCategory.OFFICE_COSTS),
                    createExpense(new BigDecimal("700.00"), ExpenseCategory.ADVERTISING),
                    createExpense(new BigDecimal("800.00"), ExpenseCategory.INTEREST),
                    createExpense(new BigDecimal("900.00"), ExpenseCategory.FINANCIAL_CHARGES),
                    createExpense(new BigDecimal("1000.00"), ExpenseCategory.PROFESSIONAL_FEES),
                    createExpense(new BigDecimal("1100.00"), ExpenseCategory.OTHER_EXPENSES)
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("50000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(expenses);
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            ArgumentCaptor<PeriodicUpdate> captor = ArgumentCaptor.forClass(PeriodicUpdate.class);
            verify(mtdClient).submitPeriodicUpdate(any(), any(), any(), captor.capture());

            PeriodicUpdate.PeriodExpenses exp = captor.getValue().periodExpenses();
            assertThat(exp.costOfGoodsBought()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(exp.staffCosts()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(exp.travelCosts()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(exp.premisesRunningCosts()).isEqualByComparingTo(new BigDecimal("400.00"));
            assertThat(exp.maintenanceCosts()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(exp.adminCosts()).isEqualByComparingTo(new BigDecimal("600.00"));
            assertThat(exp.advertisingCosts()).isEqualByComparingTo(new BigDecimal("700.00"));
            assertThat(exp.interest()).isEqualByComparingTo(new BigDecimal("800.00"));
            assertThat(exp.financialCharges()).isEqualByComparingTo(new BigDecimal("900.00"));
            assertThat(exp.professionalFees()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(exp.other()).isEqualByComparingTo(new BigDecimal("1100.00"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should wrap HMRC API exception in SubmissionException")
        void shouldWrapHmrcApiException() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenThrow(new HmrcApiException("Server error"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1))
                    .isInstanceOf(SubmissionException.class)
                    .hasCauseInstanceOf(HmrcApiException.class);
        }

        @Test
        @DisplayName("Should save failed submission with error message")
        void shouldSaveFailedSubmissionWithError() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenThrow(new HmrcApiException("Connection timeout"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1))
                    .isInstanceOf(SubmissionException.class);

            // Verify failed submission was saved
            ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
            verify(submissionRepository).save(captor.capture());

            Submission saved = captor.getValue();
            assertThat(saved.status()).isEqualTo(SubmissionStatus.REJECTED);
            assertThat(saved.errorMessage()).contains("Connection timeout");
        }
    }

    @Nested
    @DisplayName("Token Integration (TD-001)")
    class TokenIntegrationTests {

        @Test
        @DisplayName("should use TokenProvider for HMRC API calls")
        void shouldUseTokenProviderForApiCalls() {
            // Given
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(eq(nino), any(), eq(TEST_BEARER_TOKEN), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF123", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            verify(tokenProvider).getValidToken();
            verify(mtdClient).submitPeriodicUpdate(eq(nino), any(), eq(TEST_BEARER_TOKEN), any());
        }

        @Test
        @DisplayName("should propagate TokenException when no token available")
        void shouldPropagateTokenExceptionWhenNoToken() {
            // Given
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(tokenProvider.getValidToken())
                    .thenThrow(new TokenException(TokenException.TokenError.NO_TOKEN));

            // When / Then
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1))
                    .isInstanceOf(TokenException.class)
                    .satisfies(ex -> {
                        TokenException tokenEx = (TokenException) ex;
                        assertThat(tokenEx.getError()).isEqualTo(TokenException.TokenError.NO_TOKEN);
                        assertThat(tokenEx.requiresReauthentication()).isTrue();
                    });

            // Verify HMRC API was NOT called
            verify(mtdClient, never()).submitPeriodicUpdate(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should propagate TokenException when refresh fails")
        void shouldPropagateTokenExceptionWhenRefreshFails() {
            // Given
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(tokenProvider.getValidToken())
                    .thenThrow(new TokenException(TokenException.TokenError.REFRESH_FAILED));

            // When / Then
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1))
                    .isInstanceOf(TokenException.class)
                    .satisfies(ex -> {
                        TokenException tokenEx = (TokenException) ex;
                        assertThat(tokenEx.getError()).isEqualTo(TokenException.TokenError.REFRESH_FAILED);
                        assertThat(tokenEx.getUserMessage()).contains("re-authenticate");
                    });
        }
    }

    // Helper methods
    private Income createIncome(BigDecimal amount, LocalDate date) {
        return new Income(
                UUID.randomUUID(),
                businessId,
                date,
                amount,
                "Test income",
                IncomeCategory.SALES,
                null
        );
    }

    private Expense createExpense(BigDecimal amount, ExpenseCategory category) {
        return new Expense(
                UUID.randomUUID(),
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
