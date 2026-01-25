package uk.selfemploy.ui.viewmodel;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * ViewModel representing an existing (matched) record for comparison during import.
 * Used in the comparison dialog to show the existing record alongside the import candidate.
 *
 * <p>SE-10B-005: Import Review UI - Comparison Dialog</p>
 */
public class MatchedRecordViewModel {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    private final UUID id;
    private final LocalDate date;
    private final String description;
    private final BigDecimal amount;
    private final String category;

    /**
     * Creates a new matched record view model.
     *
     * @param id Record ID
     * @param date Transaction date
     * @param description Transaction description
     * @param amount Transaction amount
     * @param category Category name (e.g., "Office Costs", "Travel")
     */
    public MatchedRecordViewModel(UUID id, LocalDate date, String description,
                                   BigDecimal amount, String category) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.category = category != null ? category : "";
    }

    // === Getters ===

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getFormattedDate() {
        return date != null ? date.format(DATE_FORMAT) : "-";
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFormattedAmount() {
        if (amount == null) {
            return "-";
        }
        String formatted = CURRENCY_FORMAT.format(amount.abs());
        return amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" + formatted : "-" + formatted;
    }

    public String getCategory() {
        return category;
    }

    public String getDisplayCategory() {
        return category.isEmpty() ? "-" : category;
    }

    public boolean isIncome() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isExpense() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    @Override
    public String toString() {
        return "MatchedRecordViewModel{" +
               "id=" + id +
               ", date=" + date +
               ", description='" + description + '\'' +
               ", amount=" + amount +
               ", category='" + category + '\'' +
               '}';
    }
}
