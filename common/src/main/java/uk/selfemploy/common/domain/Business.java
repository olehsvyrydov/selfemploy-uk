package uk.selfemploy.common.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a self-employed business entity.
 *
 * This is the core domain entity representing the user's business,
 * including their UTR (Unique Taxpayer Reference) for HMRC submissions.
 */
public record Business(
    UUID id,
    String name,
    String utr,
    LocalDate accountingPeriodStart,
    LocalDate accountingPeriodEnd,
    BusinessType type,
    String description
) {
    /**
     * Creates a new business with a generated ID.
     */
    public static Business create(
            String name,
            String utr,
            LocalDate accountingPeriodStart,
            LocalDate accountingPeriodEnd,
            BusinessType type,
            String description) {
        return new Business(
            UUID.randomUUID(),
            name,
            utr,
            accountingPeriodStart,
            accountingPeriodEnd,
            type,
            description
        );
    }
}
