package uk.selfemploy.core.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable record representing the complete 6-checkbox submission declaration.
 *
 * <p>SE-512: This captures all 6 HMRC-required declaration confirmations before
 * annual Self Assessment submission. Each declaration must be individually
 * confirmed with a timestamp.</p>
 *
 * <h3>The 6 Required Declarations:</h3>
 * <ol>
 *   <li><b>accuracy_statement</b> - Information is correct and complete (HMRC official)</li>
 *   <li><b>penalties_warning</b> - Understands penalties for false information (HMRC official)</li>
 *   <li><b>record_keeping</b> - Records kept and will be retained for 5 years</li>
 *   <li><b>calculation_verification</b> - Tax calculation has been reviewed</li>
 *   <li><b>legal_effect</b> - Submission is a legal act</li>
 *   <li><b>identity_confirmation</b> - User is the taxpayer or authorised agent</li>
 * </ol>
 *
 * @param declarationId Unique ID in format DECL-YYYYMMDD-HHMMSS-XXXXX
 * @param taxYear       Tax year for the submission (e.g., "2025-26")
 * @param completedAt   UTC timestamp when all 6 declarations were confirmed
 * @param items         List of 6 individual declaration items with timestamps
 *
 * @see <a href="https://www.gov.uk/self-assessment-tax-returns/sending-return">HMRC Self Assessment</a>
 */
public record SubmissionDeclaration(
    String declarationId,
    String taxYear,
    Instant completedAt,
    List<DeclarationItem> items
) {

    /**
     * The number of declarations required for a complete submission.
     */
    public static final int DECLARATION_COUNT = 6;

    /**
     * Declaration keys in the required order.
     */
    private static final List<String> DECLARATION_KEYS = List.of(
        "accuracy_statement",
        "penalties_warning",
        "record_keeping",
        "calculation_verification",
        "legal_effect",
        "identity_confirmation"
    );

    /**
     * HMRC-required declaration texts.
     */
    private static final Map<String, String> DECLARATION_TEXTS = Map.of(
        "accuracy_statement",
        "I declare that the information I have given on this tax return and any " +
        "supplementary pages is correct and complete to the best of my knowledge and belief.",

        "penalties_warning",
        "I understand that I may have to pay financial penalties and face prosecution " +
        "if I give false information.",

        "record_keeping",
        "I confirm that I have kept records to support the entries in this return and " +
        "will retain them for at least 5 years from the filing deadline.",

        "calculation_verification",
        "I have reviewed the tax calculation and believe it to be accurate based on " +
        "the information I have provided.",

        "legal_effect",
        "I understand that submitting this return to HMRC is a legal act and has the " +
        "same effect as signing a paper return.",

        "identity_confirmation",
        "I confirm that I am the person whose details appear on this return, or I am " +
        "authorised to submit on their behalf."
    );

    private static final DateTimeFormatter ID_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    /**
     * Compact constructor for validation.
     */
    public SubmissionDeclaration {
        if (declarationId == null || declarationId.isBlank()) {
            throw new IllegalArgumentException("Declaration ID is required");
        }
        if (taxYear == null || taxYear.isBlank()) {
            throw new IllegalArgumentException("Tax year is required");
        }
        if (completedAt == null) {
            throw new IllegalArgumentException("Completion timestamp is required");
        }
        if (items == null || items.size() != DECLARATION_COUNT) {
            throw new IllegalArgumentException("Exactly 6 declaration items are required");
        }
        // Make items immutable
        items = List.copyOf(items);
    }

    /**
     * Returns the ordered list of declaration keys.
     *
     * @return immutable list of 6 declaration keys
     */
    public static List<String> getDeclarationKeys() {
        return DECLARATION_KEYS;
    }

    /**
     * Returns the declaration text for a given key.
     *
     * @param key the declaration key
     * @return the full declaration text
     * @throws IllegalArgumentException if key is not recognized
     */
    public static String getDeclarationText(String key) {
        String text = DECLARATION_TEXTS.get(key);
        if (text == null) {
            throw new IllegalArgumentException("Unknown declaration key: " + key);
        }
        return text;
    }

    /**
     * Creates a new builder for constructing a SubmissionDeclaration.
     *
     * @param clock clock for timestamps (allows testing with fixed clock)
     * @return new builder instance
     */
    public static Builder builder(Clock clock) {
        return new Builder(clock);
    }

    /**
     * Creates a new builder using the system clock.
     *
     * @return new builder instance with system clock
     */
    public static Builder builder() {
        return new Builder(Clock.systemUTC());
    }

    /**
     * Individual declaration item within the submission.
     *
     * @param index       1-based index (1-6)
     * @param key         declaration key (e.g., "accuracy_statement")
     * @param confirmed   whether this declaration has been confirmed
     * @param confirmedAt UTC timestamp when confirmed, or null if not confirmed
     */
    public record DeclarationItem(
        int index,
        String key,
        boolean confirmed,
        Instant confirmedAt
    ) {
        /**
         * Compact constructor for validation.
         */
        public DeclarationItem {
            if (index < 1 || index > DECLARATION_COUNT) {
                throw new IllegalArgumentException("Index must be 1-6, got: " + index);
            }
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Declaration key is required");
            }
            if (confirmed && confirmedAt == null) {
                throw new IllegalArgumentException("Confirmed items must have a timestamp");
            }
        }
    }

    /**
     * Builder for constructing SubmissionDeclaration instances.
     *
     * <p>Tracks confirmation state for each declaration and only allows
     * building when all 6 are confirmed.</p>
     */
    public static class Builder {

        private final Clock clock;
        private final Map<String, Instant> confirmations;
        private String taxYear;

        Builder(Clock clock) {
            this.clock = clock;
            this.confirmations = new LinkedHashMap<>();
        }

        /**
         * Sets the tax year for this declaration.
         *
         * @param taxYear tax year in format "YYYY-YY" (e.g., "2025-26")
         * @return this builder for chaining
         */
        public Builder forTaxYear(String taxYear) {
            this.taxYear = taxYear;
            return this;
        }

        /**
         * Confirms a declaration with the current timestamp.
         *
         * @param key the declaration key to confirm
         * @return this builder for chaining
         * @throws IllegalArgumentException if key is not recognized
         */
        public Builder confirm(String key) {
            validateKey(key);
            confirmations.put(key, clock.instant());
            return this;
        }

        /**
         * Removes confirmation for a declaration.
         *
         * @param key the declaration key to unconfirm
         * @return this builder for chaining
         */
        public Builder unconfirm(String key) {
            confirmations.remove(key);
            return this;
        }

        /**
         * Checks if a specific declaration is confirmed.
         *
         * @param key the declaration key
         * @return true if confirmed
         */
        public boolean isConfirmed(String key) {
            return confirmations.containsKey(key);
        }

        /**
         * Gets the timestamp when a declaration was confirmed.
         *
         * @param key the declaration key
         * @return timestamp or null if not confirmed
         */
        public Instant getConfirmedAt(String key) {
            return confirmations.get(key);
        }

        /**
         * Returns the number of confirmed declarations.
         *
         * @return count of confirmations (0-6)
         */
        public int getConfirmedCount() {
            return confirmations.size();
        }

        /**
         * Checks if all 6 declarations are confirmed.
         *
         * @return true if complete
         */
        public boolean isComplete() {
            return confirmations.size() == DECLARATION_COUNT;
        }

        /**
         * Returns progress text for UI display.
         *
         * @return text like "3 of 6 confirmations completed"
         */
        public String getProgressText() {
            return String.format("%d of %d confirmations completed",
                getConfirmedCount(), DECLARATION_COUNT);
        }

        /**
         * Builds the immutable SubmissionDeclaration.
         *
         * @return completed declaration
         * @throws IllegalStateException if not all 6 are confirmed or tax year not set
         */
        public SubmissionDeclaration build() {
            if (taxYear == null || taxYear.isBlank()) {
                throw new IllegalStateException("Tax year must be set before building");
            }
            if (!isComplete()) {
                throw new IllegalStateException(
                    "All 6 declarations must be confirmed before building. " +
                    "Currently " + getConfirmedCount() + " of 6 confirmed."
                );
            }

            Instant now = clock.instant();
            String declarationId = generateDeclarationId(now);
            List<DeclarationItem> items = buildItems();

            return new SubmissionDeclaration(declarationId, taxYear, now, items);
        }

        private void validateKey(String key) {
            if (!DECLARATION_KEYS.contains(key)) {
                throw new IllegalArgumentException("Unknown declaration key: " + key);
            }
        }

        private String generateDeclarationId(Instant timestamp) {
            String datePart = ID_DATE_FORMATTER.format(timestamp);
            String uniquePart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 5)
                .toUpperCase();
            return "DECL-" + datePart + "-" + uniquePart;
        }

        private List<DeclarationItem> buildItems() {
            List<DeclarationItem> items = new ArrayList<>();
            int index = 1;
            for (String key : DECLARATION_KEYS) {
                Instant confirmedAt = confirmations.get(key);
                items.add(new DeclarationItem(index++, key, true, confirmedAt));
            }
            return items;
        }
    }
}
