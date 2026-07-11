package uk.selfemploy.core.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the ports-and-adapters boundary: the {@code core} and {@code common} domain
 * must stay independent of the UI toolkit, of raw JDBC, and of the UI module, so a
 * future non-desktop edition can supply its own adapters without touching the core.
 *
 * <p>This is the guard for the "persistence/transport-agnostic core" decision. If it
 * fails, a boundary leak has been introduced — move the offending dependency behind an
 * interface (a port) rather than relaxing the rule.</p>
 */
@DisplayName("Core architecture boundary")
class CoreBoundaryTest {

    private static JavaClasses coreAndCommon;

    @BeforeAll
    static void importClasses() {
        coreAndCommon = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("uk.selfemploy.core", "uk.selfemploy.common");
    }

    @Test
    @DisplayName("core/common must not depend on the JavaFX UI toolkit")
    void noJavaFx() {
        noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("javafx..")
            .because("the domain must stay UI-toolkit-agnostic so it can run headless "
                + "(server edition) as well as under JavaFX")
            .allowEmptyShould(true)
            .check(coreAndCommon);
    }

    @Test
    @DisplayName("core/common must not use raw JDBC (java.sql)")
    void noRawJdbc() {
        noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("java.sql..", "javax.sql..")
            .because("persistence must sit behind repository ports, not raw JDBC in the domain")
            .allowEmptyShould(true)
            .check(coreAndCommon);
    }

    @Test
    @DisplayName("core/common must not depend on the ui module")
    void noUiModule() {
        noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("uk.selfemploy.ui..")
            .because("dependencies point inward: the UI depends on the core, never the reverse")
            .allowEmptyShould(true)
            .check(coreAndCommon);
    }
}
