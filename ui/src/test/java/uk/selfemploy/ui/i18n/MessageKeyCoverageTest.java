package uk.selfemploy.ui.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the FXML/message-bundle contract that the runtime no longer enforces: {@code Messages.bundle()}
 * renders a missing {@code %key} as a visible marker instead of failing to load, so a typo or a renamed
 * key would otherwise ship silently. This pure file scan replaces that lost fail-fast:
 *
 * <ul>
 *   <li>every {@code %key} referenced by an FXML must exist in {@code messages.properties}; and</li>
 *   <li>no {@code %key} may sit in a property <em>element</em> ({@code <text>%key</text>}), which
 *       FXMLLoader resolves only in attribute values — the element form renders the raw key.</li>
 * </ul>
 */
@DisplayName("FXML message-key coverage")
class MessageKeyCoverageTest {

    /** Orphaned, pending removal; deliberately not externalised, so excluded from the contract. */
    private static final Set<String> EXCLUDED_FXML = Set.of("column-mapping-wizard.fxml");

    private static final Pattern KEY = Pattern.compile("%([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+)");
    private static final Pattern ELEMENT_FORM =
            Pattern.compile("<(?:text|promptText|accessibleText)>\\s*%([A-Za-z0-9_.]+)");

    @Test
    @DisplayName("every %key in every FXML is defined and uses the resolvable attribute form")
    void everyKeyIsDefinedInAttributeForm() throws IOException {
        Properties bundle = loadBundle();
        Path fxmlDir = resources().resolve("fxml");

        List<String> undefined = new ArrayList<>();
        List<String> elementForm = new ArrayList<>();

        try (Stream<Path> files = Files.list(fxmlDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".fxml"))
                 .filter(p -> !EXCLUDED_FXML.contains(p.getFileName().toString()))
                 .forEach(p -> scan(p, bundle, undefined, elementForm));
        }

        assertThat(elementForm)
                .as("%%key must be in attribute form (text=\"%%key\"); the element form renders the raw key")
                .isEmpty();
        assertThat(undefined)
                .as("every %%key referenced by an FXML must exist in messages.properties")
                .isEmpty();
    }

    private static void scan(Path fxml, Properties bundle, List<String> undefined, List<String> elementForm) {
        String content;
        try {
            content = Files.readString(fxml);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String name = fxml.getFileName().toString();

        Matcher element = ELEMENT_FORM.matcher(content);
        while (element.find()) {
            elementForm.add(name + " -> " + element.group(1));
        }

        Set<String> keys = new TreeSet<>();
        Matcher key = KEY.matcher(content);
        while (key.find()) {
            keys.add(key.group(1));
        }
        for (String k : keys) {
            if (!bundle.containsKey(k)) {
                undefined.add(name + " -> " + k);
            }
        }
    }

    private static Properties loadBundle() throws IOException {
        Properties props = new Properties();
        try (InputStream in = MessageKeyCoverageTest.class.getResourceAsStream("/i18n/messages.properties")) {
            assertThat(in).as("built-in messages.properties on the classpath").isNotNull();
            props.load(in);
        }
        return props;
    }

    /** The module's {@code src/main/resources}, whether the test runs from the module or the repo root. */
    private static Path resources() {
        for (Path candidate : List.of(Path.of("src/main/resources"), Path.of("ui/src/main/resources"))) {
            if (Files.isDirectory(candidate.resolve("fxml"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("could not locate ui/src/main/resources/fxml");
    }
}
