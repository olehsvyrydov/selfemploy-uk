package uk.selfemploy.common.domain;

import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents an income transaction for a self-employed business.
 *
 * Income is mapped to SA103 form boxes 9 (Turnover) and 10 (Other Income).
 */
public record Income(
    UUID id,
    UUID businessId,
    LocalDate date,
    BigDecimal amount,
    String description,
    IncomeCategory category,
    String reference
) {
    /**
     * Compact constructor for validation.
     */
    public Income {
        validateBusinessId(businessId);
        validateDate(date);
        validateAmount(amount);
        validateDescription(description);
        validateCategory(category);
    }

    /**
     * Creates a new income with a generated ID.
     */
    public static Income create(
            UUID businessId,
            LocalDate date,
            BigDecimal amount,
            String description,
            IncomeCategory category,
            String reference) {
        return new Income(
            UUID.randomUUID(),
            businessId,
            date,
            amount,
            description,
            category,
            reference
        );
    }

    private static void validateBusinessId(UUID businessId) {
        if (businessId == null) {
            throw new IllegalArgumentException("businessId cannot be null");
        }
    }

    private static void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Income date cannot be null");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Income amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Income amount must be positive");
        }
    }

    private static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Income description cannot be null or empty");
        }
    }

    private static void validateCategory(IncomeCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Income category cannot be null");
        }
    }
}
