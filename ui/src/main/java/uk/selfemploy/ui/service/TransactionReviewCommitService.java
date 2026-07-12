package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Turns a reviewed bank transaction into a committed income or expense record and links the two.
 *
 * <p>Composed from the bank-transaction, income and expense services so the "create the record,
 * link it, flip the review status" invariant lives in one place. A committed transaction can be
 * reverted (its created record deleted) to support undo.</p>
 *
 * <p>The created record's tax year is derived from the transaction date by
 * {@link IncomeService#create}/{@link ExpenseService#create}, so no extra scoping is needed here.</p>
 */
public class TransactionReviewCommitService {

    /** Default SA103 category for an expense committed without a suggested category (box 30). */
    private static final ExpenseCategory DEFAULT_EXPENSE_CATEGORY = ExpenseCategory.OTHER_EXPENSES;

    private final SqliteBankTransactionService bankTransactionService;
    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final UUID businessId;

    public TransactionReviewCommitService(
            SqliteBankTransactionService bankTransactionService,
            IncomeService incomeService,
            ExpenseService expenseService,
            UUID businessId) {
        this.bankTransactionService = bankTransactionService;
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.businessId = businessId;
    }

    /**
     * Commits a transaction as a business record: creates an Income (positive amount) or Expense
     * (negative amount), links it to the transaction and flips the transaction to CATEGORIZED.
     *
     * @return the id of the created income/expense record
     */
    public UUID commitAsBusiness(BankTransaction tx, Instant now) {
        // Idempotent guard: a transaction already linked to a record must not be committed again
        // (that would create a duplicate and orphan the previous link, and would break undo, which
        // assumes a commit takes a link from null to non-null).
        if (tx.incomeId() != null) {
            return tx.incomeId();
        }
        if (tx.expenseId() != null) {
            return tx.expenseId();
        }
        if (tx.isIncome()) {
            Income created = incomeService.create(
                businessId, tx.date(), tx.absoluteAmount(), tx.description(), IncomeCategory.SALES, null);
            bankTransactionService.categorizeAsIncome(tx.id(), created.id(), now);
            return created.id();
        }
        ExpenseCategory category = tx.suggestedCategory() != null
            ? tx.suggestedCategory()
            : DEFAULT_EXPENSE_CATEGORY;
        Expense created = expenseService.create(
            businessId, tx.date(), tx.absoluteAmount(), tx.description(), category, null, null);
        bankTransactionService.categorizeAsExpense(tx.id(), created.id(), now);
        return created.id();
    }

    /**
     * Commits every given transaction as a business record. Fast path for "commit all as business".
     */
    public void commitAllBusiness(List<BankTransaction> transactions, Instant now) {
        for (BankTransaction tx : transactions) {
            commitAsBusiness(tx, now);
        }
    }

    /**
     * Reverts a commit by deleting the income/expense record the transaction was linked to. The
     * caller is responsible for restoring the transaction's own review status.
     */
    public void revertCommit(BankTransaction tx) {
        if (tx.incomeId() != null) {
            incomeService.delete(tx.incomeId());
        }
        if (tx.expenseId() != null) {
            expenseService.delete(tx.expenseId());
        }
    }
}
