package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.selfemploy.hmrc.client.dto.ItsaStatusReason;
import uk.selfemploy.hmrc.client.dto.SaIndividualDetailsResponse;
import uk.selfemploy.hmrc.client.dto.SaIndividualDetailsResponse.ItsaStatus;
import uk.selfemploy.hmrc.client.dto.SaIndividualDetailsResponse.ItsaStatusForTaxYear;

import jakarta.ws.rs.Path;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract + DTO + enum tests for SaIndividualDetailsClient (HMRC SA Individual
 * Details API v2 — SLFEMPUK-31 / S17-07).
 *
 * <p>Locks the seven new ITSA sign-up reason codes added 2026-05-15, the
 * {@code Digitally Exempt} rename, and the {@code CLIENT_NOT_MTD_ENROLLED}
 * error-code constant exposed for callers.
 */
@DisplayName("SaIndividualDetailsClient (HMRC SA Individual Details API v2)")
class SaIndividualDetailsClientTest {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /individuals/details")
        void pathIsCorrect() {
            Path path = SaIndividualDetailsClient.class.getAnnotation(Path.class);
            assertThat(path).isNotNull();
            assertThat(path.value()).isEqualTo("/individuals/details");
        }

        @Test
        @DisplayName("Accept header declares HMRC v2 (SLFEMPUK-31)")
        void acceptHeaderIsV2() {
            ClientHeaderParam header = SaIndividualDetailsClient.class.getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.name()).isEqualTo("Accept");
            assertThat(header.value()).containsExactly("application/vnd.hmrc.2.0+json");
        }

        @Test
        @DisplayName("CLIENT_NOT_MTD_ENROLLED error-code constant is exposed to callers")
        void errorCodeConstantExposed() {
            assertThat(SaIndividualDetailsClient.ERROR_CLIENT_NOT_MTD_ENROLLED)
                .isEqualTo("CLIENT_NOT_MTD_ENROLLED");
            assertThat(SaIndividualDetailsClient.GOV_UK_MTD_SIGNUP_URL)
                .as("gov.uk MTD sign-up URL must be exposed so callers redirect users correctly")
                .startsWith("https://www.gov.uk/")
                .contains("making-tax-digital-for-income-tax");
        }
    }

    @Nested
    @DisplayName("v2 fixture deserialization")
    class V2Fixture {

        @Test
        @DisplayName("loads top-level personal details + per-tax-year status list")
        void personalAndStatusList() throws Exception {
            SaIndividualDetailsResponse response = loadFixture();

            assertThat(response.nino()).isEqualTo("AA000001A");
            assertThat(response.mtdbsa()).isEqualTo("XQIT00000000001");
            assertThat(response.firstName()).isEqualTo("Alex");
            assertThat(response.lastName()).isEqualTo("Sample");
            assertThat(response.itsaStatusByTaxYearOrEmpty()).hasSize(3);
        }

        @Test
        @DisplayName("recognises Digitally Exempt status (renamed from 'Non-Digital' on 2026-05-15)")
        void digitallyExemptStatusRecognised() throws Exception {
            ItsaStatusForTaxYear exemptYear = loadFixture().itsaStatusByTaxYearOrEmpty().stream()
                .filter(y -> "2024-25".equals(y.taxYear()))
                .findFirst()
                .orElseThrow();

            assertThat(exemptYear.status())
                .as("HMRC renamed 'Non-Digital' to 'Digitally Exempt' — the app must accept the new label")
                .isEqualTo(ItsaStatus.DIGITALLY_EXEMPT);
        }

        @Test
        @DisplayName("recognises new Capacitor reason (one of the 7 added on 2026-05-15)")
        void capacitorReasonRecognised() throws Exception {
            ItsaStatusForTaxYear y = loadFixture().itsaStatusByTaxYearOrEmpty().stream()
                .filter(s -> s.reason() == ItsaStatusReason.CAPACITOR)
                .findFirst()
                .orElseThrow();
            assertThat(y.taxYear()).isEqualTo("2025-26");
        }

        @Test
        @DisplayName("flags Blind Persons Allowance as Art.9 Special Category data")
        void blindPersonsAllowanceIsSpecialCategory() throws Exception {
            ItsaStatusForTaxYear y = loadFixture().itsaStatusByTaxYearOrEmpty().stream()
                .filter(s -> s.reason() == ItsaStatusReason.BLIND_PERSONS_ALLOWANCE)
                .findFirst()
                .orElseThrow();

            assertThat(y.reason().isSpecialCategoryData())
                .as("Blind Persons Allowance is Art.9 health data — lawful basis Art.9(2)(b) only")
                .isTrue();
        }

        @Test
        @DisplayName("unknown top-level fields tolerated (forward-compat with HMRC additions)")
        void unknownFieldsTolerated() throws Exception {
            SaIndividualDetailsResponse response = loadFixture();
            assertThat(response).isNotNull();
        }

        private SaIndividualDetailsResponse loadFixture() throws Exception {
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("hmrc-sandbox/sa-individual-details-v2-response.json")) {
                assertThat(is)
                    .as("fixture sa-individual-details-v2-response.json missing on classpath")
                    .isNotNull();
                return mapper.readValue(is, SaIndividualDetailsResponse.class);
            }
        }
    }

    @Nested
    @DisplayName("ItsaStatusReason enum")
    class ItsaStatusReasonEnumCoverage {

        @Test
        @DisplayName("encodes all 7 new reason codes from 2026-05-15")
        void allSevenNewReasonsModelled() {
            assertThat(ItsaStatusReason.values()).contains(
                ItsaStatusReason.CAPACITOR,
                ItsaStatusReason.RESIDENCE_AND_REMITTANCE,
                ItsaStatusReason.MINISTERS_OF_RELIGION,
                ItsaStatusReason.LLOYDS_UNDERWRITERS,
                ItsaStatusReason.BLIND_PERSONS_ALLOWANCE,
                ItsaStatusReason.MARRIED_COUPLES_ALLOWANCE,
                ItsaStatusReason.RETURN_NOT_CONSIDERED);
        }

        @ParameterizedTest
        @EnumSource(ItsaStatusReason.class)
        @DisplayName("every value round-trips via HMRC label")
        void everyReasonRoundTripsViaHmrcLabel(ItsaStatusReason reason) {
            assertThat(ItsaStatusReason.fromHmrcLabel(reason.hmrcLabel()))
                .as("ItsaStatusReason.fromHmrcLabel(%s) must return the same enum value", reason)
                .isEqualTo(reason);
        }

        @Test
        @DisplayName("unmodelled HMRC labels deserialize to UNKNOWN — no exception thrown")
        void unmodelledLabelFallsBackToUnknown() {
            assertThat(ItsaStatusReason.fromHmrcLabel("Some new reason added by HMRC in 2027"))
                .isEqualTo(ItsaStatusReason.UNKNOWN);
            assertThat(ItsaStatusReason.fromHmrcLabel(null)).isEqualTo(ItsaStatusReason.UNKNOWN);
        }

        @Test
        @DisplayName("only Blind Persons Allowance and Ministers of Religion are flagged as Special Category")
        void onlyTwoReasonsAreSpecialCategory() {
            for (ItsaStatusReason reason : ItsaStatusReason.values()) {
                boolean expectedSpecial = reason == ItsaStatusReason.BLIND_PERSONS_ALLOWANCE
                    || reason == ItsaStatusReason.MINISTERS_OF_RELIGION;
                assertThat(reason.isSpecialCategoryData())
                    .as("%s.isSpecialCategoryData()", reason)
                    .isEqualTo(expectedSpecial);
            }
        }
    }
}
