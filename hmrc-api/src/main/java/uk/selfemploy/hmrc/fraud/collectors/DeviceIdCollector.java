package uk.selfemploy.hmrc.fraud.collectors;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.selfemploy.hmrc.fraud.FraudPreventionHeaders;
import uk.selfemploy.hmrc.fraud.HeaderCollector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Collects a unique device identifier.
 * Uses hardware-based identifiers where available, falls back to a persistent UUID.
 */
@ApplicationScoped
public class DeviceIdCollector implements HeaderCollector {

    private static final Logger log = LoggerFactory.getLogger(DeviceIdCollector.class);

    private String cachedDeviceId;

    @Override
    public String getHeaderName() {
        return FraudPreventionHeaders.Headers.DEVICE_ID;
    }

    @Override
    public String collect() {
        if (cachedDeviceId == null) {
            cachedDeviceId = generateDeviceId();
        }
        return cachedDeviceId;
    }

    private String generateDeviceId() {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            String hardwareId = null;
            if (os.contains("win")) {
                hardwareId = getWindowsDeviceId();
            } else if (os.contains("mac")) {
                hardwareId = getMacOsDeviceId();
            } else if (os.contains("nix") || os.contains("nux")) {
                hardwareId = getLinuxDeviceId();
            }

            if (hardwareId != null && !hardwareId.isBlank()) {
                // Hash the hardware ID for privacy
                return hashToUuid(hardwareId);
            }
        } catch (Exception e) {
            log.warn("Failed to get hardware device ID, using fallback", e);
        }

        // Fallback: generate based on system properties
        return generateFallbackDeviceId();
    }

    private String getWindowsDeviceId() {
        try {
            // Use WMIC to get machine GUID
            Process process = Runtime.getRuntime().exec(
                "wmic csproduct get UUID");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.equalsIgnoreCase("UUID")) {
                        return line;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get Windows UUID", e);
        }
        return null;
    }

    private String getMacOsDeviceId() {
        try {
            // Use ioreg to get hardware UUID
            Process process = Runtime.getRuntime().exec(
                "ioreg -rd1 -c IOPlatformExpertDevice");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("IOPlatformUUID")) {
                        // Extract UUID from the line
                        int start = line.indexOf("\"", line.indexOf("="));
                        int end = line.lastIndexOf("\"");
                        if (start > 0 && end > start) {
                            return line.substring(start + 1, end);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get macOS UUID", e);
        }
        return null;
    }

    private String getLinuxDeviceId() {
        try {
            // Try to read machine-id
            Process process = Runtime.getRuntime().exec("cat /etc/machine-id");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String machineId = reader.readLine();
                if (machineId != null && !machineId.isBlank()) {
                    return machineId.trim();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get Linux machine-id", e);
        }
        return null;
    }

    private String generateFallbackDeviceId() {
        // Generate a deterministic ID based on system properties
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("user.name", ""));
        sb.append(System.getProperty("os.name", ""));
        sb.append(System.getProperty("os.arch", ""));
        sb.append(System.getProperty("user.home", ""));

        try {
            java.net.InetAddress localhost = java.net.InetAddress.getLocalHost();
            sb.append(localhost.getHostName());
        } catch (Exception e) {
            // Ignore
        }

        return hashToUuid(sb.toString());
    }

    private String hashToUuid(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // Use first 16 bytes to create a UUID
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xff);
            }

            return new UUID(msb, lsb).toString();
        } catch (Exception e) {
            // Last resort: random UUID
            return UUID.randomUUID().toString();
        }
    }
}
