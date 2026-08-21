package com.oauth.server.security;

import com.oauth.server.model.ServiceToken;
import com.oauth.server.model.User;
import com.oauth.server.model.UserToken;
import com.oauth.server.repository.ServiceTokenRepository;
import com.oauth.server.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Authentication provider that validates bearer tokens against the database.
 * Checks both user_tokens (session tokens) and service_tokens (API keys).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BearerTokenAuthenticationProvider implements AuthenticationProvider {

    private final UserTokenRepository tokenRepository;
    private final ServiceTokenRepository serviceTokenRepository;

    @Override
    @Transactional(readOnly = true)
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();

        // First, try to look up as a user token
        UserToken userToken = tokenRepository.findByAccessToken(token).orElse(null);

        if (userToken != null) {
            // Check if the token is revoked
            if (userToken.isRevoked()) {
                log.debug("User token has been revoked");
                return null;
            }

            // Check if the token is expired
            if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.debug("User token has expired");
                return null;
            }

            // User token is valid - return authenticated token with username and role
            User user = userToken.getUser();
            String role = user.getRole() != null ? user.getRole() : "USER";
            return new BearerTokenAuthenticationToken(
                    token,
                    user.getUsername(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
        }

        // If not found, try to look up as a service token
        ServiceToken serviceToken = serviceTokenRepository.findByToken(token).orElse(null);

        if (serviceToken != null) {
            // Check if the token is revoked
            if (serviceToken.isRevoked()) {
                log.debug("Service token has been revoked");
                return null;
            }

            // Check if the token is expired (null expiresAt means never expires)
            if (serviceToken.getExpiresAt() != null
                    && serviceToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.debug("Service token has expired");
                return null;
            }

            // Service token is valid - return authenticated token with service role
            String principal = "service:" + serviceToken.getName();
            return new BearerTokenAuthenticationToken(
                    token,
                    principal,
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
            );
        }

        log.debug("Token not found in database");
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
