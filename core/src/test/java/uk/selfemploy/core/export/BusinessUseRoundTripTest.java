package uk.selfemploy.core.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A partial claim survives being exported and imported again.
 *
 * <p>Exporting and re-importing is what a user does to move to a new machine or to come back after a
 * reinstall. If the file cannot describe a 60% phone bill, every apportioned expense returns as a
 * whole one and the allowable total silently rises — the same class of loss as an edit resetting the
 * share, but affecting every expense at once and with no obvious moment to notice it.
 */
@DisplayName("Business-use share survives an export and re-import")
class BusinessUseRoundTripTest {

    private static final UUID BUSINESS = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2025, 6, 10);

    /** Parsing needs no services; nulls keep the test on the mapping. */
    private static DataImportService importService() {
        return new DataImportService(null, null);
    }

    private static String exportedExpense(Expense expense) {
        return """
            {"metadata":{"appVersion":"1.0"},"incomes":[],"expenses":[
              {"id":"%s","date":"%s","amount":"%s","description":"%s","category":"%s",
               "sa103Box":"%s","allowable":%s,"businessUsePercentage":%d,
               "allowableAmount":"%s","receiptPath":null,"notes":null}]}"""
            .formatted(expense.id(), expense.date(), expense.amount().toPlainString(),
                expense.description(), expense.category().name(),
                expense.category().getSa103Box(), expense.category().isAllowable(),
                expense.businessUsePercentage(), expense.allowableAmount().toPlainString());
    }

    @Test
    @DisplayName("a 60% phone bill comes back at 60%")
    void theShareComesBack() {
        Expense phoneBill = Expense.create(BUSINESS, DATE, new BigDecimal("60.00"), "Phone bill",
                ExpenseCategory.OFFICE_COSTS, null, null).withBusinessUsePercentage(60);

        DataImportService.ParsedJsonData parsed = importService()
                .parseJson(exportedExpense(phoneBill).getBytes(StandardCharsets.UTF_8));

        assertThat(parsed.expenses()).singleElement().satisfies(imported -> {
            assertThat(imported.businessUsePercentage()).isEqualTo(60);
            assertThat(imported.amount()).isEqualByComparingTo(new BigDecimal("60.00"));
            assertThat(imported.allowableAmount()).isEqualByComparingTo(new BigDecimal("36.00"));
        });
    }

    @Test
    @DisplayName("restoring a backup writes the share, not just reads it")
    void restoringKeepsTheShare() {
        Expense phoneBill = Expense.create(BUSINESS, DATE, new BigDecimal("60.00"), "Phone bill",
                ExpenseCategory.OFFICE_COSTS, null, null).withBusinessUsePercentage(60);
        ExpenseService expenseService = mock(ExpenseService.class);
        when(expenseService.create(any(), any(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(phoneBill);

        new DataImportService(mock(IncomeService.class), expenseService).importJson(BUSINESS,
                exportedExpense(phoneBill).getBytes(StandardCharsets.UTF_8),
                new ImportOptions(false, false));

        ArgumentCaptor<Integer> share = ArgumentCaptor.forClass(Integer.class);
        verify(expenseService).create(any(), any(), any(), any(), any(), any(), any(), share.capture());
        assertThat(share.getValue())
                .as("the share reaching the database on a restore")
                .isEqualTo(60);
    }

    @Test
    @DisplayName("a file written before shares existed imports as wholly business")
    void anOlderFileIsWhollyBusiness() {
        String withoutTheField = """
            {"metadata":{"appVersion":"1.0"},"incomes":[],"expenses":[
              {"id":"%s","date":"2025-06-10","amount":"60.00","description":"Phone bill",
               "category":"OFFICE_COSTS","sa103Box":"25","allowable":true,
               "receiptPath":null,"notes":null}]}""".formatted(UUID.randomUUID());

        DataImportService.ParsedJsonData parsed = importService()
                .parseJson(withoutTheField.getBytes(StandardCharsets.UTF_8));

        assertThat(parsed.expenses()).singleElement().satisfies(imported ->
                assertThat(imported.businessUsePercentage())
                        .as("no stated share means the whole amount, as it did before the field existed")
                        .isEqualTo(Expense.FULLY_BUSINESS));
    }
}
