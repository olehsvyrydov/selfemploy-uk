package uk.selfemploy.ui.component;

import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import javafx.stage.Stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Tests for SE-701: HelpIcon component.
 *
 * <p>HelpIcon is a reusable "?" icon that shows contextual help
 * via tooltip or popup.</p>
 *
 * <p>Tagged as "e2e" to exclude from CI headless environment -
 * requires JavaFX platform with display.</p>
 */
@DisplayName("SE-701: HelpIcon")
@ExtendWith(ApplicationExtension.class)
@Tag("e2e")
class HelpIconTest {

    private HelpIcon helpIcon;

    @Start
    void start(Stage stage) {
        // Required for JavaFX initialization
    }

    @BeforeEach
    void setUp() {
        helpIcon = new HelpIcon();
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create help icon with default values")
        void shouldCreateHelpIconWithDefaultValues() {
            assertThat(helpIcon).isNotNull();
            assertThat(helpIcon.getTitle()).isEmpty();
            assertThat(helpIcon.getContent()).isEmpty();
            assertThat(helpIcon.getLinkUrl()).isNull();
            assertThat(helpIcon.getLinkText()).isNull();
        }

        @Test
        @DisplayName("should create help icon with title and content")
        void shouldCreateHelpIconWithTitleAndContent() {
            HelpIcon icon = new HelpIcon("Tax Help", "Information about tax rates");

            assertThat(icon.getTitle()).isEqualTo("Tax Help");
            assertThat(icon.getContent()).isEqualTo("Information about tax rates");
        }

        @Test
        @DisplayName("should create help icon with all properties")
        void shouldCreateHelpIconWithAllProperties() {
            HelpIcon icon = new HelpIcon(
                    "Net Profit",
                    "Net Profit is your income minus expenses.",
                    "https://www.gov.uk/guidance",
                    "View HMRC guidance"
            );

            assertThat(icon.getTitle()).isEqualTo("Net Profit");
            assertThat(icon.getContent()).isEqualTo("Net Profit is your income minus expenses.");
            assertThat(icon.getLinkUrl()).isEqualTo("https://www.gov.uk/guidance");
            assertThat(icon.getLinkText()).isEqualTo("View HMRC guidance");
        }
    }

    @Nested
    @DisplayName("Properties")
    class Properties {

        @Test
        @DisplayName("should allow setting title")
        void shouldAllowSettingTitle() {
            helpIcon.setTitle("New Title");

            assertThat(helpIcon.getTitle()).isEqualTo("New Title");
        }

        @Test
        @DisplayName("should allow setting content")
        void shouldAllowSettingContent() {
            helpIcon.setContent("New content text");

            assertThat(helpIcon.getContent()).isEqualTo("New content text");
        }

        @Test
        @DisplayName("should allow setting link URL")
        void shouldAllowSettingLinkUrl() {
            helpIcon.setLinkUrl("https://example.com");

            assertThat(helpIcon.getLinkUrl()).isEqualTo("https://example.com");
        }

        @Test
        @DisplayName("should allow setting link text")
        void shouldAllowSettingLinkText() {
            helpIcon.setLinkText("Learn more");

            assertThat(helpIcon.getLinkText()).isEqualTo("Learn more");
        }

        @Test
        @DisplayName("should provide title property for binding")
        void shouldProvideTitlePropertyForBinding() {
            assertThat(helpIcon.titleProperty()).isNotNull();

            helpIcon.titleProperty().set("Bound Title");
            assertThat(helpIcon.getTitle()).isEqualTo("Bound Title");
        }

        @Test
        @DisplayName("should provide content property for binding")
        void shouldProvideContentPropertyForBinding() {
            assertThat(helpIcon.contentProperty()).isNotNull();

            helpIcon.contentProperty().set("Bound Content");
            assertThat(helpIcon.getContent()).isEqualTo("Bound Content");
        }
    }

    @Nested
    @DisplayName("Tooltip")
    class TooltipTests {

        @Test
        @DisplayName("should have tooltip installed")
        void shouldHaveTooltipInstalled() {
            helpIcon.setTitle("Help Title");
            helpIcon.setContent("Help body text");

            Tooltip tooltip = helpIcon.getTooltip();
            assertThat(tooltip).isNotNull();
        }

        @Test
        @DisplayName("should show title in tooltip")
        void shouldShowTitleInTooltip() {
            helpIcon.setTitle("Income Tax");
            helpIcon.setContent("Calculated on your taxable income");

            Tooltip tooltip = helpIcon.getTooltip();
            String tooltipText = tooltip.getText();

            assertThat(tooltipText).contains("Income Tax");
        }

        @Test
        @DisplayName("should show content in tooltip")
        void shouldShowContentInTooltip() {
            helpIcon.setTitle("Title");
            helpIcon.setContent("This is the help content");

            Tooltip tooltip = helpIcon.getTooltip();
            String tooltipText = tooltip.getText();

            assertThat(tooltipText).contains("This is the help content");
        }

        @Test
        @DisplayName("should update tooltip when content changes")
        void shouldUpdateTooltipWhenContentChanges() {
            helpIcon.setTitle("Title");
            helpIcon.setContent("Original content");

            helpIcon.setContent("Updated content");

            Tooltip tooltip = helpIcon.getTooltip();
            assertThat(tooltip.getText()).contains("Updated content");
        }
    }

    @Nested
    @DisplayName("Structure")
    class Structure {

        @Test
        @DisplayName("should have correct CSS class")
        void shouldHaveCorrectCssClass() {
            assertThat(helpIcon.getStyleClass()).contains("help-icon-btn");
        }

        @Test
        @DisplayName("should be focusable for accessibility")
        void shouldBeFocusableForAccessibility() {
            assertThat(helpIcon.isFocusTraversable()).isTrue();
        }

        @Test
        @DisplayName("should have accessible role description")
        void shouldHaveAccessibleRoleDescription() {
            // The accessible text should describe the help icon
            assertThat(helpIcon.getAccessibleText()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Link Handling")
    class LinkHandling {

        @Test
        @DisplayName("should indicate when link is present")
        void shouldIndicateWhenLinkIsPresent() {
            helpIcon.setLinkUrl("https://example.com");

            assertThat(helpIcon.hasLink()).isTrue();
        }

        @Test
        @DisplayName("should indicate when link is absent")
        void shouldIndicateWhenLinkIsAbsent() {
            assertThat(helpIcon.hasLink()).isFalse();
        }

        @Test
        @DisplayName("should not indicate link for empty URL")
        void shouldNotIndicateLinkForEmptyUrl() {
            helpIcon.setLinkUrl("");

            assertThat(helpIcon.hasLink()).isFalse();
        }

        @Test
        @DisplayName("should not indicate link for blank URL")
        void shouldNotIndicateLinkForBlankUrl() {
            helpIcon.setLinkUrl("   ");

            assertThat(helpIcon.hasLink()).isFalse();
        }
    }
}
