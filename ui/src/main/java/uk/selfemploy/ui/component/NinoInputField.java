package uk.selfemploy.ui.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.selfemploy.common.validation.HmrcIdentifierValidator;

/**
 * A custom input field for NINO (National Insurance Number) entry.
 * Sprint 12 - SE-12-002: NINO Entry with Validation Screen
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Auto-formatting as user types (QQ 12 34 56 A format)</li>
 *   <li>Real-time validation using HmrcIdentifierValidator</li>
 *   <li>Visual feedback with green checkmark (valid) or red X (invalid)</li>
 *   <li>Bindable valid and nino properties</li>
 * </ul>
 *
 * <p>CSS classes:</p>
 * <ul>
 *   <li>.nino-input-field - Container HBox</li>
 *   <li>.nino-text-field - The text input</li>
 *   <li>.nino-validation-icon - The validation icon</li>
 *   <li>.nino-validation-icon.valid - Green checkmark state</li>
 *   <li>.nino-validation-icon.invalid - Red X state</li>
 * </ul>
 */
public class NinoInputField extends HBox {

    /** Placeholder text showing expected format */
    public static final String PLACEHOLDER = "QQ 12 34 56 A";

    /** Maximum length of formatted NINO (with spaces) */
    private static final int MAX_FORMATTED_LENGTH = 13; // "XX 00 00 00 X"

    /** Maximum raw NINO length (without spaces) */
    private static final int MAX_RAW_LENGTH = 9;

    private static final String STYLE_CLASS = "nino-input-field";
    private static final String TEXT_FIELD_CLASS = "nino-text-field";
    private static final String VALIDATION_ICON_CLASS = "nino-validation-icon";
    private static final String VALID_CLASS = "valid";
    private static final String INVALID_CLASS = "invalid";

    private final TextField textField;
    private final Label validationIcon;

    private final BooleanProperty valid = new SimpleBooleanProperty(false);
    private final StringProperty nino = new SimpleStringProperty("");

    // Flag to prevent recursive updates during formatting
    private boolean updating = false;

    /**
     * Creates a new NINO input field.
     */
    public NinoInputField() {
        super(8); // 8px gap between text field and icon
        getStyleClass().add(STYLE_CLASS);
        setAlignment(Pos.CENTER_LEFT);

        // Create text field
        textField = createTextField();

        // Create validation icon
        validationIcon = createValidationIcon();

        getChildren().addAll(textField, validationIcon);
        HBox.setHgrow(textField, Priority.ALWAYS);

        // Setup text change listener for auto-formatting and validation
        setupTextChangeListener();
    }

    /**
     * Creates the text input field.
     */
    private TextField createTextField() {
        TextField field = new TextField();
        field.getStyleClass().add(TEXT_FIELD_CLASS);
        field.setPromptText(PLACEHOLDER);
        field.setAccessibleText("National Insurance Number input");
        return field;
    }

    /**
     * Creates the validation icon (initially hidden).
     */
    private Label createValidationIcon() {
        Label icon = new Label();
        icon.getStyleClass().add(VALIDATION_ICON_CLASS);
        icon.setVisible(false);
        icon.setMinWidth(24);
        icon.setMinHeight(24);
        return icon;
    }

    /**
     * Sets up the text change listener for auto-formatting and validation.
     */
    private void setupTextChangeListener() {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (updating) {
                return;
            }

            updating = true;
            try {
                String formatted = autoFormat(newValue);
                if (!formatted.equals(newValue)) {
                    textField.setText(formatted);
                    // Move caret to end
                    textField.positionCaret(formatted.length());
                }

                // Update nino property with clean value (no spaces)
                String clean = formatted.replaceAll("\\s", "").toUpperCase();
                nino.set(clean);

                // Validate and update visual feedback
                validate(clean);
            } finally {
                updating = false;
            }
        });
    }

    /**
     * Auto-formats the input to NINO format (XX 00 00 00 X).
     *
     * @param input the raw input
     * @return the formatted string
     */
    private String autoFormat(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Remove all non-alphanumeric characters and convert to uppercase
        String clean = input.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        // Limit to max raw length
        if (clean.length() > MAX_RAW_LENGTH) {
            clean = clean.substring(0, MAX_RAW_LENGTH);
        }

        // Format as: XX 00 00 00 X
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);

            // Add spaces after positions: 2, 4, 6, 8 (indices 1, 3, 5, 7)
            if (i == 2 || i == 4 || i == 6 || i == 8) {
                formatted.append(' ');
            }

            formatted.append(c);
        }

        return formatted.toString();
    }

    /**
     * Validates the NINO and updates visual feedback.
     *
     * @param cleanNino the NINO without spaces
     */
    private void validate(String cleanNino) {
        if (cleanNino == null || cleanNino.isEmpty()) {
            // Hide icon when empty
            valid.set(false);
            validationIcon.setVisible(false);
            validationIcon.getStyleClass().removeAll(VALID_CLASS, INVALID_CLASS);
            return;
        }

        boolean isValid = HmrcIdentifierValidator.isValidNino(cleanNino);
        valid.set(isValid);

        // Update icon
        validationIcon.setVisible(true);
        validationIcon.getStyleClass().removeAll(VALID_CLASS, INVALID_CLASS);

        if (isValid) {
            FontIcon checkIcon = FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 18);
            validationIcon.setGraphic(checkIcon);
            validationIcon.getStyleClass().add(VALID_CLASS);
        } else {
            FontIcon xIcon = FontIcon.of(FontAwesomeSolid.TIMES_CIRCLE, 18);
            validationIcon.setGraphic(xIcon);
            validationIcon.getStyleClass().add(INVALID_CLASS);
        }
    }

    // === Public API ===

    /**
     * Returns the text field for direct access.
     *
     * @return the text field
     */
    public TextField getTextField() {
        return textField;
    }

    /**
     * Returns the validation icon for direct access.
     *
     * @return the validation icon label
     */
    public Label getValidationIcon() {
        return validationIcon;
    }

    /**
     * Returns the clean NINO value (without spaces).
     *
     * @return the NINO value
     */
    public String getNino() {
        return nino.get();
    }

    /**
     * Returns the NINO property for binding.
     *
     * @return the nino property
     */
    public StringProperty ninoProperty() {
        return nino;
    }

    /**
     * Checks if the current NINO is valid.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return valid.get();
    }

    /**
     * Returns the valid property for binding.
     *
     * @return the valid property
     */
    public BooleanProperty validProperty() {
        return valid;
    }

    /**
     * Sets the NINO value programmatically.
     *
     * @param value the NINO to set
     */
    public void setNino(String value) {
        if (value == null) {
            textField.setText("");
        } else {
            textField.setText(autoFormat(value));
        }
    }

    /**
     * Clears the input field.
     */
    public void clear() {
        textField.clear();
    }
}
