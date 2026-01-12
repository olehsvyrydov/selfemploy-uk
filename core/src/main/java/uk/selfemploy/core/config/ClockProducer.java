package uk.selfemploy.core.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Clock;

/**
 * CDI producer for java.time.Clock.
 *
 * <p>Provides a singleton Clock bean for dependency injection.
 * Using the system UTC clock by default ensures consistent timestamps
 * across the application.
 *
 * <p>For testing, a different Clock can be injected.
 */
@ApplicationScoped
public class ClockProducer {

    /**
     * Produces a system UTC clock bean.
     *
     * @return the system UTC clock
     */
    @Produces
    @ApplicationScoped
    public Clock clock() {
        return Clock.systemUTC();
    }
}
