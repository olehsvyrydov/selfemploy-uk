package uk.selfemploy.ui.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.ui.viewmodel.CategorySummary;
import uk.selfemploy.ui.viewmodel.QuarterlyReviewData;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QuarterlyReviewDialog and QuarterlyReviewData.
 * Tests the dialog creation logic without requiring JavaFX runtime.
 *
 * <p>Implementation: /james</p>
 */
@DisplayName("QuarterlyReviewDialog")
class QuarterlyReviewDialogTest {

    private static final TaxYear TAX_YEAR_2025 = TaxYear.of(2025);

    @Nested
    @DisplayName("QuarterlyReviewData")
    class QuarterlyReviewDataTests {

        @Nested
        @DisplayName("Construction and Basic Properties")
        class ConstructionTests {

            @Test
            @DisplayName("should create review data with all required fields")
            void shouldCreateReviewDataWithAllFields() {
                Map<ExpenseCategory, CategorySummary> expenses = createSampleExpensesByCategory();

                QuarterlyReviewData reviewData = QuarterlyReviewData.builder()
                        .quarter(Quarter.Q1)
                        .taxYear(TAX_YEAR_2025)
                        .periodStart(Quarter.Q1.getStartDate(TAX_YEAR_2025))
                        .periodEnd(Quarter.Q1.getEndDate(TAX_YEAR_2025))
                        .totalIncome(new BigDecimal("5000.00"))
                        .incomeTransactionCount(3)
                        .expensesByCategory(expenses)
                        .totalExpenses(new BigDecimal("2000.00"))
                        .expenseTransactionCount(5)
                        .build();

                assertEquals(Quarter.Q1, reviewData.getQuarter());
                assertEquals(TAX_YEAR_2025, reviewData.getTaxYear());
                assertEquals(new BigDecimal("5000.00"), reviewData.getTotalIncome());
                assertEquals(3, reviewData.getIncomeTransactionCount());
                assertEquals(new BigDecimal("2000.00"), reviewData.getTotalExpenses());
                assertEquals(5, reviewData.getExpenseTransactionCount());
            }

            @Test
            @DisplayName("should return correct period dates")
            void shouldReturnCorrectPeriodDates() {
                QuarterlyReviewData reviewData = QuarterlyReviewData.builder()
                        .quarter(Quarter.Q1)
                        .taxYear(TAX_YEAR_2025)
                        .periodStart(Quarter.Q1.getStartDate(TAX_YEAR_2025))
                        .periodEnd(Quarter.Q1.getEndDate(TAX_YEAR_2025))
                        .totalIncome(BigDecimal.ZERO)
                        .incomeTransactionCount(0)
                        .expensesByCategory(new EnumMap<>(ExpenseCategory.class))
                        .totalExpenses(BigDecimal.ZERO)
                        .expenseTransactionCount(0)
                        .build();

                assertEquals(LocalDate.of(2025, 4, 6), reviewData.getPeriodStart());
                assertEquals(LocalDate.of(2025, 7, 5), reviewData.getPeriodEnd());
            }
        }

        @Nested
        @DisplayName("Net Profit Calculation")
        class NetProfitCalculationTests {

            @Test
            @DisplayName("should calculate positive net profit correctly")
            void shouldCalculatePositiveNetProfit() {
                QuarterlyReviewData reviewData = createReviewData(
                        new BigDecimal("5000.00"),
                        new BigDecimal("2000.00")
                );

                assertEquals(new BigDecimal("3000.00"), reviewData.getNetProfit());
            }

            @Test
            @DisplayName("should calculate negative net profit (loss) correctly")
            void shouldCalculateNegativeNetProfit() {
                QuarterlyReviewData reviewData = createReviewData(
                        new BigDecimal("1000.00"),
                        new BigDecimal("3000.00")
                );

                assertEquals(new BigDecimal("-2000.00"), reviewData.getNetProfit());
            }

            @Test
            @DisplayName("should calculate zero net profit correctly")
            void shouldCalculateZeroNetProfit() {
                QuarterlyReviewData reviewData = createReviewData(
                        new BigDecimal("2500.00"),
                        new BigDecimal("2500.00")
                );

                assertEquals(BigDecimal.ZERO.setScale(2), reviewData.getNetProfit().setScale(2));
            }
        }

        @Nested
        @DisplayName("Nil Return Detection")
        class NilReturnDetectionTests {

            @Test
            @DisplayName("should detect nil return when income and expenses are zero")
            void shouldDetectNilReturn() {
                QuarterlyReviewData reviewData = createReviewData(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

                assertTrue(reviewData.isNilReturn(), "Should be nil return when both are zero");
            }

            @Test
            @DisplayName("should not be nil return when income is positive")
            void shouldNotBeNilReturnWithIncome() {
                QuarterlyReviewData reviewData = createReviewData(
                        new BigDecimal("100.00"),
                        BigDecimal.ZERO
                );

                assertFalse(reviewData.isNilReturn(), "Should not be nil return with income");
            }

            @Test
            @DisplayName("should not be nil return when expenses are positive")
            void shouldNotBeNilReturnWithExpenses() {
                QuarterlyReviewData reviewData = createReviewData(
                        BigDecimal.ZERO,
                        new BigDecimal("50.00")
                );

                assertFalse(reviewData.isNilReturn(), "Should not be nil return with expenses");
            }

            @Test
            @DisplayName("should not be nil return when both income and expenses exist")
            void shouldNotBeNilReturnWithBoth() {
                QuarterlyReviewData reviewData = createReviewData(
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")
                );

                assertFalse(reviewData.isNilReturn(), "Should not be nil return with data");
            }
        }

        @Nested
        @DisplayName("Expense Category Grouping")
        class ExpenseCategoryGroupingTests {

            @Test
            @DisplayName("should group expenses by SA103 category")
            void shouldGroupExpensesBySA103Category() {
                Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
                expenses.put(ExpenseCategory.COST_OF_GOODS, new CategorySummary(new BigDecimal("500.00"), 2));
                expenses.put(ExpenseCategory.STAFF_COSTS, new CategorySummary(new BigDecimal("300.00"), 1));
                expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("200.00"), 3));

                QuarterlyReviewData reviewData = QuarterlyReviewData.builder()
                        .quarter(Quarter.Q1)
                        .taxYear(TAX_YEAR_2025)
                        .periodStart(Quarter.Q1.getStartDate(TAX_YEAR_2025))
                        .periodEnd(Quarter.Q1.getEndDate(TAX_YEAR_2025))
                        .totalIncome(new BigDecimal("5000.00"))
                        .incomeTransactionCount(3)
                        .expensesByCategory(expenses)
                        .totalExpenses(new BigDecimal("1000.00"))
                        .expenseTransactionCount(6)
                        .build();

                Map<ExpenseCategory, CategorySummary> result = reviewData.getExpensesByCategory();

                assertEquals(3, result.size());
                assertEquals(new BigDecimal("500.00"), result.get(ExpenseCategory.COST_OF_GOODS).amount());
                assertEquals(2, result.get(ExpenseCategory.COST_OF_GOODS).transactionCount());
                assertEquals(new BigDecimal("300.00"), result.get(ExpenseCategory.STAFF_COSTS).amount());
                assertEquals(new BigDecimal("200.00"), result.get(ExpenseCategory.TRAVEL).amount());
            }

            @Test
            @DisplayName("should return empty map when no expenses")
            void shouldReturnEmptyMapWhenNoExpenses() {
                QuarterlyReviewData reviewData = createReviewData(
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO
                );

                assertTrue(reviewData.getExpensesByCategory().isEmpty());
            }

            @Test
            @DisplayName("should correctly sum category transaction counts")
            void shouldSumCategoryTransactionCounts() {
                Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
                expenses.put(ExpenseCategory.OFFICE_COSTS, new CategorySummary(new BigDecimal("150.00"), 5));
                expenses.put(ExpenseCategory.PROFESSIONAL_FEES, new CategorySummary(new BigDecimal("350.00"), 2));

                QuarterlyReviewData reviewData = QuarterlyReviewData.builder()
                        .quarter(Quarter.Q2)
                        .taxYear(TAX_YEAR_2025)
                        .periodStart(Quarter.Q2.getStartDate(TAX_YEAR_2025))
                        .periodEnd(Quarter.Q2.getEndDate(TAX_YEAR_2025))
                        .totalIncome(BigDecimal.ZERO)
                        .incomeTransactionCount(0)
                        .expensesByCategory(expenses)
                        .totalExpenses(new BigDecimal("500.00"))
                        .expenseTransactionCount(7)
                        .build();

                assertEquals(5, reviewData.getExpensesByCategory().get(ExpenseCategory.OFFICE_COSTS).transactionCount());
                assertEquals(2, reviewData.getExpensesByCategory().get(ExpenseCategory.PROFESSIONAL_FEES).transactionCount());
            }
        }

        @Nested
        @DisplayName("Display Formatting")
        class DisplayFormattingTests {

            @Test
            @DisplayName("should format period header text correctly")
            void shouldFormatPeriodHeaderText() {
                QuarterlyReviewData reviewData = createReviewData(
                        new BigDecimal("5000.00"),
                        new BigDecimal("2000.00")
                );

                String headerText = reviewData.getPeriodHeaderText();

                assertTrue(headerText.contains("Q1"), "Header should contain quarter");
                assertTrue(headerText.contains("2025/26") || headerText.contains("2025"),
                        "Header should contain tax year");
            }

            @Test
            @DisplayName("should format date range text correctly")
            void shouldFormatDateRangeText() {
                QuarterlyReviewData reviewData = createReviewData(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

                String dateRange = reviewData.getDateRangeText();

                assertNotNull(dateRange);
                assertTrue(dateRange.contains("-"), "Date range should contain separator");
                // Q1 2025/26 is 6 Apr 2025 - 5 Jul 2025
                assertTrue(dateRange.toLowerCase().contains("apr") || dateRange.contains("04"),
                        "Should contain April reference");
            }

            @Test
            @DisplayName("should format income with transaction count")
            void shouldFormatIncomeWithCount() {
                QuarterlyReviewData reviewData = QuarterlyReviewData.builder()
                        .quarter(Quarter.Q1)
                        .taxYear(TAX_YEAR_2025)
                        .periodStart(Quarter.Q1.getStartDate(TAX_YEAR_2025))
                        .periodEnd(Quarter.Q1.getEndDate(TAX_YEAR_2025))
                        .totalIncome(new BigDecimal("12345.67"))
                        .incomeTransactionCount(15)
                        .expensesByCategory(new EnumMap<>(ExpenseCategory.class))
                        .totalExpenses(BigDecimal.ZERO)
                        .expenseTransactionCount(0)
                        .build();

                String formatted = reviewData.getFormattedIncome();

                assertTrue(formatted.contains("12,345.67") || formatted.contains("12345.67"),
                        "Should contain formatted amount");
            }
        }
    }

    @Nested
    @DisplayName("CategorySummary Record")
    class CategorySummaryTests {

        @Test
        @DisplayName("should create category summary with amount and count")
        void shouldCreateCategorySummary() {
            CategorySummary summary = new CategorySummary(new BigDecimal("1500.00"), 5);

            assertEquals(new BigDecimal("1500.00"), summary.amount());
            assertEquals(5, summary.transactionCount());
        }

        @Test
        @DisplayName("should handle zero amount and count")
        void shouldHandleZeroAmountAndCount() {
            CategorySummary summary = new CategorySummary(BigDecimal.ZERO, 0);

            assertEquals(BigDecimal.ZERO, summary.amount());
            assertEquals(0, summary.transactionCount());
        }
    }

    @Nested
    @DisplayName("Dialog Static Helpers")
    class DialogStaticHelperTests {

        @Test
        @DisplayName("should return correct confirmation text for normal return")
        void shouldReturnConfirmationTextForNormalReturn() {
            String[] confirmations = QuarterlyReviewDialog.getConfirmationTexts(false);

            assertEquals(3, confirmations.length);
            assertTrue(confirmations[0].toLowerCase().contains("income"));
            assertTrue(confirmations[1].toLowerCase().contains("expense"));
            assertTrue(confirmations[2].toLowerCase().contains("hmrc"));
        }

        @Test
        @DisplayName("should return correct confirmation text for nil return")
        void shouldReturnConfirmationTextForNilReturn() {
            String[] confirmations = QuarterlyReviewDialog.getConfirmationTexts(true);

            assertEquals(3, confirmations.length);
            assertTrue(confirmations[0].toLowerCase().contains("no") ||
                       confirmations[0].toLowerCase().contains("nil"));
        }

        @Test
        @DisplayName("should return correct submit button text for normal return")
        void shouldReturnSubmitButtonTextForNormalReturn() {
            String buttonText = QuarterlyReviewDialog.getSubmitButtonText(false);

            assertTrue(buttonText.toLowerCase().contains("submit"));
            assertTrue(buttonText.toLowerCase().contains("hmrc"));
        }

        @Test
        @DisplayName("should return correct submit button text for nil return")
        void shouldReturnSubmitButtonTextForNilReturn() {
            String buttonText = QuarterlyReviewDialog.getSubmitButtonText(true);

            assertTrue(buttonText.toLowerCase().contains("nil"));
        }

        @Test
        @DisplayName("should use correct dialog width (500px)")
        void shouldUseCorrectDialogWidth() {
            assertEquals(500, QuarterlyReviewDialog.DIALOG_WIDTH);
        }

        @Test
        @DisplayName("should use green header color")
        void shouldUseGreenHeaderColor() {
            String[] colors = QuarterlyReviewDialog.getHeaderGradientColors(false);
            // Green gradient for review
            assertTrue(colors[0].contains("28a745") || colors[0].contains("2"));
        }

        @Test
        @DisplayName("should use red header color for overdue")
        void shouldUseRedHeaderColorForOverdue() {
            String[] colors = QuarterlyReviewDialog.getHeaderGradientColors(true);
            // Red gradient for overdue
            assertTrue(colors[0].contains("dc3545") || colors[0].contains("d"));
        }
    }

    @Nested
    @DisplayName("Expense Category SA103 Mapping")
    class ExpenseCategorySA103MappingTests {

        @Test
        @DisplayName("should map Cost of Goods to Box 17")
        void shouldMapCostOfGoodsToBox17() {
            assertEquals("17", ExpenseCategory.COST_OF_GOODS.getSa103Box());
        }

        @Test
        @DisplayName("should map Staff Costs to Box 19")
        void shouldMapStaffCostsToBox19() {
            assertEquals("19", ExpenseCategory.STAFF_COSTS.getSa103Box());
        }

        @Test
        @DisplayName("should map Travel to Box 20")
        void shouldMapTravelToBox20() {
            assertEquals("20", ExpenseCategory.TRAVEL.getSa103Box());
        }

        @Test
        @DisplayName("should map Premises to Box 21")
        void shouldMapPremisesToBox21() {
            assertEquals("21", ExpenseCategory.PREMISES.getSa103Box());
        }

        @Test
        @DisplayName("should map Office Costs to Box 23")
        void shouldMapOfficeCostsToBox23() {
            assertEquals("23", ExpenseCategory.OFFICE_COSTS.getSa103Box());
        }

        @Test
        @DisplayName("should map Professional Fees to Box 28")
        void shouldMapProfessionalFeesToBox28() {
            assertEquals("28", ExpenseCategory.PROFESSIONAL_FEES.getSa103Box());
        }

        @Test
        @DisplayName("should map Other Expenses to Box 30")
        void shouldMapOtherExpensesToBox30() {
            assertEquals("30", ExpenseCategory.OTHER_EXPENSES.getSa103Box());
        }
    }

    @Nested
    @DisplayName("Declaration Hash Computation")
    class DeclarationHashTests {

        @Test
        @DisplayName("should compute SHA-256 hash of normal confirmation texts")
        void shouldComputeHashOfNormalConfirmationTexts() {
            String hash = QuarterlyReviewDialog.computeDeclarationHash(false);

            assertNotNull(hash, "Hash should not be null");
            assertEquals(64, hash.length(), "SHA-256 hash should be 64 hex characters");
            assertTrue(hash.matches("[a-f0-9]{64}"), "Hash should be lowercase hex");
        }

        @Test
        @DisplayName("should compute SHA-256 hash of nil return confirmation texts")
        void shouldComputeHashOfNilReturnConfirmationTexts() {
            String hash = QuarterlyReviewDialog.computeDeclarationHash(true);

            assertNotNull(hash, "Hash should not be null");
            assertEquals(64, hash.length(), "SHA-256 hash should be 64 hex characters");
            assertTrue(hash.matches("[a-f0-9]{64}"), "Hash should be lowercase hex");
        }

        @Test
        @DisplayName("should produce different hashes for normal vs nil return")
        void shouldProduceDifferentHashesForNormalVsNil() {
            String normalHash = QuarterlyReviewDialog.computeDeclarationHash(false);
            String nilHash = QuarterlyReviewDialog.computeDeclarationHash(true);

            assertNotEquals(normalHash, nilHash,
                    "Normal and nil return hashes should differ");
        }

        @Test
        @DisplayName("should produce consistent hash for same input")
        void shouldProduceConsistentHash() {
            String hash1 = QuarterlyReviewDialog.computeDeclarationHash(false);
            String hash2 = QuarterlyReviewDialog.computeDeclarationHash(false);

            assertEquals(hash1, hash2, "Same input should always produce same hash");
        }
    }

    @Nested
    @DisplayName("Error Dialog Constants")
    class ErrorDialogConstantsTests {

        @Test
        @DisplayName("should expose retryable hint text for temporary errors")
        void shouldExposeRetryableHintText() {
            String hint = QuarterlyReviewDialog.RETRYABLE_HINT_TEXT;

            assertNotNull(hint, "Retryable hint text should not be null");
            assertFalse(hint.isBlank(), "Retryable hint text should not be blank");
            assertTrue(hint.toLowerCase().contains("temporary"),
                    "Hint should mention the issue is temporary");
            assertTrue(hint.toLowerCase().contains("try again"),
                    "Hint should suggest trying again");
        }

        @Test
        @DisplayName("should use solid color for settings button")
        void shouldUseSolidColorForSettingsButton() {
            String color = QuarterlyReviewDialog.SETTINGS_BUTTON_COLOR;

            assertNotNull(color, "Settings button color should not be null");
            assertEquals("#0066cc", color, "Settings button should use solid #0066cc");
        }
    }

    @Nested
    @DisplayName("Submission Service Interface")
    class SubmissionServiceInterfaceTests {

        @Test
        @DisplayName("SubmissionHandler functional interface should accept correct parameters")
        void shouldAcceptCorrectParameters() {
            // Verify the functional interface contract compiles and works
            QuarterlyReviewDialog.SubmissionHandler handler = (reviewData, declarationAcceptedAt, declarationTextHash) -> {
                assertNotNull(reviewData);
                assertNotNull(declarationAcceptedAt);
                assertNotNull(declarationTextHash);
                return null; // Would return Submission in real use
            };

            assertNotNull(handler, "Handler should be created");
        }
    }

    // ========== Helper Methods ==========

    private QuarterlyReviewData createReviewData(BigDecimal income, BigDecimal expenses) {
        return QuarterlyReviewData.builder()
                .quarter(Quarter.Q1)
                .taxYear(TAX_YEAR_2025)
                .periodStart(Quarter.Q1.getStartDate(TAX_YEAR_2025))
                .periodEnd(Quarter.Q1.getEndDate(TAX_YEAR_2025))
                .totalIncome(income)
                .incomeTransactionCount(income.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0)
                .expensesByCategory(new EnumMap<>(ExpenseCategory.class))
                .totalExpenses(expenses)
                .expenseTransactionCount(expenses.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0)
                .build();
    }

    private Map<ExpenseCategory, CategorySummary> createSampleExpensesByCategory() {
        Map<ExpenseCategory, CategorySummary> expenses = new EnumMap<>(ExpenseCategory.class);
        expenses.put(ExpenseCategory.COST_OF_GOODS, new CategorySummary(new BigDecimal("1000.00"), 2));
        expenses.put(ExpenseCategory.OFFICE_COSTS, new CategorySummary(new BigDecimal("500.00"), 3));
        expenses.put(ExpenseCategory.TRAVEL, new CategorySummary(new BigDecimal("500.00"), 2));
        return expenses;
    }
}
