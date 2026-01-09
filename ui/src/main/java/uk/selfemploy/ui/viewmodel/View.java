package uk.selfemploy.ui.viewmodel;

/**
 * Enumeration of available views in the application.
 * Each view has an associated FXML path and display title.
 */
public enum View {
    DASHBOARD("Dashboard", "/fxml/dashboard.fxml"),
    INCOME("Income", "/fxml/income.fxml"),
    EXPENSES("Expenses", "/fxml/expenses.fxml"),
    TAX_SUMMARY("Tax Summary", "/fxml/tax-summary.fxml"),
    HMRC_SUBMISSION("HMRC Submission", "/fxml/hmrc-submission.fxml"),
    SETTINGS("Settings", "/fxml/settings.fxml"),
    HELP("Help", "/fxml/help.fxml");

    private final String title;
    private final String fxmlPath;

    View(String title, String fxmlPath) {
        this.title = title;
        this.fxmlPath = fxmlPath;
    }

    /**
     * Returns the display title for this view.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the FXML resource path for this view.
     */
    public String getFxmlPath() {
        return fxmlPath;
    }
}
