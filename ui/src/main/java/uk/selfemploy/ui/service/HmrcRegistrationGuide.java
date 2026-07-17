package uk.selfemploy.ui.service;

import java.util.List;

/**
 * The content of the guided "Register your app with HMRC" walkthrough: the ordered steps a user
 * follows on the HMRC Developer Hub to obtain their own API credentials, the exact values HMRC asks
 * them to enter (so the UI can offer copy buttons), and the Developer Hub link.
 *
 * <p>This is view-independent so the steps and copyable values can be tested directly; the dialog
 * only renders them. The redirect URI is supplied by the caller (from the app's OAuth config) rather
 * than hardcoded, so it always matches the callback port the app actually listens on.
 */
public final class HmrcRegistrationGuide {

    /** A name suggestion for the user's Developer Hub application; HMRC accepts any name. */
    public static final String SUGGESTED_APP_NAME = "My Self-Employment Tax Manager";

    /** The Developer Hub applications page, where a user creates and configures their app. */
    public static final String DEVELOPER_HUB_URL =
        "https://developer.service.hmrc.gov.uk/developer/applications";

    private final String redirectUri;

    public HmrcRegistrationGuide(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /** One numbered step in the walkthrough. */
    public record Step(int number, String title, String detail) {
    }

    /** A value the user must enter on the Developer Hub, offered with a copy button. */
    public record CopyValue(String label, String value) {
    }

    /** The ordered steps, numbered from 1. */
    public List<Step> steps() {
        return List.of(
            new Step(1, "Create a Developer Hub account",
                "Open the HMRC Developer Hub, register, and verify your email address."),
            new Step(2, "Add a sandbox application",
                "In Applications, choose \"Add an application\" and select Sandbox. "
                    + "Use the suggested application name below (or any name you like)."),
            new Step(3, "Set the Redirect URI",
                "In your application's Redirect URIs, add the Redirect URI below exactly as shown."),
            new Step(4, "Subscribe to the Making Tax Digital APIs",
                "In your application's API subscriptions, subscribe to the Making Tax Digital for "
                    + "Income Tax (Self Assessment) APIs — the ones the Developer Hub lists for MTD ITSA, "
                    + "so this app can read your obligations and submit updates."),
            new Step(5, "Copy your Client ID and Client Secret",
                "Open your application's Credentials to find your Client ID and Client Secret."),
            new Step(6, "Enter them below",
                "Paste your Client ID and Client Secret into the fields on this page and save."));
    }

    /** A note about moving from sandbox testing to live (production) submissions. */
    public String productionNote() {
        return "These steps set up a Sandbox application for testing. To file real returns you apply "
            + "for Production credentials on the Developer Hub (HMRC reviews the request, which can take "
            + "a few days), then switch the environment to Production in these Settings.";
    }

    /** The values HMRC asks the user to enter, each offered with a copy button. */
    public List<CopyValue> copyValues() {
        return List.of(
            new CopyValue("Application name", SUGGESTED_APP_NAME),
            new CopyValue("Redirect URI", redirectUri));
    }

    public String developerHubUrl() {
        return DEVELOPER_HUB_URL;
    }

    public String suggestedAppName() {
        return SUGGESTED_APP_NAME;
    }

    public String redirectUri() {
        return redirectUri;
    }
}
