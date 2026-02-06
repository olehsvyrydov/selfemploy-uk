package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Classifies bank transactions by direction (income/expense) and suggests
 * expense categories with confidence scoring.
 *
 * <p>Confidence thresholds determine the review workflow behavior:
 * <ul>
 *   <li>HIGH (&gt;90%): Auto-suggest, user confirms with one click</li>
 *   <li>MEDIUM (60-90%): Suggest with confirmation dialog</li>
 *   <li>LOW (&lt;60%): Manual categorization required</li>
 * </ul>
 *
 * <p>Direction classification follows the bank statement convention:
 * positive amounts are income (money in), negative amounts are expenses (money out).</p>
 */
@ApplicationScoped
public class TransactionClassificationService {

    private static final BigDecimal HIGH_SCORE = new BigDecimal("0.95");
    private static final BigDecimal MEDIUM_SCORE = new BigDecimal("0.75");
    private static final BigDecimal LOW_SCORE = new BigDecimal("0.30");

    private final DescriptionCategorizer categorizer;

    @Inject
    public TransactionClassificationService(DescriptionCategorizer categorizer) {
        this.categorizer = categorizer;
    }

    /**
     * Classifies a bank transaction by direction and suggests a category.
     *
     * @param tx the bank transaction to classify
     * @return classification result with direction, category, and confidence
     */
    public ClassificationResult classify(BankTransaction tx) {
        boolean isIncome = tx.isIncome();

        if (isIncome) {
            return classifyIncome(tx);
        } else {
            return classifyExpense(tx);
        }
    }

    /**
     * Classifies a transaction and applies the suggestion to the BankTransaction.
     *
     * @param tx        the bank transaction to classify
     * @param timestamp the timestamp for the update
     * @return updated BankTransaction with suggestion applied
     */
    public BankTransaction classifyAndApply(BankTransaction tx, Instant timestamp) {
        ClassificationResult result = classify(tx);
        return tx.withSuggestion(result.suggestedCategory(), result.confidenceScore(), timestamp);
    }

    private ClassificationResult classifyIncome(BankTransaction tx) {
        CategorySuggestion<uk.selfemploy.common.enums.IncomeCategory> suggestion =
            categorizer.suggestIncomeCategory(tx.description());

        BigDecimal score = mapConfidenceToScore(suggestion.confidence());
        return new ClassificationResult(true, null, score, suggestion.confidence());
    }

    private ClassificationResult classifyExpense(BankTransaction tx) {
        CategorySuggestion<ExpenseCategory> suggestion =
            categorizer.suggestExpenseCategory(tx.description());

        BigDecimal score = mapConfidenceToScore(suggestion.confidence());
        return new ClassificationResult(false, suggestion.category(), score, suggestion.confidence());
    }

    /**
     * Maps the categorical confidence level to a numeric score.
     */
    private BigDecimal mapConfidenceToScore(Confidence confidence) {
        return switch (confidence) {
            case HIGH -> HIGH_SCORE;
            case MEDIUM -> MEDIUM_SCORE;
            case LOW -> LOW_SCORE;
        };
    }
}
