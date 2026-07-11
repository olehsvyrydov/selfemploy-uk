package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.ui.service.InMemoryIncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tax-year scoping regression tests (B9).
 *
 * <p>Records must appear only under the tax year their date falls in, and switching the
 * selected year must swap the visible set. This is the per-screen contract the header
 * tax-year selector relies on.</p>
 */
@DisplayName("Income tax-year scoping (B9)")
class IncomeTaxYearScopingTest {

    private static final TaxYear YEAR_2025_26 = TaxYear.of(2025); // 6 Apr 2025 - 5 Apr 2026
    private static final TaxYear YEAR_2026_27 = TaxYear.of(2026); // 6 Apr 2026 - 5 Apr 2027

    private UUID businessId;
    private InMemoryIncomeService incomeService;
    private IncomeListViewModel viewModel;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        incomeService = new InMemoryIncomeService();
        viewModel = new IncomeListViewModel(incomeService, businessId);
    }

    private void createIncome(LocalDate date, String description) {
        incomeService.create(businessId, date, new BigDecimal("100.00"), description,
            IncomeCategory.SALES, null);
    }

    @Test
    @DisplayName("a record dated 1 May 2026 appears only under 2026/27")
    void recordAppearsOnlyUnderItsOwnYear() {
        createIncome(LocalDate.of(2026, 5, 1), "May 2026 invoice");
        createIncome(LocalDate.of(2025, 5, 1), "May 2025 invoice");

        viewModel.loadIncome(YEAR_2026_27);
        assertThat(viewModel.getIncomeItems()).extracting(IncomeTableRow::description)
            .containsExactly("May 2026 invoice");

        // Switching to the previous year must swap the visible set, not accumulate it.
        viewModel.loadIncome(YEAR_2025_26);
        assertThat(viewModel.getIncomeItems()).extracting(IncomeTableRow::description)
            .containsExactly("May 2025 invoice");
    }

    @Test
    @DisplayName("the 6 April boundary splits records into the correct tax year")
    void aprilBoundaryIsScopedCorrectly() {
        createIncome(LocalDate.of(2026, 4, 5), "5 Apr - last day of 2025/26");
        createIncome(LocalDate.of(2026, 4, 6), "6 Apr - first day of 2026/27");

        viewModel.loadIncome(YEAR_2025_26);
        assertThat(viewModel.getIncomeItems()).extracting(IncomeTableRow::description)
            .containsExactly("5 Apr - last day of 2025/26");

        viewModel.loadIncome(YEAR_2026_27);
        assertThat(viewModel.getIncomeItems()).extracting(IncomeTableRow::description)
            .containsExactly("6 Apr - first day of 2026/27");
    }
}
