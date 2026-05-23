package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for MTD ITSA quarterly deadline cadence.
 *
 * <p>HMRC Obligations API v3 (deployed in production 2026-03-24) aligns the MTD ITSA
 * quarterly update deadline with VAT MTD: the <strong>7th of the month following
 * quarter end</strong>, not the 5th. Earlier drafts of the project skill files (and
 * an earlier copy of {@code DeadlineNotificationService}) hardcoded the 5th, which
 * would cause users to miss the real deadline and trigger penalty points under
 * FA 2021 Sch 24 (4 points = £200).
 *
 * <p>This test locks the explicit dates for tax year 2026/27 — the first MTD ITSA
 * mandation year (>£50k turnover threshold). If anyone changes the {@link Quarter}
 * enum back to the 5th-of-month deadlines, the build fails immediately with a
 * pointed assertion.
 *
 * @see <a href="https://github.com/hmrc/income-tax-mtd-changelog">HMRC MTD ITSA changelog</a>
 */
@DisplayName("Quarter — MTD ITSA 7th-of-month deadline cadence (SLFEMPUK-29)")
class QuarterDeadlinesTest {

    /** Tax year 2026-27 — the first MTD ITSA mandation year (income threshold >£50k). */
    private static final TaxYear TY_2026_27 = TaxYear.of(2026);

    @Test
    @DisplayName("Q1 deadline is 7 August 2026 (not 5 August)")
    void q1Deadline2026_27() {
        assertThat(Quarter.Q1.getDeadline(TY_2026_27))
            .as("Q1 (6 Apr – 5 Jul) MTD update is due 7 Aug — Obligations API v3 cadence")
            .isEqualTo(LocalDate.of(2026, 8, 7));
    }

    @Test
    @DisplayName("Q2 deadline is 7 November 2026 (not 5 November)")
    void q2Deadline2026_27() {
        assertThat(Quarter.Q2.getDeadline(TY_2026_27))
            .as("Q2 (6 Jul – 5 Oct) MTD update is due 7 Nov — Obligations API v3 cadence")
            .isEqualTo(LocalDate.of(2026, 11, 7));
    }

    @Test
    @DisplayName("Q3 deadline is 7 February 2027 (not 5 February)")
    void q3Deadline2026_27() {
        assertThat(Quarter.Q3.getDeadline(TY_2026_27))
            .as("Q3 (6 Oct – 5 Jan) MTD update is due 7 Feb of following year — Obligations API v3 cadence")
            .isEqualTo(LocalDate.of(2027, 2, 7));
    }

    @Test
    @DisplayName("Q4 deadline is 7 May 2027 (not 5 May)")
    void q4Deadline2026_27() {
        assertThat(Quarter.Q4.getDeadline(TY_2026_27))
            .as("Q4 (6 Jan – 5 Apr) MTD update is due 7 May of following year — Obligations API v3 cadence")
            .isEqualTo(LocalDate.of(2027, 5, 7));
    }

    @Test
    @DisplayName("All four 2026/27 deadlines fall on day 7 — never day 5")
    void allDeadlinesFallOn7th() {
        for (Quarter quarter : Quarter.values()) {
            LocalDate deadline = quarter.getDeadline(TY_2026_27);
            assertThat(deadline.getDayOfMonth())
                .as("%s deadline %s must be on the 7th — HMRC retired the 5th-of-month cadence", quarter, deadline)
                .isEqualTo(7);
        }
    }
}
