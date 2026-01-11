package uk.selfemploy.common.legal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the Disclaimers class.
 *
 * <p>SE-509: Enhanced Disclaimers
 *
 * <p>These tests verify the P0 (Critical) test cases defined by /rob:
 * <ul>
 *     <li>TC-509-001: Tax summary disclaimer visible on load (constant defined)</li>
 *     <li>TC-509-002: Disclaimer text matches legal specification</li>
 *     <li>TC-509-006: HMRC submission disclaimer visible at Step 3 (constant defined)</li>
 *     <li>TC-509-010: PDF confirmation includes disclaimer (constant defined)</li>
 *     <li>TC-509-011: PDF disclaimer text is correct</li>
 *     <li>TC-509-013: Disclaimers class contains all three constants</li>
 *     <li>TC-509-014: Version identifiers exist</li>
 * </ul>
 *
 * <p>Test Author: /adam (Senior E2E Test Automation Engineer)
 * <p>Sprint: 6
 *
 * @see Disclaimers
 * @see <a href="docs/sprints/sprint-6/testing/rob-qa-SE-509-SE-802-SE-703.md">QA Test Specifications</a>
 */
@DisplayName("SE-509: Enhanced Disclaimers Integration Tests")
@Tag("integration")
@Tag("se-509")
@Tag("disclaimers")
class DisclaimersIntegrationTest {

    // ==================== TC-509-001: Tax Summary Disclaimer Visibility ====================

    @Nested
    @DisplayName("TC-509-001: Tax Summary Disclaimer Visibility")
    class TaxSummaryDisclaimerVisibility {

        @Test
        @DisplayName("TC-509-001-01: TAX_SUMMARY_DISCLAIMER constant is defined and accessible")
        void taxSummaryDisclaimerConstantIsDefined() {
            // Given/When
            String disclaimer = Disclaimers.TAX_SUMMARY_DISCLAIMER;

            // Then - The constant should be defined and accessible
            assertThat(disclaimer)
                .as("Tax summary disclaimer constant should be defined")
                .isNotNull()
                .isNotBlank();
        }

        @Test
        @DisplayName("TC-509-001-02: Tax summary disclaimer is static final constant")
        void taxSummaryDisclaimerIsStaticFinal() {
            // Given/When - Access the constant directly
            String constant1 = Disclaimers.TAX_SUMMARY_DISCLAIMER;
            String constant2 = Disclaimers.TAX_SUMMARY_DISCLAIMER;

            // Then - Both accesses return the same immutable value
            assertThat(constant1)
                .as("Static constant should return same instance")
                .isSameAs(constant2);
        }

        @Test
        @DisplayName("TC-509-001-03: Tax summary disclaimer has meaningful length")
        void taxSummaryDisclaimerHasMeaningfulLength() {
            // Given/When
            String disclaimer = Disclaimers.TAX_SUMMARY_DISCLAIMER;

            // Then - Should be substantial legal text
            assertThat(disclaimer.length())
                .as("Disclaimer should be substantial legal text")
                .isGreaterThan(50);
        }
    }

    // ==================== TC-509-002: Disclaimer Text Matches Legal Specification ====================

    @Nested
    @DisplayName("TC-509-002: Tax Summary Disclaimer Text Content")
    class TaxSummaryDisclaimerTextContent {

        @Test
        @DisplayName("TC-509-002-01: Tax summary disclaimer contains 'IMPORTANT' label")
        void taxSummaryDisclaimerContainsImportantLabel() {
            // Given/When
            String disclaimer = Disclaimers.TAX_SUMMARY_DISCLAIMER;

            // Then
            assertThat(disclaimer)
                .as("Disclaimer should start with IMPORTANT label")
                .startsWith("IMPORTANT:");
        }

        @Test
        @DisplayName("TC-509-002-02: Tax summary disclaimer mentions 'estimate only'")
        void taxSummaryDisclaimerMentionsEstimateOnly() {
            // Given/When
            String disclaimer = Disclaimers.TAX_SUMMARY_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention estimate")
                .contains("estimate");
        }

        @Test
        @DisplayName("TC-509-002-03: Tax summary disclaimer mentions qualified accountant")
        void taxSummaryDisclaimerMentionsQualifiedAccountant() {
            // Given/When
            String disclaimer = Disclaimers.TAX_SUMMARY_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention accountant")
                .contains("accountant");
        }

        @Test
        @DisplayName("TC-509-002-04: Tax summary disclaimer mentions tax advisor")
        void taxSummaryDisclaimerMentionsTaxAdvisor() {
            // Given/When
            String disclaimer = Disclaimers.TAX_SUMMARY_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention tax advisor")
                .contains("tax advisor");
        }

        @Test
        @DisplayName("TC-509-002-05: Tax summary disclaimer matches exact legal specification")
        void taxSummaryDisclaimerMatchesExactSpecification() {
            // Given - Expected text as approved by /alex
            String expectedText =
                "IMPORTANT: This tax calculation is an estimate only and may not reflect your actual tax liability. " +
                "You should verify all figures with a qualified accountant or tax advisor before making any financial decisions.";

            // When
            String actualText = Disclaimers.TAX_SUMMARY_DISCLAIMER;

            // Then
            assertThat(actualText)
                .as("Disclaimer must match exact legal specification")
                .isEqualTo(expectedText);
        }
    }

    // ==================== TC-509-006: HMRC Submission Disclaimer Visible at Step 3 ====================

    @Nested
    @DisplayName("TC-509-006: HMRC Submission Disclaimer Display")
    class HmrcSubmissionDisclaimerDisplay {

        @Test
        @DisplayName("TC-509-006-01: HMRC_SUBMISSION_DISCLAIMER constant is defined and accessible")
        void hmrcSubmissionDisclaimerConstantIsDefined() {
            // Given/When
            String disclaimer = Disclaimers.HMRC_SUBMISSION_DISCLAIMER;

            // Then
            assertThat(disclaimer)
                .as("HMRC submission disclaimer constant should be defined")
                .isNotNull()
                .isNotBlank();
        }

        @Test
        @DisplayName("TC-509-006-02: HMRC submission disclaimer mentions responsibility")
        void hmrcSubmissionDisclaimerMentionsResponsibility() {
            // Given/When
            String disclaimer = Disclaimers.HMRC_SUBMISSION_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention responsibility")
                .contains("responsible");
        }

        @Test
        @DisplayName("TC-509-006-03: HMRC submission disclaimer mentions accuracy")
        void hmrcSubmissionDisclaimerMentionsAccuracy() {
            // Given/When
            String disclaimer = Disclaimers.HMRC_SUBMISSION_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention accuracy")
                .contains("accurate");
        }

        @Test
        @DisplayName("TC-509-006-04: HMRC submission disclaimer mentions confirm/confirming")
        void hmrcSubmissionDisclaimerMentionsConfirm() {
            // Given/When
            String disclaimer = Disclaimers.HMRC_SUBMISSION_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention confirm")
                .contains("confirm");
        }

        @Test
        @DisplayName("TC-509-006-05: HMRC submission disclaimer references HMRC")
        void hmrcSubmissionDisclaimerReferencesHmrc() {
            // Given/When
            String disclaimer = Disclaimers.HMRC_SUBMISSION_DISCLAIMER;

            // Then
            assertThat(disclaimer)
                .as("Disclaimer should reference HMRC")
                .containsIgnoringCase("HMRC");
        }

        @Test
        @DisplayName("TC-509-006-06: HMRC submission disclaimer matches exact legal specification")
        void hmrcSubmissionDisclaimerMatchesExactSpecification() {
            // Given - Expected text as approved by /alex
            String expectedText =
                "By submitting this data to HMRC, you confirm that the information provided is accurate and complete " +
                "to the best of your knowledge. You are solely responsible for the accuracy of all submitted data.";

            // When
            String actualText = Disclaimers.HMRC_SUBMISSION_DISCLAIMER;

            // Then
            assertThat(actualText)
                .as("Disclaimer must match exact legal specification")
                .isEqualTo(expectedText);
        }
    }

    // ==================== TC-509-010: PDF Confirmation Includes Disclaimer ====================

    @Nested
    @DisplayName("TC-509-010: PDF Confirmation Disclaimer Presence")
    class PdfConfirmationDisclaimerPresence {

        @Test
        @DisplayName("TC-509-010-01: PDF_CONFIRMATION_DISCLAIMER constant is defined and accessible")
        void pdfConfirmationDisclaimerConstantIsDefined() {
            // Given/When
            String disclaimer = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then
            assertThat(disclaimer)
                .as("PDF confirmation disclaimer constant should be defined")
                .isNotNull()
                .isNotBlank();
        }

        @Test
        @DisplayName("TC-509-010-02: PDF disclaimer starts with 'DISCLAIMER' label")
        void pdfDisclaimerStartsWithDisclaimerLabel() {
            // Given/When
            String disclaimer = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then
            assertThat(disclaimer)
                .as("PDF disclaimer should start with DISCLAIMER label")
                .startsWith("DISCLAIMER:");
        }
    }

    // ==================== TC-509-011: PDF Disclaimer Text Is Correct ====================

    @Nested
    @DisplayName("TC-509-011: PDF Confirmation Disclaimer Text Content")
    class PdfConfirmationDisclaimerTextContent {

        @Test
        @DisplayName("TC-509-011-01: PDF disclaimer states software does not constitute professional advice")
        void pdfDisclaimerStatesNoProAdvice() {
            // Given/When
            String disclaimer = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should state software does not constitute professional advice")
                .contains("does not constitute");
        }

        @Test
        @DisplayName("TC-509-011-02: PDF disclaimer mentions professional advice")
        void pdfDisclaimerMentionsProfessionalAdvice() {
            // Given/When
            String disclaimer = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention professional")
                .contains("professional");
        }

        @Test
        @DisplayName("TC-509-011-03: PDF disclaimer mentions tax advice")
        void pdfDisclaimerMentionsTaxAdvice() {
            // Given/When
            String disclaimer = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention tax")
                .contains("tax");
        }

        @Test
        @DisplayName("TC-509-011-04: PDF disclaimer mentions legal advice")
        void pdfDisclaimerMentionsLegalAdvice() {
            // Given/When
            String disclaimer = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention legal")
                .contains("legal");
        }

        @Test
        @DisplayName("TC-509-011-05: PDF disclaimer mentions financial advice")
        void pdfDisclaimerMentionsFinancialAdvice() {
            // Given/When
            String disclaimer = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention financial")
                .contains("financial");
        }

        @Test
        @DisplayName("TC-509-011-06: PDF disclaimer mentions qualified professional")
        void pdfDisclaimerMentionsQualifiedProfessional() {
            // Given/When
            String disclaimer = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then
            assertThat(disclaimer.toLowerCase())
                .as("Disclaimer should mention qualified professional")
                .contains("qualified professional");
        }

        @Test
        @DisplayName("TC-509-011-07: PDF disclaimer matches exact legal specification")
        void pdfDisclaimerMatchesExactSpecification() {
            // Given - Expected text as approved by /alex
            String expectedText =
                "DISCLAIMER: This software does not constitute professional tax, legal, or financial advice. " +
                "The calculations shown are estimates based on the data you have provided. " +
                "Always consult a qualified professional for advice specific to your circumstances.";

            // When
            String actualText = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then
            assertThat(actualText)
                .as("Disclaimer must match exact legal specification")
                .isEqualTo(expectedText);
        }
    }

    // ==================== TC-509-013: Disclaimers Class Contains All Three Constants ====================

    @Nested
    @DisplayName("TC-509-013: Disclaimer Centralization Verification")
    class DisclaimerCentralizationVerification {

        @Test
        @DisplayName("TC-509-013-01: All three disclaimer constants are defined in Disclaimers class")
        void allThreeDisclaimerConstantsAreDefined() {
            // Given/When - Access all three constants
            String taxSummary = Disclaimers.TAX_SUMMARY_DISCLAIMER;
            String hmrcSubmission = Disclaimers.HMRC_SUBMISSION_DISCLAIMER;
            String pdfConfirmation = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then - All three should be non-null and non-blank
            assertThat(taxSummary)
                .as("TAX_SUMMARY_DISCLAIMER should be defined")
                .isNotNull()
                .isNotBlank();

            assertThat(hmrcSubmission)
                .as("HMRC_SUBMISSION_DISCLAIMER should be defined")
                .isNotNull()
                .isNotBlank();

            assertThat(pdfConfirmation)
                .as("PDF_CONFIRMATION_DISCLAIMER should be defined")
                .isNotNull()
                .isNotBlank();
        }

        @Test
        @DisplayName("TC-509-013-02: All three disclaimers are unique (not duplicates)")
        void allThreeDisclaimersAreUnique() {
            // Given/When
            String taxSummary = Disclaimers.TAX_SUMMARY_DISCLAIMER;
            String hmrcSubmission = Disclaimers.HMRC_SUBMISSION_DISCLAIMER;
            String pdfConfirmation = Disclaimers.PDF_CONFIRMATION_DISCLAIMER;

            // Then - All three should be different
            assertThat(taxSummary)
                .as("Tax summary should differ from HMRC submission")
                .isNotEqualTo(hmrcSubmission);

            assertThat(taxSummary)
                .as("Tax summary should differ from PDF confirmation")
                .isNotEqualTo(pdfConfirmation);

            assertThat(hmrcSubmission)
                .as("HMRC submission should differ from PDF confirmation")
                .isNotEqualTo(pdfConfirmation);
        }

        @Test
        @DisplayName("TC-509-013-03: Disclaimers class is final (cannot be subclassed)")
        void disclaimersClassIsFinal() {
            // Given/When
            Class<?> disclaimersClass = Disclaimers.class;

            // Then
            assertThat(java.lang.reflect.Modifier.isFinal(disclaimersClass.getModifiers()))
                .as("Disclaimers class should be final")
                .isTrue();
        }

        @Test
        @DisplayName("TC-509-013-04: Disclaimers class cannot be instantiated")
        void disclaimersClassCannotBeInstantiated() throws NoSuchMethodException {
            // Given
            Constructor<Disclaimers> constructor = Disclaimers.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            // When/Then
            assertThatThrownBy(constructor::newInstance)
                .as("Disclaimers constructor should throw UnsupportedOperationException")
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("TC-509-013-05: All disclaimer constants are public static final")
        void allDisclaimerConstantsArePublicStaticFinal() throws NoSuchFieldException {
            // Given
            Class<?> disclaimersClass = Disclaimers.class;

            // When/Then - Check TAX_SUMMARY_DISCLAIMER
            var taxSummaryField = disclaimersClass.getDeclaredField("TAX_SUMMARY_DISCLAIMER");
            assertThat(java.lang.reflect.Modifier.isPublic(taxSummaryField.getModifiers()))
                .as("TAX_SUMMARY_DISCLAIMER should be public")
                .isTrue();
            assertThat(java.lang.reflect.Modifier.isStatic(taxSummaryField.getModifiers()))
                .as("TAX_SUMMARY_DISCLAIMER should be static")
                .isTrue();
            assertThat(java.lang.reflect.Modifier.isFinal(taxSummaryField.getModifiers()))
                .as("TAX_SUMMARY_DISCLAIMER should be final")
                .isTrue();

            // When/Then - Check HMRC_SUBMISSION_DISCLAIMER
            var hmrcSubmissionField = disclaimersClass.getDeclaredField("HMRC_SUBMISSION_DISCLAIMER");
            assertThat(java.lang.reflect.Modifier.isPublic(hmrcSubmissionField.getModifiers()))
                .as("HMRC_SUBMISSION_DISCLAIMER should be public")
                .isTrue();
            assertThat(java.lang.reflect.Modifier.isStatic(hmrcSubmissionField.getModifiers()))
                .as("HMRC_SUBMISSION_DISCLAIMER should be static")
                .isTrue();
            assertThat(java.lang.reflect.Modifier.isFinal(hmrcSubmissionField.getModifiers()))
                .as("HMRC_SUBMISSION_DISCLAIMER should be final")
                .isTrue();

            // When/Then - Check PDF_CONFIRMATION_DISCLAIMER
            var pdfConfirmationField = disclaimersClass.getDeclaredField("PDF_CONFIRMATION_DISCLAIMER");
            assertThat(java.lang.reflect.Modifier.isPublic(pdfConfirmationField.getModifiers()))
                .as("PDF_CONFIRMATION_DISCLAIMER should be public")
                .isTrue();
            assertThat(java.lang.reflect.Modifier.isStatic(pdfConfirmationField.getModifiers()))
                .as("PDF_CONFIRMATION_DISCLAIMER should be static")
                .isTrue();
            assertThat(java.lang.reflect.Modifier.isFinal(pdfConfirmationField.getModifiers()))
                .as("PDF_CONFIRMATION_DISCLAIMER should be final")
                .isTrue();
        }
    }

    // ==================== TC-509-014: Version Identifiers Exist ====================

    @Nested
    @DisplayName("TC-509-014: Disclaimer Version ID Tracking")
    class DisclaimerVersionIdTracking {

        @Test
        @DisplayName("TC-509-014-01: TAX_SUMMARY_ID constant exists and equals 'TAX_SUMMARY_V1'")
        void taxSummaryIdExists() {
            // Given/When
            String taxSummaryId = Disclaimers.TAX_SUMMARY_ID;

            // Then
            assertThat(taxSummaryId)
                .as("TAX_SUMMARY_ID should equal TAX_SUMMARY_V1")
                .isNotNull()
                .isNotBlank()
                .isEqualTo("TAX_SUMMARY_V1");
        }

        @Test
        @DisplayName("TC-509-014-02: HMRC_SUBMISSION_ID constant exists and equals 'HMRC_SUBMISSION_V1'")
        void hmrcSubmissionIdExists() {
            // Given/When
            String hmrcSubmissionId = Disclaimers.HMRC_SUBMISSION_ID;

            // Then
            assertThat(hmrcSubmissionId)
                .as("HMRC_SUBMISSION_ID should equal HMRC_SUBMISSION_V1")
                .isNotNull()
                .isNotBlank()
                .isEqualTo("HMRC_SUBMISSION_V1");
        }

        @Test
        @DisplayName("TC-509-014-03: PDF_CONFIRMATION_ID constant exists and equals 'PDF_CONFIRMATION_V1'")
        void pdfConfirmationIdExists() {
            // Given/When
            String pdfConfirmationId = Disclaimers.PDF_CONFIRMATION_ID;

            // Then
            assertThat(pdfConfirmationId)
                .as("PDF_CONFIRMATION_ID should equal PDF_CONFIRMATION_V1")
                .isNotNull()
                .isNotBlank()
                .isEqualTo("PDF_CONFIRMATION_V1");
        }

        @Test
        @DisplayName("TC-509-014-04: All version IDs follow consistent naming pattern")
        void allVersionIdsFollowNamingPattern() {
            // Given/When
            String taxSummaryId = Disclaimers.TAX_SUMMARY_ID;
            String hmrcSubmissionId = Disclaimers.HMRC_SUBMISSION_ID;
            String pdfConfirmationId = Disclaimers.PDF_CONFIRMATION_ID;

            // Then - All should end with version number pattern
            assertThat(taxSummaryId)
                .as("TAX_SUMMARY_ID should follow naming pattern")
                .matches(".*_V\\d+$");

            assertThat(hmrcSubmissionId)
                .as("HMRC_SUBMISSION_ID should follow naming pattern")
                .matches(".*_V\\d+$");

            assertThat(pdfConfirmationId)
                .as("PDF_CONFIRMATION_ID should follow naming pattern")
                .matches(".*_V\\d+$");
        }

        @Test
        @DisplayName("TC-509-014-05: All version IDs are unique")
        void allVersionIdsAreUnique() {
            // Given/When
            String taxSummaryId = Disclaimers.TAX_SUMMARY_ID;
            String hmrcSubmissionId = Disclaimers.HMRC_SUBMISSION_ID;
            String pdfConfirmationId = Disclaimers.PDF_CONFIRMATION_ID;

            // Then
            assertThat(taxSummaryId)
                .as("Tax summary ID should differ from HMRC submission ID")
                .isNotEqualTo(hmrcSubmissionId);

            assertThat(taxSummaryId)
                .as("Tax summary ID should differ from PDF confirmation ID")
                .isNotEqualTo(pdfConfirmationId);

            assertThat(hmrcSubmissionId)
                .as("HMRC submission ID should differ from PDF confirmation ID")
                .isNotEqualTo(pdfConfirmationId);
        }

        @Test
        @DisplayName("TC-509-014-06: Version ID constants are public static final")
        void versionIdConstantsArePublicStaticFinal() throws NoSuchFieldException {
            // Given
            Class<?> disclaimersClass = Disclaimers.class;

            // When/Then - Check all three ID constants
            String[] idFields = {"TAX_SUMMARY_ID", "HMRC_SUBMISSION_ID", "PDF_CONFIRMATION_ID"};

            for (String fieldName : idFields) {
                var field = disclaimersClass.getDeclaredField(fieldName);
                assertThat(java.lang.reflect.Modifier.isPublic(field.getModifiers()))
                    .as("%s should be public", fieldName)
                    .isTrue();
                assertThat(java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                    .as("%s should be static", fieldName)
                    .isTrue();
                assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers()))
                    .as("%s should be final", fieldName)
                    .isTrue();
            }
        }
    }
}
