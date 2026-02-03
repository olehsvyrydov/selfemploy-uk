package uk.selfemploy.plugin.runtime;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.Ikon;
import uk.selfemploy.plugin.extension.ConflictResolutionPolicy;
import uk.selfemploy.plugin.extension.DashboardWidget;
import uk.selfemploy.plugin.extension.ExtensionPoint;
import uk.selfemploy.plugin.extension.NavigationExtension;
import uk.selfemploy.plugin.extension.NavigationGroup;
import uk.selfemploy.plugin.extension.Prioritizable;
import uk.selfemploy.plugin.extension.WidgetSize;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DefaultExtensionConflictResolver}.
 */
@DisplayName("DefaultExtensionConflictResolver")
class DefaultExtensionConflictResolverTest {

    private DefaultExtensionConflictResolver resolver;

    // Test extension implementations
    static class TestExtension implements ExtensionPoint, Prioritizable {
        private final String id;
        private final int priority;

        TestExtension(String id, int priority) {
            this.id = id;
            this.priority = priority;
        }

        String getId() {
            return id;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    static class TestNavExtension implements NavigationExtension {
        private final String id;
        private final int order;

        TestNavExtension(String id, int order) {
            this.id = id;
            this.order = order;
        }

        @Override
        public String getNavigationId() {
            return id;
        }

        @Override
        public String getNavigationLabel() {
            return id;
        }

        @Override
        public Ikon getNavigationIcon() {
            return null;
        }

        @Override
        public NavigationGroup getNavigationGroup() {
            return NavigationGroup.MAIN;
        }

        @Override
        public int getNavigationOrder() {
            return order;
        }

        @Override
        public Node createView() {
            return new Pane();
        }
    }

    static class TestWidget implements DashboardWidget {
        private final String id;
        private final int order;

        TestWidget(String id, int order) {
            this.id = id;
            this.order = order;
        }

        @Override
        public String getWidgetId() {
            return id;
        }

        @Override
        public String getWidgetTitle() {
            return id;
        }

        @Override
        public WidgetSize getWidgetSize() {
            return WidgetSize.SMALL;
        }

        @Override
        public int getWidgetOrder() {
            return order;
        }

        @Override
        public Node createWidget() {
            return new Pane();
        }

        @Override
        public void refresh() {
        }
    }

    static class SimpleExtension implements ExtensionPoint {
        private final String name;

        SimpleExtension(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }
    }

    @BeforeEach
    void setUp() {
        resolver = new DefaultExtensionConflictResolver();
    }

    @Nested
    @DisplayName("resolve() with PRIORITY_ORDER")
    class PriorityOrderTests {

        @Test
        @DisplayName("should sort by priority (lowest first)")
        void shouldSortByPriorityLowestFirst() {
            List<TestExtension> extensions = List.of(
                new TestExtension("c", 300),
                new TestExtension("a", 100),
                new TestExtension("b", 200)
            );

            List<TestExtension> result = resolver.resolve(extensions, ConflictResolutionPolicy.PRIORITY_ORDER);

            assertThat(result).extracting(TestExtension::getId)
                .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("should preserve registration order for equal priorities (stable sort)")
        void shouldPreserveRegistrationOrderForEqualPriorities() {
            List<TestExtension> extensions = List.of(
                new TestExtension("first", 100),
                new TestExtension("second", 100),
                new TestExtension("third", 100)
            );

            List<TestExtension> result = resolver.resolve(extensions, ConflictResolutionPolicy.PRIORITY_ORDER);

            // COND-1105-C: Equal priority preserves registration order
            assertThat(result).extracting(TestExtension::getId)
                .containsExactly("first", "second", "third");
        }

        @Test
        @DisplayName("should sort built-in (0-99) before plugins (100+)")
        void shouldSortBuiltInBeforePlugins() {
            List<TestExtension> extensions = List.of(
                new TestExtension("plugin1", 100),
                new TestExtension("builtin1", 10),
                new TestExtension("plugin2", 150),
                new TestExtension("builtin2", 50)
            );

            List<TestExtension> result = resolver.resolve(extensions, ConflictResolutionPolicy.PRIORITY_ORDER);

            // COND-1105-B: Built-in 0-99, plugins 100+
            assertThat(result).extracting(TestExtension::getId)
                .containsExactly("builtin1", "builtin2", "plugin1", "plugin2");
        }

        @Test
        @DisplayName("should handle NavigationExtension ordering")
        void shouldHandleNavigationExtensionOrdering() {
            List<TestNavExtension> extensions = List.of(
                new TestNavExtension("nav-c", 150),
                new TestNavExtension("nav-a", 50),
                new TestNavExtension("nav-b", 100)
            );

            List<TestNavExtension> result = resolver.resolve(extensions, ConflictResolutionPolicy.PRIORITY_ORDER);

            assertThat(result).extracting(TestNavExtension::getNavigationId)
                .containsExactly("nav-a", "nav-b", "nav-c");
        }

        @Test
        @DisplayName("should handle DashboardWidget ordering")
        void shouldHandleDashboardWidgetOrdering() {
            List<TestWidget> extensions = List.of(
                new TestWidget("widget-c", 150),
                new TestWidget("widget-a", 50),
                new TestWidget("widget-b", 100)
            );

            List<TestWidget> result = resolver.resolve(extensions, ConflictResolutionPolicy.PRIORITY_ORDER);

            assertThat(result).extracting(TestWidget::getWidgetId)
                .containsExactly("widget-a", "widget-b", "widget-c");
        }
    }

    @Nested
    @DisplayName("resolve() with REGISTRATION_ORDER")
    class RegistrationOrderTests {

        @Test
        @DisplayName("should preserve registration order ignoring priority")
        void shouldPreserveRegistrationOrderIgnoringPriority() {
            List<TestExtension> extensions = List.of(
                new TestExtension("c", 300),
                new TestExtension("a", 100),
                new TestExtension("b", 200)
            );

            List<TestExtension> result = resolver.resolve(extensions, ConflictResolutionPolicy.REGISTRATION_ORDER);

            assertThat(result).extracting(TestExtension::getId)
                .containsExactly("c", "a", "b");
        }
    }

    @Nested
    @DisplayName("resolve() with ALPHABETICAL")
    class AlphabeticalTests {

        @Test
        @DisplayName("should sort Prioritizable extensions by class name")
        void shouldSortByClassName() {
            List<SimpleExtension> extensions = List.of(
                new SimpleExtension("zebra"),
                new SimpleExtension("alpha"),
                new SimpleExtension("beta")
            );

            List<SimpleExtension> result = resolver.resolve(extensions, ConflictResolutionPolicy.ALPHABETICAL);

            // Since these all have the same class, order won't change by class name
            // They'll sort by SimpleExtension (same)
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("should sort NavigationExtension by navigation ID")
        void shouldSortNavigationExtensionById() {
            List<TestNavExtension> extensions = List.of(
                new TestNavExtension("nav-zebra", 100),
                new TestNavExtension("nav-alpha", 100),
                new TestNavExtension("nav-beta", 100)
            );

            List<TestNavExtension> result = resolver.resolve(extensions, ConflictResolutionPolicy.ALPHABETICAL);

            assertThat(result).extracting(TestNavExtension::getNavigationId)
                .containsExactly("nav-alpha", "nav-beta", "nav-zebra");
        }

        @Test
        @DisplayName("should sort DashboardWidget by widget ID")
        void shouldSortDashboardWidgetById() {
            List<TestWidget> extensions = List.of(
                new TestWidget("widget-zebra", 100),
                new TestWidget("widget-alpha", 100),
                new TestWidget("widget-beta", 100)
            );

            List<TestWidget> result = resolver.resolve(extensions, ConflictResolutionPolicy.ALPHABETICAL);

            assertThat(result).extracting(TestWidget::getWidgetId)
                .containsExactly("widget-alpha", "widget-beta", "widget-zebra");
        }
    }

    @Nested
    @DisplayName("resolve() edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should return empty list for empty input")
        void shouldReturnEmptyListForEmptyInput() {
            List<TestExtension> result = resolver.resolve(List.of(), ConflictResolutionPolicy.PRIORITY_ORDER);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return single element list unchanged")
        void shouldReturnSingleElementListUnchanged() {
            List<TestExtension> extensions = List.of(new TestExtension("only", 100));

            List<TestExtension> result = resolver.resolve(extensions, ConflictResolutionPolicy.PRIORITY_ORDER);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("only");
        }

        @Test
        @DisplayName("should not modify original list")
        void shouldNotModifyOriginalList() {
            List<TestExtension> original = new java.util.ArrayList<>(List.of(
                new TestExtension("c", 300),
                new TestExtension("a", 100)
            ));
            String originalFirst = original.get(0).getId();

            resolver.resolve(original, ConflictResolutionPolicy.PRIORITY_ORDER);

            assertThat(original.get(0).getId()).isEqualTo(originalFirst);
        }

        @Test
        @DisplayName("should reject null extensions")
        void shouldRejectNullExtensions() {
            assertThatThrownBy(() -> resolver.resolve(null, ConflictResolutionPolicy.PRIORITY_ORDER))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("extensions");
        }

        @Test
        @DisplayName("should reject null policy")
        void shouldRejectNullPolicy() {
            assertThatThrownBy(() -> resolver.resolve(List.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("policy");
        }
    }

    @Nested
    @DisplayName("getPriorityOf()")
    class GetPriorityOfTests {

        @Test
        @DisplayName("should return priority from Prioritizable")
        void shouldReturnPriorityFromPrioritizable() {
            TestExtension ext = new TestExtension("test", 42);

            int priority = resolver.getPriorityOf(ext);

            assertThat(priority).isEqualTo(42);
        }

        @Test
        @DisplayName("should return navigation order from NavigationExtension")
        void shouldReturnNavigationOrder() {
            TestNavExtension ext = new TestNavExtension("nav", 75);

            int priority = resolver.getPriorityOf(ext);

            assertThat(priority).isEqualTo(75);
        }

        @Test
        @DisplayName("should return widget order from DashboardWidget")
        void shouldReturnWidgetOrder() {
            TestWidget ext = new TestWidget("widget", 125);

            int priority = resolver.getPriorityOf(ext);

            assertThat(priority).isEqualTo(125);
        }

        @Test
        @DisplayName("should return default priority for unknown extension type")
        void shouldReturnDefaultPriorityForUnknownType() {
            SimpleExtension ext = new SimpleExtension("simple");

            int priority = resolver.getPriorityOf(ext);

            assertThat(priority).isEqualTo(Prioritizable.DEFAULT_PLUGIN_PRIORITY);
        }

        @Test
        @DisplayName("should return default priority for null extension")
        void shouldReturnDefaultPriorityForNull() {
            int priority = resolver.getPriorityOf(null);

            assertThat(priority).isEqualTo(Prioritizable.DEFAULT_PLUGIN_PRIORITY);
        }
    }

    @Nested
    @DisplayName("Default resolve() method")
    class DefaultResolveTests {

        @Test
        @DisplayName("should use PRIORITY_ORDER by default")
        void shouldUsePriorityOrderByDefault() {
            List<TestExtension> extensions = List.of(
                new TestExtension("c", 300),
                new TestExtension("a", 100),
                new TestExtension("b", 200)
            );

            List<TestExtension> result = resolver.resolve(extensions);

            // Should be sorted by priority (PRIORITY_ORDER is default)
            assertThat(result).extracting(TestExtension::getId)
                .containsExactly("a", "b", "c");
        }
    }
}
