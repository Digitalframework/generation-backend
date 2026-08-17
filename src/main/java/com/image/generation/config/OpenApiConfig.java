package com.image.generation.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI is served at /swagger-ui.html, the schema at /v3/api-docs.
 */
@Configuration
class OpenApiConfig {

    /** Name of the scheme below; only ever referred to within this file. */
    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    OpenAPI generationOpenApi() {
        return new OpenAPI()
                // Puts the "Authorize" button in the UI. Without it Swagger sends no
                // Authorization header at all, and every upload from this page comes
                // back 401 no matter who is signed in — which looks like the endpoint
                // is broken rather than like a missing token.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        Paste the accessToken from
                                        http://localhost:8080/api/token — open it in a
                                        browser tab where you are signed in to
                                        AuthAndSecurity. Uploading needs an ADMIN account.""")))
                .info(new Info()
                .title("Furniture picture API")
                .version("v1")
                .description("""
                        1024x1024 PNG renderings of furniture, stored as blobs in PostgreSQL.
                        The upload file name carries the name and the measurements:
                        <name>_<width>x<height>x<depth>.png, e.g. Hoeffner_1_180x90x40.png."""));
    }
}
