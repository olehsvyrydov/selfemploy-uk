package uk.selfemploy.ui.component;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InfoCard component.
 * Sprint 12 - SE-12-003: Government Gateway Explainer Screen
 *
 * <p>Tests the reusable info card component with icon, optional title,
 * and content (description, bullet points, or numbered steps).</p>
 *
 * <p>Note: Tagged as "e2e" because this test instantiates JavaFX components
 * which require the JavaFX toolkit to be initialized.</p>
 */
@Tag("e2e")
@DisplayName("InfoCard")
@ExtendWith(ApplicationExtension.class)
class InfoCardTest {

    private InfoCard card;

    @Start
    void start(Stage stage) {
        // Required for JavaFX initialization
    }

    @Nested
    @DisplayName("Description Card Tests")
    class DescriptionCardTests {

        @BeforeEach
        void setUp() {
            card = new InfoCard("SHIELD_ALT", "Your Security", "This is a security message.");
        }

        @Test
        @DisplayName("should create card with description")
        void shouldCreateCardWithDescription() {
            assertThat(card).isNotNull();
        }

        @Test
        @DisplayName("should store icon type")
        void shouldStoreIconType() {
            assertThat(card.getIconType()).isEqualTo("SHIELD_ALT");
        }

        @Test
        @DisplayName("should store title")
        void shouldStoreTitle() {
            assertThat(card.getTitle()).isEqualTo("Your Security");
        }

        @Test
        @DisplayName("should store description")
        void shouldStoreDescription() {
            assertThat(card.getDescription()).isEqualTo("This is a security message.");
        }

        @Test
        @DisplayName("should have base style class")
        void shouldHaveBaseStyleClass() {
            assertThat(card.getStyleClass()).contains("info-card");
        }

        @Test
        @DisplayName("should display icon")
        void shouldDisplayIcon() {
            assertThat(card.hasIcon()).isTrue();
        }

        @Test
        @DisplayName("should display title when provided")
        void shouldDisplayTitleWhenProvided() {
            assertThat(card.hasTitle()).isTrue();
        }
    }

    @Nested
    @DisplayName("Bullet Points Card Tests")
    class BulletPointsCardTests {

        private List<String> bulletPoints;

        @BeforeEach
        void setUp() {
            bulletPoints = Arrays.asList(
                "Filing your Self Assessment",
                "Viewing your tax account",
                "Managing your tax credits"
            );
            card = new InfoCard("SHIELD_ALT", null, "Introduction text:", bulletPoints);
        }

        @Test
        @DisplayName("should create card with bullet points")
        void shouldCreateCardWithBulletPoints() {
            assertThat(card).isNotNull();
            assertThat(card.hasBulletPoints()).isTrue();
        }

        @Test
        @DisplayName("should store bullet points")
        void shouldStoreBulletPoints() {
            assertThat(card.getBulletPoints()).hasSize(3);
            assertThat(card.getBulletPoints()).containsExactlyElementsOf(bulletPoints);
        }

        @Test
        @DisplayName("should not have title when null")
        void shouldNotHaveTitleWhenNull() {
            assertThat(card.hasTitle()).isFalse();
        }

        @Test
        @DisplayName("should have description as intro text")
        void shouldHaveDescriptionAsIntroText() {
            assertThat(card.getDescription()).isEqualTo("Introduction text:");
        }

        @Test
        @DisplayName("should not be numbered")
        void shouldNotBeNumbered() {
            assertThat(card.isNumbered()).isFalse();
        }
    }

    @Nested
    @DisplayName("Numbered Steps Card Tests")
    class NumberedStepsCardTests {

        private List<String> steps;

        @BeforeEach
        void setUp() {
            steps = Arrays.asList(
                "Your browser opens to Government Gateway",
                "Sign in with your Government Gateway credentials",
                "Click \"Grant authority\" to authorize this app",
                "You'll be redirected back here automatically"
            );
            card = new InfoCard("INFO_CIRCLE", "What happens next", steps, true);
        }

        @Test
        @DisplayName("should create card with numbered steps")
        void shouldCreateCardWithNumberedSteps() {
            assertThat(card).isNotNull();
            assertThat(card.isNumbered()).isTrue();
        }

        @Test
        @DisplayName("should store steps")
        void shouldStoreSteps() {
            assertThat(card.getBulletPoints()).hasSize(4);
            assertThat(card.getBulletPoints()).containsExactlyElementsOf(steps);
        }

        @Test
        @DisplayName("should have title")
        void shouldHaveTitle() {
            assertThat(card.hasTitle()).isTrue();
            assertThat(card.getTitle()).isEqualTo("What happens next");
        }

        @Test
        @DisplayName("should not have description when only steps provided")
        void shouldNotHaveDescriptionWhenOnlyStepsProvided() {
            assertThat(card.getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("Icon Type Tests")
    class IconTypeTests {

        @Test
        @DisplayName("should support SHIELD_ALT icon")
        void shouldSupportShieldAltIcon() {
            InfoCard shieldCard = new InfoCard("SHIELD_ALT", "Title", "Description");
            assertThat(shieldCard.getIconType()).isEqualTo("SHIELD_ALT");
        }

        @Test
        @DisplayName("should support LOCK icon")
        void shouldSupportLockIcon() {
            InfoCard lockCard = new InfoCard("LOCK", "Title", "Description");
            assertThat(lockCard.getIconType()).isEqualTo("LOCK");
        }

        @Test
        @DisplayName("should support INFO_CIRCLE icon")
        void shouldSupportInfoCircleIcon() {
            InfoCard infoCard = new InfoCard("INFO_CIRCLE", "Title", "Description");
            assertThat(infoCard.getIconType()).isEqualTo("INFO_CIRCLE");
        }

        @Test
        @DisplayName("should handle unknown icon type gracefully")
        void shouldHandleUnknownIconTypeGracefully() {
            InfoCard unknownCard = new InfoCard("UNKNOWN_ICON", "Title", "Description");
            assertThat(unknownCard.getIconType()).isEqualTo("UNKNOWN_ICON");
            // Should still create card without error
            assertThat(unknownCard.hasIcon()).isTrue();
        }
    }

    @Nested
    @DisplayName("Style Class Tests")
    class StyleClassTests {

        @Test
        @DisplayName("should have info-card style class")
        void shouldHaveInfoCardStyleClass() {
            card = new InfoCard("LOCK", "Title", "Description");
            assertThat(card.getStyleClass()).contains("info-card");
        }

        @Test
        @DisplayName("should allow additional style classes")
        void shouldAllowAdditionalStyleClasses() {
            card = new InfoCard("LOCK", "Title", "Description");
            card.getStyleClass().add("custom-class");

            assertThat(card.getStyleClass()).contains("info-card", "custom-class");
        }
    }

    @Nested
    @DisplayName("Content Layout Tests")
    class ContentLayoutTests {

        @Test
        @DisplayName("should have children when description provided")
        void shouldHaveChildrenWhenDescriptionProvided() {
            card = new InfoCard("LOCK", "Title", "Description");
            assertThat(card.getChildren()).isNotEmpty();
        }

        @Test
        @DisplayName("should have children when bullet points provided")
        void shouldHaveChildrenWhenBulletPointsProvided() {
            card = new InfoCard("LOCK", null, "Intro:", Arrays.asList("Item 1", "Item 2"));
            assertThat(card.getChildren()).isNotEmpty();
        }

        @Test
        @DisplayName("should have children when numbered steps provided")
        void shouldHaveChildrenWhenNumberedStepsProvided() {
            card = new InfoCard("LOCK", "Steps", Arrays.asList("Step 1", "Step 2"), true);
            assertThat(card.getChildren()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Accessibility Tests")
    class AccessibilityTests {

        @Test
        @DisplayName("should provide accessible text from title")
        void shouldProvideAccessibleTextFromTitle() {
            card = new InfoCard("LOCK", "Your Security", "Description");
            String accessibleText = card.buildAccessibleText();
            assertThat(accessibleText).contains("Your Security");
        }

        @Test
        @DisplayName("should provide accessible text from description when no title")
        void shouldProvideAccessibleTextFromDescriptionWhenNoTitle() {
            card = new InfoCard("LOCK", null, "Important information here");
            String accessibleText = card.buildAccessibleText();
            assertThat(accessibleText).contains("Important information here");
        }
    }

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("should create security info card")
        void shouldCreateSecurityInfoCard() {
            InfoCard securityCard = InfoCard.security("Your Security", "You are protected.");

            assertThat(securityCard).isNotNull();
            assertThat(securityCard.getIconType()).isEqualTo("LOCK");
            assertThat(securityCard.getTitle()).isEqualTo("Your Security");
        }

        @Test
        @DisplayName("should create info bullet card")
        void shouldCreateInfoBulletCard() {
            InfoCard bulletCard = InfoCard.infoBullets(
                "Introduction:",
                Arrays.asList("Item 1", "Item 2")
            );

            assertThat(bulletCard).isNotNull();
            assertThat(bulletCard.getIconType()).isEqualTo("SHIELD_ALT");
            assertThat(bulletCard.hasBulletPoints()).isTrue();
        }

        @Test
        @DisplayName("should create numbered steps card")
        void shouldCreateNumberedStepsCard() {
            InfoCard stepsCard = InfoCard.steps(
                "What happens next",
                Arrays.asList("Step 1", "Step 2", "Step 3")
            );

            assertThat(stepsCard).isNotNull();
            assertThat(stepsCard.getIconType()).isEqualTo("INFO_CIRCLE");
            assertThat(stepsCard.isNumbered()).isTrue();
        }
    }
}
