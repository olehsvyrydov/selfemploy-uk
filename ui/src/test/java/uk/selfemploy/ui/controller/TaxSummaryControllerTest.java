package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.TaxSummaryViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TaxSummaryController.
 * Tests the controller logic for the Tax Summary view.
 */
@DisplayName("TaxSummaryController")
class TaxSummaryControllerTest {

    private TaxSummaryController controller;
    private TaxSummaryViewModel viewModel;
    private TaxYear taxYear;

    @Mock
    private IncomeService incomeService;
    @Mock
    private ExpenseService expenseService;

    private UUID businessId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        taxYear = TaxYear.of(2025);
        viewModel = new TaxSummaryViewModel();
        controller = new TaxSummaryController();
        controller.setViewModel(viewModel);
        businessId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Data Loading from Services")
    class DataLoadingFromServices {

        @Test
        @DisplayName("should load income and expenses from services when tax year is set")
        void shouldLoadDataFromServicesWhenTaxYearIsSet() {
            // Given - services with income and expense data
            Income income1 = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("25000.00"),
                "Consulting income",
                IncomeCategory.SALES,
                null,
                null,
                null,
                null
            );
            Income income2 = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 8, 20),
                new BigDecimal("15000.00"),
                "Project work",
                IncomeCategory.SALES,
                null,
                null,
                null,
                null
            );

            Expense expense1 = new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 7, 10),
                new BigDecimal("2000.00"),
                "Office supplies",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null,
                null,
                null,
                null
            );

            when(incomeService.findByTaxYear(businessId, taxYear))
                .thenReturn(List.of(income1, income2));
            when(expenseService.findByTaxYear(businessId, taxYear))
                .thenReturn(List.of(expense1));

            // When - initialize with dependencies and set tax year
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear);

            // Then - viewModel should have the loaded data
            assertThat(viewModel.getTurnover()).isEqualByComparingTo(new BigDecimal("40000.00"));
            assertThat(viewModel.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("2000.00"));
            assertThat(viewModel.getNetProfit()).isEqualByComparingTo(new BigDecimal("38000.00"));
        }

        @Test
        @DisplayName("should calculate tax after loading data")
        void shouldCalculateTaxAfterLoadingData() {
            // Given - income of £50,000
            Income income = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("50000.00"),
                "Annual income",
                IncomeCategory.SALES,
                null,
                null,
                null,
                null
            );

            when(incomeService.findByTaxYear(businessId, taxYear))
                .thenReturn(List.of(income));
            when(expenseService.findByTaxYear(businessId, taxYear))
                .thenReturn(List.of());

            // When
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear);

            // Then - tax should be calculated (not zero)
            assertThat(viewModel.getIncomeTax()).isGreaterThan(BigDecimal.ZERO);
            assertThat(viewModel.getNiClass4()).isGreaterThan(BigDecimal.ZERO);
            assertThat(viewModel.getTotalTax()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should group expenses by category for SA103 breakdown")
        void shouldGroupExpensesByCategory() {
            // Given - multiple expenses in different categories
            Expense officeExpense = new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 7, 10),
                new BigDecimal("500.00"),
                "Office supplies",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null,
                null,
                null,
                null
            );
            Expense travelExpense = new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 8, 15),
                new BigDecimal("300.00"),
                "Train tickets",
                ExpenseCategory.TRAVEL,
                null,
                null,
                null,
                null,
                null
            );
            Expense officeExpense2 = new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 9, 20),
                new BigDecimal("200.00"),
                "Printer ink",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null,
                null,
                null,
                null
            );

            when(incomeService.findByTaxYear(businessId, taxYear))
                .thenReturn(List.of());
            when(expenseService.findByTaxYear(businessId, taxYear))
                .thenReturn(List.of(officeExpense, travelExpense, officeExpense2));

            // When
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
            controller.setTaxYear(taxYear);

            // Then - expenses should be grouped by category
            var breakdown = viewModel.getExpenseBreakdown();
            assertThat(breakdown.get(ExpenseCategory.OFFICE_COSTS)).isEqualByComparingTo(new BigDecimal("700.00"));
            assertThat(breakdown.get(ExpenseCategory.TRAVEL)).isEqualByComparingTo(new BigDecimal("300.00"));
        }
    }

    @Nested
    @DisplayName("Initial State")
    class InitialState {

        @Test
        @DisplayName("should have draft banner visible initially")
        void shouldHaveDraftBannerVisibleInitially() {
            // Given
            viewModel.setSubmitted(false);

            // Then
            assertThat(controller.isDraftBannerVisible()).isTrue();
        }

        @Test
        @DisplayName("should hide draft banner when submitted")
        void shouldHideDraftBannerWhenSubmitted() {
            // Given
            viewModel.setSubmitted(true);

            // Then
            assertThat(controller.isDraftBannerVisible()).isFalse();
        }

        @Test
        @DisplayName("should have submit button disabled")
        void shouldHaveSubmitButtonDisabled() {
            assertThat(controller.isSubmitButtonEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tax Year Display")
    class TaxYearDisplay {

        @Test
        @DisplayName("should display tax year label")
        void shouldDisplayTaxYearLabel() {
            // When
            controller.setTaxYear(taxYear);

            // Then
            assertThat(controller.getTaxYearBadgeText()).isEqualTo("Tax Year 2025/26");
        }
    }

    @Nested
    @DisplayName("Section Expansion")
    class SectionExpansion {

        @Test
        @DisplayName("should have all sections expanded by default")
        void shouldHaveAllSectionsExpandedByDefault() {
            assertThat(controller.isIncomeSectionExpanded()).isTrue();
            assertThat(controller.isExpensesSectionExpanded()).isTrue();
            assertThat(controller.isIncomeTaxSectionExpanded()).isTrue();
            assertThat(controller.isNiSectionExpanded()).isTrue();
        }

        @Test
        @DisplayName("should toggle income section")
        void shouldToggleIncomeSection() {
            // Given - initially expanded
            assertThat(controller.isIncomeSectionExpanded()).isTrue();

            // When
            controller.toggleIncomeSection();

            // Then
            assertThat(controller.isIncomeSectionExpanded()).isFalse();

            // When toggle again
            controller.toggleIncomeSection();

            // Then
            assertThat(controller.isIncomeSectionExpanded()).isTrue();
        }

        @Test
        @DisplayName("should toggle expenses section")
        void shouldToggleExpensesSection() {
            controller.toggleExpensesSection();
            assertThat(controller.isExpensesSectionExpanded()).isFalse();
        }

        @Test
        @DisplayName("should toggle income tax section")
        void shouldToggleIncomeTaxSection() {
            controller.toggleIncomeTaxSection();
            assertThat(controller.isIncomeTaxSectionExpanded()).isFalse();
        }

        @Test
        @DisplayName("should toggle NI section")
        void shouldToggleNISection() {
            controller.toggleNISection();
            assertThat(controller.isNiSectionExpanded()).isFalse();
        }

        @Test
        @DisplayName("should toggle POA section")
        void shouldTogglePOASection() {
            controller.togglePOASection();
            assertThat(controller.isPoaSectionExpanded()).isFalse();
        }
    }

    @Nested
    @DisplayName("Payments on Account Visibility")
    class PaymentsOnAccountVisibility {

        @Test
        @DisplayName("should hide POA section when not required")
        void shouldHidePoaSectionWhenNotRequired() {
            // Given - low income, tax under £1000
            viewModel.setTurnover(new BigDecimal("15000"));
            viewModel.setTaxYear(taxYear);
            viewModel.calculateTax();

            // Then
            assertThat(controller.isPoaSectionVisible()).isFalse();
        }

        @Test
        @DisplayName("should show POA section when required")
        void shouldShowPoaSectionWhenRequired() {
            // Given - high income, tax over £1000
            viewModel.setTurnover(new BigDecimal("50000"));
            viewModel.setTaxYear(taxYear);
            viewModel.calculateTax();

            // Then
            assertThat(controller.isPoaSectionVisible()).isTrue();
        }
    }

    @Nested
    @DisplayName("Toggle Icon Text")
    class ToggleIconText {

        @Test
        @DisplayName("should show [v] when expanded")
        void shouldShowExpandedIconWhenExpanded() {
            assertThat(controller.getIncomeSectionToggleText()).isEqualTo("[v]");
        }

        @Test
        @DisplayName("should show [>] when collapsed")
        void shouldShowCollapsedIconWhenCollapsed() {
            controller.toggleIncomeSection();
            assertThat(controller.getIncomeSectionToggleText()).isEqualTo("[>]");
        }
    }

    @Nested
    @DisplayName("Currency Formatting")
    class CurrencyFormatting {

        @Test
        @DisplayName("should format turnover with currency symbol")
        void shouldFormatTurnoverWithCurrencySymbol() {
            viewModel.setTurnover(new BigDecimal("50000.00"));

            assertThat(controller.getFormattedTurnover()).contains("50,000.00");
        }

        @Test
        @DisplayName("should format expenses as negative for calculation display")
        void shouldFormatExpensesAsNegativeForCalculation() {
            viewModel.setTotalExpenses(new BigDecimal("5000.00"));

            assertThat(controller.getFormattedExpensesForCalculation()).startsWith("-");
        }
    }

    @Nested
    @DisplayName("Expense Line Items")
    class ExpenseLineItems {

        @Test
        @DisplayName("should create line item with SA103 box reference")
        void shouldCreateLineItemWithSa103BoxReference() {
            // Given - COST_OF_GOODS is Box 17 per SA103F
            viewModel.addExpenseByCategory(ExpenseCategory.COST_OF_GOODS, new BigDecimal("500"));

            // When
            var lineItems = controller.getExpenseLineItems();

            // Then
            assertThat(lineItems).hasSize(1);
            assertThat(lineItems.get(0).category()).isEqualTo("Cost of goods bought for resale");
            assertThat(lineItems.get(0).boxRef()).isEqualTo("Box 17");
            assertThat(lineItems.get(0).amount()).isEqualByComparingTo(new BigDecimal("500"));
        }

        @Test
        @DisplayName("should order expense line items by SA103 box number")
        void shouldOrderExpenseLineItemsByBoxNumber() {
            // Given - add in non-sequential order per actual SA103F box numbers
            viewModel.addExpenseByCategory(ExpenseCategory.PROFESSIONAL_FEES, new BigDecimal("100")); // Box 28
            viewModel.addExpenseByCategory(ExpenseCategory.COST_OF_GOODS, new BigDecimal("200"));     // Box 17
            viewModel.addExpenseByCategory(ExpenseCategory.TRAVEL, new BigDecimal("300"));           // Box 20

            // When
            var lineItems = controller.getExpenseLineItems();

            // Then - should be ordered by box number: 17, 20, 28
            assertThat(lineItems).hasSize(3);
            assertThat(lineItems.get(0).boxRef()).isEqualTo("Box 17");
            assertThat(lineItems.get(1).boxRef()).isEqualTo("Box 20");
            assertThat(lineItems.get(2).boxRef()).isEqualTo("Box 28");
        }
    }

    @Nested
    @DisplayName("Income Tax Band Display")
    class IncomeTaxBandDisplay {

        @Test
        @DisplayName("should show all four tax bands")
        void shouldShowAllFourTaxBands() {
            // Given
            viewModel.setTurnover(new BigDecimal("50000"));
            viewModel.setTaxYear(taxYear);
            viewModel.calculateTax();

            // When
            var bands = controller.getIncomeTaxBands();

            // Then
            assertThat(bands).hasSize(4);
            assertThat(bands.get(0).description()).contains("Personal Allowance");
            assertThat(bands.get(1).description()).contains("Basic Rate");
            assertThat(bands.get(2).description()).contains("Higher Rate");
            assertThat(bands.get(3).description()).contains("Additional Rate");
        }
    }

    @Nested
    @DisplayName("NI Class 4 Band Display")
    class NiClass4BandDisplay {

        @Test
        @DisplayName("should show NI rate bands with correct 2025/26 rates")
        void shouldShowNiRateBandsWithCorrectRates() {
            // Given
            viewModel.setTurnover(new BigDecimal("50000"));
            viewModel.setTaxYear(taxYear);
            viewModel.calculateTax();

            // When
            var bands = controller.getNiClass4Bands();

            // Then - 2025/26 rates: 6% main rate, 2% additional rate
            assertThat(bands).hasSize(3);
            assertThat(bands.get(0).description()).contains("0%");
            assertThat(bands.get(1).description()).contains("6%"); // Corrected from 9%
            assertThat(bands.get(2).description()).contains("2%");
        }
    }

    @Nested
    @DisplayName("Due Date Display")
    class DueDateDisplay {

        @Test
        @DisplayName("should display due date")
        void shouldDisplayDueDate() {
            // Given - set tax year on controller (which also sets on viewModel)
            controller.setTaxYear(taxYear);

            // When
            String dueDate = controller.getDueDateText();

            // Then
            assertThat(dueDate).isEqualTo("Due by 31 January 2027");
        }
    }

    @Nested
    @DisplayName("Grand Total Calculation")
    class GrandTotalCalculation {

        @Test
        @DisplayName("should calculate grand total without POA")
        void shouldCalculateGrandTotalWithoutPoa() {
            // Given - low income, no POA
            viewModel.setTurnover(new BigDecimal("15000"));
            viewModel.setTaxYear(taxYear);
            viewModel.calculateTax();

            // When
            BigDecimal grandTotal = controller.getGrandTotal();

            // Then - should equal total tax only
            assertThat(grandTotal).isEqualByComparingTo(viewModel.getTotalTax());
        }

        @Test
        @DisplayName("should calculate grand total with POA")
        void shouldCalculateGrandTotalWithPoa() {
            // Given - high income, POA required
            viewModel.setTurnover(new BigDecimal("50000"));
            viewModel.setTaxYear(taxYear);
            viewModel.calculateTax();

            // When
            BigDecimal grandTotal = controller.getGrandTotal();

            // Then - should equal total tax + POA amount
            BigDecimal expected = viewModel.getTotalTax().add(viewModel.getPaymentOnAccountAmount());
            assertThat(grandTotal).isEqualByComparingTo(expected);
        }
    }
}
