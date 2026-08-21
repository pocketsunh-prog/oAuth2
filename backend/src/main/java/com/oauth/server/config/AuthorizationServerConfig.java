package com.oauth.server.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * OAuth2 Authorization Server configuration.
 * Configures JWT token signing, registered clients, and token settings.
 */
@Configuration
@Slf4j
public class AuthorizationServerConfig {

    @Value("${app.issuer-uri}")
    private String issuerUri;

    @Value("${app.access-token-validity}")
    private long accessTokenValidity;

    @Value("${app.refresh-token-validity}")
    private long refreshTokenValidity;

    /**
     * Security filter chain for the OAuth2 authorization server.
     * Handles /oauth2/** endpoints with stateless sessions.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        // Restrict this filter chain to OAuth2 authorization server endpoints only
        http.securityMatcher("/oauth2/**", "/.well-known/**", "/userinfo");

        http.oauth2AuthorizationServer(customizer -> customizer
            .oidc(oidc -> oidc
                .userInfoEndpoint(userInfo -> {})
            )
        );

        http
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    PathPatternRequestMatcher.pathPattern("/oauth2/**")
                )
            )
            .oauth2ResourceServer(resourceServer ->
                resourceServer.jwt(jwt -> {}));

        return http.build();
    }

    /**
     * Create an RSA key pair for signing JWT tokens.
     * In production, this should be loaded from a keystore.
     */
    @Bean
    public RSAKey generateRsaKey() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * JWK source backed by the RSA key pair.
     */
    @Bean
    public com.nimbusds.jose.jwk.source.JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * JWT decoder for validating tokens.
     */
    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder(RSAKey rsaKey) throws Exception {
        return org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                .withPublicKey(rsaKey.toRSAPublicKey())
                .build();
    }

    /**
     * Registered client repository.
     * Defines the OAuth2 clients that can request tokens.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        RegisteredClient frontendClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("frontend-client")
                .clientSecret(passwordEncoder.encode("frontend-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("http://localhost:5173/callback")
                .redirectUri("http://localhost:5173")
                .scope(OidcScopes.OPENID)
                .scope("read")
                .scope("write")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(accessTokenValidity))
                        .refreshTokenTimeToLive(Duration.ofSeconds(refreshTokenValidity))
                        .reuseRefreshTokens(false)
                        .build())
                .build();

        log.info("Registered OAuth2 client: frontend-client");
        return new InMemoryRegisteredClientRepository(frontendClient);
    }

    /**
     * Authorization server settings.
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .jwkSetEndpoint("/.well-known/jwks.json")
                .oidcUserInfoEndpoint("/userinfo")
                .build();
    }
}
