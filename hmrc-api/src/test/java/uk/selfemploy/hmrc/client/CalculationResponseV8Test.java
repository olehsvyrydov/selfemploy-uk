package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.hmrc.client.dto.CalculationResponse;
import uk.selfemploy.hmrc.client.dto.CalculationResponse.CapitalGainsTax;
import uk.selfemploy.hmrc.client.dto.CalculationResponse.StudentLoansAndPostgraduateLoan;

import java.io.InputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Contract + DTO tests for SelfAssessmentCalculationClient migrated to HMRC Individual
 * Calculations API v8.
 *
 * <p>v5, v6, v7 were retired in production on 2026-03-24. This test class is the
 * regression guard: if anyone downgrades the Accept header or removes the new v8
 * field mappings, the build fails immediately with a pointed assertion message.
 */
@DisplayName("CalculationResponse v8 (SelfAssessmentCalculationClient)")
class CalculationResponseV8Test {

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("SelfAssessmentCalculationClient declares Accept: application/vnd.hmrc.8.0+json")
        void clientAcceptsV8() {
            ClientHeaderParam header = SelfAssessmentCalculationClient.class.getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.name()).isEqualTo("Accept");
            assertThat(header.value())
                .as("v5, v6 and v7 retired 2026-03-24 — Calculations client MUST be v8")
                .containsExactly("application/vnd.hmrc.8.0+json");
        }

        @Test
        @DisplayName("SelfAssessmentDeclarationClient (final-declaration on Calc API) is also v8")
        void declarationClientAcceptsV8() {
            ClientHeaderParam header = SelfAssessmentDeclarationClient.class.getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.value()).containsExactly("application/vnd.hmrc.8.0+json");
        }
    }

    @Nested
    @DisplayName("v8 fixture deserialization")
    class V8Fixture {

        @Test
        @DisplayName("loads core fields from sandbox v8 calculation result")
        void coreFields() throws Exception {
            CalculationResponse response = loadV8Fixture();

            assertThat(response.calculationId()).isEqualTo("f2fb30e5-4ab6-4a29-b3c1-c7264259ff1e");
            assertThat(response.totalIncomeReceived()).isEqualByComparingTo("75000.00");
            assertThat(response.totalIncomeTaxAndNicsDue()).isEqualByComparingTo("18500.00");
            assertThat(response.nics().class4Nics().totalClass4Nics()).isEqualByComparingTo("3834.60");
        }

        @Test
        @DisplayName("surfaces transitionProfit (basis period reform — ITTOIA 2005 s.220+)")
        void transitionProfit() throws Exception {
            CalculationResponse response = loadV8Fixture();

            assertThat(response.transitionProfit())
                .as("transitionProfit is mandatory for traders still inside the 2023/24–2027/28 spread window")
                .isNotNull()
                .isEqualByComparingTo("8000.00");
            assertThat(response.transitionProfitAcceleratedAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("recognises Student Loan Plan Type 5 (new in v8)")
        void studentLoanPlanType5() throws Exception {
            StudentLoansAndPostgraduateLoan loan = loadV8Fixture().studentLoansAndPostgraduateLoan();

            assertThat(loan).isNotNull();
            assertThat(loan.planType()).isEqualTo("05");
            assertThat(loan.isPlanType5()).isTrue();
            assertThat(loan.studentLoanRepaymentAmount()).isEqualByComparingTo("1240.00");
        }

        @Test
        @DisplayName("exposes Capital Gains v8 additions: crypto, claim codes, RTT, unlisted, BADR multi-asset")
        void capitalGainsV8Additions() throws Exception {
            CapitalGainsTax cgt = loadV8Fixture().capitalGainsTax();

            assertThat(cgt).isNotNull();
            assertThat(cgt.realTimeTransactionTaxPaid()).isEqualByComparingTo("450.00");
            assertThat(cgt.claimOrElectionCodes()).containsExactly("BADR", "PRR");

            // Crypto disposals (CRYPTO22000+)
            assertThat(cgt.cryptoassetsDisposals()).isNotNull();
            assertThat(cgt.cryptoassetsDisposals().totalGains()).isEqualByComparingTo("6500.00");

            // Unlisted shares & securities
            assertThat(cgt.unlistedSharesAndSecurities()).isNotNull();
            assertThat(cgt.unlistedSharesAndSecurities().totalGains()).isEqualByComparingTo("3000.00");

            // BADR multi-asset (v8 supports >1 asset per BADR claim — TCGA 1992 s.169H–R)
            assertThat(cgt.businessAssetDisposalRelief()).isNotNull();
            assertThat(cgt.businessAssetDisposalRelief().assetCount())
                .as("v8 must support BADR claims covering >1 asset in a single disposal")
                .isEqualTo(2);
            assertThat(cgt.businessAssetDisposalRelief().badrTaxDue())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(cgt.businessAssetDisposalRelief().disposals().get(0).assetDescription())
                .startsWith("Trading goodwill");
        }

        @Test
        @DisplayName("tolerates unknown HMRC fields at every level (forward-compat)")
        void unknownFieldsTolerated() throws Exception {
            // The fixture contains "topLevelUnknownField" and "futureFieldFromHmrc"
            // at top level and inside capitalGainsTax respectively. Successful load
            // proves @JsonIgnoreProperties is wired correctly throughout.
            CalculationResponse response = loadV8Fixture();
            assertThat(response).isNotNull();
            assertThat(response.capitalGainsTax()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Backwards-compatibility with v7-shaped responses (only headline fields)")
    class V7ShapeStillReadable {

        @Test
        @DisplayName("a v7-style response with no v8 fields still deserializes; new fields are null")
        void v7StyleResponse() throws Exception {
            String minimalJson = """
                {
                  "id": "abc-123",
                  "calculationTimestamp": "2025-06-01T10:00:00",
                  "calculationReason": "customerRequest",
                  "totalIncomeTaxAndNicsDue": 5000.00,
                  "totalIncomeReceived": 30000.00,
                  "totalAllowancesAndDeductions": 12570.00,
                  "totalTaxableIncome": 17430.00,
                  "incomeTax": { "totalIncomeTax": 3486.00, "incomeTaxCharged": 3486.00 },
                  "nics": {
                    "class2Nics": { "amount": 179.40 },
                    "class4Nics": { "totalClass4ChargeableProfits": 15000.00, "totalClass4Nics": 1149.00 }
                  }
                }
                """;

            CalculationResponse response = mapper.readValue(minimalJson, CalculationResponse.class);

            assertThat(response.calculationId()).isEqualTo("abc-123");
            assertThat(response.totalIncomeReceived()).isEqualByComparingTo("30000.00");

            // v8 additions all null when not present in the response
            assertThat(response.transitionProfit()).isNull();
            assertThat(response.transitionProfitAcceleratedAmount()).isNull();
            assertThat(response.studentLoansAndPostgraduateLoan()).isNull();
            assertThat(response.capitalGainsTax()).isNull();
        }
    }

    private CalculationResponse loadV8Fixture() throws Exception {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("hmrc-sandbox/calculation-result-v8.json")) {
            assertThat(is)
                .as("fixture calculation-result-v8.json missing on classpath")
                .isNotNull();
            return mapper.readValue(is, CalculationResponse.class);
        }
    }
}
