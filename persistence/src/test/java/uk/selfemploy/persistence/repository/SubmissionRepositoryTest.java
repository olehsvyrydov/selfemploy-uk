package uk.selfemploy.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.common.domain.Quarter;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.BusinessType;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.persistence.entity.SubmissionEntity;
import uk.selfemploy.persistence.exception.DuplicateSubmissionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for SubmissionRepository.
 */
@QuarkusTest
@DisplayName("SubmissionRepository Integration Tests")
class SubmissionRepositoryTest {

    @Inject
    SubmissionRepository repository;

    @Inject
    BusinessRepository businessRepository;

    private UUID businessId;
    private TaxYear taxYear2024;

    @BeforeEach
    @Transactional
    void setUp() {
        repository.deleteAll();
        businessRepository.deleteAll();

        // Create a test business
        Business business = Business.create(
            "Test Business",
            "1234567890",
            LocalDate.of(2024, 4, 6),
            LocalDate.of(2025, 4, 5),
            BusinessType.SELF_EMPLOYED,
            "Test business for repository tests"
        );
        businessRepository.save(business);
        businessId = business.id();

        taxYear2024 = TaxYear.of(2024);
    }

    // === UTR/NINO Query Tests ===

    @Test
    @Transactional
    @DisplayName("should find submissions by UTR")
    void shouldFindSubmissionsByUtr() {
        String utr = "1234567890";
        Submission submission = createSubmissionWithUtrNino(utr, "AB123456A");
        repository.save(submission);

        List<Submission> results = repository.findByUtr(utr);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).utr()).isEqualTo(utr);
    }

    @Test
    @Transactional
    @DisplayName("should find submissions by NINO")
    void shouldFindSubmissionsByNino() {
        String nino = "AB123456A";
        Submission submission = createSubmissionWithUtrNino("1234567890", nino);
        repository.save(submission);

        List<Submission> results = repository.findByNino(nino);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).nino()).isEqualTo(nino);
    }

    @Test
    @Transactional
    @DisplayName("should find submissions by UTR and tax year")
    void shouldFindSubmissionsByUtrAndTaxYear() {
        String utr = "1234567890";
        Submission submission2024 = createSubmissionWithUtrNino(utr, "AB123456A");
        repository.save(submission2024);

        TaxYear taxYear2023 = TaxYear.of(2023);
        Submission submission2023 = Submission.createAnnualWithUtrAndNino(
            businessId, taxYear2023, BigDecimal.valueOf(40000), BigDecimal.valueOf(8000),
            utr, "AB123456A"
        );
        repository.save(submission2023);

        List<Submission> results = repository.findByUtrAndTaxYear(utr, taxYear2024);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).taxYear().startYear()).isEqualTo(2024);
    }

    @Test
    @Transactional
    @DisplayName("should return empty list when no submissions found for UTR")
    void shouldReturnEmptyListWhenNoSubmissionsForUtr() {
        List<Submission> results = repository.findByUtr("9999999999");
        assertThat(results).isEmpty();
    }

    // === Retention Policy Tests ===

    @Test
    @DisplayName("should calculate retention date correctly")
    void shouldCalculateRetentionDateCorrectly() {
        SubmissionEntity entity = new SubmissionEntity();
        entity.setTaxYearStart(2024);
        entity.calculateRetentionDate();

        // Tax year 2024/25 -> Filing deadline 31 Jan 2026 -> Retention until 31 Jan 2031
        assertThat(entity.getRetentionRequiredUntil())
            .isEqualTo(LocalDate.of(2031, Month.JANUARY, 31));
    }

    @Test
    @Transactional
    @DisplayName("should find submissions with expired retention")
    void shouldFindSubmissionsWithExpiredRetention() {
        // Create submission with old tax year (retention expired)
        SubmissionEntity expiredEntity = createEntityWithRetention(2015);
        repository.persist(expiredEntity);

        // Create submission with recent tax year (retention not expired)
        SubmissionEntity validEntity = createEntityWithRetention(2024);
        repository.persist(validEntity);

        // Find expired (today is well after 2022-01-31 for tax year 2015)
        List<SubmissionEntity> expired = repository.findByRetentionExpired(LocalDate.now());

        assertThat(expired).hasSize(1);
        assertThat(expired.get(0).getTaxYearStart()).isEqualTo(2015);
    }

    @Test
    @Transactional
    @DisplayName("should find deletable submissions")
    void shouldFindDeletableSubmissions() {
        // Create non-deletable submission
        SubmissionEntity normalEntity = createEntityWithRetention(2024);
        normalEntity.setIsDeletable(false);
        repository.persist(normalEntity);

        // Create deletable submission
        SubmissionEntity deletableEntity = createEntityWithRetention(2015);
        deletableEntity.setIsDeletable(true);
        repository.persist(deletableEntity);

        List<SubmissionEntity> deletable = repository.findDeletable();

        assertThat(deletable).hasSize(1);
        assertThat(deletable.get(0).getIsDeletable()).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("should approve deletion with audit trail")
    void shouldApproveDeletionWithAuditTrail() {
        SubmissionEntity entity = createEntityWithRetention(2015);
        repository.persist(entity);

        repository.approveDeletion(entity.getId(), "user@example.com", "User requested deletion");

        SubmissionEntity updated = repository.findById(entity.getId());
        assertThat(updated.getIsDeletable()).isTrue();
        assertThat(updated.getDeletionApprovedBy()).isEqualTo("user@example.com");
        assertThat(updated.getDeletionReason()).isEqualTo("User requested deletion");
        assertThat(updated.getDeletionApprovedAt()).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("should count submissions with expired retention")
    void shouldCountSubmissionsWithExpiredRetention() {
        // Create two expired submissions
        repository.persist(createEntityWithRetention(2015));
        repository.persist(createEntityWithRetention(2016));
        // Create one valid submission
        repository.persist(createEntityWithRetention(2024));

        long count = repository.countByRetentionExpired(LocalDate.now());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("should check if retention is expired")
    void shouldCheckIfRetentionIsExpired() {
        SubmissionEntity entity = createEntityWithRetention(2015);
        // 2015 tax year -> retention until 31 Jan 2022

        assertThat(entity.isRetentionExpired(LocalDate.of(2022, 2, 1))).isTrue();
        assertThat(entity.isRetentionExpired(LocalDate.of(2022, 1, 31))).isFalse();
        assertThat(entity.isRetentionExpired(LocalDate.of(2021, 12, 31))).isFalse();
    }

    // === Basic CRUD Tests ===

    @Test
    @Transactional
    @DisplayName("should save and retrieve submission")
    void shouldSaveAndRetrieveSubmission() {
        Submission submission = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000)
        );

        Submission saved = repository.save(submission);

        assertThat(saved.id()).isEqualTo(submission.id());
        assertThat(saved.businessId()).isEqualTo(businessId);
        assertThat(saved.totalIncome()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    @Transactional
    @DisplayName("should find submission by ID")
    void shouldFindSubmissionById() {
        Submission submission = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000)
        );
        repository.save(submission);

        var found = repository.findByIdAsDomain(submission.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(submission.id());
    }

    @Test
    @Transactional
    @DisplayName("should find quarterly submission")
    void shouldFindQuarterlySubmission() {
        Submission submission = Submission.createQuarterly(
            businessId, taxYear2024, Quarter.Q1, BigDecimal.valueOf(12000), BigDecimal.valueOf(3000)
        );
        repository.save(submission);

        var found = repository.findQuarterlySubmission(businessId, taxYear2024, Quarter.Q1);

        assertThat(found).isPresent();
        assertThat(found.get().type()).isEqualTo(SubmissionType.QUARTERLY_Q1);
    }

    @Test
    @Transactional
    @DisplayName("should find annual submission")
    void shouldFindAnnualSubmission() {
        Submission submission = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000)
        );
        repository.save(submission);

        var found = repository.findAnnualSubmission(businessId, taxYear2024);

        assertThat(found).isPresent();
        assertThat(found.get().type()).isEqualTo(SubmissionType.ANNUAL);
    }

    // === Uniqueness Constraint Tests ===

    @Test
    @Transactional
    @DisplayName("should prevent duplicate annual submissions")
    void shouldPreventDuplicateAnnualSubmissions() {
        // Create first submission
        Submission first = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000)
        );
        repository.save(first);

        // Attempt to create duplicate
        Submission duplicate = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(60000), BigDecimal.valueOf(15000)
        );

        assertThatThrownBy(() -> repository.save(duplicate))
            .isInstanceOf(DuplicateSubmissionException.class)
            .hasMessageContaining(businessId.toString())
            .hasMessageContaining("2024")
            .hasMessageContaining("ANNUAL");
    }

    @Test
    @Transactional
    @DisplayName("should prevent duplicate quarterly submissions")
    void shouldPreventDuplicateQuarterlySubmissions() {
        // Create first Q1 submission
        Submission first = Submission.createQuarterly(
            businessId, taxYear2024, Quarter.Q1, BigDecimal.valueOf(12000), BigDecimal.valueOf(3000)
        );
        repository.save(first);

        // Attempt to create duplicate Q1
        Submission duplicate = Submission.createQuarterly(
            businessId, taxYear2024, Quarter.Q1, BigDecimal.valueOf(15000), BigDecimal.valueOf(4000)
        );

        assertThatThrownBy(() -> repository.save(duplicate))
            .isInstanceOf(DuplicateSubmissionException.class)
            .hasMessageContaining("QUARTERLY_Q1");
    }

    @Test
    @Transactional
    @DisplayName("should allow different quarters for same tax year")
    void shouldAllowDifferentQuartersForSameTaxYear() {
        Submission q1 = Submission.createQuarterly(
            businessId, taxYear2024, Quarter.Q1, BigDecimal.valueOf(12000), BigDecimal.valueOf(3000)
        );
        Submission q2 = Submission.createQuarterly(
            businessId, taxYear2024, Quarter.Q2, BigDecimal.valueOf(13000), BigDecimal.valueOf(3500)
        );

        repository.save(q1);
        repository.save(q2); // Should not throw

        List<Submission> submissions = repository.findByBusinessIdAndTaxYear(businessId, taxYear2024);
        assertThat(submissions).hasSize(2);
    }

    @Test
    @Transactional
    @DisplayName("should allow same type for different tax years")
    void shouldAllowSameTypeForDifferentTaxYears() {
        Submission year2024 = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000)
        );
        TaxYear taxYear2023 = TaxYear.of(2023);
        Submission year2023 = Submission.createAnnual(
            businessId, taxYear2023, BigDecimal.valueOf(45000), BigDecimal.valueOf(9000)
        );

        repository.save(year2024);
        repository.save(year2023); // Should not throw

        assertThat(repository.findByBusinessId(businessId)).hasSize(2);
    }

    @Test
    @Transactional
    @DisplayName("should allow same type for different businesses")
    void shouldAllowSameTypeForDifferentBusinesses() {
        // Create a second business
        Business business2 = Business.create(
            "Second Business",
            "0987654321",
            LocalDate.of(2024, 4, 6),
            LocalDate.of(2025, 4, 5),
            BusinessType.SELF_EMPLOYED,
            "Second test business"
        );
        businessRepository.save(business2);
        UUID businessId2 = business2.id();

        Submission submission1 = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000)
        );
        Submission submission2 = Submission.createAnnual(
            businessId2, taxYear2024, BigDecimal.valueOf(60000), BigDecimal.valueOf(12000)
        );

        repository.save(submission1);
        repository.save(submission2); // Should not throw

        assertThat(repository.findByBusinessId(businessId)).hasSize(1);
        assertThat(repository.findByBusinessId(businessId2)).hasSize(1);
    }

    @Test
    @Transactional
    @DisplayName("should return existing submission when using saveIfNotExists")
    void shouldReturnExistingSubmissionWhenUsingSaveIfNotExists() {
        Submission first = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000)
        );
        Submission saved = repository.save(first);

        Submission duplicate = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(60000), BigDecimal.valueOf(15000)
        );

        Submission result = repository.saveIfNotExists(duplicate);

        // Should return the original, not create a new one
        assertThat(result.id()).isEqualTo(saved.id());
        assertThat(result.totalIncome()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    @Transactional
    @DisplayName("should find existing submission by business, year, and type")
    void shouldFindExistingSubmission() {
        Submission submission = Submission.createAnnual(
            businessId, taxYear2024, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000)
        );
        repository.save(submission);

        var found = repository.findExistingSubmission(
            businessId, taxYear2024, SubmissionType.ANNUAL
        );

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(submission.id());
    }

    // === Helper Methods ===

    private Submission createSubmissionWithUtrNino(String utr, String nino) {
        return Submission.createAnnualWithUtrAndNino(
            businessId, taxYear2024, BigDecimal.valueOf(50000), BigDecimal.valueOf(10000),
            utr, nino
        );
    }

    private SubmissionEntity createEntityWithRetention(int taxYearStart) {
        SubmissionEntity entity = new SubmissionEntity();
        entity.setId(UUID.randomUUID());
        entity.setBusinessId(businessId);
        entity.setType(SubmissionType.ANNUAL);
        entity.setTaxYearStart(taxYearStart);
        entity.setPeriodStart(LocalDate.of(taxYearStart, 4, 6));
        entity.setPeriodEnd(LocalDate.of(taxYearStart + 1, 4, 5));
        entity.setTotalIncome(BigDecimal.valueOf(50000));
        entity.setTotalExpenses(BigDecimal.valueOf(10000));
        entity.setNetProfit(BigDecimal.valueOf(40000));
        entity.setStatus(SubmissionStatus.ACCEPTED);
        entity.setSubmittedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.calculateRetentionDate();
        return entity;
    }
}
