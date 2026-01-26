package uk.selfemploy.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.persistence.repository.ExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpenseService.
 *
 * Tests cover:
 * - CRUD operations (create, findById, update, delete)
 * - Validation: date within tax year, amount positive, description required (max 100 chars), valid category
 * - Query methods: findByTaxYear, findByCategory, getTotalByTaxYear, getDeductibleTotal
 * - Business rules: cannot delete if linked to HMRC submission (future feature)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Tests")
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    private ExpenseService expenseService;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final TaxYear TAX_YEAR_2025 = TaxYear.of(2025);
    private static final LocalDate VALID_DATE = LocalDate.of(2025, 6, 15); // Within 2025/26 tax year
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("500.00");
    private static final String VALID_DESCRIPTION = "Office supplies";
    private static final ExpenseCategory VALID_CATEGORY = ExpenseCategory.OFFICE_COSTS;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(expenseRepository);
    }

    @Nested
    @DisplayName("Create Expense Tests")
    class CreateExpenseTests {

        @Test
        @DisplayName("should create expense with valid data")
        void shouldCreateExpenseWithValidData() {
            Expense expectedExpense = createValidExpense();
            when(expenseRepository.save(any(Expense.class))).thenReturn(expectedExpense);

            Expense result = expenseService.create(
                    BUSINESS_ID,
                    VALID_DATE,
                    VALID_AMOUNT,
                    VALID_DESCRIPTION,
                    VALID_CATEGORY,
                    "/receipts/receipt.pdf",
                    "Tax deductible"
            );

            assertThat(result).isNotNull();
            assertThat(result.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(result.amount()).isEqualByComparingTo(VALID_AMOUNT);
            assertThat(result.description()).isEqualTo(VALID_DESCRIPTION);
            assertThat(result.category()).isEqualTo(VALID_CATEGORY);
            verify(expenseRepository).save(any(Expense.class));
        }

        @Test
        @DisplayName("should throw ValidationException when date is null")
        void shouldThrowWhenDateIsNull() {
            assertThatThrownBy(() -> expenseService.create(
                    BUSINESS_ID, null, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("date");
        }

        @Test
        @DisplayName("should accept date in any valid tax year range (2000-2100)")
        void shouldAcceptDateInValidTaxYearRange() {
            // Any date between 2000 and 2100 is considered valid for tax year purposes
            LocalDate dateInPast = LocalDate.of(2020, 6, 15);
            Expense expectedExpense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, dateInPast, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null, null, null, null);
            when(expenseRepository.save(any(Expense.class))).thenReturn(expectedExpense);

            Expense result = expenseService.create(
                    BUSINESS_ID, dateInPast, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null);

            assertThat(result.date()).isEqualTo(dateInPast);
        }

        @Test
        @DisplayName("should throw ValidationException when amount is null")
        void shouldThrowWhenAmountIsNull() {
            assertThatThrownBy(() -> expenseService.create(
                    BUSINESS_ID, VALID_DATE, null, VALID_DESCRIPTION, VALID_CATEGORY, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw ValidationException when amount is zero")
        void shouldThrowWhenAmountIsZero() {
            assertThatThrownBy(() -> expenseService.create(
                    BUSINESS_ID, VALID_DATE, BigDecimal.ZERO, VALID_DESCRIPTION, VALID_CATEGORY, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("amount")
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("should throw ValidationException when amount is negative")
        void shouldThrowWhenAmountIsNegative() {
            assertThatThrownBy(() -> expenseService.create(
                    BUSINESS_ID, VALID_DATE, new BigDecimal("-50"), VALID_DESCRIPTION, VALID_CATEGORY, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("amount")
                    .hasMessageContaining("positive");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("should throw ValidationException when description is null or blank")
        void shouldThrowWhenDescriptionIsNullOrBlank(String description) {
            assertThatThrownBy(() -> expenseService.create(
                    BUSINESS_ID, VALID_DATE, VALID_AMOUNT, description, VALID_CATEGORY, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should throw ValidationException when description exceeds 100 characters")
        void shouldThrowWhenDescriptionExceeds100Chars() {
            String longDescription = "A".repeat(101);

            assertThatThrownBy(() -> expenseService.create(
                    BUSINESS_ID, VALID_DATE, VALID_AMOUNT, longDescription, VALID_CATEGORY, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("description")
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("should accept description with exactly 100 characters")
        void shouldAcceptDescriptionWith100Chars() {
            String maxDescription = "A".repeat(100);
            Expense expectedExpense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, VALID_DATE, VALID_AMOUNT, maxDescription, VALID_CATEGORY, null, null, null, null, null);
            when(expenseRepository.save(any(Expense.class))).thenReturn(expectedExpense);

            Expense result = expenseService.create(
                    BUSINESS_ID, VALID_DATE, VALID_AMOUNT, maxDescription, VALID_CATEGORY, null, null);

            assertThat(result.description()).hasSize(100);
        }

        @Test
        @DisplayName("should throw ValidationException when category is null")
        void shouldThrowWhenCategoryIsNull() {
            assertThatThrownBy(() -> expenseService.create(
                    BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, null, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("category");
        }

        @ParameterizedTest
        @EnumSource(ExpenseCategory.class)
        @DisplayName("should accept all valid SA103 expense categories")
        void shouldAcceptAllValidCategories(ExpenseCategory category) {
            Expense expectedExpense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, category, null, null, null, null, null);
            when(expenseRepository.save(any(Expense.class))).thenReturn(expectedExpense);

            Expense result = expenseService.create(
                    BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, category, null, null);

            assertThat(result.category()).isEqualTo(category);
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> expenseService.create(
                    null, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("should return expense when found")
        void shouldReturnExpenseWhenFound() {
            UUID expenseId = UUID.randomUUID();
            Expense expectedExpense = createValidExpense();
            when(expenseRepository.findByIdAsDomain(expenseId)).thenReturn(Optional.of(expectedExpense));

            Optional<Expense> result = expenseService.findById(expenseId);

            assertThat(result).isPresent();
            assertThat(result.get().businessId()).isEqualTo(BUSINESS_ID);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            UUID expenseId = UUID.randomUUID();
            when(expenseRepository.findByIdAsDomain(expenseId)).thenReturn(Optional.empty());

            Optional<Expense> result = expenseService.findById(expenseId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ValidationException when id is null")
        void shouldThrowWhenIdIsNull() {
            assertThatThrownBy(() -> expenseService.findById(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("id");
        }
    }

    @Nested
    @DisplayName("Update Expense Tests")
    class UpdateExpenseTests {

        @Test
        @DisplayName("should update expense with valid data")
        void shouldUpdateExpenseWithValidData() {
            Expense existingExpense = createValidExpense();
            Expense updatedExpense = new Expense(
                    existingExpense.id(), BUSINESS_ID, VALID_DATE, new BigDecimal("750.00"),
                    "Updated description", ExpenseCategory.TRAVEL, "/new/receipt.pdf", "Updated notes",
                    null, null, null);

            when(expenseRepository.findByIdAsDomain(existingExpense.id())).thenReturn(Optional.of(existingExpense));
            when(expenseRepository.update(any(Expense.class))).thenReturn(updatedExpense);

            Expense result = expenseService.update(
                    existingExpense.id(),
                    VALID_DATE,
                    new BigDecimal("750.00"),
                    "Updated description",
                    ExpenseCategory.TRAVEL,
                    "/new/receipt.pdf",
                    "Updated notes"
            );

            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("750.00"));
            assertThat(result.description()).isEqualTo("Updated description");
            assertThat(result.category()).isEqualTo(ExpenseCategory.TRAVEL);
            verify(expenseRepository).update(any(Expense.class));
        }

        @Test
        @DisplayName("should throw ValidationException when expense not found")
        void shouldThrowWhenExpenseNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(expenseRepository.findByIdAsDomain(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.update(
                    nonExistentId, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("should validate data on update")
        void shouldValidateDataOnUpdate() {
            UUID expenseId = UUID.randomUUID();
            Expense existingExpense = createValidExpense();
            when(expenseRepository.findByIdAsDomain(expenseId)).thenReturn(Optional.of(existingExpense));

            assertThatThrownBy(() -> expenseService.update(
                    expenseId, VALID_DATE, BigDecimal.ZERO, VALID_DESCRIPTION, VALID_CATEGORY, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("amount");
        }
    }

    @Nested
    @DisplayName("Delete Expense Tests")
    class DeleteExpenseTests {

        @Test
        @DisplayName("should delete expense successfully")
        void shouldDeleteExpenseSuccessfully() {
            UUID expenseId = UUID.randomUUID();
            when(expenseRepository.findByIdAsDomain(expenseId)).thenReturn(Optional.of(createValidExpense()));
            when(expenseRepository.deleteByIdAndReturn(expenseId)).thenReturn(true);

            boolean result = expenseService.delete(expenseId);

            assertThat(result).isTrue();
            verify(expenseRepository).deleteByIdAndReturn(expenseId);
        }

        @Test
        @DisplayName("should return false when expense not found")
        void shouldReturnFalseWhenNotFound() {
            UUID expenseId = UUID.randomUUID();
            when(expenseRepository.findByIdAsDomain(expenseId)).thenReturn(Optional.empty());

            boolean result = expenseService.delete(expenseId);

            assertThat(result).isFalse();
            verify(expenseRepository, never()).deleteByIdAndReturn(any());
        }

        @Test
        @DisplayName("should throw ValidationException when id is null")
        void shouldThrowWhenIdIsNull() {
            assertThatThrownBy(() -> expenseService.delete(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("id");
        }

        // TODO: Add test for HMRC submission link check when that feature is implemented
        // @Test
        // @DisplayName("should throw ValidationException when linked to HMRC submission")
        // void shouldThrowWhenLinkedToHmrcSubmission() {
        //     // This will be implemented when HMRC submission linking is added
        // }
    }

    @Nested
    @DisplayName("Find By Tax Year Tests")
    class FindByTaxYearTests {

        @Test
        @DisplayName("should return expenses within tax year date range")
        void shouldReturnExpensesWithinTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);
            List<Expense> expectedExpenses = List.of(createValidExpense(), createValidExpense());
            when(expenseRepository.findByDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(expectedExpenses);

            List<Expense> result = expenseService.findByTaxYear(BUSINESS_ID, taxYear);

            assertThat(result).hasSize(2);
            verify(expenseRepository).findByDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate());
        }

        @Test
        @DisplayName("should return empty list when no expenses in tax year")
        void shouldReturnEmptyWhenNoExpensesInTaxYear() {
            TaxYear taxYear = TaxYear.of(2020);
            when(expenseRepository.findByDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(List.of());

            List<Expense> result = expenseService.findByTaxYear(BUSINESS_ID, taxYear);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> expenseService.findByTaxYear(null, TAX_YEAR_2025))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }

        @Test
        @DisplayName("should throw ValidationException when taxYear is null")
        void shouldThrowWhenTaxYearIsNull() {
            assertThatThrownBy(() -> expenseService.findByTaxYear(BUSINESS_ID, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Tax year");
        }
    }

    @Nested
    @DisplayName("Find By Category Tests")
    class FindByCategoryTests {

        @Test
        @DisplayName("should return expenses by category")
        void shouldReturnExpensesByCategory() {
            List<Expense> expectedExpenses = List.of(createValidExpense());
            when(expenseRepository.findByCategory(BUSINESS_ID, ExpenseCategory.OFFICE_COSTS))
                    .thenReturn(expectedExpenses);

            List<Expense> result = expenseService.findByCategory(BUSINESS_ID, ExpenseCategory.OFFICE_COSTS);

            assertThat(result).hasSize(1);
            verify(expenseRepository).findByCategory(BUSINESS_ID, ExpenseCategory.OFFICE_COSTS);
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> expenseService.findByCategory(null, ExpenseCategory.TRAVEL))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }

        @Test
        @DisplayName("should throw ValidationException when category is null")
        void shouldThrowWhenCategoryIsNull() {
            assertThatThrownBy(() -> expenseService.findByCategory(BUSINESS_ID, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("category");
        }
    }

    @Nested
    @DisplayName("Get Total By Tax Year Tests")
    class GetTotalByTaxYearTests {

        @Test
        @DisplayName("should return total expenses for tax year")
        void shouldReturnTotalForTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);
            BigDecimal expectedTotal = new BigDecimal("2500.00");
            when(expenseRepository.calculateTotalForDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(expectedTotal);

            BigDecimal result = expenseService.getTotalByTaxYear(BUSINESS_ID, taxYear);

            assertThat(result).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("should return zero when no expenses")
        void shouldReturnZeroWhenNoExpenses() {
            TaxYear taxYear = TaxYear.of(2025);
            when(expenseRepository.calculateTotalForDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(BigDecimal.ZERO);

            BigDecimal result = expenseService.getTotalByTaxYear(BUSINESS_ID, taxYear);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> expenseService.getTotalByTaxYear(null, TAX_YEAR_2025))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }

        @Test
        @DisplayName("should throw ValidationException when taxYear is null")
        void shouldThrowWhenTaxYearIsNull() {
            assertThatThrownBy(() -> expenseService.getTotalByTaxYear(BUSINESS_ID, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Tax year");
        }
    }

    @Nested
    @DisplayName("Get Deductible Total Tests")
    class GetDeductibleTotalTests {

        @Test
        @DisplayName("should return deductible (allowable) expenses total for tax year")
        void shouldReturnDeductibleTotalForTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);
            BigDecimal expectedTotal = new BigDecimal("2000.00");
            when(expenseRepository.calculateAllowableTotalForDateRange(
                    BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(expectedTotal);

            BigDecimal result = expenseService.getDeductibleTotal(BUSINESS_ID, taxYear);

            assertThat(result).isEqualByComparingTo(expectedTotal);
            verify(expenseRepository).calculateAllowableTotalForDateRange(
                    BUSINESS_ID, taxYear.startDate(), taxYear.endDate());
        }

        @Test
        @DisplayName("should return zero when no deductible expenses")
        void shouldReturnZeroWhenNoDeductibleExpenses() {
            TaxYear taxYear = TaxYear.of(2025);
            when(expenseRepository.calculateAllowableTotalForDateRange(
                    BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(BigDecimal.ZERO);

            BigDecimal result = expenseService.getDeductibleTotal(BUSINESS_ID, taxYear);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> expenseService.getDeductibleTotal(null, TAX_YEAR_2025))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }

        @Test
        @DisplayName("should throw ValidationException when taxYear is null")
        void shouldThrowWhenTaxYearIsNull() {
            assertThatThrownBy(() -> expenseService.getDeductibleTotal(BUSINESS_ID, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Tax year");
        }
    }

    @Nested
    @DisplayName("Get Deductible Total By Quarter Tests - Sprint 10D")
    class GetDeductibleTotalByQuarterTests {

        @Test
        @DisplayName("should return deductible expenses for Q1")
        void shouldReturnDeductibleTotalForQ1() {
            TaxYear taxYear = TaxYear.of(2025);
            Quarter quarter = Quarter.Q1;
            BigDecimal expectedTotal = new BigDecimal("800.00");
            when(expenseRepository.calculateAllowableTotalForDateRange(
                    BUSINESS_ID, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear)))
                    .thenReturn(expectedTotal);

            BigDecimal result = expenseService.getDeductibleTotalByQuarter(BUSINESS_ID, taxYear, quarter);

            assertThat(result).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("should return zero when no deductible expenses in quarter")
        void shouldReturnZeroWhenNoDeductibleExpensesInQuarter() {
            TaxYear taxYear = TaxYear.of(2025);
            Quarter quarter = Quarter.Q2;
            when(expenseRepository.calculateAllowableTotalForDateRange(
                    BUSINESS_ID, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear)))
                    .thenReturn(BigDecimal.ZERO);

            BigDecimal result = expenseService.getDeductibleTotalByQuarter(BUSINESS_ID, taxYear, quarter);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> expenseService.getDeductibleTotalByQuarter(null, TAX_YEAR_2025, Quarter.Q1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }

        @Test
        @DisplayName("should throw ValidationException when taxYear is null")
        void shouldThrowWhenTaxYearIsNull() {
            assertThatThrownBy(() -> expenseService.getDeductibleTotalByQuarter(BUSINESS_ID, null, Quarter.Q1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Tax year");
        }

        @Test
        @DisplayName("should throw ValidationException when quarter is null")
        void shouldThrowWhenQuarterIsNull() {
            assertThatThrownBy(() -> expenseService.getDeductibleTotalByQuarter(BUSINESS_ID, TAX_YEAR_2025, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Quarter");
        }
    }

    @Nested
    @DisplayName("Tax Year Date Validation Edge Cases")
    class TaxYearDateValidationTests {

        @Test
        @DisplayName("should accept date at start of current tax year (6 April)")
        void shouldAcceptDateAtStartOfTaxYear() {
            // Use current tax year to ensure the date is valid
            TaxYear currentTaxYear = TaxYear.current();
            LocalDate startDate = currentTaxYear.startDate();
            Expense expectedExpense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, startDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null, null, null, null);
            when(expenseRepository.save(any(Expense.class))).thenReturn(expectedExpense);

            Expense result = expenseService.create(
                    BUSINESS_ID, startDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null);

            assertThat(result.date()).isEqualTo(startDate);
        }

        @Test
        @DisplayName("should accept date at end of previous tax year (5 April)")
        void shouldAcceptDateAtEndOfPreviousTaxYear() {
            // Use previous tax year to ensure the date is in the past (not future)
            TaxYear previousTaxYear = TaxYear.current().previous();
            LocalDate endDate = previousTaxYear.endDate();
            Expense expectedExpense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, endDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null, null, null, null);
            when(expenseRepository.save(any(Expense.class))).thenReturn(expectedExpense);

            Expense result = expenseService.create(
                    BUSINESS_ID, endDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null);

            assertThat(result.date()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("should accept date within current tax year")
        void shouldAcceptDateWithinCurrentTaxYear() {
            TaxYear currentTaxYear = TaxYear.current();
            LocalDate midDate = currentTaxYear.startDate().plusMonths(3);
            // Make sure the date is not in the future
            if (midDate.isAfter(LocalDate.now())) {
                midDate = LocalDate.now();
            }
            Expense expectedExpense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, midDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null, null, null, null);
            when(expenseRepository.save(any(Expense.class))).thenReturn(expectedExpense);

            final LocalDate testDate = midDate;
            Expense result = expenseService.create(
                    BUSINESS_ID, testDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null);

            assertThat(result.date()).isEqualTo(testDate);
        }

        @Test
        @DisplayName("should accept date within previous tax year")
        void shouldAcceptDateWithinPreviousTaxYear() {
            TaxYear previousTaxYear = TaxYear.current().previous();
            LocalDate dateInPreviousTaxYear = previousTaxYear.startDate().plusMonths(3);
            Expense expectedExpense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, dateInPreviousTaxYear, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null, null, null, null);
            when(expenseRepository.save(any(Expense.class))).thenReturn(expectedExpense);

            Expense result = expenseService.create(
                    BUSINESS_ID, dateInPreviousTaxYear, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null, null);

            assertThat(result.date()).isEqualTo(dateInPreviousTaxYear);
        }
    }

    @Nested
    @DisplayName("Allowable vs Non-Allowable Category Tests")
    class AllowableCategoryTests {

        @Test
        @DisplayName("should identify depreciation as non-allowable expense")
        void shouldIdentifyDepreciationAsNonAllowable() {
            Expense expense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION,
                    ExpenseCategory.DEPRECIATION, null, null, null, null, null);

            assertThat(expense.isAllowable()).isFalse();
        }

        @Test
        @DisplayName("should identify office costs as allowable expense")
        void shouldIdentifyOfficeCostsAsAllowable() {
            Expense expense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION,
                    ExpenseCategory.OFFICE_COSTS, null, null, null, null, null);

            assertThat(expense.isAllowable()).isTrue();
        }

        @Test
        @DisplayName("should identify travel as allowable expense")
        void shouldIdentifyTravelAsAllowable() {
            Expense expense = new Expense(
                    UUID.randomUUID(), BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION,
                    ExpenseCategory.TRAVEL, null, null, null, null, null);

            assertThat(expense.isAllowable()).isTrue();
        }
    }

    private Expense createValidExpense() {
        return new Expense(
                UUID.randomUUID(),
                BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                "/receipts/receipt.pdf",
                "Tax deductible",
                null,
                null,
                null
        );
    }
}
