package uk.selfemploy.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.BusinessType;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for soft delete functionality in Income and Expense repositories.
 */
@QuarkusTest
@DisplayName("Soft Delete Repository Integration Tests")
class SoftDeleteRepositoryTest {

    @Inject
    IncomeRepository incomeRepository;

    @Inject
    ExpenseRepository expenseRepository;

    @Inject
    BusinessRepository businessRepository;

    private UUID businessId;

    @BeforeEach
    @Transactional
    void setUp() {
        incomeRepository.deleteAll();
        expenseRepository.deleteAll();
        businessRepository.deleteAll();

        Business business = businessRepository.save(Business.create(
            "Test Business", "1234567890",
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5),
            BusinessType.SELF_EMPLOYED, null
        ));
        businessId = business.id();
    }

    @Test
    @Transactional
    @DisplayName("should soft delete income records by IDs")
    void shouldSoftDeleteIncomeRecordsByIds() {
        // Given
        Income income1 = incomeRepository.save(Income.create(businessId,
            LocalDate.of(2025, 6, 15), new BigDecimal("1000.00"),
            "Income 1", IncomeCategory.SALES, null));
        Income income2 = incomeRepository.save(Income.create(businessId,
            LocalDate.of(2025, 6, 16), new BigDecimal("2000.00"),
            "Income 2", IncomeCategory.SALES, null));

        // When
        int deleted = incomeRepository.softDeleteByIds(
            List.of(income1.id(), income2.id()),
            Instant.now(),
            "test user",
            "test reason"
        );

        // Then
        assertThat(deleted).isEqualTo(2);

        // Verify standard queries exclude soft-deleted records
        List<Income> activeIncomes = incomeRepository.findByBusinessId(businessId);
        assertThat(activeIncomes).isEmpty();

        // Verify IncludingDeleted queries include soft-deleted records
        List<Income> allIncomes = incomeRepository.findByBusinessIdIncludingDeleted(businessId);
        assertThat(allIncomes).hasSize(2);
    }

    @Test
    @Transactional
    @DisplayName("should exclude soft-deleted incomes from totals")
    void shouldExcludeSoftDeletedIncomesFromTotals() {
        // Given
        Income income1 = incomeRepository.save(Income.create(businessId,
            LocalDate.of(2025, 6, 15), new BigDecimal("1000.00"),
            "Income 1", IncomeCategory.SALES, null));
        incomeRepository.save(Income.create(businessId,
            LocalDate.of(2025, 6, 16), new BigDecimal("2000.00"),
            "Income 2", IncomeCategory.SALES, null));

        // Soft delete first income
        incomeRepository.softDeleteByIds(
            List.of(income1.id()),
            Instant.now(),
            "test user",
            "test reason"
        );

        // When
        BigDecimal total = incomeRepository.calculateTotalForDateRange(
            businessId,
            LocalDate.of(2025, 4, 6),
            LocalDate.of(2026, 4, 5)
        );

        // Then
        assertThat(total).isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    @Test
    @Transactional
    @DisplayName("should restore soft-deleted incomes")
    void shouldRestoreSoftDeletedIncomes() {
        // Given
        Income income = incomeRepository.save(Income.create(businessId,
            LocalDate.of(2025, 6, 15), new BigDecimal("1000.00"),
            "Income 1", IncomeCategory.SALES, null));

        incomeRepository.softDeleteByIds(
            List.of(income.id()),
            Instant.now(),
            "test user",
            "test reason"
        );

        // When
        int restored = incomeRepository.restoreByIds(List.of(income.id()));

        // Then
        assertThat(restored).isEqualTo(1);

        List<Income> activeIncomes = incomeRepository.findByBusinessId(businessId);
        assertThat(activeIncomes).hasSize(1);
    }

    @Test
    @Transactional
    @DisplayName("should soft delete expense records by IDs")
    void shouldSoftDeleteExpenseRecordsByIds() {
        // Given
        Expense expense1 = expenseRepository.save(Expense.create(businessId,
            LocalDate.of(2025, 6, 15), new BigDecimal("500.00"),
            "Expense 1", ExpenseCategory.OFFICE_COSTS, null, null));
        Expense expense2 = expenseRepository.save(Expense.create(businessId,
            LocalDate.of(2025, 6, 16), new BigDecimal("300.00"),
            "Expense 2", ExpenseCategory.TRAVEL, null, null));

        // When
        int deleted = expenseRepository.softDeleteByIds(
            List.of(expense1.id(), expense2.id()),
            Instant.now(),
            "test user",
            "test reason"
        );

        // Then
        assertThat(deleted).isEqualTo(2);

        // Verify standard queries exclude soft-deleted records
        List<Expense> activeExpenses = expenseRepository.findByBusinessId(businessId);
        assertThat(activeExpenses).isEmpty();

        // Verify IncludingDeleted queries include soft-deleted records
        List<Expense> allExpenses = expenseRepository.findByBusinessIdIncludingDeleted(businessId);
        assertThat(allExpenses).hasSize(2);
    }

    @Test
    @Transactional
    @DisplayName("should exclude soft-deleted expenses from totals")
    void shouldExcludeSoftDeletedExpensesFromTotals() {
        // Given
        Expense expense1 = expenseRepository.save(Expense.create(businessId,
            LocalDate.of(2025, 6, 15), new BigDecimal("500.00"),
            "Expense 1", ExpenseCategory.OFFICE_COSTS, null, null));
        expenseRepository.save(Expense.create(businessId,
            LocalDate.of(2025, 6, 16), new BigDecimal("300.00"),
            "Expense 2", ExpenseCategory.TRAVEL, null, null));

        // Soft delete first expense
        expenseRepository.softDeleteByIds(
            List.of(expense1.id()),
            Instant.now(),
            "test user",
            "test reason"
        );

        // When
        BigDecimal total = expenseRepository.calculateTotalForDateRange(
            businessId,
            LocalDate.of(2025, 4, 6),
            LocalDate.of(2026, 4, 5)
        );

        // Then
        assertThat(total).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @Transactional
    @DisplayName("should restore soft-deleted expenses")
    void shouldRestoreSoftDeletedExpenses() {
        // Given
        Expense expense = expenseRepository.save(Expense.create(businessId,
            LocalDate.of(2025, 6, 15), new BigDecimal("500.00"),
            "Expense 1", ExpenseCategory.OFFICE_COSTS, null, null));

        expenseRepository.softDeleteByIds(
            List.of(expense.id()),
            Instant.now(),
            "test user",
            "test reason"
        );

        // When
        int restored = expenseRepository.restoreByIds(List.of(expense.id()));

        // Then
        assertThat(restored).isEqualTo(1);

        List<Expense> activeExpenses = expenseRepository.findByBusinessId(businessId);
        assertThat(activeExpenses).hasSize(1);
    }
}
