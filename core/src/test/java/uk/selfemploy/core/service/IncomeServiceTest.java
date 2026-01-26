package uk.selfemploy.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.exception.ValidationException;
import uk.selfemploy.persistence.repository.IncomeRepository;

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
 * Unit tests for IncomeService.
 *
 * Tests cover:
 * - CRUD operations (create, findById, update, delete)
 * - Validation: date within tax year, amount positive, description required (max 100 chars)
 * - Query methods: findByTaxYear, findByCategory, getTotalByTaxYear
 * - Business rules: cannot delete if linked to HMRC submission (future feature)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IncomeService Tests")
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    private IncomeService incomeService;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final TaxYear TAX_YEAR_2025 = TaxYear.of(2025);
    private static final LocalDate VALID_DATE = LocalDate.of(2025, 6, 15); // Within 2025/26 tax year
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("1000.00");
    private static final String VALID_DESCRIPTION = "Consulting services";
    private static final IncomeCategory VALID_CATEGORY = IncomeCategory.SALES;

    @BeforeEach
    void setUp() {
        incomeService = new IncomeService(incomeRepository);
    }

    @Nested
    @DisplayName("Create Income Tests")
    class CreateIncomeTests {

        @Test
        @DisplayName("should create income with valid data")
        void shouldCreateIncomeWithValidData() {
            Income expectedIncome = createValidIncome();
            when(incomeRepository.save(any(Income.class))).thenReturn(expectedIncome);

            Income result = incomeService.create(
                    BUSINESS_ID,
                    VALID_DATE,
                    VALID_AMOUNT,
                    VALID_DESCRIPTION,
                    VALID_CATEGORY,
                    "REF-001"
            );

            assertThat(result).isNotNull();
            assertThat(result.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(result.amount()).isEqualByComparingTo(VALID_AMOUNT);
            assertThat(result.description()).isEqualTo(VALID_DESCRIPTION);
            verify(incomeRepository).save(any(Income.class));
        }

        @Test
        @DisplayName("should throw ValidationException when date is null")
        void shouldThrowWhenDateIsNull() {
            assertThatThrownBy(() -> incomeService.create(
                    BUSINESS_ID, null, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("date");
        }

        @Test
        @DisplayName("should accept date in any valid tax year range (2000-2100)")
        void shouldAcceptDateInValidTaxYearRange() {
            // Any date between 2000 and 2100 is considered valid for tax year purposes
            LocalDate dateInPast = LocalDate.of(2020, 6, 15);
            Income expectedIncome = new Income(
                    UUID.randomUUID(), BUSINESS_ID, dateInPast, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null,
                    null, null, null);
            when(incomeRepository.save(any(Income.class))).thenReturn(expectedIncome);

            Income result = incomeService.create(
                    BUSINESS_ID, dateInPast, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null);

            assertThat(result.date()).isEqualTo(dateInPast);
        }

        @Test
        @DisplayName("should throw ValidationException when amount is null")
        void shouldThrowWhenAmountIsNull() {
            assertThatThrownBy(() -> incomeService.create(
                    BUSINESS_ID, VALID_DATE, null, VALID_DESCRIPTION, VALID_CATEGORY, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("amount");
        }

        @Test
        @DisplayName("should throw ValidationException when amount is zero")
        void shouldThrowWhenAmountIsZero() {
            assertThatThrownBy(() -> incomeService.create(
                    BUSINESS_ID, VALID_DATE, BigDecimal.ZERO, VALID_DESCRIPTION, VALID_CATEGORY, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("amount")
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("should throw ValidationException when amount is negative")
        void shouldThrowWhenAmountIsNegative() {
            assertThatThrownBy(() -> incomeService.create(
                    BUSINESS_ID, VALID_DATE, new BigDecimal("-100"), VALID_DESCRIPTION, VALID_CATEGORY, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("amount")
                    .hasMessageContaining("positive");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("should throw ValidationException when description is null or blank")
        void shouldThrowWhenDescriptionIsNullOrBlank(String description) {
            assertThatThrownBy(() -> incomeService.create(
                    BUSINESS_ID, VALID_DATE, VALID_AMOUNT, description, VALID_CATEGORY, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("should throw ValidationException when description exceeds 100 characters")
        void shouldThrowWhenDescriptionExceeds100Chars() {
            String longDescription = "A".repeat(101);

            assertThatThrownBy(() -> incomeService.create(
                    BUSINESS_ID, VALID_DATE, VALID_AMOUNT, longDescription, VALID_CATEGORY, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("description")
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("should accept description with exactly 100 characters")
        void shouldAcceptDescriptionWith100Chars() {
            String maxDescription = "A".repeat(100);
            Income expectedIncome = new Income(
                    UUID.randomUUID(), BUSINESS_ID, VALID_DATE, VALID_AMOUNT, maxDescription, VALID_CATEGORY, null,
                    null, null, null);
            when(incomeRepository.save(any(Income.class))).thenReturn(expectedIncome);

            Income result = incomeService.create(
                    BUSINESS_ID, VALID_DATE, VALID_AMOUNT, maxDescription, VALID_CATEGORY, null);

            assertThat(result.description()).hasSize(100);
        }

        @Test
        @DisplayName("should throw ValidationException when category is null")
        void shouldThrowWhenCategoryIsNull() {
            assertThatThrownBy(() -> incomeService.create(
                    BUSINESS_ID, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, null, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("category");
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> incomeService.create(
                    null, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("should return income when found")
        void shouldReturnIncomeWhenFound() {
            UUID incomeId = UUID.randomUUID();
            Income expectedIncome = createValidIncome();
            when(incomeRepository.findByIdAsDomain(incomeId)).thenReturn(Optional.of(expectedIncome));

            Optional<Income> result = incomeService.findById(incomeId);

            assertThat(result).isPresent();
            assertThat(result.get().businessId()).isEqualTo(BUSINESS_ID);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            UUID incomeId = UUID.randomUUID();
            when(incomeRepository.findByIdAsDomain(incomeId)).thenReturn(Optional.empty());

            Optional<Income> result = incomeService.findById(incomeId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ValidationException when id is null")
        void shouldThrowWhenIdIsNull() {
            assertThatThrownBy(() -> incomeService.findById(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("id");
        }
    }

    @Nested
    @DisplayName("Update Income Tests")
    class UpdateIncomeTests {

        @Test
        @DisplayName("should update income with valid data")
        void shouldUpdateIncomeWithValidData() {
            Income existingIncome = createValidIncome();
            Income updatedIncome = new Income(
                    existingIncome.id(), BUSINESS_ID, VALID_DATE, new BigDecimal("2000.00"),
                    "Updated description", IncomeCategory.OTHER_INCOME, "REF-002",
                    null, null, null);

            when(incomeRepository.findByIdAsDomain(existingIncome.id())).thenReturn(Optional.of(existingIncome));
            when(incomeRepository.update(any(Income.class))).thenReturn(updatedIncome);

            Income result = incomeService.update(
                    existingIncome.id(),
                    VALID_DATE,
                    new BigDecimal("2000.00"),
                    "Updated description",
                    IncomeCategory.OTHER_INCOME,
                    "REF-002"
            );

            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(result.description()).isEqualTo("Updated description");
            verify(incomeRepository).update(any(Income.class));
        }

        @Test
        @DisplayName("should throw ValidationException when income not found")
        void shouldThrowWhenIncomeNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(incomeRepository.findByIdAsDomain(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incomeService.update(
                    nonExistentId, VALID_DATE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("should validate data on update")
        void shouldValidateDataOnUpdate() {
            UUID incomeId = UUID.randomUUID();
            Income existingIncome = createValidIncome();
            when(incomeRepository.findByIdAsDomain(incomeId)).thenReturn(Optional.of(existingIncome));

            assertThatThrownBy(() -> incomeService.update(
                    incomeId, VALID_DATE, BigDecimal.ZERO, VALID_DESCRIPTION, VALID_CATEGORY, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("amount");
        }
    }

    @Nested
    @DisplayName("Delete Income Tests")
    class DeleteIncomeTests {

        @Test
        @DisplayName("should delete income successfully")
        void shouldDeleteIncomeSuccessfully() {
            UUID incomeId = UUID.randomUUID();
            when(incomeRepository.findByIdAsDomain(incomeId)).thenReturn(Optional.of(createValidIncome()));
            when(incomeRepository.deleteByIdAndReturn(incomeId)).thenReturn(true);

            boolean result = incomeService.delete(incomeId);

            assertThat(result).isTrue();
            verify(incomeRepository).deleteByIdAndReturn(incomeId);
        }

        @Test
        @DisplayName("should return false when income not found")
        void shouldReturnFalseWhenNotFound() {
            UUID incomeId = UUID.randomUUID();
            when(incomeRepository.findByIdAsDomain(incomeId)).thenReturn(Optional.empty());

            boolean result = incomeService.delete(incomeId);

            assertThat(result).isFalse();
            verify(incomeRepository, never()).deleteByIdAndReturn(any());
        }

        @Test
        @DisplayName("should throw ValidationException when id is null")
        void shouldThrowWhenIdIsNull() {
            assertThatThrownBy(() -> incomeService.delete(null))
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
        @DisplayName("should return incomes within tax year date range")
        void shouldReturnIncomesWithinTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);
            List<Income> expectedIncomes = List.of(createValidIncome(), createValidIncome());
            when(incomeRepository.findByDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(expectedIncomes);

            List<Income> result = incomeService.findByTaxYear(BUSINESS_ID, taxYear);

            assertThat(result).hasSize(2);
            verify(incomeRepository).findByDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate());
        }

        @Test
        @DisplayName("should return empty list when no incomes in tax year")
        void shouldReturnEmptyWhenNoIncomesInTaxYear() {
            TaxYear taxYear = TaxYear.of(2020);
            when(incomeRepository.findByDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(List.of());

            List<Income> result = incomeService.findByTaxYear(BUSINESS_ID, taxYear);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> incomeService.findByTaxYear(null, TAX_YEAR_2025))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }

        @Test
        @DisplayName("should throw ValidationException when taxYear is null")
        void shouldThrowWhenTaxYearIsNull() {
            assertThatThrownBy(() -> incomeService.findByTaxYear(BUSINESS_ID, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Tax year");
        }
    }

    @Nested
    @DisplayName("Find By Category Tests")
    class FindByCategoryTests {

        @Test
        @DisplayName("should return incomes by category")
        void shouldReturnIncomesByCategory() {
            List<Income> expectedIncomes = List.of(createValidIncome());
            when(incomeRepository.findByCategory(BUSINESS_ID, IncomeCategory.SALES))
                    .thenReturn(expectedIncomes);

            List<Income> result = incomeService.findByCategory(BUSINESS_ID, IncomeCategory.SALES);

            assertThat(result).hasSize(1);
            verify(incomeRepository).findByCategory(BUSINESS_ID, IncomeCategory.SALES);
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> incomeService.findByCategory(null, IncomeCategory.SALES))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }

        @Test
        @DisplayName("should throw ValidationException when category is null")
        void shouldThrowWhenCategoryIsNull() {
            assertThatThrownBy(() -> incomeService.findByCategory(BUSINESS_ID, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("category");
        }
    }

    @Nested
    @DisplayName("Get Total By Tax Year Tests")
    class GetTotalByTaxYearTests {

        @Test
        @DisplayName("should return total income for tax year")
        void shouldReturnTotalForTaxYear() {
            TaxYear taxYear = TaxYear.of(2025);
            BigDecimal expectedTotal = new BigDecimal("5000.00");
            when(incomeRepository.calculateTotalForDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(expectedTotal);

            BigDecimal result = incomeService.getTotalByTaxYear(BUSINESS_ID, taxYear);

            assertThat(result).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("should return zero when no incomes")
        void shouldReturnZeroWhenNoIncomes() {
            TaxYear taxYear = TaxYear.of(2025);
            when(incomeRepository.calculateTotalForDateRange(BUSINESS_ID, taxYear.startDate(), taxYear.endDate()))
                    .thenReturn(BigDecimal.ZERO);

            BigDecimal result = incomeService.getTotalByTaxYear(BUSINESS_ID, taxYear);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> incomeService.getTotalByTaxYear(null, TAX_YEAR_2025))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }

        @Test
        @DisplayName("should throw ValidationException when taxYear is null")
        void shouldThrowWhenTaxYearIsNull() {
            assertThatThrownBy(() -> incomeService.getTotalByTaxYear(BUSINESS_ID, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Tax year");
        }
    }

    @Nested
    @DisplayName("Get Total By Quarter Tests - Sprint 10D")
    class GetTotalByQuarterTests {

        @Test
        @DisplayName("should return total income for Q1")
        void shouldReturnTotalForQ1() {
            TaxYear taxYear = TaxYear.of(2025);
            Quarter quarter = Quarter.Q1;
            BigDecimal expectedTotal = new BigDecimal("3000.00");
            when(incomeRepository.calculateTotalForDateRange(
                    BUSINESS_ID, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear)))
                    .thenReturn(expectedTotal);

            BigDecimal result = incomeService.getTotalByQuarter(BUSINESS_ID, taxYear, quarter);

            assertThat(result).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("should return zero when no incomes in quarter")
        void shouldReturnZeroWhenNoIncomesInQuarter() {
            TaxYear taxYear = TaxYear.of(2025);
            Quarter quarter = Quarter.Q3;
            when(incomeRepository.calculateTotalForDateRange(
                    BUSINESS_ID, quarter.getStartDate(taxYear), quarter.getEndDate(taxYear)))
                    .thenReturn(BigDecimal.ZERO);

            BigDecimal result = incomeService.getTotalByQuarter(BUSINESS_ID, taxYear, quarter);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw ValidationException when businessId is null")
        void shouldThrowWhenBusinessIdIsNull() {
            assertThatThrownBy(() -> incomeService.getTotalByQuarter(null, TAX_YEAR_2025, Quarter.Q1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Business");
        }

        @Test
        @DisplayName("should throw ValidationException when taxYear is null")
        void shouldThrowWhenTaxYearIsNull() {
            assertThatThrownBy(() -> incomeService.getTotalByQuarter(BUSINESS_ID, null, Quarter.Q1))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Tax year");
        }

        @Test
        @DisplayName("should throw ValidationException when quarter is null")
        void shouldThrowWhenQuarterIsNull() {
            assertThatThrownBy(() -> incomeService.getTotalByQuarter(BUSINESS_ID, TAX_YEAR_2025, null))
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
            Income expectedIncome = new Income(
                    UUID.randomUUID(), BUSINESS_ID, startDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null,
                    null, null, null);
            when(incomeRepository.save(any(Income.class))).thenReturn(expectedIncome);

            Income result = incomeService.create(
                    BUSINESS_ID, startDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null);

            assertThat(result.date()).isEqualTo(startDate);
        }

        @Test
        @DisplayName("should accept date at end of previous tax year (5 April)")
        void shouldAcceptDateAtEndOfPreviousTaxYear() {
            // Use previous tax year to ensure the date is in the past
            TaxYear previousTaxYear = TaxYear.current().previous();
            LocalDate endDate = previousTaxYear.endDate();
            Income expectedIncome = new Income(
                    UUID.randomUUID(), BUSINESS_ID, endDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null,
                    null, null, null);
            when(incomeRepository.save(any(Income.class))).thenReturn(expectedIncome);

            Income result = incomeService.create(
                    BUSINESS_ID, endDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null);

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
            Income expectedIncome = new Income(
                    UUID.randomUUID(), BUSINESS_ID, midDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null,
                    null, null, null);
            when(incomeRepository.save(any(Income.class))).thenReturn(expectedIncome);

            final LocalDate testDate = midDate;
            Income result = incomeService.create(
                    BUSINESS_ID, testDate, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null);

            assertThat(result.date()).isEqualTo(testDate);
        }

        @Test
        @DisplayName("should accept date within previous tax year")
        void shouldAcceptDateWithinPreviousTaxYear() {
            TaxYear previousTaxYear = TaxYear.current().previous();
            LocalDate dateInPreviousTaxYear = previousTaxYear.startDate().plusMonths(3);
            Income expectedIncome = new Income(
                    UUID.randomUUID(), BUSINESS_ID, dateInPreviousTaxYear, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null,
                    null, null, null);
            when(incomeRepository.save(any(Income.class))).thenReturn(expectedIncome);

            Income result = incomeService.create(
                    BUSINESS_ID, dateInPreviousTaxYear, VALID_AMOUNT, VALID_DESCRIPTION, VALID_CATEGORY, null);

            assertThat(result.date()).isEqualTo(dateInPreviousTaxYear);
        }
    }

    private Income createValidIncome() {
        return new Income(
                UUID.randomUUID(),
                BUSINESS_ID,
                VALID_DATE,
                VALID_AMOUNT,
                VALID_DESCRIPTION,
                VALID_CATEGORY,
                "REF-001",
                null,
                null,
                null
        );
    }
}
