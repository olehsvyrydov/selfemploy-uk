package uk.selfemploy.ui.service;

import uk.selfemploy.common.enums.SubmissionStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Answers whether a bookkeeping record's date falls inside a period that has
 * already been sent to HMRC, so edits and deletes to such records can warn the
 * user that their submitted figures would no longer match.
 *
 * <p>Only submissions that actually reached HMRC count (ACCEPTED or SUBMITTED).
 * Local estimates (NOT_SUBMITTED) and rejected attempts do not lock a period.</p>
 */
public final class SubmittedPeriodIndex {

    private final List<SubmissionRecord> coveringSubmissions;

    /**
     * Visible for testing. Prefer {@link #forBusiness(UUID)} in production.
     *
     * @param submissions all submissions for the business
     */
    SubmittedPeriodIndex(List<SubmissionRecord> submissions) {
        this.coveringSubmissions = submissions.stream()
            .filter(SubmittedPeriodIndex::wasSentToHmrc)
            .filter(s -> s.periodStart() != null && s.periodEnd() != null)
            // Narrowest period first, so a quarterly match is preferred over the
            // annual period that also contains the same date.
            .sorted(Comparator.comparingLong(SubmittedPeriodIndex::periodSpanDays))
            .toList();
    }

    /**
     * Builds an index from the submissions stored for the given business.
     */
    public static SubmittedPeriodIndex forBusiness(UUID businessId) {
        if (businessId == null) {
            return new SubmittedPeriodIndex(List.of());
        }
        return new SubmittedPeriodIndex(
            SqliteDataStore.getInstance().findSubmissionsByBusinessId(businessId));
    }

    /**
     * Returns the submitted period that covers the given date, if any. When both a
     * quarterly and the annual period cover the date, the quarterly one is returned.
     */
    public Optional<SubmissionRecord> coveringSubmission(LocalDate date) {
        if (date == null) {
            return Optional.empty();
        }
        return coveringSubmissions.stream()
            .filter(s -> covers(s, date))
            .findFirst();
    }

    /**
     * Returns true if the given date falls inside any period already sent to HMRC.
     */
    public boolean isCovered(LocalDate date) {
        return coveringSubmission(date).isPresent();
    }

    private static boolean covers(SubmissionRecord s, LocalDate date) {
        return !date.isBefore(s.periodStart()) && !date.isAfter(s.periodEnd());
    }

    private static boolean wasSentToHmrc(SubmissionRecord s) {
        SubmissionStatus status = parseStatus(s.status());
        return status != null && status.isSuccessful();
    }

    private static SubmissionStatus parseStatus(String name) {
        if (name == null) {
            return null;
        }
        try {
            return SubmissionStatus.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static long periodSpanDays(SubmissionRecord s) {
        return ChronoUnit.DAYS.between(s.periodStart(), s.periodEnd());
    }
}
