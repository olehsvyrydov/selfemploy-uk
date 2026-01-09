package uk.selfemploy.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IncomeCategory Enum Tests")
class IncomeCategoryTest {

    @Test
    @DisplayName("should have SALES category for box 9")
    void shouldHaveSalesCategory() {
        IncomeCategory category = IncomeCategory.SALES;

        assertThat(category.getDisplayName()).isEqualTo("Turnover from business");
        assertThat(category.getSa103Box()).isEqualTo("9");
    }

    @Test
    @DisplayName("should have OTHER_INCOME category for box 10")
    void shouldHaveOtherIncomeCategory() {
        IncomeCategory category = IncomeCategory.OTHER_INCOME;

        assertThat(category.getDisplayName()).isEqualTo("Other business income");
        assertThat(category.getSa103Box()).isEqualTo("10");
    }

    @Test
    @DisplayName("should have all required categories")
    void shouldHaveAllRequiredCategories() {
        assertThat(IncomeCategory.values()).hasSize(2);
        assertThat(IncomeCategory.values()).containsExactlyInAnyOrder(
            IncomeCategory.SALES,
            IncomeCategory.OTHER_INCOME
        );
    }
}
