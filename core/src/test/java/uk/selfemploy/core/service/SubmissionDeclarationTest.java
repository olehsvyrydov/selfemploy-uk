package uk.selfemploy.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Tests for SE-512: 6-Checkbox Submission Declaration.
 *
 * <p>Tests the SubmissionDeclaration model which tracks all 6 HMRC-required
 * declaration confirmations before annual Self Assessment submission.</p>
 *
 * @see uk.selfemploy.core.service.SubmissionDeclaration
 */
@DisplayName("SE-512: 6-Checkbox Submission Declaration")
class SubmissionDeclarationTest {

    private Clock fixedClock;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        fixedInstant = Instant.parse("2026-01-12T14:30:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
    }

    @Nested
    @DisplayName("Declaration Items")
    class DeclarationItems {

        @Test
        @DisplayName("should have exactly 6 declaration items")
        void shouldHaveExactly6DeclarationItems() {
            assertThat(SubmissionDeclaration.DECLARATION_COUNT).isEqualTo(6);
        }

        @Test
        @DisplayName("should define all 6 HMRC declaration keys")
        void shouldDefineAll6HmrcDeclarationKeys() {
            List<String> keys = SubmissionDeclaration.getDeclarationKeys();

            assertThat(keys).containsExactly(
                "accuracy_statement",
                "penalties_warning",
                "record_keeping",
                "calculation_verification",
                "legal_effect",
                "identity_confirmation"
            );
        }

        @Test
        @DisplayName("should return declaration text for each key")
        void shouldReturnDeclarationTextForEachKey() {
            assertThat(SubmissionDeclaration.getDeclarationText("accuracy_statement"))
                .contains("correct and complete")
                .contains("best of my knowledge and belief");

            assertThat(SubmissionDeclaration.getDeclarationText("penalties_warning"))
                .contains("financial penalties")
                .contains("prosecution");

            assertThat(SubmissionDeclaration.getDeclarationText("record_keeping"))
                .contains("kept records")
                .contains("5 years");

            assertThat(SubmissionDeclaration.getDeclarationText("calculation_verification"))
                .contains("reviewed the tax calculation")
                .contains("accurate");

            assertThat(SubmissionDeclaration.getDeclarationText("legal_effect"))
                .contains("legal act")
                .contains("signing a paper return");

            assertThat(SubmissionDeclaration.getDeclarationText("identity_confirmation"))
                .contains("I am the person")
                .contains("authorised");
        }

        @Test
        @DisplayName("should throw exception for unknown declaration key")
        void shouldThrowExceptionForUnknownKey() {
            assertThatThrownBy(() -> SubmissionDeclaration.getDeclarationText("unknown_key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown declaration key");
        }
    }

    @Nested
    @DisplayName("Declaration Builder")
    class DeclarationBuilder {

        @Test
        @DisplayName("should create empty builder with no confirmations")
        void shouldCreateEmptyBuilderWithNoConfirmations() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock);

            assertThat(builder.getConfirmedCount()).isZero();
            assertThat(builder.isComplete()).isFalse();
        }

        @Test
        @DisplayName("should confirm individual declarations with timestamp")
        void shouldConfirmIndividualDeclarationsWithTimestamp() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock);

            builder.confirm("accuracy_statement");

            assertThat(builder.isConfirmed("accuracy_statement")).isTrue();
            assertThat(builder.getConfirmedAt("accuracy_statement")).isEqualTo(fixedInstant);
            assertThat(builder.getConfirmedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should unconfirm individual declarations")
        void shouldUnconfirmIndividualDeclarations() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock);

            builder.confirm("accuracy_statement");
            builder.unconfirm("accuracy_statement");

            assertThat(builder.isConfirmed("accuracy_statement")).isFalse();
            assertThat(builder.getConfirmedAt("accuracy_statement")).isNull();
            assertThat(builder.getConfirmedCount()).isZero();
        }

        @Test
        @DisplayName("should track progress as confirmations are added")
        void shouldTrackProgressAsConfirmationsAreAdded() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock);

            assertThat(builder.getProgressText()).isEqualTo("0 of 6 confirmations completed");

            builder.confirm("accuracy_statement");
            assertThat(builder.getProgressText()).isEqualTo("1 of 6 confirmations completed");

            builder.confirm("penalties_warning");
            builder.confirm("record_keeping");
            assertThat(builder.getProgressText()).isEqualTo("3 of 6 confirmations completed");
        }

        @Test
        @DisplayName("should be complete when all 6 confirmations are made")
        void shouldBeCompleteWhenAll6ConfirmationsAreMade() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock);

            builder.confirm("accuracy_statement");
            builder.confirm("penalties_warning");
            builder.confirm("record_keeping");
            builder.confirm("calculation_verification");
            builder.confirm("legal_effect");
            assertThat(builder.isComplete()).isFalse();

            builder.confirm("identity_confirmation");
            assertThat(builder.isComplete()).isTrue();
            assertThat(builder.getConfirmedCount()).isEqualTo(6);
        }

        @Test
        @DisplayName("should throw exception when confirming unknown key")
        void shouldThrowExceptionWhenConfirmingUnknownKey() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock);

            assertThatThrownBy(() -> builder.confirm("unknown_key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown declaration key");
        }
    }

    @Nested
    @DisplayName("Build Declaration Record")
    class BuildDeclarationRecord {

        @Test
        @DisplayName("should build immutable record when complete")
        void shouldBuildImmutableRecordWhenComplete() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock)
                .forTaxYear("2025-26");

            // Confirm all 6
            builder.confirm("accuracy_statement");
            builder.confirm("penalties_warning");
            builder.confirm("record_keeping");
            builder.confirm("calculation_verification");
            builder.confirm("legal_effect");
            builder.confirm("identity_confirmation");

            SubmissionDeclaration declaration = builder.build();

            assertThat(declaration).isNotNull();
            assertThat(declaration.declarationId()).startsWith("DECL-");
            assertThat(declaration.completedAt()).isNotNull();
            assertThat(declaration.items()).hasSize(6);
        }

        @Test
        @DisplayName("should throw exception when building incomplete declaration")
        void shouldThrowExceptionWhenBuildingIncompleteDeclaration() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock)
                .forTaxYear("2025-26");

            builder.confirm("accuracy_statement");
            builder.confirm("penalties_warning");
            // Missing 4 confirmations

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("All 6 declarations must be confirmed");
        }

        @Test
        @DisplayName("should generate unique declaration ID")
        void shouldGenerateUniqueDeclarationId() {
            SubmissionDeclaration.Builder builder1 = SubmissionDeclaration.builder(fixedClock)
                .forTaxYear("2025-26");
            SubmissionDeclaration.Builder builder2 = SubmissionDeclaration.builder(fixedClock)
                .forTaxYear("2025-26");

            confirmAll(builder1);
            confirmAll(builder2);

            SubmissionDeclaration decl1 = builder1.build();
            SubmissionDeclaration decl2 = builder2.build();

            assertThat(decl1.declarationId()).isNotEqualTo(decl2.declarationId());
        }

        @Test
        @DisplayName("should include all declaration items with timestamps")
        void shouldIncludeAllDeclarationItemsWithTimestamps() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock)
                .forTaxYear("2025-26");
            confirmAll(builder);

            SubmissionDeclaration declaration = builder.build();

            assertThat(declaration.items()).allSatisfy(item -> {
                assertThat(item.confirmed()).isTrue();
                assertThat(item.confirmedAt()).isEqualTo(fixedInstant);
            });

            // Verify item ordering
            assertThat(declaration.items().get(0).key()).isEqualTo("accuracy_statement");
            assertThat(declaration.items().get(5).key()).isEqualTo("identity_confirmation");
        }

        private void confirmAll(SubmissionDeclaration.Builder builder) {
            builder.confirm("accuracy_statement");
            builder.confirm("penalties_warning");
            builder.confirm("record_keeping");
            builder.confirm("calculation_verification");
            builder.confirm("legal_effect");
            builder.confirm("identity_confirmation");
        }
    }

    @Nested
    @DisplayName("Declaration ID Format")
    class DeclarationIdFormat {

        @Test
        @DisplayName("should follow format DECL-YYYYMMDD-HHMMSS-XXXXX")
        void shouldFollowExpectedFormat() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock)
                .forTaxYear("2025-26");
            confirmAll(builder);

            SubmissionDeclaration declaration = builder.build();
            String id = declaration.declarationId();

            // Format: DECL-YYYYMMDD-HHMMSS-XXXXX
            assertThat(id).matches("DECL-\\d{8}-\\d{6}-[A-Z0-9]{5}");
            assertThat(id).startsWith("DECL-20260112-143000-");
        }

        private void confirmAll(SubmissionDeclaration.Builder builder) {
            builder.confirm("accuracy_statement");
            builder.confirm("penalties_warning");
            builder.confirm("record_keeping");
            builder.confirm("calculation_verification");
            builder.confirm("legal_effect");
            builder.confirm("identity_confirmation");
        }
    }

    @Nested
    @DisplayName("Declaration Item Record")
    class DeclarationItemRecord {

        @Test
        @DisplayName("should create valid declaration item")
        void shouldCreateValidDeclarationItem() {
            SubmissionDeclaration.DeclarationItem item = new SubmissionDeclaration.DeclarationItem(
                1,
                "accuracy_statement",
                true,
                fixedInstant
            );

            assertThat(item.index()).isEqualTo(1);
            assertThat(item.key()).isEqualTo("accuracy_statement");
            assertThat(item.confirmed()).isTrue();
            assertThat(item.confirmedAt()).isEqualTo(fixedInstant);
        }

        @Test
        @DisplayName("should allow unconfirmed items with null timestamp")
        void shouldAllowUnconfirmedItemsWithNullTimestamp() {
            SubmissionDeclaration.DeclarationItem item = new SubmissionDeclaration.DeclarationItem(
                1,
                "accuracy_statement",
                false,
                null
            );

            assertThat(item.confirmed()).isFalse();
            assertThat(item.confirmedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Tax Year Association")
    class TaxYearAssociation {

        @Test
        @DisplayName("should associate declaration with tax year")
        void shouldAssociateDeclarationWithTaxYear() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock)
                .forTaxYear("2025-26");

            confirmAll(builder);

            SubmissionDeclaration declaration = builder.build();

            assertThat(declaration.taxYear()).isEqualTo("2025-26");
        }

        @Test
        @DisplayName("should require tax year when building")
        void shouldRequireTaxYearWhenBuilding() {
            SubmissionDeclaration.Builder builder = SubmissionDeclaration.builder(fixedClock);
            // No tax year set
            confirmAll(builder);

            assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tax year must be set");
        }

        private void confirmAll(SubmissionDeclaration.Builder builder) {
            builder.confirm("accuracy_statement");
            builder.confirm("penalties_warning");
            builder.confirm("record_keeping");
            builder.confirm("calculation_verification");
            builder.confirm("legal_effect");
            builder.confirm("identity_confirmation");
        }
    }
}
