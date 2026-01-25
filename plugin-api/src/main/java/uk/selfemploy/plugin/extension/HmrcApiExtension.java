package uk.selfemploy.plugin.extension;

import java.util.Map;

/**
 * Extension point for extending HMRC API interactions.
 *
 * <p>Plugins implement this interface to add support for additional HMRC
 * MTD (Making Tax Digital) APIs or to customize how data is submitted
 * to existing APIs. This enables support for different business types
 * or additional tax obligations.</p>
 *
 * <h2>HMRC API Categories</h2>
 * <ul>
 *   <li><b>Self Assessment</b> - Individual tax returns (SA103)</li>
 *   <li><b>VAT</b> - VAT returns for registered businesses</li>
 *   <li><b>Partnership</b> - Partnership returns (SA800)</li>
 *   <li><b>Corporation Tax</b> - Company tax returns</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Adding VAT return submission for VAT-registered businesses</li>
 *   <li>Supporting partnership returns for business partners</li>
 *   <li>Custom fraud prevention header generation</li>
 *   <li>Additional supplementary pages for Self Assessment</li>
 * </ul>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class VatApiExtension implements HmrcApiExtension {
 *     @Override
 *     public String getExtensionId() {
 *         return "hmrc-vat";
 *     }
 *
 *     @Override
 *     public String getExtensionName() {
 *         return "VAT Returns";
 *     }
 *
 *     @Override
 *     public HmrcApiType getApiType() {
 *         return HmrcApiType.VAT;
 *     }
 *
 *     @Override
 *     public boolean canSubmit(HmrcSubmissionContext context) {
 *         return context.isVatRegistered();
 *     }
 *
 *     @Override
 *     public HmrcSubmissionResult submit(HmrcSubmissionContext context) {
 *         // Submit VAT return
 *         return result;
 *     }
 * }
 * }</pre>
 *
 * <h2>Security</h2>
 * <p>HMRC API extensions require special permissions and must be signed
 * by trusted publishers. OAuth tokens are managed by the core application.</p>
 *
 * @see ExtensionPoint
 */
public interface HmrcApiExtension extends ExtensionPoint {

    /**
     * Types of HMRC APIs that can be extended.
     */
    enum HmrcApiType {
        /** Self Assessment returns (SA100, SA103, etc.) */
        SELF_ASSESSMENT,

        /** VAT returns under MTD */
        VAT,

        /** Partnership returns (SA800) */
        PARTNERSHIP,

        /** Corporation Tax returns */
        CORPORATION_TAX,

        /** Other HMRC APIs */
        OTHER
    }

    /**
     * Returns the unique identifier for this extension.
     *
     * <p>The ID must be unique across all HMRC API extensions.</p>
     *
     * @return the extension ID, never null or blank
     */
    String getExtensionId();

    /**
     * Returns the display name for this extension.
     *
     * <p>This name is shown when listing available HMRC submission options.</p>
     *
     * @return the extension name, never null or blank
     */
    String getExtensionName();

    /**
     * Returns a description of what this extension provides.
     *
     * <p>This description helps users understand what tax obligations
     * this extension handles.</p>
     *
     * @return the extension description, never null (may be empty)
     */
    default String getExtensionDescription() {
        return "";
    }

    /**
     * Returns the type of HMRC API this extension handles.
     *
     * @return the API type, never null
     */
    HmrcApiType getApiType();

    /**
     * Returns whether this extension can handle submission for the given context.
     *
     * <p>Implementations should check the context to determine if this
     * extension applies (e.g., is the user VAT registered?).</p>
     *
     * @param context the submission context
     * @return true if this extension can handle the submission
     */
    boolean canSubmit(HmrcSubmissionContext context);

    /**
     * Submits data to HMRC using this extension's API.
     *
     * <p>This method performs the actual API call to HMRC. The OAuth token
     * is obtained from the application's token manager.</p>
     *
     * <p>This method may be called from a background thread.</p>
     *
     * @param context the submission context with data and credentials
     * @return the submission result
     * @throws HmrcApiException if submission fails
     */
    HmrcSubmissionResult submit(HmrcSubmissionContext context);

    /**
     * Returns the required OAuth scopes for this API.
     *
     * <p>These scopes are requested when the user authorizes the application
     * with HMRC.</p>
     *
     * @return list of required OAuth scopes
     */
    default java.util.List<String> getRequiredScopes() {
        return java.util.Collections.emptyList();
    }

    /**
     * Context for HMRC submissions.
     *
     * @param taxYear      the tax year for submission
     * @param accessToken  the OAuth access token (provided by application)
     * @param sandbox      whether to use sandbox APIs
     * @param submissionData the data to submit (extension-specific format)
     */
    record HmrcSubmissionContext(
        int taxYear,
        String accessToken,
        boolean sandbox,
        Map<String, Object> submissionData
    ) {
        /**
         * Returns a submission data value cast to the expected type.
         *
         * @param key          the data key
         * @param defaultValue the default value if not set
         * @param <T>          the value type
         * @return the value or default
         */
        @SuppressWarnings("unchecked")
        public <T> T getData(String key, T defaultValue) {
            Object value = submissionData.get(key);
            return value != null ? (T) value : defaultValue;
        }
    }

    /**
     * Result of an HMRC submission.
     *
     * @param success        whether the submission was accepted
     * @param correlationId  HMRC correlation ID for tracking
     * @param receiptId      HMRC receipt ID (if successful)
     * @param errorCode      HMRC error code (if failed)
     * @param errorMessage   human-readable error message (if failed)
     * @param metadata       additional response data
     */
    record HmrcSubmissionResult(
        boolean success,
        String correlationId,
        String receiptId,
        String errorCode,
        String errorMessage,
        Map<String, Object> metadata
    ) {
        /**
         * Creates a successful submission result.
         *
         * @param correlationId HMRC correlation ID
         * @param receiptId     HMRC receipt ID
         * @return a success result
         */
        public static HmrcSubmissionResult success(String correlationId, String receiptId) {
            return new HmrcSubmissionResult(true, correlationId, receiptId, null, null, java.util.Collections.emptyMap());
        }

        /**
         * Creates a failed submission result.
         *
         * @param correlationId HMRC correlation ID
         * @param errorCode     HMRC error code
         * @param errorMessage  human-readable error message
         * @return a failure result
         */
        public static HmrcSubmissionResult failure(String correlationId, String errorCode, String errorMessage) {
            return new HmrcSubmissionResult(false, correlationId, null, errorCode, errorMessage, java.util.Collections.emptyMap());
        }
    }
}
