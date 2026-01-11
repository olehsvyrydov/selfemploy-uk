package uk.selfemploy.common.legal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Disclaimers constants class.
 *
 * SE-509: Enhanced Disclaimers
 *
 * Tests verify that all legal disclaimer texts are properly defined
 * and contain the required content as specified by /alex legal review.
 */
@DisplayName("Disclaimers Constants Tests")
class DisclaimersTest {

    @Nested
    @DisplayName("Tax Summary Disclaimer Tests")
    class TaxSummaryDisclaimerTests {

        @Test
        @DisplayName("should have tax summary disclaimer defined")
        void shouldHaveTaxSummaryDisclaimerDefined() {
            assertThat(Disclaimers.TAX_SUMMARY_DISCLAIMER)
                .isNotNull()
                .isNotBlank();
        }

        @Test
        @DisplayName("tax summary disclaimer should mention estimate")
        void taxSummaryDisclaimerShouldMentionEstimate() {
            assertThat(Disclaimers.TAX_SUMMARY_DISCLAIMER)
                .containsIgnoringCase("estimate");
        }

        @Test
        @DisplayName("tax summary disclaimer should mention accountant verification")
        void taxSummaryDisclaimerShouldMentionAccountantVerification() {
            assertThat(Disclaimers.TAX_SUMMARY_DISCLAIMER)
                .containsIgnoringCase("accountant");
        }

        @Test
        @DisplayName("tax summary disclaimer should mention tax advisor")
        void taxSummaryDisclaimerShouldMentionTaxAdvisor() {
            assertThat(Disclaimers.TAX_SUMMARY_DISCLAIMER)
                .containsIgnoringCase("tax advisor");
        }

        @Test
        @DisplayName("tax summary disclaimer should contain IMPORTANT label")
        void taxSummaryDisclaimerShouldContainImportantLabel() {
            assertThat(Disclaimers.TAX_SUMMARY_DISCLAIMER)
                .startsWith("IMPORTANT:");
        }
    }

    @Nested
    @DisplayName("HMRC Submission Disclaimer Tests")
    class HmrcSubmissionDisclaimerTests {

        @Test
        @DisplayName("should have HMRC submission disclaimer defined")
        void shouldHaveHmrcSubmissionDisclaimerDefined() {
            assertThat(Disclaimers.HMRC_SUBMISSION_DISCLAIMER)
                .isNotNull()
                .isNotBlank();
        }

        @Test
        @DisplayName("HMRC submission disclaimer should mention accuracy responsibility")
        void hmrcSubmissionDisclaimerShouldMentionAccuracyResponsibility() {
            assertThat(Disclaimers.HMRC_SUBMISSION_DISCLAIMER)
                .containsIgnoringCase("responsible")
                .containsIgnoringCase("accuracy");
        }

        @Test
        @DisplayName("HMRC submission disclaimer should mention data confirmation")
        void hmrcSubmissionDisclaimerShouldMentionDataConfirmation() {
            assertThat(Disclaimers.HMRC_SUBMISSION_DISCLAIMER)
                .containsIgnoringCase("confirm");
        }

        @Test
        @DisplayName("HMRC submission disclaimer should mention HMRC")
        void hmrcSubmissionDisclaimerShouldMentionHmrc() {
            assertThat(Disclaimers.HMRC_SUBMISSION_DISCLAIMER)
                .containsIgnoringCase("HMRC");
        }
    }

    @Nested
    @DisplayName("PDF Confirmation Disclaimer Tests")
    class PdfConfirmationDisclaimerTests {

        @Test
        @DisplayName("should have PDF confirmation disclaimer defined")
        void shouldHavePdfConfirmationDisclaimerDefined() {
            assertThat(Disclaimers.PDF_CONFIRMATION_DISCLAIMER)
                .isNotNull()
                .isNotBlank();
        }

        @Test
        @DisplayName("PDF disclaimer should state software does not constitute professional advice")
        void pdfDisclaimerShouldStateNoProAdvice() {
            assertThat(Disclaimers.PDF_CONFIRMATION_DISCLAIMER)
                .containsIgnoringCase("does not constitute")
                .containsIgnoringCase("professional");
        }

        @Test
        @DisplayName("PDF disclaimer should mention tax advice")
        void pdfDisclaimerShouldMentionTaxAdvice() {
            assertThat(Disclaimers.PDF_CONFIRMATION_DISCLAIMER)
                .containsIgnoringCase("tax");
        }

        @Test
        @DisplayName("PDF disclaimer should mention legal advice")
        void pdfDisclaimerShouldMentionLegalAdvice() {
            assertThat(Disclaimers.PDF_CONFIRMATION_DISCLAIMER)
                .containsIgnoringCase("legal");
        }

        @Test
        @DisplayName("PDF disclaimer should mention financial advice")
        void pdfDisclaimerShouldMentionFinancialAdvice() {
            assertThat(Disclaimers.PDF_CONFIRMATION_DISCLAIMER)
                .containsIgnoringCase("financial");
        }

        @Test
        @DisplayName("PDF disclaimer should mention qualified professional consultation")
        void pdfDisclaimerShouldMentionQualifiedProfessional() {
            assertThat(Disclaimers.PDF_CONFIRMATION_DISCLAIMER)
                .containsIgnoringCase("qualified professional");
        }

        @Test
        @DisplayName("PDF disclaimer should contain DISCLAIMER label")
        void pdfDisclaimerShouldContainDisclaimerLabel() {
            assertThat(Disclaimers.PDF_CONFIRMATION_DISCLAIMER)
                .startsWith("DISCLAIMER:");
        }
    }

    @Nested
    @DisplayName("Disclaimer ID Tests")
    class DisclaimerIdTests {

        @Test
        @DisplayName("should have tax summary disclaimer ID")
        void shouldHaveTaxSummaryDisclaimerId() {
            assertThat(Disclaimers.TAX_SUMMARY_ID)
                .isNotNull()
                .isNotBlank()
                .isEqualTo("TAX_SUMMARY_V1");
        }

        @Test
        @DisplayName("should have HMRC submission disclaimer ID")
        void shouldHaveHmrcSubmissionDisclaimerId() {
            assertThat(Disclaimers.HMRC_SUBMISSION_ID)
                .isNotNull()
                .isNotBlank()
                .isEqualTo("HMRC_SUBMISSION_V1");
        }

        @Test
        @DisplayName("should have PDF confirmation disclaimer ID")
        void shouldHavePdfConfirmationDisclaimerId() {
            assertThat(Disclaimers.PDF_CONFIRMATION_ID)
                .isNotNull()
                .isNotBlank()
                .isEqualTo("PDF_CONFIRMATION_V1");
        }
    }

    @Nested
    @DisplayName("Disclaimer Consistency Tests")
    class DisclaimerConsistencyTests {

        @Test
        @DisplayName("all disclaimers should be unique")
        void allDisclaimersShouldBeUnique() {
            assertThat(Disclaimers.TAX_SUMMARY_DISCLAIMER)
                .isNotEqualTo(Disclaimers.HMRC_SUBMISSION_DISCLAIMER)
                .isNotEqualTo(Disclaimers.PDF_CONFIRMATION_DISCLAIMER);

            assertThat(Disclaimers.HMRC_SUBMISSION_DISCLAIMER)
                .isNotEqualTo(Disclaimers.PDF_CONFIRMATION_DISCLAIMER);
        }

        @Test
        @DisplayName("all disclaimer IDs should be unique")
        void allDisclaimerIdsShouldBeUnique() {
            assertThat(Disclaimers.TAX_SUMMARY_ID)
                .isNotEqualTo(Disclaimers.HMRC_SUBMISSION_ID)
                .isNotEqualTo(Disclaimers.PDF_CONFIRMATION_ID);

            assertThat(Disclaimers.HMRC_SUBMISSION_ID)
                .isNotEqualTo(Disclaimers.PDF_CONFIRMATION_ID);
        }

        @Test
        @DisplayName("disclaimer class should not be instantiable")
        void disclaimerClassShouldNotBeInstantiable() {
            // The class should only have a private constructor
            // This is checked by trying to access the constructor count
            assertThat(Disclaimers.class.getDeclaredConstructors())
                .hasSize(1);

            assertThat(Disclaimers.class.getDeclaredConstructors()[0].canAccess(null))
                .isFalse();
        }
    }
}
