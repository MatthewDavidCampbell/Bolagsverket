package se.arbetsprov.service.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import se.arbetsprov.countries.CountriesClient;

/**
 * Fills the countries cache at start-up (brief, user story 1). Spring Boot marks the application
 * ready only after all runners have finished, so the readiness probe stays down until the cache
 * is full and Kubernetes sends no traffic before then.
 */
@Component
class CountriesLoader implements ApplicationRunner {

    private final CountriesClient countries;

    CountriesLoader(CountriesClient countries) {
        this.countries = countries;
    }

    @Override
    public void run(ApplicationArguments args) {
        countries.load();
    }
}
