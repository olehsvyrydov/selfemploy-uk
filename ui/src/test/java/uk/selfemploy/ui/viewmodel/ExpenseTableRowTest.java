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
    @DisplayName("a disallowed category is not 'partly' claimed — none of it is")
    void aDisallowedExpenseIsNotPartlyClaimed() {
        ExpenseTableRow row = row(new BigDecimal("79.11"), ExpenseCategory.BUSINESS_ENTERTAINMENT,
                Expense.FULLY_BUSINESS);

        assertThat(row.hasClaimableAmount()).isFalse();
        assertThat(row.hasNonClaimableAmount()).isTrue();
        assertThat(row.isPartlyClaimed())
                .as("the row is marked not claimable rather than partly claimed, so the amount "
                    + "must not carry a claim note of its own")
                .isFalse();
        assertThat(row.getClaimNote()).isEmpty();
    }

    private static ExpenseTableRow row(BigDecimal amount, ExpenseCategory category, int businessUse) {
        return ExpenseTableRow.fromExpense(new Expense(UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2025, 6, 15), amount, category.getDisplayName(), category,
                null, null, null, null, null, null, businessUse));
    }
}
