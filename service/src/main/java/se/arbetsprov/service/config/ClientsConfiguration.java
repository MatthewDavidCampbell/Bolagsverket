package se.arbetsprov.service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.arbetsprov.core.credential.KeyCredential;
import se.arbetsprov.countries.CountriesClient;
import se.arbetsprov.countries.CountriesClientOptions;
import se.arbetsprov.visits.VisitsClient;
import se.arbetsprov.visits.VisitsClientOptions;

/**
 * Maps configuration onto the service libraries' options and registers their clients as beans.
 * The libraries know nothing about Spring; this is the only place they meet it.
 */
@Configuration
@EnableConfigurationProperties({CountriesProperties.class, VisitsProperties.class})
public class ClientsConfiguration {

    @Bean
    CountriesClient countriesClient(CountriesProperties properties) {
        return new CountriesClient(new CountriesClientOptions()
                .baseUri(properties.baseUri())
                .credential(KeyCredential.ofNullable(properties.apiKey()))
                .pageSize(properties.pageSize()));
    }

    @Bean
    VisitsClient visitsClient(VisitsProperties properties, CountriesClient countries) {
        // Visits only needs to know whether a country exists; the countries cache answers that.
        return new VisitsClient(
                new VisitsClientOptions().backend(properties.backendType()),
                code -> countries.get(code).isPresent());
    }
}
