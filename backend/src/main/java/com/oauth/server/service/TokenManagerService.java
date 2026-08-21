package com.oauth.server.service;

import com.oauth.server.dto.TokenInfo;
import com.oauth.server.model.User;
import com.oauth.server.model.UserToken;
import com.oauth.server.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing OAuth2 tokens (list, revoke).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenManagerService {

    private final UserTokenRepository tokenRepository;

    /**
     * List all tokens for a given user.
     *
     * @param user the token owner
     * @return list of token info DTOs
     */
    @Transactional(readOnly = true)
    public List<TokenInfo> listTokens(User user) {
        return tokenRepository.findByUserIdOrderByIssuedAtDesc(user.getId())
                .stream()
                .map(this::toTokenInfo)
                .collect(Collectors.toList());
    }

    /**
     * Revoke a token by its ID.
     * Only tokens owned by the given user can be revoked.
     *
     * @param tokenId the token to revoke
     * @param user    the owner of the token
     * @return true if the token was found and revoked
     */
    @Transactional
    public boolean revokeToken(Long tokenId, User user) {
        UserToken token = tokenRepository.findById(tokenId).orElse(null);

        if (token == null) {
            log.warn("Token not found: {}", tokenId);
            return false;
        }

        if (!token.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to revoke token {} owned by user {}",
                    user.getId(), tokenId, token.getUser().getId());
            return false;
        }

        token.setRevoked(true);
        tokenRepository.save(token);
        log.info("Revoked token {} for user {}", tokenId, user.getUsername());
        return true;
    }

    /**
     * Revoke all tokens for a user.
     *
     * @param user the owner of the tokens
     * @return the number of tokens revoked
     */
    @Transactional
    public int revokeAllTokens(User user) {
        List<UserToken> tokens = tokenRepository.findByUserIdOrderByIssuedAtDesc(user.getId());
        int count = 0;

        for (UserToken token : tokens) {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                count++;
            }
        }

        tokenRepository.saveAll(tokens);
        log.info("Revoked {} tokens for user {}", count, user.getUsername());
        return count;
    }

    /**
     * Count active (non-revoked) tokens for a user.
     */
    @Transactional(readOnly = true)
    public long countActiveTokens(User user) {
        return tokenRepository.countByUserIdAndRevokedFalse(user.getId());
    }

    /**
     * Convert a UserToken entity to a TokenInfo DTO.
     * The access token value is partially masked for security.
     */
    private TokenInfo toTokenInfo(UserToken token) {
        return TokenInfo.builder()
                .id(token.getId())
                .clientId(token.getClientId())
                .tokenType(token.getTokenType())
                .scopes(token.getScopes())
                .accessTokenPreview(maskToken(token.getAccessToken()))
                .issuedAt(token.getIssuedAt())
                .expiresAt(token.getExpiresAt())
                .revoked(token.isRevoked())
                .expired(token.getExpiresAt().isBefore(LocalDateTime.now()))
                .build();
    }

    /**
     * Mask a token value, showing only the last 8 characters.
     * e.g., "abcdef1234567890" -> "...567890"
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return "..." + token.substring(token.length() - 8);
    }
}
