package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.enums.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for Income.
 */
@Entity
@Table(name = "incomes")
public class IncomeEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncomeCategory category;

    private String reference;

    // Default constructor for JPA
    public IncomeEntity() {}

    /**
     * Creates a JPA entity from a domain Income.
     */
    public static IncomeEntity fromDomain(Income income) {
        IncomeEntity entity = new IncomeEntity();
        entity.id = income.id();
        entity.businessId = income.businessId();
        entity.date = income.date();
        entity.amount = income.amount();
        entity.description = income.description();
        entity.category = income.category();
        entity.reference = income.reference();
        return entity;
    }

    /**
     * Converts this entity to a domain Income.
     */
    public Income toDomain() {
        return new Income(
            id,
            businessId,
            date,
            amount,
            description,
            category,
            reference
        );
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID businessId) { this.businessId = businessId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public IncomeCategory getCategory() { return category; }
    public void setCategory(IncomeCategory category) { this.category = category; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
}
