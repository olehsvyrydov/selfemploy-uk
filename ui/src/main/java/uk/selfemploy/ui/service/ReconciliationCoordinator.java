package uk.selfemploy.ui.service;

import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.reconciliation.ReconciliationMatch;
import uk.selfemploy.core.reconciliation.ReconciliationService;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.ReconciliationIssue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Joins the bank-transaction, income and expense stores to the reconciliation engine and its
 * persistence. For a tax year it reconciles imported bank transactions against manually entered
 * records, saves the resulting matches, and returns the summary the dashboard renders. This is the
 * glue the dashboard previously lacked — before, it only showed hard-coded sample data.
 */
public class ReconciliationCoordinator {

    private final UUID businessId;
    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final ReconciliationMatchRepository matchRepository;
    private final Supplier<List<BankTransaction>> bankTransactionSupplier;

    public ReconciliationCoordinator(UUID businessId,
                                     IncomeService incomeService,
                                     ExpenseService expenseService,
                                     ReconciliationMatchRepository matchRepository,
                                     Supplier<List<BankTransaction>> bankTransactionSupplier) {
        this.businessId = Objects.requireNonNull(businessId);
        this.incomeService = Objects.requireNonNull(incomeService);
        this.expenseService = Objects.requireNonNull(expenseService);
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.bankTransactionSupplier = Objects.requireNonNull(bankTransactionSupplier);
    }

    /**
     * Reconciles the tax year's manual records against the imported bank transactions, persists the
     * matches, and returns the dashboard summary.
     */
    public ReconciliationSummary reconcile(TaxYear taxYear) {
        List<Income> incomes = incomeService.findByTaxYear(businessId, taxYear);
        List<Expense> expenses = expenseService.findByTaxYear(businessId, taxYear);
        List<BankTransaction> bankTransactions = bankTransactionSupplier.get();

        List<ReconciliationMatch> matches = ReconciliationService.reconcile(
            bankTransactions, incomes, expenses, businessId, Instant.now());
        matchRepository.saveAll(matches);

        BigDecimal totalIncome = sum(incomes.stream().map(Income::amount).toList());
        BigDecimal totalExpenses = sum(expenses.stream().map(Expense::amount).toList());

        List<ReconciliationIssue> issues = new ArrayList<>();
        if (!matches.isEmpty()) {
            issues.add(ReconciliationIssue.duplicates(matches.size(),
                sampleDescriptions(matches, bankTransactions)));
        }

        // Every income/expense in this app carries a required category, so there is no
        // "uncategorized" backlog to surface here; the count stays zero by construction.
        return new ReconciliationSummary(totalIncome, totalExpenses,
            incomes.size(), expenses.size(), matches.size(), 0, issues);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<String> sampleDescriptions(List<ReconciliationMatch> matches,
                                                   List<BankTransaction> bankTransactions) {
        Map<UUID, String> descriptionById = bankTransactions.stream()
            .collect(Collectors.toMap(BankTransaction::id, BankTransaction::description, (a, b) -> a));
        return matches.stream()
            .map(m -> descriptionById.get(m.bankTransactionId()))
            .filter(Objects::nonNull)
            .distinct()
            .limit(3)
            .toList();
    }

    /** The dashboard-facing summary of one reconciliation run. */
    public record ReconciliationSummary(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        int incomeCount,
        int expenseCount,
        int duplicateCount,
        int uncategorizedCount,
        List<ReconciliationIssue> issues) {
    }
}
