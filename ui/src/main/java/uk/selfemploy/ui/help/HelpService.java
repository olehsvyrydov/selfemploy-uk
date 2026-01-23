package uk.selfemploy.ui.help;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * @param url the URL to open
     */
    public void openExternalLink(String url) {
        if (url == null || url.isBlank()) {
            LOG.warn("Attempted to open null or blank URL");
            return;
        }

        try {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                desktop.browse(new java.net.URI(url));
                LOG.debug("Opened external URL: {}", url);
            } else {
                LOG.warn("Desktop browse action not supported on this platform");
            }
        } catch (Exception e) {
            LOG.warn("Failed to open URL: {} - {}", url, e.getMessage());
        }
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

        // === Expense Topics ===

        helpContent.put(HelpTopic.EXPENSE_CATEGORY, HelpContent.builder()
                .title("SA103 Expense Categories")
                .body("Each expense category maps to a specific box on your " +
                        "HMRC Self Assessment SA103 form.\n\n" +
                        "Selecting the correct category ensures your expenses " +
                        "are properly reported to HMRC.\n\n" +
                        "Categories include:\n" +
                        "• Cost of goods (Box 10)\n" +
                        "• Staff costs (Box 12)\n" +
                        "• Travel (Box 13)\n" +
                        "• Premises (Box 14)\n" +
                        "• Office costs (Box 16)\n" +
                        "• Professional fees (Box 21)\n" +
                        "• Other expenses (Box 23)")
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
                .hmrcLink(HmrcLinkTopic.FILING_DEADLINES.getUrl())
                .linkText("View filing guidance")
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
    }
}
