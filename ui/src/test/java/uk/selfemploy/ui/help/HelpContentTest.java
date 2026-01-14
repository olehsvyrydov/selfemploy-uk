package uk.selfemploy.ui.help;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Tests for SE-701: Help Content model.
 *
 * <p>HelpContent represents a single help topic with title, body,
 * and optional HMRC link.</p>
 */
@DisplayName("SE-701: HelpContent")
class HelpContentTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create help content with all fields")
        void shouldCreateHelpContentWithAllFields() {
            HelpContent help = new HelpContent(
                    "Net Profit",
                    "Net Profit is your total income minus allowable expenses.",
                    "https://www.gov.uk/guidance/net-profit"
            );

            assertThat(help.title()).isEqualTo("Net Profit");
            assertThat(help.body()).isEqualTo("Net Profit is your total income minus allowable expenses.");
            assertThat(help.hmrcLinkOptional()).isPresent();
            assertThat(help.hmrcLinkOptional().get()).isEqualTo("https://www.gov.uk/guidance/net-profit");
        }

        @Test
        @DisplayName("should create help content without link")
        void shouldCreateHelpContentWithoutLink() {
            HelpContent help = new HelpContent(
                    "Simple Help",
                    "This is a simple help message.",
                    null
            );

            assertThat(help.title()).isEqualTo("Simple Help");
            assertThat(help.body()).isEqualTo("This is a simple help message.");
            assertThat(help.hmrcLinkOptional()).isEmpty();
        }

        @Test
        @DisplayName("should reject null title")
        void shouldRejectNullTitle() {
            assertThatThrownBy(() -> new HelpContent(null, "Body", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject null body")
        void shouldRejectNullBody() {
            assertThatThrownBy(() -> new HelpContent("Title", null, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should reject blank title")
        void shouldRejectBlankTitle() {
            assertThatThrownBy(() -> new HelpContent("   ", "Body", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject blank body")
        void shouldRejectBlankBody() {
            assertThatThrownBy(() -> new HelpContent("Title", "  ", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("should build help content with builder")
        void shouldBuildHelpContentWithBuilder() {
            HelpContent help = HelpContent.builder()
                    .title("Income Tax")
                    .body("Income Tax is calculated on your taxable income.")
                    .hmrcLink("https://www.gov.uk/income-tax-rates")
                    .build();

            assertThat(help.title()).isEqualTo("Income Tax");
            assertThat(help.body()).isEqualTo("Income Tax is calculated on your taxable income.");
            assertThat(help.hmrcLinkOptional()).contains("https://www.gov.uk/income-tax-rates");
        }

        @Test
        @DisplayName("should build help content without link using builder")
        void shouldBuildHelpContentWithoutLinkUsingBuilder() {
            HelpContent help = HelpContent.builder()
                    .title("Quick Tip")
                    .body("Remember to save your receipts.")
                    .build();

            assertThat(help.hmrcLinkOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Link Text")
    class LinkText {

        @Test
        @DisplayName("should provide default link text")
        void shouldProvideDefaultLinkText() {
            HelpContent help = new HelpContent(
                    "Title",
                    "Body",
                    "https://www.gov.uk/guidance"
            );

            assertThat(help.linkText()).isEqualTo("View HMRC guidance");
        }

        @Test
        @DisplayName("should allow custom link text")
        void shouldAllowCustomLinkText() {
            HelpContent help = HelpContent.builder()
                    .title("Title")
                    .body("Body")
                    .hmrcLink("https://www.gov.uk/guidance")
                    .linkText("Learn more about this topic")
                    .build();

            assertThat(help.linkText()).isEqualTo("Learn more about this topic");
        }

        @Test
        @DisplayName("should return empty string for link text when no link")
        void shouldReturnEmptyStringForLinkTextWhenNoLink() {
            HelpContent help = new HelpContent("Title", "Body", null);

            assertThat(help.linkText()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Has Link")
    class HasLink {

        @Test
        @DisplayName("should return true when link present")
        void shouldReturnTrueWhenLinkPresent() {
            HelpContent help = new HelpContent("Title", "Body", "https://example.com");

            assertThat(help.hasLink()).isTrue();
        }

        @Test
        @DisplayName("should return false when link absent")
        void shouldReturnFalseWhenLinkAbsent() {
            HelpContent help = new HelpContent("Title", "Body", null);

            assertThat(help.hasLink()).isFalse();
        }
    }
}
