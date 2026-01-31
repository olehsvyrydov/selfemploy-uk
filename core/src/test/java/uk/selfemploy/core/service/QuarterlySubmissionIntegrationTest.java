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
import uk.selfemploy.core.exception.SubmissionException;
import uk.selfemploy.core.auth.TokenProvider;
import uk.selfemploy.hmrc.client.MtdPeriodicUpdateClient;
import uk.selfemploy.hmrc.client.dto.HmrcSubmissionResponse;
import uk.selfemploy.hmrc.exception.HmrcServerException;
import uk.selfemploy.hmrc.exception.HmrcValidationException;
import uk.selfemploy.persistence.repository.ExpenseRepository;
import uk.selfemploy.persistence.repository.IncomeRepository;
import uk.selfemploy.persistence.repository.SubmissionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Integration tests for SE-402 Quarterly MTD Submission.
 * Tests based on /rob's QA test specification.
 *
 * Test IDs: IT-402-001 to IT-402-023
 *
 * @see docs/sprints/sprint-4/testing/rob-qa-SE-402.md
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SE-402 Integration Tests")
class QuarterlySubmissionIntegrationTest {

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

        // Setup default token provider behavior (lenient for tests that don't use it)
        lenient().when(tokenProvider.getValidToken()).thenReturn("Bearer test-token");
    }

    @Nested
    @DisplayName("P0: Quarter Selection Tests (TC-402-001 to TC-402-004)")
    class QuarterSelectionIntegrationTests {

        @Test
        @DisplayName("IT-402-001: Q1 submission uses correct period dates (6 Apr - 5 Jul)")
        void q1SubmissionUsesCorrectPeriodDates() {
            // Given
            setupSuccessfulSubmission();

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
        @DisplayName("IT-402-002: Q2 submission uses correct period dates (6 Jul - 5 Oct)")
        void q2SubmissionUsesCorrectPeriodDates() {
            // Given
            setupSuccessfulSubmissionForQuarter(Quarter.Q2);

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q2);

            // Then
            ArgumentCaptor<PeriodicUpdate> captor = ArgumentCaptor.forClass(PeriodicUpdate.class);
            verify(mtdClient).submitPeriodicUpdate(any(), any(), any(), captor.capture());

            PeriodicUpdate update = captor.getValue();
            assertThat(update.periodFromDate()).isEqualTo(LocalDate.of(2025, 7, 6));
            assertThat(update.periodToDate()).isEqualTo(LocalDate.of(2025, 10, 5));
        }

        @Test
        @DisplayName("IT-402-003: Q3 submission uses correct period dates (6 Oct - 5 Jan)")
        void q3SubmissionUsesCorrectPeriodDates() {
            // Given
            setupSuccessfulSubmissionForQuarter(Quarter.Q3);

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q3);

            // Then
            ArgumentCaptor<PeriodicUpdate> captor = ArgumentCaptor.forClass(PeriodicUpdate.class);
            verify(mtdClient).submitPeriodicUpdate(any(), any(), any(), captor.capture());

            PeriodicUpdate update = captor.getValue();
            assertThat(update.periodFromDate()).isEqualTo(LocalDate.of(2025, 10, 6));
            assertThat(update.periodToDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        }

        @Test
        @DisplayName("IT-402-004: Q4 submission uses correct period dates (6 Jan - 5 Apr)")
        void q4SubmissionUsesCorrectPeriodDates() {
            // Given
            setupSuccessfulSubmissionForQuarter(Quarter.Q4);

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q4);

            // Then
            ArgumentCaptor<PeriodicUpdate> captor = ArgumentCaptor.forClass(PeriodicUpdate.class);
            verify(mtdClient).submitPeriodicUpdate(any(), any(), any(), captor.capture());

            PeriodicUpdate update = captor.getValue();
            assertThat(update.periodFromDate()).isEqualTo(LocalDate.of(2026, 1, 6));
            assertThat(update.periodToDate()).isEqualTo(LocalDate.of(2026, 4, 5));
        }
    }

    @Nested
    @DisplayName("P0: Cumulative Totals Tests (TC-402-005)")
    class CumulativeTotalsIntegrationTests {

        @Test
        @DisplayName("IT-402-005: Q1 cumulative = Q1 only (£10,000 income, £2,000 expenses)")
        void q1CumulativeEqualsQ1Only() {
            // Given - Q1 data only
            List<Income> q1Incomes = List.of(
                    createIncome(new BigDecimal("6000.00"), LocalDate.of(2025, 5, 1)),
                    createIncome(new BigDecimal("4000.00"), LocalDate.of(2025, 6, 15))
            );
            List<Expense> q1Expenses = List.of(
                    createExpense(new BigDecimal("1200.00"), ExpenseCategory.TRAVEL, LocalDate.of(2025, 5, 10)),
                    createExpense(new BigDecimal("800.00"), ExpenseCategory.OFFICE_COSTS, LocalDate.of(2025, 6, 20))
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(q1Incomes);
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(q1Expenses);
            when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, Quarter.Q1))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("MTD-Q1-2025-001", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission result = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(result.totalExpenses()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(result.netProfit()).isEqualByComparingTo(new BigDecimal("8000.00"));
        }

        @Test
        @DisplayName("IT-402-006: Q2 cumulative = Q1 + Q2 (£18,000 income, £3,000 expenses)")
        void q2CumulativeEqualsQ1PlusQ2() {
            // Given - Q1 + Q2 data (cumulative from tax year start)
            List<Income> cumulativeIncomes = List.of(
                    // Q1 data
                    createIncome(new BigDecimal("6000.00"), LocalDate.of(2025, 5, 1)),
                    createIncome(new BigDecimal("4000.00"), LocalDate.of(2025, 6, 15)),
                    // Q2 data
                    createIncome(new BigDecimal("5000.00"), LocalDate.of(2025, 8, 1)),
                    createIncome(new BigDecimal("3000.00"), LocalDate.of(2025, 9, 15))
            );
            List<Expense> cumulativeExpenses = List.of(
                    // Q1 data
                    createExpense(new BigDecimal("1200.00"), ExpenseCategory.TRAVEL, LocalDate.of(2025, 5, 10)),
                    createExpense(new BigDecimal("800.00"), ExpenseCategory.OFFICE_COSTS, LocalDate.of(2025, 6, 20)),
                    // Q2 data
                    createExpense(new BigDecimal("700.00"), ExpenseCategory.TRAVEL, LocalDate.of(2025, 8, 10)),
                    createExpense(new BigDecimal("300.00"), ExpenseCategory.OFFICE_COSTS, LocalDate.of(2025, 9, 20))
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(cumulativeIncomes);
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(cumulativeExpenses);
            when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, Quarter.Q2))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("MTD-Q2-2025-001", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission result = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q2);

            // Then
            assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("18000.00"));
            assertThat(result.totalExpenses()).isEqualByComparingTo(new BigDecimal("3000.00"));
            assertThat(result.netProfit()).isEqualByComparingTo(new BigDecimal("15000.00"));
        }
    }

    @Nested
    @DisplayName("P0: Submission Content Tests (TC-402-009 to TC-402-011)")
    class SubmissionContentIntegrationTests {

        @Test
        @DisplayName("IT-402-009: Income mapped correctly to periodIncome.turnover")
        void incomeMappedCorrectly() {
            // Given
            List<Income> incomes = List.of(
                    createIncome(new BigDecimal("12500.00"), LocalDate.of(2025, 5, 15))
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(incomes);
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

            assertThat(captor.getValue().periodIncome().calculateTotal())
                    .isEqualByComparingTo(new BigDecimal("12500.00"));
        }

        @Test
        @DisplayName("IT-402-010: Expenses mapped by SA103F category to MTD fields")
        void expensesMappedBySA103Category() {
            // Given - expenses matching SA103F boxes
            List<Expense> expenses = List.of(
                    createExpense(new BigDecimal("1200.00"), ExpenseCategory.COST_OF_GOODS, LocalDate.of(2025, 5, 1)),     // Box 17
                    createExpense(new BigDecimal("350.00"), ExpenseCategory.TRAVEL, LocalDate.of(2025, 5, 5)),             // Box 20
                    createExpense(new BigDecimal("800.00"), ExpenseCategory.PREMISES, LocalDate.of(2025, 5, 10)),          // Box 21
                    createExpense(new BigDecimal("250.00"), ExpenseCategory.OFFICE_COSTS, LocalDate.of(2025, 5, 15)),      // Box 23
                    createExpense(new BigDecimal("150.00"), ExpenseCategory.PROFESSIONAL_FEES, LocalDate.of(2025, 5, 20)), // Box 28
                    createExpense(new BigDecimal("100.00"), ExpenseCategory.OTHER_EXPENSES, LocalDate.of(2025, 5, 25))     // Box 30
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("10000.00"), LocalDate.of(2025, 5, 1))));
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
            assertThat(exp.costOfGoodsBought()).isEqualByComparingTo(new BigDecimal("1200.00"));
            assertThat(exp.travelCosts()).isEqualByComparingTo(new BigDecimal("350.00"));
            assertThat(exp.premisesRunningCosts()).isEqualByComparingTo(new BigDecimal("800.00"));
            assertThat(exp.adminCosts()).isEqualByComparingTo(new BigDecimal("250.00"));
            assertThat(exp.professionalFees()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(exp.other()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("IT-402-011: Net profit calculated correctly (income - expenses)")
        void netProfitCalculatedCorrectly() {
            // Given
            List<Income> incomes = List.of(
                    createIncome(new BigDecimal("12500.00"), LocalDate.of(2025, 5, 15))
            );
            List<Expense> expenses = List.of(
                    createExpense(new BigDecimal("2850.00"), ExpenseCategory.OFFICE_COSTS, LocalDate.of(2025, 5, 20))
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(incomes);
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(expenses);
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission result = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            assertThat(result.netProfit()).isEqualByComparingTo(new BigDecimal("9650.00"));
        }
    }

    @Nested
    @DisplayName("P0: Response Handling Tests (TC-402-014, TC-402-015)")
    class ResponseHandlingIntegrationTests {

        @Test
        @DisplayName("IT-402-014: HMRC 201 Created - returns ACCEPTED with reference")
        void hmrc201CreatedReturnsAccepted() {
            // Given
            setupSuccessfulSubmission();
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("MTD-Q1-2025-ABC123", "ACCEPTED"));

            // When
            Submission result = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            assertThat(result.status()).isEqualTo(SubmissionStatus.ACCEPTED);
            assertThat(result.hmrcReference()).isEqualTo("MTD-Q1-2025-ABC123");
        }

        @Test
        @DisplayName("IT-402-015: HMRC 400 Bad Request - returns REJECTED with validation error")
        void hmrc400BadRequestReturnsRejected() {
            // Given
            setupBasicSubmissionData();
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenThrow(new HmrcValidationException("Invalid date format", "INVALID_DATE", 400));

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
    @DisplayName("P0: History Persistence Tests (TC-402-019, TC-402-020)")
    class HistoryPersistenceIntegrationTests {

        @Test
        @DisplayName("IT-402-019: Successful submission saved with ACCEPTED status")
        void successfulSubmissionSavedWithAcceptedStatus() {
            // Given
            setupSuccessfulSubmission();
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("HMRC-REF-001", "ACCEPTED"));

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
            verify(submissionRepository).save(captor.capture());

            Submission saved = captor.getValue();
            assertThat(saved.status()).isEqualTo(SubmissionStatus.ACCEPTED);
            assertThat(saved.businessId()).isEqualTo(businessId);
            assertThat(saved.taxYear()).isEqualTo(taxYear2025);
            assertThat(saved.type()).isEqualTo(SubmissionType.QUARTERLY_Q1);
        }

        @Test
        @DisplayName("IT-402-020: HMRC reference stored in submission record")
        void hmrcReferenceStoredInSubmission() {
            // Given
            setupSuccessfulSubmission();
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("HMRC-Q1-2025-XYZ789", "ACCEPTED"));

            // When
            service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
            verify(submissionRepository).save(captor.capture());

            assertThat(captor.getValue().hmrcReference()).isEqualTo("HMRC-Q1-2025-XYZ789");
        }
    }

    @Nested
    @DisplayName("P0: Duplicate Prevention Tests (TC-402-021, TC-402-022)")
    class DuplicatePreventionIntegrationTests {

        @Test
        @DisplayName("IT-402-021: Block duplicate Q1 submission - throws SubmissionException")
        void blockDuplicateQ1Submission() {
            // Given
            when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, Quarter.Q1))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("already been submitted");

            // Verify no HMRC call was made
            verify(mtdClient, never()).submitPeriodicUpdate(any(), any(), any(), any());
        }

        @Test
        @DisplayName("IT-402-022: Allow Q2 submission after Q1 was submitted")
        void allowQ2AfterQ1Submitted() {
            // Given - Q2 not yet submitted
            when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, Quarter.Q2))
                    .thenReturn(false);
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of(createIncome(new BigDecimal("5000.00"), LocalDate.of(2025, 8, 1))));
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF-Q2", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission result = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q2);

            // Then
            assertThat(result.type()).isEqualTo(SubmissionType.QUARTERLY_Q2);
            verify(mtdClient).submitPeriodicUpdate(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("P1: Error Scenarios (TC-402-016, TC-402-017)")
    class ErrorScenariosIntegrationTests {

        @Test
        @DisplayName("IT-402-016: HMRC 503 Service Unavailable - throws retryable exception")
        void hmrc503ServiceUnavailable() {
            // Given
            setupBasicSubmissionData();
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenThrow(new HmrcServerException("Service Unavailable", "SERVER_ERROR", 503));

            // When/Then
            assertThatThrownBy(() -> service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1))
                    .isInstanceOf(SubmissionException.class)
                    .hasMessageContaining("HMRC submission failed");
        }

        @Test
        @DisplayName("IT-402-017: HMRC error details preserved in saved submission")
        void hmrcErrorDetailsPreserved() {
            // Given
            setupBasicSubmissionData();
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenThrow(new HmrcValidationException(
                            "Date is invalid",
                            "RULE_INVALID_DATE_RANGE",
                            400
                    ));

            // When
            try {
                service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);
            } catch (SubmissionException ignored) {
            }

            // Then
            ArgumentCaptor<Submission> captor = ArgumentCaptor.forClass(Submission.class);
            verify(submissionRepository).save(captor.capture());

            Submission saved = captor.getValue();
            assertThat(saved.status()).isEqualTo(SubmissionStatus.REJECTED);
            assertThat(saved.errorMessage()).contains("RULE_INVALID_DATE_RANGE");
            assertThat(saved.errorMessage()).contains("Date is invalid");
        }
    }

    @Nested
    @DisplayName("P2: Edge Cases (TC-402-028, TC-402-029)")
    class EdgeCasesIntegrationTests {

        @Test
        @DisplayName("IT-402-028: Zero income/expenses submission succeeds")
        void zeroIncomeExpensesSucceeds() {
            // Given - no income or expenses
            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF-ZERO", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission result = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            assertThat(result.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.totalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.netProfit()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        }

        @Test
        @DisplayName("IT-402-029: Large amounts submission succeeds (within DECIMAL(12,2))")
        void largeAmountsSucceeds() {
            // Given - large amounts
            List<Income> incomes = List.of(
                    createIncome(new BigDecimal("999999.99"), LocalDate.of(2025, 5, 1))
            );

            when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(incomes);
            when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                    .thenReturn(List.of());
            when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                    .thenReturn(false);
            when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                    .thenReturn(new HmrcSubmissionResponse("REF-LARGE", "ACCEPTED"));
            when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            Submission result = service.submitQuarter(businessId, nino, taxYear2025, Quarter.Q1);

            // Then
            assertThat(result.totalIncome()).isEqualByComparingTo(new BigDecimal("999999.99"));
            assertThat(result.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        }
    }

    // === Helper Methods ===

    private void setupSuccessfulSubmission() {
        setupSuccessfulSubmissionForQuarter(Quarter.Q1);
    }

    private void setupSuccessfulSubmissionForQuarter(Quarter quarter) {
        when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
        when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                .thenReturn(List.of());
        when(submissionRepository.existsQuarterlySubmission(businessId, taxYear2025, quarter))
                .thenReturn(false);
        when(mtdClient.submitPeriodicUpdate(any(), any(), any(), any()))
                .thenReturn(new HmrcSubmissionResponse("REF-" + quarter, "ACCEPTED"));
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setupBasicSubmissionData() {
        when(incomeRepository.findByDateRange(eq(businessId), any(), any()))
                .thenReturn(List.of(createIncome(new BigDecimal("1000.00"), LocalDate.of(2025, 5, 1))));
        when(expenseRepository.findByDateRange(eq(businessId), any(), any()))
                .thenReturn(List.of());
        when(submissionRepository.existsQuarterlySubmission(any(), any(), any()))
                .thenReturn(false);
        when(submissionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Income createIncome(BigDecimal amount, LocalDate date) {
        return new Income(
                UUID.randomUUID(),
                businessId,
                date,
                amount,
                "Test income",
                IncomeCategory.SALES,
                null,
                null,
                null,
                null
        );
    }

    private Expense createExpense(BigDecimal amount, ExpenseCategory category, LocalDate date) {
        return new Expense(
                UUID.randomUUID(),
                businessId,
                date,
                amount,
                "Test expense",
                category,
                null,
                null,
                null,
                null,
                null
        );
    }
}
