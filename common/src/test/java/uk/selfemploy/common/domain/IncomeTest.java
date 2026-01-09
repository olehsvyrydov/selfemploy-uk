package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Income Entity Tests")
class IncomeTest {

    private static final UUID VALID_BUSINESS_ID = UUID.randomUUID();
    private static final LocalDate VALID_DATE = LocalDate.of(2025, 6, 15);
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("1500.00");
    private static final String VALID_DESCRIPTION = "Web development project";
    private static final IncomeCategory VALID_CATEGORY = IncomeCategory.SALES;
    private static final String VALID_REFERENCE = "INV-001";

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("should create valid income with all fields")
        void shouldCreateValidIncome() {
            Income income = Income.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                VALID_REFERENCE
            );

            assertThat(income).isNotNull();
            assertThat(income.id()).isNotNull();
            assertThat(income.businessId()).isEqualTo(VALID_BUSINESS_ID);
            assertThat(income.date()).isEqualTo(VALID_DATE);
            assertThat(income.amount()).isEqualByComparingTo(VALID_AMOUNT);
            assertThat(income.description()).isEqualTo(VALID_DESCRIPTION);
            assertThat(income.category()).isEqualTo(VALID_CATEGORY);
            assertThat(income.reference()).isEqualTo(VALID_REFERENCE);
        }

        @Test
        @DisplayName("should generate unique ID for each income")
        void shouldGenerateUniqueId() {
            Income income1 = Income.create(VALID_BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE);
            Income income2 = Income.create(VALID_BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE);

            assertThat(income1.id()).isNotEqualTo(income2.id());
        }

        @Test
        @DisplayName("should create income with null optional fields")
        void shouldCreateWithNullOptionalFields() {
            Income income = Income.create(
                VALID_BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                null  // reference is optional
            );

            assertThat(income.reference()).isNull();
        }
    }

    @Nested
    @DisplayName("Amount Validation Tests")
    class AmountValidationTests {

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThatThrownBy(() ->
                Income.create(VALID_BUSINESS_ID, VALID_DATE, null, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            BigDecimal negativeAmount = new BigDecimal("-100.00");

            assertThatThrownBy(() ->
                Income.create(VALID_BUSINESS_ID, VALID_DATE, negativeAmount, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("amount")
             .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThatThrownBy(() ->
                Income.create(VALID_BUSINESS_ID, VALID_DATE, BigDecimal.ZERO, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("amount")
             .hasMessageContaining("positive");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0.01", "1.00", "999999.99", "1000000.00"})
        @DisplayName("should accept valid positive amounts")
        void shouldAcceptPositiveAmounts(String amountStr) {
            BigDecimal amount = new BigDecimal(amountStr);
            Income income = Income.create(VALID_BUSINESS_ID, VALID_DATE, amount, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE);

            assertThat(income.amount()).isEqualByComparingTo(amount);
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    class DateValidationTests {

        @Test
        @DisplayName("should reject null date")
        void shouldRejectNullDate() {
            assertThatThrownBy(() ->
                Income.create(VALID_BUSINESS_ID, null, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("date");
        }

        @Test
        @DisplayName("should accept past date")
        void shouldAcceptPastDate() {
            LocalDate pastDate = LocalDate.now().minusMonths(6);
            Income income = Income.create(VALID_BUSINESS_ID, pastDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE);

            assertThat(income.date()).isEqualTo(pastDate);
        }

        @Test
        @DisplayName("should accept today's date")
        void shouldAcceptTodayDate() {
            LocalDate today = LocalDate.now();
            Income income = Income.create(VALID_BUSINESS_ID, today, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE);

            assertThat(income.date()).isEqualTo(today);
        }
    }

    @Nested
    @DisplayName("Business ID Validation Tests")
    class BusinessIdValidationTests {

        @Test
        @DisplayName("should reject null business ID")
        void shouldRejectNullBusinessId() {
            assertThatThrownBy(() ->
                Income.create(null, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, VALID_REFERENCE)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("businessId");
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
                Income.create(VALID_BUSINESS_ID, VALID_DATE, VALID_AMOUNT, invalidDescription, VALID_CATEGORY, VALID_REFERENCE)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("description");
        }
    }

    @Nested
    @DisplayName("Category Validation Tests")
    class CategoryValidationTests {

        @Test
        @DisplayName("should reject null category")
        void shouldRejectNullCategory() {
            assertThatThrownBy(() ->
                Income.create(VALID_BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, null, VALID_REFERENCE)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("category");
        }
    }
}
