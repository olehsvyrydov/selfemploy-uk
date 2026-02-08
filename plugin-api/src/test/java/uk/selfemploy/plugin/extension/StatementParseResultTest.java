package uk.selfemploy.plugin.extension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StatementParseResult}.
 * Tests the parse result record for bank statement parsers.
 */
@DisplayName("StatementParseResult")
class StatementParseResultTest {

    private static final ParsedTransaction SAMPLE_TX = new ParsedTransaction(
        LocalDate.of(2025, 6, 15), "Payment", new BigDecimal("500.00"), null, null, null
    );

    @Nested
    @DisplayName("when creating a result")
    class Creation {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            List<ParsedTransaction> txns = List.of(SAMPLE_TX);
            List<String> warnings = List.of("Skipped row 5");
            List<String> errors = List.of("Fatal error");

            StatementParseResult result = new StatementParseResult(txns, warnings, errors, "csv-barclays");

            assertThat(result.transactions()).hasSize(1);
            assertThat(result.warnings()).containsExactly("Skipped row 5");
            assertThat(result.errors()).containsExactly("Fatal error");
            assertThat(result.detectedFormatId()).isEqualTo("csv-barclays");
        }

        @Test
        @DisplayName("should convert null lists to empty lists")
        void shouldConvertNullListsToEmpty() {
            StatementParseResult result = new StatementParseResult(null, null, null, null);

            assertThat(result.transactions()).isEmpty();
            assertThat(result.warnings()).isEmpty();
            assertThat(result.errors()).isEmpty();
            assertThat(result.detectedFormatId()).isNull();
        }

        @Test
        @DisplayName("should make transactions unmodifiable")
        void shouldMakeTransactionsUnmodifiable() {
            List<ParsedTransaction> mutable = new ArrayList<>();
            mutable.add(SAMPLE_TX);

            StatementParseResult result = new StatementParseResult(mutable, null, null, null);

            assertThatThrownBy(() -> result.transactions().add(SAMPLE_TX))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should make warnings unmodifiable")
        void shouldMakeWarningsUnmodifiable() {
            List<String> mutable = new ArrayList<>();
            mutable.add("warning");

            StatementParseResult result = new StatementParseResult(null, mutable, null, null);

            assertThatThrownBy(() -> result.warnings().add("new"))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should make errors unmodifiable")
        void shouldMakeErrorsUnmodifiable() {
            List<String> mutable = new ArrayList<>();
            mutable.add("error");

            StatementParseResult result = new StatementParseResult(null, null, mutable, null);

            assertThatThrownBy(() -> result.errors().add("new"))
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should be immune to external mutation of source list")
        void shouldBeImmuneToExternalMutation() {
            List<ParsedTransaction> mutable = new ArrayList<>();
            mutable.add(SAMPLE_TX);

            StatementParseResult result = new StatementParseResult(mutable, null, null, null);

            ParsedTransaction extra = new ParsedTransaction(
                LocalDate.of(2025, 7, 1), "Extra", new BigDecimal("100"), null, null, null
            );
            mutable.add(extra);

            // Result should not be affected
            assertThat(result.transactions()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("helper methods")
    class HelperMethods {

        @Test
        @DisplayName("hasErrors returns true when errors exist")
        void hasErrorsTrue() {
            StatementParseResult result = new StatementParseResult(
                null, null, List.of("Error"), null
            );

            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("hasErrors returns false when no errors")
        void hasErrorsFalse() {
            StatementParseResult result = new StatementParseResult(
                List.of(SAMPLE_TX), null, null, null
            );

            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("hasWarnings returns true when warnings exist")
        void hasWarningsTrue() {
            StatementParseResult result = new StatementParseResult(
                null, List.of("Warning"), null, null
            );

            assertThat(result.hasWarnings()).isTrue();
        }

        @Test
        @DisplayName("hasWarnings returns false when no warnings")
        void hasWarningsFalse() {
            StatementParseResult result = StatementParseResult.success(List.of(SAMPLE_TX), "csv");

            assertThat(result.hasWarnings()).isFalse();
        }

        @Test
        @DisplayName("transactionCount returns size of transaction list")
        void transactionCount() {
            StatementParseResult result = StatementParseResult.success(
                List.of(SAMPLE_TX, SAMPLE_TX), "csv"
            );

            assertThat(result.transactionCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("failure creates result with single error and no transactions")
        void failureCreatesErrorResult() {
            StatementParseResult result = StatementParseResult.failure("File not found");

            assertThat(result.transactions()).isEmpty();
            assertThat(result.errors()).containsExactly("File not found");
            assertThat(result.hasErrors()).isTrue();
            assertThat(result.warnings()).isEmpty();
            assertThat(result.detectedFormatId()).isNull();
        }

        @Test
        @DisplayName("success creates result with transactions and format ID")
        void successCreatesGoodResult() {
            StatementParseResult result = StatementParseResult.success(
                List.of(SAMPLE_TX), "csv-barclays"
            );

            assertThat(result.transactions()).hasSize(1);
            assertThat(result.errors()).isEmpty();
            assertThat(result.hasErrors()).isFalse();
            assertThat(result.detectedFormatId()).isEqualTo("csv-barclays");
        }
    }
}
