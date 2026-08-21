package com.oauth.server.service;

import com.oauth.server.dto.RegisterRequest;
import com.oauth.server.model.User;
import com.oauth.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing users (registration, lookup, profile updates).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new user.
     *
     * @param request registration details
     * @return the created user
     * @throws IllegalArgumentException if username or email is already taken
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        // Check for duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Create the user with encoded password
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user: {}", savedUser.getUsername());
        return savedUser;
    }

    /**
     * Find a user by username.
     *
     * @return the user, or null if not found
     */
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Find a user by ID.
     *
     * @return the user, or null if not found
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * Register a new user with a specific role.
     *
     * @param request registration details
     * @param role    the role to assign (e.g., "USER" or "ADMIN")
     * @return the created user
     * @throws IllegalArgumentException if username or email is already taken
     */
    @Transactional
    public User registerUser(RegisterRequest request, String role) {
        // Check for duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Create the user with encoded password and specified role
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .role(role != null ? role : "USER")
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user: {} with role: {}", savedUser.getUsername(), savedUser.getRole());
        return savedUser;
    }

    /**
     * List all users in the system.
     *
     * @return list of all users
     */
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}
