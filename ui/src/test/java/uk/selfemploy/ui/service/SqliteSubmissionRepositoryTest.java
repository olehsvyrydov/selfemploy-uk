package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for SqliteSubmissionRepository.
 * Uses in-memory SQLite database for isolation.
 */
@DisplayName("SqliteSubmissionRepository")
class SqliteSubmissionRepositoryTest {

    private SqliteSubmissionRepository repository;
    private UUID businessId;

    @BeforeEach
    void setUp() {
        // Enable test mode to use in-memory database
        SqliteDataStore.testMode = true;
        SqliteDataStore.instance = null; // Reset singleton

        businessId = UUID.randomUUID();
        repository = new SqliteSubmissionRepository(businessId);
    }

    @AfterEach
    void tearDown() {
        SqliteDataStore.getInstance().close();
        SqliteDataStore.instance = null;
        SqliteDataStore.testMode = false;
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should throw IllegalArgumentException when businessId is null")
        void shouldThrowWhenBusinessIdNull() {
            assertThatThrownBy(() -> new SqliteSubmissionRepository(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Business ID cannot be null");
        }

        @Test
        @DisplayName("should create repository with valid businessId")
        void shouldCreateWithValidBusinessId() {
            SqliteSubmissionRepository repo = new SqliteSubmissionRepository(UUID.randomUUID());
            assertThat(repo).isNotNull();
            assertThat(repo.getBusinessId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("should save and return submission")
        void shouldSaveAndReturnSubmission() {
            SubmissionRecord record = createTestRecord("QUARTERLY_Q1", 2025, "ACCEPTED");

            SubmissionRecord saved = repository.save(record);

            assertThat(saved).isEqualTo(record);
        }

        @Test
        @DisplayName("should persist submission to database")
        void shouldPersistToDatabase() {
            SubmissionRecord record = createTestRecord("QUARTERLY_Q2", 2025, "SUBMITTED");

            repository.save(record);
            Optional<SubmissionRecord> found = repository.findById(record.id());

            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(record.id());
            assertThat(found.get().type()).isEqualTo("QUARTERLY_Q2");
            assertThat(found.get().status()).isEqualTo("SUBMITTED");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when submission is null")
        void shouldThrowWhenSubmissionNull() {
            assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Submission cannot be null");
        }

        @Test
        @DisplayName("should update existing submission on re-save")
        void shouldUpdateExistingSubmission() {
            String id = UUID.randomUUID().toString();
            SubmissionRecord original = createTestRecordWithId(id, "QUARTERLY_Q3", 2025, "PENDING");
            repository.save(original);

            SubmissionRecord updated = new SubmissionRecord(
                id, businessId.toString(), "QUARTERLY_Q3", 2025,
                original.periodStart(), original.periodEnd(),
                original.totalIncome(), original.totalExpenses(), original.netProfit(),
                "ACCEPTED", "HMRC-REF-123", null, original.submittedAt()
            );
            repository.save(updated);

            Optional<SubmissionRecord> found = repository.findById(id);
            assertThat(found).isPresent();
            assertThat(found.get().status()).isEqualTo("ACCEPTED");
            assertThat(found.get().hmrcReference()).isEqualTo("HMRC-REF-123");
        }

        @Test
        @DisplayName("should save submission with null optional fields")
        void shouldSaveWithNullOptionalFields() {
            SubmissionRecord record = new SubmissionRecord(
                UUID.randomUUID().toString(),
                businessId.toString(),
                "QUARTERLY_Q4",
                2025,
                LocalDate.of(2026, 1, 6),
                LocalDate.of(2026, 4, 5),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("4000.00"),
                "PENDING",
                null,  // hmrcReference
                null,  // errorMessage
                Instant.now()
            );

            repository.save(record);
            Optional<SubmissionRecord> found = repository.findById(record.id());

            assertThat(found).isPresent();
            assertThat(found.get().hmrcReference()).isNull();
            assertThat(found.get().errorMessage()).isNull();
        }

        @Test
        @DisplayName("should save submission with error message")
        void shouldSaveWithErrorMessage() {
            SubmissionRecord record = new SubmissionRecord(
                UUID.randomUUID().toString(),
                businessId.toString(),
                "QUARTERLY_Q1",
                2025,
                LocalDate.of(2025, 4, 6),
                LocalDate.of(2025, 7, 5),
                new BigDecimal("3000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2500.00"),
                "REJECTED",
                null,
                "FORMAT_VALUE: Invalid income format",
                Instant.now()
            );

            repository.save(record);
            Optional<SubmissionRecord> found = repository.findById(record.id());

            assertThat(found).isPresent();
            assertThat(found.get().status()).isEqualTo("REJECTED");
            assertThat(found.get().errorMessage()).isEqualTo("FORMAT_VALUE: Invalid income format");
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("should return empty list when no submissions exist")
        void shouldReturnEmptyListWhenNoSubmissions() {
            List<SubmissionRecord> all = repository.findAll();

            assertThat(all).isEmpty();
        }

        @Test
        @DisplayName("should return all submissions for business")
        void shouldReturnAllSubmissionsForBusiness() {
            repository.save(createTestRecord("QUARTERLY_Q1", 2025, "ACCEPTED"));
            repository.save(createTestRecord("QUARTERLY_Q2", 2025, "SUBMITTED"));
            repository.save(createTestRecord("QUARTERLY_Q3", 2025, "PENDING"));

            List<SubmissionRecord> all = repository.findAll();

            assertThat(all).hasSize(3);
        }

        @Test
        @DisplayName("should return submissions ordered by submitted_at descending")
        void shouldReturnOrderedBySubmittedAtDesc() {
            Instant earlier = Instant.parse("2026-01-10T10:00:00Z");
            Instant later = Instant.parse("2026-01-15T10:00:00Z");
            Instant latest = Instant.parse("2026-01-20T10:00:00Z");

            repository.save(createTestRecordWithTime("QUARTERLY_Q1", earlier));
            repository.save(createTestRecordWithTime("QUARTERLY_Q3", latest));
            repository.save(createTestRecordWithTime("QUARTERLY_Q2", later));

            List<SubmissionRecord> all = repository.findAll();

            assertThat(all).hasSize(3);
            assertThat(all.get(0).type()).isEqualTo("QUARTERLY_Q3"); // latest
            assertThat(all.get(1).type()).isEqualTo("QUARTERLY_Q2"); // later
            assertThat(all.get(2).type()).isEqualTo("QUARTERLY_Q1"); // earlier
        }

        @Test
        @DisplayName("should only return submissions for this business")
        void shouldOnlyReturnSubmissionsForThisBusiness() {
            repository.save(createTestRecord("QUARTERLY_Q1", 2025, "ACCEPTED"));

            // Create another repository for a different business
            UUID otherBusiness = UUID.randomUUID();
            SqliteSubmissionRepository otherRepo = new SqliteSubmissionRepository(otherBusiness);
            otherRepo.save(new SubmissionRecord(
                UUID.randomUUID().toString(),
                otherBusiness.toString(),
                "QUARTERLY_Q2",
                2025,
                LocalDate.of(2025, 7, 6),
                LocalDate.of(2025, 10, 5),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "ACCEPTED", null, null, Instant.now()
            ));

            List<SubmissionRecord> thisBusinessSubmissions = repository.findAll();
            List<SubmissionRecord> otherBusinessSubmissions = otherRepo.findAll();

            assertThat(thisBusinessSubmissions).hasSize(1);
            assertThat(thisBusinessSubmissions.get(0).type()).isEqualTo("QUARTERLY_Q1");
            assertThat(otherBusinessSubmissions).hasSize(1);
            assertThat(otherBusinessSubmissions.get(0).type()).isEqualTo("QUARTERLY_Q2");
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should return empty when submission not found")
        void shouldReturnEmptyWhenNotFound() {
            Optional<SubmissionRecord> found = repository.findById(UUID.randomUUID().toString());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should return submission when found")
        void shouldReturnSubmissionWhenFound() {
            SubmissionRecord record = createTestRecord("QUARTERLY_Q1", 2025, "ACCEPTED");
            repository.save(record);

            Optional<SubmissionRecord> found = repository.findById(record.id());

            assertThat(found).isPresent();
            assertThat(found.get().id()).isEqualTo(record.id());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when id is null")
        void shouldThrowWhenIdNull() {
            assertThatThrownBy(() -> repository.findById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Submission ID cannot be null");
        }
    }

    @Nested
    @DisplayName("findByTaxYear()")
    class FindByTaxYearTests {

        @Test
        @DisplayName("should return empty when no submissions for tax year")
        void shouldReturnEmptyWhenNoSubmissionsForTaxYear() {
            repository.save(createTestRecord("QUARTERLY_Q1", 2024, "ACCEPTED"));

            List<SubmissionRecord> found = repository.findByTaxYear(2025);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("should return all submissions for tax year")
        void shouldReturnAllSubmissionsForTaxYear() {
            repository.save(createTestRecord("QUARTERLY_Q1", 2025, "ACCEPTED"));
            repository.save(createTestRecord("QUARTERLY_Q2", 2025, "SUBMITTED"));
            repository.save(createTestRecord("QUARTERLY_Q1", 2024, "ACCEPTED")); // Different year

            List<SubmissionRecord> found = repository.findByTaxYear(2025);

            assertThat(found).hasSize(2);
            assertThat(found).allMatch(r -> r.taxYearStart() == 2025);
        }
    }

    @Nested
    @DisplayName("count()")
    class CountTests {

        @Test
        @DisplayName("should return 0 when no submissions")
        void shouldReturn0WhenNoSubmissions() {
            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return correct count")
        void shouldReturnCorrectCount() {
            repository.save(createTestRecord("QUARTERLY_Q1", 2025, "ACCEPTED"));
            repository.save(createTestRecord("QUARTERLY_Q2", 2025, "SUBMITTED"));

            assertThat(repository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("should return false when submission not found")
        void shouldReturnFalseWhenNotFound() {
            boolean deleted = repository.delete(UUID.randomUUID().toString());

            assertThat(deleted).isFalse();
        }

        @Test
        @DisplayName("should delete and return true when found")
        void shouldDeleteAndReturnTrueWhenFound() {
            SubmissionRecord record = createTestRecord("QUARTERLY_Q1", 2025, "ACCEPTED");
            repository.save(record);

            boolean deleted = repository.delete(record.id());

            assertThat(deleted).isTrue();
            assertThat(repository.findById(record.id())).isEmpty();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when id is null")
        void shouldThrowWhenIdNull() {
            assertThatThrownBy(() -> repository.delete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Submission ID cannot be null");
        }
    }

    // === Helper Methods ===

    private SubmissionRecord createTestRecord(String type, int taxYearStart, String status) {
        return createTestRecordWithId(UUID.randomUUID().toString(), type, taxYearStart, status);
    }

    private SubmissionRecord createTestRecordWithId(String id, String type, int taxYearStart, String status) {
        LocalDate periodStart = switch (type) {
            case "QUARTERLY_Q1" -> LocalDate.of(taxYearStart, 4, 6);
            case "QUARTERLY_Q2" -> LocalDate.of(taxYearStart, 7, 6);
            case "QUARTERLY_Q3" -> LocalDate.of(taxYearStart, 10, 6);
            case "QUARTERLY_Q4" -> LocalDate.of(taxYearStart + 1, 1, 6);
            case "ANNUAL" -> LocalDate.of(taxYearStart, 4, 6);
            default -> LocalDate.of(taxYearStart, 4, 6);
        };
        LocalDate periodEnd = switch (type) {
            case "QUARTERLY_Q1" -> LocalDate.of(taxYearStart, 7, 5);
            case "QUARTERLY_Q2" -> LocalDate.of(taxYearStart, 10, 5);
            case "QUARTERLY_Q3" -> LocalDate.of(taxYearStart + 1, 1, 5);
            case "QUARTERLY_Q4" -> LocalDate.of(taxYearStart + 1, 4, 5);
            case "ANNUAL" -> LocalDate.of(taxYearStart + 1, 4, 5);
            default -> LocalDate.of(taxYearStart, 7, 5);
        };

        return new SubmissionRecord(
            id,
            businessId.toString(),
            type,
            taxYearStart,
            periodStart,
            periodEnd,
            new BigDecimal("10000.00"),
            new BigDecimal("2000.00"),
            new BigDecimal("8000.00"),
            status,
            status.equals("ACCEPTED") ? "HMRC-REF-" + id.substring(0, 8) : null,
            status.equals("REJECTED") ? "Error message" : null,
            Instant.now()
        );
    }

    private SubmissionRecord createTestRecordWithTime(String type, Instant submittedAt) {
        return new SubmissionRecord(
            UUID.randomUUID().toString(),
            businessId.toString(),
            type,
            2025,
            LocalDate.of(2025, 4, 6),
            LocalDate.of(2025, 7, 5),
            new BigDecimal("10000.00"),
            new BigDecimal("2000.00"),
            new BigDecimal("8000.00"),
            "ACCEPTED",
            "HMRC-REF-123",
            null,
            submittedAt
        );
    }
}
