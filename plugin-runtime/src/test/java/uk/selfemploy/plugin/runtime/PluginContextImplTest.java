package uk.selfemploy.plugin.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.plugin.api.PluginPermission;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PluginContextImpl")
class PluginContextImplTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Creates context with valid parameters")
        void createsWithValidParams() {
            PluginContextImpl context = new PluginContextImpl("1.0.0", tempDir);

            assertThat(context.getAppVersion()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("Throws on null appVersion")
        void throwsOnNullAppVersion() {
            assertThatThrownBy(() -> new PluginContextImpl(null, tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appVersion");
        }

        @Test
        @DisplayName("Throws on blank appVersion")
        void throwsOnBlankAppVersion() {
            assertThatThrownBy(() -> new PluginContextImpl("  ", tempDir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("appVersion");
        }

        @Test
        @DisplayName("Throws on null pluginDataDirectory")
        void throwsOnNullDirectory() {
            assertThatThrownBy(() -> new PluginContextImpl("1.0.0", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("pluginDataDirectory");
        }
    }

    @Nested
    @DisplayName("Plugin data directory")
    class PluginDataDirectory {

        @Test
        @DisplayName("Returns the configured directory")
        void returnsConfiguredDirectory() {
            Path dataDir = tempDir.resolve("plugin-data");
            PluginContextImpl context = new PluginContextImpl("1.0.0", dataDir);

            assertThat(context.getPluginDataDirectory()).isEqualTo(dataDir);
        }

        @Test
        @DisplayName("Creates directory if it does not exist")
        void createsDirectoryIfNotExists() {
            Path dataDir = tempDir.resolve("new-plugin-data");
            PluginContextImpl context = new PluginContextImpl("1.0.0", dataDir);

            assertThat(Files.exists(dataDir)).isFalse();

            Path result = context.getPluginDataDirectory();

            assertThat(result).isEqualTo(dataDir);
            assertThat(Files.exists(dataDir)).isTrue();
            assertThat(Files.isDirectory(dataDir)).isTrue();
        }

        @Test
        @DisplayName("Does not fail if directory already exists")
        void doesNotFailIfExists() throws Exception {
            Path dataDir = tempDir.resolve("existing-dir");
            Files.createDirectories(dataDir);

            PluginContextImpl context = new PluginContextImpl("1.0.0", dataDir);

            assertThat(context.getPluginDataDirectory()).isEqualTo(dataDir);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Builds context with all parameters")
        void buildsWithAllParams() {
            PluginContextImpl context = PluginContextImpl.builder()
                .appVersion("2.0.0")
                .baseDataDirectory(tempDir)
                .pluginId("my-plugin")
                .build();

            assertThat(context.getAppVersion()).isEqualTo("2.0.0");
            assertThat(context.getPluginDataDirectory().toString())
                .contains("my-plugin");
        }

        @Test
        @DisplayName("Sanitizes plugin ID for directory name")
        void sanitizesPluginId() {
            PluginContextImpl context = PluginContextImpl.builder()
                .appVersion("1.0.0")
                .baseDataDirectory(tempDir)
                .pluginId("uk.selfemploy.plugin/special:chars")
                .build();

            // Should replace invalid chars with underscore
            assertThat(context.getPluginDataDirectory().getFileName().toString())
                .doesNotContain("/")
                .doesNotContain(":");
        }

        @Test
        @DisplayName("Throws on null baseDataDirectory")
        void throwsOnNullBaseDir() {
            assertThatThrownBy(() -> PluginContextImpl.builder()
                .appVersion("1.0.0")
                .pluginId("test")
                .build()
            ).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("baseDataDirectory");
        }

        @Test
        @DisplayName("Throws on null pluginId")
        void throwsOnNullPluginId() {
            assertThatThrownBy(() -> PluginContextImpl.builder()
                .appVersion("1.0.0")
                .baseDataDirectory(tempDir)
                .build()
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId");
        }

        @Test
        @DisplayName("Throws on blank pluginId")
        void throwsOnBlankPluginId() {
            assertThatThrownBy(() -> PluginContextImpl.builder()
                .appVersion("1.0.0")
                .baseDataDirectory(tempDir)
                .pluginId("  ")
                .build()
            ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pluginId");
        }
    }

    @Test
    @DisplayName("toString includes key information")
    void toStringIncludesInfo() {
        PluginContextImpl context = new PluginContextImpl("1.0.0", tempDir);

        String str = context.toString();
        assertThat(str)
            .contains("1.0.0")
            .contains(tempDir.toString());
    }

    @Nested
    @DisplayName("Permissions")
    class PermissionTests {

        @Test
        @DisplayName("Returns empty permissions by default")
        void returnsEmptyPermissionsByDefault() {
            PluginContextImpl context = new PluginContextImpl("1.0.0", tempDir);

            assertThat(context.getGrantedPermissions()).isEmpty();
        }

        @Test
        @DisplayName("Returns granted permissions")
        void returnsGrantedPermissions() {
            Set<PluginPermission> permissions = Set.of(
                PluginPermission.DATA_READ,
                PluginPermission.UI_EXTENSION
            );
            PluginContextImpl context = new PluginContextImpl("1.0.0", tempDir, permissions);

            assertThat(context.getGrantedPermissions())
                .containsExactlyInAnyOrder(
                    PluginPermission.DATA_READ,
                    PluginPermission.UI_EXTENSION
                );
        }

        @Test
        @DisplayName("hasPermission returns true for granted permission")
        void hasPermissionReturnsTrue() {
            Set<PluginPermission> permissions = Set.of(PluginPermission.DATA_READ);
            PluginContextImpl context = new PluginContextImpl("1.0.0", tempDir, permissions);

            assertThat(context.hasPermission(PluginPermission.DATA_READ)).isTrue();
        }

        @Test
        @DisplayName("hasPermission returns false for non-granted permission")
        void hasPermissionReturnsFalse() {
            Set<PluginPermission> permissions = Set.of(PluginPermission.DATA_READ);
            PluginContextImpl context = new PluginContextImpl("1.0.0", tempDir, permissions);

            assertThat(context.hasPermission(PluginPermission.DATA_WRITE)).isFalse();
        }

        @Test
        @DisplayName("requirePermission does not throw for granted permission")
        void requirePermissionDoesNotThrow() {
            Set<PluginPermission> permissions = Set.of(PluginPermission.HMRC_API);
            PluginContextImpl context = new PluginContextImpl("1.0.0", tempDir, permissions);

            // Should not throw
            context.requirePermission(PluginPermission.HMRC_API);
        }

        @Test
        @DisplayName("requirePermission throws for non-granted permission")
        void requirePermissionThrows() {
            Set<PluginPermission> permissions = Set.of(PluginPermission.DATA_READ);
            PluginContextImpl context = new PluginContextImpl("1.0.0", tempDir, permissions);

            assertThatThrownBy(() -> context.requirePermission(PluginPermission.HMRC_API))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("HMRC_API")
                .hasMessageContaining("not been granted");
        }

        @Test
        @DisplayName("Granted permissions are immutable")
        void grantedPermissionsAreImmutable() {
            Set<PluginPermission> permissions = Set.of(PluginPermission.DATA_READ);
            PluginContextImpl context = new PluginContextImpl("1.0.0", tempDir, permissions);

            assertThatThrownBy(() ->
                context.getGrantedPermissions().add(PluginPermission.DATA_WRITE)
            ).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Builder supports granted permissions")
        void builderSupportsPermissions() {
            Set<PluginPermission> permissions = Set.of(
                PluginPermission.NETWORK_ACCESS,
                PluginPermission.FILE_ACCESS
            );

            PluginContextImpl context = PluginContextImpl.builder()
                .appVersion("1.0.0")
                .baseDataDirectory(tempDir)
                .pluginId("test-plugin")
                .grantedPermissions(permissions)
                .build();

            assertThat(context.getGrantedPermissions())
                .containsExactlyInAnyOrder(
                    PluginPermission.NETWORK_ACCESS,
                    PluginPermission.FILE_ACCESS
                );
        }
    }

    @Nested
    @DisplayName("Security - Path Traversal Prevention (SEC-001 to SEC-005)")
    class PathTraversalPrevention {

        @Test
        @DisplayName("SEC-001: Plugin ID with path traversal '../' - slashes are sanitized")
        void pathTraversalDoubleDotSlashSanitized() {
            PluginContextImpl context = PluginContextImpl.builder()
                .appVersion("1.0.0")
                .baseDataDirectory(tempDir)
                .pluginId("../../../etc/passwd")
                .build();

            Path dataDir = context.getPluginDataDirectory();

            // Forward slashes are replaced with underscores, preventing traversal
            assertThat(dataDir.getFileName().toString()).doesNotContain("/");
            // Path still stays under base directory (no actual traversal)
            assertThat(dataDir.normalize().startsWith(tempDir)).isTrue();
        }

        @Test
        @DisplayName("SEC-002: Plugin ID with absolute path is sanitized")
        void absolutePathSanitized() {
            PluginContextImpl context = PluginContextImpl.builder()
                .appVersion("1.0.0")
                .baseDataDirectory(tempDir)
                .pluginId("/etc/passwd")
                .build();

            Path dataDir = context.getPluginDataDirectory();

            // Path should stay under base directory
            assertThat(dataDir.normalize().startsWith(tempDir)).isTrue();
            assertThat(dataDir.getFileName().toString()).doesNotStartWith("/etc");
        }

        @Test
        @DisplayName("SEC-003: Plugin ID special characters are sanitized")
        void specialCharactersSanitized() {
            PluginContextImpl context = PluginContextImpl.builder()
                .appVersion("1.0.0")
                .baseDataDirectory(tempDir)
                .pluginId("plugin<>:\"\\|?*name")
                .build();

            String dirName = context.getPluginDataDirectory().getFileName().toString();

            // Invalid filesystem characters should be replaced
            assertThat(dirName).doesNotContain("<");
            assertThat(dirName).doesNotContain(">");
            assertThat(dirName).doesNotContain(":");
            assertThat(dirName).doesNotContain("\"");
            assertThat(dirName).doesNotContain("\\");
            assertThat(dirName).doesNotContain("|");
            assertThat(dirName).doesNotContain("?");
            assertThat(dirName).doesNotContain("*");
        }

        @Test
        @DisplayName("SEC-004: Plugin data directory is sandboxed under base directory")
        void pluginDataDirectoryIsSandboxed() {
            PluginContextImpl context = PluginContextImpl.builder()
                .appVersion("1.0.0")
                .baseDataDirectory(tempDir)
                .pluginId("test-plugin")
                .build();

            Path dataDir = context.getPluginDataDirectory();

            // Data directory must be under the configured base
            assertThat(dataDir.startsWith(tempDir)).isTrue();
        }

        @Test
        @DisplayName("SEC-005: sanitizeForFilesystem only allows safe characters")
        void sanitizeAllowsOnlySafeCharacters() {
            // Test various characters
            String[] testCases = {
                "valid-plugin_name.v1",  // All safe characters
                "plugin/with/slashes",    // Slashes should be replaced
                "plugin:colon",           // Colon should be replaced
                "plugin spaces",          // Spaces should be replaced
                "plugin@special#chars",   // Special chars should be replaced
            };

            for (String pluginId : testCases) {
                PluginContextImpl context = PluginContextImpl.builder()
                    .appVersion("1.0.0")
                    .baseDataDirectory(tempDir)
                    .pluginId(pluginId)
                    .build();

                String dirName = context.getPluginDataDirectory().getFileName().toString();

                // Verify only safe characters remain
                assertThat(dirName).matches("[a-zA-Z0-9._-]+");
            }
        }
    }
}
