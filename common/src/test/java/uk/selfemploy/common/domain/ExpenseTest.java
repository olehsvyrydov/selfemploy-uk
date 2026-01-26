package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Expense Entity Tests")
class ExpenseTest {

    private static final UUID VALID_BUSINESS_ID = UUID.randomUUID();
    private static final LocalDate VALID_DATE = LocalDate.of(2025, 6, 15);
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("250.00");
    private static final String VALID_DESCRIPTION = "Office supplies";
    private static final ExpenseCategory VALID_CATEGORY = ExpenseCategory.OFFICE_COSTS;
    private static final String VALID_RECEIPT_PATH = "/receipts/2025/06/receipt-001.pdf";
    private static final String VALID_NOTES = "Printer ink and paper";

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("should create valid expense with all fields")
        void shouldCreateValidExpense() {
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                VALID_RECEIPT_PATH,
                VALID_NOTES
            );

            assertThat(expense).isNotNull();
            assertThat(expense.id()).isNotNull();
            assertThat(expense.businessId()).isEqualTo(VALID_BUSINESS_ID);
            assertThat(expense.date()).isEqualTo(VALID_DATE);
            assertThat(expense.amount()).isEqualByComparingTo(VALID_AMOUNT);
            assertThat(expense.description()).isEqualTo(VALID_DESCRIPTION);
            assertThat(expense.category()).isEqualTo(VALID_CATEGORY);
            assertThat(expense.receiptPath()).isEqualTo(VALID_RECEIPT_PATH);
            assertThat(expense.notes()).isEqualTo(VALID_NOTES);
        }

        @Test
        @DisplayName("should generate unique ID for each expense")
        void shouldGenerateUniqueId() {
            Expense expense1 = Expense.create(VALID_BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null);
            Expense expense2 = Expense.create(VALID_BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null);

            assertThat(expense1.id()).isNotEqualTo(expense2.id());
        }

        @Test
        @DisplayName("should create expense with null optional fields")
        void shouldCreateWithNullOptionalFields() {
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                null,  // receiptPath is optional
                null   // notes is optional
            );

            assertThat(expense.receiptPath()).isNull();
            assertThat(expense.notes()).isNull();
        }
    }

    @Nested
    @DisplayName("Amount Validation Tests")
    class AmountValidationTests {

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() ->
                Expense.create(VALID_BUSINESS_ID, VALID_DATE, null, VALID_DESCRIPTION, VALID_CATEGORY, null, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            BigDecimal negativeAmount = new BigDecimal("-50.00");

            assertThatThrownBy(() ->
                Expense.create(VALID_BUSINESS_ID, VALID_DATE, negativeAmount, VALID_DESCRIPTION, VALID_CATEGORY, null, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("amount")
             .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThatThrownBy(() ->
                Expense.create(VALID_BUSINESS_ID, VALID_DATE, BigDecimal.ZERO, VALID_DESCRIPTION, VALID_CATEGORY, null, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("amount")
             .hasMessageContaining("positive");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0.01", "1.00", "999999.99"})
        @DisplayName("should accept valid positive amounts")
        void shouldAcceptPositiveAmounts(String amountStr) {
            BigDecimal amount = new BigDecimal(amountStr);
            Expense expense = Expense.create(VALID_BUSINESS_ID, VALID_DATE, amount, VALID_DESCRIPTION, VALID_CATEGORY, null, null);

            assertThat(expense.amount()).isEqualByComparingTo(amount);
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    class DateValidationTests {

        @Test
        @DisplayName("should reject null date")
        void shouldRejectNullDate() {
            assertThatThrownBy(() ->
                Expense.create(VALID_BUSINESS_ID, null, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("date");
        }

        @Test
        @DisplayName("should reject future date")
        void shouldRejectFutureDate() {
            LocalDate futureDate = LocalDate.now().plusDays(1);

            assertThatThrownBy(() ->
                Expense.create(VALID_BUSINESS_ID, futureDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("date")
             .hasMessageContaining("future");
        }

        @Test
        @DisplayName("should accept today's date")
        void shouldAcceptTodayDate() {
            LocalDate today = LocalDate.now();
            Expense expense = Expense.create(VALID_BUSINESS_ID, today, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null);

            assertThat(expense.date()).isEqualTo(today);
        }

        @Test
        @DisplayName("should accept past date")
        void shouldAcceptPastDate() {
            LocalDate pastDate = LocalDate.now().minusYears(2);
            Expense expense = Expense.create(VALID_BUSINESS_ID, pastDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null);

            assertThat(expense.date()).isEqualTo(pastDate);
        }
    }

    @Nested
    @DisplayName("Category Validation Tests")
    class CategoryValidationTests {

        @Test
        @DisplayName("should reject null category")
        void shouldRejectNullCategory() {
            assertThatThrownBy(() ->
                Expense.create(VALID_BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, null, null, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("category");
        }

        @Test
        @DisplayName("should accept all SA103 categories")
        void shouldAcceptAllCategories() {
            for (ExpenseCategory category : ExpenseCategory.values()) {
                Expense expense = Expense.create(VALID_BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, category, null, null);
                assertThat(expense.category()).isEqualTo(category);
            }
        }
    }

    @Nested
    @DisplayName("Description Validation Tests")
    class DescriptionValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should reject null or empty description")
        void shouldRejectInvalidDescription(String invalidDescription) {
            assertThatThrownBy(() ->
                Expense.create(VALID_BUSINESS_ID, VALID_DATE, VALID_AMOUNT, invalidDescription, VALID_CATEGORY, null, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("description");
        }
    }

    @Nested
    @DisplayName("Business ID Validation Tests")
    class BusinessIdValidationTests {

        @Test
        @DisplayName("should reject null business ID")
        void shouldRejectNullBusinessId() {
            assertThatThrownBy(() ->
                Expense.create(null, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("businessId");
        }
    }

    @Nested
    @DisplayName("Allowable Expense Tests")
    class AllowableExpenseTests {

        @Test
        @DisplayName("should identify allowable expenses")
        void shouldIdentifyAllowableExpenses() {
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                ExpenseCategory.OFFICE_COSTS,
                null,
                null
            );

            assertThat(expense.isAllowable()).isTrue();
        }

        @Test
        @DisplayName("should identify non-allowable expenses (depreciation)")
        void shouldIdentifyNonAllowableExpenses() {
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                "Equipment depreciation",
                ExpenseCategory.DEPRECIATION,
                null,
                null
            );

            assertThat(expense.isAllowable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Unique Identifier Fields Tests (SE-10C-002)")
    class UniqueIdentifierFieldsTests {

        private static final String VALID_BANK_TRANSACTION_REF = "FPS-2025-001234";
        private static final String VALID_SUPPLIER_REF = "SUP-REF-001";
        private static final String VALID_INVOICE_NUMBER = "INV-2025-001";

        @Test
        @DisplayName("should create expense with bank transaction reference")
        void shouldCreateExpenseWithBankTransactionRef() {
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                VALID_RECEIPT_PATH,
                VALID_NOTES,
                VALID_BANK_TRANSACTION_REF,
                null,
                null
            );

            assertThat(expense.bankTransactionRef()).isEqualTo(VALID_BANK_TRANSACTION_REF);
        }

        @Test
        @DisplayName("should create expense with supplier reference")
        void shouldCreateExpenseWithSupplierRef() {
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                VALID_RECEIPT_PATH,
                VALID_NOTES,
                null,
                VALID_SUPPLIER_REF,
                null
            );

            assertThat(expense.supplierRef()).isEqualTo(VALID_SUPPLIER_REF);
        }

        @Test
        @DisplayName("should create expense with invoice number")
        void shouldCreateExpenseWithInvoiceNumber() {
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                VALID_RECEIPT_PATH,
                VALID_NOTES,
                null,
                null,
                VALID_INVOICE_NUMBER
            );

            assertThat(expense.invoiceNumber()).isEqualTo(VALID_INVOICE_NUMBER);
        }

        @Test
        @DisplayName("should create expense with all unique identifier fields")
        void shouldCreateExpenseWithAllUniqueIdentifierFields() {
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                VALID_RECEIPT_PATH,
                VALID_NOTES,
                VALID_BANK_TRANSACTION_REF,
                VALID_SUPPLIER_REF,
                VALID_INVOICE_NUMBER
            );

            assertThat(expense.bankTransactionRef()).isEqualTo(VALID_BANK_TRANSACTION_REF);
            assertThat(expense.supplierRef()).isEqualTo(VALID_SUPPLIER_REF);
            assertThat(expense.invoiceNumber()).isEqualTo(VALID_INVOICE_NUMBER);
        }

        @Test
        @DisplayName("should allow null unique identifier fields (backward compatible)")
        void shouldAllowNullUniqueIdentifierFields() {
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                VALID_RECEIPT_PATH,
                VALID_NOTES,
                null,
                null,
                null
            );

            assertThat(expense.bankTransactionRef()).isNull();
            assertThat(expense.supplierRef()).isNull();
            assertThat(expense.invoiceNumber()).isNull();
        }

        @Test
        @DisplayName("should preserve bank transaction ref with special characters")
        void shouldPreserveBankTransactionRefWithSpecialCharacters() {
            String refWithSpecialChars = "FPS/2025-001234#ABC";
            Expense expense = Expense.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                VALID_RECEIPT_PATH,
                VALID_NOTES,
                refWithSpecialChars,
                null,
                null
            );

            assertThat(expense.bankTransactionRef()).isEqualTo(refWithSpecialChars);
        }
    }
}
