package uk.selfemploy.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Declaration Timestamp Persistence (SE-803).
 * Implements P0 test cases from /rob's QA specifications.
 *
 * <p>Test Reference: docs/sprints/sprint-6/testing/rob-qa-SE-508-SE-803.md
 *
 * <p>P0 Test Cases Implemented:
 * <ul>
 *   <li>TC-803-001: Declaration columns exist in submissions table (schema test)</li>
 *   <li>TC-803-002: Declaration timestamp stored in UTC ISO 8601</li>
 *   <li>TC-803-003: Declaration recorded on checkbox check</li>
 *   <li>TC-803-007: Valid timestamp within 24 hours accepted</li>
 *   <li>TC-803-009: Timestamp older than 24 hours rejected</li>
 *   <li>TC-803-011: Cannot submit without declaration</li>
 * </ul>
 *
 * <p>This class uses fixed {@link Clock} instances for deterministic timestamp testing,
 * ensuring tests are reliable and reproducible in CI/CD environments.
 *
 * @author /adam (Senior E2E Test Automation Engineer)
 * @see DeclarationService
 * @see DeclarationRecord
 */
@DisplayName("SE-803: Declaration Timestamp Persistence Integration Tests")
class DeclarationServiceIntegrationTest {

    private DeclarationService declarationService;

    // Fixed instant for deterministic tests: 2026-01-11T10:30:00Z
    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-11T10:30:00Z");

    // Standard HMRC declaration text
    private static final String HMRC_DECLARATION_TEXT =
            "I declare that the information I have given on this return is correct and complete " +
            "to the best of my knowledge and belief. I understand that I may have to pay financial " +
            "penalties and face prosecution if I give false information.";

    // 24-hour duration for validation
    private static final Duration MAX_DECLARATION_AGE = Duration.ofHours(24);

    @BeforeEach
    void setUp() {
        // Use fixed clock for deterministic timestamp tests
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        declarationService = new DeclarationService(fixedClock);
    }

    // =========================================================================
    // TC-803-001: Declaration Columns Exist in Submissions Table
    // Priority: P0 (Critical)
    // AC Reference: AC-1, AC-3
    // Note: Schema validation is tested via migration V7; this tests service layer
    // =========================================================================
    @Nested
    @DisplayName("TC-803-001: Declaration Fields Structure")
    class DeclarationFieldsStructure {

        @Test
        @DisplayName("DeclarationRecord should contain all required fields")
        void declarationRecordShouldContainAllRequiredFields() {
            // When: Create a declaration record
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then: Should have all required fields
            assertThat(record.acceptedAt())
                .as("declaration_accepted_at equivalent field should exist")
                .isNotNull();

            assertThat(record.declarationTextHash())
                .as("declaration_text_hash equivalent field should exist")
                .isNotNull();

            assertThat(record.declarationText())
                .as("declaration_text field should exist")
                .isNotNull();
        }

        @Test
        @DisplayName("declaration_text_hash should be VARCHAR(64) equivalent - 64 chars")
        void declarationTextHashShouldBe64Characters() {
            // When: Generate hash
            String hash = declarationService.generateDeclarationTextHash();

            // Then: Should be exactly 64 characters (SHA-256 hex)
            assertThat(hash)
                .as("Hash should be 64 characters (VARCHAR(64))")
                .hasSize(64);
        }
    }

    // =========================================================================
    // TC-803-002: Declaration Timestamp Stored in UTC ISO 8601
    // Priority: P0 (Critical)
    // AC Reference: AC-2
    // =========================================================================
    @Nested
    @DisplayName("TC-803-002: Declaration Timestamp Stored in UTC")
    class TimestampStoredInUtc {

        @Test
        @DisplayName("timestamp should be stored in UTC")
        void timestampShouldBeStoredInUtc() {
            // When: Record declaration
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then: Timestamp should be in UTC (Instant is always UTC)
            Instant timestamp = record.acceptedAt();
            assertThat(timestamp)
                .as("Timestamp should be recorded")
                .isNotNull();

            // Instant toString() always produces UTC format ending with 'Z'
            assertThat(timestamp.toString())
                .as("Timestamp should be in UTC format (ending with Z)")
                .endsWith("Z");
        }

        @Test
        @DisplayName("timestamp should match fixed clock time")
        void timestampShouldMatchFixedClockTime() {
            // When: Record declaration with fixed clock
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then: Should match our fixed instant
            assertThat(record.acceptedAt())
                .as("Timestamp should match fixed clock time")
                .isEqualTo(FIXED_INSTANT);
        }

        @Test
        @DisplayName("timestamp should be ISO 8601 compatible")
        void timestampShouldBeIso8601Compatible() {
            // When: Record declaration
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then: Should parse back correctly (ISO 8601 roundtrip)
            String isoString = record.acceptedAt().toString();
            Instant parsed = Instant.parse(isoString);

            assertThat(parsed)
                .as("Timestamp should roundtrip through ISO 8601 parsing")
                .isEqualTo(record.acceptedAt());

            // Verify format: 2026-01-11T10:30:00Z
            assertThat(isoString)
                .as("ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ")
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z");
        }
    }

    // =========================================================================
    // TC-803-003: Declaration Recorded on Checkbox Check
    // Priority: P0 (Critical)
    // AC Reference: AC-4
    // =========================================================================
    @Nested
    @DisplayName("TC-803-003: Declaration Recorded on Checkbox Check")
    class DeclarationRecordedOnCheck {

        @Test
        @DisplayName("recordDeclarationAcceptance should create complete DeclarationRecord")
        void recordDeclarationAcceptanceShouldCreateCompleteRecord() {
            // When: Record declaration (simulating checkbox check)
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then: Should have all fields populated
            assertThat(record)
                .as("DeclarationRecord should be created")
                .isNotNull();

            assertThat(record.acceptedAt())
                .as("acceptedAt should be populated")
                .isEqualTo(FIXED_INSTANT);

            assertThat(record.declarationTextHash())
                .as("declarationTextHash should be populated")
                .hasSize(64)
                .matches("[a-f0-9]{64}");

            assertThat(record.declarationText())
                .as("declarationText should contain HMRC declaration")
                .isEqualTo(HMRC_DECLARATION_TEXT);
        }

        @Test
        @DisplayName("timestamp should be recorded at moment of checkbox check")
        void timestampShouldBeRecordedAtMomentOfCheck() {
            // Given: Fixed time is 2026-01-11T10:30:00Z

            // When: Record declaration
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then: Timestamp should be exactly the fixed time (within 2 seconds for real-world tolerance)
            assertThat(record.acceptedAt())
                .as("Timestamp should be recorded at moment of check")
                .isEqualTo(FIXED_INSTANT);
        }

        @Test
        @DisplayName("hash should be valid SHA-256 format")
        void hashShouldBeValidSha256Format() {
            // When: Record declaration
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then: Hash should be 64 lowercase hex characters
            assertThat(record.declarationTextHash())
                .as("Hash should be 64 lowercase hex characters")
                .hasSize(64)
                .matches("[a-f0-9]{64}");
        }

        @Test
        @DisplayName("declaration text should match official HMRC wording")
        void declarationTextShouldMatchOfficialHmrcWording() {
            // When: Record declaration
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then: Text should be the official HMRC declaration
            assertThat(record.declarationText())
                .as("Declaration text should start with 'I declare'")
                .startsWith("I declare that the information I have given");

            assertThat(record.declarationText())
                .as("Declaration text should mention penalties")
                .contains("financial penalties");

            assertThat(record.declarationText())
                .as("Declaration text should mention prosecution")
                .contains("prosecution");

            assertThat(record.declarationText())
                .as("Declaration text should mention false information")
                .contains("false information");
        }
    }

    // =========================================================================
    // TC-803-007: Valid Timestamp Within 24 Hours Accepted
    // Priority: P0 (Critical)
    // AC Reference: AC-4
    // =========================================================================
    @Nested
    @DisplayName("TC-803-007: Valid Timestamp Within 24 Hours Accepted")
    class ValidTimestampWithin24Hours {

        @Test
        @DisplayName("current timestamp should be valid")
        void currentTimestampShouldBeValid() {
            // Given: Current time (fixed instant)
            Instant currentTime = FIXED_INSTANT;

            // When/Then: Should be valid
            assertThat(declarationService.isValidDeclarationTimestamp(currentTime))
                .as("Current timestamp should be valid")
                .isTrue();
        }

        @Test
        @DisplayName("timestamp 1 hour ago should be valid")
        void timestamp1HourAgoShouldBeValid() {
            // Given: 1 hour ago
            Instant oneHourAgo = FIXED_INSTANT.minus(Duration.ofHours(1));

            // When/Then: Should be valid
            assertThat(declarationService.isValidDeclarationTimestamp(oneHourAgo))
                .as("Timestamp 1 hour ago should be valid")
                .isTrue();
        }

        @Test
        @DisplayName("timestamp 23 hours ago should be valid")
        void timestamp23HoursAgoShouldBeValid() {
            // Given: 23 hours ago (within 24-hour window)
            Instant twentyThreeHoursAgo = FIXED_INSTANT.minus(Duration.ofHours(23));

            // When/Then: Should be valid
            assertThat(declarationService.isValidDeclarationTimestamp(twentyThreeHoursAgo))
                .as("Timestamp 23 hours ago should be valid")
                .isTrue();
        }

        @Test
        @DisplayName("timestamp just under 24 hours ago should be valid")
        void timestampJustUnder24HoursShouldBeValid() {
            // Given: 23 hours, 59 minutes, 59 seconds ago
            Instant almostExpired = FIXED_INSTANT.minus(Duration.ofHours(23).plusMinutes(59).plusSeconds(59));

            // When/Then: Should still be valid
            assertThat(declarationService.isValidDeclarationTimestamp(almostExpired))
                .as("Timestamp 23:59:59 ago should be valid")
                .isTrue();
        }
    }

    // =========================================================================
    // TC-803-008: Invalid Timestamp - Future
    // Priority: P1 (Important)
    // AC Reference: AC-4
    // =========================================================================
    @Nested
    @DisplayName("TC-803-008: Invalid Timestamp - Future")
    class InvalidTimestampFuture {

        @Test
        @DisplayName("timestamp 1 minute in future should be invalid")
        void timestamp1MinuteInFutureShouldBeInvalid() {
            // Given: 1 minute in future
            Instant futureTime = FIXED_INSTANT.plus(Duration.ofMinutes(1));

            // When/Then: Should be invalid
            assertThat(declarationService.isValidDeclarationTimestamp(futureTime))
                .as("Future timestamp should be invalid (prevents manipulation)")
                .isFalse();
        }

        @Test
        @DisplayName("timestamp 1 hour in future should be invalid")
        void timestamp1HourInFutureShouldBeInvalid() {
            // Given: 1 hour in future
            Instant futureTime = FIXED_INSTANT.plus(Duration.ofHours(1));

            // When/Then: Should be invalid
            assertThat(declarationService.isValidDeclarationTimestamp(futureTime))
                .as("Future timestamp (1 hour ahead) should be invalid")
                .isFalse();
        }

        @Test
        @DisplayName("timestamp 1 second in future should be invalid")
        void timestamp1SecondInFutureShouldBeInvalid() {
            // Given: 1 second in future
            Instant futureTime = FIXED_INSTANT.plusSeconds(1);

            // When/Then: Should be invalid
            assertThat(declarationService.isValidDeclarationTimestamp(futureTime))
                .as("Future timestamp (1 second ahead) should be invalid")
                .isFalse();
        }
    }

    // =========================================================================
    // TC-803-009: Timestamp Older Than 24 Hours Rejected
    // Priority: P0 (Critical)
    // AC Reference: AC-4
    // =========================================================================
    @Nested
    @DisplayName("TC-803-009: Timestamp Older Than 24 Hours Rejected")
    class TimestampOlderThan24HoursRejected {

        @Test
        @DisplayName("timestamp exactly 24 hours ago should be valid (at boundary)")
        void timestampExactly24HoursAgoShouldBeValid() {
            // Given: Exactly 24 hours ago
            Instant exactly24HoursAgo = FIXED_INSTANT.minus(Duration.ofHours(24));

            // When/Then: Should be valid (at boundary - implementation uses isBefore, so exactly 24h is allowed)
            // Note: Per implementation, timestamp.isBefore(oldestAllowed) means exactly 24 hours is NOT before,
            // so it passes validation
            assertThat(declarationService.isValidDeclarationTimestamp(exactly24HoursAgo))
                .as("Timestamp exactly 24 hours ago should be at boundary (valid)")
                .isTrue();
        }

        @Test
        @DisplayName("timestamp 24 hours + 1 second ago should be invalid")
        void timestamp24HoursPlus1SecondShouldBeInvalid() {
            // Given: 24 hours + 1 second ago
            Instant justOver24Hours = FIXED_INSTANT.minus(Duration.ofHours(24).plusSeconds(1));

            // When/Then: Should be invalid
            assertThat(declarationService.isValidDeclarationTimestamp(justOver24Hours))
                .as("Timestamp 24h + 1s ago should be invalid (stale)")
                .isFalse();
        }

        @Test
        @DisplayName("timestamp 25 hours ago should be invalid")
        void timestamp25HoursAgoShouldBeInvalid() {
            // Given: 25 hours ago
            Instant twentyFiveHoursAgo = FIXED_INSTANT.minus(Duration.ofHours(25));

            // When/Then: Should be invalid
            assertThat(declarationService.isValidDeclarationTimestamp(twentyFiveHoursAgo))
                .as("Timestamp 25 hours ago should be invalid")
                .isFalse();
        }

        @Test
        @DisplayName("timestamp 48 hours ago should be invalid")
        void timestamp48HoursAgoShouldBeInvalid() {
            // Given: 48 hours ago
            Instant fortyEightHoursAgo = FIXED_INSTANT.minus(Duration.ofHours(48));

            // When/Then: Should be invalid
            assertThat(declarationService.isValidDeclarationTimestamp(fortyEightHoursAgo))
                .as("Timestamp 48 hours ago should be invalid")
                .isFalse();
        }

        @Test
        @DisplayName("timestamp 1 week ago should be invalid")
        void timestamp1WeekAgoShouldBeInvalid() {
            // Given: 1 week ago
            Instant oneWeekAgo = FIXED_INSTANT.minus(Duration.ofDays(7));

            // When/Then: Should be invalid
            assertThat(declarationService.isValidDeclarationTimestamp(oneWeekAgo))
                .as("Timestamp 1 week ago should be invalid")
                .isFalse();
        }
    }

    // =========================================================================
    // TC-803-010: Invalid Timestamp - Null
    // Priority: P1 (Important)
    // AC Reference: AC-4
    // =========================================================================
    @Nested
    @DisplayName("TC-803-010: Invalid Timestamp - Null")
    class InvalidTimestampNull {

        @Test
        @DisplayName("null timestamp should be invalid")
        void nullTimestampShouldBeInvalid() {
            // When/Then: Null should be invalid
            assertThat(declarationService.isValidDeclarationTimestamp(null))
                .as("Null timestamp should be invalid")
                .isFalse();
        }
    }

    // =========================================================================
    // TC-803-011: Cannot Submit Without Declaration
    // Priority: P0 (Critical)
    // AC Reference: AC-6
    // Note: This tests the validation logic that would block submission
    // =========================================================================
    @Nested
    @DisplayName("TC-803-011: Cannot Submit Without Declaration")
    class CannotSubmitWithoutDeclaration {

        @Test
        @DisplayName("null timestamp should fail validation (blocks submission)")
        void nullTimestampShouldFailValidation() {
            // When: Validate null timestamp
            boolean isValid = declarationService.isValidDeclarationTimestamp(null);

            // Then: Should be invalid (blocks submission)
            assertThat(isValid)
                .as("Null declaration timestamp should block submission")
                .isFalse();
        }

        @Test
        @DisplayName("DeclarationRecord should reject null acceptedAt")
        void declarationRecordShouldRejectNullAcceptedAt() {
            // When/Then: Should throw IllegalArgumentException
            assertThatThrownBy(() -> new DeclarationRecord(
                null,
                "validhash1234567890123456789012345678901234567890123456789012",
                "I declare..."
            ))
                .as("DeclarationRecord should reject null acceptedAt")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Acceptance timestamp is required");
        }

        @Test
        @DisplayName("valid declaration timestamp should pass validation (allows submission)")
        void validTimestampShouldPassValidation() {
            // Given: Valid timestamp (current time)
            Instant validTimestamp = FIXED_INSTANT;

            // When/Then: Should be valid (allows submission)
            assertThat(declarationService.isValidDeclarationTimestamp(validTimestamp))
                .as("Valid declaration timestamp should allow submission")
                .isTrue();
        }
    }

    // =========================================================================
    // TC-803-012: Successful Submission With Declaration
    // Priority: P0 (Critical)
    // AC Reference: AC-4, AC-6
    // =========================================================================
    @Nested
    @DisplayName("TC-803-012: Successful Submission With Declaration")
    class SuccessfulSubmissionWithDeclaration {

        @Test
        @DisplayName("complete declaration record should be created for valid submission")
        void completeDeclarationRecordShouldBeCreated() {
            // When: Create declaration record
            DeclarationRecord record = declarationService.recordDeclarationAcceptance();

            // Then: All fields should be valid
            assertThat(record.acceptedAt())
                .as("acceptedAt should be set")
                .isNotNull();

            assertThat(record.declarationTextHash())
                .as("declarationTextHash should be valid SHA-256")
                .hasSize(64);

            assertThat(declarationService.verifyDeclarationHash(record.declarationTextHash()))
                .as("Hash should verify against current declaration text")
                .isTrue();
        }
    }

    // =========================================================================
    // Hash Verification Tests (TC-803-004, TC-803-005, TC-803-006)
    // Priority: P1 (Important)
    // AC Reference: AC-3
    // =========================================================================
    @Nested
    @DisplayName("Hash Verification Tests")
    class HashVerificationTests {

        @Test
        @DisplayName("TC-803-004: SHA-256 hash should be generated correctly")
        void sha256HashShouldBeGeneratedCorrectly() {
            // When: Generate hash
            String hash = declarationService.generateDeclarationTextHash();

            // Then: Should be valid SHA-256 format
            assertThat(hash)
                .as("Hash should be 64 characters (SHA-256 hex)")
                .hasSize(64);

            assertThat(hash)
                .as("Hash should be lowercase hex")
                .matches("[a-f0-9]{64}");
        }

        @Test
        @DisplayName("TC-803-004: Same text should produce same hash")
        void sameTextShouldProduceSameHash() {
            // When: Generate hash multiple times
            String hash1 = declarationService.generateDeclarationTextHash();
            String hash2 = declarationService.generateDeclarationTextHash();
            String hash3 = declarationService.generateDeclarationTextHash();

            // Then: All should be identical
            assertThat(hash1)
                .as("Same text should produce consistent hash")
                .isEqualTo(hash2)
                .isEqualTo(hash3);
        }

        @Test
        @DisplayName("TC-803-005: Valid hash should verify successfully")
        void validHashShouldVerifySuccessfully() {
            // Given: Generate valid hash
            String validHash = declarationService.generateDeclarationTextHash();

            // When/Then: Should verify
            assertThat(declarationService.verifyDeclarationHash(validHash))
                .as("Valid hash should verify successfully")
                .isTrue();
        }

        @Test
        @DisplayName("TC-803-006: Null hash should fail verification")
        void nullHashShouldFailVerification() {
            assertThat(declarationService.verifyDeclarationHash(null))
                .as("Null hash should fail verification")
                .isFalse();
        }

        @Test
        @DisplayName("TC-803-006: Empty hash should fail verification")
        void emptyHashShouldFailVerification() {
            assertThat(declarationService.verifyDeclarationHash(""))
                .as("Empty hash should fail verification")
                .isFalse();
        }

        @Test
        @DisplayName("TC-803-006: Short hash should fail verification")
        void shortHashShouldFailVerification() {
            assertThat(declarationService.verifyDeclarationHash("too-short"))
                .as("Short hash should fail verification")
                .isFalse();
        }

        @Test
        @DisplayName("TC-803-006: Uppercase hash should fail verification")
        void uppercaseHashShouldFailVerification() {
            // Get valid hash and convert to uppercase
            String validHash = declarationService.generateDeclarationTextHash();
            String uppercaseHash = validHash.toUpperCase();

            assertThat(declarationService.verifyDeclarationHash(uppercaseHash))
                .as("Uppercase hash should fail verification (case-sensitive)")
                .isFalse();
        }

        @Test
        @DisplayName("TC-803-006: Wrong hash value should fail verification")
        void wrongHashValueShouldFailVerification() {
            // Valid length but wrong value
            String wrongHash = "0000000000000000000000000000000000000000000000000000000000000000";

            assertThat(declarationService.verifyDeclarationHash(wrongHash))
                .as("Wrong hash value should fail verification")
                .isFalse();
        }

        @Test
        @DisplayName("TC-803-006: Whitespace hash should fail verification")
        void whitespaceHashShouldFailVerification() {
            assertThat(declarationService.verifyDeclarationHash("   "))
                .as("Whitespace-only hash should fail verification")
                .isFalse();
        }
    }

    // =========================================================================
    // DeclarationRecord Validation Tests (TC-803-016, TC-803-017, TC-803-018)
    // Priority: P1/P2
    // =========================================================================
    @Nested
    @DisplayName("DeclarationRecord Validation Tests")
    class DeclarationRecordValidationTests {

        private static final String VALID_HASH = "e7b9f3c8a1d2e4f5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9";
        private static final String VALID_TEXT = "I declare...";

        @Test
        @DisplayName("TC-803-016: DeclarationRecord should reject null timestamp")
        void declarationRecordShouldRejectNullTimestamp() {
            assertThatThrownBy(() -> new DeclarationRecord(null, VALID_HASH, VALID_TEXT))
                .as("Should throw IllegalArgumentException for null timestamp")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Acceptance timestamp is required");
        }

        @Test
        @DisplayName("TC-803-017: DeclarationRecord should reject null hash")
        void declarationRecordShouldRejectNullHash() {
            assertThatThrownBy(() -> new DeclarationRecord(FIXED_INSTANT, null, VALID_TEXT))
                .as("Should throw IllegalArgumentException for null hash")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Declaration text hash is required");
        }

        @Test
        @DisplayName("TC-803-017: DeclarationRecord should reject empty hash")
        void declarationRecordShouldRejectEmptyHash() {
            assertThatThrownBy(() -> new DeclarationRecord(FIXED_INSTANT, "", VALID_TEXT))
                .as("Should throw IllegalArgumentException for empty hash")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Declaration text hash is required");
        }

        @Test
        @DisplayName("TC-803-017: DeclarationRecord should reject whitespace hash")
        void declarationRecordShouldRejectWhitespaceHash() {
            assertThatThrownBy(() -> new DeclarationRecord(FIXED_INSTANT, "   ", VALID_TEXT))
                .as("Should throw IllegalArgumentException for whitespace hash")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Declaration text hash is required");
        }

        @Test
        @DisplayName("TC-803-018: DeclarationRecord should reject null text")
        void declarationRecordShouldRejectNullText() {
            assertThatThrownBy(() -> new DeclarationRecord(FIXED_INSTANT, VALID_HASH, null))
                .as("Should throw IllegalArgumentException for null text")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Declaration text is required");
        }

        @Test
        @DisplayName("TC-803-018: DeclarationRecord should reject empty text")
        void declarationRecordShouldRejectEmptyText() {
            assertThatThrownBy(() -> new DeclarationRecord(FIXED_INSTANT, VALID_HASH, ""))
                .as("Should throw IllegalArgumentException for empty text")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Declaration text is required");
        }

        @Test
        @DisplayName("Valid DeclarationRecord should be created successfully")
        void validDeclarationRecordShouldBeCreatedSuccessfully() {
            // When: Create valid record
            DeclarationRecord record = new DeclarationRecord(FIXED_INSTANT, VALID_HASH, VALID_TEXT);

            // Then: Should be valid
            assertThat(record.acceptedAt()).isEqualTo(FIXED_INSTANT);
            assertThat(record.declarationTextHash()).isEqualTo(VALID_HASH);
            assertThat(record.declarationText()).isEqualTo(VALID_TEXT);
        }
    }

    // =========================================================================
    // Declaration Text Tests
    // =========================================================================
    @Nested
    @DisplayName("Declaration Text Tests")
    class DeclarationTextTests {

        @Test
        @DisplayName("getDeclarationText should return standard HMRC declaration")
        void getDeclarationTextShouldReturnStandardHmrcDeclaration() {
            // When: Get declaration text
            String declarationText = declarationService.getDeclarationText();

            // Then: Should be the standard HMRC text
            assertThat(declarationText)
                .as("Declaration text should be HMRC standard")
                .isEqualTo(HMRC_DECLARATION_TEXT);
        }

        @Test
        @DisplayName("declaration text should contain key legal phrases")
        void declarationTextShouldContainKeyLegalPhrases() {
            String text = declarationService.getDeclarationText();

            assertThat(text)
                .contains("I declare")
                .contains("correct and complete")
                .contains("best of my knowledge and belief")
                .contains("financial penalties")
                .contains("prosecution")
                .contains("false information");
        }
    }

    // =========================================================================
    // Boundary Condition Tests
    // =========================================================================
    @Nested
    @DisplayName("Boundary Condition Tests")
    class BoundaryConditionTests {

        @Test
        @DisplayName("timestamp 23 hours 59 minutes 59 seconds ago should be valid")
        void timestampJustBeforeBoundaryShouldBeValid() {
            Instant justBeforeBoundary = FIXED_INSTANT.minus(Duration.ofHours(24)).plusSeconds(1);

            assertThat(declarationService.isValidDeclarationTimestamp(justBeforeBoundary))
                .as("Timestamp 1 second before 24h boundary should be valid")
                .isTrue();
        }

        @Test
        @DisplayName("timestamp at exact current time should be valid")
        void timestampAtExactCurrentTimeShouldBeValid() {
            assertThat(declarationService.isValidDeclarationTimestamp(FIXED_INSTANT))
                .as("Timestamp at exact current time should be valid")
                .isTrue();
        }

        @Test
        @DisplayName("multiple DeclarationRecords should have independent timestamps")
        void multipleRecordsShouldHaveIndependentTimestamps() {
            // Create first record
            DeclarationRecord record1 = declarationService.recordDeclarationAcceptance();

            // Create new service with different time
            Clock laterClock = Clock.fixed(FIXED_INSTANT.plus(Duration.ofHours(1)), ZoneOffset.UTC);
            DeclarationService laterService = new DeclarationService(laterClock);
            DeclarationRecord record2 = laterService.recordDeclarationAcceptance();

            // Timestamps should be different
            assertThat(record1.acceptedAt())
                .as("Different clocks should produce different timestamps")
                .isNotEqualTo(record2.acceptedAt());
        }
    }

    // =========================================================================
    // Clock Injection Tests (for CI/CD determinism)
    // =========================================================================
    @Nested
    @DisplayName("Clock Injection Tests")
    class ClockInjectionTests {

        @Test
        @DisplayName("service should use injected clock for timestamps")
        void serviceShouldUseInjectedClockForTimestamps() {
            // Given: Specific fixed time
            Instant specificTime = Instant.parse("2025-06-15T14:30:00Z");
            Clock specificClock = Clock.fixed(specificTime, ZoneOffset.UTC);
            DeclarationService service = new DeclarationService(specificClock);

            // When: Record declaration
            DeclarationRecord record = service.recordDeclarationAcceptance();

            // Then: Should use the injected clock's time
            assertThat(record.acceptedAt())
                .as("Should use injected clock time")
                .isEqualTo(specificTime);
        }

        @Test
        @DisplayName("validation should use injected clock for comparison")
        void validationShouldUseInjectedClockForComparison() {
            // Given: Specific fixed time
            Instant currentTime = Instant.parse("2025-06-15T14:30:00Z");
            Clock specificClock = Clock.fixed(currentTime, ZoneOffset.UTC);
            DeclarationService service = new DeclarationService(specificClock);

            // 12 hours ago from the fixed time
            Instant twelveHoursAgo = currentTime.minus(Duration.ofHours(12));

            // When/Then: Should be valid relative to the fixed clock
            assertThat(service.isValidDeclarationTimestamp(twelveHoursAgo))
                .as("Validation should use injected clock")
                .isTrue();
        }

        @Test
        @DisplayName("default constructor should use system UTC clock")
        void defaultConstructorShouldUseSystemUtcClock() {
            // Given: Default service
            DeclarationService defaultService = new DeclarationService();

            // When: Record declaration
            Instant before = Instant.now();
            DeclarationRecord record = defaultService.recordDeclarationAcceptance();
            Instant after = Instant.now();

            // Then: Timestamp should be between before and after
            assertThat(record.acceptedAt())
                .as("Default constructor should use system time")
                .isBetween(before, after);
        }
    }
}
