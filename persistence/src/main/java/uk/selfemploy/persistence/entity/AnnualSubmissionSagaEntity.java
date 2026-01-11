package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;
import uk.selfemploy.common.domain.AnnualSubmissionSaga;
import uk.selfemploy.common.domain.AnnualSubmissionState;
import uk.selfemploy.common.domain.TaxCalculationResult;
import uk.selfemploy.common.domain.TaxYear;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for Annual Submission Saga.
 *
 * <p>Persists saga state to enable resume capability after failures.
 */
@Entity
@Table(name = "annual_submission_sagas", indexes = {
    @Index(name = "idx_annual_saga_nino_tax_year", columnList = "nino, tax_year_start", unique = true),
    @Index(name = "idx_annual_saga_state", columnList = "state")
})
public class AnnualSubmissionSagaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 9)
    private String nino;

    @Column(name = "tax_year_start", nullable = false)
    private int taxYearStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnualSubmissionState state;

    @Column(name = "calculation_id", length = 100)
    private String calculationId;

    // Tax calculation result fields
    @Column(name = "total_income", precision = 12, scale = 2)
    private BigDecimal totalIncome;

    @Column(name = "total_expenses", precision = 12, scale = 2)
    private BigDecimal totalExpenses;

    @Column(name = "net_profit", precision = 12, scale = 2)
    private BigDecimal netProfit;

    @Column(name = "income_tax", precision = 12, scale = 2)
    private BigDecimal incomeTax;

    @Column(name = "ni_class2", precision = 12, scale = 2)
    private BigDecimal nationalInsuranceClass2;

    @Column(name = "ni_class4", precision = 12, scale = 2)
    private BigDecimal nationalInsuranceClass4;

    @Column(name = "total_tax_liability", precision = 12, scale = 2)
    private BigDecimal totalTaxLiability;

    @Column(name = "hmrc_confirmation", length = 100)
    private String hmrcConfirmation;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default constructor for JPA
    public AnnualSubmissionSagaEntity() {}

    /**
     * Creates a JPA entity from a domain AnnualSubmissionSaga.
     */
    public static AnnualSubmissionSagaEntity fromDomain(AnnualSubmissionSaga saga) {
        AnnualSubmissionSagaEntity entity = new AnnualSubmissionSagaEntity();
        entity.id = saga.id();
        entity.nino = saga.nino();
        entity.taxYearStart = saga.taxYear().startYear();
        entity.state = saga.state();
        entity.calculationId = saga.calculationId();
        entity.hmrcConfirmation = saga.hmrcConfirmation();
        entity.errorMessage = saga.errorMessage();
        entity.createdAt = saga.createdAt();
        entity.updatedAt = saga.updatedAt();

        // Map calculation result if present
        if (saga.calculationResult() != null) {
            TaxCalculationResult result = saga.calculationResult();
            entity.totalIncome = result.totalIncome();
            entity.totalExpenses = result.totalExpenses();
            entity.netProfit = result.netProfit();
            entity.incomeTax = result.incomeTax();
            entity.nationalInsuranceClass2 = result.nationalInsuranceClass2();
            entity.nationalInsuranceClass4 = result.nationalInsuranceClass4();
            entity.totalTaxLiability = result.totalTaxLiability();
        }

        return entity;
    }

    /**
     * Converts this entity to a domain AnnualSubmissionSaga.
     */
    public AnnualSubmissionSaga toDomain() {
        TaxYear taxYear = TaxYear.of(taxYearStart);

        TaxCalculationResult calculationResult = null;
        if (totalIncome != null && state.ordinal() >= AnnualSubmissionState.CALCULATED.ordinal()) {
            calculationResult = new TaxCalculationResult(
                UUID.randomUUID(), // New ID for the result object
                calculationId,
                totalIncome,
                totalExpenses,
                netProfit,
                incomeTax,
                nationalInsuranceClass2,
                nationalInsuranceClass4,
                totalTaxLiability,
                updatedAt
            );
        }

        return new AnnualSubmissionSaga(
            id,
            nino,
            taxYear,
            state,
            calculationId,
            calculationResult,
            hmrcConfirmation,
            errorMessage,
            createdAt,
            updatedAt
        );
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNino() { return nino; }
    public void setNino(String nino) { this.nino = nino; }

    public int getTaxYearStart() { return taxYearStart; }
    public void setTaxYearStart(int taxYearStart) { this.taxYearStart = taxYearStart; }

    public AnnualSubmissionState getState() { return state; }
    public void setState(AnnualSubmissionState state) { this.state = state; }

    public String getCalculationId() { return calculationId; }
    public void setCalculationId(String calculationId) { this.calculationId = calculationId; }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }

    public BigDecimal getIncomeTax() { return incomeTax; }
    public void setIncomeTax(BigDecimal incomeTax) { this.incomeTax = incomeTax; }

    public BigDecimal getNationalInsuranceClass2() { return nationalInsuranceClass2; }
    public void setNationalInsuranceClass2(BigDecimal nationalInsuranceClass2) {
        this.nationalInsuranceClass2 = nationalInsuranceClass2;
    }

    public BigDecimal getNationalInsuranceClass4() { return nationalInsuranceClass4; }
    public void setNationalInsuranceClass4(BigDecimal nationalInsuranceClass4) {
        this.nationalInsuranceClass4 = nationalInsuranceClass4;
    }

    public BigDecimal getTotalTaxLiability() { return totalTaxLiability; }
    public void setTotalTaxLiability(BigDecimal totalTaxLiability) {
        this.totalTaxLiability = totalTaxLiability;
    }

    public String getHmrcConfirmation() { return hmrcConfirmation; }
    public void setHmrcConfirmation(String hmrcConfirmation) { this.hmrcConfirmation = hmrcConfirmation; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
