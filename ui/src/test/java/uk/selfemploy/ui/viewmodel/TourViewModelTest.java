package uk.selfemploy.ui.viewmodel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TourViewModel")
class TourViewModelTest {

    private static final List<TourStep> STEPS = List.of(
        new TourStep("a", "A", "first"),
        new TourStep("b", "B", "second"),
        new TourStep("c", "C", "third")
    );

    private TourViewModel newTour() {
        return new TourViewModel(STEPS);
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects an empty step list")
        void rejectsEmpty() {
            assertThatThrownBy(() -> new TourViewModel(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a null step list")
        void rejectsNull() {
            assertThatThrownBy(() -> new TourViewModel(null))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("is inactive before start")
        void inactiveBeforeStart() {
            assertThat(newTour().isActive()).isFalse();
        }

        @Test
        @DisplayName("the default tour has six steps and starts on the dashboard")
        void defaultTour() {
            TourViewModel tour = TourViewModel.defaultTour();
            tour.start();
            assertThat(tour.totalSteps()).isEqualTo(6);
            assertThat(tour.currentStep().targetNodeId()).isEqualTo("navDashboard");
        }
    }

    @Nested
    @DisplayName("navigation")
    class Navigation {

        @Test
        @DisplayName("start activates the tour on the first step")
        void startOnFirst() {
            TourViewModel tour = newTour();
            tour.start();
            assertThat(tour.isActive()).isTrue();
            assertThat(tour.currentStep().title()).isEqualTo("A");
            assertThat(tour.stepNumber()).isEqualTo(1);
            assertThat(tour.hasPrevious()).isFalse();
            assertThat(tour.hasNext()).isTrue();
            assertThat(tour.isLastStep()).isFalse();
        }

        @Test
        @DisplayName("next advances through the steps")
        void nextAdvances() {
            TourViewModel tour = newTour();
            tour.start();
            tour.next();
            assertThat(tour.currentStep().title()).isEqualTo("B");
            assertThat(tour.stepNumber()).isEqualTo(2);
            assertThat(tour.hasPrevious()).isTrue();
            assertThat(tour.hasNext()).isTrue();
        }

        @Test
        @DisplayName("back returns to the previous step")
        void backReturns() {
            TourViewModel tour = newTour();
            tour.start();
            tour.next();
            tour.back();
            assertThat(tour.currentStep().title()).isEqualTo("A");
            assertThat(tour.stepNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("back on the first step is a no-op")
        void backOnFirstNoOp() {
            TourViewModel tour = newTour();
            tour.start();
            tour.back();
            assertThat(tour.stepNumber()).isEqualTo(1);
            assertThat(tour.isActive()).isTrue();
        }

        @Test
        @DisplayName("the last step reports itself and has no next")
        void lastStep() {
            TourViewModel tour = newTour();
            tour.start();
            tour.next();
            tour.next();
            assertThat(tour.currentStep().title()).isEqualTo("C");
            assertThat(tour.isLastStep()).isTrue();
            assertThat(tour.hasNext()).isFalse();
        }

        @Test
        @DisplayName("next on the last step finishes the tour")
        void nextOnLastFinishes() {
            TourViewModel tour = newTour();
            tour.start();
            tour.next();
            tour.next();
            tour.next();
            assertThat(tour.isActive()).isFalse();
        }

        @Test
        @DisplayName("progress text reflects the current position")
        void progressText() {
            TourViewModel tour = newTour();
            tour.start();
            tour.next();
            assertThat(tour.progressText()).isEqualTo("Step 2 of 3");
        }
    }

    @Nested
    @DisplayName("ending")
    class Ending {

        @Test
        @DisplayName("skip ends the tour from any step")
        void skipEnds() {
            TourViewModel tour = newTour();
            tour.start();
            tour.next();
            tour.skip();
            assertThat(tour.isActive()).isFalse();
        }

        @Test
        @DisplayName("currentStep throws once the tour has ended")
        void currentStepThrowsWhenEnded() {
            TourViewModel tour = newTour();
            tour.start();
            tour.skip();
            assertThatThrownBy(tour::currentStep).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("navigation flags are false when inactive")
        void flagsFalseWhenInactive() {
            TourViewModel tour = newTour();
            assertThat(tour.hasNext()).isFalse();
            assertThat(tour.hasPrevious()).isFalse();
            assertThat(tour.isLastStep()).isFalse();
        }

        @Test
        @DisplayName("a finished tour can be restarted from the beginning")
        void canRestart() {
            TourViewModel tour = newTour();
            tour.start();
            tour.skip();
            tour.start();
            assertThat(tour.isActive()).isTrue();
            assertThat(tour.stepNumber()).isEqualTo(1);
        }
    }
}
