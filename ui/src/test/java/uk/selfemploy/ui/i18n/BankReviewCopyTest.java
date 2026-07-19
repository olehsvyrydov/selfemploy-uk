package uk.selfemploy.ui.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Guards the message keys referenced from Java (not FXML) by the Bank Review columns and the import
 * wizard's category help, and the friendly wording of the import source hint. {@code MessageKeyCoverageTest}
 * only scans FXML {@code %key} references, so these code-referenced keys need their own guard.
 */
@DisplayName("Bank Review / import copy")
class BankReviewCopyTest {

    @BeforeEach
    void useEnglish() {
        Messages.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void reset() {
        Messages.resetForTesting();
    }

    @Test
    @DisplayName("per-row toggle labels and their accessible hints are defined")
    void toggleKeysDefined() {
        assertThat(Messages.get("review.toggle.business")).isEqualTo("Business");
        assertThat(Messages.get("review.toggle.personal")).isEqualTo("Personal");
        for (String key : new String[]{
                "review.toggle.business.hint", "review.toggle.personal.hint",
                "review.action.exclude", "review.action.skip",
                "review.action.exclude.hint", "review.action.skip.hint",
                "bankImport.categoryHelp.button", "bankImport.categoryHelp.accessible",
                "bankImport.categoryHelp.none", "bankImport.categoryHelp.allowable",
                "bankImport.categoryHelp.notAllowable"}) {
            assertThat(Messages.get(key))
                    .as("key %s should resolve to real copy, not a missing-key marker", key)
                    .doesNotStartWith("!")
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("category help SA103 hint formats the box number")
    void sa103HintFormats() {
        assertThat(Messages.format("bankImport.categoryHelp.sa103", "30"))
                .contains("30")
                .doesNotContain("{0}");
    }

    @Test
    @DisplayName("the import source hint no longer leaks the internal BANK_IMPORT token")
    void sourceHintIsFriendly() {
        String hint = Messages.get("bankImport.source.hint");
        assertThat(hint).doesNotContain("BANK_IMPORT");
        assertThat(hint.toLowerCase(Locale.ROOT)).contains("bank");
    }
}
