package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.TaxYear;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HmrcSubmissionController.
 * Tests the controller logic for the HMRC Submission hub page.
 */
@DisplayName("HmrcSubmissionController")
class HmrcSubmissionControllerTest {

    private HmrcSubmissionController controller;
    private TaxYear taxYear;

    @BeforeEach
    void setUp() {
        controller = new HmrcSubmissionController();
        taxYear = TaxYear.of(2025);
    }

    @Test
    @DisplayName("should implement TaxYearAware interface")
    void shouldImplementTaxYearAware() {
        assertThat(controller).isInstanceOf(MainController.TaxYearAware.class);
    }

    @Test
    @DisplayName("should implement Initializable interface")
    void shouldImplementInitializable() {
        assertThat(controller).isInstanceOf(javafx.fxml.Initializable.class);
    }

    @Nested
    @DisplayName("Tax Year Management")
    class TaxYearManagement {

        @Test
        @DisplayName("should store tax year when set")
        void shouldStoreTaxYear() {
            // When
            controller.setTaxYear(taxYear);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(taxYear);
        }

        @Test
        @DisplayName("should handle null tax year gracefully")
        void shouldHandleNullTaxYear() {
            // When
            controller.setTaxYear(null);

            // Then
            assertThat(controller.getTaxYear()).isNull();
        }

        @Test
        @DisplayName("should update tax year when changed")
        void shouldUpdateTaxYear() {
            // Given
            TaxYear year2024 = TaxYear.of(2024);
            controller.setTaxYear(year2024);

            // When
            controller.setTaxYear(taxYear);

            // Then
            assertThat(controller.getTaxYear()).isEqualTo(taxYear);
        }
    }

    @Nested
    @DisplayName("Quarter Status Determination")
    class QuarterStatusDetermination {

        @Test
        @DisplayName("should return Q1 status for April")
        void shouldReturnQ1ForApril() {
            LocalDate aprilDate = LocalDate.of(2025, 4, 15);
            String status = controller.determineQuarterStatus(aprilDate, taxYear);

            assertThat(status).contains("Q1");
            assertThat(status).contains("Apr-Jun");
            assertThat(status).contains("7 Aug");
        }

        @Test
        @DisplayName("should return Q1 status for May")
        void shouldReturnQ1ForMay() {
            LocalDate mayDate = LocalDate.of(2025, 5, 15);
            String status = controller.determineQuarterStatus(mayDate, taxYear);

            assertThat(status).contains("Q1");
        }

        @Test
        @DisplayName("should return Q1 status for June")
        void shouldReturnQ1ForJune() {
            LocalDate juneDate = LocalDate.of(2025, 6, 30);
            String status = controller.determineQuarterStatus(juneDate, taxYear);

            assertThat(status).contains("Q1");
        }

        @Test
        @DisplayName("should return Q2 status for July")
        void shouldReturnQ2ForJuly() {
            LocalDate julyDate = LocalDate.of(2025, 7, 1);
            String status = controller.determineQuarterStatus(julyDate, taxYear);

            assertThat(status).contains("Q2");
            assertThat(status).contains("Jul-Sep");
            assertThat(status).contains("7 Nov");
        }

        @Test
        @DisplayName("should return Q2 status for August")
        void shouldReturnQ2ForAugust() {
            LocalDate augDate = LocalDate.of(2025, 8, 15);
            String status = controller.determineQuarterStatus(augDate, taxYear);

            assertThat(status).contains("Q2");
        }

        @Test
        @DisplayName("should return Q2 status for September")
        void shouldReturnQ2ForSeptember() {
            LocalDate septDate = LocalDate.of(2025, 9, 30);
            String status = controller.determineQuarterStatus(septDate, taxYear);

            assertThat(status).contains("Q2");
        }

        @Test
        @DisplayName("should return Q3 status for October")
        void shouldReturnQ3ForOctober() {
            LocalDate octDate = LocalDate.of(2025, 10, 1);
            String status = controller.determineQuarterStatus(octDate, taxYear);

            assertThat(status).contains("Q3");
            assertThat(status).contains("Oct-Dec");
            assertThat(status).contains("7 Feb");
        }

        @Test
        @DisplayName("should return Q3 status for November")
        void shouldReturnQ3ForNovember() {
            LocalDate novDate = LocalDate.of(2025, 11, 15);
            String status = controller.determineQuarterStatus(novDate, taxYear);

            assertThat(status).contains("Q3");
        }

        @Test
        @DisplayName("should return Q3 status for December")
        void shouldReturnQ3ForDecember() {
            LocalDate decDate = LocalDate.of(2025, 12, 31);
            String status = controller.determineQuarterStatus(decDate, taxYear);

            assertThat(status).contains("Q3");
        }

        @Test
        @DisplayName("should return Q4 status for January")
        void shouldReturnQ4ForJanuary() {
            LocalDate janDate = LocalDate.of(2026, 1, 15);
            String status = controller.determineQuarterStatus(janDate, taxYear);

            assertThat(status).contains("Q4");
            assertThat(status).contains("Jan-Mar");
            assertThat(status).contains("7 May");
        }

        @Test
        @DisplayName("should return Q4 status for February")
        void shouldReturnQ4ForFebruary() {
            LocalDate febDate = LocalDate.of(2026, 2, 15);
            String status = controller.determineQuarterStatus(febDate, taxYear);

            assertThat(status).contains("Q4");
        }

        @Test
        @DisplayName("should return Q4 status for March")
        void shouldReturnQ4ForMarch() {
            LocalDate marchDate = LocalDate.of(2026, 3, 31);
            String status = controller.determineQuarterStatus(marchDate, taxYear);

            assertThat(status).contains("Q4");
        }
    }

    @Nested
    @DisplayName("Connection State")
    class ConnectionState {

        @Test
        @DisplayName("should be disconnected initially")
        void shouldBeDisconnectedInitially() {
            assertThat(controller.isConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("Annual Deadline Display")
    class AnnualDeadlineDisplay {

        @Test
        @DisplayName("should return empty string when no tax year set")
        void shouldReturnEmptyWhenNoTaxYear() {
            // When - tax year not set
            String deadline = controller.getFormattedAnnualDeadline();

            // Then
            assertThat(deadline).isEmpty();
        }

        @Test
        @DisplayName("should format deadline correctly for tax year 2025/26")
        void shouldFormatDeadlineCorrectly() {
            // Given
            controller.setTaxYear(taxYear);

            // When
            String deadline = controller.getFormattedAnnualDeadline();

            // Then
            assertThat(deadline).isEqualTo("Deadline: 31 January 2027");
        }

        @Test
        @DisplayName("should format deadline correctly for tax year 2024/25")
        void shouldFormatDeadlineForPreviousYear() {
            // Given
            TaxYear year2024 = TaxYear.of(2024);
            controller.setTaxYear(year2024);

            // When
            String deadline = controller.getFormattedAnnualDeadline();

            // Then
            assertThat(deadline).isEqualTo("Deadline: 31 January 2026");
        }

        @Test
        @DisplayName("should use correct date format")
        void shouldUseCorrectDateFormat() {
            // Given
            controller.setTaxYear(taxYear);

            // When
            String deadline = controller.getFormattedAnnualDeadline();

            // Then - should use "d MMMM yyyy" format
            assertThat(deadline).matches("Deadline: \\d{1,2} [A-Z][a-z]+ \\d{4}");
        }
    }

    @Nested
    @DisplayName("Tax Year Deadline Calculation")
    class TaxYearDeadlineCalculation {

        @Test
        @DisplayName("should calculate online filing deadline as 31 January following tax year end")
        void shouldCalculateOnlineFilingDeadline() {
            // Tax year 2025/26 ends 5 April 2026
            // Online filing deadline is 31 January 2027
            TaxYear year2025 = TaxYear.of(2025);

            assertThat(year2025.onlineFilingDeadline()).isEqualTo(LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("should calculate deadline for tax year 2024/25")
        void shouldCalculateDeadlineFor2024() {
            // Tax year 2024/25 ends 5 April 2025
            // Online filing deadline is 31 January 2026
            TaxYear year2024 = TaxYear.of(2024);

            assertThat(year2024.onlineFilingDeadline()).isEqualTo(LocalDate.of(2026, 1, 31));
        }
    }
}
