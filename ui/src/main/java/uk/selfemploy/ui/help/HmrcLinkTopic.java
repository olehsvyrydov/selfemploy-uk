package uk.selfemploy.ui.help;

/**
 * Enumeration of HMRC external link topics.
 *
 * <p>SE-701: In-App Help System</p>
 *
 * <p>Each topic maps to an official HMRC guidance page
 * managed by {@link HelpService}.</p>
 */
public enum HmrcLinkTopic {

    /**
     * SA103 Self-Employment Form guidance.
     */
    SA103_FORM("https://www.gov.uk/government/publications/self-assessment-self-employment-full-sa103f"),

    /**
     * Income Tax rates and bands.
     */
    TAX_RATES("https://www.gov.uk/income-tax-rates"),

    /**
     * Self-employed National Insurance rates.
     */
    NI_RATES("https://www.gov.uk/self-employed-national-insurance-rates"),

    /**
     * Allowable business expenses guidance.
     */
    ALLOWABLE_EXPENSES("https://www.gov.uk/expenses-if-youre-self-employed"),

    /**
     * Self Assessment filing deadlines.
     */
    FILING_DEADLINES("https://www.gov.uk/self-assessment-tax-returns/deadlines"),

    /**
     * Payments on Account guidance.
     */
    PAYMENTS_ON_ACCOUNT("https://www.gov.uk/understand-self-assessment-bill/payments-on-account"),

    /**
     * Personal Allowance guidance.
     */
    PERSONAL_ALLOWANCE("https://www.gov.uk/income-tax-rates/income-over-100000"),

    /**
     * Making Tax Digital for Income Tax.
     */
    MTD_FOR_ITSA("https://www.gov.uk/guidance/sign-up-your-business-for-making-tax-digital-for-income-tax"),

    /**
     * State Pension forecast.
     */
    STATE_PENSION("https://www.gov.uk/check-state-pension"),

    /**
     * National Insurance record check.
     */
    NI_RECORD("https://www.gov.uk/check-national-insurance-record"),

    /**
     * Cash basis for self-employed income recording.
     */
    CASH_BASIS("https://www.gov.uk/simpler-income-tax-cash-basis"),

    /**
     * Business records for self-employed.
     */
    BUSINESS_RECORDS("https://www.gov.uk/self-employed-records"),

    /**
     * How long to keep business records (in case of HMRC enquiry).
     */
    KEEPING_RECORDS("https://www.gov.uk/self-employed-records/how-long-to-keep-your-records"),

    /**
     * Self Assessment tax return penalties.
     */
    PENALTIES("https://www.gov.uk/self-assessment-tax-returns/penalties");

    private final String url;

    HmrcLinkTopic(String url) {
        this.url = url;
    }

    /**
     * Returns the HMRC URL for this topic.
     *
     * @return the URL string
     */
    public String getUrl() {
        return url;
    }
}
