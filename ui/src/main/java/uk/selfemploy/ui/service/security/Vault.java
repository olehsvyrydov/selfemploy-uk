package uk.selfemploy.ui.service.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.selfemploy.ui.service.AppDataDirectory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The on-disk key vault: the small file that lets a passphrase (or recovery code) unlock the database
 * key without holding the key or the passphrase itself. It sits <em>outside</em> the encrypted database
 * (next to it in the app-data directory) because it is needed to open the database.
 *
 * <p>Each {@link Slot} stores an independent AES-256-GCM-wrapped copy of the <em>same</em> random
 * database key, so a passphrase change or a recovery code re-wraps one 32-byte secret rather than
 * re-encrypting the whole database. The presence of the vault file is what marks protection as enabled;
 * no vault means the database is plaintext (unchanged behaviour).
 *
 * <p>The container itself is not secret — its integrity is enforced by the GCM tag on each slot (with
 * the KDF parameters bound in as additional authenticated data), and a wrong passphrase simply fails
 * the tag. The file is written owner-only (0600) via {@link AppDataDirectory#writeRestricted}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Vault(int vaultVersion, String cipher, List<Slot> slots) {

    /** Current vault format version. */
    public static final int VERSION = 1;
    public static final String CIPHER = "AES-256-GCM";
    public static final String SLOT_PASSPHRASE = "passphrase";
    public static final String SLOT_RECOVERY = "recovery";

    private static final String FILE_NAME = "selfemploy.vault";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** One unlock method: a KDF (salt + params) and the GCM-wrapped database key it protects. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Slot(String name, KdfParams kdf, String nonceB64, String wrappedKeyB64) {}

    /** Argon2id parameters for a slot. Stored so parameters can be upgraded across versions. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KdfParams(String algo, int memoryKib, int iterations, int parallelism, String saltB64) {}

    /** The canonical vault file location, a sibling of the database in the app-data directory. */
    public static Path defaultPath() {
        return AppDataDirectory.resolve().resolve(FILE_NAME);
    }

    /** Reads the vault, or {@code null} if no vault file exists (protection not enabled). */
    public static Vault read(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        return MAPPER.readValue(Files.readAllBytes(path), Vault.class);
    }

    /** Writes the vault owner-only (0600) and atomically. */
    public void write(Path path) throws IOException {
        AppDataDirectory.writeRestricted(path, MAPPER.writeValueAsBytes(this));
    }

    public Slot slot(String name) {
        if (slots == null) {
            return null;
        }
        return slots.stream().filter(s -> name.equals(s.name())).findFirst().orElse(null);
    }

    /** Returns a copy of this vault with {@code slot} added or replacing an existing slot of the same name. */
    public Vault withSlot(Slot slot) {
        List<Slot> next = new ArrayList<>();
        if (slots != null) {
            slots.stream().filter(s -> !slot.name().equals(s.name())).forEach(next::add);
        }
        next.add(slot);
        return new Vault(vaultVersion, cipher, next);
    }

    /** The additional authenticated data binding a wrap to its version, slot and KDF params. */
    static byte[] aad(int version, Slot slot) {
        KdfParams k = slot.kdf();
        String s = version + "|" + slot.name() + "|" + k.algo() + "|" + k.memoryKib()
                + "|" + k.iterations() + "|" + k.parallelism() + "|" + k.saltB64();
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
