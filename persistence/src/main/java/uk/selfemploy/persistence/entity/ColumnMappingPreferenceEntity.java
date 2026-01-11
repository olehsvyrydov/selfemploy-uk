package uk.selfemploy.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for storing column mapping preferences.
 * Allows users to save and reuse mapping configurations for bank imports.
 *
 * SE-802: Bank Import Column Mapping Wizard
 */
@Entity
@Table(name = "column_mapping_preferences")
public class ColumnMappingPreferenceEntity {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    /**
     * Bank identifier - e.g., "BARCLAYS", "HSBC", or hash of headers for unknown formats.
     */
    @Column(name = "bank_identifier", nullable = false, length = 100)
    private String bankIdentifier;

    /**
     * User-friendly name for this mapping (e.g., "My Barclays Business Account").
     */
    @Column(name = "mapping_name", length = 100)
    private String mappingName;

    // Column mapping indexes (nullable - not all columns may be mapped)

    @Column(name = "date_column_index")
    private Integer dateColumnIndex;

    @Column(name = "date_column_name", length = 100)
    private String dateColumnName;

    @Column(name = "description_column_index")
    private Integer descriptionColumnIndex;

    @Column(name = "description_column_name", length = 100)
    private String descriptionColumnName;

    @Column(name = "amount_column_index")
    private Integer amountColumnIndex;

    @Column(name = "amount_column_name", length = 100)
    private String amountColumnName;

    @Column(name = "income_column_index")
    private Integer incomeColumnIndex;

    @Column(name = "income_column_name", length = 100)
    private String incomeColumnName;

    @Column(name = "expense_column_index")
    private Integer expenseColumnIndex;

    @Column(name = "expense_column_name", length = 100)
    private String expenseColumnName;

    @Column(name = "category_column_index")
    private Integer categoryColumnIndex;

    @Column(name = "category_column_name", length = 100)
    private String categoryColumnName;

    /**
     * Date format pattern (e.g., "dd/MM/yyyy").
     */
    @Column(name = "date_format", length = 50)
    private String dateFormat;

    /**
     * Amount interpretation: STANDARD, INVERTED, or SEPARATE_COLUMNS.
     */
    @Column(name = "amount_interpretation", nullable = false, length = 20)
    private String amountInterpretation;

    /**
     * When this mapping was first created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * When this mapping was last used for import.
     */
    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    /**
     * Number of times this mapping has been used.
     */
    @Column(name = "use_count", nullable = false)
    private int useCount;

    // Default constructor for JPA
    public ColumnMappingPreferenceEntity() {
    }

    // Static factory methods

    /**
     * Creates a new entity with a generated UUID.
     */
    public static ColumnMappingPreferenceEntity create(UUID businessId, String bankIdentifier) {
        ColumnMappingPreferenceEntity entity = new ColumnMappingPreferenceEntity();
        entity.id = UUID.randomUUID();
        entity.businessId = businessId;
        entity.bankIdentifier = bankIdentifier;
        entity.amountInterpretation = "STANDARD";
        entity.createdAt = Instant.now();
        entity.lastUsedAt = Instant.now();
        entity.useCount = 0;
        return entity;
    }

    // Business methods

    /**
     * Marks this preference as used, updating the timestamp and count.
     */
    public void markUsed() {
        this.lastUsedAt = Instant.now();
        this.useCount++;
    }

    // Getters and setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public String getBankIdentifier() {
        return bankIdentifier;
    }

    public void setBankIdentifier(String bankIdentifier) {
        this.bankIdentifier = bankIdentifier;
    }

    public String getMappingName() {
        return mappingName;
    }

    public void setMappingName(String mappingName) {
        this.mappingName = mappingName;
    }

    public Integer getDateColumnIndex() {
        return dateColumnIndex;
    }

    public void setDateColumnIndex(Integer dateColumnIndex) {
        this.dateColumnIndex = dateColumnIndex;
    }

    public String getDateColumnName() {
        return dateColumnName;
    }

    public void setDateColumnName(String dateColumnName) {
        this.dateColumnName = dateColumnName;
    }

    public Integer getDescriptionColumnIndex() {
        return descriptionColumnIndex;
    }

    public void setDescriptionColumnIndex(Integer descriptionColumnIndex) {
        this.descriptionColumnIndex = descriptionColumnIndex;
    }

    public String getDescriptionColumnName() {
        return descriptionColumnName;
    }

    public void setDescriptionColumnName(String descriptionColumnName) {
        this.descriptionColumnName = descriptionColumnName;
    }

    public Integer getAmountColumnIndex() {
        return amountColumnIndex;
    }

    public void setAmountColumnIndex(Integer amountColumnIndex) {
        this.amountColumnIndex = amountColumnIndex;
    }

    public String getAmountColumnName() {
        return amountColumnName;
    }

    public void setAmountColumnName(String amountColumnName) {
        this.amountColumnName = amountColumnName;
    }

    public Integer getIncomeColumnIndex() {
        return incomeColumnIndex;
    }

    public void setIncomeColumnIndex(Integer incomeColumnIndex) {
        this.incomeColumnIndex = incomeColumnIndex;
    }

    public String getIncomeColumnName() {
        return incomeColumnName;
    }

    public void setIncomeColumnName(String incomeColumnName) {
        this.incomeColumnName = incomeColumnName;
    }

    public Integer getExpenseColumnIndex() {
        return expenseColumnIndex;
    }

    public void setExpenseColumnIndex(Integer expenseColumnIndex) {
        this.expenseColumnIndex = expenseColumnIndex;
    }

    public String getExpenseColumnName() {
        return expenseColumnName;
    }

    public void setExpenseColumnName(String expenseColumnName) {
        this.expenseColumnName = expenseColumnName;
    }

    public Integer getCategoryColumnIndex() {
        return categoryColumnIndex;
    }

    public void setCategoryColumnIndex(Integer categoryColumnIndex) {
        this.categoryColumnIndex = categoryColumnIndex;
    }

    public String getCategoryColumnName() {
        return categoryColumnName;
    }

    public void setCategoryColumnName(String categoryColumnName) {
        this.categoryColumnName = categoryColumnName;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getAmountInterpretation() {
        return amountInterpretation;
    }

    public void setAmountInterpretation(String amountInterpretation) {
        this.amountInterpretation = amountInterpretation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }
}
