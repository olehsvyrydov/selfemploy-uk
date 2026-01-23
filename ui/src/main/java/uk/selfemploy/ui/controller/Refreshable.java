package uk.selfemploy.ui.controller;

/**
 * Interface for controllers that can refresh their data.
 * Used by MainController to refresh views when navigating to cached views.
 */
public interface Refreshable {

    /**
     * Refreshes the controller's data from the database/service layer.
     */
    void refreshData();
}
