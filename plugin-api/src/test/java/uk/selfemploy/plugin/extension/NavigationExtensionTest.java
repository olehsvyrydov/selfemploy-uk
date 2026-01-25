package uk.selfemploy.plugin.extension;

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.Ikon;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NavigationExtension} interface.
 */
@DisplayName("NavigationExtension")
class NavigationExtensionTest {

    @Nested
    @DisplayName("NavigationGroup enum")
    class NavigationGroupEnum {

        @Test
        @DisplayName("should have expected groups")
        void shouldHaveExpectedGroups() {
            assertThat(NavigationGroup.values()).containsExactlyInAnyOrder(
                NavigationGroup.MAIN,
                NavigationGroup.REPORTS,
                NavigationGroup.INTEGRATIONS,
                NavigationGroup.SETTINGS
            );
        }

        @Test
        @DisplayName("should have display names")
        void shouldHaveDisplayNames() {
            assertThat(NavigationGroup.MAIN.getDisplayName()).isEqualTo("Main");
            assertThat(NavigationGroup.REPORTS.getDisplayName()).isEqualTo("Reports");
            assertThat(NavigationGroup.INTEGRATIONS.getDisplayName()).isEqualTo("Integrations");
            assertThat(NavigationGroup.SETTINGS.getDisplayName()).isEqualTo("Settings");
        }
    }

    @Nested
    @DisplayName("when implementing extension")
    class Implementation {

        @Test
        @DisplayName("should be implementable")
        void shouldBeImplementable() {
            NavigationExtension extension = new TestNavigationExtension();

            assertThat(extension.getNavigationId()).isEqualTo("test-nav");
            assertThat(extension.getNavigationLabel()).isEqualTo("Test Navigation");
            assertThat(extension.getNavigationGroup()).isEqualTo(NavigationGroup.INTEGRATIONS);
        }

        @Test
        @DisplayName("should have default navigation order")
        void shouldHaveDefaultNavigationOrder() {
            NavigationExtension extension = new TestNavigationExtension();

            assertThat(extension.getNavigationOrder()).isEqualTo(100);
        }

        @Test
        @DisplayName("should allow null icon")
        void shouldAllowNullIcon() {
            NavigationExtension extension = new TestNavigationExtension();

            assertThat(extension.getNavigationIcon()).isNull();
        }

        @Test
        @DisplayName("should extend ExtensionPoint")
        void shouldExtendExtensionPoint() {
            NavigationExtension extension = new TestNavigationExtension();

            assertThat(extension).isInstanceOf(ExtensionPoint.class);
        }
    }

    /**
     * Test implementation of NavigationExtension.
     */
    private static class TestNavigationExtension implements NavigationExtension {
        @Override
        public String getNavigationId() {
            return "test-nav";
        }

        @Override
        public String getNavigationLabel() {
            return "Test Navigation";
        }

        @Override
        public Ikon getNavigationIcon() {
            return null; // Using null to avoid Ikonli runtime dependency in tests
        }

        @Override
        public NavigationGroup getNavigationGroup() {
            return NavigationGroup.INTEGRATIONS;
        }

        @Override
        public Node createView() {
            return new Label("Test View");
        }
    }
}
