package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for ColumnMappingViewModel.
 * Tests the column mapping wizard state management, amount interpretation,
 * preview calculations, and mapping preferences.
 *
 * SE-802: Bank Import Column Mapping Wizard
 */
@DisplayName("ColumnMappingViewModel")
class ColumnMappingViewModelTest {

    private ColumnMappingViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new ColumnMappingViewModel();
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should initialize on step 1 (Column Selection)")
        void shouldInitializeOnStepOne() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            assertThat(viewModel.getCurrentStepName()).isEqualTo("Column Selection");
        }

        @Test
        @DisplayName("should initialize with no columns selected")
        void shouldInitializeWithNoColumnsSelected() {
            assertThat(viewModel.getSelectedDateColumn()).isNull();
            assertThat(viewModel.getSelectedDescriptionColumn()).isNull();
            assertThat(viewModel.getSelectedAmountColumn()).isNull();
        }

        @Test
        @DisplayName("should initialize with STANDARD amount interpretation")
        void shouldInitializeWithStandardAmountInterpretation() {
            assertThat(viewModel.getAmountInterpretation()).isEqualTo(AmountInterpretation.STANDARD);
        }

        @Test
        @DisplayName("should initialize with empty preview data")
        void shouldInitializeWithEmptyPreviewData() {
            assertThat(viewModel.getPreviewRows()).isEmpty();
            assertThat(viewModel.getCsvHeaders()).isEmpty();
        }

        @Test
        @DisplayName("should initialize with no mapping confirmed")
        void shouldInitializeWithNoMappingConfirmed() {
            assertThat(viewModel.isMappingConfirmed()).isFalse();
        }

        @Test
        @DisplayName("should initialize with save preference disabled")
        void shouldInitializeWithSavePreferenceDisabled() {
            assertThat(viewModel.isSavePreferenceSelected()).isFalse();
        }

        @Test
        @DisplayName("should not allow next without required columns")
        void shouldNotAllowNextWithoutRequiredColumns() {
            assertThat(viewModel.canProceedFromStep1()).isFalse();
        }
    }

    @Nested
    @DisplayName("Step 1: Column Selection")
    class Step1ColumnSelection {

        @BeforeEach
        void setupHeaders() {
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount", "Balance", "Category"));
            viewModel.setPreviewRows(createSamplePreviewRows());
        }

        @Test
        @DisplayName("should store CSV headers")
        void shouldStoreCsvHeaders() {
            assertThat(viewModel.getCsvHeaders()).containsExactly("Date", "Description", "Amount", "Balance", "Category");
        }

        @Test
        @DisplayName("should store preview rows (first 5 rows)")
        void shouldStorePreviewRows() {
            assertThat(viewModel.getPreviewRows()).hasSize(5);
        }

        @Test
        @DisplayName("should select date column")
        void shouldSelectDateColumn() {
            viewModel.setSelectedDateColumn("Date");
            assertThat(viewModel.getSelectedDateColumn()).isEqualTo("Date");
        }

        @Test
        @DisplayName("should select description column")
        void shouldSelectDescriptionColumn() {
            viewModel.setSelectedDescriptionColumn("Description");
            assertThat(viewModel.getSelectedDescriptionColumn()).isEqualTo("Description");
        }

        @Test
        @DisplayName("should select amount column")
        void shouldSelectAmountColumn() {
            viewModel.setSelectedAmountColumn("Amount");
            assertThat(viewModel.getSelectedAmountColumn()).isEqualTo("Amount");
        }

        @Test
        @DisplayName("should select optional category column")
        void shouldSelectOptionalCategoryColumn() {
            viewModel.setSelectedCategoryColumn("Category");
            assertThat(viewModel.getSelectedCategoryColumn()).isEqualTo("Category");
        }

        @Test
        @DisplayName("should allow proceed when all required columns selected")
        void shouldAllowProceedWhenAllRequiredColumnsSelected() {
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");

            assertThat(viewModel.canProceedFromStep1()).isTrue();
        }

        @Test
        @DisplayName("should not allow proceed without date column")
        void shouldNotAllowProceedWithoutDateColumn() {
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");

            assertThat(viewModel.canProceedFromStep1()).isFalse();
        }

        @Test
        @DisplayName("should not allow proceed without description column")
        void shouldNotAllowProceedWithoutDescriptionColumn() {
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");

            assertThat(viewModel.canProceedFromStep1()).isFalse();
        }

        @Test
        @DisplayName("should not allow proceed without amount column")
        void shouldNotAllowProceedWithoutAmountColumn() {
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");

            assertThat(viewModel.canProceedFromStep1()).isFalse();
        }

        @Test
        @DisplayName("should not allow proceed without date format")
        void shouldNotAllowProceedWithoutDateFormat() {
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");

            assertThat(viewModel.canProceedFromStep1()).isFalse();
        }

        @Test
        @DisplayName("should auto-suggest date column from common names")
        void shouldAutoSuggestDateColumn() {
            viewModel.autoDetectColumns();
            assertThat(viewModel.getSelectedDateColumn()).isEqualTo("Date");
        }

        @Test
        @DisplayName("should auto-suggest description column from common names")
        void shouldAutoSuggestDescriptionColumn() {
            viewModel.autoDetectColumns();
            assertThat(viewModel.getSelectedDescriptionColumn()).isEqualTo("Description");
        }

        @Test
        @DisplayName("should auto-suggest amount column from common names")
        void shouldAutoSuggestAmountColumn() {
            viewModel.autoDetectColumns();
            assertThat(viewModel.getSelectedAmountColumn()).isEqualTo("Amount");
        }

        @Test
        @DisplayName("should detect date column variations")
        void shouldDetectDateColumnVariations() {
            viewModel.setCsvHeaders(List.of("Transaction Date", "Desc", "Value"));
            viewModel.autoDetectColumns();
            assertThat(viewModel.getSelectedDateColumn()).isEqualTo("Transaction Date");
        }

        @Test
        @DisplayName("should detect amount column variations")
        void shouldDetectAmountColumnVariations() {
            viewModel.setCsvHeaders(List.of("Date", "Description", "Value"));
            viewModel.autoDetectColumns();
            assertThat(viewModel.getSelectedAmountColumn()).isEqualTo("Value");
        }

        @Test
        @DisplayName("should get column sample value for dropdown display")
        void shouldGetColumnSampleValue() {
            String sample = viewModel.getColumnSampleValue("Date");
            assertThat(sample).isNotBlank();
        }

        @Test
        @DisplayName("should format column option for dropdown")
        void shouldFormatColumnOptionForDropdown() {
            String formatted = viewModel.formatColumnOption("Date");
            // Format: "Column Name (sample value...)"
            assertThat(formatted).startsWith("Date");
            assertThat(formatted).contains("(");
        }
    }

    @Nested
    @DisplayName("Step 2: Amount Interpretation")
    class Step2AmountInterpretation {

        @BeforeEach
        void setupStep2() {
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
            viewModel.setPreviewRows(createSamplePreviewRows());
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
            viewModel.goToNextStep();
        }

        @Test
        @DisplayName("should be on step 2 after proceeding from step 1")
        void shouldBeOnStep2() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
            assertThat(viewModel.getCurrentStepName()).isEqualTo("Amount Interpretation");
        }

        @Test
        @DisplayName("should set STANDARD interpretation - positive=income, negative=expense")
        void shouldSetStandardInterpretation() {
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);

            assertThat(viewModel.getAmountInterpretation()).isEqualTo(AmountInterpretation.STANDARD);
            assertThat(viewModel.getPositiveMeaning()).isEqualTo("INCOME");
            assertThat(viewModel.getNegativeMeaning()).isEqualTo("EXPENSE");
        }

        @Test
        @DisplayName("should set INVERTED interpretation - positive=expense, negative=income")
        void shouldSetInvertedInterpretation() {
            viewModel.setAmountInterpretation(AmountInterpretation.INVERTED);

            assertThat(viewModel.getAmountInterpretation()).isEqualTo(AmountInterpretation.INVERTED);
            assertThat(viewModel.getPositiveMeaning()).isEqualTo("EXPENSE");
            assertThat(viewModel.getNegativeMeaning()).isEqualTo("INCOME");
        }

        @Test
        @DisplayName("should set SEPARATE_COLUMNS interpretation")
        void shouldSetSeparateColumnsInterpretation() {
            viewModel.setAmountInterpretation(AmountInterpretation.SEPARATE_COLUMNS);

            assertThat(viewModel.getAmountInterpretation()).isEqualTo(AmountInterpretation.SEPARATE_COLUMNS);
        }

        @Test
        @DisplayName("should require income/expense columns for SEPARATE_COLUMNS")
        void shouldRequireColumnsForSeparateColumnsMode() {
            viewModel.setAmountInterpretation(AmountInterpretation.SEPARATE_COLUMNS);

            assertThat(viewModel.canProceedFromStep2()).isFalse();

            viewModel.setSelectedIncomeColumn("Income");
            viewModel.setSelectedExpenseColumn("Expense");

            assertThat(viewModel.canProceedFromStep2()).isTrue();
        }

        @Test
        @DisplayName("should find positive amount example from data")
        void shouldFindPositiveAmountExample() {
            BigDecimal example = viewModel.getPositiveAmountExample();
            assertThat(example).isNotNull();
            assertThat(example).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should find negative amount example from data")
        void shouldFindNegativeAmountExample() {
            BigDecimal example = viewModel.getNegativeAmountExample();
            assertThat(example).isNotNull();
            assertThat(example).isLessThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should format example with interpretation - STANDARD")
        void shouldFormatExampleWithStandardInterpretation() {
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);

            String positiveFormatted = viewModel.getFormattedPositiveExample();
            String negativeFormatted = viewModel.getFormattedNegativeExample();

            assertThat(positiveFormatted).contains("INCOME");
            assertThat(negativeFormatted).contains("EXPENSE");
        }

        @Test
        @DisplayName("should format example with interpretation - INVERTED")
        void shouldFormatExampleWithInvertedInterpretation() {
            viewModel.setAmountInterpretation(AmountInterpretation.INVERTED);

            String positiveFormatted = viewModel.getFormattedPositiveExample();
            String negativeFormatted = viewModel.getFormattedNegativeExample();

            assertThat(positiveFormatted).contains("EXPENSE");
            assertThat(negativeFormatted).contains("INCOME");
        }

        @Test
        @DisplayName("should allow proceed with standard interpretation")
        void shouldAllowProceedWithStandardInterpretation() {
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);
            assertThat(viewModel.canProceedFromStep2()).isTrue();
        }

        @Test
        @DisplayName("should allow proceed with inverted interpretation")
        void shouldAllowProceedWithInvertedInterpretation() {
            viewModel.setAmountInterpretation(AmountInterpretation.INVERTED);
            assertThat(viewModel.canProceedFromStep2()).isTrue();
        }
    }

    @Nested
    @DisplayName("Step 3: Summary & Confirmation")
    class Step3SummaryConfirmation {

        @BeforeEach
        void setupStep3() {
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
            viewModel.setPreviewRows(createSamplePreviewRows());
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);
            viewModel.goToNextStep(); // to step 2
            viewModel.goToNextStep(); // to step 3
        }

        @Test
        @DisplayName("should be on step 3 after proceeding from step 2")
        void shouldBeOnStep3() {
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);
            assertThat(viewModel.getCurrentStepName()).isEqualTo("Summary & Confirmation");
        }

        @Test
        @DisplayName("should calculate income transaction count")
        void shouldCalculateIncomeTransactionCount() {
            int incomeCount = viewModel.getPreviewIncomeCount();
            assertThat(incomeCount).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should calculate expense transaction count")
        void shouldCalculateExpenseTransactionCount() {
            int expenseCount = viewModel.getPreviewExpenseCount();
            assertThat(expenseCount).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should calculate income total")
        void shouldCalculateIncomeTotal() {
            BigDecimal incomeTotal = viewModel.getPreviewIncomeTotal();
            assertThat(incomeTotal).isNotNull();
            assertThat(incomeTotal).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should calculate expense total")
        void shouldCalculateExpenseTotal() {
            BigDecimal expenseTotal = viewModel.getPreviewExpenseTotal();
            assertThat(expenseTotal).isNotNull();
            assertThat(expenseTotal).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should format income summary")
        void shouldFormatIncomeSummary() {
            String summary = viewModel.getIncomeSummaryText();
            // Format: "X transactions"
            assertThat(summary).containsPattern("\\d+ transaction");
        }

        @Test
        @DisplayName("should format expense summary")
        void shouldFormatExpenseSummary() {
            String summary = viewModel.getExpenseSummaryText();
            // Format: "X transactions"
            assertThat(summary).containsPattern("\\d+ transaction");
        }

        @Test
        @DisplayName("should format income total as GBP")
        void shouldFormatIncomeTotalAsGbp() {
            String formatted = viewModel.getFormattedIncomeTotal();
            assertThat(formatted).matches("\\+?[\\p{Sc}]?[0-9,]+\\.[0-9]{2}");
        }

        @Test
        @DisplayName("should format expense total as GBP")
        void shouldFormatExpenseTotalAsGbp() {
            String formatted = viewModel.getFormattedExpenseTotal();
            assertThat(formatted).matches("-?[\\p{Sc}]?[0-9,]+\\.[0-9]{2}");
        }

        @Test
        @DisplayName("should toggle save preference")
        void shouldToggleSavePreference() {
            assertThat(viewModel.isSavePreferenceSelected()).isFalse();

            viewModel.setSavePreferenceSelected(true);
            assertThat(viewModel.isSavePreferenceSelected()).isTrue();

            viewModel.setSavePreferenceSelected(false);
            assertThat(viewModel.isSavePreferenceSelected()).isFalse();
        }

        @Test
        @DisplayName("should confirm mapping")
        void shouldConfirmMapping() {
            assertThat(viewModel.isMappingConfirmed()).isFalse();

            viewModel.confirmMapping();

            assertThat(viewModel.isMappingConfirmed()).isTrue();
        }

        @Test
        @DisplayName("should recalculate counts when interpretation changes")
        void shouldRecalculateCountsWhenInterpretationChanges() {
            int originalIncomeCount = viewModel.getPreviewIncomeCount();
            int originalExpenseCount = viewModel.getPreviewExpenseCount();

            // Change interpretation to inverted
            viewModel.goToPreviousStep(); // back to step 2
            viewModel.setAmountInterpretation(AmountInterpretation.INVERTED);
            viewModel.goToNextStep(); // back to step 3

            int newIncomeCount = viewModel.getPreviewIncomeCount();
            int newExpenseCount = viewModel.getPreviewExpenseCount();

            // With inverted interpretation, income and expense counts should swap
            assertThat(newIncomeCount).isEqualTo(originalExpenseCount);
            assertThat(newExpenseCount).isEqualTo(originalIncomeCount);
        }
    }

    @Nested
    @DisplayName("Navigation")
    class Navigation {

        @BeforeEach
        void setupNavigation() {
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
            viewModel.setPreviewRows(createSamplePreviewRows());
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
        }

        @Test
        @DisplayName("should navigate from step 1 to step 2")
        void shouldNavigateFromStep1ToStep2() {
            viewModel.goToNextStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("should navigate from step 2 to step 3")
        void shouldNavigateFromStep2ToStep3() {
            viewModel.goToNextStep(); // to 2
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);
            viewModel.goToNextStep(); // to 3
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);
        }

        @Test
        @DisplayName("should navigate back from step 2 to step 1")
        void shouldNavigateBackFromStep2ToStep1() {
            viewModel.goToNextStep(); // to 2
            viewModel.goToPreviousStep(); // back to 1
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should navigate back from step 3 to step 2")
        void shouldNavigateBackFromStep3ToStep2() {
            viewModel.goToNextStep(); // to 2
            viewModel.goToNextStep(); // to 3
            viewModel.goToPreviousStep(); // back to 2
            assertThat(viewModel.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("should not navigate before step 1")
        void shouldNotNavigateBeforeStep1() {
            viewModel.goToPreviousStep();
            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("should not navigate past step 3")
        void shouldNotNavigatePastStep3() {
            viewModel.goToNextStep(); // to 2
            viewModel.goToNextStep(); // to 3
            viewModel.goToNextStep(); // try to go past 3
            assertThat(viewModel.getCurrentStep()).isEqualTo(3);
        }

        @Test
        @DisplayName("should allow previous from step 2")
        void shouldAllowPreviousFromStep2() {
            viewModel.goToNextStep();
            assertThat(viewModel.canGoPrevious()).isTrue();
        }

        @Test
        @DisplayName("should not allow previous from step 1")
        void shouldNotAllowPreviousFromStep1() {
            assertThat(viewModel.canGoPrevious()).isFalse();
        }

        @Test
        @DisplayName("should not allow next from step 1 without required fields")
        void shouldNotAllowNextFromStep1WithoutRequiredFields() {
            ColumnMappingViewModel emptyViewModel = new ColumnMappingViewModel();
            assertThat(emptyViewModel.canGoNext()).isFalse();
        }

        @Test
        @DisplayName("should allow next from step 1 with required fields")
        void shouldAllowNextFromStep1WithRequiredFields() {
            assertThat(viewModel.canGoNext()).isTrue();
        }
    }

    @Nested
    @DisplayName("Mapping Result")
    class MappingResult {

        @BeforeEach
        void setupMappingResult() {
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount", "Category"));
            viewModel.setPreviewRows(createSamplePreviewRows());
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedCategoryColumn("Category");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);
        }

        @Test
        @DisplayName("should build column mapping result")
        void shouldBuildColumnMappingResult() {
            ColumnMapping result = viewModel.buildColumnMapping();

            assertThat(result.getDateColumn()).isEqualTo("Date");
            assertThat(result.getDescriptionColumn()).isEqualTo("Description");
            assertThat(result.getAmountColumn()).isEqualTo("Amount");
            assertThat(result.getCategoryColumn()).isEqualTo("Category");
            assertThat(result.getDateFormat()).isEqualTo("dd/MM/yyyy");
        }

        @Test
        @DisplayName("should include amount interpretation in result")
        void shouldIncludeAmountInterpretationInResult() {
            viewModel.setAmountInterpretation(AmountInterpretation.INVERTED);
            ColumnMapping result = viewModel.buildColumnMapping();

            assertThat(result.getAmountInterpretation()).isEqualTo(AmountInterpretation.INVERTED);
        }

        @Test
        @DisplayName("should handle separate columns in result")
        void shouldHandleSeparateColumnsInResult() {
            viewModel.setAmountInterpretation(AmountInterpretation.SEPARATE_COLUMNS);
            viewModel.setSelectedIncomeColumn("Income");
            viewModel.setSelectedExpenseColumn("Expense");

            ColumnMapping result = viewModel.buildColumnMapping();

            assertThat(result.hasSeparateAmountColumns()).isTrue();
            assertThat(result.getIncomeColumn()).isEqualTo("Income");
            assertThat(result.getExpenseColumn()).isEqualTo("Expense");
        }
    }

    @Nested
    @DisplayName("Preview Row Classification")
    class PreviewRowClassification {

        @BeforeEach
        void setupClassification() {
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
            viewModel.setPreviewRows(createSamplePreviewRows());
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
        }

        @Test
        @DisplayName("should classify positive amounts as income with STANDARD interpretation")
        void shouldClassifyPositiveAsIncomeWithStandard() {
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);

            List<ClassifiedPreviewRow> classified = viewModel.getClassifiedPreviewRows();

            // Find a row with positive amount
            ClassifiedPreviewRow positiveRow = classified.stream()
                    .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .orElse(null);

            assertThat(positiveRow).isNotNull();
            assertThat(positiveRow.getClassification()).isEqualTo(TransactionType.INCOME);
        }

        @Test
        @DisplayName("should classify negative amounts as expense with STANDARD interpretation")
        void shouldClassifyNegativeAsExpenseWithStandard() {
            viewModel.setAmountInterpretation(AmountInterpretation.STANDARD);

            List<ClassifiedPreviewRow> classified = viewModel.getClassifiedPreviewRows();

            // Find a row with negative amount
            ClassifiedPreviewRow negativeRow = classified.stream()
                    .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .findFirst()
                    .orElse(null);

            assertThat(negativeRow).isNotNull();
            assertThat(negativeRow.getClassification()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("should classify positive amounts as expense with INVERTED interpretation")
        void shouldClassifyPositiveAsExpenseWithInverted() {
            viewModel.setAmountInterpretation(AmountInterpretation.INVERTED);

            List<ClassifiedPreviewRow> classified = viewModel.getClassifiedPreviewRows();

            // Find a row with positive amount
            ClassifiedPreviewRow positiveRow = classified.stream()
                    .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst()
                    .orElse(null);

            assertThat(positiveRow).isNotNull();
            assertThat(positiveRow.getClassification()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("should classify negative amounts as income with INVERTED interpretation")
        void shouldClassifyNegativeAsIncomeWithInverted() {
            viewModel.setAmountInterpretation(AmountInterpretation.INVERTED);

            List<ClassifiedPreviewRow> classified = viewModel.getClassifiedPreviewRows();

            // Find a row with negative amount
            ClassifiedPreviewRow negativeRow = classified.stream()
                    .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .findFirst()
                    .orElse(null);

            assertThat(negativeRow).isNotNull();
            assertThat(negativeRow.getClassification()).isEqualTo(TransactionType.INCOME);
        }
    }

    @Nested
    @DisplayName("Reset and Clear")
    class ResetAndClear {

        @BeforeEach
        void setupForReset() {
            viewModel.setCsvHeaders(List.of("Date", "Description", "Amount"));
            viewModel.setPreviewRows(createSamplePreviewRows());
            viewModel.setSelectedDateColumn("Date");
            viewModel.setSelectedDescriptionColumn("Description");
            viewModel.setSelectedAmountColumn("Amount");
            viewModel.setSelectedDateFormat("dd/MM/yyyy");
            viewModel.setAmountInterpretation(AmountInterpretation.INVERTED);
            viewModel.goToNextStep();
            viewModel.goToNextStep();
            viewModel.setSavePreferenceSelected(true);
            viewModel.confirmMapping();
        }

        @Test
        @DisplayName("should reset to initial state")
        void shouldResetToInitialState() {
            viewModel.reset();

            assertThat(viewModel.getCurrentStep()).isEqualTo(1);
            assertThat(viewModel.getSelectedDateColumn()).isNull();
            assertThat(viewModel.getSelectedDescriptionColumn()).isNull();
            assertThat(viewModel.getSelectedAmountColumn()).isNull();
            assertThat(viewModel.getAmountInterpretation()).isEqualTo(AmountInterpretation.STANDARD);
            assertThat(viewModel.isMappingConfirmed()).isFalse();
            assertThat(viewModel.isSavePreferenceSelected()).isFalse();
        }

        @Test
        @DisplayName("should preserve CSV headers and preview on reset")
        void shouldPreserveCsvHeadersOnReset() {
            viewModel.reset();

            // Headers should be preserved for re-mapping
            assertThat(viewModel.getCsvHeaders()).isNotEmpty();
            assertThat(viewModel.getPreviewRows()).isNotEmpty();
        }

        @Test
        @DisplayName("should clear all data including headers")
        void shouldClearAllData() {
            viewModel.clearAll();

            assertThat(viewModel.getCsvHeaders()).isEmpty();
            assertThat(viewModel.getPreviewRows()).isEmpty();
            assertThat(viewModel.getSelectedDateColumn()).isNull();
        }
    }

    // === Helper Methods ===

    private List<PreviewRow> createSamplePreviewRows() {
        return List.of(
                new PreviewRow(List.of("15/01/2026", "AMAZON UK", "-45.99")),
                new PreviewRow(List.of("14/01/2026", "PAYPAL TRANSFER", "1250.00")),
                new PreviewRow(List.of("13/01/2026", "TESCO STORES", "-32.50")),
                new PreviewRow(List.of("12/01/2026", "CLIENT A PAYMENT", "2500.00")),
                new PreviewRow(List.of("11/01/2026", "EE MOBILE", "-45.00"))
        );
    }
}
