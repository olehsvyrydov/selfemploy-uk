/**
 * Extension point interfaces for plugin functionality.
 *
 * <p>This package contains the {@link uk.selfemploy.plugin.extension.ExtensionPoint}
 * marker interface and all concrete extension point interfaces that define how
 * plugins can extend the application's functionality.</p>
 *
 * <h2>Available Extension Points</h2>
 *
 * <h3>UI Extensions</h3>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.extension.NavigationExtension} - Add navigation items and pages to the sidebar</li>
 *   <li>{@link uk.selfemploy.plugin.extension.DashboardWidget} - Add widgets to the dashboard</li>
 * </ul>
 *
 * <h3>Data Extensions</h3>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.extension.DataImporter} - Import bank statements and transactions</li>
 *   <li>{@link uk.selfemploy.plugin.extension.DataExporter} - Export data in various formats</li>
 *   <li>{@link uk.selfemploy.plugin.extension.ReportGenerator} - Generate custom reports (PDF, Excel)</li>
 * </ul>
 *
 * <h3>Business Logic Extensions</h3>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.extension.TaxCalculatorExtension} - Override/extend tax calculations (e.g., Scottish tax)</li>
 *   <li>{@link uk.selfemploy.plugin.extension.ExpenseCategoryExtension} - Add custom expense categories</li>
 * </ul>
 *
 * <h3>Integration Extensions</h3>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.extension.IntegrationExtension} - Integrate with external services (payments, banking)</li>
 *   <li>{@link uk.selfemploy.plugin.extension.HmrcApiExtension} - Extend HMRC API interactions (VAT, partnerships)</li>
 * </ul>
 *
 * <h2>Supporting Types</h2>
 * <p>Each extension point has associated context, result, and exception types:</p>
 * <ul>
 *   <li>{@link uk.selfemploy.plugin.extension.ReportContext} - Parameters for report generation</li>
 *   <li>{@link uk.selfemploy.plugin.extension.ImportContext} / {@link uk.selfemploy.plugin.extension.ImportResult} - Import parameters and results</li>
 *   <li>{@link uk.selfemploy.plugin.extension.ExportContext} - Export parameters</li>
 *   <li>{@link uk.selfemploy.plugin.extension.TaxContext} / {@link uk.selfemploy.plugin.extension.TaxResult} - Tax calculation parameters and results</li>
 * </ul>
 *
 * <h2>Implementing Extensions</h2>
 * <p>To implement an extension:</p>
 * <ol>
 *   <li>Create a class that implements the specific extension point interface</li>
 *   <li>Register it with the application through {@link uk.selfemploy.plugin.api.PluginContext}</li>
 * </ol>
 *
 * <pre>{@code
 * public class MyPlugin implements Plugin {
 *     @Override
 *     public void onLoad(PluginContext context) {
 *         context.registerExtension(DashboardWidget.class, new MyDashboardWidget());
 *     }
 * }
 * }</pre>
 *
 * @see uk.selfemploy.plugin.extension.ExtensionPoint
 * @see uk.selfemploy.plugin.api.PluginContext
 */
package uk.selfemploy.plugin.extension;
