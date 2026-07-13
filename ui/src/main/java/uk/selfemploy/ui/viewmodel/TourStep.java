package uk.selfemploy.ui.viewmodel;

/**
 * A single stop in the guided product tour.
 *
 * @param targetNodeId the {@code fx:id} of the control to highlight, or {@code null} to show the
 *                     coach-mark without a spotlight
 * @param title        the short heading for this stop
 * @param body         one or two sentences describing the highlighted area
 */
public record TourStep(String targetNodeId, String title, String body) {
}
