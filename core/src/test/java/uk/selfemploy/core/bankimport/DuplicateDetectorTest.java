package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.persistence.repository.ExpenseRepository;
import uk.selfemploy.persistence.repository.IncomeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DuplicateDetector.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DuplicateDetector Tests")
class DuplicateDetectorTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    private DuplicateDetector detector;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final LocalDate BASE_DATE = LocalDate.of(2025, 6, 15);

    @BeforeEach
    void setUp() {
        detector = new DuplicateDetector(incomeRepository, expenseRepository);
    }

    @Nested
    @DisplayName("No Duplicates Tests")
    class NoDuplicatesTests {

        @Test
        @DisplayName("should return all transactions as unique when no existing transactions")
        void shouldReturnAllAsUniqueWhenNoExisting() {
            when(incomeRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());
            when(expenseRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            List<ImportedTransaction> imports = List.of(
                createTransaction(BASE_DATE, new BigDecimal("100.00"), "PAYMENT 1"),
                createTransaction(BASE_DATE, new BigDecimal("-50.00"), "EXPENSE 1")
            );

            DuplicateCheckResult result = detector.checkDuplicates(BUSINESS_ID, imports);

            assertThat(result.uniqueTransactions()).hasSize(2);
            assertThat(result.duplicateTransactions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Duplicate Detection Tests")
    class DuplicateDetectionTests {

        @Test
        @DisplayName("should detect exact income duplicate")
        void shouldDetectExactIncomeDuplicate() {
            Income existingIncome = new Income(
                UUID.randomUUID(),
                BUSINESS_ID,
                BASE_DATE,
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                IncomeCategory.SALES,
                null
            );

            when(incomeRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existingIncome));
            when(expenseRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            List<ImportedTransaction> imports = List.of(
                createTransaction(BASE_DATE, new BigDecimal("100.00"), "CLIENT PAYMENT"),
                createTransaction(BASE_DATE, new BigDecimal("200.00"), "OTHER PAYMENT")
            );

            DuplicateCheckResult result = detector.checkDuplicates(BUSINESS_ID, imports);

            assertThat(result.uniqueTransactions()).hasSize(1);
            assertThat(result.duplicateTransactions()).hasSize(1);
            assertThat(result.duplicateTransactions().get(0).description()).isEqualTo("CLIENT PAYMENT");
        }

        @Test
        @DisplayName("should detect exact expense duplicate")
        void shouldDetectExactExpenseDuplicate() {
            Expense existingExpense = new Expense(
                UUID.randomUUID(),
                BUSINESS_ID,
                BASE_DATE,
                new BigDecimal("50.00"),
                "AMAZON PURCHASE",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null
            );

            when(incomeRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());
            when(expenseRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existingExpense));

            List<ImportedTransaction> imports = List.of(
                createTransaction(BASE_DATE, new BigDecimal("-50.00"), "AMAZON PURCHASE")
            );

            DuplicateCheckResult result = detector.checkDuplicates(BUSINESS_ID, imports);

            assertThat(result.uniqueTransactions()).isEmpty();
            assertThat(result.duplicateTransactions()).hasSize(1);
        }

        @Test
        @DisplayName("should detect duplicate with normalized description")
        void shouldDetectDuplicateWithNormalizedDescription() {
            Income existingIncome = new Income(
                UUID.randomUUID(),
                BUSINESS_ID,
                BASE_DATE,
                new BigDecimal("100.00"),
                "Client Payment",
                IncomeCategory.SALES,
                null
            );

            when(incomeRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existingIncome));
            when(expenseRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            // Same transaction but with different case/whitespace
            List<ImportedTransaction> imports = List.of(
                createTransaction(BASE_DATE, new BigDecimal("100.00"), "  CLIENT   PAYMENT  ")
            );

            DuplicateCheckResult result = detector.checkDuplicates(BUSINESS_ID, imports);

            assertThat(result.uniqueTransactions()).isEmpty();
            assertThat(result.duplicateTransactions()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Non-Duplicate Cases Tests")
    class NonDuplicateCasesTests {

        @Test
        @DisplayName("should not detect as duplicate when date differs")
        void shouldNotDetectAsDuplicateWhenDateDiffers() {
            Income existingIncome = new Income(
                UUID.randomUUID(),
                BUSINESS_ID,
                BASE_DATE,
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                IncomeCategory.SALES,
                null
            );

            when(incomeRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existingIncome));
            when(expenseRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            // Same amount and description but different date
            List<ImportedTransaction> imports = List.of(
                createTransaction(BASE_DATE.plusDays(1), new BigDecimal("100.00"), "CLIENT PAYMENT")
            );

            DuplicateCheckResult result = detector.checkDuplicates(BUSINESS_ID, imports);

            assertThat(result.uniqueTransactions()).hasSize(1);
            assertThat(result.duplicateTransactions()).isEmpty();
        }

        @Test
        @DisplayName("should not detect as duplicate when amount differs")
        void shouldNotDetectAsDuplicateWhenAmountDiffers() {
            Income existingIncome = new Income(
                UUID.randomUUID(),
                BUSINESS_ID,
                BASE_DATE,
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                IncomeCategory.SALES,
                null
            );

            when(incomeRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existingIncome));
            when(expenseRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            // Same date and description but different amount
            List<ImportedTransaction> imports = List.of(
                createTransaction(BASE_DATE, new BigDecimal("100.01"), "CLIENT PAYMENT")
            );

            DuplicateCheckResult result = detector.checkDuplicates(BUSINESS_ID, imports);

            assertThat(result.uniqueTransactions()).hasSize(1);
            assertThat(result.duplicateTransactions()).isEmpty();
        }

        @Test
        @DisplayName("should not detect as duplicate when description differs")
        void shouldNotDetectAsDuplicateWhenDescriptionDiffers() {
            Income existingIncome = new Income(
                UUID.randomUUID(),
                BUSINESS_ID,
                BASE_DATE,
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                IncomeCategory.SALES,
                null
            );

            when(incomeRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existingIncome));
            when(expenseRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            // Same date and amount but different description
            List<ImportedTransaction> imports = List.of(
                createTransaction(BASE_DATE, new BigDecimal("100.00"), "ANOTHER CLIENT PAYMENT")
            );

            DuplicateCheckResult result = detector.checkDuplicates(BUSINESS_ID, imports);

            assertThat(result.uniqueTransactions()).hasSize(1);
            assertThat(result.duplicateTransactions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Duplicate Within Import Tests")
    class DuplicateWithinImportTests {

        @Test
        @DisplayName("should detect duplicates within the same import batch")
        void shouldDetectDuplicatesWithinBatch() {
            when(incomeRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());
            when(expenseRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(Collections.emptyList());

            // Two identical transactions in the import
            List<ImportedTransaction> imports = List.of(
                createTransaction(BASE_DATE, new BigDecimal("100.00"), "SAME PAYMENT"),
                createTransaction(BASE_DATE, new BigDecimal("100.00"), "SAME PAYMENT")
            );

            DuplicateCheckResult result = detector.checkDuplicates(BUSINESS_ID, imports);

            // First occurrence is unique, second is duplicate
            assertThat(result.uniqueTransactions()).hasSize(1);
            assertThat(result.duplicateTransactions()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Mixed Transactions Tests")
    class MixedTransactionsTests {

        @Test
        @DisplayName("should handle mix of income and expense duplicates")
        void shouldHandleMixedDuplicates() {
            Income existingIncome = new Income(
                UUID.randomUUID(),
                BUSINESS_ID,
                BASE_DATE,
                new BigDecimal("100.00"),
                "CLIENT PAYMENT",
                IncomeCategory.SALES,
                null
            );
            Expense existingExpense = new Expense(
                UUID.randomUUID(),
                BUSINESS_ID,
                BASE_DATE,
                new BigDecimal("50.00"),
                "AMAZON",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null
            );

            when(incomeRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existingIncome));
            when(expenseRepository.findByDateRange(eq(BUSINESS_ID), any(), any()))
                .thenReturn(List.of(existingExpense));

            List<ImportedTransaction> imports = List.of(
                createTransaction(BASE_DATE, new BigDecimal("100.00"), "CLIENT PAYMENT"),   // Duplicate income
                createTransaction(BASE_DATE, new BigDecimal("-50.00"), "AMAZON"),           // Duplicate expense
                createTransaction(BASE_DATE, new BigDecimal("200.00"), "NEW INCOME"),       // New
                createTransaction(BASE_DATE, new BigDecimal("-75.00"), "NEW EXPENSE")       // New
            );

            DuplicateCheckResult result = detector.checkDuplicates(BUSINESS_ID, imports);

            assertThat(result.uniqueTransactions()).hasSize(2);
            assertThat(result.duplicateTransactions()).hasSize(2);
        }
    }

    private ImportedTransaction createTransaction(LocalDate date, BigDecimal amount, String description) {
        return new ImportedTransaction(date, amount, description, null, null);
    }
}
