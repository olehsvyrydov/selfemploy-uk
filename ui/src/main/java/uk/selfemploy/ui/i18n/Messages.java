package uk.selfemploy.ui.i18n;

import javafx.fxml.FXMLLoader;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central access point for user-facing text, designed so new languages can be added without
 * recompiling the app.
 *
 * <p>Resolution order for a key, in the active {@link #locale()}:</p>
 * <ol>
 *   <li>a registered {@link TranslationSource} whose language matches (e.g. a language-pack plugin);</li>
 *   <li>the built-in {@code /i18n/messages*.properties} bundle for that locale;</li>
 *   <li>the base English bundle;</li>
 *   <li>otherwise the key itself, bracketed, so a gap is visible rather than fatal.</li>
 * </ol>
 *
 * <p>A language can therefore ship three ways: a bundled {@code messages_<lang>.properties}, a
 * {@code messages_<lang>.properties} dropped on the classpath, or a plugin that registers a
 * {@link TranslationSource} at runtime (see the {@code LanguagePack} plugin extension).</p>
 *
 * <p>Load FXML with {@link #loader(URL)} (or pass {@link #bundle()} to an {@link FXMLLoader}) so FXML
 * can reference keys with the {@code %key} syntax; use {@link #get(String)} /
 * {@link #format(String, Object...)} from code.</p>
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

    /**
     * The active locale paired with its built-in bundle. Swapped as a single reference so a reader
     * never sees the new locale against the old bundle (or vice versa).
     */
    private record Active(Locale locale, ResourceBundle builtin) {
    }

    /** Registered sources keyed by language tag, so re-registering a language replaces it. */
    private static final Map<String, TranslationSource> sources = new ConcurrentHashMap<>();
    private static volatile Active active = activeFor(Locale.getDefault());
    private static final ResourceBundle FX_BUNDLE = new ResolvingBundle();

    private Messages() {
    }

    // === Language registration & selection ===

    /**
     * Registers a translation source (e.g. from a discovered language-pack plugin). A source for a
     * language already registered replaces the previous one.
     *
     * @throws NullPointerException if the source or its locale is {@code null}
     */
    public static void register(TranslationSource source) {
        Objects.requireNonNull(source, "source");
        Locale sourceLocale = Objects.requireNonNull(source.locale(), "source.locale()");
        sources.put(sourceLocale.getLanguage(), source);
    }

    /** Switches the active language; reload any already-shown FXML for it to take effect. */
    public static void setLocale(Locale newLocale) {
        active = activeFor(Objects.requireNonNull(newLocale, "newLocale"));
    }

    /** The active locale. */
    public static Locale locale() {
        return active.locale();
    }

    /**
     * The languages the app can display: English (built in) plus any registered via a pack, keyed by
     * locale with the pack's display name (for a language selector).
     */
    public static Map<Locale, String> availableLanguages() {
        Map<Locale, String> languages = new LinkedHashMap<>();
        languages.put(Locale.ENGLISH, "English");
        for (TranslationSource source : sources.values()) {
            languages.putIfAbsent(source.locale(), source.displayName());
        }
        return languages;
    }

    // === Lookups ===

    /** The resource bundle to hand to an {@link FXMLLoader} for {@code %key} text. */
    public static ResourceBundle bundle() {
        return FX_BUNDLE;
    }

    /**
     * An {@link FXMLLoader} for {@code url} with the active message bundle already attached, so the
     * view's {@code %key} text resolves. Prefer this over constructing {@code FXMLLoader} directly:
     * an FXML that uses {@code %key} but is loaded without a bundle throws at load time.
     */
    public static FXMLLoader loader(URL url) {
        return new FXMLLoader(url, FX_BUNDLE);
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

    /** A {@link MessageFormat}-formatted message for a key, formatted in the active {@link #locale()}. */
    public static String format(String key, Object... args) {
        return new MessageFormat(get(key), locale()).format(args);
    }

    private static Optional<String> resolve(String key) {
        Active current = active; // one read → a consistent (locale, bundle) pair
        TranslationSource source = sources.get(current.locale().getLanguage());
        if (source != null) {
            Optional<String> translation = source.translate(key);
            if (translation.isPresent()) {
                return translation;
            }
        }
        try {
            return Optional.of(current.builtin().getString(key));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    private static Active activeFor(Locale locale) {
        return new Active(locale, ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale));
    }

    /** Test hook: forgets all registered sources and resets to English, for test isolation. */
    static void resetForTesting() {
        sources.clear();
        setLocale(Locale.ENGLISH);
    }

    /** A live view over the resolution chain, so FXML and code always read the current language. */
    private static final class ResolvingBundle extends ResourceBundle {
        @Override
        protected Object handleGetObject(String key) {
            // Never null: a missing key resolves to a visible "!key!" marker rather than throwing.
            return get(key);
        }

        @Override
        public boolean containsKey(String key) {
            // Every key resolves (to a translation or a visible marker), so an FXML %key never fails
            // the loader's containsKey precheck — the resolution chain, not this bundle's own keys,
            // decides the value.
            return true;
        }

        @Override
        public Enumeration<String> getKeys() {
            return active.builtin().getKeys();
        }
    }
}
