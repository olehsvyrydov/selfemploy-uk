package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.util.BrowserUtil;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SettingsController.
 * Tests the controller logic for the Settings page.
 *
 * <p>Test Categories:
 * <ul>
 *   <li>Initialization Tests - Controller setup and interface implementation</li>
 *   <li>Tax Year Management Tests - Tax year handling</li>
 *   <li>Application Version Tests - Version display formatting</li>
 *   <li>Settings Categories Tests - Category organization</li>
 *   <li>Legal Documents Tests - Terms and privacy access</li>
 *   <li>Data Management Tests - Export/import functionality</li>
 *   <li>UTR Management Tests - UTR validation and formatting</li>
 *   <li>URL Configuration Tests - External link configuration</li>
 *   <li>Event Handler Tests - FXML action handler behavior</li>
 *   <li>Edge Cases - Boundary conditions and error handling</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsController")
class SettingsControllerTest {

    private SettingsController controller;

    @BeforeEach
    void setUp() {
        controller = new SettingsController();
    }

    // ========================================================================
    // Initialization Tests
    // ========================================================================

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

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

        @Test
        @DisplayName("should create controller without errors")
        void shouldCreateControllerWithoutErrors() {
            assertThat(controller).isNotNull();
        }

        @Test
        @DisplayName("should initialize with null FXML fields without throwing")
        void shouldInitializeWithNullFxmlFields() {
            // When - initialize is called without FXML injection
            controller.initialize(null, null);

            // Then - no exception is thrown, controller remains usable
            assertThat(controller.getApplicationVersion()).isNotNull();
        }

        @Test
        @DisplayName("should have default empty UTR after initialization")
        void shouldHaveDefaultEmptyUtrAfterInitialization() {
            controller.initialize(null, null);
            assertThat(controller.getUtr()).isEmpty();
        }

        @Test
        @DisplayName("should have null tax year before setting")
        void shouldHaveNullTaxYearBeforeSetting() {
            assertThat(controller.getTaxYear()).isNull();
        }
    }

    // ========================================================================
    // Tax Year Management Tests
    // ========================================================================

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

        @Test
        @DisplayName("should update tax year when changed")
        void shouldUpdateTaxYear() {
            // Given
            TaxYear year2024 = TaxYear.of(2024);
            TaxYear year2025 = TaxYear.of(2025);
            controller.setTaxYear(year2024);

            // When
            controller.setTaxYear(year2025);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(year2025);
        }

        @Test
        @DisplayName("should accept multiple tax year changes")
        void shouldAcceptMultipleTaxYearChanges() {
            // Given
            TaxYear year2023 = TaxYear.of(2023);
            TaxYear year2024 = TaxYear.of(2024);
            TaxYear year2025 = TaxYear.of(2025);

            // When/Then
            controller.setTaxYear(year2023);
            assertThat(controller.getTaxYear()).isEqualTo(year2023);

            controller.setTaxYear(year2024);
            assertThat(controller.getTaxYear()).isEqualTo(year2024);

            controller.setTaxYear(year2025);
            assertThat(controller.getTaxYear()).isEqualTo(year2025);
        }

        @Test
        @DisplayName("should allow setting same tax year multiple times")
        void shouldAllowSettingSameTaxYearMultipleTimes() {
            // Given
            TaxYear taxYear = TaxYear.of(2025);

            // When
            controller.setTaxYear(taxYear);
            controller.setTaxYear(taxYear);
            controller.setTaxYear(taxYear);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(taxYear);
        }
    }

    // ========================================================================
    // Application Version Tests
    // ========================================================================

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
        @DisplayName("should format version string with prefix")
        void shouldFormatVersionString() {
            // When
            String versionText = controller.getFormattedVersion();

            // Then
            assertThat(versionText).startsWith("Version ");
        }

        @Test
        @DisplayName("should contain version number in formatted string")
        void shouldContainVersionNumberInFormattedString() {
            // When
            String versionText = controller.getFormattedVersion();

            // Then
            assertThat(versionText).contains(controller.getApplicationVersion());
        }

        @Test
        @DisplayName("should return consistent version across calls")
        void shouldReturnConsistentVersion() {
            // When
            String version1 = controller.getApplicationVersion();
            String version2 = controller.getApplicationVersion();
            String version3 = controller.getApplicationVersion();

            // Then
            assertThat(version1).isEqualTo(version2).isEqualTo(version3);
        }

        @Test
        @DisplayName("should have version in expected format")
        void shouldHaveVersionInExpectedFormat() {
            // When
            String version = controller.getApplicationVersion();

            // Then - should match semantic versioning pattern (e.g., 0.1.0-SNAPSHOT)
            assertThat(version).matches("\\d+\\.\\d+\\.\\d+(-[A-Za-z0-9]+)?");
        }

        @Test
        @DisplayName("should format version with correct prefix")
        void shouldFormatVersionWithCorrectPrefix() {
            // When
            String formatted = controller.getFormattedVersion();

            // Then
            assertThat(formatted).isEqualTo("Version " + controller.getApplicationVersion());
        }
    }

    // ========================================================================
    // Settings Categories Tests
    // ========================================================================

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

        @Test
        @DisplayName("should have exactly four categories")
        void shouldHaveExactlyFourCategories() {
            assertThat(controller.getSettingsCategories()).hasSize(4);
        }

        @Test
        @DisplayName("should return categories in order")
        void shouldReturnCategoriesInOrder() {
            // When
            List<String> categories = controller.getSettingsCategories();

            // Then
            assertThat(categories).containsExactly("Profile", "Legal", "Data", "About");
        }

        @Test
        @DisplayName("should return consistent categories across calls")
        void shouldReturnConsistentCategoriesAcrossCalls() {
            // When
            List<String> categories1 = controller.getSettingsCategories();
            List<String> categories2 = controller.getSettingsCategories();

            // Then
            assertThat(categories1).isEqualTo(categories2);
        }

        @Test
        @DisplayName("should return immutable list reference")
        void shouldReturnImmutableListReference() {
            // When
            List<String> categories = controller.getSettingsCategories();

            // Then - same reference returned each time
            assertThat(categories).isSameAs(controller.getSettingsCategories());
        }
    }

    // ========================================================================
    // Legal Documents Tests
    // ========================================================================

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

        @Test
        @DisplayName("should consistently report terms accessibility")
        void shouldConsistentlyReportTermsAccessibility() {
            // When
            boolean accessible1 = controller.canShowTermsOfService();
            boolean accessible2 = controller.canShowTermsOfService();

            // Then
            assertThat(accessible1).isEqualTo(accessible2);
        }

        @Test
        @DisplayName("should consistently report privacy accessibility")
        void shouldConsistentlyReportPrivacyAccessibility() {
            // When
            boolean accessible1 = controller.canShowPrivacyNotice();
            boolean accessible2 = controller.canShowPrivacyNotice();

            // Then
            assertThat(accessible1).isEqualTo(accessible2);
        }
    }

    // ========================================================================
    // Data Management Tests
    // ========================================================================

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

        @Test
        @DisplayName("should consistently report export capability")
        void shouldConsistentlyReportExportCapability() {
            // When
            boolean canExport1 = controller.canExportData();
            boolean canExport2 = controller.canExportData();

            // Then
            assertThat(canExport1).isEqualTo(canExport2);
        }

        @Test
        @DisplayName("should consistently report import capability")
        void shouldConsistentlyReportImportCapability() {
            // When
            boolean canImport1 = controller.canImportData();
            boolean canImport2 = controller.canImportData();

            // Then
            assertThat(canImport1).isEqualTo(canImport2);
        }
    }

    // ========================================================================
    // UTR Management Tests
    // ========================================================================

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
        @DisplayName("should update UTR when changed")
        void shouldUpdateUtrWhenChanged() {
            // Given
            controller.setUtr("1234567890");

            // When
            controller.setUtr("0987654321");

            // Then
            assertThat(controller.getUtr()).isEqualTo("0987654321");
        }

        @Test
        @DisplayName("should handle null UTR by setting to empty string")
        void shouldHandleNullUtr() {
            // Given
            controller.setUtr("1234567890");

            // When
            controller.setUtr(null);

            // Then
            assertThat(controller.getUtr()).isEmpty();
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

        @Test
        @DisplayName("should return placeholder when UTR is null")
        void shouldReturnPlaceholderWhenUtrNull() {
            // Given
            controller.setUtr(null);

            // When
            String formatted = controller.getFormattedUtr();

            // Then
            assertThat(formatted).isEqualTo("Not set");
        }

        @Test
        @DisplayName("should return UTR as-is when not 10 digits")
        void shouldReturnUtrAsIsWhenNot10Digits() {
            // Given
            controller.setUtr("12345");

            // When
            String formatted = controller.getFormattedUtr();

            // Then - should return unformatted
            assertThat(formatted).isEqualTo("12345");
        }
    }

    // ========================================================================
    // UTR Validation Tests
    // ========================================================================

    @Nested
    @DisplayName("UTR Validation")
    class UtrValidation {

        @Test
        @DisplayName("should validate valid 10-digit UTR")
        void shouldValidateValid10DigitUtr() {
            assertThat(controller.isValidUtr("1234567890")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"0000000000", "9999999999", "1111111111"})
        @DisplayName("should validate various 10-digit UTRs")
        void shouldValidateVarious10DigitUtrs(String utr) {
            assertThat(controller.isValidUtr(utr)).isTrue();
        }

        @Test
        @DisplayName("should reject UTR with less than 10 digits")
        void shouldRejectUtrWithLessThan10Digits() {
            assertThat(controller.isValidUtr("123456789")).isFalse();
        }

        @Test
        @DisplayName("should reject UTR with more than 10 digits")
        void shouldRejectUtrWithMoreThan10Digits() {
            assertThat(controller.isValidUtr("12345678901")).isFalse();
        }

        @Test
        @DisplayName("should reject empty UTR")
        void shouldRejectEmptyUtr() {
            assertThat(controller.isValidUtr("")).isFalse();
        }

        @Test
        @DisplayName("should reject null UTR")
        void shouldRejectNullUtr() {
            assertThat(controller.isValidUtr(null)).isFalse();
        }

        @Test
        @DisplayName("should reject UTR with letters")
        void shouldRejectUtrWithLetters() {
            assertThat(controller.isValidUtr("abc1234567")).isFalse();
        }

        @Test
        @DisplayName("should reject UTR with special characters")
        void shouldRejectUtrWithSpecialCharacters() {
            assertThat(controller.isValidUtr("123-456-78")).isFalse();
        }

        @Test
        @DisplayName("should reject UTR with spaces")
        void shouldRejectUtrWithSpaces() {
            assertThat(controller.isValidUtr("12345 67890")).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABCDEFGHIJ", "123456789a", "!@#$%^&*()"})
        @DisplayName("should reject non-numeric UTRs")
        void shouldRejectNonNumericUtrs(String utr) {
            assertThat(controller.isValidUtr(utr)).isFalse();
        }

        @Test
        @DisplayName("should reject UTR with leading zeros but wrong length")
        void shouldRejectUtrWithWrongLength() {
            assertThat(controller.isValidUtr("000000001")).isFalse();  // 9 digits
            assertThat(controller.isValidUtr("00000000001")).isFalse();  // 11 digits
        }

        @Test
        @DisplayName("should accept UTR with all zeros")
        void shouldAcceptUtrWithAllZeros() {
            assertThat(controller.isValidUtr("0000000000")).isTrue();
        }

        @Test
        @DisplayName("should reject UTR with unicode digits")
        void shouldRejectUtrWithUnicodeDigits() {
            // Arabic-Indic digits look like numbers but are different characters
            assertThat(controller.isValidUtr("\u0660\u0661\u0662\u0663\u0664\u0665\u0666\u0667\u0668\u0669")).isFalse();
        }
    }

    // ========================================================================
    // URL Configuration Tests
    // ========================================================================

    @Nested
    @DisplayName("URL Configuration")
    class UrlConfiguration {

        @Test
        @DisplayName("should return null for unconfigured URL")
        void shouldReturnNullForUnconfiguredUrl() {
            // When
            String url = controller.getConfiguredUrl("app.url.nonexistent");

            // Then
            assertThat(url).isNull();
        }

        @Test
        @DisplayName("should return system property value when set")
        void shouldReturnSystemPropertyWhenSet() {
            // Given
            String key = "app.url.test.unique." + System.currentTimeMillis();
            String expectedUrl = "https://example.com";
            System.setProperty(key, expectedUrl);

            try {
                // When
                String url = controller.getConfiguredUrl(key);

                // Then
                assertThat(url).isEqualTo(expectedUrl);
            } finally {
                System.clearProperty(key);
            }
        }

        @Test
        @DisplayName("should return null for empty system property")
        void shouldReturnNullForEmptySystemProperty() {
            // Given
            String key = "app.url.empty.test." + System.currentTimeMillis();
            System.setProperty(key, "");

            try {
                // When
                String url = controller.getConfiguredUrl(key);

                // Then
                assertThat(url).isNull();
            } finally {
                System.clearProperty(key);
            }
        }

        @Test
        @DisplayName("should handle key with dots correctly")
        void shouldHandleKeyWithDotsCorrectly() {
            // When - key with multiple dots
            String url = controller.getConfiguredUrl("app.nested.deep.url.key");

            // Then - should not throw, just return null if not set
            assertThat(url).isNull();
        }
    }

    // ========================================================================
    // External Link Tests
    // ========================================================================

    @Nested
    @DisplayName("External Links")
    class ExternalLinks {

        @Test
        @DisplayName("should call BrowserUtil to open external link")
        void shouldCallBrowserUtilToOpenExternalLink() {
            try (MockedStatic<BrowserUtil> browserUtil = mockStatic(BrowserUtil.class)) {
                // Given
                String url = "https://example.com";

                // When
                controller.openExternalLink(url);

                // Then
                browserUtil.verify(() -> BrowserUtil.openUrl(eq(url), any(Consumer.class)));
            }
        }

        @Test
        @DisplayName("should pass error callback to BrowserUtil")
        void shouldPassErrorCallbackToBrowserUtil() {
            try (MockedStatic<BrowserUtil> browserUtil = mockStatic(BrowserUtil.class)) {
                // Given
                String url = "https://test.com";

                // When
                controller.openExternalLink(url);

                // Then - verify callback was provided (not null)
                browserUtil.verify(() -> BrowserUtil.openUrl(eq(url), any(Consumer.class)));
            }
        }
    }

    // ========================================================================
    // Event Handler Tests (without JavaFX toolkit)
    // ========================================================================

    @Nested
    @DisplayName("Event Handlers - Logic")
    class EventHandlersLogic {

        @Test
        @DisplayName("should handle save UTR with null utrField gracefully")
        void shouldHandleSaveUtrWithNullUtrField() {
            // Given - no FXML injection (utrField is null)
            ActionEvent event = mock(ActionEvent.class);

            // When - should not throw even without utrField
            controller.handleSaveUtr(event);

            // Then - no exception, UTR unchanged
            assertThat(controller.getUtr()).isEmpty();
        }

        @Test
        @DisplayName("should handle export data with null button gracefully")
        void shouldHandleExportDataWithNullButton() {
            // Given - no FXML injection
            ActionEvent event = mock(ActionEvent.class);

            // When/Then - should not throw
            controller.handleExportData(event);
        }

        @Test
        @DisplayName("should handle import data with null button gracefully")
        void shouldHandleImportDataWithNullButton() {
            // Given - no FXML injection
            ActionEvent event = mock(ActionEvent.class);

            // When/Then - should not throw
            controller.handleImportData(event);
        }

        @Test
        @DisplayName("should call openExternalLink when GitHub URL is configured")
        void shouldCallOpenExternalLinkWhenGitHubUrlConfigured() {
            // Given - URL configured via system property
            String key = "app.url.github";
            String expectedUrl = "https://github.com/test/repo";
            System.setProperty(key, expectedUrl);

            try (MockedStatic<BrowserUtil> browserUtil = mockStatic(BrowserUtil.class)) {
                ActionEvent event = mock(ActionEvent.class);

                // When
                controller.handleGitHubLink(event);

                // Then - BrowserUtil should be called with the URL
                browserUtil.verify(() -> BrowserUtil.openUrl(eq(expectedUrl), any(Consumer.class)));
            } finally {
                System.clearProperty(key);
            }
        }

        @Test
        @DisplayName("should call openExternalLink when Report Issue URL is configured")
        void shouldCallOpenExternalLinkWhenReportIssueUrlConfigured() {
            // Given - URL configured via system property
            String key = "app.url.issues";
            String expectedUrl = "https://github.com/test/repo/issues";
            System.setProperty(key, expectedUrl);

            try (MockedStatic<BrowserUtil> browserUtil = mockStatic(BrowserUtil.class)) {
                ActionEvent event = mock(ActionEvent.class);

                // When
                controller.handleReportIssueLink(event);

                // Then - BrowserUtil should be called with the URL
                browserUtil.verify(() -> BrowserUtil.openUrl(eq(expectedUrl), any(Consumer.class)));
            } finally {
                System.clearProperty(key);
            }
        }
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle multiple initialize calls")
        void shouldHandleMultipleInitializeCalls() {
            // When
            controller.initialize(null, null);
            controller.initialize(null, null);
            controller.initialize(null, null);

            // Then - should not throw, state remains valid
            assertThat(controller.getApplicationVersion()).isNotNull();
        }

        @Test
        @DisplayName("should maintain state across method calls")
        void shouldMaintainStateAcrossMethodCalls() {
            // Given
            TaxYear taxYear = TaxYear.of(2025);
            controller.setTaxYear(taxYear);
            controller.setUtr("1234567890");

            // When - call various methods
            controller.getApplicationVersion();
            controller.getSettingsCategories();
            controller.canExportData();

            // Then - state unchanged
            assertThat(controller.getTaxYear()).isEqualTo(taxYear);
            assertThat(controller.getUtr()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("should handle empty string UTR")
        void shouldHandleEmptyStringUtr() {
            // When
            controller.setUtr("");

            // Then
            assertThat(controller.getUtr()).isEmpty();
            assertThat(controller.getFormattedUtr()).isEqualTo("Not set");
        }

        @Test
        @DisplayName("should handle UTR with only spaces")
        void shouldHandleUtrWithOnlySpaces() {
            // When
            controller.setUtr("          ");

            // Then - stored as-is but not valid
            assertThat(controller.isValidUtr("          ")).isFalse();
        }

        @Test
        @DisplayName("should validate UTR before and after setting")
        void shouldValidateUtrBeforeAndAfterSetting() {
            // Given
            String validUtr = "1234567890";
            String invalidUtr = "12345";

            // When/Then - validation should work independently of set state
            assertThat(controller.isValidUtr(validUtr)).isTrue();
            assertThat(controller.isValidUtr(invalidUtr)).isFalse();

            controller.setUtr(validUtr);

            assertThat(controller.isValidUtr(validUtr)).isTrue();
            assertThat(controller.isValidUtr(invalidUtr)).isFalse();
        }
    }

    // ========================================================================
    // Formatted UTR Edge Cases
    // ========================================================================

    @Nested
    @DisplayName("Formatted UTR Edge Cases")
    class FormattedUtrEdgeCases {

        @Test
        @DisplayName("should format 10-digit UTR correctly")
        void shouldFormat10DigitUtrCorrectly() {
            controller.setUtr("9876543210");
            assertThat(controller.getFormattedUtr()).isEqualTo("98765 43210");
        }

        @Test
        @DisplayName("should return short UTR unformatted")
        void shouldReturnShortUtrUnformatted() {
            controller.setUtr("123");
            assertThat(controller.getFormattedUtr()).isEqualTo("123");
        }

        @Test
        @DisplayName("should return long UTR unformatted")
        void shouldReturnLongUtrUnformatted() {
            controller.setUtr("12345678901234");
            assertThat(controller.getFormattedUtr()).isEqualTo("12345678901234");
        }

        @Test
        @DisplayName("should handle single character UTR")
        void shouldHandleSingleCharacterUtr() {
            controller.setUtr("1");
            assertThat(controller.getFormattedUtr()).isEqualTo("1");
        }

        @Test
        @DisplayName("should handle 9-digit UTR without formatting")
        void shouldHandle9DigitUtrWithoutFormatting() {
            controller.setUtr("123456789");
            assertThat(controller.getFormattedUtr()).isEqualTo("123456789");
        }

        @Test
        @DisplayName("should handle 11-digit UTR without formatting")
        void shouldHandle11DigitUtrWithoutFormatting() {
            controller.setUtr("12345678901");
            assertThat(controller.getFormattedUtr()).isEqualTo("12345678901");
        }
    }

    // ========================================================================
    // Tax Year Format Tests
    // ========================================================================

    @Nested
    @DisplayName("Tax Year Integration")
    class TaxYearIntegration {

        @Test
        @DisplayName("should work with current tax year")
        void shouldWorkWithCurrentTaxYear() {
            // Given
            TaxYear current = TaxYear.current();

            // When
            controller.setTaxYear(current);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(current);
        }

        @Test
        @DisplayName("should work with historical tax years")
        void shouldWorkWithHistoricalTaxYears() {
            // Given
            TaxYear year2020 = TaxYear.of(2020);

            // When
            controller.setTaxYear(year2020);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(year2020);
        }

        @Test
        @DisplayName("should work with future tax years")
        void shouldWorkWithFutureTaxYears() {
            // Given
            TaxYear year2030 = TaxYear.of(2030);

            // When
            controller.setTaxYear(year2030);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(year2030);
        }
    }

    // ========================================================================
    // Version String Tests
    // ========================================================================

    @Nested
    @DisplayName("Version String Patterns")
    class VersionStringPatterns {

        @Test
        @DisplayName("should have non-empty version")
        void shouldHaveNonEmptyVersion() {
            assertThat(controller.getApplicationVersion()).isNotBlank();
        }

        @Test
        @DisplayName("should have formatted version starting with Version prefix")
        void shouldHaveFormattedVersionPrefix() {
            assertThat(controller.getFormattedVersion()).startsWith("Version ");
        }

        @Test
        @DisplayName("should have formatted version containing actual version")
        void shouldHaveFormattedVersionContainingActualVersion() {
            String version = controller.getApplicationVersion();
            String formatted = controller.getFormattedVersion();

            assertThat(formatted).contains(version);
        }

        @Test
        @DisplayName("should match expected formatted version structure")
        void shouldMatchExpectedFormattedVersionStructure() {
            String formatted = controller.getFormattedVersion();

            // Should be "Version X.Y.Z" or "Version X.Y.Z-SUFFIX"
            assertThat(formatted).matches("Version \\d+\\.\\d+\\.\\d+(-[A-Za-z0-9]+)?");
        }
    }
}
