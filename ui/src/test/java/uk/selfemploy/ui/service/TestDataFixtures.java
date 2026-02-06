package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Test data fixtures for creating predefined test data.
 * Provides consistent, reusable test data across all tests.
 *
 * <p>Usage:
 * <pre>
 * TestDataFixtures fixtures = TestDataFixtures.forBusiness(businessId);
 * Expense expense = fixtures.createOfficeExpense("Printer paper", new BigDecimal("25.00"));
 * </pre>
 */
public final class TestDataFixtures {

    private final UUID businessId;

    private TestDataFixtures(UUID businessId) {
        this.businessId = businessId;
    }

    /**
     * Creates fixtures for the given business ID.
     */
    public static TestDataFixtures forBusiness(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("Business ID cannot be null");
        }
        return new TestDataFixtures(businessId);
    }

    /**
     * Creates fixtures with a random business ID.
     */
    public static TestDataFixtures withRandomBusiness() {
        return new TestDataFixtures(UUID.randomUUID());
    }

    /**
     * Returns the business ID for these fixtures.
     */
    public UUID getBusinessId() {
        return businessId;
    }

    // === Expense Fixtures ===

    /**
     * Creates a basic office expense.
     */
    public Expense createOfficeExpense(String description, BigDecimal amount) {
        return createExpense(description, amount, ExpenseCategory.OFFICE_COSTS, LocalDate.now().minusDays(1));
    }

    /**
     * Creates a travel expense.
     */
    public Expense createTravelExpense(String description, BigDecimal amount) {
        return createExpense(description, amount, ExpenseCategory.TRAVEL, LocalDate.now().minusDays(1));
    }

    /**
     * Creates a professional fees expense.
     */
    public Expense createProfessionalFeesExpense(String description, BigDecimal amount) {
        return createExpense(description, amount, ExpenseCategory.PROFESSIONAL_FEES, LocalDate.now().minusDays(1));
    }

    /**
     * Creates an expense with a specific category.
     */
    public Expense createExpense(String description, BigDecimal amount, ExpenseCategory category) {
        return createExpense(description, amount, category, LocalDate.now().minusDays(1));
    }

    /**
     * Creates an expense with a specific date.
     */
    public Expense createExpense(String description, BigDecimal amount, LocalDate date) {
        return createExpense(description, amount, ExpenseCategory.OFFICE_COSTS, date);
    }

    /**
     * Creates a fully customized expense.
     */
    public Expense createExpense(String description, BigDecimal amount, ExpenseCategory category, LocalDate date) {
        return new Expense(
                UUID.randomUUID(),
                businessId,
                date,
                amount,
                description,
                category,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Creates an expense with receipt and notes.
     */
    public Expense createExpenseWithReceipt(String description, BigDecimal amount,
                                            ExpenseCategory category, String receiptPath, String notes) {
        return new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.now().minusDays(1),
                amount,
                description,
                category,
                receiptPath,
                notes,
                null,
                null,
                null,
                null
        );
    }

    // === Income Fixtures ===

    /**
     * Creates a basic sales income.
     */
    public Income createSalesIncome(String description, BigDecimal amount) {
        return createIncome(description, amount, IncomeCategory.SALES, LocalDate.now().minusDays(1), null);
    }

    /**
     * Creates a sales income with reference.
     */
    public Income createSalesIncome(String description, BigDecimal amount, String reference) {
        return createIncome(description, amount, IncomeCategory.SALES, LocalDate.now().minusDays(1), reference);
    }

    /**
     * Creates other income.
     */
    public Income createOtherIncome(String description, BigDecimal amount) {
        return createIncome(description, amount, IncomeCategory.OTHER_INCOME, LocalDate.now().minusDays(1), null);
    }

    /**
     * Creates income with a specific category.
     */
    public Income createIncome(String description, BigDecimal amount, IncomeCategory category) {
        return createIncome(description, amount, category, LocalDate.now().minusDays(1), null);
    }

    /**
     * Creates income with a specific date.
     */
    public Income createIncome(String description, BigDecimal amount, LocalDate date) {
        return createIncome(description, amount, IncomeCategory.SALES, date, null);
    }

    /**
     * Creates a fully customized income.
     */
    public Income createIncome(String description, BigDecimal amount, IncomeCategory category,
                               LocalDate date, String reference) {
        return new Income(
                UUID.randomUUID(),
                businessId,
                date,
                amount,
                description,
                category,
                reference,
                null,
                null,
                null,
                null
        );
    }

    // === Bulk Data Fixtures ===

    /**
     * Creates a set of typical monthly expenses for testing.
     */
    public List<Expense> createMonthlyExpenses(TaxYear taxYear) {
        List<Expense> expenses = new ArrayList<>();
        LocalDate baseDate = taxYear.startDate();

        // Office costs spread across the year
        expenses.add(createExpense("Printer paper", new BigDecimal("25.00"), ExpenseCategory.OFFICE_COSTS, baseDate.plusMonths(1)));
        expenses.add(createExpense("Ink cartridges", new BigDecimal("45.00"), ExpenseCategory.OFFICE_COSTS, baseDate.plusMonths(2)));
        expenses.add(createExpense("Office chair", new BigDecimal("150.00"), ExpenseCategory.OFFICE_COSTS, baseDate.plusMonths(3)));

        // Travel expenses
        expenses.add(createExpense("Client meeting train", new BigDecimal("85.50"), ExpenseCategory.TRAVEL, baseDate.plusMonths(1)));
        expenses.add(createExpense("Conference travel", new BigDecimal("125.00"), ExpenseCategory.TRAVEL, baseDate.plusMonths(5)));

        // Professional fees
        expenses.add(createExpense("Accountant fees", new BigDecimal("350.00"), ExpenseCategory.PROFESSIONAL_FEES, baseDate.plusMonths(6)));
        expenses.add(createExpense("Legal consultation", new BigDecimal("200.00"), ExpenseCategory.PROFESSIONAL_FEES, baseDate.plusMonths(9)));

        // Phone and office costs (included in OFFICE_COSTS per SA103 Box 23)
        expenses.add(createExpense("Mobile phone bill", new BigDecimal("35.00"), ExpenseCategory.OFFICE_COSTS, baseDate.plusMonths(1)));
        expenses.add(createExpense("Broadband", new BigDecimal("40.00"), ExpenseCategory.OFFICE_COSTS, baseDate.plusMonths(2)));

        return expenses;
    }

    /**
     * Creates a set of typical quarterly income entries for testing.
     */
    public List<Income> createQuarterlyIncome(TaxYear taxYear) {
        List<Income> incomeList = new ArrayList<>();
        LocalDate baseDate = taxYear.startDate();

        // Q1 income
        incomeList.add(createIncome("Invoice INV-001", new BigDecimal("2500.00"), IncomeCategory.SALES, baseDate.plusMonths(1), "INV-001"));
        incomeList.add(createIncome("Invoice INV-002", new BigDecimal("3200.00"), IncomeCategory.SALES, baseDate.plusMonths(2), "INV-002"));

        // Q2 income
        incomeList.add(createIncome("Invoice INV-003", new BigDecimal("2800.00"), IncomeCategory.SALES, baseDate.plusMonths(4), "INV-003"));
        incomeList.add(createIncome("Invoice INV-004", new BigDecimal("4100.00"), IncomeCategory.SALES, baseDate.plusMonths(5), "INV-004"));

        // Q3 income
        incomeList.add(createIncome("Invoice INV-005", new BigDecimal("3500.00"), IncomeCategory.SALES, baseDate.plusMonths(7), "INV-005"));
        incomeList.add(createIncome("Consulting bonus", new BigDecimal("500.00"), IncomeCategory.OTHER_INCOME, baseDate.plusMonths(8), null));

        // Q4 income
        incomeList.add(createIncome("Invoice INV-006", new BigDecimal("4500.00"), IncomeCategory.SALES, baseDate.plusMonths(10), "INV-006"));
        incomeList.add(createIncome("Invoice INV-007", new BigDecimal("3800.00"), IncomeCategory.SALES, baseDate.plusMonths(11), "INV-007"));

        return incomeList;
    }

    /**
     * Creates a complete dataset with expenses and income for a tax year.
     * Useful for integration testing.
     */
    public TestDataSet createCompleteDataSet(TaxYear taxYear) {
        return new TestDataSet(
                createMonthlyExpenses(taxYear),
                createQuarterlyIncome(taxYear)
        );
    }

    /**
     * Creates non-allowable expenses for testing deduction calculations.
     */
    public List<Expense> createNonAllowableExpenses() {
        List<Expense> expenses = new ArrayList<>();
        LocalDate date = LocalDate.now().minusDays(30);

        expenses.add(createExpense("Client dinner", new BigDecimal("150.00"), ExpenseCategory.BUSINESS_ENTERTAINMENT, date));
        expenses.add(createExpense("Computer depreciation", new BigDecimal("500.00"), ExpenseCategory.DEPRECIATION, date.plusDays(10)));

        return expenses;
    }

    /**
     * Holds a complete test dataset with expenses and income.
     */
    public record TestDataSet(List<Expense> expenses, List<Income> income) {

        /**
         * Returns total expense amount.
         */
        public BigDecimal totalExpenses() {
            return expenses.stream()
                    .map(Expense::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /**
         * Returns total income amount.
         */
        public BigDecimal totalIncome() {
            return income.stream()
                    .map(Income::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /**
         * Returns total allowable expenses.
         */
        public BigDecimal totalAllowableExpenses() {
            return expenses.stream()
                    .filter(e -> e.category().isAllowable())
                    .map(Expense::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /**
         * Returns net profit (income - allowable expenses).
         */
        public BigDecimal netProfit() {
            return totalIncome().subtract(totalAllowableExpenses());
        }
    }

    // === Predefined Common Amounts ===

    /**
     * Common test amounts for quick access.
     */
    public static final class Amounts {
        public static final BigDecimal SMALL = new BigDecimal("25.00");
        public static final BigDecimal MEDIUM = new BigDecimal("100.00");
        public static final BigDecimal LARGE = new BigDecimal("500.00");
        public static final BigDecimal INVOICE = new BigDecimal("2500.00");

        private Amounts() {
            // Constants class
        }
    }
}
