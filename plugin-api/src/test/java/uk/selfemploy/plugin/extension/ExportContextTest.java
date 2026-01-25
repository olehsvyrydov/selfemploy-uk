package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.extension.ExportContext.DataType;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ExportContext}.
 */
@DisplayName("ExportContext")
class ExportContextTest {

    @Nested
    @DisplayName("when creating context")
    class Creation {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            LocalDate start = LocalDate.of(2024, 4, 6);
            LocalDate end = LocalDate.of(2025, 4, 5);

            ExportContext context = new ExportContext(
                2024, start, end,
                EnumSet.of(DataType.INCOME, DataType.EXPENSES),
                Map.of(ExportContext.OPTION_INCLUDE_HEADERS, true)
            );

            assertThat(context.taxYear()).isEqualTo(2024);
            assertThat(context.startDate()).isEqualTo(start);
            assertThat(context.endDate()).isEqualTo(end);
            assertThat(context.dataTypes()).containsExactlyInAnyOrder(DataType.INCOME, DataType.EXPENSES);
        }

        @Test
        @DisplayName("should reject null start date")
        void shouldRejectNullStartDate() {
            assertThatThrownBy(() -> new ExportContext(
                2024, null, LocalDate.of(2025, 4, 5), null, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should reject null end date")
        void shouldRejectNullEndDate() {
            assertThatThrownBy(() -> new ExportContext(
                2024, LocalDate.of(2024, 4, 6), null, null, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should reject end date before start date")
        void shouldRejectEndDateBeforeStartDate() {
            assertThatThrownBy(() -> new ExportContext(
                2024,
                LocalDate.of(2025, 4, 6),
                LocalDate.of(2024, 4, 5),
                null, null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should default empty dataTypes to ALL")
        void shouldDefaultEmptyDataTypesToAll() {
            ExportContext context = new ExportContext(
                2024,
                LocalDate.of(2024, 4, 6),
                LocalDate.of(2025, 4, 5),
                EnumSet.noneOf(DataType.class),
                null
            );

            assertThat(context.dataTypes()).containsExactly(DataType.ALL);
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("forTaxYear should create correct date range")
        void forTaxYearShouldCreateCorrectDateRange() {
            ExportContext context = ExportContext.forTaxYear(2024);

            assertThat(context.taxYear()).isEqualTo(2024);
            assertThat(context.startDate()).isEqualTo(LocalDate.of(2024, 4, 6));
            assertThat(context.endDate()).isEqualTo(LocalDate.of(2025, 4, 5));
            assertThat(context.dataTypes()).containsExactly(DataType.ALL);
        }
    }

    @Nested
    @DisplayName("data type helpers")
    class DataTypeHelpers {

        @Test
        @DisplayName("includeIncome should return true for ALL")
        void includeIncomeShouldReturnTrueForAll() {
            ExportContext context = ExportContext.forTaxYear(2024);

            assertThat(context.includeIncome()).isTrue();
        }

        @Test
        @DisplayName("includeIncome should return true for INCOME type")
        void includeIncomeShouldReturnTrueForIncomeType() {
            ExportContext context = new ExportContext(
                2024,
                LocalDate.of(2024, 4, 6),
                LocalDate.of(2025, 4, 5),
                EnumSet.of(DataType.INCOME),
                null
            );

            assertThat(context.includeIncome()).isTrue();
            assertThat(context.includeExpenses()).isFalse();
        }

        @Test
        @DisplayName("includeExpenses should return true for EXPENSES type")
        void includeExpensesShouldReturnTrueForExpensesType() {
            ExportContext context = new ExportContext(
                2024,
                LocalDate.of(2024, 4, 6),
                LocalDate.of(2025, 4, 5),
                EnumSet.of(DataType.EXPENSES),
                null
            );

            assertThat(context.includeExpenses()).isTrue();
            assertThat(context.includeIncome()).isFalse();
        }

        @Test
        @DisplayName("includeTaxSummary should return true when included")
        void includeTaxSummaryShouldReturnTrueWhenIncluded() {
            ExportContext context = new ExportContext(
                2024,
                LocalDate.of(2024, 4, 6),
                LocalDate.of(2025, 4, 5),
                EnumSet.of(DataType.TAX_SUMMARY),
                null
            );

            assertThat(context.includeTaxSummary()).isTrue();
        }
    }
}
