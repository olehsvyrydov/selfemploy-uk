package uk.selfemploy.ui.service.sql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads named SQL statements from a classpath {@code .sql} resource so that SQL lives in
 * resource files rather than inline in Java code.
 *
 * <p>Statements are delimited by {@code -- name: <key>} marker lines; each statement runs
 * from its marker to the next marker (or end of file). A single trailing {@code ;} is
 * stripped so the text can be handed straight to a {@code PreparedStatement}.</p>
 *
 * <p>This is the shared mechanism for separating SQL from Java as the JDBC persistence
 * classes are refactored into ports and adapters.</p>
 */
public final class NamedSql {

    private static final String NAME_MARKER = "-- name:";

    private final Map<String, String> statements;

    private NamedSql(Map<String, String> statements) {
        this.statements = statements;
    }

    /**
     * Loads and parses the named statements from the given classpath resource.
     *
     * @param resourcePath absolute classpath path, e.g. {@code "/sql/wizard-progress.sql"}
     * @return the parsed statements
     * @throws IllegalArgumentException if the resource is missing
     * @throws UncheckedIOException     if the resource cannot be read
     */
    public static NamedSql load(String resourcePath) {
        Map<String, String> parsed = new LinkedHashMap<>();
        try (InputStream in = NamedSql.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("SQL resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String currentName = null;
                StringBuilder current = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith(NAME_MARKER)) {
                        if (currentName != null) {
                            parsed.put(currentName, finish(current));
                        }
                        currentName = trimmed.substring(NAME_MARKER.length()).trim();
                        current.setLength(0);
                    } else if (currentName != null) {
                        current.append(line).append('\n');
                    }
                }
                if (currentName != null) {
                    parsed.put(currentName, finish(current));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load SQL resource: " + resourcePath, e);
        }
        return new NamedSql(parsed);
    }

    /**
     * Returns the statement registered under the given name.
     *
     * @throws IllegalArgumentException if no statement has that name
     */
    public String get(String name) {
        String sql = statements.get(name);
        if (sql == null) {
            throw new IllegalArgumentException("No SQL statement named '" + name + "'");
        }
        return sql;
    }

    private static String finish(StringBuilder sb) {
        String sql = sb.toString().trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }
}
