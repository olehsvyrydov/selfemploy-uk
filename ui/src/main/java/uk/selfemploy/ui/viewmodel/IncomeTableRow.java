package uk.selfemploy.ui.viewmodel;

import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.IncomeStatus;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/**
 * Display model for income table rows.
 * Provides formatted values for UI display.
 */
public record IncomeTableRow(
    UUID id,
    LocalDate date,
    String clientName,
    String description,
    BigDecimal amount,
    IncomeStatus status,
    IncomeCategory category,
    String reference
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM ''yy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);

    /**
     * Creates an IncomeTableRow from an Income domain object.
     * Note: Since Income doesn't have clientName and status fields,
     * we extract clientName from description and default status to PAID.
     *
     * @param income The income domain object
     * @param clientName The client name (extracted or provided separately)
     * @param status The payment status
     * @return A new IncomeTableRow
     */
    public static IncomeTableRow fromIncome(Income income, String clientName, IncomeStatus status) {
        return new IncomeTableRow(
            income.id(),
            income.date(),
            clientName,
            income.description(),
            income.amount(),
            status,
            income.category(),
            income.reference()
        );
    }

    /**
     * Creates an IncomeTableRow from an Income with default values.
     * Uses description as client name and defaults to PAID status.
     *
     * @param income The income domain object
     * @return A new IncomeTableRow
     */
    public static IncomeTableRow fromIncome(Income income) {
        // For now, we'll extract client name from the first part of description
        // or use a placeholder. This should be replaced when Income domain is updated.
        String clientName = extractClientName(income.description());
        return fromIncome(income, clientName, IncomeStatus.PAID);
    }

    /**
     * Returns the date formatted for display (e.g., "10 Jan '26").
     */
    public String getFormattedDate() {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Returns the amount formatted as GBP currency (e.g., "£2,500.00").
     */
    public String getFormattedAmount() {
        return CURRENCY_FORMAT.format(amount);
    }

    /**
     * Returns the category display name with SA103 box reference.
     */
    public String getCategoryDisplay() {
        return category.getDisplayName() + " (Box " + category.getSa103Box() + ")";
    }

    /**
     * Returns the status display name.
     */
    public String getStatusDisplay() {
        return status.getDisplayName();
    }

    /**
     * Checks if this income matches the search query.
     * Searches in client name and description (case-insensitive).
     *
     * @param query The search query
     * @return true if matches, false otherwise
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String lowerQuery = query.toLowerCase();
        return (clientName != null && clientName.toLowerCase().contains(lowerQuery))
            || (description != null && description.toLowerCase().contains(lowerQuery));
    }

    /**
     * Checks if this income matches the status filter.
     *
     * @param filterStatus The status to filter by (null means all)
     * @return true if matches, false otherwise
     */
    public boolean matchesStatus(IncomeStatus filterStatus) {
        return filterStatus == null || this.status == filterStatus;
    }

    /**
     * Extracts a client name from the description.
     * This is a temporary solution until the Income domain is updated.
     */
    private static String extractClientName(String description) {
        if (description == null || description.isBlank()) {
            return "Unknown Client";
        }
        // For now, just return the description as the client name
        // In a real implementation, the Income domain would have a separate clientName field
        return description.length() > 50 ? description.substring(0, 50) : description;
    }
}
