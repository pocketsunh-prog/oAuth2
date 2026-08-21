package com.oauth.server.service;

import com.oauth.server.dto.CreateServiceTokenRequest;
import com.oauth.server.dto.ServiceTokenInfo;
import com.oauth.server.model.ServiceToken;
import com.oauth.server.model.User;
import com.oauth.server.repository.ServiceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing service-to-service tokens.
 * Service tokens are long-lived API keys issued by admins.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceTokenService {

    private final ServiceTokenRepository serviceTokenRepository;

    /**
     * Create a new service token.
     *
     * @param request the creation request
     * @param admin   the admin user creating the token
     * @return the created service token entity (with full token value)
     */
    @Transactional
    public ServiceToken createToken(CreateServiceTokenRequest request, User admin) {
        String tokenValue = "srv_" + UUID.randomUUID().toString().replace("-", "");

        LocalDateTime expiresAt = null;
        if (request.getExpiresInDays() != null) {
            expiresAt = LocalDateTime.now().plusDays(request.getExpiresInDays());
        }

        ServiceToken serviceToken = ServiceToken.builder()
                .name(request.getName())
                .token(tokenValue)
                .scopes(request.getScopes())
                .issuedBy(admin)
                .issuedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        ServiceToken saved = serviceTokenRepository.save(serviceToken);
        log.info("Admin {} created service token: {}", admin.getUsername(), request.getName());
        return saved;
    }

    /**
     * List all service tokens.
     *
     * @return list of service token info DTOs
     */
    @Transactional(readOnly = true)
    public List<ServiceTokenInfo> listTokens() {
        return serviceTokenRepository.findAllByOrderByIssuedAtDesc()
                .stream()
                .map(this::toServiceTokenInfo)
                .collect(Collectors.toList());
    }

    /**
     * Revoke a service token by its ID.
     *
     * @param id the token ID
     * @return true if the token was found and revoked
     */
    @Transactional
    public boolean revokeToken(Long id) {
        ServiceToken token = serviceTokenRepository.findById(id).orElse(null);

        if (token == null) {
            log.warn("Service token not found: {}", id);
            return false;
        }

        token.setRevoked(true);
        serviceTokenRepository.save(token);
        log.info("Revoked service token {} ({})", id, token.getName());
        return true;
    }

    /**
     * Convert a ServiceToken entity to a ServiceTokenInfo DTO.
     * The token value is partially masked for security.
     */
    private ServiceTokenInfo toServiceTokenInfo(ServiceToken token) {
        boolean expired = token.getExpiresAt() != null
                && token.getExpiresAt().isBefore(LocalDateTime.now());

        return ServiceTokenInfo.builder()
                .id(token.getId())
                .name(token.getName())
                .tokenPreview(maskToken(token.getToken()))
                .scopes(token.getScopes())
                .issuedBy(token.getIssuedBy() != null ? token.getIssuedBy().getUsername() : null)
                .issuedAt(token.getIssuedAt())
                .expiresAt(token.getExpiresAt())
                .revoked(token.isRevoked())
                .expired(expired)
                .build();
    }

    /**
     * Mask a token value, showing only the last 8 characters.
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return "..." + token.substring(token.length() - 8);
    }
}
