package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SettingsController.
 * Tests the controller logic for the Settings page.
 */
@DisplayName("SettingsController")
class SettingsControllerTest {

    private SettingsController controller;

    @BeforeEach
    void setUp() {
        controller = new SettingsController();
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
    @DisplayName("Application Version")
    class ApplicationVersion {

        @Test
        @DisplayName("should return application version")
        void shouldReturnApplicationVersion() {
            // When
            String version = controller.getApplicationVersion();

            // Then
            assertThat(version).isNotNull();
            assertThat(version).isNotEmpty();
        }

        @Test
        @DisplayName("should format version string")
        void shouldFormatVersionString() {
            // When
            String versionText = controller.getFormattedVersion();

            // Then
            assertThat(versionText).startsWith("Version ");
        }
    }

    @Nested
    @DisplayName("Legal Documents")
    class LegalDocuments {

        @Test
        @DisplayName("should have terms of service accessible")
        void shouldHaveTermsAccessible() {
            // When/Then
            assertThat(controller.canShowTermsOfService()).isTrue();
        }

        @Test
        @DisplayName("should have privacy notice accessible")
        void shouldHavePrivacyNoticeAccessible() {
            // When/Then
            assertThat(controller.canShowPrivacyNotice()).isTrue();
        }
    }

    @Nested
    @DisplayName("Data Management")
    class DataManagement {

        @Test
        @DisplayName("should support data export")
        void shouldSupportDataExport() {
            // When/Then
            assertThat(controller.canExportData()).isTrue();
        }

        @Test
        @DisplayName("should support data import")
        void shouldSupportDataImport() {
            // When/Then
            assertThat(controller.canImportData()).isTrue();
        }
    }

    @Nested
    @DisplayName("Settings Categories")
    class SettingsCategories {

        @Test
        @DisplayName("should have profile settings category")
        void shouldHaveProfileCategory() {
            assertThat(controller.getSettingsCategories()).contains("Profile");
        }

        @Test
        @DisplayName("should have legal settings category")
        void shouldHaveLegalCategory() {
            assertThat(controller.getSettingsCategories()).contains("Legal");
        }

        @Test
        @DisplayName("should have data settings category")
        void shouldHaveDataCategory() {
            assertThat(controller.getSettingsCategories()).contains("Data");
        }

        @Test
        @DisplayName("should have about settings category")
        void shouldHaveAboutCategory() {
            assertThat(controller.getSettingsCategories()).contains("About");
        }
    }

    @Nested
    @DisplayName("UTR Management")
    class UtrManagement {

        @Test
        @DisplayName("should return empty UTR initially")
        void shouldReturnEmptyUtrInitially() {
            assertThat(controller.getUtr()).isEmpty();
        }

        @Test
        @DisplayName("should store UTR when set")
        void shouldStoreUtr() {
            // When
            controller.setUtr("1234567890");

            // Then
            assertThat(controller.getUtr()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should validate UTR format")
        void shouldValidateUtrFormat() {
            // Valid 10-digit UTR
            assertThat(controller.isValidUtr("1234567890")).isTrue();

            // Invalid formats
            assertThat(controller.isValidUtr("123")).isFalse();
            assertThat(controller.isValidUtr("")).isFalse();
            assertThat(controller.isValidUtr(null)).isFalse();
            assertThat(controller.isValidUtr("abc1234567")).isFalse();
            assertThat(controller.isValidUtr("12345678901")).isFalse();
        }

        @Test
        @DisplayName("should format UTR with spaces for display")
        void shouldFormatUtrForDisplay() {
            // Given
            controller.setUtr("1234567890");

            // When
            String formatted = controller.getFormattedUtr();

            // Then - UTR should be formatted as "12345 67890"
            assertThat(formatted).isEqualTo("12345 67890");
        }

        @Test
        @DisplayName("should return placeholder when no UTR set")
        void shouldReturnPlaceholderWhenNoUtr() {
            // When
            String formatted = controller.getFormattedUtr();

            // Then
            assertThat(formatted).isEqualTo("Not set");
        }
    }
}
