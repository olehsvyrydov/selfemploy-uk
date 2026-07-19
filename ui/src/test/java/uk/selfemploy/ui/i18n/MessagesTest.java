package uk.selfemploy.ui.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.extension.LanguagePack;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Messages (pluggable i18n)")
class MessagesTest {

    @AfterEach
    void reset() {
        Messages.resetForTesting();
    }

    @Test
    @DisplayName("resolves keys from the built-in English bundle")
    void resolvesBuiltinKeys() {
        Messages.setLocale(Locale.ENGLISH);
        assertThat(Messages.get("nav.dashboard")).isEqualTo("Dashboard");
        assertThat(Messages.get("bank.title")).isEqualTo("Bank");
    }

    @Test
    @DisplayName("a missing key renders visibly rather than throwing")
    void missingKeyIsVisible() {
        assertThat(Messages.get("no.such.key")).isEqualTo("!no.such.key!");
    }

    @Test
    @DisplayName("the FXML bundle renders a missing key as a marker instead of throwing")
    void bundleMissingKeyDoesNotThrow() {
        ResourceBundle bundle = Messages.bundle();
        // FXMLLoader checks containsKey then getString; both must stay non-fatal for a %key gap.
        assertThat(bundle.containsKey("no.such.key")).isTrue();
        assertThat(bundle.getString("no.such.key")).isEqualTo("!no.such.key!");
        assertThat(bundle.getString("nav.dashboard")).isEqualTo("Dashboard");
    }

    @Test
    @DisplayName("a registered language pack overrides English for its locale, falling back for gaps")
    void languagePackOverridesForItsLocale() {
        Locale uk = Locale.forLanguageTag("uk");
        Messages.register(new LanguagePackSource(new LanguagePack() {
            @Override public Locale locale() { return uk; }
            @Override public String displayName() { return "Українська"; }
            @Override public Map<String, String> translations() {
                return Map.of("nav.dashboard", "Панель"); // deliberately partial
            }
        }));

        // English active: the pack does not apply.
        Messages.setLocale(Locale.ENGLISH);
        assertThat(Messages.get("nav.dashboard")).isEqualTo("Dashboard");

        // Ukrainian active: the pack's translation wins; an untranslated key falls back to English.
        Messages.setLocale(uk);
        assertThat(Messages.get("nav.dashboard")).isEqualTo("Панель");
        assertThat(Messages.get("nav.income")).isEqualTo("Income");

        assertThat(Messages.availableLanguages()).containsKey(uk);
    }

    @Test
    @DisplayName("a pack applies even when the active locale carries a country variant")
    void languagePackAppliesToCountryVariant() {
        Locale uk = Locale.forLanguageTag("uk");
        Messages.register(new LanguagePackSource(new LanguagePack() {
            @Override public Locale locale() { return uk; }
            @Override public String displayName() { return "Українська"; }
            @Override public Map<String, String> translations() {
                return Map.of("nav.dashboard", "Панель");
            }
        }));

        Messages.setLocale(Locale.forLanguageTag("uk-UA"));
        assertThat(Messages.get("nav.dashboard")).isEqualTo("Панель");
    }
}
