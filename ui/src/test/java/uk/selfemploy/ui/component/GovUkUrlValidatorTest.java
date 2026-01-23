package uk.selfemploy.ui.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for SE-7XX: GovUkUrlValidator.
 *
 * <p>The validator ensures only GOV.UK domains are allowed in the in-app browser
 * to protect users from malicious redirects or phishing attempts.</p>
 *
 * <p>Architecture Requirement: URL whitelist validation against GOV.UK domains
 * (see jorge-architecture-inapp-browser.md)</p>
 */
@DisplayName("SE-7XX: GovUkUrlValidator")
class GovUkUrlValidatorTest {

    private GovUkUrlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GovUkUrlValidator();
    }

    @Nested
    @DisplayName("Valid GOV.UK URLs")
    class ValidGovUkUrls {

        @ParameterizedTest
        @DisplayName("should accept www.gov.uk URLs")
        @ValueSource(strings = {
                "https://www.gov.uk/income-tax-rates",
                "https://www.gov.uk/self-employed-national-insurance-rates",
                "https://www.gov.uk/expenses-if-youre-self-employed",
                "https://www.gov.uk/government/publications/self-assessment-self-employment-sa103",
                "https://www.gov.uk/self-assessment-tax-returns/deadlines"
        })
        void shouldAcceptWwwGovUkUrls(String url) {
            assertThat(validator.isAllowedUrl(url))
                    .as("URL should be allowed: %s", url)
                    .isTrue();
        }

        @ParameterizedTest
        @DisplayName("should accept gov.uk URLs without www")
        @ValueSource(strings = {
                "https://gov.uk/income-tax-rates",
                "https://gov.uk/self-assessment-tax-returns"
        })
        void shouldAcceptGovUkWithoutWww(String url) {
            assertThat(validator.isAllowedUrl(url))
                    .as("URL should be allowed: %s", url)
                    .isTrue();
        }

        @ParameterizedTest
        @DisplayName("should accept service.gov.uk subdomains")
        @ValueSource(strings = {
                "https://test-api.service.hmrc.gov.uk",
                "https://api.service.hmrc.gov.uk",
                "https://www.tax.service.gov.uk"
        })
        void shouldAcceptServiceGovUkSubdomains(String url) {
            assertThat(validator.isAllowedUrl(url))
                    .as("URL should be allowed: %s", url)
                    .isTrue();
        }

        @ParameterizedTest
        @DisplayName("should accept hmrc.gov.uk subdomains")
        @ValueSource(strings = {
                "https://www.hmrc.gov.uk",
                "https://hmrc.gov.uk",
                "https://developer.hmrc.gov.uk"
        })
        void shouldAcceptHmrcGovUkSubdomains(String url) {
            assertThat(validator.isAllowedUrl(url))
                    .as("URL should be allowed: %s", url)
                    .isTrue();
        }

        @Test
        @DisplayName("should accept HTTP URLs (will be upgraded)")
        void shouldAcceptHttpUrls() {
            assertThat(validator.isAllowedUrl("http://www.gov.uk/income-tax-rates"))
                    .isTrue();
        }

        @Test
        @DisplayName("should accept URLs with query parameters")
        void shouldAcceptUrlsWithQueryParameters() {
            assertThat(validator.isAllowedUrl("https://www.gov.uk/search?q=self-employment"))
                    .isTrue();
        }

        @Test
        @DisplayName("should accept URLs with fragments")
        void shouldAcceptUrlsWithFragments() {
            assertThat(validator.isAllowedUrl("https://www.gov.uk/income-tax-rates#section1"))
                    .isTrue();
        }

        @Test
        @DisplayName("should accept URLs with port numbers")
        void shouldAcceptUrlsWithPortNumbers() {
            assertThat(validator.isAllowedUrl("https://www.gov.uk:443/income-tax-rates"))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Invalid URLs")
    class InvalidUrls {

        @ParameterizedTest
        @DisplayName("should reject non-gov.uk domains")
        @ValueSource(strings = {
                "https://www.example.com",
                "https://www.google.com",
                "https://www.hmrc.com",
                "https://www.gov.com",
                "https://www.gov.org",
                "https://www.taxhelp.co.uk"
        })
        void shouldRejectNonGovUkDomains(String url) {
            assertThat(validator.isAllowedUrl(url))
                    .as("URL should be rejected: %s", url)
                    .isFalse();
        }

        @ParameterizedTest
        @DisplayName("should reject spoofed domains")
        @ValueSource(strings = {
                "https://www.gov.uk.evil.com",
                "https://gov.uk.phishing.net",
                "https://fake-gov.uk",
                "https://www-gov.uk",
                "https://gov-uk.com"
        })
        void shouldRejectSpoofedDomains(String url) {
            assertThat(validator.isAllowedUrl(url))
                    .as("Spoofed URL should be rejected: %s", url)
                    .isFalse();
        }

        @Test
        @DisplayName("should reject null URL")
        void shouldRejectNullUrl() {
            assertThat(validator.isAllowedUrl(null)).isFalse();
        }

        @Test
        @DisplayName("should reject empty URL")
        void shouldRejectEmptyUrl() {
            assertThat(validator.isAllowedUrl("")).isFalse();
        }

        @Test
        @DisplayName("should reject blank URL")
        void shouldRejectBlankUrl() {
            assertThat(validator.isAllowedUrl("   ")).isFalse();
        }

        @Test
        @DisplayName("should reject malformed URLs")
        void shouldRejectMalformedUrls() {
            assertThat(validator.isAllowedUrl("not-a-url")).isFalse();
            assertThat(validator.isAllowedUrl("ftp://www.gov.uk")).isFalse();
            assertThat(validator.isAllowedUrl("javascript:alert('xss')")).isFalse();
        }

        @Test
        @DisplayName("should reject file:// URLs")
        void shouldRejectFileUrls() {
            assertThat(validator.isAllowedUrl("file:///etc/passwd")).isFalse();
            assertThat(validator.isAllowedUrl("file://C:/Windows/System32")).isFalse();
        }

        @Test
        @DisplayName("should reject data: URLs")
        void shouldRejectDataUrls() {
            assertThat(validator.isAllowedUrl("data:text/html,<script>alert('xss')</script>")).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should be case-insensitive for domain matching")
        void shouldBeCaseInsensitiveForDomainMatching() {
            assertThat(validator.isAllowedUrl("https://WWW.GOV.UK/income-tax-rates")).isTrue();
            assertThat(validator.isAllowedUrl("https://www.Gov.Uk/income-tax-rates")).isTrue();
            assertThat(validator.isAllowedUrl("HTTPS://www.gov.uk/income-tax-rates")).isTrue();
        }

        @Test
        @DisplayName("should handle URLs with authentication info")
        void shouldHandleUrlsWithAuthenticationInfo() {
            // URLs with user:pass@ should be rejected as suspicious
            assertThat(validator.isAllowedUrl("https://user:pass@www.gov.uk/")).isFalse();
        }

        @Test
        @DisplayName("should handle URLs with trailing slashes")
        void shouldHandleUrlsWithTrailingSlashes() {
            assertThat(validator.isAllowedUrl("https://www.gov.uk/")).isTrue();
            assertThat(validator.isAllowedUrl("https://www.gov.uk")).isTrue();
        }

        @Test
        @DisplayName("should handle very long URLs")
        void shouldHandleVeryLongUrls() {
            String longPath = "a".repeat(1000);
            assertThat(validator.isAllowedUrl("https://www.gov.uk/" + longPath)).isTrue();
        }

        @Test
        @DisplayName("should handle unicode in path")
        void shouldHandleUnicodeInPath() {
            assertThat(validator.isAllowedUrl("https://www.gov.uk/path%20with%20spaces")).isTrue();
        }
    }

    @Nested
    @DisplayName("Allowed Domains List")
    class AllowedDomainsList {

        @Test
        @DisplayName("should expose the set of allowed domain patterns")
        void shouldExposeAllowedDomainPatterns() {
            assertThat(GovUkUrlValidator.getAllowedDomainPatterns())
                    .contains("gov.uk", "www.gov.uk");
        }

        @Test
        @DisplayName("allowed domains should be immutable")
        void allowedDomainsShouldBeImmutable() {
            var domains = GovUkUrlValidator.getAllowedDomainPatterns();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> domains.add("evil.com"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
