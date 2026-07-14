package uk.selfemploy.ui.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    /**
     * Writes {@code content} to {@code file} so it is owner-only from the moment it exists,
     * replacing any current file atomically where the platform supports it.
     *
     * <p>The bytes are first written to a sibling temporary file — which the JDK creates with
     * owner-only permissions on POSIX — and then moved into place, so a secret such as the master
     * key is never briefly visible under the process umask the way a plain {@code Files.write}
     * followed by a permission change would leave it.</p>
     *
     * @param file    the destination path
     * @param content the bytes to write
     * @throws IOException if the temporary file cannot be written or moved into place
     */
    public static void writeRestricted(Path file, byte[] content) throws IOException {
        Path parent = file.getParent();
        // Files.createTempFile creates the file owner-only on POSIX, so the secret is never
        // written to a world-readable path; the atomic move carries those permissions to the
        // destination, and restrictFile is re-applied afterwards for non-POSIX filesystems.
        Path temp = parent != null
            ? Files.createTempFile(parent, ".tmp-", null)
            : Files.createTempFile(".tmp-", null);
        try {
            Files.write(temp, content);
            try {
                Files.move(temp, file,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            restrictFile(file);
        } finally {
            Files.deleteIfExists(temp);
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
