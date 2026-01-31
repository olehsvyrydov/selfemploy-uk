package uk.selfemploy.ui.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PrerequisiteItem component.
 * Sprint 12 - SE-12-001: Prerequisites Checklist Screen
 *
 * <p>Tests the reusable prerequisite checklist item with icon,
 * title, description, and external link.</p>
 *
 * <p>Note: Tagged as "e2e" because this test instantiates JavaFX components
 * which require the JavaFX toolkit to be initialized.</p>
 */
@Tag("e2e")
@DisplayName("PrerequisiteItem")
class PrerequisiteItemTest {

    private PrerequisiteItem item;

    @BeforeEach
    void setUp() {
        item = new PrerequisiteItem(
            "FILE_ALT",
            "teal",
            "Registered for Self Assessment",
            "You must be registered with HMRC for Self Assessment tax returns.",
            "How to register",
            "https://www.gov.uk/register-for-self-assessment"
        );
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("should create item without errors")
        void shouldCreateItemWithoutErrors() {
            assertThat(item).isNotNull();
        }

        @Test
        @DisplayName("should store title")
        void shouldStoreTitle() {
            assertThat(item.getTitle()).isEqualTo("Registered for Self Assessment");
        }

        @Test
        @DisplayName("should store description")
        void shouldStoreDescription() {
            assertThat(item.getDescription())
                .isEqualTo("You must be registered with HMRC for Self Assessment tax returns.");
        }

        @Test
        @DisplayName("should store link text")
        void shouldStoreLinkText() {
            assertThat(item.getLinkText()).isEqualTo("How to register");
        }

        @Test
        @DisplayName("should store link URL")
        void shouldStoreLinkUrl() {
            assertThat(item.getLinkUrl())
                .isEqualTo("https://www.gov.uk/register-for-self-assessment");
        }

        @Test
        @DisplayName("should store icon type")
        void shouldStoreIconType() {
            assertThat(item.getIconType()).isEqualTo("FILE_ALT");
        }

        @Test
        @DisplayName("should store color variant")
        void shouldStoreColorVariant() {
            assertThat(item.getColorVariant()).isEqualTo("teal");
        }
    }

    @Nested
    @DisplayName("Style Class Tests")
    class StyleClassTests {

        @Test
        @DisplayName("should have base style class")
        void shouldHaveBaseStyleClass() {
            assertThat(item.getStyleClass())
                .contains("prerequisite-item");
        }

        @Test
        @DisplayName("should have icon wrapper style class based on color")
        void shouldHaveIconWrapperStyleClass() {
            // The icon wrapper inside the item should have the color class
            assertThat(item.getIconWrapperStyleClass())
                .contains("prerequisite-icon-wrapper")
                .contains("teal");
        }
    }

    @Nested
    @DisplayName("Accessibility Tests")
    class AccessibilityTests {

        @Test
        @DisplayName("should be focus traversable")
        void shouldBeFocusTraversable() {
            // The link within should be focusable
            assertThat(item.isLinkFocusable()).isTrue();
        }

        @Test
        @DisplayName("should provide accessible description")
        void shouldProvideAccessibleDescription() {
            assertThat(item.getAccessibleText())
                .contains("Registered for Self Assessment");
        }
    }

    @Nested
    @DisplayName("Link Action Tests")
    class LinkActionTests {

        @Test
        @DisplayName("should have clickable link")
        void shouldHaveClickableLink() {
            assertThat(item.hasLink()).isTrue();
        }

        @Test
        @DisplayName("should store link URL for external navigation")
        void shouldStoreLinkUrlForExternalNavigation() {
            assertThat(item.getLinkUrl())
                .startsWith("https://")
                .contains("gov.uk");
        }
    }

    @Nested
    @DisplayName("Color Variant Tests")
    class ColorVariantTests {

        @Test
        @DisplayName("should support teal variant")
        void shouldSupportTealVariant() {
            PrerequisiteItem tealItem = new PrerequisiteItem(
                "FILE_ALT", "teal", "Title", "Desc", "Link", "http://example.com");
            assertThat(tealItem.getColorVariant()).isEqualTo("teal");
        }

        @Test
        @DisplayName("should support blue variant")
        void shouldSupportBlueVariant() {
            PrerequisiteItem blueItem = new PrerequisiteItem(
                "USER_SHIELD", "blue", "Title", "Desc", "Link", "http://example.com");
            assertThat(blueItem.getColorVariant()).isEqualTo("blue");
        }

        @Test
        @DisplayName("should support violet variant")
        void shouldSupportVioletVariant() {
            PrerequisiteItem violetItem = new PrerequisiteItem(
                "ID_CARD", "violet", "Title", "Desc", "Link", "http://example.com");
            assertThat(violetItem.getColorVariant()).isEqualTo("violet");
        }
    }

    @Nested
    @DisplayName("Icon Type Tests")
    class IconTypeTests {

        @Test
        @DisplayName("should support FILE_ALT icon")
        void shouldSupportFileAltIcon() {
            PrerequisiteItem fileItem = new PrerequisiteItem(
                "FILE_ALT", "teal", "Title", "Desc", "Link", "http://example.com");
            assertThat(fileItem.getIconType()).isEqualTo("FILE_ALT");
        }

        @Test
        @DisplayName("should support USER_SHIELD icon")
        void shouldSupportUserShieldIcon() {
            PrerequisiteItem userItem = new PrerequisiteItem(
                "USER_SHIELD", "blue", "Title", "Desc", "Link", "http://example.com");
            assertThat(userItem.getIconType()).isEqualTo("USER_SHIELD");
        }

        @Test
        @DisplayName("should support ID_CARD icon")
        void shouldSupportIdCardIcon() {
            PrerequisiteItem idItem = new PrerequisiteItem(
                "ID_CARD", "violet", "Title", "Desc", "Link", "http://example.com");
            assertThat(idItem.getIconType()).isEqualTo("ID_CARD");
        }
    }

    @Nested
    @DisplayName("Event Handler Tests")
    class EventHandlerTests {

        @Test
        @DisplayName("should accept link click handler")
        void shouldAcceptLinkClickHandler() {
            // Given
            boolean[] handlerCalled = {false};

            // When
            item.setOnLinkClick(url -> handlerCalled[0] = true);

            // Then - handler should be settable without error
            assertThat(item.hasLinkClickHandler()).isTrue();
        }
    }
}
