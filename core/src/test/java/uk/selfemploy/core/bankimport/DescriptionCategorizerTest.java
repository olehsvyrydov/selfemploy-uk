package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DescriptionCategorizer.
 */
@DisplayName("DescriptionCategorizer Tests")
class DescriptionCategorizerTest {

    private DescriptionCategorizer categorizer;

    @BeforeEach
    void setUp() {
        categorizer = new DescriptionCategorizer();
    }

    @Nested
    @DisplayName("Expense Category Suggestion Tests")
    class ExpenseCategorySuggestionTests {

        @ParameterizedTest
        @CsvSource({
            "AMAZON MARKETPLACE, OFFICE_COSTS",
            "amazon.co.uk purchase, OFFICE_COSTS",
            "Amazon Prime, OFFICE_COSTS",
            "OFFICE SUPPLIES LTD, OFFICE_COSTS",
            "Microsoft Office 365, OFFICE_COSTS",
            "Adobe Software, OFFICE_COSTS"
        })
        @DisplayName("should suggest OFFICE_COSTS for office-related descriptions")
        void shouldSuggestOfficeCosts(String description, String expectedCategory) {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.valueOf(expectedCategory));
            assertThat(suggestion.confidence()).isIn(Confidence.HIGH, Confidence.MEDIUM);
        }

        @ParameterizedTest
        @CsvSource({
            "UBER TRIP, TRAVEL",
            "uber.com, TRAVEL",
            "TRAINLINE BOOKING, TRAVEL",
            "National Rail, TRAVEL",
            "HOTEL BOOKING, TRAVEL",
            "Premier Inn, TRAVEL",
            "Travelodge, TRAVEL"
        })
        @DisplayName("should suggest TRAVEL for travel-related descriptions")
        void shouldSuggestTravel(String description, String expectedCategory) {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.valueOf(expectedCategory));
        }

        @ParameterizedTest
        @CsvSource({
            "SHELL PETROL, TRAVEL_MILEAGE",
            "BP FUEL, TRAVEL_MILEAGE",
            "TESCO PETROL, TRAVEL_MILEAGE",
            "ESSO DIESEL, TRAVEL_MILEAGE"
        })
        @DisplayName("should suggest TRAVEL_MILEAGE for fuel descriptions")
        void shouldSuggestTravelMileage(String description, String expectedCategory) {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.valueOf(expectedCategory));
        }

        @ParameterizedTest
        @CsvSource({
            "BRITISH GAS ELECTRICITY, PREMISES",
            "EDF ENERGY, PREMISES",
            "RENT PAYMENT, PREMISES",
            "WATER BILL, PREMISES",
            "BUSINESS INSURANCE PREMIUM, PREMISES"
        })
        @DisplayName("should suggest PREMISES for utility descriptions")
        void shouldSuggestPremises(String description, String expectedCategory) {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.valueOf(expectedCategory));
        }

        @ParameterizedTest
        @CsvSource({
            "SMITH AND CO ACCOUNTANTS, PROFESSIONAL_FEES",
            "Legal services, PROFESSIONAL_FEES",
            "SOLICITOR FEE, PROFESSIONAL_FEES",
            "Accounting fee, PROFESSIONAL_FEES"
        })
        @DisplayName("should suggest PROFESSIONAL_FEES for professional service descriptions")
        void shouldSuggestProfessionalFees(String description, String expectedCategory) {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.valueOf(expectedCategory));
        }

        @ParameterizedTest
        @CsvSource({
            "BT PHONE BILL, OFFICE_COSTS",
            "Vodafone business, OFFICE_COSTS",
            "SKY BROADBAND, OFFICE_COSTS",
            "Internet service, OFFICE_COSTS"
        })
        @DisplayName("should suggest OFFICE_COSTS for phone/internet descriptions")
        void shouldSuggestOfficeCostsForPhone(String description, String expectedCategory) {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory(description);

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.valueOf(expectedCategory));
        }

        @Test
        @DisplayName("should suggest OTHER_EXPENSES with LOW confidence for unknown descriptions")
        void shouldSuggestOtherExpensesForUnknown() {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory("RANDOM UNKNOWN MERCHANT XYZ123");

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.OTHER_EXPENSES);
            assertThat(suggestion.confidence()).isEqualTo(Confidence.LOW);
        }

        @Test
        @DisplayName("should handle case insensitivity")
        void shouldHandleCaseInsensitivity() {
            CategorySuggestion lower = categorizer.suggestExpenseCategory("amazon marketplace");
            CategorySuggestion upper = categorizer.suggestExpenseCategory("AMAZON MARKETPLACE");
            CategorySuggestion mixed = categorizer.suggestExpenseCategory("Amazon Marketplace");

            assertThat(lower.category()).isEqualTo(upper.category());
            assertThat(lower.category()).isEqualTo(mixed.category());
        }

        @Test
        @DisplayName("should handle extra whitespace")
        void shouldHandleExtraWhitespace() {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory("  AMAZON   MARKETPLACE  ");

            assertThat(suggestion.category()).isEqualTo(ExpenseCategory.OFFICE_COSTS);
        }
    }

    @Nested
    @DisplayName("Income Category Suggestion Tests")
    class IncomeCategorySuggestionTests {

        @Test
        @DisplayName("should suggest SALES as default for income")
        void shouldSuggestSalesAsDefault() {
            CategorySuggestion suggestion = categorizer.suggestIncomeCategory("CLIENT PAYMENT");

            assertThat(suggestion.category()).isEqualTo(IncomeCategory.SALES);
        }

        @ParameterizedTest
        @CsvSource({
            "INTEREST PAYMENT, OTHER_INCOME",
            "BANK INTEREST, OTHER_INCOME",
            "Dividend payment, OTHER_INCOME"
        })
        @DisplayName("should suggest OTHER_INCOME for interest/dividend descriptions")
        void shouldSuggestOtherIncome(String description, String expectedCategory) {
            CategorySuggestion suggestion = categorizer.suggestIncomeCategory(description);

            assertThat(suggestion.category()).isEqualTo(IncomeCategory.valueOf(expectedCategory));
        }
    }

    @Nested
    @DisplayName("Confidence Level Tests")
    class ConfidenceLevelTests {

        @Test
        @DisplayName("should have HIGH confidence for exact keyword match")
        void shouldHaveHighConfidenceForExactMatch() {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory("AMAZON");

            assertThat(suggestion.confidence()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("should have LOW confidence for no match")
        void shouldHaveLowConfidenceForNoMatch() {
            CategorySuggestion suggestion = categorizer.suggestExpenseCategory("COMPLETELY UNKNOWN");

            assertThat(suggestion.confidence()).isEqualTo(Confidence.LOW);
        }
    }
}
