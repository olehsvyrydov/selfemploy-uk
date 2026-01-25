package uk.selfemploy.plugin.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PluginDescriptor}.
 * Tests validate the plugin metadata record and its validation logic.
 */
@DisplayName("PluginDescriptor")
class PluginDescriptorTest {

    private static final String VALID_ID = "uk.selfemploy.plugin.test";
    private static final String VALID_NAME = "Test Plugin";
    private static final String VALID_VERSION = "1.0.0";
    private static final String VALID_DESCRIPTION = "A test plugin for unit testing";
    private static final String VALID_AUTHOR = "Test Author";
    private static final String VALID_MIN_APP_VERSION = "0.1.0";

    @Nested
    @DisplayName("when creating a valid descriptor")
    class ValidDescriptor {

        @Test
        @DisplayName("should create descriptor with all fields")
        void shouldCreateDescriptorWithAllFields() {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID,
                VALID_NAME,
                VALID_VERSION,
                VALID_DESCRIPTION,
                VALID_AUTHOR,
                VALID_MIN_APP_VERSION
            );

            assertThat(descriptor.id()).isEqualTo(VALID_ID);
            assertThat(descriptor.name()).isEqualTo(VALID_NAME);
            assertThat(descriptor.version()).isEqualTo(VALID_VERSION);
            assertThat(descriptor.description()).isEqualTo(VALID_DESCRIPTION);
            assertThat(descriptor.author()).isEqualTo(VALID_AUTHOR);
            assertThat(descriptor.minAppVersion()).isEqualTo(VALID_MIN_APP_VERSION);
        }

        @Test
        @DisplayName("should have correct toString representation")
        void shouldHaveCorrectToString() {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID,
                VALID_NAME,
                VALID_VERSION,
                VALID_DESCRIPTION,
                VALID_AUTHOR,
                VALID_MIN_APP_VERSION
            );

            String toString = descriptor.toString();

            assertThat(toString).contains(VALID_ID);
            assertThat(toString).contains(VALID_NAME);
            assertThat(toString).contains(VALID_VERSION);
        }

        @Test
        @DisplayName("should support equality")
        void shouldSupportEquality() {
            PluginDescriptor descriptor1 = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            );
            PluginDescriptor descriptor2 = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            );

            assertThat(descriptor1).isEqualTo(descriptor2);
            assertThat(descriptor1.hashCode()).isEqualTo(descriptor2.hashCode());
        }

        @Test
        @DisplayName("should not be equal to descriptor with different id")
        void shouldNotBeEqualWithDifferentId() {
            PluginDescriptor descriptor1 = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            );
            PluginDescriptor descriptor2 = new PluginDescriptor(
                "different.id", VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            );

            assertThat(descriptor1).isNotEqualTo(descriptor2);
        }
    }

    @Nested
    @DisplayName("when validating id")
    class IdValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should reject null or blank id")
        void shouldRejectNullOrBlankId(String invalidId) {
            assertThatThrownBy(() -> new PluginDescriptor(
                invalidId, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "uk.selfemploy.plugin.test",
            "com.example.myplugin",
            "my-plugin-id",
            "plugin_v1"
        })
        @DisplayName("should accept valid id formats")
        void shouldAcceptValidIdFormats(String validId) {
            PluginDescriptor descriptor = new PluginDescriptor(
                validId, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            );

            assertThat(descriptor.id()).isEqualTo(validId);
        }
    }

    @Nested
    @DisplayName("when validating name")
    class NameValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should reject null or blank name")
        void shouldRejectNullOrBlankName(String invalidName) {
            assertThatThrownBy(() -> new PluginDescriptor(
                VALID_ID, invalidName, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        }
    }

    @Nested
    @DisplayName("when validating version")
    class VersionValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should reject null or blank version")
        void shouldRejectNullOrBlankVersion(String invalidVersion) {
            assertThatThrownBy(() -> new PluginDescriptor(
                VALID_ID, VALID_NAME, invalidVersion,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "1.0.0",
            "0.1.0-SNAPSHOT",
            "2.3.4-beta.1",
            "1.0.0+build.123"
        })
        @DisplayName("should accept semantic version formats")
        void shouldAcceptSemanticVersionFormats(String validVersion) {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID, VALID_NAME, validVersion,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            );

            assertThat(descriptor.version()).isEqualTo(validVersion);
        }
    }

    @Nested
    @DisplayName("when validating minAppVersion")
    class MinAppVersionValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should reject null or blank minAppVersion")
        void shouldRejectNullOrBlankMinAppVersion(String invalidMinVersion) {
            assertThatThrownBy(() -> new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, invalidMinVersion
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minAppVersion");
        }
    }

    @Nested
    @DisplayName("when validating optional fields")
    class OptionalFieldsValidation {

        @Test
        @DisplayName("should allow null description")
        void shouldAllowNullDescription() {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                null, VALID_AUTHOR, VALID_MIN_APP_VERSION
            );

            assertThat(descriptor.description()).isNull();
        }

        @Test
        @DisplayName("should allow empty description")
        void shouldAllowEmptyDescription() {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                "", VALID_AUTHOR, VALID_MIN_APP_VERSION
            );

            assertThat(descriptor.description()).isEmpty();
        }

        @Test
        @DisplayName("should allow null author")
        void shouldAllowNullAuthor() {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, null, VALID_MIN_APP_VERSION
            );

            assertThat(descriptor.author()).isNull();
        }
    }

    @Nested
    @DisplayName("when working with permissions")
    class PermissionsValidation {

        @Test
        @DisplayName("should create descriptor with permissions")
        void shouldCreateDescriptorWithPermissions() {
            Set<PluginPermission> permissions = Set.of(
                PluginPermission.DATA_READ,
                PluginPermission.UI_EXTENSION
            );

            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION,
                permissions
            );

            assertThat(descriptor.permissions())
                .containsExactlyInAnyOrder(
                    PluginPermission.DATA_READ,
                    PluginPermission.UI_EXTENSION
                );
        }

        @Test
        @DisplayName("should create descriptor with empty permissions by default")
        void shouldCreateDescriptorWithEmptyPermissions() {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION
            );

            assertThat(descriptor.permissions()).isEmpty();
        }

        @Test
        @DisplayName("should normalize null permissions to empty set")
        void shouldNormalizeNullPermissions() {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION,
                null
            );

            assertThat(descriptor.permissions()).isEmpty();
        }

        @Test
        @DisplayName("should check hasPermission correctly")
        void shouldCheckHasPermission() {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION,
                Set.of(PluginPermission.DATA_READ)
            );

            assertThat(descriptor.hasPermission(PluginPermission.DATA_READ)).isTrue();
            assertThat(descriptor.hasPermission(PluginPermission.DATA_WRITE)).isFalse();
        }

        @Test
        @DisplayName("should detect high sensitivity permissions")
        void shouldDetectHighSensitivityPermissions() {
            PluginDescriptor withHighSensitivity = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION,
                Set.of(PluginPermission.HMRC_API)
            );

            PluginDescriptor withLowSensitivity = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION,
                Set.of(PluginPermission.UI_EXTENSION)
            );

            assertThat(withHighSensitivity.hasHighSensitivityPermissions()).isTrue();
            assertThat(withLowSensitivity.hasHighSensitivityPermissions()).isFalse();
        }

        @Test
        @DisplayName("permissions should be immutable")
        void permissionsShouldBeImmutable() {
            PluginDescriptor descriptor = new PluginDescriptor(
                VALID_ID, VALID_NAME, VALID_VERSION,
                VALID_DESCRIPTION, VALID_AUTHOR, VALID_MIN_APP_VERSION,
                Set.of(PluginPermission.DATA_READ)
            );

            assertThatThrownBy(() ->
                descriptor.permissions().add(PluginPermission.DATA_WRITE)
            ).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
