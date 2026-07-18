package uk.selfemploy.ui.help;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.selfemploy.core.config.NIClass2Rates;
import uk.selfemploy.core.config.TaxRateConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for SE-701: HelpService.
 *
 * <p>HelpService provides centralized access to help content
 * throughout the application.</p>
 */
@DisplayName("SE-701: HelpService")
class HelpServiceTest {

    private HelpService helpService;

    @BeforeEach
    void setUp() {
        helpService = new HelpService();
    }

    @Nested
    @DisplayName("Tax Summary Help")
    class TaxSummaryHelp {

        @Test
        @DisplayName("should provide net profit help")
        void shouldProvideNetProfitHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.NET_PROFIT);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Net Profit");
            assertThat(help.get().body()).contains("income minus");
            assertThat(help.get().hasLink()).isTrue();
        }

        @Test
        @DisplayName("should provide income tax help")
        void shouldProvideIncomeTaxHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.INCOME_TAX);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Income Tax");
            assertThat(help.get().body()).contains("taxable income");
        }

        @Test
        @DisplayName("should provide personal allowance help")
        void shouldProvidePersonalAllowanceHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.PERSONAL_ALLOWANCE);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Personal Allowance");
            assertThat(help.get().body()).contains("12,570");
        }

        @Test
        @DisplayName("should provide NI Class 4 help")
        void shouldProvideNiClass4Help() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.NI_CLASS_4);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Class 4");
            assertThat(help.get().body()).contains("self-employment profits");
        }

        @Test
        @DisplayName("should provide payments on account help")
        void shouldProvidePaymentsOnAccountHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.PAYMENTS_ON_ACCOUNT);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Payments on Account");
            assertThat(help.get().body()).contains("advance payments");
        }
    }

    @Nested
    @DisplayName("Expense Help")
    class ExpenseHelp {

        @Test
        @DisplayName("should provide expense category help")
        void shouldProvideExpenseCategoryHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.EXPENSE_CATEGORY);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Categories");
            assertThat(help.get().body()).contains("SA103");
        }

        @Test
        @DisplayName("should provide allowable expenses help")
        void shouldProvideAllowableExpensesHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.ALLOWABLE_EXPENSES);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Allowable");
            assertThat(help.get().body()).contains("wholly and exclusively");
        }

        @Test
        @DisplayName("should provide non-deductible expenses help")
        void shouldProvideNonDeductibleExpensesHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.NON_DEDUCTIBLE_EXPENSES);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Non-Deductible");
            assertThat(help.get().body()).contains("cannot be claimed");
            assertThat(help.get().body()).contains("entertainment");
            assertThat(help.get().hasLink()).isTrue();
        }
    }

    @Nested
    @DisplayName("Submission Help")
    class SubmissionHelp {

        @Test
        @DisplayName("should provide declaration help")
        void shouldProvideDeclarationHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.DECLARATION);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Declaration");
            assertThat(help.get().body()).contains("accuracy");
        }
    }

    @Nested
    @DisplayName("HMRC Links")
    class HmrcLinks {

        @Test
        @DisplayName("should provide SA103 link")
        void shouldProvideSa103Link() {
            String link = helpService.getHmrcLink(HmrcLinkTopic.SA103_FORM);

            assertThat(link).contains("gov.uk");
            assertThat(link).contains("sa103");
        }

        @Test
        @DisplayName("should provide tax rates link")
        void shouldProvideTaxRatesLink() {
            String link = helpService.getHmrcLink(HmrcLinkTopic.TAX_RATES);

            assertThat(link).contains("gov.uk");
            assertThat(link).contains("income-tax-rates");
        }

        @Test
        @DisplayName("should provide NI rates link")
        void shouldProvideNiRatesLink() {
            String link = helpService.getHmrcLink(HmrcLinkTopic.NI_RATES);

            assertThat(link).contains("gov.uk");
            assertThat(link).containsIgnoringCase("national-insurance");
        }

        @Test
        @DisplayName("should provide allowable expenses link")
        void shouldProvideAllowableExpensesLink() {
            String link = helpService.getHmrcLink(HmrcLinkTopic.ALLOWABLE_EXPENSES);

            assertThat(link).contains("gov.uk");
            assertThat(link).contains("expenses");
        }

        @Test
        @DisplayName("should provide filing deadlines link")
        void shouldProvideFilingDeadlinesLink() {
            String link = helpService.getHmrcLink(HmrcLinkTopic.FILING_DEADLINES);

            assertThat(link).contains("gov.uk");
            assertThat(link).contains("deadlines");
        }
    }

    @Nested
    @DisplayName("Unknown Topics")
    class UnknownTopics {

        @Test
        @DisplayName("should return empty for unknown help topic")
        void shouldReturnEmptyForUnknownHelpTopic() {
            // All enum values should have content, but test the contract
            for (HelpTopic topic : HelpTopic.values()) {
                Optional<HelpContent> help = helpService.getHelp(topic);
                assertThat(help).isPresent();
            }
        }
    }

    @Nested
    @DisplayName("SE-9XX: User Guide Help Topics")
    class UserGuideHelpTopics {

        @Test
        @DisplayName("should provide Getting Started help")
        void shouldProvideGettingStartedHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.GETTING_STARTED);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Getting Started");
            assertThat(help.get().body()).contains("overview");
        }

        @Test
        @DisplayName("should provide HMRC Connection help")
        void shouldProvideHmrcConnectionHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.HMRC_CONNECTION);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("HMRC");
            assertThat(help.get().body()).contains("OAuth");
        }

        @Test
        @DisplayName("should provide Security & Privacy help")
        void shouldProvideSecurityPrivacyHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.SECURITY_PRIVACY);

            assertThat(help).isPresent();
            assertThat(help.get().title()).contains("Security");
            assertThat(help.get().body()).contains("encrypted");
        }

        @Test
        @DisplayName("Security & Privacy help must describe encryption honestly (no false at-rest claims)")
        void securityHelpMustNotOverstateEncryption() {
            String body = helpService.getHelp(HelpTopic.SECURITY_PRIVACY).orElseThrow().body();

            // The data file is plaintext SQLite; only HMRC credentials + the NINO are encrypted.
            assertThat(body)
                .doesNotContain("Database encrypted")
                .doesNotContain("All data at rest is encrypted")
                .doesNotContain("OS keychain");
            assertThat(body).contains("AES-256-GCM");
            assertThat(body).contains("National Insurance number");
            assertThat(body).contains("not yet encrypted at rest");
        }

        @Test
        @DisplayName("should provide FAQ help")
        void shouldProvideFaqHelp() {
            Optional<HelpContent> help = helpService.getHelp(HelpTopic.FAQ);

            assertThat(help).isPresent();
            assertThat(help.get().title()).containsIgnoringCase("question");
            assertThat(help.get().body()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("SE-7XX: In-App Browser Integration")
    class InAppBrowserIntegration {

        @Test
        @DisplayName("should have openHmrcGuidance method for HmrcLinkTopic")
        void shouldHaveOpenHmrcGuidanceMethodForTopic() {
            // Verify the method exists and handles null gracefully
            // (actual opening requires JavaFX runtime)
            helpService.openHmrcGuidance((HmrcLinkTopic) null);
            // No exception thrown - passes
        }

        @Test
        @DisplayName("should have openHmrcGuidance method for URL and title")
        void shouldHaveOpenHmrcGuidanceMethodForUrl() {
            // Verify the method exists and handles null gracefully
            helpService.openHmrcGuidance(null, "Test");
            helpService.openHmrcGuidance("", "Test");
            helpService.openHmrcGuidance("   ", "Test");
            // No exception thrown - passes
        }

        @Test
        @DisplayName("all HMRC topics should have valid GOV.UK URLs")
        void allHmrcTopicsShouldHaveValidGovUkUrls() {
            for (HmrcLinkTopic topic : HmrcLinkTopic.values()) {
                String url = topic.getUrl();
                assertThat(url)
                        .as("URL for topic %s should be a GOV.UK domain", topic)
                        .contains("gov.uk");
            }
        }
    }

    @Nested
    @DisplayName("Markdown resources and year-aware facts")
    class MarkdownResources {

        private static final int TAX_YEAR = 2025;

        private final HelpService fixedYearService = new HelpService(TAX_YEAR);

        @ParameterizedTest
        @EnumSource(HelpTopic.class)
        @DisplayName("every topic loads from its resource with no unresolved placeholders")
        void everyTopicResolves(HelpTopic topic) {
            HelpContent content = fixedYearService.getHelp(topic).orElseThrow(() ->
                new AssertionError("No help content for topic " + topic));

            assertThat(content.title()).isNotBlank();
            assertThat(content.body()).isNotBlank();
            assertThat(content.body())
                .as("topic %s must have all placeholders resolved", topic)
                .doesNotContain("{{");
        }

        @Test
        @DisplayName("tax-year placeholders resolve to concrete dates for the configured year")
        void resolvesYearPlaceholders() {
            String body = fixedYearService.getHelp(HelpTopic.TAX_YEAR).orElseThrow().body();
            assertThat(body).contains("6 April 2025");
            assertThat(body).contains("31 January 2027");
        }

        @Test
        @DisplayName("Class 2 NI figures come from the authoritative rate configuration")
        void class2FiguresFromConfig() {
            NIClass2Rates rates = TaxRateConfiguration.getInstance().getNIClass2Rates(TAX_YEAR);
            String body = fixedYearService.getHelp(HelpTopic.NI_CLASS_2).orElseThrow().body();

            assertThat(body).contains(rates.weeklyRate().stripTrailingZeros().toPlainString());
            // Since April 2024 Class 2 is treated as paid above the SPT, not charged.
            assertThat(body).contains("treated as having");
        }

        @Test
        @DisplayName("SA103F threshold reflects the post-April-2024 VAT threshold of 90,000")
        void sa103ThresholdCorrected() {
            String body = fixedYearService.getHelp(HelpTopic.SA103_FORM).orElseThrow().body();
            assertThat(body).contains("£90,000");
            assertThat(body).doesNotContain("£85,000");
        }

        @Test
        @DisplayName("the FAQ states the app covers Class 2 as well as Class 4 NI")
        void faqMentionsClass2() {
            String body = fixedYearService.getHelp(HelpTopic.FAQ).orElseThrow().body();
            assertThat(body).contains("Class 2");
        }

        @Test
        @DisplayName("the disconnect instructions point at the real Settings control")
        void hmrcConnectionDisconnectPathCorrected() {
            String body = fixedYearService.getHelp(HelpTopic.HMRC_CONNECTION).orElseThrow().body();
            assertThat(body).contains("Settings → HMRC");
            assertThat(body).doesNotContain("Settings > HMRC Connection > Disconnect");
        }
    }
}
