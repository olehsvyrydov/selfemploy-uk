package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.help.HelpService;
import uk.selfemploy.ui.help.HelpTopic;
import uk.selfemploy.ui.help.HmrcLinkTopic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HelpController.
 * Tests the controller logic for the Help page.
 */
@DisplayName("HelpController")
class HelpControllerTest {

    private HelpController controller;

    @BeforeEach
    void setUp() {
        controller = new HelpController();
    }

    @Test
    @DisplayName("should implement TaxYearAware interface")
    void shouldImplementTaxYearAware() {
        assertThat(controller).isInstanceOf(MainController.TaxYearAware.class);
    }

    @Test
    @DisplayName("should implement Initializable interface")
    void shouldImplementInitializable() {
        assertThat(controller).isInstanceOf(javafx.fxml.Initializable.class);
    }

    @Nested
    @DisplayName("Tax Year Management")
    class TaxYearManagement {

        @Test
        @DisplayName("should store tax year when set")
        void shouldStoreTaxYear() {
            // Given
            TaxYear taxYear = TaxYear.of(2025);

            // When
            controller.setTaxYear(taxYear);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(taxYear);
        }

        @Test
        @DisplayName("should handle null tax year gracefully")
        void shouldHandleNullTaxYear() {
            // When
            controller.setTaxYear(null);

            // Then
            assertThat(controller.getTaxYear()).isNull();
        }
    }

    @Nested
    @DisplayName("HelpService Integration")
    class HelpServiceIntegration {

        @Test
        @DisplayName("should have HelpService available")
        void shouldHaveHelpServiceAvailable() {
            assertThat(controller.getHelpService()).isNotNull();
        }

        @Test
        @DisplayName("should retrieve help content for topic")
        void shouldRetrieveHelpContent() {
            // When
            var content = controller.getHelpForTopic(HelpTopic.NET_PROFIT);

            // Then
            assertThat(content).isPresent();
            assertThat(content.get().title()).contains("Net Profit");
        }

        @Test
        @DisplayName("should retrieve HMRC link")
        void shouldRetrieveHmrcLink() {
            // When
            String link = controller.getHmrcLink(HmrcLinkTopic.TAX_RATES);

            // Then
            assertThat(link).isNotNull();
            assertThat(link).contains("gov.uk");
        }
    }

    @Nested
    @DisplayName("Help Categories")
    class HelpCategories {

        @Test
        @DisplayName("should have tax topics category")
        void shouldHaveTaxCategory() {
            assertThat(controller.getHelpCategories()).contains("Tax & Calculation");
        }

        @Test
        @DisplayName("should have expenses category")
        void shouldHaveExpensesCategory() {
            assertThat(controller.getHelpCategories()).contains("Expenses");
        }

        @Test
        @DisplayName("should have submission category")
        void shouldHaveSubmissionCategory() {
            assertThat(controller.getHelpCategories()).contains("HMRC Submission");
        }

        @Test
        @DisplayName("should have general category")
        void shouldHaveGeneralCategory() {
            assertThat(controller.getHelpCategories()).contains("General");
        }
    }

    @Nested
    @DisplayName("Topic Listing")
    class TopicListing {

        @Test
        @DisplayName("should return topics for tax category")
        void shouldReturnTaxTopics() {
            // When
            List<HelpTopic> topics = controller.getTopicsForCategory("Tax & Calculation");

            // Then
            assertThat(topics).isNotEmpty();
            assertThat(topics).contains(HelpTopic.INCOME_TAX, HelpTopic.NI_CLASS_4);
        }

        @Test
        @DisplayName("should return topics for expenses category")
        void shouldReturnExpensesTopics() {
            // When
            List<HelpTopic> topics = controller.getTopicsForCategory("Expenses");

            // Then
            assertThat(topics).isNotEmpty();
            assertThat(topics).contains(HelpTopic.EXPENSE_CATEGORY, HelpTopic.ALLOWABLE_EXPENSES);
        }

        @Test
        @DisplayName("should return empty list for unknown category")
        void shouldReturnEmptyForUnknownCategory() {
            // When
            List<HelpTopic> topics = controller.getTopicsForCategory("Unknown");

            // Then
            assertThat(topics).isEmpty();
        }
    }

    @Nested
    @DisplayName("Quick Links")
    class QuickLinks {

        @Test
        @DisplayName("should provide HMRC quick links")
        void shouldProvideHmrcQuickLinks() {
            // When
            List<HmrcLinkTopic> links = controller.getQuickLinks();

            // Then
            assertThat(links).isNotEmpty();
            assertThat(links).contains(
                HmrcLinkTopic.TAX_RATES,
                HmrcLinkTopic.FILING_DEADLINES,
                HmrcLinkTopic.ALLOWABLE_EXPENSES
            );
        }
    }

    @Nested
    @DisplayName("Topic Display Names")
    class TopicDisplayNames {

        @Test
        @DisplayName("should return display name for NET_PROFIT")
        void shouldReturnDisplayNameForNetProfit() {
            assertThat(controller.getTopicDisplayName(HelpTopic.NET_PROFIT))
                .isEqualTo("Net Profit");
        }

        @Test
        @DisplayName("should return display name for INCOME_TAX")
        void shouldReturnDisplayNameForIncomeTax() {
            assertThat(controller.getTopicDisplayName(HelpTopic.INCOME_TAX))
                .isEqualTo("Income Tax");
        }

        @Test
        @DisplayName("should return display name for NI_CLASS_4")
        void shouldReturnDisplayNameForNiClass4() {
            assertThat(controller.getTopicDisplayName(HelpTopic.NI_CLASS_4))
                .isEqualTo("NI Class 4");
        }

        @Test
        @DisplayName("should return display name for SA103_FORM")
        void shouldReturnDisplayNameForSa103Form() {
            assertThat(controller.getTopicDisplayName(HelpTopic.SA103_FORM))
                .isEqualTo("SA103 Form");
        }
    }

    @Nested
    @DisplayName("Link Display Names")
    class LinkDisplayNames {

        @Test
        @DisplayName("should return display name for TAX_RATES")
        void shouldReturnDisplayNameForTaxRates() {
            assertThat(controller.getLinkDisplayName(HmrcLinkTopic.TAX_RATES))
                .isEqualTo("Income Tax Rates");
        }

        @Test
        @DisplayName("should return display name for FILING_DEADLINES")
        void shouldReturnDisplayNameForFilingDeadlines() {
            assertThat(controller.getLinkDisplayName(HmrcLinkTopic.FILING_DEADLINES))
                .isEqualTo("Filing Deadlines");
        }
    }

    @Nested
    @DisplayName("GitHub URLs")
    class GitHubUrls {

        @Test
        @DisplayName("should return correct GitHub repository URL")
        void shouldReturnCorrectGitHubRepoUrl() {
            // The URL should point to the selfemploy-uk/self-employment repository
            assertThat(HelpController.GITHUB_REPO_URL)
                .isEqualTo("https://github.com/selfemploy-uk/self-employment");
        }

        @Test
        @DisplayName("should return correct GitHub Issues URL")
        void shouldReturnCorrectGitHubIssuesUrl() {
            // The URL should point to the issues page of the correct repository
            assertThat(HelpController.GITHUB_ISSUES_URL)
                .isEqualTo("https://github.com/selfemploy-uk/self-employment/issues");
        }

        @Test
        @DisplayName("GitHub URLs should not point to anthropic repository")
        void shouldNotPointToAnthropicRepository() {
            assertThat(HelpController.GITHUB_REPO_URL)
                .doesNotContain("anthropic");
            assertThat(HelpController.GITHUB_ISSUES_URL)
                .doesNotContain("anthropic");
        }
    }

    @Nested
    @DisplayName("Help Topic Click Handlers")
    class HelpTopicClickHandlers {

        @Test
        @DisplayName("should have handler for each help topic in FXML")
        void shouldHaveHandlerForEachTopic() {
            // Verify we have a mapping for display names to HelpTopic
            // This ensures the click handlers can find the right topic
            for (HelpTopic topic : HelpTopic.values()) {
                String displayName = controller.getTopicDisplayName(topic);
                assertThat(displayName).isNotNull();
                assertThat(displayName).isNotBlank();
            }
        }

        @Test
        @DisplayName("should find topic by display name for Net Profit")
        void shouldFindTopicByDisplayNameNetProfit() {
            HelpTopic topic = controller.findTopicByDisplayName("Net Profit");
            assertThat(topic).isEqualTo(HelpTopic.NET_PROFIT);
        }

        @Test
        @DisplayName("should find topic by display name for Income Tax")
        void shouldFindTopicByDisplayNameIncomeTax() {
            HelpTopic topic = controller.findTopicByDisplayName("Income Tax");
            assertThat(topic).isEqualTo(HelpTopic.INCOME_TAX);
        }

        @Test
        @DisplayName("should find topic by display name for Personal Allowance")
        void shouldFindTopicByDisplayNamePersonalAllowance() {
            HelpTopic topic = controller.findTopicByDisplayName("Personal Allowance");
            assertThat(topic).isEqualTo(HelpTopic.PERSONAL_ALLOWANCE);
        }

        @Test
        @DisplayName("should return null for unknown display name")
        void shouldReturnNullForUnknownDisplayName() {
            HelpTopic topic = controller.findTopicByDisplayName("Unknown Topic");
            assertThat(topic).isNull();
        }
    }
}
