package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubmittedPeriodIndex")
class SubmittedPeriodIndexTest {

    private SubmissionRecord submission(String type, int taxYearStart,
                                        LocalDate periodStart, LocalDate periodEnd,
                                        String status, String reference) {
        return new SubmissionRecord(
            "id-" + type + "-" + status,
            "business-1",
            type,
            taxYearStart,
            periodStart,
            periodEnd,
            new BigDecimal("100.00"),
            new BigDecimal("20.00"),
            new BigDecimal("80.00"),
            status,
            reference,
            null,
            Instant.parse("2025-08-10T09:00:00Z")
        );
    }

    private SubmissionRecord q1Accepted() {
        return submission("QUARTERLY_Q1", 2025,
            LocalDate.of(2025, 4, 6), LocalDate.of(2025, 7, 5), "ACCEPTED", "REF-Q1");
    }

    @Test
    @DisplayName("a date inside an accepted quarter is covered and names the quarter")
    void coveredByAcceptedQuarter() {
        SubmittedPeriodIndex index = new SubmittedPeriodIndex(List.of(q1Accepted()));

        Optional<SubmissionRecord> covering = index.coveringSubmission(LocalDate.of(2025, 5, 1));

        assertThat(covering).isPresent();
        assertThat(covering.get().getPeriodLabel()).isEqualTo("Q1 2025/26");
    }

    @Test
    @DisplayName("period boundaries are inclusive")
    void boundariesInclusive() {
        SubmittedPeriodIndex index = new SubmittedPeriodIndex(List.of(q1Accepted()));

        assertThat(index.isCovered(LocalDate.of(2025, 4, 6))).isTrue();  // first day
        assertThat(index.isCovered(LocalDate.of(2025, 7, 5))).isTrue();  // last day
        assertThat(index.isCovered(LocalDate.of(2025, 7, 6))).isFalse(); // next quarter
    }

    @Test
    @DisplayName("a submitted (not yet accepted) period still locks")
    void submittedPeriodLocks() {
        SubmissionRecord q1Submitted = submission("QUARTERLY_Q1", 2025,
            LocalDate.of(2025, 4, 6), LocalDate.of(2025, 7, 5), "SUBMITTED", "REF-Q1");
        SubmittedPeriodIndex index = new SubmittedPeriodIndex(List.of(q1Submitted));

        assertThat(index.isCovered(LocalDate.of(2025, 5, 1))).isTrue();
    }

    @Test
    @DisplayName("a local (NOT_SUBMITTED) period does not lock")
    void notSubmittedDoesNotLock() {
        SubmissionRecord local = submission("ANNUAL", 2025,
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5), "NOT_SUBMITTED", null);
        SubmittedPeriodIndex index = new SubmittedPeriodIndex(List.of(local));

        assertThat(index.isCovered(LocalDate.of(2025, 5, 1))).isFalse();
    }

    @Test
    @DisplayName("a rejected period does not lock")
    void rejectedDoesNotLock() {
        SubmissionRecord rejected = submission("QUARTERLY_Q1", 2025,
            LocalDate.of(2025, 4, 6), LocalDate.of(2025, 7, 5), "REJECTED", null);
        SubmittedPeriodIndex index = new SubmittedPeriodIndex(List.of(rejected));

        assertThat(index.isCovered(LocalDate.of(2025, 5, 1))).isFalse();
    }

    @Test
    @DisplayName("a date outside every submitted period is not covered")
    void dateOutsideAnyPeriod() {
        SubmittedPeriodIndex index = new SubmittedPeriodIndex(List.of(q1Accepted()));

        assertThat(index.isCovered(LocalDate.of(2025, 9, 1))).isFalse();
    }

    @Test
    @DisplayName("when a quarter and the annual period both cover a date, the quarter is returned")
    void prefersNarrowerQuarterlyPeriod() {
        SubmissionRecord annual = submission("ANNUAL", 2025,
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5), "ACCEPTED", "REF-ANNUAL");
        SubmittedPeriodIndex index = new SubmittedPeriodIndex(List.of(annual, q1Accepted()));

        Optional<SubmissionRecord> covering = index.coveringSubmission(LocalDate.of(2025, 5, 1));

        assertThat(covering).isPresent();
        assertThat(covering.get().getPeriodLabel()).isEqualTo("Q1 2025/26");
    }

    @Test
    @DisplayName("a null date is never covered")
    void nullDateNotCovered() {
        SubmittedPeriodIndex index = new SubmittedPeriodIndex(List.of(q1Accepted()));

        assertThat(index.coveringSubmission(null)).isEmpty();
    }
}
