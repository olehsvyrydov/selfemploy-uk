package uk.selfemploy.hmrc.fraud;

import java.util.Map;

/**
 * Interface for collecting fraud prevention header values.
 * Each collector is responsible for gathering specific information
 * required by HMRC for fraud detection.
 */
public interface FraudPreventionHeaders {

    /**
     * Header names as defined by HMRC.
     */
    interface Headers {
        String CONNECTION_METHOD = "Gov-Client-Connection-Method";
        String DEVICE_ID = "Gov-Client-Device-ID";
        String USER_IDS = "Gov-Client-User-IDs";
        String TIMEZONE = "Gov-Client-Timezone";
        String LOCAL_IPS = "Gov-Client-Local-IPs";
        String MAC_ADDRESSES = "Gov-Client-MAC-Addresses";
        String SCREENS = "Gov-Client-Screens";
        String WINDOW_SIZE = "Gov-Client-Window-Size";
        String USER_AGENT = "Gov-Client-Browser-JS-User-Agent";
        String VENDOR_VERSION = "Gov-Vendor-Version";
        String VENDOR_LICENSE_IDS = "Gov-Vendor-License-IDs";
        String VENDOR_PUBLIC_IP = "Gov-Vendor-Public-IP";
        String VENDOR_FORWARDED = "Gov-Vendor-Forwarded";
        String VENDOR_PRODUCT_NAME = "Gov-Vendor-Product-Name";
    }

    /**
     * Connection method for desktop applications calling HMRC directly.
     */
    String CONNECTION_METHOD_DESKTOP_APP_DIRECT = "DESKTOP_APP_DIRECT";

    /**
     * Returns all fraud prevention headers as a map.
     *
     * @return Map of header names to values
     */
    Map<String, String> getHeaders();

    /**
     * Returns the header name this collector provides.
     *
     * @return Header name
     */
    String getHeaderName();

    /**
     * Returns the header value.
     * Values should be URL-encoded where necessary.
     *
     * @return Header value
     */
    String getHeaderValue();
}
