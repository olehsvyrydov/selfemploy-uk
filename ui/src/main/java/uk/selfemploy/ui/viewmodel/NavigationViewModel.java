package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.selfemploy.common.domain.TaxYear;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ViewModel for main window navigation.
 * Manages view switching, navigation history, and tax year selection.
 */
public class NavigationViewModel {

    private static final int MAX_HISTORY_SIZE = 20;

    private final ObjectProperty<View> currentView = new SimpleObjectProperty<>(View.DASHBOARD);
    private final ObjectProperty<TaxYear> selectedTaxYear = new SimpleObjectProperty<>();
    private final ObservableList<TaxYear> availableTaxYears = FXCollections.observableArrayList();
    private final Deque<View> navigationHistory = new ArrayDeque<>();

    public NavigationViewModel() {
        // Initialize with current tax year
        TaxYear current = TaxYear.current();
        selectedTaxYear.set(current);

        // Populate available tax years (current + 2 previous)
        availableTaxYears.add(current);
        availableTaxYears.add(current.previous());
        availableTaxYears.add(current.previous().previous());
    }

    // === Navigation ===

    /**
     * Returns the current view.
     */
    public View getCurrentView() {
        return currentView.get();
    }

    public ObjectProperty<View> currentViewProperty() {
        return currentView;
    }

    /**
     * Navigates to the specified view.
     * Adds current view to history before switching.
     */
    public void navigateTo(View view) {
        if (view == null || view == getCurrentView()) {
            return;
        }

        // Add current view to history
        if (getCurrentView() != null) {
            navigationHistory.push(getCurrentView());
            if (navigationHistory.size() > MAX_HISTORY_SIZE) {
                navigationHistory.removeLast();
            }
        }

        currentView.set(view);
    }

    /**
     * Returns true if there is navigation history to go back to.
     */
    public boolean canGoBack() {
        return !navigationHistory.isEmpty();
    }

    /**
     * Navigates back to the previous view.
     */
    public void goBack() {
        if (canGoBack()) {
            View previousView = navigationHistory.pop();
            currentView.set(previousView);
        }
    }

    /**
     * Clears navigation history.
     */
    public void clearHistory() {
        navigationHistory.clear();
    }

    // === Tax Year Selection ===

    /**
     * Returns the currently selected tax year.
     */
    public TaxYear getSelectedTaxYear() {
        return selectedTaxYear.get();
    }

    public ObjectProperty<TaxYear> selectedTaxYearProperty() {
        return selectedTaxYear;
    }

    /**
     * Sets the selected tax year.
     */
    public void setSelectedTaxYear(TaxYear year) {
        if (year != null) {
            selectedTaxYear.set(year);
        }
    }

    /**
     * Returns the list of available tax years for selection.
     */
    public ObservableList<TaxYear> getAvailableTaxYears() {
        return availableTaxYears;
    }

    // === Status Bar ===

    /**
     * Returns formatted tax year label for status bar.
     */
    public String getTaxYearLabel() {
        TaxYear year = getSelectedTaxYear();
        if (year == null) {
            return "";
        }
        return "Tax Year " + year.label();
    }

    /**
     * Returns number of days until the filing deadline.
     */
    public long getDaysUntilDeadline() {
        TaxYear year = getSelectedTaxYear();
        if (year == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), year.onlineFilingDeadline());
    }

    /**
     * Returns formatted deadline countdown string.
     */
    public String getDeadlineCountdown() {
        long days = getDaysUntilDeadline();
        return days + " days until filing deadline";
    }
}
