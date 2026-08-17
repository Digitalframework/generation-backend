package com.image.generation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Who this service will talk to, and how it recognises them.
 *
 * There is no login here and there never will be: sign-in lives in AuthAndSecurity,
 * which hands a caller a short-lived signed token. This application only verifies
 * that token, and it does so with the public key published at that application's
 * JWKS endpoint — so it holds nothing that could mint one, and needs no share of its
 * session store or its user table.
 *
 * The URL rules below stop at "is this caller anyone at all". Whether they may
 * <em>write</em> is decided by {@code @PreAuthorize} on the controller and the
 * service, so the rule sits next to the code it protects rather than being restated
 * as a path pattern that a later {@code @RequestMapping} rename would quietly
 * detach.
 */
@Configuration
@EnableWebSecurity
// Without this, every @PreAuthorize in the application is ignored — silently, and
// with no warning at startup. It is the annotation that turns them on.
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Nothing here reads a cookie, so there is no ambient credential for a
                // forged cross-site request to ride on — which is the only thing CSRF
                // tokens defend. A bearer token has to be attached deliberately.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // The category tree stays open — it is a list of names, and a
                        // client needs it to build the filters before it has anyone
                        // signed in. The pictures themselves do not: reading those
                        // takes a token, and the role is checked on the controller
                        // like every other rule here.
                        //
                        // The cost of that is deliberate: a browser cannot put
                        // /api/images/{id}/content in an <img src> any more, because an
                        // img tag sends no Authorization header. A client that wants to
                        // display one has to fetch the bytes itself and hand the result
                        // to the tag as a blob or data URL.
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                // Turns a missing or expired token into 401 rather than a redirect to
                // a login page this service does not have.
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Lets the browser app on another origin actually send the token.
     *
     * Without this the preflight is refused and the request is never made — the
     * console reports a CORS error and nothing reaches this application at all, which
     * reads like the endpoint is broken rather than like a policy decision.
     *
     * Origins are listed rather than wildcarded because {@code allowedHeaders} here
     * includes {@code Authorization}: a wildcard origin would invite any page on the
     * internet to spend a token it somehow got hold of.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {

        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // The upload response's Location header, which the browser otherwise hides.
        cors.setExposedHeaders(List.of("Location", "ETag"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    /**
     * Translates the token's {@code roles} claim into authorities.
     *
     * Two defaults have to be overridden and both are silent when wrong — the result
     * is simply a caller with no authorities, refused with a 403 that says nothing
     * about why. Spring looks in {@code scope}/{@code scp} by default, which this
     * token does not use, and prefixes with {@code SCOPE_}, whereas
     * {@code hasRole('ADMIN')} tests for exactly {@code ROLE_ADMIN}.
     *
     * The prefix is added here rather than baked into the claim so the token stays a
     * plain statement of fact — {@code ["USER","ADMIN"]} — instead of carrying one
     * framework's naming convention across a network boundary.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
