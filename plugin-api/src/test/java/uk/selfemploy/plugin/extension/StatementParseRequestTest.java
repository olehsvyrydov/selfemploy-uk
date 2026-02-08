package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StatementParseRequest}.
 * Tests the parse configuration record for bank statement parsers.
 */
@DisplayName("StatementParseRequest")
class StatementParseRequestTest {

    @Nested
    @DisplayName("when creating a request")
    class Creation {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            Map<String, Object> options = Map.of("key", "value");
            StatementParseRequest request = new StatementParseRequest(
                "dd/MM/yyyy", "Date", "Description", "Amount", options
            );

            assertThat(request.dateFormat()).isEqualTo("dd/MM/yyyy");
            assertThat(request.dateColumn()).isEqualTo("Date");
            assertThat(request.descriptionColumn()).isEqualTo("Description");
            assertThat(request.amountColumn()).isEqualTo("Amount");
            assertThat(request.options()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("should create with null optional fields")
        void shouldCreateWithNullFields() {
            StatementParseRequest request = new StatementParseRequest(
                null, null, null, null, null
            );

            assertThat(request.dateFormat()).isNull();
            assertThat(request.dateColumn()).isNull();
            assertThat(request.options()).isEmpty();
        }

        @Test
        @DisplayName("should make options unmodifiable")
        void shouldMakeOptionsUnmodifiable() {
            Map<String, Object> mutableOptions = new HashMap<>();
            mutableOptions.put("key", "value");

            StatementParseRequest request = new StatementParseRequest(
                null, null, null, null, mutableOptions
            );

            assertThatThrownBy(() -> request.options().put("new", "entry"))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should be immune to external mutation of source map")
        void shouldBeImmuneToExternalMutation() {
            Map<String, Object> mutableOptions = new HashMap<>();
            mutableOptions.put("key", "value");

            StatementParseRequest request = new StatementParseRequest(
                null, null, null, null, mutableOptions
            );

            // Modify original map
            mutableOptions.put("extra", "data");

            // Request should not be affected
            assertThat(request.options()).doesNotContainKey("extra");
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("autoDetect creates request with all nulls")
        void autoDetectCreatesNullRequest() {
            StatementParseRequest request = StatementParseRequest.autoDetect();

            assertThat(request.dateFormat()).isNull();
            assertThat(request.dateColumn()).isNull();
            assertThat(request.descriptionColumn()).isNull();
            assertThat(request.amountColumn()).isNull();
            assertThat(request.options()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOption helper")
    class GetOptionHelper {

        @Test
        @DisplayName("returns value when present")
        void returnsValueWhenPresent() {
            StatementParseRequest request = new StatementParseRequest(
                null, null, null, null, Map.of("flag", true)
            );

            assertThat(request.<Boolean>getOption("flag", false)).isTrue();
        }

        @Test
        @DisplayName("returns default when absent")
        void returnsDefaultWhenAbsent() {
            StatementParseRequest request = StatementParseRequest.autoDetect();

            assertThat(request.<Boolean>getOption("missing", true)).isTrue();
        }
    }

    @Nested
    @DisplayName("option constants")
    class OptionConstants {

        @Test
        @DisplayName("has separate columns option key")
        void hasSeparateColumnsKey() {
            assertThat(StatementParseRequest.OPT_SEPARATE_COLUMNS).isEqualTo("separateColumns");
        }

        @Test
        @DisplayName("has income column option key")
        void hasIncomeColumnKey() {
            assertThat(StatementParseRequest.OPT_INCOME_COLUMN).isEqualTo("incomeColumn");
        }

        @Test
        @DisplayName("has expense column option key")
        void hasExpenseColumnKey() {
            assertThat(StatementParseRequest.OPT_EXPENSE_COLUMN).isEqualTo("expenseColumn");
        }
    }
}
