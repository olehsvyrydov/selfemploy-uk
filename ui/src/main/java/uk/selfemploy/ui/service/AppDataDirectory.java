package uk.selfemploy.ui.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves the per-user application data directory and restricts access to its owner.
 *
 * <p>The directory holds the database, which contains OAuth tokens and taxpayer identifiers, so it
 * must not be readable by other users of the machine. Permissions are applied on a best-effort
 * basis: POSIX systems get owner-only bits; on Windows, where POSIX permissions are unsupported,
 * the directory inherits the user-private ACLs of {@code %APPDATA%}.</p>
 */
public final class AppDataDirectory {

    private static final Logger LOG = Logger.getLogger(AppDataDirectory.class.getName());

    private static final String OWNER_ONLY_DIR = "rwx------";
    private static final String OWNER_ONLY_FILE = "rw-------";

    private AppDataDirectory() {
    }

    /**
     * Returns the platform-appropriate data directory for this application. The directory is not
     * created by this call.
     *
     * @return the data directory path
     */
    public static Path resolve() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return appData != null
                ? Paths.get(appData, "SelfEmployment")
                : Paths.get(userHome, "AppData", "Roaming", "SelfEmployment");
        }
        if (os.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", "SelfEmployment");
        }
        String xdgData = System.getenv("XDG_DATA_HOME");
        return xdgData != null
            ? Paths.get(xdgData, "selfemployment")
            : Paths.get(userHome, ".local", "share", "selfemployment");
    }

    /**
     * Creates the directory if absent and restricts it to its owner.
     *
     * @param directory the directory to create and protect
     */
    public static void createRestricted(Path directory) {
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                LOG.info("Created data directory: " + directory);
            }
            restrict(directory, OWNER_ONLY_DIR);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to create data directory " + directory, e);
        }
    }

    /**
     * Restricts an existing file to owner-only read/write. Silently does nothing if the file is
     * absent, so it is safe to call for database sidecars that may not exist yet.
     *
     * @param file the file to protect
     */
    public static void restrictFile(Path file) {
        if (Files.exists(file)) {
            restrict(file, OWNER_ONLY_FILE);
        }
    }

    private static void restrict(Path path, String posixPermissions) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(posixPermissions));
        } catch (UnsupportedOperationException windows) {
            // Non-POSIX filesystem: the path inherits the user-private ACLs of its parent.
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not restrict permissions on " + path, e);
        }
    }
}
