package uk.selfemploy.app;

/**
 * Packaged-app entry point.
 *
 * <p>The {@code java} launcher refuses to start a main class that extends
 * {@link javafx.application.Application} when JavaFX sits on the class path rather than the module
 * path, which is how the packaged app ships. This class is deliberately not an {@code Application}
 * subclass, so it passes that check and hands straight over to {@link Launcher}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Launcher.main(args);
    }
}
