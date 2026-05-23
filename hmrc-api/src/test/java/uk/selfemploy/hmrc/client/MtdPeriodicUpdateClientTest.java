package uk.selfemploy.hmrc.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.selfemploy.common.dto.CumulativeSummary;
import uk.selfemploy.common.dto.PeriodicUpdate;

import jakarta.ws.rs.Path;

import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Contract + DTO tests for MtdPeriodicUpdateClient (HMRC Self-Employment Business
 * API v5 — SLFEMPUK-34 / S17-10).
 *
 * <p>Locks the v5 Accept header, the {@link MtdPeriodicUpdateClient#ERROR_OVERLAP_RELIEF_USED_NOT_ALLOWED}
 * constant, and — most importantly — the absence of the two HMRC-deprecated
 * fields {@code averagingAdjustment} and {@code overlapReliefUsed} on both
 * {@link PeriodicUpdate} and {@link CumulativeSummary} and their nested records.
 * If a future contributor re-introduces either field as a record component, the
 * reflection contract test below will fail before the change can reach HMRC.
 *
 * <p>The legacy fixture deserialisation test proves
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} tolerates historical
 * persisted JSON that still carries the deprecated fields.
 */
@DisplayName("MtdPeriodicUpdateClient (HMRC Self-Employment Business API v5)")
class MtdPeriodicUpdateClientTest {

    /**
     * Names HMRC has deprecated and which the application must never emit
     * (see HMRC MTD changelog 2026-04-24 + 2026-05-12). Kept in one place
     * because the same assertion is run across four record classes.
     */
    private static final String[] DEPRECATED_FIELD_NAMES = {
        "averagingAdjustment",
        "overlapReliefUsed"
    };

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("path is /individuals/business/self-employment")
        void pathIsCorrect() {
            Path path = MtdPeriodicUpdateClient.class.getAnnotation(Path.class);
            assertThat(path).isNotNull();
            assertThat(path.value()).isEqualTo("/individuals/business/self-employment");
        }

        @Test
        @DisplayName("Accept header declares HMRC v5")
        void acceptHeaderIsV5() {
            ClientHeaderParam header = MtdPeriodicUpdateClient.class.getAnnotation(ClientHeaderParam.class);
            assertThat(header).isNotNull();
            assertThat(header.name()).isEqualTo("Accept");
            assertThat(header.value())
                .as("Self-Employment Business v5 — periodic + cumulative endpoints share Accept header")
                .containsExactly("application/vnd.hmrc.5.0+json");
        }

        @Test
        @DisplayName("RULE_OVERLAP_RELIEF_USED_NOT_ALLOWED constant is exposed to callers")
        void errorCodeConstantExposed() {
            assertThat(MtdPeriodicUpdateClient.ERROR_OVERLAP_RELIEF_USED_NOT_ALLOWED)
                .as("HMRC error code introduced 2026-05-12 must be available as a symbolic constant")
                .isEqualTo("RULE_OVERLAP_RELIEF_USED_NOT_ALLOWED");
        }
    }

    @Nested
    @DisplayName("PeriodicUpdate DTO — deprecated fields absent")
    class PeriodicUpdateContract {

        @Test
        @DisplayName("PeriodicUpdate record has no averagingAdjustment or overlapReliefUsed component")
        void topLevelHasNoDeprecatedComponents() {
            assertRecordHasNoDeprecatedComponent(PeriodicUpdate.class);
        }

        @Test
        @DisplayName("PeriodIncome record has no averagingAdjustment or overlapReliefUsed component")
        void periodIncomeHasNoDeprecatedComponents() {
            assertRecordHasNoDeprecatedComponent(PeriodicUpdate.PeriodIncome.class);
        }

        @Test
        @DisplayName("PeriodExpenses record has no averagingAdjustment or overlapReliefUsed component")
        void periodExpensesHasNoDeprecatedComponents() {
            assertRecordHasNoDeprecatedComponent(PeriodicUpdate.PeriodExpenses.class);
        }

        @Test
        @DisplayName("PeriodDates record has no averagingAdjustment or overlapReliefUsed component")
        void periodDatesHasNoDeprecatedComponents() {
            assertRecordHasNoDeprecatedComponent(PeriodicUpdate.PeriodDates.class);
        }
    }

    @Nested
    @DisplayName("CumulativeSummary DTO — deprecated fields absent")
    class CumulativeSummaryContract {

        @Test
        @DisplayName("CumulativeSummary record has no averagingAdjustment or overlapReliefUsed component")
        void topLevelHasNoDeprecatedComponents() {
            assertRecordHasNoDeprecatedComponent(CumulativeSummary.class);
        }

        @Test
        @DisplayName("CumulativeIncome record has no averagingAdjustment or overlapReliefUsed component")
        void cumulativeIncomeHasNoDeprecatedComponents() {
            assertRecordHasNoDeprecatedComponent(CumulativeSummary.CumulativeIncome.class);
        }

        @Test
        @DisplayName("CumulativeExpenses record has no averagingAdjustment or overlapReliefUsed component")
        void cumulativeExpensesHasNoDeprecatedComponents() {
            assertRecordHasNoDeprecatedComponent(CumulativeSummary.CumulativeExpenses.class);
        }
    }

    @Nested
    @DisplayName("Outgoing serialisation — deprecated fields never on the wire")
    class OutgoingSerialisation {

        @Test
        @DisplayName("PeriodicUpdate JSON contains no averagingAdjustment or overlapReliefUsed key")
        void periodicUpdateJsonHasNoDeprecatedKeys() throws Exception {
            PeriodicUpdate update = new PeriodicUpdate(
                LocalDate.of(2025, 4, 6),
                LocalDate.of(2025, 7, 5),
                PeriodicUpdate.PeriodIncome.of(new BigDecimal("10000.00"), new BigDecimal("250.00")),
                PeriodicUpdate.PeriodExpenses.builder()
                    .staffCosts(new BigDecimal("3000.00"))
                    .travelCosts(new BigDecimal("150.00"))
                    .build()
            );

            String json = mapper.writeValueAsString(update);

            assertThat(json)
                .as("Outgoing PeriodicUpdate JSON must never carry HMRC-deprecated field names")
                .doesNotContain("averagingAdjustment")
                .doesNotContain("overlapReliefUsed");
        }

        @Test
        @DisplayName("CumulativeSummary JSON contains no averagingAdjustment or overlapReliefUsed key")
        void cumulativeSummaryJsonHasNoDeprecatedKeys() throws Exception {
            CumulativeSummary summary = new CumulativeSummary(
                new CumulativeSummary.CumulativeIncome(
                    new BigDecimal("25000.00"), new BigDecimal("400.00")),
                CumulativeSummary.CumulativeExpenses.builder()
                    .staffCosts(new BigDecimal("7000.00"))
                    .travelCosts(new BigDecimal("420.00"))
                    .build()
            );

            String json = mapper.writeValueAsString(summary);

            assertThat(json)
                .as("Outgoing CumulativeSummary JSON must never carry HMRC-deprecated field names")
                .doesNotContain("averagingAdjustment")
                .doesNotContain("overlapReliefUsed");
        }
    }

    @Nested
    @DisplayName("Legacy fixture deserialisation — @JsonIgnoreProperties tolerates historical data")
    class LegacyFixtureTolerance {

        @Test
        @DisplayName("legacy JSON carrying both deprecated fields deserialises without exception")
        void legacyFieldsIgnoredOnDeserialisation() {
            assertThatCode(() -> {
                try (InputStream is = getClass().getClassLoader()
                        .getResourceAsStream("hmrc-sandbox/periodic-update-legacy-deprecated-fields.json")) {
                    assertThat(is)
                        .as("fixture periodic-update-legacy-deprecated-fields.json missing on classpath")
                        .isNotNull();
                    PeriodicUpdate update = mapper.readValue(is, PeriodicUpdate.class);
                    // Surviving fields still come through — the deprecated ones are simply dropped.
                    assertThat(update.periodIncome().turnover()).isEqualByComparingTo("12500.00");
                    assertThat(update.periodExpenses().staffCosts()).isEqualByComparingTo("4500.00");
                }
            })
                .as("@JsonIgnoreProperties(ignoreUnknown = true) must absorb legacy averagingAdjustment "
                    + "and overlapReliefUsed values produced by versions of the app shipped before SLFEMPUK-34")
                .doesNotThrowAnyException();
        }
    }

    private static void assertRecordHasNoDeprecatedComponent(Class<?> recordClass) {
        assertThat(recordClass.isRecord())
            .as("%s must be a record (precondition for component-name reflection check)", recordClass.getName())
            .isTrue();
        for (RecordComponent c : recordClass.getRecordComponents()) {
            assertThat(DEPRECATED_FIELD_NAMES)
                .as("%s.%s must not exist — HMRC deprecated this field (changelog 2026-04-24 / 2026-05-12, "
                    + "SLFEMPUK-34); re-introducing it risks RULE_OVERLAP_RELIEF_USED_NOT_ALLOWED rejections "
                    + "and silent data loss on the response side.", recordClass.getSimpleName(), c.getName())
                .doesNotContain(c.getName());
        }
    }
}
