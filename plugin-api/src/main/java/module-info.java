/**
 * UK Self-Employment Manager Plugin API module.
 *
 * <p>This module provides the Plugin SDK for developing extensions to the
 * UK Self-Employment Manager application. It is designed to be published
 * to Maven Central for third-party plugin developers.</p>
 *
 * <h2>Package Overview</h2>
 * <ul>
 *   <li>{@code uk.selfemploy.plugin.api} - Core plugin interfaces and types</li>
 *   <li>{@code uk.selfemploy.plugin.extension} - Extension point interfaces</li>
 * </ul>
 *
 * <h2>Extension Points</h2>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.extension.NavigationExtension} - Add navigation pages</li>
 *   <li>{@link uk.selfemploy.plugin.extension.DashboardWidget} - Add dashboard widgets</li>
 *   <li>{@link uk.selfemploy.plugin.extension.ReportGenerator} - Generate custom reports</li>
 *   <li>{@link uk.selfemploy.plugin.extension.DataImporter} - Import bank/CSV data</li>
 *   <li>{@link uk.selfemploy.plugin.extension.DataExporter} - Export data</li>
 *   <li>{@link uk.selfemploy.plugin.extension.TaxCalculatorExtension} - Custom tax calculations</li>
 *   <li>{@link uk.selfemploy.plugin.extension.ExpenseCategoryExtension} - Custom expense categories</li>
 *   <li>{@link uk.selfemploy.plugin.extension.IntegrationExtension} - External service integrations</li>
 *   <li>{@link uk.selfemploy.plugin.extension.HmrcApiExtension} - Extend HMRC interactions</li>
 * </ul>
 *
 * <h2>Getting Started</h2>
 * <p>Plugin developers should:</p>
 * <ol>
 *   <li>Add this module as a dependency</li>
 *   <li>Implement the {@link uk.selfemploy.plugin.api.Plugin} interface</li>
 *   <li>Implement desired extension point interfaces</li>
 *   <li>Create a ServiceLoader configuration file</li>
 *   <li>Package as a JAR with a plugin.yaml manifest</li>
 * </ol>
 *
 * @see uk.selfemploy.plugin.api.Plugin
 * @see uk.selfemploy.plugin.api.PluginContext
 * @see uk.selfemploy.plugin.extension.ExtensionPoint
 */
module uk.selfemploy.plugin.api {
    // Required modules
    requires javafx.controls;
    requires org.kordamp.ikonli.core;

    // Exported packages
    exports uk.selfemploy.plugin.api;
    exports uk.selfemploy.plugin.extension;

    // ServiceLoader support for plugin discovery
    uses uk.selfemploy.plugin.api.Plugin;
}
