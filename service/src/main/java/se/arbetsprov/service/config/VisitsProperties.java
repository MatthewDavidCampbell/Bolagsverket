package se.arbetsprov.service.config;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import se.arbetsprov.visits.VisitsBackend;

/**
 * Settings under {@code visits.*}.
 *
 * @param backend where visits are kept, for example {@code in-process}
 */
@ConfigurationProperties("visits")
public record VisitsProperties(@DefaultValue("in-process") String backend) {

    /** The configured backend as the library's enum; fails on an unknown value. */
    public VisitsBackend backendType() {
        try {
            return VisitsBackend.valueOf(backend.strip().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown visits.backend '" + backend + "'; supported: in-process", e);
        }
    }
}
