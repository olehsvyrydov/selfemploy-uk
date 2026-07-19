package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.UpdateCheckService.ReleaseFetcher;
import uk.selfemploy.ui.service.UpdateCheckService.UpdateCheckResult;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateCheckService")
class UpdateCheckServiceTest {

    @Nested
    @DisplayName("semantic version comparison")
    class VersionComparison {

        @Test
        @DisplayName("older is less than newer")
        void olderIsLessThanNewer() {
            assertThat(UpdateCheckService.compareVersions("1.0.0", "1.0.1")).isNegative();
            assertThat(UpdateCheckService.compareVersions("1.2.0", "1.10.0")).isNegative();
            assertThat(UpdateCheckService.compareVersions("1.9.9", "2.0.0")).isNegative();
        }

        @Test
        @DisplayName("newer is greater than older")
        void newerIsGreaterThanOlder() {
            assertThat(UpdateCheckService.compareVersions("2.0.0", "1.9.9")).isPositive();
            assertThat(UpdateCheckService.compareVersions("1.10.0", "1.2.0")).isPositive();
        }

        @Test
        @DisplayName("equal versions compare as zero")
        void equalVersions() {
            assertThat(UpdateCheckService.compareVersions("1.2.3", "1.2.3")).isZero();
        }

        @Test
        @DisplayName("a leading v is ignored")
        void leadingVIsIgnored() {
            assertThat(UpdateCheckService.compareVersions("v1.2.3", "1.2.3")).isZero();
            assertThat(UpdateCheckService.compareVersions("v1.2.3", "v1.2.4")).isNegative();
        }

        @Test
        @DisplayName("differing segment counts pad with zero")
        void differingSegmentCounts() {
            assertThat(UpdateCheckService.compareVersions("1.2", "1.2.0")).isZero();
            assertThat(UpdateCheckService.compareVersions("1.2", "1.2.1")).isNegative();
            assertThat(UpdateCheckService.compareVersions("1.2.0.0", "1.2")).isZero();
        }

        @Test
        @DisplayName("a pre-release ranks below its release")
        void preReleaseRanksBelowRelease() {
            assertThat(UpdateCheckService.compareVersions("1.0.0-beta.1", "1.0.0")).isNegative();
            assertThat(UpdateCheckService.compareVersions("1.0.0", "1.0.0-beta.1")).isPositive();
        }

        @Test
        @DisplayName("pre-release identifiers compare per SemVer")
        void preReleaseIdentifiers() {
            assertThat(UpdateCheckService.compareVersions("1.0.0-beta.1", "1.0.0-beta.2")).isNegative();
            assertThat(UpdateCheckService.compareVersions("1.0.0-alpha", "1.0.0-beta")).isNegative();
            assertThat(UpdateCheckService.compareVersions("1.0.0-beta", "1.0.0-beta.1")).isNegative();
            assertThat(UpdateCheckService.compareVersions("1.0.0-beta.1", "1.0.0-beta.1")).isZero();
        }

        @Test
        @DisplayName("build metadata is ignored")
        void buildMetadataIgnored() {
            assertThat(UpdateCheckService.compareVersions("1.2.3+build.9", "1.2.3")).isZero();
        }

        @Test
        @DisplayName("malformed versions do not signal an update")
        void malformedVersionsAreSafe() {
            assertThat(UpdateCheckService.isUpdateAvailable("1.0.0", "not-a-version")).isFalse();
            assertThat(UpdateCheckService.isUpdateAvailable("abc", "1.0.0")).isFalse();
            assertThat(UpdateCheckService.isUpdateAvailable("1.0.0", "")).isFalse();
            assertThat(UpdateCheckService.isUpdateAvailable(null, "1.0.0")).isFalse();
            assertThat(UpdateCheckService.isUpdateAvailable("1.0.0", null)).isFalse();
        }

        @Test
        @DisplayName("isUpdateAvailable is true only when the latest is strictly newer")
        void isUpdateAvailableSemantics() {
            assertThat(UpdateCheckService.isUpdateAvailable("1.0.0", "1.0.1")).isTrue();
            assertThat(UpdateCheckService.isUpdateAvailable("1.0.0", "v1.1.0")).isTrue();
            assertThat(UpdateCheckService.isUpdateAvailable("1.0.0", "1.0.0")).isFalse();
            assertThat(UpdateCheckService.isUpdateAvailable("1.0.1", "1.0.0")).isFalse();
            assertThat(UpdateCheckService.isUpdateAvailable("1.0.0-beta.1", "1.0.0")).isTrue();
        }
    }

    @Nested
    @DisplayName("check behaviour")
    class CheckBehaviour {

        private static final String RELEASE_JSON = "{\"tag_name\":\"v2.0.0\",\"name\":\"Release\"}";

        @Test
        @DisplayName("reports an update when the release tag is newer")
        void reportsUpdateWhenNewer() {
            UpdateCheckService service = new UpdateCheckService(
                    () -> Optional.of(RELEASE_JSON), () -> true, () -> "1.0.0");

            Optional<UpdateCheckResult> result = service.checkForUpdate();

            assertThat(result).isPresent();
            assertThat(result.get().currentVersion()).isEqualTo("1.0.0");
            assertThat(result.get().latestVersion()).isEqualTo("2.0.0");
            assertThat(result.get().updateAvailable()).isTrue();
        }

        @Test
        @DisplayName("reports no update when already current")
        void noUpdateWhenCurrent() {
            UpdateCheckService service = new UpdateCheckService(
                    () -> Optional.of(RELEASE_JSON), () -> true, () -> "2.0.0");

            Optional<UpdateCheckResult> result = service.checkForUpdate();

            assertThat(result).isPresent();
            assertThat(result.get().updateAvailable()).isFalse();
        }

        @Test
        @DisplayName("when disabled it never touches the network and returns empty")
        void disabledSkipsHttp() {
            AtomicInteger fetchCount = new AtomicInteger();
            ReleaseFetcher countingFetcher = () -> {
                fetchCount.incrementAndGet();
                return Optional.of(RELEASE_JSON);
            };
            UpdateCheckService service = new UpdateCheckService(
                    countingFetcher, () -> false, () -> "1.0.0");

            assertThat(service.checkForUpdate()).isEmpty();
            assertThat(fetchCount.get()).isZero();
        }

        @Test
        @DisplayName("the async entry point also skips the fetcher when disabled")
        void disabledSkipsHttpAsync() {
            AtomicInteger fetchCount = new AtomicInteger();
            ReleaseFetcher countingFetcher = () -> {
                fetchCount.incrementAndGet();
                return Optional.of(RELEASE_JSON);
            };
            UpdateCheckService service = new UpdateCheckService(
                    countingFetcher, () -> false, () -> "1.0.0");

            assertThat(service.checkForUpdateAsync().join()).isEmpty();
            assertThat(fetchCount.get()).isZero();
        }

        @Test
        @DisplayName("a network failure yields an empty result rather than throwing")
        void networkFailureIsSilent() {
            UpdateCheckService service = new UpdateCheckService(
                    Optional::empty, () -> true, () -> "1.0.0");

            assertThat(service.checkForUpdate()).isEmpty();
        }

        @Test
        @DisplayName("malformed release JSON yields an empty result")
        void malformedJsonIsSilent() {
            UpdateCheckService service = new UpdateCheckService(
                    () -> Optional.of("{ this is not json"), () -> true, () -> "1.0.0");

            assertThat(service.checkForUpdate()).isEmpty();
        }

        @Test
        @DisplayName("a missing tag_name yields an empty result")
        void missingTagIsSilent() {
            UpdateCheckService service = new UpdateCheckService(
                    () -> Optional.of("{\"name\":\"no tag here\"}"), () -> true, () -> "1.0.0");

            assertThat(service.checkForUpdate()).isEmpty();
        }
    }
}
