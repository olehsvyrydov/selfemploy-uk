package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.ui.service.HmrcCalculationService.CalculationOutcome;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("HmrcCalculationService")
class HmrcCalculationServiceTest {

    private static final String NINO = "AA123456A";
    private static final TaxYear TAX_YEAR = TaxYear.of(2025);

    private HttpClient httpClient;
    private HmrcOAuthService oauthService;
    private HmrcCalculationService service;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        oauthService = mock(HmrcOAuthService.class);
        service = new HmrcCalculationService(httpClient, oauthService);
        service.configurePolling(3, 0); // no real sleeping in tests
    }

    private void connected() {
        when(oauthService.isConnected()).thenReturn(true);
        when(oauthService.getCurrentTokens())
            .thenReturn(OAuthTokens.create("token-abc", "refresh-abc", 3600, "bearer", "read write"));
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(status);
        when(resp.body()).thenReturn(body);
        return resp;
    }

    private static final String CALCULATION_BODY = """
        {
          "id": "calc-abc-123",
          "totalIncomeTaxAndNicsDue": 5432.10,
          "totalTaxableIncome": 30000.00,
          "incomeTax": { "totalIncomeTax": 3500.00, "incomeTaxCharged": 3500.00 },
          "nics": {
            "class2Nics": { "amount": 179.40 },
            "class4Nics": { "totalClass4ChargeableProfits": 30000.00, "totalClass4Nics": 1752.70 }
          }
        }
        """;

    @Nested
    @DisplayName("URL and version")
    class UrlAndVersion {

        @Test
        @DisplayName("targets the Individual Calculations v8 Accept header")
        void acceptHeaderIsV8() {
            assertThat(HmrcCalculationService.ACCEPT_HEADER).isEqualTo("application/vnd.hmrc.8.0+json");
        }

        @Test
        @DisplayName("builds the trigger URL with the NINO and HMRC tax-year format")
        void triggerUrl() {
            String url = HmrcCalculationService.buildTriggerUrl(
                "https://test-api.service.hmrc.gov.uk", NINO, TAX_YEAR);
            assertThat(url).isEqualTo(
                "https://test-api.service.hmrc.gov.uk/individuals/calculations/self-assessment/AA123456A/2025-26");
        }

        @Test
        @DisplayName("builds the retrieve URL with the calculation id appended")
        void retrieveUrl() {
            String url = HmrcCalculationService.buildRetrieveUrl(
                "https://test-api.service.hmrc.gov.uk", NINO, TAX_YEAR, "calc-abc-123");
            assertThat(url).endsWith("/AA123456A/2025-26/calc-abc-123");
        }
    }

    @Nested
    @DisplayName("validation and connection")
    class ValidationAndConnection {

        @Test
        @DisplayName("a blank NINO fails validation without calling HMRC")
        void blankNino() {
            CalculationOutcome outcome = service.calculate("  ", TAX_YEAR, false);
            assertThat(outcome).isInstanceOf(CalculationOutcome.Failure.class);
            assertThat(((CalculationOutcome.Failure) outcome).reason())
                .isEqualTo(CalculationOutcome.Reason.VALIDATION);
        }

        @Test
        @DisplayName("not connected to HMRC surfaces as NOT_CONNECTED")
        void notConnected() {
            when(oauthService.isConnected()).thenReturn(false);
            CalculationOutcome outcome = service.calculate(NINO, TAX_YEAR, false);
            assertThat(outcome).isInstanceOf(CalculationOutcome.Failure.class);
            assertThat(((CalculationOutcome.Failure) outcome).reason())
                .isEqualTo(CalculationOutcome.Reason.NOT_CONNECTED);
        }
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("triggers then retrieves and returns the liability breakdown")
        void triggerThenRetrieve() throws Exception {
            connected();
            HttpResponse<String> trigger = response(202, "{\"id\":\"calc-abc-123\"}");
            HttpResponse<String> retrieve = response(200, CALCULATION_BODY);
            when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenReturn(trigger).thenReturn(retrieve);

            CalculationOutcome outcome = service.calculate(NINO, TAX_YEAR, true);

            assertThat(outcome).isInstanceOf(CalculationOutcome.Success.class);
            var calc = ((CalculationOutcome.Success) outcome).calculation();
            assertThat(calc.calculationId()).isEqualTo("calc-abc-123");
            assertThat(calc.totalIncomeTaxAndNicsDue()).isEqualByComparingTo("5432.10");
            assertThat(calc.incomeTax().totalIncomeTax()).isEqualByComparingTo("3500.00");
            assertThat(calc.nics().class2Nics().amount()).isEqualByComparingTo("179.40");
            assertThat(calc.nics().class4Nics().totalClass4Nics()).isEqualByComparingTo("1752.70");
        }

        @Test
        @DisplayName("sends the v8 Accept header and bearer token on the trigger request")
        void sendsCorrectHeaders() throws Exception {
            connected();
            HttpResponse<String> trigger = response(202, "{\"id\":\"calc-abc-123\"}");
            HttpResponse<String> retrieve = response(200, CALCULATION_BODY);
            when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenReturn(trigger).thenReturn(retrieve);

            service.calculate(NINO, TAX_YEAR, true);

            ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
            verifySend(captor);
            HttpRequest triggerRequest = captor.getAllValues().get(0);
            assertThat(triggerRequest.method()).isEqualTo("POST");
            assertThat(triggerRequest.uri().toString()).endsWith("/AA123456A/2025-26");
            assertThat(triggerRequest.headers().firstValue("Accept")).contains("application/vnd.hmrc.8.0+json");
            assertThat(triggerRequest.headers().firstValue("Authorization")).contains("Bearer token-abc");
            assertThat(triggerRequest.headers().firstValue("Gov-Client-Connection-Method"))
                .contains("DESKTOP_APP_DIRECT");
        }

        private void verifySend(ArgumentCaptor<HttpRequest> captor) throws IOException, InterruptedException {
            org.mockito.Mockito.verify(httpClient, org.mockito.Mockito.atLeastOnce())
                .<String>send(captor.capture(), any());
        }
    }

    @Nested
    @DisplayName("error mapping")
    class ErrorMapping {

        @Test
        @DisplayName("a 403 on trigger surfaces as FORBIDDEN")
        void forbidden() throws Exception {
            connected();
            HttpResponse<String> forbidden = response(403,
                "{\"code\":\"CLIENT_OR_AGENT_NOT_AUTHORISED\",\"message\":\"Not authorised\"}");
            when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(forbidden);

            CalculationOutcome outcome = service.calculate(NINO, TAX_YEAR, false);

            assertThat(outcome).isInstanceOf(CalculationOutcome.Failure.class);
            CalculationOutcome.Failure failure = (CalculationOutcome.Failure) outcome;
            assertThat(failure.reason()).isEqualTo(CalculationOutcome.Reason.FORBIDDEN);
            assertThat(failure.httpStatus()).isEqualTo(403);
            assertThat(failure.message()).isEqualTo("Not authorised");
        }

        @Test
        @DisplayName("a persistent 404 on retrieve surfaces as NOT_READY after polling")
        void notReadyAfterPolling() throws Exception {
            connected();
            HttpResponse<String> trigger = response(202, "{\"id\":\"calc-abc-123\"}");
            HttpResponse<String> notFound = response(404, "{\"code\":\"MATCHING_RESOURCE_NOT_FOUND\"}");
            when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenReturn(trigger).thenReturn(notFound);

            CalculationOutcome outcome = service.calculate(NINO, TAX_YEAR, false);

            assertThat(outcome).isInstanceOf(CalculationOutcome.Failure.class);
            assertThat(((CalculationOutcome.Failure) outcome).reason())
                .isEqualTo(CalculationOutcome.Reason.NOT_READY);
        }

        @Test
        @DisplayName("a network error surfaces as NETWORK")
        void networkError() throws Exception {
            connected();
            when(httpClient.<String>send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("connection refused"));

            CalculationOutcome outcome = service.calculate(NINO, TAX_YEAR, false);

            assertThat(outcome).isInstanceOf(CalculationOutcome.Failure.class);
            assertThat(((CalculationOutcome.Failure) outcome).reason())
                .isEqualTo(CalculationOutcome.Reason.NETWORK);
        }

        @Test
        @DisplayName("a 202 with no calculation id surfaces as UNEXPECTED")
        void triggerWithoutId() throws Exception {
            connected();
            HttpResponse<String> trigger = response(202, "{}");
            when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(trigger);

            CalculationOutcome outcome = service.calculate(NINO, TAX_YEAR, false);

            assertThat(outcome).isInstanceOf(CalculationOutcome.Failure.class);
            assertThat(((CalculationOutcome.Failure) outcome).reason())
                .isEqualTo(CalculationOutcome.Reason.UNEXPECTED);
        }
    }

    @Test
    @DisplayName("recovers when the first retrieve is 404 but a later poll succeeds")
    void pollingRecovers() throws Exception {
        connected();
        HttpResponse<String> trigger = response(202, "{\"id\":\"calc-abc-123\"}");
        HttpResponse<String> notFound = response(404, "{\"code\":\"MATCHING_RESOURCE_NOT_FOUND\"}");
        HttpResponse<String> ready = response(200, CALCULATION_BODY);
        when(httpClient.<String>send(any(HttpRequest.class), any()))
            .thenReturn(trigger).thenReturn(notFound).thenReturn(ready);

        CalculationOutcome outcome = service.calculate(NINO, TAX_YEAR, false);

        assertThat(outcome).isInstanceOf(CalculationOutcome.Success.class);
    }
}
