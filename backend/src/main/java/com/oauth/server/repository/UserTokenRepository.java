package com.oauth.server.repository;

import com.oauth.server.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing UserToken entities.
 */
@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    /**
     * Find all tokens belonging to a specific user.
     */
    List<UserToken> findByUserIdOrderByIssuedAtDesc(Long userId);

    /**
     * Find a token by its access token value.
     */
    Optional<UserToken> findByAccessToken(String accessToken);

    /**
     * Find a token by its refresh token value.
     */
    Optional<UserToken> findByRefreshToken(String refreshToken);

    /**
     * Find all tokens for a specific client.
     */
    List<UserToken> findByClientId(String clientId);

    /**
     * Count active (non-revoked) tokens for a user.
     */
    long countByUserIdAndRevokedFalse(Long userId);
}
