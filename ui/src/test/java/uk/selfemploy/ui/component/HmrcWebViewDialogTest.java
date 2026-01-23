package uk.selfemploy.ui.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.ui.help.HmrcLinkTopic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Tests for SE-7XX: HmrcWebViewDialog.
 *
 * <p>These tests verify the non-UI logic of the HmrcWebViewDialog component.
 * UI integration tests requiring JavaFX runtime are tagged with @Tag("e2e")
 * and run separately.</p>
 *
 * <p>Architecture Conditions (from jorge-architecture-inapp-browser.md):</p>
 * <ul>
 *   <li>URL whitelist validation against GOV.UK domains</li>
 *   <li>Non-modal dialog allowing main window interaction</li>
 *   <li>"Open in Browser" fallback always visible</li>
 *   <li>Loading indicator during page load</li>
 *   <li>Error state with user-friendly message</li>
 *   <li>Back/Forward navigation via WebView history</li>
 * </ul>
 */
@DisplayName("SE-7XX: HmrcWebViewDialog")
class HmrcWebViewDialogTest {

    @Nested
    @DisplayName("URL Validation Integration")
    class UrlValidationIntegration {

        @Test
        @DisplayName("should validate GOV.UK URLs before loading")
        void shouldValidateGovUkUrlsBeforeLoading() {
            String validUrl = "https://www.gov.uk/income-tax-rates";
            assertThat(HmrcWebViewDialog.isValidUrl(validUrl)).isTrue();
        }

        @Test
        @DisplayName("should reject non-GOV.UK URLs")
        void shouldRejectNonGovUkUrls() {
            String invalidUrl = "https://www.example.com";
            assertThat(HmrcWebViewDialog.isValidUrl(invalidUrl)).isFalse();
        }

        @Test
        @DisplayName("should reject null URL")
        void shouldRejectNullUrl() {
            assertThat(HmrcWebViewDialog.isValidUrl(null)).isFalse();
        }

        @Test
        @DisplayName("should reject blank URL")
        void shouldRejectBlankUrl() {
            assertThat(HmrcWebViewDialog.isValidUrl("")).isFalse();
            assertThat(HmrcWebViewDialog.isValidUrl("   ")).isFalse();
        }
    }

    @Nested
    @DisplayName("HmrcLinkTopic Conversion")
    class HmrcLinkTopicConversion {

        @Test
        @DisplayName("should get URL from HmrcLinkTopic")
        void shouldGetUrlFromHmrcLinkTopic() {
            String url = HmrcWebViewDialog.getUrlForTopic(HmrcLinkTopic.TAX_RATES);
            assertThat(url).isEqualTo(HmrcLinkTopic.TAX_RATES.getUrl());
        }

        @Test
        @DisplayName("should handle all HmrcLinkTopic values")
        void shouldHandleAllHmrcLinkTopicValues() {
            for (HmrcLinkTopic topic : HmrcLinkTopic.values()) {
                String url = HmrcWebViewDialog.getUrlForTopic(topic);
                assertThat(url)
                        .as("URL for topic %s", topic)
                        .isNotNull()
                        .isNotBlank()
                        .contains("gov.uk");
            }
        }

        @Test
        @DisplayName("should throw for null topic")
        void shouldThrowForNullTopic() {
            assertThatThrownBy(() -> HmrcWebViewDialog.getUrlForTopic(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("topic");
        }
    }

    @Nested
    @DisplayName("Title Generation")
    class TitleGeneration {

        @Test
        @DisplayName("should generate title from HmrcLinkTopic")
        void shouldGenerateTitleFromHmrcLinkTopic() {
            String title = HmrcWebViewDialog.getTitleForTopic(HmrcLinkTopic.TAX_RATES);
            assertThat(title).isEqualTo("Tax Rates");
        }

        @Test
        @DisplayName("should format title with proper capitalization")
        void shouldFormatTitleWithProperCapitalization() {
            assertThat(HmrcWebViewDialog.getTitleForTopic(HmrcLinkTopic.SA103_FORM))
                    .isEqualTo("SA103 Form");
            assertThat(HmrcWebViewDialog.getTitleForTopic(HmrcLinkTopic.NI_RATES))
                    .isEqualTo("NI Rates");
            assertThat(HmrcWebViewDialog.getTitleForTopic(HmrcLinkTopic.MTD_FOR_ITSA))
                    .isEqualTo("MTD For ITSA");
        }

        @Test
        @DisplayName("should throw for null topic in title generation")
        void shouldThrowForNullTopicInTitleGeneration() {
            assertThatThrownBy(() -> HmrcWebViewDialog.getTitleForTopic(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Dialog Configuration")
    class DialogConfiguration {

        @Test
        @DisplayName("should have default dimensions")
        void shouldHaveDefaultDimensions() {
            assertThat(HmrcWebViewDialog.DEFAULT_WIDTH).isEqualTo(1024);
            assertThat(HmrcWebViewDialog.DEFAULT_HEIGHT).isEqualTo(768);
        }

        @Test
        @DisplayName("should have minimum dimensions")
        void shouldHaveMinimumDimensions() {
            assertThat(HmrcWebViewDialog.MIN_WIDTH).isEqualTo(800);
            assertThat(HmrcWebViewDialog.MIN_HEIGHT).isEqualTo(600);
        }

        @Test
        @DisplayName("minimum dimensions should be less than default")
        void minimumDimensionsShouldBeLessThanDefault() {
            assertThat(HmrcWebViewDialog.MIN_WIDTH).isLessThan(HmrcWebViewDialog.DEFAULT_WIDTH);
            assertThat(HmrcWebViewDialog.MIN_HEIGHT).isLessThan(HmrcWebViewDialog.DEFAULT_HEIGHT);
        }
    }

    @Nested
    @DisplayName("Error Messages")
    class ErrorMessages {

        @Test
        @DisplayName("should provide offline error message")
        void shouldProvideOfflineErrorMessage() {
            String message = HmrcWebViewDialog.getOfflineErrorMessage();
            assertThat(message)
                    .contains("internet")
                    .contains("connection");
        }

        @Test
        @DisplayName("should provide load error message")
        void shouldProvideLoadErrorMessage() {
            String message = HmrcWebViewDialog.getLoadErrorMessage();
            assertThat(message)
                    .contains("load")
                    .contains("page");
        }

        @Test
        @DisplayName("should provide URL rejected message")
        void shouldProvideUrlRejectedMessage() {
            String message = HmrcWebViewDialog.getUrlRejectedMessage("https://evil.com");
            assertThat(message)
                    .contains("GOV.UK")
                    .contains("not allowed");
        }
    }

    @Nested
    @DisplayName("CSS Stylesheet")
    class CssStylesheet {

        @Test
        @DisplayName("should expose stylesheet location")
        void shouldExposeStylesheetLocation() {
            String stylesheet = HmrcWebViewDialog.getStylesheetLocation();
            assertThat(stylesheet)
                    .isNotNull()
                    .contains("webview")
                    .endsWith(".css");
        }
    }
}
