package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.BankTransaction;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Evaluates bank transactions against exclusion rules to identify
 * non-P&amp;L (profit and loss) transactions.
 *
 * <p>Non-P&amp;L transactions should be excluded from income/expense categorization
 * because they do not represent business income or allowable expenses. Common
 * examples include inter-account transfers, tax payments, loan transactions,
 * and cash withdrawals.</p>
 *
 * <p>Rules are evaluated in priority order. The first matching rule wins.</p>
 */
@ApplicationScoped
public class ExclusionRulesEngine {

    /**
     * Exclusion rules: keyword → reason category.
     * Ordered by priority (most specific first).
     */
    private static final Map<String, String> EXCLUSION_RULES = new LinkedHashMap<>();

    static {
        // Transfer indicators (inter-account movements, not P&L)
        EXCLUSION_RULES.put("tfr ", "TRANSFER");
        EXCLUSION_RULES.put("tfr-", "TRANSFER");
        EXCLUSION_RULES.put("transfer", "TRANSFER");
        EXCLUSION_RULES.put("fpo ", "TRANSFER");
        EXCLUSION_RULES.put("fpi ", "TRANSFER");

        // HMRC tax payments (not allowable expenses)
        EXCLUSION_RULES.put("hmrc", "TAX_PAYMENT");

        // Loan transactions (capital, not P&L)
        EXCLUSION_RULES.put("loan credit", "LOAN");
        EXCLUSION_RULES.put("loan payment", "LOAN");
        EXCLUSION_RULES.put("loan repayment", "LOAN");

        // Credit card payments (avoid double-counting with card statement)
        EXCLUSION_RULES.put("cc payment", "CREDIT_CARD");
        EXCLUSION_RULES.put("credit card payment", "CREDIT_CARD");

        // Cash withdrawals (unverifiable business use)
        EXCLUSION_RULES.put("atm", "CASH_WITHDRAWAL");
        EXCLUSION_RULES.put("cash withdrawal", "CASH_WITHDRAWAL");
        EXCLUSION_RULES.put("cash ", "CASH_WITHDRAWAL");
    }

    /**
     * Evaluates a bank transaction against all exclusion rules.
     *
     * @param tx the bank transaction to evaluate
     * @return exclusion result indicating whether the transaction should be excluded
     */
    public ExclusionResult evaluate(BankTransaction tx) {
        String normalized = normalizeDescription(tx.description());

        for (Map.Entry<String, String> rule : EXCLUSION_RULES.entrySet()) {
            if (normalized.contains(rule.getKey())) {
                return ExclusionResult.excluded(rule.getValue());
            }
        }

        return ExclusionResult.notExcluded();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        return description.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
