package com.oauth.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oauth.server.dto.ApiError;
import com.oauth.server.security.BearerTokenAuthenticationFilter;
import com.oauth.server.security.BearerTokenAuthenticationProvider;
import com.oauth.server.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Main security configuration for the OAuth2 server.
 * Sets up authentication, authorization, CORS, and session management.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final BearerTokenAuthenticationProvider bearerTokenAuthenticationProvider;

    /**
     * Password encoder bean used throughout the application.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager bean.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * CORS configuration to allow the frontend to call the API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Bearer token authentication filter bean.
     */
    @Bean
    public BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(
            AuthenticationManager authenticationManager) {
        return new BearerTokenAuthenticationFilter(authenticationManager);
    }

    /**
     * Main security filter chain.
     * Configures endpoint access rules, bearer token filter, and exception handling.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          BearerTokenAuthenticationFilter filter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh",
                    "/api/time",
                    "/oauth2/**",
                    "/.well-known/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/h2-console/**",
                    "/error"
                ).permitAll()
                // Admin endpoints require ROLE_ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Allow frames for H2 console
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // Register the custom authentication provider
            .authenticationProvider(bearerTokenAuthenticationProvider)
            // Add the bearer token filter before the username/password filter
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            // Handle unauthorized access with JSON
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    ApiError error = ApiError.builder()
                            .status(401)
                            .message("Authentication required")
                            .path(request.getRequestURI())
                            .build();
                    new ObjectMapper().writeValue(response.getOutputStream(), error);
                })
            );

        return http.build();
    }
}
