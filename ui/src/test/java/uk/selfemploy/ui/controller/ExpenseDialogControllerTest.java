package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.ui.viewmodel.ExpenseDialogViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpenseDialogController.
 * Tests the controller logic for add/edit expense dialog.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseDialogController")
class ExpenseDialogControllerTest {

    @Mock
    private ExpenseService expenseService;

    private ExpenseDialogViewModel viewModel;
    private TaxYear taxYear;
    private UUID businessId;

    @BeforeEach
    void setUp() {
        businessId = UUID.randomUUID();
        taxYear = TaxYear.of(2025);
        viewModel = new ExpenseDialogViewModel(expenseService);
        viewModel.setBusinessId(businessId);
        viewModel.setTaxYear(taxYear);
    }

    @Nested
    @DisplayName("Add Mode")
    class AddMode {

        @Test
        @DisplayName("should initialize in add mode with defaults")
        void shouldInitializeInAddMode() {
            assertThat(viewModel.isEditMode()).isFalse();
            assertThat(viewModel.getDialogTitle()).isEqualTo("Add Expense");
            assertThat(viewModel.getSaveButtonText()).isEqualTo("Save Expense");
        }

        @Test
        @DisplayName("should have date set to today")
        void shouldHaveDateSetToToday() {
            assertThat(viewModel.getDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("should have deductible enabled and checked")
        void shouldHaveDeductibleEnabled() {
            assertThat(viewModel.isDeductibleEnabled()).isTrue();
            assertThat(viewModel.isDeductible()).isTrue();
        }

        @Test
        @DisplayName("should hide delete button")
        void shouldHideDeleteButton() {
            assertThat(viewModel.isDeleteVisible()).isFalse();
        }

        @Test
        @DisplayName("should have form invalid initially")
        void shouldHaveFormInvalid() {
            assertThat(viewModel.isFormValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edit Mode")
    class EditMode {

        private Expense existingExpense;

        @BeforeEach
        void setUp() {
            existingExpense = new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("54.99"),
                "Adobe Creative Cloud",
                ExpenseCategory.OFFICE_COSTS,
                null,
                "Monthly subscription"
            );
        }

        @Test
        @DisplayName("should initialize in edit mode")
        void shouldInitializeInEditMode() {
            viewModel.loadExpense(existingExpense);

            assertThat(viewModel.isEditMode()).isTrue();
            assertThat(viewModel.getDialogTitle()).isEqualTo("Edit Expense");
            assertThat(viewModel.getSaveButtonText()).isEqualTo("Save Changes");
        }

        @Test
        @DisplayName("should populate all fields from expense")
        void shouldPopulateAllFields() {
            viewModel.loadExpense(existingExpense);

            assertThat(viewModel.getDate()).isEqualTo(existingExpense.date());
            assertThat(viewModel.getDescription()).isEqualTo(existingExpense.description());
            assertThat(viewModel.getAmount()).isEqualTo("54.99");
            assertThat(viewModel.getCategory()).isEqualTo(existingExpense.category());
            assertThat(viewModel.getNotes()).isEqualTo(existingExpense.notes());
        }

        @Test
        @DisplayName("should show delete button")
        void shouldShowDeleteButton() {
            viewModel.loadExpense(existingExpense);

            assertThat(viewModel.isDeleteVisible()).isTrue();
        }

        @Test
        @DisplayName("should have form valid when fields populated")
        void shouldHaveFormValid() {
            viewModel.loadExpense(existingExpense);

            assertThat(viewModel.isFormValid()).isTrue();
        }

        @Test
        @DisplayName("should not be dirty initially")
        void shouldNotBeDirtyInitially() {
            viewModel.loadExpense(existingExpense);

            assertThat(viewModel.isDirty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Category Selection")
    class CategorySelection {

        @Test
        @DisplayName("should update category help when category selected")
        void shouldUpdateCategoryHelp() {
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.getCategoryHelpTitle()).isNotEmpty();
            assertThat(viewModel.getCategoryHelpText()).isNotEmpty();
            assertThat(viewModel.getCategoryHelpExamples()).isNotEmpty();
        }

        @Test
        @DisplayName("should disable deductible for non-allowable category")
        void shouldDisableDeductibleForNonAllowable() {
            viewModel.setCategory(ExpenseCategory.DEPRECIATION);

            assertThat(viewModel.isDeductibleEnabled()).isFalse();
            assertThat(viewModel.isDeductible()).isFalse();
        }

        @Test
        @DisplayName("should show warning for non-allowable category")
        void shouldShowWarningForNonAllowable() {
            viewModel.setCategory(ExpenseCategory.DEPRECIATION);

            assertThat(viewModel.isCategoryWarning()).isTrue();
        }

        @Test
        @DisplayName("should re-enable deductible when switching to allowable category")
        void shouldReEnableDeductible() {
            viewModel.setCategory(ExpenseCategory.DEPRECIATION);
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isDeductibleEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Form Validation")
    class FormValidation {

        @Test
        @DisplayName("should validate form when all fields filled")
        void shouldValidateCompleteForm() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isTrue();
        }

        @Test
        @DisplayName("should show date error for future date")
        void shouldShowDateErrorForFutureDate() {
            viewModel.setDate(LocalDate.now().plusDays(1));

            assertThat(viewModel.getDateError()).isNotEmpty();
        }

        @Test
        @DisplayName("should show date error for date outside tax year")
        void shouldShowDateErrorForOutsideTaxYear() {
            viewModel.setDate(taxYear.startDate().minusDays(1));

            assertThat(viewModel.getDateError()).isNotEmpty();
        }

        @Test
        @DisplayName("should show description error when empty")
        void shouldShowDescriptionErrorWhenEmpty() {
            viewModel.setDescription("");
            // Trigger validation by checking form validity
            viewModel.isFormValid();

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should show amount error for invalid amount")
        void shouldShowAmountErrorForInvalidAmount() {
            viewModel.setAmount("abc");

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should show amount error for zero amount")
        void shouldShowAmountErrorForZeroAmount() {
            viewModel.setAmount("0.00");

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should invalidate form when category cleared")
        void shouldInvalidateWhenCategoryCleared() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);
            assertThat(viewModel.isFormValid()).isTrue();

            viewModel.setCategory(null);

            assertThat(viewModel.isFormValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Dirty Tracking")
    class DirtyTracking {

        @Test
        @DisplayName("should mark dirty when description changes")
        void shouldMarkDirtyWhenDescriptionChanges() {
            viewModel.setDescription("New description");

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should mark dirty when amount changes")
        void shouldMarkDirtyWhenAmountChanges() {
            viewModel.setAmount("100.00");

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should mark dirty when category changes")
        void shouldMarkDirtyWhenCategoryChanges() {
            viewModel.setCategory(ExpenseCategory.TRAVEL);

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should not mark dirty when loading expense")
        void shouldNotMarkDirtyWhenLoading() {
            Expense expense = new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("50.00"),
                "Test",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null
            );

            viewModel.loadExpense(expense);

            assertThat(viewModel.isDirty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Save Operation")
    class SaveOperation {

        private void setupCreateMock() {
            when(expenseService.create(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    UUID bId = invocation.getArgument(0);
                    LocalDate date = invocation.getArgument(1);
                    BigDecimal amount = invocation.getArgument(2);
                    String desc = invocation.getArgument(3);
                    ExpenseCategory cat = invocation.getArgument(4);
                    String receipt = invocation.getArgument(5);
                    String notes = invocation.getArgument(6);
                    return Expense.create(bId, date, amount, desc, cat, receipt, notes);
                });
        }

        @Test
        @DisplayName("should call service create in add mode")
        void shouldCallServiceCreateInAddMode() {
            setupCreateMock();
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            viewModel.save();

            verify(expenseService).create(
                eq(businessId),
                eq(LocalDate.of(2025, 6, 15)),
                eq(new BigDecimal("50.00")),
                eq("Test expense"),
                eq(ExpenseCategory.OFFICE_COSTS),
                isNull(),
                isNull()
            );
        }

        @Test
        @DisplayName("should invoke onSave callback")
        void shouldInvokeOnSaveCallback() {
            setupCreateMock();
            AtomicReference<Expense> saved = new AtomicReference<>();
            viewModel.setOnSave(saved::set);

            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            viewModel.save();

            assertThat(saved.get()).isNotNull();
        }

        @Test
        @DisplayName("should not save when form invalid")
        void shouldNotSaveWhenFormInvalid() {
            // No mock setup needed - form is invalid so service shouldn't be called
            viewModel.setDescription(""); // Invalid

            viewModel.save();

            verify(expenseService, never()).create(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Update Operation")
    class UpdateOperation {

        private Expense existingExpense;

        @BeforeEach
        void setUp() {
            existingExpense = new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("54.99"),
                "Adobe Creative Cloud",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null
            );

            when(expenseService.update(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    LocalDate date = invocation.getArgument(1);
                    BigDecimal amount = invocation.getArgument(2);
                    String desc = invocation.getArgument(3);
                    ExpenseCategory cat = invocation.getArgument(4);
                    String receipt = invocation.getArgument(5);
                    String notes = invocation.getArgument(6);
                    return new Expense(id, businessId, date, amount, desc, cat, receipt, notes);
                });
        }

        @Test
        @DisplayName("should call service update in edit mode")
        void shouldCallServiceUpdateInEditMode() {
            viewModel.loadExpense(existingExpense);
            viewModel.setDescription("Updated description");

            viewModel.save();

            verify(expenseService).update(
                eq(existingExpense.id()),
                any(),
                any(),
                eq("Updated description"),
                any(),
                any(),
                any()
            );
        }
    }

    @Nested
    @DisplayName("Delete Operation")
    class DeleteOperation {

        private Expense createExistingExpense() {
            return new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("54.99"),
                "Adobe Creative Cloud",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null
            );
        }

        @Test
        @DisplayName("should call service delete")
        void shouldCallServiceDelete() {
            Expense existingExpense = createExistingExpense();
            when(expenseService.delete(any())).thenReturn(true);
            viewModel.loadExpense(existingExpense);

            viewModel.delete();

            verify(expenseService).delete(existingExpense.id());
        }

        @Test
        @DisplayName("should invoke onDelete callback")
        void shouldInvokeOnDeleteCallback() {
            Expense existingExpense = createExistingExpense();
            when(expenseService.delete(any())).thenReturn(true);
            java.util.concurrent.atomic.AtomicBoolean deleted = new java.util.concurrent.atomic.AtomicBoolean(false);
            viewModel.setOnDelete(() -> deleted.set(true));
            viewModel.loadExpense(existingExpense);

            viewModel.delete();

            assertThat(deleted.get()).isTrue();
        }

        @Test
        @DisplayName("should not delete in add mode")
        void shouldNotDeleteInAddMode() {
            // No mock setup needed - in add mode, service shouldn't be called
            viewModel.delete();

            verify(expenseService, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Reset Form")
    class ResetForm {

        @Test
        @DisplayName("should clear all fields")
        void shouldClearAllFields() {
            viewModel.setDate(LocalDate.of(2025, 7, 1));
            viewModel.setDescription("Test");
            viewModel.setAmount("100.00");
            viewModel.setCategory(ExpenseCategory.TRAVEL);
            viewModel.setNotes("Notes");

            viewModel.resetForm();

            assertThat(viewModel.getDate()).isEqualTo(LocalDate.now());
            assertThat(viewModel.getDescription()).isEmpty();
            assertThat(viewModel.getAmount()).isEmpty();
            assertThat(viewModel.getCategory()).isNull();
            assertThat(viewModel.getNotes()).isEmpty();
        }

        @Test
        @DisplayName("should exit edit mode")
        void shouldExitEditMode() {
            Expense expense = new Expense(
                UUID.randomUUID(),
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("50.00"),
                "Test",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null
            );
            viewModel.loadExpense(expense);

            viewModel.resetForm();

            assertThat(viewModel.isEditMode()).isFalse();
        }

        @Test
        @DisplayName("should clear dirty flag")
        void shouldClearDirtyFlag() {
            viewModel.setDescription("Test");
            assertThat(viewModel.isDirty()).isTrue();

            viewModel.resetForm();

            assertThat(viewModel.isDirty()).isFalse();
        }
    }

    @Nested
    @DisplayName("CIS Business")
    class CISBusiness {

        @Test
        @DisplayName("should hide CIS categories when not CIS business")
        void shouldHideCisCategoriesWhenNotCisBusiness() {
            viewModel.setCisBusiness(false);

            assertThat(viewModel.getAvailableCategories())
                .doesNotContain(ExpenseCategory.SUBCONTRACTOR_COSTS);
        }

        @Test
        @DisplayName("should show CIS categories when CIS business")
        void shouldShowCisCategoriesWhenCisBusiness() {
            viewModel.setCisBusiness(true);

            assertThat(viewModel.getAvailableCategories())
                .contains(ExpenseCategory.SUBCONTRACTOR_COSTS);
        }
    }
}
