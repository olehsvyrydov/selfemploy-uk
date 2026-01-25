package uk.selfemploy.plugin.extension;

import java.util.List;

/**
 * Extension point for adding custom expense categories.
 *
 * <p>Plugins implement this interface to provide additional expense categories
 * beyond the standard HMRC SA103 categories. Custom categories can be mapped
 * to standard SA103 boxes for tax return purposes.</p>
 *
 * <h2>Standard SA103 Categories</h2>
 * <p>The standard expense categories map to HMRC Self Assessment boxes:</p>
 * <ul>
 *   <li>Box 10: Cost of goods sold</li>
 *   <li>Box 12: Staff costs</li>
 *   <li>Box 13: Travel and subsistence</li>
 *   <li>Box 14: Premises costs</li>
 *   <li>Box 16: Administrative expenses</li>
 *   <li>Box 21: Professional fees</li>
 *   <li>Box 23: Other allowable expenses</li>
 * </ul>
 *
 * <h2>Custom Category Use Cases</h2>
 * <ul>
 *   <li>Industry-specific categories (e.g., "Software Subscriptions" for IT)</li>
 *   <li>Sub-categories for better tracking (e.g., "Home Office", "Coworking")</li>
 *   <li>Client-specific expense tracking</li>
 *   <li>Project-based expense categorization</li>
 * </ul>
 *
 * <h2>Implementation Example</h2>
 * <pre>{@code
 * public class ITExpenseCategories implements ExpenseCategoryExtension {
 *     @Override
 *     public String getExtensionId() {
 *         return "it-expense-categories";
 *     }
 *
 *     @Override
 *     public String getExtensionName() {
 *         return "IT Professional Expenses";
 *     }
 *
 *     @Override
 *     public List<ExpenseCategory> getCategories() {
 *         return List.of(
 *             new ExpenseCategory("software-subscriptions", "Software Subscriptions", "BOX_16"),
 *             new ExpenseCategory("cloud-hosting", "Cloud Hosting & Servers", "BOX_16"),
 *             new ExpenseCategory("domain-names", "Domain Names", "BOX_16")
 *         );
 *     }
 * }
 * }</pre>
 *
 * @see ExtensionPoint
 */
public interface ExpenseCategoryExtension extends ExtensionPoint {

    /**
     * Returns the unique identifier for this extension.
     *
     * <p>The ID must be unique across all expense category extensions.</p>
     *
     * @return the extension ID, never null or blank
     */
    String getExtensionId();

    /**
     * Returns the display name for this extension.
     *
     * <p>This name is shown in settings when managing custom categories.</p>
     *
     * @return the extension name, never null or blank
     */
    String getExtensionName();

    /**
     * Returns a description of the categories this extension provides.
     *
     * <p>This description helps users understand what industries or use cases
     * these categories are designed for.</p>
     *
     * @return the extension description, never null (may be empty)
     */
    default String getExtensionDescription() {
        return "";
    }

    /**
     * Returns the list of custom expense categories provided by this extension.
     *
     * @return list of expense categories, never null or empty
     */
    List<ExpenseCategory> getCategories();

    /**
     * Represents a custom expense category.
     *
     * @param categoryId  unique identifier for the category
     * @param displayName user-visible name
     * @param sa103Box    the SA103 box this maps to (e.g., "BOX_16", "BOX_23")
     * @param description optional description of what expenses belong here
     */
    record ExpenseCategory(
        String categoryId,
        String displayName,
        String sa103Box,
        String description
    ) {
        /**
         * Constructs an ExpenseCategory with validation.
         *
         * @param categoryId  unique identifier, must not be blank
         * @param displayName user-visible name, must not be blank
         * @param sa103Box    SA103 box mapping, must not be blank
         * @param description optional description (may be null)
         */
        public ExpenseCategory {
            if (categoryId == null || categoryId.isBlank()) {
                throw new IllegalArgumentException("Category ID must not be blank");
            }
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Display name must not be blank");
            }
            if (sa103Box == null || sa103Box.isBlank()) {
                throw new IllegalArgumentException("SA103 box must not be blank");
            }
            description = description == null ? "" : description;
        }

        /**
         * Constructs an ExpenseCategory without description.
         *
         * @param categoryId  unique identifier
         * @param displayName user-visible name
         * @param sa103Box    SA103 box mapping
         */
        public ExpenseCategory(String categoryId, String displayName, String sa103Box) {
            this(categoryId, displayName, sa103Box, null);
        }
    }
}
