package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewModel for the Import History page.
 * Manages the list of past imports with filtering and undo capabilities.
 */
public class ImportHistoryViewModel {

    // All imports
    private final ObservableList<ImportHistoryItemViewModel> allImports = FXCollections.observableArrayList();

    // Filtered imports
    private final ObservableList<ImportHistoryItemViewModel> filteredImports = FXCollections.observableArrayList();

    // Current filter
    private final ObjectProperty<ImportHistoryFilter> filter = new SimpleObjectProperty<>(ImportHistoryFilter.ALL_TIME);

    // Summary
    private final IntegerProperty totalCount = new SimpleIntegerProperty(0);
    private final IntegerProperty activeCount = new SimpleIntegerProperty(0);
    private final IntegerProperty undoableCount = new SimpleIntegerProperty(0);

    // Loading state
    private final BooleanProperty loading = new SimpleBooleanProperty(false);

    /**
     * Creates a new ImportHistoryViewModel.
     */
    public ImportHistoryViewModel() {
        // Apply filter when it changes
        filter.addListener((obs, oldVal, newVal) -> applyFilter());
    }

    /**
     * Sets the import history items.
     *
     * @param imports The list of import history items
     */
    public void setImports(List<ImportHistoryItemViewModel> imports) {
        allImports.setAll(imports);
        applyFilter();
        updateCounts();
    }

    /**
     * Applies the current filter to the import list.
     */
    private void applyFilter() {
        List<ImportHistoryItemViewModel> filtered = allImports.stream()
            .filter(item -> filter.get().matches(item.getImportDateTime()))
            .sorted(Comparator.comparing(ImportHistoryItemViewModel::getImportDateTime).reversed())
            .collect(Collectors.toList());

        filteredImports.setAll(filtered);
    }

    /**
     * Updates summary counts.
     */
    private void updateCounts() {
        totalCount.set(allImports.size());

        int active = 0;
        int undoable = 0;

        for (ImportHistoryItemViewModel item : allImports) {
            if (item.getStatus() == ImportStatus.ACTIVE) {
                active++;
            }
            if (item.canUndo()) {
                undoable++;
            }
        }

        activeCount.set(active);
        undoableCount.set(undoable);
    }

    /**
     * Removes an import from the list (after successful undo).
     *
     * @param item The item to remove
     */
    public void removeImport(ImportHistoryItemViewModel item) {
        allImports.remove(item);
        filteredImports.remove(item);
        updateCounts();
    }

    /**
     * Updates an import's status (e.g., after undo).
     *
     * @param item The item to update
     * @param newStatus The new status
     */
    public void updateImportStatus(ImportHistoryItemViewModel item, ImportStatus newStatus) {
        // The item would need to be replaced with a new one with the updated status
        // For now, we'll just refresh from the service
        updateCounts();
    }

    /**
     * Returns true if there are no imports.
     */
    public boolean isEmpty() {
        return allImports.isEmpty();
    }

    /**
     * Returns true if the filtered list is empty but there are imports.
     */
    public boolean isFilteredEmpty() {
        return filteredImports.isEmpty() && !allImports.isEmpty();
    }

    // === Properties ===

    public ObservableList<ImportHistoryItemViewModel> getFilteredImports() {
        return filteredImports;
    }

    public ObservableList<ImportHistoryItemViewModel> getAllImports() {
        return allImports;
    }

    public ImportHistoryFilter getFilter() {
        return filter.get();
    }

    public void setFilter(ImportHistoryFilter filter) {
        this.filter.set(filter);
    }

    public ObjectProperty<ImportHistoryFilter> filterProperty() {
        return filter;
    }

    public int getTotalCount() {
        return totalCount.get();
    }

    public IntegerProperty totalCountProperty() {
        return totalCount;
    }

    public int getActiveCount() {
        return activeCount.get();
    }

    public IntegerProperty activeCountProperty() {
        return activeCount;
    }

    public int getUndoableCount() {
        return undoableCount.get();
    }

    public IntegerProperty undoableCountProperty() {
        return undoableCount;
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
}
