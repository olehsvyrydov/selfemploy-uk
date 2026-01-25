package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * ViewModel for the Reconciliation Dashboard.
 * Manages data health metrics and issues list.
 */
public class ReconciliationViewModel {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    // Summary metrics
    private final ObjectProperty<BigDecimal> totalIncome = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> totalExpenses = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final IntegerProperty incomeCount = new SimpleIntegerProperty(0);
    private final IntegerProperty expenseCount = new SimpleIntegerProperty(0);
    private final IntegerProperty duplicateCount = new SimpleIntegerProperty(0);
    private final IntegerProperty uncategorizedCount = new SimpleIntegerProperty(0);

    // Issues
    private final ObservableList<ReconciliationIssue> issues = FXCollections.observableArrayList();

    // State
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDateTime> lastChecked = new SimpleObjectProperty<>();
    private final BooleanProperty allClear = new SimpleBooleanProperty(true); // Starts true (no issues yet)

    /**
     * Creates a new ReconciliationViewModel.
     */
    public ReconciliationViewModel() {
        // Update allClear when issues change
        issues.addListener((javafx.collections.ListChangeListener<ReconciliationIssue>) c -> {
            updateAllClearState();
        });

        // Initialize last checked to now
        lastChecked.set(LocalDateTime.now());
    }

    /**
     * Updates the allClear state based on issues, duplicates, and uncategorized counts.
     */
    private void updateAllClearState() {
        allClear.set(issues.isEmpty() && duplicateCount.get() == 0 && uncategorizedCount.get() == 0);
    }

    /**
     * Sets the summary metrics.
     *
     * @param income Total income amount
     * @param expenses Total expense amount
     * @param incomeRecords Number of income records
     * @param expenseRecords Number of expense records
     * @param duplicates Number of potential duplicates
     * @param uncategorized Number of uncategorized expenses
     */
    public void setMetrics(BigDecimal income, BigDecimal expenses,
                           int incomeRecords, int expenseRecords,
                           int duplicates, int uncategorized) {
        totalIncome.set(income);
        totalExpenses.set(expenses);
        incomeCount.set(incomeRecords);
        expenseCount.set(expenseRecords);
        duplicateCount.set(duplicates);
        uncategorizedCount.set(uncategorized);
    }

    /**
     * Sets the issues list.
     *
     * @param newIssues The list of reconciliation issues
     */
    public void setIssues(List<ReconciliationIssue> newIssues) {
        // Sort by severity (HIGH first, then MEDIUM, then LOW)
        List<ReconciliationIssue> sorted = newIssues.stream()
            .sorted(Comparator.comparing(ReconciliationIssue::getSeverity))
            .collect(Collectors.toList());

        issues.setAll(sorted);
        lastChecked.set(LocalDateTime.now());

        // Ensure allClear state is updated even if the list was already empty
        updateAllClearState();
    }

    /**
     * Removes an issue from the list (after resolution).
     *
     * @param issue The issue to remove
     */
    public void removeIssue(ReconciliationIssue issue) {
        issues.remove(issue);
    }

    /**
     * Refreshes the data (triggers a new analysis).
     */
    public void refresh() {
        lastChecked.set(LocalDateTime.now());
        // The controller will call the service and update via setMetrics/setIssues
    }

    // === Formatted Values ===

    public String getFormattedTotalIncome() {
        return CURRENCY_FORMAT.format(totalIncome.get());
    }

    public String getFormattedTotalExpenses() {
        return CURRENCY_FORMAT.format(totalExpenses.get());
    }

    public String getIncomeCountText() {
        int count = incomeCount.get();
        return count == 1 ? "1 record" : count + " records";
    }

    public String getExpenseCountText() {
        int count = expenseCount.get();
        return count == 1 ? "1 record" : count + " records";
    }

    public String getDuplicateCountText() {
        return String.valueOf(duplicateCount.get());
    }

    public String getUncategorizedCountText() {
        return String.valueOf(uncategorizedCount.get());
    }

    public String getLastCheckedText() {
        if (lastChecked.get() == null) {
            return "Never";
        }

        LocalDateTime checked = lastChecked.get();
        LocalDateTime now = LocalDateTime.now();

        long minutesAgo = java.time.Duration.between(checked, now).toMinutes();

        if (minutesAgo < 1) {
            return "Just now";
        } else if (minutesAgo == 1) {
            return "1 minute ago";
        } else if (minutesAgo < 60) {
            return minutesAgo + " minutes ago";
        } else {
            return "at " + checked.format(TIME_FORMAT);
        }
    }

    public int getIssueCount() {
        return issues.size();
    }

    // === State Checks ===

    /**
     * Returns true if there are no issues (all clear).
     */
    public boolean isAllClear() {
        return allClear.get();
    }

    /**
     * Returns true if there are potential duplicates.
     */
    public boolean hasDuplicates() {
        return duplicateCount.get() > 0;
    }

    /**
     * Returns true if there are uncategorized expenses.
     */
    public boolean hasUncategorized() {
        return uncategorizedCount.get() > 0;
    }

    /**
     * Returns issues filtered by severity.
     */
    public List<ReconciliationIssue> getIssuesBySeverity(IssueSeverity severity) {
        return issues.stream()
            .filter(issue -> issue.getSeverity() == severity)
            .collect(Collectors.toList());
    }

    // === Properties ===

    public ObservableList<ReconciliationIssue> getIssues() {
        return issues;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome.get();
    }

    public ObjectProperty<BigDecimal> totalIncomeProperty() {
        return totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses.get();
    }

    public ObjectProperty<BigDecimal> totalExpensesProperty() {
        return totalExpenses;
    }

    public int getIncomeCount() {
        return incomeCount.get();
    }

    public IntegerProperty incomeCountProperty() {
        return incomeCount;
    }

    public int getExpenseCount() {
        return expenseCount.get();
    }

    public IntegerProperty expenseCountProperty() {
        return expenseCount;
    }

    public int getDuplicateCount() {
        return duplicateCount.get();
    }

    public IntegerProperty duplicateCountProperty() {
        return duplicateCount;
    }

    public int getUncategorizedCount() {
        return uncategorizedCount.get();
    }

    public IntegerProperty uncategorizedCountProperty() {
        return uncategorizedCount;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public LocalDateTime getLastChecked() {
        return lastChecked.get();
    }

    public ObjectProperty<LocalDateTime> lastCheckedProperty() {
        return lastChecked;
    }

    public BooleanProperty allClearProperty() {
        return allClear;
    }
}
