package uk.selfemploy.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TransactionSource enum.
 */
@DisplayName("TransactionSource Tests")
class TransactionSourceTest {

    @Test
    @DisplayName("should have MANUAL source")
    void shouldHaveManualSource() {
        assertThat(TransactionSource.MANUAL).isNotNull();
        assertThat(TransactionSource.MANUAL.getDisplayName()).isEqualTo("Manual Entry");
    }

    @Test
    @DisplayName("should have BANK_IMPORT source")
    void shouldHaveBankImportSource() {
        assertThat(TransactionSource.BANK_IMPORT).isNotNull();
        assertThat(TransactionSource.BANK_IMPORT.getDisplayName()).isEqualTo("Bank Import");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveTwoValues() {
        assertThat(TransactionSource.values()).hasSize(2);
    }
}
