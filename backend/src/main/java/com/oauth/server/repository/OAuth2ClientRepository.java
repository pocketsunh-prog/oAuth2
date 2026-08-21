package com.oauth.server.repository;

import com.oauth.server.model.OAuth2Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing OAuth2Client entities.
 */
@Repository
public interface OAuth2ClientRepository extends JpaRepository<OAuth2Client, Long> {

    /**
     * Find a client by its client ID.
     */
    Optional<OAuth2Client> findByClientId(String clientId);

    /**
     * Find a client by its name.
     */
    Optional<OAuth2Client> findByClientName(String clientName);

    /**
     * Check if a client ID already exists.
     */
    boolean existsByClientId(String clientId);
}
