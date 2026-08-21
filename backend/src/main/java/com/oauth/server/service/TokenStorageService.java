package com.oauth.server.service;

import com.oauth.server.model.User;
import com.oauth.server.model.UserToken;
import com.oauth.server.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for persisting OAuth2 tokens to the database.
 * Enables token management (listing and revocation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenStorageService {

    private final UserTokenRepository tokenRepository;

    /**
     * Save a new token to the database.
     *
     * @param user        the user who owns the token
     * @param clientId    the OAuth2 client that requested the token
     * @param accessToken the access token value
     * @param refreshToken the refresh token value (may be null)
     * @param tokenType   the token type (e.g., "Bearer")
     * @param scopes      the granted scopes
     * @param expiresAt   the expiration time
     */
    @Transactional
    public void storeToken(User user, String clientId, String accessToken,
                           String refreshToken, String tokenType,
                           String scopes, LocalDateTime expiresAt) {
        UserToken token = UserToken.builder()
                .user(user)
                .clientId(clientId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .scopes(scopes)
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        tokenRepository.save(token);
        log.debug("Stored token for user {} from client {}", user.getUsername(), clientId);
    }
}
