package uk.selfemploy.ui.help;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents help content for a specific topic.
 *
 * <p>SE-701: In-App Help System</p>
 *
 * <p>Each HelpContent contains:</p>
 * <ul>
 *   <li><b>title</b> - Short title for the help topic</li>
 *   <li><b>body</b> - Main explanation text</li>
 *   <li><b>hmrcLink</b> - Optional link to HMRC guidance</li>
 *   <li><b>linkText</b> - Text to display for the link</li>
 * </ul>
 *
 * @param title    the help topic title (required)
 * @param body     the help body text (required)
 * @param hmrcLink optional URL to HMRC guidance
 * @param linkText text to display for the link (default: "View HMRC guidance")
 */
public record HelpContent(
        String title,
        String body,
        String hmrcLink,
        String linkText
) {
    private static final String DEFAULT_LINK_TEXT = "View HMRC guidance";

    /**
     * Creates a HelpContent with default link text.
     *
     * @param title    the help topic title
     * @param body     the help body text
     * @param hmrcLink optional URL to HMRC guidance (can be null)
     */
    public HelpContent(String title, String body, String hmrcLink) {
        this(title, body, hmrcLink, hmrcLink != null ? DEFAULT_LINK_TEXT : "");
    }

    /**
     * Canonical constructor with validation.
     */
    public HelpContent {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(body, "body must not be null");

        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (body.isBlank()) {
            throw new IllegalArgumentException("body must not be blank");
        }

        // Normalize empty link text
        if (linkText == null) {
            linkText = hmrcLink != null ? DEFAULT_LINK_TEXT : "";
        }
    }

    /**
     * Returns the HMRC link as an Optional.
     *
     * @return Optional containing the link URL, or empty if no link
     */
    public Optional<String> hmrcLinkOptional() {
        return Optional.ofNullable(hmrcLink);
    }

    /**
     * Returns true if this help content has an HMRC link.
     *
     * @return true if link is present
     */
    public boolean hasLink() {
        return hmrcLink != null && !hmrcLink.isBlank();
    }

    /**
     * Creates a new builder for HelpContent.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for HelpContent.
     */
    public static class Builder {
        private String title;
        private String body;
        private String hmrcLink;
        private String linkText;

        /**
         * Sets the title.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the body text.
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets the HMRC link URL.
         */
        public Builder hmrcLink(String hmrcLink) {
            this.hmrcLink = hmrcLink;
            return this;
        }

        /**
         * Sets the link display text.
         */
        public Builder linkText(String linkText) {
            this.linkText = linkText;
            return this;
        }

        /**
         * Builds the HelpContent instance.
         *
         * @return a new HelpContent
         */
        public HelpContent build() {
            return new HelpContent(title, body, hmrcLink, linkText);
        }
    }
}
