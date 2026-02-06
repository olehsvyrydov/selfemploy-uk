package uk.selfemploy.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewStatus")
class ReviewStatusTest {

    @Nested
    @DisplayName("isReviewed")
    class IsReviewed {

        @Test
        @DisplayName("PENDING is not reviewed")
        void pendingIsNotReviewed() {
            assertThat(ReviewStatus.PENDING.isReviewed()).isFalse();
        }

        @Test
        @DisplayName("CATEGORIZED is reviewed")
        void categorizedIsReviewed() {
            assertThat(ReviewStatus.CATEGORIZED.isReviewed()).isTrue();
        }

        @Test
        @DisplayName("EXCLUDED is reviewed")
        void excludedIsReviewed() {
            assertThat(ReviewStatus.EXCLUDED.isReviewed()).isTrue();
        }

        @Test
        @DisplayName("SKIPPED is reviewed")
        void skippedIsReviewed() {
            assertThat(ReviewStatus.SKIPPED.isReviewed()).isTrue();
        }
    }

    @Nested
    @DisplayName("isCategorized")
    class IsCategorized {

        @Test
        @DisplayName("CATEGORIZED returns true")
        void categorizedReturnsTrue() {
            assertThat(ReviewStatus.CATEGORIZED.isCategorized()).isTrue();
        }

        @Test
        @DisplayName("PENDING returns false")
        void pendingReturnsFalse() {
            assertThat(ReviewStatus.PENDING.isCategorized()).isFalse();
        }

        @Test
        @DisplayName("EXCLUDED returns false")
        void excludedReturnsFalse() {
            assertThat(ReviewStatus.EXCLUDED.isCategorized()).isFalse();
        }

        @Test
        @DisplayName("SKIPPED returns false")
        void skippedReturnsFalse() {
            assertThat(ReviewStatus.SKIPPED.isCategorized()).isFalse();
        }
    }

    @Test
    @DisplayName("has exactly 4 values")
    void hasFourValues() {
        assertThat(ReviewStatus.values()).hasSize(4);
    }
}
