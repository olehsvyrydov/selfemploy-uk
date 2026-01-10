package uk.selfemploy.hmrc.client.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for BusinessDetails DTO.
 */
@DisplayName("BusinessDetails")
class BusinessDetailsTest {

    @Nested
    @DisplayName("isActive")
    class IsActive {

        @Test
        @DisplayName("should return true when cessationDate is null")
        void shouldReturnTrueWhenCessationDateIsNull() {
            // Given
            BusinessDetails business = createBusinessDetails(null);

            // When/Then
            assertThat(business.isActive()).isTrue();
        }

        @Test
        @DisplayName("should return false when cessationDate is set")
        void shouldReturnFalseWhenCessationDateIsSet() {
            // Given
            BusinessDetails business = createBusinessDetails(LocalDate.of(2023, 3, 31));

            // When/Then
            assertThat(business.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("getFormattedAddress")
    class GetFormattedAddress {

        @Test
        @DisplayName("should format full address with all fields")
        void shouldFormatFullAddressWithAllFields() {
            // Given
            BusinessDetails business = createBusinessDetailsWithAddress(
                "123 Business Street",
                "Suite 100",
                "London",
                null,
                "SW1A 1AA"
            );

            // When
            String formatted = business.getFormattedAddress();

            // Then
            assertThat(formatted).isEqualTo("123 Business Street, Suite 100, London SW1A 1AA");
        }

        @Test
        @DisplayName("should format address with only first line and postcode")
        void shouldFormatAddressWithOnlyFirstLineAndPostcode() {
            // Given
            BusinessDetails business = createBusinessDetailsWithAddress(
                "123 Business Street",
                null,
                null,
                null,
                "SW1A 1AA"
            );

            // When
            String formatted = business.getFormattedAddress();

            // Then
            assertThat(formatted).isEqualTo("123 Business Street SW1A 1AA");
        }

        @Test
        @DisplayName("should format address without postcode")
        void shouldFormatAddressWithoutPostcode() {
            // Given
            BusinessDetails business = createBusinessDetailsWithAddress(
                "123 Business Street",
                "London",
                null,
                null,
                null
            );

            // When
            String formatted = business.getFormattedAddress();

            // Then
            assertThat(formatted).isEqualTo("123 Business Street, London");
        }

        @Test
        @DisplayName("should return empty string when all address fields are null")
        void shouldReturnEmptyStringWhenAllAddressFieldsAreNull() {
            // Given
            BusinessDetails business = createBusinessDetailsWithAddress(
                null, null, null, null, null
            );

            // When
            String formatted = business.getFormattedAddress();

            // Then
            assertThat(formatted).isEmpty();
        }
    }

    @Nested
    @DisplayName("AccountingPeriod")
    class AccountingPeriodTests {

        @Test
        @DisplayName("should store start and end dates")
        void shouldStoreStartAndEndDates() {
            // Given
            LocalDate start = LocalDate.of(2023, 4, 6);
            LocalDate end = LocalDate.of(2024, 4, 5);

            // When
            BusinessDetails.AccountingPeriod period =
                new BusinessDetails.AccountingPeriod(start, end);

            // Then
            assertThat(period.start()).isEqualTo(start);
            assertThat(period.end()).isEqualTo(end);
        }
    }

    @Nested
    @DisplayName("Record equality")
    class RecordEquality {

        @Test
        @DisplayName("should be equal for same values")
        void shouldBeEqualForSameValues() {
            // Given
            BusinessDetails business1 = createBusinessDetails(null);
            BusinessDetails business2 = createBusinessDetails(null);

            // When/Then
            assertThat(business1).isEqualTo(business2);
            assertThat(business1.hashCode()).isEqualTo(business2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different business IDs")
        void shouldNotBeEqualForDifferentBusinessIds() {
            // Given
            BusinessDetails business1 = new BusinessDetails(
                "XAIS12345678901",
                "self-employment",
                "Test Business",
                LocalDate.of(2020, 4, 6),
                List.of(),
                null,
                null, null, null, null, null, null
            );
            BusinessDetails business2 = new BusinessDetails(
                "XAIS98765432101",
                "self-employment",
                "Test Business",
                LocalDate.of(2020, 4, 6),
                List.of(),
                null,
                null, null, null, null, null, null
            );

            // When/Then
            assertThat(business1).isNotEqualTo(business2);
        }
    }

    private BusinessDetails createBusinessDetails(LocalDate cessationDate) {
        return new BusinessDetails(
            "XAIS12345678901",
            "self-employment",
            "Test Business",
            LocalDate.of(2020, 4, 6),
            List.of(new BusinessDetails.AccountingPeriod(
                LocalDate.of(2023, 4, 6),
                LocalDate.of(2024, 4, 5)
            )),
            cessationDate,
            "123 Business Street",
            "London",
            null,
            null,
            "SW1A 1AA",
            "GB"
        );
    }

    private BusinessDetails createBusinessDetailsWithAddress(
            String line1, String line2, String line3, String line4, String postcode) {
        return new BusinessDetails(
            "XAIS12345678901",
            "self-employment",
            "Test Business",
            LocalDate.of(2020, 4, 6),
            List.of(),
            null,
            line1, line2, line3, line4, postcode, "GB"
        );
    }
}
