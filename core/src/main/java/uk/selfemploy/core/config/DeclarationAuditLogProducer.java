package uk.selfemploy.core.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import uk.selfemploy.core.audit.DeclarationAuditLog;
import uk.selfemploy.core.audit.FileSystemDeclarationAuditLog;
import uk.selfemploy.core.audit.NinoHasher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * CDI producer for {@link DeclarationAuditLog} — SLFEMPUK-35 / S17-11.
 *
 * <p>Resolves the audit log location following the same platform convention used
 * by {@code TokenStorageService}:
 * <ul>
 *   <li>Windows: {@code %APPDATA%\SelfEmployment\audit\declarations.jsonl}</li>
 *   <li>macOS: {@code ~/Library/Application Support/SelfEmployment/audit/declarations.jsonl}</li>
 *   <li>Linux: {@code $XDG_DATA_HOME/selfemployment/audit/declarations.jsonl}
 *       (falling back to {@code ~/.local/share/selfemployment/audit/declarations.jsonl})</li>
 * </ul>
 *
 * <p>An explicit override may be supplied via the
 * {@code selfemploy.audit.declarations.path} configuration property (used by
 * tests and headless environments).
 */
@ApplicationScoped
public class DeclarationAuditLogProducer {

    @ConfigProperty(name = "selfemploy.audit.declarations.path")
    Optional<String> configuredPath;

    @Produces
    @ApplicationScoped
    public DeclarationAuditLog declarationAuditLog() throws IOException {
        Path baseDir = resolveAuditDirectory(configuredPath.orElse(null));
        Path logFile = baseDir.resolve("declarations.jsonl");
        Path saltFile = baseDir.resolve(".nino-salt");
        NinoHasher hasher = new NinoHasher(saltFile);
        return new FileSystemDeclarationAuditLog(logFile, hasher);
    }

    private static Path resolveAuditDirectory(String configuredPath) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Paths.get(configuredPath);
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home", ".");

        Path basePath;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            basePath = appData != null
                    ? Paths.get(appData, "SelfEmployment")
                    : Paths.get(userHome, "AppData", "Roaming", "SelfEmployment");
        } else if (os.contains("mac")) {
            basePath = Paths.get(userHome, "Library", "Application Support", "SelfEmployment");
        } else {
            String xdgData = System.getenv("XDG_DATA_HOME");
            basePath = xdgData != null
                    ? Paths.get(xdgData, "selfemployment")
                    : Paths.get(userHome, ".local", "share", "selfemployment");
        }
        return basePath.resolve("audit");
    }
}
