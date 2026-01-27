package uk.selfemploy.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import uk.selfemploy.common.domain.Submission;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.common.enums.SubmissionStatus;
import uk.selfemploy.common.enums.SubmissionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

/**
 * JPA entity for HMRC Submission.
 */
@Entity
@Table(name = "submissions",
    indexes = {
        @Index(name = "idx_submissions_business_id", columnList = "business_id"),
        @Index(name = "idx_submissions_tax_year", columnList = "tax_year_start"),
        @Index(name = "idx_submissions_type_status", columnList = "type, status"),
        @Index(name = "idx_submissions_declaration_at", columnList = "declaration_accepted_at"),
        @Index(name = "idx_submissions_utr", columnList = "utr"),
        @Index(name = "idx_submissions_nino", columnList = "nino"),
        @Index(name = "idx_submissions_retention", columnList = "retention_required_until"),
        @Index(name = "idx_submissions_deletable", columnList = "is_deletable"),
        @Index(name = "idx_submissions_saga_id", columnList = "saga_id"),
        @Index(name = "idx_submissions_calculation_id", columnList = "calculation_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_submission_business_year_type",
            columnNames = {"business_id", "tax_year_start", "type"}
        )
    }
)
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

    /**
     * UTC timestamp when the user accepted the HMRC declaration.
     * Required for audit trail compliance.
     */
    @Column(name = "declaration_accepted_at")
    private Instant declarationAcceptedAt;

    /**
     * SHA-256 hash of the declaration text for version tracking.
     * 64 lowercase hex characters.
     */
    @Column(name = "declaration_text_hash", length = 64)
    private String declarationTextHash;

    /**
     * Unique Taxpayer Reference (UTR) - 10 digits.
     * Links submission to the business's tax record at HMRC.
     */
    @Column(name = "utr", length = 10)
    private String utr;

    /**
     * National Insurance Number (NINO).
     * Format: 2 letters + 6 digits + 1 letter suffix (A-D).
     * Stored in uppercase.
     */
    @Column(name = "nino", length = 9)
    private String nino;

    // === Retention Policy Fields (SE-SH-002) ===

    /**
     * Date until which this submission must be retained per HMRC 6-year rule.
     * Calculated as: Filing deadline (31 Jan following tax year) + 6 years.
     */
    @Column(name = "retention_required_until")
    private LocalDate retentionRequiredUntil;

    /**
     * Whether this submission can be deleted.
     * Only true if retention period has expired AND deletion has been approved.
     */
    @Column(name = "is_deletable")
    private Boolean isDeletable = false;

    /**
     * Timestamp when deletion was approved.
     */
    @Column(name = "deletion_approved_at")
    private Instant deletionApprovedAt;

    /**
     * Who approved the deletion (user email or SYSTEM).
     */
    @Column(name = "deletion_approved_by", length = 100)
    private String deletionApprovedBy;

    /**
     * Reason for approving deletion.
     */
    @Column(name = "deletion_reason", length = 255)
    private String deletionReason;

    // === Saga Sync Fields (SE-SH-003) ===

    /**
     * HMRC calculation ID returned from trigger calculation API.
     */
    @Column(name = "calculation_id", length = 100)
    private String calculationId;

    /**
     * HMRC charge reference from final declaration submission.
     */
    @Column(name = "charge_reference", length = 100)
    private String chargeReference;

    /**
     * Calculated income tax liability from HMRC.
     */
    @Column(name = "income_tax", precision = 12, scale = 2)
    private BigDecimal incomeTax;

    /**
     * National Insurance Class 4 liability.
     */
    @Column(name = "ni_class4", precision = 12, scale = 2)
    private BigDecimal niClass4;

    /**
     * Total tax + NI payable to HMRC.
     */
    @Column(name = "total_tax_liability", precision = 12, scale = 2)
    private BigDecimal totalTaxLiability;

    /**
     * Links to annual_submission_sagas.id for idempotency.
     */
    @Column(name = "saga_id")
    private UUID sagaId;

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
        entity.declarationAcceptedAt = submission.declarationAcceptedAt();
        entity.declarationTextHash = submission.declarationTextHash();
        entity.utr = submission.utr();
        entity.nino = submission.nino();
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
            updatedAt,
            declarationAcceptedAt,
            declarationTextHash,
            utr,
            nino
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

    public Instant getDeclarationAcceptedAt() { return declarationAcceptedAt; }
    public void setDeclarationAcceptedAt(Instant declarationAcceptedAt) { this.declarationAcceptedAt = declarationAcceptedAt; }

    public String getDeclarationTextHash() { return declarationTextHash; }
    public void setDeclarationTextHash(String declarationTextHash) { this.declarationTextHash = declarationTextHash; }

    public String getUtr() { return utr; }
    public void setUtr(String utr) { this.utr = utr; }

    public String getNino() { return nino; }
    public void setNino(String nino) { this.nino = nino; }

    public LocalDate getRetentionRequiredUntil() { return retentionRequiredUntil; }
    public void setRetentionRequiredUntil(LocalDate retentionRequiredUntil) { this.retentionRequiredUntil = retentionRequiredUntil; }

    public Boolean getIsDeletable() { return isDeletable; }
    public void setIsDeletable(Boolean isDeletable) { this.isDeletable = isDeletable; }

    public Instant getDeletionApprovedAt() { return deletionApprovedAt; }
    public void setDeletionApprovedAt(Instant deletionApprovedAt) { this.deletionApprovedAt = deletionApprovedAt; }

    public String getDeletionApprovedBy() { return deletionApprovedBy; }
    public void setDeletionApprovedBy(String deletionApprovedBy) { this.deletionApprovedBy = deletionApprovedBy; }

    public String getDeletionReason() { return deletionReason; }
    public void setDeletionReason(String deletionReason) { this.deletionReason = deletionReason; }

    /**
     * Calculates and sets the retention date based on the tax year.
     * Retention = Filing deadline (31 Jan following tax year) + 6 years.
     */
    public void calculateRetentionDate() {
        // Filing deadline is 31 January following the tax year end
        // Tax year 2024/25 ends 5 April 2025, deadline is 31 January 2026
        // Retention = deadline + 6 years = 31 January 2032
        // Formula: tax_year_start + 7 years = retention year
        this.retentionRequiredUntil = LocalDate.of(taxYearStart + 7, Month.JANUARY, 31);
    }

    /**
     * Checks if the retention period has expired.
     *
     * @param currentDate the current date to compare against
     * @return true if retention period has expired
     */
    public boolean isRetentionExpired(LocalDate currentDate) {
        return retentionRequiredUntil != null && currentDate.isAfter(retentionRequiredUntil);
    }

    // === Saga Sync Getters/Setters ===

    public String getCalculationId() { return calculationId; }
    public void setCalculationId(String calculationId) { this.calculationId = calculationId; }

    public String getChargeReference() { return chargeReference; }
    public void setChargeReference(String chargeReference) { this.chargeReference = chargeReference; }

    public BigDecimal getIncomeTax() { return incomeTax; }
    public void setIncomeTax(BigDecimal incomeTax) { this.incomeTax = incomeTax; }

    public BigDecimal getNiClass4() { return niClass4; }
    public void setNiClass4(BigDecimal niClass4) { this.niClass4 = niClass4; }

    public BigDecimal getTotalTaxLiability() { return totalTaxLiability; }
    public void setTotalTaxLiability(BigDecimal totalTaxLiability) { this.totalTaxLiability = totalTaxLiability; }

    public UUID getSagaId() { return sagaId; }
    public void setSagaId(UUID sagaId) { this.sagaId = sagaId; }

    /**
     * Updates this entity from a completed annual submission saga.
     *
     * @param saga the completed saga
     */
    public void updateFromSaga(uk.selfemploy.common.domain.AnnualSubmissionSaga saga) {
        this.sagaId = saga.id();
        this.calculationId = saga.calculationId();
        this.chargeReference = saga.hmrcConfirmation();
        if (saga.calculationResult() != null) {
            this.incomeTax = saga.calculationResult().incomeTax();
            this.niClass4 = saga.calculationResult().nationalInsuranceClass4();
            this.totalTaxLiability = saga.calculationResult().totalTaxLiability();
        }
    }
}
