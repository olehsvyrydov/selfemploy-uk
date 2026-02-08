package uk.selfemploy.core.reconciliation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for ReconciliationMatch record.
 */
@DisplayName("ReconciliationMatch Tests")
class ReconciliationMatchTest {

    private static final UUID BANK_TX_ID = UUID.randomUUID();
    private static final UUID MANUAL_TX_ID = UUID.randomUUID();
    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void rejectsNullId() {
            assertThatThrownBy(() -> new ReconciliationMatch(
                null, BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, ReconciliationStatus.UNRESOLVED,
                BUSINESS_ID, NOW, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id cannot be null");
        }

        @Test
        void rejectsNullBankTransactionId() {
            assertThatThrownBy(() -> new ReconciliationMatch(
                UUID.randomUUID(), null, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, ReconciliationStatus.UNRESOLVED,
                BUSINESS_ID, NOW, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bankTransactionId cannot be null");
        }

        @Test
        void rejectsNullManualTransactionId() {
            assertThatThrownBy(() -> new ReconciliationMatch(
                UUID.randomUUID(), BANK_TX_ID, null, "INCOME",
                1.0, MatchTier.EXACT, ReconciliationStatus.UNRESOLVED,
                BUSINESS_ID, NOW, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manualTransactionId cannot be null");
        }

        @Test
        void rejectsInvalidManualTransactionType() {
            assertThatThrownBy(() -> new ReconciliationMatch(
                UUID.randomUUID(), BANK_TX_ID, MANUAL_TX_ID, "INVALID",
                1.0, MatchTier.EXACT, ReconciliationStatus.UNRESOLVED,
                BUSINESS_ID, NOW, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manualTransactionType must be INCOME or EXPENSE");
        }

        @Test
        void rejectsConfidenceBelowZero() {
            assertThatThrownBy(() -> new ReconciliationMatch(
                UUID.randomUUID(), BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                -0.1, MatchTier.EXACT, ReconciliationStatus.UNRESOLVED,
                BUSINESS_ID, NOW, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confidence must be between");
        }

        @Test
        void rejectsConfidenceAboveOne() {
            assertThatThrownBy(() -> new ReconciliationMatch(
                UUID.randomUUID(), BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.1, MatchTier.EXACT, ReconciliationStatus.UNRESOLVED,
                BUSINESS_ID, NOW, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("confidence must be between");
        }

        @Test
        void rejectsNullBusinessId() {
            assertThatThrownBy(() -> new ReconciliationMatch(
                UUID.randomUUID(), BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, ReconciliationStatus.UNRESOLVED,
                null, NOW, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("businessId cannot be null");
        }

        @Test
        void acceptsValidIncomeMatch() {
            ReconciliationMatch match = ReconciliationMatch.create(
                BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
            assertThat(match.manualTransactionType()).isEqualTo("INCOME");
        }

        @Test
        void acceptsValidExpenseMatch() {
            ReconciliationMatch match = ReconciliationMatch.create(
                BANK_TX_ID, MANUAL_TX_ID, "EXPENSE",
                0.85, MatchTier.LIKELY, BUSINESS_ID, NOW);
            assertThat(match.manualTransactionType()).isEqualTo("EXPENSE");
        }
    }

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        void createSetsUnresolvedStatus() {
            ReconciliationMatch match = ReconciliationMatch.create(
                BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
            assertThat(match.status()).isEqualTo(ReconciliationStatus.UNRESOLVED);
            assertThat(match.resolvedAt()).isNull();
            assertThat(match.resolvedBy()).isNull();
        }

        @Test
        void createGeneratesUniqueId() {
            ReconciliationMatch match1 = ReconciliationMatch.create(
                BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
            ReconciliationMatch match2 = ReconciliationMatch.create(
                BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
            assertThat(match1.id()).isNotEqualTo(match2.id());
        }
    }

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitions {

        @Test
        void withConfirmedSetsCorrectStatus() {
            ReconciliationMatch match = ReconciliationMatch.create(
                BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, BUSINESS_ID, NOW);

            Instant resolvedAt = Instant.parse("2025-06-16T10:00:00Z");
            ReconciliationMatch confirmed = match.withConfirmed(resolvedAt, "local-user");

            assertThat(confirmed.status()).isEqualTo(ReconciliationStatus.CONFIRMED);
            assertThat(confirmed.resolvedAt()).isEqualTo(resolvedAt);
            assertThat(confirmed.resolvedBy()).isEqualTo("local-user");
            assertThat(confirmed.id()).isEqualTo(match.id());
        }

        @Test
        void withDismissedSetsCorrectStatus() {
            ReconciliationMatch match = ReconciliationMatch.create(
                BANK_TX_ID, MANUAL_TX_ID, "EXPENSE",
                0.85, MatchTier.LIKELY, BUSINESS_ID, NOW);

            Instant resolvedAt = Instant.parse("2025-06-16T10:00:00Z");
            ReconciliationMatch dismissed = match.withDismissed(resolvedAt, "local-user");

            assertThat(dismissed.status()).isEqualTo(ReconciliationStatus.DISMISSED);
            assertThat(dismissed.resolvedAt()).isEqualTo(resolvedAt);
            assertThat(dismissed.resolvedBy()).isEqualTo("local-user");
        }
    }

    @Nested
    @DisplayName("Status Queries")
    class StatusQueries {

        @Test
        void newMatchIsUnresolved() {
            ReconciliationMatch match = ReconciliationMatch.create(
                BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, BUSINESS_ID, NOW);
            assertThat(match.isUnresolved()).isTrue();
            assertThat(match.isConfirmed()).isFalse();
            assertThat(match.isDismissed()).isFalse();
        }

        @Test
        void confirmedMatchIsNotUnresolved() {
            ReconciliationMatch match = ReconciliationMatch.create(
                BANK_TX_ID, MANUAL_TX_ID, "INCOME",
                1.0, MatchTier.EXACT, BUSINESS_ID, NOW)
                .withConfirmed(NOW, "user");
            assertThat(match.isUnresolved()).isFalse();
            assertThat(match.isConfirmed()).isTrue();
            assertThat(match.isDismissed()).isFalse();
        }
    }
}
