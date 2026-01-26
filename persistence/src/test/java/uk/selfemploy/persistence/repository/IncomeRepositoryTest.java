package uk.selfemploy.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.BusinessType;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("IncomeRepository Integration Tests")
class IncomeRepositoryTest {

    @Inject
    IncomeRepository incomeRepository;

    @Inject
    BusinessRepository businessRepository;

    private UUID businessId;

    @BeforeEach
    @Transactional
    void setUp() {
        incomeRepository.deleteAll();
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
    @DisplayName("should save and retrieve income")
    void shouldSaveAndRetrieveIncome() {
        Income income = Income.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("1500.00"),
            "Web development project",
            IncomeCategory.SALES,
            "INV-001"
        );

        Income saved = incomeRepository.save(income);

        assertThat(saved.id()).isEqualTo(income.id());
        assertThat(saved.amount()).isEqualByComparingTo(new BigDecimal("1500.00"));

        List<Income> found = incomeRepository.findByBusinessId(businessId);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).description()).isEqualTo("Web development project");
    }

    @Test
    @Transactional
    @DisplayName("should find incomes by date range")
    void shouldFindByDateRange() {
        incomeRepository.save(Income.create(businessId, LocalDate.of(2025, 5, 1),
            new BigDecimal("1000.00"), "Income 1", IncomeCategory.SALES, null));
        incomeRepository.save(Income.create(businessId, LocalDate.of(2025, 6, 15),
            new BigDecimal("2000.00"), "Income 2", IncomeCategory.SALES, null));
        incomeRepository.save(Income.create(businessId, LocalDate.of(2025, 8, 1),
            new BigDecimal("1500.00"), "Income 3", IncomeCategory.SALES, null));

        List<Income> found = incomeRepository.findByDateRange(
            businessId,
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 6, 30)
        );

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Income::description)
            .containsExactlyInAnyOrder("Income 1", "Income 2");
    }

    @Test
    @Transactional
    @DisplayName("should calculate total income for date range")
    void shouldCalculateTotalForDateRange() {
        incomeRepository.save(Income.create(businessId, LocalDate.of(2025, 5, 1),
            new BigDecimal("1000.00"), "Income 1", IncomeCategory.SALES, null));
        incomeRepository.save(Income.create(businessId, LocalDate.of(2025, 6, 15),
            new BigDecimal("2000.00"), "Income 2", IncomeCategory.SALES, null));
        incomeRepository.save(Income.create(businessId, LocalDate.of(2025, 8, 1),
            new BigDecimal("1500.00"), "Income 3", IncomeCategory.SALES, null));

        BigDecimal total = incomeRepository.calculateTotalForDateRange(
            businessId,
            LocalDate.of(2025, 4, 6),
            LocalDate.of(2026, 4, 5)
        );

        assertThat(total).isEqualByComparingTo(new BigDecimal("4500.00"));
    }

    @Test
    @Transactional
    @DisplayName("should find incomes by category")
    void shouldFindByCategory() {
        incomeRepository.save(Income.create(businessId, LocalDate.of(2025, 5, 1),
            new BigDecimal("1000.00"), "Sales Income", IncomeCategory.SALES, null));
        incomeRepository.save(Income.create(businessId, LocalDate.of(2025, 6, 15),
            new BigDecimal("500.00"), "Other Income", IncomeCategory.OTHER_INCOME, null));

        List<Income> salesIncomes = incomeRepository.findByCategory(businessId, IncomeCategory.SALES);

        assertThat(salesIncomes).hasSize(1);
        assertThat(salesIncomes.get(0).description()).isEqualTo("Sales Income");
    }

    // ===== Duplicate Detection by Bank Transaction Reference (SE-10C-002) =====

    @Test
    @Transactional
    @DisplayName("should find existing income by bank transaction reference")
    void shouldFindByBankTransactionRef() {
        String bankRef = "FPS-2025-001234";
        incomeRepository.save(Income.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("1500.00"),
            "Payment received",
            IncomeCategory.SALES,
            null,
            bankRef,
            null,
            null
        ));

        boolean exists = incomeRepository.existsByBusinessIdAndBankTransactionRef(businessId, bankRef);

        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("should return false when bank transaction reference does not exist")
    void shouldReturnFalseWhenBankRefNotFound() {
        boolean exists = incomeRepository.existsByBusinessIdAndBankTransactionRef(
            businessId, "NONEXISTENT-REF"
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should not find bank transaction ref from different business")
    void shouldNotFindBankRefFromDifferentBusiness() {
        String bankRef = "FPS-2025-001234";
        incomeRepository.save(Income.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("1500.00"),
            "Payment received",
            IncomeCategory.SALES,
            null,
            bankRef,
            null,
            null
        ));

        UUID differentBusinessId = UUID.randomUUID();
        boolean exists = incomeRepository.existsByBusinessIdAndBankTransactionRef(
            differentBusinessId, bankRef
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle null bank transaction reference")
    void shouldHandleNullBankRef() {
        boolean exists = incomeRepository.existsByBusinessIdAndBankTransactionRef(
            businessId, null
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle blank bank transaction reference")
    void shouldHandleBlankBankRef() {
        boolean exists = incomeRepository.existsByBusinessIdAndBankTransactionRef(
            businessId, "   "
        );

        assertThat(exists).isFalse();
    }

    // ===== Duplicate Detection by Invoice Number (SE-10C-002) =====

    @Test
    @Transactional
    @DisplayName("should find existing income by invoice number")
    void shouldFindByInvoiceNumber() {
        String invoiceNumber = "INV-2025-001";
        incomeRepository.save(Income.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("1500.00"),
            "Invoice payment",
            IncomeCategory.SALES,
            null,
            null,
            invoiceNumber,
            null
        ));

        boolean exists = incomeRepository.existsByBusinessIdAndInvoiceNumber(
            businessId, invoiceNumber
        );

        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("should return false when invoice number does not exist")
    void shouldReturnFalseWhenInvoiceNumberNotFound() {
        boolean exists = incomeRepository.existsByBusinessIdAndInvoiceNumber(
            businessId, "NONEXISTENT-INV"
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle null invoice number")
    void shouldHandleNullInvoiceNumber() {
        boolean exists = incomeRepository.existsByBusinessIdAndInvoiceNumber(
            businessId, null
        );

        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("should handle blank invoice number")
    void shouldHandleBlankInvoiceNumber() {
        boolean exists = incomeRepository.existsByBusinessIdAndInvoiceNumber(
            businessId, "   "
        );

        assertThat(exists).isFalse();
    }

    // ===== Save Income with Unique Identifier Fields (SE-10C-002) =====

    @Test
    @Transactional
    @DisplayName("should save and retrieve income with all unique identifier fields")
    void shouldSaveAndRetrieveWithAllFields() {
        String bankRef = "FPS-2025-001234";
        String invoiceNumber = "INV-2025-001";
        String receiptPath = "/receipts/2025/06/receipt-001.pdf";

        Income income = Income.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("1500.00"),
            "Full featured income",
            IncomeCategory.SALES,
            "REF-001",
            bankRef,
            invoiceNumber,
            receiptPath
        );

        Income saved = incomeRepository.save(income);

        List<Income> found = incomeRepository.findByBusinessId(businessId);
        assertThat(found).hasSize(1);

        Income retrieved = found.get(0);
        assertThat(retrieved.bankTransactionRef()).isEqualTo(bankRef);
        assertThat(retrieved.invoiceNumber()).isEqualTo(invoiceNumber);
        assertThat(retrieved.receiptPath()).isEqualTo(receiptPath);
    }

    @Test
    @Transactional
    @DisplayName("should save income with null unique identifier fields (backward compatible)")
    void shouldSaveWithNullUniqueIdentifierFields() {
        Income income = Income.create(
            businessId,
            LocalDate.of(2025, 6, 15),
            new BigDecimal("1500.00"),
            "Simple income",
            IncomeCategory.SALES,
            null
        );

        Income saved = incomeRepository.save(income);

        List<Income> found = incomeRepository.findByBusinessId(businessId);
        assertThat(found).hasSize(1);

        Income retrieved = found.get(0);
        assertThat(retrieved.bankTransactionRef()).isNull();
        assertThat(retrieved.invoiceNumber()).isNull();
        assertThat(retrieved.receiptPath()).isNull();
    }
}
