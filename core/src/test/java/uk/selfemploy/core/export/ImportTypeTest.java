package uk.selfemploy.core.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ImportType enum.
 * Tests enum values and behavior.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("ImportType")
class ImportTypeTest {

    @Nested
    @DisplayName("Enum Values")
    class EnumValues {

        @Test
        @DisplayName("should have INCOME value")
        void shouldHaveIncomeValue() {
            assertThat(ImportType.INCOME).isNotNull();
            assertThat(ImportType.INCOME.name()).isEqualTo("INCOME");
        }

        @Test
        @DisplayName("should have EXPENSE value")
        void shouldHaveExpenseValue() {
            assertThat(ImportType.EXPENSE).isNotNull();
            assertThat(ImportType.EXPENSE.name()).isEqualTo("EXPENSE");
        }

        @Test
        @DisplayName("should have exactly two values")
        void shouldHaveExactlyTwoValues() {
            assertThat(ImportType.values()).hasSize(2);
        }

        @Test
        @DisplayName("values should be in expected order")
        void valuesShouldBeInExpectedOrder() {
            ImportType[] values = ImportType.values();
            assertThat(values[0]).isEqualTo(ImportType.INCOME);
            assertThat(values[1]).isEqualTo(ImportType.EXPENSE);
        }
    }

    @Nested
    @DisplayName("Value Of")
    class ValueOf {

        @Test
        @DisplayName("valueOf(INCOME) should return INCOME")
        void valueOfIncomeShouldReturnIncome() {
            assertThat(ImportType.valueOf("INCOME")).isEqualTo(ImportType.INCOME);
        }

        @Test
        @DisplayName("valueOf(EXPENSE) should return EXPENSE")
        void valueOfExpenseShouldReturnExpense() {
            assertThat(ImportType.valueOf("EXPENSE")).isEqualTo(ImportType.EXPENSE);
        }

        @Test
        @DisplayName("valueOf with invalid name should throw exception")
        void valueOfWithInvalidNameShouldThrow() {
            assertThatThrownBy(() -> ImportType.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("valueOf is case sensitive")
        void valueOfIsCaseSensitive() {
            assertThatThrownBy(() -> ImportType.valueOf("income"))
                .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> ImportType.valueOf("Income"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Ordinal")
    class Ordinal {

        @Test
        @DisplayName("INCOME should have ordinal 0")
        void incomeShouldHaveOrdinal0() {
            assertThat(ImportType.INCOME.ordinal()).isZero();
        }

        @Test
        @DisplayName("EXPENSE should have ordinal 1")
        void expenseShouldHaveOrdinal1() {
            assertThat(ImportType.EXPENSE.ordinal()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Usage Patterns")
    class UsagePatterns {

        @Test
        @DisplayName("can be used in switch statement")
        void canBeUsedInSwitch() {
            // Given
            ImportType type = ImportType.INCOME;
            String result;

            // When
            switch (type) {
                case INCOME -> result = "income";
                case EXPENSE -> result = "expense";
                default -> result = "unknown";
            }

            // Then
            assertThat(result).isEqualTo("income");
        }

        @Test
        @DisplayName("can be used in switch expression")
        void canBeUsedInSwitchExpression() {
            // Given
            ImportType type = ImportType.EXPENSE;

            // When
            String result = switch (type) {
                case INCOME -> "income";
                case EXPENSE -> "expense";
            };

            // Then
            assertThat(result).isEqualTo("expense");
        }

        @Test
        @DisplayName("can iterate over all values")
        void canIterateOverAllValues() {
            // When
            int count = 0;
            for (ImportType type : ImportType.values()) {
                count++;
            }

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("same enum values should be equal")
        void sameEnumValuesShouldBeEqual() {
            assertThat(ImportType.INCOME).isEqualTo(ImportType.INCOME);
            assertThat(ImportType.EXPENSE).isEqualTo(ImportType.EXPENSE);
        }

        @Test
        @DisplayName("different enum values should not be equal")
        void differentEnumValuesShouldNotBeEqual() {
            assertThat(ImportType.INCOME).isNotEqualTo(ImportType.EXPENSE);
        }

        @Test
        @DisplayName("can be compared with == operator")
        void canBeComparedWithEqualsOperator() {
            ImportType type = ImportType.INCOME;
            assertThat(type == ImportType.INCOME).isTrue();
            assertThat(type == ImportType.EXPENSE).isFalse();
        }
    }
}
