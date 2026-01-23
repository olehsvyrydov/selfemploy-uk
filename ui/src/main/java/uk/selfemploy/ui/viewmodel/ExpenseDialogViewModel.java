package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.ReceiptMetadata;
import uk.selfemploy.core.service.ReceiptStorageException;
import uk.selfemploy.core.service.ReceiptStorageService;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

/**
 * ViewModel for the Add/Edit Expense dialog.
 * Manages form state, validation, and interaction with ExpenseService.
 */
public class ExpenseDialogViewModel {

    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_RECEIPTS = 5;

    private final ExpenseService expenseService;
    private ReceiptStorageService receiptStorageService;

    // Business context
    private UUID businessId;
    private TaxYear taxYear;
    private boolean cisBusiness = false;

    // Mode tracking
    private final BooleanProperty editMode = new SimpleBooleanProperty(false);
    private UUID existingExpenseId;
    private UUID tempExpenseIdForReceipts; // Used when attaching receipts to a new (unsaved) expense

    // Form fields
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty amount = new SimpleStringProperty("");
    private final ObjectProperty<ExpenseCategory> category = new SimpleObjectProperty<>(null);
    private final BooleanProperty deductible = new SimpleBooleanProperty(true);
    private final StringProperty notes = new SimpleStringProperty("");

    // Category help
    private final StringProperty categoryHelpTitle = new SimpleStringProperty("");
    private final StringProperty categoryHelpText = new SimpleStringProperty("");
    private final ObservableList<String> categoryHelpExamples = FXCollections.observableArrayList();
    private final BooleanProperty categoryWarning = new SimpleBooleanProperty(false);

    // Validation state
    private final BooleanProperty formValid = new SimpleBooleanProperty(false);
    private final StringProperty dateError = new SimpleStringProperty("");
    private final StringProperty descriptionError = new SimpleStringProperty("");
    private final StringProperty amountError = new SimpleStringProperty("");
    private final StringProperty categoryError = new SimpleStringProperty("");

    // Deductible control
    private final BooleanProperty deductibleEnabled = new SimpleBooleanProperty(true);

    // Dirty tracking
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private boolean ignoreChanges = false;

    // Receipt management (SE-308)
    private final ObservableList<ReceiptMetadata> receipts = FXCollections.observableArrayList();
    private final IntegerProperty receiptCount = new SimpleIntegerProperty(0);
    private final StringProperty receiptCountText = new SimpleStringProperty("0 of 5");
    private final BooleanProperty canAddMoreReceipts = new SimpleBooleanProperty(true);
    private final BooleanProperty hasReceiptError = new SimpleBooleanProperty(false);
    private final StringProperty receiptErrorMessage = new SimpleStringProperty("");
    private final StringProperty receiptErrorHelper = new SimpleStringProperty("");
    private final BooleanProperty dropzoneVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty receiptGridVisible = new SimpleBooleanProperty(false);

    // Callbacks
    private Consumer<Expense> onSave;
    private Runnable onDelete;
    private Runnable onClose;

    public ExpenseDialogViewModel(ExpenseService expenseService) {
        this.expenseService = expenseService;

        // Setup validation listeners
        date.addListener((obs, oldVal, newVal) -> {
            if (!ignoreChanges) dirty.set(true);
            validateDate();
            validateForm();
        });
        description.addListener((obs, oldVal, newVal) -> {
            if (!ignoreChanges) dirty.set(true);
            validateDescription();
            validateForm();
        });
        amount.addListener((obs, oldVal, newVal) -> {
            if (!ignoreChanges) dirty.set(true);
            validateAmount();
            validateForm();
        });
        category.addListener((obs, oldVal, newVal) -> {
            if (!ignoreChanges) dirty.set(true);
            onCategoryChanged(newVal);
            validateCategory();
            validateForm();
        });
        notes.addListener((obs, oldVal, newVal) -> {
            if (!ignoreChanges) dirty.set(true);
        });
    }

    // === Loading and Resetting ===

    /**
     * Loads an existing expense for editing.
     */
    public void loadExpense(Expense expense) {
        ignoreChanges = true;
        try {
            existingExpenseId = expense.id();
            editMode.set(true);

            date.set(expense.date());
            description.set(expense.description());
            amount.set(expense.amount().toPlainString());
            category.set(expense.category());
            notes.set(expense.notes() != null ? expense.notes() : "");

            // Set deductible based on category allowability
            deductible.set(expense.category().isAllowable());
            deductibleEnabled.set(expense.category().isAllowable());

            // Update category help
            onCategoryChanged(expense.category());

            // Load existing receipts (SE-308)
            loadReceiptsForExpense(expense.id());

            // Validate form
            validateForm();

            dirty.set(false);
        } finally {
            ignoreChanges = false;
        }
    }

    /**
     * Loads existing receipts for an expense.
     */
    private void loadReceiptsForExpense(UUID expenseId) {
        receipts.clear();
        if (receiptStorageService != null) {
            List<ReceiptMetadata> existingReceipts = receiptStorageService.listReceipts(expenseId);
            receipts.addAll(existingReceipts);
        }
        updateReceiptState();
    }

    /**
     * Resets the form to default state for adding a new expense.
     */
    public void resetForm() {
        ignoreChanges = true;
        try {
            existingExpenseId = null;
            tempExpenseIdForReceipts = null;
            editMode.set(false);

            date.set(LocalDate.now());
            description.set("");
            amount.set("");
            category.set(null);
            deductible.set(true);
            deductibleEnabled.set(true);
            notes.set("");

            // Clear validation errors
            dateError.set("");
            descriptionError.set("");
            amountError.set("");
            categoryError.set("");

            // Clear category help
            categoryHelpTitle.set("");
            categoryHelpText.set("");
            categoryHelpExamples.clear();
            categoryWarning.set(false);

            // Clear receipts (SE-308)
            receipts.clear();
            updateReceiptState();
            clearReceiptError();

            formValid.set(false);
            dirty.set(false);
        } finally {
            ignoreChanges = false;
        }
    }

    /**
     * Updates receipt-related state properties.
     */
    private void updateReceiptState() {
        int count = receipts.size();
        receiptCount.set(count);
        receiptCountText.set(count + " of " + MAX_RECEIPTS);
        canAddMoreReceipts.set(count < MAX_RECEIPTS);
        dropzoneVisible.set(count == 0);
        receiptGridVisible.set(count > 0);
    }

    // === Validation ===

    private void validateForm() {
        boolean valid = true;

        // Date validation
        if (date.get() == null || !dateError.get().isEmpty()) {
            valid = false;
        }

        // Description validation
        String desc = description.get();
        if (desc == null || desc.isBlank() || desc.length() > MAX_DESCRIPTION_LENGTH) {
            valid = false;
        }

        // Amount validation
        if (!isValidAmount()) {
            valid = false;
        }

        // Category validation
        if (category.get() == null) {
            valid = false;
        }

        formValid.set(valid);
    }

    private void validateDate() {
        LocalDate d = date.get();
        if (d == null) {
            dateError.set("Date is required");
            return;
        }

        if (d.isAfter(LocalDate.now())) {
            dateError.set("Date cannot be in the future");
            return;
        }

        if (taxYear != null && !taxYear.contains(d)) {
            dateError.set("Date must be within tax year " + taxYear.label());
            return;
        }

        dateError.set("");
    }

    private void validateDescription() {
        String desc = description.get();
        if (desc == null || desc.isBlank()) {
            descriptionError.set("Description is required");
            return;
        }

        if (desc.length() > MAX_DESCRIPTION_LENGTH) {
            descriptionError.set("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
            return;
        }

        descriptionError.set("");
    }

    private void validateAmount() {
        String amt = amount.get();
        if (amt == null || amt.isBlank()) {
            amountError.set("Amount is required");
            return;
        }

        try {
            BigDecimal value = new BigDecimal(amt);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                amountError.set("Amount must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            amountError.set("Please enter a valid amount");
            return;
        }

        amountError.set("");
    }

    private void validateCategory() {
        if (category.get() == null) {
            categoryError.set("Please select a category");
            return;
        }

        categoryError.set("");
    }

    private boolean isValidAmount() {
        String amt = amount.get();
        if (amt == null || amt.isBlank()) {
            return false;
        }

        try {
            BigDecimal value = new BigDecimal(amt);
            return value.compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // === Category Help ===

    private void onCategoryChanged(ExpenseCategory cat) {
        if (cat == null) {
            categoryHelpTitle.set("");
            categoryHelpText.set("");
            categoryHelpExamples.clear();
            categoryWarning.set(false);
            deductibleEnabled.set(true);
            return;
        }

        // Update deductible based on category
        boolean isAllowable = cat.isAllowable();
        deductibleEnabled.set(isAllowable);
        if (!isAllowable) {
            deductible.set(false);
        }

        // Set warning mode for non-allowable categories
        categoryWarning.set(!isAllowable);

        // Update help content
        CategoryHelpContent content = getCategoryHelpContent(cat);
        categoryHelpTitle.set(content.title());
        categoryHelpText.set(content.description());
        categoryHelpExamples.setAll(content.examples());
    }

    private CategoryHelpContent getCategoryHelpContent(ExpenseCategory category) {
        return switch (category) {
            case COST_OF_GOODS -> new CategoryHelpContent(
                "Cost of Goods",
                "Goods purchased for resale, raw materials, or direct production costs (SA103 Box 17)",
                List.of("Raw materials for manufacturing", "Goods purchased for resale", "Packaging materials")
            );
            case SUBCONTRACTOR_COSTS -> new CategoryHelpContent(
                "Construction Industry Subcontractor Costs",
                "Payments to subcontractors under the Construction Industry Scheme (SA103 Box 18)",
                List.of("Labour-only subcontractors", "Materials provided by subcontractors", "CIS deductions made")
            );
            case STAFF_COSTS -> new CategoryHelpContent(
                "Staff Costs",
                "Wages, salaries, employer NI contributions, and staff benefits (SA103 Box 19)",
                List.of("Employee wages and salaries", "Employer NI contributions", "Pension contributions", "Staff training")
            );
            case TRAVEL -> new CategoryHelpContent(
                "Travel Expenses (Actual Costs)",
                "Business travel using actual cost method for vehicles (SA103 Box 20)",
                List.of("Fuel for business travel", "Vehicle insurance (business portion)", "Parking fees", "Train and bus fares")
            );
            case TRAVEL_MILEAGE -> new CategoryHelpContent(
                "Travel Expenses (Mileage Rate)",
                "Business travel using simplified mileage allowance (SA103 Box 20)",
                List.of("Cars: 45p/mile (first 10,000), 25p/mile after", "Motorcycles: 24p/mile", "Bicycles: 20p/mile")
            );
            case PREMISES -> new CategoryHelpContent(
                "Premises Costs",
                "Rent, rates, utilities, and insurance for business premises (SA103 Box 21)",
                List.of("Business premises rent", "Business rates", "Electricity and heating", "Building insurance")
            );
            case REPAIRS -> new CategoryHelpContent(
                "Repairs and Maintenance",
                "Repairs and maintenance of property and equipment (SA103 Box 22)",
                List.of("Building repairs", "Equipment maintenance", "Decorating business premises")
            );
            case OFFICE_COSTS -> new CategoryHelpContent(
                "Office Costs",
                "Phone, stationery, and other office expenses (SA103 Box 23)",
                List.of("Phone and internet charges", "Stationery and printing", "Computer software subscriptions", "Postage")
            );
            case ADVERTISING -> new CategoryHelpContent(
                "Advertising and Marketing",
                "Advertising and marketing costs (SA103 Box 24)",
                List.of("Website hosting and maintenance", "Social media advertising", "Business cards and brochures", "Trade show exhibitions")
            );
            case INTEREST -> new CategoryHelpContent(
                "Interest on Business Loans",
                "Interest payments on business loans and HP agreements (SA103 Box 25)",
                List.of("Bank loan interest", "Overdraft interest", "HP interest on business assets")
            );
            case FINANCIAL_CHARGES -> new CategoryHelpContent(
                "Financial Charges",
                "Bank charges, credit card fees, and currency exchange costs (SA103 Box 26)",
                List.of("Bank account charges", "Credit card merchant fees", "Currency exchange costs")
            );
            case BAD_DEBTS -> new CategoryHelpContent(
                "Bad Debts",
                "Irrecoverable debts that have been written off (SA103 Box 27)",
                List.of("Customer invoices that cannot be recovered", "Debts formally written off")
            );
            case PROFESSIONAL_FEES -> new CategoryHelpContent(
                "Professional Fees",
                "Accountancy, legal, and professional subscription fees (SA103 Box 28)",
                List.of("Accountant fees", "Legal fees for business matters", "Professional body subscriptions", "Indemnity insurance")
            );
            case DEPRECIATION -> new CategoryHelpContent(
                "Non-Deductible Category",
                "Depreciation is not an allowable expense for tax purposes. Consider using Capital Allowances instead (SA103 Box 29)",
                List.of("Use Annual Investment Allowance for equipment", "Consider capital allowances for vehicles", "Records kept for accounting purposes only")
            );
            case OTHER_EXPENSES -> new CategoryHelpContent(
                "Other Business Expenses",
                "Other allowable business expenses not covered elsewhere (SA103 Box 30)",
                List.of("Trade subscriptions", "Sundry business expenses", "Net VAT payments (if on flat rate scheme)")
            );
            case HOME_OFFICE_SIMPLIFIED -> new CategoryHelpContent(
                "Home Office (Flat Rate)",
                "Use of home as office using HMRC simplified flat rate method (SA103 Box 30)",
                List.of("25-50 hours/month: £10", "51-100 hours/month: £18", "101+ hours/month: £26")
            );
            case BUSINESS_ENTERTAINMENT -> new CategoryHelpContent(
                "Non-Deductible Category",
                "Business entertainment is not an allowable expense. Only staff entertainment up to £150/head/year is allowable under Staff Costs",
                List.of("Client meals and hospitality", "Customer gifts over £50", "Event tickets for clients")
            );
        };
    }

    // === Save and Delete ===

    /**
     * Saves the expense (creates or updates based on mode).
     */
    public void save() {
        if (!formValid.get()) {
            return;
        }

        BigDecimal amountValue = new BigDecimal(amount.get());
        Expense savedExpense;

        if (editMode.get()) {
            savedExpense = expenseService.update(
                existingExpenseId,
                date.get(),
                amountValue,
                description.get(),
                category.get(),
                null, // receiptPath - managed by ReceiptStorageService
                notes.get().isBlank() ? null : notes.get()
            );
        } else {
            savedExpense = expenseService.create(
                businessId,
                date.get(),
                amountValue,
                description.get(),
                category.get(),
                null, // receiptPath - managed by ReceiptStorageService
                notes.get().isBlank() ? null : notes.get()
            );

            // Reassociate receipts from temp ID to the actual expense ID
            if (tempExpenseIdForReceipts != null && receiptStorageService != null) {
                receiptStorageService.reassociateReceipts(tempExpenseIdForReceipts, savedExpense.id());
                tempExpenseIdForReceipts = null;
            }
        }

        if (onSave != null) {
            onSave.accept(savedExpense);
        }
    }

    /**
     * Deletes the current expense (only valid in edit mode).
     */
    public void delete() {
        if (!editMode.get() || existingExpenseId == null) {
            return;
        }

        expenseService.delete(existingExpenseId);

        if (onDelete != null) {
            onDelete.run();
        }
    }

    // === Available Categories ===

    /**
     * Returns available categories based on whether this is a CIS business.
     */
    public List<ExpenseCategory> getAvailableCategories() {
        return Arrays.stream(ExpenseCategory.values())
            .filter(cat -> cisBusiness || !cat.isCisOnly())
            .toList();
    }

    // === UI Text ===

    /**
     * Returns the dialog title based on mode.
     */
    public String getDialogTitle() {
        return editMode.get() ? "Edit Expense" : "Add Expense";
    }

    /**
     * Returns the save button text based on mode.
     */
    public String getSaveButtonText() {
        return editMode.get() ? "Save Changes" : "Save Expense";
    }

    /**
     * Returns true if delete button should be visible (edit mode only).
     */
    public boolean isDeleteVisible() {
        return editMode.get();
    }

    // === Getters and Setters ===

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public TaxYear getTaxYear() {
        return taxYear;
    }

    public void setTaxYear(TaxYear taxYear) {
        this.taxYear = taxYear;
    }

    public boolean isCisBusiness() {
        return cisBusiness;
    }

    public void setCisBusiness(boolean cisBusiness) {
        this.cisBusiness = cisBusiness;
    }

    public boolean isEditMode() {
        return editMode.get();
    }

    public BooleanProperty editModeProperty() {
        return editMode;
    }

    public LocalDate getDate() {
        return date.get();
    }

    public void setDate(LocalDate value) {
        date.set(value);
    }

    public ObjectProperty<LocalDate> dateProperty() {
        return date;
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(String value) {
        description.set(value);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public String getAmount() {
        return amount.get();
    }

    public void setAmount(String value) {
        amount.set(value);
    }

    public StringProperty amountProperty() {
        return amount;
    }

    public ExpenseCategory getCategory() {
        return category.get();
    }

    public void setCategory(ExpenseCategory value) {
        category.set(value);
    }

    public ObjectProperty<ExpenseCategory> categoryProperty() {
        return category;
    }

    public boolean isDeductible() {
        return deductible.get();
    }

    public void setDeductible(boolean value) {
        deductible.set(value);
    }

    public BooleanProperty deductibleProperty() {
        return deductible;
    }

    public boolean isDeductibleEnabled() {
        return deductibleEnabled.get();
    }

    public BooleanProperty deductibleEnabledProperty() {
        return deductibleEnabled;
    }

    public String getNotes() {
        return notes.get();
    }

    public void setNotes(String value) {
        notes.set(value);
    }

    public StringProperty notesProperty() {
        return notes;
    }

    public String getCategoryHelpTitle() {
        return categoryHelpTitle.get();
    }

    public StringProperty categoryHelpTitleProperty() {
        return categoryHelpTitle;
    }

    public String getCategoryHelpText() {
        return categoryHelpText.get();
    }

    public StringProperty categoryHelpTextProperty() {
        return categoryHelpText;
    }

    public ObservableList<String> getCategoryHelpExamples() {
        return categoryHelpExamples;
    }

    public boolean isCategoryWarning() {
        return categoryWarning.get();
    }

    public BooleanProperty categoryWarningProperty() {
        return categoryWarning;
    }

    public boolean isFormValid() {
        return formValid.get();
    }

    public BooleanProperty formValidProperty() {
        return formValid;
    }

    public String getDateError() {
        return dateError.get();
    }

    public StringProperty dateErrorProperty() {
        return dateError;
    }

    public String getDescriptionError() {
        return descriptionError.get();
    }

    public StringProperty descriptionErrorProperty() {
        return descriptionError;
    }

    public String getAmountError() {
        return amountError.get();
    }

    public StringProperty amountErrorProperty() {
        return amountError;
    }

    public String getCategoryError() {
        return categoryError.get();
    }

    public StringProperty categoryErrorProperty() {
        return categoryError;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public void setOnSave(Consumer<Expense> onSave) {
        this.onSave = onSave;
    }

    public void setOnDelete(Runnable onDelete) {
        this.onDelete = onDelete;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    // === Receipt Management (SE-308) ===

    /**
     * Sets the receipt storage service.
     */
    public void setReceiptStorageService(ReceiptStorageService receiptStorageService) {
        this.receiptStorageService = receiptStorageService;
    }

    /**
     * Attaches a single receipt file.
     */
    public void attachReceipt(File file) {
        if (receiptStorageService == null || file == null) {
            return;
        }

        try {
            UUID expenseId;
            if (existingExpenseId != null) {
                // Editing existing expense - use the real ID
                expenseId = existingExpenseId;
            } else {
                // New expense - use or create a temp ID
                if (tempExpenseIdForReceipts == null) {
                    tempExpenseIdForReceipts = UUID.randomUUID();
                }
                expenseId = tempExpenseIdForReceipts;
            }
            ReceiptMetadata metadata = receiptStorageService.storeReceipt(
                expenseId, file.toPath(), file.getName());
            receipts.add(metadata);
            updateReceiptState();
            if (!ignoreChanges) {
                dirty.set(true);
            }
        } catch (ReceiptStorageException e) {
            showReceiptError(file.getName(), e);
        }
    }

    /**
     * Attaches multiple receipt files.
     */
    public void attachReceipts(List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (File file : files) {
            if (receipts.size() >= MAX_RECEIPTS) {
                showReceiptError(file.getName(),
                    new ReceiptStorageException(ReceiptStorageException.ErrorType.MAX_RECEIPTS_EXCEEDED));
                break;
            }
            attachReceipt(file);
        }
    }

    /**
     * Removes a receipt by ID.
     */
    public void removeReceipt(UUID receiptId) {
        if (receiptStorageService == null || receiptId == null) {
            return;
        }

        receiptStorageService.deleteReceipt(receiptId);
        receipts.removeIf(r -> r.receiptId().equals(receiptId));
        updateReceiptState();
        if (!ignoreChanges) {
            dirty.set(true);
        }
    }

    /**
     * Shows a receipt error message.
     */
    private void showReceiptError(String filename, ReceiptStorageException e) {
        hasReceiptError.set(true);

        switch (e.getErrorType()) {
            case UNSUPPORTED_FORMAT -> {
                receiptErrorMessage.set(filename + " - Unsupported file format");
                receiptErrorHelper.set("Supported: JPG, PNG, PDF, GIF");
            }
            case FILE_TOO_LARGE -> {
                receiptErrorMessage.set(filename + " - File exceeds size limit");
                receiptErrorHelper.set("Maximum file size: 10MB");
            }
            case MAX_RECEIPTS_EXCEEDED -> {
                receiptErrorMessage.set("Maximum 5 receipts per expense reached");
                receiptErrorHelper.set("Remove a receipt to add more");
            }
            case STORAGE_ERROR -> {
                receiptErrorMessage.set("Could not save " + filename);
                receiptErrorHelper.set("Please try again");
            }
            default -> {
                receiptErrorMessage.set("Error processing " + filename);
                receiptErrorHelper.set(e.getMessage());
            }
        }
    }

    /**
     * Clears the receipt error message.
     */
    public void clearReceiptError() {
        hasReceiptError.set(false);
        receiptErrorMessage.set("");
        receiptErrorHelper.set("");
    }

    // === Receipt Getters ===

    public ObservableList<ReceiptMetadata> getReceipts() {
        return receipts;
    }

    public int getReceiptCount() {
        return receiptCount.get();
    }

    public IntegerProperty receiptCountProperty() {
        return receiptCount;
    }

    public String getReceiptCountText() {
        return receiptCountText.get();
    }

    public StringProperty receiptCountTextProperty() {
        return receiptCountText;
    }

    public boolean canAddMoreReceipts() {
        return canAddMoreReceipts.get();
    }

    public BooleanProperty canAddMoreReceiptsProperty() {
        return canAddMoreReceipts;
    }

    public boolean hasReceiptError() {
        return hasReceiptError.get();
    }

    public BooleanProperty hasReceiptErrorProperty() {
        return hasReceiptError;
    }

    public String getReceiptErrorMessage() {
        return receiptErrorMessage.get();
    }

    public StringProperty receiptErrorMessageProperty() {
        return receiptErrorMessage;
    }

    public String getReceiptErrorHelper() {
        return receiptErrorHelper.get();
    }

    public StringProperty receiptErrorHelperProperty() {
        return receiptErrorHelper;
    }

    public boolean isDropzoneVisible() {
        return dropzoneVisible.get();
    }

    public BooleanProperty dropzoneVisibleProperty() {
        return dropzoneVisible;
    }

    public boolean isReceiptGridVisible() {
        return receiptGridVisible.get();
    }

    public BooleanProperty receiptGridVisibleProperty() {
        return receiptGridVisible;
    }

    // === Helper Record ===

    private record CategoryHelpContent(String title, String description, List<String> examples) {}
}
