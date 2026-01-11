package uk.selfemploy.ui.viewmodel;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Represents a preview row with parsed values and classification.
 * Used to show how each row will be classified (income/expense) based on the mapping.
 *
 * SE-802: Bank Import Column Mapping Wizard
 */
public class ClassifiedPreviewRow {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.UK);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM ''yy");

    private final PreviewRow rawRow;
    private final String date;
    private final String description;
    private final BigDecimal amount;
    private final TransactionType classification;

    /**
     * Creates a classified preview row.
     *
     * @param rawRow the original raw preview row
     * @param date parsed date string
     * @param description parsed description
     * @param amount parsed amount
     * @param classification how this transaction is classified
     */
    public ClassifiedPreviewRow(PreviewRow rawRow, String date, String description,
                                 BigDecimal amount, TransactionType classification) {
        this.rawRow = rawRow;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.classification = classification;
    }

    public PreviewRow getRawRow() {
        return rawRow;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getClassification() {
        return classification;
    }

    /**
     * Returns the amount formatted as GBP currency with sign.
     */
    public String getFormattedAmount() {
        String formatted = CURRENCY_FORMAT.format(amount.abs());
        if (classification == TransactionType.INCOME) {
            return "+" + formatted;
        } else {
            return "-" + formatted;
        }
    }

    /**
     * Returns the CSS class for styling based on classification.
     */
    public String getClassificationCssClass() {
        return classification == TransactionType.INCOME ? "row-income" : "row-expense";
    }

    /**
     * Returns a display badge for the classification.
     */
    public String getClassificationBadge() {
        return classification == TransactionType.INCOME ? "[+] INCOME" : "[-] EXPENSE";
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s",
                date, description, getFormattedAmount(), classification);
    }
}
