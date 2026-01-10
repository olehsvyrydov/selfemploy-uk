package uk.selfemploy.hmrc.fraud;

/**
 * Base interface for collecting a single fraud prevention header.
 */
public interface HeaderCollector {

    /**
     * Returns the header name this collector provides.
     *
     * @return Header name (e.g., "Gov-Client-Device-ID")
     */
    String getHeaderName();

    /**
     * Collects and returns the header value.
     * Values are automatically URL-encoded by the service.
     *
     * @return Header value, or null if not available
     */
    String collect();

    /**
     * Returns true if this header is mandatory for the connection type.
     *
     * @return true if mandatory
     */
    default boolean isMandatory() {
        return true;
    }
}
