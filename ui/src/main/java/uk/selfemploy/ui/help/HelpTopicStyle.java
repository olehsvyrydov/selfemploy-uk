package uk.selfemploy.ui.help;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * How a help topic is presented: which icon its dialog carries, and which colour it is drawn in.
 *
 * <p>One owner, because there were three. The help page kept maps of icons and category colours,
 * while the income and expense screens each carried their own {@code switch} for the topics they
 * open — with {@code ALLOWABLE_EXPENSES} defined in two of them at once. They agreed, which is the
 * dangerous state: nothing would have reported it if they stopped.
 *
 * <p>A topic with no entry falls back to a neutral icon and colour rather than failing, because a
 * help dialog that opens looking plain is better than one that does not open. {@code
 * HelpTopicStyleTest} holds every topic to having its own, so the fallback stays unreachable.
 */
public final class HelpTopicStyle {

    /** Category names, which are also the keys the colours are held under. */
    public static final String CATEGORY_USER_GUIDE = "User Guide";
    public static final String CATEGORY_TAX = "Tax & Calculation";
    public static final String CATEGORY_INCOME = "Income";
    public static final String CATEGORY_EXPENSES = "Expenses";
    public static final String CATEGORY_HMRC = "HMRC Submission";
    public static final String CATEGORY_GENERAL = "General";

    /** Used when a topic has no category of its own, and as the colour for an unknown category. */
    static final String FALLBACK_COLOUR = "#0066cc";
    static final Ikon FALLBACK_ICON = FontAwesomeSolid.INFO_CIRCLE;

    private static final Map<HelpTopic, Ikon> ICONS = new EnumMap<>(HelpTopic.class);
    private static final Map<HelpTopic, String> CATEGORIES = new EnumMap<>(HelpTopic.class);
    private static final Map<String, String> COLOURS = new HashMap<>();

    static {
        COLOURS.put(CATEGORY_USER_GUIDE, "#7c3aed");
        COLOURS.put(CATEGORY_TAX, "#059669");
        COLOURS.put(CATEGORY_INCOME, "#059669");
        COLOURS.put(CATEGORY_EXPENSES, "#d97706");
        COLOURS.put(CATEGORY_HMRC, "#0066cc");
        COLOURS.put(CATEGORY_GENERAL, "#6b7280");

        put(HelpTopic.USER_GUIDE, FontAwesomeSolid.BOOK, CATEGORY_USER_GUIDE);
        put(HelpTopic.GETTING_STARTED, FontAwesomeSolid.ROCKET, CATEGORY_USER_GUIDE);
        put(HelpTopic.BANK_IMPORT, FontAwesomeSolid.FILE_UPLOAD, CATEGORY_USER_GUIDE);
        put(HelpTopic.HMRC_CONNECTION, FontAwesomeSolid.LINK, CATEGORY_USER_GUIDE);
        put(HelpTopic.SECURITY_PRIVACY, FontAwesomeSolid.LOCK, CATEGORY_USER_GUIDE);
        put(HelpTopic.FAQ, FontAwesomeSolid.QUESTION_CIRCLE, CATEGORY_USER_GUIDE);

        put(HelpTopic.NET_PROFIT, FontAwesomeSolid.CHART_LINE, CATEGORY_TAX);
        put(HelpTopic.INCOME_TAX, FontAwesomeSolid.POUND_SIGN, CATEGORY_TAX);
        put(HelpTopic.PERSONAL_ALLOWANCE, FontAwesomeSolid.SHIELD_ALT, CATEGORY_TAX);
        put(HelpTopic.NI_CLASS_4, FontAwesomeSolid.HEARTBEAT, CATEGORY_TAX);
        put(HelpTopic.NI_CLASS_2, FontAwesomeSolid.HEARTBEAT, CATEGORY_TAX);
        put(HelpTopic.PAYMENTS_ON_ACCOUNT, FontAwesomeSolid.CHART_BAR, CATEGORY_TAX);

        // Opened from the income screen, which drew them green; the Income category keeps that.
        put(HelpTopic.PAID_INCOME, FontAwesomeSolid.POUND_SIGN, CATEGORY_INCOME);
        put(HelpTopic.UNPAID_INCOME, FontAwesomeSolid.CLIPBOARD, CATEGORY_INCOME);

        put(HelpTopic.EXPENSE_CATEGORY, FontAwesomeSolid.CLIPBOARD, CATEGORY_EXPENSES);
        put(HelpTopic.ALLOWABLE_EXPENSES, FontAwesomeSolid.CHECK_CIRCLE, CATEGORY_EXPENSES);
        put(HelpTopic.NON_DEDUCTIBLE_EXPENSES, FontAwesomeSolid.TIMES_CIRCLE, CATEGORY_EXPENSES);

        put(HelpTopic.DECLARATION, FontAwesomeSolid.SIGNATURE, CATEGORY_HMRC);
        put(HelpTopic.HMRC_SUBMISSION, FontAwesomeSolid.UPLOAD, CATEGORY_HMRC);

        put(HelpTopic.TAX_YEAR, FontAwesomeSolid.CALENDAR_ALT, CATEGORY_GENERAL);
        put(HelpTopic.SA103_FORM, FontAwesomeSolid.FILE_ALT, CATEGORY_GENERAL);
    }

    private HelpTopicStyle() {
    }

    private static void put(HelpTopic topic, Ikon icon, String category) {
        ICONS.put(topic, icon);
        CATEGORIES.put(topic, category);
    }

    /** The icon a topic's help dialog carries. */
    public static Ikon iconFor(HelpTopic topic) {
        return ICONS.getOrDefault(topic, FALLBACK_ICON);
    }

    /** The category a topic belongs to, which is what decides its colour. */
    public static String categoryFor(HelpTopic topic) {
        return CATEGORIES.getOrDefault(topic, CATEGORY_GENERAL);
    }

    /** The colour a topic's help dialog is drawn in. */
    public static String colourFor(HelpTopic topic) {
        return colourForCategory(categoryFor(topic));
    }

    /** The colour for a named category. */
    public static String colourForCategory(String category) {
        return COLOURS.getOrDefault(category, FALLBACK_COLOUR);
    }

    /** The category names that have a colour, so a test can pin every topic to one of them. */
    public static Map<String, String> colours() {
        return Collections.unmodifiableMap(COLOURS);
    }

    /**
     * The categories actually declared, topic by topic.
     *
     * <p>Exposed because {@link #categoryFor} cannot answer the question a test needs to ask: it
     * returns {@link #CATEGORY_GENERAL} for a topic with no category, which is indistinguishable
     * from a topic genuinely filed under General. A missing entry would otherwise look correct.
     */
    public static Map<HelpTopic, String> declaredCategories() {
        return Collections.unmodifiableMap(CATEGORIES);
    }
}
