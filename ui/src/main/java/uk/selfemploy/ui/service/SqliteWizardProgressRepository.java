package uk.selfemploy.ui.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation of WizardProgressRepository.
 *
 * <p>Uses AES-256-GCM encryption for sensitive data (NINO).
 * Clock injection is used for testable timestamps.
 *
 * <p>Encryption follows the pattern from EncryptedFileTokenStorage.
 */
public class SqliteWizardProgressRepository implements WizardProgressRepository {

    private static final Logger LOG = Logger.getLogger(SqliteWizardProgressRepository.class.getName());

    // Encryption constants - matching EncryptedFileTokenStorage pattern
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 65536;
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";

    private final Clock clock;
    private final SqliteDataStore dataStore;
    private final SecureRandom secureRandom;
    private final byte[] salt;

    /**
     * Creates a repository with the specified clock for timestamp generation.
     *
     * @param clock The clock to use for timestamps
     */
    public SqliteWizardProgressRepository(Clock clock) {
        this.clock = clock;
        this.dataStore = SqliteDataStore.getInstance();
        this.secureRandom = new SecureRandom();
        this.salt = deriveSalt();
        ensureTableExists();
    }

    /**
     * Creates a repository with the system default clock.
     */
    public SqliteWizardProgressRepository() {
        this(Clock.systemUTC());
    }

    /**
     * Ensures the wizard_progress table exists.
     */
    private void ensureTableExists() {
        try {
            Connection connection = getConnection();
            if (connection != null) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS wizard_progress (
                            id INTEGER PRIMARY KEY,
                            wizard_type TEXT NOT NULL,
                            current_step INTEGER NOT NULL,
                            checklist_state TEXT,
                            nino_entered TEXT,
                            created_at TEXT NOT NULL,
                            updated_at TEXT NOT NULL,
                            UNIQUE(wizard_type)
                        )
                    """);
                    LOG.fine("Ensured wizard_progress table exists");
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to create wizard_progress table", e);
        }
    }

    @Override
    public Optional<WizardProgress> findByType(String wizardType) {
        validateWizardType(wizardType);

        String sql = "SELECT * FROM wizard_progress WHERE wizard_type = ?";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return Optional.empty();
            }

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, wizardType);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to find wizard progress: " + wizardType, e);
        }
        return Optional.empty();
    }

    @Override
    public WizardProgress save(WizardProgress progress) {
        if (progress == null) {
            throw new IllegalArgumentException("Progress cannot be null");
        }

        String sql = """
            INSERT INTO wizard_progress
            (wizard_type, current_step, checklist_state, nino_entered, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(wizard_type) DO UPDATE SET
                current_step = excluded.current_step,
                checklist_state = excluded.checklist_state,
                nino_entered = excluded.nino_entered,
                updated_at = excluded.updated_at
        """;

        try {
            Connection connection = getConnection();
            if (connection == null) {
                throw new IllegalStateException("Database connection not available");
            }

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, progress.wizardType());
                pstmt.setInt(2, progress.currentStep());
                pstmt.setString(3, progress.checklistState());
                pstmt.setString(4, encryptNino(progress.ninoEntered()));
                pstmt.setString(5, progress.createdAt().toString());
                pstmt.setString(6, progress.updatedAt().toString());
                pstmt.executeUpdate();

                LOG.fine("Saved wizard progress: " + progress.wizardType() + " at step " + progress.currentStep());
                return progress;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to save wizard progress", e);
            throw new IllegalStateException("Failed to save wizard progress", e);
        }
    }

    @Override
    public boolean deleteByType(String wizardType) {
        validateWizardType(wizardType);

        String sql = "DELETE FROM wizard_progress WHERE wizard_type = ?";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return false;
            }

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, wizardType);
                int affected = pstmt.executeUpdate();
                if (affected > 0) {
                    LOG.info("Deleted wizard progress: " + wizardType);
                    return true;
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to delete wizard progress: " + wizardType, e);
        }
        return false;
    }

    /**
     * Gets the raw (encrypted) NINO value from the database for testing purposes.
     * This method is package-private and only for use in tests.
     *
     * @param wizardType The wizard type
     * @return The raw encrypted NINO value, or null if not found
     */
    String getRawNinoFromDatabase(String wizardType) {
        String sql = "SELECT nino_entered FROM wizard_progress WHERE wizard_type = ?";
        try {
            Connection connection = getConnection();
            if (connection == null) {
                return null;
            }

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, wizardType);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    return rs.getString("nino_entered");
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to get raw NINO: " + wizardType, e);
        }
        return null;
    }

    private void validateWizardType(String wizardType) {
        if (wizardType == null) {
            throw new IllegalArgumentException("Wizard type cannot be null");
        }
        if (wizardType.isBlank()) {
            throw new IllegalArgumentException("Wizard type cannot be null or blank");
        }
    }

    private WizardProgress mapResultSet(ResultSet rs) throws SQLException {
        String encryptedNino = rs.getString("nino_entered");
        String decryptedNino = decryptNino(encryptedNino);

        return new WizardProgress(
            rs.getString("wizard_type"),
            rs.getInt("current_step"),
            rs.getString("checklist_state"),
            decryptedNino,
            Instant.parse(rs.getString("created_at")),
            Instant.parse(rs.getString("updated_at"))
        );
    }

    // === Encryption Methods (following EncryptedFileTokenStorage pattern) ===

    private String encryptNino(String plainNino) {
        if (plainNino == null || plainNino.isEmpty()) {
            return null;
        }

        try {
            byte[] encrypted = encrypt(plainNino.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            LOG.log(Level.SEVERE, "Failed to encrypt NINO", e);
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    private String decryptNino(String encryptedNino) {
        if (encryptedNino == null || encryptedNino.isEmpty()) {
            return null;
        }

        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedNino);
            byte[] decrypted = decrypt(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            LOG.log(Level.SEVERE, "Failed to decrypt NINO", e);
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Derive key
        SecretKey key = deriveKey();

        // Encrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Prepend IV to ciphertext
        ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);

        return buffer.array();
    }

    private byte[] decrypt(byte[] encryptedData) throws GeneralSecurityException {
        if (encryptedData.length < GCM_IV_LENGTH) {
            throw new GeneralSecurityException("Invalid encrypted data - too short");
        }

        // Extract IV
        ByteBuffer buffer = ByteBuffer.wrap(encryptedData);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        // Extract ciphertext
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        // Derive key
        SecretKey key = deriveKey();

        // Decrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        return cipher.doFinal(ciphertext);
    }

    private SecretKey deriveKey() throws GeneralSecurityException {
        // Derive password from machine identifiers
        String password = deriveMachinePassword();

        // Use PBKDF2 to derive key
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);

        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    private String deriveMachinePassword() {
        // Combine machine-specific identifiers
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("user.name", ""));
        sb.append(System.getProperty("os.name", ""));
        sb.append(System.getProperty("user.home", ""));

        // Add some additional entropy
        try {
            java.net.InetAddress localhost = java.net.InetAddress.getLocalHost();
            sb.append(localhost.getHostName());
        } catch (Exception e) {
            // Ignore - use what we have
        }

        return sb.toString();
    }

    private byte[] deriveSalt() {
        // Use consistent salt based on application name
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest("uk.selfemploy.ui.wizard.progress".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Fallback to hardcoded salt
            return "selfemploy-wizard-progress".getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Gets the database connection using reflection to access SqliteDataStore's private connection.
     */
    private Connection getConnection() {
        try {
            Field connectionField = SqliteDataStore.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            return (Connection) connectionField.get(dataStore);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to get connection via reflection", e);
            return null;
        }
    }
}
