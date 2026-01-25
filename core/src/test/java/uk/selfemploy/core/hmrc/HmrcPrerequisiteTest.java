package uk.selfemploy.core.hmrc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HmrcPrerequisite record.
 * Tests record properties and withComplete method.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("HmrcPrerequisite")
class HmrcPrerequisiteTest {

    private static final String TEST_ID = "test_prerequisite";
    private static final String TEST_TITLE = "Test Prerequisite";
    private static final String TEST_DESCRIPTION = "This is a test prerequisite description.";
    private static final String TEST_URL = "https://www.gov.uk/test";

    @Nested
    @DisplayName("Record Creation")
    class RecordCreation {

        @Test
        @DisplayName("should create prerequisite with all properties")
        void shouldCreatePrerequisiteWithAllProperties() {
            // When
            HmrcPrerequisite prerequisite = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, false
            );

            // Then
            assertThat(prerequisite.id()).isEqualTo(TEST_ID);
            assertThat(prerequisite.title()).isEqualTo(TEST_TITLE);
            assertThat(prerequisite.description()).isEqualTo(TEST_DESCRIPTION);
            assertThat(prerequisite.helpUrl()).isEqualTo(TEST_URL);
            assertThat(prerequisite.isComplete()).isFalse();
        }

        @Test
        @DisplayName("should create completed prerequisite")
        void shouldCreateCompletedPrerequisite() {
            // When
            HmrcPrerequisite prerequisite = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, true
            );

            // Then
            assertThat(prerequisite.isComplete()).isTrue();
        }

        @Test
        @DisplayName("should allow null help URL")
        void shouldAllowNullHelpUrl() {
            // When
            HmrcPrerequisite prerequisite = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, null, false
            );

            // Then
            assertThat(prerequisite.helpUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("With Complete Method")
    class WithCompleteMethod {

        @Test
        @DisplayName("withComplete(true) should return completed prerequisite")
        void withCompleteTrueShouldReturnCompleted() {
            // Given
            HmrcPrerequisite original = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, false
            );

            // When
            HmrcPrerequisite completed = original.withComplete(true);

            // Then
            assertThat(completed.isComplete()).isTrue();
            assertThat(completed.id()).isEqualTo(original.id());
            assertThat(completed.title()).isEqualTo(original.title());
            assertThat(completed.description()).isEqualTo(original.description());
            assertThat(completed.helpUrl()).isEqualTo(original.helpUrl());
        }

        @Test
        @DisplayName("withComplete(false) should return incomplete prerequisite")
        void withCompleteFalseShouldReturnIncomplete() {
            // Given
            HmrcPrerequisite original = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, true
            );

            // When
            HmrcPrerequisite incomplete = original.withComplete(false);

            // Then
            assertThat(incomplete.isComplete()).isFalse();
            assertThat(incomplete.id()).isEqualTo(original.id());
        }

        @Test
        @DisplayName("withComplete should return new instance")
        void withCompleteShouldReturnNewInstance() {
            // Given
            HmrcPrerequisite original = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, false
            );

            // When
            HmrcPrerequisite modified = original.withComplete(true);

            // Then
            assertThat(modified).isNotSameAs(original);
            assertThat(original.isComplete()).isFalse(); // Original unchanged
            assertThat(modified.isComplete()).isTrue();
        }

        @Test
        @DisplayName("withComplete same value should still create new instance")
        void withCompleteSameValueShouldCreateNewInstance() {
            // Given
            HmrcPrerequisite original = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, true
            );

            // When
            HmrcPrerequisite same = original.withComplete(true);

            // Then
            assertThat(same).isNotSameAs(original);
            assertThat(same).isEqualTo(original); // But equal in content
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("equal prerequisites should be equal")
        void equalPrerequisitesShouldBeEqual() {
            // Given
            HmrcPrerequisite prereq1 = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, false
            );
            HmrcPrerequisite prereq2 = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, false
            );

            // Then
            assertThat(prereq1).isEqualTo(prereq2);
            assertThat(prereq1.hashCode()).isEqualTo(prereq2.hashCode());
        }

        @Test
        @DisplayName("prerequisites with different completion status should not be equal")
        void prerequisitesWithDifferentCompletionShouldNotBeEqual() {
            // Given
            HmrcPrerequisite incomplete = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, false
            );
            HmrcPrerequisite complete = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, TEST_DESCRIPTION, TEST_URL, true
            );

            // Then
            assertThat(incomplete).isNotEqualTo(complete);
        }

        @Test
        @DisplayName("prerequisites with different IDs should not be equal")
        void prerequisitesWithDifferentIdsShouldNotBeEqual() {
            // Given
            HmrcPrerequisite prereq1 = new HmrcPrerequisite(
                "id1", TEST_TITLE, TEST_DESCRIPTION, TEST_URL, false
            );
            HmrcPrerequisite prereq2 = new HmrcPrerequisite(
                "id2", TEST_TITLE, TEST_DESCRIPTION, TEST_URL, false
            );

            // Then
            assertThat(prereq1).isNotEqualTo(prereq2);
        }
    }

    @Nested
    @DisplayName("Real World Prerequisites")
    class RealWorldPrerequisites {

        @Test
        @DisplayName("should represent Self Assessment registration prerequisite")
        void shouldRepresentSelfAssessmentRegistration() {
            // When
            HmrcPrerequisite prerequisite = new HmrcPrerequisite(
                "self_assessment_registration",
                "Register for Self Assessment",
                "You must be registered for Self Assessment with HMRC before you can submit tax returns.",
                "https://www.gov.uk/register-for-self-assessment",
                false
            );

            // Then
            assertThat(prerequisite.id()).isEqualTo("self_assessment_registration");
            assertThat(prerequisite.title()).contains("Self Assessment");
            assertThat(prerequisite.helpUrl()).contains("gov.uk");
        }

        @Test
        @DisplayName("should represent UTR prerequisite")
        void shouldRepresentUtrPrerequisite() {
            // When
            HmrcPrerequisite prerequisite = new HmrcPrerequisite(
                "utr_number",
                "Unique Taxpayer Reference (UTR)",
                "You need your 10-digit UTR number. HMRC sends this by post within 10 days of registration.",
                "https://www.gov.uk/find-lost-utr-number",
                false
            );

            // Then
            assertThat(prerequisite.id()).isEqualTo("utr_number");
            assertThat(prerequisite.description()).contains("10-digit");
        }

        @Test
        @DisplayName("should represent Government Gateway prerequisite")
        void shouldRepresentGovernmentGatewayPrerequisite() {
            // When
            HmrcPrerequisite prerequisite = new HmrcPrerequisite(
                "government_gateway",
                "Government Gateway Account",
                "You need a Government Gateway account to authorize this app to submit on your behalf.",
                "https://www.gov.uk/log-in-register-hmrc-online-services",
                false
            );

            // Then
            assertThat(prerequisite.id()).isEqualTo("government_gateway");
            assertThat(prerequisite.title()).contains("Government Gateway");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty strings")
        void shouldHandleEmptyStrings() {
            // When
            HmrcPrerequisite prerequisite = new HmrcPrerequisite(
                "", "", "", "", false
            );

            // Then
            assertThat(prerequisite.id()).isEmpty();
            assertThat(prerequisite.title()).isEmpty();
            assertThat(prerequisite.description()).isEmpty();
            assertThat(prerequisite.helpUrl()).isEmpty();
        }

        @Test
        @DisplayName("should handle very long description")
        void shouldHandleVeryLongDescription() {
            // Given
            String longDescription = "This is a very detailed description. ".repeat(100);

            // When
            HmrcPrerequisite prerequisite = new HmrcPrerequisite(
                TEST_ID, TEST_TITLE, longDescription, TEST_URL, false
            );

            // Then
            assertThat(prerequisite.description()).hasSize(longDescription.length());
        }

        @Test
        @DisplayName("should handle special characters in text")
        void shouldHandleSpecialCharactersInText() {
            // Given
            String specialTitle = "Register for Self Assessment (SA100) - HMRC's Online Service";
            String specialDescription = "You'll need to complete form SA100 & submit it to HMRC.";

            // When
            HmrcPrerequisite prerequisite = new HmrcPrerequisite(
                "sa_100_registration",
                specialTitle,
                specialDescription,
                "https://www.gov.uk/self-assessment-tax-return",
                false
            );

            // Then
            assertThat(prerequisite.title()).contains("SA100");
            assertThat(prerequisite.description()).contains("&");
        }
    }
}
