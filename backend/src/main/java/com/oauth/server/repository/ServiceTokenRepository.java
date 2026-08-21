package com.oauth.server.repository;

import com.oauth.server.model.ServiceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ServiceToken entities.
 */
@Repository
public interface ServiceTokenRepository extends JpaRepository<ServiceToken, Long> {

    /**
     * Find all service tokens ordered by issue date (newest first).
     */
    List<ServiceToken> findAllByOrderByIssuedAtDesc();

    /**
     * Find a service token by its token value.
     */
    Optional<ServiceToken> findByToken(String token);
}
