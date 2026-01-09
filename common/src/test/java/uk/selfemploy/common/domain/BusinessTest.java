package uk.selfemploy.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.selfemploy.common.enums.BusinessType;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Business Entity Tests")
class BusinessTest {

    private static final String VALID_NAME = "Test Business";
    private static final String VALID_UTR = "1234567890";
    private static final LocalDate VALID_START = LocalDate.of(2025, 4, 6);
    private static final LocalDate VALID_END = LocalDate.of(2026, 4, 5);
    private static final BusinessType VALID_TYPE = BusinessType.SELF_EMPLOYED;
    private static final String VALID_DESCRIPTION = "Freelance consulting";

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("should create valid business with all fields")
        void shouldCreateValidBusiness() {
            Business business = Business.create(
                VALID_NAME,
                VALID_UTR,
                VALID_START,
                VALID_END,
                VALID_TYPE,
                VALID_DESCRIPTION
            );

            assertThat(business).isNotNull();
            assertThat(business.id()).isNotNull();
            assertThat(business.name()).isEqualTo(VALID_NAME);
            assertThat(business.utr()).isEqualTo(VALID_UTR);
            assertThat(business.accountingPeriodStart()).isEqualTo(VALID_START);
            assertThat(business.accountingPeriodEnd()).isEqualTo(VALID_END);
            assertThat(business.type()).isEqualTo(VALID_TYPE);
            assertThat(business.description()).isEqualTo(VALID_DESCRIPTION);
        }

        @Test
        @DisplayName("should generate unique ID for each business")
        void shouldGenerateUniqueId() {
            Business business1 = Business.create(VALID_NAME, VALID_UTR, VALID_START, VALID_END, VALID_TYPE, VALID_DESCRIPTION);
            Business business2 = Business.create(VALID_NAME, VALID_UTR, VALID_START, VALID_END, VALID_TYPE, VALID_DESCRIPTION);

            assertThat(business1.id()).isNotEqualTo(business2.id());
        }

        @Test
        @DisplayName("should create business with explicit ID")
        void shouldCreateWithExplicitId() {
            UUID explicitId = UUID.randomUUID();
            Business business = new Business(
                explicitId,
                VALID_NAME,
                VALID_UTR,
                VALID_START,
                VALID_END,
                VALID_TYPE,
                VALID_DESCRIPTION,
                true
            );

            assertThat(business.id()).isEqualTo(explicitId);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should reject null or empty business name")
        void shouldRejectInvalidName(String invalidName) {
            assertThatThrownBy(() ->
                Business.create(invalidName, VALID_UTR, VALID_START, VALID_END, VALID_TYPE, VALID_DESCRIPTION)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("name");
        }

        @ParameterizedTest
        @ValueSource(strings = {"123456789", "12345678901", "ABCDEFGHIJ", "12-34-56-78"})
        @DisplayName("should reject invalid UTR format")
        void shouldRejectInvalidUtr(String invalidUtr) {
            assertThatThrownBy(() ->
                Business.create(VALID_NAME, invalidUtr, VALID_START, VALID_END, VALID_TYPE, VALID_DESCRIPTION)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("UTR");
        }

        @Test
        @DisplayName("should accept null UTR for businesses without UTR yet")
        void shouldAcceptNullUtr() {
            Business business = Business.create(VALID_NAME, null, VALID_START, VALID_END, VALID_TYPE, VALID_DESCRIPTION);
            assertThat(business.utr()).isNull();
        }

        @Test
        @DisplayName("should reject null business type")
        void shouldRejectNullBusinessType() {
            assertThatThrownBy(() ->
                Business.create(VALID_NAME, VALID_UTR, VALID_START, VALID_END, null, VALID_DESCRIPTION)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("type");
        }

        @Test
        @DisplayName("should reject end date before start date")
        void shouldRejectEndDateBeforeStartDate() {
            LocalDate invalidEnd = VALID_START.minusDays(1);

            assertThatThrownBy(() ->
                Business.create(VALID_NAME, VALID_UTR, VALID_START, invalidEnd, VALID_TYPE, VALID_DESCRIPTION)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("date");
        }
    }

    @Nested
    @DisplayName("Business Status Tests")
    class StatusTests {

        @Test
        @DisplayName("should return active status")
        void shouldReturnActiveStatus() {
            Business business = new Business(
                UUID.randomUUID(),
                VALID_NAME,
                VALID_UTR,
                VALID_START,
                VALID_END,
                VALID_TYPE,
                VALID_DESCRIPTION,
                true
            );

            assertThat(business.active()).isTrue();
        }

        @Test
        @DisplayName("should return inactive status")
        void shouldReturnInactiveStatus() {
            Business business = new Business(
                UUID.randomUUID(),
                VALID_NAME,
                VALID_UTR,
                VALID_START,
                VALID_END,
                VALID_TYPE,
                VALID_DESCRIPTION,
                false
            );

            assertThat(business.active()).isFalse();
        }

        @Test
        @DisplayName("should create active business by default")
        void shouldCreateActiveByDefault() {
            Business business = Business.create(VALID_NAME, VALID_UTR, VALID_START, VALID_END, VALID_TYPE, VALID_DESCRIPTION);
            assertThat(business.active()).isTrue();
        }
    }

    @Nested
    @DisplayName("UTR Validation Tests")
    class UtrValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"1234567890", "9876543210", "0000000001"})
        @DisplayName("should accept valid 10-digit UTR")
        void shouldAcceptValidUtr(String validUtr) {
            Business business = Business.create(VALID_NAME, validUtr, VALID_START, VALID_END, VALID_TYPE, VALID_DESCRIPTION);
            assertThat(business.utr()).isEqualTo(validUtr);
        }
    }
}
