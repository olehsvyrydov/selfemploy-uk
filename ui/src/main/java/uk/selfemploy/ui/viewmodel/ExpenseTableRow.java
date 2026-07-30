package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.i18n.Messages;
import uk.selfemploy.ui.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    BigDecimal allowableAmount,
    int businessUsePercentage,
    String notes,
    int receiptCount
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM ''yy");

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
            expense.allowableAmount(),
            expense.businessUsePercentage(),
            expense.notes(),
            receiptCount
        );
    }

    /**
     * Whether any of this expense may be claimed. Not the same as {@link #deductible()}, which asks
     * only whether the category is allowable: an expense in an allowable category that is marked 0%
     * business use is deductible by category and claimable for nothing.
     */
    public boolean hasClaimableAmount() {
        return allowableAmount.signum() > 0;
    }

    /**
     * Whether any of this expense cannot be claimed — a disallowed category, or the private share of
     * one marked part business use. An expense can answer true to this and {@link #hasClaimableAmount()}
     * at once, which is why the summary card counts do not add up to the number of rows.
     */
    public boolean hasNonClaimableAmount() {
        return allowableAmount.compareTo(amount) < 0;
    }

    /** Whether only part of this expense is claimed, which is what the row has to explain. */
    public boolean isPartlyClaimed() {
        return hasClaimableAmount() && hasNonClaimableAmount();
    }

    /**
     * How much of this expense reduces the tax bill, which is what the row's mark has to report.
     *
     * <p>Read from the money rather than from {@link #deductible()}, because the category answers a
     * different question. An expense in an allowable category marked 0% business use is deductible by
     * category and claims nothing, and a mark taken from the category alone tells that user their
     * whole spend reduces their bill while every total on the same screen says it does not.
     */
    public ClaimState claimState() {
        if (!hasClaimableAmount()) {
            return ClaimState.NONE;
        }
        return hasNonClaimableAmount() ? ClaimState.PARTIAL : ClaimState.FULL;
    }

    /** What a row's claim mark can say. */
    public enum ClaimState { FULL, PARTIAL, NONE }

    /**
     * The claim shown beneath the amount when it is not the whole of it, e.g. "£36.00 claimed (60%
     * business)"; empty when the amount and the claim are the same and there is nothing to explain.
     *
     * <p>The share is the one the user stated, not one derived by dividing the claim by the amount:
     * the claim is rounded to the penny, so dividing back gives 59% or 61% as often as it gives the
     * figure they typed.
     */
    public String getClaimNote() {
        if (isPartlyClaimed()) {
            return Messages.format("expenses.row.partClaimed",
                Money.format(allowableAmount), businessUsePercentage);
        }
        // An allowable category claiming nothing is the case a reader cannot work out for themselves:
        // the category column says "Office costs", so without this the row looks like an ordinary
        // claim. A disallowed category needs no note — its own name is the reason.
        if (claimState() == ClaimState.NONE && deductible) {
            return Messages.format("expenses.row.noneClaimed", businessUsePercentage);
        }
        return "";
    }

    /**
     * The whole sentence behind {@link #getClaimNote()}, which the column is too narrow to show.
     * Empty when there is no claim note.
     */
    public String getClaimTooltip() {
        if (claimState() == ClaimState.NONE && deductible) {
            return Messages.format("expenses.row.noneClaimed.tooltip",
                Money.format(amount), businessUsePercentage);
        }
        if (!isPartlyClaimed()) {
            return "";
        }
        return Messages.format("expenses.row.partClaimed.tooltip",
            Money.format(allowableAmount), Money.format(amount), businessUsePercentage);
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
        return Money.format(amount);
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
            case EQUIPMENT_CAPITAL -> "Equipment";
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
            case EQUIPMENT_CAPITAL -> "category-equipment";
            case OTHER_EXPENSES -> "category-other";
            case HOME_OFFICE_SIMPLIFIED -> "category-home-office";
            case BUSINESS_ENTERTAINMENT -> "category-entertainment";
        };
    }
}
