package uk.selfemploy.hmrc.client;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import uk.selfemploy.hmrc.client.dto.StateBenefit;
import uk.selfemploy.hmrc.client.dto.StateBenefit.BenefitType;

import jakarta.ws.rs.Path;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Contract + validator tests for StateBenefitsClient (HMRC Individuals State
 * Benefits API v2 — SLFEMPUK-32 / S17-08).
 *
 * <p>Locks the v2 Accept header and the new {@code taxPaid}-prohibited business
 * rule that HMRC introduced in the 2026-03-24 production deployment.
 */
@DisplayName("StateBenefitsClient (HMRC Individuals State Benefits API v2)")
class StateBenefitsClientTest {

    private static final LocalDate START = LocalDate.of(2026, 4, 6);
    private static final LocalDate END = LocalDate.of(2027, 4, 5);

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /individuals/state-benefits")
        void pathIsCorrect() {
            Path path = StateBenefitsClient.class.getAnnotation(Path.class);
            assertThat(path).isNotNull();
            assertThat(path.value()).isEqualTo("/individuals/state-benefits");
        }

        @Test
        @DisplayName("Accept header declares HMRC v2 (SLFEMPUK-32)")
        void acceptHeaderIsV2() {
            ClientHeaderParam header = StateBenefitsClient.class.getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.value()).containsExactly("application/vnd.hmrc.2.0+json");
        }
    }

    @Nested
    @DisplayName("StateBenefit.validate() — taxPaid prohibition rule")
    class TaxPaidProhibitionRule {

        @ParameterizedTest
        @EnumSource(value = BenefitType.class, names = {"STATE_PENSION", "BEREAVEMENT_ALLOWANCE", "OTHER_STATE_BENEFITS"})
        @DisplayName("validate() throws for prohibited benefit types when taxPaid is populated")
        void rejectsTaxPaidForProhibitedBenefitTypes(BenefitType type) {
            StateBenefit benefit = new StateBenefit(type, START, END,
                new BigDecimal("5000.00"), new BigDecimal("100.00"));

            assertThatThrownBy(benefit::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rejects taxPaid for benefitType=" + type.hmrcLabel())
                .hasMessageContaining("paid gross by DWP");
        }

        @ParameterizedTest
        @EnumSource(value = BenefitType.class, names = {"STATE_PENSION", "BEREAVEMENT_ALLOWANCE", "OTHER_STATE_BENEFITS"})
        @DisplayName("validate() passes for prohibited benefit types when taxPaid is null")
        void allowsNullTaxPaidForProhibitedBenefitTypes(BenefitType type) {
            StateBenefit benefit = new StateBenefit(type, START, END,
                new BigDecimal("5000.00"), null);

            assertThatCode(benefit::validate).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(value = BenefitType.class, mode = Mode.EXCLUDE,
            names = {"STATE_PENSION", "BEREAVEMENT_ALLOWANCE", "OTHER_STATE_BENEFITS"})
        @DisplayName("validate() passes for non-prohibited benefit types even with taxPaid populated")
        void allowsTaxPaidForAllOtherBenefitTypes(BenefitType type) {
            StateBenefit benefit = new StateBenefit(type, START, END,
                new BigDecimal("5000.00"), new BigDecimal("100.00"));

            assertThatCode(benefit::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("TAX_PAID_PROHIBITED set contains exactly the three benefit types listed by HMRC")
        void prohibitedSetIsExactlyThree() {
            assertThat(BenefitType.TAX_PAID_PROHIBITED)
                .as("HMRC v2 rule lists exactly three benefit types as gross-paid: "
                    + "statePension, bereavementAllowance, otherStateBenefits")
                .containsExactlyInAnyOrder(
                    BenefitType.STATE_PENSION,
                    BenefitType.BEREAVEMENT_ALLOWANCE,
                    BenefitType.OTHER_STATE_BENEFITS);
        }
    }

    @Nested
    @DisplayName("BenefitType enum — JSON round-trip")
    class BenefitTypeRoundTrip {

        @ParameterizedTest
        @EnumSource(BenefitType.class)
        @DisplayName("every BenefitType round-trips via its hmrcLabel()")
        void roundTrip(BenefitType type) {
            assertThat(BenefitType.fromHmrcLabel(type.hmrcLabel())).isEqualTo(type);
        }

        @Test
        @DisplayName("unknown labels throw IllegalArgumentException (strict — unlike ItsaStatusReason)")
        void unknownLabelThrows() {
            assertThatThrownBy(() -> BenefitType.fromHmrcLabel("notARealBenefit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown benefit type");
        }
    }
}
