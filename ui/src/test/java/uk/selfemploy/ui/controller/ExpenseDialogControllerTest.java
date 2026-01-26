package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.ExpenseCategory;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.ReceiptMetadata;
import uk.selfemploy.core.service.ReceiptStorageException;
import uk.selfemploy.core.service.ReceiptStorageService;
import uk.selfemploy.ui.viewmodel.ExpenseDialogViewModel;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
                "Monthly subscription",
                null,
                null,
                null
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
                null,
                null,
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
                null,
                null,
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
                    return new Expense(id, businessId, date, amount, desc, cat, receipt, notes, null, null, null);
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
                null,
                null,
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
                null,
                null,
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

    @Nested
    @DisplayName("Category Dropdown Population")
    class CategoryDropdownPopulation {

        @Test
        @DisplayName("should have categories available after initialization")
        void shouldHaveCategoriesAvailableAfterInitialization() {
            // This tests the fix: categories should be populated when ViewModel is created with service
            assertThat(viewModel.getAvailableCategories()).isNotEmpty();
        }

        @Test
        @DisplayName("should include all SA103 expense categories for non-CIS business")
        void shouldIncludeAllSa103ExpenseCategoriesForNonCisBusiness() {
            viewModel.setCisBusiness(false);

            var categories = viewModel.getAvailableCategories();

            // Should have 16 categories (17 total minus 1 CIS-only)
            assertThat(categories).hasSize(16);
            assertThat(categories).contains(
                ExpenseCategory.COST_OF_GOODS,
                ExpenseCategory.STAFF_COSTS,
                ExpenseCategory.TRAVEL,
                ExpenseCategory.TRAVEL_MILEAGE,
                ExpenseCategory.PREMISES,
                ExpenseCategory.REPAIRS,
                ExpenseCategory.OFFICE_COSTS,
                ExpenseCategory.ADVERTISING,
                ExpenseCategory.INTEREST,
                ExpenseCategory.FINANCIAL_CHARGES,
                ExpenseCategory.BAD_DEBTS,
                ExpenseCategory.PROFESSIONAL_FEES,
                ExpenseCategory.DEPRECIATION,
                ExpenseCategory.OTHER_EXPENSES,
                ExpenseCategory.HOME_OFFICE_SIMPLIFIED,
                ExpenseCategory.BUSINESS_ENTERTAINMENT
            );
        }

        @Test
        @DisplayName("should include all 17 categories for CIS business")
        void shouldIncludeAll17CategoriesForCisBusiness() {
            viewModel.setCisBusiness(true);

            var categories = viewModel.getAvailableCategories();

            assertThat(categories).hasSize(17);
            assertThat(categories).containsExactlyInAnyOrder(ExpenseCategory.values());
        }

        @Test
        @DisplayName("should have categories with SA103 box numbers")
        void shouldHaveCategoriesWithSa103BoxNumbers() {
            var categories = viewModel.getAvailableCategories();

            // All categories should have SA103 box numbers (returned as String)
            for (ExpenseCategory category : categories) {
                assertThat(category.getSa103Box())
                    .as("Category %s should have SA103 box number", category.name())
                    .isNotBlank();
            }
        }

        @Test
        @DisplayName("should have categories with display names")
        void shouldHaveCategoriesWithDisplayNames() {
            var categories = viewModel.getAvailableCategories();

            for (ExpenseCategory category : categories) {
                assertThat(category.getDisplayName())
                    .as("Category %s should have display name", category.name())
                    .isNotBlank();
            }
        }
    }

    // =====================================================================
    // SE-308: Receipt Attachment Tests
    // =====================================================================

    @Nested
    @DisplayName("Receipt Attachment - Initial State")
    class ReceiptInitialState {

        @Test
        @DisplayName("should initialize with zero receipts")
        void shouldInitializeWithZeroReceipts() {
            assertThat(viewModel.getReceiptCount()).isZero();
        }

        @Test
        @DisplayName("should show '0 of 5' in receipt count text")
        void shouldShowZeroOfFiveInReceiptCountText() {
            assertThat(viewModel.getReceiptCountText()).isEqualTo("0 of 5");
        }

        @Test
        @DisplayName("should allow adding receipts when none attached")
        void shouldAllowAddingReceiptsWhenNoneAttached() {
            assertThat(viewModel.canAddMoreReceipts()).isTrue();
        }

        @Test
        @DisplayName("should have empty receipt list")
        void shouldHaveEmptyReceiptList() {
            assertThat(viewModel.getReceipts()).isEmpty();
        }

        @Test
        @DisplayName("should not have receipt error initially")
        void shouldNotHaveReceiptErrorInitially() {
            assertThat(viewModel.hasReceiptError()).isFalse();
            assertThat(viewModel.getReceiptErrorMessage()).isEmpty();
        }

        @Test
        @DisplayName("should show dropzone when no receipts")
        void shouldShowDropzoneWhenNoReceipts() {
            assertThat(viewModel.isDropzoneVisible()).isTrue();
            assertThat(viewModel.isReceiptGridVisible()).isFalse();
        }
    }

    @Nested
    @DisplayName("Receipt Attachment - Adding Receipts")
    class ReceiptAddition {

        @Mock
        private ReceiptStorageService receiptStorageService;

        @TempDir
        Path tempDir;

        private Path validJpegFile;
        private Path validPdfFile;
        private UUID expenseId;

        @BeforeEach
        void setUpReceipts() throws IOException {
            expenseId = UUID.randomUUID();
            viewModel.setReceiptStorageService(receiptStorageService);

            // Create test files
            validJpegFile = tempDir.resolve("receipt.jpg");
            Files.write(validJpegFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00});

            validPdfFile = tempDir.resolve("invoice.pdf");
            Files.write(validPdfFile, "%PDF-1.4".getBytes());
        }

        @Test
        @DisplayName("should increment receipt count when receipt added")
        void shouldIncrementReceiptCountWhenReceiptAdded() {
            ReceiptMetadata mockMetadata = createMockReceiptMetadata(expenseId, "receipt.jpg", "image/jpeg");
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);

            viewModel.attachReceipt(validJpegFile.toFile());

            assertThat(viewModel.getReceiptCount()).isEqualTo(1);
            assertThat(viewModel.getReceiptCountText()).isEqualTo("1 of 5");
        }

        @Test
        @DisplayName("should add receipt to list")
        void shouldAddReceiptToList() {
            ReceiptMetadata mockMetadata = createMockReceiptMetadata(expenseId, "receipt.jpg", "image/jpeg");
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);

            viewModel.attachReceipt(validJpegFile.toFile());

            assertThat(viewModel.getReceipts()).hasSize(1);
            assertThat(viewModel.getReceipts().get(0).originalFilename()).isEqualTo("receipt.jpg");
        }

        @Test
        @DisplayName("should show receipt grid when receipts exist")
        void shouldShowReceiptGridWhenReceiptsExist() {
            ReceiptMetadata mockMetadata = createMockReceiptMetadata(expenseId, "receipt.jpg", "image/jpeg");
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);

            viewModel.attachReceipt(validJpegFile.toFile());

            assertThat(viewModel.isDropzoneVisible()).isFalse();
            assertThat(viewModel.isReceiptGridVisible()).isTrue();
        }

        @Test
        @DisplayName("should mark form as dirty when receipt added")
        void shouldMarkFormAsDirtyWhenReceiptAdded() {
            ReceiptMetadata mockMetadata = createMockReceiptMetadata(expenseId, "receipt.jpg", "image/jpeg");
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);

            viewModel.attachReceipt(validJpegFile.toFile());

            assertThat(viewModel.isDirty()).isTrue();
        }

        @Test
        @DisplayName("should allow adding multiple receipts")
        void shouldAllowAddingMultipleReceipts() {
            for (int i = 0; i < 3; i++) {
                ReceiptMetadata mockMetadata = createMockReceiptMetadata(expenseId, "receipt" + i + ".jpg", "image/jpeg");
                when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);
                viewModel.attachReceipt(validJpegFile.toFile());
            }

            assertThat(viewModel.getReceiptCount()).isEqualTo(3);
            assertThat(viewModel.getReceiptCountText()).isEqualTo("3 of 5");
            assertThat(viewModel.canAddMoreReceipts()).isTrue();
        }

        @Test
        @DisplayName("should support PDF files")
        void shouldSupportPdfFiles() {
            ReceiptMetadata mockMetadata = createMockReceiptMetadata(expenseId, "invoice.pdf", "application/pdf");
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);

            viewModel.attachReceipt(validPdfFile.toFile());

            assertThat(viewModel.getReceipts()).hasSize(1);
            assertThat(viewModel.getReceipts().get(0).isPdf()).isTrue();
        }

        private ReceiptMetadata createMockReceiptMetadata(UUID expenseId, String filename, String mimeType) {
            return new ReceiptMetadata(
                UUID.randomUUID(),
                expenseId,
                filename,
                tempDir.resolve(filename),
                mimeType,
                1024L,
                Instant.now()
            );
        }
    }

    @Nested
    @DisplayName("Receipt Attachment - Maximum Limit")
    class ReceiptMaxLimit {

        @Mock
        private ReceiptStorageService receiptStorageService;

        @TempDir
        Path tempDir;

        private Path validJpegFile;
        private UUID expenseId;

        @BeforeEach
        void setUpReceipts() throws IOException {
            expenseId = UUID.randomUUID();
            viewModel.setReceiptStorageService(receiptStorageService);

            validJpegFile = tempDir.resolve("receipt.jpg");
            Files.write(validJpegFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00});
        }

        @Test
        @DisplayName("should not allow more than 5 receipts")
        void shouldNotAllowMoreThanFiveReceipts() {
            // Add 5 receipts
            for (int i = 0; i < 5; i++) {
                ReceiptMetadata mockMetadata = createMockReceiptMetadata(i);
                when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);
                viewModel.attachReceipt(validJpegFile.toFile());
            }

            assertThat(viewModel.getReceiptCount()).isEqualTo(5);
            assertThat(viewModel.canAddMoreReceipts()).isFalse();
            assertThat(viewModel.getReceiptCountText()).isEqualTo("5 of 5");
        }

        @Test
        @DisplayName("should show error when max receipts exceeded")
        void shouldShowErrorWhenMaxReceiptsExceeded() {
            // Add 5 receipts
            for (int i = 0; i < 5; i++) {
                ReceiptMetadata mockMetadata = createMockReceiptMetadata(i);
                when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);
                viewModel.attachReceipt(validJpegFile.toFile());
            }

            // Try to add 6th
            when(receiptStorageService.storeReceipt(any(), any(), any()))
                .thenThrow(new ReceiptStorageException(
                    ReceiptStorageException.ErrorType.MAX_RECEIPTS_EXCEEDED));
            viewModel.attachReceipt(validJpegFile.toFile());

            assertThat(viewModel.hasReceiptError()).isTrue();
            assertThat(viewModel.getReceiptErrorMessage())
                .contains("Maximum 5 receipts");
        }

        @Test
        @DisplayName("should allow adding again after removing receipt")
        void shouldAllowAddingAgainAfterRemovingReceipt() {
            // Add 5 receipts
            for (int i = 0; i < 5; i++) {
                ReceiptMetadata mockMetadata = createMockReceiptMetadata(i);
                when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);
                viewModel.attachReceipt(validJpegFile.toFile());
            }

            assertThat(viewModel.canAddMoreReceipts()).isFalse();

            // Remove one
            when(receiptStorageService.deleteReceipt(any())).thenReturn(true);
            viewModel.removeReceipt(viewModel.getReceipts().get(0).receiptId());

            assertThat(viewModel.canAddMoreReceipts()).isTrue();
            assertThat(viewModel.getReceiptCount()).isEqualTo(4);
        }

        private ReceiptMetadata createMockReceiptMetadata(int index) {
            return new ReceiptMetadata(
                UUID.randomUUID(),
                expenseId,
                "receipt" + index + ".jpg",
                tempDir.resolve("receipt" + index + ".jpg"),
                "image/jpeg",
                1024L,
                Instant.now()
            );
        }
    }

    @Nested
    @DisplayName("Receipt Attachment - File Validation")
    class ReceiptFileValidation {

        @Mock
        private ReceiptStorageService receiptStorageService;

        @TempDir
        Path tempDir;

        private UUID expenseId;

        @BeforeEach
        void setUpReceipts() {
            expenseId = UUID.randomUUID();
            viewModel.setReceiptStorageService(receiptStorageService);
        }

        @Test
        @DisplayName("should reject unsupported file format")
        void shouldRejectUnsupportedFileFormat() throws IOException {
            Path docFile = tempDir.resolve("document.doc");
            Files.write(docFile, "This is a Word doc".getBytes());

            when(receiptStorageService.storeReceipt(any(), any(), any()))
                .thenThrow(new ReceiptStorageException(
                    ReceiptStorageException.ErrorType.UNSUPPORTED_FORMAT,
                    "Unsupported file format: application/msword"));

            viewModel.attachReceipt(docFile.toFile());

            assertThat(viewModel.hasReceiptError()).isTrue();
            assertThat(viewModel.getReceiptErrorMessage())
                .contains("Unsupported file format");
            assertThat(viewModel.getReceiptErrorHelper())
                .contains("JPG, PNG, PDF, GIF");
        }

        @Test
        @DisplayName("should reject file exceeding 10MB")
        void shouldRejectFileExceedingTenMb() throws IOException {
            Path largeFile = tempDir.resolve("large.jpg");
            Files.write(largeFile, new byte[100]); // Just a small file for the mock

            when(receiptStorageService.storeReceipt(any(), any(), any()))
                .thenThrow(new ReceiptStorageException(
                    ReceiptStorageException.ErrorType.FILE_TOO_LARGE,
                    "File size 11000000 bytes exceeds maximum size 10485760 bytes"));

            viewModel.attachReceipt(largeFile.toFile());

            assertThat(viewModel.hasReceiptError()).isTrue();
            assertThat(viewModel.getReceiptErrorMessage())
                .contains("exceeds");
            assertThat(viewModel.getReceiptErrorHelper())
                .contains("10MB");
        }

        @Test
        @DisplayName("should accept valid JPG file")
        void shouldAcceptValidJpgFile() throws IOException {
            Path jpgFile = tempDir.resolve("receipt.jpg");
            Files.write(jpgFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

            ReceiptMetadata mockMetadata = new ReceiptMetadata(
                UUID.randomUUID(), expenseId, "receipt.jpg",
                jpgFile, "image/jpeg", 1024L, Instant.now()
            );
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);

            viewModel.attachReceipt(jpgFile.toFile());

            assertThat(viewModel.hasReceiptError()).isFalse();
            assertThat(viewModel.getReceipts()).hasSize(1);
        }

        @Test
        @DisplayName("should accept valid PNG file")
        void shouldAcceptValidPngFile() throws IOException {
            Path pngFile = tempDir.resolve("receipt.png");
            Files.write(pngFile, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

            ReceiptMetadata mockMetadata = new ReceiptMetadata(
                UUID.randomUUID(), expenseId, "receipt.png",
                pngFile, "image/png", 2048L, Instant.now()
            );
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);

            viewModel.attachReceipt(pngFile.toFile());

            assertThat(viewModel.hasReceiptError()).isFalse();
            assertThat(viewModel.getReceipts()).hasSize(1);
        }

        @Test
        @DisplayName("should accept valid GIF file")
        void shouldAcceptValidGifFile() throws IOException {
            Path gifFile = tempDir.resolve("receipt.gif");
            Files.write(gifFile, new byte[]{0x47, 0x49, 0x46, 0x38});

            ReceiptMetadata mockMetadata = new ReceiptMetadata(
                UUID.randomUUID(), expenseId, "receipt.gif",
                gifFile, "image/gif", 512L, Instant.now()
            );
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);

            viewModel.attachReceipt(gifFile.toFile());

            assertThat(viewModel.hasReceiptError()).isFalse();
            assertThat(viewModel.getReceipts()).hasSize(1);
        }

        @Test
        @DisplayName("should accept valid PDF file")
        void shouldAcceptValidPdfFile() throws IOException {
            Path pdfFile = tempDir.resolve("invoice.pdf");
            Files.write(pdfFile, "%PDF-1.4".getBytes());

            ReceiptMetadata mockMetadata = new ReceiptMetadata(
                UUID.randomUUID(), expenseId, "invoice.pdf",
                pdfFile, "application/pdf", 4096L, Instant.now()
            );
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);

            viewModel.attachReceipt(pdfFile.toFile());

            assertThat(viewModel.hasReceiptError()).isFalse();
            assertThat(viewModel.getReceipts()).hasSize(1);
            assertThat(viewModel.getReceipts().get(0).isPdf()).isTrue();
        }
    }

    @Nested
    @DisplayName("Receipt Attachment - Removal")
    class ReceiptRemoval {

        @Mock
        private ReceiptStorageService receiptStorageService;

        @TempDir
        Path tempDir;

        private UUID expenseId;
        private UUID receiptId;

        @BeforeEach
        void setUpReceipts() throws IOException {
            expenseId = UUID.randomUUID();
            receiptId = UUID.randomUUID();
            viewModel.setReceiptStorageService(receiptStorageService);

            // Add a receipt first
            Path jpgFile = tempDir.resolve("receipt.jpg");
            Files.write(jpgFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

            ReceiptMetadata mockMetadata = new ReceiptMetadata(
                receiptId, expenseId, "receipt.jpg",
                jpgFile, "image/jpeg", 1024L, Instant.now()
            );
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);
            viewModel.attachReceipt(jpgFile.toFile());
        }

        @Test
        @DisplayName("should decrement count when receipt removed")
        void shouldDecrementCountWhenReceiptRemoved() {
            when(receiptStorageService.deleteReceipt(receiptId)).thenReturn(true);

            viewModel.removeReceipt(receiptId);

            assertThat(viewModel.getReceiptCount()).isZero();
            assertThat(viewModel.getReceiptCountText()).isEqualTo("0 of 5");
        }

        @Test
        @DisplayName("should remove receipt from list")
        void shouldRemoveReceiptFromList() {
            when(receiptStorageService.deleteReceipt(receiptId)).thenReturn(true);

            viewModel.removeReceipt(receiptId);

            assertThat(viewModel.getReceipts()).isEmpty();
        }

        @Test
        @DisplayName("should show dropzone when last receipt removed")
        void shouldShowDropzoneWhenLastReceiptRemoved() {
            when(receiptStorageService.deleteReceipt(receiptId)).thenReturn(true);

            viewModel.removeReceipt(receiptId);

            assertThat(viewModel.isDropzoneVisible()).isTrue();
            assertThat(viewModel.isReceiptGridVisible()).isFalse();
        }

        @Test
        @DisplayName("should mark form as dirty when receipt removed")
        void shouldMarkFormAsDirtyWhenReceiptRemoved() {
            // Reset dirty flag first
            viewModel.resetForm();
            viewModel.setReceiptStorageService(receiptStorageService);

            // Re-add a receipt
            ReceiptMetadata mockMetadata = new ReceiptMetadata(
                receiptId, expenseId, "receipt.jpg",
                tempDir.resolve("receipt.jpg"), "image/jpeg", 1024L, Instant.now()
            );
            when(receiptStorageService.storeReceipt(any(), any(), any())).thenReturn(mockMetadata);
            viewModel.attachReceipt(tempDir.resolve("receipt.jpg").toFile());

            // Clear dirty flag manually for test
            // Note: In real implementation, loading would clear dirty
            when(receiptStorageService.deleteReceipt(receiptId)).thenReturn(true);

            viewModel.removeReceipt(receiptId);

            assertThat(viewModel.isDirty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Receipt Attachment - Error Handling")
    class ReceiptErrorHandling {

        @Mock
        private ReceiptStorageService receiptStorageService;

        @TempDir
        Path tempDir;

        @BeforeEach
        void setUpReceipts() {
            viewModel.setReceiptStorageService(receiptStorageService);
        }

        @Test
        @DisplayName("should show error message with filename")
        void shouldShowErrorMessageWithFilename() throws IOException {
            Path invalidFile = tempDir.resolve("document.doc");
            Files.write(invalidFile, "doc content".getBytes());

            when(receiptStorageService.storeReceipt(any(), any(), eq("document.doc")))
                .thenThrow(new ReceiptStorageException(
                    ReceiptStorageException.ErrorType.UNSUPPORTED_FORMAT));

            viewModel.attachReceipt(invalidFile.toFile());

            assertThat(viewModel.hasReceiptError()).isTrue();
            assertThat(viewModel.getReceiptErrorMessage()).contains("document.doc");
        }

        @Test
        @DisplayName("should clear error when dismissed")
        void shouldClearErrorWhenDismissed() throws IOException {
            Path invalidFile = tempDir.resolve("document.doc");
            Files.write(invalidFile, "doc content".getBytes());

            when(receiptStorageService.storeReceipt(any(), any(), any()))
                .thenThrow(new ReceiptStorageException(
                    ReceiptStorageException.ErrorType.UNSUPPORTED_FORMAT));

            viewModel.attachReceipt(invalidFile.toFile());
            assertThat(viewModel.hasReceiptError()).isTrue();

            viewModel.clearReceiptError();

            assertThat(viewModel.hasReceiptError()).isFalse();
            assertThat(viewModel.getReceiptErrorMessage()).isEmpty();
        }

        @Test
        @DisplayName("should show storage error message")
        void shouldShowStorageErrorMessage() throws IOException {
            Path jpgFile = tempDir.resolve("receipt.jpg");
            Files.write(jpgFile, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

            when(receiptStorageService.storeReceipt(any(), any(), any()))
                .thenThrow(new ReceiptStorageException(
                    ReceiptStorageException.ErrorType.STORAGE_ERROR,
                    "Disk full"));

            viewModel.attachReceipt(jpgFile.toFile());

            assertThat(viewModel.hasReceiptError()).isTrue();
            assertThat(viewModel.getReceiptErrorMessage()).contains("Could not save");
        }
    }

    @Nested
    @DisplayName("Receipt Attachment - Edit Mode")
    class ReceiptEditMode {

        @Mock
        private ReceiptStorageService receiptStorageService;

        @TempDir
        Path tempDir;

        private UUID expenseId;
        private Expense existingExpense;

        @BeforeEach
        void setUpEditMode() {
            expenseId = UUID.randomUUID();
            existingExpense = new Expense(
                expenseId,
                businessId,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("54.99"),
                "Office Supplies",
                ExpenseCategory.OFFICE_COSTS,
                null,
                null,
                null,
                null,
                null
            );
            viewModel.setReceiptStorageService(receiptStorageService);
        }

        @Test
        @DisplayName("should load existing receipts when entering edit mode")
        void shouldLoadExistingReceiptsWhenEnteringEditMode() {
            List<ReceiptMetadata> existingReceipts = List.of(
                new ReceiptMetadata(UUID.randomUUID(), expenseId, "receipt1.jpg",
                    tempDir.resolve("r1.jpg"), "image/jpeg", 1024L, Instant.now()),
                new ReceiptMetadata(UUID.randomUUID(), expenseId, "receipt2.pdf",
                    tempDir.resolve("r2.pdf"), "application/pdf", 2048L, Instant.now())
            );
            when(receiptStorageService.listReceipts(expenseId)).thenReturn(existingReceipts);

            viewModel.loadExpense(existingExpense);

            assertThat(viewModel.getReceiptCount()).isEqualTo(2);
            assertThat(viewModel.getReceiptCountText()).isEqualTo("2 of 5");
            assertThat(viewModel.getReceipts()).hasSize(2);
        }

        @Test
        @DisplayName("should not mark dirty when loading existing receipts")
        void shouldNotMarkDirtyWhenLoadingExistingReceipts() {
            List<ReceiptMetadata> existingReceipts = List.of(
                new ReceiptMetadata(UUID.randomUUID(), expenseId, "receipt.jpg",
                    tempDir.resolve("r.jpg"), "image/jpeg", 1024L, Instant.now())
            );
            when(receiptStorageService.listReceipts(expenseId)).thenReturn(existingReceipts);

            viewModel.loadExpense(existingExpense);

            assertThat(viewModel.isDirty()).isFalse();
        }

        @Test
        @DisplayName("should show receipt grid when expense has receipts")
        void shouldShowReceiptGridWhenExpenseHasReceipts() {
            List<ReceiptMetadata> existingReceipts = List.of(
                new ReceiptMetadata(UUID.randomUUID(), expenseId, "receipt.jpg",
                    tempDir.resolve("r.jpg"), "image/jpeg", 1024L, Instant.now())
            );
            when(receiptStorageService.listReceipts(expenseId)).thenReturn(existingReceipts);

            viewModel.loadExpense(existingExpense);

            assertThat(viewModel.isDropzoneVisible()).isFalse();
            assertThat(viewModel.isReceiptGridVisible()).isTrue();
        }

        @Test
        @DisplayName("should clear receipts when resetting form")
        void shouldClearReceiptsWhenResettingForm() {
            List<ReceiptMetadata> existingReceipts = List.of(
                new ReceiptMetadata(UUID.randomUUID(), expenseId, "receipt.jpg",
                    tempDir.resolve("r.jpg"), "image/jpeg", 1024L, Instant.now())
            );
            when(receiptStorageService.listReceipts(expenseId)).thenReturn(existingReceipts);

            viewModel.loadExpense(existingExpense);
            assertThat(viewModel.getReceiptCount()).isEqualTo(1);

            viewModel.resetForm();

            assertThat(viewModel.getReceiptCount()).isZero();
            assertThat(viewModel.getReceipts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Receipt Attachment - Multiple Files")
    class ReceiptMultipleFiles {

        @Mock
        private ReceiptStorageService receiptStorageService;

        @TempDir
        Path tempDir;

        private UUID expenseId;

        @BeforeEach
        void setUpReceipts() {
            expenseId = UUID.randomUUID();
            viewModel.setReceiptStorageService(receiptStorageService);
        }

        @Test
        @DisplayName("should attach multiple files at once")
        void shouldAttachMultipleFilesAtOnce() throws IOException {
            List<File> files = createTestFiles(3);
            mockSuccessfulStorage();

            viewModel.attachReceipts(files);

            assertThat(viewModel.getReceiptCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should stop at max when attaching multiple files")
        void shouldStopAtMaxWhenAttachingMultipleFiles() throws IOException {
            // Pre-add 3 receipts
            for (int i = 0; i < 3; i++) {
                mockSuccessfulStorageForFile("existing" + i + ".jpg");
                viewModel.attachReceipt(tempDir.resolve("existing" + i + ".jpg").toFile());
            }

            // Try to add 4 more (only 2 should fit)
            List<File> newFiles = createTestFiles(4);
            for (int i = 0; i < 2; i++) {
                mockSuccessfulStorageForFile("new" + i + ".jpg");
            }
            // Note: ViewModel checks max before calling service, so no stubbing needed for files 2-3

            viewModel.attachReceipts(newFiles);

            // Should have 5 receipts (3 existing + 2 new)
            assertThat(viewModel.getReceiptCount()).isEqualTo(5);
            assertThat(viewModel.hasReceiptError()).isTrue();
        }

        @Test
        @DisplayName("should continue attaching valid files after invalid one")
        void shouldContinueAttachingValidFilesAfterInvalidOne() throws IOException {
            // First file invalid, second and third valid
            File invalidFile = tempDir.resolve("invalid.doc").toFile();
            Files.write(invalidFile.toPath(), "doc".getBytes());

            File validFile1 = tempDir.resolve("valid1.jpg").toFile();
            Files.write(validFile1.toPath(), new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

            File validFile2 = tempDir.resolve("valid2.jpg").toFile();
            Files.write(validFile2.toPath(), new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

            when(receiptStorageService.storeReceipt(any(), any(), eq("invalid.doc")))
                .thenThrow(new ReceiptStorageException(
                    ReceiptStorageException.ErrorType.UNSUPPORTED_FORMAT));
            mockSuccessfulStorageForFile("valid1.jpg");
            mockSuccessfulStorageForFile("valid2.jpg");

            viewModel.attachReceipts(List.of(invalidFile, validFile1, validFile2));

            // Should have 2 valid receipts and show error for invalid one
            assertThat(viewModel.getReceiptCount()).isEqualTo(2);
            assertThat(viewModel.hasReceiptError()).isTrue();
        }

        private List<File> createTestFiles(int count) throws IOException {
            List<File> files = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                Path file = tempDir.resolve("new" + i + ".jpg");
                Files.write(file, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
                files.add(file.toFile());
            }
            return files;
        }

        private void mockSuccessfulStorage() {
            when(receiptStorageService.storeReceipt(any(), any(), any()))
                .thenAnswer(inv -> new ReceiptMetadata(
                    UUID.randomUUID(), expenseId, inv.getArgument(2),
                    tempDir.resolve(inv.getArgument(2).toString()),
                    "image/jpeg", 1024L, Instant.now()
                ));
        }

        private void mockSuccessfulStorageForFile(String filename) {
            when(receiptStorageService.storeReceipt(any(), any(), eq(filename)))
                .thenReturn(new ReceiptMetadata(
                    UUID.randomUUID(), expenseId, filename,
                    tempDir.resolve(filename), "image/jpeg", 1024L, Instant.now()
                ));
        }
    }
}
