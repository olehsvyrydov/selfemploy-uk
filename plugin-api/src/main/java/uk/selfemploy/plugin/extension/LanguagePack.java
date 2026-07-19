package uk.selfemploy.plugin.extension;

import java.util.Locale;
import java.util.Map;

/**
 * Extension point for adding a display language to the application.
 *
 * <p>Plugins implement this to translate the user interface into a new language without the app
 * being recompiled. The runtime discovers each {@code LanguagePack} and registers its translations
 * with the app's message system; the new language then appears in the language selector and its
 * strings are used wherever a key is provided (falling back to English for any key the pack omits).</p>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class UkrainianLanguagePack implements LanguagePack {
 *     @Override public Locale locale() { return Locale.forLanguageTag("uk"); }
 *     @Override public String displayName() { return "Українська"; }
 *     @Override public Map<String, String> translations() {
 *         return Map.of(
 *             "nav.dashboard", "Панель",
 *             "nav.income", "Дохід",
 *             ...);
 *     }
 * }
 * }</pre>
 *
 * <p>Keys correspond to those in the base {@code /i18n/messages.properties}; a pack may translate
 * any subset — untranslated keys fall back to English.</p>
 */
public interface LanguagePack extends ExtensionPoint {

    /** The locale this pack provides (e.g. {@code Locale.forLanguageTag("uk")}). */
    Locale locale();

    /** The language's name in that language, for a language selector (e.g. "Українська"). */
    String displayName();

    /** The translations this pack provides, as message key → translated text. */
    Map<String, String> translations();
}
