package com.oauth.server.service;

import com.oauth.server.model.User;
import com.oauth.server.model.UserToken;
import com.oauth.server.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for persisting OAuth2 tokens to the database.
 * Enables token management (listing and revocation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenStorageService {

    private final UserTokenRepository tokenRepository;

    @Value("${app.refresh-token-validity}")
    private long refreshTokenValidity;

    /**
     * Find a token record by its refresh token value.
     */
    @Transactional(readOnly = true)
    public Optional<UserToken> findByRefreshToken(String refreshToken) {
        return tokenRepository.findByRefreshToken(refreshToken);
    }

    /**
     * Save a new token to the database.
     *
     * @param user        the user who owns the token
     * @param clientId    the OAuth2 client that requested the token
     * @param accessToken the access token value
     * @param refreshToken the refresh token value (may be null)
     * @param tokenType   the token type (e.g., "Bearer")
     * @param scopes      the granted scopes
     * @param expiresAt   the access token expiration time
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
                .refreshExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenValidity))
                .revoked(false)
                .build();

        tokenRepository.save(token);
        log.debug("Stored token for user {} from client {}", user.getUsername(), clientId);
    }

    /**
     * Rotate an existing token pair: replace access/refresh tokens and update expiry.
     * The old refresh token is no longer valid after this call.
     *
     * @param token          the existing token entity to update
     * @param newAccessToken the new access token value
     * @param newRefreshToken the new refresh token value
     * @param newExpiresAt   the new access token expiration time
     */
    @Transactional
    public void rotateTokens(UserToken token, String newAccessToken,
                             String newRefreshToken, LocalDateTime newExpiresAt) {
        token.setAccessToken(newAccessToken);
        token.setRefreshToken(newRefreshToken);
        token.setExpiresAt(newExpiresAt);
        token.setRefreshExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenValidity));
        token.setIssuedAt(LocalDateTime.now());
        tokenRepository.save(token);
        log.debug("Rotated tokens for user {}", token.getUser().getUsername());
    }
}
