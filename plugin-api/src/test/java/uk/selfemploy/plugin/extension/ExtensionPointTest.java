package uk.selfemploy.plugin.extension;

import javafx.scene.Node;
import javafx.scene.control.Label;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.Ikon;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExtensionPoint} interface and its implementations.
 */
@DisplayName("ExtensionPoint")
class ExtensionPointTest {

    @Nested
    @DisplayName("as marker interface")
    class AsMarkerInterface {

        @Test
        @DisplayName("should be implementable as a marker")
        void shouldBeImplementableAsMarker() {
            ExtensionPoint extension = new TestExtension();
            assertThat(extension).isInstanceOf(ExtensionPoint.class);
        }

        @Test
        @DisplayName("should allow concrete extension types")
        void shouldAllowConcreteExtensionTypes() {
            ExtensionPoint extension = new ConcreteExtension();
            assertThat(extension).isInstanceOf(ExtensionPoint.class);
            assertThat(extension).isInstanceOf(ConcreteExtension.class);
        }
    }

    @Nested
    @DisplayName("all extension point interfaces")
    class AllExtensionPoints {

        @Test
        @DisplayName("NavigationExtension should extend ExtensionPoint")
        void navigationExtensionShouldExtendExtensionPoint() {
            NavigationExtension extension = new TestNavigationExtension();
            assertThat(extension).isInstanceOf(ExtensionPoint.class);
        }

        @Test
        @DisplayName("DashboardWidget should extend ExtensionPoint")
        void dashboardWidgetShouldExtendExtensionPoint() {
            DashboardWidget widget = new TestDashboardWidget();
            assertThat(widget).isInstanceOf(ExtensionPoint.class);
        }

        @Test
        @DisplayName("ReportGenerator should extend ExtensionPoint")
        void reportGeneratorShouldExtendExtensionPoint() {
            ReportGenerator generator = new TestReportGenerator();
            assertThat(generator).isInstanceOf(ExtensionPoint.class);
        }

        @Test
        @DisplayName("DataImporter should extend ExtensionPoint")
        void dataImporterShouldExtendExtensionPoint() {
            DataImporter importer = new TestDataImporter();
            assertThat(importer).isInstanceOf(ExtensionPoint.class);
        }

        @Test
        @DisplayName("DataExporter should extend ExtensionPoint")
        void dataExporterShouldExtendExtensionPoint() {
            DataExporter exporter = new TestDataExporter();
            assertThat(exporter).isInstanceOf(ExtensionPoint.class);
        }

        @Test
        @DisplayName("TaxCalculatorExtension should extend ExtensionPoint")
        void taxCalculatorExtensionShouldExtendExtensionPoint() {
            TaxCalculatorExtension calculator = new TestTaxCalculatorExtension();
            assertThat(calculator).isInstanceOf(ExtensionPoint.class);
        }

        @Test
        @DisplayName("ExpenseCategoryExtension should extend ExtensionPoint")
        void expenseCategoryExtensionShouldExtendExtensionPoint() {
            ExpenseCategoryExtension extension = new TestExpenseCategoryExtension();
            assertThat(extension).isInstanceOf(ExtensionPoint.class);
        }

        @Test
        @DisplayName("IntegrationExtension should extend ExtensionPoint")
        void integrationExtensionShouldExtendExtensionPoint() {
            IntegrationExtension integration = new TestIntegrationExtension();
            assertThat(integration).isInstanceOf(ExtensionPoint.class);
        }

        @Test
        @DisplayName("HmrcApiExtension should extend ExtensionPoint")
        void hmrcApiExtensionShouldExtendExtensionPoint() {
            HmrcApiExtension extension = new TestHmrcApiExtension();
            assertThat(extension).isInstanceOf(ExtensionPoint.class);
        }
    }

    // Test implementations

    private static class TestExtension implements ExtensionPoint {}
    private static class ConcreteExtension implements ExtensionPoint {}

    private static class TestNavigationExtension implements NavigationExtension {
        @Override public String getNavigationId() { return "test"; }
        @Override public String getNavigationLabel() { return "Test"; }
        @Override public Ikon getNavigationIcon() { return null; }
        @Override public NavigationGroup getNavigationGroup() { return NavigationGroup.MAIN; }
        @Override public Node createView() { return new Label(); }
    }

    private static class TestDashboardWidget implements DashboardWidget {
        @Override public String getWidgetId() { return "test"; }
        @Override public String getWidgetTitle() { return "Test"; }
        @Override public WidgetSize getWidgetSize() { return WidgetSize.SMALL; }
        @Override public Node createWidget() { return new Label(); }
        @Override public void refresh() {}
    }

    private static class TestReportGenerator implements ReportGenerator {
        @Override public String getReportId() { return "test"; }
        @Override public String getReportName() { return "Test"; }
        @Override public String getReportDescription() { return ""; }
        @Override public List<String> getSupportedFormats() { return List.of("PDF"); }
        @Override public byte[] generateReport(ReportContext context, String format) { return new byte[0]; }
    }

    private static class TestDataImporter implements DataImporter {
        @Override public String getImporterId() { return "test"; }
        @Override public String getImporterName() { return "Test"; }
        @Override public List<String> getSupportedFileTypes() { return List.of(".csv"); }
        @Override public ImportResult importData(Path file, ImportContext context) { return ImportResult.success(0, 0); }
    }

    private static class TestDataExporter implements DataExporter {
        @Override public String getExporterId() { return "test"; }
        @Override public String getExporterName() { return "Test"; }
        @Override public List<String> getSupportedFormats() { return List.of("CSV"); }
        @Override public byte[] exportData(ExportContext context, String format) { return new byte[0]; }
    }

    private static class TestTaxCalculatorExtension implements TaxCalculatorExtension {
        @Override public String getCalculatorId() { return "test"; }
        @Override public String getCalculatorName() { return "Test"; }
        @Override public boolean appliesTo(TaxContext context) { return false; }
        @Override public TaxResult calculateTax(TaxContext context) { return TaxResult.zero(); }
    }

    private static class TestExpenseCategoryExtension implements ExpenseCategoryExtension {
        @Override public String getExtensionId() { return "test"; }
        @Override public String getExtensionName() { return "Test"; }
        @Override public List<ExpenseCategory> getCategories() { return List.of(); }
    }

    private static class TestIntegrationExtension implements IntegrationExtension {
        @Override public String getIntegrationId() { return "test"; }
        @Override public String getIntegrationName() { return "Test"; }
        @Override public IntegrationType getIntegrationType() { return IntegrationType.OTHER; }
        @Override public ConnectionStatus getConnectionStatus() { return ConnectionStatus.NOT_CONFIGURED; }
        @Override public Node getConfigurationView() { return new Label(); }
        @Override public void connect() {}
        @Override public void disconnect() {}
        @Override public void sync() {}
    }

    private static class TestHmrcApiExtension implements HmrcApiExtension {
        @Override public String getExtensionId() { return "test"; }
        @Override public String getExtensionName() { return "Test"; }
        @Override public HmrcApiType getApiType() { return HmrcApiType.SELF_ASSESSMENT; }
        @Override public boolean canSubmit(HmrcSubmissionContext context) { return false; }
        @Override public HmrcSubmissionResult submit(HmrcSubmissionContext context) { return HmrcSubmissionResult.failure("", "", ""); }
    }
}
