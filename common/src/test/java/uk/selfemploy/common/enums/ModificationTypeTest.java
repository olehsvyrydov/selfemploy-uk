package uk.selfemploy.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModificationType")
class ModificationTypeTest {

    @Test
    @DisplayName("has exactly 6 modification types")
    void hasSixTypes() {
        assertThat(ModificationType.values()).hasSize(6);
    }

    @Test
    @DisplayName("each type has a display name")
    void eachTypeHasDisplayName() {
        for (ModificationType type : ModificationType.values()) {
            assertThat(type.getDisplayName()).isNotBlank();
        }
    }

    @Test
    @DisplayName("CATEGORIZED display name is correct")
    void categorizedDisplayName() {
        assertThat(ModificationType.CATEGORIZED.getDisplayName()).isEqualTo("Categorized");
    }

    @Test
    @DisplayName("EXCLUDED display name is correct")
    void excludedDisplayName() {
        assertThat(ModificationType.EXCLUDED.getDisplayName()).isEqualTo("Excluded");
    }

    @Test
    @DisplayName("values can be looked up by name")
    void valuesLookupByName() {
        assertThat(ModificationType.valueOf("RECATEGORIZED")).isEqualTo(ModificationType.RECATEGORIZED);
        assertThat(ModificationType.valueOf("RESTORED")).isEqualTo(ModificationType.RESTORED);
        assertThat(ModificationType.valueOf("BUSINESS_PERSONAL_CHANGED")).isEqualTo(ModificationType.BUSINESS_PERSONAL_CHANGED);
        assertThat(ModificationType.valueOf("CATEGORY_CHANGED")).isEqualTo(ModificationType.CATEGORY_CHANGED);
    }
}
