package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for NavigationViewModel.
 * Tests navigation state and tax year selection logic.
 */
@DisplayName("NavigationViewModel")
class NavigationViewModelTest {

    private NavigationViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new NavigationViewModel();
    }

    @Nested
    @DisplayName("Navigation State")
    class NavigationState {

        @Test
        @DisplayName("should default to Dashboard view")
        void shouldDefaultToDashboard() {
            assertThat(viewModel.getCurrentView()).isEqualTo(View.DASHBOARD);
        }

        @Test
        @DisplayName("should update current view on navigation")
        void shouldUpdateCurrentView() {
            viewModel.navigateTo(View.INCOME);
            assertThat(viewModel.getCurrentView()).isEqualTo(View.INCOME);

            viewModel.navigateTo(View.EXPENSES);
            assertThat(viewModel.getCurrentView()).isEqualTo(View.EXPENSES);
        }

        @Test
        @DisplayName("should track navigation history")
        void shouldTrackNavigationHistory() {
            viewModel.navigateTo(View.INCOME);
            viewModel.navigateTo(View.EXPENSES);
            viewModel.navigateTo(View.TAX_SUMMARY);

            assertThat(viewModel.canGoBack()).isTrue();
        }

        @Test
        @DisplayName("should go back to previous view")
        void shouldGoBackToPreviousView() {
            viewModel.navigateTo(View.INCOME);
            viewModel.navigateTo(View.EXPENSES);

            viewModel.goBack();

            assertThat(viewModel.getCurrentView()).isEqualTo(View.INCOME);
        }

        @Test
        @DisplayName("should not go back when at start")
        void shouldNotGoBackWhenAtStart() {
            assertThat(viewModel.canGoBack()).isFalse();

            viewModel.goBack(); // Should not throw

            assertThat(viewModel.getCurrentView()).isEqualTo(View.DASHBOARD);
        }
    }

    @Nested
    @DisplayName("Tax Year Selection")
    class TaxYearSelection {

        @Test
        @DisplayName("should initialize with current tax year")
        void shouldInitializeWithCurrentTaxYear() {
            TaxYear current = TaxYear.current();
            assertThat(viewModel.getSelectedTaxYear().startYear())
                .isEqualTo(current.startYear());
        }

        @Test
        @DisplayName("should provide available tax years")
        void shouldProvideAvailableTaxYears() {
            var years = viewModel.getAvailableTaxYears();

            // Should include current and 2 previous years
            assertThat(years).hasSize(3);
        }

        @Test
        @DisplayName("should include current year in available years")
        void shouldIncludeCurrentYearInAvailableYears() {
            TaxYear current = TaxYear.current();
            var years = viewModel.getAvailableTaxYears();

            assertThat(years).anyMatch(y -> y.startYear() == current.startYear());
        }

        @Test
        @DisplayName("should update selected tax year")
        void shouldUpdateSelectedTaxYear() {
            TaxYear previousYear = TaxYear.current().previous();

            viewModel.setSelectedTaxYear(previousYear);

            assertThat(viewModel.getSelectedTaxYear().startYear())
                .isEqualTo(previousYear.startYear());
        }
    }

    @Nested
    @DisplayName("Status Bar")
    class StatusBar {

        @Test
        @DisplayName("should format tax year label")
        void shouldFormatTaxYearLabel() {
            TaxYear year = TaxYear.of(2025);
            viewModel.setSelectedTaxYear(year);

            assertThat(viewModel.getTaxYearLabel()).isEqualTo("Tax Year 2025/26");
        }

        @Test
        @DisplayName("should calculate days until deadline")
        void shouldCalculateDaysUntilDeadline() {
            TaxYear year = TaxYear.of(2025);
            viewModel.setSelectedTaxYear(year);

            long expectedDays = ChronoUnit.DAYS.between(LocalDate.now(), year.onlineFilingDeadline());

            assertThat(viewModel.getDaysUntilDeadline()).isEqualTo(expectedDays);
        }

        @Test
        @DisplayName("should format deadline countdown")
        void shouldFormatDeadlineCountdown() {
            TaxYear year = TaxYear.of(2025);
            viewModel.setSelectedTaxYear(year);

            String countdown = viewModel.getDeadlineCountdown();

            assertThat(countdown).matches("\\d+ days until filing deadline");
        }
    }

    @Nested
    @DisplayName("View Enumeration")
    class ViewEnumeration {

        @Test
        @DisplayName("should have all required views")
        void shouldHaveAllRequiredViews() {
            assertThat(View.values()).containsExactly(
                View.DASHBOARD,
                View.INCOME,
                View.EXPENSES,
                View.TRANSACTION_REVIEW,
                View.TAX_SUMMARY,
                View.HMRC_SUBMISSION,
                View.SETTINGS,
                View.HELP
            );
        }

        @Test
        @DisplayName("should provide FXML path for each view")
        void shouldProvideFxmlPathForEachView() {
            for (View view : View.values()) {
                assertThat(view.getFxmlPath()).isNotBlank();
                assertThat(view.getFxmlPath()).endsWith(".fxml");
            }
        }

        @Test
        @DisplayName("should provide title for each view")
        void shouldProvideTitleForEachView() {
            assertThat(View.DASHBOARD.getTitle()).isEqualTo("Dashboard");
            assertThat(View.INCOME.getTitle()).isEqualTo("Income");
            assertThat(View.EXPENSES.getTitle()).isEqualTo("Expenses");
            assertThat(View.TAX_SUMMARY.getTitle()).isEqualTo("Tax Summary");
            assertThat(View.HMRC_SUBMISSION.getTitle()).isEqualTo("HMRC Submission");
        }
    }
}
