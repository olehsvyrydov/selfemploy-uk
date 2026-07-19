package uk.selfemploy.ui.i18n;

import uk.selfemploy.plugin.extension.LanguagePack;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapts a plugin {@link LanguagePack} to a {@link Messages.TranslationSource}, so a discovered
 * language-pack plugin can contribute its language to the app's message system.
 *
 * <p>The plugin runtime creates one of these per discovered {@code LanguagePack} and passes it to
 * {@link Messages#register(Messages.TranslationSource)} (wiring lands with the plugin platform).</p>
 */
public final class LanguagePackSource implements Messages.TranslationSource {

    private final Locale locale;
    private final String displayName;
    private final Map<String, String> translations;

    public LanguagePackSource(LanguagePack pack) {
        Objects.requireNonNull(pack, "pack");
        this.locale = Objects.requireNonNull(pack.locale(), "pack.locale()");
        this.displayName = Objects.requireNonNull(pack.displayName(), "pack.displayName()");
        this.translations = Map.copyOf(pack.translations());
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Optional<String> translate(String key) {
        return Optional.ofNullable(translations.get(key));
    }
}
