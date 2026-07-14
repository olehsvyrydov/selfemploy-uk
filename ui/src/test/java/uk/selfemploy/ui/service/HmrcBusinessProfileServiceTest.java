package uk.selfemploy.ui.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.selfemploy.ui.service.HmrcBusinessProfileService.Outcome;
import uk.selfemploy.ui.service.HmrcBusinessProfileService.Result;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the outcome decision and persistence policy of {@link HmrcBusinessProfileService}.
 *
 * <p>The central invariants: the NINO is persisted as the verified, connected NINO only when HMRC
 * confirms it; a definitive rejection never overwrites a previously-stored NINO; and a connection
 * that could not fetch the profile still keeps the NINO so the user's input is not lost.
 */
@DisplayName("HmrcBusinessProfileService - outcome and persistence policy")
class HmrcBusinessProfileServiceTest {

    private static final String NINO = "AB123456C";
    private static final String OTHER_NINO = "CD654321B";
    private static final String BUSINESS_JSON =
            "{\"selfEmployments\":[{\"businessId\":\"XAIS12345678901\",\"tradingName\":\"Test\"}]}";

    private HmrcBusinessProfileService service;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        service = new HmrcBusinessProfileService();
    }

    private static SqliteDataStore store() {
        return SqliteDataStore.getInstance();
    }

    @Nested
    @DisplayName("Production responses")
    class ProductionResponses {

        @Test
        @DisplayName("200 with a business verifies and persists the NINO and business ID")
        void okWithBusinessVerifies() {
            Result result = service.applyResponse(200, BUSINESS_JSON, NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.VERIFIED);
            assertThat(result.businessId()).isEqualTo("XAIS12345678901");
            assertThat(store().loadHmrcBusinessId()).isEqualTo("XAIS12345678901");
            assertThat(store().loadNino()).isEqualTo(NINO);
            assertThat(store().loadConnectedNino()).isEqualTo(NINO);
            assertThat(store().isNinoVerified()).isTrue();
        }

        @Test
        @DisplayName("200 with no business reports no-business and does not persist the NINO")
        void okWithoutBusiness() {
            Result result = service.applyResponse(200, "{\"selfEmployments\":[]}", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.NO_BUSINESS_FOUND);
            assertThat(store().loadNino()).isNull();
            assertThat(store().isNinoVerified()).isFalse();
        }

        @Test
        @DisplayName("403 reports NINO mismatch and does not overwrite a previously-stored NINO")
        void forbiddenDoesNotOverwriteStoredNino() {
            store().saveNino(OTHER_NINO);

            Result result = service.applyResponse(403, "", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.NINO_MISMATCH);
            assertThat(store().loadNino()).isEqualTo(OTHER_NINO);
            assertThat(store().isNinoVerified()).isFalse();
        }

        @Test
        @DisplayName("404 in production reports NINO-not-found and does not persist the NINO")
        void notFoundInProduction() {
            Result result = service.applyResponse(404, "", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.NINO_NOT_FOUND);
            assertThat(store().loadNino()).isNull();
            assertThat(store().isNinoVerified()).isFalse();
        }

        @Test
        @DisplayName("a definitive rejection clears a previously-stored business ID")
        void rejectionClearsStaleBusinessId() {
            store().saveHmrcBusinessId("XAIS99999999999");
            store().saveConnectedNino(NINO);

            Result result = service.applyResponse(403, "", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.NINO_MISMATCH);
            assertThat(store().loadHmrcBusinessId()).isNull();
            assertThat(store().loadConnectedNino()).isNull();
        }

        @Test
        @DisplayName("a server error keeps a first-connection NINO but leaves it unverified")
        void serverErrorKeepsFirstConnectionNino() {
            Result result = service.applyResponse(503, "", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.PROFILE_SYNC_PENDING);
            assertThat(result.connected()).isTrue();
            assertThat(store().loadNino()).isEqualTo(NINO);
            assertThat(store().isNinoVerified()).isFalse();
        }

        @Test
        @DisplayName("a transient error does not overwrite a different NINO already on file")
        void transientErrorDoesNotOverwriteDifferentNino() {
            store().saveNino(OTHER_NINO);

            Result result = service.applyResponse(503, "", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.PROFILE_SYNC_PENDING);
            assertThat(store().loadNino()).isEqualTo(OTHER_NINO);
        }
    }

    @Nested
    @DisplayName("Sandbox responses")
    class SandboxResponses {

        @Test
        @DisplayName("404 on first sandbox connection uses the fallback business ID and persists the NINO")
        void firstSandboxConnection() {
            Result result = service.applyResponse(404, "", NINO, true);

            assertThat(result.outcome()).isEqualTo(Outcome.VERIFIED);
            assertThat(result.businessId()).isEqualTo(HmrcBusinessProfileService.sandboxFallbackBusinessId());
            assertThat(store().loadHmrcBusinessId())
                    .isEqualTo(HmrcBusinessProfileService.sandboxFallbackBusinessId());
            assertThat(store().loadNino()).isEqualTo(NINO);
            assertThat(store().loadConnectedNino()).isEqualTo(NINO);
        }

        @Test
        @DisplayName("404 with a changed NINO in sandbox reports NINO-changed and updates the connected NINO")
        void sandboxNinoChanged() {
            store().saveConnectedNino(OTHER_NINO);
            store().saveNino(OTHER_NINO);

            Result result = service.applyResponse(404, "", NINO, true);

            assertThat(result.outcome()).isEqualTo(Outcome.NINO_CHANGED_SANDBOX);
            assertThat(result.previousNino()).isEqualTo(OTHER_NINO);
            assertThat(store().loadNino()).isEqualTo(NINO);
            assertThat(store().loadConnectedNino()).isEqualTo(NINO);
            assertThat(store().isNinoVerified()).isFalse();
        }
    }

    @Nested
    @DisplayName("Helpers")
    class Helpers {

        @Test
        @DisplayName("detects the sandbox environment from the API base URL")
        void detectsSandbox() {
            assertThat(HmrcBusinessProfileService.isSandbox("https://test-api.service.hmrc.gov.uk")).isTrue();
            assertThat(HmrcBusinessProfileService.isSandbox("https://api.service.hmrc.gov.uk")).isFalse();
            assertThat(HmrcBusinessProfileService.isSandbox(null)).isFalse();
        }

        @Test
        @DisplayName("recognises 5xx as server errors")
        void recognisesServerErrors() {
            assertThat(HmrcBusinessProfileService.isServerError(500)).isTrue();
            assertThat(HmrcBusinessProfileService.isServerError(503)).isTrue();
            assertThat(HmrcBusinessProfileService.isServerError(404)).isFalse();
        }

        @Test
        @DisplayName("parses and validates the business ID from a response")
        void parsesBusinessId() {
            assertThat(HmrcBusinessProfileService.parseBusinessId(BUSINESS_JSON)).isEqualTo("XAIS12345678901");
            assertThat(HmrcBusinessProfileService.parseBusinessId("{\"businessId\":\"nope\"}")).isNull();
            assertThat(HmrcBusinessProfileService.parseBusinessId(null)).isNull();
        }
    }
}
