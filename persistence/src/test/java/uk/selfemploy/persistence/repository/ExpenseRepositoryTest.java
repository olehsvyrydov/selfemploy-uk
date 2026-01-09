package uk.selfemploy.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.BusinessType;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("ExpenseRepository Integration Tests")
class ExpenseRepositoryTest {

    @Inject
    ExpenseRepository expenseRepository;

    @Inject
    BusinessRepository businessRepository;

    private UUID businessId;

    @BeforeEach
    @Transactional
    void setUp() {
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
    @DisplayName("should save and retrieve expense")
    void shouldSaveAndRetrieveExpense() {
        Expense expense = Expense.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("250.00"),
            "Office supplies",
            ExpenseCategory.OFFICE_COSTS,
            null,
            "Printer ink and paper"
        );

        Expense saved = expenseRepository.save(expense);

        assertThat(saved.id()).isEqualTo(expense.id());
        assertThat(saved.amount()).isEqualByComparingTo(new BigDecimal("250.00"));

        List<Expense> found = expenseRepository.findByBusinessId(businessId);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).description()).isEqualTo("Office supplies");
    }

    @Test
    @Transactional
    @DisplayName("should find expenses by date range")
    void shouldFindByDateRange() {
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 5, 1),
            new BigDecimal("100.00"), "Expense 1", ExpenseCategory.OFFICE_COSTS, null, null));
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 6, 15),
            new BigDecimal("200.00"), "Expense 2", ExpenseCategory.TRAVEL, null, null));
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 8, 1),
            new BigDecimal("150.00"), "Expense 3", ExpenseCategory.OFFICE_COSTS, null, null));

        List<Expense> found = expenseRepository.findByDateRange(
            businessId,
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 6, 30)
        );

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Expense::description)
            .containsExactlyInAnyOrder("Expense 1", "Expense 2");
    }

    @Test
    @Transactional
    @DisplayName("should calculate total expenses for date range")
    void shouldCalculateTotalForDateRange() {
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 5, 1),
            new BigDecimal("100.00"), "Expense 1", ExpenseCategory.OFFICE_COSTS, null, null));
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 6, 15),
            new BigDecimal("200.00"), "Expense 2", ExpenseCategory.TRAVEL, null, null));
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 8, 1),
            new BigDecimal("150.00"), "Expense 3", ExpenseCategory.OFFICE_COSTS, null, null));

        BigDecimal total = expenseRepository.calculateTotalForDateRange(
            businessId,
            LocalDate.of(2025, 4, 6),
            LocalDate.of(2026, 4, 5)
        );

        assertThat(total).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    @Transactional
    @DisplayName("should calculate allowable expenses only")
    void shouldCalculateAllowableExpensesOnly() {
        // Allowable expense
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 5, 1),
            new BigDecimal("100.00"), "Office supplies", ExpenseCategory.OFFICE_COSTS, null, null));

        // Non-allowable expense (depreciation)
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 6, 15),
            new BigDecimal("500.00"), "Equipment depreciation", ExpenseCategory.DEPRECIATION, null, null));

        BigDecimal allowableTotal = expenseRepository.calculateAllowableTotalForDateRange(
            businessId,
            LocalDate.of(2025, 4, 6),
            LocalDate.of(2026, 4, 5)
        );

        assertThat(allowableTotal).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @Transactional
    @DisplayName("should calculate totals by category")
    void shouldCalculateTotalsByCategory() {
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 5, 1),
            new BigDecimal("100.00"), "Office 1", ExpenseCategory.OFFICE_COSTS, null, null));
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 5, 15),
            new BigDecimal("50.00"), "Office 2", ExpenseCategory.OFFICE_COSTS, null, null));
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 6, 1),
            new BigDecimal("200.00"), "Travel 1", ExpenseCategory.TRAVEL, null, null));
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 7, 1),
            new BigDecimal("300.00"), "Fees", ExpenseCategory.PROFESSIONAL_FEES, null, null));

        Map<ExpenseCategory, BigDecimal> totals = expenseRepository.calculateTotalsByCategoryForDateRange(
            businessId,
            LocalDate.of(2025, 4, 6),
            LocalDate.of(2026, 4, 5)
        );

        assertThat(totals.get(ExpenseCategory.OFFICE_COSTS)).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(totals.get(ExpenseCategory.TRAVEL)).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(totals.get(ExpenseCategory.PROFESSIONAL_FEES)).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @Transactional
    @DisplayName("should find expenses by category")
    void shouldFindByCategory() {
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 5, 1),
            new BigDecimal("100.00"), "Office expense", ExpenseCategory.OFFICE_COSTS, null, null));
        expenseRepository.save(Expense.create(businessId, LocalDate.of(2025, 6, 15),
            new BigDecimal("200.00"), "Travel expense", ExpenseCategory.TRAVEL, null, null));

        List<Expense> officeExpenses = expenseRepository.findByCategory(businessId, ExpenseCategory.OFFICE_COSTS);

        assertThat(officeExpenses).hasSize(1);
        assertThat(officeExpenses.get(0).description()).isEqualTo("Office expense");
    }
}
