package uk.selfemploy.ui.service;

/**
 * Validates the <em>shape</em> of HMRC Developer Hub credentials before they are saved, so obvious
 * garbage (for example {@code "abc"}, previously accepted) is rejected at entry.
 *
 * <p>HMRC does not publish a fixed format for client ids or secrets, so validation is deliberately
 * conservative: it rejects only what cannot be a real credential — blank, internal whitespace,
 * control or non-ASCII characters, or a value too short to be a credential — and otherwise accepts.
 * The goal is to catch typos and placeholder text without ever rejecting a credential HMRC issued.
 *
 * <p>A rejection {@link Result} names the rule that failed and never contains the submitted value,
 * so it is safe to show in the UI or write to a log.
 */
public final class HmrcCredentialValidator {

    /**
     * Minimum plausible length. HMRC's issued values are far longer; eight rejects placeholders like
     * {@code "abc"} while staying well below any real credential.
     */
    static final int MIN_LENGTH = 8;

    private HmrcCredentialValidator() {
    }

    /** The outcome of validating one credential. {@code message} is null when {@code valid}. */
    public record Result(boolean valid, String message) {

        static Result ok() {
            return new Result(true, null);
        }

        static Result invalid(String message) {
            return new Result(false, message);
        }
    }

    /**
     * Validates a client id's shape.
     *
     * @param clientId the value entered by the user
     * @return a valid result, or an invalid one whose message names the failing rule
     */
    public static Result validateClientId(String clientId) {
        return validate(clientId, "Client ID");
    }

    /**
     * Validates a client secret's shape.
     *
     * @param clientSecret the value entered by the user
     * @return a valid result, or an invalid one whose message names the failing rule
     */
    public static Result validateClientSecret(String clientSecret) {
        return validate(clientSecret, "Client Secret");
    }

    private static Result validate(String value, String label) {
        if (value == null || value.isBlank()) {
            return Result.invalid("Enter your HMRC Developer Hub " + label + ".");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0x7E || c < 0x21) {
                return Result.invalid("The " + label + " contains spaces or unusual characters. "
                    + "Copy it exactly from the HMRC Developer Hub, with no surrounding spaces.");
            }
        }
        if (value.length() < MIN_LENGTH) {
            return Result.invalid("That " + label + " looks too short to be a real HMRC credential. "
                + "Copy the full value from the HMRC Developer Hub.");
        }
        return Result.ok();
    }
}
