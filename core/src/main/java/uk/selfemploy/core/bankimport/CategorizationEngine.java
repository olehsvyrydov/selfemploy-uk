package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.common.domain.BankTransaction;

import java.time.Instant;

/**
 * Unified categorization engine for bank transactions.
 *
 * <p>Combines multiple categorization signals into a single recommendation:
 * <ol>
 *   <li>Exclusion rules (transfers, HMRC, loans, ATM)</li>
 *   <li>Direction classification (income vs expense)</li>
 *   <li>Category suggestion with SA103 box mapping</li>
 *   <li>Confidence scoring for review workflow</li>
 * </ol>
 *
 * <p>Exclusion rules are evaluated first. If a transaction matches an exclusion
 * rule, no further classification is performed.</p>
 */
@ApplicationScoped
public class CategorizationEngine {

    private final TransactionClassificationService classificationService;
    private final ExclusionRulesEngine exclusionEngine;

    @Inject
    public CategorizationEngine(
            TransactionClassificationService classificationService,
            ExclusionRulesEngine exclusionEngine) {
        this.classificationService = classificationService;
        this.exclusionEngine = exclusionEngine;
    }

    /**
     * Produces a categorization recommendation for a bank transaction.
     *
     * @param tx the bank transaction to analyze
     * @return comprehensive categorization recommendation
     */
    public CategorizationRecommendation recommend(BankTransaction tx) {
        // Check exclusion rules first
        ExclusionResult exclusionResult = exclusionEngine.evaluate(tx);
        if (exclusionResult.shouldExclude()) {
            return CategorizationRecommendation.excluded(tx.isIncome(), exclusionResult.reason());
        }

        // Classify the transaction
        ClassificationResult classification = classificationService.classify(tx);

        if (classification.isIncome()) {
            return CategorizationRecommendation.income(
                classification.confidenceScore(), classification.confidenceLevel()
            );
        }

        return CategorizationRecommendation.expense(
            classification.suggestedCategory(),
            classification.confidenceScore(),
            classification.confidenceLevel()
        );
    }

    /**
     * Applies the recommendation to a bank transaction, updating its suggestion
     * fields or marking it as excluded.
     *
     * @param tx        the bank transaction to update
     * @param timestamp the timestamp for the update
     * @return updated bank transaction with recommendation applied
     */
    public BankTransaction applyRecommendation(BankTransaction tx, Instant timestamp) {
        CategorizationRecommendation rec = recommend(tx);

        if (rec.shouldExclude()) {
            return tx.withExcluded(rec.exclusionReason(), timestamp);
        }

        return tx.withSuggestion(rec.expenseCategory(), rec.confidenceScore(), timestamp);
    }
}
