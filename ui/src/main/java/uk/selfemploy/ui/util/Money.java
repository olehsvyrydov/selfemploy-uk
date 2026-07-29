package uk.selfemploy.ui.util;

import uk.selfemploy.ui.i18n.Messages;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;

/**
 * Formats money for display.
 *
 * <p>The number follows the language the user has chosen, so separators and digit placement read
 * naturally to them. The currency does not: these are UK tax figures and they are pounds whichever
 * language the app is running in. Taking the currency from the locale as well would show a reader
 * using a different language their tax liability in the wrong money.
 */
public final class Money {

    private static final Currency POUNDS = Currency.getInstance("GBP");

    private Money() {
    }

    /** {@code amount} as pounds, grouped and punctuated for the language in use. */
    public static String format(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Messages.locale());
        format.setCurrency(POUNDS);
        return format.format(amount);
    }
}
