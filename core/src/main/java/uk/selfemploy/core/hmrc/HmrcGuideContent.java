package uk.selfemploy.core.hmrc;

import java.util.List;

/**
 * Content for HMRC registration guide.
 */
public record HmrcGuideContent(
    String title,
    String introduction,
    List<GuideStep> steps,
    String helpUrl
) {
    /**
     * A single step in the guide.
     */
    public record GuideStep(
        int number,
        String title,
        String description,
        String actionUrl
    ) {}
}
