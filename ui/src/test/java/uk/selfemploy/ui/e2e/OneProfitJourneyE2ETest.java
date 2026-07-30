package uk.selfemploy.ui.e2e;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.OneProfitFixture;
import uk.selfemploy.ui.i18n.Messages;
import uk.selfemploy.ui.service.CoreServiceFactory;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteExpenseService;
import uk.selfemploy.ui.service.SqliteIncomeService;
import uk.selfemploy.ui.service.SqliteTestSupport;
import uk.selfemploy.ui.util.Money;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /**
     * The factory is cleared as well as the store, because the two are separate pieces of state.
     * Discarding the in-memory database leaves the factory still holding services built for a business
     * that only existed inside it, and Surefire reuses one fork — so the next class to load the app
     * shell would read the real database under an id that is not in it.
     */
    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
        SqliteTestSupport.resetCoreServiceFactory();
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

        assertThat(textOf("#nonDeductibleCount"))
                .as("the dinner and the phone bill both put money in this card's total, so a count "
                    + "that named only the dinner would leave part of it unexplained")
                .isEqualTo(Messages.format("expenses.card.entries", 2));
    }

    @Test
    @DisplayName("the Tax Summary marks the spend it is not claiming")
    void theTaxSummaryMarksWhatIsNotClaimed() {
        navigateTo("#navTax");

        assertThat(textOf("#expensesTotalLabel"))
                .as("the section reports what was spent, so it must not be headed as if all of it "
                    + "were claimable")
                .isEqualTo(Money.format(OneProfitFixture.GROSS_SPEND_WITH_PHONE));

        List<String> rows = lookup(".line-item").queryAll().stream()
                .map(node -> ((Parent) node).getChildrenUnmodifiable().stream()
                        .filter(Label.class::isInstance)
                        .map(label -> ((Label) label).getText())
                        .collect(Collectors.joining(" ")))
                .toList();

        assertThat(rows)
                .as("the disallowed dinner is reported at what it cost and marked, rather than shown "
                    + "as £0.00 or silently counted as claimable")
                .anySatisfy(row -> assertThat(row)
                        .contains(Money.format(OneProfitFixture.ENTERTAINMENT))
                        .contains(Messages.get("taxSummary.lineItem.notClaimable")));
        assertThat(rows)
                .as("the office row holds the rent plus the whole phone bill, annotated with the part "
                    + "of it being claimed")
                .anySatisfy(row -> assertThat(row)
                        .contains(Money.format(
                                OneProfitFixture.OFFICE_COSTS.add(OneProfitFixture.PHONE_BILL)))
                        .contains(Money.format(OneProfitFixture.ALLOWABLE_WITH_PHONE)));
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
