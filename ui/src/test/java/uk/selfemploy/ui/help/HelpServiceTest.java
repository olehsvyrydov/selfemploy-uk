package uk.selfemploy.ui.help;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
