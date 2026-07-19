package uk.selfemploy.ui.service;

import java.io.File;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * How this copy of the application was installed, used to tailor the update instructions.
 *
 * <p>Detection is a best-effort heuristic based on the running code's location and the presence of
 * an AppImage runtime marker; it cannot always distinguish a native package's exact format, so
 * ambiguous cases fall back to {@link #UNKNOWN} with generic guidance.</p>
 */
public enum InstallType {

    /** Packaged as an AppImage (the {@code APPIMAGE} runtime variable is present). */
    APPIMAGE("update.guidance.appimage"),

    /** Installed from a Debian package into a system location. */
    DEB("update.guidance.deb"),

    /** Installed from an RPM package into a system location. */
    RPM("update.guidance.rpm"),

    /** Run from a portable jar outside a system location. */
    JAR("update.guidance.generic"),

    /** Run from a build/IDE working tree (classes on disk, or a {@code target} directory). */
    DEVELOPMENT("update.guidance.generic"),

    /** The install method could not be determined. */
    UNKNOWN("update.guidance.generic");

    private static final Logger LOG = Logger.getLogger(InstallType.class.getName());

    private final String guidanceKey;

    InstallType(String guidanceKey) {
        this.guidanceKey = guidanceKey;
    }

    /**
     * The i18n key for the update instruction appropriate to this install type.
     */
    public String guidanceKey() {
        return guidanceKey;
    }

    /**
     * Detects the install type of the running application from its environment.
     */
    public static InstallType detect() {
        return detect(
                System.getenv("APPIMAGE"),
                codeSourcePath(),
                new File("/etc/debian_version").exists(),
                new File("/etc/redhat-release").exists() || new File("/etc/fedora-release").exists());
    }

    /**
     * Pure detection logic, decoupled from the real environment so it can be unit-tested.
     *
     * @param appImageEnv    the value of the {@code APPIMAGE} variable, or {@code null} if unset
     * @param codeSourcePath the filesystem path the running code was loaded from, or {@code null}
     * @param debianMarker   whether a Debian package marker file is present on the host
     * @param redhatMarker   whether a Red Hat / Fedora package marker file is present on the host
     * @return the detected install type
     */
    static InstallType detect(String appImageEnv, String codeSourcePath,
                              boolean debianMarker, boolean redhatMarker) {
        if (appImageEnv != null && !appImageEnv.isBlank()) {
            return APPIMAGE;
        }
        if (codeSourcePath == null || codeSourcePath.isBlank()) {
            return UNKNOWN;
        }
        String path = codeSourcePath.toLowerCase(Locale.ROOT);
        if (path.contains("/target/") || path.endsWith("/classes") || path.endsWith("/classes/")) {
            return DEVELOPMENT;
        }
        boolean systemPath = path.startsWith("/usr/") || path.startsWith("/opt/");
        if (systemPath) {
            if (debianMarker) {
                return DEB;
            }
            if (redhatMarker) {
                return RPM;
            }
            return UNKNOWN;
        }
        if (path.endsWith(".jar")) {
            return JAR;
        }
        return UNKNOWN;
    }

    private static String codeSourcePath() {
        try {
            CodeSource source = InstallType.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return null;
            }
            return Paths.get(source.getLocation().toURI()).toString();
        } catch (Exception e) {
            LOG.fine("Could not determine code source location: " + e.getMessage());
            return null;
        }
    }
}
