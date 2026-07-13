package uk.selfemploy.core.bankimport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ReviewStatus;
import uk.selfemploy.persistence.repository.BankTransactionRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("BusinessPersonalService")
class BusinessPersonalServiceTest {

    private BankTransactionRepository repository;
    private Clock clock;
    private BusinessPersonalService service;

    private static final UUID BUSINESS_ID = UUID.randomUUID();
    private static final UUID AUDIT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2025-06-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        repository = mock(BankTransactionRepository.class);
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new BusinessPersonalService(repository, clock);

        when(repository.update(any(BankTransaction.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    private BankTransaction createPending(String description, BigDecimal amount) {
        return BankTransaction.create(
            BUSINESS_ID, AUDIT_ID, "csv-barclays",
            LocalDate.of(2025, 6, 15), amount, description,
            "1234", null, "hash-" + description.hashCode(), NOW
        );
    }

    @Nested
    @DisplayName("flagAsBusiness()")
    class FlagAsBusiness {

        @Test
        @DisplayName("marks transaction as business")
        void marksAsBusiness() {
            BankTransaction tx = createPending("CLIENT PAYMENT", new BigDecimal("1500.00"));
            when(repository.findByIdActive(tx.id())).thenReturn(Optional.of(tx));

            BankTransaction result = service.flagAsBusiness(tx.id());

            assertThat(result.isBusiness()).isTrue();
            verify(repository).update(any(BankTransaction.class));
        }

        @Test
        @DisplayName("throws when transaction not found")
        void throwsWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(repository.findByIdActive(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.flagAsBusiness(missingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("flagAsPersonal()")
    class FlagAsPersonal {

        @Test
        @DisplayName("marks transaction as personal")
        void marksAsPersonal() {
            BankTransaction tx = createPending("GROCERIES", new BigDecimal("-50.00"));
            when(repository.findByIdActive(tx.id())).thenReturn(Optional.of(tx));

            BankTransaction result = service.flagAsPersonal(tx.id());

            assertThat(result.isBusiness()).isFalse();
            verify(repository).update(any(BankTransaction.class));
        }
    }

    @Nested
    @DisplayName("clearFlag()")
    class ClearFlag {

        @Test
        @DisplayName("resets flag to null (uncategorized)")
        void resetsToNull() {
            BankTransaction tx = createPending("SOMETHING", new BigDecimal("-25.00"))
                .withBusinessFlag(true, NOW);
            when(repository.findByIdActive(tx.id())).thenReturn(Optional.of(tx));

            BankTransaction result = service.clearFlag(tx.id());

            assertThat(result.isBusiness()).isNull();
        }
    }

    @Nested
    @DisplayName("Readiness checks")
    class ReadinessChecks {

        @Test
        @DisplayName("hasUncategorizedTransactions returns true when null flags exist")
        void uncategorizedExists() {
            BankTransaction uncategorized = createPending("TX1", new BigDecimal("100.00"));
            when(repository.findByBusinessId(BUSINESS_ID))
                .thenReturn(List.of(uncategorized));

            assertThat(service.hasUncategorizedTransactions(BUSINESS_ID)).isTrue();
        }

        @Test
        @DisplayName("hasUncategorizedTransactions returns false when all flagged")
        void allFlagged() {
            BankTransaction flagged = createPending("TX1", new BigDecimal("100.00"))
                .withBusinessFlag(true, NOW);
            when(repository.findByBusinessId(BUSINESS_ID))
                .thenReturn(List.of(flagged));

            assertThat(service.hasUncategorizedTransactions(BUSINESS_ID)).isFalse();
        }

        @Test
        @DisplayName("countUncategorized returns count of null-flagged transactions")
        void countsUncategorized() {
            BankTransaction tx1 = createPending("TX1", new BigDecimal("100.00"));
            BankTransaction tx2 = createPending("TX2", new BigDecimal("200.00"))
                .withBusinessFlag(true, NOW);
            BankTransaction tx3 = createPending("TX3", new BigDecimal("300.00"));

            when(repository.findByBusinessId(BUSINESS_ID))
                .thenReturn(List.of(tx1, tx2, tx3));

            assertThat(service.countUncategorized(BUSINESS_ID)).isEqualTo(2);
        }

        @Test
        @DisplayName("isReadyForSubmission returns false with uncategorized transactions")
        void notReadyWithUncategorized() {
            BankTransaction uncategorized = createPending("TX1", new BigDecimal("100.00"));
            when(repository.findByBusinessId(BUSINESS_ID))
                .thenReturn(List.of(uncategorized));

            assertThat(service.isReadyForSubmission(BUSINESS_ID)).isFalse();
        }

        @Test
        @DisplayName("isReadyForSubmission returns true when all flagged")
        void readyWhenAllFlagged() {
            BankTransaction business = createPending("TX1", new BigDecimal("100.00"))
                .withBusinessFlag(true, NOW);
            BankTransaction personal = createPending("TX2", new BigDecimal("50.00"))
                .withBusinessFlag(false, NOW);

            when(repository.findByBusinessId(BUSINESS_ID))
                .thenReturn(List.of(business, personal));

            assertThat(service.isReadyForSubmission(BUSINESS_ID)).isTrue();
        }

        @Test
        @DisplayName("isReadyForSubmission returns true with no transactions")
        void readyWithNoTransactions() {
            when(repository.findByBusinessId(BUSINESS_ID))
                .thenReturn(List.of());

            assertThat(service.isReadyForSubmission(BUSINESS_ID)).isTrue();
        }
    }
}
