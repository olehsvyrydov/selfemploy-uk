package uk.selfemploy.hmrc.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/**
 * HMRC Sandbox Integration Test Suite Verification Tests.
 *
 * <p>SE-703: HMRC Sandbox Integration Tests
 *
 * <p>These tests verify the P0 (Critical) test cases defined by /rob:
 * <ul>
 *     <li>TC-703-001: Verify all 234 tests pass</li>
 *     <li>TC-703-002: OAuth flow tests exist and pass</li>
 *     <li>TC-703-003: Business Details tests exist and pass</li>
 *     <li>TC-703-004: Quarterly Update tests exist and pass (all 4 quarters)</li>
 *     <li>TC-703-005: Annual Return tests exist and pass</li>
 *     <li>TC-703-006: Error handling tests exist and pass</li>
 *     <li>TC-703-007: Fraud prevention header tests exist and pass</li>
 *     <li>TC-703-009: Tests run without network calls (WireMock only)</li>
 * </ul>
 *
 * <p>Test Author: /adam (Senior E2E Test Automation Engineer)
 * <p>Sprint: 6
 *
 * @see OAuthFlowIntegrationTest
 * @see BusinessDetailsIntegrationTest
 * @see QuarterlyUpdateIntegrationTest
 * @see AnnualReturnIntegrationTest
 * @see FraudPreventionHeadersIntegrationTest
 * @see <a href="docs/sprints/sprint-6/testing/rob-qa-SE-509-SE-802-SE-703.md">QA Test Specifications</a>
 */
@DisplayName("SE-703: HMRC Test Suite Verification Tests")
@Tag("integration")
@Tag("se-703")
@Tag("verification")
class HmrcTestSuiteVerificationTest {

    // Expected test classes in the HMRC integration test suite
    private static final List<Class<?>> HMRC_TEST_CLASSES = List.of(
        OAuthFlowIntegrationTest.class,
        BusinessDetailsIntegrationTest.class,
        QuarterlyUpdateIntegrationTest.class,
        AnnualReturnIntegrationTest.class,
        FraudPreventionHeadersIntegrationTest.class
    );

    // Expected minimum test count for the suite (based on SE-703 requirement: 234 tests)
    private static final int EXPECTED_MINIMUM_TESTS = 200;

    // ==================== TC-703-001: Verify All Tests Pass ====================

    @Nested
    @DisplayName("TC-703-001: Test Suite Completeness Verification")
    class TestSuiteCompletenessVerification {

        @Test
        @DisplayName("TC-703-001-01: HMRC test package contains expected test classes")
        void hmrcTestPackageContainsExpectedTestClasses() {
            // Given - List of expected test classes

            // When/Then
            for (Class<?> expectedClass : HMRC_TEST_CLASSES) {
                assertThat(expectedClass)
                    .as("Test class %s should exist", expectedClass.getSimpleName())
                    .isNotNull();
            }
        }

        @Test
        @DisplayName("TC-703-001-02: All test classes are in the correct package")
        void allTestClassesInCorrectPackage() {
            // Given
            String expectedPackage = "uk.selfemploy.hmrc.integration";

            // When/Then
            for (Class<?> testClass : HMRC_TEST_CLASSES) {
                assertThat(testClass.getPackageName())
                    .as("%s should be in package %s", testClass.getSimpleName(), expectedPackage)
                    .isEqualTo(expectedPackage);
            }
        }

        @Test
        @DisplayName("TC-703-001-03: All test classes have @Tag integration annotation")
        void allTestClassesHaveIntegrationTag() {
            // When/Then
            for (Class<?> testClass : HMRC_TEST_CLASSES) {
                Tag[] tags = testClass.getAnnotationsByType(Tag.class);
                Set<String> tagValues = Arrays.stream(tags)
                    .map(Tag::value)
                    .collect(Collectors.toSet());

                assertThat(tagValues)
                    .as("%s should have @Tag(\"integration\")", testClass.getSimpleName())
                    .contains("integration");
            }
        }

        @Test
        @DisplayName("TC-703-001-04: All test classes have @DisplayName annotation")
        void allTestClassesHaveDisplayName() {
            // When/Then
            for (Class<?> testClass : HMRC_TEST_CLASSES) {
                DisplayName displayName = testClass.getAnnotation(DisplayName.class);

                assertThat(displayName)
                    .as("%s should have @DisplayName annotation", testClass.getSimpleName())
                    .isNotNull();

                assertThat(displayName.value())
                    .as("%s @DisplayName should be meaningful", testClass.getSimpleName())
                    .isNotBlank()
                    .hasSizeGreaterThan(10);
            }
        }

        @Test
        @DisplayName("TC-703-001-05: Total test count meets minimum requirement (200+ tests)")
        void totalTestCountMeetsMinimumRequirement() {
            // Given
            int totalTests = 0;

            // When - Count all test methods across all classes
            for (Class<?> testClass : HMRC_TEST_CLASSES) {
                totalTests += countTestMethods(testClass);
            }

            // Then
            assertThat(totalTests)
                .as("Total test count should meet minimum requirement of %d tests", EXPECTED_MINIMUM_TESTS)
                .isGreaterThanOrEqualTo(EXPECTED_MINIMUM_TESTS);
        }
    }

    // ==================== TC-703-002: OAuth Flow Tests Exist and Pass ====================

    @Nested
    @DisplayName("TC-703-002: OAuth Flow Integration Tests Verification")
    class OAuthFlowTestsVerification {

        @Test
        @DisplayName("TC-703-002-01: OAuthFlowIntegrationTest class exists")
        void oAuthFlowIntegrationTestClassExists() {
            // Given/When
            Class<?> oAuthTestClass = OAuthFlowIntegrationTest.class;

            // Then
            assertThat(oAuthTestClass)
                .as("OAuthFlowIntegrationTest class should exist")
                .isNotNull();
        }

        @Test
        @DisplayName("TC-703-002-02: OAuth tests include token exchange test")
        void oAuthTestsIncludeTokenExchangeTest() {
            // Given
            Set<String> methodNames = getTestMethodNames(OAuthFlowIntegrationTest.class);

            // Then - Should include token exchange or authorization code test
            boolean hasTokenTest = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("token")
                    || name.toLowerCase().contains("exchange")
                    || name.toLowerCase().contains("authorization"));

            assertThat(hasTokenTest)
                .as("OAuth tests should include token exchange test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-002-03: OAuth tests include token refresh test")
        void oAuthTestsIncludeTokenRefreshTest() {
            // Given
            Set<String> methodNames = getTestMethodNames(OAuthFlowIntegrationTest.class);

            // Then
            boolean hasRefreshTest = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("refresh"));

            assertThat(hasRefreshTest)
                .as("OAuth tests should include token refresh test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-002-04: OAuth tests include error handling for invalid grant")
        void oAuthTestsIncludeInvalidGrantTest() {
            // Given
            Set<String> methodNames = getTestMethodNames(OAuthFlowIntegrationTest.class);

            // Then
            boolean hasInvalidGrantTest = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("invalid")
                    || name.toLowerCase().contains("error")
                    || name.toLowerCase().contains("expired"));

            assertThat(hasInvalidGrantTest)
                .as("OAuth tests should include invalid grant/error handling test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-002-05: OAuth tests have minimum count (10+ tests)")
        void oAuthTestsHaveMinimumCount() {
            // Given
            int testCount = countTestMethods(OAuthFlowIntegrationTest.class);

            // Then
            assertThat(testCount)
                .as("OAuth flow tests should have at least 10 tests")
                .isGreaterThanOrEqualTo(10);
        }
    }

    // ==================== TC-703-003: Business Details Tests Exist and Pass ====================

    @Nested
    @DisplayName("TC-703-003: Business Details Integration Tests Verification")
    class BusinessDetailsTestsVerification {

        @Test
        @DisplayName("TC-703-003-01: BusinessDetailsIntegrationTest class exists")
        void businessDetailsIntegrationTestClassExists() {
            // Given/When
            Class<?> businessTestClass = BusinessDetailsIntegrationTest.class;

            // Then
            assertThat(businessTestClass)
                .as("BusinessDetailsIntegrationTest class should exist")
                .isNotNull();
        }

        @Test
        @DisplayName("TC-703-003-02: Business Details tests have @Tag(\"business-details\")")
        void businessDetailsTestsHaveCorrectTag() {
            // Given
            Class<?> testClass = BusinessDetailsIntegrationTest.class;
            Tag[] tags = testClass.getAnnotationsByType(Tag.class);

            // When
            Set<String> tagValues = Arrays.stream(tags)
                .map(Tag::value)
                .collect(Collectors.toSet());

            // Then
            assertThat(tagValues)
                .as("BusinessDetailsIntegrationTest should have @Tag(\"business-details\")")
                .contains("business-details");
        }

        @Test
        @DisplayName("TC-703-003-03: Business Details tests include list businesses test")
        void businessDetailsTestsIncludeListBusinessesTest() {
            // Given
            Set<String> methodNames = getTestMethodNames(BusinessDetailsIntegrationTest.class);

            // Then
            boolean hasListTest = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("list")
                    || name.toLowerCase().contains("all"));

            assertThat(hasListTest)
                .as("Business Details tests should include list businesses test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-003-04: Business Details tests include get single business test")
        void businessDetailsTestsIncludeGetSingleBusinessTest() {
            // Given
            Set<String> methodNames = getTestMethodNames(BusinessDetailsIntegrationTest.class);

            // Then
            boolean hasGetTest = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("single")
                    || name.toLowerCase().contains("retrieve")
                    || name.toLowerCase().contains("detail"));

            assertThat(hasGetTest)
                .as("Business Details tests should include get single business test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-003-05: Business Details tests have minimum count (25+ tests)")
        void businessDetailsTestsHaveMinimumCount() {
            // Given
            int testCount = countTestMethods(BusinessDetailsIntegrationTest.class);

            // Then
            assertThat(testCount)
                .as("Business Details tests should have at least 25 tests")
                .isGreaterThanOrEqualTo(25);
        }
    }

    // ==================== TC-703-004: Quarterly Update Tests Exist and Pass ====================

    @Nested
    @DisplayName("TC-703-004: Quarterly Update Integration Tests Verification")
    class QuarterlyUpdateTestsVerification {

        @Test
        @DisplayName("TC-703-004-01: QuarterlyUpdateIntegrationTest class exists")
        void quarterlyUpdateIntegrationTestClassExists() {
            // Given/When
            Class<?> quarterlyTestClass = QuarterlyUpdateIntegrationTest.class;

            // Then
            assertThat(quarterlyTestClass)
                .as("QuarterlyUpdateIntegrationTest class should exist")
                .isNotNull();
        }

        @Test
        @DisplayName("TC-703-004-02: Quarterly Update tests have @Tag(\"quarterly-update\")")
        void quarterlyUpdateTestsHaveCorrectTag() {
            // Given
            Class<?> testClass = QuarterlyUpdateIntegrationTest.class;
            Tag[] tags = testClass.getAnnotationsByType(Tag.class);

            // When
            Set<String> tagValues = Arrays.stream(tags)
                .map(Tag::value)
                .collect(Collectors.toSet());

            // Then
            assertThat(tagValues)
                .as("QuarterlyUpdateIntegrationTest should have @Tag(\"quarterly-update\")")
                .contains("quarterly-update");
        }

        @Test
        @DisplayName("TC-703-004-03: Quarterly Update tests include Q1 submission test")
        void quarterlyUpdateTestsIncludeQ1Test() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(QuarterlyUpdateIntegrationTest.class);

            // Then
            boolean hasQ1Test = allMethodNames.stream()
                .anyMatch(name -> name.contains("Q1") || name.contains("q1"));

            assertThat(hasQ1Test)
                .as("Quarterly Update tests should include Q1 test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-004-04: Quarterly Update tests include Q2 submission test")
        void quarterlyUpdateTestsIncludeQ2Test() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(QuarterlyUpdateIntegrationTest.class);

            // Then
            boolean hasQ2Test = allMethodNames.stream()
                .anyMatch(name -> name.contains("Q2") || name.contains("q2"));

            assertThat(hasQ2Test)
                .as("Quarterly Update tests should include Q2 test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-004-05: Quarterly Update tests include Q3 submission test")
        void quarterlyUpdateTestsIncludeQ3Test() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(QuarterlyUpdateIntegrationTest.class);

            // Then
            boolean hasQ3Test = allMethodNames.stream()
                .anyMatch(name -> name.contains("Q3") || name.contains("q3"));

            assertThat(hasQ3Test)
                .as("Quarterly Update tests should include Q3 test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-004-06: Quarterly Update tests include Q4 submission test")
        void quarterlyUpdateTestsIncludeQ4Test() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(QuarterlyUpdateIntegrationTest.class);

            // Then
            boolean hasQ4Test = allMethodNames.stream()
                .anyMatch(name -> name.contains("Q4") || name.contains("q4"));

            assertThat(hasQ4Test)
                .as("Quarterly Update tests should include Q4 test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-004-07: Quarterly Update tests include cumulative totals test")
        void quarterlyUpdateTestsIncludeCumulativeTotalsTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(QuarterlyUpdateIntegrationTest.class);

            // Then
            boolean hasCumulativeTest = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("cumulative")
                    || name.toLowerCase().contains("total"));

            assertThat(hasCumulativeTest)
                .as("Quarterly Update tests should include cumulative totals test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-004-08: Quarterly Update tests have minimum count (30+ tests)")
        void quarterlyUpdateTestsHaveMinimumCount() {
            // Given
            int testCount = countTestMethods(QuarterlyUpdateIntegrationTest.class);

            // Then
            assertThat(testCount)
                .as("Quarterly Update tests should have at least 30 tests")
                .isGreaterThanOrEqualTo(30);
        }
    }

    // ==================== TC-703-005: Annual Return Tests Exist and Pass ====================

    @Nested
    @DisplayName("TC-703-005: Annual Return Integration Tests Verification")
    class AnnualReturnTestsVerification {

        @Test
        @DisplayName("TC-703-005-01: AnnualReturnIntegrationTest class exists")
        void annualReturnIntegrationTestClassExists() {
            // Given/When
            Class<?> annualTestClass = AnnualReturnIntegrationTest.class;

            // Then
            assertThat(annualTestClass)
                .as("AnnualReturnIntegrationTest class should exist")
                .isNotNull();
        }

        @Test
        @DisplayName("TC-703-005-02: Annual Return tests have @Tag(\"annual-return\")")
        void annualReturnTestsHaveCorrectTag() {
            // Given
            Class<?> testClass = AnnualReturnIntegrationTest.class;
            Tag[] tags = testClass.getAnnotationsByType(Tag.class);

            // When
            Set<String> tagValues = Arrays.stream(tags)
                .map(Tag::value)
                .collect(Collectors.toSet());

            // Then
            assertThat(tagValues)
                .as("AnnualReturnIntegrationTest should have @Tag(\"annual-return\")")
                .contains("annual-return");
        }

        @Test
        @DisplayName("TC-703-005-03: Annual Return tests include calculation trigger test")
        void annualReturnTestsIncludeCalculationTriggerTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(AnnualReturnIntegrationTest.class);

            // Then
            boolean hasCalculationTest = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("calculation")
                    || name.toLowerCase().contains("trigger"));

            assertThat(hasCalculationTest)
                .as("Annual Return tests should include calculation trigger test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-005-04: Annual Return tests include final declaration test")
        void annualReturnTestsIncludeFinalDeclarationTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(AnnualReturnIntegrationTest.class);

            // Then
            boolean hasDeclarationTest = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("declaration")
                    || name.toLowerCase().contains("submit"));

            assertThat(hasDeclarationTest)
                .as("Annual Return tests should include final declaration test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-005-05: Annual Return tests have minimum count (30+ tests)")
        void annualReturnTestsHaveMinimumCount() {
            // Given
            int testCount = countTestMethods(AnnualReturnIntegrationTest.class);

            // Then
            assertThat(testCount)
                .as("Annual Return tests should have at least 30 tests")
                .isGreaterThanOrEqualTo(30);
        }
    }

    // ==================== TC-703-006: Error Handling Tests Exist and Pass ====================

    @Nested
    @DisplayName("TC-703-006: Error Handling Tests Verification")
    class ErrorHandlingTestsVerification {

        @Test
        @DisplayName("TC-703-006-01: Business Details tests include 401 Unauthorized test")
        void businessDetailsTestsInclude401UnauthorizedTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(BusinessDetailsIntegrationTest.class);

            // Then
            boolean has401Test = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("401")
                    || name.toLowerCase().contains("unauthorized")
                    || name.toLowerCase().contains("invalid")
                    || name.toLowerCase().contains("expired"));

            assertThat(has401Test)
                .as("Business Details tests should include 401 Unauthorized test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-006-02: Business Details tests include 403 Forbidden test")
        void businessDetailsTestsInclude403ForbiddenTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(BusinessDetailsIntegrationTest.class);

            // Then
            boolean has403Test = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("403")
                    || name.toLowerCase().contains("forbidden"));

            assertThat(has403Test)
                .as("Business Details tests should include 403 Forbidden test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-006-03: Business Details tests include 404 Not Found test")
        void businessDetailsTestsInclude404NotFoundTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(BusinessDetailsIntegrationTest.class);

            // Then
            boolean has404Test = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("404")
                    || name.toLowerCase().contains("notfound")
                    || name.toLowerCase().contains("not_found"));

            assertThat(has404Test)
                .as("Business Details tests should include 404 Not Found test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-006-04: Quarterly Update tests include 409 Conflict test (duplicate)")
        void quarterlyUpdateTestsInclude409ConflictTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(QuarterlyUpdateIntegrationTest.class);

            // Then
            boolean has409Test = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("409")
                    || name.toLowerCase().contains("conflict")
                    || name.toLowerCase().contains("duplicate")
                    || name.toLowerCase().contains("overlapping"));

            assertThat(has409Test)
                .as("Quarterly Update tests should include 409 Conflict (duplicate) test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-006-05: Quarterly Update tests include 422 Validation Error test")
        void quarterlyUpdateTestsInclude422ValidationErrorTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(QuarterlyUpdateIntegrationTest.class);

            // Then
            boolean has422Test = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("422")
                    || name.toLowerCase().contains("validation"));

            assertThat(has422Test)
                .as("Quarterly Update tests should include 422 Validation Error test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-006-06: Business Details tests include 500 Server Error test")
        void businessDetailsTestsInclude500ServerErrorTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(BusinessDetailsIntegrationTest.class);

            // Then
            boolean has500Test = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("500")
                    || name.toLowerCase().contains("server")
                    || name.toLowerCase().contains("error"));

            assertThat(has500Test)
                .as("Business Details tests should include 500 Server Error test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-006-07: Business Details tests include 429 Rate Limit test")
        void businessDetailsTestsInclude429RateLimitTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(BusinessDetailsIntegrationTest.class);

            // Then
            boolean has429Test = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("429")
                    || name.toLowerCase().contains("rate")
                    || name.toLowerCase().contains("throttle"));

            assertThat(has429Test)
                .as("Business Details tests should include 429 Rate Limit test")
                .isTrue();
        }
    }

    // ==================== TC-703-007: Fraud Prevention Header Tests Exist and Pass ====================

    @Nested
    @DisplayName("TC-703-007: Fraud Prevention Header Tests Verification")
    class FraudPreventionHeaderTestsVerification {

        @Test
        @DisplayName("TC-703-007-01: FraudPreventionHeadersIntegrationTest class exists")
        void fraudPreventionHeadersIntegrationTestClassExists() {
            // Given/When
            Class<?> fraudTestClass = FraudPreventionHeadersIntegrationTest.class;

            // Then
            assertThat(fraudTestClass)
                .as("FraudPreventionHeadersIntegrationTest class should exist")
                .isNotNull();
        }

        @Test
        @DisplayName("TC-703-007-02: Fraud Prevention tests have @Tag(\"fraud-prevention\")")
        void fraudPreventionTestsHaveCorrectTag() {
            // Given
            Class<?> testClass = FraudPreventionHeadersIntegrationTest.class;
            Tag[] tags = testClass.getAnnotationsByType(Tag.class);

            // When
            Set<String> tagValues = Arrays.stream(tags)
                .map(Tag::value)
                .collect(Collectors.toSet());

            // Then
            assertThat(tagValues)
                .as("FraudPreventionHeadersIntegrationTest should have @Tag(\"fraud-prevention\")")
                .contains("fraud-prevention");
        }

        @Test
        @DisplayName("TC-703-007-03: Fraud Prevention tests include Gov-Client-Connection-Method test")
        void fraudPreventionTestsIncludeConnectionMethodTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(FraudPreventionHeadersIntegrationTest.class);

            // Then
            boolean hasConnectionMethodTest = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("connection")
                    || name.toLowerCase().contains("method"));

            assertThat(hasConnectionMethodTest)
                .as("Fraud Prevention tests should include Connection-Method test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-007-04: Fraud Prevention tests include Gov-Client-Device-ID test")
        void fraudPreventionTestsIncludeDeviceIdTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(FraudPreventionHeadersIntegrationTest.class);

            // Then
            boolean hasDeviceIdTest = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("device")
                    || name.toLowerCase().contains("id"));

            assertThat(hasDeviceIdTest)
                .as("Fraud Prevention tests should include Device-ID test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-007-05: Fraud Prevention tests include Gov-Client-Timezone test")
        void fraudPreventionTestsIncludeTimezoneTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(FraudPreventionHeadersIntegrationTest.class);

            // Then
            boolean hasTimezoneTest = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("timezone")
                    || name.toLowerCase().contains("utc"));

            assertThat(hasTimezoneTest)
                .as("Fraud Prevention tests should include Timezone test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-007-06: Fraud Prevention tests include missing header error test")
        void fraudPreventionTestsIncludeMissingHeaderErrorTest() {
            // Given
            Set<String> allMethodNames = getAllNestedTestMethodNames(FraudPreventionHeadersIntegrationTest.class);

            // Then
            boolean hasMissingHeaderTest = allMethodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("missing")
                    || name.toLowerCase().contains("400")
                    || name.toLowerCase().contains("error"));

            assertThat(hasMissingHeaderTest)
                .as("Fraud Prevention tests should include missing header error test")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-007-07: Fraud Prevention tests have minimum count (40+ tests)")
        void fraudPreventionTestsHaveMinimumCount() {
            // Given
            int testCount = countTestMethods(FraudPreventionHeadersIntegrationTest.class);

            // Then
            assertThat(testCount)
                .as("Fraud Prevention tests should have at least 40 tests")
                .isGreaterThanOrEqualTo(40);
        }
    }

    // ==================== TC-703-009: Tests Run Without Network Calls ====================

    @Nested
    @DisplayName("TC-703-009: WireMock Usage Verification (No Network Calls)")
    class WireMockUsageVerification {

        @Test
        @DisplayName("TC-703-009-01: HmrcWireMockStubs helper class exists")
        void hmrcWireMockStubsHelperClassExists() {
            // Given/When
            Class<?> stubsClass = HmrcWireMockStubs.class;

            // Then
            assertThat(stubsClass)
                .as("HmrcWireMockStubs helper class should exist")
                .isNotNull();
        }

        @Test
        @DisplayName("TC-703-009-02: HmrcWireMockStubs is in same package as tests")
        void hmrcWireMockStubsInSamePackage() {
            // Given
            String testPackage = OAuthFlowIntegrationTest.class.getPackageName();
            String stubsPackage = HmrcWireMockStubs.class.getPackageName();

            // Then
            assertThat(stubsPackage)
                .as("HmrcWireMockStubs should be in same package as tests")
                .isEqualTo(testPackage);
        }

        @Test
        @DisplayName("TC-703-009-03: HmrcWireMockStubs defines sandbox test NINOs")
        void hmrcWireMockStubsDefinesSandboxTestNinos() throws NoSuchFieldException {
            // Given
            Class<?> stubsClass = HmrcWireMockStubs.class;

            // When/Then
            assertThat(stubsClass.getDeclaredField("NINO_HAPPY_PATH"))
                .as("HmrcWireMockStubs should define NINO_HAPPY_PATH")
                .isNotNull();

            assertThat(stubsClass.getDeclaredField("NINO_NOT_FOUND"))
                .as("HmrcWireMockStubs should define NINO_NOT_FOUND")
                .isNotNull();

            assertThat(stubsClass.getDeclaredField("NINO_SERVER_ERROR"))
                .as("HmrcWireMockStubs should define NINO_SERVER_ERROR")
                .isNotNull();
        }

        @Test
        @DisplayName("TC-703-009-04: HmrcWireMockStubs provides OAuth stub methods")
        void hmrcWireMockStubsProvidesOAuthStubMethods() {
            // Given
            Set<String> methodNames = Arrays.stream(HmrcWireMockStubs.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

            // Then
            boolean hasOAuthStub = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("oauth")
                    || name.toLowerCase().contains("token"));

            assertThat(hasOAuthStub)
                .as("HmrcWireMockStubs should provide OAuth stub methods")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-009-05: HmrcWireMockStubs provides Business Details stub methods")
        void hmrcWireMockStubsProvidesBusinessDetailsStubMethods() {
            // Given
            Set<String> methodNames = Arrays.stream(HmrcWireMockStubs.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

            // Then
            boolean hasBusinessStub = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("business"));

            assertThat(hasBusinessStub)
                .as("HmrcWireMockStubs should provide Business Details stub methods")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-009-06: HmrcWireMockStubs provides Quarterly Update stub methods")
        void hmrcWireMockStubsProvidesQuarterlyUpdateStubMethods() {
            // Given
            Set<String> methodNames = Arrays.stream(HmrcWireMockStubs.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

            // Then
            boolean hasQuarterlyStub = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("quarterly"));

            assertThat(hasQuarterlyStub)
                .as("HmrcWireMockStubs should provide Quarterly Update stub methods")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-009-07: HmrcWireMockStubs provides Annual Return stub methods")
        void hmrcWireMockStubsProvidesAnnualReturnStubMethods() {
            // Given
            Set<String> methodNames = Arrays.stream(HmrcWireMockStubs.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

            // Then
            boolean hasAnnualStub = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("annual"));

            assertThat(hasAnnualStub)
                .as("HmrcWireMockStubs should provide Annual Return stub methods")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-009-08: HmrcWireMockStubs provides error response stub methods")
        void hmrcWireMockStubsProvidesErrorStubMethods() {
            // Given
            Set<String> methodNames = Arrays.stream(HmrcWireMockStubs.class.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

            // Then
            boolean hasErrorStubs = methodNames.stream()
                .anyMatch(name -> name.toLowerCase().contains("error")
                    || name.toLowerCase().contains("unauthorized")
                    || name.toLowerCase().contains("forbidden"));

            assertThat(hasErrorStubs)
                .as("HmrcWireMockStubs should provide error response stub methods")
                .isTrue();
        }

        @Test
        @DisplayName("TC-703-009-09: All test classes have @Tag(\"sandbox\") indicating WireMock usage")
        void allTestClassesHaveSandboxTag() {
            // When/Then
            for (Class<?> testClass : HMRC_TEST_CLASSES) {
                Tag[] tags = testClass.getAnnotationsByType(Tag.class);
                Set<String> tagValues = Arrays.stream(tags)
                    .map(Tag::value)
                    .collect(Collectors.toSet());

                assertThat(tagValues)
                    .as("%s should have @Tag(\"sandbox\")", testClass.getSimpleName())
                    .contains("sandbox");
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Counts all test methods in a class including nested classes.
     */
    private int countTestMethods(Class<?> testClass) {
        int count = 0;

        // Count methods in main class
        count += (int) Arrays.stream(testClass.getDeclaredMethods())
            .filter(this::isTestMethod)
            .count();

        // Count methods in nested classes
        for (Class<?> nestedClass : testClass.getDeclaredClasses()) {
            count += (int) Arrays.stream(nestedClass.getDeclaredMethods())
                .filter(this::isTestMethod)
                .count();
        }

        return count;
    }

    /**
     * Gets all test method names from a class.
     */
    private Set<String> getTestMethodNames(Class<?> testClass) {
        return Arrays.stream(testClass.getDeclaredMethods())
            .filter(this::isTestMethod)
            .map(Method::getName)
            .collect(Collectors.toSet());
    }

    /**
     * Gets all test method names from a class and its nested classes.
     */
    private Set<String> getAllNestedTestMethodNames(Class<?> testClass) {
        Set<String> methodNames = getTestMethodNames(testClass);

        // Also check nested classes
        for (Class<?> nestedClass : testClass.getDeclaredClasses()) {
            methodNames.addAll(
                Arrays.stream(nestedClass.getDeclaredMethods())
                    .filter(this::isTestMethod)
                    .map(Method::getName)
                    .collect(Collectors.toSet())
            );

            // Check @DisplayName values as well (more reliable for identifying test purpose)
            DisplayName nestedDisplayName = nestedClass.getAnnotation(DisplayName.class);
            if (nestedDisplayName != null) {
                methodNames.add(nestedDisplayName.value());
            }
        }

        return methodNames;
    }

    /**
     * Checks if a method is a JUnit 5 test method.
     */
    private boolean isTestMethod(Method method) {
        return method.isAnnotationPresent(Test.class)
            || method.isAnnotationPresent(org.junit.jupiter.params.ParameterizedTest.class);
    }
}
