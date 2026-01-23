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
}
