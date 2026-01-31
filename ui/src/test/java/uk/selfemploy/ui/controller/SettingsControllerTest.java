package uk.selfemploy.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
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
            assertThat(controller.getSettingsCategories()).isNotEmpty();
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
        @DisplayName("should have exactly three categories (About moved to Help)")
        void shouldHaveExactlyThreeCategories() {
            assertThat(controller.getSettingsCategories()).hasSize(3);
        }

        @Test
        @DisplayName("should return categories in order")
        void shouldReturnCategoriesInOrder() {
            // When
            List<String> categories = controller.getSettingsCategories();

            // Then
            assertThat(categories).containsExactly("Profile", "Legal", "Data");
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
    // Display Name Management Tests
    // ========================================================================

    @Nested
    @DisplayName("Display Name Management")
    class DisplayNameManagement {

        @Test
        @DisplayName("should return empty display name initially")
        void shouldReturnEmptyDisplayNameInitially() {
            assertThat(controller.getDisplayName()).isEmpty();
        }

        @Test
        @DisplayName("should store display name when set")
        void shouldStoreDisplayName() {
            // When
            controller.setDisplayName("Sarah");

            // Then
            assertThat(controller.getDisplayName()).isEqualTo("Sarah");
        }

        @Test
        @DisplayName("should update display name when changed")
        void shouldUpdateDisplayNameWhenChanged() {
            // Given
            controller.setDisplayName("Sarah");

            // When
            controller.setDisplayName("John Smith");

            // Then
            assertThat(controller.getDisplayName()).isEqualTo("John Smith");
        }

        @Test
        @DisplayName("should handle null display name by setting to empty string")
        void shouldHandleNullDisplayName() {
            // Given
            controller.setDisplayName("Sarah");

            // When
            controller.setDisplayName(null);

            // Then
            assertThat(controller.getDisplayName()).isEmpty();
        }

        @Test
        @DisplayName("should trim whitespace from display name")
        void shouldTrimWhitespaceFromDisplayName() {
            // When
            controller.setDisplayName("  Sarah  ");

            // Then
            assertThat(controller.getDisplayName()).isEqualTo("Sarah");
        }

        @Test
        @DisplayName("should format display name for greeting")
        void shouldFormatDisplayNameForGreeting() {
            // Given
            controller.setDisplayName("Sarah");

            // When
            String formatted = controller.getFormattedDisplayName();

            // Then
            assertThat(formatted).isEqualTo("Sarah");
        }

        @Test
        @DisplayName("should return placeholder when no display name set")
        void shouldReturnPlaceholderWhenNoDisplayName() {
            // When
            String formatted = controller.getFormattedDisplayName();

            // Then
            assertThat(formatted).isEqualTo("Not set");
        }

        @Test
        @DisplayName("should return placeholder when display name is empty")
        void shouldReturnPlaceholderWhenDisplayNameEmpty() {
            // Given
            controller.setDisplayName("");

            // When
            String formatted = controller.getFormattedDisplayName();

            // Then
            assertThat(formatted).isEqualTo("Not set");
        }

        @Test
        @DisplayName("should handle special characters in display name")
        void shouldHandleSpecialCharactersInDisplayName() {
            // When
            controller.setDisplayName("O'Brien-Smith");

            // Then
            assertThat(controller.getDisplayName()).isEqualTo("O'Brien-Smith");
        }

        @Test
        @DisplayName("should handle unicode characters in display name")
        void shouldHandleUnicodeCharactersInDisplayName() {
            // When
            controller.setDisplayName("Émile François");

            // Then
            assertThat(controller.getDisplayName()).isEqualTo("Émile François");
        }

        @Test
        @DisplayName("should return true when display name is set")
        void shouldReturnTrueWhenDisplayNameIsSet() {
            // When
            controller.setDisplayName("Sarah");

            // Then
            assertThat(controller.hasDisplayName()).isTrue();
        }

        @Test
        @DisplayName("should return false when display name is not set")
        void shouldReturnFalseWhenDisplayNameIsNotSet() {
            // Then
            assertThat(controller.hasDisplayName()).isFalse();
        }

        @Test
        @DisplayName("should return false when display name is empty")
        void shouldReturnFalseWhenDisplayNameIsEmpty() {
            // When
            controller.setDisplayName("");

            // Then
            assertThat(controller.hasDisplayName()).isFalse();
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
            assertThat(controller.getSettingsCategories()).isNotEmpty();
        }

        @Test
        @DisplayName("should maintain state across method calls")
        void shouldMaintainStateAcrossMethodCalls() {
            // Given
            TaxYear taxYear = TaxYear.of(2025);
            controller.setTaxYear(taxYear);
            controller.setUtr("1234567890");

            // When - call various methods
            controller.getSettingsCategories();
            controller.canExportData();
            controller.canImportData();

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
    // Sandbox Mode Detection Tests
    // ========================================================================

    @Nested
    @DisplayName("Sandbox Mode Detection")
    class SandboxModeDetection {

        @Test
        @DisplayName("should detect sandbox mode when URL contains test-api")
        void shouldDetectSandboxModeWhenUrlContainsTestApi() {
            // Given
            String sandboxUrl = "https://test-api.service.hmrc.gov.uk";

            // When
            boolean isSandbox = controller.isSandboxMode(sandboxUrl);

            // Then
            assertThat(isSandbox).isTrue();
        }

        @Test
        @DisplayName("should detect production mode when URL does not contain test-api")
        void shouldDetectProductionModeWhenUrlDoesNotContainTestApi() {
            // Given
            String productionUrl = "https://api.service.hmrc.gov.uk";

            // When
            boolean isSandbox = controller.isSandboxMode(productionUrl);

            // Then
            assertThat(isSandbox).isFalse();
        }

        @Test
        @DisplayName("should handle null URL gracefully")
        void shouldHandleNullUrlGracefully() {
            // When
            boolean isSandbox = controller.isSandboxMode(null);

            // Then - default to production mode (safer)
            assertThat(isSandbox).isFalse();
        }

        @Test
        @DisplayName("should handle empty URL gracefully")
        void shouldHandleEmptyUrlGracefully() {
            // When
            boolean isSandbox = controller.isSandboxMode("");

            // Then - default to production mode (safer)
            assertThat(isSandbox).isFalse();
        }

        @Test
        @DisplayName("should be case insensitive for test-api detection")
        void shouldBeCaseInsensitiveForTestApiDetection() {
            // Given
            String mixedCaseUrl = "https://TEST-API.service.hmrc.gov.uk";

            // When
            boolean isSandbox = controller.isSandboxMode(mixedCaseUrl);

            // Then
            assertThat(isSandbox).isTrue();
        }
    }

    // ========================================================================
    // Sandbox Fallback Business ID Tests
    // ========================================================================

    @Nested
    @DisplayName("Sandbox Fallback Business ID")
    class SandboxFallbackBusinessId {

        @Test
        @DisplayName("should return sandbox test business ID constant")
        void shouldReturnSandboxTestBusinessIdConstant() {
            // When
            String fallbackId = controller.getSandboxFallbackBusinessId();

            // Then
            assertThat(fallbackId).isEqualTo("XAIS12345678901");
        }

        @Test
        @DisplayName("sandbox fallback business ID should match HMRC test format")
        void sandboxFallbackBusinessIdShouldMatchHmrcTestFormat() {
            // When
            String fallbackId = controller.getSandboxFallbackBusinessId();

            // Then - HMRC business ID format: X[A-Z0-9]{1}IS[0-9]{11}
            assertThat(fallbackId).matches("^X[A-Z0-9]{1}IS[0-9]{11}$");
        }
    }

    // ========================================================================
    // NINO Verification Status Tests (BUG FIX: Invalid NINO shows green ticks)
    // ========================================================================

    @Nested
    @DisplayName("NINO Verification Status")
    class NinoVerificationStatus {

        @Test
        @DisplayName("should return UNVERIFIED when NINO is using sandbox fallback")
        void shouldReturnUnverifiedWhenNinoUsingSandboxFallback() {
            // Given - NINO is saved but not verified by HMRC (404 response)
            controller.setNino("KP467718D");
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.SANDBOX_FALLBACK);

            // When
            SettingsController.NinoVerificationStatus status = controller.getNinoVerificationStatus();

            // Then - should indicate sandbox fallback (unverified)
            assertThat(status).isEqualTo(SettingsController.NinoVerificationStatus.SANDBOX_FALLBACK);
        }

        @Test
        @DisplayName("should return VERIFIED when NINO was confirmed by HMRC")
        void shouldReturnVerifiedWhenNinoConfirmedByHmrc() {
            // Given - NINO was actually verified by HMRC (200 response with business ID)
            controller.setNino("QQ123456A");
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.VERIFIED);

            // When
            SettingsController.NinoVerificationStatus status = controller.getNinoVerificationStatus();

            // Then
            assertThat(status).isEqualTo(SettingsController.NinoVerificationStatus.VERIFIED);
        }

        @Test
        @DisplayName("should return NOT_VERIFIED when NINO has not been checked")
        void shouldReturnNotVerifiedWhenNinoNotChecked() {
            // Given - NINO saved but never verified
            controller.setNino("QQ123456A");

            // When
            SettingsController.NinoVerificationStatus status = controller.getNinoVerificationStatus();

            // Then
            assertThat(status).isEqualTo(SettingsController.NinoVerificationStatus.NOT_VERIFIED);
        }

        @Test
        @DisplayName("should return FAILED when NINO verification returned error")
        void shouldReturnFailedWhenNinoVerificationFailed() {
            // Given - NINO verification returned 401/403 (mismatch)
            controller.setNino("QQ123456A");
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.FAILED);

            // When
            SettingsController.NinoVerificationStatus status = controller.getNinoVerificationStatus();

            // Then
            assertThat(status).isEqualTo(SettingsController.NinoVerificationStatus.FAILED);
        }

        @Test
        @DisplayName("isNinoVerified should return true only for VERIFIED status")
        void isNinoVerifiedShouldReturnTrueOnlyForVerifiedStatus() {
            // VERIFIED
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.VERIFIED);
            assertThat(controller.isNinoVerified()).isTrue();

            // SANDBOX_FALLBACK - not truly verified
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.SANDBOX_FALLBACK);
            assertThat(controller.isNinoVerified()).isFalse();

            // NOT_VERIFIED
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.NOT_VERIFIED);
            assertThat(controller.isNinoVerified()).isFalse();

            // FAILED
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.FAILED);
            assertThat(controller.isNinoVerified()).isFalse();
        }

        @Test
        @DisplayName("should provide descriptive message for sandbox fallback status")
        void shouldProvideDescriptiveMessageForSandboxFallbackStatus() {
            // Given
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.SANDBOX_FALLBACK);

            // When
            String message = controller.getNinoVerificationMessage();

            // Then - should indicate sandbox fallback clearly
            assertThat(message).containsIgnoringCase("sandbox");
            assertThat(message).containsIgnoringCase("not verified");
        }
    }

    // ========================================================================
    // NINO Change Detection Tests (Bug Fix: Changing NINO in sandbox mode)
    // ========================================================================

    @Nested
    @DisplayName("NINO Change Detection")
    class NinoChangeDetection {

        @Test
        @DisplayName("should detect when current NINO differs from connected NINO")
        void shouldDetectNinoChange() {
            // Given - connected with one NINO
            SqliteDataStore.getInstance().saveConnectedNino("QQ123456A");
            // User changes to a different NINO
            SqliteDataStore.getInstance().saveNino("AB654321D");

            // When
            boolean hasChanged = controller.hasNinoChangedSinceConnection();

            // Then
            assertThat(hasChanged).isTrue();
        }

        @Test
        @DisplayName("should not detect change when NINO is the same")
        void shouldNotDetectChangeWhenNinoIsSame() {
            // Given - connected and current NINO are the same
            SqliteDataStore.getInstance().saveConnectedNino("QQ123456A");
            SqliteDataStore.getInstance().saveNino("QQ123456A");

            // When
            boolean hasChanged = controller.hasNinoChangedSinceConnection();

            // Then
            assertThat(hasChanged).isFalse();
        }

        @Test
        @DisplayName("should not detect change when no connected NINO exists (first connection)")
        void shouldNotDetectChangeOnFirstConnection() {
            // Given - no connected NINO (first time connecting)
            SqliteDataStore.getInstance().saveNino("QQ123456A");
            // No saveConnectedNino called

            // When
            boolean hasChanged = controller.hasNinoChangedSinceConnection();

            // Then - first connection, no change to detect
            assertThat(hasChanged).isFalse();
        }

        @Test
        @DisplayName("should handle case-insensitive NINO comparison")
        void shouldHandleCaseInsensitiveNinoComparison() {
            // Given
            SqliteDataStore.getInstance().saveConnectedNino("qq123456a");
            SqliteDataStore.getInstance().saveNino("QQ123456A");

            // When
            boolean hasChanged = controller.hasNinoChangedSinceConnection();

            // Then - same NINO, different case
            assertThat(hasChanged).isFalse();
        }

        @Test
        @DisplayName("should return NINO_CHANGED status when NINO differs in sandbox")
        void shouldReturnNinoChangedStatusInSandbox() {
            // Given
            SqliteDataStore.getInstance().saveConnectedNino("QQ123456A");
            SqliteDataStore.getInstance().saveNino("AB654321D");
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.NINO_CHANGED);

            // When
            SettingsController.NinoVerificationStatus status = controller.getNinoVerificationStatus();

            // Then
            assertThat(status).isEqualTo(SettingsController.NinoVerificationStatus.NINO_CHANGED);
        }

        @Test
        @DisplayName("should provide descriptive message for NINO changed status")
        void shouldProvideDescriptiveMessageForNinoChanged() {
            // Given
            controller.setNinoVerificationStatus(SettingsController.NinoVerificationStatus.NINO_CHANGED);

            // When
            String message = controller.getNinoVerificationMessage();

            // Then
            assertThat(message).containsIgnoringCase("changed");
            assertThat(message).containsIgnoringCase("sandbox");
        }

        @Test
        @DisplayName("should not detect change after connected NINO is updated to new value")
        void shouldNotDetectChangeAfterConnectedNinoUpdated() {
            // Given - initially connected with one NINO
            SqliteDataStore.getInstance().saveConnectedNino("QQ123456A");
            // User changes to a new NINO
            SqliteDataStore.getInstance().saveNino("AB654321D");

            // Verify change is detected
            assertThat(controller.hasNinoChangedSinceConnection()).isTrue();

            // When - connected NINO is updated to the new value (simulating what happens
            // after user acknowledges the NINO change warning)
            SqliteDataStore.getInstance().saveConnectedNino("AB654321D");

            // Then - no change should be detected anymore
            assertThat(controller.hasNinoChangedSinceConnection()).isFalse();
        }

        @Test
        @DisplayName("should detect change again if user changes NINO after acknowledgment")
        void shouldDetectChangeAgainIfUserChangesNinoAfterAcknowledgment() {
            // Given - user initially connected with NINO_A
            SqliteDataStore.getInstance().saveConnectedNino("QQ123456A");
            SqliteDataStore.getInstance().saveNino("QQ123456A");
            assertThat(controller.hasNinoChangedSinceConnection()).isFalse();

            // User changes to NINO_B and reconnects
            SqliteDataStore.getInstance().saveNino("AB654321D");
            assertThat(controller.hasNinoChangedSinceConnection()).isTrue();

            // User acknowledges, connected NINO updated
            SqliteDataStore.getInstance().saveConnectedNino("AB654321D");
            assertThat(controller.hasNinoChangedSinceConnection()).isFalse();

            // When - user changes to NINO_C
            SqliteDataStore.getInstance().saveNino("CD789012B");

            // Then - change should be detected again
            assertThat(controller.hasNinoChangedSinceConnection()).isTrue();
        }
    }

    // ========================================================================
    // NINO Management Tests
    // ========================================================================

    @Nested
    @DisplayName("NINO Management")
    class NinoManagement {

        @Test
        @DisplayName("should return empty NINO initially")
        void shouldReturnEmptyNinoInitially() {
            assertThat(controller.getNino()).isEmpty();
        }

        @Test
        @DisplayName("should store NINO when set")
        void shouldStoreNino() {
            // When
            controller.setNino("QQ123456A");

            // Then
            assertThat(controller.getNino()).isEqualTo("QQ123456A");
        }

        @Test
        @DisplayName("should uppercase NINO when set")
        void shouldUppercaseNinoWhenSet() {
            // When
            controller.setNino("qq123456a");

            // Then
            assertThat(controller.getNino()).isEqualTo("QQ123456A");
        }

        @Test
        @DisplayName("should strip spaces from NINO when set")
        void shouldStripSpacesFromNinoWhenSet() {
            // When
            controller.setNino("QQ 12 34 56 A");

            // Then
            assertThat(controller.getNino()).isEqualTo("QQ123456A");
        }

        @Test
        @DisplayName("should handle null NINO by setting to empty string")
        void shouldHandleNullNino() {
            // Given
            controller.setNino("QQ123456A");

            // When
            controller.setNino(null);

            // Then
            assertThat(controller.getNino()).isEmpty();
        }

        @Test
        @DisplayName("should format NINO for display with spaces")
        void shouldFormatNinoForDisplayWithSpaces() {
            // Given
            controller.setNino("QQ123456A");

            // When
            String formatted = controller.getFormattedNino();

            // Then - NINO format: "QQ 12 34 56 A"
            assertThat(formatted).isEqualTo("QQ 12 34 56 A");
        }

        @Test
        @DisplayName("should return placeholder when no NINO set")
        void shouldReturnPlaceholderWhenNoNino() {
            // When
            String formatted = controller.getFormattedNino();

            // Then
            assertThat(formatted).isEqualTo("Not set");
        }
    }

    // ========================================================================
    // NINO Validation Tests
    // ========================================================================

    @Nested
    @DisplayName("NINO Validation")
    class NinoValidation {

        @Test
        @DisplayName("should validate valid NINO format")
        void shouldValidateValidNinoFormat() {
            // AB123456C is a valid NINO (A and B are allowed, C is valid suffix)
            assertThat(controller.isValidNino("AB123456C")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"AA123456A", "AB123456B", "AC123456C", "AE123456D"})
        @DisplayName("should validate various valid NINOs with different suffix letters")
        void shouldValidateVariousValidNinos(String nino) {
            assertThat(controller.isValidNino(nino)).isTrue();
        }

        @Test
        @DisplayName("should validate NINO with spaces")
        void shouldValidateNinoWithSpaces() {
            assertThat(controller.isValidNino("AB 12 34 56 A")).isTrue();
        }

        @Test
        @DisplayName("should validate lowercase NINO")
        void shouldValidateLowercaseNino() {
            assertThat(controller.isValidNino("ab123456a")).isTrue();
        }

        @Test
        @DisplayName("should reject empty NINO")
        void shouldRejectEmptyNino() {
            assertThat(controller.isValidNino("")).isFalse();
        }

        @Test
        @DisplayName("should reject null NINO")
        void shouldRejectNullNino() {
            assertThat(controller.isValidNino(null)).isFalse();
        }

        @Test
        @DisplayName("should reject NINO with invalid first letter D")
        void shouldRejectNinoWithInvalidFirstLetterD() {
            // D is an invalid first letter per HMRC rules
            assertThat(controller.isValidNino("DA123456A")).isFalse();
        }

        @Test
        @DisplayName("should reject NINO with invalid first letter Q")
        void shouldRejectNinoWithInvalidFirstLetterQ() {
            // Q is an invalid first letter per HMRC rules
            assertThat(controller.isValidNino("QA123456A")).isFalse();
        }

        @Test
        @DisplayName("should reject NINO with invalid suffix E")
        void shouldRejectNinoWithInvalidSuffixE() {
            // Only A, B, C, D are valid suffixes
            assertThat(controller.isValidNino("AB123456E")).isFalse();
        }

        @Test
        @DisplayName("should reject NINO with wrong length")
        void shouldRejectNinoWithWrongLength() {
            assertThat(controller.isValidNino("AB12345A")).isFalse();
            assertThat(controller.isValidNino("AB1234567A")).isFalse();
        }
    }

}
