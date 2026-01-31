package uk.selfemploy.ui.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;
import uk.selfemploy.ui.viewmodel.SubmissionTableRow;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SubmissionRecord.
 * Verifies record construction, conversion to SubmissionTableRow,
 * and tax year formatting.
 */
@DisplayName("SubmissionRecord")
class SubmissionRecordTest {

    private static final String TEST_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String TEST_BUSINESS_ID = "987e6543-e21b-12d3-a456-426614174000";
    private static final Instant TEST_SUBMITTED_AT = Instant.parse("2026-01-15T10:30:00Z");

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("should create record with all fields")
        void shouldCreateRecordWithAllFields() {
            SubmissionRecord record = new SubmissionRecord(
                TEST_ID,
                TEST_BUSINESS_ID,
                "QUARTERLY_Q1",
                2025,
                LocalDate.of(2025, 4, 6),
                LocalDate.of(2025, 7, 5),
                new BigDecimal("10000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("8000.00"),
                "ACCEPTED",
                "HMRC-REF-123",
                null,
                TEST_SUBMITTED_AT
            );

            assertThat(record.id()).isEqualTo(TEST_ID);
            assertThat(record.businessId()).isEqualTo(TEST_BUSINESS_ID);
            assertThat(record.type()).isEqualTo("QUARTERLY_Q1");
            assertThat(record.taxYearStart()).isEqualTo(2025);
            assertThat(record.periodStart()).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(record.periodEnd()).isEqualTo(LocalDate.of(2025, 7, 5));
            assertThat(record.totalIncome()).isEqualByComparingTo("10000.00");
            assertThat(record.totalExpenses()).isEqualByComparingTo("2000.00");
            assertThat(record.netProfit()).isEqualByComparingTo("8000.00");
            assertThat(record.status()).isEqualTo("ACCEPTED");
            assertThat(record.hmrcReference()).isEqualTo("HMRC-REF-123");
            assertThat(record.errorMessage()).isNull();
            assertThat(record.submittedAt()).isEqualTo(TEST_SUBMITTED_AT);
        }

        @Test
        @DisplayName("should allow null optional fields")
        void shouldAllowNullOptionalFields() {
            SubmissionRecord record = new SubmissionRecord(
                TEST_ID,
                TEST_BUSINESS_ID,
                "QUARTERLY_Q2",
                2025,
                LocalDate.of(2025, 7, 6),
                LocalDate.of(2025, 10, 5),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("4000.00"),
                "PENDING",
                null,  // hmrcReference is nullable
                null,  // errorMessage is nullable
                TEST_SUBMITTED_AT
            );

            assertThat(record.hmrcReference()).isNull();
            assertThat(record.errorMessage()).isNull();
        }

        @Test
        @DisplayName("should allow error message for rejected submissions")
        void shouldAllowErrorMessageForRejectedSubmissions() {
            SubmissionRecord record = new SubmissionRecord(
                TEST_ID,
                TEST_BUSINESS_ID,
                "QUARTERLY_Q3",
                2025,
                LocalDate.of(2025, 10, 6),
                LocalDate.of(2026, 1, 5),
                new BigDecimal("7000.00"),
                new BigDecimal("1500.00"),
                new BigDecimal("5500.00"),
                "REJECTED",
                null,
                "RULE_VIOLATION: Invalid period dates",
                TEST_SUBMITTED_AT
            );

            assertThat(record.status()).isEqualTo("REJECTED");
            assertThat(record.errorMessage()).isEqualTo("RULE_VIOLATION: Invalid period dates");
        }
    }

    @Nested
    @DisplayName("Tax Year Formatting")
    class TaxYearFormattingTests {

        @Test
        @DisplayName("should format tax year 2025 as '2025/26'")
        void shouldFormatTaxYear2025() {
            SubmissionRecord record = createRecord(2025);

            assertThat(record.getFormattedTaxYear()).isEqualTo("2025/26");
        }

        @Test
        @DisplayName("should format tax year 2024 as '2024/25'")
        void shouldFormatTaxYear2024() {
            SubmissionRecord record = createRecord(2024);

            assertThat(record.getFormattedTaxYear()).isEqualTo("2024/25");
        }

        @Test
        @DisplayName("should format tax year 2030 as '2030/31'")
        void shouldFormatTaxYear2030() {
            SubmissionRecord record = createRecord(2030);

            assertThat(record.getFormattedTaxYear()).isEqualTo("2030/31");
        }

        private SubmissionRecord createRecord(int taxYearStart) {
            return new SubmissionRecord(
                TEST_ID, TEST_BUSINESS_ID, "QUARTERLY_Q1", taxYearStart,
                LocalDate.of(taxYearStart, 4, 6), LocalDate.of(taxYearStart, 7, 5),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "ACCEPTED", null, null, TEST_SUBMITTED_AT
            );
        }
    }

    @Nested
    @DisplayName("Conversion to SubmissionTableRow")
    class ToTableRowTests {

        @Test
        @DisplayName("should convert to SubmissionTableRow with all fields")
        void shouldConvertToTableRowWithAllFields() {
            SubmissionRecord record = new SubmissionRecord(
                TEST_ID,
                TEST_BUSINESS_ID,
                "QUARTERLY_Q1",
                2025,
                LocalDate.of(2025, 4, 6),
                LocalDate.of(2025, 7, 5),
                new BigDecimal("10000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("8000.00"),
                "ACCEPTED",
                "HMRC-REF-123",
                null,
                TEST_SUBMITTED_AT
            );

            SubmissionTableRow row = record.toTableRow();

            assertThat(row.type()).isEqualTo(SubmissionType.QUARTERLY_Q1);
            assertThat(row.taxYear()).isEqualTo("2025/26");
            assertThat(row.status()).isEqualTo(SubmissionStatus.ACCEPTED);
            assertThat(row.hmrcReference()).isEqualTo("HMRC-REF-123");
            assertThat(row.totalIncome()).isEqualByComparingTo("10000.00");
            assertThat(row.totalExpenses()).isEqualByComparingTo("2000.00");
            assertThat(row.netProfit()).isEqualByComparingTo("8000.00");
            assertThat(row.errorMessage()).isNull();
        }

        @Test
        @DisplayName("should convert submittedAt Instant to LocalDateTime")
        void shouldConvertSubmittedAtToLocalDateTime() {
            Instant instant = Instant.parse("2026-01-15T10:30:00Z");
            SubmissionRecord record = new SubmissionRecord(
                TEST_ID, TEST_BUSINESS_ID, "QUARTERLY_Q2", 2025,
                LocalDate.of(2025, 7, 6), LocalDate.of(2025, 10, 5),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "SUBMITTED", null, null, instant
            );

            SubmissionTableRow row = record.toTableRow();

            // Should convert to system default timezone
            LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            assertThat(row.submittedAt()).isEqualTo(expected);
        }

        @Test
        @DisplayName("should handle ANNUAL submission type")
        void shouldHandleAnnualSubmissionType() {
            SubmissionRecord record = new SubmissionRecord(
                TEST_ID, TEST_BUSINESS_ID, "ANNUAL", 2024,
                LocalDate.of(2024, 4, 6), LocalDate.of(2025, 4, 5),
                new BigDecimal("50000.00"), new BigDecimal("10000.00"), new BigDecimal("40000.00"),
                "ACCEPTED", "SA-2024-REF", null, TEST_SUBMITTED_AT
            );

            SubmissionTableRow row = record.toTableRow();

            assertThat(row.type()).isEqualTo(SubmissionType.ANNUAL);
            assertThat(row.taxYear()).isEqualTo("2024/25");
        }

        @Test
        @DisplayName("should handle REJECTED status with error message")
        void shouldHandleRejectedStatusWithErrorMessage() {
            SubmissionRecord record = new SubmissionRecord(
                TEST_ID, TEST_BUSINESS_ID, "QUARTERLY_Q4", 2025,
                LocalDate.of(2026, 1, 6), LocalDate.of(2026, 4, 5),
                new BigDecimal("3000.00"), new BigDecimal("500.00"), new BigDecimal("2500.00"),
                "REJECTED", null, "FORMAT_VALUE: Invalid income format", TEST_SUBMITTED_AT
            );

            SubmissionTableRow row = record.toTableRow();

            assertThat(row.status()).isEqualTo(SubmissionStatus.REJECTED);
            assertThat(row.errorMessage()).isEqualTo("FORMAT_VALUE: Invalid income format");
            assertThat(row.hmrcReference()).isNull();
        }

        @Test
        @DisplayName("should handle all quarterly types")
        void shouldHandleAllQuarterlyTypes() {
            assertThat(createRecord("QUARTERLY_Q1").toTableRow().type()).isEqualTo(SubmissionType.QUARTERLY_Q1);
            assertThat(createRecord("QUARTERLY_Q2").toTableRow().type()).isEqualTo(SubmissionType.QUARTERLY_Q2);
            assertThat(createRecord("QUARTERLY_Q3").toTableRow().type()).isEqualTo(SubmissionType.QUARTERLY_Q3);
            assertThat(createRecord("QUARTERLY_Q4").toTableRow().type()).isEqualTo(SubmissionType.QUARTERLY_Q4);
        }

        @Test
        @DisplayName("should handle all status types")
        void shouldHandleAllStatusTypes() {
            assertThat(createRecordWithStatus("PENDING").toTableRow().status()).isEqualTo(SubmissionStatus.PENDING);
            assertThat(createRecordWithStatus("SUBMITTED").toTableRow().status()).isEqualTo(SubmissionStatus.SUBMITTED);
            assertThat(createRecordWithStatus("ACCEPTED").toTableRow().status()).isEqualTo(SubmissionStatus.ACCEPTED);
            assertThat(createRecordWithStatus("REJECTED").toTableRow().status()).isEqualTo(SubmissionStatus.REJECTED);
        }

        private SubmissionRecord createRecord(String type) {
            return new SubmissionRecord(
                TEST_ID, TEST_BUSINESS_ID, type, 2025,
                LocalDate.of(2025, 4, 6), LocalDate.of(2025, 7, 5),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "ACCEPTED", null, null, TEST_SUBMITTED_AT
            );
        }

        private SubmissionRecord createRecordWithStatus(String status) {
            return new SubmissionRecord(
                TEST_ID, TEST_BUSINESS_ID, "QUARTERLY_Q1", 2025,
                LocalDate.of(2025, 4, 6), LocalDate.of(2025, 7, 5),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                status, null, null, TEST_SUBMITTED_AT
            );
        }
    }

    @Nested
    @DisplayName("Type and Status Enums")
    class EnumTests {

        @Test
        @DisplayName("getSubmissionType should parse all valid types")
        void shouldParseAllValidTypes() {
            assertThat(SubmissionRecord.getSubmissionType("QUARTERLY_Q1")).isEqualTo(SubmissionType.QUARTERLY_Q1);
            assertThat(SubmissionRecord.getSubmissionType("QUARTERLY_Q2")).isEqualTo(SubmissionType.QUARTERLY_Q2);
            assertThat(SubmissionRecord.getSubmissionType("QUARTERLY_Q3")).isEqualTo(SubmissionType.QUARTERLY_Q3);
            assertThat(SubmissionRecord.getSubmissionType("QUARTERLY_Q4")).isEqualTo(SubmissionType.QUARTERLY_Q4);
            assertThat(SubmissionRecord.getSubmissionType("ANNUAL")).isEqualTo(SubmissionType.ANNUAL);
        }

        @Test
        @DisplayName("getSubmissionStatus should parse all valid statuses")
        void shouldParseAllValidStatuses() {
            assertThat(SubmissionRecord.getSubmissionStatus("PENDING")).isEqualTo(SubmissionStatus.PENDING);
            assertThat(SubmissionRecord.getSubmissionStatus("SUBMITTED")).isEqualTo(SubmissionStatus.SUBMITTED);
            assertThat(SubmissionRecord.getSubmissionStatus("ACCEPTED")).isEqualTo(SubmissionStatus.ACCEPTED);
            assertThat(SubmissionRecord.getSubmissionStatus("REJECTED")).isEqualTo(SubmissionStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("Factory Method fromDomainSubmission")
    class FromDomainSubmissionTests {

        private static final UUID SUBMISSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        private static final UUID BUSINESS_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

        @Test
        @DisplayName("should convert accepted quarterly submission to SubmissionRecord")
        void shouldConvertAcceptedQuarterlySubmission() {
            Submission domainSubmission = new Submission(
                SUBMISSION_ID,
                BUSINESS_ID,
                SubmissionType.QUARTERLY_Q1,
                TaxYear.of(2025),
                LocalDate.of(2025, 4, 6),
                LocalDate.of(2025, 7, 5),
                new BigDecimal("10000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("8000.00"),
                SubmissionStatus.ACCEPTED,
                "HMRC-REF-123",
                null,
                Instant.parse("2026-01-15T10:30:00Z"),
                Instant.now(),
                Instant.now(),
                "hash123",
                null,
                "QQ123456C"
            );

            SubmissionRecord record = SubmissionRecord.fromDomainSubmission(domainSubmission);

            assertThat(record.id()).isEqualTo(SUBMISSION_ID.toString());
            assertThat(record.businessId()).isEqualTo(BUSINESS_ID.toString());
            assertThat(record.type()).isEqualTo("QUARTERLY_Q1");
            assertThat(record.taxYearStart()).isEqualTo(2025);
            assertThat(record.periodStart()).isEqualTo(LocalDate.of(2025, 4, 6));
            assertThat(record.periodEnd()).isEqualTo(LocalDate.of(2025, 7, 5));
            assertThat(record.totalIncome()).isEqualByComparingTo("10000.00");
            assertThat(record.totalExpenses()).isEqualByComparingTo("2000.00");
            assertThat(record.netProfit()).isEqualByComparingTo("8000.00");
            assertThat(record.status()).isEqualTo("ACCEPTED");
            assertThat(record.hmrcReference()).isEqualTo("HMRC-REF-123");
            assertThat(record.errorMessage()).isNull();
            assertThat(record.submittedAt()).isEqualTo(Instant.parse("2026-01-15T10:30:00Z"));
        }

        @Test
        @DisplayName("should convert rejected submission with error message")
        void shouldConvertRejectedSubmissionWithErrorMessage() {
            Submission domainSubmission = new Submission(
                SUBMISSION_ID,
                BUSINESS_ID,
                SubmissionType.QUARTERLY_Q2,
                TaxYear.of(2025),
                LocalDate.of(2025, 7, 6),
                LocalDate.of(2025, 10, 5),
                new BigDecimal("5000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("4000.00"),
                SubmissionStatus.REJECTED,
                null,
                "FORMAT_VALUE: Invalid income format",
                Instant.parse("2026-01-20T14:00:00Z"),
                Instant.now(),
                Instant.now(),
                "hash456",
                null,
                null
            );

            SubmissionRecord record = SubmissionRecord.fromDomainSubmission(domainSubmission);

            assertThat(record.status()).isEqualTo("REJECTED");
            assertThat(record.hmrcReference()).isNull();
            assertThat(record.errorMessage()).isEqualTo("FORMAT_VALUE: Invalid income format");
        }

        @Test
        @DisplayName("should convert annual submission")
        void shouldConvertAnnualSubmission() {
            Submission domainSubmission = new Submission(
                SUBMISSION_ID,
                BUSINESS_ID,
                SubmissionType.ANNUAL,
                TaxYear.of(2024),
                LocalDate.of(2024, 4, 6),
                LocalDate.of(2025, 4, 5),
                new BigDecimal("50000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("40000.00"),
                SubmissionStatus.ACCEPTED,
                "SA-2024-REF",
                null,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                "hash789",
                "1234567890",
                "QQ123456C"
            );

            SubmissionRecord record = SubmissionRecord.fromDomainSubmission(domainSubmission);

            assertThat(record.type()).isEqualTo("ANNUAL");
            assertThat(record.taxYearStart()).isEqualTo(2024);
            assertThat(record.hmrcReference()).isEqualTo("SA-2024-REF");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null submission")
        void shouldThrowForNullSubmission() {
            assertThatThrownBy(() -> SubmissionRecord.fromDomainSubmission(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("submission");
        }

        @Test
        @DisplayName("should convert all quarterly types correctly")
        void shouldConvertAllQuarterlyTypes() {
            for (SubmissionType type : SubmissionType.values()) {
                Submission domainSubmission = new Submission(
                    UUID.randomUUID(),
                    BUSINESS_ID,
                    type,
                    TaxYear.of(2025),
                    LocalDate.of(2025, 4, 6),
                    LocalDate.of(2025, 7, 5),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    SubmissionStatus.PENDING,
                    null,
                    null,
                    Instant.now(),
                    Instant.now(),
                    null,
                    null,
                    null,
                    null
                );

                SubmissionRecord record = SubmissionRecord.fromDomainSubmission(domainSubmission);

                assertThat(record.type()).isEqualTo(type.name());
            }
        }
    }
}
