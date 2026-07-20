package uk.selfemploy.ui.style;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the SCSS build produced {@code /css/components.css} on the classpath with the expected
 * compiled output. Pure resource read (no JavaFX toolkit), so it runs in the normal CI test job and
 * catches a Sass regression — a token change or a rule that fails to compile — before it ships. The
 * runtime application of the stylesheet is proven separately by the display-backed {@code ComponentStylesheetTest}.
 */
@DisplayName("SCSS-compiled component CSS")
class ComponentCssContentTest {

    private static String read(String resource) throws IOException {
        try (InputStream in = ComponentCssContentTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("compiled resource %s on the classpath", resource).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("components.css exists and carries the compiled popover rules and token values")
    void componentsCssCompiled() throws IOException {
        String css = read("/css/components.css");

        assertThat(css).contains(".category-help-popover");
        // Token values compiled to the expected literals.
        assertThat(css).contains("-fx-background-color: #1e293b");
        assertThat(css).contains("-fx-border-color: #475569");
        // Nested rule flattened by Sass, and the JavaFX effect function passed through intact.
        assertThat(css).contains(".category-help-popover .popover-title");
        assertThat(css).contains("-fx-text-fill: #f8fafc");
        assertThat(css).contains("-fx-effect: dropshadow(");
        // --no-charset was honoured (JavaFX's parser can warn on @charset).
        assertThat(css).doesNotContain("@charset");
    }
}
