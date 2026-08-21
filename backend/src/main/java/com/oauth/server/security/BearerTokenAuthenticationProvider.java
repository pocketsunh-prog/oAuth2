package com.oauth.server.security;

import com.oauth.server.model.User;
import com.oauth.server.model.UserToken;
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
 * Tokens are looked up in the user_tokens table and checked for expiration and revocation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BearerTokenAuthenticationProvider implements AuthenticationProvider {

    private final UserTokenRepository tokenRepository;

    @Override
    @Transactional(readOnly = true)
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();

        // Look up the token in the database
        UserToken userToken = tokenRepository.findByAccessToken(token).orElse(null);

        if (userToken == null) {
            log.debug("Token not found in database");
            return null;
        }

        // Check if the token is revoked
        if (userToken.isRevoked()) {
            log.debug("Token has been revoked");
            return null;
        }

        // Check if the token is expired
        if (userToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.debug("Token has expired");
            return null;
        }

        // Token is valid - return authenticated token with username and role
        User user = userToken.getUser();
        String role = user.getRole() != null ? user.getRole() : "USER";
        return new BearerTokenAuthenticationToken(
                token,
                user.getUsername(),
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
