package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.core.reconciliation.MatchingUtils;
import uk.selfemploy.persistence.repository.ExpenseRepository;
import uk.selfemploy.persistence.repository.IncomeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Detects duplicate transactions by comparing imported transactions
 * against existing income and expense records.
 *
 * <p>Duplicate detection is based on:
 * <ul>
 *   <li>Date (exact match)</li>
 *   <li>Amount (exact match, considering sign)</li>
 *   <li>Description (normalized: lowercase, trimmed, collapsed whitespace)</li>
 * </ul>
 */
@ApplicationScoped
public class DuplicateDetector {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    @Inject
    public DuplicateDetector(IncomeRepository incomeRepository, ExpenseRepository expenseRepository) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Checks a list of imported transactions for duplicates.
     *
     * @param businessId the business ID to check against
     * @param imports the list of imported transactions to check
     * @return result containing unique and duplicate transactions
     */
    public DuplicateCheckResult checkDuplicates(UUID businessId, List<ImportedTransaction> imports) {
        if (imports.isEmpty()) {
            return new DuplicateCheckResult(Collections.emptyList(), Collections.emptyList());
        }

        // Find date range of imports
        LocalDate minDate = imports.stream()
            .map(ImportedTransaction::date)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());

        LocalDate maxDate = imports.stream()
            .map(ImportedTransaction::date)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());

        // Build set of existing transaction hashes
        Set<String> existingHashes = new HashSet<>();

        // Load existing incomes in date range
        List<Income> existingIncomes = incomeRepository.findByDateRange(businessId, minDate, maxDate);
        for (Income income : existingIncomes) {
            existingHashes.add(createIncomeHash(income));
        }

        // Load existing expenses in date range
        List<Expense> existingExpenses = expenseRepository.findByDateRange(businessId, minDate, maxDate);
        for (Expense expense : existingExpenses) {
            existingHashes.add(createExpenseHash(expense));
        }

        // Check each import against existing and previously seen imports
        List<ImportedTransaction> uniqueTransactions = new ArrayList<>();
        List<ImportedTransaction> duplicateTransactions = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();

        for (ImportedTransaction tx : imports) {
            String hash = tx.transactionHash();

            if (existingHashes.contains(hash) || seenInBatch.contains(hash)) {
                duplicateTransactions.add(tx);
            } else {
                uniqueTransactions.add(tx);
                seenInBatch.add(hash);
            }
        }

        return new DuplicateCheckResult(uniqueTransactions, duplicateTransactions);
    }

    /**
     * Creates a hash string for an existing income record.
     * Delegates normalization to shared MatchingUtils.
     */
    private String createIncomeHash(Income income) {
        return MatchingUtils.createExactKey(income.date(), income.amount(), income.description());
    }

    /**
     * Creates a hash string for an existing expense record.
     *
     * <p>Note: Expense amounts are stored as positive in the database,
     * but imported expenses are negative. We convert to negative for comparison.</p>
     */
    private String createExpenseHash(Expense expense) {
        // Expenses are stored as positive but imported as negative
        BigDecimal negativeAmount = expense.amount().negate();
        return MatchingUtils.createExactKey(expense.date(), negativeAmount, expense.description());
    }
}
