package com.image.generation.common;

import com.image.generation.config.SecurityConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Inlines {@code swagger/lock-icon.css} into the Swagger UI page, which swaps the
 * padlock so that a successful authorization shows an open lock instead of a closed one.
 * <p>
 * Inlined rather than linked on purpose: an extra {@code .css} URL would need its own
 * {@code permitAll()} rule in {@link SecurityConfig}, and would come back 401 without one.
 * <p>
 * Replaces springdoc's own transformer bean (declared {@code @ConditionalOnMissingBean}),
 * so everything springdoc normally injects into index.html still happens via {@code super}.
 */
@Component
class SwaggerLockIconTransformer extends SwaggerIndexPageTransformer {

    private static final String MARKER = "swagger-lock-icon";
    private static final String CSS_LOCATION = "swagger/lock-icon.css";

    SwaggerLockIconTransformer(SwaggerUiConfigProperties swaggerUiConfig,
                               SwaggerUiOAuthProperties swaggerUiOAuthProperties,
                               SwaggerWelcomeCommon swaggerWelcomeCommon,
                               ObjectMapperProvider objectMapperProvider) {
        super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain chain)
            throws IOException {
        Resource transformed = super.transform(request, resource, chain);
        if (!"index.html".equals(resource.getFilename())) {
            return transformed;
        }

        String html = readFullyAsString(transformed.getInputStream());
        if (html.contains(MARKER) || !html.contains("</head>")) {
            return transformed;
        }

        String style = "<style id=\"%s\">%n%s%n</style>%n</head>".formatted(MARKER, css());
        return new TransformedResource(resource,
                html.replace("</head>", style).getBytes(StandardCharsets.UTF_8));
    }

    private String css() throws IOException {
        try (var in = new ClassPathResource(CSS_LOCATION).getInputStream()) {
            return readFullyAsString(in);
        }
    }
}
