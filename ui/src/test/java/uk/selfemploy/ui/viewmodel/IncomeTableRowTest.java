package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.IncomeCategory;
import uk.selfemploy.common.enums.IncomeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for IncomeTableRow display model.
 */
@DisplayName("IncomeTableRow")
class IncomeTableRowTest {

    private static final UUID BUSINESS_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("should create from Income with client name and status")
        void shouldCreateFromIncomeWithClientNameAndStatus() {
            // Given
            Income income = new Income(
                UUID.randomUUID(),
                BUSINESS_ID,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("2500.00"),
                "Website redesign",
                IncomeCategory.SALES,
                "INV-001"
            );

            // When
            IncomeTableRow row = IncomeTableRow.fromIncome(income, "Acme Corp", IncomeStatus.PAID);

            // Then
            assertThat(row.id()).isEqualTo(income.id());
            assertThat(row.date()).isEqualTo(LocalDate.of(2025, 6, 15));
            assertThat(row.clientName()).isEqualTo("Acme Corp");
            assertThat(row.description()).isEqualTo("Website redesign");
            assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(row.status()).isEqualTo(IncomeStatus.PAID);
            assertThat(row.category()).isEqualTo(IncomeCategory.SALES);
            assertThat(row.reference()).isEqualTo("INV-001");
        }

        @Test
        @DisplayName("should create from Income with default values")
        void shouldCreateFromIncomeWithDefaultValues() {
            // Given
            Income income = new Income(
                UUID.randomUUID(),
                BUSINESS_ID,
                LocalDate.of(2025, 6, 15),
                new BigDecimal("1000.00"),
                "Consulting services",
                IncomeCategory.SALES,
                null
            );

            // When
            IncomeTableRow row = IncomeTableRow.fromIncome(income);

            // Then
            assertThat(row.id()).isEqualTo(income.id());
            assertThat(row.clientName()).isNotEmpty();
            assertThat(row.status()).isEqualTo(IncomeStatus.PAID);
        }
    }

    @Nested
    @DisplayName("Formatting")
    class Formatting {

        @Test
        @DisplayName("should format date as 'd MMM 'yy'")
        void shouldFormatDate() {
            // Given
            IncomeTableRow row = createRow(LocalDate.of(2025, 1, 10));

            // Then
            assertThat(row.getFormattedDate()).isEqualTo("10 Jan '25");
        }

        @Test
        @DisplayName("should format amount as GBP currency")
        void shouldFormatAmount() {
            // Given
            IncomeTableRow row = createRowWithAmount(new BigDecimal("2500.00"));

            // Then
            assertThat(row.getFormattedAmount()).isEqualTo("£2,500.00");
        }

        @Test
        @DisplayName("should format large amounts with commas")
        void shouldFormatLargeAmounts() {
            // Given
            IncomeTableRow row = createRowWithAmount(new BigDecimal("1234567.89"));

            // Then
            assertThat(row.getFormattedAmount()).isEqualTo("£1,234,567.89");
        }

        @Test
        @DisplayName("should format category with box number")
        void shouldFormatCategory() {
            // Given
            IncomeTableRow row = createRowWithCategory(IncomeCategory.SALES);

            // Then
            assertThat(row.getCategoryDisplay()).isEqualTo("Turnover from business (Box 9)");
        }

        @Test
        @DisplayName("should format other income category")
        void shouldFormatOtherIncomeCategory() {
            // Given
            IncomeTableRow row = createRowWithCategory(IncomeCategory.OTHER_INCOME);

            // Then
            assertThat(row.getCategoryDisplay()).isEqualTo("Other business income (Box 10)");
        }

        @Test
        @DisplayName("should format status display name")
        void shouldFormatStatusDisplayName() {
            // Given
            IncomeTableRow paidRow = createRowWithStatus(IncomeStatus.PAID);
            IncomeTableRow unpaidRow = createRowWithStatus(IncomeStatus.UNPAID);

            // Then
            assertThat(paidRow.getStatusDisplay()).isEqualTo("Paid");
            assertThat(unpaidRow.getStatusDisplay()).isEqualTo("Unpaid");
        }
    }

    @Nested
    @DisplayName("Search Matching")
    class SearchMatching {

        @Test
        @DisplayName("should match client name")
        void shouldMatchClientName() {
            // Given
            IncomeTableRow row = createRowWithClientAndDescription("Acme Corp", "Website design");

            // Then
            assertThat(row.matchesSearch("Acme")).isTrue();
            assertThat(row.matchesSearch("acme")).isTrue(); // case insensitive
            assertThat(row.matchesSearch("Corp")).isTrue();
        }

        @Test
        @DisplayName("should match description")
        void shouldMatchDescription() {
            // Given
            IncomeTableRow row = createRowWithClientAndDescription("Acme Corp", "Website design");

            // Then
            assertThat(row.matchesSearch("Website")).isTrue();
            assertThat(row.matchesSearch("design")).isTrue();
        }

        @Test
        @DisplayName("should not match unrelated query")
        void shouldNotMatchUnrelatedQuery() {
            // Given
            IncomeTableRow row = createRowWithClientAndDescription("Acme Corp", "Website design");

            // Then
            assertThat(row.matchesSearch("NotFound")).isFalse();
        }

        @Test
        @DisplayName("should match all for empty query")
        void shouldMatchAllForEmptyQuery() {
            // Given
            IncomeTableRow row = createRowWithClientAndDescription("Acme Corp", "Website design");

            // Then
            assertThat(row.matchesSearch("")).isTrue();
            assertThat(row.matchesSearch(null)).isTrue();
        }
    }

    @Nested
    @DisplayName("Status Matching")
    class StatusMatching {

        @Test
        @DisplayName("should match paid status")
        void shouldMatchPaidStatus() {
            // Given
            IncomeTableRow row = createRowWithStatus(IncomeStatus.PAID);

            // Then
            assertThat(row.matchesStatus(IncomeStatus.PAID)).isTrue();
            assertThat(row.matchesStatus(IncomeStatus.UNPAID)).isFalse();
        }

        @Test
        @DisplayName("should match unpaid status")
        void shouldMatchUnpaidStatus() {
            // Given
            IncomeTableRow row = createRowWithStatus(IncomeStatus.UNPAID);

            // Then
            assertThat(row.matchesStatus(IncomeStatus.UNPAID)).isTrue();
            assertThat(row.matchesStatus(IncomeStatus.PAID)).isFalse();
        }

        @Test
        @DisplayName("should match all for null status filter")
        void shouldMatchAllForNullStatusFilter() {
            // Given
            IncomeTableRow paidRow = createRowWithStatus(IncomeStatus.PAID);
            IncomeTableRow unpaidRow = createRowWithStatus(IncomeStatus.UNPAID);

            // Then
            assertThat(paidRow.matchesStatus(null)).isTrue();
            assertThat(unpaidRow.matchesStatus(null)).isTrue();
        }
    }

    // === Helper Methods ===

    private IncomeTableRow createRow(LocalDate date) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            date,
            "Test Client",
            "Test description",
            new BigDecimal("1000.00"),
            IncomeStatus.PAID,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createRowWithAmount(BigDecimal amount) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.now(),
            "Test Client",
            "Test description",
            amount,
            IncomeStatus.PAID,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createRowWithCategory(IncomeCategory category) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.now(),
            "Test Client",
            "Test description",
            new BigDecimal("1000.00"),
            IncomeStatus.PAID,
            category,
            null
        );
    }

    private IncomeTableRow createRowWithStatus(IncomeStatus status) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.now(),
            "Test Client",
            "Test description",
            new BigDecimal("1000.00"),
            status,
            IncomeCategory.SALES,
            null
        );
    }

    private IncomeTableRow createRowWithClientAndDescription(String clientName, String description) {
        return new IncomeTableRow(
            UUID.randomUUID(),
            LocalDate.now(),
            clientName,
            description,
            new BigDecimal("1000.00"),
            IncomeStatus.PAID,
            IncomeCategory.SALES,
            null
        );
    }
}
