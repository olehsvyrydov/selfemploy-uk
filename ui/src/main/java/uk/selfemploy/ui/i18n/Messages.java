package uk.selfemploy.ui.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Central access point for user-facing text. All UI strings live in
 * {@code /i18n/messages*.properties} so they can be translated and reviewed as content rather than
 * being scattered as code/FXML literals.
 *
 * <p>Use {@link #bundle()} when constructing an {@link javafx.fxml.FXMLLoader} so FXML can reference
 * keys with the {@code %key} syntax, and {@link #get(String)} / {@link #format(String, Object...)}
 * from controllers.</p>
 */
public final class Messages {

    private static final Logger LOG = Logger.getLogger(Messages.class.getName());

    /** Base name of the bundle (resolved from {@code /i18n/messages.properties} on the classpath). */
    private static final String BUNDLE_BASE_NAME = "i18n.messages";

    private static volatile ResourceBundle bundle =
        ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.getDefault());

    private Messages() {
    }

    /** The active resource bundle — pass this to an {@link javafx.fxml.FXMLLoader}. */
    public static ResourceBundle bundle() {
        return bundle;
    }

    /**
     * Returns the string for a key, or the key itself (bracketed) if it is missing, so a missing
     * translation is visible in the UI rather than crashing the screen.
     *
     * @param key the message key
     * @return the localized string
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            LOG.warning("Missing message key: " + key);
            return "!" + key + "!";
        }
    }

    /**
     * Returns a {@link MessageFormat}-formatted message for a key.
     *
     * @param key  the message key (a MessageFormat pattern)
     * @param args the arguments to substitute
     * @return the formatted string
     */
    public static String format(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    /** Reloads the bundle for a given locale (e.g. when the user changes language). Test/future use. */
    public static void setLocale(Locale locale) {
        bundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
    }
}
