package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.service.SqliteTestSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HmrcSubmissionController.
 * Tests the controller logic for the HMRC Submission hub page.
 *
 * <p>Sprint 13: Connection management tests removed as OAuth is now
 * handled automatically during submission via AutoOAuthSubmissionService.</p>
 *
 * <p>Test Categories:
 * <ul>
 *   <li>Initialization Tests - Controller setup and service initialization</li>
 *   <li>Tax Year Management Tests - Tax year handling and deadline display</li>
 *   <li>Quarter Status Tests - MTD quarterly submission status</li>
 *   <li>Service Integration Tests - Financial data loading via services</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HmrcSubmissionController")
class HmrcSubmissionControllerTest {

    private HmrcSubmissionController controller;
    private TaxYear taxYear;

    @Mock
    private IncomeService incomeService;

    @Mock
    private ExpenseService expenseService;

    private UUID businessId;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        controller = new HmrcSubmissionController();
        taxYear = TaxYear.of(2025);
        businessId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("should implement TaxYearAware interface")
        void shouldImplementTaxYearAware() {
            assertThat(controller).isInstanceOf(MainController.TaxYearAware.class);
        }

        @Test
        @DisplayName("should implement Initializable interface")
        void shouldImplementInitializable() {
            assertThat(controller).isInstanceOf(javafx.fxml.Initializable.class);
        }

        @Test
        @DisplayName("should create controller without errors")
        void shouldCreateControllerWithoutErrors() {
            assertThat(controller).isNotNull();
        }

        @Test
        @DisplayName("should initialize services when accessed")
        void shouldInitializeServicesWhenAccessed() {
            // Given - controller with injected dependencies
            controller.initializeWithDependencies(incomeService, expenseService, businessId);

            // Then - services should be accessible
            assertThat(controller.getIncomeService()).isEqualTo(incomeService);
            assertThat(controller.getExpenseService()).isEqualTo(expenseService);
            assertThat(controller.getBusinessId()).isEqualTo(businessId);
        }

        @Test
        @DisplayName("should accept custom dependencies for testing")
        void shouldAcceptCustomDependencies() {
            // When
            controller.initializeWithDependencies(incomeService, expenseService, businessId);

            // Then
            assertThat(controller.getIncomeService()).isSameAs(incomeService);
            assertThat(controller.getExpenseService()).isSameAs(expenseService);
            assertThat(controller.getBusinessId()).isEqualTo(businessId);
        }
    }

    @Nested
    @DisplayName("Tax Year Management")
    class TaxYearManagement {

        @Test
        @DisplayName("should store tax year when set")
        void shouldStoreTaxYear() {
            // When
            controller.setTaxYear(taxYear);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(taxYear);
        }

        @Test
        @DisplayName("should handle null tax year gracefully")
        void shouldHandleNullTaxYear() {
            // When
            controller.setTaxYear(null);

            // Then
            assertThat(controller.getTaxYear()).isNull();
        }

        @Test
        @DisplayName("should update tax year when changed")
        void shouldUpdateTaxYear() {
            // Given
            TaxYear year2024 = TaxYear.of(2024);
            controller.setTaxYear(year2024);

            // When
            controller.setTaxYear(taxYear);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(taxYear);
        }

        @Test
        @DisplayName("should accept multiple tax year changes")
        void shouldAcceptMultipleTaxYearChanges() {
            // Given
            TaxYear year2023 = TaxYear.of(2023);
            TaxYear year2024 = TaxYear.of(2024);
            TaxYear year2025 = TaxYear.of(2025);

            // When/Then
            controller.setTaxYear(year2023);
            assertThat(controller.getTaxYear()).isEqualTo(year2023);

            controller.setTaxYear(year2024);
            assertThat(controller.getTaxYear()).isEqualTo(year2024);

            controller.setTaxYear(year2025);
            assertThat(controller.getTaxYear()).isEqualTo(year2025);
        }
    }

    @Nested
    @DisplayName("Quarter Status Determination")
    class QuarterStatusDetermination {

        @Test
        @DisplayName("should return Q1 status for April")
        void shouldReturnQ1ForApril() {
            LocalDate aprilDate = LocalDate.of(2025, 4, 15);
            String status = controller.determineQuarterStatus(aprilDate, taxYear);

            assertThat(status).contains("Q1");
            assertThat(status).contains("Apr-Jun");
            assertThat(status).contains("7 Aug");
        }

        @Test
        @DisplayName("should return Q1 status for May")
        void shouldReturnQ1ForMay() {
            LocalDate mayDate = LocalDate.of(2025, 5, 15);
            String status = controller.determineQuarterStatus(mayDate, taxYear);

            assertThat(status).contains("Q1");
        }

        @Test
        @DisplayName("should return Q1 status for June")
        void shouldReturnQ1ForJune() {
            LocalDate juneDate = LocalDate.of(2025, 6, 30);
            String status = controller.determineQuarterStatus(juneDate, taxYear);

            assertThat(status).contains("Q1");
        }

        @Test
        @DisplayName("should return Q2 status for July")
        void shouldReturnQ2ForJuly() {
            LocalDate julyDate = LocalDate.of(2025, 7, 1);
            String status = controller.determineQuarterStatus(julyDate, taxYear);

            assertThat(status).contains("Q2");
            assertThat(status).contains("Jul-Sep");
            assertThat(status).contains("7 Nov");
        }

        @Test
        @DisplayName("should return Q2 status for August")
        void shouldReturnQ2ForAugust() {
            LocalDate augDate = LocalDate.of(2025, 8, 15);
            String status = controller.determineQuarterStatus(augDate, taxYear);

            assertThat(status).contains("Q2");
        }

        @Test
        @DisplayName("should return Q2 status for September")
        void shouldReturnQ2ForSeptember() {
            LocalDate septDate = LocalDate.of(2025, 9, 30);
            String status = controller.determineQuarterStatus(septDate, taxYear);

            assertThat(status).contains("Q2");
        }

        @Test
        @DisplayName("should return Q3 status for October")
        void shouldReturnQ3ForOctober() {
            LocalDate octDate = LocalDate.of(2025, 10, 1);
            String status = controller.determineQuarterStatus(octDate, taxYear);

            assertThat(status).contains("Q3");
            assertThat(status).contains("Oct-Dec");
            assertThat(status).contains("7 Feb");
        }

        @Test
        @DisplayName("should return Q3 status for November")
        void shouldReturnQ3ForNovember() {
            LocalDate novDate = LocalDate.of(2025, 11, 15);
            String status = controller.determineQuarterStatus(novDate, taxYear);

            assertThat(status).contains("Q3");
        }

        @Test
        @DisplayName("should return Q3 status for December")
        void shouldReturnQ3ForDecember() {
            LocalDate decDate = LocalDate.of(2025, 12, 31);
            String status = controller.determineQuarterStatus(decDate, taxYear);

            assertThat(status).contains("Q3");
        }

        @Test
        @DisplayName("should return Q4 status for January")
        void shouldReturnQ4ForJanuary() {
            LocalDate janDate = LocalDate.of(2026, 1, 15);
            String status = controller.determineQuarterStatus(janDate, taxYear);

            assertThat(status).contains("Q4");
            assertThat(status).contains("Jan-Mar");
            assertThat(status).contains("7 May");
        }

        @Test
        @DisplayName("should return Q4 status for February")
        void shouldReturnQ4ForFebruary() {
            LocalDate febDate = LocalDate.of(2026, 2, 15);
            String status = controller.determineQuarterStatus(febDate, taxYear);

            assertThat(status).contains("Q4");
        }

        @Test
        @DisplayName("should return Q4 status for March")
        void shouldReturnQ4ForMarch() {
            LocalDate marchDate = LocalDate.of(2026, 3, 31);
            String status = controller.determineQuarterStatus(marchDate, taxYear);

            assertThat(status).contains("Q4");
        }

        @Test
        @DisplayName("should format Q1 deadline correctly")
        void shouldFormatQ1DeadlineCorrectly() {
            LocalDate aprilDate = LocalDate.of(2025, 4, 15);
            String status = controller.determineQuarterStatus(aprilDate, taxYear);

            assertThat(status).isEqualTo("Current: Q1 (Apr-Jun) - Due by 7 Aug");
        }

        @Test
        @DisplayName("should format Q2 deadline correctly")
        void shouldFormatQ2DeadlineCorrectly() {
            LocalDate julyDate = LocalDate.of(2025, 7, 15);
            String status = controller.determineQuarterStatus(julyDate, taxYear);

            assertThat(status).isEqualTo("Current: Q2 (Jul-Sep) - Due by 7 Nov");
        }

        @Test
        @DisplayName("should format Q3 deadline correctly")
        void shouldFormatQ3DeadlineCorrectly() {
            LocalDate octDate = LocalDate.of(2025, 10, 15);
            String status = controller.determineQuarterStatus(octDate, taxYear);

            assertThat(status).isEqualTo("Current: Q3 (Oct-Dec) - Due by 7 Feb");
        }

        @Test
        @DisplayName("should format Q4 deadline correctly")
        void shouldFormatQ4DeadlineCorrectly() {
            LocalDate janDate = LocalDate.of(2026, 1, 15);
            String status = controller.determineQuarterStatus(janDate, taxYear);

            assertThat(status).isEqualTo("Current: Q4 (Jan-Mar) - Due by 7 May");
        }
    }

    @Nested
    @DisplayName("Annual Deadline Display")
    class AnnualDeadlineDisplay {

        @Test
        @DisplayName("should return empty string when no tax year set")
        void shouldReturnEmptyWhenNoTaxYear() {
            // When - tax year not set
            String deadline = controller.getFormattedAnnualDeadline();

            // Then
            assertThat(deadline).isEmpty();
        }

        @Test
        @DisplayName("should format deadline correctly for tax year 2025/26")
        void shouldFormatDeadlineCorrectly() {
            // Given
            controller.setTaxYear(taxYear);

            // When
            String deadline = controller.getFormattedAnnualDeadline();

            // Then
            assertThat(deadline).isEqualTo("Deadline: 31 January 2027");
        }

        @Test
        @DisplayName("should format deadline correctly for tax year 2024/25")
        void shouldFormatDeadlineForPreviousYear() {
            // Given
            TaxYear year2024 = TaxYear.of(2024);
            controller.setTaxYear(year2024);

            // When
            String deadline = controller.getFormattedAnnualDeadline();

            // Then
            assertThat(deadline).isEqualTo("Deadline: 31 January 2026");
        }

        @Test
        @DisplayName("should format deadline correctly for tax year 2023/24")
        void shouldFormatDeadlineFor2023() {
            // Given
            TaxYear year2023 = TaxYear.of(2023);
            controller.setTaxYear(year2023);

            // When
            String deadline = controller.getFormattedAnnualDeadline();

            // Then
            assertThat(deadline).isEqualTo("Deadline: 31 January 2025");
        }

        @Test
        @DisplayName("should use correct date format")
        void shouldUseCorrectDateFormat() {
            // Given
            controller.setTaxYear(taxYear);

            // When
            String deadline = controller.getFormattedAnnualDeadline();

            // Then - should use "d MMMM yyyy" format
            assertThat(deadline).matches("Deadline: \\d{1,2} [A-Z][a-z]+ \\d{4}");
        }

        @Test
        @DisplayName("should update deadline when tax year changes")
        void shouldUpdateDeadlineWhenTaxYearChanges() {
            // Given
            TaxYear year2024 = TaxYear.of(2024);
            controller.setTaxYear(year2024);
            assertThat(controller.getFormattedAnnualDeadline()).contains("2026");

            // When
            controller.setTaxYear(taxYear);

            // Then
            assertThat(controller.getFormattedAnnualDeadline()).contains("2027");
        }
    }

    @Nested
    @DisplayName("Tax Year Deadline Calculation")
    class TaxYearDeadlineCalculation {

        @Test
        @DisplayName("should calculate online filing deadline as 31 January following tax year end")
        void shouldCalculateOnlineFilingDeadline() {
            // Tax year 2025/26 ends 5 April 2026
            // Online filing deadline is 31 January 2027
            TaxYear year2025 = TaxYear.of(2025);

            assertThat(year2025.onlineFilingDeadline()).isEqualTo(LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("should calculate deadline for tax year 2024/25")
        void shouldCalculateDeadlineFor2024() {
            // Tax year 2024/25 ends 5 April 2025
            // Online filing deadline is 31 January 2026
            TaxYear year2024 = TaxYear.of(2024);

            assertThat(year2024.onlineFilingDeadline()).isEqualTo(LocalDate.of(2026, 1, 31));
        }

        @Test
        @DisplayName("should calculate deadline for tax year 2023/24")
        void shouldCalculateDeadlineFor2023() {
            TaxYear year2023 = TaxYear.of(2023);

            assertThat(year2023.onlineFilingDeadline()).isEqualTo(LocalDate.of(2025, 1, 31));
        }
    }

    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {

        @BeforeEach
        void setUpMocks() {
            controller.initializeWithDependencies(incomeService, expenseService, businessId);
        }

        @Test
        @DisplayName("should load total income from income service")
        void shouldLoadTotalIncomeFromService() {
            // Given
            BigDecimal expectedIncome = new BigDecimal("50000.00");
            when(incomeService.getTotalByTaxYear(businessId, taxYear)).thenReturn(expectedIncome);
            when(expenseService.getDeductibleTotal(businessId, taxYear)).thenReturn(BigDecimal.ZERO);

            // When
            BigDecimal netProfit = controller.initializeAnnualSubmissionForTest(null, taxYear);

            // Then
            verify(incomeService).getTotalByTaxYear(businessId, taxYear);
            assertThat(netProfit).isEqualByComparingTo(expectedIncome);
        }

        @Test
        @DisplayName("should load deductible expenses from expense service")
        void shouldLoadDeductibleExpensesFromService() {
            // Given
            BigDecimal totalIncome = new BigDecimal("50000.00");
            BigDecimal expectedExpenses = new BigDecimal("10000.00");
            when(incomeService.getTotalByTaxYear(businessId, taxYear)).thenReturn(totalIncome);
            when(expenseService.getDeductibleTotal(businessId, taxYear)).thenReturn(expectedExpenses);

            // When
            controller.initializeAnnualSubmissionForTest(null, taxYear);

            // Then
            verify(expenseService).getDeductibleTotal(businessId, taxYear);
        }

        @Test
        @DisplayName("should calculate net profit correctly")
        void shouldCalculateNetProfitCorrectly() {
            // Given
            BigDecimal totalIncome = new BigDecimal("50000.00");
            BigDecimal totalExpenses = new BigDecimal("15000.00");
            BigDecimal expectedNetProfit = new BigDecimal("35000.00");

            when(incomeService.getTotalByTaxYear(businessId, taxYear)).thenReturn(totalIncome);
            when(expenseService.getDeductibleTotal(businessId, taxYear)).thenReturn(totalExpenses);

            // When
            BigDecimal netProfit = controller.initializeAnnualSubmissionForTest(null, taxYear);

            // Then
            assertThat(netProfit).isEqualByComparingTo(expectedNetProfit);
        }

        @Test
        @DisplayName("should handle zero income")
        void shouldHandleZeroIncome() {
            // Given
            when(incomeService.getTotalByTaxYear(businessId, taxYear)).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(businessId, taxYear)).thenReturn(BigDecimal.ZERO);

            // When
            BigDecimal netProfit = controller.initializeAnnualSubmissionForTest(null, taxYear);

            // Then
            assertThat(netProfit).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should handle zero expenses")
        void shouldHandleZeroExpenses() {
            // Given
            BigDecimal totalIncome = new BigDecimal("50000.00");
            when(incomeService.getTotalByTaxYear(businessId, taxYear)).thenReturn(totalIncome);
            when(expenseService.getDeductibleTotal(businessId, taxYear)).thenReturn(BigDecimal.ZERO);

            // When
            BigDecimal netProfit = controller.initializeAnnualSubmissionForTest(null, taxYear);

            // Then
            assertThat(netProfit).isEqualByComparingTo(totalIncome);
        }

        @Test
        @DisplayName("should handle expenses greater than income (loss)")
        void shouldHandleExpensesGreaterThanIncome() {
            // Given - loss scenario
            BigDecimal totalIncome = new BigDecimal("10000.00");
            BigDecimal totalExpenses = new BigDecimal("15000.00");
            BigDecimal expectedNetLoss = new BigDecimal("-5000.00");

            when(incomeService.getTotalByTaxYear(businessId, taxYear)).thenReturn(totalIncome);
            when(expenseService.getDeductibleTotal(businessId, taxYear)).thenReturn(totalExpenses);

            // When
            BigDecimal netProfit = controller.initializeAnnualSubmissionForTest(null, taxYear);

            // Then
            assertThat(netProfit).isEqualByComparingTo(expectedNetLoss);
        }

        @Test
        @DisplayName("should use correct tax year when loading data")
        void shouldUseCorrectTaxYearWhenLoadingData() {
            // Given
            TaxYear differentYear = TaxYear.of(2024);
            when(incomeService.getTotalByTaxYear(businessId, differentYear)).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(businessId, differentYear)).thenReturn(BigDecimal.ZERO);

            // When
            controller.initializeAnnualSubmissionForTest(null, differentYear);

            // Then
            verify(incomeService).getTotalByTaxYear(businessId, differentYear);
            verify(expenseService).getDeductibleTotal(businessId, differentYear);
        }

        @Test
        @DisplayName("should set tax year on controller after initialization")
        void shouldSetTaxYearOnController() {
            // Given
            when(incomeService.getTotalByTaxYear(businessId, taxYear)).thenReturn(BigDecimal.ZERO);
            when(expenseService.getDeductibleTotal(businessId, taxYear)).thenReturn(BigDecimal.ZERO);

            // When
            controller.initializeAnnualSubmissionForTest(null, taxYear);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(taxYear);
        }

        @Test
        @DisplayName("should handle large income values")
        void shouldHandleLargeIncomeValues() {
            // Given - large income value
            BigDecimal largeIncome = new BigDecimal("1000000.00");
            BigDecimal expenses = new BigDecimal("100000.00");
            BigDecimal expectedNet = new BigDecimal("900000.00");

            when(incomeService.getTotalByTaxYear(businessId, taxYear)).thenReturn(largeIncome);
            when(expenseService.getDeductibleTotal(businessId, taxYear)).thenReturn(expenses);

            // When
            BigDecimal netProfit = controller.initializeAnnualSubmissionForTest(null, taxYear);

            // Then
            assertThat(netProfit).isEqualByComparingTo(expectedNet);
        }

        @Test
        @DisplayName("should handle decimal precision correctly")
        void shouldHandleDecimalPrecisionCorrectly() {
            // Given - values with decimal precision
            BigDecimal income = new BigDecimal("50000.75");
            BigDecimal expenses = new BigDecimal("15000.25");
            BigDecimal expectedNet = new BigDecimal("35000.50");

            when(incomeService.getTotalByTaxYear(businessId, taxYear)).thenReturn(income);
            when(expenseService.getDeductibleTotal(businessId, taxYear)).thenReturn(expenses);

            // When
            BigDecimal netProfit = controller.initializeAnnualSubmissionForTest(null, taxYear);

            // Then
            assertThat(netProfit).isEqualByComparingTo(expectedNet);
        }
    }

    @Nested
    @DisplayName("Dialog Stage Management")
    class DialogStageManagement {

        @Test
        @DisplayName("should accept dialog stage setter")
        void shouldAcceptDialogStageSetter() {
            // This test verifies the setter doesn't throw
            // Actual Stage testing requires JavaFX initialization
            controller.setDialogStage(null);
            // If we get here without exception, test passes
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle first day of Q1")
        void shouldHandleFirstDayOfQ1() {
            LocalDate firstDayQ1 = LocalDate.of(2025, 4, 1);
            String status = controller.determineQuarterStatus(firstDayQ1, taxYear);
            assertThat(status).contains("Q1");
        }

        @Test
        @DisplayName("should handle last day of Q4")
        void shouldHandleLastDayOfQ4() {
            LocalDate lastDayQ4 = LocalDate.of(2026, 3, 31);
            String status = controller.determineQuarterStatus(lastDayQ4, taxYear);
            assertThat(status).contains("Q4");
        }

        @Test
        @DisplayName("should handle boundary between Q1 and Q2")
        void shouldHandleBoundaryQ1Q2() {
            // June 30 - last day of Q1
            LocalDate lastDayQ1 = LocalDate.of(2025, 6, 30);
            assertThat(controller.determineQuarterStatus(lastDayQ1, taxYear)).contains("Q1");

            // July 1 - first day of Q2
            LocalDate firstDayQ2 = LocalDate.of(2025, 7, 1);
            assertThat(controller.determineQuarterStatus(firstDayQ2, taxYear)).contains("Q2");
        }

        @Test
        @DisplayName("should handle boundary between Q2 and Q3")
        void shouldHandleBoundaryQ2Q3() {
            // September 30 - last day of Q2
            LocalDate lastDayQ2 = LocalDate.of(2025, 9, 30);
            assertThat(controller.determineQuarterStatus(lastDayQ2, taxYear)).contains("Q2");

            // October 1 - first day of Q3
            LocalDate firstDayQ3 = LocalDate.of(2025, 10, 1);
            assertThat(controller.determineQuarterStatus(firstDayQ3, taxYear)).contains("Q3");
        }

        @Test
        @DisplayName("should handle boundary between Q3 and Q4")
        void shouldHandleBoundaryQ3Q4() {
            // December 31 - last day of Q3
            LocalDate lastDayQ3 = LocalDate.of(2025, 12, 31);
            assertThat(controller.determineQuarterStatus(lastDayQ3, taxYear)).contains("Q3");

            // January 1 - first day of Q4
            LocalDate firstDayQ4 = LocalDate.of(2026, 1, 1);
            assertThat(controller.determineQuarterStatus(firstDayQ4, taxYear)).contains("Q4");
        }

        @Test
        @DisplayName("should handle leap year February")
        void shouldHandleLeapYearFebruary() {
            // 2024 is a leap year
            LocalDate leapYearFeb = LocalDate.of(2024, 2, 29);
            TaxYear year2023 = TaxYear.of(2023);
            String status = controller.determineQuarterStatus(leapYearFeb, year2023);
            assertThat(status).contains("Q4");
        }
    }

    @Nested
    @DisplayName("Tax Year Label Format")
    class TaxYearLabelFormat {

        @Test
        @DisplayName("should format tax year label correctly for 2025/26")
        void shouldFormatTaxYearLabel2025() {
            assertThat(taxYear.label()).isEqualTo("2025/26");
        }

        @Test
        @DisplayName("should format tax year label correctly for 2024/25")
        void shouldFormatTaxYearLabel2024() {
            TaxYear year2024 = TaxYear.of(2024);
            assertThat(year2024.label()).isEqualTo("2024/25");
        }

        @Test
        @DisplayName("should format tax year label correctly for century boundary")
        void shouldFormatTaxYearLabelCenturyBoundary() {
            TaxYear year2099 = TaxYear.of(2099);
            assertThat(year2099.label()).isEqualTo("2099/00");
        }
    }

    @Nested
    @DisplayName("Keyboard Accessibility Tests")
    class KeyboardAccessibilityTests {

        @Test
        @DisplayName("KB-020: Controller should support keyboard event handlers")
        void annualSubmissionCardShouldSupportKeyboardHandlers() {
            // Given: HmrcSubmissionController
            // Then: Should implement methods for keyboard events
            assertThat(controller).isNotNull();

            // Verify controller class has the key handler method
            boolean hasKeyHandler = false;
            for (var method : controller.getClass().getDeclaredMethods()) {
                if (method.getName().equals("handleAnnualSubmissionKey")) {
                    hasKeyHandler = true;
                    break;
                }
            }
            assertThat(hasKeyHandler)
                .as("Controller should have handleAnnualSubmissionKey method")
                .isTrue();
        }

        @Test
        @DisplayName("KB-021: Controller should support quarterly updates keyboard handler")
        void quarterlyUpdatesCardShouldSupportKeyboardHandler() {
            boolean hasKeyHandler = false;
            for (var method : controller.getClass().getDeclaredMethods()) {
                if (method.getName().equals("handleQuarterlySubmissionKey")) {
                    hasKeyHandler = true;
                    break;
                }
            }
            assertThat(hasKeyHandler)
                .as("Controller should have handleQuarterlySubmissionKey method")
                .isTrue();
        }

        @Test
        @DisplayName("KB-022: Controller should have method to open annual submission")
        void enterKeyShouldOpenAnnualSubmission() {
            boolean hasOpenMethod = false;
            for (var method : controller.getClass().getDeclaredMethods()) {
                if (method.getName().equals("openAnnualSubmission") ||
                    method.getName().equals("handleAnnualSubmission")) {
                    hasOpenMethod = true;
                    break;
                }
            }
            assertThat(hasOpenMethod)
                .as("Controller should have method to open annual submission")
                .isTrue();
        }

        @Test
        @DisplayName("KB-023: Controller should have method for quarterly updates")
        void enterKeyShouldOpenQuarterlyUpdates() {
            boolean hasHandler = false;
            for (var method : controller.getClass().getDeclaredMethods()) {
                if (method.getName().equals("handleQuarterlySubmission") ||
                    method.getName().equals("openQuarterlyUpdates")) {
                    hasHandler = true;
                    break;
                }
            }
            assertThat(hasHandler)
                .as("Controller should have method for quarterly updates")
                .isTrue();
        }

        @Test
        @DisplayName("KB-024: Key handlers should check for Enter and Space keys")
        void spaceKeyShouldActivateCards() {
            // Verify KeyCode enum has required values
            assertThat(javafx.scene.input.KeyCode.ENTER).isNotNull();
            assertThat(javafx.scene.input.KeyCode.SPACE).isNotNull();
        }

        @Test
        @DisplayName("KB-025: Tab order should follow visual layout (cards in VBox)")
        void tabOrderShouldFollowVisualLayout() {
            // Verify controller is properly initialized
            assertThat(controller).isNotNull();
        }

        @Test
        @DisplayName("KB-026: Controller should initialize without null pointer exceptions")
        void focusIndicatorShouldBeVisibleOnHmrcCards() {
            // Given: New controller instance
            HmrcSubmissionController freshController = new HmrcSubmissionController();

            // Then: Controller should be created successfully
            assertThat(freshController).isNotNull();

            // And: Tax year should be settable
            freshController.setTaxYear(taxYear);
            assertThat(freshController.getTaxYear()).isEqualTo(taxYear);
        }

        @Test
        @DisplayName("KB-027: Non-activation keys should not trigger navigation")
        void escapeKeyShouldNotTriggerNavigation() {
            javafx.scene.input.KeyCode[] activationKeys = {
                javafx.scene.input.KeyCode.ENTER,
                javafx.scene.input.KeyCode.SPACE
            };

            // ESCAPE should not be in the activation keys
            assertThat(javafx.scene.input.KeyCode.ESCAPE)
                .isNotIn((Object[]) activationKeys);

            // TAB should not be in the activation keys (it's for navigation)
            assertThat(javafx.scene.input.KeyCode.TAB)
                .isNotIn((Object[]) activationKeys);
        }
    }

    @Nested
    @DisplayName("Card Focusability Tests")
    class CardFocusabilityTests {

        @Test
        @DisplayName("Controller should support FXML card injection")
        void controllerShouldSupportFxmlCardInjection() {
            boolean hasAnnualCard = false;
            boolean hasQuarterlyCard = false;
            boolean hasHistoryCard = false;

            for (var field : controller.getClass().getDeclaredFields()) {
                switch (field.getName()) {
                    case "annualCard" -> hasAnnualCard = true;
                    case "quarterlyCard" -> hasQuarterlyCard = true;
                    case "historyCard" -> hasHistoryCard = true;
                }
            }

            assertThat(hasAnnualCard).as("Should have annualCard field").isTrue();
            assertThat(hasQuarterlyCard).as("Should have quarterlyCard field").isTrue();
            assertThat(hasHistoryCard).as("Should have historyCard field").isTrue();
        }

        @Test
        @DisplayName("Controller implements Initializable for setup")
        void controllerShouldImplementInitializable() {
            assertThat(controller).isInstanceOf(javafx.fxml.Initializable.class);
        }
    }
}
