package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the ColumnMappingViewModel.
 *
 * <p>SE-802: Bank Import Column Mapping Wizard
 *
 * <p>These tests verify the P0 (Critical) test cases defined by /rob:
 * <ul>
 *     <li>TC-802-001: Wizard displays after CSV file selection</li>
 *     <li>TC-802-004: Date column selection required</li>
 *     <li>TC-802-005: Amount column selection required</li>
 *     <li>TC-802-006: Description column selection required</li>
 *     <li>TC-802-008: Auto-detection of "Date" column</li>
 *     <li>TC-802-009: Auto-detection of "Amount" column</li>
 *     <li>TC-802-012: Amount interpretation defaults to STANDARD</li>
 *     <li>TC-802-016: Summary shows income count</li>
 *     <li>TC-802-017: Summary shows expense count</li>
 *     <li>TC-802-018: Summary shows income total</li>
 *     <li>TC-802-019: Summary shows expense total</li>
 *     <li>TC-802-022: Confirm button disabled until all mapped</li>
 *     <li>TC-802-029: Negative amounts parsed correctly</li>
 *     <li>TC-802-030: Parentheses indicate negative amounts</li>
 * </ul>
 *
 * <p>Test Author: /adam (Senior E2E Test Automation Engineer)
 * <p>Sprint: 6
 *
 * @see ColumnMappingViewModel
 * @see <a href="docs/sprints/sprint-6/testing/rob-qa-SE-509-SE-802-SE-703.md">QA Test Specifications</a>
 */
@DisplayName("SE-802: Bank Import Column Mapping Wizard Integration Tests")
@Tag("integration")
@Tag("se-802")
@Tag("column-mapping")
class ColumnMappingViewModelIntegrationTest {

    private ColumnMappingViewModel viewModel;

    // Sample CSV data for testing
    private static final List<String> STANDARD_HEADERS = List.of("Date", "Description", "Amount", "Balance", "Category");

    private static final List<PreviewRow> STANDARD_PREVIEW_ROWS = List.of(
        new PreviewRow(List.of("15/01/2026", "AMAZON UK", "-45.99", "1250.00", "Shopping")),
        new PreviewRow(List.of("14/01/2026", "PAYPAL TRANSFER", "1250.00", "1295.99", "Transfer")),
        new PreviewRow(List.of("13/01/2026", "TESCO STORES", "-32.50", "45.99", "Groceries")),
        new PreviewRow(List.of("12/01/2026", "CLIENT A PAYMENT", "2500.00", "78.49", "Invoice")),
        new PreviewRow(List.of("11/01/2026", "EE MOBILE", "-45.00", "-2421.51", "Bills"))
    );

    // Sample with parentheses format for negatives
    private static final List<PreviewRow> PARENTHESES_PREVIEW_ROWS = List.of(
        new PreviewRow(List.of("15/01/2026", "AMAZON UK", "(45.99)", "1250.00", "Shopping")),
        new PreviewRow(List.of("14/01/2026", "PAYPAL TRANSFER", "1250.00", "1295.99", "Transfer")),
        new PreviewRow(List.of("13/01/2026", "TESCO STORES", "(32.50)", "45.99", "Groceries"))
    );

    @BeforeEach
    void setUp() {
        viewModel = new ColumnMappingViewModel();
    }

    // ==================== TC-802-001: Wizard Display After File Selection ====================

    @Nested
    @DisplayName("TC-802-001: Wizard Display After File Selection")
    class WizardDisplayAfterFileSelection {

        @Test
        @DisplayName("TC-802-001-01: ViewModel initializes on Step 1")
        void viewModelInitializesOnStep1() {
            // Given - Fresh viewModel from setUp()

            // When
            int currentStep = viewModel.getCurrentStep();

            // Then
            assertThat(currentStep)
                .as("Wizard should start at Step 1")
                .isEqualTo(1);
        }

        @Test
        @DisplayName("TC-802-001-02: Step 1 is named 'Column Selection'")
        void step1IsNamedColumnSelection() {
            // Given - Fresh viewModel from setUp()

            // When
            String stepName = viewModel.getCurrentStepName();

            // Then
            assertThat(stepName)
                .as("Step 1 should be named Column Selection")
                .isEqualTo("Column Selection");
        }

        @Test
        @DisplayName("TC-802-001-03: CSV headers can be loaded into wizard")
        void csvHeadersCanBeLoaded() {
            // Given
            viewModel.setCsvHeaders(STANDARD_HEADERS);

            // When
            List<String> loadedHeaders = viewModel.getCsvHeaders();

            // Then
            assertThat(loadedHeaders)
                .as("Headers should be loaded")
                .containsExactlyElementsOf(STANDARD_HEADERS);
        }

        @Test
        @DisplayName("TC-802-001-04: Preview rows are limited to first 5 rows")
        void previewRowsLimitedToFirst5() {
            // Given - More than 5 rows
            List<PreviewRow> manyRows = List.of(
                new PreviewRow(List.of("01/01/2026", "Row 1", "100.00")),
                new PreviewRow(List.of("02/01/2026", "Row 2", "200.00")),
                new PreviewRow(List.of("03/01/2026", "Row 3", "300.00")),
                new PreviewRow(List.of("04/01/2026", "Row 4", "400.00")),
                new PreviewRow(List.of("05/01/2026", "Row 5", "500.00")),
                new PreviewRow(List.of("06/01/2026", "Row 6", "600.00")),
                new PreviewRow(List.of("07/01/2026", "Row 7", "700.00"))
            );

            // When
            viewModel.setPreviewRows(manyRows);

            // Then
            assertThat(viewModel.getPreviewRows())
                .as("Preview should only contain first 5 rows")
                .hasSize(5);
        }
    }

    // ==================== TC-802-004: Date Column Selection Required ====================

    @Nested
    @DisplayName("TC-802-004: Date Column Selection Required")
    class DateColumnSelectionRequired {

        @BeforeEach
        void setupHeaders() {
            viewModel.setCsvHeaders(STANDARD_HEADERS);
            viewModel.setPreviewRows(STANDARD_PREVIEW_ROWS);
        }

        @Test
        @DisplayName("TC-802-004-01: Cannot proceed from Step 1 without Date column")
        void cannotProceedWithoutDateColumn() {
            // Given - Only Description and Amount selected
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");

            // When
            boolean canProceed = viewModel.canProceedFromStep1();

            // Then
            assertThat(canProceed)
                .as("Should not proceed without Date column")
                .isFalse();
        }

        @Test
        @DisplayName("TC-802-004-02: Can proceed when Date column is selected with other required fields")
        void canProceedWithDateColumnSelected() {
            // Given - All required columns selected
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");

            // When
            boolean canProceed = viewModel.canProceedFromStep1();

            // Then
            assertThat(canProceed)
                .as("Should proceed when Date column is selected")
                .isTrue();
        }
    }

    // ==================== TC-802-005: Amount Column Selection Required ====================

    @Nested
    @DisplayName("TC-802-005: Amount Column Selection Required")
    class AmountColumnSelectionRequired {

        @BeforeEach
        void setupHeaders() {
            viewModel.setCsvHeaders(STANDARD_HEADERS);
            viewModel.setPreviewRows(STANDARD_PREVIEW_ROWS);
        }

        @Test
        @DisplayName("TC-802-005-01: Cannot proceed from Step 1 without Amount column")
        void cannotProceedWithoutAmountColumn() {
            // Given - Date and Description selected, but not Amount
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");

            // When
            boolean canProceed = viewModel.canProceedFromStep1();

            // Then
            assertThat(canProceed)
                .as("Should not proceed without Amount column")
                .isFalse();
        }

        @Test
        @DisplayName("TC-802-005-02: Amount column stores selection correctly")
        void amountColumnStoresSelection() {
            // Given/When
            viewModel.setSelectedAmountColumn("Amount");

            // Then
            assertThat(viewModel.getSelectedAmountColumn())
                .as("Amount column selection should be stored")
                .isEqualTo("Amount");
        }
    }

    // ==================== TC-802-006: Description Column Selection Required ====================

    @Nested
    @DisplayName("TC-802-006: Description Column Selection Required")
    class DescriptionColumnSelectionRequired {

        @BeforeEach
        void setupHeaders() {
            viewModel.setCsvHeaders(STANDARD_HEADERS);
            viewModel.setPreviewRows(STANDARD_PREVIEW_ROWS);
        }

        @Test
        @DisplayName("TC-802-006-01: Cannot proceed from Step 1 without Description column")
        void cannotProceedWithoutDescriptionColumn() {
            // Given - Date and Amount selected, but not Description
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");

            // When
            boolean canProceed = viewModel.canProceedFromStep1();

            // Then
            assertThat(canProceed)
                .as("Should not proceed without Description column")
                .isFalse();
        }

        @Test
        @DisplayName("TC-802-006-02: Description column stores selection correctly")
        void descriptionColumnStoresSelection() {
            // Given/When
            viewModel.setSelectedDescriptionColumn("Description");

            // Then
            assertThat(viewModel.getSelectedDescriptionColumn())
                .as("Description column selection should be stored")
                .isEqualTo("Description");
        }
    }

    // ==================== TC-802-008: Auto-detection of Date Column ====================

    @Nested
    @DisplayName("TC-802-008: Column Auto-Detection - Date")
    class ColumnAutoDetectionDate {

        @Test
        @DisplayName("TC-802-008-01: Auto-detects column named 'Date'")
        void autoDetectsDateColumn() {
            // Given
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));

            // When
            viewModel.autoDetectColumns();

            // Then
            assertThat(viewModel.getSelectedDateColumn())
                .as("Should auto-detect Date column")
                .isEqualTo("Date");
        }

        @Test
        @DisplayName("TC-802-008-02: Auto-detects column named 'Transaction Date'")
        void autoDetectsTransactionDateColumn() {
            // Given
            viewModel.setCsvHeaders(List.of("Transaction Date", "Desc", "Value"));

            // When
            viewModel.autoDetectColumns();

            // Then
            assertThat(viewModel.getSelectedDateColumn())
                .as("Should auto-detect Transaction Date column")
                .isEqualTo("Transaction Date");
        }

        @Test
        @DisplayName("TC-802-008-03: Auto-detects column containing 'date' keyword (case insensitive)")
        void autoDetectsDateKeywordCaseInsensitive() {
            // Given
            viewModel.setCsvHeaders(List.of("POSTING DATE", "Description", "Amount"));

            // When
            viewModel.autoDetectColumns();

            // Then
            assertThat(viewModel.getSelectedDateColumn())
                .as("Should auto-detect POSTING DATE column (case insensitive)")
                .isEqualTo("POSTING DATE");
        }
    }

    // ==================== TC-802-009: Auto-detection of Amount Column ====================

    @Nested
    @DisplayName("TC-802-009: Column Auto-Detection - Amount")
    class ColumnAutoDetectionAmount {

        @Test
        @DisplayName("TC-802-009-01: Auto-detects column named 'Amount'")
        void autoDetectsAmountColumn() {
            // Given
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));

            // When
            viewModel.autoDetectColumns();

            // Then
            assertThat(viewModel.getSelectedAmountColumn())
                .as("Should auto-detect Amount column")
                .isEqualTo("Amount");
        }

        @Test
        @DisplayName("TC-802-009-02: Auto-detects column named 'Value'")
        void autoDetectsValueColumn() {
            // Given
            viewModel.setCsvHeaders(List.of("Date", "Description", "Value"));

            // When
            viewModel.autoDetectColumns();

            // Then
            assertThat(viewModel.getSelectedAmountColumn())
                .as("Should auto-detect Value column as Amount")
                .isEqualTo("Value");
        }

        @Test
        @DisplayName("TC-802-009-03: Auto-detects column containing 'amount' keyword")
        void autoDetectsAmountKeyword() {
            // Given
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount (GBP)"));

            // When
            viewModel.autoDetectColumns();

            // Then
            assertThat(viewModel.getSelectedAmountColumn())
                .as("Should auto-detect Amount (GBP) column")
                .isEqualTo("Amount (GBP)");
        }
    }

    // ==================== TC-802-012: Amount Interpretation Defaults to STANDARD ====================

    @Nested
    @DisplayName("TC-802-012: Amount Interpretation - Standard Option")
    class AmountInterpretationStandardOption {

        @Test
        @DisplayName("TC-802-012-01: Amount interpretation defaults to STANDARD")
        void amountInterpretationDefaultsToStandard() {
            // Given - Fresh viewModel from setUp()

            // When
            AmountInterpretation defaultInterpretation = viewModel.getAmountInterpretation();

            // Then
            assertThat(defaultInterpretation)
                .as("Amount interpretation should default to STANDARD")
                .isEqualTo(AmountInterpretation.STANDARD);
        }

        @Test
        @DisplayName("TC-802-012-02: STANDARD interpretation: positive = INCOME")
        void standardInterpretationPositiveMeansIncome() {
            // Given
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);

            // When
            String positiveMeaning = viewModel.getPositiveMeaning();

            // Then
            assertThat(positiveMeaning)
                .as("In STANDARD mode, positive should mean INCOME")
                .isEqualTo("INCOME");
        }

        @Test
        @DisplayName("TC-802-012-03: STANDARD interpretation: negative = EXPENSE")
        void standardInterpretationNegativeMeansExpense() {
            // Given
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);

            // When
            String negativeMeaning = viewModel.getNegativeMeaning();

            // Then
            assertThat(negativeMeaning)
                .as("In STANDARD mode, negative should mean EXPENSE")
                .isEqualTo("EXPENSE");
        }

        @Test
        @DisplayName("TC-802-012-04: Can proceed from Step 2 with STANDARD interpretation (no extra columns needed)")
        void canProceedFromStep2WithStandardInterpretation() {
            // Given - Setup and move to Step 2
            setupForStep2();
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);

            // When
            boolean canProceed = viewModel.canProceedFromStep2();

            // Then
            assertThat(canProceed)
                .as("Should proceed from Step 2 with STANDARD interpretation")
                .isTrue();
        }
    }

    // ==================== TC-802-016: Summary Shows Income Count ====================

    @Nested
    @DisplayName("TC-802-016: Summary Calculation - Income Count")
    class SummaryCalculationIncomeCount {

        @Test
        @DisplayName("TC-802-016-01: Summary shows correct income transaction count")
        void summaryShowsCorrectIncomeCount() {
            // Given - Setup and move to Step 3
            setupForStep3();

            // When
            int incomeCount = viewModel.getPreviewIncomeCount();

            // Then - In sample data with STANDARD interpretation, 2 rows have positive amounts
            assertThat(incomeCount)
                .as("Income count should match positive amounts in preview")
                .isEqualTo(2);
        }

        @Test
        @DisplayName("TC-802-016-02: Income count formatted as 'X transactions'")
        void incomeCountFormattedCorrectly() {
            // Given
            setupForStep3();

            // When
            String incomeSummary = viewModel.getIncomeSummaryText();

            // Then
            assertThat(incomeSummary)
                .as("Income summary should show count and 'transactions'")
                .matches("\\d+ transactions?");
        }
    }

    // ==================== TC-802-017: Summary Shows Expense Count ====================

    @Nested
    @DisplayName("TC-802-017: Summary Calculation - Expense Count")
    class SummaryCalculationExpenseCount {

        @Test
        @DisplayName("TC-802-017-01: Summary shows correct expense transaction count")
        void summaryShowsCorrectExpenseCount() {
            // Given - Setup and move to Step 3
            setupForStep3();

            // When
            int expenseCount = viewModel.getPreviewExpenseCount();

            // Then - In sample data with STANDARD interpretation, 3 rows have negative amounts
            assertThat(expenseCount)
                .as("Expense count should match negative amounts in preview")
                .isEqualTo(3);
        }

        @Test
        @DisplayName("TC-802-017-02: Expense count formatted as 'X transactions'")
        void expenseCountFormattedCorrectly() {
            // Given
            setupForStep3();

            // When
            String expenseSummary = viewModel.getExpenseSummaryText();

            // Then
            assertThat(expenseSummary)
                .as("Expense summary should show count and 'transactions'")
                .matches("\\d+ transactions?");
        }
    }

    // ==================== TC-802-018: Summary Shows Income Total ====================

    @Nested
    @DisplayName("TC-802-018: Summary Calculation - Income Total")
    class SummaryCalculationIncomeTotal {

        @Test
        @DisplayName("TC-802-018-01: Summary calculates correct income total")
        void summaryCalculatesCorrectIncomeTotal() {
            // Given - Setup and move to Step 3
            setupForStep3();

            // When
            BigDecimal incomeTotal = viewModel.getPreviewIncomeTotal();

            // Then - Income in sample: 1250.00 + 2500.00 = 3750.00
            assertThat(incomeTotal)
                .as("Income total should sum positive amounts")
                .isEqualByComparingTo(new BigDecimal("3750.00"));
        }

        @Test
        @DisplayName("TC-802-018-02: Income total is non-negative (absolute value)")
        void incomeTotalIsNonNegative() {
            // Given
            setupForStep3();

            // When
            BigDecimal incomeTotal = viewModel.getPreviewIncomeTotal();

            // Then
            assertThat(incomeTotal)
                .as("Income total should be non-negative")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("TC-802-018-03: Formatted income total includes currency symbol or + prefix")
        void formattedIncomeTotalIncludesPrefix() {
            // Given
            setupForStep3();

            // When
            String formattedTotal = viewModel.getFormattedIncomeTotal();

            // Then
            assertThat(formattedTotal)
                .as("Formatted income total should include + prefix")
                .startsWith("+");
        }
    }

    // ==================== TC-802-019: Summary Shows Expense Total ====================

    @Nested
    @DisplayName("TC-802-019: Summary Calculation - Expense Total")
    class SummaryCalculationExpenseTotal {

        @Test
        @DisplayName("TC-802-019-01: Summary calculates correct expense total")
        void summaryCalculatesCorrectExpenseTotal() {
            // Given - Setup and move to Step 3
            setupForStep3();

            // When
            BigDecimal expenseTotal = viewModel.getPreviewExpenseTotal();

            // Then - Expenses in sample (absolute): 45.99 + 32.50 + 45.00 = 123.49
            assertThat(expenseTotal)
                .as("Expense total should sum absolute values of negative amounts")
                .isEqualByComparingTo(new BigDecimal("123.49"));
        }

        @Test
        @DisplayName("TC-802-019-02: Expense total is non-negative (absolute value)")
        void expenseTotalIsNonNegative() {
            // Given
            setupForStep3();

            // When
            BigDecimal expenseTotal = viewModel.getPreviewExpenseTotal();

            // Then
            assertThat(expenseTotal)
                .as("Expense total should be non-negative (absolute)")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("TC-802-019-03: Formatted expense total includes - prefix")
        void formattedExpenseTotalIncludesPrefix() {
            // Given
            setupForStep3();

            // When
            String formattedTotal = viewModel.getFormattedExpenseTotal();

            // Then
            assertThat(formattedTotal)
                .as("Formatted expense total should include - prefix")
                .startsWith("-");
        }
    }

    // ==================== TC-802-022: Confirm Button Disabled Until All Mapped ====================

    @Nested
    @DisplayName("TC-802-022: Confirm Mapping Button Disabled Until All Mapped")
    class ConfirmButtonDisabledUntilAllMapped {

        @Test
        @DisplayName("TC-802-022-01: Cannot proceed from Step 1 without all required columns")
        void cannotProceedFromStep1WithoutAllRequiredColumns() {
            // Given - Only Date column selected
            viewModel.setCsvHeaders(STANDARD_HEADERS);
            viewModel.setSelectedDateColumn("Date");

            // When
            boolean canProceed = viewModel.canProceedFromStep1();

            // Then
            assertThat(canProceed)
                .as("Should not proceed without all required columns")
                .isFalse();
        }

        @Test
        @DisplayName("TC-802-022-02: Cannot proceed from Step 1 without date format")
        void cannotProceedFromStep1WithoutDateFormat() {
            // Given - All columns selected but no date format
            viewModel.setCsvHeaders(STANDARD_HEADERS);
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");

            // When
            boolean canProceed = viewModel.canProceedFromStep1();

            // Then
            assertThat(canProceed)
                .as("Should not proceed without date format")
                .isFalse();
        }

        @Test
        @DisplayName("TC-802-022-03: Can proceed from Step 1 with all required fields")
        void canProceedFromStep1WithAllRequiredFields() {
            // Given
            viewModel.setCsvHeaders(STANDARD_HEADERS);
            viewModel.setPreviewRows(STANDARD_PREVIEW_ROWS);
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");

            // When
            boolean canProceed = viewModel.canProceedFromStep1();

            // Then
            assertThat(canProceed)
                .as("Should proceed with all required fields")
                .isTrue();
        }

        @Test
        @DisplayName("TC-802-022-04: Mapping not confirmed initially")
        void mappingNotConfirmedInitially() {
            // Given - Fresh viewModel from setUp()

            // When
            boolean confirmed = viewModel.isMappingConfirmed();

            // Then
            assertThat(confirmed)
                .as("Mapping should not be confirmed initially")
                .isFalse();
        }

        @Test
        @DisplayName("TC-802-022-05: Mapping can be confirmed after wizard completion")
        void mappingCanBeConfirmedAfterWizardCompletion() {
            // Given
            setupForStep3();

            // When
            viewModel.confirmMapping();

            // Then
            assertThat(viewModel.isMappingConfirmed())
                .as("Mapping should be confirmed after confirmMapping()")
                .isTrue();
        }
    }

    // ==================== TC-802-029: Negative Amounts Parsed Correctly ====================

    @Nested
    @DisplayName("TC-802-029: Amount Parsing - Negative Values")
    class AmountParsingNegativeValues {

        @Test
        @DisplayName("TC-802-029-01: Negative amounts classified as EXPENSE with STANDARD interpretation")
        void negativeAmountsClassifiedAsExpense() {
            // Given
            setupForStep3();

            // When
            List<ClassifiedPreviewRow> classified = viewModel.getClassifiedPreviewRows();

            // Then - Find a row with negative amount
            ClassifiedPreviewRow negativeRow = classified.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .findFirst()
                .orElse(null);

            assertThat(negativeRow)
                .as("Should find a row with negative amount")
                .isNotNull();
            assertThat(negativeRow.getClassification())
                .as("Negative amount should be classified as EXPENSE")
                .isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("TC-802-029-02: Positive amounts classified as INCOME with STANDARD interpretation")
        void positiveAmountsClassifiedAsIncome() {
            // Given
            setupForStep3();

            // When
            List<ClassifiedPreviewRow> classified = viewModel.getClassifiedPreviewRows();

            // Then - Find a row with positive amount
            ClassifiedPreviewRow positiveRow = classified.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(null);

            assertThat(positiveRow)
                .as("Should find a row with positive amount")
                .isNotNull();
            assertThat(positiveRow.getClassification())
                .as("Positive amount should be classified as INCOME")
                .isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("TC-802-029-03: Classifications swap with INVERTED interpretation")
        void classificationsSwapWithInvertedInterpretation() {
            // Given
            setupForStep3();

            // Get counts with STANDARD
            int standardIncomeCount = viewModel.getPreviewIncomeCount();
            int standardExpenseCount = viewModel.getPreviewExpenseCount();

            // When - Switch to INVERTED
            viewModel.setAmountInterpretation(AmountInterpretation.INVERTED);

            // Then - Counts should swap
            assertThat(viewModel.getPreviewIncomeCount())
                .as("Income count should now equal original expense count")
                .isEqualTo(standardExpenseCount);
            assertThat(viewModel.getPreviewExpenseCount())
                .as("Expense count should now equal original income count")
                .isEqualTo(standardIncomeCount);
        }
    }

    // ==================== TC-802-030: Parentheses Indicate Negative Amounts ====================

    @Nested
    @DisplayName("TC-802-030: Amount Parsing - Parentheses Notation")
    class AmountParsingParenthesesNotation {

        @Test
        @DisplayName("TC-802-030-01: Amounts in parentheses parsed as negative")
        void amountsInParenthesesParsedAsNegative() {
            // Given - Setup with parentheses format data
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount", "Balance", "Category"));
            viewModel.setPreviewRows(PARENTHESES_PREVIEW_ROWS);
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);
            viewModel.goToNextStep();
            viewModel.goToNextStep();

            // When
            List<ClassifiedPreviewRow> classified = viewModel.getClassifiedPreviewRows();

            // Then - First row "(45.99)" should be classified as EXPENSE (negative)
            ClassifiedPreviewRow firstRow = classified.get(0);
            assertThat(firstRow.getAmount())
                .as("(45.99) should be parsed as -45.99")
                .isEqualByComparingTo(new BigDecimal("-45.99"));
            assertThat(firstRow.getClassification())
                .as("Parentheses amount should be classified as EXPENSE")
                .isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("TC-802-030-02: Mix of parentheses and normal values parsed correctly")
        void mixOfParenthesesAndNormalValuesParsed() {
            // Given - Setup with parentheses format data
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount", "Balance", "Category"));
            viewModel.setPreviewRows(PARENTHESES_PREVIEW_ROWS);
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);
            viewModel.goToNextStep();
            viewModel.goToNextStep();

            // When
            List<ClassifiedPreviewRow> classified = viewModel.getClassifiedPreviewRows();

            // Then - Second row "1250.00" should be INCOME (positive)
            ClassifiedPreviewRow secondRow = classified.get(1);
            assertThat(secondRow.getAmount())
                .as("1250.00 should be parsed as positive")
                .isEqualByComparingTo(new BigDecimal("1250.00"));
            assertThat(secondRow.getClassification())
                .as("Positive amount should be classified as INCOME")
                .isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("TC-802-030-03: Expense count correct with parentheses notation")
        void expenseCountCorrectWithParenthesesNotation() {
            // Given - Setup with parentheses format data
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount", "Balance", "Category"));
            viewModel.setPreviewRows(PARENTHESES_PREVIEW_ROWS);
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);
            viewModel.goToNextStep();
            viewModel.goToNextStep();

            // When
            int expenseCount = viewModel.getPreviewExpenseCount();

            // Then - 2 rows with parentheses in sample data
            assertThat(expenseCount)
                .as("Expense count should include parentheses amounts")
                .isEqualTo(2);
        }
    }

    // ==================== Additional Integration Tests ====================

    @Nested
    @DisplayName("Navigation and State Management")
    class NavigationAndStateManagement {

        @Test
        @DisplayName("Step names are correct for all steps")
        void stepNamesCorrectForAllSteps() {
            // Given
            setupForStep3();

            // When/Then - Step 1
            viewModel.goToPreviousStep();
            viewModel.goToPreviousStep();
            assertThat(viewModel.getCurrentStepName())
                .isEqualTo("Column Selection");

            // When/Then - Step 2
            viewModel.goToNextStep();
            assertThat(viewModel.getCurrentStepName())
                .isEqualTo("Amount Interpretation");

            // When/Then - Step 3
            viewModel.goToNextStep();
            assertThat(viewModel.getCurrentStepName())
                .isEqualTo("Summary & Confirmation");
        }

        @Test
        @DisplayName("Selections preserved on back navigation")
        void selectionsPreservedOnBackNavigation() {
            // Given
            setupForStep3();
            String dateColumn = viewModel.getSelectedDateColumn();
            AmountInterpretation interpretation = viewModel.getAmountInterpretation();

            // When - Navigate back and forward
            viewModel.goToPreviousStep();
            viewModel.goToPreviousStep();
            viewModel.goToNextStep();
            viewModel.goToNextStep();

            // Then - Selections should be preserved
            assertThat(viewModel.getSelectedDateColumn())
                .isEqualTo(dateColumn);
            assertThat(viewModel.getAmountInterpretation())
                .isEqualTo(interpretation);
        }

        @Test
        @DisplayName("Reset clears all selections but keeps CSV data")
        void resetClearsSelectionsButKeepsCsvData() {
            // Given
            setupForStep3();
            viewModel.confirmMapping();

            // When
            viewModel.reset();

            // Then - Selections cleared
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            assertThat(viewModel.getSelectedDateColumn()).isNull();
            assertThat(viewModel.getSelectedDescriptionColumn()).isNull();
            assertThat(viewModel.getSelectedAmountColumn()).isNull();
            assertThat(viewModel.isMappingConfirmed()).isFalse();
            assertThat(viewModel.getAmountInterpretation()).isEqualTo(AmountInterpretation.STANDARD);

            // CSV data preserved
            assertThat(viewModel.getCsvHeaders()).isNotEmpty();
            assertThat(viewModel.getPreviewRows()).isNotEmpty();
        }

        @Test
        @DisplayName("ClearAll removes everything including CSV data")
        void clearAllRemovesEverything() {
            // Given
            setupForStep3();

            // When
            viewModel.clearAll();

            // Then - Everything cleared
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            assertThat(viewModel.getCsvHeaders()).isEmpty();
            assertThat(viewModel.getPreviewRows()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Mapping Result Building")
    class MappingResultBuilding {

        @Test
        @DisplayName("buildColumnMapping returns complete mapping")
        void buildColumnMappingReturnsCompleteMapping() {
            // Given
            setupForStep3();

            // When
            ColumnMapping result = viewModel.buildColumnMapping();

            // Then
            assertThat(result.getDateColumn()).isEqualTo("Date");
            assertThat(result.getDescriptionColumn()).isEqualTo("Description");
            assertThat(result.getAmountColumn()).isEqualTo("Amount");
            assertThat(result.getDateFormat()).isEqualTo("dd/MM/yyyy");
            assertThat(result.getAmountInterpretation()).isEqualTo(AmountInterpretation.STANDARD);
        }

        @Test
        @DisplayName("buildColumnMapping includes category if selected")
        void buildColumnMappingIncludesCategoryIfSelected() {
            // Given
            setupForStep3();
            viewModel.setSelectedCategoryColumn("Category");

            // When
            ColumnMapping result = viewModel.buildColumnMapping();

            // Then
            assertThat(result.getCategoryColumn()).isEqualTo("Category");
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Sets up the viewModel for Step 2 (Amount Interpretation).
     */
    private void setupForStep2() {
        viewModel.setCsvHeaders(STANDARD_HEADERS);
        viewModel.setPreviewRows(STANDARD_PREVIEW_ROWS);
        viewModel.setSelectedDateColumn("Date");
        viewModel.setSelectedDescriptionColumn("Description");
        viewModel.setSelectedAmountColumn("Amount");
        viewModel.setSelectedDateFormat("dd/MM/yyyy");
        viewModel.goToNextStep();
    }

    /**
     * Sets up the viewModel for Step 3 (Summary & Confirmation).
     */
    private void setupForStep3() {
        viewModel.setCsvHeaders(STANDARD_HEADERS);
        viewModel.setPreviewRows(STANDARD_PREVIEW_ROWS);
        viewModel.setSelectedDateColumn("Date");
        viewModel.setSelectedDescriptionColumn("Description");
        viewModel.setSelectedAmountColumn("Amount");
        viewModel.setSelectedDateFormat("dd/MM/yyyy");
        viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);
        viewModel.goToNextStep(); // to Step 2
        viewModel.goToNextStep(); // to Step 3
    }
}
