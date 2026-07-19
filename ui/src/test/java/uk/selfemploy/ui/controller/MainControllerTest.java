package uk.selfemploy.ui.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import uk.selfemploy.ui.controller.MainController.NavHighlight;
import uk.selfemploy.ui.viewmodel.View;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MainController}'s sidebar-highlight mapping.
 *
 * <p>Guards the fix for the stale-highlight bug: navigating to Settings or Help (which own no
 * sidebar button) must clear the highlight rather than leave the previously selected item marked
 * as the current view.</p>
 */
@DisplayName("MainController navigation highlight")
class MainControllerTest {

    @Nested
    @DisplayName("Sidebar highlight mapping")
    class SidebarHighlight {

        @Test
        @DisplayName("Settings clears the sidebar highlight (owns no nav button)")
        void settingsClearsHighlight() {
            assertThat(MainController.highlightFor(View.SETTINGS)).isEqualTo(NavHighlight.NONE);
        }

        @Test
        @DisplayName("Help clears the sidebar highlight (owns no nav button)")
        void helpClearsHighlight() {
            assertThat(MainController.highlightFor(View.HELP)).isEqualTo(NavHighlight.NONE);
        }

        @Test
        @DisplayName("primary views highlight their own nav button")
        void primaryViewsMapToOwnButton() {
            assertThat(MainController.highlightFor(View.DASHBOARD)).isEqualTo(NavHighlight.DASHBOARD);
            assertThat(MainController.highlightFor(View.INCOME)).isEqualTo(NavHighlight.INCOME);
            assertThat(MainController.highlightFor(View.EXPENSES)).isEqualTo(NavHighlight.EXPENSES);
            assertThat(MainController.highlightFor(View.TAX_SUMMARY)).isEqualTo(NavHighlight.TAX);
            assertThat(MainController.highlightFor(View.HMRC_SUBMISSION)).isEqualTo(NavHighlight.HMRC);
        }

        @ParameterizedTest
        @EnumSource(value = View.class,
                names = {"BANK", "TRANSACTION_REVIEW", "RECONCILIATION", "IMPORT_HISTORY"})
        @DisplayName("Bank and its sub-views all highlight the Bank button")
        void bankSubViewsHighlightBank(View view) {
            assertThat(MainController.highlightFor(view)).isEqualTo(NavHighlight.BANK);
        }

        @ParameterizedTest
        @EnumSource(View.class)
        @DisplayName("every view maps to a highlight, so none is left unhandled")
        void everyViewMaps(View view) {
            assertThat(MainController.highlightFor(view)).isNotNull();
        }
    }
}
