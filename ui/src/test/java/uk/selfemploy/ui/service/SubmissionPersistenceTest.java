package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A submitted return is still there after the app is closed and reopened.
 *
 * <p>Worth its own test because losing it is unrecoverable in the way that matters: a user who has
 * filed with HMRC and then cannot see it has no way to tell, from the app, whether they filed at all.
 *
 * <p>It writes to a real file and opens it twice, because that is the only arrangement in which the
 * claim means anything. The suite's usual store is in memory, where the records and the database go
 * at the same moment — a test written against that can look like it proves persistence while proving
 * that a fresh database is empty.
 */
@DisplayName("Submission history survives a restart")
class SubmissionPersistenceTest {

    @TempDir
    Path dir;

    @Test
    @DisplayName("submissions written before a restart are readable after it")
    void submissionsSurviveAReopen() throws Exception {
        Path database = dir.resolve("selfemploy.db");
        UUID businessId = UUID.randomUUID();

        SqliteDataStore before = new SqliteDataStore(database);
        try {
            SqliteSubmissionRepository repository = new SqliteSubmissionRepository(businessId, before);
            repository.save(quarterlyReturn(businessId, "QUARTERLY_Q1", LocalDate.of(2025, 4, 6),
                    LocalDate.of(2025, 7, 5), "REF-PERSIST-001"));
            repository.save(quarterlyReturn(businessId, "QUARTERLY_Q2", LocalDate.of(2025, 7, 6),
                    LocalDate.of(2025, 10, 5), "REF-PERSIST-002"));
        } finally {
            before.close();
        }

        // A different store over the same file: what the app does on its next launch.
        SqliteDataStore after = new SqliteDataStore(database);
        try {
            List<SubmissionRecord> restored =
                    new SqliteSubmissionRepository(businessId, after).findAll();

            assertThat(restored).hasSize(2);
            assertThat(restored).extracting(SubmissionRecord::hmrcReference)
                    .as("the HMRC reference is the only proof the user has that they filed")
                    .containsExactlyInAnyOrder("REF-PERSIST-001", "REF-PERSIST-002");
            assertThat(restored).extracting(SubmissionRecord::status)
                    .containsOnly("ACCEPTED");
        } finally {
            after.close();
        }
    }

    @Test
    @DisplayName("one business's submissions do not appear under another after a restart")
    void submissionsStayWithTheirBusinessAcrossAReopen() throws Exception {
        Path database = dir.resolve("selfemploy.db");
        UUID mine = UUID.randomUUID();
        UUID theirs = UUID.randomUUID();

        SqliteDataStore before = new SqliteDataStore(database);
        try {
            new SqliteSubmissionRepository(mine, before).save(
                    quarterlyReturn(mine, "QUARTERLY_Q1", LocalDate.of(2025, 4, 6),
                            LocalDate.of(2025, 7, 5), "REF-MINE"));
            new SqliteSubmissionRepository(theirs, before).save(
                    quarterlyReturn(theirs, "QUARTERLY_Q1", LocalDate.of(2025, 4, 6),
                            LocalDate.of(2025, 7, 5), "REF-THEIRS"));
        } finally {
            before.close();
        }

        SqliteDataStore after = new SqliteDataStore(database);
        try {
            assertThat(new SqliteSubmissionRepository(mine, after).findAll())
                    .extracting(SubmissionRecord::hmrcReference)
                    .containsExactly("REF-MINE");
        } finally {
            after.close();
        }
    }

    private static SubmissionRecord quarterlyReturn(
            UUID businessId, String type, LocalDate periodStart, LocalDate periodEnd, String reference) {
        return new SubmissionRecord(
                UUID.randomUUID().toString(),
                businessId.toString(),
                type,
                periodStart.getYear(),
                periodStart,
                periodEnd,
                new BigDecimal("10000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("8000.00"),
                "ACCEPTED",
                reference,
                null,
                Instant.now());
    }
}
