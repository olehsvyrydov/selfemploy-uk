package uk.selfemploy.plugin.extension;

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DashboardWidget} interface.
 */
@DisplayName("DashboardWidget")
class DashboardWidgetTest {

    @Nested
    @DisplayName("WidgetSize enum")
    class WidgetSizeEnum {

        @Test
        @DisplayName("should have expected sizes")
        void shouldHaveExpectedSizes() {
            assertThat(WidgetSize.values()).containsExactlyInAnyOrder(
                WidgetSize.SMALL,
                WidgetSize.MEDIUM,
                WidgetSize.LARGE,
                WidgetSize.CUSTOM
            );
        }

        @Test
        @DisplayName("should have correct grid spans")
        void shouldHaveCorrectGridSpans() {
            assertThat(WidgetSize.SMALL.getGridSpan()).isEqualTo(1);
            assertThat(WidgetSize.MEDIUM.getGridSpan()).isEqualTo(2);
            assertThat(WidgetSize.LARGE.getGridSpan()).isEqualTo(4);
            assertThat(WidgetSize.CUSTOM.getGridSpan()).isZero();
        }
    }

    @Nested
    @DisplayName("when implementing widget")
    class Implementation {

        @Test
        @DisplayName("should be implementable")
        void shouldBeImplementable() {
            DashboardWidget widget = new TestDashboardWidget();

            assertThat(widget.getWidgetId()).isEqualTo("test-widget");
            assertThat(widget.getWidgetTitle()).isEqualTo("Test Widget");
            assertThat(widget.getWidgetSize()).isEqualTo(WidgetSize.MEDIUM);
        }

        @Test
        @DisplayName("should have default widget order")
        void shouldHaveDefaultWidgetOrder() {
            DashboardWidget widget = new TestDashboardWidget();

            assertThat(widget.getWidgetOrder()).isEqualTo(100);
        }

        @Test
        @DisplayName("should extend ExtensionPoint")
        void shouldExtendExtensionPoint() {
            DashboardWidget widget = new TestDashboardWidget();

            assertThat(widget).isInstanceOf(ExtensionPoint.class);
        }
    }

    /**
     * Test implementation of DashboardWidget.
     */
    private static class TestDashboardWidget implements DashboardWidget {
        private boolean refreshed = false;

        @Override
        public String getWidgetId() {
            return "test-widget";
        }

        @Override
        public String getWidgetTitle() {
            return "Test Widget";
        }

        @Override
        public WidgetSize getWidgetSize() {
            return WidgetSize.MEDIUM;
        }

        @Override
        public Node createWidget() {
            return new Label("Test Widget Content");
        }

        @Override
        public void refresh() {
            refreshed = true;
        }

        public boolean isRefreshed() {
            return refreshed;
        }
    }
}
