package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What an expense row tells a user about its own claim.
 *
 * <p>A row shows the amount that left the bank account, because that is what reconciles against a
 * statement. For a part-business expense that figure is not what reduces profit, so the row has to say
 * so — otherwise the expenses list and the tax it produces disagree with nothing to explain the gap.
 */
@DisplayName("Expense row: what is claimed of the amount")
class ExpenseTableRowTest {

    @Test
    @DisplayName("a wholly business expense has nothing to explain")
    void aFullyBusinessExpenseCarriesNoNote() {
        ExpenseTableRow row = row(new BigDecimal("60.00"), ExpenseCategory.OFFICE_COSTS,
                Expense.FULLY_BUSINESS);

        assertThat(row.isPartlyClaimed()).isFalse();
        assertThat(row.getClaimNote())
                .as("repeating the amount as the claim would be noise on every ordinary row")
                .isEmpty();
    }

    @Test
    @DisplayName("a part-business expense shows what is claimed and at what share")
    void aPartBusinessExpenseNamesItsClaim() {
        ExpenseTableRow row = row(new BigDecimal("60.00"), ExpenseCategory.OFFICE_COSTS, 60);

        assertThat(row.isPartlyClaimed()).isTrue();
        assertThat(row.getClaimNote()).contains("36.00").contains("60%");
    }

    @Test
    @DisplayName("the share shown is the one the user stated, not one divided back out")
    void theShareIsTheStatedOne() {
        // £0.03 at 50% claims £0.02 after rounding half up, so dividing the claim back out reports
        // 67% business — a figure the user never entered.
        ExpenseTableRow row = row(new BigDecimal("0.03"), ExpenseCategory.OFFICE_COSTS, 50);

        assertThat(row.allowableAmount()).isEqualByComparingTo(new BigDecimal("0.02"));
        assertThat(row.isPartlyClaimed()).isTrue();
        assertThat(row.getClaimNote())
                .as("derived from the claim this would read 67%, contradicting the entry")
                .contains("50%")
                .doesNotContain("67%");
    }

    @Test
    @DisplayName("an allowable category at 0% business use claims nothing, and says so")
    void anAllowableCategoryAtZeroPercentClaimsNothing() {
        // Reachable from the UI: the dialog accepts 0, and the category is deductible, so the row
        // looks like an ordinary claim unless it is marked otherwise.
        ExpenseTableRow row = row(new BigDecimal("600.00"), ExpenseCategory.OFFICE_COSTS, 0);

        assertThat(row.deductible())
                .as("the category permits a claim, which is exactly why the row cannot take its "
                    + "mark from the category")
                .isTrue();
        assertThat(row.allowableAmount()).isZero();
        assertThat(row.claimState())
                .as("a tick here would tell the user £600.00 reduces their bill while the CLAIMABLE "
                    + "card excludes it and the Tax Summary calls it not claimable")
                .isEqualTo(ExpenseTableRow.ClaimState.NONE);
        assertThat(row.getClaimNote()).contains("0%");
    }

    @Test
    @DisplayName("a wholly business allowable expense is claimed in full")
    void aWhollyBusinessAllowableExpenseIsClaimedInFull() {
        ExpenseTableRow row = row(new BigDecimal("500.00"), ExpenseCategory.OFFICE_COSTS,
                Expense.FULLY_BUSINESS);

        assertThat(row.claimState()).isEqualTo(ExpenseTableRow.ClaimState.FULL);
    }

    @Test
    @DisplayName("a part-business expense is marked as claimed in part")
    void aPartBusinessExpenseIsMarkedPartial() {
        ExpenseTableRow row = row(new BigDecimal("60.00"), ExpenseCategory.OFFICE_COSTS, 60);

        assertThat(row.claimState()).isEqualTo(ExpenseTableRow.ClaimState.PARTIAL);
    }

    @Test
    @DisplayName("a disallowed category is not 'partly' claimed — none of it is")
    void aDisallowedExpenseIsNotPartlyClaimed() {
        ExpenseTableRow row = row(new BigDecimal("79.11"), ExpenseCategory.BUSINESS_ENTERTAINMENT,
                Expense.FULLY_BUSINESS);

        assertThat(row.hasClaimableAmount()).isFalse();
        assertThat(row.hasNonClaimableAmount()).isTrue();
        assertThat(row.isPartlyClaimed())
                .as("the row is marked not claimable rather than partly claimed")
                .isFalse();
        assertThat(row.claimState()).isEqualTo(ExpenseTableRow.ClaimState.NONE);
        assertThat(row.getClaimNote())
                .as("the category name is itself the reason, so a note would only repeat it — "
                    + "unlike an allowable category claiming nothing, which looks ordinary")
                .isEmpty();
    }

    private static ExpenseTableRow row(BigDecimal amount, ExpenseCategory category, int businessUse) {
        return ExpenseTableRow.fromExpense(new Expense(UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2025, 6, 15), amount, category.getDisplayName(), category,
                null, null, null, null, null, null, businessUse));
    }
}
