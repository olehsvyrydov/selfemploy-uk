package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;
import uk.selfemploy.common.domain.TaxYear;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for TaxYear.
 */
@Entity
@Table(name = "tax_years")
public class TaxYearEntity {

    @Id
    private UUID id;

    @Column(name = "start_year", nullable = false, unique = true)
    private int startYear;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String label;

    // Default constructor for JPA
    public TaxYearEntity() {}

    /**
     * Creates a JPA entity from a domain TaxYear.
     */
    public static TaxYearEntity fromDomain(TaxYear taxYear) {
        TaxYearEntity entity = new TaxYearEntity();
        entity.id = taxYear.id();
        entity.startYear = taxYear.startYear();
        entity.startDate = taxYear.startDate();
        entity.endDate = taxYear.endDate();
        entity.label = taxYear.label();
        return entity;
    }

    /**
     * Converts this entity to a domain TaxYear.
     */
    public TaxYear toDomain() {
        return new TaxYear(
            id,
            startYear,
            startDate,
            endDate,
            label
        );
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getStartYear() { return startYear; }
    public void setStartYear(int startYear) { this.startYear = startYear; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
