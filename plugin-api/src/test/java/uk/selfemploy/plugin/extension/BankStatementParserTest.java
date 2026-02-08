package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for the {@link BankStatementParser} interface.
 * Verifies the interface hierarchy, default methods, and type constraints.
 */
@DisplayName("BankStatementParser")
class BankStatementParserTest {

    @Nested
    @DisplayName("interface hierarchy")
    class InterfaceHierarchy {

        @Test
        @DisplayName("extends DataImporter")
        void extendsDataImporter() {
            assertThat(DataImporter.class).isAssignableFrom(BankStatementParser.class);
        }

        @Test
        @DisplayName("extends Prioritizable")
        void extendsPrioritizable() {
            assertThat(Prioritizable.class).isAssignableFrom(BankStatementParser.class);
        }

        @Test
        @DisplayName("extends ExtensionPoint transitively through DataImporter")
        void extendsExtensionPointTransitively() {
            assertThat(ExtensionPoint.class).isAssignableFrom(BankStatementParser.class);
        }
    }

    @Nested
    @DisplayName("default methods")
    class DefaultMethods {

        @Test
        @DisplayName("requiresColumnMapping defaults to false")
        void requiresColumnMappingDefaultsFalse() {
            BankStatementParser parser = createMinimalParser();

            assertThat(parser.requiresColumnMapping()).isFalse();
        }

        @Test
        @DisplayName("importData bridges to parseStatement via default implementation")
        void importDataBridgesToParseStatement() {
            BankStatementParser parser = createMinimalParser();

            ImportResult result = parser.importData(Path.of("test.csv"), ImportContext.forTaxYear(2025));

            // The minimal parser returns 1 transaction, so importedCount should be 1
            assertThat(result.importedCount()).isEqualTo(1);
            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("importData returns failure when parseStatement has errors")
        void importDataReturnsFailureOnErrors() {
            BankStatementParser errorParser = createErrorParser();

            ImportResult result = errorParser.importData(Path.of("test.csv"), ImportContext.forTaxYear(2025));

            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("parsePreview returns transactions from file")
        void parsePreviewReturnsTransactions() {
            BankStatementParser parser = createMinimalParser();

            List<ParsedTransaction> preview = parser.parsePreview(Path.of("test.csv"), 5);

            assertThat(preview).hasSize(1);
        }

        @Test
        @DisplayName("getPriority defaults to plugin priority (100)")
        void getPriorityDefaultsToPlugin() {
            BankStatementParser parser = createMinimalParser();

            assertThat(parser.getPriority()).isEqualTo(Prioritizable.DEFAULT_PLUGIN_PRIORITY);
        }
    }

    /**
     * Creates a minimal BankStatementParser for testing default methods.
     */
    private BankStatementParser createMinimalParser() {
        return new BankStatementParser() {
            @Override
            public String getFormatId() {
                return "test";
            }

            @Override
            public Set<String> getSupportedBankFormats() {
                return Set.of("test-generic");
            }

            @Override
            public StatementParseResult parseStatement(StatementParseRequest request) {
                return StatementParseResult.success(
                    List.of(new ParsedTransaction(
                        LocalDate.of(2025, 1, 1), "Test", new BigDecimal("100"), null, null, null
                    )),
                    "test"
                );
            }

            @Override
            public Optional<String> detectFormat(Path file) {
                return Optional.of("test");
            }

            @Override
            public List<ParsedTransaction> parsePreview(Path file, int maxRows) {
                return parseStatement(StatementParseRequest.autoDetect()).transactions();
            }

            @Override
            public String getImporterId() {
                return "test-parser";
            }

            @Override
            public String getImporterName() {
                return "Test Parser";
            }

            @Override
            public List<String> getSupportedFileTypes() {
                return List.of(".csv");
            }
        };
    }

    /**
     * Creates a BankStatementParser that returns errors.
     */
    private BankStatementParser createErrorParser() {
        return new BankStatementParser() {
            @Override
            public String getFormatId() {
                return "error";
            }

            @Override
            public Set<String> getSupportedBankFormats() {
                return Set.of();
            }

            @Override
            public StatementParseResult parseStatement(StatementParseRequest request) {
                return StatementParseResult.failure("Parse error");
            }

            @Override
            public Optional<String> detectFormat(Path file) {
                return Optional.empty();
            }

            @Override
            public List<ParsedTransaction> parsePreview(Path file, int maxRows) {
                return List.of();
            }

            @Override
            public String getImporterId() {
                return "error-parser";
            }

            @Override
            public String getImporterName() {
                return "Error Parser";
            }

            @Override
            public List<String> getSupportedFileTypes() {
                return List.of(".csv");
            }
        };
    }
}
