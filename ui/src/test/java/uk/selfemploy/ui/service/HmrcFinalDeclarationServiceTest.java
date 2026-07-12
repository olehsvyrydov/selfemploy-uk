package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.hmrc.oauth.HmrcOAuthService;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.ui.service.HmrcFinalDeclarationService.DeclarationConfirmation;
import uk.selfemploy.ui.service.HmrcFinalDeclarationService.DeclarationOutcome;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HmrcFinalDeclarationService")
class HmrcFinalDeclarationServiceTest {

    private static final String NINO = "AA123456A";
    private static final TaxYear TAX_YEAR = TaxYear.of(2025);
    private static final String CALC_ID = "calc-abc-123";
    private static final Instant CONFIRMED_AT = Instant.parse("2026-07-11T10:15:00Z");

    private HttpClient httpClient;
    private HmrcOAuthService oauthService;
    private HmrcFinalDeclarationService service;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        oauthService = mock(HmrcOAuthService.class);
        service = new HmrcFinalDeclarationService(httpClient, oauthService);
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

    private DeclarationConfirmation confirmed() {
        return new DeclarationConfirmation(true, CONFIRMED_AT);
    }

    @Test
    @DisplayName("builds the declaration URL under /individuals/declarations")
    void declarationUrl() {
        String url = HmrcFinalDeclarationService.buildDeclarationUrl(
            "https://test-api.service.hmrc.gov.uk", NINO, TAX_YEAR);
        assertThat(url).isEqualTo(
            "https://test-api.service.hmrc.gov.uk/individuals/declarations/self-assessment/AA123456A/2025-26");
    }

    @Test
    @DisplayName("refuses to send without an explicit user confirmation and never calls HMRC")
    void failsClosedWithoutConfirmation() throws Exception {
        DeclarationOutcome outcome = service.submitFinalDeclaration(
            NINO, TAX_YEAR, CALC_ID, new DeclarationConfirmation(false, CONFIRMED_AT));

        assertThat(outcome).isInstanceOf(DeclarationOutcome.Failure.class);
        assertThat(((DeclarationOutcome.Failure) outcome).reason())
            .isEqualTo(DeclarationOutcome.Reason.NOT_CONFIRMED);
        verify(httpClient, never()).<String>send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("a null confirmation also fails closed")
    void nullConfirmationFailsClosed() throws Exception {
        DeclarationOutcome outcome = service.submitFinalDeclaration(NINO, TAX_YEAR, CALC_ID, null);
        assertThat(((DeclarationOutcome.Failure) outcome).reason())
            .isEqualTo(DeclarationOutcome.Reason.NOT_CONFIRMED);
        verify(httpClient, never()).<String>send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("not connected surfaces as NOT_CONNECTED")
    void notConnected() {
        when(oauthService.isConnected()).thenReturn(false);
        DeclarationOutcome outcome = service.submitFinalDeclaration(NINO, TAX_YEAR, CALC_ID, confirmed());
        assertThat(((DeclarationOutcome.Failure) outcome).reason())
            .isEqualTo(DeclarationOutcome.Reason.NOT_CONNECTED);
    }

    @Test
    @DisplayName("a 204 success uses the calculation id as the HMRC reference")
    void success204UsesCalculationId() throws Exception {
        connected();
        HttpResponse<String> ok = response(204, "");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(ok);

        DeclarationOutcome outcome = service.submitFinalDeclaration(NINO, TAX_YEAR, CALC_ID, confirmed());

        assertThat(outcome).isInstanceOf(DeclarationOutcome.Success.class);
        DeclarationOutcome.Success success = (DeclarationOutcome.Success) outcome;
        assertThat(success.hmrcReference()).isEqualTo(CALC_ID);
        assertThat(success.declaredAt()).isEqualTo(CONFIRMED_AT);
    }

    @Test
    @DisplayName("prefers a charge reference from the response body when present")
    void prefersChargeReference() throws Exception {
        connected();
        HttpResponse<String> ok = response(200,
            "{\"chargeReference\":\"XM002610011594\",\"declarationTimestamp\":\"2026-07-11T10:16:00\"}");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(ok);

        DeclarationOutcome outcome = service.submitFinalDeclaration(NINO, TAX_YEAR, CALC_ID, confirmed());

        assertThat(((DeclarationOutcome.Success) outcome).hmrcReference()).isEqualTo("XM002610011594");
    }

    @Test
    @DisplayName("sends the calculation id in the request body with the v8 Accept header")
    void sendsCalculationIdAndVersion() throws Exception {
        connected();
        HttpResponse<String> ok = response(204, "");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(ok);

        service.submitFinalDeclaration(NINO, TAX_YEAR, CALC_ID, confirmed());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).<String>send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.uri().toString()).endsWith("/individuals/declarations/self-assessment/AA123456A/2025-26");
        assertThat(request.headers().firstValue("Accept")).contains("application/vnd.hmrc.8.0+json");
    }

    @Test
    @DisplayName("a 403 surfaces as FORBIDDEN with the HMRC message")
    void forbidden() throws Exception {
        connected();
        HttpResponse<String> denied = response(403,
            "{\"code\":\"CLIENT_OR_AGENT_NOT_AUTHORISED\",\"message\":\"Not authorised\"}");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(denied);

        DeclarationOutcome outcome = service.submitFinalDeclaration(NINO, TAX_YEAR, CALC_ID, confirmed());

        DeclarationOutcome.Failure failure = (DeclarationOutcome.Failure) outcome;
        assertThat(failure.reason()).isEqualTo(DeclarationOutcome.Reason.FORBIDDEN);
        assertThat(failure.message()).isEqualTo("Not authorised");
    }

    @Test
    @DisplayName("a 409 surfaces as ALREADY_DECLARED")
    void alreadyDeclared() throws Exception {
        connected();
        HttpResponse<String> conflict = response(409,
            "{\"code\":\"RULE_FINAL_DECLARATION_RECEIVED\",\"message\":\"Already declared\"}");
        when(httpClient.<String>send(any(HttpRequest.class), any())).thenReturn(conflict);

        DeclarationOutcome outcome = service.submitFinalDeclaration(NINO, TAX_YEAR, CALC_ID, confirmed());

        assertThat(((DeclarationOutcome.Failure) outcome).reason())
            .isEqualTo(DeclarationOutcome.Reason.ALREADY_DECLARED);
    }

    @Test
    @DisplayName("a network error surfaces as NETWORK")
    void networkError() throws Exception {
        connected();
        when(httpClient.<String>send(any(HttpRequest.class), any()))
            .thenThrow(new IOException("connection refused"));

        DeclarationOutcome outcome = service.submitFinalDeclaration(NINO, TAX_YEAR, CALC_ID, confirmed());

        assertThat(((DeclarationOutcome.Failure) outcome).reason())
            .isEqualTo(DeclarationOutcome.Reason.NETWORK);
    }
}
