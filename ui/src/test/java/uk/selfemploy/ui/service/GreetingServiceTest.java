package uk.selfemploy.ui.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GreetingService.
 * Tests personalized time-based greeting generation.
 */
@DisplayName("GreetingService")
class GreetingServiceTest {

    private GreetingService greetingService;

    @BeforeEach
    void setUp() {
        greetingService = new GreetingService();
    }

    @Nested
    @DisplayName("Time-Based Greeting")
    class TimeBasedGreeting {

        @Test
        @DisplayName("should return 'Good morning' between 5:00 and 11:59")
        void shouldReturnGoodMorningInMorning() {
            // Given - 9:00 AM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T09:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getTimeBasedGreeting();

            // Then
            assertThat(greeting).isEqualTo("Good morning");
        }

        @Test
        @DisplayName("should return 'Good afternoon' between 12:00 and 17:59")
        void shouldReturnGoodAfternoonInAfternoon() {
            // Given - 2:00 PM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T14:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getTimeBasedGreeting();

            // Then
            assertThat(greeting).isEqualTo("Good afternoon");
        }

        @Test
        @DisplayName("should return 'Good evening' between 18:00 and 4:59")
        void shouldReturnGoodEveningInEvening() {
            // Given - 8:00 PM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T20:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getTimeBasedGreeting();

            // Then
            assertThat(greeting).isEqualTo("Good evening");
        }

        @Test
        @DisplayName("should return 'Good evening' at midnight")
        void shouldReturnGoodEveningAtMidnight() {
            // Given - midnight
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T00:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getTimeBasedGreeting();

            // Then
            assertThat(greeting).isEqualTo("Good evening");
        }

        @Test
        @DisplayName("should return 'Good morning' at 5:00 AM")
        void shouldReturnGoodMorningAt5AM() {
            // Given - 5:00 AM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T05:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getTimeBasedGreeting();

            // Then
            assertThat(greeting).isEqualTo("Good morning");
        }

        @Test
        @DisplayName("should return 'Good afternoon' at noon")
        void shouldReturnGoodAfternoonAtNoon() {
            // Given - 12:00 PM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T12:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getTimeBasedGreeting();

            // Then
            assertThat(greeting).isEqualTo("Good afternoon");
        }

        @Test
        @DisplayName("should return 'Good evening' at 6:00 PM")
        void shouldReturnGoodEveningAt6PM() {
            // Given - 6:00 PM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T18:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getTimeBasedGreeting();

            // Then
            assertThat(greeting).isEqualTo("Good evening");
        }
    }

    @Nested
    @DisplayName("Personalized Greeting")
    class PersonalizedGreeting {

        @Test
        @DisplayName("should return greeting with name when name is provided")
        void shouldReturnGreetingWithName() {
            // Given - 9:00 AM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T09:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getPersonalizedGreeting("Sarah");

            // Then
            assertThat(greeting).isEqualTo("Good morning, Sarah!");
        }

        @Test
        @DisplayName("should return greeting without name when name is null")
        void shouldReturnGreetingWithoutNameWhenNull() {
            // Given - 9:00 AM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T09:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getPersonalizedGreeting(null);

            // Then
            assertThat(greeting).isEqualTo("Good morning!");
        }

        @Test
        @DisplayName("should return greeting without name when name is empty")
        void shouldReturnGreetingWithoutNameWhenEmpty() {
            // Given - 2:00 PM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T14:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getPersonalizedGreeting("");

            // Then
            assertThat(greeting).isEqualTo("Good afternoon!");
        }

        @Test
        @DisplayName("should return greeting without name when name is blank")
        void shouldReturnGreetingWithoutNameWhenBlank() {
            // Given - 8:00 PM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T20:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getPersonalizedGreeting("   ");

            // Then
            assertThat(greeting).isEqualTo("Good evening!");
        }

        @Test
        @DisplayName("should trim name in personalized greeting")
        void shouldTrimNameInPersonalizedGreeting() {
            // Given - 9:00 AM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T09:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getPersonalizedGreeting("  John  ");

            // Then
            assertThat(greeting).isEqualTo("Good morning, John!");
        }

        @Test
        @DisplayName("should handle unicode names in greeting")
        void shouldHandleUnicodeNamesInGreeting() {
            // Given - 2:00 PM
            Clock clock = Clock.fixed(
                Instant.parse("2026-01-28T14:00:00Z"),
                ZoneId.of("Europe/London")
            );
            greetingService = new GreetingService(clock);

            // When
            String greeting = greetingService.getPersonalizedGreeting("Émile");

            // Then
            assertThat(greeting).isEqualTo("Good afternoon, Émile!");
        }
    }

    @Nested
    @DisplayName("Default Clock")
    class DefaultClock {

        @Test
        @DisplayName("should use system default clock when none provided")
        void shouldUseSystemDefaultClock() {
            // Given - default constructor
            GreetingService service = new GreetingService();

            // When
            String greeting = service.getTimeBasedGreeting();

            // Then - should return one of the three valid greetings
            assertThat(greeting).isIn("Good morning", "Good afternoon", "Good evening");
        }
    }
}
