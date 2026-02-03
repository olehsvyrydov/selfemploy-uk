package uk.selfemploy.plugin.api;

/**
 * Marker interface for plugin-provided services.
 *
 * <p>All services registered with the {@link ServiceRegistry} must implement
 * this interface. This ensures type safety and allows the framework to
 * distinguish plugin services from other objects.</p>
 *
 * <h2>Implementing a Service</h2>
 * <pre>{@code
 * public interface BankImportService extends PluginService {
 *     List<Transaction> importTransactions(File csvFile);
 * }
 *
 * public class BarclaysBankImport implements BankImportService {
 *     @Override
 *     public List<Transaction> importTransactions(File csvFile) {
 *         // Implementation
 *     }
 * }
 * }</pre>
 *
 * <h2>Registering a Service</h2>
 * <pre>{@code
 * @Override
 * public void onLoad(PluginContext context) {
 *     context.getServiceRegistry().register(
 *         BankImportService.class,
 *         new BarclaysBankImport(),
 *         context.getPluginId()
 *     );
 * }
 * }</pre>
 *
 * @see ServiceRegistry
 * @see ServiceReference
 */
public interface PluginService {
    // Marker interface - no methods required
}
