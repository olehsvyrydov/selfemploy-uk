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

    // ===== Duplicate Detection by Bank Transaction Reference (SE-10C-002) =====

    @Test
    @Transactional
    @DisplayName("should find existing expense by bank transaction reference")
    void shouldFindByBankTransactionRef() {
        String bankRef = "FPS-2025-001234";
        expenseRepository.save(Expense.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("250.00"),
            "Office supplies",
            ExpenseCategory.OFFICE_COSTS,
            null,
            null,
            bankRef,
            null,
            null
        ));

        boolean exists = expenseRepository.existsByBusinessIdAndBankTransactionRef(businessId, bankRef);

        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("should return false when bank transaction reference does not exist")
    void shouldReturnFalseWhenBankRefNotFound() {
        boolean exists = expenseRepository.existsByBusinessIdAndBankTransactionRef(
            businessId, "NONEXISTENT-REF"
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should not find bank transaction ref from different business")
    void shouldNotFindBankRefFromDifferentBusiness() {
        String bankRef = "FPS-2025-001234";
        expenseRepository.save(Expense.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("250.00"),
            "Office supplies",
            ExpenseCategory.OFFICE_COSTS,
            null,
            null,
            bankRef,
            null,
            null
        ));

        UUID differentBusinessId = UUID.randomUUID();
        boolean exists = expenseRepository.existsByBusinessIdAndBankTransactionRef(
            differentBusinessId, bankRef
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle null bank transaction reference")
    void shouldHandleNullBankRef() {
        boolean exists = expenseRepository.existsByBusinessIdAndBankTransactionRef(
            businessId, null
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle blank bank transaction reference")
    void shouldHandleBlankBankRef() {
        boolean exists = expenseRepository.existsByBusinessIdAndBankTransactionRef(
            businessId, "   "
        );

        assertThat(exists).isFalse();
    }

    // ===== Duplicate Detection by Supplier Reference (SE-10C-002) =====

    @Test
    @Transactional
    @DisplayName("should find existing expense by supplier reference")
    void shouldFindBySupplierRef() {
        String supplierRef = "SUP-REF-001";
        expenseRepository.save(Expense.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("250.00"),
            "Supplier order",
            ExpenseCategory.COST_OF_GOODS,
            null,
            null,
            null,
            supplierRef,
            null
        ));

        boolean exists = expenseRepository.existsByBusinessIdAndSupplierRef(businessId, supplierRef);

        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("should return false when supplier reference does not exist")
    void shouldReturnFalseWhenSupplierRefNotFound() {
        boolean exists = expenseRepository.existsByBusinessIdAndSupplierRef(
            businessId, "NONEXISTENT-SUP"
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle null supplier reference")
    void shouldHandleNullSupplierRef() {
        boolean exists = expenseRepository.existsByBusinessIdAndSupplierRef(
            businessId, null
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle blank supplier reference")
    void shouldHandleBlankSupplierRef() {
        boolean exists = expenseRepository.existsByBusinessIdAndSupplierRef(
            businessId, "   "
        );

        assertThat(exists).isFalse();
    }

    // ===== Duplicate Detection by Invoice Number (SE-10C-002) =====

    @Test
    @Transactional
    @DisplayName("should find existing expense by invoice number")
    void shouldFindByInvoiceNumber() {
        String invoiceNumber = "INV-2025-001";
        expenseRepository.save(Expense.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("250.00"),
            "Invoice payment",
            ExpenseCategory.PROFESSIONAL_FEES,
            null,
            null,
            null,
            null,
            invoiceNumber
        ));

        boolean exists = expenseRepository.existsByBusinessIdAndInvoiceNumber(
            businessId, invoiceNumber
        );

        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("should return false when invoice number does not exist")
    void shouldReturnFalseWhenInvoiceNumberNotFound() {
        boolean exists = expenseRepository.existsByBusinessIdAndInvoiceNumber(
            businessId, "NONEXISTENT-INV"
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle null invoice number")
    void shouldHandleNullInvoiceNumber() {
        boolean exists = expenseRepository.existsByBusinessIdAndInvoiceNumber(
            businessId, null
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle blank invoice number")
    void shouldHandleBlankInvoiceNumber() {
        boolean exists = expenseRepository.existsByBusinessIdAndInvoiceNumber(
            businessId, "   "
        );

        assertThat(exists).isFalse();
    }

    // ===== Save Expense with Unique Identifier Fields (SE-10C-002) =====

    @Test
    @Transactional
    @DisplayName("should save and retrieve expense with all unique identifier fields")
    void shouldSaveAndRetrieveWithAllFields() {
        String bankRef = "FPS-2025-001234";
        String supplierRef = "SUP-REF-001";
        String invoiceNumber = "INV-2025-001";

        Expense expense = Expense.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("250.00"),
            "Full featured expense",
            ExpenseCategory.OFFICE_COSTS,
            "/receipts/receipt.pdf",
            "Notes here",
            bankRef,
            supplierRef,
            invoiceNumber
        );

        Expense saved = expenseRepository.save(expense);

        List<Expense> found = expenseRepository.findByBusinessId(businessId);
        assertThat(found).hasSize(1);

        Expense retrieved = found.get(0);
        assertThat(retrieved.bankTransactionRef()).isEqualTo(bankRef);
        assertThat(retrieved.supplierRef()).isEqualTo(supplierRef);
        assertThat(retrieved.invoiceNumber()).isEqualTo(invoiceNumber);
    }

    @Test
    @Transactional
    @DisplayName("should save expense with null unique identifier fields (backward compatible)")
    void shouldSaveWithNullUniqueIdentifierFields() {
        Expense expense = Expense.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("250.00"),
            "Simple expense",
            ExpenseCategory.OFFICE_COSTS,
            null,
            null
        );

        Expense saved = expenseRepository.save(expense);

        List<Expense> found = expenseRepository.findByBusinessId(businessId);
        assertThat(found).hasSize(1);

        Expense retrieved = found.get(0);
        assertThat(retrieved.bankTransactionRef()).isNull();
        assertThat(retrieved.supplierRef()).isNull();
        assertThat(retrieved.invoiceNumber()).isNull();
    }
}
