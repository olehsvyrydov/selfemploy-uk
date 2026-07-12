package uk.selfemploy.core.bankimport;

import java.util.List;

/**
 * Supplies the built-in per-bank CSV parsers for non-CDI (desktop) use.
 *
 * <p>The Quarkus path injects these via {@code Instance<BankCsvParser>}; the desktop app has no
 * CDI container, so this factory provides the same set for {@link BankFormatDetector} and
 * {@link CsvStatementSource}.</p>
 */
public final class StandardBankParsers {

    private StandardBankParsers() {
    }

    /**
     * Returns a fresh list of the built-in UK bank CSV parsers.
     */
    public static List<BankCsvParser> all() {
        return List.of(
            new BarclaysCsvParser(),
            new HsbcCsvParser(),
            new LloydsCsvParser(),
            new MetroBankCsvParser(),
            new MonzoCsvParser(),
            new NationwideCsvParser(),
            new RevolutCsvParser(),
            new SantanderCsvParser(),
            new StarlingCsvParser()
        );
    }
}
