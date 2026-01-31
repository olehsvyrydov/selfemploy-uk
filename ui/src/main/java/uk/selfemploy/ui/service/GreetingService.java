package uk.selfemploy.ui.service;

import java.time.Clock;
import java.time.LocalTime;

/**
 * Service for generating personalized time-based greetings.
 *
 * <p>Provides greetings based on time of day:
 * <ul>
 *   <li>5:00 - 11:59: "Good morning"</li>
 *   <li>12:00 - 17:59: "Good afternoon"</li>
 *   <li>18:00 - 4:59: "Good evening"</li>
 * </ul>
 *
 * <p>Uses Clock injection for testability following project patterns.
 */
public class GreetingService {

    private static final int MORNING_START = 5;
    private static final int AFTERNOON_START = 12;
    private static final int EVENING_START = 18;

    private final Clock clock;

    /**
     * Creates a GreetingService using the system default clock.
     */
    public GreetingService() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Creates a GreetingService with a specific clock.
     * Useful for testing with fixed times.
     *
     * @param clock the clock to use for time calculations
     */
    public GreetingService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Returns a time-based greeting without personalization.
     *
     * @return "Good morning", "Good afternoon", or "Good evening"
     */
    public String getTimeBasedGreeting() {
        LocalTime now = LocalTime.now(clock);
        int hour = now.getHour();

        if (hour >= MORNING_START && hour < AFTERNOON_START) {
            return "Good morning";
        } else if (hour >= AFTERNOON_START && hour < EVENING_START) {
            return "Good afternoon";
        } else {
            return "Good evening";
        }
    }

    /**
     * Returns a personalized time-based greeting.
     *
     * <p>If name is provided: "Good morning, Sarah!"
     * <p>If name is null/empty/blank: "Good morning!"
     *
     * @param displayName the user's display name, or null/empty for anonymous greeting
     * @return the personalized greeting with exclamation mark
     */
    public String getPersonalizedGreeting(String displayName) {
        String baseGreeting = getTimeBasedGreeting();

        if (displayName == null || displayName.isBlank()) {
            return baseGreeting + "!";
        }

        return baseGreeting + ", " + displayName.trim() + "!";
    }
}
