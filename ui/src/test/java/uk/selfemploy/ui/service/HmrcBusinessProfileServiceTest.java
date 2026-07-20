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
    // Real HMRC Business Details API v2 "List All Businesses" shape: a listOfBusinesses array whose
    // items carry typeOfBusiness + businessId + tradingName.
    private static final String BUSINESS_JSON =
            "{\"listOfBusinesses\":[{\"typeOfBusiness\":\"self-employment\","
            + "\"businessId\":\"XAIS12345678901\",\"tradingName\":\"Test\"}]}";

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
        @DisplayName("200 with an empty business list reports no-business and does not persist the NINO")
        void okWithoutBusiness() {
            Result result = service.applyResponse(200,
                    "{\"listOfBusinesses\":[]}", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.NO_BUSINESS_FOUND);
            assertThat(store().loadNino()).isNull();
            assertThat(store().isNinoVerified()).isFalse();
        }

        @Test
        @DisplayName("200 with only a property business (no self-employment) reports no-business")
        void okWithPropertyOnly() {
            String propertyOnly =
                    "{\"listOfBusinesses\":[{\"typeOfBusiness\":\"uk-property\","
                    + "\"businessId\":\"XPIS12345678901\"}]}";

            Result result = service.applyResponse(200, propertyOnly, NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.NO_BUSINESS_FOUND);
            assertThat(store().loadNino()).isNull();
        }

        @Test
        @DisplayName("a self-employment businessId with a lower-case second character is accepted")
        void lowerCaseSecondCharBusinessId() {
            String json = "{\"listOfBusinesses\":[{\"typeOfBusiness\":\"self-employment\","
                    + "\"businessId\":\"XmIS12345678901\"}]}";

            Result result = service.applyResponse(200, json, NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.VERIFIED);
            assertThat(result.businessId()).isEqualTo("XmIS12345678901");
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
            assertThat(store().loadNino()).isEqualTo(NINO);
            assertThat(store().isNinoVerified()).isFalse();
        }

        @Test
        @DisplayName("a 200 with an unreadable body is treated as sync-pending and does not wipe a verified profile")
        void malformedOkDoesNotWipeVerifiedProfile() {
            store().saveHmrcBusinessId("XAIS12345678901");
            store().saveConnectedNino(NINO);

            Result result = service.applyResponse(200, "{ this is not valid json", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.PROFILE_SYNC_PENDING);
            assertThat(store().loadHmrcBusinessId()).isEqualTo("XAIS12345678901");
            assertThat(store().loadConnectedNino()).isEqualTo(NINO);
        }

        @Test
        @DisplayName("a 200 with an empty body or an object lacking listOfBusinesses is sync-pending, not a wipe")
        void unrecognisedObjectDoesNotWipe() {
            store().saveHmrcBusinessId("XAIS12345678901");
            store().saveConnectedNino(NINO);

            Result empty = service.applyResponse(200, "", NINO, false);
            assertThat(empty.outcome()).isEqualTo(Outcome.PROFILE_SYNC_PENDING);
            assertThat(store().loadHmrcBusinessId()).isEqualTo("XAIS12345678901");

            // An object without the listOfBusinesses marker (e.g. a gateway error envelope) is not a
            // genuine "no business" — it must not clear a previously-verified profile.
            Result envelope = service.applyResponse(200, "{\"code\":\"SERVER_ERROR\"}", NINO, false);
            assertThat(envelope.outcome()).isEqualTo(Outcome.PROFILE_SYNC_PENDING);
            assertThat(store().loadHmrcBusinessId()).isEqualTo("XAIS12345678901");

            Result emptyObject = service.applyResponse(200, "{}", NINO, false);
            assertThat(emptyObject.outcome()).isEqualTo(Outcome.PROFILE_SYNC_PENDING);
            assertThat(store().loadHmrcBusinessId()).isEqualTo("XAIS12345678901");
        }

        @Test
        @DisplayName("a body that is not a business-details object is sync-pending and does not wipe a verified profile")
        void nonObjectBodyIsSyncPending() {
            store().saveHmrcBusinessId("XAIS12345678901");
            store().saveConnectedNino(NINO);

            Result result = service.applyResponse(200, "[]", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.PROFILE_SYNC_PENDING);
            assertThat(store().loadHmrcBusinessId()).isEqualTo("XAIS12345678901");
            assertThat(store().loadConnectedNino()).isEqualTo(NINO);
        }

        @Test
        @DisplayName("an unexpected scalar body is sync-pending and does not wipe a verified profile")
        void scalarBodyIsSyncPending() {
            store().saveHmrcBusinessId("XAIS12345678901");
            store().saveConnectedNino(NINO);

            Result result = service.applyResponse(200, "\"OK\"", NINO, false);

            assertThat(result.outcome()).isEqualTo(Outcome.PROFILE_SYNC_PENDING);
            assertThat(store().loadHmrcBusinessId()).isEqualTo("XAIS12345678901");
            assertThat(store().loadConnectedNino()).isEqualTo(NINO);
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
    }
}
