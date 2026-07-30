package uk.selfemploy.ui.e2e;

import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.OneProfitFixture;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteExpenseService;
import uk.selfemploy.ui.service.SqliteIncomeService;
import uk.selfemploy.ui.service.SqliteTestSupport;
import uk.selfemploy.ui.util.Money;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The same dataset, read off the screens a user actually looks at.
 *
 * <p>The headless consistency suite proves the view models and the aggregation agree. It cannot see the
 * last step: whether the number a screen displays is the number its view model holds, formatted as
 * money. That wiring is what this covers — the Dashboard and the Tax Summary must show the same profit,
 * as text, for one seeded year.
 *
 * <p>It seeds through the services rather than typing into the dialogs. Driving the entry dialogs would
 * be a fuller journey, but clicks do not register on this project's CI display — an unresolved problem
 * affecting a whole cluster of tests there — so a click-driven version would be red for a reason that
 * has nothing to do with the figures. Worth revisiting once that is fixed.
 */
@Tag("e2e")
@DisplayName("One profit: the screens show what was calculated")
class OneProfitJourneyE2ETest extends BaseE2ETest {

    private static UUID businessId;

    @Override
    public void start(Stage stage) throws Exception {
        // Before the app shell loads, so the screens read the seeded data rather than the real store.
        SqliteTestSupport.setUpTestEnvironment();
        SqliteTestSupport.resetCoreServiceFactory();
        businessId = CoreServiceFactory.getDefaultBusinessId();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
        OneProfitFixture.seedToday(
                new SqliteIncomeService(businessId), new SqliteExpenseService(businessId), businessId);

        super.start(stage);
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    private String textOf(String id) {
        return lookup(id).queryAs(Label.class).getText();
    }

    /**
     * Switches screen by firing the navigation button rather than clicking it.
     *
     * <p>The handler is the same either way, and this test is about the figures a screen shows. A
     * robot click would add a dependency on the CI display registering clicks, which it currently does
     * not — the assertions would then fail for a reason unrelated to what they are checking.
     */
    private void navigateTo(String navId) {
        ToggleButton nav = lookup(navId).queryAs(ToggleButton.class);
        if (!nav.isSelected()) {
            interact(nav::fire);
            waitForFxEvents();
        }
    }

    @Test
    @DisplayName("the Dashboard shows the spend and the profit for the seeded year")
    void theDashboardShowsTheSeededFigures() {
        navigateTo("#navDashboard");

        assertThat(textOf("#incomeValue")).isEqualTo(Money.format(OneProfitFixture.TURNOVER));
        assertThat(textOf("#expensesValue"))
                .as("the Dashboard reports what was spent, including the private share and the "
                    + "disallowed category")
                .isEqualTo(Money.format(OneProfitFixture.GROSS_SPEND_WITH_PHONE));
        assertThat(textOf("#profitValue"))
                .isEqualTo(Money.format(OneProfitFixture.TAXABLE_PROFIT_WITH_PHONE));
    }

    @Test
    @DisplayName("the Tax Summary shows the same profit as the Dashboard")
    void theTaxSummaryAgreesOnScreen() {
        navigateTo("#navDashboard");
        String dashboardProfit = textOf("#profitValue");

        navigateTo("#navTax");

        assertThat(textOf("#netProfitValue"))
                .as("one year cannot have two profits, and a user comparing the two screens will notice")
                .isEqualTo(dashboardProfit);
        assertThat(textOf("#netProfitValue"))
                .isEqualTo(Money.format(OneProfitFixture.TAXABLE_PROFIT_WITH_PHONE));
    }

    @Test
    @DisplayName("the expenses screen reconciles: spent, claimable, and the rest")
    void theExpensesScreenReconciles() {
        navigateTo("#navExpenses");

        assertThat(textOf("#totalValue")).isEqualTo(Money.format(OneProfitFixture.GROSS_SPEND_WITH_PHONE));
        assertThat(textOf("#deductibleValue"))
                .isEqualTo(Money.format(OneProfitFixture.ALLOWABLE_WITH_PHONE));
        assertThat(textOf("#nonDeductibleValue"))
                .as("what cannot be claimed: the disallowed dinner plus the private %s of the phone "
                    + "bill, so the three cards add up", "40%")
                .isEqualTo(Money.format(OneProfitFixture.GROSS_SPEND_WITH_PHONE
                        .subtract(OneProfitFixture.ALLOWABLE_WITH_PHONE)));
    }

    @Test
    @DisplayName("the Tax Summary deducts only the claimable part")
    void theTaxSummaryDeductsTheClaimablePart() {
        navigateTo("#navTax");

        assertThat(textOf("#expensesCalcValue"))
                .as("shown beside 'Less: Allowable Expenses', so it must be the claimable figure and "
                    + "not the whole spend")
                .contains(Money.format(OneProfitFixture.ALLOWABLE_WITH_PHONE));
    }
}
