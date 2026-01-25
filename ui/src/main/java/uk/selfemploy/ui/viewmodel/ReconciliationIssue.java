package uk.selfemploy.ui.viewmodel;

import java.util.List;
import java.util.UUID;

/**
 * Represents a single reconciliation issue.
 */
public class ReconciliationIssue {

    private final UUID id;
    private final ReconciliationIssueType type;
    private final IssueSeverity severity;
    private final String title;
    private final String description;
    private final int affectedCount;
    private final List<String> details; // Optional details list (e.g., affected items)

    /**
     * Creates a new reconciliation issue.
     *
     * @param id Unique identifier
     * @param type Issue type
     * @param severity Issue severity
     * @param title Issue title
     * @param description Issue description
     * @param affectedCount Number of affected records
     * @param details Optional list of details
     */
    public ReconciliationIssue(UUID id, ReconciliationIssueType type, IssueSeverity severity,
                                String title, String description, int affectedCount, List<String> details) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.affectedCount = affectedCount;
        this.details = details != null ? List.copyOf(details) : List.of();
    }

    /**
     * Creates a duplicate issue.
     */
    public static ReconciliationIssue duplicates(int count, List<String> samples) {
        return new ReconciliationIssue(
            UUID.randomUUID(),
            ReconciliationIssueType.POTENTIAL_DUPLICATES,
            IssueSeverity.HIGH,
            count + " potential duplicate " + (count == 1 ? "expense" : "expenses"),
            "Transactions may have been entered twice",
            count,
            samples
        );
    }

    /**
     * Creates a missing categories issue.
     */
    public static ReconciliationIssue missingCategories(int count) {
        return new ReconciliationIssue(
            UUID.randomUUID(),
            ReconciliationIssueType.MISSING_CATEGORIES,
            IssueSeverity.MEDIUM,
            count + " " + (count == 1 ? "expense" : "expenses") + " without category",
            "Assign categories for accurate tax deduction calculations and SA103 form mapping",
            count,
            null
        );
    }

    /**
     * Creates a date gaps issue.
     */
    public static ReconciliationIssue dateGaps(int count, List<String> months) {
        return new ReconciliationIssue(
            UUID.randomUUID(),
            ReconciliationIssueType.DATE_GAPS,
            IssueSeverity.LOW,
            count + " " + (count == 1 ? "gap" : "gaps") + " in your income records",
            "This may be normal, or you may have forgotten to record some income",
            count,
            months
        );
    }

    // === Getters ===

    public UUID getId() {
        return id;
    }

    public ReconciliationIssueType getType() {
        return type;
    }

    public IssueSeverity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getAffectedCount() {
        return affectedCount;
    }

    public List<String> getDetails() {
        return details;
    }

    public boolean hasDetails() {
        return !details.isEmpty();
    }

    public String getActionText() {
        return type.getActionText();
    }

    /**
     * Returns accessible text for screen readers.
     */
    public String getAccessibleText() {
        return String.format("%s priority: %s. %s. Action: %s",
            severity.getDisplayText(), title, description, getActionText());
    }

    @Override
    public String toString() {
        return "ReconciliationIssue{" +
               "type=" + type +
               ", severity=" + severity +
               ", title='" + title + '\'' +
               ", affectedCount=" + affectedCount +
               '}';
    }
}
