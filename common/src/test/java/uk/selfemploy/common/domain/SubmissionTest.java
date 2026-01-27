package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Submission domain record.
 */
@DisplayName("Submission Domain Tests")
class SubmissionTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final TaxYear TAX_YEAR = TaxYear.of(2024);
    private static final BigDecimal TOTAL_INCOME = new BigDecimal("50000.00");
    private static final BigDecimal TOTAL_EXPENSES = new BigDecimal("10000.00");
    private static final String VALID_UTR = "1234567890";
    private static final String VALID_NINO = "AB123456A";

    @Nested
    @DisplayName("UTR/NINO Field Tests")
    class UtrNinoFieldTests {

        @Test
        @DisplayName("should create submission with UTR and NINO")
        void shouldCreateSubmissionWithUtrAndNino() {
            Submission submission = createSubmissionWithUtrNino(VALID_UTR, VALID_NINO);

            assertThat(submission.utr()).isEqualTo(VALID_UTR);
            assertThat(submission.nino()).isEqualTo(VALID_NINO);
        }

        @Test
        @DisplayName("should create submission with null UTR and NINO")
        void shouldCreateSubmissionWithNullUtrAndNino() {
            Submission submission = createSubmissionWithUtrNino(null, null);

            assertThat(submission.utr()).isNull();
            assertThat(submission.nino()).isNull();
        }

        @Test
        @DisplayName("should create annual submission with UTR and NINO")
        void shouldCreateAnnualSubmissionWithUtrAndNino() {
            Submission submission = Submission.createAnnualWithUtrAndNino(
                BUSINESS_ID, TAX_YEAR, TOTAL_INCOME, TOTAL_EXPENSES,
                VALID_UTR, VALID_NINO
            );

            assertThat(submission.utr()).isEqualTo(VALID_UTR);
            assertThat(submission.nino()).isEqualTo(VALID_NINO);
            assertThat(submission.type()).isEqualTo(SubmissionType.ANNUAL);
        }

        @Test
        @DisplayName("should create quarterly submission with UTR and NINO")
        void shouldCreateQuarterlySubmissionWithUtrAndNino() {
            Submission submission = Submission.createQuarterlyWithUtrAndNino(
                BUSINESS_ID, TAX_YEAR, Quarter.Q1, TOTAL_INCOME, TOTAL_EXPENSES,
                VALID_UTR, VALID_NINO
            );

            assertThat(submission.utr()).isEqualTo(VALID_UTR);
            assertThat(submission.nino()).isEqualTo(VALID_NINO);
            assertThat(submission.type()).isEqualTo(SubmissionType.QUARTERLY_Q1);
        }

        @Test
        @DisplayName("should preserve UTR and NINO when updating status")
        void shouldPreserveUtrNinoWhenUpdatingStatus() {
            Submission original = createSubmissionWithUtrNino(VALID_UTR, VALID_NINO);
            Submission accepted = original.withAccepted("HMRC-REF-123");

            assertThat(accepted.utr()).isEqualTo(VALID_UTR);
            assertThat(accepted.nino()).isEqualTo(VALID_NINO);
            assertThat(accepted.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        }

        @Test
        @DisplayName("should preserve UTR and NINO when marking submitted")
        void shouldPreserveUtrNinoWhenMarkingSubmitted() {
            Submission original = createSubmissionWithUtrNino(VALID_UTR, VALID_NINO);
            Submission submitted = original.withSubmitted();

            assertThat(submitted.utr()).isEqualTo(VALID_UTR);
            assertThat(submitted.nino()).isEqualTo(VALID_NINO);
            assertThat(submitted.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        }

        @Test
        @DisplayName("should preserve UTR and NINO when marking rejected")
        void shouldPreserveUtrNinoWhenMarkingRejected() {
            Submission original = createSubmissionWithUtrNino(VALID_UTR, VALID_NINO);
            Submission rejected = original.withRejected("Error message");

            assertThat(rejected.utr()).isEqualTo(VALID_UTR);
            assertThat(rejected.nino()).isEqualTo(VALID_NINO);
            assertThat(rejected.status()).isEqualTo(SubmissionStatus.REJECTED);
        }

        @Test
        @DisplayName("should create copy with new UTR and NINO")
        void shouldCreateCopyWithNewUtrAndNino() {
            Submission original = createSubmissionWithUtrNino(null, null);
            Submission updated = original.withUtrAndNino(VALID_UTR, VALID_NINO);

            assertThat(updated.utr()).isEqualTo(VALID_UTR);
            assertThat(updated.nino()).isEqualTo(VALID_NINO);
            // Original should be unchanged
            assertThat(original.utr()).isNull();
            assertThat(original.nino()).isNull();
        }
    }

    @Nested
    @DisplayName("Existing Functionality Preservation Tests")
    class ExistingFunctionalityTests {

        @Test
        @DisplayName("should preserve quarterly creation without UTR/NINO")
        void shouldPreserveQuarterlyCreationWithoutUtrNino() {
            Submission submission = Submission.createQuarterly(
                BUSINESS_ID, TAX_YEAR, Quarter.Q2, TOTAL_INCOME, TOTAL_EXPENSES
            );

            assertThat(submission.type()).isEqualTo(SubmissionType.QUARTERLY_Q2);
            assertThat(submission.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(submission.utr()).isNull();
            assertThat(submission.nino()).isNull();
        }

        @Test
        @DisplayName("should preserve annual creation without UTR/NINO")
        void shouldPreserveAnnualCreationWithoutUtrNino() {
            Submission submission = Submission.createAnnual(
                BUSINESS_ID, TAX_YEAR, TOTAL_INCOME, TOTAL_EXPENSES
            );

            assertThat(submission.type()).isEqualTo(SubmissionType.ANNUAL);
            assertThat(submission.businessId()).isEqualTo(BUSINESS_ID);
            assertThat(submission.utr()).isNull();
            assertThat(submission.nino()).isNull();
        }

        @Test
        @DisplayName("should preserve declaration fields")
        void shouldPreserveDeclarationFields() {
            Instant declarationTime = Instant.now();
            String declarationHash = "abc123hash";

            Submission submission = Submission.createAnnualWithDeclaration(
                BUSINESS_ID, TAX_YEAR, TOTAL_INCOME, TOTAL_EXPENSES,
                declarationTime, declarationHash
            );

            assertThat(submission.declarationAcceptedAt()).isEqualTo(declarationTime);
            assertThat(submission.declarationTextHash()).isEqualTo(declarationHash);
            assertThat(submission.hasDeclaration()).isTrue();
        }

        @Test
        @DisplayName("should calculate net profit correctly")
        void shouldCalculateNetProfitCorrectly() {
            Submission submission = Submission.createAnnual(
                BUSINESS_ID, TAX_YEAR, TOTAL_INCOME, TOTAL_EXPENSES
            );

            assertThat(submission.netProfit())
                .isEqualByComparingTo(TOTAL_INCOME.subtract(TOTAL_EXPENSES));
        }
    }

    private Submission createSubmissionWithUtrNino(String utr, String nino) {
        return new Submission(
            UUID.randomUUID(),
            BUSINESS_ID,
            SubmissionType.ANNUAL,
            TAX_YEAR,
            TAX_YEAR.startDate(),
            TAX_YEAR.endDate(),
            TOTAL_INCOME,
            TOTAL_EXPENSES,
            TOTAL_INCOME.subtract(TOTAL_EXPENSES),
            SubmissionStatus.PENDING,
            null,
            null,
            Instant.now(),
            Instant.now(),
            null,
            null,
            utr,
            nino
        );
    }
}
