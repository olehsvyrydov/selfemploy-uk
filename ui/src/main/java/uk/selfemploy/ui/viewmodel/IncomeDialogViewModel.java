package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * ViewModel for the Add/Edit Income Dialog.
 * Manages form data, validation, and save/delete operations.
 */
public class IncomeDialogViewModel {

    private static final int MAX_CLIENT_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final int MAX_REFERENCE_LENGTH = 50;

    private final IncomeService incomeService;
    private final UUID businessId;
    private final TaxYear taxYear;

    // Mode
    private final BooleanProperty editMode = new SimpleBooleanProperty(false);
    private UUID existingIncomeId;

    // Form fields
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>(LocalDate.now());
    private final StringProperty clientName = new SimpleStringProperty("");
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty amount = new SimpleStringProperty("");
    private final ObjectProperty<IncomeCategory> category = new SimpleObjectProperty<>(IncomeCategory.SALES);
    private final ObjectProperty<IncomeStatus> status = new SimpleObjectProperty<>(IncomeStatus.PAID);
    private final StringProperty reference = new SimpleStringProperty("");

    // Validation errors
    private final StringProperty dateError = new SimpleStringProperty();
    private final StringProperty clientNameError = new SimpleStringProperty();
    private final StringProperty descriptionError = new SimpleStringProperty();
    private final StringProperty amountError = new SimpleStringProperty();
    private final StringProperty referenceError = new SimpleStringProperty();

    // Form state
    private final BooleanProperty formValid = new SimpleBooleanProperty(false);
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    // Original values for dirty tracking
    private LocalDate originalDate;
    private String originalClientName;
    private String originalDescription;
    private String originalAmount;
    private IncomeCategory originalCategory;
    private IncomeStatus originalStatus;
    private String originalReference;

    // Flag to prevent dirty tracking during load
    private boolean loading = false;

    // Callbacks
    private Consumer<Income> onSaveCallback;
    private Runnable onDeleteCallback;
    private Runnable onCloseCallback;

    /**
     * Creates a new IncomeDialogViewModel in add mode.
     *
     * @param incomeService The income service for data operations
     * @param businessId The business ID
     * @param taxYear The current tax year
     */
    public IncomeDialogViewModel(IncomeService incomeService, UUID businessId, TaxYear taxYear) {
        this.incomeService = incomeService;
        this.businessId = businessId;
        this.taxYear = taxYear;

        setupValidationListeners();
        storeOriginalValues();
    }

    private void setupValidationListeners() {
        // Validate on field changes
        date.addListener((obs, oldVal, newVal) -> {
            validateDate();
            updateDirtyFlag();
        });
        clientName.addListener((obs, oldVal, newVal) -> {
            validateClientName();
            updateDirtyFlag();
        });
        description.addListener((obs, oldVal, newVal) -> {
            validateDescription();
            updateDirtyFlag();
        });
        amount.addListener((obs, oldVal, newVal) -> {
            validateAmount();
            updateDirtyFlag();
        });
        reference.addListener((obs, oldVal, newVal) -> {
            validateReference();
            updateDirtyFlag();
        });
        category.addListener((obs, oldVal, newVal) -> updateDirtyFlag());
        status.addListener((obs, oldVal, newVal) -> updateDirtyFlag());
    }

    private void storeOriginalValues() {
        originalDate = date.get();
        originalClientName = clientName.get();
        originalDescription = description.get();
        originalAmount = amount.get();
        originalCategory = category.get();
        originalStatus = status.get();
        originalReference = reference.get();
    }

    private void updateDirtyFlag() {
        if (loading) return; // Don't update dirty flag during loading
        boolean isDirty = !java.util.Objects.equals(date.get(), originalDate)
            || !java.util.Objects.equals(clientName.get(), originalClientName)
            || !java.util.Objects.equals(description.get(), originalDescription)
            || !java.util.Objects.equals(amount.get(), originalAmount)
            || !java.util.Objects.equals(category.get(), originalCategory)
            || !java.util.Objects.equals(status.get(), originalStatus)
            || !java.util.Objects.equals(reference.get(), originalReference);
        dirty.set(isDirty);
    }

    /**
     * Loads an existing income for editing.
     *
     * @param income The income to edit
     * @param clientNameValue The client name
     * @param statusValue The payment status
     */
    public void loadIncome(Income income, String clientNameValue, IncomeStatus statusValue) {
        loading = true; // Prevent dirty flag updates during loading
        try {
            editMode.set(true);
            existingIncomeId = income.id();

            date.set(income.date());
            clientName.set(clientNameValue);
            description.set(income.description());
            amount.set(income.amount().toPlainString());
            category.set(income.category());
            status.set(statusValue);
            reference.set(income.reference() != null ? income.reference() : "");

            clearErrors();
            storeOriginalValues();
            validate();
        } finally {
            loading = false;
            dirty.set(false); // Ensure dirty is false after loading
        }
    }

    /**
     * Resets the form to default values.
     */
    public void resetForm() {
        editMode.set(false);
        existingIncomeId = null;

        date.set(LocalDate.now());
        clientName.set("");
        description.set("");
        amount.set("");
        category.set(IncomeCategory.SALES);
        status.set(IncomeStatus.PAID);
        reference.set("");

        clearErrors();
        storeOriginalValues();
        dirty.set(false);
    }

    /**
     * Validates all form fields and returns true if valid.
     */
    public boolean validate() {
        boolean valid = true;
        valid &= validateDate();
        valid &= validateClientName();
        valid &= validateDescription();
        valid &= validateAmount();
        valid &= validateReference();
        formValid.set(valid);
        return valid;
    }

    private boolean validateDate() {
        if (date.get() == null) {
            dateError.set("Date is required");
            updateFormValid();
            return false;
        }
        if (!taxYear.contains(date.get())) {
            dateError.set("Date must be within the current tax year");
            updateFormValid();
            return false;
        }
        dateError.set(null);
        updateFormValid();
        return true;
    }

    private boolean validateClientName() {
        String value = clientName.get();
        if (value == null || value.isBlank()) {
            clientNameError.set("Client name is required");
            updateFormValid();
            return false;
        }
        if (value.length() > MAX_CLIENT_NAME_LENGTH) {
            clientNameError.set("Client name cannot exceed " + MAX_CLIENT_NAME_LENGTH + " characters");
            updateFormValid();
            return false;
        }
        clientNameError.set(null);
        updateFormValid();
        return true;
    }

    private boolean validateDescription() {
        String value = description.get();
        if (value == null || value.isBlank()) {
            descriptionError.set("Description is required");
            updateFormValid();
            return false;
        }
        if (value.length() > MAX_DESCRIPTION_LENGTH) {
            descriptionError.set("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
            updateFormValid();
            return false;
        }
        descriptionError.set(null);
        updateFormValid();
        return true;
    }

    private boolean validateAmount() {
        String value = amount.get();
        if (value == null || value.isBlank()) {
            amountError.set("Amount is required");
            updateFormValid();
            return false;
        }

        BigDecimal parsed = getParsedAmount();
        if (parsed == null) {
            amountError.set("Please enter a valid amount");
            updateFormValid();
            return false;
        }

        if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
            amountError.set("Amount must be positive");
            updateFormValid();
            return false;
        }

        amountError.set(null);
        updateFormValid();
        return true;
    }

    private boolean validateReference() {
        String value = reference.get();
        if (value != null && value.length() > MAX_REFERENCE_LENGTH) {
            referenceError.set("Reference cannot exceed " + MAX_REFERENCE_LENGTH + " characters");
            updateFormValid();
            return false;
        }
        referenceError.set(null);
        updateFormValid();
        return true;
    }

    private void updateFormValid() {
        boolean valid = dateError.get() == null
            && clientNameError.get() == null
            && descriptionError.get() == null
            && amountError.get() == null
            && referenceError.get() == null
            && clientName.get() != null && !clientName.get().isBlank()
            && description.get() != null && !description.get().isBlank()
            && getParsedAmount() != null && getParsedAmount().compareTo(BigDecimal.ZERO) > 0;
        formValid.set(valid);
    }

    private void clearErrors() {
        dateError.set(null);
        clientNameError.set(null);
        descriptionError.set(null);
        amountError.set(null);
        referenceError.set(null);
    }

    /**
     * Parses the amount string to BigDecimal.
     *
     * @return The parsed amount, or null if invalid
     */
    public BigDecimal getParsedAmount() {
        String value = amount.get();
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            // Remove commas and parse
            String cleaned = value.replace(",", "").trim();
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Saves the income (create or update).
     *
     * @return true if save was successful, false otherwise
     */
    public boolean save() {
        if (!validate()) {
            return false;
        }

        try {
            Income savedIncome;
            BigDecimal parsedAmount = getParsedAmount();
            String ref = reference.get().isBlank() ? null : reference.get();

            if (editMode.get()) {
                savedIncome = incomeService.update(
                    existingIncomeId,
                    date.get(),
                    parsedAmount,
                    description.get(),
                    category.get(),
                    ref
                );
            } else {
                savedIncome = incomeService.create(
                    businessId,
                    date.get(),
                    parsedAmount,
                    description.get(),
                    category.get(),
                    ref
                );
            }

            if (onSaveCallback != null) {
                onSaveCallback.accept(savedIncome);
            }

            return true;
        } catch (Exception e) {
            // Handle validation exceptions from service
            return false;
        }
    }

    /**
     * Deletes the current income (edit mode only).
     *
     * @return true if delete was successful, false otherwise
     */
    public boolean delete() {
        if (!editMode.get() || existingIncomeId == null) {
            return false;
        }

        try {
            boolean deleted = incomeService.delete(existingIncomeId);

            if (deleted && onDeleteCallback != null) {
                onDeleteCallback.run();
            }

            return deleted;
        } catch (Exception e) {
            return false;
        }
    }

    // === Getters and Setters ===

    public boolean isEditMode() {
        return editMode.get();
    }

    public BooleanProperty editModeProperty() {
        return editMode;
    }

    public UUID getExistingIncomeId() {
        return existingIncomeId;
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

    public String getClientName() {
        return clientName.get();
    }

    public void setClientName(String value) {
        clientName.set(value);
    }

    public StringProperty clientNameProperty() {
        return clientName;
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

    public IncomeCategory getCategory() {
        return category.get();
    }

    public void setCategory(IncomeCategory value) {
        category.set(value);
    }

    public ObjectProperty<IncomeCategory> categoryProperty() {
        return category;
    }

    public IncomeStatus getStatus() {
        return status.get();
    }

    public void setStatus(IncomeStatus value) {
        status.set(value);
    }

    public ObjectProperty<IncomeStatus> statusProperty() {
        return status;
    }

    public String getReference() {
        return reference.get();
    }

    public void setReference(String value) {
        reference.set(value);
    }

    public StringProperty referenceProperty() {
        return reference;
    }

    public String getDateError() {
        return dateError.get();
    }

    public StringProperty dateErrorProperty() {
        return dateError;
    }

    public String getClientNameError() {
        return clientNameError.get();
    }

    public StringProperty clientNameErrorProperty() {
        return clientNameError;
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

    public String getReferenceError() {
        return referenceError.get();
    }

    public StringProperty referenceErrorProperty() {
        return referenceError;
    }

    public boolean isFormValid() {
        return formValid.get();
    }

    public BooleanProperty formValidProperty() {
        return formValid;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public void setOnSaveCallback(Consumer<Income> callback) {
        this.onSaveCallback = callback;
    }

    public void setOnDeleteCallback(Runnable callback) {
        this.onDeleteCallback = callback;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    public Runnable getOnCloseCallback() {
        return onCloseCallback;
    }

    /**
     * Returns the dialog title based on mode.
     */
    public String getDialogTitle() {
        return editMode.get() ? "Edit Income" : "Add Income";
    }

    /**
     * Returns the save button text based on mode.
     */
    public String getSaveButtonText() {
        return editMode.get() ? "Save Changes" : "Save Income";
    }

    /**
     * Returns the current tax year.
     */
    public TaxYear getTaxYear() {
        return taxYear;
    }

    /**
     * Returns the business ID.
     */
    public UUID getBusinessId() {
        return businessId;
    }
}
