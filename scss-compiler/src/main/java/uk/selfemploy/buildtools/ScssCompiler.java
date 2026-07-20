package uk.selfemploy.buildtools;

import de.larsgrefer.sass.embedded.SassCompiler;
import de.larsgrefer.sass.embedded.SassCompilerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Build-time SCSS compiler. Compiles every non-partial {@code *.scss} (files not starting with
 * {@code _}) in an input directory to {@code *.css} in an output directory, using the Dart Sass
 * embedded compiler bundled as a Maven Central artifact — no build-time network download.
 *
 * <p>Invoked from the build via exec-maven-plugin: {@code ScssCompiler <inputDir> <outputDir>}.
 * Partials referenced through {@code @use}/{@code @forward} are resolved by the compiler relative to
 * each entry file.</p>
 */
public final class ScssCompiler {

    private ScssCompiler() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: ScssCompiler <inputDir> <outputDir>");
        }
        File inputDir = new File(args[0]);
        File outputDir = new File(args[1]);
        if (!inputDir.isDirectory()) {
            System.out.println("[scss] no SCSS source directory at " + inputDir + " — nothing to compile");
            return;
        }
        Files.createDirectories(outputDir.toPath());

        File[] entries = inputDir.listFiles(
                (dir, name) -> name.endsWith(".scss") && !name.startsWith("_"));
        if (entries == null || entries.length == 0) {
            System.out.println("[scss] no entry stylesheets in " + inputDir);
            return;
        }

        // Defaults: OutputStyle.EXPANDED and emitCharset=false (no @charset, which JavaFX's parser dislikes).
        try (SassCompiler compiler = SassCompilerFactory.bundled()) {
            for (File entry : entries) {
                String css;
                try {
                    css = compiler.compileFile(entry).getCss();
                } catch (Exception e) {
                    throw new IOException("Failed to compile " + entry.getName(), e);
                }
                String cssName = entry.getName().substring(0, entry.getName().length() - ".scss".length()) + ".css";
                File out = new File(outputDir, cssName);
                Files.writeString(out.toPath(), css, StandardCharsets.UTF_8);
                System.out.println("[scss] " + entry.getName() + " -> css/" + cssName);
            }
        }
    }
}
