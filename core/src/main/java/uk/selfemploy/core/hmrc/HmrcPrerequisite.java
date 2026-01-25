package uk.selfemploy.core.hmrc;

/**
 * A prerequisite that must be completed before connecting to HMRC.
 */
public record HmrcPrerequisite(
    String id,
    String title,
    String description,
    String helpUrl,
    boolean isComplete
) {
    /**
     * Creates a prerequisite with completion status.
     */
    public HmrcPrerequisite withComplete(boolean complete) {
        return new HmrcPrerequisite(id, title, description, helpUrl, complete);
    }
}
