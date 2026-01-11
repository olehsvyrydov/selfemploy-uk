package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BankFormatDetector.
 */
@DisplayName("BankFormatDetector Tests")
class BankFormatDetectorTest {

    @TempDir
    Path tempDir;

    private BankFormatDetector detector;
    private BankCsvParser barclaysParser;
    private BankCsvParser hsbcParser;

    @BeforeEach
    void setUp() {
        barclaysParser = mock(BankCsvParser.class);
        hsbcParser = mock(BankCsvParser.class);

        when(barclaysParser.getBankName()).thenReturn("Barclays");
        when(hsbcParser.getBankName()).thenReturn("HSBC");

        detector = new BankFormatDetector(List.of(barclaysParser, hsbcParser));
    }

    @Nested
    @DisplayName("Header Detection Tests")
    class HeaderDetectionTests {

        @Test
        @DisplayName("should detect Barclays format from headers")
        void shouldDetectBarclaysFormat() throws IOException {
            String[] barclaysHeaders = {"Date", "Description", "Money Out", "Money In", "Balance"};
            when(barclaysParser.canParse(barclaysHeaders)).thenReturn(true);
            when(hsbcParser.canParse(barclaysHeaders)).thenReturn(false);

            Path csvFile = createCsvFile("Date,Description,Money Out,Money In,Balance\n2025-01-15,TEST,10.00,,500.00");

            Optional<BankCsvParser> result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
            assertThat(result.get().getBankName()).isEqualTo("Barclays");
        }

        @Test
        @DisplayName("should detect HSBC format from headers")
        void shouldDetectHsbcFormat() throws IOException {
            String[] hsbcHeaders = {"Date", "Type", "Description", "Paid Out", "Paid In", "Balance"};
            when(barclaysParser.canParse(hsbcHeaders)).thenReturn(false);
            when(hsbcParser.canParse(hsbcHeaders)).thenReturn(true);

            Path csvFile = createCsvFile("Date,Type,Description,Paid Out,Paid In,Balance\n2025-01-15,DEB,TEST,10.00,,500.00");

            Optional<BankCsvParser> result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
            assertThat(result.get().getBankName()).isEqualTo("HSBC");
        }

        @Test
        @DisplayName("should return empty when no parser matches")
        void shouldReturnEmptyWhenNoMatch() throws IOException {
            String[] unknownHeaders = {"Column1", "Column2", "Column3"};
            when(barclaysParser.canParse(unknownHeaders)).thenReturn(false);
            when(hsbcParser.canParse(unknownHeaders)).thenReturn(false);

            Path csvFile = createCsvFile("Column1,Column2,Column3\nval1,val2,val3");

            Optional<BankCsvParser> result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty file")
        void shouldHandleEmptyFile() throws IOException {
            Path csvFile = createCsvFile("");

            Optional<BankCsvParser> result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle file with only headers")
        void shouldHandleFileWithOnlyHeaders() throws IOException {
            String[] headers = {"Date", "Description", "Money Out", "Money In", "Balance"};
            when(barclaysParser.canParse(headers)).thenReturn(true);

            Path csvFile = createCsvFile("Date,Description,Money Out,Money In,Balance");

            Optional<BankCsvParser> result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("should trim whitespace from headers")
        void shouldTrimWhitespaceFromHeaders() throws IOException {
            String[] expectedHeaders = {"Date", "Description", "Money Out", "Money In", "Balance"};
            when(barclaysParser.canParse(expectedHeaders)).thenReturn(true);

            Path csvFile = createCsvFile(" Date , Description , Money Out , Money In , Balance \n2025-01-15,TEST,10.00,,500.00");

            Optional<BankCsvParser> result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("should handle quoted headers")
        void shouldHandleQuotedHeaders() throws IOException {
            String[] expectedHeaders = {"Date", "Description", "Money Out", "Money In", "Balance"};
            when(barclaysParser.canParse(expectedHeaders)).thenReturn(true);

            Path csvFile = createCsvFile("\"Date\",\"Description\",\"Money Out\",\"Money In\",\"Balance\"\n2025-01-15,TEST,10.00,,500.00");

            Optional<BankCsvParser> result = detector.detectFormat(csvFile, StandardCharsets.UTF_8);

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("Header Extraction Tests")
    class HeaderExtractionTests {

        @Test
        @DisplayName("should extract headers from file")
        void shouldExtractHeaders() throws IOException {
            Path csvFile = createCsvFile("Date,Description,Amount\n2025-01-15,TEST,10.00");

            String[] headers = detector.extractHeaders(csvFile, StandardCharsets.UTF_8);

            assertThat(headers).containsExactly("Date", "Description", "Amount");
        }

        @Test
        @DisplayName("should return empty array for empty file")
        void shouldReturnEmptyArrayForEmptyFile() throws IOException {
            Path csvFile = createCsvFile("");

            String[] headers = detector.extractHeaders(csvFile, StandardCharsets.UTF_8);

            assertThat(headers).isEmpty();
        }
    }

    @Nested
    @DisplayName("Available Parsers Tests")
    class AvailableParsersTests {

        @Test
        @DisplayName("should list all available parsers")
        void shouldListAllParsers() {
            List<String> bankNames = detector.getAvailableBankNames();

            assertThat(bankNames).containsExactlyInAnyOrder("Barclays", "HSBC");
        }
    }

    private Path createCsvFile(String content) throws IOException {
        Path file = tempDir.resolve("test.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
