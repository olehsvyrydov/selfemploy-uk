package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.plugin.extension.BankStatementParser;
import uk.selfemploy.plugin.extension.ImportContext;
import uk.selfemploy.plugin.extension.ImportResult;
import uk.selfemploy.plugin.extension.ParsedTransaction;
import uk.selfemploy.plugin.extension.StatementParseRequest;
import uk.selfemploy.plugin.extension.StatementParseResult;
import uk.selfemploy.plugin.runtime.ExtensionRegistry;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BankStatementParserRegistry}.
 * Verifies the registry facade over ExtensionRegistry for
 * parser discovery, format detection, and priority ordering.
 */
@DisplayName("BankStatementParserRegistry")
class BankStatementParserRegistryTest {

    private ExtensionRegistry extensionRegistry;
    private BankStatementParserRegistry registry;

    @BeforeEach
    void setUp() {
        extensionRegistry = new ExtensionRegistry();
        registry = new BankStatementParserRegistry(extensionRegistry);
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("rejects null extension registry")
        void rejectsNullExtensionRegistry() {
            assertThatThrownBy(() -> new BankStatementParserRegistry(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("extensionRegistry must not be null");
        }
    }

    @Nested
    @DisplayName("getAllParsers")
    class GetAllParsers {

        @Test
        @DisplayName("returns empty list when no parsers registered")
        void returnsEmptyWhenNoParsers() {
            List<BankStatementParser> parsers = registry.getAllParsers();
            assertThat(parsers).isEmpty();
        }

        @Test
        @DisplayName("returns single registered parser")
        void returnsSingleParser() {
            TestBankStatementParser csvParser = new TestBankStatementParser("csv", 10);
            extensionRegistry.register(BankStatementParser.class, csvParser);

            List<BankStatementParser> parsers = registry.getAllParsers();
            assertThat(parsers).hasSize(1);
            assertThat(parsers.get(0).getFormatId()).isEqualTo("csv");
        }

        @Test
        @DisplayName("returns parsers sorted by priority (lowest first)")
        void returnsParsersInPriorityOrder() {
            TestBankStatementParser csvParser = new TestBankStatementParser("csv", 10);
            TestBankStatementParser ofxParser = new TestBankStatementParser("ofx", 5);
            TestBankStatementParser qifParser = new TestBankStatementParser("qif", 100);

            extensionRegistry.register(BankStatementParser.class, csvParser);
            extensionRegistry.register(BankStatementParser.class, ofxParser);
            extensionRegistry.register(BankStatementParser.class, qifParser);

            List<BankStatementParser> parsers = registry.getAllParsers();
            assertThat(parsers).hasSize(3);
            assertThat(parsers.get(0).getFormatId()).isEqualTo("ofx");   // priority 5
            assertThat(parsers.get(1).getFormatId()).isEqualTo("csv");   // priority 10
            assertThat(parsers.get(2).getFormatId()).isEqualTo("qif");   // priority 100
        }
    }

    @Nested
    @DisplayName("findParserByFormatId")
    class FindParserByFormatId {

        @Test
        @DisplayName("finds parser by format ID")
        void findsParserByFormatId() {
            TestBankStatementParser csvParser = new TestBankStatementParser("csv", 10);
            extensionRegistry.register(BankStatementParser.class, csvParser);

            Optional<BankStatementParser> found = registry.findParserByFormatId("csv");
            assertThat(found).isPresent();
            assertThat(found.get().getFormatId()).isEqualTo("csv");
        }

        @Test
        @DisplayName("returns empty when format ID not found")
        void returnsEmptyForUnknownFormatId() {
            Optional<BankStatementParser> found = registry.findParserByFormatId("ofx");
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("rejects null format ID")
        void rejectsNullFormatId() {
            assertThatThrownBy(() -> registry.findParserByFormatId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("formatId must not be null");
        }
    }

    @Nested
    @DisplayName("findParserForFile")
    class FindParserForFile {

        @Test
        @DisplayName("finds parser that detects the file format")
        void findsParserThatDetectsFormat() {
            TestBankStatementParser csvParser = new TestBankStatementParser("csv", 10, ".csv");
            extensionRegistry.register(BankStatementParser.class, csvParser);

            Optional<BankStatementParser> found = registry.findParserForFile(Path.of("transactions.csv"));
            assertThat(found).isPresent();
            assertThat(found.get().getFormatId()).isEqualTo("csv");
        }

        @Test
        @DisplayName("returns empty when no parser detects the file")
        void returnsEmptyWhenNoDetection() {
            TestBankStatementParser csvParser = new TestBankStatementParser("csv", 10, ".csv");
            extensionRegistry.register(BankStatementParser.class, csvParser);

            Optional<BankStatementParser> found = registry.findParserForFile(Path.of("data.ofx"));
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("rejects null file path")
        void rejectsNullFile() {
            assertThatThrownBy(() -> registry.findParserForFile(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("file must not be null");
        }

        @Test
        @DisplayName("skips parsers that throw exceptions during detection")
        void skipsExceptionThrowingParsers() {
            TestBankStatementParser brokenParser = new TestBankStatementParser("broken", 1) {
                @Override
                public Optional<String> detectFormat(Path file) {
                    throw new RuntimeException("Parser is broken");
                }
            };
            TestBankStatementParser csvParser = new TestBankStatementParser("csv", 10, ".csv");

            extensionRegistry.register(BankStatementParser.class, brokenParser);
            extensionRegistry.register(BankStatementParser.class, csvParser);

            Optional<BankStatementParser> found = registry.findParserForFile(Path.of("data.csv"));
            assertThat(found).isPresent();
            assertThat(found.get().getFormatId()).isEqualTo("csv");
        }
    }

    @Nested
    @DisplayName("getDefaultParser")
    class GetDefaultParser {

        @Test
        @DisplayName("returns lowest-priority parser as default")
        void returnsLowestPriorityAsDefault() {
            TestBankStatementParser csvParser = new TestBankStatementParser("csv", 10);
            TestBankStatementParser ofxParser = new TestBankStatementParser("ofx", 5);

            extensionRegistry.register(BankStatementParser.class, csvParser);
            extensionRegistry.register(BankStatementParser.class, ofxParser);

            Optional<BankStatementParser> defaultParser = registry.getDefaultParser();
            assertThat(defaultParser).isPresent();
            // OFX has lower priority (5 < 10), so it should be first
            assertThat(defaultParser.get().getFormatId()).isEqualTo("ofx");
        }

        @Test
        @DisplayName("returns empty when no parsers registered")
        void returnsEmptyWhenNoParsers() {
            Optional<BankStatementParser> defaultParser = registry.getDefaultParser();
            assertThat(defaultParser).isEmpty();
        }
    }

    @Nested
    @DisplayName("lazy discovery")
    class LazyDiscovery {

        @Test
        @DisplayName("discovers newly registered parsers on next call")
        void discoversNewlyRegisteredParsers() {
            assertThat(registry.getAllParsers()).isEmpty();

            TestBankStatementParser csvParser = new TestBankStatementParser("csv", 10);
            extensionRegistry.register(BankStatementParser.class, csvParser);

            assertThat(registry.getAllParsers()).hasSize(1);
        }

        @Test
        @DisplayName("reflects unregistered parsers on next call")
        void reflectsUnregisteredParsers() {
            TestBankStatementParser csvParser = new TestBankStatementParser("csv", 10);
            extensionRegistry.register(BankStatementParser.class, csvParser);
            assertThat(registry.getAllParsers()).hasSize(1);

            extensionRegistry.unregister(BankStatementParser.class, csvParser);
            assertThat(registry.getAllParsers()).isEmpty();
        }
    }

    // ---------------------------------------------------------------
    // Test helper: minimal BankStatementParser for registry testing
    // ---------------------------------------------------------------

    private static class TestBankStatementParser implements BankStatementParser {

        private final String formatId;
        private final int priority;
        private final String detectedExtension;

        TestBankStatementParser(String formatId, int priority) {
            this(formatId, priority, null);
        }

        TestBankStatementParser(String formatId, int priority, String detectedExtension) {
            this.formatId = formatId;
            this.priority = priority;
            this.detectedExtension = detectedExtension;
        }

        @Override
        public String getFormatId() {
            return formatId;
        }

        @Override
        public Set<String> getSupportedBankFormats() {
            return Set.of(formatId + "-generic");
        }

        @Override
        public StatementParseResult parseStatement(StatementParseRequest request) {
            return StatementParseResult.success(List.of(), formatId);
        }

        @Override
        public Optional<String> detectFormat(Path file) {
            if (detectedExtension != null && file != null) {
                String fileName = file.getFileName().toString().toLowerCase();
                if (fileName.endsWith(detectedExtension)) {
                    return Optional.of(formatId);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<ParsedTransaction> parsePreview(Path file, int maxRows) {
            return List.of();
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public String getImporterId() {
            return formatId + "-generic";
        }

        @Override
        public String getImporterName() {
            return formatId.toUpperCase() + " Bank Statement";
        }

        @Override
        public List<String> getSupportedFileTypes() {
            return detectedExtension != null
                ? List.of(detectedExtension)
                : List.of();
        }

        @Override
        public ImportResult importData(Path file, ImportContext context) {
            return BankStatementParser.super.importData(file, context);
        }
    }
}
