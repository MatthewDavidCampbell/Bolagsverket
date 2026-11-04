package se.arbetsprov.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description shown at the top of Swagger UI. */
@Configuration
class OpenApiConfiguration {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Countries and visits API")
                .version("0.1.0")
                .description("""
                        Lists countries imported from REST Countries at start-up, and keeps each \
                        user's visited countries with a short note. Countries are always read from \
                        the local cache, never from REST Countries. Visits calls take the user from \
                        the X-User-Id header (a stand-in for an authenticated user)."""));
    }
}
