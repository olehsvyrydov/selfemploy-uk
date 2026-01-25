package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

/**
 * ViewModel for a single import history item.
 * Represents a past import operation with its status and undo capabilities.
 */
public class ImportHistoryItemViewModel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    // Number of days within which undo is allowed (per /inga finance requirement)
    private static final int UNDO_WINDOW_DAYS = 7;

    private final UUID id;
    private final LocalDateTime importDateTime;
    private final String fileName;
    private final String sourceFilePath;
    private final String bankFormat;
    private final int totalRecords;
    private final int incomeRecords;
    private final int expenseRecords;
    private final BigDecimal incomeTotal;
    private final BigDecimal expenseTotal;
    private final ImportStatus status;
    private final LocalDateTime undoneDateTime; // When was it undone, if applicable
    private final LocalDateTime taxSubmissionDate; // When records were used in tax submission

    // Observable properties
    private final BooleanProperty expanded = new SimpleBooleanProperty(false);

    /**
     * Creates a new import history item view model.
     */
    public ImportHistoryItemViewModel(UUID id, LocalDateTime importDateTime, String fileName,
                                       String sourceFilePath, String bankFormat,
                                       int incomeRecords, int expenseRecords,
                                       BigDecimal incomeTotal, BigDecimal expenseTotal,
                                       ImportStatus status, LocalDateTime undoneDateTime,
                                       LocalDateTime taxSubmissionDate) {
        this.id = id;
        this.importDateTime = importDateTime;
        this.fileName = fileName;
        this.sourceFilePath = sourceFilePath;
        this.bankFormat = bankFormat;
        this.totalRecords = incomeRecords + expenseRecords;
        this.incomeRecords = incomeRecords;
        this.expenseRecords = expenseRecords;
        this.incomeTotal = incomeTotal;
        this.expenseTotal = expenseTotal;
        this.status = status;
        this.undoneDateTime = undoneDateTime;
        this.taxSubmissionDate = taxSubmissionDate;
    }

    // === Display Formatting ===

    public String getFormattedDate() {
        return importDateTime.format(DATE_FORMAT);
    }

    public String getFormattedTime() {
        return importDateTime.format(TIME_FORMAT);
    }

    public String getFormattedDateTime() {
        return importDateTime.format(DATETIME_FORMAT);
    }

    public String getRecordCountText() {
        return totalRecords == 1 ? "1 record imported" : totalRecords + " records imported";
    }

    public String getFormattedIncomeTotal() {
        return "+" + CURRENCY_FORMAT.format(incomeTotal);
    }

    public String getFormattedExpenseTotal() {
        return "-" + CURRENCY_FORMAT.format(expenseTotal);
    }

    public String getBankFormatDisplay() {
        return bankFormat != null && !bankFormat.isBlank()
            ? bankFormat + " (Auto-detected)"
            : "Unknown format";
    }

    // === Undo Eligibility (per /inga finance requirements) ===

    /**
     * Checks if this import can be undone.
     * Undo is only available if:
     * 1. Import is within UNDO_WINDOW_DAYS (7 days)
     * 2. No records were used in tax submission
     * 3. Status is ACTIVE (not already undone)
     */
    public boolean canUndo() {
        if (status != ImportStatus.ACTIVE) {
            return false;
        }
        if (taxSubmissionDate != null) {
            return false;
        }
        return isWithinUndoWindow();
    }

    /**
     * Checks if this import is within the undo window.
     */
    public boolean isWithinUndoWindow() {
        long daysSinceImport = ChronoUnit.DAYS.between(importDateTime.toLocalDate(),
            java.time.LocalDate.now());
        return daysSinceImport <= UNDO_WINDOW_DAYS;
    }

    /**
     * Returns the reason why undo is disabled, or null if undo is available.
     * Used for tooltip text per /inga requirement.
     */
    public String getUndoDisabledReason() {
        if (status == ImportStatus.UNDONE) {
            String undoneDate = undoneDateTime != null
                ? undoneDateTime.format(DATE_FORMAT)
                : "previously";
            return "This import was already undone on " + undoneDate;
        }
        if (taxSubmissionDate != null) {
            return "Cannot undo - records were included in your tax submission on " +
                   taxSubmissionDate.format(DATE_FORMAT);
        }
        if (!isWithinUndoWindow()) {
            return "Undo is only available for imports made within the last 7 days";
        }
        return null; // Undo is available
    }

    /**
     * Returns tooltip text for the undo button.
     */
    public String getUndoTooltipText() {
        String disabledReason = getUndoDisabledReason();
        return disabledReason != null ? disabledReason : "Undo this import";
    }

    // === Accessible Text ===

    /**
     * Returns accessible text for screen readers.
     */
    public String getAccessibleText() {
        String statusText = switch (status) {
            case ACTIVE -> "active";
            case UNDONE -> "undone";
            case LOCKED -> "locked, included in tax submission";
        };
        return String.format("Import from %s, %s, %s, status: %s",
            fileName, getFormattedDateTime(), getRecordCountText(), statusText);
    }

    // === Getters ===

    public UUID getId() {
        return id;
    }

    public LocalDateTime getImportDateTime() {
        return importDateTime;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public String getBankFormat() {
        return bankFormat;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getIncomeRecords() {
        return incomeRecords;
    }

    public int getExpenseRecords() {
        return expenseRecords;
    }

    public BigDecimal getIncomeTotal() {
        return incomeTotal;
    }

    public BigDecimal getExpenseTotal() {
        return expenseTotal;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public LocalDateTime getUndoneDateTime() {
        return undoneDateTime;
    }

    public LocalDateTime getTaxSubmissionDate() {
        return taxSubmissionDate;
    }

    // === Observable Properties ===

    public boolean isExpanded() {
        return expanded.get();
    }

    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }

    @Override
    public String toString() {
        return "ImportHistoryItemViewModel{" +
               "fileName='" + fileName + '\'' +
               ", importDateTime=" + importDateTime +
               ", totalRecords=" + totalRecords +
               ", status=" + status +
               '}';
    }
}
