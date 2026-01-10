package uk.selfemploy.ui.service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * User preferences for deadline notifications.
 * Controls which notifications are enabled and when they trigger.
 */
public class NotificationPreferences {

    /**
     * Default trigger days before deadline: 30, 7, and 1 day.
     */
    public static final List<Integer> DEFAULT_TRIGGER_DAYS = List.of(30, 7, 1);

    // Master enable/disable
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);

    // Individual notification type toggles
    private final BooleanProperty filingDeadlineEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty paymentDeadlineEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty quarterlyReminderEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty poaDeadlineEnabled = new SimpleBooleanProperty(true);

    // Sound and visual preferences
    private final BooleanProperty soundEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty systemTrayEnabled = new SimpleBooleanProperty(true);

    // Trigger timing
    private List<Integer> triggerDays = new ArrayList<>(DEFAULT_TRIGGER_DAYS);

    // === Master Enable ===

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean value) {
        enabled.set(value);
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    // === Filing Deadline ===

    public boolean isFilingDeadlineEnabled() {
        return filingDeadlineEnabled.get();
    }

    public void setFilingDeadlineEnabled(boolean value) {
        filingDeadlineEnabled.set(value);
    }

    public BooleanProperty filingDeadlineEnabledProperty() {
        return filingDeadlineEnabled;
    }

    // === Payment Deadline ===

    public boolean isPaymentDeadlineEnabled() {
        return paymentDeadlineEnabled.get();
    }

    public void setPaymentDeadlineEnabled(boolean value) {
        paymentDeadlineEnabled.set(value);
    }

    public BooleanProperty paymentDeadlineEnabledProperty() {
        return paymentDeadlineEnabled;
    }

    // === Quarterly Reminder ===

    public boolean isQuarterlyReminderEnabled() {
        return quarterlyReminderEnabled.get();
    }

    public void setQuarterlyReminderEnabled(boolean value) {
        quarterlyReminderEnabled.set(value);
    }

    public BooleanProperty quarterlyReminderEnabledProperty() {
        return quarterlyReminderEnabled;
    }

    // === POA Deadline ===

    public boolean isPoaDeadlineEnabled() {
        return poaDeadlineEnabled.get();
    }

    public void setPoaDeadlineEnabled(boolean value) {
        poaDeadlineEnabled.set(value);
    }

    public BooleanProperty poaDeadlineEnabledProperty() {
        return poaDeadlineEnabled;
    }

    // === Sound ===

    public boolean isSoundEnabled() {
        return soundEnabled.get();
    }

    public void setSoundEnabled(boolean value) {
        soundEnabled.set(value);
    }

    public BooleanProperty soundEnabledProperty() {
        return soundEnabled;
    }

    // === System Tray ===

    public boolean isSystemTrayEnabled() {
        return systemTrayEnabled.get();
    }

    public void setSystemTrayEnabled(boolean value) {
        systemTrayEnabled.set(value);
    }

    public BooleanProperty systemTrayEnabledProperty() {
        return systemTrayEnabled;
    }

    // === Trigger Days ===

    public List<Integer> getTriggerDays() {
        return List.copyOf(triggerDays);
    }

    public void setTriggerDays(List<Integer> days) {
        this.triggerDays = new ArrayList<>(days);
    }

    /**
     * Checks if a specific trigger day is configured.
     */
    public boolean hasTriggerDay(int days) {
        return triggerDays.contains(days);
    }

    /**
     * Resets all preferences to defaults.
     */
    public void resetToDefaults() {
        enabled.set(true);
        filingDeadlineEnabled.set(true);
        paymentDeadlineEnabled.set(true);
        quarterlyReminderEnabled.set(true);
        poaDeadlineEnabled.set(true);
        soundEnabled.set(true);
        systemTrayEnabled.set(true);
        triggerDays = new ArrayList<>(DEFAULT_TRIGGER_DAYS);
    }
}
