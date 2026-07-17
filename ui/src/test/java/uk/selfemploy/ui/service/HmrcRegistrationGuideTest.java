package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.service.HmrcRegistrationGuide.CopyValue;
import uk.selfemploy.ui.service.HmrcRegistrationGuide.Step;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HmrcRegistrationGuide")
class HmrcRegistrationGuideTest {

    private static final String REDIRECT_URI = "http://localhost:8088/oauth/callback";

    private final HmrcRegistrationGuide guide = new HmrcRegistrationGuide(REDIRECT_URI);

    @Test
    @DisplayName("steps are numbered sequentially from 1 and cover account, app, redirect, subscribe, credentials")
    void stepsAreOrderedAndComplete() {
        List<Step> steps = guide.steps();

        assertThat(steps).isNotEmpty();
        for (int i = 0; i < steps.size(); i++) {
            assertThat(steps.get(i).number()).isEqualTo(i + 1);
            assertThat(steps.get(i).title()).isNotBlank();
            assertThat(steps.get(i).detail()).isNotBlank();
        }
        String allText = steps.stream().map(s -> s.title() + " " + s.detail())
            .reduce("", (a, b) -> a + " " + b).toLowerCase();
        assertThat(allText).contains("account");
        assertThat(allText).contains("redirect uri");
        assertThat(allText).contains("self assessment");
        assertThat(allText).contains("client id");
    }

    @Test
    @DisplayName("copy values expose the redirect URI and a suggested app name")
    void copyValuesExposeRedirectAndAppName() {
        List<CopyValue> values = guide.copyValues();

        assertThat(values).extracting(CopyValue::value).contains(REDIRECT_URI);
        assertThat(values).extracting(CopyValue::value).contains(HmrcRegistrationGuide.SUGGESTED_APP_NAME);
        assertThat(values).allSatisfy(v -> {
            assertThat(v.label()).isNotBlank();
            assertThat(v.value()).isNotBlank();
        });
    }

    @Test
    @DisplayName("the redirect URI passed in is the one exposed, not a hardcoded default")
    void redirectUriIsPassedThrough() {
        HmrcRegistrationGuide custom = new HmrcRegistrationGuide("http://localhost:9000/oauth/callback");
        assertThat(custom.redirectUri()).isEqualTo("http://localhost:9000/oauth/callback");
        assertThat(custom.copyValues()).extracting(CopyValue::value)
            .contains("http://localhost:9000/oauth/callback");
    }

    @Test
    @DisplayName("the Developer Hub URL is HTTPS on the official HMRC developer host")
    void developerHubUrlIsOfficialHttps() {
        assertThat(guide.developerHubUrl()).startsWith("https://developer.service.hmrc.gov.uk");
    }

    @Test
    @DisplayName("the production note explains that the sandbox steps need a separate production application")
    void productionNoteCoversProduction() {
        assertThat(guide.productionNote().toLowerCase())
            .contains("sandbox")
            .contains("production");
    }
}
