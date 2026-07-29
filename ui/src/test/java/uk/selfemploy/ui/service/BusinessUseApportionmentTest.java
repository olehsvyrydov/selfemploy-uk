package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A partly-business expense reaches the return as the part that is business.
 *
 * <p>The phone bill is the case this exists for: £60 used 60% for work is a £36 claim, and the £60
 * still has to match the bank line it came from. Both facts have to survive being written to the
 * database and read back, because that is where a claim turns into a figure on a return.
 */
@DisplayName("Business-use apportionment")
class BusinessUseApportionmentTest {

    private static final TaxYear TAX_YEAR = TaxYear.of(2025);
    private static final LocalDate WITHIN = LocalDate.of(2025, 6, 10);

    private UUID businessId;
    private SqliteExpenseRepository expenses;

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
    }

    @AfterEach
    void tearDown() {
        SqliteTestSupport.clearAllData();
    }

    private Expense phoneBill(String amount, int businessUse) {
        return Expense.create(businessId, WITHIN, new BigDecimal(amount), "Phone bill",
                ExpenseCategory.OFFICE_COSTS, null, null).withBusinessUsePercentage(businessUse);
    }

    @Test
    @DisplayName("the stated share is stored and read back")
    void theShareSurvivesTheDatabase() {
        Expense saved = expenses.save(phoneBill("60.00", 60));

        Expense read = expenses.findById(saved.id()).orElseThrow();

        assertThat(read.businessUsePercentage()).isEqualTo(60);
        assertThat(read.amount())
                .as("the full amount is what the receipt and the bank line show")
                .isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(read.allowableAmount()).isEqualByComparingTo(new BigDecimal("36.00"));
    }

    @Test
    @DisplayName("an expense saved without a share reads back as wholly business")
    void anUnapportionedExpenseIsWhollyBusiness() {
        Expense saved = expenses.save(Expense.create(businessId, WITHIN, new BigDecimal("42.00"),
                "Stationery", ExpenseCategory.OFFICE_COSTS, null, null));

        assertThat(expenses.findById(saved.id()).orElseThrow().businessUsePercentage()).isEqualTo(100);
    }

    @Test
    @DisplayName("the allowable total counts the business share, not the whole bill")
    void theTotalCountsOnlyTheBusinessShare() {
        expenses.save(phoneBill("60.00", 60));
        expenses.save(Expense.create(businessId, WITHIN, new BigDecimal("40.00"),
                "Stationery", ExpenseCategory.OFFICE_COSTS, null, null));

        assertThat(expenses.getAllowableTotalByTaxYear(TAX_YEAR))
                .as("36.00 of the phone bill plus 40.00 of stationery")
                .isEqualByComparingTo(new BigDecimal("76.00"));
    }

    @Test
    @DisplayName("the total is the sum of the rounded shares, so it matches its own breakdown")
    void theTotalMatchesItsBreakdown() {
        for (int i = 0; i < 3; i++) {
            expenses.save(Expense.create(businessId, WITHIN, new BigDecimal("10.01"),
                    "Shared cost " + i, ExpenseCategory.OFFICE_COSTS, null, null)
                    .withBusinessUsePercentage(33));
        }

        assertThat(expenses.getAllowableTotalByTaxYear(TAX_YEAR))
                .as("three shares of 3.30, not one apportionment of the 30.03 total, which is 9.91")
                .isEqualByComparingTo(new BigDecimal("9.90"));
    }

    @Test
    @DisplayName("editing an expense leaves its business share alone")
    void editingKeepsTheShare() {
        SqliteExpenseService service = new SqliteExpenseService(businessId);
        Expense saved = expenses.save(phoneBill("60.00", 60));

        Expense edited = service.update(saved.id(), saved.date(), saved.amount(),
                "Phone bill - corrected", saved.category(), null, null);

        assertThat(edited.businessUsePercentage())
                .as("an edit that says nothing about the share must not silently claim the whole bill")
                .isEqualTo(60);
        assertThat(expenses.findById(saved.id()).orElseThrow().businessUsePercentage()).isEqualTo(60);
    }

    @Test
    @DisplayName("the category breakdown apportions too, so the screens cannot disagree")
    void theCategoryBreakdownApportions() {
        expenses.save(phoneBill("60.00", 60));

        assertThat(expenses.getTotalsByCategoryForTaxYear(TAX_YEAR))
                .containsEntry(ExpenseCategory.OFFICE_COSTS, new BigDecimal("36.00"));
        assertThat(expenses.getTotalsByCategoryForTaxYear(TAX_YEAR).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .as("the breakdown adds up to the same allowable total")
                .isEqualByComparingTo(expenses.getAllowableTotalByTaxYear(TAX_YEAR));
    }

    @Test
    @DisplayName("the in-memory store answers the same as the database one")
    void bothImplementationsAgree() {
        InMemoryExpenseRepository inMemory = new InMemoryExpenseRepository();
        Expense apportioned = phoneBill("60.00", 60);
        Expense whole = Expense.create(businessId, WITHIN, new BigDecimal("40.00"), "Stationery",
                ExpenseCategory.OFFICE_COSTS, null, null);
        Expense laptop = Expense.create(businessId, WITHIN, new BigDecimal("899.00"), "Laptop",
                ExpenseCategory.EQUIPMENT_CAPITAL, null, null);

        for (Expense expense : java.util.List.of(apportioned, whole, laptop)) {
            expenses.save(expense);
            inMemory.save(expense);
        }

        assertThat(inMemory.calculateAllowableTotalForDateRange(
                businessId, TAX_YEAR.startDate(), TAX_YEAR.endDate()))
                .isEqualByComparingTo(expenses.getAllowableTotalByTaxYear(TAX_YEAR));
    }

    @Test
    @DisplayName("equipment is recorded but not claimed as an expense")
    void equipmentIsKeptOutOfTheAllowableTotal() {
        expenses.save(Expense.create(businessId, WITHIN, new BigDecimal("899.00"),
                "Laptop", ExpenseCategory.EQUIPMENT_CAPITAL, null, null));
        expenses.save(Expense.create(businessId, WITHIN, new BigDecimal("40.00"),
                "Stationery", ExpenseCategory.OFFICE_COSTS, null, null));

        assertThat(expenses.getAllowableTotalByTaxYear(TAX_YEAR))
                .as("the laptop is a capital allowance claim, not an expense deduction")
                .isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(expenses.getTotalByTaxYear(TAX_YEAR))
                .as("but it is still on record, and still spent")
                .isEqualByComparingTo(new BigDecimal("939.00"));
    }
}
