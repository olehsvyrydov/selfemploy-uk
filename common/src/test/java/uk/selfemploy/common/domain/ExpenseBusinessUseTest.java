package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The part of an expense that may be claimed.
 *
 * <p>A phone bill used 60% for work is not a £60 business expense; £36 of it is. The app records the
 * split the user asserts and does the arithmetic, so the claim is a stated proportion of a real
 * amount rather than a number typed from memory — and so the original amount still matches the
 * receipt and the bank line it came from.
 */
@DisplayName("Expense - the share that is business use")
class ExpenseBusinessUseTest {

    private static final UUID BUSINESS = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2025, 6, 10);

    private static Expense of(String amount, int businessUse) {
        return Expense.create(BUSINESS, DATE, new BigDecimal(amount), "Phone bill",
                ExpenseCategory.OFFICE_COSTS, null, null)
                .withBusinessUsePercentage(businessUse);
    }

    @Test
    @DisplayName("an expense with no split stated is wholly business")
    void defaultsToFullyBusiness() {
        Expense expense = Expense.create(BUSINESS, DATE, new BigDecimal("60.00"), "Phone bill",
                ExpenseCategory.OFFICE_COSTS, null, null);

        assertThat(expense.businessUsePercentage()).isEqualTo(100);
        assertThat(expense.allowableAmount()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("the claimable share is the stated percentage of the amount")
    void apportionsByPercentage() {
        assertThat(of("60.00", 60).allowableAmount()).isEqualByComparingTo(new BigDecimal("36.00"));
        assertThat(of("899.00", 50).allowableAmount()).isEqualByComparingTo(new BigDecimal("449.50"));
    }

    @Test
    @DisplayName("a share that does not divide evenly is rounded to the penny, half up")
    void roundsToThePenny() {
        assertThat(of("10.01", 33).allowableAmount())
                .as("33%% of 10.01 is 3.3033, and a claim has to be a real number of pence")
                .isEqualByComparingTo(new BigDecimal("3.30"));
        assertThat(of("6.67", 50).allowableAmount())
                .as("3.335 rounds up, not to even")
                .isEqualByComparingTo(new BigDecimal("3.34"));
    }

    @Test
    @DisplayName("a wholly-business amount is not rounded, so adding the column changes no total")
    void aWhollyBusinessAmountIsUntouched() {
        Expense threeDecimals = Expense.create(BUSINESS, DATE, new BigDecimal("10.005"), "Imported",
                ExpenseCategory.OFFICE_COSTS, null, null);

        assertThat(threeDecimals.allowableAmount())
                .as("imports carry whatever precision the file holds; rounding it would move a total "
                    + "that was right before this column existed")
                .isEqualByComparingTo(new BigDecimal("10.005"));
        assertThat(threeDecimals.allowableAmount()).isEqualTo(threeDecimals.amount());
    }

    @Test
    @DisplayName("nothing is claimable at nought percent")
    void zeroPercentClaimsNothing() {
        assertThat(of("60.00", 0).allowableAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("the amount itself is never changed by the split")
    void theRecordedAmountIsUntouched() {
        Expense expense = of("60.00", 60);

        assertThat(expense.amount())
                .as("the amount still has to match the receipt and the bank line")
                .isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("a percentage outside 0 to 100 is refused")
    void refusesAnImpossibleShare() {
        assertThatThrownBy(() -> of("60.00", 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 100");
        assertThatThrownBy(() -> of("60.00", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 100");
    }

    @Test
    @DisplayName("a category that is not allowable claims nothing, whatever the split says")
    void aNonAllowableCategoryClaimsNothing() {
        Expense depreciation = Expense.create(BUSINESS, DATE, new BigDecimal("899.00"), "Laptop",
                ExpenseCategory.DEPRECIATION, null, null).withBusinessUsePercentage(100);

        assertThat(depreciation.allowableAmount())
                .as("depreciation is never an allowable expense; capital allowances are a separate claim")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
