package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Totals are added up exactly, to the penny.
 *
 * <p>These are the figures a return is built from: turnover, allowable expenses, and the profit
 * between them. Adding them in binary floating point makes 10.10 three times over come to
 * 30.299999999999997, and the error grows with the number of records — which for a year of bank
 * transactions is not small.
 *
 * <p>The amounts here are chosen because they cannot be represented exactly in binary. A test using
 * round numbers would pass either way and prove nothing.
 */
@DisplayName("Money totals are exact")
class MoneyTotalsAreExactTest {

    private UUID businessId;
    private SqliteExpenseRepository expenses;
    private SqliteIncomeRepository income;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetInstance();
        businessId = UUID.randomUUID();
        expenses = new SqliteExpenseRepository(businessId);
        income = new SqliteIncomeRepository(businessId);
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.clearAllData();
    }

    private static final TaxYear TAX_YEAR = TaxYear.of(2025);
    private static final LocalDate WITHIN = LocalDate.of(2025, 6, 10);

    @Test
    @DisplayName("allowable expenses add up to the penny")
    void allowableExpenseTotalIsExact() {
        for (int i = 0; i < 3; i++) {
            expenses.save(Expense.create(businessId, WITHIN, new BigDecimal("10.10"),
                    "Stationery " + i, ExpenseCategory.OFFICE_COSTS, null, null));
        }

        assertThat(expenses.getAllowableTotalByTaxYear(TAX_YEAR))
                .as("three expenses of 10.10 come to 30.30, not 30.299999999999997")
                .isEqualByComparingTo(new BigDecimal("30.30"));
    }

    @Test
    @DisplayName("every expense, allowable or not, adds up to the penny")
    void expenseTotalIsExact() {
        expenses.save(Expense.create(businessId, WITHIN, new BigDecimal("0.10"),
                "Ten pence", ExpenseCategory.OFFICE_COSTS, null, null));
        expenses.save(Expense.create(businessId, WITHIN, new BigDecimal("0.20"),
                "Twenty pence", ExpenseCategory.OFFICE_COSTS, null, null));

        assertThat(expenses.getTotalByTaxYear(TAX_YEAR))
                .as("0.10 and 0.20 come to 0.30, not 0.30000000000000004")
                .isEqualByComparingTo(new BigDecimal("0.30"));
    }

    @Test
    @DisplayName("turnover adds up to the penny")
    void incomeTotalIsExact() {
        for (int i = 0; i < 3; i++) {
            income.save(Income.create(businessId, WITHIN, new BigDecimal("10.10"),
                    "Client payment " + i, IncomeCategory.SALES, null));
        }

        assertThat(income.getTotalByTaxYear(TAX_YEAR))
                .as("three payments of 10.10 come to 30.30")
                .isEqualByComparingTo(new BigDecimal("30.30"));
    }

    @Test
    @DisplayName("a year of small amounts does not drift")
    void manyRecordsDoNotDrift() {
        // One 0.07 on every day of the tax year: the volume a bank import produces, at an amount
        // with no exact binary form. Dated day by day from 6 April so the last falls on 5 April,
        // which also holds the range filter to the year's boundaries.
        LocalDate day = TAX_YEAR.startDate();
        int days = 0;
        while (!day.isAfter(TAX_YEAR.endDate())) {
            expenses.save(Expense.create(businessId, day, new BigDecimal("0.07"),
                    "Daily " + days, ExpenseCategory.OFFICE_COSTS, null, null));
            day = day.plusDays(1);
            days++;
        }

        assertThat(days).as("6 April to 5 April inclusive").isEqualTo(365);
        assertThat(expenses.getAllowableTotalByTaxYear(TAX_YEAR))
                .as("365 x 0.07 is 25.55 exactly")
                .isEqualByComparingTo(new BigDecimal("25.55"));
    }

    @Test
    @DisplayName("amounts outside the tax year are left out of it")
    void amountsOutsideTheYearAreExcluded() {
        expenses.save(Expense.create(businessId, TAX_YEAR.startDate().minusDays(1),
                new BigDecimal("100.00"), "Last year", ExpenseCategory.OFFICE_COSTS, null, null));
        expenses.save(Expense.create(businessId, TAX_YEAR.startDate(), new BigDecimal("10.10"),
                "First day", ExpenseCategory.OFFICE_COSTS, null, null));
        expenses.save(Expense.create(businessId, TAX_YEAR.endDate(), new BigDecimal("10.10"),
                "Last day", ExpenseCategory.OFFICE_COSTS, null, null));
        expenses.save(Expense.create(businessId, TAX_YEAR.endDate().plusDays(1),
                new BigDecimal("100.00"), "Next year", ExpenseCategory.OFFICE_COSTS, null, null));

        assertThat(expenses.getAllowableTotalByTaxYear(TAX_YEAR))
                .as("both boundary days count, and neither neighbour does")
                .isEqualByComparingTo(new BigDecimal("20.20"));
    }
}
