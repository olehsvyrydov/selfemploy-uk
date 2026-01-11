package uk.selfemploy.core.bankimport;

import jakarta.enterprise.context.ApplicationScoped;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.common.enums.IncomeCategory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Suggests categories for imported transactions based on description keywords.
 *
 * <p>Uses keyword matching to suggest appropriate SA103F expense categories
 * and income categories for bank transactions.</p>
 *
 * <p>Keywords approved by /inga for UK self-employment tax categories.</p>
 */
@ApplicationScoped
public class DescriptionCategorizer {

    // Expense keywords mapped to categories (ordered by priority)
    private static final Map<String, ExpenseCategory> EXPENSE_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, IncomeCategory> INCOME_KEYWORDS = new LinkedHashMap<>();

    static {
        // Office costs - Box 23
        EXPENSE_KEYWORDS.put("amazon", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("office", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("software", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("microsoft", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("adobe", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("stationery", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("staples", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("ryman", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("phone", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("vodafone", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("ee ", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("three ", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("o2 ", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("bt ", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("broadband", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("internet", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("sky ", ExpenseCategory.OFFICE_COSTS);
        EXPENSE_KEYWORDS.put("virgin media", ExpenseCategory.OFFICE_COSTS);

        // Travel - Box 20
        EXPENSE_KEYWORDS.put("uber", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("train", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("trainline", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("national rail", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("travel", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("hotel", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("premier inn", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("travelodge", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("ibis", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("holiday inn", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("airways", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("airlines", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("easyjet", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("ryanair", ExpenseCategory.TRAVEL);
        EXPENSE_KEYWORDS.put("parking", ExpenseCategory.TRAVEL);

        // Travel mileage - Box 20 (fuel)
        EXPENSE_KEYWORDS.put("petrol", ExpenseCategory.TRAVEL_MILEAGE);
        EXPENSE_KEYWORDS.put("diesel", ExpenseCategory.TRAVEL_MILEAGE);
        EXPENSE_KEYWORDS.put("fuel", ExpenseCategory.TRAVEL_MILEAGE);
        EXPENSE_KEYWORDS.put("shell", ExpenseCategory.TRAVEL_MILEAGE);
        EXPENSE_KEYWORDS.put("bp ", ExpenseCategory.TRAVEL_MILEAGE);
        EXPENSE_KEYWORDS.put("esso", ExpenseCategory.TRAVEL_MILEAGE);
        EXPENSE_KEYWORDS.put("texaco", ExpenseCategory.TRAVEL_MILEAGE);

        // Premises - Box 21
        EXPENSE_KEYWORDS.put("electricity", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("gas bill", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("british gas", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("edf", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("scottish power", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("eon", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("sse ", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("octopus energy", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("rent", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("water", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("rates", ExpenseCategory.PREMISES);
        EXPENSE_KEYWORDS.put("business insurance", ExpenseCategory.PREMISES);

        // Professional fees - Box 28
        EXPENSE_KEYWORDS.put("accountant", ExpenseCategory.PROFESSIONAL_FEES);
        EXPENSE_KEYWORDS.put("accounting", ExpenseCategory.PROFESSIONAL_FEES);
        EXPENSE_KEYWORDS.put("solicitor", ExpenseCategory.PROFESSIONAL_FEES);
        EXPENSE_KEYWORDS.put("legal", ExpenseCategory.PROFESSIONAL_FEES);
        EXPENSE_KEYWORDS.put("lawyer", ExpenseCategory.PROFESSIONAL_FEES);

        // Financial charges - Box 26
        EXPENSE_KEYWORDS.put("bank charge", ExpenseCategory.FINANCIAL_CHARGES);
        EXPENSE_KEYWORDS.put("bank fee", ExpenseCategory.FINANCIAL_CHARGES);
        EXPENSE_KEYWORDS.put("transaction fee", ExpenseCategory.FINANCIAL_CHARGES);
        EXPENSE_KEYWORDS.put("card fee", ExpenseCategory.FINANCIAL_CHARGES);

        // Advertising - Box 24
        EXPENSE_KEYWORDS.put("advertising", ExpenseCategory.ADVERTISING);
        EXPENSE_KEYWORDS.put("marketing", ExpenseCategory.ADVERTISING);
        EXPENSE_KEYWORDS.put("google ads", ExpenseCategory.ADVERTISING);
        EXPENSE_KEYWORDS.put("facebook ads", ExpenseCategory.ADVERTISING);
        EXPENSE_KEYWORDS.put("linkedin ads", ExpenseCategory.ADVERTISING);

        // Interest - Box 25
        EXPENSE_KEYWORDS.put("loan interest", ExpenseCategory.INTEREST);

        // Staff costs - Box 19
        EXPENSE_KEYWORDS.put("salary", ExpenseCategory.STAFF_COSTS);
        EXPENSE_KEYWORDS.put("wages", ExpenseCategory.STAFF_COSTS);
        EXPENSE_KEYWORDS.put("payroll", ExpenseCategory.STAFF_COSTS);
        EXPENSE_KEYWORDS.put("pension", ExpenseCategory.STAFF_COSTS);

        // Income keywords
        INCOME_KEYWORDS.put("interest", IncomeCategory.OTHER_INCOME);
        INCOME_KEYWORDS.put("dividend", IncomeCategory.OTHER_INCOME);
        INCOME_KEYWORDS.put("refund", IncomeCategory.OTHER_INCOME);
    }

    /**
     * Suggests an expense category based on the transaction description.
     *
     * @param description the transaction description
     * @return category suggestion with confidence level
     */
    public CategorySuggestion<ExpenseCategory> suggestExpenseCategory(String description) {
        String normalized = normalizeDescription(description);

        for (Map.Entry<String, ExpenseCategory> entry : EXPENSE_KEYWORDS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return new CategorySuggestion<>(entry.getValue(), Confidence.HIGH);
            }
        }

        return new CategorySuggestion<>(ExpenseCategory.OTHER_EXPENSES, Confidence.LOW);
    }

    /**
     * Suggests an income category based on the transaction description.
     *
     * @param description the transaction description
     * @return category suggestion with confidence level
     */
    public CategorySuggestion<IncomeCategory> suggestIncomeCategory(String description) {
        String normalized = normalizeDescription(description);

        for (Map.Entry<String, IncomeCategory> entry : INCOME_KEYWORDS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return new CategorySuggestion<>(entry.getValue(), Confidence.HIGH);
            }
        }

        // Default to SALES for most income
        return new CategorySuggestion<>(IncomeCategory.SALES, Confidence.MEDIUM);
    }

    /**
     * Normalizes a description for keyword matching.
     */
    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        return description.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
