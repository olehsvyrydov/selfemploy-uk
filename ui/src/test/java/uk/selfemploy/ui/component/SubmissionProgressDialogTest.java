package uk.selfemploy.ui.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.AutoOAuthSubmissionService.SubmissionProgress;
import uk.selfemploy.ui.service.AutoOAuthSubmissionService.SubmissionStage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SubmissionProgressDialog.
 *
 * <p>Tests the progress dialog that appears during HMRC submission,
 * showing the current stage of the submission process including OAuth
 * authentication when needed.</p>
 *
 * <p>Note: These are unit tests that don't require JavaFX initialization.
 * E2E tests would require the @Tag("e2e") annotation.</p>
 */
@DisplayName("SubmissionProgressDialog")
class SubmissionProgressDialogTest {

    @Nested
    @DisplayName("Progress Messages")
    class ProgressMessagesTests {

        @Test
        @DisplayName("TC-1: VALIDATING stage should have appropriate message")
        void validatingStageShouldHaveMessage() {
            // Given
            SubmissionProgress progress = SubmissionProgress.of(SubmissionStage.VALIDATING);

            // Then
            assertThat(progress.stage()).isEqualTo(SubmissionStage.VALIDATING);
            assertThat(progress.message()).isNotBlank();
            assertThat(progress.message()).containsIgnoringCase("validat");
        }

        @Test
        @DisplayName("TC-2: AUTHENTICATING stage should have appropriate message")
        void authenticatingStageShouldHaveMessage() {
            // Given
            SubmissionProgress progress = SubmissionProgress.of(SubmissionStage.AUTHENTICATING);

            // Then
            assertThat(progress.stage()).isEqualTo(SubmissionStage.AUTHENTICATING);
            assertThat(progress.message()).isNotBlank();
            assertThat(progress.message()).containsIgnoringCase("authenticat");
        }

        @Test
        @DisplayName("TC-3: SUBMITTING stage should have appropriate message")
        void submittingStageShouldHaveMessage() {
            // Given
            SubmissionProgress progress = SubmissionProgress.of(SubmissionStage.SUBMITTING);

            // Then
            assertThat(progress.stage()).isEqualTo(SubmissionStage.SUBMITTING);
            assertThat(progress.message()).isNotBlank();
            assertThat(progress.message()).containsIgnoringCase("submit");
        }

        @Test
        @DisplayName("TC-4: COMPLETE stage should have appropriate message")
        void completeStageShouldHaveMessage() {
            // Given
            SubmissionProgress progress = SubmissionProgress.of(SubmissionStage.COMPLETE);

            // Then
            assertThat(progress.stage()).isEqualTo(SubmissionStage.COMPLETE);
            assertThat(progress.message()).isNotBlank();
            assertThat(progress.message()).containsIgnoringCase("complete");
        }

        @Test
        @DisplayName("TC-5: FAILED stage should have appropriate message")
        void failedStageShouldHaveMessage() {
            // Given
            SubmissionProgress progress = SubmissionProgress.of(SubmissionStage.FAILED);

            // Then
            assertThat(progress.stage()).isEqualTo(SubmissionStage.FAILED);
            assertThat(progress.message()).isNotBlank();
            assertThat(progress.message()).containsIgnoringCase("failed");
        }

        @Test
        @DisplayName("TC-6: custom message should override default")
        void customMessageShouldOverrideDefault() {
            // Given
            String customMessage = "Re-authenticating with HMRC...";
            SubmissionProgress progress = SubmissionProgress.of(SubmissionStage.AUTHENTICATING, customMessage);

            // Then
            assertThat(progress.stage()).isEqualTo(SubmissionStage.AUTHENTICATING);
            assertThat(progress.message()).isEqualTo(customMessage);
        }
    }

    @Nested
    @DisplayName("Stage Default Messages")
    class StageDefaultMessagesTests {

        @Test
        @DisplayName("TC-7: all stages should have non-null default messages")
        void allStagesShouldHaveDefaultMessages() {
            for (SubmissionStage stage : SubmissionStage.values()) {
                assertThat(stage.getDefaultMessage())
                        .as("Stage %s should have a default message", stage)
                        .isNotNull()
                        .isNotBlank();
            }
        }

        @Test
        @DisplayName("TC-8: stage values should include all expected stages")
        void stageValuesShouldIncludeAllExpected() {
            assertThat(SubmissionStage.values())
                    .containsExactlyInAnyOrder(
                            SubmissionStage.VALIDATING,
                            SubmissionStage.AUTHENTICATING,
                            SubmissionStage.SUBMITTING,
                            SubmissionStage.COMPLETE,
                            SubmissionStage.FAILED
                    );
        }
    }

    @Nested
    @DisplayName("SubmissionProgress Record")
    class SubmissionProgressRecordTests {

        @Test
        @DisplayName("TC-9: SubmissionProgress factory method should create valid instance")
        void factoryMethodShouldCreateValidInstance() {
            // Given
            SubmissionStage stage = SubmissionStage.SUBMITTING;

            // When
            SubmissionProgress progress = SubmissionProgress.of(stage);

            // Then
            assertThat(progress).isNotNull();
            assertThat(progress.stage()).isEqualTo(stage);
            assertThat(progress.message()).isEqualTo(stage.getDefaultMessage());
        }

        @Test
        @DisplayName("TC-10: SubmissionProgress with custom message should store both")
        void withCustomMessageShouldStoreBoth() {
            // Given
            SubmissionStage stage = SubmissionStage.AUTHENTICATING;
            String customMessage = "Opening browser for HMRC login...";

            // When
            SubmissionProgress progress = SubmissionProgress.of(stage, customMessage);

            // Then
            assertThat(progress).isNotNull();
            assertThat(progress.stage()).isEqualTo(stage);
            assertThat(progress.message()).isEqualTo(customMessage);
        }

        @Test
        @DisplayName("TC-11: SubmissionProgress record equality")
        void recordEqualityShouldWork() {
            // Given
            SubmissionProgress progress1 = SubmissionProgress.of(SubmissionStage.SUBMITTING);
            SubmissionProgress progress2 = SubmissionProgress.of(SubmissionStage.SUBMITTING);
            SubmissionProgress progress3 = SubmissionProgress.of(SubmissionStage.COMPLETE);

            // Then
            assertThat(progress1).isEqualTo(progress2);
            assertThat(progress1).isNotEqualTo(progress3);
        }
    }
}
