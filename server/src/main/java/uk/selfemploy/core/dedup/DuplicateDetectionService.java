package uk.selfemploy.core.dedup;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import uk.selfemploy.core.bankimport.ImportedTransaction;
import uk.selfemploy.core.reconciliation.MatchingUtils;
import uk.selfemploy.persistence.entity.ExpenseEntity;
import uk.selfemploy.persistence.entity.IncomeEntity;
import uk.selfemploy.persistence.repository.ExpenseRepository;
import uk.selfemploy.persistence.repository.IncomeRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Enhanced duplicate detection service for import operations.
 *
 * <p>Implements a 3-tier detection system per ADR-10B-003:</p>
 * <ul>
 *   <li>EXACT: Same date + amount + description (case-insensitive)</li>
 *   <li>LIKELY: Same date + amount + Levenshtein distance &lt;= 3</li>
 *   <li>DATE_ONLY: Same date + similar amount (+/- 5%)</li>
 * </ul>
 *
 * <p>Performance target: &lt; 5 seconds for 10,000 existing records vs 500 imports.</p>
 */
@ApplicationScoped
public class DuplicateDetectionService {

    private static final int MAX_LEVENSHTEIN_DISTANCE = 3;
    private static final double FUZZY_MATCH_THRESHOLD = MatchingUtils.LIKELY_THRESHOLD;
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.05"); // 5%

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;

    @Inject
    public DuplicateDetectionService(IncomeRepository incomeRepository,
                                     ExpenseRepository expenseRepository) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Detects duplicates for a list of imported transactions.
     *
     * @param imports the list of imported transactions to check
     * @param businessId the business ID to check against
     * @return list of duplicate match results, one per imported transaction
     */
    public List<DuplicateMatch> detectDuplicates(List<ImportedTransaction> imports, UUID businessId) {
        if (imports == null || imports.isEmpty()) {
            return Collections.emptyList();
        }

        // Find date range of imports
        LocalDate minDate = imports.stream()
            .map(ImportedTransaction::date)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now());

        LocalDate maxDate = imports.stream()
            .map(ImportedTransaction::date)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now());

        // Load existing records in date range
        List<IncomeEntity> existingIncomes = incomeRepository.findEntitiesByDateRange(
            businessId, minDate, maxDate);
        List<ExpenseEntity> existingExpenses = expenseRepository.findEntitiesByDateRange(
            businessId, minDate, maxDate);

        // Build lookup maps for efficient comparison
        Map<String, ExistingRecord> exactMatchMap = buildExactMatchMap(existingIncomes, existingExpenses);
        List<ExistingRecord> allRecords = buildRecordList(existingIncomes, existingExpenses);

        // Analyze each imported transaction
        List<DuplicateMatch> results = new ArrayList<>();
        for (ImportedTransaction imported : imports) {
            DuplicateMatch match = analyzeTransaction(imported, exactMatchMap, allRecords);
            results.add(match);
        }

        return results;
    }

    /**
     * Analyzes a single imported transaction for duplicates.
     */
    private DuplicateMatch analyzeTransaction(
            ImportedTransaction imported,
            Map<String, ExistingRecord> exactMatchMap,
            List<ExistingRecord> allRecords) {

        // 1. Check exact match (fast path)
        String exactKey = createExactKey(imported);
        if (exactMatchMap.containsKey(exactKey)) {
            ExistingRecord match = exactMatchMap.get(exactKey);
            return DuplicateMatch.exactMatch(imported, match.id(), match.description());
        }

        // 2. Check likely matches (same date + amount, similar description)
        Optional<FuzzyMatch> likelyMatch = findLikelyMatch(imported, allRecords);
        if (likelyMatch.isPresent() && likelyMatch.get().similarity() >= FUZZY_MATCH_THRESHOLD) {
            ExistingRecord record = likelyMatch.get().record();
            return DuplicateMatch.likelyMatch(imported, likelyMatch.get().similarity(),
                record.id(), record.description());
        }

        // 3. Check date-only matches (same date, any record)
        Optional<ExistingRecord> dateMatch = findDateOnlyMatch(imported, allRecords);
        if (dateMatch.isPresent()) {
            return DuplicateMatch.dateOnlyMatch(imported, dateMatch.get().id(),
                dateMatch.get().description());
        }

        // 4. No match found
        return DuplicateMatch.noMatch(imported);
    }

    /**
     * Finds the best likely match (same date + amount, similar description).
     */
    private Optional<FuzzyMatch> findLikelyMatch(ImportedTransaction imported, List<ExistingRecord> records) {
        BigDecimal importAmount = imported.amount().abs();
        String normalizedImportDesc = MatchingUtils.normalizeDescription(imported.description());

        return records.stream()
            .filter(r -> r.date().equals(imported.date()))
            .filter(r -> r.amount().compareTo(importAmount) == 0)
            .map(r -> new FuzzyMatch(r, MatchingUtils.calculateSimilarity(normalizedImportDesc,
                MatchingUtils.normalizeDescription(r.description()))))
            .filter(fm -> fm.similarity() >= FUZZY_MATCH_THRESHOLD)
            .max(Comparator.comparingDouble(FuzzyMatch::similarity));
    }

    /**
     * Finds a date-only match (same date, similar amount within 5%).
     */
    private Optional<ExistingRecord> findDateOnlyMatch(ImportedTransaction imported, List<ExistingRecord> records) {
        BigDecimal importAmount = imported.amount().abs();
        BigDecimal lowerBound = importAmount.multiply(BigDecimal.ONE.subtract(AMOUNT_TOLERANCE));
        BigDecimal upperBound = importAmount.multiply(BigDecimal.ONE.add(AMOUNT_TOLERANCE));

        return records.stream()
            .filter(r -> r.date().equals(imported.date()))
            .filter(r -> {
                BigDecimal amount = r.amount();
                return amount.compareTo(lowerBound) >= 0 && amount.compareTo(upperBound) <= 0;
            })
            .findFirst();
    }

    /**
     * Creates an exact match key for a transaction.
     * Delegates to shared MatchingUtils for consistent normalization.
     */
    private String createExactKey(ImportedTransaction tx) {
        return MatchingUtils.createExactKey(tx.date(), tx.amount().abs(), tx.description());
    }

    /**
     * Creates an exact match key for an existing record.
     * Delegates to shared MatchingUtils for consistent normalization.
     */
    private String createExactKey(ExistingRecord record) {
        return MatchingUtils.createExactKey(record.date(), record.amount(), record.description());
    }

    /**
     * Builds an exact match lookup map for efficient O(1) lookups.
     */
    private Map<String, ExistingRecord> buildExactMatchMap(
            List<IncomeEntity> incomes, List<ExpenseEntity> expenses) {

        Map<String, ExistingRecord> map = new HashMap<>();

        for (IncomeEntity income : incomes) {
            ExistingRecord record = new ExistingRecord(
                income.getId(),
                income.getDate(),
                income.getAmount(),
                income.getDescription(),
                true
            );
            map.put(createExactKey(record), record);
        }

        for (ExpenseEntity expense : expenses) {
            ExistingRecord record = new ExistingRecord(
                expense.getId(),
                expense.getDate(),
                expense.getAmount(),  // Stored as positive
                expense.getDescription(),
                false
            );
            map.put(createExactKey(record), record);
        }

        return map;
    }

    /**
     * Builds a list of all existing records for fuzzy matching.
     */
    private List<ExistingRecord> buildRecordList(
            List<IncomeEntity> incomes, List<ExpenseEntity> expenses) {

        List<ExistingRecord> records = new ArrayList<>();

        for (IncomeEntity income : incomes) {
            records.add(new ExistingRecord(
                income.getId(),
                income.getDate(),
                income.getAmount(),
                income.getDescription(),
                true
            ));
        }

        for (ExpenseEntity expense : expenses) {
            records.add(new ExistingRecord(
                expense.getId(),
                expense.getDate(),
                expense.getAmount(),
                expense.getDescription(),
                false
            ));
        }

        return records;
    }

    /**
     * Internal record representing an existing database record.
     */
    private record ExistingRecord(
        UUID id,
        LocalDate date,
        BigDecimal amount,
        String description,
        boolean isIncome
    ) {}

    /**
     * Internal record for fuzzy match results.
     */
    private record FuzzyMatch(ExistingRecord record, double similarity) {}
}
