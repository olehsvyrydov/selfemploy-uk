package uk.selfemploy.persistence.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.common.enums.BusinessType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("BusinessRepository Integration Tests")
class BusinessRepositoryTest {

    @Inject
    BusinessRepository repository;

    @BeforeEach
    @Transactional
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @Transactional
    @DisplayName("should save and retrieve business")
    void shouldSaveAndRetrieveBusiness() {
        Business business = Business.create(
            "Test Business",
            "1234567890",
            LocalDate.of(2025, 4, 6),
            LocalDate.of(2026, 4, 5),
            BusinessType.SELF_EMPLOYED,
            "Test description"
        );

        Business saved = repository.save(business);

        assertThat(saved.id()).isEqualTo(business.id());
        assertThat(saved.name()).isEqualTo("Test Business");
        assertThat(saved.utr()).isEqualTo("1234567890");

        Optional<Business> found = repository.findByIdAsDomain(business.id());
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Test Business");
    }

    @Test
    @Transactional
    @DisplayName("should find all active businesses")
    void shouldFindAllActiveBusinesses() {
        Business active1 = repository.save(Business.create(
            "Active Business 1", "1234567890",
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5),
            BusinessType.SELF_EMPLOYED, null
        ));

        Business active2 = repository.save(Business.create(
            "Active Business 2", "0987654321",
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5),
            BusinessType.PARTNERSHIP, null
        ));

        List<Business> activeBusinesses = repository.findAllActive();

        assertThat(activeBusinesses).hasSize(2);
        assertThat(activeBusinesses).extracting(Business::name)
            .containsExactlyInAnyOrder("Active Business 1", "Active Business 2");
    }

    @Test
    @Transactional
    @DisplayName("should find business by UTR")
    void shouldFindByUtr() {
        Business business = repository.save(Business.create(
            "Test Business", "1234567890",
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5),
            BusinessType.SELF_EMPLOYED, null
        ));

        Optional<Business> found = repository.findByUtr("1234567890");

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(business.id());
    }

    @Test
    @Transactional
    @DisplayName("should return empty when UTR not found")
    void shouldReturnEmptyWhenUtrNotFound() {
        Optional<Business> found = repository.findByUtr("0000000000");

        assertThat(found).isEmpty();
    }

    @Test
    @Transactional
    @DisplayName("should delete business")
    void shouldDeleteBusiness() {
        Business business = repository.save(Business.create(
            "Test Business", "1234567890",
            LocalDate.of(2025, 4, 6), LocalDate.of(2026, 4, 5),
            BusinessType.SELF_EMPLOYED, null
        ));

        boolean deleted = repository.deleteByIdAndReturn(business.id());

        assertThat(deleted).isTrue();
        assertThat(repository.findByIdAsDomain(business.id())).isEmpty();
    }
}
