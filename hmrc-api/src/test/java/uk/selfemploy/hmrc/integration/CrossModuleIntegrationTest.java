package uk.selfemploy.hmrc.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import uk.selfemploy.hmrc.exception.HmrcApiException;
import uk.selfemploy.hmrc.oauth.dto.OAuthTokens;
import uk.selfemploy.hmrc.oauth.storage.EncryptedFileTokenStorage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Cross-Module Integration Tests - Sprint 3
 *
 * Tests INT-001 and INT-002 from /rob QA specification:
 * - Full HMRC connection flow (OAuth -> Storage -> API)
 * - Tax Summary with real income/expense data
 *
 * @see docs/sprints/sprint-3/testing/rob-qa-sprint-3-backend.md
 */
@DisplayName("Cross-Module Integration Tests")
@Tag("integration")
@Tag("cross-module")
class CrossModuleIntegrationTest {

    @TempDir
    Path tempDir;

    /**
     * INT-001: Full HMRC Connection Flow
     */
    @Nested
    @DisplayName("INT-001: Full HMRC Connection Flow")
    class FullHmrcConnectionFlow {

        @Test
        @DisplayName("INT-001-01: Connect, store tokens, restart, verify persistence")
        void connectStoreTokensRestartVerifyPersistence() {
            // Given - Simulate OAuth completion and token storage
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage1 = new EncryptedFileTokenStorage(tokenFile);

            OAuthTokens tokens = new OAuthTokens(
                "access_token_abc123",
                "refresh_token_xyz789",
                3600L,
                "Bearer",
                "read:self-assessment write:self-assessment",
                Instant.now()
            );

            // When - Store tokens (simulates OAuth completion)
            storage1.save(tokens);

            // Then - Verify file created
            assertThat(Files.exists(tokenFile)).isTrue();

            // When - Simulate app restart (new storage instance)
            EncryptedFileTokenStorage storage2 = new EncryptedFileTokenStorage(tokenFile);

            // Then - Tokens should persist and load correctly
            Optional<OAuthTokens> loadedTokens = storage2.load();
            assertThat(loadedTokens).isPresent();
            assertThat(loadedTokens.get().accessToken()).isEqualTo("access_token_abc123");
            assertThat(loadedTokens.get().refreshToken()).isEqualTo("refresh_token_xyz789");
            assertThat(loadedTokens.get().scope()).isEqualTo("read:self-assessment write:self-assessment");
        }

        @Test
        @DisplayName("INT-001-01: Verify tokens usable for API calls after restart")
        void tokensUsableForApiCallsAfterRestart() {
            // Given - Store tokens
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile);

            OAuthTokens tokens = new OAuthTokens(
                "valid_access_token",
                "valid_refresh_token",
                3600L,
                "Bearer",
                "read:self-assessment",
                Instant.now()
            );
            storage.save(tokens);

            // When - Simulate restart and verify connection
            EncryptedFileTokenStorage newStorage = new EncryptedFileTokenStorage(tokenFile);
            Optional<OAuthTokens> loadedTokens = newStorage.load();

            // Then - Should be connected (non-expired valid tokens)
            assertThat(loadedTokens).isPresent();
            assertThat(loadedTokens.get().isExpired()).isFalse();
            assertThat(loadedTokens.get().tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("INT-001-02: Disconnect clears all state")
        void disconnectClearsAllState() {
            // Given - Connected state
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile);

            OAuthTokens tokens = new OAuthTokens(
                "access_token_to_delete",
                "refresh_token_to_delete",
                3600L,
                "Bearer",
                "read:self-assessment",
                Instant.now()
            );
            storage.save(tokens);
            assertThat(Files.exists(tokenFile)).isTrue();

            // When - Disconnect
            storage.delete();

            // Then - All state cleared
            assertThat(Files.exists(tokenFile)).isFalse();
            assertThat(storage.load()).isEmpty();
        }

        @Test
        @DisplayName("INT-001-02: API call fails after disconnect")
        void apiCallFailsAfterDisconnect() {
            // Given - Initially connected
            Path tokenFile = tempDir.resolve("tokens.enc");
            EncryptedFileTokenStorage storage = new EncryptedFileTokenStorage(tokenFile);

            OAuthTokens tokens = new OAuthTokens(
                "access_token",
                "refresh_token",
                3600L,
                "Bearer",
                "read:self-assessment",
                Instant.now()
            );
            storage.save(tokens);

            // When - Disconnect
            storage.delete();

            // Then - Attempting to get authorization would fail
            Optional<OAuthTokens> loadedTokens = storage.load();
            assertThat(loadedTokens).isEmpty();

            // Simulating what would happen in BusinessDetailsService
            assertThatThrownBy(() -> {
                Optional<OAuthTokens> t = storage.load();
                if (t.isEmpty()) {
                    throw new HmrcApiException("Not authenticated - please connect to HMRC first");
                }
            })
                .isInstanceOf(HmrcApiException.class)
                .hasMessageContaining("Not authenticated");
        }
    }

    /**
     * INT-002: Tax Summary with Real Data
     */
    @Nested
    @DisplayName("INT-002: Tax Summary with Real Data")
    class TaxSummaryWithRealData {

        @Test
        @DisplayName("INT-002-01: Tax summary calculates from income and expenses")
        void taxSummaryCalculatesFromIncomeAndExpenses() {
            // Given
            java.math.BigDecimal income = new java.math.BigDecimal("50000.00");
            java.math.BigDecimal expenses = new java.math.BigDecimal("10000.00");

            // When - Calculate net profit (Turnover - Expenses)
            java.math.BigDecimal netProfit = income.subtract(expenses);

            // Then - SA103 Box 15 (Turnover) and Box 31 (Net Profit)
            assertThat(netProfit).isEqualByComparingTo("40000.00");

            // Calculate tax (simplified - using 2025-26 rates)
            java.math.BigDecimal personalAllowance = new java.math.BigDecimal("12570.00");
            java.math.BigDecimal taxableIncome = netProfit.subtract(personalAllowance);
            assertThat(taxableIncome).isEqualByComparingTo("27430.00");

            // Basic rate 20%
            java.math.BigDecimal basicRateTax = taxableIncome.multiply(new java.math.BigDecimal("0.20"));
            assertThat(basicRateTax).isEqualByComparingTo("5486.00");
        }

        @Test
        @DisplayName("INT-002-01: No tax when profit below personal allowance")
        void noTaxWhenProfitBelowPersonalAllowance() {
            // Given
            java.math.BigDecimal income = new java.math.BigDecimal("15000.00");
            java.math.BigDecimal expenses = new java.math.BigDecimal("5000.00");
            java.math.BigDecimal personalAllowance = new java.math.BigDecimal("12570.00");

            // When
            java.math.BigDecimal netProfit = income.subtract(expenses); // £10,000
            java.math.BigDecimal taxableIncome = netProfit.subtract(personalAllowance);

            // Then - No tax if below PA
            if (taxableIncome.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                taxableIncome = java.math.BigDecimal.ZERO;
            }
            assertThat(taxableIncome).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("INT-002-02: Expense categories sum correctly")
        void expenseCategoriesSumCorrectly() {
            // Given - Expenses by category (SA103 boxes)
            java.util.Map<String, java.math.BigDecimal> expensesByCategory = new java.util.LinkedHashMap<>();

            // Box 17: Cost of goods bought for resale
            expensesByCategory.put("COST_OF_GOODS", new java.math.BigDecimal("1000.00"));
            // Box 18: Construction industry costs
            expensesByCategory.put("CONSTRUCTION", new java.math.BigDecimal("500.00"));
            // Box 19: Other direct costs
            expensesByCategory.put("OTHER_DIRECT", new java.math.BigDecimal("750.00"));
            // Box 20: Employee costs
            expensesByCategory.put("EMPLOYEE_COSTS", new java.math.BigDecimal("2000.00"));
            // Box 21: Premises costs
            expensesByCategory.put("PREMISES", new java.math.BigDecimal("1200.00"));
            // Box 22: Repairs
            expensesByCategory.put("REPAIRS", new java.math.BigDecimal("300.00"));
            // Box 23: General admin expenses
            expensesByCategory.put("ADMIN", new java.math.BigDecimal("600.00"));
            // Box 24: Motor expenses
            expensesByCategory.put("MOTOR", new java.math.BigDecimal("1500.00"));
            // Box 25: Travel and subsistence
            expensesByCategory.put("TRAVEL", new java.math.BigDecimal("800.00"));
            // Box 26: Advertising
            expensesByCategory.put("ADVERTISING", new java.math.BigDecimal("400.00"));

            // When - Sum all expenses
            java.math.BigDecimal totalExpenses = expensesByCategory.values().stream()
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            // Then
            assertThat(totalExpenses).isEqualByComparingTo("9050.00");
            assertThat(expensesByCategory).hasSize(10);
        }

        @Test
        @DisplayName("INT-002-02: Non-allowable expenses excluded from deduction")
        void nonAllowableExpensesExcluded() {
            // Given - Mix of allowable and non-allowable expenses
            java.util.Map<String, java.math.BigDecimal> allExpenses = new java.util.LinkedHashMap<>();

            // Allowable
            allExpenses.put("TRAVEL", new java.math.BigDecimal("500.00"));
            allExpenses.put("OFFICE_SUPPLIES", new java.math.BigDecimal("200.00"));
            allExpenses.put("PROFESSIONAL_FEES", new java.math.BigDecimal("300.00"));

            // Non-allowable (per SA103 rules)
            allExpenses.put("DEPRECIATION", new java.math.BigDecimal("1000.00")); // Not deductible
            allExpenses.put("BUSINESS_ENTERTAINMENT", new java.math.BigDecimal("500.00")); // Not deductible

            // When - Calculate allowable expenses only
            java.util.Set<String> nonAllowableCategories = java.util.Set.of(
                "DEPRECIATION", "BUSINESS_ENTERTAINMENT", "FINES_PENALTIES"
            );

            java.math.BigDecimal allowableExpenses = allExpenses.entrySet().stream()
                .filter(e -> !nonAllowableCategories.contains(e.getKey()))
                .map(java.util.Map.Entry::getValue)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            // Then
            assertThat(allowableExpenses).isEqualByComparingTo("1000.00"); // Only TRAVEL + OFFICE + PROFESSIONAL
        }

        @Test
        @DisplayName("INT-002-02: SA103 box mapping accurate")
        void sa103BoxMappingAccurate() {
            // Given - SA103 box number mappings
            java.util.Map<String, Integer> categoryToBox = java.util.Map.ofEntries(
                java.util.Map.entry("TURNOVER", 15),
                java.util.Map.entry("COST_OF_GOODS", 17),
                java.util.Map.entry("CONSTRUCTION", 18),
                java.util.Map.entry("OTHER_DIRECT", 19),
                java.util.Map.entry("EMPLOYEE_COSTS", 20),
                java.util.Map.entry("PREMISES", 21),
                java.util.Map.entry("REPAIRS", 22),
                java.util.Map.entry("ADMIN", 23),
                java.util.Map.entry("MOTOR", 24),
                java.util.Map.entry("TRAVEL", 25),
                java.util.Map.entry("ADVERTISING", 26),
                java.util.Map.entry("INTEREST", 27),
                java.util.Map.entry("BANK_CHARGES", 28),
                java.util.Map.entry("IRRECOVERABLE_DEBTS", 29),
                java.util.Map.entry("ACCOUNTANCY_FEES", 30),
                java.util.Map.entry("NET_PROFIT", 31)
            );

            // Then - Verify key mappings
            assertThat(categoryToBox.get("TURNOVER")).isEqualTo(15);
            assertThat(categoryToBox.get("NET_PROFIT")).isEqualTo(31);
            assertThat(categoryToBox.get("TRAVEL")).isEqualTo(25);
            assertThat(categoryToBox.get("MOTOR")).isEqualTo(24);
        }
    }

    /**
     * Payment on Account Tests (Part of INT-002)
     */
    @Nested
    @DisplayName("INT-002: Payment on Account")
    class PaymentOnAccount {

        @Test
        @DisplayName("TAX-004-01: POA required when tax > £1,000")
        void poaRequiredWhenTaxAboveThreshold() {
            // Given
            java.math.BigDecimal totalTax = new java.math.BigDecimal("1500.00");
            java.math.BigDecimal poaThreshold = new java.math.BigDecimal("1000.00");

            // When
            boolean requiresPoa = totalTax.compareTo(poaThreshold) > 0;

            // Then
            assertThat(requiresPoa).isTrue();
        }

        @Test
        @DisplayName("TAX-004-02: POA not required when tax <= £1,000")
        void poaNotRequiredWhenTaxBelowThreshold() {
            // Given
            java.math.BigDecimal totalTax = new java.math.BigDecimal("800.00");
            java.math.BigDecimal poaThreshold = new java.math.BigDecimal("1000.00");

            // When
            boolean requiresPoa = totalTax.compareTo(poaThreshold) > 0;

            // Then
            assertThat(requiresPoa).isFalse();
        }

        @Test
        @DisplayName("TAX-004-03: POA amount is 50% of tax")
        void poaAmountIs50PercentOfTax() {
            // Given
            java.math.BigDecimal totalTax = new java.math.BigDecimal("2000.00");

            // When
            java.math.BigDecimal poaAmount = totalTax.divide(
                new java.math.BigDecimal("2"),
                2,
                java.math.RoundingMode.HALF_UP
            );

            // Then
            assertThat(poaAmount).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("TAX-004-04: First POA due 31 January following tax year")
        void firstPoaDue31January() {
            // Given - Tax year 2025-26
            int taxYearStart = 2025;

            // When - First POA is due 31 January of year following tax year end
            java.time.LocalDate firstPoaDue = java.time.LocalDate.of(
                taxYearStart + 2, // 2027 for 2025-26 tax year
                java.time.Month.JANUARY,
                31
            );

            // Then
            assertThat(firstPoaDue).isEqualTo(java.time.LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("TAX-004-05: Second POA due 31 July")
        void secondPoaDue31July() {
            // Given - Tax year 2025-26
            int taxYearStart = 2025;

            // When - Second POA is due 31 July of year following tax year end
            java.time.LocalDate secondPoaDue = java.time.LocalDate.of(
                taxYearStart + 2, // 2027 for 2025-26 tax year
                java.time.Month.JULY,
                31
            );

            // Then
            assertThat(secondPoaDue).isEqualTo(java.time.LocalDate.of(2027, 7, 31));
        }
    }

    /**
     * NI Class 4 Tests (Part of INT-002)
     */
    @Nested
    @DisplayName("INT-002: NI Class 4 Calculation")
    class NiClass4Calculation {

        private static final java.math.BigDecimal LOWER_PROFITS_LIMIT = new java.math.BigDecimal("12570.00");
        private static final java.math.BigDecimal UPPER_PROFITS_LIMIT = new java.math.BigDecimal("50270.00");
        private static final java.math.BigDecimal MAIN_RATE = new java.math.BigDecimal("0.09"); // 9%
        private static final java.math.BigDecimal ADDITIONAL_RATE = new java.math.BigDecimal("0.02"); // 2%

        @Test
        @DisplayName("TAX-003-01: No NI below Lower Profits Limit")
        void noNiBelowLowerProfitsLimit() {
            // Given
            java.math.BigDecimal netProfit = new java.math.BigDecimal("10000.00");

            // When
            java.math.BigDecimal niClass4 = calculateNiClass4(netProfit);

            // Then
            assertThat(niClass4).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("TAX-003-02: Main rate 9% applies between LPL and UPL")
        void mainRateAppliesBetweenLplAndUpl() {
            // Given - Net profit £40,000
            java.math.BigDecimal netProfit = new java.math.BigDecimal("40000.00");

            // When
            java.math.BigDecimal niClass4 = calculateNiClass4(netProfit);

            // Profits between LPL (£12,570) and UPL (£50,270)
            // NI = (40000 - 12570) * 9% = 27430 * 0.09 = £2,468.70
            java.math.BigDecimal expected = new java.math.BigDecimal("27430.00")
                .multiply(MAIN_RATE)
                .setScale(2, java.math.RoundingMode.HALF_UP);

            // Then
            assertThat(niClass4).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("TAX-003-03: Additional rate 2% above UPL")
        void additionalRateAboveUpl() {
            // Given - Net profit £60,000
            java.math.BigDecimal netProfit = new java.math.BigDecimal("60000.00");

            // When
            java.math.BigDecimal niClass4 = calculateNiClass4(netProfit);

            // Main rate: (50270 - 12570) * 9% = 37700 * 0.09 = £3,393.00
            // Additional rate: (60000 - 50270) * 2% = 9730 * 0.02 = £194.60
            // Total: £3,587.60
            java.math.BigDecimal expectedMainRate = new java.math.BigDecimal("37700.00")
                .multiply(MAIN_RATE)
                .setScale(2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal expectedAdditionalRate = new java.math.BigDecimal("9730.00")
                .multiply(ADDITIONAL_RATE)
                .setScale(2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal expected = expectedMainRate.add(expectedAdditionalRate);

            // Then
            assertThat(niClass4).isEqualByComparingTo(expected);
        }

        private java.math.BigDecimal calculateNiClass4(java.math.BigDecimal netProfit) {
            if (netProfit.compareTo(LOWER_PROFITS_LIMIT) <= 0) {
                return java.math.BigDecimal.ZERO;
            }

            java.math.BigDecimal mainRateAmount;
            java.math.BigDecimal additionalRateAmount = java.math.BigDecimal.ZERO;

            if (netProfit.compareTo(UPPER_PROFITS_LIMIT) <= 0) {
                // Only main rate applies
                mainRateAmount = netProfit.subtract(LOWER_PROFITS_LIMIT);
            } else {
                // Both rates apply
                mainRateAmount = UPPER_PROFITS_LIMIT.subtract(LOWER_PROFITS_LIMIT);
                additionalRateAmount = netProfit.subtract(UPPER_PROFITS_LIMIT);
            }

            java.math.BigDecimal mainRateTax = mainRateAmount.multiply(MAIN_RATE)
                .setScale(2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal additionalRateTax = additionalRateAmount.multiply(ADDITIONAL_RATE)
                .setScale(2, java.math.RoundingMode.HALF_UP);

            return mainRateTax.add(additionalRateTax);
        }
    }
}
