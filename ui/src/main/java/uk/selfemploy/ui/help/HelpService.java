package uk.selfemploy.ui.help;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.config.NIClass2Rates;
import uk.selfemploy.core.config.TaxRateConfiguration;
import uk.selfemploy.ui.component.HmrcWebViewDialog;
import uk.selfemploy.ui.help.markdown.HelpMarkdownParser;
import uk.selfemploy.ui.help.markdown.HelpMarkdownParser.FrontMatter;
import uk.selfemploy.ui.util.BrowserUtil;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Centralized service for help content throughout the application.
 *
 * <p>Topic copy lives as markdown resources under {@code /help/&lt;TOPIC&gt;.md} (one file per
 * {@link HelpTopic}), each with a YAML front-matter header (title, category, optional HMRC link).
 * Keeping the copy out of Java means the wording can be reviewed as prose and rendered through a
 * single {@link uk.selfemploy.ui.help.markdown.HelpMarkdownRenderer} so it cannot drift per call
 * site. Year-specific figures are written as {@code {{placeholder}}} tokens and resolved once, at
 * load time, from {@link TaxRateConfiguration} for the current tax year.</p>
 */
public class HelpService {

    private static final Logger LOG = LoggerFactory.getLogger(HelpService.class);

    private static final String RESOURCE_DIR = "/help/";

    private final Map<HelpTopic, HelpContent> helpContent;

    /**
     * Creates a new HelpService, resolving year-aware figures for the current tax year.
     */
    public HelpService() {
        this(TaxYear.current().startYear());
    }

    /**
     * Creates a HelpService whose year-aware figures are resolved for the given tax year.
     *
     * @param taxYearStart the tax year start (e.g. 2025 for 2025/26)
     */
    public HelpService(int taxYearStart) {
        this.helpContent = loadContent(buildPlaceholders(taxYearStart));
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

    // === Content loading ===

    private Map<HelpTopic, HelpContent> loadContent(Map<String, String> placeholders) {
        HelpMarkdownParser parser = new HelpMarkdownParser();
        Map<HelpTopic, HelpContent> content = new EnumMap<>(HelpTopic.class);
        for (HelpTopic topic : HelpTopic.values()) {
            loadTopic(topic, parser, placeholders)
                .ifPresent(help -> content.put(topic, help));
        }
        return content;
    }

    private Optional<HelpContent> loadTopic(HelpTopic topic, HelpMarkdownParser parser,
                                            Map<String, String> placeholders) {
        String resource = RESOURCE_DIR + topic.name() + ".md";
        try (InputStream in = HelpService.class.getResourceAsStream(resource)) {
            if (in == null) {
                LOG.warn("No help resource for topic {} ({})", topic, resource);
                return Optional.empty();
            }
            String resolved = applyPlaceholders(new String(in.readAllBytes(), StandardCharsets.UTF_8),
                placeholders);
            FrontMatter fm = parser.parseFrontMatter(resolved);
            String title = fm.title() != null ? fm.title() : topic.name();
            String body = stripFrontMatter(resolved);
            return Optional.of(new HelpContent(title, body, fm.hmrcLink(), fm.linkText()));
        } catch (IOException e) {
            LOG.warn("Failed to read help resource {}: {}", resource, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Removes a leading YAML front-matter block so the stored body is pure markdown. The renderer's
     * parser also strips front matter, but keeping it out of {@link HelpContent#body()} avoids the
     * metadata leaking into any consumer that reads the body directly.
     */
    private static String stripFrontMatter(String markdown) {
        String trimmed = markdown.stripLeading();
        if (!trimmed.startsWith("---")) {
            return markdown;
        }
        int end = trimmed.indexOf("\n---", 3);
        if (end < 0) {
            return markdown;
        }
        int newline = trimmed.indexOf('\n', end + 1);
        return newline < 0 ? "" : trimmed.substring(newline + 1).stripLeading();
    }

    private static String applyPlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    /**
     * Builds the year-aware token values from the authoritative tax configuration, so a single edit
     * to the rate config (or a change of tax year) updates every topic that cites those figures.
     */
    private static Map<String, String> buildPlaceholders(int taxYearStart) {
        TaxYear taxYear = TaxYear.of(taxYearStart);
        NIClass2Rates class2 = TaxRateConfiguration.getInstance().getNIClass2Rates(taxYearStart);

        Map<String, String> values = new HashMap<>();
        values.put("taxYear", taxYear.label());
        values.put("taxYearStart", String.valueOf(taxYearStart));
        values.put("taxYearEnd", String.valueOf(taxYearStart + 1));
        values.put("filingYear", String.valueOf(taxYearStart + 2));
        values.put("class2WeeklyRate", formatMoney(class2.weeklyRate()));
        values.put("class2SPT", formatMoney(class2.smallProfitsThreshold()));
        return values;
    }

    private static String formatMoney(BigDecimal amount) {
        boolean hasPence = amount.stripTrailingZeros().scale() > 0;
        DecimalFormat format = new DecimalFormat(hasPence ? "£#,##0.00" : "£#,##0");
        return format.format(amount.setScale(hasPence ? 2 : 0, RoundingMode.HALF_UP));
    }
}
