package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * ViewModel for a single import candidate row in the Import Review table.
 * Represents a transaction being reviewed for import.
 */
public class ImportCandidateViewModel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    private final UUID id;
    private final LocalDate date;
    private final String description;
    private final BigDecimal amount;
    private final String category;
    private final MatchType matchType;
    private final UUID matchedRecordId; // ID of existing record if EXACT or LIKELY match
    private final MatchedRecordViewModel matchedRecord; // Full details of matched record

    // Observable properties
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final ObjectProperty<ImportAction> action = new SimpleObjectProperty<>();

    /**
     * Creates a new import candidate view model.
     *
     * @param id Unique identifier for this candidate
     * @param date Transaction date
     * @param description Transaction description
     * @param amount Transaction amount (positive for income, negative for expense)
     * @param matchType The duplicate detection result
     * @param matchedRecordId ID of the matched existing record (null for NEW)
     */
    public ImportCandidateViewModel(UUID id, LocalDate date, String description,
                                     BigDecimal amount, MatchType matchType, UUID matchedRecordId) {
        this(id, date, description, amount, null, matchType, matchedRecordId, null);
    }

    /**
     * Creates a new import candidate view model with category and matched record details.
     *
     * @param id Unique identifier for this candidate
     * @param date Transaction date
     * @param description Transaction description
     * @param amount Transaction amount (positive for income, negative for expense)
     * @param category Category name (e.g., "Office Costs")
     * @param matchType The duplicate detection result
     * @param matchedRecordId ID of the matched existing record (null for NEW)
     * @param matchedRecord Full details of matched record (null for NEW)
     */
    public ImportCandidateViewModel(UUID id, LocalDate date, String description,
                                     BigDecimal amount, String category, MatchType matchType,
                                     UUID matchedRecordId, MatchedRecordViewModel matchedRecord) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.category = category != null ? category : "";
        this.matchType = matchType;
        this.matchedRecordId = matchedRecordId;
        this.matchedRecord = matchedRecord;

        // Set default action based on match type
        this.action.set(getDefaultAction(matchType));
    }

    /**
     * Returns the default action for a given match type.
     */
    public static ImportAction getDefaultAction(MatchType matchType) {
        return switch (matchType) {
            case NEW -> ImportAction.IMPORT;
            case EXACT -> ImportAction.SKIP;
            case LIKELY -> ImportAction.IMPORT; // Default to import, user should review
            case SIMILAR -> ImportAction.IMPORT; // Same date+amount, different description - user should review
        };
    }

    // === Getters ===

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getFormattedDate() {
        return date.format(DATE_FORMAT);
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFormattedAmount() {
        String formatted = CURRENCY_FORMAT.format(amount.abs());
        return amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" + formatted : "-" + formatted;
    }

    public boolean isIncome() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isExpense() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public UUID getMatchedRecordId() {
        return matchedRecordId;
    }

    public boolean hasMatch() {
        return matchedRecordId != null;
    }

    public MatchedRecordViewModel getMatchedRecord() {
        return matchedRecord;
    }

    public String getCategory() {
        return category;
    }

    public String getDisplayCategory() {
        return category.isEmpty() ? "-" : category;
    }

    // === Observable Properties ===

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public ImportAction getAction() {
        return action.get();
    }

    public void setAction(ImportAction action) {
        this.action.set(action);
    }

    public ObjectProperty<ImportAction> actionProperty() {
        return action;
    }

    /**
     * Returns true if this candidate will be imported based on current action.
     */
    public boolean willBeImported() {
        return action.get() == ImportAction.IMPORT || action.get() == ImportAction.UPDATE;
    }

    /**
     * Returns accessible text for screen readers.
     */
    public String getAccessibleText() {
        String type = isIncome() ? "Income" : "Expense";
        String status = switch (matchType) {
            case NEW -> "new transaction";
            case EXACT -> "exact duplicate found";
            case LIKELY -> "possible duplicate";
            case SIMILAR -> "same date and amount, different description";
        };
        return String.format("%s: %s on %s, %s, %s, action: %s",
            type, getDescription(), getFormattedDate(), getFormattedAmount(), status, action.get().getDisplayText());
    }

    /**
     * Checks if this candidate matches a search string.
     */
    public boolean matchesSearch(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return true;
        }
        String lower = searchText.toLowerCase();
        return description.toLowerCase().contains(lower) ||
               getFormattedAmount().toLowerCase().contains(lower) ||
               getFormattedDate().toLowerCase().contains(lower);
    }

    @Override
    public String toString() {
        return "ImportCandidateViewModel{" +
               "date=" + date +
               ", description='" + description + '\'' +
               ", amount=" + amount +
               ", matchType=" + matchType +
               ", action=" + action.get() +
               '}';
    }
}
