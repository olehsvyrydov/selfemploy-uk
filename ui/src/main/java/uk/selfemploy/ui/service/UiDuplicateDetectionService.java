package uk.selfemploy.ui.service;

import org.apache.commons.text.similarity.LevenshteinDistance;
import uk.selfemploy.common.domain.Expense;
import uk.selfemploy.common.domain.Income;
import uk.selfemploy.common.domain.TaxYear;
import uk.selfemploy.core.service.ExpenseService;
import uk.selfemploy.core.service.IncomeService;
import uk.selfemploy.ui.viewmodel.ImportCandidateViewModel;
import uk.selfemploy.ui.viewmodel.MatchType;
import uk.selfemploy.ui.viewmodel.MatchedRecordViewModel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

/**
 * UI-layer duplicate detection service for import review.
 *
 * <p>Compares imported records against existing data to detect duplicates
 * using a 3-tier matching system:</p>
 * <ul>
 *   <li>EXACT: Same date + amount + description (case-insensitive)</li>
 *   <li>LIKELY: Same date + amount + Levenshtein similarity >= 80%</li>
 *   <li>NEW: No match found</li>
 * </ul>
 *
 * <p>BUG-10B-002: Integration with Settings import flow.</p>
 */
public class UiDuplicateDetectionService {

    private static final Logger LOG = Logger.getLogger(UiDuplicateDetectionService.class.getName());
    private static final double FUZZY_MATCH_THRESHOLD = 0.80;
    private static final LevenshteinDistance LEVENSHTEIN = new LevenshteinDistance();

    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final UUID businessId;

    public UiDuplicateDetectionService(IncomeService incomeService, ExpenseService expenseService, UUID businessId) {
        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.businessId = businessId;
    }

    /**
     * Analyzes imported income records for duplicates.
     *
     * @param importedIncomes List of income records to analyze
     * @param taxYear Tax year to check against
     * @return List of import candidates with match information
     */
    public List<ImportCandidateViewModel> analyzeIncomes(List<Income> importedIncomes, TaxYear taxYear) {
        if (importedIncomes == null || importedIncomes.isEmpty()) {
            return Collections.emptyList();
        }

        // Load existing incomes for the tax year
        List<Income> existingIncomes = incomeService.findByTaxYear(businessId, taxYear);
        Map<String, ExistingRecord> exactMatchMap = buildExactMatchMapForIncomes(existingIncomes);
        List<ExistingRecord> allRecords = buildRecordListForIncomes(existingIncomes);

        LOG.info("Analyzing " + importedIncomes.size() + " imports against " + existingIncomes.size() + " existing incomes");

        List<ImportCandidateViewModel> candidates = new ArrayList<>();
        for (Income imported : importedIncomes) {
            ImportCandidateViewModel candidate = analyzeIncome(imported, exactMatchMap, allRecords);
            candidates.add(candidate);
        }

        return candidates;
    }

    /**
     * Analyzes imported expense records for duplicates.
     *
     * @param importedExpenses List of expense records to analyze
     * @param taxYear Tax year to check against
     * @return List of import candidates with match information
     */
    public List<ImportCandidateViewModel> analyzeExpenses(List<Expense> importedExpenses, TaxYear taxYear) {
        if (importedExpenses == null || importedExpenses.isEmpty()) {
            return Collections.emptyList();
        }

        // Load existing expenses for the tax year
        List<Expense> existingExpenses = expenseService.findByTaxYear(businessId, taxYear);
        Map<String, ExistingRecord> exactMatchMap = buildExactMatchMapForExpenses(existingExpenses);
        List<ExistingRecord> allRecords = buildRecordListForExpenses(existingExpenses);

        LOG.info("Analyzing " + importedExpenses.size() + " imports against " + existingExpenses.size() + " existing expenses");

        List<ImportCandidateViewModel> candidates = new ArrayList<>();
        for (Expense imported : importedExpenses) {
            ImportCandidateViewModel candidate = analyzeExpense(imported, exactMatchMap, allRecords);
            candidates.add(candidate);
        }

        return candidates;
    }

    private ImportCandidateViewModel analyzeIncome(Income imported,
                                                    Map<String, ExistingRecord> exactMatchMap,
                                                    List<ExistingRecord> allRecords) {
        String exactKey = createExactKey(imported.date(), imported.amount(), imported.description());

        // Check exact match
        if (exactMatchMap.containsKey(exactKey)) {
            ExistingRecord match = exactMatchMap.get(exactKey);
            return createCandidate(imported.id(), imported.date(), imported.description(),
                imported.amount(), MatchType.EXACT, match.id(), match);
        }

        // Check fuzzy match
        Optional<FuzzyMatch> fuzzyMatch = findBestFuzzyMatch(imported.date(), imported.amount(),
            imported.description(), allRecords);
        if (fuzzyMatch.isPresent() && fuzzyMatch.get().similarity() >= FUZZY_MATCH_THRESHOLD) {
            ExistingRecord match = fuzzyMatch.get().record();
            return createCandidate(imported.id(), imported.date(), imported.description(),
                imported.amount(), MatchType.LIKELY, match.id(), match);
        }

        // No match - new record
        return createCandidate(imported.id(), imported.date(), imported.description(),
            imported.amount(), MatchType.NEW, null, null);
    }

    private ImportCandidateViewModel analyzeExpense(Expense imported,
                                                     Map<String, ExistingRecord> exactMatchMap,
                                                     List<ExistingRecord> allRecords) {
        String exactKey = createExactKey(imported.date(), imported.amount(), imported.description());

        // Check exact match
        if (exactMatchMap.containsKey(exactKey)) {
            ExistingRecord match = exactMatchMap.get(exactKey);
            // Expenses are shown as negative amounts
            return createCandidate(imported.id(), imported.date(), imported.description(),
                imported.amount().negate(), MatchType.EXACT, match.id(), match);
        }

        // Check fuzzy match
        Optional<FuzzyMatch> fuzzyMatch = findBestFuzzyMatch(imported.date(), imported.amount(),
            imported.description(), allRecords);
        if (fuzzyMatch.isPresent() && fuzzyMatch.get().similarity() >= FUZZY_MATCH_THRESHOLD) {
            ExistingRecord match = fuzzyMatch.get().record();
            return createCandidate(imported.id(), imported.date(), imported.description(),
                imported.amount().negate(), MatchType.LIKELY, match.id(), match);
        }

        // No match - new record
        return createCandidate(imported.id(), imported.date(), imported.description(),
            imported.amount().negate(), MatchType.NEW, null, null);
    }

    private ImportCandidateViewModel createCandidate(UUID id, LocalDate date, String description,
                                                      BigDecimal amount, MatchType matchType,
                                                      UUID matchedId, ExistingRecord matchedRecord) {
        MatchedRecordViewModel matched = null;
        if (matchedRecord != null) {
            matched = new MatchedRecordViewModel(
                matchedRecord.id(),
                matchedRecord.date(),
                matchedRecord.description(),
                matchedRecord.amount(),
                matchedRecord.category()
            );
        }
        return new ImportCandidateViewModel(id != null ? id : UUID.randomUUID(),
            date, description, amount, null, matchType, matchedId, matched);
    }

    private Optional<FuzzyMatch> findBestFuzzyMatch(LocalDate date, BigDecimal amount,
                                                     String description, List<ExistingRecord> records) {
        String normalizedDesc = normalizeDescription(description);

        return records.stream()
            .filter(r -> r.date().equals(date))
            .filter(r -> r.amount().compareTo(amount) == 0)
            .map(r -> new FuzzyMatch(r, calculateSimilarity(normalizedDesc, normalizeDescription(r.description()))))
            .filter(fm -> fm.similarity() >= FUZZY_MATCH_THRESHOLD)
            .max(Comparator.comparingDouble(FuzzyMatch::similarity));
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        int distance = LEVENSHTEIN.apply(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        return 1.0 - ((double) distance / maxLength);
    }

    private String createExactKey(LocalDate date, BigDecimal amount, String description) {
        String normalizedDesc = normalizeDescription(description);
        return String.format("%s|%s|%s",
            date.toString(),
            amount.stripTrailingZeros().toPlainString(),
            normalizedDesc);
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        return description.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private Map<String, ExistingRecord> buildExactMatchMapForIncomes(List<Income> incomes) {
        Map<String, ExistingRecord> map = new HashMap<>();
        for (Income income : incomes) {
            ExistingRecord record = new ExistingRecord(
                income.id(),
                income.date(),
                income.amount(),
                income.description(),
                income.category() != null ? income.category().getDisplayName() : ""
            );
            map.put(createExactKey(income.date(), income.amount(), income.description()), record);
        }
        return map;
    }

    private Map<String, ExistingRecord> buildExactMatchMapForExpenses(List<Expense> expenses) {
        Map<String, ExistingRecord> map = new HashMap<>();
        for (Expense expense : expenses) {
            ExistingRecord record = new ExistingRecord(
                expense.id(),
                expense.date(),
                expense.amount(),
                expense.description(),
                expense.category() != null ? expense.category().getDisplayName() : ""
            );
            map.put(createExactKey(expense.date(), expense.amount(), expense.description()), record);
        }
        return map;
    }

    private List<ExistingRecord> buildRecordListForIncomes(List<Income> incomes) {
        List<ExistingRecord> records = new ArrayList<>();
        for (Income income : incomes) {
            records.add(new ExistingRecord(
                income.id(),
                income.date(),
                income.amount(),
                income.description(),
                income.category() != null ? income.category().getDisplayName() : ""
            ));
        }
        return records;
    }

    private List<ExistingRecord> buildRecordListForExpenses(List<Expense> expenses) {
        List<ExistingRecord> records = new ArrayList<>();
        for (Expense expense : expenses) {
            records.add(new ExistingRecord(
                expense.id(),
                expense.date(),
                expense.amount(),
                expense.description(),
                expense.category() != null ? expense.category().getDisplayName() : ""
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
        String category
    ) {}

    /**
     * Internal record for fuzzy match results.
     */
    private record FuzzyMatch(ExistingRecord record, double similarity) {}
}
