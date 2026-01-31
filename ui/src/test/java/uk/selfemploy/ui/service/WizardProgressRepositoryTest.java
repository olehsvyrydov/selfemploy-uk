package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD tests for WizardProgressRepository.
 * Following TDD methodology: tests written first, then implementation.
 *
 * Test Cases:
 * 1. findByType_notFound_returnsEmpty
 * 2. findByType_exists_returnsProgress
 * 3. save_new_insertsRecord
 * 4. save_existing_updatesRecord
 * 5. save_updatesTimestamp
 * 6. deleteByType_removes_record
 * 7. deleteByType_noRecord_noError
 * 8. save_encryptsNino
 * 9. findByType_decryptsNino
 * 10. checklistState_parsesJson
 */
@DisplayName("WizardProgressRepository")
class WizardProgressRepositoryTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-01-29T10:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);

    private SqliteWizardProgressRepository repository;

    @BeforeEach
    void setUp() {
        // Enable test mode to use in-memory database
        SqliteDataStore.testMode = true;
        SqliteDataStore.instance = null; // Reset singleton

        repository = new SqliteWizardProgressRepository(FIXED_CLOCK);
    }

    @AfterEach
    void tearDown() {
        SqliteDataStore.getInstance().close();
        SqliteDataStore.instance = null;
        SqliteDataStore.testMode = false;
    }

    @Nested
    @DisplayName("findByType()")
    class FindByTypeTests {

        @Test
        @DisplayName("should return empty when progress not found")
        void findByType_notFound_returnsEmpty() {
            Optional<WizardProgress> result = repository.findByType(WizardProgress.HMRC_CONNECTION);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return progress when exists")
        void findByType_exists_returnsProgress() {
            // Given
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                2,
                "{\"step1\":true,\"step2\":false}",
                "QQ123456C",
                FIXED_TIME,
                FIXED_TIME
            );
            repository.save(progress);

            // When
            Optional<WizardProgress> result = repository.findByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().wizardType()).isEqualTo(WizardProgress.HMRC_CONNECTION);
            assertThat(result.get().currentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when wizardType is null")
        void findByType_nullType_throwsException() {
            assertThatThrownBy(() -> repository.findByType(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wizard type cannot be null");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when wizardType is blank")
        void findByType_blankType_throwsException() {
            assertThatThrownBy(() -> repository.findByType("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wizard type cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("should insert new record when none exists")
        void save_new_insertsRecord() {
            // Given
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                1,
                null,
                null,
                FIXED_TIME,
                FIXED_TIME
            );

            // When
            WizardProgress saved = repository.save(progress);

            // Then
            assertThat(saved).isNotNull();
            assertThat(saved.wizardType()).isEqualTo(WizardProgress.HMRC_CONNECTION);
            assertThat(saved.currentStep()).isEqualTo(1);

            // Verify it's persisted
            Optional<WizardProgress> found = repository.findByType(WizardProgress.HMRC_CONNECTION);
            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("should update existing record when one exists")
        void save_existing_updatesRecord() {
            // Given - save initial progress
            WizardProgress initial = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                1,
                "{\"step1\":true}",
                null,
                FIXED_TIME,
                FIXED_TIME
            );
            repository.save(initial);

            // When - update to step 2
            WizardProgress updated = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                2,
                "{\"step1\":true,\"step2\":true}",
                "QQ123456C",
                FIXED_TIME,
                FIXED_TIME
            );
            repository.save(updated);

            // Then
            Optional<WizardProgress> found = repository.findByType(WizardProgress.HMRC_CONNECTION);
            assertThat(found).isPresent();
            assertThat(found.get().currentStep()).isEqualTo(2);
            assertThat(found.get().checklistState()).isEqualTo("{\"step1\":true,\"step2\":true}");
        }

        @Test
        @DisplayName("should update timestamp on save")
        void save_updatesTimestamp() {
            // Given - save initial progress with fixed clock
            WizardProgress initial = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                1,
                null,
                null,
                FIXED_TIME,
                FIXED_TIME
            );
            repository.save(initial);

            // When - save again with different clock
            Instant laterTime = FIXED_TIME.plusSeconds(3600);
            Clock laterClock = Clock.fixed(laterTime, ZoneOffset.UTC);
            SqliteWizardProgressRepository laterRepo = new SqliteWizardProgressRepository(laterClock);

            WizardProgress updated = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                2,
                null,
                null,
                initial.createdAt(),
                laterTime
            );
            laterRepo.save(updated);

            // Then
            Optional<WizardProgress> found = laterRepo.findByType(WizardProgress.HMRC_CONNECTION);
            assertThat(found).isPresent();
            assertThat(found.get().updatedAt()).isEqualTo(laterTime);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when progress is null")
        void save_nullProgress_throwsException() {
            assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Progress cannot be null");
        }
    }

    @Nested
    @DisplayName("deleteByType()")
    class DeleteByTypeTests {

        @Test
        @DisplayName("should remove record when exists")
        void deleteByType_removes_record() {
            // Given
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                3,
                "{\"completed\":true}",
                "QQ123456C",
                FIXED_TIME,
                FIXED_TIME
            );
            repository.save(progress);

            // Verify it exists
            assertThat(repository.findByType(WizardProgress.HMRC_CONNECTION)).isPresent();

            // When
            boolean deleted = repository.deleteByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(deleted).isTrue();
            assertThat(repository.findByType(WizardProgress.HMRC_CONNECTION)).isEmpty();
        }

        @Test
        @DisplayName("should return false when no record exists")
        void deleteByType_noRecord_noError() {
            // When
            boolean deleted = repository.deleteByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(deleted).isFalse();
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when wizardType is null")
        void deleteByType_nullType_throwsException() {
            assertThatThrownBy(() -> repository.deleteByType(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wizard type cannot be null");
        }
    }

    @Nested
    @DisplayName("NINO Encryption")
    class NinoEncryptionTests {

        @Test
        @DisplayName("should encrypt NINO when saving")
        void save_encryptsNino() {
            // Given
            String plainNino = "QQ123456C";
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                2,
                null,
                plainNino,
                FIXED_TIME,
                FIXED_TIME
            );

            // When
            repository.save(progress);

            // Then - verify raw database value is not plaintext
            // This is an implementation detail test - the NINO stored should be encrypted
            String storedValue = repository.getRawNinoFromDatabase(WizardProgress.HMRC_CONNECTION);
            assertThat(storedValue).isNotNull();
            assertThat(storedValue).isNotEqualTo(plainNino);
            assertThat(storedValue).isNotBlank();
        }

        @Test
        @DisplayName("should decrypt NINO when loading")
        void findByType_decryptsNino() {
            // Given
            String plainNino = "QQ123456C";
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                2,
                null,
                plainNino,
                FIXED_TIME,
                FIXED_TIME
            );
            repository.save(progress);

            // When
            Optional<WizardProgress> result = repository.findByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().ninoEntered()).isEqualTo(plainNino);
        }

        @Test
        @DisplayName("should handle null NINO gracefully")
        void save_nullNino_handled() {
            // Given
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                1,
                null,
                null, // null NINO
                FIXED_TIME,
                FIXED_TIME
            );

            // When
            repository.save(progress);
            Optional<WizardProgress> result = repository.findByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().ninoEntered()).isNull();
        }
    }

    @Nested
    @DisplayName("Checklist State JSON")
    class ChecklistStateTests {

        @Test
        @DisplayName("should store and retrieve JSON checklist state")
        void checklistState_parsesJson() {
            // Given
            String jsonState = "{\"prerequisiteChecks\":[true,true,false],\"currentCheckIndex\":2}";
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                2,
                jsonState,
                null,
                FIXED_TIME,
                FIXED_TIME
            );

            // When
            repository.save(progress);
            Optional<WizardProgress> result = repository.findByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().checklistState()).isEqualTo(jsonState);
        }

        @Test
        @DisplayName("should handle null checklist state")
        void checklistState_null_handled() {
            // Given
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                1,
                null, // null checklist state
                null,
                FIXED_TIME,
                FIXED_TIME
            );

            // When
            repository.save(progress);
            Optional<WizardProgress> result = repository.findByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().checklistState()).isNull();
        }

        @Test
        @DisplayName("should handle empty checklist state")
        void checklistState_empty_handled() {
            // Given
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                1,
                "", // empty checklist state
                null,
                FIXED_TIME,
                FIXED_TIME
            );

            // When
            repository.save(progress);
            Optional<WizardProgress> result = repository.findByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().checklistState()).isEmpty();
        }

        @Test
        @DisplayName("should preserve complex JSON structure")
        void checklistState_complexJson_preserved() {
            // Given
            String complexJson = """
                {
                    "steps": [
                        {"id": 1, "completed": true, "timestamp": "2026-01-29T10:00:00Z"},
                        {"id": 2, "completed": false, "timestamp": null}
                    ],
                    "metadata": {
                        "version": "1.0",
                        "lastModified": "2026-01-29T10:30:00Z"
                    }
                }
                """.trim();
            WizardProgress progress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                2,
                complexJson,
                null,
                FIXED_TIME,
                FIXED_TIME
            );

            // When
            repository.save(progress);
            Optional<WizardProgress> result = repository.findByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().checklistState()).isEqualTo(complexJson);
        }
    }

    @Nested
    @DisplayName("Timestamp Handling")
    class TimestampTests {

        @Test
        @DisplayName("should preserve createdAt timestamp on update")
        void save_preservesCreatedAt() {
            // Given
            Instant originalCreatedAt = Instant.parse("2026-01-20T10:00:00Z");
            WizardProgress initial = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                1,
                null,
                null,
                originalCreatedAt,
                originalCreatedAt
            );
            repository.save(initial);

            // When - update with new updatedAt but same createdAt
            Instant newUpdatedAt = Instant.parse("2026-01-29T10:30:00Z");
            WizardProgress updated = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                2,
                null,
                null,
                originalCreatedAt, // same createdAt
                newUpdatedAt
            );
            repository.save(updated);

            // Then
            Optional<WizardProgress> result = repository.findByType(WizardProgress.HMRC_CONNECTION);
            assertThat(result).isPresent();
            assertThat(result.get().createdAt()).isEqualTo(originalCreatedAt);
            assertThat(result.get().updatedAt()).isEqualTo(newUpdatedAt);
        }
    }

    @Nested
    @DisplayName("Multiple Wizard Types")
    class MultipleWizardTypesTests {

        @Test
        @DisplayName("should handle different wizard types independently")
        void differentWizardTypes_independent() {
            // Given
            WizardProgress hmrcProgress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                3,
                "{\"hmrc\":true}",
                "QQ123456C",
                FIXED_TIME,
                FIXED_TIME
            );
            WizardProgress otherProgress = new WizardProgress(
                "other_wizard",
                1,
                "{\"other\":true}",
                null,
                FIXED_TIME,
                FIXED_TIME
            );

            // When
            repository.save(hmrcProgress);
            repository.save(otherProgress);

            // Then
            Optional<WizardProgress> hmrcResult = repository.findByType(WizardProgress.HMRC_CONNECTION);
            Optional<WizardProgress> otherResult = repository.findByType("other_wizard");

            assertThat(hmrcResult).isPresent();
            assertThat(hmrcResult.get().currentStep()).isEqualTo(3);
            assertThat(hmrcResult.get().checklistState()).isEqualTo("{\"hmrc\":true}");

            assertThat(otherResult).isPresent();
            assertThat(otherResult.get().currentStep()).isEqualTo(1);
            assertThat(otherResult.get().checklistState()).isEqualTo("{\"other\":true}");
        }

        @Test
        @DisplayName("should delete only specified wizard type")
        void deleteByType_onlyDeletesSpecifiedType() {
            // Given
            WizardProgress hmrcProgress = new WizardProgress(
                WizardProgress.HMRC_CONNECTION,
                2,
                null,
                null,
                FIXED_TIME,
                FIXED_TIME
            );
            WizardProgress otherProgress = new WizardProgress(
                "other_wizard",
                1,
                null,
                null,
                FIXED_TIME,
                FIXED_TIME
            );
            repository.save(hmrcProgress);
            repository.save(otherProgress);

            // When
            repository.deleteByType(WizardProgress.HMRC_CONNECTION);

            // Then
            assertThat(repository.findByType(WizardProgress.HMRC_CONNECTION)).isEmpty();
            assertThat(repository.findByType("other_wizard")).isPresent();
        }
    }
}
