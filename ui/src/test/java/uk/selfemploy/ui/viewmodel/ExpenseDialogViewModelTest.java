package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD tests for ExpenseDialogViewModel.
 * Tests the ViewModel logic for add/edit expense dialog.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseDialogViewModel")
class ExpenseDialogViewModelTest {

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
    @DisplayName("Add Mode Initial State")
    class AddModeInitialState {

        @Test
        @DisplayName("should not be in edit mode")
        void shouldNotBeInEditMode() {
            assertThat(viewModel.isEditMode()).isFalse();
        }

        @Test
        @DisplayName("should default date to today")
        void shouldDefaultDateToToday() {
            assertThat(viewModel.getDate()).isEqualTo(LocalDate.now());
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
        @DisplayName("should have no category selected")
        void shouldHaveNoCategorySelected() {
            assertThat(viewModel.getCategory()).isNull();
        }

        @Test
        @DisplayName("should have deductible checked by default")
        void shouldHaveDeductibleChecked() {
            assertThat(viewModel.isDeductible()).isTrue();
        }

        @Test
        @DisplayName("should have empty notes")
        void shouldHaveEmptyNotes() {
            assertThat(viewModel.getNotes()).isEmpty();
        }

        @Test
        @DisplayName("should have form invalid initially")
        void shouldHaveFormInvalid() {
            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should not be dirty initially")
        void shouldNotBeDirty() {
            assertThat(viewModel.isDirty()).isFalse();
        }

        @Test
        @DisplayName("should have save button text as 'Save Expense'")
        void shouldHaveSaveButtonText() {
            assertThat(viewModel.getSaveButtonText()).isEqualTo("Save Expense");
        }

        @Test
        @DisplayName("should have dialog title as 'Add Expense'")
        void shouldHaveDialogTitle() {
            assertThat(viewModel.getDialogTitle()).isEqualTo("Add Expense");
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
        @DisplayName("should be in edit mode when expense loaded")
        void shouldBeInEditMode() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.isEditMode()).isTrue();
        }

        @Test
        @DisplayName("should populate date from expense")
        void shouldPopulateDate() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.getDate()).isEqualTo(existingExpense.date());
        }

        @Test
        @DisplayName("should populate description from expense")
        void shouldPopulateDescription() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.getDescription()).isEqualTo(existingExpense.description());
        }

        @Test
        @DisplayName("should populate amount from expense")
        void shouldPopulateAmount() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.getAmount()).isEqualTo("54.99");
        }

        @Test
        @DisplayName("should populate category from expense")
        void shouldPopulateCategory() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.getCategory()).isEqualTo(existingExpense.category());
        }

        @Test
        @DisplayName("should populate deductible based on category")
        void shouldPopulateDeductible() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.isDeductible()).isTrue();
        }

        @Test
        @DisplayName("should populate notes from expense")
        void shouldPopulateNotes() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.getNotes()).isEqualTo(existingExpense.notes());
        }

        @Test
        @DisplayName("should have save button text as 'Save Changes'")
        void shouldHaveSaveChangesText() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.getSaveButtonText()).isEqualTo("Save Changes");
        }

        @Test
        @DisplayName("should have dialog title as 'Edit Expense'")
        void shouldHaveEditTitle() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.getDialogTitle()).isEqualTo("Edit Expense");
        }

        @Test
        @DisplayName("should show delete button in edit mode")
        void shouldShowDeleteButton() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.isDeleteVisible()).isTrue();
        }

        @Test
        @DisplayName("should not be dirty initially in edit mode")
        void shouldNotBeDirtyInitially() {
            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.isDirty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Form Validation")
    class FormValidation {

        @Test
        @DisplayName("should be valid when all required fields are filled")
        void shouldBeValidWithAllFields() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isTrue();
        }

        @Test
        @DisplayName("should be invalid when date is null")
        void shouldBeInvalidWithoutDate() {
            viewModel.setDate(null);
            viewModel.setDescription("Test expense");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when description is empty")
        void shouldBeInvalidWithoutDescription() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when description is only whitespace")
        void shouldBeInvalidWithWhitespaceDescription() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("   ");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when amount is empty")
        void shouldBeInvalidWithoutAmount() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when amount is not numeric")
        void shouldBeInvalidWithNonNumericAmount() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("abc");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when amount is zero")
        void shouldBeInvalidWithZeroAmount() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("0.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when amount is negative")
        void shouldBeInvalidWithNegativeAmount() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("-50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when category is null")
        void shouldBeInvalidWithoutCategory() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("50.00");
            viewModel.setCategory(null);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should be invalid when description exceeds max length")
        void shouldBeInvalidWithTooLongDescription() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("a".repeat(201));
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isFalse();
        }

        @Test
        @DisplayName("should be valid with description at max length")
        void shouldBeValidWithMaxLengthDescription() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("a".repeat(200));
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isFormValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Date Validation")
    class DateValidation {

        @Test
        @DisplayName("should have date error when date is in the future")
        void shouldHaveDateErrorForFutureDate() {
            viewModel.setDate(LocalDate.now().plusDays(1));

            assertThat(viewModel.getDateError()).isNotEmpty();
        }

        @Test
        @DisplayName("should have date error when date is outside tax year")
        void shouldHaveDateErrorForOutsideTaxYear() {
            // Date before tax year start
            viewModel.setDate(taxYear.startDate().minusDays(1));

            assertThat(viewModel.getDateError()).isNotEmpty();
        }

        @Test
        @DisplayName("should have no date error for valid date")
        void shouldHaveNoDateErrorForValidDate() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));

            assertThat(viewModel.getDateError()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Category Change Behavior")
    class CategoryChangeBehavior {

        @Test
        @DisplayName("should disable deductible when non-allowable category selected")
        void shouldDisableDeductibleForNonAllowable() {
            viewModel.setCategory(ExpenseCategory.DEPRECIATION);

            assertThat(viewModel.isDeductibleEnabled()).isFalse();
            assertThat(viewModel.isDeductible()).isFalse();
        }

        @Test
        @DisplayName("should enable deductible when allowable category selected")
        void shouldEnableDeductibleForAllowable() {
            viewModel.setCategory(ExpenseCategory.DEPRECIATION);
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isDeductibleEnabled()).isTrue();
        }

        @Test
        @DisplayName("should update category help text when category changes")
        void shouldUpdateCategoryHelpText() {
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.getCategoryHelpText()).isNotEmpty();
        }

        @Test
        @DisplayName("should show warning mode for non-allowable category")
        void shouldShowWarningForNonAllowable() {
            viewModel.setCategory(ExpenseCategory.DEPRECIATION);

            assertThat(viewModel.isCategoryWarning()).isTrue();
        }

        @Test
        @DisplayName("should not show warning for allowable category")
        void shouldNotShowWarningForAllowable() {
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            assertThat(viewModel.isCategoryWarning()).isFalse();
        }

        @Test
        @DisplayName("should also disable deductible for BUSINESS_ENTERTAINMENT")
        void shouldDisableDeductibleForBusinessEntertainment() {
            viewModel.setCategory(ExpenseCategory.BUSINESS_ENTERTAINMENT);

            assertThat(viewModel.isDeductibleEnabled()).isFalse();
            assertThat(viewModel.isDeductible()).isFalse();
        }
    }

    @Nested
    @DisplayName("Dirty Tracking")
    class DirtyTracking {

        @Test
        @DisplayName("should become dirty when description changes")
        void shouldBeDirtyWhenDescriptionChanges() {
            viewModel.setDescription("New description");

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should become dirty when amount changes")
        void shouldBeDirtyWhenAmountChanges() {
            viewModel.setAmount("100.00");

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should become dirty when category changes")
        void shouldBeDirtyWhenCategoryChanges() {
            viewModel.setCategory(ExpenseCategory.TRAVEL);

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should become dirty when date changes")
        void shouldBeDirtyWhenDateChanges() {
            viewModel.setDate(LocalDate.of(2025, 7, 1));

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should become dirty when notes changes")
        void shouldBeDirtyWhenNotesChanges() {
            viewModel.setNotes("Some notes");

            assertThat(viewModel.isDirty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Save Expense - Add Mode")
    class SaveExpenseAddMode {

        @BeforeEach
        void setUp() {
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
        @DisplayName("should call service create when saving new expense")
        void shouldCallServiceCreate() {
            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);
            viewModel.setNotes("Test notes");

            viewModel.save();

            verify(expenseService).create(
                eq(businessId),
                eq(LocalDate.of(2025, 6, 15)),
                eq(new BigDecimal("50.00")),
                eq("Test expense"),
                eq(ExpenseCategory.OFFICE_COSTS),
                isNull(),
                eq("Test notes")
            );
        }

        @Test
        @DisplayName("should invoke onSave callback with created expense")
        void shouldInvokeOnSaveCallback() {
            AtomicReference<Expense> savedExpense = new AtomicReference<>();
            viewModel.setOnSave(savedExpense::set);

            viewModel.setDate(LocalDate.of(2025, 6, 15));
            viewModel.setDescription("Test expense");
            viewModel.setAmount("50.00");
            viewModel.setCategory(ExpenseCategory.OFFICE_COSTS);

            viewModel.save();

            assertThat(savedExpense.get()).isNotNull();
            assertThat(savedExpense.get().description()).isEqualTo("Test expense");
        }
    }

    @Nested
    @DisplayName("Save Expense - Edit Mode")
    class SaveExpenseEditMode {

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
        @DisplayName("should call service update when saving existing expense")
        void shouldCallServiceUpdate() {
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

        @Test
        @DisplayName("should invoke onSave callback with updated expense")
        void shouldInvokeOnSaveCallback() {
            AtomicReference<Expense> savedExpense = new AtomicReference<>();
            viewModel.setOnSave(savedExpense::set);
            viewModel.loadExpense(existingExpense);
            viewModel.setDescription("Updated description");

            viewModel.save();

            assertThat(savedExpense.get()).isNotNull();
            assertThat(savedExpense.get().description()).isEqualTo("Updated description");
        }
    }

    @Nested
    @DisplayName("Delete Expense")
    class DeleteExpense {

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
            when(expenseService.delete(any())).thenReturn(true);
        }

        @Test
        @DisplayName("should call service delete")
        void shouldCallServiceDelete() {
            viewModel.loadExpense(existingExpense);

            viewModel.delete();

            verify(expenseService).delete(existingExpense.id());
        }

        @Test
        @DisplayName("should invoke onDelete callback")
        void shouldInvokeOnDeleteCallback() {
            AtomicBoolean deleted = new AtomicBoolean(false);
            viewModel.setOnDelete(() -> deleted.set(true));
            viewModel.loadExpense(existingExpense);

            viewModel.delete();

            assertThat(deleted.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("Reset Form")
    class ResetForm {

        @Test
        @DisplayName("should reset all fields to defaults")
        void shouldResetFields() {
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
            assertThat(viewModel.isDeductible()).isTrue();
            assertThat(viewModel.isDirty()).isFalse();
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
    }

    @Nested
    @DisplayName("Available Categories")
    class AvailableCategories {

        @Test
        @DisplayName("should return non-CIS categories when not CIS business")
        void shouldReturnNonCisCategories() {
            viewModel.setCisBusiness(false);

            assertThat(viewModel.getAvailableCategories())
                .doesNotContain(ExpenseCategory.SUBCONTRACTOR_COSTS);
        }

        @Test
        @DisplayName("should include CIS categories when CIS business")
        void shouldIncludeCisCategories() {
            viewModel.setCisBusiness(true);

            assertThat(viewModel.getAvailableCategories())
                .contains(ExpenseCategory.SUBCONTRACTOR_COSTS);
        }
    }

    @Nested
    @DisplayName("Category Help Content")
    class CategoryHelpContent {

        @Test
        @DisplayName("should provide help title for each category")
        void shouldProvideHelpTitle() {
            for (ExpenseCategory category : ExpenseCategory.values()) {
                viewModel.setCategory(category);
                assertThat(viewModel.getCategoryHelpTitle()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("should provide help text for each category")
        void shouldProvideHelpText() {
            for (ExpenseCategory category : ExpenseCategory.values()) {
                viewModel.setCategory(category);
                assertThat(viewModel.getCategoryHelpText()).isNotEmpty();
            }
        }

        @Test
        @DisplayName("should provide examples for each category")
        void shouldProvideExamples() {
            for (ExpenseCategory category : ExpenseCategory.values()) {
                viewModel.setCategory(category);
                assertThat(viewModel.getCategoryHelpExamples()).isNotEmpty();
            }
        }
    }
}
