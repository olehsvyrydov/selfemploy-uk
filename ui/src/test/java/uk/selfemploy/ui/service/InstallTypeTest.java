package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InstallType detection")
class InstallTypeTest {

    @Test
    @DisplayName("the AppImage runtime marker wins over the code location")
    void appImageMarkerWins() {
        assertThat(InstallType.detect("/tmp/App.AppImage", "/usr/lib/app/app.jar", true, false))
                .isEqualTo(InstallType.APPIMAGE);
    }

    @Test
    @DisplayName("a system-path jar on a Debian host is a deb install")
    void systemJarOnDebian() {
        assertThat(InstallType.detect(null, "/usr/lib/selfemploy/app.jar", true, false))
                .isEqualTo(InstallType.DEB);
    }

    @Test
    @DisplayName("a system-path jar on a Red Hat host is an rpm install")
    void systemJarOnRedhat() {
        assertThat(InstallType.detect(null, "/opt/selfemploy/app.jar", false, true))
                .isEqualTo(InstallType.RPM);
    }

    @Test
    @DisplayName("a system-path jar with no package marker is unknown")
    void systemJarWithoutMarker() {
        assertThat(InstallType.detect(null, "/usr/lib/selfemploy/app.jar", false, false))
                .isEqualTo(InstallType.UNKNOWN);
    }

    @Test
    @DisplayName("a portable jar outside a system location is a plain jar run")
    void portableJar() {
        assertThat(InstallType.detect(null, "/home/user/apps/selfemploy/app.jar", true, false))
                .isEqualTo(InstallType.JAR);
    }

    @Test
    @DisplayName("classes on disk or a target directory is a development run")
    void developmentRun() {
        assertThat(InstallType.detect(null, "/home/user/git/selfemploy/ui/target/classes", false, false))
                .isEqualTo(InstallType.DEVELOPMENT);
        assertThat(InstallType.detect(null, "/home/user/git/selfemploy/ui/target/classes/", false, false))
                .isEqualTo(InstallType.DEVELOPMENT);
    }

    @Test
    @DisplayName("an unknown code location is unknown")
    void unknownLocation() {
        assertThat(InstallType.detect(null, null, false, false)).isEqualTo(InstallType.UNKNOWN);
        assertThat(InstallType.detect(null, "", false, false)).isEqualTo(InstallType.UNKNOWN);
    }

    @Test
    @DisplayName("every install type maps to a defined guidance key")
    void guidanceKeysDefined() {
        for (InstallType type : InstallType.values()) {
            assertThat(type.guidanceKey()).startsWith("update.guidance.");
        }
    }
}
