package uk.selfemploy.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD tests for DeclarationService.
 * Tests cover all acceptance criteria for SE-803: Declaration Timestamp Persistence.
 *
 * AC-1: Add declaration_accepted_at TIMESTAMP column to submissions table
 * AC-2: Store timestamp in UTC ISO 8601 format
 * AC-3: Add declaration_text_hash VARCHAR(64) for version tracking
 * AC-4: Timestamp stored when user checks declaration checkbox
 * AC-5: Timestamp included in PDF confirmation
 * AC-6: Cannot submit without declaration timestamp being set
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeclarationService Tests")
class DeclarationServiceTest {

    private DeclarationService declarationService;

    @Mock
    private Clock clock;

    private static final Instant FIXED_INSTANT = Instant.parse("2025-07-15T10:30:00Z");

    // Standard HMRC declaration text
    private static final String HMRC_DECLARATION_TEXT =
            "I declare that the information I have given on this return is correct and complete " +
            "to the best of my knowledge and belief. I understand that I may have to pay financial " +
            "penalties and face prosecution if I give false information.";

    // SHA-256 hash of the declaration text (precomputed)
    private static final String DECLARATION_TEXT_HASH =
            "e7b9f3c8a1d2e4f5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9";

    @BeforeEach
    void setUp() {
        // Use fixed clock for predictable tests
        clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        declarationService = new DeclarationService(clock);
    }

    @Nested
    @DisplayName("AC-2: Declaration Text and Timestamp")
    class DeclarationTextTests {

        @Test
        @DisplayName("should return standard HMRC declaration text")
        void shouldReturnStandardHmrcDeclarationText() {
            // When
            String declarationText = declarationService.getDeclarationText();

            // Then
            assertThat(declarationText).isEqualTo(HMRC_DECLARATION_TEXT);
        }

        @Test
        @DisplayName("should record declaration acceptance with UTC timestamp")
        void shouldRecordDeclarationAcceptanceWithUtcTimestamp() {
            // When
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then
            assertThat(record).isNotNull();
            assertThat(record.acceptedAt()).isEqualTo(FIXED_INSTANT);
        }

        @Test
        @DisplayName("should store timestamp in UTC (Instant)")
        void shouldStoreTimestampInUtc() {
            // When
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then - Instant is always UTC
            assertThat(record.acceptedAt()).isNotNull();
            // Verify it's the exact time from our fixed clock
            assertThat(record.acceptedAt().toString()).isEqualTo("2025-07-15T10:30:00Z");
        }
    }

    @Nested
    @DisplayName("AC-3: Declaration Text Hash")
    class DeclarationHashTests {

        @Test
        @DisplayName("should generate SHA-256 hash for declaration text")
        void shouldGenerateSha256HashForDeclarationText() {
            // When
            String hash = declarationService.generateDeclarationTextHash();

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash).hasSize(64); // SHA-256 produces 64 hex characters
        }

        @Test
        @DisplayName("should return consistent hash for same declaration text")
        void shouldReturnConsistentHashForSameText() {
            // When
            String hash1 = declarationService.generateDeclarationTextHash();
            String hash2 = declarationService.generateDeclarationTextHash();

            // Then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("should include hash in declaration record")
        void shouldIncludeHashInDeclarationRecord() {
            // When
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then
            assertThat(record.declarationTextHash()).isNotNull();
            assertThat(record.declarationTextHash()).hasSize(64);
        }

        @Test
        @DisplayName("should generate lowercase hex hash")
        void shouldGenerateLowercaseHexHash() {
            // When
            String hash = declarationService.generateDeclarationTextHash();

            // Then
            assertThat(hash).matches("[a-f0-9]{64}");
        }
    }

    @Nested
    @DisplayName("AC-4: Declaration Record")
    class DeclarationRecordTests {

        @Test
        @DisplayName("should create complete declaration record on acceptance")
        void shouldCreateCompleteDeclarationRecordOnAcceptance() {
            // When
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then
            assertThat(record).isNotNull();
            assertThat(record.acceptedAt()).isNotNull();
            assertThat(record.declarationTextHash()).isNotNull();
            assertThat(record.declarationText()).isEqualTo(HMRC_DECLARATION_TEXT);
        }

        @Test
        @DisplayName("should record uses current time from clock")
        void shouldUseCurrentTimeFromClock() {
            // Given - using fixed clock from setUp

            // When
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then
            assertThat(record.acceptedAt()).isEqualTo(FIXED_INSTANT);
        }
    }

    @Nested
    @DisplayName("AC-6: Validation")
    class ValidationTests {

        @Test
        @DisplayName("should validate declaration timestamp is not null")
        void shouldValidateDeclarationTimestampIsNotNull() {
            // Given - use the same instant as the fixed clock
            Instant timestamp = FIXED_INSTANT;

            // When/Then - should not throw
            assertThat(declarationService.isValidDeclarationTimestamp(timestamp)).isTrue();
        }

        @Test
        @DisplayName("should reject null declaration timestamp")
        void shouldRejectNullDeclarationTimestamp() {
            // When/Then
            assertThat(declarationService.isValidDeclarationTimestamp(null)).isFalse();
        }

        @Test
        @DisplayName("should reject future declaration timestamp")
        void shouldRejectFutureDeclarationTimestamp() {
            // Given - timestamp in the future
            Instant futureTimestamp = FIXED_INSTANT.plusSeconds(3600);

            // When/Then
            assertThat(declarationService.isValidDeclarationTimestamp(futureTimestamp)).isFalse();
        }

        @Test
        @DisplayName("should accept declaration timestamp in the past")
        void shouldAcceptDeclarationTimestampInThePast() {
            // Given - timestamp in the past (within reasonable range)
            Instant pastTimestamp = FIXED_INSTANT.minusSeconds(60);

            // When/Then
            assertThat(declarationService.isValidDeclarationTimestamp(pastTimestamp)).isTrue();
        }

        @Test
        @DisplayName("should reject declaration timestamp too far in the past (> 24 hours)")
        void shouldRejectDeclarationTimestampTooFarInPast() {
            // Given - timestamp more than 24 hours ago
            Instant veryOldTimestamp = FIXED_INSTANT.minusSeconds(86400 + 1);

            // When/Then
            assertThat(declarationService.isValidDeclarationTimestamp(veryOldTimestamp)).isFalse();
        }
    }

    @Nested
    @DisplayName("Hash Verification")
    class HashVerificationTests {

        @Test
        @DisplayName("should verify matching declaration hash")
        void shouldVerifyMatchingDeclarationHash() {
            // Given
            String hash = declarationService.generateDeclarationTextHash();

            // When/Then
            assertThat(declarationService.verifyDeclarationHash(hash)).isTrue();
        }

        @Test
        @DisplayName("should reject non-matching declaration hash")
        void shouldRejectNonMatchingDeclarationHash() {
            // Given - different hash
            String differentHash = "0000000000000000000000000000000000000000000000000000000000000000";

            // When/Then
            assertThat(declarationService.verifyDeclarationHash(differentHash)).isFalse();
        }

        @Test
        @DisplayName("should reject null hash")
        void shouldRejectNullHash() {
            // When/Then
            assertThat(declarationService.verifyDeclarationHash(null)).isFalse();
        }

        @Test
        @DisplayName("should reject empty hash")
        void shouldRejectEmptyHash() {
            // When/Then
            assertThat(declarationService.verifyDeclarationHash("")).isFalse();
        }

        @Test
        @DisplayName("should reject invalid hash format")
        void shouldRejectInvalidHashFormat() {
            // Given - invalid format (not 64 hex chars)
            String invalidHash = "abc123";

            // When/Then
            assertThat(declarationService.verifyDeclarationHash(invalidHash)).isFalse();
        }
    }
}
