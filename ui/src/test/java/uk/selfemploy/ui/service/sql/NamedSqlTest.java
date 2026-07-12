package uk.selfemploy.ui.service.sql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NamedSql")
class NamedSqlTest {

    private final NamedSql sql = NamedSql.load("/sql/wizard-progress.sql");

    @Test
    @DisplayName("returns the statement registered under a name, without its trailing semicolon")
    void returnsNamedStatement() {
        String findByType = sql.get("findWizardProgressByType");

        assertThat(findByType)
            .isEqualTo("SELECT * FROM wizard_progress WHERE wizard_type = ?");
    }

    @Test
    @DisplayName("parses a multi-line statement into a single block")
    void parsesMultiLineStatement() {
        String upsert = sql.get("upsertWizardProgress");

        assertThat(upsert)
            .startsWith("INSERT INTO wizard_progress")
            .contains("ON CONFLICT(wizard_type) DO UPDATE SET")
            .doesNotEndWith(";");
    }

    @Test
    @DisplayName("throws for an unknown statement name")
    void throwsForUnknownName() {
        assertThatThrownBy(() -> sql.get("doesNotExist"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("doesNotExist");
    }

    @Test
    @DisplayName("throws for a missing resource")
    void throwsForMissingResource() {
        assertThatThrownBy(() -> NamedSql.load("/sql/nope.sql"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SQL resource not found");
    }

    @Test
    @DisplayName("fails fast on a duplicate statement name")
    void throwsOnDuplicateName() {
        assertThatThrownBy(() -> NamedSql.load("/sql/duplicate-names.sql"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate SQL statement name 'dup'");
    }

    @Test
    @DisplayName("fails fast on an empty statement name")
    void throwsOnEmptyName() {
        assertThatThrownBy(() -> NamedSql.load("/sql/empty-name.sql"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Empty statement name");
    }
}
