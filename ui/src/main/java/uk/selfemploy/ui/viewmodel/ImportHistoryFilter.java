package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.domain.TaxYear;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Filter options for import history.
 */
public enum ImportHistoryFilter {
    ALL_TIME("All Time") {
        @Override
        public boolean matches(LocalDateTime importDateTime) {
            return true;
        }
    },
    LAST_7_DAYS("Last 7 Days") {
        @Override
        public boolean matches(LocalDateTime importDateTime) {
            LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
            return !importDateTime.toLocalDate().isBefore(sevenDaysAgo);
        }
    },
    LAST_30_DAYS("Last 30 Days") {
        @Override
        public boolean matches(LocalDateTime importDateTime) {
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            return !importDateTime.toLocalDate().isBefore(thirtyDaysAgo);
        }
    },
    THIS_TAX_YEAR("This Tax Year") {
        @Override
        public boolean matches(LocalDateTime importDateTime) {
            TaxYear currentYear = TaxYear.current();
            return !importDateTime.toLocalDate().isBefore(currentYear.startDate()) &&
                   !importDateTime.toLocalDate().isAfter(currentYear.endDate());
        }
    };

    private final String displayText;

    ImportHistoryFilter(String displayText) {
        this.displayText = displayText;
    }

    public String getDisplayText() {
        return displayText;
    }

    /**
     * Checks if an import with the given date/time matches this filter.
     */
    public abstract boolean matches(LocalDateTime importDateTime);

    @Override
    public String toString() {
        return displayText;
    }
}
