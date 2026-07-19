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
 * <p>English is the built-in base language and is always available; a pack cannot replace it.
 * Resolution order for a key, in the active {@link #locale()}:</p>
 * <ol>
 *   <li>a registered {@link TranslationSource} whose locale matches the active one (exactly, else by
 *       language — so a {@code uk} pack serves a {@code uk_UA} default);</li>
 *   <li>the built-in {@code /i18n/messages*.properties} bundle for that locale;</li>
 *   <li>the base English bundle;</li>
 *   <li>otherwise the key itself, bracketed, so a gap is visible rather than fatal.</li>
 * </ol>
 *
 * <p>A non-English language can therefore ship three ways: a bundled {@code messages_<lang>.properties},
 * a {@code messages_<lang>.properties} dropped on the classpath, or a plugin that registers a
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

    /** The built-in language; a pack for this language is ignored so the shipped text stays canonical. */
    private static final String BASE_LANGUAGE = Locale.ENGLISH.getLanguage();

    /** Load only {@code .properties} and never fall back to the JVM default locale (only to the base). */
    private static final ResourceBundle.Control NO_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

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

    /** Registered sources keyed by their exact locale, so regional packs (pt-PT, pt-BR) coexist. */
    private static final Map<Locale, TranslationSource> sources = new ConcurrentHashMap<>();
    private static volatile Active active = activeFor(Locale.getDefault());
    private static final ResourceBundle FX_BUNDLE = new ResolvingBundle();

    private Messages() {
    }

    // === Language registration & selection ===

    /**
     * Registers a translation source (e.g. from a discovered language-pack plugin). A source for a
     * locale already registered replaces the previous one. A source for the built-in language, or one
     * with an undetermined locale, is ignored (English stays canonical and a language must be
     * identifiable).
     *
     * @throws NullPointerException if the source, its locale, or its display name is {@code null}
     */
    public static void register(TranslationSource source) {
        Objects.requireNonNull(source, "source");
        Locale sourceLocale = Objects.requireNonNull(source.locale(), "source.locale()");
        Objects.requireNonNull(source.displayName(), "source.displayName()");
        String language = sourceLocale.getLanguage();
        if (language.isEmpty()) {
            LOG.warning("Ignoring a translation source with an undetermined locale: " + sourceLocale);
            return;
        }
        if (language.equals(BASE_LANGUAGE)) {
            LOG.warning("Ignoring a translation source for the built-in language: " + sourceLocale);
            return;
        }
        sources.put(sourceLocale, source);
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
     *
     * @throws NullPointerException if {@code url} is {@code null} (typically a mistyped resource path)
     */
    public static FXMLLoader loader(URL url) {
        Objects.requireNonNull(url, "url (FXML resource not found — check the path)");
        return new FXMLLoader(url, FX_BUNDLE);
    }

    /**
     * The string for a key, or the key itself (bracketed) if it is missing, so a missing translation
     * is visible in the UI rather than crashing the screen.
     */
    public static String get(String key) {
        return getFrom(active, key);
    }

    /**
     * A {@link MessageFormat}-formatted message for a key, formatted in the active {@link #locale()}.
     *
     * <p>With no {@code args} the value is returned verbatim (no {@code MessageFormat} pass), so a
     * message containing a literal apostrophe is unaffected. A message that <em>does</em> use
     * {@code {0}} placeholders must double its apostrophes ({@code ''}) per {@link MessageFormat}.</p>
     */
    public static String format(String key, Object... args) {
        Active current = active; // one read → the string and its formatting locale agree
        String pattern = getFrom(current, key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        return new MessageFormat(pattern, current.locale()).format(args);
    }

    private static String getFrom(Active current, String key) {
        return resolve(current, key).orElseGet(() -> {
            LOG.warning("Missing message key: " + key);
            return "!" + key + "!";
        });
    }

    private static Optional<String> resolve(Active current, String key) {
        TranslationSource source = sourceFor(current.locale());
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

    /** The best source for {@code locale}: an exact locale match, else one for the same language. */
    private static TranslationSource sourceFor(Locale locale) {
        TranslationSource exact = sources.get(locale);
        if (exact != null) {
            return exact;
        }
        for (TranslationSource source : sources.values()) {
            if (source.locale().getLanguage().equals(locale.getLanguage())) {
                return source;
            }
        }
        return null;
    }

    private static Active activeFor(Locale locale) {
        return new Active(locale, ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale, NO_FALLBACK));
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
            // decides the value. A missing key is caught by MessageKeyCoverageTest, not at load time.
            return true;
        }

        @Override
        public Enumeration<String> getKeys() {
            return active.builtin().getKeys();
        }
    }
}
