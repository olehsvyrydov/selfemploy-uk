package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.viewmodel.DashboardViewModel;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DashboardController.
 * Tests the controller logic without JavaFX initialization.
 */
@DisplayName("DashboardController")
class DashboardControllerTest {

    private DashboardController controller;
    private DashboardViewModel viewModel;

    @BeforeEach
    void setUp() {
        controller = new DashboardController();
        viewModel = controller.getViewModel();
    }

    @Test
    @DisplayName("should have a ViewModel")
    void shouldHaveViewModel() {
        assertThat(viewModel).isNotNull();
    }

    @Test
    @DisplayName("should implement TaxYearAware interface")
    void shouldImplementTaxYearAware() {
        assertThat(controller).isInstanceOf(MainController.TaxYearAware.class);
    }

    @Test
    @DisplayName("should update ViewModel when tax year is set")
    void shouldUpdateViewModelWhenTaxYearSet() {
        TaxYear taxYear = TaxYear.of(2024);
        controller.setTaxYear(taxYear);

        assertThat(viewModel.getCurrentTaxYear()).isEqualTo(taxYear);
    }

    @Test
    @DisplayName("should have default metric values of zero")
    void shouldHaveDefaultMetricValuesOfZero() {
        assertThat(viewModel.getTotalIncome()).isEqualTo(BigDecimal.ZERO);
        assertThat(viewModel.getTotalExpenses()).isEqualTo(BigDecimal.ZERO);
        assertThat(viewModel.getNetProfit()).isEqualTo(BigDecimal.ZERO);
        assertThat(viewModel.getEstimatedTax()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should have deadlines populated from tax year")
    void shouldHaveDeadlinesFromTaxYear() {
        // ViewModel initializes with current tax year, so deadlines should exist
        assertThat(viewModel.getDeadlines()).isNotEmpty();
        assertThat(viewModel.getDeadlines()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("should update deadlines when tax year changes")
    void shouldUpdateDeadlinesWhenTaxYearChanges() {
        TaxYear year2024 = TaxYear.of(2024);
        controller.setTaxYear(year2024);

        assertThat(viewModel.getDeadlines()).isNotEmpty();
        // Verify deadlines are for the correct year
        assertThat(viewModel.getDeadlines().get(0).date().getYear())
            .isIn(year2024.onlineFilingDeadline().getYear());
    }

    @Test
    @DisplayName("should have year progress calculated")
    void shouldHaveYearProgressCalculated() {
        // Progress should be between 0 and 1
        double progress = viewModel.getYearProgress();
        assertThat(progress).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("should have days remaining calculated")
    void shouldHaveDaysRemainingCalculated() {
        int daysRemaining = viewModel.getDaysRemaining();
        assertThat(daysRemaining).isBetween(0, 366);
    }

    @Test
    @DisplayName("should have empty recent activity initially")
    void shouldHaveEmptyRecentActivityInitially() {
        assertThat(viewModel.getRecentActivity()).isEmpty();
    }

    @Nested
    @DisplayName("Navigation Callbacks")
    class NavigationCallbackTests {

        @Test
        @DisplayName("should invoke onNavigateToTax callback when handleViewTax is called")
        void shouldInvokeOnNavigateToTaxCallback() {
            // Given
            AtomicBoolean taxNavCalled = new AtomicBoolean(false);
            controller.setNavigationCallbacks(
                () -> {},  // income
                () -> {},  // expenses
                () -> taxNavCalled.set(true)  // tax
            );

            // When
            controller.handleViewTax(new ActionEvent());

            // Then
            assertThat(taxNavCalled.get()).isTrue();
        }

        @Test
        @DisplayName("should not throw when handleViewTax is called without callbacks set")
        void shouldNotThrowWhenHandleViewTaxCalledWithoutCallbacks() {
            // Given - no callbacks set (simulates the bug scenario)

            // When/Then - should not throw, just do nothing
            controller.handleViewTax(new ActionEvent());
            // If we get here without exception, test passes
        }

        @Test
        @DisplayName("should invoke onNavigateToIncome callback when handleAddIncome is called without services")
        void shouldInvokeOnNavigateToIncomeCallback() {
            // Given
            AtomicBoolean incomeNavCalled = new AtomicBoolean(false);
            controller.setNavigationCallbacks(
                () -> incomeNavCalled.set(true),  // income
                () -> {},  // expenses
                () -> {}   // tax
            );

            // When
            controller.handleAddIncome(new ActionEvent());

            // Then
            assertThat(incomeNavCalled.get()).isTrue();
        }

        @Test
        @DisplayName("should invoke onNavigateToExpenses callback when handleAddExpense is called without services")
        void shouldInvokeOnNavigateToExpensesCallback() {
            // Given
            AtomicBoolean expensesNavCalled = new AtomicBoolean(false);
            controller.setNavigationCallbacks(
                () -> {},  // income
                () -> expensesNavCalled.set(true),  // expenses
                () -> {}   // tax
            );

            // When
            controller.handleAddExpense(new ActionEvent());

            // Then
            assertThat(expensesNavCalled.get()).isTrue();
        }

        @Test
        @DisplayName("should invoke onNavigateToIncome callback when handleViewAllActivity is called")
        void shouldInvokeOnNavigateToIncomeForViewAllActivity() {
            // Given
            AtomicBoolean incomeNavCalled = new AtomicBoolean(false);
            controller.setNavigationCallbacks(
                () -> incomeNavCalled.set(true),  // income
                () -> {},  // expenses
                () -> {}   // tax
            );

            // When
            controller.handleViewAllActivity(new ActionEvent());

            // Then
            assertThat(incomeNavCalled.get()).isTrue();
        }
    }
}
