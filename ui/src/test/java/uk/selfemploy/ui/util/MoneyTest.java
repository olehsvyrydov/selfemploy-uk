package uk.selfemploy.ui.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.i18n.Messages;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Money is always pounds, whatever language the app is displaying.
 *
 * <p>The figures here are UK tax figures. Asking Java for a currency format in the user's locale
 * gives that locale's currency, so a reader who has switched the app to another language would be
 * shown their liability in euros or hryvnia — a number that means nothing and looks authoritative.
 * The number formatting should follow their language; the currency must not.
 */
@DisplayName("Money - pounds in every language")
class MoneyTest {

    private final Locale original = Messages.locale();

    @AfterEach
    void restoreLocale() {
        Messages.setLocale(original);
    }

    @Test
    @DisplayName("a non-UK locale still shows pounds")
    void alwaysPounds() {
        for (Locale locale : new Locale[] {Locale.GERMANY, Locale.FRANCE, Locale.of("uk", "UA")}) {
            Messages.setLocale(locale);

            assertThat(Money.format(new BigDecimal("1234.50")))
                    .as("tax figures are sterling, and %s must not turn them into its own currency",
                            locale)
                    .containsAnyOf("£", "GBP")
                    .doesNotContain("€", "₴");
        }
    }

    @Test
    @DisplayName("the number itself is written the way that language writes numbers")
    void groupingFollowsTheLanguage() {
        Messages.setLocale(Locale.UK);
        String uk = Money.format(new BigDecimal("1234.50"));
        Messages.setLocale(Locale.GERMANY);
        String german = Money.format(new BigDecimal("1234.50"));

        assertThat(uk).isEqualTo("£1,234.50");
        assertThat(german)
                .as("German groups with a dot and separates decimals with a comma")
                .contains("1.234,50");
    }
}
