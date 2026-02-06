package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.IncomeStatus;
import uk.selfemploy.core.service.IncomeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD tests for IncomeDialogViewModel.
 * Tests form validation, data binding, and save/delete operations.
 */
@DisplayName("IncomeDialogViewModel")
class IncomeDialogViewModelTest {

    @Mock
    private IncomeService incomeService;

    private IncomeDialogViewModel viewModel;
    private UUID businessId;
    private TaxYear taxYear;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        businessId = UUID.randomUUID();
        taxYear = TaxYear.of(2025);
        viewModel = new IncomeDialogViewModel(incomeService, businessId, taxYear);
    }

    @Nested
    @DisplayName("Initial State - Add Mode")
    class InitialStateAddMode {

        @Test
        @DisplayName("should not be in edit mode")
        void shouldNotBeInEditMode() {
            assertThat(viewModel.isEditMode()).isFalse();
        }

        @Test
        @DisplayName("should have date set to today")
        void shouldHaveDateSetToToday() {
            assertThat(viewModel.getDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("should have empty client name")
        void shouldHaveEmptyClientName() {
            assertThat(viewModel.getClientName()).isEmpty();
        }

        @Test
        @DisplayName("should have empty description")
        void shouldHaveEmptyDescription() {
            assertThat(viewModel.getDescription()).isEmpty();
        }

        @Test
        @DisplayName("should have empty amount")
        void shouldHaveEmptyAmount() {
            assertThat(viewModel.getAmount()).isEmpty();
        }

        @Test
        @DisplayName("should have default category as SALES")
        void shouldHaveDefaultCategoryAsSales() {
            assertThat(viewModel.getCategory()).isEqualTo(IncomeCategory.SALES);
        }

        @Test
        @DisplayName("should have default status as PAID")
        void shouldHaveDefaultStatusAsPaid() {
            assertThat(viewModel.getStatus()).isEqualTo(IncomeStatus.PAID);
        }

        @Test
        @DisplayName("should have empty reference")
        void shouldHaveEmptyReference() {
            assertThat(viewModel.getReference()).isEmpty();
        }

        @Test
        @DisplayName("should have form invalid initially")
        void shouldHaveFormInvalidInitially() {
            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should not be dirty initially")
        void shouldNotBeDirtyInitially() {
            assertThat(viewModel.isDirty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edit Mode")
    class EditMode {

        private Income existingIncome;

        @BeforeEach
        void setUpExistingIncome() {
            existingIncome = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("2500.00"),
                "Website redesign",
                IncomeCategory.SALES,
                "INV-001",
                null,
                null,
                null,
                null
            );
        }

        @Test
        @DisplayName("should be in edit mode when loading existing income")
        void shouldBeInEditModeWhenLoadingExistingIncome() {
            // When
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);

            // Then
            assertThat(viewModel.isEditMode()).isTrue();
        }

        @Test
        @DisplayName("should populate all fields from existing income")
        void shouldPopulateAllFieldsFromExistingIncome() {
            // When
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);

            // Then
            assertThat(viewModel.getDate()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(viewModel.getClientName()).isEqualTo("Acme Corp");
            assertThat(viewModel.getDescription()).isEqualTo("Website redesign");
            assertThat(viewModel.getAmount()).isEqualTo("2500.00");
            assertThat(viewModel.getCategory()).isEqualTo(IncomeCategory.SALES);
            assertThat(viewModel.getStatus()).isEqualTo(IncomeStatus.PAID);
            assertThat(viewModel.getReference()).isEqualTo("INV-001");
        }

        @Test
        @DisplayName("should have form valid when loaded with valid income")
        void shouldHaveFormValidWhenLoadedWithValidIncome() {
            // When
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);

            // Then
            assertThat(viewModel.isFormValid()).isTrue();
        }

        @Test
        @DisplayName("should not be dirty after loading")
        void shouldNotBeDirtyAfterLoading() {
            // When
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);

            // Then
            assertThat(viewModel.isDirty()).isFalse();
        }

        @Test
        @DisplayName("should become dirty when field changes")
        void shouldBecomeDirtyWhenFieldChanges() {
            // Given
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);

            // When
            viewModel.setClientName("Modified Corp");

            // Then
            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should store existing income ID")
        void shouldStoreExistingIncomeId() {
            // When
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);

            // Then
            assertThat(viewModel.getExistingIncomeId()).isEqualTo(existingIncome.id());
        }
    }

    @Nested
    @DisplayName("Field Validation")
    class FieldValidation {

        @Test
        @DisplayName("should require date")
        void shouldRequireDate() {
            // Given - fill all required fields except date
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount("1000.00");
            viewModel.setDate(null);

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getDateError()).isEqualTo("Date is required");
        }

        @Test
        @DisplayName("should validate date within tax year")
        void shouldValidateDateWithinTaxYear() {
            // Given
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount("1000.00");
            viewModel.setDate(LocalDate.of(2023, 1, 1)); // Outside current tax year

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getDateError()).isEqualTo("Date must be within the current tax year");
        }

        @Test
        @DisplayName("should require client name")
        void shouldRequireClientName() {
            // Given
            viewModel.setDescription("Website design");
            viewModel.setAmount("1000.00");
            viewModel.setClientName(""); // Already empty, need to call validate

            // When
            viewModel.validate();

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getClientNameError()).isEqualTo("Client name is required");
        }

        @Test
        @DisplayName("should limit client name to 100 characters")
        void shouldLimitClientNameTo100Characters() {
            // Given
            String longName = "A".repeat(101);
            viewModel.setClientName(longName);
            viewModel.setDescription("Website design");
            viewModel.setAmount("1000.00");

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getClientNameError()).isEqualTo("Client name cannot exceed 100 characters");
        }

        @Test
        @DisplayName("should require description")
        void shouldRequireDescription() {
            // Given
            viewModel.setClientName("Acme Corp");
            viewModel.setAmount("1000.00");
            viewModel.setDescription(""); // Already empty, need to call validate

            // When
            viewModel.validate();

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getDescriptionError()).isEqualTo("Description is required");
        }

        @Test
        @DisplayName("should limit description to 200 characters")
        void shouldLimitDescriptionTo200Characters() {
            // Given
            String longDescription = "A".repeat(201);
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription(longDescription);
            viewModel.setAmount("1000.00");

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getDescriptionError()).isEqualTo("Description cannot exceed 200 characters");
        }

        @Test
        @DisplayName("should require amount")
        void shouldRequireAmount() {
            // Given
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount(""); // Already empty, need to call validate

            // When
            viewModel.validate();

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getAmountError()).isEqualTo("Amount is required");
        }

        @Test
        @DisplayName("should validate amount is positive")
        void shouldValidateAmountIsPositive() {
            // Given
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount("-100.00");

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getAmountError()).isEqualTo("Amount must be positive");
        }

        @Test
        @DisplayName("should validate amount is not zero")
        void shouldValidateAmountIsNotZero() {
            // Given
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount("0.00");

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getAmountError()).isEqualTo("Amount must be positive");
        }

        @Test
        @DisplayName("should validate amount is numeric")
        void shouldValidateAmountIsNumeric() {
            // Given
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount("not-a-number");

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getAmountError()).isEqualTo("Please enter a valid amount");
        }

        @Test
        @DisplayName("should accept valid form")
        void shouldAcceptValidForm() {
            // Given
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount("1000.00");

            // Then
            assertThat(viewModel.isFormValid()).isTrue();
            assertThat(viewModel.getDateError()).isNull();
            assertThat(viewModel.getClientNameError()).isNull();
            assertThat(viewModel.getDescriptionError()).isNull();
            assertThat(viewModel.getAmountError()).isNull();
        }

        @Test
        @DisplayName("should clear errors when field becomes valid")
        void shouldClearErrorsWhenFieldBecomesValid() {
            // Given - invalid state
            viewModel.setClientName("");
            viewModel.validate();
            assertThat(viewModel.getClientNameError()).isNotNull();

            // When - make valid
            viewModel.setClientName("Acme Corp");

            // Then
            assertThat(viewModel.getClientNameError()).isNull();
        }

        @Test
        @DisplayName("should limit reference to 50 characters")
        void shouldLimitReferenceTo50Characters() {
            // Given
            String longReference = "A".repeat(51);
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount("1000.00");
            viewModel.setReference(longReference);

            // Then
            assertThat(viewModel.isFormValid()).isFalse();
            assertThat(viewModel.getReferenceError()).isEqualTo("Reference cannot exceed 50 characters");
        }
    }

    @Nested
    @DisplayName("Amount Parsing")
    class AmountParsing {

        @Test
        @DisplayName("should parse integer amount")
        void shouldParseIntegerAmount() {
            // When
            viewModel.setAmount("1000");

            // Then
            assertThat(viewModel.getParsedAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        @DisplayName("should parse decimal amount")
        void shouldParseDecimalAmount() {
            // When
            viewModel.setAmount("1000.50");

            // Then
            assertThat(viewModel.getParsedAmount()).isEqualByComparingTo(new BigDecimal("1000.50"));
        }

        @Test
        @DisplayName("should parse amount with commas")
        void shouldParseAmountWithCommas() {
            // When
            viewModel.setAmount("1,000.50");

            // Then
            assertThat(viewModel.getParsedAmount()).isEqualByComparingTo(new BigDecimal("1000.50"));
        }

        @Test
        @DisplayName("should handle empty amount")
        void shouldHandleEmptyAmount() {
            // When
            viewModel.setAmount("");

            // Then
            assertThat(viewModel.getParsedAmount()).isNull();
        }

        @Test
        @DisplayName("should handle invalid amount")
        void shouldHandleInvalidAmount() {
            // When
            viewModel.setAmount("invalid");

            // Then
            assertThat(viewModel.getParsedAmount()).isNull();
        }
    }

    @Nested
    @DisplayName("Save Operation")
    class SaveOperation {

        @BeforeEach
        void setUpValidForm() {
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount("1000.00");
        }

        @Test
        @DisplayName("should call service create for new income")
        void shouldCallServiceCreateForNewIncome() {
            // Given
            Income createdIncome = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.now(),
                new BigDecimal("1000.00"),
                "Website design",
                IncomeCategory.SALES,
                null,
                null,
                null,
                null,
                null
            );
            when(incomeService.create(any(), any(), any(), any(), any(), any())).thenReturn(createdIncome);

            // When
            viewModel.save();

            // Then
            verify(incomeService).create(
                eq(businessId),
                eq(LocalDate.now()),
                eq(new BigDecimal("1000.00")),
                eq("Website design"),
                eq(IncomeCategory.SALES),
                isNull()
            );
        }

        @Test
        @DisplayName("should call service update for existing income")
        void shouldCallServiceUpdateForExistingIncome() {
            // Given
            Income existingIncome = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("2500.00"),
                "Old description",
                IncomeCategory.SALES,
                "INV-001",
                null,
                null,
                null,
                null
            );
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);
            viewModel.setDescription("Updated description");

            when(incomeService.update(any(), any(), any(), any(), any(), any())).thenReturn(existingIncome);

            // When
            viewModel.save();

            // Then
            verify(incomeService).update(
                eq(existingIncome.id()),
                eq(LocalDate.of(2025, 6, 15)),
                eq(new BigDecimal("2500.00")),
                eq("Updated description"),
                eq(IncomeCategory.SALES),
                eq("INV-001")
            );
        }

        @Test
        @DisplayName("should trigger save callback on success")
        void shouldTriggerSaveCallbackOnSuccess() {
            // Given
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            AtomicReference<Income> savedIncome = new AtomicReference<>();

            Income createdIncome = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.now(),
                new BigDecimal("1000.00"),
                "Website design",
                IncomeCategory.SALES,
                null,
                null,
                null,
                null,
                null
            );
            when(incomeService.create(any(), any(), any(), any(), any(), any())).thenReturn(createdIncome);

            viewModel.setOnSaveCallback(income -> {
                callbackCalled.set(true);
                savedIncome.set(income);
            });

            // When
            viewModel.save();

            // Then
            assertThat(callbackCalled.get()).isTrue();
            assertThat(savedIncome.get()).isEqualTo(createdIncome);
        }

        @Test
        @DisplayName("should not save when form is invalid")
        void shouldNotSaveWhenFormIsInvalid() {
            // Given
            viewModel.setClientName(""); // Make form invalid

            // When
            boolean result = viewModel.save();

            // Then
            assertThat(result).isFalse();
            verify(incomeService, never()).create(any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Delete Operation")
    class DeleteOperation {

        private Income existingIncome;

        @BeforeEach
        void setUpExistingIncome() {
            existingIncome = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("2500.00"),
                "Website redesign",
                IncomeCategory.SALES,
                "INV-001",
                null,
                null,
                null,
                null
            );
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);
        }

        @Test
        @DisplayName("should call service delete for existing income")
        void shouldCallServiceDeleteForExistingIncome() {
            // Given
            when(incomeService.delete(existingIncome.id())).thenReturn(true);

            // When
            viewModel.delete();

            // Then
            verify(incomeService).delete(existingIncome.id());
        }

        @Test
        @DisplayName("should trigger delete callback on success")
        void shouldTriggerDeleteCallbackOnSuccess() {
            // Given
            AtomicBoolean callbackCalled = new AtomicBoolean(false);
            when(incomeService.delete(existingIncome.id())).thenReturn(true);

            viewModel.setOnDeleteCallback(() -> callbackCalled.set(true));

            // When
            viewModel.delete();

            // Then
            assertThat(callbackCalled.get()).isTrue();
        }

        @Test
        @DisplayName("should not delete in add mode")
        void shouldNotDeleteInAddMode() {
            // Given
            IncomeDialogViewModel addModeViewModel = new IncomeDialogViewModel(incomeService, businessId, taxYear);

            // When
            boolean result = addModeViewModel.delete();

            // Then
            assertThat(result).isFalse();
            verify(incomeService, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Reset Form")
    class ResetForm {

        @Test
        @DisplayName("should reset all fields to defaults")
        void shouldResetAllFieldsToDefaults() {
            // Given - populate form
            viewModel.setClientName("Acme Corp");
            viewModel.setDescription("Website design");
            viewModel.setAmount("1000.00");
            viewModel.setReference("INV-001");

            // When
            viewModel.resetForm();

            // Then
            assertThat(viewModel.getDate()).isEqualTo(LocalDate.now());
            assertThat(viewModel.getClientName()).isEmpty();
            assertThat(viewModel.getDescription()).isEmpty();
            assertThat(viewModel.getAmount()).isEmpty();
            assertThat(viewModel.getCategory()).isEqualTo(IncomeCategory.SALES);
            assertThat(viewModel.getStatus()).isEqualTo(IncomeStatus.PAID);
            assertThat(viewModel.getReference()).isEmpty();
        }

        @Test
        @DisplayName("should clear edit mode")
        void shouldClearEditMode() {
            // Given
            Income existingIncome = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("2500.00"),
                "Website redesign",
                IncomeCategory.SALES,
                "INV-001",
                null,
                null,
                null,
                null
            );
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);

            // When
            viewModel.resetForm();

            // Then
            assertThat(viewModel.isEditMode()).isFalse();
        }

        @Test
        @DisplayName("should clear errors")
        void shouldClearErrors() {
            // Given - trigger validation errors
            viewModel.setClientName("");
            viewModel.validate();

            // When
            viewModel.resetForm();

            // Then
            assertThat(viewModel.getDateError()).isNull();
            assertThat(viewModel.getClientNameError()).isNull();
            assertThat(viewModel.getDescriptionError()).isNull();
            assertThat(viewModel.getAmountError()).isNull();
        }

        @Test
        @DisplayName("should clear dirty flag")
        void shouldClearDirtyFlag() {
            // Given
            viewModel.setClientName("Test");

            // When
            viewModel.resetForm();

            // Then
            assertThat(viewModel.isDirty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Dialog Title")
    class DialogTitle {

        @Test
        @DisplayName("should return 'Add Income' for add mode")
        void shouldReturnAddIncomeForAddMode() {
            assertThat(viewModel.getDialogTitle()).isEqualTo("Add Income");
        }

        @Test
        @DisplayName("should return 'Edit Income' for edit mode")
        void shouldReturnEditIncomeForEditMode() {
            // Given
            Income existingIncome = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("2500.00"),
                "Website redesign",
                IncomeCategory.SALES,
                "INV-001",
                null,
                null,
                null,
                null
            );

            // When
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);

            // Then
            assertThat(viewModel.getDialogTitle()).isEqualTo("Edit Income");
        }
    }

    @Nested
    @DisplayName("Save Button Text")
    class SaveButtonText {

        @Test
        @DisplayName("should return 'Save Income' for add mode")
        void shouldReturnSaveIncomeForAddMode() {
            assertThat(viewModel.getSaveButtonText()).isEqualTo("Save Income");
        }

        @Test
        @DisplayName("should return 'Save Changes' for edit mode")
        void shouldReturnSaveChangesForEditMode() {
            // Given
            Income existingIncome = new Income(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("2500.00"),
                "Website redesign",
                IncomeCategory.SALES,
                "INV-001",
                null,
                null,
                null,
                null
            );

            // When
            viewModel.loadIncome(existingIncome, "Acme Corp", IncomeStatus.PAID);

            // Then
            assertThat(viewModel.getSaveButtonText()).isEqualTo("Save Changes");
        }
    }
}
