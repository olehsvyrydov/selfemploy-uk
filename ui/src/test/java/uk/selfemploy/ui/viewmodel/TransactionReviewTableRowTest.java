package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TransactionReviewTableRow display formatting and filter matching.
 */
class TransactionReviewTableRowTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID IMPORT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Nested
    class FromDomain {

        @Test
        void shouldMapAllFieldsFromBankTransaction() {
            BankTransaction tx = new BankTransaction(
                UUID.randomUUID(), BUSINESS_ID, IMPORT_ID, "csv-barclays",
                LocalDate.of(2025, 6, 15), new BigDecimal("250.00"),
                "Client payment", "1234", null, "hash1",
                ReviewStatus.PENDING, null, null, null,
                true, new BigDecimal("0.9"), ExpenseCategory.PROFESSIONAL_FEES,
                NOW, NOW, null, null, null
            );

            TransactionReviewTableRow row = TransactionReviewTableRow.fromDomain(tx);

            assertThat(row.id()).isEqualTo(tx.id());
            assertThat(row.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(row.description()).isEqualTo("Client payment");
            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
            assertThat(row.isIncome()).isTrue();
            assertThat(row.reviewStatus()).isEqualTo(ReviewStatus.PENDING);
            assertThat(row.isBusiness()).isTrue();
            assertThat(row.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.9"));
            assertThat(row.suggestedCategory()).isEqualTo(ExpenseCategory.PROFESSIONAL_FEES);
        }

        @Test
        void shouldDetectExpense_whenAmountNegative() {
            TransactionReviewTableRow row = createRow(new BigDecimal("-50.00"));
            assertThat(row.isIncome()).isFalse();
        }

        @Test
        void shouldDetectIncome_whenAmountPositive() {
            TransactionReviewTableRow row = createRow(new BigDecimal("100.00"));
            assertThat(row.isIncome()).isTrue();
        }
    }

    @Nested
    class Formatting {

        @Test
        void getFormattedDate_shouldFormatUkStyle() {
            TransactionReviewTableRow row = createRowWithDate(LocalDate.of(2025, 1, 10));
            assertThat(row.getFormattedDate()).isEqualTo("10 Jan '25");
        }

        @Test
        void getFormattedAmount_shouldFormatAbsoluteGbp() {
            TransactionReviewTableRow row = createRow(new BigDecimal("-45.50"));
            assertThat(row.getFormattedAmount()).isEqualTo("£45.50");
        }

        @Test
        void getSignedFormattedAmount_shouldShowPlusForIncome() {
            TransactionReviewTableRow row = createRow(new BigDecimal("100.00"));
            assertThat(row.getSignedFormattedAmount()).startsWith("+£");
        }

        @Test
        void getSignedFormattedAmount_shouldShowMinusForExpense() {
            TransactionReviewTableRow row = createRow(new BigDecimal("-100.00"));
            assertThat(row.getSignedFormattedAmount()).startsWith("-£");
        }
    }

    @Nested
    class ConfidenceLabels {

        @Test
        void shouldReturnHigh_whenScoreAbove90Percent() {
            TransactionReviewTableRow row = createRowWithConfidence(new BigDecimal("0.95"));
            assertThat(row.getConfidenceLabel()).isEqualTo("HIGH");
            assertThat(row.getConfidenceStyleClass()).isEqualTo("confidence-high");
        }

        @Test
        void shouldReturnMedium_whenScoreBetween60And90() {
            TransactionReviewTableRow row = createRowWithConfidence(new BigDecimal("0.75"));
            assertThat(row.getConfidenceLabel()).isEqualTo("MEDIUM");
            assertThat(row.getConfidenceStyleClass()).isEqualTo("confidence-medium");
        }

        @Test
        void shouldReturnLow_whenScoreBelow60() {
            TransactionReviewTableRow row = createRowWithConfidence(new BigDecimal("0.55"));
            assertThat(row.getConfidenceLabel()).isEqualTo("LOW");
            assertThat(row.getConfidenceStyleClass()).isEqualTo("confidence-low");
        }

        @Test
        void shouldReturnDash_whenNoScore() {
            TransactionReviewTableRow row = createRowWithConfidence(null);
            assertThat(row.getConfidenceLabel()).isEqualTo("\u2014");
            assertThat(row.getConfidenceStyleClass()).isEqualTo("confidence-none");
        }

        @Test
        void shouldReturnHigh_whenScoreExactly90() {
            TransactionReviewTableRow row = createRowWithConfidence(new BigDecimal("0.9"));
            assertThat(row.getConfidenceLabel()).isEqualTo("HIGH");
        }

        @Test
        void shouldReturnMedium_whenScoreExactly60() {
            TransactionReviewTableRow row = createRowWithConfidence(new BigDecimal("0.6"));
            assertThat(row.getConfidenceLabel()).isEqualTo("MEDIUM");
        }

        @Test
        void shouldReturnMedium_whenScoreIs85() {
            // 85% is above 60% but below 90%, so MEDIUM
            TransactionReviewTableRow row = createRowWithConfidence(new BigDecimal("0.85"));
            assertThat(row.getConfidenceLabel()).isEqualTo("MEDIUM");
        }

        @Test
        void shouldReturnLow_whenScoreIs59() {
            // 59% is below 60%, so LOW
            TransactionReviewTableRow row = createRowWithConfidence(new BigDecimal("0.59"));
            assertThat(row.getConfidenceLabel()).isEqualTo("LOW");
        }
    }

    @Nested
    class StatusStyles {

        @Test
        void pending_shouldReturnCorrectClass() {
            TransactionReviewTableRow row = createRowWithStatus(ReviewStatus.PENDING);
            assertThat(row.getStatusStyleClass()).isEqualTo("status-pending");
        }

        @Test
        void categorized_shouldReturnCorrectClass() {
            TransactionReviewTableRow row = createRowWithStatus(ReviewStatus.CATEGORIZED);
            assertThat(row.getStatusStyleClass()).isEqualTo("status-categorized");
        }

        @Test
        void excluded_shouldReturnCorrectClass() {
            TransactionReviewTableRow row = createRowWithStatus(ReviewStatus.EXCLUDED);
            assertThat(row.getStatusStyleClass()).isEqualTo("status-excluded");
        }

        @Test
        void skipped_shouldReturnCorrectClass() {
            TransactionReviewTableRow row = createRowWithStatus(ReviewStatus.SKIPPED);
            assertThat(row.getStatusStyleClass()).isEqualTo("status-skipped");
        }
    }

    @Nested
    class CategoryAndBusinessDisplay {

        @Test
        void getSuggestedCategoryDisplay_shouldShowName() {
            TransactionReviewTableRow row = new TransactionReviewTableRow(
                UUID.randomUUID(), LocalDate.now(), "Test", BigDecimal.TEN,
                true, ReviewStatus.PENDING, null, null, ExpenseCategory.TRAVEL, null
            );
            assertThat(row.getSuggestedCategoryDisplay()).isEqualTo(ExpenseCategory.TRAVEL.getDisplayName());
        }

        @Test
        void getSuggestedCategoryDisplay_shouldShowDash_whenNull() {
            TransactionReviewTableRow row = createRow(BigDecimal.TEN);
            assertThat(row.getSuggestedCategoryDisplay()).isEqualTo("\u2014");
        }

        @Test
        void getBusinessLabel_shouldShowBusiness() {
            TransactionReviewTableRow row = createRowWithBusiness(true);
            assertThat(row.getBusinessLabel()).isEqualTo("Business");
        }

        @Test
        void getBusinessLabel_shouldShowPersonal() {
            TransactionReviewTableRow row = createRowWithBusiness(false);
            assertThat(row.getBusinessLabel()).isEqualTo("Personal");
        }

        @Test
        void getBusinessLabel_shouldShowDash_whenNull() {
            TransactionReviewTableRow row = createRowWithBusiness(null);
            assertThat(row.getBusinessLabel()).isEqualTo("\u2014");
        }
    }

    @Nested
    class FilterMatching {

        @Test
        void matchesSearch_shouldMatchDescription_caseInsensitive() {
            TransactionReviewTableRow row = new TransactionReviewTableRow(
                UUID.randomUUID(), LocalDate.now(), "Amazon Web Services", BigDecimal.TEN,
                false, ReviewStatus.PENDING, null, null, null, null
            );
            assertThat(row.matchesSearch("amazon")).isTrue();
            assertThat(row.matchesSearch("AMAZON")).isTrue();
            assertThat(row.matchesSearch("web")).isTrue();
            assertThat(row.matchesSearch("netflix")).isFalse();
        }

        @Test
        void matchesSearch_shouldMatchAll_whenNullOrBlank() {
            TransactionReviewTableRow row = createRow(BigDecimal.TEN);
            assertThat(row.matchesSearch(null)).isTrue();
            assertThat(row.matchesSearch("")).isTrue();
            assertThat(row.matchesSearch("  ")).isTrue();
        }

        @Test
        void matchesStatus_shouldMatchAll_whenNull() {
            TransactionReviewTableRow row = createRowWithStatus(ReviewStatus.PENDING);
            assertThat(row.matchesStatus(null)).isTrue();
        }

        @Test
        void matchesStatus_shouldMatchExact() {
            TransactionReviewTableRow row = createRowWithStatus(ReviewStatus.EXCLUDED);
            assertThat(row.matchesStatus(ReviewStatus.EXCLUDED)).isTrue();
            assertThat(row.matchesStatus(ReviewStatus.PENDING)).isFalse();
        }

        @Test
        void matchesDateRange_shouldFilterInclusive() {
            TransactionReviewTableRow row = createRowWithDate(LocalDate.of(2025, 6, 15));

            // Within range
            assertThat(row.matchesDateRange(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30))).isTrue();
            // On boundary
            assertThat(row.matchesDateRange(LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 15))).isTrue();
            // Before range
            assertThat(row.matchesDateRange(LocalDate.of(2025, 7, 1), null)).isFalse();
            // After range
            assertThat(row.matchesDateRange(null, LocalDate.of(2025, 5, 31))).isFalse();
            // Null bounds = match all
            assertThat(row.matchesDateRange(null, null)).isTrue();
        }

        @Test
        void matchesAmountRange_shouldFilterAbsoluteValue() {
            TransactionReviewTableRow row = createRow(new BigDecimal("-75.00"));

            // |amount| = 75
            assertThat(row.matchesAmountRange(new BigDecimal("50"), new BigDecimal("100"))).isTrue();
            assertThat(row.matchesAmountRange(new BigDecimal("76"), null)).isFalse();
            assertThat(row.matchesAmountRange(null, new BigDecimal("74"))).isFalse();
            assertThat(row.matchesAmountRange(null, null)).isTrue();
        }
    }

    // === Helpers ===

    private TransactionReviewTableRow createRow(BigDecimal amount) {
        return new TransactionReviewTableRow(
            UUID.randomUUID(), LocalDate.of(2025, 6, 1), "Test description", amount,
            amount.compareTo(BigDecimal.ZERO) > 0, ReviewStatus.PENDING,
            null, null, null, null
        );
    }

    private TransactionReviewTableRow createRowWithDate(LocalDate date) {
        return new TransactionReviewTableRow(
            UUID.randomUUID(), date, "Test", BigDecimal.TEN,
            true, ReviewStatus.PENDING, null, null, null, null
        );
    }

    private TransactionReviewTableRow createRowWithConfidence(BigDecimal score) {
        return new TransactionReviewTableRow(
            UUID.randomUUID(), LocalDate.now(), "Test", BigDecimal.TEN,
            true, ReviewStatus.PENDING, null, score, null, null
        );
    }

    private TransactionReviewTableRow createRowWithStatus(ReviewStatus status) {
        return new TransactionReviewTableRow(
            UUID.randomUUID(), LocalDate.now(), "Test", BigDecimal.TEN,
            true, status, null, null, null, null
        );
    }

    private TransactionReviewTableRow createRowWithBusiness(Boolean isBusiness) {
        return new TransactionReviewTableRow(
            UUID.randomUUID(), LocalDate.now(), "Test", BigDecimal.TEN,
            true, ReviewStatus.PENDING, isBusiness, null, null, null
        );
    }
}
