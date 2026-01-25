package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewModel for the Import Review page.
 * Manages the state of import candidates being reviewed.
 */
public class ImportReviewViewModel {

    // Import candidates
    private final ObservableList<ImportCandidateViewModel> candidates = FXCollections.observableArrayList();

    // Summary counts
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    private final IntegerProperty newCount = new SimpleIntegerProperty(0);
    private final IntegerProperty exactCount = new SimpleIntegerProperty(0);
    private final IntegerProperty likelyCount = new SimpleIntegerProperty(0);
    private final IntegerProperty selectedCount = new SimpleIntegerProperty(0);
    private final IntegerProperty importCount = new SimpleIntegerProperty(0);

    // Loading state
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty loadingMessage = new SimpleStringProperty("Checking for duplicates...");

    /**
     * Creates a new ImportReviewViewModel.
     */
    public ImportReviewViewModel() {
        // Update counts when candidates change
        candidates.addListener((javafx.collections.ListChangeListener<ImportCandidateViewModel>) c -> {
            updateCounts();
        });
    }

    /**
     * Sets the candidates to review.
     *
     * @param newCandidates The list of import candidates
     */
    public void setCandidates(List<ImportCandidateViewModel> newCandidates) {
        candidates.setAll(newCandidates);

        // Listen for selection changes on each candidate
        for (ImportCandidateViewModel candidate : candidates) {
            candidate.selectedProperty().addListener((obs, oldVal, newVal) -> updateSelectedCount());
            candidate.actionProperty().addListener((obs, oldVal, newVal) -> updateImportCount());
        }

        updateCounts();
    }

    /**
     * Updates all summary counts.
     */
    private void updateCounts() {
        totalCount.set(candidates.size());

        int newCnt = 0, exactCnt = 0, likelyCnt = 0;
        for (ImportCandidateViewModel candidate : candidates) {
            switch (candidate.getMatchType()) {
                case NEW -> newCnt++;
                case EXACT -> exactCnt++;
                case LIKELY -> likelyCnt++;
            }
        }

        newCount.set(newCnt);
        exactCount.set(exactCnt);
        likelyCount.set(likelyCnt);

        updateSelectedCount();
        updateImportCount();
    }

    private void updateSelectedCount() {
        int count = 0;
        for (ImportCandidateViewModel candidate : candidates) {
            if (candidate.isSelected()) {
                count++;
            }
        }
        selectedCount.set(count);
    }

    private void updateImportCount() {
        int count = 0;
        for (ImportCandidateViewModel candidate : candidates) {
            if (candidate.willBeImported()) {
                count++;
            }
        }
        importCount.set(count);
    }

    // === Bulk Actions ===

    /**
     * Selects all candidates.
     */
    public void selectAll() {
        for (ImportCandidateViewModel candidate : candidates) {
            candidate.setSelected(true);
        }
    }

    /**
     * Deselects all candidates.
     */
    public void deselectAll() {
        for (ImportCandidateViewModel candidate : candidates) {
            candidate.setSelected(false);
        }
    }

    /**
     * Sets all NEW items to IMPORT action.
     */
    public void importAllNew() {
        for (ImportCandidateViewModel candidate : candidates) {
            if (candidate.getMatchType() == MatchType.NEW) {
                candidate.setAction(ImportAction.IMPORT);
            }
        }
    }

    /**
     * Sets all EXACT items to SKIP action.
     */
    public void skipAllDuplicates() {
        for (ImportCandidateViewModel candidate : candidates) {
            if (candidate.getMatchType() == MatchType.EXACT) {
                candidate.setAction(ImportAction.SKIP);
            }
        }
    }

    /**
     * Sets action for selected items.
     *
     * @param action The action to set
     */
    public void setActionForSelected(ImportAction action) {
        for (ImportCandidateViewModel candidate : candidates) {
            if (candidate.isSelected()) {
                candidate.setAction(action);
            }
        }
    }

    // === Filtering ===

    /**
     * Returns candidates filtered by match type.
     *
     * @param matchType The match type to filter by (null for all)
     * @return Filtered list of candidates
     */
    public List<ImportCandidateViewModel> getCandidatesByMatchType(MatchType matchType) {
        if (matchType == null) {
            return List.copyOf(candidates);
        }
        return candidates.stream()
            .filter(c -> c.getMatchType() == matchType)
            .collect(Collectors.toList());
    }

    /**
     * Returns candidates that will be imported.
     */
    public List<ImportCandidateViewModel> getCandidatesToImport() {
        return candidates.stream()
            .filter(ImportCandidateViewModel::willBeImported)
            .collect(Collectors.toList());
    }

    /**
     * Returns the total amount of income to be imported.
     */
    public BigDecimal getTotalIncomeToImport() {
        return getCandidatesToImport().stream()
            .filter(ImportCandidateViewModel::isIncome)
            .map(ImportCandidateViewModel::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns the total amount of expenses to be imported.
     */
    public BigDecimal getTotalExpensesToImport() {
        return getCandidatesToImport().stream()
            .filter(ImportCandidateViewModel::isExpense)
            .map(c -> c.getAmount().abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // === State Checks ===

    /**
     * Returns true if there are no duplicates found.
     */
    public boolean hasNoDuplicates() {
        return exactCount.get() == 0 && likelyCount.get() == 0;
    }

    /**
     * Returns true if all items are new.
     */
    public boolean isAllNew() {
        return newCount.get() == totalCount.get();
    }

    /**
     * Returns true if there are candidates to review.
     */
    public boolean hasCandidates() {
        return !candidates.isEmpty();
    }

    /**
     * Returns true if any items are selected for import.
     */
    public boolean hasItemsToImport() {
        return importCount.get() > 0;
    }

    // === Properties ===

    public ObservableList<ImportCandidateViewModel> getCandidates() {
        return candidates;
    }

    public int getTotalCount() {
        return totalCount.get();
    }

    public IntegerProperty totalCountProperty() {
        return totalCount;
    }

    public int getNewCount() {
        return newCount.get();
    }

    public IntegerProperty newCountProperty() {
        return newCount;
    }

    public int getExactCount() {
        return exactCount.get();
    }

    public IntegerProperty exactCountProperty() {
        return exactCount;
    }

    public int getLikelyCount() {
        return likelyCount.get();
    }

    public IntegerProperty likelyCountProperty() {
        return likelyCount;
    }

    public int getSelectedCount() {
        return selectedCount.get();
    }

    public IntegerProperty selectedCountProperty() {
        return selectedCount;
    }

    public int getImportCount() {
        return importCount.get();
    }

    public IntegerProperty importCountProperty() {
        return importCount;
    }

    public boolean isLoading() {
        return loading.get();
    }

    public void setLoading(boolean loading) {
        this.loading.set(loading);
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public String getLoadingMessage() {
        return loadingMessage.get();
    }

    public void setLoadingMessage(String message) {
        this.loadingMessage.set(message);
    }

    public StringProperty loadingMessageProperty() {
        return loadingMessage;
    }
}
