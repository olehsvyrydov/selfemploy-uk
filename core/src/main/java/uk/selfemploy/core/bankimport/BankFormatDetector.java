package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Detects the bank CSV format by analyzing header rows.
 *
 * <p>Uses the Strategy pattern to delegate parsing to the appropriate
 * {@link BankCsvParser} implementation based on the detected format.</p>
 */
@ApplicationScoped
public class BankFormatDetector {

    private final List<BankCsvParser> parsers;

    /**
     * CDI constructor - injects all available bank parsers.
     */
    @Inject
    public BankFormatDetector(Instance<BankCsvParser> parserInstances) {
        this.parsers = new ArrayList<>();
        parserInstances.forEach(parsers::add);
    }

    /**
     * Constructor for testing with explicit parser list.
     */
    public BankFormatDetector(List<BankCsvParser> parsers) {
        this.parsers = new ArrayList<>(parsers);
    }

    /**
     * Detects the bank format of a CSV file.
     *
     * @param csvFile the path to the CSV file
     * @param charset the character encoding
     * @return Optional containing the matching parser, or empty if no match
     */
    public Optional<BankCsvParser> detectFormat(Path csvFile, Charset charset) {
        String[] headers = extractHeaders(csvFile, charset);

        if (headers.length == 0) {
            return Optional.empty();
        }

        for (BankCsvParser parser : parsers) {
            if (parser.canParse(headers)) {
                return Optional.of(parser);
            }
        }

        return Optional.empty();
    }

    /**
     * Extracts the header row from a CSV file.
     *
     * @param csvFile the path to the CSV file
     * @param charset the character encoding
     * @return array of header names, or empty array if file is empty
     */
    public String[] extractHeaders(Path csvFile, Charset charset) {
        try (BufferedReader reader = Files.newBufferedReader(csvFile, charset)) {
            String headerLine = reader.readLine();

            if (headerLine == null || headerLine.isBlank()) {
                return new String[0];
            }

            return parseHeaderLine(headerLine);
        } catch (IOException e) {
            throw new CsvParseException("Failed to read CSV headers", csvFile.getFileName().toString(), 1, e);
        }
    }

    /**
     * Parses a header line into individual column names.
     *
     * <p>Handles:
     * <ul>
     *   <li>Quoted fields (e.g., "Field Name")</li>
     *   <li>Whitespace trimming</li>
     * </ul>
     */
    private String[] parseHeaderLine(String headerLine) {
        List<String> headers = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < headerLine.length(); i++) {
            char c = headerLine.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                headers.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Add the last field
        headers.add(current.toString().trim());

        return headers.toArray(new String[0]);
    }

    /**
     * Returns a list of all available bank names.
     *
     * @return list of bank names that can be parsed
     */
    public List<String> getAvailableBankNames() {
        return parsers.stream()
            .map(BankCsvParser::getBankName)
            .collect(Collectors.toList());
    }

    /**
     * Returns all registered parsers.
     *
     * @return list of all bank CSV parsers
     */
    public List<BankCsvParser> getParsers() {
        return new ArrayList<>(parsers);
    }
}
