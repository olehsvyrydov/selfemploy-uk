package uk.selfemploy.core.reconciliation;

import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ReviewStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Detects and manages duplicate transactions between bank-imported records
 * and manually entered income/expense records.
 *
 * <p>Implements a 4-tier matching system:</p>
 * <ul>
 *   <li>Tier 0 (LINKED): Income/Expense already has bankTransactionId FK link</li>
 *   <li>Tier 1 (EXACT): Same date + exact amount + identical normalized description</li>
 *   <li>Tier 2 (LIKELY): Same date + exact amount + Levenshtein similarity >= 0.80</li>
 *   <li>Tier 3 (POSSIBLE): Same date + amount within 1% or GBP 1.00 tolerance</li>
 * </ul>
 *
 * <p>All matching is direction-aware: income bank transactions (amount > 0) are
 * matched only against Income records, and expense bank transactions (amount < 0)
 * are matched only against Expense records.</p>
 *
 * <p>All queries are scoped to the same business_id to prevent cross-business
 * contamination.</p>
 *
 * <p>Manual entries are the source of truth and are never modified or deleted
 * by the reconciliation process.</p>
 */
public class ReconciliationService {

    private ReconciliationService() {
        // Static utility - no instantiation
    }

    /**
     * Runs reconciliation for the given bank transactions against manual entries.
     *
     * <p>Bank transactions that are already EXCLUDED or already linked (have
     * incomeId or expenseId) are skipped.</p>
     *
     * @param bankTransactions the bank transactions to check for duplicates
     * @param incomes          the manually entered income records for the same business
     * @param expenses         the manually entered expense records for the same business
     * @param businessId       the business ID (all data must belong to this business)
     * @param now              current timestamp for created_at
     * @return list of detected reconciliation matches
     */
    public static List<ReconciliationMatch> reconcile(
            List<BankTransaction> bankTransactions,
            List<Income> incomes,
            List<Expense> expenses,
            UUID businessId,
            Instant now) {

        if (bankTransactions == null || bankTransactions.isEmpty()) {
            return Collections.emptyList();
        }
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null");
        }

        // Build set of bank transaction IDs that are already linked to incomes/expenses
        Set<UUID> linkedBankTxIds = buildLinkedBankTxIds(incomes, expenses);

        List<ReconciliationMatch> allMatches = new ArrayList<>();

        for (BankTransaction bankTx : bankTransactions) {
            // Skip transactions not belonging to this business
            if (!bankTx.businessId().equals(businessId)) {
                continue;
            }

            // Skip transactions that are already linked via FK
            if (bankTx.incomeId() != null || bankTx.expenseId() != null) {
                continue;
            }

            // Skip excluded transactions — they are not part of the accounting books
            if (bankTx.reviewStatus() == ReviewStatus.EXCLUDED) {
                continue;
            }

            // Direction-aware matching
            if (bankTx.isIncome()) {
                List<ReconciliationMatch> matches = matchAgainstIncomes(
                    bankTx, incomes != null ? incomes : Collections.emptyList(),
                    linkedBankTxIds, businessId, now);
                allMatches.addAll(matches);
            } else if (bankTx.isExpense()) {
                List<ReconciliationMatch> matches = matchAgainstExpenses(
                    bankTx, expenses != null ? expenses : Collections.emptyList(),
                    linkedBankTxIds, businessId, now);
                allMatches.addAll(matches);
            }
            // Zero-amount transactions are skipped (neither income nor expense)
        }

        return Collections.unmodifiableList(allMatches);
    }

    /**
     * Matches a single income bank transaction against manual income records.
     */
    private static List<ReconciliationMatch> matchAgainstIncomes(
            BankTransaction bankTx,
            List<Income> incomes,
            Set<UUID> linkedBankTxIds,
            UUID businessId,
            Instant now) {

        BigDecimal bankAbsAmount = bankTx.absoluteAmount();
        LocalDate bankDate = bankTx.date();
        String bankNormDesc = MatchingUtils.normalizeDescription(bankTx.description());

        List<ReconciliationMatch> matches = new ArrayList<>();

        for (Income income : incomes) {
            // Skip incomes not belonging to same business
            if (!income.businessId().equals(businessId)) {
                continue;
            }

            // Tier 0: Check if this income is already linked to this bank transaction
            if (income.bankTransactionId() != null
                    && income.bankTransactionId().equals(bankTx.id())) {
                // Already linked - not a duplicate candidate
                continue;
            }

            // Only compare transactions on the same date
            if (!income.date().equals(bankDate)) {
                continue;
            }

            BigDecimal incomeAmount = income.amount(); // Always positive

            // Tier 1: EXACT - same date + exact amount + identical normalized description
            if (MatchingUtils.isExactAmount(bankAbsAmount, incomeAmount)) {
                String incomeNormDesc = MatchingUtils.normalizeDescription(income.description());
                double similarity = MatchingUtils.calculateSimilarity(bankNormDesc, incomeNormDesc);

                if (similarity == 1.0) {
                    matches.add(ReconciliationMatch.create(
                        bankTx.id(), income.id(), "INCOME",
                        1.0, MatchTier.EXACT, businessId, now));
                    continue;
                }

                // Tier 2: LIKELY - same date + exact amount + Levenshtein >= 0.80
                if (similarity >= MatchingUtils.LIKELY_THRESHOLD) {
                    matches.add(ReconciliationMatch.create(
                        bankTx.id(), income.id(), "INCOME",
                        similarity, MatchTier.LIKELY, businessId, now));
                    continue;
                }
            }

            // Tier 3: POSSIBLE - same date + amount within tolerance
            if (MatchingUtils.isWithinTolerance(bankAbsAmount, incomeAmount)) {
                matches.add(ReconciliationMatch.create(
                    bankTx.id(), income.id(), "INCOME",
                    MatchTier.POSSIBLE.getMinimumConfidence(), MatchTier.POSSIBLE,
                    businessId, now));
            }
        }

        return matches;
    }

    /**
     * Matches a single expense bank transaction against manual expense records.
     */
    private static List<ReconciliationMatch> matchAgainstExpenses(
            BankTransaction bankTx,
            List<Expense> expenses,
            Set<UUID> linkedBankTxIds,
            UUID businessId,
            Instant now) {

        BigDecimal bankAbsAmount = bankTx.absoluteAmount(); // Bank expense is negative, abs() makes positive
        LocalDate bankDate = bankTx.date();
        String bankNormDesc = MatchingUtils.normalizeDescription(bankTx.description());

        List<ReconciliationMatch> matches = new ArrayList<>();

        for (Expense expense : expenses) {
            // Skip expenses not belonging to same business
            if (!expense.businessId().equals(businessId)) {
                continue;
            }

            // Tier 0: Check if this expense is already linked to this bank transaction
            if (expense.bankTransactionId() != null
                    && expense.bankTransactionId().equals(bankTx.id())) {
                // Already linked - not a duplicate candidate
                continue;
            }

            // Only compare transactions on the same date
            if (!expense.date().equals(bankDate)) {
                continue;
            }

            BigDecimal expenseAmount = expense.amount(); // Always positive

            // Tier 1: EXACT - same date + exact amount + identical normalized description
            if (MatchingUtils.isExactAmount(bankAbsAmount, expenseAmount)) {
                String expenseNormDesc = MatchingUtils.normalizeDescription(expense.description());
                double similarity = MatchingUtils.calculateSimilarity(bankNormDesc, expenseNormDesc);

                if (similarity == 1.0) {
                    matches.add(ReconciliationMatch.create(
                        bankTx.id(), expense.id(), "EXPENSE",
                        1.0, MatchTier.EXACT, businessId, now));
                    continue;
                }

                // Tier 2: LIKELY - same date + exact amount + Levenshtein >= 0.80
                if (similarity >= MatchingUtils.LIKELY_THRESHOLD) {
                    matches.add(ReconciliationMatch.create(
                        bankTx.id(), expense.id(), "EXPENSE",
                        similarity, MatchTier.LIKELY, businessId, now));
                    continue;
                }
            }

            // Tier 3: POSSIBLE - same date + amount within tolerance
            if (MatchingUtils.isWithinTolerance(bankAbsAmount, expenseAmount)) {
                matches.add(ReconciliationMatch.create(
                    bankTx.id(), expense.id(), "EXPENSE",
                    MatchTier.POSSIBLE.getMinimumConfidence(), MatchTier.POSSIBLE,
                    businessId, now));
            }
        }

        return matches;
    }

    /**
     * Builds a set of bank transaction IDs that are already linked to incomes/expenses
     * via the bankTransactionId FK.
     */
    private static Set<UUID> buildLinkedBankTxIds(List<Income> incomes, List<Expense> expenses) {
        Set<UUID> linked = new HashSet<>();

        if (incomes != null) {
            for (Income income : incomes) {
                if (income.bankTransactionId() != null) {
                    linked.add(income.bankTransactionId());
                }
            }
        }

        if (expenses != null) {
            for (Expense expense : expenses) {
                if (expense.bankTransactionId() != null) {
                    linked.add(expense.bankTransactionId());
                }
            }
        }

        return linked;
    }
}
