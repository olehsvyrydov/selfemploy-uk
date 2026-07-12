package uk.selfemploy.ui.viewmodel;

import java.util.List;

/**
 * State machine for the guided product tour.
 *
 * <p>Holds an ordered list of {@link TourStep}s and a cursor into them. The tour is inactive until
 * {@link #start()} is called; {@link #next()} advances (finishing after the last step),
 * {@link #back()} moves to the previous step, and {@link #skip()} ends the tour immediately. The
 * view (the overlay) reads {@link #currentStep()} and the navigation flags after each transition.</p>
 *
 * <p>This type has no JavaFX dependency so the flow can be unit-tested directly.</p>
 */
public class TourViewModel {

    private final List<TourStep> steps;
    private int index = -1;
    private boolean active = false;

    public TourViewModel(List<TourStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("A tour must have at least one step");
        }
        this.steps = List.copyOf(steps);
    }

    /** The six stops of the default core-loop tour, highlighting the sidebar navigation. */
    public static TourViewModel defaultTour() {
        return new TourViewModel(List.of(
            new TourStep("navDashboard", "Your dashboard",
                "This is your home base. Once you add income and expenses, your key numbers for "
                + "the tax year appear here at a glance."),
            new TourStep("navIncome", "Record income",
                "Add what you earn here, or import it from a bank statement CSV. Each entry is "
                + "scoped to a tax year."),
            new TourStep("navExpenses", "Track expenses",
                "Record business costs and pick an SA103 category so they map straight onto your "
                + "Self Assessment."),
            new TourStep("navTransactionReview", "Review imported transactions",
                "Bank statement imports land here first. Mark each transaction as business or "
                + "personal before it becomes income or an expense."),
            new TourStep("navTax", "See your tax",
                "The tax summary estimates the Income Tax and National Insurance to set aside, "
                + "based on the figures you have entered."),
            new TourStep("navHmrc", "Submit to HMRC",
                "When you are ready, prepare and send your Making Tax Digital submissions to HMRC "
                + "from here.")
        ));
    }

    /** Begins the tour at the first step. */
    public void start() {
        active = true;
        index = 0;
    }

    /** Whether the tour is currently running. */
    public boolean isActive() {
        return active;
    }

    /**
     * The step currently being shown.
     *
     * @throws IllegalStateException if the tour is not active
     */
    public TourStep currentStep() {
        if (!active) {
            throw new IllegalStateException("Tour is not active");
        }
        return steps.get(index);
    }

    /** Whether there is a step after the current one. */
    public boolean hasNext() {
        return active && index < steps.size() - 1;
    }

    /** Whether there is a step before the current one. */
    public boolean hasPrevious() {
        return active && index > 0;
    }

    /** Whether the current step is the last one. */
    public boolean isLastStep() {
        return active && index == steps.size() - 1;
    }

    /** Advances to the next step, or finishes the tour if already on the last step. */
    public void next() {
        if (!active) {
            return;
        }
        if (index < steps.size() - 1) {
            index++;
        } else {
            end();
        }
    }

    /** Moves back to the previous step; a no-op on the first step. */
    public void back() {
        if (active && index > 0) {
            index--;
        }
    }

    /** Ends the tour immediately (used by Skip and Escape). */
    public void skip() {
        end();
    }

    /** The 1-based number of the current step. */
    public int stepNumber() {
        return index + 1;
    }

    /** The total number of steps. */
    public int totalSteps() {
        return steps.size();
    }

    /** A human-readable progress label, e.g. {@code "Step 2 of 6"}. */
    public String progressText() {
        return "Step " + stepNumber() + " of " + totalSteps();
    }

    private void end() {
        active = false;
        index = -1;
    }
}
