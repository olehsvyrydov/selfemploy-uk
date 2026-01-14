package uk.selfemploy.ui.viewmodel;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;

import java.util.regex.Pattern;

/**
 * ViewModel for the Employment Income Warning banner.
 *
 * <p>SE-809: Manages the display of a warning banner when employment income
 * (PAYE) is detected. The app is designed for self-employment only, so users
 * with employment income need to understand the limitations.</p>
 *
 * <h3>Detection Triggers:</h3>
 * <ul>
 *   <li>Bank transaction with PAYE-like pattern (e.g., "SALARY FROM...")</li>
 *   <li>HMRC connection shows employment income present</li>
 *   <li>Manual categorization as employment income</li>
 * </ul>
 *
 * <h3>Visibility Rules:</h3>
 * <ul>
 *   <li>Shows when employment income detected AND not dismissed</li>
 *   <li>Can be dismissed for current session (unless in critical mode)</li>
 *   <li>Critical mode (e.g., Annual Submission) prevents dismissal</li>
 * </ul>
 */
public class EmploymentIncomeWarningViewModel {

    // Detection patterns for employment income
    private static final Pattern SALARY_PATTERN = Pattern.compile(
            "\\b(SALARY|WAGES|PAYE|P60|EMPLOYMENT|PAYROLL)\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Warning content
    private static final String TITLE_TEXT = "Employment Income Detected";

    private static final String FIRST_PARAGRAPH =
            "This application is designed for SELF-EMPLOYMENT income only. " +
                    "If you receive income from employment (PAYE), you'll need to " +
                    "report that separately through HMRC's Self Assessment portal.";

    private static final String SECOND_PARAGRAPH =
            "Your employer should provide a P60 showing your employment income " +
                    "and tax paid, which you'll need for your tax return.";

    private static final String HMRC_GUIDANCE_URL =
            "https://www.gov.uk/self-assessment-tax-returns/who-must-send-a-tax-return";

    // === State Properties ===

    private final BooleanProperty employmentIncomeDetected = new SimpleBooleanProperty(false);
    private final StringProperty detectionReason = new SimpleStringProperty();
    private final BooleanProperty dismissed = new SimpleBooleanProperty(false);
    private final BooleanProperty dismissable = new SimpleBooleanProperty(true);

    // === Computed Properties ===

    private final BooleanProperty showWarning = new SimpleBooleanProperty(false);
    private final BooleanProperty criticalMode = new SimpleBooleanProperty(false);

    /**
     * Creates a new EmploymentIncomeWarningViewModel.
     */
    public EmploymentIncomeWarningViewModel() {
        setupBindings();
    }

    // === State Properties ===

    /**
     * Property indicating whether employment income has been detected.
     */
    public BooleanProperty employmentIncomeDetectedProperty() {
        return employmentIncomeDetected;
    }

    /**
     * Property containing the reason for detection (e.g., "BANK_IMPORT", "HMRC_DATA").
     */
    public StringProperty detectionReasonProperty() {
        return detectionReason;
    }

    /**
     * Property indicating whether the warning has been dismissed for this session.
     */
    public BooleanProperty dismissedProperty() {
        return dismissed;
    }

    /**
     * Property indicating whether the warning can be dismissed.
     * Set to false for critical contexts like Annual Submission.
     */
    public BooleanProperty dismissableProperty() {
        return dismissable;
    }

    // === Computed Properties ===

    /**
     * Property indicating whether the warning banner should be shown.
     * Shows when: detected AND not dismissed (or not dismissable).
     */
    public ReadOnlyBooleanProperty showWarningProperty() {
        return showWarning;
    }

    /**
     * Property indicating whether critical mode is active.
     * Critical mode uses a more prominent style and prevents dismissal.
     */
    public ReadOnlyBooleanProperty criticalModeProperty() {
        return criticalMode;
    }

    // === Actions ===

    /**
     * Sets the employment income detection state.
     *
     * @param detected whether employment income was detected
     * @param reason   the reason for detection (e.g., "BANK_IMPORT", "HMRC_DATA")
     */
    public void setEmploymentIncomeDetected(boolean detected, String reason) {
        employmentIncomeDetected.set(detected);
        detectionReason.set(reason);
    }

    /**
     * Dismisses the warning for the current session.
     * Has no effect if the warning is not dismissable.
     */
    public void dismiss() {
        if (dismissable.get()) {
            dismissed.set(true);
        }
    }

    /**
     * Resets the dismissal state (e.g., for a new session).
     */
    public void resetDismissal() {
        dismissed.set(false);
    }

    // === Content Getters ===

    /**
     * Returns the warning title text.
     */
    public String getTitleText() {
        return TITLE_TEXT;
    }

    /**
     * Returns the first paragraph of warning text.
     */
    public String getFirstParagraphText() {
        return FIRST_PARAGRAPH;
    }

    /**
     * Returns the second paragraph of warning text.
     */
    public String getSecondParagraphText() {
        return SECOND_PARAGRAPH;
    }

    /**
     * Returns the HMRC guidance URL.
     */
    public String getHmrcGuidanceUrl() {
        return HMRC_GUIDANCE_URL;
    }

    // === Detection Helpers ===

    /**
     * Checks if a transaction description contains employment income patterns.
     *
     * @param description the transaction description
     * @return true if employment income pattern detected
     */
    public boolean checkTransactionDescription(String description) {
        if (description == null || description.isBlank()) {
            return false;
        }
        return SALARY_PATTERN.matcher(description).find();
    }

    // === Private Methods ===

    private void setupBindings() {
        // Show warning when: detected AND (not dismissed OR not dismissable)
        showWarning.bind(Bindings.createBooleanBinding(
                () -> employmentIncomeDetected.get() && (!dismissed.get() || !dismissable.get()),
                employmentIncomeDetected, dismissed, dismissable
        ));

        // Critical mode when not dismissable
        criticalMode.bind(dismissable.not());
    }
}
