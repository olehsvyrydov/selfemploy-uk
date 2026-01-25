package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.Ikon;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.help.HelpContent;
import uk.selfemploy.ui.help.HelpService;
import uk.selfemploy.ui.help.HelpTopic;
import uk.selfemploy.ui.help.HmrcLinkTopic;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HelpController.
 * Tests the controller logic for the Help page.
 *
 * Test Coverage (as per Sprint 8 Test Design):
 * - TC-HLP-001 to TC-HLP-008: Help Topic Cards Display
 * - TC-HLP-010 to TC-HLP-020: Help Topic Click Handlers
 * - TC-HLP-030 to TC-HLP-037: Quick Links Functionality
 * - TC-HLP-040 to TC-HLP-043: Support Links
 * - TC-HLP-050 to TC-HLP-053: Search Functionality
 * - TC-HLP-060 to TC-HLP-064: Accessibility
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
    @DisplayName("Help Topic Cards Display - TC-HLP-001 to TC-HLP-008")
    class HelpTopicCardsDisplay {

        @Test
        @DisplayName("TC-HLP-001: should have Tax & Calculation category")
        void shouldHaveTaxCategory() {
            assertThat(controller.getHelpCategories()).contains("Tax & Calculation");
        }

        @Test
        @DisplayName("TC-HLP-002: should have Expenses category")
        void shouldHaveExpensesCategory() {
            assertThat(controller.getHelpCategories()).contains("Expenses");
        }

        @Test
        @DisplayName("TC-HLP-003: should have HMRC Submission category")
        void shouldHaveSubmissionCategory() {
            assertThat(controller.getHelpCategories()).contains("HMRC Submission");
        }

        @Test
        @DisplayName("TC-HLP-004: should have General category")
        void shouldHaveGeneralCategory() {
            assertThat(controller.getHelpCategories()).contains("General");
        }

        @Test
        @DisplayName("TC-HLP-005: should have title and description for each topic")
        void shouldHaveTitleAndDescriptionForTopics() {
            for (HelpTopic topic : HelpTopic.values()) {
                String displayName = controller.getTopicDisplayName(topic);
                String description = controller.getTopicDescription(topic);

                assertThat(displayName).isNotNull().isNotBlank();
                assertThat(description).isNotNull();
            }
        }

        @Test
        @DisplayName("TC-HLP-006: should have icon for each topic")
        void shouldHaveIconForTopics() {
            for (HelpTopic topic : HelpTopic.values()) {
                Ikon icon = controller.getTopicIcon(topic);
                assertThat(icon).isNotNull();
            }
        }

        @Test
        @DisplayName("TC-HLP-008: should have category colors")
        void shouldHaveCategoryColors() {
            for (String category : controller.getHelpCategories()) {
                String color = controller.getCategoryColor(category);
                assertThat(color).isNotNull().startsWith("#");
            }
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
    @DisplayName("Help Topic Click Handlers - TC-HLP-010 to TC-HLP-020")
    class HelpTopicClickHandlers {

        @Test
        @DisplayName("TC-HLP-010: should return NET_PROFIT content with correct title")
        void shouldReturnNetProfitContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.NET_PROFIT);

            assertThat(content).isPresent();
            assertThat(content.get().title()).contains("Net Profit");
            assertThat(content.get().body()).isNotBlank();
        }

        @Test
        @DisplayName("TC-HLP-011: should return INCOME_TAX content with correct title")
        void shouldReturnIncomeTaxContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.INCOME_TAX);

            assertThat(content).isPresent();
            assertThat(content.get().title()).contains("Income Tax");
            assertThat(content.get().body()).contains("20%"); // Basic rate
        }

        @Test
        @DisplayName("TC-HLP-012: should return PERSONAL_ALLOWANCE content")
        void shouldReturnPersonalAllowanceContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.PERSONAL_ALLOWANCE);

            assertThat(content).isPresent();
            assertThat(content.get().title()).contains("Personal Allowance");
            assertThat(content.get().body()).contains("12,570"); // PA amount
        }

        @Test
        @DisplayName("TC-HLP-013: should return NI_CLASS_4 content")
        void shouldReturnNiClass4Content() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.NI_CLASS_4);

            assertThat(content).isPresent();
            assertThat(content.get().title()).contains("Class 4");
            assertThat(content.get().body()).contains("6%"); // Main rate
        }

        @Test
        @DisplayName("TC-HLP-014: should return PAYMENTS_ON_ACCOUNT content")
        void shouldReturnPaymentsOnAccountContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.PAYMENTS_ON_ACCOUNT);

            assertThat(content).isPresent();
            assertThat(content.get().title()).contains("Payments on Account");
        }

        @Test
        @DisplayName("TC-HLP-015: should return EXPENSE_CATEGORY content")
        void shouldReturnExpenseCategoryContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.EXPENSE_CATEGORY);

            assertThat(content).isPresent();
            assertThat(content.get().body()).contains("SA103");
        }

        @Test
        @DisplayName("TC-HLP-016: should return ALLOWABLE_EXPENSES content")
        void shouldReturnAllowableExpensesContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.ALLOWABLE_EXPENSES);

            assertThat(content).isPresent();
            assertThat(content.get().title()).contains("Allowable");
        }

        @Test
        @DisplayName("TC-HLP-017: should return DECLARATION content")
        void shouldReturnDeclarationContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.DECLARATION);

            assertThat(content).isPresent();
            assertThat(content.get().title()).contains("Declaration");
        }

        @Test
        @DisplayName("TC-HLP-018: should return HMRC_SUBMISSION content")
        void shouldReturnHmrcSubmissionContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.HMRC_SUBMISSION);

            assertThat(content).isPresent();
            assertThat(content.get().body()).contains("MTD");
        }

        @Test
        @DisplayName("TC-HLP-019: should return TAX_YEAR content")
        void shouldReturnTaxYearContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.TAX_YEAR);

            assertThat(content).isPresent();
            assertThat(content.get().body()).contains("6 April");
        }

        @Test
        @DisplayName("TC-HLP-020: should return SA103_FORM content")
        void shouldReturnSa103FormContent() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.SA103_FORM);

            assertThat(content).isPresent();
            assertThat(content.get().title()).contains("SA103");
        }

        @Test
        @DisplayName("should have handler for each help topic")
        void shouldHaveHandlerForEachTopic() {
            // Verify we have content for each topic
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

    @Nested
    @DisplayName("Quick Links - TC-HLP-030 to TC-HLP-037")
    class QuickLinks {

        @Test
        @DisplayName("TC-HLP-030: should have income tax rates quick link")
        void shouldHaveIncomeTaxRatesLink() {
            List<HmrcLinkTopic> links = controller.getQuickLinks();
            assertThat(links).contains(HmrcLinkTopic.TAX_RATES);
        }

        @Test
        @DisplayName("TC-HLP-031: should have SA103 form quick link")
        void shouldHaveSa103FormLink() {
            List<HmrcLinkTopic> links = controller.getQuickLinks();
            assertThat(links).contains(HmrcLinkTopic.SA103_FORM);
        }

        @Test
        @DisplayName("TC-HLP-032: should have filing deadlines quick link")
        void shouldHaveFilingDeadlinesLink() {
            List<HmrcLinkTopic> links = controller.getQuickLinks();
            assertThat(links).contains(HmrcLinkTopic.FILING_DEADLINES);
        }

        @Test
        @DisplayName("TC-HLP-033: should have allowable expenses quick link")
        void shouldHaveAllowableExpensesLink() {
            List<HmrcLinkTopic> links = controller.getQuickLinks();
            assertThat(links).contains(HmrcLinkTopic.ALLOWABLE_EXPENSES);
        }

        @Test
        @DisplayName("TC-HLP-034: should have NI rates quick link")
        void shouldHaveNiRatesLink() {
            List<HmrcLinkTopic> links = controller.getQuickLinks();
            assertThat(links).contains(HmrcLinkTopic.NI_RATES);
        }

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

        @Test
        @DisplayName("TC-HLP-037: quick links should point to valid GOV.UK domains")
        void quickLinksShouldPointToGovUk() {
            for (HmrcLinkTopic topic : controller.getQuickLinks()) {
                String url = controller.getHmrcLink(topic);
                assertThat(url).contains("gov.uk");
            }
        }
    }

    @Nested
    @DisplayName("Support Links - TC-HLP-040 to TC-HLP-043")
    class SupportLinks {

        @Test
        @DisplayName("TC-HLP-042: should have correct GitHub repository URL format")
        void shouldHaveCorrectGitHubRepoUrl() {
            // The URL should point to a valid GitHub repository
            assertThat(HelpController.GITHUB_REPO_URL)
                .startsWith("https://github.com/");
        }

        @Test
        @DisplayName("TC-HLP-042b: should have correct GitHub Issues URL format")
        void shouldHaveCorrectGitHubIssuesUrl() {
            // The URL should point to the issues page
            assertThat(HelpController.GITHUB_ISSUES_URL)
                .contains("/issues");
        }

        @Test
        @DisplayName("TC-HLP-043: GitHub URLs should not point to incorrect repository")
        void shouldNotPointToIncorrectRepository() {
            // URLs should not point to anthropic (example of wrong org)
            assertThat(HelpController.GITHUB_REPO_URL)
                .doesNotContain("anthropic");
            assertThat(HelpController.GITHUB_ISSUES_URL)
                .doesNotContain("anthropic");
        }

        @Test
        @DisplayName("GitHub issues URL should be derived from repo URL")
        void issuesUrlShouldBeDerivedFromRepoUrl() {
            assertThat(HelpController.GITHUB_ISSUES_URL)
                .startsWith(HelpController.GITHUB_REPO_URL);
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
    @DisplayName("Help Content HMRC Links")
    class HelpContentHmrcLinks {

        @Test
        @DisplayName("TC-EXP-012: help content should include HMRC link when available")
        void helpContentShouldIncludeHmrcLink() {
            // Most help topics should have HMRC links
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.INCOME_TAX);

            assertThat(content).isPresent();
            assertThat(content.get().hmrcLink()).isNotNull().isNotBlank();
            assertThat(content.get().hmrcLink()).contains("gov.uk");
        }

        @Test
        @DisplayName("help content should include link text")
        void helpContentShouldIncludeLinkText() {
            Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.INCOME_TAX);

            assertThat(content).isPresent();
            assertThat(content.get().linkText()).isNotNull().isNotBlank();
        }
    }

    @Nested
    @DisplayName("Category to Topic Mapping")
    class CategoryToTopicMapping {

        @Test
        @DisplayName("should return correct category for topic")
        void shouldReturnCorrectCategoryForTopic() {
            // Tax topics
            assertThat(controller.getCategoryForTopic(HelpTopic.INCOME_TAX))
                .isEqualTo("Tax & Calculation");

            // Expense topics
            assertThat(controller.getCategoryForTopic(HelpTopic.ALLOWABLE_EXPENSES))
                .isEqualTo("Expenses");

            // Submission topics
            assertThat(controller.getCategoryForTopic(HelpTopic.DECLARATION))
                .isEqualTo("HMRC Submission");

            // General topics
            assertThat(controller.getCategoryForTopic(HelpTopic.TAX_YEAR))
                .isEqualTo("General");
        }
    }

    @Nested
    @DisplayName("All Topics Have Complete Content")
    class AllTopicsHaveCompleteContent {

        @Test
        @DisplayName("all help topics should have retrievable content")
        void allTopicsShouldHaveContent() {
            for (HelpTopic topic : HelpTopic.values()) {
                Optional<HelpContent> content = controller.getHelpForTopic(topic);

                assertThat(content)
                    .as("Content should be present for topic: " + topic)
                    .isPresent();

                assertThat(content.get().title())
                    .as("Title should not be blank for topic: " + topic)
                    .isNotBlank();

                assertThat(content.get().body())
                    .as("Body should not be blank for topic: " + topic)
                    .isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("SE-9XX: User Guide Feature")
    class UserGuideFeature {

        @Nested
        @DisplayName("New Help Topics")
        class NewHelpTopics {

            @Test
            @DisplayName("should have GETTING_STARTED topic in User Guide category")
            void shouldHaveGettingStartedTopic() {
                List<HelpTopic> topics = controller.getTopicsForCategory("User Guide");
                assertThat(topics).contains(HelpTopic.GETTING_STARTED);
            }

            @Test
            @DisplayName("should have HMRC_CONNECTION topic in User Guide category")
            void shouldHaveHmrcConnectionTopic() {
                List<HelpTopic> topics = controller.getTopicsForCategory("User Guide");
                assertThat(topics).contains(HelpTopic.HMRC_CONNECTION);
            }

            @Test
            @DisplayName("should have SECURITY_PRIVACY topic in User Guide category")
            void shouldHaveSecurityPrivacyTopic() {
                List<HelpTopic> topics = controller.getTopicsForCategory("User Guide");
                assertThat(topics).contains(HelpTopic.SECURITY_PRIVACY);
            }

            @Test
            @DisplayName("should have FAQ topic in User Guide category")
            void shouldHaveFaqTopic() {
                List<HelpTopic> topics = controller.getTopicsForCategory("User Guide");
                assertThat(topics).contains(HelpTopic.FAQ);
            }

            @Test
            @DisplayName("should have User Guide category")
            void shouldHaveUserGuideCategory() {
                assertThat(controller.getHelpCategories()).contains("User Guide");
            }
        }

        @Nested
        @DisplayName("User Guide Content")
        class UserGuideContent {

            @Test
            @DisplayName("should return GETTING_STARTED content with correct information")
            void shouldReturnGettingStartedContent() {
                Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.GETTING_STARTED);

                assertThat(content).isPresent();
                assertThat(content.get().title()).contains("Getting Started");
                assertThat(content.get().body()).isNotBlank();
            }

            @Test
            @DisplayName("should return HMRC_CONNECTION content with OAuth explanation")
            void shouldReturnHmrcConnectionContent() {
                Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.HMRC_CONNECTION);

                assertThat(content).isPresent();
                assertThat(content.get().body()).containsIgnoringCase("OAuth");
            }

            @Test
            @DisplayName("should return SECURITY_PRIVACY content with encryption info")
            void shouldReturnSecurityPrivacyContent() {
                Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.SECURITY_PRIVACY);

                assertThat(content).isPresent();
                assertThat(content.get().body()).containsIgnoringCase("encrypt");
            }

            @Test
            @DisplayName("should return FAQ content")
            void shouldReturnFaqContent() {
                Optional<HelpContent> content = controller.getHelpForTopic(HelpTopic.FAQ);

                assertThat(content).isPresent();
                assertThat(content.get().title()).containsIgnoringCase("question");
            }
        }

        @Nested
        @DisplayName("Display Names for New Topics")
        class DisplayNamesForNewTopics {

            @Test
            @DisplayName("should return display name for GETTING_STARTED")
            void shouldReturnDisplayNameForGettingStarted() {
                assertThat(controller.getTopicDisplayName(HelpTopic.GETTING_STARTED))
                    .isEqualTo("Getting Started");
            }

            @Test
            @DisplayName("should return display name for HMRC_CONNECTION")
            void shouldReturnDisplayNameForHmrcConnection() {
                assertThat(controller.getTopicDisplayName(HelpTopic.HMRC_CONNECTION))
                    .isEqualTo("HMRC Connection");
            }

            @Test
            @DisplayName("should return display name for SECURITY_PRIVACY")
            void shouldReturnDisplayNameForSecurityPrivacy() {
                assertThat(controller.getTopicDisplayName(HelpTopic.SECURITY_PRIVACY))
                    .isEqualTo("Security & Privacy");
            }

            @Test
            @DisplayName("should return display name for FAQ")
            void shouldReturnDisplayNameForFaq() {
                assertThat(controller.getTopicDisplayName(HelpTopic.FAQ))
                    .isEqualTo("FAQ");
            }
        }

        @Nested
        @DisplayName("Topic Icons for New Topics")
        class TopicIconsForNewTopics {

            @Test
            @DisplayName("should return icon for GETTING_STARTED")
            void shouldReturnIconForGettingStarted() {
                Ikon icon = controller.getTopicIcon(HelpTopic.GETTING_STARTED);
                assertThat(icon).isNotNull();
            }

            @Test
            @DisplayName("should return icon for HMRC_CONNECTION")
            void shouldReturnIconForHmrcConnection() {
                Ikon icon = controller.getTopicIcon(HelpTopic.HMRC_CONNECTION);
                assertThat(icon).isNotNull();
            }

            @Test
            @DisplayName("should return icon for SECURITY_PRIVACY")
            void shouldReturnIconForSecurityPrivacy() {
                Ikon icon = controller.getTopicIcon(HelpTopic.SECURITY_PRIVACY);
                assertThat(icon).isNotNull();
            }

            @Test
            @DisplayName("should return icon for FAQ")
            void shouldReturnIconForFaq() {
                Ikon icon = controller.getTopicIcon(HelpTopic.FAQ);
                assertThat(icon).isNotNull();
            }
        }

        @Nested
        @DisplayName("Topic Descriptions for New Topics")
        class TopicDescriptionsForNewTopics {

            @Test
            @DisplayName("should return description for GETTING_STARTED")
            void shouldReturnDescriptionForGettingStarted() {
                String desc = controller.getTopicDescription(HelpTopic.GETTING_STARTED);
                assertThat(desc).isNotNull().isNotBlank();
            }

            @Test
            @DisplayName("should return description for HMRC_CONNECTION")
            void shouldReturnDescriptionForHmrcConnection() {
                String desc = controller.getTopicDescription(HelpTopic.HMRC_CONNECTION);
                assertThat(desc).isNotNull().isNotBlank();
            }

            @Test
            @DisplayName("should return description for SECURITY_PRIVACY")
            void shouldReturnDescriptionForSecurityPrivacy() {
                String desc = controller.getTopicDescription(HelpTopic.SECURITY_PRIVACY);
                assertThat(desc).isNotNull().isNotBlank();
            }

            @Test
            @DisplayName("should return description for FAQ")
            void shouldReturnDescriptionForFaq() {
                String desc = controller.getTopicDescription(HelpTopic.FAQ);
                assertThat(desc).isNotNull().isNotBlank();
            }
        }

        @Nested
        @DisplayName("User Guide Dialog")
        class UserGuideDialog {

            @Test
            @DisplayName("should have user guide content available")
            void shouldHaveUserGuideContentAvailable() {
                String userGuideContent = controller.getUserGuideContent();
                assertThat(userGuideContent).isNotNull().isNotBlank();
            }

            @Test
            @DisplayName("user guide content should include key sections")
            void userGuideContentShouldIncludeKeySections() {
                String content = controller.getUserGuideContent();

                assertThat(content).contains("What This App Does");
                assertThat(content).contains("Getting Started");
                assertThat(content).contains("Daily Usage");
                assertThat(content).contains("HMRC Connection");
                assertThat(content).contains("Security");
                assertThat(content).contains("Deadline");
            }
        }

        @Nested
        @DisplayName("Category Color for User Guide")
        class CategoryColorForUserGuide {

            @Test
            @DisplayName("should have color for User Guide category")
            void shouldHaveColorForUserGuideCategory() {
                String color = controller.getCategoryColor("User Guide");
                assertThat(color).isNotNull().startsWith("#");
            }
        }
    }
}
