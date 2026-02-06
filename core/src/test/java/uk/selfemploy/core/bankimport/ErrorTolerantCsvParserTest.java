package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ErrorTolerantCsvParser.
 *
 * Verifies that malformed rows are collected as errors rather than
 * throwing exceptions, allowing the rest of the file to be parsed.
 */
@DisplayName("ErrorTolerantCsvParser Tests")
class ErrorTolerantCsvParserTest {

    @TempDir
    Path tempDir;

    private BarclaysCsvParser barclaysParser;

    @BeforeEach
    void setUp() {
        barclaysParser = new BarclaysCsvParser();
    }

    @Nested
    @DisplayName("CsvParseResult Record Tests")
    class CsvParseResultTests {

        @Test
        @DisplayName("should report total count as transactions plus errors")
        void shouldReportTotalCount() {
            var result = new CsvParseResult(
                java.util.List.of(),
                java.util.List.of(new CsvParseError(2, "bad line", "Invalid date"))
            );

            assertThat(result.totalRowsProcessed()).isEqualTo(1);
            assertThat(result.successCount()).isEqualTo(0);
            assertThat(result.errorCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should report success when no errors")
        void shouldReportSuccessWhenNoErrors() {
            var tx = new ImportedTransaction(
                LocalDate.of(2025, 6, 15), new BigDecimal("-10.00"), "TESCO", null, null
            );
            var result = new CsvParseResult(java.util.List.of(tx), java.util.List.of());

            assertThat(result.hasErrors()).isFalse();
            assertThat(result.successCount()).isEqualTo(1);
            assertThat(result.errorCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should report has errors when errors present")
        void shouldReportHasErrorsWhenPresent() {
            var result = new CsvParseResult(
                java.util.List.of(),
                java.util.List.of(new CsvParseError(2, "bad line", "Parse error"))
            );

            assertThat(result.hasErrors()).isTrue();
        }
    }

    @Nested
    @DisplayName("CsvParseError Record Tests")
    class CsvParseErrorTests {

        @Test
        @DisplayName("should store line number and raw line and message")
        void shouldStoreAllFields() {
            var error = new CsvParseError(5, "bad,data,here", "Invalid date format");

            assertThat(error.lineNumber()).isEqualTo(5);
            assertThat(error.rawLine()).isEqualTo("bad,data,here");
            assertThat(error.errorMessage()).isEqualTo("Invalid date format");
        }
    }

    @Nested
    @DisplayName("Error-Tolerant Parsing Tests")
    class ErrorTolerantParsingTests {

        @Test
        @DisplayName("should parse valid rows and collect errors for invalid rows")
        void shouldParseValidAndCollectErrors() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,VALID EXPENSE,10.00,,990.00
                INVALID_DATE,BAD ROW,10.00,,990.00
                16/06/2025,ANOTHER VALID,,50.00,1040.00
                """;
            Path csvFile = createCsvFile(csv);

            CsvParseResult result = ErrorTolerantCsvParser.parse(barclaysParser, csvFile, StandardCharsets.UTF_8);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).lineNumber()).isEqualTo(3);
            assertThat(result.errors().get(0).errorMessage()).containsIgnoringCase("date");
        }

        @Test
        @DisplayName("should return all transactions when no errors")
        void shouldReturnAllTransactionsWhenNoErrors() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,EXPENSE 1,10.00,,990.00
                16/06/2025,INCOME 1,,50.00,1040.00
                """;
            Path csvFile = createCsvFile(csv);

            CsvParseResult result = ErrorTolerantCsvParser.parse(barclaysParser, csvFile, StandardCharsets.UTF_8);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.errors()).isEmpty();
            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("should collect all errors when all rows are invalid")
        void shouldCollectAllErrorsWhenAllInvalid() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                INVALID,BAD 1,10.00,,990.00
                ALSO_BAD,BAD 2,20.00,,970.00
                """;
            Path csvFile = createCsvFile(csv);

            CsvParseResult result = ErrorTolerantCsvParser.parse(barclaysParser, csvFile, StandardCharsets.UTF_8);

            assertThat(result.transactions()).isEmpty();
            assertThat(result.errors()).hasSize(2);
        }

        @Test
        @DisplayName("should handle empty file (header only)")
        void shouldHandleEmptyFile() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                """;
            Path csvFile = createCsvFile(csv);

            CsvParseResult result = ErrorTolerantCsvParser.parse(barclaysParser, csvFile, StandardCharsets.UTF_8);

            assertThat(result.transactions()).isEmpty();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("should skip empty lines without counting as errors")
        void shouldSkipEmptyLinesWithoutErrors() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,EXPENSE,10.00,,990.00

                16/06/2025,INCOME,,50.00,1040.00
                """;
            Path csvFile = createCsvFile(csv);

            CsvParseResult result = ErrorTolerantCsvParser.parse(barclaysParser, csvFile, StandardCharsets.UTF_8);

            assertThat(result.transactions()).hasSize(2);
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("should include raw line content in error")
        void shouldIncludeRawLineInError() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                BADDATE,SOME DESC,10.00,,990.00
                """;
            Path csvFile = createCsvFile(csv);

            CsvParseResult result = ErrorTolerantCsvParser.parse(barclaysParser, csvFile, StandardCharsets.UTF_8);

            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).rawLine()).contains("BADDATE");
        }

        @Test
        @DisplayName("should handle missing amount as error")
        void shouldHandleMissingAmountAsError() throws IOException {
            String csv = """
                Date,Description,Money Out,Money In,Balance
                15/06/2025,EXPENSE,,,990.00
                16/06/2025,VALID EXPENSE,20.00,,970.00
                """;
            Path csvFile = createCsvFile(csv);

            CsvParseResult result = ErrorTolerantCsvParser.parse(barclaysParser, csvFile, StandardCharsets.UTF_8);

            assertThat(result.transactions()).hasSize(1);
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).lineNumber()).isEqualTo(2);
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("test.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
