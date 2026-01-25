package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MatchedRecordViewModel.
 *
 * <p>SE-10B-005: Import Review UI - Comparison Dialog</p>
 */
@DisplayName("MatchedRecordViewModel Tests")
class MatchedRecordViewModelTest {

    @Nested
    @DisplayName("Construction and Getters")
    class ConstructionAndGetters {

        @Test
        @DisplayName("should create view model with all properties")
        void shouldCreateViewModelWithAllProperties() {
            // Given
            UUID id = UUID.randomUUID();
            LocalDate date = LocalDate.of(2026, 1, 15);
            String description = "Office Supplies";
            BigDecimal amount = new BigDecimal("-45.99");
            String category = "Office Costs";

            // When
            MatchedRecordViewModel vm = new MatchedRecordViewModel(id, date, description, amount, category);

            // Then
            assertThat(vm.getId()).isEqualTo(id);
            assertThat(vm.getDate()).isEqualTo(date);
            assertThat(vm.getDescription()).isEqualTo(description);
            assertThat(vm.getAmount()).isEqualByComparingTo(amount);
            assertThat(vm.getCategory()).isEqualTo(category);
        }

        @Test
        @DisplayName("should handle null category")
        void shouldHandleNullCategory() {
            // When
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(),
                LocalDate.now(),
                "Test",
                new BigDecimal("100.00"),
                null
            );

            // Then
            assertThat(vm.getCategory()).isEmpty();
            assertThat(vm.getDisplayCategory()).isEqualTo("-");
        }
    }

    @Nested
    @DisplayName("Formatting")
    class Formatting {

        @Test
        @DisplayName("should format date correctly")
        void shouldFormatDateCorrectly() {
            // Given
            LocalDate date = LocalDate.of(2026, 1, 15);
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(), date, "Test", BigDecimal.ZERO, null
            );

            // Then
            assertThat(vm.getFormattedDate()).isEqualTo("15 Jan 2026");
        }

        @Test
        @DisplayName("should format null date as dash")
        void shouldFormatNullDateAsDash() {
            // Given
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(), null, "Test", BigDecimal.ZERO, null
            );

            // Then
            assertThat(vm.getFormattedDate()).isEqualTo("-");
        }

        @Test
        @DisplayName("should format positive amount with plus sign")
        void shouldFormatPositiveAmountWithPlusSign() {
            // Given
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(), LocalDate.now(), "Test", new BigDecimal("100.00"), null
            );

            // Then
            assertThat(vm.getFormattedAmount()).startsWith("+");
            assertThat(vm.getFormattedAmount()).contains("100");
        }

        @Test
        @DisplayName("should format negative amount with minus sign")
        void shouldFormatNegativeAmountWithMinusSign() {
            // Given
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(), LocalDate.now(), "Test", new BigDecimal("-50.00"), null
            );

            // Then
            assertThat(vm.getFormattedAmount()).startsWith("-");
            assertThat(vm.getFormattedAmount()).contains("50");
        }

        @Test
        @DisplayName("should format null amount as dash")
        void shouldFormatNullAmountAsDash() {
            // Given
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(), LocalDate.now(), "Test", null, null
            );

            // Then
            assertThat(vm.getFormattedAmount()).isEqualTo("-");
        }

        @Test
        @DisplayName("should show category or dash for display")
        void shouldShowCategoryOrDashForDisplay() {
            // Given
            MatchedRecordViewModel withCategory = new MatchedRecordViewModel(
                UUID.randomUUID(), LocalDate.now(), "Test", BigDecimal.ZERO, "Travel"
            );
            MatchedRecordViewModel withoutCategory = new MatchedRecordViewModel(
                UUID.randomUUID(), LocalDate.now(), "Test", BigDecimal.ZERO, ""
            );

            // Then
            assertThat(withCategory.getDisplayCategory()).isEqualTo("Travel");
            assertThat(withoutCategory.getDisplayCategory()).isEqualTo("-");
        }
    }

    @Nested
    @DisplayName("Type Detection")
    class TypeDetection {

        @Test
        @DisplayName("should detect income")
        void shouldDetectIncome() {
            // Given
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(), LocalDate.now(), "Test", new BigDecimal("100.00"), null
            );

            // Then
            assertThat(vm.isIncome()).isTrue();
            assertThat(vm.isExpense()).isFalse();
        }

        @Test
        @DisplayName("should detect expense")
        void shouldDetectExpense() {
            // Given
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(), LocalDate.now(), "Test", new BigDecimal("-50.00"), null
            );

            // Then
            assertThat(vm.isIncome()).isFalse();
            assertThat(vm.isExpense()).isTrue();
        }

        @Test
        @DisplayName("should handle zero amount")
        void shouldHandleZeroAmount() {
            // Given
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(), LocalDate.now(), "Test", BigDecimal.ZERO, null
            );

            // Then
            assertThat(vm.isIncome()).isFalse();
            assertThat(vm.isExpense()).isFalse();
        }

        @Test
        @DisplayName("should handle null amount for type detection")
        void shouldHandleNullAmountForTypeDetection() {
            // Given
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                UUID.randomUUID(), LocalDate.now(), "Test", null, null
            );

            // Then
            assertThat(vm.isIncome()).isFalse();
            assertThat(vm.isExpense()).isFalse();
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTests {

        @Test
        @DisplayName("should generate readable toString")
        void shouldGenerateReadableToString() {
            // Given
            UUID id = UUID.randomUUID();
            MatchedRecordViewModel vm = new MatchedRecordViewModel(
                id,
                LocalDate.of(2026, 1, 15),
                "Office Supplies",
                new BigDecimal("-45.99"),
                "Office Costs"
            );

            // When
            String result = vm.toString();

            // Then
            assertThat(result).contains("MatchedRecordViewModel");
            assertThat(result).contains(id.toString());
            assertThat(result).contains("Office Supplies");
            assertThat(result).contains("-45.99");
            assertThat(result).contains("Office Costs");
        }
    }
}
