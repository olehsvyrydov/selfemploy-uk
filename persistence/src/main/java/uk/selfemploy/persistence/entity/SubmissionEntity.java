package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity for HMRC Submission.
 */
@Entity
@Table(name = "submissions", indexes = {
    @Index(name = "idx_submissions_business_id", columnList = "business_id"),
    @Index(name = "idx_submissions_tax_year", columnList = "tax_year_start"),
    @Index(name = "idx_submissions_type_status", columnList = "type, status")
})
public class SubmissionEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionType type;

    @Column(name = "tax_year_start", nullable = false)
    private int taxYearStart;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expenses", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalExpenses;

    @Column(name = "net_profit", nullable = false, precision = 12, scale = 2)
    private BigDecimal netProfit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(name = "hmrc_reference")
    private String hmrcReference;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor for JPA
    public SubmissionEntity() {}

    /**
     * Creates a JPA entity from a domain Submission.
     */
    public static SubmissionEntity fromDomain(Submission submission) {
        SubmissionEntity entity = new SubmissionEntity();
        entity.id = submission.id();
        entity.businessId = submission.businessId();
        entity.type = submission.type();
        entity.taxYearStart = submission.taxYear().startYear();
        entity.periodStart = submission.periodStart();
        entity.periodEnd = submission.periodEnd();
        entity.totalIncome = submission.totalIncome();
        entity.totalExpenses = submission.totalExpenses();
        entity.netProfit = submission.netProfit();
        entity.status = submission.status();
        entity.hmrcReference = submission.hmrcReference();
        entity.errorMessage = submission.errorMessage();
        entity.submittedAt = submission.submittedAt();
        entity.updatedAt = submission.updatedAt();
        return entity;
    }

    /**
     * Converts this entity to a domain Submission.
     */
    public Submission toDomain() {
        return new Submission(
            id,
            businessId,
            type,
            TaxYear.of(taxYearStart),
            periodStart,
            periodEnd,
            totalIncome,
            totalExpenses,
            netProfit,
            status,
            hmrcReference,
            errorMessage,
            submittedAt,
            updatedAt
        );
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBusinessId() { return businessId; }
    public void setBusinessId(UUID businessId) { this.businessId = businessId; }

    public SubmissionType getType() { return type; }
    public void setType(SubmissionType type) { this.type = type; }

    public int getTaxYearStart() { return taxYearStart; }
    public void setTaxYearStart(int taxYearStart) { this.taxYearStart = taxYearStart; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }

    public String getHmrcReference() { return hmrcReference; }
    public void setHmrcReference(String hmrcReference) { this.hmrcReference = hmrcReference; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
