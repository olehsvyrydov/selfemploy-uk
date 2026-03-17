package uk.selfemploy.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VersionInfo")
class VersionInfoTest {

    @Test
    @DisplayName("should load version from properties file")
    void shouldLoadVersion() {
        assertThat(VersionInfo.getVersion()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("should load app name from properties file")
    void shouldLoadAppName() {
        assertThat(VersionInfo.getAppName()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("should load build timestamp from properties file")
    void shouldLoadBuildTimestamp() {
        assertThat(VersionInfo.getBuildTimestamp()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("should return license identifier")
    void shouldReturnLicense() {
        assertThat(VersionInfo.getLicense()).isEqualTo("Apache License 2.0");
    }

    @Test
    @DisplayName("should return GitHub URL")
    void shouldReturnGitHubUrl() {
        assertThat(VersionInfo.getGitHubUrl()).contains("github.com");
    }
}
