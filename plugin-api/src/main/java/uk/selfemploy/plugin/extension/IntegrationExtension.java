package uk.selfemploy.plugin.extension;

import javafx.scene.Node;

/**
 * Extension point for integrating with external services.
 *
 * <p>Plugins implement this interface to provide integrations with external
 * services such as payment processors, banking APIs, invoice platforms, or
 * cloud storage services. Integrations typically require authentication and
 * provide both configuration UI and data synchronization capabilities.</p>
 *
 * <h2>Integration Types</h2>
 * <ul>
 *   <li><b>Payment Processors</b> - Stripe, PayPal, Square</li>
 *   <li><b>Banking</b> - Open Banking connections</li>
 *   <li><b>Invoicing</b> - Invoice Ninja, Freshbooks</li>
 *   <li><b>Storage</b> - Google Drive, Dropbox for receipts</li>
 *   <li><b>Accounting</b> - Xero, QuickBooks sync</li>
 * </ul>
 *
 * <h2>Integration Lifecycle</h2>
 * <ol>
 *   <li>User enables integration from settings</li>
 *   <li>{@link #getConfigurationView()} provides OAuth/setup UI</li>
 *   <li>User completes authentication</li>
 *   <li>{@link #connect()} establishes connection</li>
 *   <li>{@link #sync()} imports/exports data as needed</li>
 *   <li>{@link #disconnect()} when user disables</li>
 * </ol>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class StripeIntegration implements IntegrationExtension {
 *     @Override
 *     public String getIntegrationId() {
 *         return "stripe-payments";
 *     }
 *
 *     @Override
 *     public String getIntegrationName() {
 *         return "Stripe";
 *     }
 *
 *     @Override
 *     public IntegrationType getIntegrationType() {
 *         return IntegrationType.PAYMENT_PROCESSOR;
 *     }
 *
 *     @Override
 *     public Node getConfigurationView() {
 *         return new StripeSetupPane();
 *     }
 *
 *     @Override
 *     public void connect() {
 *         // Establish OAuth connection
 *     }
 *
 *     @Override
 *     public void sync() {
 *         // Import Stripe payments as income
 *     }
 * }
 * }</pre>
 *
 * @see ExtensionPoint
 */
public interface IntegrationExtension extends ExtensionPoint {

    /**
     * Types of external integrations.
     */
    enum IntegrationType {
        /** Payment processors (Stripe, PayPal, etc.) */
        PAYMENT_PROCESSOR,

        /** Bank connections via Open Banking */
        BANKING,

        /** Invoice and billing platforms */
        INVOICING,

        /** Cloud storage for receipts/documents */
        STORAGE,

        /** Accounting software sync */
        ACCOUNTING,

        /** Other external services */
        OTHER
    }

    /**
     * Connection status for the integration.
     */
    enum ConnectionStatus {
        /** Not yet configured */
        NOT_CONFIGURED,

        /** Configured but not connected */
        DISCONNECTED,

        /** Attempting to connect */
        CONNECTING,

        /** Successfully connected */
        CONNECTED,

        /** Connection failed */
        ERROR
    }

    /**
     * Returns the unique identifier for this integration.
     *
     * <p>The ID must be unique across all integrations.</p>
     *
     * @return the integration ID, never null or blank
     */
    String getIntegrationId();

    /**
     * Returns the display name for this integration.
     *
     * <p>This is the name of the service being integrated (e.g., "Stripe").</p>
     *
     * @return the integration name, never null or blank
     */
    String getIntegrationName();

    /**
     * Returns a description of what this integration provides.
     *
     * <p>This description helps users understand the benefits of enabling
     * this integration.</p>
     *
     * @return the integration description, never null (may be empty)
     */
    default String getIntegrationDescription() {
        return "";
    }

    /**
     * Returns the type of this integration.
     *
     * @return the integration type, never null
     */
    IntegrationType getIntegrationType();

    /**
     * Returns the current connection status.
     *
     * @return the connection status, never null
     */
    ConnectionStatus getConnectionStatus();

    /**
     * Creates the configuration view for setting up this integration.
     *
     * <p>This view is shown when the user enables the integration from
     * settings. It should provide UI for authentication (OAuth flows),
     * API key entry, or other setup requirements.</p>
     *
     * <p>This method is called on the JavaFX Application Thread.</p>
     *
     * @return the configuration UI, never null
     */
    Node getConfigurationView();

    /**
     * Establishes or re-establishes the connection to the external service.
     *
     * <p>This method should verify credentials and prepare the integration
     * for data synchronization. It may be called from a background thread.</p>
     *
     * @throws IntegrationException if connection fails
     */
    void connect();

    /**
     * Disconnects from the external service.
     *
     * <p>This method should revoke any tokens and clean up resources.
     * After disconnect, the status should be {@link ConnectionStatus#DISCONNECTED}.</p>
     */
    void disconnect();

    /**
     * Synchronizes data with the external service.
     *
     * <p>This method imports data from and/or exports data to the external
     * service. The specific behavior depends on the integration type.</p>
     *
     * <p>This method may be called from a background thread.</p>
     *
     * @throws IntegrationException if sync fails
     */
    void sync();

    /**
     * Returns whether automatic synchronization is supported.
     *
     * <p>If true, the application may call {@link #sync()} periodically.</p>
     *
     * @return true if automatic sync is supported
     */
    default boolean supportsAutoSync() {
        return false;
    }
}
