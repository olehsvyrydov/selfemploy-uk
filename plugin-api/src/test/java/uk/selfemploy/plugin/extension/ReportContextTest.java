package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReportContext}.
 */
@DisplayName("ReportContext")
class ReportContextTest {

    @Nested
    @DisplayName("when creating context")
    class Creation {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            LocalDate start = LocalDate.of(2024, 4, 6);
            LocalDate end = LocalDate.of(2025, 4, 5);
            Map<String, Object> params = Map.of("includeCharts", true);

            ReportContext context = new ReportContext(2024, start, end, params);

            assertThat(context.taxYear()).isEqualTo(2024);
            assertThat(context.startDate()).isEqualTo(start);
            assertThat(context.endDate()).isEqualTo(end);
            assertThat(context.parameters()).containsEntry("includeCharts", true);
        }

        @Test
        @DisplayName("should convert null parameters to empty map")
        void shouldConvertNullParametersToEmptyMap() {
            ReportContext context = new ReportContext(
                2024,
                LocalDate.of(2024, 4, 6),
                LocalDate.of(2025, 4, 5),
                null
            );

            assertThat(context.parameters()).isEmpty();
        }

        @Test
        @DisplayName("should make parameters immutable")
        void shouldMakeParametersImmutable() {
            ReportContext context = new ReportContext(
                2024,
                LocalDate.of(2024, 4, 6),
                LocalDate.of(2025, 4, 5),
                Map.of("key", "value")
            );

            assertThatThrownBy(() -> context.parameters().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should reject null start date")
        void shouldRejectNullStartDate() {
            assertThatThrownBy(() -> new ReportContext(
                2024, null, LocalDate.of(2025, 4, 5), null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Start date");
        }

        @Test
        @DisplayName("should reject null end date")
        void shouldRejectNullEndDate() {
            assertThatThrownBy(() -> new ReportContext(
                2024, LocalDate.of(2024, 4, 6), null, null
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("End date");
        }

        @Test
        @DisplayName("should reject end date before start date")
        void shouldRejectEndDateBeforeStartDate() {
            assertThatThrownBy(() -> new ReportContext(
                2024,
                LocalDate.of(2025, 4, 6),
                LocalDate.of(2024, 4, 5),
                null
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date");
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("forTaxYear should create correct date range")
        void forTaxYearShouldCreateCorrectDateRange() {
            ReportContext context = ReportContext.forTaxYear(2024);

            assertThat(context.taxYear()).isEqualTo(2024);
            assertThat(context.startDate()).isEqualTo(LocalDate.of(2024, 4, 6));
            assertThat(context.endDate()).isEqualTo(LocalDate.of(2025, 4, 5));
            assertThat(context.parameters()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getParameter method")
    class GetParameter {

        @Test
        @DisplayName("should return parameter value")
        void shouldReturnParameterValue() {
            ReportContext context = new ReportContext(
                2024,
                LocalDate.of(2024, 4, 6),
                LocalDate.of(2025, 4, 5),
                Map.of("includeCharts", true)
            );

            assertThat(context.<Boolean>getParameter("includeCharts", false)).isTrue();
        }

        @Test
        @DisplayName("should return default for missing parameter")
        void shouldReturnDefaultForMissingParameter() {
            ReportContext context = ReportContext.forTaxYear(2024);

            assertThat(context.<Boolean>getParameter("includeCharts", true)).isTrue();
        }
    }
}
