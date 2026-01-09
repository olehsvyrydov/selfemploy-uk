package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;
import uk.selfemploy.common.domain.Business;
import uk.selfemploy.common.enums.BusinessType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for Business.
 */
@Entity
@Table(name = "businesses")
public class BusinessEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 10)
    private String utr;

    @Column(name = "accounting_period_start")
    private LocalDate accountingPeriodStart;

    @Column(name = "accounting_period_end")
    private LocalDate accountingPeriodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessType type;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    // Default constructor for JPA
    public BusinessEntity() {}

    /**
     * Creates a JPA entity from a domain Business.
     */
    public static BusinessEntity fromDomain(Business business) {
        BusinessEntity entity = new BusinessEntity();
        entity.id = business.id();
        entity.name = business.name();
        entity.utr = business.utr();
        entity.accountingPeriodStart = business.accountingPeriodStart();
        entity.accountingPeriodEnd = business.accountingPeriodEnd();
        entity.type = business.type();
        entity.description = business.description();
        entity.active = business.active();
        return entity;
    }

    /**
     * Converts this entity to a domain Business.
     */
    public Business toDomain() {
        return new Business(
            id,
            name,
            utr,
            accountingPeriodStart,
            accountingPeriodEnd,
            type,
            description,
            active
        );
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUtr() { return utr; }
    public void setUtr(String utr) { this.utr = utr; }
    public LocalDate getAccountingPeriodStart() { return accountingPeriodStart; }
    public void setAccountingPeriodStart(LocalDate accountingPeriodStart) { this.accountingPeriodStart = accountingPeriodStart; }
    public LocalDate getAccountingPeriodEnd() { return accountingPeriodEnd; }
    public void setAccountingPeriodEnd(LocalDate accountingPeriodEnd) { this.accountingPeriodEnd = accountingPeriodEnd; }
    public BusinessType getType() { return type; }
    public void setType(BusinessType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
