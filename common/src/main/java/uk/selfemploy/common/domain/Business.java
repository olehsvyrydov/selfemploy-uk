package uk.selfemploy.common.domain;

import uk.selfemploy.common.enums.BusinessType;

import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

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
    String description,
    boolean active
) {
    private static final Pattern UTR_PATTERN = Pattern.compile("^\\d{10}$");

    /**
     * Compact constructor for validation.
     */
    public Business {
        validateName(name);
        validateUtr(utr);
        validateType(type);
        validateDates(accountingPeriodStart, accountingPeriodEnd);
    }

    /**
     * Creates a new active business with a generated ID.
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
            description,
            true  // active by default
        );
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Business name cannot be null or empty");
        }
    }

    private static void validateUtr(String utr) {
        // UTR is optional (businesses may not have one yet)
        if (utr != null && !UTR_PATTERN.matcher(utr).matches()) {
            throw new IllegalArgumentException("UTR must be exactly 10 digits");
        }
    }

    private static void validateType(BusinessType type) {
        if (type == null) {
            throw new IllegalArgumentException("Business type cannot be null");
        }
    }

    private static void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("Accounting period end date cannot be before start date");
        }
    }
}
