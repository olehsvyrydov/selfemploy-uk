package uk.selfemploy.ui.util;

import java.net.URL;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attaches the SCSS-compiled component stylesheet to individual nodes. The compiled {@code /css/components.css}
 * is produced by the Sass build; nodes with their own scene (a {@code Popup}, for example) do not inherit
 * a window's stylesheets, so a component's root attaches it directly.
 *
 * <p>Centralised so the resource path and its handling live in one place as more components migrate to SCSS.</p>
 */
public final class Stylesheets {

    private static final Logger LOG = LoggerFactory.getLogger(Stylesheets.class);

    /** Classpath location of the compiled SCSS component stylesheet. */
    public static final String COMPONENTS = "/css/components.css";

    private Stylesheets() {
    }

    /**
     * Attaches the compiled component stylesheet to {@code node}'s own stylesheets. If the stylesheet is
     * absent (a build that skipped the Sass step), the node renders unstyled and a warning is logged
     * rather than the caller failing — styling never breaks interaction.
     */
    public static void attachComponents(Parent node) {
        URL url = Stylesheets.class.getResource(COMPONENTS);
        if (url != null) {
            node.getStylesheets().add(url.toExternalForm());
        } else {
            LOG.warn("{} not on the classpath; SCSS-styled components render unstyled "
                    + "(run the Sass build via generate-resources)", COMPONENTS);
        }
    }
}
