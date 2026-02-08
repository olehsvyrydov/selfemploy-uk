package uk.selfemploy.core.bankimport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.plugin.extension.BankStatementParser;
import uk.selfemploy.plugin.runtime.ExtensionRegistry;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Facade over {@link ExtensionRegistry} for discovering and querying
 * registered {@link BankStatementParser} implementations.
 *
 * <p>This registry is lazy: it queries the ExtensionRegistry on each call
 * so that hot-reloaded plugins are picked up automatically without caching.</p>
 *
 * <h2>Lookup Methods</h2>
 * <ul>
 *   <li>{@link #getAllParsers()} -- all parsers sorted by priority (lowest first)</li>
 *   <li>{@link #findParserForFile(Path)} -- first parser whose detectFormat returns non-empty</li>
 *   <li>{@link #findParserByFormatId(String)} -- parser matching the given format ID</li>
 *   <li>{@link #getDefaultParser()} -- lowest-priority parser (typically the built-in CSV)</li>
 * </ul>
 *
 * <p>This class has zero imports from the {@code ui} module, ensuring the
 * core module does not depend on UI-layer types.</p>
 *
 * @see BankStatementParser
 * @see ExtensionRegistry
 */
public class BankStatementParserRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(BankStatementParserRegistry.class);

    private final ExtensionRegistry extensionRegistry;

    /**
     * Creates a new registry backed by the given extension registry.
     *
     * @param extensionRegistry the extension registry to query, never null
     * @throws NullPointerException if extensionRegistry is null
     */
    public BankStatementParserRegistry(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = Objects.requireNonNull(
            extensionRegistry, "extensionRegistry must not be null"
        );
    }

    /**
     * Returns all registered bank statement parsers, sorted by priority (lowest first).
     *
     * <p>The returned list is unmodifiable. The ordering is determined by the
     * {@link uk.selfemploy.plugin.extension.Prioritizable#getPriority()} value
     * via the ExtensionRegistry's conflict resolution policy.</p>
     *
     * @return unmodifiable list of parsers sorted by priority, never null
     */
    public List<BankStatementParser> getAllParsers() {
        return extensionRegistry.getExtensions(BankStatementParser.class);
    }

    /**
     * Finds the first parser that recognizes the given file, searching in priority order.
     *
     * <p>Each parser's {@link BankStatementParser#detectFormat(Path)} is called.
     * The first parser that returns a non-empty Optional is returned.</p>
     *
     * @param file the file to detect format for
     * @return the matching parser, or empty if no parser recognizes the file
     */
    public Optional<BankStatementParser> findParserForFile(Path file) {
        Objects.requireNonNull(file, "file must not be null");

        for (BankStatementParser parser : getAllParsers()) {
            try {
                Optional<String> detected = parser.detectFormat(file);
                if (detected.isPresent()) {
                    LOG.debug("Parser '{}' detected format '{}' for file: {}",
                        parser.getFormatId(), detected.get(), file);
                    return Optional.of(parser);
                }
            } catch (Exception e) {
                LOG.warn("Parser '{}' threw exception during format detection for {}: {}",
                    parser.getFormatId(), file, e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * Finds a parser by its format ID.
     *
     * @param formatId the format ID to search for (e.g., "csv", "ofx")
     * @return the matching parser, or empty if not found
     */
    public Optional<BankStatementParser> findParserByFormatId(String formatId) {
        Objects.requireNonNull(formatId, "formatId must not be null");

        return getAllParsers().stream()
            .filter(parser -> formatId.equals(parser.getFormatId()))
            .findFirst();
    }

    /**
     * Returns the default (lowest priority) parser, typically the built-in CSV parser.
     *
     * <p>Since {@link #getAllParsers()} returns parsers sorted by priority
     * (lowest first), the default parser is the first element in the list.</p>
     *
     * @return the default parser, or empty if no parsers are registered
     */
    public Optional<BankStatementParser> getDefaultParser() {
        List<BankStatementParser> parsers = getAllParsers();
        if (parsers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parsers.get(0));
    }
}
