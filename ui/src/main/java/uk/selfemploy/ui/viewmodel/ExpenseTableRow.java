package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Display model for expense table rows.
 * Maps the Expense domain object to UI-friendly format.
 */
public record ExpenseTableRow(
    UUID id,
    LocalDate date,
    String description,
    ExpenseCategory category,
    BigDecimal amount,
    boolean deductible,
    String notes,
    int receiptCount
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM ''yy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    /**
     * Creates an ExpenseTableRow from an Expense domain object.
     */
    public static ExpenseTableRow fromExpense(Expense expense) {
        return fromExpense(expense, 0);
    }

    /**
     * Creates an ExpenseTableRow from an Expense domain object with receipt count.
     */
    public static ExpenseTableRow fromExpense(Expense expense, int receiptCount) {
        return new ExpenseTableRow(
            expense.id(),
            expense.date(),
            expense.description(),
            expense.category(),
            expense.amount(),
            expense.isAllowable(),
            expense.notes(),
            receiptCount
        );
    }

    /**
     * Returns the formatted date for display (e.g., "10 Jan '26").
     */
    public String getFormattedDate() {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Returns the formatted amount with GBP symbol (e.g., "£54.99").
     */
    public String getFormattedAmount() {
        return CURRENCY_FORMAT.format(amount);
    }

    /**
     * Returns the category display name with SA103 box number (e.g., "Office costs (Box 23)").
     */
    public String getCategoryDisplayName() {
        return category.getDisplayName() + " (Box " + category.getSa103Box() + ")";
    }

    /**
     * Returns a short category name for table display.
     */
    public String getCategoryShortName() {
        return switch (category) {
            case COST_OF_GOODS -> "Goods";
            case SUBCONTRACTOR_COSTS -> "Subcontr.";
            case STAFF_COSTS -> "Staff";
            case TRAVEL, TRAVEL_MILEAGE -> "Travel";
            case PREMISES -> "Premises";
            case REPAIRS -> "Repairs";
            case OFFICE_COSTS -> "Office";
            case ADVERTISING -> "Advert.";
            case INTEREST -> "Interest";
            case FINANCIAL_CHARGES -> "Finance";
            case BAD_DEBTS -> "Bad Debt";
            case PROFESSIONAL_FEES -> "Prof. Fees";
            case DEPRECIATION -> "Deprec.";
            case OTHER_EXPENSES -> "Other";
            case HOME_OFFICE_SIMPLIFIED -> "Home";
            case BUSINESS_ENTERTAINMENT -> "Entertain.";
        };
    }

    /**
     * Returns the CSS class for the category color dot.
     */
    public String getCategoryStyleClass() {
        return switch (category) {
            case COST_OF_GOODS -> "category-cost-of-goods";
            case SUBCONTRACTOR_COSTS -> "category-subcontractor";
            case STAFF_COSTS -> "category-staff";
            case TRAVEL, TRAVEL_MILEAGE -> "category-travel";
            case PREMISES -> "category-premises";
            case REPAIRS -> "category-repairs";
            case OFFICE_COSTS -> "category-office";
            case ADVERTISING -> "category-advertising";
            case INTEREST -> "category-interest";
            case FINANCIAL_CHARGES -> "category-financial";
            case BAD_DEBTS -> "category-bad-debts";
            case PROFESSIONAL_FEES -> "category-professional";
            case DEPRECIATION -> "category-depreciation";
            case OTHER_EXPENSES -> "category-other";
            case HOME_OFFICE_SIMPLIFIED -> "category-home-office";
            case BUSINESS_ENTERTAINMENT -> "category-entertainment";
        };
    }
}
