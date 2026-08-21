package com.oauth.server.repository;

import com.oauth.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by their email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a username already exists.
     */
    boolean existsByUsername(String username);

    /**
     * Check if an email address already exists.
     */
    boolean existsByEmail(String email);
}
