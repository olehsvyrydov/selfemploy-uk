package uk.selfemploy.ui.help;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every help topic is presented deliberately, not by fallback.
 *
 * <p>Both lookups answer something for a topic they have never heard of, so a dialog always opens.
 * That is the right behaviour and the reason this test exists: a topic added without an icon or a
 * category renders plain and grey while every other test stays green. Only asserting the fallback
 * is unreachable catches it.
 */
@DisplayName("HelpTopicStyle - how each topic is presented")
class HelpTopicStyleTest {

    @Test
    @DisplayName("every topic has an icon of its own")
    void everyTopicHasItsOwnIcon() {
        for (HelpTopic topic : HelpTopic.values()) {
            assertThat(HelpTopicStyle.iconFor(topic))
                    .as("icon for %s", topic)
                    .isNotNull()
                    .isNotEqualTo(HelpTopicStyle.FALLBACK_ICON);
        }
    }

    @Test
    @DisplayName("every topic belongs to a category that has a colour")
    void everyTopicHasAColouredCategory() {
        for (HelpTopic topic : HelpTopic.values()) {
            String category = HelpTopicStyle.categoryFor(topic);
            assertThat(HelpTopicStyle.colours())
                    .as("category '%s' (topic %s) has no colour, so its dialog falls back", category, topic)
                    .containsKey(category);
        }
    }

    @Test
    @DisplayName("a topic's colour is its category's colour")
    void colourFollowsTheCategory() {
        assertThat(HelpTopicStyle.colourFor(HelpTopic.NET_PROFIT))
                .isEqualTo(HelpTopicStyle.colourForCategory(HelpTopicStyle.CATEGORY_TAX));
        assertThat(HelpTopicStyle.colourFor(HelpTopic.ALLOWABLE_EXPENSES))
                .isEqualTo(HelpTopicStyle.colourForCategory(HelpTopicStyle.CATEGORY_EXPENSES));
    }

    @Test
    @DisplayName("the screens that used to keep their own copies still get what they drew")
    void theTopicsTakenFromTheOtherScreensAreUnchanged() {
        // These four were defined in IncomeController and ExpenseController before the mappings were
        // merged; pinned so the move cannot have quietly restyled a dialog.
        assertThat(HelpTopicStyle.colourFor(HelpTopic.PAID_INCOME)).isEqualTo("#059669");
        assertThat(HelpTopicStyle.colourFor(HelpTopic.UNPAID_INCOME)).isEqualTo("#059669");
        assertThat(HelpTopicStyle.colourFor(HelpTopic.ALLOWABLE_EXPENSES)).isEqualTo("#d97706");
        assertThat(HelpTopicStyle.colourFor(HelpTopic.NON_DEDUCTIBLE_EXPENSES)).isEqualTo("#d97706");
    }

    @Test
    @DisplayName("an unknown category still yields a colour, so a dialog never fails to open")
    void unknownCategoryFallsBack() {
        assertThat(HelpTopicStyle.colourForCategory("no such category"))
                .isEqualTo(HelpTopicStyle.FALLBACK_COLOUR);
    }
}
