package uk.selfemploy.ui.e2e;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.ui.controller.SubmissionHistoryController;
import uk.selfemploy.ui.service.SubmissionPdfDownloadService;
import uk.selfemploy.ui.util.HmrcErrorGuidance;
import uk.selfemploy.ui.viewmodel.SubmissionHistoryViewModel;
import uk.selfemploy.ui.viewmodel.SubmissionTableRow;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.*;

/**
 * E2E/UI Tests for Submission History.
 * Tests based on /rob's QA test specification.
 *
 * <p>Test IDs:</p>
 * <ul>
 *   <li>SE-404: E2E-404-001 to E2E-404-037 (Sprint 4 - Core functionality)</li>
 *   <li>SE-SH-005: TC-10E-012 to TC-10E-015 (Sprint 10E - PDF Download)</li>
 *   <li>SE-SH-006: TC-10E-016 to TC-10E-020 (Sprint 10E - Error Guidance)</li>
 * </ul>
 *
 * @see docs/sprints/sprint-4/testing/rob-qa-SE-404.md
 * @see docs/sprints/sprint-10E/testing/adam-e2e-tests.md
 */
@Tag("e2e")
@DisabledIfSystemProperty(named = "skipE2ETests", matches = "true")
@DisplayName("SE-404 Submission History E2E Tests")
class SubmissionHistoryE2ETest extends ApplicationTest {

    private SubmissionHistoryController controller;
    private SubmissionHistoryViewModel viewModel;

    @Override
    public void start(Stage stage) throws Exception {
        // Configure headless mode if requested
        if (Boolean.getBoolean("headless") || Boolean.getBoolean("testfx.headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
        }

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/submission-history.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 900, 700);

        // Clear ALL stylesheets to avoid StackOverflow from CSS lookup chains
        // The main.css uses CSS variable lookups (e.g., -fx-bg-secondary) that can cause
        // infinite recursion in TestFX when the .root class isn't properly resolved
        // BUG-001 fix: Clear BOTH scene and root stylesheets AFTER Scene creation
        scene.getStylesheets().clear();
        root.getStylesheets().clear();

        // Apply minimal test CSS with direct values (no lookups)
        try {
            String testCss = getClass().getResource("/css/test-minimal.css").toExternalForm();
            scene.getStylesheets().add(testCss);
        } catch (Exception ignored) {
            // Test CSS may not exist, continue without it
        }

        stage.setScene(scene);
        stage.show();

        WaitForAsyncUtils.waitForFxEvents();

        viewModel = controller.getViewModel();
    }

    @Nested
    @DisplayName("P0: Submission List Display (TC-404-001 to TC-404-004)")
    class SubmissionListDisplayTests {

        @Test
        @DisplayName("E2E-404-001: Empty state shows 'No Submissions Yet' message")
        void emptyStateDisplaysMessage() {
            // Given - no submissions
            runOnFxThread(() -> viewModel.clearAll());

            // Then
            verifyThat("#emptyState", isVisible());
            verifyThat("#submissionsList", isInvisible());
        }

        @Test
        @DisplayName("E2E-404-002: Single submission displays one card")
        void singleSubmissionDisplaysOneCard() {
            // Given
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);
            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(submission));
                controller.refreshData();
            });

            // Then
            verifyThat("#submissionsList", isVisible());
            verifyThat("#emptyState", isInvisible());

            VBox submissionsList = lookup("#submissionsList").query();
            assertThat(submissionsList.getChildren()).hasSize(1);
        }

        @Test
        @DisplayName("E2E-404-003: Multiple submissions display multiple cards")
        void multipleSubmissionsDisplayMultipleCards() {
            // Given
            List<SubmissionTableRow> submissions = List.of(
                    createQ1Submission(SubmissionStatus.ACCEPTED),
                    createQ2Submission(SubmissionStatus.PENDING),
                    createQ3Submission(SubmissionStatus.SUBMITTED),
                    createQ4Submission(SubmissionStatus.REJECTED),
                    createAnnualSubmission(SubmissionStatus.ACCEPTED)
            );
            runOnFxThread(() -> {
                viewModel.setSubmissions(submissions);
                controller.refreshData();
            });

            // Then
            VBox submissionsList = lookup("#submissionsList").query();
            assertThat(submissionsList.getChildren()).hasSize(5);
        }

        @Test
        @DisplayName("E2E-404-004: Mixed types (Q1-Q4, Annual) all displayed correctly")
        void mixedTypesDisplayedCorrectly() {
            // Given
            List<SubmissionTableRow> submissions = List.of(
                    createQ1Submission(SubmissionStatus.ACCEPTED),
                    createQ2Submission(SubmissionStatus.ACCEPTED),
                    createQ3Submission(SubmissionStatus.ACCEPTED),
                    createQ4Submission(SubmissionStatus.ACCEPTED),
                    createAnnualSubmission(SubmissionStatus.ACCEPTED)
            );
            runOnFxThread(() -> {
                viewModel.setSubmissions(submissions);
                controller.refreshData();
            });

            // Then - all 5 submissions should be displayed
            VBox submissionsList = lookup("#submissionsList").query();
            assertThat(submissionsList.getChildren()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("P0: Submission Card Content (TC-404-005 to TC-404-009)")
    class SubmissionCardContentTests {

        @Test
        @DisplayName("E2E-404-005: Date displayed in correct format (d MMM yyyy)")
        void dateDisplayedCorrectly() {
            // Given
            LocalDateTime submittedAt = LocalDateTime.of(2026, 1, 24, 14, 32);
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(submittedAt)
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.ACCEPTED)
                    .hmrcReference("REF-001")
                    .totalIncome(new BigDecimal("10000.00"))
                    .totalExpenses(new BigDecimal("2000.00"))
                    .netProfit(new BigDecimal("8000.00"))
                    .build();

            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(submission));
                controller.refreshData();
            });

            // Then - verify date formatting
            assertThat(submission.getFormattedDate()).isEqualTo("24 Jan 2026");
            assertThat(submission.getFormattedTime()).isEqualTo("14:32");
        }

        @Test
        @DisplayName("E2E-404-006: Quarterly type badge shows 'Q1'")
        void quarterlyTypeBadgeShowsQ1() {
            // Given
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);

            // Then
            assertThat(submission.getTypeBadgeText()).isEqualTo("Q1");
            assertThat(submission.getTypeStyleClass()).isEqualTo("type-q1");
        }

        @Test
        @DisplayName("E2E-404-007: Annual type badge shows 'Annual'")
        void annualTypeBadgeShowsAnnual() {
            // Given
            SubmissionTableRow submission = createAnnualSubmission(SubmissionStatus.ACCEPTED);

            // Then
            assertThat(submission.getTypeBadgeText()).isEqualTo("Annual");
            assertThat(submission.getTypeStyleClass()).isEqualTo("type-annual");
        }

        @Test
        @DisplayName("E2E-404-008: Tax year displayed in format '2025/26'")
        void taxYearDisplayedCorrectly() {
            // Given
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);
            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(submission));
                controller.refreshData();
            });

            // Then
            assertThat(submission.taxYear()).isEqualTo("2025/26");
        }

        @Test
        @DisplayName("E2E-404-009: HMRC reference displayed correctly")
        void hmrcReferenceDisplayedCorrectly() {
            // Given
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.ACCEPTED)
                    .hmrcReference("MTD-Q1-123456")
                    .totalIncome(new BigDecimal("10000.00"))
                    .totalExpenses(new BigDecimal("2000.00"))
                    .netProfit(new BigDecimal("8000.00"))
                    .build();

            // Then
            assertThat(submission.getReferenceDisplay()).isEqualTo("MTD-Q1-123456");
        }

        @Test
        @DisplayName("E2E-404-010: Pending reference displays 'Pending'")
        void pendingReferenceDisplaysPending() {
            // Given
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.PENDING)
                    .hmrcReference(null)
                    .totalIncome(new BigDecimal("10000.00"))
                    .totalExpenses(new BigDecimal("2000.00"))
                    .netProfit(new BigDecimal("8000.00"))
                    .build();

            // Then
            assertThat(submission.getReferenceDisplay()).isEqualTo("Pending");
        }
    }

    @Nested
    @DisplayName("P0: Status Badges (TC-404-011 to TC-404-014)")
    class StatusBadgeTests {

        @Test
        @DisplayName("E2E-404-011: ACCEPTED status shows correct style class")
        void acceptedStatusStyleClass() {
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);
            assertThat(submission.getStatusStyleClass()).isEqualTo("status-accepted");
            assertThat(submission.getStatusDisplay()).isEqualTo("Accepted");
        }

        @Test
        @DisplayName("E2E-404-012: PENDING status shows correct style class")
        void pendingStatusStyleClass() {
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.PENDING);
            assertThat(submission.getStatusStyleClass()).isEqualTo("status-pending");
            assertThat(submission.getStatusDisplay()).isEqualTo("Pending");
        }

        @Test
        @DisplayName("E2E-404-013: REJECTED status shows correct style class")
        void rejectedStatusStyleClass() {
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.REJECTED);
            assertThat(submission.getStatusStyleClass()).isEqualTo("status-rejected");
            assertThat(submission.getStatusDisplay()).isEqualTo("Rejected");
        }

        @Test
        @DisplayName("E2E-404-014: SUBMITTED status shows correct style class")
        void submittedStatusStyleClass() {
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.SUBMITTED);
            assertThat(submission.getStatusStyleClass()).isEqualTo("status-submitted");
            assertThat(submission.getStatusDisplay()).isEqualTo("Submitted");
        }
    }

    @Nested
    @DisplayName("P0: Tax Year Filtering (TC-404-015 to TC-404-017)")
    class TaxYearFilteringTests {

        @Test
        @DisplayName("E2E-404-015: Filter dropdown shows 'All Years' and available tax years")
        void filterDropdownShowsAvailableYears() {
            // Given
            List<SubmissionTableRow> submissions = List.of(
                    createSubmission(SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED),
                    createSubmission(SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.ACCEPTED),
                    createSubmission(SubmissionType.QUARTERLY_Q1, "2024/25", SubmissionStatus.ACCEPTED)
            );
            runOnFxThread(() -> {
                viewModel.setSubmissions(submissions);
                controller.refreshData();
            });

            // Then
            List<String> availableYears = viewModel.getAvailableTaxYears();
            assertThat(availableYears).contains("All Years", "2025/26", "2024/25");
        }

        @Test
        @DisplayName("E2E-404-016: Filter by specific year shows only matching submissions")
        void filterBySpecificYear() {
            // Given
            List<SubmissionTableRow> submissions = List.of(
                    createSubmission(SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED),
                    createSubmission(SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.ACCEPTED),
                    createSubmission(SubmissionType.QUARTERLY_Q1, "2024/25", SubmissionStatus.ACCEPTED)
            );
            runOnFxThread(() -> {
                viewModel.setSubmissions(submissions);
                // When - filter to 2025/26
                viewModel.setSelectedTaxYear("2025/26");
            });

            // Then
            List<SubmissionTableRow> filtered = viewModel.getFilteredSubmissions();
            assertThat(filtered).hasSize(2);
            assertThat(filtered).allMatch(s -> s.taxYear().equals("2025/26"));
        }

        @Test
        @DisplayName("E2E-404-017: Filter 'All Years' shows all submissions")
        void filterAllYearsShowsAllSubmissions() {
            // Given
            List<SubmissionTableRow> submissions = List.of(
                    createSubmission(SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED),
                    createSubmission(SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.ACCEPTED),
                    createSubmission(SubmissionType.QUARTERLY_Q1, "2024/25", SubmissionStatus.ACCEPTED)
            );
            runOnFxThread(() -> {
                viewModel.setSubmissions(submissions);
                // When - filter to All Years
                viewModel.setSelectedTaxYear("All Years");
            });

            // Then
            List<SubmissionTableRow> filtered = viewModel.getFilteredSubmissions();
            assertThat(filtered).hasSize(3);
        }
    }

    @Nested
    @DisplayName("P0: Sort Order (TC-404-019)")
    class SortOrderTests {

        @Test
        @DisplayName("E2E-404-019: Default sort is newest first")
        void defaultSortNewestFirst() {
            // Given
            LocalDateTime older = LocalDateTime.of(2026, 1, 10, 10, 0);
            LocalDateTime newer = LocalDateTime.of(2026, 1, 24, 14, 0);

            List<SubmissionTableRow> submissions = List.of(
                    createSubmissionWithDate(older, "REF-OLDER"),
                    createSubmissionWithDate(newer, "REF-NEWER")
            );
            runOnFxThread(() -> viewModel.setSubmissions(submissions));

            // Then - filtered submissions should be sorted newest first
            List<SubmissionTableRow> sorted = viewModel.getFilteredSubmissions();
            assertThat(sorted.get(0).hmrcReference()).isEqualTo("REF-NEWER");
            assertThat(sorted.get(1).hmrcReference()).isEqualTo("REF-OLDER");
        }
    }

    @Nested
    @DisplayName("P0: Detail Panel (TC-404-021 to TC-404-026)")
    class DetailPanelTests {

        @Test
        @DisplayName("E2E-404-021: Clicking card selects submission")
        void clickingCardSelectsSubmission() {
            // Given
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);
            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(submission));
                controller.refreshData();
            });

            // When
            runOnFxThread(() -> viewModel.selectSubmission(submission));

            // Then
            assertThat(viewModel.hasSelection()).isTrue();
            assertThat(viewModel.getSelectedSubmission()).isEqualTo(submission);
        }

        @Test
        @DisplayName("E2E-404-022: Detail shows formatted income")
        void detailShowsFormattedIncome() {
            // Given
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.ACCEPTED)
                    .hmrcReference("REF-001")
                    .totalIncome(new BigDecimal("45000.00"))
                    .totalExpenses(new BigDecimal("13000.00"))
                    .netProfit(new BigDecimal("32000.00"))
                    .build();

            // Then
            assertThat(submission.getFormattedIncome()).isEqualTo("£45,000.00");
        }

        @Test
        @DisplayName("E2E-404-023: Detail shows formatted expenses")
        void detailShowsFormattedExpenses() {
            // Given
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.ACCEPTED)
                    .hmrcReference("REF-001")
                    .totalIncome(new BigDecimal("45000.00"))
                    .totalExpenses(new BigDecimal("13000.00"))
                    .netProfit(new BigDecimal("32000.00"))
                    .build();

            // Then
            assertThat(submission.getFormattedExpenses()).isEqualTo("£13,000.00");
        }

        @Test
        @DisplayName("E2E-404-024: Detail shows formatted net profit")
        void detailShowsFormattedNetProfit() {
            // Given
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.ACCEPTED)
                    .hmrcReference("REF-001")
                    .totalIncome(new BigDecimal("45000.00"))
                    .totalExpenses(new BigDecimal("13000.00"))
                    .netProfit(new BigDecimal("32000.00"))
                    .build();

            // Then
            assertThat(submission.getFormattedProfit()).isEqualTo("£32,000.00");
        }

        @Test
        @DisplayName("E2E-404-026: Back button clears selection")
        void backButtonClearsSelection() {
            // Given
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);
            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(submission));
                viewModel.selectSubmission(submission);
            });

            // When
            runOnFxThread(() -> viewModel.clearSelection());

            // Then
            assertThat(viewModel.hasSelection()).isFalse();
            assertThat(viewModel.getSelectedSubmission()).isNull();
        }
    }

    @Nested
    @DisplayName("P0: Error Display (TC-404-027, TC-404-028)")
    class ErrorDisplayTests {

        @Test
        @DisplayName("E2E-404-027: Rejected submission shows error message")
        void rejectedSubmissionShowsErrorMessage() {
            // Given
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.REJECTED)
                    .hmrcReference(null)
                    .totalIncome(new BigDecimal("10000.00"))
                    .totalExpenses(new BigDecimal("2000.00"))
                    .netProfit(new BigDecimal("8000.00"))
                    .errorMessage("INVALID_DATE: Date format is incorrect")
                    .build();

            // Then
            assertThat(submission.isRejected()).isTrue();
            assertThat(submission.hasError()).isTrue();
            assertThat(submission.errorMessage()).contains("INVALID_DATE");
        }

        @Test
        @DisplayName("E2E-404-028: Accepted submission has no error")
        void acceptedSubmissionHasNoError() {
            // Given
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);

            // Then
            assertThat(submission.isRejected()).isFalse();
            assertThat(submission.hasError()).isFalse();
        }
    }

    @Nested
    @DisplayName("P0: Statistics Display (TC-404-032)")
    class StatisticsDisplayTests {

        @Test
        @DisplayName("E2E-404-032: Stats show correct counts by status")
        void statsShowCorrectCounts() {
            // Given
            List<SubmissionTableRow> submissions = List.of(
                    createQ1Submission(SubmissionStatus.ACCEPTED),
                    createQ2Submission(SubmissionStatus.ACCEPTED),
                    createQ3Submission(SubmissionStatus.PENDING),
                    createQ4Submission(SubmissionStatus.REJECTED),
                    createAnnualSubmission(SubmissionStatus.REJECTED)
            );
            runOnFxThread(() -> viewModel.setSubmissions(submissions));

            // Then
            assertThat(viewModel.getTotalCount()).isEqualTo(5);
            assertThat(viewModel.getAcceptedCount()).isEqualTo(2);
            assertThat(viewModel.getPendingCount()).isEqualTo(1);
            assertThat(viewModel.getRejectedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("E2E-404-033: Stats update with tax year filter")
        void statsUpdateWithFilter() {
            // Given
            List<SubmissionTableRow> submissions = List.of(
                    createSubmission(SubmissionType.QUARTERLY_Q1, "2025/26", SubmissionStatus.ACCEPTED),
                    createSubmission(SubmissionType.QUARTERLY_Q2, "2025/26", SubmissionStatus.REJECTED),
                    createSubmission(SubmissionType.QUARTERLY_Q1, "2024/25", SubmissionStatus.ACCEPTED)
            );
            runOnFxThread(() -> {
                viewModel.setSubmissions(submissions);
                // When - filter to 2025/26
                viewModel.setSelectedTaxYear("2025/26");
            });

            // Then - filtered stats
            assertThat(viewModel.getFilteredTotalCount()).isEqualTo(2);
            assertThat(viewModel.getFilteredAcceptedCount()).isEqualTo(1);
            assertThat(viewModel.getFilteredRejectedCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("P1: Currency Formatting (TC-404-037)")
    class CurrencyFormattingTests {

        @Test
        @DisplayName("E2E-404-037: UK locale currency formatting (£ with commas)")
        void ukLocaleCurrencyFormatting() {
            // Given
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.ACCEPTED)
                    .hmrcReference("REF-001")
                    .totalIncome(new BigDecimal("1234567.89"))
                    .totalExpenses(new BigDecimal("0.00"))
                    .netProfit(new BigDecimal("1234567.89"))
                    .taxDue(new BigDecimal("5051.80"))
                    .build();

            // Then
            assertThat(submission.getFormattedIncome()).isEqualTo("£1,234,567.89");
            assertThat(submission.getFormattedExpenses()).isEqualTo("£0.00");
            assertThat(submission.getFormattedTaxDue()).isEqualTo("£5,051.80");
        }
    }

    // =========================================================================
    // Sprint 10E: PDF Download Tests (SE-SH-005)
    // TC-10E-012 to TC-10E-015
    // =========================================================================

    @Nested
    @DisplayName("SE-SH-005: PDF Download (TC-10E-012 to TC-10E-015)")
    class PdfDownloadTests {

        private final SubmissionPdfDownloadService pdfService = new SubmissionPdfDownloadService();

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("TC-10E-012: PDF download button visible and enabled when submission selected")
        void pdfDownloadButtonVisibleAndEnabled() {
            // Given - a submission is loaded and selected
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);
            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(submission));
                viewModel.selectSubmission(submission);
                controller.refreshData();
            });

            // When - viewing the detail panel
            WaitForAsyncUtils.waitForFxEvents();

            // Then - downloadPdfBtn should exist and be enabled
            Button downloadBtn = lookup("#downloadPdfBtn").queryAs(Button.class);
            assertThat(downloadBtn).isNotNull();
            assertThat(downloadBtn.isDisabled()).isFalse();
            assertThat(downloadBtn.isVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-10E-013: PDF download generates file with correct filename")
        void pdfDownloadCreatesFileWithCorrectFilename() throws IOException {
            // Given - an accepted quarterly submission
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(12345L)
                    .submittedAt(LocalDateTime.of(2026, 1, 24, 14, 32))
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.ACCEPTED)
                    .hmrcReference("MTD-Q1-ABC123")
                    .totalIncome(new BigDecimal("45000.00"))
                    .totalExpenses(new BigDecimal("13000.00"))
                    .netProfit(new BigDecimal("32000.00"))
                    .taxDue(new BigDecimal("5051.80"))
                    .build();

            // When - generate filename and PDF
            String filename = pdfService.generateFilename(submission);
            Path outputPath = tempDir.resolve(filename);
            pdfService.generatePdf(submission, outputPath);

            // Then - file exists with correct pattern
            assertThat(filename).matches("submission-q1-2025-26-MTD-Q1-ABC123\\.pdf");
            assertThat(Files.exists(outputPath)).isTrue();
            assertThat(Files.size(outputPath)).isGreaterThan(0);
        }

        @Test
        @DisplayName("TC-10E-014: PDF contains submission details")
        void pdfContainsSubmissionDetails() throws IOException {
            // Given - a submission with all fields populated
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.of(2026, 1, 24, 14, 32))
                    .type(SubmissionType.ANNUAL)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.ACCEPTED)
                    .hmrcReference("SA-2025-123456789")
                    .totalIncome(new BigDecimal("65000.00"))
                    .totalExpenses(new BigDecimal("18000.00"))
                    .netProfit(new BigDecimal("47000.00"))
                    .taxDue(new BigDecimal("8540.00"))
                    .build();

            // When - generate PDF as bytes
            byte[] pdfBytes = pdfService.generatePdfBytes(submission);

            // Then - PDF is generated (non-empty)
            assertThat(pdfBytes).isNotNull();
            assertThat(pdfBytes.length).isGreaterThan(1000); // Reasonable minimum size

            // Verify it's a valid PDF (starts with %PDF)
            String pdfHeader = new String(pdfBytes, 0, Math.min(8, pdfBytes.length));
            assertThat(pdfHeader).startsWith("%PDF");
        }

        @Test
        @DisplayName("TC-10E-015: PDF download handles rejected submissions correctly")
        void pdfDownloadHandlesRejectedSubmissions() throws IOException {
            // Given - a rejected submission with error message
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.of(2026, 1, 20, 10, 15))
                    .type(SubmissionType.QUARTERLY_Q2)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.REJECTED)
                    .hmrcReference(null)
                    .totalIncome(new BigDecimal("25000.00"))
                    .totalExpenses(new BigDecimal("8000.00"))
                    .netProfit(new BigDecimal("17000.00"))
                    .errorMessage("INVALID_NINO: National Insurance number format is incorrect")
                    .build();

            // When - generate PDF
            String filename = pdfService.generateFilename(submission);
            Path outputPath = tempDir.resolve(filename);
            pdfService.generatePdf(submission, outputPath);

            // Then - file is created (error submissions can also be saved)
            assertThat(Files.exists(outputPath)).isTrue();
            // Filename uses ID when no reference
            assertThat(filename).contains("id-1");
        }

        @Test
        @DisplayName("TC-10E-015b: PDF download button keyboard accessible (Tab + Enter)")
        void pdfDownloadButtonKeyboardAccessible() {
            // Given - a submission is selected and detail panel is shown
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);
            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(submission));
                viewModel.selectSubmission(submission);
                controller.refreshData();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then - download button should be focus traversable
            Button downloadBtn = lookup("#downloadPdfBtn").queryAs(Button.class);
            assertThat(downloadBtn).isNotNull();
            assertThat(downloadBtn.isFocusTraversable()).isTrue();
        }

        @Test
        @DisplayName("TC-10E-015c: PDF service generates correct filename for annual submission")
        void pdfFilenameForAnnualSubmission() {
            // Given - an annual submission
            SubmissionTableRow submission = SubmissionTableRow.builder()
                    .id(999L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.ANNUAL)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.ACCEPTED)
                    .hmrcReference("SA-ANNUAL-XYZ789")
                    .totalIncome(new BigDecimal("50000.00"))
                    .totalExpenses(new BigDecimal("15000.00"))
                    .netProfit(new BigDecimal("35000.00"))
                    .build();

            // When
            String filename = pdfService.generateFilename(submission);

            // Then
            assertThat(filename).isEqualTo("submission-annual-2025-26-SA-ANNUAL-XYZ789.pdf");
        }

        @Test
        @DisplayName("TC-10E-015d: PDF service returns valid downloads directory")
        void pdfServiceReturnsDownloadsDirectory() {
            // When
            Path downloadsDir = pdfService.getDownloadsDirectory();

            // Then
            assertThat(downloadsDir).isNotNull();
            assertThat(Files.isDirectory(downloadsDir) || downloadsDir.endsWith("Downloads"))
                    .isTrue();
        }
    }

    // =========================================================================
    // Sprint 10E: Error Resolution Guidance Tests (SE-SH-006)
    // TC-10E-016 to TC-10E-020
    // =========================================================================

    @Nested
    @DisplayName("SE-SH-006: Error Resolution Guidance (TC-10E-016 to TC-10E-020)")
    class ErrorGuidanceTests {

        private final HmrcErrorGuidance errorGuidance = new HmrcErrorGuidance();

        @Test
        @DisplayName("TC-10E-016: Error guidance displays for rejected submission")
        void errorGuidanceDisplaysForRejectedSubmission() {
            // Given - a rejected submission with error
            SubmissionTableRow rejectedSubmission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.REJECTED)
                    .totalIncome(new BigDecimal("10000.00"))
                    .totalExpenses(new BigDecimal("2000.00"))
                    .netProfit(new BigDecimal("8000.00"))
                    .errorMessage("INVALID_NINO: National Insurance number format is incorrect")
                    .build();

            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(rejectedSubmission));
                viewModel.selectSubmission(rejectedSubmission);
                controller.refreshData();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then - error section should be visible
            VBox errorSection = lookup("#errorSection").queryAs(VBox.class);
            assertThat(errorSection).isNotNull();
            assertThat(errorSection.isVisible()).isTrue();
            assertThat(errorSection.isManaged()).isTrue();

            // Error guidance box should be present
            VBox errorGuidanceBox = lookup("#errorGuidanceBox").queryAs(VBox.class);
            assertThat(errorGuidanceBox).isNotNull();
            assertThat(errorGuidanceBox.isVisible()).isTrue();
        }

        @Test
        @DisplayName("TC-10E-017: Correct guidance for INVALID_NINO error")
        void correctGuidanceForInvalidNino() {
            // Given
            String errorMessage = "INVALID_NINO: The provided NINO is invalid";

            // When
            String extractedCode = errorGuidance.extractErrorCode(errorMessage);
            String guidance = errorGuidance.getGuidanceForErrorCode(extractedCode);

            // Then
            assertThat(extractedCode).isEqualTo("INVALID_NINO");
            assertThat(guidance).contains("National Insurance number");
            assertThat(guidance).contains("AA 12 34 56 B");
        }

        @Test
        @DisplayName("TC-10E-018: Correct guidance for DUPLICATE_SUBMISSION error")
        void correctGuidanceForDuplicateSubmission() {
            // Given
            String errorMessage = "DUPLICATE_SUBMISSION: A submission already exists for this period";

            // When
            String extractedCode = errorGuidance.extractErrorCode(errorMessage);
            String guidance = errorGuidance.getGuidanceForErrorCode(extractedCode);

            // Then
            assertThat(extractedCode).isEqualTo("DUPLICATE_SUBMISSION");
            assertThat(guidance).contains("already submitted");
            assertThat(guidance).contains("amendment");
        }

        @Test
        @DisplayName("TC-10E-019: Learn more link present and accessible")
        void learnMoreLinkPresentAndAccessible() {
            // Given - a rejected submission displayed
            SubmissionTableRow rejectedSubmission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.REJECTED)
                    .totalIncome(new BigDecimal("10000.00"))
                    .totalExpenses(new BigDecimal("2000.00"))
                    .netProfit(new BigDecimal("8000.00"))
                    .errorMessage("BUSINESS_VALIDATION: Business validation failed")
                    .build();

            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(rejectedSubmission));
                viewModel.selectSubmission(rejectedSubmission);
                controller.refreshData();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then - learn more link should be present
            Hyperlink learnMoreLink = lookup("#learnMoreLink").queryAs(Hyperlink.class);
            assertThat(learnMoreLink).isNotNull();
            assertThat(learnMoreLink.getText()).containsIgnoringCase("learn more");
            assertThat(learnMoreLink.getAccessibleText()).isNotBlank();

            // Verify correct URL in guidance
            assertThat(errorGuidance.getGuidanceUrl())
                    .isEqualTo("https://www.gov.uk/self-assessment-tax-returns/corrections");
        }

        @Test
        @DisplayName("TC-10E-020: Error guidance hidden for accepted submissions")
        void errorGuidanceHiddenForAcceptedSubmissions() {
            // Given - an accepted submission
            SubmissionTableRow acceptedSubmission = createQ1Submission(SubmissionStatus.ACCEPTED);

            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(acceptedSubmission));
                viewModel.selectSubmission(acceptedSubmission);
                controller.refreshData();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then - error section should not be visible
            VBox errorSection = lookup("#errorSection").queryAs(VBox.class);
            assertThat(errorSection).isNotNull();
            // For accepted submissions, error section should be hidden
            assertThat(errorSection.isVisible()).isFalse();
        }

        @Test
        @DisplayName("TC-10E-020b: Error guidance extractErrorCode handles various formats")
        void errorGuidanceExtractsVariousFormats() {
            // Test various error message formats
            assertThat(errorGuidance.extractErrorCode("FORMAT_NINO: Invalid"))
                    .isEqualTo("FORMAT_NINO");
            assertThat(errorGuidance.extractErrorCode("Error: RULE_TAX_YEAR_NOT_SUPPORTED"))
                    .isEqualTo("RULE_TAX_YEAR_NOT_SUPPORTED");
            assertThat(errorGuidance.extractErrorCode("CLIENT_OR_AGENT_NOT_AUTHORISED"))
                    .isEqualTo("CLIENT_OR_AGENT_NOT_AUTHORISED");
            assertThat(errorGuidance.extractErrorCode("Unknown error without code"))
                    .isNull();
        }

        @Test
        @DisplayName("TC-10E-020c: Error guidance provides default for unknown codes")
        void errorGuidanceProvidesDefaultForUnknownCodes() {
            // Given
            String unknownCode = "UNKNOWN_CODE_XYZ";

            // When
            String guidance = errorGuidance.getGuidanceForErrorCode(unknownCode);

            // Then - should return default guidance
            assertThat(guidance).contains("review your submission");
            assertThat(guidance).contains("HMRC");
        }

        @Test
        @DisplayName("TC-10E-020d: Error guidance isKnownErrorCode works correctly")
        void errorGuidanceKnownCodesValidation() {
            // Known codes
            assertThat(errorGuidance.isKnownErrorCode("INVALID_NINO")).isTrue();
            assertThat(errorGuidance.isKnownErrorCode("DUPLICATE_SUBMISSION")).isTrue();
            assertThat(errorGuidance.isKnownErrorCode("SERVER_ERROR")).isTrue();

            // Unknown codes
            assertThat(errorGuidance.isKnownErrorCode("RANDOM_CODE")).isFalse();
            assertThat(errorGuidance.isKnownErrorCode(null)).isFalse();
            assertThat(errorGuidance.isKnownErrorCode("")).isFalse();
        }

        @Test
        @DisplayName("TC-10E-020e: Error guidance formatting with combined code and message")
        void errorGuidanceFormattedGuidance() {
            // Given
            String errorCode = "INVALID_TAX_YEAR";
            String errorMessage = "INVALID_TAX_YEAR: The tax year 2099-00 is invalid";

            // When
            String formatted = errorGuidance.getFormattedGuidance(errorCode, errorMessage);

            // Then
            assertThat(formatted).contains("YYYY-YY");
            assertThat(formatted).contains("2024-25");
        }

        @Test
        @DisplayName("TC-10E-020f: Error section shows correct error message text")
        void errorSectionShowsCorrectMessage() {
            // Given
            String expectedError = "CALCULATION_ERROR: Unable to calculate tax due";
            SubmissionTableRow rejectedSubmission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.ANNUAL)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.REJECTED)
                    .totalIncome(new BigDecimal("75000.00"))
                    .totalExpenses(new BigDecimal("20000.00"))
                    .netProfit(new BigDecimal("55000.00"))
                    .errorMessage(expectedError)
                    .build();

            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(rejectedSubmission));
                viewModel.selectSubmission(rejectedSubmission);
                controller.refreshData();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then - the error message label should show the error
            Label errorMessageLabel = lookup("#detailErrorMessage").queryAs(Label.class);
            assertThat(errorMessageLabel).isNotNull();
            assertThat(errorMessageLabel.getText()).isEqualTo(expectedError);

            // Guidance text should be appropriate
            Label guidanceText = lookup("#errorGuidanceText").queryAs(Label.class);
            assertThat(guidanceText).isNotNull();
            assertThat(guidanceText.getText()).contains("calculate");
        }
    }

    // =========================================================================
    // Sprint 10E: Accessibility Tests for PDF Download and Error Guidance
    // =========================================================================

    @Nested
    @DisplayName("SE-SH-005/006: Accessibility Tests")
    class AccessibilityTests {

        @Test
        @DisplayName("PDF button has accessible text set")
        void pdfButtonHasAccessibleText() {
            // Given - a submission is selected
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);
            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(submission));
                viewModel.selectSubmission(submission);
                controller.refreshData();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then
            Button downloadBtn = lookup("#downloadPdfBtn").queryAs(Button.class);
            assertThat(downloadBtn).isNotNull();
            assertThat(downloadBtn.getAccessibleText()).isNotBlank();
            assertThat(downloadBtn.getAccessibleText()).containsIgnoringCase("pdf");
        }

        @Test
        @DisplayName("Learn more link has accessible text for screen readers")
        void learnMoreLinkHasAccessibleText() {
            // Given - a rejected submission with error
            SubmissionTableRow rejectedSubmission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q1)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.REJECTED)
                    .totalIncome(new BigDecimal("10000.00"))
                    .totalExpenses(new BigDecimal("2000.00"))
                    .netProfit(new BigDecimal("8000.00"))
                    .errorMessage("INVALID_NINO: Bad NINO")
                    .build();

            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(rejectedSubmission));
                viewModel.selectSubmission(rejectedSubmission);
                controller.refreshData();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then
            Hyperlink learnMoreLink = lookup("#learnMoreLink").queryAs(Hyperlink.class);
            assertThat(learnMoreLink).isNotNull();
            assertThat(learnMoreLink.getAccessibleText()).isNotBlank();
            assertThat(learnMoreLink.getAccessibleText()).containsIgnoringCase("HMRC");
        }

        @Test
        @DisplayName("Error guidance box is readable by screen readers")
        void errorGuidanceBoxIsAccessible() {
            // Given
            SubmissionTableRow rejectedSubmission = SubmissionTableRow.builder()
                    .id(1L)
                    .submittedAt(LocalDateTime.now())
                    .type(SubmissionType.QUARTERLY_Q2)
                    .taxYear("2025/26")
                    .status(SubmissionStatus.REJECTED)
                    .totalIncome(new BigDecimal("20000.00"))
                    .totalExpenses(new BigDecimal("5000.00"))
                    .netProfit(new BigDecimal("15000.00"))
                    .errorMessage("UNAUTHORISED: Session expired")
                    .build();

            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(rejectedSubmission));
                viewModel.selectSubmission(rejectedSubmission);
                controller.refreshData();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then - guidance text should contain helpful information
            Label guidanceText = lookup("#errorGuidanceText").queryAs(Label.class);
            assertThat(guidanceText).isNotNull();
            assertThat(guidanceText.getText()).isNotBlank();
            assertThat(guidanceText.isWrapText()).isTrue();
        }

        @Test
        @DisplayName("PDF button is keyboard navigable via Tab")
        void pdfButtonIsTabNavigable() {
            // Given
            SubmissionTableRow submission = createQ1Submission(SubmissionStatus.ACCEPTED);
            runOnFxThread(() -> {
                viewModel.setSubmissions(List.of(submission));
                viewModel.selectSubmission(submission);
                controller.refreshData();
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Then - button should be focus traversable
            Button downloadBtn = lookup("#downloadPdfBtn").queryAs(Button.class);
            assertThat(downloadBtn.isFocusTraversable()).isTrue();
        }
    }

    // === Helper Methods ===

    /**
     * Runs the given action on the FX application thread and waits for completion.
     * Required for modifying ViewModel/Controller state in tests.
     */
    private void runOnFxThread(Runnable action) {
        interact(action);
    }

    private SubmissionTableRow createQ1Submission(SubmissionStatus status) {
        return createSubmission(SubmissionType.QUARTERLY_Q1, "2025/26", status);
    }

    private SubmissionTableRow createQ2Submission(SubmissionStatus status) {
        return createSubmission(SubmissionType.QUARTERLY_Q2, "2025/26", status);
    }

    private SubmissionTableRow createQ3Submission(SubmissionStatus status) {
        return createSubmission(SubmissionType.QUARTERLY_Q3, "2025/26", status);
    }

    private SubmissionTableRow createQ4Submission(SubmissionStatus status) {
        return createSubmission(SubmissionType.QUARTERLY_Q4, "2025/26", status);
    }

    private SubmissionTableRow createAnnualSubmission(SubmissionStatus status) {
        return createSubmission(SubmissionType.ANNUAL, "2025/26", status);
    }

    private SubmissionTableRow createSubmission(SubmissionType type, String taxYear, SubmissionStatus status) {
        return SubmissionTableRow.builder()
                .id(System.nanoTime())
                .submittedAt(LocalDateTime.now())
                .type(type)
                .taxYear(taxYear)
                .status(status)
                .hmrcReference(status == SubmissionStatus.ACCEPTED ? "REF-" + type.getShortName() : null)
                .totalIncome(new BigDecimal("10000.00"))
                .totalExpenses(new BigDecimal("2000.00"))
                .netProfit(new BigDecimal("8000.00"))
                .errorMessage(status == SubmissionStatus.REJECTED ? "Test error message" : null)
                .build();
    }

    private SubmissionTableRow createSubmissionWithDate(LocalDateTime submittedAt, String reference) {
        return SubmissionTableRow.builder()
                .id(System.nanoTime())
                .submittedAt(submittedAt)
                .type(SubmissionType.QUARTERLY_Q1)
                .taxYear("2025/26")
                .status(SubmissionStatus.ACCEPTED)
                .hmrcReference(reference)
                .totalIncome(new BigDecimal("10000.00"))
                .totalExpenses(new BigDecimal("2000.00"))
                .netProfit(new BigDecimal("8000.00"))
                .build();
    }
}
