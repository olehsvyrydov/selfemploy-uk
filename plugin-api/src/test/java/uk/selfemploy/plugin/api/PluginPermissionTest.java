package uk.selfemploy.plugin.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PluginPermission}.
 */
@DisplayName("PluginPermission")
class PluginPermissionTest {

    @Nested
    @DisplayName("Permission values")
    class PermissionValues {

        @Test
        @DisplayName("FILE_ACCESS permission exists")
        void fileAccessPermission() {
            assertThat(PluginPermission.FILE_ACCESS)
                .isNotNull();
            assertThat(PluginPermission.FILE_ACCESS.getDisplayName())
                .isEqualTo("File System Access");
            assertThat(PluginPermission.FILE_ACCESS.getDescription())
                .contains("file");
        }

        @Test
        @DisplayName("NETWORK_ACCESS permission exists")
        void networkAccessPermission() {
            assertThat(PluginPermission.NETWORK_ACCESS)
                .isNotNull();
            assertThat(PluginPermission.NETWORK_ACCESS.getDisplayName())
                .isEqualTo("Network Access");
            assertThat(PluginPermission.NETWORK_ACCESS.getDescription())
                .contains("network");
        }

        @Test
        @DisplayName("HMRC_API permission exists")
        void hmrcApiPermission() {
            assertThat(PluginPermission.HMRC_API)
                .isNotNull();
            assertThat(PluginPermission.HMRC_API.getDisplayName())
                .isEqualTo("HMRC API Access");
            assertThat(PluginPermission.HMRC_API.getDescription())
                .contains("HMRC");
        }

        @Test
        @DisplayName("DATA_READ permission exists")
        void dataReadPermission() {
            assertThat(PluginPermission.DATA_READ)
                .isNotNull();
            assertThat(PluginPermission.DATA_READ.getDisplayName())
                .isEqualTo("Read Transaction Data");
        }

        @Test
        @DisplayName("DATA_WRITE permission exists")
        void dataWritePermission() {
            assertThat(PluginPermission.DATA_WRITE)
                .isNotNull();
            assertThat(PluginPermission.DATA_WRITE.getDisplayName())
                .isEqualTo("Write Transaction Data");
        }

        @Test
        @DisplayName("SETTINGS_READ permission exists")
        void settingsReadPermission() {
            assertThat(PluginPermission.SETTINGS_READ)
                .isNotNull();
            assertThat(PluginPermission.SETTINGS_READ.getDisplayName())
                .isEqualTo("Read User Settings");
        }

        @Test
        @DisplayName("SETTINGS_WRITE permission exists")
        void settingsWritePermission() {
            assertThat(PluginPermission.SETTINGS_WRITE)
                .isNotNull();
            assertThat(PluginPermission.SETTINGS_WRITE.getDisplayName())
                .isEqualTo("Write User Settings");
        }

        @Test
        @DisplayName("UI_EXTENSION permission exists")
        void uiExtensionPermission() {
            assertThat(PluginPermission.UI_EXTENSION)
                .isNotNull();
            assertThat(PluginPermission.UI_EXTENSION.getDisplayName())
                .isEqualTo("UI Extension");
        }
    }

    @Nested
    @DisplayName("Permission properties")
    class PermissionProperties {

        @Test
        @DisplayName("All permissions have non-null display name")
        void allHaveDisplayName() {
            for (PluginPermission permission : PluginPermission.values()) {
                assertThat(permission.getDisplayName())
                    .isNotNull()
                    .isNotBlank();
            }
        }

        @Test
        @DisplayName("All permissions have non-null description")
        void allHaveDescription() {
            for (PluginPermission permission : PluginPermission.values()) {
                assertThat(permission.getDescription())
                    .isNotNull()
                    .isNotBlank();
            }
        }

        @Test
        @DisplayName("All permissions have defined sensitivity level")
        void allHaveSensitivityLevel() {
            for (PluginPermission permission : PluginPermission.values()) {
                assertThat(permission.getSensitivity())
                    .isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Sensitivity levels")
    class SensitivityLevels {

        @Test
        @DisplayName("HMRC_API is HIGH sensitivity")
        void hmrcApiIsHighSensitivity() {
            assertThat(PluginPermission.HMRC_API.getSensitivity())
                .isEqualTo(PluginPermission.Sensitivity.HIGH);
        }

        @Test
        @DisplayName("DATA_WRITE is MEDIUM sensitivity")
        void dataWriteIsMediumSensitivity() {
            assertThat(PluginPermission.DATA_WRITE.getSensitivity())
                .isEqualTo(PluginPermission.Sensitivity.MEDIUM);
        }

        @Test
        @DisplayName("UI_EXTENSION is LOW sensitivity")
        void uiExtensionIsLowSensitivity() {
            assertThat(PluginPermission.UI_EXTENSION.getSensitivity())
                .isEqualTo(PluginPermission.Sensitivity.LOW);
        }
    }

    @Nested
    @DisplayName("Permission parsing")
    class PermissionParsing {

        @Test
        @DisplayName("Can parse permission by name")
        void parseByName() {
            PluginPermission permission = PluginPermission.valueOf("HMRC_API");
            assertThat(permission).isEqualTo(PluginPermission.HMRC_API);
        }

        @Test
        @DisplayName("Can get all high sensitivity permissions")
        void getHighSensitivityPermissions() {
            Set<PluginPermission> highSensitivity = PluginPermission.getPermissionsBySensitivity(
                PluginPermission.Sensitivity.HIGH
            );

            assertThat(highSensitivity)
                .contains(PluginPermission.HMRC_API);
        }
    }
}
