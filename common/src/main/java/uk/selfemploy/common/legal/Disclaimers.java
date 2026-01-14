package uk.selfemploy.common.legal;

/**
 * Legal disclaimers for the Self Employment tax software.
 *
 * <p>SE-509: Enhanced Disclaimers
 *
 * <p>This class provides centralized legal disclaimer texts as approved by
 * /alex (Senior UK Legal Counsel). These disclaimers are required for
 * professional indemnity protection.
 *
 * <p><strong>Usage Guidelines:</strong>
 * <ul>
 *   <li>Disclaimers MUST NOT be modified without legal approval</li>
 *   <li>Disclaimers CANNOT be dismissed permanently by users (AC-4)</li>
 *   <li>All disclaimer text must match /alex legal review specifications (AC-5)</li>
 * </ul>
 *
 * <p><strong>Disclaimer Locations:</strong>
 * <ul>
 *   <li>{@link #TAX_SUMMARY_DISCLAIMER} - Displayed on tax summary view (AC-1)</li>
 *   <li>{@link #HMRC_SUBMISSION_DISCLAIMER} - Displayed before HMRC submission (AC-2)</li>
 *   <li>{@link #PDF_CONFIRMATION_DISCLAIMER} - Included in PDF confirmation footer (AC-3)</li>
 * </ul>
 *
 * @see <a href="docs/sprints/sprint-6/approvals/alex-legal.md">Legal Approval Document</a>
 */
public final class Disclaimers {

    // Version identifiers for tracking disclaimer versions
    /**
     * Version identifier for the tax summary disclaimer.
     */
    public static final String TAX_SUMMARY_ID = "TAX_SUMMARY_V1";

    /**
     * Version identifier for the HMRC submission disclaimer.
     */
    public static final String HMRC_SUBMISSION_ID = "HMRC_SUBMISSION_V1";

    /**
     * Version identifier for the PDF confirmation disclaimer.
     */
    public static final String PDF_CONFIRMATION_ID = "PDF_CONFIRMATION_V1";

    /**
     * Version identifier for the consumer rights disclaimer (SE-513).
     */
    public static final String CONSUMER_RIGHTS_ID = "CONSUMER_RIGHTS_V1";

    /**
     * Tax Summary Disclaimer (AC-1).
     *
     * <p>Displayed prominently on the tax summary view to inform users that
     * calculations are estimates only and should be verified by a professional.
     *
     * <p>This disclaimer addresses:
     * <ul>
     *   <li>The estimate nature of calculations</li>
     *   <li>The need for professional verification</li>
     *   <li>The limitation of software calculations</li>
     * </ul>
     */
    public static final String TAX_SUMMARY_DISCLAIMER =
        "IMPORTANT: This tax calculation is an estimate only and may not reflect your actual tax liability. " +
        "You should verify all figures with a qualified accountant or tax advisor before making any financial decisions.";

    /**
     * HMRC Submission Disclaimer (AC-2).
     *
     * <p>Displayed before the user confirms submission to HMRC. The user must
     * acknowledge this disclaimer before proceeding with submission.
     *
     * <p>This disclaimer emphasizes:
     * <ul>
     *   <li>User confirmation of data accuracy</li>
     *   <li>User responsibility for submitted data</li>
     *   <li>The finality of HMRC submissions</li>
     * </ul>
     */
    public static final String HMRC_SUBMISSION_DISCLAIMER =
        "By submitting this data to HMRC, you confirm that the information provided is accurate and complete " +
        "to the best of your knowledge. You are solely responsible for the accuracy of all submitted data.";

    /**
     * PDF Confirmation Disclaimer (AC-3).
     *
     * <p>Included in the footer of all PDF confirmation documents generated
     * after successful HMRC submission.
     *
     * <p>This disclaimer clarifies:
     * <ul>
     *   <li>Software does not constitute professional advice</li>
     *   <li>Calculations are based on user-provided data</li>
     *   <li>Users should consult qualified professionals</li>
     * </ul>
     */
    public static final String PDF_CONFIRMATION_DISCLAIMER =
        "DISCLAIMER: This software does not constitute professional tax, legal, or financial advice. " +
        "The calculations shown are estimates based on the data you have provided. " +
        "Always consult a qualified professional for advice specific to your circumstances.";

    // === Consumer Rights Act 2015 Disclaimers (SE-513) ===

    /**
     * Consumer Rights Disclaimer Title.
     *
     * <p>SE-513: Displayed in the Settings/About section.
     */
    public static final String CONSUMER_RIGHTS_TITLE = "Software Accuracy & Your Rights";

    /**
     * Consumer Rights Disclaimer - First Paragraph.
     *
     * <p>SE-513: Explains the software's purpose and user responsibility.
     */
    public static final String CONSUMER_RIGHTS_PARAGRAPH_1 =
        "This software provides tax calculation tools based on published HMRC rates and thresholds. " +
        "While we strive for accuracy, you are responsible for verifying all calculations before submission to HMRC.";

    /**
     * Consumer Rights Disclaimer - Second Paragraph.
     *
     * <p>SE-513: Addresses Consumer Rights Act 2015 and open-source nature.
     */
    public static final String CONSUMER_RIGHTS_PARAGRAPH_2 =
        "Under the Consumer Rights Act 2015, digital content must be of satisfactory quality. " +
        "As this is free, open-source software provided under the Apache License 2.0, " +
        "the statutory remedies for defects differ from paid software.";

    /**
     * Consumer Rights Disclaimer - Recommendations Header.
     */
    public static final String CONSUMER_RIGHTS_RECOMMENDATIONS_HEADER = "We strongly recommend:";

    /**
     * Consumer Rights Disclaimer - First Recommendation.
     */
    public static final String CONSUMER_RIGHTS_RECOMMENDATION_1 = "Reviewing all calculations before submission";

    /**
     * Consumer Rights Disclaimer - Second Recommendation.
     */
    public static final String CONSUMER_RIGHTS_RECOMMENDATION_2 = "Keeping your own records to verify figures";

    /**
     * Consumer Rights Disclaimer - Third Recommendation.
     */
    public static final String CONSUMER_RIGHTS_RECOMMENDATION_3 = "Consulting a qualified accountant for complex situations";

    /**
     * Consumer Rights Disclaimer - Acknowledgment Statement.
     *
     * <p>SE-513: Final statement about user responsibility.
     */
    public static final String CONSUMER_RIGHTS_ACKNOWLEDGMENT =
        "By using this software, you acknowledge that tax calculations are estimates " +
        "and that you bear responsibility for the accuracy of any submissions made to HMRC.";

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static constants and should not be instantiated.
     */
    private Disclaimers() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }
}
