package uk.selfemploy.ui.viewmodel;

import javafx.beans.property.*;
import uk.selfemploy.common.enums.BusinessType;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ViewModel for the User Onboarding Wizard.
 * Manages the 4-step wizard flow: Welcome, Your Details, Tax Year, Business Type.
 *
 * SE-702: User Onboarding Wizard
 */
public class OnboardingViewModel {

    private static final int TOTAL_STEPS = 4;
    private static final int MIN_NAME_LENGTH = 2;

    /**
     * Pattern for UK National Insurance Number.
     * Format: 2 letters + 6 digits + 1 letter (with optional spaces).
     * Example: AB123456C or AB 12 34 56 C
     */
    private static final Pattern NI_NUMBER_PATTERN = Pattern.compile(
            "^[A-Za-z]{2}\\s?\\d{2}\\s?\\d{2}\\s?\\d{2}\\s?[A-Za-z]$"
    );

    /**
     * Pattern for UK Unique Taxpayer Reference.
     * Format: exactly 10 digits.
     */
    private static final Pattern UTR_PATTERN = Pattern.compile("^\\d{10}$");

    // === Wizard State ===
    private final IntegerProperty currentStep = new SimpleIntegerProperty(1);
    private final BooleanProperty completed = new SimpleBooleanProperty(false);

    // === Step 2: Your Details ===
    private final StringProperty userName = new SimpleStringProperty("");
    private final StringProperty utr = new SimpleStringProperty("");
    private final StringProperty niNumber = new SimpleStringProperty("");

    // === Step 3: Tax Year ===
    private final ObjectProperty<String> selectedTaxYear = new SimpleObjectProperty<>();

    // === Step 4: Business Type ===
    private final ObjectProperty<BusinessType> selectedBusinessType = new SimpleObjectProperty<>();

    /**
     * Creates a new OnboardingViewModel.
     */
    public OnboardingViewModel() {
        // ViewModel initialization - no dependencies needed
    }

    // =====================================================
    // WIZARD NAVIGATION
    // =====================================================

    public int getCurrentStep() {
        return currentStep.get();
    }

    public IntegerProperty currentStepProperty() {
        return currentStep;
    }

    public int getTotalSteps() {
        return TOTAL_STEPS;
    }

    public boolean canGoNext() {
        return switch (currentStep.get()) {
            case 1 -> true; // Welcome - always can proceed
            case 2 -> isStep2Valid();
            case 3 -> isStep3Valid();
            case 4 -> false; // Final step - use complete() instead
            default -> false;
        };
    }

    public boolean canGoPrevious() {
        return currentStep.get() > 1;
    }

    public void goToNextStep() {
        if (canGoNext() && currentStep.get() < TOTAL_STEPS) {
            currentStep.set(currentStep.get() + 1);
        }
    }

    public void goToPreviousStep() {
        if (canGoPrevious()) {
            currentStep.set(currentStep.get() - 1);
        }
    }

    public String getStepLabel(int step) {
        return switch (step) {
            case 1 -> "Welcome";
            case 2 -> "Your Details";
            case 3 -> "Tax Year";
            case 4 -> "Business Type";
            default -> "";
        };
    }

    public boolean isStepCompleted(int step) {
        return switch (step) {
            case 1 -> true; // Welcome always completed
            case 2 -> isNameValid();
            case 3 -> selectedTaxYear.get() != null;
            case 4 -> false; // Never pre-completed
            default -> false;
        };
    }

    public boolean isStepActive(int step) {
        return currentStep.get() == step;
    }

    // =====================================================
    // STEP 2: YOUR DETAILS
    // =====================================================

    public String getUserName() {
        return userName.get();
    }

    public void setUserName(String name) {
        userName.set(name);
    }

    public StringProperty userNameProperty() {
        return userName;
    }

    public boolean isNameValid() {
        String name = userName.get();
        return name != null && name.trim().length() >= MIN_NAME_LENGTH;
    }

    public String getUtr() {
        return utr.get();
    }

    public void setUtr(String value) {
        utr.set(value != null ? value : "");
    }

    public StringProperty utrProperty() {
        return utr;
    }

    public boolean isUtrValid() {
        String value = utr.get();
        if (value == null || value.isEmpty()) {
            return true; // Empty is valid (optional)
        }
        return UTR_PATTERN.matcher(value).matches();
    }

    public String getNiNumber() {
        return niNumber.get();
    }

    public void setNiNumber(String value) {
        niNumber.set(value != null ? value : "");
    }

    public StringProperty niNumberProperty() {
        return niNumber;
    }

    public boolean isNiNumberValid() {
        String value = niNumber.get();
        if (value == null || value.isEmpty()) {
            return true; // Empty is valid (optional)
        }
        return NI_NUMBER_PATTERN.matcher(value.toUpperCase()).matches();
    }

    private boolean isStep2Valid() {
        // Name is required, UTR and NI are optional but must be valid if provided
        if (!isNameValid()) {
            return false;
        }
        // If UTR is partially filled, it must be complete
        String utrValue = utr.get();
        if (utrValue != null && !utrValue.isEmpty() && !isUtrValid()) {
            return false;
        }
        // If NI is partially filled, it must be complete
        String niValue = niNumber.get();
        if (niValue != null && !niValue.isEmpty() && !isNiNumberValid()) {
            return false;
        }
        return true;
    }

    // === UTR Segmented Input ===

    public String getUtrSegment1() {
        String value = utr.get();
        if (value == null || value.length() < 4) {
            return value != null ? value : "";
        }
        return value.substring(0, 4);
    }

    public String getUtrSegment2() {
        String value = utr.get();
        if (value == null || value.length() < 7) {
            return value != null && value.length() > 4 ? value.substring(4) : "";
        }
        return value.substring(4, 7);
    }

    public String getUtrSegment3() {
        String value = utr.get();
        if (value == null || value.length() <= 7) {
            return value != null && value.length() > 7 ? value.substring(7) : "";
        }
        return value.substring(7);
    }

    public void setUtrFromSegments(String seg1, String seg2, String seg3) {
        StringBuilder sb = new StringBuilder();
        if (seg1 != null) sb.append(seg1);
        if (seg2 != null) sb.append(seg2);
        if (seg3 != null) sb.append(seg3);
        utr.set(sb.toString());
    }

    // =====================================================
    // STEP 3: TAX YEAR
    // =====================================================

    public List<String> getAvailableTaxYears() {
        // Provide tax years from 2023/24 to current + 1
        return List.of("2023/24", "2024/25", "2025/26");
    }

    public String getSelectedTaxYear() {
        return selectedTaxYear.get();
    }

    public void setSelectedTaxYear(String taxYear) {
        selectedTaxYear.set(taxYear);
    }

    public ObjectProperty<String> selectedTaxYearProperty() {
        return selectedTaxYear;
    }

    public String getRecommendedTaxYear() {
        // Current tax year (April to April)
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        // Tax year runs 6 April to 5 April
        if (now.getMonthValue() < 4 || (now.getMonthValue() == 4 && now.getDayOfMonth() < 6)) {
            year -= 1;
        }
        return year + "/" + String.format("%02d", (year + 1) % 100);
    }

    public String getTaxYearDateRange(String taxYear) {
        if (taxYear == null || !taxYear.contains("/")) {
            return "";
        }
        String[] parts = taxYear.split("/");
        int startYear = Integer.parseInt(parts[0]);
        int endYear = startYear + 1;
        return String.format("6 Apr %d - 5 Apr %d", startYear, endYear);
    }

    public boolean isTaxYearRecommended(String taxYear) {
        return taxYear != null && taxYear.equals(getRecommendedTaxYear());
    }

    private boolean isStep3Valid() {
        return selectedTaxYear.get() != null && !selectedTaxYear.get().isEmpty();
    }

    // =====================================================
    // STEP 4: BUSINESS TYPE
    // =====================================================

    public List<BusinessType> getAvailableBusinessTypes() {
        return List.of(
                BusinessType.SOLE_TRADER,
                BusinessType.FREELANCER,
                BusinessType.CONTRACTOR,
                BusinessType.PARTNERSHIP // Disabled but shown
        );
    }

    public BusinessType getSelectedBusinessType() {
        return selectedBusinessType.get();
    }

    public void setSelectedBusinessType(BusinessType type) {
        selectedBusinessType.set(type);
    }

    public ObjectProperty<BusinessType> selectedBusinessTypeProperty() {
        return selectedBusinessType;
    }

    public String getBusinessTypeDisplayName(BusinessType type) {
        if (type == null) return "";
        return type.getDisplayName();
    }

    public String getBusinessTypeDescription(BusinessType type) {
        if (type == null) return "";
        return switch (type) {
            case SOLE_TRADER -> "Working independently for clients";
            case FREELANCER -> "Working on projects and gigs";
            case CONTRACTOR -> "Providing services to businesses";
            case PARTNERSHIP -> "Business with 2+ partners";
            case LIMITED_COMPANY -> "Registered limited company";
            default -> "";
        };
    }

    public boolean isBusinessTypeEnabled(BusinessType type) {
        if (type == null) return false;
        return type.isEnabled();
    }

    // =====================================================
    // WIZARD COMPLETION
    // =====================================================

    public boolean canComplete() {
        // Can complete from step 4 with minimum required data
        return currentStep.get() == 4 && isStep2Valid() && isStep3Valid();
    }

    public boolean canSkip() {
        return currentStep.get() > 1 && !completed.get();
    }

    public void skipSetup() {
        if (canSkip()) {
            // Set defaults
            if (selectedTaxYear.get() == null) {
                selectedTaxYear.set(getRecommendedTaxYear());
            }
            if (selectedBusinessType.get() == null) {
                selectedBusinessType.set(BusinessType.SOLE_TRADER);
            }
            completed.set(true);
        }
    }

    public void complete() {
        if (canComplete() || canSkip()) {
            completed.set(true);
        }
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public BooleanProperty completedProperty() {
        return completed;
    }

    public OnboardingCompletionSummary getCompletionSummary() {
        return new OnboardingCompletionSummary(
                userName.get(),
                utr.get(),
                niNumber.get(),
                selectedTaxYear.get(),
                selectedBusinessType.get()
        );
    }

    public String getPersonalizedWelcome() {
        String name = userName.get();
        if (name == null || name.isEmpty()) {
            return "You're all set!";
        }
        return "You're all set, " + name + "!";
    }

    // =====================================================
    // WIZARD RESET
    // =====================================================

    public void reset() {
        currentStep.set(1);
        userName.set("");
        utr.set("");
        niNumber.set("");
        selectedTaxYear.set(null);
        selectedBusinessType.set(null);
        completed.set(false);
    }

    // =====================================================
    // INNER CLASSES
    // =====================================================

    /**
     * Summary of onboarding data after completion.
     */
    public record OnboardingCompletionSummary(
            String userName,
            String utr,
            String niNumber,
            String taxYear,
            BusinessType businessType
    ) {}
}
