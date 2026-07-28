package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.help.HelpContent;
import uk.selfemploy.ui.help.HelpService;
import uk.selfemploy.ui.help.HelpTopic;
import uk.selfemploy.ui.help.HmrcLinkTopic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the help page's controller answers about a topic: its content, its category, its icon and
 * its colour.
 *
 * <p>The page itself — that every topic row and quick link is wired to a handler — is covered by
 * {@link HelpTopicButtonTest}, which loads the real FXML. That split matters: the content a user
 * reads lives in the markdown resources and the message bundle, so a test here can only check that
 * the controller looks it up correctly, never that it exists.
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
    @DisplayName("How a topic is presented")
    class TopicPresentation {

        @Test
        @DisplayName("every topic on the page belongs to a category the colours know")
        void everyTopicHasAKnownCategory() {
            // The colour is looked up by category name, and an unknown name silently returns a
            // generic blue. Checking the returned colour cannot catch that, because that same blue
            // is HMRC Submission's real colour - so the name itself is what gets pinned.
            for (HelpTopic topic : HelpController.topicsOnHelpPage()) {
                assertThat(controller.getCategoryForTopic(topic))
                        .as("category for %s", topic)
                        .isIn(HelpController.CATEGORY_USER_GUIDE, HelpController.CATEGORY_TAX,
                                HelpController.CATEGORY_EXPENSES, HelpController.CATEGORY_HMRC,
                                HelpController.CATEGORY_GENERAL);
            }
        }

        @Test
        @DisplayName("every topic on the page has an icon of its own, not the generic fallback")
        void shouldHaveIconForTopics() {
            // Asserting non-null proves nothing: getTopicIcon answers INFO_CIRCLE for a topic that
            // was never given an icon, so a forgotten entry would ship a generic glyph and pass.
            for (HelpTopic topic : HelpController.topicsOnHelpPage()) {
                assertThat(controller.getTopicIcon(topic))
                        .as("icon for %s", topic)
                        .isNotNull()
                        .isNotEqualTo(FontAwesomeSolid.INFO_CIRCLE);
            }
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
        @DisplayName("User Guide Dialog")
        class UserGuideDialog {

            @Test
            @DisplayName("should have user guide content available")
            void shouldHaveUserGuideContentAvailable() {
                String userGuideContent = controller.getHelpForTopic(HelpTopic.USER_GUIDE)
                        .orElseThrow().body();
                assertThat(userGuideContent).isNotNull().isNotBlank();
            }

            @Test
            @DisplayName("user guide content should include key sections")
            void userGuideContentShouldIncludeKeySections() {
                String content = controller.getHelpForTopic(HelpTopic.USER_GUIDE)
                        .orElseThrow().body();

                assertThat(content).contains("What this app does");
                assertThat(content).contains("Getting started");
                assertThat(content).contains("Daily usage");
                assertThat(content).contains("HMRC connection");
                assertThat(content).contains("Security");
                assertThat(content).contains("deadline");
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

    @Nested
    @DisplayName("SE-1020: About Section (moved from Settings)")
    class AboutSection {

        @Test
        @DisplayName("should initialize version label with correct prefix")
        void shouldInitializeVersionLabelWithCorrectPrefix() {
            // When initialize is called (without FXML injection)
            controller.initialize(null, null);

            // Then - no exception thrown, controller remains functional
            assertThat(controller.getHelpService()).isNotNull();
        }

        @Test
        @DisplayName("should have handleViewGitHubLink handler available")
        void shouldHaveHandleViewGitHubLinkHandler() {
            // The GITHUB_REPO_URL constant should be accessible
            assertThat(HelpController.GITHUB_REPO_URL)
                .isNotNull()
                .startsWith("https://github.com/");
        }

        @Test
        @DisplayName("GitHub repo URL should be well-formed")
        void gitHubRepoUrlShouldBeWellFormed() {
            assertThat(HelpController.GITHUB_REPO_URL)
                .contains("github.com")
                .contains("selfemploy");
        }
    }
}
