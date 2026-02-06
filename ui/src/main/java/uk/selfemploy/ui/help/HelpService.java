package uk.selfemploy.ui.help;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.ui.component.HmrcWebViewDialog;
import uk.selfemploy.ui.util.BrowserUtil;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Centralized service for help content throughout the application.
 *
 * <p>SE-701: In-App Help System</p>
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>Help content for various topics</li>
 *   <li>HMRC external links</li>
 *   <li>Content internationalization support (future)</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * HelpService helpService = new HelpService();
 * Optional&lt;HelpContent&gt; help = helpService.getHelp(HelpTopic.NET_PROFIT);
 * String link = helpService.getHmrcLink(HmrcLinkTopic.TAX_RATES);
 * </pre>
 */
public class HelpService {

    private static final Logger LOG = LoggerFactory.getLogger(HelpService.class);

    private final Map<HelpTopic, HelpContent> helpContent;

    /**
     * Creates a new HelpService with default content.
     */
    public HelpService() {
        this.helpContent = new EnumMap<>(HelpTopic.class);
        initializeHelpContent();
    }

    /**
     * Gets help content for a specific topic.
     *
     * @param topic the help topic
     * @return Optional containing the help content, or empty if not found
     */
    public Optional<HelpContent> getHelp(HelpTopic topic) {
        return Optional.ofNullable(helpContent.get(topic));
    }

    /**
     * Gets the HMRC URL for a specific topic.
     *
     * @param topic the HMRC link topic
     * @return the URL string
     */
    public String getHmrcLink(HmrcLinkTopic topic) {
        return topic.getUrl();
    }

    /**
     * Opens an external URL in the system browser.
     *
     * <p>This method uses {@link BrowserUtil} to open URLs safely on a background
     * thread, avoiding the "Force Quit" crash that occurred when Desktop.browse()
     * was called directly on the JavaFX Application Thread.</p>
     *
     * @param url the URL to open
     */
    public void openExternalLink(String url) {
        if (url == null || url.isBlank()) {
            LOG.warn("Attempted to open null or blank URL");
            return;
        }

        LOG.debug("Opening external URL: {}", url);
        BrowserUtil.openUrl(url, error -> LOG.warn("Failed to open URL {}: {}", url, error));
    }

    /**
     * Opens an HMRC guidance page in the in-app browser.
     *
     * <p>SE-7XX: In-App Browser for HMRC Guidance</p>
     *
     * <p>This method opens the specified HMRC topic in a non-modal WebView dialog,
     * allowing users to view guidance without leaving the application. The dialog
     * includes navigation controls and an "Open in Browser" fallback button.</p>
     *
     * <p>If the URL is not a valid GOV.UK domain, it falls back to opening
     * in the external browser for security reasons.</p>
     *
     * @param topic the HMRC link topic to display
     */
    public void openHmrcGuidance(HmrcLinkTopic topic) {
        if (topic == null) {
            LOG.warn("Attempted to open null HMRC topic");
            return;
        }

        LOG.debug("Opening HMRC guidance: {}", topic);
        HmrcWebViewDialog.showTopic(topic);
    }

    /**
     * Opens an HMRC guidance page by URL in the in-app browser.
     *
     * <p>SE-7XX: In-App Browser for HMRC Guidance</p>
     *
     * <p>This method validates that the URL is a GOV.UK domain before opening
     * in the in-app browser. If the URL fails validation, it falls back to
     * opening in the external browser.</p>
     *
     * @param url   the URL to open (must be a GOV.UK domain)
     * @param title the title to display in the dialog
     */
    public void openHmrcGuidance(String url, String title) {
        if (url == null || url.isBlank()) {
            LOG.warn("Attempted to open null or blank HMRC URL");
            return;
        }

        if (!HmrcWebViewDialog.isValidUrl(url)) {
            LOG.warn("URL not allowed in in-app browser, falling back to external: {}", url);
            openExternalLink(url);
            return;
        }

        LOG.debug("Opening HMRC guidance in in-app browser: {}", url);
        HmrcWebViewDialog.showUrl(url, title);
    }

    // === Private Methods ===

    private void initializeHelpContent() {
        // === Tax Summary Topics ===

        helpContent.put(HelpTopic.NET_PROFIT, HelpContent.builder()
                .title("Net Profit (Box 31)")
                .body("Net Profit is your total income minus allowable expenses. " +
                        "This is the amount you'll pay Income Tax and National Insurance on.\n\n" +
                        "Calculation:\n" +
                        "  Turnover (income)\n" +
                        "- Allowable expenses\n" +
                        "= Net Profit")
                .hmrcLink(HmrcLinkTopic.SA103_FORM.getUrl())
                .linkText("View SA103 guidance")
                .build());

        helpContent.put(HelpTopic.INCOME_TAX, HelpContent.builder()
                .title("Income Tax Calculation")
                .body("Income Tax is calculated on your taxable income after deducting " +
                        "your Personal Allowance.\n\n" +
                        "2025/26 Tax Bands:\n" +
                        "• Personal Allowance: £0 - £12,570 (0%)\n" +
                        "• Basic Rate: £12,571 - £50,270 (20%)\n" +
                        "• Higher Rate: £50,271 - £125,140 (40%)\n" +
                        "• Additional Rate: Over £125,140 (45%)")
                .hmrcLink(HmrcLinkTopic.TAX_RATES.getUrl())
                .linkText("View current tax rates")
                .build());

        helpContent.put(HelpTopic.PERSONAL_ALLOWANCE, HelpContent.builder()
                .title("Personal Allowance (2025/26)")
                .body("The standard Personal Allowance is £12,570. This is the amount " +
                        "of income you can earn before paying Income Tax.\n\n" +
                        "High Earner Taper:\n" +
                        "If your income exceeds £100,000, your allowance reduces by £1 " +
                        "for every £2 over this threshold.\n\n" +
                        "At £125,140 or above, you have no Personal Allowance.")
                .hmrcLink(HmrcLinkTopic.PERSONAL_ALLOWANCE.getUrl())
                .linkText("Calculate your allowance")
                .build());

        helpContent.put(HelpTopic.NI_CLASS_4, HelpContent.builder()
                .title("National Insurance Class 4")
                .body("Class 4 contributions are paid on your self-employment profits " +
                        "between the Lower and Upper Profits Limits.\n\n" +
                        "2025/26 Rates:\n" +
                        "• Below £12,570: 0%\n" +
                        "• £12,570 - £50,270: 6%\n" +
                        "• Over £50,270: 2%\n\n" +
                        "You pay Class 4 NI as part of your Self Assessment tax bill.")
                .hmrcLink(HmrcLinkTopic.NI_RATES.getUrl())
                .linkText("View NI rates for self-employed")
                .build());

        helpContent.put(HelpTopic.NI_CLASS_2, HelpContent.builder()
                .title("National Insurance Class 2")
                .body("Class 2 is a flat-rate contribution that helps you qualify for " +
                        "State Pension.\n\n" +
                        "2025/26:\n" +
                        "• Weekly rate: £3.45\n" +
                        "• Annual amount: £179.40\n" +
                        "• Small Profits Threshold: £6,725\n\n" +
                        "Below the SPT, Class 2 is voluntary but may be worth paying " +
                        "to protect your pension entitlement.")
                .hmrcLink(HmrcLinkTopic.STATE_PENSION.getUrl())
                .linkText("Check your State Pension forecast")
                .build());

        helpContent.put(HelpTopic.PAYMENTS_ON_ACCOUNT, HelpContent.builder()
                .title("Payments on Account")
                .body("If your tax bill is more than £1,000, you'll usually need to make " +
                        "advance payments towards next year's tax bill.\n\n" +
                        "How it works:\n" +
                        "• Each payment is half of your previous year's tax bill\n" +
                        "• First payment: 31 January\n" +
                        "• Second payment: 31 July\n\n" +
                        "If you've paid too much, you'll get a refund. If you haven't " +
                        "paid enough, you'll need to make a 'balancing payment'.")
                .hmrcLink(HmrcLinkTopic.PAYMENTS_ON_ACCOUNT.getUrl())
                .linkText("Learn about payments on account")
                .build());

        // === Income Topics ===

        helpContent.put(HelpTopic.PAID_INCOME, HelpContent.builder()
                .title("Paid Income")
                .body("Paid income represents money you have actually received " +
                        "for goods or services provided.\n\n" +
                        "This includes:\n" +
                        "• Invoice payments received\n" +
                        "• Cash payments collected\n" +
                        "• Bank transfers completed\n" +
                        "• Cheques that have cleared\n\n" +
                        "For tax purposes, self-employed individuals typically use " +
                        "the 'cash basis' where income is recorded when received, " +
                        "or 'accrual basis' where income is recorded when earned.")
                .hmrcLink(HmrcLinkTopic.CASH_BASIS.getUrl())
                .linkText("Learn about cash basis accounting")
                .build());

        helpContent.put(HelpTopic.UNPAID_INCOME, HelpContent.builder()
                .title("Unpaid Income")
                .body("Unpaid income represents money you are owed but haven't " +
                        "yet received.\n\n" +
                        "This includes:\n" +
                        "• Outstanding invoices\n" +
                        "• Pending client payments\n" +
                        "• Amounts owed to you\n\n" +
                        "Tracking unpaid income helps you:\n" +
                        "• Monitor cash flow\n" +
                        "• Follow up on late payments\n" +
                        "• Plan for expected revenue\n\n" +
                        "Note: If using the cash basis, unpaid income is not " +
                        "counted towards your tax year turnover until received.")
                .hmrcLink(HmrcLinkTopic.BUSINESS_RECORDS.getUrl())
                .linkText("View business records guidance")
                .build());

        // === Expense Topics ===

        helpContent.put(HelpTopic.EXPENSE_CATEGORY, HelpContent.builder()
                .title("SA103F Expense Categories")
                .body("Each expense category maps to a specific box on your " +
                        "HMRC Self Assessment SA103F form (2024-25).\n\n" +
                        "Selecting the correct category ensures your expenses " +
                        "are properly reported to HMRC.\n\n" +
                        "Categories include:\n" +
                        "• Cost of goods (Box 17)\n" +
                        "• Staff costs (Box 19)\n" +
                        "• Travel (Box 20)\n" +
                        "• Premises (Box 21)\n" +
                        "• Office costs (Box 23)\n" +
                        "• Professional fees (Box 28)\n" +
                        "• Other expenses (Box 30)")
                .hmrcLink(HmrcLinkTopic.SA103_FORM.getUrl())
                .linkText("View all SA103 categories")
                .build());

        helpContent.put(HelpTopic.ALLOWABLE_EXPENSES, HelpContent.builder()
                .title("Allowable Business Expenses")
                .body("Only expenses 'wholly and exclusively' for business purposes " +
                        "are tax-deductible.\n\n" +
                        "Mixed personal/business expenses must be apportioned - " +
                        "you can only claim the business portion.\n\n" +
                        "Examples of allowable expenses:\n" +
                        "• Office supplies and equipment\n" +
                        "• Business travel (not commuting)\n" +
                        "• Professional subscriptions\n" +
                        "• Accountancy fees\n" +
                        "• Business insurance")
                .hmrcLink(HmrcLinkTopic.ALLOWABLE_EXPENSES.getUrl())
                .linkText("Read HMRC guidance on allowable expenses")
                .build());

        helpContent.put(HelpTopic.NON_DEDUCTIBLE_EXPENSES, HelpContent.builder()
                .title("Non-Deductible Expenses")
                .body("Some business expenses cannot be claimed for tax relief. " +
                        "You should still record them for accurate accounting, " +
                        "but they won't reduce your tax bill.\n\n" +
                        "Common non-deductible expenses:\n" +
                        "• Business entertainment (client meals, hospitality)\n" +
                        "• Depreciation (use Capital Allowances instead)\n" +
                        "• Fines and penalties\n" +
                        "• Personal expenses\n" +
                        "• Political donations\n\n" +
                        "Note: Some expenses may be partially deductible " +
                        "if they have a genuine business portion.")
                .hmrcLink(HmrcLinkTopic.ALLOWABLE_EXPENSES.getUrl())
                .linkText("Read HMRC guidance on expenses")
                .build());

        // === Submission Topics ===

        helpContent.put(HelpTopic.DECLARATION, HelpContent.builder()
                .title("Declaration Checkbox")
                .body("By checking the declaration boxes, you are confirming that " +
                        "the information you've provided is accurate and complete.\n\n" +
                        "Important:\n" +
                        "• HMRC may check your figures and records\n" +
                        "• Penalties may apply for inaccurate returns\n" +
                        "• You are responsible for the accuracy of your submission\n\n" +
                        "Keep your records for at least 5 years in case HMRC " +
                        "opens an enquiry.")
                .hmrcLink(HmrcLinkTopic.KEEPING_RECORDS.getUrl())
                .linkText("Record keeping requirements")
                .build());

        helpContent.put(HelpTopic.HMRC_SUBMISSION, HelpContent.builder()
                .title("HMRC Submission")
                .body("When you submit to HMRC:\n\n" +
                        "1. Your data is sent securely via the MTD API\n" +
                        "2. HMRC processes your return\n" +
                        "3. You receive a confirmation reference\n\n" +
                        "Keep a copy of your submission confirmation " +
                        "for your records.")
                .hmrcLink(HmrcLinkTopic.MTD_FOR_ITSA.getUrl())
                .linkText("Learn about Making Tax Digital")
                .build());

        // === General Topics ===

        helpContent.put(HelpTopic.TAX_YEAR, HelpContent.builder()
                .title("Tax Year")
                .body("The UK tax year runs from 6 April to 5 April.\n\n" +
                        "Key dates for 2025/26:\n" +
                        "• Tax year starts: 6 April 2025\n" +
                        "• Tax year ends: 5 April 2026\n" +
                        "• Online filing deadline: 31 January 2027\n" +
                        "• Payment deadline: 31 January 2027")
                .hmrcLink(HmrcLinkTopic.FILING_DEADLINES.getUrl())
                .linkText("View all deadlines")
                .build());

        helpContent.put(HelpTopic.SA103_FORM, HelpContent.builder()
                .title("SA103 Self-Employment Form")
                .body("The SA103 is the supplementary page for self-employment " +
                        "income in your Self Assessment tax return.\n\n" +
                        "There are two versions:\n" +
                        "• SA103F (Full) - For businesses with turnover over £85,000\n" +
                        "• SA103S (Short) - For simpler businesses\n\n" +
                        "This software uses the full form format to ensure " +
                        "complete and accurate reporting.")
                .hmrcLink(HmrcLinkTopic.SA103_FORM.getUrl())
                .linkText("Download SA103 form and notes")
                .build());

        // === User Guide Topics (SE-9XX) ===

        helpContent.put(HelpTopic.GETTING_STARTED, HelpContent.builder()
                .title("Getting Started")
                .body("Welcome to UK Self-Employment Manager - a free, privacy-first " +
                        "application for UK self-employed individuals.\n\n" +
                        "Quick overview of features:\n" +
                        "• Track Income: Record all business income\n" +
                        "• Track Expenses: Log expenses by SA103 category\n" +
                        "• Calculate Tax: Real-time Income Tax & NI estimates\n" +
                        "• Submit to HMRC: One-click MTD submission\n\n" +
                        "First Steps:\n" +
                        "1. Complete the setup wizard with your details\n" +
                        "2. Add your first income entry from Dashboard\n" +
                        "3. Record business expenses as you incur them\n" +
                        "4. Review your Tax Summary regularly\n" +
                        "5. Connect to HMRC when ready to submit")
                .build());

        helpContent.put(HelpTopic.HMRC_CONNECTION, HelpContent.builder()
                .title("HMRC Connection & OAuth2")
                .body("This app uses secure OAuth2 to connect to HMRC.\n\n" +
                        "How it works:\n" +
                        "1. Click 'Connect to HMRC' in the app\n" +
                        "2. Your browser opens HMRC's Government Gateway\n" +
                        "3. You log in with YOUR Government Gateway credentials\n" +
                        "4. HMRC asks if you grant access to this app\n" +
                        "5. You click 'Allow' and return to the app\n\n" +
                        "Password Safety:\n" +
                        "• Your password is entered ONLY on HMRC's website\n" +
                        "• This app NEVER sees or stores your password\n" +
                        "• We receive only a limited-access token\n" +
                        "• HMRC controls what we can do on your behalf\n\n" +
                        "To Disconnect:\n" +
                        "• Settings > HMRC Connection > Disconnect, or\n" +
                        "• Log into HMRC and revoke access manually")
                .hmrcLink(HmrcLinkTopic.MTD_FOR_ITSA.getUrl())
                .linkText("Learn about Making Tax Digital")
                .build());

        helpContent.put(HelpTopic.SECURITY_PRIVACY, HelpContent.builder()
                .title("Security & Privacy")
                .body("Your data privacy is our priority.\n\n" +
                        "Local-Only Storage:\n" +
                        "• All data stays on YOUR computer\n" +
                        "• We have NO access to your data\n" +
                        "• No cloud servers store your information\n\n" +
                        "Encryption:\n" +
                        "• Database encrypted with AES-256-GCM\n" +
                        "• HMRC tokens stored in OS keychain\n" +
                        "• All data at rest is encrypted\n\n" +
                        "What's Sent to HMRC:\n" +
                        "• Your tax figures (income, expenses, profit)\n" +
                        "• Fraud prevention headers (required by law)\n" +
                        "• Device ID, OS, app version, timezone\n\n" +
                        "No Third-Party Sharing:\n" +
                        "• We share nothing with advertisers\n" +
                        "• No analytics or tracking companies\n" +
                        "• Only HMRC receives data (when you submit)")
                .build());

        helpContent.put(HelpTopic.BANK_IMPORT, HelpContent.builder()
                .title("Bank Statement Import")
                .body("Import transactions directly from your bank's CSV export.\n\n" +
                        "Supported Banks:\n" +
                        "• Barclays, HSBC, Lloyds, Nationwide\n" +
                        "• Starling, Monzo, Revolut\n" +
                        "• Santander, Metro Bank\n" +
                        "• Any bank (manual column mapping)\n\n" +
                        "How It Works:\n" +
                        "1. Export a CSV statement from your online banking\n" +
                        "2. Open the Import Wizard from Income or Expenses\n" +
                        "3. Drag & drop your CSV file (or browse)\n" +
                        "4. The app auto-detects your bank format\n" +
                        "5. Review transactions and assign categories\n" +
                        "6. Confirm to import into your records\n\n" +
                        "Key Features:\n" +
                        "• Auto-detection: recognises 9 major UK banks\n" +
                        "• Duplicate detection: flags already-imported transactions\n" +
                        "• Smart categorisation: suggests SA103 categories\n" +
                        "• Import history: view and undo past imports\n\n" +
                        "Tips:\n" +
                        "• Download CSV (not PDF) from your bank\n" +
                        "• Include the full date range you need\n" +
                        "• Review suggested categories before confirming\n" +
                        "• Personal transactions can be skipped during import")
                .build());

        helpContent.put(HelpTopic.FAQ, HelpContent.builder()
                .title("Frequently Asked Questions")
                .body("Common Questions:\n\n" +
                        "Do I need to be registered as self-employed?\n" +
                        "Yes. You must be registered with HMRC and have a UTR.\n\n" +
                        "What tax years are supported?\n" +
                        "UK tax years from 2024/25 onwards are supported.\n\n" +
                        "Does the app work offline?\n" +
                        "Yes, for recording income/expenses. Internet is needed " +
                        "only for HMRC submissions.\n\n" +
                        "What if I make a mistake?\n" +
                        "Before submitting: just edit or delete the entry.\n" +
                        "After submitting: contact HMRC to make corrections.\n\n" +
                        "Does this handle VAT?\n" +
                        "No. This app is for Income Tax and NI Class 4 only.\n\n" +
                        "Why doesn't my tax match my accountant's figure?\n" +
                        "Accountants may apply reliefs or allowances not entered. " +
                        "Always verify with a qualified professional.")
                .build());
    }
}
