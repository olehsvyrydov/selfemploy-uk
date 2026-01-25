package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.extension.ExpenseCategoryExtension.ExpenseCategory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ExpenseCategoryExtension}.
 */
@DisplayName("ExpenseCategoryExtension")
class ExpenseCategoryExtensionTest {

    @Nested
    @DisplayName("ExpenseCategory record")
    class ExpenseCategoryRecord {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            ExpenseCategory category = new ExpenseCategory(
                "software-subscriptions",
                "Software Subscriptions",
                "BOX_16",
                "Monthly software subscriptions"
            );

            assertThat(category.categoryId()).isEqualTo("software-subscriptions");
            assertThat(category.displayName()).isEqualTo("Software Subscriptions");
            assertThat(category.sa103Box()).isEqualTo("BOX_16");
            assertThat(category.description()).isEqualTo("Monthly software subscriptions");
        }

        @Test
        @DisplayName("should create without description")
        void shouldCreateWithoutDescription() {
            ExpenseCategory category = new ExpenseCategory(
                "software-subscriptions",
                "Software Subscriptions",
                "BOX_16"
            );

            assertThat(category.description()).isEmpty();
        }

        @Test
        @DisplayName("should convert null description to empty string")
        void shouldConvertNullDescriptionToEmptyString() {
            ExpenseCategory category = new ExpenseCategory(
                "software-subscriptions",
                "Software Subscriptions",
                "BOX_16",
                null
            );

            assertThat(category.description()).isEmpty();
        }

        @Test
        @DisplayName("should reject blank category ID")
        void shouldRejectBlankCategoryId() {
            assertThatThrownBy(() -> new ExpenseCategory("", "Name", "BOX_16"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category ID");
        }

        @Test
        @DisplayName("should reject null category ID")
        void shouldRejectNullCategoryId() {
            assertThatThrownBy(() -> new ExpenseCategory(null, "Name", "BOX_16"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category ID");
        }

        @Test
        @DisplayName("should reject blank display name")
        void shouldRejectBlankDisplayName() {
            assertThatThrownBy(() -> new ExpenseCategory("id", "", "BOX_16"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Display name");
        }

        @Test
        @DisplayName("should reject blank SA103 box")
        void shouldRejectBlankSa103Box() {
            assertThatThrownBy(() -> new ExpenseCategory("id", "Name", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SA103 box");
        }
    }

    @Nested
    @DisplayName("when implementing extension")
    class Implementation {

        @Test
        @DisplayName("should be implementable")
        void shouldBeImplementable() {
            ExpenseCategoryExtension extension = new TestExpenseCategoryExtension();

            assertThat(extension.getExtensionId()).isEqualTo("test-categories");
            assertThat(extension.getExtensionName()).isEqualTo("Test Categories");
            assertThat(extension.getCategories()).hasSize(2);
        }

        @Test
        @DisplayName("should provide default description")
        void shouldProvideDefaultDescription() {
            ExpenseCategoryExtension extension = new TestExpenseCategoryExtension();

            assertThat(extension.getExtensionDescription()).isEmpty();
        }
    }

    /**
     * Test implementation of ExpenseCategoryExtension.
     */
    private static class TestExpenseCategoryExtension implements ExpenseCategoryExtension {
        @Override
        public String getExtensionId() {
            return "test-categories";
        }

        @Override
        public String getExtensionName() {
            return "Test Categories";
        }

        @Override
        public List<ExpenseCategory> getCategories() {
            return List.of(
                new ExpenseCategory("cat1", "Category 1", "BOX_16"),
                new ExpenseCategory("cat2", "Category 2", "BOX_23", "Description")
            );
        }
    }
}
