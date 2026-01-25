package uk.selfemploy.core.hmrc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HmrcGuideContent record and nested GuideStep record.
 * Tests guide content structure and properties.
 *
 * Sprint 10A: SE-10A-002
 */
@DisplayName("HmrcGuideContent")
class HmrcGuideContentTest {

    @Nested
    @DisplayName("Guide Content Creation")
    class GuideContentCreation {

        @Test
        @DisplayName("should create guide content with all properties")
        void shouldCreateGuideContentWithAllProperties() {
            // Given
            List<HmrcGuideContent.GuideStep> steps = List.of(
                new HmrcGuideContent.GuideStep(1, "Step 1", "Description 1", "https://example.com/1"),
                new HmrcGuideContent.GuideStep(2, "Step 2", "Description 2", "https://example.com/2")
            );

            // When
            HmrcGuideContent guide = new HmrcGuideContent(
                "Guide Title",
                "This is the introduction.",
                steps,
                "https://www.gov.uk/guide"
            );

            // Then
            assertThat(guide.title()).isEqualTo("Guide Title");
            assertThat(guide.introduction()).isEqualTo("This is the introduction.");
            assertThat(guide.steps()).hasSize(2);
            assertThat(guide.helpUrl()).isEqualTo("https://www.gov.uk/guide");
        }

        @Test
        @DisplayName("should create guide content with empty steps")
        void shouldCreateGuideContentWithEmptySteps() {
            // When
            HmrcGuideContent guide = new HmrcGuideContent(
                "Empty Guide",
                "No steps yet.",
                List.of(),
                "https://example.com"
            );

            // Then
            assertThat(guide.steps()).isEmpty();
        }

        @Test
        @DisplayName("should allow null help URL")
        void shouldAllowNullHelpUrl() {
            // When
            HmrcGuideContent guide = new HmrcGuideContent(
                "Guide",
                "Introduction",
                List.of(),
                null
            );

            // Then
            assertThat(guide.helpUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("Guide Step Creation")
    class GuideStepCreation {

        @Test
        @DisplayName("should create guide step with all properties")
        void shouldCreateGuideStepWithAllProperties() {
            // When
            HmrcGuideContent.GuideStep step = new HmrcGuideContent.GuideStep(
                1,
                "First Step",
                "Do this first.",
                "https://example.com/step1"
            );

            // Then
            assertThat(step.number()).isEqualTo(1);
            assertThat(step.title()).isEqualTo("First Step");
            assertThat(step.description()).isEqualTo("Do this first.");
            assertThat(step.actionUrl()).isEqualTo("https://example.com/step1");
        }

        @Test
        @DisplayName("should allow null action URL")
        void shouldAllowNullActionUrl() {
            // When
            HmrcGuideContent.GuideStep step = new HmrcGuideContent.GuideStep(
                1, "Title", "Description", null
            );

            // Then
            assertThat(step.actionUrl()).isNull();
        }

        @Test
        @DisplayName("should allow zero step number")
        void shouldAllowZeroStepNumber() {
            // When
            HmrcGuideContent.GuideStep step = new HmrcGuideContent.GuideStep(
                0, "Pre-step", "Before you begin...", null
            );

            // Then
            assertThat(step.number()).isZero();
        }
    }

    @Nested
    @DisplayName("Registration Guide")
    class RegistrationGuide {

        @Test
        @DisplayName("should represent real registration guide structure")
        void shouldRepresentRealRegistrationGuide() {
            // When
            HmrcGuideContent guide = new HmrcGuideContent(
                "Register for Self Assessment",
                "If you're self-employed, you need to register for Self Assessment with HMRC to submit your tax returns.",
                List.of(
                    new HmrcGuideContent.GuideStep(
                        1,
                        "Check if you need to register",
                        "You must register if you earned more than GBP 1,000 from self-employment.",
                        "https://www.gov.uk/check-if-you-need-to-register-self-assessment"
                    ),
                    new HmrcGuideContent.GuideStep(
                        2,
                        "Register online",
                        "Use the HMRC online service to register as self-employed.",
                        "https://www.gov.uk/register-for-self-assessment/self-employed"
                    ),
                    new HmrcGuideContent.GuideStep(
                        3,
                        "Wait for your UTR",
                        "HMRC will send you a Unique Taxpayer Reference (UTR) by post within 10 working days.",
                        null
                    ),
                    new HmrcGuideContent.GuideStep(
                        4,
                        "Set up your Government Gateway",
                        "Create a Government Gateway account to access HMRC online services.",
                        "https://www.gov.uk/log-in-register-hmrc-online-services"
                    )
                ),
                "https://www.gov.uk/register-for-self-assessment"
            );

            // Then
            assertThat(guide.title()).contains("Self Assessment");
            assertThat(guide.steps()).hasSize(4);
            assertThat(guide.steps().get(0).number()).isEqualTo(1);
            assertThat(guide.steps().get(3).number()).isEqualTo(4);
            assertThat(guide.steps().get(2).actionUrl()).isNull(); // Step 3 has no action URL
        }
    }

    @Nested
    @DisplayName("UTR Guide")
    class UtrGuide {

        @Test
        @DisplayName("should represent real UTR guide structure")
        void shouldRepresentRealUtrGuide() {
            // When
            HmrcGuideContent guide = new HmrcGuideContent(
                "Your Unique Taxpayer Reference (UTR)",
                "Your UTR is a 10-digit number that identifies you for Self Assessment.",
                List.of(
                    new HmrcGuideContent.GuideStep(
                        1,
                        "Check your records",
                        "Your UTR is on previous Self Assessment tax returns.",
                        null
                    ),
                    new HmrcGuideContent.GuideStep(
                        2,
                        "Use your Personal Tax Account",
                        "Sign in to your HMRC Personal Tax Account to view your UTR online.",
                        "https://www.gov.uk/personal-tax-account"
                    ),
                    new HmrcGuideContent.GuideStep(
                        3,
                        "Request a reminder",
                        "If you can't find your UTR, you can request HMRC send you a reminder.",
                        "https://www.gov.uk/find-lost-utr-number"
                    )
                ),
                "https://www.gov.uk/find-lost-utr-number"
            );

            // Then
            assertThat(guide.title()).contains("UTR");
            assertThat(guide.introduction()).contains("10-digit");
            assertThat(guide.steps()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Record Equality")
    class RecordEquality {

        @Test
        @DisplayName("equal guide contents should be equal")
        void equalGuideContentsShouldBeEqual() {
            // Given
            List<HmrcGuideContent.GuideStep> steps = List.of(
                new HmrcGuideContent.GuideStep(1, "Step", "Desc", null)
            );

            HmrcGuideContent guide1 = new HmrcGuideContent(
                "Title", "Intro", steps, "https://example.com"
            );
            HmrcGuideContent guide2 = new HmrcGuideContent(
                "Title", "Intro", steps, "https://example.com"
            );

            // Then
            assertThat(guide1).isEqualTo(guide2);
            assertThat(guide1.hashCode()).isEqualTo(guide2.hashCode());
        }

        @Test
        @DisplayName("guide contents with different steps should not be equal")
        void guideContentsWithDifferentStepsShouldNotBeEqual() {
            // Given
            HmrcGuideContent guide1 = new HmrcGuideContent(
                "Title", "Intro",
                List.of(new HmrcGuideContent.GuideStep(1, "Step", "Desc", null)),
                null
            );
            HmrcGuideContent guide2 = new HmrcGuideContent(
                "Title", "Intro",
                List.of(new HmrcGuideContent.GuideStep(2, "Step", "Desc", null)),
                null
            );

            // Then
            assertThat(guide1).isNotEqualTo(guide2);
        }

        @Test
        @DisplayName("equal guide steps should be equal")
        void equalGuideStepsShouldBeEqual() {
            // Given
            HmrcGuideContent.GuideStep step1 = new HmrcGuideContent.GuideStep(
                1, "Title", "Desc", "https://example.com"
            );
            HmrcGuideContent.GuideStep step2 = new HmrcGuideContent.GuideStep(
                1, "Title", "Desc", "https://example.com"
            );

            // Then
            assertThat(step1).isEqualTo(step2);
            assertThat(step1.hashCode()).isEqualTo(step2.hashCode());
        }

        @Test
        @DisplayName("guide steps with different numbers should not be equal")
        void guideStepsWithDifferentNumbersShouldNotBeEqual() {
            // Given
            HmrcGuideContent.GuideStep step1 = new HmrcGuideContent.GuideStep(
                1, "Title", "Desc", null
            );
            HmrcGuideContent.GuideStep step2 = new HmrcGuideContent.GuideStep(
                2, "Title", "Desc", null
            );

            // Then
            assertThat(step1).isNotEqualTo(step2);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle many steps")
        void shouldHandleManySteps() {
            // Given
            List<HmrcGuideContent.GuideStep> manySteps = java.util.stream.IntStream.range(1, 101)
                .mapToObj(i -> new HmrcGuideContent.GuideStep(i, "Step " + i, "Description " + i, null))
                .toList();

            // When
            HmrcGuideContent guide = new HmrcGuideContent(
                "Long Guide", "Many steps...", manySteps, null
            );

            // Then
            assertThat(guide.steps()).hasSize(100);
            assertThat(guide.steps().get(0).number()).isEqualTo(1);
            assertThat(guide.steps().get(99).number()).isEqualTo(100);
        }

        @Test
        @DisplayName("should handle empty strings")
        void shouldHandleEmptyStrings() {
            // When
            HmrcGuideContent guide = new HmrcGuideContent("", "", List.of(), "");
            HmrcGuideContent.GuideStep step = new HmrcGuideContent.GuideStep(0, "", "", "");

            // Then
            assertThat(guide.title()).isEmpty();
            assertThat(step.title()).isEmpty();
        }

        @Test
        @DisplayName("should handle special characters")
        void shouldHandleSpecialCharacters() {
            // Given
            String specialText = "Register for HMRC's Self Assessment (SA100) - \"Make Tax Digital\"";

            // When
            HmrcGuideContent guide = new HmrcGuideContent(
                specialText,
                "You'll need to complete & submit form SA100.",
                List.of(),
                "https://www.gov.uk/self-assessment"
            );

            // Then
            assertThat(guide.title()).contains("SA100");
            assertThat(guide.introduction()).contains("&");
        }
    }
}
