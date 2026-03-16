package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.domain.BankTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Evaluates bank transactions against exclusion rules to identify
 * non-P&amp;L (profit and loss) transactions.
 *
 * <p>Non-P&amp;L transactions should be excluded from income/expense categorization
 * because they do not represent business income or allowable expenses. Common
 * examples include inter-account transfers, tax payments, loan transactions,
 * and cash withdrawals.</p>
 *
 * <p>Rules are evaluated in priority order. The first matching rule wins.
 * Keywords use word-boundary matching to prevent false positives where
 * a keyword appears as a substring of a longer word (e.g., "atm" should
 * not match "atmosphere").</p>
 */
@ApplicationScoped
public class ExclusionRulesEngine {

    private record ExclusionRule(Pattern pattern, String reason) {}

    /**
     * Compiled exclusion rules with word-boundary regex patterns.
     * Ordered by priority (most specific first).
     */
    private static final List<ExclusionRule> EXCLUSION_RULES = new ArrayList<>();

    static {
        // Transfer indicators (inter-account movements, not P&L)
        addRule("tfr", "TRANSFER");
        addRule("transfer", "TRANSFER");
        addRule("fpo", "TRANSFER");
        addRule("fpi", "TRANSFER");

        // HMRC tax payments (not allowable expenses)
        addRule("hmrc", "TAX_PAYMENT");

        // Loan transactions (capital, not P&L)
        addRule("loan credit", "LOAN");
        addRule("loan payment", "LOAN");
        addRule("loan repayment", "LOAN");

        // Credit card payments (avoid double-counting with card statement)
        addRule("cc payment", "CREDIT_CARD");
        addRule("credit card payment", "CREDIT_CARD");

        // Cash withdrawals (unverifiable business use)
        addRule("atm", "CASH_WITHDRAWAL");
        addRule("cash withdrawal", "CASH_WITHDRAWAL");
        addRule("cash w/d", "CASH_WITHDRAWAL");
        addRule("cashpoint", "CASH_WITHDRAWAL");
    }

    private static void addRule(String keyword, String reason) {
        // Use word boundaries (\b) to prevent substring false positives.
        // Pattern.quote escapes any regex special characters in the keyword,
        // then \b anchors at word boundaries on both sides.
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b");
        EXCLUSION_RULES.add(new ExclusionRule(pattern, reason));
    }

    /**
     * Evaluates a bank transaction against all exclusion rules.
     *
     * @param tx the bank transaction to evaluate
     * @return exclusion result indicating whether the transaction should be excluded
     */
    public ExclusionResult evaluate(BankTransaction tx) {
        String normalized = normalizeDescription(tx.description());

        for (ExclusionRule rule : EXCLUSION_RULES) {
            if (rule.pattern().matcher(normalized).find()) {
                return ExclusionResult.excluded(rule.reason());
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
