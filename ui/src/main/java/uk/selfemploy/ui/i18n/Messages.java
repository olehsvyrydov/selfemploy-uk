package uk.selfemploy.ui.i18n;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Central access point for user-facing text, designed so new languages can be added without
 * recompiling the app.
 *
 * <p>Resolution order for a key, in the active {@link #locale()}:</p>
 * <ol>
 *   <li>any registered {@link TranslationSource} for that locale (e.g. a language-pack plugin);</li>
 *   <li>the built-in {@code /i18n/messages*.properties} bundle for that locale;</li>
 *   <li>the base English bundle;</li>
 *   <li>otherwise the key itself, bracketed, so a gap is visible rather than fatal.</li>
 * </ol>
 *
 * <p>A language can therefore ship three ways: a bundled {@code messages_<lang>.properties}, a
 * {@code messages_<lang>.properties} dropped on the classpath, or a plugin that registers a
 * {@link TranslationSource} at runtime (see the {@code LanguagePack} plugin extension).</p>
 *
 * <p>Pass {@link #bundle()} to an {@link javafx.fxml.FXMLLoader} so FXML can reference keys with the
 * {@code %key} syntax; use {@link #get(String)} / {@link #format(String, Object...)} from code.</p>
 */
public final class Messages {

    private static final Logger LOG = Logger.getLogger(Messages.class.getName());

    /** Base name of the built-in bundle (resolved from {@code /i18n/messages.properties}). */
    private static final String BUNDLE_BASE_NAME = "i18n.messages";

    /** A pluggable provider of translations for one locale (implemented by language-pack plugins). */
    public interface TranslationSource {
        /** The locale this source provides. */
        Locale locale();

        /** A human-readable name for a language menu, in that language (e.g. "Українська"). */
        String displayName();

        /** The translation for a key, or empty if this source does not define it. */
        Optional<String> translate(String key);
    }

    private static final List<TranslationSource> sources = new CopyOnWriteArrayList<>();
    private static volatile Locale locale = Locale.getDefault();
    private static volatile ResourceBundle builtin = loadBuiltin(locale);
    private static final ResourceBundle FX_BUNDLE = new ResolvingBundle();

    private Messages() {
    }

    // === Language registration & selection ===

    /** Registers a translation source (e.g. from a discovered language-pack plugin). */
    public static void register(TranslationSource source) {
        sources.add(source);
    }

    /** Switches the active language; reload any already-shown FXML for it to take effect. */
    public static void setLocale(Locale newLocale) {
        locale = newLocale;
        builtin = loadBuiltin(newLocale);
    }

    /** The active locale. */
    public static Locale locale() {
        return locale;
    }

    /**
     * The languages the app can display: English (built in) plus any registered via a pack, keyed by
     * locale with the pack's display name (for a language selector).
     */
    public static Map<Locale, String> availableLanguages() {
        Map<Locale, String> languages = new LinkedHashMap<>();
        languages.put(Locale.ENGLISH, "English");
        for (TranslationSource source : sources) {
            languages.putIfAbsent(source.locale(), source.displayName());
        }
        return languages;
    }

    // === Lookups ===

    /** The resource bundle to hand to an {@link javafx.fxml.FXMLLoader} for {@code %key} text. */
    public static ResourceBundle bundle() {
        return FX_BUNDLE;
    }

    /**
     * The string for a key, or the key itself (bracketed) if it is missing, so a missing translation
     * is visible in the UI rather than crashing the screen.
     */
    public static String get(String key) {
        return resolve(key).orElseGet(() -> {
            LOG.warning("Missing message key: " + key);
            return "!" + key + "!";
        });
    }

    /** A {@link MessageFormat}-formatted message for a key. */
    public static String format(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    private static Optional<String> resolve(String key) {
        for (TranslationSource source : sources) {
            if (source.locale().equals(locale)) {
                Optional<String> translation = source.translate(key);
                if (translation.isPresent()) {
                    return translation;
                }
            }
        }
        try {
            return Optional.of(builtin.getString(key));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    private static ResourceBundle loadBuiltin(Locale forLocale) {
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, forLocale);
    }

    /** A live view over the resolution chain, so FXML and code always read the current language. */
    private static final class ResolvingBundle extends ResourceBundle {
        @Override
        protected Object handleGetObject(String key) {
            return resolve(key).orElse(null);
        }

        @Override
        public Enumeration<String> getKeys() {
            List<String> keys = new ArrayList<>();
            for (TranslationSource source : sources) {
                if (source.locale().equals(locale)) {
                    // Sources expose only translate(); their keys are surfaced lazily via resolve().
                    break;
                }
            }
            keys.addAll(java.util.Collections.list(builtin.getKeys()));
            return java.util.Collections.enumeration(keys);
        }
    }
}
