package uk.selfemploy.ui.service;

/**
 * Identifies which HMRC environment the app is talking to, so every submission
 * screen can label it honestly ("HMRC Sandbox") rather than implying a real filing.
 */
public enum SubmissionEnvironment {

    SANDBOX("HMRC Sandbox"),
    PRODUCTION("HMRC Production");

    private final String badgeLabel;

    SubmissionEnvironment(String badgeLabel) {
        this.badgeLabel = badgeLabel;
    }

    /**
     * Resolves the environment from the configured HMRC base URL. HMRC's test
     * platform is served from {@code test-api.service.hmrc.gov.uk}.
     */
    public static SubmissionEnvironment current() {
        String base = System.getProperty("HMRC_API_BASE_URL", "https://test-api.service.hmrc.gov.uk");
        return base.contains("test-api") ? SANDBOX : PRODUCTION;
    }

    public String badgeLabel() {
        return badgeLabel;
    }

    public boolean isSandbox() {
        return this == SANDBOX;
    }
}
