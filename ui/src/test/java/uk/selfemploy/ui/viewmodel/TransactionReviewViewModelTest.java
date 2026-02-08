package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.*;
import uk.selfemploy.common.domain.BankTransaction;
import uk.selfemploy.common.enums.ReviewStatus;
import uk.selfemploy.ui.service.SqliteBankTransactionService;
import uk.selfemploy.ui.service.SqliteDataStore;
import uk.selfemploy.ui.service.SqliteTestSupport;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TransactionReviewViewModel.
 * Verifies loading, filtering, sorting, selection, batch ops, undo, export, and pagination.
 */
class TransactionReviewViewModelTest {

    private static UUID businessId;
    private SqliteBankTransactionService service;
    private TransactionReviewViewModel viewModel;

    @BeforeAll
    static void setUpClass() {
        SqliteTestSupport.setUpTestEnvironment();
        businessId = UUID.randomUUID();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
    }

    @AfterAll
    static void tearDownClass() {
        SqliteTestSupport.tearDownTestEnvironment();
    }

    @BeforeEach
    void setUp() {
        SqliteTestSupport.resetTestData();
        SqliteDataStore.getInstance().ensureBusinessExists(businessId);
        service = new SqliteBankTransactionService(businessId);
        viewModel = new TransactionReviewViewModel(service);
    }

    @Nested
    class Loading {

        @Test
        void loadTransactions_shouldPopulateItems() {
            saveTestTransaction("Payment A", new BigDecimal("100.00"));
            saveTestTransaction("Payment B", new BigDecimal("-50.00"));

            viewModel.loadTransactions();

            assertThat(viewModel.getAllItems()).hasSize(2);
            assertThat(viewModel.getFilteredItems()).hasSize(2);
        }

        @Test
        void loadTransactions_shouldUpdateSummaries() {
            saveTestTransaction("Pending 1", new BigDecimal("10"));
            BankTransaction tx2 = saveTestTransaction("For exclude", new BigDecimal("20"));
            service.exclude(tx2.id(), "Transfer", Instant.now());

            viewModel.loadTransactions();

            assertThat(viewModel.getTotalCount()).isEqualTo(2);
            assertThat(viewModel.getPendingCount()).isEqualTo(1);
            assertThat(viewModel.getExcludedCount()).isEqualTo(1);
        }

        @Test
        void loadTransactions_shouldCalculateProgress() {
            saveTestTransaction("Pending", new BigDecimal("10"));
            BankTransaction tx2 = saveTestTransaction("Done", new BigDecimal("20"));
            service.exclude(tx2.id(), "Transfer", Instant.now());

            viewModel.loadTransactions();

            assertThat(viewModel.getReviewedCount()).isEqualTo(1);
            assertThat(viewModel.getReviewProgress()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        void isEmptyState_shouldBeTrue_whenNoTransactions() {
            viewModel.loadTransactions();
            assertThat(viewModel.isEmptyState()).isTrue();
        }
    }

    @Nested
    class Filtering {

        @Test
        void searchFilter_shouldMatchDescription() {
            saveTestTransaction("Amazon purchase", new BigDecimal("-30"));
            saveTestTransaction("Netflix subscription", new BigDecimal("-15"));
            viewModel.loadTransactions();

            viewModel.searchTextProperty().set("amazon");

            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).description()).isEqualTo("Amazon purchase");
        }

        @Test
        void statusFilter_shouldFilterByStatus() {
            saveTestTransaction("Pending one", new BigDecimal("10"));
            BankTransaction tx2 = saveTestTransaction("Excluded one", new BigDecimal("20"));
            service.exclude(tx2.id(), "Transfer", Instant.now());
            viewModel.loadTransactions();

            viewModel.statusFilterProperty().set(ReviewStatus.EXCLUDED);

            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
        }

        @Test
        void dateFilter_shouldFilterByRange() {
            saveTestTransaction("Jan", new BigDecimal("10"), LocalDate.of(2025, 1, 15));
            saveTestTransaction("Jun", new BigDecimal("20"), LocalDate.of(2025, 6, 15));
            viewModel.loadTransactions();

            viewModel.dateFromProperty().set(LocalDate.of(2025, 6, 1));
            viewModel.dateToProperty().set(LocalDate.of(2025, 6, 30));

            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).description()).isEqualTo("Jun");
        }

        @Test
        void amountFilter_shouldFilterByAbsoluteRange() {
            saveTestTransaction("Small", new BigDecimal("-10"));
            saveTestTransaction("Big", new BigDecimal("-500"));
            viewModel.loadTransactions();

            viewModel.amountMinProperty().set(new BigDecimal("100"));

            assertThat(viewModel.getFilteredItems()).hasSize(1);
            assertThat(viewModel.getFilteredItems().get(0).description()).isEqualTo("Big");
        }

        @Test
        void isNoResults_shouldBeTrue_whenFiltersMatchNothing() {
            saveTestTransaction("Existing", new BigDecimal("10"));
            viewModel.loadTransactions();

            viewModel.searchTextProperty().set("nonexistent");

            assertThat(viewModel.isNoResults()).isTrue();
            assertThat(viewModel.isEmptyState()).isFalse();
        }
    }

    @Nested
    class Sorting {

        @Test
        void sortByDate_ascending() {
            saveTestTransaction("Old", new BigDecimal("10"), LocalDate.of(2025, 1, 1));
            saveTestTransaction("New", new BigDecimal("20"), LocalDate.of(2025, 12, 1));
            viewModel.loadTransactions();

            viewModel.sortByDate(true);

            assertThat(viewModel.getFilteredItems().get(0).description()).isEqualTo("Old");
            assertThat(viewModel.getFilteredItems().get(1).description()).isEqualTo("New");
        }

        @Test
        void sortByAmount_descending() {
            saveTestTransaction("Small", new BigDecimal("10"));
            saveTestTransaction("Large", new BigDecimal("500"));
            viewModel.loadTransactions();

            viewModel.sortByAmount(false);

            assertThat(viewModel.getFilteredItems().get(0).description()).isEqualTo("Large");
        }

        @Test
        void sortByDescription_ascending() {
            saveTestTransaction("Zebra", new BigDecimal("10"));
            saveTestTransaction("Apple", new BigDecimal("20"));
            viewModel.loadTransactions();

            viewModel.sortByDescription(true);

            assertThat(viewModel.getFilteredItems().get(0).description()).isEqualTo("Apple");
        }
    }

    @Nested
    class Selection {

        @Test
        void toggleSelection_shouldSelectAndDeselect() {
            BankTransaction tx = saveTestTransaction("Test", new BigDecimal("10"));
            viewModel.loadTransactions();

            viewModel.toggleSelection(tx.id());
            assertThat(viewModel.isSelected(tx.id())).isTrue();
            assertThat(viewModel.getSelectedCount()).isEqualTo(1);

            viewModel.toggleSelection(tx.id());
            assertThat(viewModel.isSelected(tx.id())).isFalse();
            assertThat(viewModel.getSelectedCount()).isZero();
        }

        @Test
        void selectAll_shouldSelectAllFiltered() {
            saveTestTransaction("A", new BigDecimal("10"));
            saveTestTransaction("B", new BigDecimal("20"));
            viewModel.loadTransactions();

            viewModel.selectAll();

            assertThat(viewModel.getSelectedCount()).isEqualTo(2);
        }

        @Test
        void selectAllPending_shouldOnlySelectPending() {
            saveTestTransaction("Pending", new BigDecimal("10"));
            BankTransaction excluded = saveTestTransaction("Excluded", new BigDecimal("20"));
            service.exclude(excluded.id(), "Transfer", Instant.now());
            viewModel.loadTransactions();

            viewModel.selectAllPending();

            assertThat(viewModel.getSelectedCount()).isEqualTo(1);
        }

        @Test
        void clearSelection_shouldDeselectAll() {
            saveTestTransaction("A", new BigDecimal("10"));
            viewModel.loadTransactions();
            viewModel.selectAll();

            viewModel.clearSelection();

            assertThat(viewModel.getSelectedCount()).isZero();
        }
    }

    @Nested
    class BatchOperations {

        @Test
        void batchMarkBusiness_shouldFlagAllSelected() {
            BankTransaction tx1 = saveTestTransaction("A", new BigDecimal("10"));
            BankTransaction tx2 = saveTestTransaction("B", new BigDecimal("20"));
            viewModel.loadTransactions();
            viewModel.selectAll();

            viewModel.batchMarkBusiness();

            assertThat(service.findById(tx1.id()).orElseThrow().isBusiness()).isTrue();
            assertThat(service.findById(tx2.id()).orElseThrow().isBusiness()).isTrue();
            assertThat(viewModel.getSelectedCount()).isZero(); // Selection cleared
        }

        @Test
        void batchMarkPersonal_shouldFlagAllSelected() {
            BankTransaction tx1 = saveTestTransaction("A", new BigDecimal("10"));
            viewModel.loadTransactions();
            viewModel.selectAll();

            viewModel.batchMarkPersonal();

            assertThat(service.findById(tx1.id()).orElseThrow().isBusiness()).isFalse();
        }

        @Test
        void batchExclude_shouldExcludeAllSelected() {
            BankTransaction tx1 = saveTestTransaction("A", new BigDecimal("10"));
            BankTransaction tx2 = saveTestTransaction("B", new BigDecimal("20"));
            viewModel.loadTransactions();
            viewModel.selectAll();

            viewModel.batchExclude("Personal transaction");

            assertThat(service.findById(tx1.id()).orElseThrow().reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
            assertThat(service.findById(tx2.id()).orElseThrow().reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
        }

        @Test
        void batchOps_shouldNoOp_whenNothingSelected() {
            saveTestTransaction("A", new BigDecimal("10"));
            viewModel.loadTransactions();

            // Should not throw or change anything
            viewModel.batchMarkBusiness();
            viewModel.batchMarkPersonal();
            viewModel.batchExclude("reason");

            assertThat(service.findAll().get(0).reviewStatus()).isEqualTo(ReviewStatus.PENDING);
        }
    }

    @Nested
    class IndividualOperations {

        @Test
        void excludeTransaction_shouldExcludeWithReason() {
            BankTransaction tx = saveTestTransaction("Exclude me", new BigDecimal("10"));
            viewModel.loadTransactions();

            viewModel.excludeTransaction(tx.id(), "Duplicate");

            BankTransaction updated = service.findById(tx.id()).orElseThrow();
            assertThat(updated.reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
            assertThat(updated.exclusionReason()).isEqualTo("Duplicate");
        }

        @Test
        void skipTransaction_shouldSetStatusToSkipped() {
            BankTransaction tx = saveTestTransaction("Skip me", new BigDecimal("10"));
            viewModel.loadTransactions();

            viewModel.skipTransaction(tx.id());

            assertThat(service.findById(tx.id()).orElseThrow().reviewStatus()).isEqualTo(ReviewStatus.SKIPPED);
        }

        @Test
        void toggleBusinessFlag_shouldSetFlag() {
            BankTransaction tx = saveTestTransaction("Flag me", new BigDecimal("10"));
            viewModel.loadTransactions();

            viewModel.toggleBusinessFlag(tx.id(), true);

            assertThat(service.findById(tx.id()).orElseThrow().isBusiness()).isTrue();
        }
    }

    @Nested
    class Undo {

        @Test
        void undo_shouldRestorePreviousState() {
            BankTransaction tx = saveTestTransaction("Will be excluded", new BigDecimal("10"));
            viewModel.loadTransactions();

            // Verify initial state
            assertThat(viewModel.getCanUndo()).isFalse();

            // Exclude it
            viewModel.excludeTransaction(tx.id(), "Transfer");
            assertThat(service.findById(tx.id()).orElseThrow().reviewStatus()).isEqualTo(ReviewStatus.EXCLUDED);
            assertThat(viewModel.getCanUndo()).isTrue();

            // Undo
            viewModel.undo();
            assertThat(service.findById(tx.id()).orElseThrow().reviewStatus()).isEqualTo(ReviewStatus.PENDING);
            assertThat(viewModel.getCanUndo()).isFalse();
        }

        @Test
        void undo_shouldNoOp_whenNoSnapshot() {
            viewModel.loadTransactions();
            viewModel.undo(); // Should not throw
            assertThat(viewModel.getCanUndo()).isFalse();
        }
    }

    @Nested
    class Export {

        @Test
        void exportCsv_shouldWriteFilteredData() throws IOException {
            saveTestTransaction("Export test", new BigDecimal("100.00"));
            viewModel.loadTransactions();

            Path tempFile = Files.createTempFile("test-export", ".csv");
            try {
                viewModel.exportCsv(tempFile);

                List<String> lines = Files.readAllLines(tempFile);
                assertThat(lines).hasSize(2); // header + 1 data row
                assertThat(lines.get(0)).contains("Date,Description,Amount");
                assertThat(lines.get(1)).contains("Export test");
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        void exportJson_shouldWriteFilteredData() throws IOException {
            saveTestTransaction("JSON test", new BigDecimal("-50.00"));
            viewModel.loadTransactions();

            Path tempFile = Files.createTempFile("test-export", ".json");
            try {
                viewModel.exportJson(tempFile);

                String content = Files.readString(tempFile);
                assertThat(content).contains("JSON test");
                assertThat(content).startsWith("[");
                assertThat(content).endsWith("]");
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    @Nested
    class Pagination {

        @Test
        void pagination_shouldReturnCorrectPageItems() {
            // Create 25 transactions
            for (int i = 0; i < 25; i++) {
                saveTestTransaction("Tx " + String.format("%02d", i), new BigDecimal(i + 1));
            }
            viewModel.loadTransactions();

            // Default page size is 20
            List<TransactionReviewTableRow> page1 = viewModel.getCurrentPageItems();
            assertThat(page1).hasSize(20);

            assertThat(viewModel.canGoNext()).isTrue();
            assertThat(viewModel.canGoPrevious()).isFalse();

            viewModel.nextPage();
            List<TransactionReviewTableRow> page2 = viewModel.getCurrentPageItems();
            assertThat(page2).hasSize(5);

            assertThat(viewModel.canGoNext()).isFalse();
            assertThat(viewModel.canGoPrevious()).isTrue();
        }

        @Test
        void getResultCountText_shouldFormatCorrectly() {
            saveTestTransaction("Tx 1", new BigDecimal("10"));
            saveTestTransaction("Tx 2", new BigDecimal("20"));
            viewModel.loadTransactions();

            String text = viewModel.getResultCountText();
            assertThat(text).contains("1").contains("2");
        }

        @Test
        void getResultCountText_shouldShowZero_whenEmpty() {
            viewModel.loadTransactions();
            assertThat(viewModel.getResultCountText()).isEqualTo("Showing 0 entries");
        }
    }

    // === Helpers ===

    private BankTransaction saveTestTransaction(String desc, BigDecimal amount) {
        return saveTestTransaction(desc, amount, LocalDate.of(2025, 6, 1));
    }

    private BankTransaction saveTestTransaction(String desc, BigDecimal amount, LocalDate date) {
        BankTransaction tx = BankTransaction.create(
            businessId, UUID.randomUUID(), null,
            date, amount, desc, null, null,
            UUID.randomUUID().toString(), Instant.now()
        );
        service.save(tx);
        return tx;
    }
}
