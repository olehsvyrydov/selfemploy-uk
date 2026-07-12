package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.persistence.repository.BankTransactionRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing the business vs personal classification of bank transactions.
 *
 * <p>Each imported bank transaction must be classified as either business or personal
 * before it can be included in a tax submission. The classification uses a nullable
 * Boolean field: null = uncategorized, true = business, false = personal.</p>
 *
 * <p>HMRC submission is blocked while uncategorized transactions exist.</p>
 */
@ApplicationScoped
public class BusinessPersonalService {

    private final BankTransactionRepository repository;
    private final Clock clock;

    @Inject
    public BusinessPersonalService(BankTransactionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Flags a transaction as a business transaction.
     *
     * @param transactionId the transaction to flag
     * @return the updated transaction
     * @throws IllegalArgumentException if the transaction is not found
     */
    @Transactional
    public BankTransaction flagAsBusiness(UUID transactionId) {
        BankTransaction tx = findOrThrow(transactionId);
        BankTransaction updated = tx.withBusinessFlag(true, clock.instant());
        return repository.update(updated);
    }

    /**
     * Flags a transaction as a personal transaction.
     *
     * @param transactionId the transaction to flag
     * @return the updated transaction
     * @throws IllegalArgumentException if the transaction is not found
     */
    @Transactional
    public BankTransaction flagAsPersonal(UUID transactionId) {
        BankTransaction tx = findOrThrow(transactionId);
        BankTransaction updated = tx.withBusinessFlag(false, clock.instant());
        return repository.update(updated);
    }

    /**
     * Clears the business/personal flag, returning to uncategorized state.
     *
     * @param transactionId the transaction to clear
     * @return the updated transaction
     * @throws IllegalArgumentException if the transaction is not found
     */
    @Transactional
    public BankTransaction clearFlag(UUID transactionId) {
        BankTransaction tx = findOrThrow(transactionId);
        BankTransaction updated = tx.withBusinessFlag(null, clock.instant());
        return repository.update(updated);
    }

    /**
     * Checks if there are any uncategorized (null isBusiness) transactions.
     *
     * @param businessId the business to check
     * @return true if any transactions have null business/personal flag
     */
    public boolean hasUncategorizedTransactions(UUID businessId) {
        return countUncategorized(businessId) > 0;
    }

    /**
     * Counts transactions with null business/personal flag.
     *
     * @param businessId the business to check
     * @return count of uncategorized transactions
     */
    public long countUncategorized(UUID businessId) {
        List<BankTransaction> transactions = repository.findByBusinessId(businessId);
        return transactions.stream()
            .filter(tx -> tx.isBusiness() == null)
            .count();
    }

    /**
     * Checks if all transactions are categorized and ready for HMRC submission.
     *
     * @param businessId the business to check
     * @return true if no uncategorized transactions exist
     */
    public boolean isReadyForSubmission(UUID businessId) {
        return !hasUncategorizedTransactions(businessId);
    }

    private BankTransaction findOrThrow(UUID transactionId) {
        return repository.findByIdActive(transactionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Bank transaction not found: " + transactionId));
    }
}
